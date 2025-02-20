package events;

public abstract class Event {
    private String timeStamp;

    public Event(String timeStamp){
        this.timeStamp = timeStamp;
    }
    public String getTimeStamp(){
        return this.timeStamp;
    }

    public abstract String toString();
    public abstract void fromString(String s);
}