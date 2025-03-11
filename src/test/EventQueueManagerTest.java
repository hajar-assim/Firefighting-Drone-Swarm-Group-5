package test;


import subsystems.EventType;
import subsystems.drone.events.DroneArrivedEvent;
import subsystems.drone.events.DropAgentEvent;
import subsystems.Event;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import main.EventQueueManager;
import subsystems.fire_incident.events.IncidentEvent;
import subsystems.fire_incident.events.Severity;

public class EventQueueManagerTest {
    private EventQueueManager queueManager;
    private IncidentEvent event;

    @BeforeEach
    public void setup() {
        // Create a new EventQueueManager for testing.
        queueManager = new EventQueueManager("Test Queue");
        // Create a sample event.
        event = new IncidentEvent("24:24:24",10, EventType.FIRE_DETECTED, Severity.MODERATE);
    }

    @Test
    @DisplayName("Test put/get")
    public void testPutAndGet() {
        // Put event in the queue
        queueManager.put(event);

        // Get event from the queue
        IncidentEvent retrievedEvent = (IncidentEvent) queueManager.get();

        // Compare both events for equality
        assertEquals(event, retrievedEvent);
    }

    // Test DroneArrivedEvent Handling
    @Test
    @DisplayName("Test Queueing and Retrieval of DroneArrivedEvent")
    public void testDroneArrivedEventQueue() {
        DroneArrivedEvent event = new DroneArrivedEvent(1, 50);
        queueManager.put(event);
        Event retrievedEvent = queueManager.get();

        assertNotNull(retrievedEvent);
        assertTrue(retrievedEvent instanceof DroneArrivedEvent);
        assertEquals(1, ((DroneArrivedEvent) retrievedEvent).getDroneID());
        assertEquals(50, ((DroneArrivedEvent) retrievedEvent).getZoneID());
    }

    // Test DropAgentEvent Handling
    @Test
    @DisplayName("Test Queueing and Retrieval of DropAgentEvent")
    public void testDropAgentEventQueue() {
        DropAgentEvent event = new DropAgentEvent(30, 2);
        queueManager.put(event);
        Event retrievedEvent = queueManager.get();

        assertNotNull(retrievedEvent);
        assertInstanceOf(DropAgentEvent.class, retrievedEvent);
        assertEquals(30, ((DropAgentEvent) retrievedEvent).getVolume());
        assertEquals(2, ((DropAgentEvent) retrievedEvent).getDroneID());
    }

}