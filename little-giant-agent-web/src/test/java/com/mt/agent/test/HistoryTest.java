package com.mt.agent.test;

import com.mt.agent.repository.consensus.service.IConsConversationRecordService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class HistoryTest {
    @Autowired
    private IConsConversationRecordService conversationRecordService;

    @Test
    public void test() {
        String userId = "4";
        String historyLogs = conversationRecordService.getHistoryLogs(userId);
        log.info(historyLogs);

    }
}
