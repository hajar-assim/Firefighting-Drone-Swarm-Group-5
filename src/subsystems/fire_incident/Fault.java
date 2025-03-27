package subsystems.fire_incident;

/**
 * The faults a drone can experience
 */
public enum Fault {
    DRONE_STUCK_IN_FLIGHT,
    NOZZLE_JAMMED,
    PACKET_LOSS,
    NONE;

    public static Fault fromString(String fault) {
        for (Fault type : values()) {
            if (type.name().equalsIgnoreCase(fault)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid Fault Type: " + fault);
    }
}
