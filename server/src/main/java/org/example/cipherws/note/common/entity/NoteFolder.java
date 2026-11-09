package org.example.cipherws.note.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应表 {@code note_folder}。
 *
 * <p>文件夹本身不直接关联笔记，而是绑定若干标签；带有其中任意标签的笔记
 * 会自动归入这个文件夹。唯一键是 (user_id, name)。</p>
 */
@Data
public class NoteFolder {
    /** 主键 */
    private Long id;
    /** 所属用户；文件夹是私有资源，跨用户不可见 */
    private Long userId;
    /** 文件夹名；同一用户下不允许重名 */
    private String name;
    /** 创建时间 */
    private LocalDateTime createdAt;
}
