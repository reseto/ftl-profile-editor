package net.blerf.ftl.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;


@XmlRootElement( name = "event" )
@XmlAccessorType( XmlAccessType.FIELD )
public class FTLEvent implements Cloneable {
	@XmlAttribute( name = "name", required = false )
	private String id;

	@XmlAttribute
	private String load;

	@XmlAttribute
	private boolean unique;

	@XmlElement
	private NamedText text;

	@XmlElement(name = "img", required = false)
	private BackgroundImage image;

	@XmlElement(name = "choice", required = false)
	private List<Choice> choiceList;

	@XmlElement
	private ShipEvent ship;

	@XmlElement(name = "item_modify", required = false)
	private ItemList itemList;

	public static class Reward {
		@XmlAttribute
		public String type;

		@XmlAttribute
		public int min = 0;

		@XmlAttribute
		public int max = 0;

		public int value;
	}

	@XmlAccessorType( XmlAccessType.FIELD )
	public static class ItemList {
		@XmlElement( name = "item" )
		public List<Reward> items;
	}

	@XmlElement
	private AutoReward autoReward;

	@XmlAccessorType( XmlAccessType.NONE )
	public static class AutoReward {
		@XmlAttribute( name = "level", required = false )
		public String level;

		@XmlValue
		public String reward;

		public int scrap = 0;
		public int resources[] = {0, 0, 0};
		public String weapon = null;
		public String augment = null;
		public String drone = null;
	}

	@XmlElement
	private CrewMember crewMember;

	@XmlAccessorType( XmlAccessType.NONE )
	public static class CrewMember {
		@XmlAttribute
		public int amount = 0;

		@XmlAttribute( name = "class" )
		public String id = null;

		@XmlAttribute
		public int weapons = -1;

		@XmlAttribute
		public int shields = -1;

		@XmlAttribute
		public int pilot = -1;

		@XmlAttribute
		public int engines = -1;

		@XmlAttribute
		public int combat = -1;

		@XmlAttribute
		public int repair = -1;

		@XmlAttribute( name = "all_skills" )
		public int all_skills = -1;

		@XmlValue
		public String name = null;
	}

	@XmlElement
	private Item weapon = null;

	@XmlElement
	private Item augment = null;

	@XmlElement
	private Item drone = null;

	@XmlAccessorType( XmlAccessType.NONE )
	public static class Item {
		@XmlAttribute
		public String name = null;
	}

	@XmlElement
	private Boarders boarders = null;

	@XmlAccessorType( XmlAccessType.NONE )
	public static class Boarders {
		@XmlAttribute
		public int min = 0;

		@XmlAttribute
		public int max = 0;

		@XmlAttribute( name = "class" )
		public String name = null;
	}


	public Object clone() {
		FTLEvent o = null;
		try {
			o = (FTLEvent)super.clone();
		} catch(CloneNotSupportedException cnse) {
			cnse.printStackTrace(System.err);
		}

		o.id = id;
		o.load = load;
		o.unique = unique;

		if (text != null) {
			o.text = (NamedText)text.clone();
		}

		if (choiceList != null) {
			o.choiceList = new ArrayList<Choice>(choiceList.size());
			for (Choice c : choiceList) {
				Choice newC = new Choice();
				newC.setHidden(c.getHidden());
				newC.setReq(c.getReq());
				newC.setLevel(c.getLevel());
				if (c.getText() != null)
					newC.setText((NamedText)c.getText().clone());
				if (c.getEvent() != null)
					newC.setEvent((FTLEvent)c.getEvent().clone());
				o.choiceList.add(newC);
			}
		}
		if (ship != null) {
			o.ship = new ShipEvent();
			o.ship.setId(ship.getId());
			o.ship.setHostile(ship.getHostile());
			o.ship.setLoad(ship.getLoad());
			o.ship.setAutoBlueprintId(ship.getAutoBlueprintId());
		}

		if (itemList != null) {
			ItemList il = new ItemList();
			ItemList oil = o.getItemList();

			il.items = new ArrayList<Reward>(oil.items.size());
			for (Reward i : oil.items) {
				Reward newI = new Reward();
				newI.type = i.type;
				newI.min = i.min;
				newI.max = i.max;
				il.items.add(newI);
			}
			o.setItemList(il);
		}

		if (autoReward != null) {
			o.autoReward = new AutoReward();
			o.autoReward.level = autoReward.level;
			o.autoReward.reward = autoReward.reward;
		}

		if (crewMember != null) {
			o.crewMember = new CrewMember();
			o.crewMember.amount = crewMember.amount;
			o.crewMember.id = crewMember.id;
			o.crewMember.weapons = crewMember.weapons;
			o.crewMember.shields = crewMember.shields;
			o.crewMember.pilot = crewMember.pilot;
			o.crewMember.engines = crewMember.engines;
			o.crewMember.combat = crewMember.combat;
			o.crewMember.repair = crewMember.repair;
			o.crewMember.all_skills = crewMember.all_skills;
			o.crewMember.name = crewMember.name;
		}

		if (weapon != null) {
			o.weapon = new Item();
			o.weapon.name = weapon.name;
		}

		if (augment != null) {
			o.augment = new Item();
			o.augment.name = augment.name;
		}

		if (drone != null) {
			o.drone = new Item();
			o.drone.name = drone.name;
		}

		if (boarders != null) {
			o.boarders = new Boarders();
			o.boarders.min = boarders.min;
			o.boarders.max = boarders.max;
			o.boarders.name = boarders.name;
		}

		return o;
	}

	public String getId() {
		return id;
	}

	public void setId( String id ) {
		this.id = id;
	}

	public String getLoad() {
		return load;
	}

	public void setLoad( String load ) {
		this.load = load;
	}

	public NamedText getText() {
		return text;
	}

	public void setText( NamedText text ) {
		this.text = text;
	}

	public BackgroundImage getImg() {
		return image;
	}

	public void setImg( BackgroundImage image ) {
		this.image = image;
	}

	public boolean getUnique() {
		return unique;
	}

	public void setUnique( boolean unique ) {
		this.unique = unique;
	}

	public List<Choice> getChoiceList() {
		return choiceList;
	}

	public void setChoiceList( List<Choice> choiceList ) {
		this.choiceList = choiceList;
	}

	public ItemList getItemList() {
		return itemList;
	}

	public void setItemList( ItemList itemList ) {
		this.itemList = itemList;
	}

	public ShipEvent getShip() {
		return ship;
	}

	public void setShip( ShipEvent ship ) {
		this.ship = ship;
	}

	public AutoReward getAutoReward() {
		return autoReward;
	}

	public void setAutoReward( AutoReward autoReward ) {
		this.autoReward = autoReward;
	}

	public CrewMember getCrewMember() {
		return crewMember;
	}

	public void setCrewMember( CrewMember crewMember ) {
		this.crewMember = crewMember;
	}

	public Item getWeapon() {
		return weapon;
	}

	public void setWeapon( Item weapon ) {
		this.weapon = weapon;
	}

	public Item getAugment() {
		return augment;
	}

	public void setAugment( Item augment ) {
		this.augment = augment;
	}

	public Item getDrone() {
		return drone;
	}

	public void setDrone( Item drone ) {
		this.drone = drone;
	}

	public Boarders getBoarders() {
		return boarders;
	}

	public void setBoarders( Boarders boarders ) {
		this.boarders = boarders;
	}

	@Override
	public String toString() {
		if (id != null)
			return ""+id;
		else if (load != null)
			return ""+load;
		return "<NONAME>";
	}

	private StringBuilder indent(StringBuilder sb, int level) {
		sb.append(new String(new char[level]).replaceAll("\0", "    "));
		return sb;
	}

	public String toDescription(int level) {
		StringBuilder sb = new StringBuilder();
		if (id != null)
			indent(sb, level).append("id: ").append(id).append("\n");

		if (unique)
			indent(sb, level).append("unique: true\n");

		if (text != null)
			indent(sb, level).append("text: ").append(text.getText()).append("\n");

		if (ship != null)
			indent(sb, level).append("ship: ").append(ship.toString()).append("\n");

		if (itemList != null)
			for (Reward i : itemList.items)
				indent(sb, level).append("item_modify: ").append(i.type).append(" with quantity ").append(i.value).append("\n");

		if (autoReward != null) {
			indent(sb, level).append("autoreward level ").append(autoReward.level).append(" and reward ").append(autoReward.reward).append(":\n");
			indent(sb, level+1).append("scrap: ").append(autoReward.scrap).append("\n");
			indent(sb, level+1).append("fuel: ").append(autoReward.resources[0]).append("\n");
			indent(sb, level+1).append("missiles: ").append(autoReward.resources[1]).append("\n");
			indent(sb, level+1).append("droneparts: ").append(autoReward.resources[2]).append("\n");
			if (autoReward.weapon != null)
				indent(sb, level+1).append("weapon: ").append(autoReward.weapon).append("\n");
			if (autoReward.augment != null)
				indent(sb, level+1).append("augment: ").append(autoReward.augment).append("\n");
			if (autoReward.drone != null)
				indent(sb, level+1).append("drone: ").append(autoReward.drone).append("\n");
		}

		if (weapon != null)
			indent(sb, level).append("weapon: ").append(weapon.name).append("\n");

		if (augment != null)
			indent(sb, level).append("augment: ").append(augment.name).append("\n");

		if (drone != null)
			indent(sb, level).append("drone: ").append(drone.name).append("\n");

		if (boarders != null) {
			indent(sb, level).append("boarders: ").append("\n");
			indent(sb, level+1).append("min: ").append(boarders.min).append("\n");
			indent(sb, level+1).append("max: ").append(boarders.max).append("\n");
			indent(sb, level+1).append("class: ").append(boarders.name).append("\n");
		}

		if (crewMember != null) {
			indent(sb, level).append("crew: ").append("\n");
			indent(sb, level+1).append("amount: ").append(crewMember.amount).append("\n");
			if (crewMember.id != null)
				indent(sb, level+1).append("id: ").append(crewMember.id).append("\n");
			indent(sb, level+1).append("weapons: ").append(crewMember.weapons).append("\n");
			indent(sb, level+1).append("shields: ").append(crewMember.shields).append("\n");
			indent(sb, level+1).append("pilot: ").append(crewMember.pilot).append("\n");
			indent(sb, level+1).append("engines: ").append(crewMember.engines).append("\n");
			indent(sb, level+1).append("combat: ").append(crewMember.combat).append("\n");
			indent(sb, level+1).append("repair: ").append(crewMember.repair).append("\n");
			indent(sb, level+1).append("all_skills: ").append(crewMember.all_skills).append("\n");
			if (crewMember.name != null)
				indent(sb, level+1).append("name: ").append(crewMember.name).append("\n");
		}

		sb.append("\n");

		if (choiceList != null) {
			for (Choice c : choiceList) {
				indent(sb, level).append("choice:\n");
				sb.append(c.toDescription(level+1));
			}
		}

		return sb.toString();
	}

}
