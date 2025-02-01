package subsystems;

import events.EventType;
import events.IncidentEvent;
import main.EventQueueManager;

public class DroneSubsystem implements Runnable {
    EventQueueManager sendEventManager;
    EventQueueManager receiveEventManager;

    public DroneSubsystem(EventQueueManager receiveEventManager, EventQueueManager sendEventManager){
        this.receiveEventManager = receiveEventManager;
        this.sendEventManager = sendEventManager;
    }

    public void run(){
        while(true){
            IncidentEvent message = receiveEventManager.get();

            if (message.getEventType() == EventType.EVENTS_DONE) {
                System.out.println("\nDrone subsystem received EVENTS_DONE. Shutting down...");
                return;
            }

            System.out.println("\nDrone subsystem received request: " + message);

            // create response
            message.setReceiver("FireIncident");
            message.setEventType(EventType.DRONE_DISPATCHED);

            // Send response back to the scheduler
            System.out.println("Drone subsystem, sending response to Scheduler");
            sendEventManager.put(message);
        }
    }
}
