package events;

public enum EventType {
    FIRE_DETECTED,
    DRONE_REQUEST;

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
            default -> throw new IllegalArgumentException("Invalid Event Type: " + str);
        };
    }
}
