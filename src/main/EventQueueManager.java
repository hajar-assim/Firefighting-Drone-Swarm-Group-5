package main;

import events.EventType;
import events.IncidentEvent;

public class EventQueueManager {
    private boolean isEmpty;
    private IncidentEvent message;
    private String queueName;

    public EventQueueManager(String queueName){
        this.queueName = queueName;
        this.isEmpty = true;
    }

    /**
     * Place message in the queue for it to be read
     * @param message the message to be passed (IncidentEvent)
     */
    public synchronized void put(IncidentEvent message) {
        // Wait while the queue is full
        while (!isEmpty) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        this.message = message;
        isEmpty = false;

        switch (message.getEventType()){
            case DRONE_DISPATCHED -> {}
            case EVENTS_DONE -> System.out.println("\n" + this.queueName + ": No more incident events.");
            default -> System.out.println("\nIncident added to " + this.queueName + ": " + message);
        }

        notifyAll();
    }

    /**
     * Method to get a message from the queue
     * @return The message placed in the queue
     */
    public synchronized IncidentEvent get() {
        while (isEmpty) {
            try {
                // if the message has an EVENTS_DONE event type return it to avoid waiting for no reason
                if (message != null  && message.getEventType() == EventType.EVENTS_DONE){
                    return message;
                }
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }

        // Variable to retrieve and return message from the queue
        IncidentEvent tmpMsg = message;
        message = null;
        isEmpty = true;
        notifyAll();

        return tmpMsg;
    }
}
