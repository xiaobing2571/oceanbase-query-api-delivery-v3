package com.oceanbase.query.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.query.api.domain.entity.SqlTemplate;
import com.oceanbase.query.api.domain.entity.SqlTemplateVersion;
import com.oceanbase.query.api.domain.repository.SqlTemplateRepository;
import com.oceanbase.query.api.domain.repository.SqlTemplateVersionRepository;
import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.SqlTemplateDto;
import com.oceanbase.query.api.dto.TemplateParameterDto;
import com.oceanbase.query.api.dto.TemplateRegisterRequest;
import com.oceanbase.query.api.dto.VersionMappingDto;
import com.oceanbase.query.api.service.impl.TemplateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TemplateServiceImplTest {

    @Mock
    private SqlTemplateRepository sqlTemplateRepository;

    @Mock
    private SqlTemplateVersionRepository sqlTemplateVersionRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TemplateServiceImpl templateService;

    private TemplateRegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new TemplateRegisterRequest();
        validRequest.setTemplateId("test-template");
        validRequest.setSceneCode("test-scene");
        validRequest.setDbType("OCEANBASE");
        validRequest.setDescription("A test template");

        TemplateParameterDto paramDto = new TemplateParameterDto("param1", "string", true, null);
        validRequest.setParameters(Collections.singletonList(paramDto));

        VersionMappingDto versionMappingDto = new VersionMappingDto(">=4.0.0", "SELECT * FROM test_table");
        validRequest.setVersionMapping(Collections.singletonList(versionMappingDto));
    }

    @Test
    void registerTemplate_Success() throws JsonProcessingException {
        when(sqlTemplateRepository.existsByTemplateId("test-template")).thenReturn(false);
        
        SqlTemplate savedSqlTemplate = new SqlTemplate();
        savedSqlTemplate.setId(1L);
        savedSqlTemplate.setTemplateId("test-template");
        savedSqlTemplate.setSceneCode("test-scene");
        savedSqlTemplate.setDbType("OCEANBASE");
        savedSqlTemplate.setParameters(objectMapper.writeValueAsString(validRequest.getParameters()));

        when(sqlTemplateRepository.save(any(SqlTemplate.class))).thenReturn(savedSqlTemplate);
        
        // Mock version retrieval for convertToDto
        when(sqlTemplateVersionRepository.findByTemplateId(anyString())).thenReturn(
            Collections.singletonList(new SqlTemplateVersion())
        );

        ApiResponse<SqlTemplateDto> response = templateService.registerTemplate(validRequest);

        assertEquals("success", response.getStatus());
        assertNotNull(response.getData());
        assertEquals("test-template", response.getData().getTemplateId());

        verify(sqlTemplateRepository, times(1)).save(any(SqlTemplate.class));
        verify(sqlTemplateVersionRepository, times(1)).saveAll(anyList());

        ArgumentCaptor<SqlTemplate> templateCaptor = ArgumentCaptor.forClass(SqlTemplate.class);
        verify(sqlTemplateRepository).save(templateCaptor.capture());
        assertEquals("test-template", templateCaptor.getValue().getTemplateId());
        assertEquals(objectMapper.writeValueAsString(validRequest.getParameters()), templateCaptor.getValue().getParameters());

        ArgumentCaptor<List<SqlTemplateVersion>> versionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sqlTemplateVersionRepository).saveAll(versionsCaptor.capture());
        assertEquals(1, versionsCaptor.getValue().size());
        assertEquals("test-template", versionsCaptor.getValue().get(0).getTemplateId());
        assertEquals(">=4.0.0", versionsCaptor.getValue().get(0).getVersionCondition());
    }

    @Test
    void registerTemplate_AlreadyExists() {
        when(sqlTemplateRepository.existsByTemplateId("test-template")).thenReturn(true);

        ApiResponse<SqlTemplateDto> response = templateService.registerTemplate(validRequest);

        assertEquals("failed", response.getStatus());
        assertEquals("30001", response.getErrorCode());
        assertTrue(response.getMessage().contains("模板ID已存在"));

        verify(sqlTemplateRepository, never()).save(any());
        verify(sqlTemplateVersionRepository, never()).saveAll(anyList());
    }

    @Test
    void registerTemplate_ParameterSerializationError() throws JsonProcessingException {
        when(sqlTemplateRepository.existsByTemplateId("test-template")).thenReturn(false);

        // Configure the spied objectMapper (which is already injected into templateService)
        // to throw an exception during parameter serialization
        doThrow(new JsonProcessingException("Serialization error") {}).when(objectMapper).writeValueAsString(any());

        ApiResponse<SqlTemplateDto> response = templateService.registerTemplate(validRequest);

        assertEquals("failed", response.getStatus());
        assertEquals("40001", response.getErrorCode());
        assertTrue(response.getMessage().contains("序列化模板参数失败"));

        verify(sqlTemplateRepository, never()).save(any());
        verify(sqlTemplateVersionRepository, never()).saveAll(anyList());
        
        // Reset the spy to avoid side effects on other tests
        reset(objectMapper);
    }
    
    @Test
    void registerTemplate_NoParameters() throws JsonProcessingException {
        validRequest.setParameters(null);
        when(sqlTemplateRepository.existsByTemplateId("test-template")).thenReturn(false);
        
        SqlTemplate savedSqlTemplate = new SqlTemplate();
        savedSqlTemplate.setId(1L);
        savedSqlTemplate.setTemplateId("test-template");
        savedSqlTemplate.setSceneCode("test-scene");
        savedSqlTemplate.setDbType("OCEANBASE");
        
        when(sqlTemplateRepository.save(any(SqlTemplate.class))).thenReturn(savedSqlTemplate);
        
        // Mock version retrieval for convertToDto
        when(sqlTemplateVersionRepository.findByTemplateId(anyString())).thenReturn(
            Collections.singletonList(new SqlTemplateVersion())
        );

        ApiResponse<SqlTemplateDto> response = templateService.registerTemplate(validRequest);

        assertEquals("success", response.getStatus());
        assertNotNull(response.getData());
        assertEquals("test-template", response.getData().getTemplateId());
        
        verify(sqlTemplateRepository, times(1)).save(any(SqlTemplate.class));
        ArgumentCaptor<SqlTemplate> templateCaptor = ArgumentCaptor.forClass(SqlTemplate.class);
        verify(sqlTemplateRepository).save(templateCaptor.capture());
        assertNull(templateCaptor.getValue().getParameters()); // Ensure parameters field is null or empty as expected
    }
}
