package subsystems;

import java.awt.geom.Point2D;

/**
 * Represents the state of a drone, including its status, location, flight time, and water level.
 */
public class DroneState {
    private DroneStatus status;
    private int zoneID;
    private Point2D coordinates;
    private double flightTime;
    private int waterLevel;

    /**
     * Constructs a DroneState with the given parameters.
     *
     * @param status      the current status of the drone
     * @param zoneID      the ID of the zone where the drone is located
     * @param coordinates the current coordinates of the drone
     * @param flightTime  the total flight time of the drone
     * @param waterLevel  the remaining water level in the drone
     */
    public DroneState(DroneStatus status, int zoneID, Point2D coordinates, double flightTime, int waterLevel) {
        this.status = status;
        this.zoneID = zoneID;
        this.coordinates = coordinates;
        this.flightTime = flightTime;
        this.waterLevel = waterLevel;
    }


    /**
     * Gets the current status of the drone.
     *
     * @return the drone's status
     */
    public DroneStatus getStatus() {
        return status;
    }


    /**
     * Sets the current status of the drone.
     *
     * @param status the new status of the drone
     */
    public void setStatus(DroneStatus status) {
        this.status = status;
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
     * Returns a string representation of the drone's state.
     *
     * @return a formatted string describing the drone's state
     */
    @Override
    public String toString() {
        return String.format("DroneState[status=%s, zoneID=%d, coordinates=(%.2f, %.2f), flightTime=%.2f, waterLevel=%d%%]",
                status, zoneID, coordinates.getX(), coordinates.getY(), flightTime, waterLevel);
    }
}
