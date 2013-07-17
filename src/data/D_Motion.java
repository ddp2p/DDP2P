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

import static java.lang.System.out;
import static util.Util._;
import hds.ASNSyncRequest;
import hds.Client;

import java.util.ArrayList;
import java.util.Calendar;

import ciphersuits.SK;

import util.P2PDDSQLException;

import config.Application;
import config.DD;

import streaming.ConstituentHandling;
import streaming.MotionHandling;
import streaming.NeighborhoodHandling;
import streaming.OrgHandling;
import streaming.RequestData;
import util.DBInterface;
import util.Util;


import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
D_MOTION ::= SEQUENCE {
	global_motionID PrintableString,
	motion_title Title_Document,
	motion_text Document
	constituent WB_Constituent
	date GeneralizedDate,
	signature OCTET_STRING
}
 */

public class D_Motion extends ASNObj implements util.Summary{
	private static final String V0 = "V0";
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int DEFAULT_STATUS = 0;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	public String hash_alg=V0;
	public String global_motionID;//Printable
	public D_Document_Title motion_title = new D_Document_Title();
	public D_Document motion_text = new D_Document();
	public String global_constituent_ID;//Printable
	public String global_enhanced_motionID;//Printable
	public String global_organization_ID;//Printable, does not enter encoding
	public int status;
	public D_MotionChoice[] choices;
	public Calendar creation_date;
	public byte[] signature; //OCT STR
	public D_Constituent constituent;
	public D_Motion enhanced;
	public D_Organization organization;
	
	public String motionID;
	public String constituent_ID;
	public String enhanced_motionID;
	public String organization_ID;
	public Calendar arrival_date;
	public boolean requested = false;
	public boolean blocked = false;
	public boolean broadcasted = D_Organization.DEFAULT_BROADCASTED_ORG;
	public String category;
	
	public D_Motion() {}
	public D_Motion(long motion_ID) throws P2PDDSQLException {
		if(motion_ID<=0) return;
		motionID = ""+motion_ID;
		String sql = 
			"SELECT "+Util.setDatabaseAlias(table.motion.fields,"m")+
			", c."+table.constituent.global_constituent_ID+
			", e."+table.motion.global_motion_ID+
			", o."+table.organization.global_organization_ID+
			" FROM "+table.motion.TNAME+" AS m "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=m."+table.motion.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS e ON(e."+table.motion.motion_ID+"=m."+table.motion.enhances_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE m."+table.motion.motion_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> m = Application.db.select(sql, new String[]{motionID}, DEBUG);
		if(m.size() == 0) return;
		init(m.get(0));
	}
	/**
	 * 
	 * @param motion_GID : global_motion_ID
	 * @throws P2PDDSQLException
	 */
	public D_Motion(String motion_GID) throws P2PDDSQLException {
		if(motion_GID == null) return;
		this.global_motionID = motion_GID;
		String sql = 
			"SELECT "+Util.setDatabaseAlias(table.motion.fields,"m")+
			", c."+table.constituent.global_constituent_ID+
			", e."+table.motion.global_motion_ID+
			", o."+table.organization.global_organization_ID+
			" FROM "+table.motion.TNAME+" AS m "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=m."+table.motion.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS e ON(e."+table.motion.motion_ID+"=m."+table.motion.enhances_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE m."+table.motion.global_motion_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> m = Application.db.select(sql, new String[]{motion_GID}, DEBUG);
		if(m.size() == 0) return;
		init(m.get(0));
	}
	/**
		String sql = 
			"SELECT "+Util.setDatabaseAlias(table.motion.fields,"m")+
			", c."+table.constituent.global_constituent_ID+
			", e."+table.motion.global_motion_ID+
			", o."+table.organization.global_organization_ID+
			" FROM "+table.motion.TNAME+" AS m "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=m."+table.motion.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS e ON(e."+table.motion.motion_ID+"=m."+table.motion.enhances_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
	 * 
	 * @param o
	 * @throws P2PDDSQLException
	 */
	public D_Motion(ArrayList<Object> o) throws P2PDDSQLException {
		init(o);
	}
	public D_Motion instance() throws CloneNotSupportedException{
		return new D_Motion();
	}
	void init(ArrayList<Object> o) throws P2PDDSQLException {
		hash_alg = Util.getString(o.get(table.motion.M_HASH_ALG));
		String _title_format = Util.getString(o.get(table.motion.M_TITLE_FORMAT));
		if(_title_format!=null){
			motion_title = new D_Document_Title();
			motion_title.title_document.setFormatString(_title_format);
		}
		
		String _motion_title = Util.getString(o.get(table.motion.M_TITLE));
		if(_motion_title!=null){
			if(motion_title == null) motion_title = new D_Document_Title();
			motion_title.title_document.setDocumentString(_motion_title);
		}
		
		String _text_format = Util.getString(o.get(table.motion.M_TEXT_FORMAT));
		if(_text_format != null){
			motion_text = new D_Document();
			motion_text.setFormatString(_text_format);
		}
		
		String _text = Util.getString(o.get(table.motion.M_TEXT));
		if(_text != null) {
			if(motion_text==null) motion_text = new D_Document();
			motion_text.setDocumentString(_text);
		}
		
		status = Util.ival(Util.getString(o.get(table.motion.M_STATUS)), DEFAULT_STATUS);
		creation_date = Util.getCalendar(Util.getString(o.get(table.motion.M_CREATION)));
		arrival_date = Util.getCalendar(Util.getString(o.get(table.motion.M_ARRIVAL)));
		signature = Util.byteSignatureFromString(Util.getString(o.get(table.motion.M_SIGNATURE)));
		motionID = Util.getString(o.get(table.motion.M_MOTION_ID));
		constituent_ID = Util.getString(o.get(table.motion.M_CONSTITUENT_ID));
		enhanced_motionID = Util.getString(o.get(table.motion.M_ENHANCED_ID));
		organization_ID = Util.getString(o.get(table.motion.M_ORG_ID));
		
		String db_choices = Util.getString(o.get(table.motion.M_CHOICES));
		String[] s_choices = D_OrgConcepts.stringArrayFromString(db_choices);
		choices = D_MotionChoice.getChoices(s_choices);
		//System.out.println("D_Motion:init:db_choices="+db_choices);
		//System.out.println("D_Motion:init: s_choices="+Util.concat(s_choices, ":"));
		//System.out.println("D_Motion:init: choices="+Util.concat(choices, ":"));
		
		
		this.category = Util.getString(o.get(table.motion.M_CATEGORY));		
		this.blocked = Util.stringInt2bool(o.get(table.motion.M_BLOCKED),false);
		this.requested = Util.stringInt2bool(o.get(table.motion.M_REQUESTED),false);
		this.broadcasted = Util.stringInt2bool(o.get(table.motion.M_BROADCASTED), D_Organization.DEFAULT_BROADCASTED_ORG_ITSELF);
		
		global_motionID = Util.getString(o.get(table.motion.M_MOTION_GID));
		global_constituent_ID = Util.getString(o.get(table.motion.M_FIELDS+0));
		global_enhanced_motionID = Util.getString(o.get(table.motion.M_FIELDS+1));
		global_organization_ID = Util.getString(o.get(table.motion.M_FIELDS+2));
		
		choices = D_MotionChoice.getChoices(motionID);
	}
	
	public String toString() {
		return "D_Motion: "+
		"\n hash_alg="+hash_alg+
		"\n global_motionID="+global_motionID+
		"\n motion_title="+Util.trimmed(motion_title.toString())+
		"\n motion_text="+Util.trimmed(motion_text.toString())+
		"\n constituent="+constituent+
		"\n global_constituent_ID="+global_constituent_ID+
		"\n global_enhanced_motionID="+global_enhanced_motionID+
		"\n global_organization_ID="+global_organization_ID+
		"\n status="+status+
		"\n choices="+Util.concat(choices, "::")+
		"\n creation_date="+Encoder.getGeneralizedTime(creation_date)+
		"\n signature="+Util.byteToHexDump(signature)+
		"\n motionID="+motionID+
		"\n constituent_ID="+constituent_ID+
		"\n enhanced_motionID="+enhanced_motionID+
		"\n organization_ID="+organization_ID;
	}
	public String toSummaryString() {
		return "[D_Motion:] "+
				"\n motion_title="+Util.trimmed(motion_title.toString())+
				"\n motion_text="+Util.trimmed(motion_text.toString());
	}
	public Encoder getSignableEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(global_motionID!=null)enc.addToSequence(new Encoder(global_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(motion_title!=null)enc.addToSequence(motion_title.getEncoder().setASN1Type(DD.TAG_AC2));
		if(motion_text!=null)enc.addToSequence(motion_text.getEncoder().setASN1Type(DD.TAG_AC3));
		if(category!=null)enc.addToSequence(new Encoder(category).setASN1Type(DD.TAG_AC11));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC5));
		if(global_enhanced_motionID!=null)enc.addToSequence(new Encoder(global_enhanced_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		if(global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC7));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC8));
		if(choices!=null)enc.addToSequence(Encoder.getEncoder(choices).setASN1Type(DD.TAG_AC10));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC9));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC4));
		return enc;
	}
	public Encoder getHashEncoder() {
		Encoder enc = new Encoder().initSequence();
		//if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		//if(global_motionID!=null)enc.addToSequence(new Encoder(global_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(motion_title!=null)enc.addToSequence(motion_title.getEncoder().setASN1Type(DD.TAG_AC2));
		if(motion_text!=null)enc.addToSequence(motion_text.getEncoder().setASN1Type(DD.TAG_AC3));
		if(category!=null)enc.addToSequence(new Encoder(category).setASN1Type(DD.TAG_AC11));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC5));
		if(global_enhanced_motionID!=null)enc.addToSequence(new Encoder(global_enhanced_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		if(global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC7));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC8));
		if(choices!=null)enc.addToSequence(Encoder.getEncoder(choices).setASN1Type(DD.TAG_AC10));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC9));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC4));
		return enc;
	}
	
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(global_motionID!=null)enc.addToSequence(new Encoder(global_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(motion_title!=null)enc.addToSequence(motion_title.getEncoder().setASN1Type(DD.TAG_AC2));
		if(motion_text!=null)enc.addToSequence(motion_text.getEncoder().setASN1Type(DD.TAG_AC3));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		if(global_enhanced_motionID!=null)enc.addToSequence(new Encoder(global_enhanced_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC5));
		if(global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC7));
		if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC8));
		if(choices!=null)enc.addToSequence(Encoder.getEncoder(choices).setASN1Type(DD.TAG_AC9));
		if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC10));
		if(enhanced!=null)enc.addToSequence(enhanced.getEncoder().setASN1Type(DD.TAG_AC11));
		if(organization!=null)enc.addToSequence(organization.getEncoder().setASN1Type(DD.TAG_AC12));
		if(category!=null)enc.addToSequence(new Encoder(category).setASN1Type(DD.TAG_AC13));
		return enc;
	}

	@Override
	public D_Motion decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0)hash_alg = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if(dec.getTypeByte()==DD.TAG_AC1)global_motionID = dec.getFirstObject(true).getString(DD.TAG_AC1);
		if(dec.getTypeByte()==DD.TAG_AC2)motion_title = new D_Document_Title().decode(dec.getFirstObject(true));	
		//System.out.println("D_Motion:decode: title="+motion_title);
		if(dec.getTypeByte()==DD.TAG_AC3)motion_text = new D_Document().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC4)global_constituent_ID = dec.getFirstObject(true).getString(DD.TAG_AC4);
		if(dec.getTypeByte()==DD.TAG_AC5)global_enhanced_motionID = dec.getFirstObject(true).getString(DD.TAG_AC5);
		if(dec.getTypeByte()==DD.TAG_AC6)global_organization_ID = dec.getFirstObject(true).getString(DD.TAG_AC6);
		if(dec.getTypeByte()==DD.TAG_AC7)creation_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC7);
		//else System.out.println("D_Motion:decode: no creation date");
		if(dec.getTypeByte()==DD.TAG_AC8)signature = dec.getFirstObject(true).getBytes(DD.TAG_AC8);
		//else System.out.println("D_Motion:decode: no signature");
		if(dec.getTypeByte()==DD.TAG_AC9)choices = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_SEQUENCE, new D_MotionChoice[0], new D_MotionChoice());	
		//else System.out.println("D_Motion:decode: no choices");
		if(dec.getTypeByte()==DD.TAG_AC10)constituent = new D_Constituent().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC11)enhanced = new D_Motion().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC12)organization = new D_Organization().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC13)category = dec.getFirstObject(true).getString(DD.TAG_AC13);
		return this;
	}
	public byte[] sign() {
		return sign(this.global_constituent_ID);
	}
	public byte[] sign(String signer_GID) {
		if(DEBUG) System.out.println("D_Motion:sign: start signer="+signer_GID);
		ciphersuits.SK sk = util.Util.getStoredSK(signer_GID);
		if(sk==null) {
			if(_DEBUG) System.out.println("D_Motion:sign: no signature secret key!");
			Application.warning(_("No secret key to sign motion, no constituent GID"), _("No Secret Key!"));
			return null;
		}
		if(DEBUG) System.out.println("D_Motion:sign: sign="+sk);

		return sign(sk);
	}
	/**
	 * Both store signature and returns it
	 * @param sk
	 * @return
	 */
	public byte[] sign(SK sk){
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("D_Motion:sign: this="+this+"\nsk="+sk);
		signature = Util.sign(this.getSignableEncoder().getBytes(), sk);
		if(DEBUG) System.out.println("D_Motion:sign:got this="+Util.byteToHexDump(signature));
		return signature;
	}
	public String make_ID(){
		try {
			fillGlobals();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		// return this.global_witness_ID =  
		return "M:"+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
	}
	public boolean verifySignature(){
		if(DEBUG) System.out.println("D_Motion:verifySignature: start");
		String pk_ID = this.global_constituent_ID;//.submitter_global_ID;
		
		if((pk_ID == null) && (this.constituent!=null) && (this.constituent.global_constituent_id!=null))
			pk_ID = this.constituent.global_constituent_id;
		if(pk_ID == null) return false;
		
		String newGID = make_ID();
		if(!newGID.equals(this.global_motionID)) {
			Util.printCallPath("D_Motion: WRONG EXTERNAL GID");
			if(DEBUG) System.out.println("D_Motion:verifySignature: WRONG HASH GID="+this.global_motionID+" vs="+newGID);
			if(DEBUG) System.out.println("D_Motion:verifySignature: WRONG HASH GID result="+false);
			System.err.println("D_Motion: "+this.toString());
			return false;
		}
		
		boolean result = Util.verifySignByID(this.getSignableEncoder().getBytes(), pk_ID, signature);
		if(DEBUG) System.out.println("WB_Witness:verifySignature: result wGID="+result);
		return result;
	}
	/**
	 * before call, one should set organization_ID and global_motionID
	 * @param rq
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long store(streaming.RequestData rq) throws P2PDDSQLException {
		boolean failure = false;
		boolean locals = fillLocals(rq, true, true, true);
		if(!locals) failure = true;

		if(!this.verifySignature())
			if(! DD.ACCEPT_UNSIGNED_DATA)
				failure = true;
		if(failure){
			if(this.motionID != null) return Util.lval(motionID, -1);
			return Util.lval(getMotionLocalID(this.global_motionID), -1);
		}
		String _old_date[] = new String[1];
		if ((this.motionID == null) && (this.global_motionID != null))
			this.motionID = getMotionLocalIDandDate(this.global_motionID,_old_date);
		if(this.motionID != null ) {
			String old_date = _old_date[0];//getDateFor(this.motionID);
			if(old_date != null) {
				String new_date = Encoder.getGeneralizedTime(this.creation_date);
				if((new_date==null) || new_date.compareTo(old_date)<=0) return new Integer(this.motionID).longValue();
			}
		}
		
		if((this.organization_ID == null ) && (this.global_organization_ID != null))
			this.organization_ID = D_Organization.getLocalOrgID_(this.global_organization_ID);
		if((this.organization_ID == null ) && (this.global_organization_ID != null)) {
			organization_ID = ""+data.D_Organization.insertTemporaryGID(global_organization_ID);
			rq.orgs.add(global_organization_ID);
		}
		
		if((this.constituent_ID == null ) && (this.global_constituent_ID != null))
			this.constituent_ID = D_Constituent.getConstituentLocalID(this.global_constituent_ID);
		if((this.constituent_ID == null ) && (this.global_constituent_ID != null)) {
			constituent_ID =
				""+D_Constituent.insertTemporaryConstituentGID(global_constituent_ID, this.organization_ID);
			rq.cons.put(global_constituent_ID,DD.EMPTYDATE);
		}

		if ((this.enhanced_motionID == null) && (this.global_enhanced_motionID != null))
			this.enhanced_motionID = getMotionLocalID(this.global_enhanced_motionID);
		if ((this.enhanced_motionID == null) && (this.global_enhanced_motionID != null)) {
			this.enhanced_motionID = ""+ insertTemporaryGID(global_enhanced_motionID, this.organization_ID);
			rq.moti.add(global_enhanced_motionID);
		}
		
		rq.moti.remove(this.global_motionID);
		
		return storeVerified();
	
	}
	public static long insertTemporaryGID(String motion_GID, String org_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("ConstituentHandling:insertTemporaryConstituentGID: start");
		//Util.printCallPath("temporary motion: "+motion_GID);
		return Application.db.insert(table.motion.TNAME,
				new String[]{table.motion.global_motion_ID, table.motion.organization_ID},
				new String[]{motion_GID, org_ID},
				DEBUG);
	}
	private static String getDateFor(String motionID) throws P2PDDSQLException {
		String sql = "SELECT "+table.motion.creation_date+" FROM "+table.motion.TNAME+
		" WHERE "+table.motion.motion_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{""+motionID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public boolean fillLocals(RequestData rq, boolean tempOrg, boolean default_blocked_org, boolean tempConst) throws P2PDDSQLException {
		if((global_organization_ID==null)&&(organization_ID == null)){
			Util.printCallPath("cannot store witness with not orgGID");
			return false;
		}
		if((this.global_constituent_ID==null)&&(constituent_ID == null)){
			Util.printCallPath("cannot store witness with not submitterGID");
			return false;
		}
		
		if((global_organization_ID!=null)&&(organization_ID == null)){
			organization_ID = Util.getStringID(D_Organization.getLocalOrgID(global_organization_ID));
			if(tempOrg && (organization_ID == null)) {
				String orgGID_hash = D_Organization.getOrgGIDHashGuess(global_organization_ID);
				if(rq!=null)rq.orgs.add(orgGID_hash);
				organization_ID = Util.getStringID(D_Organization.insertTemporaryGID(global_organization_ID, orgGID_hash, default_blocked_org));
				if(default_blocked_org) return false;
			}
			if(organization_ID == null) return false;
		}
			
		if((this.global_constituent_ID!=null)&&(constituent_ID == null)){
			this.constituent_ID = D_Constituent.getConstituentLocalID(global_constituent_ID);
			if(tempConst && (constituent_ID == null ))  {
				String consGID_hash = D_Constituent.getGIDHashFromGID(global_constituent_ID);
				if(rq!=null)rq.cons.put(consGID_hash,DD.EMPTYDATE);
				constituent_ID = Util.getStringID(D_Constituent.insertTemporaryConstituentGID(global_constituent_ID, organization_ID));
			}
			if(constituent_ID == null) return false;
		}
		return true;
	}
	private void fillGlobals() throws P2PDDSQLException {
		if((this.enhanced_motionID != null ) && (this.global_enhanced_motionID == null))
			this.global_enhanced_motionID = D_Motion.getMotionGlobalID(this.enhanced_motionID);
		
		if((this.organization_ID != null ) && (this.global_organization_ID == null))
			this.global_organization_ID = D_Organization.getGlobalOrgID(this.organization_ID);

		//if((this.motionID != null ) && (this.global_motionID == null)) this.global_motionID = D_Motion.getMotionGlobalID(this.motionID);
		
		if((this.constituent_ID != null ) && (this.global_constituent_ID == null))
			this.global_constituent_ID = D_Constituent.getConstituentGlobalID(this.constituent_ID);
	}
	public long storeVerified() throws P2PDDSQLException {
		Calendar now = Util.CalendargetInstance();
		return storeVerified(now, DEBUG);
	}
	public long storeVerified(boolean DEBUG) throws P2PDDSQLException {
		Calendar now = Util.CalendargetInstance();
		return storeVerified(now, DEBUG);
	}
	public long storeVerified(Calendar arrival_date) throws P2PDDSQLException {
		return storeVerified(arrival_date, DEBUG);
	}
	public long storeVerified(Calendar arrival_date, boolean DEBUG) throws P2PDDSQLException {
		long result = -1;
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("D_Motion:storeVerified: start arrival="+Encoder.getGeneralizedTime(arrival_date));
		if(this.constituent_ID == null )
			constituent_ID = D_Constituent.getConstituentLocalID(this.global_constituent_ID);
		if(constituent_ID == null){
			if(DEBUG) System.out.println("D_Motion:storeVerified: no signer!");
			return -1;
		}
		if((this.enhanced_motionID == null ) && (this.global_enhanced_motionID != null))
			this.enhanced_motionID = getMotionLocalID(this.global_enhanced_motionID);
		
		if((this.organization_ID == null ) && (this.global_organization_ID != null))
			this.organization_ID = D_Organization.getLocalOrgID_(this.global_organization_ID);

		if((this.motionID == null ) && (this.global_motionID != null))
			this.motionID = getMotionLocalID(this.global_motionID);
		
		if(DEBUG) System.out.println("D_Motion:storeVerified: fixed local="+this);
		
		String params[] = new String[table.motion.M_FIELDS];
		params[table.motion.M_MOTION_GID] = this.global_motionID;
		params[table.motion.M_HASH_ALG] = this.hash_alg;
		params[table.motion.M_TITLE_FORMAT] = this.motion_title.title_document.getFormatString();
		params[table.motion.M_TEXT_FORMAT] = this.motion_text.getFormatString();
		params[table.motion.M_TITLE] = this.motion_title.title_document.getDocumentString();
		params[table.motion.M_TEXT] = this.motion_text.getDocumentString();
		params[table.motion.M_ENHANCED_ID] = this.enhanced_motionID;
		params[table.motion.M_CONSTITUENT_ID] = this.constituent_ID;
		params[table.motion.M_ORG_ID] = organization_ID;
		params[table.motion.M_STATUS] = status+"";
		params[table.motion.M_CHOICES] = D_OrgConcepts.stringFromStringArray(D_MotionChoice.getNames(this.choices));
		params[table.motion.M_SIGNATURE] = Util.stringSignatureFromByte(signature);
		params[table.motion.M_CREATION] = Encoder.getGeneralizedTime(this.creation_date);
		params[table.motion.M_ARRIVAL] = Encoder.getGeneralizedTime(arrival_date);
		params[table.motion.M_CATEGORY] = this.category;
		params[table.motion.M_BLOCKED] = Util.bool2StringInt(blocked);
		params[table.motion.M_REQUESTED] = Util.bool2StringInt(requested);
		params[table.motion.M_BROADCASTED] = Util.bool2StringInt(broadcasted);
		if(this.motionID == null) {
			if(DEBUG) System.out.println("D_Motion:storeVerified:inserting");
			result = Application.db.insert(table.motion.TNAME,
					table.motion.fields_array,
					params,
					DEBUG
					);
			motionID=Util.getStringID(result);
		}else{
			if(DEBUG) System.out.println("D_Motion:storeVerified:inserting");
			params[table.motion.M_MOTION_ID] = motionID;
			Application.db.update(table.motion.TNAME,
					table.motion.fields_noID_array,
					new String[]{table.motion.motion_ID},
					params,
					DEBUG
					);
			result = Util.lval(this.motionID, -1);
		}
		Application.db.delete(table.motion_choice.TNAME, new String[]{table.motion_choice.motion_ID}, new String[]{result+""}, DEBUG);
		D_MotionChoice.save(choices, motionID);
		
		if((this.signature!=null) && (global_motionID != null))
			Client.payload_recent.add(streaming.RequestData.MOTI, this.global_motionID, D_Organization.getOrgGIDHashGuess(this.global_organization_ID), Client.MAX_ITEMS_PER_TYPE_PAYLOAD);

		return result;
	}
	/**
	 * update signature
	 * @param witness_ID
	 * @param signer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long readSignSave(long motion_ID, long signer_ID) throws P2PDDSQLException {
		D_Motion w=new D_Motion(motion_ID);
		ciphersuits.SK sk = util.Util.getStoredSK(D_Constituent.getConstituentGlobalID(""+signer_ID));
		w.sign(sk);
		return w.storeVerified();
	}
	private static String getMotionLocalIDandDate(String global_motionID,	String[] _old_date) throws P2PDDSQLException {
		String sql = "SELECT "+table.motion.motion_ID+","+table.motion.creation_date+" FROM "+table.motion.TNAME+
		" WHERE "+table.motion.global_motion_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{global_motionID}, DEBUG);
		if(o.size()==0) return null;
		_old_date[0] = Util.getString(o.get(0).get(1));
		return Util.getString(o.get(0).get(0));
	}
	public static String getMotionLocalID(String global_motionID) throws P2PDDSQLException {
		String sql = "SELECT "+table.motion.motion_ID+" FROM "+table.motion.TNAME+
		" WHERE "+table.motion.global_motion_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{global_motionID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public static String getMotionGlobalID(String motionID) throws P2PDDSQLException {
		String sql = "SELECT "+table.motion.global_motion_ID+" FROM "+table.motion.TNAME+
		" WHERE "+table.motion.motion_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{motionID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public static ArrayList<String> checkAvailability(ArrayList<String> cons,
			String orgID, boolean DBG) throws P2PDDSQLException {
		ArrayList<String> result = new ArrayList<String>();
		for (String hash : cons) {
			if(!available(hash, orgID, DBG)) result.add(hash);
		}
		return result;
	}
	/**
	 * check blocking at this level
	 * @param hash
	 * @param orgID
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static boolean available(String hash, String orgID, boolean DBG) throws P2PDDSQLException {
		String sql = 
			"SELECT "+table.motion.motion_ID+
			" FROM "+table.motion.TNAME+
			" WHERE "+table.motion.global_motion_ID+"=? "+
			" AND "+table.motion.organization_ID+"=? "+
			" AND ( "+table.motion.signature + " IS NOT NULL " +
			" OR "+table.motion.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{hash, orgID}, DEBUG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_Motion:available: "+hash+" in "+orgID+" = "+result);
		return result;
	}

	public static void main(String[] args) {
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			if(args.length>0){readSignSave(3,1); if(true) return;}
			
			D_Motion c=new D_Motion(3);
			if(!c.verifySignature()) System.out.println("\n************Signature Failure\n**********\nread="+c);
			else System.out.println("\n************Signature Pass\n**********\nread="+c);
			Decoder dec = new Decoder(c.getEncoder().getBytes());
			D_Motion d = new D_Motion().decode(dec);
			Calendar arrival_date = d.arrival_date=Util.CalendargetInstance();
			//if(d.global_organization_ID==null) d.global_organization_ID = OrgHandling.getGlobalOrgID(d.organization_ID);
			if(!d.verifySignature()) System.out.println("\n************Signature Failure\n**********\nrec="+d);
			else System.out.println("\n************Signature Pass\n**********\nrec="+d);
			d.global_motionID = d.make_ID();
			d.storeVerified(arrival_date);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static byte getASN1Type() {
		return TAG;
	}
	public void setEditable() {
		signature = null;
		this.global_motionID = null;
	}
	public boolean isEditable() {
		if(signature == null){
			if(DEBUG) out.println("D_Motion:editable: no sign");
			return true;
		}
		if(this.global_motionID == null){
			if(DEBUG) out.println("D_Motion:editable: no GID");
			return true;
		}
		if(!verifySignature()) {
			if(_DEBUG) out.println("D_Motion:editable: no verifiable signature");
			signature = null;
			try {
				storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return true;			
		}
		return false;
	}
	public String getDefaultChoice() {
		if((this.choices!=null)&&(this.choices.length>0)) return this.choices[0].short_name;
		return "0";
	}
	public void changeToDefaultEnhancement() {
		this.enhanced_motionID = motionID;
		this.global_enhanced_motionID = this.global_motionID;
		this.motionID = null;
		this.global_motionID = null;
		this.signature = null;
		this.constituent_ID = null;
		this.global_constituent_ID = null;
		this.motion_title.title_document.setDocumentString(_("Answer To:")+" "+this.motion_title.title_document.getDocumentUTFString());
		this.motion_text.setDocumentString(_("Answer To:")+" \n"+this.motion_text.getDocumentUTFString());
		this.creation_date = this.arrival_date = Util.CalendargetInstance();
		this.requested = this.blocked = this.broadcasted = false;		
	}
}
