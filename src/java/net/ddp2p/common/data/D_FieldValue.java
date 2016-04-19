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
package net.ddp2p.common.data;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.streaming.NeighborhoodHandling;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class D_FieldValue extends ASNObj{
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public String field_extra_GID;
	public String value;
	public String oid;
	public String field_GID_above;
	public String field_GID_default_next;
	public String value_lang;
	public String global_neighborhood_ID;
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
	public D_OrgParam field_extra; 
	public D_FieldValue instance() throws CloneNotSupportedException{return new D_FieldValue();}
	@Override
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	/**
	 * parameter used for dictionaries
	 */
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) { 
		Encoder enc = new Encoder().initSequence();
		String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, field_extra_GID);
		enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString));
		enc.addToSequence(new Encoder(value,Encoder.TAG_OCTET_STRING));
		repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, field_GID_above);
		enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString));
		repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, field_GID_default_next);
		enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString));
		enc.addToSequence(new Encoder(value_lang,Encoder.TAG_PrintableString));
		if(neighborhood!=null) enc.addToSequence(Encoder.getEncoder(neighborhood));
		if(global_neighborhood_ID!=null) {
			repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_neighborhood_ID);
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AP1));
		}
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
		if(dec.getTypeByte()==DD.TAG_AC0) neighborhood = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE,new D_Neighborhood[]{}, D_Neighborhood.getEmpty());
		if(dec.getTypeByte()==DD.TAG_AP1) global_neighborhood_ID = dec.getFirstObject(true).getString();
		return this;
	}
	public static byte getASN1Type() {
		return Encoder.TAG_SEQUENCE;
	}
	public final static String v_fields_values = Util.setDatabaseAlias(net.ddp2p.common.table.field_value.fields_list,"v");
	static final String sql_get_const_fields = "SELECT "+v_fields_values+ 
	",e."+net.ddp2p.common.table.field_extra.oid+
	",e."+net.ddp2p.common.table.field_extra.global_field_extra_ID+
	",a."+net.ddp2p.common.table.field_extra.global_field_extra_ID+
	",d."+net.ddp2p.common.table.field_extra.global_field_extra_ID+
	",n."+net.ddp2p.common.table.neighborhood.global_neighborhood_ID+
	" FROM "+net.ddp2p.common.table.field_value.TNAME+" AS v " +
	" LEFT JOIN "+net.ddp2p.common.table.field_extra.TNAME+" AS e ON (e."+net.ddp2p.common.table.field_extra.field_extra_ID+" = v."+net.ddp2p.common.table.field_value.field_extra_ID+")" +
	" LEFT JOIN "+net.ddp2p.common.table.field_extra.TNAME+" AS a ON (a."+net.ddp2p.common.table.field_extra.field_extra_ID+" = v."+net.ddp2p.common.table.field_value.fieldID_above+")" +
	" LEFT JOIN "+net.ddp2p.common.table.field_extra.TNAME+" AS d ON (d."+net.ddp2p.common.table.field_extra.field_extra_ID+" = v."+net.ddp2p.common.table.field_value.field_default_next+")" +
	" LEFT JOIN "+net.ddp2p.common.table.neighborhood.TNAME+" AS n ON (n."+net.ddp2p.common.table.neighborhood.global_neighborhood_ID+" = v."+net.ddp2p.common.table.field_value.neighborhood_ID+")" +
	" WHERE v."+net.ddp2p.common.table.field_value.constituent_ID+" = ?"+
		" ORDER BY e."+net.ddp2p.common.table.field_extra.global_field_extra_ID+";";
	static public D_FieldValue[] getFieldValues(String constituentID) throws P2PDDSQLException{
		D_FieldValue[] result = null;
		ArrayList<ArrayList<Object>> all = Application.getDB().select(sql_get_const_fields, new String[]{constituentID}, DEBUG);
		int asz = all.size();
		if(asz==0) return null;
		result = new D_FieldValue[asz];
		for(int a=0; a<asz; a++) {
			D_FieldValue v = new D_FieldValue();
			v.value = Util.getString(all.get(a).get(net.ddp2p.common.table.field_value.VAL_COL_VALUE));
			v.value_lang = Util.getString(all.get(a).get(net.ddp2p.common.table.field_value.VAL_COL_LANG));
			v.oid = Util.getString(all.get(a).get(net.ddp2p.common.table.field_value.VAL_COLs+0));
			v.field_extra_GID = Util.getString(all.get(a).get(net.ddp2p.common.table.field_value.VAL_COLs+1));
			v.field_GID_above = Util.getString(all.get(a).get(net.ddp2p.common.table.field_value.VAL_COLs+2));
			v.field_GID_default_next = Util.getString(all.get(a).get(net.ddp2p.common.table.field_value.VAL_COLs+3));
			v.global_neighborhood_ID = Util.getString(all.get(a).get(net.ddp2p.common.table.field_value.VAL_COLs+4));
			result[a] = v;
			v.field_extra_ID = Util.lval(all.get(a).get(net.ddp2p.common.table.field_value.VAL_COL_FIELD_EXTRA_ID),-1);
			v.field_ID_above = Util.lval(all.get(a).get(net.ddp2p.common.table.field_value.VAL_COL_FIELD_ID_ABOVE),-1);
			v.field_ID_default_next = Util.lval(all.get(a).get(net.ddp2p.common.table.field_value.VAL_COL_FIELD_DEFAULT_NEXT),-1);
			v.neighborhood_ID = Util.lval(all.get(a).get(net.ddp2p.common.table.field_value.VAL_COL_NEIGH_ID),-1);
			if(DEBUG) System.out.println("D_FieldValue:getFieldValues: New Constituent address = "+result[a]);		
		}
		Arrays.sort(result, new FVComparator()); 
		return result;
	}
	static D_FieldValue[] getFieldValues(long c_ID) throws P2PDDSQLException {
		return getFieldValues(Util.getStringID(c_ID));
	}
	public static void store(boolean sync, D_FieldValue[] address, String constituent_ID, 
			long org_ID, boolean accept_new_fields, D_Organization org) throws P2PDDSQLException, ExtraFieldException {
		if (address == null) return;
		Application.getDB().delete(sync, net.ddp2p.common.table.field_value.TNAME, new String[]{net.ddp2p.common.table.field_value.constituent_ID}, new String[]{constituent_ID}, DEBUG);
		for (int k = 0; k < address.length; k ++)
			store(sync, address[k], constituent_ID, org_ID, accept_new_fields, org);
	}
	private static long store(boolean sync, D_FieldValue wf, String constituent_ID,
			long org_ID, boolean accept_new_fields, D_Organization org) throws P2PDDSQLException, ExtraFieldException {
		if(DEBUG) System.out.println("D_FieldValue:store: start wf="+wf+ " constID="+constituent_ID);
		String[] fields = net.ddp2p.common.table.field_value.fields_list.split(",");
		String[] params = new String[fields.length];
		params[net.ddp2p.common.table.field_value.VAL_COL_VALUE] = wf.value;
		params[net.ddp2p.common.table.field_value.VAL_COL_CONSTITUENT_ID] = constituent_ID;
		String def_next_LID = org.getFieldExtraID(wf.field_GID_default_next, accept_new_fields);
		if (def_next_LID == null) def_next_LID = Util.getStringID(wf.field_ID_default_next);
		params[net.ddp2p.common.table.field_value.VAL_COL_FIELD_DEFAULT_NEXT] = def_next_LID;
		String extra_LID = org.getFieldExtraID(wf.field_extra_GID, accept_new_fields);
		if (extra_LID == null) extra_LID = Util.getStringID(wf.field_extra_ID);
		params[net.ddp2p.common.table.field_value.VAL_COL_FIELD_EXTRA_ID] = extra_LID;
		String def_abv_LID = org.getFieldExtraID(wf.field_GID_default_next, accept_new_fields);
		if (def_abv_LID == null) def_abv_LID = Util.getStringID(wf.field_ID_default_next);
		params[net.ddp2p.common.table.field_value.VAL_COL_FIELD_ID_ABOVE] = def_abv_LID;
		params[net.ddp2p.common.table.field_value.VAL_COL_LANG] = wf.value_lang;
		params[net.ddp2p.common.table.field_value.VAL_COL_NEIGH_ID] = D_Neighborhood.getLIDstrFromGID(wf.global_neighborhood_ID, org_ID);
		if(params[net.ddp2p.common.table.field_value.VAL_COL_NEIGH_ID]==null) 
			params[net.ddp2p.common.table.field_value.VAL_COL_NEIGH_ID] = net.ddp2p.common.table.field_extra.NEIGHBORHOOD_ID_NA;
		long id=Application.getDB().insert(net.ddp2p.common.table.field_value.TNAME, fields, params, DEBUG);
		if(DEBUG) System.out.println("D_FieldValue:store: end result="+id);
		return id;
	}
	/**
	 * Compares the values of extra fields.
	 * Sorts a2 (assuming that is is not yet sorted).
	 * Does not sort a1 which is assumed sorted (? have to check that this is true)
	 * based on FVComparator
	 * @param a1 (local)
	 * @param a2 (remote)
	 * @return
	 */
	public static boolean different(D_FieldValue[] a1,
			D_FieldValue[] a2) {
		if ( (a1 == null) && (a2 == null) ) return false;
		if ( (a1 == null) || (a2 == null) ) return true;
		if (a1.length != a2.length) return true;
		Arrays.sort(a2, new FVComparator()); 
		for (int k = 0; k < a1.length; k ++) {
			if (different(a1[k], a2[k])) return true;
		}
		return false;
	}
	static class FVComparator implements Comparator<D_FieldValue> {
			@Override
			public int compare(D_FieldValue o1, D_FieldValue o2) {
				if (o1.field_extra_GID == o2.field_extra_GID) return 0;
				if (o1.field_extra_GID == null && o2.field_extra_GID != null) return -1;
				if (o1.field_extra_GID != null && o2.field_extra_GID == null) return 1;
				return o1.field_extra_GID.compareTo(o2.field_extra_GID);
			}
	 }
	public static boolean different(D_FieldValue f1,
			D_FieldValue f2) {
		if (f1 == null && f2 == null) return false;
		if (f1 == null || f2 == null) return true;
		if (! Util.equalStrings_null_or_not(f1.field_extra_GID, f2.field_extra_GID)) return true;
		if (! Util.equalStrings_null_or_not(f1.value, f2.value)) return true;
		if (! Util.equalStrings_null_or_not(f1.value_lang, f2.value_lang)) return true;
		if (! Util.equalStrings_null_or_not(f1.field_GID_above, f2.field_GID_above)) return true;
		if (! Util.equalStrings_null_or_not(f1.field_GID_default_next, f2.field_GID_default_next)) return true;
		if (! Util.equalStrings_null_or_not(f1.global_neighborhood_ID, f2.global_neighborhood_ID)) return true;
		return false;
	}
}
