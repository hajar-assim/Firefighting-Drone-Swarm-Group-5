package test;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import main.EventQueueManager;
import main.Scheduler;
import events.IncidentEvent;
import subsystems.DroneSubsystem;

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

        scheduler = new Scheduler(schedulerQueue, fireIncidentQueue, droneQueue);
        schedulerThread = new Thread(scheduler);
        schedulerThread.start();

        droneSubsystem = new DroneSubsystem(droneQueue, schedulerQueue);
        droneThread = new Thread(droneSubsystem);
        droneThread.start();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // Send an EVENTS_DONE event to signal end of simulation
        schedulerQueue.put(new IncidentEvent("", 0, "EVENTS_DONE", "LOW"));
        schedulerThread.join(1000);
        droneThread.join(1000);
    }

    @Test
    @DisplayName("Test for correct flow of message from scheduler to the fire incident subsystem")
    void testMessageTransferFlow() {
        IncidentEvent fireEvent = new IncidentEvent("24:24:24",10,"FIRE_DETECTED","MODERATE");

        // Place the event
        schedulerQueue.put(fireEvent);
        IncidentEvent processedEvent = (IncidentEvent) fireIncidentQueue.get();

        // Verify that the drone subsystem has processed the event as expected
        assertEquals(fireEvent, processedEvent);
    }

    @Test
    @DisplayName("Test that scheduler stops running when EVENTS_DONE event is sent")
    void testStopScheduler() throws InterruptedException {
        // Send the EVENTS_DONE event
        schedulerQueue.put(new IncidentEvent("", 0, "EVENTS_DONE", "LOW"));
        schedulerThread.join(1000);

        // Make sure that scheduler thread has stopped running
        assertFalse(schedulerThread.isAlive());
    }
}
