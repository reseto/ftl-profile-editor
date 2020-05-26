package net.blerf.ftl.seedsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap;
import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.constants.Difficulty;
import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.random.RandRNG;
import net.blerf.ftl.parser.random.NativeRandom;
import net.blerf.ftl.parser.shiplayout.RandomShipLayout;
import net.blerf.ftl.parser.sectormap.RandomSectorMapGenerator;
import net.blerf.ftl.parser.sectormap.GeneratedSectorMap;
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

	public void generateAll(RandRNG rng) {

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
	}

	public void search() {

		RandRNG rng = new NativeRandom( "Native" );

		for (int seed=1; seed < 2; seed++) {
			log.info( String.format( "Seed %d", seed ) );
			rng.srand( seed );

			generateAll(rng);
		}
	}

	// private int minDistanceMap(GeneratedSectorMap map) {
	//
	// }
}
