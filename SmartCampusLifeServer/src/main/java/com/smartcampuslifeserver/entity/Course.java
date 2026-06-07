package com.smartcampuslifeserver.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "course")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 50)
    private String userId;

    @Column(nullable = false)
    private Long semesterId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String weekPattern;

    @Column(nullable = false)
    private Integer dayOfWeek;

    @Column(nullable = false)
    private Integer startPeriod;

    @Column(nullable = false)
    private Integer endPeriod;

    @Column(length = 100)
    private String location;
}
