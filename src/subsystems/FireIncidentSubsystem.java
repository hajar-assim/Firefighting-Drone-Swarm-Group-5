package subsystems;

import events.EventType;
import events.IncidentEvent;
import events.Severity;
import main.EventQueueManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * The FireIncidentSubsystem class is responsible for processing fire incident data.
 * It reads input files containing fire incident events and zone data, then adds incidents
 * to the EventQueueManager.
 */
public class FireIncidentSubsystem implements Runnable {
    private HashMap<Integer, ArrayList<String>> zones;
    private final String INPUT_FOLDER;
    private File eventFile;
    private File zoneFile;
    private EventQueueManager receiveEventManager;
    private EventQueueManager sendEventManager;

    /**
     * Constructs a FireIncidentSubsystem with an EventQueueManager.
     *
     * @param receiveEventManager The queue manager responsible for handling received events.
     */
    public FireIncidentSubsystem(EventQueueManager receiveEventManager, EventQueueManager sendEventManager, String inputFolderPath) {
        this.zones = new HashMap<Integer, ArrayList<String>>();
        this.receiveEventManager = receiveEventManager;
        this.sendEventManager = sendEventManager;
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
                ArrayList<String> zoneCoordinates = zones.get(zoneId);

                if (zoneCoordinates != null) {
                    IncidentEvent incident = new IncidentEvent(parts[0], zoneId, parts[2], parts[3], zoneCoordinates.get(0), zoneCoordinates.get(1), "Drone");
                    sendEventManager.put(incident);
                    IncidentEvent event = receiveEventManager.get();
                    System.out.println("Fire Incident Subsystem received response from Scheduler: " + event);
                } else {
                    System.out.println("Warning: No zone data found for Zone ID " + zoneId);
                }
            }

            IncidentEvent noMoreIncidents = new IncidentEvent("", 0, "EVENTS_DONE", "HIGH", "(0;0)", "(0;0)", "");
            sendEventManager.put(noMoreIncidents);
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
                ArrayList<String> zoneCoordinates = new ArrayList<>(Arrays.asList(parts[1], parts[2]));
                zones.put(zoneId, zoneCoordinates);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs the FireIncidentSubsystem by first parsing the zone data
     * and then processing the event file to queue incident events.
     */
    @Override
    public void run() {
        this.parseZones();
        this.parseEvents();
    }
}