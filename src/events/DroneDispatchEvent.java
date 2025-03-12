package events;

import java.awt.geom.Point2D;

/**
 * Represents an event where a drone is dispatched to a specific zone with coordinates.
 */
public class DroneDispatchEvent extends Event {
    private int zoneID;
    private Point2D coords;


    /**
     * Constructs a DroneDispatchEvent with the specified zone ID and coordinates.
     *
     * @param zoneID the ID of the zone where the drone is dispatched
     * @param coords the coordinates of the dispatch location
     */
    public DroneDispatchEvent(int zoneID, Point2D coords) {
        super(null);
        this.zoneID = zoneID;
        this.coords = coords;
    }


    /**
     * Gets the ID of the zone where the drone is dispatched.
     *
     * @return the zone ID
     */
    public int getZoneID() {
        return zoneID;
    }


    /**
     * Gets the coordinates of the dispatch location.
     *
     * @return the coordinates as a {@code Point2D} object
     */
    public Point2D getCoords() {
        return coords;
    }
}
