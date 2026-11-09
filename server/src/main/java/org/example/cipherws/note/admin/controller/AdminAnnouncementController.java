package org.example.cipherws.note.admin.controller;

import jakarta.annotation.Resource;
import org.example.cipherws.note.admin.dto.AdminDtos.AnnouncementRequest;
import org.example.cipherws.note.admin.service.AdminService;
import org.example.cipherws.note.common.result.Result;
import org.example.cipherws.note.common.security.Auth;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台公告管理：需求 4.6.4 管理员发布 / 编辑 / 下线 / 删除公告。
 *
 * <p>与普通用户侧的 {@link AnnouncementController} 分开：那边只读且只要 enabled=1，
 * 这边能看到全部并带写操作，靠 URL 前缀区分权限。</p>
 */
@RestController
@RequestMapping("/admin/announcements")
public class AdminAnnouncementController {

    /** 后台业务统一入口：公告的增删改查与对应审计日志都在 service 内完成 */
    @Resource
    private AdminService adminService;

    /** 公告列表：包含已下线的公告，分页返回；仅管理员可访问 */
    @GetMapping
    public Result list(@RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "size", required = false) Integer size,
                       @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        // 后台列表必须管理员，否则普通用户能看到已下线公告
        Auth.requireAdmin(role);
        return adminService.listAnnouncements(page, size);
    }

    /** 发布公告；请求体缺省 enabled 时按「立即启用」处理 */
    @PostMapping
    public Result create(@RequestBody(required = false) AnnouncementRequest request,
                         @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                         @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        // 先校验管理员身份，前端路由守卫只影响体验，不能当作权限边界
        Auth.requireAdmin(role);
        return adminService.createAnnouncement(userId, request);
    }

    /** 编辑公告：请求体里为 null 的字段表示这一项保持原值 */
    @PutMapping("/{id}")
    public Result update(@PathVariable("id") Long id,
                         @RequestBody(required = false) AnnouncementRequest request,
                         @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                         @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        // 管理员校验；id 缺省或公告不存在统一由 service 报「公告不存在」
        Auth.requireAdmin(role);
        return adminService.updateAnnouncement(userId, id, request);
    }

    /** 删除公告：物理删除且不可恢复，删除动作会写审计日志 */
    @DeleteMapping("/{id}")
    public Result remove(@PathVariable("id") Long id,
                         @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                         @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        // 管理员校验；userId 用于把这条删除记到具体操作人名下
        Auth.requireAdmin(role);
        return adminService.removeAnnouncement(userId, id);
    }
}
