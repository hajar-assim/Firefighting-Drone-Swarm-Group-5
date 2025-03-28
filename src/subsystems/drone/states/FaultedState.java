package subsystems.drone.states;

import subsystems.Event;
import subsystems.drone.DroneSubsystem;
import subsystems.drone.events.DropAgentEvent;
import subsystems.drone.events.DroneDispatchEvent;

public class FaultedState implements DroneState {
    private final String faultDescription;

    /**
     * Constructs a new FaultedState with a human‑readable description of the fault.
     *
     * @param faultDescription a description of why the drone is faulted
     */
    public FaultedState(String faultDescription) {
        this.faultDescription = faultDescription;
    }

    /**
     * Ignore all events since drone is faulted
     *
     * @param drone the faulted drone
     * @param event the event to ignore
     */
    @Override
    public void handleEvent(DroneSubsystem drone, Event event) {
        // No operation since drone is faulted
    }

    /**
     * Prevents dispatching while in faulted state.
     *
     * @param drone the faulted drone
     * @param event the dispatch event (ignored)
     */
    @Override
    public void dispatch(DroneSubsystem drone, DroneDispatchEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot dispatch, drone is faulted (" + faultDescription + ").");
    }

    /**
     * Prevents travel while in a faulted state.
     *
     * @param drone the faulted drone
     */
    @Override
    public void travel(DroneSubsystem drone) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot travel, drone is faulted (" + faultDescription + ").");
    }

    /**
     * Prevents dropping agent while in a faulted state.
     *
     * @param drone the faulted drone
     * @param event the drop event that is ignored
     */
    @Override
    public void dropAgent(DroneSubsystem drone, DropAgentEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot drop agent, drone is faulted (" + faultDescription + ").");
    }

    /**
     * Returns a string of this faulted state.
     *
     * @return string for logging
     */
    @Override
    public String toString() {
        return "FaultedState: " + faultDescription;
    }
}
