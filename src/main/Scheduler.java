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

            // no more events
            if (message.getEventType() == EventType.EVENTS_DONE){
                System.out.println("\nScheduler received EVENTS_DONE message. Forwarding message to drone subsystem and shutting down...");
                droneManager.put(message);
                return;
            }

            System.out.println("\nScheduler received a message: " + message);

            if(message.getReceiver().equals("Drone")){
                System.out.println("Scheduler forwarding message to Drone Subsystem");
                droneManager.put(message);
            }

            if(message.getReceiver().equals("FireIncident")){
                System.out.println("Scheduler forwarding message to Fire Incident Subsystem");
                fireIncidentManager.put(message);
            }
        }
    }
}
