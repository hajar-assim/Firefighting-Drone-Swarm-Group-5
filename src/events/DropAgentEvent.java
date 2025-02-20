package events;

public class DropAgentEvent extends Event{
    private int volume;

    public DropAgentEvent(int volume){
        super(null);
        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public void fromString() {

    }
}
