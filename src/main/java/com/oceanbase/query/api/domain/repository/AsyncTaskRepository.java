package com.oceanbase.query.api.domain.repository;

import com.oceanbase.query.api.domain.entity.AsyncTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AsyncTaskRepository extends JpaRepository<AsyncTask, String> {

    Optional<AsyncTask> findByTaskId(String taskId);
}

