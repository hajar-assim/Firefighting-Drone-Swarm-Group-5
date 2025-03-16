package test;

import main.EventSocket;
import org.junit.jupiter.api.*;
import subsystems.Event;
import subsystems.EventType;
import subsystems.fire_incident.events.IncidentEvent;
import subsystems.fire_incident.events.Severity;
import subsystems.fire_incident.events.ZoneEvent;

import java.awt.geom.Point2D;
import java.net.*;

import static org.junit.jupiter.api.Assertions.*;

class EventSocketTest {
    private EventSocket sender;
    private EventSocket receiver;
    private InetAddress localAddress;
    private static final int TEST_PORT = 9876;

    @BeforeEach
    void setUp() throws UnknownHostException {
        sender = new EventSocket();
        receiver = new EventSocket(TEST_PORT);
        localAddress = InetAddress.getLocalHost();
    }

    @AfterEach
    void tearDown() {
        if (sender != null) {
            sender.close();  // Close sender socket
        }
        if (receiver != null) {
            receiver.close();  // Close receiver socket
        }
        sender = null;
        receiver = null;
    }


    @Test
    void testSendAndReceiveIncidentEvent() {
        // Creating an IncidentEvent
        EventType testEventType = EventType.FIRE_DETECTED; // Replace with valid enum value
        Severity testSeverity = Severity.HIGH; // Replace with valid enum value
        IncidentEvent sentEvent = new IncidentEvent("2025-03-15T10:30:00", 5, testEventType, testSeverity);

        // Send event in a separate thread to prevent blocking
        new Thread(() -> sender.send(sentEvent, localAddress, TEST_PORT)).start();

        // Receive the event
        Event receivedEvent = receiver.receive();

        // Assertions to verify event transmission
        assertNotNull(receivedEvent, "Received event should not be null");
        assertTrue(receivedEvent instanceof IncidentEvent, "Received event should be an instance of IncidentEvent");

        // Cast to IncidentEvent to access properties
        IncidentEvent receivedIncident = (IncidentEvent) receivedEvent;

        assertEquals(sentEvent.getTimeStamp(), receivedIncident.getTimeStamp(), "Timestamps should match");
        assertEquals(sentEvent.getZoneID(), receivedIncident.getZoneID(), "Zone IDs should match");
        assertEquals(sentEvent.getEventType(), receivedIncident.getEventType(), "Event types should match");
        assertEquals(sentEvent.getSeverity(), receivedIncident.getSeverity(), "Severities should match");
    }

    @Test
    void testSendAndReceiveZoneEvent() {
        // Creating a ZoneEvent
        int zoneID = 3;
        String startCoordinates = "(10;20)";
        String endCoordinates = "(30;40)";
        ZoneEvent sentEvent = new ZoneEvent(zoneID, startCoordinates, endCoordinates);

        // Send event in a separate thread to prevent blocking
        new Thread(() -> sender.send(sentEvent, localAddress, TEST_PORT)).start();

        // Receive the event
        Event receivedEvent = receiver.receive();

        // Assertions to verify event transmission
        assertNotNull(receivedEvent, "Received event should not be null");
        assertTrue(receivedEvent instanceof ZoneEvent, "Received event should be an instance of ZoneEvent");

        // Cast to ZoneEvent to access properties
        ZoneEvent receivedZoneEvent = (ZoneEvent) receivedEvent;

        assertEquals(sentEvent.getZoneID(), receivedZoneEvent.getZoneID(), "Zone IDs should match");

        // Verify that the center coordinates match
        Point2D sentCenter = sentEvent.getCenter();
        Point2D receivedCenter = receivedZoneEvent.getCenter();
        assertEquals(sentCenter.getX(), receivedCenter.getX(), "Center X-coordinates should match");
        assertEquals(sentCenter.getY(), receivedCenter.getY(), "Center Y-coordinates should match");
    }
}
