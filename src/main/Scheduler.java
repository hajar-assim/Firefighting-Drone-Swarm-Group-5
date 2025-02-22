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
    private IncidentEvent unassignedTask = null;
    private HashMap<Integer, Point2D> fireZones; // Stores fire zone coordinates (zoneID, center)
    private HashMap<Integer, Integer> fireIncidents; // Tracks required water for each fire incident (zoneID, liters needed)
    private HashMap<Integer, Integer> droneAssignments; // Tracks which drone is assigned to which fire (droneID, zoneID)
    private Map<Integer, Map.Entry<DroneState, EventQueueManager>> dronesByID; // Stores drones by their unique ID (droneID, DroneState)
    private volatile boolean running = true;

    /**
     * Constructor initializes event managers and HashMaps.
     *
     * @param receiveEventManager The event manager for receiving events.
     * @param fireIncidentManager The event manager for fire incident events.
     * @param drones A map of drone IDs to their corresponding state and event manager.
     */
    public Scheduler(EventQueueManager receiveEventManager, EventQueueManager fireIncidentManager, Map<Integer, Map.Entry<DroneState, EventQueueManager>> drones) {
        this.receiveEventManager = receiveEventManager;
        this.fireIncidentManager = fireIncidentManager;
        this.fireZones = new HashMap<>();
        this.fireIncidents = new HashMap<>();
        this.droneAssignments = new HashMap<>();
        this.dronesByID = drones;
    }

    /**
     * Gets the event manager for a specific drone.
     *
     * @param droneID The unique ID of the drone.
     * @return The EventQueueManager for the specified drone.
     */
    private EventQueueManager getDroneManager(int droneID){
        return this.dronesByID.get(droneID).getValue();
    }

    /**
     * Gets the state of a specific drone.
     *
     * @param droneID The unique ID of the drone.
     * @return The DroneState for the specified drone.
     */
    private DroneState getDroneState(int droneID){
        return this.dronesByID.get(droneID).getKey();
    }

    /**
     * Continuously listens for incoming events and processes them.
     * It assigns tasks, handles events, and manages drone dispatches.
     */
    @Override
    public void run() {
        while (running) {
            try {
                // attempt to assign task
                if (unassignedTask != null) {
                    attemptAssignUnassignedTask();
                }

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
     * Stores fire zone data from a ZoneEvent.
     * Maps the fire zone ID to its center coordinates.
     *
     * @param event The ZoneEvent containing fire zone data.
     */
    private void storeZoneData(ZoneEvent event) {
        fireZones.put(event.getZoneID(), event.getCenter());
        System.out.printf("[SCHEDULER] Stored fire zone {Zone: %d | Center: (%.1f, %.1f)}%n",
                event.getZoneID(),
                event.getCenter().getX(),
                event.getCenter().getY()
        );
    }

    /**
     * Handles fire incident events by determining required water and dispatching drones.
     * If no drone is available, the task is stored for later assignment.
     *
     * @param event The IncidentEvent containing fire incident data.
     */
    private void handleIncidentEvent(IncidentEvent event) {

        if (event.getEventType() == EventType.EVENTS_DONE) {
            System.out.println("\n[SCHEDULER] Received EVENTS_DONE.");
            DroneDispatchEvent dispatchToBase = new DroneDispatchEvent(0, new Point2D.Double(0,0));

            for (int droneID : this.dronesByID.keySet()) {
                this.getDroneManager(droneID).put(dispatchToBase);
            }
            running = false;
            return;
        }

        if (!fireZones.containsKey(event.getZoneID())) {
            System.out.println("[SCHEDULER] Error: Fire zone center not found for Zone " + event.getZoneID());
            return;
        }

        int requiredWater = event.getSeverity().getWaterFoamAmount();
        System.out.println("\n[SCHEDULER] New fire incident at Zone " + event.getZoneID() + ". Requires " + requiredWater + "L of water.");
        fireIncidents.put(event.getZoneID(), requiredWater);

        boolean assigned = assignDrone(event.getZoneID());

        if (assigned) {
            event.setEventType(EventType.DRONE_DISPATCHED);
            fireIncidentManager.put(event);
        } else {
            System.out.println("[SCHEDULER] No drone available. Storing task for later assignment.");
            unassignedTask = event;
        }
    }

    /**
     * Attempts to assign an unassigned task to an available drone.
     *
     * @return true if the task is assigned, false otherwise.
     */
    private boolean attemptAssignUnassignedTask() {
        if (unassignedTask == null) return false;

        // Check if there are any ongoing fires left before assigning waiting tasks
        for (int zoneID : fireIncidents.keySet()) {
            if (fireIncidents.get(zoneID) > 0) {
                return false; // do not assign a new task while active fires exist
            }
        }

        boolean assigned = assignDrone(unassignedTask.getZoneID());
        if (assigned) {
            System.out.println("[SCHEDULER] Assigned drone to waiting fire at Zone " + unassignedTask.getZoneID());
            unassignedTask.setEventType(EventType.DRONE_DISPATCHED);
            fireIncidentManager.put(unassignedTask);
            unassignedTask = null;
            return true;
        }

        return false;
    }

    /**
     * Assigns an available drone to a fire zone.
     *
     * @param zoneID The ID of the fire zone that needs assistance.
     * @return true if a drone is successfully assigned, false otherwise.
     */
    private boolean assignDrone(int zoneID) {
        Point2D fireZoneCenter = this.fireZones.get(zoneID);
        int droneID = this.findAvailableDrone(fireZoneCenter);

        // no drone available
        if (droneID == -1) {
            return false;
        }

        // send dispatch event to the drone
        DroneDispatchEvent dispatchEvent = new DroneDispatchEvent(zoneID, fireZoneCenter);
        System.out.println(
                String.format("[SCHEDULER] Dispatching Drone %d to Zone %d | Coordinates: (%.1f, %.1f)",
                        droneID,
                        zoneID,
                        fireZoneCenter.getX(),
                        fireZoneCenter.getY()
                )
        );
        this.getDroneManager(droneID).put(dispatchEvent);

        // track drone assignment
        droneAssignments.put(droneID, zoneID);
        return true;
    }

    /**
     * Checks if the drone has enough battery to complete a round trip to the target coordinates.
     *
     * @param drone The drone to check.
     * @param targetCoords The coordinates of the target zone.
     * @return true if the drone has enough battery, false otherwise.
     */
    private boolean hasEnoughBattery(DroneState drone, Point2D targetCoords){
        double distanceToTarget = drone.getCoordinates().distance(targetCoords);
        double distanceToBase = targetCoords.distance(new Point2D.Double(0,0));
        double travelTime = (((distanceToTarget + distanceToBase) - 46.875) / 15 + 6.25);

        return (drone.getFlightTime() - travelTime > 30); // use 30 sec limit for now
    }

    /**
     * Finds an available drone with sufficient battery and water level to complete the task.
     *
     * @param fireZoneCenter The coordinates of the fire zone.
     * @return The ID of the available drone, or -1 if no drone is available.
     */
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
     * Handles the event when a drone arrives at a fire zone to drop water.
     *
     * @param event The DroneArrivedEvent containing the drone's arrival details.
     */
    private void handleDroneArrival(DroneArrivedEvent event) {
        int droneID = event.getDroneID();

        // Ensure drone is assigned to a fire
        if (!droneAssignments.containsKey(droneID)) {
            System.out.println("[SCHEDULER] Unrecognized Drone Arrived Event.");
            return;
        }

        int zoneID = droneAssignments.get(droneID);

        // Ensure fire incident exists for the zone
        if (!fireIncidents.containsKey(zoneID)) {
            System.out.println("[SCHEDULER] Error: Drone " + droneID + " arrived at untracked Zone " + zoneID);
            return;
        }

        // Calc how much water to drop
        int waterToDrop = Math.min(fireIncidents.get(zoneID), dronesByID.get(droneID).getKey().getWaterLevel());
        System.out.println("[SCHEDULER] Ordering Drone " + droneID + " to drop " + waterToDrop + "L at Zone " + zoneID);

        // Send drop event to drone
        DropAgentEvent dropEvent = new DropAgentEvent(waterToDrop);
        this.getDroneManager(droneID).put(dropEvent);
    }

    /**
     * Handles a DropAgentEvent, updating the fire incident data and reassigning drones if necessary.
     *
     * @param event The DropAgentEvent containing the details of the water drop.
     */
    private void handleDropAgent(DropAgentEvent event) {
        int droneID = event.getDroneID();

        // Get zone id using drone id key
        int zoneID = droneAssignments.get(droneID);

        if (!fireIncidents.containsKey(zoneID)) {
            System.out.println("[SCHEDULER] DropAgentEvent received for unknown zone " + zoneID);
            return;
        }

        // Subtract the dropped water from the fire requirement
        int remainingWater = fireIncidents.get(zoneID) - event.getVolume();

        // If required water vol has been used then remove the incident from the incident list and unassign drone from zone id
        if (remainingWater <= 0) {
            fireIncidents.remove(zoneID);
            droneAssignments.remove(droneID);
            // notify FireIncidentSubSystem that the fire has been put out
            IncidentEvent fireOutEvent = new IncidentEvent("", zoneID, "FIRE_EXTINGUISHED", "NONE");
            fireIncidentManager.put(fireOutEvent);
        } else {
            // Otherwise update remaining water and assign another drone to zone
            fireIncidents.put(zoneID, remainingWater);
            droneAssignments.remove(droneID);
            System.out.println("[SCHEDULER] Fire at Zone " + zoneID + " still needs " + remainingWater + "L of water to extinguish.");
            assignDrone(zoneID);
        }
    }

    /**
     * Handles a DroneUpdateEvent, updating the drone's status and potentially assigning it a new task.
     *
     * @param event The DroneUpdateEvent containing the updated drone details.
     */
    private void handleDroneUpdate(DroneUpdateEvent event) {
        int droneID = event.getDroneID();
        DroneStatus status = event.getDroneState().getStatus();

        System.out.println("\n[SCHEDULER] Received update: Drone " + droneID + " is now " + status);

        if (status == DroneStatus.IDLE) {
            // prevent double assignment
            if (droneAssignments.containsKey(droneID)) {
                return;
            }

            // First, check ongoing fires that still need water
            for (Map.Entry<Integer, Integer> entry : fireIncidents.entrySet()) {
                int zoneID = entry.getKey();
                int remainingWater = entry.getValue();

                if (remainingWater > 0) {
                    System.out.println("[SCHEDULER] Reassigning Drone " + droneID + " to continue fire suppression at Zone " + zoneID);
                    assignDrone(zoneID);
                    return; // stop once a fire has been assigned
                }
            }

            // If no ongoing fires, check for waiting fires
            if (unassignedTask != null) {
                System.out.println("[SCHEDULER] no active fires, assigning waiting fire at zone " + unassignedTask.getZoneID());
                boolean assigned = assignDrone(unassignedTask.getZoneID());

                if (assigned) {
                    unassignedTask.setEventType(EventType.DRONE_DISPATCHED);
                    fireIncidentManager.put(unassignedTask);
                    unassignedTask = null;
                }
            }
        }
    }
}
