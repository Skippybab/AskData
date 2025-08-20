package com.mt.agent.constant;

/**
 * 类描述
 *
 * @author lfz
 * @date 2025/5/23 9:16
 */
public interface Prompt {

    //判断用户是否赞同方案
    String AGREE_OR_DISAGREE_SCHEME = """
            已知对话历史为
            ```
            ${diag_history}
            ```
            
            已知规划的【任务方案】如下：
            ```
            ${plan}
            ```
            
            此时此刻，用户说的话是
            ```
            ${user_input}
            ```
            
            请判断用户是否认同【任务方案】，按以下要求进行返回：
            - 如果用户对【任务方案】完全认同，就直接输出"Y"
            - 如果用户对【任务方案】有调整或不认同，就直接输出"N"
            - 请直接输出结果，不需要其他回复其他内容
            """;

}
