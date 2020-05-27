package net.blerf.ftl.parser.shiplayout;

import java.util.ArrayList;
import java.util.List;
import java.util.AbstractMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import net.blerf.ftl.parser.DataManager;
import net.blerf.ftl.parser.random.RandRNG;
import net.blerf.ftl.model.shiplayout.ShipLayout;
import net.blerf.ftl.model.shiplayout.ShipLayoutRoom;

/**
 * A generator of ship layout.
 *
 * @see net.blerf.ftl.model.shiplayout.ShipLayout
 * @see net.blerf.ftl.parser.random.NativeRandom
 */
public class RandomShipLayout {

	// private static final Logger log = LoggerFactory.getLogger( RandomShipLayout.class );
	private static final Logger log = NOPLogger.NOP_LOGGER;

	protected RandRNG rng;

	private static Set<Integer> uniqueCrewNames = null;

	public RandomShipLayout( RandRNG rng ) {
		this.rng = rng;
	}

	public void setRNG( RandRNG newRNG ) {
		rng = newRNG;
	}

	public RandRNG getRNG() {
		return rng;
	}

	public void setUniqueNames( Set<Integer> un ) {
		uniqueCrewNames = un;
	}

	public class RoomSquare {
		public int roomId;
		public int squareId;

		/* Cell coordinates in game units */
		public int x;
		public int y;
	}

	public void generateShipLayout( int seed, String shipLayoutId ) {

		rng.srand( seed );

		// ShipLayout shipLayout = DataManager.getInstance().getShipLayout( shipLayoutId );
		ShipLayout shipLayout = DataManager.get().getShipLayout( shipLayoutId );

		/* Pick a random square in each room */
		List<RoomSquare> roomSquares = new ArrayList<RoomSquare>();

		int roomCount = shipLayout.getRoomCount();
		for ( int r=0; r < roomCount; r++ ) {
			ShipLayoutRoom layoutRoom = shipLayout.getRoom( r );

			/* Pick a random square in the room */
			RoomSquare square = new RoomSquare();
			square.roomId = r;
			square.squareId = rng.rand() % (layoutRoom.squaresH * layoutRoom.squaresV);
			square.x = layoutRoom.locationX + square.squareId % layoutRoom.squaresH;
			square.y = layoutRoom.locationY + square.squareId / layoutRoom.squaresH;

			/* Translate to game coordinates */
			square.x = square.x * 35 + 17;
			square.y = square.y * 35 + 87;

			if (log.isDebugEnabled())
				log.debug( String.format( "Room %d has coords %d - %d", square.roomId, square.x, square.y ) );
			roomSquares.add(square);
		}

		List<AbstractMap.SimpleEntry<RoomSquare, RoomSquare>> squarePairs = new ArrayList<AbstractMap.SimpleEntry<RoomSquare, RoomSquare>>();

		for (RoomSquare square1 : roomSquares)
			for (RoomSquare square2 : roomSquares) {
				if (square1.roomId == square2.roomId) continue;

				/* Compute euclidian distance between the two rooms */
				double distance = Math.sqrt((square1.x - square2.x) * (square1.x - square2.x) + (square1.y - square2.y) * (square1.y - square2.y));

				if ((int)distance < 107) {

					boolean isPair = false;
					/* Check if there is already a pair between the two squares */
					for (AbstractMap.SimpleEntry<RoomSquare, RoomSquare> p : squarePairs) {
						if (((p.getKey().roomId == square1.roomId) && (p.getValue().roomId == square2.roomId)) ||
							((p.getKey().roomId == square2.roomId) && (p.getValue().roomId == square1.roomId))) {
							isPair = true;
							break;
						}
					}

					if (isPair) continue;

					/* If the two squares are not aligned,
					 * generate a Square from coords of the two squares */
					RoomSquare extraSquare = null;
					if ((square1.x != square2.x) && (square1.y != square2.y)) {
						extraSquare = new RoomSquare();
						int rr = rng.rand();
						if (log.isDebugEnabled())
							log.debug( String.format( "Rooms %d - %d value is %d", square1.roomId, square2.roomId, rr ) );

						if ((rr & 0x1) == 0) {
							extraSquare.x = square1.x;
							extraSquare.y = square2.y;
						}
						else {
							extraSquare.x = square2.x;
							extraSquare.y = square1.y;
						}
					}

					/* Detect if there is a third square between the pairs */
					isPair = false;
					for (RoomSquare square3 : roomSquares) {
						if (square3.roomId == square1.roomId) continue;
						if (square3.roomId == square2.roomId) continue;
						if (extraSquare == null) {
							if (middleSquare(square1, square2, square3)) {
								isPair = true;
								break;
							}
						}
						else {
							if (middleSquare(square1, extraSquare, square3)) {
								isPair = true;
								break;
							}
							if (middleSquare(extraSquare, square2, square3)) {
								isPair = true;
								break;
							}
						}
					}

					if (isPair) {
						if (log.isDebugEnabled())
							log.debug( String.format( "Found middle room" ) );
						continue;
					}

					/* Insert the square pair */
					squarePairs.add(new AbstractMap.SimpleEntry<RoomSquare, RoomSquare>(square1, square2));
				}
			}

		for (int k = 0; k < 6; k++)
			rng.rand();

		/* Generate crew names */
		if (uniqueCrewNames != null) {
			/* Generate 3 names. TODO: get that from ship layout */
			for (int k = 0; k < 3; k++) {
				int n = rng.rand() % 169; // TODO: Magic number, look at (sorted?) crew names

				while (uniqueCrewNames.contains(n)) {
					n = rng.rand() % 169;
				}
				uniqueCrewNames.add(n);
			}
		}

		for (int k = 0; k < 3; k++) {
			rng.rand(); // 0x521559 -> 0x51d7e2
			rng.rand(); // 0x5216c3
			rng.rand(); // 0x5216c3
		}

		for (int k = 0; k < 48; k++) {
			rng.rand(); // 0x56e351 -> 0x52db6b
			rng.rand();	// 0x56e351 -> 0x52de31
		}

		for (int k = 0; k < 3; k++) {
			rng.rand(); // 0x511116 -> 0x68a266 -> 0x689eea
			rng.rand(); // 0x511116 -> 0x68a266 -> 0x689f03
			rng.rand(); // 0x511116 -> 0x68a266 -> 0x689f7a
			rng.rand(); // 0x511116 -> 0x68a266 -> 0x689f93
			rng.rand(); // 0x511116 -> 0x68a266 -> 0x68a00c
		}
	}

	/* Returns if square3 is between square1 and square2 on a same axis */
	private boolean middleSquare(RoomSquare square1, RoomSquare square2, RoomSquare square3) {
		if ((square1.x == square3.x) && (square1.y == square3.y))
			return true;
		if ((square2.x == square3.x) && (square2.y == square3.y))
			return true;

		if ((square1.x == square2.x) && (square1.x == square3.x)) {
			if ((square1.y < square3.y) && (square3.y < square2.y))
				return true;
			if ((square2.y < square3.y) && (square3.y < square1.y))
				return true;
			return false;
		}
		else if ((square1.y == square2.y) && (square1.y == square3.y)) {
			if ((square1.x < square3.x) && (square3.x < square2.x))
				return true;
			if ((square2.x < square3.x) && (square3.x < square1.x))
				return true;
		}

		return false;
	}
}
