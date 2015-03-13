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
package net.ddp2p.common.table;

import java.math.BigInteger;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.DD;

public class HashFields extends ASNObj{
	public String label;
	public String label_lang;
	public boolean can_be_provided_later;
	public boolean certificated;
	public boolean required;
	public int entry_size;
	public int partNeigh;
	public String default_val;
	public String default_value_lang;
	public String list_of_values[];
	public String list_of_values_lang[];
	public String tip[];
	public String tip_lang[];
	public BigInteger oid[];
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(label!=null)enc.addToSequence(new Encoder(label));
		if(label_lang!=null)enc.addToSequence(new Encoder(label_lang,false));
		enc.addToSequence(new Encoder(can_be_provided_later));
		enc.addToSequence(new Encoder(certificated));
		enc.addToSequence(new Encoder(required));
		enc.addToSequence(new Encoder(entry_size));
		enc.addToSequence(new Encoder(partNeigh));
		if(default_val!=null)enc.addToSequence(new Encoder(default_val).setASN1Type(DD.TAG_AC0));
		if(default_value_lang!=null)enc.addToSequence(new Encoder(default_value_lang,false).setASN1Type(DD.TAG_AC1));
		if(list_of_values!=null)enc.addToSequence(Encoder.getStringEncoder(list_of_values, Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC2));
		if(list_of_values_lang!=null)enc.addToSequence(Encoder.getStringEncoder(list_of_values_lang, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(tip!=null)enc.addToSequence(Encoder.getStringEncoder(tip, Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC4));
		if(tip_lang!=null)enc.addToSequence(Encoder.getStringEncoder(tip_lang, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC5));
		if(oid!=null)enc.addToSequence(new Encoder(oid).setASN1Type(DD.TAG_AC6));
		return enc;
	}
	public ASNObj instance() throws CloneNotSupportedException{return new HashFields();}
	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		return null;
	}
}
