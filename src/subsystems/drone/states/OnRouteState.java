package subsystems.drone.states;

import logger.EventLogger;
import main.Scheduler;
import subsystems.drone.events.DroneArrivedEvent;
import subsystems.drone.events.DroneDispatchEvent;
import subsystems.drone.events.DroneUpdateEvent;
import subsystems.drone.events.DropAgentEvent;
import subsystems.Event;
import subsystems.drone.DroneSubsystem;
import subsystems.fire_incident.Faults;

import java.awt.geom.Point2D;

/**
 * Represents the state of a drone when it is on route to a target zone or base.
 * This class implements the DroneState interface and handles events related to
 * dispatching, traveling, and dropping agents while the drone is in transit.
 */

public class OnRouteState implements DroneState {
    private DroneDispatchEvent dispatchEvent;

    /**
     * Handles events while the drone is in the OnRouteState.
     * If the event is a DroneDispatchEvent, the drone is redirected to a new target zone.
     * If the event is a DropAgentEvent, the drone transitions to the DroppingAgentState to drop the agent.
     *
     * @param drone The drone in the OnRouteState.
     * @param event The event to handle.
     */
    @Override
    public void handleEvent(DroneSubsystem drone, Event event) {
        if (event instanceof DroneDispatchEvent) {
            EventLogger.info(drone.getDroneID(), "Redirecting to a new target zone.");
            dispatch(drone, (DroneDispatchEvent) event);
        } else if (event instanceof DropAgentEvent dropAgentEvent) {
            EventLogger.info(drone.getDroneID(), "Received order to drop " + dropAgentEvent.getVolume() + "L of water.");
            drone.setState(new DroppingAgentState());
            drone.getState().handleEvent(drone, dropAgentEvent);
        } else {
            EventLogger.warn(drone.getDroneID(), "Ignoring event while in transit.");
        }
    }

    /**
     * Constructs an OnRouteState with the provided dispatch event.
     *
     * @param dispatchEvent The DroneDispatchEvent containing the dispatch details.
     */
    public OnRouteState(DroneDispatchEvent dispatchEvent) {
        this.dispatchEvent = dispatchEvent;
    }

    /**
     * Prevents the drone from being dispatched while it is in transit.
     * It prints a message indicating that the drone is already on route and cannot be dispatched.
     *
     * @param drone The drone to dispatch.
     * @param event The dispatch event.
     */
    @Override
    public void dispatch(DroneSubsystem drone, DroneDispatchEvent event) {
        EventLogger.warn(drone.getDroneID(), "Cannot dispatch while on route.");
    }


    /**
     * Simulates the drone traveling to a target zone or base.
     * The drone's flight time is calculated based on the distance to the target coordinates,
     * and the drone arrives at the target location after the estimated flight time.
     *
     * @param drone The drone traveling to the target zone.
     */
    @Override
    public void travel(DroneSubsystem drone) {
        Point2D targetCoords = dispatchEvent.getCoords();
        double flightTime = drone.timeToZone(drone.getCoordinates(), targetCoords);
        boolean returningToBase = dispatchEvent.getZoneID() == 0;
        String onRoute = returningToBase ? "Base" : "Zone: " + drone.getZoneID();

        EventLogger.info(drone.getDroneID(),
                String.format("On route to %s | Estimated time: %.2f seconds", onRoute, flightTime));

        try {
            Thread.sleep((long) flightTime * Scheduler.sleepMultiplier);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check if a fault is to be simulated
        if (dispatchEvent.isSimulateFault()) {
            Faults injectedFault = dispatchEvent.getFault();
            String faultDescription;
            if (injectedFault == Faults.NOZZLE_JAMMED) {
                faultDescription = "NOZZLE_JAMMED";
            } else {
                faultDescription = "DRONE_STUCK_IN_FLIGHT";
            }
            EventLogger.info(drone.getDroneID(), "Simulating " + faultDescription + " fault mid-flight. Not sending arrival event.");
            // Transition to FaultedState
            drone.setState(new FaultedState(faultDescription));
            return;
        }

        drone.setCoordinates(targetCoords);
        EventLogger.info(drone.getDroneID(), "Arrived at " + onRoute);

        if (returningToBase) {
            refill(drone);
        }
    }

    /**
     * Prevents the drone from dropping an agent while it is in transit.
     * It prints a message indicating that the drone cannot drop an agent while traveling.
     *
     * @param drone The drone attempting to drop the agent.
     * @param event The DropAgentEvent containing the agent to drop.
     */
    @Override
    public void dropAgent(DroneSubsystem drone, DropAgentEvent event) {
        EventLogger.warn(drone.getDroneID(), "Cannot drop agent while in transit.");
    }


    /**
     * Refills the drone's water level and resets its flight time when it returns to base.
     * It transitions the drone to the IdleState once it has been refilled.
     *
     * @param drone The drone returning to base to refill.
     */
    private void refill(DroneSubsystem drone){
        // reset water level and flight time
        drone.setWaterLevel(15);
        drone.setFlightTime(10 * 60);
        EventLogger.info(drone.getDroneID(), "Refilled to " + drone.getWaterLevel() + " liters.");

        // transition back to IdleState
        IdleState idleState = new IdleState();
        EventLogger.info(drone.getDroneID(), "Now idle and ready for dispatch.\n");
        drone.setState(idleState);
    }
}
