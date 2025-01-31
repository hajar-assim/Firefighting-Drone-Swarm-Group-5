package subsystems;

import events.EventType;
import events.IncidentEvent;
import main.EventQueueManager;

public class DroneSubsystem implements Runnable {
    private EventQueueManager droneManager;

    public DroneSubsystem(EventQueueManager droneManager) {
        this.droneManager = droneManager;
    }

    public void run() {
        System.out.println("Drone Subsystem started...");

        while (true) {
            IncidentEvent request = droneManager.get("DroneSubsystem");

            if (request.getEventType() == EventType.EVENTS_DONE) {
                System.out.println("DroneSubsystem received EVENTS_DONE. Shutting down...");
                return;
            }

            System.out.println("DroneSubsystem received request: Zone " + request.getZoneId() + ", Severity: " + request.getSeverity());

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.out.println("DroneSubsystem interrupted. Exiting...");
                return;
            }

            System.out.println("DroneSubsystem completed action for Zone " + request.getZoneId());

            // Send response back to the scheduler
            request.setReceiver("Scheduler");
            droneManager.put(request);
        }
    }
}
