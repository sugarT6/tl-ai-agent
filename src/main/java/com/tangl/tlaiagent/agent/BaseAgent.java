package com.tangl.tlaiagent.agent;

import com.tangl.tlaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象基础代理类，用来管理代理状态和执行流程
 * 提供状态转移、内存管理和基于步骤的执行循环的基本功能
 * 子类必须实现step方法
 */

@Data
@Slf4j
public abstract class BaseAgent {
    // 核心属性
    private String name;
    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;
    // 状态
    private AgentState state = AgentState.IDLE;
    //执行控制
    private int maxStep = 10;
    private int currentStep = 0;
    // LLM
    private ChatClient chatClient;
    // memory(需要自主维护上下文对话记忆)
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 运行结果
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StringUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 更改状态
        state = AgentState.RUNNING;
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> resultList = new ArrayList<>();
        try {
            for (int i = 0; i < maxStep && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing Step " + stepNumber + "/" + maxStep);
                // 单步执行
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                resultList.add(result);
            }
            // 检查是否超出最大步数
            if (currentStep <= maxStep) {
                state = AgentState.FINISHED;
                resultList.add("Terminated: Reached max steps (" + maxStep + ")");
            }
            return String.join("\n", resultList);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Executing agent falied: " + e);
            return "执行错误" + e.getMessage();
        } finally {
            this.cleanup();
        }
    }

    /**
     * 执行单个步骤
     *
     * @return 执行结果
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法进行清理工作
    }
}
