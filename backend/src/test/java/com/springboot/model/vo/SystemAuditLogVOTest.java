package com.springboot.model.vo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import org.junit.jupiter.api.Test;

class SystemAuditLogVOTest {

    @Test
    void createdAtShouldSerializeAsGmtPlus8DateTimeString() throws Exception {
        SystemAuditLogVO vo = new SystemAuditLogVO();
        vo.setCreatedAt(new Date(0L));

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(vo);

        assertTrue(json.contains("\"createdAt\":\"1970-01-01 08:00:00\""));
    }
}
