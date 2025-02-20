package subsystems;

import java.awt.geom.Point2D;

public class DroneState {
    private DroneStatus status;
    private int zoneID;
    private Point2D coordinates;
    private double flightTime;
    private int waterLevel;

    public DroneState(DroneStatus status, int zoneID, Point2D coordinates, double flightTime, int waterLevel){
        this.status = status;
        this.zoneID = zoneID;
        this.coordinates = coordinates;
        this.flightTime = flightTime;
        this.waterLevel = waterLevel;
    }

    public DroneStatus getStatus() {
        return status;
    }

    public void setStatus(DroneStatus status) {
        this.status = status;
    }

    public int getZoneID() {
        return zoneID;
    }

    public void setZoneID(int zoneID) {
        this.zoneID = zoneID;
    }

    public Point2D getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Point2D coordinates) {
        this.coordinates = coordinates;
    }

    public double getFlightTime() {
        return flightTime;
    }

    public void setFlightTime(double flightTime) {
        this.flightTime = flightTime;
    }

    public int getWaterLevel() {
        return waterLevel;
    }

    public void setWaterLevel(int waterLevel) {
        this.waterLevel = waterLevel;
    }
}
