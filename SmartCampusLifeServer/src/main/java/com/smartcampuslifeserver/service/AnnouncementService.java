package com.smartcampuslifeserver.service;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.AnnouncementResponse;

import java.util.List;

public interface AnnouncementService {

    Result<List<AnnouncementResponse>> listAnnouncements(String userId);

    Result<Void> dismissAnnouncement(String userId, Long announcementId);
}
