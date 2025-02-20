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

    public Scheduler(EventQueueManager receiveEventManager, EventQueueManager fireIncidentManager, EventQueueManager droneManager) {
        this.receiveEventManager = receiveEventManager;
        this.fireIncidentManager = fireIncidentManager;
        this.droneManager = droneManager;
        this.sendEventManager = sendEventManager;
        this.drones = new ArrayList<>();

        // Init single drone
        drones.add(new DroneState(DroneStatus.IDLE, 0, new Point2D.Double(0, 0), 100, 15));
    }

    /**
     * Main loop that listens for events and processes them.
     */
    @Override
    public void run() {
        while (running) {
            try {
                Event message = receiveEventManager.get();

                if (message instanceof IncidentEvent event) {
                    handleIncidentEvent(event);
                } else if (message instanceof DroneArrivedEvent arrivedEvent) {
                    handleDroneArrival(arrivedEvent);
                } else if (message instanceof DropAgentEvent dropEvent) {
                    handleDropAgent(dropEvent);
                }
            } catch (Exception e) {
                System.err.println("Scheduler encountered an error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles fire incident events -> assigns available drones -> and dispatches them.
     */
    private void handleIncidentEvent(IncidentEvent event) {
        if (event.getEventType() == EventType.EVENTS_DONE) {
            System.out.println("\nScheduler received EVENTS_DONE.");
            droneManager.put(event);
            running = false;
            return;
        }

        System.out.println("\nScheduler received event: " + event);

        // Find available idle drone
        DroneState selectedDrone = drones.get(0);
        if (selectedDrone.getStatus() != DroneStatus.IDLE) {
            System.out.println("No available drones. Queuing event...");
            return;
        }

        // Assign drone and update status
        selectedDrone.setStatus(DroneStatus.ON_ROUTE);
        selectedDrone.setCoordinates(new Point2D.Double(12, 40));
        selectedDrone.setZoneID(event.getZoneID());

        // Dispatch event to DroneSubsystem
        DroneDispatchEvent dispatchEvent = new DroneDispatchEvent(event.getZoneID(), selectedDrone.getCoordinates());
        System.out.println("Scheduler dispatching drone to Zone: " + event.getZoneID());
        droneManager.put(dispatchEvent);
    }

    /**
     * Handles events when a drone arrives at its assigned fire zone.
     */
    private void handleDroneArrival(DroneArrivedEvent event) {
        System.out.println("Scheduler: Drone " + event.getDroneID() + " arrived at Zone " + event.getZoneID());

        DroneState drone = drones.get(0);
        if (drone.getZoneID() == event.getZoneID()) {
            drone.setStatus(DroneStatus.DROPPING_AGENT);
        }
    }

    /**
     * Handles events when a drone completes dropping the firefighting agent.
     */
    private void handleDropAgent(DropAgentEvent event) {
        System.out.println("Scheduler: Drop finished, returning drone.");

        DroneState drone = drones.get(0);
        if (drone.getStatus() == DroneStatus.DROPPING_AGENT) {
            drone.setStatus(DroneStatus.IDLE);
            drone.setCoordinates(new Point2D.Double(0, 0)); // Return to base
        }
    }
}
