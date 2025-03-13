package subsystems.fire_incident;

import main.EventSocket;
import subsystems.EventType;
import subsystems.fire_incident.events.IncidentEvent;
import subsystems.fire_incident.events.Severity;
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
    private EventSocket sendSocket;
    private EventSocket receiveSocket;
    private InetAddress schedulerAddress;
    private int schedulerPort;
    private HashSet<Integer> activeFires = new HashSet<>();

    /**
     * Constructs a FireIncidentSubsystem.
     *
     * @param inputFolderPath The path to input folder.
     */
    public FireIncidentSubsystem(String inputFolderPath, InetAddress schedulerAddress, int schedulerPort) {
        this.sendSocket = new EventSocket();
        this.receiveSocket = new EventSocket();
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
            System.out.println("No files found in the input folder.");
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
                int zoneId = Integer.parseInt(parts[1]);

                IncidentEvent incident = new IncidentEvent(parts[0], zoneId, EventType.fromString(parts[2]), Severity.fromString(parts[3]));
                System.out.println("\n[FIRE INCIDENT SYSTEM] New incident detected: {" + incident + "}");
                sendSocket.send(incident, schedulerAddress, schedulerPort);
                activeFires.add(zoneId);

                IncidentEvent event = (IncidentEvent) receiveSocket.receive();
                System.out.println("\n[FIRE INCIDENT SYSTEM] Scheduler Response: {" + event + "}");

                while(event.getEventType() != EventType.DRONE_DISPATCHED){
                    // If the fire was extinguished before all events were reported, remove it
                    if (event.getEventType() == EventType.FIRE_EXTINGUISHED) {
                        removeFire(event.getZoneID());
                    }
                    event = (IncidentEvent) receiveSocket.receive();
                    System.out.println("\n[FIRE INCIDENT SYSTEM] Scheduler Response: {" + event + "}");
                }
            }

            System.out.println("[FIRE INCIDENT SYSTEM] All fires reported, waiting for all fires to be extinguished...");
            waitForFiresToBeExtinguished();

            // only send EVENTS_DONE once all fires are extinguished
            IncidentEvent noMoreIncidents = new IncidentEvent("", 0, EventType.EVENTS_DONE, Severity.NONE);
            System.out.println("[FIRE INCIDENT SYSTEM] All fires extinguished. Sending EVENTS_DONE.");
            sendSocket.send(noMoreIncidents, schedulerAddress, schedulerPort);

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
                sendSocket.send(zoneEvent, schedulerAddress, schedulerPort);
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
            System.out.println("[FIRE INCIDENT SYSTEM] Fire extinguished at Zone " + zoneID);
        }
    }


    /**
     * Continuously listens for fire extinguishment events and removes corresponding fires
     * from the active fires list until all fires are extinguished.
     * This method will block until all active fires have been extinguished.
     */
    private void waitForFiresToBeExtinguished() {
        while (!activeFires.isEmpty()) {
            IncidentEvent event = (IncidentEvent) receiveSocket.receive();

            if (event.getEventType() == EventType.FIRE_EXTINGUISHED) {
                removeFire(event.getZoneID());
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
    }

    public static void main(String args[]) {
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