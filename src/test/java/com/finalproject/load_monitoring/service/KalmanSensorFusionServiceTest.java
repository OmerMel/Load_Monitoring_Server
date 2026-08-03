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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KalmanSensorFusionServiceTest {

    @Mock
    private OccupancyLogRepository occupancyLogRepository;

    @InjectMocks
    private KalmanSensorFusionService kalmanService;

    private SensorDataDTO data;

    @BeforeEach
    void setUp() {
        data = new SensorDataDTO();
        data.setTrainId(1L);
        data.setCarriageNumber(1);
    }

    private void printResult(String testName, int cameraCount, int tofCount, int kalmanResult, int expectedResult, double newUncertainty) {
        System.out.printf("Test: %s | Camera: %d | ToF: %d | Kalman: %d | Expected: %d | Uncertainty: %.2f | Status: %s%n",
                testName, cameraCount, tofCount, kalmanResult, expectedResult, newUncertainty, (kalmanResult == expectedResult ? "PASS" : "FAIL"));
    }

    private OccupancyLog createLog(double lastEstimatedCount, Double lastUncertainty, int lastTofCount) {
        OccupancyLog log = new OccupancyLog();
        log.setCalculatedOccupancy((int) Math.round(lastEstimatedCount));
        log.setCalculatedUncertainty(lastUncertainty);
        log.setIrCount(lastTofCount);
        return log;
    }

    @Test
    void testFirstMeasurementNoPreviousUncertainty() {
        String testName = "First measurement (No previous uncertainty)";
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.empty());

        data.setCameraCount(10);
        data.setIrCount(15);

        int result = kalmanService.calculateOccupancy(data, 1L);

        assertEquals(10, result);
        assertEquals(1.0, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, 10, 15, result, 10, data.getCalculatedUncertainty());
        verify(occupancyLogRepository).findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L);
    }

    @Test
    void testEqualSensorValues() {
        String testName = "Camera and ToF returning the same value";
        OccupancyLog log = createLog(10.0, 1.0, 15);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));

        data.setCameraCount(20);
        data.setIrCount(25);

        int result = kalmanService.calculateOccupancy(data, 1L);

        int expected = 20;
        double expectedUncertainty = 416.0 / 42.0;

        assertEquals(expected, result);
        assertEquals(expectedUncertainty, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, 20, 25, result, expected, data.getCalculatedUncertainty());
    }

    @Test
    void testSmallDifferenceCameraAndToF() {
        String testName = "Small differences between the camera and ToF";
        OccupancyLog log = createLog(10.0, 1.0, 15);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));

        data.setCameraCount(22);
        data.setIrCount(25);

        int result = kalmanService.calculateOccupancy(data, 1L);

        int expected = 21;
        double expectedUncertainty = 416.0 / 42.0;

        assertEquals(expected, result);
        assertEquals(expectedUncertainty, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, 22, 25, result, expected, data.getCalculatedUncertainty());
    }

    @Test
    void testLargeDifferenceCameraAndToF() {
        String testName = "Large differences between the camera and ToF";
        OccupancyLog log = createLog(10.0, 1.0, 15);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));

        data.setCameraCount(40);
        data.setIrCount(25);

        int result = kalmanService.calculateOccupancy(data, 1L);

        int expected = 32;
        double expectedUncertainty = 416.0 / 42.0;

        assertEquals(expected, result);
        assertEquals(expectedUncertainty, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, 40, 25, result, expected, data.getCalculatedUncertainty());
    }

    @Test
    void testCameraHigherThanToF() {
        String testName = "Camera count higher than the ToF count";
        OccupancyLog log = createLog(15.0, 5.0, 20);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));

        data.setCameraCount(30);
        data.setIrCount(22);

        int result = kalmanService.calculateOccupancy(data, 1L);

        int expected = 25;
        double expectedUncertainty = 240.0 / 23.0;

        assertEquals(expected, result);
        assertEquals(expectedUncertainty, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, 30, 22, result, expected, data.getCalculatedUncertainty());
    }

    @Test
    void testToFHigherThanCamera() {
        String testName = "ToF count higher than the camera count";
        OccupancyLog log = createLog(15.0, 5.0, 20);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));

        data.setCameraCount(15);
        data.setIrCount(35);

        int result = kalmanService.calculateOccupancy(data, 1L);

        int expected = 20;
        double expectedUncertainty = 240.0 / 23.0;

        assertEquals(expected, result);
        assertEquals(expectedUncertainty, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, 15, 35, result, expected, data.getCalculatedUncertainty());
    }

    @Test
    void testBothSensorsZero() {
        String testName = "Both sensors returning zero";
        OccupancyLog log = createLog(10.0, 9.9047619, 10);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));

        data.setCameraCount(0);
        data.setIrCount(0);

        int result = kalmanService.calculateOccupancy(data, 1L);

        int expected = 0;
        double expectedUncertainty = (1 - (34.9047619 / 50.9047619)) * 34.9047619;

        assertEquals(expected, result);
        assertEquals(expectedUncertainty, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, 0, 0, result, expected, data.getCalculatedUncertainty());
    }

    @Test
    void testOneSensorZeroOtherValid() {
        String testName = "One sensor returning zero while the other returns a valid value";
        OccupancyLog log = createLog(10.0, 2.0, 10);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));

        data.setCameraCount(0);
        data.setIrCount(20);

        int result = kalmanService.calculateOccupancy(data, 1L);

        int expected = 7;
        double expectedUncertainty = (16.0 / 43.0) * 27.0;

        assertEquals(expected, result);
        assertEquals(expectedUncertainty, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, 0, 20, result, expected, data.getCalculatedUncertainty());
    }

    @Test
    void testVeryHighPassengerCounts() {
        String testName = "Very high passenger counts";
        OccupancyLog log = createLog(500.0, 10.0, 500);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));

        data.setCameraCount(600);
        data.setIrCount(600);

        int result = kalmanService.calculateOccupancy(data, 1L);

        int expected = 600;
        double expectedUncertainty = (16.0 / 51.0) * 35.0;

        assertEquals(expected, result);
        assertEquals(expectedUncertainty, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, 600, 600, result, expected, data.getCalculatedUncertainty());
    }

    @Test
    void testSuddenIncreasesInPassengerCount() {
        String testName = "Sudden increases in passenger count";
        OccupancyLog log = createLog(20.0, 8.0, 20);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));

        data.setCameraCount(100);
        data.setIrCount(120);

        int result = kalmanService.calculateOccupancy(data, 1L);

        int expected = 107;
        double expectedUncertainty = (16.0 / 49.0) * 33.0;

        assertEquals(expected, result);
        assertEquals(expectedUncertainty, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, 100, 120, result, expected, data.getCalculatedUncertainty());
    }

    @Test
    void testSuddenDecreasesInPassengerCount() {
        String testName = "Sudden decreases in passenger count";
        OccupancyLog log = createLog(100.0, 8.0, 120);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));

        data.setCameraCount(20);
        data.setIrCount(60);

        int result = kalmanService.calculateOccupancy(data, 1L);

        int expected = 27;
        double expectedUncertainty = (16.0 / 49.0) * 33.0;

        assertEquals(expected, result);
        assertEquals(expectedUncertainty, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, 20, 60, result, expected, data.getCalculatedUncertainty());
    }

    @Test
    void testInvalidOrNegativeSensorValues() {
        String testName = "Invalid or negative sensor values";
        OccupancyLog log = createLog(5.0, 5.0, 10);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));

        data.setCameraCount(-5);
        data.setIrCount(0);

        int result = kalmanService.calculateOccupancy(data, 1L);

        int expected = 0;
        double expectedUncertainty = (8.0 / 23.0) * 30.0;

        assertEquals(expected, result);
        assertEquals(expectedUncertainty, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, -5, 0, result, expected, data.getCalculatedUncertainty());
    }

    @Test
    void testPreviousUncertaintyExistsInDatabase() {
        String testName = "Measurements when a previous uncertainty value already exists in the database (null fallback)";
        OccupancyLog log = createLog(15.0, null, 20); // Database returns null for uncertainty
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));

        data.setCameraCount(20);
        data.setIrCount(25);

        int result = kalmanService.calculateOccupancy(data, 1L);

        int expected = 20;
        double expectedUncertainty = 416.0 / 42.0;

        assertEquals(expected, result);
        assertEquals(expectedUncertainty, data.getCalculatedUncertainty(), 0.001);

        printResult(testName, 20, 25, result, expected, data.getCalculatedUncertainty());
    }

    @Test
    void testMeasurementsForDifferentCarriages() {
        String testName1 = "Measurements for different carriages (Carriage 1)";
        String testName2 = "Measurements for different carriages (Carriage 2)";

        OccupancyLog log1 = createLog(10.0, 1.0, 15);
        OccupancyLog log2 = createLog(30.0, 2.0, 40);

        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log1));
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(2L)).thenReturn(Optional.of(log2));

        // Carriage 1 test
        SensorDataDTO data1 = new SensorDataDTO();
        data1.setTrainId(1L);
        data1.setCarriageNumber(1);
        data1.setCameraCount(20);
        data1.setIrCount(25);

        int result1 = kalmanService.calculateOccupancy(data1, 1L);
        int expected1 = 20;
        double expectedUncertainty1 = 416.0 / 42.0;

        assertEquals(expected1, result1);
        assertEquals(expectedUncertainty1, data1.getCalculatedUncertainty(), 0.001);
        printResult(testName1, 20, 25, result1, expected1, data1.getCalculatedUncertainty());

        // Carriage 2 test
        SensorDataDTO data2 = new SensorDataDTO();
        data2.setTrainId(1L);
        data2.setCarriageNumber(2);
        data2.setCameraCount(45);
        data2.setIrCount(50);

        int result2 = kalmanService.calculateOccupancy(data2, 2L);
        int expected2 = 43;
        double expectedUncertainty2 = (16.0 / 43.0) * 27.0;

        assertEquals(expected2, result2);
        assertEquals(expectedUncertainty2, data2.getCalculatedUncertainty(), 0.001);
        printResult(testName2, 45, 50, result2, expected2, data2.getCalculatedUncertainty());
        
        verify(occupancyLogRepository).findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L);
        verify(occupancyLogRepository).findFirstByCarriage_CarriageIdOrderByTimestampDesc(2L);
    }
    
    @Test
    void testRepeatedMeasurementsForSameCarriage() {
        String testName = "Repeated measurements for the same carriage";
        OccupancyLog log1 = createLog(10.0, 1.0, 15);
        
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log1));

        // Measurement 1
        data.setCameraCount(20);
        data.setIrCount(25);
        int result1 = kalmanService.calculateOccupancy(data, 1L);
        
        int expected1 = 20;
        double expectedUncertainty1 = 416.0 / 42.0;
        
        assertEquals(expected1, result1);
        assertEquals(expectedUncertainty1, data.getCalculatedUncertainty(), 0.001);
        printResult(testName + " (Meas 1)", 20, 25, result1, expected1, data.getCalculatedUncertainty());

        // Now mock for Measurement 2
        OccupancyLog log2 = createLog((double) result1, data.getCalculatedUncertainty(), 25);
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log2));

        // Measurement 2
        data.setCameraCount(22);
        data.setIrCount(30);
        
        int result2 = kalmanService.calculateOccupancy(data, 1L);
        
        int expected2 = 23;
        double expectedUncertainty2 = (1 - (34.9047619 / 50.9047619)) * 34.9047619;
        
        assertEquals(expected2, result2);
        assertEquals(expectedUncertainty2, data.getCalculatedUncertainty(), 0.001);
        printResult(testName + " (Meas 2)", 22, 30, result2, expected2, data.getCalculatedUncertainty());
        
        verify(occupancyLogRepository, times(2)).findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L);
    }
}
