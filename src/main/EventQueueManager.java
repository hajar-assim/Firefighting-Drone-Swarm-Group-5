package main;

import events.IncidentEvent;

public class EventQueueManager {
    // Boolean to hold box state
    private boolean isEmpty = true;
    // Message to be passed to the queue
    private IncidentEvent message;

    /**
     * Place message in the queue for it to be read
     * @param message
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
        // Put message in the queue
        this.message = message;

        // Queue is not empty anymore so flip bool
        isEmpty = false;
        System.out.println("Incident added to queue: " + message);

        // Notify other threads that the queue has a message
        notifyAll();
    }

    /**
     * Method to get a message from the queue
     * @return The message placed in the queue
     */
    public synchronized IncidentEvent get(String receiver) {
        // Wait while queue is empty
        while (isEmpty || !message.getReceiver().equals(receiver)) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }

        // Variable to retrieve and return message from the queue
        IncidentEvent tmpMsg = message;

        // Set message as null again since this queue no longer has a message
        message = null;

        // Flip boolean to true since queue is now empty
        isEmpty = true;

        // Notify waiting threads that queue is now empty
        notifyAll();

        return tmpMsg;
    }
}
