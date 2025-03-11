package subsystems.drone.states;

import subsystems.drone.events.DroneDispatchEvent;
import subsystems.drone.events.DropAgentEvent;
import subsystems.Event;
import subsystems.drone.DroneSubsystem;

public interface DroneState {
    /**
     * Handles generic events related to drone operations.
     */
    void handleEvent(DroneSubsystem drone, Event event);

    /**
     * Transitions the drone to a dispatched state.
     */
    void dispatch(DroneSubsystem drone, DroneDispatchEvent event);

    /**
     * Moves the drone to a new location.
     */
    void travel(DroneSubsystem drone);

    /**
     * Drops firefighting agent at the target zone.
     */
    void dropAgent(DroneSubsystem drone, DropAgentEvent event);

    /**
     * Refills the drone’s water/foam tank.
     */
    void refill(DroneSubsystem drone);
}
