package main;

import subsystems.fire_incident.FireIncidentSubsystem;
import subsystems.drone.DroneSubsystem;

import java.util.*;

public class Main {
    public static void main(String[] args) {

        System.out.println("\n === Firefighting Drone System Starting... ===\n");
        String inputFolderPath = "data";
        int numDrones = 1;
        Map<Integer, DroneSubsystem> dronesByID = new HashMap<>();
        ArrayList<Thread> droneThreads = new ArrayList<>();

        EventQueueManager schedulerManager = new EventQueueManager("Scheduler Queue");
        EventQueueManager fireIncidentManager = new EventQueueManager("Fire Incident Queue");

        // create fire incident subsystem thread
        Thread fireIncidentThread = new Thread(new FireIncidentSubsystem(fireIncidentManager, schedulerManager, inputFolderPath));

        for (int i=0; i < numDrones; i++){
            EventQueueManager droneManager = new EventQueueManager("Drone " + (i + 1) + " Queue");
            DroneSubsystem drone = new DroneSubsystem(droneManager, schedulerManager);
            droneThreads.add(new Thread(drone));
            dronesByID.put(drone.getDroneID(), drone);
        }

        // create scheduler thread
        Thread schedulerThread = new Thread(new Scheduler(schedulerManager, fireIncidentManager, dronesByID));

        // start threads
        fireIncidentThread.start();
        schedulerThread.start();

        for (Thread droneThread: droneThreads){
            droneThread.start();
        }

        try {
            fireIncidentThread.join();
            schedulerThread.join();
            for (Thread droneThread: droneThreads){
                droneThread.join();
            }
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        System.out.println("\n === Firefighting Drone System Completed Successfully! ===\n");
    }
}