/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014
		Authors: Khalid Alhamed, Marius Silaghi
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

import java.util.ArrayList;

import util.Util;
import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class D_TesterRatingByPeer extends ASNObj  {
	public String testerGID;//public_key_hash;
	public String weight;  // encoded in ASN1 as a String
	public float _weight;  // encoded in ASN1 as a String
	public String address; //address where the tester can be contacted
	public ArrayList<D_TesterCreationHistory> testerCreationHistory = new ArrayList<D_TesterCreationHistory>();
	@Override
	public D_TesterRatingByPeer instance() {
		return new D_TesterRatingByPeer();
	}
	
	public String toString() {
		return this.toTXT();
	}
	public String toTXT() {
		String result ="";
		result += this.testerGID+"\r\n";
		result += this.weight+"\r\n";
		result += this.address+"\r\n";
		result += "["+Util.concat(testerCreationHistory, "|", null) +"]\r\n";
		return result;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(testerGID));
		enc.addToSequence(new Encoder(weight));
		enc.addToSequence(new Encoder(address));
		//enc.addToSequence(new Encoder(testerCreationHistory));
		enc.setASN1Type(getASNType());
		return enc;
	}
	
	static byte getASNType() { 
		return DD.TAG_AC17;
	}
		@Override
	public D_TesterRatingByPeer decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		testerGID = d.getFirstObject(true).getString();
		_weight = Util.fval(weight = d.getFirstObject(true).getString(), 0.0f);
		address = d.getFirstObject(true).getString();
		//testerCreationHistory = d.getFirstObject(true).getString();
		return this;
	}
	/** No longer used since no longer implementing comparator*/
		public int compareTo(D_TesterRatingByPeer testerScore) {
			if (this._weight == testerScore._weight)
	            return 0;
	        else if (this._weight > testerScore._weight)
	            return 1;
	        else
	            return -1;
		}
}