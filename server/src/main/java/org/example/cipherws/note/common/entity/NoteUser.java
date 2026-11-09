package org.example.cipherws.note.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应表 {@code note_user}。
 *
 * 这些实体只做「一行记录」的容器，不含任何业务判断 —— 校验、权限、事务
 * 都在 Service 层。MyBatis 靠 map-underscore-to-camel-case 把 ai_quota
 * 映射到 aiQuota，所以字段名必须与列名的下划线写法一一对应。
 */
@Data
public class NoteUser {
    /** 主键，同时作为 JWT 里的 uid */
    private Long id;
    /** 登录名，全局唯一 */
    private String username;
    /** BCrypt 摘要，绝不参与序列化到前端（VO 里没有这个字段） */
    private String password;
    /** 角色：user 为普通用户，admin 为管理员；权限判断只认这个字段 */
    private String role;
    /** 账号是否可用；false 时登录会被拒绝，已签发的 token 也应在过滤器里失效 */
    private Boolean enabled;
    /** 邮箱，用于找回密码与发送验证码 */
    private String email;
    /** 昵称，展示用，可为空 */
    private String nickname;
    /** 头像地址或文件名，可为空 */
    private String avatar;
    /** 个人简介 */
    private String bio;
    /** 剩余 AI 总结次数；每次调用前校验并原子扣减 */
    private Integer aiQuota;
    /** 注册时间 */
    private LocalDateTime createdAt;
    /** 最后修改时间，改资料、改密码、扣额度后更新 */
    private LocalDateTime updatedAt;
}
