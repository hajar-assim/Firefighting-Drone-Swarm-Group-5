package main;

import subsystems.Event;
import subsystems.EventType;
import subsystems.drone.DroneInfo;
import subsystems.drone.DroneSubsystem;
import subsystems.drone.events.*;
import subsystems.drone.states.DroneState;
import subsystems.drone.states.IdleState;
import subsystems.fire_incident.events.IncidentEvent;
import subsystems.fire_incident.events.Severity;
import subsystems.fire_incident.events.ZoneEvent;

import java.awt.geom.Point2D;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Scheduler {
    public static int sleepMultiplier = 1000; // adjust to speed up or slow down (more accurate) the run --> original value = 1000
    private EventSocket sendSocket;
    private EventSocket receiveSocket;
    private IncidentEvent unassignedTask = null;
    private HashMap<Integer, Point2D> fireZones; // Stores fire zone coordinates (zoneID, center)
    private HashMap<Integer, Integer> fireIncidents; // Tracks required water for each fire incident (zoneID, liters needed)
    private HashMap<Integer, Integer> droneAssignments; // Tracks which drone is assigned to which fire (droneID, zoneID)
    private InetAddress fireSubsystemAddress;
    private int fireSubsystemPort;
    private Map<Integer, DroneInfo> dronesInfo;
    private Map<Integer, InetAddress> droneAddresses;
    private Map<Integer, Integer> dronePorts;
    private volatile boolean running = true;
    private Queue<IncidentEvent> unassignedIncidents;

    /**
     * Constructor initializes event managers and HashMaps.
     */
    public Scheduler(InetAddress fireSubsystemAddress, int fireSubsystemPort, Map<Integer, DroneInfo> dronesInfo, Map<Integer, InetAddress> droneAddresses, Map<Integer, Integer> dronePorts) {
        this.sendSocket = new EventSocket();
        this.receiveSocket = new EventSocket(5000);
        this.fireZones = new HashMap<>();
        this.fireIncidents = new HashMap<>();
        this.droneAssignments = new HashMap<>();
        this.fireSubsystemAddress = fireSubsystemAddress;
        this.fireSubsystemPort = fireSubsystemPort;
        this.dronesInfo = dronesInfo;
        this.droneAddresses = droneAddresses;
        this.dronePorts = dronePorts;
        this.unassignedIncidents = new LinkedList<>();
    }

    /**
     * Continuously listens for incoming events and processes them.
     * It assigns tasks, handles events, and manages drone dispatches.
     */
    public void run() {
        while (running) {
            try {
                // attempt to assign task
                if (!unassignedIncidents.isEmpty()) {
                    attemptAssignUnassignedTask();
                }

                // Retrieve an event from the queue
                Event message = receiveSocket.receive();

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

    private void sendToDrone(Event event, int droneID){
        InetAddress address = droneAddresses.get(droneID);
        Integer port = dronePorts.get(droneID);

        if (address != null && port != null) {
            sendSocket.send(event, address, port);
        } else {
            System.err.println("[ERROR] Address not found for drone ID: " + droneID);
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

            for (int droneID : this.droneAddresses.keySet()) {
                sendToDrone(dispatchToBase, droneID);
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
            // Give drone some time to set its state to on route
            try{
                Thread.sleep(2000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }

            event.setEventType(EventType.DRONE_DISPATCHED);
            sendSocket.send(event, fireSubsystemAddress, fireSubsystemPort);
        } else {
            System.out.println("[SCHEDULER] No drones available for fire at zone " + event.getZoneID() + ", added to unassigned incident buffer for assignment when drone is available");

            // Add to buffer of unassigned incidents
            unassignedIncidents.add(event);
        }
    }

    /**
     * Attempts to assign unassigned tasks to available drones.
     *
     * @return true if at least one task is assigned, false otherwise.
     */
    private boolean attemptAssignUnassignedTask() {
        if (unassignedIncidents.isEmpty()) {
            return false;
        }

        // Assume failure to assign until a task is assigned
        boolean assignedTaskSuccessfully = false;

        // Process tasks in the queue
        while (!unassignedIncidents.isEmpty()) {
            // Get next task to assign
            IncidentEvent task = unassignedIncidents.peek();
            // Assign drone to task if drones are available
            boolean assigned = assignDrone(task.getZoneID());

            if (assigned) {
                System.out.println("[SCHEDULER] Assigned drone to waiting fire at Zone " + task.getZoneID());
                task.setEventType(EventType.DRONE_DISPATCHED);

                sendSocket.send(task, fireSubsystemAddress, fireSubsystemPort);
                // Remove task from buffer
                unassignedIncidents.poll();

                assignedTaskSuccessfully = true;

            } else {
                System.out.println("[SCHEDULER] No drones available for waiting fire at zone " + task.getZoneID());
                break;
            }
        }

        return assignedTaskSuccessfully;
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
        System.out.printf("[SCHEDULER] Dispatching Drone %d to Zone %d | Coordinates: (%.1f, %.1f)%n",
                droneID,
                zoneID,
                fireZoneCenter.getX(),
                fireZoneCenter.getY()
        );

        sendToDrone(dispatchEvent, droneID);

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
    private boolean hasEnoughBattery(DroneSubsystem drone, Point2D targetCoords){
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
    private int findAvailableDrone(Point2D fireZoneCenter) {
        for (int droneID : dronesInfo.keySet()) {
            DroneState droneState = dronesInfo.get(droneID).getState();

            if (droneState == null) {
                continue; // skip drones without a valid state
            }

            if (droneState instanceof IdleState) {
                System.out.println("[SCHEDULER] Found available idle drone: " + droneID);
                return droneID;
            }
        }
        return -1; // no available drone
    }


    /**
     * Handles the event when a drone arrives at a fire zone to drop water.
     *
     * @param event The DroneArrivedEvent containing the drone's arrival details.
     */
    private void handleDroneArrival(DroneArrivedEvent event) {
        int droneID = event.getDroneID();

        // ensure drone is assigned to a fire
        if (!droneAssignments.containsKey(droneID)) {
            System.out.println("[SCHEDULER] Unrecognized Drone Arrived Event.");
            return;
        }

        int zoneID = droneAssignments.get(droneID);

        // ensure fire incident exists for the zone
        if (!fireIncidents.containsKey(zoneID)) {
            System.out.println("[SCHEDULER] Error: Drone " + droneID + " arrived at untracked Zone " + zoneID);
            return;
        }

        // calculate how much water to drop
        int waterToDrop = Math.min(fireIncidents.get(zoneID), dronesInfo.get(droneID).getWaterLevel());
        System.out.println("[SCHEDULER] Ordering Drone " + droneID + " to drop " + waterToDrop + "L at Zone " + zoneID);

        // send drop event to drone
        DropAgentEvent dropEvent = new DropAgentEvent(waterToDrop);
        sendToDrone(dropEvent,droneID);
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
            IncidentEvent fireOutEvent = new IncidentEvent("", zoneID, EventType.FIRE_EXTINGUISHED, Severity.NONE);
            sendSocket.send(fireOutEvent, fireSubsystemAddress, fireSubsystemPort);
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

        // retrieve necessary drone data
        int droneID = event.getDroneID();
        DroneInfo drone = event.getDroneInfo();
        dronesInfo.put(droneID, drone);
        if (drone == null) {
            System.out.println("[SCHEDULER] Error: Received update for unknown drone ID " + droneID);
            return;
        }

        // getting drone state
        DroneState currentState = drone.getState();
        System.out.println("\n[SCHEDULER] Received update: Drone " + droneID + " is now in state " + currentState.getClass().getSimpleName());

        // attempt to reassign drone if idle
        if (currentState instanceof IdleState) {

            // prevent double assignment
            if (droneAssignments.containsKey(droneID)) {
                return;
            }

            // assign to an ongoing fire that needs water
            for (Map.Entry<Integer, Integer> entry : fireIncidents.entrySet()) {
                int zoneID = entry.getKey();
                int remainingWater = entry.getValue();

                if (remainingWater > 0 && !droneAssignments.containsValue(zoneID)) {
                    System.out.println("[SCHEDULER] Reassigning Drone " + droneID + " to continue fire suppression at Zone " + zoneID);
                    assignDrone(zoneID);
                    return; // stop once a fire has been assigned
                }
            }

            // if no ongoing fires, check for unassigned tasks
            if (unassignedTask != null) {
                System.out.println("[SCHEDULER] No active fires, assigning waiting fire at zone " + unassignedTask.getZoneID());
                boolean assigned = assignDrone(unassignedTask.getZoneID());

                if (assigned) {
                    unassignedTask.setEventType(EventType.DRONE_DISPATCHED);
                    sendSocket.send(unassignedTask, fireSubsystemAddress, fireSubsystemPort);
                    unassignedTask = null;
                }
            }
        }
    }

    public static void main(String args[]) {
        InetAddress address = null;
        try{
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Map<Integer, DroneInfo> dronesInfo = new HashMap<>();
        Map<Integer, InetAddress> droneAddresses = new HashMap<>();
        Map<Integer, Integer> dronePorts = new HashMap<>();

        for(int i = 1; i <= 1; i++){
            dronesInfo.put(i, new DroneInfo());
            droneAddresses.put(i, address);
            dronePorts.put(i, 6000 + i);
        }

        Scheduler scheduler = new Scheduler(address, 7000, dronesInfo, droneAddresses, dronePorts);
        scheduler.run();
    }
}
