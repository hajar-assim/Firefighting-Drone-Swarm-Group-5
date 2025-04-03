package subsystems.fire_incident;

import logger.EventLogger;
import main.EventSocket;
import subsystems.EventType;
import subsystems.fire_incident.events.IncidentEvent;
import subsystems.fire_incident.events.ZoneEvent;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;

/**
 * The FireIncidentSubsystem class is responsible for processing fire incident data.
 * It reads input files containing fire incident events and zone data, then adds incidents
 * to the EventQueueManager.
 */
public class FireIncidentSubsystem {
    public static Point2D BASE_COORDINATES = new Point2D.Double(0,0);
    private final String INPUT_FOLDER;
    private File eventFile;
    private File zoneFile;
    private EventSocket socket;
    private InetAddress schedulerAddress;
    private int schedulerPort;
    private HashSet<Integer> activeFires = new HashSet<>();

    /**
     * Constructs a FireIncidentSubsystem.
     *
     * @param inputFolderPath The path to input folder.
     * @param schedulerAddress The IP address of the scheduler to send events to
     * @param schedulerPort The port of the scheduler to send events to
     */
    public FireIncidentSubsystem(String inputFolderPath, InetAddress schedulerAddress, int schedulerPort) {
        this.socket = new EventSocket(7000);
        this.schedulerAddress = schedulerAddress;
        this.schedulerPort = schedulerPort;
        this.INPUT_FOLDER = inputFolderPath;
        this.getInputFiles();
    }

    /**
     * Retrieves the input files from the specified directory.
     * Assigns the event and zone files accordingly.
     */
    private void getInputFiles() {
        File folder = new File(INPUT_FOLDER);
        File[] files = folder.listFiles();
        if (files == null) {
            EventLogger.error(EventLogger.NO_ID, "No files found in the input folder.");
            return;
        }

        for (File file : files) {
            if (file.getName().contains("zone")) {
                this.zoneFile = file;
            } else if (file.getName().contains("events")) {
                this.eventFile = file;
            }
        }
    }

    /**
     * Parses the fire incident event file and adds events to the EventQueueManager.
     * Each event is associated with a zone, and if zone data is missing, a warning is logged.
     */
    private void parseEvents() {
        try (BufferedReader reader = new BufferedReader(new FileReader(this.eventFile))) {
            String line;
            reader.readLine(); // Skip header line

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 0) {
                    EventLogger.error(EventLogger.NO_ID, "Invalid event data: " + line);
                    return;
                }

                int zoneId = Integer.parseInt(parts[1]);

                // Parse from data file
                String timestamp = parts[0];
                EventType eventType = EventType.fromString(parts[2]);
                Severity severity = Severity.fromString(parts[3]);
                Faults fault;

                try {
                    fault = Faults.fromString(parts[4]);
                } catch (IllegalArgumentException e) {
                    EventLogger.error(EventLogger.NO_ID, "Invalid fault type '" + parts[4] + "', defaulting to fault type NONE.");
                    fault = Faults.NONE;
                }

                // Create IncidentEvent with injected fault
                IncidentEvent incident = new IncidentEvent(timestamp, zoneId, eventType, severity, fault);
                EventLogger.info(EventLogger.NO_ID, "New incident detected: {" + incident + "}", true);

                socket.send(incident, schedulerAddress, schedulerPort);
                activeFires.add(zoneId);
            }

            EventLogger.info(EventLogger.NO_ID, "All fires reported, waiting for all fires to be extinguished...\n", false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses the zone file and stores zone data in a hashmap.
     * Each zone ID is mapped to its corresponding start and end coordinates.
     */
    private void parseZones() {
        try (BufferedReader reader = new BufferedReader(new FileReader(this.zoneFile))) {
            String line;
            reader.readLine(); // Skip header line

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                int zoneId = Integer.parseInt(parts[0]);
                ZoneEvent zoneEvent = new ZoneEvent(zoneId, parts[1], parts[2]);
                EventLogger.info(EventLogger.NO_ID, "New zone detected: {" + zoneEvent + "}", true);

                socket.send(zoneEvent, schedulerAddress, schedulerPort);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Removes the fire from the active fires list once it is extinguished.
     *
     * @param zoneID The identifier of the fire zone to be removed.
     *               If the fire exists in the active fires list, it will be removed.
     */
    private void removeFire(int zoneID) {
        if (activeFires.contains(zoneID)) {
            activeFires.remove(zoneID);
            EventLogger.info(EventLogger.NO_ID, "Fire extinguished at Zone " + zoneID, true);
        }
    }


    /**
     * Continuously listens for fire extinguishment events and removes corresponding fires
     * from the active fires list until all fires are extinguished.
     * This method will block until all active fires have been extinguished.
     */
    private void waitForFiresToBeExtinguished() {
        while (!activeFires.isEmpty()) {
            IncidentEvent event = (IncidentEvent) socket.receive();
            EventLogger.info(EventLogger.NO_ID, "Received event: " + event, false);

            if (event.getEventType() == EventType.FIRE_EXTINGUISHED) {
                removeFire(event.getZoneID());
            } else {
                EventLogger.info(EventLogger.NO_ID, "Scheduler Response: {" + event + "}", false);
            }
        }
    }


    /**
     * Runs the FireIncidentSubsystem by first parsing the zone data
     * and then processing the event file to queue incident events.
     */
    public void run() {
        this.parseZones();
        this.parseEvents();

        waitForFiresToBeExtinguished();

        // only send EVENTS_DONE once all fires are extinguished
        IncidentEvent noMoreIncidents = new IncidentEvent("", 0, EventType.EVENTS_DONE, Severity.NONE, Faults.NONE);
        EventLogger.info(EventLogger.NO_ID, "All fires extinguished. Sending EVENTS_DONE.", true);
        socket.send(noMoreIncidents, schedulerAddress, schedulerPort);

        socket.getSocket().close();

    }

    public static void main(String[] args) {
        System.out.println("[FIRE INCIDENT SYSTEM] Fire Incident System has started.");
        InetAddress address = null;
        try{
            address = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }

        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem("data", address, 5000);
        fireIncidentSubsystem.run();
    }
}