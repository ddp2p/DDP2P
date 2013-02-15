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

import handling_wb.BroadcastQueueHandled;
import handling_wb.PreparedMessage;
import hds.Client;

import java.util.ArrayList;
import java.util.Calendar;

import streaming.ConstituentHandling;
import streaming.JustificationHandling;
import streaming.MotionHandling;
import streaming.OrgHandling;
import streaming.RequestData;
import util.DBInterface;
import util.Util;
import wireless.BroadcastClient;

import ciphersuits.SK;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;


import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
/**
WB_VOTE ::= SEQUENCE {
    global_vote_ID PrintableString,
	choice UTF8String,
	voter WB_Constituent,
	date GeneralizedDate,
	justification WB_JUSTIFICATION,
	signature OCTET_STRING
}
 */
public
class D_Vote extends ASNObj{
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final String V0 = "0";
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	public String hash_alg = V0;
	public String global_vote_ID; //Printable
	public String global_constituent_ID; //Printable
	public String global_motion_ID; //Printable
	public String global_justification_ID; //Printable
	public String global_organization_ID; //Printable
	public String choice;//UTF8
	public String format;//UTF8
	public Calendar creation_date;
	public byte[] signature; //OCT STR
	public Calendar arrival_date;
	
	public D_Constituent constituent;
	public D_Motion motion;
	public D_Justification justification;
	
	public String vote_ID;
	public String constituent_ID;
	public String justification_ID;
	public String motion_ID;
	public String organization_ID;

	public D_Vote() {}
	public D_Vote(long _vote_ID) throws SQLiteException {
		if(_vote_ID<=0) return;
		vote_ID = Util.getStringID(_vote_ID);
		String sql = 
			"SELECT "+Util.setDatabaseAlias(table.signature.fields,"v")+
			", c."+table.constituent.global_constituent_ID+
			", m."+table.motion.global_motion_ID+
			", j."+table.justification.global_justification_ID+
			", o."+table.organization.global_organization_ID+
			", o."+table.organization.organization_ID+
			" FROM "+table.signature.TNAME+" AS v "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"+
			" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE v."+table.signature.signature_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> v = Application.db.select(sql, new String[]{vote_ID}, DEBUG);
		if(v.size() == 0) return;
		init(v.get(0));
	}
	public D_Vote(String vote_GID) throws SQLiteException {
		if(vote_GID == null) return;
		this.global_vote_ID = vote_GID;
		String sql = 
			"SELECT "+Util.setDatabaseAlias(table.signature.fields,"v")+
			", c."+table.constituent.global_constituent_ID+
			", m."+table.motion.global_motion_ID+
			", j."+table.justification.global_justification_ID+
			", o."+table.organization.global_organization_ID+
			", o."+table.organization.organization_ID+
			" FROM "+table.signature.TNAME+" AS v "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"+
			" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE v."+table.signature.global_signature_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> v = Application.db.select(sql, new String[]{vote_GID}, DEBUG);
		if(v.size() == 0) return;
		init(v.get(0));
	}
	public static D_Vote getMyVoteForMotion(String motionID) throws SQLiteException {
		D_Vote v = new D_Vote();
		if(motionID == null) return null;
		v.motion_ID = motionID;
		String sql = 
			"SELECT "+Util.setDatabaseAlias(table.signature.fields,"v")+
			", c."+table.constituent.global_constituent_ID+
			", m."+table.motion.global_motion_ID+
			", j."+table.justification.global_justification_ID+
			", o."+table.organization.global_organization_ID+
			", o."+table.organization.organization_ID+
			" FROM "+table.signature.TNAME+" AS v "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"+
			" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE v."+table.signature.motion_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> _v = Application.db.select(sql, new String[]{motionID}, DEBUG);
		if(_v.size() == 0) return null;
		v.init(_v.get(0));
		return v;
	}

	/**
			"SELECT "+Util.setDatabaseAlias(table.signature.fields,"v")+
			", c."+table.constituent.global_constituent_ID+
			", m."+table.motion.global_motion_ID+
			", j."+table.justification.global_justification_ID+
			", o."+table.organization.global_organization_ID+
			", o."+table.organization.organization_ID+
			" FROM "+table.signature.TNAME+" AS v "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"+
			" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
	 * 
	 * @param o
	 * @throws SQLiteException
	 */
	public D_Vote(ArrayList<Object> o) throws SQLiteException {
		init(o);
	}
	public D_Vote instance() throws CloneNotSupportedException{
		return new D_Vote();
	}
	/**
			"SELECT "+Util.setDatabaseAlias(table.signature.fields,"v")+
			", c."+table.constituent.global_constituent_ID+
			", m."+table.motion.global_motion_ID+
			", j."+table.justification.global_justification_ID+
			", o."+table.organization.global_organization_ID+
			", o."+table.organization.organization_ID+
			" FROM "+table.signature.TNAME+" AS v "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"+
			" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
	 * 
	 * @param o
	 * @throws SQLiteException
	 */
	public void init(ArrayList<Object> o) throws SQLiteException {
		this.hash_alg = Util.getString(o.get(table.signature.S_HASH_ALG));
		this.global_vote_ID = Util.getString(o.get(table.signature.S_GID));
		this.creation_date = Util.getCalendar(Util.getString(o.get(table.signature.S_CREATION)));
		this.arrival_date = Util.getCalendar(Util.getString(o.get(table.signature.S_ARRIVAL)));
		this.signature = Util.byteSignatureFromString(Util.getString(o.get(table.signature.S_SIGNATURE)));
		this.choice = Util.getString(o.get(table.signature.S_CHOICE));
		this.format = Util.getString(o.get(table.signature.S_FORMAT));
		this.motion_ID = Util.getString(o.get(table.signature.S_MOTION_ID));
		this.constituent_ID = Util.getString(o.get(table.signature.S_CONSTITUENT_ID));
		this.justification_ID = Util.getString(o.get(table.signature.S_JUSTIFICATION_ID));
		this.vote_ID = Util.getString(o.get(table.signature.S_ID));
		
		this.global_constituent_ID = Util.getString(o.get(table.signature.S_FIELDS+0));
		this.global_motion_ID = Util.getString(o.get(table.signature.S_FIELDS+1));
		this.global_justification_ID = Util.getString(o.get(table.signature.S_FIELDS+2));
		this.global_organization_ID = Util.getString(o.get(table.signature.S_FIELDS+3));
		this.organization_ID = Util.getString(o.get(table.signature.S_FIELDS+4));
		
		//this.choices = WB_Choice.getChoices(motionID);
	}
	public void init_all(ArrayList<Object> v) throws Exception {
		if(DEBUG)System.out.println("D_Vote:init_all: start");
		try{
			init(v);
		}catch(Exception e){e.printStackTrace();}
		if(DEBUG)System.out.println("D_Vote:init_all: contained objects");
		if(constituent_ID!=null)this.constituent = new D_Constituent(Integer.parseInt(this.constituent_ID));
		if(global_motion_ID!=null) this.motion = new D_Motion(this.global_motion_ID);
		if(global_justification_ID!=null) this.justification = new D_Justification(this.global_justification_ID);
		if(DEBUG)System.out.println("D_Vote:init_all: done");
	}

	public Encoder getSignableEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(global_vote_ID!=null)enc.addToSequence(new Encoder(global_vote_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(global_motion_ID!=null)enc.addToSequence(new Encoder(global_motion_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(global_justification_ID!=null)enc.addToSequence(new Encoder(global_justification_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		if(choice!=null)enc.addToSequence(new Encoder(choice,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC5));
		if(format!=null)enc.addToSequence(new Encoder(format,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC6));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC7));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC8));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC9));
		//if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		//if(justification!=null) enc.addToSequence(justification.getEncoder().setASN1Type(DD.TAG_AC11));
		return enc;
	}
	
	public Encoder getHashEncoder() {
		Encoder enc = new Encoder().initSequence();
		//if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		//if(global_vote_ID!=null)enc.addToSequence(new Encoder(global_vote_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(global_motion_ID!=null)enc.addToSequence(new Encoder(global_motion_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		//if(global_justification_ID!=null)enc.addToSequence(new Encoder(global_justification_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		//if(choice!=null)enc.addToSequence(new Encoder(choice,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC5));
		//if(format!=null)enc.addToSequence(new Encoder(format,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC6));
		//if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC7));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC8));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC9));
		//if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		//if(justification!=null) enc.addToSequence(justification.getEncoder().setASN1Type(DD.TAG_AC11));
		return enc;
	}	
	
	private Encoder getEntityEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(global_vote_ID!=null)enc.addToSequence(new Encoder(global_vote_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(global_motion_ID!=null)enc.addToSequence(new Encoder(global_motion_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(global_justification_ID!=null)enc.addToSequence(new Encoder(global_justification_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		if(choice!=null)enc.addToSequence(new Encoder(choice,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC5));
		if(format!=null)enc.addToSequence(new Encoder(format,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC6));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC7));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC8));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC9));
		//if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		//if(justification!=null) enc.addToSequence(justification.getEncoder().setASN1Type(DD.TAG_AC11));
		if(global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		return enc;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(global_vote_ID!=null)enc.addToSequence(new Encoder(global_vote_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(global_motion_ID!=null)enc.addToSequence(new Encoder(global_motion_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(global_justification_ID!=null)enc.addToSequence(new Encoder(global_justification_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		if(choice!=null)enc.addToSequence(new Encoder(choice,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC5));
		if(format!=null)enc.addToSequence(new Encoder(format,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC6));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC7));
		if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC8));
		if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC9));
		if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		if(justification!=null) enc.addToSequence(justification.getEncoder().setASN1Type(DD.TAG_AC11));
		if(global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		return enc;
	}

	@Override
	public D_Vote decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0) hash_alg = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if(dec.getTypeByte()==DD.TAG_AC1) global_vote_ID = dec.getFirstObject(true).getString(DD.TAG_AC1);
		if(dec.getTypeByte()==DD.TAG_AC2) global_constituent_ID = dec.getFirstObject(true).getString(DD.TAG_AC2);
		if(dec.getTypeByte()==DD.TAG_AC3) global_motion_ID = dec.getFirstObject(true).getString(DD.TAG_AC3);
		if(dec.getTypeByte()==DD.TAG_AC4) global_justification_ID = dec.getFirstObject(true).getString(DD.TAG_AC4);
		if(dec.getTypeByte()==DD.TAG_AC5) choice = dec.getFirstObject(true).getString(DD.TAG_AC5);
		if(dec.getTypeByte()==DD.TAG_AC6) format = dec.getFirstObject(true).getString(DD.TAG_AC6);
		if(dec.getTypeByte()==DD.TAG_AC7)  creation_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC7);
		if(dec.getTypeByte()==DD.TAG_AC8)  signature = dec.getFirstObject(true).getBytes(DD.TAG_AC8);
		if(dec.getTypeByte()==DD.TAG_AC9)  constituent = new D_Constituent().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC10) motion = new D_Motion().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC11) justification = new D_Justification().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC12) global_organization_ID = dec.getFirstObject(true).getString(DD.TAG_AC12);
		return this;
	}	
	
	public String toString() {
		return "WB_Vote:" +
				"\n hash_alg="+hash_alg+
				"\n global_vote_id="+global_vote_ID+
				"\n global_constituent_id="+global_constituent_ID+
				"\n global_motion_id="+global_motion_ID+
				"\n global_justification_id="+global_justification_ID+
				"\n global_organization_ID="+global_organization_ID+
				"\n choice="+choice+
				"\n format="+format+
				"\n creation_date="+Encoder.getGeneralizedTime(creation_date)+
				"\n signature="+Util.byteToHexDump(signature)+
				"\n arrival_date="+Encoder.getGeneralizedTime(arrival_date)+
				
				"\n vote_ID="+vote_ID+
				"\n constituent_ID="+constituent_ID+
				"\n justification_ID="+justification_ID+
				"\n motion_ID="+motion_ID+
				"\n organization_ID="+organization_ID+
				
				"\n voter="+constituent+
				"\n motion="+motion+
				"\n justification="+justification;
	}
	public void sign() {
		sign(this.global_constituent_ID);
	}
	public byte[] sign(String signer_GID) {
		if(DEBUG) System.out.println("WB_Vote:sign: start signer="+signer_GID);
		ciphersuits.SK sk = util.Util.getStoredSK(signer_GID);
		if(sk==null) 
			if(_DEBUG) System.out.println("WB_Vote:sign: no signature");
		if(DEBUG) System.out.println("WB_Vote:sign: sign="+sk);

		return sign(sk);
	}
	/**
	 * Both store signature and returns it
	 * @param sk
	 * @return
	 */
	public byte[] sign(SK sk){
		if(DEBUG) System.out.println("WB_Vote:sign: this="+this+"\nsk="+sk);
		signature = Util.sign(this.getSignableEncoder().getBytes(), sk);
		if(DEBUG) System.out.println("WB_Vote:sign:got this="+Util.byteToHexDump(signature));
		return signature;
	}
	public String make_ID() {
		if(DEBUG) System.out.println("WB_Vote:makeID: start");
		try {
			fillGlobals();
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		if(DEBUG) System.out.println("WB_Vote:makeID: id of "+this);
		// return this.global_witness_ID =  
		String result = "V:"+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
		if(DEBUG) System.out.println("WB_Vote:makeID: id = "+result);
		return result;
	}
	public boolean verifySignature(){
		if(DEBUG) System.out.println("WB_Vote:verifySignature: start");
		String pk_ID = this.global_constituent_ID;//.submitter_global_ID;
		if((pk_ID == null) && (this.constituent!=null) && (this.constituent.global_constituent_id!=null))
			pk_ID = this.constituent.global_constituent_id;
		if(pk_ID == null) return false;
		
		String newGID = make_ID();
		if(!newGID.equals(this.global_vote_ID)) {
			Util.printCallPath("WB_Vote: WRONG EXTERNAL GID");
			if(DEBUG) System.out.println("WB_Vote:verifySignature: WRONG HASH GID="+this.global_vote_ID+" vs="+newGID);
			if(DEBUG) System.out.println("WB_Vote:verifySignature: WRONG HASH GID result="+false);
			return false;
		}
		
		boolean result = Util.verifySignByID(this.getSignableEncoder().getBytes(), pk_ID, signature);
		if(DEBUG) System.out.println("WB_Vote:verifySignature: result wGID="+result);
		return result;
	}
	private boolean hashConflictCreationDateDropThis() throws SQLiteException {
		String this_hash=new String(this.getEntityEncoder().getBytes());
		String old = new String(new D_Vote(this.global_vote_ID).getEntityEncoder().getBytes());
		if(old.compareTo(this_hash)>=0){
			if(DEBUG) System.out.println("D_Vote:store: signature hashConflictCreationDateDropThis true");
			return true;
		}
		if(DEBUG) System.out.println("D_Vote:store: signature hashConflictCreationDateDropThis false");
		return false;
	}
	/**
	 * before call, one should set organization_ID and global_motionID
	 * @param rq
	 * @return
	 * @throws SQLiteException
	 */
	public long store(streaming.RequestData rq) throws SQLiteException {
		return store(null, rq);
	}
	public long store(PreparedMessage pm, streaming.RequestData rq) throws SQLiteException {
		if(DEBUG) System.out.println("D_Vote:store: signature start");
		
		boolean locals = fillLocals(rq, true, true, true, true, true);
		if(!locals) return -1;
		
		if(!this.verifySignature()){
			if(_DEBUG) System.out.println("D_Vote:store: signature test failure="+this);
			if(! DD.ACCEPT_UNSIGNED_DATA)
				if(_DEBUG) System.out.println("D_Vote:store: signature test quit");
				return -1;
		}
		if(DEBUG) System.out.println("D_Vote:store: signature storing");

		String _old_date[] = new String[1];
		if ((this.vote_ID == null) && (this.global_vote_ID != null))
			this.vote_ID = getLocalIDandDateforGID(this.global_vote_ID,_old_date);
		if(this.vote_ID != null ) {
			String old_date = _old_date[0];//getDateFor(this.vote_ID);
			if(old_date != null) {
				String new_date = Encoder.getGeneralizedTime(this.creation_date);
				if(new_date.compareTo(old_date) < 0) return new Integer(vote_ID).longValue();
				if(new_date.compareTo(old_date)==0) {
					if(hashConflictCreationDateDropThis()) {
						if(DEBUG) System.out.println("D_Vote:store: signature hashConflictCreationDateDropThis quit");
						return new Integer(vote_ID).longValue();
					}
				}
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
				Util.getStringID(D_Constituent.insertTemporaryConstituentGID(global_constituent_ID, this.organization_ID));
			rq.cons.put(global_constituent_ID,DD.EMPTYDATE);
		}
		
		if ((this.motion_ID == null) && (this.global_motion_ID != null))
			this.motion_ID = D_Motion.getMotionLocalID(this.global_motion_ID);
		if ((this.motion_ID == null) && (this.global_motion_ID != null)) {
			this.motion_ID = Util.getStringID(D_Motion.insertTemporaryGID(global_motion_ID, this.organization_ID));
			rq.moti.add(global_motion_ID);
		}

		if ((this.justification_ID == null) && (this.global_justification_ID != null))
			this.justification_ID = JustificationHandling.getJustificationLocalID(this.global_justification_ID);
		if ((this.justification_ID == null) && (this.global_justification_ID != null)) {
			this.justification_ID = Util.getStringID(D_Justification.insertTemporaryGID(global_justification_ID, this.motion_ID));
			rq.just.add(global_justification_ID);
		}

		rq.sign.remove(this.global_vote_ID);
		
		return storePMVerified(pm);
	
	}
	private static String getLocalIDandDateforGID(String global_vote_ID, String[]_date) throws SQLiteException {
		if(DEBUG) System.out.println("WB_Vote:getLocalIDforGID: start");
		if(global_vote_ID==null) return null;
		String sql = "SELECT "+table.signature.signature_ID+","+table.signature.creation_date+
		" FROM "+table.signature.TNAME+
		" WHERE "+table.signature.global_signature_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.db.select(sql, new String[]{global_vote_ID}, DEBUG);
		if(n.size()==0) return null;
		_date[0] = Util.getString(n.get(0).get(1));
		return Util.getString(n.get(0).get(0));
	}
	private static String getLocalIDforGID(String global_vote_ID) throws SQLiteException {
		if(DEBUG) System.out.println("WB_Vote:getLocalIDforGID: start");
		if(global_vote_ID==null) return null;
		String sql = "SELECT "+table.signature.signature_ID+
		" FROM "+table.signature.TNAME+
		" WHERE "+table.signature.global_signature_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.db.select(sql, new String[]{global_vote_ID}, DEBUG);
		if(n.size()==0) return null;
		return Util.getString(n.get(0).get(0));
	}
	public static long insertTemporaryGID(String sign_GID, String mot_ID) throws SQLiteException {
		if(DEBUG) System.out.println("WB_Vote:insertTemporaryGID: start");
		return Application.db.insert(table.signature.TNAME,
				new String[]{table.signature.global_signature_ID, table.signature.motion_ID},
				new String[]{sign_GID, mot_ID},
				DEBUG);
	}
	private static String getDateFor(String signID) throws SQLiteException {
		String sql = "SELECT "+table.signature.creation_date+" FROM "+table.signature.TNAME+
		" WHERE "+table.signature.signature_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{""+signID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public boolean fillLocals(RequestData rq, boolean tempOrg, boolean default_blocked_org, boolean tempConst, boolean tempMotion, boolean tempJust) throws SQLiteException {
	
		if((global_organization_ID==null)&&(organization_ID == null)){
			Util.printCallPath("cannot store witness with not orgGID");
			return false;
		}
		
		if((this.global_constituent_ID==null)&&(constituent_ID == null)){
			Util.printCallPath("cannot store witness with not submitterGID");
			return false;
		}
		if((this.global_motion_ID==null)&&(motion_ID == null)){
			Util.printCallPath("cannot store just with no motionGID");
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
		
		if((this.global_motion_ID!=null)&&(motion_ID == null)){
			this.motion_ID = D_Motion.getMotionLocalID(global_motion_ID);
			if(tempMotion && (motion_ID == null ))  {
				if(rq!=null)rq.moti.add(global_motion_ID);
				motion_ID = Util.getStringID(D_Motion.insertTemporaryGID(global_motion_ID, organization_ID));
			}
			if(motion_ID == null) return false;
		}
		
		if((this.global_justification_ID!=null)&&(justification_ID == null)){
			this.justification_ID = D_Justification.getLocalID(global_justification_ID);
			if(tempJust && (justification_ID == null ))  {
				if(rq!=null)rq.just.add(global_justification_ID);
				justification_ID = Util.getStringID(D_Justification.insertTemporaryGID(global_justification_ID, organization_ID));
			}
			if(justification_ID == null) return false;
		}
		return true;
	}
	private void fillGlobals() throws SQLiteException {
		if((this.organization_ID != null ) && (this.global_organization_ID == null))
			this.global_organization_ID = D_Organization.getGlobalOrgID(this.organization_ID);

		if((this.justification_ID != null ) && (this.global_justification_ID == null))
			this.global_justification_ID = D_Justification.getGlobalID(this.justification_ID);
		
		if((this.motion_ID != null ) && (this.global_motion_ID == null))
			this.global_motion_ID = D_Motion.getMotionGlobalID(this.motion_ID);
		
		if((this.constituent_ID != null ) && (this.global_constituent_ID == null))
			this.global_constituent_ID = D_Constituent.getConstituentGlobalID(this.constituent_ID);
	}
	public long storeVerified() throws SQLiteException {
		return storePMVerified(null);
	}
	public long storePMVerified(PreparedMessage pm) throws SQLiteException {
		Calendar now = Util.CalendargetInstance();
		return storePMVerified(pm, now);
	}
	public long storeVerified(Calendar arrival_date) throws SQLiteException {
		return storePMVerified(null, arrival_date);
	}
	public long storePMVerified(PreparedMessage pm, Calendar arrival_date) throws SQLiteException {
		//boolean DEBUG = true;
		long result = -1;
		if(DEBUG) System.out.println("WB_Vote:storeVerified: start arrival="+Encoder.getGeneralizedTime(arrival_date));
		if(this.constituent_ID == null )
			constituent_ID = D_Constituent.getConstituentLocalID(this.global_constituent_ID);
		if(constituent_ID == null){
			if(DEBUG) System.out.println("WB_Vote:storeVerified: no signer!");
			return -1;
		}
		if((this.justification_ID == null ) && (this.global_justification_ID != null))
			this.justification_ID = JustificationHandling.getJustificationLocalID(this.global_justification_ID);
		
		if((this.organization_ID == null ) && (this.global_organization_ID != null))
			this.organization_ID = D_Organization.getLocalOrgID_(this.global_organization_ID);

		if((this.motion_ID == null ) && (this.global_motion_ID != null))
			this.motion_ID = D_Motion.getMotionLocalID(this.global_motion_ID);
		
		if((this.vote_ID == null ) && (this.global_vote_ID != null))
			this.vote_ID = D_Vote.getSignatureLocalID(this.global_vote_ID);
		
		if(DEBUG) System.out.println("WB_Vote:storeVerified: fixed local="+this);
		
		String params[] = new String[table.signature.S_FIELDS];
		params[table.signature.S_HASH_ALG] = this.hash_alg;
		params[table.signature.S_GID] = this.global_vote_ID;
		params[table.signature.S_CONSTITUENT_ID] = this.constituent_ID;
		params[table.signature.S_JUSTIFICATION_ID] = this.justification_ID;
		params[table.signature.S_MOTION_ID] = this.motion_ID;
		params[table.signature.S_SIGNATURE] = Util.stringSignatureFromByte(signature);
		params[table.signature.S_CHOICE] = this.choice;
		params[table.signature.S_FORMAT] = this.format;
		params[table.signature.S_CREATION] = Encoder.getGeneralizedTime(this.creation_date);
		params[table.signature.S_ARRIVAL] = Encoder.getGeneralizedTime(arrival_date);
		if(this.vote_ID == null) {
			if(DEBUG) System.out.println("WB_Vote:storeVerified:inserting");
			result = Application.db.insert(table.signature.TNAME,
					table.signature.fields_array,
					params,
					DEBUG
					);
			vote_ID=""+result;
		}else{
			if(DEBUG) System.out.println("WB_Vote:storeVerified:inserting");
			params[table.signature.S_ID] = vote_ID;
			Application.db.update(table.signature.TNAME,
					table.signature.fields_noID_array,
					new String[]{table.signature.signature_ID},
					params,
					DEBUG
					);
			result = Util.lval(this.vote_ID, -1);
		}
		synchronized(BroadcastClient.msgs_monitor) {
			if(BroadcastClient.msgs!=null) {
				D_Message dm = new D_Message();
				if((this.constituent_ID != null ) && (this.global_constituent_ID == null))
					this.global_constituent_ID = D_Constituent.getConstituentGlobalID(this.constituent_ID);
				if((this.motion_ID != null ) && (this.global_motion_ID == null))
					this.global_motion_ID = D_Motion.getMotionGlobalID(this.motion_ID);
				if((this.justification_ID != null ) && (this.global_justification_ID == null))
					this.global_justification_ID = JustificationHandling.getJustificationLocalID(this.justification_ID);
				if((this.organization_ID != null ) && (this.global_organization_ID == null))
					this.global_organization_ID = D_Organization.getGlobalOrgID(this.organization_ID);
				dm.vote = this; // may have to add GIDs
				dm.sender = new D_PeerAddress();
				dm.sender.globalID = DD.getMyPeerGIDFromIdentity(); //DD.getAppText(DD.APP_my_global_peer_ID);
				dm.sender.name = DD.getAppText(DD.APP_my_peer_name);

				if((this.signature!=null) && (global_vote_ID != null)) {
					if(pm != null) {
						if(pm.raw == null)pm.raw = dm.encode();
						if(pm.motion_ID == null)pm.motion_ID=this.motion.global_motionID;
						if(this.constituent.global_constituent_id_hash != null)pm.constituent_ID_hash.add(this.constituent.global_constituent_id_hash);
						if(this.justification.global_justificationID != null)pm.justification_ID = this.justification.global_justificationID;
						if(this.global_organization_ID !=null)pm.org_ID_hash = this.global_organization_ID;
					
						BroadcastClient.msgs.registerRecent(pm, BroadcastQueueHandled.VOTE);
					}
					Client.payload_recent.add(streaming.RequestData.SIGN, this.global_vote_ID, D_Organization.getOrgGIDHashGuess(this.global_organization_ID), Client.MAX_ITEMS_PER_TYPE_PAYLOAD);
				}
			}
		}
		return result;
	}
	/**
	 * update signature
	 * @param witness_ID
	 * @param signer_ID
	 * @return
	 * @throws SQLiteException
	 */
	public static long readSignSave(long vote_ID, long signer_ID) throws SQLiteException {
		D_Vote w=new D_Vote(vote_ID);
		ciphersuits.SK sk = util.Util.getStoredSK(D_Constituent.getConstituentGlobalID(""+signer_ID));
		w.sign(sk);
		return w.storeVerified();
	}
	public static String getSignatureLocalID(String global_vote_ID) throws SQLiteException {
		String sql = "SELECT "+table.signature.signature_ID+" FROM "+table.signature.TNAME+
		" WHERE "+table.signature.global_signature_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{global_vote_ID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}

	public static void main(String[] args) {
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			if(args.length>0){readSignSave(3,1); if(true) return;}
			
			D_Vote c=new D_Vote(1);
			if(!c.verifySignature()) System.out.println("\n************Signature Failure\n**********\nread="+c);
			else System.out.println("\n************Signature Pass\n**********\nread="+c);
			Decoder dec = new Decoder(c.getEncoder().getBytes());
			D_Vote d = new D_Vote().decode(dec);
			Calendar arrival_date = d.arrival_date=Util.CalendargetInstance();
			//if(d.global_organization_ID==null) d.global_organization_ID = OrgHandling.getGlobalOrgID(d.organization_ID);
			if(!d.verifySignature()) System.out.println("\n************Signature Failure\n**********\nrec="+d);
			else System.out.println("\n************Signature Pass\n**********\nrec="+d);
			d.global_vote_ID = d.make_ID();
			//d.storeVerified(arrival_date);
			if(_DEBUG) out.println("D_Vote:editable: ID="+d.global_vote_ID);
		} catch (SQLiteException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public boolean setEditable() {
		if(signature == null){
			if(DEBUG) out.println("D_Vote:editable: no sign");
			return true;
		}
		if(this.global_vote_ID == null){
			if(DEBUG) out.println("D_Vote:editable: no GID");
			return true;
		}
		return false;
	}
	public static ArrayList<String> checkAvailability(ArrayList<String> hashes,
			String orgID, boolean DBG) throws SQLiteException {
		ArrayList<String> result = new ArrayList<String>();
		for (String hash : hashes) {
			if(!available(hash, orgID, DBG)) result.add(hash);
		}
		return result;
	}
	/**
	 * check blocking at this level
	 * @param hash
	 * @param orgID
	 * @return
	 * @throws SQLiteException
	 */
	public static boolean available(String hash, String orgID, boolean DBG) throws SQLiteException {
		boolean result = true;
		String sql = 
			"SELECT "+table.signature.signature_ID+
			" FROM "+table.signature.TNAME+
			" WHERE "+table.signature.global_signature_ID+"=? "+
			//" AND "+table.signature.organization_ID+"=? "+
			" AND "+table.signature.signature + " IS NOT NULL ";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{hash}, DEBUG);
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_Vote:available: "+hash+" in "+orgID+"(?) = "+result);
		return result;
	}
	public static byte getASN1Type() {
		return TAG;
	}
	/**
	 * 
	 * @param gID
	 * @param DBG
	 * @return
	 *  0 for absent,
	 *  1 for present&signed,
	 *  -1 for temporary
	 * @throws SQLiteException
	 */
	public static int isGIDavailable(String gID, boolean DBG) throws SQLiteException {
		String sql = 
			"SELECT "+table.signature.signature_ID+","+table.signature.signature+
			" FROM "+table.signature.TNAME+
			" WHERE "+table.signature.global_signature_ID+"=? ;";
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
}
