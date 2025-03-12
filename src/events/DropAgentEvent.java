package events;

/**
 * Represents an event where a drone drops an extinguishing agent.
 */
public class DropAgentEvent extends Event {
    private int volume;
    private int droneID;

    /**
     * Constructs a DropAgentEvent with the specified volume.
     * The drone ID is not specified in this constructor.
     *
     * @param volume the amount of agent dropped
     */
    public DropAgentEvent(int volume) {
        super(null);
        this.volume = volume;
        this.droneID = -1; // Default value indicating no specific drone ID
    }


    /**
     * Constructs a DropAgentEvent with the specified volume and drone ID.
     *
     * @param volume the amount of agent dropped
     * @param droneID the ID of the drone performing the drop
     */
    public DropAgentEvent(int volume, int droneID) {
        super(null);
        this.volume = volume;
        this.droneID = droneID;
    }


    /**
     * Gets the amount of agent dropped.
     *
     * @return the volume of the agent dropped
     */
    public int getVolume() {
        return volume;
    }


    /**
     * Gets the ID of the drone performing the drop.
     * If the drone ID was not specified, this may return a default value.
     *
     * @return the drone ID, or a default value if not set
     */
    public int getDroneID() {
        return droneID;
    }
}
