# Firefighting-Drone-Swarm-Group-5

## Overview
This project simulates a firefighting drone swarm system, which includes the following subsystems:

1. **Fire Incident Subsystem (Client)**: Reads input event files and sends them to the Scheduler.
2. **Drone Subsystem (Client)**: Consults the Scheduler to check for tasks and executes them.
3. **Scheduler (Server)**: Acts as a message pass-through between the Fire Incident and Drone subsystems.

***
# Iteration #1
For Iteration #1, the focus is on establishing basic communication between these components. Real scheduling logic will be introduced in later iterations.

### Project Structure
```
Firefighting-Drone-Swarm-Group-5/
├── data/
│   ├── fire_events_sample.csv
│   └── fire_zone_sample.csv
├── out/
├── src/
    ├── diagrams/
    │   ├── DRONE_STATE_MACHINE.puml
    │   └── SCHEDULER_STATE_MACHINE.puml
    ├── events/
    │   ├── EventType.java
    │   ├── IncidentEvent.java
    │   └── Severity.java
    ├── main/
    │   ├── EventQueueManager.java
    │   ├── Main.java
    │   └── Scheduler.java
    ├── subsystems/
    │   ├── DroneSubsystem.java
    │   └── FireIncidentSubsystem.java
    └── test/
        ├── DroneSubsystemTest.java
        ├── EventQueueManagerTest.java
        ├── FireIncidentSubsystemTest.java
        ├── IncidentEventTest.java
        └── SchedulerTest.java
├── .gitignore
├── Firefighting-Drone-Swarm-Group-5.iml
└── README.md
```

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
***
## Iteration #4
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
