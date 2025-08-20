package com.mt.agent.enums.consensus;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum QuestionFields {
    QUESTION_NUM("questionNum", "问题编号", String.class),
    QUESTION_NAME("questionName", "问题名称",String.class),
    QUESTION_STATUS("status", "问题状态", Integer.class),
    QUESTION_PARAMS("params", "问题参数", List.class);

    private final String fieldName;
    private final String name;
    private final Class<?> clazz;


    /**
     * 根据字段名获取枚举
     *
     * @param fieldName 字段名
     * @return QuestionFields
     */
    public static QuestionFields getByFieldName(String fieldName) {
        for (QuestionFields field : QuestionFields.values()) {
            if (field.fieldName.equals(fieldName)) {
                return field;
            }
        }
        return null;
    }
}
