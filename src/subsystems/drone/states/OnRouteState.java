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
            EventLogger.info(drone.getDroneID(), "Redirecting to a new target zone.", false);
            dispatch(drone, (DroneDispatchEvent) event);
        } else if (event instanceof DropAgentEvent dropAgentEvent) {
            EventLogger.info(drone.getDroneID(), "Received order to drop " + dropAgentEvent.getVolume() + "L of water.", false);
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
        OnRouteState onRoute = new OnRouteState(event);
        drone.setState(onRoute);
        drone.getState().travel(drone);
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
        Point2D start = drone.getCoordinates();
        Point2D targetCoords = dispatchEvent.getCoords();
        double flightTime = DroneSubsystem.timeToZone(start, targetCoords);

        boolean returningToBase = dispatchEvent.getZoneID() == 0;
        String onRoute = returningToBase ? "Base" : "Zone: " + drone.getZoneID();

        EventLogger.info(drone.getDroneID(), String.format("On route to " + onRoute
                + " | Estimated time: " + String.format("%.2f seconds", flightTime)), false);

        // simulate animated flight
        int steps = 20;
        long stepDuration = (long) ((flightTime * Scheduler.sleepMultiplier) / steps);

        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            double x = start.getX() + (targetCoords.getX() - start.getX()) * t;
            double y = start.getY() + (targetCoords.getY() - start.getY()) * t;

            drone.setCoordinates(new Point2D.Double(x, y));

            // notify scheduler (GUI)
            DroneUpdateEvent updateEvent = new DroneUpdateEvent(drone.getDroneInfo());
            drone.getSocket().send(updateEvent, drone.getSchedulerAddress(), drone.getSchedulerPort());

            try {
                Thread.sleep(stepDuration);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Inject simulated fault mid-flight
            if (dispatchEvent.getFault() == Faults.DRONE_STUCK_IN_FLIGHT && i == steps / 2) {
                EventLogger.warn(drone.getDroneID(), "Simulating " + dispatchEvent.getFault() + " fault mid-flight. Not sending arrival event.");
                drone.setState(new FaultedState(dispatchEvent.getFault()));
                drone.setZoneID(0);
                return;
            }
        }

        // Handle nozzle jam before arrival
        if (dispatchEvent.getFault() == Faults.NOZZLE_JAMMED) {
            drone.getDroneInfo().setNozzleJam(true);
        }

        // final snap to exact coords (just in case)
        drone.setCoordinates(targetCoords);
        DroneArrivedEvent arrivedEvent = new DroneArrivedEvent(drone.getDroneID(), drone.getZoneID());
        drone.getSocket().send(arrivedEvent, drone.getSchedulerAddress(), drone.getSchedulerPort());
        EventLogger.info(drone.getDroneID(), "Arrived at " + onRoute, false);

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
        EventLogger.info(drone.getDroneID(), "Refilled to " + drone.getWaterLevel() + " liters.", false);

        // transition back to IdleState
        IdleState idleState = new IdleState();
        drone.setState(idleState);
        EventLogger.info(drone.getDroneID(), "Now idle and ready for dispatch.\n", true);
    }

}
