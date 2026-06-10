package com.smartcampuslifeserver.service;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.analysis.*;

import java.time.LocalDate;

public interface AnalysisService {

    Result<SleepAnalysisResponse> analyzeSleep(String userId);

    Result<ExerciseAnalysisResponse> analyzeExercise(String userId);

    Result<PressureAnalysisResponse> analyzePressure(String userId);

    Result<ProcrastinationAnalysisResponse> analyzeProcrastination(String userId);

    Result<SportSuggestionResponse> sportSuggestion(String userId);

    PressureAnalysisResponse calculatePressure(String userId, LocalDate weekStart, LocalDate weekEnd);
}
