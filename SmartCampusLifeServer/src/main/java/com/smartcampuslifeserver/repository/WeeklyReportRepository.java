package com.smartcampuslifeserver.repository;

import com.smartcampuslifeserver.entity.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    List<WeeklyReport> findByUserId(String userId);

    List<WeeklyReport> findByUserIdOrderByWeekNumberDesc(String userId);

    Optional<WeeklyReport> findByUserIdAndWeekNumber(String userId, Integer weekNumber);

    Optional<WeeklyReport> findByIdAndUserId(Long id, String userId);

    void deleteByCreateTimeBefore(LocalDateTime time);

    void deleteByUserId(String userId);
}
