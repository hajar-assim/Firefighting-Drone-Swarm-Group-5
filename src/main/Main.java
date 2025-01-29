package main;

import subsystems.FireIncidentSubsystem;
import subsystems.DroneSubsystem;

public class Main {
    public static void main(String[] args) {

        System.out.println("\n === Firefighting Drone System Starting... ===\n");

        // create and start subsystem threads
        EventQueueManager eventQueueManager = new EventQueueManager();
        Thread fireIncidentThread = new Thread(new FireIncidentSubsystem(eventQueueManager));
        //Thread schedulerThread = new Thread(new Scheduler());
        //Thread droneThread = new Thread(new DroneSubsystem());

        fireIncidentThread.start();
        //schedulerThread.start();
        //droneThread.start();

        try {
            fireIncidentThread.join();
            //schedulerThread.join();
            //droneThread.join();
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        System.out.println("\n === Firefighting Drone System Completed Successfully! ===\n");
    }
}
