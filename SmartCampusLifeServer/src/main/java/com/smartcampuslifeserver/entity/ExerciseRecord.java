package com.smartcampuslifeserver.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exercise_record")
public class ExerciseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 50)
    private String userId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private Integer duration;

    private Integer count = 1;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
