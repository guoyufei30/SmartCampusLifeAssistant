package com.smartcampuslifeserver.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String userId;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 15)
    private String nickname;

    @Column(length = 255)
    private String avatar;

    @Column(length = 10)
    private String gender;

    @Column(length = 20)
    private String grade;

    @Column(length = 20)
    private String birthDate;

    @Column(length = 50)
    private String department;

    private Integer height;

    private Double weight;

    @Column(nullable = false, length = 20)
    private String role = "user";

    @Column(nullable = false, length = 20)
    private String status = "normal";

    private String frozenTime;

    @Column(length = 20)
    private String frozenReason;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime lastLoginTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
