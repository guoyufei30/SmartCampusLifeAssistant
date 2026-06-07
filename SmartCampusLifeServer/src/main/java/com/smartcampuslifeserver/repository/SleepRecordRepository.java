package com.smartcampuslifeserver.repository;

import com.smartcampuslifeserver.entity.SleepRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SleepRecordRepository extends JpaRepository<SleepRecord, String> {

    List<SleepRecord> findByUserId(String userId);

    Optional<SleepRecord> findByUserIdAndSleepDate(String userId, LocalDate sleepDate);

    List<SleepRecord> findByUserIdAndSleepDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    Optional<SleepRecord> findByIdAndUserId(String id, String userId);

    void deleteByCreateTimeBefore(LocalDateTime time);

    void deleteByUserId(String userId);
}
