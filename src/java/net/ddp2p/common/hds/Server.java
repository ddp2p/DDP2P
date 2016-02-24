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
 package net.ddp2p.common.hds;
 import static java.lang.System.out;
import static java.lang.System.err;
import static net.ddp2p.common.util.Util.__;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.HashSet;
import java.util.Random;
import java.util.Collections;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_PeerInstance;
import net.ddp2p.common.network.sim.DDP2P_NetworkInterface;
import net.ddp2p.common.plugin_data.D_PluginInfo;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.streaming.UpdateMessages;
import net.ddp2p.common.util.CommEvent;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DDP2P_ServiceThread;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
class ServerThread extends net.ddp2p.common.util.DDP2P_ServiceThread {
	private static final int MAX_SR = 100000;
	private static final boolean DEBUG = false;
	private static int cnt = 0;
	Socket s;
	Server server;
	String name;
	public String peer_ID;
	ServerThread(Socket s, Server server){
		super("TCP Server Thread", false);
		if(DEBUG) out.println("server thread: Created");
		this.s=s;
		this.server = server;
		name=cnt+""; cnt++;
	}
	public void _run() {
		this.setName("TCP Server Thread: "+name);
		try {
			__run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void __run(){
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
				if(!dec.fetchAll(is)){
					System.err.println("Buffer too small for receiving request!");
					continue;
				}
				ASNSyncRequest asr = new ASNSyncRequest();				
				asr.decode(dec);
				if(!asr.verifySignature()){
					DD.ed.fireServerUpdate(new CommEvent(this, null, s.getRemoteSocketAddress(), "Client", "Server Unsigned Sync Request received: "+asr));
					if(DEBUG)out.println("Server: Unsigned Request received: "+asr.toString());
					break; 
				}
				SocketAddress isa = s.getRemoteSocketAddress();
				DD.ed.fireServerUpdate(new CommEvent(this, null, isa, "Client", "Sync Request received: "+asr));
				Server.extractDataSyncRequest(asr, isa, this);
				if (DEBUG) out.println("Request received: "+asr.toString());
				if (asr.address != null) {
					asr.address.setLID (peer_ID = D_Peer.getLocalPeerIDforGID(asr.address.component_basic_data.globalID));
				}else{
					Application_GUI.warning(__("Peer does not authenticate itself"), __("Contact from unknown peer"));
					if(!DD.ACCEPT_DATA_FROM_UNSIGNED_PEERS) break;
				}
				SyncAnswer sa = UpdateMessages.buildAnswer(asr, peer_ID);
				byte[]msg = sa.encode();
				if(DEBUG) out.println("Answering: "+Util.byteToHexDump(msg, " ")+"::"+Util.trimmed(sa.toString(),300));
				DD.ed.fireServerUpdate(new CommEvent(this, null, s.getRemoteSocketAddress(), "Sharing", sa.toString()));
				s.getOutputStream().write(msg);
				DD.ed.fireServerUpdate(new CommEvent(this, null, s.getRemoteSocketAddress(), "Client", "Reply Sent"));
				if(DEBUG) out.println("Answered:"+msg.length);//+"::"+Util.byteToHex(msg, " "));
				s.getInputStream().read(sr);
				if(DEBUG) out.println("Closing: "+s);
				DD.ed.fireServerUpdate(new CommEvent(this, null, s.getRemoteSocketAddress(), "Client", "Closing connection"));
				s.close();
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
			} catch (P2PDDSQLException e) {
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
class ThreadAskPull extends net.ddp2p.common.util.DDP2P_ServiceThread {
	final static boolean _DEBUG = true;
	D_Peer pa;
	String peer_ID;
	ASNSyncRequest asr;
	SocketAddress sa;
	Object caller;
	/**
	 * If created pith a peer pa with blocking false, then it would recursively call itself!
	 * @param pa
	 * @param peer_ID
	 * @param asr
	 * @param sa
	 * @param caller
	 */
	ThreadAskPull(D_Peer pa, String peer_ID, ASNSyncRequest asr, SocketAddress sa, Object caller) {
		super("TCP Server ASK-Pull /new peer", true);
		this.pa = pa;
		this.peer_ID = peer_ID;
		this.asr = asr;
		this.sa = sa;
		this.caller = caller;
	}
	public void _run() {
		try {
			__run();
			Server.queried.remove(pa.getGIDH_force());
			if (pa.component_preferences.blocked) return;
			/**
			 * Run again the data extraction in case blocking is off
			 */
			Server.extractDataSyncRequest(asr, sa, caller);
		} catch (Exception e) {
			Server.queried.remove(pa.getGIDH_force());
			e.printStackTrace();
		}
	}
	public void __run() throws P2PDDSQLException{
		int i = 2;
		Object sync = __("Pull from")+" "+Util.trimmed(pa.component_basic_data.name);
		Object options[] = new Object[] {
				sync,
				__("Do not pull from it"),
				__("Use defaults"),
				__("Use defaults, do not ask again"),
				__("Block"),
				__("Discard")
				};
		i = Application_GUI.ask(
			__("Use new peer for synchronization?"),
			__("A new peer is contacting you. Do you want to pull from it, too?")+" \n"+
			__("Default for discarding:")+"     "+(DD.REJECT_NEW_ARRIVING_PEERS_CONTACTING_ME?__("Discard"):__("Do not discard"))+"\n"+
			__("Default for pulling is affected by the first 2 choices.")+"\n"+
			__("Default for pulling from new peer currently is:")+"     "+(DD.USE_NEW_ARRIVING_PEERS_CONTACTING_ME?__("Sync from this"):__("Do not sync from this"))+"\n"+
			__("Default for blocking (pushed data) is:")+"              "+(DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME?__("Blocked"):__("Not blocked"))+" \n"+
			__("Name of Peer contacting you is:")+"                     \""+Util.trimmed(pa.component_basic_data.name,30)+"\" \n"+
			__("Email of Peer contacting you is:")+"                    \""+Util.trimmed(pa.component_basic_data.emails,30)+"\" \n"+
			__("Address of Peer contacting you is:")+"                    \""+Util.trimmed(pa.getAddressesDesc(),30)+"\" \n"+
			__("The current slogan of the peer is:")+"                  \""+Util.trimmed(pa.component_basic_data.slogan,100)+"\"",
			options, sync, null);
		switch (i) {
		case 0: 
			pa.setBlocked(false);
			pa.setUsed(DD.USE_NEW_ARRIVING_PEERS_CONTACTING_ME = true);
			if (pa.getStatusLockWrite() == 0) {
				pa.assertReferenced();
				System.out.println("Server: __run: assertions in setUsed do not run");
			}
			peer_ID = D_Peer.save_external_instance(pa, asr.dpi); 
			break;
		case 1: 
			pa.setBlocked(DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME);
			pa.setUsed(DD.USE_NEW_ARRIVING_PEERS_CONTACTING_ME = false);
			peer_ID = D_Peer.save_external_instance(pa, asr.dpi);
			break;
		case 4: 
			pa.setBlocked(true);
			pa.setUsed(false);
			peer_ID = D_Peer.save_external_instance(pa, asr.dpi);
			break;
		case 5: 
			DD.REJECT_NEW_ARRIVING_PEERS_CONTACTING_ME = true;
			break;
		case 3: 
			DD.ASK_USAGE_NEW_ARRIVING_PEERS_CONTACTING_ME = false;
		case 2: 
		case Application_GUI.CLOSED_OPTION:
		default:
			pa.setBlocked(DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME);
			pa.setUsed(DD.USE_NEW_ARRIVING_PEERS_CONTACTING_ME);
			peer_ID = D_Peer.save_external_instance(pa, asr.dpi);
			break;
		}
	}
}
public class Server extends net.ddp2p.common.util.DDP2P_ServiceThread {
	public static final int TIMEOUT_Handler = 1000000;
	public static final int TIMEOUT_Server = 2000;
	public static final int TIMEOUT_Client_Data = 20000;
	public static int PORT = 45000;
	public static final int TIMEOUT_Client_wait_Server = 2000;
	public static final int TIMEOUT_Client_wait_Dir = 2000;
	public static final String DIR = "DIR";
	public static final String SOCKET = "Socket";
	public static int TIMEOUT_UDP_NAT_BORER = 20000;
	public static final long TIMEOUT_UDP_Reclaim = 2000;
	public static final int TIMEOUT_UDP_Announcement_Diviser = 30;
	public static boolean DEBUG = false;
	public static final boolean _DEBUG = true;
	Object lock = new Object();
	static ArrayList<InetSocketAddress> serv_sock_addresses = new ArrayList<InetSocketAddress>();
	ServerSocket ss;
	int threads = 0;
	static int MAX_THREADS = 10;
	void incThreads(){
		synchronized(lock){
			threads++;
		}
	}
	void decThreads(){
		synchronized (lock) {
			threads--;
			if (threads < MAX_THREADS) 
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
	/**
	 * GIDH of peers about which the user is currently asked on whether to enable them or not. 
	 */
	public static HashSet<String> queried = new HashSet<String>();
	/**
	 * 
	 * @param asr
	 * @param sa
	 * @param caller
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static boolean extractDataSyncRequest(ASNSyncRequest asr, SocketAddress sa, Object caller) throws P2PDDSQLException {
		if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: asr");	
		Calendar crt_date = Util.CalendargetInstance();
		String _crt_date = Encoder.getGeneralizedTime(crt_date);
		D_Peer received_peer = asr.address;
		if (received_peer != null) {
			String __peer_ID = null;
			DD.ed.fireServerUpdate(new CommEvent(caller, received_peer.component_basic_data.name, sa, "Client", "Peer Name decoded"));
			if ((received_peer != null) && (received_peer.getSignature() != null) &&
					((received_peer.getGID() != null) || (received_peer.getGIDH() != null))) {
				if (queried.contains(received_peer.getGIDH_force())) {
					if (_DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: asr");	
					return false; 
				}
				boolean verified_peer_success = received_peer.verifySignature();
				if (verified_peer_success) {
					if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: will verif");	
					D_Peer local_peer = D_Peer.getPeerByGID_or_GIDhash (
							received_peer.getGID(),
							received_peer.getGIDH(), 
							true, false, false, null);  
					if ((local_peer == null)) { 
						if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: local was null");	
						if (DD.ASK_USAGE_NEW_ARRIVING_PEERS_CONTACTING_ME) {
							queried.add(received_peer.getGIDH_force()); 
							received_peer.component_preferences.blocked = true; 
							received_peer.component_preferences.used = false; 
							new ThreadAskPull(received_peer, __peer_ID, asr, sa, caller).start();
							if (_DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: asking user");	
							return false;
						} else {
							if (DD.REJECT_NEW_ARRIVING_PEERS_CONTACTING_ME) {
								if (_DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest:  rejecting automatically");	
								return false;
							}
							received_peer.component_preferences.blocked = DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME;
							received_peer.component_preferences.used = DD.USE_NEW_ARRIVING_PEERS_CONTACTING_ME;
							local_peer = D_Peer.getPeerByGID_or_GIDhash (
									received_peer.getGID(),
									received_peer.getGIDH(), 
									true, true, true, null); 
							local_peer.loadRemote(received_peer, null, null);
							if (asr.dpi != null) local_peer.integratePeerInstance(asr.dpi);
							__peer_ID = Util.getStringID(local_peer.storeSynchronouslyNoException());
							if (local_peer.dirty_any()) local_peer.storeRequest();
							local_peer.releaseReference();
							if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: get a peer_ID: "+__peer_ID);	
						}
					} else {
						if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: local not nulll!");	
						local_peer = D_Peer.getPeerByPeer_Keep(local_peer);
						if (local_peer.loadRemote(received_peer, null, null)) {
							local_peer.setArrivalDate(crt_date, _crt_date);
							if (asr.dpi != null) local_peer.integratePeerInstance(asr.dpi);
							__peer_ID = local_peer.getLIDstr_force(); 
							if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: saved peer_ID! "+__peer_ID);	
						} else {
							__peer_ID = local_peer.getLIDstr_force();
						}
						if (local_peer.dirty_any()) local_peer.storeRequest();
						local_peer.releaseReference();
					}
				} 
			} 
			if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: peer_ID! "+__peer_ID);	
			if (__peer_ID == null) { 
				if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: discard message from peer=null!");	
			} else {
				if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: distrib info: "+Util.nullDiscrimArray(asr.plugin_info,"|||")+"; ");	
				D_PluginInfo.recordPluginInfo(received_peer.getInstance(), asr.plugin_info, received_peer.component_basic_data.globalID, __peer_ID);
				if (asr.plugin_msg != null) {
					try {
						if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: message "+asr.plugin_msg);
						asr.plugin_msg.distributeToPlugins(received_peer.component_basic_data.globalID);
					} catch (ASN1DecoderFail e) {
						e.printStackTrace();
					}
				} else {
					if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: no message!");				
				}
			}
			if ((__peer_ID != null) || (DD.ACCEPT_STREAMING_SYNC_REQUEST_PAYLOAD_DATA_FROM_UNKNOWN_PEERS)) {
				if (asr.pushChanges != null) {
					RequestData _rq = new RequestData();
					InetSocketAddress isa = (InetSocketAddress)sa;
					String address_ID = D_Peer.getAddressID(isa, __peer_ID);
					try {
						D_PeerInstance incoming_pi = asr.pushChanges.peer_instance;
						if (incoming_pi == null) {incoming_pi = asr.dpi;}
						if (incoming_pi == null) {incoming_pi = new D_PeerInstance(); incoming_pi.peer_instance = received_peer.instance;}
						UpdateMessages.integrateUpdate(
								asr.pushChanges,
								isa, caller, received_peer.getGID(), incoming_pi, __peer_ID, address_ID, _rq,
								asr.address,
								false);
					} catch (ASN1DecoderFail e) {
						e.printStackTrace();
					}
				}
			}
		} else { 
			if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nServer: extractDataSyncRequest: no peer address for plugin data!");
		}
		return true;
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
			Application.setPeerTCPPort(ss.getLocalPort());
			UDPServer.directoryAnnouncement = null;
		}
		if(DEBUG) out.println("END Server.try_connect");
	}
	public Server() throws P2PDDSQLException {
		super("TCP Server", false);
		if(DEBUG) out.println("Start Server");
		try_connect(PORT);
	}
	public Server(int port) throws P2PDDSQLException {
		super("TCP Server", false);
		if(DEBUG) out.println("Start Server port="+port);
		try_connect(port);
	}
	public Server(Identity peer_id) throws P2PDDSQLException {
		super("TCP Server", false);
		if(DEBUG) out.println("Start Server peer_id="+peer_id);
		try_connect(PORT);
	}
	public Server(int port, Identity peer_id) throws P2PDDSQLException {
		super("TCP Server", false);
		if(DEBUG) out.println("Start Server port="+port+" id="+peer_id);
		try_connect(port);
	}
	public static void prepareLocalDomainsLists(int port) throws SocketException {
		synchronized (Application.getMy_Server_Domains()) {
			if ((Application.getMy_Server_Domains().size() > 0) || (Application.getMy_Server_Domains_Loopback().size() > 0)) return;
			if (DEBUG) out.println("END Server.detectDomain");
			if (DD.ONLY_IP4) System.setProperty("java.net.preferIPv4Stack", "true");
			Enumeration<DDP2P_NetworkInterface> nets = DDP2P_NetworkInterface.getNetworkInterfaces();
			for (DDP2P_NetworkInterface netint : Collections.list(nets)) {
				if (DEBUG) out.printf("server: Display name: %s\n", netint.getDisplayName());
				if (! netint.isUp()) {
					if(DEBUG) out.printf("server: Interface down\n");
					continue;
				}
				if (DEBUG) out.printf("server: Name: %s (loopback: %s; p2p:%s; up: %s, v: %s, m:%s)\n", netint.getName(),
						""+netint.isLoopback(),
						""+netint.isPointToPoint(),
						""+netint.isUp(),
						""+netint.isVirtual(),
						""+netint.supportsMulticast()
						);
				Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
				for (InetAddress inetAddress : Collections.list(inetAddresses)) {
					if(DEBUG) out.printf("server: inetAddress: %s\n", inetAddress);
					serv_sock_addresses.add(new InetSocketAddress(inetAddress, port));
					if (! netint.isLoopback()) {
						if(DEBUG) out.printf("server: Interface is not loopback\n");
						Application.getMy_Server_Domains().add(inetAddress);
					} else {
						Application.getMy_Server_Domains_Loopback().add(inetAddress);
					}
				}
			}
		}
	}
	/**
	 * Initializes Identity.domain with the current inetAddress of the machine.
	 * @throws SocketException
	 */
	public static void detectDomain() throws SocketException{
		detectDomain(Application.getPeerTCPPort());
	}
	private final static Object domainsDetectionThread_monitor = new Object();
	public static DDP2P_ServiceThread domainsDetectionThread = null;
	/**
	 * Fill the domain cache: 
	 * Identity.my_server_domains
	 * Identity.my_server_domains_loopback
	 * serv_sock_addresses
	 * 
	 * synchronized on Identity.my_server_domains
	 * 
	 * abandons if a detection has already happened (this should change, and redone in a thread)
	 * 
	 * @param port
	 * @throws SocketException
	 */
	public static void detectDomain(int port) throws SocketException{
		if (port <= 0) port = Server.PORT;
		try {
			prepareLocalDomainsLists(port);
		} catch(Exception e) {e.printStackTrace();}
		UDPServer.directoryAnnouncement = null;
		if (DEBUG) out.println("END Server.detectDomain: domains="+Application.getMy_Server_Domains().size()+
				", loopdomains="+Application.getMy_Server_Domains_Loopback().size());
		synchronized(domainsDetectionThread_monitor) {
			if (domainsDetectionThread == null) {
				domainsDetectionThread = new DDP2P_ServiceThread("Domain detection: "+Util.getGeneralizedTime(), true) {
					public void _run() {
						for (;!stop;) {
							try {
								synchronized(this) {
									this.wait(DD.DOMAINS_UPDATE_WAIT);
								}
							} catch (InterruptedException e) {
							}
							if (stop) {
								return;
							}
							try {
								if (DEBUG) System.out.println("Server:<Thread>run: detectDomain");
								try {
									prepareLocalDomainsLists(Application.getPeerTCPPort());
								} catch(Exception e) {e.printStackTrace();}
								net.ddp2p.common.data.HandlingMyself_Peer.updateAddress(net.ddp2p.common.data.HandlingMyself_Peer.get_myself_with_wait());
								if (DEBUG) System.out.println("Server:<Thread>run: updateAddress");
								Application_GUI.ThreadsAccounting_ping("Detected domains");
							} catch (Exception e) {
								e.printStackTrace();
								Application_GUI.ThreadsAccounting_ping("Error detect: "+e.getLocalizedMessage());
							}
						}
					}
				};
				domainsDetectionThread.start();
			}
		}
	}
	/**
	 * If "preferred" is true, on success add this directory's index as the "Identity.preferred_directory_idx".
	 * 
	 * On failure exceptions adds this to "DD.directories_failed"
	 * @param da
	 * @param adr (should have set inetSockAddr)
	 * @param preferred
	 * @return 
	 * return Returns true on success.
	 */
	public static boolean announceMyselfToDirectory(DirectoryAnnouncement da, Address adr, boolean preferred) {
		String dir_address = null;
		InetSocketAddress dir = adr.inetSockAddr;
		da.setAddressVersionForDestination(adr);
		if (dir == null) {
			if (_DEBUG) out.println("Server:announceMyselfToDirectories: quit announce to null adr.inetSockAddr : "+adr);
			return false;
		}
		if (DEBUG) out.println("Server:announceMyselfToDirectories: announce to: "+dir);
		try {
			dir_address = adr.ipPort();
			byte msg[] = da.encode();
			if (DEBUG) out.println("Server:announceMyselfToDirectories: sent length: "+msg.length);
			if (DEBUG) {
				Decoder d = new Decoder(msg);
				DirectoryAnnouncement _da = new DirectoryAnnouncement(d);
				if (DEBUG) out.println("Server:announceMyselfToDirectories: actually sent length: "+_da);
			}
			if (DEBUG) out.println("Server:announceMyselfToDirectories: sent: "+da);//+"\n"+Util.byteToHex(msg," "));
			Socket s = new Socket();
			s.setSoTimeout(TIMEOUT_Client_wait_Dir);
			s.connect(dir, TIMEOUT_Client_wait_Dir);
			s.getOutputStream().write(msg);
			byte answer[] = new byte[200];
			if (DEBUG) out.println("Server:announceMyselfToDirectories: Waiting answer!");
			int alen = s.getInputStream().read(answer);
			if (DEBUG) out.println("Server:announceMyselfToDirectories: Got answer: "+Util.byteToHex(answer, 0, alen, " "));
			Decoder answer_dec = new Decoder(answer);
			try {
				DirectoryAnnouncement_Answer ans = new DirectoryAnnouncement_Answer(answer_dec);
				if (DEBUG) out.println("Server:announceMyselfToDirectories: Directory Answer: "+ans);
			} catch(Exception e){
					e.printStackTrace();}
			s.close();
			if (preferred) {
				Identity.setPreferred_Directory_IDX(Identity.getListing_directories_inet().indexOf(dir));
			}
			if (Application.directory_status != null) {
				Application.directory_status.setTCPOn(dir_address, new Boolean(true), null);
			} else {
				System.out.println("Server:announceMyselfToDirs:Tcp success, no display");
			}
			return true;
		} catch (Exception e) {
			try {DD.directories_failed.add(dir);} catch(Exception e2) {e2.printStackTrace();}
			if (Application.directory_status != null)
				Application.directory_status.setTCPOn(dir_address, new Boolean(false), e);
			if (DEBUG) err.println("Server: "+__("Announcing myself to directory:")+dir+" "+e.getLocalizedMessage());
			if (DEBUG) err.println("Server: "+__("Error announcing myself to directory:")+dir);
			if (DEBUG || DD.DEBUG_TODO) e.printStackTrace();
		}
		return false;
	}
	/**
	 * TCP Send to each directory in the list:
	 * Identity.listing_directories_addr
	 * sets address version separately for each destinations.
	 * 
	 * sets the: Identity.preferred_directory_idx as the index for the first successful dir
	 * 
	 * @param da A prepared Directory Announcement
	 */
	public static void announceMyselfToDirectories(DirectoryAnnouncement da) {
		if(DEBUG) out.println("Server:announceMyselfToDirectories:");
		boolean first = true; 
		for (Address adr : Identity.getListing_directories_addr()) {
			if (announceMyselfToDirectory(da, adr, first))
				first = false;
		}
		if (DEBUG) out.println("Server:announceMyselfToDirectories: Done!");
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
	public void _run() {
		DD.ed.fireServerUpdate(new CommEvent(this, null, null, "LOCAL", "Server starting"));
		if(DEBUG) out.println("BEGIN Server.try_connect: detectDomain");
		try{detectDomain();}catch(Exception e){}
		UDPServer.announceMyselfToDirectories();
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
	static public void main(String arg[]) throws P2PDDSQLException {
		boolean directory_server_on_start = false;
		boolean data_server_on_start = true;
		boolean data_client_on_start = true;
		String guID="";
		if(arg.length>0) {
			Application.DELIBERATION_FILE = arg[0];
		}
		if(DEBUG) out.println("Opening database: "+Application.DELIBERATION_FILE);
		try {
			Application.setDB(new DBInterface(Application.DELIBERATION_FILE));
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		Identity id = Identity.getCurrentConstituentIdentity();
		if (id != null) guID = id.getPeerGID();
		if(arg.length>1) {
			guID = arg[1];
		}
		if(DEBUG) System.out.println("My ID: "+guID);
		if (directory_server_on_start) {
			try {
				Application.setG_DirectoryServer(new DirectoryServer(DirectoryServer.PORT));
				Application.getG_DirectoryServer().start();
			}catch(Exception e) {
				System.exit(-1);
			}
		}
		try {
			Identity.getListing_directories_inet().add(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),20046));
			Identity.getListing_directories_string().add("127.0.0.1:20046");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		if (data_server_on_start) {
			Identity id2 = Identity.getCurrentPeerIdentity_QuitOnFailure();
			Application.setG_TCPServer(new Server(id2));
			Application.getG_TCPServer().start();
		}
		if (data_client_on_start) {
			Application.setG_PollingStreamingClient(ClientSync.startClient());
		}
	}
	public static boolean isMyself(InetSocketAddress sock_addr) {
		for (InetSocketAddress sa : Server.serv_sock_addresses) {
			if(sa.equals(sock_addr)) return true;
		}
		return false;
	}
}
