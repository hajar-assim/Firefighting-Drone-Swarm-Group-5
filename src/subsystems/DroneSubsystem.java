package subsystems;

import events.*;
import main.EventQueueManager;

import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The DroneSubsystem class simulates the behavior of a drone unit that receives
 * incident events, processes them, and sends back a response.
 * It continuously listens for new events from the receive event queue
 * and dispatches responses to the send event queue.
 */
public class DroneSubsystem implements Runnable {
    private static final AtomicInteger nextId = new AtomicInteger(1);
    private final int MAX_AGENT = 15;
    private final int NOZZLE_OPEN_TIME = 1;
    private EventQueueManager sendEventManager;
    private EventQueueManager receiveEventManager;
    private final int droneID;
    private DroneState droneState;
    private volatile boolean running;

    /**
     * Constructs a DroneSubsystem with the specified event managers.
     *
     * @param receiveEventManager The event queue manager from which the subsystem receives incident events.
     * @param sendEventManager    The event queue manager to which the subsystem sends processed events.
     */
    public DroneSubsystem(EventQueueManager receiveEventManager, EventQueueManager sendEventManager) {
        this.receiveEventManager = receiveEventManager;
        this.sendEventManager = sendEventManager;
        this.droneID = nextId.getAndIncrement();
        this.droneState = new DroneState(DroneStatus.IDLE, 0, new Point2D.Double(0,0), 100, MAX_AGENT);
        this.running = false;
    }

    private long timeToZone(Point2D startCoords, Point2D endCoords){
        double distance = startCoords.distance(endCoords);

        return (long) ((distance - 46.875) / 15 + 6.25);
    }

    private void dispatchDrone(DroneDispatchEvent droneDispatchEvent){
        this.droneState.setZoneID(droneDispatchEvent.getZoneID());
        this.droneState.setStatus(DroneStatus.ON_ROUTE);

        System.out.println("Drone " + this.droneID + " received Dispatch Request to Zone: " + droneDispatchEvent.getZoneID() + " " + droneDispatchEvent.getCoords());

        try{
            Thread.sleep(this.timeToZone(this.droneState.getCoordinates(), droneDispatchEvent.getCoords()) * 1000);
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        this.droneState.setCoordinates(droneDispatchEvent.getCoords());

        if(droneDispatchEvent.getZoneID() == 0){
            this.running = false;
            System.out.println("No more events, drone returned to base and shutting down");
            return;
        }

        DroneArrivedEvent arrivedEvent = new DroneArrivedEvent(this.droneID, this.droneState.getZoneID());
        this.sendEventManager.put(arrivedEvent);
    }

    private void dropAgent(DropAgentEvent dropAgentEvent){
        this.droneState.setStatus(DroneStatus.DROPPING_AGENT);

        while (dropAgentEvent.getVolume() > 0) {
            // Determine how much can be dropped
            double dropAmount = Math.min(this.droneState.getWaterLevel(), dropAgentEvent.getVolume());

            try{
                Thread.sleep(NOZZLE_OPEN_TIME * 1000);
            } catch (InterruptedException e){
                e.printStackTrace();
            }

            // Reduce both the drone's water level and the event's remaining volume
            this.droneState.setWaterLevel((int) (this.droneState.getWaterLevel() - dropAmount));
            dropAgentEvent.setVolume((int) (dropAgentEvent.getVolume() - dropAmount));

            System.out.println("Dropped " + dropAmount + " liters. Remaining: " + dropAgentEvent.getVolume());

            // If there's still more water needed but the drone is empty, go refill
            if (dropAgentEvent.getVolume() > 0 && this.droneState.getWaterLevel() == 0) {
                this.droneRefill();
            }
        }
    }

    private void droneRefill(){
        System.out.println("Drone " + this.droneID + " returning to Base (0,0) to refill.");
        this.droneState.setStatus(DroneStatus.REFILLING);
        Point2D zoneCoords = this.droneState.getCoordinates();

        try{
            Thread.sleep(this.timeToZone(this.droneState.getCoordinates(), new Point2D.Double(0,0)) * 1000);
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        this.droneState.setCoordinates(new Point2D.Double(0, 0));
        this.droneState.setWaterLevel(MAX_AGENT);
        System.out.println("Drone " + this.droneID + " refilled to " + this.droneState.getWaterLevel() + " liters.");
        this.droneState.setStatus(DroneStatus.ON_ROUTE);

        try{
            Thread.sleep(this.timeToZone(this.droneState.getCoordinates(), zoneCoords) * 1000);
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        this.droneState.setCoordinates(zoneCoords);
        System.out.println("Drone " + this.droneID + " returned to drop site.");
    }


    /**
     * Starts the drone subsystem, which continuously listens for new incident events.
     * When an event is received, it processes the request and dispatches a response.
     * If an "EVENTS_DONE" event is received, the subsystem shuts down.
     */
    @Override
    public void run() {
        this.running = true;
        while (this.running) {
            Event event = receiveEventManager.get();

            if (event instanceof DroneDispatchEvent droneDispatchEvent){
                this.dispatchDrone(droneDispatchEvent);
            } else if (event instanceof DropAgentEvent dropAgentEvent) {
                this.dropAgent(dropAgentEvent);
            }

        }
    }
}
