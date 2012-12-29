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

import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
Document ::= SEQUENCE{
	format PrintableString,
	document OCTET_STRING
}
 */

public class D_Document extends ASNObj{

	private String format;//Printable
	private String document; //OCT STR
	public String toString() {
		return "D:"+format+":"+document;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(format!=null)enc.addToSequence(new Encoder(format,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(document!=null)enc.addToSequence(new Encoder(document).setASN1Type(Encoder.TAG_OCTET_STRING).setASN1Type(DD.TAG_AC1));
		return enc;
	}

	@Override
	public D_Document decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0)format = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if(dec.getTypeByte()==DD.TAG_AC1)document = dec.getFirstObject(true).getString(DD.TAG_AC1);
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
}
