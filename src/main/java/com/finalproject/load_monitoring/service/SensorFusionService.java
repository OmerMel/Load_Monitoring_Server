package com.finalproject.load_monitoring.service;

import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.entity.OccupancyLog;
import com.finalproject.load_monitoring.repository.OccupancyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorFusionService {

    private final OccupancyLogRepository occupancyLogRepository;

    // --- Configuration Constants ---
    private static final int PERSISTENT_GAP_THRESHOLD = 8;
    private static final double DRIFT_CORRECTION_ALPHA = 0.30;
    private static final int CLOSE_COUNT_THRESHOLD = 5;
    private static final int CLOSE_DELTA_THRESHOLD = 3;

    // --- Weights ---
    private static final double[] WEIGHTS_AGREEMENT = {0.50, 0.50}; // {cameraWeight, irWeight}
    private static final double[] WEIGHTS_SIMILAR_TREND = {0.45, 0.55}; // {cameraWeight, irWeight}
    private static final double[] WEIGHTS_SUSPICIOUS_CAMERA = {0.25, 0.75}; // {cameraWeight, irWeight}
    private static final double[] WEIGHTS_SUSPICIOUS_IR = {0.70, 0.30}; // {cameraWeight, irWeight}
    private static final double[] WEIGHTS_FALLBACK = {0.40, 0.60}; // {cameraWeight, irWeight}

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Calculates the final occupancy based on camera and ToF (IR) sensor data.
    public int calculateOccupancy(SensorDataDTO data, Long carriageId) {
        int currentCamera = Math.max(0, data.getCameraCount()); // current camera count
        int currentIr = Math.max(0, data.getIrCount()); // current IR count

        // Fetch recent historical logs
        List<OccupancyLog> recentLogs = occupancyLogRepository.findTop3ByCarriage_CarriageIdOrderByTimestampDesc(carriageId);

        if (recentLogs.isEmpty()) {
            log.info("No historical logs found for Carriage {}. Using default fallback weights.", carriageId);
            // No history, use default balanced weights
            double[] weights = WEIGHTS_FALLBACK;
            return calculateWeighted(currentCamera, currentIr, weights); // {cameraWeight: 0.40, irWeight: 0.60}
        }

        // Calculate deltas (current - previous)
        OccupancyLog prevLog = recentLogs.get(0); // latest log
        int prevCamera = Math.max(0, prevLog.getCameraCount());
        int prevIr = Math.max(0, prevLog.getIrCount());

        int countDiff = Math.abs(currentCamera - currentIr);
        int cameraDelta = currentCamera - prevCamera;
        int irDelta = currentIr - prevIr;
        int deltaDiff = Math.abs(cameraDelta - irDelta);

        log.info("Deltas - countDiff: {}, cameraDelta: {}, irDelta: {}, deltaDiff: {}", 
                countDiff, cameraDelta, irDelta, deltaDiff);

        // Determine weights based on cases
        double[] selectedWeights;

        if (countDiff <= CLOSE_COUNT_THRESHOLD) {
            // Case A: Current agreement
            selectedWeights = WEIGHTS_AGREEMENT; // {cameraWeight: 0.50, irWeight: 0.50}
            log.info("Case A: Sensors agree. Using weights: {}", selectedWeights);
        } else if (deltaDiff <= CLOSE_DELTA_THRESHOLD) {
            // Case B: Similar trend
            selectedWeights = WEIGHTS_SIMILAR_TREND; // {cameraWeight: 0.45, irWeight: 0.55}
            log.info("Case B: Similar trend detected. Using weights: {}", selectedWeights);
        } else if (Math.abs(cameraDelta) > Math.abs(irDelta)) {
            // Case C: Suspicious camera jump
            selectedWeights = WEIGHTS_SUSPICIOUS_CAMERA; // {cameraWeight: 0.25, irWeight: 0.75}
            log.info("Case C: Suspicious camera jump. Using weights: {}", selectedWeights);
        } else {
            // Case D: Suspicious IR jump
            selectedWeights = WEIGHTS_SUSPICIOUS_IR; // {cameraWeight: 0.70, irWeight: 0.30}
            log.info("Case D: Suspicious IR jump. Using weights: {}", selectedWeights);
        }

        // Calculate initial fused value based on the chosen case
        int fusedOccupancy = calculateWeighted(currentCamera, currentIr, selectedWeights);

        // Apply Persistent Drift Correction if applicable
        // בודק האם יש פער גדול בין המצלמה לחיישנים גם עכשיו וגם בלוגים האחרונים 
        if (hasPersistentGap(countDiff, recentLogs)) { 
            double cameraAverage = getCameraAverage(currentCamera, recentLogs);
            int prevCalculated = Math.max(0, prevLog.getCalculatedOccupancy()); // previous calculated occupancy
            
            // Formula: fused = previousCalculated + alpha * (cameraAverage - previousCalculated)
            double corrected = prevCalculated + DRIFT_CORRECTION_ALPHA * (cameraAverage - prevCalculated);
            fusedOccupancy = Math.max(0, (int) Math.round(corrected));
            
            log.info("Drift correction applied! Camera Avg: {}, Prev Calculated: {}, New Fused: {}", 
                    cameraAverage, prevCalculated, fusedOccupancy);
        }

        return fusedOccupancy;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Checks if there is a persistent gap between camera and IR counts across the recent history.
     */
    private boolean hasPersistentGap(int countDiff, List<OccupancyLog> recentLogs) {
        // First check current gap
        if (countDiff < PERSISTENT_GAP_THRESHOLD) {
            return false;
        }

        // Then check if the gap existed in all recent logs
        for (OccupancyLog log : recentLogs) {
            int histCamera = Math.max(0, log.getCameraCount());
            int histIr = Math.max(0, log.getIrCount());
            if (Math.abs(histCamera - histIr) < PERSISTENT_GAP_THRESHOLD) {
                return false; // Gap is not persistent
            }
        }
        return true;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Calculates the average camera count across the current payload and recent history.
     */
    private double getCameraAverage(int currentCamera, List<OccupancyLog> recentLogs) {
        int sum = currentCamera;
        for (OccupancyLog log : recentLogs) {
            sum += Math.max(0, log.getCameraCount());
        }
        return (double) sum / (recentLogs.size() + 1);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Calculates the weighted sum and rounds it to the nearest integer.
     */
    private int calculateWeighted(int cameraCount, int irCount, double[] weights) {
        double cameraWeight = weights[0];
        double irWeight = weights[1];
        long fused = Math.round(cameraWeight * cameraCount + irWeight * irCount);
        return Math.max(0, (int) fused);
    }
    //////////////////////////////////////////////////////////////////////////////////////////////
}
