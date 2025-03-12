package events;

import java.io.Serializable;

/**
 * Represents an abstract event with a timestamp.
 * Subclasses must implement methods to convert the event to and from a string representation.
 */
public abstract class Event implements Serializable {
    private String timeStamp;

    /**
     * Constructs an event with the specified timestamp.
     *
     * @param timeStamp the timestamp of the event
     */
    public Event(String timeStamp) {
        this.timeStamp = timeStamp;
    }


    /**
     * Gets the timestamp of the event.
     *
     * @return the timestamp as a string
     */
    public String getTimeStamp() {
        return this.timeStamp;
    }
}
