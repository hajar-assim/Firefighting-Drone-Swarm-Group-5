package subsystems.drone;

import main.EventSocket;
import subsystems.Event;
import subsystems.drone.states.DroneState;

import java.awt.geom.Point2D;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The {@code DroneSubsystem} class represents a drone unit that responds to incident events.
 * It continuously listens for new events from the receive event queue, processes them,
 * and dispatches responses to the send event queue.
 */
public class DroneSubsystem {
    private EventSocket sendSocket;
    private EventSocket receiveSocket;
    private InetAddress schedulerAddress;
    private int schedulerPort;
    DroneInfo info;


    /**
     * Constructs a {@code DroneSubsystem} with the specified event managers.
     *
     * @param schedulerAddress The IP address of the scheduler to send events to
     * @param schedulerPort The port of the scheduler to send events to
     */
    public DroneSubsystem(InetAddress schedulerAddress, int schedulerPort) {
        info = new DroneInfo();
        sendSocket = new EventSocket();
        receiveSocket = new EventSocket(6000 + info.getDroneID());
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
     * Returns the receiving socket of the drone.
     *
     * @return The EventSocket.
     */
    public EventSocket getRecieveSocket(){
        return this.receiveSocket;
    }

    /**
     * Returns the sending socket of the drone.
     *
     * @return The EventSocket.
     */
    public EventSocket getSendSocket(){
        return this.sendSocket;
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
        info.setState(newState);
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
     * @param waterLevel the new water level in percentage
     */
    public void setWaterLevel(int waterLevel) {
        info.setWaterLevel(waterLevel);
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
    public void run() {
        setRunning(true);
        while (getRunning()) {
            Event event = receiveSocket.receive();
            getState().handleEvent(this, event);
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
                getZoneID(), getCoordinates().getX(), getCoordinates().getY(), getFlightTime(), getWaterLevel());
    }

    public static void main(String args[]) {
        InetAddress address = null;
        try{
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }

        DroneSubsystem drone = new DroneSubsystem(address, 5000);
        drone.run();
    }

}
