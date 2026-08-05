package com.finalproject.load_monitoring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.entity.Station;
import com.finalproject.load_monitoring.entity.Train;
import com.finalproject.load_monitoring.repository.CarriageRepository;
import com.finalproject.load_monitoring.repository.OccupancyLogRepository;
import com.finalproject.load_monitoring.repository.StationRepository;
import com.finalproject.load_monitoring.repository.TrainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Simulates real HTTP requests hitting POST /api/sensors/update and reading
 * back GET /api/sensors/{carriageId}, exercising the full pipeline:
 * JSON -> SensorDataDTO (Jackson) -> OccupancyService -> H2 database -> JSON.
 *
 * This is the end-to-end counterpart to OccupancyServiceTest and
 * OccupancyLogConverterTest, which test those layers in isolation with mocks.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:statustestdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SensorControllerStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private CarriageRepository carriageRepository;

    @Autowired
    private OccupancyLogRepository occupancyLogRepository;

    // Mock the MQTT adapter so it doesn't try to connect to a real broker during testing
    @MockitoBean(name = "inbound")
    private org.springframework.integration.core.MessageProducer inbound;

    private Long testTrainId;
    private Long testCarriageId;

    @BeforeEach
    void setUp() {
        // Child rows first - carriages can't be deleted while occupancy_logs
        // still has a foreign key pointing at them (left over from the
        // previous test method in this same class).
        occupancyLogRepository.deleteAll();
        carriageRepository.deleteAll();
        trainRepository.deleteAll();
        stationRepository.deleteAll();

        Station origin = new Station();
        origin.setStationName("Origin Station");
        origin = stationRepository.save(origin);

        Station dest = new Station();
        dest.setStationName("Dest Station");
        dest = stationRepository.save(dest);

        Train train = new Train();
        train.setOriginStation(origin);
        train.setDestinationStation(dest);
        train.setDepartureTime(LocalDateTime.now().minusHours(1));
        train.setArrivalTime(LocalDateTime.now().plusHours(1));
        train.setLastUpdated(LocalDateTime.now());
        train = trainRepository.save(train);
        testTrainId = train.getTrainId();

        Carriage carriage = new Carriage();
        carriage.setTrain(train);
        carriage.setCarriageNumber(1);
        carriage.setOccupancy(0);
        carriage.setMaxCapacity(100);
        carriage.setLastUpdated(LocalDateTime.now());
        carriage = carriageRepository.save(carriage);
        testCarriageId = carriage.getCarriageId();
    }

    private SensorDataDTO baseSensorData() {
        SensorDataDTO data = new SensorDataDTO();
        data.setTrainId(testTrainId);
        data.setCarriageNumber(1);
        data.setTimestamp(LocalDateTime.now());
        return data;
    }

    @Test
    void bothSensorsOk_persistsStatusAndUsesKalmanFusion() throws Exception {
        SensorDataDTO data = baseSensorData();
        data.setCameraCount(25);
        data.setIrCount(23);
        data.setCameraStatus("ok");
        data.setIrStatus("ok");

        mockMvc.perform(post("/api/sensors/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sensors/{carriageId}", testCarriageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cameraCount").value(25))
                .andExpect(jsonPath("$.irCount").value(23))
                .andExpect(jsonPath("$.cameraStatus").value("ok"))
                .andExpect(jsonPath("$.irStatus").value("ok"))
                .andExpect(jsonPath("$.calculatedOccupancy").isNumber());
    }

    @Test
    void irUnavailable_bypassesFusion_usesCameraCountDirectly() throws Exception {
        SensorDataDTO data = baseSensorData();
        data.setCameraCount(18);
        data.setIrCount(0); // stale value from the edge - should not affect the result
        data.setCameraStatus("ok");
        data.setIrStatus("unavailable");

        mockMvc.perform(post("/api/sensors/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sensors/{carriageId}", testCarriageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculatedOccupancy").value(18))
                .andExpect(jsonPath("$.cameraStatus").value("ok"))
                .andExpect(jsonPath("$.irStatus").value("unavailable"))
                // uncertainty was reset to null so Kalman re-initializes later
                .andExpect(jsonPath("$.calculatedUncertainty").value(nullValue()));
    }

    @Test
    void bothUnavailable_keepsLastKnownOccupancyAcrossRealRequests() throws Exception {
        // 1) Establish a baseline with both sensors working
        SensorDataDTO firstUpdate = baseSensorData();
        firstUpdate.setCameraCount(10);
        firstUpdate.setIrCount(10);
        firstUpdate.setCameraStatus("ok");
        firstUpdate.setIrStatus("ok");

        mockMvc.perform(post("/api/sensors/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstUpdate)))
                .andExpect(status().isOk());

        // 2) Both sensors go down in the next cycle
        SensorDataDTO secondUpdate = baseSensorData();
        secondUpdate.setCameraCount(0);
        secondUpdate.setIrCount(0);
        secondUpdate.setCameraStatus("unavailable");
        secondUpdate.setIrStatus("unavailable");

        mockMvc.perform(post("/api/sensors/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondUpdate)))
                .andExpect(status().isOk());

        // The occupancy must still reflect the last known good reading (10),
        // not the fake 0/0 sent while both sources were down.
        mockMvc.perform(get("/api/sensors/{carriageId}", testCarriageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculatedOccupancy").value(10))
                .andExpect(jsonPath("$.cameraStatus").value("unavailable"))
                .andExpect(jsonPath("$.irStatus").value("unavailable"));
    }

    @Test
    void statusFieldsOmitted_backwardCompatible_doesNotCrash() throws Exception {
        // Simulates an older edge device / client that never sends the new
        // cameraStatus/irStatus fields at all - the raw JSON simply omits them.
        String rawJson = String.format(
                "{\"trainId\":%d,\"carriageNumber\":1,\"cameraCount\":12,\"irCount\":14}",
                testTrainId);

        mockMvc.perform(post("/api/sensors/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sensors/{carriageId}", testCarriageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculatedOccupancy").isNumber());
    }
}