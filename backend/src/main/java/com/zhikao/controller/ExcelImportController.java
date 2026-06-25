package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.entity.Question;
import com.zhikao.service.QuestionService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/import")
public class ExcelImportController {

    @Autowired
    private QuestionService questionService;

    @PreAuthorize("hasAnyRole('TEACHER','SUPER_ADMIN')")
    @PostMapping("/questions")
    public Result<Map<String, Object>> importQuestions(@RequestParam("file") MultipartFile file,
                                                        HttpServletRequest request) {
        if (file.isEmpty()) return Result.badRequest("文件为空");

        List<Question> questions = new ArrayList<>();
        int success = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    Question q = new Question();
                    q.setQuestionType((int) getCellValue(row, 0));
                    q.setTitle(getCellValue(row, 1).toString());
                    q.setAnswer(getCellValue(row, 5).toString());

                    Object diff = getCellValue(row, 2);
                    q.setDifficulty(diff instanceof Number ? ((Number) diff).intValue() : 1);
                    Object score = getCellValue(row, 6);
                    q.setScore(score instanceof Number ? java.math.BigDecimal.valueOf(((Number)score).doubleValue()) : java.math.BigDecimal.valueOf(5));

                    // 选项A-D
                    for (int j = 0; j < 4; j++) {
                        Object opt = getCellValue(row, 7 + j);
                        if (opt != null) {
                            try {
                                String fieldName = "option" + (char)('A' + j);
                                q.getClass().getMethod("set" + fieldName, String.class).invoke(q, opt.toString());
                            } catch (Exception ignored) {}
                        }
                    }
                    questionService.save(q);
                    questions.add(q);
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("行" + (i+1) + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return Result.error("导入失败: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        return Result.success("导入完成", result);
    }

    private Object getCellValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case NUMERIC: return cell.getNumericCellValue();
            case STRING: return cell.getStringCellValue();
            case BOOLEAN: return cell.getBooleanCellValue();
            default: return cell.toString();
        }
    }
}
