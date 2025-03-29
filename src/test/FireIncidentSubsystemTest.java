//package test;
//
//import main.EventSocket;
//import org.junit.jupiter.api.*;
//import subsystems.EventType;
//import subsystems.fire_incident.FireIncidentSubsystem;
//import subsystems.fire_incident.Faults;
//import subsystems.fire_incident.events.IncidentEvent;
//import subsystems.fire_incident.Severity;
//import subsystems.fire_incident.events.ZoneEvent;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.InetAddress;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.Arrays;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class FireIncidentSubsystemTest {
//    private FireIncidentSubsystem fiss;
//    private Path tempDir;
//    private EventSocket schedulerSocket;
//    private final int SCHEDULER_PORT = 5000;
//    private final int SUBSYSTEM_RECEIVE_PORT = 7000;
//
//    @BeforeEach
//    void setUp() throws IOException {
//        tempDir = Files.createTempDirectory("test-input");
//        createZoneFile();
//        createEventFile();
//        schedulerSocket = new EventSocket(SCHEDULER_PORT);
//    }
//
//    private void createZoneFile() throws IOException {
//        File zoneFile = new File(tempDir.toFile(), "zone.csv");
//        Files.write(zoneFile.toPath(), Arrays.asList(
//                "zoneID,startCoordinates,endCoordinates",
//                "0,(0;0),(10;10)",
//                "1,(20;20),(30;30)"
//        ));
//    }
//
//    private void createEventFile() throws IOException {
//        File eventFile = new File(tempDir.toFile(), "events.csv");
//        Files.write(eventFile.toPath(), Arrays.asList(
//                "timestamp,zoneID,eventType,severity",
//                "2023-01-01T00:00:00,0,FIRE_DETECTED,HIGH"
//        ));
//    }
//
//    @AfterEach
//    void tearDown() {
//        if (fiss != null) {
//            // Close sockets to free ports
//            fiss = null;
//        }
//        if (schedulerSocket != null) {
//            schedulerSocket.close();
//        }
//    }
//
//    @Test
//    void testAllFunctionalityInOneMethod() throws Exception {
//        // ===== Setup Input Files =====
//        // Create zone file with 4 zones
//        File zoneFile = new File(tempDir.toFile(), "zone.csv");
//        Files.write(zoneFile.toPath(), Arrays.asList(
//                "zoneID,startCoordinates,endCoordinates",
//                "0,(0;0),(10;10)",
//                "1,(10;10),(20;20)",
//                "2,(20;20),(30;30)",
//                "3,(30;30),(40;40)"
//        ));
//
//        // Create event file with all severities
//        File eventFile = new File(tempDir.toFile(), "events.csv");
//        Files.write(eventFile.toPath(), Arrays.asList(
//                "timestamp,zoneID,eventType,severity",
//                "2023-01-01T00:00:00,0,FIRE_DETECTED,NONE",
//                "2023-01-01T00:00:01,1,FIRE_DETECTED,LOW",
//                "2023-01-01T00:00:02,2,FIRE_DETECTED,MODERATE",
//                "2023-01-01T00:00:03,3,FIRE_DETECTED,HIGH"
//        ));
//
//        // ===== Initialize Subsystem =====
//        fiss = new FireIncidentSubsystem(tempDir.toString(), InetAddress.getLocalHost(), SCHEDULER_PORT);
//        Thread subsystemThread = new Thread(fiss::run);
//        subsystemThread.start();
//
//        // Verify Zone Events
//        for (int zoneId = 0; zoneId < 4; zoneId++) {
//            ZoneEvent zone = (ZoneEvent) schedulerSocket.receive();
//            assertEquals(zoneId, zone.getZoneID(), "Zone ID mismatch for zone event");
//        }
//
//        // Verify Incident Events & Severities
//        Severity[] expectedSeverities = {Severity.NONE, Severity.LOW, Severity.MODERATE, Severity.HIGH};
//        for (int zoneId = 0; zoneId < 4; zoneId++) {
//            // Receive incident event
//            IncidentEvent incident = (IncidentEvent) schedulerSocket.receive();
//            Severity originalSeverity = incident.getSeverity();
//            Faults originalFault = incident.getFault();
//
//            assertEquals(zoneId, incident.getZoneID());
//            assertEquals(expectedSeverities[zoneId], originalSeverity);
//
//            // Simulate scheduler responses WITH ORIGINAL SEVERITY
//            EventSocket responseSender = new EventSocket();
//
//            // Send DRONE_DISPATCHED with original severity
//            IncidentEvent dispatchedEvent = new IncidentEvent(
//                    "", zoneId, EventType.DRONE_DISPATCHED, originalSeverity, originalFault
//            );
//            responseSender.send(dispatchedEvent, InetAddress.getLocalHost(), SUBSYSTEM_RECEIVE_PORT);
//
//            // Send FIRE_EXTINGUISHED with original severity
//            IncidentEvent extinguishedEvent = new IncidentEvent(
//                    "", zoneId, EventType.FIRE_EXTINGUISHED, originalSeverity, originalFault
//            );
//            responseSender.send(extinguishedEvent, InetAddress.getLocalHost(), SUBSYSTEM_RECEIVE_PORT);
//        }
//
//        // Verify Final EVENTS_DONE
//        IncidentEvent doneEvent = (IncidentEvent) schedulerSocket.receive();
//        assertEquals(EventType.EVENTS_DONE, doneEvent.getEventType(), "EVENTS_DONE not received");
//
//        subsystemThread.join(3000);
//        assertFalse(subsystemThread.isAlive(), "Subsystem thread did not terminate");
//    }
//}