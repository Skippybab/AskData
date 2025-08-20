package com.mt.agent.yaml;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mt.agent.model.workflow.WorkflowTemplate;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 工作流YAML配置解析器
 */
@Slf4j
public class WorkflowYamlParser {

    private static final ObjectMapper objectMapper;

    static {
        YAMLFactory yamlFactory = new YAMLFactory();
        objectMapper = new ObjectMapper(yamlFactory);
        // 忽略未知字段
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 从文件路径解析工作流配置
     *
     * @param filePath YAML文件路径
     * @return 工作流模板对象
     */
    public static WorkflowTemplate parse(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("YAML文件不存在: " + filePath);
            }
            return objectMapper.readValue(path.toFile(), WorkflowTemplate.class);
        } catch (IOException e) {
            log.error("解析工作流YAML配置失败: {}", filePath, e);
            throw new RuntimeException("解析工作流YAML配置失败", e);
        }
    }

    /**
     * 从输入流解析工作流配置
     *
     * @param inputStream YAML文件输入流
     * @return 工作流模板对象
     */
    public static WorkflowTemplate parse(InputStream inputStream) {
        try {
            return objectMapper.readValue(inputStream, WorkflowTemplate.class);
        } catch (IOException e) {
            log.error("解析工作流YAML配置失败", e);
            throw new RuntimeException("解析工作流YAML配置失败", e);
        }
    }

    /**
     * 从文件对象解析工作流配置
     *
     * @param file YAML文件对象
     * @return 工作流模板对象
     */
    public static WorkflowTemplate parse(File file) {
        try {
            return objectMapper.readValue(file, WorkflowTemplate.class);
        } catch (IOException e) {
            log.error("解析工作流YAML配置失败: {}", file.getPath(), e);
            throw new RuntimeException("解析工作流YAML配置失败", e);
        }
    }

    /**
     * 从YAML字符串解析工作流配置
     *
     * @param yamlContent YAML字符串内容
     * @return 工作流模板对象
     */
    public static WorkflowTemplate parseFromString(String yamlContent) {
        try {
            return objectMapper.readValue(yamlContent, WorkflowTemplate.class);
        } catch (IOException e) {
            log.error("解析工作流YAML配置失败", e);
            throw new RuntimeException("解析工作流YAML配置失败", e);
        }
    }
}
