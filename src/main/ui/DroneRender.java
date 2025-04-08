package main.ui;

import java.awt.*;

/**
 * Represents the state of a drone.
 */
public class DroneRender {
    public final int worldX, worldY;
    public final DroneStateEnum state;

    public DroneRender(Point worldPos, DroneStateEnum state) {
        this.worldX = worldPos.x;
        this.worldY = worldPos.y;
        this.state = state;
    }
}
