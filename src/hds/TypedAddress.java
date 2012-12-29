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

package hds;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class TypedAddress extends ASNObj{
	public String address; //utf8
	public String type; //PrintableString
	public TypedAddress(){}
	public TypedAddress instance(){return new TypedAddress();}
	public String toString(){
		return "\n\t TypedAddress: [address="+address+" type="+type+"]";
	}
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(address).setASN1Type(Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(type).setASN1Type(Encoder.TAG_PrintableString));
		return enc;
	}
	public TypedAddress decode(Decoder dec){
		Decoder content;
		try {
			content = dec.getContent();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return this;
		}
		address = content.getFirstObject(true).getString();
		type = content.getFirstObject(true).getString();
		return this;
	}
}
