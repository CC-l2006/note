package org.example.cipherws.note.core.service.impl;

import org.example.cipherws.note.common.entity.NoteFolder;
import org.example.cipherws.note.common.exception.BusinessException;
import org.example.cipherws.note.common.result.Result;
import org.example.cipherws.note.core.dto.NoteDtos.FolderSaveRequest;
import org.example.cipherws.note.core.dto.NoteDtos.FolderVo;
import org.example.cipherws.note.core.mapper.FolderMapper;
import org.example.cipherws.note.core.service.FolderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件夹业务实现。SQL 全部在 FolderMapper.xml。
 *
 * <p>文件夹不再绑定标签；笔记通过 note.folder_id 直接归入。
 * 「自动整理」只处理未分类笔记，按「文件夹名 = 标签名」匹配。</p>
 */
@Service
public class FolderServiceImpl implements FolderService {

    /** note_folder.name 是 VARCHAR(20) */
    private static final int MAX_NAME_LENGTH = 20;

    /** 文件夹数据访问入口，SQL 全在 FolderMapper.xml */
    private final FolderMapper folderMapper;

    /** 构造器注入：依赖不可变，单测里直接传假 Mapper 即可 */
    public FolderServiceImpl(FolderMapper folderMapper) {
        this.folderMapper = folderMapper;
    }

    /**
     * 列表：文件夹与笔记数各查一次，在内存里按 folder_id 拼装，
     * 避免为每个文件夹再查一次计数（N+1）。
     */
    @Override
    public Result listFolders(long userId) {
        List<FolderVo> folders = loadFolders(userId);
        return Result.ok(folders, (long) folders.size());
    }

    /** 新建：名称先归一，插入后统一走 loadFolders 回查，返回带 noteCount 的视图 */
    @Override
    @Transactional
    public Result createFolder(long userId, FolderSaveRequest request) {
        String name = normalizeName(request == null ? null : request.name());
        NoteFolder folder = new NoteFolder();
        folder.setUserId(userId);
        folder.setName(name);
        try {
            folderMapper.insertFolder(folder);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("同名文件夹已存在");
        }
        return Result.ok(findFolder(userId, folder.getId()));
    }

    /** 改名：先确认文件夹属于当前用户，再归一名称并更新 */
    @Override
    @Transactional
    public Result updateFolder(long userId, long folderId, FolderSaveRequest request) {
        requireOwned(userId, folderId);
        String name = normalizeName(request == null ? null : request.name());
        try {
            folderMapper.updateFolderName(folderId, userId, name);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("同名文件夹已存在");
        }
        return Result.ok(findFolder(userId, folderId));
    }

    /** 删除：先把笔记退回未分类，再删文件夹本身；两步同一事务，不会出现悬空的 folder_id */
    @Override
    @Transactional
    public Result removeFolder(long userId, long folderId) {
        requireOwned(userId, folderId);
        // 笔记不删，只退回「未分类」
        folderMapper.clearFolderNotes(folderId, userId);
        folderMapper.deleteFolder(folderId, userId);
        return Result.ok(true);
    }

    /** 自动整理：归类规则整段写进一条 UPDATE，这里只透传影响行数给前端展示 */
    @Override
    @Transactional
    public Result autoOrganize(long userId) {
        int moved = folderMapper.autoOrganize(userId);
        return Result.ok(moved);
    }

    /* ------------------------------ 内部实现 ------------------------------ */

    /** 文件夹列表的唯一装配点：两份 Map 结果在这里合并成 FolderVo，列表、新建、改名共用 */
    private List<FolderVo> loadFolders(long userId) {
        List<Map<String, Object>> rows = folderMapper.selectFolders(userId);

        // 先把「文件夹 -> 笔记数」铺平成 Map，后面按 id 直接取，省掉嵌套遍历
        Map<Long, Long> countByFolder = new HashMap<>();
        for (Map<String, Object> row : folderMapper.countNotesByFolder(userId)) {
            Long folderId = toLong(row.get("folder_id"));
            Long count = toLong(row.get("note_count"));
            if (folderId != null) {
                countByFolder.put(folderId, count == null ? 0L : count);
            }
        }

        // 再按查询顺序逐个拼 VO；没有主键的脏行直接跳过，避免前端拿到 null id 无法操作
        List<FolderVo> folders = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Long id = toLong(row.get("id"));
            if (id == null) {
                continue;
            }
            folders.add(new FolderVo(
                    id,
                    (String) row.get("name"),
                    countByFolder.getOrDefault(id, 0L)));
        }
        return folders;
    }

    /** 从当前用户的列表中定位刚写入的那一项，复用同一套装配逻辑，不另写一条按 id 查询 */
    private FolderVo findFolder(long userId, long folderId) {
        return loadFolders(userId).stream()
                .filter(folder -> folder.id() != null && folder.id() == folderId)
                .findFirst()
                .orElseThrow(() -> new BusinessException("文件夹不存在"));
    }

    /** 归属校验：不是自己的文件夹一律按「不存在」处理，不向外泄露该 id 是否真实存在 */
    private void requireOwned(long userId, long folderId) {
        if (folderMapper.countOwnedFolder(folderId, userId) == 0) {
            throw new BusinessException("文件夹不存在");
        }
    }

    /** 名称归一：去首尾空格后不能为空，并按码点限制 20 字符（emoji 不会被算成两个） */
    private static String normalizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException("文件夹名不能为空");
        }
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_NAME_LENGTH) {
            throw new BusinessException("文件夹名最多 20 个字符");
        }
        return trimmed;
    }

    /** Map 结果里的数字列可能是 Long / Integer / BigInteger，统一按 Number 取 long */
    private static Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
