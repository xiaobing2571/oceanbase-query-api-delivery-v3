package com.oceanbase.query.api.service;

import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.SqlTemplateDto;
import com.oceanbase.query.api.dto.TemplateRegisterRequest;

import java.util.List;

public interface TemplateService {

    ApiResponse<SqlTemplateDto> registerTemplate(TemplateRegisterRequest request);

    ApiResponse<SqlTemplateDto> getTemplate(String templateId);

    ApiResponse<Void> deleteTemplate(String templateId);

    ApiResponse<List<SqlTemplateDto>> listTemplates(); // General listing

    ApiResponse<List<SqlTemplateDto>> listTemplatesByScene(String sceneCode);

    ApiResponse<SqlTemplateDto> updateTemplate(String templateId, TemplateRegisterRequest request);
}

