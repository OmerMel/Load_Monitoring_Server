package com.finalproject.load_monitoring.converter;

import com.finalproject.load_monitoring.dto.OccupancyLogDTO;
import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.entity.OccupancyLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers OccupancyLogConverter's mapping between SensorDataDTO / OccupancyLog / OccupancyLogDTO.
 *
 * Deliberately uses DIFFERENT values for cameraCount vs irCount and for
 * cameraStatus vs irStatus in every test (rather than symmetric values).
 * Fields of the same type (int/int, String/String) are exactly the kind of
 * thing that's easy to swap by accident in a Lombok @AllArgsConstructor call
 * without the compiler ever noticing - using distinct values means such a
 * swap would fail the test instead of passing silently.
 */
class OccupancyLogConverterTest {

    private OccupancyLogConverter converter;

    @BeforeEach
    void setUp() {
        converter = new OccupancyLogConverter();
    }

    // ----------------------------- toEntity -----------------------------

    @Test
    void toEntity_mapsAllFieldsCorrectly_noFieldsSwapped() {
        Carriage carriage = new Carriage();
        carriage.setCarriageId(100L);

        SensorDataDTO data = new SensorDataDTO();
        data.setCameraCount(11);
        data.setIrCount(22);
        data.setCalculatedOccupancy(15);
        data.setCalculatedUncertainty(3.5);
        data.setCameraStatus("ok");
        data.setIrStatus("unavailable");

        OccupancyLog log = converter.toEntity(data, carriage);

        assertNotNull(log);
        assertSame(carriage, log.getCarriage());
        assertEquals(11, log.getCameraCount());
        assertEquals(22, log.getIrCount());
        assertEquals(15, log.getCalculatedOccupancy());
        assertEquals(3.5, log.getCalculatedUncertainty());
        assertEquals("ok", log.getCameraStatus());
        assertEquals("unavailable", log.getIrStatus());
        assertNotNull(log.getTimestamp());
    }

    @Test
    void toEntity_nullCalculatedUncertainty_isPreservedAsNull() {
        // This is exactly what OccupancyService writes when it bypasses Kalman
        // fusion (camera-only / IR-only / both-down) - it must reach the DB as
        // NULL, not silently become 0.0 or some other default.
        Carriage carriage = new Carriage();
        SensorDataDTO data = new SensorDataDTO();
        data.setCameraStatus("ok");
        data.setIrStatus("unavailable");
        data.setCalculatedUncertainty(null);

        OccupancyLog log = converter.toEntity(data, carriage);

        assertNull(log.getCalculatedUncertainty());
    }

    @Test
    void toEntity_nullData_returnsNull() {
        assertNull(converter.toEntity(null, new Carriage()));
    }

    @Test
    void toEntity_nullStatusFields_arePreservedAsNull() {
        // Backward compatibility: an older edge device / direct REST call
        // that never sends status fields at all.
        Carriage carriage = new Carriage();
        SensorDataDTO data = new SensorDataDTO();
        data.setCameraStatus(null);
        data.setIrStatus(null);

        OccupancyLog log = converter.toEntity(data, carriage);

        assertNull(log.getCameraStatus());
        assertNull(log.getIrStatus());
    }

    // ------------------------------ toDTO --------------------------------

    @Test
    void toDTO_mapsAllFieldsCorrectly_noFieldsSwapped() {
        Carriage carriage = new Carriage();
        carriage.setCarriageId(200L);

        OccupancyLog log = new OccupancyLog();
        log.setLogId(1L);
        log.setCarriage(carriage);
        log.setCameraCount(33);
        log.setIrCount(44);
        log.setCalculatedOccupancy(38);
        log.setCalculatedUncertainty(7.25);
        log.setCameraStatus("unavailable");
        log.setIrStatus("ok");
        LocalDateTime ts = LocalDateTime.of(2026, 1, 1, 12, 0);
        log.setTimestamp(ts);

        OccupancyLogDTO dto = converter.toDTO(log);

        assertNotNull(dto);
        assertEquals(1L, dto.getLogId());
        assertEquals(200L, dto.getCarriageId());
        assertEquals(33, dto.getCameraCount());
        assertEquals(44, dto.getIrCount());
        assertEquals(38, dto.getCalculatedOccupancy());
        assertEquals(7.25, dto.getCalculatedUncertainty());
        assertEquals("unavailable", dto.getCameraStatus());
        assertEquals("ok", dto.getIrStatus());
        assertEquals(ts, dto.getTimestamp());
    }

    @Test
    void toDTO_nullCarriage_setsNullCarriageId() {
        OccupancyLog log = new OccupancyLog();
        log.setCarriage(null);

        OccupancyLogDTO dto = converter.toDTO(log);

        assertNull(dto.getCarriageId());
    }

    @Test
    void toDTO_nullEntityLog_returnsNull() {
        assertNull(converter.toDTO(null));
    }

    // --------------------------- round trip -------------------------------

    @Test
    void roundTrip_toEntityThenToDTO_preservesStatusFields() {
        Carriage carriage = new Carriage();
        carriage.setCarriageId(300L);

        SensorDataDTO data = new SensorDataDTO();
        data.setCameraCount(5);
        data.setIrCount(9);
        data.setCalculatedOccupancy(6);
        data.setCalculatedUncertainty(1.1);
        data.setCameraStatus("ok");
        data.setIrStatus("unavailable");

        OccupancyLog log = converter.toEntity(data, carriage);
        log.setLogId(42L); // simulate persistence assigning an ID

        OccupancyLogDTO dto = converter.toDTO(log);

        assertEquals(42L, dto.getLogId());
        assertEquals(300L, dto.getCarriageId());
        assertEquals("ok", dto.getCameraStatus());
        assertEquals("unavailable", dto.getIrStatus());
    }
}