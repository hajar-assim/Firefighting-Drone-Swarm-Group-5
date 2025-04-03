package subsystems.drone.states;

import logger.EventLogger;
import subsystems.Event;
import subsystems.drone.DroneSubsystem;
import subsystems.drone.events.DropAgentEvent;
import subsystems.drone.events.DroneDispatchEvent;
import subsystems.fire_incident.Faults;

import java.util.ArrayList;

public class FaultedState implements DroneState {
    private final Faults faultDescription;
    private static final int RECOVERY_TIME = 10000;
    public static final ArrayList<Faults> UNRECOVERABLE_FAULTS = new ArrayList<>() {{
        add(Faults.NOZZLE_JAMMED);
    }};

    /**
     * Constructs a new FaultedState with a human‑readable description of the fault.
     *
     * @param faultDescription a description of why the drone is faulted
     */
    public FaultedState(Faults faultDescription) {
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
        if (event instanceof DroneDispatchEvent){
            if (UNRECOVERABLE_FAULTS.contains(((DroneDispatchEvent) event).getFault())){
                drone.shutdown();
            }
            else {
                // Simulate recovering
                try {
                    Thread.sleep(RECOVERY_TIME);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                EventLogger.info(drone.getDroneID(), "Recovered from fault: " + ((DroneDispatchEvent) event).getFault() + ", returning to base", false);
                dispatch(drone, (DroneDispatchEvent) event);
            }
        }
    }

    /**
     * Prevents dispatching while in faulted state.
     *
     * @param drone the faulted drone
     * @param event the dispatch event (ignored)
     */
    @Override
    public void dispatch(DroneSubsystem drone, DroneDispatchEvent event) {
        OnRouteState onRoute = new OnRouteState(event);
        drone.setState(onRoute);
        drone.getState().travel(drone);
    }

    /**
     * Prevents travel while in a faulted state.
     *
     * @param drone the faulted drone
     */
    @Override
    public void travel(DroneSubsystem drone) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot travel, drone is faulted (" + faultDescription.toString() + ").");
    }

    /**
     * Prevents dropping agent while in a faulted state.
     *
     * @param drone the faulted drone
     * @param event the drop event that is ignored
     */
    @Override
    public void dropAgent(DroneSubsystem drone, DropAgentEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot drop agent, drone is faulted (" + faultDescription.toString() + ").");
    }

    /**
     * Returns a string of this faulted state.
     *
     * @return string for logging
     */
    @Override
    public String toString() {
        return "FaultedState: " + faultDescription.toString();
    }

    public Faults getFaultDescription(){
        return this.faultDescription;
    }
}
