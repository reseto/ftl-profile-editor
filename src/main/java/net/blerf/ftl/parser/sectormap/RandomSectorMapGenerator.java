package net.blerf.ftl.parser.sectormap;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.constants.Difficulty;
import net.blerf.ftl.xml.FTLEvent;
import net.blerf.ftl.xml.FTLEventList;
import net.blerf.ftl.xml.SectorDescription;
import net.blerf.ftl.xml.Choice;
import net.blerf.ftl.xml.NamedText;
import net.blerf.ftl.xml.TextList;

import net.blerf.ftl.parser.sectormap.GeneratedBeacon;
import net.blerf.ftl.parser.sectormap.GeneratedSectorMap;
import net.blerf.ftl.parser.sectormap.RandomEvent;
import net.blerf.ftl.parser.random.RandRNG;


/**
 * A generator to create a GeneratedSectorMap object from a seed as FTL would.
 *
 * The rebelFleetFudge will be set, though the SavedGameState's value overrides
 * it.
 *
 * This class iterates over a rectangular grid randomly skipping cells. Each
 * beacon's throbTicks will be random. Each beacon's x/y location will be
 * random within cells. The result will not be rectangular. Maintaining the
 * grid should not be necessary.
 *
 * FTL 1.03.3's map was 530x346, with its origin at (438, 206) (on a 1286x745
 * screenshot including +3,+22 padding from the OS Window decorations).
 *
 * FTL 1.5.4 changed the algorithm from what had been used previously. It also
 * enlarged the map's overall dimensions: 640x488, with its origin at
 * (389, 146) (on a 1286x745 screenshot including +3,+22 padding).
 *
 * @see net.blerf.ftl.parser.SavedGameParser.SavedGameState#getFileFormat()
 */
public class RandomSectorMapGenerator {

	// private static final Logger log = LoggerFactory.getLogger( RandomSectorMapGenerator.class );
	private static final Logger log = NOPLogger.NOP_LOGGER;

	/**
	 * The threshold for re-rolling a map with disconnected beacons.
	 *
	 * This was determined empirically, checking against FTL and raising the
	 * value until the editor stopped re-rolling excessively.
	 *
	 * Observed values:
	 *   FTL 1.5.13: 163.6 ... x ... 166.98.
	 *   FTL 1.6.2: 163.41 ... x ... 165.87.
	 *   (Lower bound was not re-rolled. Upper bound was re-rolled.)
	 *
	 * @see #calculateIsolation(GeneratedSectorMap)
	 */
	public static final double ISOLATION_THRESHOLD = 165d;

	public String sectorId = "STANDARD_SPACE";
	public int sectorNumber = 0;
	public Difficulty difficulty = Difficulty.NORMAL;
	public boolean dlcEnabled = false;

	private static Set<Integer> uniqueCrewNames = null;

	public void setUniqueNames( Set<Integer> un ) {
		uniqueCrewNames = un;
	}

	public static class EmptyBeacon {
		public int id;
		public int x;
		public int y;
		public FTLEvent event;
	}

	public static class NebulaRect {
		public int x;
		public int y;
		public int w;
		public int h;
	}

	/**
	 * Generates the sector map.
	 *
	 * Note: The RNG needs to be seeded immediately before calling this method.
	 *
	 * @see net.blerf.ftl.parser.SavedGameParser.SavedGameState#getFileFormat()
	 * @throws IllegalStateException if a valid map isn't generated after 50 attempts
	 */
	public GeneratedSectorMap generateSectorMap( RandRNG rng, int fileFormat ) {

		if ( fileFormat == 2 ) {
			// FTL 1.01-1.03.3

			int columns = 6;  // TODO: Magic numbers.
			int rows = 4;

			GeneratedSectorMap genMap = new GeneratedSectorMap();
			genMap.setPreferredSize( new Dimension( 530, 346 ) );  // TODO: Magic numbers.

			int n;

			n = rng.rand();
			genMap.setRebelFleetFudge( n % 294 + 50 );

			List<GeneratedBeacon> genBeaconList = new ArrayList<GeneratedBeacon>();
			int skipInclusiveCount = 0;
			int z = 0;

			for ( int c=0; c < columns; c++ ) {

				for ( int r=0; r < rows; r++ ) {
					n = rng.rand();
					if ( n % 5 == 0 ) {
						z++;

						if ( skipInclusiveCount / z > 4 ) {  // Skip this cell.
							skipInclusiveCount++;
							continue;
						}
					}
					GeneratedBeacon genBeacon = new GeneratedBeacon();

					genBeacon.setGridPosition( c, r );

					n = rng.rand();
					genBeacon.setThrobTicks( n % 2001 );

					n = rng.rand();
					int locX = n % 66 + c*86 + 10;
					n = rng.rand();
					int locY = n % 66 + r*86 + 10;

					if ( c == 5 && locX > 450 ) {  // Yes, this really was FTL's logic.
						locX -= 10;
					}
					if ( r == 3 && locY > 278 ) {  // Yes, this really was FTL's logic.
						locY -= 10;
					}

					genBeacon.setLocation( locX, locY );

					genBeaconList.add( genBeacon );
					skipInclusiveCount++;
				}
			}

			genMap.setGeneratedBeaconList( genBeaconList );

			return genMap;
		}
		else if ( fileFormat == 7 || fileFormat == 8 || fileFormat == 9 || fileFormat == 11 ) {
			// FTL 1.5.4-1.5.10, 1.5.12, 1.5.13, 1.6.1-1.6.2.

			int columns = 6;  // TODO: Magic numbers.
			int rows = 4;

			GeneratedSectorMap genMap = new GeneratedSectorMap();
			genMap.setPreferredSize( new Dimension( 640, 488 ) );  // TODO: Magic numbers.

			int n;
			// int generations = 0;

			n = rng.rand();
			genMap.setRebelFleetFudge( n % 250 + 50 );

			// while ( generations < 50 ) {
				List<GeneratedBeacon> genBeaconList = new ArrayList<GeneratedBeacon>();
				int skipInclusiveCount = 0;
				int z = 0;

				for ( int c=0; c < columns; c++ ) {

					for ( int r=0; r < rows; r++ ) {
						n = rng.rand();
						if ( n % 5 == 0 ) {
							z++;

							if ( skipInclusiveCount / z > 4 ) {  // Skip this cell.
								skipInclusiveCount++;
								continue;
							}
						}
						GeneratedBeacon genBeacon = new GeneratedBeacon();

						genBeacon.setGridPosition( c, r );

						n = rng.rand();
						genBeacon.setThrobTicks( n % 2001 );

						n = rng.rand();
						int locX = n % 90 + c*110 + 10;
						n = rng.rand();
						int locY = n % 90 + r*110 + 10;
						locY = Math.min( locY, 415 );

						if ( c > 3 && r == 0 ) {  // Yes, this really was FTL's logic.
							locY = Math.max( locY, 30 );
						}

						genBeacon.setLocation( locX, locY );

						genBeaconList.add( genBeacon );
						skipInclusiveCount++;
					}
				}

				genMap.setGeneratedBeaconList( genBeaconList );
				// generations++;

				// boolean isolation = calculateIsolation( genMap );
				// if ( isolation  ) {
				// 	if (log.isDebugEnabled())
				// 		log.debug( String.format( "Re-rolling sector map because attempt #%d has isolated beacons (threshold dist %5.2f): %5.2f", generations, ISOLATION_THRESHOLD, isolation ) );
				// 	genMap.setGeneratedBeaconList( null );
				// }
				// else {
				// 	break;  // Success!
				// }
			// }

			// if ( genMap.getGeneratedBeaconList() == null ) {
			// 	throw new IllegalStateException( String.format( "No valid map was produced after %d attempts!?", generations ) );
			// }

			RandomEvent.setSectorId(sectorId);
			RandomEvent.setSectorNumber(sectorNumber);
			RandomEvent.setDifficulty(difficulty);
			RandomEvent.setDlc(dlcEnabled);
			RandomEvent.resetUniqueSectors();
			RandomEvent.setUniqueNames(uniqueCrewNames);

			// List<GeneratedBeacon> genBeaconList = genMap.getGeneratedBeaconList();

			SectorDescription tmpDesc = DataManager.getInstance().getSectorDescriptionById( sectorId );
			if (tmpDesc == null) {
				tmpDesc = DataManager.getInstance().getSectorDescriptionById( "STANDARD_SPACE" );
			}

			/* Generate starting beacon position: 0x4e7b95 */
			int startingBeacon = rng.rand() & 3;

			/* Generate starting beacon event: 0x4e7f57 */
			String startEvent = tmpDesc.getStartEvent();
			if (startEvent == null) {
				startEvent = "START_BEACON";
			}

			genMap.startBeacon = startingBeacon;
			genBeaconList.get(startingBeacon).event = RandomEvent.loadEventId(startEvent, rng);

			/* Generate ending beacon position: two rands at 0x4e8032 and 0x4e804d */
			int r, c;
			GeneratedBeacon endingGb = null;
			do {
				r = rng.rand() & 3;
				c = (rng.rand() & 1) + 4;
				if ( false ) { // Some condition, probably last sector
					if ( difficulty == Difficulty.HARD ) {
						c = (rng.rand() & 1) + 3;
					}
					else {
						c = (rng.rand() & 1) + 2;
					}
				}

				/* Check that the position has a beacon in it, otherwise loop */
				for (int g = genBeaconList.size() - 1; g >= 0; g--) {
					GeneratedBeacon gb = genBeaconList.get(g);
					Point gridLoc = gb.getGridPosition();
					if ((gridLoc.x == c) && (gridLoc.y == r)) {
						endingGb = gb;
						genMap.endBeacon = g;
						break;
					}
				}
			} while (endingGb == null);

			/* If no path of four jumps possible, return */
			if (minDistanceMap(genMap, 4) == -1)
				return null;

			/* Generate ending beacon event ("FINISH_BEACON") */
			endingGb.event = RandomEvent.loadEventId("FINISH_BEACON", rng);

			/* Place NEBULA beacons first */
			List<SectorDescription.EventDistribution> eventDistribution = tmpDesc.getEventDistributions();

			/* Build the list of all nebula beacons */
			List<String> nebulaEvents = new ArrayList<String>();

			for (SectorDescription.EventDistribution ed : eventDistribution) {
				if (ed.name.startsWith("NEBULA")) {
					int m = (rng.rand() % (ed.max + 1 - ed.min)) + ed.min;
					if (log.isDebugEnabled())
						log.debug( String.format( "min %d max %d value %d", ed.min, ed.max, m ) );

					for (int i=0; i<m; i++)
						nebulaEvents.add(ed.name);
				}
			}

			if (log.isDebugEnabled())
				log.debug( String.format( "Generate %d nebula events", nebulaEvents.size() ) );

			if (!nebulaEvents.isEmpty()) {

				/* Build a list of empty beacons */
				List<EmptyBeacon> emptyBeacons = new ArrayList<EmptyBeacon>();

				for (int bb = 0; bb < genBeaconList.size(); bb++) {
					GeneratedBeacon curBeacon = genBeaconList.get(bb);

					EmptyBeacon e = new EmptyBeacon();
					e.id = bb;
					e.x = curBeacon.x;
					e.y = curBeacon.y;
					e.event = curBeacon.event;

					emptyBeacons.add(e);
				}

				/* Hardcoded list of nebula models */
				List<Integer> nebulaModelListW;
				List<Integer> nebulaModelListH;

				if (nebulaEvents.size() < 6) {
					nebulaModelListW = Arrays.asList(119, 67, 89, 117);
					nebulaModelListH = Arrays.asList(63, 110, 67, 108);
				}
				else {
					nebulaModelListW = Arrays.asList(250, 200, 250);
					nebulaModelListH = Arrays.asList(234, 250, 200);
				}

				/* Print nebula nebula models:
				break *0x4d6b55
				commands
				silent
				printf "rect x %d\n",*(int*)($rsp+0x40)
				printf "rect y %d\n",*(int*)($rsp+0x44)
				printf "rect w %d\n",*(int*)($rsp+0x38)
				printf "rect h %d\n",*(int*)($rsp+0x3c)
				cont
				end
				 */

				/* Choose a random nebula model */
				n = rng.rand() % nebulaModelListW.size();

				/* If less than 4 non-nebula beacons, remove random nebulas */
				while ((emptyBeacons.size() - nebulaEvents.size()) < 4) {
					int k = rng.rand() % nebulaEvents.size();
					nebulaEvents.remove(k);
				}

				/* Choose a random beacon */
				int bId = rng.rand() % emptyBeacons.size();
				EmptyBeacon beacon = emptyBeacons.get(bId);

				if (log.isDebugEnabled())
					log.debug( String.format( "Starting nebula beacon: %d ", bId ) );

				/* The nebula model is centered on the chosen beacon */
				int modelW = nebulaModelListW.get(n);
				int modelH = nebulaModelListH.get(n);
				int modelX = beacon.x - modelW / 2;
				int modelY = beacon.y - modelH / 2;


				/* Number of failed attemps */
				int failedAttempts = 0;

				/* Build a list of empty beacons */
				List<NebulaRect> nebulaRects = new ArrayList<NebulaRect>();

				do {
					boolean oneNewBeacon = false;
					if (log.isDebugEnabled())
						log.debug( String.format( "Nebula rect is: (%d, %d, %d, %d) ", modelX, modelY, modelW, modelH ) );

					/* Iterate over all empty beacons */
					int be = 0;
					while (be < emptyBeacons.size()) {

						EmptyBeacon curBeacon = emptyBeacons.get(be);

						/* Check if the beacon is inside the nebula model */
						if ( (curBeacon.x > (modelX + 5)) &&
							 (curBeacon.x < (modelX + modelW - 5)) &&
						     (curBeacon.y > (modelY + 5)) &&
							 (curBeacon.y < (modelY + modelH - 5))) {

							/* Check the beacon event */
							if (curBeacon.event == null) {

								/* No event in that beacon, load one nebula event */

								/* Default nebula event */
								String nebulaEvent = "NEBULA";

								if (!nebulaEvents.isEmpty()) {
									/* Choose a random nebula from the list */
				 					int ne = rng.rand() % nebulaEvents.size();

									nebulaEvent = nebulaEvents.get(ne);
									nebulaEvents.remove(ne);
								}

								/* Load the nebula event */
								genBeaconList.get(curBeacon.id).event = RandomEvent.loadEventId(nebulaEvent, rng);

								if (log.isDebugEnabled())
									log.debug( String.format( "Nebula event at beacon %d (%d,%d)", curBeacon.id, curBeacon.x, curBeacon.y ) );
							}

							/* If finish beacon, load the FINISH_BEACON_NEBULA event instead */
							else if (curBeacon.event.getId().equals("FINISH_BEACON")) {
								genBeaconList.get(curBeacon.id).event = RandomEvent.loadEventId("FINISH_BEACON_NEBULA", rng);
								if (log.isDebugEnabled())
									log.debug( String.format( "Nebula finish event at beacon %d (%d,%d)", curBeacon.id, curBeacon.x, curBeacon.y ) );
							}

							/* Remove empty beacon from list */
							emptyBeacons.remove(be);

							/* We generated at least one new beacon */
							oneNewBeacon = true;
						}
						else {
							/* Next beacon */
							be++;
						}
					}

					/* Update the number of failed attemps */
					if (!oneNewBeacon)
						failedAttempts++;
					else {
						/* Insert the nebula */
						NebulaRect nr = new NebulaRect();
						nr.x = modelX;
						nr.y = modelY;
						nr.w = modelW;
						nr.h = modelH;

						nebulaRects.add(nr);
					}

					if (failedAttempts < 0x15) {
						/* Pick an existing nebula rect */
						n = rng.rand() % nebulaRects.size();
						NebulaRect oldnr = nebulaRects.get(n);

						/* Pick a new nebula model */
						n = rng.rand() % nebulaModelListW.size();

						/* Build the new nebula rect so that it intersects with
						 * the chosen existing nebula
						 */
						modelW = nebulaModelListW.get(n);
						modelH = nebulaModelListH.get(n);
						modelX = oldnr.x - modelW + rng.rand() % (oldnr.w + modelW);
						modelY = oldnr.y - modelH + rng.rand() % (oldnr.h + modelH);
					}
					else {
						/* Place the new nebula around an empty beacon,
						 * keep the current model.
						 */
						bId = rng.rand() % emptyBeacons.size();
						beacon = emptyBeacons.get(bId);

						modelX = beacon.x - modelW / 2;
						modelY = beacon.y - modelH / 2;

						failedAttempts = 0;
					}
				}
				while (!nebulaEvents.isEmpty());
			}

			/* Build the other beacons */

			/* Build a list of beacon ids */
			List<Integer> beaconIds = new ArrayList<Integer>();
			for (int bb = 0; bb < genBeaconList.size(); bb++) {
				beaconIds.add(bb);
			}

			for (SectorDescription.EventDistribution ed : eventDistribution) {
				/* Skip nebulas */
				if (ed.name.startsWith("NEBULA"))
					continue;

				/* Pick a random number of events from the distribution */
				int m = 0;
				if (ed.max != 0) {
					if (log.isDebugEnabled())
						log.debug( String.format( "Generate the number of events of distribution %s", ed.name ) );
					m = (rng.rand() % (ed.max + 1 - ed.min)) + ed.min;
				}

				int i = 0;
				while ((i<m) && (!beaconIds.isEmpty())) {
					/* Choose a random empty beacon */
					if (log.isDebugEnabled())
						log.debug( String.format( "Choose the beacon to apply event" ) );
					int b = rng.rand() % beaconIds.size();
					GeneratedBeacon gb = genBeaconList.get(beaconIds.get(b));

					/* Check if the beacon is empty */
					if (gb.event == null) {
						if (log.isDebugEnabled())
							log.debug( String.format( "Generate event %s for beacon %d", ed.name, beaconIds.get(b) ) );
						Point p = gb.getLocation();
						if (log.isDebugEnabled())
							log.debug( String.format( "Coords %d - %d", p.x, p.y ) );
						gb.event = RandomEvent.loadEventId(ed.name, rng);
						i++;
					}

					/* Remove the beacon id from the list */
					beaconIds.remove(b);
				}

				if (beaconIds.isEmpty())
					break;

			}

			/* Fill the remaining beacons with NEUTRAL */
			for (int b = 0; b<beaconIds.size(); b++) {
				GeneratedBeacon gb = genBeaconList.get(beaconIds.get(b));

				/* Check if the beacon is empty */
				if (gb.event == null) {
					if (log.isDebugEnabled())
						log.debug( String.format( "Generate event NEUTRAL for beacon %d", beaconIds.get(b) ) );
					gb.event = RandomEvent.loadEventId("NEUTRAL", rng);
				}
			}

			uniqueCrewNames.clear(); // TODO: should be kept between sectors?

			return genMap;
		}
		else {
			throw new UnsupportedOperationException( String.format( "Random sector maps for fileFormat (%d) have not been implemented", fileFormat ) );
		}
	}

	/**
	 * Returns the most isolated beacon's distance to its nearest neighbor.
	 *
	 * FTL 1.5.4 introduced a check to re-generate invalid maps. The changelog
	 * said, "Maps will no longer have disconnected beacons, everything will be
	 * accessible."
	 *
	 * Try using a fast code, because this will be performed often.
	 */
	public boolean calculateIsolation( GeneratedSectorMap genMap ) {
		List<GeneratedBeacon> beaconList = genMap.getGeneratedBeaconList();

		GeneratedBeacon startBeacon = beaconList.get(0);

		startBeacon.distance = 0;

		boolean oneNewBeacon = true;
		for (int currentDist = 0; oneNewBeacon; currentDist++) {
			oneNewBeacon = false;

			for (int bd = 0; bd < beaconList.size(); bd++) {
				GeneratedBeacon curBec = beaconList.get(bd);

				if (curBec.distance != currentDist)
					continue;

				int curRow = curBec.row;
				int curCol = curBec.col;

				for (int gb = 0; gb < beaconList.size(); gb++) {
					if (bd == gb)
						continue;

					GeneratedBeacon otherBec = beaconList.get(gb);

					/* Check if beacon already processed */
					if (otherBec.distance != -1)
						continue;

					/* Check if the two beacons are connected */
					if (Math.abs(otherBec.row - curRow) > 1)
						continue;

					if (Math.abs(otherBec.col - curCol) > 1)
						continue;

					if (distance(curBec, otherBec) >= ISOLATION_THRESHOLD)
						continue;

					otherBec.distance = currentDist + 1;
					oneNewBeacon = true;
				}
			}
		}

		/* Check if all distances are not -1, and reset all distances */
		boolean isolated = false;
		for (int bd = 0; bd < beaconList.size(); bd++) {
			GeneratedBeacon curBec = beaconList.get(bd);
			if (curBec.distance == -1)
				isolated = true;
			else
				curBec.distance = -1;
		}

		return isolated;
	}

	/**
	 * Computes the distance of each beacon to the start beacon, if on a path
	 * of max upperBound jumps from start to finish.
	 * If no path of upperBound jumps, the distance of the finish beacon will be -1
	 */
	private int minDistanceMap(GeneratedSectorMap map, int upperBound) {
		List<GeneratedBeacon> beaconList = map.getGeneratedBeaconList();

		GeneratedBeacon startBeacon = beaconList.get(map.startBeacon);
		GeneratedBeacon endBeacon = beaconList.get(map.endBeacon);

		if ((upperBound < 5) && (endBeacon.col == 5))
			return -1;

		if (distance(startBeacon, endBeacon) > (upperBound * ISOLATION_THRESHOLD))
			return -1;

		startBeacon.distance = 0;

		for (int currentDist = 0; currentDist < upperBound; currentDist++) {
			for (int bd = 0; bd < beaconList.size(); bd++) {
				GeneratedBeacon curBec = beaconList.get(bd);

				if (curBec.distance != currentDist)
					continue;

				int curRow = curBec.row;
				int curCol = curBec.col;

				for (int gb = 0; gb < beaconList.size(); gb++) {
					if (bd == gb)
						continue;

					GeneratedBeacon otherBec = beaconList.get(gb);

					/* Check if beacon already processed */
					if (otherBec.distance != -1)
						continue;

					/* Check if the two beacons are connected */
					if (Math.abs(otherBec.row - curRow) > 1)
						continue;

					if (Math.abs(otherBec.col - curCol) > 1)
						continue;

					if (distance(curBec, otherBec) >= ISOLATION_THRESHOLD)
						continue;

					/* Check if final beacon */
					if (gb == map.endBeacon) {
						otherBec.distance = currentDist + 1;
						return currentDist + 1;
					}

					/* Some more pruning to skip beacons which are too far */
					if (Math.abs(otherBec.col - endBeacon.col) > (upperBound - (currentDist+1)))
						continue;

					if (Math.abs(otherBec.row - endBeacon.row) > (upperBound - (currentDist+1)))
						continue;

					if (distance(otherBec, endBeacon) < ((upperBound - (currentDist+1))*ISOLATION_THRESHOLD)) {
						otherBec.distance = currentDist + 1;
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
