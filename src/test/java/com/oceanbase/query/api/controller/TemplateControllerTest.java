package com.oceanbase.query.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.SqlTemplateDto;
import com.oceanbase.query.api.dto.TemplateRegisterRequest;
import com.oceanbase.query.api.dto.VersionMappingDto;
import com.oceanbase.query.api.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TemplateController.class)
public class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TemplateService templateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerTemplate_Success() throws Exception {
        TemplateRegisterRequest request = new TemplateRegisterRequest();
        request.setTemplateId("tpl-001");
        request.setSceneCode("scn-001");
        request.setDbType("OCEANBASE");
        request.setVersionMapping(Collections.singletonList(new VersionMappingDto(">=4.0", "SELECT 1")));

        SqlTemplateDto templateDto = new SqlTemplateDto();
        templateDto.setTemplateId("tpl-001");
        templateDto.setSceneCode("scn-001");
        templateDto.setDbType("OCEANBASE");
        
        ApiResponse<SqlTemplateDto> serviceResponse = ApiResponse.success(templateDto);
        when(templateService.registerTemplate(any(TemplateRegisterRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/api/query/v1/templates/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.templateId").value("tpl-001"));
    }

    @Test
    void registerTemplate_Failure_AlreadyExists() throws Exception {
        TemplateRegisterRequest request = new TemplateRegisterRequest();
        request.setTemplateId("tpl-001");
        // ... other fields

        ApiResponse<SqlTemplateDto> serviceResponse = ApiResponse.error("30001", "模板ID已存在: tpl-001");
        when(templateService.registerTemplate(any(TemplateRegisterRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/api/query/v1/templates/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.errorCode").value("30001"))
                .andExpect(jsonPath("$.message").value("模板ID已存在: tpl-001"));
    }

    @Test
    void registerTemplate_InvalidRequest_MissingFields() throws Exception {
        TemplateRegisterRequest request = new TemplateRegisterRequest(); // Missing required fields

        // Assuming @Valid on RequestBody would trigger Spring's validation
        // and return a 400 Bad Request before hitting the service mock in some cases.
        // However, if validation is basic (e.g. only @NotNull on templateId) and service handles other logic errors:
        ApiResponse<SqlTemplateDto> serviceResponse = ApiResponse.error("VALIDATION_ERROR", "模板ID不能为空"); // Example service-level validation
        when(templateService.registerTemplate(any(TemplateRegisterRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/api/query/v1/templates/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))) // Sending an incomplete request
                .andExpect(status().isBadRequest()) // Or whatever status Spring validation returns for @Valid failures
                .andExpect(jsonPath("$.status").value("failed"));
                // The exact error message/code might depend on how @Valid is configured and handled.
    }
}
