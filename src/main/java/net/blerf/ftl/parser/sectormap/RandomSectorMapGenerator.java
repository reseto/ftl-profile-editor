package net.blerf.ftl.parser.sectormap;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory.getLogger( RandomSectorMapGenerator.class );

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

	public String sectorId = "ENGI_HOME";
	public int sectorNumber = 0;
	public Difficulty difficulty = Difficulty.NORMAL;
	public boolean dlcEnabled = false;

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
			int generations = 0;

			n = rng.rand();
			genMap.setRebelFleetFudge( n % 250 + 50 );

			while ( generations < 50 ) {
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
				generations++;

				double isolation = calculateIsolation( genMap );
				if ( isolation > ISOLATION_THRESHOLD ) {
					log.info( String.format( "Re-rolling sector map because attempt #%d has isolated beacons (threshold dist %5.2f): %5.2f", generations, ISOLATION_THRESHOLD, isolation ) );
					genMap.setGeneratedBeaconList( null );
				}
				else {
					break;  // Success!
				}
			}

			if ( genMap.getGeneratedBeaconList() == null ) {
				throw new IllegalStateException( String.format( "No valid map was produced after %d attempts!?", generations ) );
			}

			RandomEvent.setSectorId(sectorId);
			RandomEvent.setSectorNumber(sectorNumber);
			RandomEvent.setDifficulty(difficulty);
			RandomEvent.setDlc(dlcEnabled);
			RandomEvent.resetUniqueSectors();

			List<GeneratedBeacon> genBeaconList = genMap.getGeneratedBeaconList();

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
				for (GeneratedBeacon gb : genBeaconList) {
					/* TODO: Could be optimized because genBeaconList is ordered */
					Point gridLoc = gb.getGridPosition();
					if ((gridLoc.x == c) && (gridLoc.y == r)) {
						endingGb = gb;
						break;
					}
				}
			} while (endingGb == null);

			/* Generate ending beacon event ("FINISH_BEACON") */
			endingGb.event = RandomEvent.loadEventId("FINISH_BEACON", rng);

			/* Place NEBULA beacons first */
			List<SectorDescription.EventDistribution> eventDistribution = tmpDesc.getEventDistributions();

			/* Build the list of all nebula beacons */
			List<String> nebulaEvents = new ArrayList<String>();

			Iterator<SectorDescription.EventDistribution> it = eventDistribution.iterator();
			while (it.hasNext()) {
				SectorDescription.EventDistribution ed = it.next();
				if (ed.name.startsWith("NEBULA")) {
					int m = (rng.rand() % (ed.max + 1 - ed.min)) + ed.min;

					for (int i=0; i<m; i++)
						nebulaEvents.add(ed.name);

					/* Remove event from list */
					it.remove();
				}
			}

			log.info( String.format( "Generate %d nebula events", nebulaEvents.size() ) );

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

				log.info( String.format( "Starting nebula beacon: %d ", bId ) );

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
					log.info( String.format( "Nebula rect is: (%d, %d, %d, %d) ", modelX, modelY, modelW, modelH ) );

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

								log.info( String.format( "Nebula event at beacon %d (%d,%d)", curBeacon.id, curBeacon.x, curBeacon.y ) );
							}

							/* If finish beacon, load the FINISH_BEACON_NEBULA event instead */
							else if (curBeacon.event.getId().equals("FINISH_BEACON")) {
								genBeaconList.get(curBeacon.id).event = RandomEvent.loadEventId("FINISH_BEACON_NEBULA", rng);
								log.info( String.format( "Nebula finish event at beacon %d (%d,%d)", curBeacon.id, curBeacon.x, curBeacon.y ) );
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

				/* Pick a random number of events from the distribution */
				int m = 0;
				if (ed.max != 0) {
					log.info( String.format( "Generate the number of events of distribution %s", ed.name ) );
					m = (rng.rand() % (ed.max + 1 - ed.min)) + ed.min;
				}

				int i = 0;
				while ((i<m) && (!beaconIds.isEmpty())) {
					/* Choose a random empty beacon */
					log.info( String.format( "Choose the beacon to apply event" ) );
					int b = rng.rand() % beaconIds.size();
					GeneratedBeacon gb = genBeaconList.get(beaconIds.get(b));

					/* Check if the beacon is empty */
					if (gb.event == null) {
						log.info( String.format( "Generate event %s for beacon %d", ed.name, beaconIds.get(b) ) );
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
					log.info( String.format( "Generate event NEUTRAL for beacon %d", beaconIds.get(b) ) );
					gb.event = RandomEvent.loadEventId("NEUTRAL", rng);
				}
			}

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
	 * TODO: This code's a guess. The exact algorithm and threshold have not
	 * been verified, but it seems to work.
	 */
	public double calculateIsolation( GeneratedSectorMap genMap ) {
		double result = 0;

		List<GeneratedBeacon> genBeaconList = genMap.getGeneratedBeaconList();

		for ( int i=0; i < genBeaconList.size(); i++ ) {
			double minDist = 0d;
			boolean measured = false;

			for ( int j=0; j < genBeaconList.size(); j++ ) {
				if ( i == j ) continue;

				GeneratedBeacon a = genBeaconList.get( i );
				GeneratedBeacon b = genBeaconList.get( j );
				Point aLoc = a.getLocation();
				Point bLoc = b.getLocation();

				double d = Math.hypot( aLoc.x - bLoc.x, aLoc.y - bLoc.y );
				if ( !measured ) {
					minDist = d;
					measured = true;
				} else {
					minDist = Math.min( minDist, d );
				}
			}

			//if ( measured ) log.info( String.format( "%5.2f", minDist ) );

			if ( measured ) {
				result = Math.max( result, minDist );
			}
		}

		return result;
	}

}
