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

/**
 * Finding good seeds
 */
public class SeedSearch {

	private static final Logger log = LoggerFactory.getLogger( SeedSearch.class );

	protected RandRNG rng;
	boolean dlcEnabled = true;
	Difficulty difficulty = Difficulty.EASY;

	private static Set<Integer> uniqueCrewNames = new HashSet<Integer>();

	public int generateAll(RandRNG rng) {

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
		log.info( String.format( "Ship generation" ) );
		int seed = rng.rand();
		log.info( String.format( "Seed: %d", seed ) );

		RandomShipLayout ship = new RandomShipLayout(rng);
		ship.setUniqueNames(uniqueCrewNames);
		ship.generateShipLayout(seed, "kestral");

		/* Sector tree generation */
		log.info( String.format( "Sector tree generation" ) );
		rng.rand();
		rng.rand();
		rng.rand();

		RandomSectorTreeGenerator expandedTreeGen = new RandomSectorTreeGenerator( rng );
		seed = rng.rand();
		log.info( String.format( "Seed: %d", seed ) );
		expandedTreeGen.generateSectorTree(seed, dlcEnabled);

		/* Sector map generation */
		RandomSectorMapGenerator sectorMapGen = new RandomSectorMapGenerator();
		sectorMapGen.sectorId = "STANDARD_SPACE";
		sectorMapGen.sectorNumber = 0;
		sectorMapGen.difficulty = difficulty;
		sectorMapGen.dlcEnabled = dlcEnabled;
		sectorMapGen.setUniqueNames(uniqueCrewNames);

		log.info( String.format( "Sector map generation" ) );
		seed = rng.rand();
		rng.srand(seed);
		log.info( String.format( "Seed: %d", seed ) );

		GeneratedSectorMap map = sectorMapGen.generateSectorMap(rng, 9);

		int md = minDistanceMap(map, 4);
		log.info( String.format( "Min distance is %d", md ) );
		return md;
	}

	public void search() {

		RandRNG rng = new NativeRandom( "Native" );

		for (int seed=39; seed < 1000; seed++) {
			log.info( String.format( "Seed %d", seed ) );
			rng.srand( seed );

			int md = generateAll( rng );
			if ((md > 0) && (md < 5)) break;
		}
	}

	public static final double ISOLATION_THRESHOLD = 165d;

	public static class BeaconDist {
		public int id;
		public int step;
	}

	private int minDistanceMap(GeneratedSectorMap map, int upperBound) {
		List<GeneratedBeacon> beaconList = map.getGeneratedBeaconList();

		GeneratedBeacon startBeacon = beaconList.get(map.startBeacon);
		GeneratedBeacon endBeacon = beaconList.get(map.endBeacon);

		if ((upperBound < 5) && (endBeacon.col == 5))
			return -1;

		if (distance(startBeacon, endBeacon) > (upperBound * ISOLATION_THRESHOLD))
			return -1;

		List<Integer> beaconDists = new ArrayList<Integer>(Collections.nCopies(beaconList.size(), -1));
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
					if (gb == map.endBeacon)
						return currentDist+1;

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

}
