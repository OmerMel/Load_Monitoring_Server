package com.finalproject.load_monitoring.service;

import com.finalproject.load_monitoring.converter.OccupancyLogConverter;
import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.entity.OccupancyLog;
import com.finalproject.load_monitoring.exception.ResourceNotFoundException;
import com.finalproject.load_monitoring.repository.CarriageRepository;
import com.finalproject.load_monitoring.repository.OccupancyLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Covers the fallback logic added to OccupancyService.updateOccupancy():
 * when the camera and/or the IR sensor report themselves as "unavailable",
 * the Kalman fusion is skipped entirely and the occupancy is derived
 * directly from whichever source is still working (or left unchanged if
 * both are down).
 */
@ExtendWith(MockitoExtension.class)
class OccupancyServiceTest {

    @Mock
    private CarriageRepository carriageRepository;
    @Mock
    private OccupancyLogRepository occupancyLogRepository;
    @Mock
    private OccupancyLogConverter occupancyLogConverter;
    @Mock
    private KalmanSensorFusionService sensorFusionService;

    @InjectMocks
    private OccupancyService occupancyService;

    private SensorDataDTO data;
    private Carriage carriage;

    @BeforeEach
    void setUp() {
        data = new SensorDataDTO();
        data.setTrainId(1L);
        data.setCarriageNumber(1);

        carriage = new Carriage();
        carriage.setCarriageId(100L);
        carriage.setOccupancy(12); // last known occupancy, before this update

        when(carriageRepository.findByTrainTrainIdAndCarriageNumber(1L, 1))
                .thenReturn(Optional.of(carriage));

        // toEntity() is only used to build the object that gets persisted;
        // the exact mapping is already covered elsewhere, so a lenient stub is enough here.
        lenient().when(occupancyLogConverter.toEntity(any(), any())).thenReturn(new OccupancyLog());
    }

    @Test
    void bothSensorsOk_usesKalmanFusion() {
        data.setCameraStatus("ok");
        data.setIrStatus("ok");
        data.setCameraCount(20);
        data.setIrCount(25);

        when(sensorFusionService.calculateOccupancy(data, 100L)).thenReturn(22);

        occupancyService.updateOccupancy(data);

        assertEquals(22, data.getCalculatedOccupancy());
        assertEquals(22, carriage.getOccupancy());
        verify(sensorFusionService).calculateOccupancy(data, 100L);
        verify(carriageRepository).save(carriage);
        verify(occupancyLogRepository).save(any(OccupancyLog.class));
    }

    @Test
    void nullStatusFields_treatedAsOk_backwardCompatible() {
        // Simulates an older edge device / a direct REST call that doesn't send status at all
        data.setCameraStatus(null);
        data.setIrStatus(null);
        data.setCameraCount(20);
        data.setIrCount(25);

        when(sensorFusionService.calculateOccupancy(data, 100L)).thenReturn(22);

        occupancyService.updateOccupancy(data);

        assertEquals(22, data.getCalculatedOccupancy());
        verify(sensorFusionService).calculateOccupancy(data, 100L);
    }

    @Test
    void irUnavailable_usesCameraCountDirectly_skipsFusion() {
        data.setCameraStatus("ok");
        data.setIrStatus("unavailable");
        data.setCameraCount(18);
        data.setIrCount(0); // stale/zero value from the edge - must NOT be trusted

        occupancyService.updateOccupancy(data);

        assertEquals(18, data.getCalculatedOccupancy());
        assertEquals(18, carriage.getOccupancy());
        assertNull(data.getCalculatedUncertainty(), "uncertainty must be reset so Kalman re-initializes later");
        verify(sensorFusionService, never()).calculateOccupancy(any(), any());
        verify(carriageRepository).save(carriage);
    }

    @Test
    void cameraUnavailable_usesIrCountDirectly_skipsFusion() {
        data.setCameraStatus("unavailable");
        data.setIrStatus("ok");
        data.setCameraCount(0); // stale/zero value from the edge - must NOT be trusted
        data.setIrCount(9);

        occupancyService.updateOccupancy(data);

        assertEquals(9, data.getCalculatedOccupancy());
        assertEquals(9, carriage.getOccupancy());
        assertNull(data.getCalculatedUncertainty());
        verify(sensorFusionService, never()).calculateOccupancy(any(), any());
    }

    @Test
    void bothUnavailable_keepsLastKnownOccupancy() {
        data.setCameraStatus("unavailable");
        data.setIrStatus("unavailable");
        data.setCameraCount(0);
        data.setIrCount(0);
        // carriage.getOccupancy() == 12 from setUp()

        occupancyService.updateOccupancy(data);

        assertEquals(12, data.getCalculatedOccupancy());
        assertEquals(12, carriage.getOccupancy());
        assertNull(data.getCalculatedUncertainty());
        verify(sensorFusionService, never()).calculateOccupancy(any(), any());
        // A log entry is still written, so the outage itself is recorded in history
        verify(occupancyLogRepository).save(any(OccupancyLog.class));
    }

    @Test
    void irUnavailable_negativeCameraCount_neverGoesBelowZero() {
        // Defensive check: even a corrupt/negative reading from the working
        // source must not push the reported occupancy below zero.
        data.setCameraStatus("ok");
        data.setIrStatus("unavailable");
        data.setCameraCount(-3);

        occupancyService.updateOccupancy(data);

        assertEquals(0, data.getCalculatedOccupancy());
    }

    @Test
    void unknownCarriage_throwsResourceNotFoundException() {
        when(carriageRepository.findByTrainTrainIdAndCarriageNumber(1L, 1))
                .thenReturn(Optional.empty());

        data.setCameraStatus("ok");
        data.setIrStatus("ok");

        assertThrows(ResourceNotFoundException.class, () -> occupancyService.updateOccupancy(data));
        verifyNoInteractions(sensorFusionService);
        verify(occupancyLogRepository, never()).save(any());
    }
}