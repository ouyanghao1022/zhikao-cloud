package com.zhikao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.entity.FavoriteFolder;
import com.zhikao.entity.FavoriteItem;

import java.util.List;
import java.util.Map;

/**
 * 收藏夹Service接口
 */
public interface FavoriteService extends IService<FavoriteFolder> {

    /**
     * 获取用户的收藏夹列表
     */
    List<FavoriteFolder> listFolders(Long userId);

    /**
     * 创建收藏夹
     */
    FavoriteFolder createFolder(Long userId, String folderName, String description);

    /**
     * 更新收藏夹
     */
    void updateFolder(Long id, String folderName, String description);

    /**
     * 删除收藏夹（同时删除收藏夹下的所有收藏项）
     */
    void deleteFolder(Long id, Long userId);

    /**
     * 分页获取收藏项列表
     */
    com.baomidou.mybatisplus.core.metadata.IPage<FavoriteItem> listItems(
            Long userId, Long folderId, long current, long size);

    /**
     * 添加收藏
     */
    FavoriteItem addItem(Long userId, Long folderId, Integer itemType,
                         Long itemId, String itemTitle, String itemUrl, String note);

    /**
     * 取消收藏
     */
    void removeItem(Long id, Long userId);

    /**
     * 更新笔记
     */
    void updateNote(Long id, Long userId, String note);

    /**
     * 检查题目是否已收藏
     */
    Map<String, Object> checkQuestionFavorite(Long userId, Long questionId);
}
