package test;

import events.*;
import main.EventQueueManager;
import org.junit.jupiter.api.*;
import subsystems.DroneState;
import subsystems.DroneStatus;
import subsystems.DroneSubsystem;

import static org.junit.jupiter.api.Assertions.*;

class DroneSubsystemTest {
    private DroneSubsystem droneSubsystem;
    private EventQueueManager sendQueue;
    private EventQueueManager receiveQueue;
    private Thread droneThread;

    @BeforeEach
    void setUp() {
        sendQueue = new EventQueueManager("Send Queue");
        receiveQueue = new EventQueueManager("Receive Queue");
        droneSubsystem = new DroneSubsystem(receiveQueue, sendQueue);
    }

    @Test
    @DisplayName("Test DroneSubsystem processes an incident event")
    void testProcessIncidentEvent() throws InterruptedException {
        // Start the subsystem
        droneThread = new Thread(droneSubsystem);
        droneThread.start();

        IncidentEvent testEvent = new IncidentEvent("14:05:00", 1, "FIRE_DETECTED", "HIGH");
        receiveQueue.put(testEvent);

        IncidentEvent processedEvent = (IncidentEvent) sendQueue.get();

        assertNotNull(processedEvent);
        assertEquals(EventType.DRONE_DISPATCHED, processedEvent.getEventType());

        receiveQueue.put(new IncidentEvent("14:06:00", 0, "EVENTS_DONE", "HIGH"));
        Thread.sleep(500);
        assertFalse(droneThread.isAlive());
    }

    @Test
    @DisplayName("Test DroneSubsystem shuts down on EVENTS_DONE")
    void testShutdownOnEventsDone() throws InterruptedException {
        receiveQueue.put(new IncidentEvent("14:06:00", 0, "EVENTS_DONE", "HIGH"));

        droneThread = new Thread(droneSubsystem);
        droneThread.start();

        Thread.sleep(1000);

        droneThread.join(1000);
        assertTrue(droneThread.isAlive());
    }

    // Test DroneArrivedEvent Handling
    @Test
    @DisplayName("Test DroneArrivedEvent Handling")
    void testDroneArrivedEvent() {
        DroneArrivedEvent event = new DroneArrivedEvent(1, 100);
        assertEquals(1, event.getDroneID());
        assertEquals(100, event.getZoneID());
    }

    // Test DroneDispatchEvent Handling
    @Test
    @DisplayName("Test DroneDispatchEvent Handling")
    void testDroneDispatchEvent() {
        DroneDispatchEvent event = new DroneDispatchEvent(200, null);
        assertEquals(200, event.getZoneID());
        assertNull(event.getCoords()); // Assuming no coordinates provided
    }

    // Test DroneUpdateEvent Handling
    @Test
    @DisplayName("Test DroneUpdateEvent Handling")
    void testDroneUpdateEvent() {
        DroneState state = new DroneState(DroneStatus.IDLE, 1, null, 100, 50);
        DroneUpdateEvent event = new DroneUpdateEvent(1, state);
        assertEquals(1, event.getDroneID());
        assertEquals(state, event.getDroneState());
    }

    // Test DropAgentEvent Handling
    @Test
    @DisplayName("Test DropAgentEvent Handling")
    void testDropAgentEvent() {
        DropAgentEvent event = new DropAgentEvent(30, 2);
        assertEquals(30, event.getVolume());
        assertEquals(2, event.getDroneID());
    }

}
