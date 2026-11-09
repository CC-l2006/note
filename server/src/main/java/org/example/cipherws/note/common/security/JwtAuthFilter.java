package org.example.cipherws.note.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cipherws.note.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 单体应用的 JWT 校验过滤器。
 *
 * <p>没有独立网关后，鉴权不能依赖「网关验完写 X-User-*，下游直接信」：
 * 任何能访问 8084 的请求都必须自己验签。这个过滤器做三件事：</p>
 * <ol>
 *   <li>白名单（登录/注册/发码/重置密码、actuator、头像）直接放行；</li>
 *   <li>其余请求验证 Authorization 里的 HS256 JWT；</li>
 *   <li>无论走哪条路，先把客户端伪造的 X-User-* 删掉，验签通过后再写回可信值。</li>
 * </ol>
 *
 * <p>控制器仍然用 {@code @RequestHeader(Auth.HEADER_USER_ID)} 读身份，
 * 所以业务代码不需要改；变化只发生在 HTTP 入口这一层。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthFilter extends OncePerRequestFilter {

    /** 过滤器自身日志；验签失败只记审计信息，不打印 token 原文 */
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    /** 通配符匹配器，用于白名单的 /actuator/** 这类模式 */
    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    /** 手工序列化 401 响应体，避免在过滤器里依赖 MVC 的消息转换器 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 服务内路径（没有 /api 前缀，前缀已由 Nginx / Vite 去掉）。
     * 只放「没有 token 也必须能进」的接口。
     */
    private static final List<String> WHITE_LIST = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/send-code",
            "/auth/reset-password",
            "/actuator/**",
            "/uploads/**"
    );

    /** 需要整体接管（删除后按验签结果重写）的身份头名称集合，大小写不敏感 */
    private static final Set<String> IDENTITY_HEADERS = Set.of(
            Auth.HEADER_USER_ID,
            Auth.HEADER_USER_ROLE,
            Auth.HEADER_USERNAME
    );

    /** HS256 密钥，启动时校验长度，避免弱密钥上线 */
    private final String secret;

    /**
     * 通过构造器注入配置，并在容器启动阶段就校验密钥强度：
     * 早失败比运行期才发现签名不安全要好。
     */
    public JwtAuthFilter(@Value("${note.jwt.secret}") String secret) {
        // 32 字节是 HS256 推荐的密钥长度下限
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("NOTE_JWT_SECRET 至少需要 32 字节");
        }
        this.secret = secret;
    }

    /**
     * 每个请求都会经过这里：先判断是否需要鉴权，再决定放行还是返回 401。
     * 注意无论哪条分支都必须调用 doFilter，否则请求会静默挂住。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 去掉 context-path，得到与白名单一致的「服务内路径」
        String path = request.getRequestURI().substring(request.getContextPath().length());
        log.debug("JWT filter path={} contextPath={} public={}", path, request.getContextPath(), isPublic(path));

        // 预检请求不携带 Authorization，先放行；跨域响应头由 Nginx 或后续 CORS 配置负责
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(path)) {
            filterChain.doFilter(new IdentityRequest(request, null), response);
            return;
        }

        // 缺少凭证时直接拒绝：不继续往下走，避免控制器拿到空的用户身份
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (token == null || token.isBlank()) {
            unauthorized(response, "未登录，请先登录");
            return;
        }

        // 验签失败统一转成 401；BusinessException 的文案已经是给用户看的
        JwtUtil.Payload payload;
        try {
            payload = JwtUtil.verify(token, secret);
        } catch (BusinessException e) {
            unauthorized(response, e.getMessage());
            return;
        }

        // 校验通过：用包装后的请求把可信身份传给下游控制器
        filterChain.doFilter(new IdentityRequest(request, payload), response);
    }

    /** 白名单命中即免鉴权；按声明顺序逐个匹配，路径模式支持通配符 */
    private boolean isPublic(String path) {
        for (String pattern : WHITE_LIST) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /** 鉴权失败时直接写 JSON，保持与 Result 相同的字段约定，前端无需特殊处理 */
    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        // 401 让前端拦截器能识别为登录态问题并跳转登录页
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 只回 success 与 errMessage 两个字段，与成功响应结构保持一致
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("errMessage", message);
        response.getWriter().write(MAPPER.writeValueAsString(body));
    }

    /**
     * 先删除客户端可能伪造的身份头，再按验签结果写回可信值。
     * 白名单请求传 null，相当于只做「删除」。
     */
    private static final class IdentityRequest extends HttpServletRequestWrapper {

        /** 本次请求可信的身份头；白名单请求为空表，等价于「全部删除」 */
        private final Map<String, String> identity;

        /** payload 为 null 时只做身份头清除，不为请求补充任何身份信息 */
        private IdentityRequest(HttpServletRequest request, JwtUtil.Payload payload) {
            super(request);
            this.identity = new LinkedHashMap<>();
            if (payload != null) {
                // 声明里的 userId 是 long，转成字符串后与 @RequestHeader 的读取方式对齐
                identity.put(Auth.HEADER_USER_ID, String.valueOf(payload.userId()));
                identity.put(Auth.HEADER_USER_ROLE, payload.role());
                identity.put(Auth.HEADER_USERNAME, payload.username());
            }
        }

        /** 身份头一律返回可信值；未登录时返回 null，控制器据此判定为匿名 */
        @Override
        public String getHeader(String name) {
            String key = findIdentityKey(name);
            if (key != null) {
                return identity.get(key);
            }
            return super.getHeader(name);
        }

        /** 身份头最多只有一个值，其余情况都交回原始请求 */
        @Override
        public Enumeration<String> getHeaders(String name) {
            String key = findIdentityKey(name);
            if (key != null) {
                String value = identity.get(key);
                return value == null
                        ? Collections.emptyEnumeration()
                        : Collections.enumeration(List.of(value));
            }
            return super.getHeaders(name);
        }

        /** 列举头名时先剔除客户端伪造的身份头，再补齐本次可信的身份头 */
        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> originals = super.getHeaderNames();
            while (originals.hasMoreElements()) {
                String name = originals.nextElement();
                if (findIdentityKey(name) == null) {
                    names.add(name);
                }
            }
            names.addAll(identity.keySet());
            return Collections.enumeration(names);
        }

        /** 大小写不敏感地判断某个头是否属于需要接管的身份头，返回规范名或 null */
        private static String findIdentityKey(String name) {
            if (name == null) {
                return null;
            }
            for (String header : IDENTITY_HEADERS) {
                if (header.equalsIgnoreCase(name)) {
                    return header;
                }
            }
            return null;
        }
    }
}
