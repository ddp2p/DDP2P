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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import ciphersuits.Cipher;
import ciphersuits.SK;

import com.almworks.sqlite4java.SQLiteException;

import registration.ASNConstituentOP;
import registration.ASNNeighborhoodOP;
import registration.WB_ASN_Peer;
import streaming.OrgHandling;
import streaming.RequestData;
import util.DBInterface;
import util.Summary;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.Application;
import config.DD;
public
class D_Organization extends ASNObj implements Summary {
	private static final String V0 = "0";
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	public static final boolean DEFAULT_BROADCASTED_ORG = true;
	public static final boolean DEFAULT_BROADCASTED_ORG_ITSELF = false;
	public RequestData availableHashes;
	public String version=V0; //PrintableString
	public String global_organization_ID; //PrintableString
	public String global_organization_IDhash;
	public String name; //UTF8String OPTIONAL
	public Calendar reset_date;
	public Calendar last_sync_date;
	public String _last_sync_date;
	public D_OrgParams params; // AC1 OPTIONAL
	public D_OrgConcepts concepts; //AC2 OPTIONAL
	public byte[] signature;//=new byte[0];
	
	public D_PeerAddress creator;
	public data.D_Message requested_data[]; //AC11, data requested on a GID basis
	public ASNNeighborhoodOP neighborhoods[]; //AC3 OPTIONAL
	public ASNConstituentOP constituents[]; //AC4 OPTIONAL
	public D_Witness[] witnesses; //AC5 PTIONAL
	public D_Motion motions[]; //AC6 OPTIONAL
	public D_Justification justifications[]; //AC7 OPTIONAL
	public D_Vote signatures[]; // AC8 OPT
	public D_Translations translations[]; //AC9 OPTIONAL
	public D_News news[];	// AC10 OPTIONAL
	//PluginInfo plugins; //AC12 OPTIONAL
	//PluginData plugin_data; //AC13 OPTIONAL
	public String organization_ID;
	public String creator_ID;
	public boolean blocked = false;
	public boolean requested = false;
	public boolean broadcasted = DEFAULT_BROADCASTED_ORG_ITSELF;
	public Calendar arrival_date;
	public long _organization_ID;
	public byte[] signature_initiator;

	@Override
	public String toSummaryString() {
		String result = "\n OrgData: [\n";
		//";\n  ver = "+version+
		//";\n  id  = "+global_organization_ID+";\n"+
		result += " name="+name+"; ";
		//";\n  l_s_d="+_last_sync_date+
		//";\n  params="+params+
		//";\n  creator="+creator+
		//";\n  concepts=<"+concepts+">"+
		//";\n  signature=<"+Util.byteToHexDump(signature)+">"+
		if(creator != null) result += ";\n  creator="+creator.toSummaryString();
		if(neighborhoods != null) result += ";\n  neighborhoods="+Util.concatSummary(neighborhoods, "\nOrgData-Neighborhood:", null);
		if(constituents != null) result += ";\n  constituents="+Util.concatSummary(constituents, "\nOrgData-Constituents:", null);
		if(witnesses != null) result += ";\n  witnesses="+Util.concatSummary(witnesses, "\nOrgData-Witnesses:", null);
		if(motions != null) result += ";\n  motions="+Util.concatSummary(motions, "\nOrgData-Motions:", null);
		if(justifications != null) result += ";\n  justifications="+Util.concat(justifications, "\nOrgData-Justifications:");
		if(signatures != null) result += ";\n  signatures="+Util.concat(signatures, "\nOrgData-Signatures:");
		if(translations != null) result += ";\n  translations="+Util.concat(translations, "\nOrgData-Translations:");
		if(news != null) result += ";\n  news="+Util.concat(news, "\nOrgData-News:");
		if(availableHashes!=null) result += ";\n  available="+availableHashes.toSummaryString();
		result += "\n]";
		return result;
	}
	public String toString() {
		String result = "OrgData: [";
		result += ";\n  ver = "+version;
		result += ";\n  id  = "+global_organization_ID;
		result += ";\n  name="+name;
		result += ";\n  l_s_d="+_last_sync_date;
		result += ";\n  params="+params;
		result += ";\n  creator="+creator;
		result += ";\n  concepts=<"+concepts+">";
		result += ";\n  signature=<"+Util.byteToHexDump(signature)+">";
		result += ";\n  signature_initiator=<"+Util.byteToHexDump(signature_initiator)+">";
		if(neighborhoods != null) result += ";\n  neighborhoods="+Util.concat(neighborhoods, "\nOrgData-Neighborhood:");
		if(constituents != null) result += ";\n  constituents="+Util.concat(constituents, "\nOrgData-Constituents:");
		if(witnesses != null) result += ";\n  witnesses="+Util.concat(witnesses, "\nOrgData-Witnesses:");
		if(motions != null) result += ";\n  motions="+Util.concat(motions, "\nOrgData-Motions:");
		if(justifications != null) result += ";\n  justifications="+Util.concat(justifications, "\nOrgData-Justifications:");
		if(signatures != null) result += ";\n  signatures="+Util.concat(signatures, "\nOrgData-Signatures:");
		if(translations != null) result +=  ";\n  translations="+Util.concat(translations, "\nOrgData-Translations:");
		if(news != null) result +=  ";\n  news="+Util.concat(news, "\nOrgData-News:");
		if(availableHashes!=null) result +=  ";\n  available="+availableHashes;
		result += "\n]";
		return result;
	}
	
	public D_Organization() {
		this.params = new D_OrgParams();
		this.params.hash_org_alg = table.organization.hash_org_alg_crt;
	}
	public D_Organization(ArrayList<Object> row) throws Exception {
		init(row);
	}
	/**
	 * 
	 * @param org_gid
	 * @param object
	 * @param extra_fields
	 * @param extra_creator
	 * @throws SQLiteException
	 */
	public D_Organization(String org_gid, String org_GIDhash, boolean extra_fields, boolean extra_creator) throws SQLiteException {
		init(org_gid, org_GIDhash, extra_fields, extra_creator);
	}
	/**
	 * 
	 * @param orgGID
	 * @param orgGID_hash
	 * @throws SQLiteException
	 */
	public D_Organization(String orgGID, String orgGID_hash) throws SQLiteException   {
		init(orgGID, orgGID_hash, true, true);
	}
	public D_Organization(long _organization_ID) throws SQLiteException {
		if(_organization_ID<=0) throw new RuntimeException("Invalid org ID");
		organization_ID = Util.getStringID(_organization_ID);
		String sql = D_Organization.org_field +
		" FROM "+table.organization.TNAME +
		" WHERE  "+table.organization.organization_ID+" = ?;";
		//" AND arrival_time > ? AND arrival_time <= ? ORDER BY arrival_time LIMIT 100;";

		ArrayList<ArrayList<Object>>p_data =
			Application.db.select(sql, new String[]{organization_ID/*, last_sync_date, max_Date*/}, DEBUG);
		if(p_data.size()>=1) {
			ArrayList<Object> row = p_data.get(0);
			init(row);
		}
	}
	/**
	 * 
	 * @param orgGID
	 * @param orgGID_hash
	 * @param extra_fields
	 * @param extra_creator
	 * @throws SQLiteException
	 */
	public void init(String orgGID, String orgGID_hash, boolean extra_fields, boolean extra_creator) throws SQLiteException{
		String sql = D_Organization.org_field +
				" FROM "+table.organization.TNAME +
				" WHERE ( "+table.organization.global_organization_ID+" = ? OR "+table.organization.global_organization_ID_hash+" = ? ) ";
						//+" ORDER BY "+table.organization.arrival_date+" LIMIT 100;";
				//" AND arrival_time > ? AND arrival_time <= ? ORDER BY arrival_time LIMIT 100;";
		
		ArrayList<ArrayList<Object>>p_data =
			Application.db.select(sql, new String[]{orgGID, orgGID_hash/*, last_sync_date, max_Date*/}, DEBUG);
		if(p_data.size()>=1) {
			ArrayList<Object> row = p_data.get(0);
			init(row, extra_fields, extra_creator);
		}
	}
	/**
	 * 
	 * @param row
	 * @throws SQLiteException
	 */
	public void init(ArrayList<Object> row) throws SQLiteException {
		init(row, true, true);
	}
	/**
	 * 
	 * @param row
	 * @param extra_fields
	 * @param extra_creator
	 * @throws SQLiteException
	 */
	public void init(ArrayList<Object> row, boolean extra_fields, boolean extra_creator) throws SQLiteException {
		organization_ID = Util.getString(row.get(table.organization.ORG_COL_ID),null);
		_organization_ID = Util.lval(organization_ID, -1);
		this.arrival_date = Util.getCalendar(Util.getString(row.get(table.organization.ORG_COL_ARRIVAL)),null);
		this.global_organization_ID = Util.getString(row.get(table.organization.ORG_COL_GID),null);
		this.global_organization_IDhash = Util.getString(row.get(table.organization.ORG_COL_GID_HASH),null);
		this.name = Util.getString(row.get(table.organization.ORG_COL_NAME),null);
		this.params = new D_OrgParams();
		this.params.languages = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_LANG)));
		this.params.instructions_registration = Util.getString(row.get(table.organization.ORG_COL_INSTRUC_REGIS),null);
		this.params.instructions_new_motions = Util.getString(row.get(table.organization.ORG_COL_INSTRUC_MOTION),null);
		this.params.description = Util.getString(row.get(table.organization.ORG_COL_DESCRIPTION),null);
		this.params.default_scoring_options = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_SCORES),null));//new String[0];//null;
		this.params.category = Util.getString(row.get(table.organization.ORG_COL_CATEG),null);
		
		try{this.params.certifMethods = Integer.parseInt(Util.getString(row.get(table.organization.ORG_COL_CERTIF_METHODS)));}catch(NumberFormatException e){this.params.certifMethods = 0;}
		this.params.hash_org_alg = Util.getString(row.get(table.organization.ORG_COL_HASH_ALG),null);
		this.params.creation_time = Util.getCalendar(Util.getString(row.get(table.organization.ORG_COL_CREATION_DATE)), Util.CalendargetInstance());
		this.reset_date = Util.getCalendar(Util.getString(row.get(table.organization.ORG_COL_RESET_DATE)), null);
		this.params.certificate = Util.byteSignatureFromString(Util.getString(row.get(table.organization.ORG_COL_CERTIF_DATA),null));
		String creator_ID=Util.getString(row.get(table.organization.ORG_COL_CREATOR_ID));
		this.blocked = Util.stringInt2bool(row.get(table.organization.ORG_COL_BLOCK),false);
		this.requested = Util.stringInt2bool(row.get(table.organization.ORG_COL_REQUEST),false);
		this.broadcasted = Util.stringInt2bool(row.get(table.organization.ORG_COL_BROADCASTED), D_Organization.DEFAULT_BROADCASTED_ORG_ITSELF);
		
		this.concepts = new D_OrgConcepts();
		this.concepts.name_organization = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_NAME_ORG),null));
		this.concepts.name_forum = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_NAME_FORUM),null));
		this.concepts.name_motion = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_NAME_MOTION),null));
		this.concepts.name_justification = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_NAME_JUST),null));
		this.signature = getSignatureFromString(Util.getString(row.get(table.organization.ORG_COL_SIGN),null));
		this.signature_initiator = getSignatureFromString(Util.getString(row.get(table.organization.ORG_COL_SIGN_INITIATOR),null));
		
		if(extra_fields)this.params.orgParam = D_Organization.getOrgParams(organization_ID);
		D_PeerAddress creator = null;
//		D_PeerAddress creator1 = null;
//		D_PeerAddress creator2 = null;
		if(extra_creator) creator = D_PeerAddress.getPeerAddress(creator_ID, true, true); // for creator one also needs _served
		else this.params.creator_global_ID = D_PeerAddress.getPeerGIDforID(creator_ID);

		if(creator == null) {
			//if(DEBUG)Util.printCallPath("No creator:"+creator_ID);
			//Application.warning(Util._("Missing organization creator, or may have incomplete data. You should create a new one!"), Util._("Missing organization creator."));
			//throw new Exception("No creator");
		}else
			this.params.creator_global_ID = creator.globalID;
		if((this.signature_initiator == null)&&(this.params.creator_global_ID!=null)){
			byte[]msg = this.getSignableEncoder().getBytes();
			SK sk = Util.getStoredSK(this.params.creator_global_ID, null);
			if(sk!=null) {
				this.signature_initiator = Util.sign(msg, sk);
				Application.db.updateNoSync(table.organization.TNAME,
						new String[]{table.organization.signature_initiator},
						new String[]{table.organization.organization_ID},
						new String[]{Util.stringSignatureFromByte(this.signature_initiator), this.organization_ID}, DEBUG);
				Application.warning(_("Update org initiator signature for:")+this.name, _("Updated org Signature"));
			}
		}
	}
	public D_Organization instance() {return new D_Organization();}
	public void setDate(Calendar lsd){
		last_sync_date = lsd;
		_last_sync_date = Encoder.getGeneralizedTime(lsd);
	}
	public void setGT(String lsd){
		_last_sync_date = lsd;
		last_sync_date = Util.getCalendar(lsd);
	}		
	/**
	 * Will compute the signature  of the org_id setting current time as creation_time and using the provided secret key
	 * @param org_ID
	 * @param currentTime
	 * @param sk
	 * @return
	 * @throws SQLiteException
	 */
	public static byte[] getOrgSignature(String org_ID, String currentTime, ciphersuits.SK sk, D_Organization[] org) throws SQLiteException {
		String sql = D_Organization.org_field +
			" FROM "+table.organization.TNAME+" WHERE "+table.organization.organization_ID+" = ? ";
		if(DEBUG) out.println("\n***********\nOrgHandling:getOrgSignature: Prepared org: "+org_ID);
		ArrayList<ArrayList<Object>>p_data = Application.db.select(sql, new String[]{org_ID}, DEBUG);
		if(DEBUG)out.println("OrgHandling:getOrgSignature: Organizations="+p_data.size());

		String gid = Util.getString(p_data.get(0).get(table.organization.ORG_COL_GID));
		String org_id = Util.getString(p_data.get(0).get(table.organization.ORG_COL_ID));
		D_Organization orgData = OrgHandling.signableOrgData(gid, org_ID, p_data.get(0));
		orgData.params.creation_time = Util.getCalendar(currentTime);
		if(DEBUG) out.println("OrgHandling:getOrgSignature: Prepared org: "+orgData);
		if(DEBUG) out.println("*****************");
		if((org!=null)||(org.length>0)) org[0] = orgData;
		//byte[]h = orgData.hash(null);
		SK sk_ini = Util.getStoredSK(orgData.params.creator_global_ID, null);
		//orgData.signature_initiator = Util.sign(h, sk_ini);
		return orgData.sign(sk, sk_ini);
	}
	/**
	 * Compute the orgGID for grass-root organizations, used in editor when creating an org
	 * loads a D_Organization and sets the current time
	 * uses hash(DD.APP_ORGID_HASH) to compute the hash
	 * @param org_ID
	 * @param currentTime
	 * @param org : returns the org in parameter if this is not null
	 * @return
	 * @throws SQLiteException
	 */
	public static byte[] getOrgGIDandHashForGrassRoot(String org_ID, String currentTime, D_Organization[] org) throws SQLiteException {
		String sql = D_Organization.org_field +
			" FROM "+table.organization.TNAME+" WHERE "+table.organization.organization_ID+" = ? ";
		if(DEBUG) out.println("OrgHandling:getOrgHash: Prepared org: "+org_ID);
		ArrayList<ArrayList<Object>>p_data = Application.db.select(sql, new String[]{org_ID}, DEBUG);
		if(DEBUG)out.println("OrgHandling:getOrgHash: Organizations="+p_data.size());
		if(p_data.size()==0) return null;

		String gid = Util.getString(p_data.get(0).get(table.organization.ORG_COL_GID));
		//String org_id = Util.getString(p_data.get(0).get(table.organization.ORG_COL_ID));
		D_Organization orgData = OrgHandling.signableOrgData(gid, org_ID, p_data.get(0));
		if(orgData==null) return null;
		orgData.params.creation_time = Util.getCalendar(currentTime);
		if(DEBUG) out.println("OrgHandling:getOrgHash: Prepared org: "+orgData);
		if(DEBUG) out.println("*****************");
		if((org!=null)&&(org.length>0)) org[0]=orgData;
		
		byte[]h = orgData.hash(DD.APP_ORGID_HASH);
		SK sk_ini = Util.getStoredSK(orgData.params.creator_global_ID, null);
		if(sk_ini!=null) orgData.signature_initiator = Util.sign(h, sk_ini);
		
		return h;
	}
	/**
	 * FromDB
	 * @param org_ID
	 * @param currentTime
	 * @param org 
	 * @return
	 * @throws SQLiteException
	 */
	public static String _getOrgGIDandHashForGrassRoot(String org_ID, String currentTime, D_Organization[] org) throws SQLiteException {
		byte[] h = getOrgGIDandHashForGrassRoot(org_ID, currentTime, org);
		if(h==null){
			Util.printCallPath("Failure to compute Hash, incomplete data");
			return null;
		}
		String _gid = Util.stringSignatureFromByte(h);
		return "G:"+_gid;
	}
	/**
	 * makeGID
	 * @return
	 */
	public String getOrgGIDandHashForGrassRoot(){
		return getOrgGIDandHashForGrassRoot(null);
	}
	public String getOrgGIDandHashForGrassRoot(boolean[] verif){
		byte[] hash = this.hash(DD.APP_ORGID_HASH);
		if(verif!=null) verif[0]=!DD.ENFORCE_ORG_INITIATOR||Util.verifySignByID(hash, this.params.creator_global_ID, this.signature_initiator);
		return "G:"+Util.stringSignatureFromByte(hash);
	}
	public static byte[] getHashFromGrassrootGID(String id) {
		if((id==null) || (id.length()<2)) return null;
		return Util.byteSignatureFromString(id.substring(2));
	}
	public static String getOrgGIDHashAuthoritarian(String GID){
		if (GID==null) return null;
		String hash = Util.getGIDhash(GID);
		if(hash==null) return null;
		return "O:"+hash;
	}
	/**
	 * Gets GIDhash from internal GIFfunction on type
	 * @return
	 */
	public String getOrgGIDHashFromGID() {
		String result=null;
		if(params == null) return null;
		if(params.certifMethods == table.organization._GRASSROOT){
			result = global_organization_ID;
		}else if(params.certifMethods == table.organization._AUTHORITARIAN){
			result = getOrgGIDHashAuthoritarian(global_organization_ID);
		}else result=null;
		return  result;
	}
	/**
	 * try to compute has. if not shorter, then this is already the hash
	 * @param GID
	 * @return
	 */
	public static String getOrgGIDHashGuess(String GID) {
		if (GID==null) return null;
		if (GID.startsWith("O:")) return GID; // already hash
		if (GID.startsWith("G:")) return GID; // grass root
		String GID_hash = getOrgGIDHashAuthoritarian(GID);
		//if(GID_hash.length() == GID.length()) return GID;
		return GID_hash;
	}
	/**
	 * Gets the GIDhash of a given organizationID
	 * @param oID
	 * @return
	 * @throws SQLiteException
	 */
	public static String getOrgGIDHashFromDB(String oID) throws SQLiteException {
		String sql = 
			"SELECT "+table.organization.global_organization_ID_hash+
			" FROM "+table.organization.TNAME+
			" WHERE "+table.organization.organization_ID+"=?";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{oID}, DEBUG);
		if(a.size()==0) return null;
		return Util.getString(a.get(0).get(0));
	}

	public boolean verifyExtraFieldGIDs() {
		return verifyExtraFieldGIDs(this);
	}
	public static boolean verifyExtraFieldGIDs(D_Organization od) {
		if ((od.params==null) || (od.params.orgParam==null)) return true;
		for(int i=0; i<od.params.orgParam.length; i++) {
			D_OrgParam dop = od.params.orgParam[i];
			String eGID = dop.makeGID();
			if(!eGID.equals(dop.global_field_extra_ID)){
				if(_DEBUG)System.out.println("D_Organization: verifySignatureAllTypes: fail to verify field ID: "+dop.label);
				return false;
			}
		}
		return true;
	}
	/**
	 * Without id, (which is the result of hash/signing, and without constituents etc
	 * @return
	 */
	public Encoder getSignableEncoder() {
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding OrgData sign: "+this);
		if((params==null) || (params.creator_global_ID==null)) {
			Util.printCallPath("No creator for this org!");
			//return null;
		}
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version,false));
		//enc.addToSequence(new Encoder(id,false));
		if (name != null) enc.addToSequence(new Encoder(name));
		//if (last_sync_date != null) enc.addToSequence(new Encoder(last_sync_date));
		if (params != null) enc.addToSequence(params.getEncoder().setASN1Type(DD.TAG_AC1));
		if (concepts != null) enc.addToSequence(concepts.getEncoder().setASN1Type(DD.TAG_AC2));
		if(ASNSyncRequest.DEBUG)System.out.println("Encoded OrgData sign: "+this);
		return enc;
	}
	private Encoder getEntityEncoder() {
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding OrgData: "+this);
		Encoder enc = new Encoder().initSequence();
		//enc.addToSequence(new Encoder(version,false));
		enc.addToSequence(new Encoder(global_organization_ID,false));
		if (name != null) enc.addToSequence(new Encoder(name));
		//if (last_sync_date != null) enc.addToSequence(new Encoder(last_sync_date));
		if (params != null) enc.addToSequence(params.getEncoder().setASN1Type(DD.TAG_AC1));
		if (concepts != null) enc.addToSequence(concepts.getEncoder().setASN1Type(DD.TAG_AC2));
		//if (signature != null) enc.addToSequence(new Encoder(signature));
		//if (creator != null) enc.addToSequence(creator.getEncoder().setASN1Type(DD.TAG_AC0));
		//if (neighborhoods != null) enc.addToSequence(Encoder.getEncoder(neighborhoods).setASN1Type(DD.TAG_AC3));
		//if (constituents != null) enc.addToSequence(Encoder.getEncoder(constituents).setASN1Type(DD.TAG_AC4));
		//if (witnesses != null) enc.addToSequence(Encoder.getEncoder(witnesses).setASN1Type(DD.TAG_AC5));
		//if (motions != null) enc.addToSequence(Encoder.getEncoder(motions).setASN1Type(DD.TAG_AC6));
		//if (justifications != null) enc.addToSequence(Encoder.getEncoder(justifications).setASN1Type(DD.TAG_AC7));		
		//if (signatures != null) enc.addToSequence(Encoder.getEncoder(signatures).setASN1Type(DD.TAG_AC8));
		//if (translations != null) enc.addToSequence(Encoder.getEncoder(translations).setASN1Type(DD.TAG_AC9));
		//if (news != null) enc.addToSequence(Encoder.getEncoder(news).setASN1Type(DD.TAG_AC10));
		//if (requested_data != null) enc.addToSequence(Encoder.getEncoder(requested_data).setASN1Type(DD.TAG_AC11));
		enc.setASN1Type(DD.TYPE_ORG_DATA);
		if(ASNSyncRequest.DEBUG)System.out.println("Encoded OrgData: "+this);
		return enc;
	}
	public Encoder getEncoder() {
		if(ASNSyncRequest.DEBUG)System.out.println("Encoding OrgData: "+this);
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version,false));
		enc.addToSequence(new Encoder(global_organization_ID,false));
		if (name != null) enc.addToSequence(new Encoder(name));
		if (last_sync_date != null) enc.addToSequence(new Encoder(last_sync_date));
		if (params != null) enc.addToSequence(params.getEncoder().setASN1Type(DD.TAG_AC1));
		if (concepts != null) enc.addToSequence(concepts.getEncoder().setASN1Type(DD.TAG_AC2));
		if (signature != null) enc.addToSequence(new Encoder(signature));
		if (signature_initiator != null) enc.addToSequence(new Encoder(signature_initiator).setASN1Type(DD.TAG_AC14));
		if (creator != null) enc.addToSequence(creator.getEncoder().setASN1Type(DD.TAG_AC0));
		if (neighborhoods != null) enc.addToSequence(Encoder.getEncoder(neighborhoods).setASN1Type(DD.TAG_AC3));
		if (constituents != null) enc.addToSequence(Encoder.getEncoder(constituents).setASN1Type(DD.TAG_AC4));
		if (witnesses != null) enc.addToSequence(Encoder.getEncoder(witnesses).setASN1Type(DD.TAG_AC5));
		if (motions != null) enc.addToSequence(Encoder.getEncoder(motions).setASN1Type(DD.TAG_AC6));
		if (justifications != null) enc.addToSequence(Encoder.getEncoder(justifications).setASN1Type(DD.TAG_AC7));		
		if (signatures != null) enc.addToSequence(Encoder.getEncoder(signatures).setASN1Type(DD.TAG_AC8));
		if (translations != null) enc.addToSequence(Encoder.getEncoder(translations).setASN1Type(DD.TAG_AC9));
		if (news != null) enc.addToSequence(Encoder.getEncoder(news).setASN1Type(DD.TAG_AC10));
		if (requested_data != null) enc.addToSequence(Encoder.getEncoder(requested_data).setASN1Type(DD.TAG_AC11));
		// these are aggregated in ASNSyncPayload in "advertised"
		//if (this.availableHashes != null) enc.addToSequence(availableHashes.getEncoder().setASN1Type(DD.TAG_AC12));
		enc.setASN1Type(DD.TYPE_ORG_DATA);
		if(ASNSyncRequest.DEBUG)System.out.println("Encoded OrgData: "+this);
		return enc;
	}
	public D_Organization decode(Decoder decoder) throws ASN1DecoderFail {
		if(ASNSyncRequest.DEBUG)System.out.println("DEncoding OrgData: "+this);
		if((decoder==null)||(decoder.getTypeByte()==Encoder.TAG_NULL)) return null;
		Decoder dec = decoder.getContent();
		version = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString(Encoder.TAG_PrintableString);
		global_organization_ID = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString(Encoder.TAG_PrintableString);
		if(DEBUG) System.out.println("OrgData id="+global_organization_ID);
		if(dec.getTypeByte()==Encoder.TAG_UTF8String){
			name = dec.getFirstObject(true, Encoder.TAG_UTF8String).getString(Encoder.TAG_UTF8String);
			if(DEBUG) System.out.println("OrgData name="+name);
		}
		if(dec.getTypeByte()==Encoder.TAG_GeneralizedTime){ setDate(dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType()); if(DEBUG )System.out.println("OrgData d="+last_sync_date);}
		if(dec.getTypeByte()==DD.TAG_AC1){ params=new D_OrgParams().decode(dec.getFirstObject(true)); if(DEBUG )System.out.println("OrgData p="+params);}
		if(dec.getTypeByte()==DD.TAG_AC2){ concepts=new D_OrgConcepts().decode(dec.getFirstObject(true)); if(DEBUG)System.out.println("OrgData c="+concepts);}
		if(dec.getTypeByte()==Encoder.TAG_OCTET_STRING){ signature=dec.getFirstObject(true).getBytes(); if(DEBUG)System.out.println("OrgData s="+Util.byteToHexDump(signature));}
		if(dec.getTypeByte()==DD.TAG_AC14){ signature_initiator=dec.getFirstObject(true).getBytes(); if(DEBUG)System.out.println("OrgData s="+Util.byteToHexDump(signature_initiator));}
		if(dec.getTypeByte()==DD.TAG_AC0){ creator = new D_PeerAddress().decode(dec.getFirstObject(true)); if(DEBUG)System.out.println("OrgData cr="+creator);}
		if(dec.getTypeByte()==DD.TAG_AC3){ neighborhoods = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new ASNNeighborhoodOP[]{}, new ASNNeighborhoodOP()); if(DEBUG)System.out.println("OrgData n="+neighborhoods);}
		if(dec.getTypeByte()==DD.TAG_AC4){ constituents = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new ASNConstituentOP[]{}, new ASNConstituentOP()); if(DEBUG)System.out.println("OrgData co="+constituents);}
		if(dec.getTypeByte()==DD.TAG_AC5){ witnesses = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Witness[]{}, new D_Witness()); if(DEBUG)System.out.println("OrgData w="+witnesses);}
		if(dec.getTypeByte()==DD.TAG_AC6){ motions = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Motion[]{}, new D_Motion()); if(DEBUG)System.out.println("OrgData m="+motions);}
		if(dec.getTypeByte()==DD.TAG_AC7){ justifications = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Justification[]{}, new D_Justification()); if(DEBUG)System.out.println("OrgData j="+justifications);}
		if(dec.getTypeByte()==DD.TAG_AC8){ signatures = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Vote[]{}, new D_Vote()); if(DEBUG)System.out.println("OrgData s="+signatures);}
		if(dec.getTypeByte()==DD.TAG_AC9){ translations = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Translations[]{}, new D_Translations()); if(DEBUG)System.out.println("OrgData t="+translations);}
		if(dec.getTypeByte()==DD.TAG_AC10){ news = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_News[]{}, new D_News()); if(DEBUG)System.out.println("OrgData nw="+news);}
		if(dec.getTypeByte()==DD.TAG_AC11){ requested_data = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new data.D_Message[]{}, new data.D_Message()); if(DEBUG)System.out.println("OrgData rd="+requested_data);}
		//if(dec.getTypeByte()==DD.TAG_AC12){ availableHashes = new RequestData().decode(dec.getFirstObject(true)); if(DEBUG)System.out.println("OrgData available="+availableHashes);}
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects in decoder: "+dec.dumpHex()+"\n"+decoder.dumpHex());
		if(ASNSyncRequest.DEBUG)System.out.println("DEncoded OrgData: "+this);
		return this;
	}
	/**
	 * Gets the components of field_extra, ordered by GID (needed because encoder is used in comparing orgs with same date)
	 * @param local_id
	 * @return
	 * @throws SQLiteException
	 */
	public static D_OrgParam[]  getOrgParams(String local_id) throws SQLiteException {
		if(DEBUG) out.println("\n****************\nOrgHandling:getOrgParams: start organization orgID="+local_id);
		D_OrgParam[] result=null;
		String psql = "SELECT "+table.field_extra.org_field_extra +
				" FROM "+table.field_extra.TNAME+
				" WHERE "+table.field_extra.organization_ID+" = ? AND ("+table.field_extra.tmp+" IS NULL OR "+table.field_extra.tmp+"='0' )" +
				" ORDER BY "+table.field_extra.field_extra_ID+";";
		ArrayList<ArrayList<Object>>p_org = Application.db.select(psql, new String[]{local_id}, DEBUG);
		if(p_org.size()>0) {
			result = new D_OrgParam[p_org.size()];
			for(int p_i=0; p_i<p_org.size(); p_i++) {
				D_OrgParam op = D_OrgParam.getOrgParam(p_org.get(p_i));
				result[p_i] = op;
				if(DEBUG) System.out.println("OrgHandling:signOrgData: encoded orgParam="+op);
			}
		}
		if(DEBUG) out.println("OrgHandling:getOrgParams: exit\n****************\n");
		return result;
	}
	/**
	 * 
	 * @param orgID local organization ID
	 * @param orgParam An OrgParam[] array
	 * @throws SQLiteException
	 */
	public static void storeOrgParams(String orgID, D_OrgParam[] orgParam) throws SQLiteException{
		// Cannot be deleted and rewrutten since we would lose references to the IDs from const values
		Application.db.updateNoSync(table.field_extra.TNAME,
				new String[]{table.field_extra.tmp},
				new String[]{table.organization.organization_ID},
				new String[]{"1",orgID}, DEBUG);
		long _orgID = Util.lval(orgID, -1);
		if(orgParam!=null){
			String[] fieldNames_extra = Util.trimmed(table.field_extra.org_field_extra_insert.split(","));
			//String[] fieldNames_extra_update = Util.trimmed(table.field_extra.org_field_extra_insert.split(","));
			for(int k=0; k<orgParam.length; k++) {
				D_OrgParam op = orgParam[k];
				if(DEBUG) out.println("OrgHandling:updateOrg: Inserting/Updating field: "+op);
				String fe = D_FieldExtra.getFieldExtraID(op.global_field_extra_ID, _orgID);
				String[] p_extra;
				if(fe==null) p_extra = new String[fieldNames_extra.length];
				else p_extra = new String[fieldNames_extra.length+1];
				p_extra[table.field_extra.OPARAM_LABEL] = op.label;
				p_extra[table.field_extra.OPARAM_LABEL_L] = op.label_lang;
				p_extra[table.field_extra.OPARAM_LATER] = Util.bool2StringInt(op.can_be_provided_later);
				p_extra[table.field_extra.OPARAM_CERT] = Util.bool2StringInt(op.certificated);
				p_extra[table.field_extra.OPARAM_SIZE] = op.entry_size+"";
				p_extra[table.field_extra.OPARAM_NEIGH] = op.partNeigh+"";
				p_extra[table.field_extra.OPARAM_REQ] = Util.bool2StringInt(op.required);
				p_extra[table.field_extra.OPARAM_DEFAULT] = op.default_value;
				p_extra[table.field_extra.OPARAM_DEFAULT_L] = op.default_value_lang;
				p_extra[table.field_extra.OPARAM_LIST_VAL] = Util.concat(op.list_of_values, table.organization.ORG_VAL_SEP, null);
				p_extra[table.field_extra.OPARAM_LIST_VAL_L] = op.list_of_values_lang;
				p_extra[table.field_extra.OPARAM_TIP] = op.tip;
				p_extra[table.field_extra.OPARAM_TIP_L] = op.tip_lang;
				p_extra[table.field_extra.OPARAM_OID] = Util.BNOID2String(op.oid);
				p_extra[table.field_extra.OPARAM_ORG_ID] = orgID;
				p_extra[table.field_extra.OPARAM_GID] = op.global_field_extra_ID;
				p_extra[table.field_extra.OPARAM_VERSION] = op.version;
				p_extra[table.field_extra.OPARAM_TMP] = null;
				if(fe == null) {
					Application.db.insertNoSync(table.field_extra.TNAME, fieldNames_extra, p_extra, DEBUG);
				}else{
					p_extra[table.field_extra.OPARAM_EXTRA_FIELD_ID] = fe;
					Application.db.updateNoSync(table.field_extra.TNAME, fieldNames_extra,
							new String[]{table.field_extra.field_extra_ID},
							p_extra, DEBUG);
				}
			}
		}		
	}
	public static final String org_field = "SELECT " + table.organization.org_list;
	static public String getLocalOrgIDandDate(String global_organization_ID, String[] old_date) throws SQLiteException {
		String organization_ID = null;
		String sql = org_field + " FROM "+table.organization.TNAME+" WHERE "+table.organization.global_organization_ID+" = ?;";

		ArrayList<ArrayList<Object>>p_data = Application.db.select(sql, new String[]{global_organization_ID}, DEBUG);
		if(p_data.size() > 0) {
			if(DEBUG) out.println("OrgHandling:updateOrg: Found existing organizations: "+p_data.size());
			organization_ID = Util.getString(p_data.get(0).get(table.organization.ORG_COL_ID));
			old_date[0] = Util.getString(p_data.get(0).get(table.organization.ORG_COL_CREATION_DATE));
		}
		return organization_ID;
	}
	public byte[]hash(String _h) {
		return hash(_h, null);
	}
	public byte[]hash(String _h, ciphersuits.SK sk_ini) {
		Encoder enc = getSignableEncoder();
		byte[] msg = enc.getBytes();
		if(sk_ini !=null)this.signature_initiator = Util.sign(msg, sk_ini);
		return Util.simple_hash(msg, _h);	
	}
	public byte[]sign(ciphersuits.SK sk){
		return sign(sk, null);
	}
	public byte[]sign(ciphersuits.SK sk, ciphersuits.SK sk_ini){
		Encoder enc = getSignableEncoder();
		byte[] msg = enc.getBytes();
		if(sk_ini !=null)this.signature_initiator = Util.sign(msg, sk_ini);
		return Util.sign(msg, sk);	
	}
	public boolean verifySignAuthoritarian(byte[] sign) {
		if(DEBUG) System.out.println("OrgData:verifySign: KEY=="+global_organization_ID);
		if(DEBUG) System.out.println("OrgData:verifySign: sign="+Util.byteToHex(sign, ":"));
		try {
			this.fillGlobals();
		} catch (SQLiteException e1) {
			e1.printStackTrace();
		}		
		Encoder enc = getSignableEncoder();
		byte[] msg = enc.getBytes();
		if(DEBUG) System.out.println("OrgData:verifySign: msg="+Util.byteToHex(msg, ":"));
		if(!Util.verifySignByID(msg, this.params.creator_global_ID, this.signature_initiator)) return false;
		return Util.verifySignByID(msg, global_organization_ID, sign);
	}
	/**
	 * starts by verifying the field_extra IDs and the orgID for grass root orgs
	 * @param sign
	 * @return
	 */
	public boolean verifySign(byte[] sign) {
		if(params.certifMethods == table.organization._AUTHORITARIAN)
			if(signature==null) signature = sign;
		return verifySignature();
	}
	public static byte[] getSignatureFromString(String s){
		if(s==null) return null;
		if(s.length()<2) return null;
		if(s.startsWith("G:")) return Util.byteSignatureFromString(s.substring(2));
		return Util.byteSignatureFromString(s);
	}
	public boolean verifySignature() {
		boolean verified = true;
		//verify signature!
		if(params == null) {
			if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes: null param for="+this);
			return false;
		}
		if(params.certifMethods == table.organization._GRASSROOT){
			if(!this.verifyExtraFieldGIDs()){
				if(DEBUG) out.println("D_Organization:verifySignature: grassroot extras failed");
				verified = false;
			}else{
				//byte[] hash = hash(DD.APP_ORGID_HASH);
				boolean[]verif = new boolean[]{false};
				String tmpGID = this.getOrgGIDandHashForGrassRoot(verif);
				//if(!Util.equalBytes(hash, getHashFromGrassrootGID(global_organization_ID))) {
				if(!verif[0]||(tmpGID==null)||(global_organization_ID==null)||(tmpGID.compareTo(global_organization_ID)!=0)) {
					verified = false;
					if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes: recomp hash="+tmpGID);
					if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes:      IDhash="+global_organization_ID);
					if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes: exit grassroot signature verification failed");
				}
			}
			if(!verified){
				return false;
			}
		}else if(params.certifMethods == table.organization._AUTHORITARIAN){
			if((signature == null) || (signature.length == 0) || !verifySignAuthoritarian(signature)){
				if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes exit authoritarian signature verification failed");
				return false;
			}
		}else{
			if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes exit unknown type signature verification failed");
			return false;
		}
		return true;
	}
	private void fillGlobals() throws SQLiteException {
		if((this.params==null)||((this.params.creator_global_ID==null)&&(this.creator_ID ==  null))) {
			Util.printCallPath("cannot test org with no peerGID");
			return;
		}
		if((this.creator_ID != null ) && (this.params.creator_global_ID == null))
			this.params.creator_global_ID = D_Organization.getGlobalOrgID(this.creator_ID);
	}
	public boolean fillLocals(RequestData rq, boolean tempPeer, String arrival_time) throws SQLiteException {
		if((this.params==null)||((this.params.creator_global_ID==null)&&(this.creator_ID ==  null))) {
			if(DEBUG)Util.printCallPath("cannot store org with no peerGID");
			return false;
		}
		
		if((this.params.creator_global_ID!=null)&&(this.creator_ID == null)) {
			creator_ID = D_PeerAddress.getLocalPeerIDforGID(this.params.creator_global_ID);
			
			if(tempPeer && (creator_ID == null))  {
				creator_ID = D_PeerAddress.storePeerAndGetOrInsertTemporaryLocalForPeerGID(this.params.creator_global_ID, creator, arrival_time);
				
				String consGID_hash = D_PeerAddress.getGIDHashFromGID(this.params.creator_global_ID);
				if(rq!=null)rq.peers.add(consGID_hash);
				//creator_ID = Util.getStringID(D_PeerAddress.insertTemporaryGID(this.params.creator_global_ID, consGID_hash));
			}
			if(creator_ID == null) return false;
		}
		
		return true;
	}
	public long store(boolean[] _changed) throws SQLiteException {
		return store(_changed, null);
	}
	/**
	 * Probably one should store a temporary if not signed and unavailable (not yet done)
	 * @param _changed
	 * @return
	 * @throws SQLiteException
	 */
	public long store(boolean _changed[], RequestData _rq) throws SQLiteException {
		
		boolean locals = fillLocals(_rq, true, Util.getGeneralizedTime());
		//if(!locals)return -1;
		
		if((!locals) || (this.params==null) || !verifySignature()) {
			if((this.signature!=null)||(DD.ENFORCE_ORG_INITIATOR&&(this.signature_initiator!=null)))
				if(_DEBUG) out.println("D_Organization: store: exit signature verification failed for:"+this);
			if(_changed!=null)_changed[0] = false;
			//long local_org_ID = D_Organization.getLocalOrgID(global_organization_ID);
			//return local_org_ID;
			return _organization_ID;
		}
		return storeVerified(_changed);
	}
	public long storeVerified() throws SQLiteException {
		return storeVerified(Util.getGeneralizedTime(), null);
	}
	public long storeVerified(boolean _changed[]) throws SQLiteException {
		return storeVerified(Util.getGeneralizedTime(), _changed);
	}
	/**
	 * Synchronized GUI
	 * @param arrival_time
	 * @param changed
	 * @return
	 * @throws SQLiteException
	 */
	public long storeVerified(String arrival_time, boolean changed[]) throws SQLiteException {
		return storeVerified(arrival_time, changed, true);
	}
	/**
	 * 
	 * @param arrival_time
	 * @param changed
	 * @param sync : true to synchronize GUI
	 * @return
	 * @throws SQLiteException
	 */
	public long storeVerified(String arrival_time, boolean changed[], boolean sync) throws SQLiteException {
		//boolean DEBUG = true;
		if(DEBUG) out.println("D_Organization: storeVerified: start "+this.global_organization_IDhash);
		this.arrival_date = Util.getCalendar(arrival_time);
		arrival_time = Encoder.getGeneralizedTime(arrival_date);
		long result = -1;
		int filter = 0;
				
		if(this.global_organization_IDhash == null) global_organization_IDhash = getOrgGIDHashFromGID();
		
		String creation_time = Encoder.getGeneralizedTime(params.creation_time);
		String old_date[] = new String[1];
		String organization_ID = D_Organization.getLocalOrgIDandDate(global_organization_ID, old_date);
		if((organization_ID != null) && (old_date!=null) && (old_date[0]!=null) && (creation_time.compareTo(old_date[0]) <= 0)) {
			if(!Util.equalStrings_null_or_not(creation_time, old_date[0]) || hashConflictCreationDateDropThis()) {
				if(DEBUG) out.println("D_Organization: storeVerified: Will not integrate old ["+creation_time+"]: "+this);
				if(changed!=null)changed[0]=false;
				return Util.lval(organization_ID,-1);
			}
		}
		if(changed!=null) changed[0] = true;
		if(organization_ID != null) filter = 1;
		
		if(creator_ID == null)
			creator_ID = D_PeerAddress.storePeerAndGetOrInsertTemporaryLocalForPeerGID(params.creator_global_ID,creator,arrival_time);
		
		String field_sign = (signature!=null)?Util.stringSignatureFromByte(signature):null;
		if((field_sign!=null) && (params.certifMethods == table.organization._GRASSROOT)){
			field_sign = "G:"+field_sign;
		}
		
		String[] fieldNames = Util.trimmed(table.organization.org_list.split(","));
		String[] p = new String[fieldNames.length + filter];
		p[table.organization.ORG_COL_GID] = global_organization_ID;
		p[table.organization.ORG_COL_GID_HASH] = global_organization_IDhash;
		p[table.organization.ORG_COL_NAME] = name;
		p[table.organization.ORG_COL_NAME_ORG] = D_OrgConcepts.stringFromStringArray(concepts.name_organization);
		p[table.organization.ORG_COL_NAME_FORUM] = D_OrgConcepts.stringFromStringArray(concepts.name_forum);
		p[table.organization.ORG_COL_NAME_MOTION] = D_OrgConcepts.stringFromStringArray(concepts.name_motion);
		p[table.organization.ORG_COL_NAME_JUST] = D_OrgConcepts.stringFromStringArray(concepts.name_justification);//, table.organization.ORG_TRANS_SEP,null);
		p[table.organization.ORG_COL_LANG] = D_OrgConcepts.stringFromStringArray(params.languages);//, table.organization.ORG_LANG_SEP,null);
		p[table.organization.ORG_COL_INSTRUC_REGIS] = params.instructions_registration;
		p[table.organization.ORG_COL_INSTRUC_MOTION] = params.instructions_new_motions;
		p[table.organization.ORG_COL_DESCRIPTION] = params.description;
		p[table.organization.ORG_COL_SCORES] = D_OrgConcepts.stringFromStringArray(params.default_scoring_options);//, table.organization.ORG_SCORE_SEP,null);
		p[table.organization.ORG_COL_CATEG] = params.category;
		p[table.organization.ORG_COL_CERTIF_METHODS] = ""+params.certifMethods;
		p[table.organization.ORG_COL_HASH_ALG] = params.hash_org_alg;
		//p[table.organization.ORG_COL_HASH] = (params.hash_org!=null)?Util.byteToHex(params.hash_org, table.organization.ORG_HASH_BYTE_SEP):null;
		p[table.organization.ORG_COL_CREATION_DATE] = creation_time;
		p[table.organization.ORG_COL_CERTIF_DATA] = (params.certificate!=null)?Util.stringSignatureFromByte(params.certificate):null;
		p[table.organization.ORG_COL_CREATOR_ID] = creator_ID;
		p[table.organization.ORG_COL_SIGN] = field_sign;
		p[table.organization.ORG_COL_SIGN_INITIATOR] = Util.stringSignatureFromByte(this.signature_initiator);
		p[table.organization.ORG_COL_ARRIVAL] = arrival_time; //Util.getGeneralizedTime();
		p[table.organization.ORG_COL_RESET_DATE] = (reset_date!=null)?Encoder.getGeneralizedTime(reset_date):null; //Util.getGeneralizedTime();
		p[table.organization.ORG_COL_BLOCK] = Util.bool2StringInt(blocked);
		p[table.organization.ORG_COL_REQUEST] = Util.bool2StringInt(requested);
		p[table.organization.ORG_COL_BROADCASTED] = Util.bool2StringInt(broadcasted);
		
		//String orgID;
		if(organization_ID == null) {
			organization_ID = Util.getStringID(result = Application.db.insertNoSync(table.organization.TNAME, fieldNames, p, DEBUG));//changed = true;
			if(DEBUG) out.println("D_Organization: storeVerified: Inserted: "+this);
		}else{
			//orgID = Util.getString(p_data.get(0).get(table.organization.ORG_COL_ID));
			p[table.organization.ORG_COL_ID] = organization_ID;
			if(filter>0) p[fieldNames.length] = organization_ID;
			Application.db.updateNoSync(table.organization.TNAME,  fieldNames, new String[]{table.organization.organization_ID}, p, DEBUG);//changed = true;
			result = Util.lval(organization_ID, -1);
			if(DEBUG) out.println("\nD_Organization: storeVerified: Updated: "+this);
		}
		if(sync) Application.db.sync(new ArrayList<String>(Arrays.asList(table.organization.TNAME)));
		//organization_ID = orgID;
		if(params!=null) D_Organization.storeOrgParams(organization_ID, params.orgParam);

		return this._organization_ID=result;
	}
	private boolean hashConflictCreationDateDropThis() throws SQLiteException {
		D_PeerAddress o_creator = this.creator;
		this.creator = null;
		String this_hash=new String(this.getEntityEncoder().getBytes());
		creator = o_creator;
		D_Organization dorg;
		try {
			dorg = new D_Organization(this.global_organization_ID, this.global_organization_IDhash, true, false); // no creator
		} catch (Exception e) {
			e.printStackTrace();
			return false; // old not valid!
		}
		String old = new String(dorg.getEntityEncoder().getBytes());
		if(old.compareTo(this_hash)>=0) return true;
		return false;
	}
	public static long insertTemporaryGID(String global_organization_ID) throws SQLiteException {
		return insertTemporaryGID(global_organization_ID, null);
	}
	/**
	 * Default is to block temporary organizations
	 * @param global_organization_ID
	 * @param GID_hash
	 * @return
	 * @throws SQLiteException
	 */
	public static long insertTemporaryGID(String global_organization_ID, String GID_hash) throws SQLiteException {
		if(GID_hash==null) GID_hash = D_Organization.getOrgGIDHashGuess(global_organization_ID);
		return insertTemporaryGID(global_organization_ID, GID_hash, true);
	}
	public static long insertTemporaryGID(String global_organization_ID, String GID_hash, boolean default_blocked_org) throws SQLiteException {
		return insertTemporaryGID(global_organization_ID, GID_hash, default_blocked_org, null);
	}
	public static long insertTemporaryGID(String global_organization_ID, String GID_hash, boolean default_blocked_org, String name) throws SQLiteException {
		if(DEBUG) System.out.println("D_Organization:insertTemporaryGID: start "+GID_hash);
		boolean grass = Util.equalStrings_or_one_null(GID_hash, global_organization_ID);
		//boolean grass = Util.equalString_and_non_null(GID_hash, global_organization_ID);
		return Application.db.insert(table.organization.TNAME,
				new String[]{table.organization.global_organization_ID,
				table.organization.global_organization_ID_hash,
				table.organization.blocked,
				table.organization.broadcasted,
				table.organization.name,
				table.organization.certification_methods},
				new String[]{global_organization_ID,
				GID_hash,
				Util.bool2StringInt(default_blocked_org),
				"0",
				name,
				""+(grass?table.organization._GRASSROOT:table.organization._AUTHORITARIAN)
				},
				DEBUG);
	}

	public static long getLocalOrgID(String global_organization_ID) throws SQLiteException {
		String lID = getLocalOrgID_(global_organization_ID);
		if(lID==null) return -1;
		return new Integer(lID).longValue();
	}
	public static String getGlobalOrgID(String id) throws SQLiteException {
		if(DEBUG) System.out.println("D_Organization:getGlobalOrgID: start");
		String sql = "SELECT "+table.organization.global_organization_ID+" FROM "+table.organization.TNAME+
		" WHERE "+table.organization.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{id}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public static long getLocalOrgID(String orgGID, String orgID_hash) throws SQLiteException {
		if(orgGID!=null) return getLocalOrgID(orgGID);
		
		String sql = "SELECT "+table.organization.organization_ID+" FROM "+table.organization.TNAME+
		" WHERE "+table.organization.global_organization_ID_hash+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{orgID_hash}, DEBUG);
		if(o.size()==0) return -1;
		return new Integer(Util.getString(o.get(0).get(0))).longValue();
	}
	public static String getLocalOrgID_(String global_organization_ID) throws SQLiteException {
		String sql = "SELECT "+table.organization.organization_ID+" FROM "+table.organization.TNAME+
		" WHERE "+table.organization.global_organization_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{global_organization_ID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	public static D_MotionChoice[] getDefaultMotionChoices(String[] opts) {
		D_MotionChoice[] result;
		if(opts == null) return null;
		result = new D_MotionChoice[opts.length];
		for(int k=0; k<result.length; k++) {
			result[k]=new D_MotionChoice(opts[k], ""+k);
		}
		return result;		
	}
	public D_MotionChoice[] getDefaultMotionChoices() {
		D_MotionChoice[] result;
		String[] opts = this.params.default_scoring_options;
		if(opts==null) opts = get_DEFAULT_OPTIONS();
		result = getDefaultMotionChoices(opts);
		return result;
	}
	public static byte getASN1Type() {
		return TAG;
	}
	public static String[] get_DEFAULT_OPTIONS() {
		String[] DEFAULT_OPTIONS = new String[]{_("Endorse"),_("Abstain"),_("Oppose")};
		return DEFAULT_OPTIONS;
	}
	/**
	 * Now returns the ID even if the organization data is temporary (not signed), but not if it is blocked (default)
	 * @param orgHash
	 * @return
	 * @throws SQLiteException
	 */
	public static String getLocalOrgID_fromHashIfNotBlocked(String orgHash) throws SQLiteException {
		String sql = 
			"SELECT "+table.organization.organization_ID+
			" FROM "+table.organization.TNAME+
			" WHERE "+table.organization.global_organization_ID_hash+"=?"+
			" AND "+table.organization.blocked+" <> '1' ;";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{orgHash}, DEBUG);
		if(a.size()==0) return null;
		return Util.getString(a.get(0).get(0));
	}
	public static String getLocalOrgID_fromGIDIfNotBlocked(String orgGID) throws SQLiteException {
		String sql = 
			"SELECT "+table.organization.organization_ID+
			" FROM "+table.organization.TNAME+
			" WHERE "+table.organization.global_organization_ID+"=?"+
			" AND "+table.organization.blocked+" <> '1' ;";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{orgGID}, DEBUG);
		if(a.size()==0) return null;
		return Util.getString(a.get(0).get(0));
	}
	public void getLocalIDfromGIDandBlock() throws SQLiteException {
		String sql = 
				"SELECT "+
						table.organization.organization_ID+","
						+table.organization.blocked+","
						+table.organization.broadcasted+","
						+table.organization.requested+","
						+table.organization.plugins_excluding+","
						+table.organization.specific_requests+","
						+table.organization.motions_excluding+
				" FROM "+table.organization.TNAME+
				" WHERE "+table.organization.global_organization_ID+"=?;";
			ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{this.global_organization_ID}, DEBUG);
			if(a.size()==0){
				this.blocked = DD.BLOCK_NEW_ARRIVING_ORGS;
				return;
			}else{
				this.organization_ID = Util.getString(a.get(0).get(0));
				_organization_ID = Util.lval(organization_ID, -1);
				this.blocked = Util.stringInt2bool(a.get(0).get(1), true);
				this.broadcasted = Util.stringInt2bool(a.get(0).get(2), true);
				this.requested = Util.stringInt2bool(a.get(0).get(3), true);
//				this.plugins_excluding = Util.getString(a.get(0).get(4));
//				this.motions_excluding = Util.getString(a.get(0).get(6));
//				this.specific_requests = Util.getString(a.get(0).get(5));
			}
		
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
			"SELECT "+table.organization.organization_ID+","+table.organization.signature+
			" FROM "+table.organization.TNAME+
			" WHERE "+table.organization.global_organization_ID_hash+"=? ;";
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
	public void storeLocalFlags() throws SQLiteException {
		Application.db.update(table.organization.TNAME,
				new String[]{table.organization.broadcasted,table.organization.blocked,table.organization.requested},
				new String[]{table.organization.organization_ID},
				new String[]{Util.bool2StringInt(broadcasted),
				Util.bool2StringInt(blocked), 
				Util.bool2StringInt(requested),
				this.organization_ID}, _DEBUG);
	}	
	public static boolean compareOrgs(D_Organization o1, D_Organization o2){
		try{
			if((o1==null)&&(o2==null)) return true;
			if((o1==null)||(o2==null)) throw new Exception("");

			if(!Util.equalStrings_null_or_not(o1.version,o2.version)) throw new Exception("");
			if(!Util.equalStrings_null_or_not(o1.name,o2.name)) throw new Exception("");
			if(o1.params.certifMethods!=(o2.params.certifMethods)) throw new Exception("");
			if(!Util.equalStrings_null_or_not(o1.params.hash_org_alg,o2.params.hash_org_alg)) throw new Exception("");
			if(!o1.params.creation_time.equals(o2.params.creation_time)) throw new Exception("");
			if(!Util.equalStrings_null_or_not(o1.params.creator_global_ID,o2.params.creator_global_ID)) throw new Exception("");
			if(!Util.equalStrings_null_or_not(o1.params.category,o2.params.category)) throw new Exception("");
			if(!Util.equalBytes_null_or_not(o1.params.certificate, o2.params.certificate)) throw new Exception("");
			if(!Util.equalBytes_null_or_not(o1.signature, o2.signature)) throw new Exception("");
			if(!Util.equalBytes_null_or_not(o1.signature_initiator, o2.signature_initiator)) throw new Exception("");
		}catch(Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public static void main(String args[]){
		try {
			String source = Application.DELIBERATION_FILE;
			if(args.length>0) source = args[0];
			int id=1;
			if(args.length>1) id = Integer.parseInt(args[1]);
			if(args.length>2) DEBUG = Util.stringInt2bool((args[1]), false);
			Application.db = new DBInterface(source);
			//if(args.length>0){readSignSave(3,1); if(true) return;}
			
			//long id=simulator.Fill_database.add_organization();
			
			System.out.println("\n************Loading="+id);
			D_Organization c=new D_Organization(id);
			System.out.println("\n************Original\n**********\nrec="+c.toString());

			if(!c.verifySignature()) System.out.println("\n************Signature Failure\n**********\nread="+c.toSummaryString());
			else System.out.println("\n************Signature Pass\n**********\nread="+c.toSummaryString());
//
//			D_Organization c2=new D_Organization(c.global_organization_ID, c.global_organization_IDhash, true, false);
//			if(!c2.verifySignature()) System.out.println("\n************Signature Failure\n**********\nread="+c2.toSummaryString());
//			else System.out.println("\n************Signature Pass\n**********\nread="+c2.toSummaryString());

			Decoder dec = new Decoder(c.getEncoder().getBytes());
			System.out.println("\n************After transmission *******\n");
			D_Organization d = new D_Organization().decode(dec);
			compareOrgs(d, c);
			if(!d.verifySignature()) System.out.println("\n************Signature Failure\n**********\nrec="+d.toSummaryString());
			else System.out.println("\n************Signature Pass\n**********\nrec="+d.toSummaryString());
			//Calendar arrival_date = d.arrival_date=Util.CalendargetInstance();
			//if(d.global_organization_ID==null) d.global_organization_ID = OrgHandling.getGlobalOrgID(d.organization_ID);
			//d.global_organization_ID = d.make_ID();
			//boolean [] changed = new boolean[2];
			//d.storeVerified(changed);//arrival_date);
			
			System.out.println("\n************Obtained\n**********\nrec="+d.toString());
		} catch (SQLiteException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
}
