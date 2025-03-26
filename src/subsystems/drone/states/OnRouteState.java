package subsystems.drone.states;

import main.Scheduler;
import subsystems.drone.events.DroneArrivedEvent;
import subsystems.drone.events.DroneDispatchEvent;
import subsystems.drone.events.DroneUpdateEvent;
import subsystems.drone.events.DropAgentEvent;
import subsystems.Event;
import subsystems.drone.DroneSubsystem;

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
            System.out.println("[DRONE " + drone.getDroneID() + "] Redirecting to a new target zone.");
            dispatch(drone, (DroneDispatchEvent) event);
        } else if (event instanceof DropAgentEvent dropAgentEvent) {
            System.out.println("[DRONE " + drone.getDroneID() + "] Received order to drop " + dropAgentEvent.getVolume() + "L of water.");
            drone.setState(new DroppingAgentState());
            drone.getState().handleEvent(drone, dropAgentEvent);
        } else {
            System.out.println("[DRONE " + drone.getDroneID() + "] Ignoring event while in transit.");
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
        System.out.println("[DRONE " + drone.getDroneID() + "] Already in transit, cannot dispatch.");
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

        System.out.println("\n[DRONE " + drone.getDroneID() + "] On route to " + onRoute
                + " | Estimated time: " + String.format("%.2f seconds", flightTime));

        try {
            Thread.sleep((long) flightTime * Scheduler.sleepMultiplier);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // If simulateStuckFault flag is true then set drone state to stuck
        if (dispatchEvent.isSimulateStuckFault()) {
            System.out.println("[DRONE " + drone.getDroneID() + "] Simulating stuck mid-flight. Not sending arrival event.");

            // Transition to FaultedState so that this drone is not available
            drone.setState(new FaultedState("DRONE_STUCK_IN_FLIGHT"));
            return;
        }

        drone.setCoordinates(targetCoords);
        System.out.println("[DRONE " + drone.getDroneID() + "] Arrived at " + onRoute);

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
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot drop agent while in transit.");
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
        System.out.println("[DRONE " + drone.getDroneID() + "] Refilled to " + drone.getWaterLevel() + " liters.");

        // transition back to IdleState
        IdleState idleState = new IdleState();
        System.out.println("[DRONE " + drone.getDroneID() + "] Now idle and ready for dispatch.");
        drone.setState(idleState);
    }
}
