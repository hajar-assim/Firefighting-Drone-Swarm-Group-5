package main.ui;

import subsystems.fire_incident.Severity;

import java.awt.*;

// fire Severity → Color
public enum FireSeverityColor {
    LOW(new Color(255, 255, 0, 80)),       // translucent yellow
    MODERATE(new Color(255, 165, 0, 80)),  // translucent orange
    HIGH(new Color(255, 0, 0, 80)),        // translucent red
    NONE(new Color(200, 200, 200, 60));    // gray

    public final Color color;

    FireSeverityColor(Color color) {
        this.color = color;
    }

    public static Color fromSeverity(Severity s) {
        return switch (s) {
            case LOW -> LOW.color;
            case MODERATE -> MODERATE.color;
            case HIGH -> HIGH.color;
            case NONE -> NONE.color;
            case null -> NONE.color;
        };
    }
    
}