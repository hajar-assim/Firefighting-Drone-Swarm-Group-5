package subsystems.drone.states;

import logger.EventLogger;
import subsystems.drone.events.DroneDispatchEvent;
import subsystems.drone.events.DropAgentEvent;
import subsystems.Event;
import subsystems.drone.DroneSubsystem;

/**
 * Represents the idle state of a drone, where it is not engaged in any active mission.
 * This class implements the DroneState interface and defines behavior for handling events
 * while the drone is idle.
 */

public class IdleState implements DroneState {

    /**
     * Handles events for the drone while it is in the IdleState.
     * If the event is a DroneDispatchEvent, the drone is dispatched to a target zone.
     * If the event is a DropAgentEvent, it prints a message indicating that the agent cannot be dropped.
     *
     * @param drone The drone in the IdleState.
     * @param event The event to handle.
     */
    @Override
    public void handleEvent(DroneSubsystem drone, Event event) {
        if (event instanceof DroneDispatchEvent){
            if (((DroneDispatchEvent) event).getZoneID() == 0){
                drone.shutdown();
            }
            else {
                dispatch(drone, (DroneDispatchEvent) event);
            }
        } else if (event instanceof DropAgentEvent) {
            dropAgent(drone, (DropAgentEvent) event);
        }
    }

    /**
     * Dispatches the drone to a specified zone with coordinates. The drone transitions to the OnRouteState.
     *
     * @param drone The drone to dispatch.
     * @param event The dispatch event containing the target zone and coordinates.
     */
    @Override
    public void dispatch(DroneSubsystem drone, DroneDispatchEvent event) {
        EventLogger.info(drone.getDroneID(), String.format("Received dispatch request: {Zone: %d | Coordinates: (%.1f, %.1f)}",
                event.getZoneID(),
                event.getCoords().getX(),
                event.getCoords().getY()), true);

        drone.setZoneID(event.getZoneID());

        OnRouteState onRoute = new OnRouteState(event);
        drone.setState(onRoute);
        drone.getState().travel(drone);
    }

    /**
     * Attempts to make the drone travel while it is in the IdleState.
     * It prints a message indicating that the drone cannot travel without a dispatch request.
     *
     * @param drone The drone that should travel.
     */
    @Override
    public void travel(DroneSubsystem drone) {
        EventLogger.warn(drone.getDroneID(), "Cannot travel without dispatch request.");
    }

    /**
     * Attempts to drop an agent while the drone is in the IdleState.
     * It prints a message indicating that the agent cannot be dropped while the drone is idle.
     *
     * @param drone The drone that should drop the agent.
     * @param event The DropAgentEvent containing information about the agent to drop.
     */
    @Override
    public void dropAgent(DroneSubsystem drone, DropAgentEvent event) {
        EventLogger.warn(drone.getDroneID(), "Cannot drop agent, not at a target zone.");
    }

}
