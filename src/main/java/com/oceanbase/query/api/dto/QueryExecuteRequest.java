package com.oceanbase.query.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryExecuteRequest {

    @NotBlank(message = "数据源类型不能为空")
    private String datasourceType; // direct, external, registered

    private DatasourceConfigDto datasourceConfig; // For 'direct' type

    private String externalSourceId; // For 'external' type

    private String datasourceId; // For 'registered' type

    // scene_code is part of the path, so not needed in request body as per design doc
    // private String sceneCode; 

    private String tenant; // As per design doc, though not explicitly in table, can be used in SQL or logic

    private String startTime; // ISO 8601 format: "2025-05-01T00:00:00Z"

    private String endTime; // ISO 8601 format: "2025-05-01T23:59:59Z"

    private Map<String, Object> extraParams; // For user-defined template parameters

    private Boolean async = false; // Default to synchronous execution

    private Long executionTimeoutMs; // Query execution timeout in milliseconds

    private String callbackUrl; // Callback URL for asynchronous tasks
}

