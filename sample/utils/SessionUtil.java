package com.cultivate.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Session工具类
 * 用于获取当前用户会话信息
 */
@Slf4j
public class SessionUtil {

    private static final String USER_ID_KEY = "userId";
    private static final String USER_TYPE_KEY = "userType";
    private static final String USER_NAME_KEY = "userName";

    /**
     * 获取当前请求的HttpSession
     */
    public static HttpSession getCurrentSession() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getSession(false);
        }
        return null;
    }

    /**
     * 获取当前用户ID
     * 
     * @return 用户ID，如果未登录返回null
     */
    public static String getCurrentUserId() {
        HttpSession session = getCurrentSession();
        if (session != null) {
            Object userId = session.getAttribute(USER_ID_KEY);
            if (userId != null) {
                String userIdStr = userId.toString();
                log.debug("从session获取userId: {}", userIdStr);
                return userIdStr;
            }
        }
        log.warn("无法从session获取userId，可能用户未登录");
        return null;
    }

    /**
     * 获取当前用户类型
     */
    public static String getCurrentUserType() {
        HttpSession session = getCurrentSession();
        if (session != null) {
            Object userType = session.getAttribute(USER_TYPE_KEY);
            return userType != null ? userType.toString() : null;
        }
        return null;
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUserName() {
        HttpSession session = getCurrentSession();
        if (session != null) {
            Object userName = session.getAttribute(USER_NAME_KEY);
            return userName != null ? userName.toString() : null;
        }
        return null;
    }

    /**
     * 设置用户ID到session
     */
    public static void setCurrentUserId(String userId) {
        HttpSession session = getCurrentSession();
        if (session != null) {
            session.setAttribute(USER_ID_KEY, userId);
            log.debug("设置userId到session: {}", userId);
        }
    }

    /**
     * 设置用户类型到session
     */
    public static void setCurrentUserType(String userType) {
        HttpSession session = getCurrentSession();
        if (session != null) {
            session.setAttribute(USER_TYPE_KEY, userType);
        }
    }

    /**
     * 设置用户名到session
     */
    public static void setCurrentUserName(String userName) {
        HttpSession session = getCurrentSession();
        if (session != null) {
            session.setAttribute(USER_NAME_KEY, userName);
        }
    }

    /**
     * 检查用户是否已登录
     */
    public static boolean isUserLoggedIn() {
        String userId = getCurrentUserId();
        return userId != null && !userId.trim().isEmpty();
    }

    /**
     * 清除session中的用户信息
     */
    public static void clearUserSession() {
        HttpSession session = getCurrentSession();
        if (session != null) {
            session.removeAttribute(USER_ID_KEY);
            session.removeAttribute(USER_TYPE_KEY);
            session.removeAttribute(USER_NAME_KEY);
            log.debug("清除session中的用户信息");
        }
    }

    /**
     * 获取session ID
     */
    public static String getSessionId() {
        HttpSession session = getCurrentSession();
        return session != null ? session.getId() : null;
    }
}