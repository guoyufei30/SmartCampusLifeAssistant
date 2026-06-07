package com.smartcampuslifeserver.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sleep_record")
public class SleepRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 50)
    private String userId;

    @Column(nullable = false)
    private LocalDate sleepDate;

    @Column(nullable = false)
    private LocalDateTime bedTime;

    @Column(nullable = false)
    private LocalDateTime wakeTime;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal duration;

    @Column(nullable = false, length = 10)
    private String quality;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
