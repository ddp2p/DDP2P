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
import java.util.Calendar;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public
class D_Translations extends ASNObj{
	private static final boolean _DEBUG = false;
	private static final boolean DEBUG = false;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	public String hash_alg;
	public String global_translation_ID;
	public String value;
	public String value_lang;
	public String value_ctx;
	public String translation;
	public String translation_lang;
	public String translation_charset;
	public String translation_flavor;
	public String global_constituent_ID;
	public String global_organization_ID;
	public Calendar creation_date;
	public byte[] signature;
	public Calendar arrival_date;
	public D_Constituent constituent;
	public D_Organization organization;
	public String translation_ID;
	public String submitter_ID;
	public String organization_ID;
	public static D_Translations getEmpty() {return new D_Translations();}
	public D_Translations(){}
	public D_Translations(long translationID) throws P2PDDSQLException{
		if(_DEBUG) System.out.println("WB_Translations:WB_Translations: start wID="+translationID);
		this.translation_ID = ""+translationID;
		String sql = 
			"SELECT "
			+ Util.setDatabaseAlias(net.ddp2p.common.table.translation.fields, "t")+" "
			+" FROM "+net.ddp2p.common.table.translation.TNAME+" AS t "+
			" WHERE t."+net.ddp2p.common.table.translation.translation_ID+"=?;"
			;
		ArrayList<ArrayList<Object>> w = Application.getDB().select(sql, new String[]{""+translationID}, _DEBUG);
		if(w.size()>0) init(w.get(0));
		if(_DEBUG) System.out.println("WB_Translations:WB_Translations: Done");
	}
	public D_Translations(String translationGID) throws P2PDDSQLException{
		if(_DEBUG) System.out.println("WB_Translations:WB_Translations: start wGID="+translationGID);
		this.global_translation_ID = translationGID;
		String sql = 
				"SELECT "
				+ Util.setDatabaseAlias(net.ddp2p.common.table.translation.fields, "t")+" "
				+" FROM "+net.ddp2p.common.table.translation.TNAME+" AS t "+
				" WHERE t."+net.ddp2p.common.table.translation.global_translation_ID+"=?;"
				;
		ArrayList<ArrayList<Object>> w = Application.getDB().select(sql, new String[]{translationGID}, _DEBUG);
		if(w.size()>0) init(w.get(0));
		if(_DEBUG) System.out.println("WB_Translations:WB_Translations: Done");
	}
	/**
	 * "SELECT "
				+ Util.setDatabaseAlias(table.translation.fields, "t")+" "
				+", c."+table.constituent.global_constituent_ID
				+", o."+table.organization.global_organization_ID
				+" FROM "+table.translation.TNAME+" AS t "+
				" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=t."+table.translation.submitter_ID+") "+
				" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=t."+table.translation.organization_ID+") "+
	 * @param w
	 */
	public D_Translations(ArrayList<Object> w) {
		init(w);
	}	
	void init(ArrayList<Object> w) {
		this.hash_alg = Util.getString(w.get(net.ddp2p.common.table.translation.T_HASH_ALG));
		this.global_translation_ID = Util.getString(w.get(net.ddp2p.common.table.translation.T_GID));
		this.value = Util.getString(w.get(net.ddp2p.common.table.translation.T_VALUE));
		this.value_lang = Util.getString(w.get(net.ddp2p.common.table.translation.T_VALUE_LANG));
		this.value_ctx = Util.getString(w.get(net.ddp2p.common.table.translation.T_VALUE_CTX));
		this.translation = Util.getString(w.get(net.ddp2p.common.table.translation.T_TRANSLATION));
		this.translation_lang = Util.getString(w.get(net.ddp2p.common.table.translation.T_TRANSLATION_LANG));
		this.translation_charset = Util.getString(w.get(net.ddp2p.common.table.translation.T_TRANSLATION_CHARSET));
		this.translation_flavor = Util.getString(w.get(net.ddp2p.common.table.translation.T_TRANSLATION_FLAVOR));
		this.organization_ID = Util.getString(w.get(net.ddp2p.common.table.translation.T_ORGANIZATION_ID));
		this.submitter_ID = Util.getString(w.get(net.ddp2p.common.table.translation.T_CONSTITUENT_ID));		
		this.signature = Util.byteSignatureFromString(Util.getString(w.get(net.ddp2p.common.table.translation.T_SIGN)));
		this.creation_date = Util.getCalendar(Util.getString(w.get(net.ddp2p.common.table.translation.T_CREATION_DATE)));
		this.arrival_date = Util.getCalendar(Util.getString(w.get(net.ddp2p.common.table.translation.T_ARRIVAL_DATE)));
		this.global_constituent_ID = D_Constituent.getGIDFromLID(this.submitter_ID); 
		this.global_organization_ID = D_Organization.getGIDbyLIDstr(organization_ID); 
	}
	public D_Translations instance() throws CloneNotSupportedException{
		return new D_Translations();
	}
	public Encoder getSignableEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(hash_alg));
		enc.addToSequence(new Encoder(global_translation_ID));
		enc.addToSequence(new Encoder(value));
		enc.addToSequence(new Encoder(value_lang));
		enc.addToSequence(new Encoder(value_ctx));
		enc.addToSequence(new Encoder(translation));
		enc.addToSequence(new Encoder(translation_lang));
		enc.addToSequence(new Encoder(translation_charset));
		enc.addToSequence(new Encoder(translation_flavor));
		enc.addToSequence(new Encoder(global_constituent_ID));
		enc.addToSequence(new Encoder(global_organization_ID));
		enc.addToSequence(new Encoder(creation_date));		
		return enc;
	}
	public Encoder getHashEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(value));
		enc.addToSequence(new Encoder(value_lang));
		enc.addToSequence(new Encoder(value_ctx));
		enc.addToSequence(new Encoder(translation));
		enc.addToSequence(new Encoder(translation_lang));
		enc.addToSequence(new Encoder(translation_charset));
		enc.addToSequence(new Encoder(translation_flavor));
		enc.addToSequence(new Encoder(global_constituent_ID));
		return enc;
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
		String repl_GID;
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(hash_alg));
		repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_translation_ID);
		enc.addToSequence(new Encoder(repl_GID));
		enc.addToSequence(new Encoder(value));
		enc.addToSequence(new Encoder(value_lang));
		enc.addToSequence(new Encoder(value_ctx));
		enc.addToSequence(new Encoder(translation));
		enc.addToSequence(new Encoder(translation_lang));
		enc.addToSequence(new Encoder(translation_charset));
		enc.addToSequence(new Encoder(translation_flavor));
		repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_constituent_ID);
		enc.addToSequence(new Encoder(repl_GID));
		repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_translation_ID);
		enc.addToSequence(new Encoder(repl_GID));
		enc.addToSequence(new Encoder(creation_date));		
		enc.addToSequence(new Encoder(signature));
		/**
		 * TODO
		 * May decide to comment encoding of "global_organization_ID" out completely, since the org_GID is typically
		 * available at the destination from enclosing fields, and will be filled out at expansion
		 * by ASNSyncPayload.expand at decoding.
		 * However, it is not that damaging when using compression, and can be stored without much overhead.
		 * So it is left here for now.  Test if you comment out!
		 */
		return enc;
	}
	@Override
	public Object decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		hash_alg = d.getFirstObject(true).getString();
		global_translation_ID = d.getFirstObject(true).getString();
		value = d.getFirstObject(true).getString();
		value_lang = d.getFirstObject(true).getString();
		value_ctx = d.getFirstObject(true).getString();
		translation = d.getFirstObject(true).getString();
		translation_lang = d.getFirstObject(true).getString();
		translation_charset = d.getFirstObject(true).getString();
		translation_flavor = d.getFirstObject(true).getString();
		global_constituent_ID = d.getFirstObject(true).getString();
		global_organization_ID = d.getFirstObject(true).getString();
		creation_date = d.getFirstObject(true).getGeneralizedTimeCalender_();
		signature = d.getFirstObject(true).getBytes();
		return this;
	}
	public String toString() {
		return "WB_Translations:" +
				"\n translation_ID="+translation_ID+
				"\n hash_alg="+hash_alg+
				"\n global_translation_ID="+global_translation_ID+
				"\n value="+value+
				"\n value_lang="+value_lang+
				"\n value_ctx="+value_ctx+
				"\n translation="+translation+
				"\n translation_lang="+translation_lang+
				"\n translation_charset="+translation_charset+
				"\n translation_flavor="+translation_flavor+
				"\n global_constituent_ID="+global_constituent_ID+
				"\n global_organization_ID="+global_organization_ID+
				"\n constituent_ID="+submitter_ID+
				"\n organization_ID="+organization_ID+
				"\n creation_date="+Encoder.getGeneralizedTime(creation_date)+
				"\n arrival_date="+Encoder.getGeneralizedTime(arrival_date)+
				"\n signature="+Util.byteToHexDump(signature);
	}
	public byte[] sign(String signer_GID) {
		if(_DEBUG) System.out.println("WB_Translations:sign: start signer="+signer_GID);
		net.ddp2p.ciphersuits.SK sk = net.ddp2p.common.util.Util.getStoredSK(signer_GID);
		if(sk==null) 
			if(_DEBUG) System.out.println("WB_Translations:sign: no signature");
		if(DEBUG) System.out.println("WB_Translations:sign: sign="+sk);
		return sign(sk);
	}
	/**
	 * Both store signature and returns it
	 * @param sk
	 * @return
	 */
	public byte[] sign(SK sk){
		if(_DEBUG) System.out.println("WB_Translations:sign: this="+this+"\nsk="+sk);
		signature = Util.sign(this.getSignableEncoder().getBytes(), sk);
		if(_DEBUG) System.out.println("WB_Translations:sign:got this="+Util.byteToHexDump(signature));
		return signature;
	}
	public String make_ID(){
		try {
			fillGlobals();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return "W:"+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
	}
	public boolean verifySignature(){
		if(_DEBUG) System.out.println("WB_Translations:verifySignature: start");
		String pk_ID = this.global_constituent_ID;
		if((pk_ID == null) && (this.constituent!=null) && (this.constituent.getGID()!=null))
			pk_ID = this.constituent.getGID();
		if(pk_ID == null) return false;
		String newGID = make_ID();
		if(!newGID.equals(this.global_translation_ID)) {
			Util.printCallPath("WB_Translations: WRONG EXTERNAL GID");
			if(DEBUG) System.out.println("WB_Translations:verifySignature: WRONG HASH GID="+this.global_translation_ID+" vs="+newGID);
			if(DEBUG) System.out.println("WB_Translations:verifySignature: WRONG HASH GID result="+false);
			return false;
		}
		boolean result = Util.verifySignByID(this.getSignableEncoder().getBytes(), pk_ID, signature);
		if(_DEBUG) System.out.println("WB_Translations:verifySignature: result wGID="+result);
		return result;
	}
	/**
	 * before call, one should set organization_ID and global_motionID
	 * @param rq
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long store(net.ddp2p.common.streaming.RequestData sol_rq, RequestData new_rq, D_Peer __peer) throws P2PDDSQLException {
		boolean default_blocked = false;
		boolean locals = fillLocals(new_rq, true, true, true);
		if(!locals) return -1;
		if(!this.verifySignature())
			if(! DD.ACCEPT_UNSIGNED_DATA)
				return -1;
		String _old_date[] = new String[1];
		if ((this.translation_ID == null) && (this.global_translation_ID != null))
			this.translation_ID = getLocalIDandDateforGID(this.global_translation_ID,_old_date);
		if(this.translation_ID != null ) {
			String old_date = _old_date[0];
			if(old_date != null) {
				String new_date = Encoder.getGeneralizedTime(this.creation_date);
				if(new_date.compareTo(old_date)<=0) return new Integer(translation_ID).longValue();
			}
		}
		net.ddp2p.common.config.Application_GUI.inform_arrival(this, __peer);
		if((this.organization_ID == null ) && (this.global_organization_ID != null))
			this.organization_ID = D_Organization.getLIDstrByGID_(this.global_organization_ID);
		if((this.organization_ID == null ) && (this.global_organization_ID != null)) {
			organization_ID = ""+net.ddp2p.common.data.D_Organization.insertTemporaryGID(global_organization_ID, __peer);
			new_rq.orgs.add(global_organization_ID);
		}
		if((this.submitter_ID == null ) && (this.global_constituent_ID != null))
			this.submitter_ID = D_Constituent.getLIDstrFromGID(this.global_constituent_ID, Util.Lval(this.organization_ID));
		if((this.submitter_ID == null ) && (this.global_constituent_ID != null)) {
			submitter_ID =
				""+D_Constituent.insertTemporaryGID(global_constituent_ID, null, Util.lval(this.organization_ID), __peer, default_blocked);
			new_rq.cons.put(global_constituent_ID,DD.EMPTYDATE);
		}
		if(sol_rq!=null)sol_rq.tran.add(this.global_translation_ID);
		return storeVerified();
	}
	private static String getLocalIDforGID(String global_translation_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("WB_Translations:getLocalIDforGID: start");
		if(global_translation_ID==null) return null;
		String sql = "SELECT "+net.ddp2p.common.table.translation.translation_ID+
		" FROM "+net.ddp2p.common.table.translation.TNAME+
		" WHERE "+net.ddp2p.common.table.translation.global_translation_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.getDB().select(sql, new String[]{global_translation_ID}, DEBUG);
		if(n.size()==0) return null;
		return Util.getString(n.get(0).get(0));
	}
	private static String getLocalIDandDateforGID(String global_translation_ID, String[]date) throws P2PDDSQLException {
		if(DEBUG) System.out.println("WB_Translations:getLocalIDforGID: start");
		if(global_translation_ID==null) return null;
		String sql = "SELECT "+net.ddp2p.common.table.translation.translation_ID+","+net.ddp2p.common.table.translation.creation_date+
		" FROM "+net.ddp2p.common.table.translation.TNAME+
		" WHERE "+net.ddp2p.common.table.translation.global_translation_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.getDB().select(sql, new String[]{global_translation_ID}, DEBUG);
		if(n.size()==0) return null;
		date[0] = Util.getString(n.get(0).get(1));
		return Util.getString(n.get(0).get(0));
	}
	public static long insertTemporaryGID(String trans_GID, String org_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("WB_Transl:insertTemporaryGID: start");
		return Application.getDB().insert(net.ddp2p.common.table.translation.TNAME,
				new String[]{net.ddp2p.common.table.translation.global_translation_ID, net.ddp2p.common.table.translation.organization_ID},
				new String[]{trans_GID, org_ID},
				_DEBUG);
	}
	private static String getDateFor(String transID) throws P2PDDSQLException {
		String sql = "SELECT "+net.ddp2p.common.table.translation.creation_date+" FROM "+net.ddp2p.common.table.translation.TNAME+
		" WHERE "+net.ddp2p.common.table.translation.translation_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.getDB().select(sql, new String[]{""+transID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public boolean fillLocals(RequestData new_rq, boolean tempOrg, boolean default_blocked_org, boolean tempConst) throws P2PDDSQLException {
		D_Peer __peer = null;
		boolean default_blocked = false;
		if((global_organization_ID==null)&&(organization_ID == null)){
			Util.printCallPath("cannot store witness with not orgGID");
			return false;
		}
		if((this.global_constituent_ID==null)&&(this.submitter_ID == null)){
			Util.printCallPath("cannot store witness with not submitterGID");
			return false;
		}
		if((global_organization_ID!=null)&&(organization_ID == null)){
			organization_ID = Util.getStringID(D_Organization.getLIDbyGID(global_organization_ID));
			if(tempOrg && (organization_ID == null)) {
				String orgGID_hash = D_Organization.getOrgGIDHashGuess(global_organization_ID);
				if(new_rq!=null)new_rq.orgs.add(orgGID_hash);
				organization_ID = Util.getStringID(D_Organization.insertTemporaryGID(global_organization_ID, orgGID_hash, default_blocked_org, __peer));
				if(default_blocked_org) return false;
			}
			if(organization_ID == null) return false;
		}
		if((this.global_constituent_ID!=null)&&(submitter_ID == null)){
			this.submitter_ID = D_Constituent.getLIDstrFromGID(global_constituent_ID, Util.Lval(this.organization_ID));
			if(tempConst && (submitter_ID == null ))  {
				String consGID_hash = D_Constituent.getGIDHashFromGID(global_constituent_ID);
				if(new_rq!=null)new_rq.cons.put(consGID_hash,DD.EMPTYDATE);
				submitter_ID = Util.getStringID(D_Constituent.insertTemporaryGID(global_constituent_ID, null, Util.lval(this.organization_ID), __peer, default_blocked));
			}
			if(submitter_ID == null) return false;
		}
		return true;
	}
	private void fillGlobals() throws P2PDDSQLException {		
		if((this.organization_ID != null ) && (this.global_organization_ID == null))
			this.global_organization_ID = D_Organization.getGIDbyLIDstr(this.organization_ID);
		if((this.submitter_ID != null ) && (this.global_constituent_ID == null))
			this.global_constituent_ID = D_Constituent.getGIDFromLID(this.submitter_ID);
	}
	/**
	 * Store setting arrival time to now
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long storeVerified() throws P2PDDSQLException {
		Calendar now = Util.CalendargetInstance();
		return storeVerified(now);
	}
	/**
	 * It will refuse to create new entries if it it has the same global_ID (value&translation for given constituent)
	 * @param arrival_date
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long storeVerified(Calendar arrival_date) throws P2PDDSQLException {
		long result = -1;
		if(_DEBUG) System.out.println("WB_Translations:storeVerified: start arrival="+Encoder.getGeneralizedTime(arrival_date));
		if(this.submitter_ID == null )
			submitter_ID = D_Constituent.getLIDstrFromGID(this.global_constituent_ID, Util.Lval(this.organization_ID));
		if(submitter_ID == null){
			if(DEBUG) System.out.println("WB_Translations:storeVerified: no signer!");
			return -1;
		}
		if((this.organization_ID == null ) && (this.global_organization_ID != null))
			this.organization_ID = D_Organization.getLIDstrByGID_(this.global_organization_ID);
		if((this.translation_ID == null ) && (this.global_translation_ID != null))
			this.translation_ID = getLocalID(this.global_translation_ID);
		if(_DEBUG) System.out.println("WB_Translations:storeVerified: fixed local="+this);
		String params[] = new String[net.ddp2p.common.table.translation.T_FIELDS];
		params[net.ddp2p.common.table.translation.T_HASH_ALG] = this.hash_alg;
		params[net.ddp2p.common.table.translation.T_GID] = this.global_translation_ID;
		params[net.ddp2p.common.table.translation.T_VALUE] = this.value;
		params[net.ddp2p.common.table.translation.T_VALUE_LANG] = this.value_lang;
		params[net.ddp2p.common.table.translation.T_VALUE_CTX] = this.value_ctx;
		params[net.ddp2p.common.table.translation.T_TRANSLATION] = this.translation;
		params[net.ddp2p.common.table.translation.T_TRANSLATION_LANG] = this.translation_lang;
		params[net.ddp2p.common.table.translation.T_TRANSLATION_CHARSET] = this.translation_charset;
		params[net.ddp2p.common.table.translation.T_TRANSLATION_FLAVOR] = this.translation_flavor;
		params[net.ddp2p.common.table.translation.T_CONSTITUENT_ID] = this.submitter_ID;
		params[net.ddp2p.common.table.translation.T_ORGANIZATION_ID] = this.organization_ID;
		params[net.ddp2p.common.table.translation.T_SIGN] = Util.stringSignatureFromByte(signature);
		params[net.ddp2p.common.table.translation.T_CREATION_DATE] = Encoder.getGeneralizedTime(this.creation_date);
		params[net.ddp2p.common.table.translation.T_ARRIVAL_DATE] = Encoder.getGeneralizedTime(arrival_date);
		if(this.translation_ID == null) {
			if(DEBUG) System.out.println("WB_Translations:storeVerified:inserting");
			result = Application.getDB().insert(net.ddp2p.common.table.translation.TNAME,
					net.ddp2p.common.table.translation.fields_array,
					params,
					DEBUG
					);
			translation_ID=""+result;
		}else{
			if(DEBUG) System.out.println("WB_Translations:storeVerified:inserting");
			params[net.ddp2p.common.table.translation.T_ID] = translation_ID;
			Application.getDB().update(net.ddp2p.common.table.translation.TNAME,
					net.ddp2p.common.table.translation.fields_noID_array,
					new String[]{net.ddp2p.common.table.translation.translation_ID},
					params,
					DEBUG
					);
			result = Util.lval(this.translation_ID, -1);
		}
		return result;
	}
	/**
	 * update signature
	 * @param witness_ID
	 * @param signer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long readSignSave(long translation_ID, long signer_ID) throws P2PDDSQLException {
		D_Translations w=new D_Translations(translation_ID);
		net.ddp2p.ciphersuits.SK sk = net.ddp2p.common.util.Util.getStoredSK(D_Constituent.getGIDFromLID(signer_ID));
		w.sign(sk);
		return w.storeVerified();
	}
	public static String getLocalID(String global_translation_ID) throws P2PDDSQLException {
		String sql = "SELECT "+net.ddp2p.common.table.translation.translation_ID+" FROM "+net.ddp2p.common.table.translation.TNAME+
		" WHERE "+net.ddp2p.common.table.translation.global_translation_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.getDB().select(sql, new String[]{global_translation_ID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
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
	private static boolean available(String hash, String orgID, boolean DBG) throws P2PDDSQLException {
		String sql = 
			"SELECT "+net.ddp2p.common.table.translation.translation_ID+
			" FROM "+net.ddp2p.common.table.translation.TNAME+
			" WHERE "+net.ddp2p.common.table.translation.global_translation_ID+"=? "+
			" AND "+net.ddp2p.common.table.translation.organization_ID+"=? "+
			" AND "+net.ddp2p.common.table.translation.signature + " IS NOT NULL ;";
		ArrayList<ArrayList<Object>> a = Application.getDB().select(sql, new String[]{hash, orgID}, DEBUG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_Translation:available: "+hash+" in "+orgID+" = "+result);
		return result;
	}
	public static byte getASN1Type() {
		return TAG;
	}
}
