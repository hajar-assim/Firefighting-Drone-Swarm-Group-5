package main;

import events.EventType;
import events.IncidentEvent;

public class Scheduler implements Runnable{
    private EventQueueManager fireIncidentManager;
    private EventQueueManager droneManager;

    public Scheduler(EventQueueManager fireIncidentManager, EventQueueManager droneManager){
        this.fireIncidentManager = fireIncidentManager;
        this.droneManager = droneManager;
    }

    public void run(){
        while(true){
            IncidentEvent request = fireIncidentManager.get("Scheduler");

            // no more events
            if (request.getEventType() == EventType.EVENTS_DONE){
                droneManager.put(request);
                break;
            }

            System.out.println("Scheduler received request from Fire Incident Subsystem");

            request.setReceiver("DroneSubsystem");
            System.out.println("Scheduler forwarding request to Drone Subsystem");
            droneManager.put(request);

            IncidentEvent response = droneManager.get("Scheduler");
            System.out.println("\nScheduler received response from Drone Subsystem");

            System.out.println("Scheduler forwarding response to Fire Incident Subsystem");
            response.setReceiver("FireIncidentSubsystem");
            fireIncidentManager.put(request);
        }
    }
}
