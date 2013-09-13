package hds;

import static java.lang.System.out;
import static util.Util._;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.regex.Pattern;

import ASN1.Encoder;

import config.Application;
import config.DD;
import config.Identity;
import data.D_PeerAddress;

import util.CommEvent;
import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.DBSelector;
import util.P2PDDSQLException;
import util.Util;
//import util.DBSelector;
import widgets.peers.PeerContacts;

class SocketAddresses_Domain{
	Address ad;
	InetSocketAddress isa_tcp, isa_udp;
	InetAddress ia;
	
	SocketAddresses_Domain(InetAddress _ia, InetSocketAddress _isa_tcp, InetSocketAddress _isa_udp, Address _ad){
		ad = _ad;
		ia = _ia;
		isa_tcp = _isa_tcp;
		isa_udp = _isa_udp;
	}
	public String toString() {
		return "[SockAddr_D: ad="+ad+" ia="+ia+" tcp="+isa_tcp+" udp="+isa_udp+"]";
	}
}
class Peer_Directory{
	SocketAddress_Domain supernode_addr;
	SocketAddress_Domain reported_peer_addr;
	Calendar last_contact;
	String _last_contact;
	boolean contacted_since_start = false;
	boolean last_contact_successful = false;
	// conditions negotiated
	public long address_ID;
	public Peer_Directory() {}
	public Peer_Directory(Address ad, InetAddress ia) {
		InetSocketAddress isa = new InetSocketAddress(ia, ad.tcp_port);
		supernode_addr = new SocketAddress_Domain(isa, ad);
		address_ID = -1;
	}
	public String toString() {
		return "[Peer_Directory: ID="+address_ID+" dir="+supernode_addr+" reported="+reported_peer_addr+"]";
	}
}
class My_Directory{
	SocketAddress_Domain supernode_addr;
	SocketAddress_Domain reported_my_addr;
	Calendar last_contact;
	String _last_contact;
	boolean contacted_since_start = false;
	boolean last_contact_successful = false;
	public String toString() {
		return "[My_Dir: super="+supernode_addr+" reported="+reported_my_addr+"]";
	}
}
class Peer_Socket{
	SocketAddresses_Domain addr;
	long address_ID;
	boolean behind_NAT = false; // always false (otherwise keep in Peer_Directory)
	Calendar last_contact;
	String _last_contact;
	boolean contacted_since_start_TCP = false;
	boolean last_contact_successful_TCP = false;
	boolean contacted_since_start_UDP = false;
	boolean last_contact_successful_UDP = false;
	Socket tcp_connection_open = null; // non-null if a TCP connection is open (synchronized)
	boolean tcp_connection_open_busy = false; // is some thread using this connection?
	Address ad;
	public Peer_Socket(Address ad2) {ad = ad2;}
	public Peer_Socket(Address ad, InetAddress ia) {
		this.ad = ad;
		InetSocketAddress isa_t = null, isa_u = null;
		if(ad.tcp_port > 0) isa_t = new InetSocketAddress(ia, ad.tcp_port);
		if(ad.udp_port > 0) isa_u = new InetSocketAddress(ia, ad.udp_port);
		addr = new SocketAddresses_Domain(ia, isa_t, isa_u, ad);
		address_ID = -1;
	}
	public String toString() {
		return "[Peer_Socket: ID="+address_ID+" addr="+addr+" date="+_last_contact+"]";
	}
	public Address getAddress() {
		return ad;
	}
}
class Peer_Connection {
	public ArrayList<Peer_Directory> peer_directories; //order by last_contact!
	public ArrayList<Peer_Socket> peer_sockets; // static addresses, order by contact!
	String GID;
	long ID;
	String name;
	boolean filtered;
	public Calendar last_sync_date;
	boolean contacted_since_start = false;
	boolean last_contact_successful = false;
	public D_PeerAddress peer;
	Peer_Connection(){
		peer_directories = new ArrayList<Peer_Directory>();
		peer_sockets = new ArrayList<Peer_Socket>();
	}
	public String toString() {
		return "[Peer_Connection: ID="+ID+" name="+name+" date="+Encoder.getGeneralizedTime(last_sync_date)+
				" contact="+contacted_since_start+
				" success="+last_contact_successful+
				"\n dirs=\n  " + Util.concat(peer_directories, "\n  ", "empty")+
				"\n socks=\n  " + Util.concat(peer_sockets, "\n  ", "empty")+
				"]";
	}
}
/**
 * For a new thread.
 * @author M. Silaghi
 *
 */

public class Connections extends Thread implements DBListener{
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	static public Hashtable<String,Peer_Connection> used_peers;
	static public ArrayList<Peer_Connection> _used_peers;
	static public ArrayList<My_Directory> my_directories;
	
	static private Hashtable<String,Peer_Connection> tmp_used_peers;
	static private ArrayList<Peer_Connection> _tmp_used_peers;
	static private ArrayList<My_Directory> tmp_my_directories;
	static public int peersAvailable = 0;
	private final static Object wait_obj = new Object();
	private final static Object lock_update_pc = new Object(); // hold during wait_obj
	private final static Object lock_used_structures = new Object();
	private static boolean update_dirs;
	private static boolean update_peers;
	private static ArrayList<Peer_Connection> update_pc = new ArrayList<Peer_Connection>();
	private static int tmp_peersAvailable;
	
	public static String _toString() {
		return "[Connections: #"+peersAvailable+
				" \npeers=\n "+Util.concat(_used_peers, "\n ", "empty")+
				" \ntmp_peers=\n "+Util.concat(_tmp_used_peers, "\n ", "empty")+
				" \nmy_dirs=\n "+Util.concat(my_directories, "\n ","empty")+
				" \ntmp_my_dirs=\n "+Util.concat(tmp_my_directories, "\n ","empty")+"]";
	}
	
	public Connections(DBInterface db){
		if(DEBUG) System.out.println("Connections: <init>");
		setDaemon(true);
		allocate();
		switch_tmp();
		db.addListener(this,
				new ArrayList<String>(Arrays.asList(table.peer.TNAME,
				table.peer_address.TNAME, table.application.TNAME)),
				DBSelector.getHashTable(table.application.TNAME, table.application.field,
						DD.APP_LISTING_DIRECTORIES)
				);
		start();
		if(DEBUG) System.out.println("Connections: <init> done");
	}
	/**
	 * synchronized used_structures
	 * @param crt
	 * @return
	 */
	public static Peer_Connection getConnectionAtIdx(int crt) {
		Peer_Connection result = null;
		if(DEBUG) System.out.println("Connections: getConnectionAtIdx: "+crt+"/"+peersAvailable);
		synchronized(lock_used_structures) {
			if(crt < 0) result = null;
			else if(crt >= peersAvailable) result = null;
				else result = _used_peers.get(crt);
		}
		if(DEBUG) System.out.println("Connections: getConnectionAtIdx: result="+result);
		return result;
	}

	private static void allocate_peers(){
		tmp_used_peers = new Hashtable<String,Peer_Connection>();
		_tmp_used_peers = new ArrayList<Peer_Connection>();
	}
	
	private static void allocate_dirs(){
		tmp_my_directories = new ArrayList<My_Directory>();
	}
	/**
	 * allocate temporary structures. Only called from this thread
	 */
	private static void allocate(){
		allocate_dirs(); allocate_peers();
	}
	
	private static void switch_tmp_peers(){
		synchronized(lock_used_structures){
			used_peers = tmp_used_peers;
			_used_peers = _tmp_used_peers;
			peersAvailable = _used_peers.size();
		}
		
		tmp_used_peers = null;
		_tmp_used_peers = null;
	}
	
	private static void switch_tmp_dirs(){
		my_directories = tmp_my_directories;
		tmp_my_directories = null;
	} 
	private static void switch_tmp(){
		synchronized(lock_used_structures){
			switch_tmp_peers(); switch_tmp_dirs();
		}
	}
	/**
	 * Creates peers in tmp_peers
	 */
	private static void init_peers(){
		if(DEBUG) System.out.println("Connections: init_peers");
		int QUERY_ID = 0;
		int QUERY_DATE = 1;
		int QUERY_NAME = 2;
		int QUERY_FILTERED = 3;
		//int QUERY_GID = 4;
		String peers_scan_sql =
				"SELECT "+
						table.peer.peer_ID+", "+
						table.peer.last_sync_date+", "+table.peer.name+", "+
						table.peer.filtered+//", "+table.peer.global_peer_ID+
				" FROM "+table.peer.TNAME+
				" WHERE "+table.peer.used+" = 1 " +
						" ORDER BY "+table.peer.last_sync_date+";";
		/*
		String peers_scan_sql = "SELECT " +
				table.peer.global_peer_ID+","+table.peer.peer_ID+","+
				table.peer.last_sync_date +
				table.peer_address.address+","+table.peer_address.type+","+
				" FROM "+table.peer.TNAME +
				" JOIN "+table.peer_address.TNAME+" ON "+table.peer.peer_ID+"="+table.peer_address.peer_ID +
				" WHERE "+table.peer.used +" = 1 " +
				" ORDER BY "+table.peer_address.my_last_connection+";";
		*/
		ArrayList<ArrayList<Object>> peers;
		try {
			peers = Application.db.select(peers_scan_sql,
					new String[]{});
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
			return;
		}
		tmp_peersAvailable = peers.size();
		if(DEBUG) System.out.println("Connections: init_peers: peers #="+tmp_peersAvailable);
		for(int k=0; k<tmp_peersAvailable; k++) {
			Peer_Connection p = new Peer_Connection();
			//p.GID = Util.getString(peers.get(k).get(QUERY_GID));
			p.ID = Util.lval(peers.get(k).get(QUERY_ID), -1);
			if(DEBUG) System.out.println("Connections: init_peers: peer #="+p.ID);
			p.name = Util.getString(peers.get(k).get(QUERY_NAME));
			p.filtered = Util.stringInt2bool(peers.get(k).get(QUERY_FILTERED), false);
			p.last_sync_date = Util.getCalendar(Util.getString(peers.get(k).get(QUERY_DATE)));
			loadAddresses(p);
			_tmp_used_peers.add(p);
			tmp_used_peers.put(p.GID, p);
		}
		if(DEBUG) System.out.println("Connections: init_peers: got="+_toString());
	}
	/**
	 * Loads the addresses of Peer_Connection p in tmp_peers.
	 * @param p
	 */
	private static void loadAddresses(Peer_Connection p) {
		if(DEBUG) System.out.println("Connections: loadAddresses:"+p);
		int QUERY_ADDR = 0;
		int QUERY_TYPE = 1;
		int QUERY_ADDR_ID = 2;
		int QUERY_LAST_CONN = 3;
		
		
		D_PeerAddress peer = null;  // not yet used when saving new served orgs
		if(DEBUG)System.out.println("Connection:loadAddresses: peer: "+p.name);
		try { // addresses are found below, ordered by last connection
			peer = new D_PeerAddress(p.ID, false, true, true);
			p.peer = peer;
			p.GID = peer.globalID;
		} catch (P2PDDSQLException e2) {
			e2.printStackTrace();
		}
		if(DEBUG)System.out.println("Connection:loadAddresses: peer: loaded D_PeerAddresses");
		
		Peer_Connection _p = used_peers.get(p.GID);
		
		ArrayList<ArrayList<Object>> peers_addr = null;
		try{
			String query = "SELECT "+
					table.peer_address.address+", "+table.peer_address.type+", "+
					table.peer_address.peer_address_ID+", "+table.peer_address.my_last_connection+
					" FROM "+table.peer_address.TNAME+
					" WHERE "+table.peer_address.peer_ID+" = ? " +
					" ORDER BY "+table.peer_address.my_last_connection+" DESC;";
			peers_addr = Application.db.select(query,
					new String[]{peer.peer_ID});
			
		} catch (P2PDDSQLException e1) {
			Application.warning(_("Database: ")+e1, _("Database"));
			if(DEBUG) e1.printStackTrace();
			return;
		}
		if(DEBUG)System.out.println("Connection:loadAddresses: peer: loaded addresses: #"+peers_addr.size());

		for(int a = 0; a<peers_addr.size(); a++) {
			if(DEBUG)System.out.println("Connection:loadAddresses: peer: loaded addresses: a="+a);
			ArrayList<Object> item = peers_addr.get(a);
			String type = Util.getString(item.get(QUERY_TYPE));
			if(Address.NAT.equals(type)) continue;
			String adr = Util.getString(item.get(QUERY_ADDR));
			//System.out.println("Connectionr:loadAddresses: Address: "+adr);
			Address ad = new Address(adr);
			String domain = ad.domain;
			
			if(Address.DIR.equals(type)){
				if(DEBUG)System.out.println("Connection:loadAddresses: DIR: locate");
				Peer_Directory pd = locatePD(_p, ad);
				if(DEBUG)System.out.println("Connection:loadAddresses: DIR: located");
				if(pd == null){

					/**
					 * New addresses
					 */
					InetSocketAddress isa_tcp = null;
					InetSocketAddress isa_udp = null;
					//if(DEBUG)System.out.println("Connection:loadAddresses: get nonblocking "+domain);
					InetAddress ia = Util.getNonBlockingHostIA(domain);
					//if(DEBUG)System.out.println("Connection:loadAddresses: got nonblocking "+ia);
					if(ia != null) {
						if(ad.tcp_port > 0) isa_tcp = new InetSocketAddress(ia, ad.tcp_port);
						if(ad.udp_port > 0) isa_udp = new InetSocketAddress(ia, ad.udp_port);
					}else{
						if(DEBUG)System.out.println("Connection:loadAddresses: Unknown host: "+adr);
						continue;
					}
					/**
					 * New Addresses retrieved
					 */
					
					pd = new Peer_Directory();
					pd.supernode_addr = new SocketAddress_Domain(isa_tcp, ad);
					pd._last_contact = Util.getString(item.get(QUERY_LAST_CONN));
					pd.last_contact = Util.getCalendar(pd._last_contact);
				}
				pd.address_ID = Util.lval(item.get(QUERY_ADDR_ID), -1);
				p.peer_directories.add(pd);
				if(DEBUG)System.out.println("Connection:loadAddresses: Directory: "+adr);
			}else
				if(Address.SOCKET.equals(type)){ // socket
					if(DEBUG)System.out.println("Connection:loadAddresses: Socket: locate");
					Peer_Socket ps = locatePS(_p, ad);
					if(DEBUG)System.out.println("Connection:loadAddresses: Socket: located");
					if(ps == null) {


						/**
						 * New addresses
						 */
						InetSocketAddress isa_tcp = null;
						InetSocketAddress isa_udp = null;
						//if(DEBUG)System.out.println("Connection:loadAddresses: get nonblocking "+domain);
						InetAddress ia = Util.getNonBlockingHostIA(domain);
						//if(DEBUG)System.out.println("Connection:loadAddresses: got nonblocking "+ia);
						if(ia != null) {
							if(ad.tcp_port > 0) isa_tcp = new InetSocketAddress(ia, ad.tcp_port);
							if(ad.udp_port > 0) isa_udp = new InetSocketAddress(ia, ad.udp_port);
						}else{
							if(DEBUG)System.out.println("Connection:loadAddresses: Unknown host: "+adr);
							continue;
						}
						/**
						 * New Addresses retrieved
						 */
						
						
						if(DEBUG)System.out.println("Connection:loadAddresses: Socket: myself");
						if((isa_udp!=null)&&
								ClientSync.isMyself(Identity.udp_server_port, isa_udp, ad)){
							if(DEBUG) out.println("Connections:loadAddresses: UPeer "+p.name+" is myself!"+isa_udp);
							isa_udp = null;
						}
						//if(DEBUG)System.out.println("Connection:loadAddresses: Socket: self udp");
						if((isa_tcp!=null)&&
								(Server.isMyself(isa_udp)||
										ClientSync.isMyself(Identity.port, isa_tcp, ad))){
							if(DEBUG) out.println("Connections:loadAddresses: UPeer "+p.name+" is myself!"+isa_udp);
							isa_tcp = null;
						}
						if((isa_udp==null) && (isa_tcp==null)) continue;
						ps = new Peer_Socket(ad);
						//ps.ad = ad;
						//if(DEBUG)System.out.println("Connection:loadAddresses: Socket: PS");
						ps.addr = new SocketAddresses_Domain(ia, isa_tcp, isa_udp, ad);
						//if(DEBUG)System.out.println("Connection:loadAddresses: Socket: SAD");
						ps._last_contact = Util.getString(item.get(QUERY_LAST_CONN));
						ps.last_contact = Util.getCalendar(ps._last_contact);
					}
					ps.address_ID = Util.lval(item.get(QUERY_ADDR_ID), -1);
					p.peer_sockets.add(ps);
					if(DEBUG)System.out.println("Connection:loadAddresses: Socket: "+adr);
				}
		}
		try{
			if(_p!=null) {
				if(DEBUG)System.out.println("Connection:loadAddresses: Socket: old peer_socks #"+_p.peer_sockets.size());
				for (int c = _p.peer_sockets.size()-1; c>=0; c--){
					Peer_Socket tmpsock = _p.peer_sockets.get(c);
				
					if(tmpsock.address_ID>0){
						if(DEBUG)System.out.println("Connection:loadAddresses: Socket: fix old peer_sock #"+tmpsock);
						continue;
					}
					/**
					 * Locate a new instance of tmpsock (received from Dir and not in the database)
					 * that could be now in the database, and use it (to have its local address_ID
					 */
					Peer_Socket crt = locatePS(p, tmpsock.getAddress());
					if(crt != null){
						if(DEBUG)System.out.println("Connection:loadAddresses: Socket: fixated peer_sock #"+tmpsock);
						continue;
					}
					if(DEBUG)System.out.println("Connection:loadAddresses: Socket: imported peer_sock #"+tmpsock);
					p.peer_sockets.add(0, tmpsock);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		if(DEBUG) System.out.println("Connections: loadAddresses: got: "+p);
	}
	// public static void init_myself(){}
	private static void init_directories(){
		if(DEBUG) System.out.println("Connections: init_directories");
    	String ld;
		try {
			ld = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
			return;
		}
    	if(ld == null){
    		if(! DD.WARNED_NO_DIRS) {
    			Application.warning(_("No listing_directories for connections found at initialization: " +
    					"\nDo not forget to add some later \n" +
    					"(e.g., from the DirectDemocracyP2P.net list)!\n" +
    					"If you have a stable IP, than you probably do not need it."), _("Configuration"));
    			DD.WARNED_NO_DIRS = true;
    		}
    		return;
    	}
    	String dirs[] = ld.split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_SEP));
    	for(int k=0; k<dirs.length; k++) {
    		try{
	    		String[] d=dirs[k].split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_ELEM_SEP));
	    		My_Directory md = new My_Directory();
	    		Address ad = new Address(dirs[k]);
	    		InetAddress ia = Util.getNonBlockingHostIA(d[0]);
	    		InetSocketAddress isa = new InetSocketAddress(ia, Integer.parseInt(d[1]));
	    		md.supernode_addr = new SocketAddress_Domain(isa, ad);
	    		Connections.tmp_my_directories.add(md);
    		}catch(Exception e) {
    			Application.warning(_("Error for "+dirs[k]+"\nError: "+e.getMessage()), _("Error installing directories"));
    			continue;
    		}
    	}
		if(DEBUG) System.out.println("Connections: init_directories: done");
	}
	
	private static void init(){
		if(DEBUG) System.out.println("Connections: init()");
		allocate();
		//init_myself();
		init_directories();
		init_peers();
		switch_tmp();
		if(DEBUG) System.out.println("Connections: init() done:"+_toString());
	}

	@Override
	public void update(ArrayList<String> _table, Hashtable<String, DBInfo> info) {
		if(DEBUG) System.out.println("Connections: update: "+Util.concat(_table, ":", "empty"));
		if (_table.contains(table.application.TNAME)) {update_dirs=true;}
		if(_table.contains(table.peer.TNAME) || _table.contains(table.peer_address.TNAME)){
			update_peers=true;}
		if(DEBUG) System.out.println("Connections: update: will wait_obj");
		synchronized(wait_obj) {
			if(DEBUG) System.out.println("Connections: update: got wait_obj");
			wait_obj.notifyAll();
			if(DEBUG) System.out.println("Connections: update: yield wait_obj");
		}
		if(DEBUG) System.out.println("Connections: update done");
	}
	/**
	 * called on timeout alarm or on database changes
	 */
	private void updates(){	
		if(DEBUG) System.out.println("Connections: updates: ***********");
		if(DEBUG) System.out.println("Connections: updates: start");
		/**
		 * database changes
		 */
		if (update_dirs) {
			if(DEBUG) System.out.println("Connections: updates dirs");
			update_dirs = false;
			Connections.allocate_dirs();
			Connections.init_directories();
			Connections.switch_tmp_dirs();
		}
		if(update_peers){
			if(DEBUG) System.out.println("Connections: updates peers");
			update_peers = false;
			Connections.allocate_peers();
			Connections.init_peers();
			Connections.switch_tmp_peers();
		}
		/**
		 * The client cannot contact a peer and requests its re-evaluation
		 */
		if(update_pc.size()>0){
			if(DEBUG) System.out.println("Connections: updates pc");
			Peer_Connection pc;
			if(DEBUG) System.out.println("Connections: updates will wait_obj");
			synchronized(lock_update_pc){
				if(DEBUG) System.out.println("Connections: updates got wait_obj");
				pc = update_pc.remove(0);
				if(DEBUG) System.out.println("Connections: updates yield wait_obj");
			}
			update_supernode_address(pc);
			if(DEBUG) System.out.println("Connections: updates pc done");
		}
		if(DEBUG) System.out.println("Connections: updates done");
		if(DEBUG) System.out.println("Connections: updates: ^^^^^^^^");
	}
	
	public void run(){
		DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Start"));
		try{_run();}catch(Exception e){}
		DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Will Stop"));
		if(DEBUG) out.println("Connections: run: turned Off");
	}
	/**
	 * Continuously tries to update, each time something changes
	 */
	private void _run(){
		init();
		for(;;){
			if(DEBUG) System.out.println("Connections: _run: will wait_obj");
			try{
				synchronized(wait_obj){
					if(DEBUG) System.out.println("Connections: _run: got wait_obj");
					if(!updates_available())
						wait_obj.wait(60*1000);
				}
				updates();
			}catch(InterruptedException e){
				if(_DEBUG) System.out.println("Connections: _run: interrupted");
				//e.printStackTrace();
			}catch(Exception e){
				if(_DEBUG) System.out.println("Connections: _run: continue");
				e.printStackTrace();
			}
			if(DEBUG) System.out.println("Connections: _run: yield wait_obj");
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
	 * called from client2
	 * @param pc
	 */
	public static void update_supernode_address_request(Peer_Connection pc) {
		if(DEBUG) out.println("Connections: update_supernode_address_request: will wait_obj");
		synchronized(lock_update_pc) {
			if(!update_pc.contains(pc)) {
				update_pc.add(pc);
				if(DEBUG) out.println("Connections: update_supernode_address_request: added");
			}
		}
		synchronized(wait_obj){
			if(DEBUG) out.println("Connections: update_supernode_address_request: got wait_obj");
			wait_obj.notifyAll();
			if(DEBUG) out.println("Connections: update_supernode_address_request: yield wait_obj");
		}
		if(DEBUG) out.println("Connections: update_supernode_address_request: done");
	}

	/**
	 * Get directory IP (if not yet found), and extract addresses from it
	 * Return null on failure
	 * @param _pc
	 * @param pd
	 * @param dir_address
	 * @param global_peer_ID
	 * @param peer_name
	 * @param peer_ID
	 * @return
	 */
	static ArrayList<Address> getDirAddress(Peer_Connection _pc, Peer_Directory pd, 
			String dir_address, String global_peer_ID, String peer_name, String peer_ID) {
		if(DEBUG) out.println("Connections: getDirAddress: "+dir_address+" ID="+Util.trimmed(global_peer_ID));
		Address ad = pd.supernode_addr.ad;
		InetSocketAddress sock_addr = null;
		
		// test again if directory IP is reachable
		if(pd.supernode_addr.isa == null){
			InetAddress ia = Util.getHostIA(ad.domain);
			if(ia == null){
				ClientSync.reportDa(dir_address, global_peer_ID, peer_name, null, _("Null Socket"));
				return null; // cannot find directory server
			}
			pd.supernode_addr.isa = sock_addr = new InetSocketAddress(ia, ad.tcp_port);// Client.getTCPSockAddress(dir_address);
		}else
			sock_addr = pd.supernode_addr.isa;
		if(sock_addr == null){
			if(DEBUG) out.println("Connections: getDirAddress: null dir address:  reportDa");
			ClientSync.reportDa(dir_address, global_peer_ID, peer_name, null, _("Null Socket"));
			return null; //"";
		}
		Socket socket = new Socket();
		try {
			socket.connect(sock_addr, Server.TIMEOUT_Client_wait_Dir);
			if(DEBUG) out.println("Connections: getDirAddress:  Sending to Directory Server: connected:"+sock_addr);
			DirectoryRequest dr = new DirectoryRequest(global_peer_ID,
					Identity.getMyPeerGID(),
					Identity.udp_server_port, 
					peer_ID,
					dir_address);
			byte[] msg = dr.encode();
			socket.setSoTimeout(Server.TIMEOUT_Client_wait_Dir);
			socket.getOutputStream().write(msg);
			if(DEBUG) out.println("Connections: getDirAddress:  Sending to Directory Server: "+Util.byteToHexDump(msg, " ")+dr);
			DirectoryAnswer da = new DirectoryAnswer(socket.getInputStream());
			ClientSync.reportDa(dir_address, global_peer_ID, peer_name, da, null);

			if((da.terms != null) && (da.terms.length != 0)) {
				dr.terms = dr.updateTerms(da.terms, peer_ID, global_peer_ID, dir_address, dr.terms);
				if(dr!=null){
					msg = dr.encode();
					socket.setSoTimeout(Server.TIMEOUT_Client_wait_Dir);
					socket.getOutputStream().write(msg);
					if(DEBUG) out.println("Connections: getDirAddress:  Sending to Directory Server: "+Util.byteToHexDump(msg, " ")+dr);
					da = new DirectoryAnswer(socket.getInputStream());
				}
			}
			
			if(da.addresses.size()==0){
				if(DEBUG) out.println("Connections: getDirAddress:  Got no addresses! da="+da+" for:"+_pc.name);
				socket.close();
				return null;
			}
			if(DEBUG) out.println("Connections: getDirAddress: Dir Answer: "+da);
			socket.close();
			if(da.addresses==null){
				if(_DEBUG) out.println("Connections: getDirAddress:  Got empty addresses!");
				return null;
			}
			return da.addresses;
			//InetSocketAddress s= da.address.get(0);
			//return s.getHostName()+":"+s.getPort();
		}catch (IOException e) {
			if(DEBUG) out.println("Connections: getDirAddress:  fail: "+e+" peer: "+peer_name+" DIR addr="+dir_address);
			ClientSync.reportDa(dir_address, global_peer_ID, peer_name, null, e.getLocalizedMessage());
			//e.printStackTrace();
			//Directories.setUDPOn(dir_address, new Boolean(false));
		} catch (Exception e) {
			if(DEBUG) out.println("Connections: getDirAddress:  fail: "+e+" peer: "+peer_name+" DIR addr="+dir_address);
			ClientSync.reportDa(dir_address, global_peer_ID, peer_name, null, e.getLocalizedMessage());
			e.printStackTrace();
		}
		//socket.close();
		if(DEBUG) out.println("Connections: getDirAddress fail");
		return null;
	}
	/**
	 * 
	 * @param _pc
	 */
	private static void update_supernode_address(Peer_Connection _pc) {
		if(DEBUG) out.println("Connections: update_supernode_address: **********");
		if(DEBUG) out.println("Connections: update_supernode_address: pc="+_pc);
		String peer_name = _pc.name;
		String peer_ID = _pc.peer.peer_ID;
		String global_peer_ID = _pc.GID;
		Object This = Application.ac;
		ArrayList<Address> adr_addresses;
		//ArrayList<SocketAddress_Domain> tcp_sock_addresses=new ArrayList<SocketAddress_Domain>();
		//ArrayList<SocketAddress_Domain> udp_sock_addresses=new ArrayList<SocketAddress_Domain>();

		String peer_key = peer_name;
		String _now = Util.getGeneralizedTime();
		if(DEBUG) out.println("Connections: update_supernode_address: now="+_now);
		if(peer_key==null) peer_key=Util.trimmed(global_peer_ID);
		Hashtable<String, Hashtable<String,String>> pc = PeerContacts.peer_contacts.get(peer_key);
		if(pc==null){
			pc = new Hashtable<String, Hashtable<String,String>>();
			PeerContacts.peer_contacts.put(peer_key, pc);
		}
		
		for(int k=0; k<_pc.peer_directories.size(); k++) {
			Peer_Directory pd = _pc.peer_directories.get(k);
			Address ad = pd.supernode_addr.ad;
			String type = ad.protocol;
			String now = Util.getGeneralizedTime();
			if(type==null) type = Address.DIR;
			String s_address = ad.toString();
			String old_address = s_address;
			DD.ed.fireClientUpdate(new CommEvent(This, s_address, null,"DIR REQUEST", peer_name+" ("+global_peer_ID+")"));
			if(DEBUG) out.println("Connections:update_supernode_address:"+k+" will getDir");
			// can be slow
			// adr_addresses = Client.getDirAddress(s_address, global_peer_ID, peer_name, peer_ID);
			adr_addresses = Connections.getDirAddress(_pc, pd, s_address, global_peer_ID, peer_name, peer_ID);
			if(adr_addresses == null){
				if(DEBUG) out.println("Connections:update_supernode_address:"+k+" did getDir: null");
				if(DEBUG) out.print(" ");
				
				String key = type+":"+s_address;
				Hashtable<String,String> value = pc.get(key);
				if(value==null){
					value = new Hashtable<String,String>();
					pc.put(key, value);
				}
				value.put("No contact", now);
				
				if(DEBUG) out.println("Connections:update_supernode_address:"+k+" DIR returns empty");
				//return false;
				continue;
			}else{
				if(DEBUG) out.println("Connections:update_supernode_address:"+k+" did getDir: non-null");
			}
			getSocketAddresses(//tcp_sock_addresses, udp_sock_addresses,
					adr_addresses,
					global_peer_ID, type, s_address, peer_key, now, pc);
			integrateDirAddresses(_pc, pd, adr_addresses, global_peer_ID, type, s_address, peer_key, now, pc);
			s_address=Util.concat(adr_addresses.toArray(), DirectoryServer.ADDR_SEP);
			
			if(DEBUG) out.println("Connections:update_supernode_address: DIR obtained address: "+s_address);
			DD.ed.fireClientUpdate(new CommEvent(This, old_address, null,"DIR ANSWER", peer_name+" ("+s_address+")"));
		}
	}

	private static void getSocketAddresses(
			// ArrayList<SocketAddress_Domain> tcp_sock_addresses,
			// ArrayList<SocketAddress_Domain> udp_sock_addresses,
			ArrayList<Address> adr_addresses,
			String global_peer_ID,
			String type,
			String s_address,
			String peer_key,
			String now,
			Hashtable<String, Hashtable<String,String>> pc
			) {
		boolean DEBUG = Connections.DEBUG || DD.DEBUG_PLUGIN;
		if(DEBUG) System.out.println("Connections:getSocketAddresses: start");
		if(adr_addresses ==null){
			if(DEBUG) System.out.println("Connections:getSocketAddresses: null addresses");
			return;
		}
		int sizes = adr_addresses.size();
		if(DEBUG) System.out.println("Connections:getSocketAddresses: addresses = "+sizes+" ["+Util.concat(adr_addresses, " ", "DEF")+" ]");
		for(int k=0;k<sizes;k++) {
			Address ad = adr_addresses.get(k);
			if(DEBUG) out.print(" "+k+"+"+ad+" ");
			ClientSync.add_to_peer_contacted_addresses(ad, global_peer_ID);
			
			if(pc!=null) {
				if(DEBUG) System.out.println("Connections:getSocketAddresses:  add to peer contact");
				ClientSync.peerContactAdd(ad, type, s_address, now, pc);
				if(DEBUG) out.println("Connections:getSocketAddresses: enum d_adr="+peer_key+":"+type+":"+s_address+" val="+ad+" "+now);
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
			if(DEBUG) System.out.println("Connections:getSocketAddresses: Done handling "+ad);
		}
		if(DEBUG) out.println("");
		if(DEBUG) System.out.println("Connections:getSocketAddresses: done");
	}

	
	/**
	 * Integrate addresses s_address of _pc received from directory pd into this pd
	 * @param _pc
	 * @param pd
	 * @param adr_addresses
	 * @param global_peer_ID
	 * @param type
	 * @param s_address
	 * @param peer_key
	 * @param now
	 * @param pc
	 */
	private static void integrateDirAddresses(Peer_Connection _pc,
			Peer_Directory pd, ArrayList<Address> adr_addresses, String global_peer_ID,
			String type, String s_address, String peer_key, String now,
			Hashtable<String, Hashtable<String, String>> pc) {
		if(adr_addresses==null){
			if(_DEBUG) System.out.println("Connections: integrateDirAddresses #null addresses");
			return;
		}
		if(DEBUG) System.out.println("Connections: integrateDirAddresses #"+adr_addresses.size()+" "+pd);
		for(int k=0; k<adr_addresses.size(); k++) {
			Address ad = adr_addresses.get(k);
			if(DEBUG) System.out.println("Connections: integrateDirAddresses:  ad= "+ad);
			if(Address.NAT.equals(ad.protocol)){
				InetSocketAddress isa = new InetSocketAddress(ad.domain, ad.udp_port);
				pd.reported_peer_addr = new SocketAddress_Domain(isa , ad);
				if(DEBUG) System.out.println("Connections: integrateDirAddresses: got= "+pd.reported_peer_addr);
				continue;
			}
			if(Address.DIR.equals(ad.protocol)){
				Peer_Directory _pd = locatePD(_pc, ad);
				if(_pd != null){
					if(DEBUG) System.out.println("Connections: integrateDirAddresses: preexisting= "+_pd);
					continue;
				}
				InetAddress ia = Util.getHostIA(ad.domain);
				if(ia == null){
					if(DEBUG) System.out.println("Connections: integrateDirAddresses: got= "+ad.domain);
					continue;
				}
				_pd = new Peer_Directory(ad, ia);
				_pc.peer_directories.add(_pd);
				if(DEBUG) System.out.println("Connections: integrateDirAddresses: got dir= "+_pd);
				continue;
			}
			if((ad.protocol == null) || Address.SOCKET.equals(ad.protocol)){
				Peer_Socket _ps = locatePS(_pc, ad);
				if(_ps != null){
					if(DEBUG) System.out.println("Connections: integrateDirAddresses: got existing= "+_ps);
					continue;
				}
				InetAddress ia = Util.getHostIA(ad.domain);
				if(ia == null){
					if(DEBUG) System.out.println("Connections: integrateDirAddresses: null ia= "+ad);
					continue;
				}
				Peer_Socket ps = new Peer_Socket(ad, ia);
				_pc.peer_sockets.add(ps);
				if(DEBUG) System.out.println("Connections: integrateDirAddresses: got sock= "+_ps);
				continue;
			}
			if(DEBUG) System.out.println("Connections: integrateDirAddresses: ad no protocol");
		}
		if(DEBUG) System.out.println("Connections: integrateDirAddresses: got "+pc);
	}
/**
 * May use a hashtable to retrieve faster the domains!
 * @param _pc
 * @param domain
 * @param tcp_port
 * @return
 */
	private static Peer_Socket locatePS(Peer_Connection _pc, Address _ad){
		//if(DEBUG) System.out.println("Connectione: locatePS: "+_ad);
		if(_pc == null){
			if(DEBUG) System.out.println("Connection: locatePS: null _pc for: "+_ad);
			return null;
		}
		String domain = _ad.domain;
		int tcp_port = _ad.tcp_port;
		int udp_port = _ad.udp_port;
		for(int k=0; k<_pc.peer_sockets.size(); k++){
			Peer_Socket ps = _pc.peer_sockets.get(k);
			Address ad = ps.addr.ad;
			if(ad.domain.equals(domain) && (ad.tcp_port == tcp_port) && (ad.udp_port == udp_port)) {
				if(DEBUG) System.out.println("Connection: locatePS: "+_ad+" got-> "+ps);
				return ps;
			}
		}
		if(DEBUG) System.out.println("Connection: locatePS: not found "+_ad);
		return null;
	}

	private static Peer_Directory locatePD(Peer_Connection _pc, Address _ad) {
		if(DEBUG) System.out.println("Connection: locatePD: "+_ad);
		if(_pc == null){
			if(DEBUG) System.out.println("Connection: locatePD: null _pc");
			return null;
		}
		String domain = _ad.domain;
		int tcp_port = _ad.tcp_port;
		int udp_port = _ad.udp_port;
		for(int k=0; k<_pc.peer_directories.size(); k++){
			Peer_Directory pd = _pc.peer_directories.get(k);
			Address ad = pd.supernode_addr.ad;
			if(ad.domain.equals(domain) && (ad.tcp_port == tcp_port) && (ad.udp_port == udp_port)) {
				if(DEBUG) System.out.println("Connection: locatePD: "+pd);
				return pd;
			}
		}
		if(DEBUG) System.out.println("Connection: locatePD: not found");
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
			ArrayList<SocketAddress_Domain> tcp_sock_addresses,
			ArrayList<SocketAddress_Domain> udp_sock_addresses,
			ArrayList<Address> pca, String peer_GID, 
			ArrayList<String> peer_directories, ArrayList<InetSocketAddress> peer_directories_sockets) {
		synchronized(lock_used_structures) {
			Peer_Connection pc = Connections.used_peers.get(peer_GID);
			for(int k=0; k<pc.peer_sockets.size(); k++){
				Peer_Socket ps = pc.peer_sockets.get(k);
				if(ps.addr.isa_tcp!=null) tcp_sock_addresses.add(SocketAddress_Domain.getTCP(ps.addr));
				if(ps.addr.isa_udp!=null) udp_sock_addresses.add(SocketAddress_Domain.getUDP(ps.addr));
			}
			for(int k=0; k<pc.peer_directories.size(); k++){
				Peer_Directory pd = pc.peer_directories.get(k);
				if(pd.reported_peer_addr != null) {
					udp_sock_addresses.add(pd.reported_peer_addr);
					peer_directories.add(pd.supernode_addr.ad.toString());
					peer_directories_sockets.add(pd.supernode_addr.isa);
				}
			}
		}
	}

	public static void main(String args[]){
		try {
			Application.db=new DBInterface(Application.DELIBERATION_FILE);
			if(DEBUG) System.out.println("Connection: main: Client2");
			Client2 c2 = new Client2();
			c2.start();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
}
