package events;

import subsystems.DroneState;

public class DroneUpdateEvent extends Event{
    private int droneID;
    private DroneState droneState;

    public DroneUpdateEvent(int droneID, DroneState droneState){
        super(null);
        this.droneID = droneID;
        this.droneState = droneState;
    }

    public int getDroneID() {
        return droneID;
    }

    public DroneState getDroneState() {
        return droneState;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public void fromString(String s) {

    }
}
