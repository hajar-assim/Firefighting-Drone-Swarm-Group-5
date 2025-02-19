package subsystems;


public enum DroneStatus {
    IDLE,
    ON_ROUTE,
    DROPPING_AGENT;


    public static DroneStatus fromString(String str) {
        return switch (str.trim().toUpperCase()) {
            case "IDLE" -> IDLE;
            case "ON_ROUTE" -> ON_ROUTE;
            case "DROPPING_AGENT" -> DROPPING_AGENT;
            default -> throw new IllegalArgumentException("Invalid Drone Status Type: " + str);
        };
    }
}
