package com.mt.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 自定义拦截器接管
     *
     * @return
     */
    @Bean
    public AuthInterceptor authInterceptor() {
        return new AuthInterceptor();
    }

    /**
     * 设置请求拦截器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 放行路径
        List<String> patterns = new ArrayList<>();
        // swagger资源放行
        patterns.add("/swagger-ui.html/**");
        patterns.add("/swagger-resources/**");
        patterns.add("/v2/**");
        patterns.add("/webjars/**");

        patterns.add("/sys/login");
        patterns.add("/sys/user");
        patterns.add("/dataTransfer/aiDataResult");
        patterns.add("/api/chat");
        registry.addInterceptor(authInterceptor()).addPathPatterns("/**")
                .excludePathPatterns(patterns);
    }

    /**
     * 设置跨域放行
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("GET", "POST", "HEAD", "PUT", "DELETE", "OPTIONS")
                .allowCredentials(true)
                .allowedOriginPatterns("*")
                .maxAge(3600)
                .allowedHeaders("*");
    }
}
