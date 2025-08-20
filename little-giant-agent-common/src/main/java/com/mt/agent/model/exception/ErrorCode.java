package com.mt.agent.model.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TODO
 * <p>
 * 错误编码
 *
 * @author hbm
 * @version 1.0
 * @date 2022/10/19 17:00
 * @ClassName ErrorCode.java
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    UNAUTHORIZED(401, "还未授权，不能访问"),
    FORBIDDEN(403, "没有权限，禁止访问"),
    INTERNAL_SERVER_ERROR(500, "服务器异常，请稍后再试"),

    ACCOUNT_PASSWORD_ERROR(1001, "账号或密码错误"),

    PARAMS_ERROR(1002, "参数错误"),

    REQUEST_ERROR(1003, "请求错误"),

    TOKEN_ERROR(1004, "token 错误或无效"),

    ;


    private final int code;
    private final String msg;
}
