package com.mt.agent.dify;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Dify配置类
 * 集中管理Dify相关配置信息
 *
 * @author wsx
 * @date 2025/6/30
 */
@Data
@Component
@ConfigurationProperties(prefix = "dify.api")
public class DifyConfig {

    /**
     * Dify API基础地址
     */
    private String baseUrl = "http://113.45.193.155:8888/v1";

    /**
     * Dify API Key
     */
    private String apiKey = "app-TxiNiLXqeVGJP6IeQPc3Bd6a";

    /**
     * 请求超时时间（毫秒）
     */
    private int timeout = 1000000;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 1000000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 1000000;

}
