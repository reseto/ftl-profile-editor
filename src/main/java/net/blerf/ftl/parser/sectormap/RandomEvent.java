package net.blerf.ftl.parser.sectormap;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.blerf.ftl.parser.random.RandRNG;
import net.blerf.ftl.parser.DataManager;

import net.blerf.ftl.xml.FTLEvent;
import net.blerf.ftl.xml.FTLEventList;
import net.blerf.ftl.xml.SectorDescription;
import net.blerf.ftl.xml.Choice;
import net.blerf.ftl.xml.NamedText;
import net.blerf.ftl.xml.TextList;
import net.blerf.ftl.xml.ShipEvent;


/**
 * Event processing class.
 */
public final class RandomEvent {

	private static final Logger log = LoggerFactory.getLogger( RandomEvent.class );

	private static int sectorNumber = 0; // between 0 and 7
	private static int difficulty = 1; // between 0 and 2

	public static void setSectorNumber( int sn ) { sectorNumber = sn; }
	public static void setDifficulty( int d ) { difficulty = d; }

	/**
	 * Load an event from an event id.
	 */
	public static FTLEvent loadEventId( String id, RandRNG rng ) {

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
	 * Load an event.
	 * To print which event is loaded on the game, use gdb with:
	 *   break *0x4a2c38
	 *   commands
	 *   silent
	 *   printf "load event %s\n",(char*)*$rsi
	 *   cont
	 *   end
	 */
	public static FTLEvent loadEvent( FTLEvent event, RandRNG rng ) {
		log.info( String.format( "Load event %s", event.toString() ) );

		/* If there's a load attribute, load the corresponding event */
		String load = event.getLoad();
		if (load != null) {
			return loadEventId(load, rng);
		}

		/* Handle text */
		NamedText text = event.getText();
		if (text != null) {
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
		}

		/* item_modify offer */

		/* Randomize item quantities */
		int ivar12 = 100;
		if (false) {
			ivar12 = 5; // random range
		}

		// 0x4a3681
		/* Randomize missile quantity */
		int n = rng.rand();
		if ((n & 3) < ivar12) {
			int p = itemOfferQuantity(event, rng, "missiles");
			if (p > 0)
				ivar12 -= 1;
		}

		// 0x4a36cb
		/* Randomize drone quantity */
		n = rng.rand();
		if ((n % 3) < ivar12) {
			int p = itemOfferQuantity(event, rng, "drones");
			if (p > 0)
				ivar12 -= 1;
		}

		// 0x4a371d
		/* Randomize fuel quantity */
		n = rng.rand();
		if ((n & 1) < ivar12) {
			int p = itemOfferQuantity(event, rng, "fuel");
			if (p > 0)
				ivar12 -= 1;
		}

 		// 0x4a3764
		/* Randomize scrap quantity */
		rng.rand();
		if (0 < ivar12) {
			int p = itemOfferQuantity(event, rng, "scrap");
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

		/* autoReward */

		FTLEvent.AutoReward autoReward = event.getAutoReward();
		if (autoReward != null) {

			log.info( String.format( "Generating autoReward with level %s and type %s", autoReward.level, autoReward.reward ) );

			/* Alter sector number based on difficulty */
			int newSectorNumber = sectorNumber;
			if (difficulty == 0)
				newSectorNumber++;
			if ((difficulty == 2) && (newSectorNumber > 0))
				newSectorNumber--;

			/* Determine reward level */
			int rewardLevel = 0;
			if (autoReward.level.equals("LOW"))
				rewardLevel = 0;
			else if (autoReward.level.equals("MED"))
				rewardLevel = 1;
			else if (autoReward.level.equals("HIGH"))
				rewardLevel = 2;
			else if (autoReward.level.equals("RANDOM"))
				rewardLevel = rng.rand() % 3;
			else
				throw new UnsupportedOperationException( String.format( "Unknown reward level %s", autoReward.level ) );

			String resources[] = {"fuel", "missiles", "droneparts"};

			boolean extraItem = false;

			/* Standard reward */
			if (autoReward.reward.equals("standard")) {
				autoReward.scrap = autoRewardQuantity(rng, "scrap", rewardLevel, newSectorNumber);
				int resourceOne = rng.rand() % 3;
				int resourceTwo = rng.rand() % 3;
				while (resourceTwo == resourceOne)
					resourceTwo = rng.rand() % 3;

				autoReward.resources[resourceOne] = autoRewardQuantity(rng, resources[resourceOne], 0, 0);
				autoReward.resources[resourceTwo] = autoRewardQuantity(rng, resources[resourceTwo], 0, 0);

				if ((rng.rand() % 100) < 3)
					extraItem = true;
			}
			else if (autoReward.reward.equals("stuff")) {
				autoReward.scrap = autoRewardQuantity(rng, "scrap", 0, newSectorNumber);
				int resourceOne = rng.rand() % 3;
				int resourceTwo = rng.rand() % 3;
				while (resourceTwo == resourceOne)
					resourceTwo = rng.rand() % 3;

				autoReward.resources[resourceOne] = autoRewardQuantity(rng, resources[resourceOne], rewardLevel, 0);
				autoReward.resources[resourceTwo] = autoRewardQuantity(rng, resources[resourceTwo], rewardLevel, 0);

				if ((rng.rand() % 100) < 6)
					extraItem = true;

			}
			else if (autoReward.reward.equals("scrap_only")) {
				autoReward.scrap = autoRewardQuantity(rng, "scrap", rewardLevel, newSectorNumber);
			}
			else if (autoReward.reward.equals("fuel")) {
				autoReward.scrap = autoRewardQuantity(rng, "scrap", rewardLevel, newSectorNumber);
				autoReward.resources[0] = autoRewardQuantity(rng, "fuel", rewardLevel, 0);
			}
			else if (autoReward.reward.equals("missiles")) {
				autoReward.scrap = autoRewardQuantity(rng, "scrap", rewardLevel, newSectorNumber);
				autoReward.resources[1] = autoRewardQuantity(rng, "missiles", rewardLevel, 0);
			}
			else if (autoReward.reward.equals("droneparts")) {
				autoReward.scrap = autoRewardQuantity(rng, "scrap", rewardLevel, newSectorNumber);
				autoReward.resources[2] = autoRewardQuantity(rng, "droneparts", rewardLevel, 0);
			}
			else if (autoReward.reward.equals("fuel_only")) {
				autoReward.resources[0] = autoRewardQuantity(rng, "fuel", rewardLevel, 0);
			}
			else if (autoReward.reward.equals("missiles_only")) {
				autoReward.resources[1] = autoRewardQuantity(rng, "missiles", rewardLevel, 0);
			}
			else if (autoReward.reward.equals("droneparts_only")) {
				autoReward.resources[2] = autoRewardQuantity(rng, "droneparts", rewardLevel, 0);
			}

			if (extraItem) {
				int i = rng.rand() % 3;
				if (i == 0)
					autoReward.reward = "weapon";
				else if (i == 1)
					autoReward.reward = "augment";
				else
					autoReward.reward = "drone";
			}

			if (autoReward.reward.equals("weapon")) {
				rng.rand(); // TODO
				autoReward.weapon = "TODO";
			}
			else if (autoReward.reward.equals("augment")) {
				rng.rand(); // TODO
				autoReward.augment = "TODO";
			}
			else if (autoReward.reward.equals("drone")) {
				rng.rand(); // TODO
				autoReward.drone = "TODO";
			}
		}

		/* Ship event: generate the seed */
		ShipEvent se = event.getShip();
		if (se != null) {
			se.setSeed(rng.rand());
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
	 * Randomize an item quantity
	 */
	private static int itemOfferQuantity( FTLEvent event, RandRNG rng, String id ) {

		FTLEvent.ItemList itemList = event.getItemList();
		if (itemList == null)
			return 0;

		for ( int i=0; i < itemList.items.size(); i++ ) {
			FTLEvent.Item item = itemList.items.get(i);
			if (item.type.equals(id)) {
				if (item.max != 0) {
					int r = item.max + 1 - item.min;
					item.value = (rng.rand() % r) + item.min;
					itemList.items.set(i, item);
					event.setItemList(itemList);
					log.info( String.format( "Random quantity of %s is %d", id, item.value ) );
					return item.value;
				}
				return 0;
			}
		}
		return 0;
	}

	/**
	 * Compute autoReward quantity
	 */
	private static int autoRewardQuantity( RandRNG rng, String resource, int reward_level, int sector_level ) {

/*			StandardFixedDrop:
			- scrap:
			-- array is [0.5, 0.7, 0.8, 1.3, 1.3, 1.55] (min_low, max_low, min_med, etc.)
			-- chosen random number between [min*1000 ; max*1000+1]
			-- divide that number by 1000, and multiply with (sector * 6 + 0xf)

			- fuel:
			-- array is [1, 3, 2, 4, 3, 6]
			-- gives rand() % (max + 1 - min) + min

			- missiles:
			-- array is [1, 2, 2, 4, 4, 8]
			-- gives rand() % (max + 1 - min) + min

			- droneparts:
			-- array is [1, 1, 1, 1, 1, 2]
			-- gives rand() % (max + 1 - min) + min
*/

		final float scrap_min[] = {0.5f, 0.8f, 1.3f};
		final float scrap_max[] = {0.7f, 1.3f, 1.55f};

		final int fuel_min[] = {1, 2, 3};
		final int fuel_max[] = {3, 4, 6};

		final int missiles_min[] = {1, 2, 4};
		final int missiles_max[] = {2, 4, 8};

		final int droneparts_min[] = {1, 1, 1};
		final int droneparts_max[] = {1, 1, 2};

		if (resource.equals("scrap")) {
			float min = scrap_min[reward_level];
			float max = scrap_max[reward_level];

			int range = (int)(max*1000.0)+1 - (int)min;
			int qint = (int)min + (rng.rand() % range);

			int q = (int)(((float)qint / 1000.0f) * ((float) sector_level * 6 + 0xf));
			log.info( String.format( "autoReward scrap: %d", q ) );
			return q;
		}

		int min = 0, max = 0;
		if (resource.equals("fuel")) {
			min = fuel_min[reward_level];
			max = fuel_max[reward_level];
		}
		else if (resource.equals("missiles")) {
			min = missiles_min[reward_level];
			max = missiles_max[reward_level];
		}
		else if (resource.equals("droneparts")) {
			min = droneparts_min[reward_level];
			max = droneparts_max[reward_level];
		}

		int r = min + (rng.rand() % (max + 1 - min));
		log.info( String.format( "autoReward %s: %d", resource, r ) );

		return r;
	}

}
