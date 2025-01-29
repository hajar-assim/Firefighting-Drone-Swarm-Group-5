package events;

import java.util.AbstractMap;

/**
 * Represents an incident event, including details such as timestamp, zone ID, event type, severity,
 * and coordinates of the affected area.
 */
public class IncidentEvent {
    private String timestamp;
    private int zoneId;
    private EventType eventType;
    private Severity severity;
    private AbstractMap.SimpleEntry<Integer, Integer> startCoordinates;
    private AbstractMap.SimpleEntry<Integer, Integer> endCoordinates;

    /**
     * Constructs an IncidentEvent object.
     *
     * @param timestamp   The time of the event.
     * @param zoneId      The ID of the affected zone.
     * @param eventType   The type of event occurring.
     * @param severity    The severity of the incident.
     * @param startCoords The start coordinates of the affected area.
     * @param endCoords   The end coordinates of the affected area.
     */
    public IncidentEvent(String timestamp, int zoneId, String eventType, String severity, String startCoords, String endCoords) {
        this.timestamp = timestamp;
        this.zoneId = zoneId;
        this.eventType = EventType.fromString(eventType);
        this.severity = Severity.fromString(severity);
        this.startCoordinates = parseCoordinates(startCoords);
        this.endCoordinates = parseCoordinates(endCoords);
    }

    /**
     * Retrieves the timestamp of the incident.
     *
     * @return The timestamp as a String.
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Retrieves the ID of the affected zone.
     *
     * @return The zone ID as an integer.
     */
    public int getZoneId() {
        return zoneId;
    }

    /**
     * Retrieves the type of event.
     *
     * @return The event type as an EventType enum.
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Retrieves the severity level of the incident.
     *
     * @return The severity level as a Severity enum.
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Retrieves the start coordinates of the affected area.
     *
     * @return The start coordinates as an AbstractMap.SimpleEntry<Integer, Integer>.
     */
    public AbstractMap.SimpleEntry<Integer, Integer> getStartCoordinates() {
        return startCoordinates;
    }

    /**
     * Retrieves the end coordinates of the affected area.
     *
     * @return The end coordinates as an AbstractMap.SimpleEntry<Integer, Integer>.
     */
    public AbstractMap.SimpleEntry<Integer, Integer> getEndCoordinates() {
        return endCoordinates;
    }

    /**
     * Parses a coordinate string into an AbstractMap.SimpleEntry<Integer, Integer>.
     *
     * @param coordinates The coordinate string in the format "(x;y)".
     * @return A key-value pair representing the parsed coordinates.
     * @throws NumberFormatException if the input string cannot be properly parsed.
     */
    private AbstractMap.SimpleEntry<Integer, Integer> parseCoordinates(String coordinates) {
        coordinates = coordinates.replace("(", "").replace(")", "");
        String[] parts = coordinates.split(";");
        return new AbstractMap.SimpleEntry<>(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
    }

    /**
     * Returns a string representation of the IncidentEvent object.
     *
     * @return A string containing the timestamp, zone ID, event type, severity, and coordinates.
     */
    @Override
    public String toString() {
        return timestamp + " " + zoneId + " " + eventType + " " + severity + " Start Coordinates: " + startCoordinates + " End Coordinates: " + endCoordinates;
    }
}
