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
public class SceneRegisterRequest {

    @NotBlank(message = "场景代码不能为空")
    private String sceneCode;

    private String sceneName;

    private String description;

    @NotEmpty(message = "模板ID列表不能为空")
    private List<String> templateIds;
}

