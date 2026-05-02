package com.springboot.service;

import java.util.List;

import com.springboot.model.entity.AiChatMessage;
import com.springboot.model.vo.AiChatMessageVO;

/** AI对话消息服务接口 */
public interface AiChatMessageService {

    /** 保存消息 */
    AiChatMessage saveMessage(
            Long conversationId,
            String role,
            String content,
            String functionName,
            String functionArgs,
            String functionResult,
            Integer tokensUsed);

    /** 获取会话的消息历史 */
    List<AiChatMessage> listMessagesByConversationId(Long conversationId);

    /** 获取消息VO */
    AiChatMessageVO getMessageVO(AiChatMessage message);

    /** 批量获取消息VO */
    List<AiChatMessageVO> getMessageVOList(List<AiChatMessage> messages);

    /** 清空会话消息 */
    void clearMessages(Long conversationId, Long userId);
}
