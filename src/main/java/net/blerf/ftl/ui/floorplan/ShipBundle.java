package net.blerf.ftl.ui.floorplan;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.blerf.ftl.constants.FTLConstants;
import net.blerf.ftl.model.shiplayout.RoomAndSquare;
import net.blerf.ftl.model.shiplayout.ShipLayout;
import net.blerf.ftl.model.state.CrewState;
import net.blerf.ftl.model.state.DoorState;
import net.blerf.ftl.model.state.DroneState;
import net.blerf.ftl.model.state.RoomState;
import net.blerf.ftl.model.state.SystemState;
import net.blerf.ftl.model.state.WeaponState;
import net.blerf.ftl.model.systeminfo.BatteryInfo;
import net.blerf.ftl.model.systeminfo.ExtendedSystemInfo;
import net.blerf.ftl.model.type.CrewType;
import net.blerf.ftl.model.type.SystemType;
import net.blerf.ftl.ui.SpriteReference;
import net.blerf.ftl.xml.ship.ShipBlueprint;
import net.blerf.ftl.xml.ship.ShipChassis;


/**
 * A container to organize ship variables.
 * <p>
 * Had this been a regular parent component, its bounds would limit the visible
 * area of descendants.
 */
@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class ShipBundle {

    private FTLConstants ftlConstants = null;

    private ShipBlueprint shipBlueprint = null;
    private ShipLayout shipLayout = null;
    private ShipChassis shipChassis = null;
    private String shipGraphicsBaseName = null;

    private int reservePowerCapacity = 0;
    private String shipName = null;

    private int hullAmt = 0;
    private int fuelAmt = 0;
    private int dronePartsAmt = 0;
    private int missilesAmt = 0;
    private int scrapAmt = 0;
    private boolean hostile = false;
    private int jumpChargeTicks = 0;
    private boolean jumping = false;
    private int jumpAnimTicks = 0;
    private int cloakAnimTicks = 0;
    private boolean playerControlled = false;
    private List<String> augmentIdList = new ArrayList<>();

    private int originX = 0;
    private int originY = 0;
    private int layoutX = 0;
    private int layoutY = 0;
    private Map<Rectangle, Integer> roomRegionRoomIdMap = new HashMap<>();
    private Map<Rectangle, FloorplanCoord> squareRegionCoordMap = new HashMap<>();
    private List<RoomAndSquare> blockedRasList = new ArrayList<>(1);

    private List<SpriteReference<DroneState>> droneRefs = new ArrayList<>();
    private List<SpriteReference<WeaponState>> weaponRefs = new ArrayList<>();
    private List<SpriteReference<RoomState>> roomRefs = new ArrayList<>();
    private List<SpriteReference<SystemState>> systemRefs = new ArrayList<>();
    private List<SpriteReference<DoorState>> doorRefs = new ArrayList<>();
    private List<SpriteReference<CrewState>> crewRefs = new ArrayList<>();

    private List<DroneBoxSprite> droneBoxSprites = new ArrayList<>();
    private List<DroneBodySprite> droneBodySprites = new ArrayList<>();
    private List<WeaponSprite> weaponSprites = new ArrayList<>();
    private List<RoomSprite> roomSprites = new ArrayList<>();
    private List<SystemRoomSprite> systemRoomSprites = new ArrayList<>();
    private List<BreachSprite> breachSprites = new ArrayList<>();
    private List<FireSprite> fireSprites = new ArrayList<>();
    private List<DoorSprite> doorSprites = new ArrayList<>();
    private List<CrewSprite> crewSprites = new ArrayList<>();

    private List<ExtendedSystemInfo> extendedSystemInfoList = new ArrayList<>();

    private JLabel baseLbl = null;
    private JLabel floorLbl = null;
    private ShipInteriorComponent interiorComp = null;


    /**
     * Returns the first extended system info of a given class, or null.
     */
    public <T extends ExtendedSystemInfo> T getExtendedSystemInfo(Class<T> infoClass) {
        T result = null;
        for (ExtendedSystemInfo info : extendedSystemInfoList) {
            if (infoClass.isInstance(info)) {
                result = infoClass.cast(info);
                break;
            }
        }
        return result;
    }

    /**
     * Returns the first system reference, or null.
     */
    public SpriteReference<SystemState> getSystemRef(SystemType systemType) {
        SpriteReference<SystemState> result = null;

        for (SpriteReference<SystemState> systemRef : systemRefs) {
            if (systemType.equals(systemRef.get().getSystemType())) {
                result = systemRef;
                break;
            }
        }

        return result;
    }

    /**
     * Returns the roomId which contains the center of a given sprite, or -1.
     */
    public int getSpriteRoomId(Component c) {
        int result = -1;
        int centerX = c.getBounds().x + c.getBounds().width / 2;
        int centerY = c.getBounds().y + c.getBounds().height / 2;

        for (Map.Entry<Rectangle, Integer> regionEntry : roomRegionRoomIdMap.entrySet()) {
            if (regionEntry.getKey().contains(centerX, centerY)) {
                result = regionEntry.getValue().intValue();
            }
        }

        return result;
    }

    /**
     * Returns the number of friendly Zoltan crew sprites in a room.
     */
    public int getRoomZoltanEnergy(int roomId) {
        if (roomId < 0) return 0;

        int result = 0;
        Rectangle roomRect = null;

        for (Map.Entry<Rectangle, Integer> regionEntry : roomRegionRoomIdMap.entrySet()) {
            if (roomId == regionEntry.getValue()) {
                roomRect = regionEntry.getKey();
                break;
            }
        }

        if (roomRect != null) {
            for (SpriteReference<CrewState> crewRef : crewRefs) {
                if (CrewType.ENERGY.equals(crewRef.get().getRace())) {
                    if (crewRef.get().isPlayerControlled() == this.playerControlled) {
                        CrewSprite crewSprite = crewRef.getSprite(CrewSprite.class);
                        int centerX = crewSprite.getX() + crewSprite.getWidth() / 2;
                        int centerY = crewSprite.getY() + crewSprite.getHeight() / 2;
                        if (roomRect.contains(centerX, centerY)) {
                            result++;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns available reserve power after limits are applied and systems'
     * demand is subtracted (min 0).
     * <p>
     * Note: Plasma storms are not considered, due to limitations in the saved
     * game format. That info is buried in events and obscured by the random
     * sector layout seed.
     * <p>
     * Overallocation will cause FTL to depower systems in-game.
     *
     * @param excludeRef count demand from all systems except one (may be null)
     */
    public int getReservePool(SpriteReference<SystemState> excludeRef) {
        int result = reservePowerCapacity;

        int systemsPower = 0;
        for (SpriteReference<SystemState> systemRef : systemRefs) {
            if (SystemType.BATTERY.equals(systemRef.get().getSystemType())) {
                // TODO: Check if Battery system is currently being hack-disrupted,
                // then subtract 2.
            }

            if (systemRef == excludeRef) continue;

            if (!systemRef.get().getSystemType().isSubsystem()) {
                systemsPower += systemRef.get().getPower();
            }
        }
        result -= systemsPower;
        result = Math.max(0, result);

        return result;
    }

    /**
     * Returns the total battery power produced by the Battery system.
     */
    public int getBatteryPoolCapacity() {
        int batteryPoolCapacity = 0;

        BatteryInfo batteryInfo = getExtendedSystemInfo(BatteryInfo.class);
        if (batteryInfo != null && batteryInfo.isActive()) {
            SpriteReference<SystemState> batterySystemRef = getSystemRef(SystemType.BATTERY);
            // This should not be null.
            batteryPoolCapacity = ftlConstants.getBatteryPoolCapacity(batterySystemRef.get().getCapacity());
        }

        return batteryPoolCapacity;
    }

    /**
     * Returns available battery power after systems' demand is subtracted
     * (min 0).
     *
     * @param excludeRef count demand from all systems except one (may be null)
     */
    public int getBatteryPool(SpriteReference<SystemState> excludeRef) {
        int result = 0;

        BatteryInfo batteryInfo = getExtendedSystemInfo(BatteryInfo.class);
        if (batteryInfo != null && batteryInfo.isActive()) {
            int batteryPoolCapacity = 0;
            int systemsBattery = 0;
            for (SpriteReference<SystemState> systemRef : systemRefs) {
                if (SystemType.BATTERY.equals(systemRef.get().getSystemType())) {
                    batteryPoolCapacity = ftlConstants.getBatteryPoolCapacity(systemRef.get().getCapacity());
                }

                if (systemRef == excludeRef) continue;

                if (!systemRef.get().getSystemType().isSubsystem()) {
                    systemsBattery += systemRef.get().getBatteryPower();
                }
            }
            result = batteryPoolCapacity - systemsBattery;
            result = Math.max(0, result);
        }

        return result;
    }

    public void updateBatteryPool() {
        SpriteReference<SystemState> batterySystemRef = null;
        int systemsBattery = 0;
        for (SpriteReference<SystemState> systemRef : systemRefs) {
            if (SystemType.BATTERY.equals(systemRef.get().getSystemType())) {
                batterySystemRef = systemRef;
            }

            if (!systemRef.get().getSystemType().isSubsystem()) {
                systemsBattery += systemRef.get().getBatteryPower();
            }
        }

        BatteryInfo batteryInfo = getExtendedSystemInfo(BatteryInfo.class);
        if (batterySystemRef != null && batterySystemRef.get().getCapacity() > 0) {
            if (batteryInfo == null) {
                batteryInfo = new BatteryInfo();
                extendedSystemInfoList.add(batteryInfo);
            }
            if (!batteryInfo.isActive()) {
                batteryInfo.setActive(true);
                batteryInfo.setDischargeTicks(0);
            }
            batteryInfo.setUsedBattery(systemsBattery);
        } else {
            if (batteryInfo != null) {
                batteryInfo.setActive(false);
                batteryInfo.setDischargeTicks(1000);
                extendedSystemInfoList.remove(batteryInfo);
            }
        }
    }
}
