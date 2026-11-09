package org.example.cipherws.note.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 对应表 {@code note_ai_config}。约定 id=1 那一行生效；apiKey 绝不能回传前端。 */
@Data
public class NoteAiConfig {
    /** 主键；业务上只读取 id=1 的一行，其余行视为历史配置 */
    private Long id;
    /** 模型服务地址，形如 https://host/v1/chat/completions */
    private String endpoint;
    /** 调用凭证；只在服务端使用，任何返回给前端的 VO 都不能带出去 */
    private String apiKey;
    /** 模型名，例如 deepseek-chat */
    private String model;
    /** 单次总结允许送入模型的最大字符数，用于控制费用与超时 */
    private Integer maxChars;
    /** 最后一次修改配置的管理员 ID */
    private Long updatedBy;
    /** 最后修改时间，后台展示配置新鲜度用 */
    private LocalDateTime updatedAt;
}
