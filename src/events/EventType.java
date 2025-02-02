package events;

/**
 * EventType represents the different types of events in the fire incident and drone response system.
 *
 * Events include:
 * - FIRE_DETECTED: A fire has been detected.
 * - DRONE_REQUEST: A request for a drone has been made.
 * - DRONE_DISPATCHED: A drone has been dispatched.
 * - EVENTS_DONE: All events for the incident are complete.
 */

public enum EventType {
    FIRE_DETECTED,
    DRONE_REQUEST,
    DRONE_DISPATCHED,
    EVENTS_DONE;

    /**
     * Converts a string to an EventType enum, handling case variations.
     * @param str The string representation of event type (e.g., "fire_detected", "DRONE_REQUEST").
     * @return The corresponding EventType enum.
     * @throws IllegalArgumentException if the input is invalid.
     */
    public static EventType fromString(String str) {
        return switch (str.trim().toUpperCase()) {
            case "FIRE_DETECTED" -> FIRE_DETECTED;
            case "DRONE_REQUEST" -> DRONE_REQUEST;
            case "DRONE_DISPATCHED" -> DRONE_DISPATCHED;
            case "EVENTS_DONE" -> EVENTS_DONE;
            default -> throw new IllegalArgumentException("Invalid Event Type: " + str);
        };
    }
}
