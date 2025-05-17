package com.oceanbase.query.api.service;

import com.oceanbase.query.api.dto.QueryExecuteRequest;
import com.oceanbase.query.api.dto.ApiResponse;
import com.oceanbase.query.api.dto.QueryResultDto;

import java.util.List;

public interface QueryService {

    ApiResponse<List<QueryResultDto>> executeQuery(String sceneCode, QueryExecuteRequest request);

    ApiResponse<List<QueryResultDto>> getAsyncTaskResult(String taskId);

}

