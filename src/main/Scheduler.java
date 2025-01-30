package main;

import events.IncidentEvent;

import java.util.ArrayList;

public class Scheduler implements Runnable{
    private EventQueueManager fireIncidentManager;
    private ArrayList<EventQueueManager> droneManagers;

    public Scheduler(EventQueueManager fireIncidentManager, ArrayList<EventQueueManager> droneManagers){
        this.fireIncidentManager = fireIncidentManager;
        this.droneManagers = droneManagers;
    }

    private EventQueueManager scheduleDrone(){
        // Placeholder for while only one drone is being considered
        return droneManagers.get(0);
    }

    public void run(){
        while(true){
            IncidentEvent request = fireIncidentManager.get("Scheduler");
            System.out.println("Received request from Fire Incident Subsystem");

            EventQueueManager droneManager = scheduleDrone();

            request.setReceiver("DroneSubsystem");
            droneManager.put(request);
            System.out.println("Forwarding request to Drone Subsystem");

            IncidentEvent response = droneManager.get("Scheduler");
            System.out.println("Received response from Drone Subsystem");

            request.setReceiver("FireIncidentSubsystem");
            fireIncidentManager.put(response);
            System.out.println("Forwarding response to Fire Incident Subsystem");
        }
    }
}
