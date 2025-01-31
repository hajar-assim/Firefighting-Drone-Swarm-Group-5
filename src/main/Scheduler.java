package main;

import events.EventType;
import events.IncidentEvent;

public class Scheduler implements Runnable{
    private EventQueueManager receiveEventManager;
    private EventQueueManager fireIncidentManager;
    private EventQueueManager droneManager;

    public Scheduler(EventQueueManager receiveEventManager, EventQueueManager fireIncidentManager, EventQueueManager droneManager){
        this.receiveEventManager = receiveEventManager;
        this.fireIncidentManager = fireIncidentManager;
        this.droneManager = droneManager;
    }

    public void run(){
        while(true){
            IncidentEvent message = receiveEventManager.get();
            System.out.println("Scheduler received a message: " + message);

            // no more events
            if (message.getEventType() == EventType.EVENTS_DONE){
                droneManager.put(message);
                System.out.println("Scheduler received EVENTS_DONE message. Stopping thread");
                break;
            }

            if(message.getReceiver().equals("Drone")){
                droneManager.put(message);
                System.out.println("Scheduler forwarding message to Drone Subsystem");
            }

            if(message.getReceiver().equals("FireIncident")){
                fireIncidentManager.put(message);
                System.out.println("Scheduler forwarding message to Fire Incident Subsystem");
            }
        }
    }
}
