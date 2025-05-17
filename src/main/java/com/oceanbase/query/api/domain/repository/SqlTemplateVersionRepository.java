package com.oceanbase.query.api.domain.repository;

import com.oceanbase.query.api.domain.entity.SqlTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SqlTemplateVersionRepository extends JpaRepository<SqlTemplateVersion, Long> {

    List<SqlTemplateVersion> findByTemplateId(String templateId);

    void deleteByTemplateId(String templateId);
}

