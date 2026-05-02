package com.springboot.ai.chat;

import java.util.List;

import com.springboot.config.AppAiIntelligenceProperties;
import com.springboot.model.dto.ai.ChatResponse;
import com.springboot.model.entity.AiChatConversation;
import com.springboot.model.entity.AiChatMessage;
import com.springboot.service.AiChatConversationService;
import com.springboot.service.AiChatMessageService;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI聊天服务默认实现（使用Spring AI ChatClient） */
@Slf4j
@Service
@ConditionalOnProperty(
        name = "app.ai.intelligence.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DefaultChatService implements ChatService {

    @Resource private ChatClient chatClient;

    @Resource private AppAiIntelligenceProperties aiProperties;

    @Resource private AiChatConversationService conversationService;

    @Resource private AiChatMessageService messageService;

    @Override
    public ChatResponse chat(Long userId, Long conversationId, String userMessage) {
        AiChatConversation conversation = ensureConversation(userId, conversationId);

        messageService.saveMessage(
                conversation.getId(), "user", userMessage, null, null, null, null);

        List<Message> messages = buildSpringAiMessages(conversation.getId(), userMessage);

        try {
            String assistantContent =
                    chatClient
                            .prompt()
                            .messages(messages)
                            .functions(
                                    "getAlertRecords",
                                    "getDeviceStatus",
                                    "getLifeguardOnDuty",
                                    "getStatsSnapshot",
                                    "getMonitorTasks")
                            .call()
                            .content();

            if (assistantContent == null || assistantContent.isEmpty()) {
                assistantContent = "AI服务暂时不可用，请稍后再试。";
            }

            messageService.saveMessage(
                    conversation.getId(), "assistant", assistantContent, null, null, null, null);
            updateConversationTimestamp(conversation.getId());

            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setConversationId(conversation.getId());
            chatResponse.setMessage(assistantContent);
            return chatResponse;
        } catch (Exception e) {
            log.error("AI对话失败: userId={}, conversationId={}", userId, conversationId, e);
            String fallbackMessage = "AI服务暂时不可用，请稍后再试。";
            messageService.saveMessage(
                    conversation.getId(), "assistant", fallbackMessage, null, null, null, null);

            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setConversationId(conversation.getId());
            chatResponse.setMessage(fallbackMessage);
            return chatResponse;
        }
    }

    @Override
    public SseEmitter chatStream(Long userId, Long conversationId, String userMessage) {
        SseEmitter emitter = new SseEmitter(120000L);

        AiChatConversation conversation = ensureConversation(userId, conversationId);
        messageService.saveMessage(
                conversation.getId(), "user", userMessage, null, null, null, null);

        StringBuilder fullResponse = new StringBuilder();

        emitter.onCompletion(
                () -> {
                    log.info("SSE流完成: conversationId={}", conversation.getId());
                    updateConversationTimestamp(conversation.getId());
                });

        emitter.onTimeout(
                () -> {
                    log.warn("SSE流超时: conversationId={}", conversation.getId());
                    emitter.complete();
                });

        emitter.onError(
                e -> {
                    log.error(
                            "SSE流错误: conversationId={}, error={}",
                            conversation.getId(),
                            e.getMessage());
                });

        List<Message> messages = buildSpringAiMessages(conversation.getId(), userMessage);

        try {
            chatClient
                    .prompt()
                    .messages(messages)
                    .functions(
                            "getAlertRecords",
                            "getDeviceStatus",
                            "getLifeguardOnDuty",
                            "getStatsSnapshot",
                            "getMonitorTasks")
                    .stream()
                    .content()
                    .doOnNext(
                            chunk -> {
                                fullResponse.append(chunk);
                                try {
                                    emitter.send(SseEmitter.event().data(chunk));
                                } catch (Exception ex) {
                                    log.error("SSE发送失败: {}", ex.getMessage());
                                }
                            })
                    .doOnComplete(
                            () -> {
                                try {
                                    messageService.saveMessage(
                                            conversation.getId(),
                                            "assistant",
                                            fullResponse.toString(),
                                            null,
                                            null,
                                            null,
                                            null);
                                    emitter.complete();
                                } catch (Exception ex) {
                                    log.error("保存消息失败: {}", ex.getMessage());
                                }
                            })
                    .doOnError(
                            error -> {
                                log.error("流式对话失败: {}", error.getMessage());
                                try {
                                    emitter.send(SseEmitter.event().data("AI服务暂时不可用，请稍后再试。"));
                                    emitter.complete();
                                } catch (Exception ex) {
                                    emitter.completeWithError(ex);
                                }
                            })
                    .subscribe();
        } catch (Exception e) {
            log.error("启动流式对话失败: {}", e.getMessage());
            try {
                emitter.send(SseEmitter.event().data("AI服务暂时不可用，请稍后再试。"));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }

        return emitter;
    }

    @Override
    public String chatWithSystemPrompt(String systemPrompt, String userMessage) {
        try {
            return chatClient.prompt().system(systemPrompt).user(userMessage).call().content();
        } catch (Exception e) {
            log.error("纯对话失败: {}", e.getMessage());
            return null;
        }
    }

    private AiChatConversation ensureConversation(Long userId, Long conversationId) {
        if (conversationId == null || conversationId <= 0) {
            return conversationService.createConversation(userId, null);
        }
        AiChatConversation existing = conversationService.getByIdAndUserId(conversationId, userId);
        if (existing == null) {
            return conversationService.createConversation(userId, null);
        }
        return existing;
    }

    private List<Message> buildSpringAiMessages(Long conversationId, String currentMessage) {
        List<Message> messages = new java.util.ArrayList<>();

        messages.add(new SystemMessage(aiProperties.getChatSystemPrompt()));

        List<AiChatMessage> history = messageService.listMessagesByConversationId(conversationId);
        int maxHistory = aiProperties.getChatHistory().getMaxMessagesPerConversation();
        int startIndex = Math.max(0, history.size() - maxHistory);

        for (int i = startIndex; i < history.size(); i++) {
            AiChatMessage msg = history.get(i);
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        messages.add(new UserMessage(currentMessage));
        return messages;
    }

    private void updateConversationTimestamp(Long conversationId) {
        conversationService.updateConversationTitle(conversationId, null, null);
    }
}
