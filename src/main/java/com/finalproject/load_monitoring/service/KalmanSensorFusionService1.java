package com.finalproject.load_monitoring.service;

import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.entity.OccupancyLog;
import com.finalproject.load_monitoring.repository.OccupancyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class KalmanSensorFusionService1 {

    private final OccupancyLogRepository occupancyLogRepository;

    // זיכרון בתוך השרת ששומר את חוסר הוודאות (P) עבור כל קרון בנפרד
    private final Map<Long, Double> errorCovarianceCache = new ConcurrentHashMap<>();

    // --- קבועי קלמן (כיול מערכת) ---
    private static final double PROCESS_NOISE_Q = 0.15; // כמה דריפט ה-ToF צובר ב-2 דקות
    private static final double MEASUREMENT_NOISE_R = 1.5; // רמת הרעש של המצלמה
    private static final int MAX_REALISTIC_PASSENGER_FLOW = 30; // מקסימום אנשים ב-2 דקות

    public int calculateOccupancy(SensorDataDTO data, Long carriageId) {
        int currentCamera = Math.max(0, data.getCameraCount());
        int currentIr = Math.max(0, data.getIrCount());

        // 1. שליפת מצב האכלוס הקודם מה-DB
        List<OccupancyLog> recentLogs = occupancyLogRepository.findTop3ByCarriage_CarriageIdOrderByTimestampDesc(carriageId);

        if (recentLogs.isEmpty()) {
            log.info("No history for Carriage {}. Initializing with Camera count.", carriageId);
            errorCovarianceCache.put(carriageId, 1.0); // אתחול ה-P בזיכרון
            return currentCamera;
        }

        OccupancyLog prevLog = recentLogs.get(0);
        double prevCalculated = prevLog.getCalculatedOccupancy();
        int prevIr = Math.max(0, prevLog.getIrCount());

        // 2. שליפת ה-P של הקרון הספציפי מהזיכרון של השרת (אם לא קיים, ברירת המחדל היא 1.0)
        double prevP = errorCovarianceCache.getOrDefault(carriageId, 1.0);

        // 3. חישוב השינוי בחיישן (ToF Delta)
        int tofDelta = currentIr - prevIr;

        // הגנה מפני "מלכודת איפוס החומרה" בקצה
        if (Math.abs(tofDelta) > MAX_REALISTIC_PASSENGER_FLOW) {
            log.warn("Edge reset detected or anomaly in IR Delta ({}). Skipping prediction step.", tofDelta);
            tofDelta = 0;
            prevP = 5.0; // מעלים את חוסר הוודאות כדי שנקבל תיקון חזק מהמצלמה
        }

        // =================================================================
        // שלב א': חיזוי (Prediction) - מריצים על נתון ה-ToF
        // =================================================================
        double predictedState = prevCalculated + tofDelta;
        double predictedP = prevP + PROCESS_NOISE_Q;

        // =================================================================
        // שלב ב': עדכון (Update) - מריצים על נתון המצלמה
        // =================================================================
        double kalmanGain = predictedP / (predictedP + MEASUREMENT_NOISE_R);
        double currentState = predictedState + kalmanGain * (currentCamera - predictedState);
        double currentP = (1 - kalmanGain) * predictedP;
        // =================================================================

        // 4. שמירת ה-P המעודכן בזיכרון של השרת בשביל הסבב הבא
        errorCovarianceCache.put(carriageId, currentP);

        int finalOccupancy = Math.max(0, (int) Math.round(currentState));

        log.info("Carriage {} | Kalman Filter - Gain: {}, Predicted: {}, Camera: {}, Final: {}, Saved P: {}",
                carriageId, String.format("%.2f", kalmanGain), String.format("%.2f", predictedState),
                currentCamera, finalOccupancy, String.format("%.2f", currentP));

        return finalOccupancy;
    }
}