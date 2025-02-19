package test;


import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import main.EventQueueManager;
import events.IncidentEvent;

public class EventQueueManagerTest {
    private EventQueueManager queueManager;
    private IncidentEvent event;

    @BeforeEach
    public void setup() {
        // Create a new EventQueueManager for testing.
        queueManager = new EventQueueManager("Test Queue");
        // Create a sample event.
        event = new IncidentEvent("24:24:24",10,"FIRE_DETECTED","MODERATE");
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
}