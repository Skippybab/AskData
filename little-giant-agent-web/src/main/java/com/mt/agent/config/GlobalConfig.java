package com.mt.agent.config;

import org.springframework.stereotype.Component;

/**
 * @author Edwin
 * @version 1.0.0
 */
@Component
public class GlobalConfig {

    // 移除ddlApplicationRunner Bean定义，避免与MyBatis Plus自动配置冲突
    // MyBatis Plus会根据配置自动管理DDL相关功能

}
