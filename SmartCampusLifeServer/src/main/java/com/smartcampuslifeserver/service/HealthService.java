package com.smartcampuslifeserver.service;

import com.smartcampuslifeserver.common.result.Result;
import com.smartcampuslifeserver.dto.health.*;

import java.util.List;

public interface HealthService {

    Result<List<SleepRecordResponse>> listSleep(String userId, String startDate, String endDate);

    Result<CreateSleepResponse> createSleep(String userId, CreateSleepRequest request);

    Result<Void> deleteSleep(String userId, String recordId);

    Result<List<ExerciseRecordResponse>> listExercise(String userId, String startDate, String endDate);

    Result<ExerciseRecordResponse> createExercise(String userId, CreateExerciseRequest request);

    Result<List<WeightRecordResponse>> listWeight(String userId, String startDate, String endDate);

    Result<WeightRecordResponse> createWeight(String userId, CreateWeightRequest request);

    Result<ChartResponse> getChart(String userId, String type, String startDate, String endDate);
}
