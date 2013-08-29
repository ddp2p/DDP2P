/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
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
import static util.Util._;

import javax.swing.JOptionPane;

import util.Util;
import widgets.components.DocumentEditor;
import config.Application;
import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;



//Html2Text class imports
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.io.IOException;


/**
Document ::= SEQUENCE{
	format PrintableString,
	document OCTET_STRING
}
 */

public class D_Document extends ASNObj{

	public static final String TXT_FORMAT = "TXT";
	private static final String D = "D:";
	private String format;//Printable
	private String document; //OCT STR
	public String toString() {
		return "D:"+format+":"+document;
	}
	public void decode(String d) {
		if(!d.startsWith(D)) return;
		String[] v = d.split(":");
		format = v[1];
		document=v[2];
	}
	public D_Document(){
		format = DocumentEditor.DEFAULT_FORMAT;
		document = ""; // "<html></html>";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(format!=null)enc.addToSequence(new Encoder(format,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(document!=null){
			if(format==DocumentEditor.PDF_BODY_FORMAT){
				enc.addToSequence(new Encoder(Util.byteSignatureFromString(document)).setASN1Type(Encoder.TAG_OCTET_STRING).setASN1Type(DD.TAG_AC1));
			}else{
				enc.addToSequence(new Encoder(document).setASN1Type(Encoder.TAG_OCTET_STRING).setASN1Type(DD.TAG_AC1));
			}
		}
		return enc;
	}

	@Override
	public D_Document decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0)format = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if(dec.getTypeByte()==DD.TAG_AC1){
			if(format==DocumentEditor.PDF_BODY_FORMAT)
				document = Util.stringSignatureFromByte(dec.getFirstObject(true).getBytes(DD.TAG_AC1));
			else
				//document = util.Util.readAll(new java.util.zip.GZIPInputStream(new StringBufferInputStream(dec.getFirstObject(true).getString(DD.TAG_AC1))));
				document = dec.getFirstObject(true).getString(DD.TAG_AC1);
		}
		return this;
	}

	/**
	 * representation that can be stored in database
	 * @return
	 */
	public String getFormatString() {
		return format;
	}
	/**
	 * representation that can be stored in database
	 * @return
	 */
	public String getDocumentString() {
		return document;
	}
	/**
	 * representation that can be read from database
	 * @param string
	 */
	public void setFormatString(String string) {
		format = string;
	}
	/**
	 * representation that can be read from database
	 * @param string
	 */
	public void setDocumentString(String string) {
		document = string;
	}
	public String getDocumentUTFString() {
		return document;
	}
	public String convertTo(String newFormat) {
		switch(format){
		case DocumentEditor.TXT_BODY_FORMAT:
			switch(newFormat){
			case DocumentEditor.HTM_BODY_FORMAT:
				document = "<html>"+document+"</html>";
				format = newFormat;
				break;
			case DocumentEditor.TXT_BODY_FORMAT: break;
			}
			break;
		case DocumentEditor.HTM_BODY_FORMAT:
			switch(newFormat){
			case DocumentEditor.HTM_BODY_FORMAT: break;
			case DocumentEditor.TXT_BODY_FORMAT:
				if(0 != Application.ask(_("You may lose data by switching to the new format!"),
						_("Losing Data"), JOptionPane.OK_CANCEL_OPTION)){
				    System.out.println("txt document conversion abandoned");
					return null;
				}
				
			//	document = "<html>"+document+"</html>";
			    Html2Text parser = new Html2Text();
			    try{
			    	parser.parse(new StringReader(document));
			    }catch(IOException e){
			    	System.err.println(e);
			    }
			    document = parser.getText();
			    System.out.println("txt document= "+document);
				format = newFormat;
				break;
			}
			break;
		case DocumentEditor.PDF_BODY_FORMAT: break;
		}
		return document;
	}
	public String getDBDoc() {
		return Util.stringSignatureFromByte(this.getEncoder().getBytes());
	}
	public void setDBDoc(String string) {
		try {
			decode(new Decoder(Util.byteSignatureFromString(string, false)));
		} catch (Exception e) {
			setDocumentString(string);
			setFormatString(DocumentEditor.HTM_BODY_FORMAT);
		}
	}
}


class Html2Text extends HTMLEditorKit.ParserCallback {
 StringBuffer s;

 public Html2Text() {}

 public void parse(Reader in) throws IOException {
   s = new StringBuffer();
   ParserDelegator delegator = new ParserDelegator();
   // the third parameter is TRUE to ignore charset directive
   delegator.parse(in, this, Boolean.TRUE);
 }

 public void handleText(char[] text, int pos) {
 //System.out.println("pos: "+pos);
 s.append(" ");
   s.append(text);
 }

 public String getText() {
   return s.toString();
 }
}