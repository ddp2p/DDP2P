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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

import ciphersuits.SK;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;
import simulator.Fill_database;
import streaming.ConstituentHandling;
import streaming.NeighborhoodHandling;
import streaming.OrgHandling;
import streaming.RequestData;
import util.DBInterface;
import util.Summary;
import util.Util;

public
class D_Witness extends ASNObj implements Summary {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	public String hash_alg = "V0";
	public String global_witness_ID; // hash (global key)
	public String witness_category; // known, hearsay
	public String witnessed_global_neighborhoodID;
	public String witnessed_global_constituentID;
	public String witnessing_global_constituentID;
	public String global_organization_ID;
	public int sense_y_n;
	public byte[] signature;
	public Calendar creation_date;
	public Calendar arrival_date;
	public D_Constituent witnessed;
	public D_Constituent witnessing;
	
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

	public String toString() {
		return "D_Witness:["
		+"\n hash_alg="+hash_alg
		+"\n global_witness_ID="+global_witness_ID
		+"\n witness_category="+witness_category
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
		;
	}
	
	public String toSummaryString() {
		return "D_Witness:["
				//+"\n witnessing_global_constituentID="+witnessing_global_constituentID
				//+"\n witnessed_global_constituentID="+witnessed_global_constituentID
				+" witness_category="+witness_category
				//+"\n global_organization_ID="+global_organization_ID
				+"]";
	}
	
	public D_Witness(){}
	public D_Witness(long witnessID) throws SQLiteException{
		if(DEBUG) System.out.println("D_Witness:D_Witness: start wID="+witnessID);
		String sql = 
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
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=s."+table.constituent.organization_ID+") "+
			" WHERE w."+table.witness.witness_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> w = Application.db.select(sql, new String[]{Util.getStringID(witnessID)}, DEBUG);
		if(w.size()>0) init(w.get(0));
		if(DEBUG) System.out.println("D_Witness:D_Witness: Done");
	}
	public D_Witness(String witnessGID) throws SQLiteException{
		if(DEBUG) System.out.println("D_Witness:D_Witness: start wGID="+witnessGID);
		if(witnessGID==null) throw new D_NoDataException("null witnessGID");
		String sql = 
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
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=s."+table.constituent.organization_ID+") "+
			" WHERE w."+table.witness.global_witness_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> w = Application.db.select(sql, new String[]{witnessGID}, DEBUG);
		if(w.size()>0) init(w.get(0));
		else throw new D_NoDataException("absent witnessGID:"+witnessGID);
		if(DEBUG) System.out.println("D_Witness:D_Witness: Done");
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
		this.witnessing = new D_Constituent(this.witnessing_constituentID);
		if(this.witnessed_constituentID > 0)this.witnessed = new D_Constituent(this.witnessed_constituentID);
		if(this.witnessed_global_neighborhoodID != null) this.neighborhood = new D_Neighborhood(this.witnessed_global_neighborhoodID);
		if(DEBUG)System.out.println("D_Witness:init_all: done");
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
		if(DEBUG)System.out.println("D_Witness:init: start");
		hash_alg = Util.getString(w.get(table.witness.WIT_COL_HASH_ALG));
		global_witness_ID = Util.getString(w.get(table.witness.WIT_COL_GID));
		creation_date = Util.getCalendar(Util.getString(w.get(table.witness.WIT_COL_CREATION_DATE)));
		arrival_date = Util.getCalendar(Util.getString(w.get(table.witness.WIT_COL_ARRIVAL_DATE)));
		witness_category = Util.getString(w.get(table.witness.WIT_COL_CAT));
		witnessed_global_neighborhoodID = Util.getString(w.get(table.witness.WIT_FIELDS+0));
		witnessed_global_constituentID = Util.getString(w.get(table.witness.WIT_FIELDS+1));
		witnessing_global_constituentID = Util.getString(w.get(table.witness.WIT_FIELDS+2));
		global_organization_ID = Util.getString(w.get(table.witness.WIT_FIELDS+3));
		sense_y_n = Util.ival(w.get(table.witness.WIT_COL_SENSE),-1);
		signature = Util.byteSignatureFromString(Util.getString(w.get(table.witness.WIT_COL_SIGN)));
		if(DEBUG)System.out.println("D_Witness:init: sign="+Util.byteToHexDump(signature, 10)+"  from sign="+w.get(table.witness.WIT_COL_SIGN));
		witnessed_neighborhoodID = Util.lval(w.get(table.witness.WIT_COL_T_N_ID), -1);
		witnessed_constituentID = Util.lval(w.get(table.witness.WIT_COL_T_C_ID), -1);
		witnessing_constituentID = Util.lval(w.get(table.witness.WIT_COL_S_C_ID), -1);
		witnessID = Util.lval(w.get(table.witness.WIT_COL_ID), -1);
		if(DEBUG)System.out.println("D_Witness:init: done");
	}
	public D_Witness instance() throws CloneNotSupportedException{
		return new D_Witness();
	}	
	@Override
	public D_Witness decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		hash_alg = d.getFirstObject(true).getString();
		global_witness_ID = d.getFirstObject(true).getString();
		witness_category = d.getFirstObject(true).getString();
		witnessed_global_neighborhoodID = d.getFirstObject(true).getString();
		witnessed_global_constituentID = d.getFirstObject(true).getString();
		witnessing_global_constituentID = d.getFirstObject(true).getString();
		global_organization_ID = d.getFirstObject(true).getString();
		sense_y_n = d.getFirstObject(true).getInteger().intValue();
		creation_date = d.getFirstObject(true).getGeneralizedTimeCalender_();
		signature = d.getFirstObject(true).getBytes();
		if(d.getTypeByte() == DD.TAG_AC0) witnessing = new D_Constituent().decode(d.getFirstObject(true));
		if(d.getTypeByte() == DD.TAG_AC1) witnessed = new D_Constituent().decode(d.getFirstObject(true));
		return this;
	}
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(hash_alg));
		enc.addToSequence(new Encoder(global_witness_ID));
		enc.addToSequence(new Encoder(witness_category));
		enc.addToSequence(new Encoder(witnessed_global_neighborhoodID));
		enc.addToSequence(new Encoder(witnessed_global_constituentID));
		enc.addToSequence(new Encoder(witnessing_global_constituentID));
		enc.addToSequence(new Encoder(global_organization_ID));
		enc.addToSequence(new Encoder(new BigInteger(""+sense_y_n)));
		enc.addToSequence(new Encoder(creation_date));		
		enc.addToSequence(new Encoder(signature));
		if(witnessing!=null) enc.addToSequence(witnessing.getEncoder().setASN1Type(DD.TAG_AC0));
		if(witnessed!=null) enc.addToSequence(witnessed.getEncoder().setASN1Type(DD.TAG_AC1));
		return enc;
	}
	public Encoder getSignableEncoder() {
		Encoder enc = new Encoder().initSequence();
		//enc.addToSequence(new Encoder(global_organization_ID));
		enc.addToSequence(new Encoder(hash_alg));
		enc.addToSequence(new Encoder(global_witness_ID));
		enc.addToSequence(new Encoder(witness_category));
		enc.addToSequence(new Encoder(witnessed_global_neighborhoodID));
		enc.addToSequence(new Encoder(witnessed_global_constituentID));
		enc.addToSequence(new Encoder(witnessing_global_constituentID));
		enc.addToSequence(new Encoder(global_organization_ID));
		enc.addToSequence(new Encoder(new BigInteger(""+sense_y_n)));
		enc.addToSequence(new Encoder(creation_date));
		//enc.addToSequence(new Encoder(signature));
		return enc;
	}
	public Encoder getHashEncoder() {
		Encoder enc = new Encoder().initSequence();
		////enc.addToSequence(new Encoder(global_organization_ID));
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
		} catch (SQLiteException e) {
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
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		// return this.global_witness_ID =  
		return "W:"+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
	}
	public boolean verifySignature(){
		if(DEBUG) System.out.println("D_Witness:D_Witness: start");
		try {
			fillGlobals();
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		String pk_ID = this.witnessing_global_constituentID;//.submitter_global_ID;
		if((pk_ID == null) && (this.witnessing!=null) && (this.witnessing.global_constituent_id!=null))
			pk_ID = this.witnessing.global_constituent_id;
		if(pk_ID == null) return false;
		
		String newGID = make_ID();
		if(!newGID.equals(this.global_witness_ID)) {
			Util.printCallPath("WRONG EXTERNAL GID");
			if(DEBUG) System.out.println("D_Witness:verifySignature: WRONG HASH GID="+this.global_witness_ID+" vs="+newGID);
			if(DEBUG) System.out.println("D_Witness:verifySignature: WRONG HASH GID result="+false);
			return false;
		}
		
		boolean result = Util.verifySignByID(this.getSignableEncoder().getBytes(), pk_ID, signature);
		if(DEBUG){
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
	 * @throws SQLiteException
	 */
	public long store(RequestData rq) throws SQLiteException {
		if(DEBUG) out.println("D_Witness:store: start "+this.global_witness_ID);
		
		boolean locals = fillLocals(rq, true, true, true, true);
		if(!locals){
			if(_DEBUG) out.println("D_Witness:store: locals fail");
			return -1;
		}
		
		
		if(! verifySignature()) {
			if(! DD.ACCEPT_UNSIGNED_DATA){
				if(_DEBUG) out.println("D_Witness:store: sign fail exit");
				return -1;
			}
			if(_DEBUG) out.println("D_Witness:store: sign fail");

		}
		
		if ((this.witnessID <= 0) && (this.global_witness_ID != null))
			this.witnessID = getLocalIDforGlobal(this.global_witness_ID);
		if(this.witnessID >0 ) {
			String old_date = getDateFor(this.witnessID);
			String new_date = Encoder.getGeneralizedTime(this.creation_date);
			if(new_date.compareTo(old_date)<=0) {
				if(_DEBUG) out.println("D_Witness:store: date "+new_date+" old vs "+old_date);
				D_Witness old = new D_Witness(this.witnessID);
				if(_DEBUG) out.println("D_Witness:store: old= "+old);
				if(_DEBUG) out.println("D_Witness:store: nou= "+this);
				return -1;
			}
		}

		// find existing local IDs
		if((witnessing_constituentID <=0 ) && (witnessing_global_constituentID!=null))
			witnessing_constituentID = Util.lval(D_Constituent.getConstituentLocalID(witnessing_global_constituentID),0);
		if((witnessed_neighborhoodID <=0 ) && (witnessed_global_neighborhoodID!=null))
			witnessed_neighborhoodID = Util.lval(D_Neighborhood.getNeighborhoodLocalID(witnessed_global_neighborhoodID),0);
		if((witnessed_constituentID <=0 ) && (witnessed_global_constituentID != null))
			witnessed_constituentID = Util.lval(D_Constituent.getConstituentLocalID(witnessed_global_constituentID),0);

		if((witnessing_constituentID <=0 ) && (witnessing_global_constituentID!=null)) {
			witnessing_constituentID =
				D_Constituent.insertTemporaryConstituentGID(witnessing_global_constituentID, this.organization_ID);
			rq.cons.put(witnessing_global_constituentID,DD.EMPTYDATE);
		}
		if((witnessed_neighborhoodID <=0 ) && (witnessed_global_neighborhoodID!=null)) {
			witnessed_neighborhoodID =
				D_Neighborhood._insertTemporaryNeighborhoodGID(witnessed_global_neighborhoodID, this.organization_ID, 0);
			rq.neig.add(witnessed_global_neighborhoodID);
		}
		if((witnessed_constituentID <=0 ) && (witnessed_global_constituentID != null)) {
			witnessed_constituentID =
				D_Constituent.insertTemporaryConstituentGID(witnessed_global_constituentID, this.organization_ID);
			rq.cons.put(witnessed_global_constituentID,DD.EMPTYDATE);
		}
		
		return storeVerified();
		//rq.witn.remove(witnesses[k].global_witness_ID); // never needed
	}
	/**
	 * Get creation date in database for this ID
	 * @param witnessID
	 * @return
	 * @throws SQLiteException
	 */
	private static String getDateFor(long witnessID) throws SQLiteException {
		String sql = "SELECT "+table.witness.creation_date+" FROM "+table.witness.TNAME+
		" WHERE "+table.witness.witness_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{""+witnessID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public boolean fillLocals(RequestData rq, boolean tempOrg, boolean default_blocked_org, boolean tempConst, boolean tempNeig) throws SQLiteException {
		if((global_organization_ID==null)&&(organization_ID == null)){
			Util.printCallPath("cannot store witness with not orgGID");
			return false;
		}
		if((witnessing_global_constituentID==null)&&(witnessing_constituentID <= 0)){
			Util.printCallPath("cannot store witness with not submitterGID");
			return false;
		}
		
		if((global_organization_ID!=null)&&(organization_ID == null)){
			organization_ID = Util.getStringID(D_Organization.getLocalOrgID(global_organization_ID));
			if(tempOrg && (organization_ID == null)) {
				String orgGID_hash = D_Organization.getOrgGIDHashGuess(global_organization_ID);
				if(rq!=null)rq.orgs.add(orgGID_hash);
				organization_ID = Util.getStringID(D_Organization.insertTemporaryGID(global_organization_ID, orgGID_hash, default_blocked_org));
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
			
		if((this.witnessing_global_constituentID!=null) && (witnessing_constituentID <= 0)) {
			this.witnessing_constituentID = Util.lval(D_Constituent.getConstituentLocalID(witnessing_global_constituentID), this.witnessing_constituentID);
			if(tempConst && (witnessing_constituentID <= 0 ))  {
				String consGID_hash = D_Constituent.getGIDHashFromGID(witnessing_global_constituentID);
				if(rq!=null)rq.cons.put(consGID_hash,DD.EMPTYDATE);
				witnessing_constituentID = D_Constituent.insertTemporaryConstituentGID(witnessing_global_constituentID, organization_ID);
			}
			if(witnessing_constituentID <= 0){
				Util.printCallPath("cannot store witness with no constituent");
				return false;
			}
		}
		
		if((this.witnessed_global_constituentID!=null) && (witnessed_constituentID <= 0)) {
			this.witnessed_constituentID = Util.lval(D_Constituent.getConstituentLocalID(witnessed_global_constituentID), this.witnessed_constituentID);
			if(tempConst && (witnessed_constituentID <= 0 ))  {
				String consGID_hash = D_Constituent.getGIDHashFromGID(witnessed_global_constituentID);
				if(rq!=null)rq.cons.put(consGID_hash,DD.EMPTYDATE);
				witnessed_constituentID = D_Constituent.insertTemporaryConstituentGID(witnessed_global_constituentID, organization_ID);
			}
			if(witnessed_constituentID <= 0){
				Util.printCallPath("cannot store witness with no witnessed constituent");
				System.err.println("D_Witness: Since we cannot save temporary witnessed, we will not save witness: "+this);
				return false;
			}
		}
		
		if((this.witnessed_global_neighborhoodID!=null) && (witnessed_neighborhoodID <= 0)){
			this.witnessed_neighborhoodID = Util.lval(D_Neighborhood.getLocalID(witnessed_global_neighborhoodID), this.witnessed_neighborhoodID);
			if(tempNeig && (witnessed_neighborhoodID <= 0)) {
				if(rq!=null)rq.neig.add(witnessed_global_neighborhoodID);
				witnessed_neighborhoodID = D_Neighborhood._insertTemporaryNeighborhoodGID(witnessed_global_neighborhoodID, organization_ID, -1);
			}
			if(witnessed_neighborhoodID <= 0){
				Util.printCallPath("cannot store witness with no neighborhood");
				return false;
			}
		}
		
		return true;
	}
	private void fillGlobals() throws SQLiteException {
		if((this.witnessed_neighborhoodID > 0 ) && (this.witnessed_global_neighborhoodID == null))
			this.witnessed_global_neighborhoodID = D_Neighborhood.getGlobalID(Util.getStringID(this.witnessed_neighborhoodID));
		
		if((this.organization_ID != null ) && (this.global_organization_ID == null))
			this.global_organization_ID = D_Organization.getGlobalOrgID(this.organization_ID);

		if((this.witnessing_constituentID > 0 ) && (this.witnessing_global_constituentID == null))
			this.witnessing_global_constituentID = D_Constituent.getConstituentGlobalID(Util.getStringID(this.witnessing_constituentID));

		if((this.witnessed_constituentID > 0 ) && (this.witnessed_global_constituentID == null))
			this.witnessed_global_constituentID = D_Constituent.getConstituentGlobalID(Util.getStringID(this.witnessed_constituentID));
	}
	public long storeVerified() throws SQLiteException {
		if(DEBUG) out.println("D_Witness:storeVerified: start");
		Calendar now = Util.CalendargetInstance();
		return storeVerified(now);
	}
	public long storeVerified(Calendar arrival_date) throws SQLiteException {
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("D_Witness:storeVerified: start arrival="+Encoder.getGeneralizedTime(arrival_date));
		
		if((this.organization_ID == null ) && (this.global_organization_ID != null))
			this.organization_ID = Util.getStringID(D_Organization.getLocalOrgID(this.global_organization_ID));
		
		if(witnessing_constituentID <=0 )
			witnessing_constituentID = Util.lval(D_Constituent.getConstituentLocalID(this.witnessing_global_constituentID),0);
		if(witnessing_constituentID <= 0){
			if(_DEBUG) System.out.println("D_Witness:storeVerified: no signer!");
			return -1;
		}
		if((witnessed_neighborhoodID <=0 ) && (witnessed_global_neighborhoodID!=null))
			witnessed_neighborhoodID = Util.lval(D_Neighborhood.getNeighborhoodLocalID(witnessed_global_neighborhoodID),0);
		if((witnessed_constituentID <=0 ) && (witnessed_global_constituentID != null))
			witnessed_constituentID = Util.lval(D_Constituent.getConstituentLocalID(this.witnessed_global_constituentID),0);
		if(DEBUG) System.out.println("D_Witness:storeVerified: fixed local="+this);
		if((witnessed_constituentID <= 0) && (witnessed_neighborhoodID <= 0)){
			if(_DEBUG) System.out.println("D_Witness:storeVerified: no target!");
			return -1;
		}
		
		if ((this.witnessID <= 0) && (this.global_witness_ID != null))
			this.witnessID = getLocalIDforGlobal(this.global_witness_ID);
		
		String sql, params[];
		if(witnessed_neighborhoodID<=0) {
			sql =
			"SELECT "+table.witness.witness_ID+", "+table.witness.creation_date+
			" FROM "+table.witness.TNAME+
			" WHERE "+table.witness.source_ID+"=? AND "+table.witness.target_ID+"=?;";
			params = new String[]{witnessing_constituentID+"", witnessed_constituentID+""};
		}else{
			sql =
				"SELECT "+table.witness.witness_ID+", "+table.witness.creation_date+
				" FROM "+table.witness.TNAME+
				" WHERE "+table.witness.source_ID+"=? AND "+table.witness.neighborhood_ID+"=?;";			
			params = new String[]{witnessing_constituentID+"", witnessed_neighborhoodID+""};
		}
		ArrayList<ArrayList<Object>> e = Application.db.select(sql, params, DEBUG);
		long result;
		if((e.size()>=1)&&(this.witnessID<=0)) Util.printCallPath("Inconsistent Presence of witnessing");
		if(!(e.size()>=1) && !(this.witnessID<=0)) Util.printCallPath("Inconsistent Absence of witnessing");
		/*
		if(!((e.size()>0)^(this.witnessID<=0))){
			Util.printCallPath("Inconsistent Presence of witnessing");
			System.err.println("Data="+this+"\n vs ");
			if(e.size()>0)System.err.println("wID"+Util.getString(e.get(0).get(0)));
		}
		*/
		if(e.size()==0) {
			if(DEBUG) System.out.println("D_Witness:storeVerified:inserting");
			result = witnessID =Application.db.insert(table.witness.TNAME, 
				new String[]{
				table.witness.global_witness_ID,
				table.witness.hash_witness_alg,
				table.witness.signature,
				table.witness.category,
				table.witness.neighborhood_ID,
				table.witness.sense_y_n,
				table.witness.source_ID,
				table.witness.target_ID,
				table.witness.creation_date,
				table.witness.arrival_date
				}, 
				new String[]{
				global_witness_ID,
				hash_alg,
				Util.stringSignatureFromByte(signature),
				witness_category,
				witnessed_neighborhoodID+"",
				sense_y_n+"",
				witnessing_constituentID+"",
				witnessed_constituentID+"",
				Encoder.getGeneralizedTime(creation_date),
				Encoder.getGeneralizedTime(arrival_date)
				}, DEBUG); 
		}else{
			witnessID = Util.lval(e.get(0).get(0), 0);
			result = witnessID;
			String old_date = Util.getString(e.get(0).get(0));
			String _creation_date=Encoder.getGeneralizedTime(creation_date);
			if((old_date==null)||_creation_date.compareTo(old_date)>0) {
				if(DEBUG) System.out.println("D_Witness:storeVerified:updating");
				Application.db.update(table.witness.TNAME, 
					new String[]{
					table.witness.global_witness_ID,
					table.witness.hash_witness_alg,
					table.witness.signature,
					table.witness.category,
					table.witness.neighborhood_ID,
					table.witness.sense_y_n,
					table.witness.source_ID,
					table.witness.target_ID,
					table.witness.creation_date,
					table.witness.arrival_date
					}, new String[]{table.witness.witness_ID},
					new String[]{
					global_witness_ID,
					hash_alg,
					Util.stringSignatureFromByte(signature),
					witness_category,
					witnessed_neighborhoodID+"",
					sense_y_n+"",
					witnessing_constituentID+"",
					witnessed_constituentID+"",
					_creation_date,
					Encoder.getGeneralizedTime(arrival_date),
					//selection
					witnessID+""
					}, DEBUG);
			}else{
				if(_DEBUG) System.out.println("D_Witness:storeVerified:Old data!");
			}
		}
		if(DEBUG) System.out.println("D_Witness:storeVerified: done result="+result);
		return result;
	}
	/**
	 * Update the signature of "witness_ID"
	 * @param witness_ID
	 * @param signer_ID
	 * @return
	 * @throws SQLiteException
	 */
	public static long readSignSave(long witness_ID, long signer_ID) throws SQLiteException {
		D_Witness w=new D_Witness(witness_ID);
		ciphersuits.SK sk = util.Util.getStoredSK(D_Constituent.getConstituentGlobalID(Util.getStringID(signer_ID)));
		w.sign(sk);
		return w.storeVerified();
	}

	public static long getLocalIDforGlobal(String global_witness_ID) throws SQLiteException {
		String sql = "SELECT "+table.witness.witness_ID+" FROM "+table.witness.TNAME+
		" WHERE "+table.witness.global_witness_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{global_witness_ID}, DEBUG);
		if(o.size()==0) return -1;
		return Util.lval(o.get(0).get(0), -1);
	}

	public static ArrayList<String> checkAvailability(ArrayList<String> witn,
			String orgID, boolean DBG) throws SQLiteException {
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
	 * @throws SQLiteException
	 */
	private static boolean available(String hash, String orgID, boolean DBG) throws SQLiteException {
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
	 * @throws SQLiteException
	 */
	public static int isGIDavailable(String gID, boolean DBG) throws SQLiteException {
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
	
	public static void main(String[] args) {
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
		} catch (SQLiteException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}