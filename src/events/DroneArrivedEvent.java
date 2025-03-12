package events;

/**
 * Represents an event when a drone arrives at a specific zone.
 */
public class DroneArrivedEvent extends Event {
    private int droneID;
    private int zoneID;

    /**
     * Constructs a DroneArrivedEvent with the specified drone ID and zone ID.
     *
     * @param droneID the ID of the drone that has arrived
     * @param zoneID the ID of the zone where the drone has arrived
     */
    public DroneArrivedEvent(int droneID, int zoneID) {
        super(null);
        this.droneID = droneID;
        this.zoneID = zoneID;
    }


    /**
     * Gets the ID of the drone that has arrived.
     *
     * @return the drone ID
     */
    public int getDroneID() {
        return droneID;
    }


    /**
     * Gets the ID of the zone where the drone has arrived.
     *
     * @return the zone ID
     */
    public int getZoneID() {
        return zoneID;
    }

}
