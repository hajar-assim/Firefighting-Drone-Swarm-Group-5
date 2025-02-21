package main;

import events.*;
import subsystems.DroneState;
import subsystems.DroneStatus;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler implements Runnable {
    private EventQueueManager receiveEventManager;
    private EventQueueManager fireIncidentManager;
    private EventQueueManager droneManager;
    private EventQueueManager sendEventManager;
    private ArrayList<DroneState> drones;
    private volatile boolean running = true;
    private HashMap<Integer, DroneStatus> droneStates;
    private Queue<IncidentEvent> incidentQueue; // Pending incidents


    public Scheduler(EventQueueManager receiveEventManager, EventQueueManager fireIncidentManager, EventQueueManager droneManager) {
        this.receiveEventManager = receiveEventManager;
        this.fireIncidentManager = fireIncidentManager;
        this.droneManager = droneManager;
        this.droneStates = new HashMap<>();
        this.incidentQueue = new LinkedList<>();
        this.sendEventManager = sendEventManager;
        this.drones = drones;
    }

    /**
     * Keeps listening for events
     */
    @Override
    public void run() {
        while (true) {
            IncidentEvent message = (IncidentEvent) receiveEventManager.get();

            if (message.getEventType() == EventType.EVENTS_DONE) {
                System.out.println("\nScheduler received EVENTS_DONE message. Forwarding to drones and shutting down...");
                droneManager.put(message);
                return;
            }

            // Handle drone status updates
            if (message.getEventType().toString().equals("DRONE_UPDATE")) {
                int droneId = message.getZoneID();  // Using zoneID as drone ID
                DroneStatus status = DroneStatus.fromString(message.getSeverity().toString());
                System.out.println("\nScheduler received drone status update: Drone " + droneId + " -> " + status);
                updateDroneStatus(droneId, status);
                continue;
            }

            System.out.println("\nScheduler received a fire incident: " + message);

            if (assignDroneToIncident(message)) {
                System.out.println("Incident assigned immediately: " + message);
            } else {
                System.out.println("No available drones. Incident added to queue: " + message);
                incidentQueue.add(message);
            }

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

    /**
     * Handles fire incident events, assigns available drones, and dispatches them.
     */
    private void handleIncidentEvent(IncidentEvent event) {
        // If event type is events_done, stop scheduler
        if (event.getEventType() == EventType.EVENTS_DONE) {
            System.out.println("\nScheduler received EVENTS_DONE.");
            droneManager.put(event);
            running = false;
            return;
        }
        System.out.println("\nScheduler received event: " + event);

        // Find available drone
        for (DroneState drone : drones) {
            if (drone.getStatus() == DroneStatus.IDLE) {
                // Get drone's coords
                Point2D droneCoords = drone.getCoordinates();

                // Send dispatch event
                DroneDispatchEvent dispatchEvent = new DroneDispatchEvent(event.getZoneID(), droneCoords);
                System.out.println("Scheduler dispatching drone to Zone: " + event.getZoneID() + " from " + droneCoords);
                droneManager.put(dispatchEvent);
                return;
            }
        }
        // Inform user if no drone is available
        System.out.println("No available drones");
    }

    private boolean hasEnoughBattery(DroneState drone, Point2D targetCoords){
        double distanceToTarget = drone.getCoordinates().distance(targetCoords);
        double distanceToBase = targetCoords.distance(new Point2D.Double(0,0));
        double travelTime = (((distanceToTarget + distanceToBase) - 46.875) / 15 + 6.25);

        return (drone.getFlightTime() - travelTime > 30); // use 30 sec limit for now
    }

    /**
     * Handle events when drone arrives at the fire zone
     */
    private boolean assignDroneToIncident(IncidentEvent incident) {
        for (Integer droneId : droneStates.keySet()) {
            if (droneStates.get(droneId) == DroneStatus.IDLE) {
                droneStates.put(droneId, DroneStatus.ON_ROUTE);
                droneManager.put(incident);
                System.out.println("Assigned Drone " + droneId + " to Incident at Zone " + incident.getZoneID());
                return true;
            }
        }
        return false;
    }

    public void updateDroneStatus(int droneId, DroneStatus status) {
        droneStates.put(droneId, status);
        if (status == DroneStatus.IDLE && !incidentQueue.isEmpty()) {
            IncidentEvent nextIncident = incidentQueue.poll();
            assignDroneToIncident(nextIncident);
        }
    }
}
