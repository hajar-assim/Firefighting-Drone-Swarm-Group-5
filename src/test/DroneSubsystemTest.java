package test;

import events.EventType;
import events.IncidentEvent;
import main.EventQueueManager;
import org.junit.jupiter.api.*;
import subsystems.DroneSubsystem;

import static org.junit.jupiter.api.Assertions.*;

class DroneSubsystemTest {
    private DroneSubsystem droneSubsystem;
    private TestEventQueueManager eventQueue;
    private Thread droneThread;

    @BeforeEach
    void setUp() {
        eventQueue = new TestEventQueueManager();
        droneSubsystem = new DroneSubsystem(eventQueue);
    }

    @Test
    @DisplayName("Test DroneSubsystem processes an incident event")
    void testProcessIncidentEvent() throws InterruptedException {
        // Add an incident event
        IncidentEvent testEvent = new IncidentEvent("14:05:00", 1, "FIRE_DETECTED", "HIGH", "(0;0)", "(0;600)", "DroneSubsystem");
        eventQueue.put(testEvent);

        // Add shutdown event
        eventQueue.put(new IncidentEvent("14:06:00", 0, "EVENTS_DONE", "HIGH", "(0;0)", "(0;0)", "DroneSubsystem"));

        // Start the subsystem
        droneThread = new Thread(droneSubsystem);
        droneThread.start();

        // Wait for processing to complete
        droneThread.join(5000);

        // Verify processed event
        IncidentEvent processedEvent = eventQueue.get("Scheduler");

        assertNotNull(processedEvent);
        assertEquals("Scheduler", processedEvent.getReceiver());
    }

    @Test
    @DisplayName("Test DroneSubsystem shuts down on EVENTS_DONE")
    void testShutdownOnEventsDone() throws InterruptedException {
        // Updated to use valid Severity level
        eventQueue.put(new IncidentEvent("14:06:00", 0, "EVENTS_DONE", "HIGH", "(0;0)", "(0;0)", "DroneSubsystem"));

        droneThread = new Thread(droneSubsystem);
        droneThread.start();

        Thread.sleep(1000);

        // Ensure thread stops
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
        public synchronized IncidentEvent get(String receiver) {
            while (queue.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    return new IncidentEvent("14:07:00", 0, "EVENTS_DONE", "NONE", "(0;0)", "(0;0)", "Scheduler");
                }
            }
            return queue.poll();
        }
    }
}
