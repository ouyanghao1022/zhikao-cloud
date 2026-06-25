package com.zhikao.controller;

import com.zhikao.common.PageRequest;
import com.zhikao.common.PageResult;
import com.zhikao.common.Result;
import com.zhikao.entity.FavoriteFolder;
import com.zhikao.entity.FavoriteItem;
import com.zhikao.service.FavoriteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 收藏夹控制器
 */
@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    /**
     * 获取收藏夹列表
     */
    @GetMapping("/folders")
    public Result<List<FavoriteFolder>> listFolders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(favoriteService.listFolders(userId));
    }

    /**
     * 创建收藏夹
     */
    @PostMapping("/folder")
    public Result<?> createFolder(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String folderName = body.get("folderName");
        String description = body.get("description");
        FavoriteFolder folder = favoriteService.createFolder(userId, folderName, description);
        return Result.success("创建成功", folder);
    }

    /**
     * 更新收藏夹
     */
    @PutMapping("/folder/{id}")
    public Result<?> updateFolder(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String folderName = body.get("folderName");
        String description = body.get("description");
        favoriteService.updateFolder(id, folderName, description);
        return Result.success("更新成功");
    }

    /**
     * 删除收藏夹
     */
    @DeleteMapping("/folder/{id}")
    public Result<?> deleteFolder(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        favoriteService.deleteFolder(id, userId);
        return Result.success("删除成功");
    }

    /**
     * 获取收藏项列表
     */
    @GetMapping("/items")
    public Result<PageResult<FavoriteItem>> listItems(
            PageRequest pageRequest,
            @RequestParam(required = false) Long folderId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var page = favoriteService.listItems(userId, folderId,
                pageRequest.getCurrent(), pageRequest.getSize());
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(),
                page.getSize(), page.getCurrent()));
    }

    /**
     * 添加收藏
     */
    @PostMapping("/item")
    public Result<?> addItem(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long folderId = Long.valueOf(body.get("folderId").toString());
        Integer itemType = Integer.valueOf(body.get("itemType").toString());
        Long itemId = body.containsKey("itemId") && body.get("itemId") != null
                ? Long.valueOf(body.get("itemId").toString()) : null;
        String itemTitle = (String) body.get("itemTitle");
        String itemUrl = (String) body.get("itemUrl");
        String note = (String) body.get("note");

        FavoriteItem item = favoriteService.addItem(userId, folderId, itemType,
                itemId, itemTitle, itemUrl, note);
        return Result.success("收藏成功", item);
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/item/{id}")
    public Result<?> removeItem(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        favoriteService.removeItem(id, userId);
        return Result.success("取消收藏成功");
    }

    /**
     * 更新收藏笔记
     */
    @PutMapping("/item/{id}/note")
    public Result<?> updateNote(@PathVariable Long id, @RequestBody Map<String, String> body,
                                HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String note = body.get("note");
        favoriteService.updateNote(id, userId, note);
        return Result.success("更新笔记成功");
    }

    /**
     * 检查题目是否已收藏
     */
    @GetMapping("/question/{questionId}")
    public Result<Map<String, Object>> checkQuestionFavorite(
            @PathVariable Long questionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(favoriteService.checkQuestionFavorite(userId, questionId));
    }
}
