#!/bin/bash

CLASS_PATH="/Users/hajarassim/Desktop/school/third_year/winter/sysc_3303/projects/Firefighting-Drone-Swarm-Group-5/out/production/Firefighting-Drone-Swarm-Group-5"

echo "Starting Scheduler..."
osascript <<EOF
tell application "iTerm"
    create window with default profile
    tell current session of current window
        write text "java -cp '$CLASS_PATH' main.Scheduler"
    end tell
end tell
EOF

sleep 1

echo "Starting FireIncidentSubsystem..."
osascript <<EOF
tell application "iTerm"
    tell current window
        create tab with default profile
        tell current session
            write text "java -cp '$CLASS_PATH' subsystems.fire_incident.FireIncidentSubsystem"
        end tell
    end tell
end tell
EOF

sleep 1

echo "Starting DroneSubsystem..."
osascript <<EOF
tell application "iTerm"
    tell current window
        create tab with default profile
        tell current session
            write text "java -cp '$CLASS_PATH' subsystems.drone.DroneSubsystem"
        end tell
    end tell
end tell
EOF