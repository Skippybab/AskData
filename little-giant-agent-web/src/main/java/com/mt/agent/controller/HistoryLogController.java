package com.mt.agent.controller;

import com.mt.agent.repository.consensus.service.IConsConversationRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/history")
@Slf4j
@RequiredArgsConstructor
public class HistoryLogController {

    private final IConsConversationRecordService consConversationRecordService;

//    @GetMapping

}
