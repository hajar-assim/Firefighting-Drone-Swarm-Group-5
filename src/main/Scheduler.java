package main;

import events.IncidentEvent;

public class Scheduler implements Runnable{
    private EventQueueManager eventManager;
    public Scheduler(EventQueueManager eventManager){
        this.eventManager = eventManager;
    }
    public void run(){
        while(true){
            IncidentEvent request = eventManager.get("Scheduler");
            System.out.println("Received request from Fire Incident Subsystem");

            request.setReceiver("FireIncidentSubsystem");
            eventManager.put(request);
            System.out.println("Forwarding request to Drone Subsystem");

            IncidentEvent response = eventManager.get("Scheduler");
            System.out.println("Received response from Drone Subsystem");

            request.setReceiver("DroneSubsystem");
            eventManager.put(response);
            System.out.println("Forwarding response to Fire Incident Subsystem");
        }
    }
}
