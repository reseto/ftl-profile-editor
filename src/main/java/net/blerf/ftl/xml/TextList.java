package net.blerf.ftl.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.blerf.ftl.xml.NamedText;


@XmlRootElement(name = "textList")
@XmlAccessorType(XmlAccessType.FIELD)
public class TextList {
	@XmlAttribute(name = "name")
	private String id;

	@XmlElement(name = "text", required = false)
	private List<NamedText> textList;

	public String getId() {
		return id;
	}

	public void setId( String id ) {
		this.id = id;
	}

	public List<NamedText> getTextList() {
		return textList;
	}

	public void setTextList( List<NamedText> textList ) {
		this.textList = textList;
	}

	@Override
	public String toString() {
		return ""+id;
	}
}
