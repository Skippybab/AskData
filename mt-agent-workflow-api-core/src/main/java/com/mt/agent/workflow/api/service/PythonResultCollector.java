package com.mt.agent.workflow.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.agent.workflow.api.dto.PythonExecutionResult;
import com.mt.agent.workflow.api.entity.ChatMessage;
import com.mt.agent.workflow.api.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Collects and processes the result from a Python script execution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PythonResultCollector {

    private final ChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper;

    /**
     * Collects the result from the process, updates the database, and returns the structured result.
     *
     * @param process     The running process of the Python script.
     * @param chatMessage The chat message entity associated with this execution.
     * @return A PythonExecutionResult object.
     */
    public PythonExecutionResult collectResult(Process process, ChatMessage chatMessage) {
        try {
            String stdOut = readStream(process.getInputStream());
            String stdErr = readStream(process.getErrorStream());

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                log.info("Python script executed successfully for messageId: {}.", chatMessage.getId());
                updateExecutionResult(chatMessage, stdOut, true);
                return PythonExecutionResult.success(stdOut);
            } else {
                log.error("Python script execution failed for messageId: {} with exit code: {}. Error: {}",
                        chatMessage.getId(), exitCode, stdErr);
                updateExecutionResult(chatMessage, stdErr, false);
                return PythonExecutionResult.failure(stdErr, "EXECUTION_ERROR");
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error while collecting python execution result for messageId: {}", chatMessage.getId(), e);
            updateExecutionResult(chatMessage, e.getMessage(), false);
            return PythonExecutionResult.failure(e.getMessage(), "COLLECTION_ERROR");
        }
    }

    /**
     * Reads the content from an InputStream.
     *
     * @param inputStream The stream to read from.
     * @return The content of the stream as a String.
     * @throws IOException If an I/O error occurs.
     */
    private String readStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    /**
     * Updates the chat message entity with the execution result.
     *
     * @param chatMessage The entity to update.
     * @param result      The result string (data or error).
     * @param success     The status of the execution.
     */
    private void updateExecutionResult(ChatMessage chatMessage, String result, boolean success) {
        try {
            chatMessage.setExecutionResult(result);
            chatMessage.setExecutionStatus(success ? 1 : 2); // 1=成功, 2=失败
            chatMessage.setStatus(success ? 1 : 2); // 更新主状态字段
            if (!success) {
                chatMessage.setErrorMessage(result);
            }
            chatMessageMapper.updateById(chatMessage);
            log.info("Updated execution result for messageId: {}, success: {}", chatMessage.getId(), success);
        } catch (Exception e) {
            log.error("Failed to update execution result in database for messageId: {}", chatMessage.getId(), e);
        }
    }
}
