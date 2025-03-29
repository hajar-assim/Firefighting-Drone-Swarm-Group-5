package test;

import org.junit.jupiter.api.*;
import subsystems.drone.DroneInfo;
import subsystems.drone.events.DroneArrivedEvent;
import subsystems.drone.events.DroneDispatchEvent;
import subsystems.drone.events.DroneUpdateEvent;
import subsystems.drone.events.DropAgentEvent;
import subsystems.fire_incident.Faults;

import java.awt.geom.Point2D;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class EventTests {

    // Test IncidentEvent class
    @Test
    @DisplayName("Test DroneArrivedEvent class")
    void testDroneArrivedEvent() {
        DroneArrivedEvent event = new DroneArrivedEvent(1, 100);
        assertEquals(1, event.getDroneID());
        assertEquals(100, event.getZoneID());
        event.fromString("");
    }

    // Test DroneDispatchEvent class
    @Test
    @DisplayName("Test DroneDispatchEvent class")
    void testDroneDispatchEvent() {
        Point2D coords = new Point2D.Double(100.0, 200.0);
        DroneDispatchEvent event = new DroneDispatchEvent(200, coords, Faults.NONE);
        assertEquals(200, event.getZoneID());
        assertEquals(coords, event.getCoords());
        event.fromString("");
    }

    // Test DroneUpdateEvent class
    @Test
    @DisplayName("Test DroneUpdateEvent class")
    void testDroneUpdateEvent() {
        DroneInfo info = null;
        try {
            info = new DroneInfo(InetAddress.getLocalHost(), 2000);
        } catch (UnknownHostException e){
            e.printStackTrace();
        }

        DroneUpdateEvent event = new DroneUpdateEvent(info);

        assertEquals(-1, event.getDroneID());
        assertEquals(info, event.getDroneInfo());

        assertDoesNotThrow(() -> event.fromString(""));
    }

    // Test DropAgentEvent class
    @Test
    @DisplayName("Test DropAgentEvent class")
    void testDropAgentEvent() {
        DropAgentEvent event = new DropAgentEvent(30, 2);
        assertEquals(30, event.getVolume());
        assertEquals(2, event.getDroneID());
        event.fromString("");
    }

}
