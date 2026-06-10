package com.smartcampuslifeserver.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "schedule_event")
public class ScheduleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 50)
    private String userId;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, length = 20)
    private String category;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime deadline;

    @Column(length = 100)
    private String location;

    @Column(length = 500)
    private String remark;

    @Column(length = 20)
    private String reminder;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(length = 36)
    private String courseId;

    @Column(nullable = false)
    private Boolean reminderAcked = false;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
