package main;

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
            System.out.println("Received request from Fire Incident Subsystem");

            request.setReceiver("DroneSubsystem");
            droneManager.put(request);
            System.out.println("Forwarding request to Drone Subsystem");

            IncidentEvent response = droneManager.get("Scheduler");
            System.out.println("Received response from Drone Subsystem");

            response.setReceiver("FireIncidentSubsystem");
            fireIncidentManager.put(response);
            System.out.println("Forwarding response to Fire Incident Subsystem");
        }
    }
}
