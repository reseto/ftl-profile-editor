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
		log.info( String.format( "Ship generation, seed: %d", seed ) );

		RandomShipLayout ship = new RandomShipLayout(rng);
		ship.setUniqueNames(uniqueCrewNames);
		ship.generateShipLayout(seed, "kestral");

		/* Sector tree generation */
		rng.rand();
		rng.rand();
		rng.rand();

		RandomSectorTreeGenerator expandedTreeGen = new RandomSectorTreeGenerator( rng );
		seed = rng.rand();
		log.info( String.format( "Sector tree generation, seed: %d", seed ) );
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
		log.info( String.format( "Sector map generation, seed: %d", seed ) );

		GeneratedSectorMap map = sectorMapGen.generateSectorMap(rng, 9);

		List<GeneratedBeacon> beaconList = map.getGeneratedBeaconList();
		List<Integer> beaconDists = new ArrayList<Integer>(Collections.nCopies(beaconList.size(), -1));

		int md = minDistanceMap(map, beaconDists, 4);

		if (md < 0)
			return false;

		return bfsStart(map, beaconDists);
	}

	/* Iterate for each seed value and look at a valid path */
	public void search() {

		RandRNG rng = new NativeRandom( "Native" );

		for (int seed = 7662; seed < 100000; seed++) {
			log.info( String.format( "Seed %d", seed ) );
			rng.srand( seed );

			boolean res = generateAll( rng );
			if (res) break;
		}
	}

	public static final double ISOLATION_THRESHOLD = 165d;

	public static class BeaconDist {
		public int id;
		public int step;
	}

	private int minDistanceMap(GeneratedSectorMap map, List<Integer> beaconDists, int upperBound) {
		List<GeneratedBeacon> beaconList = map.getGeneratedBeaconList();

		GeneratedBeacon startBeacon = beaconList.get(map.startBeacon);
		GeneratedBeacon endBeacon = beaconList.get(map.endBeacon);

		if ((upperBound < 5) && (endBeacon.col == 5))
			return -1;

		if (distance(startBeacon, endBeacon) > (upperBound * ISOLATION_THRESHOLD))
			return -1;

		beaconDists.set(map.startBeacon, 0);

		for (int currentDist = 0; currentDist < upperBound; currentDist++) {
			for (int bd = 0; bd < beaconDists.size(); bd++) {
				if (beaconDists.get(bd) != currentDist)
					continue;

				GeneratedBeacon curBec = beaconList.get(bd);
				int curRow = curBec.row;
				int curCol = curBec.col;

				for (int gb = 0; gb < beaconList.size(); gb++) {
					if (bd == gb)
						continue;

					/* Check if beacon already in list */
					if (beaconDists.get(gb) != -1)
						continue;

					GeneratedBeacon otherBec = beaconList.get(gb);

					/* Check if the two beacons are connected */
					if (Math.abs(otherBec.row - curRow) > 1)
						continue;

					if (Math.abs(otherBec.col - curCol) > 1)
						continue;

					if (distance(curBec, otherBec) >= ISOLATION_THRESHOLD)
						continue;

					/* Check if final beacon */
					if (gb == map.endBeacon) {
						beaconDists.set(gb, currentDist+1);
						return currentDist+1;
					}

					/* Some more pruning to skip beacons which are too far */
					if (Math.abs(otherBec.col - endBeacon.col) > (upperBound - (currentDist+1)))
						continue;

					if (Math.abs(otherBec.row - endBeacon.row) > (upperBound - (currentDist+1)))
						continue;

					if (distance(otherBec, endBeacon) < ((upperBound - (currentDist+1))*ISOLATION_THRESHOLD)) {
						beaconDists.set(gb, currentDist+1);
					}
				}
			}
		}

		return -1;
	}

	private double distance(GeneratedBeacon b1, GeneratedBeacon b2) {
		Point p1 = b1.getLocation();
		Point p2 = b2.getLocation();
		return Math.hypot( p1.x - p2.x, p1.y - p2.y );
	}

	private boolean bfsStart(GeneratedSectorMap map, List<Integer> beaconDists) {
		List<GeneratedBeacon> beaconList = map.getGeneratedBeaconList();

		List<Integer> beaconPath = new ArrayList<Integer>(5);

		return bfs(map, beaconDists, map.startBeacon, beaconPath);
	}

	private boolean bfs(GeneratedSectorMap map, List<Integer> beaconDists, int currentBeacon, List<Integer> beaconPath) {
		List<GeneratedBeacon> beaconList = map.getGeneratedBeaconList();

		GeneratedBeacon endBeacon = beaconList.get(map.endBeacon);

		/* Examine current beacon event */
		GeneratedBeacon gb = beaconList.get(currentBeacon);
		FTLEvent event = gb.getEvent();

		/* Is ship hostile */
		if (eventHostile(event, false))
			return false;

		/* Register the beacon in the list */
		int currentDist = beaconDists.get(currentBeacon);
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
		for (int bd = 0; bd < beaconDists.size(); bd++) {
			if (beaconDists.get(bd) != (currentDist+1))
				continue;

			GeneratedBeacon otherBec = beaconList.get(bd);

			/* Check if the two beacons are connected */
			if (Math.abs(otherBec.row - curRow) > 1)
				continue;

			if (Math.abs(otherBec.col - curCol) > 1)
				continue;

			if (distance(gb, otherBec) >= ISOLATION_THRESHOLD)
				continue;

			res = res || bfs(map, beaconDists, bd, beaconPath);
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
		FTLEvent.Item weapon = event.getWeapon();
		if (weapon != null && weapon.name.equals(item))
			return true;

		FTLEvent.Item augment = event.getAugment();
		if (augment != null && augment.name.equals(item))
			return true;

		FTLEvent.Item drone = event.getDrone();
		if (drone != null && drone.name.equals(item))
			return true;

		FTLEvent.AutoReward autoReward = event.getAutoReward();
		if (autoReward != null) {
			if (autoReward.weapon != null && autoReward.weapon.equals(item))
				return true;

			if (autoReward.augment != null && autoReward.augment.equals(item))
				return true;

			if (autoReward.drone != null && autoReward.drone.equals(item))
				return true;
		}

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
