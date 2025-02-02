package test;

import events.*;
import main.EventQueueManager;
import org.junit.jupiter.api.*;
import org.opentest4j.AssertionFailedError;
import subsystems.FireIncidentSubsystem;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.reflect.Field;


import static org.junit.jupiter.api.Assertions.*;

class FireIncidentSubsystemTest {
    private FireIncidentSubsystem fireIncidentSubsystem;
    private Thread fireIncidentSubsystemThread;
    private EventQueueManager receiveEventManager;
    private EventQueueManager sendEventManager;
    private Path tempDir;
    private File tempZoneFile;
    private File tempEventFile;

    @BeforeEach
    void setUp() throws IOException {
        receiveEventManager = new EventQueueManager("Receiving Queue");
        sendEventManager = new EventQueueManager("Sending Queue");


        // create a temporary directory for input files
        tempDir = Files.createTempDirectory("testInputDir");

        // create zone file inside temp directory
        tempZoneFile = new File(tempDir.toFile(), "zone.csv");
        Files.write(tempZoneFile.toPath(),
                ("""
                ZoneID,StartCoord,EndCoord
                1,(0;0),(0;600)
                2,(0;600),(650;1500)
                """).getBytes(),
                StandardOpenOption.CREATE
        );

        // create event file inside temp directory
        tempEventFile = new File(tempDir.toFile(), "events.csv");
        Files.write(tempEventFile.toPath(),
                ("""
                Time,Zone ID,Event type,Severity
                14:03:15,1,FIRE_DETECTED,High
                14:10:00,2,DRONE_REQUEST,Moderate
                14:15:00,3,DRONE_REQUEST,Moderate
                """).getBytes(),
                StandardOpenOption.CREATE
        );

        fireIncidentSubsystem = new FireIncidentSubsystem(receiveEventManager, sendEventManager, tempDir.toString());
        fireIncidentSubsystemThread = new Thread(new FireIncidentSubsystem(receiveEventManager, sendEventManager, tempDir.toString()));
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempZoneFile.toPath());
        Files.deleteIfExists(tempEventFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    @DisplayName("Testing private method parseZones()")
    void testParseZones() throws Exception {
        Method parseZonesMethod = FireIncidentSubsystem.class.getDeclaredMethod("parseZones"); // get thee private method parseZones()
        parseZonesMethod.setAccessible(true); // make it accessible
        parseZonesMethod.invoke(fireIncidentSubsystem); // call the method

        // get the zones attribute, make it accesible and cast to Hashmap
        Field zonesField = fireIncidentSubsystem.getClass().getDeclaredField("zones");
        zonesField.setAccessible(true);
        HashMap<Integer, ArrayList<String>> zones = (HashMap<Integer, ArrayList<String>>) zonesField.get(fireIncidentSubsystem);

        // make sure zone file was parsed correctly
        assertEquals(2, zones.size());
        assertEquals("(0;0)", zones.get(1).get(0));
        assertEquals("(0;600)", zones.get(1).get(1));
        assertEquals("(0;600)", zones.get(2).get(0));
        assertEquals("(650;1500)", zones.get(2).get(1));
    }

    @Test
    @DisplayName("Testing run() method (which tests private method parseEvents())")
    void testRun() throws Exception {
        // start the thread (calls parseEvents())
        this.fireIncidentSubsystemThread.start();

        // make sure event that was added matches our test file
        IncidentEvent event1 = sendEventManager.get();
        assertNotNull(event1, "Is a valid Event, shouldn't be null");
        assertEquals("14:03:15", event1.getTimestamp());
        assertEquals(1, event1.getZoneId());
        assertEquals(EventType.FIRE_DETECTED, event1.getEventType());
        assertEquals(Severity.HIGH, event1.getSeverity());
        assertEquals(new AbstractMap.SimpleEntry<>(0, 0), event1.getStartCoordinates());
        assertEquals(new AbstractMap.SimpleEntry<>(0, 600), event1.getEndCoordinates());

        //simulate scheduler sending back a response so were not busy waiting
        event1.setReceiver("FireIncidentSubsystem");
        receiveEventManager.put(event1);

        // get the second event and make sure it matches our test event
        IncidentEvent event2 = sendEventManager.get();
        assertNotNull(event2, "Second event should not be null");
        assertEquals("14:10:00", event2.getTimestamp());
        assertEquals(2, event2.getZoneId());
        assertEquals(EventType.DRONE_REQUEST, event2.getEventType());
        assertEquals(Severity.MODERATE, event2.getSeverity());
        assertEquals(new AbstractMap.SimpleEntry<>(0, 600), event2.getStartCoordinates());
        assertEquals(new AbstractMap.SimpleEntry<>(650, 1500), event2.getEndCoordinates());

        //simulate scheduler sending back a response so were not busy waiting
        event2.setReceiver("FireIncidentSubsystem");
        receiveEventManager.put(event2);

        //event 3 should not have been sent, instead, end of events file was reached so send EVENTS_DONE event type
        IncidentEvent endEvent = sendEventManager.get();
        assertEquals(EventType.EVENTS_DONE, endEvent.getEventType());
    }

}
