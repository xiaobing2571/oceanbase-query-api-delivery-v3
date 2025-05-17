package com.oceanbase.query.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryResultDto {
    private String templateId;
    private String version;
    private String status; // "success" or "failed"
    private List<Map<String, Object>> data;
    private Long executionTimeMs;
    private String errorMessage;
}

