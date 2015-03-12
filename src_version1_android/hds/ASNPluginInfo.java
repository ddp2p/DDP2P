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

import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.DD;


/**
PluginInfo ::= SEQUENCE {
	  id PrintableString,
	  info UTF8String,
	  url PrintableString
	}
*/
public 
class ASNPluginInfo extends ASNObj{
	private static final byte TAG = DD.TAG_AC11;
	public String gid;
	public String name;
	public String info;
	public String url;
	public ASNPluginInfo(){}
	public ASNPluginInfo(
			String gid,
			String name,
			String info,
			String url){
		this.gid = gid;
		this.name = name;
		this.info = info;
		this.url = url;
	}
	public String toString() {
		return "D_PluginInfo: (name="+name+", info="+Util.trimmed(info)+", url="+url+", GID="+Util.trimmed(gid)+") ";
	}
	public ASNPluginInfo instance() {return new ASNPluginInfo();}
	/**
	 * ASNPluginInfo ::= SEQUENCE [AC11] IMPLICIT {
	 * 	gid PrintableString,
	 *  name UTF8String,
	 *  info UTF8String,
	 *  url PrintableString
	 * }
	 */
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder();
		enc.addToSequence(new Encoder(gid, false));
		enc.addToSequence(new Encoder(name));
		enc.addToSequence(new Encoder(info));
		enc.addToSequence(new Encoder(url, false));
		enc.setASN1Type(TAG);
		return enc;
	}
	@Override
	public ASNPluginInfo decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		gid = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		name = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		info = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		url = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		return this;
	}
	public static byte getASN1Type() {
		return TAG;
	}
}
