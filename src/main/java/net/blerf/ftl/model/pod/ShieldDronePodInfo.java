package net.blerf.ftl.model.pod;

public class ShieldDronePodInfo extends ExtendedDronePodInfo {
    private int unknownAlpha = -1000;

    /**
     * Constructor.
     */
    public ShieldDronePodInfo() {
        super();
    }

    /**
     * Copy constructor.
     */
    protected ShieldDronePodInfo(ShieldDronePodInfo srcInfo) {
        super(srcInfo);
        unknownAlpha = srcInfo.getUnknownAlpha();
    }

    @Override
    public ShieldDronePodInfo copy() {
        return new ShieldDronePodInfo(this);
    }


    /**
     * Resets aspects of an existing object to be viable for player use.
     * <p>
     * This will be called by the ship object when it is commandeered.
     * <p>
     * Warning: Dangerous while values remain undeciphered.
     * TODO: Recurse into all nested objects.
     */
    @Override
    public void commandeer() {
        setUnknownAlpha(-1000);
    }


    /**
     * Unknown.
     * <p>
     * Zoltan shield recharge ticks?
     * <p>
     * Observed values: -1000 (inactive)
     */
    public void setUnknownAlpha(int n) {
        unknownAlpha = n;
    }

    public int getUnknownAlpha() {
        return unknownAlpha;
    }

    @Override
    public String toString() {
        return String.format("Alpha?:             %7d%n", unknownAlpha);
    }
}
