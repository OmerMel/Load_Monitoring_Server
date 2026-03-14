package com.finalproject.load_monitoring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// For the Swagger UI
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI loadMonitoringOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Train Carriage Occupancy API")
                        .description("API for monitoring train carriage occupancy, receiving sensor data, and serving passenger information.")
                        .version("1.0"));
    }
}
