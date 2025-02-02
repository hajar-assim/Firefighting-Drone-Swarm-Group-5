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

### Prerequisites
- **Java Development Kit (JDK)**: Version 11 or higher.
- **IntelliJ IDEA** (or any preferred IDE).
- **JUnit 5** for running tests.

### Clone the Repository
```bash
git clone https://github.com/hajar-assim/Firefighting-Drone-Swarm-Group-5.git
cd Firefighting-Drone-Swarm-Group-5
```

### Open in IntelliJ IDEA
1. Launch IntelliJ IDEA.
2. Click on **Open** and select the `Firefighting-Drone-Swarm-Group-5` directory.
3. Configure the Project SDK:
  - Navigate to `File > Project Structure > Project`.
  - Set the Project SDK to JDK 11 or higher.

## Running the Program
1. Locate `Main.java` in the `src/main/` directory.
2. Right-click on `Main.java` and select **Run 'Main'**.

***
## Iteration #2
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
