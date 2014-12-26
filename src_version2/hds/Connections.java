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
public class Connections extends util.DDP2P_ServiceThread implements DBListener {
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static final long CONNECTIONS_UPDATE_TIMEOUT_MSEC = 60*1000;

	/**
	 * This (myself) peer's addresses as seen by AddressBooks (directories). 
	 * The key in the hashtable is obtained with getIPPORT()
	 * being a concatenation of domain and udp port separated with ":".
	 * The value is the Connection_sPeer_Directories structure.
	 */
	static public Hashtable<String,Connections_Peer_Directory> myselfPeer_HT_IPPORT_CPD = new Hashtable<String,Connections_Peer_Directory>();
	/**
	 * The ordered list of contacted peer addresses
	 */
	static public ArrayList<Connection_Peer> used_peers_AL_CP;
	/**
	 * The addresses of contacted peers (by GID), accessed in "used_peers_AL_CP"
	 */
	static public Hashtable<String,Connection_Peer> used_peers_HT_GID_CP;//= new Hashtable<String,Connection_Peer>();
	/**
	 * The addresses of contacted peers (by GIDH), accessed in "used_peers_AL_CP"
	 */
	static public Hashtable<String,Connection_Peer> used_peers_HT_GIDH_CP; //= new Hashtable<String,Connection_Peer>();

	static public ArrayList<My_Directory> my_directories_AL;
	static public int peersAvailable = 0;
	
	static private Hashtable<String,Connection_Peer> tmp_used_peers_HT_GID_PC;
	static private Hashtable<String,Connection_Peer> tmp_used_peers_HT_GIDH_PC;
	static private ArrayList<Connection_Peer> tmp_used_peers_AL_PC;
	static private ArrayList<My_Directory> tmp_my_directories_AL;
	private static int tmp_peersAvailable;
	
	/** flags on whether changes are needed for dirs */
	private static boolean update_dirs;
	/** flags on whether changes are needed for list of peers */
	private static boolean update_peers;
	/** list of peer_connections that need updating */
	private static ArrayList<Connection_Peer> update_needing_peer_connections = new ArrayList<Connection_Peer>();
	
	/** flags and monitors to update needs, keep wait_obj during lock_update_pc */
	private final static Object monitor_wait_obj = new Object();
	/** control access to update_needing_peer_connections; it happens to be locked during wait_obj ! (do not try inverse for deadly brace!) */
	private final static Object lock_update_needing_peer_connections = new Object();
	/** changing used_peers_AL_PC, used_peers_HT_GIDH_PC */
	private final static Object lock_used_structures = new Object();
	/** locked while calling "integrateDirAddresses" */
	private static final Object monitor_integrateDirAddresses = new Object();

	
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
				synchronized (monitor_wait_obj) { // waked up by updates (from DB) and by updates request from client
					if (DEBUG) System.out.println("Connections: _run: got wait_obj");
					if (! updates_available()) {
						Application_GUI.ThreadsAccounting_ping("No updates available");
						monitor_wait_obj.wait(CONNECTIONS_UPDATE_TIMEOUT_MSEC); //60*1000);
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
	/** allocate new tmp structures, fill them with dirs and peers; then switch tmp-actual; (why only on start?) */
	private static void init() {
		if (DEBUG) System.out.println("Connections: init()");
		allocate_tmp_structures();
		//init_myself();
		init_my_active_directories_listings();
		init_used_peers();
		switch_tmp();
		if (DEBUG) System.out.println("Connections: init() done:"+_toString());
	}
	
	public static hds.Connections_Peer_Directory getConnectionPeerDirectory(String domain, int udp_port, String GIDH, String instance) {
		//boolean DEBUG = true;
		if (DEBUG || (GIDH == null)) System.out.println("Connections: getConnectionPeerDirectory: enter GIDH="+GIDH+" i:"+instance+" from="+domain+":"+udp_port);
		Connection_Peer peer = used_peers_HT_GIDH_CP.get(GIDH);
		if (peer != null) {
			
			// instance
			Connection_Instance pi = peer.getInstanceConnection(instance);
			if (pi == null) {
				if (DEBUG) System.out.println("Connections: getConnectionPeerDirectory: unknown instance: "+instance);
				return null;
			}
			for (Connections_Peer_Directory dir : pi.peer_directories) {
				if (DEBUG) System.out.println("Connections: getConnectionPeerDirectory: try: "+dir.supernode_addr.getAddressSupernode().toLongString());
				if (!Util.equalStrings_null_or_not(domain, dir.supernode_addr.getAddressSupernode().domain)) continue;
				if (dir.supernode_addr.getAddressSupernode().udp_port != udp_port) continue;
				return dir;
			}
			
			for (Connections_Peer_Directory dir : peer.shared_peer_directories) {
				if (DEBUG) System.out.println("Connections: getConnectionPeerDirectory: try: "+dir.supernode_addr.getAddressSupernode().toLongString());
				if (!Util.equalStrings_null_or_not(domain, dir.supernode_addr.getAddressSupernode().domain)) continue;
				if (dir.supernode_addr.getAddressSupernode().udp_port != udp_port) continue;
				return dir;
			}
		} else {
			if (DEBUG) System.out.println("Connections: getConnectionPeerDirectory: peer not used: "+GIDH);
		}
		return null;
	}
	
	public static Connection_Instance getConnectionPeerInstance(String GIDH, String instance, Connection_Peer _pc[]) {
		Connection_Peer peer = used_peers_HT_GIDH_CP.get(GIDH);
		if (peer != null) {
			if ((_pc != null) && (_pc.length > 0)) _pc[0] = peer;
			// instance
			Connection_Instance pi = peer.getInstanceConnection(instance);
			return pi;
		}
		return null;
	}
	
	public static Connection_Peer getConnectionPeer(String GIDH) {
		Connection_Peer peer = used_peers_HT_GIDH_CP.get(GIDH);
		return peer;
	}
	
	public static String _toString() {
		return "[Connections: #"+peersAvailable+
				" \npeers=\n "+Util.concat(used_peers_AL_CP, "\n ", "empty")+
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
			if (crt < 0) result = null;
			else if(crt >= peersAvailable) result = null;
				else result = used_peers_AL_CP.get(crt);
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
			used_peers_HT_GID_CP = tmp_used_peers_HT_GID_PC;
			used_peers_HT_GIDH_CP = tmp_used_peers_HT_GIDH_PC;
			if (tmp_used_peers_HT_GIDH_PC == null) Util.printCallPath("");
			used_peers_AL_CP = tmp_used_peers_AL_PC;
			peersAvailable = used_peers_AL_CP.size();
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
	 * Computes # of used peers in: tmp_peersAvailable. Gets the list of all used "peers", then:
	 * Loads peers in tmp_used_peers_AL_PC, tmp_used_peers_HT_GID_PC, tmp_used_peers_HT_GIDH_PC.
	 * 
	 * TODO ? Should compute last_sync_date from instances.
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
			Connection_Peer peer_connection = new Connection_Peer();
			//long ID = peers.get(k).getLID_keep_force(); //Util.lval(peers.get(k).get(QUERY_ID), -1);
			D_Peer peer = peers.get(k);
			if (DEBUG) System.out.println("Connections: init_peers: peer #=" + peer); //ID);
			loadAddresses_to_PeerConnection(peer_connection, peer);
			tmp_used_peers_AL_PC.add(peer_connection);
			tmp_used_peers_HT_GID_PC.put(peer_connection.getGID(), peer_connection);
			tmp_used_peers_HT_GIDH_PC.put(peer_connection.getGIDH(), peer_connection);
		}
		if (DEBUG) System.out.println("Connections: init_peers: got="+_toString());
	}
	/**
	 * Loads the addresses from _peer in Peer_Connection p (for tmp_used_peers_xxx).
	 * For each address in the database, it checks the current values and uses the eventually found sockets and flags values.
	 * 
	 * Then it loads eventual uncertified addressed potentially received.
	 * 
	 * @param next_conn_peer
	 * @param peer_ID : In the future should us GIDH to support clouds
	 * @param _peer 
	 */
	private static void loadAddresses_to_PeerConnection(Connection_Peer next_conn_peer, D_Peer _peer) {
		if (DEBUG) System.out.println("Connections: loadAddresses_to_PeerConnection: start with empty pc for: "+_peer);
		
		// when using clouds, the query should be by GIDH (and parameter ID should be GIDH)
		//if (_peer == null) _peer = D_Peer.getPeerByLID_NoKeep(peer_ID, true);  // not yet used when saving new served orgs
		//if (DEBUG) System.out.println("Connection: loadAddresses_to_PeerConnection: peer: "+p.getName());
		if (_peer != null) next_conn_peer.peer = _peer;
		else {
			if (_DEBUG) System.out.println("Connection: loadAddresses_to_PeerConnection: null peer");
			return;
		}
		if (DEBUG) System.out.println("Connection: loadAddresses_to_PeerConnection: peer: loaded D_PeerAddresses");
		
		loadInstanceAddresses(next_conn_peer, _peer, _peer.shared_addresses, null);
		
		for (D_PeerInstance dpi : _peer._instances.values()) {
			loadInstanceAddresses(next_conn_peer, _peer, dpi.addresses, dpi);
		}
		if (DEBUG) System.out.println("Connections: loadAddresses_to_PeerConnection: quit with loaded: "+next_conn_peer);
	}
	/**
	 * copy addresses from addresses to p for instance in dpi
	 * Use old sockets from used_peers_HT_GID_PC
	 * @param next_conn_peer
	 * @param peer
	 * @param addresses
	 * @param dpi
	 */
	static void loadInstanceAddresses(Connection_Peer next_conn_peer, D_Peer peer, ArrayList<Address> addresses, D_PeerInstance dpi) {
		// old resolved IP addresses, to avoid repeat the waiting
		//boolean DEBUG = true;//(dpi != null);
		if (DEBUG) System.out.println("*********\n*********\nConnection: loadInstanceAddresses: start dpi=\n"+dpi+"\n crt stored adr="+Util.concat(addresses, ",", "NULL")+" p=\n"+next_conn_peer);
		Connection_Peer old_conn_peer = used_peers_HT_GID_CP.get(next_conn_peer.getGID()); // previous values
		if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: prev _p=\n"+old_conn_peer);
		Connection_Instance old_conn_peer_instance, next_conn_peer_instance = null;
		ArrayList<Connections_Peer_Directory> old_conn_peer_dirs_AL = null, next_conn_peer_dirs_AL = null;  // previous values
		ArrayList<Connections_Peer_Socket> old_conn_peer_sock_AL = null, next_conn_peer_sock_AL = null;  // previous values
		if (dpi != null) {
			next_conn_peer_instance = next_conn_peer.getInstanceConnection(dpi.peer_instance);
			if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: crt pi=\n"+next_conn_peer_instance);
			if (next_conn_peer_instance == null) {
				next_conn_peer_instance = new Connection_Instance ();
				next_conn_peer_instance.dpi = dpi;
				next_conn_peer.putInstanceConnection (dpi.peer_instance, next_conn_peer_instance);
			}
			// the temporary values
			next_conn_peer_dirs_AL = next_conn_peer_instance.peer_directories;
			next_conn_peer_sock_AL = next_conn_peer_instance.peer_sockets;
			
			// old addresses for this peer
			if (old_conn_peer != null) {
				old_conn_peer_instance = old_conn_peer.getInstanceConnection (dpi.peer_instance);
				if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: old _pi=\n"+old_conn_peer_instance);
				if (old_conn_peer_instance != null) {
					old_conn_peer_dirs_AL = old_conn_peer_instance.peer_directories;
					old_conn_peer_sock_AL = old_conn_peer_instance.peer_sockets;
				}
			}
		} else {
			if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: in null dpi");
			next_conn_peer_dirs_AL = next_conn_peer.shared_peer_directories;
			next_conn_peer_sock_AL = next_conn_peer.shared_peer_sockets;
			if (old_conn_peer != null) {
				old_conn_peer_dirs_AL = old_conn_peer.shared_peer_directories;
				old_conn_peer_sock_AL = old_conn_peer.shared_peer_sockets;
			}
		}
		if (DEBUG) System.out.println("******\nConnection: loadInstanceAddresses: copy old addresses #"+addresses.size());
		loadInstanceAddresses_certified(addresses, old_conn_peer_dirs_AL, next_conn_peer_dirs_AL, old_conn_peer_sock_AL, next_conn_peer_sock_AL, next_conn_peer.getName());

		loadInstanceAddresses_uncertified(old_conn_peer_dirs_AL, next_conn_peer_dirs_AL, old_conn_peer_sock_AL, next_conn_peer_sock_AL);
		if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses: got: "+next_conn_peer);
	}
	/**
	 * Now load only the addresses coming from the database: found in loadInstanceAddresses_certified
	 * @param addresses
	 * @param old_conn_peer_dirs_AL
	 * @param next_conn_peer_dirs_AL
	 * @param old_conn_peer_sock_AL
	 * @param next_conn_peer_sock_AL
	 * @param peer_name
	 */
	static void loadInstanceAddresses_certified (
			ArrayList<Address> addresses,
			ArrayList<Connections_Peer_Directory> old_conn_peer_dirs_AL,
			ArrayList<Connections_Peer_Directory> next_conn_peer_dirs_AL,
			ArrayList<Connections_Peer_Socket> old_conn_peer_sock_AL,
			ArrayList<Connections_Peer_Socket> next_conn_peer_sock_AL,
			String peer_name
			)
	{
		for (int crt_adr_idx = 0; crt_adr_idx < addresses.size(); crt_adr_idx ++) {
			Address crt_adr = addresses.get(crt_adr_idx);
			if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_certified: a["+crt_adr_idx+"]="+crt_adr);
			if (Address.NAT.equals(crt_adr.getPureProtocol())) {
				if (_DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_certified: continue, NAT");
				continue; // not handling NATs stored in database
			}
			if (Address.DIR.equals(crt_adr.getPureProtocol())) {
				if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_certified: DIR: locate in old");
				Connections_Peer_Directory pd = locatePD(old_conn_peer_dirs_AL, crt_adr);
				if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_certified: DIR: located in old: pd=\n"+pd);
				if (pd == null) {
					// New addresses
					InetSocketAddress isa_tcp = null;
					InetSocketAddress isa_udp = null;
					//if(DEBUG)System.out.println("Connection:loadAddresses: get nonblocking "+domain);
					InetAddress ia = Util.getNonBlockingHostIA(crt_adr.getDomain());
					//if(DEBUG)System.out.println("Connection:loadAddresses: got nonblocking "+ia);
					if (ia != null) {
						if (crt_adr.tcp_port > 0) isa_tcp = new InetSocketAddress(ia, crt_adr.tcp_port);
						if (crt_adr.udp_port > 0) isa_udp = new InetSocketAddress(ia, crt_adr.udp_port);
					} else {
						if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_certified: Unknown host: "+crt_adr);
						continue;
					}
					/** New Addresses retrieved */
					
					pd = new Connections_Peer_Directory();
					pd.supernode_addr = new Address_SocketResolved_TCP(isa_tcp, isa_udp, crt_adr);
					pd._last_contact_TCP = crt_adr.last_contact; //Util.getString(item.get(QUERY_LAST_CONN));
				}
				pd.address_ID = crt_adr.get_peer_address_ID(); //Util.lval(item.get(QUERY_ADDR_ID), -1);
				next_conn_peer_dirs_AL.add(pd);
				if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_certified: Directory got "+pd);
			} else
				if (Address.SOCKET.equals(crt_adr.getPureProtocol())) { // socket
					if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_certified: Socket: locate in old");
					Connections_Peer_Socket ps = locatePS(old_conn_peer_sock_AL, crt_adr);
					if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_certified: Socket: located in old: pd=\n"+ps);
					if (ps == null) {
						/** New addresses */
						InetSocketAddress isa_tcp = null;
						InetSocketAddress isa_udp = null;
						//if(DEBUG)System.out.println("Connection:loadAddresses: get nonblocking "+domain);
						InetAddress ia = Util.getNonBlockingHostIA(crt_adr.getDomain());
						//if(DEBUG)System.out.println("Connection:loadAddresses: got nonblocking "+ia);
						if (ia != null) {
							if (crt_adr.tcp_port > 0) isa_tcp = new InetSocketAddress(ia, crt_adr.tcp_port);
							if (crt_adr.udp_port > 0) isa_udp = new InetSocketAddress(ia, crt_adr.udp_port);
						} else {
							if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_certified: Unknown host: "+crt_adr);
							continue;
						}
						/** New Addresses retrieved */
												
						if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_certified: Socket: myself");
						if ((isa_udp != null) &&
								ClientSync.isMyself(Identity.udp_server_port, isa_udp, crt_adr)) {
							if (DEBUG) out.println("***\nConnection: loadInstanceAddresses_certified:  UPeer " + peer_name +" is myself!"+isa_udp);
							isa_udp = null;
						}
						//if(DEBUG)System.out.println("Connection:loadAddresses: Socket: self udp");
						if ((isa_tcp != null) &&
								(Server.isMyself(isa_udp) ||
										ClientSync.isMyself(Identity.port, isa_tcp, crt_adr))) {
							if (DEBUG) out.println("***\nConnection: loadInstanceAddresses_certified: UPeer " + peer_name + " is myself!"+isa_udp);
							isa_tcp = null;
						}
						if ((isa_udp == null) && (isa_tcp == null)) {
							if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_certified: Socket: null socks, continue");
							continue;
						}
						ps = new Connections_Peer_Socket();
						//if(DEBUG)System.out.println("Connection:loadAddresses: Socket: PS");
						ps.addr = new Address_SocketResolved(ia, isa_tcp, isa_udp, crt_adr);
						//if(DEBUG)System.out.println("Connection:loadAddresses: Socket: SAD");
						ps._last_contact = crt_adr.last_contact;
					}
					ps.address_ID = crt_adr.get_peer_address_ID();// Util.lval(item.get(QUERY_ADDR_ID), -1);
					next_conn_peer_sock_AL.add(ps);
					if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_certified: Socket: ps=\n"+ps);
				}
		}
	}
	/**
	 * Could have uncertified addresses coming from a server and not saved
	 * @param old_conn_peer_dirs_AL
	 * @param next_conn_peer_dirs_AL
	 * @param old_conn_peer_sock_AL
	 * @param next_conn_peer_sock_AL
	 */
	static void loadInstanceAddresses_uncertified(
			ArrayList<Connections_Peer_Directory> old_conn_peer_dirs_AL,
			ArrayList<Connections_Peer_Directory> next_conn_peer_dirs_AL,
			ArrayList<Connections_Peer_Socket> old_conn_peer_sock_AL,
			ArrayList<Connections_Peer_Socket> next_conn_peer_sock_AL
			) {
		if (DEBUG) System.out.println("******\nConnection: loadInstanceAddresses_uncertified: try old");
		try {
			if (old_conn_peer_sock_AL != null) {
				if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_uncertified: Socket: old peer_socks #"+old_conn_peer_sock_AL.size());
				for (int c = old_conn_peer_sock_AL.size() - 1; c >= 0; c --) {
					Connections_Peer_Socket tmpsock = old_conn_peer_sock_AL.get(c);
				
					if (tmpsock.address_ID > 0) {
						if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_uncertified: Socket: fix old peer_sock #"+tmpsock);
						continue;
					}
					if ((tmpsock.contacted_since_start_TCP && tmpsock.contacted_since_start_UDP) && (!tmpsock.last_contact_successful_TCP && !tmpsock.last_contact_successful_UDP)) {
						if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_uncertified: Socket: failed old peer_sock #"+tmpsock);
						continue;
					}
					/**
					 * Locate a new instance of tmpsock (received from Dir and not in the database)
					 * that could be now in the database, and use it (to have its local address_ID
					 */
					Connections_Peer_Socket crt = locatePS(next_conn_peer_sock_AL, tmpsock.getAddress());
					if (crt != null) {
						if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_uncertified: Socket: fixated peer_sock #"+tmpsock);
						continue;
					}
					if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_uncertified: Socket: imported peer_sock #"+tmpsock);
					next_conn_peer_sock_AL.add(0, tmpsock);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (DEBUG) System.out.println("***\nConnection: loadInstanceAddresses_uncertified: done");
	}
	/**
	 * Analyze my listing directories 
	 * (if table "directory_addresses" is empty, then try loading them from "application", APP_LISTING_DIRECTORIES, as active). 
	 *
	 * Adds those that are active (with initialized TCP SocketAddress "supernode_addr") to tmp_my_directories_AL: 
	 *
	 */
	private static void init_my_active_directories_listings() {
		if (DEBUG) System.out.println("Connections: init_my_active_directories_listings");
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
					if (listing_directories != null) {
						if (DEBUG) System.out.println("Connections: init_my_active_directories_listings: loading from application: " + listing_directories);
						DirectoryAddress.reset(listing_directories);
						dirs = DirectoryAddress.getActiveDirectoryAddresses();
					}
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
			if (DEBUG) System.out.println("Connections: init_my_active_directories_listings: quit on empty directories list");
			return;
    	}
		if (DEBUG) System.out.println("Connections: init_my_active_directories_listings: handling dirs #" + dirs.length);
     	for (int k = 0; k < dirs.length; k ++) {
     		try {
	    		//String[] d=dirs[k].split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_ELEM_SEP));
    			Address adr = new Address(dirs[k]);
    			if (!adr.active) {
    				if (DEBUG) System.out.println("Connections: init_my_active_directories_listings: skip inactive dir="+adr);
    				continue;
    			}
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
		if (DEBUG) System.out.println("Connections: init_my_active_directories_listings: done");
	}

	@Override
	public void update(ArrayList<String> _table, Hashtable<String, DBInfo> info) {
		if (DEBUG) System.out.println("Connections: update: "+Util.concat(_table, ":", "empty"));
		// if (_table.contains(table.application.TNAME)) {update_dirs=true;}
		if (_table.contains(table.directory_address.TNAME)) {update_dirs=true;}
		if (_table.contains(table.peer.TNAME) || _table.contains(table.peer_address.TNAME)){
			update_peers=true;}
		if (DEBUG) System.out.println("Connections: update: will wait_obj");
		synchronized (monitor_wait_obj) {
			if (DEBUG) System.out.println("Connections: update: got wait_obj");
			monitor_wait_obj.notifyAll();
			if (DEBUG) System.out.println("Connections: update: yield wait_obj");
		}
		if (DEBUG) System.out.println("Connections: update done");
	}
	/**
	 * called on timeout alarm or on database changes
	 */
	private void updates() {	
		boolean __DEBUG = Connections.DEBUG;
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
		} else
			if (__DEBUG) System.out.println("Connections: no updates dirs");
		if (update_peers) {
			if (__DEBUG) System.out.println("Connections: updates peers");
			update_peers = false;
			Connections.allocate_peers();
			Connections.init_used_peers();
			Connections.switch_tmp_peers();
		} else
			if (__DEBUG) System.out.println("Connections: no updates peers");
		/**
		 * The client cannot contact a peer and requests its re-evaluation
		 */
		if (update_needing_peer_connections.size() > 0) {
			if (__DEBUG) System.out.println("Connections: updates pc");
			Connection_Peer pc;
			if (__DEBUG) System.out.println("Connections: updates will wait_obj");
			synchronized (lock_update_needing_peer_connections) {
				if (__DEBUG) System.out.println("Connections: updates got wait_obj");
				pc = update_needing_peer_connections.remove(0);
				if (__DEBUG) System.out.println("Connections: updates yield wait_obj");
			}
			update_supernode_address(pc);
			if (__DEBUG) System.out.println("Connections: updates pc done");
		} else
			if (__DEBUG) System.out.println("Connections: updates pc not needed");
		
		if (DEBUG) System.out.println("Connections: updates done");
		if (__DEBUG) System.out.println("Connections: updates: ^^^^^^^^\n"+_toString()+"\n*******");
	}
	/**
	 * //synchronized  (protecting update_pc list)
	 * // only called while holding wait_obj (from _run)
	 * @return
	 */
	private static boolean updates_available() {
		synchronized (lock_update_needing_peer_connections) {
			return update_dirs || update_peers || (update_needing_peer_connections.size() != 0);
		}
	}
	/**
	 * To request the update of a given peer from directories
	 * add parameter "pc" to "update_needing_peer_connections" (and wake up _run).
	 * 
	 * It is called from Client2 (handleNotRecently...).
	 * @param pc
	 */
	public static void update_supernode_address_request(Connection_Peer pc) {
		if (DEBUG) out.println("Connections: update_supernode_address_request: will wait_obj");
		synchronized (lock_update_needing_peer_connections) {
			if (! update_needing_peer_connections.contains(pc)) {
				update_needing_peer_connections.add(pc);
				if (DEBUG) out.println("Connections: update_supernode_address_request: added");
			}
		}
		synchronized (monitor_wait_obj) {
			if (DEBUG) out.println("Connections: update_supernode_address_request: got wait_obj");
			monitor_wait_obj.notifyAll();
			if (DEBUG) out.println("Connections: update_supernode_address_request: yield wait_obj");
		}
		if (DEBUG) out.println("Connections: update_supernode_address_request: done");
	}
	public static class DirectoryRequestAnswer {
		public DirectoryRequest dr;
		public DirectoryAnswerMultipleIdentities da;
		DirectoryRequestAnswer(DirectoryRequest _dr, DirectoryAnswerMultipleIdentities _da) {
			dr = _dr;
			da = _da;
		}
	}
	/**
	 * The actual TCP code for requiring an address from a directory
	 * @param global_peer_ID
	 * @param peer_ID
	 * @param sock_addr
	 * @param dir_address
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	public static DirectoryRequestAnswer requestDirectoryAnswerByTCP (
			String global_peer_ID, String peer_ID, 
			InetSocketAddress sock_addr, Address dir_address)
					throws IOException, Exception {
		//boolean DEBUG = true;
		if (DEBUG) out.println("Connections: requestDirectoryAnswer: pLID="+peer_ID+" from:"+dir_address.toLongString()+" GIDH="+D_Peer.getGIDHashFromGID(global_peer_ID));
		Socket socket = new Socket();
		DirectoryRequest dr = null;
		DirectoryAnswerMultipleIdentities da = null;
		//try {
			socket.connect(sock_addr, Server.TIMEOUT_Client_wait_Dir);
			if (DEBUG) out.println("Connections: requestDirectoryAnswer:  Sending to Directory Server: connected:"+sock_addr);
			if (DEBUG) out.println("Connections: requestDirectoryAnswer:  Sending to Directory Server: connected:"+dir_address);
			//System.out.println("-----------------------------------------Identity.current_peer_ID.instance= "+Identity.current_peer_ID.instance);
			dr = new DirectoryRequest(
					global_peer_ID,
					Identity.getMyPeerGID(),
					Identity.current_peer_ID.peerInstance,
					Identity.udp_server_port, 
					peer_ID,
					dir_address);
			if (DEBUG) out.println("Connections: requestDirectoryAnswer: sending:"+dr);
			byte[] msg = dr.encode();
			if (DEBUG) {
				Decoder d = new Decoder(msg);
				DirectoryRequest _dr = new DirectoryRequest(d);
				if (DEBUG) out.println("Connections: requestDirectoryAnswer: actually sent:"+_dr);
			}
			socket.setSoTimeout(Server.TIMEOUT_Client_wait_Dir);
			socket.getOutputStream().write(msg);
			if (DEBUG) out.println("Connections: requestDirectoryAnswer:  Sending to Directory Server: "+Util.byteToHexDump(msg, " ")+dr);
			da = new DirectoryAnswerMultipleIdentities(socket.getInputStream());
			if (DEBUG) out.println("Connections: requestDirectoryAnswer:  Receiving DirectoryAnswerMultipleIdentities: "+da);
			socket.close();
			return new DirectoryRequestAnswer(dr, da);
		//} catch (Exception e) {e.printStackTrace();}
		//return new DirectoryRequestAnswer(dr, da);
	}
	/**
	 * Go to the next level of negotiating terms, updating request dr based on da.
	 * 
	 * Ideally here we would immediately resend a new request with the updated offer, but it is not yet implemented.
	 * TODO
	 * 
	 * @param dr
	 * @param da
	 * @param global_peer_ID
	 * @param peer_ID
	 * @param dir_address
	 * @return
	 *   Returns true if negotiation ended and no new round is needed. Currently always returns true.
	 */
	public static boolean updateNegotiationTerms (DirectoryRequest dr, DirectoryAnswerMultipleIdentities da, String global_peer_ID, String peer_ID, Address dir_address) {
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
		}
		return true;
	}
	/**
	 * Get addresses by instance
	 * @param dr
	 * @param da
	 * @param peerc_name
	 * @return
	 */
	public static Hashtable<String, ArrayList<Address>> extractAddresses (DirectoryRequest dr, DirectoryAnswerMultipleIdentities da,
			String peerc_name) {
		Hashtable<String,ArrayList<Address>> addresses = new Hashtable<String,ArrayList<Address>>();
		for ( DirectoryAnswerInstance inst : da.instances) {
			if (inst.addresses == null) {
				if (_DEBUG) out.println("Connections: getDirAddress:  Got empty addresses!");
				continue;
				//socket.close();
				//return null;
			}
			if (inst.addresses.size() == 0) {
				if(DEBUG) out.println("Connections: getDirAddress:  Got no addresses! da="+da+" for:"+peerc_name);
				continue;
				//socket.close();
				//return null;
			}
			String _inst = Util.getStringNonNullUnique(inst.instance);
			addresses.put(_inst, inst.addresses); // da.addresses
			if (DEBUG) out.println("Connections: getDirAddress: Dir Answer: "+da);
			//socket.close();
		}
		return addresses;
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
	static Hashtable<String,ArrayList<Address>> getDirAddress (
			Connection_Peer _pc,
			Connections_Peer_Directory pd, 
			Address dir_address,
			String global_peer_ID,
			String peer_name,
			String peer_ID) {
		boolean DEBUG = Connections.DEBUG || DD.DEBUG_COMMUNICATION_ADDRESSES;
		if (DEBUG) out.println("Connections: getDirAddress: "+dir_address.toLongString()+" p_name="+peer_name
				+" GIDH="+D_Peer.getGIDHashFromGID(global_peer_ID) + " ID="+Util.trimmed(global_peer_ID));
		Address ad = pd.supernode_addr.getAddressSupernode();
		InetSocketAddress sock_addr = null;
		
		// test again if directory IP is reachable
		if (pd.supernode_addr.isa == null) {
			InetAddress ia = Util.getHostIA(ad.domain);
			if (ia == null) {
				if (DEBUG) out.println("Connections: trouble finding directory server, null socket");
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
		try {
			for (;;) {
				if (DEBUG) out.println("Connections: getDirAddress: new negotiation loop");
				DirectoryRequestAnswer dra = requestDirectoryAnswerByTCP(global_peer_ID, peer_ID, sock_addr, dir_address);
				// Reporting to the widget in Directories Widget
				ClientSync.reportDa(dir_address.ipPort(), global_peer_ID, peer_name, dra.da, null);
				
				/**
				 * Go to the next level of negotiating terms, updating request dr based on da.
				 * Currently not yet implemented, but just updating the dr terms.
				 * However, a loop has to be added with the new negotiation. Currently function returns always true;
				 */
				if (! updateNegotiationTerms(dra.dr, dra.da, global_peer_ID, peer_ID, dir_address)) {
					if (DEBUG) out.println("Connections: getDirAddress: ! updating Negotition terms");
					continue;
				}
			
				Hashtable<String,ArrayList<Address>> addresses = extractAddresses(dra.dr, dra.da, _pc.getName());
				if (DEBUG) out.println("Connections: getDirAddress: returning result: "+addresses);
				return addresses;
			}
			//InetSocketAddress s= da.address.get(0);
			//return s.getHostName()+":"+s.getPort();
		} catch (IOException e) {
			if (DEBUG) out.println("Connections: getDirAddress: IO fail: "+e+" peer: "+peer_name+" DIR addr="+dir_address);
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
	 * Creates and sends by UDP a request for GID from dir_address listening on sock_addr
	 * @param directory_sock_addr
	 * @param dir_address
	 * @param GID
	 * @param peer_name
	 * @param peer_ID
	 * @return
	 */
	public static DirectoryRequest getDirAddressUDP(
			InetSocketAddress directory_sock_addr,
			Address dir_address,
			String GID,
			String peer_name, 
			String peer_ID
			) {
		boolean DEBUG = Connections.DEBUG || DD.DEBUG_COMMUNICATION_ADDRESSES;
		if (DEBUG) System.out.println("Directories:askAddressUDP: enter dir_address = "+dir_address);
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
			if (DEBUG) System.out.println("Directories:askAddressUDP: actually sent (decoded) request "+drt);
		}
		try {
			DatagramPacket dp = new DatagramPacket(msg, msg.length, directory_sock_addr);
			UDPServer.getUDPSocket().send(dp);
		} catch (IOException e) {
			if (DEBUG) e.printStackTrace();
		}
		return dr;
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
		boolean DEBUG = Connections.DEBUG || DD.DEBUG_COMMUNICATION_ADDRESSES;
		if (DEBUG) System.out.println("Directories: getDirAddressUDP: enter dir_address = "+dir_address);
		if ((Application.aus == null) || (UDPServer.getUDPSocket() == null)) {
			if (DEBUG) System.out.println("Directories: getDirAddressUDP: exit no server");
			return null;
		}
		InetSocketAddress directory_sock_addr = pd.supernode_addr.isa_udp;
		//int udp_port = dir_address.udp_port;
		//if (udp_port <= 0) Util.printCallPath("udp_port="+dir_address.toLongString()); 
		//if (udp_port <= 0) udp_port = dir_address.getTCPPort();
		//if (udp_port <= 0) return null; 
		String GIDH = _pc.getGIDH();

		DirectoryRequest dr = getDirAddressUDP(directory_sock_addr, dir_address, GID, peer_name, peer_ID);
		if (DEBUG) System.out.println("Directories: getDirAddressUDP: sent "+dr);
		
		return Connections.getKnownDirectoryAddresses(dir_address, GIDH, Identity.getMyPeerInstance());
	}
	/**
	 * The peer contact has to be reevaluated
	 * @param _pc
	 */
	private static void update_supernode_address(Connection_Peer _pc) {
		boolean DEBUG = Connections.DEBUG || DD.DEBUG_COMMUNICATION_ADDRESSES;
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
		if (DEBUG) out.println("Connections: update_supernode_address: got pc="+_pc);
	}
	/**
	 * update a given instance's addresses, from all its supernodes
	 * @param _pc
	 * @param dpi
	 * @param peer_key
	 * @param crt_peer_directories
	 * @param crt_peer_sockets
	 */
	private static void update_supernode_address_instance(Connection_Peer _pc, Connection_Instance dpi, String peer_key,
			ArrayList<Connections_Peer_Directory> crt_peer_directories, ArrayList<Connections_Peer_Socket> crt_peer_sockets) {
		boolean DEBUG = Connections.DEBUG || DD.DEBUG_COMMUNICATION_ADDRESSES;
		if (DEBUG) out.println("Connections: update_supernode_address_instance: start dpi="+dpi);

		if (DEBUG) out.println("Connections: update_supernode_address_instance: try directories #" + crt_peer_directories.size());
		
		for (int k = 0; k < crt_peer_directories.size(); k ++) {
			update_supernode_address_instance_dir(_pc, dpi, peer_key, crt_peer_directories, crt_peer_sockets, 
					k, crt_peer_directories.get(k));
		}
	}
	private static void update_supernode_address_instance_dir(
			Connection_Peer _pc,
			Connection_Instance dpi,
			String peer_key,
			ArrayList<Connections_Peer_Directory> crt_peer_directories,
			ArrayList<Connections_Peer_Socket> crt_peer_sockets,
			int k,
			Connections_Peer_Directory pd
			) {
		boolean DEBUG = Connections.DEBUG || DD.DEBUG_COMMUNICATION_ADDRESSES;
		IClient This = Application.ac;
		if (DEBUG) out.println("Connections: update_supernode_address_instance_dir: start dp="+pd);
		
		Hashtable<String, Hashtable<String, Hashtable<String,String>>> _peer_contacts_for_this_peer = D_Peer.peer_contacts.get(peer_key);
		if (_peer_contacts_for_this_peer == null) {
			_peer_contacts_for_this_peer = new Hashtable<String, Hashtable<String, Hashtable<String,String>>>();
			D_Peer.peer_contacts.put(peer_key, _peer_contacts_for_this_peer);
		}
		
		Hashtable<String, Hashtable<String,String>> _peer_contacts_for_this_instance;
		String crt_inst = null;
		if (dpi != null && dpi.dpi != null) crt_inst = dpi.dpi.peer_instance;
		_peer_contacts_for_this_instance = _peer_contacts_for_this_peer.get(Util.getStringNonNullUnique(crt_inst));
		if (_peer_contacts_for_this_instance == null) {
			_peer_contacts_for_this_instance = new Hashtable<String, Hashtable<String,String>>();
			_peer_contacts_for_this_peer.put(Util.getStringNonNullUnique(crt_inst), _peer_contacts_for_this_instance);
		}
		
		String peer_ID = _pc.peer.getLIDstr();
		// ArrayList<Address> adr_addresses;
		Hashtable<String,ArrayList<Address>> adr_addresses; // <identity,addresses_list>
		String peer_name = _pc.getName();
		String global_peer_ID = _pc.getGID();
		//Connections_Peer_Directory pd = crt_peer_directories.get(k);
		
		if (DEBUG) out.println("Connections: update_supernode_address_instance_dir: try dir" + pd);
		Address address_supernode = pd.supernode_addr.getAddressSupernode();
		String type = address_supernode.pure_protocol;
		String now = Util.getGeneralizedTime();
		if (type == null) type = Address.DIR;
		//String s_address = ad.toString();
		String s_addr_ip = address_supernode.ipPort();
		String old_address = s_addr_ip;
		DD.ed.fireClientUpdate(new CommEvent(This, address_supernode.ipPort(), null, "DIR REQUEST", peer_name+" ("+global_peer_ID+")"));
		if (DEBUG) out.println("Connections: update_supernode_address_instance_dir:"+k+" will getDir");
		// can be slow
		// adr_addresses = Client.getDirAddress(s_address, global_peer_ID, peer_name, peer_ID);
		adr_addresses = Connections.getDirAddress(_pc, pd, address_supernode, global_peer_ID, peer_name, peer_ID);
		if (adr_addresses == null) {
			if (DEBUG) out.println("Connections: update_supernode_address_instance_dir:"+k+" did getDir: null");
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
				
			if (DEBUG) out.println("Connections: update_supernode_address_instance_dir:"+k+" DIR returns empty");
			//return false;
			return;
		} else {
			if (DEBUG) out.println("Connections: update_supernode_address_instance_dir:"+k+" did getDir: non"
					+ "-null");
				
			if (DEBUG) {
				System.out.println("\n\nGetting:");
				for (String key  : adr_addresses.keySet()) {
					System.out.println("Inst: \""+key+"\" :"+Util.concat(adr_addresses.get(key), " --- ", "NULL"));
				}
			}
		}
		
		// tell of "no contact" if this instance is not in received addresses
		if (adr_addresses.get(Util.getStringNonNullUnique(crt_inst)) == null) {
			ClientSync.peerContactAdd(null, type, s_addr_ip, now, _peer_contacts_for_this_instance);
		}
			
		// add addresses to widget PeerContacts displayed with Peers/Safes
		for (String _inst_nonull : adr_addresses.keySet()) {
			Hashtable<String, ArrayList<Address>> _adr_addresses = new Hashtable<String, ArrayList<Address>>();
			_adr_addresses.put(_inst_nonull, adr_addresses.get(_inst_nonull));
			// treating separately the other (unknown? instances)
			// probably they need not be treated separately
			if (! Util.equalStrings_null_or_not(Util.getStringNonNullUnique(crt_inst), _inst_nonull)) {
				if (DEBUG) System.out.println("Connections: update_supernode_address_instance_dir: dropping inst:"+_inst_nonull+" -> "+Util.concat(adr_addresses.get(_inst_nonull), "---", ""));

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

					
				if (DEBUG) Util.printPeerContacts("Connections: update_supernode_address_instance_dir: after diff:"); 
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
			
		synchronized(Connections.monitor_integrateDirAddresses) {
				
			if (DEBUG) System.out.println("****\nConnections: update_supernode_address_instance_dir: add\n" + Util.concat("\t", adr_addresses, ",", "NULL")); 
				// add addresses and instances to _pc
				integrateDirAddresses(_pc, pd, adr_addresses, //global_peer_ID, type, s_addr_ip, peer_key, now, peer_contacts_for_this_peer,
						crt_peer_directories, crt_peer_sockets);
				if (DEBUG) System.out.println("Connections: update_supernode_address_instance_dir: after integrate: "); 
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
			
		if (DEBUG) out.println("Connections: update_supernode_address_instance_dir: DIR obtained address: "+s_addr_ip);
		DD.ed.fireClientUpdate(new CommEvent(This, old_address, null,"DIR ANSWER", peer_name+" ("+s_addr_ip+")"));
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
					//pd.reported_peer_addr_ = reported_peer_addr;
					pd._reported_peer_addr.put(_inst, reported_peer_addr); // pd.reported_peer_addr_);
					if (DEBUG) System.out.println("Connections: integrateDirAddresses: got= "+pd._reported_peer_addr);
					continue;
				}
				if ((ad.pure_protocol == null) || Address.SOCKET.equals(ad.pure_protocol)) {
					Connections_Peer_Socket _ps = locatePS(peer_sockets_list, ad);
					if (_ps != null) {
						if(DEBUG) System.out.println("Connections: integrateDirAddresses: got existing= "+_ps);
						continue;
					}
					InetAddress ia = Util.getHostIA(ad.domain);
					if (ia == null) {
						if (DEBUG) System.out.println("Connections: integrateDirAddresses: null ia= "+ad);
						continue;
					}
					Connections_Peer_Socket ps = new Connections_Peer_Socket(ad, ia);
					peer_sockets_list.add(ps);
					if (DEBUG) System.out.println("Connections: integrateDirAddresses: got sock= "+_ps);
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
			Address ad = pd.supernode_addr.getAddressSupernode();
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
		synchronized (lock_used_structures) {
			Connection_Peer pc = Connections.used_peers_HT_GID_CP.get(peer_GID);
			if (pc != null) {
				if (pc.shared_peer_sockets != null) {
					for (int k = 0; k < pc.shared_peer_sockets.size(); k ++) {
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
			
			for (int k = 0; k < pc.shared_peer_directories.size(); k ++) {
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
//				if (pd.reported_peer_addr_ != null) {
//					udp_sock_addresses.add(pd.reported_peer_addr_);
//					added = true;
//				}
				if (added) {
					peer_directories.add(pd.supernode_addr.getAddressSupernode().toString());
					peer_directories_sockets.add(pd.supernode_addr.isa);
				}
			}
		}
	}

//	public static void main(String args[]){
//		try {
//			Application.db = new DBInterface(Application.DELIBERATION_FILE);
//			if (DEBUG) System.out.println("Connection: main: Client2");
//			Client2 c2 = new Client2();
//			c2.start();
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
//	}
	/**
	 * Just concatenates the domain and the port with separator ":" to be used as keys in "personal_HT_IPPORT_PC"
	 * @param domain
	 * @param port
	 * @return
	 */
	public static String getIPPORT(String domain, int port) {
		return domain+":"+port;
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
			String ipport = getIPPORT(directory_domain, directory_udp_port);
			if (DEBUG) System.out.println("Connections: getKnownDirectoryAddresses: me "+ipport);
			Connections_Peer_Directory pd = Connections.myselfPeer_HT_IPPORT_CPD.get(ipport);
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
		String ipport = getIPPORT(directory_domain, directory_udp_port);
		D_Peer me = data.HandlingMyself_Peer.get_myself_with_wait();
		D_Peer target = D_Peer.getPeerByGID_or_GIDhash(null, dami.remote_GIDhash, true, false, false, null);
		ClientSync.reportDa(ipport+"/UDP", target.getGID(), target.getName(), dami, null);
		Connections_Peer_Directory pd;
		
		Hashtable<String,ArrayList<Address>> adr = new Hashtable<String,ArrayList<Address>> ();
		for (DirectoryAnswerInstance inst : dami.instances) { 
			if (DEBUG) System.out.println("Connections: registerIncomingDirectoryAnswer: handling "+inst);
			if ((Util.equalStrings_null_or_not(inst.instance, me.getInstance())) && Util.equalStrings_null_or_not(dami.remote_GIDhash, me.getGIDH_force())) {
				if (DEBUG) System.out.println("Connections: registerIncomingDirectoryAnswer: me "+ipport);
				Connections_Peer_Directory _pd = Connections.myselfPeer_HT_IPPORT_CPD.get(ipport);
				if (_pd == null) {
					_pd = new Connections_Peer_Directory();
					Connections.myselfPeer_HT_IPPORT_CPD.put(ipport, _pd);
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

			/**
			 * The synchronization added but its need was not yet tested
			 */
			synchronized(Connections.monitor_integrateDirAddresses) {
				integrateDirAddresses(_pc[0], pd, adr, pi.peer_directories, pi.peer_sockets);
			}
		}	
	}
}
