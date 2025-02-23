package events;

import java.awt.geom.Point2D;

/**
 * Represents an event related to a specific zone, calculating the zone's center
 * based on start and end coordinates.
 */
public class ZoneEvent extends Event {
    private int zoneID;
    private Point2D center;

    /**
     * Constructs a ZoneEvent with the specified zone ID, start coordinates, and end coordinates.
     * The center of the zone is computed as the midpoint between the start and end coordinates.
     *
     * @param zoneID          the ID of the zone
     * @param startCoordinates the start coordinates in the format "(x;y)"
     * @param endCoordinates   the end coordinates in the format "(x;y)"
     */
    public ZoneEvent(int zoneID, String startCoordinates, String endCoordinates) {
        super(null);
        this.zoneID = zoneID;
        this.center = this.getZoneCenter(startCoordinates, endCoordinates);
    }


    /**
     * Gets the ID of the zone.
     *
     * @return the zone ID
     */
    public int getZoneID() {
        return zoneID;
    }


    /**
     * Gets the center point of the zone.
     *
     * @return the center coordinates as a {@code Point2D} object
     */
    public Point2D getCenter() {
        return center;
    }


    /**
     * Calculates the center of the zone based on the start and end coordinates.
     *
     * @param startCoordinates the start coordinates in the format "(x;y)"
     * @param endCoordinates   the end coordinates in the format "(x;y)"
     * @return the calculated center as a {@code Point2D} object
     */
    private Point2D getZoneCenter(String startCoordinates, String endCoordinates) {
        Point2D start = this.parseCoordinates(startCoordinates);
        Point2D end = this.parseCoordinates(endCoordinates);

        double midX = (start.getX() + end.getX()) / 2;
        double midY = (start.getY() + end.getY()) / 2;

        return new Point2D.Double(midX, midY);
    }


    /**
     * Parses a coordinate string in the format "(x;y)" into a {@code Point2D} object.
     *
     * @param coordinates the coordinate string
     * @return the parsed coordinates as a {@code Point2D} object
     * @throws NumberFormatException if the string format is invalid
     */
    private Point2D parseCoordinates(String coordinates) {
        // Remove parentheses if present
        coordinates = coordinates.replace("(", "").replace(")", "");
        String[] parts = coordinates.split(",");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid coordinates: " + coordinates);
        }
        double x = Double.parseDouble(parts[0].trim());
        double y = Double.parseDouble(parts[1].trim());
        return new Point2D.Double(x, y);
    }


    /**
     * Returns a string representation of the event.
     *
     * @return a formatted string describing the event
     */
    @Override
    public String toString() {
        return String.format("ZoneEvent[zoneID=%d, center=(%.2f, %.2f)]",
                zoneID, center.getX(), center.getY());
    }


    /**
     * Parses a string representation of the event and updates the object's state.
     *
     * @param s the string to parse (format not yet defined)
     */
    @Override
    public void fromString(String s) {
        // Implementation needed based on the expected format
    }
}
