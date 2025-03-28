package subsystems.drone.states;

import logger.EventLogger;
import main.Scheduler;
import subsystems.Event;
import subsystems.drone.events.DroneDispatchEvent;
import subsystems.drone.events.DropAgentEvent;
import subsystems.drone.DroneSubsystem;

import java.awt.geom.Point2D;

/**
 * Represents the state of a drone when it is dropping an agent (e.g., releasing water or another substance).
 * This class implements the DroneState interface and handles events and actions related to dropping an agent.
 */

public class DroppingAgentState implements DroneState {

    /**
     * Handles events for the drone when it is in the DroppingAgentState.
     * If the event is a DropAgentEvent, it triggers the dropAgent method.
     *
     * @param drone The drone that is in the DroppingAgentState.
     * @param event The event to handle.
     */
    @Override
    public void handleEvent(DroneSubsystem drone, Event event) {
        if (event instanceof DropAgentEvent) {
            dropAgent(drone, (DropAgentEvent) event);
        }
    }


    /**
     * Attempts to dispatch the drone while it is in the DroppingAgentState.
     * It prints a message indicating that the drone cannot be dispatched during this state.
     *
     * @param drone The drone to dispatch.
     * @param event The dispatch event.
     */
    @Override
    public void dispatch(DroneSubsystem drone, DroneDispatchEvent event) {
        EventLogger.warn(drone.getDroneID(), "Cannot dispatch while dropping agent.");
    }


    /**
     * Attempts to make the drone travel while in the DroppingAgentState.
     * It prints a message indicating that the drone cannot travel during this state.
     *
     * @param drone The drone that should travel.
     */
    @Override
    public void travel(DroneSubsystem drone) {
        EventLogger.warn(drone.getDroneID(), "Cannot travel while dropping agent.");
    }


    /**
     * Drops the agent (water or another substance) and updates the drone's state.
     * The drone's water level is reduced by the volume of the dropped agent, and it transitions
     * to the OnRouteState to return to base for a refill.
     *
     * @param drone The drone dropping the agent.
     * @param event The DropAgentEvent containing information about the agent to drop.
     */
    @Override
    public void dropAgent(DroneSubsystem drone, DropAgentEvent event) {
        int volume = event.getVolume();

        // Simulate nozzle jam if volume is -1
        if (volume == -1) {
            EventLogger.error(drone.getDroneID(), "Nozzle jam detected! Transitioning to faulted state.");
            drone.setState(new FaultedState("NOZZLE_JAMMED"));
            return;
        }

        EventLogger.info(drone.getDroneID(), "Dropping agent...");

        try {
            Thread.sleep((long) volume * Scheduler.sleepMultiplier);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        drone.subtractWaterLevel(volume);
        EventLogger.info(drone.getDroneID(), "Dropped " + volume + " liters.");

        // Transition to OnRouteState to return to base
        EventLogger.info(drone.getDroneID(), "Returning to base to refill.");
        OnRouteState toBase = new OnRouteState(new DroneDispatchEvent(0, new Point2D.Double(0, 0), false));
        drone.setZoneID(0);
        drone.setState(toBase);
        drone.getState().travel(drone);
    }

}
