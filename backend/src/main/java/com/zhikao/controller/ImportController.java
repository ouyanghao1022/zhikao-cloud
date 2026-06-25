package com.zhikao.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhikao.common.Result;
import com.zhikao.entity.*;
import com.zhikao.mapper.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 数据导入控制器 — 用户批量导入 / 题库批量导入 / 试卷批量导入
 */
@RestController
@RequestMapping("/admin/import")
public class ImportController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private QuestionCategoryMapper categoryMapper;
    @Autowired
    private ExamPaperMapper examPaperMapper;
    @Autowired
    private ExamPaperQuestionMapper examPaperQuestionMapper;

    // ==================== 用户导入 ====================

    /**
     * 下载用户导入模板
     */
    @GetMapping("/template/users")
    public void downloadUserTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = URLEncoder.encode("用户导入模板.xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);

        try (Workbook wb = new XSSFWorkbook(); OutputStream os = response.getOutputStream()) {
            Sheet sheet = wb.createSheet("用户导入");
            Row header = sheet.createRow(0);
            String[] heads = {"用户名*", "昵称*", "密码*", "角色*", "邮箱", "手机号", "学校", "年级", "性别"};
            for (int i = 0; i < heads.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(heads[i]);
                CellStyle style = wb.createCellStyle();
                Font font = wb.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }
            // 示例行
            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("zhangsan");
            example.createCell(1).setCellValue("张三");
            example.createCell(2).setCellValue("123456");
            example.createCell(3).setCellValue("STUDENT");
            example.createCell(4).setCellValue("zhangsan@qq.com");
            example.createCell(5).setCellValue("13800138000");
            example.createCell(6).setCellValue("第一中学");
            example.createCell(7).setCellValue("初一");
            example.createCell(8).setCellValue("男");

            for (int i = 0; i < heads.length; i++) sheet.autoSizeColumn(i);
            wb.write(os);
        }
    }

    /**
     * 批量导入用户
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @PostMapping("/users")
    @Transactional
    public Result<Map<String, Object>> importUsers(@RequestParam("file") MultipartFile file,
                                                    HttpServletRequest request) {
        Long currentUserId = (Long) request.getAttribute("userId");
        Map<String, Object> result = new HashMap<>();
        int success = 0, skip = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);

            // 查找角色ID
            Map<String, Long> roleMap = new HashMap<>();
            List<Role> roles = roleMapper.selectList(null);
            for (Role r : roles) roleMap.put(r.getRoleCode(), r.getId());

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String username = getCellStr(row, 0);
                    String nickname = getCellStr(row, 1);
                    String password = getCellStr(row, 2);
                    String roleCode = getCellStr(row, 3);
                    String email = getCellStr(row, 4);
                    String phone = getCellStr(row, 5);
                    String school = getCellStr(row, 6);
                    String grade = getCellStr(row, 7);
                    String genderStr = getCellStr(row, 8);

                    if (StrUtil.isBlank(username) || StrUtil.isBlank(nickname) || StrUtil.isBlank(password)) {
                        errors.add("第" + (i + 1) + "行: 用户名/昵称/密码不能为空"); skip++; continue;
                    }
                    // 检查用户名唯一
                    if (userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                            .eq(User::getUsername, username.trim())) != null) {
                        errors.add("第" + (i + 1) + "行: 用户名「" + username + "」已存在"); skip++; continue;
                    }

                    User user = new User();
                    user.setUsername(username.trim());
                    user.setNickname(nickname.trim());
                    user.setPassword(BCrypt.hashpw(password.trim(), BCrypt.gensalt()));
                    user.setEmail(email.isBlank() ? null : email.trim());
                    user.setPhone(phone.isBlank() ? null : phone.trim());
                    user.setSchool(school.isBlank() ? null : school.trim());
                    user.setGrade(grade.isBlank() ? null : grade.trim());
                    user.setGender(parseGender(genderStr));
                    user.setStatus(1);
                    user.setCreatedAt(new Date());
                    user.setUpdatedAt(new Date());
                    userMapper.insert(user);

                    // 分配角色
                    String rc = StrUtil.isBlank(roleCode) ? "STUDENT" : roleCode.trim().toUpperCase();
                    Long roleId = roleMap.get(rc);
                    if (roleId == null) roleId = roleMap.get("STUDENT");
                    UserRole ur = new UserRole();
                    ur.setUserId(user.getId());
                    ur.setRoleId(roleId);
                    userRoleMapper.insert(ur);

                    success++;
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                    skip++;
                }
            }
        } catch (IOException e) {
            return Result.error("文件读取失败: " + e.getMessage());
        }

        result.put("success", success);
        result.put("skip", skip);
        result.put("total", success + skip);
        result.put("errors", errors.size() > 10 ? errors.subList(0, 10) : errors);
        return Result.success("导入完成", result);
    }

    // ==================== 题库导入 ====================

    /**
     * 下载题库导入模板
     */
    @GetMapping("/template/questions")
    public void downloadQuestionTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = URLEncoder.encode("题库批量导入模板.xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);

        try (InputStream is = getClass().getResourceAsStream("/import-template-questions.xlsx");
             OutputStream os = response.getOutputStream()) {
            if (is == null) {
                response.setStatus(404);
                os.write("模板文件未找到".getBytes(StandardCharsets.UTF_8));
                return;
            }
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
        }
    }

    /**
     * 批量导入题目
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @PostMapping("/questions")
    @Transactional
    public Result<Map<String, Object>> importQuestions(@RequestParam("file") MultipartFile file,
                                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = new HashMap<>();
        int success = 0, skip = 0;
        List<String> errors = new ArrayList<>();

        // 缓存分类名称→ID映射
        Map<String, Long> categoryNameMap = new HashMap<>();
        List<QuestionCategory> allCats = categoryMapper.selectList(null);
        for (QuestionCategory c : allCats) categoryNameMap.put(c.getCategoryName().trim(), c.getId());

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    // 0:所属目录 1:题型 2:难度 3:允许练习 4:题目内容 5:A 6:B 7:C 8:D 9:正确答案 10:解析
                    String catName = getCellStr(row, 0);
                    String typeStr = getCellStr(row, 1);
                    String diffStr = getCellStr(row, 2);
                    String allowPracticeStr = getCellStr(row, 3);
                    String title = getCellStr(row, 4);
                    String optionA = getCellStr(row, 5);
                    String optionB = getCellStr(row, 6);
                    String optionC = getCellStr(row, 7);
                    String optionD = getCellStr(row, 8);
                    String answer = getCellStr(row, 9);
                    String analysis = getCellStr(row, 10);

                    if (StrUtil.isBlank(title) || StrUtil.isBlank(answer)) {
                        errors.add("第" + (i + 1) + "行: 题目内容/正确答案不能为空"); skip++; continue;
                    }

                    int questionType = parseQuestionType(typeStr);
                    int difficulty = parseDifficulty(diffStr);
                    boolean allowPractice = parseAllowPractice(allowPracticeStr);

                    // 确定分类ID
                    Long categoryId = null;
                    if (StrUtil.isNotBlank(catName)) {
                        categoryId = categoryNameMap.get(catName.trim());
                        if (categoryId == null) {
                            // 自动创建分类
                            QuestionCategory newCat = new QuestionCategory();
                            newCat.setCategoryName(catName.trim());
                            newCat.setCategoryType(1);
                            newCat.setParentId(0L);
                            newCat.setStatus(1);
                            newCat.setCreatorId(userId);
                            newCat.setAllowPractice(1);
                            newCat.setCreatedAt(new Date());
                            newCat.setUpdatedAt(new Date());
                            categoryMapper.insert(newCat);
                            categoryId = newCat.getId();
                            categoryNameMap.put(catName.trim(), categoryId);
                        }
                    }

                    // 构建选项JSON
                    String contentJson = null;
                    if (questionType == 1 || questionType == 2) {
                        StringBuilder sb = new StringBuilder("[");
                        if (StrUtil.isNotBlank(optionA)) sb.append("{\"label\":\"A\",\"content\":\"").append(escapeJson(optionA)).append("\"},");
                        if (StrUtil.isNotBlank(optionB)) sb.append("{\"label\":\"B\",\"content\":\"").append(escapeJson(optionB)).append("\"},");
                        if (StrUtil.isNotBlank(optionC)) sb.append("{\"label\":\"C\",\"content\":\"").append(escapeJson(optionC)).append("\"},");
                        if (StrUtil.isNotBlank(optionD)) sb.append("{\"label\":\"D\",\"content\":\"").append(escapeJson(optionD)).append("\"},");
                        if (sb.charAt(sb.length() - 1) == ',') sb.setLength(sb.length() - 1);
                        sb.append("]");
                        contentJson = sb.toString();
                    }

                    Question q = new Question();
                    q.setQuestionType(questionType);
                    q.setTitle(title.trim());
                    q.setAnswer(answer.trim());
                    q.setContent(contentJson);
                    q.setAnswerAnalysis(StrUtil.isNotBlank(analysis) ? analysis.trim() : null);
                    q.setCategoryId(categoryId);
                    q.setDifficulty(difficulty);
                    q.setScore(java.math.BigDecimal.valueOf(5));
                    q.setCreatorId(userId);
                    q.setStatus(1);
                    q.setAllowPractice(allowPractice ? 1 : 0);
                    q.setBankType(0);
                    q.setCreatedAt(new Date());
                    q.setUpdatedAt(new Date());
                    questionMapper.insert(q);
                    success++;
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                    skip++;
                }
            }
        } catch (IOException e) {
            return Result.error("文件读取失败: " + e.getMessage());
        }

        result.put("success", success);
        result.put("skip", skip);
        result.put("total", success + skip);
        result.put("errors", errors.size() > 10 ? errors.subList(0, 10) : errors);
        return Result.success("导入完成", result);
    }

    // ==================== 试卷导入 ====================

    /**
     * 下载试卷导入模板（双Sheet：试卷信息 + 题目列表）
     */
    @GetMapping("/template/exam")
    public void downloadExamTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = URLEncoder.encode("试卷导入模板.xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);

        try (Workbook wb = new XSSFWorkbook(); OutputStream os = response.getOutputStream()) {
            // Sheet1：试卷信息（A列字段名 B列示例值；带*为必填）
            Sheet infoSheet = wb.createSheet("试卷信息");
            Object[][] infoRows = {
                {"试卷名称*", "（示例）高三数学期中考试"},
                {"考试时长", "60"},
                {"总分", "100"},
                {"及格分", "60"},
                {"开始时间*", "2026-07-01 09:00:00"},
                {"结束时间*", "2026-07-01 11:00:00"},
                {"考试说明", "高三数学期中考试"},
                {"组卷模式", "固定"},
            };
            for (int i = 0; i < infoRows.length; i++) {
                Row row = infoSheet.createRow(i);
                row.createCell(0).setCellValue(infoRows[i][0].toString());
                row.createCell(1).setCellValue(infoRows[i][1].toString());
            }
            infoSheet.setColumnWidth(0, 4000);
            infoSheet.setColumnWidth(1, 9000);

            // Sheet2：题目列表（表头 + 示例行）
            Sheet qSheet = wb.createSheet("题目列表");
            String[] headers = {"题型", "难度", "题目内容", "选项A", "选项B", "选项C", "选项D", "正确答案", "解析", "分值"};
            Row headerRow = qSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);
            String[] example = {"单选", "简单", "log₂8 + log₂4 =", "5", "6", "7", "8", "A", "log₂8=3, log₂4=2, 相加为5", "5"};
            Row exampleRow = qSheet.createRow(1);
            for (int i = 0; i < example.length; i++) exampleRow.createCell(i).setCellValue(example[i]);

            wb.write(os);
        }
    }

    /**
     * 批量导入试卷
     * Excel 格式：
     *   Sheet1 "试卷信息"：A列字段名 B列字段值（试卷名称*、考试时长、总分、及格分、考试说明、组卷模式）
     *   Sheet2 "题目列表"：题型*、难度、题目内容*、选项A、选项B、选项C、选项D、正确答案*、解析、分值*
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','TEACHER')")
    @PostMapping("/exam")
    @Transactional
    public Result<Map<String, Object>> importExam(@RequestParam("file") MultipartFile file,
                                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            // ====== Sheet1: 试卷信息 ======
            Sheet infoSheet = wb.getSheet("试卷信息");
            if (infoSheet == null) infoSheet = wb.getSheetAt(0);

            Map<String, String> infoMap = new HashMap<>();
            for (int i = 0; i <= infoSheet.getLastRowNum(); i++) {
                Row row = infoSheet.getRow(i);
                if (row == null) continue;
                String key = getCellStr(row, 0);
                String val = getCellStr(row, 1);
                if (StrUtil.isNotBlank(key)) infoMap.put(key.trim(), val.trim());
            }

            String title = infoMap.getOrDefault("试卷名称", "");
            if (StrUtil.isBlank(title)) {
                return Result.error("Sheet「试卷信息」中缺少「试卷名称」");
            }

            int duration = parseIntSafe(infoMap.get("考试时长"), 60);
            BigDecimal totalScore = parseDecimalSafe(infoMap.get("总分"), BigDecimal.valueOf(100));
            BigDecimal passScore = parseDecimalSafe(infoMap.get("及格分"), BigDecimal.valueOf(60));
            String description = infoMap.getOrDefault("考试说明", "");
            String modeStr = infoMap.getOrDefault("组卷模式", "固定");
            int paperType = modeStr.contains("随机") ? 2 : 1;

            // 创建试卷
            ExamPaper paper = new ExamPaper();
            paper.setTitle(title);
            paper.setDescription(StrUtil.isBlank(description) ? null : description);
            paper.setTotalScore(totalScore);
            paper.setPassScore(passScore);
            paper.setDuration(duration);
            paper.setPaperType(paperType);
            paper.setShuffleQuestion(0);
            paper.setShuffleOption(0);
            paper.setMaxAttempts(1);
            paper.setAllowViewAnswer(1);
            paper.setShowScoreType(1);
            paper.setAntiCheatLevel(1);
            paper.setMaxScreenSwitch(3);
            // 考试时间（必填）
            String startTimeStr = infoMap.getOrDefault("开始时间", "");
            String endTimeStr = infoMap.getOrDefault("结束时间", "");
            if (StrUtil.isBlank(startTimeStr) || StrUtil.isBlank(endTimeStr)) {
                return Result.error("Sheet「试卷信息」中缺少「开始时间」或「结束时间」（必填）");
            }
            try {
                paper.setStartTime(java.sql.Timestamp.valueOf(startTimeStr.replace("T", " ")));
                paper.setEndTime(java.sql.Timestamp.valueOf(endTimeStr.replace("T", " ")));
            } catch (Exception e) {
                return Result.error("考试时间格式错误，应为 yyyy-MM-dd HH:mm:ss，如 2026-07-01 09:00:00");
            }
            paper.setCreatorId(userId);
            paper.setStatus(0); // 草稿
            paper.setEnrolledCount(0);
            paper.setCreatedAt(new Date());
            paper.setUpdatedAt(new Date());
            examPaperMapper.insert(paper);

            // ====== Sheet2: 题目列表 ======
            Sheet qSheet = wb.getSheet("题目列表");
            if (qSheet == null && wb.getNumberOfSheets() > 1) qSheet = wb.getSheetAt(1);
            if (qSheet == null) {
                return Result.error("Excel 中缺少「题目列表」Sheet");
            }

            int success = 0, skip = 0;
            BigDecimal actualTotal = BigDecimal.ZERO;
            ObjectMapper objectMapper = new ObjectMapper();
            int sort = 1;

            for (int i = 1; i <= qSheet.getLastRowNum(); i++) {
                Row row = qSheet.getRow(i);
                if (row == null) continue;
                try {
                    // 0:题型 1:难度 2:题目内容 3:A 4:B 5:C 6:D 7:正确答案 8:解析 9:分值
                    String typeStr = getCellStr(row, 0);
                    String diffStr = getCellStr(row, 1);
                    String qTitle = getCellStr(row, 2);
                    String optionA = getCellStr(row, 3);
                    String optionB = getCellStr(row, 4);
                    String optionC = getCellStr(row, 5);
                    String optionD = getCellStr(row, 6);
                    String answer = getCellStr(row, 7);
                    String analysis = getCellStr(row, 8);
                    String scoreStr = getCellStr(row, 9);

                    if (StrUtil.isBlank(qTitle) || StrUtil.isBlank(answer)) {
                        errors.add("第" + (i + 1) + "行: 题目内容/正确答案不能为空"); skip++; continue;
                    }

                    int questionType = parseQuestionType(typeStr);
                    int difficulty = parseDifficulty(diffStr);
                    BigDecimal qScore = parseDecimalSafe(scoreStr, BigDecimal.valueOf(5));

                    // 构建选项JSON
                    String contentJson = null;
                    if (questionType == 1 || questionType == 2) {
                        StringBuilder sb = new StringBuilder("[");
                        if (StrUtil.isNotBlank(optionA)) sb.append("{\"label\":\"A\",\"content\":\"").append(escapeJson(optionA)).append("\"},");
                        if (StrUtil.isNotBlank(optionB)) sb.append("{\"label\":\"B\",\"content\":\"").append(escapeJson(optionB)).append("\"},");
                        if (StrUtil.isNotBlank(optionC)) sb.append("{\"label\":\"C\",\"content\":\"").append(escapeJson(optionC)).append("\"},");
                        if (StrUtil.isNotBlank(optionD)) sb.append("{\"label\":\"D\",\"content\":\"").append(escapeJson(optionD)).append("\"},");
                        if (sb.charAt(sb.length() - 1) == ',') sb.setLength(sb.length() - 1);
                        sb.append("]");
                        contentJson = sb.toString();
                    }

                    // 1. 写入题库（作为试卷专属题目）
                    Question q = new Question();
                    q.setQuestionType(questionType);
                    q.setTitle(qTitle.trim());
                    q.setAnswer(answer.trim());
                    q.setContent(contentJson);
                    q.setAnswerAnalysis(StrUtil.isNotBlank(analysis) ? analysis.trim() : null);
                    q.setDifficulty(difficulty);
                    q.setScore(qScore);
                    q.setCreatorId(userId);
                    q.setStatus(1);
                    q.setAllowPractice(0); // 试卷题目默认不允许练习
                    q.setBankType(0);
                    q.setCreatedAt(new Date());
                    q.setUpdatedAt(new Date());
                    questionMapper.insert(q);

                    // 2. 构建快照 JSON
                    Map<String, Object> snapshot = new LinkedHashMap<>();
                    snapshot.put("id", q.getId());
                    snapshot.put("questionType", questionType);
                    snapshot.put("title", qTitle.trim());
                    snapshot.put("content", contentJson);
                    snapshot.put("answer", answer.trim());
                    snapshot.put("answerAnalysis", StrUtil.isNotBlank(analysis) ? analysis.trim() : null);
                    snapshot.put("difficulty", difficulty);
                    snapshot.put("score", qScore);
                    String snapshotJson = objectMapper.writeValueAsString(snapshot);

                    // 3. 关联到试卷
                    ExamPaperQuestion epq = new ExamPaperQuestion();
                    epq.setPaperId(paper.getId());
                    epq.setQuestionId(q.getId());
                    epq.setSort(sort++);
                    epq.setScore(qScore);
                    epq.setQuestionSnapshot(snapshotJson);
                    epq.setCreatedAt(new Date());
                    examPaperQuestionMapper.insert(epq);

                    actualTotal = actualTotal.add(qScore);
                    success++;
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                    skip++;
                }
            }

            // 如果实际总分与填写的不一致，以实际为准
            if (success > 0 && actualTotal.compareTo(BigDecimal.ZERO) > 0) {
                paper.setTotalScore(actualTotal);
                // 自动按60%设置及格分（如果用户未显式指定及格分或与实际总分比例偏差大）
                BigDecimal autoPass = actualTotal.multiply(BigDecimal.valueOf(0.6))
                        .setScale(0, java.math.RoundingMode.HALF_UP);
                paper.setPassScore(autoPass);
                examPaperMapper.updateById(paper);
            }

            result.put("paperId", paper.getId());
            result.put("paperTitle", title);
            result.put("success", success);
            result.put("skip", skip);
            result.put("total", success + skip);
            result.put("actualTotalScore", actualTotal);
            result.put("errors", errors.size() > 10 ? errors.subList(0, 10) : errors);
            return Result.success("试卷导入完成", result);

        } catch (IOException e) {
            return Result.error("文件读取失败: " + e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private static final DataFormatter dataFormatter = new DataFormatter();

    private String getCellStr(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        // 使用 DataFormatter 安全读取，不调用已废弃的 setCellType（会清空值）
        String val = dataFormatter.formatCellValue(cell);
        return val == null ? "" : val.trim();
    }

    private int parseGender(String s) {
        if (StrUtil.isBlank(s)) return 0;
        String lower = s.trim();
        if (lower.contains("男") || lower.equals("1") || lower.equalsIgnoreCase("M")) return 1;
        if (lower.contains("女") || lower.equals("2") || lower.equalsIgnoreCase("F")) return 2;
        return 0;
    }

    private int parseQuestionType(String s) {
        if (StrUtil.isBlank(s)) return 1;
        String lower = s.trim();
        if (lower.contains("多选")) return 2;
        if (lower.contains("填空")) return 3;
        if (lower.contains("判断")) return 5;
        return 1; // 默认单选
    }

    private int parseDifficulty(String s) {
        if (StrUtil.isBlank(s)) return 1;
        String lower = s.trim();
        if (lower.contains("困难") || lower.contains("难")) return 3;
        if (lower.contains("中等") || lower.contains("中")) return 2;
        if (lower.contains("简单") || lower.contains("易")) return 1;
        try { return Integer.parseInt(lower); } catch (NumberFormatException e) { return 1; }
    }

    private boolean parseAllowPractice(String s) {
        if (StrUtil.isBlank(s)) return true;
        String lower = s.trim();
        return !lower.equals("否") && !lower.equals("0") && !lower.equalsIgnoreCase("n") && !lower.equalsIgnoreCase("false");
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private int parseIntSafe(String s, int defaultVal) {
        if (StrUtil.isBlank(s)) return defaultVal;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return defaultVal; }
    }

    private BigDecimal parseDecimalSafe(String s, BigDecimal defaultVal) {
        if (StrUtil.isBlank(s)) return defaultVal;
        try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return defaultVal; }
    }
}
