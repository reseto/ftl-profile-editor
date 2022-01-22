package net.blerf.ftl.model.state;


public enum StoreItemType {
    WEAPON("Weapon"), DRONE("Drone"), AUGMENT("Augment"), CREW("Crew"), SYSTEM("System");

    private final String title;

    StoreItemType(String title) {
        this.title = title;
    }

    public static StoreItemType fromInt(int i) {
        if (i < 0 || i > StoreItemType.values().length) {
            return WEAPON;
        }
        return StoreItemType.values()[i];
    }

    public int toInt() {
        return ordinal();
    }

    public String toString() {
        return title;
    }
}
