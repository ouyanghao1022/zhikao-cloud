package com.zhikao.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 证书图片渲染工具 — 返回 BufferedImage，不耦合 HttpServletResponse，
 * 供 CertificateController（按需流式输出）与 ExamCertificateServiceImpl（持久化到磁盘）共用。
 */
public final class CertificateImageUtil {

    private CertificateImageUtil() {}

    public static BufferedImage render(String name, String exam, int score) {
        int width = 800, height = 600;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(255, 250, 240));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(180, 140, 80));
        g.setStroke(new BasicStroke(6));
        g.drawRect(15, 15, width - 30, height - 30);
        g.setStroke(new BasicStroke(2));
        g.drawRect(25, 25, width - 50, height - 50);

        g.setColor(new Color(60, 40, 20));
        g.setFont(new Font("微软雅黑", Font.BOLD, 42));
        drawCentered(g, "智考云 · 成绩证书", width / 2, 100);

        g.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        g.setColor(Color.DARK_GRAY);
        drawCentered(g, "兹证明", width / 2, 180);

        g.setFont(new Font("微软雅黑", Font.BOLD, 30));
        g.setColor(new Color(30, 60, 120));
        drawCentered(g, name, width / 2, 240);

        g.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        g.setColor(Color.DARK_GRAY);
        drawCentered(g, "在「" + exam + "」中取得", width / 2, 300);

        g.setFont(new Font("微软雅黑", Font.BOLD, 48));
        g.setColor(new Color(180, 60, 40));
        drawCentered(g, score + " 分", width / 2, 370);

        g.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        g.setColor(Color.GRAY);
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日"));
        drawCentered(g, "签发日期：" + date, width / 2, 450);

        g.setFont(new Font("微软雅黑", Font.ITALIC, 14));
        drawCentered(g, "智考云在线考试平台 · 电子证书", width / 2, 520);

        g.dispose();
        return img;
    }

    private static void drawCentered(Graphics2D g, String text, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = cx - fm.stringWidth(text) / 2;
        g.drawString(text, x, y);
    }
}
