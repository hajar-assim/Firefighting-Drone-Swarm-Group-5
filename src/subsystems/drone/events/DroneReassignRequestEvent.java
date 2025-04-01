package subsystems.drone.events;

import subsystems.Event;

public class DroneReassignRequestEvent extends Event {
    private final int droneID;

    public DroneReassignRequestEvent(int droneID) {
        super(null);
        this.droneID = droneID;
    }

    public int getDroneID() {
        return droneID;
    }

    @Override
    public String toString() {
        return "DroneReassignRequestEvent[droneID=%d]";
    }

    @Override
    public void fromString(String s) {

    }
}
