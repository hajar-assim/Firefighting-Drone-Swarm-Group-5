package subsystems;

import events.DroneDispatchEvent;
import events.DropAgentEvent;
import events.Event;
import main.EventQueueManager;

public class RefillingState implements DroneStates {

    @Override
    public void handleEvent(DroneSubsystem drone, Event event, EventQueueManager sendEvent) {
        if (event instanceof DroneDispatchEvent){
            dispatch(drone, (DroneDispatchEvent) event);
        } else if (event instanceof DropAgentEvent) {
            dropAgent(drone, (DropAgentEvent) event, sendEvent);
        }
    }

    @Override
    public void dispatch(DroneSubsystem drone, events.DroneDispatchEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot dispatch while refilling.");
    }

    @Override
    public void travel(DroneSubsystem drone, EventQueueManager sendEvent) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Returning to base for refilling.");
    }

    @Override
    public void dropAgent(DroneSubsystem drone, events.DropAgentEvent event, EventQueueManager sendEvent) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot drop agent while refilling.");
    }

    @Override
    public void refill(DroneSubsystem drone) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Refilling water...");
        drone.setWaterLevel(15);
        System.out.println("[DRONE " + drone.getDroneID() + "] Refilled to full capacity.");
        drone.setState(new IdleState());
    }
}
