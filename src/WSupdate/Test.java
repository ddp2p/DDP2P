
package WSupdate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Test complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Test">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="qualityRef" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="QoT" type="{http://www.w3.org/2001/XMLSchema}float" minOccurs="0"/>
 *         &lt;element name="RoT" type="{http://www.w3.org/2001/XMLSchema}float" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Test", propOrder = {
    "qualityRef",
    "qoT",
    "roT"
})
public class Test {

    protected Integer qualityRef;
    @XmlElement(name = "QoT")
    protected Float qoT;
    @XmlElement(name = "RoT")
    protected Float roT;

    /**
     * Gets the value of the qualityRef property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getQualityRef() {
        return qualityRef;
    }

    /**
     * Sets the value of the qualityRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setQualityRef(Integer value) {
        this.qualityRef = value;
    }

    /**
     * Gets the value of the qoT property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getQoT() {
        return qoT;
    }

    /**
     * Sets the value of the qoT property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setQoT(Float value) {
        this.qoT = value;
    }

    /**
     * Gets the value of the roT property.
     * 
     * @return
     *     possible object is
     *     {@link Float }
     *     
     */
    public Float getRoT() {
        return roT;
    }

    /**
     * Sets the value of the roT property.
     * 
     * @param value
     *     allowed object is
     *     {@link Float }
     *     
     */
    public void setRoT(Float value) {
        this.roT = value;
    }

}
