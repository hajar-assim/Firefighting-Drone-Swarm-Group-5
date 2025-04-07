package subsystems.drone;

import logger.EventLogger;
import main.EventSocket;
import subsystems.Event;
import subsystems.drone.events.DroneUpdateEvent;
import subsystems.drone.events.DropAgentEvent;
import subsystems.drone.states.DroneState;
import subsystems.drone.states.IdleState;

import java.awt.geom.Point2D;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The {@code DroneSubsystem} class represents a drone unit that responds to incident events.
 * It continuously listens for new events from the recieve event queue, processes them,
 * and dispatches responses to the send event queue.
 */
public class DroneSubsystem {
    private final EventSocket socket;
    private final InetAddress schedulerAddress;
    private final int schedulerPort;
    public static int DRONE_BATTERY_TIME = 30;
    DroneInfo info;

    /**
     * Constructs a {@code DroneSubsystem} with the specified event managers.
     *
     * @param schedulerAddress The IP address of the scheduler to send events to
     * @param schedulerPort The port of the scheduler to send events to
     */
    public DroneSubsystem(InetAddress schedulerAddress, int schedulerPort) {
        socket = new EventSocket();
        try {
            info = new DroneInfo(InetAddress.getLocalHost(), socket.getSocket().getLocalPort());
        } catch (UnknownHostException e) {
            System.err.println("Unknown host being assigned to Drone.");
        }
        this.schedulerAddress = schedulerAddress;
        this.schedulerPort = schedulerPort;
    }

    /**
     * Returns the unique ID of the drone.
     *
     * @return The drone ID.
     */
    public int getDroneID(){
        return info.getDroneID();
    }

    /**
     * Returns the sending socket of the drone.
     *
     * @return The EventSocket.
     */
    public EventSocket getSocket(){
        return this.socket;
    }

    /**
     * Returns the IP address of the scheduler.
     *
     * @return The IP address of the scheduler.
     */
    public InetAddress getSchedulerAddress() {
        return schedulerAddress;
    }

    /**
     * Returns the port of the scheduler.
     *
     * @return The port of the scheduler.
     */
    public int getSchedulerPort() {
        return schedulerPort;
    }

    /**
     * Returns the info of the drone.
     *
     * @return The drone info.
     */
    public DroneInfo getDroneInfo(){
        return info;
    }

    /**
     * Sets the current info of the drone.
     *
     * @param info the new info of the drone
     */
    public void setDroneInfo(DroneInfo info){
        this.info = info;
    }

    /**
     * Returns the current state of the drone.
     *
     * @return The drone's state.
     */
    public DroneState getState(){
        return info.getState();
    }

    /**
     * Sets a new state for the drone.
     *
     * @param newState The new state to transition to.
     */
    public void setState(DroneState newState){
        // If the current state is IdleState and we're leaving it, update idle time.
        if (info.getState() instanceof IdleState && !(newState instanceof IdleState)) {
            if (info.getIdleStartTime() > 0) {
                long idleDuration = System.currentTimeMillis() - info.getIdleStartTime();
                info.setTotalIdleTime(info.getTotalIdleTime() + idleDuration);
                info.setIdleStartTime(0);
            }
        }

        // If transitioning into IdleState, record the start time if not already set.
        if (newState instanceof IdleState && info.getIdleStartTime() == 0) {
            info.setIdleStartTime(System.currentTimeMillis());
        }


        info.setState(newState);
        DroneUpdateEvent droneUpdateEvent = new DroneUpdateEvent(info);
        socket.send(droneUpdateEvent, getSchedulerAddress(), getSchedulerPort());
    }

    /**
     * Gets the ID of the zone where the drone is located.
     *
     * @return the zone ID
     */
    public int getZoneID(){
        return info.getZoneID();
    }

    /**
     * Sets the ID of the zone where the drone is located.
     *
     * @param zoneID the new zone ID
     */
    public void setZoneID(int zoneID){
        info.setZoneID(zoneID);
    }

    /**
     * Gets the current coordinates of the drone.
     *
     * @return the coordinates as a {@code Point2D} object
     */
    public Point2D getCoordinates() {
        return info.getCoordinates();
    }

    /**
     * Sets the current coordinates of the drone.
     *
     * @param coordinates the new coordinates of the drone
     */
    public void setCoordinates(Point2D coordinates) {
        info.setCoordinates(coordinates);
    }

    /**
     * Gets the total flight time of the drone.
     *
     * @return the flight time in minutes
     */
    public double getFlightTime() {
        return info.getFlightTime();
    }

    /**
     * Sets the total flight time of the drone.
     *
     * @param flightTime the new flight time in minutes
     */
    public void setFlightTime(double flightTime) {
        info.setFlightTime(flightTime);
    }

    /**
     * Gets the remaining water level in the drone.
     *
     * @return the water level in percentage
     */
    public int getWaterLevel() {
        return info.getWaterLevel();
    }

    /**
     * Sets the remaining water level in the drone.
     *
     * @param waterLevel the new water level
     */
    public void setWaterLevel(int waterLevel) {
        info.setWaterLevel(waterLevel);
    }

    /**
     * Changes the remaining water level in the drone.
     *
     * @param change the value to subtract to new water level
     */
    public void subtractWaterLevel(int change) {
        info.setWaterLevel(info.getWaterLevel() - change);
        DropAgentEvent dropEvent =  new DropAgentEvent(change, getDroneID());
        socket.send(dropEvent, schedulerAddress, schedulerPort);
    }

    /**
     * Gets the running status of the drone.
     *
     * @return the running status
     */
    public boolean getRunning(){
        return info.getRunning();
    }

    /**
     * Sets the running status of the drone.
     *
     * @param running the new running status
     */
    public void setRunning(boolean running){
        info.setRunning(running);
    }

    /**
     * Shuts down the drone by setting the running status to false and closing the socket.
     * The drone will no longer be able to receive events.
     */
    public void shutdown() {
        EventLogger.warn(getDroneID(), "Shutting down drone...");
        socket.getSocket().close();
        setRunning(false);
    }

    /**
     * Calculates the estimated flight time required to travel between two coordinates.
     *
     * @param startCoords The starting coordinates.
     * @param endCoords   The target coordinates.
     * @return The estimated flight time in seconds.
     */
    public static double timeToZone(Point2D startCoords, Point2D endCoords) {
        double distance = startCoords.distance(endCoords);
        return ((distance - 46.875) / 15 + 6.25);
    }

    /**
     * Starts the drone subsystem, continuously listening for new incident events.
     * The subsystem processes received events and dispatches responses accordingly.
     * If an "EVENTS_DONE" event is received, the subsystem shuts down.
     */
    public void run() {
        setRunning(true);
        registerWithScheduler();
        while (getRunning()) {
            Event event = socket.receive();
            getState().handleEvent(this, event);
        }
        EventLogger.info(getDroneID(), "No more incidents, drone has been shut down.", false);
        socket.getSocket().close();
    }

    /**
     * Returns a string representation of the drone's state.
     *
     * @return a formatted string describing the drone's state
     */
    @Override
    public String toString() {
        return String.format("DroneState[zoneID=%d, coordinates=(%.2f, %.2f), flightTime=%.2f, waterLevel=%d%%]",
                getZoneID(), getCoordinates().getX(), getCoordinates().getY(), getFlightTime(), getWaterLevel());
    }

    /**
     * Registers the drone with the scheduler by sending a registration event
     * and awaiting confirmation. Once the drone is successfully registered,
     * the drone's information is updated with the assigned Drone ID.
     *
     * In the process, the drone sends its information to the scheduler and
     * waits for a response. If the registration is successful, the drone's
     * information is updated with the assigned Drone ID.
     */
    private void registerWithScheduler() {
        try {
            DroneUpdateEvent event = new DroneUpdateEvent(this.info);
            socket.send(event, schedulerAddress, schedulerPort);
            EventLogger.info(-1, "Sent registration to Scheduler. Drone Address: " + InetAddress.getLocalHost() + ", Drone Port: " + socket.getSocket().getLocalPort(), false);
            event = (DroneUpdateEvent) socket.receive();
            EventLogger.info(event.getDroneInfo().getDroneID(), "Drone registered with Scheduler as Drone " + event.getDroneInfo().getDroneID() + ".\n", false);
            this.setDroneInfo(event.getDroneInfo());
        } catch (Exception e) {
            EventLogger.error(-1, "Error registering drone with Scheduler: " + e.getMessage());
        }
    }

    /**
     * Creates a new drone.
     * @param args
     */
    public static void main(String[] args) {
        InetAddress address = null;
        try{
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            EventLogger.error(-1, "Unable to retrieve local host: " + e.getMessage());
            System.exit(1);
        }

        DroneSubsystem drone = new DroneSubsystem(address, 5000);
        drone.run();
    }

}
