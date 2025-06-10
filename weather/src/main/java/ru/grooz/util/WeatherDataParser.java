package ru.grooz.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.grooz.dto.WeatherData;

@Component
@Slf4j
public class WeatherDataParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherData parse(String jsonPayload) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonPayload);

            // Пример 1: { "temp": 20.1, "hum": 55 }
            if (rootNode.has("temp") && rootNode.has("hum")) {
                return new WeatherData(
                        rootNode.get("temp").asDouble(),
                        rootNode.get("hum").asDouble()
                );
            }

            // Пример 2: { "temperature": "21.7", "humidity": "58" }
            if (rootNode.has("temperature") && rootNode.has("humidity")) {
                return new WeatherData(
                        Double.parseDouble(rootNode.get("temperature").asText()),
                        Double.parseDouble(rootNode.get("humidity").asText())
                );
            }

            // Пример 3: { "weather": { "t": 22.5, "h": 53.3 } }
            if (rootNode.has("weather")) {
                JsonNode weatherNode = rootNode.get("weather");
                if (weatherNode.has("t") && weatherNode.has("h")) {
                    return new WeatherData(
                            weatherNode.get("t").asDouble(),
                            weatherNode.get("h").asDouble()
                    );
                }
            }

            // Если ни один формат не подходит, можно выбросить исключение или вернуть null
            throw new IllegalArgumentException("Unknown weather data format: " + jsonPayload);

        } catch (Exception e) {
            // Логирование ошибки парсинга
            log.error("Error parsing weather data: " + jsonPayload + " - " + e.getMessage());
            throw new RuntimeException("Failed to parse weather data", e);
        }
    }
}
