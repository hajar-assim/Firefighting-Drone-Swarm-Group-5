package subsystems;

import events.DroneDispatchEvent;
import events.DropAgentEvent;
import events.Event;
import main.EventQueueManager;

public interface DroneStates {
    void handleEvent(DroneSubsystem drone, Event event, EventQueueManager sendEvent);
    void dispatch(DroneSubsystem drone, DroneDispatchEvent event);
    void travel(DroneSubsystem drone, EventQueueManager sendEvent);
    void dropAgent(DroneSubsystem drone, DropAgentEvent event, EventQueueManager sendManager);
    void refill(DroneSubsystem drone);
}
