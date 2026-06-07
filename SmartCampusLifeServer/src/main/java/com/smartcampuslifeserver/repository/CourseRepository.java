package com.smartcampuslifeserver.repository;

import com.smartcampuslifeserver.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {

    List<Course> findByUserId(String userId);

    List<Course> findByUserIdAndSemesterId(String userId, Long semesterId);

    Optional<Course> findByIdAndUserId(String id, String userId);

    void deleteByUserId(String userId);
}
