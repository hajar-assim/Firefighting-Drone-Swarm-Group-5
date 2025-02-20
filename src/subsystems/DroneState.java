package subsystems;

import java.awt.geom.Point2D;

public class DroneState {
    private DroneStatus status;
    private int zoneID;
    private Point2D coordinates;
    private int batteryLevel;
    private int waterLevel;

    public DroneState(DroneStatus status, int zoneID, Point2D coordinates, int batteryLevel, int waterLevel){
        this.status = status;
        this.zoneID = zoneID;
        this.coordinates = coordinates;
        this.batteryLevel = batteryLevel;
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

    public Point2D getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Point2D coordinates) {
        this.coordinates = coordinates;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public int getWaterLevel() {
        return waterLevel;
    }

    public void setWaterLevel(int waterLevel) {
        this.waterLevel = waterLevel;
    }

    public void setZoneID(int zoneID) {
        this.zoneID = zoneID;
    }
}
