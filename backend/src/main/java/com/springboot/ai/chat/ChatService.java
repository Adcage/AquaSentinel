package com.springboot.ai.chat;

import com.springboot.model.dto.ai.ChatResponse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI聊天服务接口 */
public interface ChatService {

    /** 同步对话（返回完整响应） */
    ChatResponse chat(Long userId, Long conversationId, String userMessage);

    /** 流式对话（返回 SSE 流） */
    SseEmitter chatStream(Long userId, Long conversationId, String userMessage);

    /** 纯对话（无上下文，用于报警分析等场景） */
    String chatWithSystemPrompt(String systemPrompt, String userMessage);
}
