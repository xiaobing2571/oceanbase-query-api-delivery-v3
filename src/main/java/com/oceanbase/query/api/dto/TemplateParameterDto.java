package com.oceanbase.query.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateParameterDto {

    @NotBlank(message = "参数名称不能为空")
    private String name;

    @NotBlank(message = "参数类型不能为空")
    private String type;

    @NotNull(message = "是否必填不能为空")
    private Boolean required;

    private String expression; // 校验表达式 (预留)
}

