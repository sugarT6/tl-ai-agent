package com.tangl.tlaiagent.controller.AiController;

import com.tangl.tlaiagent.agent.TlManus;
import com.tangl.tlaiagent.app.LoveApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return loveApp.doChat(message, chatId);
    }

    /**
     * 异步获取聊天结果，通过 SSEEmitter的send方法 实现（以文本碎片的形式返回）
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/love_app/chat/sse/emitter")
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter emitter = new SseEmitter(180000L); // 3分钟超时
        // 获取 Flux 数据流并直接订阅
        loveApp.doChatWithStream(message, chatId)
                .subscribe(chuck -> {
                    try {
                        emitter.send(chuck);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }, emitter::completeWithError, emitter::complete);
        // 返回emitter
        return emitter;
    }

    /**
     * 流式调用manus智能体
     * @param message
     * @return
     */
    @GetMapping("/chat/manus")
    public SseEmitter doChatWithManus(String message){
        TlManus tlManus = new TlManus(allTools, dashscopeChatModel);
        return tlManus.runStream(message);
    }

}
