package com.mt.agent.model.workflow;

import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
public class JavaExecutable {

    //步骤编号
    private Integer num;

    //输出结果名称
    private String outputName;

    //调用的系统函数名称
    private String functionName;

    //输入参数列表
    private List<String> inputs;

    //结果存取需要的数据编号，为空则不需要进行数据提取，如get(0)、xxx[0]
    private Integer resultDataIndex;

    // 指令类型
    private String javaExecutableType;

    @Getter
    public enum JavaExecutableType{
        FUNCTIOM_EXCUTION("function"),
        DATA_EXTRACTION("data"),
        DEFAULT_VALUE("default");

        private String type;

        JavaExecutableType(String type){
            this.type = type;
        }

        public String getType(){
            return type;
        }

    }
}
