package subsystems.drone.states;

import main.Scheduler;
import subsystems.Event;
import subsystems.drone.events.DroneDispatchEvent;
import subsystems.drone.events.DropAgentEvent;
import subsystems.drone.DroneSubsystem;

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
        drone.getSendEventManager().put(new DropAgentEvent(event.getVolume(), drone.getDroneID()));

        // transition to RefillingState
        System.out.println("[DRONE " + drone.getDroneID() + "] Returning to base to refill.");
        RefillingState refillingState = new RefillingState();
        drone.setState(refillingState);
        refillingState.refill(drone);
    }

    @Override
    public void refill(DroneSubsystem drone) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot refill, currently dropping agent.");
    }
}
