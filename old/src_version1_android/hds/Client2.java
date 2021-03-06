package hds;

import static java.lang.System.err;
import static java.lang.System.out;
import static util.Util._;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import streaming.RequestData;
import streaming.UpdateMessages;
import table.peer;
import util.CommEvent;
import util.P2PDDSQLException;
import util.Util;
import config.Application;
import config.Application_GUI;
import config.DD;
import config.Identity;
import data.D_Peer;

public class Client2 extends util.DDP2P_ServiceThread  implements IClient{
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public static final Object conn_monitor = new Object();
	private static boolean recentlyTouched;
	static int peersToGo = -1;
	boolean turnOff = false;
	public Object wait_lock = new Object();
	public static Connections conn = null;
	public void turnOff() {
		if(ClientSync.DEBUG) System.out.println("Client2: turnOff");
		turnOff = true;
		this.interrupt();
	}
	static public void touchClient() throws NumberFormatException, P2PDDSQLException {
		if(ClientSync.DEBUG) System.out.println("Client2: touchClient");
		Client2 ac = (Client2)Application.ac;
		if(ac==null) {
			if(ClientSync.DEBUG) System.out.println("Client2: touchClient: start");
			startClient(true);
			ac = (Client2)Application.ac;
		}
		synchronized(ac.wait_lock) {
			if(ClientSync.DEBUG) System.out.println("Client2: touchClient really");
			Client2.recentlyTouched = true;
			//Client.peersToGo = Client.peersAvailable;
			ac.wait_lock.notify();
		}
	}
	static public boolean startClient(boolean on) throws NumberFormatException, P2PDDSQLException {
		if(ClientSync.DEBUG) System.out.println("Client2: startClient: "+on);
		Client2 ac = (Client2)Application.ac;
		
		if((on == false)&&(ac!=null)) {ac.turnOff(); Application.ac=null;}
		if(ac != null) return false;
		try {
			Application.ac = new Client2();
			Application.ac.start();
		} catch (Exception e) {
			return false;
		}
		if(ClientSync.DEBUG) System.out.println("Client2: startClient: done");
		return true;
	}
	public Client2() {
		super ("Client 2", false);
		boolean DEBUG = ClientSync.DEBUG;// || true;
		if (DEBUG) System.out.println("Client2: <init>");
		//Connections c = 
		try {
			ClientSync.buildStaticPayload();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void _run() {
		//if (_DEBUG) System.out.println("Client2: run");
		//this.setName("Client 2");
		//ThreadsAccounting.registerThread();
		synchronized(wait_lock) {
			try {
				wait_lock.wait(DD.PAUSE_BEFORE_CONNECTIONS_START);
				if (ClientSync.DEBUG || DD.DEBUG_LIVE_THREADS) System.out.println("Client2: run: connections go");
				synchronized(Client2.conn_monitor) {
					if (conn == null)
						conn  = new Connections(Application.db);
				}
				wait_lock.wait(DD.PAUSE_BEFORE_CLIENT_START);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
		if (ClientSync.DEBUG || DD.DEBUG_LIVE_THREADS) System.out.println("Client2: run: go");
		if (ClientSync.DEBUG) System.out.println("Client2: run: start");
		try {__run();} catch(Exception e) {e.printStackTrace();}
		if (ClientSync.DEBUG) System.out.println("Client2: run: done");
		//ThreadsAccounting.unregisterThread();
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
			if (try_wait(peersToGo)) continue; // too many busy threads
			if (turnOff) {
				if (ClientSync.DEBUG) System.out.println("Client2: _run: turnOff 2");
				break;
			}

			if (peersToGo < 0) {
				cnt++;
				if (ClientSync.DEBUG) System.out.println("Client2: _run: sync myself: round: "+cnt);
				if (cnt%ClientSync.CYCLES_PAYLOAD == 0){
					ClientSync.initPayload();
				}
				synchronize_Myself(); // ??
				
				peersToGo = 0;
				continue;
			}
			Connection_Peer pc = Connections.getConnectionAtIdx(peersToGo);
			if (pc != null) {
				if (ClientSync.DEBUG) System.out.println("Client2: _run: handle "+peersToGo);
				if (ClientSync.DEBUG && !Connections.DEBUG) System.out.println("Client2: _run: handle "+pc);
				handlePeer(pc);
			}else{
				if(ClientSync.DEBUG) System.out.println("Client2: _run: handle null");
			}
			peersToGo++;
			if(peersToGo > Connections.peersAvailable) peersToGo = -1;
		}
	}
	/**
	 * Return true on wait due to busy threads (to know to retry wait again)
	 * @param crt used to tell when a cycle is done, to sleep some longer
	 * @return
	 */
	private boolean try_wait(int crt) {
		if(ClientSync.DEBUG) System.out.println("Client2: try_wait: "+crt+"/"+Connections.peersAvailable);
		// Avoid asking in parallel too many synchronizations (wait for answers)
		if((Application.aus!=null)&&(Application.aus.getThreads() > UDPServer.MAX_THREADS/2)){
			try {
				if(ClientSync._DEBUG) System.out.println("Client2: try_wait: overloaded threads = "+Application.aus.getThreads());
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Will Sleep: "+ClientSync.PAUSE));
				synchronized(wait_lock ){
					wait_lock.wait(ClientSync.PAUSE);
				}
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Wakes Up"));
				return true;
			} catch (InterruptedException e2) {
				//e2.printStackTrace();
				return true;
			}
		}
		try {
			if((!Client2.recentlyTouched) && (crt >= Connections.peersAvailable)) {
				if(ClientSync.DEBUG || DD.DEBUG_LIVE_THREADS) out.println("Client2: try_wait: Will wait ms: "+ClientSync.PAUSE+" p="+Connections.peersAvailable);
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Will Sleep: "+ClientSync.PAUSE));
				synchronized(wait_lock ){
					wait_lock.wait(ClientSync.PAUSE);
				}
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Wakes Up"));
			}
			//Client.peersToGo--;
			Client2.recentlyTouched = false;
			//peers = Application.db.select(peers_scan_sql, new String[]{});
			
		} catch (InterruptedException e) {
			if(ClientSync.DEBUG) e.printStackTrace();
		}
		/*
		catch (P2PDDSQLException e1) {Application.warning(_("Database: ")+e1, _("Database"));}
		*/
		if(ClientSync.DEBUG) System.out.println("Client2: try_wait: done");
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
		if(ClientSync.DEBUG) System.out.println("Client2: handlePeer");
		if(!pc.contacted_since_start || !pc.last_contact_successful) {
			result = handlePeerNotRecentlyContacted(pc);
		}else{
			pc.last_contact_successful = false;
			boolean success = handlePeerRecent(pc);
			if(success){
				pc.last_contact_successful = true;
				pc.contacted_since_start = true;
			}
			result = success;
		}
		if(ClientSync.DEBUG) System.out.println("Client2: handlePeer: done");
		return result;
	}
	public boolean trySocketsListTCP(ArrayList<Connections_Peer_Socket> peer_sockets, Connection_Peer pc, boolean retry, String instance) {
		boolean success = false;
		for (int k = 0; k < peer_sockets.size(); k ++) {
			Connections_Peer_Socket ps = peer_sockets.get(k);
			if (retry || (ps.contacted_since_start_TCP && ps.last_contact_successful_TCP)) {
				ps.last_contact_successful_TCP = false;
				Socket sock = try_TCP_connection(pc, ps, instance); // Do this in a thread?
				if (sock != null) { 
					success = Client2.transfer_TCP(pc, ps, sock);
					if (success) ps.last_contact_successful_TCP = true;
				}
				if (success) {
					if (ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: done TCP "+success);
					return success;
				}
			}
		}
		return false;
	}
	public boolean trySocketsListUDP(ArrayList<Connections_Peer_Socket> peer_sockets, Connection_Peer pc, ASNUDPPing aup, boolean retry) {
		if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: trySocketsListUDP: start #"+peer_sockets.size());
		for (int k = 0; k < peer_sockets.size(); k ++) {
			Connections_Peer_Socket ps = peer_sockets.get(k);
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
	 * For not yet contacted peers
	 * @param pc
	 * @return
	 */
	private boolean handlePeerNotRecentlyContacted(Connection_Peer pc) {
		//boolean DEBUG = Client2.DEBUG;
		if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld");
		boolean retry = true;
		boolean success = false;
		if (retry) Connections.update_supernode_address_request(pc); // requests queries for supernodes
		pc.last_contact_successful = false;
		//pc.contacted_since_start = true;

		if (DD.ClientTCP) {
			if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: try TCP");
			
			// does not really make sense to have sockets not linked to an instance
			success |= trySocketsListTCP(pc.shared_peer_sockets, pc, retry, null);
			if (success) return success;
			
			pc.sortInstances();
			
			for (Connection_Instance ci : pc.instances_AL) {
				success |= trySocketsListTCP(ci.peer_sockets, pc, retry, ci.dpi.peer_instance);
				if (success) return success;
			}
		}
		if (DD.ClientUDP) {
			if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: try UDP");
			// TRY first direct
			ASNUDPPing aup = preparePing(pc.getGID(), null);
			if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: ping = "+aup);
			/*
			trySocketsListUDP(pc.shared_peer_sockets, pc, aup, retry); // not expected to have any address
			// TRY STUN for null
			if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: directories: #"+pc.shared_peer_directories.size());
			for (int k = 0; k < pc.shared_peer_directories.size(); k ++) {
				Connections_Peer_Directory ps = pc.shared_peer_directories.get(k);
				if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: NAT sock ping to "+k+":"+ps);
				if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: NAT sock skip "+
						"retry="+retry+" contacted="+ps.contacted_since_start_UDP+" succes="+ps.last_contact_successful_UDP);
				if (retry || (ps.contacted_since_start_UDP && ps.last_contact_successful_UDP)) {
					ps.last_contact_successful_UDP = false;
					if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: NAT sock retry:"+k);
					try_UDP_connection_directory(pc, ps, aup);
					if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: NAT sock retried:"+k);
				} else {
					if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: NAT sock skip:"+k);
				}
			}
			*/

			for (Connection_Instance ci : pc.instances_AL) {
				if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: try instance: "+ci);
				aup.peer_instance = ci.dpi.peer_instance;
				trySocketsListUDP(ci.peer_sockets, pc, aup, retry);
				
				// STUN
				for (int k = 0; k < pc.shared_peer_directories.size(); k ++) {
					Connections_Peer_Directory ps = pc.shared_peer_directories.get(k);
					if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: try shared directory: "+ps);
					if (retry || (ps.contacted_since_start_UDP && ps.last_contact_successful_UDP)) {
						ps.last_contact_successful_UDP = false;
						try_UDP_connection_directory(pc, ps, aup, ci.dpi.peer_instance);
					}
				}
				for (int k = 0; k < ci.peer_directories.size(); k ++) {
					Connections_Peer_Directory ps = pc.shared_peer_directories.get(k);
					if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: try instance directory: "+ps);
					if (retry || (ps.contacted_since_start_UDP && ps.last_contact_successful_UDP)) {
						ps.last_contact_successful_UDP = false;
						try_UDP_connection_directory(pc, ps, aup, ci.dpi.peer_instance);
					}
				}
				/*
				for (Connections_Peer_Directory dir : ci.peer_directories) {
					if (dir.reportedAddressesUDP != null) tryAddressListUDP(dir.reportedAddressesUDP, pc, aup, retry);
					if (dir.reportedAddressesTCP != null) tryAddressListUDP(dir.reportedAddressesTCP, pc, aup, retry);
				}
				*/
			}
			
			if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: try UDP done");
		}
		if (DEBUG || ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: done");
		return false;
	}
	private boolean handlePeerRecent(Connection_Peer pc) {
		if (ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: "+pc);
		boolean success = false;
		boolean retry = false;
		pc.last_contact_successful = false;
		if (DD.ClientTCP) {
			if(ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: try TCP");
			success |= trySocketsListTCP(pc.shared_peer_sockets, pc, retry, null);
			if (success) return success;
			
			pc.sortInstances();
			
			for (Connection_Instance ci : pc.instances_AL) {
				success |= trySocketsListTCP(ci.peer_sockets, pc, retry, ci.dpi.peer_instance);
				if (success) return success;
			}
		}
		
		if (DD.ClientUDP) {
			if (ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: try UDP");
			ASNUDPPing aup = preparePing(pc.getGID(), null);

			/*
			trySocketsListUDP(pc.shared_peer_sockets, pc, aup, retry); // not expected to have any address
			// TRY STUN
			if (ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: directories");
			for (int k = 0; k < pc.shared_peer_directories.size(); k ++) {
				Connections_Peer_Directory ps = pc.shared_peer_directories.get(k);
				if (ps.contacted_since_start_UDP && ps.last_contact_successful_UDP) {
					ps.last_contact_successful_UDP = false;
					try_UDP_connection_directory(pc, ps, aup);
				}
			}
			*/
			
			for (Connection_Instance ci : pc.instances_AL) {
				aup.peer_instance = ci.dpi.peer_instance;
				trySocketsListUDP(ci.peer_sockets, pc, aup, retry);
			
				// STUN
				for (int k = 0; k < pc.shared_peer_directories.size(); k ++) {
					Connections_Peer_Directory ps = pc.shared_peer_directories.get(k);
					if (retry || (ps.contacted_since_start_UDP && ps.last_contact_successful_UDP)) {
						ps.last_contact_successful_UDP = false;
						try_UDP_connection_directory(pc, ps, aup, null);
					}
				}
				for (int k = 0; k < ci.peer_directories.size(); k ++) {
					Connections_Peer_Directory ps = pc.shared_peer_directories.get(k);
					if (retry || (ps.contacted_since_start_UDP && ps.last_contact_successful_UDP)) {
						ps.last_contact_successful_UDP = false;
						try_UDP_connection_directory(pc, ps, aup, ci.dpi.peer_instance);
					}
				}
			}
			
		}
		if (ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: done "+success);
		return success;
	}
	private static boolean transfer_TCP(Connection_Peer pc, Connections_Peer_Socket ps, Socket client_socket) {
		if (ClientSync.DEBUG) System.out.println("Client2: transfer_TCP: "+ps);
		String peer_ID = pc.peer.peer_ID;
		String peer_name = pc.getName();
		String global_peer_ID = pc.getGID();
		boolean filtered = Util.stringInt2bool(peer.filtered, false);
		String _lastSnapshotString = peer.last_sync_date;
		Calendar _lastSnapshot = Util.getCalendar(_lastSnapshotString);
		Application_GUI.peer_contacts_update();
		if (Application.peers != null) Application.peers.setConnectionState(peer_ID, DD.PEERS_STATE_CONNECTION_TCP);
		if (ClientSync.DEBUG) out.println("Client2: transfer_TCP: Connected!");
		DD.ed.fireClientUpdate(new CommEvent(Application.ac, peer_name, client_socket.getRemoteSocketAddress(), "Server", "Connected"));
				
		ASNSyncRequest sr = ClientSync.buildRequest(_lastSnapshotString, _lastSnapshot, peer_ID);
		if (filtered) sr.orgFilter=UpdateMessages.getOrgFilter(peer_ID);
		sr.sign();
		try {
			//out.println("Request sent last sync date: "+Encoder.getGeneralizedTime(sr.lastSnapshot)+" ie "+sr.lastSnapshot);
			//out.println("Request sent last sync date: "+sr);

			byte[] msg = sr.encode();
			if(ClientSync.DEBUG) out.println("Client2: transfer_TCP: Sync Request sent: "+Util.byteToHexDump(msg, " ")+"::"+sr);
			client_socket.getOutputStream().write(msg);
			DD.ed.fireClientUpdate(new CommEvent(Application.ac, peer_name, client_socket.getRemoteSocketAddress(), "Server", "Request Sent"));
			byte update[] = new byte[Client1.MAX_BUFFER];
			if(ClientSync.DEBUG) out.println("Waiting data from socket: "+client_socket+" on len: "+update.length+" timeout ms:"+Server.TIMEOUT_Client_Data);
			client_socket.setSoTimeout(Server.TIMEOUT_Client_Data);
			InputStream is=client_socket.getInputStream();
			int len = is.read(update);
			if(len>0){
				if(ClientSync.DEBUG) err.println("Client2: transfer_TCP: answer received length: "+len);
				DD.ed.fireClientUpdate(new CommEvent(Application.ac, peer_name, client_socket.getRemoteSocketAddress(), "Server", "Answered Received"));
				Decoder dec = new Decoder(update,0,len);
				System.out.println("Got first msg size: "+len);//+"  bytes: "+Util.byteToHex(update, 0, len, " "));
				if(!dec.fetchAll(is)){
					System.err.println("Buffer too small for receiving update answer!");
					return false;
				}
				len = dec.getMSGLength();
				//System.out.println("Got msg size: "+len);//+"  bytes: "+Util.byteToHex(update, 0, len, " "));
				RequestData rq = new RequestData();
				integrateUpdate(update,len, (InetSocketAddress)client_socket.getRemoteSocketAddress(), Application.ac, global_peer_ID, peer.peer_ID, Util.getStringID(ps.address_ID), rq, pc.peer);
				if(ClientSync.DEBUG) err.println("Client2: transfer_TCP: answer received rq: "+rq);
			}else{
				if(ClientSync.DEBUG) out.println("Client2: transfer_TCP: No answered received!");
				DD.ed.fireClientUpdate(new CommEvent(Application.ac, peer_name, client_socket.getRemoteSocketAddress(), "Server", "TIMEOUT_Client_Data may be too short: No Answered Received"));
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
		try {
			if(ClientSync.DEBUG) out.println("Client2: transfer_TCP: will close socket");
			client_socket.close();
		} catch (IOException e) {
			e.printStackTrace();
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
		UpdateMessages.integrateUpdate(asa, s_address, src, global_peer_ID, asa.peer_instance, peer_ID, address_ID, rq, peer);
		if(ClientSync.DEBUG) err.println("Client2: integrateUpdate: done");
	}
	private ASNUDPPing preparePing(String global_peer_ID, String instance) { //, String domain, int peer_port) {
		if (ClientSync.DEBUG) err.println("Client2: preparePing");
		ASNUDPPing aup = new ASNUDPPing();
		aup.senderIsPeer = false;
		aup.senderIsInitiator = true;
		aup.initiator_domain = Identity.get_a_server_domain(); //client.getInetAddress().getHostAddress();
		aup.initiator_globalID = Identity.getMyPeerGID(); //dr.initiator_globalID;
		aup.initiator_instance = Identity.getMyPeerInstance();
		aup.initiator_port = Identity.udp_server_port; //dr.UDP_port;
		aup.peer_globalID = global_peer_ID;
		aup.peer_instance = instance;
		// aup.peer_domain=domain;//;
		// if(ClientSync.DEBUG) System.out.println("Client:try_connect: domain ping = \""+aup.peer_domain+"\" vs \""+/*Util.getNonBlockingHostName(domain)+*/"\"");
		// aup.peer_port=peer_port;
		if (ClientSync.DEBUG) err.println("Client2: preparePing: done");
		return aup;
	}
	/**
	 * Try NAT piercing via STUN (NAT signaled by reported address of peer) 
	 * @param pc
	 * @param ps
	 * @param aup
	 */
	private void try_UDP_connection_directory(Connection_Peer pc, Connections_Peer_Directory ps, ASNUDPPing aup, String instance) {
		boolean DEBUG = false || ClientSync.DEBUG;
		if (DEBUG) err.println("Client2: try_UDP_connection_directory: "+ps);
		if (ps.supernode_addr == null) { if (DEBUG) err.println("Client2:try_UDP_connection_directory:x1"); return;}
		if (ps.supernode_addr.isa == null) { if (DEBUG) err.println("Client2:try_UDP_connection_directory:x2"); return;}
		if (ps.supernode_addr.isa.isUnresolved()) { if (DEBUG) err.println("Client2:try_UDP_connection_directory:x3"); return;}
		Address_SocketResolved_TCP reported_peer_addr = ps.getReportedAddress(instance);
		if (reported_peer_addr == null) { if (DEBUG) err.println("Client2:try_UDP_connection_directory:x4"); return;}
		if (reported_peer_addr.isa == null) { if (DEBUG) err.println("Client2:try_UDP_connection_directory:x5"); return;}
		if (reported_peer_addr.isa.isUnresolved()) { if (DEBUG) err.println("Client2:try_UDP_connection_directory:x6"); return;}
		aup.peer_domain=reported_peer_addr.ad.domain;
		aup.peer_port=reported_peer_addr.ad.udp_port;
		byte[]msg = aup.encode();
		DatagramPacket dp = new DatagramPacket(msg, msg.length);
		if (DEBUG) err.println("Client2: try_UDP_connection_directory: ping supernode "+aup);
		if (!sendUDP(ps.supernode_addr.isa, dp, pc.getName())) {
			if(DEBUG) System.out.println("Client2:try_UDP_connection_directory: fail "+ps.supernode_addr);
			return;
		}
		if (DEBUG) err.println("Client2: try_UDP_connection_directory: ping peer");
		if (!sendUDP(reported_peer_addr.isa, dp, pc.getName())) {
			if (DEBUG) System.out.println("Client2:try_UDP_connection_directory: fail "+reported_peer_addr+" from "+ps.supernode_addr);
			return;
		}
		if (DEBUG) err.println("Client2: try_UDP_connection_directory: register contact in logs");
		registerPeerContact(reported_peer_addr.isa, pc.getGID(), pc.getName(), ps.supernode_addr, "UDP", instance);
		if (DEBUG) err.println("Client2: try_UDP_connection_directory: done");
	}
	private void registerPeerContact(InetSocketAddress sock_addr, String global_peer_ID, String peer_name, Address_SocketResolved_TCP supernode_addr, String trans, String instance){
		if(ClientSync.DEBUG) System.out.println("Client2: registerPeerContact: "+peer_name);
		Address ad = null;
		if(supernode_addr!=null) ad = supernode_addr.ad;
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
		if(ClientSync.DEBUG) System.out.println("Client2: sendUDP");
		try{
			dp.setSocketAddress(sock_addr);
		}catch(Exception e){
			System.err.println("Client2: sendUDP: is Skipping address: "+sock_addr+ " due to: "+e);
			return false;
		}
		try {
			if(DEBUG)System.out.print("Client2:sendUDP:#_"+dp.getSocketAddress());
			if(Application.aus!=null) Application.aus.send(dp);
			else if(ClientSync._DEBUG)System.out.println("Client2: sendUDP: fail due to absent UDP Server");
		} catch (IOException e) {
			if(ClientSync.DEBUG)System.out.println("Client2: sendUDP: Fail to send ping to peer \""+peer_name+"\" at "+sock_addr);
			return false;
		}
		if(ClientSync.DEBUG) System.out.println("Client2: sendUDP done: "+sock_addr);
		return true;
	}
	private void try_UDP_connection_socket(Connection_Peer pc, Connections_Peer_Socket ps, byte[] msg) {
		if(ClientSync.DEBUG) System.out.println("Client2: try_UDP_connection_socket");
		if((ps==null)||(ps.addr==null)||(ps.addr.isa_udp==null)){
			if(ClientSync.DEBUG) System.out.println("Client2: try_UDP_connection_socket: done empty: pc="+pc+"\nps="+ps);
			return;
		}
		if(ps.addr.isa_udp.isUnresolved()) {
			if(ClientSync.DEBUG) out.println("Client2: try_UDP_connection_socket: UPeer "+pc.getName()+" is unresolved! "+ps.addr.isa_tcp);
			return;
		}
		DatagramPacket dp = new DatagramPacket(msg, msg.length);
		sendUDP(ps.addr.isa_udp, dp, pc.getName());
		if(ClientSync.DEBUG) System.out.println("Client2: try_UDP_connection_socket: done");
	}
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
			this.registerPeerContact(sock_addr, pc.getGID(), pc.getName(), ps.addr.ad, "TCP", instance);
			if (ClientSync.DEBUG) System.out.println("Client2: try_TCP_connection: done "+client_socket);
			return client_socket;
		}
		catch (SocketTimeoutException e) {
			if (ClientSync.DEBUG) out.println("Client: TIMEOUT connecting: "+sock_addr);
			DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Connection TIMEOUT"));
			//continue; //
			return null;
		}
		catch (ConnectException e) {
			if (ClientSync.DEBUG) out.println("Client: Connection Exception: "+e);
			DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Connection e="+e));
			//continue; //
			return null;
		}
		catch (Exception e) {
			if (ClientSync.DEBUG) out.println("Client: General exception try-ing: "+sock_addr+" is: "+e);
			//if(ClientSync.DEBUG) e.printStackTrace();
			DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Exception="+e));
			//continue; //
			return null;
		}
		//return true;
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
			Client2.recentlyTouched = true;
			//Client2.peersToGo = Connections.peersAvailable;
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
		String instance = null; //TODO decide instance to send to
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
			
				//address = addresses[k];
				Address_SocketResolved_TCP sad = tcp_sock_addresses.get(k);
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
					continue; //return false;
				}
				catch(ConnectException e){
					if(DEBUG) out.println("Client: Connection Exception: "+e);
					DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Connection e="+e));
					continue; //return false;
				}
				catch(Exception e){
					if(DEBUG) out.println("Client: General exception try-ing: "+sock_addr+" is: "+e);
					//if(ClientSync.DEBUG) e.printStackTrace();
					DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Exception="+e));
					continue; //return false;
				}
			}
		}
		//System.out.print("#0");
		if (DD.ClientUDP) {
			if(Application.aus == null){
				DD.ed.fireClientUpdate(new CommEvent(this, s_address,null,"FAIL: UDP Server not running", peer_name+" ("+global_peer_ID+")"));
				if(_DEBUG) err.println("UClient socket not yet open, no UDP server");
				//System.out.print("#1");
				return false;
			}
			if(DEBUG) out.println("UClient received addresses #:"+udp_sock_addresses.size());
			for(int k=0; k<udp_sock_addresses.size(); k++) {
				//boolean ClientSync.DEBUG = true;
				if(DEBUG) out.println("UClient try address["+k+"]"+udp_sock_addresses.get(k));

				if (DD.AVOID_REPEATING_AT_PING && (Application.aus!=null) && (!Application.aus.hasSyncRequests(global_peer_ID, instance))) {
					DD.ed.fireClientUpdate(new CommEvent(this, peer_name, null, "LOCAL", "Stop sending: Received ping confirmation already handled from peer"));
					if (DEBUG) System.out.println("UDPServer Ping already handled for: "+Util.trimmed(global_peer_ID));
					{
						String key = type+":"+old_address;
						Hashtable<String,String> value = pc.get(key);
						if(value==null){
							value = new Hashtable<String,String>();
							pc.put(key, value);
						}
						sock_addr=udp_sock_addresses.get(k).isa;//getSockAddress(address);
						value.put(sock_addr+" UDP-"+DD.ALREADY_CONTACTED, now);
					}
					//System.out.print("#2");
					return false;					
				}
				Address_SocketResolved_TCP sad = udp_sock_addresses.get(k);
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
	
}