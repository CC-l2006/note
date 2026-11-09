package org.example.cipherws.note.admin.controller;

import jakarta.annotation.Resource;
import org.example.cipherws.note.admin.service.AdminService;
import org.example.cipherws.note.common.exception.BusinessException;
import org.example.cipherws.note.common.result.Result;
import org.example.cipherws.note.common.security.Auth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 普通用户可见的公告：需求 4.6.4 用户登录后在公告页看到启用中的通知。
 *
 * <p>这个接口不要管理员，但必须已登录：公告是站内信息，
 * 没登录的人（请求头没有 X-User-Id）不该看到，所以这里自己补一道登录校验。</p>
 */
@RestController
@RequestMapping("/announcements")
public class AnnouncementController {

    /** 公告数据入口；这里只读，复用后台 service 的已发布公告查询 */
    @Resource
    private AdminService adminService;

    /** 登录用户可见的公告列表：只返回 enabled=1，按发布时间倒序 */
    @GetMapping
    public Result list(@RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId) {
        // 公告不算敏感数据，但仍要求登录：未登录（userId 为 null）直接拒绝
        if (userId == null) {
            throw new BusinessException("未登录，请先登录");
        }
        return adminService.listPublishedAnnouncements();
    }
}
