package com.zhikao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.entity.FavoriteFolder;
import com.zhikao.entity.FavoriteItem;
import com.zhikao.mapper.FavoriteFolderMapper;
import com.zhikao.mapper.FavoriteItemMapper;
import com.zhikao.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 收藏夹Service实现类
 */
@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteFolderMapper, FavoriteFolder> implements FavoriteService {

    @Autowired
    private FavoriteFolderMapper favoriteFolderMapper;

    @Autowired
    private FavoriteItemMapper favoriteItemMapper;

    @Override
    public List<FavoriteFolder> listFolders(Long userId) {
        LambdaQueryWrapper<FavoriteFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FavoriteFolder::getUserId, userId)
               .orderByAsc(FavoriteFolder::getSort)
               .orderByDesc(FavoriteFolder::getCreatedAt);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FavoriteFolder createFolder(Long userId, String folderName, String description) {
        FavoriteFolder folder = new FavoriteFolder();
        folder.setUserId(userId);
        folder.setFolderName(folderName);
        folder.setDescription(description);
        folder.setIsPublic(0);
        folder.setSort(0);
        folder.setCreatedAt(new Date());
        folder.setUpdatedAt(new Date());
        save(folder);
        return folder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFolder(Long id, String folderName, String description) {
        FavoriteFolder folder = new FavoriteFolder();
        folder.setId(id);
        folder.setFolderName(folderName);
        folder.setDescription(description);
        folder.setUpdatedAt(new Date());
        updateById(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long id, Long userId) {
        // 删除收藏夹下的所有收藏项
        LambdaQueryWrapper<FavoriteItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(FavoriteItem::getFolderId, id);
        favoriteItemMapper.delete(itemWrapper);
        // 删除收藏夹
        removeById(id);
    }

    @Override
    public IPage<FavoriteItem> listItems(Long userId, Long folderId, long current, long size) {
        LambdaQueryWrapper<FavoriteItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FavoriteItem::getUserId, userId);
        if (folderId != null) {
            wrapper.eq(FavoriteItem::getFolderId, folderId);
        }
        wrapper.orderByDesc(FavoriteItem::getCreatedAt);
        Page<FavoriteItem> page = new Page<>(current, size);
        return favoriteItemMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FavoriteItem addItem(Long userId, Long folderId, Integer itemType,
                                Long itemId, String itemTitle, String itemUrl, String note) {
        FavoriteItem item = new FavoriteItem();
        item.setUserId(userId);
        item.setFolderId(folderId);
        item.setItemType(itemType);
        item.setItemId(itemId);
        item.setItemTitle(itemTitle);
        item.setItemUrl(itemUrl);
        item.setNote(note);
        item.setCreatedAt(new Date());
        favoriteItemMapper.insert(item);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long id, Long userId) {
        LambdaQueryWrapper<FavoriteItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FavoriteItem::getId, id)
               .eq(FavoriteItem::getUserId, userId);
        favoriteItemMapper.delete(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNote(Long id, Long userId, String note) {
        LambdaQueryWrapper<FavoriteItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FavoriteItem::getId, id)
               .eq(FavoriteItem::getUserId, userId);
        FavoriteItem existing = favoriteItemMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setNote(note);
            favoriteItemMapper.updateById(existing);
        }
    }

    @Override
    public Map<String, Object> checkQuestionFavorite(Long userId, Long questionId) {
        LambdaQueryWrapper<FavoriteItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FavoriteItem::getUserId, userId)
               .eq(FavoriteItem::getItemType, 1)
               .eq(FavoriteItem::getItemId, questionId);
        FavoriteItem item = favoriteItemMapper.selectOne(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("isFavorite", item != null);
        if (item != null) {
            result.put("itemId", item.getId());
            result.put("folderId", item.getFolderId());
        }
        return result;
    }
}
