package org.example.cipherws.note.common.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cipherws.note.common.exception.BusinessException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 极简 JWT（HS256）实现，**只用 JDK 自带的 javax.crypto + Jackson**，
 * 不引入 jjwt 之类的库 —— 笔记系统只需要「签发 / 验签 / 取声明」三件事，
 * 为了这点功能多一个传递依赖不划算。
 *
 * 注意：这不是通用 JWT 库。它只认 HS256、不处理 jwk/x5c、不校验 aud/iss。
 * 如果将来要接第三方登录，应该换成标准实现。
 */
public final class JwtUtil {

    /** 声明段的序列化/反序列化都走 Jackson，避免手写字符串拼接 */
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** JWT 要求 base64url 且去掉补位的 '=' */
    private static final Base64.Encoder B64E = Base64.getUrlEncoder().withoutPadding();
    /** 与之配对的解码器 */
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    /** 固定的 header 段：{"alg":"HS256","typ":"JWT"} 的 base64url */
    private static final String HEADER_B64 =
            B64E.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    /** 纯静态工具类，禁止实例化 */
    private JwtUtil() {
    }

    /** 解析出来的载荷，只放业务真正要用的三个字段 */
    public record Payload(long userId, String username, String role) {
    }

    /**
     * 签发令牌。
     *
     * @param ttlSeconds 有效期（秒）；前端会话超时的体验由此决定
     */
    public static String sign(long userId, String username, String role, String secret, long ttlSeconds) {
        // 统一用秒级时间戳，与 JWT 规范的 iat / exp 单位一致
        long now = System.currentTimeMillis() / 1000;
        // 只放业务需要的字段：uid 用于定位用户，role 用于鉴权，sub 用于日志展示
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("uid", userId);
        claims.put("sub", username);
        claims.put("role", role);
        claims.put("iat", now);
        claims.put("exp", now + ttlSeconds);

        // 声明段序列化失败属于代码/环境问题，不应该伪装成业务失败
        String payloadB64;
        try {
            payloadB64 = B64E.encodeToString(MAPPER.writeValueAsBytes(claims));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 载荷序列化失败", e);
        }

        // 签名输入固定为「header.payload」，第三段是它的 HMAC
        String signingInput = HEADER_B64 + "." + payloadB64;
        return signingInput + "." + B64E.encodeToString(hmac(signingInput, secret));
    }

    /**
     * 验签并取出载荷。
     * 任何一步不通过都抛 BusinessException —— 前端会把它显示成「登录已失效」。
     */
    public static Payload verify(String token, String secret) {
        // 空值先拦掉，后续即可假定字符串非空
        if (token == null || token.isBlank()) {
            throw new BusinessException("未登录，请先登录");
        }
        // 兼容前端统一加 "Bearer " 前缀的写法，同时容忍首尾空格
        String raw = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();

        // JWT 必须正好三段，段数不对说明不是我们签发的格式
        String[] parts = raw.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException("登录凭证格式不正确");
        }

        // 重新计算前两段的签名，再与第三段比对
        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = hmac(signingInput, secret);
        byte[] actual;
        try {
            actual = B64D.decode(parts[2]);
        } catch (IllegalArgumentException e) {
            // 第三段不是合法 base64url，同样按格式错误处理
            throw new BusinessException("登录凭证格式不正确");
        }
        // 定长比较，避免按字节短路带来的时序侧信道
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new BusinessException("登录凭证无效，请重新登录");
        }

        // 验签通过后才解析声明，避免对不可信内容做无谓处理
        Map<String, Object> claims;
        try {
            // 用 TypeReference 读成强类型 Map，否则 getOrDefault 的入参类型是 ?，
            // 传 String 默认值会编译不过
            claims = MAPPER.readValue(B64D.decode(parts[1]), new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new BusinessException("登录凭证内容无法解析");
        }

        // 过期判断：exp 必须是数字，缺失或类型异常都视为无效凭证
        Object exp = claims.get("exp");
        if (!(exp instanceof Number) || ((Number) exp).longValue() < System.currentTimeMillis() / 1000) {
            throw new BusinessException("登录已过期，请重新登录");
        }

        // 用户标识是后续所有查询的依据，缺失就无法继续
        Object uid = claims.get("uid");
        if (!(uid instanceof Number)) {
            throw new BusinessException("登录凭证缺少用户标识");
        }

        // 角色缺失时退化为普通用户，避免旧 token 因新增字段而全部失效
        return new Payload(
                ((Number) uid).longValue(),
                String.valueOf(claims.getOrDefault("sub", "")),
                String.valueOf(claims.getOrDefault("role", "user")));
    }

    /** 计算 HmacSHA256；签名与验签共用这一处，保证算法只改一处 */
    private static byte[] hmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 算法或密钥不可用时属于部署配置问题，直接中断
            throw new IllegalStateException("JWT 签名失败，请检查 note.jwt.secret 配置", e);
        }
    }
}
