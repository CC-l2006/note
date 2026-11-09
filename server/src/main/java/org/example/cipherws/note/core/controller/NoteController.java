package org.example.cipherws.note.core.controller;

import jakarta.annotation.Resource;
import org.example.cipherws.note.common.exception.BusinessException;
import org.example.cipherws.note.common.result.Result;
import org.example.cipherws.note.common.security.Auth;
import org.example.cipherws.note.core.dto.NoteDtos.MoveNoteReq;
import org.example.cipherws.note.core.dto.NoteDtos.NoteSaveRequest;
import org.example.cipherws.note.core.service.NoteService;
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

import java.util.List;

/**
 * 笔记接口：路径与入参和 frontend/note/src/api/index.js 的 noteApi 一一对应。
 * 前端 / Nginx 已去掉 /api 前缀，所以这里直接映射 /notes。
 *
 * <p>身份只信任 {@code JwtAuthFilter} 写回的请求头。X-User-Role 也一并读出，但本模块没有管理员接口，
 * 用不到它 —— 管理员巡检在 admin 包。</p>
 */
@RestController
@RequestMapping("/notes")
public class NoteController {

    /** 笔记业务入口：分页归一、动态筛选、软删除判定都收在 Service 一层 */
    @Resource
    private NoteService noteService;

    /** 列表：关键词 / 文件夹 / 标签 / 未分类四个条件均可选，翻页参数由 Service 归一 */
    @GetMapping
    public Result listNotes(@RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "folderId", required = false) Long folderId,
                            @RequestParam(value = "tagIds", required = false) List<Long> tagIds,
                            @RequestParam(value = "unfiled", defaultValue = "false") boolean unfiled,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                            @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return noteService.listNotes(requireLogin(userId), keyword, folderId, tagIds, unfiled, page, size);
    }

    /** 回收站列表：只含已软删除的笔记，其余筛选条件一律不生效 */
    @GetMapping("/trash")
    public Result listTrashNotes(@RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "10") int size,
                                 @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                                 @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return noteService.listTrashNotes(requireLogin(userId), page, size);
    }

    /** 清空回收站：物理删除，删完不可恢复 */
    @DeleteMapping("/trash")
    public Result clearTrash(@RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                             @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return noteService.clearTrash(requireLogin(userId));
    }

    /** 详情：返回笔记本体，并附带该笔记的 AI 总结历史 */
    @GetMapping("/{id}")
    public Result getNote(@PathVariable("id") long id,
                          @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                          @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return noteService.getNote(requireLogin(userId), id);
    }

    /** 新建：标题兜底、状态固定私有、标签一起绑定，全部在 Service 里完成 */
    @PostMapping
    public Result createNote(@RequestBody NoteSaveRequest request,
                             @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                             @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return noteService.createNote(requireLogin(userId), request);
    }

    /** 更新：字段为 null 表示「这一项不改」，tagIds 为 null 表示不动标签 */
    @PutMapping("/{id}")
    public Result updateNote(@PathVariable("id") long id,
                             @RequestBody NoteSaveRequest request,
                             @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                             @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return noteService.updateNote(requireLogin(userId), id, request);
    }

    /** 把笔记放进文件夹（folderId 为 null 表示移出到未分类），不覆盖正文和标签 */
    @PostMapping("/{id}/move")
    public Result moveNote(@PathVariable("id") long id,
                           @RequestBody(required = false) MoveNoteReq request,
                           @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                           @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return noteService.moveToFolder(
                requireLogin(userId), id, request == null ? null : request.folderId());
    }

    /** 删除：hard=false 只进回收站，hard=true 永久删除；默认参数保证老前端不传也能用 */
    @DeleteMapping("/{id}")
    public Result removeNote(@PathVariable("id") long id,
                             @RequestParam(value = "hard", defaultValue = "false") boolean hard,
                             @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                             @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return noteService.removeNote(requireLogin(userId), id, hard);
    }

    /** 从回收站恢复：deleted 归零并清空进站时间 */
    @PostMapping("/{id}/restore")
    public Result restoreNote(@PathVariable("id") long id,
                              @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                              @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return noteService.restoreNote(requireLogin(userId), id);
    }

    /** 没带 X-User-Id 说明请求没经过验签过滤器，一律按未登录处理 */
    private static long requireLogin(Long userId) {
        if (userId == null) {
            throw new BusinessException("未登录，请先登录");
        }
        return userId;
    }
}
