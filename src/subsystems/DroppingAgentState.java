package subsystems;

import events.DroneDispatchEvent;
import events.DropAgentEvent;
import events.Event;
import main.EventQueueManager;

public class DroppingAgentState implements DroneStates {
    @Override
    public void handleEvent(DroneSubsystem drone, Event event, EventQueueManager sendEvent) {
        if (event instanceof DroneDispatchEvent){
            dispatch(drone, (DroneDispatchEvent) event);
        } else if (event instanceof DropAgentEvent) {
            dropAgent(drone, (DropAgentEvent) event, sendEvent);
        }
    }

    @Override
    public void dispatch(DroneSubsystem drone, events.DroneDispatchEvent event) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot dispatch while dropping agent.");
    }

    @Override
    public void travel(DroneSubsystem drone, EventQueueManager sendEvent) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot travel while dropping agent.");
    }

    @Override
    public void dropAgent(DroneSubsystem drone, DropAgentEvent event, EventQueueManager sendEvent) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Dropping agent...");
        try {
            Thread.sleep(event.getVolume() * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("[DRONE " + drone.getDroneID() + "] Dropped " + event.getVolume() + " liters.");
        sendEvent.put(new DropAgentEvent(drone.getDroneID(), event.getVolume()));
        drone.setState(new RefillingState());
    }

    @Override
    public void refill(DroneSubsystem drone) {
        System.out.println("[DRONE " + drone.getDroneID() + "] Cannot refill, currently dropping agent.");
    }
}
