package subsystems;

import events.DroneDispatchEvent;
import events.DropAgentEvent;
import events.Event;
import main.EventQueueManager;

public class IdleState implements DroneStates {

    @Override
    public void handleEvent(DroneSubsystem drone, Event event, EventQueueManager sendEvent) {
        if (event instanceof DroneDispatchEvent){
            dispatch(drone, (DroneDispatchEvent) event);
        } else if (event instanceof DropAgentEvent) {
            dropAgent(drone, (DropAgentEvent) event, sendEvent);
        }
    }

    @Override
    public void dispatch(DroneSubsystem drone, DroneDispatchEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Dispatching to Zone: " + event.getZoneID());
        drone.setState(new OnRouteState(event));
    }

    @Override
    public void travel(DroneSubsystem drone, EventQueueManager sendEvent) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot travel, no dispatch request.");
    }

    @Override
    public void dropAgent(DroneSubsystem drone, DropAgentEvent event, EventQueueManager sendEvent) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot drop agent, not at a target zone.");
    }

    @Override
    public void refill(DroneSubsystem drone) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Already at base, no need to refill.");
    }
}
