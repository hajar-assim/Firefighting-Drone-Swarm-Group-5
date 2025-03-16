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
import java.util.concurrent.atomic.AtomicInteger;

public class Scheduler {
    private static final AtomicInteger nextDroneId = new AtomicInteger(1);
    public static int sleepMultiplier = 500; // adjust to speed up or slow down (more accurate) the run --> original value = 1000
    private final EventSocket sendSocket;
    private final EventSocket receiveSocket;
    private final HashMap<Integer, Point2D> fireZones; // Stores fire zone coordinates (zoneID, center)
    private final HashMap<Integer, IncidentEvent> droneAssignments; // Tracks which drone is assigned to which Incident
    private final InetAddress fireSubsystemAddress;
    private final int fireSubsystemPort;
    private final Map<Integer, DroneInfo> dronesInfo;
    private volatile boolean running = true;
    private final Queue<IncidentEvent> unassignedIncidents;

    /**
     * Constructor initializes event managers and HashMaps.
     */
    public Scheduler(InetAddress fireSubsystemAddress, int fireSubsystemPort) {
        this.sendSocket = new EventSocket();
        this.receiveSocket = new EventSocket(5000);
        this.fireZones = new HashMap<>();
        this.droneAssignments = new HashMap<>();
        this.fireSubsystemAddress = fireSubsystemAddress;
        this.fireSubsystemPort = fireSubsystemPort;
        this.dronesInfo = new HashMap<>();
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
                System.err.println("Issue handling message: " + e.getMessage());
                e.printStackTrace();
            }
        }

        this.receiveSocket.getSocket().close();
        this.sendSocket.getSocket().close();
    }

    private void sendToDrone(Event event, int droneID){
        InetAddress address = dronesInfo.get(droneID).getAddress();
        Integer port = dronesInfo.get(droneID).getPort();

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

            for (int droneID : this.dronesInfo.keySet()) {
                sendToDrone(dispatchToBase, droneID);
            }
            running = false;
            return;
        }

        if (!fireZones.containsKey(event.getZoneID())) {
            System.out.println("[SCHEDULER] Error: Fire zone center not found for Zone " + event.getZoneID());
            return;
        }

        System.out.println("\n[SCHEDULER] New fire incident at Zone " + event.getZoneID() + ". Requires " + event.getWaterFoamAmount() + "L of water.");

        if (assignDrone(event)) {
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
     */
    private void attemptAssignUnassignedTask() {
        if (unassignedIncidents.isEmpty()) {
            return;
        }

        // Process tasks in the queue
        while (!unassignedIncidents.isEmpty()) {
            // Get next task to assign
            IncidentEvent task = unassignedIncidents.peek();
            // Assign drone to task if drones are available
            boolean assigned = assignDrone(task);

            if (assigned) {
                // Remove task from buffer
                unassignedIncidents.poll();
                task.setEventType(EventType.DRONE_DISPATCHED);
                sendSocket.send(task, fireSubsystemAddress, fireSubsystemPort);

            } else {
                break;
            }
        }

    }

    /**
     * Assigns an available drone to a fire zone.
     *
     * @param task The Incident event needed to be assigned.
     * @return true if a drone is successfully assigned, false otherwise.
     */
    private boolean assignDrone(IncidentEvent task) {
        int zoneID = task.getZoneID();
        Point2D fireZoneCenter = this.fireZones.get(zoneID);
        int droneID = this.findAvailableDrone();

        // no drone available
        if (droneID == -1) {
            return false;
        }

        // track drone assignment
        droneAssignments.put(droneID, task);
        System.out.println("[SCHEDULER] Assigned drone to waiting fire at Zone " + task.getZoneID());

        // send dispatch event to the drone
        DroneDispatchEvent dispatchEvent = new DroneDispatchEvent(zoneID, fireZoneCenter);
        System.out.printf("[SCHEDULER] Dispatching Drone %d to Zone %d | Coordinates: (%.1f, %.1f)%n",
                droneID,
                zoneID,
                fireZoneCenter.getX(),
                fireZoneCenter.getY()
        );

        sendToDrone(dispatchEvent, droneID);
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
     * @return The ID of the available drone, or -1 if no drone is available.
     */
    private int findAvailableDrone() {
        for (int droneID : dronesInfo.keySet()) {
            DroneState droneState = dronesInfo.get(droneID).getState();

            if (droneState == null) {
                continue; // skip drones without a valid state
            }

            if (droneState instanceof IdleState && !droneAssignments.containsKey(droneID)) {
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

        IncidentEvent incident = droneAssignments.get(droneID);

        // calculate how much water to drop
        int waterToDrop = Math.min(incident.getWaterFoamAmount(), dronesInfo.get(droneID).getWaterLevel());
        System.out.println("[SCHEDULER] Ordering Drone " + droneID + " to drop " + waterToDrop + "L at Zone " + incident.getZoneID());

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
        IncidentEvent incident = droneAssignments.get(droneID);


        // Subtract the dropped water from the fire requirement
        int remainingWater = incident.getWaterFoamAmount() - event.getVolume();

        // If required water vol has been used then remove the incident from the incident list and unassign drone from zone id
        if (remainingWater <= 0) {
            droneAssignments.remove(droneID);
            // notify FireIncidentSubSystem that the fire has been put out
            IncidentEvent fireOutEvent = new IncidentEvent("", incident.getZoneID(), EventType.FIRE_EXTINGUISHED, Severity.NONE);
            sendSocket.send(fireOutEvent, fireSubsystemAddress, fireSubsystemPort);
        } else {
            // Otherwise update remaining water and put incident in buffer
            incident.setWaterFoamAmount(remainingWater);
            droneAssignments.remove(droneID);
            System.out.println("[SCHEDULER] Fire at Zone " + incident.getZoneID() + " still needs " + remainingWater + "L of water to extinguish.");
            unassignedIncidents.add(incident);
        }
    }

    /**
     * Handles a DroneUpdateEvent, updating the drone's status and potentially assigning it a new task. If the droneID
     * is uninitialized, will assign the drone an ID and add to list of registered drones.
     *
     * @param event The DroneUpdateEvent containing the updated drone details.
     */
    private void handleDroneUpdate(DroneUpdateEvent event) {
        int droneID = event.getDroneID();

        // If the drone ID is -1, it's a new drone requesting registration
        if (droneID == -1) {
            droneID = nextDroneId.getAndIncrement();
            event.getDroneInfo().setDroneID(droneID);
            System.out.println("[SCHEDULER] New drone detected, assigning new drone with ID: " + droneID);
            dronesInfo.put(droneID, event.getDroneInfo());
            this.sendToDrone(event, droneID);
            System.out.println("\n[SCHEDULER] Registered new Drone {" + droneID + ", Address: " + event.getDroneInfo().getPort() + ", Port: " + event.getDroneInfo().getPort() + "}\n");
        } else {
            // Store or update the drone info
            DroneInfo drone = event.getDroneInfo();
            dronesInfo.put(droneID, drone);

            // Ensure we don't process a null drone state
            if (drone.getState() == null) {
                System.out.println("[SCHEDULER] Warning: Drone " + droneID + " has no valid state.");
                return;
            }

            // Log drone update
            System.out.println("[SCHEDULER] Received update: Drone " + droneID + " is now in state " + drone.getState().getClass().getSimpleName());
        }

    }

    public static void main(String[] args) {
        System.out.println("======== FIREFIGHTING DRONE SWARM ========");
        System.out.println("[SCHEDULER] Scheduler has started.");
        InetAddress address = null;
        try{
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }


        Scheduler scheduler = new Scheduler(address, 7000);
        scheduler.run();
    }
}
