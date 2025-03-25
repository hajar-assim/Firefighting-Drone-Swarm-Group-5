package subsystems.fire_incident;

/**
 * Severity represents the level of severity for a fire incident.
 *
 * Each level has an associated amount of water and foam required:
 * - LOW: 10 units
 * - MODERATE: 20 units
 * - HIGH: 30 units
 */

public enum Severity {
    NONE(0),
    LOW(10),
    MODERATE(20),
    HIGH(30);

    private final int waterFoamAmount;

    Severity(int amount) {
        this.waterFoamAmount = amount;
    }

    public int getWaterFoamAmount() {
        return waterFoamAmount;
    }

    /**
     * Converts a string to a Severity enum, handling case variations.
     * @return The corresponding Severity enum.
     * @throws IllegalArgumentException if the input is invalid.
     */
    public static Severity fromString(String level) {
        for (Severity severity : values()) {
            if (severity.name().equalsIgnoreCase(level)) {
                return severity;
            }
        }
        throw new IllegalArgumentException("Invalid Severity: " + level);
    }
}