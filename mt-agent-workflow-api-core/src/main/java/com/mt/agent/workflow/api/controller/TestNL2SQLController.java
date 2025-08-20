package com.mt.agent.workflow.api.controller;

import com.mt.agent.workflow.api.service.ChatOrchestratorService;
import com.mt.agent.workflow.api.service.PythonExecutorService;
import com.mt.agent.workflow.api.service.impl.SubEventReporter;
import com.mt.agent.workflow.api.util.BufferUtil;
import com.mt.agent.workflow.api.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * NL2SQL本地执行器
 */
@Slf4j
@RestController
@RequestMapping("/api/test/nl2sql")
@CrossOrigin
public class TestNL2SQLController {
    
    @Autowired(required = false)
    private PythonExecutorService pythonExecutorService;
    
    @Autowired
    private BufferUtil bufferUtil;
    
    @Autowired
    private ChatOrchestratorService orchestratorService;
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("python_executor", pythonExecutorService != null ? "enabled" : "disabled");
        status.put("buffer_util", bufferUtil != null ? "enabled" : "disabled");
        status.put("orchestrator", orchestratorService != null ? "enabled" : "disabled");
        status.put("status", "healthy");
        
        return Result.success(status);
    }
}
