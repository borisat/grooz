package ru.grooz.dto;

public class SourceWeatherData {
    private final int sourceId;
    private final String payload;

    public SourceWeatherData(int sourceId, String payload) {
        this.sourceId = sourceId;
        this.payload = payload;
    }

    public int getSourceId() {
        return sourceId;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "SourceWeatherData{sourceId=" + sourceId + ", payload='" + payload + "'}";
    }
}