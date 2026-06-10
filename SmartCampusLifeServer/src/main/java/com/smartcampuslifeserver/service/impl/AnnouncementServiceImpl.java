package com.smartcampuslifeserver.service.impl;

import com.smartcampuslifeserver.common.exception.BusinessException;
import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.AnnouncementResponse;
import com.smartcampuslifeserver.entity.Announcement;
import com.smartcampuslifeserver.entity.User;
import com.smartcampuslifeserver.entity.UserAnnouncement;
import com.smartcampuslifeserver.repository.AnnouncementRepository;
import com.smartcampuslifeserver.repository.UserAnnouncementRepository;
import com.smartcampuslifeserver.repository.UserRepository;
import com.smartcampuslifeserver.service.AnnouncementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserAnnouncementRepository userAnnouncementRepository;
    private final UserRepository userRepository;

    public AnnouncementServiceImpl(AnnouncementRepository announcementRepository,
                                   UserAnnouncementRepository userAnnouncementRepository,
                                   UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.userAnnouncementRepository = userAnnouncementRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Result<List<AnnouncementResponse>> listAnnouncements(String userId) {
        validateUser(userId);

        Set<Long> dismissedIds = userAnnouncementRepository.findByUserIdAndDismissedTrue(userId)
                .stream()
                .map(UserAnnouncement::getAnnouncementId)
                .collect(Collectors.toSet());

        List<AnnouncementResponse> list = announcementRepository.findByStatusOrderByCreateTimeDesc("active")
                .stream()
                .filter(a -> !dismissedIds.contains(a.getId()))
                .map(this::toResponse)
                .toList();

        return Result.success(list, "获取成功");
    }

    @Override
    @Transactional
    public Result<Void> dismissAnnouncement(String userId, Long announcementId) {
        validateUser(userId);

        if (!announcementRepository.existsById(announcementId)) {
            throw new BusinessException(404, "公告不存在");
        }

        if (userAnnouncementRepository.existsByUserIdAndAnnouncementId(userId, announcementId)) {
            return Result.success(null, "已关闭");
        }

        UserAnnouncement record = new UserAnnouncement();
        record.setUserId(userId);
        record.setAnnouncementId(announcementId);
        record.setDismissed(true);
        userAnnouncementRepository.save(record);

        return Result.success(null, "已关闭");
    }

    private AnnouncementResponse toResponse(Announcement announcement) {
        AnnouncementResponse response = new AnnouncementResponse();
        response.setId(announcement.getId());
        response.setContent(announcement.getContent());
        response.setType(announcement.getType());
        response.setDismissible(announcement.getDismissible());
        return response;
    }

    private void validateUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        if (Boolean.TRUE.equals(user.getForceChangePassword())) {
            throw new BusinessException(403, "请先修改临时密码", "temp_password_required");
        }
        if ("frozen".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被冻结", "account_frozen");
        }
    }
}
