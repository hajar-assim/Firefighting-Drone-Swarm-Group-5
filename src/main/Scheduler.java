package main;

import events.EventType;
import events.IncidentEvent;

public class Scheduler implements Runnable{
    private EventQueueManager receiveEventManager;
    private EventQueueManager fireIncidentManager;
    private EventQueueManager droneManager;

    /**
     * Constructs a FireIncidentSubsystem.
     *
     * @param receiveEventManager The queue manager responsible for handling received events.
     * @param fireIncidentManager The queue manager to forward messages to the fire incident subsystem.
     * @param droneManager The queue manager to forward messages to the fire incident subsystem.
     */
    public Scheduler(EventQueueManager receiveEventManager, EventQueueManager fireIncidentManager, EventQueueManager droneManager){
        this.receiveEventManager = receiveEventManager;
        this.fireIncidentManager = fireIncidentManager;
        this.droneManager = droneManager;
    }

    /**
     * Runs the Scheduler thread. Listens for messages and forwards them to the intended receivers.
     */
    public void run(){
        while(true){
            IncidentEvent message = (IncidentEvent) receiveEventManager.get();

            // no more events
            if (message.getEventType() == EventType.EVENTS_DONE){
                System.out.println("\nScheduler received EVENTS_DONE message. Forwarding message to drone subsystem and shutting down...");
                droneManager.put(message);
                return;
            }

            System.out.println("\nScheduler received a message: " + message);

//            if(message.getReceiver().equals("Drone")){
//                System.out.println("Scheduler forwarding message to Drone Subsystem");
//                droneManager.put(message);
//            }
//
//            if(message.getReceiver().equals("FireIncident")){
//                System.out.println("Scheduler forwarding message to Fire Incident Subsystem");
//                fireIncidentManager.put(message);
//            }
        }
    }
}
