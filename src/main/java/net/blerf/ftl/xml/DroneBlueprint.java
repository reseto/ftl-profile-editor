package net.blerf.ftl.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@XmlRootElement(name = "droneBlueprint")
@XmlAccessorType(XmlAccessType.FIELD)
public class DroneBlueprint {

    @XmlAttribute(name = "name")
    private String id;

    private String type;

    @XmlElement()
    private Integer locked;

    private DefaultDeferredText title;

    @XmlElement(name = "short")
    private DefaultDeferredText shortTitle;

    private DefaultDeferredText description;

    @XmlElement(name = "bp")
    private int blueprint;

    @XmlElement()
    private Integer cooldown;
    @XmlElement()
    private Integer dodge;
    @XmlElement()
    private Integer speed;

    private int power;
    private int cost;

    @XmlElement()
    private String droneImage;

    @XmlElement(name = "image")
    private String imagePath;  // InnerPath of a projectile anim sheet. Unused?

    @XmlElement()
    private String iconImage;  // TODO: FTL 1.5.4 introduced this. For iPad?

    @XmlElement(name = "weaponBlueprint")
    private String weaponId;

    private int rarity;

    @Override
    public String toString() {
        return "" + title;
    }
}
