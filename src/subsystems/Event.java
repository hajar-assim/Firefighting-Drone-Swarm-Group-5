package subsystems;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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

    /**
     * Sets the timestamp of the event.
     *
     * @param timeStamp the new timestamp
     */
    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * Gets the date of the event.
     *
     * @return the date as a string
     */
    public String getDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDateTime.parse(this.getTimeStamp(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).format(formatter);
    }

    /**
     * Gets the time of the event.
     *
     * @return the time as a string
     */
    public String getTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return LocalDateTime.parse(this.getTimeStamp(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).format(formatter);
    }

    /**
     * Gets the time of the event as a LocalTime object.
     *
     * @return the time as a LocalTime object
     */
    public LocalTime getParsedTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return LocalTime.parse(this.getTimeStamp(), formatter);
    }

    /**
     * Returns a string representation of the event.
     *
     * @return a formatted string describing the event
     */
    @Override
    public abstract String toString();


    /**
     * Parses a string representation of the event and updates the object's state.
     *
     * @param s the string to parse
     */
    public abstract void fromString(String s);
}
