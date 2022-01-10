package net.blerf.ftl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import net.blerf.ftl.xml.DefaultDeferredText;


@XmlRootElement( name = "ship" )
@XmlAccessorType( XmlAccessType.FIELD )
public class ShipEvent {

	@XmlAttribute( name = "name" )
	private String id;

	@XmlAttribute( name = "load", required = false )
	private String load;

	@XmlAttribute
	private boolean hostile = false;

	private int seed;

	@XmlAttribute( name = "auto_blueprint" )
	private String autoBlueprintId;

	// The rest is uninteresting. ;)


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

	public boolean getHostile() {
		return hostile;
	}

	public void setHostile( boolean hostile ) {
		this.hostile = hostile;
	}

	public int getSeed() {
		return seed;
	}

	public void setSeed( int seed ) {
		this.seed = seed;
	}

	public String getAutoBlueprintId() {
		return autoBlueprintId;
	}

	public void setAutoBlueprintId( String autoBlueprintId ) {
		this.autoBlueprintId = autoBlueprintId;
	}

	@Override
	public String toString() {
		if (id != null)
			return ""+id;
		else if (load != null)
			return ""+load;
		return "<null>";
	}
}
