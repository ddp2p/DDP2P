package hds;

import static java.lang.System.out;
import static util.Util.__;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;

import ASN1.Decoder;
import config.Application;
import config.Application_GUI;
import config.DD;
import config.Identity;
import data.D_Peer;
import data.D_PeerInstance;
import util.CommEvent;
import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.DirectoryAddress;
import util.P2PDDSQLException;
import util.Util;

/**
 * Contains SocketAddress_Domain supernode_addr and reported_peer_addr,
 * last_contact, contacted_since_start, last_contact_successful
 * 
 * @author msilaghi
 *
 */
class My_Directory {
	Address_SocketResolved_TCP supernode_addr;
	Address_SocketResolved_TCP reported_my_addr;
	Calendar last_contact;
	String _last_contact;
	boolean contacted_since_start = false;
	boolean last_contact_successful = false;
	public String toString() {
		return "[My_Dir: super="+supernode_addr+" reported="+reported_my_addr+"]";
	}
}
/**
 * HashTable<GID,Peer_Connection> used_peer, ArrayList _used_peera
 * @author M. Silaghi
 */
public class Connections extends util.DDP2P_ServiceThread implements DBListener{
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;

	/**
	 * This peer's addresses as seen by AddressBooks (directories)
	 */
	static public Hashtable<String,Connections_Peer_Directory> personal_HT_IPPORT_PC = new Hashtable<String,Connections_Peer_Directory>();
	/**
	 * The ordered list of contacted peer addresses
	 */
	static public ArrayList<Connection_Peer> used_peers_AL_PC;

	
	/**
	 * The addresses of contacted peers (by GID), accessed in "used_peers_AL_PC"
	 */
	static public Hashtable<String,Connection_Peer> used_peers_HT_GID_PC;//= new Hashtable<String,Connection_Peer>();
	/**
	 * The addresses of contacted peers (by GIDH), accessed in "used_peers_AL_PC"
	 */
	static public Hashtable<String,Connection_Peer> used_peers_HT_GIDH_PC; //= new Hashtable<String,Connection_Peer>();

	static public ArrayList<My_Directory> my_directories_AL;
	static public int peersAvailable = 0;
	
	static private Hashtable<String,Connection_Peer> tmp_used_peers_HT_GID_PC;
	static private Hashtable<String,Connection_Peer> tmp_used_peers_HT_GIDH_PC;
	static private ArrayList<Connection_Peer> tmp_used_peers_AL_PC;
	static private ArrayList<My_Directory> tmp_my_directories_AL;
	private static int tmp_peersAvailable;
	
	/** flags and monitors to update needs, keep wait_obj during lock_update_pc */
	private final static Object wait_obj = new Object();
	private final static Object lock_update_pc = new Object(); // hold during wait_obj
	private final static Object lock_used_structures = new Object();
	private static final Object monitor = new Object();
	
	private static boolean update_dirs;
	private static boolean update_peers;
	/** list of pc that need updating */
	private static ArrayList<Connection_Peer> update_pc = new ArrayList<Connection_Peer>();
	
	public static hds.Connections_Peer_Directory getConnectionPeerDirectory(String domain, int udp_port, String GIDH, String instance) {
		//boolean DEBUG = true;
		if (DEBUG || (GIDH == null)) System.out.println("Connections: getConnectionPeerDirectory: enter GIDH="+GIDH+" i:"+instance+" from="+domain+":"+udp_port);
		Connection_Peer peer = used_peers_HT_GIDH_PC.get(GIDH);
		if (peer != null) {
			
			// instance
			Connection_Instance pi = peer.getInstanceConnection(instance);
			if (pi == null) {
				if (DEBUG) System.out.println("Connections: getConnectionPeerDirectory: unknown instance: "+instance);
				return null;
			}
			for (Connections_Peer_Directory dir : pi.peer_directories) {
				if (DEBUG) System.out.println("Connections: getConnectionPeerDirectory: try: "+dir.supernode_addr.ad.toLongString());
				if (!Util.equalStrings_null_or_not(domain, dir.supernode_addr.ad.domain)) continue;
				if (dir.supernode_addr.ad.udp_port != udp_port) continue;
				return dir;
			}
			
			for (Connections_Peer_Directory dir : peer.shared_peer_directories) {
				if (DEBUG) System.out.println("Connections: getConnectionPeerDirectory: try: "+dir.supernode_addr.ad.toLongString());
				if (!Util.equalStrings_null_or_not(domain, dir.supernode_addr.ad.domain)) continue;
				if (dir.supernode_addr.ad.udp_port != udp_port) continue;
				return dir;
			}
		} else {
			if (DEBUG) System.out.println("Connections: getConnectionPeerDirectory: peer not used: "+GIDH);
		}
		return null;
	}
	
	public static Connection_Instance getConnectionPeerInstance(String GIDH, String instance, Connection_Peer _pc[]) {
		Connection_Peer peer = used_peers_HT_GIDH_PC.get(GIDH);
		if (peer != null) {
			if ((_pc != null) && (_pc.length > 0)) _pc[0] = peer;
			// instance
			Connection_Instance pi = peer.getInstanceConnection(instance);
			return pi;
		}
		return null;
	}
	
	public static Connection_Peer getConnectionPeer(String GIDH) {
		Connection_Peer peer = used_peers_HT_GIDH_PC.get(GIDH);
		return peer;
	}
	
	public static String _toString() {
		return "[Connections: #"+peersAvailable+
				" \npeers=\n "+Util.concat(used_peers_AL_PC, "\n ", "empty")+
				" \ntmp_peers=\n "+Util.concat(tmp_used_peers_AL_PC, "\n ", "empty")+
				" \nmy_dirs=\n "+Util.concat(my_directories_AL, "\n ","empty")+
				" \ntmp_my_dirs=\n "+Util.concat(tmp_my_directories_AL, "\n ","empty")+"]";
	}
	
	public Connections(DBInterface db) {
		super("Connections Manager", true);
		if (DEBUG) System.out.println("Connections: <init>");
		allocate_tmp_structures();
		switch_tmp();
		db.addListener(this,
				new ArrayList<String>(
						Arrays.asList(
								table.peer.TNAME,
								table.peer_address.TNAME,
								table.directory_address.TNAME
								//table.application.TNAME
								)),
								null
								/*
								DBSelector.getHashTable(
										table.application.TNAME,
										table.application.field,
										DD.APP_LISTING_DIRECTORIES)
										*/
				);
		start();
		if (DEBUG) System.out.println("Connections: <init> done");
	}
	/**
	 * synchronized used_structures
	 * @param crt
	 * @return
	 */
	public static Connection_Peer getConnectionAtIdx(int crt) {
		Connection_Peer result = null;
		if(DEBUG) System.out.println("Connections: getConnectionAtIdx: "+crt+"/"+peersAvailable);
		synchronized(lock_used_structures) {
			if(crt < 0) result = null;
			else if(crt >= peersAvailable) result = null;
				else result = used_peers_AL_PC.get(crt);
		}
		if(DEBUG) System.out.println("Connections: getConnectionAtIdx: result="+result);
		return result;
	}
	/** Allocates: tmp_used_peers_HT_GID_PC, tmp_used_peers_AL_PC */
	private static void allocate_peers() {
		tmp_used_peers_HT_GID_PC = new Hashtable<String,Connection_Peer>();
		tmp_used_peers_HT_GIDH_PC = new Hashtable<String,Connection_Peer>();
		tmp_used_peers_AL_PC = new ArrayList<Connection_Peer>();
	}
	/** Allocates: tmp_my_directories_AL */
	private static void allocate_dirs() {
		tmp_my_directories_AL = new ArrayList<My_Directory>();
	}
	/**
	 * allocate new empty 3 temporary structures (tmp_used_peers_HT_GID_PC, ...). Only called from this thread
	 */
	private static void allocate_tmp_structures(){
		allocate_dirs(); allocate_peers();
	}
	
	private static void switch_tmp_peers() {
		if (DEBUG) System.out.println("Connections: switch_tmp_peers: start");
		synchronized(lock_used_structures) {
			used_peers_HT_GID_PC = tmp_used_peers_HT_GID_PC;
			used_peers_HT_GIDH_PC = tmp_used_peers_HT_GIDH_PC;
			if (tmp_used_peers_HT_GIDH_PC == null) Util.printCallPath("");
			used_peers_AL_PC = tmp_used_peers_AL_PC;
			peersAvailable = used_peers_AL_PC.size();
		}
		
		tmp_used_peers_HT_GID_PC = null;
		tmp_used_peers_HT_GIDH_PC = null;
		tmp_used_peers_AL_PC = null;
	}
	
	private static void switch_tmp_dirs() {
		my_directories_AL = tmp_my_directories_AL;
		tmp_my_directories_AL = null;
	} 
	/** moves tmp_structures to used ones, sets tmp_structs to null */
	private static void switch_tmp() {
		if (DEBUG) System.out.println("Connections: switch_tmp: start");
		synchronized(lock_used_structures) {
			switch_tmp_peers(); switch_tmp_dirs();
		}
	}
	/**
	 * Creates peers in tmp_peers.
	 * Loads them by in tmp_used_peers_AL_PC, tmp_used_peers_HT_GID_PC, tmp_peersAvailable
	 * Should compute last_sync_date from instances.
	 */
	private static void init_used_peers() {
		if (DEBUG) System.out.println("Connections: init_peers");
		/*
		int QUERY_ID = 0;
		String peers_scan_sql =
				"SELECT "+table.peer.peer_ID+
				" FROM "+table.peer.TNAME+
				" WHERE "+table.peer.used+" = 1 " +
						" ORDER BY "+table.peer.last_sync_date+";";
		ArrayList<ArrayList<Object>> peers;
		try {
			peers = Application.db.select(peers_scan_sql, new String[]{});
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
			return;
		}
		*/
		ArrayList<D_Peer> peers = D_Peer.getUsedPeers();
		tmp_peersAvailable = peers.size();
		if (DEBUG) System.out.println("Connections: init_peers: peers #="+tmp_peersAvailable);
		for (int k = 0; k < tmp_peersAvailable; k++) {
			Connection_Peer p = new Connection_Peer();
			long ID = peers.get(k).getLID_keep_force(); //Util.lval(peers.get(k).get(QUERY_ID), -1);
			if (DEBUG) System.out.println("Connections: init_peers: peer #="+ID);
			loadAddresses_to_PeerConnection(p, ID);
			tmp_used_peers_AL_PC.add(p);
			tmp_used_peers_HT_GID_PC.put(p.getGID(), p);
			tmp_used_peers_HT_GIDH_PC.put(p.getGIDH(), p);
		}
		if (DEBUG) System.out.println("Connections: init_peers: got="+_toString());
	}
	/**
	 * Loads the addresses of Peer_Connection p in tmp_peers.
	 * @param p
	 * @param peer_ID : In the future should us GIDH to support clouds
	 */
	private static void loadAddresses_to_PeerConnection(Connection_Peer p, long peer_ID) {
		if (DEBUG) System.out.println("Connections: loadAddresses:"+p);
		
		// when using clouds, the query should be by GIDH (and parameter ID should be GIDH)
		D_Peer peer = D_Peer.getPeerByLID_NoKeep(peer_ID, true);  // not yet used when saving new served orgs
		if (DEBUG) System.out.println("Connection:loadAddresses: peer: "+p.getName());
		if (peer != null) p.peer = peer; else return;
		if (DEBUG) System.out.println("Connection:loadAddresses: peer: loaded D_PeerAddresses");
		loadInstanceAddresses(p, peer, peer.shared_addresses, null);
		for (D_PeerInstance dpi : peer._instances.values()) {
			loadInstanceAddresses(p, peer, dpi.addresses, dpi);
		}
	}
	/**
	 * copy addresses from addresses to p for instance in dpi
	 * Use old sockets from used_peers_HT_GID_PC
	 * @param p
	 * @param peer
	 * @param addresses
	 * @param dpi
	 */
	static void loadInstanceAddresses(Connection_Peer p, D_Peer peer, ArrayList<Address> addresses, D_PeerInstance dpi) {
		// old resolved IP addresses, to avoid repeat the waiting
		//boolean DEBUG = true;//(dpi != null);
		if (DEBUG) System.out.println("*********\n*********\nConnection: loadInstanceAddresses: start dpi=\n"+dpi+"\n crt stored adr="+Util.concat(addresses, ",", "NULL")+" p=\n"+p);
		Connection_Peer _p = used_peers_HT_GID_PC.get(p.getGID()); // previous values
		if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: prev _p=\n"+_p);
		Connection_Instance pi, _pi = null;
		ArrayList<Connections_Peer_Directory> _apd = null, apd = null;  // previous values
		ArrayList<Connections_Peer_Socket> _aps = null, aps = null;  // previous values
		if (dpi != null) {
			pi = p.getInstanceConnection(dpi.peer_instance);
			if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: crt pi=\n"+pi);
			if (pi == null) {
				pi = new Connection_Instance ();
				pi.dpi = dpi;
				p.putInstanceConnection (dpi.peer_instance, pi);
			}
			// the temporary values
			apd = pi.peer_directories;
			aps = pi.peer_sockets;
			
			// old addresses for this peer
			if (_p != null) {
				_pi = _p.getInstanceConnection (dpi.peer_instance);
				if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: old _pi=\n"+_pi);
				if (_pi != null) {
					_apd = _pi.peer_directories;
					_aps = _pi.peer_sockets;
				}
			}
		} else {
			if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: in null dpi");
			apd = p.shared_peer_directories;
			aps = p.shared_peer_sockets;
			if (_p != null) {
				_apd = _p.shared_peer_directories;
				_aps = _p.shared_peer_sockets;
			}
		}
		if (DEBUG) System.out.println("******\nConnection: loadInstanceAddresses: copy old addresses #"+addresses.size());
		for (int a = 0; a < addresses.size(); a ++) {
			Address ad = addresses.get(a);
			if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: a["+a+"]="+ad);
			if (Address.NAT.equals(ad.getPureProtocol())) {
				if (_DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: continue, NAT");
				continue; // not handling NATs stored in database
			}
			if (Address.DIR.equals(ad.getPureProtocol())) {
				if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: DIR: locate in old");
				Connections_Peer_Directory pd = locatePD(_apd, ad);
				if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: DIR: located in old: pd=\n"+pd);
				if (pd == null) {
					// New addresses
					InetSocketAddress isa_tcp = null;
					InetSocketAddress isa_udp = null;
					//if(DEBUG)System.out.println("Connection:loadAddresses: get nonblocking "+domain);
					InetAddress ia = Util.getNonBlockingHostIA(ad.getDomain());
					//if(DEBUG)System.out.println("Connection:loadAddresses: got nonblocking "+ia);
					if (ia != null) {
						if (ad.tcp_port > 0) isa_tcp = new InetSocketAddress(ia, ad.tcp_port);
						if (ad.udp_port > 0) isa_udp = new InetSocketAddress(ia, ad.udp_port);
					} else {
						if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Unknown host: "+ad);
						continue;
					}
					/** New Addresses retrieved */
					
					pd = new Connections_Peer_Directory();
					pd.supernode_addr = new Address_SocketResolved_TCP(isa_tcp, isa_udp, ad);
					pd._last_contact_TCP = ad.last_contact; //Util.getString(item.get(QUERY_LAST_CONN));
				}
				pd.address_ID = ad.get_peer_address_ID(); //Util.lval(item.get(QUERY_ADDR_ID), -1);
				apd.add(pd);
				if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Directory got "+pd);
			} else
				if (Address.SOCKET.equals(ad.getPureProtocol())) { // socket
					if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Socket: locate in old");
					Connections_Peer_Socket ps = locatePS(_aps, ad);
					if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Socket: located in old: pd=\n"+ps);
					if (ps == null) {
						/** New addresses */
						InetSocketAddress isa_tcp = null;
						InetSocketAddress isa_udp = null;
						//if(DEBUG)System.out.println("Connection:loadAddresses: get nonblocking "+domain);
						InetAddress ia = Util.getNonBlockingHostIA(ad.getDomain());
						//if(DEBUG)System.out.println("Connection:loadAddresses: got nonblocking "+ia);
						if (ia != null) {
							if (ad.tcp_port > 0) isa_tcp = new InetSocketAddress(ia, ad.tcp_port);
							if (ad.udp_port > 0) isa_udp = new InetSocketAddress(ia, ad.udp_port);
						} else {
							if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Unknown host: "+ad);
							continue;
						}
						/** New Addresses retrieved */
												
						if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Socket: myself");
						if ((isa_udp != null) &&
								ClientSync.isMyself(Identity.udp_server_port, isa_udp, ad)) {
							if (DEBUG) out.println("***\nConnection: loadInstanceAddresses:  UPeer "+p.getName()+" is myself!"+isa_udp);
							isa_udp = null;
						}
						//if(DEBUG)System.out.println("Connection:loadAddresses: Socket: self udp");
						if ((isa_tcp != null) &&
								(Server.isMyself(isa_udp) ||
										ClientSync.isMyself(Identity.port, isa_tcp, ad))) {
							if (DEBUG) out.println("***\nConnection: loadInstanceAddresses: UPeer "+p.getName()+" is myself!"+isa_udp);
							isa_tcp = null;
						}
						if ((isa_udp == null) && (isa_tcp == null)) {
							if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Socket: null socks, continue");
							continue;
						}
						ps = new Connections_Peer_Socket();
						//if(DEBUG)System.out.println("Connection:loadAddresses: Socket: PS");
						ps.addr = new Address_SocketResolved(ia, isa_tcp, isa_udp, ad);
						//if(DEBUG)System.out.println("Connection:loadAddresses: Socket: SAD");
						ps._last_contact = ad.last_contact;
					}
					ps.address_ID = ad.get_peer_address_ID();// Util.lval(item.get(QUERY_ADDR_ID), -1);
					aps.add(ps);
					if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Socket: ps=\n"+ps);
				}
		}
		/**
		 * Could have uncertified addresses coming from a server and not saved
		 */
		if (DEBUG) System.out.println("******\nConnection: loadInstanceAddresses: try old");
		try {
			if (_aps != null) {
				if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Socket: old peer_socks #"+_aps.size());
				for (int c = _aps.size() - 1; c >= 0; c --) {
					Connections_Peer_Socket tmpsock = _aps.get(c);
				
					if (tmpsock.address_ID > 0) {
						if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Socket: fix old peer_sock #"+tmpsock);
						continue;
					}
					if ((tmpsock.contacted_since_start_TCP && tmpsock.contacted_since_start_UDP) && (!tmpsock.last_contact_successful_TCP && !tmpsock.last_contact_successful_UDP)) {
						if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Socket: failed old peer_sock #"+tmpsock);
						continue;
					}
					/**
					 * Locate a new instance of tmpsock (received from Dir and not in the database)
					 * that could be now in the database, and use it (to have its local address_ID
					 */
					Connections_Peer_Socket crt = locatePS(aps, tmpsock.getAddress());
					if (crt != null) {
						if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Socket: fixated peer_sock #"+tmpsock);
						continue;
					}
					if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: Socket: imported peer_sock #"+tmpsock);
					aps.add(0, tmpsock);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: got: "+p);
	}
	/**
	 * Loads my active listing directories. 
	 * adds them (and TCP socket address) to tmp_my_directories_AL: 
	 */
	private static void init_my_active_directories_listings() {
		if (DEBUG) System.out.println("Connections: init_directories");
		/*
		String ld;
		try {
			ld = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
			return;
		}
		
    	String dirs[] = ld.split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_SEP));
    	*/
		DirectoryAddress dirs[] = DirectoryAddress.getActiveDirectoryAddresses();

		if ((dirs == null) || (dirs.length == 0)) {
			// Only reinit dirs if there is no directory (even inactive) 
			DirectoryAddress _dirs[] = DirectoryAddress.getDirectoryAddresses();
			if ((_dirs == null) || (_dirs.length == 0)) {
	     		String listing_directories;
				try {
					listing_directories = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
					DirectoryAddress.reset(listing_directories);
					dirs = DirectoryAddress.getActiveDirectoryAddresses();
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}
     	}
     	if ((dirs == null) || (dirs.length == 0)) {
    		if (! DD.WARNED_NO_DIRS) {
    			Application_GUI.warning(__("Currently you have no listing_directories for connections found at Connections initialization: " +
    					"\nDo not forget to add some later \n" +
    					"(e.g., from the DirectDemocracyP2P.net list)!\n" +
    					"If you have a stable IP, than you probably do not need it."), __("Configuration"));
    			DD.WARNED_NO_DIRS = true;
    		}
    		return;
    	}
   	for (int k = 0; k < dirs.length; k ++) {
    		try {
	    		//String[] d=dirs[k].split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_ELEM_SEP));
    			Address adr = new Address(dirs[k]);
    			if (!adr.active) continue;
	    		My_Directory md = new My_Directory();
	    		//Address ad = new Address(dirs[k]);
	    		InetAddress ia = Util.getNonBlockingHostIA(adr.getIP());
	    		InetSocketAddress isa = new InetSocketAddress(ia, adr.getTCPPort());
	    		md.supernode_addr = new Address_SocketResolved_TCP(isa, adr);
	    		Connections.tmp_my_directories_AL.add(md);
    		} catch (Exception e) {
    			e.printStackTrace();
    			Application_GUI.warning(__("Error for "+dirs[k]+"\nError Connections: "+e.getMessage()), __("Error installing directories"));
    			continue;
    		}
    	}
		if (DEBUG) System.out.println("Connections: init_directories: done");
	}
	/** alloc tmp structures, fill them with dirs and peers; then switch; (only on start) */
	private static void init() {
		if (DEBUG) System.out.println("Connections: init()");
		allocate_tmp_structures();
		//init_myself();
		init_my_active_directories_listings();
		init_used_peers();
		switch_tmp();
		if (DEBUG) System.out.println("Connections: init() done:"+_toString());
	}

	@Override
	public void update(ArrayList<String> _table, Hashtable<String, DBInfo> info) {
		if (DEBUG) System.out.println("Connections: update: "+Util.concat(_table, ":", "empty"));
		// if (_table.contains(table.application.TNAME)) {update_dirs=true;}
		if (_table.contains(table.directory_address.TNAME)) {update_dirs=true;}
		if (_table.contains(table.peer.TNAME) || _table.contains(table.peer_address.TNAME)){
			update_peers=true;}
		if (DEBUG) System.out.println("Connections: update: will wait_obj");
		synchronized (wait_obj) {
			if (DEBUG) System.out.println("Connections: update: got wait_obj");
			wait_obj.notifyAll();
			if (DEBUG) System.out.println("Connections: update: yield wait_obj");
		}
		if (DEBUG) System.out.println("Connections: update done");
	}
	/**
	 * called on timeout alarm or on database changes
	 */
	private void updates() {	
		boolean __DEBUG = DEBUG;
		if (DEBUG) System.out.println("Connections: updates: ***********");
		if(__DEBUG) System.out.println("**********\nConnections: updates: **********\n"+_toString()+"\nvvvvvvvvvv");
		if (DEBUG) System.out.println("Connections: updates: start");
		/**
		 * database changes
		 */
		if (update_dirs) {
			if (__DEBUG) System.out.println("Connections: updates dirs");
			update_dirs = false;
			Connections.allocate_dirs();
			Connections.init_my_active_directories_listings();
			Connections.switch_tmp_dirs();
		}
		if (update_peers) {
			if (__DEBUG) System.out.println("Connections: updates peers");
			update_peers = false;
			Connections.allocate_peers();
			Connections.init_used_peers();
			Connections.switch_tmp_peers();
		}
		/**
		 * The client cannot contact a peer and requests its re-evaluation
		 */
		if (update_pc.size() > 0) {
			if (__DEBUG) System.out.println("Connections: updates pc");
			Connection_Peer pc;
			if (__DEBUG) System.out.println("Connections: updates will wait_obj");
			synchronized (lock_update_pc) {
				if (__DEBUG) System.out.println("Connections: updates got wait_obj");
				pc = update_pc.remove(0);
				if (__DEBUG) System.out.println("Connections: updates yield wait_obj");
			}
			update_supernode_address(pc);
			if (__DEBUG) System.out.println("Connections: updates pc done");
		}
		if(DEBUG) System.out.println("Connections: updates done");
		if(__DEBUG) System.out.println("Connections: updates: ^^^^^^^^\n"+_toString()+"\n*******");
	}
	
	public void _run() {
		DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Connections Start"));
		try {__run();} catch(Exception e) {}
		DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Will Stop Connections"));
		if (DEBUG) out.println("Connections: run: turned Off");
	}
	/**
	 * Continuously tries to update, each time something changes
	 */
	private void __run() {
		init();
		for (;;) {
			if (DEBUG) System.out.println("Connections: _run: will wait_obj");
			try {
				synchronized (wait_obj) { // waked up by updates (from DB) and by updates request from client
					if (DEBUG) System.out.println("Connections: _run: got wait_obj");
					if (!updates_available()) {
						Application_GUI.ThreadsAccounting_ping("No updates available");
						wait_obj.wait(60*1000);
					}
				}
				Application_GUI.ThreadsAccounting_ping("Updates");
				updates();
			} catch (InterruptedException e) {
				if (_DEBUG) System.out.println("Connections: _run: interrupted");
				//e.printStackTrace();
			} catch (Exception e) {
				if (_DEBUG) System.out.println("Connections: _run: continue");
				e.printStackTrace();
			}
			if (DEBUG) System.out.println("Connections: _run: yield wait_obj");
		}
	}
	/**
	 * //synchronized  (protecting update_pc list)
	 * // only called while holding wait_obj (from _run)
	 * @return
	 */
	private static boolean updates_available() {
		synchronized(lock_update_pc){
			return update_dirs || update_peers || (update_pc.size() != 0);
		}
	}
	/**
	 * To request the update of a given peer from directories
	 * called from client2 (and wake up _run)
	 * @param pc
	 */
	public static void update_supernode_address_request(Connection_Peer pc) {
		if (DEBUG) out.println("Connections: update_supernode_address_request: will wait_obj");
		synchronized (lock_update_pc) {
			if (!update_pc.contains(pc)) {
				update_pc.add(pc);
				if (DEBUG) out.println("Connections: update_supernode_address_request: added");
			}
		}
		synchronized (wait_obj) {
			if (DEBUG) out.println("Connections: update_supernode_address_request: got wait_obj");
			wait_obj.notifyAll();
			if (DEBUG) out.println("Connections: update_supernode_address_request: yield wait_obj");
		}
		if (DEBUG) out.println("Connections: update_supernode_address_request: done");
	}

	/**
	 * Get directory IP (if not yet found), and extract addresses from it
	 * Return null on failure
	 * Currently it returns the addresses for the instances that do not require renegotiation.
	 * @param _pc
	 * @param pd
	 * @param dir_address
	 * @param global_peer_ID
	 * @param peer_name
	 * @param peer_ID
	 * @return
	 */
	static Hashtable<String,ArrayList<Address>> getDirAddress(
			Connection_Peer _pc,
			Connections_Peer_Directory pd, 
			Address dir_address,
			String global_peer_ID,
			String peer_name,
			String peer_ID) {
		
		if (DEBUG) out.println("Connections: getDirAddress: "+dir_address+" ID="+Util.trimmed(global_peer_ID));
		Address ad = pd.supernode_addr.ad;
		InetSocketAddress sock_addr = null;
		
		// test again if directory IP is reachable
		if (pd.supernode_addr.isa == null) {
			InetAddress ia = Util.getHostIA(ad.domain);
			if (ia == null) {
				ClientSync.reportDa(dir_address.ipPort(), global_peer_ID, peer_name, null, __("Null Socket"));
				return null; // cannot find directory server
			}
			pd.supernode_addr.isa = sock_addr = new InetSocketAddress(ia, ad.tcp_port);// Client.getTCPSockAddress(dir_address);
		} else
			sock_addr = pd.supernode_addr.isa;
		if (sock_addr == null) {
			if (DEBUG) out.println("Connections: getDirAddress: null dir address:  reportDa");
			ClientSync.reportDa(dir_address.ipPort(), global_peer_ID, peer_name, null, __("Null Socket"));
			return null; //"";
		}
		Socket socket = new Socket();
		try {
			socket.connect(sock_addr, Server.TIMEOUT_Client_wait_Dir);
			if(DEBUG) out.println("Connections: getDirAddress:  Sending to Directory Server: connected:"+sock_addr);
			if(DEBUG) out.println("Connections: getDirAddress:  Sending to Directory Server: connected:"+dir_address);
			//System.out.println("-----------------------------------------Identity.current_peer_ID.instance= "+Identity.current_peer_ID.instance);
			DirectoryRequest dr = new DirectoryRequest(
					global_peer_ID,
					Identity.getMyPeerGID(),
					Identity.current_peer_ID.instance,
					Identity.udp_server_port, 
					peer_ID,
					dir_address);
			if (DEBUG) out.println("Connections: getDirAddress: got:"+dr);
			byte[] msg = dr.encode();
			socket.setSoTimeout(Server.TIMEOUT_Client_wait_Dir);
			socket.getOutputStream().write(msg);
			if (DEBUG) out.println("Connections: getDirAddress:  Sending to Directory Server: "+Util.byteToHexDump(msg, " ")+dr);
			DirectoryAnswerMultipleIdentities da = new DirectoryAnswerMultipleIdentities(socket.getInputStream());
			
			// Reporting to the widget in Directories Widget
			ClientSync.reportDa(dir_address.ipPort(), global_peer_ID, peer_name, da, null);

			Hashtable<String,ArrayList<Address>> addresses = new Hashtable<String,ArrayList<Address>>();
			for ( DirectoryAnswerInstance inst : da.instances) {
				DIR_Terms_Requested[] terms = inst.instance_terms; // da.terms
				if ((terms != null) && (terms.length != 0)) {
					dr.terms_default = dr.updateTerms(terms, peer_ID, global_peer_ID, dir_address, dr.terms_default);
					if (dr != null) {
						System.out.println("Connections: getDirAddress: negotiation TODO: "+inst);
						continue;
						/* TODO negotiations
						msg = dr.encode();
						socket.setSoTimeout(Server.TIMEOUT_Client_wait_Dir);
						socket.getOutputStream().write(msg);
						if(DEBUG) out.println("Connections: getDirAddress:  Sending to Directory Server: "+Util.byteToHexDump(msg, " ")+dr);
						da = new DirectoryAnswerMultipleIdentities(socket.getInputStream());
						*/
					}
				}
				if (inst.addresses == null) {
					if (_DEBUG) out.println("Connections: getDirAddress:  Got empty addresses!");
					continue;
					//socket.close();
					//return null;
				}
				if (inst.addresses.size() == 0) {
					if(DEBUG) out.println("Connections: getDirAddress:  Got no addresses! da="+da+" for:"+_pc.getName());
					continue;
					//socket.close();
					//return null;
				}
				String _inst = Util.getStringNonNullUnique(inst.instance);
				addresses.put(_inst, inst.addresses); // da.addresses
				if (DEBUG) out.println("Connections: getDirAddress: Dir Answer: "+da);
				//socket.close();
			}
			socket.close();
			return addresses;
			//InetSocketAddress s= da.address.get(0);
			//return s.getHostName()+":"+s.getPort();
		} catch (IOException e) {
			if (DEBUG) out.println("Connections: getDirAddress:  fail: "+e+" peer: "+peer_name+" DIR addr="+dir_address);
			ClientSync.reportDa(dir_address.ipPort(), global_peer_ID, peer_name, null, e.getLocalizedMessage());
			getDirAddressUDP( _pc, pd, dir_address, global_peer_ID, peer_name, peer_ID);
			//e.printStackTrace();
			//Directories.setUDPOn(dir_address, new Boolean(false));
		} catch (Exception e) {
			if (DEBUG) out.println("Connections: getDirAddress:  fail: "+e+" peer: "+peer_name+" DIR addr="+dir_address);
			ClientSync.reportDa(dir_address.ipPort(), global_peer_ID, peer_name, null, e.getLocalizedMessage());
			e.printStackTrace();
			getDirAddressUDP( _pc, pd, dir_address, global_peer_ID, peer_name, peer_ID);
		}
		//socket.close();
		if(DEBUG) out.println("Connections: getDirAddress fail");
		return null;
	}
	/**
	 * This function is queried when the TCP request fails. 
	 * It sends a UDP request but does not wait for answer (retrieving the last available in Connections.
	 * @param sock_addr
	 * @param GID
	 * @param peer_ID
	 * @param dir_address
	 * @return
	 */
	static ArrayList<Address> getDirAddressUDP(
			Connection_Peer _pc,
			Connections_Peer_Directory pd,			
			Address dir_address,
			String GID,
			String peer_name, 
			String peer_ID
			) {
		if (DEBUG) System.out.println("Directories:askAddressUDP: enter dir_address = "+dir_address);
		if ((Application.aus == null) || (UDPServer.ds == null)) return null;
		InetSocketAddress sock_addr = pd.supernode_addr.isa_udp;
		//int udp_port = dir_address.udp_port;
		//if (udp_port <= 0) Util.printCallPath("udp_port="+dir_address.toLongString()); 
		//if (udp_port <= 0) udp_port = dir_address.getTCPPort();
		//if (udp_port <= 0) return null; 
		String GIDH = _pc.getGIDH();
		DirectoryRequest dr =
				new DirectoryRequest(GID,
						Identity.getMyPeerInstance(),
				Identity.getMyPeerGID(),
				Identity.getMyPeerInstance(),
				Identity.udp_server_port, 
				peer_ID,
				dir_address);
		if (DEBUG) System.out.println("Directories:askAddressUDP: prepared dir request dr="+dr);
		//if (DEBUG) System.out.println("Directories:askAddressUDP: dir_address = "+dir_address+" -> "+dir_address.toLongString());
		//if (DEBUG) System.out.println("Directories:askAddressUDP: sock_address="+_sock_addr.getAddress()+" port="+udp_port);
		//InetSocketAddress sock_addr = new InetSocketAddress(_sock_addr.getAddress(), udp_port);
		byte[] msg = dr.encode();
		if (DEBUG) {
			DirectoryRequest drt = new DirectoryRequest(new Decoder(msg));
			if (DEBUG) System.out.println("Directories:askAddressUDP: prepared dec request "+drt);
		}
		try {
			DatagramPacket dp = new DatagramPacket(msg, msg.length, sock_addr);
			UDPServer.ds.send(dp);
		} catch (IOException e) {
			if (DEBUG) e.printStackTrace();
		}
		return Connections.getKnownDirectoryAddresses(dir_address, GIDH, Identity.getMyPeerInstance());
	}
	/**
	 * The peer contact has to be reevaluated
	 * @param _pc
	 */
	private static void update_supernode_address(Connection_Peer _pc) {
		//boolean DEBUG = true;
		if(DEBUG) out.println("Connections: update_supernode_address: **********");
		if(DEBUG) out.println("Connections: update_supernode_address: pc="+_pc);
		String peer_name = _pc.getName();
		String global_peer_ID = _pc.getGID();
		//ArrayList<SocketAddress_Domain> tcp_sock_addresses=new ArrayList<SocketAddress_Domain>();
		//ArrayList<SocketAddress_Domain> udp_sock_addresses=new ArrayList<SocketAddress_Domain>();

		///// start section on PeerContacts tree widget
		String peer_key = peer_name;
		String _now = Util.getGeneralizedTime();
		if (DEBUG) out.println("Connections: update_supernode_address: now="+_now);
		if (peer_key == null) peer_key = Util.trimmed(global_peer_ID);
		/*
		Hashtable<String, Hashtable<String, Hashtable<String,String>>> opc = PeerContacts.peer_contacts.get(peer_key);
		if (opc == null) {
			opc = new Hashtable<String, Hashtable<String, Hashtable<String,String>>>();
			PeerContacts.peer_contacts.put(peer_key, opc);
		}
		String inst = null;
		Hashtable<String, Hashtable<String,String>> pc = opc.get(Util.getStringNonNullUnique(inst));
		if (pc == null) {
			pc = new Hashtable<String, Hashtable<String,String>>();
			opc.put(Util.getStringNonNullUnique(inst), pc);
		}
		*/
		///// end section for displaying peer contacts
		
		update_supernode_address_instance(_pc, null, peer_key, _pc.shared_peer_directories, _pc.shared_peer_sockets);
		for (Connection_Instance i : _pc.instances_HT.values()) {
			update_supernode_address_instance(_pc, i, peer_key, i.peer_directories, i.peer_sockets);
		}
		if(DEBUG) out.println("Connections: update_supernode_address: got pc="+_pc);
	}
	/**
	 * update a given instance's addresses
	 * @param _pc
	 * @param dpi
	 * @param peer_key
	 * @param crt_peer_directories
	 * @param crt_peer_sockets
	 */
	private static void update_supernode_address_instance(Connection_Peer _pc, Connection_Instance dpi, String peer_key,
			ArrayList<Connections_Peer_Directory> crt_peer_directories, ArrayList<Connections_Peer_Socket> crt_peer_sockets) {

		Hashtable<String, Hashtable<String, Hashtable<String,String>>> _peer_contacts_for_this_peer = D_Peer.peer_contacts.get(peer_key);
		if (_peer_contacts_for_this_peer == null) {
			_peer_contacts_for_this_peer = new Hashtable<String, Hashtable<String, Hashtable<String,String>>>();
			D_Peer.peer_contacts.put(peer_key, _peer_contacts_for_this_peer);
		}
		Hashtable<String, Hashtable<String,String>> _peer_contacts_for_this_instance = null;
		String inst = null;
		if (dpi != null && dpi.dpi != null) inst = dpi.dpi.peer_instance;
		_peer_contacts_for_this_instance = _peer_contacts_for_this_peer.get(Util.getStringNonNullUnique(inst));
		if (_peer_contacts_for_this_instance == null) {
			_peer_contacts_for_this_instance = new Hashtable<String, Hashtable<String,String>>();
			_peer_contacts_for_this_peer.put(Util.getStringNonNullUnique(inst), _peer_contacts_for_this_instance);
		}
		IClient This = Application.ac;
		String peer_ID = _pc.peer.getLIDstr();
		// ArrayList<Address> adr_addresses;
		Hashtable<String,ArrayList<Address>> adr_addresses; // <identity,addresses_list>
		String peer_name = _pc.getName();
		String global_peer_ID = _pc.getGID();
		
		for (int k = 0; k < crt_peer_directories.size(); k++) {
			Connections_Peer_Directory pd = crt_peer_directories.get(k);
			Address address_supernode = pd.supernode_addr.ad;
			String type = address_supernode.pure_protocol;
			String now = Util.getGeneralizedTime();
			if (type == null) type = Address.DIR;
			//String s_address = ad.toString();
			String s_addr_ip = address_supernode.ipPort();
			String old_address = s_addr_ip;
			DD.ed.fireClientUpdate(new CommEvent(This, address_supernode.ipPort(), null, "DIR REQUEST", peer_name+" ("+global_peer_ID+")"));
			if (DEBUG) out.println("Connections:update_supernode_address:"+k+" will getDir");
			// can be slow
			// adr_addresses = Client.getDirAddress(s_address, global_peer_ID, peer_name, peer_ID);
			adr_addresses = Connections.getDirAddress(_pc, pd, address_supernode, global_peer_ID, peer_name, peer_ID);
			if (adr_addresses == null) {
				if (DEBUG) out.println("Connections:update_supernode_address:"+k+" did getDir: null");
				if (DEBUG) out.print(" ");
				
				///// Another section on displaying PeerContacts tree widget (new addresses)
				String key = type+":"+s_addr_ip;
				Hashtable<String,String> value = _peer_contacts_for_this_instance.get(key);
				if (value == null) {
					value = new Hashtable<String,String>();
					_peer_contacts_for_this_instance.put(key, value);
				}
				value.put(DD.NO_CONTACT, now);
				///// Done displaying contacts
				
				if (DEBUG) out.println("Connections:update_supernode_address:"+k+" DIR returns empty");
				//return false;
				continue;
			} else {
				if (DEBUG) out.println("Connections:update_supernode_address:"+k+" did getDir: non"
						+ "-null");
				
				if (DEBUG) {
					System.out.println("\n\nGetting:");
					for (String key  : adr_addresses.keySet()) {
						System.out.println("Inst: \""+key+"\" :"+Util.concat(adr_addresses.get(key), " --- ", "NULL"));
					}
				}
			}
			
			// tell of "no contact" if this instance is not in received addresses
			if (adr_addresses.get(Util.getStringNonNullUnique(inst)) == null) {
				ClientSync.peerContactAdd(null, type, s_addr_ip, now, _peer_contacts_for_this_instance);
			}
			
			// add addresses to widget PeerContacts displayed with Peers/Safes
			for (String _inst_nonull : adr_addresses.keySet()) {
				Hashtable<String, ArrayList<Address>> _adr_addresses = new Hashtable<String, ArrayList<Address>>();
				_adr_addresses.put(_inst_nonull, adr_addresses.get(_inst_nonull));
				// treating separately the other (unknown? instances)
				// probably they need not be treated separately
				if (! Util.equalStrings_null_or_not(Util.getStringNonNullUnique(inst), _inst_nonull)) {
					if (DEBUG) System.out.println("Connections:update_supernode_address: dropping inst:"+_inst_nonull+" -> "+Util.concat(adr_addresses.get(_inst_nonull), "---", ""));

					Hashtable<String, Hashtable<String, String>> peer_contacts_for_this_instance = _peer_contacts_for_this_peer.get(_inst_nonull);
					if (peer_contacts_for_this_instance == null) {
						peer_contacts_for_this_instance = new Hashtable<String, Hashtable<String,String>>();
						_peer_contacts_for_this_peer.put(_inst_nonull, peer_contacts_for_this_instance);
					}					
					
					getSocketAddresses_for_peerContacts_widget(//tcp_sock_addresses, udp_sock_addresses,
							_adr_addresses,
							global_peer_ID, 
							type, 
							s_addr_ip, 
							peer_key, 
							now, 
							peer_contacts_for_this_instance,
							DEBUG);

					
					if (DEBUG) Util.printPeerContacts("Connections:update_supernode_address: after diff:"); 
					continue;
				}
				
				// Also handle this current instance, if received (storing in model for PeerContact widget)
				
				
				getSocketAddresses_for_peerContacts_widget(//tcp_sock_addresses, udp_sock_addresses,
						_adr_addresses,
						global_peer_ID, 
						type, 
						s_addr_ip, 
						peer_key, 
						now, 
						_peer_contacts_for_this_instance,
						DEBUG);
				if (DEBUG) Util.printPeerContacts("Connections:update_supernode_address: after eq:"); 
				if (DEBUG) System.out.println("Connections:update_supernode_address: got "+_peer_contacts_for_this_instance);
			}
			
			synchronized(Connections.monitor) {
				
			if (DEBUG) System.out.println("****\nConnections: update_supernode_address_instance: add\n" + Util.concat("\t", adr_addresses, ",", "NULL")); 
			// add addresses and instances to _pc
			integrateDirAddresses(_pc, pd, adr_addresses, //global_peer_ID, type, s_addr_ip, peer_key, now, peer_contacts_for_this_peer,
					crt_peer_directories, crt_peer_sockets);
			if (DEBUG) System.out.println("Connections: update_supernode_address_instance: after integrate: "); 
			if (DEBUG) System.out.println(_pc); 
			if (DEBUG) System.out.println(_toString()); 
			if (DEBUG) Util.printPeerContacts("-"); 
			if (DEBUG) Util.printCallPath("");
			
			}			
			
			
			// pretty print addresses and instances in the Client log
			s_addr_ip = null;
			for (String i : adr_addresses.keySet()) {
				ArrayList<Address> adrs = adr_addresses.get(i);
				String _s_addr_ip = "\""+i+"\" "+Util.concat(adrs.toArray(), DirectoryServer.ADDR_SEP);
				if (s_addr_ip == null) s_addr_ip = _s_addr_ip;
				else s_addr_ip = _s_addr_ip + DirectoryServer.ADDR_SEP + s_addr_ip;
			}
			
			if (DEBUG) out.println("Connections:update_supernode_address: DIR obtained address: "+s_addr_ip);
			DD.ed.fireClientUpdate(new CommEvent(This, old_address, null,"DIR ANSWER", peer_name+" ("+s_addr_ip+")"));
		}
	}
	/**
	 * Adding the addresses in adr_addresses, from s_address, to the PeerContacts tree widget pc.
	 * 
	 * pc is the branch for the current instance, which has the addresses in the adr_addresses.
	 * 
	 * Should handle only one instance at a time!!!
	 * 
	 * @param adr_addresses (by instance, only for one instance)
	 * @param global_peer_ID
	 * @param type
	 * @param string_address
	 * @param peer_key
	 * @param now
	 * @param pc
	 */
	private static void getSocketAddresses_for_peerContacts_widget(
			// ArrayList<SocketAddress_Domain> tcp_sock_addresses,
			// ArrayList<SocketAddress_Domain> udp_sock_addresses,
			Hashtable<String,ArrayList<Address>> _adr_addresses,
			String global_peer_ID,
			String type,
			String string_address,
			String peer_key,
			String now,
			Hashtable<String, Hashtable<String,String>> pc,
			boolean force_DEBUG
			) {
		assert(_adr_addresses.size() <=1);
		boolean DEBUG = Connections.DEBUG || DD.DEBUG_PLUGIN || force_DEBUG;
		if (DEBUG) System.out.println("Connections: getSocketAddresses_for_peerContacts_widget: start");
		if (_adr_addresses == null) {
			if (DEBUG) System.out.println("Connections: getSocketAddresses_for_peerContacts_widget: null addresses");
			return;
		}
		for (String _inst : _adr_addresses.keySet()) { //TODO _inst not used in display!
			ArrayList<Address> adr_addresses = _adr_addresses.get(_inst);
			int sizes = adr_addresses.size();
			if (DEBUG) System.out.println("Connections: getSocketAddresses_for_peerContacts_widget: addresses = "+sizes+" ["+Util.concat(adr_addresses, " ", "DEF")+" ]");
			for (int k = 0; k < sizes; k ++) {
				Address ad = adr_addresses.get(k);
				if (DEBUG) out.print(" "+k+"+"+ad+" ");
				
				// record for sending plugin messages fast
				ClientSync.add_to_peer_contacted_addresses(ad, global_peer_ID);
				
				if (pc != null) {
					if (DEBUG) System.out.println("Connections: getSocketAddresses_for_peerContacts_widget:  add to peer contact");
					ClientSync.peerContactAdd(ad, type, string_address, now, pc);
					if (DEBUG) out.println("Connections: getSocketAddresses_for_peerContacts_widget: enum d_adr="+peer_key+":"+type+":"+string_address+" val="+ad+" "+now);
				}
				/*
				InetSocketAddress ta=null,ua=null;
				try{
					if(ad.tcp_port>0)
						ta=new InetSocketAddress(ad.domain,ad.tcp_port); // can be slow
				}catch(Exception e){e.printStackTrace();}
				if(ta!=null)tcp_sock_addresses.add(new SocketAddress_Domain(ta,ad));
				try{
					if(ad.udp_port>0)
						ua=new InetSocketAddress(ad.domain,ad.udp_port); // can be slow
				}catch(Exception e){e.printStackTrace();}
				if(ua!=null)udp_sock_addresses.add(new SocketAddress_Domain(ua,ad));
				*/
				if (DEBUG) System.out.println("Connections: getSocketAddresses_for_peerContacts_widget: Done handling "+ad);
			}
		}
		if (DEBUG) out.println("");
		if (DEBUG) System.out.println("Connections: getSocketAddresses_for_peerContacts_widget: done");
	}

	
	/**
	 * Integrate addresses adr_addresses received from directory pd into peer lists
	 * @param pd
	 * @param adr_addresses
	 * @param peer_directories_list
	 * @param peer_sockets_list
	 */
	private static void integrateDirAddresses(Connection_Peer _pc,
			Connections_Peer_Directory pd,
			Hashtable<String,ArrayList<Address>> _adr_addresses,
			ArrayList<Connections_Peer_Directory> peer_directories_list,
			ArrayList<Connections_Peer_Socket> peer_sockets_list) {
		
		if (_adr_addresses == null) {
			if (_DEBUG) System.out.println("Connections: integrateDirAddresses #null addresses");
			return;
		}
		if (DEBUG) System.out.println("Connections: integrateDirAddresses insts #"+_adr_addresses.size()+" "+pd);
		for (String _inst : _adr_addresses.keySet()) {
			ArrayList<Address> adr_addresses = _adr_addresses.get(_inst);
			if (DEBUG) System.out.println("Connections: integrateDirAddresses #"+adr_addresses.size()+" "+pd);
			Connection_Instance inst = _pc.instances_HT.get(_inst);
			if (inst == null) {
				inst = new Connection_Instance();
				_pc.instances_HT.put(_inst, inst);
			}
			peer_directories_list = inst.peer_directories;
			peer_sockets_list = inst.peer_sockets;
			
			for (int k = 0; k < adr_addresses.size(); k++) {
				Address ad = adr_addresses.get(k);
				if (DEBUG) System.out.println("Connections: integrateDirAddresses:  ad= "+ad);
				if (Address.NAT.equals(ad.pure_protocol)) {
					InetSocketAddress isa = new InetSocketAddress(ad.domain, ad.udp_port);
					Address_SocketResolved_TCP reported_peer_addr = new Address_SocketResolved_TCP(isa, ad);
					pd.reported_peer_addr_ = reported_peer_addr;
					pd._reported_peer_addr.put(_inst, pd.reported_peer_addr_);
					if (DEBUG) System.out.println("Connections: integrateDirAddresses: got= "+pd._reported_peer_addr);
					continue;
				}
				// TODO if a DIR is retrieved, it should be queried recursively!
				if (Address.DIR.equals(ad.pure_protocol)) {
					Connections_Peer_Directory _pd = locatePD(peer_directories_list, ad);
					if (_pd != null) {
						if (DEBUG) System.out.println("Connections: integrateDirAddresses: preexisting= "+_pd);
						continue;
					}
					InetAddress ia = Util.getHostIA(ad.domain);
					if (ia == null) {
						if (DEBUG) System.out.println("Connections: integrateDirAddresses: got= "+ad.domain);
						continue;
					}
					_pd = new Connections_Peer_Directory(ad, ia);
					peer_directories_list.add(_pd);
					if (DEBUG) System.out.println("Connections: integrateDirAddresses: got dir= "+_pd);
					continue;
				}
				if ((ad.pure_protocol == null) || Address.SOCKET.equals(ad.pure_protocol)) {
					Connections_Peer_Socket _ps = locatePS(peer_sockets_list, ad);
					if (_ps != null) {
						if(DEBUG) System.out.println("Connections: integrateDirAddresses: got existing= "+_ps);
						continue;
					}
					InetAddress ia = Util.getHostIA(ad.domain);
					if(ia == null){
						if(DEBUG) System.out.println("Connections: integrateDirAddresses: null ia= "+ad);
						continue;
					}
					Connections_Peer_Socket ps = new Connections_Peer_Socket(ad, ia);
					peer_sockets_list.add(ps);
					if(DEBUG) System.out.println("Connections: integrateDirAddresses: got sock= "+_ps);
					continue;
				}
				if(DEBUG) System.out.println("Connections: integrateDirAddresses: ad no protocol");
			}
		}
	}
/**
 * May once use a hashtable to retrieve faster the domains!
 * Finds the entry in _pc.peer_sockets that has the same:
 * domain, tcp_port and udp_port (if any). tests for null _pc
 * @param _pc
 * @param domain
 * @param tcp_port
 * @return
 */
	private static Connections_Peer_Socket locatePS(ArrayList<Connections_Peer_Socket> shared_peer_sockets, Address _ad){
		//if (DEBUG) System.out.println("Connectione: locatePS: "+_ad);
		if (shared_peer_sockets == null) {
			if(DEBUG) System.out.println("Connection: locatePS: null _pc for: "+_ad);
			return null;
		}
		String domain = _ad.domain;
		int tcp_port = _ad.tcp_port;
		int udp_port = _ad.udp_port;
		for (int k = 0; k < shared_peer_sockets.size(); k ++) {
			Connections_Peer_Socket ps = shared_peer_sockets.get(k);
			Address ad = ps.addr.ad;
			if (ad.domain.equals(domain) && (ad.tcp_port == tcp_port) && (ad.udp_port == udp_port)) {
				if (DEBUG) System.out.println("Connection: locatePS: "+_ad+" got-> "+ps);
				return ps;
			}
		}
		if (DEBUG) System.out.println("Connection: locatePS: not found "+_ad);
		return null;
	}
	/**
	 * Finds the entry in _pc.peerdirectories that has the same:
	 * domain, tcp_port and udp_port (if any). tests for null _pc
	 * @param _pc
	 * @param _ad
	 * @return
	 */
	private static Connections_Peer_Directory locatePD(ArrayList<Connections_Peer_Directory> shared_peer_directories, Address _ad) {
		if (DEBUG) System.out.println("Connection: locatePD: "+_ad);
		if (shared_peer_directories == null){
			if(DEBUG) System.out.println("Connection: locatePD: null _pc");
			return null;
		}
		String domain = _ad.domain;
		int tcp_port = _ad.tcp_port;
		int udp_port = _ad.udp_port;
		for (int k = 0; k < shared_peer_directories.size(); k++){
			Connections_Peer_Directory pd = shared_peer_directories.get(k);
			Address ad = pd.supernode_addr.ad;
			if (ad.domain.equals(domain) && (ad.tcp_port == tcp_port) && (ad.udp_port == udp_port)) {
				if(DEBUG) System.out.println("Connection: locatePD: "+pd);
				return pd;
			}
		}
		if (DEBUG) System.out.println("Connection: locatePD: not found");
		return null;
	}
	/**
	 * Temporary function converting data from Client2 for usage with Client1.try_send
	 * Ideally should be abandoned when try_send uses directly the parameters of Client2
	 * @param tcp_sock_addresses
	 * @param udp_sock_addresses
	 * @param pca
	 * @param peer_GID
	 * @param peer_directories
	 * @param peer_directories_sockets
	 */
	public static void getSocketAddresses(
			ArrayList<Address_SocketResolved_TCP> tcp_sock_addresses,
			ArrayList<Address_SocketResolved_TCP> udp_sock_addresses,
			ArrayList<Address> pca, String peer_GID, 
			ArrayList<String> peer_directories,
			ArrayList<InetSocketAddress> peer_directories_sockets) {
		synchronized(lock_used_structures) {
			Connection_Peer pc = Connections.used_peers_HT_GID_PC.get(peer_GID);
			if (pc != null) {
				if (pc.shared_peer_sockets != null) {
					for(int k = 0; k < pc.shared_peer_sockets.size(); k++){
						Connections_Peer_Socket ps = pc.shared_peer_sockets.get(k);
						if (ps.addr.isa_tcp != null) tcp_sock_addresses.add(Address_SocketResolved_TCP.getTCP(ps.addr));
						if (ps.addr.isa_udp != null) udp_sock_addresses.add(Address_SocketResolved_TCP.getUDP(ps.addr));
					}
				} else {
					System.out.println ("Connections:getSocketAddresses null peer_sockets for "+peer_GID);
				}
			} else {
				System.out.println ("Connections:getSocketAddresses null pc for peer "+peer_GID);
				return;
			}
			
			if (pc.shared_peer_directories == null) { 
				System.out.println ("Connections:getSocketAddresses null peer_directories for "+peer_GID);
				return;
			}
			
			for (int k = 0; k < pc.shared_peer_directories.size(); k ++){
				Connections_Peer_Directory pd = pc.shared_peer_directories.get(k);
				if (pd == null) {
					System.out.println ("Connections:getSocketAddresses null pd for peer_dir "+k);
					continue;
				}
				boolean added = false;
				for (Address_SocketResolved_TCP adr : pd._reported_peer_addr.values()) {
					udp_sock_addresses.add(adr);
					added = true;
				}
				if (pd.reported_peer_addr_ != null) {
					udp_sock_addresses.add(pd.reported_peer_addr_);
					added = true;
				}
				if (added) {
					peer_directories.add(pd.supernode_addr.ad.toString());
					peer_directories_sockets.add(pd.supernode_addr.isa);
				}
			}
		}
	}

	public static void main(String args[]){
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			if (DEBUG) System.out.println("Connection: main: Client2");
			Client2 c2 = new Client2();
			c2.start();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * This should return address of "GID:instance" as last received from the directory: dir_address.
	 * Ideally we should find a way to also return the date of this information.
	 * @param dir_address
	 * @param GID
	 * @param instance
	 * @return
	 */
	public static ArrayList<Address> getKnownDirectoryAddresses (
			Address dir_address, String GIDH, String instance) {
		if (DEBUG) System.out.println("Connections: getKnownDirectoryAddresses: query dir = "+dir_address+" GIDH = "+GIDH+" : "+instance);
		String directory_domain = dir_address.getIP();
		int directory_udp_port = dir_address.udp_port;
		D_Peer me = data.HandlingMyself_Peer.get_myself_with_wait();
		
		if ((Util.equalStrings_null_or_not(instance, me.getInstance())) && Util.equalStrings_null_or_not(GIDH, me.getGIDH_force())) {
			String ipport = directory_domain+":"+directory_udp_port;
			if (DEBUG) System.out.println("Connections: getKnownDirectoryAddresses: me "+ipport);
			Connections_Peer_Directory pd = Connections.personal_HT_IPPORT_PC.get(ipport);
			if (pd == null) {
				if (DEBUG) System.out.println("Connections: getKnownDirectoryAddresses: null pd");
				return null;
			} else {
				if (DEBUG) System.out.println("Connections: getKnownDirectoryAddresses: found pd");
				return pd.reportedAddressesUDP;
			}
		}
		
		Connections_Peer_Directory pd = Connections.getConnectionPeerDirectory(directory_domain, directory_udp_port, GIDH, instance);
		if (pd == null) {
			if (DEBUG) System.out.println("Connections: getKnownDirectoryAddresses: no pd");
			return null;
		}
		return pd.reportedAddressesUDP;
	}

	/**
	 * This procedure should register an UDP incoming answer to some request.
	 * It should also continue negotiating the directory server using extra terms (if appropriate).
	 * The sender IP can be identified from the packet.
	 * @param dami
	 * @param pak
	 */
	public static void registerIncomingDirectoryAnswer (
			DirectoryAnswerMultipleIdentities dami, DatagramPacket pak) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("Connections: registerIncomingDirectoryAnswer: got "+pak.getSocketAddress()+" "+dami);
		String directory_domain = dami.directory_domain;
		int directory_udp_port = dami.directory_udp_port;
		InetSocketAddress sa = (InetSocketAddress) pak.getSocketAddress();
		if (directory_udp_port <= 0) directory_udp_port = sa.getPort();
		if (directory_domain == null) directory_domain = sa.getHostString();
		String ipport = directory_domain+":"+directory_udp_port;
		D_Peer me = data.HandlingMyself_Peer.get_myself_with_wait();
		D_Peer target = D_Peer.getPeerByGID_or_GIDhash(null, dami.remote_GIDhash, true, false, false, null);
		ClientSync.reportDa(ipport+"/UDP", target.getGID(), target.getName(), dami, null);
		Connections_Peer_Directory pd;
		
		Hashtable<String,ArrayList<Address>> adr = new Hashtable<String,ArrayList<Address>> ();
		for (DirectoryAnswerInstance inst : dami.instances) { 
			if (DEBUG) System.out.println("Connections: registerIncomingDirectoryAnswer: handling "+inst);
			if ((Util.equalStrings_null_or_not(inst.instance, me.getInstance())) && Util.equalStrings_null_or_not(dami.remote_GIDhash, me.getGIDH_force())) {
				if (DEBUG) System.out.println("Connections: registerIncomingDirectoryAnswer: me "+ipport);
				Connections_Peer_Directory _pd = Connections.personal_HT_IPPORT_PC.get(ipport);
				if (_pd == null) {
					_pd = new Connections_Peer_Directory();
					Connections.personal_HT_IPPORT_PC.put(ipport, _pd);
					_pd.supernode_addr = new Address_SocketResolved_TCP((InetSocketAddress) pak.getSocketAddress(), new Address(ipport));
				}
				_pd._last_contact_UDP = Util.getGeneralizedTime();
				_pd.last_contact_successful_UDP = true;
				_pd.contacted_since_start_UDP = true;
				_pd.reportedAddressesUDP = inst.addresses;
				adr.put(Util.getStringNonNullUnique(inst.instance), _pd.reportedAddressesUDP);
				//continue;
			} else {
				if (DEBUG) System.out.println("Connections: registerIncomingDirectoryAnswer: not me inst:"+inst.instance+ " vs "+ me.getInstance()+" = "+Util.equalStrings_null_or_not(inst.instance, me.getInstance()));
				if (DEBUG) System.out.println("Connections: registerIncomingDirectoryAnswer: not me gidh:"+dami.remote_GIDhash+" vs "+ me.getGIDH_force()+" = "+Util.equalStrings_null_or_not(dami.remote_GIDhash, me.getGIDH_force()));
			}
		
			pd = Connections.getConnectionPeerDirectory(directory_domain, directory_udp_port, dami.remote_GIDhash, inst.instance);
			if (pd == null) {
				if (DEBUG) System.out.println("Connections: registerIncomingDirectoryAnswer: no pd");
				continue;
			}
			if (DEBUG) System.out.println("Connections: registerIncomingDirectoryAnswer: store in pd = "+pd);
			pd.reportedAddressesUDP = inst.addresses;
			pd._last_contact_UDP = Util.getGeneralizedTime();
			pd.last_contact_successful_UDP = true;
			pd.contacted_since_start_UDP = true;
			
			adr.put(Util.getStringNonNullUnique(inst.instance), pd.reportedAddressesUDP);

			
			String type = null; //dami.pure_protocol;
			String now = Util.getGeneralizedTime();
			if (type == null) type = Address.DIR;
			String s_addr_ip = sa.toString();
			if (s_addr_ip.startsWith("/")) s_addr_ip = s_addr_ip.substring(1);
			s_addr_ip = s_addr_ip + "/UDP";
			Connection_Peer pc = getConnectionPeer(dami.remote_GIDhash);
			String peer_name = pc.getName();
			//String key = type+":"+s_addr_ip;

			String peer_key = peer_name;
			String _now = Util.getGeneralizedTime();
			if (DEBUG) out.println("Connections: registerIncomingDirectoryAnswer: now="+_now);
			if (peer_key == null) peer_key = Util.trimmed(dami.remote_GIDhash);

			
			if (DEBUG) {
				Util.printPeerContacts("Connections: registerIncomingDirectoryAnswer: start: ");
			}
		

			Hashtable<String,ArrayList<Address>> reportedAddressesUDP = new Hashtable<String,ArrayList<Address>>();
			reportedAddressesUDP.put(Util.getStringNonNullUnique(inst.instance), pd.reportedAddressesUDP);
			
			Hashtable<String, Hashtable<String, Hashtable<String,String>>> _p_c = D_Peer.peer_contacts.get(peer_key);
			
			if (_p_c == null) {
				_p_c = new Hashtable<String, Hashtable<String, Hashtable<String,String>>>();
				D_Peer.peer_contacts.put(peer_key, _p_c);
			}
			
			Hashtable<String, Hashtable<String,String>> p_c = _p_c.get(Util.getStringNonNullUnique(inst.instance));
			if (p_c == null) {
				p_c = new Hashtable<String, Hashtable<String,String>>();
				_p_c.put(Util.getStringNonNullUnique(inst.instance), p_c);
			}
			getSocketAddresses_for_peerContacts_widget(//tcp_sock_addresses, udp_sock_addresses,
					reportedAddressesUDP,
					dami.remote_GIDhash, type, s_addr_ip, peer_key, now,
					p_c,
					DEBUG);
			if (DEBUG) Util.printPeerContacts("Connections: registerIncomingDirectoryAnswer: after:"); 
			if (DEBUG) System.out.println("Connections: registerIncomingDirectoryAnswer: set in = "+peer_key);
		
			Connection_Peer _pc[] = new Connection_Peer[1];
			Connection_Instance pi = Connections.getConnectionPeerInstance(dami.remote_GIDhash, inst.instance, _pc);
			integrateDirAddresses(_pc[0], pd, adr, pi.peer_directories, pi.peer_sockets);			
		}	
	}
}
