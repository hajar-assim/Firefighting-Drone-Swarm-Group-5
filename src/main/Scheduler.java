package main;

import events.*;
import subsystems.DroneState;
import subsystems.DroneStatus;
import java.awt.geom.Point2D;
import java.util.HashMap;

public class Scheduler implements Runnable {
    private EventQueueManager receiveEventManager;
    private EventQueueManager fireIncidentManager;
    private EventQueueManager droneManager;
    private EventQueueManager sendEventManager;

    // Stores fire zone coordinates (zoneID, center)
    private HashMap<Integer, Point2D> fireZones;

    // Tracks required water for each fire incident (zoneID, liters needed)
    private HashMap<Integer, Integer> fireIncidents;

    // Tracks which drone is assigned to which fire (droneID, zoneID)
    private HashMap<Integer, Integer> droneAssignments;

    // Stores drones by their unique ID (droneID, DroneState)
    private HashMap<Integer, DroneState> dronesByID;

    private volatile boolean running = true;
    private final int MAX_DRONE_CAPACITY = 15;
    // Last drone that dropped water
    private int lastDroneToDrop = -1;


    /**
     * Constructor initializes event managers and HashMaps.
     */
    public Scheduler(EventQueueManager receiveEventManager, EventQueueManager fireIncidentManager, EventQueueManager droneManager, EventQueueManager sendEventManager) {
        this.receiveEventManager = receiveEventManager;
        this.fireIncidentManager = fireIncidentManager;
        this.droneManager = droneManager;
        this.sendEventManager = sendEventManager;
        this.fireZones = new HashMap<>();
        this.fireIncidents = new HashMap<>();
        this.droneAssignments = new HashMap<>();
        this.dronesByID = new HashMap<>();
    }

    /**
     * Continuously listens for incoming events and processes them.
     */
    @Override
    public void run() {
        while (running) {
            try {
                // Retrieve an event from the queue
                Event message = receiveEventManager.get();

                // Handle event based on its type
                if (message instanceof ZoneEvent zoneEvent) {
                    storeZoneData(zoneEvent);
                } else if (message instanceof IncidentEvent incidentEvent) {
                    handleIncidentEvent(incidentEvent);
                } else if (message instanceof DroneArrivedEvent arrivedEvent) {
                    handleDroneArrival(arrivedEvent);
                } else if (message instanceof DropAgentEvent dropEvent) {
                    handleDropAgent(dropEvent);
                } else if (message instanceof DroneUpdateEvent updateEvent) {
                    handleDroneUpdate(updateEvent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stores fire zone data from ZoneEvent.
     * Maps the fire zone ID to its center coordinates.
     */
    private void storeZoneData(ZoneEvent event) {
        fireZones.put(event.getZoneID(), event.getCenter());
        System.out.println("Stored fire zone: Zone " + event.getZoneID() + ", Center: " + event.getCenter());
    }

    /**
     * Handles fire incident events by determining required water and dispatching drones.
     */
    private void handleIncidentEvent(IncidentEvent event) {
        if (event.getEventType() == EventType.EVENTS_DONE) {
            System.out.println("\nScheduler received EVENTS_DONE.");
            droneManager.put(event);
            running = false;
            return;
        }

        // Determine water requirement based on severity
        int requiredWater = switch (event.getSeverity()) {
            case LOW -> 10;
            case MODERATE -> 20;
            case HIGH -> 30;
        };

        System.out.println("\nNew fire incident at Zone " + event.getZoneID() + ". Requires " + requiredWater + "L of water.");

        // Add zone id and amount of water required to fireIncidents hashmap
        fireIncidents.put(event.getZoneID(), requiredWater);

        // Assign a drone to handle the fire
        assignDrone(event.getZoneID());
    }

    private boolean hasEnoughBattery(DroneState drone, Point2D targetCoords){
        double distanceToTarget = drone.getCoordinates().distance(targetCoords);
        double distanceToBase = targetCoords.distance(new Point2D.Double(0,0));
        double travelTime = (((distanceToTarget + distanceToBase) - 46.875) / 15 + 6.25);

        return (drone.getFlightTime() - travelTime > 30); // use 30 sec limit for now
    }

    /**
     * Assigns an available drone to a fire zone.
     */
    private void assignDrone(int zoneID) {
        if (!fireZones.containsKey(zoneID)) {
            System.out.println("Error: Fire zone center not found for Zone " + zoneID);
            return;
        }

        // Iterate through all drones and find an available one
        for (int droneID : dronesByID.keySet()) {
            DroneState drone = dronesByID.get(droneID);

            // Assign only idle drones with sufficient water
            if (drone.getStatus() == DroneStatus.IDLE && drone.getWaterLevel() > 0) {

                // Get firezone center through its zone id
                Point2D fireZoneCenter = fireZones.get(zoneID);

                // Send dispatch event to drone
                DroneDispatchEvent dispatchEvent = new DroneDispatchEvent(zoneID, fireZoneCenter);
                System.out.println("Dispatching Drone " + droneID + " to Zone " + zoneID + " at " + fireZoneCenter);
                droneManager.put(dispatchEvent);

                // Track assignment of drone to zones
                droneAssignments.put(droneID, zoneID);
                return;
            }
        }
        // If no drones available then inform user
        System.out.println("No available drones to dispatch.");
    }

    /**
     * Update last known drone when ordering a drop
     */
    private void handleDroneArrival(DroneArrivedEvent event) {
        int droneID = event.getDroneID();

        // Store which drone is currently handling the drop
        lastDroneToDrop = droneID;

        // Ensure drone is assigned to a fire
        if (!droneAssignments.containsKey(droneID)) {
            System.out.println("Unrecognized Drone Arrived Event.");
            return;
        }

        int zoneID = droneAssignments.get(droneID);

        // Ensure fire incident exists for the zone
        if (!fireIncidents.containsKey(zoneID)) {
            System.out.println("Error: Drone " + droneID + " arrived at untracked Zone " + zoneID);
            return;
        }

        // Calc how much water to drop
        int waterNeeded = fireIncidents.get(zoneID);
        int droneWater = dronesByID.get(droneID).getWaterLevel();
        int waterToDrop = Math.min(waterNeeded, Math.min(droneWater, MAX_DRONE_CAPACITY));

        System.out.println("Ordering Drone " + droneID + " to drop " + waterToDrop + "L at Zone " + zoneID);

        // Send drop event to drone
        DropAgentEvent dropEvent = new DropAgentEvent(waterToDrop);
        droneManager.put(dropEvent);
    }

    /**
     * Handles a DropAgentEvent.
     */
    private void handleDropAgent(DropAgentEvent event) {
        int droneID = getDroneIDForDropEvent(event);

        // Get zone id using drone id key
        int zoneID = droneAssignments.get(droneID);

        if (!fireIncidents.containsKey(zoneID)) {
            System.out.println("DropAgentEvent received for unknown zone " + zoneID);
            return;
        }

        // Subtract the dropped water from the fire requirement
        int waterDropped = event.getVolume();
        int remainingWater = fireIncidents.get(zoneID) - waterDropped;

        // If required water vol has been used then remove the incident from the incident list and unassign drone from zone id
        if (remainingWater <= 0) {
            System.out.println("Fire at Zone " + zoneID + " is now extinguished.");
            fireIncidents.remove(zoneID);
            droneAssignments.remove(droneID);
        } else {
            // Otherwise update remaining water and assign another drone to zone
            fireIncidents.put(zoneID, remainingWater);
            System.out.println("Fire at Zone " + zoneID + " still needs " + remainingWater + "L of water to extinguish.");
            assignDrone(zoneID);
        }
    }

    /**
     * Handles drone updates.
     */
    private void handleDroneUpdate(DroneUpdateEvent event) {
        int droneID = event.getDroneID();
        DroneState droneState = event.getDroneState();


        // Update stored drone state
        dronesByID.put(droneID, droneState);

        // If drone is refilling then remove it from assignments
        if (droneState.getStatus() == DroneStatus.REFILLING) {
            droneAssignments.remove(droneID);
            System.out.println("Drone " + droneID + " is refilling. Removed from assignments.");
        }
    }

    /**
     * Handles a DropAgentEvent by identifying the drone that sent it.
     */
    private int getDroneIDForDropEvent(DropAgentEvent event) {
        if (lastDroneToDrop != -1) {
            // Return last drone that dropped water
            return lastDroneToDrop;
        }

        return -1;
    }
}
