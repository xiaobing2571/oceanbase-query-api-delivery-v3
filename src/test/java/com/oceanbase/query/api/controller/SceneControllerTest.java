package com.oceanbase.query.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.SceneDto;
import com.oceanbase.query.api.dto.SceneRegisterRequest;
import com.oceanbase.query.api.service.SceneService;
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

@WebMvcTest(SceneController.class)
public class SceneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SceneService sceneService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerScene_Success() throws Exception {
        SceneRegisterRequest request = new SceneRegisterRequest();
        request.setSceneCode("scn-001");
        request.setSceneName("Test Scene");
        request.setTemplateIds(Collections.singletonList("tpl-001"));

        SceneDto sceneDto = new SceneDto();
        sceneDto.setSceneCode("scn-001");
        sceneDto.setSceneName("Test Scene");
        sceneDto.setTemplateIds(Collections.singletonList("tpl-001"));
        
        ApiResponse<SceneDto> serviceResponse = ApiResponse.success(sceneDto);
        when(sceneService.registerScene(any(SceneRegisterRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/api/query/v1/scenes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.sceneCode").value("scn-001"));
    }

    @Test
    void registerScene_Failure_AlreadyExists() throws Exception {
        SceneRegisterRequest request = new SceneRegisterRequest();
        request.setSceneCode("scn-001");
        // ... other fields

        ApiResponse<SceneDto> serviceResponse = ApiResponse.error("30002", "场景代码已存在: scn-001");
        when(sceneService.registerScene(any(SceneRegisterRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/api/query/v1/scenes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.errorCode").value("30002"))
                .andExpect(jsonPath("$.message").value("场景代码已存在: scn-001"));
    }

    @Test
    void registerScene_InvalidRequest_MissingFields() throws Exception {
        SceneRegisterRequest request = new SceneRegisterRequest(); // Missing sceneCode, etc.

        ApiResponse<SceneDto> serviceResponse = ApiResponse.error("VALIDATION_ERROR", "场景代码不能为空");
        when(sceneService.registerScene(any(SceneRegisterRequest.class))).thenReturn(serviceResponse);

        mockMvc.perform(post("/api/query/v1/scenes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // Assuming service handles this or @Valid catches it
                .andExpect(jsonPath("$.status").value("failed"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
