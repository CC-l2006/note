package org.example.cipherws.note.user.service;

/**
 * 邮件发送。
 *
 * 存在两个实现路径（同一个类里切换，不拆两个 bean）：
 * - 配了 SMTP（note.mail.enabled=true 且 MAIL_HOST/MAIL_USERNAME 非空）→ 真发邮件；
 * - 没配 → 把验证码打到日志里，本地开发不需要真的去申请邮箱授权码。
 *
 * 这样「找回密码 / 绑定邮箱」两条流程在任何环境都能跑通，差别只是
 * 验证码从邮件来还是从日志来。
 *
 * 关键约定：实现方不得吞掉发送异常 —— 失败要抛给 UserService，
 * 由它回滚「已发送」状态并给出可读的提示；重试与频控也不在本层做。
 */
public interface MailService {

    /** 当前是否真的会发信（false 表示走日志回退） */
    boolean isEnabled();

    /**
     * 发送验证码邮件。
     *
     * @param to     收件邮箱
     * @param code   6 位验证码
     * @param purpose reset（找回密码）/ bind（绑定邮箱），只用于邮件正文措辞
     */
    void sendCode(String to, String code, String purpose);
}
