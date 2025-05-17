package com.oceanbase.query.api.service;

import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.SceneDto; // Assuming a SceneDto exists or will be created
import com.oceanbase.query.api.dto.SceneRegisterRequest;

import java.util.List;

public interface SceneService {

    ApiResponse<SceneDto> registerScene(SceneRegisterRequest request);

    ApiResponse<SceneDto> getScene(String sceneCode);

    ApiResponse<Void> deleteScene(String sceneCode);

    ApiResponse<List<SceneDto>> listScenes();

    ApiResponse<SceneDto> updateScene(String sceneCode, SceneRegisterRequest request);
}

