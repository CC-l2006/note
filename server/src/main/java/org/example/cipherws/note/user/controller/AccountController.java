package org.example.cipherws.note.user.controller;

import org.example.cipherws.note.common.exception.BusinessException;
import org.example.cipherws.note.common.result.Result;
import org.example.cipherws.note.common.security.Auth;
import org.example.cipherws.note.user.dto.UserDtos.BindEmailReq;
import org.example.cipherws.note.user.dto.UserDtos.CodeOnlyReq;
import org.example.cipherws.note.user.dto.UserDtos.LoginReq;
import org.example.cipherws.note.user.dto.UserDtos.LoginRes;
import org.example.cipherws.note.user.dto.UserDtos.PasswordReq;
import org.example.cipherws.note.user.dto.UserDtos.ProfileReq;
import org.example.cipherws.note.user.dto.UserDtos.RegisterReq;
import org.example.cipherws.note.user.dto.UserDtos.ResetPasswordReq;
import org.example.cipherws.note.user.dto.UserDtos.SendCodeReq;
import org.example.cipherws.note.user.dto.UserDtos.SendCodeRes;
import org.example.cipherws.note.user.dto.UserDtos.UserVo;
import org.example.cipherws.note.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 认证与用户中心。
 *
 * 没有类级 @RequestMapping，因为这里同时覆盖 /auth/**（白名单）
 * 和 /user/**（需要登录）两组路径 —— 前者不能要求 token，后者必须要求，
 * 分成两个 controller 只是多一个文件，写在一起反而更清楚边界在哪。
 *
 * 关于验证码的两个入口：
 * - {@code POST /auth/send-code}  找回密码用，不需要登录（那时还没登录）；
 * - {@code POST /user/email/code} 绑定邮箱用，**必须登录**。
 * 之所以不合成一个接口加个 purpose 参数：/auth/send-code 在 JwtAuthFilter 白名单里，
 * 白名单分支不写身份头，拿不到 userId 就没法校验「这个邮箱是不是别人的」。
 * 走两条路径，鉴权边界就天然分开了。
 *
 * 职责边界：本类只做三件事 —— 声明路由、从请求头取登录身份、把结果包成 {@link Result}。
 * 参数含义校验、验证码、密码摘要、令牌签发一律在 {@link UserService} 里，
 * 这样换 Web 框架或加一层网关都不会影响业务规则。
 */
@RestController
public class AccountController {

    // 全部业务委托给它：controller 里不出现任何 SQL、加密或验证码逻辑
    private final UserService userService;

    // 构造器注入：单依赖，直接赋值即可，无需 @Autowired
    public AccountController(UserService userService) {
        this.userService = userService;
    }

    /* ----------------------------- /auth/** ----------------------------- */

    /** 注册第二步：邮箱 + 验证码 + 密码（+ 可选昵称）。验证码要先调 /auth/send-code */
    @PostMapping("/auth/register")
    public Result register(@RequestBody RegisterReq req) {
        return Result.ok(userService.register(req.email(), req.code(), req.password(), req.nickname()));
    }

    /** 登录：用邮箱作为账号 */
    @PostMapping("/auth/login")
    public Result login(@RequestBody LoginReq req) {
        // 服务端不保存会话，只把签好的 JWT 交给前端保管
        LoginRes res = userService.login(req.email(), req.password());
        return Result.ok(res);
    }

    /**
     * 退出登录。
     *
     * JWT 是无状态的，服务端没有会话可清 —— 真正的「退出」是前端丢掉 token。
     * 这里保留接口是为了让前端的调用链完整，将来若要支持「强制下线」，
     * 需要引入 token 黑名单（Redis）再在这里实现。
     */
    @PostMapping("/auth/logout")
    public Result logout() {
        return Result.ok(true);
    }

    /**
     * 发验证码（不需要登录）。
     *
     * purpose 决定校验规则：
     *   register（注册）—— 邮箱必须**未**注册
     *   reset（找回密码，缺省）—— 邮箱必须已注册
     * 绑定邮箱不在这里，它需要登录态，走下面的 /user/email/code。
     */
    @PostMapping("/auth/send-code")
    public Result sendCode(@RequestBody SendCodeReq req) {
        // 第一个参数传 null：本接口在 JWT 白名单里，此时还没有登录身份可用
        SendCodeRes res = userService.issueCode(null, req.email(), req.purpose());
        return Result.ok(res);
    }

    /**
     * 找回密码第二步：邮箱 + 验证码 + 新密码。
     *
     * 不要求登录 —— 忘记密码的人本来就拿不到令牌，安全边界由「邮箱验证码」承担。
     */
    @PostMapping("/auth/reset-password")
    public Result resetPassword(@RequestBody ResetPasswordReq req) {
        userService.resetPassword(req.email(), req.code(), req.password());
        return Result.ok(true);
    }

    /* ----------------------------- /user/** ----------------------------- */

    /**
     * 读当前登录用户资料。
     *
     * 只返回 VO，password 摘要在实体里，转换时就被丢掉了。
     */
    @GetMapping("/user/profile")
    public Result profile(@RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId) {
        return Result.ok(userService.profile(requireUserId(userId)));
    }

    /** 改昵称 / 简介 / 头像。**邮箱不在这里**，要走 /user/email 并验码 */
    @PutMapping("/user/profile")
    public Result updateProfile(
            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
            @RequestBody ProfileReq req) {
        UserVo vo = userService.updateProfile(
                requireUserId(userId), req.nickname(), req.bio(), req.avatar());
        return Result.ok(vo);
    }

    /**
     * 更换绑定邮箱第 1 步：给**当前邮箱**发验证码。
     * 请求体不需要传 email —— 服务端直接取库里的当前邮箱，避免被伪造。
     */
    @PostMapping("/user/email/code")
    public Result sendEmailCode(
            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
            @RequestBody SendCodeReq req) {
        // purpose 只认 bind_old / bind_new；不传时按「给新邮箱发」处理（兼容旧前端）
        String purpose = req.purpose() == null ? "bind_new" : req.purpose();
        SendCodeRes res = userService.issueCode(requireUserId(userId), req.email(), purpose);
        return Result.ok(res);
    }

    /**
     * 更换绑定邮箱第 1 步的校验：确认旧邮箱的验证码。
     * 通过后 10 分钟内才能走第 2 步，这是「先验旧、再验新」的关键一环。
     */
    @PostMapping("/user/email/verify-old")
    public Result verifyOldEmail(
            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
            @RequestBody CodeOnlyReq req) {
        userService.verifyOldEmail(requireUserId(userId), req.code());
        return Result.ok(true);
    }

    /** 更换绑定邮箱第 2 步：新邮箱 + 新邮箱收到的验证码 */
    @PutMapping("/user/email")
    public Result bindEmail(
            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
            @RequestBody BindEmailReq req) {
        // 服务端会重新校验新邮箱的验证码，并检查第 1 步（旧邮箱）是否仍在有效期内
        UserVo vo = userService.bindEmail(requireUserId(userId), req.email(), req.code());
        return Result.ok(vo);
    }

    /**
     * 上传头像（multipart/form-data，字段名 file）。
     * 大小与格式在 AvatarService 里校验，这里只负责把文件转给 service。
     */
    @PostMapping("/user/avatar")
    public Result uploadAvatar(
            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
            @RequestParam("file") MultipartFile file) {
        UserVo vo = userService.uploadAvatar(requireUserId(userId), file);
        return Result.ok(vo);
    }

    /** 修改密码第一步：给当前邮箱发验证码（需要登录） */
    @PostMapping("/user/password/code")
    public Result sendPasswordCode(
            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId) {
        SendCodeRes res = userService.issueCode(requireUserId(userId), null, "password");
        return Result.ok(res);
    }

    /** 修改密码第二步：旧密码 + 新密码 + 邮箱验证码 */
    @PutMapping("/user/password")
    public Result changePassword(
            @RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId,
            @RequestBody PasswordReq req) {
        userService.changePassword(
                requireUserId(userId), req.oldPassword(), req.newPassword(), req.code());
        return Result.ok(true);
    }

    /** 前端侧栏要显示「剩余 AI 额度」，单独给一个轻量接口，不必拉整个 profile */
    @GetMapping("/user/quota")
    public Result quota(@RequestHeader(value = Auth.HEADER_USER_ID, required = false) Long userId) {
        // 侧栏会频繁轮询，这里只从 VO 里取一个字段，避免把整份资料来回传
        UserVo vo = userService.profile(requireUserId(userId));
        return Result.ok(java.util.Map.of("aiQuota", vo.aiQuota()));
    }

    /**
     * 正常流程下 JwtAuthFilter 已经拦过未登录请求，这里再判一次是**纵深防御**：
     * 万一过滤器没生效（比如测试里直接调 controller），也不能让接口裸奔。
     */
    private long requireUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException("未登录，请先登录");
        }
        return userId;
    }
}
