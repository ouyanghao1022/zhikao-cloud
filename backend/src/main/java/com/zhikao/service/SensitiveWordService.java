package com.zhikao.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SensitiveWordService {

    private static final Set<String> WORDS = new HashSet<>(Arrays.asList(
        "fuck", "shit", "ass", "dick", "bitch", "porn", "xxx", "sex",
        "赌博", "赌场", "彩票", "裸聊", "色情", "黄色", "枪支", "毒品",
        "代办证件", "刻章", "假证", "枪支弹药", "迷药", "嫖娼", "卖淫"
    ));

    public boolean containsSensitive(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        for (String word : WORDS) {
            if (lower.contains(word)) return true;
        }
        return false;
    }

    public String filter(String text) {
        if (text == null) return null;
        String result = text;
        for (String word : WORDS) {
            result = result.replaceAll("(?i)" + word, "*".repeat(word.length()));
        }
        return result;
    }
}
