package main;

import subsystems.FireIncidentSubsystem;
import subsystems.DroneSubsystem;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {

        System.out.println("\n === Firefighting Drone System Starting... ===\n");

        // create and start fire incident subsystem thread
        EventQueueManager fireIncidentManager = new EventQueueManager();
        Thread fireIncidentThread = new Thread(new FireIncidentSubsystem(fireIncidentManager));

        // create and start drone subsystem thread
        ArrayList<EventQueueManager> droneManagers = new ArrayList<EventQueueManager>();
        EventQueueManager droneManager1 = new EventQueueManager();
        droneManagers.add(droneManager1);
        //Thread droneThread1 = new Thread(new DroneSubsystem(droneManager1));

        // create and start scheduler thread
        Thread schedulerThread = new Thread(new Scheduler(fireIncidentManager, droneManagers));

        fireIncidentThread.start();
        schedulerThread.start();
        //droneThread.start();

        try {
            fireIncidentThread.join();
            schedulerThread.join();
            //droneThread.join();
        } catch (InterruptedException e) {
            System.err.println("Main thread interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        System.out.println("\n === Firefighting Drone System Completed Successfully! ===\n");
    }
}
