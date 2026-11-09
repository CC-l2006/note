package org.example.cipherws.note.user.config;

import org.example.cipherws.note.user.service.AvatarService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 把上传目录挂成静态资源：{@code GET /uploads/**} 直接吐文件。
 *
 * 为什么不用 controller 读文件返回：<img src> 带不了 Authorization 头，
 * 走 controller 就得额外做一套鉴权；而头像本来就是「拿到 URL 就能看」的东西。
 * 文件名是 用户id-随机串，不可枚举，所以这里不再加鉴权。
 *
 * 生产环境更推荐让 nginx 直接指到这个目录（少一层 Java 转发），
 * 当前 Nginx 已把 /uploads/ 转到本服务，两种方式都能用。
 *
 * 职责边界：这里只登记「URL 前缀 → 磁盘目录」的映射，不负责上传、
 * 不负责校验文件内容，那些都在 {@link AvatarService} 里；目录位置也只由它决定。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 依赖 AvatarService 只为拿到上传根目录：目录配置只解析一次，
    // 避免这里再写一份 @Value 导致两处配置漂移
    private final AvatarService avatarService;

    // 构造器注入：Spring 启动时就把目录算好，映射注册阶段不用再处理异常
    public WebConfig(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    /**
     * 注册 /uploads/** 的静态资源映射，让 {@code <img src>} 能直接取到头像。
     *
     * 该回调由 Spring MVC 启动时调用一次，这里只做注册，不读文件。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 转成 URI 再交给 Spring：Windows 盘符与空格路径都能被正确转义
        String location = avatarService.getRoot().toUri().toString();
        // Spring 要求目录型 location 以 / 结尾，否则会把它当成单个文件
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
