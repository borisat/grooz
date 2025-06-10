package ru.mockweather.service;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Locale;
import java.util.Random;

@Service
public class MockWeatherService {

    private final Random random = new Random();

    public String getSourceData(@PathVariable int id) {
        double temp = 15.0 + random.nextDouble() * 10.0; // Температура от 15
        double hum = 25.0 + random.nextDouble() * 20.0; // Влажность от 25

        int format = random.nextInt(3); // Случайный формат: 0, 1 или 2

        return switch (format) {
            case 0 -> String.format(Locale.ENGLISH, "{ \"temp\": %.1f, \"hum\": %.0f }", temp, hum);
            case 1 -> String.format(Locale.ENGLISH, "{ \"temperature\": \"%.1f\", \"humidity\": \"%.0f\" }", temp, hum);
            case 2 -> String.format(Locale.ENGLISH, "{ \"weather\": { \"t\": %.1f, \"h\": %.1f } }", temp, hum);
            default -> throw new IllegalStateException("Unexpected format value: " + format);
        };
    }
}