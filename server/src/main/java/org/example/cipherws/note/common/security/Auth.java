package org.example.cipherws.note.common.security;

import org.example.cipherws.note.common.exception.BusinessException;

/**
 * 验签通过后，{@link JwtAuthFilter} 会把身份放进这几个请求头，供控制器读取。
 *
 * <p>没有独立 Java 网关后，每个服务都要自己验 token：过滤器先删掉客户端伪造的
 * X-User-*，再按 JWT 的声明写回可信值。控制器只认这几个头，不直接解析 token。</p>
 */
public final class Auth {

    /** 用户 ID，由 JwtAuthFilter 验签后写入 */
    public static final String HEADER_USER_ID = "X-User-Id";
    /** 角色，user / admin */
    public static final String HEADER_USER_ROLE = "X-User-Role";
    /** 账号名，仅用于日志 */
    public static final String HEADER_USERNAME = "X-Username";

    /** 管理员角色值；与数据库 note_user.role 的取值保持一致 */
    public static final String ROLE_ADMIN = "admin";

    /** 工具类不允许实例化 */
    private Auth() {
    }

    /**
     * 管理员专属接口的守卫。需求 4.1.4：普通用户无后台入口，
     * 前端已经拦了一层，这里再拦一层 —— 前端拦截只是体验，后端拦截才是安全。
     */
    public static void requireAdmin(String role) {
        // 角色空值同样视为无权限：未登录或头缺失时不能放行
        if (!ROLE_ADMIN.equals(role)) {
            throw new BusinessException("无管理员权限");
        }
    }
}
