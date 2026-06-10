package com.smartcampuslifeserver.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operation_logs")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String adminId;

    @Column(length = 50)
    private String adminNickname;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 50)
    private String actionText;

    @Column(length = 20)
    private String targetType;

    @Column(length = 50)
    private String targetId;

    @Column(length = 50)
    private String targetNickname;

    @Column(length = 30)
    private String reasonCode;

    @Column(length = 100)
    private String reasonText;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(length = 50)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
