package com.smartcampuslifeserver.controller;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.common.utils.SecurityUtils;
import com.smartcampuslifeserver.dto.AnnouncementResponse;
import com.smartcampuslifeserver.service.AnnouncementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public Result<List<AnnouncementResponse>> listAnnouncements() {
        return announcementService.listAnnouncements(SecurityUtils.getUserId());
    }

    @PostMapping("/{id}/dismiss")
    public Result<Void> dismissAnnouncement(@PathVariable Long id) {
        return announcementService.dismissAnnouncement(SecurityUtils.getUserId(), id);
    }
}
