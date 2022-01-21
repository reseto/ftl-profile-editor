// Variables with unknown meanings are named with greek letters.
// Classes for unknown objects are named after deities.
// http://en.wikipedia.org/wiki/List_of_Greek_mythological_figures#Personified_concepts

// For reference on weapons and projectiles, see the "Complete Weapon Attribute Table":
// https://subsetgames.com/forum/viewtopic.php?f=12&t=24600


package net.blerf.ftl.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.blerf.ftl.constants.Difficulty;
import net.blerf.ftl.model.XYPair;
import net.blerf.ftl.model.pod.BoarderDronePodInfo;
import net.blerf.ftl.model.pod.EmptyDronePodInfo;
import net.blerf.ftl.model.pod.ExtendedDronePodInfo;
import net.blerf.ftl.model.pod.HackingDronePodInfo;
import net.blerf.ftl.model.pod.IntegerDronePodInfo;
import net.blerf.ftl.model.pod.ShieldDronePodInfo;
import net.blerf.ftl.model.pod.ZigZagDronePodInfo;
import net.blerf.ftl.model.shiplayout.DoorCoordinate;
import net.blerf.ftl.model.shiplayout.ShipLayout;
import net.blerf.ftl.model.shiplayout.ShipLayoutDoor;
import net.blerf.ftl.model.shiplayout.ShipLayoutRoom;
import net.blerf.ftl.model.state.CrewState;
import net.blerf.ftl.model.state.CrewType;
import net.blerf.ftl.model.state.SavedGameState;
import net.blerf.ftl.model.state.ShipState;
import net.blerf.ftl.model.state.SystemType;
import net.blerf.ftl.model.state.WeaponModuleState;
import net.blerf.ftl.xml.DroneBlueprint;
import net.blerf.ftl.xml.ShipBlueprint;
import net.blerf.ftl.xml.WeaponBlueprint;

@Slf4j
public class SavedGameParser extends Parser {

    public SavedGameParser() {
    }

    public SavedGameState readSavedGame(File savFile) throws IOException {
        SavedGameState gameState = null;

        FileInputStream in = null;
        try {
            in = new FileInputStream(savFile);
            gameState = readSavedGame(in);
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
            }
        }

        return gameState;
    }

    public SavedGameState readSavedGame(FileInputStream in) throws IOException {
        InputStream layoutStream = null;
        try {
            SavedGameState gameState = new SavedGameState();

            int fileFormat = readInt(in);
            gameState.setFileFormat(fileFormat);

            // FTL 1.6.1 introduced UTF-8 strings.
            super.setUnicode(fileFormat >= 11);

            if (fileFormat == 11) {
                gameState.setRandomNative(readBool(in));
            } else {
                gameState.setRandomNative(true);  // Always native before FTL 1.6.1.
            }

            if (fileFormat == 2) {
                // FTL 1.03.3 and earlier.
                gameState.setDLCEnabled(false);  // Not present before FTL 1.5.4.
            } else if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
                // FTL 1.5.4-1.5.10, 1.5.12, 1.5.13, or 1.6.1.
                gameState.setDLCEnabled(readBool(in));
            } else {
                throw new IOException(String.format("Unexpected first byte (%d) for a SAVED GAME.", fileFormat));
            }

            int diffFlag = readInt(in);
            Difficulty diff;
            if (diffFlag == 0) {
                diff = Difficulty.EASY;
            } else if (diffFlag == 1) {
                diff = Difficulty.NORMAL;
            } else if (diffFlag == 2 && (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11)) {
                diff = Difficulty.HARD;
            } else {
                throw new IOException(String.format("Unsupported difficulty flag for saved game: %d", diffFlag));
            }

            gameState.setDifficulty(diff);
            gameState.setTotalShipsDefeated(readInt(in));
            gameState.setTotalBeaconsExplored(readInt(in));
            gameState.setTotalScrapCollected(readInt(in));
            gameState.setTotalCrewHired(readInt(in));

            String playerShipName = readString(in);         // Redundant.
            gameState.setPlayerShipName(playerShipName);

            String playerShipBlueprintId = readString(in);  // Redundant.
            gameState.setPlayerShipBlueprintId(playerShipBlueprintId);

            int oneBasedSectorNumber = readInt(in);  // Redundant.

            // Always 0?
            gameState.setUnknownBeta(readInt(in));

            int stateVarCount = readInt(in);
            for (int i = 0; i < stateVarCount; i++) {
                String stateVarId = readString(in);
                Integer stateVarValue = readInt(in);
                gameState.setStateVar(stateVarId, stateVarValue);
            }

            ShipState playerShipState = readShip(in, false, fileFormat, gameState.isDLCEnabled());
            gameState.setPlayerShip(playerShipState);

            // Nearby ships have no cargo, so this isn't in readShip().
            int cargoCount = readInt(in);
            for (int i = 0; i < cargoCount; i++) {
                gameState.addCargoItemId(readString(in));
            }

            gameState.setSectorTreeSeed(readInt(in));

            gameState.setSectorLayoutSeed(readInt(in));

            gameState.setRebelFleetOffset(readInt(in));

            gameState.setRebelFleetFudge(readInt(in));

            gameState.setRebelPursuitMod(readInt(in));

            if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
                gameState.setCurrentBeaconId(readInt(in));

                gameState.setWaiting(readBool(in));
                gameState.setWaitEventSeed(readInt(in));
                gameState.setUnknownEpsilon(readString(in));
                gameState.setSectorHazardsVisible(readBool(in));
                gameState.setRebelFlagshipVisible(readBool(in));
                gameState.setRebelFlagshipHop(readInt(in));
                gameState.setRebelFlagshipMoving(readBool(in));
                gameState.setRebelFlagshipRetreating(readBool(in));
                gameState.setRebelFlagshipBaseTurns(readInt(in));
            } else if (fileFormat == 2) {
                gameState.setSectorHazardsVisible(readBool(in));

                gameState.setRebelFlagshipVisible(readBool(in));

                gameState.setRebelFlagshipHop(readInt(in));

                gameState.setRebelFlagshipMoving(readBool(in));
            }

            int sectorVisitationCount = readInt(in);
            List<Boolean> route = new ArrayList<Boolean>();
            for (int i = 0; i < sectorVisitationCount; i++) {
                route.add(readBool(in));
            }
            gameState.setSectorVisitation(route);

            int sectorNumber = readInt(in);
            gameState.setSectorNumber(sectorNumber);

            gameState.setSectorIsHiddenCrystalWorlds(readBool(in));

            int beaconCount = readInt(in);
            for (int i = 0; i < beaconCount; i++) {
                gameState.addBeacon(readBeacon(in, fileFormat));
            }

            int questEventCount = readInt(in);
            for (int i = 0; i < questEventCount; i++) {
                String questEventId = readString(in);
                int questBeaconId = readInt(in);
                gameState.addQuestEvent(questEventId, questBeaconId);
            }

            int distantQuestEventCount = readInt(in);
            for (int i = 0; i < distantQuestEventCount; i++) {
                String distantQuestEventId = readString(in);
                gameState.addDistantQuestEvent(distantQuestEventId);
            }

            if (fileFormat == 2) {
                gameState.setCurrentBeaconId(readInt(in));

                boolean shipNearby = readBool(in);
                if (shipNearby) {
                    ShipState nearbyShipState = readShip(in, true, fileFormat, gameState.isDLCEnabled());
                    gameState.setNearbyShip(nearbyShipState);
                }

                RebelFlagshipState flagshipState = readRebelFlagship(in);
                gameState.setRebelFlagshipState(flagshipState);
            } else if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
                // Current beaconId was set earlier.

                gameState.setUnknownMu(readInt(in));

                EncounterState encounter = readEncounter(in, fileFormat);
                gameState.setEncounter(encounter);

                boolean shipNearby = readBool(in);
                if (shipNearby) {
                    gameState.setRebelFlagshipNearby(readBool(in));

                    ShipState nearbyShipState = readShip(in, true, fileFormat, gameState.isDLCEnabled());
                    gameState.setNearbyShip(nearbyShipState);

                    gameState.setNearbyShipAI(readNearbyShipAI(in));
                }

                gameState.setEnvironment(readEnvironment(in));

                // Flagship state is set much later.

                int projectileCount = readInt(in);
                for (int i = 0; i < projectileCount; i++) {
                    gameState.addProjectile(readProjectile(in, fileFormat));
                }

                readExtendedShipInfo(in, gameState.getPlayerShip(), fileFormat);

                if (gameState.getNearbyShip() != null) {
                    readExtendedShipInfo(in, gameState.getNearbyShip(), fileFormat);
                }

                gameState.setUnknownNu(readInt(in));

                if (gameState.getNearbyShip() != null) {
                    gameState.setUnknownXi(readInt(in));
                }

                gameState.setAutofire(readBool(in));

                RebelFlagshipState flagship = new RebelFlagshipState();

                flagship.setUnknownAlpha(readInt(in));
                flagship.setPendingStage(readInt(in));
                flagship.setUnknownGamma(readInt(in));
                flagship.setUnknownDelta(readInt(in));

                int flagshipOccupancyCount = readInt(in);
                for (int i = 0; i < flagshipOccupancyCount; i++) {
                    flagship.setPreviousOccupancy(i, readInt(in));
                }

                gameState.setRebelFlagshipState(flagship);
            }

            // The stream should end here.

            int bytesRemaining = (int) (in.getChannel().size() - in.getChannel().position());
            if (bytesRemaining > 0) {
                gameState.addMysteryBytes(new MysteryBytes(in, bytesRemaining));
            }

            return gameState;  // The finally block will still be executed.
        } finally {
            try {
                if (layoutStream != null) layoutStream.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Writes a gameState to a stream.
     * <p>
     * Any MysteryBytes will be omitted.
     */
    public void writeSavedGame(OutputStream out, SavedGameState gameState) throws IOException {

        int fileFormat = gameState.getFileFormat();
        writeInt(out, fileFormat);

        // FTL 1.6.1 introduced UTF-8 strings.
        super.setUnicode(fileFormat >= 11);

        if (fileFormat == 11) {
            writeBool(out, gameState.isRandomNative());
        }

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            writeBool(out, gameState.isDLCEnabled());
        } else {
            throw new IOException("Unsupported fileFormat: " + fileFormat);
        }

        int diffFlag = 0;
        if (gameState.getDifficulty() == Difficulty.EASY) {
            diffFlag = 0;
        } else if (gameState.getDifficulty() == Difficulty.NORMAL) {
            diffFlag = 1;
        } else if (gameState.getDifficulty() == Difficulty.HARD && (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11)) {
            diffFlag = 2;
        } else {
            log.warn("Substituting EASY for unsupported difficulty for saved game: {}", gameState.getDifficulty());
            diffFlag = 0;
        }
        writeInt(out, diffFlag);
        writeInt(out, gameState.getTotalShipsDefeated());
        writeInt(out, gameState.getTotalBeaconsExplored());
        writeInt(out, gameState.getTotalScrapCollected());
        writeInt(out, gameState.getTotalCrewHired());

        writeString(out, gameState.getPlayerShipName());
        writeString(out, gameState.getPlayerShipBlueprintId());

        // Redundant 1-based sector number.
        writeInt(out, gameState.getSectorNumber() + 1);

        writeInt(out, gameState.getUnknownBeta());

        writeInt(out, gameState.getStateVars().size());
        for (Map.Entry<String, Integer> entry : gameState.getStateVars().entrySet()) {
            writeString(out, entry.getKey());
            writeInt(out, entry.getValue());
        }

        writeShip(out, gameState.getPlayerShip(), fileFormat);

        writeInt(out, gameState.getCargoIdList().size());
        for (String cargoItemId : gameState.getCargoIdList()) {
            writeString(out, cargoItemId);
        }

        writeInt(out, gameState.getSectorTreeSeed());
        writeInt(out, gameState.getSectorLayoutSeed());
        writeInt(out, gameState.getRebelFleetOffset());
        writeInt(out, gameState.getRebelFleetFudge());
        writeInt(out, gameState.getRebelPursuitMod());

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            writeInt(out, gameState.getCurrentBeaconId());

            writeBool(out, gameState.isWaiting());
            writeInt(out, gameState.getWaitEventSeed());
            writeString(out, gameState.getUnknownEpsilon());
            writeBool(out, gameState.areSectorHazardsVisible());
            writeBool(out, gameState.isRebelFlagshipVisible());
            writeInt(out, gameState.getRebelFlagshipHop());
            writeBool(out, gameState.isRebelFlagshipMoving());
            writeBool(out, gameState.isRebelFlagshipRetreating());
            writeInt(out, gameState.getRebelFlagshipBaseTurns());
        } else if (fileFormat == 2) {
            writeBool(out, gameState.areSectorHazardsVisible());
            writeBool(out, gameState.isRebelFlagshipVisible());
            writeInt(out, gameState.getRebelFlagshipHop());
            writeBool(out, gameState.isRebelFlagshipMoving());
        }

        writeInt(out, gameState.getSectorVisitation().size());
        for (Boolean visited : gameState.getSectorVisitation()) {
            writeBool(out, visited);
        }

        writeInt(out, gameState.getSectorNumber());
        writeBool(out, gameState.isSectorHiddenCrystalWorlds());

        writeInt(out, gameState.getBeaconList().size());
        for (BeaconState beacon : gameState.getBeaconList()) {
            writeBeacon(out, beacon, fileFormat);
        }

        writeInt(out, gameState.getQuestEventMap().size());
        for (Map.Entry<String, Integer> entry : gameState.getQuestEventMap().entrySet()) {
            writeString(out, entry.getKey());
            writeInt(out, entry.getValue());
        }

        writeInt(out, gameState.getDistantQuestEventList().size());
        for (String questEventId : gameState.getDistantQuestEventList()) {
            writeString(out, questEventId);
        }

        if (fileFormat == 2) {
            writeInt(out, gameState.getCurrentBeaconId());

            ShipState nearbyShip = gameState.getNearbyShip();
            writeBool(out, (nearbyShip != null));
            if (nearbyShip != null) {
                writeShip(out, nearbyShip, fileFormat);
            }

            writeRebelFlagship(out, gameState.getRebelFlagshipState());
        } else if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            // Current beaconId was set earlier.

            writeInt(out, gameState.getUnknownMu());

            writeEncounter(out, gameState.getEncounter(), fileFormat);

            ShipState nearbyShip = gameState.getNearbyShip();
            writeBool(out, (nearbyShip != null));
            if (nearbyShip != null) {
                writeBool(out, gameState.isRebelFlagshipNearby());

                writeShip(out, nearbyShip, fileFormat);

                writeNearbyShipAI(out, gameState.getNearbyShipAI());
            }

            writeEnvironment(out, gameState.getEnvironment());

            // Flagship state is set much later.

            writeInt(out, gameState.getProjectileList().size());
            for (ProjectileState projectile : gameState.getProjectileList()) {
                writeProjectile(out, projectile, fileFormat);
            }

            writeExtendedShipInfo(out, gameState.getPlayerShip(), fileFormat);

            if (gameState.getNearbyShip() != null) {
                writeExtendedShipInfo(out, gameState.getNearbyShip(), fileFormat);
            }

            writeInt(out, gameState.getUnknownNu());

            if (gameState.getNearbyShip() != null) {
                writeInt(out, gameState.getUnknownXi());
            }

            writeBool(out, gameState.getAutofire());

            RebelFlagshipState flagship = gameState.getRebelFlagshipState();

            writeInt(out, flagship.getUnknownAlpha());
            writeInt(out, flagship.getPendingStage());
            writeInt(out, flagship.getUnknownGamma());
            writeInt(out, flagship.getUnknownDelta());

            writeInt(out, flagship.getOccupancyMap().size());
            for (Map.Entry<Integer, Integer> entry : flagship.getOccupancyMap().entrySet()) {
                int occupantCount = entry.getValue();
                writeInt(out, occupantCount);
            }
        }
    }

    private ShipState readShip(InputStream in, boolean auto, int fileFormat, boolean dlcEnabled) throws IOException {

        String shipBlueprintId = readString(in);
        String shipName = readString(in);
        String shipGfxBaseName = readString(in);

        ShipBlueprint shipBlueprint = DataManager.get().getShip(shipBlueprintId);
        if (shipBlueprint == null) {
            throw new RuntimeException(String.format("Could not find blueprint for%s ship: %s", (auto ? " auto" : ""), shipName));
        }

        String shipLayoutId = shipBlueprint.getLayoutId();

        // Use this for room and door info later.
        ShipLayout shipLayout = DataManager.get().getShipLayout(shipLayoutId);
        if (shipLayout == null) {
            throw new RuntimeException(String.format("Could not find layout for%s ship: %s", (auto ? " auto" : ""), shipName));
        }

        ShipState shipState = new ShipState(shipName, shipBlueprintId, shipLayoutId, shipGfxBaseName, auto);

        int startingCrewCount = readInt(in);
        for (int i = 0; i < startingCrewCount; i++) {
            shipState.addStartingCrewMember(readStartingCrewMember(in));
        }

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            shipState.setHostile(readBool(in));
            shipState.setJumpChargeTicks(readInt(in));
            shipState.setJumping(readBool(in));
            shipState.setJumpAnimTicks(readInt(in));
        }

        shipState.setHullAmt(readInt(in));
        shipState.setFuelAmt(readInt(in));
        shipState.setDronePartsAmt(readInt(in));
        shipState.setMissilesAmt(readInt(in));
        shipState.setScrapAmt(readInt(in));

        int crewCount = readInt(in);
        for (int i = 0; i < crewCount; i++) {
            shipState.addCrewMember(readCrewMember(in, fileFormat));
        }

        // System info is stored in this order.
        List<SystemType> systemTypes = new ArrayList<SystemType>();
        systemTypes.add(SystemType.SHIELDS);
        systemTypes.add(SystemType.ENGINES);
        systemTypes.add(SystemType.OXYGEN);
        systemTypes.add(SystemType.WEAPONS);
        systemTypes.add(SystemType.DRONE_CTRL);
        systemTypes.add(SystemType.MEDBAY);
        systemTypes.add(SystemType.PILOT);
        systemTypes.add(SystemType.SENSORS);
        systemTypes.add(SystemType.DOORS);
        systemTypes.add(SystemType.TELEPORTER);
        systemTypes.add(SystemType.CLOAKING);
        systemTypes.add(SystemType.ARTILLERY);
        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            systemTypes.add(SystemType.BATTERY);
            systemTypes.add(SystemType.CLONEBAY);
            systemTypes.add(SystemType.MIND);
            systemTypes.add(SystemType.HACKING);
        }

        shipState.setReservePowerCapacity(readInt(in));
        for (SystemType systemType : systemTypes) {
            shipState.addSystem(readSystem(in, systemType, fileFormat));

            // Systems that exist in multiple rooms have additional SystemStates.
            // Example: Flagship's artillery.
            //
            // In FTL 1.01-1.03.3 the flagship wasn't a nearby ship outside of combat,
            // So this never occurred. TODO: Confirm reports that 1.5.4 allows
            // multi-room systems on regular ships and check the editor's
            // compatibility.

            ShipBlueprint.SystemList.SystemRoom[] rooms = shipBlueprint.getSystemList().getSystemRoom(systemType);
            if (rooms != null && rooms.length > 1) {
                for (int q = 1; q < rooms.length; q++) {
                    shipState.addSystem(readSystem(in, systemType, fileFormat));
                }
            }
        }

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {

            SystemState tmpSystem = null;

            tmpSystem = shipState.getSystem(SystemType.CLONEBAY);
            if (tmpSystem != null && tmpSystem.getCapacity() > 0) {
                ClonebayInfo clonebayInfo = new ClonebayInfo();

                clonebayInfo.setBuildTicks(readInt(in));
                clonebayInfo.setBuildTicksGoal(readInt(in));
                clonebayInfo.setDoomTicks(readInt(in));

                shipState.addExtendedSystemInfo(clonebayInfo);
            }
            tmpSystem = shipState.getSystem(SystemType.BATTERY);
            if (tmpSystem != null && tmpSystem.getCapacity() > 0) {
                BatteryInfo batteryInfo = new BatteryInfo();

                batteryInfo.setActive(readBool(in));
                batteryInfo.setUsedBattery(readInt(in));
                batteryInfo.setDischargeTicks(readInt(in));

                shipState.addExtendedSystemInfo(batteryInfo);
            }

            // The shields info always exists, even if the shields system doesn't.
            if (true) {
                ShieldsInfo shieldsInfo = new ShieldsInfo();

                shieldsInfo.setShieldLayers(readInt(in));
                shieldsInfo.setEnergyShieldLayers(readInt(in));
                shieldsInfo.setEnergyShieldMax(readInt(in));
                shieldsInfo.setShieldRechargeTicks(readInt(in));

                shieldsInfo.setShieldDropAnimOn(readBool(in));
                shieldsInfo.setShieldDropAnimTicks(readInt(in));    // TODO: Confirm.

                shieldsInfo.setShieldRaiseAnimOn(readBool(in));
                shieldsInfo.setShieldRaiseAnimTicks(readInt(in));   // TODO: Confirm.

                shieldsInfo.setEnergyShieldAnimOn(readBool(in));
                shieldsInfo.setEnergyShieldAnimTicks(readInt(in));  // TODO: Confirm.

                // A pair. Usually noise. Sometimes 0.
                shieldsInfo.setUnknownLambda(readInt(in));   // TODO: Confirm: Shield down point X.
                shieldsInfo.setUnknownMu(readInt(in));       // TODO: Confirm: Shield down point Y.

                shipState.addExtendedSystemInfo(shieldsInfo);
            }

            tmpSystem = shipState.getSystem(SystemType.CLOAKING);
            if (tmpSystem != null && tmpSystem.getCapacity() > 0) {
                CloakingInfo cloakingInfo = new CloakingInfo();

                cloakingInfo.setUnknownAlpha(readInt(in));
                cloakingInfo.setUnknownBeta(readInt(in));
                cloakingInfo.setCloakTicksGoal(readInt(in));
                cloakingInfo.setCloakTicks(readMinMaxedInt(in));

                shipState.addExtendedSystemInfo(cloakingInfo);
            }

            // Other ExtendedSystemInfo may be added to the ship later (FTL 1.5.4+).
        }

        // Room states are stored in roomId order.
        int roomCount = shipLayout.getRoomCount();
        for (int r = 0; r < roomCount; r++) {
            ShipLayoutRoom layoutRoom = shipLayout.getRoom(r);

            shipState.addRoom(readRoom(in, layoutRoom.squaresH, layoutRoom.squaresV, fileFormat));
        }

        int breachCount = readInt(in);
        for (int i = 0; i < breachCount; i++) {
            shipState.setBreach(readInt(in), readInt(in), readInt(in));
        }

        // Doors are defined in the layout text file, but their order is
        // different at runtime. Vacuum-adjacent doors are plucked out and
        // moved to the end... for some reason.
        Map<DoorCoordinate, ShipLayoutDoor> vacuumDoorMap = new LinkedHashMap<DoorCoordinate, ShipLayoutDoor>();
        Map<DoorCoordinate, ShipLayoutDoor> layoutDoorMap = shipLayout.getDoorMap();
        for (Map.Entry<DoorCoordinate, ShipLayoutDoor> entry : layoutDoorMap.entrySet()) {
            DoorCoordinate doorCoord = entry.getKey();
            ShipLayoutDoor layoutDoor = entry.getValue();

            if (layoutDoor.roomIdA == -1 || layoutDoor.roomIdB == -1) {
                vacuumDoorMap.put(doorCoord, layoutDoor);
                continue;
            }
            shipState.setDoor(doorCoord.x, doorCoord.y, doorCoord.v, readDoor(in, fileFormat));
        }
        for (Map.Entry<DoorCoordinate, ShipLayoutDoor> entry : vacuumDoorMap.entrySet()) {
            DoorCoordinate doorCoord = entry.getKey();

            shipState.setDoor(doorCoord.x, doorCoord.y, doorCoord.v, readDoor(in, fileFormat));
        }

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            shipState.setCloakAnimTicks(readInt(in));
        }

        if (fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            int crystalCount = readInt(in);
            List<LockdownCrystal> crystalList = new ArrayList<LockdownCrystal>();
            for (int i = 0; i < crystalCount; i++) {
                crystalList.add(readLockdownCrystal(in));
            }
            shipState.setLockdownCrystalList(crystalList);
        }

        int weaponCount = readInt(in);
        for (int i = 0; i < weaponCount; i++) {
            WeaponState weapon = new WeaponState();
            weapon.setWeaponId(readString(in));
            weapon.setArmed(readBool(in));

            if (fileFormat == 2) {  // No longer used as of FTL 1.5.4.
                weapon.setCooldownTicks(readInt(in));
            }

            shipState.addWeapon(weapon);
        }
        // WeaponStates may have WeaponModules set on them later (FTL 1.5.4+).

        int droneCount = readInt(in);
        for (int i = 0; i < droneCount; i++) {
            shipState.addDrone(readDrone(in));
        }
        // DroneStates may have ExtendedDroneInfo set on them later (FTL 1.5.4+).

        int augmentCount = readInt(in);
        for (int i = 0; i < augmentCount; i++) {
            shipState.addAugmentId(readString(in));
        }

        // Standalone drones may be added to the ship later (FTL 1.5.4+).

        return shipState;
    }

    public void writeShip(OutputStream out, ShipState shipState, int fileFormat) throws IOException {
        String shipBlueprintId = shipState.getShipBlueprintId();

        ShipBlueprint shipBlueprint = DataManager.get().getShip(shipBlueprintId);
        if (shipBlueprint == null)
            throw new RuntimeException(String.format("Could not find blueprint for%s ship: %s", (shipState.isAuto() ? " auto" : ""), shipState.getShipName()));

        String shipLayoutId = shipBlueprint.getLayoutId();

        ShipLayout shipLayout = DataManager.get().getShipLayout(shipLayoutId);
        if (shipLayout == null)
            throw new RuntimeException(String.format("Could not find layout for%s ship: %s", (shipState.isAuto() ? " auto" : ""), shipState.getShipName()));


        writeString(out, shipBlueprintId);
        writeString(out, shipState.getShipName());
        writeString(out, shipState.getShipGraphicsBaseName());

        writeInt(out, shipState.getStartingCrewList().size());
        for (StartingCrewState startingCrew : shipState.getStartingCrewList()) {
            writeStartingCrewMember(out, startingCrew);
        }

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            writeBool(out, shipState.isHostile());
            writeInt(out, shipState.getJumpChargeTicks());
            writeBool(out, shipState.isJumping());
            writeInt(out, shipState.getJumpAnimTicks());
        }

        writeInt(out, shipState.getHullAmt());
        writeInt(out, shipState.getFuelAmt());
        writeInt(out, shipState.getDronePartsAmt());
        writeInt(out, shipState.getMissilesAmt());
        writeInt(out, shipState.getScrapAmt());

        writeInt(out, shipState.getCrewList().size());
        for (CrewState crew : shipState.getCrewList()) {
            writeCrewMember(out, crew, fileFormat);
        }

        // System info is stored in this order.
        List<SystemType> systemTypes = new ArrayList<SystemType>();
        systemTypes.add(SystemType.SHIELDS);
        systemTypes.add(SystemType.ENGINES);
        systemTypes.add(SystemType.OXYGEN);
        systemTypes.add(SystemType.WEAPONS);
        systemTypes.add(SystemType.DRONE_CTRL);
        systemTypes.add(SystemType.MEDBAY);
        systemTypes.add(SystemType.PILOT);
        systemTypes.add(SystemType.SENSORS);
        systemTypes.add(SystemType.DOORS);
        systemTypes.add(SystemType.TELEPORTER);
        systemTypes.add(SystemType.CLOAKING);
        systemTypes.add(SystemType.ARTILLERY);
        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            systemTypes.add(SystemType.BATTERY);
            systemTypes.add(SystemType.CLONEBAY);
            systemTypes.add(SystemType.MIND);
            systemTypes.add(SystemType.HACKING);
        }

        writeInt(out, shipState.getReservePowerCapacity());

        for (SystemType systemType : systemTypes) {
            List<SystemState> systemList = shipState.getSystems(systemType);
            if (systemList.size() > 0) {
                for (SystemState systemState : systemList) {
                    writeSystem(out, systemState, fileFormat);
                }
            } else {
                writeInt(out, 0);  // Equivalent to constructing and writing a 0-capacity system.
            }
        }

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {

            SystemState clonebayState = shipState.getSystem(SystemType.CLONEBAY);
            if (clonebayState != null && clonebayState.getCapacity() > 0) {
                ClonebayInfo clonebayInfo = shipState.getExtendedSystemInfo(ClonebayInfo.class);
                // This should not be null.
                writeInt(out, clonebayInfo.getBuildTicks());
                writeInt(out, clonebayInfo.getBuildTicksGoal());
                writeInt(out, clonebayInfo.getDoomTicks());
            }

            SystemState batteryState = shipState.getSystem(SystemType.BATTERY);
            if (batteryState != null && batteryState.getCapacity() > 0) {
                BatteryInfo batteryInfo = shipState.getExtendedSystemInfo(BatteryInfo.class);
                // This should not be null.
                writeBool(out, batteryInfo.isActive());
                writeInt(out, batteryInfo.getUsedBattery());
                writeInt(out, batteryInfo.getDischargeTicks());
            }

            if (true) {
                ShieldsInfo shieldsInfo = shipState.getExtendedSystemInfo(ShieldsInfo.class);
                // This should not be null.
                writeInt(out, shieldsInfo.getShieldLayers());
                writeInt(out, shieldsInfo.getEnergyShieldLayers());
                writeInt(out, shieldsInfo.getEnergyShieldMax());
                writeInt(out, shieldsInfo.getShieldRechargeTicks());

                writeBool(out, shieldsInfo.isShieldDropAnimOn());
                writeInt(out, shieldsInfo.getShieldDropAnimTicks());

                writeBool(out, shieldsInfo.isShieldRaiseAnimOn());
                writeInt(out, shieldsInfo.getShieldRaiseAnimTicks());

                writeBool(out, shieldsInfo.isEnergyShieldAnimOn());
                writeInt(out, shieldsInfo.getEnergyShieldAnimTicks());

                writeInt(out, shieldsInfo.getUnknownLambda());
                writeInt(out, shieldsInfo.getUnknownMu());
            }

            SystemState cloakingState = shipState.getSystem(SystemType.CLOAKING);
            if (cloakingState != null && cloakingState.getCapacity() > 0) {
                CloakingInfo cloakingInfo = shipState.getExtendedSystemInfo(CloakingInfo.class);
                // This should not be null.
                writeInt(out, cloakingInfo.getUnknownAlpha());
                writeInt(out, cloakingInfo.getUnknownBeta());
                writeInt(out, cloakingInfo.getCloakTicksGoal());

                writeMinMaxedInt(out, cloakingInfo.getCloakTicks());
            }
        }

        int roomCount = shipLayout.getRoomCount();
        for (int r = 0; r < roomCount; r++) {
            ShipLayoutRoom layoutRoom = shipLayout.getRoom(r);

            RoomState room = shipState.getRoom(r);
            writeRoom(out, room, layoutRoom.squaresH, layoutRoom.squaresV, fileFormat);
        }

        writeInt(out, shipState.getBreachMap().size());
        for (Map.Entry<XYPair, Integer> entry : shipState.getBreachMap().entrySet()) {
            writeInt(out, entry.getKey().x);
            writeInt(out, entry.getKey().y);
            writeInt(out, entry.getValue());
        }

        // Doors are defined in the layout text file, but their
        // order is different at runtime. Vacuum-adjacent doors
        // are plucked out and moved to the end... for some
        // reason.
        Map<DoorCoordinate, DoorState> shipDoorMap = shipState.getDoorMap();
        Map<DoorCoordinate, ShipLayoutDoor> vacuumDoorMap = new LinkedHashMap<DoorCoordinate, ShipLayoutDoor>();
        Map<DoorCoordinate, ShipLayoutDoor> layoutDoorMap = shipLayout.getDoorMap();
        for (Map.Entry<DoorCoordinate, ShipLayoutDoor> entry : layoutDoorMap.entrySet()) {
            DoorCoordinate doorCoord = entry.getKey();
            ShipLayoutDoor layoutDoor = entry.getValue();

            if (layoutDoor.roomIdA == -1 || layoutDoor.roomIdB == -1) {
                vacuumDoorMap.put(doorCoord, layoutDoor);
                continue;
            }
            writeDoor(out, shipDoorMap.get(doorCoord), fileFormat);
        }
        for (Map.Entry<DoorCoordinate, ShipLayoutDoor> entry : vacuumDoorMap.entrySet()) {
            DoorCoordinate doorCoord = entry.getKey();

            writeDoor(out, shipDoorMap.get(doorCoord), fileFormat);
        }

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            writeInt(out, shipState.getCloakAnimTicks());
        }

        if (fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            writeInt(out, shipState.getLockdownCrystalList().size());
            for (LockdownCrystal crystal : shipState.getLockdownCrystalList()) {
                writeLockdownCrystal(out, crystal);
            }
        }

        writeInt(out, shipState.getWeaponList().size());
        for (WeaponState weapon : shipState.getWeaponList()) {
            writeString(out, weapon.getWeaponId());
            writeBool(out, weapon.isArmed());

            if (fileFormat == 2) {  // No longer used as of FTL 1.5.4.
                writeInt(out, weapon.getCooldownTicks());
            }
        }

        writeInt(out, shipState.getDroneList().size());
        for (DroneState drone : shipState.getDroneList()) {
            writeDrone(out, drone);
        }

        writeInt(out, shipState.getAugmentIdList().size());
        for (String augmentId : shipState.getAugmentIdList()) {
            writeString(out, augmentId);
        }
    }

    private StartingCrewState readStartingCrewMember(InputStream in) throws IOException {
        StartingCrewState startingCrew = new StartingCrewState();

        String raceString = readString(in);
        CrewType race = CrewType.findById(raceString);
        if (race != null) {
            startingCrew.setRace(race);
        } else {
            throw new IOException("Unsupported starting crew race: " + raceString);
        }

        startingCrew.setName(readString(in));

        return startingCrew;
    }

    public void writeStartingCrewMember(OutputStream out, StartingCrewState startingCrew) throws IOException {
        writeString(out, startingCrew.getRace().getId());
        writeString(out, startingCrew.getName());
    }

    private CrewState readCrewMember(InputStream in, int fileFormat) throws IOException {
        CrewState crew = new CrewState();
        crew.setName(readString(in));

        String raceString = readString(in);
        CrewType race = CrewType.findById(raceString);
        if (race != null) {
            crew.setRace(race);
        } else {
            throw new IOException("Unsupported crew race: " + raceString);
        }

        crew.setEnemyBoardingDrone(readBool(in));
        crew.setHealth(readInt(in));
        crew.setSpriteX(readInt(in));
        crew.setSpriteY(readInt(in));
        crew.setRoomId(readInt(in));
        crew.setRoomSquare(readInt(in));
        crew.setPlayerControlled(readBool(in));

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            crew.setCloneReady(readInt(in));

            int deathOrder = readInt(in);  // Redundant. Exactly the same as Clonebay Priority.

            int tintCount = readInt(in);
            List<Integer> spriteTintIndeces = new ArrayList<Integer>();
            for (int i = 0; i < tintCount; i++) {
                spriteTintIndeces.add(readInt(in));
            }
            crew.setSpriteTintIndices(spriteTintIndeces);

            crew.setMindControlled(readBool(in));
            crew.setSavedRoomSquare(readInt(in));
            crew.setSavedRoomId(readInt(in));
        }

        crew.setPilotSkill(readInt(in));
        crew.setEngineSkill(readInt(in));
        crew.setShieldSkill(readInt(in));
        crew.setWeaponSkill(readInt(in));
        crew.setRepairSkill(readInt(in));
        crew.setCombatSkill(readInt(in));
        crew.setMale(readBool(in));
        crew.setRepairs(readInt(in));
        crew.setCombatKills(readInt(in));
        crew.setPilotedEvasions(readInt(in));
        crew.setJumpsSurvived(readInt(in));
        crew.setSkillMasteriesEarned(readInt(in));

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            crew.setStunTicks(readInt(in));
            crew.setHealthBoost(readInt(in));
            crew.setClonebayPriority(readInt(in));
            crew.setDamageBoost(readInt(in));
            crew.setUnknownLambda(readInt(in));
            crew.setUniversalDeathCount(readInt(in));

            if (fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
                crew.setPilotMasteryOne(readBool(in));
                crew.setPilotMasteryTwo(readBool(in));
                crew.setEngineMasteryOne(readBool(in));
                crew.setEngineMasteryTwo(readBool(in));
                crew.setShieldMasteryOne(readBool(in));
                crew.setShieldMasteryTwo(readBool(in));
                crew.setWeaponMasteryOne(readBool(in));
                crew.setWeaponMasteryTwo(readBool(in));
                crew.setRepairMasteryOne(readBool(in));
                crew.setRepairMasteryTwo(readBool(in));
                crew.setCombatMasteryOne(readBool(in));
                crew.setCombatMasteryTwo(readBool(in));
            }

            crew.setUnknownNu(readBool(in));

            crew.setTeleportAnim(readAnim(in));

            crew.setUnknownPhi(readBool(in));

            if (CrewType.CRYSTAL.equals(crew.getRace())) {
                crew.setLockdownRechargeTicks(readInt(in));
                crew.setLockdownRechargeTicksGoal(readInt(in));
                crew.setUnknownOmega(readInt(in));
            }
        }

        return crew;
    }

    public void writeCrewMember(OutputStream out, CrewState crew, int fileFormat) throws IOException {
        writeString(out, crew.getName());
        writeString(out, crew.getRace().getId());
        writeBool(out, crew.isEnemyBoardingDrone());
        writeInt(out, crew.getHealth());
        writeInt(out, crew.getSpriteX());
        writeInt(out, crew.getSpriteY());
        writeInt(out, crew.getRoomId());
        writeInt(out, crew.getRoomSquare());
        writeBool(out, crew.isPlayerControlled());

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            writeInt(out, crew.getCloneReady());

            int deathOrder = crew.getClonebayPriority();  // Redundant.
            writeInt(out, deathOrder);

            writeInt(out, crew.getSpriteTintIndices().size());
            for (Integer tintInt : crew.getSpriteTintIndices()) {
                writeInt(out, tintInt);
            }

            writeBool(out, crew.isMindControlled());
            writeInt(out, crew.getSavedRoomSquare());
            writeInt(out, crew.getSavedRoomId());
        }

        writeInt(out, crew.getPilotSkill());
        writeInt(out, crew.getEngineSkill());
        writeInt(out, crew.getShieldSkill());
        writeInt(out, crew.getWeaponSkill());
        writeInt(out, crew.getRepairSkill());
        writeInt(out, crew.getCombatSkill());
        writeBool(out, crew.isMale());
        writeInt(out, crew.getRepairs());
        writeInt(out, crew.getCombatKills());
        writeInt(out, crew.getPilotedEvasions());
        writeInt(out, crew.getJumpsSurvived());
        writeInt(out, crew.getSkillMasteriesEarned());

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            writeInt(out, crew.getStunTicks());
            writeInt(out, crew.getHealthBoost());
            writeInt(out, crew.getClonebayPriority());
            writeInt(out, crew.getDamageBoost());
            writeInt(out, crew.getUnknownLambda());
            writeInt(out, crew.getUniversalDeathCount());

            if (fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
                writeBool(out, crew.isPilotMasteryOne());
                writeBool(out, crew.isPilotMasteryTwo());
                writeBool(out, crew.isEngineMasteryOne());
                writeBool(out, crew.isEngineMasteryTwo());
                writeBool(out, crew.isShieldMasteryOne());
                writeBool(out, crew.isShieldMasteryTwo());
                writeBool(out, crew.isWeaponMasteryOne());
                writeBool(out, crew.isWeaponMasteryTwo());
                writeBool(out, crew.isRepairMasteryOne());
                writeBool(out, crew.isRepairMasteryTwo());
                writeBool(out, crew.isCombatMasteryOne());
                writeBool(out, crew.isCombatMasteryTwo());
            }

            writeBool(out, crew.isUnknownNu());

            writeAnim(out, crew.getTeleportAnim());

            writeBool(out, crew.isUnknownPhi());

            if (CrewType.CRYSTAL.equals(crew.getRace())) {
                writeInt(out, crew.getLockdownRechargeTicks());
                writeInt(out, crew.getLockdownRechargeTicksGoal());
                writeInt(out, crew.getUnknownOmega());
            }
        }
    }

    private SystemState readSystem(InputStream in, SystemType systemType, int fileFormat) throws IOException {
        SystemState system = new SystemState(systemType);
        int capacity = readInt(in);

        // Normally systems are 28 bytes, but if not present on the
        // ship, capacity will be zero, and the system will only
        // occupy the 4 bytes that declared the capacity. And the
        // next system will begin 24 bytes sooner.
        if (capacity > 0) {
            system.setCapacity(capacity);
            system.setPower(readInt(in));
            system.setDamagedBars(readInt(in));
            system.setIonizedBars(readInt(in));       // TODO: Active mind control has -1?

            system.setDeionizationTicks(readMinMaxedInt(in));  // May be MIN_VALUE.

            system.setRepairProgress(readInt(in));
            system.setDamageProgress(readInt(in));

            if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
                system.setBatteryPower(readInt(in));
                system.setHackLevel(readInt(in));
                system.setHacked(readBool(in));
                system.setTemporaryCapacityCap(readInt(in));
                system.setTemporaryCapacityLoss(readInt(in));
                system.setTemporaryCapacityDivisor(readInt(in));
            }
        }
        return system;
    }

    public void writeSystem(OutputStream out, SystemState system, int fileFormat) throws IOException {
        writeInt(out, system.getCapacity());
        if (system.getCapacity() > 0) {
            writeInt(out, system.getPower());
            writeInt(out, system.getDamagedBars());
            writeInt(out, system.getIonizedBars());

            writeMinMaxedInt(out, system.getDeionizationTicks());  // May be MIN_VALUE.

            writeInt(out, system.getRepairProgress());
            writeInt(out, system.getDamageProgress());

            if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
                writeInt(out, system.getBatteryPower());
                writeInt(out, system.getHackLevel());
                writeBool(out, system.isHacked());
                writeInt(out, system.getTemporaryCapacityCap());
                writeInt(out, system.getTemporaryCapacityLoss());
                writeInt(out, system.getTemporaryCapacityDivisor());
            }
        }
    }

    private RoomState readRoom(InputStream in, int squaresH, int squaresV, int fileFormat) throws IOException {
        RoomState room = new RoomState();
        int oxygen = readInt(in);
        if (oxygen < 0 || oxygen > 100) {
            throw new IOException("Unsupported room oxygen: " + oxygen);
        }
        room.setOxygen(oxygen);

        // Squares are written to disk top-to-bottom, left-to-right. (Index != ID!)
        SquareState[][] tmpSquares = new SquareState[squaresH][squaresV];
        for (int h = 0; h < squaresH; h++) {
            for (int v = 0; v < squaresV; v++) {
                tmpSquares[h][v] = new SquareState(readInt(in), readInt(in), readInt(in));
            }
        }
        // Add them to the room left-to-right, top-to-bottom. (Index == ID)
        for (int v = 0; v < squaresV; v++) {
            for (int h = 0; h < squaresH; h++) {
                room.addSquare(tmpSquares[h][v]);
            }
        }

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            room.setStationSquare(readInt(in));

            StationDirection stationDirection = null;
            int stationDirectionFlag = readInt(in);

            if (stationDirectionFlag == 0) {
                stationDirection = StationDirection.DOWN;
            } else if (stationDirectionFlag == 1) {
                stationDirection = StationDirection.RIGHT;
            } else if (stationDirectionFlag == 2) {
                stationDirection = StationDirection.UP;
            } else if (stationDirectionFlag == 3) {
                stationDirection = StationDirection.LEFT;
            } else if (stationDirectionFlag == 4) {
                stationDirection = StationDirection.NONE;
            } else {
                throw new IOException("Unsupported room station direction flag: " + stationDirection);
            }
            room.setStationDirection(stationDirection);
        }

        return room;
    }

    public void writeRoom(OutputStream out, RoomState room, int squaresH, int squaresV, int fileFormat) throws IOException {
        writeInt(out, room.getOxygen());

        // Squares referenced by IDs left-to-right, top-to-bottom. (Index == ID)
        List<SquareState> squareList = room.getSquareList();
        int squareIndex = 0;
        SquareState[][] tmpSquares = new SquareState[squaresH][squaresV];
        for (int v = 0; v < squaresV; v++) {
            for (int h = 0; h < squaresH; h++) {
                tmpSquares[h][v] = squareList.get(squareIndex++);
            }
        }
        // Squares are written to disk top-to-bottom, left-to-right. (Index != ID!)
        for (int h = 0; h < squaresH; h++) {
            for (int v = 0; v < squaresV; v++) {
                SquareState square = tmpSquares[h][v];
                writeInt(out, square.getFireHealth());
                writeInt(out, square.getIgnitionProgress());
                writeInt(out, square.getExtinguishmentProgress());
            }
        }

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            writeInt(out, room.getStationSquare());

            int stationDirectionFlag = 0;
            if (room.getStationDirection() == StationDirection.DOWN) {
                stationDirectionFlag = 0;
            } else if (room.getStationDirection() == StationDirection.RIGHT) {
                stationDirectionFlag = 1;
            } else if (room.getStationDirection() == StationDirection.UP) {
                stationDirectionFlag = 2;
            } else if (room.getStationDirection() == StationDirection.LEFT) {
                stationDirectionFlag = 3;
            } else if (room.getStationDirection() == StationDirection.NONE) {
                stationDirectionFlag = 4;
            } else {
                throw new IOException("Unsupported room station direction: " + room.getStationDirection().toString());
            }
            writeInt(out, stationDirectionFlag);
        }
    }

    private DoorState readDoor(InputStream in, int fileFormat) throws IOException {
        DoorState door = new DoorState();

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            door.setCurrentMaxHealth(readInt(in));
            door.setHealth(readInt(in));
            door.setNominalHealth(readInt(in));
        }

        door.setOpen(readBool(in));
        door.setWalkingThrough(readBool(in));

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            door.setUnknownDelta(readInt(in));
            door.setUnknownEpsilon(readInt(in));  // TODO: Confirm: Drone lockdown.
        }

        return door;
    }

    public void writeDoor(OutputStream out, DoorState door, int fileFormat) throws IOException {
        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            writeInt(out, door.getCurrentMaxHealth());
            writeInt(out, door.getHealth());
            writeInt(out, door.getNominalHealth());
        }

        writeBool(out, door.isOpen());
        writeBool(out, door.isWalkingThrough());

        if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
            writeInt(out, door.getUnknownDelta());
            writeInt(out, door.getUnknownEpsilon());
        }
    }

    private LockdownCrystal readLockdownCrystal(InputStream in) throws IOException {
        LockdownCrystal crystal = new LockdownCrystal();

        crystal.setCurrentPositionX(readInt(in));
        crystal.setCurrentPositionY(readInt(in));
        crystal.setSpeed(readInt(in));
        crystal.setGoalPositionX(readInt(in));
        crystal.setGoalPositionY(readInt(in));
        crystal.setArrived(readBool(in));
        crystal.setDone(readBool(in));
        crystal.setLifetime(readInt(in));
        crystal.setSuperFreeze(readBool(in));
        crystal.setLockingRoom(readInt(in));
        crystal.setAnimDirection(readInt(in));
        crystal.setShardProgress(readInt(in));

        return crystal;
    }

    public void writeLockdownCrystal(OutputStream out, LockdownCrystal crystal) throws IOException {
        writeInt(out, crystal.getCurrentPositionX());
        writeInt(out, crystal.getCurrentPositionY());
        writeInt(out, crystal.getSpeed());
        writeInt(out, crystal.getGoalPositionX());
        writeInt(out, crystal.getGoalPositionY());
        writeBool(out, crystal.hasArrived());
        writeBool(out, crystal.isDone());
        writeInt(out, crystal.getLifetime());
        writeBool(out, crystal.isSuperFreeze());
        writeInt(out, crystal.getLockingRoom());
        writeInt(out, crystal.getAnimDirection());
        writeInt(out, crystal.getShardProgress());
    }

    private DroneState readDrone(InputStream in) throws IOException {
        DroneState drone = new DroneState(readString(in));
        drone.setArmed(readBool(in));
        drone.setPlayerControlled(readBool(in));
        drone.setBodyX(readInt(in));
        drone.setBodyY(readInt(in));
        drone.setBodyRoomId(readInt(in));
        drone.setBodyRoomSquare(readInt(in));
        drone.setHealth(readInt(in));
        return drone;
    }

    public void writeDrone(OutputStream out, DroneState drone) throws IOException {
        writeString(out, drone.getDroneId());
        writeBool(out, drone.isArmed());
        writeBool(out, drone.isPlayerControlled());
        writeInt(out, drone.getBodyX());
        writeInt(out, drone.getBodyY());
        writeInt(out, drone.getBodyRoomId());
        writeInt(out, drone.getBodyRoomSquare());
        writeInt(out, drone.getHealth());
    }

    private BeaconState readBeacon(InputStream in, int fileFormat) throws IOException {
        BeaconState beacon = new BeaconState();

        beacon.setVisitCount(readInt(in));
        if (beacon.getVisitCount() > 0) {
            beacon.setBgStarscapeImageInnerPath(readString(in));
            beacon.setBgSpriteImageInnerPath(readString(in));
            beacon.setBgSpritePosX(readInt(in));
            beacon.setBgSpritePosY(readInt(in));
            beacon.setBgSpriteRotation(readInt(in));
        }

        beacon.setSeen(readBool(in));

        boolean enemyPresent = readBool(in);
        beacon.setEnemyPresent(enemyPresent);
        if (enemyPresent) {
            beacon.setShipEventId(readString(in));
            beacon.setAutoBlueprintId(readString(in));
            beacon.setShipEventSeed(readInt(in));

            // When player's at this beacon, the seed here matches
            // current encounter's seed.
        }

        int fleetPresence = readInt(in);
        switch (fleetPresence) {
            case 0:
                beacon.setFleetPresence(FleetPresence.NONE);
                break;
            case 1:
                beacon.setFleetPresence(FleetPresence.REBEL);
                break;
            case 2:
                beacon.setFleetPresence(FleetPresence.FEDERATION);
                break;
            case 3:
                beacon.setFleetPresence(FleetPresence.BOTH);
                break;
            default:
                throw new RuntimeException("Unknown fleet presence: " + fleetPresence);
        }

        beacon.setUnderAttack(readBool(in));

        boolean storePresent = readBool(in);
        if (storePresent) {
            StoreState store = new StoreState();

            int shelfCount = 2;          // FTL 1.01-1.03.3 only had two shelves.
            if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
                shelfCount = readInt(in);  // FTL 1.5.4 made shelves into an N-sized list.
            }
            for (int i = 0; i < shelfCount; i++) {
                store.addShelf(readStoreShelf(in, fileFormat));
            }

            store.setFuel(readInt(in));
            store.setMissiles(readInt(in));
            store.setDroneParts(readInt(in));
            beacon.setStore(store);
        }

        return beacon;

    }

    public void writeBeacon(OutputStream out, BeaconState beacon, int fileFormat) throws IOException {
        writeInt(out, beacon.getVisitCount());
        if (beacon.getVisitCount() > 0) {
            writeString(out, beacon.getBgStarscapeImageInnerPath());
            writeString(out, beacon.getBgSpriteImageInnerPath());
            writeInt(out, beacon.getBgSpritePosX());
            writeInt(out, beacon.getBgSpritePosY());
            writeInt(out, beacon.getBgSpriteRotation());
        }

        writeBool(out, beacon.isSeen());

        writeBool(out, beacon.isEnemyPresent());
        if (beacon.isEnemyPresent()) {
            writeString(out, beacon.getShipEventId());
            writeString(out, beacon.getAutoBlueprintId());
            writeInt(out, beacon.getShipEventSeed());
        }

        FleetPresence fleetPresence = beacon.getFleetPresence();
        if (fleetPresence == FleetPresence.NONE) writeInt(out, 0);
        else if (fleetPresence == FleetPresence.REBEL) writeInt(out, 1);
        else if (fleetPresence == FleetPresence.FEDERATION) writeInt(out, 2);
        else if (fleetPresence == FleetPresence.BOTH) writeInt(out, 3);
        else throw new RuntimeException("Unknown fleet presence: " + fleetPresence);

        writeBool(out, beacon.isUnderAttack());

        boolean storePresent = (beacon.getStore() != null);
        writeBool(out, storePresent);

        if (storePresent) {
            StoreState store = beacon.getStore();

            if (fileFormat == 2) {
                // FTL 1.01-1.03.3 always had two shelves.

                int shelfLimit = 2;
                int shelfCount = Math.min(store.getShelfList().size(), shelfLimit);
                for (int i = 0; i < shelfCount; i++) {
                    writeStoreShelf(out, store.getShelfList().get(i), fileFormat);
                }
                for (int i = 0; i < shelfLimit - shelfCount; i++) {
                    StoreShelf dummyShelf = new StoreShelf();
                    writeStoreShelf(out, dummyShelf, fileFormat);
                }
            } else if (fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
                // FTL 1.5.4+ requires at least one shelf.
                int shelfReq = 1;

                List<StoreShelf> pendingShelves = new ArrayList<StoreShelf>();
                pendingShelves.addAll(store.getShelfList());

                while (pendingShelves.size() < shelfReq) {
                    StoreShelf dummyShelf = new StoreShelf();
                    pendingShelves.add(dummyShelf);
                }

                writeInt(out, pendingShelves.size());

                for (StoreShelf shelf : pendingShelves) {
                    writeStoreShelf(out, shelf, fileFormat);
                }
            }

            writeInt(out, store.getFuel());
            writeInt(out, store.getMissiles());
            writeInt(out, store.getDroneParts());
        }
    }

    private StoreShelf readStoreShelf(InputStream in, int fileFormat) throws IOException {
        StoreShelf shelf = new StoreShelf();

        int itemType = readInt(in);
        switch (itemType) {
            case 0:
                shelf.setItemType(StoreItemType.WEAPON);
                break;
            case 1:
                shelf.setItemType(StoreItemType.DRONE);
                break;
            case 2:
                shelf.setItemType(StoreItemType.AUGMENT);
                break;
            case 3:
                shelf.setItemType(StoreItemType.CREW);
                break;
            case 4:
                shelf.setItemType(StoreItemType.SYSTEM);
                break;
            default:
                throw new RuntimeException("Unknown store item type: " + itemType);
        }

        for (int i = 0; i < 3; i++) {
            int available = readInt(in); // -1=no item, 0=bought already, 1=buyable
            if (available < 0)
                continue;

            StoreItem item = new StoreItem(readString(in));
            item.setAvailable((available > 0));

            if (fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
                item.setExtraData(readInt(in));
            }

            shelf.addItem(item);
        }

        return shelf;
    }

    public void writeStoreShelf(OutputStream out, StoreShelf shelf, int fileFormat) throws IOException {
        StoreItemType itemType = shelf.getItemType();
        if (itemType == StoreItemType.WEAPON) writeInt(out, 0);
        else if (itemType == StoreItemType.DRONE) writeInt(out, 1);
        else if (itemType == StoreItemType.AUGMENT) writeInt(out, 2);
        else if (itemType == StoreItemType.CREW) writeInt(out, 3);
        else if (itemType == StoreItemType.SYSTEM) writeInt(out, 4);
        else throw new RuntimeException("Unknown store item type: " + itemType);

        List<StoreItem> items = shelf.getItems();
        for (int i = 0; i < 3; i++) {  // TODO: Magic number.
            if (items.size() > i) {
                StoreItem item = items.get(i);

                int available = (item.isAvailable() ? 1 : 0);
                writeInt(out, available);
                writeString(out, item.getItemId());

                if (fileFormat == 8 || fileFormat == 9 || fileFormat == 11) {
                    writeInt(out, item.getExtraData());
                }
            } else {
                writeInt(out, -1);  // No item.
            }
        }
    }

    public EncounterState readEncounter(InputStream in, int fileFormat) throws IOException {
        EncounterState encounter = new EncounterState();

        encounter.setShipEventSeed(readInt(in));
        encounter.setSurrenderEventId(readString(in));
        encounter.setEscapeEventId(readString(in));
        encounter.setDestroyedEventId(readString(in));
        encounter.setDeadCrewEventId(readString(in));
        encounter.setGotAwayEventId(readString(in));

        encounter.setLastEventId(readString(in));

        if (fileFormat == 11) {
            encounter.setUnknownAlpha(readInt(in));
        }

        encounter.setText(readString(in));
        encounter.setAffectedCrewSeed(readInt(in));

        int choiceCount = readInt(in);
        List<Integer> choiceList = new ArrayList<Integer>();
        for (int i = 0; i < choiceCount; i++) {
            choiceList.add(readInt(in));
        }
        encounter.setChoiceList(choiceList);

        return encounter;
    }

    public void writeEncounter(OutputStream out, EncounterState encounter, int fileFormat) throws IOException {
        writeInt(out, encounter.getShipEventSeed());
        writeString(out, encounter.getSurrenderEventId());
        writeString(out, encounter.getEscapeEventId());
        writeString(out, encounter.getDestroyedEventId());
        writeString(out, encounter.getDeadCrewEventId());
        writeString(out, encounter.getGotAwayEventId());

        writeString(out, encounter.getLastEventId());

        if (fileFormat == 11) {
            writeInt(out, encounter.getUnknownAlpha());
        }

        writeString(out, encounter.getText());
        writeInt(out, encounter.getAffectedCrewSeed());

        writeInt(out, encounter.getChoiceList().size());
        for (Integer choiceInt : encounter.getChoiceList()) {
            writeInt(out, choiceInt);
        }
    }

    private NearbyShipAIState readNearbyShipAI(FileInputStream in) throws IOException {
        NearbyShipAIState ai = new NearbyShipAIState();

        ai.setSurrendered(readBool(in));
        ai.setEscaping(readBool(in));
        ai.setDestroyed(readBool(in));
        ai.setSurrenderThreshold(readInt(in));
        ai.setEscapeThreshold(readInt(in));
        ai.setEscapeTicks(readInt(in));
        ai.setStalemateTriggered(readBool(in));
        ai.setStalemateTicks(readInt(in));
        ai.setBoardingAttempts(readInt(in));
        ai.setBoardersNeeded(readInt(in));

        return ai;
    }

    public void writeNearbyShipAI(OutputStream out, NearbyShipAIState ai) throws IOException {
        writeBool(out, ai.hasSurrendered());
        writeBool(out, ai.isEscaping());
        writeBool(out, ai.isDestroyed());
        writeInt(out, ai.getSurrenderThreshold());
        writeInt(out, ai.getEscapeThreshold());
        writeInt(out, ai.getEscapeTicks());
        writeBool(out, ai.isStalemateTriggered());
        writeInt(out, ai.getStalemateTicks());
        writeInt(out, ai.getBoardingAttempts());
        writeInt(out, ai.getBoardersNeeded());
    }

    private EnvironmentState readEnvironment(FileInputStream in) throws IOException {
        EnvironmentState env = new EnvironmentState();

        env.setRedGiantPresent(readBool(in));
        env.setPulsarPresent(readBool(in));
        env.setPDSPresent(readBool(in));

        int vulnFlag = readInt(in);
        HazardVulnerability vuln = null;
        if (vulnFlag == 0) {
            vuln = HazardVulnerability.PLAYER_SHIP;
        } else if (vulnFlag == 1) {
            vuln = HazardVulnerability.NEARBY_SHIP;
        } else if (vulnFlag == 2) {
            vuln = HazardVulnerability.BOTH_SHIPS;
        } else {
            throw new IOException(String.format("Unsupported environment vulnerability flag: %d", vulnFlag));
        }
        env.setVulnerableShips(vuln);

        boolean asteroidsPresent = readBool(in);
        if (asteroidsPresent) {
            AsteroidFieldState asteroidField = new AsteroidFieldState();
            asteroidField.setUnknownAlpha(readInt(in));
            asteroidField.setStrayRockTicks(readInt(in));
            asteroidField.setUnknownGamma(readInt(in));
            asteroidField.setBgDriftTicks(readInt(in));
            asteroidField.setCurrentTarget(readInt(in));
            env.setAsteroidField(asteroidField);
        }
        env.setSolarFlareFadeTicks(readInt(in));
        env.setHavocTicks(readInt(in));
        env.setPDSTicks(readInt(in));

        return env;
    }

    public void writeEnvironment(OutputStream out, EnvironmentState env) throws IOException {
        writeBool(out, env.isRedGiantPresent());
        writeBool(out, env.isPulsarPresent());
        writeBool(out, env.isPDSPresent());

        int vulnFlag = 0;
        if (env.getVulnerableShips() == HazardVulnerability.PLAYER_SHIP) {
            vulnFlag = 0;
        } else if (env.getVulnerableShips() == HazardVulnerability.NEARBY_SHIP) {
            vulnFlag = 1;
        } else if (env.getVulnerableShips() == HazardVulnerability.BOTH_SHIPS) {
            vulnFlag = 2;
        } else {
            throw new IOException(String.format("Unsupported environment vulnerability: %s", env.getVulnerableShips().toString()));
        }
        writeInt(out, vulnFlag);

        boolean asteroidsPresent = (env.getAsteroidField() != null);
        writeBool(out, asteroidsPresent);
        if (asteroidsPresent) {
            AsteroidFieldState asteroidField = env.getAsteroidField();
            writeInt(out, asteroidField.getUnknownAlpha());
            writeInt(out, asteroidField.getStrayRockTicks());
            writeInt(out, asteroidField.getUnknownGamma());
            writeInt(out, asteroidField.getBgDriftTicks());
            writeInt(out, asteroidField.getCurrentTarget());
        }

        writeInt(out, env.getSolarFlareFadeTicks());
        writeInt(out, env.getHavocTicks());
        writeInt(out, env.getPDSTicks());
    }

    public RebelFlagshipState readRebelFlagship(InputStream in) throws IOException {
        RebelFlagshipState flagship = new RebelFlagshipState();

        flagship.setPendingStage(readInt(in));

        int previousRoomCount = readInt(in);
        for (int i = 0; i < previousRoomCount; i++) {
            flagship.setPreviousOccupancy(i, readInt(in));
        }

        return flagship;
    }

    public void writeRebelFlagship(OutputStream out, RebelFlagshipState flagship) throws IOException {
        writeInt(out, flagship.getPendingStage());

        writeInt(out, flagship.getOccupancyMap().size());
        for (Map.Entry<Integer, Integer> entry : flagship.getOccupancyMap().entrySet()) {
            int occupantCount = entry.getValue();
            writeInt(out, occupantCount);
        }
    }

    public AnimState readAnim(InputStream in) throws IOException {
        AnimState anim = new AnimState();

        anim.setPlaying(readBool(in));
        anim.setLooping(readBool(in));
        anim.setCurrentFrame(readInt(in));
        anim.setProgressTicks(readInt(in));
        anim.setScale(readInt(in));
        anim.setX(readInt(in));
        anim.setY(readInt(in));

        return anim;
    }

    public void writeAnim(OutputStream out, AnimState anim) throws IOException {
        writeBool(out, anim.isPlaying());
        writeBool(out, anim.isLooping());
        writeInt(out, anim.getCurrentFrame());
        writeInt(out, anim.getProgressTicks());
        writeInt(out, anim.getScale());
        writeInt(out, anim.getX());
        writeInt(out, anim.getY());
    }

    private ProjectileState readProjectile(FileInputStream in, int fileFormat) throws IOException {
        //log.debug( String.format( "Projectile: @%d", in.getChannel().position() ) );

        ProjectileState projectile = new ProjectileState();

        int projectileTypeFlag = readInt(in);
        if (projectileTypeFlag == 0) {
            projectile.setProjectileType(ProjectileType.INVALID);
            return projectile;  // No other fields are set for invalid projectiles.
        } else if (projectileTypeFlag == 1) {
            projectile.setProjectileType(ProjectileType.LASER_OR_BURST);
        } else if (projectileTypeFlag == 2) {
            projectile.setProjectileType(ProjectileType.ROCK_OR_EXPLOSION);
        } else if (projectileTypeFlag == 3) {
            projectile.setProjectileType(ProjectileType.MISSILE);
        } else if (projectileTypeFlag == 4) {
            projectile.setProjectileType(ProjectileType.BOMB);
        } else if (projectileTypeFlag == 5) {
            projectile.setProjectileType(ProjectileType.BEAM);
        } else if (projectileTypeFlag == 6 && fileFormat == 11) {
            projectile.setProjectileType(ProjectileType.PDS);
        } else {
            throw new IOException(String.format("Unsupported projectileType flag: %d", projectileTypeFlag));
        }

        projectile.setCurrentPositionX(readInt(in));
        projectile.setCurrentPositionY(readInt(in));
        projectile.setPreviousPositionX(readInt(in));
        projectile.setPreviousPositionY(readInt(in));
        projectile.setSpeed(readInt(in));
        projectile.setGoalPositionX(readInt(in));
        projectile.setGoalPositionY(readInt(in));
        projectile.setHeading(readInt(in));
        projectile.setOwnerId(readInt(in));
        projectile.setSelfId(readInt(in));

        projectile.setDamage(readDamage(in));

        projectile.setLifespan(readInt(in));
        projectile.setDestinationSpace(readInt(in));
        projectile.setCurrentSpace(readInt(in));
        projectile.setTargetId(readInt(in));
        projectile.setDead(readBool(in));
        projectile.setDeathAnimId(readString(in));
        projectile.setFlightAnimId(readString(in));

        projectile.setDeathAnim(readAnim(in));
        projectile.setFlightAnim(readAnim(in));

        projectile.setVelocityX(readInt(in));
        projectile.setVelocityY(readInt(in));
        projectile.setMissed(readBool(in));
        projectile.setHitTarget(readBool(in));
        projectile.setHitSolidSound(readString(in));
        projectile.setHitShieldSound(readString(in));
        projectile.setMissSound(readString(in));
        projectile.setEntryAngle(readMinMaxedInt(in));
        projectile.setStartedDying(readBool(in));
        projectile.setPassedTarget(readBool(in));

        projectile.setType(readInt(in));
        projectile.setBroadcastTarget(readBool(in));

        ExtendedProjectileInfo extendedInfo = null;
        if (ProjectileType.LASER_OR_BURST.equals(projectile.getProjectileType())) {
            // Laser/Burst (2).
            // Usually getType() is 4 for Laser, 2 for Burst.
            extendedInfo = readLaserProjectileInfo(in);
        } else if (ProjectileType.ROCK_OR_EXPLOSION.equals(projectile.getProjectileType())) {
            // Explosion/Asteroid (0).
            // getType() is always 2?
            extendedInfo = new EmptyProjectileInfo();
        } else if (ProjectileType.MISSILE.equals(projectile.getProjectileType())) {
            // Missile (0).
            // getType() is always 1?
            extendedInfo = new EmptyProjectileInfo();
        } else if (ProjectileType.BOMB.equals(projectile.getProjectileType())) {
            // Bomb (5).
            // getType() is always 5?
            extendedInfo = readBombProjectileInfo(in);
        } else if (ProjectileType.BEAM.equals(projectile.getProjectileType())) {
            // Beam (25).
            // getType() is always 5?
            extendedInfo = readBeamProjectileInfo(in);
        } else if (ProjectileType.PDS.equals(projectile.getProjectileType())) {
            // PDS (12)
            // getType() is always 5?
            extendedInfo = readPDSProjectileInfo(in);
        }
        projectile.setExtendedInfo(extendedInfo);

        return projectile;
    }

    public void writeProjectile(OutputStream out, ProjectileState projectile, int fileFormat) throws IOException {

        int projectileTypeFlag = 0;
        if (ProjectileType.INVALID.equals(projectile.getProjectileType())) {
            projectileTypeFlag = 0;
        } else if (ProjectileType.LASER_OR_BURST.equals(projectile.getProjectileType())) {
            projectileTypeFlag = 1;
        } else if (ProjectileType.ROCK_OR_EXPLOSION.equals(projectile.getProjectileType())) {
            projectileTypeFlag = 2;
        } else if (ProjectileType.MISSILE.equals(projectile.getProjectileType())) {
            projectileTypeFlag = 3;
        } else if (ProjectileType.BOMB.equals(projectile.getProjectileType())) {
            projectileTypeFlag = 4;
        } else if (ProjectileType.BEAM.equals(projectile.getProjectileType())) {
            projectileTypeFlag = 5;
        } else if (ProjectileType.PDS.equals(projectile.getProjectileType()) && fileFormat == 11) {
            projectileTypeFlag = 6;
        } else {
            throw new IOException(String.format("Unsupported projectileType: %s", projectile.getProjectileType().toString()));
        }
        writeInt(out, projectileTypeFlag);

        if (ProjectileType.INVALID.equals(projectile.getProjectileType())) {
            return;  // No other fields are set for invalid projectiles.
        }

        writeInt(out, projectile.getCurrentPositionX());
        writeInt(out, projectile.getCurrentPositionY());
        writeInt(out, projectile.getPreviousPositionX());
        writeInt(out, projectile.getPreviousPositionY());
        writeInt(out, projectile.getSpeed());
        writeInt(out, projectile.getGoalPositionX());
        writeInt(out, projectile.getGoalPositionY());
        writeInt(out, projectile.getHeading());
        writeInt(out, projectile.getOwnerId());
        writeInt(out, projectile.getSelfId());

        writeDamage(out, projectile.getDamage());

        writeInt(out, projectile.getLifespan());
        writeInt(out, projectile.getDestinationSpace());
        writeInt(out, projectile.getCurrentSpace());
        writeInt(out, projectile.getTargetId());
        writeBool(out, projectile.isDead());
        writeString(out, projectile.getDeathAnimId());
        writeString(out, projectile.getFlightAnimId());

        writeAnim(out, projectile.getDeathAnim());
        writeAnim(out, projectile.getFlightAnim());

        writeInt(out, projectile.getVelocityX());
        writeInt(out, projectile.getVelocityY());
        writeBool(out, projectile.hasMissed());
        writeBool(out, projectile.hasHitTarget());
        writeString(out, projectile.getHitSolidSound());
        writeString(out, projectile.getHitShieldSound());
        writeString(out, projectile.getMissSound());
        writeMinMaxedInt(out, projectile.getEntryAngle());
        writeBool(out, projectile.hasStartedDying());
        writeBool(out, projectile.hasPassedTarget());

        writeInt(out, projectile.getType());
        writeBool(out, projectile.getBroadcastTarget());

        ExtendedProjectileInfo extendedInfo = projectile.getExtendedInfo(ExtendedProjectileInfo.class);
        if (extendedInfo instanceof IntegerProjectileInfo) {
            IntegerProjectileInfo intInfo = projectile.getExtendedInfo(IntegerProjectileInfo.class);
            for (int i = 0; i < intInfo.getSize(); i++) {
                writeMinMaxedInt(out, intInfo.get(i));
            }
        } else if (extendedInfo instanceof BeamProjectileInfo) {
            writeBeamProjectileInfo(out, projectile.getExtendedInfo(BeamProjectileInfo.class));
        } else if (extendedInfo instanceof BombProjectileInfo) {
            writeBombProjectileInfo(out, projectile.getExtendedInfo(BombProjectileInfo.class));
        } else if (extendedInfo instanceof LaserProjectileInfo) {
            writeLaserProjectileInfo(out, projectile.getExtendedInfo(LaserProjectileInfo.class));
        } else if (extendedInfo instanceof PDSProjectileInfo) {
            writePDSProjectileInfo(out, projectile.getExtendedInfo(PDSProjectileInfo.class));
        } else if (extendedInfo instanceof EmptyProjectileInfo) {
            // No-op.
        } else {
            throw new IOException("Unsupported extended projectile info: " + extendedInfo.getClass().getSimpleName());
        }
    }

    public DamageState readDamage(InputStream in) throws IOException {
        DamageState damage = new DamageState();

        damage.setHullDamage(readInt(in));
        damage.setShieldPiercing(readInt(in));
        damage.setFireChance(readInt(in));
        damage.setBreachChance(readInt(in));
        damage.setIonDamage(readInt(in));
        damage.setSystemDamage(readInt(in));
        damage.setPersonnelDamage(readInt(in));
        damage.setHullBuster(readBool(in));
        damage.setOwnerId(readInt(in));
        damage.setSelfId(readInt(in));
        damage.setLockdown(readBool(in));
        damage.setCrystalShard(readBool(in));
        damage.setStunChance(readInt(in));
        damage.setStunAmount(readInt(in));

        return damage;
    }

    public void writeDamage(OutputStream out, DamageState damage) throws IOException {
        writeInt(out, damage.getHullDamage());
        writeInt(out, damage.getShieldPiercing());
        writeInt(out, damage.getFireChance());
        writeInt(out, damage.getBreachChance());
        writeInt(out, damage.getIonDamage());
        writeInt(out, damage.getSystemDamage());
        writeInt(out, damage.getPersonnelDamage());
        writeBool(out, damage.isHullBuster());
        writeInt(out, damage.getOwnerId());
        writeInt(out, damage.getSelfId());
        writeBool(out, damage.isLockdown());
        writeBool(out, damage.isCrystalShard());
        writeInt(out, damage.getStunChance());
        writeInt(out, damage.getStunAmount());
    }

    private BeamProjectileInfo readBeamProjectileInfo(FileInputStream in) throws IOException {
        BeamProjectileInfo beamInfo = new BeamProjectileInfo();

        beamInfo.setEmissionEndX(readInt(in));
        beamInfo.setEmissionEndY(readInt(in));
        beamInfo.setStrafeSourceX(readInt(in));
        beamInfo.setStrafeSourceY(readInt(in));

        beamInfo.setStrafeEndX(readInt(in));
        beamInfo.setStrafeEndY(readInt(in));
        beamInfo.setUnknownBetaX(readInt(in));
        beamInfo.setUnknownBetaY(readInt(in));

        beamInfo.setSwathEndX(readInt(in));
        beamInfo.setSwathEndY(readInt(in));
        beamInfo.setSwathStartX(readInt(in));
        beamInfo.setSwathStartY(readInt(in));

        beamInfo.setUnknownGamma(readInt(in));
        beamInfo.setSwathLength(readInt(in));
        beamInfo.setUnknownDelta(readInt(in));

        beamInfo.setUnknownEpsilonX(readInt(in));
        beamInfo.setUnknownEpsilonY(readInt(in));

        beamInfo.setUnknownZeta(readInt(in));
        beamInfo.setUnknownEta(readInt(in));
        beamInfo.setEmissionAngle(readInt(in));

        beamInfo.setUnknownIota(readBool(in));
        beamInfo.setUnknownKappa(readBool(in));
        beamInfo.setFromDronePod(readBool(in));
        beamInfo.setUnknownMu(readBool(in));
        beamInfo.setUnknownNu(readBool(in));

        return beamInfo;
    }

    public void writeBeamProjectileInfo(OutputStream out, BeamProjectileInfo beamInfo) throws IOException {
        writeInt(out, beamInfo.getEmissionEndX());
        writeInt(out, beamInfo.getEmissionEndY());
        writeInt(out, beamInfo.getStrafeSourceX());
        writeInt(out, beamInfo.getStrafeSourceY());

        writeInt(out, beamInfo.getStrafeEndX());
        writeInt(out, beamInfo.getStrafeEndY());
        writeInt(out, beamInfo.getUnknownBetaX());
        writeInt(out, beamInfo.getUnknownBetaY());

        writeInt(out, beamInfo.getSwathEndX());
        writeInt(out, beamInfo.getSwathEndY());
        writeInt(out, beamInfo.getSwathStartX());
        writeInt(out, beamInfo.getSwathStartY());

        writeInt(out, beamInfo.getUnknownGamma());
        writeInt(out, beamInfo.getSwathLength());
        writeInt(out, beamInfo.getUnknownDelta());

        writeInt(out, beamInfo.getUnknownEpsilonX());
        writeInt(out, beamInfo.getUnknownEpsilonY());

        writeInt(out, beamInfo.getUnknownZeta());
        writeInt(out, beamInfo.getUnknownEta());
        writeInt(out, beamInfo.getEmissionAngle());

        writeBool(out, beamInfo.getUnknownIota());
        writeBool(out, beamInfo.getUnknownKappa());
        writeBool(out, beamInfo.isFromDronePod());
        writeBool(out, beamInfo.getUnknownMu());
        writeBool(out, beamInfo.getUnknownNu());
    }

    private BombProjectileInfo readBombProjectileInfo(FileInputStream in) throws IOException {
        BombProjectileInfo bombInfo = new BombProjectileInfo();

        bombInfo.setUnknownAlpha(readInt(in));
        bombInfo.setFuseTicks(readInt(in));
        bombInfo.setUnknownGamma(readInt(in));
        bombInfo.setUnknownDelta(readInt(in));
        bombInfo.setArrived(readBool(in));

        return bombInfo;
    }

    public void writeBombProjectileInfo(OutputStream out, BombProjectileInfo bombInfo) throws IOException {
        writeInt(out, bombInfo.getUnknownAlpha());
        writeInt(out, bombInfo.getFuseTicks());
        writeInt(out, bombInfo.getUnknownGamma());
        writeInt(out, bombInfo.getUnknownDelta());
        writeBool(out, bombInfo.hasArrived());
    }

    private LaserProjectileInfo readLaserProjectileInfo(FileInputStream in) throws IOException {
        LaserProjectileInfo laserInfo = new LaserProjectileInfo();

        laserInfo.setUnknownAlpha(readInt(in));
        laserInfo.setSpin(readInt(in));

        return laserInfo;
    }

    public void writeLaserProjectileInfo(OutputStream out, LaserProjectileInfo laserInfo) throws IOException {
        writeInt(out, laserInfo.getUnknownAlpha());
        writeInt(out, laserInfo.getSpin());
    }

    private PDSProjectileInfo readPDSProjectileInfo(FileInputStream in) throws IOException {
        PDSProjectileInfo pdsInfo = new PDSProjectileInfo();

        pdsInfo.setUnknownAlpha(readInt(in));
        pdsInfo.setUnknownBeta(readInt(in));
        pdsInfo.setUnknownGamma(readInt(in));
        pdsInfo.setUnknownDelta(readInt(in));
        pdsInfo.setUnknownEpsilon(readInt(in));
        pdsInfo.setUnknownZeta(readAnim(in));

        return pdsInfo;
    }

    public void writePDSProjectileInfo(OutputStream out, PDSProjectileInfo pdsInfo) throws IOException {
        writeInt(out, pdsInfo.getUnknownAlpha());
        writeInt(out, pdsInfo.getUnknownBeta());
        writeInt(out, pdsInfo.getUnknownGamma());
        writeInt(out, pdsInfo.getUnknownDelta());
        writeInt(out, pdsInfo.getUnknownEpsilon());
        writeAnim(out, pdsInfo.getUnknownZeta());
    }


    /**
     * Counters used for event criteria and achievements.
     * <p>
     * FTL 1.5.4 introduced HIGH_O2 and SUFFOCATED_CREW.
     */
    public static enum StateVar {
        // TODO: Magic strings.
        BLUE_ALIEN("blue_alien", "Blue event choices clicked. (Only ones that require a race.)"),
        DEAD_CREW("dead_crew", "Ships defeated by killing all enemy crew."),
        DESTROYED_ROCK("destroyed_rock", "Rock ships destroyed, including pirates."),
        ENV_DANGER("env_danger", "Jumps into beacons with environmental dangers."),
        FIRED_SHOT("fired_shot", "Individual beams/blasts/projectiles fired. (See also: used_missile)"),
        HIGH_O2("higho2", "Times oxygen exceeded 20%, incremented when arriving at a beacon. (Bug: Or loading in FTL 1.5.4-1.5.10)"),
        KILLED_CREW("killed_crew", "Enemy crew killed. (And possibly friendly fire?)"),
        LOST_CREW("lost_crew", "Crew you've lost: killed, abandoned on nearby ships, taken by events?, but not dismissed. Even if cloned later. (See also: dead_crew)"),
        NEBULA("nebula", "Jumps into nebula beacons."),
        OFFENSIVE_DRONE("offensive_drone", "The number of times drones capable of damaging an enemy ship powered up."),
        REACTOR_UPGRADE("reactor_upgrade", "Reactor (power bar) upgrades beyond the ship's default levels."),
        STORE_PURCHASE("store_purchase", "Non-repair purchases, such as crew/items. (Selling isn't counted.)"),
        STORE_REPAIR("store_repair", "Store repair button clicks."),
        SUFFOCATED_CREW("suffocated_crew", "???"),
        SYSTEM_UPGRADE("system_upgrade", "System (and subsystem; not reactor) upgrades beyond the ship's default levels."),
        TELEPORTED("teleported", "Teleporter activations, in either direction."),
        USED_DRONE("used_drone", "The number of times drone parts were consumed."),
        USED_MISSILE("used_missile", "Missile/bomb weapon discharges. (See also: fired_shot)"),
        WEAPON_UPGRADE("weapon_upgrade", "Weapons system upgrades beyond the ship's default levels. (See also: system_upgrade)");

        private String id;
        private String description;

        private StateVar(String id, String description) {
            this.id = id;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public String toString() {
            return id;
        }

        public static StateVar findById(String id) {
            for (StateVar v : values()) {
                if (v.getId().equals(id)) return v;
            }
            return null;
        }

        public static String getDescription(String id) {
            StateVar v = StateVar.findById(id);
            if (v != null) return v.getDescription();
            return id + " is an unknown var. Please report it on the forum thread.";
        }
    }






    public static class StartingCrewState {

        private String name = "Frank";
        private CrewType race = CrewType.HUMAN;


        public StartingCrewState() {
        }

        public void setName(String s) {
            name = s;
        }

        public String getName() {
            return name;
        }

        public void setRace(CrewType race) {
            this.race = race;
        }

        public CrewType getRace() {
            return race;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append(String.format("Name: %s%n", name));
            result.append(String.format("Race: %s%n", race.getId()));
            return result.toString();
        }
    }


    public static class SystemState {
        private SystemType systemType;
        private int capacity = 0;
        private int power = 0;
        private int damagedBars = 0;
        private int ionizedBars = 0;
        private int repairProgress = 0;
        private int damageProgress = 0;
        private int deionizationTicks = Integer.MIN_VALUE;

        private int batteryPower = 0;
        private int hackLevel = 0;
        private boolean hacked = false;
        private int temporaryCapacityCap = 1000;
        private int temporaryCapacityLoss = 0;
        private int temporaryCapacityDivisor = 1;


        /**
         * Constructor.
         */
        public SystemState(SystemType systemType) {
            this.systemType = systemType;
        }

        /**
         * Copy constructor.
         */
        public SystemState(SystemState srcSystem) {
            systemType = srcSystem.getSystemType();
            capacity = srcSystem.getCapacity();
            power = srcSystem.getPower();
            damagedBars = srcSystem.getDamagedBars();
            ionizedBars = srcSystem.getIonizedBars();
            repairProgress = srcSystem.getRepairProgress();
            damageProgress = srcSystem.getDamageProgress();
            deionizationTicks = srcSystem.getDeionizationTicks();
            batteryPower = srcSystem.getBatteryPower();
            hackLevel = srcSystem.getHackLevel();
            hacked = srcSystem.isHacked();
            temporaryCapacityCap = srcSystem.getTemporaryCapacityCap();
            temporaryCapacityLoss = srcSystem.getTemporaryCapacityLoss();
            temporaryCapacityDivisor = srcSystem.getTemporaryCapacityDivisor();
        }

        /**
         * Resets aspects of an existing object to be viable for player use.
         * <p>
         * This will be called by the ship object when it is commandeered.
         * <p>
         * Warning: Dangerous while values remain undeciphered.
         */
        public void commandeer() {
            if (!systemType.isSubsystem()) {
                setPower(0);
            }
            // TODO: Find out if NOT resetting subsystem power is okay.
            // Otherwise, damage, etc. will need to be taken into account.

            setBatteryPower(0);
            setHackLevel(0);
            setHacked(false);
            setTemporaryCapacityCap(1000);
            setTemporaryCapacityLoss(0);
            setTemporaryCapacityDivisor(1);
        }

        public SystemType getSystemType() {
            return systemType;
        }

        /**
         * Sets the number of power bars this system can use.
         * <p>
         * A capacity of zero means the system is not currently installed.
         */
        public void setCapacity(int n) {
            capacity = n;
        }

        public int getCapacity() {
            return capacity;
        }

        /**
         * Sets the number of reserve power bars assigned to this system.
         * <p>
         * Power bars appear at the bottom of the stack.
         * <p>
         * Note: For Weapons and DroneCtrl systems, this value ideally would
         * be set whenever a weapon/drone is armed/disarmed and vice versa.
         * <p>
         * FTL seems to recalculate Weapons system power upon loading, so this
         * value can be sloppily set to 0 or ignored while editing.
         *
         * @see #setBatteryPower(int)
         */
        public void setPower(int n) {
            power = n;
        }

        public int getPower() {
            return power;
        }

        /**
         * Sets the number of unusable power bars, in need of repair.
         * <p>
         * Damaged bars appear at the top of the stack, painted over capacity
         * limit bars.
         */
        public void setDamagedBars(int n) {
            damagedBars = n;
        }

        public int getDamagedBars() {
            return damagedBars;
        }

        /**
         * Sets the number of ionized power bars.
         * <p>
         * In-game when N ion damage is applied, up to N power/battery bars are
         * deallocated. Any others remain to power the system, but for a short
         * time, transfers to/from that system will not be possible.
         * <p>
         * A countdown will repeat N times, decrementing this value to 0. N may
         * exceed the total number of power bars on a system, to increase the
         * total time required to remove all the 'ionized bars'.
         * <p>
         * This should be less than or equal to MAX_IONIZED_BARS, or FTL's
         * interface will be unable to find an image to display the number, and
         * a warning graphic will appear.
         * <p>
         * When a system disables itself (white lock), this will be -1. For
         * the Cloaking system in FTL 1.01-1.03.3, setting this to -1 would
         * engage the cloak. Systems which do not normally disable themselves
         * will remain locked until they get hit with a weapon that produces
         * ion damage. See ExtendedSystemInfo classes for timer fields that
         * might used to unlock systems on their own.
         * <p>
         * TODO: Teleporter has not been tested. AE systems have not been
         * tested.
         *
         * @see net.blerf.ftl.constants.FTLConstants#getMaxIonizedBars()
         */
        public void setIonizedBars(int n) {
            ionizedBars = n;
        }

        public int getIonizedBars() {
            return ionizedBars;
        }

        /**
         * Sets progress toward repairing one damaged power bar.
         * <p>
         * A growing portion of the bottommost damaged bar will turn yellow.
         * <p>
         * Note: Repair progress and damage progress can both be non-zero at the
         * same time. They affect different bars.
         *
         * @param n 0-100 (0 when not repairing)
         */
        public void setRepairProgress(int n) {
            repairProgress = n;
        }

        public int getRepairProgress() {
            return repairProgress;
        }

        /**
         * Sets progress toward damaging one power bar.
         * <p>
         * A growing portion of the topmost empty/energy/battery/power bar will
         * turn red.
         * <p>
         * This is typically caused by fire or boarders attempting sabotage.
         * <p>
         * Note: Repair progress and damage progress can both be non-zero at the
         * same time. They affect different bars.
         *
         * @param n 0-100 (0 when not damaging)
         */
        public void setDamageProgress(int n) {
            damageProgress = n;
        }

        public int getDamageProgress() {
            return damageProgress;
        }

        /**
         * Sets elapsed time while waiting to remove each ionized power bar.
         * <p>
         * The system is inoperative while any ionized bars remain, and any
         * power assigned will be unavailable. If this system is using battery
         * power, and the battery deactivates, a lock countdown will complete
         * immediately (but not a plain ion countdown).
         * <p>
         * The game's interface responds as this increments, including
         * resetting after intervals. If not needed, it may be 0, or
         * more often, MIN_INT.
         * <p>
         * It was thought that in FTL 1.01-1.03.3, deionization of each bar
         * counted to 5000. In FTL 1.5.13, it was observed at 14407 (with half
         * the circle remaining).
         * <p>
         * TODO:
         * Nearly every system has been observed with non-zero values,
         * but aside from Teleporter/Cloaking, normal use doesn't reliably
         * set such values. Might be unspecified garbage when not actively
         * counting. Sometimes has huge positive and negative values.
         * <p>
         * This value is reset upon loading.
         * (TODO: Check if still true in FTL 1.5.4.)
         *
         * @see #setIonizedBars(int)
         */
        public void setDeionizationTicks(int n) {
            deionizationTicks = n;
        }

        public int getDeionizationTicks() {
            return deionizationTicks;
        }


        /**
         * Sets the number of battery power bars assigned to this system.
         * <p>
         * Battery bars have an orange border and will appear above reserve
         * power bars in the stack. When the battery system is fully discharged,
         * they will be lost, but spare reserve power at that moment will be
         * allocated to replace them.
         * <p>
         * Note: For Weapons and DroneCtrl systems, this value must be set
         * whenever a weapon/drone is armed/disarmed and vice versa.
         * <p>
         * Note: Whenever this value changes, the ship's Battery extended system
         * info must be updated.
         * <p>
         * This was introduced in FTL 1.5.4.
         *
         * @see #setPower(int)
         * @see BatteryInfo#setUsedBattery(int)
         */
        public void setBatteryPower(int n) {
            batteryPower = n;
        }

        public int getBatteryPower() {
            return batteryPower;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: 0 (no hacking drone pod), 1 (pod passively
         * attached, set on contact), 2 (disrupting).
         * <p>
         * If the hacking system of the other ship is inoperative, this will be
         * set to 0, even though there is still a pod attached.
         * <p>
         * TODO: Revise this description.
         * <p>
         * This was introduced in FTL 1.5.4.
         *
         * @see SavedGameParser#setHacked(int)
         */
        public void setHackLevel(int n) {
            hackLevel = n;
        }

        public int getHackLevel() {
            return hackLevel;
        }

        /**
         * Toggles whether this system has a hacking drone pod attached.
         * <p>
         * This only describes attachment (set the moment the pod makes
         * contact), not disruption.
         * <p>
         * If the hacking system of the other ship is inoperative, this will be
         * set to false, even though there is still a pod attached.
         * <p>
         * This was introduced in FTL 1.5.4.
         *
         * @see #setHackLevel(int)
         */
        public void setHacked(boolean b) {
            hacked = b;
        }

        public boolean isHacked() {
            return hacked;
        }

        /**
         * Sets an upper limit on this system's usable capacity.
         * <p>
         * The effect lasts for the current beacon only, or until reset.
         * <p>
         * In the game's xml resources, the cap value comes from a "status" tag
         * with the "limit=" attribute.
         * <p>
         * Mods are reportedly only capable of using one flavor of capacity
         * limit, but in saved games, they're all set, and the most restrictive
         * one applies.
         * <p>
         * Under normal circumstances, the cap is 1000.
         * At a beacon with a nebula, the Sensors system has cap of 0.
         * <p>
         * This was introduced in FTL 1.5.4.
         *
         * @see #setTemporaryCapacityLoss(int)
         * @see #setTemporaryCapacityDivisor(int)
         */
        public void setTemporaryCapacityCap(int n) {
            temporaryCapacityCap = n;
        }

        public int getTemporaryCapacityCap() {
            return temporaryCapacityCap;
        }

        /**
         * Sets a number to subtract from this system's usable capacity.
         * <p>
         * The effect lasts for the current beacon only, or until reset.
         * <p>
         * In the game's xml resources, the cap value comes from a "status" tag
         * with the "loss=" attribute.
         * <p>
         * Mods are reportedly only capable of using one flavor of capacity
         * limit, but in saved games, they're all set, and the most restrictive
         * one applies.
         * <p>
         * Under normal circumstances, the loss is 0.
         * <p>
         * This was introduced in FTL 1.5.4.
         *
         * @see #setTemporaryCapacityCap(int)
         * @see #setTemporaryCapacityDivisor(int)
         */
        public void setTemporaryCapacityLoss(int n) {
            temporaryCapacityLoss = n;
        }

        public int getTemporaryCapacityLoss() {
            return temporaryCapacityLoss;
        }

        /**
         * Sets a number to divide this system's usable capacity by.
         * <p>
         * The real capacity will be rounded up to the nearest multiple of N
         * before dividing.
         * <p>
         * The effect lasts for the current beacon only, or until reset.
         * <p>
         * In the game's xml resources, the cap value comes from a "status" tag
         * with the "divide=" attribute.
         * <p>
         * Mods are reportedly only capable of using one flavor of capacity
         * limit, but in saved games, they're all set, and the most restrictive
         * one applies.
         * <p>
         * Under normal circumstances, the divisor is 1.
         * <p>
         * This was introduced in FTL 1.5.4.
         *
         * @see #setTemporaryCapacityCap(int)
         * @see #setTemporaryCapacityLoss(int)
         */
        public void setTemporaryCapacityDivisor(int n) {
            temporaryCapacityDivisor = n;
        }

        public int getTemporaryCapacityDivisor() {
            return temporaryCapacityDivisor;
        }


        /**
         * Returns the effective capacity after applying limits (min 0).
         * <p>
         * Damage is not considered.
         *
         * @see #getUsableCapacity()
         */
        public int getLimitedCapacity() {
            int capLimit = temporaryCapacityCap;
            int lossLimit = capacity - temporaryCapacityLoss;
            int divLimit = (capacity + temporaryCapacityDivisor - 1) / temporaryCapacityDivisor;

            int limit = Math.max(0, Math.min(capLimit, Math.min(lossLimit, divLimit)));
            return limit;
        }

        /**
         * Returns the effective capacity after applying limits and damage
         * (min 0).
         * <p>
         * The result is the maximum total power, battery, or zoltan bars.
         *
         * @see #getLimitedCapacity()
         */
        public int getUsableCapacity() {
            int limitedCapacity = getLimitedCapacity();
            int damagedCapacity = capacity - damagedBars;
            return Math.max(0, Math.min(limitedCapacity, damagedCapacity));
        }


        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("SystemId:              %s%n", systemType.getId()));
            if (capacity > 0) {
                result.append(String.format("Capacity:                %3d%n", capacity));
                result.append(String.format("Power:                   %3d%n", power));
                result.append(String.format("Damaged Bars:            %3d%n", damagedBars));
                result.append(String.format("Ionized Bars:            %3d%n", ionizedBars));
                result.append(String.format("Repair Progress:         %3d%%%n", repairProgress));
                result.append(String.format("Damage Progress:         %3d%%%n", damageProgress));
                result.append(String.format("Deionization Ticks:    %5s%n", (deionizationTicks == Integer.MIN_VALUE ? "N/A" : deionizationTicks)));
                result.append(String.format("Battery Power:           %3d%n", batteryPower));
                result.append(String.format("Hack Level:              %3d%n", hackLevel));
                result.append(String.format("Hacked:                %5b%n", hacked));
                result.append(String.format("Temp Capacity Cap:     %5d%n", temporaryCapacityCap));
                result.append(String.format("Temp Capacity Loss:      %3d%n", temporaryCapacityLoss));
                result.append(String.format("Temp Capacity Divisor:   %3d%n", temporaryCapacityDivisor));
            } else {
                result.append(String.format("(Not installed)%n"));
            }

            return result.toString();
        }
    }


    /**
     * The direction crew will face when standing at a system room's terminal.
     */
    public static enum StationDirection {DOWN, RIGHT, UP, LEFT, NONE}

    public static class RoomState {
        private int oxygen = 100;
        private List<SquareState> squareList = new ArrayList<SquareState>();
        private int stationSquare = -1;
        private StationDirection stationDirection = StationDirection.NONE;


        /**
         * Constructs an incomplete RoomState.
         * <p>
         * It will need squares.
         */
        public RoomState() {
        }

        /**
         * Copy constructor.
         * <p>
         * Each SquareState will be copy-constructed as well.
         */
        public RoomState(RoomState srcRoom) {
            oxygen = srcRoom.getOxygen();

            for (SquareState square : srcRoom.getSquareList()) {
                squareList.add(new SquareState(square));
            }

            stationSquare = srcRoom.getStationSquare();
            stationDirection = srcRoom.getStationDirection();
        }

        /**
         * Set's the oxygen percentage in the room.
         * <p>
         * When this is below 5, a warning appears.
         * <p>
         * At 0, the game changes the room's appearance.
         * Since 1.03.1, it paints red stripes on the floor.
         * Before that, it highlighted the walls orange.
         *
         * @param n 0-100
         */
        public void setOxygen(int n) {
            oxygen = n;
        }

        public int getOxygen() {
            return oxygen;
        }

        /**
         * Sets a room square for a station, to man a system.
         * <p>
         * When the system's capacity is 0, this is not set.
         * <p>
         * The station's direction must be set as well.
         * <p>
         * This was introduced in FTL 1.5.4.
         *
         * @param n the room square index, or -1 for none
         */
        public void setStationSquare(int n) {
            stationSquare = n;
        }

        public int getStationSquare() {
            return stationSquare;
        }

        /**
         * Sets which edge of a room square a station should be placed at.
         * <p>
         * When the system's capacity is 0, this is not set.
         * <p>
         * The station's room square must be set as well.
         * <p>
         * This was introduced in FTL 1.5.4.
         *
         * @param n 0=D,1=R,2=U,3=L,4=None
         */
        public void setStationDirection(StationDirection d) {
            stationDirection = d;
        }

        public StationDirection getStationDirection() {
            return stationDirection;
        }


        /**
         * Adds a floor square to the room.
         * <p>
         * Squares are indexed horizontally, left-to-right, wrapping
         * into the next row down.
         */
        public void addSquare(SquareState square) {
            squareList.add(square);
        }

        public SquareState getSquare(int n) {
            return squareList.get(n);
        }

        public List<SquareState> getSquareList() {
            return squareList;
        }


        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("Oxygen: %3d%%%n", oxygen));
            result.append(String.format("Station Square: %2d, Station Direction: %s%n", stationSquare, stationDirection.toString()));

            result.append("Squares...\n");
            for (SquareState square : squareList) {
                result.append(square.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            return result.toString();
        }
    }

    public static class SquareState {
        private int fireHealth = 0;
        private int ignitionProgress = 0;
        private int extinguishmentProgress = -1;


        public SquareState() {
        }

        public SquareState(int fireHealth, int ignitionProgress, int extinguishmentProgress) {
            this.fireHealth = fireHealth;
            this.ignitionProgress = ignitionProgress;
            this.extinguishmentProgress = extinguishmentProgress;
        }

        /**
         * Copy constructor.
         */
        public SquareState(SquareState srcSquare) {
            this.fireHealth = srcSquare.getFireHealth();
            this.ignitionProgress = srcSquare.getIgnitionProgress();
            this.extinguishmentProgress = srcSquare.getExtinguishmentProgress();
        }

        /**
         * Sets the health of a fire in this square, or 0.
         *
         * @param n 0-100
         */
        public void setFireHealth(int n) {
            fireHealth = n;
        }

        public int getFireHealth() {
            return fireHealth;
        }

        /**
         * Sets the square's ignition progress.
         * <p>
         * Squares adjacent to a fire grow closer to igniting as
         * time passes. Then a new fire spawns in them at full health.
         *
         * @param n 0-100
         */
        public void setIgnitionProgress(int n) {
            ignitionProgress = n;
        }

        public int getIgnitionProgress() {
            return ignitionProgress;
        }

        /**
         * Unknown.
         * <p>
         * This is a rapidly decrementing number, as a fire disappears in a puff
         * of smoke. When not set, this is -1.
         * <p>
         * Starving a fire of oxygen does not affect its health.
         * <p>
         * In FTL 1.01-1.5.10 this always seemed to be -1. In FTL 1.5.13, other
         * values were finally observed.
         * <p>
         * Observed values: -1 (almost always), 9,8,7,6,5,2,1,0.
         */
        public void setExtinguishmentProgress(int n) {
            extinguishmentProgress = n;
        }

        public int getExtinguishmentProgress() {
            return extinguishmentProgress;
        }

        @Override
        public String toString() {

            return String.format("Fire HP: %3d, Ignition: %3d%%, Extinguishment: %2d%n", fireHealth, ignitionProgress, extinguishmentProgress);
        }
    }


    public static class DoorState {
        private boolean open = false;
        private boolean walkingThrough = false;

        private int currentMaxHealth = 0;
        private int health = 0;
        private int nominalHealth = 0;
        private int unknownDelta = 0;
        private int unknownEpsilon = 0;


        /**
         * Constructor.
         */
        public DoorState() {
        }

        /**
         * Copy constructor.
         */
        public DoorState(DoorState srcDoor) {
            open = srcDoor.isOpen();
            walkingThrough = srcDoor.isWalkingThrough();
            currentMaxHealth = srcDoor.getCurrentMaxHealth();
            health = srcDoor.getHealth();
            nominalHealth = srcDoor.getNominalHealth();
            unknownDelta = srcDoor.getUnknownDelta();
            unknownEpsilon = srcDoor.getUnknownEpsilon();
        }

        /**
         * Resets aspects of an existing object to be viable for player use.
         * <p>
         * This will be called by the ship object when it is commandeered.
         * <p>
         * Warning: Dangerous while values remain undeciphered.
         */
        public void commandeer() {
            setCurrentMaxHealth(getNominalHealth());
            setHealth(getCurrentMaxHealth());

            setUnknownDelta(0);    // TODO: Vet this default.
            setUnknownEpsilon(0);  // TODO: Vet this default.
        }

        public void setOpen(boolean b) {
            open = b;
        }

        public void setWalkingThrough(boolean b) {
            walkingThrough = b;
        }

        public boolean isOpen() {
            return open;
        }

        public boolean isWalkingThrough() {
            return walkingThrough;
        }


        /**
         * Sets current max door health.
         * <p>
         * This is affected by situational modifiers like Crystal lockdown,
         * but it likely copies the nominal value at some point.
         * <p>
         * This was introduced in FTL 1.5.4.
         */
        public void setCurrentMaxHealth(int n) {
            currentMaxHealth = n;
        }

        public int getCurrentMaxHealth() {
            return currentMaxHealth;
        }

        /**
         * Sets the current door health.
         * <p>
         * Starting at current max, this decreases as someone tries to break it
         * down.
         * <p>
         * TODO: After combat in which a hacking drone boosts the door's health,
         * the current max returns to normal, but the actual health stays high
         * for some reason.
         * <p>
         * This was introduced in FTL 1.5.4.
         */
        public void setHealth(int n) {
            health = n;
        }

        public int getHealth() {
            return health;
        }

        /**
         * Sets nominal max door health.
         * This is the value to which the current max will eventually reset.
         * <p>
         * Observed values:
         * 04 = Level 0 (un-upgraded or damaged Doors system).
         * 08 = Level 1 (???)
         * 12 = Level 2 (confirmed)
         * 16 = Level 3 (confirmed)
         * 20 = Level 4 (Level 3, plus manned; confirmed)
         * 18 = Level 3 (max, plus manned) (or is it 15, 10 while unmanned?)
         * 50 = Lockdown.
         * <p>
         * TODO: The Mantis Basilisk ship's doors went from 4 to 12 when the
         * 1-capacity Doors system was manned. Doors that were already hacked at
         * the time stayed at 16.
         * <p>
         * TODO: Check what the Rock B Ship's doors have (it lacks a Doors
         * system). Damaged system is 4 (hacked doors were still 16).
         * <p>
         * TODO: Investigate why an attached hacking drone adds to ALL THREE
         * healths (set on contact). Observed diffs: 4 to 16.
         * <p>
         * This was introduced in FTL 1.5.4.
         */
        public void setNominalHealth(int n) {
            nominalHealth = n;
        }

        public int getNominalHealth() {
            return nominalHealth;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: 0 (normal), 1 (while level 2 Doors system is
         * unmanned), 1 (while level 1 Doors system is manned), 2 (while level 3
         * Doors system is unmanned), 3 (while level 3 Doors system is manned),
         * 2 (hacking pod passively attached, set on
         * contact). Still 2 while hack-disrupting.
         * <p>
         * This was introduced in FTL 1.5.4.
         */
        public void setUnknownDelta(int n) {
            unknownDelta = n;
        }

        public int getUnknownDelta() {
            return unknownDelta;
        }

        /**
         * Sets hacking drone lockdown status.
         * <p>
         * Observed values:
         * 0 = N/A
         * 1 = Hacking drone pod passively attached.
         * 2 = Hacking drone pod attached and disrupting.
         * <p>
         * A hacking system launches a drone pod that will latch onto a target
         * system room, granting visibility. While the pod is attached and there
         * is power to the hacking system, the doors of the room turn purple,
         * locked to the crew of the targeted ship, but passable to the hacker's
         * crew.
         * <p>
         * This was introduced in FTL 1.5.4.
         */
        public void setUnknownEpsilon(int n) {
            unknownEpsilon = n;
        }

        public int getUnknownEpsilon() {
            return unknownEpsilon;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("Open: %-5b, Walking Through: %-5b%n", open, walkingThrough));
            result.append(String.format("Full HP: %3d, Current HP: %3d, Nominal HP: %3d, Delta?: %3d, Epsilon?: %3d%n", currentMaxHealth, health, nominalHealth, unknownDelta, unknownEpsilon));

            return result.toString();
        }
    }


    public static class LockdownCrystal {
        private int currentPosX = 0, currentPosY = 0;
        private int speed = 0;
        private int goalPosX = 0, goalPosY = 0;
        private boolean arrived = false;
        private boolean done = false;
        private int lifetime = 0;
        private boolean superFreeze = false;
        private int lockingRoom = 0;
        private int animDirection = 0;
        private int shardProgress = 0;


        public LockdownCrystal() {
        }

        public void setCurrentPositionX(int n) {
            currentPosX = n;
        }

        public void setCurrentPositionY(int n) {
            currentPosY = n;
        }

        public void setSpeed(int n) {
            speed = n;
        }

        public void setGoalPositionX(int n) {
            goalPosX = n;
        }

        public void setGoalPositionY(int n) {
            goalPosY = n;
        }

        public void setArrived(boolean b) {
            arrived = b;
        }

        public void setDone(boolean b) {
            done = b;
        }

        public void setLifetime(int n) {
            lifetime = n;
        }

        public void setSuperFreeze(boolean b) {
            superFreeze = b;
        }

        public void setLockingRoom(int n) {
            lockingRoom = n;
        }

        public void setAnimDirection(int n) {
            animDirection = n;
        }

        public void setShardProgress(int n) {
            shardProgress = n;
        }

        public int getCurrentPositionX() {
            return currentPosX;
        }

        public int getCurrentPositionY() {
            return currentPosY;
        }

        public int getSpeed() {
            return speed;
        }

        public int getGoalPositionX() {
            return goalPosX;
        }

        public int getGoalPositionY() {
            return goalPosY;
        }

        public boolean hasArrived() {
            return arrived;
        }

        public boolean isDone() {
            return done;
        }

        public int getLifetime() {
            return lifetime;
        }

        public boolean isSuperFreeze() {
            return superFreeze;
        }

        public int getLockingRoom() {
            return lockingRoom;
        }

        public int getAnimDirection() {
            return animDirection;
        }

        public int getShardProgress() {
            return shardProgress;
        }

        @Override
        public String toString() {
            return String.format("Current Position:  %8d,%8d (%9.03f,%9.03f)%n", currentPosX, currentPosY, currentPosX / 1000f, currentPosY / 1000f) +
                    String.format("Speed?:            %8d%n", speed) +
                    String.format("Goal Position:     %8d,%8d (%9.03f,%9.03f)%n", goalPosX, goalPosY, goalPosX / 1000f, goalPosY / 1000f) +
                    String.format("Arrived?:          %8b%n", arrived) +
                    String.format("Done?:             %8b%n", done) +
                    String.format("Lifetime?:         %8d%n", lifetime) +
                    String.format("SuperFreeze?:      %8b%n", superFreeze) +
                    String.format("Locking Room?:     %8d%n", lockingRoom) +
                    String.format("Anim Direction?:   %8d%n", animDirection) +
                    String.format("Shard Progress?:   %8d%n", shardProgress);
        }
    }


    public static class WeaponState {
        private String weaponId = null;
        private boolean armed = false;
        private int cooldownTicks = 0;
        private WeaponModuleState weaponMod = null;


        /**
         * Constructs an incomplete WeaponState.
         * <p>
         * It will need a weaponId.
         * <p>
         * For FTL 1.5.4+ saved games, a weapon module will be needed.
         */
        public WeaponState() {
        }

        /**
         * Copy constructor.
         * <p>
         * The weapon module will be copy-constructed as well.
         */
        public WeaponState(WeaponState srcWeapon) {
            weaponId = srcWeapon.getWeaponId();
            armed = srcWeapon.isArmed();
            cooldownTicks = srcWeapon.getCooldownTicks();

            if (srcWeapon.getWeaponModule() != null) {
                weaponMod = new WeaponModuleState(srcWeapon.getWeaponModule());
            }
        }


        /**
         * Resets aspects of an existing object to be viable for player use.
         * <p>
         * This will be called by the ship object when it is commandeered.
         * <p>
         * Warning: Dangerous while values remain undeciphered.
         */
        public void commandeer() {
            setArmed(false);
            setCooldownTicks(0);

            if (getWeaponModule() != null) {
                getWeaponModule().commandeer();
            }
        }


        public void setWeaponId(String s) {
            weaponId = s;
        }

        public String getWeaponId() {
            return weaponId;
        }

        public void setArmed(boolean b) {
            armed = b;
            if (b == false) cooldownTicks = 0;
        }

        public boolean isArmed() {
            return armed;
        }

        /**
         * Sets time elapsed waiting for the weapon to cool down.
         * <p>
         * This increments from 0, by 1 each second. Its goal is the value of
         * the 'coolown' tag in its WeaponBlueprint xml (0 when not armed).
         * <p>
         * Since FTL 1.5.4, this is no longer saved.
         *
         * @see WeaponModuleState.setCooldownTicks(int)
         */
        public void setCooldownTicks(int n) {
            cooldownTicks = n;
        }

        public int getCooldownTicks() {
            return cooldownTicks;
        }

        /**
         * Sets additional weapon fields.
         * <p>
         * Advanced Edition added extra weapon fields at the end of saved game
         * files. They're nested inside this class for convenience.
         * <p>
         * This was introduced in FTL 1.5.4.
         */
        public void setWeaponModule(WeaponModuleState weaponMod) {
            this.weaponMod = weaponMod;
        }

        public WeaponModuleState getWeaponModule() {
            return weaponMod;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            WeaponBlueprint weaponBlueprint = DataManager.get().getWeapon(weaponId);
            String cooldownString = (weaponBlueprint != null ? weaponBlueprint.getCooldown() + "" : "?");

            result.append(String.format("WeaponId:       %s%n", weaponId));
            result.append(String.format("Armed:          %b%n", armed));
            result.append(String.format("Cooldown Ticks: %2d (max: %2s) (Not used as of FTL 1.5.4)%n", cooldownTicks, cooldownString));

            result.append("\nWeapon Module...\n");
            if (weaponMod != null) {
                result.append(weaponMod.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            return result.toString();
        }
    }


    /**
     * Types of drones.
     * <p>
     * FTL 1.5.4 introduced HACKING, BEAM, and SHIELD.
     */
    public static enum DroneType {
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

        private String id;
        private int maxHealth;

        private DroneType(String id, int maxHealth) {
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

    public static class DroneState {
        private String droneId = null;
        private boolean armed = false;
        private boolean playerControlled = false;
        private int bodyX = -1, bodyY = -1;
        private int bodyRoomId = -1;
        private int bodyRoomSquare = -1;
        private int health = 1;
        private ExtendedDroneInfo droneInfo = null;


        /**
         * Constructs an incomplete DroneState.
         * <p>
         * It will need a droneId.
         * <p>
         * For FTL 1.5.4+ saved games, extended info may be needed.
         */
        public DroneState() {
        }

        /**
         * Constructs an incomplete DroneState.
         * <p>
         * For FTL 1.5.4+ saved games, extended info may be needed.
         */
        public DroneState(String droneId) {
            this.droneId = droneId;
        }

        /**
         * Copy constructor.
         * <p>
         * The extended info will be copy-constructed as well.
         */
        public DroneState(DroneState srcDrone) {
            droneId = srcDrone.getDroneId();
            armed = srcDrone.isArmed();
            playerControlled = srcDrone.isPlayerControlled();
            bodyX = srcDrone.getBodyX();
            bodyY = srcDrone.getBodyY();
            bodyRoomId = srcDrone.getBodyRoomId();
            bodyRoomSquare = srcDrone.getBodyRoomSquare();
            health = srcDrone.getHealth();

            if (srcDrone.getExtendedDroneInfo() != null) {
                droneInfo = new ExtendedDroneInfo(srcDrone.getExtendedDroneInfo());
            }
        }


        /**
         * Resets aspects of an existing object to be viable for player use.
         * <p>
         * This will be called by the ship object when it is commandeered.
         * <p>
         * Warning: Dangerous while values remain undeciphered.
         * TODO: Recurse into all nested objects.
         */
        public void commandeer() {
            setArmed(false);
            setPlayerControlled(false);

            if (getExtendedDroneInfo() != null) {
                getExtendedDroneInfo().commandeer();
            }
        }


        public void setDroneId(String s) {
            droneId = s;
        }

        public String getDroneId() {
            return droneId;
        }

        /**
         * Sets whether this drone is powered.
         *
         * @see ExtendedDroneInfo#setArmed(boolean)
         */
        public void setArmed(boolean b) {
            armed = b;
        }

        public boolean isArmed() {
            return armed;
        }

        /**
         * Sets whether this drone is controlled by the player.
         * <p>
         * When the drone is not armed, this should be set to false.
         */
        public void setPlayerControlled(boolean b) {
            playerControlled = b;
        }

        public boolean isPlayerControlled() {
            return playerControlled;
        }

        /**
         * Sets the position of the drone's body image.
         * <p>
         * Technically the roomId/square fields set the goal location.
         * This field is where the body really is, possibly en route.
         * <p>
         * It's the position of the body image's center, relative to the
         * top-left corner of the floor layout of the ship it's on.
         * <p>
         * This value lingers, even after the body is gone.
         * <p>
         * Note: This is only set by drones which have a body on their own ship.
         */
        public void setBodyX(int n) {
            bodyX = n;
        }

        public void setBodyY(int n) {
            bodyY = n;
        }

        public int getBodyX() {
            return bodyX;
        }

        public int getBodyY() {
            return bodyY;
        }

        /**
         * Sets the room this drone's body is in (or at least trying to move
         * toward).
         * <p>
         * When no body is present, this is -1.
         * <p>
         * roomId and roomSquare need to be specified together.
         * <p>
         * Note: This is only set by drones which have a body on their own ship.
         */
        public void setBodyRoomId(int n) {
            bodyRoomId = n;
        }

        public void setBodyRoomSquare(int n) {
            bodyRoomSquare = n;
        }

        public int getBodyRoomId() {
            return bodyRoomId;
        }

        public int getBodyRoomSquare() {
            return bodyRoomSquare;
        }

        public void setHealth(int n) {
            health = n;
        }

        public int getHealth() {
            return health;
        }

        /**
         * Sets additional drone fields.
         * <p>
         * Advanced Edition added extra drone fields at the end of saved game
         * files. They're nested inside this class for convenience.
         * <p>
         * This was introduced in FTL 1.5.4.
         */
        public void setExtendedDroneInfo(ExtendedDroneInfo droneInfo) {
            this.droneInfo = droneInfo;
        }

        public ExtendedDroneInfo getExtendedDroneInfo() {
            return droneInfo;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("DroneId:           %s%n", droneId));
            result.append(String.format("Armed:             %5b%n", armed));
            result.append(String.format("Health:            %5d%n", health));
            result.append(String.format("Body Position:     %3d,%3d%n", bodyX, bodyY));
            result.append(String.format("Body Room Id:      %5d%n", bodyRoomId));
            result.append(String.format("Body Room Square:  %5d%n", bodyRoomSquare));
            result.append(String.format("Player Controlled: %5b%n", playerControlled));

            result.append("\nExtended Drone Info...\n");
            if (droneInfo != null) {
                result.append(droneInfo.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            return result.toString();
        }
    }


    public static enum FleetPresence {
        NONE("None"), REBEL("Rebel"), FEDERATION("Federation"), BOTH("Both");

        private String title;

        private FleetPresence(String title) {
            this.title = title;
        }

        public String toString() {
            return title;
        }
    }

    /**
     * A beacon on the sector map.
     * <p>
     * Beacon states do not contain their randomly determined values until they
     * are actually visited.
     * <p>
     * FTL uses the sector layout seed to decide pending events and such upon
     * entering the sector. Any distress, stores, etc. events get signs when
     * seen (or hazard icons when the map is revealed).
     *
     * @see SavedGameState#setSectorLayoutSeed(int)
     */
    public static class BeaconState {
        private int visitCount = 0;
        private String bgStarscapeImageInnerPath = null;
        private String bgSpriteImageInnerPath = null;
        private int bgSpritePosX = -1, bgSpritePosY = -1;
        private int bgSpriteRotation = 0;

        private boolean seen = false;

        private boolean enemyPresent = false;
        private String shipEventId = null;
        private String autoBlueprintId = null;
        private int shipEventSeed = 0;

        private FleetPresence fleetPresence = FleetPresence.NONE;

        private boolean underAttack = false;

        private StoreState store = null;


        /**
         * Constructor.
         */
        public BeaconState() {
        }

        /**
         * Copy constructor.
         * <p>
         * Any store will be copy-constructed as well.
         */
        public BeaconState(BeaconState srcBeacon) {
            visitCount = srcBeacon.getVisitCount();
            bgStarscapeImageInnerPath = srcBeacon.getBgStarscapeImageInnerPath();
            bgSpriteImageInnerPath = srcBeacon.getBgSpriteImageInnerPath();
            bgSpritePosX = srcBeacon.getBgSpritePosX();
            bgSpritePosY = srcBeacon.getBgSpritePosY();
            bgSpriteRotation = srcBeacon.getBgSpriteRotation();
            seen = srcBeacon.isSeen();
            enemyPresent = srcBeacon.isEnemyPresent();
            shipEventId = srcBeacon.getShipEventId();
            autoBlueprintId = srcBeacon.getAutoBlueprintId();
            shipEventSeed = srcBeacon.getShipEventSeed();
            fleetPresence = srcBeacon.getFleetPresence();
            underAttack = srcBeacon.isUnderAttack();

            if (srcBeacon.getStore() != null) {
                store = new StoreState(srcBeacon.getStore());
            }
        }

        /**
         * Sets the number of times the player has arrived at this beacon.
         * <p>
         * If non-zero, starscape and sprite paths must be set,
         * as well as the sprite's X, Y, and rotation.
         * <p>
         * When non-zero, this prevents randomly generated events
         * from triggering. The sector exit will still exist.
         */
        public void setVisitCount(int n) {
            visitCount = n;
        }

        public int getVisitCount() {
            return visitCount;
        }

        /**
         * Sets a fullscreen starscape image for the background.
         * <p>
         * By convention, this path is from the BG_* imageLists.
         */
        public void setBgStarscapeImageInnerPath(String s) {
            bgStarscapeImageInnerPath = s;
        }

        public String getBgStarscapeImageInnerPath() {
            return bgStarscapeImageInnerPath;
        }

        /**
         * Sets a background sprite to draw over the starscape.
         * <p>
         * By convention, this path is from the PLANET_* imageLists.
         * To not display a sprite, set it to "NONE".
         */
        public void setBgSpriteImageInnerPath(String s) {
            bgSpriteImageInnerPath = s;
        }

        public String getBgSpriteImageInnerPath() {
            return bgSpriteImageInnerPath;
        }

        /**
         * Sets the position of the background sprite image.
         * <p>
         * When the sprite's inner path is "NONE",
         * X and Y should be 0.
         */
        public void setBgSpritePosX(int n) {
            bgSpritePosX = n;
        }

        public void setBgSpritePosY(int n) {
            bgSpritePosY = n;
        }

        public int getBgSpritePosX() {
            return bgSpritePosX;
        }

        public int getBgSpritePosY() {
            return bgSpritePosY;
        }

        /**
         * Sets the rotation of the background sprite image.
         * <p>
         * When the sprite's inner path is "NONE", this should be 0.
         *
         * @param n degrees clockwise (may be negative)
         */
        public void setBgSpriteRotation(int n) {
            bgSpriteRotation = n;
        }

        public int getBgSpriteRotation() {
            return bgSpriteRotation;
        }

        /**
         * Sets whether the player has been within one hop of this beacon.
         */
        public void setSeen(boolean b) {
            seen = b;
        }

        public boolean isSeen() {
            return seen;
        }

        /**
         * Sets whether an enemy ship is waiting at this beacon.
         * <p>
         * If true, a ShipEvent and AutoBlueprint must be set,
         * as well as the ShipEvent seed.
         */
        public void setEnemyPresent(boolean b) {
            enemyPresent = b;
        }

        public boolean isEnemyPresent() {
            return enemyPresent;
        }

        /**
         * Sets a ShipEvent to trigger upon arrival.
         */
        public void setShipEventId(String s) {
            shipEventId = s;
        }

        public String getShipEventId() {
            return shipEventId;
        }

        /**
         * Sets an auto blueprint (or blueprintList) to spawn with the ShipEvent.
         */
        public void setAutoBlueprintId(String s) {
            autoBlueprintId = s;
        }

        public String getAutoBlueprintId() {
            return autoBlueprintId;
        }

        /**
         * Sets a seed to randomly generate the enemy ship (layout, etc).
         * <p>
         * When the player ship visits this beacon, the resulting encounter
         * will use this seed. When no enemy ship is present, this is 0.
         * <p>
         * In distant beacons occupied by the rebel fleet, this has been
         * observed varying between saves during a single fight!?
         */
        public void setShipEventSeed(int n) {
            shipEventSeed = n;
        }

        public int getShipEventSeed() {
            return shipEventSeed;
        }

        /**
         * Sets fleet background sprites and possibly the beacon icon.
         * <p>
         * When FTL moves the rebel fleet over a beacon, the beacon's
         * fleet presence becomes REBEL, and if it was visited, a
         * LONG_FLEET ShipEvent is set. Otherwise, one of the FLEET_*
         * events will be triggered to spawn the LONG_FLEET upon arrival.
         */
        public void setFleetPresence(FleetPresence fp) {
            fleetPresence = fp;
        }

        public FleetPresence getFleetPresence() {
            return fleetPresence;
        }

        /**
         * Sets whether this beacon is under attack by rebels (flashing red).
         * <p>
         * If true, the next time the player jumps to a beacon, this one
         * will have a REBEL fleet and possibly a LONG_FLEET ShipEvent,
         * and will no longer be under attack.
         */
        public void setUnderAttack(boolean b) {
            underAttack = b;
        }

        public boolean isUnderAttack() {
            return underAttack;
        }

        /**
         * Places a store at this beacon, or null for none.
         */
        public void setStore(StoreState storeState) {
            store = storeState;
        }

        public StoreState getStore() {
            return store;
        }


        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("Visit Count:           %5d%n", visitCount));
            if (visitCount > 0) {
                result.append(String.format("  Bkg Starscape:       %s%n", bgStarscapeImageInnerPath));
                result.append(String.format("  Bkg Sprite:          %s%n", bgSpriteImageInnerPath));
                result.append(String.format("  Bkg Sprite Position:   %3d,%3d%n", bgSpritePosX, bgSpritePosY));
                result.append(String.format("  Bkg Sprite Rotation:   %3d%n", bgSpriteRotation));
            }

            result.append(String.format("Seen:                  %5b%n", seen));

            result.append(String.format("Enemy Present:         %5b%n", enemyPresent));
            if (enemyPresent) {
                result.append(String.format("  Ship Event ID:       %s%n", shipEventId));
                result.append(String.format("  Auto Blueprint ID:   %s%n", autoBlueprintId));
                result.append(String.format("  Ship Event Seed:     %5d%n", shipEventSeed));
            }

            result.append(String.format("Fleets Present:        %s%n", fleetPresence));

            result.append(String.format("Under Attack:          %5b%n", underAttack));

            if (store != null) {
                result.append("\nStore...\n");
                result.append(store.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            return result.toString();
        }
    }


    /**
     * A store, which contains supplies and item shelves of various types.
     * <p>
     * FTL 1.01-1.03.3 always had two StoreShelf objects.
     * If more are added, only the first two will be saved.
     * If fewer, writeBeacon() will add dummy shelves with no items.
     * <p>
     * FTL 1.5.4 can have a variable number of shelves, but zero
     * crashes the game. So writeBeacon() will add a dummy shelf.
     * <p>
     * TODO: Find out what happens if more than four shelves are added.
     */
    public static class StoreState {
        private int fuel = 0, missiles = 0, droneParts = 0;
        private List<StoreShelf> shelfList = new ArrayList<StoreShelf>(4);  // TODO: Magic number.


        /**
         * Constructs a StoreState.
         */
        public StoreState() {
        }

        /**
         * Copy constructor.
         * <p>
         * Each StoreShelf will be copy-constructed as well.
         */
        public StoreState(StoreState srcStore) {
            fuel = srcStore.getFuel();
            missiles = srcStore.getMissiles();
            droneParts = srcStore.getDroneParts();

            for (StoreShelf srcShelf : srcStore.getShelfList()) {
                addShelf(new StoreShelf(srcShelf));
            }
        }

        public void setFuel(int n) {
            fuel = n;
        }

        public void setMissiles(int n) {
            missiles = n;
        }

        public void setDroneParts(int n) {
            droneParts = n;
        }

        public int getFuel() {
            return fuel;
        }

        public int getMissiles() {
            return missiles;
        }

        public int getDroneParts() {
            return droneParts;
        }

        public void setShelfList(List<StoreShelf> shelfList) {
            this.shelfList = shelfList;
        }

        public List<StoreShelf> getShelfList() {
            return shelfList;
        }

        public void addShelf(StoreShelf shelf) {
            shelfList.add(shelf);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("Fuel:        %2d%n", fuel));
            result.append(String.format("Missiles:    %2d%n", missiles));
            result.append(String.format("Drone Parts: %2d%n", droneParts));

            for (int i = 0; i < shelfList.size(); i++) {
                result.append(String.format("\nShelf %d...%n", i));
                result.append(shelfList.get(i).toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            return result.toString();
        }
    }

    public static enum StoreItemType {
        WEAPON("Weapon"), DRONE("Drone"), AUGMENT("Augment"),
        CREW("Crew"), SYSTEM("System");

        private String title;

        private StoreItemType(String title) {
            this.title = title;
        }

        public String toString() {
            return title;
        }
    }

    public static class StoreShelf {
        private StoreItemType itemType = StoreItemType.WEAPON;
        private List<StoreItem> items = new ArrayList<StoreItem>(3);  // TODO: Magic number.


        /**
         * Constructor.
         * <p>
         * Up to 3 StoreItems may to be added (Set the StoreItemType, too.)
         * Fewer StoreItems mean empty space on the shelf.
         */
        public StoreShelf() {
        }

        /**
         * Copy constructor.
         * <p>
         * Each StoreItem will be copy-constructed as well.
         */
        public StoreShelf(StoreShelf srcShelf) {
            itemType = srcShelf.getItemType();

            for (StoreItem tmpItem : srcShelf.getItems()) {
                addItem(new StoreItem(tmpItem));
            }
        }

        public void setItemType(StoreItemType type) {
            itemType = type;
        }

        public StoreItemType getItemType() {
            return itemType;
        }

        public void addItem(StoreItem item) {
            items.add(item);
        }

        public List<StoreItem> getItems() {
            return items;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            boolean first = true;

            result.append(String.format("Item Type: %s%n", itemType));
            for (StoreItem item : items) {
                if (first) {
                    first = false;
                } else {
                    result.append(",\n");
                }
                result.append(item.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            return result.toString();
        }
    }

    /**
     * An item in a store which either can be bought or has been bought already.
     */
    public static class StoreItem {
        private String itemId = null;
        private boolean available = false;
        private int extraData = 0;


        /**
         * Constructor.
         *
         * @param itemId a weapon/drone/augment blueprint or crew-race/system id
         */
        public StoreItem(String itemId) {
            this.itemId = itemId;
        }

        /**
         * Copy constructor.
         */
        public StoreItem(StoreItem srcItem) {
            this(srcItem.getItemId());
            available = srcItem.isAvailable();
            extraData = srcItem.getExtraData();
        }

        public String getItemId() {
            return itemId;
        }

        /**
         * Sets whether this item has been sold already.
         */
        public void setAvailable(boolean b) {
            available = b;
        }

        public boolean isAvailable() {
            return available;
        }

        /**
         * Unknown.
         * <p>
         * Bonus drones (Repair/Defense 1) are not remembered, so it's not
         * that. Reloading at a store offering a bonus Repair always results in
         * a Defense 1.
         * <p>
         * Observed values: 1 (w/Drone_Ctrl+Repair), 2 (w/Cloaking),
         * 1 (w/Clonebay), 0 (on them all after reloading!?). Also seen:
         * 2 (w/Drone_Ctrl), 1 (w/Teleporter), 2 (w/Battery).
         * <p>
         * This was introduced in FTL 1.5.12.
         */
        public void setExtraData(int n) {
            extraData = n;
        }

        public int getExtraData() {
            return extraData;
        }

        @Override
        public String toString() {
            return String.format("Item: %s, Available: %5b, Extra?: %3d%n", itemId, available, extraData);
        }
    }


    public static class EncounterState {
        private int shipEventSeed = 0;
        private String surrenderEventId = "";
        private String escapeEventId = "";
        private String destroyedEventId = "";
        private String deadCrewEventId = "";
        private String gotAwayEventId = "";

        private String lastEventId = "";
        private int unknownAlpha = 0;
        private String text = "";
        private int affectedCrewSeed = -1;
        private List<Integer> choiceList = new ArrayList<Integer>();


        public EncounterState() {
        }


        /**
         * Sets a seed to randomly generate the enemy ship (layout, etc).
         * <p>
         * When the player ship visits a beacon, the resulting encounter
         * will use the beacon's enemy ship event seed.
         * <p>
         * When not set, this is 0. After encountering ships, this value lingers.
         */
        public void setShipEventSeed(int n) {
            shipEventSeed = n;
        }

        public int getShipEventSeed() {
            return shipEventSeed;
        }

        public void setSurrenderEventId(String s) {
            surrenderEventId = s;
        }

        public void setEscapeEventId(String s) {
            escapeEventId = s;
        }

        public void setDestroyedEventId(String s) {
            destroyedEventId = s;
        }

        public void setDeadCrewEventId(String s) {
            deadCrewEventId = s;
        }

        public void setGotAwayEventId(String s) {
            gotAwayEventId = s;
        }

        public String getSurrenderEventId() {
            return surrenderEventId;
        }

        public String getEscapeEventId() {
            return escapeEventId;
        }

        public String getDestroyedEventId() {
            return destroyedEventId;
        }

        public String getDeadCrewEventId() {
            return deadCrewEventId;
        }

        public String getGotAwayEventId() {
            return gotAwayEventId;
        }

        /**
         * Sets the id of the most recent (possibly current) event id.
         * <p>
         * As secondary and tertiary events are triggered at a beacon, this
         * value will be replaced by their ids.
         * <p>
         * Sometimes this is blank.
         * <p>
         * Matthew's hint: There are two kinds of event: static events, assigned
         * randomly based on the sector seed and "sector_data.xml" (like for
         * nebula beacons); and dynamic events. This value only tracks dynamic
         * events.
         */
        public void setLastEventId(String s) {
            lastEventId = s;
        }

        public String getLastEventId() {
            return lastEventId;
        }

        /**
         * Unknown.
         * <p>
         * This was introduced in FTL 1.6.1.
         */
        public void setUnknownAlpha(int n) {
            unknownAlpha = n;
        }

        public int getUnknownAlpha() {
            return unknownAlpha;
        }

        /**
         * Sets the last situation-describing text shown in an event window.
         * <p>
         * Any event - 'static', secondary, or wait - may set this value. It
         * may have no relation to the last event id.
         * <p>
         * Note: Wait events triggered in-game set this value. Toggling waiting
         * programmatically does NOT set this value. That must be done
         * manually.
         * <p>
         * FTL 1.6.1 introduced XML "id" attributes on elements, which
         * referenced text elsewhere. This value may be one of those references
         * instead of the actual text.
         * <p>
         * After the event popup is dismissed, this value lingers.
         * <p>
         * This may include line breaks ("\n").
         *
         * @see SavedGameState#setWaiting(boolean)
         */
        public void setText(String s) {
            text = s;
        }

        public String getText() {
            return text;
        }

        /**
         * Sets a seed used to randomly select crew.
         * <p>
         * When saved mid-event, this allows FTL to reselect the same crew.
         * <p>
         * When no random selection has been made, this is -1.
         */
        public void setAffectedCrewSeed(int n) {
            affectedCrewSeed = n;
        }

        public int getAffectedCrewSeed() {
            return affectedCrewSeed;
        }

        /**
         * Sets a list of breadcrumbs for choices made during the last event.
         * <p>
         * Each integer in the list corresponds to a prompt, and the Integer's
         * value is the Nth choice that was clicked. (0-based)
         * <p>
         * TODO: 52 was observed in the list once!?
         * <p>
         * The event will still be in-progress if there aren't enough
         * breadcrumbs to renavigate to the end of the event.
         * <p>
         * The list typically ends with a 0, since events usually conclude with
         * a lone "continue" choice.
         * <p>
         * Note: If waiting, this list will cause a wait event to be selected
         * from fuel-related event lists, instead of a normal event.
         *
         * @see SavedGameState#setWaiting(boolean)
         */
        public void setChoiceList(List<Integer> choiceList) {
            this.choiceList = choiceList;
        }

        public List<Integer> getChoiceList() {
            return choiceList;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            result.append(String.format("Ship Event Seed:    %3d%n", shipEventSeed));
            result.append(String.format("Surrender Event:    %s%n", surrenderEventId));
            result.append(String.format("Escape Event:       %s%n", escapeEventId));
            result.append(String.format("Destroyed Event:    %s%n", destroyedEventId));
            result.append(String.format("Dead Crew Event:    %s%n", deadCrewEventId));
            result.append(String.format("Got Away Event:     %s%n", gotAwayEventId));

            result.append("\n");

            result.append(String.format("Last Event:         %s%n", lastEventId));
            result.append(String.format("Alpha?:             %3d%n", unknownAlpha));

            result.append("\nText...\n");
            result.append(String.format("%s%n", text));
            result.append("\n");

            result.append(String.format("Affected Crew Seed: %3d%n", affectedCrewSeed));

            result.append("\nLast Event Choices...\n");
            first = true;
            for (Integer choiceInt : choiceList) {
                if (first) {
                    first = false;
                } else {
                    result.append(",");
                }
                result.append(choiceInt);
            }
            result.append("\n");

            return result.toString();
        }
    }


    public static class NearbyShipAIState {
        private boolean surrendered = false;
        private boolean escaping = false;
        private boolean destroyed = false;
        private int surrenderThreshold = 0;
        private int escapeThreshold = -1;
        private int escapeTicks = 15000;
        private boolean stalemateTriggered = false;  // TODO: Does this start sudden death, or mark its completion?
        private int stalemateTicks = 0;
        private int boardingAttempts = 0;
        private int boardersNeeded = 0;


        /**
         * Constructor.
         */
        public NearbyShipAIState() {
        }

        /**
         * Toggles whether the ship has offered surrender.
         * <p>
         * FTL sets this the moment it triggers the surrender event (before the
         * player accepts/declines).
         */
        public void setSurrendered(boolean b) {
            surrendered = b;
        }

        public boolean hasSurrendered() {
            return surrendered;
        }

        /**
         * Toggles whether the ship is powering up its FTL to escape.
         */
        public void setEscaping(boolean b) {
            escaping = b;
        }

        public boolean isEscaping() {
            return escaping;
        }

        /**
         * Toggles whether the ship has been destroyed.
         * <p>
         * TODO: Confirm this.
         */
        public void setDestroyed(boolean b) {
            destroyed = b;
        }

        public boolean isDestroyed() {
            return destroyed;
        }

        /**
         * Sets the hull amount that will cause the ship will surrender.
         * <p>
         * For the rebel flagship, this is -100.
         */
        public void setSurrenderThreshold(int n) {
            surrenderThreshold = n;
        }

        public int getSurrenderThreshold() {
            return surrenderThreshold;
        }

        /**
         * Sets the hull amount that will cause the ship to flee.
         * <p>
         * For the rebel flagship, this is -101.
         * <p>
         * When not set, this is -1.
         */
        public void setEscapeThreshold(int n) {
            escapeThreshold = n;
        }

        public int getEscapeThreshold() {
            return escapeThreshold;
        }

        /**
         * Sets time elapsed while waiting for the FTL drive to charge.
         * <p>
         * This decrements to 0; then the nearby ship jumps away.
         * <p>
         * Observed values: 15000 (initially), 27533 (30000?).
         * <p>
         * TODO: An FTL Jammer augment might only override the default once an
         * escape attempt is initiated. It was still 15000 at the beginning of
         * one battle.
         */
        public void setEscapeTicks(int n) {
            escapeTicks = n;
        }

        public int getEscapeTicks() {
            return escapeTicks;
        }

        public void setStalemateTriggered(boolean b) {
            stalemateTriggered = b;
        }

        public boolean isStalemateTriggered() {
            return stalemateTriggered;
        }

        public void setStalemateTicks(int n) {
            stalemateTicks = n;
        }

        public int getStalemateTicks() {
            return stalemateTicks;
        }

        /**
         * Sets the count of times crew teleported so far.
         * <p>
         * After a certain number, no further boarding attempts will be made.
         * <p>
         * TODO: Determine that limit, and whether it counts crew or parties.
         */
        public void setBoardingAttempts(int n) {
            boardingAttempts = n;
        }

        public int getBoardingAttempts() {
            return boardingAttempts;
        }

        /**
         * Sets the number of crew to teleport as boarders.
         * <p>
         * Matthew's hint: It's based on the original crew strength.
         * <p>
         * TODO: Test if this is the limit for setBoardingAttempts().
         */
        public void setBoardersNeeded(int n) {
            boardersNeeded = n;
        }

        public int getBoardersNeeded() {
            return boardersNeeded;
        }

        @Override
        public String toString() {
            return String.format("Surrender Offered:   %7b%n", surrendered) +
                    String.format("Escaping:            %7b%n", escaping) +
                    String.format("Destroyed?:          %7b%n", destroyed) +
                    String.format("Surrender Threshold: %7d (Hull amount when surrender is offered)%n", surrenderThreshold) +
                    String.format("Escape Threshold:    %7d (Hull amount when escape begins)%n", escapeThreshold) +
                    String.format("Escape Ticks:        %7d (Decrements to 0)%n", escapeTicks) +
                    String.format("Stalemate Triggered?:%7b%n", stalemateTriggered) +
                    String.format("Stalemate Ticks?:    %7d%n", stalemateTicks) +
                    String.format("Boarding Attempts?:  %7d%n", boardingAttempts) +
                    String.format("Boarders Needed?:    %7d%n", boardersNeeded);
        }
    }


    public static enum HazardVulnerability {
        PLAYER_SHIP, NEARBY_SHIP, BOTH_SHIPS
    }

    public static class EnvironmentState {
        private boolean redGiantPresent = false;
        private boolean pulsarPresent = false;
        private boolean pdsPresent = false;
        private HazardVulnerability vulnerableShips = HazardVulnerability.BOTH_SHIPS;
        private AsteroidFieldState asteroidField = null;
        private int solarFlareFadeTicks = 0;
        private int havocTicks = 0;
        private int pdsTicks = 0;  // Used by: PDS. Value lingers after leaving a beacon (sometimes varying by 1).


        public EnvironmentState() {
        }


        /**
         * Toggles the presence of a red giant hazard.
         * <p>
         * Red giant, pulsar, and PDS hazards can coexist.
         */
        public void setRedGiantPresent(boolean b) {
            redGiantPresent = b;
        }

        public boolean isRedGiantPresent() {
            return redGiantPresent;
        }

        /**
         * Toggles the presence of a pulsar hazard.
         * <p>
         * Red giant, pulsar, and PDS hazards can coexist.
         */
        public void setPulsarPresent(boolean b) {
            pulsarPresent = b;
        }

        public boolean isPulsarPresent() {
            return pulsarPresent;
        }

        /**
         * Toggles the presence of a PDS hazard.
         * <p>
         * Red giant, pulsar, and PDS hazards can coexist.
         */
        public void setPDSPresent(boolean b) {
            pdsPresent = b;
        }

        public boolean isPDSPresent() {
            return pdsPresent;
        }

        /**
         * Sets which ships will be targeted by a PDS.
         * <p>
         * Matthew's hint: Values are 0,1,2 for player ship, nearby ship, or
         * both. (0 and 1 are unconfirmed.)
         */
        public void setVulnerableShips(HazardVulnerability vuln) {
            vulnerableShips = vuln;
        }

        public HazardVulnerability getVulnerableShips() {
            return vulnerableShips;
        }

        public void setAsteroidField(AsteroidFieldState asteroidField) {
            this.asteroidField = asteroidField;
        }

        public AsteroidFieldState getAsteroidField() {
            return asteroidField;
        }


        /**
         * Sets elapsed time while the screen fades to/from white during a
         * solar flare from a red giant or pulsar.
         * <p>
         * TODO: Determine the number this counts to.
         */
        public void setSolarFlareFadeTicks(int n) {
            solarFlareFadeTicks = n;
        }

        public int getSolarFlareFadeTicks() {
            return solarFlareFadeTicks;
        }

        /**
         * Sets elapsed time while waiting for havoc from a red giant/pulsar/PDS.
         * <p>
         * For red giants, This counts to 30000, triggers a solar flare, and
         * returns to 0. A warning appears around 25000.
         * <p>
         * For pulsars, this hasn't been observed over 11000.
         * <p>
         * For PDS, this might count to 20000 before firing AT the ship (as
         * opposed to decorative misses)?
         * <p>
         * After leaving a beacon with such hazards, this value lingers (+/-1).
         */
        public void setHavocTicks(int n) {
            havocTicks = n;
        }

        public int getHavocTicks() {
            return havocTicks;
        }

        public void setPDSTicks(int n) {
            pdsTicks = n;
        }

        public int getPDSTicks() {
            return pdsTicks;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("Red Giant Present: %5b%n", redGiantPresent));
            result.append(String.format("Pulsar Present:    %5b%n", pulsarPresent));
            result.append(String.format("PDS Present:       %5b%n", pdsPresent));
            result.append(String.format("Vulnerable Ships:  %s (PDS only)%n", vulnerableShips.toString()));

            result.append("\nAsteroid Field...\n");
            if (asteroidField != null)
                result.append(asteroidField.toString().replaceAll("(^|\n)(.+)", "$1  $2"));

            result.append("\n");

            result.append(String.format("Flare Fade Ticks?: %7d%n", solarFlareFadeTicks));
            result.append(String.format("Havoc Ticks?:      %7d (Red Giant/Pulsar/PDS only, Goal varies)%n", havocTicks));
            result.append(String.format("PDS Ticks?:        %7d (PDS only)%n", pdsTicks));

            return result.toString();
        }
    }

    public static class AsteroidFieldState {
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


    /**
     * Info used for spawning the rebel flagship.
     * <p>
     * Whereas regular ship encounters are preserved in BeaconStates for repeat
     * visits, the flagship is not tied to a location.
     * <p>
     * In FTL 1.01-1.03.3, this info is not present in saved games until
     * after engaging the rebel flagship in sector 8 for the first time.
     * <p>
     * In FTL 1.5.4, this is always present, though the occupancy map may be
     * empty.
     */
    public static class RebelFlagshipState {
        private int unknownAlpha = 0;
        private int pendingStage = 1;
        private int unknownGamma = 30000;
        private int unknownDelta = 0;
        private Map<Integer, Integer> occupancyMap = new LinkedHashMap<Integer, Integer>();


        /**
         * Constructor.
         */
        public RebelFlagshipState() {
        }

        /**
         * Unknown.
         * <p>
         * Observed values: 0 (normal), 1 (after encountering
         * first-stage boss), 2 (after encountering second-stage boss), 3
         * (after encountering third-stage boss).
         * <p>
         * This was introduced in FTL 1.5.4.
         */
        public void setUnknownAlpha(int n) {
            unknownAlpha = n;
        }

        public int getUnknownAlpha() {
            return unknownAlpha;
        }

        /**
         * Sets the next version of the flagship that will be encountered (1-based).
         * <p>
         * This must be one of the available stages: 1-3.
         */
        public void setPendingStage(int pendingStage) {
            this.pendingStage = pendingStage;
        }

        public int getPendingStage() {
            return pendingStage;
        }

        /**
         * Unknown.
         * <p>
         * During the third-stage boss fight, this does not change.
         * <p>
         * Observed values: 30000 (normal), 21326 (after encountering
         * first-stage boss).
         * <p>
         * This was introduced in FTL 1.5.4.
         */
        public void setUnknownGamma(int n) {
            unknownGamma = n;
        }

        public int getUnknownGamma() {
            return unknownGamma;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: 0 (normal), 240 (after encountering
         * first-stage boss), 26563 (after defeating second-stage boss). Seems
         * to have no effect on first-stage boss, but this changes nonetheless.
         * During the second-stage boss, counts to ~25000, then it resets to 0,
         * and surge drones appear. During the third-stage boss, counts to
         * ~16000, then it either recharges its Zoltan shield or fires lots of
         * laser projectiles.
         * <p>
         * This was introduced in FTL 1.5.4.
         */
        public void setUnknownDelta(int n) {
            unknownDelta = n;
        }

        public int getUnknownDelta() {
            return unknownDelta;
        }

        /**
         * Sets whether a room had crew members in the last seen layout.
         * <p>
         * Stage 1 sets this, but doesn't read it.
         * Fleeing stage 1, editing, then returning only results in a fresh
         * fight.
         * <p>
         * Upon first engaging stage 2, the layout is migrated.
         * The occupancy list is truncated to the new layout's rooms.
         * (The blueprints happen to have matching low roomIds.)
         * <p>
         * Stage 1 (BOSS_1): 19 rooms.
         * Stage 2 (BOSS_2): 15 rooms.
         * Stage 3 (BOSS_3): 11 rooms.
         * Having 0 rooms occupied is allowed, meaning AI took over.
         * <p>
         * Stage 2 will respond to pre-skirmish editing.
         * <p>
         * Stage 3 probably will, too. (TODO: Confirm this.)
         *
         * @param roomId a room in the last seen stage's shipLayout
         * @param n      the number of crew in that room
         */
        public void setPreviousOccupancy(int roomId, int n) {
            occupancyMap.put(roomId, n);
        }

        public Map<Integer, Integer> getOccupancyMap() {
            return occupancyMap;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("Alpha?:                 %11d%n", unknownAlpha));
            result.append(String.format("Pending Flagship Stage: %11d%n", pendingStage));
            result.append(String.format("Gamma?:                 %11d%n", unknownGamma));
            result.append(String.format("Delta?:                 %11d%n", unknownDelta));

            result.append("\nOccupancy of Last Seen Flagship...\n");
            for (Map.Entry<Integer, Integer> entry : occupancyMap.entrySet()) {
                int roomId = entry.getKey();
                int occupantCount = entry.getValue();

                result.append(String.format("Room Id: %2d, Crew: %d%n", roomId, occupantCount));
            }

            return result.toString();
        }
    }


    public static class AnimState {
        private boolean playing = false;
        private boolean looping = false;
        private int currentFrame = 0;
        private int progressTicks = 0;
        private int scale = 1000;
        private int x = -1000;
        private int y = -1000;


        /**
         * Constructor.
         */
        public AnimState() {
        }

        /**
         * Copy constructor.
         */
        public AnimState(AnimState srcAnim) {
            playing = srcAnim.isPlaying();
            looping = srcAnim.isLooping();
            currentFrame = srcAnim.getCurrentFrame();
            progressTicks = srcAnim.getProgressTicks();
            scale = srcAnim.getScale();
            x = srcAnim.getX();
            y = srcAnim.getY();
        }

        public void setPlaying(boolean b) {
            playing = b;
        }

        public boolean isPlaying() {
            return playing;
        }

        public void setLooping(boolean b) {
            looping = b;
        }

        public boolean isLooping() {
            return looping;
        }

        /**
         * Sets the current frame of this anim (0-based).
         * <p>
         * Start/end frames during playback vary. Anims, and their important
         * frames, are defined in "animations.xml".
         * <p>
         * FTL seems to clobber this value upon loading, based on the
         * circumstances driving the anim, so editing it is probably useless.
         */
        public void setCurrentFrame(int n) {
            currentFrame = n;
        }

        public int getCurrentFrame() {
            return currentFrame;
        }

        /**
         * Sets time elapsed while playing this anim.
         * <p>
         * Technically this doesn't count, so much as remember how far into the
         * anim playback was when the current frame appeared.
         * <p>
         * This value is 1000 / (animSheet's frame count) * (currentFrame).
         * Sometimes that's off by 1 due to rounding somewhere.
         * <p>
         * TODO: That formula matched WeaponModuleState's weaponAnim, at least.
         * <p>
         * FTL seems to clobber this value upon loading, based on the
         * circumstances driving the anim, so editing it is probably useless.
         */
        public void setProgressTicks(int n) {
            progressTicks = n;
        }

        public int getProgressTicks() {
            return progressTicks;
        }

        /**
         * Sets a scale factor.
         * <p>
         * Projectiles with flightAnimId "debris_small" set their deathAnim
         * scale to 250.
         *
         * @param n a pseudo-float (1000 is 1.0)
         */
        public void setScale(int n) {
            scale = n;
        }

        public int getScale() {
            return scale;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: 0 (when playing), -1000 (when not playing).
         * One time, a missile exploded whose deathAnim had -32000.
         */
        public void setX(int n) {
            x = n;
        }

        public void setY(int n) {
            y = n;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public String toString() {
            return String.format("Playing:           %7b%n", playing) +
                    String.format("Looping?:          %7b%n", looping) +
                    String.format("Current Frame:     %7d%n", currentFrame) +
                    String.format("Progress Ticks:    %7d%n", progressTicks) +
                    String.format("Scale:             %7d (%5.03f)%n", scale, scale / 1000f) +
                    String.format("X,Y?:                %5d,%5d%n", x, y);
        }
    }


    public static abstract class ExtendedSystemInfo {

        protected ExtendedSystemInfo() {
        }

        protected ExtendedSystemInfo(ExtendedSystemInfo srcInfo) {
        }

        /**
         * Blindly copy-constructs objects.
         * <p>
         * Subclasses override this with return values of their own type.
         */
        public abstract ExtendedSystemInfo copy();

        /**
         * Resets aspects of an existing object to be viable for player use.
         * <p>
         * This will be called by the ship object when it is commandeered.
         * <p>
         * Warning: Dangerous while values remain undeciphered.
         */
        public abstract void commandeer();
    }

    public static class ClonebayInfo extends ExtendedSystemInfo {
        private int buildTicks = 0;
        private int buildTicksGoal = 0;
        private int doomTicks = 0;


        public ClonebayInfo() {
            super();
        }

        /**
         * Copy constructor.
         */
        protected ClonebayInfo(ClonebayInfo srcInfo) {
            super(srcInfo);
            buildTicks = srcInfo.getBuildTicks();
            buildTicksGoal = srcInfo.getBuildTicksGoal();
            doomTicks = srcInfo.getDoomTicks();
        }

        @Override
        public ClonebayInfo copy() {
            return new ClonebayInfo(this);
        }

        /**
         * Resets aspects of an existing object to be viable for player use.
         * <p>
         * This will be called by the ship object when it is commandeered.
         */
        @Override
        public void commandeer() {
            setBuildTicks(0);
            setBuildTicksGoal(0);
            setDoomTicks(0);
        }

        /**
         * Sets elapsed time while building a clone.
         *
         * @param n a positive int less than, or equal to, the goal (0 when not engaged)
         * @see #setBuildTicksGoal(int)
         */
        public void setBuildTicks(int n) {
            buildTicks = n;
        }

        public int getBuildTicks() {
            return buildTicks;
        }


        /**
         * Sets total time needed to finish building a clone.
         * <p>
         * This can vary depending on the system level when the clonebay is
         * initially engaged. When not engaged, this value lingers.
         *
         * @see #setBuildTicks(int)
         */
        public void setBuildTicksGoal(int n) {
            buildTicksGoal = n;
        }

        public int getBuildTicksGoal() {
            return buildTicksGoal;
        }

        /**
         * Sets elapsed time while there are dead crew and the clonebay is unpowered.
         * <p>
         * This counts to 3000, at which point dead crew are lost.
         *
         * @param n 0-3000, or -1000
         */
        public void setDoomTicks(int n) {
            doomTicks = n;
        }

        public int getDoomTicks() {
            return doomTicks;
        }

        @Override
        public String toString() {
            return String.format("SystemId:                 %s%n", SystemType.CLONEBAY.getId()) +
                    String.format("Build Ticks:            %7d (For the current dead crew being cloned)%n", buildTicks) +
                    String.format("Build Ticks Goal:       %7d%n", buildTicksGoal) +
                    String.format("DoomTicks:              %7d (If unpowered, dead crew are lost at 3000)%n", doomTicks);
        }
    }

    public static class BatteryInfo extends ExtendedSystemInfo {
        private boolean active = false;
        private int usedBattery = 0;
        private int dischargeTicks = 1000;

        // Plasma storms only halve *reserve* power.
        // The Battery system is unaffected by plasma storms (<environment type="storm"/>).


        public BatteryInfo() {
            super();
        }

        /**
         * Copy constructor.
         */
        protected BatteryInfo(BatteryInfo srcInfo) {
            super(srcInfo);
            active = srcInfo.isActive();
            usedBattery = srcInfo.getUsedBattery();
            dischargeTicks = srcInfo.getDischargeTicks();
        }

        @Override
        public BatteryInfo copy() {
            return new BatteryInfo(this);
        }

        /**
         * Resets aspects of an existing object to be viable for player use.
         * <p>
         * This will be called by the ship object when it is commandeered.
         */
        @Override
        public void commandeer() {
            setActive(false);
            setUsedBattery(0);
            setDischargeTicks(1000);
        }

        /**
         * Toggles whether the battery is turned on.
         *
         * @see #setDischargeTicks(int)
         */
        public void setActive(boolean b) {
            active = b;
        }

        public boolean isActive() {
            return active;
        }

        /**
         * Sets the total battery power currently assigned to systems.
         * <p>
         * This is subtracted from a pool based on the battery system's level
         * to calculate remaining battery power.
         */
        public void setUsedBattery(int n) {
            usedBattery = n;
        }

        public int getUsedBattery() {
            return usedBattery;
        }

        /**
         * Sets elapsed time while the battery is active.
         * <p>
         * This counts to 1000. When not discharging, it's 1000.
         * After it's fully discharged, the battery system will be locked for
         * a bit.
         *
         * @param n 0-1000
         * @see #setDischargeTicks(int)
         */
        public void setDischargeTicks(int n) {
            dischargeTicks = n;
        }

        public int getDischargeTicks() {
            return dischargeTicks;
        }

        @Override
        public String toString() {
            return String.format("SystemId:                 %s%n", SystemType.BATTERY.getId()) +
                    String.format("Active:                   %5b%n", active) +
                    String.format("Battery Power in Use:     %5d%n", usedBattery) +
                    String.format("Discharge Ticks:          %5d%n", dischargeTicks);
        }
    }

    public static class ShieldsInfo extends ExtendedSystemInfo {
        private int shieldLayers = 0;
        private int energyShieldLayers = 0;
        private int energyShieldMax = 0;
        private int shieldRechargeTicks = 0;

        private boolean shieldDropAnimOn = false;
        private int shieldDropAnimTicks = 0;

        private boolean shieldRaiseAnimOn = false;
        private int shieldRaiseAnimTicks = 0;

        private boolean energyShieldAnimOn = false;
        private int energyShieldAnimTicks = 0;

        private int unknownLambda = 0;
        private int unknownMu = 0;


        public ShieldsInfo() {
            super();
        }

        /**
         * Copy constructor.
         */
        protected ShieldsInfo(ShieldsInfo srcInfo) {
            super(srcInfo);
            shieldLayers = srcInfo.getShieldLayers();
            energyShieldLayers = srcInfo.getEnergyShieldLayers();
            energyShieldMax = srcInfo.getEnergyShieldMax();
            shieldRechargeTicks = srcInfo.getShieldRechargeTicks();

            shieldDropAnimOn = srcInfo.isShieldDropAnimOn();
            shieldDropAnimTicks = srcInfo.getShieldDropAnimTicks();

            shieldRaiseAnimOn = srcInfo.isShieldRaiseAnimOn();
            shieldRaiseAnimTicks = srcInfo.getShieldRaiseAnimTicks();

            energyShieldAnimOn = srcInfo.isEnergyShieldAnimOn();
            energyShieldAnimTicks = srcInfo.getEnergyShieldAnimTicks();

            unknownLambda = srcInfo.getUnknownLambda();
            unknownMu = srcInfo.getUnknownMu();
        }

        @Override
        public ShieldsInfo copy() {
            return new ShieldsInfo(this);
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
            setShieldLayers(0);
            setShieldRechargeTicks(0);
            setShieldDropAnimOn(false);
            setShieldDropAnimTicks(0);   // TODO: Vet this default.
            setShieldRaiseAnimOn(false);
            setShieldRaiseAnimTicks(0);  // TODO: Vet this default.
        }

        /**
         * Sets the current number of normal shield layers.
         * <p>
         * This is indicated in-game by filled bubbles.
         */
        public void setShieldLayers(int n) {
            shieldLayers = n;
        }

        public int getShieldLayers() {
            return shieldLayers;
        }

        /**
         * Sets the current number of energy shield layers.
         * <p>
         * This is indicated in-game by green rectangles.
         */
        public void setEnergyShieldLayers(int n) {
            energyShieldLayers = n;
        }

        public int getEnergyShieldLayers() {
            return energyShieldLayers;
        }

        /**
         * Sets the number of energy shield layers when fully charged.
         * <p>
         * This is 0 until set by a mechanism that adds energy layers. This
         * value lingers after a temporary energy shield is exhausted.
         */
        public void setEnergyShieldMax(int n) {
            energyShieldMax = n;
        }

        public int getEnergyShieldMax() {
            return energyShieldMax;
        }

        /**
         * Sets elapsed time while waiting for the next normal shield layer
         * to recharge.
         * <p>
         * This counts to 2000. When not recharging, it is 0.
         */
        public void setShieldRechargeTicks(int n) {
            shieldRechargeTicks = n;
        }

        public int getShieldRechargeTicks() {
            return shieldRechargeTicks;
        }


        /**
         * Toggles whether the regular shield drop animation is being played.
         * <p>
         * Note: The drop and raise anims can both play simultaneously.
         */
        public void setShieldDropAnimOn(boolean b) {
            shieldDropAnimOn = b;
        }

        public boolean isShieldDropAnimOn() {
            return shieldDropAnimOn;
        }

        /**
         * Sets elapsed time while playing the regular shield drop anim.
         *
         * @param n 0-1000
         */
        public void setShieldDropAnimTicks(int n) {
            shieldDropAnimTicks = n;
        }

        public int getShieldDropAnimTicks() {
            return shieldDropAnimTicks;
        }


        /**
         * Toggles whether the regular shield raise animation is being played.
         * <p>
         * Note: The drop and raise anims can both play simultaneously.
         */
        public void setShieldRaiseAnimOn(boolean b) {
            shieldRaiseAnimOn = b;
        }

        public boolean isShieldRaiseAnimOn() {
            return shieldRaiseAnimOn;
        }

        /**
         * Sets elapsed time while playing the regular shield raise anim.
         *
         * @param n 0-1000
         */
        public void setShieldRaiseAnimTicks(int n) {
            shieldRaiseAnimTicks = n;
        }

        public int getShieldRaiseAnimTicks() {
            return shieldRaiseAnimTicks;
        }


        /**
         * Toggles whether the energy shield animation is being played.
         */
        public void setEnergyShieldAnimOn(boolean b) {
            energyShieldAnimOn = b;
        }

        public boolean isEnergyShieldAnimOn() {
            return energyShieldAnimOn;
        }

        /**
         * Sets elapsed time while playing the energy shield anim.
         *
         * @param n 0-1000
         */
        public void setEnergyShieldAnimTicks(int n) {
            energyShieldAnimTicks = n;
        }

        public int getEnergyShieldAnimTicks() {
            return energyShieldAnimTicks;
        }


        public void setUnknownLambda(int n) {
            unknownLambda = n;
        }

        public void setUnknownMu(int n) {
            unknownMu = n;
        }

        public int getUnknownLambda() {
            return unknownLambda;
        }

        public int getUnknownMu() {
            return unknownMu;
        }

        @Override
        public String toString() {
            return String.format("SystemId:                 %s%n", SystemType.SHIELDS.getId()) +
                    String.format("Shield Layers:            %5d (Currently filled bubbles)%n", shieldLayers) +
                    String.format("Energy Shield Layers:     %5d%n", energyShieldLayers) +
                    String.format("Energy Shield Max:        %5d (Layers when fully charged)%n", energyShieldLayers) +
                    String.format("Shield Recharge Ticks:    %5d%n", shieldRechargeTicks) +
                    "\n" +
                    String.format("Shield Drop Anim:   Play: %-5b, Ticks: %4d%n", shieldDropAnimOn, shieldDropAnimTicks) +
                    String.format("Shield Raise Anim:  Play: %-5b, Ticks: %4d%n", shieldRaiseAnimOn, shieldRaiseAnimTicks) +
                    String.format("Energy Shield Anim: Play: %-5b, Ticks: %4d%n", energyShieldAnimOn, energyShieldAnimTicks) +
                    String.format("Lambda?, Mu?:           %7d,%7d (Some kind of coord?)%n", unknownLambda, unknownMu);
        }
    }

    public static class CloakingInfo extends ExtendedSystemInfo {
        private int unknownAlpha = 0;
        private int unknownBeta = 0;
        private int cloakTicksGoal = 0;
        private int cloakTicks = Integer.MIN_VALUE;


        public CloakingInfo() {
            super();
        }

        /**
         * Copy constructor.
         */
        protected CloakingInfo(CloakingInfo srcInfo) {
            super(srcInfo);
            unknownAlpha = srcInfo.getUnknownAlpha();
            unknownBeta = srcInfo.getUnknownBeta();
            cloakTicksGoal = srcInfo.getCloakTicksGoal();
            cloakTicks = srcInfo.getCloakTicks();
        }

        @Override
        public CloakingInfo copy() {
            return new CloakingInfo(this);
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
            setUnknownAlpha(0);    // TODO: Vet this default.
            setUnknownBeta(0);     // TODO: Vet this default.
            setCloakTicksGoal(0);
            setCloakTicks(Integer.MIN_VALUE);
        }

        public void setUnknownAlpha(int n) {
            unknownAlpha = n;
        }

        public void setUnknownBeta(int n) {
            unknownBeta = n;
        }

        public int getUnknownAlpha() {
            return unknownAlpha;
        }

        public int getUnknownBeta() {
            return unknownBeta;
        }

        /**
         * Sets total time the cloak will stay engaged.
         * <p>
         * This can vary depending on the system level when the cloak is
         * initially engaged. When not engaged, this is 0.
         *
         * @see #setCloakTicks(int)
         */
        public void setCloakTicksGoal(int n) {
            cloakTicksGoal = n;
        }

        public int getCloakTicksGoal() {
            return cloakTicksGoal;
        }

        /**
         * Sets elapsed time while the cloak is engaged.
         * <p>
         * When this is not set, it is MIN_INT. After reaching or passing the
         * goal, this value lingers.
         *
         * @param n a positive int less than, or equal to, the goal (or MIN_INT)
         * @see #setCloakTicksGoal(int)
         */
        public void setCloakTicks(int n) {
            cloakTicks = n;
        }

        public int getCloakTicks() {
            return cloakTicks;
        }

        @Override
        public String toString() {
            return String.format("SystemId:                 %s%n", SystemType.CLOAKING.getId()) +
                    String.format("Alpha?:                 %7d%n", unknownAlpha) +
                    String.format("Beta?:                  %7d%n", unknownBeta) +
                    String.format("Cloak Ticks Goal:       %7d%n", cloakTicksGoal) +
                    String.format("Cloak Ticks:            %7s%n", (cloakTicks == Integer.MIN_VALUE ? "MIN" : cloakTicks));
        }
    }

    /**
     * Extended info about the Hacking system.
     *
     * @see DoorState
     * @see SystemState#setHacked(boolean)
     * @see SystemState#setHackLevel(int)
     */
    public static class HackingInfo extends ExtendedSystemInfo {
        private SystemType targetSystemType = null;
        private int unknownBeta = 0;
        private boolean dronePodVisible = false;
        private int unknownDelta = 0;

        private int unknownEpsilon = 0;
        private int unknownZeta = 0;
        private int unknownEta = 0;

        private int disruptionTicks = 0;
        private int disruptionTicksGoal = 10000;
        private boolean disrupting = false;

        private DronePodState dronePod = null;


        /**
         * Constructs an incomplete HackingInfo.
         * <p>
         * It will need a hacking DronePodState.
         */
        public HackingInfo() {
            super();
        }

        /**
         * Copy constructor.
         * <p>
         * The DronePodState will be copy-constructed as well.
         */
        protected HackingInfo(HackingInfo srcInfo) {
            super(srcInfo);
            targetSystemType = srcInfo.getTargetSystemType();
            unknownBeta = srcInfo.getUnknownBeta();
            dronePodVisible = srcInfo.isDronePodVisible();
            unknownDelta = srcInfo.getUnknownDelta();
            unknownEpsilon = srcInfo.getUnknownEpsilon();
            unknownZeta = srcInfo.getUnknownZeta();
            unknownEta = srcInfo.getUnknownEta();
            disruptionTicks = srcInfo.getDisruptionTicks();
            disruptionTicksGoal = srcInfo.getDisruptionTicksGoal();
            disrupting = srcInfo.isDisrupting();
            dronePod = new DronePodState(srcInfo.getDronePod());
        }

        @Override
        public HackingInfo copy() {
            return new HackingInfo(this);
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
            setTargetSystemType(null);
            setUnknownBeta(0);
            setDronePodVisible(false);
            setUnknownDelta(0);

            setUnknownEpsilon(0);
            setUnknownZeta(0);
            setUnknownEta(0);

            setDisruptionTicks(0);
            setDisruptionTicksGoal(10000);
            setDisrupting(false);

            if (getDronePod() != null) {
                getDronePod().commandeer();
            }
        }

        /**
         * Sets the target system to hack.
         * <p>
         * This is set when the drone pod is launched. Pressing the hack
         * button to select a system while paused will have no immediate
         * effect on a saved game; unpausing is necessary for tha pod to
         * launch and commit the changes.
         * <p>
         * Editing this value when a system had already been hacked will not
         * unhack the original system. Upon loading, FTL will modify the new
         * system's hacked and hackLevel values, reveal the room, lock the
         * doors, etc. If edited while disrupting, the previous system will
         * stay disrupted indefinitely. Only the current system will return to
         * normal when disruptionTicks reaches its goal.
         * <p>
         * FTL 1.5.13 bug: The hacking system only remembers the type of system
         * targeted, not a specific room. The rebel flagship has multiple
         * artillery rooms. An in-game choice to hack any of the right three
         * rooms will set the 'hacked' flag on that SystemState, but upon
         * reloading, the hacking system will seek the *first* artillery room
         * (the leftmost one) instead, which will get marked as 'hacked' and be
         * subject to disruption. The original room will still have its flag
         * lingering from before, but the hacking system only affects one room
         * and it already picked the left one. Both flagged rooms will be
         * revealed, but disruption will only affect only the left one.
         * <p>
         * When not set, this is null.
         */
        public void setTargetSystemType(SystemType systemType) {
            targetSystemType = systemType;
        }

        public SystemType getTargetSystemType() {
            return targetSystemType;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: Went from 0 to 1 when drone pod was launched.
         * Went from 1 to 0 when hacking system was inoperative (either from
         * damage or depowering) while the pod was still attached.
         */
        public void setUnknownBeta(int n) {
            unknownBeta = n;
        }

        public int getUnknownBeta() {
            return unknownBeta;
        }

        /**
         * Sets the drone pod's visibility.
         * <p>
         * Editing this to false after the drone pod has been launched will
         * only make the pod invisible. The Hacking system will continue to
         * function normally as if the pod were there.
         * <p>
         * Observed values: true (when launched), false (when the nearby ship
         * is defeated and has disappeared).
         */
        public void setDronePodVisible(boolean b) {
            dronePodVisible = b;
        }

        public boolean isDronePodVisible() {
            return dronePodVisible;
        }

        /**
         * Unknown.
         * <p>
         * Went from 0 to 1 when hacking drone pod was launched.
         */
        public void setUnknownDelta(int n) {
            unknownDelta = n;
        }

        public int getUnknownDelta() {
            return unknownDelta;
        }

        public void setUnknownEpsilon(int n) {
            unknownEpsilon = n;
        }

        public int getUnknownEpsilon() {
            return unknownEpsilon;
        }

        public void setUnknownZeta(int n) {
            unknownZeta = n;
        }

        public int getUnknownZeta() {
            return unknownZeta;
        }

        public void setUnknownEta(int n) {
            unknownEta = n;
        }

        public int getUnknownEta() {
            return unknownEta;
        }

        /**
         * Sets elapsed time while systems are disrupted.
         * <p>
         * When this is not set, it is 0. After reaching or passing the goal,
         * this value lingers.
         * <p>
         * When the goal is reached, the Hacking system will get 4 ionized bars
         * (ionized bars had been -1 while disrupting).
         *
         * @param n a positive int less than, or equal to, the goal
         * @see #setDisruptionTicksGoal(int)
         */
        public void setDisruptionTicks(int n) {
            disruptionTicks = n;
        }

        public int getDisruptionTicks() {
            return disruptionTicks;
        }

        /**
         * Sets total time systems will stay disrupted.
         * <p>
         * This can vary depending on the system level when disruption is
         * initially engaged. When not engaged, this is 10000!?
         *
         * @see #setDisruptionTicks(int)
         */
        public void setDisruptionTicksGoal(int n) {
            disruptionTicksGoal = n;
        }

        public int getDisruptionTicksGoal() {
            return disruptionTicksGoal;
        }

        /**
         * Sets whether an enemy system is currently being disrupted.
         *
         * @see SystemState.setHackLevel(int)
         */
        public void setDisrupting(boolean b) {
            disrupting = b;
        }

        public boolean isDisrupting() {
            return disrupting;
        }

        public void setDronePod(DronePodState pod) {
            dronePod = pod;
        }

        public DronePodState getDronePod() {
            return dronePod;
        }


        private String prettyInt(int n) {
            if (n == Integer.MIN_VALUE) return "MIN";
            if (n == Integer.MAX_VALUE) return "MAX";

            return String.format("%d", n);
        }


        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            boolean first = true;

            result.append(String.format("SystemId:                 %s%n", SystemType.HACKING.getId()));
            result.append(String.format("Target SystemId:          %s%n", (targetSystemType != null ? targetSystemType.getId() : "N/A")));
            result.append(String.format("Beta?:                  %7d%n", unknownBeta));
            result.append(String.format("Drone Pod Visible:      %7b%n", dronePodVisible));
            result.append(String.format("Delta?:                 %7d%n", unknownDelta));
            result.append(String.format("Epsilon?:               %7d%n", unknownEpsilon));
            result.append(String.format("Zeta?:                  %7d%n", unknownZeta));
            result.append(String.format("Eta?:                   %7d%n", unknownEta));
            result.append(String.format("Disruption Ticks:       %7d%n", disruptionTicks));
            result.append(String.format("Disruption Ticks Goal:  %7d%n", disruptionTicksGoal));
            result.append(String.format("Disrupting:             %7b%n", disrupting));

            result.append("\nDrone Pod...\n");
            if (dronePod != null) {
                result.append(dronePod.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            return result.toString();
        }
    }

    public static class MindInfo extends ExtendedSystemInfo {
        private int mindControlTicksGoal = 0;
        private int mindControlTicks = 0;


        /**
         * Constructor.
         */
        public MindInfo() {
            super();
        }

        /**
         * Copy constructor.
         */
        protected MindInfo(MindInfo srcInfo) {
            super(srcInfo);
            mindControlTicksGoal = srcInfo.getMindControlTicksGoal();
            mindControlTicks = srcInfo.getMindControlTicks();
        }

        @Override
        public MindInfo copy() {
            return new MindInfo(this);
        }

        /**
         * Resets aspects of an existing object to be viable for player use.
         * <p>
         * This will be called by the ship object when it is commandeered.
         */
        @Override
        public void commandeer() {
            setMindControlTicks(0);
            setMindControlTicksGoal(0);
        }

        /**
         * Sets elapsed time while crew are mind controlled.
         * <p>
         * After reaching or passing the goal, this value lingers.
         * <p>
         * When the goal is reached, the Mind system will get 4 ionized bars
         * (ionized bars had been -1 while disrupting).
         *
         * @param n a positive int less than, or equal to, the goal
         * @see #setMindControlTicksGoal(int)
         */
        public void setMindControlTicks(int n) {
            mindControlTicks = n;
        }

        public int getMindControlTicks() {
            return mindControlTicks;
        }

        /**
         * Sets total time crew will stay mind controlled.
         * <p>
         * This can vary depending on the system level when mind control is
         * initially engaged. When not engaged, this value lingers.
         *
         * @see #setMindControlTicks(int)
         */
        public void setMindControlTicksGoal(int n) {
            mindControlTicksGoal = n;
        }

        public int getMindControlTicksGoal() {
            return mindControlTicksGoal;
        }

        @Override
        public String toString() {
            return String.format("SystemId:                 %s%n", SystemType.MIND.getId()) +
                    String.format("Mind Ctrl Ticks:        %7d%n", mindControlTicks) +
                    String.format("Mind Ctrl Ticks Goal:   %7d%n", mindControlTicksGoal);
        }
    }

    public static class ArtilleryInfo extends ExtendedSystemInfo {
        private WeaponModuleState weaponMod = null;


        /**
         * Constructor.
         * <p>
         * It will need a WeaponModuleState.
         */
        public ArtilleryInfo() {
            super();
        }

        /**
         * Copy constructor.
         */
        protected ArtilleryInfo(ArtilleryInfo srcInfo) {
            super(srcInfo);
            weaponMod = new WeaponModuleState(srcInfo.getWeaponModule());
        }

        @Override
        public ArtilleryInfo copy() {
            return new ArtilleryInfo(this);
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
            if (getWeaponModule() != null) {
                getWeaponModule().commandeer();
            }
        }

        public void setWeaponModule(WeaponModuleState weaponMod) {
            this.weaponMod = weaponMod;
        }

        public WeaponModuleState getWeaponModule() {
            return weaponMod;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("SystemId:                 %s%n", SystemType.ARTILLERY.getId()));

            result.append("\nWeapon Module...\n");
            if (weaponMod != null) {
                result.append(weaponMod.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            return result.toString();
        }
    }


    public static enum ProjectileType {
        BEAM, BOMB, LASER_OR_BURST, MISSILE, ROCK_OR_EXPLOSION, PDS, INVALID
    }


    /**
     * Constants for projectile/damage ownership.
     * <p>
     * OwnerId (-1, 0, 1)
     */
    public static enum Affiliation {
        OTHER, PLAYER_SHIP, NEARBY_SHIP
    }


    public static class ProjectileState {
        private ProjectileType projectileType = ProjectileType.INVALID;
        private int currentPosX = 0, currentPosY = 0;
        private int prevPosX = 0, prevPosY = 0;
        private int speed = 0;
        private int goalPosX = 0, goalPosY = 0;
        private int heading = 0;
        private int ownerId = 0;
        private int selfId = 0;

        private DamageState damage = null;

        private int lifespan = 0;
        private int destinationSpace = 0;
        private int currentSpace = 0;
        private int targetId = 0;
        private boolean dead = false;

        private String deathAnimId = "";
        private String flightAnimId = "";

        private AnimState deathAnim = new AnimState();
        private AnimState flightAnim = new AnimState();

        private int velocityX = 0, velocityY = 0;
        private boolean missed = false;
        private boolean hitTarget = false;

        private String hitSolidSound = "";
        private String hitShieldSound = "";
        private String missSound = "";

        private int entryAngle = -1;  // Guess: X degrees CCW, where 0 is due East.
        private boolean startedDying = false;
        private boolean passedTarget = false;

        private int type = 0;
        private boolean broadcastTarget = false;

        private ExtendedProjectileInfo extendedInfo = null;


        /**
         * Constructs an incomplete ProjectileState.
         * <p>
         * It will need Damage and type-specific extended info.
         */
        public ProjectileState() {
        }

        /**
         * Copy constructor.
         * <p>
         * Each anim, Damage, and ExtendedProjectileInfo will be
         * copy-constructed as well.
         */
        public ProjectileState(ProjectileState srcProjectile) {
            projectileType = srcProjectile.getProjectileType();
            currentPosX = srcProjectile.getCurrentPositionX();
            currentPosY = srcProjectile.getCurrentPositionY();
            prevPosX = srcProjectile.getPreviousPositionX();
            prevPosY = srcProjectile.getPreviousPositionY();
            speed = srcProjectile.getSpeed();
            goalPosX = srcProjectile.getGoalPositionX();
            goalPosY = srcProjectile.getGoalPositionY();
            heading = srcProjectile.getHeading();
            ownerId = srcProjectile.getOwnerId();
            selfId = srcProjectile.getSelfId();

            damage = new DamageState(srcProjectile.getDamage());

            lifespan = srcProjectile.getLifespan();
            destinationSpace = srcProjectile.getDestinationSpace();
            currentSpace = srcProjectile.getCurrentSpace();
            targetId = srcProjectile.getTargetId();
            dead = srcProjectile.isDead();

            deathAnimId = srcProjectile.getDeathAnimId();
            flightAnimId = srcProjectile.getFlightAnimId();

            deathAnim = new AnimState(srcProjectile.getDeathAnim());
            flightAnim = new AnimState(srcProjectile.getFlightAnim());

            velocityX = srcProjectile.getVelocityX();
            velocityY = srcProjectile.getVelocityY();
            missed = srcProjectile.hasMissed();
            hitTarget = srcProjectile.hasHitTarget();

            hitSolidSound = srcProjectile.getHitSolidSound();
            hitShieldSound = srcProjectile.getHitShieldSound();
            missSound = srcProjectile.getMissSound();

            entryAngle = srcProjectile.getEntryAngle();
            startedDying = srcProjectile.hasStartedDying();
            passedTarget = srcProjectile.hasPassedTarget();

            type = srcProjectile.getType();
            broadcastTarget = srcProjectile.getBroadcastTarget();

            if (srcProjectile.getExtendedInfo(ExtendedProjectileInfo.class) != null) {
                extendedInfo = srcProjectile.getExtendedInfo(ExtendedProjectileInfo.class).copy();
            }
        }

        public void setProjectileType(ProjectileType t) {
            projectileType = t;
        }

        public ProjectileType getProjectileType() {
            return projectileType;
        }

        public void setCurrentPositionX(int n) {
            currentPosX = n;
        }

        public void setCurrentPositionY(int n) {
            currentPosY = n;
        }

        public int getCurrentPositionX() {
            return currentPosX;
        }

        public int getCurrentPositionY() {
            return currentPosY;
        }

        public void setPreviousPositionX(int n) {
            prevPosX = n;
        }

        public void setPreviousPositionY(int n) {
            prevPosY = n;
        }

        public int getPreviousPositionX() {
            return prevPosX;
        }

        public int getPreviousPositionY() {
            return prevPosY;
        }

        /**
         * Sets the projectile's speed.
         * <p>
         * This is a pseudo-float based on the 'speed' tag of the
         * WeaponBlueprint's xml.
         */
        public void setSpeed(int n) {
            speed = n;
        }

        public int getSpeed() {
            return speed;
        }

        public void setGoalPositionX(int n) {
            goalPosX = n;
        }

        public void setGoalPositionY(int n) {
            goalPosY = n;
        }

        public int getGoalPositionX() {
            return goalPosX;
        }

        public int getGoalPositionY() {
            return goalPosY;
        }

        /**
         * Set's the projectile's orientation.
         * <p>
         * MISSILE_2's image file points north.
         * A heading of 0 renders it pointing east.
         * A heading of 45 points southeast, pivoting the body around the tip.
         * A heading of 90 points south, with the body above the pivot point.
         *
         * @param n degrees clockwise (may be negative)
         */
        public void setHeading(int n) {
            heading = n;
        }

        public int getHeading() {
            return heading;
        }

        /**
         * Unknown.
         *
         * @param n player ship (0) or nearby ship (1), even for drones' projectiles
         */
        public void setOwnerId(int n) {
            ownerId = n;
        }

        public int getOwnerId() {
            return ownerId;
        }

        /**
         * Unknown.
         * <p>
         * A unique number for this projectile, presumably copied from some
         * global counter which increments with each new projectile.
         * <p>
         * The DamageState will usually have the same value set for its own
         * selfId. But not always!? Projectile type and pending/fired status are
         * not predictive.
         */
        public void setSelfId(int n) {
            selfId = n;
        }

        public int getSelfId() {
            return selfId;
        }

        public void setDamage(DamageState damage) {
            this.damage = damage;
        }

        public DamageState getDamage() {
            return damage;
        }

        /**
         * Unknown.
         * <p>
         * There doesn't appear to be a ticks field to track when to start
         * dying?
         */
        public void setLifespan(int n) {
            lifespan = n;
        }

        public int getLifespan() {
            return lifespan;
        }

        /**
         * Sets which ship to eventually use as the origin for position
         * coordinates.
         *
         * @param n player ship (0) or nearby ship (1)
         * @see #setCurrentSpace(int)
         */
        public void setDestinationSpace(int n) {
            destinationSpace = n;
        }

        public int getDestinationSpace() {
            return destinationSpace;
        }

        /**
         * Sets which ship to use as the origin for position coordinates.
         *
         * @param n player ship (0) or nearby ship (1)
         * @see #setDestinationSpace(int)
         */
        public void setCurrentSpace(int n) {
            currentSpace = n;
        }

        public int getCurrentSpace() {
            return currentSpace;
        }

        /**
         * Unknown.
         *
         * @param n player ship (0) or nearby ship (1)
         * @see #setDestinationSpace(int)
         * @see #setOwnerId(int)
         */
        public void setTargetId(int n) {
            targetId = n;
        }

        public int getTargetId() {
            return targetId;
        }

        public void setDead(boolean b) {
            dead = b;
        }

        public boolean isDead() {
            return dead;
        }

        public void setDeathAnimId(String s) {
            deathAnimId = s;
        }

        public String getDeathAnimId() {
            return deathAnimId;
        }

        /**
         * Sets an animSheet to play depcting the projectile in flight.
         * <p>
         * TODO: This has been observed as "" when it's an asteroid!?
         */
        public void setFlightAnimId(String s) {
            flightAnimId = s;
        }

        public String getFlightAnimId() {
            return flightAnimId;
        }

        /**
         * Sets the death anim state, played on impact.
         * <p>
         * TODO: Determine what happens when the projectile is shot.
         *
         * @see #setDeathAnimId(String)
         */
        public void setDeathAnim(AnimState anim) {
            deathAnim = anim;
        }

        public AnimState getDeathAnim() {
            return deathAnim;
        }

        /**
         * Sets the flight anim state, played while in transit.
         * <p>
         * Newly spawned projectiles, and pending ones that haven't been fired
         * yet, have their flightAnim's playing set to true.
         *
         * @see #setFlightAnimId(String)
         */
        public void setFlightAnim(AnimState anim) {
            flightAnim = anim;
        }

        public AnimState getFlightAnim() {
            return flightAnim;
        }

        public void setVelocityX(int n) {
            velocityX = n;
        }

        public void setVelocityY(int n) {
            velocityY = n;
        }

        public int getVelocityX() {
            return velocityX;
        }

        public int getVelocityY() {
            return velocityY;
        }

        /**
         * Sets whether this projectile will never hit its target.
         * <p>
         * FTL will mark it as missed before it passes its target. This is
         * probably set when it's created.
         *
         * @see #setPassedTarget(boolean)
         */
        public void setMissed(boolean b) {
            missed = b;
        }

        public boolean hasMissed() {
            return missed;
        }

        /**
         * Sets whether this projectile hit a target (even shields).
         */
        public void setHitTarget(boolean b) {
            hitTarget = b;
        }

        public boolean hasHitTarget() {
            return hitTarget;
        }

        /**
         * Sets the sound to play when this projectile hits something solid.
         * <p>
         * This will be a tag name from "sounds.xml", such as "hitHull2".
         */
        public void setHitSolidSound(String s) {
            hitSolidSound = s;
        }

        public String getHitSolidSound() {
            return hitSolidSound;
        }

        /**
         * Sets the sound to play when this projectile hits shields.
         * <p>
         * This will be a tag name from "sounds.xml", such as "hitShield3".
         */
        public void setHitShieldSound(String s) {
            hitShieldSound = s;
        }

        public String getHitShieldSound() {
            return hitShieldSound;
        }

        /**
         * Sets the sound to play when this projectile misses.
         * <p>
         * This will be a tag name from "sounds.xml", such as "miss".
         */
        public void setMissSound(String s) {
            missSound = s;
        }

        public String getMissSound() {
            return missSound;
        }

        /**
         * Unknown.
         * <p>
         * When not set, this is -1.
         */
        public void setEntryAngle(int n) {
            entryAngle = n;
        }

        public int getEntryAngle() {
            return entryAngle;
        }

        /**
         * Unknown.
         */
        public void setStartedDying(boolean b) {
            startedDying = b;
        }

        public boolean hasStartedDying() {
            return startedDying;
        }

        /**
         * Sets whether this projectile has passed its target.
         * <p>
         * FTL will have already marked it as having missed first.
         *
         * @see setMissed(boolean)
         */
        public void setPassedTarget(boolean b) {
            passedTarget = b;
        }

        public boolean hasPassedTarget() {
            return passedTarget;
        }

        public void setType(int n) {
            type = n;
        }

        public int getType() {
            return type;
        }

        /**
         * Sets whether a red dot should be painted at the targeted location.
         * <p>
         * This is used by burst volleys (e.g., flak).
         */
        public void setBroadcastTarget(boolean b) {
            broadcastTarget = b;
        }

        public boolean getBroadcastTarget() {
            return broadcastTarget;
        }

        public void setExtendedInfo(ExtendedProjectileInfo info) {
            extendedInfo = info;
        }

        public <T extends ExtendedProjectileInfo> T getExtendedInfo(Class<T> infoClass) {
            if (extendedInfo == null) return null;
            return infoClass.cast(extendedInfo);
        }


        private String prettyInt(int n) {
            if (n == Integer.MIN_VALUE) return "MIN";
            if (n == Integer.MAX_VALUE) return "MAX";

            return String.format("%d", n);
        }


        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("Projectile Type:   %s%n", projectileType.toString()));

            if (ProjectileType.INVALID.equals(projectileType)) {
                result.append("\n");
                result.append("(When Projectile Type is INVALID, no other fields are set.)\n");
                return result.toString();
            }

            result.append(String.format("Current Position:  %8d,%8d (%9.03f,%9.03f)%n", currentPosX, currentPosY, currentPosX / 1000f, currentPosY / 1000f));
            result.append(String.format("Previous Position: %8d,%8d (%9.03f,%9.03f)%n", prevPosX, prevPosY, prevPosX / 1000f, prevPosY / 1000f));
            result.append(String.format("Speed:             %8d (%7.03f)%n", speed, speed / 1000f));
            result.append(String.format("Goal Position:     %8d,%8d (%9.03f,%9.03f)%n", goalPosX, goalPosY, goalPosX / 1000f, goalPosY / 1000f));
            result.append(String.format("Heading:           %8d%n", heading));
            result.append(String.format("Owner Id?:         %8d%n", ownerId));
            result.append(String.format("Self Id?:          %8d%n", selfId));

            result.append(String.format("\nDamage...%n"));
            if (damage != null) {
                result.append(damage.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            result.append("\n");

            result.append(String.format("Lifespan:          %8d%n", lifespan));
            result.append(String.format("Destination Space: %8d%n", destinationSpace));
            result.append(String.format("Current Space:     %8d%n", currentSpace));
            result.append(String.format("Target Id?:        %8d%n", targetId));
            result.append(String.format("Dead:              %8b%n", dead));
            result.append(String.format("Death AnimId:      %s%n", deathAnimId));
            result.append(String.format("Flight AnimId:     %s%n", flightAnimId));

            result.append(String.format("\nDeath Anim?...%n"));
            if (deathAnim != null) {
                result.append(deathAnim.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            result.append(String.format("\nFlight Anim?...%n"));
            if (flightAnim != null) {
                result.append(flightAnim.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            result.append("\n");

            result.append(String.format("Velocity (x,y):    %8d,%6d (%7.03f,%7.03f)%n", velocityX, velocityY, velocityX / 1000f, velocityY / 1000f));
            result.append(String.format("Missed:            %8b%n", missed));
            result.append(String.format("Hit Target:        %8b%n", hitTarget));
            result.append(String.format("Hit Solid Sound:   %s%n", hitSolidSound));
            result.append(String.format("Hit Shield Sound:  %s%n", hitShieldSound));
            result.append(String.format("Miss Sound:        %s%n", missSound));
            result.append(String.format("Entry Angle?:      %8s%n", prettyInt(entryAngle)));
            result.append(String.format("Started Dying:     %8b%n", startedDying));
            result.append(String.format("Passed Target?:    %8b%n", passedTarget));

            result.append("\n");

            result.append(String.format("Type?:             %8d%n", type));
            result.append(String.format("Broadcast Target:  %8b (Red dot at targeted location)%n", broadcastTarget));

            result.append(String.format("\nExtended Projectile Info...%n"));
            if (extendedInfo != null) {
                result.append(extendedInfo.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            return result.toString();
        }
    }

    public static class DamageState {
        private int hullDamage = 0;
        private int shieldPiercing = 0;
        private int fireChance = 0;
        private int breachChance = 0;
        private int ionDamage = 0;
        private int systemDamage = 0;
        private int personnelDamage = 0;
        private boolean hullBuster = false;
        private int ownerId = -1;
        private int selfId = -1;
        private boolean lockdown = false;
        private boolean crystalShard = false;
        private int stunChance = 0;
        private int stunAmount = 0;


        /**
         * Constructor.
         */
        public DamageState() {
        }

        /**
         * Copy constructor.
         */
        public DamageState(DamageState srcDamage) {
            hullDamage = srcDamage.getHullDamage();
            shieldPiercing = srcDamage.getShieldPiercing();
            fireChance = srcDamage.getFireChance();
            breachChance = srcDamage.getBreachChance();
            ionDamage = srcDamage.getIonDamage();
            systemDamage = srcDamage.getSystemDamage();
            personnelDamage = srcDamage.getPersonnelDamage();
            boolean hullBuster = srcDamage.isHullBuster();
            ownerId = srcDamage.getOwnerId();
            selfId = srcDamage.getSelfId();
            lockdown = srcDamage.isLockdown();
            crystalShard = srcDamage.isCrystalShard();
            stunChance = srcDamage.getStunChance();
            stunAmount = srcDamage.getStunAmount();
        }

        public void setHullDamage(int n) {
            hullDamage = n;
        }

        public void setShieldPiercing(int n) {
            shieldPiercing = n;
        }

        public void setFireChance(int n) {
            fireChance = n;
        }

        public void setBreachChance(int n) {
            breachChance = n;
        }

        public void setIonDamage(int n) {
            ionDamage = n;
        }

        public void setSystemDamage(int n) {
            systemDamage = n;
        }

        public int getHullDamage() {
            return hullDamage;
        }

        public int getShieldPiercing() {
            return shieldPiercing;
        }

        public int getFireChance() {
            return fireChance;
        }

        public int getBreachChance() {
            return breachChance;
        }

        public int getIonDamage() {
            return ionDamage;
        }

        public int getSystemDamage() {
            return systemDamage;
        }

        /**
         * Sets damage to apply to personnel.
         * <p>
         * This is dealt per-square to each crew in the room hit. A Beam weapon
         * can injure someone twice if it follows them into another room.
         */
        public void setPersonnelDamage(int n) {
            personnelDamage = n;
        }

        public int getPersonnelDamage() {
            return personnelDamage;
        }

        /**
         * Toggles whether this projectile deals double hull damage against
         * systemless rooms.
         * <p>
         * This is based on the 'hullBust' tag (0/1) of a WeaponBlueprint's xml.
         */
        public void setHullBuster(boolean b) {
            hullBuster = b;
        }

        public boolean isHullBuster() {
            return hullBuster;
        }

        /**
         * Unknown.
         * <p>
         * When not set, this is -1.
         * <p>
         * This only seems to be set by projectiles from bomb weapons: 1 when
         * from the nearby ship, once it materializes (-1 a moment before).
         */
        public void setOwnerId(int n) {
            ownerId = n;
        }

        public int getOwnerId() {
            return ownerId;
        }

        /**
         * Unknown.
         * <p>
         * When not set, this is -1.
         */
        public void setSelfId(int n) {
            selfId = n;
        }

        public int getSelfId() {
            return selfId;
        }

        public void setLockdown(boolean b) {
            lockdown = b;
        }

        public void setCrystalShard(boolean b) {
            crystalShard = b;
        }

        public void setStunChance(int n) {
            stunChance = n;
        }

        public void setStunAmount(int n) {
            stunAmount = n;
        }

        public boolean isLockdown() {
            return lockdown;
        }

        public boolean isCrystalShard() {
            return crystalShard;
        }

        public int getStunChance() {
            return stunChance;
        }

        public int getStunAmount() {
            return stunAmount;
        }

        @Override
        public String toString() {
            return String.format("Hull Damage:       %7d%n", hullDamage) +
                    String.format("ShieldPiercing:    %7d%n", shieldPiercing) +
                    String.format("Fire Chance:       %7d%n", fireChance) +
                    String.format("Breach Chance:     %7d%n", breachChance) +
                    String.format("Ion Damage:        %7d%n", ionDamage) +
                    String.format("System Damage:     %7d%n", systemDamage) +
                    String.format("Personnel Damage:  %7d%n", personnelDamage) +
                    String.format("Hull Buster:       %7b (2x Hull damage vs systemless rooms)%n", hullBuster) +
                    String.format("Owner Id?:         %7d%n", ownerId) +
                    String.format("Self Id?:          %7d%n", selfId) +
                    String.format("Lockdown:          %7b%n", lockdown) +
                    String.format("Crystal Shard:     %7b%n", crystalShard) +
                    String.format("Stun Chance:       %7d%n", stunChance) +
                    String.format("Stun Amount:       %7d%n", stunAmount);
        }
    }

    public static abstract class ExtendedProjectileInfo {

        protected ExtendedProjectileInfo() {
        }

        protected ExtendedProjectileInfo(ExtendedProjectileInfo srcInfo) {
        }

        /**
         * Blindly copy-constructs objects.
         * <p>
         * Subclasses override this with return values of their own type.
         */
        public abstract ExtendedProjectileInfo copy();
    }

    public static class EmptyProjectileInfo extends ExtendedProjectileInfo {

        /**
         * Constructor.
         */
        public EmptyProjectileInfo() {
            super();
        }

        /**
         * Copy constructor.
         */
        protected EmptyProjectileInfo(EmptyProjectileInfo srcInfo) {
            super(srcInfo);
        }

        @Override
        public EmptyProjectileInfo copy() {
            return new EmptyProjectileInfo(this);
        }

        @Override
        public String toString() {
            return "N/A\n";
        }
    }

    public static class IntegerProjectileInfo extends ExtendedProjectileInfo {
        private int[] unknownAlpha;

        /**
         * Constructs an incomplete IntegerProjectileInfo.
         * <p>
         * A number of integers equal to size will need to be set.
         */
        public IntegerProjectileInfo(int size) {
            super();
            unknownAlpha = new int[size];
        }

        /**
         * Copy constructor.
         */
        protected IntegerProjectileInfo(IntegerProjectileInfo srcInfo) {
            super(srcInfo);
            unknownAlpha = new int[srcInfo.getSize()];
            for (int i = 0; i < unknownAlpha.length; i++) {
                unknownAlpha[i] = srcInfo.get(i);
            }
        }

        @Override
        public IntegerProjectileInfo copy() {
            return new IntegerProjectileInfo(this);
        }

        public int getSize() {
            return unknownAlpha.length;
        }

        public void set(int index, int n) {
            unknownAlpha[index] = n;
        }

        public int get(int index) {
            return unknownAlpha[index];
        }


        private String prettyInt(int n) {
            if (n == Integer.MIN_VALUE) return "MIN";
            if (n == Integer.MAX_VALUE) return "MAX";

            return String.format("%d", n);
        }


        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("Type:               Unknown Info%n"));

            result.append(String.format("\nAlpha?...%n"));
            for (int i = 0; i < unknownAlpha.length; i++) {
                result.append(String.format("%7s", prettyInt(unknownAlpha[i])));

                if (i != unknownAlpha.length - 1) {
                    if (i % 2 == 1) {
                        result.append(",\n");
                    } else {
                        result.append(", ");
                    }
                }
            }
            result.append("\n");

            return result.toString();
        }
    }

    /**
     * Extended info for Beam projectiles.
     * <p>
     * Beam projectiles have several parts.
     * An emission line, drawn from the weapon.
     * A strafe line, drawn toward the target ship.
     * A spot, where the strafe line hits the target ship.
     * A swath, the path the spot tries to travel along.
     * <p>
     * For ship weapons, the emission line ends off-screen, and the strafe line
     * begins somewhere off-screen. For Beam drones, the emission line is
     * ignored, and the strafe line is drawn from the drone pod directly to the
     * swath.
     * <p>
     * The ProjectileState's current/previous position is the emission line's
     * source - at the weapon or drone pod that fired. The ProjectileState's
     * goal position is where the spot is, along the swath (shield blocking is
     * not considered).
     */
    public static class BeamProjectileInfo extends ExtendedProjectileInfo {
        private int emissionEndX = 0, emissionEndY = 0;
        private int strafeSourceX = 0, strafeSourceY = 0;
        private int strafeEndX = 0, strafeEndY = 0;
        private int unknownBetaX = 0, unknownBetaY = 0;
        private int swathEndX = 0, swathEndY = 0;
        private int swathStartX = 0, swathStartY = 0;
        private int unknownGamma = 0;
        private int swathLength = 0;
        private int unknownDelta = 0;
        private int unknownEpsilonX = 0, unknownEpsilonY = 0;
        private int unknownZeta = 0;
        private int unknownEta = 0;
        private int emissionAngle = 0;
        private boolean unknownIota = false;
        private boolean unknownKappa = false;
        private boolean fromDronePod = false;
        private boolean unknownMu = false;
        private boolean unknownNu = false;


        /**
         * Constructor.
         */
        public BeamProjectileInfo() {
            super();
        }

        /**
         * Copy constructor.
         */
        protected BeamProjectileInfo(BeamProjectileInfo srcInfo) {
            super(srcInfo);
            emissionEndX = srcInfo.getEmissionEndX();
            emissionEndY = srcInfo.getEmissionEndY();
            strafeSourceX = srcInfo.getStrafeSourceX();
            strafeSourceY = srcInfo.getStrafeSourceY();
            strafeEndX = srcInfo.getStrafeEndX();
            strafeEndY = srcInfo.getStrafeEndY();
            unknownBetaX = srcInfo.getUnknownBetaX();
            unknownBetaY = srcInfo.getUnknownBetaY();
            swathEndX = srcInfo.getSwathEndX();
            swathEndY = srcInfo.getSwathEndY();
            swathStartX = srcInfo.getSwathStartX();
            swathStartY = srcInfo.getSwathStartY();
            unknownGamma = srcInfo.getUnknownGamma();
            swathLength = srcInfo.getSwathLength();
            unknownDelta = srcInfo.getUnknownDelta();
            unknownEpsilonX = srcInfo.getUnknownEpsilonX();
            unknownEpsilonY = srcInfo.getUnknownEpsilonY();
            unknownZeta = srcInfo.getUnknownZeta();
            unknownEta = srcInfo.getUnknownEta();
            emissionAngle = srcInfo.getEmissionAngle();
            unknownIota = srcInfo.getUnknownIota();
            unknownKappa = srcInfo.getUnknownKappa();
            fromDronePod = srcInfo.isFromDronePod();
            unknownMu = srcInfo.getUnknownMu();
            unknownNu = srcInfo.getUnknownNu();
        }

        @Override
        public BeamProjectileInfo copy() {
            return new BeamProjectileInfo(this);
        }

        /**
         * Sets the off-screen endpoint of the line drawn from the weapon.
         * <p>
         * For Beam drones, this point will be the same as strafeSource, except
         * this y will be shifted upward by -2000. The emission line won't be
         * drawn in that case, obvisously, since the drone is right there.
         * <p>
         * This is relative to the ship space the beam was emitted from
         * (e.g., weapon of a player ship, or a drone hovering over a nearby
         * ship).
         */
        public void setEmissionEndX(int n) {
            emissionEndX = n;
        }

        public void setEmissionEndY(int n) {
            emissionEndY = n;
        }

        public int getEmissionEndX() {
            return emissionEndX;
        }

        public int getEmissionEndY() {
            return emissionEndY;
        }

        /**
         * Sets the off-screen endpoint of the line drawn toward the swath.
         * <p>
         * This is relative to the target ship.
         */
        public void setStrafeSourceX(int n) {
            strafeSourceX = n;
        }

        public void setStrafeSourceY(int n) {
            strafeSourceY = n;
        }

        public int getStrafeSourceX() {
            return strafeSourceX;
        }

        public int getStrafeSourceY() {
            return strafeSourceY;
        }

        /**
         * Sets the on-screen endpoint of the line drawn toward the swath.
         * <p>
         * When shields are up, this point is not on the swath but at the
         * intersection of the line and shield oval.
         * <p>
         * This is relative to the target ship.
         */
        public void setStrafeEndX(int n) {
            strafeEndX = n;
        }

        public void setStrafeEndY(int n) {
            strafeEndY = n;
        }

        public int getStrafeEndX() {
            return strafeEndX;
        }

        public int getStrafeEndY() {
            return strafeEndY;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: The current location of the travelling spot.
         * <p>
         * This is relative to the target ship.
         */
        public void setUnknownBetaX(int n) {
            unknownBetaX = n;
        }

        public void setUnknownBetaY(int n) {
            unknownBetaY = n;
        }

        public int getUnknownBetaX() {
            return unknownBetaX;
        }

        public int getUnknownBetaY() {
            return unknownBetaY;
        }

        /**
         * Sets the point the travelling spot will end at.
         * <p>
         * This is relative to the target ship.
         */
        public void setSwathEndX(int n) {
            swathEndX = n;
        }

        public void setSwathEndY(int n) {
            swathEndY = n;
        }

        public int getSwathEndX() {
            return swathEndX;
        }

        public int getSwathEndY() {
            return swathEndY;
        }

        /**
         * Sets the point the travelling spot will start from.
         * <p>
         * This is relative to the target ship.
         */
        public void setSwathStartX(int n) {
            swathStartX = n;
        }

        public void setSwathStartY(int n) {
            swathStartY = n;
        }

        public int getSwathStartX() {
            return swathStartX;
        }

        public int getSwathStartY() {
            return swathStartY;
        }

        /**
         * Unknown.
         * <p>
         * This is always 1000.
         */
        public void setUnknownGamma(int n) {
            unknownGamma = n;
        }

        public int getUnknownGamma() {
            return unknownGamma;
        }

        /**
         * Unknown.
         * <p>
         * This is a pseudo-float based on the 'length' tag of the
         * WeaponBlueprint's xml.
         */
        public void setSwathLength(int n) {
            swathLength = n;
        }

        public int getSwathLength() {
            return swathLength;
        }

        /**
         * Unknown.
         * <p>
         * This is a constant, at least for a given WeaponBlueprint.
         */
        public void setUnknownDelta(int n) {
            unknownDelta = n;
        }

        public int getUnknownDelta() {
            return unknownDelta;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: The current location of the travelling spot.
         * <p>
         * This is relative to the target ship.
         */
        public void setUnknownEpsilonX(int n) {
            unknownEpsilonX = n;
        }

        public void setUnknownEpsilonY(int n) {
            unknownEpsilonY = n;
        }

        public int getUnknownEpsilonX() {
            return unknownEpsilonX;
        }

        public int getUnknownEpsilonY() {
            return unknownEpsilonY;
        }

        /**
         * Unknown.
         * <p>
         * This is an erratic int (seen 0-350) with no clear progression from
         * moment to moment.
         */
        public void setUnknownZeta(int n) {
            unknownZeta = n;
        }

        public int getUnknownZeta() {
            return unknownZeta;
        }

        /**
         * Unknown.
         * <p>
         * Possibly damage per room, based on the 'damage' tag of the
         * WeaponBlueprint's xml? (That's DamageState's hullDamage)
         * <p>
         * Observed values: 1, 2.
         */
        public void setUnknownEta(int n) {
            unknownEta = n;
        }

        public int getUnknownEta() {
            return unknownEta;
        }

        /**
         * Sets the angle of the line drawn from the weapon.
         * <p>
         * For ships, this will be 0 (player ship) or 270000 (nearby ship).
         * <p>
         * For Beam drones, this is related to the turret angle, though this
         * may be a large negative angle while the turret may be a small
         * positive one.
         * <p>
         * Observed values: 0, 270000, -323106.
         *
         * @param n a pseudo-float (n degrees clockwise from east)
         */
        public void setEmissionAngle(int n) {
            emissionAngle = n;
        }

        public int getEmissionAngle() {
            return emissionAngle;
        }

        /**
         * Unknown.
         */
        public void setUnknownIota(boolean b) {
            unknownIota = b;
        }

        public boolean getUnknownIota() {
            return unknownIota;
        }

        /**
         * Unknown.
         * <p>
         * Seems to be true only when the target ship's shields are down, and
         * the line will reach the swath without being blocked (even set while
         * pending).
         */
        public void setUnknownKappa(boolean b) {
            unknownKappa = b;
        }

        public boolean getUnknownKappa() {
            return unknownKappa;
        }

        /**
         * Sets whether this this beam was fired from a drone pod or a ship
         * weapon.
         * <p>
         * For ship weapons, this is false, and both the emission and strafe
         * lines will be drawn.
         * <p>
         * If true, only the strafe line be drawn - from the ProjectileState's
         * current position (the drone's aperture).
         * <p>
         * If edited to false on a drone, the emission line will be drawn,
         * northward, with no strafe line - completely missing the target ship.
         * This weirdness may have to to with current/destination space not
         * being separate, as they would be for a ship weapon?
         */
        public void setFromDronePod(boolean b) {
            fromDronePod = b;
        }

        public boolean isFromDronePod() {
            return fromDronePod;
        }

        /**
         * Unknown.
         */
        public void setUnknownMu(boolean b) {
            unknownMu = b;
        }

        public boolean getUnknownMu() {
            return unknownMu;
        }

        /**
         * Unknown.
         * <p>
         * Might have to do with the line having hit crew?
         */
        public void setUnknownNu(boolean b) {
            unknownNu = b;
        }

        public boolean getUnknownNu() {
            return unknownNu;
        }


        @Override
        public String toString() {
            return "Type:               Beam Info%n" +
                    String.format("Emission End:       %8d,%8d (%9.03f,%9.03f) (Off-screen endpoint of line from weapon)%n", emissionEndX, emissionEndY, emissionEndX / 1000f, emissionEndY / 1000f) +
                    String.format("Strafe Source:      %8d,%8d (%9.03f,%9.03f) (Off-screen endpoint of line drawn toward swath)%n", strafeSourceX, strafeSourceY, strafeSourceX / 1000f, strafeSourceY / 1000f) +
                    String.format("Strafe End:         %8d,%8d (%9.03f,%9.03f) (On-screen endpoint of line drawn toward swath)%n", strafeEndX, strafeEndY, strafeEndX / 1000f, strafeEndY / 1000f) +
                    String.format("Beta?:              %8d,%8d (%9.03f,%9.03f)%n", unknownBetaX, unknownBetaY, unknownBetaX / 1000f, unknownBetaY / 1000f) +
                    String.format("Swath End:          %8d,%8d (%9.03f,%9.03f)%n", swathEndX, swathEndY, swathEndX / 1000f, swathEndY / 1000f) +
                    String.format("Swath Start:        %8d,%8d (%9.03f,%9.03f)%n", swathStartX, swathStartY, swathStartX / 1000f, swathStartY / 1000f) +
                    String.format("Gamma?:             %8d%n", unknownGamma) +
                    String.format("Swath Length:       %8d (%9.03f)%n", swathLength, swathLength / 1000f) +
                    String.format("Delta?:             %8d%n", unknownDelta) +
                    String.format("Epsilon?:           %8d,%8d (%9.03f,%9.03f)%n", unknownEpsilonX, unknownEpsilonY, unknownEpsilonX / 1000f, unknownEpsilonY / 1000f) +
                    String.format("Zeta?:              %8d%n", unknownZeta) +
                    String.format("Eta?:               %8d%n", unknownEta) +
                    String.format("Emission Angle:     %8d%n", emissionAngle) +
                    String.format("Iota?:              %8b%n", unknownIota) +
                    String.format("Kappa?:             %8b%n", unknownKappa) +
                    String.format("From Drone Pod:     %8b%n", fromDronePod) +
                    String.format("Mu?:                %8b%n", unknownMu) +
                    String.format("Nu?:                %8b%n", unknownNu);
        }
    }

    public static class BombProjectileInfo extends ExtendedProjectileInfo {
        private int unknownAlpha = 0;
        private int fuseTicks = 400;
        private int unknownGamma = 0;
        private int unknownDelta = 0;
        private boolean arrived = false;


        /**
         * Constructor.
         */
        public BombProjectileInfo() {
            super();
        }

        /**
         * Copy constructor.
         */
        protected BombProjectileInfo(BombProjectileInfo srcInfo) {
            super(srcInfo);
            unknownAlpha = srcInfo.getUnknownAlpha();
            fuseTicks = srcInfo.getFuseTicks();
            unknownGamma = srcInfo.getUnknownGamma();
            unknownDelta = srcInfo.getUnknownDelta();
            arrived = srcInfo.hasArrived();
        }

        @Override
        public BombProjectileInfo copy() {
            return new BombProjectileInfo(this);
        }

        /**
         * Unknown.
         * <p>
         * Observed values: 0.
         */
        public void setUnknownAlpha(int n) {
            unknownAlpha = n;
        }

        public int getUnknownAlpha() {
            return unknownAlpha;
        }

        /**
         * Sets time elapsed while this bomb is about to detonate.
         * <p>
         * After fading into a room, this decrements ticks from 400. At 0, the
         * bomb's casing (flightAnimId) disappears, and the explosion anim
         * plays. This value continues decrementing to some negative number
         * (varies by weapon), until the explosion completes, and the
         * projectile is gone.
         * <p>
         * Changing this from a positive value to a higher one will delay the
         * detonation. Once negative, the explosion will have already started,
         * and setting a new positive value will only make the casing visible
         * amidst the blast for whatever time is left of that animation.
         * <p>
         * Observed values: 400 (During fade-in), 356, -205, -313, -535.
         *
         * @see #setArrived(boolean)
         */
        public void setFuseTicks(int n) {
            fuseTicks = n;
        }

        public int getFuseTicks() {
            return fuseTicks;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: 0.
         */
        public void setUnknownGamma(int n) {
            unknownGamma = n;
        }

        public int getUnknownGamma() {
            return unknownGamma;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: 0.
         */
        public void setUnknownDelta(int n) {
            unknownDelta = n;
        }

        public int getUnknownDelta() {
            return unknownDelta;
        }

        /**
         * Sets whether this bomb has begun fading in.
         * <p>
         * When FTL sees this is false, the value will become true, and the
         * bomb will begin fading into a room. If set to false once more, the
         * fade will start over. Fuse ticks will cease decrementing while this
         * is false or while the fade is still in progress.
         * <p>
         * When FTL sees this is true, the bomb casing is fully opaque and fuse
         * ticks immediately resume decrementing.
         * <p>
         * Newly spawned projectiles have this set to false.
         *
         * @see #setFuseTicks(int)
         */
        public void setArrived(boolean b) {
            arrived = b;
        }

        public boolean hasArrived() {
            return arrived;
        }

        @Override
        public String toString() {
            return "Type:               Bomb Info\n" +
                    String.format("Alpha?:             %7d%n", unknownAlpha) +
                    String.format("Fuse Ticks:         %7d (Explodes at 0)%n", fuseTicks) +
                    String.format("Gamma?:             %7d%n", unknownGamma) +
                    String.format("Delta?:             %7d%n", unknownDelta) +
                    String.format("Arrived:            %7b%n", arrived);
        }
    }

    public static class LaserProjectileInfo extends ExtendedProjectileInfo {
        private int unknownAlpha = 0;
        private int spin = 0;

        // This class represents projectiles from both Laser and Burst weapons.

        /**
         * Constructor.
         */
        public LaserProjectileInfo() {
            super();
        }

        /**
         * Copy constructor.
         */
        protected LaserProjectileInfo(LaserProjectileInfo srcInfo) {
            super(srcInfo);
            unknownAlpha = srcInfo.getUnknownAlpha();
            spin = srcInfo.getSpin();
        }

        @Override
        public LaserProjectileInfo copy() {
            return new LaserProjectileInfo(this);
        }

        /**
         * Unknown.
         * <p>
         * Observed values: For burst, it varies in the range 100000-3000000.
         * Some kind of seed? For regular lasers, it is 0.
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
         * This is a pseudo-float based on the 'spin' tag of the
         * WeaponBlueprint's xml (burst-type weapons), if present, or 0.
         */
        public void setSpin(int n) {
            spin = n;
        }

        public int getSpin() {
            return spin;
        }

        @Override
        public String toString() {
            return "Type:               Laser/Burst Info\n" +
                    String.format("Alpha?:             %7d%n", unknownAlpha) +
                    String.format("Spin:               %7d%n", spin);
        }
    }


    /**
     * Extended info for PDS projectiles (called ASB in-game).
     * <p>
     * This was introduced in FTL 1.6.1.
     */
    public static class PDSProjectileInfo extends ExtendedProjectileInfo {
        private int unknownAlpha = 0;
        private int unknownBeta = 0;
        private int unknownGamma = 0;
        private int unknownDelta = 0;
        private int unknownEpsilon = 0;
        private AnimState unknownZeta = new AnimState();

        // This class represents projectiles from PDS hazards.

        /**
         * Constructor.
         */
        public PDSProjectileInfo() {
            super();
        }

        /**
         * Copy constructor.
         */
        protected PDSProjectileInfo(PDSProjectileInfo srcInfo) {
            super(srcInfo);
            unknownAlpha = srcInfo.getUnknownAlpha();
            unknownBeta = srcInfo.getUnknownBeta();
            unknownGamma = srcInfo.getUnknownGamma();
            unknownDelta = srcInfo.getUnknownDelta();
            unknownEpsilon = srcInfo.getUnknownEpsilon();
            unknownZeta = srcInfo.getUnknownZeta();
        }

        @Override
        public PDSProjectileInfo copy() {
            return new PDSProjectileInfo(this);
        }

        /**
         * Unknown.
         * <p>
         * Seems to be the spawn X position relative to ship space?
         * <p>
         * This is a pseudo-float.
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
         * Seems to be the spawn Y position relative to ship space?
         * <p>
         * This is a pseudo-float.
         */
        public void setUnknownBeta(int n) {
            unknownBeta = n;
        }

        public int getUnknownBeta() {
            return unknownBeta;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: 1, 0.
         */
        public void setUnknownGamma(int n) {
            unknownGamma = n;
        }

        public int getUnknownGamma() {
            return unknownGamma;
        }

        /**
         * Unknown.
         * <p>
         * Always matches the projectile's flightAnim scale.
         * <p>
         * Observed values: 10277; 11438, 15690, 19896, 26832, 34719; 1139.
         */
        public void setUnknownDelta(int n) {
            unknownDelta = n;
        }

        public int getUnknownDelta() {
            return unknownDelta;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: 0.
         */
        public void setUnknownEpsilon(int n) {
            unknownEpsilon = n;
        }

        public int getUnknownEpsilon() {
            return unknownEpsilon;
        }

        /**
         * Unknown.
         * <p>
         * Observed values:
         * Looping: 0.
         * Frame: 0, 2, 4, 5, 8.
         * Progress Ticks: 0, 245, 467, 648, 892.
         * Scale: 1.0
         * Position: (0, 0), (213000, 213000).
         */
        public void setUnknownZeta(AnimState anim) {
            unknownZeta = anim;
        }

        public AnimState getUnknownZeta() {
            return unknownZeta;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("Type:               PDS Info%n"));
            result.append(String.format("Alpha?:             %7d%n", unknownAlpha));
            result.append(String.format("Beta?:              %7d%n", unknownBeta));
            result.append(String.format("Gamma?:             %7d%n", unknownGamma));
            result.append(String.format("Delta?:             %7d%n", unknownDelta));
            result.append(String.format("Epsilon?:           %7d%n", unknownEpsilon));

            result.append("\nZeta? Anim...\n");
            if (unknownZeta != null) {
                result.append(unknownZeta.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            return result.toString();
        }
    }


    public static class DronePodState {
        private DroneType droneType = null;
        private int mourningTicks = 0;
        private int currentSpace = 0;
        private int destinationSpace = -1;
        private int currentPosX = 0, currentPosY = 0;
        private int prevPosX = 0, prevPosY = 0;
        private int goalPosX = 0, goalPosY = 0;

        // This block was formerly a length 6 array named beta.
        private int unknownEpsilon = Integer.MIN_VALUE;
        private int unknownZeta = Integer.MIN_VALUE;
        private int nextTargetX = Integer.MIN_VALUE;
        private int nextTargetY = Integer.MIN_VALUE;
        private int unknownIota = Integer.MIN_VALUE;
        private int unknownKappa = Integer.MIN_VALUE;

        // This block was formerly a length 14 array named gamma.
        private int buildupTicks = -1000;
        private int stationaryTicks = 0;
        private int cooldownTicks = 0;
        private int orbitAngle = 0;
        private int turretAngle = 0;
        private int unknownXi = 0;
        private int hopsToLive = Integer.MAX_VALUE;
        private int unknownPi = 0;
        private int unknownRho = 0;
        private int overloadTicks = 0;
        private int unknownTau = -1000;
        private int unknownUpsilon = 0;
        private int deltaPosX = 0, deltaPosY = 0;

        private AnimState deathAnim = new AnimState();
        private ExtendedDronePodInfo extendedInfo = null;


        /**
         * Constructs an incomplete DronePodState.
         */
        public DronePodState() {
        }

        /**
         * Copy constructor.
         */
        public DronePodState(DronePodState srcPod) {
            droneType = srcPod.getDroneType();
            mourningTicks = srcPod.getMourningTicks();
            currentSpace = srcPod.getCurrentSpace();
            destinationSpace = srcPod.getDestinationSpace();
            currentPosX = srcPod.getCurrentPositionX();
            currentPosY = srcPod.getCurrentPositionY();
            prevPosX = srcPod.getPreviousPositionX();
            prevPosY = srcPod.getPreviousPositionY();
            goalPosX = srcPod.getGoalPositionX();
            goalPosY = srcPod.getGoalPositionY();

            unknownEpsilon = srcPod.getUnknownEpsilon();
            unknownZeta = srcPod.getUnknownZeta();
            nextTargetX = srcPod.getNextTargetX();
            nextTargetY = srcPod.getNextTargetY();
            unknownIota = srcPod.getUnknownIota();
            unknownKappa = srcPod.getUnknownKappa();

            buildupTicks = srcPod.getBuildupTicks();
            stationaryTicks = srcPod.getStationaryTicks();
            cooldownTicks = srcPod.getCooldownTicks();
            orbitAngle = srcPod.getOrbitAngle();
            turretAngle = srcPod.getTurretAngle();
            unknownXi = srcPod.getUnknownXi();
            hopsToLive = srcPod.getHopsToLive();
            unknownPi = srcPod.getUnknownPi();
            unknownRho = srcPod.getUnknownRho();
            overloadTicks = srcPod.getOverloadTicks();
            unknownTau = srcPod.getUnknownTau();
            unknownUpsilon = srcPod.getUnknownUpsilon();
            deltaPosX = srcPod.getDeltaPositionX();
            deltaPosY = srcPod.getDeltaPositionY();

            deathAnim = srcPod.getDeathAnim();

            if (srcPod.getExtendedInfo(ExtendedDronePodInfo.class) != null) {
                extendedInfo = srcPod.getExtendedInfo(ExtendedDronePodInfo.class).copy();
            }
        }


        /**
         * Resets aspects of an existing object to be viable for player use.
         * <p>
         * This will be called by the ship object when it is commandeered.
         * <p>
         * Warning: Dangerous while values remain undeciphered.
         */
        public void commandeer() {
            setMourningTicks(0);
            setCurrentSpace(0);
            setDestinationSpace(-1);

            //setNextTargetX( Integer.MIN_VALUE )?
            //setNextTargetY( Integer.MIN_VALUE )?

            setBuildupTicks(-1000);
            setStationaryTicks(0);

            setOverloadTicks(0);

            // TODO: Unknowns.

            getDeathAnim().setPlaying(false);
            getDeathAnim().setCurrentFrame(0);
            getDeathAnim().setProgressTicks(0);

            if (getExtendedInfo(ExtendedDronePodInfo.class) != null) {
                getExtendedInfo(ExtendedDronePodInfo.class).commandeer();
            }
        }


        public void setDroneType(DroneType droneType) {
            this.droneType = droneType;
        }

        public DroneType getDroneType() {
            return droneType;
        }

        /**
         * Sets time elapsed while this drone is un-redeployable after
         * destruction.
         * <p>
         * This value begins decrementing from 10000 when the drone pod is
         * destroyed and the deathAnim completes. After reaching or passing 0,
         * this value lingers.
         */
        public void setMourningTicks(int n) {
            mourningTicks = n;
        }

        public int getMourningTicks() {
            return mourningTicks;
        }

        /**
         * Sets which ship to use as the origin for position coordinates.
         *
         * @param n player ship (0) or nearby ship (1)
         * @see #setDestinationSpace(int)
         */
        public void setCurrentSpace(int n) {
            currentSpace = n;
        }

        public int getCurrentSpace() {
            return currentSpace;
        }

        /**
         * Sets which ship to eventually use as the origin for position
         * coordinates.
         * <p>
         * This value is initially -1. It is set to one of the ships when the
         * drone pod is deployed. Then this value lingers.
         *
         * @param n player ship (0) or nearby ship (1) or none (-1)
         * @see #setCurrentSpace(int)
         */
        public void setDestinationSpace(int n) {
            destinationSpace = n;
        }

        public int getDestinationSpace() {
            return destinationSpace;
        }

        public void setCurrentPositionX(int n) {
            currentPosX = n;
        }

        public void setCurrentPositionY(int n) {
            currentPosY = n;
        }

        public int getCurrentPositionX() {
            return currentPosX;
        }

        public int getCurrentPositionY() {
            return currentPosY;
        }

        public void setPreviousPositionX(int n) {
            prevPosX = n;
        }

        public void setPreviousPositionY(int n) {
            prevPosY = n;
        }

        public int getPreviousPositionX() {
            return prevPosX;
        }

        public int getPreviousPositionY() {
            return prevPosY;
        }

        public void setGoalPositionX(int n) {
            goalPosX = n;
        }

        public void setGoalPositionY(int n) {
            goalPosY = n;
        }

        public int getGoalPositionX() {
            return goalPosX;
        }

        public int getGoalPositionY() {
            return goalPosY;
        }


        /**
         * Unknown.
         * <p>
         * Observed values: Always MIN_INT?
         */
        public void setUnknownEpsilon(int n) {
            unknownEpsilon = n;
        }

        public int getUnknownEpsilon() {
            return unknownEpsilon;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: Always MIN_INT?
         */
        public void setUnknownZeta(int n) {
            unknownZeta = n;
        }

        public int getUnknownZeta() {
            return unknownZeta;
        }

        /**
         * Sets the position where this drone's next projectile will end up.
         * <p>
         * This is analogous to weapon targeting reticles deciding the next
         * target. The drone may spin or maneuver in transit, but it will
         * ultimately turn to face this point before firing.
         * <p>
         * Combat/Ship_Repair drones set a new value after each shot. Defense
         * drones intermittently set it (e.g., directly at an opposing drone)
         * and unset it.
         * <p>
         * When not set, this is MIN_INT.
         *
         * @param n a pseudo-float
         * @see WeaponModuleState#setCurrentTargets(List)
         */
        public void setNextTargetX(int n) {
            nextTargetX = n;
        }

        public void setNextTargetY(int n) {
            nextTargetY = n;
        }

        public int getNextTargetX() {
            return nextTargetX;
        }

        public int getNextTargetY() {
            return nextTargetY;
        }

        /**
         * Unknown.
         * <p>
         * When not set, this is MIN_INT.
         * <p>
         * Observed values: Defense (erratic +/- 0-20000); 962, 144, -988.
         */
        public void setUnknownIota(int n) {
            unknownIota = n;
        }

        public int getUnknownIota() {
            return unknownIota;
        }

        /**
         * Unknown.
         * <p>
         * When not set, this is MIN_INT.
         * <p>
         * Observed values: Defense (erratic +/- 0-20000); -2384, 26, 2373.
         */
        public void setUnknownKappa(int n) {
            unknownKappa = n;
        }

        public int getUnknownKappa() {
            return unknownKappa;
        }


        /**
         * Sets time elapsed while this drone is about to fire.
         * <p>
         * Drones telegraph when they're about to fire, a light will change
         * color (COMBAT_1) or glow intensely (COMBAT_BEAM). While positive,
         * this value decrements to 0. At that point, this is set to -1000,
         * firing occurs and a projectile is launched.
         * <p>
         * Observed values: 365, 43 (COMBAT_BEAM); 500 (when a launched Hacking
         * drone entered the target ship space).
         * <p>
         * When not set, this is -1000.
         */
        public void setBuildupTicks(int n) {
            buildupTicks = n;
        }

        public int getBuildupTicks() {
            return buildupTicks;
        }

        /**
         * Sets time elapsed while this drone is stationary.
         * <p>
         * While positive, this value decrements to 0. The drone will be
         * completely still for the duration. Beam drones set this when
         * buildupTicks reaches 0, so the drone will hold still to be the
         * beam's origin point.
         * <p>
         * This works on Combat drones, too, if edited.
         * <p>
         * Observed values: 470, 322, 167 (COMBAT_BEAM).
         * <p>
         * When not set, this is 0.
         */
        public void setStationaryTicks(int n) {
            stationaryTicks = n;
        }

        public int getStationaryTicks() {
            return stationaryTicks;
        }

        /**
         * Sets time elapsed while this drone is unable to shoot again after
         * firing.
         * <p>
         * This is based on the 'cooldown' tag of the DroneBlueprint's xml.
         * <p>
         * While positive, for Defense and Shield drones, this value decrements
         * to 0. The drone will be passive (e.g., not firing) for the duration.
         * After reaching or passing 0, this is set to -1000.
         * <p>
         * A Defense drone's light will turn red while passive (as opposed to
         * green).
         * <p>
         * Combat and Beam drones leave this at the xml's value without ever
         * decrementing.
         * <p>
         * When not set, this is 0.
         * <p>
         * TODO: Check Hacking and Ship_Repair drones.
         */
        public void setCooldownTicks(int n) {
            cooldownTicks = n;
        }

        public int getCooldownTicks() {
            return cooldownTicks;
        }

        /**
         * Sets the drone's orbital progress around the shield ellipse.
         * <p>
         * Drones which do not orbit will have some lingering value instead.
         * <p>
         * TODO: Modify this value in the editor. In CheatEngine, changing
         * this has no effect, appearing to be read-only field for reference.
         *
         * @param n a pseudo-float (n degrees clockwise from east)
         */
        public void setOrbitAngle(int n) {
            orbitAngle = n;
        }

        public int getOrbitAngle() {
            return orbitAngle;
        }

        /**
         * Sets the drone's turret angle.
         * <p>
         * When not set, this is 0.
         *
         * @param n a pseudo-float (n degrees clockwise from east)
         */
        public void setTurretAngle(int n) {
            turretAngle = n;
        }

        public int getTurretAngle() {
            return turretAngle;
        }


        /**
         * Unknown.
         * <p>
         * Might be facing, to rotate the entire drone?
         * <p>
         * Observed values: Hacking (U:-89 L:179 R:8.745 D:89); Combat drones
         * have strange values. Boarder (Ion Drone) body in flight is rotated
         * as expected (Eastward:0 SW:121), and turret its value is synched.
         * <p>
         * When not set, this is 0.
         *
         * @param n a pseudo-float (n degrees clockwise from east)
         */
        public void setUnknownXi(int n) {
            unknownXi = n;
        }

        public int getUnknownXi() {
            return unknownXi;
        }

        /**
         * Sets the number of waypoints this drone should arrive at before
         * disappearing.
         * <p>
         * This value decrements the moment this drone finishes idling at one
         * waypoint and begins moving toward the next. After reaching 0, the
         * drone vanishes. Then this value lingers.
         * <p>
         * When not set, this is MAX_INT.
         * <p>
         * Observed values: 4, 3, 2, 1, 0 (Ship_Repair).
         */
        public void setHopsToLive(int n) {
            hopsToLive = n;
        }

        public int getHopsToLive() {
            return hopsToLive;
        }

        public void setUnknownPi(int n) {
            unknownPi = n;
        }

        public int getUnknownPi() {
            return unknownPi;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: 1, 0.
         */
        public void setUnknownRho(int n) {
            unknownRho = n;
        }

        public int getUnknownRho() {
            return unknownRho;
        }

        /**
         * Sets time elapsed while this drone is stunned, with a chance of
         * exploding.
         * <p>
         * This value begins decrementing from a positive integer after taking
         * ion damage (e.g., from an Anti-Combat Drone). After reaching 0, the
         * drone returns to normal.
         * <p>
         * While stunned, the drone will halt movement, it'll be covered in
         * arcs of electricity, and the turret will spin rapidly. It may
         * explode at a random moment prior to reaching 0 - at which point,
         * this value will be set to 0.
         * <p>
         * When not set, this is 0. This value lingers and may even end up a
         * little negative.
         * <p>
         * Observed values: 4378 (Combat drone shot by Anti-Combat Drone)
         * <p>
         * TODO: It's unclear what determines if/when an explosion occurs.
         */
        public void setOverloadTicks(int n) {
            overloadTicks = n;
        }

        public int getOverloadTicks() {
            return overloadTicks;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: -5704; -173834; 110067, 230637.
         * <p>
         * When not set, this is -1000.
         */
        public void setUnknownTau(int n) {
            unknownTau = n;
        }

        public int getUnknownTau() {
            return unknownTau;
        }

        /**
         * Unknown.
         * <p>
         * Observed values: 1.
         */
        public void setUnknownUpsilon(int n) {
            unknownUpsilon = n;
        }

        public int getUnknownUpsilon() {
            return unknownUpsilon;
        }

        /**
         * Sets the recent change in position (Current - Previous + 1).
         * <p>
         * TODO: Modify this value in the editor. In CheatEngine, changing
         * this has no effect, appearing to be read-only field for reference.
         *
         * @param n a pseudo-float
         * @see #setCurrentPositionX(int)
         * @see #setPreviousPositionX(int)
         */
        public void setDeltaPositionX(int n) {
            deltaPosX = n;
        }

        public void setDeltaPositionY(int n) {
            deltaPosY = n;
        }

        public int getDeltaPositionX() {
            return deltaPosX;
        }

        public int getDeltaPositionY() {
            return deltaPosY;
        }


        public void setDeathAnim(AnimState anim) {
            deathAnim = anim;
        }

        public AnimState getDeathAnim() {
            return deathAnim;
        }

        public void setExtendedInfo(ExtendedDronePodInfo info) {
            extendedInfo = info;
        }

        public <T extends ExtendedDronePodInfo> T getExtendedInfo(Class<T> infoClass) {
            if (extendedInfo == null) return null;
            return infoClass.cast(extendedInfo);
        }


        private String prettyInt(int n) {
            if (n == Integer.MIN_VALUE) return "MIN";
            if (n == Integer.MAX_VALUE) return "MAX";

            return String.format("%d", n);
        }


        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            boolean first = true;

            result.append(String.format("Drone Type:        %s%n", droneType.getId()));
            result.append(String.format("Mourning Ticks:    %7d (Decrements to 0 from 10000)%n", mourningTicks));
            result.append(String.format("Current Space:     %7d%n", currentSpace));
            result.append(String.format("Destination Space: %7d%n", destinationSpace));
            result.append(String.format("Current Position:  %7s,%7s%n", prettyInt(currentPosX), prettyInt(currentPosY)));
            result.append(String.format("Previous Position: %7s,%7s%n", prettyInt(prevPosX), prettyInt(prevPosY)));
            result.append(String.format("Goal Position:     %7s,%7s%n", prettyInt(goalPosX), prettyInt(goalPosY)));

            result.append(String.format("%n"));
            result.append(String.format("Epsilon?, Zeta?:   %7s,%7s%n", prettyInt(unknownEpsilon), prettyInt(unknownZeta)));
            result.append(String.format("Next Target:       %7s,%7s%n", prettyInt(nextTargetX), prettyInt(nextTargetY)));
            result.append(String.format("Iota?, Kappa?:     %7s,%7s%n", prettyInt(unknownIota), prettyInt(unknownKappa)));

            result.append(String.format("%n"));
            result.append(String.format("Buildup Ticks:     %7d (Decrements to 0 while about to fire)%n", buildupTicks));
            result.append(String.format("Stationary Ticks:  %7d (Decrements to 0 while stationary)%n", stationaryTicks));
            result.append(String.format("Cooldown Ticks:    %7d (Decrements to 0 while passive, Defense/Shield only)%n", cooldownTicks));
            result.append(String.format("Orbit Angle:       %7d%n", orbitAngle));
            result.append(String.format("Turret Angle:      %7d%n", turretAngle));
            result.append(String.format("Xi?:               %7d%n", unknownXi));
            result.append(String.format("Hops to Live:      %7s (Waypoints to idle at before undeploying)%n", prettyInt(hopsToLive)));
            result.append(String.format("Pi?:               %7d%n", unknownPi));
            result.append(String.format("Rho?:              %7d%n", unknownRho));
            result.append(String.format("Overload Ticks:    %7d (Decrements to 0 while shocked by ion weapons)%n", overloadTicks));
            result.append(String.format("Tau?:              %7d%n", unknownTau));
            result.append(String.format("Upsilon?:          %7d%n", unknownUpsilon));
            result.append(String.format("Delta Position:    %7d,%7d (Current - Previous + 1)%n", deltaPosX, deltaPosY));

            result.append("\nDeath Anim...\n");
            if (deathAnim != null) {
                result.append(deathAnim.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            result.append("\nExtended Drone Pod Info... (Varies by Drone Type)\n");
            if (extendedInfo != null) {
                result.append(extendedInfo.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            }

            return result.toString();
        }
    }


    /**
     * Extra drone info stored separately from the original DroneState.
     * <p>
     * This was introduced in FTL 1.5.4.
     */
    public static class ExtendedDroneInfo {
        private boolean deployed = false;
        private boolean armed = false;
        private DronePodState dronePod = null;


        /**
         * Constructs an incomplete ExtendedDroneInfo.
         * <p>
         * It will need a DronePodState.
         */
        public ExtendedDroneInfo() {
        }

        /**
         * Copy-constructor.
         * <p>
         * The drone pod will be copy-constructed as well.
         */
        public ExtendedDroneInfo(ExtendedDroneInfo srcInfo) {
            deployed = srcInfo.isDeployed();
            armed = srcInfo.isArmed();

            if (srcInfo.getDronePod() != null) {
                dronePod = new DronePodState(srcInfo.getDronePod());
            }
        }


        /**
         * Resets aspects of an existing object to be viable for player use.
         * <p>
         * This will be called by the ship object when it is commandeered.
         * <p>
         * Warning: Dangerous while values remain undeciphered.
         * TODO: Recurse into all nested objects.
         */
        public void commandeer() {
            setDeployed(false);
            setArmed(false);

            if (getDronePod() != null) {
                getDronePod().commandeer();
            }
        }

        /**
         * Sets whether the drone's body/pod exists.
         * <p>
         * Re-arming an already deployed drone doesn't cost a drone part.
         * <p>
         * After defeating a nearby ship, and the window disappears, player
         * drone pods there are lost and this is set to false.
         */
        public void setDeployed(boolean b) {
            deployed = b;
        }

        public boolean isDeployed() {
            return deployed;
        }

        /**
         * Sets whether this drone is powered.
         * <p>
         * TODO: See what happens when this conflists with the DroneState.
         *
         * @see DroneState#setArmed(boolean)
         */
        public void setArmed(boolean b) {
            armed = b;
        }

        public boolean isArmed() {
            return armed;
        }

        /**
         * Sets a drone pod, which varies by DroneType.
         * <p>
         * For BATTLE and REPAIR, this should be null.
         */
        public void setDronePod(DronePodState pod) {
            dronePod = pod;
        }

        public DronePodState getDronePod() {
            return dronePod;
        }


        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append(String.format("Deployed:        %5b%n", deployed));
            result.append(String.format("Armed:           %5b%n", armed));

            result.append("\nDrone Pod...\n");
            if (dronePod != null) {
                result.append(dronePod.toString().replaceAll("(^|\n)(.+)", "$1  $2"));
            } else {
                result.append("N/A\n");
            }

            return result.toString();
        }
    }


    public static class StandaloneDroneState {
        private String droneId = null;
        private DronePodState dronePod = null;
        private int unknownAlpha = 0;
        private int unknownBeta = 0;
        private int unknownGamma = 0;


        /**
         * Constructs an incomplete StandaloneDroneState.
         */
        public StandaloneDroneState() {
        }

        public void setDroneId(String s) {
            droneId = s;
        }

        public String getDroneId() {
            return droneId;
        }

        public void setDronePod(DronePodState pod) {
            dronePod = pod;
        }

        public DronePodState getDronePod() {
            return dronePod;
        }

        public void setUnknownAlpha(int n) {
            unknownAlpha = n;
        }

        public void setUnknownBeta(int n) {
            unknownBeta = n;
        }

        public void setUnknownGamma(int n) {
            unknownGamma = n;
        }

        public int getUnknownAlpha() {
            return unknownAlpha;
        }

        public int getUnknownBeta() {
            return unknownBeta;
        }

        public int getUnknownGamma() {
            return unknownGamma;
        }


        @Override
        public String toString() {
            return String.format("DroneId:           %s%n", droneId) +
                    "\nDrone Pod...\n" +
                    dronePod.toString().replaceAll("(^|\n)(.+)", "$1  $2") +
                    "\n" +
                    String.format("Alpha?:            %3d%n", unknownAlpha) +
                    String.format("Beta?:             %3d%n", unknownBeta) +
                    String.format("Gamma?:            %3d%n", unknownGamma);
        }
    }


    private int readMinMaxedInt(InputStream in) throws IOException {
        int n = readInt(in);

        if (n == -2147483648) {
            n = Integer.MIN_VALUE;
        } else if (n == 2147483647) {
            n = Integer.MAX_VALUE;
        }

        return n;
    }

    private void writeMinMaxedInt(OutputStream out, int n) throws IOException {
        if (n == Integer.MIN_VALUE) {
            n = -2147483648;
        } else if (n == Integer.MAX_VALUE) {
            n = 2147483647;
        }

        writeInt(out, n);
    }

    /**
     * Reads additional fields of various ship-related classes.
     * <p>
     * This method does not involve a dedicated class.
     */
    private void readExtendedShipInfo(FileInputStream in, ShipState shipState, int fileFormat) throws IOException {
        // There is no explicit list count for drones.
        for (DroneState drone : shipState.getDroneList()) {
            ExtendedDroneInfo droneInfo = new ExtendedDroneInfo();

            droneInfo.setDeployed(readBool(in));
            droneInfo.setArmed(readBool(in));

            String droneId = drone.getDroneId();
            DroneBlueprint droneBlueprint = DataManager.get().getDrone(droneId);
            if (droneBlueprint == null) throw new IOException("Unrecognized DroneBlueprint: " + droneId);

            DroneType droneType = DroneType.findById(droneBlueprint.getType());
            if (droneType == null)
                throw new IOException(String.format("DroneBlueprint \"%s\" has an unrecognized type: %s", droneId, droneBlueprint.getType()));

            if (DroneType.REPAIR.equals(droneType) ||
                    DroneType.BATTLE.equals(droneType)) {
                // No drone pod for these types.
            } else {
                DronePodState dronePod = readDronePod(in, droneType);
                droneInfo.setDronePod(dronePod);
            }

            drone.setExtendedDroneInfo(droneInfo);
        }

        SystemState hackingState = shipState.getSystem(SystemType.HACKING);
        if (hackingState != null && hackingState.getCapacity() > 0) {
            HackingInfo hackingInfo = new HackingInfo();

            int targetSystemTypeFlag = readInt(in);
            SystemType targetSystemType;
            if (targetSystemTypeFlag == -1) targetSystemType = null;
            else if (targetSystemTypeFlag == 0) targetSystemType = SystemType.SHIELDS;
            else if (targetSystemTypeFlag == 1) targetSystemType = SystemType.ENGINES;
            else if (targetSystemTypeFlag == 2) targetSystemType = SystemType.OXYGEN;
            else if (targetSystemTypeFlag == 3) targetSystemType = SystemType.WEAPONS;
            else if (targetSystemTypeFlag == 4) targetSystemType = SystemType.DRONE_CTRL;
            else if (targetSystemTypeFlag == 5) targetSystemType = SystemType.MEDBAY;
            else if (targetSystemTypeFlag == 6) targetSystemType = SystemType.PILOT;
            else if (targetSystemTypeFlag == 7) targetSystemType = SystemType.SENSORS;
            else if (targetSystemTypeFlag == 8) targetSystemType = SystemType.DOORS;
            else if (targetSystemTypeFlag == 9) targetSystemType = SystemType.TELEPORTER;
            else if (targetSystemTypeFlag == 10) targetSystemType = SystemType.CLOAKING;
            else if (targetSystemTypeFlag == 11) targetSystemType = SystemType.ARTILLERY;
            else if (targetSystemTypeFlag == 12) targetSystemType = SystemType.BATTERY;
            else if (targetSystemTypeFlag == 13) targetSystemType = SystemType.CLONEBAY;
            else if (targetSystemTypeFlag == 14) targetSystemType = SystemType.MIND;
            else if (targetSystemTypeFlag == 15) targetSystemType = SystemType.HACKING;
            else {
                throw new IOException(String.format("Unsupported hacking targetSystemTypeFlag: %d", targetSystemTypeFlag));
            }
            hackingInfo.setTargetSystemType(targetSystemType);

            hackingInfo.setUnknownBeta(readInt(in));
            hackingInfo.setDronePodVisible(readBool(in));
            hackingInfo.setUnknownDelta(readInt(in));

            hackingInfo.setDisruptionTicks(readInt(in));
            hackingInfo.setDisruptionTicksGoal(readInt(in));

            hackingInfo.setDisrupting(readBool(in));

            DronePodState dronePod = readDronePod(in, DroneType.HACKING);  // The hacking drone.
            hackingInfo.setDronePod(dronePod);

            shipState.addExtendedSystemInfo(hackingInfo);
        }

        SystemState mindState = shipState.getSystem(SystemType.MIND);
        if (mindState != null && mindState.getCapacity() > 0) {
            MindInfo mindInfo = new MindInfo();

            mindInfo.setMindControlTicks(readInt(in));
            mindInfo.setMindControlTicksGoal(readInt(in));

            shipState.addExtendedSystemInfo(mindInfo);
        }

        SystemState weaponsState = shipState.getSystem(SystemType.WEAPONS);
        if (weaponsState != null && weaponsState.getCapacity() > 0) {

            int weaponCount = shipState.getWeaponList().size();
            int weaponModCount = readInt(in);
            if (weaponModCount != weaponCount) {
                throw new IOException(String.format("Found %d WeaponModules, but there are %d Weapons.", weaponModCount, weaponCount));
            }

            for (WeaponState weapon : shipState.getWeaponList()) {
                WeaponModuleState weaponMod = readWeaponModule(in, fileFormat);
                weapon.setWeaponModule(weaponMod);
            }
        }

        // Get ALL artillery rooms' SystemStates from the ShipState.
        List<SystemState> artilleryStateList = shipState.getSystems(SystemType.ARTILLERY);
        for (SystemState artilleryState : artilleryStateList) {

            if (artilleryState.getCapacity() > 0) {
                ArtilleryInfo artilleryInfo = new ArtilleryInfo();

                artilleryInfo.setWeaponModule(readWeaponModule(in, fileFormat));

                shipState.addExtendedSystemInfo(artilleryInfo);
            }
        }

        // A list of standalone drones, for flagship swarms. Always 0 for player.

        int standaloneDroneCount = readInt(in);
        for (int i = 0; i < standaloneDroneCount; i++) {
            String droneId = readString(in);
            DroneBlueprint droneBlueprint = DataManager.get().getDrone(droneId);
            if (droneBlueprint == null) throw new IOException("Unrecognized DroneBlueprint: " + droneId);

            StandaloneDroneState standaloneDrone = new StandaloneDroneState();
            standaloneDrone.setDroneId(droneId);

            DroneType droneType = DroneType.findById(droneBlueprint.getType());
            if (droneType == null)
                throw new IOException(String.format("DroneBlueprint \"%s\" has an unrecognized type: %s", droneId, droneBlueprint.getType()));

            DronePodState dronePod = readDronePod(in, droneType);
            standaloneDrone.setDronePod(dronePod);

            standaloneDrone.setUnknownAlpha(readInt(in));
            standaloneDrone.setUnknownBeta(readInt(in));
            standaloneDrone.setUnknownGamma(readInt(in));

            shipState.addStandaloneDrone(standaloneDrone);
        }
    }

    /**
     * Writes additional fields of various ship-related classes.
     * <p>
     * This method does not involve a dedicated class.
     */
    public void writeExtendedShipInfo(OutputStream out, ShipState shipState, int fileFormat) throws IOException {
        // There is no explicit list count for drones.
        for (DroneState drone : shipState.getDroneList()) {
            ExtendedDroneInfo droneInfo = drone.getExtendedDroneInfo();
            writeBool(out, droneInfo.isDeployed());
            writeBool(out, droneInfo.isArmed());

            if (droneInfo.getDronePod() != null) {
                writeDronePod(out, droneInfo.getDronePod());
            }
        }

        SystemState hackingState = shipState.getSystem(SystemType.HACKING);
        if (hackingState != null && hackingState.getCapacity() > 0) {
            // TODO: Compare system room count with extended info count.

            HackingInfo hackingInfo = shipState.getExtendedSystemInfo(HackingInfo.class);
            // This should not be null.

            SystemType targetSystemType = hackingInfo.getTargetSystemType();
            int targetSystemTypeFlag;
            if (targetSystemType == null) targetSystemTypeFlag = -1;
            else if (SystemType.SHIELDS.equals(targetSystemType)) targetSystemTypeFlag = 0;
            else if (SystemType.ENGINES.equals(targetSystemType)) targetSystemTypeFlag = 1;
            else if (SystemType.OXYGEN.equals(targetSystemType)) targetSystemTypeFlag = 2;
            else if (SystemType.WEAPONS.equals(targetSystemType)) targetSystemTypeFlag = 3;
            else if (SystemType.DRONE_CTRL.equals(targetSystemType)) targetSystemTypeFlag = 4;
            else if (SystemType.MEDBAY.equals(targetSystemType)) targetSystemTypeFlag = 5;
            else if (SystemType.PILOT.equals(targetSystemType)) targetSystemTypeFlag = 6;
            else if (SystemType.SENSORS.equals(targetSystemType)) targetSystemTypeFlag = 7;
            else if (SystemType.DOORS.equals(targetSystemType)) targetSystemTypeFlag = 8;
            else if (SystemType.TELEPORTER.equals(targetSystemType)) targetSystemTypeFlag = 9;
            else if (SystemType.CLOAKING.equals(targetSystemType)) targetSystemTypeFlag = 10;
            else if (SystemType.ARTILLERY.equals(targetSystemType)) targetSystemTypeFlag = 11;
            else if (SystemType.BATTERY.equals(targetSystemType)) targetSystemTypeFlag = 12;
            else if (SystemType.CLONEBAY.equals(targetSystemType)) targetSystemTypeFlag = 13;
            else if (SystemType.MIND.equals(targetSystemType)) targetSystemTypeFlag = 14;
            else if (SystemType.HACKING.equals(targetSystemType)) targetSystemTypeFlag = 15;
            else {
                throw new IOException(String.format("Unsupported hacking targetSystemType: %s", targetSystemType.getId()));
            }
            writeInt(out, targetSystemTypeFlag);

            writeInt(out, hackingInfo.getUnknownBeta());
            writeBool(out, hackingInfo.isDronePodVisible());
            writeInt(out, hackingInfo.getUnknownDelta());

            writeInt(out, hackingInfo.getDisruptionTicks());
            writeInt(out, hackingInfo.getDisruptionTicksGoal());

            writeBool(out, hackingInfo.isDisrupting());

            writeDronePod(out, hackingInfo.getDronePod());
        }

        SystemState mindState = shipState.getSystem(SystemType.MIND);
        if (mindState != null && mindState.getCapacity() > 0) {
            MindInfo mindInfo = shipState.getExtendedSystemInfo(MindInfo.class);
            // This should not be null.
            writeInt(out, mindInfo.getMindControlTicks());
            writeInt(out, mindInfo.getMindControlTicksGoal());
        }

        // If there's a Weapons system, write the weapon modules (even if there are 0 of them).
        SystemState weaponsState = shipState.getSystem(SystemType.WEAPONS);
        if (weaponsState != null && weaponsState.getCapacity() > 0) {

            int weaponCount = shipState.getWeaponList().size();
            writeInt(out, weaponCount);
            for (WeaponState weapon : shipState.getWeaponList()) {
                writeWeaponModule(out, weapon.getWeaponModule(), fileFormat);
            }
        }

        List<ArtilleryInfo> artilleryInfoList = shipState.getExtendedSystemInfoList(ArtilleryInfo.class);
        for (ArtilleryInfo artilleryInfo : artilleryInfoList) {
            writeWeaponModule(out, artilleryInfo.getWeaponModule(), fileFormat);
        }

        writeInt(out, shipState.getStandaloneDroneList().size());
        for (StandaloneDroneState standaloneDrone : shipState.getStandaloneDroneList()) {
            writeString(out, standaloneDrone.getDroneId());

            writeDronePod(out, standaloneDrone.getDronePod());

            writeInt(out, standaloneDrone.getUnknownAlpha());
            writeInt(out, standaloneDrone.getUnknownBeta());
            writeInt(out, standaloneDrone.getUnknownGamma());
        }
    }

    private DronePodState readDronePod(FileInputStream in, DroneType droneType) throws IOException {
        if (droneType == null) throw new IllegalArgumentException("DroneType cannot be null.");

        //log.debug( String.format( "Drone Pod: @%d", in.getChannel().position() ) );

        DronePodState dronePod = new DronePodState();
        dronePod.setDroneType(droneType);
        dronePod.setMourningTicks(readInt(in));
        dronePod.setCurrentSpace(readInt(in));
        dronePod.setDestinationSpace(readInt(in));

        dronePod.setCurrentPositionX(readMinMaxedInt(in));
        dronePod.setCurrentPositionY(readMinMaxedInt(in));
        dronePod.setPreviousPositionX(readMinMaxedInt(in));
        dronePod.setPreviousPositionY(readMinMaxedInt(in));
        dronePod.setGoalPositionX(readMinMaxedInt(in));
        dronePod.setGoalPositionY(readMinMaxedInt(in));

        dronePod.setUnknownEpsilon(readMinMaxedInt(in));
        dronePod.setUnknownZeta(readMinMaxedInt(in));
        dronePod.setNextTargetX(readMinMaxedInt(in));
        dronePod.setNextTargetY(readMinMaxedInt(in));
        dronePod.setUnknownIota(readMinMaxedInt(in));
        dronePod.setUnknownKappa(readMinMaxedInt(in));

        dronePod.setBuildupTicks(readInt(in));
        dronePod.setStationaryTicks(readInt(in));
        dronePod.setCooldownTicks(readInt(in));
        dronePod.setOrbitAngle(readInt(in));
        dronePod.setTurretAngle(readInt(in));
        dronePod.setUnknownXi(readInt(in));
        dronePod.setHopsToLive(readMinMaxedInt(in));
        dronePod.setUnknownPi(readInt(in));
        dronePod.setUnknownRho(readInt(in));
        dronePod.setOverloadTicks(readInt(in));
        dronePod.setUnknownTau(readInt(in));
        dronePod.setUnknownUpsilon(readInt(in));
        dronePod.setDeltaPositionX(readInt(in));
        dronePod.setDeltaPositionY(readInt(in));

        dronePod.setDeathAnim(readAnim(in));

        ExtendedDronePodInfo extendedInfo = null;
        if (DroneType.BOARDER.equals(droneType)) {
            BoarderDronePodInfo boarderPodInfo = new BoarderDronePodInfo();
            boarderPodInfo.setUnknownAlpha(readInt(in));
            boarderPodInfo.setUnknownBeta(readInt(in));
            boarderPodInfo.setUnknownGamma(readInt(in));
            boarderPodInfo.setUnknownDelta(readInt(in));
            boarderPodInfo.setBodyHealth(readInt(in));
            boarderPodInfo.setBodyX(readInt(in));
            boarderPodInfo.setBodyY(readInt(in));
            boarderPodInfo.setBodyRoomId(readInt(in));
            boarderPodInfo.setBodyRoomSquare(readInt(in));
            extendedInfo = boarderPodInfo;
        } else if (DroneType.HACKING.equals(droneType)) {
            HackingDronePodInfo hackingPodInfo = new HackingDronePodInfo();
            hackingPodInfo.setAttachPositionX(readInt(in));
            hackingPodInfo.setAttachPositionY(readInt(in));
            hackingPodInfo.setUnknownGamma(readInt(in));
            hackingPodInfo.setUnknownDelta(readInt(in));
            hackingPodInfo.setLandingAnim(readAnim(in));
            hackingPodInfo.setExtensionAnim(readAnim(in));
            extendedInfo = hackingPodInfo;
        } else if (DroneType.COMBAT.equals(droneType) ||
                DroneType.BEAM.equals(droneType)) {

            ZigZagDronePodInfo zigPodInfo = new ZigZagDronePodInfo();
            zigPodInfo.setLastWaypointX(readInt(in));
            zigPodInfo.setLastWaypointY(readInt(in));
            zigPodInfo.setTransitTicks(readMinMaxedInt(in));
            zigPodInfo.setExhaustAngle(readMinMaxedInt(in));
            zigPodInfo.setUnknownEpsilon(readMinMaxedInt(in));
            extendedInfo = zigPodInfo;
        } else if (DroneType.DEFENSE.equals(droneType)) {
            extendedInfo = new EmptyDronePodInfo();
        } else if (DroneType.SHIELD.equals(droneType)) {
            ShieldDronePodInfo shieldPodInfo = new ShieldDronePodInfo();
            shieldPodInfo.setUnknownAlpha(readInt(in));
            extendedInfo = shieldPodInfo;
        } else if (DroneType.SHIP_REPAIR.equals(droneType)) {
            ZigZagDronePodInfo zigPodInfo = new ZigZagDronePodInfo();
            zigPodInfo.setLastWaypointX(readInt(in));
            zigPodInfo.setLastWaypointY(readInt(in));
            zigPodInfo.setTransitTicks(readMinMaxedInt(in));
            zigPodInfo.setExhaustAngle(readMinMaxedInt(in));
            zigPodInfo.setUnknownEpsilon(readMinMaxedInt(in));
            extendedInfo = zigPodInfo;
        } else {
            throw new IOException("Unsupported droneType for drone pod: " + droneType.getId());
        }

        dronePod.setExtendedInfo(extendedInfo);

        return dronePod;
    }

    public void writeDronePod(OutputStream out, DronePodState dronePod) throws IOException {
        writeInt(out, dronePod.getMourningTicks());
        writeInt(out, dronePod.getCurrentSpace());
        writeInt(out, dronePod.getDestinationSpace());

        writeMinMaxedInt(out, dronePod.getCurrentPositionX());
        writeMinMaxedInt(out, dronePod.getCurrentPositionY());
        writeMinMaxedInt(out, dronePod.getPreviousPositionX());
        writeMinMaxedInt(out, dronePod.getPreviousPositionY());
        writeMinMaxedInt(out, dronePod.getGoalPositionX());
        writeMinMaxedInt(out, dronePod.getGoalPositionY());

        writeMinMaxedInt(out, dronePod.getUnknownEpsilon());
        writeMinMaxedInt(out, dronePod.getUnknownZeta());
        writeMinMaxedInt(out, dronePod.getNextTargetX());
        writeMinMaxedInt(out, dronePod.getNextTargetY());
        writeMinMaxedInt(out, dronePod.getUnknownIota());
        writeMinMaxedInt(out, dronePod.getUnknownKappa());

        writeInt(out, dronePod.getBuildupTicks());
        writeInt(out, dronePod.getStationaryTicks());
        writeInt(out, dronePod.getCooldownTicks());
        writeInt(out, dronePod.getOrbitAngle());
        writeInt(out, dronePod.getTurretAngle());
        writeInt(out, dronePod.getUnknownXi());
        writeMinMaxedInt(out, dronePod.getHopsToLive());
        writeInt(out, dronePod.getUnknownPi());
        writeInt(out, dronePod.getUnknownRho());
        writeInt(out, dronePod.getOverloadTicks());
        writeInt(out, dronePod.getUnknownTau());
        writeInt(out, dronePod.getUnknownUpsilon());
        writeInt(out, dronePod.getDeltaPositionX());
        writeInt(out, dronePod.getDeltaPositionY());

        writeAnim(out, dronePod.getDeathAnim());

        ExtendedDronePodInfo extendedInfo = dronePod.getExtendedInfo(ExtendedDronePodInfo.class);
        if (extendedInfo instanceof IntegerDronePodInfo) {
            IntegerDronePodInfo intPodInfo = dronePod.getExtendedInfo(IntegerDronePodInfo.class);
            for (int i = 0; i < intPodInfo.getSize(); i++) {
                writeMinMaxedInt(out, intPodInfo.get(i));
            }
        } else if (extendedInfo instanceof BoarderDronePodInfo) {
            BoarderDronePodInfo boarderPodInfo = dronePod.getExtendedInfo(BoarderDronePodInfo.class);
            writeInt(out, boarderPodInfo.getUnknownAlpha());
            writeInt(out, boarderPodInfo.getUnknownBeta());
            writeInt(out, boarderPodInfo.getUnknownGamma());
            writeInt(out, boarderPodInfo.getUnknownDelta());
            writeInt(out, boarderPodInfo.getBodyHealth());
            writeInt(out, boarderPodInfo.getBodyX());
            writeInt(out, boarderPodInfo.getBodyY());
            writeInt(out, boarderPodInfo.getBodyRoomId());
            writeInt(out, boarderPodInfo.getBodyRoomSquare());
        } else if (extendedInfo instanceof ShieldDronePodInfo) {
            ShieldDronePodInfo shieldPodInfo = dronePod.getExtendedInfo(ShieldDronePodInfo.class);
            writeInt(out, shieldPodInfo.getUnknownAlpha());
        } else if (extendedInfo instanceof HackingDronePodInfo) {
            HackingDronePodInfo hackingPodInfo = dronePod.getExtendedInfo(HackingDronePodInfo.class);
            writeInt(out, hackingPodInfo.getAttachPositionX());
            writeInt(out, hackingPodInfo.getAttachPositionY());
            writeInt(out, hackingPodInfo.getUnknownGamma());
            writeInt(out, hackingPodInfo.getUnknownDelta());
            writeAnim(out, hackingPodInfo.getLandingAnim());
            writeAnim(out, hackingPodInfo.getExtensionAnim());
        } else if (extendedInfo instanceof ZigZagDronePodInfo) {
            ZigZagDronePodInfo zigPodInfo = dronePod.getExtendedInfo(ZigZagDronePodInfo.class);
            writeInt(out, zigPodInfo.getLastWaypointX());
            writeInt(out, zigPodInfo.getLastWaypointY());
            writeMinMaxedInt(out, zigPodInfo.getTransitTicks());
            writeMinMaxedInt(out, zigPodInfo.getExhaustAngle());
            writeMinMaxedInt(out, zigPodInfo.getUnknownEpsilon());
        } else if (extendedInfo instanceof EmptyDronePodInfo) {
            // No-op.
        } else {
            throw new IOException("Unsupported extended drone pod info: " + extendedInfo.getClass().getSimpleName());
        }
    }

    private WeaponModuleState readWeaponModule(FileInputStream in, int fileFormat) throws IOException {
        WeaponModuleState weaponMod = new WeaponModuleState();

        weaponMod.setCooldownTicks(readInt(in));
        weaponMod.setCooldownTicksGoal(readInt(in));
        weaponMod.setSubcooldownTicks(readInt(in));
        weaponMod.setSubcooldownTicksGoal(readInt(in));
        weaponMod.setBoost(readInt(in));
        weaponMod.setCharge(readInt(in));

        int currentTargetsCount = readInt(in);
        List<XYPair> currentTargetsList = new ArrayList<XYPair>();
        for (int i = 0; i < currentTargetsCount; i++) {
            currentTargetsList.add(readReticleCoordinate(in));
        }
        weaponMod.setCurrentTargets(currentTargetsList);

        int prevTargetsCount = readInt(in);
        List<XYPair> prevTargetsList = new ArrayList<XYPair>();
        for (int i = 0; i < prevTargetsCount; i++) {
            prevTargetsList.add(readReticleCoordinate(in));
        }
        weaponMod.setPreviousTargets(prevTargetsList);

        weaponMod.setAutofire(readBool(in));
        weaponMod.setFireWhenReady(readBool(in));
        weaponMod.setTargetId(readInt(in));

        weaponMod.setWeaponAnim(readAnim(in));

        weaponMod.setProtractAnimTicks(readInt(in));
        weaponMod.setFiring(readBool(in));
        weaponMod.setUnknownPhi(readBool(in));

        if (fileFormat == 9 || fileFormat == 11) {
            weaponMod.setAnimCharge(readInt(in));

            weaponMod.setChargeAnim(readAnim(in));
        }
        weaponMod.setLastProjectileId(readInt(in));

        int pendingProjectilesCount = readInt(in);
        List<ProjectileState> pendingProjectiles = new ArrayList<ProjectileState>();
        for (int i = 0; i < pendingProjectilesCount; i++) {
            pendingProjectiles.add(readProjectile(in, fileFormat));
        }
        weaponMod.setPendingProjectiles(pendingProjectiles);

        return weaponMod;
    }

    public void writeWeaponModule(OutputStream out, WeaponModuleState weaponMod, int fileFormat) throws IOException {
        writeInt(out, weaponMod.getCooldownTicks());
        writeInt(out, weaponMod.getCooldownTicksGoal());
        writeInt(out, weaponMod.getSubcooldownTicks());
        writeInt(out, weaponMod.getSubcooldownTicksGoal());
        writeInt(out, weaponMod.getBoost());
        writeInt(out, weaponMod.getCharge());

        writeInt(out, weaponMod.getCurrentTargets().size());
        for (XYPair target : weaponMod.getCurrentTargets()) {
            writeReticleCoordinate(out, target);
        }

        writeInt(out, weaponMod.getPreviousTargets().size());
        for (XYPair target : weaponMod.getPreviousTargets()) {
            writeReticleCoordinate(out, target);
        }

        writeBool(out, weaponMod.getAutofire());
        writeBool(out, weaponMod.getFireWhenReady());
        writeInt(out, weaponMod.getTargetId());

        writeAnim(out, weaponMod.getWeaponAnim());

        writeInt(out, weaponMod.getProtractAnimTicks());
        writeBool(out, weaponMod.isFiring());
        writeBool(out, weaponMod.getUnknownPhi());

        if (fileFormat == 9 || fileFormat == 11) {
            writeInt(out, weaponMod.getAnimCharge());

            writeAnim(out, weaponMod.getChargeAnim());
        }

        writeInt(out, weaponMod.getLastProjectileId());

        writeInt(out, weaponMod.getPendingProjectiles().size());
        for (ProjectileState projectile : weaponMod.getPendingProjectiles()) {
            writeProjectile(out, projectile, fileFormat);
        }
    }

    private XYPair readReticleCoordinate(FileInputStream in) throws IOException {
        int reticleX = readInt(in);
        int reticleY = readInt(in);

        XYPair reticle = new XYPair(reticleX, reticleY);

        return reticle;
    }

    public void writeReticleCoordinate(OutputStream out, XYPair reticle) throws IOException {
        writeInt(out, reticle.x);
        writeInt(out, reticle.y);
    }
}
