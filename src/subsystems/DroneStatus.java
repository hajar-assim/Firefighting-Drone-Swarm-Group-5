package subsystems;


public enum DroneStatus {
    IDLE,
    ON_ROUTE,
    DROPPING_AGENT,
    REFILLING;


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
