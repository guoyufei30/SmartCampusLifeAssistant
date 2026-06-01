package com.smartcampuslifeserver.repository;

import com.smartcampuslifeserver.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    Page<Announcement> findByStatus(String status, Pageable pageable);

    Page<Announcement> findByType(String type, Pageable pageable);

    Page<Announcement> findByStatusAndType(String status, String type, Pageable pageable);
}
