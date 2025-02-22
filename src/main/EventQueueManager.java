package main;

import events.Event;
import events.EventType;
import events.IncidentEvent;

public class EventQueueManager {
    private boolean isEmpty;
    private Event message;
    private final String queueName;

    public EventQueueManager(String queueName){
        this.queueName = queueName;
        this.isEmpty = true;
    }

    /**
     * Place message in the queue for it to be read
     * @param message the message to be passed (Event)
     */
    public synchronized void put(Event message) {
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

<<<<<<< Updated upstream
=======
        if (message instanceof IncidentEvent) {
            switch (((IncidentEvent) message).getEventType()){
                case EVENTS_DONE -> System.out.println("[QUEUE] " + this.queueName + ": No more incident events.");
                default -> System.out.println("[QUEUE] Message added to " + this.queueName + ": {" + message + "}");
            }
        }

>>>>>>> Stashed changes
        notifyAll();
    }

    /**
     * Method to get a message from the queue
     * @return The message placed in the queue
     */
    public synchronized Event get() {
        while (isEmpty) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }

        // Variable to retrieve and return message from the queue
        Event tmpMsg = message;
        message = null;
        isEmpty = true;
        notifyAll();

        return tmpMsg;
    }
}