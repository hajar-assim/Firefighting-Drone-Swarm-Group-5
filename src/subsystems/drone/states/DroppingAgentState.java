package subsystems.drone.states;

import main.Scheduler;
import subsystems.Event;
import subsystems.drone.events.DroneDispatchEvent;
import subsystems.drone.events.DropAgentEvent;
import subsystems.drone.DroneSubsystem;

import java.awt.geom.Point2D;

public class DroppingAgentState implements DroneState {

    @Override
    public void handleEvent(DroneSubsystem drone, Event event) {
        if (event instanceof DropAgentEvent) {
            dropAgent(drone, (DropAgentEvent) event);
        }
    }

    @Override
    public void dispatch(DroneSubsystem drone, DroneDispatchEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot dispatch while dropping agent.");
    }

    @Override
    public void travel(DroneSubsystem drone) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot travel while dropping agent.");
    }

    @Override
    public void dropAgent(DroneSubsystem drone, DropAgentEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Dropping agent...");
        try {
            Thread.sleep((long) event.getVolume() * Scheduler.sleepMultiplier);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("[DRONE " + drone.getDroneID() + "] Dropped " + event.getVolume() + " liters.");

        // notify system that agent was dropped
        drone.getSendSocket().send(new DropAgentEvent(event.getVolume(), drone.getDroneID()), drone.getSchedulerAddress(), drone.getSchedulerPort());

        // transition to On route and Refill
        System.out.println("[DRONE " + drone.getDroneID() + "] Returning to base to refill.");

        OnRouteState toBase = new OnRouteState(new DroneDispatchEvent(0, new Point2D.Double(0,0)));
        drone.setZoneID(0);
        drone.setState(toBase);
        drone.getState().travel(drone);
    }
}
