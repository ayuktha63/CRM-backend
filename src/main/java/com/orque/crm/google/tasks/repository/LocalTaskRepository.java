package com.orque.crm.google.tasks.repository;

import com.orque.crm.google.tasks.entity.LocalTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocalTaskRepository extends JpaRepository<LocalTask, Long> {
    List<LocalTask> findByOwnerIgnoreCaseAndSyncedFalseOrderByCreatedAtDesc(String owner);
    List<LocalTask> findByOwnerIgnoreCaseAndSyncedFalse(String owner);
}
