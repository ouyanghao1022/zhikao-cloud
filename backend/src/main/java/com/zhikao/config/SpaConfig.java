package com.zhikao.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.ErrorPageRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * 单页应用（SPA）路由回退配置
 * 将未匹配到的路径转发到 index.html
 */
@Configuration
public class SpaConfig {

    @Bean
    public ErrorPageRegistrar errorPageRegistrar() {
        return registry -> registry.addErrorPages(
            new ErrorPage(HttpStatus.NOT_FOUND, "/index.html")
        );
    }
}
