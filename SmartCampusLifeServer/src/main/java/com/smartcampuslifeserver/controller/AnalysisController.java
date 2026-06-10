package com.smartcampuslifeserver.controller;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.common.utils.SecurityUtils;
import com.smartcampuslifeserver.dto.analysis.*;
import com.smartcampuslifeserver.service.AnalysisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GetMapping("/sleep")
    public Result<SleepAnalysisResponse> analyzeSleep() {
        return analysisService.analyzeSleep(SecurityUtils.getUserId());
    }

    @GetMapping("/exercise")
    public Result<ExerciseAnalysisResponse> analyzeExercise() {
        return analysisService.analyzeExercise(SecurityUtils.getUserId());
    }

    @GetMapping("/pressure")
    public Result<PressureAnalysisResponse> analyzePressure() {
        return analysisService.analyzePressure(SecurityUtils.getUserId());
    }

    @GetMapping("/procrastination")
    public Result<ProcrastinationAnalysisResponse> analyzeProcrastination() {
        return analysisService.analyzeProcrastination(SecurityUtils.getUserId());
    }

    @GetMapping("/sport_suggestion")
    public Result<SportSuggestionResponse> sportSuggestion() {
        return analysisService.sportSuggestion(SecurityUtils.getUserId());
    }
}
