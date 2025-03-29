package test;

import subsystems.EventType;
import subsystems.fire_incident.Faults;
import subsystems.fire_incident.events.IncidentEvent;
import subsystems.fire_incident.Severity;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class IncidentEventTest {

    private IncidentEvent incidentEvent;

    @BeforeEach
    void setUp() {
        incidentEvent = new IncidentEvent("14:03:15", 1, EventType.FIRE_DETECTED, Severity.HIGH, Faults.NONE); // Faults is none for now, we'll change it as needed
    }

    @Test
    @DisplayName("Test getter methods")
    void testGetters() {
        assertEquals("14:03:15", incidentEvent.getTimeStamp());
        assertEquals(1, incidentEvent.getZoneID());
        assertEquals(EventType.FIRE_DETECTED, incidentEvent.getEventType());
        assertEquals(Severity.HIGH, incidentEvent.getSeverity());
    }


    @Test
    @DisplayName("Test toString() method")
    void testToString() {
        String expected = "Time: 14:03:15 | Zone: 1 | Type: FIRE_DETECTED | Severity: HIGH | Fault: NONE";
        assertEquals(expected, incidentEvent.toString());
    }
}
