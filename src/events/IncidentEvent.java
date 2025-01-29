package events;

import java.util.AbstractMap;

public class IncidentEvent {
    private String timestamp;
    private int zoneId;
    private EventType eventType;
    private Severity severity;
    private AbstractMap.SimpleEntry<Integer, Integer> startCoordinates;
    private AbstractMap.SimpleEntry<Integer, Integer> endCoordinates;

    public IncidentEvent(String timestamp, int zoneId, String eventType, String severity, String startCoords, String endCoords) {
        this.timestamp = timestamp;
        this.zoneId = zoneId;
        this.eventType = EventType.fromString(eventType);
        this.severity = Severity.fromString(severity);
        this.startCoordinates = parseCoordinates(startCoords);
        this.endCoordinates = parseCoordinates(endCoords);
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getZoneId() {
        return zoneId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public AbstractMap.SimpleEntry<Integer, Integer> getStartCoordinates() {
        return startCoordinates;
    }

    public AbstractMap.SimpleEntry<Integer, Integer> getEndCoordinates() {
        return endCoordinates;
    }

    private AbstractMap.SimpleEntry<Integer, Integer> parseCoordinates(String coords) {
        coords = coords.replace("(", "").replace(")", "");
        String[] parts = coords.split(";");
        return new AbstractMap.SimpleEntry<>(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
    }

    @Override
    public String toString() {
        return timestamp + " " + zoneId + " " + eventType + " " + severity + " Start Coordinates: " + startCoordinates + " End Coordinates: " + endCoordinates;
    }
}
