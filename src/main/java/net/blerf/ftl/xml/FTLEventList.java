package net.blerf.ftl.xml;

import java.util.List;
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
@XmlRootElement(name = "eventList")
@XmlAccessorType(XmlAccessType.FIELD)
public class FTLEventList {
    @XmlAttribute(name = "name")
    private String id;

    @XmlElement(name = "event")
    private List<FTLEvent> eventList;

    @Override
    public String toString() {
        return "" + id;
    }
}
