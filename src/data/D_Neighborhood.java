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

import hds.ASNPoint;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.regex.Pattern;

import util.P2PDDSQLException;

import simulator.Fill_database;
import streaming.ConstituentHandling;
import streaming.NeighborhoodHandling;
import streaming.OrgHandling;
import streaming.RequestData;
import util.Summary;
import util.Util;
import ciphersuits.SK;

import config.Application;
import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
WB_Neighborhood ::= SEQUENCE {
 global_neighborhood_ID PrintableString,
 address Document,
 "name" UTF8String,
 "name_lang" PrintableString,
 name_division UTF8String,
 name_division_lang PrintableString,
 name_division_charset PrintableString,
 names_subdivisions SEQUENCE OF UTF8String,
 name_subdivisions_lang SEQUENCE OF PrintableString,
 name_subdivisions_charset SEQUENCE OF PrintableString,
 submitter WB_Constituent,
 signature OCTET_STRING
}
 */
public 
class D_Neighborhood extends ASNObj implements Summary{
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public String global_neighborhood_ID;
	public String name;
	public String name_lang;
	public String description;
	public ASNPoint[] boundary; //explanation, GPS coordinates, etc
	public String name_division;
	public String[] names_subdivisions;
	public D_Neighborhood parent;
	public String parent_global_ID;
	
	public D_Constituent submitter; //OPTIONAL
	public String submitter_global_ID; //OPTIONAL
	public Calendar creation_date;
	public Calendar arrival_date;
	public byte[] picture;
	public byte[] signature;
	
	// temporary values: may not have been initialized
	public String parent_ID = null;
	public String submitter_ID; // if negative we know it was not yet initialized
	public long organization_ID = -1;
	public String global_organization_ID = null;
	public String neighborhoodID = null;
	
	public boolean blocked=false, requested=false, broadcasted=true;
	private String name_charset;
	
	public String toSummaryString() {
		return "WB_Neighborhood: ["+
				//";\n creation_date*="+Encoder.getGeneralizedTime(creation_date)+
		";\n name*="+name+
		";\n name_division*="+name_division+
		"]";
	}
	public String toString() {
		return "WB_Neighborhood: ["+
		";\n creation_date*="+Encoder.getGeneralizedTime(creation_date)+
		";\n name*="+name+
		";\n name_lang*="+name_lang+
		";\n description*="+description+
		";\n global_neighborhood_ID="+global_neighborhood_ID+
		";\n boundary*=["+Util.nullDiscrim(boundary, Util.concat(boundary, ":"))+"]"+
		";\n name_division*="+name_division+
		";\n name_subdivisions*="+Util.nullDiscrimArray(names_subdivisions)+
		";\n orgID="+organization_ID+
		";\n orgGID="+global_organization_ID+
		";\n parentGID*="+parent_global_ID+
		";\n parentID="+parent_ID+
		";\n parent(GID*)="+parent+
		";\n submitterGID*="+submitter_global_ID+
		";\n submit_ID="+submitter_ID+
		";\n submit(GID*)="+submitter+
		";\n picture*="+Util.nullDiscrim(picture, Util.byteToHexDump(picture))+
		";\n signature="+Util.byteToHexDump(signature)+
		"]";
	}
	
	public D_Neighborhood(ASNNeighborhoodOP neighborhood) {
		//ASNNeighborhood n = neighborhood.neighborhood;
		D_Neighborhood n = neighborhood.neighborhood;
		global_neighborhood_ID = n.global_neighborhood_ID;
		name = n.name;
		name_lang = n.name_lang;
		boundary = n.boundary;
		name_division = n.name_division;
		names_subdivisions = n.names_subdivisions;
		parent_global_ID = n.parent_global_ID;
		description = n.description;
		
		submitter_global_ID = n.submitter_global_ID;
		picture = n.picture;
		signature = n.signature;
	}
	public D_Neighborhood() {}
	public D_Neighborhood(String gid) throws Exception {
		init(null, gid);
	}
	public D_Neighborhood(long id) throws Exception {
		init(Util.getStringID(id), null);
	}

	public D_Neighborhood instance() throws CloneNotSupportedException{return new D_Neighborhood();}
	/**
	 * Use local if available, otherwise global
	 * @param local_n_id
	 * @param neigh_gid
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_Neighborhood getNeighborhood(String local_n_id, String neigh_gid) throws P2PDDSQLException {
		try {
			return new D_Neighborhood(local_n_id, neigh_gid);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public D_Neighborhood(String local_n_id, String neigh_gid) throws Exception {
		init(local_n_id, neigh_gid);
	}
	public void init(String local_n_id, String neigh_gid) throws Exception {
		if(DEBUG) System.out.println("D_Neighborhood:init: getNeighborhood: loval_n_id="+local_n_id+" neigh_GID="+neigh_gid);
		String sql = "SELECT  "+Util.setDatabaseAlias(table.neighborhood.fields_neighborhoods, "n")+
		", p."+table.neighborhood.global_neighborhood_ID+
		", c."+table.constituent.global_constituent_ID+
		", o."+table.organization.global_organization_ID+
		" FROM "+table.neighborhood.TNAME+" AS n "+
		" LEFT JOIN "+table.constituent.TNAME+" AS c ON(n."+table.neighborhood.submitter_ID+"=c."+table.constituent.constituent_ID+") "+
		" LEFT JOIN "+table.neighborhood.TNAME+" AS p ON(n."+table.neighborhood.parent_nID+"=p."+table.neighborhood.neighborhood_ID+") "+
		" LEFT JOIN "+table.organization.TNAME+" AS o ON(n."+table.neighborhood.organization_ID+"=o."+table.organization.organization_ID+") "
		;
	
		ArrayList<ArrayList<Object>> a;
		//String order = " ORDER BY n."+table.neighborhood.arrival_date+";";
		if(local_n_id != null){
			String cond = " WHERE n."+table.neighborhood.neighborhood_ID+"=?;";
			a = Application.db.select(sql+cond, new String[]{local_n_id});
		}else{
			if(neigh_gid==null) throw new Exception("D_Neighborhood:init: None, null ID questioned");
			String cond = " WHERE n."+table.neighborhood.global_neighborhood_ID+"=?;";
			a = Application.db.select(sql+cond, new String[]{neigh_gid});			
		}
		if(a.size()==0) throw new Exception("D_Neighborhood:init:None for lID="+local_n_id+" GID="+neigh_gid);
		init(a.get(0));
		//WB_Neighborhood result = getNeighborhood(a.get(0));		
		//result.neighborhoodID = local_n_id;
		if(DEBUG) System.out.println("D_Neighborhood: init: got="+this);//result);
		//return result;
	}
	/**
		String sql = "SELECT  "+Util.setDatabaseAlias(table.neighborhood.fields_neighborhoods, "n")+
		", p."+table.neighborhood.global_neighborhood_ID+
		", c."+table.constituent.global_constituent_ID+
		", o."+table.organization.global_organization_ID+
		" FROM "+table.neighborhood.TNAME+" AS n "+
		" LEFT JOIN "+table.constituent.TNAME+" AS c ON(n."+table.neighborhood.submitter_ID+"=c."+table.constituent.constituent_ID+") "+
		" LEFT JOIN "+table.neighborhood.TNAME+" AS p ON(n."+table.neighborhood.parent_nID+"=p."+table.neighborhood.neighborhood_ID+") "+
		" LEFT JOIN "+table.organization.TNAME+" AS o ON(n."+table.neighborhood.organization_ID+"=o."+table.organization.organization_ID+") "
	 * 
	 * @param N
	 */
	public void init(ArrayList<Object> N) {
		boundary = null;
		creation_date = Util.getCalendar(Util.getString(N.get(table.neighborhood.IDX_CREATION_DATE)));
		arrival_date = Util.getCalendar(Util.getString(N.get(table.neighborhood.IDX_ARRIVAL_DATE)));
		global_neighborhood_ID = Util.getString(N.get(table.neighborhood.IDX_GID));
		neighborhoodID = Util.getString(N.get(table.neighborhood.IDX_ID));
		description = Util.getString(N.get(table.neighborhood.IDX_DESCRIPTION));
		name = Util.getString(N.get(table.neighborhood.IDX_NAME));
		name_lang = Util.getString(N.get(table.neighborhood.IDX_NAME_LANG));
		name_charset = Util.getString(N.get(table.neighborhood.IDX_NAME_CHARSET));
		name_division = Util.getString(N.get(table.neighborhood.IDX_NAME_DIVISION));
		submitter_ID = Util.getString(N.get(table.neighborhood.IDX_SUBMITTER_ID));
		parent_ID = Util.getString(N.get(table.neighborhood.IDX_PARENT_ID));
		organization_ID = Util.lval(Util.getString(N.get(table.neighborhood.IDX_ORG_ID)),-1);
		String subs = Util.getString(N.get(table.neighborhood.IDX_NAMES_DUBDIVISIONS));
		if(subs!=null)names_subdivisions = splitSubDivisions(subs);
		parent_global_ID = Util.getString(N.get(table.neighborhood.IDX_FIELDs+0));
		submitter_global_ID = Util.getString(N.get(table.neighborhood.IDX_FIELDs+1));
		global_organization_ID = Util.getString(N.get(table.neighborhood.IDX_FIELDs+2));
		picture = Util.byteSignatureFromString(Util.getString(N.get(table.neighborhood.IDX_PICTURE)));
		signature = Util.byteSignatureFromString(Util.getString(N.get(table.neighborhood.IDX_SIGNATURE)));	
		blocked = Util.stringInt2bool(Util.getString(N.get(table.neighborhood.IDX_BLOCKED)), false);	
		requested = Util.stringInt2bool(Util.getString(N.get(table.neighborhood.IDX_REQUESTED)), false);	
		broadcasted = Util.stringInt2bool(Util.getString(N.get(table.neighborhood.IDX_BROADCASTED)), false);
		
		if(DEBUG) System.out.println("D_Neighborhood: init: got="+this);
	}
	public static D_Neighborhood getNeighborhood(ArrayList<Object> N) {
		D_Neighborhood n = new D_Neighborhood();
		n.boundary = null;
		n.creation_date = Util.getCalendar(Util.getString(N.get(table.neighborhood.IDX_CREATION_DATE)));
		n.global_neighborhood_ID = Util.getString(N.get(table.neighborhood.IDX_GID));
		n.neighborhoodID = Util.getString(N.get(table.neighborhood.IDX_ID));
		n.description = Util.getString(N.get(table.neighborhood.IDX_DESCRIPTION));
		n.name = Util.getString(N.get(table.neighborhood.IDX_NAME));
		n.name_lang = Util.getString(N.get(table.neighborhood.IDX_NAME_LANG));
		n.name_division = Util.getString(N.get(table.neighborhood.IDX_NAME_DIVISION));
		n.submitter_ID = Util.getString(N.get(table.neighborhood.IDX_SUBMITTER_ID));
		n.parent_ID = Util.getString(N.get(table.neighborhood.IDX_PARENT_ID));
		n.organization_ID = Util.lval(Util.getString(N.get(table.neighborhood.IDX_ORG_ID)),-1);
		String subs = Util.getString(N.get(table.neighborhood.IDX_NAMES_DUBDIVISIONS));
		if(subs!=null)n.names_subdivisions = splitSubDivisions(subs);
		n.parent_global_ID = Util.getString(N.get(table.neighborhood.IDX_FIELDs+0));
		n.submitter_global_ID = Util.getString(N.get(table.neighborhood.IDX_FIELDs+1));
		n.global_organization_ID = Util.getString(N.get(table.neighborhood.IDX_FIELDs+2));
		n.picture = Util.byteSignatureFromString(Util.getString(N.get(table.neighborhood.IDX_PICTURE)));
		n.signature = Util.byteSignatureFromString(Util.getString(N.get(table.neighborhood.IDX_SIGNATURE)));
		n.blocked = Util.stringInt2bool(Util.getString(N.get(table.neighborhood.IDX_BLOCKED)), false);	
		n.requested = Util.stringInt2bool(Util.getString(N.get(table.neighborhood.IDX_REQUESTED)), false);	
		n.broadcasted = Util.stringInt2bool(Util.getString(N.get(table.neighborhood.IDX_BROADCASTED)), false);	
		return n;
	}

	@Override
	public D_Neighborhood decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==Encoder.TAG_PrintableString)global_organization_ID = dec.getFirstObject(true).getString();
		if(dec.getTypeByte()==DD.TAG_AC0)global_neighborhood_ID = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if(dec.getTypeByte()==DD.TAG_AC1)name = dec.getFirstObject(true).getString(DD.TAG_AC1);
		if(dec.getTypeByte()==DD.TAG_AC2)name_lang = dec.getFirstObject(true).getString(DD.TAG_AC2);
		if(dec.getTypeByte()==DD.TAG_AC3)description = dec.getFirstObject(true).getString(DD.TAG_AC3);
		if(dec.getTypeByte()==Encoder.TAG_SEQUENCE) boundary = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new ASNPoint[]{}, new ASNPoint());
		if(dec.getTypeByte()==DD.TAG_AC4)name_division = dec.getFirstObject(true).getString(DD.TAG_AC4);
		if(dec.getTypeByte()==DD.TAG_AC6)names_subdivisions = dec.getFirstObject(true).getSequenceOf(DD.TAG_AC5);
		if(dec.getTypeByte()==DD.TAG_AC7)parent_global_ID = dec.getFirstObject(true).getString(DD.TAG_AC7);
		if(dec.getTypeByte()==DD.TAG_AC8)parent = new D_Neighborhood().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC9)submitter_global_ID = dec.getFirstObject(true).getString(DD.TAG_AC9);
		if(dec.getTypeByte()==DD.TAG_AC10)submitter = new D_Constituent().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC11)creation_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC11);
		if(dec.getTypeByte()==DD.TAG_AC12)picture = dec.getFirstObject(true).getBytes(DD.TAG_AC12);
		if(dec.getTypeByte()==DD.TAG_AC13)signature = dec.getFirstObject(true).getBytes(DD.TAG_AC13);
		return this;
	}
	/**
	 * ASN1 type of the structure returned by getEncoder
	 * @return
	 */
	public static byte getASN1Type() {
		return Encoder.TAG_SEQUENCE;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(this.global_organization_ID!=null)enc.addToSequence(new Encoder(this.global_organization_ID,Encoder.TAG_PrintableString));
		if(global_neighborhood_ID!=null)enc.addToSequence(new Encoder(global_neighborhood_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(name!=null) enc.addToSequence(new Encoder(name).setASN1Type(DD.TAG_AC1));
		if(name_lang!=null)enc.addToSequence(new Encoder(name_lang,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(description!=null) enc.addToSequence(new Encoder(description).setASN1Type(DD.TAG_AC3));
		if(boundary!=null) enc.addToSequence(Encoder.getEncoder(boundary));
		if(name_division!=null) enc.addToSequence(new Encoder(name_division).setASN1Type(DD.TAG_AC4));
		if(names_subdivisions!=null) enc.addToSequence(Encoder.getStringEncoder(names_subdivisions, DD.TAG_AC5).setASN1Type(DD.TAG_AC6));
		if(parent_global_ID!=null) enc.addToSequence(new Encoder(parent_global_ID).setASN1Type(DD.TAG_AC7));
		//else if((parent!=null)&&(parent.global_neighborhood_ID!=null)) enc.addToSequence(new Encoder(parent.global_neighborhood_ID).setASN1Type(DD.TAG_AC7));
		if(parent!=null) enc.addToSequence(parent.getEncoder()).setASN1Type(DD.TAG_AC8);
		if(submitter_global_ID!=null) enc.addToSequence(new Encoder(submitter_global_ID).setASN1Type(DD.TAG_AC9));
		//else if((submitter!=null)&&(submitter.global_ID!=null)) enc.addToSequence(new Encoder(submitter.global_ID).setASN1Type(DD.TAG_AC9));
		if(submitter!=null) enc.addToSequence(submitter.getEncoder()).setASN1Type(DD.TAG_AC10);
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC11));
		if(picture!=null) enc.addToSequence(new Encoder(picture).setASN1Type(DD.TAG_AC12));
		if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC13));
		return enc;
	}
	public Encoder getSignableEncoder(String orgGID) {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(orgGID,Encoder.TAG_PrintableString));
		if(global_neighborhood_ID!=null)enc.addToSequence(new Encoder(global_neighborhood_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(name!=null) enc.addToSequence(new Encoder(name).setASN1Type(DD.TAG_AC1));
		if(name_lang!=null)enc.addToSequence(new Encoder(name_lang,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(description!=null) enc.addToSequence(new Encoder(description).setASN1Type(DD.TAG_AC3));
		if(boundary!=null) enc.addToSequence(Encoder.getEncoder(boundary));
		if(name_division!=null) enc.addToSequence(new Encoder(name_division).setASN1Type(DD.TAG_AC4));
		if(names_subdivisions!=null) enc.addToSequence(Encoder.getStringEncoder(names_subdivisions, DD.TAG_AC5).setASN1Type(DD.TAG_AC6));
		if(parent_global_ID!=null) enc.addToSequence(new Encoder(parent_global_ID).setASN1Type(DD.TAG_AC7));
		else if((parent!=null)&&(parent.global_neighborhood_ID!=null)) enc.addToSequence(new Encoder(parent.global_neighborhood_ID).setASN1Type(DD.TAG_AC7));
		//if(parent!=null) enc.addToSequence(parent.getEncoder()).setASN1Type(DD.TAG_AC8);
		if(submitter_global_ID!=null) enc.addToSequence(new Encoder(submitter_global_ID).setASN1Type(DD.TAG_AC9));
		else if((submitter!=null)&&(submitter.global_constituent_id!=null)) enc.addToSequence(new Encoder(submitter.global_constituent_id).setASN1Type(DD.TAG_AC9));
		//if(submitter!=null) enc.addToSequence(submitter.getEncoder()).setASN1Type(DD.TAG_AC10);
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC11));
		if(picture!=null) enc.addToSequence(new Encoder(picture).setASN1Type(DD.TAG_AC12));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC13));
		return enc;
	}
	public Encoder getHashEncoder(String orgGID) {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(orgGID,Encoder.TAG_PrintableString));
		//if(global_neighborhood_ID!=null)enc.addToSequence(new Encoder(global_neighborhood_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(name!=null) enc.addToSequence(new Encoder(name).setASN1Type(DD.TAG_AC1));
		if(name_lang!=null)enc.addToSequence(new Encoder(name_lang,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(description!=null) enc.addToSequence(new Encoder(description).setASN1Type(DD.TAG_AC3));
		if(boundary!=null) enc.addToSequence(Encoder.getEncoder(boundary));
		if(name_division!=null) enc.addToSequence(new Encoder(name_division).setASN1Type(DD.TAG_AC4));
		if(names_subdivisions!=null) enc.addToSequence(Encoder.getStringEncoder(names_subdivisions, DD.TAG_AC5).setASN1Type(DD.TAG_AC6));
		if(parent_global_ID!=null) enc.addToSequence(new Encoder(parent_global_ID).setASN1Type(DD.TAG_AC7));
		else if((parent!=null)&&(parent.global_neighborhood_ID!=null)) enc.addToSequence(new Encoder(parent.global_neighborhood_ID).setASN1Type(DD.TAG_AC7));
		//if(parent!=null) enc.addToSequence(parent.getEncoder()).setASN1Type(DD.TAG_AC8);
		//if(submitter_global_ID!=null) enc.addToSequence(new Encoder(submitter_global_ID).setASN1Type(DD.TAG_AC9));
		//else if((submitter!=null)&&(submitter.global_constituent_id!=null)) enc.addToSequence(new Encoder(submitter.global_constituent_id).setASN1Type(DD.TAG_AC9));
		//if(submitter!=null) enc.addToSequence(submitter.getEncoder()).setASN1Type(DD.TAG_AC10);
		//if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC11));
		if(picture!=null) enc.addToSequence(new Encoder(picture).setASN1Type(DD.TAG_AC12));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC13));
		return enc;
	}
	
	public byte[] sign(SK sk){
		return sign(sk, this.global_organization_ID);
	}
	public byte[] sign(SK sk, String orgGID){
		if(DEBUG) System.out.println("WB_Neighborhood:sign: start");
		if(DEBUG) System.out.println("WB_Neighborhood:sign: this="+this);
		//if(DEBUG) System.out.println("WB_Neighborhood:sign: this="+this+"\nsk="+sk+"\norgID=\""+orgGID+"\"");
		if(DEBUG) System.out.println("WB_Neighborhood:sign: sk="+sk);
		if(DEBUG) System.out.println("WB_Neighborhood:sign: orgGID=\""+orgGID+"\"");
		this.creation_date = Util.CalendargetInstance();
		if(this.global_neighborhood_ID==null) make_ID(orgGID);
		this.global_organization_ID = orgGID;
		if(DEBUG) System.out.println("WB_Neighborhood:sign: this="+this+"\norgGID="+orgGID);
		signature = Util.sign(this.getSignableEncoder(orgGID).getBytes(), sk);
		if(DEBUG) System.out.println("\nWB_Neighborhood:sign:got this="+Util.byteToHexDump(signature));
		if(DEBUG) System.out.println("\nWB_Neighborhood:sign: equiv pk="+sk.getPK());
		if(DD.VERIFY_AFTER_SIGNING_NEIGHBORHOOD)if(!verifySign(orgGID)) Util.printCallPath("Fail to test signature");
		return signature;
	}
	public String make_ID(){
		return make_ID(this.global_organization_ID);
	}
	public String make_ID(String orgGID){
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("WB_Neighborhood:make_ID: orgGID=\""+orgGID+"\"");
		if(DEBUG) System.out.println("WB_Neighborhood:make_ID: this=\""+this+"\"");
		try {
			fillGlobals();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		this.global_neighborhood_ID  = null; // this will be created as result
		
		if ((this.parent_ID != null) && (this.parent_global_ID == null))
			try {
				this.parent_global_ID = D_Neighborhood.getGlobalID(this.parent_ID);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		
		Calendar _creation_date=this.creation_date; // date should not differentiate between neighborhoods
		this.creation_date = null;
		
		String _submitter_global_ID = submitter_global_ID; // submitter should not differentiate between neighborhoods
		D_Constituent _submitter = submitter;
		submitter_global_ID=null;
		submitter=null;
		
		byte[] data=this.getHashEncoder(orgGID).getBytes();
		
		this.creation_date = _creation_date;
		this.submitter_global_ID = _submitter_global_ID;
		this.submitter = _submitter;
		
		if(DEBUG) System.out.println("WB_Neighborhood:make_ID: data="+Util.byteToHex(data));
		String gid= this.global_neighborhood_ID =  "N:"+Util.getGID_as_Hash(data);
		if(DEBUG) System.out.println("WB_Neighborhood:make_ID: gid="+gid);
		return gid;
	}
	private void fillGlobals() throws P2PDDSQLException {
		if((this.organization_ID > 0 ) && (this.global_organization_ID == null))
			this.global_organization_ID = D_Organization.getGlobalOrgID(Util.getStringID(this.organization_ID));

		if((this.submitter_ID != null ) && (this.submitter_global_ID == null))
			this.submitter_global_ID = D_Constituent.getConstituentGlobalID(this.submitter_ID);

		if((this.parent_ID != null ) && (this.parent_global_ID == null))
			this.parent_global_ID = D_Neighborhood.getGlobalID(this.parent_ID);
		
	}
	public boolean verifySignature(){
		return verifySign(this.global_organization_ID);
	}
	public boolean verifySign(String orgGID){
		if(DEBUG) System.out.println("WB_Neighborhood:verifySign: orgGID=\""+orgGID+"\"");
		try {
			fillGlobals();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		//Util.printCallPath("recursive?");
		String old_GID = this.global_neighborhood_ID;
		String tID = make_ID(orgGID);
		if(!tID.equals(old_GID)) {
			this.global_neighborhood_ID = old_GID;
			if(_DEBUG) System.out.println("WB_Neighborhood:verifySign: wrong GID:"+old_GID+" vs newGID="+tID);
			Util.printCallPath("Wrong NeighGID");
			if(_DEBUG) System.out.println("WB_Neighborhood:verifySign: wrong GID for"+this);
			return false;
		}
		
		String pk_ID = this.submitter_global_ID;
		if((pk_ID == null) && (this.submitter!=null) && (this.submitter.global_constituent_id!=null))
			pk_ID = this.submitter.global_constituent_id;
		if(pk_ID == null){
			if(_DEBUG) System.out.println("WB_Neighborhood:verifySign: unknown submitter");
			return false;
		}
		this.global_organization_ID = orgGID;
		if(DEBUG) System.out.println("WB_Neighborhood:verifySign: neigh=\""+this+"\""+"\norgGID="+orgGID);
		if(DEBUG) System.out.println("WB_Neighborhood:verifySign: pk_ID=\""+pk_ID+"\"");
		if(DEBUG) System.out.println("WB_Neighborhood:verifySign: sign=\""+Util.byteToHexDump(signature)+"\"");
		return Util.verifySignByID(this.getSignableEncoder(orgGID).getBytes(), pk_ID, signature);
	}
	public String storeVerified(String constituent_ID, String orgGID, String org_local_ID, String arrival_time, RequestData sol_rq, RequestData new_rq) throws P2PDDSQLException {
		if(DEBUG) System.out.println("WB_Neighborhood:storeVerified: setting orgGID="+orgGID+"\nl_org="+org_local_ID);
		this.global_organization_ID = orgGID;
		this.organization_ID = Util.lval(org_local_ID, organization_ID);
		if(!fillLocals(null, true, false, true, true)){
			System.err.println("N_Neighborhood:storeVerified: failure to store neighborhood locals");
			return null;
		}
		if(DEBUG) System.out.println("WB_Neighborhood:storeVerified: org_ID="+org_local_ID+" orgGID="+orgGID+" arrival="+arrival_time);
		return integrateNewVerifiedNeighborhoodData(this, orgGID, org_local_ID, arrival_time, null, sol_rq, new_rq);
	}
	/**
	 * 
	 * @param rq
	 * @param tempOrg
	 * @param default_blocked_org
	 * @param tempConst : true if a temporary should be created on absence
	 * @param tempPar : true if a temporary should be created on absence
	 * @return
	 * @throws P2PDDSQLException
	 */
	public boolean fillLocals(RequestData rq, boolean tempOrg, boolean default_blocked_org, boolean tempConst, boolean tempPar) throws P2PDDSQLException {
		if(DEBUG) System.out.println("D_Neighborhood: fillLocals: start");
		if((global_organization_ID==null)&&(organization_ID <= 0)){
			Util.printCallPath("cannot store constituent with not orgGID");
			return false;
		}
		if((this.submitter_global_ID==null) && (submitter_ID == null)){
			Util.printCallPath("cannot store constituent with not submitterGID");
			return false;
		}
		
		if((global_organization_ID!=null)&&(organization_ID <= 0)){
			organization_ID = D_Organization.getLocalOrgID(global_organization_ID);
			if(tempOrg && (organization_ID<=0)) {
				String orgGID_hash = D_Organization.getOrgGIDHashGuess(global_organization_ID);
				if(rq!=null)rq.orgs.add(orgGID_hash);
				organization_ID = D_Organization.insertTemporaryGID(global_organization_ID, orgGID_hash, default_blocked_org);
				if(default_blocked_org){
					if(DEBUG) System.out.println("D_Neighborhood: fillLocals: exit fail blocked temp org ="+default_blocked_org);
					return false;
				}
			}
			if(organization_ID<=0) return false;
		}
		
		if((this.submitter_global_ID!=null) && (submitter_ID == null)) {
			submitter_ID = D_Constituent.getConstituentLocalIDFromGID(submitter_global_ID);
			if(tempConst && (submitter_ID == null))  {
				String consGID_hash = D_Constituent.getGIDHashFromGID(submitter_global_ID);
				if(rq!=null)rq.cons.put(consGID_hash,DD.EMPTYDATE);
				submitter_ID = Util.getStringID(D_Constituent.insertTemporaryConstituentGID(submitter_global_ID, Util.getStringID(organization_ID)));
			}
			if(submitter_ID == null){
				if(DEBUG) System.out.println("D_Neighborhood: fillLocals: still empty submitter");
				return false;
			}
		}
		
		//if(this.parent_global_ID==null) return true;
		if((this.parent_global_ID!=null) && (this.parent_ID==null)) {
			parent_ID = D_Neighborhood.getLocalID(parent_global_ID);
			if(tempPar && (parent_ID == null)) {
				if(rq!=null)rq.neig.add(parent_global_ID);
				parent_ID = Util.getStringID(D_Neighborhood._insertTemporaryNeighborhoodGID(parent_global_ID, Util.getStringID(organization_ID), -1));
			}
			if(parent_ID == null){
				if(DEBUG) System.out.println("D_Neighborhood: fillLocals: still empty parent");
				return false;
			}
		}
		return true;
	}
	
	public long store(RequestData sol_rq, RequestData new_rq) throws P2PDDSQLException {
		if(DEBUG) System.out.println("D_Neighborhood: store: start");
		
		boolean locals = fillLocals(new_rq, true, true, true, true);
		if(!locals){
			if(_DEBUG) System.out.println("D_Neighborhood: store: exit no locals");
			return -1;
		}
		
		String now = Util.getGeneralizedTime();
		/*
		if (this.global_organization_ID == null){
			Util.printCallPath("cannot store constituent with not orgGID");
			return -1;
		}
		if ((this.organization_ID <= 0) && (this.global_organization_ID != null)) {
			this.organization_ID = D_Organization.getLocalOrgID(global_organization_ID);
		}
		if(this.organization_ID <= 0) {
			String orgGID_hash = D_Organization.getOrgGIDHash(global_organization_ID);
			D_Organization.insertTemporaryGID(global_organization_ID, orgGID_hash);
			rq.orgs.add(orgGID_hash);
			Util.printCallPath("Unknow org in constituent"+this);
			return -1;
		}
		*/
		String nID = store(this.global_organization_ID, ""+this.organization_ID, now, sol_rq, new_rq);
		if (nID==null){
			if(_DEBUG) System.out.println("D_Neighborhood: store: exit fail storing");
			return -1;
		}
		long _nID = new Integer(nID).longValue();
		if(DEBUG) System.out.println("D_Neighborhood: store: exit with nID="+_nID);
		return _nID;
	}

	public String store(String orgGID, String org_local_ID, String arrival_time, RequestData sol_rq, RequestData new_rq) throws P2PDDSQLException {
		return integrateNewNeighborhoodData(this, orgGID, org_local_ID, arrival_time, null, sol_rq, new_rq);
	}
	
	public static Object localForNeighborhoodGID(String gID) throws P2PDDSQLException {
		String sql = "SELECT "+table.neighborhood.neighborhood_ID+
		" FROM "+table.neighborhood.TNAME+
		" WHERE "+table.neighborhood.global_neighborhood_ID+"=?;";
		String[]params={gID};
		Application.db.select(sql, params, DEBUG);
		return null;
	}
	public static void readSignStore(long nID, SK sk, String orgGID, String submitter_ID,
			String org_local_ID, String arrival_time) throws P2PDDSQLException {
		D_Neighborhood w = D_Neighborhood.getNeighborhood(nID+"",null);
		w.sign(sk, orgGID);
		w.storeVerified(submitter_ID, orgGID, org_local_ID, arrival_time, null, null);
	}
	/**
	 * Used for storing [val,val] into database 
	 * @param names_subdivisions
	 * @return
	 */
	public static String concatSubdivisions(String[] names_subdivisions) {
		if(names_subdivisions==null) return null;
		return ":"+Util.concat(names_subdivisions,table.neighborhood.SEP_names_subdivisions,null)+":";
	}
	/**
	 * Used when loading from database into WB_Neighbornood [val:val]
	 * @param subs
	 * @return
	 */
	public static String[] splitSubDivisions(String subs) {
		if(DEBUG)System.out.println("WB_Neighborhood:splitSubDivisions: split subs="+subs);
		if(subs==null) return null;
		String[] val= subs.split(Pattern.quote(table.neighborhood.SEP_names_subdivisions));
		if(val.length<1) return null;
		if(!"".equals(val[0])) return val; // backward compatibility: if storage was not starting with ":"
		if(val.length<2) return null;
		String[] result = new String[val.length-1];
		for(int k=0;k<result.length;k++) result[k]=val[k+1];
		if(DEBUG)System.out.println("WB_Neighborhood:splitSubDivisions: got["+result.length+"]="+Util.concat(result, ":"));
		return result;
	}
	/**
	 * Used for storing ["":val:val:""] into :val:val:
	 * @param names_subdivisions
	 * @return
	 */
	public static String concatSubdivisionsIntoCAND(String[] names_subdivisions) {
		if(names_subdivisions==null) return null;
		return Util.concat(names_subdivisions,table.neighborhood.SEP_names_subdivisions,null);
	}
	/**
	 * Used when loading from :val:val: into array  ["":val:val:""] 
	 * @param subs
	 * @return
	 */
	public static String[] splitSubDivisionsFromCAND(String subs) {
		return subs.split(Pattern.quote(table.neighborhood.SEP_names_subdivisions));
	}
	public static void getLeafNeighborhoods(String org_ID) throws P2PDDSQLException{
		String sql = "SELECT n."+table.neighborhood.neighborhood_ID+
		" FROM "+table.neighborhood.TNAME+" AS n "+
		" LEFT JOIN "+table.neighborhood.TNAME+" AS d ON(d."+table.neighborhood.parent_nID+"=n."+table.neighborhood.neighborhood_ID+") "+
		" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=n."+table.neighborhood.organization_ID+") "+
		" WHERE o."+table.organization.organization_ID+"=? AND d."+table.neighborhood.neighborhood_ID+" IS NULL;"
		;
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{org_ID}, DEBUG);
		for(ArrayList<Object> s: a){
			System.out.println("Got: "+s.get(0));
		}
	}
	/**
	 * This checks for signature, not for creation date (which should not matter: can keep the oldest)
	 * @param wn
	 * @param orgGID
	 * @param org_local_ID
	 * @param arrival_time
	 * @param orgData
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String integrateNewNeighborhoodData(
			D_Neighborhood wn, String orgGID,
			String org_local_ID, String arrival_time, D_Organization orgData,
			RequestData sol_rq, RequestData new_rq) throws P2PDDSQLException {
		if(DEBUG) System.out.println("D_Neighborhood: integrateNewNeighborhoodData: exit start");
		if(wn == null){
			if(_DEBUG) System.out.println("D_Neighborhood: integrateNewNeighborhoodData: exit no data");
			return null;
		}
		if(!wn.verifySign(orgGID)){
			if(_DEBUG) System.out.println("NeighborhoodHandling:integrateNewNeighborhoodData:Signature check failed for "+wn+"\n with orgGID="+orgGID);
			if(!DD.ACCEPT_UNSIGNED_NEIGHBORHOOD){
				if(_DEBUG) System.out.println("NeighborhoodHandling:integrateNewNeighborhoodData: fail sign exit");
				return null;
			}
		}
		return integrateNewVerifiedNeighborhoodData(wn, orgGID, org_local_ID, arrival_time, orgData, sol_rq, new_rq);
	}
	public void storeVerified() throws P2PDDSQLException{
		integrateNewVerifiedNeighborhoodData(this, this.global_organization_ID, Util.getStringID(this.organization_ID), Util.getGeneralizedTime(), null, null, null);
	}
	public static String integrateNewVerifiedNeighborhoodData(
			D_Neighborhood wn, String orgGID,
			String org_local_ID, String arrival_time, D_Organization orgData,
			RequestData sol_rq, RequestData new_rq) throws P2PDDSQLException {
		try{
		String result = null;
		if(DEBUG) System.out.println("\nNeighborhoodHandling:integrateNewVerifiedNeighborhoodData: start on "+wn);
		if(DEBUG) System.out.println("NeighborhoodHandling:integrateNewVerifiedNeighborhoodData: org_ID="+org_local_ID+" orgGID="+orgGID+" arrival="+arrival_time);
		if(wn == null) {
			if(_DEBUG) System.out.println("NeighborhoodHandling:integrateNewVerifiedNeighborhoodData: wn exit "+wn);
			return null;
		}
		String pID, cID;
		if(wn.parent!=null) pID=integrateNewNeighborhoodData(wn.parent, orgGID, org_local_ID, arrival_time, orgData, sol_rq, new_rq);
		if(wn.submitter!=null) cID=ConstituentHandling.integrateNewConstituentData(wn.submitter, orgGID, org_local_ID, arrival_time, orgData, sol_rq, new_rq);

		if((org_local_ID==null)&&(orgGID!=null)) {
			org_local_ID = Util.getStringID(D_Organization.getLocalOrgID(orgGID));
			if(_DEBUG) System.out.println("NeighborhoodHandling:integrateNewVerifiedNeighborhoodData: org_id ="+org_local_ID);
		}
		String submit_ID = null;
		if(wn.submitter_ID != null) submit_ID = wn.submitter_ID;
		else submit_ID = D_Constituent.getConstituentLocalIDFromGID(wn.submitter_global_ID);
		if(submit_ID==null)
			submit_ID = Util.getStringID(D_Constituent.insertTemporaryConstituentGID(wn.submitter_global_ID, org_local_ID));
		wn.submitter_ID = submit_ID;
		
		if((wn.parent_global_ID!=null) && (wn.parent_ID==null)) {
			wn.parent_ID = D_Neighborhood.getNeighborhoodLocalID(wn.parent_global_ID);
			if(DEBUG) System.out.println("\nNeighborhoodHandling:integrateNewVerifiedNeighborhoodData: local= "+wn.parent_ID);
			if(wn.parent_ID==null) wn.parent_ID=D_Neighborhood.insertTemporaryNeighborhoodGID(wn.parent_global_ID, org_local_ID);
		}
		
		String date[] = new String[1];
		String id;
		if(wn.global_neighborhood_ID!=null){
			id=D_Neighborhood.getNeighborhoodLocalIDAndDate(wn.global_neighborhood_ID,date);
			if(id==null) id = wn.neighborhoodID;
			else{if((wn.neighborhoodID!=null)&&!id.equals(wn.neighborhoodID))Util.printCallPath("D_Neighborhood: Disagreement: id="+id+" vs old="+wn.neighborhoodID);}
		}else
			id=wn.neighborhoodID;
		if(id==null){
			id = D_Neighborhood.getLocalID(wn.global_neighborhood_ID);
			if(id!=null)
				if(_DEBUG)System.out.println("D_Neighborhood:integrateNewVerifNeigh: prereget id(GID)="+id);
		}
		// obviously GID is set by now, but may not have been saved for just edited neighborhoods. The we use the known ID for them
		//if((wn.global_neighborhood_ID!=null)||(id==null)) id = wn.neighborhoodID;
		wn.neighborhoodID = id;
    	if(DEBUG) System.err.println("ConstitentsModel:integrateNewverif: old neigh_ID="+wn.neighborhoodID+" id="+id);
 		String[] fields;
		String[] params;
		fields = table.neighborhood.fields_neighborhoods_noID.split(Pattern.quote(table.neighborhood.FIELDS_SEP));
		if(id!=null) {
			params = new String[fields.length+1];
		}else{
			params = new String[fields.length];
		}
		params[table.neighborhood.IDX_CREATION_DATE] = Encoder.getGeneralizedTime(wn.creation_date);
		params[table.neighborhood.IDX_ARRIVAL_DATE] = arrival_time;
		params[table.neighborhood.IDX_GID] = wn.global_neighborhood_ID;
		params[table.neighborhood.IDX_DESCRIPTION] = wn.description;
		params[table.neighborhood.IDX_SIGNATURE] = Util.stringSignatureFromByte(wn.signature);
		params[table.neighborhood.IDX_ORG_ID] = org_local_ID;
		params[table.neighborhood.IDX_NAME] = wn.name;
		params[table.neighborhood.IDX_NAME_LANG] = wn.name_lang;
		params[table.neighborhood.IDX_NAME_CHARSET] = wn.name_charset;
		params[table.neighborhood.IDX_NAME_DIVISION] = wn.name_division;
		params[table.neighborhood.IDX_NAMES_DUBDIVISIONS] = D_Neighborhood.concatSubdivisions(wn.names_subdivisions);// Util.concat(wn.names_subdivisions,table.neighborhood.SEP_names_subdivisions,null);
		params[table.neighborhood.IDX_PARENT_ID] = wn.parent_ID;
		params[table.neighborhood.IDX_SUBMITTER_ID] = submit_ID;
		params[table.neighborhood.IDX_BLOCKED] = Util.bool2StringInt(wn.blocked);
		params[table.neighborhood.IDX_REQUESTED] = Util.bool2StringInt(wn.requested);
		params[table.neighborhood.IDX_BROADCASTED] = Util.bool2StringInt(wn.broadcasted);
		if(id==null){
			if(DEBUG) System.out.println("\nNeighborhoodHandling:integrateNewVerifiedNeighborhoodData: insert");
			long _neig_local_ID = -1;
			try{
				_neig_local_ID = Application.db.insert(table.neighborhood.TNAME, fields, params, DEBUG);
			}catch(Exception e){
				if(_DEBUG) System.out.println("\nNeighborhoodHandling:integrateNewVerifiedNeighborhoodData: insert fail: "+wn.global_neighborhood_ID);
				id = D_Neighborhood.getLocalID(wn.global_neighborhood_ID);
				if(_DEBUG) System.out.println("\nNeighborhoodHandling:integrateNewVerifiedNeighborhoodData: insert reget: "+id);
				_neig_local_ID = Util.lval(id, -1);
			}
			result = Util.getStringID(_neig_local_ID);
		}else{
			if(DEBUG) System.out.println("\nNeighborhoodHandling:integrateNewVerifiedNeighborhoodData: update");
			if((date[0]==null)||(date[0].compareTo(params[table.neighborhood.IDX_CREATION_DATE])<0)){
				params[table.neighborhood.IDX_ID] = id;
				//params[table.neighborhood.IDX_FIELDs] = id;
				Application.db.update(table.neighborhood.TNAME, fields, new String[]{table.neighborhood.neighborhood_ID}, params, DEBUG);
			}
			result = id;
		}
		wn.neighborhoodID = id;
		if(result!=null)if(sol_rq!=null)sol_rq.neig.add(wn.global_neighborhood_ID);

		if(DEBUG) System.out.println("NeighborhoodHandling:integrateNewVerifiedNeighborhoodData:  exit id="+id);
		return result;
		}catch(Exception e){e.printStackTrace();return null;}
	}
	

	public static String getNeighborhoodLocalID(String parent_global_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("NeighborhoodHandling:getNeighborhoodLocalID: start");		
		String date[]= new String[1];
		return getNeighborhoodLocalIDAndDate(parent_global_ID, date);
	}
	public static String getNeighborhoodLocalIDAndDate(String global_ID, String[] date) throws P2PDDSQLException {
		if(DEBUG) System.out.println("NeighborhoodHandling:getNeighborhoodLocalIDAndDate: exit");		
		if(global_ID==null) return null;
		String sql = "SELECT "+table.neighborhood.neighborhood_ID+", "+table.neighborhood.creation_date+
		" FROM "+table.neighborhood.TNAME+
		" WHERE "+table.neighborhood.global_neighborhood_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.db.select(sql, new String[]{global_ID}, DEBUG);
		if(n.size()==0) return null;
		date[0] = Util.getString(n.get(0).get(1));
		return Util.getString(n.get(0).get(0));
	}

	public static String getNeighborhoodGlobalID(String local_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("NeighborhoodHandling:getNeighborhoodGlobalID: start");		
		if(local_ID==null) return null;
		String sql = "SELECT "+table.neighborhood.global_neighborhood_ID+
		" FROM "+table.neighborhood.TNAME+
		" WHERE "+table.neighborhood.neighborhood_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.db.select(sql, new String[]{local_ID}, DEBUG);
		if(n.size()==0) return null;
		return Util.getString(n.get(0).get(0));
	}
	public static String insertTemporaryNeighborhoodGID(String neighborhood_GID,
			String org_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("NeighborhoodHandling:insertTemporaryNeighborhoodGID: start");	
		if(neighborhood_GID==null){
			if(DEBUG) System.out.println("NeighborhoodHandling:insertTemporaryNeighborhoodGID: null GID");
			return null;
		}
		return ""+Application.db.insert(table.neighborhood.TNAME,
				new String[]{table.neighborhood.global_neighborhood_ID, table.neighborhood.organization_ID},
				new String[]{neighborhood_GID, org_ID},
				DEBUG);

	}
	static Object monitored_insertTemporaryNeighborhoodGID = new Object();
	public static long _insertTemporaryNeighborhoodGID(String neighborhood_GID,
			String org_ID, long _default) throws P2PDDSQLException {
		synchronized(monitored_insertTemporaryNeighborhoodGID){
			return _monitored_insertTemporaryNeighborhoodGID(neighborhood_GID,
					org_ID, _default);
		}
	}
	private static long _monitored_insertTemporaryNeighborhoodGID(String neighborhood_GID,
				String org_ID, long _default) throws P2PDDSQLException {
		if(DEBUG) System.out.println("NeighborhoodHandling:insertTemporaryNeighborhoodGID: start");	
		if(neighborhood_GID==null){
			if(_DEBUG) System.out.println("NeighborhoodHandling:insertTemporaryNeighborhoodGID: null GID");
			return _default;
		}

		long r = Util.lval(D_Neighborhood.getLocalID(neighborhood_GID), -1);
		if(r>0) return r;
		return Application.db.insert(table.neighborhood.TNAME,
				new String[]{table.neighborhood.global_neighborhood_ID, table.neighborhood.organization_ID},
				new String[]{neighborhood_GID, org_ID},
				DEBUG);

	}
	/*
	static long insertTemporaryGID(String neighborhood_GID, String _organization_ID) throws P2PDDSQLException {
		return Application.db.insertNoSync(table.neighborhood.TNAME,
				new String[]{table.neighborhood.global_neighborhood_ID, table.neighborhood.organization_ID},
				new String[]{neighborhood_GID, _organization_ID }, DEBUG);
	}
*/
	public static ArrayList<String> checkAvailability(ArrayList<String> cons,
			String orgID, boolean DBG) throws P2PDDSQLException {
		ArrayList<String> result = new ArrayList<String>();
		for (String cHash : cons) {
			if(!available(cHash, orgID, DBG)) result.add(cHash);
		}
		return result;
	}
	/**
	 * check blocking at this level
	 * @param cHash
	 * @param orgID
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static boolean available(String hash, String orgID, boolean DBG) throws P2PDDSQLException {
		String sql = 
			"SELECT "+table.neighborhood.neighborhood_ID+
			" FROM "+table.neighborhood.TNAME+
			" WHERE "+table.neighborhood.global_neighborhood_ID+"=? "+
			" AND "+table.neighborhood.organization_ID+"=? "+
			" AND ( "+table.neighborhood.signature + " IS NOT NULL " +
			" OR "+table.neighborhood.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{hash, orgID}, DEBUG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_News:available: "+hash+" in "+orgID+" = "+result);
		return result;
	}

	/**
	 * Returns the array of parent neighborhoods of global_neighborhood_ID
	 * @param global_neighborhood_ID
	 * @param nID :  local neighborhood ID
	 * @param _neighborhoods takes values: {EXPAND_NONE, EXPAND_ONE, EXPAND_ALL} EXPAND_NONE-not checked
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_Neighborhood[] getNeighborhoodHierarchy(String global_neighborhood_ID, String nID, int _neighborhoods) throws P2PDDSQLException {
		//boolean DEBUG = true;
			if(DEBUG) System.out.println("D_Neighborhood:getNeighborhoodHierarchy: neighGID="+global_neighborhood_ID+" nID="+nID+" #="+_neighborhoods);
			if(global_neighborhood_ID==null)
				global_neighborhood_ID = D_Neighborhood.getNeighborhoodGlobalID(nID);
			D_Neighborhood[] neighborhood;
			HashSet<String> nGID = new HashSet<String>();
			ArrayList<D_Neighborhood> awn = new ArrayList<D_Neighborhood>();
			String neigh_next = global_neighborhood_ID;
			do {
				if(DEBUG) System.out.println("D_Neighborhood:getNeighborhoodHierarchy: loading nGID="+neigh_next+" nID="+nID);
				if(nGID.contains(neigh_next)) {
					Util.printCallPath(Util._("D_Neighborhood:getNeighborhoodHierarchy: Circular neighborhood detected for: GID="+neigh_next+" GID="+nID));
					awn = new ArrayList<D_Neighborhood>();
					break;
				}
				//neighborhood=new WB_Neighborhood[1];
				D_Neighborhood crt_neighborhood;
				try{
					crt_neighborhood = D_Neighborhood.getNeighborhood(nID, neigh_next);
					if(DEBUG) System.out.println("D_Neighborhood: getNeighborhoodHierarchy: crt = "+crt_neighborhood);
				}catch(Exception e) {
					e.printStackTrace();
					break;
				}
				nGID.add(neigh_next);
				awn.add(crt_neighborhood);
				neigh_next = crt_neighborhood.parent_global_ID;
				nID = crt_neighborhood.parent_ID;
			}while((neigh_next!=null) && (nID!=null) && (_neighborhoods!=D_Constituent.EXPAND_ONE));
			neighborhood = awn.toArray(new D_Neighborhood[0]);
		return neighborhood;
	}

	public static String getGlobalID(String lID) throws P2PDDSQLException {
		String sql = "SELECT "+table.neighborhood.global_neighborhood_ID+" FROM "+table.neighborhood.TNAME+
		" WHERE "+table.neighborhood.neighborhood_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{lID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public static String getLocalID(String gID) throws P2PDDSQLException {
		String sql = "SELECT "+table.neighborhood.neighborhood_ID+" FROM "+table.neighborhood.TNAME+
		" WHERE "+table.neighborhood.global_neighborhood_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{gID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	
	/**
	 * 
	 * @param gID
	 * @param DBG
	 * @return
	 *  0 for absent,
	 *  1 for present&signed,
	 *  -1 for temporary
	 * @throws P2PDDSQLException
	 */
	public static int isGIDavailable(String gID, boolean DBG) throws P2PDDSQLException {
		String sql = 
			"SELECT "+table.neighborhood.neighborhood_ID+","+table.neighborhood.signature+
			" FROM "+table.neighborhood.TNAME+
			" WHERE "+table.neighborhood.global_neighborhood_ID+"=? ;";
			//" AND "+table.constituent.organization_ID+"=? "+
			//" AND ( "+table.constituent.sign + " IS NOT NULL " +
			//" OR "+table.constituent.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{gID}, DEBUG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_News:available: "+gID+" in "+" = "+result);
		if(a.size()==0) return 0;    
		String signature = Util.getString(a.get(0).get(1));
		if((signature!=null) && (signature.length()!=0)) return 1;
		return -1;
	}
	/**
	 * Gets IF for GID
	 * @param neig_GID
	 * @param existingNeighborhoodSigned : set to true if signed
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String getNeighborhoodLocalID(String neig_GID,
			boolean[] existingNeighborhoodSigned) throws P2PDDSQLException {
		if(DEBUG) System.out.println("D_Neighborhood:getNeighborhoodLocalID: "+neig_GID+" ?");
		if(neig_GID==null) {
			if(existingNeighborhoodSigned!=null) existingNeighborhoodSigned[0]= false;
			return null;
		}
		String sql = 
			"SELECT "+table.neighborhood.neighborhood_ID+","+table.neighborhood.signature+
			" FROM "+table.neighborhood.TNAME+
			" WHERE "+table.neighborhood.global_neighborhood_ID+"=? ;";
			//" AND "+table.constituent.organization_ID+"=? "+
			//" AND ( "+table.constituent.sign + " IS NOT NULL " +
			//" OR "+table.constituent.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{neig_GID}, DEBUG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG) System.out.println("D_Neighborhood:getNeighborhoodLocalID: "+neig_GID+" in "+" = "+result);
		existingNeighborhoodSigned[0]= false;
		if(a.size()==0) return null;    
		String signature = Util.getString(a.get(0).get(1));
		if((signature!=null) && (signature.length()!=0)) existingNeighborhoodSigned[0]= true;
		String id = Util.getString(a.get(0).get(0));
		return id;
	}

	public static boolean toggleBlock(long neighborhoodID) throws P2PDDSQLException {
		String sql = 
				"SELECT "+table.neighborhood.blocked+
				" FROM "+table.neighborhood.TNAME+
				" WHERE "+table.neighborhood.neighborhood_ID+"=?;";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{}, DEBUG);
		if(a.size()==0){
			Util.printCallPath("No such item");
			return false;
		}
		boolean result = !Util.stringInt2bool(a.get(0).get(0), false);
		Application.db.updateNoSync(table.neighborhood.TNAME,
				new String[]{table.neighborhood.blocked}, 
				new String[]{table.neighborhood.neighborhood_ID},
				new String[]{Util.bool2StringInt(result), Util.getStringID(neighborhoodID)}, DEBUG);
		return result;
	}
	public static boolean toggleBroadcast(long neighborhoodID) throws P2PDDSQLException {
		String sql = 
				"SELECT "+table.neighborhood.broadcasted+
				" FROM "+table.neighborhood.TNAME+
				" WHERE "+table.neighborhood.neighborhood_ID+"=?;";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{}, DEBUG);
		if(a.size()==0){
			Util.printCallPath("No such item");
			return false;
		}
		boolean result = !Util.stringInt2bool(a.get(0).get(0), false);
		Application.db.updateNoSync(table.neighborhood.TNAME,
				new String[]{table.neighborhood.broadcasted}, 
				new String[]{table.neighborhood.neighborhood_ID},
				new String[]{Util.bool2StringInt(result), Util.getStringID(neighborhoodID)}, DEBUG);
		return result;
	}

	private static void readSignSave(long i){
		D_Neighborhood w;
		try {
			w = new D_Neighborhood(i);
			ciphersuits.SK sk = util.Util.getStoredSK(w.submitter_global_ID);
			w.sign(sk);
			w.storeVerified();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
			Application.db = new util.DBInterface(Application.DELIBERATION_FILE);
			
			if(args.length>1){readSignSave(3); if(true) return;}
			long id = 1;
			if(args.length>0) id = Long.parseLong(args[0]);
			D_Neighborhood c=null;
			try {
				c = new D_Neighborhood(id);
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			if(!c.verifySignature()) System.out.println("\n************Signature Failure\n**********\nread="+c);
			else System.out.println("\n************Signature Pass\n**********\nread="+c);
			Decoder dec = new Decoder(c.getEncoder().getBytes());
			D_Neighborhood d=null;
			try {
				d = new D_Neighborhood().decode(dec);
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
			}
			Calendar arrival_date = d.arrival_date=Util.CalendargetInstance();
			//if(d.global_organization_ID==null) d.global_organization_ID = OrgHandling.getGlobalOrgID(d.organization_ID);
			if(!d.verifySignature()) System.out.println("\n************Signature Failure\n**********\nrec="+d);
			else System.out.println("\n************Signature Pass\n**********\nrec="+d);
			//return;
			d.global_neighborhood_ID = d.make_ID();
			//d.storeVerified(arrival_date);
			
			/*
			D_Witness w = new D_Witness();
			try {
				Fill_database.add_witness(1l);
			} catch (ASN1DecoderFail e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
			//getLeafNeighborhoods(args[0]);
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void zapp(long neighborhoodID2) {
		// TODO Auto-generated method stub
		try {
			Application.db.delete(table.neighborhood.TNAME,
					new String[]{table.neighborhood.neighborhood_ID},
					new String[]{Util.getStringID(neighborhoodID2)},
					DEBUG);
/*			Application.db.delete(table.constituent.TNAME,
					new String[]{table.constituent.neighborhood_ID},
					new String[]{Util.getStringID(neighborhoodID2)},
					DEBUG);
					*/
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
