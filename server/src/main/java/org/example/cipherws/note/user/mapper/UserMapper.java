package org.example.cipherws.note.user.mapper;

import org.apache.ibatis.annotations.Param;
import org.example.cipherws.note.common.entity.NoteUser;

/**
 * 用户表的数据访问接口。SQL 全部在 resources/mapper/UserMapper.xml 里。
 *
 * 为什么 SQL 放 XML 而不是写在注解里：动态条件（比如可选的关键词、
 * 批量 IN）在 XML 里用 {@code <if>} / {@code <foreach>} 写起来清楚得多，
 * 而且改 SQL 不用重新编译。
 *
 * 关键约定：
 * - 每个参数都用 {@code @Param} 显式命名，XML 里只认名字，不依赖参数顺序；
 * - 方法只做一次数据访问，不在默认方法里拼业务逻辑；
 * - 方法名与 UserMapper.xml 的 statement id 一一对应，改名必须两边同步。
 */
public interface UserMapper {

    /** 按邮箱统计账号数：注册查重与找回密码判断「邮箱是否存在」共用 */
    Long countByEmail(@Param("email") String email);

    /**
     * 登录用：按邮箱查，**会**带出 password 摘要才能比对。
     * 2026-10-20 起登录标识改成了邮箱（email 上有唯一键）。
     */
    NoteUser findByEmail(@Param("email") String email);

    /** 查资料用 */
    NoteUser findById(@Param("id") long id);

    /** 改密码前校验旧密码：只取摘要，不把整行捞出来 */
    String findPasswordById(@Param("id") long id);

    /** 插入后会把自增主键写回实体的 id（XML 里配了 useGeneratedKeys） */
    int insert(NoteUser user);

    /** 更新昵称 / 简介 / 头像三列；邮箱不在这里，换邮箱必须走 updateEmail */
    int updateProfile(@Param("id") long id,
                      @Param("nickname") String nickname,
                      @Param("bio") String bio,
                      @Param("avatar") String avatar);

    /** 换绑邮箱。调用前 Service 已确认验证码通过且该邮箱未被别人占用 */
    int updateEmail(@Param("id") long id, @Param("email") String email);

    /** 只更新头像，不动昵称/简介 —— 上传头像时不该顺带覆盖别的字段 */
    int updateAvatar(@Param("id") long id, @Param("avatar") String avatar);

    /** 换绑前查重：这个邮箱是否已经被**别的**账号占用 */
    Long countByEmailExcluding(@Param("userId") long userId, @Param("email") String email);

    /** 已登录状态下改密码：按 id 更新摘要，入参必须是 BCrypt 编码后的值 */
    int updatePassword(@Param("id") long id, @Param("password") String password);

    /** 找回密码走邮箱，因为此时用户还没登录、拿不到 id */
    int updatePasswordByEmail(@Param("email") String email, @Param("password") String password);

    /** 写一条用户操作日志；调用方负责容错，日志失败不应影响主流程 */
    int insertUserLog(@Param("userId") long userId,
                      @Param("type") String type,
                      @Param("detail") String detail,
                      @Param("ip") String ip);
}
