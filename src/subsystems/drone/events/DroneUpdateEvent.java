package subsystems.drone.events;

import subsystems.Event;
import subsystems.drone.DroneInfo;

/**
 * Represents an event that updates the state of a specific drone.
 */
public class DroneUpdateEvent extends Event {
    private int droneID;
    private DroneInfo droneInfo;

    /**
     * Constructs a DroneUpdateEvent with the specified drone ID and state.
     *
     * @param droneID the ID of the drone being updated
     * @param droneInfo the new info of the drone
     */
    public DroneUpdateEvent(int droneID, DroneInfo droneInfo) {
        super(null);
        this.droneID = droneID;
        this.droneInfo = droneInfo;
    }


    /**
     * Gets the ID of the drone being updated.
     *
     * @return the drone ID
     */
    public int getDroneID() {
        return droneID;
    }


    /**
     * Gets the updated state of the drone.
     *
     * @return the drone's state as a {@code DroneState} object
     */
    public DroneInfo getDroneInfo() {
        return droneInfo;
    }


    /**
     * Returns a string representation of the event.
     *
     * @return a formatted string describing the event
     */
    @Override
    public String toString() {
        return String.format("DroneUpdateEvent[droneID=%d, droneState=%s]",
                droneID, droneInfo);
    }


    /**
     * Parses a string representation of the event and updates the object's state.
     *
     * @param s the string to parse (format not yet defined)
     */
    @Override
    public void fromString(String s) {
        // implementation needed based on the expected format
    }
}
