package com.mt.agent.workflow.api.interceptor;

import com.mt.agent.workflow.api.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 跳过OPTIONS请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 获取Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 如果没有token，使用默认用户ID（开发环境）
            log.debug("未提供JWT token，使用默认用户");
            request.setAttribute("userId", 1L);
            request.setAttribute("tenantId", 0L);
            request.setAttribute("username", "default_user");
            return true;
        }

        try {
            // 提取token
            String token = authHeader.substring(7);
            
            // 验证token
            if (!jwtUtil.validateToken(token)) {
                log.warn("无效的JWT token: {}", token);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            // 从token中提取用户信息并设置到request属性中
            Long userId = jwtUtil.getUserIdFromToken(token);
            Long tenantId = jwtUtil.getTenantIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);

            request.setAttribute("userId", userId);
            request.setAttribute("tenantId", tenantId);
            request.setAttribute("username", username);

            log.debug("用户认证成功: userId={}, tenantId={}, username={}", userId, tenantId, username);
            return true;

        } catch (Exception e) {
            log.error("JWT token解析失败: {}, path: {}, method: {}", e.getMessage(), request.getRequestURI(), request.getMethod(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
