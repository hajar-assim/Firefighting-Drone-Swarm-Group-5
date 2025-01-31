package events;

public enum Severity {
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
     * @param str The string representation of severity (e.g., "high", "LOW").
     * @return The corresponding Severity enum.
     * @throws IllegalArgumentException if the input is invalid.
     */
    public static Severity fromString(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Severity level cannot be null");
        }
        return switch (str.trim().toUpperCase()) {
            case "LOW" -> LOW;
            case "MODERATE" -> MODERATE;
            case "HIGH" -> HIGH;
            default -> throw new IllegalArgumentException("Invalid Severity Level: " + str);
        };
    }
}