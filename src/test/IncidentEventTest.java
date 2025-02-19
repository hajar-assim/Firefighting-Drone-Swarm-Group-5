package test;

import events.EventType;
import events.IncidentEvent;
import events.Severity;
import org.junit.jupiter.api.*;

import java.awt.geom.Point2D;
import java.lang.reflect.Method;
import java.util.AbstractMap;

import static org.junit.jupiter.api.Assertions.*;

class IncidentEventTest {

    private IncidentEvent incidentEvent;

    @BeforeEach
    void setUp() {
        incidentEvent = new IncidentEvent("14:03:15", 1, "FIRE_DETECTED", "HIGH");
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
        String expected = "14:03:15 1 FIRE_DETECTED HIGH";
        assertEquals(expected, incidentEvent.toString());
    }
}
