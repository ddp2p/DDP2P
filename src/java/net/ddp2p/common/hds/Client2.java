package net.ddp2p.common.hds;
import static java.lang.System.err;
import static java.lang.System.out;
import static net.ddp2p.common.util.Util.__;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Hashtable;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.streaming.UpdateMessages;
import net.ddp2p.common.util.CommEvent;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class Client2 extends net.ddp2p.common.util.DDP2P_ServiceThread  implements IClient{
	private static final boolean _DEBUG = true;
	public static final boolean DEBUG = false;
	public static final Object conn_monitor = new Object();
	private static boolean recentlyTouched;
	static int peersToGo = -1;
	boolean turnOff = false;
	public Object wait_lock = new Object();
	public static Connections g_Connections = null;
	public void turnOff() {
		if (ClientSync.DEBUG) System.out.println("Client2: turnOff");
		turnOff = true;
		this.interrupt();
	}
	public Client2() {
		super ("Client 2", false);
		boolean DEBUG = ClientSync.DEBUG;
		if (DEBUG) System.out.println("Client2: <init>");
		try {
			ClientSync.buildStaticPayload();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public static void startConnections() {
		synchronized (Client2.conn_monitor) {
			if (g_Connections == null)
				g_Connections  = new Connections(Application.getDB());
		}
	}
	public void _run() {
		synchronized(wait_lock) {
			try {
				wait_lock.wait(DD.PAUSE_BEFORE_CONNECTIONS_START);
				if (ClientSync.DEBUG || DD.DEBUG_LIVE_THREADS) System.out.println("Client2: run: connections go");
				startConnections();
				wait_lock.wait(DD.PAUSE_BEFORE_CLIENT_START);
			} catch (InterruptedException e) {
				return;
			}
		}
		if (ClientSync.DEBUG || DD.DEBUG_LIVE_THREADS) System.out.println("Client2: run: go");
		if (ClientSync.DEBUG) System.out.println("Client2: run: start");
		try {__run();} catch(Exception e) {e.printStackTrace();}
		if (ClientSync.DEBUG) System.out.println("Client2: run: done");
	}
	public void __run(){
		int cnt = 0;
		for (;;) {
			Application_GUI.ThreadsAccounting_ping("Cycle: "+peersToGo);
			if (ClientSync.DEBUG) System.out.println("Client2: _run: next="+peersToGo);
			if (turnOff) {
				if(ClientSync.DEBUG) System.out.println("Client2: _run: turnOff 1");
				break;
			}
			if (Application.getG_UDPServer() == null) {
				if (ClientSync._DEBUG) System.out.println("Client2: _run: why no server I?");
				break;
			}
			if (try_wait(peersToGo)) continue; 
			if (turnOff) {
				if (ClientSync.DEBUG) System.out.println("Client2: _run: turnOff 2");
				break;
			}
			if (Application.getG_UDPServer() == null) {
				if (ClientSync._DEBUG) System.out.println("Client2: _run: why no server II?");
				break;
			}
			if (peersToGo < 0) {
				cnt++;
				if (ClientSync.DEBUG) System.out.println("Client2: _run: sync myself: round: "+cnt);
				if (cnt%ClientSync.CYCLES_PAYLOAD == 0){
					ClientSync.initPayload();
				}
				synchronize_Myself(); 
				peersToGo = 0;
				continue;
			}
			Connection_Peer pc = Connections.getConnectionAtIdx(peersToGo);
			if (pc != null) {
				if (ClientSync.DEBUG) System.out.println("Client2: _run: handle "+peersToGo);
				if (ClientSync.DEBUG && !Connections.DEBUG) System.out.println("Client2: _run: handle "+pc);
				handlePeer(pc);
			} else {
				if (ClientSync.DEBUG) System.out.println("Client2: _run: handle null");
			}
			peersToGo ++;
			if (peersToGo > Connections.peersAvailable) peersToGo = -1;
		}
	}
	/**
	 * Return true on wait due to busy threads (to know to retry wait again)
	 * @param crt used to tell when a cycle is done, to sleep some longer
	 * @return
	 */
	private boolean try_wait(int crt) {
		if (ClientSync.DEBUG) System.out.println("Client2: try_wait: "+crt+"/"+Connections.peersAvailable);
		if ((Application.getG_UDPServer() != null) && (Application.getG_UDPServer().getThreads() > UDPServer.MAX_THREADS/2)){
			try {
				if (ClientSync._DEBUG) System.out.println("Client2: try_wait: overloaded threads = "+Application.getG_UDPServer().getThreads());
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Will Sleep: "+ClientSync.PAUSE));
				synchronized (wait_lock) {
					wait_lock.wait(ClientSync.PAUSE);
				}
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Wakes Up"));
				return true;
			} catch (InterruptedException e2) {
				return true;
			}
		}
		try {
			if ((! Client2.recentlyTouched) && (crt >= Connections.peersAvailable)) {
				if (ClientSync.DEBUG || DD.DEBUG_LIVE_THREADS) out.println("Client2: try_wait: Will wait ms: "+ClientSync.PAUSE+" p="+Connections.peersAvailable);
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Will Sleep: "+ClientSync.PAUSE));
				synchronized(wait_lock) {
					wait_lock.wait(ClientSync.PAUSE);
				}
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Wakes Up"));
			}
			Client2.recentlyTouched = false;
		} catch (InterruptedException e) {
			if(ClientSync.DEBUG) e.printStackTrace();
		}
		if (ClientSync.DEBUG) System.out.println("Client2: try_wait: done");
		return false;
	}
	/**
	 * If never tried before, will try first attempting sockets, then DIRs
	 * If tried before, will try first previously successfully handled sockets, then DIRS
	 *     otherwise will again try everything.
	 * Will try TCP on sockets, then UDP on sockets, or UDP on DIRs
	 * @param pc
	 */
	private boolean handlePeer(Connection_Peer pc) {
		boolean result;
		if (ClientSync.DEBUG) System.out.println("Client2: handlePeer");
		if (! pc.isContactedSinceStart() || ! pc.isLastContactSuccessful()) {
			result = handlePeerNotRecentlyContacted(pc);
		} else {
			pc.setLastContactSuccessful(false);
			boolean success = handlePeerRecent(pc);
			if (success) { 
				pc.setLastContactSuccessful(true);
				pc.setContactedSinceStart(true);
			}
			result = success;
		}
		if (ClientSync.DEBUG) System.out.println("Client2: handlePeer: done");
		return result;
	}
	/**
	 * Attempts to contact by TCP, and sets success flags in Connections_Peer_Socket and Connection_Instance
	 * @param peer_sockets
	 * @param pc
	 * @param ci
	 * @param retry
	 * @param instance
	 * @return
	 */
	public boolean trySocketsListTCP(ArrayList<Connections_Peer_Socket> peer_sockets, Connection_Peer pc, Connection_Instance ci, boolean retry, String instance) {
		for (int k = 0; k < peer_sockets.size(); k ++) {
			boolean success = false;
			Connections_Peer_Socket ps = peer_sockets.get(k);
			if (retry || ps.last_contact_successful_TCP) {
				Socket sock = ps.tcp_connection_open;
				if (sock == null || sock.isClosed())
					sock = try_TCP_connection(pc, ps, instance); 
				if (sock != null) { 
					boolean close = ! Connections_Peer_Socket.OPEN_CONNECTIONS;
					success = Client2.transfer_TCP(pc, ps, sock, close);
					if (! close)
						ps.tcp_connection_open =  sock; 
				}
				if (success) {
					ps.setLastContactDateTCP();
					if (ci != null) {
						ci.setContactedSinceStart_TCP(true);
						ci.setLastContactSuccessful_TCP(true);
					}
					if (ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: done TCP "+success);
					return success;
				} else {
					ps.last_contact_successful_TCP = false;
				}
			}
		}
		if (ci != null) ci.setLastContactSuccessful_TCP(false);
		else pc.setLastContactSuccessful(false);
		return false;
	}
	/**
	 * 
	 * @param peer_sockets
	 * @param pc
	 * @param aup
	 *  the target domain/port will be added here
	 * @param retry
	 *  set to true for a peer instance that was not successfully contacted recently
	 * @return
	 * Always returns true if a ping was sent (no way to know success!)
	 */
	public boolean trySocketsListUDP(ArrayList<Connections_Peer_Socket> peer_sockets, Connection_Peer pc, ASNUDPPing aup, boolean retry) {
		boolean DEBUG = Client2.DEBUG || ClientSync.DEBUG || DD.DEBUG_COMMUNICATION_STUN;
		boolean result = false;
		if (DEBUG) System.out.println("Client2: trySocketsListUDP: start #"+peer_sockets.size());
		for (int k = 0; k < peer_sockets.size(); k ++) {
			Connections_Peer_Socket ps = peer_sockets.get(k);
			if (DEBUG) System.out.println("Client2: trySocketsListUDP: INET sock ping to "+ps);
			if (retry || ps.worksRecently()) {
				if ((ps.addr == null) || (ps.addr.addr == null)) {
					if (DEBUG) System.out.println("Client2: trySocketsListUDP:  INET sock skip null adr");
					continue;
				}
				aup.peer_domain = ps.addr.addr.domain;
				aup.peer_port = ps.addr.addr.udp_port;
				if (DEBUG) System.out.println("Client2: trySocketsListUDP:  INET sock do ping: "+aup);
				result |= try_UDP_connection_socket(pc, ps, aup.encode());
			}
		}
		return result;		
	}
	/*// integrate these in socket
	public boolean tryAddressListUDP(ArrayList<Address> peer_sockets, Connection_Peer pc, ASNUDPPing aup, boolean retry) {
		for (int k = 0; k < peer_sockets.size(); k ++) {
			Address ps = peer_sockets.get(k);
			if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: trySocketsListUDP: INET sock ping to "+ps);
			if (retry || (ps.contacted_since_start_UDP && ps.last_contact_successful_UDP)) {
				ps.last_contact_successful_UDP = false;
				if ((ps.addr == null) || (ps.addr.ad == null)) {
					if(DEBUG||ClientSync.DEBUG) System.out.println("Client2: trySocketsListUDP:  INET sock skip null adr");
					continue;
				}
				aup.peer_domain = ps.addr.ad.domain;
				aup.peer_port = ps.addr.ad.udp_port;
				if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: trySocketsListUDP:  INET sock do ping: "+aup);
				try_UDP_connection_socket(pc, ps, aup.encode());
			}
		}
		return true;
	}
	*/
	/**
	 * For not yet contacted peers. 
	 * Request updating the supernode addresses with "Connections.update_supernode_address_request"
	 * 		Without TCP this should not be done on each run but only on each second run, since if it was requested last time,
	 * 		then it was not yet possible to use the last addresses it works.
	 * If DD.ClientTCP, try first TCP sockets (shared, then instances). On success return true;
	 * 
	 * Next, if DD.ClientUDP, for each instance i,
	 * 		ping the sockets,*
	 * 		ping the shared directories, by requesting instance i, *
	 * 		ping the specialized directories of instance i (if any)
	 * 
	 * @param pc
	 * @return
	 */
	private boolean handlePeerNotRecentlyContacted(Connection_Peer pc) {
		boolean DEBUG = Client2.DEBUG || ClientSync.DEBUG || DD.DEBUG_COMMUNICATION_STUN;
		if (DEBUG) System.out.println("Client2: handlePeerNotRecentlyContacted handle "+pc);
		boolean retry = true;
		boolean success = false;
		if (! DD.ClientTCP && pc.isJustRequestedSupernodesAddresses())  {
			retry = false;
			pc.setJustRequestedSupernodesAddresses(false);
		} else {
			pc.setJustRequestedSupernodesAddresses(true);
		}
		if (retry) Connections.update_supernode_address_request(pc); 
		pc.setLastContactSuccessful(false);
		retry = true;
		if (DD.ClientTCP) {
			if (DEBUG) System.out.println("Client2: handlePeerNotRecentlyContacted: try TCP");
			Connection_Instance _ci = pc.getInstanceConnection(null);
			success = trySocketsListTCP(pc.shared_peer_sockets, pc, _ci, retry, null) || success;
			if (success) return success;
			pc.sortInstances();
			for (Connection_Instance ci : pc.instances_AL) {
				boolean success_inst;
				success = (success_inst = trySocketsListTCP(ci.peer_sockets, pc, ci, retry, ci.dpi.peer_instance)) || success;
				if (success) return success;
				success = (success_inst = trySocketsListTCP(ci.peer_sockets_transient, pc, ci, retry, ci.dpi.peer_instance)) || success;
				if (success) return success;
			}
		} else {
			if (DEBUG) System.out.println("Client2: handlePeerNotRecentlyContacted: not trying TCP");
		}
		if (DD.ClientUDP) {
			if (DEBUG) System.out.println("Client2: handlePeerNotRecentlyContacted: try UDP");
			ASNUDPPing aup = preparePing(pc.getGID(), null);
			if (DEBUG) System.out.println("Client2: handlePeerNotRecentlyContacted: prepared ping = "+aup);
			for (Connection_Instance ci : pc.instances_AL) {
				if (DEBUG) System.out.println("Client2: handlePeerNotRecentlyContacted: try instance: "+ci);
				aup.peer_instance = ci.dpi.peer_instance;
				ci.setPingPending_UDP();
				trySocketsListUDP(ci.peer_sockets, pc, aup, retry);
				trySocketsListUDP(ci.peer_sockets_transient, pc, aup, retry);
				for (int k = 0; k < pc.getSharedPeerDirectories().size(); k ++) {
					Connections_Peer_Directory pd = pc.getSharedPeerDirectories().get(k);
					if (DEBUG) System.out.println("Client2: handlePeerNotRecentlyContacted: try shared directory: "+pd);
					if (retry || pd.recentlyContacted(ci.dpi.peer_instance)) { 
						pd.setPeerPingPendingUDP(ci.dpi.peer_instance);
						try_UDP_connection_directory(pc, pd, aup, ci.dpi.peer_instance);
						if (Application.getG_UDPServer() != null) {
							if (DEBUG) System.out.println("Client2: handlePeerNotRecentlyContacted: no server");
							return false;
						}
					}
				}
				for (int k = 0; k < ci.peer_directories.size(); k ++) {
					Connections_Peer_Directory pd_inst = pc.getSharedPeerDirectories().get(k);
					if (DEBUG) System.out.println("Client2: handlePeerNotRecentlyContacted: try instance directory: "+pd_inst);
					if (retry || pd_inst.recentlyContacted(ci.dpi.peer_instance)) { 
						pd_inst.setPeerPingPendingUDP(ci.dpi.peer_instance);
						try_UDP_connection_directory(pc, pd_inst, aup, ci.dpi.peer_instance);
					}
				}
			}
			if (DEBUG) System.out.println("Client2: handlePeerNotRecentlyContacted: try UDP done");
		}
		if (DEBUG) System.out.println("Client2: handlePeerNotRecentlyContacted: done");
		return false;
	}
	private boolean handlePeerRecent(Connection_Peer pc) {
		if (ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: "+pc);
		boolean success = false;
		boolean retry = false;
		pc.setLastContactSuccessful(false);
		if (DD.ClientTCP) {
			if(ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: try TCP");
			success = trySocketsListTCP(pc.shared_peer_sockets, pc, null, retry, null) || success;
			if (success) return success;
			pc.sortInstances();
			for (Connection_Instance ci : pc.instances_AL) {
				ArrayList<Connections_Peer_Socket> peer_sockets_list = new ArrayList<Connections_Peer_Socket>(); 
				peer_sockets_list.addAll(ci.peer_sockets);
				peer_sockets_list.addAll(ci.peer_sockets_transient);
				Connections_Peer_Socket[] peer_sockets_arr = peer_sockets_list.toArray(new Connections_Peer_Socket[0]); 
				Arrays.sort(peer_sockets_arr, new Comparator<Connections_Peer_Socket>(){
					@Override
					public int compare(Connections_Peer_Socket o1,
							Connections_Peer_Socket o2) {
						if (o1.contacted_since_start_TCP && !o2.contacted_since_start_TCP) return -1;
						if (!o1.contacted_since_start_TCP && o2.contacted_since_start_TCP) return 1;
						if (!o1.contacted_since_start_TCP && !o2.contacted_since_start_TCP) return 0;
						return o1._last_contact_date_TCP.compareTo(o2._last_contact_date_TCP);
					}});
				peer_sockets_list = new ArrayList<Connections_Peer_Socket>(Arrays.asList(peer_sockets_arr));
				boolean success_inst; 
				success = (success_inst = trySocketsListTCP(peer_sockets_list, pc, ci, retry, ci.dpi.peer_instance)) || success;
				if (success) return success;
			}
		}
		if (DD.ClientUDP) {
			if (ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: try UDP");
			ASNUDPPing aup = preparePing(pc.getGID(), null);
			for (Connection_Instance ci : pc.instances_AL) {
				aup.peer_instance = ci.dpi.peer_instance;
				trySocketsListUDP(ci.peer_sockets, pc, aup, retry);
				trySocketsListUDP(ci.peer_sockets_transient, pc, aup, retry);
				for (int k = 0; k < pc.getSharedPeerDirectories().size(); k ++) {
					Connections_Peer_Directory pd = pc.getSharedPeerDirectories().get(k);
					if (retry || pd.recentlyContacted(ci.dpi.peer_instance)) { 
						pd.setPeerPingPendingUDP(ci.dpi.peer_instance);
						try_UDP_connection_directory(pc, pd, aup, null);
					}
				}
				for (int k = 0; k < ci.peer_directories.size(); k ++) {
					Connections_Peer_Directory pd_inst = pc.getSharedPeerDirectories().get(k);
					if (retry || pd_inst.recentlyContacted(ci.dpi.peer_instance)) { 
						pd_inst.setPeerPingPendingUDP(ci.dpi.peer_instance);
						try_UDP_connection_directory(pc, pd_inst, aup, ci.dpi.peer_instance);
					}
				}
			}
		}
		if (ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: done "+success);
		return success;
	}
	private static boolean transfer_TCP(Connection_Peer pc, Connections_Peer_Socket ps, Socket client_socket, boolean close) {
		if (ClientSync.DEBUG) System.out.println("Client2: transfer_TCP: "+ps);
		String peer_ID = pc.peer.getLIDstr();
		String peer_name = pc.getName();
		String global_peer_ID = pc.getGID();
		boolean filtered = pc.peer.getFiltered();
		String instance = pc.getInstance(ps);
		String _lastSnapshotString = pc.peer.getLastSyncDate(instance);
		Calendar _lastSnapshot = pc.peer.getLastSyncDateCalendar(instance);
		Application_GUI.peer_contacts_update();
		if (Application.peers != null) Application.peers.setConnectionState(peer_ID, DD.PEERS_STATE_CONNECTION_TCP);
		if (ClientSync.DEBUG) out.println("Client2: transfer_TCP: Connected!");
		DD.ed.fireClientUpdate(new CommEvent(Application.getG_PollingStreamingClient(), peer_name, client_socket.getRemoteSocketAddress(), "Server", "Connected"));
		ASNSyncRequest sr = ClientSync.buildRequest(_lastSnapshotString, _lastSnapshot, peer_ID);
		if (filtered) sr.orgFilter=UpdateMessages.getOrgFilter(peer_ID);
		sr.sign();
		try {
			byte[] msg = sr.encode();
			if(ClientSync.DEBUG) out.println("Client2: transfer_TCP: Sync Request sent: "+Util.byteToHexDump(msg, " ")+"::"+sr);
			client_socket.getOutputStream().write(msg);
			DD.ed.fireClientUpdate(new CommEvent(Application.getG_PollingStreamingClient(), peer_name, client_socket.getRemoteSocketAddress(), "Server", "Request Sent"));
			byte update[] = new byte[Client1.MAX_BUFFER];
			if(ClientSync.DEBUG) out.println("Waiting data from socket: "+client_socket+" on len: "+update.length+" timeout ms:"+Server.TIMEOUT_Client_Data);
			client_socket.setSoTimeout(Server.TIMEOUT_Client_Data);
			InputStream is=client_socket.getInputStream();
			int len = is.read(update);
			if (len > 0){
				if(ClientSync.DEBUG) err.println("Client2: transfer_TCP: answer received length: "+len);
				DD.ed.fireClientUpdate(new CommEvent(Application.getG_PollingStreamingClient(), peer_name, client_socket.getRemoteSocketAddress(), "Server", "Answered Received"));
				Decoder dec = new Decoder(update,0,len);
				System.out.println("Got first msg size: "+len);//+"  bytes: "+Util.byteToHex(update, 0, len, " "));
				if (!dec.fetchAll(is)) {
					System.err.println("Buffer too small for receiving update answer!");
					return false;
				}
				len = dec.getMSGLength();
				RequestData rq = new RequestData();
				integrateUpdate(update,len, (InetSocketAddress)client_socket.getRemoteSocketAddress(), Application.getG_PollingStreamingClient(), global_peer_ID, pc.peer.getLIDstr(), Util.getStringID(ps.address_LID), rq, pc.peer);
				if(ClientSync.DEBUG) err.println("Client2: transfer_TCP: answer received rq: "+rq);
			}else{
				if(ClientSync.DEBUG) out.println("Client2: transfer_TCP: No answered received!");
				DD.ed.fireClientUpdate(new CommEvent(Application.getG_PollingStreamingClient(), peer_name, client_socket.getRemoteSocketAddress(), "Server", "TIMEOUT_Client_Data may be too short: No Answered Received"));
			}
		} catch (SocketTimeoutException e1) {
			if(ClientSync.DEBUG) out.println("Client2: transfer_TCP: Read done: "+e1);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (close) {
			try {
				if(ClientSync.DEBUG) out.println("Client2: transfer_TCP: will close socket");
				client_socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(ClientSync.DEBUG) out.println("Client2: transfer_TCP: done peer");
		return true;
	}
	public static void integrateUpdate(byte[] update, int len, InetSocketAddress s_address, Object src, 
			String global_peer_ID, String peer_ID, String address_ID, RequestData rq, D_Peer peer) throws ASN1DecoderFail, P2PDDSQLException {
		if(ClientSync.DEBUG) err.println("Client2: integrateUpdate: will integrate update: "+update.length+" datalen="+len+"::");
		if(ClientSync.DEBUG) err.println("Client2: integrateUpdate:Update: "+Util.byteToHexDump(update,len));
		SyncAnswer asa = new SyncAnswer();
		Decoder dec = new Decoder(update, 0, len);
		if(ClientSync.DEBUG) err.println("Client2: integrateUpdate: will decode");
		asa.decode(dec);
		if(ClientSync.DEBUG) out.println("Client2: integrateUpdate: Got answer: "+asa.toString());
		UpdateMessages.integrateUpdate(asa, s_address, src, global_peer_ID, asa.peer_instance, peer_ID, address_ID, rq, peer, true);
		if(ClientSync.DEBUG) err.println("Client2: integrateUpdate: done");
	}
	private ASNUDPPing preparePing(String global_peer_ID, String instance) { 
		if (ClientSync.DEBUG) err.println("Client2: preparePing");
		ASNUDPPing aup = new ASNUDPPing();
		aup.senderIsPeer = false;
		aup.senderIsInitiator = true;
		aup.initiator_domain = Identity.get_a_server_domain(); 
		aup.initiator_globalID = Identity.getMyPeerGID(); 
		aup.initiator_instance = Identity.getMyPeerInstance();
		aup.initiator_port = Application.getPeerUDPPort(); 
		aup.peer_globalID = global_peer_ID;
		aup.peer_instance = instance;
		if (ClientSync.DEBUG) err.println("Client2: preparePing: done");
		return aup;
	}
	static boolean warned_instance_from_dir = false;
	/**
	 * Try NAT piercing via STUN (NAT signaled by reported address of peer).
	 * 
	 * The tested directory may be a shared one, but try to ping now for the instance in parameter (i.e. using its reported address).
	 * If no reported address is found for this instance, then at least send a ping to the first encountered NAT instance from this directory.
	 * 
	 * @param conn_peer
	 * @param conn_peer_dir
	 * @param aup
	 * @param instance
	 */
	private void try_UDP_connection_directory(Connection_Peer conn_peer, Connections_Peer_Directory conn_peer_dir, ASNUDPPing aup, String instance) {
		boolean DEBUG = false || ClientSync.DEBUG || DD.DEBUG_COMMUNICATION_STUN;
		if (DEBUG) err.println("Client2: try_UDP_connection_directory: "+conn_peer_dir);
		if (conn_peer_dir.supernode_addr == null) { if (DEBUG) err.println("Client2:try_UDP_connection_directory:x1"); return;}
		if (conn_peer_dir.supernode_addr.isa_tcp == null) { if (DEBUG) err.println("Client2:try_UDP_connection_directory:x2"); return;}
		if (conn_peer_dir.supernode_addr.isa_tcp.isUnresolved()) { if (DEBUG) err.println("Client2:try_UDP_connection_directory:x3"); return;}
		Address_SocketResolved_TCP reported_peer_addr = conn_peer_dir.getReportedAddress(instance);
		if (reported_peer_addr == null) {
			if (_DEBUG && !warned_instance_from_dir) {
				err.println("Client2: try_UDP_connection_directory: RATHER THAN THIS, create instances from getDirAddress");
				warned_instance_from_dir = true;
			}
			reported_peer_addr = conn_peer_dir.getReportedAddressSome();
		}
		if (reported_peer_addr == null) { if (DEBUG) err.println("Client2:try_UDP_connection_directory:x4: inst="+instance+" ps="+conn_peer_dir+" pc="+conn_peer); return;}
		if (reported_peer_addr.isa_tcp == null) { if (DEBUG) err.println("Client2:try_UDP_connection_directory:x5"); return;}
		if (reported_peer_addr.isa_tcp.isUnresolved()) { if (DEBUG) err.println("Client2:try_UDP_connection_directory:x6"); return;}
		aup.peer_domain=reported_peer_addr.getAddress().domain;
		aup.peer_port=reported_peer_addr.getAddress().udp_port;
		byte [] msg = aup.encode();
		DatagramPacket dp = new DatagramPacket(msg, msg.length);
		if (DEBUG) err.println("Client2: try_UDP_connection_directory: ping supernode "+aup);
		if (! sendUDP(conn_peer_dir.supernode_addr.isa_tcp, dp, conn_peer.getName())) {
			if(_DEBUG) System.out.println("Client2:try_UDP_connection_directory: fail "+conn_peer_dir.supernode_addr);
			return;
		}
		if (DEBUG) err.println("Client2: try_UDP_connection_directory: ping peer");
		if (! sendUDP(reported_peer_addr.isa_tcp, dp, conn_peer.getName())) {
			if (DEBUG || DD.DEBUG_COMMUNICATION_STUN) System.out.println("Client2:try_UDP_connection_directory: fail "+reported_peer_addr+" from "+conn_peer_dir.supernode_addr);
			return;
		}
		if (DEBUG) err.println("Client2: try_UDP_connection_directory: register contact in logs");
		registerPeerContact(reported_peer_addr.isa_tcp, conn_peer.getGID(), conn_peer.getName(), conn_peer_dir.supernode_addr, "UDP", instance);
		if (DEBUG) err.println("Client2: try_UDP_connection_directory: done");
	}
	private void registerPeerContact(InetSocketAddress sock_addr, String global_peer_ID, String peer_name, Address_SocketResolved_TCP supernode_addr, String trans, String instance){
		if(ClientSync.DEBUG) System.out.println("Client2: registerPeerContact: "+peer_name);
		Address ad = null;
		if(supernode_addr!=null) ad = supernode_addr.getAddress();
		registerPeerContact(sock_addr, global_peer_ID, peer_name, ad, trans, instance);
		if(ClientSync.DEBUG) System.out.println("Client2: registerPeerContact: done");
	}
	private void registerPeerContact(InetSocketAddress sock_addr, String global_peer_ID, String peer_name, Address ad, String trans, String instance) {
		if(ClientSync.DEBUG) System.out.println("Client2: registerPeerContact2: ad");
		String old_address = null; String old_type = null;
		if (ad != null) {
			old_type = ad.pure_protocol;
			old_address = ad.domain;
			if ("UDP".equals(trans)) old_address = old_address+":"+ad.tcp_port;
			else old_address = old_address+":"+ad.udp_port;
		}
		String peer_key = peer_name;
		String now = Util.getGeneralizedTime();
		if(ClientSync.DEBUG) out.println("Client2: registerPeerContact2: now="+now);
		if(peer_key==null) peer_key=Util.trimmed(global_peer_ID);
		Hashtable<String, Hashtable<String, Hashtable<String,String>>> opc = D_Peer.peer_contacts.get(peer_key);
		if (opc == null) {
			opc = new Hashtable<String, Hashtable<String, Hashtable<String,String>>>();
			D_Peer.peer_contacts.put(peer_key, opc);
		}
		Hashtable<String, Hashtable<String,String>> pc = opc.get(Util.getStringNonNullUnique(instance));
		if (pc == null) {
			pc = new Hashtable<String, Hashtable<String,String>>();
			opc.put(Util.getStringNonNullUnique(instance), pc);
		}
		String key = old_type+":"+old_address;
		Hashtable<String,String> value = pc.get(key);
		if(value==null){
			value = new Hashtable<String,String>();
			pc.put(key, value);
		}
		value.put(sock_addr+":"+trans+"***", now);
		if(DEBUG) out.println("Client2: registerPeerContact2: set adr="+old_type+":"+old_address+" val="+sock_addr+":"+trans+" "+now);
	}
	private boolean sendUDP(InetSocketAddress sock_addr, DatagramPacket dp, String peer_name) {
		if (ClientSync.DEBUG) System.out.println("Client2: sendUDP");
		try{
			dp.setSocketAddress(sock_addr);
		}catch(Exception e){
			System.err.println("Client2: sendUDP: is Skipping address: "+sock_addr+ " due to: "+e);
			return false;
		}
		try {
			if(DEBUG)System.out.print("Client2:sendUDP:#_"+dp.getSocketAddress());
			if (Application.getG_UDPServer() != null) Application.getG_UDPServer().send(dp);
			else {
				if(ClientSync._DEBUG)System.out.println("Client2: sendUDP: fail due to absent UDP Server");
				Util.printCallPath("no server?");
				return false;
			}
		} catch (IOException e) {
			if(ClientSync.DEBUG)System.out.println("Client2: sendUDP: Fail to send ping to peer \""+peer_name+"\" at "+sock_addr);
			return false;
		}
		if(ClientSync.DEBUG) System.out.println("Client2: sendUDP done: "+sock_addr);
		return true;
	}
	private boolean try_UDP_connection_socket(Connection_Peer pc, Connections_Peer_Socket ps, byte[] msg) {
		boolean DEBUG = Client2.DEBUG  || ClientSync.DEBUG || DD.DEBUG_COMMUNICATION_STUN;
		if (DEBUG) System.out.println("Client2: try_UDP_connection_socket");
		if ((ps == null) || (ps.addr == null) || (ps.addr.isa_udp == null)) {
			if(DEBUG) System.out.println("Client2: try_UDP_connection_socket: done empty: pc="+pc+"\nps="+ps);
			return false;
		}
		if (ps.addr.isa_udp.isUnresolved()) {
			if (DEBUG) out.println("Client2: try_UDP_connection_socket: UPeer "+pc.getName()+" is unresolved! "+ps.addr.isa_tcp);
			return false;
		}
		DatagramPacket dp = new DatagramPacket(msg, msg.length);
		ps.setPingSentDateUDP();
		sendUDP(ps.addr.isa_udp, dp, pc.getName());
		if (DEBUG) System.out.println("Client2: try_UDP_connection_socket: done");
		return true;
	}
	/**
	 * Returns the open socket, and it will be closed after transfer
	 * @param pc
	 * @param ps
	 * @param instance
	 * @return
	 */
	private Socket try_TCP_connection(Connection_Peer pc, Connections_Peer_Socket ps, String instance) {
		if (ClientSync.DEBUG) System.out.println("Client2: try_TCP_connection: "+ps);
		if ((ps == null) || (ps.addr == null) || (ps.addr.isa_tcp == null)) {
			if (ClientSync.DEBUG) System.out.println("Client2: try_TCP_connection: done empty");
			return null;
		}
		if (ps.addr.isa_tcp.isUnresolved()) {
			if (ClientSync._DEBUG) out.println("Client: try_connect: UPeer "+pc.getName()+" is unresolved! "+ps.addr.isa_tcp);
			return null;
		}
		InetSocketAddress sock_addr = ps.addr.isa_tcp;
		try {
			if (ClientSync.DEBUG) out.println("Client: Try TCP connection!");
			Socket client_socket = new Socket();
			client_socket.connect(sock_addr, Server.TIMEOUT_Client_wait_Server);
			if (ClientSync.DEBUG) out.println("Client: Success connecting Server: "+sock_addr);
			DD.ed.fireClientUpdate(new CommEvent(this, pc.getName(), sock_addr,"SERVER", "Connected: "+pc.getGID()));
			this.registerPeerContact(sock_addr, pc.getGID(), pc.getName(), ps.addr.addr, "TCP", instance);
			if (ClientSync.DEBUG) System.out.println("Client2: try_TCP_connection: done "+client_socket);
			return client_socket;
		}
		catch (SocketTimeoutException e) {
			if (ClientSync.DEBUG) out.println("Client: TIMEOUT connecting: "+sock_addr);
			DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Connection TIMEOUT"));
			return null;
		}
		catch (ConnectException e) {
			if (ClientSync.DEBUG) out.println("Client: Connection Exception: "+e);
			DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Connection e="+e));
			return null;
		}
		catch (Exception e) {
			if (ClientSync.DEBUG) out.println("Client: General exception try-ing: "+sock_addr+" is: "+e);
			DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Exception="+e));
			return null;
		}
	}
	/**
	 * Detect other instances of myself (??) and synchronize with them
	 * Still open how to define other instances of myself. 
	 * Probably each peer should be associated with a main GID and a signature thereof
	 * Searching a supernode for the main GID should also return attached ones.
	 * 	Peer table should be a tree????, grouped by main GID
	 */
	private void synchronize_Myself() {
		if(ClientSync.DEBUG) System.out.println("Client2: synchronize_Myself: TODO");
	}
	public Object get_wait_lock() {
		return wait_lock;
	}
	@Override
	public void wakeUp() {
		synchronized(get_wait_lock()) {
			if (Client2.DEBUG || ClientSync.DEBUG) System.out.println("Client2: wakeUp: touchClient really");
			Client2.recentlyTouched = true;
			get_wait_lock().notify();
		}
	}
	/**
	 * TODO: Decide instance
	 */
	@Override
	public boolean try_connect(
			ArrayList<Address_SocketResolved_TCP> tcp_sock_addresses,
			ArrayList<Address_SocketResolved_TCP> udp_sock_addresses,
			String old_address, String s_address, String type,
			String global_peer_ID, String peer_name,
			ArrayList<InetSocketAddress> peer_directories_udp_sockets,
			ArrayList<String> peer_directories) {
		String instance = null; 
		boolean DEBUG = ClientSync.DEBUG || DD.DEBUG_PLUGIN;
		if (DEBUG) out.println("\n\nClient2:try_connect:2 start "+s_address+" old_address="+old_address);
		InetSocketAddress sock_addr;
		String peer_key = peer_name;
		String now = Util.getGeneralizedTime();
		if (DEBUG) out.println("Client: now="+now);
		if (peer_key == null) peer_key=Util.trimmed(global_peer_ID);
		Hashtable<String, Hashtable<String, Hashtable<String,String>>> opc = D_Peer.peer_contacts.get(peer_key);
		if (opc == null) {
			opc = new Hashtable<String, Hashtable<String, Hashtable<String,String>>>();
			D_Peer.peer_contacts.put(peer_key, opc);
		}
		Hashtable<String, Hashtable<String,String>> pc = opc.get(Util.getStringNonNullUnique(instance));
		if (pc == null) {
			pc = new Hashtable<String, Hashtable<String,String>>();
			opc.put(Util.getStringNonNullUnique(instance), pc);
		}
		if (DEBUG) out.println("Client:try_connect: Client received addresses:"+s_address+ " #tcp:"+tcp_sock_addresses.size());
		if (DD.ClientTCP) {			
			for (int k=0; k<tcp_sock_addresses.size(); k++) {
				if(DEBUG) out.println("Client try address["+k+"]"+tcp_sock_addresses.get(k));
				Address_SocketResolved_TCP sad = tcp_sock_addresses.get(k);
				sock_addr=sad.isa_tcp;
				DD.ed.fireClientUpdate(new CommEvent(this, null, null,"TRY ADDRESS", sock_addr+""));
				if(sock_addr.isUnresolved()) {
					if(DEBUG) out.println("Client: Peer is unresolved!");
					continue;
				}
				if(Server.isMyself(sock_addr)){
					if(DEBUG) out.println("Client: Peer is Myself!");
					DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Peer is Myself"));
					continue; 
				}
				if(ClientSync.isMyself(Application.getPeerTCPPort(), sock_addr,sad)){
					if(DEBUG) out.println("Client: Peer is myself!");
					continue;
				}
				try{
					if(DEBUG) out.println("Client: Try TCP connection!");
					Socket client_socket = new Socket();
					client_socket.connect(sock_addr, Server.TIMEOUT_Client_wait_Server);
					if(DEBUG) out.println("Client: Success connecting Server: "+sock_addr);
					DD.ed.fireClientUpdate(new CommEvent(this, peer_name, sock_addr,"SERVER", "Connected: "+global_peer_ID));
					if(_DEBUG) out.println("Client2: Try_connect: pc="+type+":"+old_address+" => "+sock_addr);
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
					continue; 
				}
				catch(ConnectException e){
					if(DEBUG) out.println("Client: Connection Exception: "+e);
					DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Connection e="+e));
					continue; 
				}
				catch(Exception e){
					if(DEBUG) out.println("Client: General exception try-ing: "+sock_addr+" is: "+e);
					DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Exception="+e));
					continue; 
				}
			}
		}
		if (DD.ClientUDP) {
			if(Application.getG_UDPServer() == null){
				DD.ed.fireClientUpdate(new CommEvent(this, s_address,null,"FAIL: UDP Server not running", peer_name+" ("+global_peer_ID+")"));
				if(_DEBUG) err.println("UClient socket not yet open, no UDP server");
				return false;
			}
			if(DEBUG) out.println("UClient received addresses #:"+udp_sock_addresses.size());
			for(int k=0; k<udp_sock_addresses.size(); k++) {
				if(DEBUG) out.println("UClient try address["+k+"]"+udp_sock_addresses.get(k));
				if (DD.AVOID_REPEATING_AT_PING && (Application.getG_UDPServer()!=null) && (!Application.getG_UDPServer().hasSyncRequests(global_peer_ID, instance))) {
					DD.ed.fireClientUpdate(new CommEvent(this, peer_name, null, "LOCAL", "Stop sending: Received ping confirmation already handled from peer"));
					if (DEBUG) System.out.println("UDPServer Ping already handled for: "+Util.trimmed(global_peer_ID));
					{
						String key = type+":"+old_address;
						Hashtable<String,String> value = pc.get(key);
						if(value==null){
							value = new Hashtable<String,String>();
							pc.put(key, value);
						}
						sock_addr=udp_sock_addresses.get(k).isa_tcp;
						value.put(sock_addr+" UDP-"+DD.ALREADY_CONTACTED, now);
					}
					return false;					
				}
				Address_SocketResolved_TCP sad = udp_sock_addresses.get(k);
				sock_addr=sad.isa_tcp;
				if(DEBUG) out.println("UClient:try_connect: checkMyself");
				if(ClientSync.isMyself(Application.getPeerUDPPort(), sock_addr, sad)){
					if(DEBUG) out.println("Client:try_connect: UPeer "+peer_name+" is myself!"+sock_addr);
					continue;
				}
				if(DEBUG) out.println("UClient:try_connect: check unresolved");
				if(sock_addr.isUnresolved()) {
					if(ClientSync._DEBUG) out.println("Client: try_connect: UPeer "+peer_name+" is unresolved! "+sock_addr);
					continue;
				}
				if(DEBUG) out.println("UClient:try_connect: sending ping");
				if(DEBUG)System.out.println("Client Sending Ping to: "+sock_addr+" for \""+peer_name+"\"");
				ASNUDPPing aup = new ASNUDPPing();
				aup.senderIsPeer=false;
				aup.senderIsInitiator=true;
				aup.initiator_domain = Identity.get_a_server_domain();
				aup.initiator_globalID=Application.getCurrent_Peer_ID().getPeerGID(); 
				aup.initiator_port = Application.getPeerUDPPort();
				aup.peer_globalID=global_peer_ID;
				aup.peer_domain=sad.getAddress().domain;
				if(DEBUG) System.out.println("Client:try_connect: domain ping = \""+aup.peer_domain+"\" vs \""+Util.getNonBlockingHostName(sock_addr)+"\"");
				aup.peer_port=sock_addr.getPort();
				byte[] msg = aup.encode();
				DatagramPacket dp = new DatagramPacket(msg, msg.length);
				try {
					dp.setSocketAddress(sock_addr);
					String key = type+":"+old_address;
					Hashtable<String,String> value = pc.get(key);
					if (value == null) {
						value = new Hashtable<String,String>();
						pc.put(key, value);
					}
					value.put(sock_addr+":UDP****", now);
					if(DEBUG) out.println("\n\nClient2:try_connect: set adr="+type+":"+old_address+" val="+sock_addr+":UDP "+now);
				}catch(Exception e){
					System.err.println("Client is Skipping address: "+sock_addr+ " due to: "+e);
					continue;
				}
				try {
					Application.getG_UDPServer().send(dp);
				} catch (IOException e) {
					if(DEBUG)System.out.println("Fail to send ping to peer \""+peer_name+"\" at "+sock_addr);
					continue;
				}
				ArrayList<InetSocketAddress> directories = peer_directories_udp_sockets; 
				if((peer_directories.size()!=directories.size()) && (directories.size()==0) )
					directories = ClientSync.getUDPDirectoriesSockets(peer_directories, directories);
				if(DEBUG)System.out.println("I have sent to peer the UDP packet: "+aup);
				if(DEBUG)System.out.println("I have sent the UDP ping packet to: "+directories.size()+" directories");
				for (int d=0; d<directories.size(); d++) {
					try {
						InetSocketAddress dir_adr = (InetSocketAddress)directories.get(d);
						if(dir_adr.isUnresolved()) continue;
						dp.setSocketAddress(dir_adr);
						Application.getG_UDPServer().send(dp);
						if(DEBUG)System.out.println("I requested ping via: "+dp.getSocketAddress()+" ping="+aup);
					} catch (IOException e) {
						if(ClientSync._DEBUG)System.out.println("Client: try_connect: EEEEERRRRRRRROOOOOOORRRRR "+e.getMessage());
					}
				}
				if(DEBUG)System.out.println("I have sent to peer the UDP packet");
			}
		}
		DD.ed.fireClientUpdate(new CommEvent(this, peer_name, null, "SERVER", "Fail for: "+s_address+"("+old_address+")"));
		if(DEBUG) out.println("Client:try_connect:2 done");
		return false;
	}
}
