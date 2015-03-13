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

package net.ddp2p.common.data;
import static net.ddp2p.common.util.Util.__;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.Util;

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
	public static final String TXT_BODY_FORMAT = "TXT";
	public static final int TEXTAREA = 0;
	
	public static final String PDF_BODY_FORMAT = "PDF";
	public static final int DJNative = 1;
	
	public static final String HTM_BODY_FORMAT = "HTM";
	public static final int RTEDIT = 2;
	
	public static final int PDFVIEW = 3;
	
	public static final String DEFAULT_FORMAT = HTM_BODY_FORMAT;
	public static final int DEFAULT_EDITOR = RTEDIT;
	public String toString() {
		return "D:"+format+":"+document;
	}
	public void decode(String d) {
		if(!d.startsWith(D)) return;
		String[] v = d.split(":");
		format = v[1];
		document=v[2];
	}
	public D_Document() {
		format = D_Document.DEFAULT_FORMAT;
		document = ""; // "<html></html>";
	}
	public D_Document(String d) {
		format = D_Document.TXT_FORMAT;
		document = d; // "<html></html>";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(format!=null)enc.addToSequence(new Encoder(format,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(document!=null){
			if(format==D_Document.PDF_BODY_FORMAT){
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
			if(format==D_Document.PDF_BODY_FORMAT)
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
	/**
	 * Returns the document body without format type (assumed UTF8)
	 * @return
	 */
	public String getDocumentUTFString() {
		return document;
	}
	public String convertTo(String newFormat) {
		switch(format){
		case D_Document.TXT_BODY_FORMAT:
			switch(newFormat){
			case D_Document.HTM_BODY_FORMAT:
				document = "<html>"+document+"</html>";
				format = newFormat;
				break;
			case D_Document.TXT_BODY_FORMAT: break;
			}
			break;
		case D_Document.HTM_BODY_FORMAT:
			switch(newFormat){
			case D_Document.HTM_BODY_FORMAT: break;
			case D_Document.TXT_BODY_FORMAT:
				if(0 != Application_GUI.ask(__("You may lose data by switching to the new format!"),
						__("Losing Data"), Application_GUI.OK_CANCEL_OPTION)){
				    System.out.println("txt document conversion abandoned");
					return null; //document;
				}
				
			//	document = "<html>"+document+"</html>";
			    document = Application_GUI.html2text(document);
			    
			    System.out.println("txt document= "+document);
				format = newFormat;
				break;
			}
			break;
		case D_Document.PDF_BODY_FORMAT: break;
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
			setFormatString(D_Document.HTM_BODY_FORMAT);
		}
	}
	/**
	 * Copy the data from doc.
	 * @param doc
	 */
	public void copy(D_Document doc) {
		this.format = doc.format;
		this.document = doc.document;
	}
}

