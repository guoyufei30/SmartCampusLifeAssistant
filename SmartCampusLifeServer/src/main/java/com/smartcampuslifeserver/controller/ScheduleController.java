package com.smartcampuslifeserver.controller;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.common.utils.SecurityUtils;
import com.smartcampuslifeserver.dto.*;
import com.smartcampuslifeserver.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/schedule/events")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public Result<List<ScheduleEventResponse>> listEvents(
            @RequestParam String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String viewType,
            @RequestParam(required = false) String category) {
        return scheduleService.listEvents(SecurityUtils.getUserId(), startDate, endDate, viewType, category);
    }

    @PostMapping
    public Result<CreateEventResponse> createEvent(@RequestBody CreateEventRequest request) {
        return scheduleService.createEvent(SecurityUtils.getUserId(), request);
    }

    @PutMapping("/{eventId}")
    public Result<Void> updateEvent(@PathVariable String eventId, @RequestBody UpdateEventRequest request) {
        return scheduleService.updateEvent(SecurityUtils.getUserId(), eventId, request);
    }

    @DeleteMapping("/{eventId}")
    public Result<Void> deleteEvent(@PathVariable String eventId) {
        return scheduleService.deleteEvent(SecurityUtils.getUserId(), eventId);
    }

    @PatchMapping("/{eventId}/status")
    public Result<Void> updateStatus(@PathVariable String eventId, @RequestBody UpdateEventStatusRequest request) {
        return scheduleService.updateStatus(SecurityUtils.getUserId(), eventId, request);
    }

    @PostMapping("/check_conflict")
    public Result<ConflictCheckResponse> checkConflict(@RequestBody ConflictCheckRequest request) {
        return scheduleService.checkConflict(SecurityUtils.getUserId(), request);
    }

    @GetMapping("/reminder")
    public Result<List<ReminderResponse>> listReminders() {
        return scheduleService.listReminders(SecurityUtils.getUserId());
    }

    @PostMapping("/reminder/ack")
    public Result<Void> ackReminder(@RequestBody ReminderAckRequest request) {
        return scheduleService.ackReminder(SecurityUtils.getUserId(), request);
    }
}
