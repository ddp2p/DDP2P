package data;
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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.regex.Pattern;

import table.field_extra;
import util.P2PDDSQLException;
import hds.ASNSyncPayload;
import hds.ASNSyncRequest;
import config.Application;
import config.DD;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public
class D_OrgParam extends ASNObj{
	
	private static final String V0 = "0";
	static final boolean DEBUG = false;
	public long field_LID;
	public String label; //UTF8
	public String label_lang; //Printa
	public boolean can_be_provided_later;
	public boolean certificated;
	public int entry_size;
	public int partNeigh;
	public boolean required;
	public String default_value; //UTF8
	public String default_value_lang; // Printa
	public String[] list_of_values; //UTF
	public String list_of_values_lang; // Print
	public String tip; //UTF
	public String tip_lang;//Print
	public BigInteger[] oid;
	public String global_field_extra_ID;
	public String version = V0;
	public String tmp;
	public boolean dirty;
	
	public D_OrgParam(D_FieldExtra e){
		can_be_provided_later = e.can_be_provided_later;
		certificated = e.certificated;
		default_value = e.default_val;
		default_value_lang = e.default_value_lang;
		entry_size = e.entry_size;
		global_field_extra_ID = e.global_field_extra_ID;
		label=e.label;
		label_lang=e.label_lang;
		list_of_values=e.list_of_values;
		list_of_values_lang = getOneLanguage(e.list_of_values_lang);
		oid=e.oid;
		partNeigh=e.partNeigh;
		required=e.required;
		tip = e.tip;
		tip_lang = e.tip_lang;
	}
	
	public D_OrgParam() {}
	public D_OrgParam(String local_fe_ID) throws P2PDDSQLException {
		String psql = "SELECT "+table.field_extra.org_field_extra +
		" FROM "+table.field_extra.TNAME+
		" WHERE "+table.field_extra.field_extra_ID+"=?";
		ArrayList<ArrayList<Object>> a = Application.db.select(psql,new String[]{local_fe_ID});
		if(a.size()==0) return;
		init(this, a.get(0));
	}
	/**
	String psql = "SELECT "+table.field_extra.org_field_extra +
			" FROM "+table.field_extra.TNAME+
 * 
 * @param fe
 * @return
 * @throws P2PDDSQLException
 */
	public static D_OrgParam  getOrgParam(ArrayList<Object> fe) throws P2PDDSQLException {
		D_OrgParam op = new D_OrgParam();
		init(op, fe);
		return op;
	}
	public static D_OrgParam  init(D_OrgParam op, ArrayList<Object> fe) throws P2PDDSQLException {
			op.global_field_extra_ID = Util.getString(fe.get(table.field_extra.OPARAM_GID));
			op.label = Util.getString(fe.get(table.field_extra.OPARAM_LABEL));
			op.label_lang= Util.getString(fe.get(table.field_extra.OPARAM_LABEL_L));
			op.can_be_provided_later = Util.stringInt2bool(Util.getString(fe.get(table.field_extra.OPARAM_LATER)), false);
			op.certificated = Util.stringInt2bool(Util.getString(fe.get(table.field_extra.OPARAM_CERT)), false);
			try{op.entry_size = Integer.parseInt(Util.getString(fe.get(table.field_extra.OPARAM_SIZE)));}catch(NumberFormatException e){op.entry_size = 0;}
			try{op.partNeigh = Integer.parseInt(Util.getString(fe.get(table.field_extra.OPARAM_NEIGH)));}catch(NumberFormatException e){op.partNeigh = 0;}
			op.required = Util.stringInt2bool(Util.getString(fe.get(table.field_extra.OPARAM_REQ)), false);
			op.default_value = Util.getString(fe.get(table.field_extra.OPARAM_DEFAULT));
			op.default_value_lang = Util.getString(fe.get(table.field_extra.OPARAM_DEFAULT_L));
			try{op.list_of_values = Util.getString(fe.get(table.field_extra.OPARAM_LIST_VAL)).split(Pattern.quote(table.organization.ORG_VAL_SEP));}catch(Exception e){}
			op.list_of_values_lang = Util.getString(fe.get(table.field_extra.OPARAM_LIST_VAL_L));
			op.tip = Util.getString(fe.get(table.field_extra.OPARAM_TIP));
			op.tip_lang = Util.getString(fe.get(table.field_extra.OPARAM_TIP_L));
			op.oid = Util.string2BIOID(Util.getString(fe.get(table.field_extra.OPARAM_OID)));
			op.version = Util.getString(fe.get(table.field_extra.OPARAM_VERSION));
			op.field_LID = Util.lval(fe.get(table.field_extra.OPARAM_EXTRA_FIELD_ID));
			op.tmp = Util.getString(fe.get(table.field_extra.OPARAM_TMP));
			return op;
	}

	private String getOneLanguage(String[] list_of_values_lang2) {
		if(list_of_values_lang2==null) return "en";
		if(list_of_values_lang2.length==0) return "en";
		return list_of_values_lang2[0];
	}

	public String toString() {
		return "OrgParam: [["+
		";\n version="+version+
		";\n label="+label+
		";\n label_lang="+label_lang+
		";\n field_LID="+field_LID+
		";\n global_field_extra_ID="+global_field_extra_ID+
		";\n can_be_provided_later="+can_be_provided_later+
		";\n certificated="+certificated+
		";\n entry_size="+entry_size+
		";\n partNeigh="+partNeigh+
		";\n required="+required+
		";\n default_value="+default_value+
		";\n default_value_lang="+default_value_lang+
		";\n list_of_values=["+Util.concat(list_of_values, " ; ")+"]"+
		";\n list_of_values_lang="+list_of_values_lang+
		";\n tip="+tip+
		";\n tip_lang="+tip_lang+
		";\n oid=["+Util.concat(oid, ":", "NULL")+"]"+
		"]]";
	}
	public D_OrgParam instance() {return new D_OrgParam();}
	
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) {
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding OrgParam: "+this);
		Encoder enc = new Encoder().initSequence();

		String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_field_extra_ID);		
		enc.addToSequence(new Encoder(repl_GID));
		
		enc.addToSequence(new Encoder(can_be_provided_later));
		enc.addToSequence(new Encoder(certificated));
		enc.addToSequence(new Encoder(entry_size));
		enc.addToSequence(new Encoder(partNeigh));
		enc.addToSequence(new Encoder(required));
		if(label!=null) enc.addToSequence(new Encoder(label).setASN1Type(DD.TAG_AC0));
		if(label_lang!=null) enc.addToSequence(new Encoder(label_lang, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(default_value!=null) enc.addToSequence(new Encoder(default_value).setASN1Type(DD.TAG_AC2));
		if(default_value_lang!=null) enc.addToSequence(new Encoder(default_value_lang, false).setASN1Type(DD.TAG_AC3));
		if(list_of_values!=null) enc.addToSequence(Encoder.getStringEncoder(list_of_values, Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC4));
		if(list_of_values_lang!=null) enc.addToSequence(new Encoder(list_of_values_lang, false).setASN1Type(DD.TAG_AC5));
		if(tip!=null) enc.addToSequence(new Encoder(tip).setASN1Type(DD.TAG_AC6));
		if(tip_lang!=null) enc.addToSequence(new Encoder(tip_lang, false).setASN1Type(DD.TAG_AC7));
		if((oid!=null)&&(oid.length>=2))enc.addToSequence(new Encoder(oid).setASN1Type(DD.TAG_AC8));
		if(version!=null)enc.addToSequence(new Encoder(version).setASN1Type(DD.TAG_AC9));
		if(ASNSyncRequest.DEBUG)System.out.println("Encoded OrgParam: "+this);
		return enc;
	}
	@Override
	public D_OrgParam decode(Decoder decoder) throws ASN1DecoderFail {
		if(ASNSyncRequest.DEBUG)System.out.println("DEcoding OrgParam: "+this);
		version = null;
		Decoder dec = decoder.getContent();
		global_field_extra_ID=dec.getFirstObject(true).getString();
		can_be_provided_later=dec.getFirstObject(true).getBoolean();
		certificated=dec.getFirstObject(true).getBoolean();
		entry_size=dec.getFirstObject(true).getInteger().intValue();
		partNeigh=dec.getFirstObject(true).getInteger().intValue();
		required=dec.getFirstObject(true).getBoolean();
		if(dec.getTypeByte()==DD.TAG_AC0) label=dec.getFirstObject(true).getStringAnyType(); else label = null;
		if(dec.getTypeByte()==DD.TAG_AC1) label_lang=dec.getFirstObject(true).getStringAnyType();
		if(dec.getTypeByte()==DD.TAG_AC2) default_value=dec.getFirstObject(true).getStringAnyType();
		if(dec.getTypeByte()==DD.TAG_AC3) default_value_lang=dec.getFirstObject(true).getStringAnyType();
		if(dec.getTypeByte()==DD.TAG_AC4) list_of_values=dec.getFirstObject(true).getSequenceOf(Encoder.TAG_UTF8String);
		if(dec.getTypeByte()==DD.TAG_AC5) list_of_values_lang=dec.getFirstObject(true).getStringAnyType();
		if(dec.getTypeByte()==DD.TAG_AC6) tip=dec.getFirstObject(true).getStringAnyType(); else tip = null;
		if(dec.getTypeByte()==DD.TAG_AC7) tip_lang=dec.getFirstObject(true).getStringAnyType(); else tip_lang = null;
		if(dec.getTypeByte()==DD.TAG_AC8) oid=dec.getFirstObject(true).getBNOID(DD.TAG_AC8); else oid = null;
		if(dec.getTypeByte()==DD.TAG_AC9) version=dec.getFirstObject(true).getStringAnyType(); else version = null;
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects in decoder: "+decoder.dumpHex());
		if(ASNSyncRequest.DEBUG)System.out.println("Decoded OrgParam: "+this);
		return this;
	}
	/**
	 * Generates an ascii  string
	 * @param list_of_values2 
	 * @param label2 
	 * @param orgGID (in fact, it can use a orgID)
	 * @param label
	 * @return
	 */
	public String makeGID(){
		//return Util.getOrgFieldExtraID(orgGID, label);
		return makeGID(label, list_of_values, version );
	}
	/**
	 * Generates a cvasi random string
	 * @param orgGID (in fact, it can use a orgID)
	 * @param label
	 * @param list_of_values2 
	 * @return
	 */
	public static String makeGID(
			//String orgGID,
			String label, String[] list_of_values, String version){
		//return Util.getOrgFieldExtraID(orgGID, label);
		Encoder enc;
		if (version != null) {
			enc = new Encoder().initSequence();
			if(label!=null)enc.addToSequence(new Encoder(label));
			if(list_of_values!=null)enc.addToSequence(Encoder.getStringEncoder(list_of_values, Encoder.TAG_UTF8String));
		}else{
			enc = new Encoder(label);
		}
		return data.D_GIDH.d_OrgExtraFieldSign+Util.stringSignatureFromByte(enc.getBytes());
	}
}