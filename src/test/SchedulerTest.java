package test;

import main.Scheduler;
import org.junit.jupiter.api.*;
import subsystems.fire_incident.events.ZoneEvent;
import subsystems.drone.DroneInfo;

import java.awt.geom.Point2D;
import java.net.InetAddress;
import java.util.Map;
import java.util.HashMap;


import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchedulerTest {
    private Scheduler scheduler;

    @BeforeEach
    void setUp() throws Exception {
        Map<Integer, DroneInfo> dronesInfo = new HashMap<>();
        Map<Integer, InetAddress> droneAddresses = new HashMap<>();
        Map<Integer, Integer> dronePorts = new HashMap<>();

        scheduler = new Scheduler(InetAddress.getLocalHost(), 7000);
    }


    @AfterEach
    void tearDown() {
        scheduler.close();
    }

    /**
     * Test 1: Ensure fire zone data is stored correctly.
     */
    @Test
    void testStoreFireZoneData() throws Exception {
        ZoneEvent zoneEvent = new ZoneEvent(1, "(10;20)", "(30;40)");

        java.lang.reflect.Method method = Scheduler.class.getDeclaredMethod("storeZoneData", ZoneEvent.class);
        method.setAccessible(true);
        method.invoke(scheduler, zoneEvent);

        java.lang.reflect.Field fireZonesField = Scheduler.class.getDeclaredField("fireZones");
        fireZonesField.setAccessible(true);

        Map<Integer, Point2D> fireZones = (Map<Integer, Point2D>) fireZonesField.get(scheduler);

        assertEquals(new Point2D.Double(20, 30), fireZones.get(1), "Fire zone should be stored correctly.");
    }


    /**
     * Test 2: Ensure a drone can be assigned to an incident.
     */
//    @Test
//    void testAssignDroneToIncident() throws Exception {
//        IncidentEvent incident = new IncidentEvent("", 5, EventType.FIRE_DETECTED, Severity.HIGH);
//        DroneInfo droneInfo = new DroneInfo(InetAddress.getLocalHost(), 5000);
//
//        java.lang.reflect.Field fireZonesField = Scheduler.class.getDeclaredField("fireZones");
//        fireZonesField.setAccessible(true);
//        Map<Integer, Point2D> fireZones = (Map<Integer, Point2D>) fireZonesField.get(scheduler);
//        fireZones.put(incident.getZoneID(), new Point2D.Double(10, 10));  // Add fire zone manually
//
//        java.lang.reflect.Field dronesInfoField = Scheduler.class.getDeclaredField("dronesInfo");
//        dronesInfoField.setAccessible(true);
//        Map<Integer, DroneInfo> dronesInfo = (Map<Integer, DroneInfo>) dronesInfoField.get(scheduler);
//        dronesInfo.put(1, droneInfo);
//
//        java.lang.reflect.Method assignDroneMethod = Scheduler.class.getDeclaredMethod("assignDrone", int.class);
//        assignDroneMethod.setAccessible(true);
//
//        boolean assigned = (boolean) assignDroneMethod.invoke(scheduler, incident.getZoneID());
//
//        assertTrue(assigned, "Drone should be assigned.");
//    }




    /**
     * Test 3: Ensure incidents are queued when no drone is available.
     */
//    @Test
//    void testHandleIncidentEventWithoutAvailableDrone() throws Exception {
//        IncidentEvent incident = new IncidentEvent("", 20, EventType.FIRE_DETECTED, Severity.MODERATE);
//
//        java.lang.reflect.Method handleIncidentMethod = Scheduler.class.getDeclaredMethod("handleIncidentEvent", IncidentEvent.class);
//        handleIncidentMethod.setAccessible(true);
//        handleIncidentMethod.invoke(scheduler, incident);
//
//        java.lang.reflect.Field unassignedIncidentsField = Scheduler.class.getDeclaredField("unassignedIncidents");
//        unassignedIncidentsField.setAccessible(true);
//        Queue<IncidentEvent> unassignedIncidents = (Queue<IncidentEvent>) unassignedIncidentsField.get(scheduler);
//
//        assertEquals(1, unassignedIncidents.size(), "Incident should be stored when no drone is available.");
//        assertTrue(unassignedIncidents.contains(incident));
//    }

    /**
     * Test 4: Ensure drone arrival is handled correctly.
     */
//    @Test
//    void testHandleDroneArrival() throws Exception {
//        int droneID = 1;
//        int zoneID = 10;
//        DroneArrivedEvent event = new DroneArrivedEvent(droneID, zoneID);
//
//        java.lang.reflect.Field droneAssignmentsField = Scheduler.class.getDeclaredField("droneAssignments");
//        droneAssignmentsField.setAccessible(true);
//        Map<Integer, Integer> droneAssignments = (Map<Integer, Integer>) droneAssignmentsField.get(scheduler);
//        droneAssignments.put(droneID, zoneID);
//
//        java.lang.reflect.Method handleDroneArrivalMethod = Scheduler.class.getDeclaredMethod("handleDroneArrival", DroneArrivedEvent.class);
//        handleDroneArrivalMethod.setAccessible(true);
//        handleDroneArrivalMethod.invoke(scheduler, event);
//
//        assertTrue(droneAssignments.containsKey(droneID), "Drone should remain assigned until water drop.");
//    }
//
//
//    /**
//     * Test 5: Ensure drop agent event updates water requirements.
//     */
//    @Test
//    void testHandleDropAgentEvent() throws Exception {
//        int droneID = 1;
//        int zoneID = 10;
//        DropAgentEvent dropEvent = new DropAgentEvent(10, droneID);
//
//        java.lang.reflect.Field droneAssignmentsField = Scheduler.class.getDeclaredField("droneAssignments");
//        droneAssignmentsField.setAccessible(true);
//        Map<Integer, Integer> droneAssignments = (Map<Integer, Integer>) droneAssignmentsField.get(scheduler);
//        droneAssignments.put(droneID, zoneID);
//        java.lang.reflect.Field fireIncidentsField = Scheduler.class.getDeclaredField("fireIncidents");
//        fireIncidentsField.setAccessible(true);
//        Map<Integer, Integer> fireIncidents = (Map<Integer, Integer>) fireIncidentsField.get(scheduler);
//        fireIncidents.put(zoneID, 20);
//
//        java.lang.reflect.Method handleDropAgentMethod = Scheduler.class.getDeclaredMethod("handleDropAgent", DropAgentEvent.class);
//        handleDropAgentMethod.setAccessible(true);
//        handleDropAgentMethod.invoke(scheduler, dropEvent);
//
//
//        assertEquals(10, fireIncidents.get(zoneID), "Fire incident should have updated water requirement.");
//    }

}