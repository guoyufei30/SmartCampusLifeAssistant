package com.smartcampuslifeserver.controller;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.common.utils.SecurityUtils;
import com.smartcampuslifeserver.dto.health.*;
import com.smartcampuslifeserver.service.HealthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/sleep")
    public Result<List<SleepRecordResponse>> listSleep(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return healthService.listSleep(SecurityUtils.getUserId(), startDate, endDate);
    }

    @PostMapping("/sleep")
    public Result<CreateSleepResponse> createSleep(@RequestBody CreateSleepRequest request) {
        return healthService.createSleep(SecurityUtils.getUserId(), request);
    }

    @DeleteMapping("/sleep/{recordId}")
    public Result<Void> deleteSleep(@PathVariable String recordId) {
        return healthService.deleteSleep(SecurityUtils.getUserId(), recordId);
    }

    @GetMapping("/exercise")
    public Result<List<ExerciseRecordResponse>> listExercise(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return healthService.listExercise(SecurityUtils.getUserId(), startDate, endDate);
    }

    @PostMapping("/exercise")
    public Result<ExerciseRecordResponse> createExercise(@RequestBody CreateExerciseRequest request) {
        return healthService.createExercise(SecurityUtils.getUserId(), request);
    }

    @GetMapping("/weight")
    public Result<List<WeightRecordResponse>> listWeight(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return healthService.listWeight(SecurityUtils.getUserId(), startDate, endDate);
    }

    @PostMapping("/weight")
    public Result<WeightRecordResponse> createWeight(@RequestBody CreateWeightRequest request) {
        return healthService.createWeight(SecurityUtils.getUserId(), request);
    }

    @GetMapping("/chart")
    public Result<ChartResponse> getChart(
            @RequestParam String type,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return healthService.getChart(SecurityUtils.getUserId(), type, startDate, endDate);
    }
}
