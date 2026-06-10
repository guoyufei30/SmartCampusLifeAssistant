package com.smartcampuslifeserver.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "exception_logs")
public class ExceptionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String userId;

    @Column(nullable = false, length = 50)
    private String exceptionType;

    @Column(columnDefinition = "TEXT")
    private String exceptionDetail;

    @Column(length = 255)
    private String requestUrl;

    @Column(length = 50)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
