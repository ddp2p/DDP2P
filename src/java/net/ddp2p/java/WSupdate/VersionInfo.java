
package net.ddp2p.java.WSupdate;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for VersionInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="VersionInfo">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="date" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="script" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="trusted_public_key" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="data" type="{urn:ddWS}Downloadable" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="QOTD" type="{urn:ddWS}TestDef" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="testers" type="{urn:ddWS}TesterInfo" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VersionInfo", propOrder = {
    "version",
    "date",
    "script",
    "trustedPublicKey",
    "data",
    "qotd",
    "testers"
})
public class VersionInfo {

    @XmlElement(required = true)
    protected String version;
    @XmlElement(required = true)
    protected String date;
    @XmlElement(required = true)
    protected String script;
    @XmlElement(name = "trusted_public_key", required = true)
    protected String trustedPublicKey;
    @XmlElement(nillable = true)
    protected List<Downloadable> data;
    @XmlElement(name = "QOTD", nillable = true)
    protected List<TestDef> qotd;
    @XmlElement(nillable = true)
    protected List<TesterInfo> testers;

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * Gets the value of the date property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDate() {
        return date;
    }

    /**
     * Sets the value of the date property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDate(String value) {
        this.date = value;
    }

    /**
     * Gets the value of the script property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getScript() {
        return script;
    }

    /**
     * Sets the value of the script property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setScript(String value) {
        this.script = value;
    }

    /**
     * Gets the value of the trustedPublicKey property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTrustedPublicKey() {
        return trustedPublicKey;
    }

    /**
     * Sets the value of the trustedPublicKey property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTrustedPublicKey(String value) {
        this.trustedPublicKey = value;
    }

    /**
     * Gets the value of the data property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the data property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getData().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Downloadable }
     * 
     * 
     */
    public List<Downloadable> getData() {
        if (data == null) {
            data = new ArrayList<Downloadable>();
        }
        return this.data;
    }

    /**
     * Gets the value of the qotd property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the qotd property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getQOTD().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TestDef }
     * 
     * 
     */
    public List<TestDef> getQOTD() {
        if (qotd == null) {
            qotd = new ArrayList<TestDef>();
        }
        return this.qotd;
    }

    /**
     * Gets the value of the testers property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the testers property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTesters().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TesterInfo }
     * 
     * 
     */
    public List<TesterInfo> getTesters() {
        if (testers == null) {
            testers = new ArrayList<TesterInfo>();
        }
        return this.testers;
    }

}
