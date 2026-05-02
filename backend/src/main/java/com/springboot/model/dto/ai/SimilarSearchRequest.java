package com.springboot.model.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 相似报警搜索请求 */
@Data
public class SimilarSearchRequest {

    @NotBlank(message = "搜索内容不能为空")
    private String query;

    private Integer maxResults = 5;
}
