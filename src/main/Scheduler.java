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
import subsystems.fire_incident.FireIncidentSubsystem;
import subsystems.fire_incident.events.IncidentEvent;
import subsystems.fire_incident.Severity;
import subsystems.fire_incident.events.ZoneEvent;

import java.awt.*;
import java.awt.geom.Point2D;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Scheduler {
    private static final AtomicInteger nextDroneId = new AtomicInteger(1);
    public static int sleepMultiplier = 1000;
    private final EventSocket sendSocket;
    private final EventSocket receiveSocket;
    private final HashMap<Integer, Point2D> fireZones;
    private final HashMap<Integer, IncidentEvent> droneAssignments;
    private final InetAddress fireSubsystemAddress;
    private final int fireSubsystemPort;
    private final Map<Integer, DroneInfo> dronesInfo;
    private volatile boolean running = true;
    private final Map<Integer, Thread> watchdogs = new ConcurrentHashMap<>();
    private final DroneSwarmDashboard dashboard;
    private Set<Integer> dronesReturningToBase = new HashSet<>();
    private boolean shutdownPending = false;
    private final Map<Integer, IncidentEvent> activeFires = new HashMap<>();
    private final Map<IncidentEvent, Long> incidentStartTimes = new HashMap<>();
    private long totalExtinguishTime = 0;
    private int incidentsCompleted = 0;
    private double totalDistanceTravelled = 0.0;
    private final Map<Integer, Double> zoneResponseTimes = new HashMap<>();


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
        this.dashboard = new DroneSwarmDashboard();

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

                // assign all IDLE drones to any active fires
                assignAvailableDrones();

                // Retrieve an event from the queue
                Event message = receiveSocket.receive();

                switch (message) {
                    case null -> {
                        continue;
                    }

                    // handle event based on its type
                    case ZoneEvent zoneEvent -> storeZoneData(zoneEvent);
                    case IncidentEvent incidentEvent -> handleIncidentEvent(incidentEvent);
                    case DroneArrivedEvent arrivedEvent -> handleDroneArrival(arrivedEvent);
                    case DropAgentEvent dropEvent -> handleDropAgent(dropEvent);
                    case DroneUpdateEvent updateEvent -> handleDroneUpdate(updateEvent);
                    case DroneReassignRequestEvent reassignEvent -> handleReassignDrone(reassignEvent);
                    default -> {
                    }
                }

            } catch (Exception e) {
                EventLogger.error(EventLogger.NO_ID, "Issue handling message: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Log overall performance metrics
        EventLogger.info(EventLogger.NO_ID, "========== PERFORMANCE METRICS ==========", true);
        EventLogger.info(EventLogger.NO_ID, "[METRICS] Total Extinguish Time: " + totalExtinguishTime + " ms", true);
        if (incidentsCompleted > 0) {
            EventLogger.info(EventLogger.NO_ID, "[METRICS] Average Extinguish Time: " + (totalExtinguishTime / incidentsCompleted) + " ms", true);
        }
        EventLogger.info(EventLogger.NO_ID, "[METRICS] Total Distance Travelled: " + totalDistanceTravelled + " meters", true);

        // Calc total idle time for each drone
        for (DroneInfo drone : dronesInfo.values()) {

            // Finalize drones idle time
            if (drone.getState() instanceof IdleState && drone.getIdleStartTime() != 0) {
                long idleDuration = System.currentTimeMillis() - drone.getIdleStartTime();
                drone.setTotalIdleTime(drone.getTotalIdleTime() + idleDuration);
                drone.setIdleStartTime(0);
            }
            EventLogger.info(EventLogger.NO_ID, "Drone " + drone.getDroneID() + " total idle time: " + drone.getTotalIdleTime() + " ms", true);
        }

        // Print zone response times at the end of simulation
        EventLogger.info(EventLogger.NO_ID, "\n========== ZONE RESPONSE TIMES ==========", true);
        for (Map.Entry<Integer, Double> entry : zoneResponseTimes.entrySet()) {
            EventLogger.info(EventLogger.NO_ID, "Zone " + entry.getKey() + " response time: " + entry.getValue() + " s", true);
        }

        this.receiveSocket.getSocket().close();
        this.sendSocket.getSocket().close();
    }

    /**
     * Finds the next fire incident that needs help for a given drone.
     */
    private void assignAvailableDrones() {
        for (Map.Entry<Integer, DroneInfo> droneEntry : dronesInfo.entrySet()) {
            int droneID = droneEntry.getKey();
            DroneInfo drone = droneEntry.getValue();

            // skip any drone that's not idle
            if (!(drone.getState() instanceof IdleState) || droneAssignments.containsKey(droneID)) continue;

            Optional<IncidentEvent> next = findNextFireNeedingHelp(drone);
            next.ifPresent(incident -> assignDroneToIncident(incident, drone));
        }
    }

    /**
     * Finds the next fire incident that needs help for a given drone.
     */
    public Optional<IncidentEvent> findNextFireNeedingHelp(DroneInfo drone) {
        Point2D dronePosition = drone.getCoordinates();

        // get all active fires that still need water
        java.util.List<IncidentEvent> candidates = activeFires.values().stream()
                .filter(incident -> incident.getWaterFoamAmount() > 0)
                .sorted((a, b) -> {
                    int severityCompare = b.getSeverity().ordinal() - a.getSeverity().ordinal();
                    return (severityCompare != 0) ? severityCompare : a.getTime().compareTo(b.getTime());
                })
                .toList();

        // try to find a zone with 0 drones assigned first
        for (IncidentEvent incident : candidates) {
            int zoneID = incident.getZoneID();
            long assigned = droneAssignments.values().stream()
                    .filter(e -> e.getZoneID() == zoneID)
                    .count();

            if (assigned == 0) {
                Point2D target = fireZones.get(zoneID);
                if (target != null) return Optional.of(incident);
            }
        }

        // all zones are already covered once — now allow reinforcement
        for (IncidentEvent incident : candidates) {
            int zoneID = incident.getZoneID();
            long assigned = droneAssignments.values().stream()
                    .filter(e -> e.getZoneID() == zoneID)
                    .count();

            if (assigned < 2) { // optional limit max drones per zone
                Point2D target = fireZones.get(zoneID);
                if (target != null) return Optional.of(incident);
            }
        }

        return Optional.empty();
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

        // Calculate the one way distance from the base to the zone center, logs distance only if zone id is not id of base
        if (event.getZoneID() != 0) {
            int distance = (int) Math.round(FireIncidentSubsystem.BASE_COORDINATES.distance(event.getCenter()));
            EventLogger.info(EventLogger.NO_ID, "[METRICS] Distance to reach Zone " + event.getZoneID() + " from base: " + distance + " meters", true);
        }

        // update the dashboard with the new zone data
        Point start = convertToGrid(event.getStart());
        Point end = convertToGrid(event.getEnd());
        dashboard.markZone(event.getZoneID(), start, end);

        EventLogger.info(EventLogger.NO_ID, String.format(
                "Stored fire zone {ZoneID: %d | Center: (%.1f, %.1f)}",
                event.getZoneID(),
                event.getCenter().getX(),
                event.getCenter().getY()
        ), false);
    }

    /**
     * Handles fire incident events by determining required water and dispatching drones.
     * If no drone is available, the task is stored for later assignment.
     *
     * @param event The IncidentEvent containing fire incident data.
     */
    public void handleIncidentEvent(IncidentEvent event) {

        if (event.getEventType() == EventType.EVENTS_DONE) {
            EventLogger.info(EventLogger.NO_ID, "Received EVENTS_DONE. Dispatching all drones to base.", false);
            DroneDispatchEvent dispatchToBase = new DroneDispatchEvent(0, new Point2D.Double(0,0), Faults.NONE);

            shutdownPending = true;
            dronesReturningToBase.clear();

            for (int droneID : this.dronesInfo.keySet()) {
                dronesReturningToBase.add(droneID);
                sendToDrone(dispatchToBase, droneID);
            }

            checkShutdownCondition();
            return;
        }

        if (!fireZones.containsKey(event.getZoneID())) {
            EventLogger.error(EventLogger.NO_ID, "Fire zone center not found for Zone " + event.getZoneID());
            return;
        }

        // If this zone hasn't been recorded yet record its start time
        if (!zoneResponseTimes.containsKey(event.getZoneID())) {
            incidentStartTimes.put(event, System.currentTimeMillis());
        }

        // Record the start time for this incident
        incidentStartTimes.put(event, System.currentTimeMillis());

        EventLogger.info(EventLogger.NO_ID,"New fire incident at Zone " + event.getZoneID() + ". Requires " + event.getWaterFoamAmount() + "L of water.", true);
        activeFires.put(event.getZoneID(), event);
        dashboard.updateZoneWater(event.getZoneID(), event.getWaterFoamAmount());
        dashboard.setZoneFireStatus(event.getZoneID(), DroneSwarmDashboard.FireStatus.ACTIVE);
        dashboard.updateZoneSeverity(event.getZoneID(), event.getSeverity());

        if (assignDrone(event)) {
            event.setEventType(EventType.DRONE_DISPATCHED);
            sendSocket.send(event, fireSubsystemAddress, fireSubsystemPort);
        } else {
            EventLogger.info(EventLogger.NO_ID, "No drones available for fire at zone " + event.getZoneID() + ", added to unassigned incident buffer for assignment when drone is available\n", false);
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

        DroneDispatchEvent dispatchEvent = new DroneDispatchEvent(zoneID, fireZoneCenter, task.getFault());
        // track drone assignment
        droneAssignments.put(droneID, task);

        // Calc distance travelled from base to fire zone to base
        double distanceToZone = FireIncidentSubsystem.BASE_COORDINATES.distance(fireZoneCenter);
        double distanceBack = fireZoneCenter.distance(new Point2D.Double(0, 0));
        totalDistanceTravelled += (distanceToZone + distanceBack);

        EventLogger.info(EventLogger.NO_ID,
                String.format("Assigned and dispatching Drone %d → Zone %d | Coords: (%.1f, %.1f) | Fault: %s",
                        droneID,
                        zoneID,
                        fireZoneCenter.getX(),
                        fireZoneCenter.getY(),
                        task.getFault()), true);

        // Calculate dynamic deadline based on travel time (gives buffer to calculated time)
        double flightTimeSeconds = DroneSubsystem.timeToZone(dronesInfo.get(droneID).getCoordinates(), fireZoneCenter) + 5.0;

        // simulate packet loss by not sending drone the event
        startWatchdog(droneID, flightTimeSeconds);
        if (task.getFault() != Faults.PACKET_LOSS){
            sendToDrone(dispatchEvent, droneID);
        }

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
                EventLogger.info(EventLogger.NO_ID, "Found available idle drone: " + droneID, false);
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
        if (event.getZoneID() == 0) {
            EventLogger.info(EventLogger.NO_ID, "Drone " + droneID + " has returned to base.", false);
            dashboard.updateDronePosition(droneID, new Point(0, 0), DroneSwarmDashboard.DroneState.IDLE);
        } else {
            cancelWatchdog(droneID);
            IncidentEvent incident = droneAssignments.get(droneID);
            if (incident == null) {
                EventLogger.error(droneID, "Drone " + droneID + " arrived at zone " + event.getZoneID() + " but has no assignment.");
                return;
            }

            // Record zone response time if not already recorded
            if (!zoneResponseTimes.containsKey(incident.getZoneID())) {
                Long startTime = incidentStartTimes.get(incident);
                if (startTime != null) {
                    long responseTime = System.currentTimeMillis() - startTime;
                    // Convert to seconds
                    double responseTimeSec = responseTime / 1000.0;
                    zoneResponseTimes.put(incident.getZoneID(), responseTimeSec);
                }
            }

            // check if this zone still requires service by the time the drone arrives
            if (incident.getWaterFoamAmount() <= 0) {
                EventLogger.info(droneID, "Arrived at Zone " + incident.getZoneID() + " but the fire is already extinguished.", false);
                reassignDrone(dronesInfo.get(droneID));
            } else {
                // calculate how much water to drop
                int waterToDrop = Math.min(incident.getWaterFoamAmount(), dronesInfo.get(droneID).getWaterLevel());
                EventLogger.info(EventLogger.NO_ID, "Ordering Drone " + droneID + " to drop " + waterToDrop + "L at Zone " + incident.getZoneID(), false);

                // send drop event to drone
                DropAgentEvent dropEvent = new DropAgentEvent(waterToDrop);
                sendToDrone(dropEvent,droneID);
                startWatchdog(droneID, waterToDrop * 1000);
            }
        }

        // check if it's one of the returning drones and if it's at base (0,0)
        if (shutdownPending && dronesReturningToBase.contains(droneID) && isAtBase(dronesInfo.get(droneID).getCoordinates())) {
            dronesReturningToBase.remove(droneID);
            checkShutdownCondition();
        }
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

            // Calc time to extinguish
            Long startTime = incidentStartTimes.get(incident);
            if (startTime != null) {
                long extinguishTime = System.currentTimeMillis() - startTime;
                EventLogger.info(EventLogger.NO_ID, "[METRICS] Fire extinguish time for Zone " + incident.getZoneID() + ": " + extinguishTime + " ms", true);

                // Accumulate totals for average calculation
                totalExtinguishTime += extinguishTime;
                incidentsCompleted++;
            }

            // notify FireIncidentSubSystem that the fire has been put out
            IncidentEvent fireOutEvent = new IncidentEvent("", incident.getZoneID(), EventType.FIRE_EXTINGUISHED, Severity.NONE, Faults.NONE);
            EventLogger.info(EventLogger.NO_ID, "Fire at Zone " + incident.getZoneID() + " has been extinguished.", true);
            dashboard.setZoneFireStatus(incident.getZoneID(), DroneSwarmDashboard.FireStatus.EXTINGUISHED);
            sendSocket.send(fireOutEvent, fireSubsystemAddress, fireSubsystemPort);
            activeFires.remove(incident.getZoneID());
            incident.setWaterFoamAmount(0);

            reassignDrone(dronesInfo.get(droneID));
        } else {
            incident.setWaterFoamAmount(remainingWater);
            droneAssignments.remove(droneID);
            activeFires.get(incident.getZoneID()).setWaterFoamAmount(remainingWater);
            EventLogger.warn(EventLogger.NO_ID, "Fire at Zone " + incident.getZoneID() + " still needs " + remainingWater + "L of water to extinguish.");
        }

        dashboard.updateZoneWater(incident.getZoneID(), remainingWater);
    }

    /**
     * Handles a DroneUpdateEvent, updating the drone's status and potentially assigning it a new task. If the droneID
     * is uninitialized, will assign the drone an ID and add to list of registered drones.
     *
     * @param event The DroneUpdateEvent containing the updated drone details.
     */
    public void handleDroneUpdate(DroneUpdateEvent event) {
        int droneID = event.getDroneID();
        DroneInfo drone = event.getDroneInfo();

        // If the drone ID is -1, it's a new drone requesting registration
        if (droneID == -1) {
            drone.setDroneID(nextDroneId.getAndIncrement());
            drone.setState(new IdleState());
            EventLogger.info(EventLogger.NO_ID, "New drone detected, assigning new drone with ID: " + drone.getDroneID(), false);
            dronesInfo.put(drone.getDroneID(), drone);
            this.sendToDrone(event, drone.getDroneID());
            EventLogger.info(EventLogger.NO_ID, "Registered new Drone {" + drone.getDroneID() + ", Address: " + drone.getAddress() + ", Port: " + drone.getPort() + "}", true);
        } else {
            // Ensure we don't process a null drone state
            if (drone.getState() == null) {
                EventLogger.warn(EventLogger.NO_ID, "Drone " + droneID + " has no valid state.");
                return;
            }

            // Log drone update
            if (dronesInfo.get(droneID).getState().getClass() != drone.getState().getClass()) {
                if (drone.getState() instanceof FaultedState) {
                    EventLogger.warn(EventLogger.NO_ID, "Received update: Drone " + droneID + " is now in state " + drone.getState().getClass().getSimpleName());
                } else {
                    EventLogger.info(EventLogger.NO_ID, "Received update: Drone " + droneID + " is now in state " + drone.getState().getClass().getSimpleName(), false);
                }
            }

            // Store or update the drone info
            dronesInfo.put(droneID, drone);

            // Check for faulted state
            if (drone.getState() instanceof FaultedState state) {
                switch (state.getFaultDescription()){
                    case NOZZLE_JAMMED -> handleNozzleJammedDrone(droneID);
                    case DRONE_STUCK_IN_FLIGHT -> handleTransientDroneFailure(droneID, true);
                }
            }
        }

        // update drone on dashboard
        DroneSwarmDashboard.DroneState guiState = DroneSwarmDashboard.DroneState.fromDroneStateObject(drone.getState());

        if (guiState != null) {
            Point grid = convertToGrid(drone.getCoordinates());
            dashboard.updateDronePosition(drone.getDroneID(), grid, guiState);
        }
    }

    /**
     * Handles a DroneReassignRequestEvent, checking if the drone has enough water to continue its current assignment.
     * @param event The DroneReassignRequestEvent containing the drone's ID.
     */
    private void handleReassignDrone(DroneReassignRequestEvent event) {
        DroneInfo drone = dronesInfo.get(event.getDroneID());
        IncidentEvent incident = droneAssignments.get(event.getDroneID());
        if (incident == null) {
            EventLogger.error(EventLogger.NO_ID, "Drone " + event.getDroneID() + " has no assignment.");
            return;
        }

        int zoneID = incident.getZoneID();
        IncidentEvent activeFire = activeFires.get(zoneID);

        if (activeFire == null) {
            reassignDrone(drone);
        } else {
            EventLogger.info(drone.getDroneID(), "Zone " + droneAssignments.get(drone.getDroneID()).getZoneID() + " still needs water. Continue en route.", false);
            DroneDispatchEvent dispatchEvent = new DroneDispatchEvent(drone.getZoneID(), drone.getCoordinates(), droneAssignments.get(event.getDroneID()).getFault());
            this.sendToDrone(dispatchEvent, event.getDroneID());
        }
    }

    /**
     * Handles a drone that has encountered a nozzle jam fault
     *
     * @param droneID the ID of the drone
     */
    private void handleNozzleJammedDrone(int droneID) {
        EventLogger.warn(EventLogger.NO_ID, "Drone " + droneID + " in faulted state, reported NOZZLE_JAMMED. Shutting Down Drone.");
        cancelWatchdog(droneID);

        // remove broken drone from assignments
        IncidentEvent incident = droneAssignments.remove(droneID);

        if (incident == null) {
            return;
        }

        int zoneID = incident.getZoneID();
        activeFires.get(zoneID).markFaultHandled();

        DroneDispatchEvent shutDownEvent = new DroneDispatchEvent(0, new Point2D.Double(0,0), Faults.NOZZLE_JAMMED);
        sendToDrone(shutDownEvent, droneID);
    }

    /**
     * Handles a drone that is stuck mid-flight.
     * Removes the drone’s current assignment and deadline, and abandons its fire
     *
     * @param droneID The ID of drone declared stuck
     */
    private void handleTransientDroneFailure(int droneID, boolean dispatchToBase) {
        // Remove stuck drone from incident
        cancelWatchdog(droneID);
        IncidentEvent incidentEvent = droneAssignments.remove(droneID);

        EventLogger.info(EventLogger.NO_ID, "Re‑queuing Incident " + incidentEvent.toString() + " for reassignment.", true);
        incidentEvent.markFaultHandled();

        if (dispatchToBase){
            DroneDispatchEvent returnToBase = new DroneDispatchEvent(0, new Point2D.Double(0,0), Faults.NONE);
            sendToDrone(returnToBase, droneID);
        }
    }

    public void startWatchdog(int droneID, double waitTime) {
        Thread watchdog = new Thread(() -> {
            try {
                long waitTimeMillis = (long) (waitTime * 1000);
                Thread.sleep(waitTimeMillis);

                IncidentEvent incident = this.droneAssignments.get(droneID);

                EventLogger.warn(EventLogger.NO_ID, "Packet Loss occurred during handling of Incident: " + incident.toString());
                incident.markFaultHandled();
                this.handleTransientDroneFailure(droneID, false);
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
     * Converts real-world coordinates to grid coordinates for the GUI.
     * @param realWorldPoint The real-world coordinates to convert.
     * @return The grid coordinates as a Point object.
     */
    private Point convertToGrid(Point2D realWorldPoint) {
        int gridX = (int) Math.round((realWorldPoint.getX() / DroneSwarmDashboard.CELL_SIZE));
        int gridY = (int) Math.round((realWorldPoint.getY() / DroneSwarmDashboard.CELL_SIZE));
        return new Point(gridX, gridY);
    }

    /**
     * Checks if the drone is at the base (0, 0).
     * @param location The location of the drone.
     * @return true if the drone is at the base, false otherwise.
     */
    private boolean isAtBase(Point2D location) {
        return location.distance(0, 0) < 0.001;
    }

    /**
     * Checks if all drones have returned to base and if shutdown is pending.
     */
    private void checkShutdownCondition() {
        if (shutdownPending && dronesReturningToBase.isEmpty()) {
            EventLogger.info(EventLogger.NO_ID, "All drones returned to base. Terminating scheduler.", false);
            running = false;
        }
    }

    /**
     * Reassigns a drone to the next active fire zone. If no fires are active, the drone remains idle.
     * @param drone The drone to be reassigned.
     */
    public void reassignDrone(DroneInfo drone) {
        Optional<IncidentEvent> nextZone = findClosestActiveFire(drone);
        int droneID = drone.getDroneID();

        // check if drone has enough water to be re-serviced
        if (drone.getWaterLevel() <= 0) {
            EventLogger.info(droneID, "No water remaining. Returning to base for refill.", false);
            Point2D base = new Point2D.Double(0, 0);
            DroneDispatchEvent returnToBase = new DroneDispatchEvent(0, base, Faults.NONE);
            sendToDrone(returnToBase, droneID);
            droneAssignments.remove(droneID);
            return;
        }

        // find new fire to help with
        if (nextZone.isPresent()) {
            assignDroneToIncident(nextZone.get(), drone);
        } else {
            // no zone needs help — stay idle
            EventLogger.info(droneID, "No active fires nearby. Idling...", false);
            drone.setZoneID(0);
            drone.setState(new IdleState());
            droneAssignments.remove(droneID);
        }
    }

    /**
     * Finds the closest active fire incident that still requires water.
     * @param drone The drone requesting a new task.
     * @return An optional incident event the drone should be assigned to.
     */
    public Optional<IncidentEvent> findClosestActiveFire(DroneInfo drone) {
        Point2D dronePosition = drone.getCoordinates();
        IncidentEvent closest = null;
        double minDistance = Double.MAX_VALUE;

        for (IncidentEvent incident : activeFires.values()) {
            if (incident.getWaterFoamAmount() <= 0) continue;

            Point2D firePos = fireZones.get(incident.getZoneID());
            if (firePos == null) continue;

            double distance = dronePosition.distance(firePos);
            if (distance < minDistance) {
                minDistance = distance;
                closest = incident;
            }
        }

        return Optional.ofNullable(closest);
    }

    /**
     * Assigns a drone to a specific incident event.
     * @param incident The incident event to which the drone is assigned.
     * @param drone The drone to be assigned.
     */
    public void assignDroneToIncident(IncidentEvent incident, DroneInfo drone) {
        int droneID = drone.getDroneID();
        int zoneID = incident.getZoneID();
        Point2D zoneCenter = fireZones.get(zoneID);

        if (zoneCenter == null) {
            System.err.println("[SCHEDULER] Cannot assign drone to zone " + zoneID + " — zone center missing.");
            return;
        }

        // track the assignment
        droneAssignments.put(droneID, incident);

        // create dispatch event & assign drone
        DroneDispatchEvent dispatch = new DroneDispatchEvent(zoneID, zoneCenter, incident.getFault());
        sendToDrone(dispatch, droneID);

        // update fire incident that a drone has been dispatched (optional)
        incident.setEventType(EventType.DRONE_DISPATCHED);
        sendSocket.send(incident, fireSubsystemAddress, fireSubsystemPort);

        EventLogger.info(EventLogger.NO_ID, String.format("Reassigned Drone %d to closest active fire at Zone %d", droneID, zoneID), true);
    }

    /**
     * The entry point of the Firefighting Drone Swarm program.
     * Initializes the scheduler and runs it.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        EventLogger.info(EventLogger.NO_ID, "======== FIREFIGHTING DRONE SWARM ========", false);
        EventLogger.info(EventLogger.NO_ID, "[SCHEDULER] Scheduler has started.", false);
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
