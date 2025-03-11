package subsystems.drone.states;

import subsystems.drone.events.DroneDispatchEvent;
import subsystems.drone.events.DropAgentEvent;
import subsystems.Event;
import subsystems.drone.DroneSubsystem;

public class IdleState implements DroneState {

    @Override
    public void handleEvent(DroneSubsystem drone, Event event) {
        if (event instanceof DroneDispatchEvent){
            dispatch(drone, (DroneDispatchEvent) event);
        } else if (event instanceof DropAgentEvent) {
            dropAgent(drone, (DropAgentEvent) event);
        }
    }

    @Override
    public void dispatch(DroneSubsystem drone, DroneDispatchEvent event) {
        System.out.printf(
                "[DRONE %d] Received dispatch request: {Zone: %d | Coordinates: (%.1f, %.1f)}%n",
                drone.getDroneID(),
                event.getZoneID(),
                event.getCoords().getX(),
                event.getCoords().getY()
        );

        drone.setZoneID(event.getZoneID());

        OnRouteState onRoute = new OnRouteState(event);
        drone.setState(onRoute);
        drone.getState().travel(drone);
    }

    @Override
    public void travel(DroneSubsystem drone) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot travel, no dispatch request.");
    }

    @Override
    public void dropAgent(DroneSubsystem drone, DropAgentEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot drop agent, not at a target zone.");
    }

    @Override
    public void refill(DroneSubsystem drone) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Already at base, no need to refill.");
    }
}
