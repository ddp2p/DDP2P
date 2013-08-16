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
 package hds;
import static java.lang.System.out;
import static java.lang.System.err;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.regex.Pattern;


import streaming.RequestData;
import streaming.UpdateMessages;
import util.CommEvent;
import util.Util;
import widgets.peers.PeerContacts;
import widgets.peers.Peers;
import static util.Util._;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;


import util.P2PDDSQLException;

import config.Application;
import config.DD;
import config.Identity;
import data.D_PeerAddress;

public
class Client1 extends Thread implements IClient{
	static final long PAUSE = 300000;
	static final int MAX_BUFFER = 1000000;
	public static boolean recentlyTouched = false; // to avoid stopping
	public static int peersAvailable = 0;
	public static int peersToGo = 0;
	Socket client_socket=new Socket();
	public Client1() {
		try {
			ClientSync.buildStaticPayload();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		this.setDaemon(true);
		DD.ed.fireClientUpdate(new CommEvent(this,null,null,"LOCAL","Client Created"));
	}
	
	/**
	 * Add new items to advertisements
	 * this touches the client
	 * @param hash
	 * @param org_hash
	 * @param type
	 */
	public static void addToPayloadAdvertisements(String hash, String org_hash, int type) {
		RequestData target = null;
		for(RequestData a: ClientSync.payload_recent.rd) {
			if(org_hash.equals(a.global_organization_ID_hash)) {
				target = a;
			}
		}
		if(target == null) {
			target = new RequestData();
			ClientSync.payload_recent.rd.add(target);
		}
		
		if (target.addHashIfNewTo(hash, type, ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD))
			try {
				DD.touchClient();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
	}
	
	/**
	 * This function tried both TCP and UDP connections, based on the DD.ClientUDP and DD.ClientTCP
	 * @param s_address
	 * @param type
	 * @param global_peer_ID
	 * @param peer_name
	 * @param peer_directories 
	 * @param peer_directories_sockets 
	 * @return true if a TCP connection was successful
	 */
	boolean try_connect(String s_address, String type, String global_peer_ID, String peer_name,
				ArrayList<InetSocketAddress> peer_directories_udp_sockets, ArrayList<String> peer_directories, String peer_ID) {
		String old_address = s_address;
		if(ClientSync.DEBUG) out.println("Client:try_connect:1 start Try to connect to: "+ s_address+" ty="+type+" ID="+Util.trimmed(global_peer_ID));
		InetSocketAddress sock_addr;
		ArrayList<Address> adr_addresses;
		ArrayList<SocketAddress_Domain> tcp_sock_addresses=new ArrayList<SocketAddress_Domain>();
		ArrayList<SocketAddress_Domain> udp_sock_addresses=new ArrayList<SocketAddress_Domain>();
		
		String peer_key = peer_name;
		String now = Util.getGeneralizedTime();
		if(ClientSync.DEBUG) out.println("Client: now="+now);
		if(peer_key==null) peer_key=Util.trimmed(global_peer_ID);
		Hashtable<String, Hashtable<String,String>> pc = PeerContacts.peer_contacts.get(peer_key);
		if(pc==null){
			pc = new Hashtable<String, Hashtable<String,String>>();
			PeerContacts.peer_contacts.put(peer_key, pc);
		}
		
		if(ClientSync.DEBUG) out.println("Client:try_connect:1 handle");
		if(Address.DIR.equals(type)) {
			DD.ed.fireClientUpdate(new CommEvent(this, s_address,null,"DIR REQUEST", peer_name+" ("+global_peer_ID+")"));
			if(ClientSync.DEBUG) out.println("Client:try_connect:1 will getDir");
			// can be slow
			adr_addresses = getDirAddress(s_address, global_peer_ID, peer_name, peer_ID);
			if(ClientSync.DEBUG) out.println("Client:try_connect:1 did getDir: ");
			if(adr_addresses == null){
				if(ClientSync.DEBUG) out.print(" ");
				
				String key = type+":"+s_address;
				Hashtable<String,String> value = pc.get(key);
				if(value==null){
					value = new Hashtable<String,String>();
					pc.put(key, value);
				}
				value.put("No contact", now);
				
				if(ClientSync.DEBUG) out.println("Client:try_connect:1 DIR returns empty");
				return false;
			}
			ClientSync.getSocketAddresses(tcp_sock_addresses, udp_sock_addresses, adr_addresses,
					global_peer_ID, type, s_address, peer_key, now, pc);
			s_address=Util.concat(adr_addresses.toArray(), DirectoryServer.ADDR_SEP);
			
			if(ClientSync.DEBUG) out.println("Client:try_connect: Will try DIR obtained address: "+s_address);
			DD.ed.fireClientUpdate(new CommEvent(this, old_address, null,"DIR ANSWER", peer_name+" ("+s_address+")"));
		}else{
			if(ClientSync.DEBUG) out.println("Client:try_connect:1 Will try simple address: "+s_address);
			Address ad = new Address(s_address, type);
			ClientSync.add_to_peer_contacted_addresses(ad, global_peer_ID);
			
			String key = type+":"+s_address;
			Hashtable<String,String> value = pc.get(key);
			if(value==null){
				value = new Hashtable<String,String>();
				pc.put(key, value);
			}
			value.put(s_address, now);
			pc.put(type+":"+s_address, value);
			if(ClientSync.DEBUG) out.println("Client: enum s_adr="+peer_key+":"+type+":"+s_address+" val="+s_address+" "+now);
			
			tcp_sock_addresses=new ArrayList<SocketAddress_Domain>();
			sock_addr=getTCPSockAddress(s_address);
			if(ClientSync.DEBUG) out.println("Client:try_connect:1 got tcp");
			if(sock_addr!=null) tcp_sock_addresses.add(new SocketAddress_Domain(sock_addr,ad));
			udp_sock_addresses=new ArrayList<SocketAddress_Domain>();
			// can be slow
			sock_addr=ClientSync.getUDPSockAddress(s_address); 
			if(sock_addr!=null) udp_sock_addresses.add(new SocketAddress_Domain(sock_addr,ad));
			if(ClientSync.DEBUG) out.println("Client:try_connect:1 got");
		}
		if(ClientSync.DEBUG) out.println("Client:try_connect:1 done");
		return try_connect(tcp_sock_addresses, udp_sock_addresses,
				old_address, s_address, type, global_peer_ID, peer_name,
				peer_directories_udp_sockets, peer_directories);
	}
	/**
	 * Called after the socket addresses are known
	 * @param tcp_sock_addresses
	 * @param udp_sock_addresses
	 * @param old_address : address of directory, used for messages in debug window
	 * @param s_address
	 * @param type
	 * @param global_peer_ID
	 * @param peer_name
	 * @param peer_directories_udp_sockets
	 * @param peer_directories
	 * @param peer_ID
	 * @return
	 */
	public boolean try_connect(
			ArrayList<SocketAddress_Domain> tcp_sock_addresses,
			ArrayList<SocketAddress_Domain> udp_sock_addresses,
			String old_address, // a DIR address or some other address
			String s_address,  // an address or a list of addresses (from a DIR) separated by ","
			String type,       // type of old_address
			String global_peer_ID,
			String peer_name,
			ArrayList<InetSocketAddress> peer_directories_udp_sockets,
			ArrayList<String> peer_directories) {
		boolean DEBUG = ClientSync.DEBUG || DD.DEBUG_PLUGIN;
		if(DEBUG) out.println("Client:try_connect:2 start "+s_address);
		InetSocketAddress sock_addr;
		String peer_key = peer_name;
		String now = Util.getGeneralizedTime();
		if(DEBUG) out.println("Client: now="+now);
		if(peer_key==null) peer_key=Util.trimmed(global_peer_ID);
		Hashtable<String, Hashtable<String,String>> pc = PeerContacts.peer_contacts.get(peer_key);
		if(pc==null){
			pc = new Hashtable<String, Hashtable<String,String>>();
			PeerContacts.peer_contacts.put(peer_key, pc);
		}
		if(DEBUG) out.println("Client:try_connect: Client received addresses:"+s_address+ " #tcp:"+tcp_sock_addresses.size());
		if(DD.ClientTCP) {			
			for(int k=0; k<tcp_sock_addresses.size(); k++) {
				if(DEBUG) out.println("Client try address["+k+"]"+tcp_sock_addresses.get(k));
			
				//address = addresses[k];
				SocketAddress_Domain sad = tcp_sock_addresses.get(k);
				sock_addr=sad.isa;//getSockAddress(address);
				DD.ed.fireClientUpdate(new CommEvent(this, null, null,"TRY ADDRESS", sock_addr+""));
				if(sock_addr.isUnresolved()) {
					if(DEBUG) out.println("Client: Peer is unresolved!");
					continue;
				}
			
				if(Server.isMyself(sock_addr)){
					if(DEBUG) out.println("Client: Peer is Myself!");
					DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Peer is Myself"));
					continue; //return false;
				}
				if(ClientSync.isMyself(Identity.port, sock_addr,sad)){
					if(DEBUG) out.println("Client: Peer is myself!");
					continue;
				}
				try{
					if(DEBUG) out.println("Client: Try TCP connection!");
					client_socket = new Socket();
					client_socket.connect(sock_addr, Server.TIMEOUT_Client_wait_Server);
					if(DEBUG) out.println("Client: Success connecting Server: "+sock_addr);
					DD.ed.fireClientUpdate(new CommEvent(this, peer_name, sock_addr,"SERVER", "Connected: "+global_peer_ID));
					
					
					String key = type+":"+old_address;
					Hashtable<String,String> value = pc.get(key);
					if(value==null){
						value = new Hashtable<String,String>();
						pc.put(key, value);
					}
					value.put(sock_addr+":TCP***", now);
					
					return true;
				}
				catch(SocketTimeoutException e){
					if(DEBUG) out.println("Client: TIMEOUT connecting: "+sock_addr);
					DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Connection TIMEOUT"));
					continue; //return false;
				}
				catch(ConnectException e){
					if(DEBUG) out.println("Client: Connection Exception: "+e);
					DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Connection e="+e));
					continue; //return false;
				}
				catch(Exception e){
					if(DEBUG) out.println("Client: General exception try-ing: "+sock_addr+" is: "+e);
					//if(DEBUG) e.printStackTrace();
					DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Exception="+e));
					continue; //return false;
				}
			}
		}
		//System.out.print("#0");
		if(DD.ClientUDP) {
			if(Application.aus == null){
				DD.ed.fireClientUpdate(new CommEvent(this, s_address,null,"FAIL: UDP Server not running", peer_name+" ("+global_peer_ID+")"));
				if(ClientSync._DEBUG) err.println("UClient socket not yet open, no UDP server");
				//System.out.print("#1");
				return false;
			}
			if(DEBUG) out.println("UClient received addresses #:"+udp_sock_addresses.size());
			for(int k=0; k<udp_sock_addresses.size(); k++) {
				//boolean DEBUG = true;
				if(DEBUG) out.println("UClient try address["+k+"]"+udp_sock_addresses.get(k));

				if(DD.AVOID_REPEATING_AT_PING&&(Application.aus!=null)&&(!Application.aus.hasSyncRequests(global_peer_ID))) {
					DD.ed.fireClientUpdate(new CommEvent(this, peer_name, null, "LOCAL", "Stop sending: Received ping confirmation already handled from peer"));
					if(DEBUG) System.out.println("UDPServer Ping already handled for: "+Util.trimmed(global_peer_ID));
					{
						String key = type+":"+old_address;
						Hashtable<String,String> value = pc.get(key);
						if(value==null){
							value = new Hashtable<String,String>();
							pc.put(key, value);
						}
						sock_addr=udp_sock_addresses.get(k).isa;//getSockAddress(address);
						value.put(sock_addr+" UDP-"+PeerContacts.ALREADY_CONTACTED, now);
					}
					//System.out.print("#2");
					return false;					
				}
				SocketAddress_Domain sad = udp_sock_addresses.get(k);
				sock_addr=sad.isa;//getSockAddress(address);
				if(DEBUG) out.println("UClient:try_connect: checkMyself");
				
				if(ClientSync.isMyself(Identity.udp_server_port, sock_addr, sad)){
					if(DEBUG) out.println("Client:try_connect: UPeer "+peer_name+" is myself!"+sock_addr);
					//System.out.print("#3");
					continue;
				}
				
				if(DEBUG) out.println("UClient:try_connect: check unresolved");
				if(sock_addr.isUnresolved()) {
					if(ClientSync._DEBUG) out.println("Client: try_connect: UPeer "+peer_name+" is unresolved! "+sock_addr);
					//System.out.print("#4");
					continue;
				}
				if(DEBUG) out.println("UClient:try_connect: sending ping");

				if(DEBUG)System.out.println("Client Sending Ping to: "+sock_addr+" for \""+peer_name+"\"");
				ASNUDPPing aup = new ASNUDPPing();
				aup.senderIsPeer=false;
				aup.senderIsInitiator=true;
				aup.initiator_domain = Identity.get_a_server_domain();//client.getInetAddress().getHostAddress();
				aup.initiator_globalID=Identity.current_peer_ID.globalID; //dr.initiator_globalID;
				aup.initiator_port = Identity.udp_server_port;//dr.UDP_port;
				aup.peer_globalID=global_peer_ID;
				aup.peer_domain=sad.ad.domain;//;
				if(DEBUG) System.out.println("Client:try_connect: domain ping = \""+aup.peer_domain+"\" vs \""+Util.getNonBlockingHostName(sock_addr)+"\"");
				aup.peer_port=sock_addr.getPort();
				byte[] msg = aup.encode();
				DatagramPacket dp = new DatagramPacket(msg, msg.length);
				try{
					dp.setSocketAddress(sock_addr);
					
					String key = type+":"+old_address;
					Hashtable<String,String> value = pc.get(key);
					if(value==null){
						value = new Hashtable<String,String>();
						pc.put(key, value);
					}
					value.put(sock_addr+":UDP***", now);
					if(DEBUG) out.println("Client: set adr="+type+":"+old_address+" val="+sock_addr+":UDP "+now);

				}catch(Exception e){
					System.err.println("Client is Skipping address: "+sock_addr+ " due to: "+e);
					continue;
				}
				try {
					//System.out.print("#_"+dp.getSocketAddress());
					Application.aus.send(dp);
				} catch (IOException e) {
					if(DEBUG)System.out.println("Fail to send ping to peer \""+peer_name+"\" at "+sock_addr);
					continue;
				}
				ArrayList<InetSocketAddress> directories = peer_directories_udp_sockets; // Identity.listing_directories_inet
				if((peer_directories.size()!=directories.size()) && (directories.size()==0) )
					directories = ClientSync.getUDPDirectoriesSockets(peer_directories, directories);
				if(DEBUG)System.out.println("I have sent to peer the UDP packet: "+aup);
				if(DEBUG)System.out.println("I have sent the UDP ping packet to: "+directories.size()+" directories");
				for (int d=0; d<directories.size(); d++) {
					try {
						InetSocketAddress dir_adr = (InetSocketAddress)directories.get(d);
						if(dir_adr.isUnresolved()) continue;
						dp.setSocketAddress(dir_adr);
						//System.out.print("#d");
						Application.aus.send(dp);
						if(DEBUG)System.out.println("I requested ping via: "+dp.getSocketAddress()+" ping="+aup);
					} catch (IOException e) {
						if(ClientSync._DEBUG)System.out.println("Client: try_connect: EEEEERRRRRRRROOOOOOORRRRR "+e.getMessage());
						//e.printStackTrace();
					}
				}
				if(DEBUG)System.out.println("I have sent to peer the UDP packet");
			}
		}
		DD.ed.fireClientUpdate(new CommEvent(this, peer_name, null, "SERVER", "Fail for: "+s_address+"("+old_address+")"));
		if(DEBUG) out.println("Client:try_connect:2 done");
		//System.out.print("#8");
		return false;
	}
	static InetSocketAddress getTCPSockAddress(String address) {
		if(ClientSync.DEBUG) out.println("Client: getSockAddress of: "+address);
		String addresses[] = Address.split(address);
		if (addresses.length<=0){
			if(ClientSync.DEBUG) out.println("Client: Addresses length <=0 for: "+address);
			return null;
		}
		int a=Address.getTCP(addresses[0]);//.lastIndexOf(":");
		if(a<=0){
			if(ClientSync.DEBUG) out.println("Client: Address components !=2 for: "+addresses[0]);
			return null;
		}
		String c=Address.getDomain(addresses[0]);//.substring(0, a);
		return new InetSocketAddress(c,a);		
	}
	static ArrayList<Address> getDirAddress(String dir_address, String global_peer_ID, String peer_name, String peer_ID) {
		if(ClientSync.DEBUG) out.println("Client: getDirAddress: "+dir_address+" ID="+Util.trimmed(global_peer_ID));
		InetSocketAddress sock_addr=getTCPSockAddress(dir_address);
		if(sock_addr == null){
			if(ClientSync._DEBUG) out.println("Client: getDirAddress");
			ClientSync.reportDa(dir_address, global_peer_ID, peer_name, null, _("Null Socket"));
			return null;//"";
		}
		Socket socket = new Socket();
		try {
			socket.connect(sock_addr, Server.TIMEOUT_Client_wait_Dir);
			DirectoryRequest dr = new DirectoryRequest(global_peer_ID, Identity.current_peer_ID.globalID, Identity.udp_server_port, peer_ID, dir_address);
			byte[] msg = dr.encode();
			socket.setSoTimeout(Server.TIMEOUT_Client_wait_Dir);
			socket.getOutputStream().write(msg);
			if(ClientSync.DEBUG) out.println("Client: Sending to Directory Server: "+Util.byteToHexDump(msg, " ")+dr);
			DirectoryAnswer da = new DirectoryAnswer(socket.getInputStream());
			ClientSync.reportDa(dir_address, global_peer_ID, peer_name, da, null);
			if(da.addresses.size()==0){
				if(ClientSync._DEBUG) out.println("Client: Got no addresses!");
				socket.close();
				return null;
			}
			if(ClientSync.DEBUG) out.println("Dir Answer: "+da);
			socket.close();
			if(da.addresses==null){
				if(ClientSync._DEBUG) out.println("Client: Got empty addresses!");
				return null;
			}
			return da.addresses;
			//InetSocketAddress s= da.address.get(0);
			//return s.getHostName()+":"+s.getPort();
		}catch (IOException e) {
			if(ClientSync.DEBUG) out.println("Client: getDirAddress fail: "+e+" peer: "+peer_name+" DIR addr="+dir_address);
			ClientSync.reportDa(dir_address, global_peer_ID, peer_name, null, e.getLocalizedMessage());
			//e.printStackTrace();
			//Directories.setUDPOn(dir_address, new Boolean(false));
		} catch (Exception e) {
			if(ClientSync.DEBUG) out.println("Client: getDirAddress fail: "+e+" peer: "+peer_name+" DIR addr="+dir_address);
			ClientSync.reportDa(dir_address, global_peer_ID, peer_name, null, e.getLocalizedMessage());
			e.printStackTrace();
		}
		//socket.close();
		//out.println("Client: getDirAddress: fail");
		return null;
	}
	boolean turnOff = false;
	public Object wait_lock = new Object();
	public void turnOff() {
		turnOff = true;
		this.interrupt();
	}
	synchronized public void run() {
		DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Start"));
		_run();
		DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Will Stop"));
		if(ClientSync.DEBUG) out.println("Client: turned Off");
	}
	synchronized public void _run() {
		String peers_scan_sql = "SELECT "+table.peer.peer_ID+", "+table.peer.name+", "+table.peer.global_peer_ID+", "+table.peer.last_sync_date+", "+table.peer.filtered+
		" FROM "+table.peer.TNAME+" WHERE "+table.peer.used+" = 1;";
		ArrayList<ArrayList<Object>> peers;
		try {
			/*
			peers = Application.db.select("SELECT address, type, global_peer_ID, last_sync_date " +
					" FROM peer JOIN peer_address ON peer.peer_ID=peer_address.peer_ID WHERE used = 1;",
					new String[]{});
			*/
			peers = Application.db.select(peers_scan_sql,
					new String[]{});
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
			return;
		}
		Client1.peersAvailable = peers.size();
		if(ClientSync.DEBUG) out.println("Client: Found used peer adresses: "+peers.size());
		for(int p=0;;p++) {
			if(turnOff) break;
			// Avoid asking in parallel too many synchronizations (wait for answers)
			if((Application.aus!=null)&&(Application.aus.getThreads() > UDPServer.MAX_THREADS/2))
				try {
					System.out.println("Client: run: overloaded threads = "+Application.aus.getThreads());
					synchronized(wait_lock ){
						this.wait(Client1.PAUSE);
					}
				} catch (InterruptedException e2) {
					//e2.printStackTrace();
					continue;
				}
			
			//
			// Will reload peers and restart from the first peer, with "continue";
			//
			if(p>=peers.size()){
				p = -1;
				try {
					if(ClientSync.DEBUG) out.println("Will wait ms: "+Client1.PAUSE);
					if((!Client1.recentlyTouched) && (Client1.peersToGo <= 0)) {
						DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Will Sleep: "+Client1.PAUSE));
						synchronized(wait_lock ){
							wait_lock.wait(Client1.PAUSE);
						}
						DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Wakes Up"));
					}
					Client1.peersToGo--;
					Client1.recentlyTouched = false;
					peers = Application.db.select(peers_scan_sql, new String[]{});
					
				} catch (InterruptedException e) {
					if(ClientSync.DEBUG) e.printStackTrace();
				} catch (P2PDDSQLException e1) {
						Application.warning(_("Database: ")+e1, _("Database"));
						return;
					}
				//DD.userver.resetSyncRequests();
				continue;
			}
			if(turnOff) break;
			String peer_ID = Util.getString(peers.get(p).get(0));
			String peer_name = Util.getString(peers.get(p).get(1));
			String global_peer_ID = Util.getString(peers.get(p).get(2));
			
			D_PeerAddress peer = null;  // not yet used when saving new served orgs
			try { // addresses are found below, ordered by last connection
				peer = new D_PeerAddress(global_peer_ID, false, false, true);
			} catch (P2PDDSQLException e2) {
				e2.printStackTrace();
			}
			
			String _lastSnapshotString = (String)peers.get(p).get(3);
			boolean filtered = "1".equals(Util.getString(peers.get(p).get(4)));
			if(ClientSync.DEBUG)System.out.println("Client handling peer: "+Util.trimmed(peer_name));
			
			if(UDPServer.transferringPeerMessage(global_peer_ID)){
				if(ClientSync.DEBUG)System.out.println("Client peer already handled");
				continue;
			}
			
			ArrayList<ArrayList<Object>> peers_addr = null;
			try{
				peers_addr = Application.db.select("SELECT "+table.peer_address.address+", "+table.peer_address.type+", "+table.peer_address.peer_address_ID+
						" FROM "+table.peer_address.TNAME+" WHERE "+table.peer_address.peer_ID+" = ? ORDER BY "+table.peer_address.my_last_connection+" DESC;",
						new String[]{peer_ID});
				
			} catch (P2PDDSQLException e1) {
				Application.warning(_("Database: ")+e1, _("Database"));
				return;
			}
			
			
			if(Application.aus!=null) Application.aus.addSyncRequests(global_peer_ID);
			int p_addresses = peers_addr.size();
			ArrayList<String> peer_directories = getDirectories(peers_addr);
			ArrayList<InetSocketAddress> peer_directories_sockets = new ArrayList<InetSocketAddress>();
			peer_directories_sockets = ClientSync.getUDPDirectoriesSockets(peer_directories, peer_directories_sockets);
			Calendar _lastSnapshot=null;
			if(p_addresses>0) _lastSnapshot = Util.getCalendar(_lastSnapshotString);
			DD.ed.fireClientUpdate(new CommEvent(this, peer_name, null, "LOCAL", "Attempt to contact peer"));
			
			ClientSync.peer_contacted_dirs.put(global_peer_ID, peer_directories);

			if(ClientSync.DEBUG) out.println("Client: Will try #"+p_addresses);
			for(int a=0; a < p_addresses; a++) {
				String address = (String) peers_addr.get(a).get(0);
				String type=(String)peers_addr.get(a).get(1);
				if((Application.aus!=null)&&!Application.aus.hasSyncRequests(global_peer_ID)) {
					DD.ed.fireClientUpdate(new CommEvent(this, peer_name, null, "LOCAL", "Stop sending: Received ping confirmation already handled from peer"));
					if(ClientSync.DEBUG)System.out.println("Client:run: Ping already handled for: "+Util.trimmed(global_peer_ID));
					if(ClientSync.DEBUG)System.out.println("Client:run: will skip: "+address+":"+type);
					// PeerContacts keeps track of contacted peers for the debug GUI window
					String peer_key = peer_name;
					if(peer_key == null)peer_key = ""+Util.trimmed(global_peer_ID);
					Hashtable<String, Hashtable<String, String>> pc = PeerContacts.peer_contacts.get(peer_key);
					if(pc==null){
						pc = new Hashtable<String, Hashtable<String, String>>();
						PeerContacts.peer_contacts.put(peer_name, pc);
					}
					String key = type+":"+address;
					Hashtable<String, String> value = pc.get(key);
					if(value == null) {
						value = new Hashtable<String,String>();
						pc.put(key, value);
					}
					value.put(PeerContacts.ALREADY_CONTACTED, Util.getGeneralizedTime());
					PeerContacts.peer_contacts.put(peer_name, pc);
					if(Application.peer!=null)Application.peer.update(PeerContacts.peer_contacts);
					break;					
				}

				String address_ID = Util.getString(peers_addr.get(a).get(2));
				SocketAddress peer_sockaddr=null;
				try{peer_sockaddr=client_socket.getRemoteSocketAddress();}catch(Exception e){
					e.printStackTrace();
					continue;
				}
			
				DD.ed.fireClientUpdate(new CommEvent(this, peer_name, peer_sockaddr, "LOCAL", "Try to connect to: "+address));
				// This function tried both TCP and UDP connections, based on the DD.ClientUDP and DD.ClientTCP, true if TCP
				if(!try_connect(address, type, global_peer_ID, peer_name, peer_directories_sockets, peer_directories, peer_ID)){
					if(ClientSync.DEBUG)System.out.println("Client:run: Ping failed for: \""+peer_name+"\" at \""+address+"\" id="+Util.trimmed(global_peer_ID));
					if(Application.peer!=null) Application.peer.update(PeerContacts.peer_contacts);
					continue;
				}
				if(Application.peer!=null)Application.peer.update(PeerContacts.peer_contacts);
				if(Application.peers!=null) Application.peers.setConnectionState(peer_ID, Peers.STATE_CONNECTION_TCP);
				if(ClientSync.DEBUG) out.println("Client: Connected!");
				DD.ed.fireClientUpdate(new CommEvent(this, peer_name, client_socket.getRemoteSocketAddress(), "Server", "Connected"));
						
				ASNSyncRequest sr = ClientSync.buildRequest(_lastSnapshotString, _lastSnapshot, peer_ID);
				if(filtered) sr.orgFilter=UpdateMessages.getOrgFilter(peer_ID);
				sr.sign();
				try {
					//out.println("Request sent last sync date: "+Encoder.getGeneralizedTime(sr.lastSnapshot)+" ie "+sr.lastSnapshot);
					//out.println("Request sent last sync date: "+sr);

					byte[] msg = sr.encode();
					if(ClientSync.DEBUG) out.println("Client: Sync Request sent: "+Util.byteToHexDump(msg, " ")+"::"+sr);
					client_socket.getOutputStream().write(msg);
					/*
					Decoder testDec = new Decoder(msg);
					ASNSyncRequest asr = new ASNSyncRequest();				
					asr.decode(testDec);
					out.println("Request sent last sync date: "+Encoder.getGeneralizedTime(asr.lastSnapshot)+" ie "+asr.lastSnapshot);
					*/
					DD.ed.fireClientUpdate(new CommEvent(this, peer_name, client_socket.getRemoteSocketAddress(), "Server", "Request Sent"));
					byte update[] = new byte[Client1.MAX_BUFFER];
					if(ClientSync.DEBUG) out.println("Waiting data from socket: "+client_socket+" on len: "+update.length+" timeout ms:"+Server.TIMEOUT_Client_Data);
					client_socket.setSoTimeout(Server.TIMEOUT_Client_Data);
					InputStream is=client_socket.getInputStream();
					int len = is.read(update);
					if(len>0){
						if(ClientSync.DEBUG) err.println("Client: answer received length: "+len);
						DD.ed.fireClientUpdate(new CommEvent(this, peer_name, client_socket.getRemoteSocketAddress(), "Server", "Answered Received"));
						Decoder dec = new Decoder(update,0,len);
						System.out.println("Got first msg size: "+len);//+"  bytes: "+Util.byteToHex(update, 0, len, " "));
						if(!dec.fetchAll(is)){
							System.err.println("Buffer too small for receiving update answer!");
							continue;
						}
						len = dec.getMSGLength();
						//System.out.println("Got msg size: "+len);//+"  bytes: "+Util.byteToHex(update, 0, len, " "));
						RequestData rq = new RequestData();
						integrateUpdate(update,len, (InetSocketAddress)client_socket.getRemoteSocketAddress(), this, global_peer_ID, peer_ID, address_ID, rq, peer);
						if(ClientSync.DEBUG) err.println("Client: answer received rq: "+rq);
						/*
						while(true){
							try{
								integrateUpdate(update,len);
								break;
							}catch(RuntimeException e) {
								len+=s.getInputStream().read(update, len, update.length-len);
								err.println("New length="+len);
							}
						}
						*/
						// We record the date only if it was fully successful.
						/*
						String gdate = Util.getGeneralizedTime();
						Application.db.update(table.peer.TNAME, new String[]{table.peer.last_sync_date}, new String[]{table.peer.global_peer_ID},
								new String[]{gdate, global_peer_ID});
						Application.db.update(table.peer_address.TNAME, new String[]{table.peer_address.my_last_connection}, new String[]{table.peer_address.peer_ID},
								new String[]{gdate, peer_ID});
						*/
					}else{
						if(ClientSync.DEBUG) out.println("Client: No answered received!");
						DD.ed.fireClientUpdate(new CommEvent(this, peer_name, client_socket.getRemoteSocketAddress(), "Server", "TIMEOUT_Client_Data may be too short: No Answered Received"));
					}
				} catch (SocketTimeoutException e1) {
					if(ClientSync.DEBUG) out.println("Read done: "+e1);
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (ASN1DecoderFail e) {
					e.printStackTrace();
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					if(ClientSync.DEBUG) out.println("Client: will close socket");
					client_socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(ClientSync.DEBUG) out.println("Client: done peer");
				break;
			}
		}
	}
	/**
	 * Extract those whose type (get(i).get(1) is Address.DIR).
	 * @param peers_addr
	 * @return
	 */
	private ArrayList<String> getDirectories(
			ArrayList<ArrayList<Object>> peers_addr) {
		ArrayList<String> result = new ArrayList<String>();
		for(int i=0; i<peers_addr.size(); i++) {
			if(Address.DIR.equals(peers_addr.get(i).get(1))){
				result.add(Util.getString(peers_addr.get(i).get(0)));
			}
		}
		return result;
	}

	static String getString(byte[] bytes){
		if(bytes==null) return null;
		return new String(bytes);
	}
	public static void integrateUpdate(byte[] update, int len, InetSocketAddress s_address, Object src, 
			String global_peer_ID, String peer_ID, String address_ID, RequestData rq, D_PeerAddress peer) throws ASN1DecoderFail, P2PDDSQLException {
		if(ClientSync.DEBUG) err.println("Client: will integrate update: "+update.length+" datalen="+len+"::");
		if(ClientSync.DEBUG) err.println("Update: "+Util.byteToHexDump(update,len));
		SyncAnswer asa = new SyncAnswer();
		Decoder dec = new Decoder(update, 0, len);
		if(ClientSync.DEBUG) err.println("Client: will decode");
		asa.decode(dec);
		if(ClientSync.DEBUG) out.println("Client: Got answer: "+asa.toString());
		UpdateMessages.integrateUpdate(asa, s_address, src, global_peer_ID, peer_ID, address_ID, rq, peer);
	}

	public Object get_wait_lock() {
		return wait_lock;
	}

	@Override
	public void wakeUp() {
		synchronized(get_wait_lock()) {
			Client1.recentlyTouched = true;
			Client1.peersToGo = Client1.peersAvailable;
			get_wait_lock().notify();
		}
	}
}
