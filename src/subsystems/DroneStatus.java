package subsystems;

/**
 * Represents the various states a drone can be in.
 */
public enum DroneStatus {
    /** The drone is idle and not performing any tasks. */
    IDLE,

    /** The drone is en route to a destination. */
    ON_ROUTE,

    /** The drone is actively dropping fire suppression agent. */
    DROPPING_AGENT,

    /** The drone is refilling its fire suppression agent. */
    REFILLING;


    /**
     * Converts a string representation of a drone status into its corresponding enum value.
     *
     * @param str the string representation of the drone status (case-insensitive)
     * @return the corresponding {@code DroneStatus} enum value
     * @throws IllegalArgumentException if the input string does not match any known status
     */
    public static DroneStatus fromString(String str) {
        return switch (str.trim().toUpperCase()) {
            case "IDLE" -> IDLE;
            case "ON_ROUTE" -> ON_ROUTE;
            case "DROPPING_AGENT" -> DROPPING_AGENT;
            case "REFILLING" -> REFILLING;
            default -> throw new IllegalArgumentException("Invalid Drone Status Type: " + str);
        };
    }
}
