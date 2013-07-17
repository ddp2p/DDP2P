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

import java.util.ArrayList;

import streaming.NeighborhoodHandling;
import util.Util;

import util.P2PDDSQLException;

import config.Application;
import config.DD;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/*
 D_FieldValue ::= SEQUENCE {
 field_extra_ID PrintableString,
 value UTF8String,
 fieldID_above PrintableString,
 field_default_next PrintableString,
 value_lang PrintableString,
 neighborhood [0] SEQUENCE OF D_Neighborhood OPTIONAL
}
 */

public class D_FieldValue extends ASNObj{
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public String field_extra_GID;//Printable
	public String value;//OCT STR
	public String oid;//OCT STR
	public String field_GID_above;//Printable
	public String field_GID_default_next;//Printable
	public String value_lang;//Printable
	public String global_neighborhood_ID;
	
	// not sent
	public long field_extra_ID =-1;
	public long field_ID_above = -1;
	public long field_ID_default_next = -1;
	public long neighborhood_ID =-1;
	
	public String toString() {
		String result =
			"D_FieldValue ["+
			"\n  field_extra_ID="+field_extra_GID+
			"\n  value="+value+
			"\n  oid="+oid+
			"\n  fieldID_above="+field_GID_above+
			"\n  field_default_next="+field_GID_default_next+
			"\n  value_lang="+value_lang+
			"\n  global_neighborhood_ID="+global_neighborhood_ID+
			"\n ]";
		return result;
	}
	
	public D_Neighborhood[] neighborhood;
	public D_FieldValue instance() throws CloneNotSupportedException{return new D_FieldValue();}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(field_extra_GID,Encoder.TAG_PrintableString));
		enc.addToSequence(new Encoder(value,Encoder.TAG_OCTET_STRING));
		enc.addToSequence(new Encoder(field_GID_above,Encoder.TAG_PrintableString));
		enc.addToSequence(new Encoder(field_GID_default_next,Encoder.TAG_PrintableString));
		enc.addToSequence(new Encoder(value_lang,Encoder.TAG_PrintableString));
		if(neighborhood!=null) enc.addToSequence(Encoder.getEncoder(neighborhood));
		return enc;
	}

	@Override
	public D_FieldValue decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		field_extra_GID = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		value = dec.getFirstObject(true).getString(Encoder.TAG_OCTET_STRING);
		field_GID_above = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		field_GID_default_next = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		value_lang = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC0) neighborhood = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE,new D_Neighborhood[]{},new D_Neighborhood());
		return this;
	}
	public static byte getASN1Type() {
		return Encoder.TAG_SEQUENCE;
	}
	public final static String v_fields_values = Util.setDatabaseAlias(table.field_value.fields_list,"v");
	static final String sql_get_const_fields = "SELECT "+v_fields_values+ 
	",e."+table.field_extra.oid+
	",e."+table.field_extra.global_field_extra_ID+
	",a."+table.field_extra.global_field_extra_ID+
	",d."+table.field_extra.global_field_extra_ID+
	",n."+table.neighborhood.global_neighborhood_ID+
	" FROM "+table.field_value.TNAME+" AS v " +
	" LEFT JOIN "+table.field_extra.TNAME+" AS e ON (e."+table.field_extra.field_extra_ID+" = v."+table.field_value.field_extra_ID+")" +
	" LEFT JOIN "+table.field_extra.TNAME+" AS a ON (a."+table.field_extra.field_extra_ID+" = v."+table.field_value.fieldID_above+")" +
	" LEFT JOIN "+table.field_extra.TNAME+" AS d ON (d."+table.field_extra.field_extra_ID+" = v."+table.field_value.field_default_next+")" +
	" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON (n."+table.neighborhood.global_neighborhood_ID+" = v."+table.field_value.neighborhood_ID+")" +
	" WHERE v."+table.field_value.constituent_ID+" = ?"+
		" ORDER BY e."+table.field_extra.global_field_extra_ID+";";
	
	static public D_FieldValue[] getFieldValues(String constituentID) throws P2PDDSQLException{
		D_FieldValue[] result = null;
		ArrayList<ArrayList<Object>> all = Application.db.select(sql_get_const_fields, new String[]{constituentID}, DEBUG);
		int asz = all.size();
		if(asz==0) return null;
		result = new D_FieldValue[asz];
		for(int a=0; a<asz; a++) {
			//c.postalAddress = new ASNLocationItem[asz];
			D_FieldValue v = new D_FieldValue();
			v.value = Util.getString(all.get(a).get(table.field_value.VAL_COL_VALUE));
			v.value_lang = Util.getString(all.get(a).get(table.field_value.VAL_COL_LANG));
			v.oid = Util.getString(all.get(a).get(table.field_value.VAL_COLs+0));
			v.field_extra_GID = Util.getString(all.get(a).get(table.field_value.VAL_COLs+1));
			v.field_GID_above = Util.getString(all.get(a).get(table.field_value.VAL_COLs+2));
			v.field_GID_default_next = Util.getString(all.get(a).get(table.field_value.VAL_COLs+3));
			v.global_neighborhood_ID = Util.getString(all.get(a).get(table.field_value.VAL_COLs+4));
			result[a] = v;

			v.field_extra_ID = Util.lval(all.get(a).get(table.field_value.VAL_COL_FIELD_EXTRA_ID),-1);
			v.field_ID_above = Util.lval(all.get(a).get(table.field_value.VAL_COL_FIELD_ID_ABOVE),-1);
			v.field_ID_default_next = Util.lval(all.get(a).get(table.field_value.VAL_COL_FIELD_DEFAULT_NEXT),-1);
			v.neighborhood_ID = Util.lval(all.get(a).get(table.field_value.VAL_COL_NEIGH_ID),-1);

			
			//c.postalAddress[a].hierarchy = "";//fieldID_above||field_default_next||neighborhood
			//c.postalAddress[a].oid = new int[0];
			if(DEBUG) System.out.println("D_FieldValue:getFieldValues: New Constituent address = "+result[a]);		
		}
		return result;
	}
	static D_FieldValue[] getFieldValues(long c_ID) throws P2PDDSQLException {
		return getFieldValues(Util.getStringID(c_ID));
	}
	public static void store(D_FieldValue[] address, String constituent_ID, long org_ID, boolean accept_new_fields) throws P2PDDSQLException, ExtraFieldException {
		if(address == null) return;
		Application.db.delete(table.field_value.TNAME, new String[]{table.field_value.constituent_ID}, new String[]{constituent_ID}, DEBUG);
		for(int k=0; k< address.length; k++)
			store(address[k], constituent_ID, org_ID, accept_new_fields);
	}
	private static long store(D_FieldValue wf, String constituent_ID, long org_ID, boolean accept_new_fields) throws P2PDDSQLException, ExtraFieldException {
		if(DEBUG) System.out.println("D_FieldValue:store: start wf="+wf+ " constID="+constituent_ID);
		String[] fields = table.field_value.fields_list.split(",");
		String[] params = new String[fields.length];
		params[table.field_value.VAL_COL_VALUE] = wf.value;
		params[table.field_value.VAL_COL_CONSTITUENT_ID] = constituent_ID;
		params[table.field_value.VAL_COL_FIELD_DEFAULT_NEXT] = D_FieldExtra.getFieldExtraID(wf.field_GID_default_next, org_ID, accept_new_fields);
		params[table.field_value.VAL_COL_FIELD_EXTRA_ID] = D_FieldExtra.getFieldExtraID(wf.field_extra_GID, org_ID, accept_new_fields);
		params[table.field_value.VAL_COL_FIELD_ID_ABOVE] = D_FieldExtra.getFieldExtraID(wf.field_GID_above, org_ID, accept_new_fields);
		params[table.field_value.VAL_COL_LANG] = wf.value_lang;
		params[table.field_value.VAL_COL_NEIGH_ID] = D_Neighborhood.getNeighborhoodLocalID(wf.global_neighborhood_ID);
		if(params[table.field_value.VAL_COL_NEIGH_ID]==null) // table contraint refuses null;
			params[table.field_value.VAL_COL_NEIGH_ID] = table.field_extra.NEIGHBORHOOD_ID_NA;
		long id=Application.db.insert(table.field_value.TNAME, fields, params, DEBUG);
		if(DEBUG) System.out.println("D_FieldValue:store: end result="+id);
		return id;
	}
}

