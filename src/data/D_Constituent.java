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
import static util.Util._;
import handling_wb.BroadcastQueueHandled;
import handling_wb.PreparedMessage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.regex.Pattern;

import util.P2PDDSQLException;

import config.Application;
import config.DD;

import streaming.ConstituentHandling;
import streaming.NeighborhoodHandling;
import streaming.OrgHandling;
import streaming.RequestData;
import util.DBInterface;
import util.Summary;
import util.Util;
import wireless.BroadcastClient;

import ciphersuits.Cipher;
import ciphersuits.PK;
import ciphersuits.SK;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
D_Constituent ::= IMPLICIT [APPLICATION 48] SEQUENCE {
	version INTEGER DEFAULT 0,
	global_organization_ID PrintableString OPTIONAL,
	global_constituent_id [APPLICATION 0] PrintableString OPTIONAL,
	surname [APPLICATION 1] UTF8String OPTIONAL,
	forename [APPLICATION 15] UTF8String OPTIONAL,
	address [APPLICATION 2] SEQUENCE OF D_FieldValue OPTIONAL,
	email [APPLICATION 3] PrintableString OPTIONAL,
	creation_date [APPLICATION 4] GeneralizedDate OPTIONAL,
	global_neighborhood_ID [APPLICATION 10] PrintableString OPTIONAL,
	neighborhood [APPLICATION 5] SEQUENCE OF D_Neighborhood OPTIONAL,
	picture [APPLICATION 6] OCTET_STRING OPTIONAL,
	hash_alg PrintableString OPTIONAL,
	signature [APPLICATION 7] OCTET_STRING OPTIONAL,
	global_constituent_id_hash [APPLICATION 8] PrintableString OPTIONAL,
	certificate [APPLICATION 9] OCTET_STRING OPTIONAL,
	languages [APPLICATION 11] SEQUENCE OF PrintableString OPTIONAL,
	global_submitter_id [APPLICATION 12] PrintableString OPTIONAL,
	slogan [APPLICATION 13] UTF8String OPTIONAL,
	weight [APPLICATION 14] UTF8String OPTIONAL,
	valid_support [APPLICATION 16] D_Witness OPTIONAL,
	submitter [APPLICATION 17] D_Constituent OPTIONAL,
	external BOOLEAN,
	revoked BOOLEAN
}
Sign_D_Constituent ::= IMPLICIT [UNIVERSAL 48] SEQUENCE {
	version INTEGER DEFAULT 0,
	global_organization_ID PrintableString,
	global_constituent_id [APPLICATION 0] PrintableString OPTIONAL,
	surname [APPLICATION 1] UTF8String OPTIONAL,
	forename [APPLICATION 15] UTF8String OPTIONAL,
	address [APPLICATION 2] SEQUENCE OF D_FieldValue OPTIONAL,
	email [APPLICATION 3] PrintableString OPTIONAL,
	creation_date [APPLICATION 4] GeneralizedDate OPTIONAL,
	global_neighborhood_ID [APPLICATION 10] PrintableString OPTIONAL,
	-- neighborhood [APPLICATION 5] SEQUENCE OF D_Neighborhood OPTIONAL,
	picture [APPLICATION 6] OCTET_STRING OPTIONAL,
	hash_alg PrintableString OPTIONAL,
	-- signature [APPLICATION 7] OCTET_STRING OPTIONAL,
	-- global_constituent_id_hash [APPLICATION 8] PrintableString OPTIONAL,
	certificate [APPLICATION 9] OCTET_STRING OPTIONAL,
	languages [APPLICATION 11] SEQUENCE OF PrintableString OPTIONAL,
	global_submitter_id [APPLICATION 12] PrintableString OPTIONAL,
	slogan [APPLICATION 13] UTF8String OPTIONAL,
	weight [APPLICATION 14] UTF8String OPTIONAL,
	-- submitter [APPLICATION 15] D_Constituent OPTIONAL,
	external BOOLEAN,
	revoked BOOLEAN OPTIONAL -- only if not external and versions past 2
}
HashExtern_D_Constituent ::= IMPLICIT [UNIVERSAL 48] SEQUENCE {
	-- version INTEGER DEFAULT 0,
	global_organization_ID PrintableString,
	-- global_constituent_id [APPLICATION 0] PrintableString OPTIONAL,
	surname [APPLICATION 1] UTF8String OPTIONAL,
	forename [APPLICATION 15] UTF8String OPTIONAL,
	address [APPLICATION 2] SEQUENCE OF D_FieldValue OPTIONAL,
	email [APPLICATION 3] PrintableString OPTIONAL,
	-- creation_date [APPLICATION 4] GeneralizedDate OPTIONAL,
	global_neighborhood_ID [APPLICATION 10] PrintableString OPTIONAL,
	-- neighborhood [APPLICATION 5] SEQUENCE OF D_Neighborhood OPTIONAL,
	picture [APPLICATION 6] OCTET_STRING OPTIONAL,
	hash_alg PrintableString OPTIONAL,
	-- signature [APPLICATION 7] OCTET_STRING OPTIONAL,
	-- global_constituent_id_hash [APPLICATION 8] PrintableString OPTIONAL,
	certificate [APPLICATION 9] OCTET_STRING OPTIONAL,
	languages [APPLICATION 11] SEQUENCE OF PrintableString OPTIONAL,
	-- global_submitter_id [APPLICATION 12] PrintableString OPTIONAL,
	-- slogan [APPLICATION 13] UTF8String OPTIONAL,
	weight [APPLICATION 14] UTF8String OPTIONAL,
	-- submitter [APPLICATION 15] D_Constituent OPTIONAL,
	external BOOLEAN,
	-- revoked BOOLEAN OPTIONAL -- only if not external and versions past 2
}
*/
public class D_Constituent extends ASNObj implements Summary {
	private static final boolean _DEBUG = true;
	private static boolean DEBUG = false;
	public static final int EXPAND_NONE = 0;
	public static final int EXPAND_ONE = 1;
	public static final int EXPAND_ALL = 2;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	public int version = 2;
	public String global_constituent_id;//Printable
	public String global_submitter_id;//Printable
	public String global_constituent_id_hash;
	public String surname;//UTF8
	public String forename;//UTF8
	public String slogan;
	public boolean external;
	public String[] languages;
	public D_FieldValue[] address;
	public String email;//Printable
	public Calendar creation_date;
	public String weight;
	public boolean revoked;
	public D_Witness valid_support;
	
	public D_Neighborhood[] neighborhood;
	public D_Constituent submitter;
	public String global_neighborhood_ID;
	
	public String global_organization_ID;
	public String organization_ID;
	public byte[] picture;//OCT STR
	public String hash_alg;//Printable
	public byte[] signature; //OCT STR
	public byte[] certificate; //OCT STR

	public String submitter_ID;
	public String neighborhood_ID;
	
	public boolean blocked = false;
	public boolean requested = false;
	public boolean broadcasted = D_Organization.DEFAULT_BROADCASTED_ORG;
	private String constituent_ID;
	private long _constituent_ID;

	public static String c_fields_constituents = Util.setDatabaseAlias(table.constituent.fields_constituents,"c");
	public static String sql_get_const =
		"SELECT "+c_fields_constituents+",n."+table.neighborhood.global_neighborhood_ID+
		" FROM "+table.constituent.TNAME+" as c " +
		" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(c."+table.constituent.neighborhood_ID+" = n."+table.neighborhood.neighborhood_ID+") "+
		" LEFT JOIN "+table.organization.TNAME+" AS o ON(c."+table.constituent.organization_ID+" = o."+table.organization.organization_ID+") ";
	static String sql_get_const_by_ID =
		sql_get_const +
		" WHERE "+table.constituent.constituent_ID+" = ?;";
	static String sql_get_const_by_GID =
		sql_get_const +
		" WHERE "+table.constituent.global_constituent_ID+" = ? "+
		" OR "+table.constituent.global_constituent_ID_hash+" = ?;";
	//static byte[] strToBytes(String in) {return Util.hexToBytes(in.split(":"));}
	
	public D_Constituent instance() throws CloneNotSupportedException{return new D_Constituent();}
	public D_Constituent() {}
	/* 
	@SuppressWarnings("deprecation")
	public D_Constituent(registration.ASNConstituent constituent) {
		global_constituent_id = constituent.id;
		surname = constituent.surname;
		forename = constituent.forename;
		slogan = constituent.slogan;
		external = constituent.external;
		languages = D_OrgConcepts.stringArrayFromString(constituent.languages);
		address = constituent.postalAddress;
		email = constituent.email;
		creation_date = constituent.date;
		if(constituent.neighID != null){
			neighborhood = new D_Neighborhood[1];
			neighborhood[0].global_neighborhood_ID = constituent.neighID;
		}
		picture = constituent.gificon;
		signature = constituent.signature;
	}
	*/
	public D_Constituent(long c_ID) throws P2PDDSQLException {
		ArrayList<ArrayList<Object>> c;
		c = Application.db.select(sql_get_const_by_ID, new String[]{Util.getStringID(c_ID)}, DEBUG);
		load(c.get(0),EXPAND_ONE);
	}
	/**
	 * 
	 * @param c_GID 
	 * @param c_GIDh  / hash
	 * @param i neighborhoods, e.g. EXPAND_ONE
	 * @throws P2PDDSQLException
	 */
	public D_Constituent(String c_GID, String c_GIDh, int neigh) throws P2PDDSQLException, D_NoDataException {
		ArrayList<ArrayList<Object>> c;
		c = Application.db.select(sql_get_const_by_GID, new String[]{c_GID, c_GIDh}, DEBUG);
		if(c.size()==0) throw new D_NoDataException("No such constituent: +c_GID");
		load(c.get(0), neigh);
	}
	D_Constituent(ArrayList<Object> alk) throws P2PDDSQLException {
		load(alk,EXPAND_ONE);
	}
	public D_Constituent(String constituentID, int i) throws P2PDDSQLException {
		ArrayList<ArrayList<Object>> c;
		c = Application.db.select(sql_get_const_by_ID, new String[]{constituentID}, DEBUG);
		if(c.size() == 0) {
			System.err.println("D_Constituent: fail to find "+sql_get_const_by_ID+
					": cID="+constituentID);
			Util.printCallPath("Unexpected failure");
			return;
		}
		load(c.get(0),i);
	}
	static public D_Constituent get_WB_Constituent(ArrayList<Object> alk) throws P2PDDSQLException {
			D_Constituent c = new D_Constituent(alk);
			return c;
	}
	public boolean verifySignature(String _global_organization_id){
		String newGID;
		if(DEBUG) System.out.println("D_Constituents:verifySignature: orgGID="+_global_organization_id);
		try {
			this.fillGlobals();
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		if(global_organization_ID==null) global_organization_ID = _global_organization_id;
		if((global_organization_ID==null)&&(organization_ID!=null)) {
			try {
				global_organization_ID = D_Organization.getGlobalOrgID(organization_ID);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		
		if(external)
			if(!(newGID=this.makeExternalGID()).equals(this.global_constituent_id)){
				Util.printCallPath("WRONG EXTERNAL GID");
				if(DEBUG) System.out.println("WB_Constituents:verifySignature: WRONG HASH GID="+this.global_constituent_id+" vs="+newGID);
				if(DEBUG) System.out.println("WB_Constituents:verifySignature: WRONG HASH GID result="+false);
				return false;
			}
		
		String pk_ID = this.global_constituent_id;
		if(external){
			pk_ID = this.global_submitter_id;
			if(pk_ID == null) return true;
		}
		if(DEBUG) System.out.println("WB_Constituents:verifySignature: pk="+pk_ID);
		byte[] msg = getSignableEncoder(global_organization_ID).getBytes();
		boolean result = util.Util.verifySignByID(msg, pk_ID, signature);
		if(DEBUG) System.out.println("D_Constituents:verifySignature: result="+result);
		return result;
	}
	/**
	 * Before signing one may have to generate a new creation date, to guarantee change gets notified.
	 * It is done here
	 * @param global_organization_id
	 * @param signer_GID
	 * @return
	 */
	public byte[] signModified(String global_organization_id, String signer_GID) {
		creation_date = Util.CalendargetInstance();
		return sign(global_organization_id, signer_GID);
	}
	/**
	 * Before signing one may have to generate a new creation date, to guarantee change gets notified.
	 * use signModify
	 * @param global_organization_id
	 * @param signer_GID
	 * @return
	 */
	public byte[] sign(String global_organization_id, String signer_GID) {
		ciphersuits.SK sk = util.Util.getStoredSK(signer_GID);
		if(sk==null) 
			if(_DEBUG) System.out.println("WB_Constituents:sign: no signature");
		if(DEBUG) System.out.println("WB_Constituents:sign: sign="+sk);

		return sign(sk, global_organization_id);
	}
	public byte[] signModified(SK sk, String global_organization_id) {
		creation_date = Util.CalendargetInstance();
		return sign(sk, global_organization_id);
	}
	/**
	 * Before signing one may have to generate a new creation date, to guarantee change gets notified.
	 * use signModify
	 * @param sk
	 * @param global_organization_id
	 * @return
	 */
	public byte[] sign(SK sk, String global_organization_id){
		if(DEBUG) System.out.println("WB_Constituents:sign: orgGID="+global_organization_id);
		byte[] msg=this.getSignableEncoder(global_organization_id).getBytes();
		if(DEBUG) System.out.println("WB_Constituents:sign: msg["+msg.length+"]="+Util.byteToHex(msg));
		return signature = Util.sign(msg, sk);
	}
	/**
	 * Read the data of the constituent and sign it with the signature of the user
	 * For external constituents this changes gcd and gcdhash
	 * @param id
	 * @param submitter_id
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String readSignSave(long id, long submitter_id) throws P2PDDSQLException {
		if(DEBUG) System.out.println("WB_Constituents:readSignSave: subject="+id+" signer="+submitter_id);
		byte[] _signature = null;
		String gcd, gcdhash;
		D_Constituent wbc = new D_Constituent(id);
		
		if(id!=submitter_id){
			if(DEBUG) System.out.println("WB_Constituents:readSignSave: subject details external="+wbc);
			if(!wbc.external) Util.printCallPath("Should be external!");
			if(DD.SUBMITTER_REQUIRED_FOR_EXTERNAL)if(wbc.submitter_ID==null) Util.printCallPath("Should have a submitter!");
		}else{
			if(DEBUG) System.out.println("WB_Constituents:readSignSave: subject details myself="+wbc);
			if(wbc.external) Util.printCallPath("Shouldn't be external!");
			if(wbc.submitter_ID!=null) Util.printCallPath("Shouldn't have a submitter!");			
		}

		String global_organization_id = wbc.global_organization_ID;
		if(global_organization_id==null) global_organization_id = D_Constituent.getOrgGIDforConstituent(id);
		
		String submitter_GID = wbc.global_submitter_id;
		if(submitter_id==id) submitter_GID = wbc.global_constituent_id;
		if(submitter_GID==null) submitter_GID = D_Constituent.getConstituentGlobalID(submitter_id+"");
		
		
		Calendar now = Util.CalendargetInstance();
		//Calendar now = wbc.creation_date;
		String _now = Encoder.getGeneralizedTime(now);
		wbc.creation_date = now;
		
		if(DEBUG) System.out.println("WB_Constituents:readSignSave: subject details="+wbc);
		
		if(wbc.external) {
			wbc.global_constituent_id=null;
			wbc.global_constituent_id_hash=null;
			gcdhash=gcd = wbc.makeExternalGID();
			wbc.global_constituent_id = gcd;
			wbc.global_constituent_id_hash = gcdhash;
		}else{
			gcd = wbc.global_constituent_id;
			gcdhash = wbc.global_constituent_id_hash;
			wbc.global_constituent_id = gcd;
			wbc.global_constituent_id_hash = gcdhash;
		}

		String signature;
		if((submitter_id!=0)||(!wbc.external)){
			_signature = wbc.sign(global_organization_id, submitter_GID);
			if((_signature!=null)&&(_signature.length == 0)) _signature = null;
			signature = Util.stringSignatureFromByte(_signature);
		}else{
			_signature=null;
			signature = null;
		}
		Application.db.updateNoSync(table.constituent.TNAME,
				new String[]{table.constituent.sign, table.constituent.creation_date, table.constituent.arrival_date,
				table.constituent.global_constituent_ID, table.constituent.global_constituent_ID_hash},
				new String[]{table.constituent.constituent_ID},
				new String[]{signature, _now, _now,
				gcd, gcdhash,
				Util.getStringID(id)}, DEBUG);
		return gcd;
	}
	/**
	 * Get the org global ID for this constituent id
	 * @param id
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static String getOrgGIDforConstituent(long id) throws P2PDDSQLException {
		String result = null;
		String sql = "SELECT o."+table.organization.global_organization_ID+
			" FROM "+table.constituent.TNAME+" AS c "+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON (o."+table.organization.organization_ID+"=c."+table.constituent.organization_ID+") "+
			" WHERE c."+table.constituent.constituent_ID+"=?;";
		String params[] = new String[]{id+""};
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, params, DEBUG);
		if(o.size()==0) return result;
		result = Util.getString(o.get(0).get(0));
		return result;
	}
	public String toString() {
		String result="D_Constituent: [";
		result += "\n version="+version;
		result += "\n orgGID="+global_organization_ID;
		result += "\n costGID="+global_constituent_id;
		result += "\n surname="+surname;
		result += "\n forename="+forename;
		result += "\n address="+Util.nullDiscrim(address, Util.concat(address,":"));
		result += "\n email="+email;
		result += "\n crea_date="+Encoder.getGeneralizedTime(creation_date);
		result += "\n neigGID="+global_neighborhood_ID;
		result += "\n picture="+Util.nullDiscrim(picture, Util.stringSignatureFromByte(picture));
		result += "\n hash_alg="+hash_alg;
		result += "\n certif="+Util.nullDiscrim(certificate, Util.stringSignatureFromByte(certificate));
		result += "\n lang="+Util.nullDiscrim(languages, Util.concat(languages,":"));
		result += "\n submitterGID="+global_submitter_id;
		result += "\n submitter="+submitter;
		result += "\n slogan="+slogan;
		result += "\n weight="+weight;
		result += "\n external="+external;
		result += "\n revoked="+revoked;
		result += "\n signature="+Util.byteToHexDump(signature);
		if(neighborhood!=null) result += "\n neigh=["+Util.concat(neighborhood, "\n\n")+"]";
		return result+"]";
	}
	public String toSummaryString() {
		String result="WB_Constituent: [";
		if(global_organization_ID != null) result += "\n orgGID="+global_organization_ID;
		result += "\n surname="+surname;
		result += "\n forename="+forename;
		if(neighborhood!=null) result += "\n neigh=["+Util.concatSummary(neighborhood, "\n\n", null)+"]";
		return result+"]";
	}
	/**
	Sign_D_Constituent ::= IMPLICIT [UNIVERSAL 48] SEQUENCE {
		version INTEGER DEFAULT 0,
		global_organization_ID PrintableString,
		global_constituent_id [APPLICATION 0] PrintableString OPTIONAL,
		surname [APPLICATION 1] UTF8String OPTIONAL,
		forename [APPLICATION 15] UTF8String OPTIONAL,
		address [APPLICATION 2] SEQUENCE OF D_FieldValue OPTIONAL,
		email [APPLICATION 3] PrintableString OPTIONAL,
		creation_date [APPLICATION 4] GeneralizedDate OPTIONAL,
		global_neighborhood_ID [APPLICATION 10] PrintableString OPTIONAL,
		-- neighborhood [APPLICATION 5] SEQUENCE OF D_Neighborhood OPTIONAL,
		picture [APPLICATION 6] OCTET_STRING OPTIONAL,
		hash_alg PrintableString OPTIONAL,
		-- signature [APPLICATION 7] OCTET_STRING OPTIONAL,
		-- global_constituent_id_hash [APPLICATION 8] PrintableString OPTIONAL,
		certificate [APPLICATION 9] OCTET_STRING OPTIONAL,
		languages [APPLICATION 11] SEQUENCE OF PrintableString OPTIONAL,
		global_submitter_id [APPLICATION 12] PrintableString OPTIONAL,
		slogan [APPLICATION 13] UTF8String OPTIONAL,
		weight [APPLICATION 14] UTF8String OPTIONAL,
		-- submitter [APPLICATION 15] D_Constituent OPTIONAL,
		external BOOLEAN,
		revoked BOOLEAN OPTIONAL -- only if not external and versions past 2
	}
	 */
	/**
	 * SIGN(C',<C,O,i,r>)
	 * @param global_organization_id
	 * @return
	 */
	private Encoder getSignableEncoder(String global_organization_id) {
		this.global_organization_ID = global_organization_id;
		Encoder enc = new Encoder().initSequence();
		if(version>=2) enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(global_organization_id,Encoder.TAG_PrintableString));
		if(global_constituent_id!=null)enc.addToSequence(new Encoder(global_constituent_id,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(surname!=null) enc.addToSequence(new Encoder(surname,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC1));
		if(forename!=null) enc.addToSequence(new Encoder(forename,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC15));
		if(address!=null) enc.addToSequence(Encoder.getEncoder(address).setASN1Type(DD.TAG_AC2));
		if(email!=null) enc.addToSequence(new Encoder(email,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(creation_date!=null) enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC4));
		if(this.global_neighborhood_ID!=null)
			enc.addToSequence(new Encoder(this.global_neighborhood_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC10));
		//if(neighborhood!=null) enc.addToSequence(Encoder.getEncoder(neighborhood).setASN1Type(DD.TAG_AC5));
		if(picture!=null) enc.addToSequence(new Encoder(picture).setASN1Type(DD.TAG_AC6));
		if(version<2) if(hash_alg!=null) enc.addToSequence(new Encoder(hash_alg,false));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC7));
		//if(global_constituent_id_hash!=null)enc.addToSequence(new Encoder(global_constituent_id_hash,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC8));
		if(version<2) if(certificate!=null) enc.addToSequence(new Encoder(certificate).setASN1Type(DD.TAG_AC9));
		if(languages!=null) enc.addToSequence(Encoder.getStringEncoder(languages,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC11));
		if(version<2) if(global_submitter_id!=null)enc.addToSequence(new Encoder(global_submitter_id,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		if(slogan!=null) enc.addToSequence(new Encoder(slogan).setASN1Type(DD.TAG_AC13));
		if(weight!=null) enc.addToSequence(new Encoder(weight).setASN1Type(DD.TAG_AC14));
		enc.addToSequence(new Encoder(external));
		if(!external && version>=2) enc.addToSequence(new Encoder(revoked));
		return enc;
	}
	/**
	HashExtern_D_Constituent ::= IMPLICIT [UNIVERSAL 48] SEQUENCE {
		-- version INTEGER DEFAULT 0,
		global_organization_ID PrintableString,
		-- global_constituent_id [APPLICATION 0] PrintableString OPTIONAL,
		surname [APPLICATION 1] UTF8String OPTIONAL,
		forename [APPLICATION 15] UTF8String OPTIONAL,
		address [APPLICATION 2] SEQUENCE OF D_FieldValue OPTIONAL,
		email [APPLICATION 3] PrintableString OPTIONAL,
		-- creation_date [APPLICATION 4] GeneralizedDate OPTIONAL,
		global_neighborhood_ID [APPLICATION 10] PrintableString OPTIONAL,
		-- neighborhood [APPLICATION 5] SEQUENCE OF D_Neighborhood OPTIONAL,
		picture [APPLICATION 6] OCTET_STRING OPTIONAL,
		hash_alg PrintableString OPTIONAL,
		-- signature [APPLICATION 7] OCTET_STRING OPTIONAL,
		-- global_constituent_id_hash [APPLICATION 8] PrintableString OPTIONAL,
		certificate [APPLICATION 9] OCTET_STRING OPTIONAL,
		languages [APPLICATION 11] SEQUENCE OF PrintableString OPTIONAL,
		-- global_submitter_id [APPLICATION 12] PrintableString OPTIONAL,
		-- slogan [APPLICATION 13] UTF8String OPTIONAL,
		weight [APPLICATION 14] UTF8String OPTIONAL,
		-- submitter [APPLICATION 15] D_Constituent OPTIONAL,
		external BOOLEAN,
		-- revoked BOOLEAN OPTIONAL -- only if not external and versions past 2
	}
	 */
	/**
	 * Make GID for external: HASH(O,i)
	 * Without current GID, date, and slogan (externals should not have slogans?)
	 * @param global_organization_id
	 * @return
	 */
	private Encoder getHashEncoder(){//String global_organization_id) {
		//this.global_organization_ID = global_organization_id;
		Encoder enc = new Encoder().initSequence();
		
		enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString));
		//if(global_constituent_id!=null)enc.addToSequence(new Encoder(global_constituent_id,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(surname!=null) enc.addToSequence(new Encoder(surname,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC1));
		if(forename!=null) enc.addToSequence(new Encoder(forename,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC15));
		if(address!=null) enc.addToSequence(Encoder.getEncoder(address).setASN1Type(DD.TAG_AC2));
		if(email!=null)enc.addToSequence(new Encoder(email,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		//if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC4));
		if(this.global_neighborhood_ID!=null)
			enc.addToSequence(new Encoder(this.global_neighborhood_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC10));
		//if(neighborhood!=null) enc.addToSequence(Encoder.getEncoder(neighborhood).setASN1Type(DD.TAG_AC5));
		if(picture!=null) enc.addToSequence(new Encoder(picture).setASN1Type(DD.TAG_AC6));
		if(version<2)if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,false));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC7));
		//if(global_constituent_id_hash!=null)enc.addToSequence(new Encoder(global_constituent_id_hash,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC8));
		if(version<2) if(certificate!=null) enc.addToSequence(new Encoder(certificate).setASN1Type(DD.TAG_AC9));
		if(languages!=null) enc.addToSequence(Encoder.getStringEncoder(languages,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC11));
		//if(global_submitter_id!=null)enc.addToSequence(new Encoder(global_submitter_id,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		//if(slogan!=null)enc.addToSequence(new Encoder(slogan).setASN1Type(DD.TAG_AC13));
		if(weight!=null) enc.addToSequence(new Encoder(weight).setASN1Type(DD.TAG_AC14)); // weight should be signed by authority/witnessed
		enc.addToSequence(new Encoder(external));
		return enc;
		//if(true)return enc;
	}
	/**
	D_Constituent ::= IMPLICIT [APPLICATION 48] SEQUENCE {
		version INTEGER DEFAULT 0,
		global_organization_ID PrintableString OPTIONAL,
		global_constituent_id [APPLICATION 0] PrintableString OPTIONAL,
		surname [APPLICATION 1] UTF8String OPTIONAL,
		forename [APPLICATION 15] UTF8String OPTIONAL,
		address [APPLICATION 2] SEQUENCE OF D_FieldValue OPTIONAL,
		email [APPLICATION 3] PrintableString OPTIONAL,
		creation_date [APPLICATION 4] GeneralizedDate OPTIONAL,
		global_neighborhood_ID [APPLICATION 10] PrintableString OPTIONAL,
		neighborhood [APPLICATION 5] SEQUENCE OF D_Neighborhood OPTIONAL,
		picture [APPLICATION 6] OCTET_STRING OPTIONAL,
		hash_alg PrintableString OPTIONAL,
		signature [APPLICATION 7] OCTET_STRING OPTIONAL,
		global_constituent_id_hash [APPLICATION 8] PrintableString OPTIONAL,
		certificate [APPLICATION 9] OCTET_STRING OPTIONAL,
		languages [APPLICATION 11] SEQUENCE OF PrintableString OPTIONAL,
		global_submitter_id [APPLICATION 12] PrintableString OPTIONAL,
		slogan [APPLICATION 13] UTF8String OPTIONAL,
		weight [APPLICATION 14] UTF8String OPTIONAL,
		valid_support [APPLICATION 16] D_Witness OPTIONAL,
		submitter [APPLICATION 17] D_Constituent OPTIONAL,
		external BOOLEAN,
		revoked BOOLEAN
	}
	 */
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(version>=2) enc.addToSequence(new Encoder(version));
		if(global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString));
		if(global_constituent_id!=null)enc.addToSequence(new Encoder(global_constituent_id,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(surname!=null) enc.addToSequence(new Encoder(surname,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC1));
		if(forename!=null) enc.addToSequence(new Encoder(forename,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC15));
		if(address!=null) enc.addToSequence(Encoder.getEncoder(address).setASN1Type(DD.TAG_AC2));
		if(email!=null)enc.addToSequence(new Encoder(email,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC4));
		if(global_neighborhood_ID!=null) enc.addToSequence(new Encoder(global_neighborhood_ID, false).setASN1Type(DD.TAG_AC10));
		if(neighborhood!=null) enc.addToSequence(Encoder.getEncoder(neighborhood).setASN1Type(DD.TAG_AC5));
		if(picture!=null) enc.addToSequence(new Encoder(picture).setASN1Type(DD.TAG_AC6));
		if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,false));
		if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC7));
		if(global_constituent_id_hash!=null)enc.addToSequence(new Encoder(global_constituent_id_hash,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC8));
		if(certificate!=null) enc.addToSequence(new Encoder(certificate).setASN1Type(DD.TAG_AC9));
		if(languages!=null) enc.addToSequence(Encoder.getStringEncoder(languages,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC11));
		if(global_submitter_id!=null)enc.addToSequence(new Encoder(global_submitter_id,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		if(slogan!=null)enc.addToSequence(new Encoder(slogan).setASN1Type(DD.TAG_AC13));
		if(weight!=null)enc.addToSequence(new Encoder(weight).setASN1Type(DD.TAG_AC14));
		if(valid_support!=null) enc.addToSequence(valid_support.getEncoder().setASN1Type(DD.TAG_AC16));
		if(submitter!=null) enc.addToSequence(submitter.getEncoder().setASN1Type(DD.TAG_AC17));
		enc.addToSequence(new Encoder(external));
		if(!external)enc.addToSequence(new Encoder(revoked));
		enc.setASN1Type(D_Constituent.getASN1Type());
		return enc;
	}

	@Override
	public D_Constituent decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==Encoder.TAG_INTEGER) version = dec.getFirstObject(true).getInteger().intValue();
		else version = 0;
		if(dec.getTypeByte()==Encoder.TAG_PrintableString)global_organization_ID = dec.getFirstObject(true).getString();
		if(dec.getTypeByte()==DD.TAG_AC0)global_constituent_id = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if(dec.getTypeByte()==DD.TAG_AC1)surname = dec.getFirstObject(true).getString(DD.TAG_AC1);
		if(dec.getTypeByte()==DD.TAG_AC15)forename = dec.getFirstObject(true).getString(DD.TAG_AC15);
		if(dec.getTypeByte()==DD.TAG_AC2) address = dec.getFirstObject(true).getSequenceOf(D_FieldValue.getASN1Type(),new D_FieldValue[]{},new D_FieldValue());
		if(dec.getTypeByte()==DD.TAG_AC3) email = dec.getFirstObject(true).getString(DD.TAG_AC3);
		if(dec.getTypeByte()==DD.TAG_AC4) creation_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC4);
		if(dec.getTypeByte()==DD.TAG_AC10)global_neighborhood_ID = dec.getFirstObject(true).getString(DD.TAG_AC10);
		if(dec.getTypeByte()==DD.TAG_AC5) neighborhood = dec.getFirstObject(true).getSequenceOf(D_Neighborhood.getASN1Type(),new D_Neighborhood[]{},new D_Neighborhood());
		if(dec.getTypeByte()==DD.TAG_AC6) picture = dec.getFirstObject(true).getBytes(DD.TAG_AC6);
		if(dec.getTypeByte()==Encoder.TAG_PrintableString) hash_alg = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC7)signature = dec.getFirstObject(true).getBytes(DD.TAG_AC7);
		if(dec.getTypeByte()==DD.TAG_AC8)global_constituent_id_hash = dec.getFirstObject(true).getString(DD.TAG_AC8);
		if(dec.getTypeByte()==DD.TAG_AC9)certificate = dec.getFirstObject(true).getBytes(DD.TAG_AC9);
		if(dec.getTypeByte()==DD.TAG_AC11) languages = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC12) global_submitter_id = dec.getFirstObject(true).getString(DD.TAG_AC12);
		if(dec.getTypeByte()==DD.TAG_AC13) slogan = dec.getFirstObject(true).getString(DD.TAG_AC13);
		if(dec.getTypeByte()==DD.TAG_AC14) weight = dec.getFirstObject(true).getString(DD.TAG_AC14);
		if(dec.getTypeByte()==DD.TAG_AC16) valid_support = new D_Witness().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC17) submitter = new D_Constituent().decode(dec.getFirstObject(true));
		external = dec.getFirstObject(true).getBoolean();
		if(!external)
			if(dec.getTypeByte()==Encoder.TAG_BOOLEAN)
				revoked = dec.getFirstObject(true).getBoolean();
		try{
			getGIDHashFromGID(false);
		}catch(Exception e){global_constituent_id_hash = null;}
		return this;
	}
	/**
	 * 
	 * @return
	 */
	public String getGIDHashFromGID(){
		return getGIDHashFromGID(true);
	}
	/**
	 * 
	 * @param verbose : false for silent on expected exception
	 * @return
	 */
	public String getGIDHashFromGID(boolean verbose){
		if(DEBUG) System.out.println("D_Constituent: prepareGIDHash: start");
		if((global_constituent_id_hash==null)&&(global_constituent_id!=null)){
			if(external) global_constituent_id_hash = global_constituent_id;
			else global_constituent_id_hash = getGIDHashFromGID_NonExternalOnly(global_constituent_id, verbose);
		}	
		if(DEBUG) System.out.println("D_Constituent: prepareGIDHash: got="+global_constituent_id_hash);
		return global_constituent_id_hash;
	}
	/**
	 * Adds "R:" in front of the hash of the GID, for non_external (verbose on error)
	 * @param GID
	 * @return
	 */
	public static String getGIDHashFromGID_NonExternalOnly(String GID){
		return getGIDHashFromGID_NonExternalOnly(GID, true);
	}
	/**
	 *  Adds "R:" in front of the hash of the GID, for non_external
	 * @param GID
	 * @param verbose : false for silent on expected exceptions
	 * @return
	 */
	public static String getGIDHashFromGID_NonExternalOnly(String GID, boolean verbose){
		String hash = Util.getGIDhashFromGID(GID, verbose);
		if(hash==null) return null;
		return "R:"+hash;
	}
	/**
	 * unchanged if starts with "C:" (external) or "R:" (already a hash)
	 * @param s
	 * @return
	 */
	public static String getGIDHashFromGID(String s) {
		if(s.startsWith("C:")) return s; // it is an external
		if(s.startsWith("R:")) return s; // it is a GID hash
		String hash = D_Constituent.getGIDHashFromGID_NonExternalOnly(s);
		if(hash.length() != s.length()) return hash;
		return s;
	}
	/**
	 * Should be called after the data is initialized
	 * @return
	 */
	public String makeExternalGID() {
		try {
			fillGlobals();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		//return Util.getGlobalID("constituentID_neighbor",email+":"+forename+":"+surname);  
		return "C:"+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
	}
	private void fillGlobals() throws P2PDDSQLException {
		if((this.organization_ID != null ) && (this.global_organization_ID == null))
			this.global_organization_ID = D_Organization.getGlobalOrgID(this.organization_ID);
	}

	public boolean fillLocals(RequestData rq, boolean tempOrg, boolean default_blocked_org, boolean tempConst, boolean tempNeig) throws P2PDDSQLException {
		if((global_organization_ID==null)&&(organization_ID == null)){
			Util.printCallPath("cannot store constituent with not orgGID");
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
		
		if((this.global_submitter_id!=null)&&(submitter_ID == null)) {
			submitter_ID = D_Constituent.getConstituentLocalIDFromGID(global_submitter_id);
			if(tempConst && (submitter_ID == null))  {
				String consGID_hash = D_Constituent.getGIDHashFromGID(global_submitter_id);
				if(rq!=null)rq.cons.put(consGID_hash, DD.EMPTYDATE);
				submitter_ID = Util.getStringID(D_Constituent.insertTemporaryConstituentGID(global_submitter_id, organization_ID));
			}
			if(submitter_ID == null) return false;
		}
		
		if((this.global_neighborhood_ID!=null)&&(neighborhood_ID == null)) {
			this.neighborhood_ID = D_Neighborhood.getLocalID(global_neighborhood_ID);
			if(tempNeig && (neighborhood_ID == null)) {
				if(rq!=null)rq.neig.add(global_neighborhood_ID);
				neighborhood_ID = Util.getStringID(D_Neighborhood._insertTemporaryNeighborhoodGID(global_neighborhood_ID, organization_ID, -1));
			}
			if(neighborhood_ID == null) return false;
		}
		
		return true;
	}

	public long store(RequestData rq) throws P2PDDSQLException {
		
		boolean locals = fillLocals(rq, true, true, true, true);
		if(!locals) return -1;

		String now = Util.getGeneralizedTime();
		/*
		if (this.global_organization_ID == null){
			Util.printCallPath("cannot store constituent with not orgGID");
			return -1;
		}
		if ((this.organization_ID==null) && (this.global_organization_ID != null)) {
			this.organization_ID = D_Organization.getLocalOrgID_(global_organization_ID);
		}
		if(this.organization_ID == null) {
			String orgGID_hash = D_Organization.getOrgGIDHash(global_organization_ID);
			D_Organization.insertTemporaryGID(global_organization_ID, orgGID_hash);
			rq.orgs.add(orgGID_hash);
			Util.printCallPath("Unknow org in constituent"+this);
			return -1;
		}
		*/
		return store(this.global_organization_ID, this.organization_ID, now);
	}
	
	/**
	 * inserts or updates
	 * @param orgGID
	 * @param org_local_ID
	 * @param arrival_time
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long store(String orgGID, String org_local_ID, String arrival_time) throws P2PDDSQLException {
		return store(null, orgGID, org_local_ID, arrival_time);
	}
	public long store(PreparedMessage pm, String orgGID, String org_local_ID, String arrival_time) throws P2PDDSQLException {
		/**
		 * inserts or updates
		 * @param orgGID
		 * @param org_local_ID
		 * @param arrival_time
		 * @return
		 * @throws P2PDDSQLException
		 */
		
		if((this.global_constituent_id!=null)&&(this.constituent_ID==null))
			this.constituent_ID = D_Constituent.getConstituentLocalIDFromGID(global_constituent_id);
		if((this.global_constituent_id_hash!=null)&&(this.constituent_ID==null))
			this.constituent_ID = D_Constituent.getConstituentLocalIDByGID_or_Hash(this.global_constituent_id_hash, null);
		this._constituent_ID = Util.lval(constituent_ID, -1);
		if(!verifySignature(orgGID)){
			if(_DEBUG) System.out.println("ConstituentHandling:integrateNewConstituentData:Signature check failed using orgGID: "+orgGID);
			if(_DEBUG) System.out.println("ConstituentHandling:integrateNewConstituentData:Signature check failed for "+this);
			if(!DD.ACCEPT_UNSIGNED_PEERS){
				if(_DEBUG) System.out.println("ConstituentHandling:integrateNewConstituentData: exit due to signature failure");
				
				return _constituent_ID;
			}
		}
		return storeVerified(pm,orgGID, org_local_ID, arrival_time);
	}
	public void storeVerified() throws P2PDDSQLException {
		this.storeVerified(this.global_organization_ID, this.organization_ID, Util.getGeneralizedTime());
	}
	public long storeVerified(String orgGID, String org_local_ID, String arrival_time) throws P2PDDSQLException {
		return storeVerified(null, orgGID, org_local_ID, arrival_time);
	}
	public long storeVerified(PreparedMessage pm, String orgGID, String org_local_ID, String arrival_time) throws P2PDDSQLException {
		if(DEBUG) System.out.println("ConstituentHandling:storeVerified: start");

		if(global_constituent_id_hash == null)
			if(external){
				global_constituent_id_hash = global_constituent_id;
				if(_DEBUG) System.out.println("D_Constituent:storeVerified: ext hash="+global_constituent_id_hash);
			}else{
				global_constituent_id_hash = getGIDHashFromGID();
				if(_DEBUG) System.out.println("D_Constituent:storeVerified: act hash="+global_constituent_id_hash);
			}
		
		if((this.global_constituent_id!=null)&&(this.constituent_ID==null)){
			this.constituent_ID = D_Constituent.getConstituentLocalIDFromGID(global_constituent_id);
			if(_DEBUG) System.out.println("D_Constituent:storeVerified: id(GID)="+constituent_ID);
		}
		if((this.global_constituent_id_hash!=null)&&(this.constituent_ID==null)){
			this.constituent_ID = D_Constituent.getConstituentLocalIDByGID_or_Hash(this.global_constituent_id_hash, null);
			if(_DEBUG) System.out.println("D_Constituent:storeVerified: id(hash)="+constituent_ID);
		}
		this._constituent_ID = Util.lval(constituent_ID, -1);
		if((org_local_ID==null) && (orgGID!=null)) {
			org_local_ID = D_Organization.getLocalOrgID_(orgGID);
		}
		//String _submitter_ID = submitter_ID;
		if(external) {
			if(submitter_ID==null) {
				if(global_submitter_id!=null) {
					submitter_ID = D_Constituent.getConstituentLocalIDFromGID(global_submitter_id);
					if(submitter_ID == null) {
						submitter_ID = Util.getStringID(D_Constituent.insertTemporaryConstituentGID(global_submitter_id, org_local_ID));
					}
				}else{
					if(DD.SUBMITTER_REQUIRED_FOR_EXTERNAL) { // normally not
						Util.printCallPath("External without GID");
						return _constituent_ID;
					}
				}
			}
		}else{
			if(submitter_ID!=null) throw new RuntimeException("Cannot have submitter for non-external");
		}
		String _neighborhood_ID=null;
		if(this.neighborhood_ID != null)
			_neighborhood_ID = this.neighborhood_ID;
		else{
			if(global_neighborhood_ID!=null) {
				if(_neighborhood_ID == null)
					_neighborhood_ID = D_Neighborhood.getNeighborhoodLocalID(global_neighborhood_ID);
				if(_neighborhood_ID == null) 
					_neighborhood_ID = D_Neighborhood.insertTemporaryNeighborhoodGID(global_neighborhood_ID, org_local_ID);
			}
			neighborhood_ID = _neighborhood_ID;
		}
		String[]date = new String[1];
		boolean[]_old_revoked = new boolean[]{false};
		String[]_old_sign = new String[]{null};
		String constituent_ID_2 = D_Constituent.getConstituentLocalIDAndDateAndSignRevoked(this.global_constituent_id,this.global_constituent_id_hash, date, _old_sign, _old_revoked);
		if(constituent_ID==null) constituent_ID = constituent_ID_2;
		else {
			if(!constituent_ID.equals(constituent_ID_2))
				Util.printCallPath("Why:"+constituent_ID+" vs "+constituent_ID_2);
		}
		if(_old_revoked[0]) return Util.lval(constituent_ID, -1);
		String new_creation_date = Encoder.getGeneralizedTime(creation_date);
		if( (date[0] != null) && (date[0].compareTo(new_creation_date)>=0)){
			//if(D_Constituent.isGID_or_Hash_available(global_constituent_id, DEBUG)==1) {
			if(_old_sign[0]!=null) {
				if(DEBUG)System.out.println("D_Constituent: storeVerified: exit pre-existing  data:"+date[0]);
				return Util.lval(constituent_ID, -1);
			}
		}
		
		if(address!=null) {
			for(int k =0 ; k<address.length; k++) {
				if(address[k].field_extra_ID > 0) continue;
				
				address[k].field_extra_ID = Util.lval(D_FieldExtra.getFieldExtraID(address[k].field_extra_GID, Util.lval(org_local_ID,-1)), -1);
				if(address[k].field_extra_ID > 0) continue;
				if(!DD.ACCEPT_TEMPORARY_AND_NEW_CONSTITUENT_FIELDS) {
					Application.warning(_("Extra Field Type for constituent.")+" "+forename+", "+surname+"\n"+
						_("Constituent dropped for irregular field:")+" "+address[k],
						_("Unknow constituent info dropped"));
					return Util.lval(constituent_ID, -1);
				}
			}
		}
				
		String[] fields = table.constituent._fields_constituents.split(Pattern.quote(","));
		String[] params = new String[fields.length+((constituent_ID!=null)?1:0)];
		//params[table.constituent.CONST_COL_ID] = ;
		params[table.constituent.CONST_COL_GID] = global_constituent_id;
		params[table.constituent.CONST_COL_GID_HASH] = global_constituent_id_hash;
		params[table.constituent.CONST_COL_SURNAME] = surname;
		params[table.constituent.CONST_COL_FORENAME] = forename;
		params[table.constituent.CONST_COL_SLOGAN] = slogan;
		params[table.constituent.CONST_COL_EXTERNAL] = Util.bool2StringInt(external);
		params[table.constituent.CONST_COL_REVOKED] = Util.bool2StringInt(revoked);
		params[table.constituent.CONST_COL_VERSION] = ""+version;
		params[table.constituent.CONST_COL_LANG] = D_OrgConcepts.stringFromStringArray(languages);
		params[table.constituent.CONST_COL_EMAIL] = email;
		params[table.constituent.CONST_COL_PICTURE] = Util.stringSignatureFromByte(picture);
		params[table.constituent.CONST_COL_DATE] = new_creation_date; //Encoder.getGeneralizedTime(creation_date);
		params[table.constituent.CONST_COL_NEIGH] = _neighborhood_ID;
		params[table.constituent.CONST_COL_HASH_ALG] = hash_alg;
		params[table.constituent.CONST_COL_SIGNATURE] = Util.stringSignatureFromByte(signature);
		params[table.constituent.CONST_COL_CERTIF] = Util.stringSignatureFromByte(certificate);
		params[table.constituent.CONST_COL_ARRIVAL] = arrival_time;
		params[table.constituent.CONST_COL_ORG] = org_local_ID;
		params[table.constituent.CONST_COL_SUBMITTER] = submitter_ID;
		params[table.constituent.CONST_COL_OP] = "1";
		params[table.constituent.CONST_COL_BLOCKED] = Util.bool2StringInt(blocked);
		params[table.constituent.CONST_COL_REQUESTED] = Util.bool2StringInt(requested);
		params[table.constituent.CONST_COL_BROADCASTED] = Util.bool2StringInt(broadcasted);
		if(constituent_ID==null){
			if(DEBUG) System.out.println("ConstituentHandling:storeVerified: insert!");
			try{
				_constituent_ID=Application.db.insert(table.constituent.TNAME, fields, params, DEBUG);
			}catch(Exception e){
				if(_DEBUG) System.out.println("D_Constituent:storeVerified: failed hash="+global_constituent_id_hash);
				e.printStackTrace();
				if((this.global_constituent_id_hash!=null)&&(this.constituent_ID==null)){
					this.constituent_ID = D_Constituent.getConstituentLocalIDByGID_or_Hash(this.global_constituent_id_hash, null);
					if(_DEBUG) System.out.println("D_Constituent:storeVerified: failed reget id=="+this.constituent_ID);
				}
				this._constituent_ID = Util.lval(this.constituent_ID, -1);
			}
			constituent_ID = Util.getStringID(_constituent_ID);
			if(constituent_ID==null) return _constituent_ID;
		}else {
			if(DEBUG) System.out.println("ConstituentHandling:storeVerified: update!");
			//params[table.constituent.CONST_COLs] = constituent_ID;
			//params[table.constituent.CONST_COL_ID] = constituent_ID;
			if((date[0]==null)||(date[0].compareTo(params[table.constituent.CONST_COL_DATE])<0)) {
				params[params.length-1] = constituent_ID;
				Application.db.update(table.constituent.TNAME, fields, new String[]{table.constituent.constituent_ID}, params, DEBUG);
			}else{
				if(DEBUG) System.out.println("ConstituentHandling:storeVerified: not new data vs="+date[0]);				
			}
			_constituent_ID = new Integer(constituent_ID).longValue();
		}
		if(DEBUG) System.out.println("ConstituentHandling:storeVerified: stored constituent!");
		if((neighborhood!=null)&&(neighborhood.length>0)){
			for(int k=0; k<neighborhood.length; k++) {
				neighborhood[k].store(orgGID, org_local_ID, arrival_time);
			}
		}
		if(DEBUG) System.out.println("ConstituentHandling:storeVerified: store address!");
		long _organization_ID = Util.lval(org_local_ID, -1);
		try {
			D_FieldValue.store(address, constituent_ID, _organization_ID, DD.ACCEPT_TEMPORARY_AND_NEW_CONSTITUENT_FIELDS);
		} catch (ExtraFieldException e) {
			Application.db.update(table.constituent.TNAME, new String[]{table.constituent.sign},
					new String[]{table.constituent.constituent_ID},
					new String[]{null, constituent_ID}, DEBUG);
			e.printStackTrace();
			Application.warning(_("Extra Field Type for constituent.")+" "+forename+", "+surname+"\n"+
					_("Constituent dropped:")+" "+e.getLocalizedMessage(),
					_("Unknow constituent info dropped"));
		}
		
		synchronized(BroadcastClient.msgs_monitor) {
			if(BroadcastClient.msgs!=null) {
				D_Message dm = new D_Message();
				//if((this.constituent_ID != null ) && (this.global_constituent_ID == null))
				//this.global_constituent_ID = D_Constituent.getConstituentGlobalID(this.constituent_ID);
				if((this.organization_ID != null ) && (this.global_organization_ID == null))
					this.global_organization_ID = D_Organization.getGlobalOrgID(this.organization_ID);
				if((this.neighborhood_ID != null ) && (this.global_neighborhood_ID ==null))
					this.global_neighborhood_ID = D_Neighborhood.getNeighborhoodGlobalID(this.neighborhood_ID);
				dm.constituent = this; // may have to add GIDs
				String global_sender_ID = DD.getMyPeerGIDFromIdentity();//.getAppText(DD.APP_my_global_peer_ID);
				dm.sender = new D_PeerAddress();
				dm.sender.globalID = global_sender_ID;
				dm.sender.name = DD.getAppText(DD.APP_my_peer_name);
				
				if(pm!=null){
					pm.raw = dm.encode();
					pm.constituent_ID_hash.add(this.global_constituent_id_hash);
					pm.org_ID_hash = this.global_organization_ID;
					if(this.neighborhood!=null){
						for(int i=0;i<this.neighborhood.length;i++)
							pm.neighborhood_ID.add(this.neighborhood[i].global_neighborhood_ID );
					}
					BroadcastClient.msgs.registerRecent(pm, BroadcastQueueHandled.CONSTITUENT);
				}
				
			}
		}
		
		
		if(DEBUG) System.out.println("ConstituentHandling:storeVerified: return="+_constituent_ID);
		return _constituent_ID;
	}
	/**
	 * 	public static String c_fields_constituents = Util.setDatabaseAlias(table.constituent.fields_constituents,"c");
		public static String sql_get_const =
		"SELECT "+c_fields_constituents+",n."+table.neighborhood.global_neighborhood_ID+
		" FROM "+table.constituent.TNAME+" as c " +
		" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(c."+table.constituent.neighborhood_ID+" = n."+table.neighborhood.neighborhood_ID+") ";

	loads orgID and orgGID
	 * @param alk  result of the above select
	 * @param _neighborhoods  (0 means none, 1 means 1, 2 means all)
	 * @throws P2PDDSQLException
	 */
	public void load(ArrayList<Object> alk, int _neighborhoods) throws P2PDDSQLException {
		if(DEBUG) System.out.println("ConstituentHandling:load: neigh="+_neighborhoods);		
		String _constituentID = Util.getString(alk.get(table.constituent.CONST_COL_ID));
		constituent_ID = _constituentID; //Util.lval(_constituentID, -1);
		version = Util.ival(alk.get(table.constituent.CONST_COL_VERSION), 0);
		submitter_ID = Util.getString(alk.get(table.constituent.CONST_COL_SUBMITTER));
		if(submitter_ID !=null)
			global_submitter_id = D_Constituent.getConstituentGlobalID(submitter_ID);
		global_constituent_id = Util.getString(alk.get(table.constituent.CONST_COL_GID));
		global_constituent_id_hash = Util.getString(alk.get(table.constituent.CONST_COL_GID_HASH));
		organization_ID = Util.getString(alk.get(table.constituent.CONST_COL_ORG));
		global_organization_ID = D_Organization.getGlobalOrgID(organization_ID);
		surname = Util.getString(alk.get(table.constituent.CONST_COL_SURNAME));
		//if(DEBUG) System.out.println("ConstituentHandling:load: surname:"+surname+" from="+alk.get(table.constituent.CONST_COL_SURNAME));		
		forename = Util.getString(alk.get(table.constituent.CONST_COL_FORENAME));
		weight = Util.getString(alk.get(table.constituent.CONST_COL_WEIGHT));
		slogan = Util.getString(alk.get(table.constituent.CONST_COL_SLOGAN));
		if(DEBUG) System.out.println("WB_Constituent:load: external="+alk.get(table.constituent.CONST_COL_EXTERNAL));
		languages = D_OrgConcepts.stringArrayFromString(Util.getString(alk.get(table.constituent.CONST_COL_LANG)));
		address = D_FieldValue.getFieldValues(_constituentID);		
		email = Util.getString(alk.get(table.constituent.CONST_COL_EMAIL));
		creation_date = Util.getCalendar(Util.getString(alk.get(table.constituent.CONST_COL_DATE)));
		global_neighborhood_ID = Util.getString(alk.get(table.constituent.CONST_COLs+0));
		this.external = Util.stringInt2bool(alk.get(table.constituent.CONST_COL_EXTERNAL), false);
		this.revoked = Util.stringInt2bool(alk.get(table.constituent.CONST_COL_REVOKED), false);
		this.blocked = Util.stringInt2bool(alk.get(table.constituent.CONST_COL_BLOCKED),false);
		this.requested = Util.stringInt2bool(alk.get(table.constituent.CONST_COL_REQUESTED),false);
		this.broadcasted = Util.stringInt2bool(alk.get(table.constituent.CONST_COL_BROADCASTED), D_Organization.DEFAULT_BROADCASTED_ORG_ITSELF);
		
		if((global_neighborhood_ID!=null) && (_neighborhoods!=EXPAND_NONE)) {
			String nID = Util.getString(alk.get(table.constituent.CONST_COL_NEIGH));
			neighborhood = D_Neighborhood.getNeighborhoodHierarchy(global_neighborhood_ID, nID, _neighborhoods);
		}
		//picture = strToBytes(Util.getString(alk.get(table.constituent.CONST_COL_PICTURE)));
		picture = Util.byteSignatureFromString(Util.getString(alk.get(table.constituent.CONST_COL_PICTURE)));
		hash_alg = Util.getString(alk.get(table.constituent.CONST_COL_HASH_ALG));
		String _sgn = Util.getString(alk.get(table.constituent.CONST_COL_SIGNATURE));
		if("".equals(_sgn)) _sgn = null;
		signature = Util.byteSignatureFromString(_sgn);
		//certificate = strToBytes(Util.getString(alk.get(table.constituent.CONST_COL_CERTIF)));
		certificate = Util.byteSignatureFromString(Util.getString(alk.get(table.constituent.CONST_COL_CERTIF)));			
		//c.hash = Util.hexToBytes(Util.getString(al.get(k).get(table.constituent.CONST_COL_HASH)).split(Pattern.quote(":")));
		//c.cerReq = al.get(k).get(table.constituent.CONST_COL_CERREQ);
		//c.cert_hash_alg = al.get(k).get(table.constituent.CONST_COL_CERT_HASH_ALG);
		if(DEBUG) System.out.println("ConstituentHandling:load: done");		
	}
	/**
	 * Returns null on absence
	 * @param submitter_global_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String getConstituentLocalIDFromGID(String global_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("ConstituentHandling:getConstituentLocalID: start");
		String date[] = new String[1];
		String result = getConstituentLocalIDAndDateFromGID(global_ID, date);
		if(DEBUG) System.out.println("ConstituentHandling:getConstituentLocalID: result = "+result);
		return result;
	}
	/**
	 * Returns null on absence
	 * @param global_ID
	 * @param date
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String getConstituentLocalIDAndDateFromGID(
			String global_ID, String[] date) throws P2PDDSQLException {
		if(DEBUG) System.out.println("ConstituentHandling:getConstituentLocalIDAndDate: start");
		if(global_ID==null) return null;
		String sql = "SELECT "+table.constituent.constituent_ID+", "+table.constituent.creation_date+
		" FROM "+table.constituent.TNAME+
		" WHERE "+table.constituent.global_constituent_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.db.select(sql, new String[]{global_ID}, DEBUG);
		if(n.size()==0) return null;
		date[0]=Util.getString(n.get(0).get(1));
		String result = Util.getString(n.get(0).get(0));
		if(DEBUG) System.out.println("ConstituentHandling:getConstituentLocalIDAndDate: got="+result);
		return result;
	}
	/**
	 * 
	 * @param global_constituent_ID
	 * @param global_constituent_id_hash2 
	 * @param date
	 * @param _old_sign
	 * @param _old_revoked
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static String getConstituentLocalIDAndDateAndSignRevoked(
			String global_constituent_ID, String global_constituent_id_hash, String[] date, String[] _old_sign, boolean[] _old_revoked) throws P2PDDSQLException {
		if(DEBUG) System.out.println("ConstituentHandling:getConstituentLocalIDAndDate: start");
		if((global_constituent_ID==null)&&(global_constituent_id_hash==null)) return null;
		String sql = 
				"SELECT "+table.constituent.constituent_ID+", "+table.constituent.creation_date+
				", "+table.constituent.sign+", "+table.constituent.revoked+
		" FROM "+table.constituent.TNAME+
		" WHERE "+table.constituent.global_constituent_ID+"=? OR "+table.constituent.global_constituent_ID_hash+"=?;";
		ArrayList<ArrayList<Object>> n = Application.db.select(sql, new String[]{global_constituent_ID, global_constituent_id_hash}, DEBUG);
		if(n.size()==0) return null;
		date[0]=Util.getString(n.get(0).get(1));
		_old_sign[0]=Util.getString(n.get(0).get(2));
		_old_revoked[0]=Util.stringInt2bool(n.get(0).get(3), false);
		String result = Util.getString(n.get(0).get(0));
		if(DEBUG) System.out.println("ConstituentHandling:getConstituentLocalIDAndDate: got="+result);
		return result;
	}
	public static String getConstituentGlobalID(String submitter_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("ConstituentHandling:getConstituentGlobalID: start");
		if(submitter_ID==null) return null;
		String sql = "SELECT "+table.constituent.global_constituent_ID+
		" FROM "+table.constituent.TNAME+
		" WHERE "+table.constituent.constituent_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.db.select(sql, new String[]{submitter_ID}, DEBUG);
		if(n.size()==0) return null;
		return Util.getString(n.get(0).get(0));
	}
	/**
	 * 
	 * @param const_GID
	 * @param org_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long insertTemporaryConstituentGID(String const_GID, String org_ID) throws P2PDDSQLException{
		if(DEBUG)Util.printCallPath("temp why");
		if(DEBUG) System.out.println("ConstituentHandling:insertTemporaryConstituentGID: start ");
		try{
			return Application.db.insert(table.constituent.TNAME,
					new String[]{table.constituent.global_constituent_ID, table.constituent.organization_ID},
					new String[]{const_GID, org_ID},
					DEBUG);
		}catch(Exception e){
			if(_DEBUG) System.out.println("ConstituentHandling:insertTemporaryConstituentGID: regot failed="+const_GID);
			String id = D_Constituent.getConstituentLocalIDFromGID(const_GID);
			if(_DEBUG) System.out.println("ConstituentHandling:insertTemporaryConstituentGID: regot id="+id);
			return Util.lval(id, -1);
		}
	}
	public static byte getASN1Type() {
		return Encoder.buildASN1byteType(Encoder.CLASS_APPLICATION,
				Encoder.PC_CONSTRUCTED, TAG);
	}
	public static Hashtable<String, String> checkAvailability(
			Hashtable<String, String> cons, String orgID, boolean DBG) throws P2PDDSQLException {
		Hashtable<String, String> result = new Hashtable<String, String>();
		for (String cHash : cons.keySet()) {
			if(cHash == null) continue;
			if(!available(cHash, cons.get(cHash), orgID, DBG)) result.put(cHash, DD.EMPTYDATE);
		}
		return result;
	}
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
			"SELECT "+table.constituent.constituent_ID+
			" FROM "+table.constituent.TNAME+
			" WHERE "+table.constituent.global_constituent_ID_hash+"=? "+
			" AND "+table.constituent.organization_ID+"=? "+
			" AND ( "+table.constituent.sign + " IS NOT NULL " +
			" OR "+table.constituent.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{hash, orgID}, DEBUG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_News:available: "+hash+" in "+orgID+" = "+result);
		return result;
	}
	private static boolean available(String hash, String creation, String orgID, boolean DBG) throws P2PDDSQLException {
		String sql = 
			"SELECT "+table.constituent.constituent_ID+
			" FROM "+table.constituent.TNAME+
			" WHERE "+table.constituent.global_constituent_ID_hash+"=? "+
			" AND "+table.constituent.organization_ID+"=? "+
			" AND "+table.constituent.creation_date+">= ? "+
			" AND ( "+table.constituent.sign + " IS NOT NULL " +
			" OR "+table.constituent.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{hash, orgID, creation}, DEBUG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_News:available: "+hash+" in "+orgID+" = "+result);
		return result;
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
	public static int isGID_or_Hash_available(String gID, boolean DBG) throws P2PDDSQLException {
		String sql = 
			"SELECT "+table.constituent.constituent_ID+","+table.constituent.sign+
			" FROM "+table.constituent.TNAME+
			" WHERE "+table.constituent.global_constituent_ID_hash+"=? OR " +
			table.constituent.global_constituent_ID+"=?"+
					";";
			//" AND "+table.constituent.organization_ID+"=? "+
			//" AND ( "+table.constituent.sign + " IS NOT NULL " +
			//" OR "+table.constituent.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{gID}, DEBUG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_Constituent:available: "+gID+" in "+" = "+result);
		if(a.size()==0) return 0;    
		String signature = Util.getString(a.get(0).get(1));
		if((signature!=null) && (signature.length()!=0)) return 1;
		return -1;
	}
	public static int isGIDHash_available(String gIDhash, boolean DBG) throws P2PDDSQLException {
		String sql = 
			"SELECT "+table.constituent.constituent_ID+","+table.constituent.sign+
			" FROM "+table.constituent.TNAME+
			" WHERE "+table.constituent.global_constituent_ID_hash+"=? " +
					";";
			//" AND "+table.constituent.organization_ID+"=? "+
			//" AND ( "+table.constituent.sign + " IS NOT NULL " +
			//" OR "+table.constituent.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{gIDhash}, DEBUG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_Constituent:available: "+gIDhash+" in "+" = "+result);
		if(a.size()==0) return 0;    
		String signature = Util.getString(a.get(0).get(1));
		if((signature!=null) && (signature.length()!=0)) return 1;
		return -1;
	}
	public static String getConstituentLocalIDByGID_or_Hash(String gID, boolean[] existingSigned) throws P2PDDSQLException {
		String sql = 
			"SELECT "+table.constituent.constituent_ID+","+table.constituent.sign+
			" FROM "+table.constituent.TNAME+
			" WHERE "+
			table.constituent.global_constituent_ID_hash+"=? OR "+
			table.constituent.global_constituent_ID+"=?"+
			";";
			//" AND "+table.constituent.organization_ID+"=? "+
			//" AND ( "+table.constituent.sign + " IS NOT NULL " +
			//" OR "+table.constituent.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{gID,gID}, DEBUG);
		String result = null;
		if(DEBUG) System.out.println("D_Constituent:available: "+gID+" in "+" = "+result);
		if(existingSigned != null) existingSigned[0] = false;
		if(a.size()==0) {return null;}
		String signature = Util.getString(a.get(0).get(1));
		if((signature!=null) && (signature.length()!=0) && (existingSigned!=null))
			existingSigned[0] = true;
		String id = Util.getString(a.get(0).get(0));
		return id;
	}
	public static String getConstituentLocalIDByGID_or_Hash(
			String gID, String GID_hash,
			boolean existingSigned[]) throws P2PDDSQLException {
		String sql = 
				"SELECT "+table.constituent.constituent_ID+","+table.constituent.sign+
				" FROM "+table.constituent.TNAME+
				" WHERE "+
				table.constituent.global_constituent_ID_hash+"=? OR "+
				table.constituent.global_constituent_ID+"=?"+
				";";
				//" AND "+table.constituent.organization_ID+"=? "+
				//" AND ( "+table.constituent.sign + " IS NOT NULL " +
				//" OR "+table.constituent.blocked+" = '1');";
			ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{GID_hash, gID}, DEBUG);
			String result = null;
			if(DEBUG) System.out.println("D_Constituent:available: "+gID+" in "+" = "+result);
			if(existingSigned != null) existingSigned[0] = false;
			if(a.size()==0) {return null;}
			String signature = Util.getString(a.get(0).get(1));
			if((signature!=null) && (signature.length()!=0) && (existingSigned!=null))
				existingSigned[0] = true;
			String id = Util.getString(a.get(0).get(0));
			return id;
	}
	public static boolean toggleBlock(long constituentID) throws P2PDDSQLException {
		String sql = 
				"SELECT "+table.constituent.blocked+
				" FROM "+table.constituent.TNAME+
				" WHERE "+table.constituent.constituent_ID+"=?;";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{}, DEBUG);
		if(a.size()==0){
			Util.printCallPath("No such item");
			return false;
		}
		boolean result = !Util.stringInt2bool(a.get(0).get(0), false);
		Application.db.updateNoSync(table.constituent.TNAME,
				new String[]{table.constituent.blocked}, 
				new String[]{table.constituent.constituent_ID},
				new String[]{Util.bool2StringInt(result), Util.getStringID(constituentID)}, DEBUG);
		return result;
	}
	public static boolean toggleBroadcast(long constituentID) throws P2PDDSQLException {
		String sql = 
				"SELECT "+table.constituent.broadcasted+
				" FROM "+table.constituent.TNAME+
				" WHERE "+table.constituent.constituent_ID+"=?;";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{}, DEBUG);
		if(a.size()==0){
			Util.printCallPath("No such item");
			return false;
		}
		boolean result = !Util.stringInt2bool(a.get(0).get(0), false);
		Application.db.updateNoSync(table.constituent.TNAME,
				new String[]{table.constituent.broadcasted}, 
				new String[]{table.constituent.constituent_ID},
				new String[]{Util.bool2StringInt(result), Util.getStringID(constituentID)}, DEBUG);
		return result;
	}
	/**
	 * Tests
	 * @param args
	 */
	public static void _main(String[] args) {
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			//if(args.length>0){readSignSave(2,1); if(true) return;}
			int id = Util.ival(args[0], 1);
			long new_id = simulator.Fill_database.add_constituent(1);
			D_Constituent c=new D_Constituent(new_id);
			if(!c.verifySignature(c.global_organization_ID)) System.out.println("\n************Signature Failure\n**********\n"+c);
			else System.out.println("\n************Signature Pass\n**********\n"+c);
			Decoder dec = new Decoder(c.getEncoder().getBytes());
			D_Constituent d = new D_Constituent().decode(dec);
			if(d.global_organization_ID==null) d.global_organization_ID = D_Organization.getGlobalOrgID(d.organization_ID);
			if(!d.verifySignature(c.global_organization_ID)) System.out.println("\n************Signature Failure\n**********\n"+d);
			else System.out.println("\n************Signature Pass\n**********\n"+d);
			d.global_constituent_id = d.makeExternalGID();
			//d.storeVerified(c.global_organization_ID, c.organization_ID, Util.getGeneralizedTime());
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void zapp(long constituentID) {
		// TODO Auto-generated method stub
		try {
			Application.db.delete(table.witness.TNAME,
					new String[]{table.witness.source_ID},
					new String[]{Util.getStringID(constituentID)},
					DEBUG);
			Application.db.delete(table.witness.TNAME,
					new String[]{table.witness.target_ID},
					new String[]{Util.getStringID(constituentID)},
					DEBUG);
			Application.db.delete(table.field_value.TNAME,
					new String[]{table.field_value.constituent_ID},
					new String[]{Util.getStringID(constituentID)},
					DEBUG);
			Application.db.delete(table.constituent.TNAME,
					new String[]{table.constituent.constituent_ID},
					new String[]{Util.getStringID(constituentID)},
					DEBUG);
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

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
			D_Organization organization = null;
			if(id<=0){
				l = Application.db.select(
						"SELECT "+table.constituent.constituent_ID+
						" FROM "+table.constituent.TNAME, new String[]{}, DEBUG);
				for(ArrayList<Object> a: l){
					String m_ID = Util.getString(a.get(0));
					long ID = Util.lval(m_ID, -1);
					D_Constituent m = new D_Constituent(ID);
					if(m.signature==null){
						if(organization==null)organization = new D_Organization(Util.lval(m.organization_ID, -1));
						System.out.println("Fail:temporary "+m.constituent_ID+":"+m.surname+","+m.forename+" in "+m.organization_ID+":"+organization.name);

						if(fix){
							m.global_constituent_id_hash = m.getGIDHashFromGID();
							m.storeVerified();
							readSignSave(ID, Util.lval(m.submitter_ID, -1));
						}
						continue;
					}
					if(m.global_constituent_id_hash==null){
						if(organization==null)organization = new D_Organization(Util.lval(m.organization_ID, -1));
						System.out.println("Fail:edited "+m.constituent_ID+":"+m.surname+","+m.forename+" in "+m.organization_ID+":"+organization.name);
						continue;
					}
					if(!m.verifySignature(m.global_organization_ID)){
						if(organization==null)organization = new D_Organization(Util.lval(m.organization_ID, -1));
						System.out.println("Fail: "+m.constituent_ID+":"+m.surname+","+m.forename+" in "+m.organization_ID+":"+organization.name);
						if(fix){
							m.global_constituent_id_hash = m.getGIDHashFromGID();
							m.storeVerified();
							readSignSave(ID, Util.lval(m.submitter_ID, -1));
						}
					}
				}
				return;
			}else{
				long ID = id;
				D_Constituent m = new D_Constituent(ID);
				if(fix)
					if(!m.verifySignature(m.global_organization_ID)) {
						if(organization==null)organization = new D_Organization(Util.lval(m.organization_ID, -1));
						m.global_constituent_id_hash = m.getGIDHashFromGID();
						m.storeVerified();
						System.out.println("Fixing: "+m.constituent_ID+":"+m.surname+","+m.forename+" in "+m.organization_ID+":"+organization.name);
						readSignSave(ID, Util.lval(m.submitter_ID, -1));
					}
				else if(!m.verifySignature(m.global_organization_ID)){
					if(organization==null)organization = new D_Organization(Util.lval(m.organization_ID, -1));
					System.out.println("Fail: "+m.constituent_ID+":"+m.surname+","+m.forename+" in "+m.organization_ID+":"+organization.name);
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
