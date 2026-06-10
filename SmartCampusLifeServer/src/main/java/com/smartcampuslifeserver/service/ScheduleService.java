package com.smartcampuslifeserver.service;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.*;

import java.util.List;

public interface ScheduleService {

    Result<List<ScheduleEventResponse>> listEvents(String userId, String startDate, String endDate,
                                                   String viewType, String category);

    Result<CreateEventResponse> createEvent(String userId, CreateEventRequest request);

    Result<Void> updateEvent(String userId, String eventId, UpdateEventRequest request);

    Result<Void> deleteEvent(String userId, String eventId);

    Result<Void> updateStatus(String userId, String eventId, UpdateEventStatusRequest request);

    Result<ConflictCheckResponse> checkConflict(String userId, ConflictCheckRequest request);

    Result<List<ReminderResponse>> listReminders(String userId);

    Result<Void> ackReminder(String userId, ReminderAckRequest request);
}
