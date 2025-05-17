package com.oceanbase.query.api.domain.repository;

import com.oceanbase.query.api.domain.entity.DatasourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatasourceConfigRepository extends JpaRepository<DatasourceConfig, Long> {

    Optional<DatasourceConfig> findByDatasourceId(String datasourceId);

    boolean existsByDatasourceId(String datasourceId);

    void deleteByDatasourceId(String datasourceId);
}

