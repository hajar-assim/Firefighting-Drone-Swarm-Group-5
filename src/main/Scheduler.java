package main;

import events.*;
import subsystems.DroneState;
import subsystems.DroneStatus;
import subsystems.DroneSubsystem;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Scheduler implements Runnable {
    private EventQueueManager receiveEventManager;
    private EventQueueManager fireIncidentManager;

    // Stores fire zone coordinates (zoneID, center)
    private HashMap<Integer, Point2D> fireZones;

    // Tracks required water for each fire incident (zoneID, liters needed)
    private HashMap<Integer, Integer> fireIncidents;

    // Tracks which drone is assigned to which fire (droneID, zoneID)
    private HashMap<Integer, Integer> droneAssignments;

    // Stores drones by their unique ID (droneID, DroneState)
    private Map<Integer, Map.Entry<DroneState, EventQueueManager>> dronesByID;
    private volatile boolean running = true;

    /**
     * Constructor initializes event managers and HashMaps.
     */
    public Scheduler(EventQueueManager receiveEventManager, EventQueueManager fireIncidentManager, Map<Integer, Map.Entry<DroneState, EventQueueManager>> drones) {
        this.receiveEventManager = receiveEventManager;
        this.fireIncidentManager = fireIncidentManager;
        this.fireZones = new HashMap<>();
        this.fireIncidents = new HashMap<>();
        this.droneAssignments = new HashMap<>();
        this.dronesByID = drones;
    }

    private EventQueueManager getDroneManager(int droneID){
        return this.dronesByID.get(droneID).getValue();
    }

    private DroneState getDroneState(int droneID){
        return this.dronesByID.get(droneID).getKey();
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
            DroneDispatchEvent dispatchToBase = new DroneDispatchEvent(0, new Point2D.Double(0,0));

            for(int droneID : this.dronesByID.keySet()) {
                this.getDroneManager(droneID).put(dispatchToBase);
            }
            running = false;
            return;
        }

        if (!fireZones.containsKey(event.getZoneID())) {
            System.out.println("Error: Fire zone center not found for Zone " + event.getZoneID());
            return;
        }

        // Determine water requirement based on severity
        int requiredWater = event.getSeverity().getWaterFoamAmount();
        System.out.println("\nNew fire incident at Zone " + event.getZoneID() + ". Requires " + requiredWater + "L of water.");

        // Add zone id and amount of water required to fireIncidents hashmap
        fireIncidents.put(event.getZoneID(), requiredWater);
        this.assignDrone(event.getZoneID());

        event.setEventType(EventType.DRONE_DISPATCHED);
        this.fireIncidentManager.put(event);
    }


    /**
     * Assigns an available drone to a fire zone.
     */
    private void assignDrone(int zoneID) {

        Point2D fireZoneCenter = this.fireZones.get(zoneID);
        int droneID = this.findAvailableDrone(fireZoneCenter);

        System.out.println("Scheduler finding an available drone...");
        while (droneID == -1){
            try {
                Thread.sleep(2000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            droneID = this.findAvailableDrone(fireZoneCenter);
        }

        // Send dispatch event to drone
        DroneDispatchEvent dispatchEvent = new DroneDispatchEvent(zoneID, fireZoneCenter);
        System.out.println("Dispatching Drone " + droneID + " to Zone " + zoneID + " at " + fireZoneCenter);
        this.getDroneManager(droneID).put(dispatchEvent);

        // Track assignment of drone to zones
        droneAssignments.put(droneID, zoneID);
    }

    private boolean hasEnoughBattery(DroneState drone, Point2D targetCoords){
        double distanceToTarget = drone.getCoordinates().distance(targetCoords);
        double distanceToBase = targetCoords.distance(new Point2D.Double(0,0));
        double travelTime = (((distanceToTarget + distanceToBase) - 46.875) / 15 + 6.25);

        return (drone.getFlightTime() - travelTime > 30); // use 30 sec limit for now
    }

    private int findAvailableDrone(Point2D fireZoneCenter){
        // Iterate through all drones and find an available one
        for (int droneID : this.dronesByID.keySet()) {
            DroneState drone = this.getDroneState(droneID);

            // Assign only idle drones with sufficient water
            if (drone.getStatus() == DroneStatus.IDLE && drone.getWaterLevel() > 0 && this.hasEnoughBattery(drone, fireZoneCenter)) {
                return droneID;
            }
        }
        return -1;
    }

    /**
     * Update last known drone when ordering a drop
     */
    private void handleDroneArrival(DroneArrivedEvent event) {
        int droneID = event.getDroneID();

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
        int waterToDrop = Math.min(fireIncidents.get(zoneID), dronesByID.get(droneID).getKey().getWaterLevel());
        System.out.println("Ordering Drone " + droneID + " to drop " + waterToDrop + "L at Zone " + zoneID);

        // Send drop event to drone
        DropAgentEvent dropEvent = new DropAgentEvent(waterToDrop);
        this.getDroneManager(droneID).put(dropEvent);
    }

    /**
     * Handles a DropAgentEvent.
     */
    private void handleDropAgent(DropAgentEvent event) {
        int droneID = event.getDroneID();

        // Get zone id using drone id key
        int zoneID = droneAssignments.get(droneID);

        if (!fireIncidents.containsKey(zoneID)) {
            System.out.println("DropAgentEvent received for unknown zone " + zoneID);
            return;
        }

        // Subtract the dropped water from the fire requirement
        int remainingWater = fireIncidents.get(zoneID) - event.getVolume();

        // If required water vol has been used then remove the incident from the incident list and unassign drone from zone id
        if (remainingWater <= 0) {
            System.out.println("Fire at Zone " + zoneID + " is now extinguished.");
            fireIncidents.remove(zoneID);
            droneAssignments.remove(droneID);
        } else {
            // Otherwise update remaining water and assign another drone to zone
            fireIncidents.put(zoneID, remainingWater);
            droneAssignments.remove(droneID);
            System.out.println("Fire at Zone " + zoneID + " still needs " + remainingWater + "L of water to extinguish.");
            assignDrone(zoneID);
        }
    }

}
