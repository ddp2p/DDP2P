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

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class OrgsData_Hash extends ASNObj{
	String orgGIDhash;  // the org GIDhash
	byte[] hash_all_data; // the hash of all GIDs, sorted by creation date
	Calendar up_to;  // starting creation_date
	Calendar from;   // end creation_date
	
	static byte TAG = Encoder.TAG_SEQUENCE;
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(orgGIDhash, false));
		enc.addToSequence(new Encoder(hash_all_data));
		enc.addToSequence(new Encoder(up_to));
		enc.addToSequence(new Encoder(from));
		return enc;
	}
	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		orgGIDhash = d.getFirstObject(true).getString();
		hash_all_data = d.getFirstObject(true).getBytes();
		up_to = d.getFirstObject(true).getGeneralizedTimeCalender_();
		from = d.getFirstObject(true).getGeneralizedTimeCalender_();
		return this;
	}
	public OrgsData_Hash instance(){
		return new OrgsData_Hash();
	}
	public static byte getASN1Type() {
		return TAG;
	}
}