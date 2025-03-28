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
    private boolean simulateFault;
    private Faults fault;


    /**
     * Constructs a DroneDispatchEvent with the specified zone ID, coordinates, and fault flag.
     */
    public DroneDispatchEvent(int zoneID, Point2D coords, boolean simulateFault, Faults fault) {
        super(LocalDateTime.now().toString());
        this.zoneID = zoneID;
        this.coords = coords;
        this.simulateFault = simulateFault;
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
     * Gets the flag that determines if we should simulate a stuck fault.
     * @return simulateStuckFault bool
     */
    public boolean isSimulateFault() {
        return simulateFault;
    }

    /**
     * Returns the specific fault to simulate.
     * @return the fault.
     */
    public Faults getFault() {
        return fault;
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
        return String.format("DroneDispatchEvent[zoneID=%d, coords=(%.2f, %.2f), simulateStuckFault=%s]",
                zoneID, coords.getX(), coords.getY(), simulateFault);
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
