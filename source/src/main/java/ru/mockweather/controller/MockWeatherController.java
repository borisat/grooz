package ru.mockweather.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.mockweather.service.MockWeatherService;

@RestController
@RequiredArgsConstructor
public class MockWeatherController {

    private final MockWeatherService mockWeatherService;

    @GetMapping("/source/{id}")
    public String getSourceData(@PathVariable int id) {
        return mockWeatherService.getSourceData(id);
    }

}
