package subsystems;

import events.DroneDispatchEvent;
import events.DropAgentEvent;
import events.Event;
import main.EventQueueManager;

import java.awt.geom.Point2D;

public class OnRouteState implements DroneStates {
    private DroneDispatchEvent dispatchEvent;

    @Override
    public void handleEvent(DroneSubsystem drone, Event event, EventQueueManager sendEvent) {
        if (event instanceof DroneDispatchEvent){
            dispatch(drone, (DroneDispatchEvent) event);
        } else if (event instanceof DropAgentEvent) {
            dropAgent(drone, (DropAgentEvent) event, sendEvent);
        }
    }

    public OnRouteState(DroneDispatchEvent dispatchEvent) {
        this.dispatchEvent = dispatchEvent;
    }

    @Override
    public void dispatch(DroneSubsystem drone, DroneDispatchEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Already in transit, cannot dispatch.");
    }

    @Override
    public void travel(DroneSubsystem drone, EventQueueManager sendEvent) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Traveling to Zone: " + dispatchEvent.getZoneID());

        Point2D targetCoords = dispatchEvent.getCoords();
        double flightTime = drone.timeToZone(drone.getCoordinates(), targetCoords);

        try {
            Thread.sleep((long) flightTime * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        drone.setCoordinates(targetCoords);
        System.out.println("[DRONE " + drone.getDroneID() + "] Arrived at Zone: " + dispatchEvent.getZoneID());
        drone.setState(new DroppingAgentState());
    }

    @Override
    public void dropAgent(DroneSubsystem drone, DropAgentEvent event, EventQueueManager sendEvent) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot drop agent while in transit.");
    }

    @Override
    public void refill(DroneSubsystem drone) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot refill while in transit.");
    }
}
