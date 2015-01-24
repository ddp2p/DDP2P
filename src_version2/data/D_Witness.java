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
import static util.Util.__;
import hds.ASNSyncPayload;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import ciphersuits.SK;
import util.P2PDDSQLException;
import config.Application;
import config.Application_GUI;
import config.DD;
import streaming.RequestData;
import util.DBInterface;
import util.Summary;
import util.Util;

class D_WitnessStatements extends ASNObj {
	public int version = 1;
	int[] sense_y = new int[0]; // 1=yes, 0=unknown, -1=no
	String[] explanation = new String[0];
	// index 0 (eligibility), index 1 (trustworthyness)
	String[] color; // optional (other)
	public D_WitnessStatements() {
		
	}
	public D_WitnessStatements(Decoder decoder) {
		try {
			decode(decoder);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(Encoder.getEncoderArray(sense_y));
		enc.addToSequence(Encoder.getStringEncoder(explanation, Encoder.TAG_UTF8String));
		if(color != null) enc.addToSequence(Encoder.getStringEncoder(color, Encoder.TAG_UTF8String));
		return enc;
	}
	@Override
	public D_WitnessStatements decode(Decoder dec) throws ASN1DecoderFail {
		Decoder _d, d = dec.getContent();
		version = d.getFirstObject(true).getInteger().intValue();
		sense_y = d.getFirstObject(true).getIntsArray();
		_d = d.getFirstObject(true);
		if(_d!=null) explanation = _d.getSequenceOf(Encoder.TAG_UTF8String);
		_d = d.getFirstObject(true);
		if(_d != null) color = _d.getSequenceOf(Encoder.TAG_UTF8String);
		return this;
	}
	public String toString(){
		return "[Stat:] "+Util.concat(color, ":")+"\n"
				+"sense="+Util.concat(sense_y, ":", "")+"\n"
				+"expl=\n "+Util.concat(explanation, "\n ");
	}
}

public
class D_Witness extends ASNObj implements Summary {
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public static final int FAVORABLE = 1;
	public static final int UNFAVORABLE = -1;
	public static final int UNKNOWN = 0;
	public static final int UNSPECIFIED = -2;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	public static final String V0 = "V0";
	public static final String V1 = "V1";
	public String hash_alg = V1;
	public String global_witness_ID; // hash (global key)
	public String witnessed_global_neighborhoodID;
	public String witnessed_global_constituentID;
	public String witnessing_global_constituentID;
	public String global_organization_ID;
	public String witness_eligibility_category; // known, hearsay
	public int sense_y_n; // 1=positive; -1=negative; 0=unknown
	public String witness_trustworthiness_category = null; // known, hearsay
	public int sense_y_trustworthiness = 0;
	public byte[] signature;
	public Calendar creation_date;
	public Calendar arrival_date;
	public D_Constituent witnessed;
	public D_Constituent witnessing;
	public D_Neighborhood witnessed_neighborhood;
	public D_WitnessStatements statements;
	
	public void global_organization_ID(String val){
		global_organization_ID = val;
	}// part of the signature but not of transmitted data
	
	// not part of signature or transmitted data
	public long witnessID;
	public long witnessed_neighborhoodID;
	public long witnessed_constituentID;
	public long witnessing_constituentID;
	public String organization_ID;
	private D_Neighborhood neighborhood;
	public static final String[] witness_categories=new String[]{
				__("Personally known eligibility"),
				__("Hearsay eligibility"),
				__("Unknown"),"",
				__("Nonexistent address"),
				__("No such person at this address"),
				__("Not eligible"),
				__("Error in address")
	};
	public static final int[] witness_categories_sense = new int[]{FAVORABLE,FAVORABLE,UNKNOWN,UNKNOWN,UNFAVORABLE,UNFAVORABLE,UNFAVORABLE,UNFAVORABLE};
	public static final String[] witness_categories_trustworthiness=new String[]{
				__("Personal trust"),
				__("Hearsay trust"),
				__("Unknown"),"",
				__("Hearsay distrust"),
				__("Personal distrust")
	};
	public static final int[] witness_categories_trustworthiness_sense = new int[]{FAVORABLE,FAVORABLE,UNKNOWN,UNKNOWN,UNFAVORABLE,UNFAVORABLE};
	public static final Hashtable<String,Integer> sense_trustworthiness = D_Witness.init_sense_trustworthiness();
	//public final int first_negative=2;
	public static final Hashtable<String,Integer> sense_eligibility = D_Witness.init_sense_eligibility();

	public String toString() {
		return "D_Witness:["
		+"\n hash_alg="+hash_alg
		+"\n global_witness_ID="+global_witness_ID
		+"\n witness_category="+witness_eligibility_category
		+"\n witnessed_global_neighborhoodID="+witnessed_global_neighborhoodID
		+"\n witnessed_global_constituentID="+witnessed_global_constituentID
		+"\n witnessing_global_constituentID="+witnessing_global_constituentID
		+"\n global_organization_ID="+global_organization_ID
		+"\n sense_y_n="+sense_y_n
		+"\n signature="+Util.byteToHexDump(signature)
		+"\n creation_date="+Encoder.getGeneralizedTime(creation_date)
		+"\n arrival_date="+Encoder.getGeneralizedTime(arrival_date)
		+"\n witnessing constituent data="+witnessing
		+"\n witnessed constituent data="+witnessed
		//+"\n global_organization_ID="+global_organization_ID
		+"\n---"
		+"\n witnessID="+witnessID
		+"\n witnessed_neighborhoodID="+witnessed_neighborhoodID
		+"\n witnessed_constituentID="+witnessed_constituentID
		+"\n witnessing_constituentID="+witnessing_constituentID
		+"\n statements="+statements
		;
	}
	
	public String toSummaryString() {
		return "D_Witness:["
				//+"\n witnessing_global_constituentID="+witnessing_global_constituentID
				//+"\n witnessed_global_constituentID="+witnessed_global_constituentID
				+" witness_category="+witness_eligibility_category
				//+"\n global_organization_ID="+global_organization_ID
				+"]";
	}
	
	public static D_Witness getEmpty() {return new D_Witness();}
	public D_Witness(){}
	public D_Witness(long witnessID) throws P2PDDSQLException{
		if(DEBUG) System.out.println("D_Witness:D_Witness: start wID="+witnessID);
		String sql = 
			"SELECT "
			+ Util.setDatabaseAlias(table.witness.witness_fields, "w")+" "
			//+", n."+table.neighborhood.global_neighborhood_ID
			//+", t."+table.constituent.global_constituent_ID
			//+", s."+table.constituent.global_constituent_ID
			//+", o."+table.organization.global_organization_ID
			//+", o."+table.organization.organization_ID
			+" FROM "+table.witness.TNAME+" AS w "+
			//" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(n."+table.neighborhood.neighborhood_ID+"=w."+table.witness.neighborhood_ID+") "+
			//" LEFT JOIN "+table.constituent.TNAME+" AS s ON(s."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+") "+
			//" LEFT JOIN "+table.constituent.TNAME+" AS t ON(t."+table.constituent.constituent_ID+"=w."+table.witness.target_ID+") "+
			//" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=s."+table.constituent.organization_ID+") "+
			" WHERE w."+table.witness.witness_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> w = Application.db.select(sql, new String[]{Util.getStringID(witnessID)}, DEBUG);
		if(w.size()>0) init(w.get(0));
		if(DEBUG) System.out.println("D_Witness:D_Witness: Done");
	}
	public D_Witness(String witnessGID) throws P2PDDSQLException{
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("D_Witness:D_Witness: start wGID="+witnessGID);
		if(witnessGID==null) throw new D_NoDataException("null witnessGID");
		String sql = 
			"SELECT "
			+ Util.setDatabaseAlias(table.witness.witness_fields, "w")+" "
			//+", n."+table.neighborhood.global_neighborhood_ID
			//+", t."+table.constituent.global_constituent_ID
			//+", s."+table.constituent.global_constituent_ID
			//+", o."+table.organization.global_organization_ID
			//+", o."+table.organization.organization_ID
			+" FROM "+table.witness.TNAME+" AS w "+
			//" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(n."+table.neighborhood.neighborhood_ID+"=w."+table.witness.neighborhood_ID+") "+
			//" LEFT JOIN "+table.constituent.TNAME+" AS s ON(s."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+") "+
			//" LEFT JOIN "+table.constituent.TNAME+" AS t ON(t."+table.constituent.constituent_ID+"=w."+table.witness.target_ID+") "+
			//" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=s."+table.constituent.organization_ID+") "+
			" WHERE w."+table.witness.global_witness_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> w = Application.db.select(sql, new String[]{witnessGID}, DEBUG);
		if (w.size() > 0) init(w.get(0));
		else throw new D_NoDataException("absent witnessGID:"+witnessGID);
		if(DEBUG) System.out.println("D_Witness:D_Witness: Done: "+this);
	}
	/**
	 * "SELECT "
	 *		+ Util.setDatabaseAlias(table.witness.witness_fields, "w")+" "
	 *		+", neighborhood."+table.neighborhood.global_neighborhood_ID
	 *		+", target."+table.constituent.global_constituent_ID
	 *		+", source."+table.constituent.global_constituent_ID
			+", o."+table.organization.global_organization_ID
	 * @param w
	 */
	public D_Witness(ArrayList<Object> w) {
		init(w);
	}	
	/**
			"SELECT "
			+ Util.setDatabaseAlias(table.witness.witness_fields, "w")+" "
			+", n."+table.neighborhood.global_neighborhood_ID
			+", t."+table.constituent.global_constituent_ID
			+", s."+table.constituent.global_constituent_ID
			+", o."+table.organization.global_organization_ID
			+" FROM "+table.witness.TNAME+" AS w "+
			" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(n."+table.neighborhood.neighborhood_ID+"=w."+table.witness.neighborhood_ID+") "+
			" LEFT JOIN "+table.constituent.TNAME+" AS s ON(s."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+") "+
			" LEFT JOIN "+table.constituent.TNAME+" AS t ON(t."+table.constituent.constituent_ID+"=w."+table.witness.target_ID+") "+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=s."+table.witness.source_ID+") "+
	 * 
	 * @param w
	 * @throws Exception
	 */
	public void init_all(ArrayList<Object> w) throws Exception {
		if(DEBUG)System.out.println("D_Witness:init_all: start");
		try{
			init(w);
		}catch(Exception e){e.printStackTrace();}
		if(DEBUG)System.out.println("D_Witness:init_all: contained objects");
		this.witnessing = D_Constituent.getConstByLID(this.witnessing_constituentID, true, false);
		if(this.witnessed_constituentID > 0)this.witnessed = D_Constituent.getConstByLID(this.witnessed_constituentID, true, false);
		if(this.witnessed_global_neighborhoodID != null)
			this.neighborhood = D_Neighborhood.getNeighByGID(this.witnessed_global_neighborhoodID, true, false, this.getOrgLID());
		if(DEBUG)System.out.println("D_Witness:init_all: done");
	}
	public long getOrgLID() {
		// TODO
		if (_DEBUG) System.out.println("D_Witness: getOrgLID: need implement");
		return -1;
	}

	/**
			"SELECT "
			+ Util.setDatabaseAlias(table.witness.witness_fields, "w")+" "
			+", n."+table.neighborhood.global_neighborhood_ID
			+", t."+table.constituent.global_constituent_ID
			+", s."+table.constituent.global_constituent_ID
			+", o."+table.organization.global_organization_ID
			+" FROM "+table.witness.TNAME+" AS w "+
			" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(n."+table.neighborhood.neighborhood_ID+"=w."+table.witness.neighborhood_ID+") "+
			" LEFT JOIN "+table.constituent.TNAME+" AS s ON(s."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+") "+
			" LEFT JOIN "+table.constituent.TNAME+" AS t ON(t."+table.constituent.constituent_ID+"=w."+table.witness.target_ID+") "+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=s."+table.witness.source_ID+") "+
	 * 
	 * @param w
	 */
	public void init(ArrayList<Object> w) {
		//boolean DEBUG = true;
		if(DEBUG)System.out.println("D_Witness:init: start");
		hash_alg = Util.getString(w.get(table.witness.WIT_COL_HASH_ALG));
		global_witness_ID = Util.getString(w.get(table.witness.WIT_COL_GID));
		creation_date = Util.getCalendar(Util.getString(w.get(table.witness.WIT_COL_CREATION_DATE)));
		arrival_date = Util.getCalendar(Util.getString(w.get(table.witness.WIT_COL_ARRIVAL_DATE)));
		witness_eligibility_category = Util.getString(w.get(table.witness.WIT_COL_CAT));
		
		sense_y_n = Util.ival(w.get(table.witness.WIT_COL_SENSE),UNKNOWN);
		sense_y_trustworthiness = Util.ival(w.get(table.witness.WIT_COL_SENSE_TRUSTWORTHINESS),UNKNOWN);
		this.witness_trustworthiness_category = Util.getString(w.get(table.witness.WIT_COL_CAT_TRUSTWORTHINESS));
		byte[] _statements = Util.byteSignatureFromString(Util.getString(w.get(table.witness.WIT_COL_STATEMENTS)));
		if(_statements != null) statements = new D_WitnessStatements(new Decoder(_statements));
		signature = Util.byteSignatureFromString(Util.getString(w.get(table.witness.WIT_COL_SIGN)));
		if(DEBUG)System.out.println("D_Witness:init: sign="+Util.byteToHexDump(signature, 10)+"  from sign="+w.get(table.witness.WIT_COL_SIGN));
		witnessed_neighborhoodID = Util.lval(w.get(table.witness.WIT_COL_T_N_ID), -1);
		witnessed_constituentID = Util.lval(w.get(table.witness.WIT_COL_T_C_ID), -1);
		witnessing_constituentID = Util.lval(w.get(table.witness.WIT_COL_S_C_ID), -1);
		witnessID = Util.lval(w.get(table.witness.WIT_COL_ID), -1);
		
		witnessed_global_neighborhoodID = D_Neighborhood.getGIDFromLID(witnessed_neighborhoodID); //Util.getString(w.get(table.witness.WIT_FIELDS+0));
		witnessed_global_constituentID = D_Constituent.getGIDFromLID(witnessed_constituentID); //Util.getString(w.get(table.witness.WIT_FIELDS+1));
		D_Constituent witness = D_Constituent.getConstByLID(witnessing_constituentID, true, false);
		if (DEBUG) System.out.println("D_Witness:init: witness= "+witness);
		if (witness != null) {
			witnessing_global_constituentID = witness.getGID(); //Util.getString(w.get(table.witness.WIT_FIELDS+2));
			global_organization_ID = witness.getOrgGID(); //Util.getString(w.get(table.witness.WIT_FIELDS+3));
			organization_ID = witness.getOrganizationLIDstr(); //Util.getString(w.get(table.witness.WIT_FIELDS+4));
		}
		if (DEBUG) System.out.println("D_Witness:init: done: ");
	}
	public D_Witness instance() throws CloneNotSupportedException{
		return new D_Witness();
	}	
	@Override
	public D_Witness decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		hash_alg = d.getFirstObject(true).getString();
		global_witness_ID = d.getFirstObject(true).getString();
		witness_eligibility_category = d.getFirstObject(true).getString();
		witnessed_global_neighborhoodID = d.getFirstObject(true).getString();
		witnessed_global_constituentID = d.getFirstObject(true).getString();
		witnessing_global_constituentID = d.getFirstObject(true).getString();
		global_organization_ID = d.getFirstObject(true).getString();
		sense_y_n = d.getFirstObject(true).getInteger().intValue();
		creation_date = d.getFirstObject(true).getGeneralizedTimeCalender_();
		signature = d.getFirstObject(true).getBytes();
		if(d.getTypeByte() == DD.TAG_AC0) witnessing = D_Constituent.getEmpty().decode(d.getFirstObject(true));
		if(d.getTypeByte() == DD.TAG_AC1) witnessed = D_Constituent.getEmpty().decode(d.getFirstObject(true));
		if(d.getTypeByte() == DD.TAG_AC5) witnessed_neighborhood = D_Neighborhood.getEmpty().decode(d.getFirstObject(true));
		if(d.getTypeByte() == DD.TAG_AC2) statements = new D_WitnessStatements().decode(d.getFirstObject(true));
		if(d.getTypeByte() == DD.TAG_AC3) witness_trustworthiness_category = d.getFirstObject(true).getString(DD.TAG_AC3);
		if(d.getTypeByte() == DD.TAG_AC4) sense_y_trustworthiness = d.getFirstObject(true).getInteger(DD.TAG_AC4).intValue();
		return this;
	}
	@Override
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) {
		return getEncoder(dictionary_GIDs, 0);
	}
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs, int dependants) {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(hash_alg));
		String repl_GID;
		
		repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_witness_ID);
		enc.addToSequence(new Encoder(repl_GID));

		enc.addToSequence(new Encoder(witness_eligibility_category));

		repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, witnessed_global_neighborhoodID);
		enc.addToSequence(new Encoder(repl_GID));
		
		repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, witnessed_global_constituentID);
		enc.addToSequence(new Encoder(repl_GID));
		
		repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, witnessing_global_constituentID);
		enc.addToSequence(new Encoder(repl_GID));
		
		/**
		 * May decide to comment encoding of "global_organization_ID" out completely, since the org_GID is typically
		 * available at the destination from enclosing fields, and will be filled out at expansion
		 * by ASNSyncPayload.expand at decoding.
		 * However, it is not that damaging when using compression, and can be stored without much overhead.
		 * So it is left here for now.  Test if you comment out!
		 */
		repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_organization_ID);
		enc.addToSequence(new Encoder(repl_GID));
		
		enc.addToSequence(new Encoder(new BigInteger(""+sense_y_n)));
		enc.addToSequence(new Encoder(creation_date));		
		enc.addToSequence(new Encoder(signature));
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			int new_dependants = dependants;
			if (dependants > 0) new_dependants = dependants - 1;
				
			if (witnessing != null) enc.addToSequence(witnessing.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC0));
			if (witnessed != null) enc.addToSequence(witnessed.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC1));
			if (witnessed_neighborhood != null) enc.addToSequence(witnessed_neighborhood.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC5));
		}
		
		if (statements != null) enc.addToSequence(statements.getEncoder().setASN1Type(DD.TAG_AC2));
		if (this.witness_trustworthiness_category != null)  enc.addToSequence(new Encoder(witness_trustworthiness_category).setASN1Type(DD.TAG_AC3));
		if (this.sense_y_trustworthiness != 0)  enc.addToSequence(new Encoder(sense_y_trustworthiness).setASN1Type(DD.TAG_AC4));
		return enc;
	}
	public Encoder getSignableEncoder() {
		Encoder enc = new Encoder().initSequence();
		//enc.addToSequence(new Encoder(global_organization_ID));
		enc.addToSequence(new Encoder(hash_alg));
		enc.addToSequence(new Encoder(global_witness_ID));
		enc.addToSequence(new Encoder(witness_eligibility_category));
		enc.addToSequence(new Encoder(witnessed_global_neighborhoodID));
		enc.addToSequence(new Encoder(witnessed_global_constituentID));
		enc.addToSequence(new Encoder(witnessing_global_constituentID));
		enc.addToSequence(new Encoder(global_organization_ID));
		enc.addToSequence(new Encoder(new BigInteger(""+sense_y_n)));
		enc.addToSequence(new Encoder(creation_date));
		if(statements!=null)enc.addToSequence(statements.getEncoder());
		if(this.witness_trustworthiness_category!=null)  enc.addToSequence(new Encoder(this.witness_trustworthiness_category).setASN1Type(DD.TAG_AC3));
		if(this.sense_y_trustworthiness!=0)  enc.addToSequence(new Encoder(sense_y_trustworthiness).setASN1Type(DD.TAG_AC4));
		//enc.addToSequence(new Encoder(signature));
		return enc;
	}
	public Encoder getHashEncoder() {
		Encoder enc = new Encoder().initSequence();
		if ( ! V0.equals(this.hash_alg) )
			enc.addToSequence(new Encoder(global_organization_ID));
		//enc.addToSequence(new Encoder(hash_alg));
		////enc.addToSequence(new Encoder(global_witness_ID));
		//enc.addToSequence(new Encoder(witness_category));
		enc.addToSequence(new Encoder(witnessed_global_neighborhoodID));
		enc.addToSequence(new Encoder(witnessed_global_constituentID));
		enc.addToSequence(new Encoder(witnessing_global_constituentID));
		//enc.addToSequence(new Encoder(new BigInteger(""+sense_y_n)));
		////enc.addToSequence(new Encoder(creation_date));
		////enc.addToSequence(new Encoder(signature));
		return enc;
	}
	public byte[] sign(String signer_GID) {
		if(DEBUG) System.out.println("D_Witness:sign: start signer="+signer_GID);
		ciphersuits.SK sk = util.Util.getStoredSK(signer_GID);
		if(sk==null) 
			if(_DEBUG) System.out.println("D_Witness:sign: no signature");
		if(DEBUG) System.out.println("D_WItness:sign: sign="+sk);

		return sign(sk);
	}
	/**
	 * Both store signature and returns it
	 * @param sk
	 * @return
	 */
	public byte[] sign(SK sk){
		try {
			fillGlobals();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if(DEBUG) System.out.println("D_Witness:sign: this="+this+"\nsk="+sk);
		signature = Util.sign(this.getSignableEncoder().getBytes(), sk);
		if(DEBUG) System.out.println("D_Witnessing:sign:got this="+Util.byteToHexDump(signature));
		return signature;
	}
	public String make_ID(){
		try {
			fillGlobals();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		// return this.global_witness_ID =  
		return data.D_GIDH.d_Witn+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
	}
	public boolean verifySignature(){
		if(DEBUG) System.out.println("D_Witness:D_Witness: start");
		try {
			fillGlobals();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		String pk_ID = this.witnessing_global_constituentID;//.submitter_global_ID;
		if((pk_ID == null) && (this.witnessing!=null) && (this.witnessing.getGID()!=null))
			pk_ID = this.witnessing.getGID();
		if(pk_ID == null) return false;
		
		String newGID = make_ID();
		if(!newGID.equals(this.global_witness_ID)) {
			Util.printCallPath("WRONG EXTERNAL GID");
			if(DEBUG) System.out.println("D_Witness:verifySignature: WRONG HASH GID="+this.global_witness_ID+" vs="+newGID);
			if(DEBUG) System.out.println("D_Witness:verifySignature: WRONG HASH GID result="+false);
			return false;
		}
		
		boolean result = Util.verifySignByID(this.getSignableEncoder().getBytes(), pk_ID, signature);
		if (DEBUG) {
			System.out.println("D_Witness:verifySignature: result="+result);
			if(result == false) System.out.println("D_Witness:verifySignature: failed for object="+this);
			if(result == false) System.out.println("D_Witness:verifySignature: failed for pk="+pk_ID);
			if(result == false) System.out.println("D_Witness:verifySignature: failed for signature="+Util.byteToHex(signature));
		}
		return result;
	}
	/*
	@Deprecated
	public String getGID(){
		return Util.getGlobalID(table.witness.witness_ID,
				witnessed_global_constituentID+witnessing_global_constituentID);
	}
	*/
	/**
	 * Returns -1 if old or incomplete
	 * @param rq
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long store(RequestData sol_rq, RequestData new_rq, D_Peer __peer) throws P2PDDSQLException {
		if(DEBUG) out.println("D_Witness:store: start "+this.global_witness_ID);
		//D_Peer __peer = null;
		boolean default_blocked = false;
		
		boolean locals = fillLocals(new_rq, true, true, true, true);
		if (!locals) {
			if (_DEBUG) out.println("D_Witness:store: locals fail");
			return -1;
		}
		
		
		if (! verifySignature()) {
			if (! DD.ACCEPT_UNSIGNED_DATA){
				if(_DEBUG) out.println("D_Witness:store: sign fail exit");
				return -1;
			}
			if(_DEBUG) out.println("D_Witness:store: sign fail");

		}else{
			if(sol_rq!=null) sol_rq.witn.add(this.global_witness_ID);
		}
		
		if ((this.witnessID <= 0) && (this.global_witness_ID != null))
			this.witnessID = getLocalIDforGlobal(this.global_witness_ID);
		if(this.witnessID >0 ) {
			String old_date = getDateFor(this.witnessID);
			String new_date = Encoder.getGeneralizedTime(this.creation_date);
			if(new_date.compareTo(old_date)<=0) {
				if(DEBUG) out.println("D_Witness:store: date "+new_date+" old vs "+old_date);
				D_Witness old = new D_Witness(this.witnessID);
				if(DEBUG) out.println("D_Witness:store: old= "+old);
				if(DEBUG) out.println("D_Witness:store: nou= "+this);
				return -1;
			}
		}
		config.Application_GUI.inform_arrival(this, __peer);

		// find existing local IDs
		if((witnessing_constituentID <=0 ) && (witnessing_global_constituentID!=null))
			witnessing_constituentID = D_Constituent.getLIDFromGID(witnessing_global_constituentID, Util.Lval(this.organization_ID));
		if((witnessed_neighborhoodID <=0 ) && (witnessed_global_neighborhoodID!=null))
			witnessed_neighborhoodID = Util.lval(D_Neighborhood.getLIDFromGID(witnessed_global_neighborhoodID, this.getOrgLID()),0);
		if((witnessed_constituentID <=0 ) && (witnessed_global_constituentID != null))
			witnessed_constituentID = D_Constituent.getLIDFromGID(witnessed_global_constituentID, Util.lval(this.organization_ID));

		if((witnessing_constituentID <=0 ) && (witnessing_global_constituentID!=null)) {
			witnessing_constituentID = D_Constituent.insertTemporaryGID(witnessing_global_constituentID, null, Util.lval(this.organization_ID), __peer, default_blocked);
			new_rq.cons.put(witnessing_global_constituentID,DD.EMPTYDATE);
		}
		if((witnessed_neighborhoodID <=0 ) && (witnessed_global_neighborhoodID!=null)) {
			witnessed_neighborhoodID =
				D_Neighborhood._insertTemporaryNeighborhoodGID(witnessed_global_neighborhoodID, this.organization_ID, 0, __peer);
			new_rq.neig.add(witnessed_global_neighborhoodID);
		}
		if((witnessed_constituentID <=0 ) && (witnessed_global_constituentID != null)) {
			witnessed_constituentID = D_Constituent.insertTemporaryGID(witnessed_global_constituentID, null, Util.lval(this.organization_ID), __peer, default_blocked);
			new_rq.cons.put(witnessed_global_constituentID,DD.EMPTYDATE);
		}
		
		return storeVerified();
		//rq.witn.remove(witnesses[k].global_witness_ID); // never needed
	}
	/**
	 * Get creation date in database for this ID
	 * @param witnessID
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static String getDateFor(long witnessID) throws P2PDDSQLException {
		String sql = 
				"SELECT "+table.witness.creation_date+
				" FROM "+table.witness.TNAME+
				" WHERE "+table.witness.witness_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{Util.getStringID(witnessID)}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public boolean fillLocals(RequestData new_rq, boolean tempOrg, boolean default_blocked_org, boolean tempConst, boolean tempNeig) throws P2PDDSQLException {
		D_Peer __peer = null;
		boolean default_blocked = false;
		if((global_organization_ID==null)&&(organization_ID == null)){
			Util.printCallPath("cannot store witness with not orgGID");
			return false;
		}
		if((witnessing_global_constituentID==null)&&(witnessing_constituentID <= 0)){
			Util.printCallPath("cannot store witness with not submitterGID");
			return false;
		}
		
		if((global_organization_ID!=null)&&(organization_ID == null)){
			organization_ID = Util.getStringID(D_Organization.getLIDbyGID(global_organization_ID));
			if(tempOrg && (organization_ID == null)) {
				String orgGID_hash = D_Organization.getOrgGIDHashGuess(global_organization_ID);
				if(new_rq!=null)new_rq.orgs.add(orgGID_hash);
				organization_ID = Util.getStringID(D_Organization.insertTemporaryGID(global_organization_ID, orgGID_hash, default_blocked_org, __peer));
				if(default_blocked_org){
					Util.printCallPath("cannot store witness, blocked");
					return false;
				}
			}
			if(organization_ID == null){
				Util.printCallPath("cannot store witness with not nor org");
				return false;
			}
		}
			
		if ((this.witnessing_global_constituentID!=null) && (witnessing_constituentID <= 0)) {
			this.witnessing_constituentID = Util.lval(D_Constituent.getLIDFromGID(witnessing_global_constituentID, Util.lval(this.organization_ID)), this.witnessing_constituentID);
			if (tempConst && (witnessing_constituentID <= 0 ))  {
				String consGID_hash = D_Constituent.getGIDHashFromGID(witnessing_global_constituentID);
				if (new_rq != null) new_rq.cons.put(consGID_hash,DD.EMPTYDATE);
				witnessing_constituentID = D_Constituent.insertTemporaryGID(witnessing_global_constituentID, null, Util.lval(this.organization_ID), __peer, default_blocked);
				if (DEBUG) System.out.println("D_Witness: fillLocals: getting tmp LID from GID, lid="+witnessing_constituentID);
			}
			if (witnessing_constituentID <= 0){
				Util.printCallPath("cannot store witness with no constituent, tmp="+tempConst+", gid="+witnessing_global_constituentID);
				return false;
			}
		}
		
		if ((this.witnessed_global_constituentID!=null) && (witnessed_constituentID <= 0)) {
			String witned_id=D_Constituent.getLIDstrFromGID(this.witnessed_global_constituentID, Util.Lval(this.organization_ID));
			this.witnessed_constituentID = Util.lval(witned_id, this.witnessed_constituentID);
			if(tempConst && (this.witnessed_constituentID <= 0 ))  {
				String consGID_hash = D_Constituent.getGIDHashFromGID(this.witnessed_global_constituentID);
				if(new_rq!=null)new_rq.cons.put(consGID_hash,DD.EMPTYDATE);
				witned_id=D_Constituent.getLIDstrFromGID_or_GIDH(this.witnessed_global_constituentID, consGID_hash, Util.lval(this.organization_ID));
				this.witnessed_constituentID = Util.lval(witned_id, this.witnessed_constituentID);
				if(this.witnessed_constituentID>0)
					if(_DEBUG) System.out.println("D_Witness:fillLocals: regot id="+this.witnessed_constituentID);
				if(this.witnessed_constituentID<=0)
					this.witnessed_constituentID = D_Constituent.insertTemporaryGID(witnessed_global_constituentID, null, Util.lval(this.organization_ID), __peer, default_blocked);
			}
			if(witnessed_constituentID <= 0){
				Util.printCallPath("cannot store witness with no witnessed constituent");
				System.err.println("D_Witness:fillLocals: Since we cannot save temporary witnessed, we will not save witness: "+this);
				return false;
			}
		}
		
		if((this.witnessed_global_neighborhoodID!=null) && (witnessed_neighborhoodID <= 0)){
			this.witnessed_neighborhoodID = Util.lval(D_Neighborhood.getLIDstrFromGID(witnessed_global_neighborhoodID, Util.Lval(organization_ID)), this.witnessed_neighborhoodID);
			if(tempNeig && (witnessed_neighborhoodID <= 0)) {
				if(new_rq!=null)new_rq.neig.add(witnessed_global_neighborhoodID);
				witnessed_neighborhoodID = D_Neighborhood._insertTemporaryNeighborhoodGID(witnessed_global_neighborhoodID, organization_ID, -1, __peer);
			}
			if(witnessed_neighborhoodID <= 0){
				Util.printCallPath("cannot store witness with no neighborhood");
				return false;
			}
		}
		
		return true;
	}
	private void fillGlobals() throws P2PDDSQLException {
		D_Peer __peer = null;
		boolean default_blocked = false;
		if((this.witnessed_neighborhoodID > 0 ) && (this.witnessed_global_neighborhoodID == null))
			this.witnessed_global_neighborhoodID = D_Neighborhood.getGIDFromLID(this.witnessed_neighborhoodID);
		
		if((this.organization_ID != null ) && (this.global_organization_ID == null))
			this.global_organization_ID = D_Organization.getGIDbyLIDstr(this.organization_ID);

		if((this.witnessing_constituentID > 0 ) && (this.witnessing_global_constituentID == null))
			this.witnessing_global_constituentID = D_Constituent.getGIDFromLID(this.witnessing_constituentID);

		if((this.witnessed_constituentID > 0 ) && (this.witnessed_global_constituentID == null))
			this.witnessed_global_constituentID = D_Constituent.getGIDFromLID(this.witnessed_constituentID);
	}
	public long storeVerified() throws P2PDDSQLException {
		if(DEBUG) out.println("D_Witness:storeVerified: start");
		Calendar now = Util.CalendargetInstance();
		return storeVerified(now);
	}
	static Object monitored_storeVerified = new Object();
	public long storeVerified(Calendar arrival_date) throws P2PDDSQLException {
		synchronized(monitored_storeVerified){
			return _monitored_storeVerified(arrival_date);
		}
	}
	public long storeVerified(boolean sync, Calendar arrival_date) throws P2PDDSQLException {
		synchronized(monitored_storeVerified){
			return _monitored_storeVerified(sync, arrival_date);
		}
	}
	private long _monitored_storeVerified(Calendar arrival_date) throws P2PDDSQLException {
		return _monitored_storeVerified(true, arrival_date);
	}
	private long _monitored_storeVerified(boolean sync, Calendar arrival_date) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("D_Witness:storeVerified: start arrival="+Encoder.getGeneralizedTime(arrival_date));
		
		if((this.organization_ID == null ) && (this.global_organization_ID != null))
			this.organization_ID = Util.getStringID(D_Organization.getLIDbyGID(this.global_organization_ID));
		
		if(witnessing_constituentID <=0 )
			witnessing_constituentID = D_Constituent.getLIDFromGID(this.witnessing_global_constituentID, Util.Lval(this.organization_ID));
		if(witnessing_constituentID <= 0){
			if(_DEBUG) System.out.println("D_Witness:storeVerified: no signer!");
			return -1;
		}
		if((witnessed_neighborhoodID <=0 ) && (witnessed_global_neighborhoodID!=null))
			witnessed_neighborhoodID = D_Neighborhood.getLIDFromGID(witnessed_global_neighborhoodID, Util.Lval(organization_ID));
		if((witnessed_constituentID <=0 ) && (witnessed_global_constituentID != null))
			witnessed_constituentID = D_Constituent.getLIDFromGID(this.witnessed_global_constituentID, Util.Lval(this.organization_ID));
		if(DEBUG) System.out.println("D_Witness:storeVerified: fixed local="+this);
		if((witnessed_constituentID <= 0) && (witnessed_neighborhoodID <= 0)){
			if(_DEBUG) System.out.println("D_Witness:storeVerified: no target!");
			return -1;
		}
		
		String[] _old_date= new String[1];
		if ((this.witnessID <= 0) && (this.global_witness_ID != null))
			this.witnessID = getLocalIDforGlobal(this.global_witness_ID, _old_date);
		else
			_old_date[0] = getDateFor(witnessID);
		
		String sql, params[];
		if (witnessed_neighborhoodID <= 0) {
			sql =
			"SELECT "+table.witness.witness_ID+", "+table.witness.creation_date+
			" FROM "+table.witness.TNAME+
			" WHERE "+table.witness.source_ID+"=? AND "+table.witness.target_ID+"=?;";
			params = new String[]{Util.getStringID(witnessing_constituentID), Util.getStringID(witnessed_constituentID)};
		} else {
			sql =
				"SELECT "+table.witness.witness_ID+", "+table.witness.creation_date+
				" FROM "+table.witness.TNAME+
				" WHERE "+table.witness.source_ID+"=? AND "+table.witness.neighborhood_ID+"=?;";			
			params = new String[]{Util.getStringID(witnessing_constituentID), Util.getStringID(witnessed_neighborhoodID)};
		}
		ArrayList<ArrayList<Object>> __e = Application.db.select(sql, params, DEBUG);
		long result;
		if ((__e.size() >= 1) && (this.witnessID <= 0)) {
			//Util.printCallPath("Inconsistent Presence of witnessing: "+sql+" -> "+this);
			this.witnessID = Util.lval(__e.get(0));
		}
		//if((__e.size()==0) && (this.witnessID>0)) Util.printCallPath("Inconsistent Absence of witnessing"); // normal on temp
		/*
		if(!((e.size()>0)^(this.witnessID<=0))){
			Util.printCallPath("Inconsistent Presence of witnessing");
			System.err.println("Data="+this+"\n vs ");
			if(e.size()>0)System.err.println("wID"+Util.getString(e.get(0).get(0)));
		}
		*/
		if (witnessID <= 0) { //||(e.size()==0)) {
			if (DEBUG) System.out.println("D_Witness:storeVerified:inserting");
			try {
				result = witnessID =Application.db.insert(sync, table.witness.TNAME, 
					new String[]{
					table.witness.global_witness_ID,
					table.witness.hash_witness_alg,
					table.witness.signature,
					table.witness.category,
					table.witness.neighborhood_ID,
					table.witness.sense_y_n,
					table.witness.sense_y_trustworthiness,
					table.witness.category_trustworthiness,
					table.witness.statements,
					table.witness.source_ID,
					table.witness.target_ID,
					table.witness.creation_date,
					table.witness.arrival_date
					}, 
					new String[]{
					global_witness_ID,
					hash_alg,
					Util.stringSignatureFromByte(signature),
					witness_eligibility_category,
					Util.getStringID(witnessed_neighborhoodID),
					sense_y_n+"",
					sense_y_trustworthiness+"",
					this.witness_trustworthiness_category,
					((this.statements==null)?null:Util.stringSignatureFromByte(statements.getEncoder().getBytes())),
					Util.getStringID(witnessing_constituentID),
					Util.getStringID(witnessed_constituentID),
					Encoder.getGeneralizedTime(creation_date),
					Encoder.getGeneralizedTime(arrival_date)
					}, DEBUG); 
			}catch(Exception e2){
				if(_DEBUG) System.out.println("D_Witness:storeVerified: failed hash="+this.global_witness_ID);
				e2.printStackTrace();
				D_Witness old = new D_Witness(global_witness_ID);
				if(_DEBUG) System.out.println("D_Witness:storeVerified: old witness is="+old);
				if((this.global_witness_ID!=null)&&(this.witnessID <= 0)) {
					result = this.witnessID = D_Witness.getLocalIDforGlobal(global_witness_ID);
					if(_DEBUG) System.out.println("D_Witness:storeVerified: failed: reget id==" + this.witnessID);
				}else result =-1;
			}
		} else {
			long __witnessID = Util.lval(__e.get(0).get(0), -1);
			if(__witnessID != witnessID) {
				Util.printCallPath("D_Witness:storeVerified:inconsistent ids: "+__witnessID+" vs "+witnessID);
			}
			result = witnessID;
			String old_date = _old_date[0]; //Util.getString(e.get(0).get(0));
			String _creation_date=Encoder.getGeneralizedTime(creation_date);
			if ((old_date == null) || _creation_date.compareTo(old_date) > 0) {
				if(DEBUG) System.out.println("D_Witness:storeVerified:updating");
				Application.db.update(sync, table.witness.TNAME, 
					new String[]{
					table.witness.global_witness_ID,
					table.witness.hash_witness_alg,
					table.witness.signature,
					table.witness.category,
					table.witness.neighborhood_ID,
					table.witness.sense_y_n,
					table.witness.sense_y_trustworthiness,
					table.witness.category_trustworthiness,
					table.witness.statements,
					table.witness.source_ID,
					table.witness.target_ID,
					table.witness.creation_date,
					table.witness.arrival_date
					}, new String[]{table.witness.witness_ID},
					new String[]{
					global_witness_ID,
					hash_alg,
					Util.stringSignatureFromByte(signature),
					witness_eligibility_category,
					Util.getStringID(witnessed_neighborhoodID),
					sense_y_n+"",
					sense_y_trustworthiness+"",
					this.witness_trustworthiness_category+"",
					((this.statements==null)?null:Util.stringSignatureFromByte(statements.getEncoder().getBytes())),
					Util.getStringID(witnessing_constituentID),
					Util.getStringID(witnessed_constituentID),
					_creation_date,
					Encoder.getGeneralizedTime(arrival_date),
					//selection
					Util.getStringID(witnessID)
					}, DEBUG);
			}else{
				if(DEBUG) System.out.println("D_Witness:storeVerified:Old data!");
			}
		}
		if(DEBUG) System.out.println("D_Witness:storeVerified: done result="+result);
		return result;
	}

	public static Hashtable<String, Integer> init_sense_trustworthiness() {
		Hashtable<String,Integer> result = new Hashtable<String,Integer>();
		for(int i=0; i<D_Witness.witness_categories_trustworthiness.length; i++)
			result.put(D_Witness.witness_categories_trustworthiness[i], D_Witness.witness_categories_trustworthiness_sense[i]);
		return result;
	}

	public static Hashtable<String, Integer> init_sense_eligibility() {
		Hashtable<String,Integer> result = new Hashtable<String,Integer>();
		for(int i=0; i<D_Witness.witness_categories.length; i++)result.put(D_Witness.witness_categories[i], D_Witness.witness_categories_sense[i]);
		return result;
	}

	/**
	 * Update the signature of "witness_ID"
	 * @param witness_ID
	 * @param signer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long readSignSave(long witness_ID, long signer_ID) throws P2PDDSQLException {
		D_Witness w=new D_Witness(witness_ID);
		ciphersuits.SK sk = util.Util.getStoredSK(D_Constituent.getGIDFromLID(signer_ID));
		w.sign(sk);
		return w.storeVerified();
	}

	public static long getLocalIDforGlobal(String global_witness_ID) throws P2PDDSQLException {
		return getLocalIDforGlobal(global_witness_ID, null);
	}
	/**
	 * Return creation date
	 * @param global_witness_ID2
	 * @param old_date
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long getLocalIDforGlobal(String global_witness_ID,
			String[] old_date) throws P2PDDSQLException {
		String sql =
				"SELECT "+table.witness.witness_ID+","+table.witness.creation_date+
				" FROM "+table.witness.TNAME+
				" WHERE "+table.witness.global_witness_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{global_witness_ID}, DEBUG);
		if(o.size()==0) return -1;
		if((old_date!=null)&&(old_date.length>0)) old_date[0] = Util.getString(o.get(0).get(1));
		return Util.lval(o.get(0).get(0), -1);
	}

	public static ArrayList<String> checkAvailability(ArrayList<String> witn,
			String orgID, boolean DBG) throws P2PDDSQLException {
		ArrayList<String> result = new ArrayList<String>();
		for (String cHash : witn) {
			if(!available(cHash, orgID, DBG)) result.add(cHash);
		}
		return result;
	}
	public static byte getASN1Type() {
		return TAG;
	}
	/**
	 * check blocking at this level
	 * @param cHash
	 * @param orgID
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static boolean available(String hash, String orgID, boolean DBG) throws P2PDDSQLException {
		return isGIDavailable(hash, DBG)==1;
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
		int r;
		String sql = 
			"SELECT "+table.witness.witness_ID+","+table.witness.signature+
			" FROM "+table.witness.TNAME+
			" WHERE "+table.witness.global_witness_ID+"=? ;";
			//" AND "+table.constituent.organization_ID+"=? "+
			//" AND ( "+table.constituent.sign + " IS NOT NULL " +
			//" OR "+table.constituent.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{gID}, DEBUG||DBG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(!result) r = 0;
		else {
			String signature = Util.getString(a.get(0).get(1));
			if((signature!=null) && (signature.length()!=0)) r=1;
			else r = -1;
		}
		if(DEBUG||DBG) System.out.println("D_Witness:available: "+gID+" result = "+r);
		return r;
	}
	
	public static void _main(String[] args) {
		try {
			/*
			for(int a=1;a<10;a++)
				for(int b=0;b<10;b++) {
					int x;
					if((x=a*101100+b*10010+8)%1989 == 0) 
						System.out.println("Solution: "+x);
				}
			*/
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			
			/*
			D_Witness w = new D_Witness();
			try {
				Fill_database.add_witness(1l);
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
			}

			*/
			if(args.length>1){readSignSave(1,1); if(true) return;}
			
			long i=1;
			if(args.length>0)
				i = Long.parseLong(args[0]);
			D_Witness c=new D_Witness(i);
			if(!c.verifySignature()) System.out.println("\n************Signature Failure\n**********\nread="+c);
			else System.out.println("\n************Signature Pass\n**********\nread="+c);
			Decoder dec = new Decoder(c.getEncoder().getBytes());
			D_Witness d = new D_Witness().decode(dec);
			Calendar arrival_date = d.arrival_date=Util.CalendargetInstance();
			//if(d.global_organization_ID==null) d.global_organization_ID = OrgHandling.getGlobalOrgID(d.organization_ID);
			if(!d.verifySignature()) System.out.println("\n************Signature Failure\n**********\nrec="+d);
			else System.out.println("\n************Signature Pass\n**********\nrec="+d);
			//return;
			d.global_witness_ID = d.make_ID();
			//d.storeVerified(arrival_date);
			
			//throw new ASN1DecoderFail("");
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	   
    public static void witness(D_Constituent c){
    	
    	String scores[] = Application_GUI.getWitnessScores();
    	if (scores == null) return;
    	String witness_category = scores[0];
    	String witness_category_trustworthiness = scores[1];
    	
    	//int sense;
    	//int wItemIx = dialog.witness_category.getSelectedIndex();
    	//if(wItemIx >= dialog.first_negative){
    	//	sense = 0;
    	//}else sense=1;
    	String gcd = c.getGID();
     	try {
     		/*
     		ConstituentsModel model = Application.constituents.tree.getModel();
     		long myself_LID = model.getConstituentIDMyself();
     		long myself_GID = model.getConstituentGIDMyself();
    		long organizationID = model.getOrganizationID();//organizationID;
    		String organizationGID = model.getOrgGID();//D_Organization.getGlobalOrgID(organizationID+"");
    		*/
    		D_Constituent myself = Application_GUI.getMeConstituent(); //MainFrame.status.getMeConstituent();
    		if (myself == null) return;
    		long myself_LID = myself.getLID();
    		String myself_GID = myself.getGID();
    		//long organizationID = Util.lval(myself.organization_ID);
    		String organizationGID = myself.getOrganizationGID();
    		
    		ArrayList<ArrayList<Object>> sel;
    		String sql="select "+table.witness.witness_ID+" from "+table.witness.TNAME+" where " +
    		table.witness.source_ID+"=? and "+table.witness.target_ID+"=?;";
    		sel = Application.db.select(sql, 
    				new String[]{myself.getLIDstr(),
    				c.getLIDstr()});
    		if(sel.size()>0){
    			Application.db.delete(table.witness.TNAME,
    					new String[]{table.witness.source_ID,table.witness.target_ID}, 
    					new String[]{myself_LID+"",
    					c.getLIDstr()});
    		}
    		
    		Calendar creation_date = Util.CalendargetInstance();
    		
     		SK sk = DD.getConstituentSK(myself_LID);
    		
    		//String now = Util.getGeneralizedTime();		
    		D_Witness wbw = new D_Witness();
    		wbw.global_organization_ID(organizationGID);
    		wbw.witnessed_constituentID = c.getLID();
    		wbw.witnessed_global_constituentID = c.getGID();
    		wbw.witnessing_global_constituentID = myself_GID;
    		wbw.witnessing_constituentID = myself_LID;
    		wbw.witness_eligibility_category = witness_category;
    		//wbw.sense_y_n = sense;
    		wbw.sense_y_n = sense_eligibility.get(witness_category).intValue();
    		//System.out.println("ConstitAdd:: "+Util.concatSI(ConstituentsAdd.sense_eligibility, ":::", "NNN"));
    		//System.out.println("ConstitAdd:: "+witness_category+" -> "+wbw.sense_y_n);
    		wbw.witness_trustworthiness_category = witness_category_trustworthiness;
    		if(!Util.emptyString(witness_category_trustworthiness))
    			wbw.sense_y_trustworthiness = D_Witness.sense_trustworthiness.get(witness_category_trustworthiness).intValue();
    		else wbw.sense_y_trustworthiness = D_Witness.UNKNOWN;
    		wbw.creation_date = creation_date;
    		wbw.arrival_date = creation_date;
    		wbw.global_witness_ID = wbw.make_ID();
        	if(DEBUG) System.out.println("CostituentsAction: addConst: signing="+wbw);
    		wbw.sign(sk);
    		long withID = wbw.storeVerified();
       		if(DEBUG|| DD.TEST_SIGNATURES) {
       			D_Witness w_test = new D_Witness(withID);
       			if(!w_test.verifySignature()){
       				if(_DEBUG) System.out.println("CostituentsAction: addConst: failed signing="+wbw+"\nvs\n"+w_test);    			
       			}
       		}

     	}catch(Exception ev) {
    		ev.printStackTrace();
    		return;
    	}
    }

	
	public static void main(String[] args){
		try{
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
			if(id<=0){
				l = Application.db.select(
						"SELECT "+table.witness.witness_ID+
						" FROM "+table.witness.TNAME, new String[]{}, DEBUG);
				for(ArrayList<Object> a: l){
					String m_ID = Util.getString(a.get(0));
					long ID = Util.lval(m_ID, -1);
					D_Witness m = new D_Witness(ID);
					if(m.signature==null){
						System.out.println("Fail:temporary "+m_ID+":"+m.witnessID+":"+m.organization_ID);
						continue;
					}
					if(m.global_witness_ID==null){
						System.out.println("Fail:edited "+m_ID+":"+m.witnessID+":"+m.organization_ID);
						continue;
					}
					if(!m.verifySignature()){
						System.out.println("Fail: "+m.witnessID+":"+m.organization_ID);
						if(fix){
							readSignSave(ID,ID);
						}
					}
				}
				return;
			}else{
				long ID = id;
				D_Witness m = new D_Witness(ID+"");
				if(fix)
					if(!m.verifySignature()) {
						System.out.println("Fixing: "+m.witnessID+":"+m.organization_ID);
						readSignSave(ID, ID);
					}
				else if(!m.verifySignature())
					System.out.println("Fail: "+m.witnessID+":"+m.organization_ID);
				return;
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
