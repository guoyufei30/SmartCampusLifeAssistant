package com.smartcampuslifeserver.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "verify_code")
@IdClass(VerifyCode.VerifyCodeId.class)
public class VerifyCode {

    @Id
    @Column(nullable = false, length = 20)
    private String phone;

    @Id
    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expireTime;

    @Data
    public static class VerifyCodeId implements Serializable {

        private String phone;
        private String type;
    }
}
