package com.zhikao.controller;

import com.zhikao.common.Result;
import com.zhikao.entity.ExamCertificate;
import com.zhikao.entity.ExamPaper;
import com.zhikao.entity.ExamSession;
import com.zhikao.entity.User;
import com.zhikao.mapper.ExamPaperMapper;
import com.zhikao.mapper.UserMapper;
import com.zhikao.service.ExamCertificateService;
import com.zhikao.service.ExamSessionService;
import com.zhikao.utils.CertificateImageUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.List;

/**
 * 证书控制器 — 颁发/查询/生成图片
 */
@RestController
@RequestMapping("/cert")
public class CertificateController {

    @Autowired
    private ExamCertificateService examCertificateService;

    @Autowired
    private ExamSessionService examSessionService;

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 颁发证书（持久化到 exam_certificate 表）
     */
    @PostMapping("/issue/{sessionId}")
    public Result<ExamCertificate> issue(@PathVariable Long sessionId) {
        try {
            ExamCertificate cert = examCertificateService.issue(sessionId);
            return Result.success("证书颁发成功", cert);
        } catch (RuntimeException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    /**
     * 我的证书列表
     */
    @GetMapping("/my")
    public Result<List<ExamCertificate>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(examCertificateService.listByUser(userId));
    }

    /**
     * 查询某次考试记录的证书记录
     */
    @GetMapping("/{sessionId}")
    public Result<ExamCertificate> getBySession(@PathVariable Long sessionId) {
        return Result.success(examCertificateService.getBySession(sessionId));
    }

    /**
     * 按考试记录生成证书图片（从 session 读取姓名/考试名/分数）
     */
    @GetMapping("/by-session/{sessionId}")
    public void generateBySession(@PathVariable Long sessionId,
                                  HttpServletResponse response) throws Exception {
        ExamSession session = examSessionService.getById(sessionId);
        if (session == null) {
            response.sendError(404, "考试记录不存在");
            return;
        }
        ExamPaper paper = examPaperMapper.selectById(session.getPaperId());
        User user = userMapper.selectById(session.getUserId());
        String name = (user != null && user.getNickname() != null) ? user.getNickname()
                : (user != null ? user.getUsername() : "");
        String exam = (paper != null && paper.getTitle() != null) ? paper.getTitle() : "智考云在线考试";
        int score = session.getTotalScore() != null ? session.getTotalScore().intValue() : 0;
        writePng(name, exam, score, response);
    }

    /**
     * 原始生成接口（保留兼容，直接传参画图）
     */
    @GetMapping("/generate")
    public void generateCertificate(@RequestParam String name,
                                    @RequestParam(defaultValue = "智考云在线考试平台") String exam,
                                    @RequestParam(defaultValue = "100") int score,
                                    HttpServletResponse response) throws Exception {
        writePng(name, exam, score, response);
    }

    private void writePng(String name, String exam, int score,
                          HttpServletResponse response) throws Exception {
        BufferedImage img = CertificateImageUtil.render(name, exam, score);
        response.setContentType("image/png");
        response.setHeader("Content-Disposition", "inline; filename=certificate.png");
        try (OutputStream os = response.getOutputStream()) {
            ImageIO.write(img, "PNG", os);
        }
    }
}
