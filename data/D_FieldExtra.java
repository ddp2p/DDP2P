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

import java.math.BigInteger;
import java.util.ArrayList;

import util.Util;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
 WB_FieldExtra :: SEQUENCE {
 can_be_provided_later BOOLEAN,
 certificated BOOLEAN,
 default_val UTF8String,
 entry_size INTEGER,
 label UTF8String,
 list_of_values SEQUENCE OF UTF8String,
 partNeigh INTEGER,
 global_field_extra_ID PrintableString,
 required BOOLEAN,
 tip UTF8String,
 tip_lang PrintableString,
 label_lang PrintableString,
 list_of_values_lang SEQUENCE OF PrintableString,
 default_value_lang PrintableString,
 oid OID
}
 */

public class D_FieldExtra extends ASNObj{

	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public Boolean can_be_provided_later;
	public Boolean certificated;
	public String default_val;//UTF8
	public Integer entry_size;
	public String label;//UTF8
	public String list_of_values[];//UTF8
	public Integer partNeigh;
	public String global_field_extra_ID;//Printable
	public Boolean required;
	public String tip;//UTF8
	public String tip_lang;//Printable
	public String label_lang;//Printable
	public String list_of_values_lang[];//Printable
	public String default_value_lang;//Printable
	public BigInteger oid[];
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		//System.out.println("WB_ASN_FieldExtra : ");
		if(can_be_provided_later!=null) enc.addToSequence(new Encoder(can_be_provided_later).setASN1Type(Encoder.TAG_BOOLEAN).setASN1Type(DD.TAG_AC0));
		if(certificated!=null) enc.addToSequence(new Encoder(certificated).setASN1Type(Encoder.TAG_BOOLEAN).setASN1Type(DD.TAG_AC1));
		if(default_val!=null) enc.addToSequence(new Encoder(default_val,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC2));
		if(entry_size!=null) enc.addToSequence(new Encoder(entry_size).setASN1Type(Encoder.TAG_INTEGER).setASN1Type(DD.TAG_AC3));
		if(label!=null) enc.addToSequence(new Encoder(label,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC4));
		if(list_of_values!=null) enc.addToSequence(Encoder.getStringEncoder(list_of_values, Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC5));
		if(partNeigh!=null) enc.addToSequence(new Encoder(partNeigh).setASN1Type(Encoder.TAG_INTEGER).setASN1Type(DD.TAG_AC6));
		if(global_field_extra_ID!=null) enc.addToSequence(new Encoder(global_field_extra_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC7));
		if(required!=null) enc.addToSequence(new Encoder(required).setASN1Type(Encoder.TAG_BOOLEAN).setASN1Type(DD.TAG_AC8));
		if(tip!=null) enc.addToSequence(new Encoder(tip,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC9));
		if(tip_lang!=null) enc.addToSequence(new Encoder(tip_lang,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC10));
		if(label_lang!=null) enc.addToSequence(new Encoder(label_lang,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC11));
		if(list_of_values_lang!=null) enc.addToSequence(Encoder.getStringEncoder(list_of_values_lang, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		if(default_value_lang!=null) enc.addToSequence(new Encoder(default_value_lang,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC13));
		if(oid!=null) enc.addToSequence(new Encoder(oid).setASN1Type(Encoder.TAG_OID).setASN1Type(DD.TAG_AC14));
		
		return enc;
	}

	@Override
	public D_FieldExtra decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0)can_be_provided_later = dec.getFirstObject(true).getBoolean();
		if(dec.getTypeByte()==DD.TAG_AC1)certificated = dec.getFirstObject(true).getBoolean();
		if(dec.getTypeByte()==DD.TAG_AC2)default_val = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		if(dec.getTypeByte()==DD.TAG_AC3)entry_size = dec.getFirstObject(true).getInteger().intValue();
		if(dec.getTypeByte()==DD.TAG_AC4)label = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		if(dec.getTypeByte()==DD.TAG_AC5)list_of_values =dec.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
		if(dec.getTypeByte()==DD.TAG_AC6)partNeigh = dec.getFirstObject(true).getInteger().intValue();
		if(dec.getTypeByte()==DD.TAG_AC7)global_field_extra_ID = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC8)required = dec.getFirstObject(true).getBoolean();
		if(dec.getTypeByte()==DD.TAG_AC9)tip = dec.getFirstObject(true).getString(Encoder.TAG_UTF8String);
		if(dec.getTypeByte()==DD.TAG_AC10)tip_lang = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC11)label_lang = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC12)list_of_values_lang =dec.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC13)default_value_lang =dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC14)oid = dec.getFirstObject(true).getBNOID(Encoder.TAG_OID);
		return this;
	}
	public static String getFieldExtraID(String field, long org_ID) throws SQLiteException {
		if (field==null) return null;
		field = field.trim();
		if("".equals(field)) return null;
		
		String sql = "SELECT "+table.field_extra.field_extra_ID+
		" FROM "+table.field_extra.TNAME+
		" WHERE "+table.field_extra.global_field_extra_ID+"=? AND "+table.field_extra.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.db.select(sql, new String[]{field,Util.getStringID(org_ID)}, DEBUG);
		if(n.size()!=0) return Util.getString(n.get(0).get(0));
		return null;
	}
	public static String getFieldExtraID(String fieldGID, long org_ID, boolean insertTmp) throws SQLiteException, ExtraFieldException {
		if(fieldGID==null) return null;
		String id = getFieldExtraID(fieldGID, org_ID);
		if(id!=null) return id;
		if(!insertTmp) return id;
		if(insertTmp)
			return Util.getStringID(insertTmpFieldExtra(fieldGID, org_ID));
		throw new ExtraFieldException("Unknown field type: \""+fieldGID+"\" for org \""+org_ID+"\"");
	}

	public static long insertTmpFieldExtra(String fieldGID, long org_ID) throws SQLiteException {
		if(_DEBUG) System.out.println("D_FieldExtra: insertTmpFieldExtra: start");
		if(fieldGID==null) Util.printCallPath("Null field");
		return Application.db.insert(table.field_extra.TNAME,
				new String[]{table.field_extra.global_field_extra_ID, table.field_extra.organization_ID, table.field_extra.tmp},
				new String[]{fieldGID, Util.getStringID(org_ID), "1"}, _DEBUG );
	}
}
