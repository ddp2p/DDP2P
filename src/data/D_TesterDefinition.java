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

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;

public class D_TesterDefinition extends ASNObj{
	private static final boolean DEBUG = false;
	public String name;
	public String public_key;
	public String email;
	public String url;
	public String description;
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(name));
		enc.addToSequence(new Encoder(email));
		enc.addToSequence(new Encoder(url));
		enc.addToSequence(new Encoder(description));
		enc.addToSequence(new Encoder(public_key, false));
		enc.setASN1Type(getASNType());
		return enc;
	}
	@Override
	public D_TesterDefinition decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		name = d.getFirstObject(true).getString();
		email = d.getFirstObject(true).getString();
		url = d.getFirstObject(true).getString();
		description = d.getFirstObject(true).getString();
		public_key = d.getFirstObject(true).getString();
		return this;
	}
	public ASNObj instance() throws CloneNotSupportedException{return new D_TesterInfo();}
	public static byte getASNType() {
		if(DEBUG) System.out.println("DD.TAG_AC23= "+ DD.TAG_AC23);
		return DD.TAG_AC23;
	}
}