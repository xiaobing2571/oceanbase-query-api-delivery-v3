package com.oceanbase.query.api.domain.repository;

import com.oceanbase.query.api.domain.entity.SqlTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SqlTemplateRepository extends JpaRepository<SqlTemplate, Long> {

    Optional<SqlTemplate> findByTemplateId(String templateId);

    boolean existsByTemplateId(String templateId);
    
    void deleteByTemplateId(String templateId);
    
    List<SqlTemplate> findBySceneCode(String sceneCode);
}
