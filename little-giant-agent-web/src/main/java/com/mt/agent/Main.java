package com.mt.agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 类描述
 *
 * @author lfz
 * @date 2025/3/11 11:10
 */
@SpringBootApplication
@MapperScan({ "com.mt.agent.repository.**.mapper" })
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
        System.out.println("启动成功！");
    }
}