package events;

/**
 * Represents an incident event, including details such as timestamp, zone ID, event type, severity,
 * and coordinates of the affected area.
 */
public class IncidentEvent extends Event{
    private EventType eventType;
    private Severity severity;
    private int zoneID;

    /**
     * Constructs an IncidentEvent object.
     *
     * @param timestamp   The time of the event.
     * @param eventType   The type of event occurring.
     * @param severity    The severity of the incident.
     */
    public IncidentEvent(String timestamp, int zoneID, String eventType, String severity) {
        super(timestamp);
        this.zoneID = zoneID;
        this.eventType = EventType.fromString(eventType);
        this.severity = Severity.fromString(severity);
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
     * Sets the type of event.
     *
     */
    public void setEventType(EventType event) {
        this.eventType = event;
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
     * Retrieves the Zone ID.
     *
     * @return The Zone ID.
     */
    public int getZoneID() {
        return this.zoneID;
    }

    /**
     * Returns a string representation of the IncidentEvent object.
     *
     * @return A string containing the timestamp, zone ID, event type, severity, and coordinates.
     */
    @Override
    public String toString() {
        if ("fire_extinguished".equals(this.eventType)) {
            return String.format(
                    "Zone: %d | Type: %s | Severity: %s",
                    this.zoneID, this.eventType, this.severity
            );
        } else {
            return String.format(
                    "Time: %s | Zone: %d | Type: %s | Severity: %s",
                    this.getTimeStamp(), this.zoneID, this.eventType, this.severity
            );
        }
    }


    @Override
    public void fromString(String s) {

    }
}
