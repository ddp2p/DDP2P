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
 package net.ddp2p.common.config;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Calendar;

import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.hds.DirectoryServer;
import net.ddp2p.common.hds.Server;
import net.ddp2p.common.streaming.ConstituentHandling;
import net.ddp2p.common.streaming.OrgHandling;
import net.ddp2p.common.util.DDP2P_ServiceThread;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import static net.ddp2p.common.util.Util.__;

public class Identity {
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public static final String DEFAULT_PREFERRED_LANG = "en_US:ro_RO";
	public static final String DEFAULT_AUTHORSHIP_LANG = "en:US";
	public static final String DEFAULT_AUTHORSHIP_CHARSET = "latin";
	// Lists with detected local domains for interfaces
	public static ArrayList<InetAddress> my_server_domains = new ArrayList<InetAddress>();
	public static ArrayList<InetAddress> my_server_domains_loopback = new ArrayList<InetAddress>();
	public static int port = -1;		// the port where the peer is listening
	public static int udp_server_port = -1; // the port where the udp is listening
	public static Object default_id_branch; // for Identities tree widget
	public static Object current_id_branch;	// for Identities tree widget
	
	private String constituentGID;		// globalID for peer/user (required)
	private String peerGID;		// globalID for peer/user (required)
	public String peerGIDH;
	public String name; 		// user/peer name to be displayed (optional)
	public String slogan;		// slogan to be used displayed for identity (optional)
	public String peerInstance;
	
	public String organizationGID;	// global orgID for user (optional), used for translations
	public String identity_id;
	public String authorship_lang;
	public String authorship_charset;
	private String preferred_lang;
	private String preferred_charset;
	public int status_references = 0;
	private String secret_credential; // not really used
	
	/* Default user Identity (with constituent and language) from the identities table */
	private static Identity current_identity = null;
	/* Peer Identity from the DD database registry */
	public static Identity current_peer_ID = null;
	//public static ArrayList<InetSocketAddress> query_directories = new ArrayList<InetSocketAddress>();
	/* The list of directories as InetSocketAddressed */
	
	private static boolean listing_directories_loaded = false;
	private static ArrayList<InetSocketAddress> listing_directories_inet = new ArrayList<InetSocketAddress>();
	private static ArrayList<Address> listing_directories_addr = new ArrayList<Address>();
	/* The list of directories as strings prot:IP:port */
	private static ArrayList<String> listing_directories_string = new ArrayList<String>();
	/* the first directory that was online most recent */
	public static int preferred_directory_idx = 0;
	public static Calendar current_identity_creation_date;// = Util.CalendargetInstance();
	public static String peer_ID;
	/**
	 * Only used on the first initialization of the system, to temporary store user input
	 */
	public static String emails;
	
	/**
	 * <pre>
	 * Initialize :
	 * Identity.current_identity=initCurrentIdentity();
	 * which sets it to the default identity in the "identities" table.
	 * and 
	 * Identity.current_peer_ID=initMyCurrentPeerIdentity();
	 * </pre>
	 * By setting myself (when setting peer), this also announces myself to directories.
	 * 
	 * @return
	 */
	public static boolean init_Identity(boolean quit_on_failure, boolean set_peer_myself, boolean announce_dirs) {
		current_identity = null;
		// loading language, constituent and organization
		current_identity = initCurrentConstituentIdentity();
		current_peer_ID = null;
		// loading peer from application
		current_peer_ID = initMyCurrentPeerIdentity_fromDB(new Identity(), quit_on_failure, set_peer_myself, announce_dirs);
		return true;
	}

	/**
	 * Return a String with all the addresses of the peer, as available in domains and domains_loopback
	 * @return the list IP:port ...
	 */
	/*
	public static String current_server_addresses() {
		//boolean DEBUG = true;
		
		// probably we should not set the port artificially... (but should not hurt)
		// It is set similarly in detectDomain()
		int port = Identity.port, udp_server_port = Identity.udp_server_port;
		if (port <= 0 ) port = Server.PORT;
		if (port <= 0 ) udp_server_port = Server.PORT;
		
		if (DEBUG) System.out.println("Identity:current_server_addresses port=" + port);
		String result = "";
		if ((my_server_domains.size() == 0) && (my_server_domains_loopback.size() == 0)) {
			if (DEBUG) System.out.println("Identity:current_server_addresses no server");
			return null;
		}
		if (DEBUG) System.out.println("Identity:current_server_addresses addresses=#" + my_server_domains.size());
		for (InetAddress domain : my_server_domains) {
			if (DEBUG) System.out.println("Identity:current_server_addresses addresses=" + domain);
			String crt;
			//String s_domain=Identity.domain.toString().split("/")[1];
			//return s_domain+":"+port;
			String domain_HostName = Util.getNonBlockingHostName(domain);
			if (DEBUG) System.out.println("Identity:current_server_addresses hostName= \""+domain_HostName+"\"");
			String domain_HostAddress = domain.getHostAddress();
			if (DEBUG) System.out.println("Identity:current_server_addresses hostAddress= \""+domain_HostAddress+"\"");
			if ((domain_HostAddress == null) || ("".equals(domain_HostAddress)) || domain_HostName.equals(domain_HostAddress))
				crt =  Address.getAddress(domain_HostName, port, udp_server_port, null);
			else if ((domain_HostName == null) || ("".equals(domain_HostName)))
					crt =  Address.getAddress(domain_HostAddress, port, udp_server_port, null);
			else
				crt = Address.joinAddresses(Address.getAddress(domain_HostName,port,udp_server_port,null),
					Address.getAddress(domain_HostAddress,port,udp_server_port,null));
			if (!"".equals(result)) result = Address.joinAddresses(result, crt);
			else result = crt;
			if (DEBUG) System.out.println("Identity:current_server_addresses loop done: "+result);
		}
		if(DEBUG) System.out.println("Identity:current_server_addresses addresses=#" + my_server_domains_loopback.size());
		for(InetAddress domain : my_server_domains_loopback) {
			if(DEBUG) System.out.println("Identity:current_server_addresses: loopback addresses=" + domain);
			String crt;
			String domain_HostName = Util.getNonBlockingHostName(domain);
			if (DEBUG) System.out.println("Identity:current_server_addresses: loopback hostName="+domain_HostName);
			String domain_HostAddress = domain.getHostAddress();
			if (DEBUG) System.out.println("Identity:current_server_addresses: loopback hostAddress="+domain_HostAddress);
			if ((domain_HostAddress == null) || ("".equals(domain_HostAddress)) || domain_HostName.equals(domain_HostAddress))
				crt = Address.getAddress(domain_HostName, port, udp_server_port, null);
			else if ((domain_HostName == null) || ("".equals(domain_HostName))) 
				crt = Address.getAddress(domain_HostAddress, port, udp_server_port, null);
			else 
				crt = Address.joinAddresses(Address.getAddress(domain_HostName,port,udp_server_port,null),
					Address.getAddress(domain_HostAddress,port,udp_server_port,null));
			//crt = domain.getHostName()+":"+port+DirectoryServer.ADDR_SEP+domain.getHostAddress()+":"+port;
			if(!"".equals(result)) result = Address.joinAddresses(result, crt);
			else result = crt;
		}
		if(DEBUG) System.out.println("Identity:current_server_addresses addresses=" + result);
		return result;
	}
	*/
	public static ArrayList<Address> current_server_addresses_list() {
		//boolean DEBUG = true;
		
		// probably we should not set the port artificially... (but should not hurt)
		// It is set similarly in detectDomain()
		int port = Identity.port, udp_server_port = Identity.udp_server_port;
		if (port <= 0 ) port = Server.PORT;
		if (port <= 0 ) udp_server_port = Server.PORT;
		
		if (DEBUG) System.out.println("Identity:current_server_addresses port=" + port);
		ArrayList<Address> result = new ArrayList<Address>();
		if ((my_server_domains.size() == 0) && (my_server_domains_loopback.size() == 0)) {
			if (DEBUG) System.out.println("Identity:current_server_addresses no server");
			return null;
		}
		if (DEBUG) System.out.println("Identity:current_server_addresses addresses=#" + my_server_domains.size());
		for (InetAddress domain : my_server_domains) {
			if (DEBUG) System.out.println("Identity:current_server_addresses addresses=" + domain);
			String crt;
			//String s_domain=Identity.domain.toString().split("/")[1];
			//return s_domain+":"+port;
			String domain_HostName = Util.getNonBlockingHostName(domain);
			if (DEBUG) System.out.println("Identity:current_server_addresses hostName= \""+domain_HostName+"\"");
			String domain_HostAddress = domain.getHostAddress();
			if (DEBUG) System.out.println("Identity:current_server_addresses hostAddress= \""+domain_HostAddress+"\"");
			/*
			if ((domain_HostAddress == null) || ("".equals(domain_HostAddress)) || domain_HostName.equals(domain_HostAddress))
				crt =  Address.getAddress(domain_HostName, port, udp_server_port, null);
			else if ((domain_HostName == null) || ("".equals(domain_HostName)))
					crt =  Address.getAddress(domain_HostAddress, port, udp_server_port, null);
			else
				crt = Address.joinAddresses(Address.getAddress(domain_HostName,port,udp_server_port,null),
					Address.getAddress(domain_HostAddress,port,udp_server_port,null));
			if (!"".equals(result)) result = Address.joinAddresses(result, crt);
			else result = crt;
			*/
			Address a;
			if ((domain_HostName != null) && ! "".equals(domain_HostName.trim()))
				result.add(new Address(domain_HostName, port, udp_server_port));
			if ((domain_HostAddress != null) && (! "".equals(domain_HostAddress.trim()))
					&& (
							(domain_HostName == null) 
							|| ("".equals(domain_HostName.trim()))
							|| ! domain_HostName.equals(domain_HostAddress)))
				result.add(new Address(domain_HostAddress, port, udp_server_port));
			
			if (DEBUG) System.out.println("Identity:current_server_addresses loop done: "+result);
		}
		if (DEBUG) System.out.println("Identity:current_server_addresses addresses=#" + my_server_domains_loopback.size());
		for (InetAddress domain : my_server_domains_loopback) {
			if(DEBUG) System.out.println("Identity:current_server_addresses: loopback addresses=" + domain);
			String crt;
			String domain_HostName = Util.getNonBlockingHostName(domain);
			if (DEBUG) System.out.println("Identity:current_server_addresses: loopback hostName="+domain_HostName);
			String domain_HostAddress = domain.getHostAddress();
			if (DEBUG) System.out.println("Identity:current_server_addresses: loopback hostAddress="+domain_HostAddress);

//			if ((domain_HostAddress == null) || ("".equals(domain_HostAddress)) || domain_HostName.equals(domain_HostAddress))
//				crt = Address.getAddress(domain_HostName, port, udp_server_port, null);
//			else if ((domain_HostName == null) || ("".equals(domain_HostName))) 
//				crt = Address.getAddress(domain_HostAddress, port, udp_server_port, null);
//			else 
//				crt = Address.joinAddresses(Address.getAddress(domain_HostName,port,udp_server_port,null),
//					Address.getAddress(domain_HostAddress,port,udp_server_port,null));

			//crt = domain.getHostName()+":"+port+DirectoryServer.ADDR_SEP+domain.getHostAddress()+":"+port;
//			if (! "".equals(result)) result = Address.joinAddresses(result, crt);
//			else result = crt;
			Address a;
			if ((domain_HostName != null) && ! "".equals(domain_HostName.trim()))
				result.add(new Address(domain_HostName, port, udp_server_port));
			if ((domain_HostAddress != null) && (! "".equals(domain_HostAddress.trim()))
					&& (
							(domain_HostName == null) 
							|| ("".equals(domain_HostName.trim()))
							|| ! domain_HostName.equals(domain_HostAddress)))
				result.add(new Address(domain_HostAddress, port, udp_server_port));
		}
		if(DEBUG) System.out.println("Identity:current_server_addresses addresses=" + result);
		return result;
	}
	/**
	 * Get any local host address, if available, else an empty string
	 * @return
	 */
	public static String get_a_server_domain(){
		if (my_server_domains.size() == 0) return "";
		return ((InetAddress)Identity.my_server_domains.get(0)).getHostAddress();
	}
	/**
	 * checks the broadcastable status for the current_peer_ID identity in the peers table
	 * telling whether people should broadcast me further to others. Used also when inserting
	 * @return
	 */
	public static boolean getAmIBroadcastable(){
		if(Identity.current_peer_ID.getPeerGID() == null){
			return false;
		}
		/*
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
			Application_GUI.warning(__("Database unreachable for broadcastable: ")+e, __("Broadcastable"));
		}
		if((br==null) || (br.size()==0)){
			if(_DEBUG)System.out.println("config.Identity.getAmIBroadcastable: My peer_ID is not in my database: "+Identity.current_peer_ID.globalID);
			//Util.printCallPath("fail peer");
			return false;
		}
		//System.err.println("Broadcastable get val=\""+br.get(0).get(0)+"\"");
		boolean result = ("1".equals(br.get(0).get(0).toString()));
		*/
		D_Peer peer = D_Peer.getPeerByGID_or_GIDhash(Identity.current_peer_ID.getPeerGID(), null, true, false, false, null);
		if (peer == null) {
			if(_DEBUG)System.out.println("config.Identity.getAmIBroadcastable: My peer_GID is not in my database: \""+Identity.current_peer_ID.getPeerGID()+"\"");
			Util.printCallPath(""+Identity.current_peer_ID);
			return false;
		}
		boolean result = peer.getBroadcastable();
		//System.err.println("getAmIBroadcastable returns "+result);
		return result;
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
	 * Returns current if non-null (could have constituentGID, orgGID, authorshil languahe and charset)
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static Identity getCurrentConstituentIdentity() throws P2PDDSQLException{
		if (current_identity != null) return current_identity;
		return current_identity = _getDefaultConstituentIdentity();
	}
	/**
	 * Loads from table application, Sets and returns current peer identity (peerGID/peerGIDH/peer name)
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static Identity getCurrentPeerIdentity_QuitOnFailure() {
		if (Identity.current_peer_ID != null) return current_peer_ID;
		return current_peer_ID = Identity.initMyCurrentPeerIdentity_fromDB(true);
	}
	/**
	 * Loads from table application, Sets and returns current peer identity (peerGID/peerGIDH/peer name)
	 * 
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static Identity getCurrentPeerIdentity_NoQuitOnFailure() {
		if (Identity.current_peer_ID != null) return current_peer_ID;
		return current_peer_ID = Identity.initMyCurrentPeerIdentity_fromDB(false);
	}
	public static Identity addConstituentIdentity(boolean default_id) throws P2PDDSQLException {
		Identity result = new Identity();
		result.name = __("Default Identity: Double-click to set your name. Right-click to add properties.");
		result.organizationGID = null;
		result.authorship_lang = Identity.DEFAULT_AUTHORSHIP_LANG;
		result.authorship_charset = Identity.DEFAULT_AUTHORSHIP_CHARSET;
		result.preferred_lang = Identity.DEFAULT_PREFERRED_LANG;
		result.preferred_charset = null;
		result.secret_credential = null;
    	long a = Application.db.insertNoSync(net.ddp2p.common.table.identity.TNAME,
    				new String[] {
    					net.ddp2p.common.table.identity.default_id,
    					net.ddp2p.common.table.identity.profile_name,
    					net.ddp2p.common.table.identity.organization_ID,
    					net.ddp2p.common.table.identity.constituent_ID,
    					net.ddp2p.common.table.identity.preferred_lang,
    					net.ddp2p.common.table.identity.preferred_charsets,
    					net.ddp2p.common.table.identity.secret_credential
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
	/*
	String _sql = "SELECT c."+table.constituent.global_constituent_ID+", o."+table.organization.global_organization_ID +
			", i."+table.identity.identity_ID +", i."+table.identity.authorship_lang +", i."+table.identity.authorship_charset +
			" FROM "+table.identity.TNAME+" AS i" +
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON (i."+table.identity.constituent_ID+" == c."+table.constituent.constituent_ID+")" +
			" LEFT JOIN "+table.organization.TNAME+" AS o ON (o."+table.organization.organization_ID+" == i."+table.identity.organization_ID+")" +
			" WHERE i."+table.identity.default_id+"==1 LIMIT 1;";
	*/
	final static String sql_get_default_identity = "SELECT i."+net.ddp2p.common.table.identity.constituent_ID+", i."+net.ddp2p.common.table.identity.organization_ID
    			+ ", i."+net.ddp2p.common.table.identity.identity_ID +", i."+net.ddp2p.common.table.identity.authorship_lang +", i."+net.ddp2p.common.table.identity.authorship_charset +
    			" FROM "+net.ddp2p.common.table.identity.TNAME+" AS i" +
    			" WHERE i."+net.ddp2p.common.table.identity.default_id+"==1 LIMIT 1;";
	/**
	 * Builds an Identity for the default in the "identities" table containing 
	 * (identity_id: constituentGID, organizationGID, authorship_lang, authorship_charset), where default_id==1.
	 * If no default exists, it creates one if allowed by DD.APP_stop_automatic_creation_of_default_identity
	 * @return default identity in identities table or null if none exists
	 * @throws P2PDDSQLException
	 */
	private static Identity _getDefaultConstituentIdentity() throws P2PDDSQLException{
		if (DEBUG) System.out.println("Identity:getDefaultIdentity");
    	Identity result = new Identity();
    	ArrayList<ArrayList<Object>> id;
    	id = Application.db.select(
    			sql_get_default_identity,
    			new String[]{}, DEBUG);
    	if (id.size() == 0) {
    		if (DEBUG) System.err.println("No default identity found!");
    		if (! DD.getAppBoolean(DD.APP_stop_automatic_creation_of_default_identity))
    			return addConstituentIdentity(true);
    		return null;
    	}
    	
    	String cLID = (String) id.get(0).get(0);
    	result.setConstituentGID(D_Constituent.getGIDFromLID(cLID)); //(String) id.get(0).get(0);
    	String oLID = (String) id.get(0).get(1);
    	result.organizationGID = D_Organization.getGIDbyLIDstr(oLID); //(String) id.get(0).get(1);
    	result.identity_id = Util.getString(id.get(0).get(2));
		//if(DEBUG) System.out.println("Identity:getDefaultIdentity: id="+Util.getString(id.get(0).get(2)));
		result.authorship_lang = Util.getString(id.get(0).get(3));
    	result.authorship_charset = Util.getString(id.get(0).get(4));
		if (DEBUG) System.out.println("Identity:getDefaultIdentity: result="+result);
    	return result;
    }
	/**
	 * Builds an Identity for the default in the "identities" table
	 * @return default Identity in identities table, or an empty one if none exists
	 */
    public static Identity initCurrentConstituentIdentity() {
		if(DEBUG) System.out.println("Identity:initCurrentIdentity");
    	Identity result;
    	try {
    		result = _getDefaultConstituentIdentity();
    		if (result != null) return result;
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
		if (DEBUG) System.out.println("Identity: initCurrentIdentity: return empty");
    	return new Identity();
    }
    /**
     * loads peer identity (from "application" table) into a new identity objects and returns it.
     * @param quit_on_failure
     * @return
     */
     public static Identity initMyCurrentPeerIdentity_fromDB(boolean quit_on_failure) {
    	Identity result = new Identity();
    	return initMyCurrentPeerIdentity_fromDB_setPeerMyself(result, quit_on_failure);
    }
    /**
     * Builds an Identity from the Database application registry
     * @param result
     * @param quit_on_failure
     * @return identity in the registry
     */
     public static Identity initMyCurrentPeerIdentity_fromDB_setPeerMyself(Identity result, boolean quit_on_failure) {
    	 return initMyCurrentPeerIdentity_fromDB(result, quit_on_failure, true, true);
     }
     /**
      * 
      * Builds an Identity from the Database application registry
      * @param result
      * @param quit_on_failure   To call System.exit()
      * @param set_myself  (to create peer myself and announce it to dirs)
      * @param announce dirs  (to announce it to dirs, only if myself was created)
      * @return
      */
     public static Identity initMyCurrentPeerIdentity_fromDB(Identity result, boolean quit_on_failure,
    		 boolean set_myself, boolean announce_dirs) {
    	HandlingMyself_Peer.loadIdentity(result);
    	if (! set_myself) return result;
    	
    	D_Peer me = HandlingMyself_Peer.getPeer(result);
		if (me == null) {
			Identity.current_peer_ID = result;
			//if (DEBUG) Util.printCallPath("");
			new DDP2P_ServiceThread("Create Peer Dialog", true, quit_on_failure?new Object():null) {
				public void _run() {
					D_Peer me = HandlingMyself_Peer.createMyselfPeer_by_dialog_w_Addresses(true);
					if (me == null) {
						if (_DEBUG) System.out.println("StartUpThread: run: fail to create peer!");
						if (ctx != null) { // if an object is passed then we quit on failure
							Util.printCallPath("");
							System.exit(1);
						}
					}
				}
			}.start();
		} else {
			boolean kept;
			boolean saveInDB;
			HandlingMyself_Peer.setMyself(me, saveInDB = false, result, kept = false, announce_dirs);
		}
		//HandlingMyself_Peer.setMyself(me, false);
		//if (me != null) Identity.current_identity_creation_date = me.getCreationTime();
		//else System.out.println("Identity:initMyCurrentPeerIndentity: no peer identity found!");
//    	try {
//    		result.globalID = DD.getAppText(DD.APP_my_global_peer_ID);//MyselfHandling.getMyPeerGIDFromIdentity();
//    		result.globalIDH = DD.getAppText(DD.APP_my_global_peer_ID_hash);
//    		result.name = DD.getAppText(DD.APP_my_peer_name);
//       	result.slogan = DD.getAppText(DD.APP_my_peer_slogan);
//       	result.instance = DD.getAppText(DD.APP_my_peer_instance);
//    		if(result.globalID != null)
//    			Identity.current_identity_creation_date = getCreationDate(result.globalID);
//    	}catch(Exception e){
//    		e.printStackTrace();
//    	}
    	if (DEBUG) System.out.println("Identity.initMyCurrentPeerIndentity END: result="+result);
    	return result;
    }
    /*
    private static Calendar getCreationDate(String globalID) {
    	D_Peer peer = D_Peer.getPeerByGID(globalID, true, false, null);
    	if (peer == null) return null;
    	Identity.peer_ID = peer.get_ID();
    	Calendar result = peer.getCreationTime();
		return result;
	}
    */
	public String toString(){
    	return "Identity: idID="+this.identity_id+" GID="+Util.trimmed(getPeerGID())+" name="+this.name+" slogan="+slogan+" orgID="+this.organizationGID;
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
			"SELECT "+net.ddp2p.common.table.identity_ids.constituent_ID+","+net.ddp2p.common.table.identity_ids.identity_ids_ID+
			" FROM "+net.ddp2p.common.table.identity_ids.TNAME+
			" WHERE "+net.ddp2p.common.table.identity_ids.organization_ID+"=?;";
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
		Application.db.update(net.ddp2p.common.table.identity.TNAME,
				new String[]{net.ddp2p.common.table.identity.organization_ID, net.ddp2p.common.table.identity.constituent_ID},
				new String[]{net.ddp2p.common.table.identity.identity_ID},
				new String[]{""+org_id, constituent_ID, Identity.current_identity.identity_id},
				DEBUG);
	   	if(DEBUG) System.err.println("Identity:setCurrentOrg: Done");
		return result;
	}
	static final String sql_identity_for_org = 
			"SELECT "+net.ddp2p.common.table.identity_ids.identity_ids_ID+","+net.ddp2p.common.table.identity_ids.constituent_ID+
			" FROM "+net.ddp2p.common.table.identity_ids.TNAME+
			" WHERE "+
			//+"=? AND "+//""+_constituentID,
			net.ddp2p.common.table.identity_ids.identity_ID+"=? AND "+
			net.ddp2p.common.table.identity_ids.organization_ID+"=?;";
//	final static String sql_identity_for_org_ID = 
//			"SELECT "+table.identity_ids.identity_ids_ID+","+table.identity_ids.constituent_ID+
//			" FROM "+table.identity_ids.TNAME+
//			" WHERE "+
//			//+"=? AND "+//""+_constituentID,
//			table.identity_ids.identity_ID+"=? AND "+
//			table.identity_ids.organization_ID+"=?;";
	/**
	 * For each org there is a constituent set as current.
	 * Inefficient implementation. Try first updating, 
	 * Then delete empty ones, then check value, 
	 * then insert if absent, then update again.
	 * 
	 * @param _constituentID
	 * @param _organizationID
	 * @throws P2PDDSQLException
	 */
	public static boolean setCurrentConstituentForOrg(long _constituentID,
			long _organizationID) throws P2PDDSQLException {
    	if (DEBUG) System.err.println("Identity:setCurrentConstituent: "+_constituentID+" org="+_organizationID);
    	getCurrentConstituentIdentity(); // to load current Identity in not yet done ...
		if (Identity.current_identity == null) {
		  	if (DEBUG) System.err.println("Identity:setCurrentConstituent: No identity object");
			return false;
		}
		if (Identity.current_identity.identity_id == null) {
		  	if (DEBUG) System.err.println("Identity:setCurrentConstituent: No identity LID");
			return false;
		}
		//long old_constituent = setCurrentOrg(_organizationID);
		//if(old_constituent == _constituentID) return;
		
		/**
		 * Update constituent LID as the new one, if it was already here
		 */
		Application.db.update(net.ddp2p.common.table.identity.TNAME,
				new String[]{net.ddp2p.common.table.identity.organization_ID, net.ddp2p.common.table.identity.constituent_ID},
				new String[]{net.ddp2p.common.table.identity.identity_ID},
				new String[]{""+_organizationID, ""+_constituentID, Identity.current_identity.identity_id},
				DEBUG);
		
		/**
		 * Delete entry for this org if there is no constituent
		 */
		if (_constituentID < 0) {
			Application.db.delete(net.ddp2p.common.table.identity_ids.TNAME,
					new String[]{net.ddp2p.common.table.identity_ids.identity_ID,net.ddp2p.common.table.identity_ids.organization_ID},
					new String[]{Identity.current_identity.identity_id, ""+_organizationID}, DEBUG);
	    	if (DEBUG) System.err.println("Identity:setCurrentConstituent: cancelling myself Done");
			return true;
		}
		
		/**
		 * Check if it is stored
		 */
		ArrayList<ArrayList<Object>> i =
				Application.db.select(sql_identity_for_org, new String[]{Identity.current_identity.identity_id, ""+_organizationID}, DEBUG);
		/**
		 * Insert if not stored
		 */
		if (i.size() == 0) {
			Application.db.insert(net.ddp2p.common.table.identity_ids.TNAME,
					new String[]{net.ddp2p.common.table.identity_ids.constituent_ID, net.ddp2p.common.table.identity_ids.organization_ID, net.ddp2p.common.table.identity_ids.identity_ID},
					new String[]{""+_constituentID, ""+_organizationID, Identity.current_identity.identity_id},
					DEBUG);
		} else {
			long old_constituent = -1;
			try {
				old_constituent = new Integer(Util.getString(i.get(0).get(1))).longValue();
			} catch(Exception e){e.printStackTrace();}
			if (old_constituent == _constituentID) return true;
			
			/**
			 * If still not the right value, try again updating (should not be here)
			 */
			String ID = Util.getString(i.get(0).get(0));
			Application.db.update(net.ddp2p.common.table.identity_ids.TNAME,
					new String[]{net.ddp2p.common.table.identity_ids.constituent_ID, net.ddp2p.common.table.identity_ids.organization_ID},
					new String[]{net.ddp2p.common.table.identity_ids.identity_ids_ID},
					new String[]{""+_constituentID, ""+_organizationID, ID}, DEBUG);
		}
    	if(DEBUG) System.err.println("Identity:setCurrentConstituent: Done");
    	return true;
	}
	/**
	 * Returns the default constituentID in identity-ids for this org
	 * also checks if the constituent really exists (feature that may be redundant....)
	 * @param _organizationID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long getDefaultConstituentIDForOrg(long _organizationID) throws P2PDDSQLException {
    	if (DEBUG) System.err.println("Identity:getDefaultConstituentIDForOrg: begin");
    	if (DEBUG) Util.printCallPath("Defaul ID for ORG");
    	getCurrentConstituentIdentity(); // to load current Identity in not yet done ...
		if (Identity.current_identity == null){
	    	if(_DEBUG) System.err.println("Identity: getDefaultConstituentIDForOrg: no current obj Done");
			return -1;
		}
		if(Identity.current_identity.identity_id==null){
	    	if(DEBUG) System.err.println("Identity:getDefaultConstituentIDForOrg: no current identity ID Done");
			return -1;
		}
		ArrayList<ArrayList<Object>> i = 
			Application.db.select(sql_identity_for_org, new String[]{Identity.current_identity.identity_id,  ""+_organizationID}, DEBUG);
		if (i.size() == 0){
	    	if(DEBUG) System.err.println("Identity:getDefaultConstituentIDForOrg: no current identity_ids ID Done");
			return -1;
		}
		long old_constituent = -1;
		try {
			old_constituent=Util.lval(Util.getString(i.get(0).get(1)),-1);
			// The next test may be redundant. Better safe than sorry
			if (D_Constituent.getGIDFromLID(old_constituent) == null) {
		    	if (_DEBUG) System.err.println("Identity:getDefaultConstituentIDForOrg: no current GID for="+old_constituent);
				return -1;
			}
		} catch(Exception e) {e.printStackTrace();}
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
			"SELECT "+net.ddp2p.common.table.identity.organization_ID+
			" FROM "+net.ddp2p.common.table.identity.TNAME+
			" WHERE "+net.ddp2p.common.table.identity.identity_ID +"=?;";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{id+""}, DEBUG);
		if(a.size()==0) return -1;
		return Util.lval(Util.getString(a.get(0).get(0)), -1);
	}
	/**
	 * Used for testing, to start the app with a given GID
	 * @param newID
	 */
	public static void setCurrentPeerIdentity(String peerGID) {
		 //current_identity.setPeerGID(peerGID); // unclear why setting here?
		 Identity.current_peer_ID.setPeerGID(peerGID);
	}
	public static void setCurrentConstituentIdentity(Identity newID) {
		 current_identity = newID;
	}

	public static String getMyPeerGID() {
		if (current_peer_ID == null) return null;
		return current_peer_ID.getPeerGID();
	}
	public static String getMyPeerInstance() {
		if (current_peer_ID == null) return null;
		return current_peer_ID.peerInstance;
	}

	public static void setAgentWideEmails(String val) {
		emails = val;
	}

	public static String getAgentWideEmails() {
		return emails;
	}
	public static D_Constituent getCrtConstituent(String organization_LID) {
		long oLID = Util.lval(organization_LID, -1);
		return getCrtConstituent(oLID);
	}

	/**
	 * Get constituent for a given organization
	 * @param oLID
	 * @return
	 */
	public static D_Constituent getCrtConstituent(long oLID) {
    	if (oLID <= 0) return null;
//		D_Organization org;
//    	org = D_Organization.getOrgByLID(oLID, true, false);
//    	if (org == null) return null;
    	
    	long constituent_LID = -1;
    	D_Constituent constituent = null;
    	try {
    		Identity crt_identity = Identity.getCurrentConstituentIdentity();
    		if (crt_identity == null) {
    			//Log.d(TAG, "No identity");
    		} else 
    			constituent_LID = net.ddp2p.common.config.Identity.getDefaultConstituentIDForOrg(oLID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
    	
    	if (constituent_LID > 0) {
    		constituent = D_Constituent.getConstByLID(constituent_LID, true, false);
    		//Log.d(TAG, "Got const: "+constituent);
    	}
    	return constituent;
	}

	public String getPeerGID() {
		return peerGID;
	}

	public void setPeerGID(String peerGID) {
		this.peerGID = peerGID;
		//Util.printCallPath("Setting:"+peerGID);
	}

	public String getConstituentGID() {
		return constituentGID;
	}

	public void setConstituentGID(String constituentGID) {
		this.constituentGID = constituentGID;
	}

	public static int countLocalAddresses() {
		return Identity.my_server_domains.size() + Identity.my_server_domains_loopback.size();
	}

	public static ArrayList<String> getListing_directories_string() {
		if (! isListing_directories_loaded()) {
			if (_DEBUG) System.out.println("Identity:getListing_directory_string: why not loaded so far?");
			DD.load_listing_directories_noexception();
		}
		return listing_directories_string;
	}

	public static void setListing_directories_string(
			ArrayList<String> listing_directories_string) {
		Identity.listing_directories_string = listing_directories_string;
	}

	public static ArrayList<Address> getListing_directories_addr() {
		if (! isListing_directories_loaded()) {
			if (_DEBUG) {
				System.out.println("Identity:getListing_directory_addr: why not loaded so far?");
				Util.printCallPath("");
			}
			DD.load_listing_directories_noexception();
		}
		return listing_directories_addr;
	}

	public static void setListing_directories_addr(
			ArrayList<Address> listing_directories_addr) {
		Identity.listing_directories_addr = listing_directories_addr;
	}

	public static ArrayList<InetSocketAddress> getListing_directories_inet() {
		if (! isListing_directories_loaded()) {
			if (_DEBUG) System.out.println("Identity:getListing_directory_inet: why not loaded so far?");
			DD.load_listing_directories_noexception();
		}
		return listing_directories_inet;
	}

	public static void setListing_directories_inet(
			ArrayList<InetSocketAddress> listing_directories_inet) {
		Identity.listing_directories_inet = listing_directories_inet;
	}

	static public boolean isListing_directories_loaded() {
		return listing_directories_loaded;
	}

	static public void setListing_directories_loaded(boolean listing_directories_loaded) {
		Identity.listing_directories_loaded = listing_directories_loaded;
	}

}
