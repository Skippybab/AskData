package com.mt.agent.controller;


import com.mt.agent.model.Result;
import com.mt.agent.service.RecommendService;
import com.mt.agent.utils.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommend")
@Slf4j
@RequiredArgsConstructor
public class QuestionRecommendController {

    private final RecommendService recommendService;

    @GetMapping
    public Result recommendQuestion(@RequestParam String questionId, HttpServletRequest request) {
        try {
            Long userId = (Long) SessionUtil.getAttribute(request, SessionUtil.LOGIN_USER_ID);

            return recommendService.recommendQuestion(questionId, userId+"");
        } catch (Exception e) {
            log.error("[QuestionRecommendController:recommendQuestion] error:{}", e.getMessage());
            return Result.error("系统繁忙！");
        }
    }

}
