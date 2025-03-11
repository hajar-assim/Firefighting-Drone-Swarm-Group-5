package subsystems.fire_incident.events;

import subsystems.Event;
import subsystems.EventType;

/**
 * Represents an incident event, including details such as timestamp, zone ID, event type, and severity.
 */
public class IncidentEvent extends Event {
    private EventType eventType;
    private Severity severity;
    private int zoneID;

    /**
     * Constructs an IncidentEvent object.
     *
     * @param timestamp   The time of the event.
     * @param zoneID      The ID of the fire zone where the event occurs.
     * @param eventType   The type of event occurring (e.g., "FIRE_DETECTED", "FIRE_EXTINGUISHED").
     * @param severity    The severity of the incident (e.g., "LOW", "MODERATE", "HIGH").
     */
    public IncidentEvent(String timestamp, int zoneID, EventType eventType, Severity severity) {
        super(timestamp);
        this.eventType = eventType;
        this.severity = severity;
        this.zoneID = zoneID;
    }

    /**
     * Retrieves the type of event.
     *
     * @return The event type as a String.
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Sets the type of event.
     *
     * @param eventType The new event type (e.g., "FIRE_DETECTED").
     */
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
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
     * @return A formatted string containing the incident details.
     */
    @Override
    public String toString() {
        return String.format(
                "Time: %s | Zone: %d | Type: %s | Severity: %s",
                this.getTimeStamp(), this.zoneID, this.eventType, this.severity
        );
    }

    /**
     * Parses a string into an IncidentEvent object.
     * (Implementation depends on how you format event strings).
     */
    @Override
    public void fromString(String s) {
        // Implement parsing logic if necessary
    }
}
