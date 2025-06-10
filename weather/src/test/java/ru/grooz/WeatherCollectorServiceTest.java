package ru.grooz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.grooz.dto.SourceWeatherData;
import ru.grooz.service.WeatherAdaptorService;
import ru.grooz.service.WeatherCollectorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class WeatherCollectorServiceTest {

    @Mock
    private WebClient webClient;
    @Mock
    private WeatherAdaptorService weatherAdaptorService;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private WeatherCollectorService weatherCollectorService;

    private static final String MOCK_SOURCE_RESPONSE_FORMAT_1 = "{\"temperature\":\"21.7\",\"humidity\":\"58\"}";
    private static final String MOCK_SOURCE_RESPONSE_FORMAT_2 = "{\"temp\":20.1,\"hum\":55}";

    private long retryMaxAttempts;
    private long retryDelaySeconds;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(weatherCollectorService, "baseUrl", "http://mock-source-service:8081/source/");
        ReflectionTestUtils.setField(weatherCollectorService, "sourceCount", 2);

        retryMaxAttempts = 2L;
        retryDelaySeconds = 1L;
        ReflectionTestUtils.setField(weatherCollectorService, "retryMaxAttempts", retryMaxAttempts);
        ReflectionTestUtils.setField(weatherCollectorService, "retryDelaySeconds", retryDelaySeconds);
    }

    @Test
    void testCollectAndProcessAllWeatherData_SuccessForAllSources() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("http://mock-source-service:8081/source/1")).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri("http://mock-source-service:8081/source/2")).thenReturn(requestHeadersSpec);

        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(MOCK_SOURCE_RESPONSE_FORMAT_1))
                .thenReturn(Mono.just(MOCK_SOURCE_RESPONSE_FORMAT_2));

        doNothing().when(weatherAdaptorService).processAndSaveWeatherData(any(SourceWeatherData.class));

        weatherCollectorService.collectAndProcessAllWeatherData().block();

        verify(webClient, times(2)).get();
        verify(requestHeadersUriSpec).uri("http://mock-source-service:8081/source/1");
        verify(requestHeadersUriSpec).uri("http://mock-source-service:8081/source/2");
        verify(requestHeadersSpec, times(2)).retrieve();
        verify(responseSpec, times(2)).bodyToMono(String.class);
        verify(weatherAdaptorService, times(1)).processAndSaveWeatherData(
                argThat(data -> data.getSourceId() == 1 && data.getPayload().equals(MOCK_SOURCE_RESPONSE_FORMAT_1)));
        verify(weatherAdaptorService, times(1)).processAndSaveWeatherData(
                argThat(data -> data.getSourceId() == 2 && data.getPayload().equals(MOCK_SOURCE_RESPONSE_FORMAT_2)));
    }

    @Test
    void testCollectAndProcessAllWeatherData_WithErrorFromOneSource() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("http://mock-source-service:8081/source/1")).thenReturn(requestHeadersSpec);
        when(requestHeadersUriSpec.uri("http://mock-source-service:8081/source/2")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(MOCK_SOURCE_RESPONSE_FORMAT_1))
                .thenReturn(Mono.error(new RuntimeException("Simulated network error for source 2")));

        doNothing().when(weatherAdaptorService).processAndSaveWeatherData(any(SourceWeatherData.class));

        weatherCollectorService.collectAndProcessAllWeatherData().block();

        verify(webClient, times(2)).get();
        verify(requestHeadersUriSpec, times(2)).uri(anyString());
        verify(requestHeadersSpec, times(2)).retrieve();
        verify(responseSpec, times(2)).bodyToMono(String.class);
        verify(weatherAdaptorService, times(1)).processAndSaveWeatherData(
                argThat(data -> data.getSourceId() == 1 && data.getPayload().equals(MOCK_SOURCE_RESPONSE_FORMAT_1)));
        verify(weatherAdaptorService, never()).processAndSaveWeatherData(
                argThat(data -> data.getSourceId() == 2));
    }
}