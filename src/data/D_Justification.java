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
import java.util.ArrayList;
import java.util.Calendar;
import ciphersuits.SK;
import util.P2PDDSQLException;
import streaming.JustificationHandling;
import streaming.RequestData;
import util.DBInterface;
import util.Util;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.Application;
import config.DD;

/**
 WB_JUSTIFICATION ::= SEQUENCE {
global_justificationID PrintableString,
	justification_title [0] Title_Document  OPTIONAL,
	justification_text [1] Document  OPTIONAL,
	author [2] WB_Constituent OPTIONAL
	date GeneralizedDate,
	signature OCTET_STRING,
}
 */

public class D_Justification extends ASNObj{
	private static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	private static final String V0 = "0";
	public String hash_alg = V0;
	public String global_justificationID;//Printable
	public String global_motionID;//Printable
	public String global_answerTo_ID;//Printable
	public String global_constituent_ID;//Printable
	public String global_organization_ID;//Printable
	public D_Document_Title justification_title = new D_Document_Title();
	public D_Document justification_text = new D_Document();
	//public D_Constituent author;
	public Calendar last_reference_date; //not to be sent
	public Calendar creation_date;
	public byte[] signature; //OCT STR
	public Calendar arrival_date;
	
	public D_Motion motion;
	public D_Constituent constituent;
	public D_Justification answerTo;
	
	public String justification_ID;
	public String motion_ID;
	public String answerTo_ID;
	public String constituent_ID;
	public String organization_ID;
	public boolean requested = false;
	public boolean blocked = false;
	public boolean broadcasted = D_Organization.DEFAULT_BROADCASTED_ORG;

	
	public D_Justification() {}
	public D_Justification(long _justification_ID) throws P2PDDSQLException {
		if(_justification_ID<=0) throw new RuntimeException(Util._("No such justification"));
		justification_ID = ""+_justification_ID;
		String sql = 
			"SELECT "+Util.setDatabaseAlias(table.justification.fields,"j")+
			", c."+table.constituent.global_constituent_ID+
			", m."+table.motion.global_motion_ID+
			", a."+table.justification.global_justification_ID+
			", o."+table.organization.global_organization_ID+
			", o."+table.organization.organization_ID+
			" FROM "+table.justification.TNAME+" AS j "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=j."+table.justification.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=j."+table.justification.motion_ID+")"+
			" LEFT JOIN "+table.justification.TNAME+" AS a ON(a."+table.justification.justification_ID+"=j."+table.justification.answerTo_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE j."+table.justification.justification_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> m = Application.db.select(sql, new String[]{justification_ID}, DEBUG);
		if(m.size() == 0) return;
		init(m.get(0));
	}
	/**
	 * 
	 * @param justification_GID : justification_global_ID
	 * @throws P2PDDSQLException
	 */
	public D_Justification(String justification_GID) throws P2PDDSQLException {
		if(justification_GID == null) throw new RuntimeException(Util._("No such justification"));
		this.global_motionID = justification_GID;
		String sql = 
			"SELECT "+Util.setDatabaseAlias(table.justification.fields,"j")+
			", c."+table.constituent.global_constituent_ID+
			", m."+table.motion.global_motion_ID+
			", a."+table.justification.global_justification_ID+
			", o."+table.organization.global_organization_ID+
			", o."+table.organization.organization_ID+
			" FROM "+table.justification.TNAME+" AS j "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=j."+table.justification.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=j."+table.justification.motion_ID+")"+
			" LEFT JOIN "+table.justification.TNAME+" AS a ON(a."+table.justification.justification_ID+"=j."+table.justification.answerTo_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE j."+table.justification.global_justification_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> j = Application.db.select(sql, new String[]{justification_GID}, DEBUG);
		if(j.size() == 0) return;
		init(j.get(0));
	}
	/**
			"SELECT "+Util.setDatabaseAlias(table.justification.fields,"j")+
			", c."+table.constituent.global_constituent_ID+
			", m."+table.motion.global_motion_ID+
			", a."+table.justification.global_justification_ID+
			", o."+table.organization.global_organization_ID+
			", o."+table.organization.organization_ID+
			" FROM "+table.justification.TNAME+" AS j "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=j."+table.justification.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=j."+table.justification.motion_ID+")"+
			" LEFT JOIN "+table.justification.TNAME+" AS a ON(a."+table.justification.justification_ID+"=j."+table.justification.answerToID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
	 * 
	 * @param o
	 * @throws P2PDDSQLException
	 */
	public D_Justification(ArrayList<Object> o) throws P2PDDSQLException {
		if(o==null)throw new RuntimeException(Util._("No such justification"));
		init(o);
	}
	public D_Justification instance() throws CloneNotSupportedException{
		return new D_Justification();
	}
	void init(ArrayList<Object> o) throws P2PDDSQLException {
		this.hash_alg = Util.getString(o.get(table.justification.J_HASH_ALG));
		this.global_justificationID = Util.getString(o.get(table.justification.J_GID));
		this.justification_title.title_document.setFormatString(Util.getString(o.get(table.justification.J_TITLE_FORMAT)));	
		this.justification_title.title_document.setDocumentString(Util.getString(o.get(table.justification.J_TITLE)));
		this.justification_text.setFormatString(Util.getString(o.get(table.justification.J_TEXT_FORMAT)));
		this.justification_text.setDocumentString(Util.getString(o.get(table.justification.J_TEXT)));
		//this.status = Util.ival(Util.getString(o.get(table.justification.J_STATUS)), DEFAULT_STATUS);
		this.creation_date = Util.getCalendar(Util.getString(o.get(table.justification.J_CREATION)));
		this.arrival_date = Util.getCalendar(Util.getString(o.get(table.justification.J_ARRIVAL)));
		this.signature = Util.byteSignatureFromString(Util.getString(o.get(table.justification.J_SIGNATURE)));
		this.motion_ID = Util.getString(o.get(table.justification.J_MOTION_ID));
		this.constituent_ID = Util.getString(o.get(table.justification.J_CONSTITUENT_ID));
		this.answerTo_ID = Util.getString(o.get(table.justification.J_ANSWER_TO_ID));
		this.last_reference_date = Util.getCalendar(Util.getString(o.get(table.justification.J_REFERENCE)));
		this.justification_ID = Util.getString(o.get(table.justification.J_ID));

		this.blocked = Util.stringInt2bool(o.get(table.justification.J_BLOCKED),false);
		this.requested = Util.stringInt2bool(o.get(table.justification.J_REQUESTED),false);
		this.broadcasted = Util.stringInt2bool(o.get(table.justification.J_BROADCASTED), D_Organization.DEFAULT_BROADCASTED_ORG_ITSELF);

		
		this.global_constituent_ID = Util.getString(o.get(table.justification.J_FIELDS+0));
		this.global_motionID = Util.getString(o.get(table.justification.J_FIELDS+1));
		this.global_answerTo_ID = Util.getString(o.get(table.justification.J_FIELDS+2));
		this.global_organization_ID = Util.getString(o.get(table.justification.J_FIELDS+3));
		this.organization_ID = Util.getString(o.get(table.justification.J_FIELDS+4));
		
		//this.choices = WB_Choice.getChoices(motionID);
	}	
	
	public boolean equal(D_Justification j){
		if(!Util.equalStrings_null_or_not(hash_alg,j.hash_alg)) return false;
		if(!Util.equalStrings_null_or_not(global_justificationID,j.global_justificationID)) return false;
		if(!Util.equalStrings_null_or_not(global_motionID,j.global_motionID)) return false;
		if(!Util.equalStrings_null_or_not(global_answerTo_ID,j.global_answerTo_ID)) return false;
		//if(!equalStringFields(justification_title,j.justification_title)) return false;
		//if(!equalStringFields(justification_text,j.justification_text)) return false;
		//if(!equalStringFields(author,j.author)) return false;
		
		return true;
	}
	
	public Encoder getSignableEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(global_justificationID!=null)enc.addToSequence(new Encoder(global_justificationID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(global_motionID!=null)enc.addToSequence(new Encoder(global_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(global_answerTo_ID!=null)enc.addToSequence(new Encoder(global_answerTo_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(justification_title!=null) enc.addToSequence(justification_title.getEncoder().setASN1Type(DD.TAG_AC4));
		if(justification_text!=null)enc.addToSequence(justification_text.getEncoder().setASN1Type(DD.TAG_AC5));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		//if(author!=null)enc.addToSequence(author.getEncoder().setASN1Type(DD.TAG_AC6));
		//if(last_reference_date!=null)enc.addToSequence(new Encoder(last_reference_date).setASN1Type(DD.TAG_AC7));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC8));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC9));
		//if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC11));
		return enc;
	}
	
	public Encoder getHashEncoder() {
		Encoder enc = new Encoder().initSequence();
		//if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		//if(global_justificationID!=null)enc.addToSequence(new Encoder(global_justificationID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(global_motionID!=null)enc.addToSequence(new Encoder(global_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(global_answerTo_ID!=null)enc.addToSequence(new Encoder(global_answerTo_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(justification_title!=null) enc.addToSequence(justification_title.getEncoder().setASN1Type(DD.TAG_AC4));
		if(justification_text!=null)enc.addToSequence(justification_text.getEncoder().setASN1Type(DD.TAG_AC5));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		//if(author!=null)enc.addToSequence(author.getEncoder().setASN1Type(DD.TAG_AC6));
		//if(last_reference_date!=null)enc.addToSequence(new Encoder(last_reference_date).setASN1Type(DD.TAG_AC7));
		//if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC8));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC9));
		//if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC11));
		return enc;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(global_justificationID!=null)enc.addToSequence(new Encoder(global_justificationID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(global_motionID!=null)enc.addToSequence(new Encoder(global_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(global_answerTo_ID!=null)enc.addToSequence(new Encoder(global_answerTo_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		if(justification_title!=null) enc.addToSequence(justification_title.getEncoder().setASN1Type(DD.TAG_AC5));
		if(justification_text!=null)enc.addToSequence(justification_text.getEncoder().setASN1Type(DD.TAG_AC6));
		//if(author!=null)enc.addToSequence(author.getEncoder().setASN1Type(DD.TAG_AC7));
		//if(last_reference_date!=null)enc.addToSequence(new Encoder(last_reference_date).setASN1Type(DD.TAG_AC8));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC9));
		if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC10));
		if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC11));
		if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC12));
		if(answerTo!=null)enc.addToSequence(answerTo.getEncoder().setASN1Type(DD.TAG_AC13));
		if(global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC14));
		return enc;
	}

	@Override
	public D_Justification decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0)hash_alg = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if(dec.getTypeByte()==DD.TAG_AC1)global_justificationID = dec.getFirstObject(true).getString(DD.TAG_AC1);
		if(dec.getTypeByte()==DD.TAG_AC2)global_motionID = dec.getFirstObject(true).getString(DD.TAG_AC2);
		if(dec.getTypeByte()==DD.TAG_AC3)global_answerTo_ID = dec.getFirstObject(true).getString(DD.TAG_AC3);
		if(dec.getTypeByte()==DD.TAG_AC4)global_constituent_ID = dec.getFirstObject(true).getString(DD.TAG_AC4);
		if(dec.getTypeByte()==DD.TAG_AC5)justification_title = new D_Document_Title().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC6)justification_text = new D_Document().decode(dec.getFirstObject(true));
		//if(dec.getTypeByte()==DD.TAG_AC7)author = new D_Constituent().decode(dec.getFirstObject(true));
		//if(dec.getTypeByte()==DD.TAG_AC8)last_reference_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC8);
		if(dec.getTypeByte()==DD.TAG_AC9)creation_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC9);
		if(dec.getTypeByte()==DD.TAG_AC10)signature = dec.getFirstObject(true).getBytes(DD.TAG_AC10);
		if(dec.getTypeByte()==DD.TAG_AC11)motion = new D_Motion().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC12)constituent = new D_Constituent().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC13)answerTo = new D_Justification().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC14)global_organization_ID = dec.getFirstObject(true).getString(DD.TAG_AC14);
		
		if ((global_constituent_ID == null) && (constituent != null)) global_constituent_ID = constituent.global_constituent_id;
		if ((global_answerTo_ID == null) && (answerTo != null)) global_answerTo_ID = answerTo.global_justificationID;
		if ((global_motionID == null) && (motion != null)) global_motionID = motion.global_motionID;
		
		return this;
	}
	
	public String toString() {
		return "WB_Justification:" +
				"\n hash_alg="+hash_alg+
				"\n global_justificationID="+global_justificationID+
				"\n global_motionID="+global_motionID+
				"\n global_answerTo_ID="+global_answerTo_ID+
				"\n global_constituent_ID="+global_constituent_ID+
				"\n justification_title="+justification_title+
				"\n justification_text="+justification_text+
				//"\n author="+author+
				"\n last_reference_date="+Encoder.getGeneralizedTime(last_reference_date)+
				"\n creation_date="+Encoder.getGeneralizedTime(creation_date)+
				"\n arrival_date="+Encoder.getGeneralizedTime(arrival_date)+
				"\n signature="+Util.byteToHexDump(signature)+
				"\n justification_ID="+justification_ID+
				"\n motion_ID="+motion_ID+
				"\n answerTo_ID="+answerTo_ID+
				"\n constituent_ID="+constituent_ID+
				"\n motion="+motion+
				"\n constituent="+constituent;
	}
	public byte[] sign() {
		return sign(this.global_constituent_ID);
	}
	public byte[] sign(String signer_GID) {
		if(DEBUG) System.out.println("WB_Justification:sign: start signer="+signer_GID);
		ciphersuits.SK sk = util.Util.getStoredSK(signer_GID);
		if(sk==null) {
			if(_DEBUG) System.out.println("WB_Justification:sign: no signature");
			Application.warning(_("No secret key to sign motion, no constituent GID"), _("No Secret Key!"));
			return null;
		}
		if(DEBUG) System.out.println("WB_Justification:sign: sign="+sk);

		return sign(sk);
	}
	/**
	 * Both store signature and returns it
	 * @param sk
	 * @return
	 */
	public byte[] sign(SK sk){
		if(DEBUG) System.out.println("WB_Justification:sign: this="+this+"\nsk="+sk);
		signature = Util.sign(this.getSignableEncoder().getBytes(), sk);
		if(DEBUG) System.out.println("WB_Justification:sign:got this="+Util.byteToHexDump(signature));
		return signature;
	}
	public String make_ID(){
		try {
			fillGlobals();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		// return this.global_witness_ID =  
		return "J:"+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
	}
	public boolean verifySignature(){
		if(DEBUG) System.out.println("WB_Justification:verifySignature: start");
		String pk_ID = this.global_constituent_ID;//.submitter_global_ID;
		if((pk_ID == null) && (this.constituent!=null) && (this.constituent.global_constituent_id!=null))
			pk_ID = this.constituent.global_constituent_id;
		if(pk_ID == null) return false;
		
		String newGID = make_ID();
		if(!newGID.equals(this.global_justificationID)) {
			Util.printCallPath("WB_Justification: WRONG EXTERNAL GID");
			if(DEBUG) System.out.println("WB_Justification:verifySignature: WRONG HASH GID="+this.global_justificationID+" vs="+newGID);
			if(DEBUG) System.out.println("WB_Justification:verifySignature: WRONG HASH GID result="+false);
			return false;
		}
		
		boolean result = Util.verifySignByID(this.getSignableEncoder().getBytes(), pk_ID, signature);
		if(DEBUG) System.out.println("WB_Justification:verifySignature: result wGID="+result);
		return result;
	}
	/**
	 * before call, one should set organization_ID and global_motionID
	 * @param rq
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long store(streaming.RequestData rq) throws P2PDDSQLException {
		//boolean DEBUG = true;
		
		
		boolean locals = fillLocals(rq, true, true, true, true);
		if(!locals) return -1;

		
		if(DEBUG) System.out.println("D_Justification:store: start");
		if(!this.verifySignature()) {
			if(_DEBUG) System.out.println("D_Justification:store: verifySignature failed");
			if(! DD.ACCEPT_UNSIGNED_DATA) {
				if(_DEBUG) System.out.println("D_Justification:store: verifySignature failure, quit");
				return -1;
			}
		}

		String _old_date[] = new String[1];
		if ((this.justification_ID == null) && (this.global_justificationID != null))
			this.justification_ID = getLocalIDandDate(this.global_justificationID, _old_date);
		if(this.justification_ID != null ) {
			String old_date = _old_date[0];//getDateFor(this.vote_ID); getDateFor(this.justification_ID);
			if(old_date != null) {
				String new_date = Encoder.getGeneralizedTime(this.creation_date);
				if(new_date.compareTo(old_date)<=0) return new Integer(justification_ID).longValue();
			}
		}
		
		if((this.organization_ID == null ) && (this.global_organization_ID != null))
			this.organization_ID = D_Organization.getLocalOrgID_(this.global_organization_ID);
		if((this.organization_ID == null ) && (this.global_organization_ID != null)) {
			organization_ID = ""+data.D_Organization.insertTemporaryGID(global_organization_ID);
			rq.orgs.add(global_organization_ID);
		}
		
		if((this.constituent_ID == null ) && (this.global_constituent_ID != null))
			this.constituent_ID = D_Constituent.getConstituentLocalIDFromGID(this.global_constituent_ID);
		if((this.constituent_ID == null ) && (this.global_constituent_ID != null)) {
			constituent_ID =
				""+D_Constituent.insertTemporaryConstituentGID(global_constituent_ID, this.organization_ID);
			rq.cons.put(global_constituent_ID,DD.EMPTYDATE);
		}
		
		if ((this.motion_ID == null) && (this.global_motionID != null))
			this.motion_ID = D_Motion.getMotionLocalID(this.global_motionID);
		if ((this.motion_ID == null) && (this.global_motionID != null)) {
			this.motion_ID = ""+ D_Motion.insertTemporaryGID(global_motionID, this.organization_ID);
			rq.moti.add(global_motionID);
		}

		if ((this.answerTo_ID == null) && (this.global_answerTo_ID != null))
			this.answerTo_ID = JustificationHandling.getJustificationLocalID(this.global_answerTo_ID);
		if ((this.answerTo_ID == null) && (this.global_answerTo_ID != null)) {
			this.answerTo_ID = ""+ insertTemporaryGID(global_answerTo_ID, this.motion_ID);
			rq.just.add(global_answerTo_ID);
		}

		rq.just.remove(this.global_justificationID);
		
		long result = storeVerified();
		if(DEBUG) System.out.println("D_Justification:store: result ID= "+result);
		return result;
	}
	public static long insertTemporaryGID(String just_GID, String mot_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("WB_Justification:insertTemporaryGID: start");
		return Application.db.insert(table.justification.TNAME,
				new String[]{table.justification.global_justification_ID, table.justification.motion_ID},
				new String[]{just_GID, mot_ID},
				DEBUG);
	}
	private static String getDateFor(String justID) throws P2PDDSQLException {
		String sql = "SELECT "+table.justification.creation_date+" FROM "+table.justification.TNAME+
		" WHERE "+table.justification.justification_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{""+justID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public String guessOrganizationGID() throws P2PDDSQLException {
		String motion_org;
		String const_org;
		long oID=-1,m_oID=-1,c_oID=-1;
		String mot_sql = "SELECT "+table.motion.organization_ID+" FROM "+table.motion.TNAME+" WHERE "+table.motion.global_motion_ID+"=?;";
		ArrayList<ArrayList<Object>> m = Application.db.select(mot_sql, new String[]{this.global_motionID}, DEBUG);
		if(m.size()>0){
			oID=m_oID = Util.lval(m.get(0).get(0),-1);
			if(oID>0)return D_Organization.getGlobalOrgID(Util.getStringID(oID));
		}
		
		String con_sql = "SELECT "+table.constituent.organization_ID+" FROM "+table.constituent.TNAME+" WHERE "+table.constituent.global_constituent_ID+"=?;";
		ArrayList<ArrayList<Object>> c = Application.db.select(con_sql, new String[]{this.global_constituent_ID}, DEBUG);
		if(c.size()>0){
			c_oID = Util.lval(c.get(0).get(0),-1);
		}
		if((c_oID>0)&&(oID>0)&&(c_oID!=oID)){
			if(_DEBUG) System.out.println("D_Just:guess: "+c_oID+" vs "+oID);
			return null;
		}
		if(DEBUG) System.out.println("D_Just:guess right: "+c_oID+" vs "+oID);
		if(c_oID>0) oID=c_oID;
		return D_Organization.getGlobalOrgID(Util.getStringID(oID));
	}

	public boolean fillLocals(RequestData rq, boolean tempOrg, boolean default_blocked_org, boolean tempConst, boolean tempMotion) throws P2PDDSQLException {
		/*
		if((global_organization_ID==null)&&(organization_ID == null)){
			Util.printCallPath("cannot store witness with not orgGID");
			return false;
		}
		*/
		if((this.global_constituent_ID==null)&&(constituent_ID == null)){
			Util.printCallPath("cannot store witness with not submitterGID");
			return false;
		}
		if((this.global_motionID==null)&&(motion_ID == null)){
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
			this.constituent_ID = D_Constituent.getConstituentLocalIDFromGID(global_constituent_ID);
			if(tempConst && (constituent_ID == null ))  {
				String consGID_hash = D_Constituent.getGIDHashFromGID(global_constituent_ID);
				if(rq!=null)rq.cons.put(consGID_hash, DD.EMPTYDATE);
				constituent_ID = Util.getStringID(D_Constituent.insertTemporaryConstituentGID(global_constituent_ID, organization_ID));
			}
			if(constituent_ID == null) return false;
		}
		
		if((this.global_motionID!=null)&&(motion_ID == null)){
			this.motion_ID = D_Motion.getMotionLocalID(global_motionID);
			if(tempMotion && (motion_ID == null ))  {
				if(rq!=null)rq.moti.add(global_motionID);
				motion_ID = Util.getStringID(D_Motion.insertTemporaryGID(global_motionID, organization_ID));
			}
			if(motion_ID == null) return false;
		}
		return true;
	}
	private void fillGlobals() throws P2PDDSQLException {
		if((this.answerTo_ID != null ) && (this.global_answerTo_ID == null))
			this.global_answerTo_ID = D_Justification.getGlobalID(this.answerTo_ID);
		
		if((this.organization_ID != null ) && (this.global_organization_ID == null))
			this.global_organization_ID = D_Organization.getGlobalOrgID(this.organization_ID);

		if((this.motion_ID != null ) && (this.global_motionID == null))
			this.global_motionID = D_Motion.getMotionGlobalID(this.motion_ID);
		
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
		if(DEBUG) System.out.println("WB_Justification:storeVerified: start arrival="+Encoder.getGeneralizedTime(arrival_date));
		if(this.constituent_ID == null )
			constituent_ID = D_Constituent.getConstituentLocalIDFromGID(this.global_constituent_ID);
		if(constituent_ID == null){
			if(_DEBUG) System.out.println("WB_Justification:storeVerified: no signer!");
			return -1;
		}
		if((this.answerTo_ID == null ) && (this.global_answerTo_ID != null))
			this.answerTo_ID = D_Justification.getLocalID(this.global_answerTo_ID);
		
		if((this.organization_ID == null ) && (this.global_organization_ID != null))
			this.organization_ID = D_Organization.getLocalOrgID_(this.global_organization_ID);

		if((this.motion_ID == null ) && (this.global_motionID != null))
			this.motion_ID = D_Motion.getMotionLocalID(this.global_motionID);

		if((this.justification_ID == null ) && (this.global_justificationID != null)) {
			this.justification_ID = D_Justification.getLocalID(this.global_justificationID);
		}else{
			if((this.justification_ID != null ) && (this.global_justificationID != null)) {
				String tmp = D_Justification.getLocalID(this.global_justificationID);
				if((tmp!=null) && (this.justification_ID.compareTo(tmp) != 0)) {
					Application.warning(Util._("Double Justification. Modify or delete it."), Util._("Cannot save duplicated justification!"));
					return -1; 
				}
			}
		}
		
		if(DEBUG) System.out.println("D_Justification:storeVerified: fixed local="+this);
		
		String params[] = new String[table.justification.J_FIELDS];
		params[table.justification.J_HASH_ALG] = this.hash_alg;
		params[table.justification.J_GID] = this.global_justificationID;
		params[table.justification.J_TITLE_FORMAT] = this.justification_title.title_document.getFormatString();
		params[table.justification.J_TEXT_FORMAT] = this.justification_text.getFormatString();
		params[table.justification.J_TITLE] = this.justification_title.title_document.getDocumentString();
		params[table.justification.J_TEXT] = this.justification_text.getDocumentString();
		params[table.justification.J_ANSWER_TO_ID] = this.answerTo_ID;
		params[table.justification.J_CONSTITUENT_ID] = this.constituent_ID;
		//params[table.justification.J_ORG_ID] = organization_ID;
		//params[table.justification.J_STATUS] = status+"";
		params[table.justification.J_MOTION_ID] = this.motion_ID;
		params[table.justification.J_SIGNATURE] = Util.stringSignatureFromByte(signature);
		params[table.justification.J_REFERENCE] = Encoder.getGeneralizedTime(this.last_reference_date);
		params[table.justification.J_CREATION] = Encoder.getGeneralizedTime(this.creation_date);
		params[table.justification.J_ARRIVAL] = Encoder.getGeneralizedTime(arrival_date);
		params[table.justification.J_BLOCKED] = Util.bool2StringInt(blocked);
		params[table.justification.J_REQUESTED] = Util.bool2StringInt(requested);
		params[table.justification.J_BROADCASTED] = Util.bool2StringInt(broadcasted);
		if(this.justification_ID == null) {
			if(DEBUG) System.out.println("WB_Justification:storeVerified:inserting");
			result = Application.db.insert(table.justification.TNAME,
					table.justification.fields_array,
					params,
					DEBUG
					);
			justification_ID=""+result;
		}else{
			if(DEBUG) System.out.println("WB_Justification:storeVerified:updating ID="+this.justification_ID);
			params[table.justification.J_ID] = justification_ID;
			Application.db.update(table.justification.TNAME,
					table.justification.fields_noID_array,
					new String[]{table.justification.justification_ID},
					params,
					DEBUG
					);
			result = Util.lval(this.justification_ID, -1);
		}
		//Application.db.delete(table.motion_choice.TNAME, new String[]{table.motion_choice.motion_ID}, new String[]{result+""}, DEBUG);
		//WB_Choice.save(choices, enhanced_motionID);
		if(DEBUG) System.out.println("D_Justification:storeVerified: return result="+result);
		return result;
	}
	/**
	 * update signature
	 * @param witness_ID
	 * @param signer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long readSignSave(long justification_ID, long signer_ID) throws P2PDDSQLException {
		D_Justification w=new D_Justification(justification_ID);
		ciphersuits.SK sk = util.Util.getStoredSK(D_Constituent.getConstituentGlobalID(""+signer_ID));
		w.sign(sk);
		return w.storeVerified();
	}	
	private static String getLocalIDandDate(String global_justificationID,	String[] _old_date) throws P2PDDSQLException {
		String sql = "SELECT "+table.justification.justification_ID+","+table.justification.creation_date+" FROM "+table.justification.TNAME+
		" WHERE "+table.justification.global_justification_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{global_justificationID}, DEBUG);
		if(o.size()==0) return null;
		_old_date[0] = Util.getString(o.get(0).get(1));
		return Util.getString(o.get(0).get(0));
	}
	public static String getLocalID(String global_justificationID) throws P2PDDSQLException {
		String sql = "SELECT "+table.justification.justification_ID+" FROM "+table.justification.TNAME+
		" WHERE "+table.justification.global_justification_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{global_justificationID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public static String getGlobalID(String justificationID) throws P2PDDSQLException {
		String sql = "SELECT "+table.justification.global_justification_ID+" FROM "+table.justification.TNAME+
		" WHERE "+table.justification.justification_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{justificationID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public static byte getASN1Type() {
		return TAG;
	}
	public void setEditable() {
		signature = null;
		this.global_justificationID = null;
	}
	public boolean isEditable() {
		if(signature == null){
			if(DEBUG) out.println("D_Justification:editable: no sign");
			return true;
		}
		if(global_justificationID == null){
			if(DEBUG) out.println("D_Justification:editable: no GID");
			return true;
		}
		if(!verifySignature()) {
			if(DEBUG) out.println("D_Justification:editable: not verifiable signature");
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
	public void changeToDefaultAnswer() {
		this.answerTo_ID = justification_ID;
		this.global_answerTo_ID = this.global_justificationID;
		this.justification_ID = null;
		this.global_justificationID = null;
		this.signature = null;
		this.constituent_ID = null;
		this.global_constituent_ID = null;
		this.justification_title.title_document.setDocumentString(_("Answer To:")+" "+this.justification_title.title_document.getDocumentUTFString());
		this.justification_text.setDocumentString(_("Answer To:")+" \n"+this.justification_text.getDocumentUTFString());
		this.creation_date = this.arrival_date = Util.CalendargetInstance();
		this.requested = this.blocked = this.broadcasted = false;
	}
	public static ArrayList<String> checkAvailability(ArrayList<String> hashes,
			String orgID, boolean DBG) throws P2PDDSQLException {
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
	 * @throws P2PDDSQLException
	 */
	public static boolean available(String hash, String orgID, boolean DBG) throws P2PDDSQLException {
		boolean result = true;
		String sql = 
			"SELECT "+table.justification.justification_ID+
			" FROM "+table.justification.TNAME+
			" WHERE "+table.justification.global_justification_ID+"=? "+
			//" AND "+table.justification.organization_ID+"=? "+
			" AND ( "+table.justification.signature + " IS NOT NULL " +
			" OR "+table.justification.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{hash}, DEBUG);
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_Justification:available: "+hash+" in "+orgID+"(?) = "+result);
		return result;
	}
	
	public static void _main(String[] args) {
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			if(args.length>0){readSignSave(3,1); if(true) return;}
			
			D_Justification c=new D_Justification(1);
			if(!c.verifySignature()) System.out.println("\n************Signature Failure\n**********\nread="+c);
			else System.out.println("\n************Signature Pass\n**********\nread="+c);
			Decoder dec = new Decoder(c.getEncoder().getBytes());
			D_Justification d = new D_Justification().decode(dec);
			Calendar arrival_date = d.arrival_date=Util.CalendargetInstance();
			//if(d.global_organization_ID==null) d.global_organization_ID = OrgHandling.getGlobalOrgID(d.organization_ID);
			if(!d.verifySignature()) System.out.println("\n************Signature Failure\n**********\nrec="+d);
			else System.out.println("\n************Signature Pass\n**********\nrec="+d);
			d.global_justificationID = d.make_ID();
			//d.storeVerified(arrival_date);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Called as:
	 * database id fix verbose
	 * id: 0 - traverse all
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if(args.length == 0) {
				System.out.println("prog database id fix verbose");
				return;
			}
			
			String database = Application.DELIBERATION_FILE;
			if(args.length>0) database = args[0];
			
			long id = 0;
			if(args.length>1) id = Long.parseLong(args[1]);
			
			boolean fix = false;
			if(args.length>2) fix = Util.stringInt2bool(args[2], false);
			
			//boolean verbose = false;
			if(args.length>3) DEBUG = Util.stringInt2bool(args[3], false);
			
			
			Application.db = new DBInterface(database);
			
			ArrayList<ArrayList<Object>> l;
			D_Organization organization;
			if(id<=0){
				l = Application.db.select(
						"SELECT "+table.justification.justification_ID+
						" FROM "+table.justification.TNAME, new String[]{}, DEBUG);
				for(ArrayList<Object> a: l){
					String m_ID = Util.getString(a.get(0));
					long ID = Util.lval(m_ID, -1);
					D_Justification m = new D_Justification(ID);
					if(m.signature==null){
						//if(m.motion==null)m.motion = new D_Motion(Util.lval(m.organization_ID, -1));
						//organization = new D_Organization(Util.lval(m.motion.organization_ID, -1));
						System.out.println("Fail:temporary "+m.justification_ID+":"+m.justification_title+" in "+m.organization_ID+":"+m.motion_ID);
					
						if(fix){
							if(m.motion_ID==null)
								Application.db.delete(table.justification.TNAME,
										new String[]{table.justification.justification_ID},
										new String[]{m_ID}, true);
						}
						
						continue;
					}
					if(m.global_motionID==null){
						if(m.motion==null)m.motion = new D_Motion(Util.lval(m.organization_ID, -1));
						organization = new D_Organization(Util.lval(m.motion.organization_ID, -1));
						System.out.println("Fail:edited "+m.justification_ID+":"+m.justification_title+" in "+m.organization_ID+":"+organization.name);
						continue;
					}
					if(!m.verifySignature()){
						if(m.motion==null)m.motion = new D_Motion(Util.lval(m.organization_ID, -1));
						organization = new D_Organization(Util.lval(m.motion.organization_ID, -1));
						System.out.println("Fail: "+m.justification_ID+":"+m.justification_title+" in "+m.organization_ID+":"+organization.name);
						if(fix){
							m.global_motionID = m.make_ID();
							m.storeVerified();
							readSignSave(ID, Util.lval(m.constituent_ID, -1));
						}
					}
				}
				return;
			}else{
				long ID = id;
				D_Justification m = new D_Justification(ID);
				if(fix)
					if(!m.verifySignature()) {
						if(m.motion==null)m.motion = new D_Motion(Util.lval(m.organization_ID, -1));
						organization = new D_Organization(Util.lval(m.organization_ID, -1));
						System.out.println("Fixing: "+m.justification_ID+":"+m.justification_title+" in "+m.organization_ID+":"+organization.name);
						readSignSave(ID, Util.lval(m.constituent_ID, -1));
					}
				else if(!m.verifySignature()){
					if(m.motion==null)m.motion = new D_Motion(Util.lval(m.organization_ID, -1));
					organization = new D_Organization(Util.lval(m.organization_ID, -1));
					System.out.println("Fail: "+m.justification_ID+":"+m.justification_title+" in "+m.organization_ID+":"+organization.name);
				}
				return;
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		//catch (ASN1DecoderFail e) {e.printStackTrace();}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
