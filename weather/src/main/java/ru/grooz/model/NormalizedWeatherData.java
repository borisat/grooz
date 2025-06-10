package ru.grooz.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "normalized_weather_data")
@Data
public class NormalizedWeatherData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private int sourceId;
    private double temperature;
    private double humidity;
    private LocalDateTime timestamp;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_data_id", referencedColumnName = "id")
    private RawWeatherData rawData;
}