package main;

import helpers.IncidentEventComparator;
import logger.EventLogger;
import subsystems.Event;
import subsystems.EventType;
import subsystems.drone.DroneInfo;
import subsystems.drone.DroneSubsystem;
import subsystems.drone.events.*;
import subsystems.drone.states.*;
import subsystems.fire_incident.Faults;
import subsystems.fire_incident.events.IncidentEvent;
import subsystems.fire_incident.Severity;
import subsystems.fire_incident.events.ZoneEvent;

import java.awt.geom.Point2D;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<Integer, Thread> watchdogs = new ConcurrentHashMap<>();


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

        try {
            this.receiveSocket.getSocket().setSoTimeout(3000);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

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
            DroneDispatchEvent dispatchToBase = new DroneDispatchEvent(0, new Point2D.Double(0,0), Faults.NONE);

            for (int droneID : this.dronesInfo.keySet()) {
                sendToDrone(dispatchToBase, droneID);
            }
            running = false;
            return;
        }

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

    /**
     * Attempts to assign unassigned tasks to available drones.
     */
    private void attemptAssignUnassignedTask() {
        if (unassignedIncidents.isEmpty()) {
            return;
        }

        // Process everything currently in unassignedIncidents
        while (!unassignedIncidents.isEmpty()) {
            // Remove from the queue so we don't keep re-checking the same item in this pass
            IncidentEvent task = unassignedIncidents.peek();

            boolean assigned = assignDrone(task);
            if (assigned) {
                // If assigned send DRONE_DISPATCHED event
                task.setEventType(EventType.DRONE_DISPATCHED);
                sendSocket.send(task, fireSubsystemAddress, fireSubsystemPort);
                unassignedIncidents.poll();
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
        int droneID = this.findAvailableDrone(fireZoneCenter);

        // no drone available
        if (droneID == -1) {
            return false;
        }

        // track drone assignment
        droneAssignments.put(droneID, task);
        EventLogger.info(EventLogger.NO_ID, "Assigned drone to waiting fire at Zone " + task.getZoneID());


        // Create a dispatch event that carries the simulation flag and the specific fault
        DroneDispatchEvent dispatchEvent = new DroneDispatchEvent(zoneID, fireZoneCenter, task.getFault());
        EventLogger.info(EventLogger.NO_ID,
                String.format("Dispatching Drone %d to Zone %d | Coordinates: (%.1f, %.1f)%n",
                        droneID,
                        zoneID,
                        fireZoneCenter.getX(),
                        fireZoneCenter.getY()));

        // Calculate dynamic deadline based on travel time (gives buffer to calculated time)
        double flightTimeSeconds = DroneSubsystem.timeToZone(dronesInfo.get(droneID).getCoordinates(), fireZoneCenter) + 5.0;

        if (task.getFault() != Faults.PACKET_LOSS){
            sendToDrone(dispatchEvent, droneID);
        }
        startWatchdog(droneID, flightTimeSeconds);

        return true;
    }

    /**
     * Checks if the drone has enough battery to complete a round trip to the target coordinates.
     *
     * @param droneInfo The drone to check.
     * @param targetCoords The coordinates of the target zone.
     * @return true if the drone has enough battery, false otherwise.
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
        cancelWatchdog(droneID);


        IncidentEvent incident = droneAssignments.get(droneID);

        // calculate how much water to drop
        int waterToDrop = Math.min(incident.getWaterFoamAmount(), dronesInfo.get(droneID).getWaterLevel());
        EventLogger.info(EventLogger.NO_ID, "Ordering Drone " + droneID + " to drop " + waterToDrop + "L at Zone " + incident.getZoneID());
        // send drop event to drone
        DropAgentEvent dropEvent = new DropAgentEvent(waterToDrop);

        sendToDrone(dropEvent,droneID);
        startWatchdog(droneID, waterToDrop * 1000);
    }

    /**
     * Handles a DropAgentEvent, updating the fire incident data and reassigning drones if necessary.
     *
     * @param event The DropAgentEvent containing the details of the water drop.
     */
    private void handleDropAgent(DropAgentEvent event) {
        int droneID = event.getDroneID();
        cancelWatchdog(droneID);

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
            if (drone.getState() instanceof FaultedState state) {
                switch (state.getFaultDescription()){
                    case NOZZLE_JAMMED -> handleNozzleJammedDrone(droneID);
                    case DRONE_STUCK_IN_FLIGHT -> handleTransientDroneFailure(droneID);
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
        EventLogger.warn(EventLogger.NO_ID, "Drone " + droneID + " in faulted state, reported NOZZLE_JAMMED. Shutting Down Drone.");

        // Decouple the incident and drone
        IncidentEvent incident = droneAssignments.remove(droneID);

        if (incident == null) {
            return;
        }

        EventLogger.info(EventLogger.NO_ID, "Re-queuing Incident at Zone " + incident.getZoneID() + " for reassignment.");
        incident.setFault(Faults.NONE);
        unassignedIncidents.add(incident);

        DroneDispatchEvent shutDownEvent = new DroneDispatchEvent(0, new Point2D.Double(0,0), Faults.NOZZLE_JAMMED);
        sendToDrone(shutDownEvent, droneID);
    }

    /**
     * Handles a drone that is stuck mid-flight.
     * Removes the drone’s current assignment and deadline, and abandons its fire
     *
     * @param droneID The ID of drone declared stuck
     */
    private void handleTransientDroneFailure(int droneID) {
        // Remove stuck drone from incident
        IncidentEvent incidentEvent = droneAssignments.remove(droneID);

        EventLogger.info(EventLogger.NO_ID, "Re‑queuing Incident " + incidentEvent.toString() + " for reassignment.");
        incidentEvent.setFault(Faults.NONE);
        unassignedIncidents.add(incidentEvent);

        DroneDispatchEvent returnToBase = new DroneDispatchEvent(0, new Point2D.Double(0,0), Faults.NONE);
        sendToDrone(returnToBase, droneID);
    }

    public void startWatchdog(int droneID, double waitTime) {
        Thread watchdog = new Thread(() -> {
            try {
                long waitTimeMillis = (long) (waitTime * 1000);
                Thread.sleep(waitTimeMillis);

                IncidentEvent incident = this.droneAssignments.get(droneID);

                EventLogger.warn(EventLogger.NO_ID, "Packet Loss occurred during handling of Incident: " + incident.toString());
                incident.setFault(Faults.NONE);
                this.handleTransientDroneFailure(droneID);
            } catch (InterruptedException ignored) {
            } finally {
                watchdogs.remove(droneID); // Cleanup
            }
        });

        watchdogs.put(droneID, watchdog);
        watchdog.start();
    }

    public void cancelWatchdog(int droneID) {
        Thread watchdog = watchdogs.get(droneID);
        if (watchdog != null && watchdog.isAlive()) {
            watchdog.interrupt(); // Cancel timer
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
        EventLogger.info(EventLogger.NO_ID, "[SCHEDULER] Scheduler has started.");
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
