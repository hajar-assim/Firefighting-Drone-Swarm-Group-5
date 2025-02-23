package test;

import static org.junit.jupiter.api.Assertions.*;

import events.*;
import main.EventQueueManager;
import main.Scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import subsystems.DroneState;
import subsystems.DroneStatus;
import subsystems.DroneSubsystem;

import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class SchedulerTest {

    private EventQueueManager receiveEventManager;
    private EventQueueManager fireIncidentManager;
    private EventQueueManager droneManager; // Manager for our test drone
    private DroneState droneState;
    private DroneSubsystem testDrone; // Capture the actual drone subsystem

    // The Scheduler’s drones map uses the actual Map.Entry type
    private Map<Integer, Map.Entry<DroneState, EventQueueManager>> dronesByID;
    private Scheduler scheduler;

    @BeforeEach
    public void setup() {
        dronesByID = new HashMap<>();
        receiveEventManager = new EventQueueManager("");
        fireIncidentManager = new EventQueueManager("");
        droneManager = new EventQueueManager("");

        // Create an actual DroneSubsystem.
        testDrone = new DroneSubsystem(droneManager, receiveEventManager);
        droneState = testDrone.getDroneState();

        // Use the drone's unique ID as key.
        dronesByID.put(testDrone.getDroneID(), new AbstractMap.SimpleEntry<>(droneState, droneManager));
        scheduler = new Scheduler(receiveEventManager, fireIncidentManager, dronesByID);
    }

    @Test
    public void testGetDroneManager() throws Exception {
        Method getDroneManager = Scheduler.class.getDeclaredMethod("getDroneManager", int.class);
        getDroneManager.setAccessible(true);
        Object result = getDroneManager.invoke(scheduler, testDrone.getDroneID());
        assertEquals(droneManager, result);
    }

    @Test
    public void testGetDroneState() throws Exception {
        Method getDroneState = Scheduler.class.getDeclaredMethod("getDroneState", int.class);
        getDroneState.setAccessible(true);
        Object result = getDroneState.invoke(scheduler, testDrone.getDroneID());
        assertEquals(droneState, result);
    }

    @Test
    public void testStoreZoneData() throws Exception {

        ZoneEvent zoneEvent = new ZoneEvent(10, "(0;0)", "(700;600)");
        Method storeZoneData = Scheduler.class.getDeclaredMethod("storeZoneData", ZoneEvent.class);
        storeZoneData.setAccessible(true);
        storeZoneData.invoke(scheduler, zoneEvent);

        // Access the private fireZones field to verify it was updated.
        Field fireZonesField = Scheduler.class.getDeclaredField("fireZones");
        fireZonesField.setAccessible(true);
        Map<Integer, Point2D> fireZones = (Map<Integer, Point2D>) fireZonesField.get(scheduler);
        assertTrue(fireZones.containsKey(10));
        assertEquals(new Point2D.Double(350, 300), fireZones.get(10));
    }

    @Test
    public void testHandleIncidentEventWithDroneAssigned() throws Exception {
        // Store zone data with a known zone id (10).
        ZoneEvent zoneEvent = new ZoneEvent(10, "(0;0)", "(700;600)");
        Method storeZoneData = Scheduler.class.getDeclaredMethod("storeZoneData", ZoneEvent.class);
        storeZoneData.setAccessible(true);
        storeZoneData.invoke(scheduler, zoneEvent);

        // Create an IncidentEvent for zone 10.
        IncidentEvent incidentEvent = new IncidentEvent("", 10, "FIRE_DETECTED", "HIGH");
        // When handled, the Scheduler should change the event type to "DRONE_DISPATCHED"
        // and assign the drone to zone 10.

        Method handleIncidentEvent = Scheduler.class.getDeclaredMethod("handleIncidentEvent", IncidentEvent.class);
        handleIncidentEvent.setAccessible(true);
        handleIncidentEvent.invoke(scheduler, incidentEvent);

        // Verify that the incident's type was changed to DRONE_DISPATCHED.
        assertEquals(EventType.DRONE_DISPATCHED, incidentEvent.getEventType());

        // Verify that a drone assignment was recorded.
        Field assignmentsField = Scheduler.class.getDeclaredField("droneAssignments");
        assignmentsField.setAccessible(true);
        Map<Integer, Integer> assignments = (Map<Integer, Integer>) assignmentsField.get(scheduler);
        // The scheduler should have assigned the drone (with testDrone.getDroneID())
        // to zone 10.
        assertTrue(assignments.containsKey(testDrone.getDroneID()));
        assertEquals(10, assignments.get(testDrone.getDroneID()).intValue());

        // Verify that the drone manager's queue received a DroneDispatchEvent.
        Object queuedEvent = droneManager.get();
        assertNotNull(queuedEvent);
        assertTrue(queuedEvent instanceof DroneDispatchEvent);
        DroneDispatchEvent ddEvent = (DroneDispatchEvent) queuedEvent;
        assertEquals(10, ddEvent.getZoneID());
        // The coordinates should match the center from the zone data.
        assertEquals(new Point2D.Double(350, 300), ddEvent.getCoords());
    }

    @Test
    public void testHasEnoughBattery() throws Exception {
        Method hasEnoughBattery = Scheduler.class.getDeclaredMethod("hasEnoughBattery", DroneState.class, Point2D.class);
        hasEnoughBattery.setAccessible(true);

        // For a close target, assume the drone has enough battery.
        boolean result = (boolean) hasEnoughBattery.invoke(scheduler, droneState, new Point2D.Double(5, 5));
        assertTrue(result);

        // For a far target, assume the drone does not have enough battery.
        boolean result2 = (boolean) hasEnoughBattery.invoke(scheduler, droneState, new Point2D.Double(1000000000, 1000000000));
        assertFalse(result2);
    }

    @Test
    public void testFindAvailableDrone() throws Exception {
        Method findAvailableDrone = Scheduler.class.getDeclaredMethod("findAvailableDrone", Point2D.class);
        findAvailableDrone.setAccessible(true);

        // Initially, the drone should be available.
        int foundDroneId = (int) findAvailableDrone.invoke(scheduler, new Point2D.Double(5, 5));
        assertEquals(testDrone.getDroneID(), foundDroneId);

        // Now, simulate the drone being busy.
        Field statusField = droneState.getClass().getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(droneState, DroneStatus.ON_ROUTE);

        int notFoundDroneId = (int) findAvailableDrone.invoke(scheduler, new Point2D.Double(5, 5));
        assertEquals(-1, notFoundDroneId);
    }

    @Test
    public void testHandleDroneArrival() throws Exception {
        // Set up internal fields: assign the drone (id 1) to zone 40 and record a fire incident.
        Field assignmentsField = Scheduler.class.getDeclaredField("droneAssignments");
        assignmentsField.setAccessible(true);
        Map<Integer, Integer> assignments = (Map<Integer, Integer>) assignmentsField.get(scheduler);
        assignments.put(testDrone.getDroneID(), 40);

        Field incidentsField = Scheduler.class.getDeclaredField("fireIncidents");
        incidentsField.setAccessible(true);
        Map<Integer, Integer> incidents = (Map<Integer, Integer>) incidentsField.get(scheduler);
        incidents.put(40, 80);

        // Create a DroneArrivedEvent for the test drone.
        DroneArrivedEvent arrivedEvent = new DroneArrivedEvent(testDrone.getDroneID(), 0); // Adjust constructor as needed

        Method handleDroneArrival = Scheduler.class.getDeclaredMethod("handleDroneArrival", DroneArrivedEvent.class);
        handleDroneArrival.setAccessible(true);
        handleDroneArrival.invoke(scheduler, arrivedEvent);

        // Now, check that the drone manager's queue received a DropAgentEvent.
        Object queuedEvent = droneManager.get();
        assertNotNull(queuedEvent);
        assertTrue(queuedEvent instanceof DropAgentEvent);
    }

    @Test
    public void testHandleDroneUpdate() throws Exception {
        // Clear any previous assignments and incidents.
        Field assignmentsField = Scheduler.class.getDeclaredField("droneAssignments");
        assignmentsField.setAccessible(true);
        Map<Integer, Integer> assignments = (Map<Integer, Integer>) assignmentsField.get(scheduler);
        assignments.clear();

        Field incidentsField = Scheduler.class.getDeclaredField("fireIncidents");
        incidentsField.setAccessible(true);
        Map<Integer, Integer> incidents = (Map<Integer, Integer>) incidentsField.get(scheduler);
        incidents.clear();

        // Store zone data for a zone (zone id 60).
        ZoneEvent zoneEvent = new ZoneEvent(60, "(0;600)", "(600;700)");
        Method storeZoneData = Scheduler.class.getDeclaredMethod("storeZoneData", ZoneEvent.class);
        storeZoneData.setAccessible(true);
        storeZoneData.invoke(scheduler, zoneEvent);
        // Manually record an incident for zone 60.
        incidents.put(60, 30);

        // Ensure the drone is idle by setting its status to IDLE.
        Field statusField = droneState.getClass().getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(droneState, DroneStatus.IDLE);

        // Create a DroneUpdateEvent for the test drone.
        DroneUpdateEvent updateEvent = new DroneUpdateEvent(testDrone.getDroneID(), droneState);
        Method handleDroneUpdate = Scheduler.class.getDeclaredMethod("handleDroneUpdate", DroneUpdateEvent.class);
        handleDroneUpdate.setAccessible(true);
        handleDroneUpdate.invoke(scheduler, updateEvent);

        // Verify that the droneAssignments now contains an assignment for the drone.
        assertTrue(assignments.containsKey(testDrone.getDroneID()));
        // The expected assignment is to zone 60.
        assertEquals(60, assignments.get(testDrone.getDroneID()).intValue());
    }
}
