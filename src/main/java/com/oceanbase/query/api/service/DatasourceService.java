package com.oceanbase.query.api.service;

import com.oceanbase.query.api.dto.DatasourceConfigDto;
import com.oceanbase.query.api.dto.DatasourceTestRequest;
import com.oceanbase.query.api.dto.ApiResponse;

import java.util.List;

public interface DatasourceService {

    ApiResponse<Object> testDatasourceConnection(DatasourceTestRequest request);

    ApiResponse<DatasourceConfigDto> registerDatasource(DatasourceConfigDto request);

    ApiResponse<Void> deleteDatasource(String datasourceId);

    ApiResponse<DatasourceConfigDto> getDatasource(String datasourceId);

    ApiResponse<List<DatasourceConfigDto>> listDatasources();
}

