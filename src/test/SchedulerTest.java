package test;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import main.EventQueueManager;
import main.Scheduler;
import events.*;
import subsystems.DroneState;
import subsystems.DroneStatus;
import subsystems.DroneSubsystem;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

class SchedulerTest {
    private EventQueueManager schedulerQueue;
    private EventQueueManager fireIncidentQueue;
    private EventQueueManager droneQueue;
    private Scheduler scheduler;
    private Thread schedulerThread;
    private DroneSubsystem droneSubsystem;
    private Thread droneThread;

    @BeforeEach
    void setup() {
        schedulerQueue = new EventQueueManager("Scheduler Queue");
        fireIncidentQueue = new EventQueueManager("Fire Incident Queue");
        droneQueue = new EventQueueManager("Drone Queue");

        Map<Integer, Map.Entry<DroneState, EventQueueManager>> droneMap = new HashMap<>();
        // Initialize with valid coordinates and sufficient battery
        DroneState droneState = new DroneState(
                DroneStatus.IDLE,
                1,
                new Point2D.Double(0, 0), // Add initial coordinates
                1000,  // Extended flight time
                100    // Sufficient water
        );
        droneMap.put(1, Map.entry(droneState, droneQueue));

        scheduler = new Scheduler(schedulerQueue, fireIncidentQueue, droneMap);
        schedulerThread = new Thread(scheduler);
        schedulerThread.start();

        droneSubsystem = new DroneSubsystem(droneQueue, fireIncidentQueue);
        droneThread = new Thread(droneSubsystem);
        droneThread.start();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        schedulerQueue.put(new IncidentEvent("", 0, "EVENTS_DONE", "LOW"));
        schedulerThread.join(2000);  // Increased timeout
        droneThread.join(2000);
    }

    @Test
    @DisplayName("Test message flow from scheduler to fire subsystem")
    void testMessageTransferFlow() throws InterruptedException {
        // First register a fire zone
        schedulerQueue.put(new ZoneEvent(10, "(10.0,20.0)", "(30.0,40.0)"));        IncidentEvent fireEvent = new IncidentEvent("24:24:24", 10, "FIRE_DETECTED", "MODERATE");
        schedulerQueue.put(fireEvent);

        // Verify dispatched event
        IncidentEvent processedEvent = (IncidentEvent) fireIncidentQueue.get();
        assertNotNull(processedEvent, "No event processed");
        assertEquals(EventType.DRONE_DISPATCHED, processedEvent.getEventType(), "Incorrect event type");
    }

    @Test
    @DisplayName("Test scheduler shutdown")
    void testStopScheduler() throws InterruptedException {
        // Send shutdown command
        schedulerQueue.put(new IncidentEvent("", 0, "EVENTS_DONE", "LOW"));

        // Wait for termination
        schedulerThread.join(2000);

        // Verify thread stopped
        assertFalse(schedulerThread.isAlive(), "Scheduler thread did not terminate");
        assertTrue(fireIncidentQueue.isEmpty(), "Fire queue not cleared");
    }
}