package com.zhikao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * 智考云 - 在线考试与学习平台 主启动类
 */
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableMethodSecurity
public class ZhikaoCloudApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhikaoCloudApplication.class, args);
        System.out.println("""
                =====================================================
                  智考云 - 在线考试与学习平台 启动成功！
                =====================================================
                  API文档: http://localhost:8080/api/v1/doc.html
                =====================================================
                """);
    }
}
