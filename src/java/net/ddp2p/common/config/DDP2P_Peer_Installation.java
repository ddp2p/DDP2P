package net.ddp2p.common.config;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Peer_SaverThread;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.hds.DirectoryServer;
import net.ddp2p.common.hds.IClient;
import net.ddp2p.common.hds.Server;
import net.ddp2p.common.hds.UDPServer;
import net.ddp2p.common.network.NATServer;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList;
import net.ddp2p.common.wireless.BroadcastClient;
import net.ddp2p.common.wireless.BroadcastServer;

public 
class DDP2P_Peer_Installation {

	private DBInterface db;
	private DBInterface db_dir;

	public  DirectoryServer g_DirectoryServer;
	public  Server g_TCPServer;
	public  UDPServer g_UDPServer; //have to unify with the one in DD
	public  NATServer g_NATServer;
	public  BroadcastServer g_BroadcastServer = null; // reference to the unique BroadcastServer
	public  BroadcastClient g_BroadcastClient = null; // reference to the unique BroadcastClient
	public  IClient g_PollingStreamingClient;
	/* the first directory that was online most recent */
	public int preferred_directory_idx = 0;
	/** Storing */
	D_Peer_SaverThread d_peer__saverThread = new D_Peer_SaverThread();
	
	/** D_Peer Cache */
	public long d_peer_current_space = 0;
	public Hashtable<String, D_Peer> d_peer_loaded_By_GIDhash = new Hashtable<String, D_Peer>();
	public Hashtable<String, D_Peer> d_peer_loaded_By_GID = new Hashtable<String, D_Peer>();
	public Hashtable<Long, D_Peer> d_peer_loaded_By_LocalID = new Hashtable<Long, D_Peer>();
	/** Currently loaded peers, ordered by the access time*/
	public DDP2P_DoubleLinkedList<D_Peer> d_peer_loaded_objects = new DDP2P_DoubleLinkedList<D_Peer>();

	// handling myself
	static D_Peer _myself = null;
	
	
	static boolean listing_directories_loaded = false;
	static ArrayList<InetSocketAddress> listing_directories_inet = new ArrayList<InetSocketAddress>();
	static ArrayList<Address> listing_directories_addr = new ArrayList<Address>();
	/* The list of directories as strings prot:IP:port */
	static ArrayList<String> listing_directories_string = new ArrayList<String>();
	//public static ArrayList<InetSocketAddress> query_directories = new ArrayList<InetSocketAddress>();
	/* The list of directories as InetSocketAddressed */
	static Calendar current_identity_creation_date;// = Util.CalendargetInstance();
	/* Peer Identity from the DD database registry */
	static Identity current_peer_ID = null;
	/* Default user Identity (with constituent and language) from the identities table */
	static Identity current_identity = null;
	static ArrayList<InetAddress> my_server_domains_loopback = new ArrayList<InetAddress>();
	// Lists with detected local domains for interfaces
	static ArrayList<InetAddress> my_server_domains = new ArrayList<InetAddress>();
	static int udp_server_port = -1; // the port where the udp is listening
	static int port = -1;		// the port where the peer is listening
	
	public DBInterface getDB() {
		return db;
	}
	public void setDB(DBInterface db) {
		this.db = db;
	}
	public DBInterface getDB_Dir() {
		return db_dir;
	}
	public void setDB_Dir(DBInterface db_dir) {
		this.db_dir = db_dir;
	}
}