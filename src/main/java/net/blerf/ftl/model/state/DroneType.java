package net.blerf.ftl.model.state;

/**
 * Types of drones.
 * <p>
 * FTL 1.5.4 introduced HACKING, BEAM, and SHIELD.
 */
public enum DroneType {
    // TODO: Magic numbers.
    BATTLE("BATTLE", 150),
    REPAIR("REPAIR", 25),
    BOARDER("BOARDER", 1),
    HACKING("HACKING", 1),
    COMBAT("COMBAT", 1),
    BEAM("BEAM", 1),
    DEFENSE("DEFENSE", 1),
    SHIELD("SHIELD", 1),
    SHIP_REPAIR("SHIP_REPAIR", 1);

    private final String id;
    private final int maxHealth;

    DroneType(String id, int maxHealth) {
        this.id = id;
        this.maxHealth = maxHealth;
    }

    public String getId() {
        return id;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public String toString() {
        return id;
    }

    public static DroneType findById(String id) {
        for (DroneType d : values()) {
            if (d.getId().equals(id)) return d;
        }
        return null;
    }
}
