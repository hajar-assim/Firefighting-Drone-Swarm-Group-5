package subsystems.drone;

import subsystems.drone.states.DroneState;
import subsystems.drone.states.IdleState;
import subsystems.fire_incident.FireIncidentSubsystem;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class DroneInfo implements Serializable {
    private int droneID;
    private DroneState state;
    private volatile boolean running;
    private int zoneID;
    private Point2D coordinates;
    private double flightTime;
    private int waterLevel;
    private InetAddress address;
    private Integer port;
    private boolean nozzleJam;


    /**
     * Constructs a new DroneInfo object with the specified address and port.
     * Initializes the drone's state, flight time, water level, and other parameters
     * to their default values.
     *
     * @param address The InetAddress representing the drone's network address.
     * @param port The port number to associate with the drone.
     */
    public DroneInfo(InetAddress address, Integer port){
        this.droneID = -1;
        this.state = new IdleState();
        this.running = false;
        this.zoneID = 0;
        this.coordinates = FireIncidentSubsystem.BASE_COORDINATES;
        this.flightTime = 10 * 60;
        this.waterLevel = 15;
        this.address = address;
        this.port = port;
        this.nozzleJam = false;
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
     * Sets the unique ID of the drone.
     *
     */
    public void setDroneID(int droneID) {
        this.droneID = droneID;
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
     * Gets the running status of the drone.
     *
     * @return the running status
     */
    public boolean getRunning(){
        return running;
    }

    /**
     * Sets the running status of the drone.
     *
     * @param running the new running status
     */
    public void setRunning(boolean running){
        this.running = running;
    }

    /**
     * TODO: javadocs
     */
    public InetAddress getAddress(){
        return this.address;
    }


    /**
     * TODO: javadocs
     */
    public Integer getPort(){
        return this.port;
    }

    public void setNozzleJam(boolean set){
        this.nozzleJam = set;
    }

    public boolean getNozzleJam(){
        return this.nozzleJam;
    }
}
