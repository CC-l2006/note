package org.example.cipherws.note.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 对应表 {@code note_admin_operation_log}。与用户日志同构，只是操作人换成管理员。 */
@Data
public class NoteAdminOperationLog {
    /** 主键 */
    private Long id;
    /** 操作人：管理员账号 ID，便于事后追责 */
    private Long adminId;
    /** 操作类型：禁用用户 / 启用用户 / 重置 AI 配额 / 巡检笔记 / 修改 AI 配置 / 发布公告 … */
    private String type;
    /** 操作详情，记录被操作对象与变更前后值，供后台日志列表直接展示 */
    private String detail;
    /** 发起请求的客户端 IP，由过滤器或控制器解析后写入 */
    private String ip;
    /** 操作时间，日志按它倒序排列 */
    private LocalDateTime createdAt;
}
