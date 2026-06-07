package com.smartcampuslifeserver.repository;

import com.smartcampuslifeserver.entity.ExerciseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExerciseRecordRepository extends JpaRepository<ExerciseRecord, String> {

    List<ExerciseRecord> findByUserId(String userId);

    List<ExerciseRecord> findByUserIdAndRecordDate(String userId, LocalDate recordDate);

    List<ExerciseRecord> findByUserIdAndRecordDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    Optional<ExerciseRecord> findByIdAndUserId(String id, String userId);

    void deleteByCreateTimeBefore(LocalDateTime time);

    void deleteByUserId(String userId);
}
