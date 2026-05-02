package com.springboot.ai.controller;

import java.util.List;
import java.util.Objects;

import com.springboot.ai.analysis.DrowningAlertAnalysisService;
import com.springboot.ai.chat.ChatService;
import com.springboot.ai.embedding.SimilarAlertSearchService;
import com.springboot.annotation.AuthCheck;
import com.springboot.common.BaseResponse;
import com.springboot.common.ErrorCode;
import com.springboot.common.ResultUtils;
import com.springboot.constant.RoleConstant;
import com.springboot.exception.BusinessException;
import com.springboot.model.dto.ai.ChatRequest;
import com.springboot.model.dto.ai.ChatResponse;
import com.springboot.model.dto.ai.CreateConversationRequest;
import com.springboot.model.dto.ai.SimilarSearchRequest;
import com.springboot.model.entity.AiChatConversation;
import com.springboot.model.entity.AiChatMessage;
import com.springboot.model.entity.AlertEmbedding;
import com.springboot.model.entity.AlertRecord;
import com.springboot.model.vo.AiChatMessageVO;
import com.springboot.model.vo.ConversationSummaryVO;
import com.springboot.model.vo.SimilarAlertVO;
import com.springboot.ratelimit.RateLimit;
import com.springboot.security.AuthContextHolder;
import com.springboot.security.AuthUserContext;
import com.springboot.security.JwtTokenProvider;
import com.springboot.service.AiChatConversationService;
import com.springboot.service.AiChatMessageService;
import com.springboot.service.AlertRecordService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** AI 对话 REST API 控制器 */
@Slf4j
@RestController
@RequestMapping("/ai")
@ConditionalOnProperty(
        name = "app.ai.intelligence.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AiChatController {

    @Resource private ChatService chatService;

    @Resource private AiChatConversationService conversationService;

    @Resource private AiChatMessageService messageService;

    @Resource private AlertRecordService alertRecordService;

    @Resource private SimilarAlertSearchService similarAlertSearchService;

    @Resource private DrowningAlertAnalysisService drowningAlertAnalysisService;

    @Resource private JwtTokenProvider jwtTokenProvider;

    @RateLimit(capacity = 10, refillRate = 10, refillPeriodSeconds = 60, keyType = "USER")
    @PostMapping("/chat")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<ChatResponse> chat(@RequestBody @Valid ChatRequest request) {
        Long userId = getCurrentUserId();
        ChatResponse response =
                chatService.chat(userId, request.getConversationId(), request.getMessage());
        return ResultUtils.success(response);
    }

    @RateLimit(capacity = 10, refillRate = 10, refillPeriodSeconds = 60, keyType = "USER")
    @GetMapping("/chat/stream")
    public SseEmitter chatStream(
            @RequestParam(required = false) Long conversationId,
            @RequestParam String message,
            @RequestParam(required = false) String token) {
        Long userId = resolveUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return chatService.chatStream(userId, conversationId, message);
    }

    private Long resolveUserIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            AuthUserContext context = AuthContextHolder.get();
            if (context != null && context.getUserId() != null) {
                return context.getUserId();
            }
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        try {
            AuthUserContext context = jwtTokenProvider.parseAccessToken(token);
            return context.getUserId();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "无效的访问令牌");
        }
    }

    private Long getCurrentUserId() {
        AuthUserContext context = AuthContextHolder.get();
        if (context == null || context.getUserId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return context.getUserId();
    }

    @GetMapping("/conversations")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<List<ConversationSummaryVO>> listConversations() {
        Long userId = getCurrentUserId();
        return ResultUtils.success(conversationService.listConversationsByUserId(userId));
    }

    @PostMapping("/conversations")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<ConversationSummaryVO> createConversation(
            @RequestBody CreateConversationRequest request) {
        Long userId = getCurrentUserId();
        AiChatConversation conversation =
                conversationService.createConversation(userId, request.getTitle());
        return ResultUtils.success(conversationService.getConversationVO(conversation));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<List<AiChatMessageVO>> getMessages(@PathVariable Long conversationId) {
        Long userId = getCurrentUserId();
        AiChatConversation conversation =
                conversationService.getByIdAndUserId(conversationId, userId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "会话不存在或无权限");
        }
        List<AiChatMessage> messages = messageService.listMessagesByConversationId(conversationId);
        return ResultUtils.success(messageService.getMessageVOList(messages));
    }

    @DeleteMapping("/conversations/{conversationId}")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<Void> deleteConversation(@PathVariable Long conversationId) {
        Long userId = getCurrentUserId();
        conversationService.deleteConversation(conversationId, userId);
        return ResultUtils.success(null);
    }

    @RateLimit(capacity = 5, refillRate = 5, refillPeriodSeconds = 60, keyType = "USER")
    @PostMapping("/alerts/{alertId}/analyze")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<String> analyzeAlert(@PathVariable Long alertId) {
        AlertRecord alert = alertRecordService.getById(alertId);
        if (alert == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "报警不存在");
        }
        String analysis = drowningAlertAnalysisService.analyzeAlert(alertId);
        if (analysis == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI分析失败");
        }
        try {
            similarAlertSearchService.generateAndStoreEmbedding(alert);
        } catch (Exception e) {
            log.warn("报警向量嵌入生成失败: alertId={}, error={}", alertId, e.getMessage());
        }
        return ResultUtils.success(analysis);
    }

    @RateLimit(capacity = 5, refillRate = 5, refillPeriodSeconds = 60, keyType = "USER")
    @PostMapping("/alerts/search-similar")
    @AuthCheck(mustRole = RoleConstant.VENUE_ADMIN)
    public BaseResponse<List<SimilarAlertVO>> searchSimilarAlerts(
            @Valid @RequestBody SimilarSearchRequest request) {
        List<AlertEmbedding> similar =
                similarAlertSearchService.searchByText(request.getQuery(), request.getMaxResults());
        List<SimilarAlertVO> vos =
                similar.stream()
                        .map(
                                emb -> {
                                    AlertRecord alert =
                                            alertRecordService.getById(emb.getAlert_id());
                                    if (alert == null) return null;
                                    SimilarAlertVO vo = new SimilarAlertVO();
                                    vo.setAlertId(alert.getId());
                                    vo.setAlertUid(alert.getAlert_uid());
                                    vo.setAlertType(alert.getAlert_type());
                                    vo.setAlertStatus(alert.getAlert_status());
                                    vo.setIncidentLocation(alert.getIncident_location());
                                    vo.setDetectionResult(alert.getDetection_result());
                                    vo.setCreatedAt(
                                            alert.getCreated_at() != null
                                                    ? alert.getCreated_at().toString()
                                                    : null);
                                    return vo;
                                })
                        .filter(Objects::nonNull)
                        .toList();
        return ResultUtils.success(vos);
    }
}
