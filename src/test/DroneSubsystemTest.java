package test;

import events.EventType;
import events.IncidentEvent;
import main.EventQueueManager;
import org.junit.jupiter.api.*;
import subsystems.DroneSubsystem;

import static org.junit.jupiter.api.Assertions.*;

class DroneSubsystemTest {
    private DroneSubsystem droneSubsystem;
    private TestEventQueueManager sendQueue;
    private TestEventQueueManager receiveQueue;
    private Thread droneThread;

    @BeforeEach
    void setUp() {
        sendQueue = new TestEventQueueManager();
        receiveQueue = new TestEventQueueManager();
        droneSubsystem = new DroneSubsystem(receiveQueue, sendQueue);
    }

    @Test
    @DisplayName("Test DroneSubsystem processes an incident event")
    void testProcessIncidentEvent() throws InterruptedException {
        IncidentEvent testEvent = new IncidentEvent("14:05:00", 1, "FIRE_DETECTED", "HIGH", "(0;0)", "(0;600)", "DroneSubsystem");
        receiveQueue.put(testEvent);

        receiveQueue.put(new IncidentEvent("14:06:00", 0, "EVENTS_DONE", "HIGH", "(0;0)", "(0;0)", "DroneSubsystem"));

        // Start the subsystem
        droneThread = new Thread(droneSubsystem);
        droneThread.start();

        // Wait for processing to complete
        droneThread.join(5000);

        IncidentEvent processedEvent = sendQueue.get();

        assertNotNull(processedEvent);
        assertEquals("FireIncident", processedEvent.getReceiver());
        assertEquals(EventType.DRONE_DISPATCHED, processedEvent.getEventType());
    }

    @Test
    @DisplayName("Test DroneSubsystem shuts down on EVENTS_DONE")
    void testShutdownOnEventsDone() throws InterruptedException {
        receiveQueue.put(new IncidentEvent("14:06:00", 0, "EVENTS_DONE", "HIGH", "(0;0)", "(0;0)", "DroneSubsystem"));

        droneThread = new Thread(droneSubsystem);
        droneThread.start();

        Thread.sleep(1000);

        droneThread.join(1000);
        assertFalse(droneThread.isAlive());
    }

    static class TestEventQueueManager extends EventQueueManager {
        public TestEventQueueManager() {
            super("TestQueue");
        }

        private final java.util.Queue<IncidentEvent> queue = new java.util.concurrent.ConcurrentLinkedQueue<>();

        @Override
        public synchronized void put(IncidentEvent event) {
            queue.add(event);
            notifyAll();
        }

        @Override
        public synchronized IncidentEvent get() {
            while (queue.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    return new IncidentEvent("14:07:00", 0, "EVENTS_DONE", "HIGH", "(0;0)", "(0;0)", "FireIncident");
                }
            }
            return queue.poll();
        }
    }
}
