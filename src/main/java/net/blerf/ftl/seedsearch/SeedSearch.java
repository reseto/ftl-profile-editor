package net.blerf.ftl.seedsearch;

import java.awt.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.constants.Difficulty;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.random.RandRNG;
import net.blerf.ftl.parser.random.NativeRandom;
import net.blerf.ftl.parser.shiplayout.RandomShipLayout;
import net.blerf.ftl.parser.sectormap.RandomSectorMapGenerator;
import net.blerf.ftl.parser.sectormap.GeneratedSectorMap;
import net.blerf.ftl.parser.sectormap.GeneratedBeacon;
import net.blerf.ftl.parser.sectortree.RandomSectorTreeGenerator;
import net.blerf.ftl.model.shiplayout.ShipLayout;
import net.blerf.ftl.model.shiplayout.ShipLayoutRoom;
import net.blerf.ftl.xml.FTLEvent;
import net.blerf.ftl.xml.Choice;
import net.blerf.ftl.xml.ShipEvent;

/**
 * Finding good seeds
 */
public class SeedSearch {

	private static final Logger log = LoggerFactory.getLogger( SeedSearch.class );

	protected RandRNG rng;
	boolean dlcEnabled = true;
	Difficulty difficulty = Difficulty.EASY;

	private static Set<Integer> uniqueCrewNames = new HashSet<Integer>();

	/* Generate a whole seed, and look at the sector map for a valid path.
	 * Returns if one was found.
	 */
	public boolean generateAll(RandRNG rng) {

		uniqueCrewNames.clear();

		/* Game startup */
		for (int i=0; i<101; i++) {
			rng.rand();
		}

		/* New Game */
		for (int i=0; i<68; i++) {
			rng.rand();
		}

		/* Random ship generation */
		int seed = rng.rand();
		// log.info( String.format( "Ship generation, seed: %d", seed ) );

		RandomShipLayout ship = new RandomShipLayout(rng);
		ship.setUniqueNames(uniqueCrewNames);
		ship.generateShipLayout(seed, "kestral");

		/* Sector tree generation */
		rng.rand();
		rng.rand();
		rng.rand();

		RandomSectorTreeGenerator expandedTreeGen = new RandomSectorTreeGenerator( rng );
		seed = rng.rand();
		// log.info( String.format( "Sector tree generation, seed: %d", seed ) );
		expandedTreeGen.generateSectorTree(seed, dlcEnabled);

		/* Sector map generation */
		RandomSectorMapGenerator sectorMapGen = new RandomSectorMapGenerator();
		sectorMapGen.sectorId = "STANDARD_SPACE";
		sectorMapGen.sectorNumber = 0;
		sectorMapGen.difficulty = difficulty;
		sectorMapGen.dlcEnabled = dlcEnabled;
		sectorMapGen.setUniqueNames(uniqueCrewNames);

		seed = rng.rand();
		rng.srand(seed);
		// log.info( String.format( "Sector map generation, seed: %d", seed ) );

		GeneratedSectorMap map = sectorMapGen.generateSectorMap(rng, 9);

		/* Check if generation finished early */
		if (map == null)
			return false;

		List<GeneratedBeacon> beaconList = map.getGeneratedBeaconList();

		return bfsStart(map);
	}

	/* Iterate for each seed value and look at a valid path */
	public void search() {

		RandRNG rng = new NativeRandom( "Native" );

		for (int seed = 49731; seed < 100000; seed++) {
			if (0 == (seed & 0xff))
				log.info( String.format( "Seed %d", seed ) );

			rng.srand( seed );

			boolean res = generateAll( rng );
			if (res) {
				log.info( String.format( "Seed %d", seed ) );
				break;
			}
		}
	}

	public static final double ISOLATION_THRESHOLD = 165d;

	private double distance(GeneratedBeacon b1, GeneratedBeacon b2) {
		Point p1 = b1.getLocation();
		Point p2 = b2.getLocation();
		return Math.hypot( p1.x - p2.x, p1.y - p2.y );
	}

	private boolean bfsStart(GeneratedSectorMap map) {
		List<GeneratedBeacon> beaconList = map.getGeneratedBeaconList();

		List<Integer> beaconPath = new ArrayList<Integer>(5);

		return bfs(map, map.startBeacon, beaconPath);
	}

	private boolean bfs(GeneratedSectorMap map, int currentBeacon, List<Integer> beaconPath) {
		List<GeneratedBeacon> beaconList = map.getGeneratedBeaconList();

		GeneratedBeacon endBeacon = beaconList.get(map.endBeacon);

		/* Examine current beacon event */
		GeneratedBeacon gb = beaconList.get(currentBeacon);
		FTLEvent event = gb.getEvent();

		/* Is ship hostile */
		if (eventHostile(event, false))
			return false;

		/* Register the beacon in the list */
		int currentDist = gb.distance;
		if (beaconPath.size() == currentDist)
			beaconPath.add(currentBeacon);
		else
			beaconPath.set(currentDist, currentBeacon);


		/* Check if finish beacon */
		if (currentBeacon == map.endBeacon) {
			return validatePath(map, beaconPath);
		}

		int curRow = gb.row;
		int curCol = gb.col;

		boolean res = false;
		for (int bd = 0; bd < beaconList.size(); bd++) {
			GeneratedBeacon otherBec = beaconList.get(bd);

			if (otherBec.distance != (currentDist+1))
				continue;

			/* Check if the two beacons are connected */
			if (Math.abs(otherBec.row - curRow) > 1)
				continue;

			if (Math.abs(otherBec.col - curCol) > 1)
				continue;

			if (distance(gb, otherBec) >= ISOLATION_THRESHOLD)
				continue;

			res = res || bfs(map, bd, beaconPath);
		}
		return res;
	}

	private boolean validatePath(GeneratedSectorMap map, List<Integer> beaconPath) {
		List<GeneratedBeacon> beaconList = map.getGeneratedBeaconList();

		boolean ret = false;
		for (int b : beaconPath) {
			GeneratedBeacon bec = beaconList.get(b);
			FTLEvent event = bec.getEvent();
			if (eventItem(event, "WEAPON_PREIGNITE")) {
				ret = true;
				break;
			}
		}

		if (! ret)
			return false;

		ret = false;
		for (int b : beaconPath) {
			GeneratedBeacon bec = beaconList.get(b);
			FTLEvent event = bec.getEvent();
			if (eventItem(event, "BEAM_HULL")) {
				ret = true;
				break;
			}
		}

		if (! ret)
			return false;

		for (int b : beaconPath) {
			GeneratedBeacon bec = beaconList.get(b);
			FTLEvent event = bec.getEvent();
			log.info( String.format( "Got beacon %d", b ) );
			log.info( event.toDescription(0) );
		}

		return true;
	}

	private boolean eventHostile(FTLEvent event, boolean hostile) {
		ShipEvent se = event.getShip();
		if (se != null)
			hostile = se.getHostile();

		/* Browse each choice, and load the corresponding event */
		List<Choice> choiceList = event.getChoiceList();

		if (choiceList == null)
			return hostile;

		boolean childHostile = hostile;
		for ( int i=0; i < choiceList.size(); i++ ) {
			Choice choice = choiceList.get(i);
			/* We skip if any requirement, we probably don't meet any */
			if (choice.getReq() != null)
				continue;

			FTLEvent choiceEvent = choice.getEvent();
			childHostile = childHostile && eventHostile(choiceEvent, hostile);
		}

		return childHostile;
	}

	private boolean eventItem(FTLEvent event, String item) {
		boolean gotItem = false;

		FTLEvent.Item weapon = event.getWeapon();
		if (weapon != null && weapon.name.equals(item))
			gotItem = true;

		FTLEvent.Item augment = event.getAugment();
		if (augment != null && augment.name.equals(item))
			gotItem = true;

		FTLEvent.Item drone = event.getDrone();
		if (drone != null && drone.name.equals(item))
			gotItem = true;

		FTLEvent.AutoReward autoReward = event.getAutoReward();
		if (autoReward != null) {
			if (autoReward.weapon != null && autoReward.weapon.equals(item))
				gotItem = true;

			if (autoReward.augment != null && autoReward.augment.equals(item))
				gotItem = true;

			if (autoReward.drone != null && autoReward.drone.equals(item))
				gotItem = true;
		}

		if (gotItem) {
			/* Check if loosing crew */
			FTLEvent.CrewMember cm = event.getCrewMember();
			if ((cm != null) && (cm.amount < 0))
				gotItem = false;

			/* Check if loosing stuff */
			FTLEvent.ItemList il = event.getItemList();
			if (il != null) {
				for (FTLEvent.Reward r : il.items) {
					if (r.value < 0)
						gotItem = false;
				}
			}
		}

		if (gotItem)
			return true;

		/* Browse each choice, and load the corresponding event */
		List<Choice> choiceList = event.getChoiceList();

		if (choiceList == null)
			return false;

		for ( int i=0; i < choiceList.size(); i++ ) {
			Choice choice = choiceList.get(i);
			/* We skip if any requirement, we probably don't meet any */
			if (choice.getReq() != null)
				continue;

			FTLEvent choiceEvent = choice.getEvent();
			if (eventItem(choiceEvent, item))
				return true;
		}

		return false;
	}
}
