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

import java.util.Calendar;

import util.Summary;
import util.Util;

import config.DD;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class ResetOrgInfo extends ASNObj implements Summary {
	public String hash;
	public Calendar reset_date;
	public String toString(){
		return "\n\t[ResetOrgInfo:date="+Encoder.getGeneralizedTime(reset_date)+",hash="+hash+"]";
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(hash, false));
		enc.addToSequence(new Encoder(reset_date));
		return enc.setASN1Type(getASN1Type());
	}
	@Override
	public String toSummaryString() {
		return "[Rst:date="+Encoder.getGeneralizedTime(reset_date)+",#="+Util.trimmed(hash,6)+"]";
	}

	@Override
	public ResetOrgInfo decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		hash = d.getFirstObject(true).getString();
		reset_date = d.getGeneralizedTimeCalender_();
		return this;
	}
	
	public ResetOrgInfo instance() throws CloneNotSupportedException{
		return new ResetOrgInfo();
	}

	public static byte getASN1Type() {
		return DD.TAG_AC24;
	}

}
