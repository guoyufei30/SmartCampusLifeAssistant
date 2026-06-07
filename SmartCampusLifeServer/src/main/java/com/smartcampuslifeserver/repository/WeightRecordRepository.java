package com.smartcampuslifeserver.repository;

import com.smartcampuslifeserver.entity.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeightRecordRepository extends JpaRepository<WeightRecord, String> {

    List<WeightRecord> findByUserId(String userId);

    Optional<WeightRecord> findByUserIdAndRecordDate(String userId, LocalDate recordDate);

    List<WeightRecord> findByUserIdAndRecordDateBetween(String userId, LocalDate startDate, LocalDate endDate);

    Optional<WeightRecord> findByIdAndUserId(String id, String userId);

    void deleteByCreateTimeBefore(LocalDateTime time);

    void deleteByUserId(String userId);
}
