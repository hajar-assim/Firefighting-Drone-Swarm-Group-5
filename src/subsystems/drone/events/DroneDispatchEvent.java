package subsystems.drone.events;

import subsystems.Event;
import subsystems.fire_incident.Faults;
import java.awt.geom.Point2D;
import java.time.LocalDateTime;

/**
 * Represents an event where a drone is dispatched to a specific zone with coordinates.
 */

public class DroneDispatchEvent extends Event {
    private int zoneID;
    private Point2D coords;
    private Faults fault;
    private boolean faultHandled = false;

    /**
     * Constructs a DroneDispatchEvent with the specified zone ID and coordinates.
     *
     * @param zoneID the ID of the zone where the drone is dispatched
     * @param coords the coordinates of the dispatch location
     */
    public DroneDispatchEvent(int zoneID, Point2D coords, Faults fault) {
        super(LocalDateTime.now().toString());
        this.zoneID = zoneID;
        this.coords = coords;
        this.fault = fault;
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
     * Returns the specific fault to simulate.
     * @return the fault.
     */
    public Faults getFault() {
        return fault;
    }

    /**
     * Returns the specific fault to simulate.
     */
    public void setFault(Faults fault) {
        this.fault= fault;
    }

    /**
     * Gets the coordinates of the dispatch location.
     *
     * @return the coordinates as a {@code Point2D} object
     */
    public Point2D getCoords() {
        return coords;
    }

    public boolean isFaultHandled() {
        return faultHandled;
    }

    public void markFaultHandled() {
        this.faultHandled = true;
    }

    /**
     * Returns a string representation of the event.
     *
     * @return a formatted string describing the event
     */
    @Override
    public String toString() {
        return String.format("DroneDispatchEvent[zoneID=%d, coords=(%.2f, %.2f), Fault=%s]",
                zoneID, coords.getX(), coords.getY(), fault.toString());
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
