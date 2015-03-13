/**
 * @(#)VersionInfoExceptionBean.java
 *
 *
 * @author 
 * @version 1.00 2012/11/28
 */
package net.ddp2p.common.WSupdate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
 
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VersionInfoException", propOrder = {
    "message"
})
public class VersionInfoExceptionBean {
 
    protected String message;
 
    public String getMessage() {
        return message;
    }

    public void setMessage(String value) {
        this.message = value;
    }
}
