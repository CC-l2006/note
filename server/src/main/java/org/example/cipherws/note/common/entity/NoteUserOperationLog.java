package org.example.cipherws.note.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 对应表 {@code note_user_operation_log}。只增不改不删（需求 4.7.4：日志不可删除）。 */
@Data
public class NoteUserOperationLog {
    /** 主键 */
    private Long id;
    /** 操作人：普通用户 ID */
    private Long userId;
    /** 操作类型：登录 / 新建笔记 / 删除笔记 / AI 总结 … */
    private String type;
    /** 操作详情，记录对象与结果，供日志页面直接展示 */
    private String detail;
    /** 发起请求的客户端 IP */
    private String ip;
    /** 发生时间，日志按它倒序分页 */
    private LocalDateTime createdAt;
}
