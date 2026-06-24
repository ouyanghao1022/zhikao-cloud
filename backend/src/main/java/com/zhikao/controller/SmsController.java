package com.zhikao.controller;

import com.zhikao.common.Result;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/sms")
public class SmsController {

    private final Map<String, String> codeStore = new ConcurrentHashMap<>();
    private final Map<String, Long> codeTime = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @PostMapping("/send")
    public Result<?> sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return Result.badRequest("手机号格式不正确");
        }
        // 60秒内不可重复发送
        Long lastTime = codeTime.get(phone);
        if (lastTime != null && System.currentTimeMillis() - lastTime < 60000) {
            return Result.badRequest("请60秒后再试");
        }

        String code = String.format("%06d", random.nextInt(1000000));
        codeStore.put(phone, code);
        codeTime.put(phone, System.currentTimeMillis());

        // 模拟：控制台输出验证码
        System.out.println("========== 验证码 [" + phone + "]: " + code + " ==========");
        return Result.success("验证码已发送（模拟），请查看后端控制台");
    }

    @PostMapping("/verify")
    public Result<?> verifyCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        String stored = codeStore.get(phone);
        if (stored == null || !stored.equals(code)) {
            return Result.badRequest("验证码错误或已过期");
        }
        codeStore.remove(phone);
        return Result.success("验证通过");
    }
}
