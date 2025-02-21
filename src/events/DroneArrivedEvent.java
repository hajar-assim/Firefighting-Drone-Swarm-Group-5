package events;

public class DroneArrivedEvent extends Event{
    private int droneID;
    private int zoneID;

    public DroneArrivedEvent(int droneID, int zoneID){
        super(null);
        this.droneID = droneID;
        this.zoneID = zoneID;
    }

    public int getDroneID() {
        return droneID;
    }

    public int getZoneID() {
        return zoneID;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public void fromString(String s) {

    }
}
