package com.finalproject.load_monitoring.service;

import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.entity.OccupancyLog;
import com.finalproject.load_monitoring.repository.OccupancyLogRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KalmanSensorFusionRealisticTest {

    @Mock
    private OccupancyLogRepository occupancyLogRepository;

    @InjectMocks
    private KalmanSensorFusionService kalmanService;

    private static List<String> independentTestLogs = new ArrayList<>();
    private static List<String> sequentialTestLogs = new ArrayList<>();
    private static List<String> functionalConcernsLogs = new ArrayList<>();

    private static int totalMeasurements = 0;
    private static int passedAssertions = 0;
    private static int failedAssertions = 0;
    private static int sumCameraError = 0;
    private static int sumTofError = 0;
    private static int sumKalmanError = 0;
    private static int maxCameraError = 0;
    private static int maxTofError = 0;
    private static int maxKalmanError = 0;
    private static int kalmanCloserThanCamera = 0;
    private static int kalmanCloserThanTof = 0;
    private static int kalmanEqualOrCloserThanBoth = 0;
    private static int worseThanBothRawSensors = 0;

    record KalmanTestCase(
            String testName,
            int actualOccupancy,
            int cameraCount,
            int tofCount,
            LocalDateTime timestamp
    ) {}

    record ScenarioSet(String setName, List<KalmanTestCase> scenarios) {}

    record KalmanResult(int occupancy, double uncertainty) {}

    private static List<ScenarioSet> getAllScenarioSets() {
        return List.of(
                new ScenarioSet("Original Set", List.of(
                        new KalmanTestCase("Original 1", 1, 0, 1, LocalDateTime.now()),
                        new KalmanTestCase("Original 2", 6, 6, 5, LocalDateTime.now()),
                        new KalmanTestCase("Original 3", 11, 11, 7, LocalDateTime.now()),
                        new KalmanTestCase("Original 4", 15, 14, 10, LocalDateTime.now()),
                        new KalmanTestCase("Original 5", 17, 18, 11, LocalDateTime.now()),
                        new KalmanTestCase("Original 6", 22, 21, 15, LocalDateTime.now()),
                        new KalmanTestCase("Original 7", 24, 21, 17, LocalDateTime.now())
                )),
                new ScenarioSet("SET A: Camera accurate, ToF drifts low", List.of(
                        new KalmanTestCase("A-1", 30, 30, 22, LocalDateTime.now()),
                        new KalmanTestCase("A-2", 38, 39, 26, LocalDateTime.now()),
                        new KalmanTestCase("A-3", 45, 44, 31, LocalDateTime.now()),
                        new KalmanTestCase("A-4", 52, 53, 35, LocalDateTime.now()),
                        new KalmanTestCase("A-5", 60, 59, 40, LocalDateTime.now())
                )),
                new ScenarioSet("SET B: ToF accurate, Camera drifts high", List.of(
                        new KalmanTestCase("B-1", 25, 33, 26, LocalDateTime.now()),
                        new KalmanTestCase("B-2", 31, 40, 30, LocalDateTime.now()),
                        new KalmanTestCase("B-3", 40, 51, 41, LocalDateTime.now()),
                        new KalmanTestCase("B-4", 48, 60, 47, LocalDateTime.now()),
                        new KalmanTestCase("B-5", 55, 70, 56, LocalDateTime.now())
                )),
                new ScenarioSet("SET C: Both sensors agree closely", List.of(
                        new KalmanTestCase("C-1", 70, 70, 69, LocalDateTime.now()),
                        new KalmanTestCase("C-2", 80, 81, 79, LocalDateTime.now()),
                        new KalmanTestCase("C-3", 90, 90, 91, LocalDateTime.now())
                )),
                new ScenarioSet("SET D: Both sensors disagree significantly", List.of(
                        new KalmanTestCase("D-1", 65, 50, 82, LocalDateTime.now()),
                        new KalmanTestCase("D-2", 72, 90, 55, LocalDateTime.now()),
                        new KalmanTestCase("D-3", 100, 78, 120, LocalDateTime.now())
                )),
                new ScenarioSet("SET E: Occupancy decreasing", List.of(
                        new KalmanTestCase("E-1", 60, 59, 45, LocalDateTime.now()),
                        new KalmanTestCase("E-2", 50, 48, 38, LocalDateTime.now()),
                        new KalmanTestCase("E-3", 35, 34, 27, LocalDateTime.now()),
                        new KalmanTestCase("E-4", 20, 19, 15, LocalDateTime.now()),
                        new KalmanTestCase("E-5", 8, 7, 6, LocalDateTime.now())
                )),
                new ScenarioSet("SET F: Sudden jump", List.of(
                        new KalmanTestCase("F-1", 15, 14, 11, LocalDateTime.now()),
                        new KalmanTestCase("F-2", 90, 85, 60, LocalDateTime.now())
                )),
                new ScenarioSet("SET G: Edge cases / extreme values", List.of(
                        new KalmanTestCase("G-1", 0, 0, 0, LocalDateTime.now()),
                        new KalmanTestCase("G-2", 1, 0, 0, LocalDateTime.now()),
                        new KalmanTestCase("G-3", 150, 148, 105, LocalDateTime.now()),
                        new KalmanTestCase("G-4", 120, 0, 118, LocalDateTime.now()),
                        new KalmanTestCase("G-5", 110, 112, 0, LocalDateTime.now())
                ))
        );
    }

    private static List<KalmanTestCase> getIndependentScenarios() {
        return getAllScenarioSets().stream()
                .flatMap(s -> s.scenarios().stream())
                .toList();
    }

    private KalmanResult calculateExpected(Integer lastEstimatedCount, Double lastUncertainty, Integer lastTofCount, int cameraCount, int currentTofCount) {
        if (lastEstimatedCount == null) {
            return new KalmanResult(Math.max(0, cameraCount), 1.0);
        }
        double prevUncertainty = lastUncertainty != null ? lastUncertainty : 1.0;
        int deltaTof = currentTofCount - lastTofCount;
        double predictedOcc = lastEstimatedCount + deltaTof;
        double predictedUnc = prevUncertainty + 25.0;

        double cameraWeight = predictedUnc / (predictedUnc + 16.0);
        double correctedOcc = predictedOcc + cameraWeight * (cameraCount - predictedOcc);
        double correctedUnc = (1.0 - cameraWeight) * predictedUnc;

        return new KalmanResult(Math.max(0, (int) Math.round(correctedOcc)), correctedUnc);
    }

    private void updateStats(boolean passed, int actual, int camera, int tof, int kalman, String contextLog) {
        totalMeasurements++;
        if (passed) passedAssertions++;
        else failedAssertions++;

        int errCam = Math.abs(camera - actual);
        int errTof = Math.abs(tof - actual);
        int errKal = Math.abs(kalman - actual);

        sumCameraError += errCam;
        sumTofError += errTof;
        sumKalmanError += errKal;

        if (errCam > maxCameraError) maxCameraError = errCam;
        if (errTof > maxTofError) maxTofError = errTof;
        if (errKal > maxKalmanError) maxKalmanError = errKal;

        if (errKal < errCam) kalmanCloserThanCamera++;
        if (errKal < errTof) kalmanCloserThanTof++;
        if (errKal <= errCam && errKal <= errTof) kalmanEqualOrCloserThanBoth++;

        if (errKal > Math.max(errCam, errTof)) {
            worseThanBothRawSensors++;
            functionalConcernsLogs.add(contextLog);
        }
    }

    @BeforeAll
    static void setup() {
        independentTestLogs.clear();
        sequentialTestLogs.clear();
        functionalConcernsLogs.clear();
        totalMeasurements = 0;
        passedAssertions = 0;
        failedAssertions = 0;
        sumCameraError = 0;
        sumTofError = 0;
        sumKalmanError = 0;
        maxCameraError = 0;
        maxTofError = 0;
        maxKalmanError = 0;
        kalmanCloserThanCamera = 0;
        kalmanCloserThanTof = 0;
        kalmanEqualOrCloserThanBoth = 0;
        worseThanBothRawSensors = 0;
    }

    @ParameterizedTest
    @MethodSource("getIndependentScenarios")
    @Order(1)
    void independentMeasurementTests(KalmanTestCase scenario) {
        // Mock empty DB for a clean initial state
        when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.empty());

        SensorDataDTO dto = new SensorDataDTO();
        dto.setTrainId(1L);
        dto.setCarriageNumber(1);
        dto.setCameraCount(scenario.cameraCount());
        dto.setIrCount(scenario.tofCount());

        int result = kalmanService.calculateOccupancy(dto, 1L);

        KalmanResult expected = calculateExpected(null, null, null, scenario.cameraCount(), scenario.tofCount());
        boolean passed = (result == expected.occupancy()) && Math.abs(dto.getCalculatedUncertainty() - expected.uncertainty()) < 0.0001;

        int errCam = Math.abs(scenario.cameraCount() - scenario.actualOccupancy());
        int errTof = Math.abs(scenario.tofCount() - scenario.actualOccupancy());
        int errKal = Math.abs(result - scenario.actualOccupancy());
        boolean isWorse = errKal > Math.max(errCam, errTof);
        String flag = isWorse ? " [FLAG: Kalman worse than both raw sensors]" : "";

        String baseLogContext = String.format(Locale.US, "Independent %s | Actual: %d | Cam: %d | ToF: %d | Kal: %d",
                scenario.testName(), scenario.actualOccupancy(), scenario.cameraCount(), scenario.tofCount(), result);

        updateStats(passed, scenario.actualOccupancy(), scenario.cameraCount(), scenario.tofCount(), result, baseLogContext);

        String logLine = String.format(Locale.US, "Test: Actual occupancy %d (%s) | Actual: %d | Camera: %d | ToF: %d | Kalman: %d | Error: %d | Previous Uncertainty: N/A | New Uncertainty: %.5f | Functional Status: %s%s",
                scenario.actualOccupancy(), scenario.testName(), scenario.actualOccupancy(), scenario.cameraCount(), scenario.tofCount(), result, errKal, dto.getCalculatedUncertainty(), passed ? "PASS" : "FAIL", flag);

        independentTestLogs.add(logLine);
        System.out.println(logLine);

        assertEquals(expected.occupancy(), result);
        assertEquals(expected.uncertainty(), dto.getCalculatedUncertainty(), 0.0001);
        verify(occupancyLogRepository, atLeastOnce()).findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L);
        
        clearInvocations(occupancyLogRepository);
    }

    @Test
    @Order(2)
    void sequentialOccupancyIncreaseTest() {
        for (ScenarioSet set : getAllScenarioSets()) {
            Integer lastOcc = null;
            Double lastUnc = null;
            Integer lastTof = null;

            sequentialTestLogs.add("");
            sequentialTestLogs.add("--- " + set.setName() + " ---");
            System.out.println("\n--- " + set.setName() + " ---");

            int measurementNum = 1;
            int totalInSet = set.scenarios().size();

            for (KalmanTestCase scenario : set.scenarios()) {
                if (lastOcc == null) {
                    when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.empty());
                } else {
                    OccupancyLog log = new OccupancyLog();
                    log.setCalculatedOccupancy(lastOcc);
                    log.setCalculatedUncertainty(lastUnc);
                    log.setIrCount(lastTof);
                    when(occupancyLogRepository.findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L)).thenReturn(Optional.of(log));
                }

                SensorDataDTO dto = new SensorDataDTO();
                dto.setTrainId(1L);
                dto.setCarriageNumber(1);
                dto.setCameraCount(scenario.cameraCount());
                dto.setIrCount(scenario.tofCount());

                int result = kalmanService.calculateOccupancy(dto, 1L);

                KalmanResult expected = calculateExpected(lastOcc, lastUnc, lastTof, scenario.cameraCount(), scenario.tofCount());
                boolean passed = (result == expected.occupancy()) && Math.abs(dto.getCalculatedUncertainty() - expected.uncertainty()) < 0.0001;

                int errCam = Math.abs(scenario.cameraCount() - scenario.actualOccupancy());
                int errTof = Math.abs(scenario.tofCount() - scenario.actualOccupancy());
                int errKal = Math.abs(result - scenario.actualOccupancy());
                boolean isWorse = errKal > Math.max(errCam, errTof);
                String flag = isWorse ? " [FLAG: Kalman worse than both raw sensors]" : "";

                String baseLogContext = String.format(Locale.US, "Sequential %s (%s) | Actual: %d | Cam: %d | ToF: %d | Kal: %d",
                        set.setName(), scenario.testName(), scenario.actualOccupancy(), scenario.cameraCount(), scenario.tofCount(), result);

                updateStats(passed, scenario.actualOccupancy(), scenario.cameraCount(), scenario.tofCount(), result, baseLogContext);

                String prevUncStr = lastUnc == null ? "N/A" : String.format(Locale.US, "%.5f", lastUnc);
                String logLine = String.format(Locale.US, "Test: Sequential occupancy increase | Measurement: %d/%d | Actual: %d | Camera: %d | ToF: %d | Kalman: %d | Error: %d | Previous Uncertainty: %s | New Uncertainty: %.5f | Functional Status: %s%s",
                        measurementNum, totalInSet, scenario.actualOccupancy(), scenario.cameraCount(), scenario.tofCount(), result, errKal, prevUncStr, dto.getCalculatedUncertainty(), passed ? "PASS" : "FAIL", flag);

                sequentialTestLogs.add(logLine);
                System.out.println(logLine);

                assertEquals(expected.occupancy(), result);
                assertEquals(expected.uncertainty(), dto.getCalculatedUncertainty(), 0.0001);
                verify(occupancyLogRepository, atLeastOnce()).findFirstByCarriage_CarriageIdOrderByTimestampDesc(1L);

                lastOcc = result;
                lastUnc = dto.getCalculatedUncertainty();
                lastTof = scenario.tofCount();
                measurementNum++;
                
                clearInvocations(occupancyLogRepository);
            }
        }
    }

    @AfterAll
    static void generateReport() {
        File file = new File("target/test-results/kalman-test-results.txt");
        file.getParentFile().mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("============================================================");
            writer.println("KALMAN SENSOR FUSION TEST RESULTS");
            writer.println("Executed at: " + LocalDateTime.now());
            writer.println("Service under test: KalmanSensorFusionService");
            writer.println("============================================================");
            writer.println();
            writer.println("[COLLECTED SENSOR DATA]");
            for (ScenarioSet set : getAllScenarioSets()) {
                writer.println();
                writer.println("--- " + set.setName() + " ---");
                for (KalmanTestCase sc : set.scenarios()) {
                    writer.printf("Scenario %s | Actual: %d | Camera: %d | ToF: %d\n", sc.testName(), sc.actualOccupancy(), sc.cameraCount(), sc.tofCount());
                }
            }
            writer.println();
            writer.println("============================================================");
            writer.println("[INDEPENDENT MEASUREMENT TESTS]");
            writer.println("============================================================");
            writer.println();
            for (String log : independentTestLogs) {
                writer.println(log);
            }
            writer.println();
            writer.println("============================================================");
            writer.println("[SEQUENTIAL OCCUPANCY INCREASE]");
            writer.println("============================================================");
            for (String log : sequentialTestLogs) {
                writer.println(log);
            }
            writer.println();
            writer.println("============================================================");
            writer.println("SUMMARY");
            writer.println("============================================================");
            writer.println();
            writer.println("Total measurements: " + totalMeasurements);
            writer.println("Passed assertions: " + passedAssertions);
            writer.println("Failed assertions: " + failedAssertions);
            writer.println();
            writer.printf(Locale.US, "Average camera absolute error: %.2f\n", (double) sumCameraError / totalMeasurements);
            writer.printf(Locale.US, "Average ToF absolute error: %.2f\n", (double) sumTofError / totalMeasurements);
            writer.printf(Locale.US, "Average Kalman absolute error: %.2f\n", (double) sumKalmanError / totalMeasurements);
            writer.println();
            writer.println("Maximum camera error: " + maxCameraError);
            writer.println("Maximum ToF error: " + maxTofError);
            writer.println("Maximum Kalman error: " + maxKalmanError);
            writer.println();
            writer.printf(Locale.US, "Kalman closer than camera: %d (%.1f%%)\n", kalmanCloserThanCamera, (kalmanCloserThanCamera * 100.0) / totalMeasurements);
            writer.printf(Locale.US, "Kalman closer than ToF: %d (%.1f%%)\n", kalmanCloserThanTof, (kalmanCloserThanTof * 100.0) / totalMeasurements);
            writer.printf(Locale.US, "Kalman equal to or closer than both sensors: %d (%.1f%%)\n", kalmanEqualOrCloserThanBoth, (kalmanEqualOrCloserThanBoth * 100.0) / totalMeasurements);
            writer.println();
            
            writer.printf(Locale.US, "Functional Concerns (Kalman worse than raw sensors): %d (%.1f%%)\n", worseThanBothRawSensors, (worseThanBothRawSensors * 100.0) / totalMeasurements);
            if (worseThanBothRawSensors > 0) {
                writer.println("Details:");
                for (String concern : functionalConcernsLogs) {
                    writer.println("  - " + concern);
                }
            }

            writer.println();
            writer.println("============================================================");
        } catch (IOException e) {
            System.err.println("Failed to write report to target/test-results/kalman-test-results.txt");
            e.printStackTrace();
        }
    }
}