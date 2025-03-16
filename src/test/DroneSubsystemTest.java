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
        drone.getRecieveSocket().close();
    }

    //methods to test

    @Test
    public void testGetDroneID() {
        assertEquals(drone.getDroneInfo().getDroneID(), drone.getDroneID());
    }

    @Test
    public void testGetRecieveSocket() {
        assertNotNull(drone.getRecieveSocket());
    }

    @Test
    public void testGetSendSocket() {
        assertNotNull(drone.getSendSocket());
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
        DroneInfo newInfo = new DroneInfo();
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
        DroneState testState = new TestDroneState();
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
        assertEquals(expected, drone.timeToZone(start, end), 0.001);
    }

//    @Test(timeout = 5000)
//    public void testRun() throws Exception {
//        // Start drone thread
//        Thread droneThread = new Thread(() -> {
//            try {
//                drone.run();
//            } catch (Exception e) {
//                // Expected exception during shutdown
//                System.out.println("Drone thread exited: " + e.getClass().getSimpleName());
//            }
//        });
//
//        drone.setRunning(true);
//        droneThread.start();
//
//        // Verify thread starts
//        Thread.sleep(200);
//        assertTrue("Drone should be running", drone.getRunning());
//
//        // Graceful shutdown sequence
//        drone.setRunning(false);
//
//        // Unblock the receive() call by sending a dummy packet
//        int droneID = 1; // Example drone ID
//        DroneInfo droneInfo = new DroneInfo(); // Example DroneInfo object
//        new EventSocket().send(new DroneUpdateEvent(droneID, droneInfo), localhost, SCHEDULER_PORT);
//
//        // Wait for thread to finish
//        droneThread.join(1000);
//        assertFalse("Drone should be stopped", drone.getRunning());
//    }


    // Test state class for testing state transitions and event handling in DroneSubsystem class
    private static class TestDroneState implements DroneState {
        public void handleEvent(DroneSubsystem drone, Event event) {}
        public void dispatch(DroneSubsystem drone, DroneDispatchEvent event) {}
        public void travel(DroneSubsystem drone) {}
        public void dropAgent(DroneSubsystem drone, DropAgentEvent event) {}
    }
}