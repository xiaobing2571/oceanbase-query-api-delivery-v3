package com.oceanbase.query.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.query.api.domain.entity.QueryScene; // Added for listTemplatesByScene
import com.oceanbase.query.api.domain.entity.SqlTemplate;
import com.oceanbase.query.api.domain.entity.SqlTemplateVersion;
import com.oceanbase.query.api.domain.repository.QuerySceneRepository; // Added for listTemplatesByScene
import com.oceanbase.query.api.domain.repository.SqlTemplateRepository;
import com.oceanbase.query.api.domain.repository.SqlTemplateVersionRepository;
import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.SqlTemplateDto;
import com.oceanbase.query.api.dto.TemplateParameterDto;
import com.oceanbase.query.api.dto.TemplateRegisterRequest;
import com.oceanbase.query.api.dto.VersionMappingDto;
import com.oceanbase.query.api.service.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TemplateServiceImpl implements TemplateService {

    @Autowired
    private SqlTemplateRepository sqlTemplateRepository;

    @Autowired
    private SqlTemplateVersionRepository sqlTemplateVersionRepository;

    @Autowired
    private QuerySceneRepository querySceneRepository; // Added for listTemplatesByScene

    @Autowired
    private ObjectMapper objectMapper;

    private SqlTemplateDto convertToDto(SqlTemplate template) {
        SqlTemplateDto dto = new SqlTemplateDto();
        BeanUtils.copyProperties(template, dto);
        try {
            if (template.getParameters() != null && !template.getParameters().isEmpty()) {
                dto.setParameters(objectMapper.readValue(template.getParameters(), new TypeReference<List<TemplateParameterDto>>() {}));
            }
            List<SqlTemplateVersion> versions = sqlTemplateVersionRepository.findByTemplateId(template.getTemplateId());
            dto.setVersionMapping(versions.stream().map(v -> new VersionMappingDto(v.getVersionCondition(), v.getSqlText())).collect(Collectors.toList()));
        } catch (JsonProcessingException e) {
            log.warn("反序列化模板参数或版本失败 for templateId {}: {}", template.getTemplateId(), e.getMessage());
        }
        return dto;
    }

    @Override
    @Transactional
    public ApiResponse<SqlTemplateDto> registerTemplate(TemplateRegisterRequest request) {
        if (request.getTemplateId() == null || request.getTemplateId().isEmpty()) {
            return ApiResponse.error("30000", "模板ID不能为空");
        }
        if (sqlTemplateRepository.existsByTemplateId(request.getTemplateId())) {
            return ApiResponse.error("30001", "模板ID已存在: " + request.getTemplateId());
        }
        if (request.getSceneCode() == null || request.getSceneCode().isEmpty()) {
            return ApiResponse.error("30002", "场景代码不能为空");
        }
        if (CollectionUtils.isEmpty(request.getVersionMapping())) {
            return ApiResponse.error("30003", "版本映射不能为空");
        }

        SqlTemplate sqlTemplate = new SqlTemplate();
        sqlTemplate.setTemplateId(request.getTemplateId());
        sqlTemplate.setSceneCode(request.getSceneCode());
        sqlTemplate.setDescription(request.getDescription());
        sqlTemplate.setDbType(request.getDbType());

        try {
            if (request.getParameters() != null && !request.getParameters().isEmpty()) {
                sqlTemplate.setParameters(objectMapper.writeValueAsString(request.getParameters()));
            }
        } catch (JsonProcessingException e) {
            log.error("序列化模板参数失败 for templateId {}: {}", request.getTemplateId(), e.getMessage());
            return ApiResponse.error("40001", "序列化模板参数失败: " + e.getMessage());
        }

        SqlTemplate savedTemplate = sqlTemplateRepository.save(sqlTemplate);
        log.info("SQL模板已注册: {}", savedTemplate.getTemplateId());

        List<SqlTemplateVersion> versions = new ArrayList<>();
        for (VersionMappingDto mappingDto : request.getVersionMapping()) {
            SqlTemplateVersion version = new SqlTemplateVersion();
            version.setTemplateId(savedTemplate.getTemplateId());
            version.setVersionCondition(mappingDto.getVersion());
            version.setSqlText(mappingDto.getSql());
            versions.add(version);
        }
        sqlTemplateVersionRepository.saveAll(versions);
        log.info("SQL模板版本已注册 for templateId: {}", savedTemplate.getTemplateId());

        return ApiResponse.success(convertToDto(savedTemplate));
    }

    @Override
    public ApiResponse<SqlTemplateDto> getTemplate(String templateId) {
        return sqlTemplateRepository.findByTemplateId(templateId)
                .map(template -> ApiResponse.success(convertToDto(template)))
                .orElseGet(() -> ApiResponse.error("30004", "未找到模板: " + templateId));
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteTemplate(String templateId) {
        if (!sqlTemplateRepository.existsByTemplateId(templateId)) {
            return ApiResponse.error("30004", "未找到要删除的模板: " + templateId);
        }
        try {
            sqlTemplateVersionRepository.deleteByTemplateId(templateId);
            sqlTemplateRepository.deleteByTemplateId(templateId);
            log.info("模板及其版本已删除: {}", templateId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除模板失败: {}", templateId, e);
            return ApiResponse.error("30005", "删除模板时发生内部错误: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<List<SqlTemplateDto>> listTemplates() {
        try {
            List<SqlTemplateDto> dtos = sqlTemplateRepository.findAll().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            return ApiResponse.<List<SqlTemplateDto>>builder()
                    .status("success")
                    .data(dtos)
                    .build();
        } catch (Exception e) {
            log.error("列出所有模板失败", e);
            return ApiResponse.error("30006", "列出所有模板时发生内部错误: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<List<SqlTemplateDto>> listTemplatesByScene(String sceneCode) {
        Optional<QueryScene> sceneOpt = querySceneRepository.findBySceneCode(sceneCode);
        if (sceneOpt.isEmpty()) {
            return ApiResponse.error("20001", "场景代码未找到: " + sceneCode);
        }
        try {
            List<SqlTemplateDto> dtos = sqlTemplateRepository.findBySceneCode(sceneCode).stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            return ApiResponse.<List<SqlTemplateDto>>builder()
                    .status("success")
                    .data(dtos)
                    .build();
        } catch (Exception e) {
            log.error("根据场景代码 {} 列出模板失败", sceneCode, e);
            return ApiResponse.error("30007", "根据场景代码列出模板时发生内部错误: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<SqlTemplateDto> updateTemplate(String templateId, TemplateRegisterRequest request) {
        Optional<SqlTemplate> existingTemplateOpt = sqlTemplateRepository.findByTemplateId(templateId);
        if (existingTemplateOpt.isEmpty()) {
            return ApiResponse.error("30004", "未找到要更新的模板: " + templateId);
        }

        SqlTemplate existingTemplate = existingTemplateOpt.get();
        // Update fields from request, except templateId
        if (request.getSceneCode() != null) existingTemplate.setSceneCode(request.getSceneCode());
        if (request.getDescription() != null) existingTemplate.setDescription(request.getDescription());
        if (request.getDbType() != null) existingTemplate.setDbType(request.getDbType());

        try {
            if (request.getParameters() != null) { // Allow clearing parameters with empty list
                existingTemplate.setParameters(objectMapper.writeValueAsString(request.getParameters()));
            }
        } catch (JsonProcessingException e) {
            log.error("更新时序列化模板参数失败 for templateId {}: {}", templateId, e.getMessage());
            return ApiResponse.error("40001", "更新时序列化模板参数失败: " + e.getMessage());
        }

        SqlTemplate updatedTemplate = sqlTemplateRepository.save(existingTemplate);
        log.info("SQL模板已更新: {}", updatedTemplate.getTemplateId());

        // Handle version updates: delete existing and add new ones
        if (!CollectionUtils.isEmpty(request.getVersionMapping())) {
            sqlTemplateVersionRepository.deleteByTemplateId(templateId);
            List<SqlTemplateVersion> newVersions = new ArrayList<>();
            for (VersionMappingDto mappingDto : request.getVersionMapping()) {
                SqlTemplateVersion version = new SqlTemplateVersion();
                version.setTemplateId(updatedTemplate.getTemplateId());
                version.setVersionCondition(mappingDto.getVersion());
                version.setSqlText(mappingDto.getSql());
                newVersions.add(version);
            }
            sqlTemplateVersionRepository.saveAll(newVersions);
            log.info("SQL模板版本已更新 for templateId: {}", updatedTemplate.getTemplateId());
        }
        return ApiResponse.success(convertToDto(updatedTemplate));
    }
}
