package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {

    private String nickname;
    private String gender;
    private String grade;
    private String birthDate;
    private String department;
    private Integer height;
    private Double weight;
}
