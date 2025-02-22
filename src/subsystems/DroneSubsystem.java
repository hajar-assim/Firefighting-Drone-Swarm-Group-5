package subsystems;

import events.*;
import main.EventQueueManager;

import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The DroneSubsystem class simulates the behavior of a drone unit that receives
 * incident events, processes them, and sends back a response.
 * It continuously listens for new events from the receive event queue
 * and dispatches responses to the send event queue.
 */
public class DroneSubsystem implements Runnable {
    private static final AtomicInteger nextId = new AtomicInteger(1);
    private final int MAX_AGENT = 15;
    private final int NOZZLE_OPEN_TIME = 1;
    private final double FLIGHT_TIME = 10 * 60; // flight time in seconds (10 mins)
    private final Point2D BASE_COORDINATES = new Point2D.Double(0, 0);
    private final EventQueueManager sendEventManager;
    private final EventQueueManager receiveEventManager;
    private final int droneID;
    private final DroneState droneState;
    private volatile boolean running;

    /**
     * Constructs a DroneSubsystem with the specified event managers.
     *
     * @param receiveEventManager The event queue manager from which the subsystem receives incident events.
     * @param sendEventManager    The event queue manager to which the subsystem sends processed events.
     */
    public DroneSubsystem(EventQueueManager receiveEventManager, EventQueueManager sendEventManager) {
        this.receiveEventManager = receiveEventManager;
        this.sendEventManager = sendEventManager;
        this.droneID = nextId.getAndIncrement();
        this.droneState = new DroneState(DroneStatus.IDLE, 0, BASE_COORDINATES, FLIGHT_TIME, MAX_AGENT);
        this.running = false;
    }


    /**
     * Gets the ID of the drone.
     *
     * @return the drone ID.
     */
    public int getDroneID() {
        return droneID;
    }


    /**
     * Gets the current state of the drone.
     *
     * @return the {@link DroneState} of the drone.
     */
    public DroneState getDroneState() {
        return droneState;
    }


    /**
     * Calculates the estimated flight time to a target zone.
     *
     * @param startCoords the starting coordinates of the drone.
     * @param endCoords   the target coordinates.
     * @return the estimated flight time in seconds.
     */
    private double timeToZone(Point2D startCoords, Point2D endCoords) {
        double distance = startCoords.distance(endCoords);
        return ((distance - 46.875) / 15 + 6.25);
    }


    /**
     * Simulates the drone traveling to a target zone.
     *
     * @param zoneID       the target zone ID.
     * @param targetCoords the target coordinates.
     */
    private void travelToTarget(int zoneID, Point2D targetCoords) {
        try {
            double flightTime = this.timeToZone(this.droneState.getCoordinates(), targetCoords);
            System.out.println("Drone " + this.droneID + " on route to Zone: " + zoneID + ", Estimated time: " + flightTime + "s");
            Thread.sleep((long) flightTime * 1000);
            this.droneState.setFlightTime(this.droneState.getFlightTime() - flightTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Processes a dispatch event, sending the drone to a specified zone.
     *
     * @param droneDispatchEvent the dispatch event containing the zone ID and target coordinates.
     */
    private void dispatchDrone(DroneDispatchEvent droneDispatchEvent) {
        this.droneState.setZoneID(droneDispatchEvent.getZoneID());
        this.droneState.setStatus(DroneStatus.ON_ROUTE);

        System.out.println("Drone " + this.droneID + " received Dispatch Request to Zone: " + droneDispatchEvent.getZoneID() + " " + droneDispatchEvent.getCoords());
        this.travelToTarget(droneDispatchEvent.getZoneID(), droneDispatchEvent.getCoords());
        this.droneState.setCoordinates(droneDispatchEvent.getCoords());

        if (droneDispatchEvent.getZoneID() == 0) {
            this.running = false;
            System.out.println("No more events, drone returned to base and shutting down");
            return;
        }

        System.out.println("Drone " + this.droneID + " arrived to Zone: " + droneDispatchEvent.getZoneID());
        DroneArrivedEvent arrivedEvent = new DroneArrivedEvent(this.droneID, this.droneState.getZoneID());
        this.sendEventManager.put(arrivedEvent);
    }


    /**
     * Simulates the drone dropping the fire suppression agent.
     *
     * @param dropAgentEvent the event containing the volume of agent to be dropped.
     */
    private void dropAgent(DropAgentEvent dropAgentEvent) {
        this.droneState.setStatus(DroneStatus.DROPPING_AGENT);

        try {
            System.out.println("Drone " + this.droneID + " opening nozzle and dropping agent.");
            Thread.sleep(NOZZLE_OPEN_TIME * 1000);
            // Rate of drop = 1L per second
            Thread.sleep(dropAgentEvent.getVolume() * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.droneState.setWaterLevel(this.droneState.getWaterLevel() - dropAgentEvent.getVolume());

        System.out.println("Dropped " + dropAgentEvent.getVolume() + " liters.");
        this.sendEventManager.put(new DropAgentEvent(dropAgentEvent.getVolume(), this.droneID));
        this.droneRefill();
    }


    /**
     * Simulates the drone refilling at the base.
     */
    private void droneRefill() {
        System.out.println("Drone " + this.droneID + " returning to Base (0,0) to refill.");
        this.droneState.setStatus(DroneStatus.REFILLING);

        this.travelToTarget(0, BASE_COORDINATES);

        this.droneState.setCoordinates(new Point2D.Double(0, 0));
        this.droneState.setWaterLevel(MAX_AGENT);
        System.out.println("Drone " + this.droneID + " refilled to " + this.droneState.getWaterLevel() + " liters.");
        this.droneState.setStatus(DroneStatus.IDLE);
    }


    /**
     * Starts the drone subsystem, which continuously listens for new incident events.
     * When an event is received, it processes the request and dispatches a response.
     * If an "EVENTS_DONE" event is received, the subsystem shuts down.
     */
    @Override
    public void run() {
        this.running = true;
        while (this.running) {
            Event event = receiveEventManager.get();

            if (event instanceof DroneDispatchEvent droneDispatchEvent) {
                this.dispatchDrone(droneDispatchEvent);
            } else if (event instanceof DropAgentEvent dropAgentEvent) {
                this.dropAgent(dropAgentEvent);
            }
        }
    }
}
