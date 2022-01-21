package net.blerf.ftl.ui.floorplan;

import java.awt.image.BufferedImage;
import net.blerf.ftl.model.state.CrewType;
import net.blerf.ftl.parser.SavedGameParser.DroneType;
import net.blerf.ftl.model.state.SystemType;


public interface SpriteImageProvider {

    BufferedImage getShipBaseImage(String shipGfxBaseName, int w, int h);

    BufferedImage getShipFloorImage(String shipGfxBaseName);

    BufferedImage getRoomDecorImage(String decorName, int squaresH, int squaresV);

    BufferedImage getDroneBodyImage(DroneType droneType, boolean playerControlled);

    BufferedImage getCrewBodyImage(CrewType crewType, boolean male, boolean playerControlled);

    DoorAtlas getDoorAtlas();

    BufferedImage getSystemRoomImage(SystemType systemType);

    AnimAtlas getBreachAtlas();

    AnimAtlas getFireAtlas();
}
