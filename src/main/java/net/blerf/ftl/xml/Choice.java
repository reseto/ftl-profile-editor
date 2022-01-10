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


@XmlRootElement( name = "choice" )
@XmlAccessorType( XmlAccessType.FIELD )
public class Choice {
	@XmlAttribute( name = "hidden", required = false )
	private boolean hidden;

	@XmlAttribute( name = "req", required = false )
	private String req;

	@XmlAttribute( name = "lvl", required = false )
	private String level;

	@XmlElement(name = "text", required = false)
	private NamedText text;

	@XmlElement(name = "event", required = false)
	private FTLEvent event;

	public boolean getHidden() {
		return hidden;
	}

	public void setHidden( boolean hidden ) {
		this.hidden = hidden;
	}

	public String getReq() {
		return req;
	}

	public void setReq( String req ) {
		this.req = req;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel( String level ) {
		this.level = level;
	}

	public NamedText getText() {
		return text;
	}

	public void setText( NamedText text ) {
		this.text = text;
	}

	public FTLEvent getEvent() {
		return event;
	}

	public void setEvent( FTLEvent event ) {
		this.event = event;
	}

	private StringBuilder indent(StringBuilder sb, int level) {
		sb.append(new String(new char[level]).replaceAll("\0", "    "));
		return sb;
	}

	public String toDescription(int level) {
		StringBuilder sb = new StringBuilder();
		if (text != null)
			indent(sb, level).append("text: ").append(text.getText()).append("\n");
		if (event != null)
			indent(sb, level).append("event: \n").append(event.toDescription(level+1));

		return sb.toString();
	}
}
