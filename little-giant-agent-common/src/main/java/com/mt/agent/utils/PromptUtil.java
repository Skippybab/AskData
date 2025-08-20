package com.mt.agent.utils;

import com.mt.agent.model.questionYaml.Question;

import java.util.List;
import java.util.Map;

public class PromptUtil {

    /**
     * 典型问题列表转文本
     * @param questions
     * @Return: String
     * @Author: zzq
     * @Date: 2025/3/26 14:02
     */
    public static String convertQuestionsToText(List<Question> questions) {
        StringBuilder sb = new StringBuilder();
        for (Question question : questions) {
            sb.append("问题编号：").append(question.getId()).append(",");
            sb.append("问题名称：").append(question.getName());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 替换提示词参数
     *
     * @param template 提示词模板
     * @param params   参数Map
     * @return 替换后的提示词
     */
    public static String replacePromptParams(String template, Map<String, Object> params) {
        String prompt = template;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = "${" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());
            prompt = prompt.replace(key, value);
        }
        return prompt;
    }

}
