# Firefighting-Drone-Swarm-Group-5

## Overview
This project simulates a firefighting drone swarm system, which includes the following subsystems:

1. **Fire Incident Subsystem (Client)**: Reads input event files and sends them to the Scheduler.
2. **Drone Subsystem (Client)**: Consults the Scheduler to check for tasks and executes them.
3. **Scheduler (Server)**: Acts as a message pass-through between the Fire Incident and Drone subsystems.

***
# Iteration #1
For Iteration #1, the focus is on establishing basic communication between these components. Real scheduling logic will be introduced in later iterations.

## File Descriptions

### Data Files (`/data`)
- **fire_events_sample.csv**: Contains sample event data, such as fire detections and drone requests.
- **fire_zone_sample.csv**: Defines the zones where incidents can occur.

### Source Files (`/src`)
- **events/**
    - `EventType.java`: Enum defining event types like `FIRE_DETECTED`, `DRONE_REQUEST`, etc.
    - `IncidentEvent.java`: Class representing an incident event, with details like time, zone, and severity.
    - `Severity.java`: Enum defining severity levels (`LOW`, `MODERATE`, `HIGH`).

- **main/**
    - `EventQueueManager.java`: Manages the event queues between subsystems.
    - `Main.java`: Entry point for running the system.
    - `Scheduler.java`: Manages the passing of events between the Fire Incident and Drone subsystems.

- **subsystems/**
    - `DroneSubsystem.java`: Handles drone dispatch logic based on events received from the Scheduler.
    - `FireIncidentSubsystem.java`: Reads event files and sends events to the Scheduler.

### Test Files (`/src/test`)
- `DroneSubsystemTest.java`: Tests for the Drone Subsystem.
- `EventQueueManagerTest.java`: Tests for event queue management.
- `FireIncidentSubsystemTest.java`: Tests for reading and processing fire incident events.
- `IncidentEventTest.java`: Tests for the `IncidentEvent` class.
- `SchedulerTest.java`: Tests the Scheduler's ability to forward messages.

## Setup Instructions

### 1. Prerequisites
- **Java Development Kit (JDK)**: Version 11 or higher.
- **IntelliJ IDEA** (or any preferred IDE).
- **JUnit 5** for running tests.

### 2. Clone the Repository
```bash
git clone https://github.com/hajar-assim/Firefighting-Drone-Swarm-Group-5.git
cd Firefighting-Drone-Swarm-Group-5
```

### 3. Open in IntelliJ IDEA
1. Launch IntelliJ IDEA.
2. Click on **Open** and select the `Firefighting-Drone-Swarm-Group-5` directory.
3. Configure the Project SDK:
  - Navigate to `File > Project Structure > Project`.
  - Set the Project SDK to JDK 11 or higher.

### 4. Running the Program
1. Locate `Main.java` in the `src/main/` directory.
2. Right-click on `Main.java` and select **Run 'Main'**.

***
## Iteration #2

#### **Scheduler Enhancements**
- Implemented **task prioritization**: ongoing fires are handled before assigning new ones.
- Improved **fire tracking**: fires are only marked as extinguished once confirmed by the Fire Incident Subsystem.
- Optimized **logging**: improved clarity and specificness of logs.

#### **Drone Subsystem**
- Implemented **state machine logic**: drones now transition between predefined states.
- Improved **flight time calculations**: drones now track time spent traveling, dropping agent, and refilling.
- Introduced **arrival event notifications**: drones notify the scheduler upon reaching a zone.

#### **New Events Introduced**
- **`DroneArrivedEvent`**: Notifies the scheduler when a drone reaches a fire zone.
- **`DroneDispatchEvent`**: Sent when a drone is assigned to a fire.
- **`DroneUpdateEvent`**: Tracks changes in drone states (e.g., `IDLE → ON_ROUTE`).
- **`DropAgentEvent`**: Represents a drone performing a water drop.

### **Updated Source Files (`/src`)**
- **events/**
  - `DroneArrivedEvent.java`: Sent when a drone reaches its destination.
  - `DroneDispatchEvent.java`: Represents a drone being assigned to a fire.
  - `DroneUpdateEvent.java`: Captures drone state changes.
  - `DropAgentEvent.java`: Handles water drop actions.
  - `EventType.java`: Defines possible event types.
  - `IncidentEvent.java`: Represents fire incidents.
  - `Severity.java`: Enum defining fire severity levels.
  - `ZoneEvent.java`: Represents fire zones.

- **main/**
  - `EventQueueManager.java`: Handles message passing between subsystems.
  - `Main.java`: Entry point for running the system.
  - `Scheduler.java`: Manages event distribution and drone assignments.

- **subsystems/**
  - `DroneSubsystem.java`: Implements drone behavior, state transitions, and interactions with the scheduler.
  - `FireIncidentSubsystem.java`: Reads fire event data and interacts with the scheduler.
  - `DroneState.java`: Tracks drone properties (status, water level, location).
  - `DroneStatus.java`: Enum defining drone states (`IDLE`, `ON_ROUTE`, etc.).

***
## Iteration #3

### Overview
Iteration #3 introduces a significant change in the architecture of the system. The focus is on enabling **distributed execution**, where different components of the system (such as the Scheduler and the Drones) run on separate machines. Communication between these components is facilitated using **UDP** to handle the coordination and status updates in real-time.

The **Scheduler** now coordinates multiple drones and assigns them tasks to ensure a balanced workload. Each drone operates independently, but it reports its status to the Scheduler, allowing the system to optimize task allocation based on drone availability and fire zone severity.

### Key Features
- **Distributed Execution**: The system is split into separate programs that run on different machines, enabling the simultaneous operation of multiple drones.
- **Scheduler Coordination**: The Scheduler now handles multiple drones, ensuring that each drone services roughly the same number of zones, minimizing the waiting time for fires to be extinguished.
- **Drone Independence**: Each drone operates using its own state machine, but it shares its status (location, availability, etc.) with the Scheduler, which makes decisions about which drone should service which fire zone.

### New & Updated Source Files
- **main/**
  - `EventSocket.java`: Handles the UDP communication for message passing between the Scheduler and the Drones.
  - `Scheduler.java`: Enhanced to coordinate multiple drones, ensuring balanced workload and minimizing fire waiting times.

- **subsystems/drone/**
  - `DroneInfo.java`: A class that holds drone-specific data, such as ID, current location, and status.
  - `DroneSubsystem.java`: Manages drone operations, including state transitions and task execution.
  - **events/**: Contains the new event classes related to drone actions.
    - `DroneArrivedEvent.java`: Notifies when a drone arrives at a fire zone.
    - `DroneDispatchEvent.java`: Represents when a drone is assigned to a fire.
    - `DroneRegistrationEvent.java`: Event triggered when a new drone is registered in the system.
    - `DroneUpdateEvent.java`: Tracks changes in drone states (e.g., IDLE → ON_ROUTE).
    - `DropAgentEvent.java`: Represents a drone performing a water drop.
  - **states/**: Represents the different states in the drone's state machine.
    - `DroneState.java`: Base class for all drone states.
    - `DroppingAgentState.java`: Represents the state when a drone is dropping water.
    - `IdleState.java`: Represents the state when a drone is idle and waiting for a task.
    - `OnRouteState.java`: Represents the state when a drone is on its way to a fire zone.

- **subsystems/fire_incident/**
  - `FireIncidentSubsystem.java`: Manages fire incidents and communicates with the Scheduler to assign drones.
  - **events/**: Contains event-related classes for fire incidents.
    - `IncidentEvent.java`: Represents an incident event, including the details of the fire and its severity.
    - `Severity.java`: Enum for fire severity levels (e.g., LOW, MODERATE, HIGH).
    - `ZoneEvent.java`: Represents an event related to a fire zone, like zone status updates.

- **test/**
  - `DroneSubsystemTest.java`: Updated to test the behavior of drones in a distributed environment.
  - `EventTest.java`: Added tests for new event types like `DroneRegistrationEvent` and `DroneUpdateEvent`.
  - `FireIncidentSubsystemTest.java`: Tests for the fire incident subsystem's interaction with the distributed Scheduler.
  - `IncidentEventTest.java`: Tests for handling of incident events, including severity and zone updates.
  - `SchedulerTest.java`: Tests for the Scheduler's ability to manage multiple drones and allocate tasks efficiently.
  - `ZoneEventTest.java`: Tests for events related to fire zones, including updates on zone status.

***
## Iteration #4

### Overview
Iteration 4 introduces **fault detection and handling** capabilities in the system. Drones are now able to simulate and respond to both **hard faults** and **transient faults**. The Scheduler has been updated to detect these faults, respond appropriately, and reassign drones when necessary. Faults can now also be **injected via the input file**, allowing for controlled simulation and testing.

---

### Key Features Implemented

#### Fault Injection Support
- Extended the input CSV format to allow injection of faults per incident.
- Supported fault types include:
  - `DRONE_STUCK_IN_FLIGHT`
  - `NOZZLE_JAMMED`
  - `PACKET_LOSS`

#### Fault Handling Logic
- **Hard Faults** (e.g., `NOZZLE_JAMMED`) trigger permanent drone shutdown.
- **Transient Faults** (e.g., `PACKET_LOSS`) are handled with graceful retry logic — the drone remains operational after the fault is resolved.
- **Stuck in Flight** is detected using a timer — if a drone fails to send its arrival update in time, it is assumed to be stuck and marked faulty.

#### Packet Loss Simulation
- `PACKET_LOSS` fault causes intentional skipping of event transmissions (like `DroneArrivedEvent`), simulating unreliable networks.
- Scheduler responds by re-queuing the fire incident after a timeout or missed confirmation.

#### Incident Reassignment
- If a drone is faulted mid-task, the related fire incident is re-queued and reassigned to another available drone.

---

### Updated & New Files

- **events/**
  - `Faults.java`: Enum listing supported fault types.

- **fire_incident/**
  - `FireIncidentSubsystem.java`: Updated to parse and send fault info from the CSV input.

- **drone/**
  - `DroneSubsystem.java`: Updated to simulate fault behavior during mission execution.
  - `DroneInfo.java`: Can now be extended to track fault states and shutdown status.

- **main/**
  - `Scheduler.java`: Updated to detect faults, handle reassignment, and manage drone shutdowns gracefully.

---

### Diagrams
- Added **timing diagrams** for:
  - Nozzle jam fault handling
  - Drone stuck-in-flight scenario
- Diagrams illustrate how the system transitions between states and reassigns tasks in response to faults.

***
## Iteration #5
***

### Contributors ~ Group A4-5
- Colin Chen - 101229162
- Hajar Assim - 101232456
- Hasib Khodayar - 101225523
- Ayman Kamran - 101232406
- Mohammed Al Rajab - 101222347
- Salama Noureldean - 101154365
