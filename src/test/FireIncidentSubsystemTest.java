//package test;
//
//import subsystems.EventType;
//import org.junit.jupiter.api.*;
//import subsystems.fire_incident.FireIncidentSubsystem;
//import subsystems.fire_incident.events.IncidentEvent;
//import subsystems.fire_incident.events.Severity;
//import subsystems.fire_incident.events.ZoneEvent;
//
//import java.awt.geom.Point2D;
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.StandardOpenOption;
//
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class FireIncidentSubsystemTest {
//    private FireIncidentSubsystem fireIncidentSubsystem;
//    private Thread fireIncidentSubsystemThread;
//    private EventQueueManager receiveEventManager;
//    private EventQueueManager sendEventManager;
//    private Path tempDir;
//    private File tempZoneFile;
//    private File tempEventFile;
//
//    @BeforeEach
//    void setUp() throws IOException {
//        receiveEventManager = new EventQueueManager("Receiving Queue");
//        sendEventManager = new EventQueueManager("Sending Queue");
//
//
//        // create a temporary directory for input files
//        tempDir = Files.createTempDirectory("testInputDir");
//
//        // create zone file inside temp directory
//        tempZoneFile = new File(tempDir.toFile(), "zone.csv");
//        Files.write(tempZoneFile.toPath(),
//                ("""
//                ZoneID,StartCoord,EndCoord
//                1,(0;0),(0;600)
//                2,(0;600),(650;1500)
//                """).getBytes(),
//                StandardOpenOption.CREATE
//        );
//
//        // create event file inside temp directory
//        tempEventFile = new File(tempDir.toFile(), "events.csv");
//        Files.write(tempEventFile.toPath(),
//                ("""
//                Time,Zone ID,Event type,Severity
//                14:03:15,1,FIRE_DETECTED,High
//                14:10:00,2,DRONE_REQUEST,Moderate
//                """).getBytes(),
//                StandardOpenOption.CREATEin what departmn can we come into th world
//        );
//
//        fireIncidentSubsystem = new FireIncidentSubsystem(receiveEventManager, sendEventManager, tempDir.toString());
//        fireIncidentSubsystemThread = new Thread(new FireIncidentSubsystem(receiveEventManager, sendEventManager, tempDir.toString()));
//    }
//
//    @AfterEach
//    void tearDown() throws IOException {
//        Files.deleteIfExists(tempZoneFile.toPath());
//        Files.deleteIfExists(tempEventFile.toPath());
//        Files.deleteIfExists(tempDir);
//    }
//
//
//    @Test
//    @DisplayName("Testing run() method (which tests private method parseEvents())")
//    void testRun() throws Exception {
//        // start the thread (calls parseEvents())
//        this.fireIncidentSubsystemThread.start();
//
//        ZoneEvent event1 = (ZoneEvent) sendEventManager.get();
//        assertEquals(1, event1.getZoneID());
//        assertEquals(new Point2D.Double(0, 300), event1.getCenter());
//
//        ZoneEvent event2 = (ZoneEvent) sendEventManager.get();
//        assertEquals(2, event2.getZoneID());
//        assertEquals(new Point2D.Double(325, 1050), event2.getCenter());
//
//
//        // make sure event that was added matches our test file
//        IncidentEvent event3 = (IncidentEvent) sendEventManager.get();
//        assertNotNull(event3, "Is a valid Event, shouldn't be null");
//        assertEquals("14:03:15", event3.getTimeStamp());
//        assertEquals(1, event3.getZoneID());
//        assertEquals(EventType.FIRE_DETECTED, event3.getEventType());
//        assertEquals(Severity.HIGH, event3.getSeverity());
//
//        //simulate scheduler sending back a response so were not busy waiting
//        event3.setEventType(EventType.FIRE_EXTINGUISHED);
//        receiveEventManager.put(event3);
//
//        // get the second event and make sure it matches our test event
//        IncidentEvent event4 = (IncidentEvent) sendEventManager.get();
//        assertNotNull(event4, "Second event should not be null");
//        assertEquals("14:10:00", event4.getTimeStamp());
//        assertEquals(2, event4.getZoneID());
//        assertEquals(EventType.DRONE_REQUEST, event4.getEventType());
//        assertEquals(Severity.MODERATE, event4.getSeverity());
//
//        //simulate scheduler sending back a response so were not busy waiting
//        event4.setEventType(EventType.FIRE_EXTINGUISHED);
//        receiveEventManager.put(event4);
//
//
//        //event 3 should not have been sent, instead, end of events file was reached so send EVENTS_DONE event type
//        IncidentEvent endEvent = (IncidentEvent) sendEventManager.get();
//        assertEquals(EventType.EVENTS_DONE, endEvent.getEventType());
//    }
//
//}
