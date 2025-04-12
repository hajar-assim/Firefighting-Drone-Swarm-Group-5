package main.ui;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Represents the state of a drone.
 */
public class DroneRender {
    public final int worldX, worldY;
    public final DroneStateEnum state;

    public DroneRender(Point2D worldPos, DroneStateEnum state) {
        this.worldX = (int) worldPos.getX();
        this.worldY = (int) worldPos.getY();
        this.state = state;
    }
}
