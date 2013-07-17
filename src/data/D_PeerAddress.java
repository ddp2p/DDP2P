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

import static java.lang.System.err;
import static java.lang.System.out;
import static util.Util._;
import hds.Address;
import hds.DDAddress;
import hds.DirectoryServer;
import hds.SR;
import hds.Server;
import hds.TypedAddress;

import java.awt.Component;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import util.P2PDDSQLException;

import streaming.UpdateMessages;
import streaming.UpdatePeersTable;
import table.organization;
import table.peer_org;
import util.Base64Coder;
import util.DBInterface;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import config.Application;
import config.DD;
import config.Identity;

public class D_PeerAddress extends ASNObj {
	public static final String SIGN_ALG_SEPARATOR = ":";
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	//private static final boolean encode_addresses = false;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	public String version=DDAddress.V1;
	public String globalID;  //PrintableString
	public String name; //UTF8 OPT
	public String slogan; //UTF8 OPT [APT 0]
	public TypedAddress[] address=null; // OPT // should be null when signing
	public boolean broadcastable;
	public String[] signature_alg = hds.SR.HASH_ALG_V1; // of PrintStr OPT
	public String hash_alg = D_PeerAddress.getStringFromHashAlg(hds.SR.HASH_ALG_V1);
	public byte[] signature = new byte[0]; // OCT STR OPT, should be new byte[0] when signing
	public Calendar creation_date;
	public byte[] picture = null;
	public D_PeerOrgs[] served_orgs = null; //OPT
	public String peer_ID;
	public String globalIDhash;
	public Calendar arrival_date;
	public String plugins_msg;
	public String plugin_info;
	// on addition of local fields do not forget to update them in setLocals()
	public boolean used = false; // whether I try to contact and pull/push from this peer
	public boolean filtered;
	//public boolean no_update;  // replaced by blocked
	public boolean blocked;
	public Calendar last_sync_date;
	// Last generalized date when last_sync_date was reset for all orgs
	// (once may need extension for each org separately
	public Calendar last_reset; 
	public String experience;
	public String exp_avg;
	public long _peer_ID = -1;

	static final Object monitor_init_myself = new Object();
	private static D_PeerAddress _myself = null;
	public static void init_myself(String gid) throws P2PDDSQLException {
		synchronized(monitor_init_myself){
			_myself = new D_PeerAddress(gid);
		}
	}
	public static void re_init_myself() throws P2PDDSQLException {
		synchronized(monitor_init_myself) {
			if(_myself==null) return; // Why?
			init_myself(_myself.globalID);
		}
	}
	/**
	 * Returns myself if not null, else tries to load it
	 * @param gid
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_PeerAddress get_myself(String gid) throws P2PDDSQLException{
		synchronized(monitor_init_myself) {
			if(gid == null) return _myself;
			if((_myself!=null) && (gid.equals(_myself))) return _myself;
			_myself = new D_PeerAddress(gid);
			return _myself;
		}		
	}

	public D_PeerAddress(D_PeerAddress pa) {
		globalID = pa.globalID; //Printable
		name = pa.name; //UTF8
		slogan = pa.slogan; //UTF8
		address = pa.address; //UTF8
		creation_date = pa.creation_date;
		signature = pa.signature; //OCT STR
		//global_peer_ID_hash =Util.getGIDhash(pa.globalID);
		broadcastable = pa.broadcastable;
		signature_alg = pa.signature_alg;
		version = pa.version;
		picture = pa.picture;
		served_orgs = pa.served_orgs; //OPT
		//type = pa.type;
	}

	public String toSummaryString() {
		String result = "\nPeerAddress: v=" +version+
				" [gID="+Util.trimmed(globalID)+" name="+((name==null)?"null":"\""+name+"\"")
		+" slogan="+(slogan==null?"null":"\""+slogan+"\"") + " date="+Encoder.getGeneralizedTime(creation_date);
		//result += " address="+Util.nullDiscrimArray(address, " --- ");
		//result += "\n\t broadcastable="+(broadcastable?"1":"0");
		//result += " sign_alg["+((signature_alg==null)?"NULL":signature_alg.length)+"]="+Util.concat(signature_alg, ":") +" sign="+Util.byteToHexDump(signature, " ");
		//result += " pict="+Util.byteToHexDump(picture, " ");
		result += " served_org["+((served_orgs!=null)?served_orgs.length:"")+"]="+Util.concat(served_orgs, "----", "\"NULL\"");
		return result+"]";
	}	
	public String toString() {
		String result = "\nPeerAddress: v=" +version+
				" [gID="+Util.trimmed(globalID)+" name="+((name==null)?"null":"\""+name+"\"")
		+" slogan="+(slogan==null?"null":"\""+slogan+"\"") + " date="+Encoder.getGeneralizedTime(creation_date);
		result += " address="+Util.nullDiscrimArray(address, " --- ");
		result += "\n\t broadcastable="+(broadcastable?"1":"0");
		result += " sign_alg["+((signature_alg==null)?"NULL":signature_alg.length)+"]="+Util.concat(signature_alg, ":") +" sign="+Util.byteToHexDump(signature, " ") 
		+" pict="+Util.byteToHexDump(picture, " ")+
		" served_org["+((served_orgs!=null)?served_orgs.length:"")+"]="+Util.concat(served_orgs, "----", "\"NULL\"");
		return result+"]";
	}
	public static String[] getHashAlgFromString(String hash_alg) {
		if(hash_alg!=null) return hash_alg.split(Pattern.quote(DD.APP_ID_HASH_SEP));
		return null;
	}
	private static String getStringFromHashAlg(String[] signature_alg) {
		if (signature_alg==null) return null;
		return Util.concat(signature_alg, DD.APP_ID_HASH_SEP);
	}
	public Encoder getSignatureEncoder(){
		if(DDAddress.V0.equals(version)) return getSignatureEncoder_V0();
		return getSignatureEncoder_V1();
	}
	private Encoder getSignatureEncoder_V1() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version,false));
		enc.addToSequence(new Encoder(globalID).setASN1Type(Encoder.TAG_PrintableString));
		if(name!=null)enc.addToSequence(new Encoder(name,Encoder.TAG_UTF8String));
		if(slogan!=null)enc.addToSequence(new Encoder(slogan,DD.TAG_AC0));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date));
		//if(address!=null)enc.addToSequence(Encoder.getEncoder(address));
		enc.addToSequence(new Encoder(broadcastable));
		//if(signature_alg!=null)enc.addToSequence(Encoder.getStringEncoder(signature_alg, Encoder.TAG_PrintableString));
		if((served_orgs!=null)&&(served_orgs.length>0)) {
			D_PeerOrgs[] old = served_orgs;
			served_orgs = makeSignaturePeerOrgs_VI(served_orgs);
			enc.addToSequence(Encoder.getEncoder(this.served_orgs).setASN1Type(DD.TAG_AC12));
			served_orgs = old;
		}
		//enc.addToSequence(new Encoder(signature));
		//enc.setASN1Type(TAG);
		return enc;
	}

	/**
	 * Assumes served orgs are already ordered by GID
	 * @param served_orgs
	 * @return
	 */
	public static D_PeerOrgs[] makeSignaturePeerOrgs_VI(D_PeerOrgs[] served_orgs) {
		D_PeerOrgs[] result = new D_PeerOrgs[served_orgs.length];
		for(int k=0;k<result.length;k++) {
			result[k] = new D_PeerOrgs();
			if(served_orgs[k].global_organization_IDhash != null) 
				result[k].global_organization_IDhash = served_orgs[k].global_organization_IDhash;
			else 
				result[k].global_organization_IDhash =
				D_Organization.getOrgGIDHashGuess(served_orgs[k].global_organization_ID);
		}
		result = sortByOrgGIDHash(result);
		return result;
	}

	public static D_PeerOrgs[] sortByOrgGIDHash(D_PeerOrgs[] result) {
		Arrays.sort(result, new Comparator<D_PeerOrgs>() {
			@Override
			public int compare(D_PeerOrgs o1, D_PeerOrgs o2) {
				String s1 = o1.global_organization_IDhash;
				if(s1==null) return -1;
                String s2 = o2.global_organization_IDhash;
				if(s2==null) return -1;
                return s1.compareTo(s2);
			}
       });
		return result;
	}

	public Encoder getSignatureEncoder_V0(){
		TypedAddress[] _address = address;
		byte[] _signature = signature;
		address = null;
		signature = new byte[0];
		Encoder enc = getEncoder();
		address = _address;
		signature = _signature;
		return enc;
	}
	public D_PeerAddress(){}
	/**
	 * 
	 * @param peerID
	 * @param failOnNew avoids getting a peer with inexistant ID
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(String peerID, boolean failOnNew) throws P2PDDSQLException{
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.peer_ID+"=?;";
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerID});
		if(p.size()==0){
			if(failOnNew) throw new RuntimeException("Absent peer "+peerID);
			peer_ID = peerID;
		}else{
			init(p.get(0));
		}
	}
	/**
	 * 
	 * @param peerGID
	 * @param peerGIDhash
	 * @param failOnNew
	 * @param _addresses
	 * @param _served_orgs
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(String peerGID, String peerGIDhash, boolean failOnNew, boolean _addresses, boolean _served_orgs) throws P2PDDSQLException{
		String sql =
				"SELECT "+table.peer.fields_peers+
				" FROM "+table.peer.TNAME+
				" WHERE ("+table.peer.global_peer_ID+"=? OR "+table.peer.global_peer_ID_hash+"=?);";
			//String _peerID = Util.getStringID();
			ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerGID, peerGIDhash});
			if(p.size()==0){
				//peer_ID = _peerID;
				if(failOnNew) throw new RuntimeException("Absent peer="+peerGID+" hash="+peerGIDhash);
			}else{
				init(p.get(0), _addresses, _served_orgs);
			}
	}
	/**
	 * 
	 * @param peerID
	 * @param failOnNew
	 * @param _addresses
	 * @param _served_orgs
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(long peerID, boolean failOnNew, boolean _addresses, boolean _served_orgs) throws P2PDDSQLException{
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.peer_ID+"=?;";
		String _peerID = Util.getStringID(peerID);
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{_peerID});
		if(p.size()==0){
			peer_ID = _peerID;
			if(failOnNew) throw new RuntimeException("Absent peer "+peerID);
		}else{
			init(p.get(0), _addresses, _served_orgs);
		}
	}
	/**
	 * 
	 * @param peerID
	 * @param failOnNew
	 * @param _addresses
	 * @param _served_orgs
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(String peerGID, boolean failOnNew, boolean _addresses, boolean _served_orgs) throws P2PDDSQLException{
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID+"=?;";
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerGID});
		if(p.size()==0){
			this.globalID = peerGID;
			if(failOnNew) throw new RuntimeException("Absent peer "+peerGID);
		}else{
			init(p.get(0), _addresses, _served_orgs);
		}
	}
	/**
	 * 
	 * @param peerGID
	 * @param isGID dummy parameter to differentiate from case with local peerID
	 * @param failOnNew
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(String peerGID, int isGID, boolean failOnNew) throws P2PDDSQLException{
		init(peerGID, isGID, failOnNew);
	}
	public void init(String peerGID, int isGID, boolean failOnNew) throws P2PDDSQLException{
		if(DEBUG) System.out.println("D_PeerAddress: init: GID");
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID+"=?;";
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerGID}, DEBUG);
		if(p.size()==0){
			if(failOnNew) throw new RuntimeException("Absent peer "+peerGID);
			this.globalID = peerGID;
		}else{
			init(p.get(0));
		}
	}
	public D_PeerAddress(String peerGID) throws P2PDDSQLException {
		init(peerGID, 1, false);
	}
	 
	/**
	 * 
	 * @param peerGID
	 * @param isGID dummy parameter to differentiate from case with local peerID
	 * @param failOnNew
	 * @param _addresses
	 * @param _served_orgs
	 * @throws P2PDDSQLException
	 */
	public D_PeerAddress(String peerGID, int isGID, boolean failOnNew, boolean _addresses, boolean _served) throws P2PDDSQLException{
			String sql =
				"SELECT "+table.peer.fields_peers+
				" FROM "+table.peer.TNAME+
				" WHERE "+table.peer.global_peer_ID+"=?;";
			ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerGID});
			if(p.size()==0){
				this.globalID = peerGID;
				if(failOnNew) throw new RuntimeException("Absent peer "+peerGID);
			}else{
				init(p.get(0), _addresses, _served);
			}
		}
	
	private void init(ArrayList<Object> p) {
		init(p, true, true);
	}
	private void init(ArrayList<Object> p, boolean encode_addresses, boolean encode_served_orgs) {
		version = Util.getString(p.get(table.peer.PEER_COL_VERSION));
		peer_ID = Util.getString(p.get(table.peer.PEER_COL_ID));
		_peer_ID = Util.lval(peer_ID, -1);
		
		globalID = Util.getString(p.get(table.peer.PEER_COL_GID)); //dd.globalID;
		globalIDhash = Util.getString(p.get(table.peer.PEER_COL_GID_HASH)); //dd.globalID;
		name =  Util.getString(p.get(table.peer.PEER_COL_NAME)); //dd.name;
		slogan = Util.getString(p.get(table.peer.PEER_COL_SLOGAN)); //dd.slogan;
		creation_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_CREATION)));
		picture = Util.byteSignatureFromString(Util.getString(p.get(table.peer.PEER_COL_PICTURE)));
		if(encode_served_orgs)
			try {
				served_orgs = getPeerOrgs(_peer_ID);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		if(encode_addresses) address = getPeerAddresses(peer_ID);
		
		broadcastable = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_BROADCAST)), false);
		hash_alg = Util.getString(p.get(table.peer.PEER_COL_HASH_ALG));
		signature_alg = D_PeerAddress.getHashAlgFromString(hash_alg);
		signature = Util.byteSignatureFromString(Util.getString(p.get(table.peer.PEER_COL_SIGN)));

		arrival_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_ARRIVAL)));
		last_sync_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_LAST_SYNC)));
		last_reset = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_LAST_RESET)), null);
		//no_update = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_NOUPDATE)), false);
		used = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_USED)), false);
		blocked = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_BLOCKED)), false);
		filtered = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_FILTERED)), false);
		plugin_info =  Util.getString(p.get(table.peer.PEER_COL_PLUG_INFO));
		plugins_msg = Util.getString(p.get(table.peer.PEER_COL_PLUG_MSG));
		experience = Util.getString(p.get(table.peer.PEER_COL_EXPERIENCE));
		exp_avg = Util.getString(p.get(table.peer.PEER_COL_EXP_AVG));
	}

	/**
	 * Used when exporting, building a request etc.
	 * @param globalID
	 * @return
	 */
	public static D_PeerOrgs[] _getPeerOrgs(long peer_ID) {
		D_PeerOrgs[] result = null;
		if(peer_org.DEBUG) System.out.println("\n************\npeer_org: getPeerOrgs: enter "+peer_ID);
		try {
			result = getPeerOrgs(peer_ID);
//			String local_peer_ID = ""+peer_ID;
//			String orgs = getPeerOrgs(local_peer_ID, null);
//			result = peer_org.peerOrgsFromString(orgs);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if(peer_org.DEBUG) System.out.println("\n************\npeer_org: getPeerOrgs: result "+result);
		return result;
	}
	public static D_PeerOrgs[] getPeerOrgs(long _peer_ID) throws P2PDDSQLException {
		String peer_ID = Util.getStringID(_peer_ID);
		if(DEBUG) System.out.println("peer_org: getPeerOrgs: "+peer_ID);
		D_PeerOrgs[] result = null;
		String queryOrgs = "SELECT o."+
					table.organization.global_organization_ID+
					", o."+table.organization.name+", po."+table.peer_org.last_sync_date+
					", o."+table.organization.global_organization_ID_hash+
					", po."+table.peer_org.served+
					", po."+table.peer_org.organization_ID+
				" FROM "+table.peer_org.TNAME+" AS po " +
				" LEFT JOIN "+table.organization.TNAME+" AS o ON (po."+table.peer_org.organization_ID+"==o."+table.organization.organization_ID+") " +
				" WHERE ( po."+table.peer_org.peer_ID+" == ? ) "+
				" AND po."+table.peer_org.served+"== '1'"+
				" ORDER BY o."+table.organization.global_organization_ID;
		ArrayList<ArrayList<Object>>p_data = Application.db.select(queryOrgs, new String[]{peer_ID}, DEBUG);
		result = new D_PeerOrgs[p_data.size()];
		for(int i=0; i < p_data.size(); i++) {
			ArrayList<Object> o = p_data.get(i);
			result[i] = new D_PeerOrgs();
			result[i].global_organization_ID = Util.getString(o.get(0));
			result[i].org_name = Util.getString(o.get(1));
			result[i].last_sync_date = Util.getString(o.get(2));
			//result[i].global_organization_IDhash = Util.getString(o.get(3));
			result[i].served = Util.stringInt2bool(o.get(4), false);
			result[i].organization_ID = Util.lval(o.get(5), -1);
			
			//String name64 = util.Base64Coder.encodeString(name);
			//String global_org_ID = Util.getString(o.get(0))+peer_org.ORG_NAME_SEP+name64;
			//if(DEBUG) System.out.println("peer_org: getPeerOrgs: next ="+global_org_ID);
			//if(orgs!=null){orgs.add(global_org_ID);}
			//if(null==result) result = global_org_ID;
			//else result = result+peer_org.ORG_SEP+global_org_ID;
			
		}
		if(DEBUG) System.out.println("peer_org: getPeerOrgs: result ="+result);
		return result;
	}

	/**
	 * Used when exporting, building a request etc.
	 * @param globalID
	 * @return
	 */
	public static D_PeerOrgs[] _getPeerOrgs(String globalID) {
		D_PeerOrgs[] result = null;
		if(peer_org.DEBUG) System.out.println("\n************\npeer_org: getPeerOrgs: enter "+Util.trimmed(globalID));
		try {
			long peer_ID = _getLocalPeerIDforGID(globalID);
			return _getPeerOrgs(peer_ID);
			/*
			String local_peer_ID = peer_ID+"";
			String orgs = UpdatePeersTable.getPeerOrgs(local_peer_ID, null);
			result = UpdatePeersTable.peerOrgsFromString(orgs);
			*/
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if(peer_org.DEBUG) System.out.println("\n************\npeer_org: getPeerOrgs: result "+result);
		return result;
	}

	/**
	 * Prepare peer_orgs for sync answer
	 * @param peer_ID
	 * @param orgs : An output (if non null), gathering organization_IDs in an ArrayList
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String getPeerOrgs(String peer_ID, ArrayList<String> orgs) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if(peer_org.DEBUG) System.out.println("peer_org: getPeerOrgs: "+peer_ID+" orgs="+Util.concat(orgs," ","NULL"));
		String result = null;
		String queryOrgs = "SELECT o."+
					table.organization.global_organization_ID+
					", o."+table.organization.name+", po."+table.peer_org.last_sync_date+
				" FROM "+table.peer_org.TNAME+" AS po " +
				" LEFT JOIN "+table.organization.TNAME+" AS o ON (po."+table.peer_org.organization_ID+"==o."+table.organization.organization_ID+") " +
				" WHERE ( po."+table.peer_org.peer_ID+" == ? ) "+
				" AND po."+table.peer_org.served+"== '1'"+
				" ORDER BY o."+table.organization.global_organization_ID;
		ArrayList<ArrayList<Object>>p_data = Application.db.select(queryOrgs, new String[]{peer_ID}, peer_org.DEBUG);
		for(ArrayList<Object> o: p_data) {
			String name = ""+Util.getString(o.get(1)); //make "null" for null
			String name64 = util.Base64Coder.encodeString(name);
			String global_org_ID = Util.getString(o.get(0))+peer_org.ORG_NAME_SEP+name64;
			if(peer_org.DEBUG) System.out.println("peer_org: getPeerOrgs: next ="+global_org_ID);
			if(orgs!=null){
					orgs.add(global_org_ID);
			}
			if(null==result) result = global_org_ID;
			else result = result+peer_org.ORG_SEP+global_org_ID;
		}
		if(peer_org.DEBUG) System.out.println("peer_org: getPeerOrgs: result ="+result);
		return result;
	}

	/**
	 * 			" "+
			global_peer_ID+","+
			global_peer_ID_hash+","+
			peer_ID+","+
			name+","+
			slogan+","+
			hash_alg+","+
			signature+","+
			creation_date+","+
			broadcastable+","+
			no_update+","+
			plugin_info+","+
			plugins_msg+","+
			filtered+","+
			last_sync_date+","+
			arrival_date+","+
			used+","+
			picture+","+
			exp_avg+","+
			experience
	 * @param global_peer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static ArrayList<Object> getD_PeerAddress_Info(String global_peer_ID) throws P2PDDSQLException {
		ArrayList<Object> result=null;
		if(DEBUG) System.err.println("UpdateMessages:get_peer_only: for: "+Util.trimmed(global_peer_ID));
		String sql = "SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID+" = ?;";
		ArrayList<ArrayList<Object>> dt=Application.db.select(sql, new String[]{global_peer_ID});
		if((dt.size()>=1) && (dt.get(0).size()>=1)) {
			result = dt.get(0); // Long.parseLong(dt.get(0).get(0).toString());
			//if(DEBUG) err.println("Client: found peer_ID "+result+" for: "+Util.trimmed(global_peer_ID));	
		}
		if(DEBUG) System.err.println("UpdateMessages:get_peer_only: exit: "+result);
		return result;
	}
	public D_PeerAddress(D_PeerAddress dd, boolean encode_addresses) {
		version = dd.version;
		globalID = dd.globalID;
		name = dd.name;
		slogan = dd.slogan;
		creation_date = dd.creation_date;
		picture = dd.picture;
		served_orgs = dd.served_orgs;
		if(encode_addresses){
			address = dd.address;
		}
		broadcastable = dd.broadcastable;
		signature_alg = dd.signature_alg;
		signature = dd.signature;
	}
	public static TypedAddress[] parseStringAddresses(String addr){
			String[]addresses_l = Address.split(addr);
			// may have to control addresses, to defend from DOS based on false addresses
			TypedAddress[] address = new TypedAddress[addresses_l.length];
			for(int k=0; k<addresses_l.length; k++) {
				address[k] = new TypedAddress();
				String taddr[] = addresses_l[k].split(Pattern.quote(Address.ADDR_PART_SEP),2);
				if(taddr.length < 2) continue;
				address[k].type = taddr[0];
				address[k].address = taddr[1];
			}	
			return address;
	}
	public D_PeerAddress(DDAddress dd, boolean encode_addresses) {
		version = dd.version;
		globalID = dd.globalID;
		name = dd.name;
		slogan = dd.slogan;
		creation_date = Util.getCalendar(dd.creation_date);
		picture = dd.picture;
		if(encode_addresses){
			String[]addresses_l = Address.split(dd.address);
			// may have to control addresses, to defend from DOS based on false addresses
			address = new TypedAddress[addresses_l.length];
			for(int k=0; k<addresses_l.length; k++) {
				address[k] = new TypedAddress();
				String taddr[] = addresses_l[k].split(Pattern.quote(Address.ADDR_PART_SEP),2);
				if(taddr.length < 2) continue;
				address[k].type = taddr[0];
				address[k].address = taddr[1];
			}
		}
		broadcastable = dd.broadcastable;
		signature_alg = dd.hash_alg;
		hash_alg = D_PeerAddress.getStringFromHashAlg(signature_alg);
		signature = dd.signature;
		served_orgs = dd.served_orgs;
	}

	/**
	 * version, globalID, name, slogan, creation_date, address*, broadcastable, signature_alg, served_orgs, signature*
	 */
	public Encoder getEncoder(){
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version,false));
		enc.addToSequence(new Encoder(globalID).setASN1Type(Encoder.TAG_PrintableString));
		if(name!=null)enc.addToSequence(new Encoder(name,Encoder.TAG_UTF8String));
		if(slogan!=null)enc.addToSequence(new Encoder(slogan,DD.TAG_AC0));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date));
		if(address!=null)enc.addToSequence(Encoder.getEncoder(address));
		enc.addToSequence(new Encoder(broadcastable));
		if(signature_alg!=null)enc.addToSequence(Encoder.getStringEncoder(signature_alg, Encoder.TAG_PrintableString));
		if((served_orgs!=null)&&(served_orgs.length>0))enc.addToSequence(Encoder.getEncoder(this.served_orgs).setASN1Type(DD.TAG_AC12));
		enc.addToSequence(new Encoder(signature));
		enc.setASN1Type(TAG);
		return enc;
	}
	public D_PeerAddress decode(Decoder dec) throws ASN1DecoderFail{
		Decoder content=dec.getContent();
		version=content.getFirstObject(true).getString();
		globalID=content.getFirstObject(true).getString();
		if(content.getTypeByte() == Encoder.TAG_UTF8String)
			name = content.getFirstObject(true).getString();
		else name=null;
		if(content.getTypeByte() == DD.TAG_AC0)
			slogan = content.getFirstObject(true).getString();
		else slogan = null;
		if(content.getTypeByte() == Encoder.TAG_GeneralizedTime)
			creation_date = content.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		else creation_date = null;
		if(content.getTypeByte() == Encoder.TYPE_SEQUENCE)
			address = content.getFirstObject(true)
			.getSequenceOf(Encoder.TYPE_SEQUENCE,
				new TypedAddress[]{}, new TypedAddress());
		else address=null;
		//if(content.getTypeByte() == Encoder.TAG_BOOLEAN)
		broadcastable = content.getFirstObject(true).getBoolean();
		if(content.getTypeByte() == Encoder.TYPE_SEQUENCE)
			signature_alg=content.getFirstObject(true)
			.getSequenceOf(Encoder.TAG_PrintableString);
		if(content.getTypeByte() == DD.TAG_AC12)
			served_orgs = content.getFirstObject(true)
			.getSequenceOf(Encoder.TYPE_SEQUENCE,
					new D_PeerOrgs[]{}, new D_PeerOrgs());
		if(content.getTypeByte() == Encoder.TAG_OCTET_STRING) 
			signature = content.getFirstObject(true).getBytes();
		return this;
	}
	public byte[] sign(ciphersuits.SK sk){
		//boolean DEBUG = true;
		TypedAddress[] addr = address;
		byte[] pict = picture;
		signature = new byte[0];
		address=null;
		picture=null;
		signature_alg = SR.HASH_ALG_V1;
		if(DEBUG)System.err.println("PeerAddress: sign: peer_addr="+this);
		byte msg[] = this.getSignatureEncoder().getBytes();
		if(DEBUG)System.out.println("PeerAddress: sign: msg["+msg.length+"]="+Util.byteToHex(msg)+" hash="+Util.getGID_as_Hash(msg));
		signature = Util.sign(msg,  sk);
		picture = pict;
		address = addr;
		if(DEBUG)System.err.println("PeerAddress: sign: sign=["+signature.length+"]"+Util.byteToHex(signature, "")+" hash="+Util.getGID_as_Hash(signature));
		return signature;
	}
	public boolean verifySignature(ciphersuits.PK pk) {
		//boolean DEBUG = true;
		if(DEBUG)System.err.println("PeerAddress: verifySignature: this="+this);
		boolean result;
		byte[] sgn = signature;
		if(DEBUG)System.err.println("PeerAddress: verifySignature: sign=["+signature.length+"]"+Util.byteToHex(signature, "")+" hash="+Util.getGID_as_Hash(signature));
		TypedAddress[] addr = address;
		byte[] pict = picture;
		signature=new byte[0];
		address=null;
		picture = null;
		//result = Util.verifySign(this, pk, sgn);
		byte msg[] =  this.getSignatureEncoder().getBytes();
		if(DEBUG)System.out.println("PeerAddress: verifySignature: msg["+msg.length+"]="+Util.byteToHex(msg)+" hash="+Util.getGID_as_Hash(msg));
		result = Util.verifySign(msg, pk, sgn);
		if(result==false) if(_DEBUG)System.out.println("PeerAddress:verifySignature: Failed verifying: "+this+"\n sgn="+Util.byteToHexDump(sgn,":"));
		picture = pict;
		address = addr;
		signature=sgn;
		return result;
	}
	/**
	 * Verifying with SK=this.globalID
	 * @return
	 */
	public boolean verifySignature() {
		//boolean DEBUG = true;
		if(DEBUG)System.err.println("PeerAddress: verifySignature: peer_addr="+this);
		boolean result = verifySignature(ciphersuits.Cipher.getPK(globalID));
		if(DEBUG)System.err.println("PeerAddress: verifySignature: got="+result);
		return result;
	}
	/**
	 * Parses addresses found in the DDAddress string form
	 * @param addresses
	 * @return
	 */
	public static TypedAddress[] getAddress(String addresses) {
		if(DEBUG) System.out.println("PeerAddress:getAddress: parsing addresses "+addresses);
		String[]addresses_l = Address.split(addresses);
		// may have to control addresses, to defend from DOS based on false addresses
		TypedAddress[] address = new TypedAddress[addresses_l.length];
		for(int k=0; k<addresses_l.length; k++) {
			if(DEBUG)System.out.println("PeerAddress:getAddress: parsing address "+addresses_l[k]);
			address[k] = new TypedAddress();
			String taddr[] = addresses_l[k].split(Pattern.quote(Address.ADDR_TYPE_SEP),2);
			if(taddr.length < 2) continue;
			address[k].type = taddr[0];
			address[k].address = taddr[1];
			if(DEBUG)System.out.println("PeerAddress:getAddress: got typed address "+address[k]);
		}
		return address;
	}
	public static D_PeerAddress getPeerAddress(String peer_ID, boolean _addresses, boolean _served) {
		D_PeerAddress result=null;
		try {
			result = new D_PeerAddress(Util.lval(peer_ID,-1), false, _addresses, _served);
		} catch (Exception e1) {
			e1.printStackTrace();
			return null;
		}
		String sql = "SELECT "+table.peer.broadcastable+
			","+table.peer.creation_date+
			","+table.peer.global_peer_ID+
			","+table.peer.hash_alg+
			","+table.peer.name+
			","+table.peer.picture+
			","+table.peer.signature+
			","+table.peer.slogan+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.peer_ID+"=?;";
		ArrayList<ArrayList<Object>> p;
		try {
			p = Application.db.select(sql, new String[]{peer_ID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		if(p.size()==0) return null;
		if(DEBUG)System.out.println("D_PeerAddress:init:old peerID="+peer_ID);
		result.broadcastable="1".equals(p.get(0).get(0));
		result.creation_date=Util.getCalendar(Util.getString(p.get(0).get(1)));
		result.globalID=Util.getString(p.get(0).get(2));
		String hash_alg = Util.getString(p.get(0).get(3));
		if(hash_alg!=null) result.signature_alg=hash_alg.split(Pattern.quote(DD.APP_ID_HASH_SEP));
		result.name=Util.getString(p.get(0).get(4));
		result.picture=Util.byteSignatureFromString(Util.getString(p.get(0).get(5)));
		result.signature=Util.byteSignatureFromString(Util.getString(p.get(0).get(6)));
		result.slogan=Util.getString(p.get(0).get(7));
		String addresses=null;
		if(_addresses) {
			try {
				addresses = UpdatePeersTable.getPeerAddresses(peer_ID, Encoder.getGeneralizedTime(0), Util.getGeneralizedTime());
				result.address=D_PeerAddress.getAddress(addresses);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		if(_served) result.served_orgs=data.D_PeerAddress._getPeerOrgs(result.globalID);;
		return result;
	}
	public static TypedAddress[] getPeerAddresses(String peer_ID){
		TypedAddress[] result = null;
		try {
			String addresses = UpdatePeersTable.getPeerAddresses(peer_ID, Encoder.getGeneralizedTime(0), Util.getGeneralizedTime());
			result=D_PeerAddress.getAddress(addresses);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	static public long _getLocalPeerIDforGID(String peerGID) throws P2PDDSQLException{
		long result=-1;
		String adr = getLocalPeerIDforGID(peerGID);
		if(adr==null){
			if(DEBUG)System.out.println("DD_PeerAddress: _getLocalPeerIDforGID: null addr result = "+result);
			return result;
		}
		result = Util.lval(adr, -1);
		if(DEBUG)System.out.println("DD_PeerAddress: _getLocalPeerIDforGID: result = "+result);
		return result;
	}
	public static String getLocalPeerIDforGID(String peerGID) throws P2PDDSQLException {
		if(DEBUG)System.out.println("DD_PeerAddress: getLocalPeerIDforGID");
		String sql = "SELECT "+table.peer.peer_ID+" FROM "+table.peer.TNAME+
		" WHERE "+table.peer.global_peer_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{peerGID}, DEBUG);
		if(o.size()==0) return null;
		String result = Util.getString(o.get(0).get(0));
		if(DEBUG)System.out.println("DD_PeerAddress: getLocalPeerIDforGID: result = "+result);
		return result;
	}
	public static String getPeerGIDforID(String peerID) throws P2PDDSQLException {
		if(DEBUG)System.out.println("DD_PeerAddress: getPeerGIDforID");
		String sql = "SELECT "+table.peer.global_peer_ID+" FROM "+table.peer.TNAME+
		" WHERE "+table.peer.peer_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{peerID}, DEBUG);
		if(o.size()==0) return null;
		String result = Util.getString(o.get(0).get(0));
		if(DEBUG)System.out.println("DD_PeerAddress: getPeerGIDforID: result = "+result);
		return result;
	}
	public static String getLocalPeerIDforGIDhash(String peerGIDhash) throws P2PDDSQLException {
		String sql = 
			"SELECT "+table.peer.peer_ID+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID_hash+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{peerGIDhash}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	/**
	 * Adds a "P:" in front f the hash of the GID
	 * @param GID
	 * @return
	 */
	public static String getGIDHashFromGID(String GID){
		return "P:"+Util.getGIDhash(GID);
	}
	// These are not yet used...
	/**
	 * Checks if there is a P: in front, else generates a GIDhash with getGIDHashFromGID
	 */
	public static String getGIDHashGuess(String s) {
		if(s.startsWith("P:")) return s; // it is an external
		String hash = D_PeerAddress.getGIDHashFromGID(s);
		if(hash.length() != s.length()) return hash;
		return s;
	}
	public static long insertTemporaryGID(String peerGID) throws P2PDDSQLException{
		return insertTemporaryGID(peerGID, getGIDHashFromGID(peerGID));
	}
	public static long insertTemporaryGID(String peerGID, String peerGIDhash) throws P2PDDSQLException{
		return insertTemporaryGID(peerGID, peerGIDhash, Util.getGeneralizedTime());
	}
	public static long insertTemporaryGID(String peerGID, String peerGIDhash, String crt_date) throws P2PDDSQLException{
		long result=-1;
		if(DEBUG) System.out.println("\n\n******\nPeerAddress:insertTemporaryPeerGID: start");
		if(peerGIDhash==null) peerGIDhash = getGIDHashFromGID(peerGID);
		result = Application.db.insert(table.peer.TNAME,
				new String[]{table.peer.global_peer_ID, table.peer.global_peer_ID_hash, table.peer.arrival_date},
				new String[]{peerGID, peerGIDhash, crt_date},
				DEBUG);
		if(DEBUG) System.out.println("\n\n******\nPeerAddress:insertTemporaryPeerGID: got peerID="+result);
		return result;
	}
	/**
	 * 
	 * @param creator_global_ID
	 * @param pa, if present, it is verified and stored
	 * @param crt_date
	 * @return
	 */
	public static String storePeerAndGetOrInsertTemporaryLocalForPeerGID(String creator_global_ID, D_PeerAddress pa, String crt_date) {
		if(pa!=null){
			String c_ID = null;
			try {
				c_ID = storeReceived(pa, Util.getCalendar(crt_date), crt_date);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			if(creator_global_ID.equals(pa.globalID)) return c_ID;
		}
		try {
			//long peer_ID = UpdateMessages.getonly_organizationID(creator_global_ID, null);
			long peer_ID = D_PeerAddress._getLocalPeerIDforGID(creator_global_ID);
			if(peer_ID>=0) return Util.getStringID(peer_ID);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		long id;
		try {
			id = D_PeerAddress.insertTemporaryGID(creator_global_ID, null, crt_date);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
			return null;
		}
		return Util.getStringID(id);
	}

	/**
	 * 
	 * @param pa
	 * @param crt_date
	 * @param _crt_date 
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String storeReceived(D_PeerAddress pa, Calendar crt_date, String _crt_date) throws P2PDDSQLException {
		//try{
			return Util.getStringID(_storeReceived(pa,crt_date, _crt_date));
		//}catch(Exception e){e.printStackTrace(); throw e;}
	}
	/**
	 * 
	 * @param pa
	 * @param crt_date
	 * @param _crt_date 
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long _storeReceived(D_PeerAddress pa, Calendar crt_date, String _crt_date) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if(DEBUG) System.err.println("D_PeerAddress: store: start");
		if((pa == null) || (pa.globalID == null)){
			if(_DEBUG) System.err.println("D_PeerAddress: store: null "+pa);
			return -1;
		}
		D_PeerAddress local = new D_PeerAddress(pa.globalID, 0, false);
		if(local.peer_ID==null)
			if(DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME) return -1;
		
		if(!pa.verifySignature()){
			System.err.println("D_PeerAddress:store: Error verifying peer: "+pa);
			//return D_PeerAddress.getLocalPeerIDforGID(pa.globalID);
			return local._peer_ID;
		}
		local.setRemote(pa);
		return local._storeVerified(crt_date, _crt_date);
	}
	public void setRemote(D_PeerAddress pa) {
		this.set(pa.globalID, pa.name, pa.arrival_date, pa.slogan,
				this.used, pa.broadcastable, pa.hash_alg, pa.signature,
				pa.creation_date, pa.picture, pa.version);
		this.address = pa.address;
		this.served_orgs = pa.served_orgs;
	}
	
	public void setLocals(D_PeerAddress la){
		this.used = la.used;
		this._peer_ID = la._peer_ID;
		this.exp_avg = la.exp_avg;
		this.experience = la.experience;
		this.filtered = la.filtered;
		this.last_sync_date = la.last_sync_date;
		//this.no_update = la.no_update;
		this.peer_ID = la.peer_ID;
		this.plugin_info = la.plugin_info;
		this.plugins_msg = la.plugins_msg;
		this.used = la.used;
		this.blocked = la.blocked;
		this.last_reset = la.last_reset;
		//this.signature_alg = la.signature_alg; // do not use local hash_alg
		if(this.picture==null) this.picture = la.picture;
	}

	public static long storeVerified(String global_peer_ID, String name, String adding_date, String slogan,
			boolean used, boolean broadcastable, String hash_alg, byte[] signature, String creation_date, byte[] picture, String version, D_PeerOrgs[] served_orgs2) throws P2PDDSQLException {
		// boolean DEBUG = true;
		if(DEBUG) System.err.println("D_PeerAddress: storeVerified: ");
		//long result_peer_ID=-1;
		D_PeerAddress dpa = new D_PeerAddress(global_peer_ID, 0, false);

		String old_creation_date = Encoder.getGeneralizedTime(dpa.creation_date);
		if((creation_date == null) ||
				((old_creation_date!=null)&&(creation_date.compareTo(old_creation_date) <= 0)) ||
				(
						Util.equalStrings_null_or_not(dpa.name,name)
						&& Util.equalStrings_null_or_not(dpa.slogan,slogan)
						&& (dpa.broadcastable == broadcastable)
						&& Util.equalStrings_null_or_not(dpa.hash_alg,hash_alg)
				//&& Util.eq(dpa.signature,sign)
				//&& Util.eq(dpa.creation_date.toString(),creation_date)
				//&& Util.eq(dpa.signature,picture_str)
				)){
			if(DEBUG) System.err.println("D_PeerAddress: storeVerified: do not change!");
			return dpa._peer_ID;
		}
		
		dpa.set( global_peer_ID,  name,  adding_date,  slogan,
				 used,  broadcastable,  hash_alg,  signature,  creation_date,  picture,  version);
		dpa.served_orgs = served_orgs2;
		if(DEBUG) System.err.println("D_PeerAddress: storeVerified: will save!");
		return dpa._storeVerified(adding_date);
	}
	// public String storeVerified() throws P2PDDSQLException{return storeVerified(Util.getGeneralizedTime());}
	public long _storeVerified() throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified()");
		return _storeVerified(Util.CalendargetInstance());
	}
	public String storeVerified() throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: storeVerified()");
		return Util.getStringID(_storeVerified(Util.CalendargetInstance()));
	}
	public String storeVerified(String crt_date) throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: storeVerified(date)");
		return Util.getStringID(_storeVerified(crt_date));
	}
	public long _storeVerified(String crt_date) throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(date)");
		Calendar _crt_date = Util.getCalendar(crt_date);
		return _storeVerified(_crt_date, crt_date);
	}
	public long _storeVerified(Calendar _crt_date) throws P2PDDSQLException{
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(calendar)");
		String crt_date = Encoder.getGeneralizedTime(_crt_date);
		return _storeVerified(_crt_date, crt_date);
	}	
	public long _storeVerified(Calendar _crt_date, String crt_date) throws P2PDDSQLException{
		//boolean DEBUG=true;
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(_crt_date, crt_date)");
		this.arrival_date = _crt_date;
		/*
		long peer_ID=D_PeerAddress.storeVerified(this.globalID, this.name, crt_date, this.slogan, false, 
				this.broadcastable, Util.concat(this.signature_alg,D_PeerAddress.SIGN_ALG_SEPARATOR), this.signature,
				Encoder.getGeneralizedTime(this.creation_date), this.picture, this.version);
		*/
		
		D_PeerAddress old = new D_PeerAddress(this.globalID);
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified: old="+old);
		if((old._peer_ID>0) && (old.creation_date!=null)) {
			if(this.creation_date==null) return -1;
			int cmp_creation_date = old.creation_date.compareTo(this.creation_date);
			if(cmp_creation_date > 0){
				if(DEBUG) System.err.println("D_PeerAddress: _storeVerified: newer old=: "+old+"\n vs new: "+this);
				return old._peer_ID;
			}
			this._peer_ID = old._peer_ID;
			this.peer_ID = old.peer_ID;

			if((cmp_creation_date==0) || identical_except_addresses(old)) {
				for(int k=0; k<this.address.length; k++) { // should be done only for addresses not in old
					if(knownAddress(old.address,this.address[k].address, this.address[k].type)) continue;
					//long address_ID = 
					D_PeerAddress.get_peer_addresses_ID(this.address[k].address,
							this.address[k].type, old._peer_ID, crt_date);			
				}
				if(DEBUG) System.err.println("D_PeerAddress: __storeVerified: identical: "+ old._peer_ID);
				return old._peer_ID;
			}
		}
		long new_peer_ID = doSave_except_Served_and_Addresses();
		if(this.address!=null)
			for(int k=0; k<this.address.length; k++) {
				if((old.peer_ID!=null)&&knownAddress(old.address,this.address[k].address, this.address[k].type)) continue;
				//long address_ID = 
				D_PeerAddress.get_peer_addresses_ID(this.address[k].address,
						this.address[k].type, new_peer_ID, crt_date);
		}
		//long peers_orgs_ID = get_peers_orgs_ID(peer_ID, global_organizationID);
		//long organizationID = get_organizationID(global_organizationID, org_name);
		if((old.peer_ID==null) || !this.identical_served(old.served_orgs)) D_PeerAddress.integratePeerOrgs(this.served_orgs, new_peer_ID, crt_date);
		if(DEBUG) System.err.println("D_PeerAddress: _storeVerified(...): got ID ="+new_peer_ID);
		return new_peer_ID;
		//return UpdateMessages.get_and_verify_peer_ID(pa, crt_date);
	}
	private static boolean knownAddress(TypedAddress[] old, String address3,
			String type) {
		if(old==null) return false;
		for(int k=0; k<old.length; k++) {
			if(Util.equalStrings_null_or_not(old[k].address, address3) && Util.equalStrings_null_or_not(old[k].type, type))
				return true;
		}
		return false;
	}

	private boolean identical_except_addresses(D_PeerAddress old) {
		if(Util.equalBytes(this.signature, old.signature)) return true;
		if(!Util.equalStrings_null_or_not(this.slogan, old.slogan)) return false;
		if(!Util.equalStrings_null_or_not(this.name, old.name)) return false;
		if(this.broadcastable != old.broadcastable) return false;
		if(this.blocked != old.blocked) return false;
		if(this.used != old.used) return false;
		if(this.filtered != old.filtered) return false;
		//if(this.no_update != old.no_update) return false;
		if(!Util.equalCalendars_null_or_not(this.last_reset, old.last_reset)) return false;
		if(!Util.equalCalendars_null_or_not(this.last_sync_date, old.last_sync_date)) return false;

		return identical_served(old.served_orgs);
	}

	/**
	 * should copy last_sync_date from old
	 * @param old
	 * @return
	 */
	private boolean identical_served(D_PeerOrgs[] old) {
		boolean result = true;
		if((this.served_orgs==null)^(old==null)) return false;
		if(this.served_orgs == null) return true;
		if(this.served_orgs.length != old.length) result = false;
		D_PeerOrgs s;
		for(int i=0; i<this.served_orgs.length; i++){
			s = this.served_orgs[i];
			int j=0;
			if((i<old.length)&&
					Util.equalStrings_null_or_not(s.global_organization_ID,old[i].global_organization_ID)&&
					Util.equalStrings_null_or_not(s.global_organization_IDhash,old[i].global_organization_IDhash)
			) j=i;
			else
				for(j=0; j<old.length; j++) {
					if(Util.equalStrings_null_or_not(s.global_organization_ID,old[j].global_organization_ID)) break;
				}
			if(j>=old.length) {result = false; continue;}
			this.served_orgs[i].last_sync_date = old[j].last_sync_date;
			//if(!Util.equalStrings_null_or_not(s.global_organization_ID,old[i].global_organization_ID)) return false;
			//if(!Util.equalStrings_null_or_not(s.global_organization_IDhash,old[i].global_organization_IDhash)) return false;
			
		}
		return result;
	}

	/*
	 * 		arrival_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_ARRIVAL)));
		last_sync_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_LAST_SYNC)));
		no_update = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_NOUPDATE)), false);
		used = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_USED)), false);
		filtered = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_FILTERED)), false);
		plugin_info =  Util.getString(p.get(table.peer.PEER_COL_PLUG_INFO));
		plugins_msg = Util.getString(p.get(table.peer.PEER_COL_PLUG_MSG));
		experience = Util.getString(p.get(table.peer.PEER_COL_EXPERIENCE));
		exp_avg = Util.getString(p.get(table.peer.PEER_COL_EXP_AVG));
	 */
	private long doSave_except_Served_and_Addresses() throws P2PDDSQLException{
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("\n\n***********\nD_PeerAddress: doSave: "+this);
		if(peer_ID!=null) if(_peer_ID<=0) _peer_ID = Util.lval(peer_ID, -1);
		if(_peer_ID>0) if(peer_ID == null) peer_ID = Util.getStringID(_peer_ID);
		if(peer_ID==null) {
			if(this.globalID!=null) peer_ID = D_PeerAddress.getLocalPeerIDforGID(globalID);
			else if(this.globalIDhash!=null) peer_ID = D_PeerAddress.getLocalPeerIDforGIDhash(globalIDhash);
			_peer_ID = Util.lval(peer_ID, -1);
		}
		if(globalID!=null) globalIDhash = D_PeerAddress.getGIDHashFromGID(globalID);
		
		if(hash_alg==null) hash_alg = D_PeerAddress.getStringFromHashAlg(signature_alg);
		
		String params[];
		if(peer_ID==null) params = new String[table.peer.PEER_COL_FIELDS_NO_ID];
		else params = new String[table.peer.PEER_COL_FIELDS];
		params[table.peer.PEER_COL_GID]=globalID;
		params[table.peer.PEER_COL_GID_HASH]=globalIDhash;
		params[table.peer.PEER_COL_NAME]=name;
		params[table.peer.PEER_COL_SLOGAN]=slogan;
		params[table.peer.PEER_COL_HASH_ALG]=hash_alg;
		params[table.peer.PEER_COL_SIGN]=Util.stringSignatureFromByte(signature);
		params[table.peer.PEER_COL_CREATION]=Encoder.getGeneralizedTime(creation_date);
		params[table.peer.PEER_COL_BROADCAST]=Util.bool2StringInt(broadcastable);
		//params[table.peer.PEER_COL_NOUPDATE]=Util.bool2StringInt(no_update);
		params[table.peer.PEER_COL_PLUG_INFO]=plugin_info;
		params[table.peer.PEER_COL_PLUG_MSG]=plugins_msg;
		params[table.peer.PEER_COL_FILTERED]=Util.bool2StringInt(filtered);
		params[table.peer.PEER_COL_LAST_SYNC]=Encoder.getGeneralizedTime(last_sync_date);
		params[table.peer.PEER_COL_ARRIVAL]=Encoder.getGeneralizedTime(arrival_date);
		params[table.peer.PEER_COL_LAST_RESET]=(last_reset!=null)?Encoder.getGeneralizedTime(last_reset):null;
		params[table.peer.PEER_COL_USED]=Util.bool2StringInt(used);
		params[table.peer.PEER_COL_BLOCKED]=Util.bool2StringInt(blocked);
		params[table.peer.PEER_COL_PICTURE]=Util.stringSignatureFromByte(picture);
		params[table.peer.PEER_COL_EXP_AVG]=exp_avg;
		params[table.peer.PEER_COL_EXPERIENCE]=experience;
		params[table.peer.PEER_COL_VERSION]=version;
		if(peer_ID==null){
			_peer_ID = Application.db.insert(table.peer.TNAME,
					Util.trimmed(table.peer.fields_peers_no_ID.split(",")),
					params,
					DEBUG);
			peer_ID = Util.getStringID(_peer_ID);
			if(DEBUG) System.out.println("D_PeerAddress: doSave: inserted "+_peer_ID);
		}else{
			params[table.peer.PEER_COL_ID]=peer_ID;
			if("-1".equals(peer_ID))Util.printCallPath("peer_ID -1: "+this);
			Application.db.update(table.peer.TNAME,
					Util.trimmed(table.peer.fields_peers_no_ID.split(",")),
					new String[]{table.peer.peer_ID},
					params,
					DEBUG);
			if(DEBUG) System.out.println("D_PeerAddress: doSave: updated "+peer_ID);
		}
		if(DEBUG) System.out.println("D_PeerAddress: doSave: return "+_peer_ID);
		return _peer_ID;
	}
	/**
	 * Called directly when saving DDAddress
	 * @param global_peer_ID
	 * @param name
	 * @param adding_date
	 * @param slogan
	 * @param used
	 * @param broadcastable
	 * @param hash_alg
	 * @param signature
	 * @param creation_date
	 * @param picture
	 * @param version
	 */
	public void set(String global_peer_ID, String name, Calendar adding_date, String slogan,
			boolean used, boolean broadcastable, String hash_alg, byte[] signature, Calendar creation_date, byte[] picture, String version) {
		this.globalID = global_peer_ID;
		this.name = name;
		this.arrival_date = adding_date;
		this.slogan = slogan;
		this.used = used;
		this.broadcastable = broadcastable;
		this.hash_alg = hash_alg;
		this.signature_alg = D_PeerAddress.getHashAlgFromString(hash_alg);
		this.signature = signature;
		this.creation_date = creation_date;
		if(this.picture == null)this.picture = picture; // this condition should be removed if picture is part of signature
		this.version = version;
	}
	/**
	 * Called when saving a received peer in sync request
	 * @param global_peer_ID
	 * @param name
	 * @param adding_date
	 * @param slogan
	 * @param used
	 * @param broadcastable
	 * @param hash_alg
	 * @param signature
	 * @param creation_date
	 * @param picture
	 * @param version
	 */
	public void set(String global_peer_ID, String name, String adding_date, String slogan,
				boolean used, boolean broadcastable, String hash_alg, byte[] signature, String creation_date, byte[] picture, String version) {
		set(global_peer_ID, name, Util.getCalendar(adding_date), slogan, used, broadcastable, hash_alg, signature, Util.getCalendar(creation_date), picture, version);
	}
	/*		
	public static long storeVerified(String global_peer_ID, String name, String adding_date, String slogan,
			boolean used, boolean broadcastable, String hash_alg, byte[] signature, String creation_date, byte[] picture, String version) throws P2PDDSQLException {
		// boolean DEBUG = true;
		long result=-1;
		String broadcast = broadcastable?"1":"0";
		if(DEBUG) System.err.println("UpdateMessages:get_peer_ID: for: "+Util.trimmed(global_peer_ID)+" new slogan="+slogan+" broadcast="+broadcast+" hash_alg="+hash_alg+" creation_date="+creation_date);
		String sql = "SELECT "+table.peer.fields_peers+
				" FROM "+table.peer.TNAME+" WHERE "+table.peer.global_peer_ID+" = ?;";
		String sign = Util.stringSignatureFromByte(signature);
		ArrayList<ArrayList<Object>> dto=Application.db.select(sql, new String[]{global_peer_ID}, DEBUG);
		if((dto.size()>=1) && (dto.get(0).size()>=1)) {
			ArrayList<Object> _dt = dto.get(0);
			result = Long.parseLong(_dt.get(table.peer.PEER_COL_ID).toString());
			if(DEBUG) System.err.println("UpdateMessages:get_peer_ID: found peer_ID "+result+" for: "+Util.trimmed(global_peer_ID));
			String picture_str = null;
			if(picture!=null) picture_str = Util.byteToHex(picture);
			if(DEBUG) System.err.println("UpdateMessages:get_peer_ID: got "
					+_dt.get(table.peer.PEER_COL_ID)+","+_dt.get(table.peer.PEER_COL_NAME)+","
					+_dt.get(table.peer.PEER_COL_SLOGAN)+","+_dt.get(table.peer.PEER_COL_BROADCAST)+","
					+_dt.get(table.peer.PEER_COL_HASH_ALG)+","+_dt.get(table.peer.PEER_COL_SIGN)+","+_dt.get(table.peer.PEER_COL_PICTURE));
			String old_creation_date = Util.getString(_dt.get(table.peer.PEER_COL_CREATION));
			if((creation_date == null) ||
					((old_creation_date!=null)&&(creation_date.compareTo(old_creation_date) <= 0)) ||
					(
							Util.eq(Util.getString(_dt.get(table.peer.PEER_COL_NAME)),name)
							&& Util.eq(Util.getString(_dt.get(table.peer.PEER_COL_SLOGAN)),slogan)
							&& Util.eq(Util.getString(_dt.get(table.peer.PEER_COL_BROADCAST)),broadcast)
							&& Util.eq(Util.getString(_dt.get(table.peer.PEER_COL_HASH_ALG)),hash_alg)
					//&& Util.eq(_dt.get(table.peer.PEER_COL_SIGN).toString(),sign)
					//&& Util.eq(_dt.get(table.peer.PEER_COL_CREATION).toString(),creation_date)
					//&& Util.eq(_dt.get(table.peer.PEER_COL_SIGNATURE).toString(),picture_str)
					))
				return result;
			if(picture_str == null) picture_str = Util.getString(_dt.get(table.peer.PEER_COL_PICTURE));
			if (DEBUG)
					System.out.println("UpdateMessages:get_peer_IDChanged \""+_dt.get(table.peer.PEER_COL_NAME)+"\" to \""+name+"\" or \""+(String)_dt.get(table.peer.PEER_COL_SLOGAN)+"\" to \""+slogan+"\"");
			Application.db.update(table.peer.TNAME,
					new String[]{table.peer.name,table.peer.slogan,table.peer.arrival_date,
								table.peer.broadcastable, table.peer.hash_alg, table.peer.signature,
								table.peer.picture, table.peer.creation_date, table.peer.version},
					new String[]{table.peer.peer_ID},
					new String[]{name, slogan, adding_date,
								broadcast, hash_alg, sign, picture_str,creation_date, Util.getStringID(result), version},
					DEBUG);
			if(DEBUG) System.err.println("UpdateMessages:get_peer_ID: Updated slogan");
			return result;
		}
		String IDhash = Util.getGIDhash(global_peer_ID);
		result=Application.db.insert(table.peer.TNAME,
				new String[]{table.peer.global_peer_ID,table.peer.global_peer_ID_hash,table.peer.name,table.peer.arrival_date,table.peer.slogan, table.peer.used, table.peer.broadcastable, table.peer.hash_alg, table.peer.signature, table.peer.version},
				new String[]{global_peer_ID, IDhash, name, adding_date, slogan, used?"1":"0", broadcastable?"1":"0", hash_alg, sign, version},
				DEBUG);
		if(DEBUG) System.err.println("UpdateMessages:get_peer_ID: new peer_ID "+result+" for: "+Util.trimmed(global_peer_ID));
		return result;
	}
*/
	/**
	 * update signature
	 * @param peer_ID should exist
	 * @param signer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long readSignSave(long peer_ID, long signer_ID) throws P2PDDSQLException {
		D_PeerAddress w=new D_PeerAddress(Util.getStringID(peer_ID), true);
		ciphersuits.SK sk = util.Util.getStoredSK(w.globalID);
		w.sign(sk);
		return w._storeVerified();
	}
	public D_PeerAddress instance() throws CloneNotSupportedException{
		return new D_PeerAddress();
	}


	/**
	 * Integrate the served_orgs from peer_ID at date adding_date
	 * after deleting all old served ones (sets all old orgs to served = 0)
	 *  and updates the peer date
	 * called from _storeVerified and wireless
	 * 
	 * should be fixed to preserve last_sync_date field !!!!!!
	 * 
	 * @param served_orgs
	 * @param peer_ID
	 * @param adding_date
	 * @throws P2PDDSQLException
	 */
	public static void integratePeerOrgs(D_PeerOrgs[] served_orgs, long peer_ID, String adding_date) throws P2PDDSQLException{
		//boolean DEBUG=true;
		if(DEBUG) System.err.println("D_PeerAddress:integratePeerOrgs: START: ");
		//String adding_date;
		if(served_orgs==null){
			if(DEBUG) out.println("D_PeerAddress:integratePeerOrgs: EXIT null");
			return;
		}
		Application.db.delete(table.peer_org.TNAME,
				new String[]{table.peer_org.peer_ID},
				new String[]{Util.getStringID(peer_ID)}, DEBUG);
//		Application.db.update(table.peer_org.TNAME,
//				new String[]{table.peer_org.served},
//				new String[]{table.peer_org.peer_ID},
//				new String[]{"0", Util.getStringID(peer_ID)}, DEBUG);
		for(int o=0; o<served_orgs.length; o++) {
			String global_organizationID = served_orgs[o].global_organization_ID;
			String org_name = served_orgs[o].org_name;
			if(global_organizationID!=null) {
				//adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
				long organizationID = UpdateMessages.get_organizationID(global_organizationID, org_name, adding_date,null);
				//adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
				//long peers_orgs_ID = get_peers_orgs_ID(peer_ID, organizationID, adding_date);
				Application.db.insert(table.peer_org.TNAME,
						new String[]{table.peer_org.peer_ID, table.peer_org.organization_ID, table.peer_org.served, table.peer_org.last_sync_date},
						new String[]{Util.getStringID(peer_ID), Util.getStringID(organizationID), "1", served_orgs[o].last_sync_date}, DEBUG);
			}
		}
//		Application.db.delete(table.peer_org.TNAME,
//				new String[]{table.peer_org.served,
//				table.peer_org.peer_ID},
//				new String[]{"0", Util.getStringID(peer_ID)}, DEBUG);
		Application.db.update(table.peer.TNAME,
				new String[]{table.peer.arrival_date},
				new String[]{table.peer.peer_ID},
				new String[]{adding_date, Util.getStringID(peer_ID)}, DEBUG);
		if(DEBUG) out.println("D_PeerAddress:integratePeerOrgs: DONE #:"+served_orgs.length);
	}
	/**
	 * Private,  setting this org as served, or inserting it as served
	 *  and update the arrival-time of peer
	 *  never use
	 * @param peer_ID
	 * @param organizationID
	 * @param adding_date
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long get_peers_orgs_ID(long peer_ID, long organizationID, String adding_date) throws P2PDDSQLException {
		long result=0;
		if(DEBUG) System.err.println("\n************\nD_PeerAddress:get_peers_orgs_ID':  start peer_ID = "+peer_ID+" oID="+organizationID);		
		if((peer_ID <= 0) || (organizationID<=0))  return -1;
		String sql = "SELECT "+table.peer_org.peer_org_ID + "," + table.peer_org.served +
						" FROM "+table.peer_org.TNAME+" AS po " +
						" WHERE po."+table.peer_org.peer_ID+" = ? and po."+table.peer_org.organization_ID+" = ?";
		ArrayList<ArrayList<Object>> dt;
		dt=Application.db.select(sql, new String[]{Util.getStringID(peer_ID), Util.getStringID(organizationID)});
		if((dt.size()>=1) && (dt.get(0).size()>=1)) {
			String s_result = Util.getString(dt.get(0).get(0));
			result = Long.parseLong(s_result);
			if(!"1".equals(Util.getString(dt.get(0).get(1)))) {
					Application.db.update(table.peer_org.TNAME,
							new String[]{table.peer_org.served},
							new String[]{table.peer_org.peer_org_ID},
							new String[]{"1", s_result});
				
					Application.db.update(table.peer.TNAME,
							new String[]{table.peer.arrival_date},
							new String[]{table.peer.peer_ID},
							new String[]{adding_date, Util.getStringID(peer_ID)});
			}
		} else {
			result=Application.db.insert(table.peer_org.TNAME,
				new String[]{table.peer_org.peer_ID, table.peer_org.organization_ID, table.peer_org.served},
				new String[]{Util.getStringID(peer_ID), Util.getStringID(organizationID), "1"});
			Application.db.update(table.peer.TNAME,
				new String[]{table.peer.arrival_date},
				new String[]{table.peer.peer_ID},
				new String[]{adding_date, Util.getStringID(peer_ID)});
		}
		if(DEBUG) System.out.println("D_PeerAddress:get_peers_orgs_ID':  exit result = "+result);		
		if(DEBUG) System.out.println("****************");		
		return result;
	}

	/**
	 * Tries to get the ID, and inserts if the tuple is empty
	 * @param address
	 * @param type
	 * @param peer_ID
	 * @param adding_date
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long get_peer_addresses_ID(String address, String type, long peer_ID, String adding_date) throws P2PDDSQLException{
		if(DEBUG) err.println("UpdateMessages:get_peer_addresses_ID for: "+Util.trimmed(address)+" id="+peer_ID);
		long result=0;
		if((type==null)||(address==null)){
			if(DEBUG) err.println("UpdateMessages:get_peer_addresses_ID null for: "+Util.trimmed(address)+" id="+peer_ID);
			return -1;
		}
		if("null".equals(type)||"null".equals(address)){
			if(DEBUG) err.println("UpdateMessages:get_peer_addresses_ID null for: "+Util.trimmed(address)+" id="+peer_ID);
			return -1;
		}
		//Util.printCallPath("peer");
		String sql = 
			"SELECT "+table.peer_address.peer_address_ID+
			" FROM "+table.peer_address.TNAME+
			" WHERE "+table.peer_address.peer_ID+" = ? and "+table.peer_address.address+" = ? AND "+table.peer_address.type+" = ?;";
		ArrayList<ArrayList<Object>> dt=
			Application.db.select(sql, new String[]{Util.getStringID(peer_ID), address, type}, DEBUG);
		if((dt.size()>=1) && (dt.get(0).size()>=1)) 
			return Long.parseLong(Util.getString(dt.get(0).get(0)));
		result=Application.db.insert(table.peer_address.TNAME,
				new String[]{table.peer_address.address,table.peer_address.type,table.peer_address.peer_ID,table.peer_address.arrival_date},
				new String[]{address, type, Util.getStringID(peer_ID), adding_date}, 
				DEBUG);
		Application.db.update(table.peer.TNAME,
				new String[]{table.peer.arrival_date},
				new String[]{table.peer.peer_ID},
				new String[]{adding_date, Util.getStringID(peer_ID)});
		return result;		
	}

	/**
	 * Create and set a new peerID, and its keys in "key" table and "application" table
	 * @throws P2PDDSQLException
	 */
	public static void createMyPeerID() throws P2PDDSQLException{
		if(DEBUG) out.println("\n*********\nDD:createMyPeerID: start");
		String name = Identity.current_peer_ID.name;
		String slogan = Identity.current_peer_ID.slogan;
		String addresses = Identity.current_server_addresses();
		if(name==null) name = System.getProperty("user.name", _("MySelf"));
		if(addresses==null) addresses = _("LocalMachine");
		ciphersuits.Cipher keys = Util.getKeyedGlobalID("peer", name+"+"+addresses);
		keys.genKey(1024);
		if(DEBUG) out.println("DD:createMyPeerID: keys generated");
		String secret_key = Util.getKeyedIDSK(keys);
		if(DEBUG) out.println("DD:createMyPeerID: secret_key="+secret_key);
		byte[] pIDb = Util.getKeyedIDPKBytes(keys);
		String pID=Util.getKeyedIDPK(pIDb);
		if(DEBUG) out.println("DD:createMyPeerID: public_key="+pID);
		String pGIDhash = Util.getGIDhash(pID);
		String pGIDname = "PEER:"+Util.getGeneralizedTime();
		
		DD.storeSK(keys, pGIDname, pID, secret_key, pGIDhash);
				
		doSetMyself(pID, secret_key, name, slogan, pGIDhash); //sets myself
		
		if(DEBUG) out.println("DD:createMyPeerID: exit");
		if(DEBUG) out.println("**************");
	}
	private static void doSetMyself(String gID, String secret_key, String name, String slogan, String gIDhash) throws P2PDDSQLException{
		if(secret_key == null) {
			Util.printCallPath("GID="+gID+"\n; peerID="+secret_key+" name="+name+" hash="+gIDhash);
			Application.warning(_("We do not know the secret key of this peer!!"), _("Peer Secret Key not available!"));
			return;
		}
		DD.setMyPeerGID(gID, gIDhash);
		DD.setAppText(DD.APP_my_peer_name, name);
		DD.setAppText(DD.APP_my_peer_slogan, slogan);
		Identity.current_peer_ID.name = name;
		Identity.current_peer_ID.slogan = slogan;
		if(DD.WARN_ON_IDENTITY_CHANGED_DETECTION) {
			if(name!=null) Application.warning(_("Now you are:")+" \""+name+"\"", _("Peer Changed!"));
			else{ Application.warning(_("Now have an anonymous identity. You have to choose a name!"), _("Peer Changed!"));			
			}
		}
		//Application.warning(_("Now you are: \""+name+"\"\n with key:"+Util.trimmed(secret_key)), _("Peer Changed!"));
		Server.set_my_peer_ID_TCP(Identity.current_peer_ID); // tell directories and save crt address, setting myself
	}

	/**
	 * 
	 * @param peerID
	 * @throws P2PDDSQLException 
	 */
	public static void setMyself(String peerID) throws P2PDDSQLException {
		String sql = "SELECT p."+table.peer.global_peer_ID+", k."+table.key.secret_key+
		", p."+table.peer.name+", p."+table.peer.slogan+", p."+table.peer.global_peer_ID_hash+
		" FROM "+table.peer.TNAME+" AS p "+
		" LEFT JOIN "+table.key.TNAME+" AS k ON (k."+table.key.public_key+"=p."+table.peer.global_peer_ID+") "+
		" WHERE "+table.peer.peer_ID+" = ?;";
		ArrayList<ArrayList<Object>> a;
		a = Application.db.select(sql, new String[]{peerID}, DEBUG);
		String secret_key = null;
		String gID;
		
		if((a.size() > 0) || (a.get(0).get(1)!=null)) {
			gID = Util.getString(a.get(0).get(0));
			secret_key = Util.getString(a.get(0).get(1));
			String name = Util.getString(a.get(0).get(2));
			String slogan = Util.getString(a.get(0).get(3));
			String gIDhash = Util.getString(a.get(0).get(4));
			doSetMyself(gID, secret_key, name, slogan, gIDhash);
		}else{
			Util.printCallPath(sql+"; peerID="+peerID);
			Application.warning(_("We do not know the secret key of this peer!"), _("Peer Secret Key not available!"));
		}
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
	 * @throws P2PDDSQLException
	 */
	public static int isGIDavailable(String gID, boolean DBG) throws P2PDDSQLException {
		String sql = 
			"SELECT "+table.peer.peer_ID+","+table.peer.signature+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID_hash+"=? ;";
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
	
	public static void main(String[] args) {
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			if(args.length>0){readSignSave(1,1); if(true) return;}
			
			D_PeerAddress c=new D_PeerAddress("1", true);
			if(!c.verifySignature()) System.out.println("\n************Signature Failure\n**********\nread="+c);
			else System.out.println("\n************Signature Pass\n**********\nread="+c);
			Decoder dec = new Decoder(c.getEncoder().getBytes());
			D_PeerAddress d = new D_PeerAddress().decode(dec);
			//Calendar arrival_date = d.arrival_date=Util.CalendargetInstance();
			//if(d.global_organization_ID==null) d.global_organization_ID = OrgHandling.getGlobalOrgID(d.organization_ID);
			if(!d.verifySignature()) System.out.println("\n************Signature Failure\n**********\nrec="+d);
			else System.out.println("\n************Signature Pass\n**********\nrec="+d);
			//d.storeVerified(Encoder.getGeneralizedTime(arrival_date));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static String queryName(Component win) {
		String peer_Name = Identity.current_peer_ID.name;
		String val=
			JOptionPane.showInputDialog(win,
					(peer_Name != null)?
							(_("Change Peer Name.")+"\n"+_("Previously:")+" "+peer_Name):
							(_("Dear:")+" "+System.getProperty("user.name")+"\n"+_("Select a Peer Name recognized by your peers, such as: \"John Smith\"")),
					_("Peer Name"),
					JOptionPane.QUESTION_MESSAGE);
		return val;
	}
	public static void changeMyPeerName(Component win) throws P2PDDSQLException {
		if(DEBUG)System.out.println("peer_ID: "+Identity.current_peer_ID.globalID);
		String val = queryName(win);
		if((val!=null)&&(!"".equals(val))){
			Identity.current_peer_ID.name = val;
			DD.setAppText(DD.APP_my_peer_name, val);

			if(Identity.current_peer_ID.globalID == null) {
				JOptionPane.showMessageDialog(win,
					_("You are not yet a peer.\n Start your server first!"),
					_("Peer Init"), JOptionPane.WARNING_MESSAGE);
				return;
			}
			
			Server.update_my_peer_ID_peers_name_slogan();
			D_PeerAddress.re_init_myself();
			DD.touchClient();
		}else{
			if(DEBUG)System.out.println("peer_ID: Empty Val");
		}
	}

	public static void changeMyPeerSlogan(Component win) throws P2PDDSQLException {
		if(Identity.current_peer_ID.globalID==null){
			JOptionPane.showMessageDialog(win,
					_("You are not yet a peer.\n Start your server first!"),
					_("Peer Init"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		if(DEBUG)System.out.println("peer_ID: "+Identity.current_peer_ID.globalID);
		String peer_Slogan = Identity.current_peer_ID.slogan;
		String val=JOptionPane.showInputDialog(win, _("Change Peer Slogan.\nPreviously: ")+peer_Slogan, _("Peer Slogan"), JOptionPane.QUESTION_MESSAGE);
		if((val!=null)&&(!"".equals(val))){
			Identity.current_peer_ID.slogan = val;
			DD.setAppText(DD.APP_my_peer_slogan, val);
			Server.update_my_peer_ID_peers_name_slogan();
			D_PeerAddress.re_init_myself();
			DD.touchClient();
		}
	}
	/**
	 * Updating my name and slogan based on my global peer ID
	 * new values are taken from globals
	 */
	public static void update_my_peer_ID_peers_name_slogan_broadcastable(boolean broadcastable){
		//boolean DEBUG = true;
		if(DEBUG) out.println("BEGIN Server.update_my_peer_ID_peers_name_slogan_broadcastable");
		if(Identity.current_peer_ID.globalID==null){
			if(DEBUG)out.println("END Server.update_my_peer_ID_peers_name_slogan_broadcastable: Null ID");
			return;
		}
		try {
			String global_peer_ID = Identity.current_peer_ID.globalID;
			D_PeerAddress peer_data = D_PeerAddress.get_myself(global_peer_ID);
			
			// now will update potentially modified fields from globals
			peer_data.globalID = global_peer_ID;
			peer_data.name = Identity.current_peer_ID.name;
			peer_data.slogan = Identity.current_peer_ID.slogan;
			Identity.current_identity_creation_date = peer_data.creation_date = Util.CalendargetInstance();
			peer_data.signature_alg = SR.HASH_ALG_V1;
			peer_data.broadcastable = broadcastable;
			peer_data.served_orgs = data.D_PeerAddress._getPeerOrgs(peer_data.globalID);
			peer_data.picture = Util.byteSignatureFromString(Identity.getMyCurrentPictureStr());
			if(DEBUG) out.println("Server.update_my_peer_ID_peers_name_slogan_broadcastable: sign: "+peer_data);
			byte[] signature = peer_data.sign(DD.getMyPeerSK());//Util.sign_peer(pa);
			if(DEBUG){
				//out.println("Server.update_my_peer_ID_peers_name_slogan_broadcastable "+peer_data);
				peer_data.signature = signature;
				if(!peer_data.verifySignature()){
					if(DEBUG) out.println("Server:update_my_peer_ID_peers_name_slogan_broadcastable: signature verification failed at creation: "+peer_data);
				}else{
					if(DEBUG) out.println("Server:update_my_peer_ID_peers_name_slogan_broadcastable: signature verification passed at creation: "+peer_data);			
				}
			}
			peer_data.storeVerified();
			
			//D_PeerAddress.re_init_myself();
		} catch (P2PDDSQLException e) {
			Application.warning(_("Failed updating peer!"), _("Updating Peer"));
		}
		if(DEBUG) out.println("END Server.update_my_peer_ID_peers_name_slogan_broadcastable ");
	}
	/**
	 * Called when a server starts
	 * @param id
	 * @throws P2PDDSQLException
	 */
	public static long _set_my_peer_ID (Identity id) throws P2PDDSQLException {		
		String addresses = Identity.current_server_addresses();
		Calendar _creation_date=Util.CalendargetInstance();
		String creation_date=Encoder.getGeneralizedTime(_creation_date);
		long pID = -1;
		
		if(addresses!=null) {
			String address[] = addresses.split(Pattern.quote(DirectoryServer.ADDR_SEP));
			for(int k=0; k<address.length; k++)
				if(k==0){
					pID = update_insert_peer_myself(id, address[k], Address.SOCKET, _creation_date, creation_date);
					if(pID<=0) throw new RuntimeException("Failure to insert myself");
				}
				else insert_peer_address_if_new(Util.getStringID(pID), address[k], Address.SOCKET, creation_date);
		}
		
		for(String dir : Identity.listing_directories_string ) {
			if(Server.DEBUG) out.println("server: announce to: "+dir);
			String address_dir=dir;//.getHostName()+":"+dir.getPort();
			if(pID==-1){
				pID = update_insert_peer_myself(id, address_dir, Address.DIR, _creation_date, creation_date);
				if(pID<=0) throw new RuntimeException("Failure to insert my directories");
			}
			else insert_peer_address_if_new(Util.getStringID(pID), address_dir, Address.DIR, creation_date);
		}
		re_init_myself();
		if(Server.DEBUG) out.println("server: setID Done!");
		return pID;
	}

	/**
	 * Either insert or update myself as peer in the peers table and in peer_addressess
	 * Called from _set_my_peer_ID when starting a TCP or UDP server
	 * @param id
	 * @param address
	 * @param type
	 * @throws P2PDDSQLException
	 */
	public static long update_insert_peer_myself(Identity id, String address, String type, Calendar _creation_date, String creation_date) throws P2PDDSQLException{
		if(Server.DEBUG) out.println("Server:update_insert_peer_myself: id="+id+" address="+address+" type="+type);
		long pID;
		D_PeerAddress peer_data = D_PeerAddress.get_myself(id.globalID);
		//if ((myself==null) || (!id.globalID.equals(myself.globalID)))
		//	peer_data = new D_PeerAddress(id.globalID, 0, false, false, true);
		pID = peer_data._peer_ID;
	
		
		if(peer_data.peer_ID == null) { // I did not exist (with this globID)
			if(Server.DEBUG) out.println("Server:update_insert_peer_myself: required new peer");
			
			peer_data.globalID = id.globalID; //Identity.current_peer_ID.globalID;
			peer_data.name = id.name; //Identity.current_peer_ID.name;
			peer_data.slogan = id.slogan; //Identity.current_peer_ID.slogan;
			peer_data.creation_date = _creation_date;//Util.CalendargetInstance();
			peer_data.signature_alg = SR.HASH_ALG_V1;
			peer_data.broadcastable = DD.DEFAULT_BROADCASTABLE_PEER_MYSELF;//Identity.getAmIBroadcastable();
			String picture = Identity.getMyCurrentPictureStr();
			peer_data.picture = Util.byteSignatureFromString(picture);
			byte[] signature = peer_data.sign(DD.getMyPeerSK());//Util.sign_peer(pa);
			if(Server.DEBUG) {
				if(!peer_data.verifySignature()) {
					peer_data.signature = signature;
					if(Server.DEBUG) out.println("Server:update_insert_peer_myself: signature verification failed at creation: "+peer_data);
				}else{
					if(Server.DEBUG) out.println("Server:update_insert_peer_myself: signature verification passed at creation: "+peer_data);			
				}
			}
			
			String IDhash = getGIDHashFromGID(id.globalID);
			peer_data.globalIDhash = IDhash;
			if(Server.DEBUG) out.println("Server:update_insert_peer_myself: will insert new peer");
			pID=peer_data._storeVerified();
			Identity.current_identity_creation_date = peer_data.creation_date;
			Identity.peer_ID = Util.getStringID(pID);
			if(Server.DEBUG) out.println("Server:update_insert_peer_myself: inserted"+pID);
			pID= Application.db.insert(table.peer_address.TNAME,
					new String[]{table.peer_address.address, table.peer_address.type,
					table.peer_address.peer_ID, table.peer_address.arrival_date},
					new String[]{address, type, Util.getStringID(pID), creation_date},
					Server.DEBUG);
		}else{
			String my_peer_ID = peer_data.peer_ID;
			if(Server.DEBUG) out.println("old val:"+my_peer_ID);
			insert_peer_address_if_new(my_peer_ID, address, type, creation_date);
		}
		if(Server.DEBUG) out.println("Server:update_insert_peer_myself: done");
		return pID;
	}

	/**
	 * insert my address in peer_addressess if new
	 * @param _peer_ID
	 * @param address
	 * @param type
	 * @param creation_date
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long insert_peer_address_if_new(String _peer_ID, String address, String type, String creation_date) throws P2PDDSQLException{
		long result=-1;
		ArrayList<ArrayList<Object>> op = Application.db.select("SELECT "+table.peer_address.peer_address_ID+" FROM "+table.peer_address.TNAME+" WHERE "+table.peer_address.peer_ID+" = ? AND "+table.peer_address.address+"=?;",
				new String[]{_peer_ID+"", address});
		if(Server.DEBUG) out.println("Server:update_insert_peer_address_myself:db addr results="+op.size());
		if (op.size()==0) {
			result = Application.db.insert(table.peer_address.TNAME,
					new String[]{table.peer_address.address, table.peer_address.type, table.peer_address.peer_ID, table.peer_address.arrival_date},
					new String[]{address, type, _peer_ID, creation_date} , Server.DEBUG);
		}
		else result = Util.lval(op.get(0).get(0), -1);
		return result;
	}

	/**
	 * Should use "myself"
	 * @return
	 * @throws P2PDDSQLException 
	 */
	public static DDAddress getMyDDAddress() throws P2PDDSQLException {
		DDAddress myAddress;
		myAddress = new DDAddress(D_PeerAddress.get_myself(Identity.current_peer_ID.globalID));
		return myAddress;
	}
		/*
		myAddress.globalID = Identity.current_peer_ID.globalID;
		myAddress.name = Identity.current_peer_ID.name;
		myAddress.slogan = Identity.current_peer_ID.slogan;
		myAddress.creation_date = Util.getGeneralizedTime();
		myAddress.address = "";
		myAddress.broadcastable = Identity.getAmIBroadcastable();
		myAddress.served_orgs = table.peer_org.getPeerOrgs(myAddress.globalID);
		if(Identity.listing_directories_string.size()>0) {
			String isa = Server.DIR+":"+Identity.listing_directories_string.get(0);
			for(int k=1; k<Identity.listing_directories_string.size(); k++) {
				isa += DirectoryServer.ADDR_SEP+Server.DIR+":"+Identity.listing_directories_string.get(k);
			}
			myAddress.address = isa;
			if(DEBUG) System.out.println("actionExport: Directories: "+isa);
		}
		String crt_adr=Identity.current_server_addresses();
		if(DEBUG) System.out.println("actionExport: Sockets: "+crt_adr);
		if(crt_adr!=null) {
			String[] server_addr = crt_adr.split(Pattern.quote(DirectoryServer.ADDR_SEP));
			if("".equals(myAddress.address)) myAddress.address = Server.SOCKET+":"+server_addr[0];
			else myAddress.address += DirectoryServer.ADDR_SEP+Server.SOCKET+":"+server_addr[0];
			
			for(int k=1; k<server_addr.length; k++)
				myAddress.address += DirectoryServer.ADDR_SEP+Server.SOCKET+":"+server_addr[k];
		}
		if(DEBUG) System.out.println("actionExport: Adresses: "+myAddress.address);
		
		myAddress.hash_alg = SR.HASH_ALG_V1;
		SK my_sk = DD.getMyPeerSK();
		if(my_sk == null) Application.warning(_("Address is not siged. Secret Key missing!"), _("No Secret Key!"));
		myAddress.sign(my_sk);
		*/

	public static String getAddressID(InetSocketAddress saddr, String peer_ID) throws P2PDDSQLException {
		//String sname = saddr.toString();
		String ip = Util.get_IP_from_SocketAddress(saddr);
		ArrayList<ArrayList<Object>> a = Application.db.select(
				"SELECT "+table.peer_address.peer_address_ID+
				" FROM "+table.peer_address.TNAME+
				" WHERE "+table.peer_address.peer_ID+"=? AND "+table.peer_address.address+" LIKE '"+peer_ID+"%';", new String[]{});
		if(a.size()==0) return null;
		return Util.getString(a.get(0).get(0));
	}

	public static String getLastResetDate(String _peerID) throws P2PDDSQLException {
		ArrayList<ArrayList<Object>> a = Application.db.select("SELECT "+table.peer.last_reset+" FROM "+table.peer.TNAME+" WHERE "+table.peer.peer_ID+"=?;", new String[]{_peerID});
		if(a.size() == 0) return null;
		return Util.getString(a.get(0).get(0));
	}

	public static void reset(String peerID) {
    	try {
    		String now = Util.getGeneralizedTime();
			Application.db.update(table.peer.TNAME, new String[]{table.peer.last_reset, table.peer.last_sync_date}, new String[]{table.peer.peer_ID},
					new String[]{now, null, peerID}, DEBUG);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
	}
	/**
	 * Does this serve this orgID?
	 * only implement if it can be done efficiently
	 * @param orgID
	 * @return
	 */
	public boolean servesOrgEntryExists(long orgID) {
		if(orgID<=0) return false;
		if(this.served_orgs==null) return false;
		for(int k=0; k < this.served_orgs.length; k++) {
			if(this.served_orgs[k].organization_ID == orgID) return true;
		}
		return false;
	}
}
