package test;

import logger.EventLogger;
import main.EventSocket;
import main.Scheduler;
import org.junit.jupiter.api.*;
import subsystems.Event;
import subsystems.EventType;
import subsystems.drone.events.*;
import subsystems.drone.states.FaultedState;
import subsystems.fire_incident.*;
import subsystems.fire_incident.events.*;
import subsystems.drone.DroneInfo;

import java.awt.geom.Point2D;
import java.net.InetAddress;
import java.util.Map;
import java.util.PriorityQueue;


import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SchedulerTest {
    private Scheduler scheduler;
    private InetAddress localhost;
    private EventSocket droneSocket;
    private static final int SCHEDULER_PORT = 5000;

    @BeforeEach
    void setUp() throws Exception {
        localhost = InetAddress.getLocalHost();
        scheduler = new Scheduler(localhost, 7000);
        droneSocket = new EventSocket();
    }


    @AfterEach
    void tearDown() {
        droneSocket.close();
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
    @Test
    void testAssignDroneToIncident() throws Exception {
        IncidentEvent incident = new IncidentEvent("", 5, EventType.FIRE_DETECTED, Severity.HIGH, Faults.NONE);
        DroneInfo droneInfo = new DroneInfo(InetAddress.getLocalHost(), 5000);

        java.lang.reflect.Field fireZonesField = Scheduler.class.getDeclaredField("fireZones");
        fireZonesField.setAccessible(true);
        Map<Integer, Point2D> fireZones = (Map<Integer, Point2D>) fireZonesField.get(scheduler);
        fireZones.put(incident.getZoneID(), new Point2D.Double(10, 10));  // Add fire zone manually

        java.lang.reflect.Field dronesInfoField = Scheduler.class.getDeclaredField("dronesInfo");
        dronesInfoField.setAccessible(true);
        Map<Integer, DroneInfo> dronesInfo = (Map<Integer, DroneInfo>) dronesInfoField.get(scheduler);
        dronesInfo.put(1, droneInfo);

        java.lang.reflect.Method assignDroneMethod = Scheduler.class.getDeclaredMethod("assignDrone", IncidentEvent.class);
        assignDroneMethod.setAccessible(true);

        boolean assigned = (boolean) assignDroneMethod.invoke(scheduler, incident);

        assertTrue(assigned, "Drone should be assigned.");
    }




    /**
     * Test 3: Ensure incidents are queued when no drone is available.
     */
    @Test
    void testHandleIncidentEventWithoutAvailableDrone() throws Exception {
        IncidentEvent incident = new IncidentEvent("", 20, EventType.FIRE_DETECTED, Severity.MODERATE, Faults.NONE);

        java.lang.reflect.Field fireZonesField = Scheduler.class.getDeclaredField("fireZones");
        fireZonesField.setAccessible(true);
        Map<Integer, Point2D> fireZones = (Map<Integer, Point2D>) fireZonesField.get(scheduler);
        fireZones.put(incident.getZoneID(), new Point2D.Double(10, 10));  // Add fire zone manually

        java.lang.reflect.Method handleIncidentMethod = Scheduler.class.getDeclaredMethod("handleIncidentEvent", IncidentEvent.class);
        handleIncidentMethod.setAccessible(true);
        handleIncidentMethod.invoke(scheduler, incident);

        java.lang.reflect.Field unassignedIncidentsField = Scheduler.class.getDeclaredField("unassignedIncidents");
        unassignedIncidentsField.setAccessible(true);
        PriorityQueue<IncidentEvent> unassignedIncidents = (PriorityQueue<IncidentEvent>) unassignedIncidentsField.get(scheduler);

        assertEquals(1, unassignedIncidents.size(), "Incident should be stored when no drone is available.");
        assertTrue(unassignedIncidents.contains(incident));
    }

    /**
     * Test 4: Ensure drone arrival is handled correctly.
     */
    @Test
    void testHandleDroneArrival() throws Exception {
        int droneID = 1;
        int zoneID = 10;

        DroneInfo droneInfo = new DroneInfo(InetAddress.getLocalHost(), 5000);
        DroneArrivedEvent event = new DroneArrivedEvent(droneID, zoneID);
        IncidentEvent incident = new IncidentEvent("", zoneID, EventType.FIRE_DETECTED, Severity.MODERATE, Faults.NONE);

        java.lang.reflect.Field dronesInfoField = Scheduler.class.getDeclaredField("dronesInfo");
        dronesInfoField.setAccessible(true);
        Map<Integer, DroneInfo> dronesInfo = (Map<Integer, DroneInfo>) dronesInfoField.get(scheduler);
        dronesInfo.put(droneID, droneInfo);

        java.lang.reflect.Field droneAssignmentsField = Scheduler.class.getDeclaredField("droneAssignments");
        droneAssignmentsField.setAccessible(true);
        Map<Integer, IncidentEvent> droneAssignments = (Map<Integer, IncidentEvent>) droneAssignmentsField.get(scheduler);
        droneAssignments.put(droneID, incident);

        java.lang.reflect.Method handleDroneArrivalMethod = Scheduler.class.getDeclaredMethod("handleDroneArrival", DroneArrivedEvent.class);
        handleDroneArrivalMethod.setAccessible(true);
        handleDroneArrivalMethod.invoke(scheduler, event);

        assertTrue(droneAssignments.containsKey(droneID), "Drone should remain assigned until water drop.");
    }


    /**
     * Test 5: Ensure drop agent event updates water requirements.
     */
    @Test
    void testHandleDropAgentEvent() throws Exception {
        int droneID = 1;
        int zoneID = 10;

        DropAgentEvent dropEvent = new DropAgentEvent(10, droneID);
        IncidentEvent incident = new IncidentEvent("", zoneID, EventType.FIRE_DETECTED, Severity.MODERATE, Faults.NONE);

        java.lang.reflect.Field droneAssignmentsField = Scheduler.class.getDeclaredField("droneAssignments");
        droneAssignmentsField.setAccessible(true);
        Map<Integer, IncidentEvent> droneAssignments = (Map<Integer, IncidentEvent>) droneAssignmentsField.get(scheduler);
        droneAssignments.put(droneID, incident);

        java.lang.reflect.Method handleDropAgentMethod = Scheduler.class.getDeclaredMethod("handleDropAgent", DropAgentEvent.class);
        handleDropAgentMethod.setAccessible(true);
        handleDropAgentMethod.invoke(scheduler, dropEvent);

        assertEquals(10, incident.getWaterFoamAmount(), "Fire incident should have updated water requirement.");
    }

    @Test
    public void testRegisterDrone() throws Exception {
        // Start drone thread
        Thread schedulerThread = new Thread(() -> {
            try {
                scheduler.run();
            } catch (Exception e) {
                System.out.println("Drone thread exited: " + e.getClass().getSimpleName());
            }
        });

        schedulerThread.start();

        // Register drone
        DroneInfo droneInfo = new DroneInfo(localhost, droneSocket.getSocket().getLocalPort());
        droneSocket.send(new DroneUpdateEvent(droneInfo), localhost, SCHEDULER_PORT);

        DroneUpdateEvent event = (DroneUpdateEvent) droneSocket.receive();
        assertNotEquals(-1, event.getDroneInfo().getDroneID());

        IncidentEvent noMoreIncidents = new IncidentEvent("", 0, EventType.EVENTS_DONE, Severity.NONE, Faults.NONE);
        new EventSocket().send(noMoreIncidents, localhost, SCHEDULER_PORT);

        schedulerThread.join(1000);
    }

    @Test
    public void testDroneFault() throws Exception {
        // Start drone thread
        Thread schedulerThread = new Thread(() -> {
            try {
                scheduler.run();
            } catch (Exception e) {
                System.out.println("Drone thread exited: " + e.getClass().getSimpleName());
            }
        });

        schedulerThread.start();

        // Register drone
        DroneInfo droneInfo = new DroneInfo(localhost, droneSocket.getSocket().getLocalPort());
        droneSocket.send(new DroneUpdateEvent(droneInfo), localhost, SCHEDULER_PORT);

        DroneUpdateEvent event = (DroneUpdateEvent) droneSocket.receive();
        droneInfo = event.getDroneInfo();
        assertNotEquals(-1, droneInfo.getDroneID());

        // Add zone and create incident
        ZoneEvent zoneEvent = new ZoneEvent(1, "(10;20)", "(30;40)");
        new EventSocket().send(zoneEvent, localhost, SCHEDULER_PORT);

        IncidentEvent incident = new IncidentEvent("", 1, EventType.FIRE_DETECTED, Severity.HIGH, Faults.NOZZLE_JAMMED);
        new EventSocket().send(incident, localhost, SCHEDULER_PORT);

        // Check that drone is dispatched
        Event event1 = droneSocket.receive();
        assertInstanceOf(DroneDispatchEvent.class, event1);

        DroneDispatchEvent event2 = (DroneDispatchEvent) event1;
        assertEquals(1, event2.getZoneID());

        // Send drone fault
        droneInfo.setState(new FaultedState(Faults.NOZZLE_JAMMED));
        droneSocket.send(new DroneUpdateEvent(droneInfo), localhost, SCHEDULER_PORT);

        // Check that scheduler handles fault
        event1 = droneSocket.receive();
        assertInstanceOf(DroneDispatchEvent.class, event1);
        event2 = (DroneDispatchEvent) event1;
        assertEquals(0, event2.getZoneID());

        IncidentEvent noMoreIncidents = new IncidentEvent("", 0, EventType.EVENTS_DONE, Severity.NONE, Faults.NONE);
        new EventSocket().send(noMoreIncidents, localhost, SCHEDULER_PORT);

        schedulerThread.join(1000);
    }

}