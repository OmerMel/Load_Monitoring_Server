package com.finalproject.load_monitoring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finalproject.load_monitoring.dto.SensorDataDTO;
import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.entity.Station;
import com.finalproject.load_monitoring.entity.Train;
import com.finalproject.load_monitoring.repository.CarriageRepository;
import com.finalproject.load_monitoring.repository.StationRepository;
import com.finalproject.load_monitoring.repository.TrainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SensorControllerIntegrationTest {

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

    // Mock the MQTT adapter so it doesn't try to connect to the broker during testing
    @MockitoBean(name = "inbound")
    private org.springframework.integration.core.MessageProducer inbound;

    private Long testTrainId;
    private Long testCarriageId;

    @BeforeEach
    void setUp() {
        carriageRepository.deleteAll();
        trainRepository.deleteAll();
        stationRepository.deleteAll();

        // 1. Create Stations
        Station origin = new Station();
        origin.setStationName("Origin Station");
        origin = stationRepository.save(origin);

        Station dest = new Station();
        dest.setStationName("Dest Station");
        dest = stationRepository.save(dest);

        // 2. Create Train
        Train train = new Train();
        train.setOriginStation(origin);
        train.setDestinationStation(dest);
        train.setDepartureTime(LocalDateTime.now().minusHours(1));
        train.setArrivalTime(LocalDateTime.now().plusHours(1));
        train.setLastUpdated(LocalDateTime.now());
        train = trainRepository.save(train);
        testTrainId = train.getTrainId();

        // 3. Create Carriage
        Carriage carriage = new Carriage();
        carriage.setTrain(train);
        carriage.setCarriageNumber(1);
        carriage.setOccupancy(0);
        carriage.setMaxCapacity(100);
        carriage.setLastUpdated(LocalDateTime.now());
        carriage = carriageRepository.save(carriage);
        testCarriageId = carriage.getCarriageId();
    }

    @Test
    void shouldAcceptSensorUpdateAndReturnOccupancyDataSuccessfully() throws Exception {
        // Arrange: Prepare the incoming JSON from the sensor
        SensorDataDTO sensorData = new SensorDataDTO();
        sensorData.setTrainId(testTrainId);
        sensorData.setCarriageNumber(1);
        sensorData.setCameraCount(25);
        sensorData.setIrCount(23);
        sensorData.setTimestamp(LocalDateTime.now());

        // Act & Assert 1: POST to /api/sensors/update
        mockMvc.perform(post("/api/sensors/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sensorData)))
                .andExpect(status().isOk())
                .andExpect(content().string("Occupancy updated successfully"));

        // Act & Assert 2: GET from /api/sensors/{carriageId}
        mockMvc.perform(get("/api/sensors/{carriageId}", testCarriageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cameraCount").value(25))
                .andExpect(jsonPath("$.irCount").value(23))
                // The calculated occupancy should exist in the response JSON and be a valid number
                // (Usually around 25 based on the Kalman filter initialized with the camera)
                .andExpect(jsonPath("$.calculatedOccupancy").isNumber())
                // Ensure it does not leak internal Kalman state or new database fields unexpectedly
                .andExpect(jsonPath("$.uncertainty").doesNotExist())
                .andExpect(jsonPath("$.estimatedPassengerCount").doesNotExist());
    }
}
