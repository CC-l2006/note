package org.example.cipherws.note.core.controller;

import jakarta.annotation.Resource;
import org.example.cipherws.note.common.exception.BusinessException;
import org.example.cipherws.note.common.result.Result;
import org.example.cipherws.note.common.security.Auth;
import org.example.cipherws.note.core.dto.NoteDtos.TagSaveRequest;
import org.example.cipherws.note.core.service.TagService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 标签接口：路径与入参和 frontend/note/src/api/index.js 的 tagApi 一一对应。
 * 前端 / Nginx 已去掉 /api 前缀，所以这里直接映射 /tags。
 */
@RestController
@RequestMapping("/tags")
public class TagController {

    /** 标签业务入口：名称校验、归属校验、重名文案都在 Service 一层 */
    @Resource
    private TagService tagService;

    /** 列表：每个标签附带当前用户未删除笔记的数量 */
    @GetMapping
    public Result listTags(@RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                           @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return tagService.listTags(requireLogin(userId));
    }

    /** 新建标签；重名交给唯一键 (user_id, name) 判定后转成业务提示 */
    @PostMapping
    public Result createTag(@RequestBody TagSaveRequest request,
                            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                            @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return tagService.createTag(requireLogin(userId), request);
    }

    /** 重命名标签：只能改名字，归属校验在 Service 里先做 */
    @PutMapping("/{id}")
    public Result renameTag(@PathVariable("id") long id,
                            @RequestBody TagSaveRequest request,
                            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                            @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return tagService.renameTag(requireLogin(userId), id, request);
    }

    /** 删除标签：只删标签本身，关联行由外键级联清掉，笔记一篇都不动 */
    @DeleteMapping("/{id}")
    public Result removeTag(@PathVariable("id") long id,
                            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
                            @RequestHeader(value = Auth.HEADER_USER_ROLE, required = false) String role) {
        return tagService.removeTag(requireLogin(userId), id);
    }

    /** 没带 X-User-Id 说明请求没经过验签过滤器，一律按未登录处理 */
    private static long requireLogin(Long userId) {
        if (userId == null) {
            throw new BusinessException("未登录，请先登录");
        }
        return userId;
    }
}
