package test;

import events.EventType;
import events.IncidentEvent;
import events.Severity;
import org.junit.jupiter.api.*;

import java.lang.reflect.Method;
import java.util.AbstractMap;

import static org.junit.jupiter.api.Assertions.*;

class IncidentEventTest {

    private IncidentEvent incidentEvent;

    @BeforeEach
    void setUp() {
        incidentEvent = new IncidentEvent("14:03:15", 1, "FIRE_DETECTED", "HIGH", "(0;0)", "(0;600)", "Scheduler");
    }

    @Test
    @DisplayName("Test getter methods")
    void testGetters() {
        assertEquals("14:03:15", incidentEvent.getTimestamp());
        assertEquals(1, incidentEvent.getZoneId());
        assertEquals(EventType.FIRE_DETECTED, incidentEvent.getEventType());
        assertEquals(Severity.HIGH, incidentEvent.getSeverity());
        assertEquals(new AbstractMap.SimpleEntry<>(0, 0), incidentEvent.getStartCoordinates());
        assertEquals(new AbstractMap.SimpleEntry<>(0, 600), incidentEvent.getEndCoordinates());
        assertEquals("Scheduler", incidentEvent.getReceiver());
    }

    @Test
    @DisplayName("Test setReceiver() method")
    void testSetReceiver() {
        incidentEvent.setReceiver("FireIncidentSubsystem");
        assertEquals("FireIncidentSubsystem", incidentEvent.getReceiver());
    }

    @Test
    @DisplayName("Test setEventType() method")
    void testSetEventType() {
        incidentEvent.setEventType(EventType.DRONE_REQUEST);
        assertEquals(EventType.DRONE_REQUEST, incidentEvent.getEventType());
    }

    @Test
    @DisplayName("Test private method parseCoordinates()")
    void testParseCoordinates() throws Exception {
        // make the private method accesible
        Method parseCoordinatesMethod = IncidentEvent.class.getDeclaredMethod("parseCoordinates", String.class);
        parseCoordinatesMethod.setAccessible(true);

        AbstractMap.SimpleEntry<Integer, Integer> result =
                (AbstractMap.SimpleEntry<Integer, Integer>) parseCoordinatesMethod.invoke(incidentEvent, "(100;500)");

        assertEquals(new AbstractMap.SimpleEntry<>(100, 500), result);
    }

    @Test
    @DisplayName("Test toString() method")
    void testToString() {
        String expected = "14:03:15 1 FIRE_DETECTED HIGH Start Coordinates: 0=0 End Coordinates: 0=600";
        assertEquals(expected, incidentEvent.toString());
    }
}
