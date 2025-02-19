package test;

import events.EventType;
import events.IncidentEvent;
import main.EventQueueManager;
import org.junit.jupiter.api.*;
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
        assertFalse(droneThread.isAlive());
    }
}
