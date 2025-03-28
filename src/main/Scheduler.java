package main;

import helpers.IncidentEventComparator;
import logger.EventLogger;
import subsystems.Event;
import subsystems.EventType;
import subsystems.drone.DroneInfo;
import subsystems.drone.DroneSubsystem;
import subsystems.drone.events.*;
import subsystems.drone.states.DroneState;
import subsystems.drone.states.FaultedState;
import subsystems.drone.states.IdleState;
import subsystems.fire_incident.Faults;
import subsystems.fire_incident.events.IncidentEvent;
import subsystems.fire_incident.Severity;
import subsystems.fire_incident.events.ZoneEvent;

import java.awt.geom.Point2D;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Scheduler {
    private static final AtomicInteger nextDroneId = new AtomicInteger(1);
    public static int sleepMultiplier = 500;
    private final EventSocket sendSocket;
    private final EventSocket receiveSocket;
    private final HashMap<Integer, Point2D> fireZones;
    private final HashMap<Integer, IncidentEvent> droneAssignments;
    private final InetAddress fireSubsystemAddress;
    private final int fireSubsystemPort;
    private final Map<Integer, DroneInfo> dronesInfo;
    private volatile boolean running = true;
    private final PriorityQueue<IncidentEvent> unassignedIncidents;

    // Hold the time by which a drone should have arrived to its assigned zone
    private Map<Integer, Long> droneArrivalDeadlines = new HashMap<>();

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
        this.unassignedIncidents = new PriorityQueue<>(new IncidentEventComparator());
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

                // Check for if any drones arrived past its deadline
                checkForLateArrivals();

                // Retrieve an event from the queue
                Event message = receiveSocket.receive();
                if (message == null) {
                    continue;
                }

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
                EventLogger.error(EventLogger.NO_ID, "Issue handling message: " + e.getMessage());
                e.printStackTrace();
            }
        }

        this.receiveSocket.getSocket().close();
        this.sendSocket.getSocket().close();
    }

    /**
     * Sends an event to a specific drone based on its ID.
     * Retrieves the drone's address and port from the dronesInfo map and sends the event using the EventSocket.
     * If the address or port is not found, an error message is printed.
     *
     * @param event The event to be sent to the drone.
     * @param droneID The ID of the drone to which the event will be sent.
     */
    private void sendToDrone(Event event, int droneID){
        InetAddress address = dronesInfo.get(droneID).getAddress();
        Integer port = dronesInfo.get(droneID).getPort();
        if (address != null && port != null) {
            sendSocket.send(event, address, port);
        } else {
            EventLogger.error(EventLogger.NO_ID, "[ERROR] Address not found for drone ID: " + droneID);
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
        EventLogger.info(EventLogger.NO_ID, String.format(
                "Stored fire zone {ZoneID: %d | Center: (%.1f, %.1f)}",
                event.getZoneID(),
                event.getCenter().getX(),
                event.getCenter().getY()
        ));
    }

    /**
     * Handles fire incident events by determining required water and dispatching drones.
     * If no drone is available, the task is stored for later assignment.
     *
     * @param event The IncidentEvent containing fire incident data.
     */
    public void handleIncidentEvent(IncidentEvent event) {
        if (event.getEventType() == EventType.EVENTS_DONE) {
            EventLogger.info(EventLogger.NO_ID, "Received EVENTS_DONE. Dispatching all drones to base and terminating...");
            DroneDispatchEvent dispatchToBase = new DroneDispatchEvent(0, new Point2D.Double(0,0), false, Faults.NONE);

            for (int droneID : this.dronesInfo.keySet()) {
                sendToDrone(dispatchToBase, droneID);
            }
            running = false;
            return;
        }

        System.out.println("This is fire zones: " + fireZones);
        if (!fireZones.containsKey(event.getZoneID())) {
            EventLogger.error(EventLogger.NO_ID, "Fire zone center not found for Zone " + event.getZoneID());
            return;
        }

        EventLogger.info(EventLogger.NO_ID,"New fire incident at Zone " + event.getZoneID() + ". Requires " + event.getWaterFoamAmount() + "L of water.");
        if (assignDrone(event)) {
            event.setEventType(EventType.DRONE_DISPATCHED);
            sendSocket.send(event, fireSubsystemAddress, fireSubsystemPort);
        } else {
            EventLogger.info(EventLogger.NO_ID, "No drones available for fire at zone " + event.getZoneID() + ", added to unassigned incident buffer for assignment when drone is available\n");
            // Add to buffer of unassigned incidents
            unassignedIncidents.add(event);
        }
    }

    private void attemptAssignUnassignedTask() {
        if (unassignedIncidents.isEmpty()) {
            return;
        }

        // We'll collect incidents that are unassignable currently but still have hope lol
        List<IncidentEvent> leftoverIncidents = new ArrayList<>();

        // Process everything currently in unassignedIncidents
        while (!unassignedIncidents.isEmpty()) {
            // Remove from the queue so we don't keep re-checking the same item in this pass
            IncidentEvent task = unassignedIncidents.poll();

            boolean assigned = assignDrone(task);
            if (assigned) {
                // If assigned send DRONE_DISPATCHED event
                task.setEventType(EventType.DRONE_DISPATCHED);
                sendSocket.send(task, fireSubsystemAddress, fireSubsystemPort);

            } else {
                // Count healthy drones
                int healthyDrones = 0;
                for (Integer id : dronesInfo.keySet()) {
                    if (!(dronesInfo.get(id).getState() instanceof FaultedState)) {
                        healthyDrones++;
                    }
                }

                if (healthyDrones == 0) {
                    // No healthy drones then abandon fire
                    EventLogger.info(EventLogger.NO_ID, "No healthy drones available; abandoning fire at Zone " + task.getZoneID());
                    IncidentEvent abandoned = new IncidentEvent(
                            "", task.getZoneID(),
                            EventType.FIRE_EXTINGUISHED, Severity.NONE, Faults.NONE
                    );
                    sendSocket.send(abandoned, fireSubsystemAddress, fireSubsystemPort);

                } else {
                    // There is still at least one healthy drone so it might succeed later, add it to leftoverIncidents list to try again in the next pass of the main loop
                    leftoverIncidents.add(task);
                }
            }
        }

        // Now re add the leftoverIncidents incidents to the queue
        unassignedIncidents.addAll(leftoverIncidents);
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
        int droneID = this.findAvailableDrone(fireZoneCenter);
        if (droneID == -1) {
            return false;
        }

        droneAssignments.put(droneID, task);
        EventLogger.info(EventLogger.NO_ID, "Assigned drone to waiting fire at Zone " + task.getZoneID());

        // Determine if we need to simulate a fault based on the incident
        boolean simulateFault = task.getFault() == Faults.DRONE_STUCK_IN_FLIGHT || task.getFault() == Faults.NOZZLE_JAMMED;

        // Create a dispatch event that carries the simulation flag and the specific fault
        DroneDispatchEvent dispatchEvent = new DroneDispatchEvent(zoneID, fireZoneCenter, simulateFault, task.getFault());
        EventLogger.info(EventLogger.NO_ID,
                String.format("Dispatching Drone %d to Zone %d | Coordinates: (%.1f, %.1f)%n",
                        droneID,
                        zoneID,
                        fireZoneCenter.getX(),
                        fireZoneCenter.getY()));

        // Calculate dynamic deadline based on travel time (gives buffer to calculated time)
        double flightTimeSeconds = dronesInfo.get(droneID).getCoordinates().distance(fireZoneCenter) / 15.0 + 6.25;
        long flightTimeMs = (long)(flightTimeSeconds * 1000);
        long bufferTime = 5000;
        long expectedArrivalTime = System.currentTimeMillis() + flightTimeMs + bufferTime;
        droneArrivalDeadlines.put(droneID, expectedArrivalTime);

        sendToDrone(dispatchEvent, droneID);
        return true;
    }

    /**
     * Checks if the drone has enough battery to complete a round trip to the target coordinates.
     *
     * @param droneInfo The drone to check.
     * @param targetCoords The coordinates of the target zone.
     */
    private boolean hasEnoughBattery(DroneInfo droneInfo, Point2D targetCoords){
        double distanceToTarget = droneInfo.getCoordinates().distance(targetCoords);
        double distanceToBase = targetCoords.distance(new Point2D.Double(0,0));
        double travelTime = (((distanceToTarget + distanceToBase) - 46.875) / 15 + 6.25);
        return (droneInfo.getFlightTime() - travelTime > DroneSubsystem.DRONE_BATTERY_TIME);
    }

    /**
     * Finds an available drone with sufficient battery and water level to complete the task.
     *
     * @param fireZoneCenter The coordinates of the fire zone.
     * @return The ID of the available drone, or -1 if no drone is available.
     */
    private int findAvailableDrone(Point2D fireZoneCenter) {
        for (int droneID : dronesInfo.keySet()) {
            DroneInfo droneInfo = dronesInfo.get(droneID);
            DroneState droneState = dronesInfo.get(droneID).getState();

            if (droneState == null) {
                continue; // skip drones without a valid state
            }

            if (droneState instanceof IdleState && !droneAssignments.containsKey(droneID) && hasEnoughBattery(droneInfo, fireZoneCenter)) {
                EventLogger.info(EventLogger.NO_ID, "Found available idle drone: " + droneID);
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
            EventLogger.error(EventLogger.NO_ID, "Unrecognized DroneArrivedEvent.\"");
            return;
        }

        IncidentEvent incident = droneAssignments.get(droneID);

        // calculate how much water to drop
        int waterToDrop = Math.min(incident.getWaterFoamAmount(), dronesInfo.get(droneID).getWaterLevel());
        EventLogger.info(EventLogger.NO_ID, "Ordering Drone " + droneID + " to drop " + waterToDrop + "L at Zone " + incident.getZoneID());
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
            IncidentEvent fireOutEvent = new IncidentEvent("", incident.getZoneID(), EventType.FIRE_EXTINGUISHED, Severity.NONE, Faults.NONE);
            sendSocket.send(fireOutEvent, fireSubsystemAddress, fireSubsystemPort);
        } else {
            // Otherwise update remaining water and put incident in buffer
            incident.setWaterFoamAmount(remainingWater);
            droneAssignments.remove(droneID);
            EventLogger.warn(EventLogger.NO_ID, "Fire at Zone " + incident.getZoneID() + " still needs " + remainingWater + "L of water to extinguish.");
            unassignedIncidents.add(incident);
        }
    }

    /**
     * Handles a DroneUpdateEvent, updating the drone's status and potentially assigning it a new task. If the droneID
     * is uninitialized, will assign the drone an ID and add to list of registered drones.
     *
     * @param event The DroneUpdateEvent containing the updated drone details.
     */
    public void handleDroneUpdate(DroneUpdateEvent event) {
        int droneID = event.getDroneID();

        // If the drone ID is -1, it's a new drone requesting registration
        if (droneID == -1) {
            droneID = nextDroneId.getAndIncrement();
            event.getDroneInfo().setDroneID(droneID);
            EventLogger.info(EventLogger.NO_ID, "New drone detected, assigning new drone with ID: " + droneID);
            dronesInfo.put(droneID, event.getDroneInfo());
            this.sendToDrone(event, droneID);
            EventLogger.info(EventLogger.NO_ID, "Registered new Drone {" + droneID + ", Address: " + event.getDroneInfo().getPort() + ", Port: " + event.getDroneInfo().getPort() + "}");
        } else {
            // Store or update the drone info
            DroneInfo drone = event.getDroneInfo();
            dronesInfo.put(droneID, drone);

            // Ensure we don't process a null drone state
            if (drone.getState() == null) {
                EventLogger.warn(EventLogger.NO_ID, "Drone " + droneID + " has no valid state.");
                return;
            }

            // Log drone update
            EventLogger.info(EventLogger.NO_ID, "Received update: Drone " + droneID + " is now in state " + drone.getState().getClass().getSimpleName());

            // Check for faulted state
            if (drone.getState() instanceof FaultedState) {
                String state = drone.getState().toString();
                if (state.contains("NOZZLE_JAMMED")) {
                    handleNozzleJammedDrone(droneID);
                } else {
                    handleStuckDrone(droneID);
                }
            }
        }
    }

    /**
     * Handles a drone that has encountered a nozzle jam fault
     *
     * @param droneID the ID of the drone
     */
    private void handleNozzleJammedDrone(int droneID) {
        EventLogger.warn(EventLogger.NO_ID, "Drone " + droneID + " reported NOZZLE_JAMMED. Declaring it FAULTED.");

        // Decouple the incident and drone
        IncidentEvent incident = droneAssignments.remove(droneID);
        droneArrivalDeadlines.remove(droneID);

        if (incident == null) {
            return;
        }

        // Count non faulted drones
        int availableDrones = 0;
        for (Integer id : dronesInfo.keySet()) {
            if (id != droneID && !(dronesInfo.get(id).getState() instanceof FaultedState)) {
                availableDrones++;
            }
        }

        // Requeue fire if other non faulted drones are available then requeue fire, abandon otherwise
        if (availableDrones > 0) {
            EventLogger.info(EventLogger.NO_ID, "Other drones available; re-queuing Zone " + incident.getZoneID() + " for reassignment.");
            incident.setFault(Faults.NONE);
            unassignedIncidents.add(incident);
        } else {
            EventLogger.info(EventLogger.NO_ID, "No other drones available; abandoning fire at Zone " + incident.getZoneID());
            IncidentEvent abandonedEvent = new IncidentEvent("", incident.getZoneID(), EventType.FIRE_EXTINGUISHED, Severity.NONE, Faults.NONE);
            sendSocket.send(abandonedEvent, fireSubsystemAddress, fireSubsystemPort);
        }
    }

    /**
     * Handles a drone that is stuck mid-flight.
     * Removes the drone’s current assignment and deadline, and abandons its fire
     *
     * @param droneID The ID of drone declared stuck
     */
    private void handleStuckDrone(int droneID) {
        EventLogger.warn(EventLogger.NO_ID, "Drone " + droneID + " missed arrival deadline. Declaring it STUCK.");
        // Remove stuck drone from incident
        IncidentEvent stuckIncident = droneAssignments.remove(droneID);

        // Remove drone's arrival deadline since it is stuck
        droneArrivalDeadlines.remove(droneID);
        if (stuckIncident == null) return;

        // Count how many other drones are not faulted
        int availableDrones = 0;
        for (Integer id : dronesInfo.keySet()) {
            if (id != droneID && !(dronesInfo.get(id).getState() instanceof FaultedState)) {
                availableDrones++;
            }
        }

        if (availableDrones > 0) {
            EventLogger.info(EventLogger.NO_ID, "Other drones available, re‑queuing Zone " + stuckIncident.getZoneID() + " for reassignment.");
            stuckIncident.setFault(Faults.NONE);
            unassignedIncidents.add(stuckIncident);
        } else {
            EventLogger.info(EventLogger.NO_ID, "No other drones available, abandoning fire at Zone " + stuckIncident.getZoneID());
            IncidentEvent abandonedEvent = new IncidentEvent("", stuckIncident.getZoneID(), EventType.FIRE_EXTINGUISHED, Severity.NONE, Faults.NONE);
            sendSocket.send(abandonedEvent, fireSubsystemAddress, fireSubsystemPort);
        }
    }


    /**
     * Method to iterate through all the arrival deadlines and check if any have been exceeded.
     */
    private void checkForLateArrivals() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : droneArrivalDeadlines.entrySet()) {
            int droneID = entry.getKey();
            long deadline = entry.getValue();

            // If drone missed its deadline then call handleStuckDrone to abandon the fire and inform fire incident subsystem
            if (now > deadline && droneAssignments.containsKey(droneID)) {
                EventLogger.info(EventLogger.NO_ID, "Drone " + droneID + " missed arrival deadline. Assuming STUCK mid-flight.");
                handleStuckDrone(droneID);
            }
        }
    }

    public void close() {
        if (receiveSocket != null) receiveSocket.close();
        if (sendSocket != null) sendSocket.close();
    }

    /**
     * The entry point of the Firefighting Drone Swarm program.
     * Initializes the scheduler and runs it.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        EventLogger.info(EventLogger.NO_ID, "======== FIREFIGHTING DRONE SWARM ========");
        EventLogger.info(EventLogger.NO_ID, "Scheduler has started.");
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
