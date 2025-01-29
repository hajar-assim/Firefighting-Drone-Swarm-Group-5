package subsystems;

import events.IncidentEvent;
import main.EventQueueManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class FireIncidentSubsystem implements Runnable{
    private HashMap<Integer, ArrayList<String>> zones;
    private final String INPUT_FOLDER = "data";
    private File eventFile;
    private File zoneFile;
    private EventQueueManager eventQueueManager;

    public FireIncidentSubsystem(EventQueueManager eventQueueManager) {
        this.zones = new HashMap<Integer, ArrayList<String>>();
        this.eventQueueManager = eventQueueManager;
        this.getInputFiles();
    }

    private void getInputFiles(){
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

    private void parseEvents(){
        try (BufferedReader reader = new BufferedReader(new FileReader(this.eventFile))) {
            String line;
            reader.readLine(); //Skip header line

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                int zoneId = Integer.parseInt(parts[1]);
                ArrayList<String> zoneCoordinates = zones.get(zoneId);

                if (zoneCoordinates != null) {
                    IncidentEvent incident = new IncidentEvent(parts[0], zoneId, parts[2], parts[3], zoneCoordinates.get(0), zoneCoordinates.get(1));
                    eventQueueManager.put(incident);
                } else {
                    System.out.println("Warning: No zone data found for Zone ID " + zoneId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseZones(){
        try (BufferedReader reader = new BufferedReader(new FileReader(this.zoneFile))) {
            String line;
            reader.readLine(); // skip header line

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

    @Override
    public void run(){
        this.parseZones();
        this.parseEvents();
    }

}
