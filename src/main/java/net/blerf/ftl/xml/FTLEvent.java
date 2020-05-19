package net.blerf.ftl.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.blerf.ftl.xml.DefaultDeferredText;
import net.blerf.ftl.xml.FTLEvent;
import net.blerf.ftl.xml.NamedText;
import net.blerf.ftl.xml.BackgroundImage;
import net.blerf.ftl.xml.Choice;
import net.blerf.ftl.xml.ShipEvent;


@XmlRootElement( name = "event" )
@XmlAccessorType( XmlAccessType.FIELD )
public class FTLEvent {
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

	private ItemList itemList;

	public static class Item {
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
		public List<Item> items;
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

	@Override
	public String toString() {
		if (id != null)
			return ""+id;
		else if (load != null)
			return ""+load;
		return "<NONAME>";
	}
}
