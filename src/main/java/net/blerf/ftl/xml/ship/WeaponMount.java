package net.blerf.ftl.xml.ship;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class WeaponMount {

    @XmlAttribute
    public int x, y, gib;

    @XmlAttribute
    public boolean rotate, mirror;

    @XmlAttribute
    public String slide;
}
