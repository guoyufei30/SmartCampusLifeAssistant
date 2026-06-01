package com.smartcampuslifeserver.repository;

import com.smartcampuslifeserver.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    Page<OperationLog> findByAction(String action, Pageable pageable);

    Page<OperationLog> findByTargetType(String targetType, Pageable pageable);

    Page<OperationLog> findByActionAndTargetType(String action, String targetType, Pageable pageable);

    void deleteByCreateTimeBefore(LocalDateTime dateTime);

    long countByCreateTimeBefore(LocalDateTime dateTime);
}
