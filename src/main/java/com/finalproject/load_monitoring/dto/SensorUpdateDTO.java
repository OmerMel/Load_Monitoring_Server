package com.finalproject.load_monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorUpdateDTO {
    private Long trainId;    // מזהה יציב (למשל "135")
    private int carriageNumber;    // מיקום הקרון (למשל 2)
    private int currentOccupancy;  // הנתון המעובד מה-Pi
}