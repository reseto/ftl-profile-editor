package net.blerf.ftl.model.pod;

import net.blerf.ftl.parser.SavedGameParser;

/**
 * Extended Combat/Beam/Ship_Repair drone info.
 * <p>
 * These drones flit to a random (?) point, stop, then move to another,
 * and so on.
 */
public class ZigZagDronePodInfo extends ExtendedDronePodInfo {
    private int lastWaypointX = 0;
    private int lastWaypointY = 0;
    private int transitTicks = 0;
    private int exhaustAngle = 0;
    private int unknownEpsilon = 0;

    /**
     * Constructor.
     */
    public ZigZagDronePodInfo() {
        super();
    }

    /**
     * Copy constructor.
     */
    protected ZigZagDronePodInfo(ZigZagDronePodInfo srcInfo) {
        super(srcInfo);
        lastWaypointX = srcInfo.getLastWaypointX();
        lastWaypointY = srcInfo.getLastWaypointY();
        transitTicks = srcInfo.getTransitTicks();
        exhaustAngle = srcInfo.getExhaustAngle();
        unknownEpsilon = srcInfo.getUnknownEpsilon();
    }

    @Override
    public ZigZagDronePodInfo copy() {
        return new ZigZagDronePodInfo(this);
    }


    /**
     * Resets aspects of an existing object to be viable for player use.
     * <p>
     * This will be called by the ship object when it is commandeered.
     * <p>
     * Warning: Dangerous while values remain undeciphered.
     */
    @Override
    public void commandeer() {
    }


    /**
     * Sets the cached position from when the drone last stopped.
     * <p>
     * TODO: Modify this value in the editor. In CheatEngine, changing
     * this has no effect, appearing to be read-only field for reference.
     *
     * @param n a pseudo-float
     */
    public void setLastWaypointX(int n) {
        lastWaypointX = n;
    }

    public void setLastWaypointY(int n) {
        lastWaypointY = n;
    }

    public int getLastWaypointX() {
        return lastWaypointX;
    }

    public int getLastWaypointY() {
        return lastWaypointY;
    }

    /**
     * Sets time elapsed while this drone moves.
     * <p>
     * This increments from 0 to 1000 as the drone drifts toward a new
     * waypoint. While this value is below 200, exhaust flames are
     * visible. Then they vanish. The moment the drone pauses at the
     * destination, this is set to 1000.
     * <p>
     * When not set, this is MIN_INT. This happens when stationary while
     * stunned.
     * <p>
     * Observed values: 153 (stunned drift begins), 153000 (mid drift),
     * 153000000 (near end of drift).
     * <p>
     * TODO: Modify this value in the editor. In CheatEngine, changing
     * this has no effect, appearing to be read-only field for reference.
     *
     * @see #setExhaustAngle(int)
     */
    public void setTransitTicks(int n) {
        transitTicks = n;
    }

    public int getTransitTicks() {
        return transitTicks;
    }

    /**
     * Sets the angle to display exhaust flames thrusting toward.
     * <p>
     * When not set, this is MIN_INT.
     * <p>
     * TODO: Modify this value in the editor. In CheatEngine, changing
     * this DOES work.
     *
     * @param n a pseudo-float (n degrees clockwise from east)
     * @see #setTransitTicks(int)
     */
    public void setExhaustAngle(int n) {
        exhaustAngle = n;
    }

    public int getExhaustAngle() {
        return exhaustAngle;
    }

    /**
     * Unknown.
     * <p>
     * When not set, this is MIN_INT.
     */
    public void setUnknownEpsilon(int n) {
        unknownEpsilon = n;
    }

    public int getUnknownEpsilon() {
        return unknownEpsilon;
    }

    @Override
    public String toString() {
        return String.format("Last Waypoint:      %7d,%7d%n", lastWaypointX, lastWaypointY) +
                String.format("TransitTicks:       %7s%n", (transitTicks == Integer.MIN_VALUE ? "N/A" : transitTicks)) +
                String.format("Exhaust Angle:      %7s%n", (exhaustAngle == Integer.MIN_VALUE ? "N/A" : exhaustAngle)) +
                String.format("Epsilon?:           %7s%n", (unknownEpsilon == Integer.MIN_VALUE ? "N/A" : unknownEpsilon));
    }
}
