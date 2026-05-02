package com.springboot.service.impl;

import java.util.Date;
import java.util.List;

import com.springboot.mapper.AiChatMessageMapper;
import com.springboot.model.entity.AiChatMessage;
import com.springboot.model.vo.AiChatMessageVO;
import com.springboot.service.AiChatMessageService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/** AI对话消息服务实现 */
@Service
public class AiChatMessageServiceImpl implements AiChatMessageService {

    @Resource private AiChatMessageMapper messageMapper;

    @Override
    public AiChatMessage saveMessage(
            Long conversationId,
            String role,
            String content,
            String functionName,
            String functionArgs,
            String functionResult,
            Integer tokensUsed) {
        AiChatMessage message = new AiChatMessage();
        message.setConversation_id(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setFunction_name(functionName);
        message.setFunction_args(functionArgs);
        message.setFunction_result(functionResult);
        message.setTokens_used(tokensUsed);
        message.setCreated_at(new Date());
        message.setIs_delete(0);
        messageMapper.insert(message);
        return message;
    }

    @Override
    public List<AiChatMessage> listMessagesByConversationId(Long conversationId) {
        QueryWrapper<AiChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderByAsc("created_at");
        return messageMapper.selectList(queryWrapper);
    }

    @Override
    public AiChatMessageVO getMessageVO(AiChatMessage message) {
        if (message == null) {
            return null;
        }
        AiChatMessageVO vo = new AiChatMessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setFunctionName(message.getFunction_name());
        vo.setFunctionArgs(message.getFunction_args());
        vo.setFunctionResult(message.getFunction_result());
        vo.setTokensUsed(message.getTokens_used());
        vo.setCreatedAt(
                message.getCreated_at() != null ? message.getCreated_at().toString() : null);
        return vo;
    }

    @Override
    public List<AiChatMessageVO> getMessageVOList(List<AiChatMessage> messages) {
        if (messages == null) {
            return List.of();
        }
        return messages.stream().map(this::getMessageVO).toList();
    }

    @Override
    public void clearMessages(Long conversationId, Long userId) {
        QueryWrapper<AiChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        List<AiChatMessage> messages = messageMapper.selectList(queryWrapper);
        for (AiChatMessage message : messages) {
            AiChatMessage update = new AiChatMessage();
            update.setId(message.getId());
            update.setIs_delete(1);
            messageMapper.updateById(update);
        }
    }
}
