package org.example.cipherws.note;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CipherWS 笔记系统单体启动类。
 *
 * <p>原来的 user / core / ai / admin 四个服务合并到这个应用里，按包分模块：
 * 共享一个 Spring 容器、一个端口（8084）、一个数据源和一套 MyBatis 配置。</p>
 */
@SpringBootApplication(scanBasePackages = "org.example.cipherws.note")
@MapperScan({
        "org.example.cipherws.note.user.mapper",
        "org.example.cipherws.note.core.mapper",
        "org.example.cipherws.note.ai.mapper",
        "org.example.cipherws.note.admin.mapper"
})
public class NoteApplication {

    /**
     * 进程入口：启动 Spring Boot 容器。
     *
     * <p>这里不写端口、数据源等任何具体配置，全部交给 application.yml，
     * 保证本地开发与部署环境共用同一份启动代码。</p>
     */
    public static void main(String[] args) {
        SpringApplication.run(NoteApplication.class, args);
    }
}
