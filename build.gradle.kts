plugins {
	java
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.finalproject"
version = "0.0.1-SNAPSHOT"
description = "Load Monitoring Server"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-h2console")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-integration")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

	implementation("org.springframework.integration:spring-integration-http")
	implementation("org.springframework.integration:spring-integration-jpa")

    //MQTT
    implementation("org.springframework.integration:spring-integration-mqtt")

	runtimeOnly("com.h2database:h2")

    // Lombok
    compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

    //Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")


	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.integration:spring-integration-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
