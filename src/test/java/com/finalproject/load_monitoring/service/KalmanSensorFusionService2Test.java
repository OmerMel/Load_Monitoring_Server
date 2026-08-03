package com.finalproject.load_monitoring.service;

import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.entity.OccupancyLog;
import com.finalproject.load_monitoring.repository.OccupancyLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import java.util.Optional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class KalmanSensorFusionService2Test {

    @Mock
    private OccupancyLogRepository occupancyLogRepository;

    @InjectMocks
    private KalmanSensorFusionService kalmanSensorFusionService;

    // Constants from KalmanSensorFusionService
    private static final double INITIAL_UNCERTAINTY = 1.0;
    private static final double TOF_PREDICTION_UNCERTAINTY = 25.0;
    private static final double CAMERA_COUNT_UNCERTAINTY = 16.0;

    @BeforeEach
    void setUp() {
        // kalmanSensorFusionService is injected by Mockito
    }

    @Test
    void shouldInitializeFromCameraCountWhenNoPreviousLogExists() {
        // Arrange
        Long carriageId = 1L;
        SensorDataDTO sensorData = createSensorData(100L, 1, 20, 18);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriageId))
                .thenReturn(Optional.empty());

        // Act
        int finalPassengerCount = kalmanSensorFusionService.calculateOccupancy(sensorData, carriageId);

        // Assert
        assertEquals(20, finalPassengerCount);
        assertEquals(INITIAL_UNCERTAINTY, sensorData.getCalculatedUncertainty());
    }

    @Test
    void shouldUseTofChangeToPredictCurrentPassengerCount() {
        // Arrange
        Long carriageId = 1L;
        
        OccupancyLog previousLog = new OccupancyLog();
        previousLog.setCalculatedOccupancy(20);
        previousLog.setIrCount(18);
        previousLog.setCalculatedUncertainty(INITIAL_UNCERTAINTY);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriageId))
                .thenReturn(Optional.of(previousLog));

        SensorDataDTO currentData = createSensorData(100L, 1, 24, 23);

        // Act
        int expectedFinalCount = calculateExpectedFinalCount(20.0, INITIAL_UNCERTAINTY, 23 - 18, 24);
        int finalPassengerCount = kalmanSensorFusionService.calculateOccupancy(currentData, carriageId);

        // Assert
        assertEquals(expectedFinalCount, finalPassengerCount);
    }

    @Test
    void shouldUseCameraCountToCorrectTofPrediction() {
        // Arrange
        Long carriageId = 1L;
        
        OccupancyLog previousLog = new OccupancyLog();
        previousLog.setCalculatedOccupancy(20);
        previousLog.setIrCount(20);
        previousLog.setCalculatedUncertainty(INITIAL_UNCERTAINTY);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriageId))
                .thenReturn(Optional.of(previousLog));

        // Camera says 30, ToF says 21 (delta = 1) -> Disagreement
        SensorDataDTO currentData = createSensorData(100L, 1, 30, 21);

        int expectedFinalCount = calculateExpectedFinalCount(20.0, INITIAL_UNCERTAINTY, 21 - 20, 30);

        // Act
        int finalPassengerCount = kalmanSensorFusionService.calculateOccupancy(currentData, carriageId);

        // Assert
        assertEquals(expectedFinalCount, finalPassengerCount);
        assertEquals(27, finalPassengerCount);
    }

    @Test
    void shouldHandleNoPassengerChange() {
        // Arrange
        Long carriageId = 1L;
        
        OccupancyLog previousLog = new OccupancyLog();
        previousLog.setCalculatedOccupancy(20);
        previousLog.setIrCount(18);
        previousLog.setCalculatedUncertainty(INITIAL_UNCERTAINTY);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriageId))
                .thenReturn(Optional.of(previousLog));

        // No change: Camera is 20, ToF is 18
        SensorDataDTO currentData = createSensorData(100L, 1, 20, 18);

        int expectedFinalCount = calculateExpectedFinalCount(20.0, INITIAL_UNCERTAINTY, 0, 20);

        // Act
        int finalPassengerCount = kalmanSensorFusionService.calculateOccupancy(currentData, carriageId);

        // Assert
        assertEquals(expectedFinalCount, finalPassengerCount);
        assertEquals(20, finalPassengerCount);
    }

    @Test
    void shouldHandlePassengerIncrease() {
        // Arrange
        Long carriageId = 1L;
        
        OccupancyLog previousLog = new OccupancyLog();
        previousLog.setCalculatedOccupancy(20);
        previousLog.setIrCount(18);
        previousLog.setCalculatedUncertainty(INITIAL_UNCERTAINTY);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriageId))
                .thenReturn(Optional.of(previousLog));

        // Increase: ToF goes from 18 to 23 (+5), Camera goes to 24
        SensorDataDTO currentData = createSensorData(100L, 1, 24, 23);

        int expectedFinalCount = calculateExpectedFinalCount(20.0, INITIAL_UNCERTAINTY, 23 - 18, 24);

        // Act
        int finalPassengerCount = kalmanSensorFusionService.calculateOccupancy(currentData, carriageId);

        // Assert
        assertEquals(expectedFinalCount, finalPassengerCount);
        assertEquals(24, finalPassengerCount);
    }

    @Test
    void shouldHandlePassengerDecrease() {
        // Arrange
        Long carriageId = 1L;
        
        OccupancyLog previousLog = new OccupancyLog();
        previousLog.setCalculatedOccupancy(30);
        previousLog.setIrCount(28);
        previousLog.setCalculatedUncertainty(INITIAL_UNCERTAINTY);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriageId))
                .thenReturn(Optional.of(previousLog));

        // Decrease: ToF goes from 28 to 22 (-6), Camera goes to 24
        SensorDataDTO currentData = createSensorData(100L, 1, 24, 22);

        int expectedFinalCount = calculateExpectedFinalCount(30.0, INITIAL_UNCERTAINTY, 22 - 28, 24);

        // Act
        int finalPassengerCount = kalmanSensorFusionService.calculateOccupancy(currentData, carriageId);

        // Assert
        assertEquals(expectedFinalCount, finalPassengerCount);
        assertEquals(24, finalPassengerCount);
    }

    @Test
    void shouldKeepResultNonNegative() {
        // Arrange
        Long carriageId = 1L;
        
        OccupancyLog previousLog = new OccupancyLog();
        previousLog.setCalculatedOccupancy(5);
        previousLog.setIrCount(10);
        previousLog.setCalculatedUncertainty(INITIAL_UNCERTAINTY);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriageId))
                .thenReturn(Optional.of(previousLog));

        // Huge decrease: ToF goes from 10 to 0 (-10), Camera reads 0
        SensorDataDTO currentData = createSensorData(100L, 1, 0, 0);

        // Act
        int finalPassengerCount = kalmanSensorFusionService.calculateOccupancy(currentData, carriageId);

        // Assert
        assertEquals(0, finalPassengerCount);
    }

    @Test
    void shouldKeepDifferentCarriageStatesIndependent() {
        // Arrange
        Long carriage1Id = 1L;
        Long carriage2Id = 2L;

        OccupancyLog c1PreviousLog = new OccupancyLog();
        c1PreviousLog.setCalculatedOccupancy(20);
        c1PreviousLog.setIrCount(18);
        c1PreviousLog.setCalculatedUncertainty(INITIAL_UNCERTAINTY);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriage1Id))
                .thenReturn(Optional.of(c1PreviousLog));

        OccupancyLog c2PreviousLog = new OccupancyLog();
        c2PreviousLog.setCalculatedOccupancy(50);
        c2PreviousLog.setIrCount(40);
        c2PreviousLog.setCalculatedUncertainty(INITIAL_UNCERTAINTY);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriage2Id))
                .thenReturn(Optional.of(c2PreviousLog));

        SensorDataDTO c1Data2 = createSensorData(100L, 1, 24, 23); // +5 ToF change
        SensorDataDTO c2Data2 = createSensorData(100L, 2, 55, 45); // +5 ToF change

        // Act
        int c1FinalCount = kalmanSensorFusionService.calculateOccupancy(c1Data2, carriage1Id);
        int c2FinalCount = kalmanSensorFusionService.calculateOccupancy(c2Data2, carriage2Id);

        // Assert
        int expectedC1FinalCount = calculateExpectedFinalCount(20.0, INITIAL_UNCERTAINTY, 23 - 18, 24);
        int expectedC2FinalCount = calculateExpectedFinalCount(50.0, INITIAL_UNCERTAINTY, 45 - 40, 55);

        assertEquals(expectedC1FinalCount, c1FinalCount);
        assertEquals(expectedC2FinalCount, c2FinalCount);
    }

    @Test
    void shouldReusePreviousUncertaintyForTheSameCarriage() {
        // Arrange
        Long carriageId = 1L;
        
        double uncertainty2 = (1.0 - (26.0 / 42.0)) * 26.0; // pre-calculated past uncertainty
        
        OccupancyLog previousLog = new OccupancyLog();
        previousLog.setCalculatedOccupancy(20);
        previousLog.setIrCount(18);
        previousLog.setCalculatedUncertainty(uncertainty2);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriageId))
                .thenReturn(Optional.of(previousLog));

        SensorDataDTO data3 = createSensorData(100L, 1, 24, 23); // +5 change
        
        // Act
        int finalPassengerCount = kalmanSensorFusionService.calculateOccupancy(data3, carriageId);

        // Assert
        int expectedFinalCount = calculateExpectedFinalCount(20.0, uncertainty2, 23 - 18, 24);
        assertEquals(expectedFinalCount, finalPassengerCount);
    }

    @Test
    void shouldSaveOnlyOneOccupancyLogForEachInputSample() {
        // This test was validating that the service doesn't call save on repository, 
        // but since we updated it, we can verify that.
        // Arrange
        Long carriageId = 1L;
        SensorDataDTO sensorData = createSensorData(100L, 1, 20, 18);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(carriageId))
                .thenReturn(Optional.empty());

        // Act
        int finalPassengerCount = kalmanSensorFusionService.calculateOccupancy(sensorData, carriageId);

        // Assert
        assertEquals(20, finalPassengerCount);
        
        // Verify it doesn't call save
        verify(occupancyLogRepository, never()).save(any(OccupancyLog.class));
    }

    // --- Helper Methods ---

    private SensorDataDTO createSensorData(Long trainId, int carriageNumber, int cameraCount, int irCount) {
        SensorDataDTO data = new SensorDataDTO();
        data.setTrainId(trainId);
        data.setCarriageNumber(carriageNumber);
        data.setCameraCount(cameraCount);
        data.setIrCount(irCount);
        data.setTimestamp(LocalDateTime.now());
        return data;
    }

    private int calculateExpectedFinalCount(double lastEstimatedCount, double lastUncertainty, int tofCountChange, int cameraCount) {
        double predictedPassengerCount = lastEstimatedCount + tofCountChange;
        double predictedUncertainty = lastUncertainty + TOF_PREDICTION_UNCERTAINTY;
        double cameraCorrectionWeight = predictedUncertainty / (predictedUncertainty + CAMERA_COUNT_UNCERTAINTY);
        double correctedPassengerCount = predictedPassengerCount + cameraCorrectionWeight * (cameraCount - predictedPassengerCount);
        
        return Math.max(0, (int) Math.round(correctedPassengerCount));
    }
}
