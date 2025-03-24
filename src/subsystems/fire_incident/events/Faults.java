package subsystems.fire_incident.events;

/**
 * The faults a drone can experience
 */
public enum Faults {
    DRONE_STUCK_IN_FLIGHT,
    NOZZLE_JAMMED,
    PACKET_LOSS,
    NONE;

    public static Faults fromString(String fault) {
        for (Faults type : values()) {
            if (type.name().equalsIgnoreCase(fault)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid Fault Type: " + fault);
    }
}
