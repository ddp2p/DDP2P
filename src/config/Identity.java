/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
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
 package config;
import hds.Address;
import hds.DirectoryServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Calendar;

import streaming.ConstituentHandling;
import streaming.OrgHandling;
import util.P2PDDSQLException;
import util.Util;


import data.D_Constituent;
import static util.Util._;

public class Identity {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public static final String DEFAULT_PREFERRED_LANG = "en_US:ro_RO";
	public static final String DEFAULT_AUTHORSHIP_LANG = "en:US";
	public static final String DEFAULT_AUTHORSHIP_CHARSET = "latin";
	// Lists with detected local domains for interfaces
	public static ArrayList<InetAddress> my_server_domains = new ArrayList<InetAddress>();
	public static ArrayList<InetAddress> my_server_domains_loopback = new ArrayList<InetAddress>();
	public static int port = -1;		// the port where the peer is listening
	public static int udp_server_port = -1; 
	public static Object default_id_branch; // for Identities tree widget
	public static Object current_id_branch;	// for Identities tree widget
	public String globalID;		// globalID for peer/user (required)
	public String globalOrgID;	// global orgID for user (optional)
	public String name; 		// user/peer name to be displayed (optional)
	public String slogan;		// slogan to be used displayed for identity (optional)
	public String identity_id;
	public String authorship_lang;
	public String authorship_charset;
	private String preferred_lang;
	private String preferred_charset;
	private String secret_credential;
	/* Default user Identity from the identities table */
	private static Identity current_identity = null;
	/* Peer Identity from the DD database registry */
	public static Identity current_peer_ID = null;
	//public static ArrayList<InetSocketAddress> query_directories = new ArrayList<InetSocketAddress>();
	/* The list of directories as InetSocketAddressed */
	public static ArrayList<InetSocketAddress> listing_directories_inet = new ArrayList<InetSocketAddress>();
	/* The list of directories as strings prot:IP:port */
	public static ArrayList<String> listing_directories_string = new ArrayList<String>();
	/* the first directory that was online most recent */
	public static int preferred_directory_idx = 0;
	public static Calendar current_identity_creation_date;// = Util.CalendargetInstance();
	public static String peer_ID;
	
	public static void init_Identity() {
		current_identity = null;
		current_identity = initCurrentIdentity();
		current_peer_ID = null;
		current_peer_ID = initMyCurrentPeerIdentity();
	}

	/**
	 * Return a String with all the addresses of the peer, as available in domains and domains_loopback
	 * @return the list IP:port ...
	 */
	public static String current_server_addresses(){
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("Identity:current_server_addresses port=" + port);
		String result = "";
		if(my_server_domains.size()==0) return null;
		if(DEBUG) System.out.println("Identity:current_server_addresses addresses=#" + my_server_domains.size());
		for(InetAddress domain : my_server_domains) {
			if(DEBUG) System.out.println("Identity:current_server_addresses addresses=" + domain);
			String crt;
			//String s_domain=Identity.domain.toString().split("/")[1];
			//return s_domain+":"+port;
			String domain_HostName = Util.getNonBlockingHostName(domain);
			if(DEBUG) System.out.println("Identity:current_server_addresses hostName="+domain_HostName);
			String domain_HostAddress = domain.getHostAddress();
			if(DEBUG) System.out.println("Identity:current_server_addresses hostAddress="+domain_HostAddress);
			if(domain_HostName.equals(domain_HostAddress))
				crt =  Address.getAddress(domain_HostName, port, udp_server_port, null);
			else crt = Address.joinAddresses(Address.getAddress(domain_HostName,port,udp_server_port,null),
					Address.getAddress(domain_HostAddress,port,udp_server_port,null));
			if(!"".equals(result)) result = Address.joinAddresses(result, crt);
			else result = crt;
			if(DEBUG) System.out.println("Identity:current_server_addresses loop done");
		}
		if(DEBUG) System.out.println("Identity:current_server_addresses addresses=#" + my_server_domains_loopback.size());
		for(InetAddress domain : my_server_domains_loopback) {
			if(DEBUG) System.out.println("current_server_addresses addresses=" + domain);
			String crt;
			String domain_HostName = Util.getNonBlockingHostName(domain);
			String domain_HostAddress = domain.getHostAddress();
			if(domain_HostName.equals(domain_HostAddress))
				crt = Address.getAddress(domain_HostName, port, udp_server_port, null);
			else crt = Address.joinAddresses(Address.getAddress(domain_HostName,port,udp_server_port,null),
					Address.getAddress(domain_HostAddress,port,udp_server_port,null));
			//crt = domain.getHostName()+":"+port+DirectoryServer.ADDR_SEP+domain.getHostAddress()+":"+port;
			if(!"".equals(result)) result = Address.joinAddresses(result, crt);
			else result = crt;
		}
		if(DEBUG) System.out.println("Identity:current_server_addresses addresses=" + result);
		return result;
	}
	/**
	 * Get any local host address, if available, else an empty string
	 * @return
	 */
	public static String get_a_server_domain(){
		if(my_server_domains.size()==0)return "";
		return ((InetAddress)Identity.my_server_domains.get(0)).getHostAddress();
	}
	/**
	 * checks the broadcastable status for the current_peer_ID identity in the peers table
	 * telling whether people should broadcast me further to others. Used also when inserting
	 * @return
	 */
	public static boolean getAmIBroadcastable(){
		if(Identity.current_peer_ID.globalID == null){
			return false;
		}
		ArrayList<ArrayList<Object>> br=null;
		String sql =
			"SELECT "+table.peer.broadcastable+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID+"=?;";
		try {
			br = Application.db.select( sql,
					new String[]{Identity.current_peer_ID.globalID},
					DEBUG);
		} catch (P2PDDSQLException e) {
			Application.warning(_("Database unreachable for broadcastable: ")+e, _("Broadcastable"));
		}
		if((br==null) || (br.size()==0)){
			if(_DEBUG)System.out.println("config.Identity.getAmIBroadcastable: My peer_ID is not in my database: "+Identity.current_peer_ID.globalID);
			//Util.printCallPath("fail peer");
			return false;
		}
		//System.err.println("Broadcastable get val=\""+br.get(0).get(0)+"\"");
		boolean result = ("1".equals(br.get(0).get(0).toString()));
		//System.err.println("getAmIBroadcastable returns "+result);
		return result;
	}
	public static void setAmIBroadcastable(boolean val){
		//try {
			data.D_PeerAddress.update_my_peer_ID_peers_name_slogan_broadcastable(val);
/*
			Application.db.update(table.peer.TNAME, new String[]{table.peer.broadcastable}, 
					new String[]{table.peer.global_peer_ID}, 
					new String[]{val?"1":"0",Identity.current_peer_ID.globalID});
					
		} catch (P2PDDSQLException e) {
			Application.warning(_("Database unreachable for setting broadcastable: ")+e, _("Broadcastable"));
		}
		*/
	}
	/**
	 *  checks whether I should be broadcasted to by peers, even as they are asked to not broadcast
	 * me further 
	 * */
	public static boolean getAmIBroadcastableToMyPeers() {
		boolean result=true;
		try {
			result = !DD.getAppBoolean(DD.APP_hidden_from_my_peers);
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	public static void setAmIBroadcastableToMyPeers(boolean val) {
		boolean result=true;
		try {
			DD.setAppText(DD.APP_hidden_from_my_peers, val?"1":"0");
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Returns current if non-null
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static Identity getCurrentIdentity() throws P2PDDSQLException{
		if(current_identity != null) return current_identity;
		return _getDefaultIdentity();
	}
	public static Identity addIdentity(boolean default_id) throws P2PDDSQLException {
		Identity result = new Identity();
		result.name = _("Default Identity: Double-click to set your name. Right-click to add properties.");
		result.globalOrgID = null;
		result.authorship_lang = Identity.DEFAULT_AUTHORSHIP_LANG;
		result.authorship_charset = Identity.DEFAULT_AUTHORSHIP_CHARSET;
		result.preferred_lang = Identity.DEFAULT_PREFERRED_LANG;
		result.preferred_charset = null;
		result.secret_credential = null;
    	long a = Application.db.insertNoSync(table.identity.TNAME,
    				new String[] {
    					table.identity.default_id,
    					table.identity.profile_name,
    					table.identity.organization_ID,
    					table.identity.constituent_ID,
    					table.identity.preferred_lang,
    					table.identity.preferred_charsets,
    					table.identity.secret_credential
    				},
    				new String[] {
    					default_id?"1":"0",
    					result.name,
    					"-1",
    					"-1",
    					result.preferred_lang,
    					result.preferred_charset,
    					result.secret_credential
    				},
    				DEBUG);
    	result.identity_id = Util.getStringID(a);
    	return result;
	}
	/**
	 * Builds an Identity for the default in the identities table
	 * @return default identity in identities table or null if none exists
	 * @throws P2PDDSQLException
	 */
	private static Identity _getDefaultIdentity() throws P2PDDSQLException{
		if(DEBUG) System.out.println("Identity:getDefaultIdentity");
    	Identity result = new Identity();
    	ArrayList<ArrayList<Object>> id;
    	id=Application.db.select(
    			"SELECT c."+table.constituent.global_constituent_ID+", o."+table.organization.global_organization_ID +
    			", i."+table.identity.identity_ID +", i."+table.identity.authorship_lang +", i."+table.identity.authorship_charset +
    			" FROM "+table.identity.TNAME+" AS i" +
    			" LEFT JOIN "+table.constituent.TNAME+" AS c ON (i."+table.identity.constituent_ID+" == c."+table.constituent.constituent_ID+")" +
    			" LEFT JOIN "+table.organization.TNAME+" AS o ON (o."+table.organization.organization_ID+" == i."+table.identity.organization_ID+")" +
    			" WHERE i."+table.identity.default_id+"==1 LIMIT 1;",
    			new String[]{}, DEBUG);
    	if(id.size()==0) {
    		if(DEBUG) System.err.println("No default identity found!");
    		if(!DD.getAppBoolean(DD.APP_stop_automatic_creation_of_default_identity))
    			return addIdentity(true);
    		return null;
    	}
    	result.globalID = (String) id.get(0).get(0);
    	result.globalOrgID = (String) id.get(0).get(1);
    	result.identity_id = Util.getString(id.get(0).get(2));
		//if(DEBUG) System.out.println("Identity:getDefaultIdentity: id="+Util.getString(id.get(0).get(2)));
		result.authorship_lang = Util.getString(id.get(0).get(3));
    	result.authorship_charset = Util.getString(id.get(0).get(4));
		if(DEBUG) System.out.println("Identity:getDefaultIdentity: result="+result);
    	return result;
    }
	/**
	 * Builds an Identity for the default in the identities table
	 * @return default Identity in identities table, or an empty one if none exists
	 */
    public static Identity initCurrentIdentity() {
		if(DEBUG) System.out.println("Identity:initCurrentIdentity");
    	Identity result;
    	try {
    		result = _getDefaultIdentity();
    		if(result != null) return result;
    	}catch(Exception e){
    		e.printStackTrace();
    	}
		if(DEBUG) System.out.println("Identity:initCurrentIdentity: return empty");
    	return new Identity();
    }
    /**
     * Builds an Identity from the Database application registry
     * @return identity in the registry
     */
    public static Identity initMyCurrentPeerIdentity(){
    	Identity result = new Identity();
    	try {
    		result.globalID = DD.getMyPeerGIDFromIdentity();
    		result.name = DD.getAppText(DD.APP_my_peer_name);
    		result.slogan = DD.getAppText(DD.APP_my_peer_slogan);
    		if(result.globalID != null)
    			Identity.current_identity_creation_date = getCreationDate(result.globalID);
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    	if(DEBUG) System.out.println("Identity.initMyCurrentPeerIndentity END: result="+result);
    	return result;
    }
    private static Calendar getCreationDate(String globalID) {
    	String sql = "SELECT "+table.peer.creation_date+","+table.peer.peer_ID+" FROM "+table.peer.TNAME+" WHERE "+table.peer.global_peer_ID+"=?;";
		ArrayList<ArrayList<Object>> a;
		try {
			a = Application.db.select(sql, new String[]{globalID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		if(a.size()==0) return null;
		Calendar result = Util.getCalendar(Util.getString(a.get(0).get(0)));
		Identity.peer_ID = Util.getString(a.get(0).get(1));
		return result;
	}
	public String toString(){
    	return "Identity: idID="+this.identity_id+" cID="+Util.trimmed(globalID)+" name="+this.name+" slogan="+slogan+" orgID="+this.globalOrgID;
    }
    /**
     * Checks if there are any domains known (potentially to check if I am on the net)
     * @return
     *
	public static boolean hasDomains(){//create_missing_ID() {
		boolean result;
		if(DEBUG) System.out.println("BEGIN create_mission_ID");
	    result = domains.size()!=0;
		if(DEBUG) System.out.println("END create_missing_ID: result="+result);
	    return result;
	}
	*/
	public static String getMyCurrentPictureStr() {
		return null;
	}
	/**
	 * Set org_id as current in identity
	 * @param org_id
	 * @throws P2PDDSQLException
	 */
	public static long setCurrentOrg(long org_id) throws P2PDDSQLException {
	   	if(DEBUG) System.err.println("Identity:setCurrentOrg: org="+org_id);
	   	if(DEBUG) Util.printCallPath("Set current org:"+org_id);
		//String gID = OrgHandling.getGlobalOrgID(org_id+"");
	   	long result = -1;
		if(Identity.current_identity==null) {
		   	if(_DEBUG) System.err.println("Identity:setCurrentOrg: No identity");
			return -1;
		}
		if(Identity.current_identity.identity_id==null){
		   	if(_DEBUG) System.err.println("Identity:setCurrentOrg: No identity ID");
			return -1;
		}
		String constituent_ID = "-1";
		String sql = 
			"SELECT "+table.identity_ids.constituent_ID+","+table.identity_ids.identity_ids_ID+
			" FROM "+table.identity_ids.TNAME+
			" WHERE "+table.identity_ids.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> i = Application.db.select(sql, new String[]{""+org_id}, DEBUG);
		if (i.size() == 0) {
			 constituent_ID = "-1";
		}else{
			constituent_ID = Util.getString(i.get(0).get(0));
			try{
				result=new Integer(constituent_ID).longValue();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		Application.db.update(table.identity.TNAME,
				new String[]{table.identity.organization_ID, table.identity.constituent_ID},
				new String[]{table.identity.identity_ID},
				new String[]{""+org_id, constituent_ID, Identity.current_identity.identity_id},
				DEBUG);
	   	if(DEBUG) System.err.println("Identity:setCurrentOrg: Done");
		return result;
	}
	/**
	 * For each org there is a constituent set as current
	 * @param _constituentID
	 * @param _organizationID
	 * @throws P2PDDSQLException
	 */
	public static void setCurrentConstituentForOrg(long _constituentID,
			long _organizationID) throws P2PDDSQLException {
    	if(DEBUG) System.err.println("Identity:setCurrentConstituent: "+_constituentID+" org="+_organizationID);
		if(Identity.current_identity==null) return;
		if(Identity.current_identity.identity_id==null) return;
		//long old_constituent = setCurrentOrg(_organizationID);
		//if(old_constituent == _constituentID) return;
		
		Application.db.update(table.identity.TNAME,
				new String[]{table.identity.organization_ID, table.identity.constituent_ID},
				new String[]{table.identity.identity_ID},
				new String[]{""+_organizationID, ""+_constituentID, Identity.current_identity.identity_id},
				DEBUG);
		
		if(_constituentID<0) {
			Application.db.delete(table.identity_ids.TNAME,
					new String[]{table.identity_ids.identity_ID,table.identity_ids.organization_ID},
					new String[]{Identity.current_identity.identity_id, ""+_organizationID}, DEBUG);
	    	if(DEBUG) System.err.println("Identity:setCurrentConstituent: cancelling myself Done");
			return;
		}
		
		String sql = 
			"SELECT "+table.identity_ids.identity_ids_ID+","+table.identity_ids.constituent_ID+
			" FROM "+table.identity_ids.TNAME+
			" WHERE "+
			//+"=? AND "+//""+_constituentID,
			table.identity_ids.identity_ID+"=? AND "+
			table.identity_ids.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> i = Application.db.select(sql, new String[]{Identity.current_identity.identity_id, ""+_organizationID}, DEBUG);
		if (i.size() == 0) {
			Application.db.insert(table.identity_ids.TNAME,
					new String[]{table.identity_ids.constituent_ID, table.identity_ids.organization_ID, table.identity_ids.identity_ID},
					new String[]{""+_constituentID, ""+_organizationID, Identity.current_identity.identity_id},
					DEBUG);
		} else {
			long old_constituent = -1;
			try{
				old_constituent=new Integer(Util.getString(i.get(0).get(1))).longValue();
			}catch(Exception e){e.printStackTrace();}
			if(old_constituent == _constituentID) return;
			
			
			String ID = Util.getString(i.get(0).get(0));
			Application.db.update(table.identity_ids.TNAME,
					new String[]{table.identity_ids.constituent_ID, table.identity_ids.organization_ID},
					new String[]{table.identity_ids.identity_ids_ID},
					new String[]{""+_constituentID, ""+_organizationID, ID}, DEBUG);
		}
    	if(DEBUG) System.err.println("Identity:setCurrentConstituent: Done");
	}
	/**
	 * Returns the default constituentID in identity-ids for this org
	 * also checks if the constituent really exists (feature that may be redundant....)
	 * @param _organizationID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long getDefaultConstituentIDForOrg(long _organizationID) throws P2PDDSQLException {
    	if(DEBUG) System.err.println("Identity:getDefaultConstituentIDForOrg: begin");
    	if(DEBUG) Util.printCallPath("Defaul ID for ORG");
		if(Identity.current_identity==null){
	    	if(_DEBUG) System.err.println("Identity:getDefaultConstituentIDForOrg: no current Done");
			return -1;
		}
		if(Identity.current_identity.identity_id==null){
	    	if(DEBUG) System.err.println("Identity:getDefaultConstituentIDForOrg: no current identity ID Done");
			return -1;
		}
		String sql = 
			"SELECT "+table.identity_ids.identity_ids_ID+","+table.identity_ids.constituent_ID+
			" FROM "+table.identity_ids.TNAME+
			" WHERE "+
			//+"=? AND "+//""+_constituentID,
			table.identity_ids.identity_ID+"=? AND "+
			table.identity_ids.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> i = 
			Application.db.select(sql, new String[]{Identity.current_identity.identity_id,  ""+_organizationID}, DEBUG);
		if (i.size() == 0){
	    	if(DEBUG) System.err.println("Identity:getDefaultConstituentIDForOrg: no current identity_ids ID Done");
			return -1;
		}
		long old_constituent = -1;
		try{
			old_constituent=Util.lval(Util.getString(i.get(0).get(1)),-1);
			// The next test may be redundant. Better safe than sorry
			if(D_Constituent.getConstituentGlobalID(""+old_constituent)==null){
		    	if(_DEBUG) System.err.println("Identity:getDefaultConstituentIDForOrg: no current GID for="+old_constituent);
				return -1;
			}
		}catch(Exception e){e.printStackTrace();}
	   	if(DEBUG) System.err.println("Identity:getDefaultConstituentIDForOrg: Done="+old_constituent);
		return old_constituent;
	}
	public static long getDefaultOrgID() throws P2PDDSQLException {
    	if(DEBUG) System.err.println("Identity:getDefaultOrgID: begin");
    	//Util.printCallPath("Default ID for ORG");
		if(Identity.current_identity==null){
	    	if(_DEBUG) System.err.println("Identity:getDefaultOrgID: no current Done");
			return -1;
		}
		if(Identity.current_identity.identity_id==null){
	    	if(_DEBUG) System.err.println("Identity:getDefaultOrgID: no current identity ID Done");
			return -1;
		}
		long id = new Integer(Identity.current_identity.identity_id).longValue();
		String sql = 
			"SELECT "+table.identity.organization_ID+
			" FROM "+table.identity.TNAME+
			" WHERE "+table.identity.identity_ID +"=?;";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{id+""}, DEBUG);
		if(a.size()==0) return -1;
		return Util.lval(Util.getString(a.get(0).get(0)), -1);
	}
	/**
	 * Used for testing, to start the app with a given GID
	 * @param newID
	 */
	public static void setCurrentIdentity(String newID) {
		 current_identity.globalID = newID;
	}
	public static void setCurrentIdentity(Identity newID) {
		 current_identity = newID;
	}
}
