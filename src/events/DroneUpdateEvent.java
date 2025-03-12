package events;

import subsystems.DroneState;

/**
 * Represents an event that updates the state of a specific drone.
 */
public class DroneUpdateEvent extends Event {
    private int droneID;
    private DroneState droneState;

    /**
     * Constructs a DroneUpdateEvent with the specified drone ID and state.
     *
     * @param droneID the ID of the drone being updated
     * @param droneState the new state of the drone
     */
    public DroneUpdateEvent(int droneID, DroneState droneState) {
        super(null);
        this.droneID = droneID;
        this.droneState = droneState;
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
    public DroneState getDroneState() {
        return droneState;
    }
}
