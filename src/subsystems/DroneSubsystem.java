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
            System.out.println("Drone Subsystem received a message: " + message);

            // no more events
            if (message.getEventType() == EventType.EVENTS_DONE){
                System.out.println("Drone Subsystem received EVENTS_DONE message. Stopping thread");
                break;
            }

            message.setReceiver("FireIncident");
            System.out.println("Responding to Scheduler");
            sendEventManager.put(message);
        }
    }
}
