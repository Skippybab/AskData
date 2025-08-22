package com.mt.agent.workflow.api.util;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 安全工具类
 * 用于获取当前登录用户信息
 */
@Component
public class SecurityUtil {
    
    /**
     * 获取当前登录用户ID
     * 这里简化处理，实际项目中应该从JWT token或Session中获取
     */
    public Long getCurrentUserId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // 从请求头中获取用户ID（简化处理）
                String userIdStr = request.getHeader("X-User-Id");
                if (userIdStr != null && !userIdStr.isEmpty()) {
                    return Long.parseLong(userIdStr);
                }
                
                // 从session中获取
                Object userId = request.getSession().getAttribute("userId");
                if (userId != null) {
                    return Long.parseLong(userId.toString());
                }
                
                // 从请求参数中获取（仅用于测试）
                String paramUserId = request.getParameter("userId");
                if (paramUserId != null && !paramUserId.isEmpty()) {
                    return Long.parseLong(paramUserId);
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        
        // 默认返回1（测试用户）
        return 1L;
    }
    
    /**
     * 获取当前登录用户名
     */
    public String getCurrentUsername() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // 从请求头中获取用户名
                String username = request.getHeader("X-Username");
                if (username != null && !username.isEmpty()) {
                    return username;
                }
                
                // 从session中获取
                Object user = request.getSession().getAttribute("username");
                if (user != null) {
                    return user.toString();
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        
        return "default_user";
    }
    
    /**
     * 检查是否已登录
     */
    public boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }
}
