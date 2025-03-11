package subsystems.drone;

import main.EventQueueManager;
import subsystems.Event;
import subsystems.drone.states.DroneState;
import subsystems.drone.states.IdleState;
import subsystems.fire_incident.FireIncidentSubsystem;

import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@code DroneSubsystem} class represents a drone unit that responds to incident events.
 * It continuously listens for new events from the receive event queue, processes them,
 * and dispatches responses to the send event queue.
 */
public class DroneSubsystem implements Runnable {
    private static final AtomicInteger nextId = new AtomicInteger(1);
    private EventQueueManager sendEventManager;
    private EventQueueManager receiveEventManager;
    private final int droneID;
    private DroneState state;
    private volatile boolean running;
    private int zoneID;
    private Point2D coordinates;
    private double flightTime;
    private int waterLevel;

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
        this.state = new IdleState();
        this.running = false;
        this.zoneID = 0;
        this.coordinates = FireIncidentSubsystem.BASE_COORDINATES;
        this.flightTime = 10 * 60;
        this.waterLevel = 15;
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
     * Returns the receiving queue manager of the drone.
     *
     * @return The EventQueueManager.
     */
    public EventQueueManager getRecieveEventManager(){
        return this.receiveEventManager;
    }

    /**
     * Returns the sending queue manager of the drone.
     *
     * @return The EventQueueManager.
     */
    public EventQueueManager getSendEventManager(){
        return this.sendEventManager;
    }

    /**
     * Returns the current state of the drone.
     *
     * @return The drone's state.
     */
    public DroneState getState() {
        return this.state;
    }

    /**
     * Sets a new state for the drone.
     *
     * @param newState The new state to transition to.
     */
    public void setState(DroneState newState) {
        this.state = newState;
    }


    /**
     * Gets the ID of the zone where the drone is located.
     *
     * @return the zone ID
     */
    public int getZoneID() {
        return zoneID;
    }


    /**
     * Sets the ID of the zone where the drone is located.
     *
     * @param zoneID the new zone ID
     */
    public void setZoneID(int zoneID) {
        this.zoneID = zoneID;
    }


    /**
     * Gets the current coordinates of the drone.
     *
     * @return the coordinates as a {@code Point2D} object
     */
    public Point2D getCoordinates() {
        return coordinates;
    }


    /**
     * Sets the current coordinates of the drone.
     *
     * @param coordinates the new coordinates of the drone
     */
    public void setCoordinates(Point2D coordinates) {
        this.coordinates = coordinates;
    }


    /**
     * Gets the total flight time of the drone.
     *
     * @return the flight time in minutes
     */
    public double getFlightTime() {
        return flightTime;
    }


    /**
     * Sets the total flight time of the drone.
     *
     * @param flightTime the new flight time in minutes
     */
    public void setFlightTime(double flightTime) {
        this.flightTime = flightTime;
    }


    /**
     * Gets the remaining water level in the drone.
     *
     * @return the water level in percentage
     */
    public int getWaterLevel() {
        return waterLevel;
    }


    /**
     * Sets the remaining water level in the drone.
     *
     * @param waterLevel the new water level in percentage
     */
    public void setWaterLevel(int waterLevel) {
        this.waterLevel = waterLevel;
    }

    /**
     * Calculates the estimated flight time required to travel between two coordinates.
     *
     * @param startCoords The starting coordinates.
     * @param endCoords   The target coordinates.
     * @return The estimated flight time in seconds.
     */
    public double timeToZone(Point2D startCoords, Point2D endCoords) {
        double distance = startCoords.distance(endCoords);
        return ((distance - 46.875) / 15 + 6.25);
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
            state.handleEvent(this, event);
        }
    }

    /**
     * Returns a string representation of the drone's state.
     *
     * @return a formatted string describing the drone's state
     */
    @Override
    public String toString() {
        return String.format("DroneState[zoneID=%d, coordinates=(%.2f, %.2f), flightTime=%.2f, waterLevel=%d%%]",
                zoneID, coordinates.getX(), coordinates.getY(), flightTime, waterLevel);
    }

}
