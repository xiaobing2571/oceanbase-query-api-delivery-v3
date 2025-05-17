package com.oceanbase.query.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.query.api.domain.entity.QueryScene;
import com.oceanbase.query.api.domain.repository.QuerySceneRepository;
import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.SceneDto;
import com.oceanbase.query.api.dto.SceneRegisterRequest;
import com.oceanbase.query.api.service.SceneService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SceneServiceImpl implements SceneService {

    @Autowired
    private QuerySceneRepository querySceneRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private SceneDto convertToDto(QueryScene scene) {
        SceneDto dto = new SceneDto();
        BeanUtils.copyProperties(scene, dto);
        try {
            if (scene.getTemplateIds() != null && !scene.getTemplateIds().isEmpty()) {
                dto.setTemplateIds(objectMapper.readValue(scene.getTemplateIds(), new TypeReference<List<String>>() {}));
            }
        } catch (JsonProcessingException e) {
            log.warn("反序列化场景模板ID列表失败 for sceneCode {}: {}", scene.getSceneCode(), e.getMessage());
        }
        return dto;
    }

    @Override
    @Transactional
    public ApiResponse<SceneDto> registerScene(SceneRegisterRequest request) {
        if (querySceneRepository.existsBySceneCode(request.getSceneCode())) {
            return ApiResponse.error("30002", "场景代码已存在: " + request.getSceneCode());
        }

        QueryScene queryScene = new QueryScene();
        queryScene.setSceneCode(request.getSceneCode());
        queryScene.setSceneName(request.getSceneName());
        queryScene.setDescription(request.getDescription());

        try {
            if (request.getTemplateIds() != null && !request.getTemplateIds().isEmpty()) {
                queryScene.setTemplateIds(objectMapper.writeValueAsString(request.getTemplateIds()));
            }
        } catch (JsonProcessingException e) {
            log.error("序列化场景模板ID列表失败 for sceneCode {}: {}", request.getSceneCode(), e.getMessage());
            return ApiResponse.error("40001", "序列化模板ID列表失败: " + e.getMessage());
        }

        QueryScene savedScene = querySceneRepository.save(queryScene);
        log.info("查询场景已注册: {}", savedScene.getSceneCode());

        return ApiResponse.success(convertToDto(savedScene));
    }

    @Override
    public ApiResponse<SceneDto> getScene(String sceneCode) {
        return querySceneRepository.findBySceneCode(sceneCode)
                .map(scene -> ApiResponse.success(convertToDto(scene)))
                .orElseGet(() -> ApiResponse.error("20001", "未找到场景: " + sceneCode));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteScene(String sceneCode) {
        if (!querySceneRepository.existsBySceneCode(sceneCode)) {
            return ApiResponse.error("20001", "未找到要删除的场景: " + sceneCode);
        }
        try {
            querySceneRepository.deleteBySceneCode(sceneCode);
            log.info("场景已删除: {}", sceneCode);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除场景失败: {}", sceneCode, e);
            return ApiResponse.error("20003", "删除场景时发生内部错误: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<List<SceneDto>> listScenes() {
        try {
            List<SceneDto> dtos = querySceneRepository.findAll().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            return ApiResponse.<List<SceneDto>>builder()
                    .status("success")
                    .data(dtos)
                    .build();
        } catch (Exception e) {
            log.error("列出所有场景失败", e);
            return ApiResponse.error("20004", "列出所有场景时发生内部错误: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<SceneDto> updateScene(String sceneCode, SceneRegisterRequest request) {
        Optional<QueryScene> existingSceneOpt = querySceneRepository.findBySceneCode(sceneCode);
        if (existingSceneOpt.isEmpty()) {
            return ApiResponse.error("20001", "未找到要更新的场景: " + sceneCode);
        }

        QueryScene existingScene = existingSceneOpt.get();
        // Update fields from request, except sceneCode
        if (request.getSceneName() != null) existingScene.setSceneName(request.getSceneName());
        if (request.getDescription() != null) existingScene.setDescription(request.getDescription());

        try {
            if (request.getTemplateIds() != null) { // Allow clearing templateIds with empty list
                existingScene.setTemplateIds(objectMapper.writeValueAsString(request.getTemplateIds()));
            }
        } catch (JsonProcessingException e) {
            log.error("更新时序列化场景模板ID列表失败 for sceneCode {}: {}", sceneCode, e.getMessage());
            return ApiResponse.error("40001", "更新时序列化模板ID列表失败: " + e.getMessage());
        }

        QueryScene updatedScene = querySceneRepository.save(existingScene);
        log.info("场景已更新: {}", updatedScene.getSceneCode());

        return ApiResponse.success(convertToDto(updatedScene));
    }
}
