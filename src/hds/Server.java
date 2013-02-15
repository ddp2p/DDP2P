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
import static util.Util._;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Random;
import java.util.Collections;

import javax.swing.JOptionPane;

import streaming.RequestData;
import streaming.UpdateMessages;
import util.CommEvent;
import util.DBInterface;
import util.Util;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;
import config.Identity;
import data.D_PeerAddress;
import data.D_PluginInfo;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;

class ServerThread extends Thread {
	private static final int MAX_SR = 100000;
	private static final boolean DEBUG = false;
	//private static final boolean _DEBUG = true;
	private static int cnt = 0;
	Socket s;
	Server server;
	String name;
	ServerThread(Socket s, Server server){
		if(DEBUG) out.println("server thread: Created");
		this.s=s;
		this.server = server;
		name=cnt+""; cnt++;
	}
	public void run(){
		DD.ed.fireServerUpdate(new CommEvent(this, null, s.getRemoteSocketAddress(), "LOCAL", "Server Thread starting"));
		byte sr[] = new byte[MAX_SR];
		if(DEBUG) out.println("server thread: Started");
		server.incThreads();
		if(DEBUG) out.println("server thread: incremented");
		do{
			try {
				if(DEBUG) out.println("Socket Talking to: "+s);
				DD.ed.fireServerUpdate(new CommEvent(this, null, s.getRemoteSocketAddress(), "Client", "Connection"));
				InputStream is = s.getInputStream();
				int msglen=is.read(sr);
				DD.ed.fireServerUpdate(new CommEvent(this, null, s.getRemoteSocketAddress(), "Client", "Sync Requested"));
				if(DEBUG) out.println("server thread: Got: "+Util.byteToHexDump(sr,msglen));
				Decoder dec = new Decoder(sr,0,msglen);
				/*
				while(true) {
					int asrlen = dec.objectLen();
					if ((asrlen<0) || (msglen<asrlen)) {
						msglen += is.read(sr, msglen, sr.length-msglen);
						dec = new Decoder(sr,0,msglen);
						continue;
					}
					break;
				}
				*/
				if(!dec.fetchAll(is)){
					System.err.println("Buffer too small for receiving request!");
					continue;
				}
				
				ASNSyncRequest asr = new ASNSyncRequest();				
				asr.decode(dec);
				if(!asr.verifySignature()){
					DD.ed.fireServerUpdate(new CommEvent(this, null, s.getRemoteSocketAddress(), "Client", "Server Unsigned Sync Request received: "+asr));
					if(DEBUG)out.println("Server: Unsigned Request received: "+asr.toString());
					continue;
				}
				SocketAddress isa = s.getRemoteSocketAddress();
				DD.ed.fireServerUpdate(new CommEvent(this, null, isa, "Client", "Sync Request received: "+asr));
				Server.extractDataSyncRequest(asr, isa, this);
				if(DEBUG)out.println("Request received: "+asr.toString());
				
				SyncAnswer sa = UpdateMessages.buildAnswer(asr, null);
				byte[]msg = sa.encode();
				
				if(DEBUG) out.println("Answering: "+Util.byteToHexDump(msg, " ")+"::"+Util.trimmed(sa.toString(),300));
				DD.ed.fireServerUpdate(new CommEvent(this, null, s.getRemoteSocketAddress(), "Sharing", sa.toString()));
				s.getOutputStream().write(msg);
				DD.ed.fireServerUpdate(new CommEvent(this, null, s.getRemoteSocketAddress(), "Client", "Reply Sent"));
				if(DEBUG) out.println("Answered:"+msg.length);//+"::"+Util.byteToHex(msg, " "));
				//out.println("Answered:"+msg.length);//+"::"+Util.byteToHex(msg, " "));
				s.getInputStream().read(sr);
				if(DEBUG) out.println("Closing: "+s);
				DD.ed.fireServerUpdate(new CommEvent(this, null, s.getRemoteSocketAddress(), "Client", "Closing connection"));
				s.close();
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
			catch (IOException e1) {
				e1.printStackTrace();
			}
			if(server.turnOff) break;
			try{
				server.ss.setSoTimeout(Server.TIMEOUT_Handler);
				if(DEBUG) out.println("server thread: Server Handler wants to accept");
				s=server.ss.accept();
				if(DEBUG) out.println("server thread: Server Handler has obtained a new client");
			}
			catch (SocketTimeoutException e){
				if(DEBUG) out.println("server thread: Server Handler times out");
				break;
			}
			catch(Exception e) {
				e.printStackTrace();
				break;
			}
		}while(true);
		if(DEBUG) out.println("server thread: Server Handler exits");
		DD.ed.fireServerUpdate(new CommEvent(this, null, s.getRemoteSocketAddress(), "LOCAL", "Stop"));
		server.decThreads();
	}
}

public class Server extends Thread {
	public static final int TIMEOUT_Handler = 1000000;
	public static final int TIMEOUT_Server = 2000;
	public static final int TIMEOUT_Client_Data = 20000;
	public static final int PORT = 45000;
	public static final int TIMEOUT_Client_wait_Server = 2000;
	public static final int TIMEOUT_Client_wait_Dir = 2000;
	public static final String DIR = "DIR";
	public static final String SOCKET = "Socket";
	public static int TIMEOUT_UDP_NAT_BORER = 2000;
	public static final long TIMEOUT_UDP_Reclaim = 2000;
	public static final int TIMEOUT_UDP_Announcement_Diviser = 100;
	public static final boolean DEBUG = false;
	public static final boolean _DEBUG = true;
	Object lock = new Object();
	//static InetSocketAddress serv_sock_addr;
	static ArrayList<InetSocketAddress> serv_sock_addresses = new ArrayList<InetSocketAddress>();
	ServerSocket ss;
	//DatagramSocket ds;
	int port;
	int threads = 0;
	static int MAX_THREADS = 10;
	void incThreads(){
		synchronized(lock){
			threads++;
		}
	}
	void decThreads(){
		synchronized(lock) {
			threads--;
			if(threads < MAX_THREADS) 
				lock.notify();
		}
	}
	static int getRandomPort(){
		int port = new Random().nextInt();
		port = Math.abs(port);
		if (port > 65535) port = port % 65535;
		if (port <= 1000) port = 1000;
		return port;
	}
	public static void extractDataSyncRequest(ASNSyncRequest asr, SocketAddress sa, Object caller) throws SQLiteException {
		Calendar crt_date = Util.CalendargetInstance();
		String _crt_date = Encoder.getGeneralizedTime(crt_date);
		D_PeerAddress pa = asr.address;
		if(pa != null) {
			String peer_ID = null;
			DD.ed.fireServerUpdate(new CommEvent(caller, pa.name, sa, "Client", "Peer Name decoded"));
			if((pa!=null)&&(pa.signature!=null)&&((pa.globalID!=null)||(pa.globalIDhash!=null))) {
				boolean verif = pa.verifySignature();
				if(verif) {
					D_PeerAddress local = new D_PeerAddress(pa.globalID, pa.globalIDhash, false, false, true);
					if(local.peer_ID==null){
						int i = 2;
						pa.blocked = DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME;
						pa.used = false; //DD.USE_NEW_ARRIVING_PEERS_CONTACTING_ME;
						peer_ID = pa.storeVerified();

						if(DD.ASK_USAGE_NEW_ARRIVING_PEERS_CONTACTING_ME) {
							Object sync = _("Synchronize from")+" "+pa.name;
							Object options[] = new Object[]{sync, _("No Sync from it"), _("Use defaults"), _("Use defaults, do not ask again")};
							i = Application.ask(
								_("Use new peer for synchronization?"),
								_("A new peer is contacting you. Do you want to sync from it, too?")+" \n"+
								//_("Select \"Cancel\" for not being asked again, but using defaults.")+"\n"+
								_("Defaults may be affected by the current choice.")+"\n"+
								_("Current default for synchronizing with new peer is:")+" "+(DD.USE_NEW_ARRIVING_PEERS_CONTACTING_ME?_("Sync from this"):_("Do not sync from this"))+"\n"+
								_("Default for blocking (employed at No Synchronizing) is:")+" "+(DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME?_("Blocked"):_("Not blocked"))+" \n"+
								_("Name of Peer contacting you is:")+" \""+pa.name+"\" \n"+_("His current slogan is:")+" \""+pa.slogan+"\"",
								options, sync, null);
								//JOptionPane.YES_NO_CANCEL_OPTION);
						}
						switch(i) {
						case 0:
							pa.blocked = false;
							DD.USE_NEW_ARRIVING_PEERS_CONTACTING_ME = pa.used = true;
							peer_ID = pa.storeVerified();
							break;
						case 1:
							pa.blocked = DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME;
							DD.USE_NEW_ARRIVING_PEERS_CONTACTING_ME = pa.used = false;
							peer_ID = pa.storeVerified();
							break;
						case 3:
							DD.ASK_USAGE_NEW_ARRIVING_PEERS_CONTACTING_ME = false;
						case 2:
						case JOptionPane.CLOSED_OPTION:
						default:
							pa.blocked = DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME;
							pa.used = DD.USE_NEW_ARRIVING_PEERS_CONTACTING_ME;
							peer_ID = pa.storeVerified();
							break;
						}
					}else{
						local.setRemote(pa);
						peer_ID = Util.getStringID(local._storeVerified(crt_date, _crt_date)); //D_PeerAddress.storeReceived(pa, crt_date, _crt_date);
					}
				}
			}
			if(peer_ID==null){
				if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginData: distributeToPlugins: discard message from peer=null!");	
				//return;
			}else{
				D_PluginInfo.recordPluginInfo(asr.plugin_info, pa.globalID, peer_ID);
				if(asr.plugin_msg != null){
					try {
						if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginData: distributeToPlugins: message "+asr.plugin_msg);
						asr.plugin_msg.distributeToPlugins(pa.globalID);
					} catch (ASN1DecoderFail e) {
						e.printStackTrace();
					}
				}else{
					if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginData: distributeToPlugins: no message!");				
				}
			}
			
			if((peer_ID!=null) || (DD.ACCEPT_STREAMING_SYNC_REQUEST_PAYLOAD_DATA_FROM_UNKNOWN_PEERS)) {
				if(asr.pushChanges!=null) {
					RequestData _rq = new RequestData();
					InetSocketAddress isa = (InetSocketAddress)sa;
					String address_ID = D_PeerAddress.getAddressID(isa, peer_ID);
					try {
						UpdateMessages.integrateUpdate(asr.pushChanges, isa, caller, pa.globalID, peer_ID, address_ID, _rq, asr.address);
					} catch (ASN1DecoderFail e) {
						e.printStackTrace();
					}
				}
			}
		}else{
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginData: distributeToPlugins: no peer address for plugin data!");
		}
		
	}
	void try_connect(int port) {
		if(DEBUG) out.println("BEGIN Server.try_connect: port="+port);
		boolean connected = false;
		if(port <= 0) port = getRandomPort();
		do{
			try {
				if(DEBUG) out.println("BEGIN Server.try_connect: serversocket="+port);
				ss = new ServerSocket(port);
				if(DEBUG) out.println("BEGIN Server.try_connect: serversocket done");
				connected = true;
				this.port = port;
			}catch(Exception e) {
				try{ss.close();}catch(Exception ex){}
				e.printStackTrace();
				connected = false;
				port = getRandomPort(); 
			}
		} while(!connected);
		
		if(DEBUG) out.println("BEGIN Server.try_connect: synchronized on dir:"+UDPServer.directoryAnnouncementLock);
		synchronized (UDPServer.directoryAnnouncementLock){
			if(DEBUG) out.println("BEGIN Server.try_connect: synchronized");
			Identity.port = this.port = ss.getLocalPort();
			UDPServer.directoryAnnouncement = null;
		}
		
		if(DEBUG) out.println("BEGIN Server.try_connect: detectDomain");
		try{detectDomain();}catch(Exception e){}
		DD.createMyPeerIDIfEmpty();
		if(DEBUG) out.println("END Server.try_connect");
	}
	public Server() throws SQLiteException {
		if(DEBUG) out.println("Start Server");
		try_connect(PORT);
		Identity peer_ID = new Identity();
		peer_ID.globalID = Identity.current_peer_ID.globalID;
		peer_ID.name = Identity.current_peer_ID.name;
		peer_ID.slogan = Identity.current_peer_ID.slogan;
		set_my_peer_ID_TCP(peer_ID);
	}
	public Server(int port) throws SQLiteException {
		if(DEBUG) out.println("Start Server port="+port);
		try_connect(port);
		Identity peer_ID = new Identity();
		peer_ID.globalID = Identity.current_peer_ID.globalID;
		peer_ID.name = Identity.current_peer_ID.name;
		peer_ID.slogan = Identity.current_peer_ID.slogan;
		set_my_peer_ID_TCP(peer_ID);
	}
	public Server(Identity peer_id) throws SQLiteException {
		if(DEBUG) out.println("Start Server peer_id="+peer_id);
		try_connect(PORT);
		set_my_peer_ID_TCP(peer_id);
	}
	public Server(int port, Identity peer_id) throws SQLiteException {
		if(DEBUG) out.println("Start Server port="+port+" id="+peer_id);
		try_connect(port);
		set_my_peer_ID_TCP(peer_id);
	}
	/**
	 * Initializes Identity.domain with the current inetAddress of the machine.
	 * @throws SocketException
	 */
	public static void detectDomain() throws SocketException{
		detectDomain(Identity.port);
	}
	public static void detectDomain(int port) throws SocketException{
		synchronized (Identity.my_server_domains) {
			if ((Identity.my_server_domains.size()>0) || (Identity.my_server_domains_loopback.size()>0)) return;
			if(DEBUG) out.println("END Server.detectDomain");
			System.setProperty("java.net.preferIPv4Stack", "true");
			//Properties prop = System.getProperties();
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface netint : Collections.list(nets)) {
				if(DEBUG) out.printf("server: Display name: %s\n", netint.getDisplayName());
				if(!netint.isUp()){
					if(DEBUG) out.printf("server: Interface down\n");
					continue;
				}
				if(DEBUG) out.printf("server: Name: %s (loopback: %s; p2p:%s; up: %s, v: %s, m:%s)\n", netint.getName(),
						""+netint.isLoopback(),
						""+netint.isPointToPoint(),
						""+netint.isUp(),
						""+netint.isVirtual(),
						""+netint.supportsMulticast()
						);
				Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
				for (InetAddress inetAddress : Collections.list(inetAddresses)) {
					if(DEBUG) out.printf("server: inetAddress: %s\n", inetAddress);
					//Identity.domain=inetAddress;
					serv_sock_addresses.add(new InetSocketAddress(inetAddress, port));
					if (!netint.isLoopback()){
						if(DEBUG) out.printf("server: Interface is not loopback\n");
						//return;
						Identity.my_server_domains.add(inetAddress);
					}else{
						Identity.my_server_domains_loopback.add(inetAddress);
					}
					//break;
				}
			}
		}
		if(DEBUG) out.println("END Server.detectDomain: domains="+Identity.my_server_domains.size()+
				", loopdomains="+Identity.my_server_domains_loopback.size());
	}
	/**
	 * Updating my name and slogan based on my global peer ID
	 */
	public static void update_my_peer_ID_peers_name_slogan(){
		D_PeerAddress.update_my_peer_ID_peers_name_slogan_broadcastable(Identity.getAmIBroadcastable());
	}
	public static void set_my_peer_ID_UDP (Identity id, DatagramSocket ds) throws SQLiteException {
		//boolean DEBUG = true;
		if(DEBUG) out.println("Server: set_my_peer_ID_UDP: enter");
		if(id==null) return;
		if(id.globalID==null) return;
		
		if(DEBUG) out.println("Server: set_my_peer_ID_UDP: will announce");
		UDPServer.announceMyselfToDirectories(ds);
		if(DEBUG) out.println("Server: set_my_peer_ID_UDP: will set my ID");
		D_PeerAddress._set_my_peer_ID(id);
		if(DEBUG) out.println("Server: set_my_peer_ID_UDP: exit");
	}	
	/**
	 * Announce myself to directories, and register current address
	 *  and known directories into peer_address table
	 * @param id
	 * @throws SQLiteException
	 */
	public static void set_my_peer_ID_TCP (Identity id) throws SQLiteException {
		if(DEBUG) out.println("Server: set_my_peer_ID_TCP: enter");
		if(id==null) return;
		if(id.globalID==null) return;
		
		UDPServer.announceMyselfToDirectoriesTCP();
		D_PeerAddress._set_my_peer_ID(id);
		if(DEBUG) out.println("Server: set_my_peer_ID_TCP: exit");
	}
	/*
	public void setNameSlogan (Identity id, String name, String slogan) throws SQLiteException{
		ArrayList<ArrayList<Object>> op;
		op=Application.db.select("SELECT "+table.peer.peer_ID+" FROM "+table.peer.TNAME+" WHERE "+table.peer.global_peer_ID+" = ?;", new String[]{id.globalID});
		if(op.size()==0) {
			long pID=Application.db.insert(table.peer.TNAME, new String[]{table.peer.global_peer_ID, table.peer.name, table.peer.broadcastable,table.peer.arrival_date, table.peer.slogan, table.peer.used},
					new String[]{id.globalID,name, "0", Util.getGeneralizedTime(),slogan,"0"} );
		}else{
			String my_peer_ID = (String)op.get(0).get(0);
			Application.db.update(table.peer.TNAME, 
					new String[]{table.peer.name, table.peer.arrival_date, table.peer.slogan},
					new String[]{table.peer.peer_ID},
					new String[]{name, Util.getGeneralizedTime(),slogan, my_peer_ID} );
		}
	}
	*/
	/**
	 * Send to each directory in the list Identity.listing_directories_inet!
	 * @param da A prepared Directory Announcement
	 */
	public static void announceMyselfToDirectories(DirectoryAnnouncement da) {
		if(DEBUG) out.println("Server:announceMyselfToDirectories:");
		boolean first = true;
		String dir_address=null;
		for(InetSocketAddress dir : Identity.listing_directories_inet ) {
			if(DEBUG) out.println("Server:announceMyselfToDirectories: announce to: "+dir);
			try{
				dir_address = dir.getAddress().getHostAddress()+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+dir.getPort();
				Socket s = new Socket();
				s.setSoTimeout(TIMEOUT_Client_wait_Dir);
				s.connect(dir, TIMEOUT_Client_wait_Dir);
				//s.setSoTimeout(TIMEOUT_Client_wait_Dir);
				byte msg[]=da.encode();
				s.getOutputStream().write(msg);
				if(DEBUG) out.println("Server:announceMyselfToDirectories: sent: "+da);//+"\n"+Util.byteToHex(msg," "));
				byte answer[] = new byte[10];
				if(DEBUG) out.println("Server:announceMyselfToDirectories: Waiting answer!");
				int alen=s.getInputStream().read(answer);
				if(DEBUG) out.println("Server:announceMyselfToDirectories: Got answer: "+Util.byteToHex(answer, 0, alen, " "));
				Decoder answer_dec=new Decoder(answer);
				if(DEBUG) out.println("Server:announceMyselfToDirectories: Directory Answer: "+answer_dec.getContent().getBoolean());
				s.close();
				if(first){
					Identity.preferred_directory_idx = Identity.listing_directories_inet.indexOf(dir);
					first = false;
				}
				if(Application.ld!=null)
					Application.ld.setTCPOn(dir_address, new Boolean(true));
			}catch(Exception e) {
				//Application.warning(_("Error announcing myself to directory:")+dir, _("Announcing Myself to Directory"));
				DD.directories_failed.add(dir);
				if(Application.ld!=null)
					Application.ld.setTCPOn(dir_address, new Boolean(false));
				if(DEBUG) err.println("Server: "+_("Error announcing myself to directory:")+dir);
				//e.printStackTrace();
			}
		}
		if(DEBUG) out.println("Server:announceMyselfToDirectories: Done!");
	}
	void wait_if_needed() throws InterruptedException{
		synchronized(lock){ 
			if(threads>=MAX_THREADS) this.wait();		
		}
	}
	boolean turnOff = false;
	public void turnOff(){
		turnOff = true;
		try{ss.close();}catch(Exception e){}
		this.interrupt();
	}
	public void run() {
		DD.ed.fireServerUpdate(new CommEvent(this, null, null, "LOCAL", "Server starting"));
		for(;;) {
			if (turnOff) break;
			try {
				if(this.isInterrupted()) break;
				wait_if_needed();
				if(this.isInterrupted()) break;
				if(DEBUG) out.println("server: Server will accept!");
				Socket s=ss.accept();
				if(DEBUG) out.println("server: Server accepted, will launch!");
				if(this.isInterrupted()) break;
				new ServerThread(s, this).start();
			}
			catch (SocketTimeoutException e){
				
			}
			catch (SocketException e){
				if(DEBUG) out.println("server: "+e);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		if(DEBUG) out.println("server: Server Good Bye!");
		DD.ed.fireServerUpdate(new CommEvent(this, null, null, "LOCAL", "Server stopping"));
	}
	
	static public void main(String arg[]) throws SQLiteException {
		boolean directory_server_on_start = false;
		boolean data_server_on_start = true;
		boolean data_client_on_start = true;
		String guID="";
		if(arg.length>0) {
			Application.DELIBERATION_FILE = arg[0];
		}
		if(DEBUG) out.println("Opening database: "+Application.DELIBERATION_FILE);
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
		} catch (SQLiteException e1) {
			e1.printStackTrace();
		}
		Identity id = Identity.getCurrentIdentity();
		if (id!=null) guID = id.globalID;
		//id.globalOrgID = "humanitas";
		if(arg.length>1) {
			guID = arg[1];
		}
		if(DEBUG) System.out.println("My ID: "+guID);
		if (directory_server_on_start) {
			try {
				Application.ds = new DirectoryServer(DirectoryServer.PORT);
				Application.ds.start();
			}catch(Exception e) {
				System.exit(-1);
			}
		}
		try {
			Identity.listing_directories_inet.add(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),20046));
			Identity.listing_directories_string.add("127.0.0.1:20046");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		if (data_server_on_start) {
			//Identity id = new Identity();
			//id.globalID = guID;
			Application.as = new Server(id);
			Application.as.start();
			//server.setID(id);
		}
		if (data_client_on_start) {
			Application.ac = new Client();
			Application.ac.start();
		}
	}
	public static boolean isMyself(InetSocketAddress sock_addr) {
		for (InetSocketAddress sa : Server.serv_sock_addresses) {
			if(sa.equals(sock_addr)) return true;
		}
		return false;
	}
}
