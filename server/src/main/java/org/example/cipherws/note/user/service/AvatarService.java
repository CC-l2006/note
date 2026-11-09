package org.example.cipherws.note.user.service;

import org.example.cipherws.note.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 头像上传。
 *
 * 三个校验缺一不可：
 * 1. **大小**：超限直接拒，避免有人传个 50MB 的图把磁盘打满；
 * 2. **魔数**（文件头）：Content-Type 是浏览器自己填的，可以随便伪造，
 *    真正能说明「这是不是图片」的只有文件头；
 * 3. **可解码 + 尺寸**：魔数对得上的文件也可能是坏的，交给 ImageIO 真解一次。
 *
 * 文件名一律由服务端生成（用户 id + 随机串），绝不使用客户端传来的名字 ——
 * 否则一个 "../../application.yml" 就能写到任意路径。
 *
 * 边界：本类只管「文件怎么落盘、怎么删」，不碰用户表 —— 更新 avatar 列由
 * UserService 负责，这样文件存储可以独立替换（本地磁盘 / OSS）而不动业务。
 */
@Service
public class AvatarService {

    // 日志：目录初始化、写盘失败、旧文件清理失败都要留痕，便于排查磁盘与权限问题
    private static final Logger log = LoggerFactory.getLogger(AvatarService.class);

    /** 允许的图片格式，值是落盘扩展名 */
    private static final String EXT_PNG = "png";
    private static final String EXT_JPG = "jpg";
    private static final String EXT_WEBP = "webp";

    /** 头像最大边长，超过的图不一定是攻击，但肯定不是头像 */
    private static final int MAX_SIDE = 4096;

    // 存储位置与容量约束：root 是上传总目录（/uploads 的物理落点），
    // avatarDir 是其中的头像子目录，maxBytes 是单文件上限；三者在构造时定型，运行期不再变
    private final Path root;
    private final Path avatarDir;
    private final long maxBytes;

    /**
     * 构造时就把目录算好并建出来，运行期只管写文件。
     *
     * 配置项 {@code note.upload.dir} / {@code note.upload.max-bytes} 都带默认值，
     * 因此本地不配任何东西也能直接跑。
     *
     * @throws IOException 目录建不出来（权限不足等）时让应用启动失败，
     *                     而不是等到第一次上传才发现，问题能更早暴露
     */
    public AvatarService(@Value("${note.upload.dir:./uploads}") String dir,
                         @Value("${note.upload.max-bytes:2097152}") long maxBytes) throws IOException {
        // toAbsolutePath + normalize：去掉 . 和 ..，后续 startsWith 判断才可靠
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        this.avatarDir = root.resolve("avatar");
        this.maxBytes = maxBytes;
        Files.createDirectories(avatarDir);
        log.info("[note-user] 头像目录 {}（上限 {} 字节）", avatarDir, maxBytes);
    }

    /**
     * 保存头像并返回可直接给前端用的 URL（挂在 /uploads/** 下）。
     *
     * @return 形如 {@code /uploads/avatar/2-9f3c.png}
     */
    public String save(long userId, MultipartFile file) {
        // 第 0 步：空文件直接拒，后面所有解析都建立在「有内容」这个前提上
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        // 校验 1：先看声明大小，超限的请求连字节都不用读，避免大文件占内存和带宽
        if (file.getSize() > maxBytes) {
            throw new BusinessException("图片不能超过 " + (maxBytes / 1024 / 1024) + "MB");
        }

        // 把内容读成 byte[]：后面的魔数判断与解码都基于这份字节
        byte[] bytes;
        try {
            // 先按上限截断读取，避免 getBytes() 把超大文件整个吃进内存
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败，请重试");
        }
        // 再按实际字节数复核一次：getSize() 与真实长度在 chunked 请求下可能不一致
        if (bytes.length == 0 || bytes.length > maxBytes) {
            throw new BusinessException("图片不能超过 " + (maxBytes / 1024 / 1024) + "MB");
        }

        // 校验 2：只看文件头，Content-Type 由浏览器填写、可以随意伪造
        String ext = detectByMagic(bytes);
        if (ext == null) {
            throw new BusinessException("只支持 PNG / JPG / WebP 格式的图片");
        }
        // 校验 3：真解码一次，确认不是「文件头对、内容是垃圾」
        checkDecodable(bytes);

        // 文件名服务端生成：用户 id 便于人工排查，随机串保证不可枚举
        String filename = userId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "." + ext;
        Path target = avatarDir.resolve(filename).normalize();
        // 再确认一次目标仍在头像目录内（防御性写法，正常走不到）
        if (!target.startsWith(avatarDir)) {
            throw new BusinessException("非法的文件名");
        }

        try {
            // 目录已存在，write 直接覆盖/新建；写失败通常是磁盘满或权限问题
            Files.write(target, bytes);
        } catch (IOException e) {
            log.error("写入头像失败 {}", target, e);
            throw new BusinessException("保存图片失败，请稍后重试");
        }
        return "/uploads/avatar/" + filename;
    }

    /**
     * 删除旧头像文件。
     * 只删自己管理的 /uploads/avatar/ 下的文件 —— 用户可能把头像设成外链，
     * 那种 URL 不能去删（也删不到），直接跳过。
     */
    public void deleteQuietly(String url) {
        // 只处理本站上传的头像路径；外链或空值直接放过，既删不到也不该删
        if (url == null || !url.startsWith("/uploads/avatar/")) {
            return;
        }
        String name = url.substring("/uploads/avatar/".length());
        if (name.isBlank() || name.contains("..") || name.contains("/") || name.contains("\\")) {
            return;
        }
        try {
            Files.deleteIfExists(avatarDir.resolve(name));
        } catch (IOException e) {
            // 删不掉不影响上传主流程，最多留个孤儿文件
            log.warn("旧头像删除失败 {}", name, e);
        }
    }

    /** 上传根目录（只读暴露）：WebConfig 用它登记 /uploads/** 的静态资源映射 */
    public Path getRoot() {
        return root;
    }

    /* ------------------------------ 内部实现 ------------------------------ */

    /** 只认文件头，不认 Content-Type */
    private static String detectByMagic(byte[] b) {
        // PNG：固定 8 字节签名 89 50 4E 47 0D 0A 1A 0A
        if (b.length >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A
                && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
            return EXT_PNG;
        }
        // JPEG：FF D8 FF 开头，验前 3 字节已足够与其它格式区分
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return EXT_JPG;
        }
        // WebP：RIFF 容器，第 4-7 字节是文件长度，跳过它们再看 8-11 是否为 WEBP
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return EXT_WEBP;
        }
        // 三种签名都不匹配：返回 null，由调用方统一报「只支持 PNG / JPG / WebP」
        return null;
    }

    /**
     * 真解一次图，确认不是「文件头对但内容是垃圾」。
     * JDK 自带的 ImageIO 不认 WebP，读出来是 null —— 那种情况保留魔数结论，
     * 因为至少它确实是 RIFF/WEBP 容器，浏览器也能显示。
     */
    private static void checkDecodable(byte[] bytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                return;
            }
            if (img.getWidth() < 1 || img.getHeight() < 1) {
                throw new BusinessException("图片内容无效");
            }
            if (img.getWidth() > MAX_SIDE || img.getHeight() > MAX_SIDE) {
                throw new BusinessException("图片尺寸过大，请上传 " + MAX_SIDE + " 像素以内的图片");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("图片内容无法解析，请换一张试试");
        }
    }
}
