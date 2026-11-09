package org.example.cipherws.note.ai.controller;

import org.example.cipherws.note.ai.dto.AiDtos.SummarizeReq;
import org.example.cipherws.note.ai.dto.AiDtos.SummaryVo;
import org.example.cipherws.note.ai.service.AiService;
import org.example.cipherws.note.common.exception.BusinessException;
import org.example.cipherws.note.common.result.Result;
import org.example.cipherws.note.common.security.Auth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 总结接口（需求 4.4）。
 *
 * <p>挂在 {@code /ai} 下，只暴露两个动作：对一篇笔记发起总结、查这篇笔记的历史总结。
 * 这里不做鉴权过滤器的工作，用户身份来自验签过滤器写回的请求头；
 * 取不到就按未登录拒绝，避免匿名请求消耗额度。</p>
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    /** 业务入口：模型调用、额度扣减与摘要落库都在 service 里完成，controller 只做参数兜底 */
    private final AiService aiService;

    /** 构造器注入；字段 final，保证运行期不会被替换成别的实现 */
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /** 发起总结。一次调用消耗 1 次额度（需求 4.4.4） */
    @PostMapping("/summary")
    public Result summarize(
            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
            @RequestBody SummarizeReq req) {
        // 只认 noteId：正文由 service 从库里取，防止前端传来的正文与库中数据不一致
        if (req.noteId() == null) {
            throw new BusinessException("缺少笔记 ID");
        }
        return Result.ok(aiService.summarize(requireUserId(userId), req.noteId()));
    }

    /** 某篇笔记的历史总结（需求 4.4.3） */
    @GetMapping("/summary")
    public Result history(
            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
            @RequestParam Long noteId) {
        // 摘要属于小集合，总数直接用列表长度，不再额外跑一次 COUNT
        List<SummaryVo> list = aiService.history(requireUserId(userId), noteId);
        return Result.ok(list, (long) list.size());
    }

    /**
     * 从请求头取当前用户 ID。
     * 这里再兜一次：即使过滤器漏配，也不能把 null 当用户去扣额度。
     */
    private long requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException("未登录，请先登录");
        }
        return userId;
    }
}
