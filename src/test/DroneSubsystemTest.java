package test;

import events.*;
import main.EventQueueManager;
import org.junit.Before;
import org.junit.Test;
import subsystems.DroneStatus;
import subsystems.DroneSubsystem;

import java.awt.geom.Point2D;

import static org.junit.Assert.*;

/**
 * Unit tests for the DroneSubsystem class.
 * This test suite verifies the drone's state transitions, dispatch logic,
 * water drop functionality, and refilling process.
 */
public class DroneSubsystemTest {
    private EventQueueManager receiveQueue;
    private EventQueueManager sendQueue;
    private DroneSubsystem droneSubsystem;

    /**
     * Sets up the test environment before each test case runs.
     * Initializes event queues and the drone subsystem.
     */
    @Before
    public void setUp() {
        receiveQueue = new EventQueueManager("ReceiveQueue");
        sendQueue = new EventQueueManager("SendQueue");
        droneSubsystem = new DroneSubsystem(receiveQueue, sendQueue);
    }

    /**
     * Tests the initial state of the drone.
     * Ensures that the drone starts in the IDLE state at coordinates (0,0).
     */
    @Test
    public void testDroneInitialization() {
        assertNotNull(droneSubsystem);
        assertEquals(DroneStatus.IDLE, droneSubsystem.getDroneState().getStatus());
        assertEquals(0, droneSubsystem.getDroneState().getZoneID());
        assertEquals(new Point2D.Double(0, 0), droneSubsystem.getDroneState().getCoordinates());
    }

    /**
     * Tests if the drone transitions to ON_ROUTE state upon receiving a dispatch event.
     */
    @Test
    public void testDroneDispatch() {
        DroneDispatchEvent dispatchEvent = new DroneDispatchEvent(1, new Point2D.Double(350, 300));
        receiveQueue.put(dispatchEvent);

        Thread droneThread = new Thread(droneSubsystem);
        droneThread.start();

        try {
            Thread.sleep(500); // Allow time for processing
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(DroneStatus.ON_ROUTE, droneSubsystem.getDroneState().getStatus());
        assertEquals(1, droneSubsystem.getDroneState().getZoneID());
    }

    /**
     * Tests if the drone refills after depleting its water supply.
     * Ensures that it returns to the IDLE state after refilling.
     */
    @Test
    public void testDroneRefill() {
        DropAgentEvent dropEvent = new DropAgentEvent(15, droneSubsystem.getDroneID());
        receiveQueue.put(dropEvent);

        Thread droneThread = new Thread(droneSubsystem);
        droneThread.start();

        assertEquals(15, droneSubsystem.getDroneState().getWaterLevel()); // Should be refilled to max capacity
        assertEquals(DroneStatus.IDLE, droneSubsystem.getDroneState().getStatus());
    }
}
