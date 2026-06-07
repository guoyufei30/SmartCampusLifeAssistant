package com.smartcampuslifeserver.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "weekly_report")
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String userId;

    @Column(nullable = false)
    private Integer weekNumber;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String sections;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    private LocalDateTime regenerateAvailableUntil;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
