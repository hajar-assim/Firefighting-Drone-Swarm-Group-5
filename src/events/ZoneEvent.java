package events;

import java.awt.geom.Point2D;

public class ZoneEvent extends Event {
    private int zoneID;
    private Point2D center;

    public ZoneEvent(int zoneID, String startCoordinates, String endCoordinates) {
        super(null);
        this.zoneID = zoneID;
        this.center = this.getZoneCenter(startCoordinates, endCoordinates);
    }

    public int getZoneID() {
        return zoneID;
    }

    public Point2D getCenter() {
        return center;
    }

    private Point2D getZoneCenter(String startCoordinates, String endCoordinates) {
        Point2D start = this.parseCoordinates(startCoordinates);
        Point2D end = this.parseCoordinates(endCoordinates);

        double midX = (start.getX() + end.getX()) / 2;
        double midY = (start.getY() + end.getY()) / 2;

        return new Point2D.Double(midX, midY);
    }

    private Point2D parseCoordinates(String coordinates) {
        coordinates = coordinates.replace("(", "").replace(")", "");
        String[] parts = coordinates.split(";");

        double x = Double.parseDouble(parts[0].trim());
        double y = Double.parseDouble(parts[1].trim());

        return new Point2D.Double(x, y);
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public void fromString(String s) {

    }
}
