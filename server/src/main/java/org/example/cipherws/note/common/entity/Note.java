package org.example.cipherws.note.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/** 对应表 {@code note}。deleted=1 表示在回收站，flagged=1 表示被管理员标记违规。 */
@Data
public class Note {
    /** 主键，新增后由数据库回填 */
    private Long id;
    /** 归属用户；查询与写入都要带上它做数据隔离 */
    private Long userId;
    /** 所属文件夹；null 表示未分类 */
    private Long folderId;
    /** 标题，列表页直接展示 */
    private String title;
    /** 正文内容，可能较长 */
    private String content;
    /** 统一 private（草稿/发布已取消） */
    private String status;
    /** 软删除标记：true 表示已进回收站，常规查询要过滤掉 */
    private Boolean deleted;
    /** 违规标记：true 表示被管理员标记，用于后台巡检结果的落地 */
    private Boolean flagged;
    /** 进入回收站的时间；未删除时为 null */
    private LocalDateTime deletedAt;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间，列表排序与增量同步都以它为准 */
    private LocalDateTime updatedAt;
}
