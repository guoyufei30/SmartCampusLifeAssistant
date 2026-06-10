package com.smartcampuslifeserver.repository;

import com.smartcampuslifeserver.entity.ScheduleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleEventRepository extends JpaRepository<ScheduleEvent, String> {

    List<ScheduleEvent> findByUserId(String userId);

    List<ScheduleEvent> findByUserIdAndStatus(String userId, String status);

    List<ScheduleEvent> findByUserIdAndCategory(String userId, String category);

    List<ScheduleEvent> findByUserIdAndStartTimeBetween(String userId, LocalDateTime start, LocalDateTime end);

    Optional<ScheduleEvent> findByIdAndUserId(String id, String userId);

    Optional<ScheduleEvent> findByCourseIdAndUserId(String courseId, String userId);

    void deleteByCreateTimeBefore(LocalDateTime time);

    void deleteByUserId(String userId);
}
