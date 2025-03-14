package subsystems.drone.states;

import main.Scheduler;
import subsystems.drone.events.DroneArrivedEvent;
import subsystems.drone.events.DroneDispatchEvent;
import subsystems.drone.events.DroneUpdateEvent;
import subsystems.drone.events.DropAgentEvent;
import subsystems.Event;
import subsystems.drone.DroneSubsystem;

import java.awt.geom.Point2D;

public class OnRouteState implements DroneState {
    private DroneDispatchEvent dispatchEvent;

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

    public OnRouteState(DroneDispatchEvent dispatchEvent) {
        this.dispatchEvent = dispatchEvent;
    }

    @Override
    public void dispatch(DroneSubsystem drone, DroneDispatchEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Already in transit, cannot dispatch.");
    }

    @Override
    public void travel(DroneSubsystem drone) {
        Point2D targetCoords = dispatchEvent.getCoords();
        double flightTime = drone.timeToZone(drone.getCoordinates(), targetCoords);

        boolean returningToBase = dispatchEvent.getZoneID() == 0;
        String onRoute = returningToBase ? "Base" : "Zone: " + drone.getZoneID();

        System.out.println("[DRONE " + drone.getDroneID() + "] On route to " + onRoute
                + " | Estimated time: " + String.format("%.2f seconds", flightTime));

        // simulate flight time
        try {
            Thread.sleep((long) flightTime * Scheduler.sleepMultiplier);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        drone.setCoordinates(targetCoords);
        System.out.println("[DRONE " + drone.getDroneID() + "] Arrived at " + onRoute);

        if (! returningToBase) {
            // send arrivedEvent to the Schedule to receive further instructions
            DroneArrivedEvent arrivedEvent = new DroneArrivedEvent(drone.getDroneID(), dispatchEvent.getZoneID());
            drone.getSendSocket().send(arrivedEvent, drone.getSchedulerAddress(), drone.getSchedulerPort());
        }else{
            refill(drone);
        }
    }

    @Override
    public void dropAgent(DroneSubsystem drone, DropAgentEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot drop agent while in transit.");
    }


    private void refill(DroneSubsystem drone){
        // reset water level and flight time
        drone.setWaterLevel(drone.getWaterLevel());
        drone.setFlightTime(10 * 60);
        System.out.println("[DRONE " + drone.getDroneID() + "] Refilled to " + drone.getWaterLevel() + " liters.");

        // transition back to IdleState
        IdleState idleState = new IdleState();
        System.out.println("[DRONE " + drone.getDroneID() + "] Now idle and ready for dispatch.");
        drone.setState(idleState);

        // notify the scheduler that the drone is ready
        DroneUpdateEvent droneUpdateEvent = new DroneUpdateEvent(drone.getDroneID(), idleState);
        drone.getSendSocket().send(droneUpdateEvent, drone.getSchedulerAddress(), drone.getSchedulerPort());
    }
}
