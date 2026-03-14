package com.finalproject.load_monitoring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "occupancy_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OccupancyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carriage_id")
    @ToString.Exclude
    private Carriage carriage;
    private int cameraCount;
    private int irCount;
    private int calculatedOccupancy;
    private LocalDateTime timestamp;
}
