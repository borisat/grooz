package ru.grooz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.grooz.dto.AggregatedWeatherResponse;
import ru.grooz.service.WeatherService;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;
    @GetMapping("/aggregate")
    public Mono<AggregatedWeatherResponse> getAggregatedWeather() {
        return weatherService.getAggregatedWeatherData();
    }
}
