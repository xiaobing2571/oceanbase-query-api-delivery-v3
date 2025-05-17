package com.oceanbase.query.api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VersionMappingDto {

    @NotBlank(message = "版本匹配规则不能为空")
    private String version;

    @NotBlank(message = "SQL文本不能为空")
    private String sql;
}

