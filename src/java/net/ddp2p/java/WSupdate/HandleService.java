/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 
		Author: Khalid Alhamed
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package net.ddp2p.java.WSupdate;

import javax.xml.ws.soap.SOAPFaultException;





///////////////////////////////////////////////
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.net.URL;
import java.util.Map;
import java.util.Hashtable;
import java.net.MalformedURLException;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.updates.ClientUpdates;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.java.WSupdate.DdWS;
import net.ddp2p.java.WSupdate.DdWSPortType;
import net.ddp2p.java.WSupdate.Downloadable;
import net.ddp2p.java.WSupdate.History;
import net.ddp2p.java.WSupdate.Test;
import net.ddp2p.java.WSupdate.TestDef;
import net.ddp2p.java.WSupdate.TesterInfo;
import net.ddp2p.java.WSupdate.VersionInfo;


///////////////////////////////////////////////
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

///////////////////////////////////////////////





///////////////////////////////////////////////
import java.security.*;
import java.security.spec.*;
import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.XMLStructure;
import javax.xml.ws.handler.MessageContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.BindingProvider;






///////////////////////////////////////////////
import java.security.Key;
import java.security.PublicKey; 
import java.security.MessageDigest;


class Request extends net.ddp2p.ASN1.ASNObj {
	Calendar date;
	String url;
	String peerGID;
	public Request(String gID, String url2, Calendar _date) {
		date = _date;
		url = url2;
		peerGID = gID;
	}

	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(peerGID, false));
		enc.addToSequence(new Encoder(url));
		enc.addToSequence(new Encoder(date));
		return enc;
	}

	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		throw new ASN1DecoderFail("Not supported");
		//return null;
	}
	byte[] sign(SK sk){
		Encoder enc = getEncoder();
		byte[] msg = enc.getBytes();
		byte[] signature = Util.sign(msg, sk);
		return signature;
	}
}

///////////////////////////////////////////////////////
 class DateInfo{
 	Calendar LocalDate;
 	Calendar CalculatedDate;
 	Calendar ServerDate;	
 } 
///////////////////////////////////////////////////////
 class Handler  implements SOAPHandler<SOAPMessageContext> {
	private static final boolean DEBUG = false;
    private boolean DEBUG_ = DEBUG || ClientUpdates.DEBUG;
	/////////////////////////////////////////////////////////////
	// change this to redirect output if desired
    private static PrintStream out = System.out;
    // validate signature values : 0=false; 1=true; -1= never validated 
    int valid = -1;
    String PK_Hex;
    
    public void setPK_Hex(String pk) { 	
        PK_Hex=pk;
    }
    public Set<QName> getHeaders() {
        return null;
    }

    public boolean handleMessage(SOAPMessageContext smc) {
        
        
        logToSystemOut(smc);
        return true;
    }

    public boolean handleFault(SOAPMessageContext smc) {
        logToSystemOut(smc);
        return true;
    }

    // nothing to clean up
    public void close(MessageContext messageContext) {
    }

    /*
     * Check the MESSAGE_OUTBOUND_PROPERTY in the context
     * to see if this is an outgoing or incoming message.
     * Write a brief message to the print stream and
     * output the message. The writeTo() method can throw
     * SOAPException or IOException
     */
    private void logToSystemOut(SOAPMessageContext smc) {
    	boolean DEBUG_ = DEBUG || ClientUpdates.DEBUG; 
        Boolean outboundProperty = (Boolean)
            smc.get (MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if (outboundProperty.booleanValue()) {
            if(DEBUG_)out.println("\nOutbound message:");
        } else {
            if(DEBUG_)out.println("\nInbound message:");
            	try{
		 
    		 validateSignature(smc.getMessage());
    	}catch(Exception e){
    	}
        }

        SOAPMessage message = smc.getMessage();
        try {
            if(DEBUG_)message.writeTo(out);
            if(DEBUG_)out.println("");   // just to add a newline
        } catch (Exception e) {
            out.println("Exception in handler: " + e);
        }
    }

	/////////////////////////////////////////////////////////////
	public int isValidSignature(){
		return valid;
	}
	private void validateSignature(SOAPMessage soapMessage) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		SOAPPart soapPart = soapMessage.getSOAPPart();
		Source source=null;
		try{
		 
    		 source = soapPart.getContent();
    	}catch(Exception e){
    	}
		
    	Node root = null;
    	Document doc = null;
    	DocumentBuilder db = null;
    	if (source instanceof DOMSource) {
      		root = ((DOMSource) source).getNode();
      		if (root instanceof Document) {
              doc = (Document) root;
           }
    	} else if (source instanceof SAXSource) {
      		InputSource inSource = ((SAXSource) source).getInputSource();
      		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      		dbf.setNamespaceAware(true);
      		db = dbf.newDocumentBuilder();
      	    doc = db.parse(inSource);
      		root = (Node) doc.getDocumentElement();
    	} else{// if (source instanceof JAXMStreamSource){
      		StreamSource streamSource = (StreamSource)source;
      		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      		dbf.setNamespaceAware(true);
			db = dbf.newDocumentBuilder();
      	    doc = db.parse(streamSource.getInputStream());
      		root = (Node) doc.getDocumentElement();
      		root= root.getParentNode();
    	}
    	/////////////////////////////////////////////
    	NodeList nl = doc.getElementsByTagNameNS
 	    (XMLSignature.XMLNS, "Signature");
        if (nl.getLength() == 0) {
            throw new Exception("Cannot find Signature element");
           //System.out.println("Cannot find Signature element");
        }
        
        KeyValueKeySelector kvks =new KeyValueKeySelector(); 
        DOMValidateContext valContext = new DOMValidateContext(kvks, nl.item(0));//(LoadPublicKey("", "RSA"),nl.item(0));
        XMLSignatureFactory factory =  XMLSignatureFactory.getInstance("DOM");
        XMLSignature signature = factory.unmarshalXMLSignature(valContext);
        if(signature.validate(valContext)) valid=1 ; else valid=0;
        if(DEBUG_) System.out.println("WSupdate:HS: valid="+valid);
        try{
        md.update(signature.getKeySelectorResult().getKey().getEncoded());
        String PK_WS = getHexString(md.digest());
        if(PK_WS.equals(PK_Hex)) valid=1 ; else valid=0;
        if(DEBUG_)System.out.println("Signature validation by comparing URL digest PK and empadded PK (1 or 0) "+valid);	
        if(DEBUG_)System.out.println("Public Key from SOAP: " + PK_WS);
        md.reset();
        if(DEBUG_)System.out.println("Public Key from DB: " + PK_Hex);
        //md.update(LoadPublicKey("", "RSA").getEncoded());
    	//System.out.println("Public Key from File: " + getHexString(md.digest()));
        }catch(Exception e){
        	System.out.print(e);
        } 
        
        
       //  PublicKey pub = keypair.getPublic();
		
    	
		/////////////////////////////////////////////
//		Element envelope = getFirstChildElement(root);
//        Element header = getFirstChildElement(envelope);
//		Element sigElement = getFirstChildElement(header);
//        DOMValidateContext valContext = new DOMValidateContext(LoadPublicKey("","RSA"), sigElement);
//        valContext.setIdAttributeNS(getNextSiblingElement(header),
//        "http://schemas.xmlsoap.org/soap/security/2000-12", "id");
      //  if(sig.validate(valContext)) valid=1 ; else valid=0;
		
		
		////////////////////////////////////////////
		
		
	}	
	private static Element getFirstChildElement(Node node) {
    Node child = node.getFirstChild();
    while ((child != null) && (child.getNodeType() != Node.ELEMENT_NODE)) {
      child = child.getNextSibling();
    }
    return (Element) child;
   }

   public static Element getNextSiblingElement(Node node) {
     Node sibling = node.getNextSibling();
     while ((sibling != null) && (sibling.getNodeType() != Node.ELEMENT_NODE)) {
       sibling = sibling.getNextSibling();
     }
     return (Element) sibling;
   }
   //////////////////////
   public PublicKey LoadPublicKey(String path, String algorithm)
			throws IOException, NoSuchAlgorithmException,
			InvalidKeySpecException {
		// Read Public Key.
		File filePublicKey = new File(path + "public.key");
		FileInputStream fis = new FileInputStream(path + "public.key");
		byte[] encodedPublicKey = new byte[(int) filePublicKey.length()];
		fis.read(encodedPublicKey);
		fis.close();

		// Generate PublicKey.
		KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
				encodedPublicKey);
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
		return publicKey ;
	}
	private String getHexString(byte[] b) {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}
}
 
public class HandleService {
    final static int  INVALID_SIGNATURE = 0;
    final static int  VALID_SIGNATURE = 1;
    final static int  INVALID_URL = 2;
    final static int  INVALID_DATE = 3;
    final static int  DEFAULTCODE = -1;
    static int statusCode=DEFAULTCODE; // -1 never used no error
    static String errorString=null;
    
    public static boolean DEBUG = false;
    private static boolean DEBUG_ = DEBUG || ClientUpdates.DEBUG;
    
 	public static URL isWSVersionInfoService(String site){
 		if(site.startsWith("wsdl:"))
 		    try{
 				return new URL(site.substring("wsdl:".length()));
 		    }catch (MalformedURLException ex) {
          		//System.out.println("error in url: "+ url );
         		return null;
        }
 		return null;
 	}
// 	public static boolean validateSignature(URL url){
// 		
// 	}
    public static void main(String[] args) {
    	String site = args[0];// "wsdl:http://localhost:6060/ddWS_doc5.php?wsdll&123331c847ba3a5d6a52e817a4d0109fe65ffbc2038f68c8db1c3f9793b50c0d"
    	System.out.println( "URL "+ args[0]);
    	Hashtable<Object,Object> context = new Hashtable<Object,Object>();
    	URL url = isWSVersionInfoService(args[0]);
    
		try {
			net.ddp2p.common.config.Application.setDB(new net.ddp2p.common.util.DBInterface("deliberation-app.db"/*args[1]*/));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}	
    	if(url==null) return;
    	net.ddp2p.common.updates.VersionInfo response = null;
    	while(response==null){  	
	    	response = getWSVersionInfo(url,"gid:1111",net.ddp2p.common.data.HandlingMyself_Peer.getMyPeerSK(),context);
	    	if (response!=null){
	        System.out.println("Web service response getVersion: " + response.version);
	        System.out.println("Web service response getUrl: " + response.data[0].url);
	    	} else System.out.println("Response = null");
	    }
//        ArrayList<Downloadable> l = (ArrayList<Downloadable>)response.getData();
//        Downloadable d= (Downloadable) l.toArray()[0];
//        System.out.println("Web service response getUrl: " + "  "+d.getUrl() + d.getFilename() );
    }
    public static Calendar getUpdateDate(Calendar serverDate, Calendar localDate){
    	String lDate = Util.getGeneralizedTime();
        Calendar newLocalDate = Util.getCalendar(lDate);
        long timeMil=0;
        if(serverDate.compareTo(localDate)==1)
        	timeMil = serverDate.getTimeInMillis() - localDate.getTimeInMillis() ;
        if(DEBUG) System.out.println("Time def btw server and client: "+timeMil);
    	return	Util.incCalendar(newLocalDate, (int)timeMil );
    }
    public static net.ddp2p.common.updates.VersionInfo getWSVersionInfo(URL url, String GID, SK myPeerSK, Hashtable<Object,Object> context){
        if(DEBUG_) System.out.println("call getWSVersionInfo on URL " + url);
        int i = url.toString().indexOf("&");
        if(i==-1){
        	if(DEBUG_) System.out.println("URL " + url +" has no PK hash");
        	return null;
        }
        String PK_hex=url.toString().substring(i+1);
        try{
        url = new URL(url.toString().substring(0,i));
        }catch (MalformedURLException ex) {
          		System.out.println("error in url: "+ url );
         		return null;
        }
        if(DEBUG_)System.out.println("pk= " + PK_hex+ "  url= " + url);
        DdWS service = new DdWS(url);
        if(DEBUG_) if(service==null) System.out.println("service=null");
        
 //service.getHandlerResolver().getHandlerChain(service.getDdWSPort()).add(new Handler());
        DdWSPortType DDver = service.getDdWSPort();
        //System.out.println("part: "+ service);
        
        BindingProvider bindingProvider = ((BindingProvider) DDver);
        
        List<javax.xml.ws.handler.Handler> handlerChain = bindingProvider.getBinding().getHandlerChain();
        Handler handler = new Handler();
        handler.setPK_Hex(PK_hex);
        handlerChain.add(handler);
        bindingProvider.getBinding().setHandlerChain(handlerChain);
        
         
        
        if(DEBUG_) if(DDver==null) System.out.println("DDver=null");
        // build history object
        Map<String, Object> c = bindingProvider.getRequestContext();
        Object URLloc = c.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
       /* To change the location address 
        *context.put(
          BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
          newAddress);
        */
        //System.out.println("loc: "+URLloc.toString());
        DateInfo d;
        if((d=(DateInfo)context.get(url.toString()))==null){
        	if(ClientUpdates.DEBUG || DEBUG) System.out.println(" Context = null");
            d = new DateInfo();
        } else{ 
        	if(ClientUpdates.DEBUG || DEBUG)System.out.println("localtime: "+d.LocalDate.getTime());
        	if(ClientUpdates.DEBUG || DEBUG)System.out.println("Caltime: "+d.CalculatedDate.getTime());
        	if(ClientUpdates.DEBUG || DEBUG)
        		if(d.ServerDate!=null)System.out.println("Servertime: "+d.ServerDate.getTime());
        }
        if(d.ServerDate!=null)  // server date set only with error
        	d.CalculatedDate = getUpdateDate(d.ServerDate, d.LocalDate);
        d.ServerDate=null;
        String lDate = Util.getGeneralizedTime();
        d.LocalDate = Util.getCalendar(lDate);
        if(d.CalculatedDate==null)
        	d.CalculatedDate= d.LocalDate;
        Request rq = new Request(GID, URLloc.toString(), d.CalculatedDate);
        String signature = Util.stringSignatureFromByte(rq.sign(myPeerSK));
        History h = new History();
        h.setGID(rq.peerGID);
        h.setUrl(rq.url);
        h.setDate(Encoder.getGeneralizedTime(rq.date));// date as generalized for server side
        h.setSignature(signature);
        VersionInfo response= null;
        context.put(url.toString(),d);
        try {
        	 response = DDver.getVersionInfo(h);
        }catch(SOAPFaultException e){
        	if(DEBUG)System.err.println("WSUpdate:HandleService:Code: " + e.getFault().getFaultCode() + ", Actor: "+e.getFault().getFaultActor()+", String: "+ e.getFault().getFaultString());
        	if(e.getFault().getFaultActor().trim().equals("date")){
        		statusCode = INVALID_DATE;
        		errorString = e.getFault().getFaultString();
        		d=(DateInfo)context.get(url.toString());
        		d.ServerDate= Util.getCalendar(errorString+".000Z");//Calendar.getInstance();
        		context.remove(url.toString());
        		context.put(url.toString(),d);
        		//d.ServerDate.setTime(new Date(Long.parseLong(errorString)));
        		if(DEBUG)System.out.println("WSUpdate:HandleService:Server: "+d.ServerDate.getTime() + "  Client: " +d.LocalDate.getTime());
        	} else if(e.getFault().getFaultActor().trim().equals("url")){
        		statusCode = INVALID_URL;
        		errorString = e.getFault().getFaultString();
        	}
        	return null;
        }
        statusCode = handler.isValidSignature();
        if(statusCode==INVALID_SIGNATURE){
        	return null;
        }
        net.ddp2p.common.updates.VersionInfo v = new net.ddp2p.common.updates.VersionInfo();
        v.version = response.getVersion();
        v.script = response.getScript();
        v.date = Util.getCalendar(response.getDate());
       // v.signature = Util.byteSignatureFromString(response.getSignature());
        v.data = new net.ddp2p.common.updates.Downloadable[response.getData().size()];
        Downloadable downloadable = null;
        for( i=0; i< v.data.length; i++)
        {
        	v.data[i] =  new net.ddp2p.common.updates.Downloadable();
        	downloadable= (Downloadable) response.getData().get(i);
        	v.data[i].filename = downloadable.getFileName();
        	v.data[i].url = downloadable.getUrl();
        	v.data[i].digest = Util.byteSignatureFromString(downloadable.getDigest());
        }
        v.releaseQD = new net.ddp2p.common.data.D_ReleaseQuality[response.getQOTD().size()];
        TestDef testDef = null;
        for( i=0; i< v.releaseQD.length; i++)
        {   
        	testDef = (TestDef) response.getQOTD().get(i);
        //	int index = testDef.getRef().intValue() - 1;
        	int index = testDef.getRef().intValue();
        	v.releaseQD[index] =  new net.ddp2p.common.data.D_ReleaseQuality();
        	
        	v.releaseQD[index]._quality = new String[testDef.getQualityStructure().size()];
        	for( int j=0; j< v.releaseQD[index]._quality.length; j++){
        	   v.releaseQD[index]._quality[j] = testDef.getQualityStructure().get(j);	
        	}
        	v.releaseQD[index].description = testDef.getDesc();
        }
        
        v.testers_data = new net.ddp2p.common.data.D_SoftwareUpdatesReleaseInfoByTester[response.getTesters().size()];
        TesterInfo testerInfo = null;
        for( i=0; i< v.testers_data.length; i++)
        {   
        	testerInfo = (TesterInfo) response.getTesters().get(i);
        	v.testers_data[i] = new net.ddp2p.common.data.D_SoftwareUpdatesReleaseInfoByTester();
        	v.testers_data[i].name = testerInfo.getName();
        	v.testers_data[i].public_key_hash = testerInfo.getDigestPK();
        	v.testers_data[i].tester_QoT = new float[response.getQOTD().size()]; // not all array elements are used  
        	v.testers_data[i].tester_RoT = new float[response.getQOTD().size()]; // not all array elements are used
        	for( int j=0; j< testerInfo.getTests().size(); j++){
        		//int index = ((Test)testerInfo.getTests().get(j)).getQualityRef().intValue() - 1;
        		int index = ((Test)testerInfo.getTests().get(j)).getQualityRef().intValue();
        		v.testers_data[i].tester_QoT[index] = ((Test)testerInfo.getTests().get(j)).getQoT().floatValue();
        		v.testers_data[i].tester_RoT[index] = ((Test)testerInfo.getTests().get(j)).getRoT().floatValue();
        	}
            v.testers_data[i].signature = Util.byteSignatureFromString(testerInfo.getSignature());
        } 
        
  		return v;
    }
    
}
 class KeyValueKeySelector extends KeySelector {
 public static PublicKey curPK; 
  public KeySelectorResult select(KeyInfo keyInfo,
      KeySelector.Purpose purpose,
      AlgorithmMethod method,
      XMLCryptoContext context)
    throws KeySelectorException {

    if (keyInfo == null) {
      throw new KeySelectorException("Null KeyInfo object!");
    }
    SignatureMethod sm = (SignatureMethod) method;
    List list = keyInfo.getContent();

    for (int i = 0; i < list.size(); i++) {
      XMLStructure xmlStructure = (XMLStructure) list.get(i);
      if (xmlStructure instanceof KeyValue) {
        PublicKey pk = null;
        try {
          pk = ((KeyValue)xmlStructure).getPublicKey();
        } catch (KeyException ke) {
          throw new KeySelectorException(ke);
        }
        // make sure algorithm is compatible with method
        if (algEquals(sm.getAlgorithm(), 
            pk.getAlgorithm())) {
            	curPK = pk;
 //           	System.out.println("Public Key is SOAP: " + getHexString(pk.getEncoded()));
//            	System.out.println("Public Key is SOAP: " +getHexString(LoadPublicKey("", "RSA").getEncoded()));
          return new SimpleKeySelectorResult(pk);
        }
      }
    }
    throw new KeySelectorException("No KeyValue element found!");
  }

  static boolean algEquals(String algURI, String algName) {
    if (algName.equalsIgnoreCase("DSA") &&
        algURI.equalsIgnoreCase(SignatureMethod.DSA_SHA1)) {
      return true;
    } else if (algName.equalsIgnoreCase("RSA") &&
        algURI.equalsIgnoreCase(SignatureMethod.RSA_SHA1)) {
      return true;
    } else {
      return false;
    }
  }
  	
} 
 class SimpleKeySelectorResult implements KeySelectorResult {
 	 private PublicKey pk;
 	 SimpleKeySelectorResult(PublicKey pk) {
 	 	this.pk = pk;         
 	 }
 	  @Override
 	  public Key getKey() {
 	  	 return this.pk;
 	  }
 } 