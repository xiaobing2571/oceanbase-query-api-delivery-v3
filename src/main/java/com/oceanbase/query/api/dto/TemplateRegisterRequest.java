package com.oceanbase.query.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRegisterRequest {

    @NotBlank(message = "模板ID不能为空")
    private String templateId;

    @NotBlank(message = "场景代码不能为空")
    private String sceneCode;

    private String description;

    @NotBlank(message = "数据库类型不能为空")
    private String dbType; // e.g., OCEANBASE

    private List<TemplateParameterDto> parameters;

    @NotEmpty(message = "版本映射不能为空")
    private List<VersionMappingDto> versionMapping;
}

