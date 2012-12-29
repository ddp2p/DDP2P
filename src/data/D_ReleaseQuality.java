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

import java.util.regex.Pattern;

import config.DD;

import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class D_ReleaseQuality extends ASN1.ASNObj{
	private static final String Q_SEP = ".";
	private static final boolean DEBUG = false;
	public String[] _quality;
	public String[] subQualities; // temporary	 
	public String description;
	
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
}