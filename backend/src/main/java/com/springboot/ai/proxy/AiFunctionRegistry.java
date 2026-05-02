package com.springboot.ai.proxy;

import java.util.Set;

import org.springframework.stereotype.Component;

/** AI Function 注册表 管理 Function Calling 白名单和权限控制 */
@Component
public class AiFunctionRegistry {

    private static final Set<String> ALLOWED_FUNCTIONS =
            Set.of(
                    "getAlertRecords",
                    "getDeviceStatus",
                    "getLifeguardOnDuty",
                    "getStatsSnapshot",
                    "getMonitorTasks");

    private static final Set<String> CONFIRM_REQUIRED_FUNCTIONS = Set.of();

    public boolean isAllowed(String functionName) {
        return ALLOWED_FUNCTIONS.contains(functionName);
    }

    public boolean requiresConfirmation(String functionName) {
        return CONFIRM_REQUIRED_FUNCTIONS.contains(functionName);
    }

    public Set<String> getFunctionNames() {
        return ALLOWED_FUNCTIONS;
    }

    public Set<String> getAllowedFunctions() {
        return ALLOWED_FUNCTIONS;
    }

    public Set<String> getConfirmRequiredFunctions() {
        return CONFIRM_REQUIRED_FUNCTIONS;
    }
}
