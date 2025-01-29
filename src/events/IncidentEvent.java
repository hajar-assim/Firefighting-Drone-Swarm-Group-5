package events;

import java.util.AbstractMap;

public class IncidentEvent {
    String timestamp;
    int zoneId;
    EventType eventType;
    Severity severity;
    AbstractMap.SimpleEntry<Integer, Integer> startCoordinates;
    AbstractMap.SimpleEntry<Integer, Integer> endCoordinates;

    public IncidentEvent(String timestamp, int zoneId, String eventType, String severity, String startCoords, String endCoords) {
        this.timestamp = timestamp;
        this.zoneId = zoneId;
        this.eventType = EventType.fromString(eventType);
        this.severity = Severity.fromString(severity);
        this.startCoordinates = parseCoordinates(startCoords);
        this.endCoordinates = parseCoordinates(endCoords);
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
