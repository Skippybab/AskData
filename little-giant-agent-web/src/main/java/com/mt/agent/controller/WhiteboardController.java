package com.mt.agent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mt.agent.repository.config.entity.ConfigWhiteboard;
import com.mt.agent.repository.config.service.IConfigWhiteboardService;
import com.mt.agent.model.Result;
import com.mt.agent.utils.DateUtil;
import com.mt.agent.utils.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 类描述
 *
 * @author lfz
 * @date 2025/3/20 14:21
 */
@RestController
@RequestMapping("/whiteboard")
@Slf4j
@RequiredArgsConstructor
public class WhiteboardController {

    private final IConfigWhiteboardService configWhiteboardService;

    @PostMapping("/save")
    public Result save(@RequestBody Map<String,Object> params, HttpServletRequest request) {

        String jsonStr = (String) params.get("jsonStr");
        Long userId = (Long) SessionUtil.getAttribute(request, SessionUtil.LOGIN_USER_ID);

        LambdaQueryWrapper<ConfigWhiteboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigWhiteboard::getUserId, userId);

        ConfigWhiteboard whiteboard = configWhiteboardService.getOne(wrapper);
        if(whiteboard == null) {
            whiteboard = new ConfigWhiteboard();
            whiteboard.setContentJson(jsonStr);
            whiteboard.setUserId(userId);
            whiteboard.setCreateTime(DateUtil.formatCurrentDateTime());
            configWhiteboardService.save(whiteboard);
        }else {
            whiteboard.setContentJson(jsonStr);
            whiteboard.setUpdateTime(DateUtil.formatCurrentDateTime());
            configWhiteboardService.updateById(whiteboard);
        }

        return Result.success();

    }

    @GetMapping("/get")
    public Result get( HttpServletRequest request) {
        Long userId = (Long) SessionUtil.getAttribute(request, SessionUtil.LOGIN_USER_ID);
        LambdaQueryWrapper<ConfigWhiteboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConfigWhiteboard::getUserId, userId);

        ConfigWhiteboard whiteboard = configWhiteboardService.getOne(wrapper);

        return Result.success(whiteboard);

    }

}
