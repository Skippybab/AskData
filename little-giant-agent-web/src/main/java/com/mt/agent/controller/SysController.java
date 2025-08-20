package com.mt.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mt.agent.model.Result;
import com.mt.agent.repository.sys.entity.SysUser;
import com.mt.agent.repository.sys.service.ISysUserService;

import com.mt.agent.utils.MD5Util;
import com.mt.agent.utils.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统控制器
 *
 * @author lfz
 * @date 2025/3/20 9:09
 */
@RestController
@RequestMapping("/sys")
@Slf4j
@RequiredArgsConstructor
public class SysController {

    private final ISysUserService sysUserService;


    /**
     * 登录验证
     */
    @PostMapping("/login")
    public Result login(@RequestBody SysUser sysUser, HttpServletRequest request) {
        try {
            log.info("[SysController:login]{sysUserName:" + sysUser.getName() + "}");

            String username = sysUser.getName();
            String password = sysUser.getPassword();

            if (username == null || password == null) {
                return Result.error("用户名或密码不能为空");
            }

            // 验证用户名和密码
            QueryWrapper<SysUser> wrapper = new QueryWrapper<SysUser>().eq("name", username)
                    .eq("status", 1);
            SysUser user = sysUserService.getOne(wrapper);

            if (user == null) {
                return Result.error("用户不存在");
            }

            if (MD5Util.MD5X2Verification(password, user.getPassword())) {
                // 如果验证通过，返回登录成功的信息
                SessionUtil.addAttribute(request, SessionUtil.LOGIN_USER_ID, user.getId());
                return Result.success();
            }

            // 如果验证不通过，返回登录失败的信息
            return Result.error("密码错误");
        } catch (Exception e) {
            log.error("[SysController:login]{sysUserName:" + sysUser.getName() + "}.", e);
            return Result.error();
        }
    }

    /**
     * 查询登录用户
     * @param request
     * @return
     */
    @GetMapping("/user")
    public Result getUser(HttpServletRequest request) {
        try {
            log.info("[SysController:getUser]query user info");
            Long userId = (Long) SessionUtil.getAttribute(request, SessionUtil.LOGIN_USER_ID);
            Map<String, Long> map = new HashMap<>();
            map.put("userId", userId);
            return Result.success(map);
        } catch (Exception e) {
            log.error("[SysController:getUser]false to query user info", e);
            return Result.error();
        }
    }

    /**
     * 退出登录
     * @param request
     * @return
     */
    @GetMapping("/logout")
    public Result logout(HttpServletRequest request) {
        try {
            log.info("[SysController:logout]logout user info");
            SessionUtil.removeAttribute(request, SessionUtil.LOGIN_USER_ID);
            return Result.success();
        } catch (Exception e) {
            log.error("[SysController:logout]false to logout user info", e);
            return Result.error();
        }
    }
}
