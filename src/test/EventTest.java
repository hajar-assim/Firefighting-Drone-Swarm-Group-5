package test;

import events.*;
import org.junit.jupiter.api.*;
import subsystems.DroneState;
import subsystems.DroneStatus;

import java.awt.geom.Point2D;

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
        DroneDispatchEvent event = new DroneDispatchEvent(200, coords);
        assertEquals(200, event.getZoneID());
        assertEquals(coords, event.getCoords());
        event.fromString("");
    }

    // Test DroneUpdateEvent class
    @Test
    @DisplayName("Test DroneUpdateEvent class")
    void testDroneUpdateEvent() {
        DroneState state = new DroneState(DroneStatus.IDLE, 1, null, 100, 50);
        DroneUpdateEvent event = new DroneUpdateEvent(1, state);
        assertEquals(1, event.getDroneID());
        assertEquals(state, event.getDroneState());
        event.fromString("");
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
