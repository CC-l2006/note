package org.example.cipherws.note.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 对应表 {@code note_system_announcement}。enabled=0 的公告普通用户看不到。 */
@Data
public class NoteSystemAnnouncement {
    /** 主键 */
    private Long id;
    /** 公告标题，列表页展示 */
    private String title;
    /** 公告正文 */
    private String content;
    /** 发布人：管理员 ID */
    private Long adminId;
    /** 启用标记：false 表示已下线，普通用户查询时不可见 */
    private Boolean enabled;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 最后修改时间，下线或改标题后更新 */
    private LocalDateTime updatedAt;
}
