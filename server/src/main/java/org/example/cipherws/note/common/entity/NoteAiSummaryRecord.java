package org.example.cipherws.note.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 对应表 {@code note_ai_summary_record}。同一篇笔记可有多条，不加唯一约束。 */
@Data
public class NoteAiSummaryRecord {
    /** 主键 */
    private Long id;
    /** 被总结的笔记 ID；笔记删除后这里仍保留，作为历史记录 */
    private Long noteId;
    /** 发起总结的用户 ID，用于按用户统计 AI 消耗 */
    private Long userId;
    /** 送入模型前的原文摘要片段，便于回溯「模型看到的是什么」 */
    private String sourceSnippet;
    /** 模型返回的总结正文 */
    private String summary;
    /** 本次调用扣除的额度，通常为 1，便于后续调整计费粒度 */
    private Integer quotaCost;
    /** 生成时间 */
    private LocalDateTime createdAt;
}
