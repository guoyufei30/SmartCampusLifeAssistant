package com.smartcampuslifeserver.repository;

import com.smartcampuslifeserver.entity.UserAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAnnouncementRepository extends JpaRepository<UserAnnouncement, UserAnnouncement.UserAnnouncementId> {

    List<UserAnnouncement> findByUserIdAndDismissedTrue(String userId);

    Optional<UserAnnouncement> findByUserIdAndAnnouncementId(String userId, Long announcementId);

    boolean existsByUserIdAndAnnouncementId(String userId, Long announcementId);

    void deleteByUserId(String userId);
}
