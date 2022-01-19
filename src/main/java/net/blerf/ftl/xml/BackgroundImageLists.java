package net.blerf.ftl.xml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "imageLists")
@XmlAccessorType(XmlAccessType.FIELD)
public class BackgroundImageLists {

    @XmlElement(name = "imageList")
    private List<BackgroundImageList> imageLists = new ArrayList<BackgroundImageList>();


    public void setImageLists(List<BackgroundImageList> imageLists) {
        this.imageLists = imageLists;
    }

    public List<BackgroundImageList> getImageLists() {
        return imageLists;
    }
}
