package com.smartcampuslifeserver.dto;

import lombok.Data;

@Data
public class UserProfileResponse {

    private String userId;
    private String phone;
    private String nickname;
    private String avatar;
    private String gender;
    private String grade;
    private String birthDate;
    private String department;
    private String height;
    private String weight;
    private String createTime;
}
