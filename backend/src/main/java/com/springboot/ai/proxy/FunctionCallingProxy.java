package com.springboot.ai.proxy;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Function Calling 安全代理 负责 Function 调用的权限校验和审计日志记录 */
@Slf4j
@Component
public class FunctionCallingProxy {

    @Resource private AiFunctionRegistry functionRegistry;

    @Resource private ObjectMapper objectMapper;

    public boolean validateFunctionCall(String functionName, Map<String, Object> args) {
        if (!functionRegistry.isAllowed(functionName)) {
            log.warn("Function调用被拒绝: functionName={}, 原因=不在白名单中", functionName);
            return false;
        }
        return true;
    }

    public boolean requiresConfirmation(String functionName) {
        return functionRegistry.requiresConfirmation(functionName);
    }

    public void logFunctionCall(
            Long userId, String functionName, Map<String, Object> args, Object result) {
        try {
            String argsJson = args != null ? objectMapper.writeValueAsString(args) : "null";
            String resultJson = result != null ? objectMapper.writeValueAsString(result) : "null";

            log.info(
                    "Function调用审计: userId={}, functionName={}, args={}, result={}, timestamp={}",
                    userId,
                    functionName,
                    argsJson,
                    truncate(resultJson, 500),
                    new Date());
        } catch (Exception e) {
            log.warn("Function调用审计日志记录失败: {}", e.getMessage());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
