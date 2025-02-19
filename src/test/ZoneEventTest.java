package test;

import events.EventType;
import events.IncidentEvent;
import events.Severity;
import events.ZoneEvent;
import org.junit.jupiter.api.*;

import java.awt.geom.Point2D;
import java.lang.reflect.Method;
import java.util.AbstractMap;

import static org.junit.jupiter.api.Assertions.*;

class ZoneEventTest {

    private ZoneEvent zoneEvent;

    @BeforeEach
    void setUp() {
        zoneEvent = new ZoneEvent( 1, "(0;0)", "(0;100)");
    }

    @Test
    @DisplayName("Test getter methods")
    void testGetters() {
        assertEquals(null, zoneEvent.getTimeStamp());
        assertEquals(1, zoneEvent.getZoneID());
        assertEquals(new Point2D.Double(0, 50), zoneEvent.getCenter());
    }


    @Test
    @DisplayName("Test private method parseCoordinates()")
    void testParseCoordinates() throws Exception {
        // make the private method accesible
        Method parseCoordinatesMethod = ZoneEvent.class.getDeclaredMethod("parseCoordinates", String.class);
        parseCoordinatesMethod.setAccessible(true);

        Point2D result =
                (Point2D) parseCoordinatesMethod.invoke(zoneEvent, "(100;500)");

        assertEquals(new Point2D.Double(100,500), result);
    }

}
