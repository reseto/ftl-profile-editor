package net.blerf.ftl.parser.sectormap;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.xml.FTLEvent;
import net.blerf.ftl.xml.FTLEventList;
import net.blerf.ftl.xml.SectorDescription;
import net.blerf.ftl.xml.Choice;
import net.blerf.ftl.xml.NamedText;
import net.blerf.ftl.xml.TextList;

import net.blerf.ftl.parser.sectormap.GeneratedBeacon;
import net.blerf.ftl.parser.sectormap.GeneratedSectorMap;
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

	public String sectorId = "STANDARD_SPACE";

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

			List<GeneratedBeacon> genBeaconList = genMap.getGeneratedBeaconList();

			SectorDescription tmpDesc = DataManager.getInstance().getSectorDescriptionById( sectorId );

			// List<DefaultDeferredText> titlesDeferred = tmpDesc.getNameList().names;
			// List<String> titleList = new ArrayList<String>( titlesDeferred.size() );
			// for ( DefaultDeferredText t : titlesDeferred ) {
			// 	titleList.add( t.getTextValue() );
			// }
			// Sector tmpSector = new Sector( tmpDesc.isUnique(), tmpDesc.getMinSector(), tmpDesc.getId(), titleList );
			// result.add( tmpSector );


			/* Generate starting beacon position: 0x4e7b95 */
			n = rng.rand() & 3;

			/* Generate starting beacon event: 0x4e7f57 */
			String startEvent = tmpDesc.getStartEvent();

			genBeaconList.get(n).event = loadEventId(startEvent, rng);

			/* Generate ending beacon position: two rands at 0x4e8032 and 0x4e804d */
			int r, c;
			do {
				r = rng.rand() & 3; // 0x4a3681
				c = (rng.rand() & 1) + 4; // 0x4a3681
				if ( false ) { // Some condition
					if ( false ) { // Some other condition
						c = (rng.rand() & 1) + 3;
					}
					else {
						c = (rng.rand() & 1) + 2;
					}
				}
				/* Check that the position has a beacon in it, otherwise loop */
			} while ((c*4+r) >= genBeaconList.size());

			/* Generate ending beacon event ("FINISH_BEACON") */
			genBeaconList.get(c*4+r).event = loadEventId("FINISH_BEACON", rng);


			/* Place NEBULA beacons? */

			/* Pull one random value (ranged?) */

			/* For each beacon type (break if no more beacon left)
			 *     choose a random number from min to max of that type
			 *     iterate for that number (break if no more beacon left)
			 *         choose a random beacon (iterate until beacon is free)
			 *         choose a random event of that type
			 */

			/* For each beacon left:
			 *     choose a random "NEUTRAL" event
			 */

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

	/**
	 * Load an event
	 * use gdb with:
	 *   break *0x4a2c38
	 *   commands
	 *   silent
	 *   printf "load event %s\n",(char*)*$rsi
	 *   cont
	 *   end
	 */
	public FTLEvent loadEvent( FTLEvent event, RandRNG rng ) {
		log.info( String.format( "Load event %s", event.toString() ) );

		/* If there's a load attribute, load the corresponding event */
		String load = event.getLoad();
		if (load != null) {
			return loadEventId(load, rng);
		}

		/* Handle text */
		NamedText text = event.getText();
		load = text.getLoad();
		if (load != null) {
			TextList list = DataManager.getInstance().getTextListById( load );
			if (list == null) {
				throw new UnsupportedOperationException( String.format( "Could not find text list %s", load ) );
			}
			List<NamedText> textList = list.getTextList();

			if (textList.size() == 0) {
				throw new UnsupportedOperationException( String.format( "No more text left in textlist %s", load ) );
			}

			int n = rng.rand() % textList.size();
			event.setText(textList.get(n));
		}

		/* Randomize item quantities */
		int ivar12 = 100;
		if (false) {
			ivar12 = 5; // random range
		}

		// 0x4a3681
		/* Randomize missile quantity */
		int n = rng.rand();
		if ((n & 3) < ivar12) {
			int p = randomizeQuantity(event, rng, "missiles");
			if (p > 0)
				ivar12 -= 1;
		}

		// 0x4a36cb
		/* Randomize drone quantity */
		n = rng.rand();
		if ((n % 3) < ivar12) {
			int p = randomizeQuantity(event, rng, "drones");
			if (p > 0)
				ivar12 -= 1;
		}

		// 0x4a371d
		/* Randomize fuel quantity */
		n = rng.rand();
		if ((n & 1) < ivar12) {
			int p = randomizeQuantity(event, rng, "fuel");
			if (p > 0)
				ivar12 -= 1;
		}

 		// 0x4a3764
		/* Randomize scrap quantity */
		rng.rand();
		if (0 < ivar12) {
			int p = randomizeQuantity(event, rng, "scrap");
			if (p > 0)
				ivar12 -= 1;
		}

		/* Browse each choice, and load the corresponding event */
		List<Choice> choiceList = event.getChoiceList();
		if (choiceList != null) {
			for ( int i=0; i < choiceList.size(); i++ ) {
				Choice choice = choiceList.get(i);
				FTLEvent choiceEvent = choice.getEvent();
				choice.setEvent(loadEvent(choiceEvent, rng));
			}
		}

		// 0x4a4751
		if (true) { // some value == -1 (so uninitialized)
			n = rng.rand();
			int a = 1; // some value
			int b = 5; // some value
			int p = n % (b + 1 - a) + a;
		}

		return event;
	}

	/**
	 * Load an event from an event id.
	 */
	public FTLEvent loadEventId( String id, RandRNG rng ) {

		log.info( String.format( "Load event id %s", id ) );

		/* First, check if the id correspond to an event list */

		FTLEventList list = DataManager.getInstance().getEventListById( id );
		if (list != null) {
			List<FTLEvent> eventList = list.getEventList();

			int e = rng.rand() % eventList.size(); // TODO: correct formula
			return loadEvent(eventList.get(e), rng);
		}

		/* Get the event */
		FTLEvent event = DataManager.getInstance().getEventById( id );

		return loadEvent(event, rng);
	}

	/**
	 * Randomize an item quantity
	 */
	public int randomizeQuantity( FTLEvent event, RandRNG rng, String id ) {

		FTLEvent.ItemList itemList = event.getItemList();
		if (itemList == null)
			return 0;

		// log.info( String.format( "Load event id %s", id ) );

		for ( int i=0; i < itemList.items.size(); i++ ) {
			FTLEvent.Item item = itemList.items.get(i);
			if (item.type.equals(id)) {
				if (item.max != 0) {
					int r = item.max + 1 - item.min;
					item.value = (rng.rand() % r) + item.min;
					itemList.items.set(i, item);
					event.setItemList(itemList);
					return item.value;
				}
				return 0;
			}
		}

		return 0;
	}
}
