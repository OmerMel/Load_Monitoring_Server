# Load Monitoring Server

This repository contains the central server for a real-time train-carriage occupancy monitoring system. 

The server acts as the central integration and processing layer of the overall system. It is responsible for bridging the physical hardware with the end-user application by continuously receiving passenger-count data from the edge unit (Raspberry Pi), processing and combining the raw measurements into reliable estimates, storing those results in the database, and exposing the data to the client application.

## Server Responsibilities

Based on the implemented services and configuration, the server is responsible for:

- Subscribing to an MQTT broker to receive incoming live messages from the edge unit.
- Parsing and validating the continuous flow of camera and ToF (Time of Flight) sensor measurements.
- Retrieving previous carriage states (occupancy and uncertainty) from the PostgreSQL database.
- Executing a **Kalman filter** sensor-fusion algorithm to combine the new measurements with historical data.
- Calculating the final estimated number of passengers and the updated uncertainty value for the carriage.
- Persisting the newly calculated occupancy logs and uncertainty levels back into the database.
- Exposing a set of HTTP REST endpoints allowing the client application to fetch trains, stations, carriage details, and live sensor data.



## System Architecture

The server acts as the central component connecting the entire system:

1. **Edge Unit (Raspberry Pi & Sensors):** Collects passenger-count data and publishes it over MQTT.
2. **MQTT Broker:** Acts as the messaging bridge delivering the telemetry to the server.
3. **Load Monitoring Server:** Subscribes to the MQTT topics, performs Kalman sensor fusion, and handles all business logic.
4. **Database (Supabase / PostgreSQL):** Stores trains, stations, carriages, and historical occupancy logs.
5. **Client Application:** Connects to the server via HTTP to display the occupancy metrics to users.



## Main Technologies

This project is built utilizing the following technologies and libraries:

- **Java 21**
- **Spring Boot 3.4.1** (Spring Web, Spring Data JPA, Spring Integration)
- **PostgreSQL** (managed via **Supabase**)
- **MQTT** (Eclipse Paho Client via Spring Integration MQTT)
- **Gradle** (Kotlin DSL)
- **Lombok** (Code reduction)
- **MapStruct** (DTO to Entity mapping)
- **Swagger / OpenAPI** (Springdoc for API documentation)
- **JUnit 5 & Mockito** (Testing framework)



## Repository Structure

The source code follows a standard Spring Boot layer-based architecture:

```text
src/
├── main/
│   ├── java/com/finalproject/load_monitoring/
│   │   ├── config/       # MQTT and OpenAPI configurations
│   │   ├── controller/   # REST API endpoints (Sensor, Passenger, Station)
│   │   ├── converter/    # MapStruct converters bridging DTOs and Entities
│   │   ├── dto/          # Data Transfer Objects for API and MQTT payloads
│   │   ├── entity/       # JPA Entities mapping to PostgreSQL tables
│   │   ├── exception/    # Custom exception handlers
│   │   ├── repository/   # Spring Data JPA interfaces
│   │   └── service/      # Core business logic and Kalman sensor-fusion algorithms
│   └── resources/        # Application properties (DB, MQTT settings)
└── test/
    └── java/com/finalproject/load_monitoring/
        ├── controller/   # API Integration tests
        └── service/      # Extensive Kalman algorithm and service unit tests
```

## Kalman Sensor-Fusion Algorithm

The core processing logic of the server is handled by the `KalmanSensorFusionService`. 

Because the camera and the ToF (IR) sensors utilize completely different hardware technologies, they have different error characteristics and failure rates. The camera acts as a direct measurement of the current state, while the ToF sensors act as relative counters predicting the change (entrances minus exits).

The Kalman algorithm dynamically combines these measurements by weighing their respective uncertainties. It uses the previous estimated occupancy and the ToF delta to *predict* the current count, and then *corrects* that prediction using the camera reading.

It calculates both:

- **The final estimated number of passengers**
- **The updated uncertainty level**

The uncertainty value is fully maintained by the server and persisted in the database; it is not supplied by the edge devices. It is important to note that the Kalman filter is a statistical *estimation* method, not a guarantee of the mathematically exact number of passengers in reality.

## Important Code Sections

- `calculateOccupancy` [in](src/main/java/com/finalproject/load_monitoring/service/KalmanSensorFusionService.java) `KalmanSensorFusionService` — Implements the core Kalman sensor-fusion calculation. It uses the previous carriage state from the database and the new sensor inputs to calculate both the updated passenger-count estimate and the new uncertainty value.
- `[MqttConfig.handler](src/main/java/com/finalproject/load_monitoring/config/MqttConfig.java)` — Serves as the primary entry point for telemetry data. It subscribes to the MQTT broker, listens for JSON messages published by the edge unit, parses them into Java objects, and triggers the `OccupancyService` processing pipeline.



## Main Data Flow

The typical data flow upon receiving a sensor update behaves as follows:

1. The server receives a JSON message over MQTT containing telemetry such as `trainId`, `carriageNumber`, `cameraCount`, and `irCount`.
2. The `OccupancyService` queries the database for the targeted carriage to retrieve its latest occupancy and uncertainty state.
3. The `KalmanSensorFusionService` combines the new sensor data with the previous state to calculate the new occupancy estimate and the updated uncertainty.
4. The new occupancy value updates the `Carriage` entity, and a new `OccupancyLog` (containing the `calculatedUncertainty`) is generated and persisted in the database.
5. Client applications invoke the server's HTTP GET endpoints (e.g., `/api/passengers`, `/api/sensors/{carriageId}`) to retrieve the latest processed occupancy information.



## Sensor-Data Structure

When the server receives an MQTT message from the edge unit and finishes processing it, the underlying `SensorDataDTO` structure looks like this:

```json
{
  "trainId": 1,
  "carriageNumber": 1,
  "cameraCount": 14,
  "irCount": 10,
  "calculatedOccupancy": 13,
  "calculatedUncertainty": 0.85,
  "timestamp": "2026-07-30T13:39:16.302Z"
}
```

*(Note:* `cameraCount` *and* `irCount` *represent the raw sensor inputs, while* `calculatedOccupancy` *and* `calculatedUncertainty` *are generated internally by the server's Kalman filter).*

## API Endpoints & MQTT Topics

The server exposes several REST endpoints for the client applications and listens to an MQTT topic for IoT telemetry:


| Type / Method       | Endpoint / Topic                                                | Short Description                                                                                                |
| ------------------- | --------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| **REST (GET)**      | `/api/sensors/{carriageId}`                                     | Retrieve the latest sensor data (camera & IR count) and occupancy logs for a specific carriage.                  |
| **REST (POST)**     | `/api/sensors/update`                                           | Manually update a carriage's occupancy based on a `SensorDataDTO` JSON payload.                                  |
| **REST (GET)**      | `/api/passengers`                                               | Retrieve a list of all trains and their basic information.                                                       |
| **REST (GET)**      | `/api/passengers/{id}`                                          | Retrieve detailed information about a specific train by its ID.                                                  |
| **REST (GET)**      | `/api/passengers/search/{origin}/{destination}`                 | Search for trains traveling between a specific origin and destination.                                           |
| **REST (GET)**      | `/api/passengers/search/{origin}/{destination}/{departureTime}` | Search for trains between an origin and destination departing after a specific time.                             |
| **REST (GET)**      | `/api/passengers/carriage/{trainId}/{carriageId}`               | Retrieve detailed information about a specific carriage within a specific train.                                 |
| **REST (GET)**      | `/api/stations`                                                 | Retrieve a list of all available stations.                                                                       |
| **MQTT (Listener)** | `train/sensors/updates`                                         | Subscribes to the MQTT broker to continuously listen for and process live IoT JSON sensor updates automatically. |


