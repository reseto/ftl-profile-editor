package net.blerf.ftl.model.pod;

public class EmptyDronePodInfo extends ExtendedDronePodInfo {

    /**
     * Constructor.
     */
    public EmptyDronePodInfo() {
        super();
    }

    /**
     * Copy constructor.
     */
    protected EmptyDronePodInfo(EmptyDronePodInfo srcInfo) {
        super(srcInfo);
    }

    @Override
    public EmptyDronePodInfo copy() {
        return new EmptyDronePodInfo(this);
    }


    @Override
    public void commandeer() {
    }


    @Override
    public String toString() {
        return "N/A\n";
    }
}
