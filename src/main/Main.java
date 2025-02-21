package main;

import subsystems.FireIncidentSubsystem;
import subsystems.DroneSubsystem;

public class Main {
    public static void main(String[] args) {

        System.out.println("\n === Firefighting Drone System Starting... ===\n");
        String inputFolderPath = "data";

        EventQueueManager schedulerManager = new EventQueueManager("Scheduler Queue");
        EventQueueManager fireIncidentManager = new EventQueueManager("Fire Incident Queue");
        EventQueueManager droneManager = new EventQueueManager("Drone Queue");
        EventQueueManager droneSchedulerQueue = new EventQueueManager("Drone-Scheduler Queue"); // Missing queue


        // create fire incident subsystem thread
        Thread fireIncidentThread = new Thread(new FireIncidentSubsystem(fireIncidentManager, schedulerManager, inputFolderPath));

        // create drone subsystem thread
        int droneId = 1;
        Thread droneThread = new Thread(new DroneSubsystem(droneManager, schedulerManager));

        // create scheduler thread
        Thread schedulerThread = new Thread(new Scheduler(schedulerManager, fireIncidentManager, droneManager));

        // start threads
        fireIncidentThread.start();
        schedulerThread.start();
        droneThread.start();

        try {
            fireIncidentThread.join();
            schedulerThread.join();
            droneThread.join();
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        System.out.println("\n === Firefighting Drone System Completed Successfully! ===\n");
    }
}