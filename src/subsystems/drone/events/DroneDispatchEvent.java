package subsystems.drone.events;

import subsystems.Event;

import java.awt.geom.Point2D;
import java.time.LocalDateTime;

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
        super(LocalDateTime.now().toString());
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


    /**
     * Returns a string representation of the event.
     *
     * @return a formatted string describing the event
     */
    @Override
    public String toString() {
        return String.format("DroneDispatchEvent[zoneID=%d, coords=(%.2f, %.2f)]",
                zoneID, coords.getX(), coords.getY());
    }


    /**
     * Parses a string representation of the event and updates the object's state.
     *
     * @param s the string to parse (format not yet defined)
     */
    @Override
    public void fromString(String s) {
        // Implementation needed based on the expected format
    }
}
