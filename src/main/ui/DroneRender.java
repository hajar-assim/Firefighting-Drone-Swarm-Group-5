package main.ui;

import java.awt.*;

/**
 * Represents the state of a drone.
 */
public class DroneRender {
    Point gridPos;
    DroneStateEnum state;

    public DroneRender(Point pos, DroneStateEnum state) {
        this.gridPos = pos;
        this.state = state;
    }
}
