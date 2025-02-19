package subsystems;

import events.EventType;
import events.IncidentEvent;
import main.EventQueueManager;

/**
 * The DroneSubsystem class simulates the behavior of a drone unit that receives
 * incident events, processes them, and sends back a response.
 * It continuously listens for new events from the receive event queue
 * and dispatches responses to the send event queue.
 */
public class DroneSubsystem implements Runnable {
    private EventQueueManager sendEventManager;
    private EventQueueManager receiveEventManager;
    private int droneId;
    private DroneState droneState;

    /**
     * Constructs a DroneSubsystem with the specified event managers.
     *
     * @param receiveEventManager The event queue manager from which the subsystem receives incident events.
     * @param sendEventManager    The event queue manager to which the subsystem sends processed events.
     */
    public DroneSubsystem(EventQueueManager receiveEventManager, EventQueueManager sendEventManager) {
        this.receiveEventManager = receiveEventManager;
        this.sendEventManager = sendEventManager;
        this.droneState = new DroneState(DroneStatus.IDLE, 0, null, 100, 15);
    }

    /**
     * Starts the drone subsystem, which continuously listens for new incident events.
     * When an event is received, it processes the request and dispatches a response.
     * If an "EVENTS_DONE" event is received, the subsystem shuts down.
     */
    @Override
    public void run() {
        while (true) {
            IncidentEvent message = (IncidentEvent) receiveEventManager.get();

            // Check if it's an "EVENTS_DONE" signal to terminate the subsystem
            if (message.getEventType() == EventType.EVENTS_DONE) {
                System.out.println("\nDrone subsystem received EVENTS_DONE. Shutting down...");
                return;
            }

            System.out.println("\nDrone subsystem received request: " + message);

            // Create response
            // Send response back to the scheduler
            System.out.println("Drone subsystem, sending response to Scheduler");
            sendEventManager.put(message);
        }
    }
}
