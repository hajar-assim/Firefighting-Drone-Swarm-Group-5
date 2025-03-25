package helpers;

import subsystems.fire_incident.Severity;
import subsystems.fire_incident.events.IncidentEvent;

import java.util.Comparator;

public class IncidentEventComparator implements Comparator<IncidentEvent> {

    /**
     * Compares two incident events, first by severity level (HIGH > MODERATE > LOW > NONE),
     * then by timestamp.
    */
     @Override
    public int compare(IncidentEvent a, IncidentEvent b) {
        int severityA = severityToInt(a.getSeverity());
        int severityB = severityToInt(b.getSeverity());

        // higher severity should come first
        if (severityA != severityB) {
            return Integer.compare(severityB, severityA);
        }

        // compare by timestamp if severities are equal
        return a.getParsedTime().compareTo(b.getParsedTime());
    }

    /**
     * Converts a severity level to an integer for comparison.
     *
     * @param severity the severity level to convert
     * @return an integer representing the severity level
     */
    private int severityToInt(Severity severity) {
        return switch (severity) {
            case HIGH -> 3;
            case MODERATE -> 2;
            case LOW -> 1;
            default -> 0;
        };
    }
}
