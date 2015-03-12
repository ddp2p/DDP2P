/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012
		Author: Khalid Alhamed and Marius Silaghi: msilaghi@fit.edu
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
package data;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.Element;


import java.io.File;
import java.util.regex.Pattern;

import ciphersuits.SK;

import config.Application;
import config.DD;

import updates.VersionInfo;
import util.Base64Coder;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import static util.Util._;

public class D_ReleaseQuality extends ASN1.ASNObj{
	private static final String Q_SEP = ".";
	private static final boolean DEBUG = false;
	public String[] _quality; // should we add ref 
	public String[] subQualities; // temporary	 
	public String description;
	
	public static D_ReleaseQuality[] reconstructArrayFromString(String s) throws ASN1DecoderFail {
		if (s==null) return null;
		byte[] data = util.Base64Coder.decode(s);
		Decoder dec = new Decoder(data);
		D_ReleaseQuality result[] = dec.getSequenceOf(getASNType(), new D_ReleaseQuality[0], new D_ReleaseQuality());
		return result;
	}
	public static String encodeArray(D_ReleaseQuality[] a) {
		if(a==null)	return null;
		Encoder enc = Encoder.getEncoder(a);
	//	enc.setASN1Type(getASNType());
		byte[] b = enc.getBytes();
		return new String(util.Base64Coder.encode(b));
	}
	public String toString() {
		return "D_ReleaseQuality [\n"+
				"\n    _quality="+Util.concat(_quality, "  ||| ")+
				"\n    subQualities="+Util.concat(subQualities, "  ||| ")+
				"\n    description="+description+
				"\n]";
	}
	
	public String getQualityName(){
		return Util.concat(_quality, Q_SEP);
	}
	public String[] setQualityName(String __quality){
		return _quality = __quality.split(Pattern.quote(Q_SEP));
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(Encoder.getStringEncoder(_quality, Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(description));
		enc.setASN1Type(getASNType());
		return enc;
	}
	static public byte getASNType() {
		if(DEBUG) System.out.println("DD.TAG_AC24= "+ DD.TAG_AC24);
		return DD.TAG_AC24;
	}
	@Override
	public D_ReleaseQuality decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		_quality = d.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
		description = d.getFirstObject(true).getString();
		return this;
	}
	public ASNObj instance() throws CloneNotSupportedException{return new D_ReleaseQuality();}
	
	public static void main(String[]args){
		if(args.length!=1){
			Application.warning(_("Bad parameters list: need xml_file_name;"), _("RELEASE QUALITY"));
			return;
		}
		File xml = new File(args[0]);
		if(!xml.isFile()){
			Application.warning(_("Bad parameters list: need existing xml_file_name;"), _("RELEASE QUALITY"));
			return;			
		}
		D_ReleaseQuality[] rq = parseXML(xml);
		Encoder enc = Encoder.getEncoder(rq);
		System.out.println(new String(Base64Coder.encode(enc.getBytes()))); 
	}
	public static void removeWhitespaceNodes(Element e) {
	 NodeList children = e.getChildNodes();
	 for (int i = children.getLength() - 1; i >= 0; i--) {
		 Node child = children.item(i);
		 if (child instanceof Text && ((Text) child).getData().trim().length() == 0) {
			 e.removeChild(child);
		 }
		 else if (child instanceof Element) {
			 removeWhitespaceNodes((Element) child);
		 }
	 }
 }
	public static D_ReleaseQuality[] parseXML(File xmlFile) {
		D_ReleaseQuality[] result =null;
		Document doc = null;
		try{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		doc = dBuilder.parse(xmlFile);
		}catch(Exception e){
			
		}
		doc.getDocumentElement().normalize();	
		removeWhitespaceNodes(doc.getDocumentElement());
		NodeList nList = doc.getElementsByTagName("testDef");
		result = new D_ReleaseQuality[nList.getLength()];
		int index=-1;
		for (int i = 0; i < nList.getLength(); i++) {
		   Node nNode = nList.item(i);
		   NodeList nnList = nNode.getChildNodes();		   
		   for (int j = 0; j < nnList.getLength(); j++) {
		   	   Node nnNode = nnList.item(j); 
		   	   if(nnNode.getNodeName().equals("ref")){
		   	   	 // index = Integer.parseInt(nnNode.getTextContent()) - 1;
		   	   	  index = Integer.parseInt(nnNode.getTextContent());
		   	   	  result[index] = new D_ReleaseQuality();
		   	      if(DEBUG)System.out.println("ref : "+nnNode.getTextContent());
		   	   }
		   	   if(nnNode.getNodeName().equals("desc")){
		   	       result[index].description = nnNode.getTextContent();
		   	       if(DEBUG)System.out.println("desc : "+ nnNode.getTextContent());
		   	   }
		   	   if(nnNode.getNodeName().equals("qualityStructure")){
		   	   		NodeList nnnList = nnNode.getChildNodes();
		   			result[index]._quality= new String[nnnList.getLength()];
		   			int a=0;
		   			for (int jj = 0; jj < nnnList.getLength(); jj++) {
		   			   Node nnnNode = nnnList.item(jj);
		   			   if(nnnNode.getNodeName().equals("quality")){
			   			   result[index]._quality[a] = nnnNode.getTextContent();
			   			   if(DEBUG)System.out.println( "quality [ " + a +" ]= " +result[index]._quality[a]);
			   			   a++;
		   				}
		   			}
		   	   }
		   	   
		   }		  
		}

		return result;
	}
}