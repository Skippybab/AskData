package com.mt.agent.yaml;

import com.mt.agent.model.questionYaml.Question;
import com.mt.agent.model.questionYaml.QuestionConfig;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;

/**
 * YAML解析工具类
 */
@Slf4j
public class QuestionYamlParser {

    private static final Yaml yaml = new Yaml();

    /**
     * 从输入流解析YAML配置
     *
     * @param inputStream YAML文件输入流
     * @return 问题配置对象
     */
    public static QuestionConfig parse(InputStream inputStream) {
        try {
            return yaml.loadAs(inputStream, QuestionConfig.class);
        } catch (Exception e) {
            log.error("解析YAML配置失败", e);
            throw new RuntimeException("解析YAML配置失败", e);
        }
    }

    /**
     * 从YAML字符串解析配置
     *
     * @param yamlContent YAML字符串内容
     * @return 问题配置对象
     */
    public static QuestionConfig parse(String yamlContent) {
        try {
            return yaml.loadAs(yamlContent, QuestionConfig.class);
        } catch (Exception e) {
            log.error("解析YAML配置失败", e);
            throw new RuntimeException("解析YAML配置失败", e);
        }
    }

    /**
     * 从输入流解析问题列表
     *
     * @param inputStream YAML文件输入流
     * @return 问题列表
     */
    public static List<Question> parseQuestions(InputStream inputStream) {
        QuestionConfig config = parse(inputStream);
        return config.getQuestions();
    }

    /**
     * 从YAML字符串解析问题列表
     *
     * @param yamlContent YAML字符串内容
     * @return 问题列表
     */
    public static List<Question> parseQuestions(String yamlContent) {
        QuestionConfig config = parse(yamlContent);
        return config.getQuestions();
    }
}