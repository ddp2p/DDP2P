/**
 * @(#)VersionInfo_Exception.java
 *
 *
 * @author 
 * @version 1.00 2012/11/28
 */
package WSupdate;
 
import javax.xml.ws.WebFault;
 
@WebFault(name = "VersionInfoException", targetNamespace = "urn:ddWS")
public class VersionInfo_Exception extends Exception {
  private VersionInfoExceptionBean faultInfo;
  public VersionInfo_Exception(String message, VersionInfoExceptionBean faultInfo) {this.faultInfo = faultInfo;   }
  public VersionInfo_Exception(String message, VersionInfoExceptionBean faultInfo, 
     Throwable cause) { }
  public VersionInfoExceptionBean getFaultInfo() { return faultInfo; }
}


    
