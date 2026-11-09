package org.example.cipherws.note.ai.dto;

/** AI 服务的数据传输对象 */
public final class AiDtos {

    /** 纯类型容器，只用来挂 record，禁止实例化 */
    private AiDtos() {
    }

    /** 发起总结。前端只传笔记 id，正文由服务端自己取，避免前端传来的正文与库里不一致 */
    public record SummarizeReq(Long noteId) {
    }

    /** 一条总结记录。quotaLeft 是本次扣减后的剩余额度，前端用它刷新侧栏 */
    public record SummaryVo(
            Long id,
            Long noteId,
            String summary,
            String sourceSnippet,
            Integer quotaCost,
            Integer quotaLeft,
            String createdAt) {
    }
}
