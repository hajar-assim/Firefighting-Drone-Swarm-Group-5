package subsystems.drone.states;

import subsystems.Event;
import subsystems.drone.events.DroneDispatchEvent;
import subsystems.drone.events.DroneUpdateEvent;
import subsystems.drone.DroneSubsystem;
import subsystems.drone.events.DropAgentEvent;
import subsystems.fire_incident.FireIncidentSubsystem;

public class RefillingState implements DroneState {

    @Override
    public void handleEvent(DroneSubsystem drone, Event event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Ignoring event while refilling.");
    }

    @Override
    public void travel(DroneSubsystem drone) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot travel while refilling.");
    }

    @Override
    public void dropAgent(DroneSubsystem drone, DropAgentEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot drop agent while refilling.");
    }

    @Override
    public void dispatch(DroneSubsystem drone, DroneDispatchEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot dispatch, currently refilling.");
    }

    @Override
    public void refill(DroneSubsystem drone) {
        drone.setCoordinates(FireIncidentSubsystem.BASE_COORDINATES);
        System.out.println("[DRONE " + drone.getDroneID() + "] Refilling agent tank...");

        // assume auto-refill upon return to base (no sleep)

        // reset water level
        drone.setWaterLevel(drone.getWaterLevel());
        System.out.println("[DRONE " + drone.getDroneID() + "] Refilled to " + drone.getWaterLevel() + " liters.");

        // transition back to IdleState
        IdleState idleState = new IdleState();
        System.out.println("[DRONE " + drone.getDroneID() + "] Now idle and ready for dispatch.");
        drone.setState(idleState);

        // notify the scheduler that the drone is ready
        DroneUpdateEvent droneUpdateEvent = new DroneUpdateEvent(drone.getDroneID(), idleState);
        drone.getSendEventManager().put(droneUpdateEvent);
    }
}
