package com.zhikao.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhikao.common.Result;
import com.zhikao.entity.ClassEntity;
import com.zhikao.entity.ClassMember;
import com.zhikao.entity.QuestionCategory;
import com.zhikao.mapper.ClassMapper;
import com.zhikao.mapper.ClassMemberMapper;
import com.zhikao.mapper.QuestionCategoryMapper;
import com.zhikao.mapper.QuestionMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 题库分类控制器
 */
@RestController
@RequestMapping("/question/category")
public class QuestionCategoryController {

    @Autowired
    private QuestionCategoryMapper categoryMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ClassMemberMapper classMemberMapper;

    @Autowired
    private ClassMapper classMapper;

    /**
     * 获取分类树（教师只能看到自己创建的分类）
     */
    @GetMapping("/tree")
    public Result<List<QuestionCategory>> tree(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        LambdaQueryWrapper<QuestionCategory> queryWrapper = new LambdaQueryWrapper<QuestionCategory>()
                .eq(QuestionCategory::getStatus, 1)
                .orderByAsc(QuestionCategory::getSort)
                .orderByAsc(QuestionCategory::getId);

        // 非管理员只看自己创建的分类
        if (!"SUPER_ADMIN".equals(role)) {
            queryWrapper.eq(QuestionCategory::getCreatorId, userId);
        }

        List<QuestionCategory> all = categoryMapper.selectList(queryWrapper);

        Map<Long, List<QuestionCategory>> parentMap = all.stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c.getParentId() != null ? c.getParentId() : 0L));

        List<QuestionCategory> roots = parentMap.getOrDefault(0L, new ArrayList<>());
        for (QuestionCategory root : roots) {
            buildChildren(root, parentMap);
        }

        return Result.success("查询成功", roots);
    }

    /**
     * 创建分类（教师/管理员）
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @PostMapping("/create")
    public Result<?> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String categoryName = (String) body.get("categoryName");
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return Result.error("分类名称不能为空");
        }
        Long parentId = body.containsKey("parentId") ? Long.valueOf(body.get("parentId").toString()) : 0L;
        Long userId = (Long) request.getAttribute("userId");

        QuestionCategory category = new QuestionCategory();
        category.setCategoryName(categoryName.trim());
        category.setParentId(parentId);
        category.setCategoryType(parentId == 0 ? 1 : 2);
        category.setSort(0);
        category.setStatus(1);
        category.setCreatorId(userId);
        category.setCreatedAt(new Date());
        category.setUpdatedAt(new Date());
        categoryMapper.insert(category);
        return Result.success("创建成功", category.getId());
    }

    /**
     * 删除分类（仅当该分类下无题目时允许删除）
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        // 检查是否有题目使用此分类
        Long count = questionMapper.selectCount(
                new LambdaQueryWrapper<com.zhikao.entity.Question>()
                        .eq(com.zhikao.entity.Question::getCategoryId, id)
        );
        if (count != null && count > 0) {
            return Result.error("该目录下还有 " + count + " 道题目，请先移走题目再删除");
        }
        // 检查是否有子分类
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<QuestionCategory>()
                        .eq(QuestionCategory::getParentId, id)
                        .eq(QuestionCategory::getStatus, 1)
        );
        if (childCount != null && childCount > 0) {
            return Result.error("该目录下还有子目录，请先删除子目录");
        }
        categoryMapper.deleteById(id);
        return Result.success("删除成功");
    }

    /**
     * 切换题库分类练习模式开关
     * 同步更新该分类下所有题目的 allowPractice 字段
     */
    @PreAuthorize("hasAnyRole('TEACHER', 'SUPER_ADMIN')")
    @PutMapping("/{id}/toggle-practice")
    public Result<?> togglePractice(@PathVariable Long id) {
        QuestionCategory category = categoryMapper.selectById(id);
        if (category == null) return Result.error("分类不存在");
        int newVal = (category.getAllowPractice() != null && category.getAllowPractice() == 1) ? 0 : 1;
        category.setAllowPractice(newVal);
        category.setUpdatedAt(new Date());
        categoryMapper.updateById(category);

        // 同步更新该分类下所有题目的 allowPractice 字段
        List<com.zhikao.entity.Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<com.zhikao.entity.Question>()
                        .eq(com.zhikao.entity.Question::getCategoryId, id));
        for (com.zhikao.entity.Question q : questions) {
            q.setAllowPractice(newVal);
            q.setUpdatedAt(new Date());
            questionMapper.updateById(q);
        }

        return Result.success(newVal == 1 ? "已开启练习模式（同步更新 " + questions.size() + " 道题目）" : "已关闭练习模式（同步更新 " + questions.size() + " 道题目）", newVal);
    }

    /**
     * 获取可练习的分类树（学生端）
     * 学生只能看到所属班级教师创建的分类（教师数据隔离）。
     * 如果学生无班级，则回退到显示全部（向后兼容）。
     */
    @GetMapping("/practice-tree")
    public Result<List<QuestionCategory>> practiceTree(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        LambdaQueryWrapper<QuestionCategory> wrapper = new LambdaQueryWrapper<QuestionCategory>()
                .eq(QuestionCategory::getStatus, 1)
                .eq(QuestionCategory::getAllowPractice, 1)
                .orderByAsc(QuestionCategory::getSort)
                .orderByAsc(QuestionCategory::getId);

        // 教师数据隔离：学生只能看到所属班级教师创建的分类
        if ("STUDENT".equals(role)) {
            Set<Long> teacherIds = getStudentTeacherIds(userId);
            if (teacherIds.isEmpty()) {
                // 学生未加入任何班级，无权限查看任何练习题库
                return Result.success(Collections.emptyList());
            }
            wrapper.in(QuestionCategory::getCreatorId, teacherIds);
        }

        List<QuestionCategory> all = categoryMapper.selectList(wrapper);

        // 过滤掉没有题目的叶子分类（避免学生点进去看到空列表）
        Set<Long> categoryIdsWithQuestions = new HashSet<>();
        for (QuestionCategory c : all) {
            Long cnt = questionMapper.selectCount(
                    new LambdaQueryWrapper<com.zhikao.entity.Question>()
                            .eq(com.zhikao.entity.Question::getStatus, 1)
                            .eq(com.zhikao.entity.Question::getAllowPractice, 1)
                            .eq(com.zhikao.entity.Question::getCategoryId, c.getId()));
            if (cnt != null && cnt > 0) {
                categoryIdsWithQuestions.add(c.getId());
            }
        }

        // 保留：自身有题目的分类，或子分类有题目的分类
        Set<Long> keepIds = new HashSet<>(categoryIdsWithQuestions);
        // 向上传播：所有有题目的分类的祖先也保留
        Map<Long, QuestionCategory> idMap = new HashMap<>();
        for (QuestionCategory c : all) idMap.put(c.getId(), c);
        for (Long id : categoryIdsWithQuestions) {
            Long pid = idMap.get(id).getParentId();
            while (pid != null && pid > 0 && idMap.containsKey(pid)) {
                keepIds.add(pid);
                pid = idMap.get(pid).getParentId();
            }
        }

        // 最终保留的分类
        List<QuestionCategory> filtered = new ArrayList<>();
        for (QuestionCategory c : all) {
            if (keepIds.contains(c.getId())) filtered.add(c);
        }

        Map<Long, List<QuestionCategory>> parentMap = filtered.stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c.getParentId() != null ? c.getParentId() : 0L));

        List<QuestionCategory> roots = parentMap.getOrDefault(0L, new ArrayList<>());
        for (QuestionCategory root : roots) {
            buildChildren(root, parentMap);
        }
        return Result.success(roots);
    }

    /**
     * 查询学生所属班级的所有教师ID，用于教师数据隔离。
     * @param userId 学生用户ID
     * @return 教师ID集合（可能为空）
     */
    private Set<Long> getStudentTeacherIds(Long userId) {
        // 1. 查询学生所有有效班级成员记录
        LambdaQueryWrapper<ClassMember> memberWrapper = new LambdaQueryWrapper<ClassMember>()
                .eq(ClassMember::getUserId, userId)
                .eq(ClassMember::getRole, 0)
                .eq(ClassMember::getStatus, 1);
        List<ClassMember> memberships = classMemberMapper.selectList(memberWrapper);
        if (memberships.isEmpty()) {
            return Collections.emptySet();
        }

        // 2. 提取班级ID
        List<Long> classIds = memberships.stream()
                .map(ClassMember::getClassId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        // 3. 查询班级对应的教师ID
        LambdaQueryWrapper<ClassEntity> classWrapper = new LambdaQueryWrapper<ClassEntity>()
                .in(ClassEntity::getId, classIds)
                .eq(ClassEntity::getStatus, 1);
        List<ClassEntity> classes = classMapper.selectList(classWrapper);

        return classes.stream()
                .map(ClassEntity::getTeacherId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private void buildChildren(QuestionCategory parent, Map<Long, List<QuestionCategory>> parentMap) {
        List<QuestionCategory> children = parentMap.getOrDefault(parent.getId(), new ArrayList<>());
        parent.setChildren(children.isEmpty() ? null : children);
        for (QuestionCategory child : children) {
            buildChildren(child, parentMap);
        }
    }
}
