package main;

import events.*;
import subsystems.DroneState;
import subsystems.DroneStatus;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class Scheduler implements Runnable {
    private EventQueueManager receiveEventManager;
    private EventQueueManager fireIncidentManager;
    private EventQueueManager droneManager;
    private EventQueueManager sendEventManager;
    private ArrayList<DroneState> drones;
    private volatile boolean running = true;

    public Scheduler(EventQueueManager receiveEventManager, EventQueueManager fireIncidentManager, EventQueueManager droneManager, EventQueueManager sendEventManager, ArrayList<DroneState> drones) {
        this.receiveEventManager = receiveEventManager;
        this.fireIncidentManager = fireIncidentManager;
        this.droneManager = droneManager;
        this.sendEventManager = sendEventManager;
        this.drones = drones;
    }

    /**
     * Keeps listening for events
     */
    @Override
    public void run() {
        while (running) {
            try {
                // Retrieve next event from the queue
                Event message = receiveEventManager.get();

                // Handle fire incident events
                if (message instanceof IncidentEvent event) {
                    handleIncidentEvent(event);
                }
                // Handle drone arrival events
                else if (message instanceof DroneArrivedEvent arrivedEvent) {
                    handleDroneArrival(arrivedEvent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    /**
     * Handle events when drone arrives at the fire zone
     */
    private void handleDroneArrival(DroneArrivedEvent event) {
        System.out.println("Scheduler: Drone " + event.getDroneID() + " arrived at Zone " + event.getZoneID());

        // Notify drone to take out the fire
        DropAgentEvent dropEvent = new DropAgentEvent(16);
        droneManager.put(dropEvent);
    }
}
