package com.finalproject.load_monitoring.service;

import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.entity.OccupancyLog;
import com.finalproject.load_monitoring.repository.OccupancyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * One-dimensional Kalman Filter that estimates the number of passengers in a train carriage.
 * The ToF sensors count entrances and exits, so their change predicts the new count,
 * and the camera count is a direct measurement that corrects that prediction.
 * The state of every carriage is now fetched from and stored in the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KalmanSensorFusionService {

    private final OccupancyLogRepository occupancyLogRepository;

    // Uncertainty of the first estimate of a carriage.
    private static final double INITIAL_UNCERTAINTY = 1.0;
    // Uncertainty added by the ToF based prediction (about 5 passengers).
    private static final double TOF_PREDICTION_UNCERTAINTY = 25.0;
    // Uncertainty of the camera count (about 4 passengers).
    private static final double CAMERA_COUNT_UNCERTAINTY = 16.0;

    //////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Fuses the camera count and the ToF count of one sample into a single occupancy value.
     */
    public int calculateOccupancy(SensorDataDTO data, Long carriageId) {
        String carriageKey = buildCarriageKey(data, carriageId);
        int cameraCount = data.getCameraCount();
        int currentTofCount = data.getIrCount();

        Optional<OccupancyLog> lastLogOpt = occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriageId);

        // If there is no previous log, initialize the carriage.
        if ((lastLogOpt.isEmpty()) || (lastLogOpt.get().getCalculatedUncertainty() == null)) {
            return initializeCarriage(data, carriageKey, cameraCount, currentTofCount);
        }

        OccupancyLog lastLog = lastLogOpt.get(); // Get the last log.
        double lastEstimatedCount = lastLog.getCalculatedOccupancy(); // Get the last estimated count.
        double lastUncertainty = lastLog.getCalculatedUncertainty() != null ? lastLog.getCalculatedUncertainty() : INITIAL_UNCERTAINTY;
        int lastTofCount = lastLog.getIrCount();

        // Calculate the passenger count change detected by the ToF sensors.
        int deltaTofCount = currentTofCount - lastTofCount;

        // Predict the current count using the previous estimate and the ToF change.
        double predictedPassengerCount = lastEstimatedCount + deltaTofCount; // Predicted passenger count.
        double predictedUncertainty = lastUncertainty + TOF_PREDICTION_UNCERTAINTY; // Predicted uncertainty.

        // Correct the prediction using the direct camera measurement.
        double cameraCorrectionWeight = predictedUncertainty / (predictedUncertainty + CAMERA_COUNT_UNCERTAINTY);
        double correctedPassengerCount =
                predictedPassengerCount + cameraCorrectionWeight * (cameraCount - predictedPassengerCount);
        double correctedUncertainty = (1.0 - cameraCorrectionWeight) * predictedUncertainty;

        // Save the updated uncertainty to the DTO for persistence in the next sampling cycle.
        data.setCalculatedUncertainty(correctedUncertainty);

        int finalPassengerCount = Math.max(0, (int) Math.round(correctedPassengerCount));

        log.debug("Carriage {} | Kalman - LastEstimate: {}, LastToF: {}, CurrentToF: {}, ToFChange: {}, " +
                        "Predicted: {}, Camera: {}, CameraWeight: {}, Corrected: {}, Final: {}",
                carriageKey,
                String.format("%.2f", lastEstimatedCount),
                lastTofCount,
                currentTofCount,
                deltaTofCount,
                String.format("%.2f", predictedPassengerCount),
                cameraCount,
                String.format("%.2f", cameraCorrectionWeight),
                String.format("%.2f", correctedPassengerCount),
                finalPassengerCount);

        return finalPassengerCount;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Creates the first state of a carriage from the camera count.
    private int initializeCarriage(SensorDataDTO data, String carriageKey, int cameraCount, int currentTofCount) {
        data.setCalculatedUncertainty(INITIAL_UNCERTAINTY);

        int finalPassengerCount = Math.max(0, cameraCount);

        log.debug("Carriage {} | Kalman initialized - Estimate: {}, ToF baseline: {}, Uncertainty: {}",
                carriageKey, finalPassengerCount, currentTofCount, INITIAL_UNCERTAINTY);

        return finalPassengerCount;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // The carriage id is the stable key, with train id and carriage number as a fallback.
    private String buildCarriageKey(SensorDataDTO data, Long carriageId) {
        if (carriageId != null) {
            return String.valueOf(carriageId);
        }
        return data.getTrainId() + "-" + data.getCarriageNumber();
    }
}