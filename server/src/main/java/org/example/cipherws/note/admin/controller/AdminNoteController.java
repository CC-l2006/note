package org.example.cipherws.note.admin.controller;

import jakarta.annotation.Resource;
import org.example.cipherws.note.admin.dto.AdminDtos.NoteFlagRequest;
import org.example.cipherws.note.admin.service.AdminService;
import org.example.cipherws.note.common.result.Result;
import org.example.cipherws.note.common.security.Auth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 笔记巡检：需求 4.6.2 管理员只能标记违规，不能修改用户笔记正文。
 *
 * <p>这个类刻意只有两个接口：查列表、改 flagged。没有「编辑笔记」的映射，
 * 服务层对应的 UPDATE 也只碰 flagged 一列，需求 7.3 的只读约束在这里被焊死。</p>
 */
@RestController
@RequestMapping("/admin/notes")
public class AdminNoteController {

    /** 后台业务入口；笔记巡检与违规标记的落库、审计都在 service 内完成 */
    @Resource
    private AdminService adminService;

    /** 笔记巡检列表：标题 / 作者模糊搜索，按更新时间倒序，只看未删除的笔记 */
    @GetMapping
    public Result inspect(@RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "page", required = false) Integer page,
                          @RequestParam(value = "size", required = false) Integer size,
                          @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        // 巡检能看到所有用户的笔记，必须先确认管理员身份
        Auth.requireAdmin(role);
        return adminService.inspectNotes(keyword, page, size);
    }

    /** 标记 / 取消标记违规；只改 flagged 一列，绝不碰标题与正文（需求 4.6.2） */
    @PutMapping("/{id}/flag")
    public Result flag(@PathVariable("id") Long id,
                       @RequestBody(required = false) NoteFlagRequest request,
                       @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                       @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        // 管理员校验；请求体缺省或字段为 null 时传 null，由 service 按 false 处理
        Auth.requireAdmin(role);
        return adminService.flagNote(userId, id, request == null ? null : request.flagged());
    }
}
