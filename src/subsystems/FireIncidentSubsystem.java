package subsystems;

import events.*;
import java.io.*;
import java.net.*;
import java.util.HashSet;

/**
 * The FireIncidentSubsystem class is responsible for processing fire incident data.
 * It reads input files containing fire incident events and zone data, then adds incidents
 * to the EventQueueManager.
 */
public class FireIncidentSubsystem {
    private final String INPUT_FOLDER;
    private File eventFile;
    private File zoneFile;
    private DatagramSocket receiveSocket;
    private DatagramSocket sendSocket;
    private InetAddress schedulerAddress;
    private int schedulerPort;
    private HashSet<Integer> activeFires = new HashSet<>();

    /**
     * Constructs a FireIncidentSubsystem.
     * @param inputFolderPath The path to input folder.
     */
    public FireIncidentSubsystem(String inputFolderPath, InetAddress schedulerAddress, int schedulerPort) {
        try{
            this.receiveSocket = new DatagramSocket();
            this.sendSocket = new DatagramSocket();
        } catch (SocketException se) {   // Can't create the socket.
            se.printStackTrace();
            System.exit(1);
        }

        this.schedulerAddress = schedulerAddress;
        this.schedulerPort = schedulerPort;
        this.INPUT_FOLDER = inputFolderPath;
        this.getInputFiles();
    }

    private void send(Event incident) throws IOException{
        // Serialize incident
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(incident);
        byte msg[] = byteArrayOutputStream.toByteArray();

        DatagramPacket packet = new DatagramPacket(msg, msg.length, schedulerAddress, schedulerPort);

        sendSocket.send(packet);
    }

    private Event receive() throws IOException, ClassNotFoundException {
        byte data[] = new byte[100];
        DatagramPacket packet = new DatagramPacket(data, data.length);

        receiveSocket.receive(packet);

        // Deserialize object
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

        return (Event) objectInputStream.readObject();
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

                IncidentEvent incident = new IncidentEvent(parts[0], zoneId, parts[2], parts[3]);
                System.out.println("\n[FIRE INCIDENT SYSTEM] New incident detected: " + incident);
                send(incident);
                activeFires.add(zoneId);

                IncidentEvent event = (IncidentEvent) receive();
                System.out.println("\n[FIRE INCIDENT SYSTEM] Scheduler Response: " + event);

                // If the fire was extinguished before all events were reported, remove it
                if (event.getEventType() == EventType.FIRE_EXTINGUISHED) {
                    removeFire(event.getZoneID());
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("[FIRE INCIDENT SYSTEM] All fires reported, waiting for all fires to be extinguished...");
            waitForFiresToBeExtinguished();

            // only send EVENTS_DONE once all fires are extinguished
            IncidentEvent noMoreIncidents = new IncidentEvent("", 0, "EVENTS_DONE", "NONE");
            System.out.println("[FIRE INCIDENT SYSTEM] All fires extinguished. Sending EVENTS_DONE.");
            send(noMoreIncidents);

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
                send(zoneEvent);
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
            try{
                IncidentEvent event = (IncidentEvent) receive();

                if (event.getEventType() == EventType.FIRE_EXTINGUISHED) {
                    removeFire(event.getZoneID());
                }
            } catch (Exception e) {
                e.printStackTrace();
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