package net.blerf.ftl.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;


/**
 * One of the "text" tags in lookup files.
 */
@XmlRootElement( name = "text" )
@XmlAccessorType( XmlAccessType.FIELD )
public class NamedText implements Cloneable {

	@XmlAttribute( name = "name" )
	private String id;

	@XmlAttribute( name = "load" )
	private String load;

	@XmlValue
	private String text;

	public Object clone() {
		NamedText o = null;
		try {
			o = (NamedText)super.clone();
		} catch(CloneNotSupportedException cnse) {
			cnse.printStackTrace(System.err);
		}

		o.setId(getId());
		o.setLoad(getLoad());
		o.setText(getText());

		return o;
	}


	public void setId( String id ) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setLoad( String load ) {
		this.load = load;
	}

	public String getLoad() {
		return load;
	}

	public void setText( String s ) {
		text = s;
	}

	public String getText() {
		return text;
	}
}
