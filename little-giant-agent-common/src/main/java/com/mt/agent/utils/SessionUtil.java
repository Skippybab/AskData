package com.mt.agent.utils;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class SessionUtil {

	public static final String LOGIN_USER_ID = "login_user_id";
	public static final String SESSION_ID = "session_id";

	/**
	 * 添加session
	 * @param req
	 * @param key
	 * @param value
	 */
	public static void addAttribute(HttpServletRequest req, String key, Object value) {
		HttpSession session = req.getSession();
		session.setAttribute(key, value);
	}

	/**
	 * 删除session
	 * @param req
	 * @param key
	 */
	public static void removeAttribute(HttpServletRequest req, String key) {
		HttpSession session = req.getSession();
		session.removeAttribute(key);
	}

	/**
	 * 获取session
	 * @param req
	 * @param key
	 * @return
	 */
	public static Object getAttribute(HttpServletRequest req, String key) {
		HttpSession session = req.getSession();
		return session.getAttribute(key);
	}
}
