package com.oceanbase.query.api.domain.repository;

import com.oceanbase.query.api.domain.entity.QueryScene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuerySceneRepository extends JpaRepository<QueryScene, Long> {

    Optional<QueryScene> findBySceneCode(String sceneCode);

    boolean existsBySceneCode(String sceneCode);
    
    void deleteBySceneCode(String sceneCode);
}
