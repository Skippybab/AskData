package com.mt.agent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Python执行结果处理器
 * 负责智能识别和处理Python代码执行的结果变量
 */
@Slf4j
@Component
public class PythonResultHandler {
    
    private final ObjectMapper objectMapper;
    
    // 常见的结果变量名模式（按优先级排序）
    private static final List<String> COMMON_RESULT_PATTERNS = Arrays.asList(
        "result", "results", "data", "query_result", "sql_result",
        "net_profit_margin", "total_profit", "average_profit",
        "profit", "revenue", "sales", "amount", "count", "sum", "avg",
        "margin", "growth_rate", "ratio", "percentage", "total", "value"
    );
    
    // 结果变量名后缀模式
    private static final List<String> RESULT_SUFFIXES = Arrays.asList(
        "_result", "_data", "_list", "_margin", "_profit", "_revenue",
        "_sales", "_amount", "_count", "_sum", "_avg", "_total",
        "_ratio", "_rate", "_percentage", "_value"
    );
    
    public PythonResultHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * 生成动态结果识别的Python代码
     */
    public String generateResultHandlingCode() {
        return """
            # 动态识别并输出用户定义的变量
            import json
            import sys
            
            def find_and_output_result():
                # 获取所有局部变量
                local_vars = locals()
                
                # 过滤系统变量和函数
                system_vars = {'bridge', 'report', 'execute_query_and_get_json', 'gen_sql', 
                              'json', 'sys', 'traceback', 'main', 'local_vars', 'find_and_output_result',
                              'params', '__builtins__', '__name__', '__doc__', '__package__',
                              '__loader__', '__spec__', '__annotations__', '__cached__', '__file__'}
                
                user_vars = {}
                for k, v in local_vars.items():
                    if not k.startswith('_') and k not in system_vars and not callable(v):
                        user_vars[k] = v
                
                # 记录所有用户变量用于调试
                if user_vars:
                    print(f"DEBUG: 检测到用户变量: {list(user_vars.keys())}", file=sys.stderr)
                
                # 查找可能的结果变量
                result_var = None
                result_value = None
                
                # 1. 首先查找常见的结果变量名
                common_names = %s
                for var_name in common_names:
                    if var_name in user_vars:
                        result_var = var_name
                        result_value = user_vars[var_name]
                        print(f"DEBUG: 找到常见结果变量: {var_name}", file=sys.stderr)
                        break
                
                # 2. 如果没找到，查找包含特定后缀的变量
                if result_var is None:
                    suffixes = %s
                    for var_name, var_value in user_vars.items():
                        for suffix in suffixes:
                            if var_name.lower().endswith(suffix):
                                result_var = var_name
                                result_value = var_value
                                print(f"DEBUG: 找到后缀匹配变量: {var_name}", file=sys.stderr)
                                break
                        if result_var:
                            break
                
                # 3. 如果还是没找到，查找包含数据的变量（list, dict类型）
                if result_var is None:
                    for var_name, var_value in user_vars.items():
                        if isinstance(var_value, (list, dict)) and var_value:
                            result_var = var_name
                            result_value = var_value
                            print(f"DEBUG: 找到数据类型变量: {var_name}", file=sys.stderr)
                            break
                
                # 4. 最后选择第一个非空的用户变量
                if result_var is None and user_vars:
                    for var_name, var_value in user_vars.items():
                        if var_value is not None and var_value != "":
                            result_var = var_name
                            result_value = var_value
                            print(f"DEBUG: 使用第一个非空变量: {var_name}", file=sys.stderr)
                            break
                
                # 输出结果
                if result_var is not None and result_value is not None:
                    try:
                        # 构造统一的响应格式
                        response_data = {
                            "success": True,
                            "dataType": "python_result",
                            "variableName": result_var,
                            "variableType": type(result_value).__name__
                        }
                        
                        # 根据数据类型处理
                        if isinstance(result_value, list):
                            response_data["dataType"] = "python_dict_list"
                            response_data["parsedData"] = json.dumps(result_value, ensure_ascii=False, default=str)
                            response_data["rowCount"] = len(result_value)
                        elif isinstance(result_value, dict):
                            response_data["dataType"] = "python_dict"
                            response_data["parsedData"] = json.dumps(result_value, ensure_ascii=False, default=str)
                        elif isinstance(result_value, (int, float)):
                            response_data["dataType"] = "python_number"
                            response_data["parsedData"] = json.dumps({result_var: result_value}, ensure_ascii=False)
                            response_data["value"] = result_value
                        elif isinstance(result_value, str):
                            response_data["dataType"] = "python_string"
                            response_data["parsedData"] = json.dumps({result_var: result_value}, ensure_ascii=False)
                            response_data["value"] = result_value
                        else:
                            # 尝试转换为JSON
                            try:
                                response_data["parsedData"] = json.dumps(result_value, ensure_ascii=False, default=str)
                            except:
                                response_data["parsedData"] = str(result_value)
                        
                        # 输出JSON结果
                        print(json.dumps(response_data, ensure_ascii=False))
                        
                    except Exception as e:
                        print(f"ERROR: 结果序列化失败: {e}", file=sys.stderr)
                        # 降级输出
                        print(f"{result_var}: {result_value}")
                else:
                    # 没有找到结果变量
                    response_data = {
                        "success": False,
                        "dataType": "no_result",
                        "message": "代码执行完成，但未找到结果变量",
                        "availableVars": list(user_vars.keys()) if user_vars else []
                    }
                    print(json.dumps(response_data, ensure_ascii=False))
            
            # 调用结果处理函数
            find_and_output_result()
            """.formatted(
                formatPythonList(COMMON_RESULT_PATTERNS),
                formatPythonList(RESULT_SUFFIXES)
            );
    }
    
    /**
     * 将Java List转换为Python列表字符串
     */
    private String formatPythonList(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append("'").append(list.get(i)).append("'");
            if (i < list.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * 解析Python执行的输出结果
     */
    public Map<String, Object> parseExecutionOutput(String output) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        
        if (output == null || output.trim().isEmpty()) {
            result.put("error", "执行无输出");
            return result;
        }
        
        try {
            // 尝试解析为JSON
            if (output.contains("{") && output.contains("}")) {
                // 提取JSON部分
                int startIdx = output.indexOf("{");
                int endIdx = output.lastIndexOf("}") + 1;
                
                if (startIdx >= 0 && endIdx > startIdx) {
                    String jsonStr = output.substring(startIdx, endIdx);
                    Map<String, Object> jsonResult = objectMapper.readValue(jsonStr, Map.class);
                    
                    // 合并结果
                    result.putAll(jsonResult);
                    
                    // 解析嵌套的parsedData
                    if (jsonResult.containsKey("parsedData")) {
                        String parsedDataStr = (String) jsonResult.get("parsedData");
                        try {
                            Object parsedData = objectMapper.readValue(parsedDataStr, Object.class);
                            result.put("data", parsedData);
                        } catch (Exception e) {
                            log.debug("解析parsedData失败，保留原始字符串: {}", e.getMessage());
                            result.put("data", parsedDataStr);
                        }
                    }
                    
                    return result;
                }
            }
            
            // 如果不是JSON格式，作为纯文本处理
            result.put("success", true);
            result.put("dataType", "text");
            result.put("data", output);
            
        } catch (Exception e) {
            log.error("解析Python输出失败: {}", e.getMessage(), e);
            result.put("error", "输出解析失败: " + e.getMessage());
            result.put("rawOutput", output);
        }
        
        return result;
    }
    
    /**
     * 从Python代码中提取可能的结果变量名
     */
    public Set<String> extractPotentialResultVariables(String pythonCode) {
        Set<String> variables = new HashSet<>();
        
        if (pythonCode == null || pythonCode.isEmpty()) {
            return variables;
        }
        
        // 匹配赋值语句 (variable = ...)
        Pattern assignmentPattern = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=", Pattern.MULTILINE);
        Matcher matcher = assignmentPattern.matcher(pythonCode);
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            // 过滤掉一些明显的临时变量
            if (!varName.startsWith("_") && !varName.equals("i") && !varName.equals("j") 
                && !varName.equals("k") && !varName.equals("temp") && !varName.equals("tmp")) {
                variables.add(varName);
            }
        }
        
        log.debug("从Python代码中提取到潜在的结果变量: {}", variables);
        return variables;
    }
    
    /**
     * 判断变量名是否可能是结果变量
     */
    public boolean isProbableResultVariable(String varName) {
        if (varName == null || varName.isEmpty()) {
            return false;
        }
        
        String lowerName = varName.toLowerCase();
        
        // 检查是否匹配常见模式
        for (String pattern : COMMON_RESULT_PATTERNS) {
            if (lowerName.equals(pattern) || lowerName.contains(pattern)) {
                return true;
            }
        }
        
        // 检查是否包含结果后缀
        for (String suffix : RESULT_SUFFIXES) {
            if (lowerName.endsWith(suffix)) {
                return true;
            }
        }
        
        return false;
    }
}
