package com.mt.agent.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 小巨人代理测试应用程序主启动类
 * 
 * @author MT Agent Team
 * @version 1.0
 * @since 2025-01-15
 */
@SpringBootApplication(scanBasePackages = {
        "com.mt.agent.test",
        "com.mt.agent.ai",
        "com.mt.agent.sysUtil",
        "com.mt.agent.config",
        "com.mt.agent.buffer",
        "com.mt.agent.redis",
        "com.mt.agent.coze",
        "com.mt.agent.consensus",
})
public class LittleGiantAgentTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(LittleGiantAgentTestApplication.class, args);
    }

}
