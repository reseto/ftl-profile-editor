package net.blerf.ftl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Getter
@Setter
@NoArgsConstructor
@XmlRootElement( name = "ship" )
@XmlAccessorType( XmlAccessType.FIELD )
public class ShipEvent {

	// copy constructor instead of cloning the object later
//	public ShipEvent(ShipEvent o) {
//		this.id = o.getId();
//		this.load = o.getLoad();
//		this.hostile = o.isHostile();
//		this.seed = o.getSeed();
//		this.autoBlueprintId = o.getAutoBlueprintId();
//	}

	@XmlAttribute( name = "name" )
	private String id;

	@XmlAttribute( name = "load", required = false )
	private String load;

	@XmlAttribute
	private boolean hostile = false;

	private int seed;

	@XmlAttribute( name = "auto_blueprint" )
	private String autoBlueprintId;

	@Override
	public String toString() {
		if (id != null)
			return ""+id;
		else if (load != null)
			return ""+load;
		return "<null>";
	}
}
