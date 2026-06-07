package com.smartcampuslifeserver.repository;

import com.smartcampuslifeserver.entity.ExceptionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ExceptionLogRepository extends JpaRepository<ExceptionLog, Long> {

    Page<ExceptionLog> findByExceptionType(String exceptionType, Pageable pageable);

    void deleteByCreateTimeBefore(LocalDateTime dateTime);

    long countByCreateTimeBefore(LocalDateTime dateTime);

    void deleteByUserId(String userId);
}
