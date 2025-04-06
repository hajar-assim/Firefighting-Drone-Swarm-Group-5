package main;

import logger.EventLogger;
import main.ui.DroneStateEnum;
import main.ui.DroneSwarmDashboard;
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

import java.awt.*;
import java.awt.geom.Point2D;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static main.ui.GridPanel.CELL_SIZE;

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

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Finds the next fire incident that needs help for a given drone.
     */
    public Optional<IncidentEvent> findNextFireNeedingHelp(DroneInfo drone) {

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
            int assigned = (int) droneAssignments.values().stream()
                    .filter(e -> e.getZoneID() == zoneID)
                    .count();

            if (assigned == 0) {
                if (fireZones.get(zoneID) != null && hasEnoughBattery(drone, fireZones.get(zoneID))) return Optional.of(incident);
            }
        }

        // all zones are already covered once — now allow reinforcement
        for (IncidentEvent incident : candidates) {
            int zoneID = incident.getZoneID();
            int assigned = (int) droneAssignments.values().stream()
                    .filter(e -> e.getZoneID() == zoneID)
                    .count();

            if (assigned < 2 && incident.getWaterFoamAmount() > 15) { // optional limit max drones per zone
                if (fireZones.get(zoneID) != null && hasEnoughBattery(drone, fireZones.get(zoneID))) return Optional.of(incident);
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

        EventLogger.info(EventLogger.NO_ID,"New fire incident at Zone " + event.getZoneID() + ". Requires " + event.getWaterFoamAmount() + "L of water.", true);

        assignAvailableDrones();

        activeFires.put(event.getZoneID(), event);
        dashboard.updateZoneWater(event.getZoneID(), event.getWaterFoamAmount());
        dashboard.setZoneFireStatus(event.getZoneID(), DroneSwarmDashboard.FireStatus.ACTIVE);
        dashboard.updateZoneSeverity(event.getZoneID(), event.getSeverity());
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
     * Handles the event when a drone arrives at a fire zone to drop water.
     *
     * @param event The DroneArrivedEvent containing the drone's arrival details.
     */
    private void handleDroneArrival(DroneArrivedEvent event) {
        int droneID = event.getDroneID();
        if (event.getZoneID() == 0) {
            EventLogger.info(EventLogger.NO_ID, "Drone " + droneID + " has returned to base.", false);
            dashboard.updateDronePosition(droneID, new Point(0, 0), DroneStateEnum.IDLE);
        } else {
            cancelWatchdog(droneID);
            IncidentEvent incident = droneAssignments.get(droneID);
            if (incident == null) {
                EventLogger.error(droneID, "Drone " + droneID + " arrived at zone " + event.getZoneID() + " but has no assignment.");
                return;
            }
            // check if this zone still requires service by the time the drone arrives
            if (! activeFires.containsKey(incident.getZoneID())) {
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
        IncidentEvent incident = droneAssignments.remove(droneID);

        // Subtract the dropped water from the fire requirement
        int remainingWater = incident.getWaterFoamAmount() - event.getVolume();

        // If required water vol has been used then remove the incident from the incident list and unassign drone from zone id
        if (remainingWater <= 0) {
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
        DroneStateEnum guiState = DroneStateEnum.fromDroneStateObject(drone.getState());

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
            EventLogger.info(drone.getDroneID(), "Zone " + droneAssignments.get(drone.getDroneID()).getZoneID() + " has already been extinguished. Finding new assignment...", false);
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
                long waitTimeMillis = (long) (waitTime * sleepMultiplier);
                Thread.sleep(waitTimeMillis);

                IncidentEvent incident = this.droneAssignments.get(droneID);

                EventLogger.warn(EventLogger.NO_ID, "Drone " + droneID + " Packet Loss occurred during handling of Incident: " + incident.toString());
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
        int gridX = (int) Math.round((realWorldPoint.getX() / CELL_SIZE));
        int gridY = (int) Math.round((realWorldPoint.getY() / CELL_SIZE));
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
        Optional<IncidentEvent> nextZone = findNextFireNeedingHelp(drone);
        int droneID = drone.getDroneID();
        cancelWatchdog(droneID);

        // find new fire to help with
        if (nextZone.isPresent() && (drone.getWaterLevel() > 0 && hasEnoughBattery(drone, fireZones.get(nextZone.get().getZoneID())))) {
            assignDroneToIncident(nextZone.get(), drone);
        } else {
            EventLogger.info(droneID, "Drone incapable of servicing another zone. Returning to base for refill.", false);
            Point2D base = new Point2D.Double(0, 0);
            DroneDispatchEvent returnToBase = new DroneDispatchEvent(0, base, Faults.NONE);
            sendToDrone(returnToBase, droneID);
        }
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

        // create dispatch event & assign drone
        DroneDispatchEvent dispatch = new DroneDispatchEvent(zoneID, zoneCenter, incident.getFault());

        // Calculate dynamic deadline based on travel time (gives buffer to calculated time)
        if (incident.getFault() == Faults.PACKET_LOSS){
            double flightTimeSeconds = DroneSubsystem.timeToZone(dronesInfo.get(droneID).getCoordinates(), zoneCenter) + 10.0;
            this.startWatchdog(droneID, flightTimeSeconds);
        }

        EventLogger.info(EventLogger.NO_ID,
                String.format("Assigned and dispatching Drone %d to closest active fire → Zone %d | Coords: (%.1f, %.1f) | Fault: %s",
                        droneID,
                        zoneID,
                        zoneCenter.getX(),
                        zoneCenter.getY(),
                        incident.getFault()), true);

        if (incident.getFault() != Faults.PACKET_LOSS){
            sendToDrone(dispatch, droneID);
        }

        // update fire incident that a drone has been dispatched (optional)
        incident.setEventType(EventType.DRONE_DISPATCHED);
        sendSocket.send(incident, fireSubsystemAddress, fireSubsystemPort);

        // track the assignment
        incident.markFaultHandled();
        droneAssignments.put(droneID, incident);
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
