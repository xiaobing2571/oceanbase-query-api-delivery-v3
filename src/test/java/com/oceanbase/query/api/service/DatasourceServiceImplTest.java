package com.oceanbase.query.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.query.api.domain.entity.DatasourceConfig;
import com.oceanbase.query.api.domain.repository.DatasourceConfigRepository;
import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.DatasourceConfigDto;
import com.oceanbase.query.api.dto.DatasourceTestRequest;
import com.oceanbase.query.api.service.impl.DatasourceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DatasourceServiceImplTest {

    @Mock
    private DatasourceConfigRepository datasourceConfigRepository;

    @Spy // Use Spy for ObjectMapper to allow real method calls while still being mockable if needed
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DatasourceServiceImpl datasourceService;

    @BeforeEach
    void setUp() {
        // objectMapper.registerModule(new JavaTimeModule()); // If using JavaTimeModule in main code
    }

    // 注释掉依赖真实数据源的测试
    // @Test
    void testDatasourceConnection_Direct_Success() throws Exception {
        DatasourceTestRequest request = new DatasourceTestRequest();
        request.setDatasourceType("direct");
        DatasourceConfigDto configDto = new DatasourceConfigDto("jdbc:oceanbase://localhost:2881/test", "user", "pass");
        request.setDatasourceConfig(configDto);

        ApiResponse<Object> response = datasourceService.testDatasourceConnection(request);
        assertNotNull(response);
        if (response.getStatus().equals("failed")) {
            assertTrue(response.getMessage().contains("数据源连接失败") || response.getMessage().contains("JDBC 驱动未找到"));
        }
    }

    @Test
    void testDatasourceConnection_Direct_MissingConfig() {
        DatasourceTestRequest request = new DatasourceTestRequest();
        request.setDatasourceType("direct");
        // request.setDatasourceConfig(null); // Implicitly null

        ApiResponse<Object> response = datasourceService.testDatasourceConnection(request);

        assertEquals("failed", response.getStatus());
        assertEquals("10003", response.getErrorCode());
        assertTrue(response.getMessage().contains("直接连接时，datasourceConfig 不能为空"));
    }

    // 注释掉依赖真实数据源的测试
    // @Test
    void testDatasourceConnection_Registered_Success() throws Exception {
        DatasourceTestRequest request = new DatasourceTestRequest();
        request.setDatasourceType("registered");
        request.setDatasourceId("test-ds");

        DatasourceConfig registeredDs = new DatasourceConfig();
        registeredDs.setDatasourceId("test-ds");
        registeredDs.setUrl("jdbc:oceanbase://reg-host:2881/reg_test");
        registeredDs.setUsername("reg_user");
        registeredDs.setPassword("reg_pass");
        registeredDs.setExtraConfig("{\"SSL\":true}");

        when(datasourceConfigRepository.findByDatasourceId("test-ds")).thenReturn(Optional.of(registeredDs));

        ApiResponse<Object> response = datasourceService.testDatasourceConnection(request);
        assertNotNull(response);
        if (response.getStatus().equals("failed")) {
            assertTrue(response.getMessage().contains("数据源连接失败") || response.getMessage().contains("JDBC 驱动未找到"));
        }
    }

    @Test
    void testDatasourceConnection_Registered_NotFound() {
        DatasourceTestRequest request = new DatasourceTestRequest();
        request.setDatasourceType("registered");
        request.setDatasourceId("non-existent-ds");

        when(datasourceConfigRepository.findByDatasourceId("non-existent-ds")).thenReturn(Optional.empty());

        ApiResponse<Object> response = datasourceService.testDatasourceConnection(request);

        assertEquals("failed", response.getStatus());
        assertEquals("10005", response.getErrorCode());
        assertTrue(response.getMessage().contains("未找到注册的数据源"));
    }

    @Test
    void testDatasourceConnection_Registered_MissingId() {
        DatasourceTestRequest request = new DatasourceTestRequest();
        request.setDatasourceType("registered");
        // request.setDatasourceId(null); // Implicitly null

        ApiResponse<Object> response = datasourceService.testDatasourceConnection(request);

        assertEquals("failed", response.getStatus());
        assertEquals("10004", response.getErrorCode());
        assertTrue(response.getMessage().contains("注册数据源连接时，datasourceId 不能为空"));
    }

    @Test
    void testDatasourceConnection_External_NotImplemented() {
        DatasourceTestRequest request = new DatasourceTestRequest();
        request.setDatasourceType("external");
        request.setExternalSourceId("ext-id");

        ApiResponse<Object> response = datasourceService.testDatasourceConnection(request);

        assertEquals("failed", response.getStatus());
        assertEquals("10006", response.getErrorCode());
        assertTrue(response.getMessage().contains("外部数据源类型暂未实现"));
    }

    @Test
    void testDatasourceConnection_UnsupportedType() {
        DatasourceTestRequest request = new DatasourceTestRequest();
        request.setDatasourceType("unknown");

        ApiResponse<Object> response = datasourceService.testDatasourceConnection(request);

        assertEquals("failed", response.getStatus());
        assertEquals("10007", response.getErrorCode());
        assertTrue(response.getMessage().contains("不支持的数据源类型"));
    }
    
    @Test
    void testDatasourceConnection_Direct_WithExtraConfig_JsonProcessingError() throws Exception {
        DatasourceTestRequest request = new DatasourceTestRequest();
        request.setDatasourceType("direct");
        DatasourceConfigDto configDto = new DatasourceConfigDto("jdbc:oceanbase://localhost:2881/test", "user", "pass");
        request.setDatasourceConfig(configDto);
        request.setExtraConfig(Map.of("key", new Object())); // Invalid for JSON serialization by default

        // Configure the spied objectMapper (which is already injected into datasourceService)
        // to throw JsonProcessingException for extraConfig
        doThrow(new com.fasterxml.jackson.core.JsonProcessingException("Test error"){}).when(objectMapper).writeValueAsString(request.getExtraConfig());

        ApiResponse<Object> response = datasourceService.testDatasourceConnection(request);
        
        // Reset the spy if necessary for other tests, or rely on Mockito's per-test isolation.
        reset(objectMapper); // Explicitly reset the spy after use in this test

        assertEquals("failed", response.getStatus());
        assertEquals("40001", response.getErrorCode()); // General processing error
        assertTrue(response.getMessage().contains("请求处理错误"));
    }
}
