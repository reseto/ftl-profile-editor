package net.blerf.ftl.model.state;

public class AsteroidFieldState {
    private int unknownAlpha = -1000;
    private int strayRockTicks = 0;
    private int unknownGamma = 0;
    private int bgDriftTicks = 0;
    private int currentTarget = 0;


    public AsteroidFieldState() {
    }

    /**
     * Unknown.
     * <p>
     * Observed values: 3, 0; 4.
     */
    public void setUnknownAlpha(int n) {
        unknownAlpha = n;
    }

    public int getUnknownAlpha() {
        return unknownAlpha;
    }

    /**
     * Unknown.
     * <p>
     * Observed values: 15853, 15195, 14786, 12873, 12741, 12931. It's been
     * seen at 6545 immediately after reaching 0 (random starting value?).
     */
    public void setStrayRockTicks(int n) {
        strayRockTicks = n;
    }

    public int getStrayRockTicks() {
        return strayRockTicks;
    }

    /**
     * Unknown.
     * <p>
     * Observed values: 0, 1, 2, 0, 1.
     */
    public void setUnknownGamma(int n) {
        unknownGamma = n;
    }

    public int getUnknownGamma() {
        return unknownGamma;
    }

    /**
     * Sets time elapsed while the background shifts left.
     * <p>
     * Observed values: 1952, 1294, 885, 817, 685, 335. It's been seen
     * stuck at 143 until strayRockTicks hit 0, then became 1102!? Then
     * seen decrementing to 0, then became 1399.
     */
    public void setBgDriftTicks(int n) {
        bgDriftTicks = n;
    }

    public int getBgDriftTicks() {
        return bgDriftTicks;
    }

    /**
     * Unknown.
     * <p>
     * This seems to be an incrementing counter.
     * <p>
     * Observed values: 1, 8, 13.
     */
    public void setCurrentTarget(int n) {
        currentTarget = n;
    }

    public int getCurrentTarget() {
        return currentTarget;
    }

    @Override
    public String toString() {
        return String.format("Alpha?:            %7d%n", unknownAlpha) +
                String.format("Stray Rock Ticks?: %7d%n", strayRockTicks) +
                String.format("Gamma?:            %7d%n", unknownGamma) +
                String.format("Bkg Drift Ticks?:  %7d%n", bgDriftTicks) +
                String.format("Current Target?:   %7d%n", currentTarget);
    }
}
