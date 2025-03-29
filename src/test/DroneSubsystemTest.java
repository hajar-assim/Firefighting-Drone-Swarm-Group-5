package test;

import main.EventSocket;
import subsystems.Event;
import subsystems.drone.DroneInfo;
import subsystems.drone.DroneSubsystem;
import subsystems.drone.events.*;
import subsystems.drone.states.DroneState;
import java.awt.geom.Point2D;
import java.net.InetAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import subsystems.drone.states.FaultedState;
import subsystems.drone.states.IdleState;
import subsystems.drone.states.OnRouteState;
import subsystems.fire_incident.Faults;

import static org.junit.Assert.*;

public class DroneSubsystemTest {
    private DroneSubsystem drone;
    private EventSocket schedulerSocket;
    private static final int SCHEDULER_PORT = 5000;
    private InetAddress localhost;

    @Before
    public void setUp() throws Exception {
        localhost = InetAddress.getLocalHost();
        drone = new DroneSubsystem(localhost, SCHEDULER_PORT);
        schedulerSocket = new EventSocket(SCHEDULER_PORT);
    }


    @After
    public void tearDown() {
        // First stop the drone if still running
        drone.setRunning(false);

        // Then close sockets
        schedulerSocket.close();
        drone.getSocket().close();
    }

    //methods to test

    @Test
    public void testGetDroneID() {
        assertEquals(drone.getDroneInfo().getDroneID(), drone.getDroneID());
    }

    @Test
    public void testGetRecieveSocket() {
        assertNotNull(drone.getSocket());
    }

    @Test
    public void testGetSendSocket() {
        assertNotNull(drone.getSocket());
    }

    @Test
    public void testGetSchedulerAddress() {
        assertEquals(localhost, drone.getSchedulerAddress());
    }

    @Test
    public void testGetSchedulerPort() {
        assertEquals(SCHEDULER_PORT, drone.getSchedulerPort());
    }

    @Test
    public void testGetDroneInfo() {
        assertNotNull(drone.getDroneInfo());
    }

    @Test
    public void testSetDroneInfo() {
        DroneInfo newInfo = new DroneInfo(localhost, 4000);
        newInfo.setZoneID(999);
        drone.setDroneInfo(newInfo);
        assertEquals(999, drone.getDroneInfo().getZoneID());
    }

    @Test
    public void testGetState() {
        assertNotNull(drone.getState());
    }

    @Test
    public void testSetState() throws Exception {
        IdleState testState = new IdleState();
        drone.setState(testState);
        Event receivedEvent = schedulerSocket.receive();
        assertTrue(receivedEvent instanceof DroneUpdateEvent);
    }

    @Test
    public void testGetZoneID() {
        assertEquals(0, drone.getZoneID()); // Default value
    }

    @Test
    public void testSetZoneID() {
        drone.setZoneID(5);
        assertEquals(5, drone.getZoneID());
    }

    @Test
    public void testGetCoordinates() {
        assertNotNull(drone.getCoordinates());
    }

    @Test
    public void testSetCoordinates() throws Exception {
        drone.setZoneID(5);
        Point2D coords = new Point2D.Double(100, 200);
        drone.setCoordinates(coords);
        Event event = schedulerSocket.receive();
        assertTrue(event instanceof DroneArrivedEvent);
    }

    @Test
    public void testGetFlightTime() {
        assertTrue(drone.getFlightTime() >= 0);
    }

    @Test
    public void testSetFlightTime() {
        drone.setFlightTime(120.5);
        assertEquals(120.5, drone.getFlightTime(), 0.001);
    }

    @Test
    public void testGetWaterLevel() {
        assertTrue(drone.getWaterLevel() >= 0);
    }

    @Test
    public void testSetWaterLevel() {
        drone.setWaterLevel(85);
        assertEquals(85, drone.getWaterLevel());
    }

    @Test
    public void testSubtractWaterLevel() throws Exception {
        int initial = drone.getWaterLevel();
        drone.subtractWaterLevel(15);
        assertEquals(initial - 15, drone.getWaterLevel());
        Event event = schedulerSocket.receive();
        assertTrue(event instanceof DropAgentEvent);
    }

    @Test
    public void testGetRunning() {
        assertFalse(drone.getRunning());
    }

    @Test
    public void testSetRunning() {
        drone.setRunning(true);
        assertTrue(drone.getRunning());
    }

    @Test
    public void testTimeToZone() {
        Point2D start = new Point2D.Double(0, 0);
        Point2D end = new Point2D.Double(100, 0);
        double expected = ((100 - 46.875) / 15) + 6.25;
        assertEquals(expected, DroneSubsystem.timeToZone(start, end), 0.001);
    }

    @Test
    public void testRun() throws Exception {
        // Start drone thread
        Thread droneThread = new Thread(() -> {
            try {
                drone.run();
            } catch (Exception e) {
                System.out.println("Drone thread exited: " + e.getClass().getSimpleName());
            }
        });

        droneThread.start();

        // Send an event to register drone
        DroneInfo droneInfo = drone.getDroneInfo();
        int droneID = 1;
        droneInfo.setDroneID(droneID);
        int droneReceivePort = droneInfo.getPort();
        new EventSocket().send(new DroneUpdateEvent(droneInfo), localhost, droneReceivePort);
        Thread.sleep(200);

        assertTrue("Drone should be running", drone.getRunning());

        // Send a drone dispatch to base event to stop drone
        new EventSocket().send(new DroneDispatchEvent(0, new Point2D.Double(0,0), Faults.NONE), localhost, droneReceivePort);
        Thread.sleep(200);

        assertFalse("Drone should be stopped", drone.getRunning());
        droneThread.join(1000);
    }

    @Test
    public void testStuckFault() throws Exception {
        // Start drone thread
        Thread droneThread = new Thread(() -> {
            try {
                drone.run();
            } catch (Exception e) {
                System.out.println("Drone thread exited: " + e.getClass().getSimpleName());
            }
        });

        droneThread.start();

        // Send an event to register drone
        DroneInfo droneInfo = drone.getDroneInfo();
        int droneID = 1;
        droneInfo.setDroneID(droneID);
        int droneReceivePort = droneInfo.getPort();
        new EventSocket().send(new DroneUpdateEvent(droneInfo), localhost, droneReceivePort);

        DroneUpdateEvent update = (DroneUpdateEvent) schedulerSocket.receive();
        assertTrue(update.getDroneInfo().getState() instanceof IdleState);

        // Send fault event
        new EventSocket().send(new DroneDispatchEvent(1, new Point2D.Double(1,1), Faults.DRONE_STUCK_IN_FLIGHT), localhost, droneReceivePort);

        // Check that drone is flying
        update = (DroneUpdateEvent) schedulerSocket.receive();
        assertTrue(update.getDroneInfo().getState() instanceof OnRouteState);

        // Check that drone has faulted
        update = (DroneUpdateEvent) schedulerSocket.receive();
        assertTrue(update.getDroneInfo().getState() instanceof FaultedState);


        // Send a drone dispatch to base event to stop drone
        new EventSocket().send(new DroneDispatchEvent(0, new Point2D.Double(0,0), Faults.NONE), localhost, droneReceivePort);
        Thread.sleep(200);
        assertFalse("Drone should be stopped", drone.getRunning());
        droneThread.join(1000);
    }

    @Test
    public void testJamFault() throws Exception {
        // Start drone thread
        Thread droneThread = new Thread(() -> {
            try {
                drone.run();
            } catch (Exception e) {
                System.out.println("Drone thread exited: " + e.getClass().getSimpleName());
            }
        });

        droneThread.start();

        // Send an event to register drone
        DroneInfo droneInfo = drone.getDroneInfo();
        int droneID = 1;
        droneInfo.setDroneID(droneID);
        int droneReceivePort = droneInfo.getPort();
        new EventSocket().send(new DroneUpdateEvent(droneInfo), localhost, droneReceivePort);

        DroneUpdateEvent update = (DroneUpdateEvent) schedulerSocket.receive();
        assertTrue(update.getDroneInfo().getState() instanceof IdleState);

        // Send fault event
        new EventSocket().send(new DroneDispatchEvent(1, new Point2D.Double(1,1),  Faults.NOZZLE_JAMMED), localhost, droneReceivePort);

        // Check that drone is flying
        update = (DroneUpdateEvent) schedulerSocket.receive();
        assertTrue(update.getDroneInfo().getState() instanceof OnRouteState);

        // Check that done has faulted
        update = (DroneUpdateEvent) schedulerSocket.receive();
        assertTrue(update.getDroneInfo().getState() instanceof FaultedState);

        // Send a drone stop event
        new EventSocket().send(new DroneDispatchEvent(0, new Point2D.Double(0,0), Faults.NONE), localhost, droneReceivePort);
        Thread.sleep(200);
        System.out.println(drone.getRunning());
        assertFalse("Drone should be stopped", drone.getRunning());

        droneThread.join(1000);
    }
}