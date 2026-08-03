package com.finalproject.load_monitoring.service;

import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.repository.OccupancyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class WeightSensorFusionService {

    private final OccupancyLogRepository occupancyLogRepository;

    // --- Configuration Constants ---
    // אם החומרה פספסה איפוס וההפרש עדיין גדול, השרת יתערב
    private static final int SERVER_SAFETY_THRESHOLD = 10;

    // --- Weights ---
    private static final double[] WEIGHTS_AGREEMENT = {0.50, 0.50};     // בשגרה: חצי חצי להחלקת רעשים
    private static final double[] WEIGHTS_TRUST_CAMERA = {1.00, 0.00};  // במקרה קצה: המצלמה קובעת
    //////////////////////////////////////////////////////////////////////////////////////////////
    // Calculates the final occupancy based on camera and ToF (IR) sensor data.
    public int calculateOccupancy(SensorDataDTO data) {
        int currentCamera = Math.max(0, data.getCameraCount());
        int currentIr = Math.max(0, data.getIrCount());

        int countDiff = Math.abs(currentCamera - currentIr);
        double[] selectedWeights;

        if (countDiff <= SERVER_SAFETY_THRESHOLD) {
            // מצב רגיל: או שהחיישנים קרובים, או שהחומרה כבר ביצעה איפוס והם שווים
            selectedWeights = WEIGHTS_AGREEMENT;
        } else {
            // משהו השתבש בקצה: הגיע פער גדול למרות מנגנון האיפוס. השרת מפעיל רשת ביטחון
            selectedWeights = WEIGHTS_TRUST_CAMERA;
            log.warn("Server safety net triggered! Large gap escaped edge reset (diff: {}). Trusting camera 100%.", countDiff);
        }

        // חישוב המשקל והערגול
        long fused = Math.round(selectedWeights[0] * currentCamera + selectedWeights[1] * currentIr);
        return Math.max(0, (int) fused);
    }

//    //////////////////////////////////////////////////////////////////////////////////////////////
//    /**
//     * Checks if there is a persistent gap between camera and IR counts across the recent history.
//     */
//    private boolean hasPersistentGap(int countDiff, List<OccupancyLog> recentLogs) {
//        // First check current gap
//        if (countDiff < PERSISTENT_GAP_THRESHOLD) {
//            return false;
//        }
//
//        // Then check if the gap existed in all recent logs
//        for (OccupancyLog log : recentLogs) {
//            int histCamera = Math.max(0, log.getCameraCount());
//            int histIr = Math.max(0, log.getIrCount());
//            if (Math.abs(histCamera - histIr) < PERSISTENT_GAP_THRESHOLD) {
//                return false; // Gap is not persistent
//            }
//        }
//        return true;
//    }
//
//    //////////////////////////////////////////////////////////////////////////////////////////////
//    /**
//     * Calculates the average camera count across the current payload and recent history.
//     */
//    private double getCameraAverage(int currentCamera, List<OccupancyLog> recentLogs) {
//        int sum = currentCamera;
//        for (OccupancyLog log : recentLogs) {
//            sum += Math.max(0, log.getCameraCount());
//        }
//        return (double) sum / (recentLogs.size() + 1);
//    }
//
//    //////////////////////////////////////////////////////////////////////////////////////////////
//    /**
//     * Calculates the weighted sum and rounds it to the nearest integer.
//     */
//    private int calculateWeighted(int cameraCount, int irCount, double[] weights) {
//        double cameraWeight = weights[0];
//        double irWeight = weights[1];
//        long fused = Math.round(cameraWeight * cameraCount + irWeight * irCount);
//        return Math.max(0, (int) fused);
//    }
//    //////////////////////////////////////////////////////////////////////////////////////////////
}
