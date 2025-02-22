package subsystems;

import events.*;
import main.EventQueueManager;

import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@code DroneSubsystem} class represents a drone unit that responds to incident events.
 * It continuously listens for new events from the receive event queue, processes them,
 * and dispatches responses to the send event queue.
 */
public class DroneSubsystem implements Runnable {
    private static final AtomicInteger nextId = new AtomicInteger(1);
    private final int MAX_AGENT = 15;
    private final int NOZZLE_OPEN_TIME = 1;
    private final double FLIGHT_TIME = 10 * 60; // Flight time in seconds (10 minutes)
    private final Point2D BASE_COORDINATES = new Point2D.Double(0,0);
    private EventQueueManager sendEventManager;
    private EventQueueManager receiveEventManager;
    private final int droneID;
    private DroneState droneState;
    private volatile boolean running;

    /**
     * Constructs a {@code DroneSubsystem} with the specified event managers.
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
     * Returns the unique ID of the drone.
     *
     * @return The drone ID.
     */
    public int getDroneID() {
        return droneID;
    }


    /**
     * Returns the current state of the drone.
     *
     * @return The drone's state.
     */
    public DroneState getDroneState() {
        return droneState;
    }


    /**
     * Calculates the estimated flight time required to travel between two coordinates.
     *
     * @param startCoords The starting coordinates.
     * @param endCoords   The target coordinates.
     * @return The estimated flight time in seconds.
     */
    private double timeToZone(Point2D startCoords, Point2D endCoords){
        double distance = startCoords.distance(endCoords);
        return ((distance - 46.875) / 15 + 6.25);
    }


    /**
     * Simulates the drone traveling to a target zone.
     *
     * @param zoneID       The ID of the target zone.
     * @param targetCoords The coordinates of the target location.
     */
    private void travelToTarget(int zoneID, Point2D targetCoords){
        try {
            double flightTime = this.timeToZone(this.droneState.getCoordinates(), targetCoords);
            System.out.printf(
                    "[DRONE %d] On route to Zone: %d | Estimated time: %.2f seconds%n",
                    this.droneID,
                    zoneID,
                    flightTime
            );
            Thread.sleep((long) flightTime * 1000);
            this.droneState.setFlightTime(this.droneState.getFlightTime() - flightTime);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }


    /**
     * Handles the dispatch of a drone to a specific zone.
     *
     * @param droneDispatchEvent The event containing dispatch details.
     */
    private void dispatchDrone(DroneDispatchEvent droneDispatchEvent){
        this.droneState.setZoneID(droneDispatchEvent.getZoneID());
        this.droneState.setStatus(DroneStatus.ON_ROUTE);

        System.out.printf(
                "[DRONE %d] Received dispatch request → Zone: %d | Coordinates: (%.1f, %.1f)%n",
                this.droneID,
                droneDispatchEvent.getZoneID(),
                droneDispatchEvent.getCoords().getX(),
                droneDispatchEvent.getCoords().getY()
        );

        this.travelToTarget(droneDispatchEvent.getZoneID(), droneDispatchEvent.getCoords());
        this.droneState.setCoordinates(droneDispatchEvent.getCoords());

        if (droneDispatchEvent.getZoneID() == 0){
            this.running = false;
            System.out.println("[DRONE " + this.droneID + "] No more events, drone returned to base and shutting down");
            return;
        }

        System.out.println("\n[DRONE " + this.droneID + "] Arrived at Zone: " + droneDispatchEvent.getZoneID());
        DroneArrivedEvent arrivedEvent = new DroneArrivedEvent(this.droneID, this.droneState.getZoneID());
        this.sendEventManager.put(arrivedEvent);
    }


    /**
     * Handles the process of dropping agent at a target location.
     *
     * @param dropAgentEvent The event containing the drop details.
     */
    private void dropAgent(DropAgentEvent dropAgentEvent){
        this.droneState.setStatus(DroneStatus.DROPPING_AGENT);

        try{
            System.out.println("[DRONE " + this.droneID + "] Opening nozzle and dropping agent.");
            Thread.sleep(NOZZLE_OPEN_TIME * 1000);
            // Rate of drop = 1L per second
            Thread.sleep(dropAgentEvent.getVolume() * 1000L);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        this.droneState.setWaterLevel(this.droneState.getWaterLevel() - dropAgentEvent.getVolume());

        System.out.println("[DRONE " + this.droneID + "] Dropped " + dropAgentEvent.getVolume() + " liters.");
        this.sendEventManager.put(new DropAgentEvent(dropAgentEvent.getVolume(), this.droneID));
        this.droneRefill();
    }


    /**
     * Simulates the drone returning to the base for refilling.
     */
    private void droneRefill(){
        System.out.println("\n[DRONE " + this.droneID + "] Returning to Base (0,0) to refill.");
        this.droneState.setStatus(DroneStatus.REFILLING);

        this.travelToTarget(0, BASE_COORDINATES);

        this.droneState.setCoordinates(BASE_COORDINATES);
        this.droneState.setWaterLevel(MAX_AGENT);
        System.out.println("[DRONE " + this.droneID + "] Refilled to " + this.droneState.getWaterLevel() + " liters.");
        this.droneState.setStatus(DroneStatus.IDLE);

        // Notify the scheduler that this drone is operational and ready to go
        DroneUpdateEvent updateEvent = new DroneUpdateEvent(this.droneID, this.droneState);
        sendEventManager.put(updateEvent);
    }


    /**
     * Starts the drone subsystem, continuously listening for new incident events.
     * The subsystem processes received events and dispatches responses accordingly.
     * If an "EVENTS_DONE" event is received, the subsystem shuts down.
     */
    @Override
    public void run() {
        this.running = true;
        while (this.running) {
            Event event = receiveEventManager.get();

            if (event instanceof DroneDispatchEvent droneDispatchEvent){
                this.dispatchDrone(droneDispatchEvent);
            } else if (event instanceof DropAgentEvent dropAgentEvent) {
                this.dropAgent(dropAgentEvent);
            }
        }
    }
}
