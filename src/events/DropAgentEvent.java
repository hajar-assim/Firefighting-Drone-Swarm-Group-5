package events;

public class DropAgentEvent extends Event{
    private int volume;
    private int droneID;

    public DropAgentEvent(int volume){
        super(null);
        this.volume = volume;
    }

    public DropAgentEvent(int volume, int droneID){
        super(null);
        this.volume = volume;
        this.droneID = droneID;
    }

    public int getVolume() {
        return volume;
    }

    public int getDroneID() {
        return droneID;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public void fromString(String s) {

    }
}
