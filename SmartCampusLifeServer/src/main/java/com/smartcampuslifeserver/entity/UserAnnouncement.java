package com.smartcampuslifeserver.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "user_announcement")
@IdClass(UserAnnouncement.UserAnnouncementId.class)
public class UserAnnouncement {

    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Id
    @Column(name = "announcement_id", nullable = false)
    private Long announcementId;

    @Column(nullable = false)
    private Boolean dismissed = true;

    @Data
    public static class UserAnnouncementId implements Serializable {

        private String userId;
        private Long announcementId;
    }
}
