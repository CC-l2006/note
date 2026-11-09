package org.example.cipherws.note.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 对应表 {@code note_tag}。标签属于用户私有，唯一键是 (user_id, name)。 */
@Data
public class NoteTag {
    /** 主键 */
    private Long id;
    /** 所属用户；标签不跨用户共享 */
    private Long userId;
    /** 标签名；同一用户下不允许重名 */
    private String name;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
