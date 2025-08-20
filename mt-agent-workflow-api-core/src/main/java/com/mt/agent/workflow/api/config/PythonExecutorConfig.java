package com.mt.agent.workflow.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Python执行器配置
 * 
 * 注意：PythonExecutorServiceImpl使用@RequiredArgsConstructor和@Service注解，
 * Spring会自动管理依赖注入，无需手动配置Bean
 */
@Configuration
@ConditionalOnProperty(name = "python.executor.enabled", havingValue = "true", matchIfMissing = true)
public class PythonExecutorConfig {
    
    // PythonExecutorServiceImpl已经使用@Service注解，Spring会自动管理
    // 无需手动创建Bean实例
}
