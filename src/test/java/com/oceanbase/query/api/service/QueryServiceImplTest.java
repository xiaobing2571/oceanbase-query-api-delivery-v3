package com.oceanbase.query.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.query.api.domain.entity.*;
import com.oceanbase.query.api.domain.repository.*;
import com.oceanbase.query.api.dto.*;
import com.oceanbase.query.api.service.impl.QueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QueryServiceImplTest {

    @Mock
    private QuerySceneRepository querySceneRepository;
    @Mock
    private SqlTemplateRepository sqlTemplateRepository;
    @Mock
    private SqlTemplateVersionRepository sqlTemplateVersionRepository;
    @Mock
    private DatasourceConfigRepository datasourceConfigRepository;
    @Mock
    private AsyncTaskRepository asyncTaskRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper(); // Real ObjectMapper for JSON processing

    @InjectMocks
    private QueryServiceImpl queryService;

    private QueryScene testScene;
    private SqlTemplate testTemplate1, testTemplate2;
    private SqlTemplateVersion testVersion1, testVersion2, testVersion3;
    private QueryExecuteRequest syncRequest, asyncRequest;
    private DatasourceConfig registeredDs;

    @BeforeEach
    void setUp() throws Exception {
        testScene = new QueryScene();
        testScene.setSceneCode("test_scene");
        testScene.setTemplateIds(objectMapper.writeValueAsString(List.of("tpl1", "tpl2")));

        testTemplate1 = new SqlTemplate(1L, "tpl1", "test_scene", "desc1", "OCEANBASE", "[]", LocalDateTime.now(), LocalDateTime.now());
        testTemplate2 = new SqlTemplate(2L, "tpl2", "test_scene", "desc2", "OCEANBASE", "[]", LocalDateTime.now(), LocalDateTime.now());

        testVersion1 = new SqlTemplateVersion(1L, "tpl1", ">=3.0.0", "SELECT * FROM table1 WHERE id = :id");
        testVersion2 = new SqlTemplateVersion(2L, "tpl1", "<3.0.0", "SELECT name FROM table1_old WHERE id = :id");
        testVersion3 = new SqlTemplateVersion(3L, "tpl2", ">=0.0.0", "SELECT count(*) FROM table2");

        syncRequest = new QueryExecuteRequest();
        syncRequest.setDatasourceType("direct");
        syncRequest.setDatasourceConfig(new DatasourceConfigDto("jdbc:oceanbase://localhost:2881/testdb", "user", "pass"));
        syncRequest.setAsync(false);
        syncRequest.setExtraParams(Map.of("id", 123, "targetObVersion", "4.0.0"));

        asyncRequest = new QueryExecuteRequest();
        asyncRequest.setDatasourceType("direct");
        asyncRequest.setDatasourceConfig(new DatasourceConfigDto("jdbc:oceanbase://localhost:2881/testdb", "user", "pass"));
        asyncRequest.setAsync(true);
        asyncRequest.setCallbackUrl("http://localhost/callback");
        asyncRequest.setExtraParams(Map.of("targetObVersion", "2.5.0"));

        registeredDs = new DatasourceConfig(1L, "regDs", "OCEANBASE", "jdbc:oceanbase://reg-host:2881/reg_db", "reg_user", "reg_pass", null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void executeQuery_Sync_SceneNotFound() {
        when(querySceneRepository.findBySceneCode("unknown_scene")).thenReturn(Optional.empty());
        ApiResponse<List<QueryResultDto>> response = queryService.executeQuery("unknown_scene", syncRequest);
        assertEquals("failed", response.getStatus());
        assertEquals("20002", response.getErrorCode());
    }

    @Test
    void executeQuery_Sync_TemplateIdsParsingError() throws Exception {
        testScene.setTemplateIds("invalid_json_array");
        when(querySceneRepository.findBySceneCode("test_scene")).thenReturn(Optional.of(testScene));
        ApiResponse<List<QueryResultDto>> response = queryService.executeQuery("test_scene", syncRequest);
        assertEquals("failed", response.getStatus());
        assertEquals("20003", response.getErrorCode());
    }

    @Test
    void executeQuery_Sync_NoTemplatesInScene() throws Exception {
        testScene.setTemplateIds(objectMapper.writeValueAsString(Collections.emptyList()));
        when(querySceneRepository.findBySceneCode("test_scene")).thenReturn(Optional.of(testScene));
        ApiResponse<List<QueryResultDto>> response = queryService.executeQuery("test_scene", syncRequest);
        assertEquals("failed", response.getStatus());
        assertEquals("20004", response.getErrorCode());
    }

    @Test
    void executeQuery_Sync_DatasourceError() {
        syncRequest.setDatasourceType("direct");
        syncRequest.setDatasourceConfig(null); // Invalid config
        when(querySceneRepository.findBySceneCode("test_scene")).thenReturn(Optional.of(testScene));
        ApiResponse<List<QueryResultDto>> response = queryService.executeQuery("test_scene", syncRequest);
        assertEquals("failed", response.getStatus());
        assertEquals("10001", response.getErrorCode()); // Error from getTargetDataSource
    }

    // More complex test: Successful sync execution. Requires mocking DataSource and JDBC operations.
    // This is where unit testing QueryServiceImpl becomes challenging due to its direct JDBC interactions.
    // A full test would involve mocking DriverManagerDataSource, Connection, PreparedStatement, ResultSet.
    // For brevity and focus on service logic, we'll mock at a higher level where possible or simplify.

    @Test
    void executeQuery_Async_SuccessPath() {
        when(querySceneRepository.findBySceneCode("test_scene")).thenReturn(Optional.of(testScene));
        when(asyncTaskRepository.save(any(AsyncTask.class))).thenAnswer(inv -> inv.getArgument(0));

        // To avoid running the actual async method in this unit test, we can spy on the service
        // QueryServiceImpl spiedQueryService = Mockito.spy(queryService);
        // doNothing().when(spiedQueryService).executeSceneQueryAsync(anyString(), any(), any());
        // ApiResponse<List<QueryResultDto>> response = spiedQueryService.executeQuery("test_scene", asyncRequest);
        // For now, we'll let it call the async method but won't test its internal execution deeply here.

        ApiResponse<List<QueryResultDto>> response = queryService.executeQuery("test_scene", asyncRequest);

        assertEquals("success", response.getStatus());
        assertNotNull(response.getTaskId());
        assertEquals("异步查询任务已提交", response.getMessage());
        verify(asyncTaskRepository, times(1)).save(any(AsyncTask.class));
        // verify(spiedQueryService, times(1)).executeSceneQueryAsync(eq(response.getTaskId()), eq(testScene), eq(asyncRequest));
    }

    @Test
    void getAsyncTaskResult_NotFound() {
        when(asyncTaskRepository.findByTaskId("unknown_task")).thenReturn(Optional.empty());
        ApiResponse<List<QueryResultDto>> response = queryService.getAsyncTaskResult("unknown_task");
        assertEquals("failed", response.getStatus());
        assertEquals("40004", response.getErrorCode());
    }

    @Test
    void getAsyncTaskResult_Pending() {
        AsyncTask task = new AsyncTask();
        task.setTaskId("pending_task");
        task.setStatus("PENDING");
        when(asyncTaskRepository.findByTaskId("pending_task")).thenReturn(Optional.of(task));
        ApiResponse<List<QueryResultDto>> response = queryService.getAsyncTaskResult("pending_task");
        assertEquals("pending", response.getStatus());
        assertEquals("任务正在处理中", response.getMessage());
    }

    @Test
    void getAsyncTaskResult_Success() throws Exception {
        AsyncTask task = new AsyncTask();
        task.setTaskId("success_task");
        task.setStatus("SUCCESS");
        List<QueryResultDto> mockResults = List.of(QueryResultDto.builder().templateId("tpl1").status("success").build());
        task.setResults(objectMapper.writeValueAsString(mockResults));
        when(asyncTaskRepository.findByTaskId("success_task")).thenReturn(Optional.of(task));

        ApiResponse<List<QueryResultDto>> response = queryService.getAsyncTaskResult("success_task");
        assertEquals("success", response.getStatus());
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertEquals("tpl1", response.getData().get(0).getTemplateId());
    }

    @Test
    void getAsyncTaskResult_Success_ResultParsingError() throws Exception {
        AsyncTask task = new AsyncTask();
        task.setTaskId("parse_error_task");
        task.setStatus("SUCCESS");
        task.setResults("invalid_json_for_results");
        when(asyncTaskRepository.findByTaskId("parse_error_task")).thenReturn(Optional.of(task));

        ApiResponse<List<QueryResultDto>> response = queryService.getAsyncTaskResult("parse_error_task");
        assertEquals("failed", response.getStatus());
        assertEquals("40001", response.getErrorCode());
        assertTrue(response.getMessage().contains("解析任务结果失败"));
    }

    @Test
    void getAsyncTaskResult_Failed() {
        AsyncTask task = new AsyncTask();
        task.setTaskId("failed_task");
        task.setStatus("FAILED");
        task.setErrorMessage("DB connection error");
        when(asyncTaskRepository.findByTaskId("failed_task")).thenReturn(Optional.of(task));

        ApiResponse<List<QueryResultDto>> response = queryService.getAsyncTaskResult("failed_task");
        assertEquals("failed", response.getStatus());
        assertEquals("40001", response.getErrorCode());
        assertEquals("DB connection error", response.getMessage());
    }

    // Note: Testing the private methods like executeSingleTemplate, findMatchingSqlVersion, getTargetDataSource
    // directly is generally not a good unit testing practice. They are tested indirectly via the public methods.
    // However, if their logic is complex, they might be refactored into separate, testable components.
    // For `getTargetDataSource`, we tested some error paths in DatasourceServiceImplTest indirectly.
    // For `findMatchingSqlVersion`, its logic is critical and could be tested separately if made public/static or moved to a utility.

    // A more complete test for executeSceneQuerySync would require extensive mocking of JDBC interactions.
    // Example (conceptual, would need a mockable DataSource and NamedParameterJdbcTemplate):
    /*
    @Test
    void executeQuery_Sync_SuccessfulExecution() throws Exception {
        when(querySceneRepository.findBySceneCode("test_scene")).thenReturn(Optional.of(testScene));
        when(sqlTemplateRepository.findByTemplateId("tpl1")).thenReturn(Optional.of(testTemplate1));
        when(sqlTemplateRepository.findByTemplateId("tpl2")).thenReturn(Optional.of(testTemplate2));
        when(sqlTemplateVersionRepository.findByTemplateId("tpl1")).thenReturn(List.of(testVersion1, testVersion2));
        when(sqlTemplateVersionRepository.findByTemplateId("tpl2")).thenReturn(List.of(testVersion3));

        // Mock DataSource creation and JDBC execution
        DataSource mockDataSource = mock(DriverManagerDataSource.class);
        Connection mockConnection = mock(Connection.class);
        NamedParameterJdbcTemplate mockJdbc = mock(NamedParameterJdbcTemplate.class);

        // This is where it gets complex: queryService creates its own DataSource and JdbcTemplate.
        // To test this properly, getTargetDataSource would need to return a mock DataSource, or
        // the JdbcTemplate creation needs to be injectable/mockable.

        // Assuming we could inject/mock NamedParameterJdbcTemplate for targetDataSource:
        // when(mockJdbc.queryForList(eq("SELECT * FROM table1 WHERE id = :id"), any(MapSqlParameterSource.class)))
        // .thenReturn(List.of(Map.of("column", "value1")));
        // when(mockJdbc.queryForList(eq("SELECT count(*) FROM table2"), any(MapSqlParameterSource.class)))
        // .thenReturn(List.of(Map.of("count", 5)));

        // This test is more of an integration test style if it were to hit a real DB or fully mocked JDBC.
        // For a unit test, the scope is usually smaller.

        // Due to the complexity of mocking the JDBC part within QueryServiceImpl's current structure,
        // this specific success path test is omitted for brevity in pure unit test form.
        // It would be better covered by an integration test.
    }
    */
}

