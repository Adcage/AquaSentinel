package com.springboot.service.impl;

import java.util.Date;
import java.util.List;

import com.springboot.config.AppAiIntelligenceProperties;
import com.springboot.mapper.AiChatConversationMapper;
import com.springboot.model.entity.AiChatConversation;
import com.springboot.model.vo.ConversationSummaryVO;
import com.springboot.service.AiChatConversationService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/** AI对话会话服务实现 */
@Service
public class AiChatConversationServiceImpl implements AiChatConversationService {

    @Resource private AiChatConversationMapper conversationMapper;

    @Resource private AppAiIntelligenceProperties aiProperties;

    @Override
    public AiChatConversation createConversation(Long userId, String title) {
        AiChatConversation conversation = new AiChatConversation();
        conversation.setUser_id(userId);
        conversation.setTitle(title != null ? title : "新对话");
        conversation.setCreated_at(new Date());
        conversation.setUpdated_at(new Date());
        conversation.setIs_delete(0);
        conversationMapper.insert(conversation);
        return conversation;
    }

    @Override
    public List<ConversationSummaryVO> listConversationsByUserId(Long userId) {
        QueryWrapper<AiChatConversation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderByDesc("updated_at");
        queryWrapper.last("LIMIT " + aiProperties.getChatHistory().getMaxConversationsPerUser());
        List<AiChatConversation> conversations = conversationMapper.selectList(queryWrapper);
        return conversations.stream().map(this::getConversationVO).toList();
    }

    @Override
    public void deleteConversation(Long id, Long userId) {
        AiChatConversation conversation = getByIdAndUserId(id, userId);
        if (conversation == null) {
            return;
        }
        AiChatConversation update = new AiChatConversation();
        update.setId(id);
        update.setIs_delete(1);
        update.setUpdated_at(new Date());
        conversationMapper.updateById(update);
    }

    @Override
    public void updateConversationTitle(Long id, Long userId, String title) {
        AiChatConversation conversation = getByIdAndUserId(id, userId);
        if (conversation == null) {
            return;
        }
        AiChatConversation update = new AiChatConversation();
        update.setId(id);
        update.setTitle(title);
        update.setUpdated_at(new Date());
        conversationMapper.updateById(update);
    }

    @Override
    public AiChatConversation getByIdAndUserId(Long id, Long userId) {
        QueryWrapper<AiChatConversation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("is_delete", 0);
        return conversationMapper.selectOne(queryWrapper);
    }

    @Override
    public ConversationSummaryVO getConversationVO(AiChatConversation conversation) {
        if (conversation == null) {
            return null;
        }
        ConversationSummaryVO vo = new ConversationSummaryVO();
        vo.setId(conversation.getId());
        vo.setTitle(conversation.getTitle());
        vo.setCreatedAt(
                conversation.getCreated_at() != null
                        ? conversation.getCreated_at().toString()
                        : null);
        vo.setUpdatedAt(
                conversation.getUpdated_at() != null
                        ? conversation.getUpdated_at().toString()
                        : null);
        return vo;
    }
}
