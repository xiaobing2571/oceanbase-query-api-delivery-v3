package com.oceanbase.query.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasourceTestRequest {

    @NotBlank(message = "数据源类型不能为空")
    private String datasourceType; // direct, external, registered

    private DatasourceConfigDto datasourceConfig; // Only for 'direct' type

    private String externalSourceId; // Only for 'external' type

    private String datasourceId; // Only for 'registered' type

    private Map<String, Object> extraConfig; // JSON object for SSL, token etc.
}

