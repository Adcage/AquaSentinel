package com.springboot.service;

import java.util.List;

import com.springboot.model.entity.AiChatConversation;
import com.springboot.model.vo.ConversationSummaryVO;

/** AI对话会话服务接口 */
public interface AiChatConversationService {

    /** 创建新会话 */
    AiChatConversation createConversation(Long userId, String title);

    /** 获取用户的会话列表 */
    List<ConversationSummaryVO> listConversationsByUserId(Long userId);

    /** 删除会话（权限校验） */
    void deleteConversation(Long id, Long userId);

    /** 更新会话标题 */
    void updateConversationTitle(Long id, Long userId, String title);

    /** 根据ID获取会话（权限校验） */
    AiChatConversation getByIdAndUserId(Long id, Long userId);

    /** 获取会话VO */
    ConversationSummaryVO getConversationVO(AiChatConversation conversation);
}
