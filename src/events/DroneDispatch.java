package events;

import java.awt.geom.Point2D;

public class DroneDispatch extends Event{
    private int zoneID;
    private Point2D coords;

    public DroneDispatch(int zoneID, Point2D coords){
        super(null);
        this.zoneID = zoneID;
        this.coords = coords;
    }

    public int getZoneID() {
        return zoneID;
    }

    public Point2D getCoords() {
        return coords;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public void fromString() {

    }
}
