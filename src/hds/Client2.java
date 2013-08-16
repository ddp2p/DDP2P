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
import util.DBInterface;
import util.P2PDDSQLException;
import util.Util;
import widgets.peers.PeerContacts;
import widgets.peers.Peers;
import config.Application;
import config.DD;
import config.Identity;
import data.D_PeerAddress;

public class Client2 extends Thread  implements IClient{
	static final long PAUSE = 10000;
	private static final boolean _DEBUG = true;
	private static boolean recentlyTouched;
	static int peersToGo = -1;
	boolean turnOff = false;
	public Object wait_lock = new Object();
	private static Connections conn = null;
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
	public Client2(){
		if(ClientSync.DEBUG) System.out.println("Client2: <init>");
		//Connections c = 
		if(conn == null)
			conn  = new Connections(Application.db);
	}
	public void run(){
		if(ClientSync.DEBUG) System.out.println("Client2: run: start");
		try{_run();}catch(Exception e){e.printStackTrace();}
		if(ClientSync.DEBUG) System.out.println("Client2: run: done");
	}
	public void _run(){
		for(;;){
			if(ClientSync.DEBUG) System.out.println("Client2: _run: next="+peersToGo);
			if(turnOff){
				if(ClientSync.DEBUG) System.out.println("Client2: _run: turnOff 1");
				break;
			}
			if(try_wait(peersToGo)) continue; // too many busy threads
			if(turnOff){
				if(ClientSync.DEBUG) System.out.println("Client2: _run: turnOff 2");
				break;
			}

			if(peersToGo < 0) {
				if(ClientSync.DEBUG) System.out.println("Client2: _run: sync myself");
				synchronize_Myself(); // ??
				peersToGo = 0;
				continue;
			}
			Peer_Connection pc = Connections.getConnectionAtIdx(peersToGo);
			if(pc != null){
				if(ClientSync.DEBUG) System.out.println("Client2: _run: handle "+peersToGo);
				if(ClientSync.DEBUG && !Connections.DEBUG) System.out.println("Client2: _run: handle "+pc);
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
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Will Sleep: "+Client2.PAUSE));
				synchronized(wait_lock ){
					this.wait(Client2.PAUSE);
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
				if(ClientSync.DEBUG) out.println("Client2: try_wait: Will wait ms: "+Client2.PAUSE);
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Will Sleep: "+Client2.PAUSE));
				synchronized(wait_lock ){
					wait_lock.wait(Client2.PAUSE);
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
	private boolean handlePeer(Peer_Connection pc) {
		boolean result;
		if(ClientSync.DEBUG) System.out.println("Client2: handlePeer");
		if(!pc.contacted_since_start || !pc.last_contact_successful) {
			result = handlePeerOld(pc);
		}else{
			pc.last_contact_successful = false;
			boolean success = handlePeerRecent(pc);
			if(success) pc.last_contact_successful = true;
			result = success;
		}
		if(ClientSync.DEBUG) System.out.println("Client2: handlePeer: done");
		return result;
	}
	private boolean handlePeerOld(Peer_Connection pc) {
		if(ClientSync.DEBUG) System.out.println("Client2: handlePeerOld");
		boolean retry = true;
		boolean success = false;
		if (retry) Connections.update_supernode_address_request(pc);
		pc.last_contact_successful = false;

		if(DD.ClientTCP){
			if(ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: try TCP");
			for(int k=0; k<pc.peer_sockets.size(); k++) {
				Peer_Socket ps = pc.peer_sockets.get(k);
				if(retry || (ps.contacted_since_start_TCP && ps.last_contact_successful_TCP)) {
					ps.last_contact_successful_TCP = false;
					Socket sock = try_TCP_connection(pc, ps);// Do this in a thread?
					if(sock!=null){ 
						success = Client2.transfer_TCP(pc, ps, sock);
						if(success) ps.last_contact_successful_TCP = true;
					}
					if(success) return success;
				}
			}
		}
		if(DD.ClientUDP){
			if(ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: try UDP");
			ASNUDPPing aup = preparePing(pc.GID);
			if(ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: ping = "+aup);
			for(int k=0; k<pc.peer_sockets.size(); k++) {
				Peer_Socket ps = pc.peer_sockets.get(k);
				if(retry || (ps.contacted_since_start_UDP && ps.last_contact_successful_UDP)) {
					ps.last_contact_successful_UDP = false;
					if((ps.addr == null)||(ps.addr.ad == null)) continue;
					aup.peer_domain = ps.addr.ad.domain;
					aup.peer_port = ps.addr.ad.udp_port;
					try_UDP_connection_socket(pc, ps, aup.encode());
				}
			}
			if(ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: directories");
			for(int k=0; k<pc.peer_directories.size(); k++) {
				Peer_Directory ps = pc.peer_directories.get(k);
				if(retry || (ps.contacted_since_start && ps.last_contact_successful)) {
					ps.last_contact_successful = false;
					try_UDP_connection_directory(pc, ps, aup);
				}
			}
			if(ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: try UDP done");
		}
		if(ClientSync.DEBUG) System.out.println("Client2: handlePeerOld: done");
		return false;
	}
	private boolean handlePeerRecent(Peer_Connection pc) {
		if(ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: "+pc);
		boolean success = false;
		pc.last_contact_successful = false;
		if(DD.ClientTCP){
			if(ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: try TCP");
			for(int k=0; k<pc.peer_sockets.size(); k++) {
				Peer_Socket ps = pc.peer_sockets.get(k);
				if(ps.contacted_since_start_TCP && ps.last_contact_successful_TCP) {
					ps.last_contact_successful_TCP = false;
					Socket sock = try_TCP_connection(pc, ps);// Do this in a thread?
					if(sock!=null){ 
						success = Client2.transfer_TCP(pc, ps, sock);
						if(success) ps.last_contact_successful_TCP = true;
					}
					if(success){
						if(ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: done TCP "+success);
						return success;
					}
				}
			}
		}
		if(DD.ClientUDP){
			if(ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: try UDP");
			ASNUDPPing aup = preparePing(pc.GID);
			for(int k=0; k<pc.peer_sockets.size(); k++) {
				Peer_Socket ps = pc.peer_sockets.get(k);
				if(ps.contacted_since_start_UDP && ps.last_contact_successful_UDP) {
					ps.last_contact_successful_UDP = false;
					if((ps.addr == null)||(ps.addr.ad == null)) continue;
					aup.peer_domain = ps.addr.ad.domain;
					aup.peer_port = ps.addr.ad.udp_port;
					try_UDP_connection_socket(pc, ps, aup.encode());
				}
			}
			if(ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: directories");
			for(int k=0; k<pc.peer_directories.size(); k++) {
				Peer_Directory ps = pc.peer_directories.get(k);
				if(ps.contacted_since_start && ps.last_contact_successful) {
					ps.last_contact_successful = false;
					try_UDP_connection_directory(pc, ps, aup);
				}
			}
		}
		if(ClientSync.DEBUG) System.out.println("Client2: handlePeerRecent: done "+success);
		return success;
	}
	private static boolean transfer_TCP(Peer_Connection pc, Peer_Socket ps, Socket client_socket) {
		if(ClientSync.DEBUG) System.out.println("Client2: transfer_TCP: "+ps);
		String peer_ID = pc.peer.peer_ID;
		String peer_name = pc.name;
		String global_peer_ID = pc.GID;
		boolean filtered = Util.stringInt2bool(peer.filtered, false);
		String _lastSnapshotString = peer.last_sync_date;
		Calendar _lastSnapshot = Util.getCalendar(_lastSnapshotString);
		if(Application.peer!=null)Application.peer.update(PeerContacts.peer_contacts);
		if(Application.peers!=null) Application.peers.setConnectionState(peer_ID, Peers.STATE_CONNECTION_TCP);
		if(ClientSync.DEBUG) out.println("Client2: transfer_TCP: Connected!");
		DD.ed.fireClientUpdate(new CommEvent(Application.ac, peer_name, client_socket.getRemoteSocketAddress(), "Server", "Connected"));
				
		ASNSyncRequest sr = ClientSync.buildRequest(_lastSnapshotString, _lastSnapshot, peer_ID);
		if(filtered) sr.orgFilter=UpdateMessages.getOrgFilter(peer_ID);
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
			String global_peer_ID, String peer_ID, String address_ID, RequestData rq, D_PeerAddress peer) throws ASN1DecoderFail, P2PDDSQLException {
		if(ClientSync.DEBUG) err.println("Client2: integrateUpdate: will integrate update: "+update.length+" datalen="+len+"::");
		if(ClientSync.DEBUG) err.println("Client2: integrateUpdate:Update: "+Util.byteToHexDump(update,len));
		SyncAnswer asa = new SyncAnswer();
		Decoder dec = new Decoder(update, 0, len);
		if(ClientSync.DEBUG) err.println("Client2: integrateUpdate: will decode");
		asa.decode(dec);
		if(ClientSync.DEBUG) out.println("Client2: integrateUpdate: Got answer: "+asa.toString());
		UpdateMessages.integrateUpdate(asa, s_address, src, global_peer_ID, peer_ID, address_ID, rq, peer);
		if(ClientSync.DEBUG) err.println("Client2: integrateUpdate: done");
	}
	private ASNUDPPing preparePing(String global_peer_ID){//, String domain, int peer_port) {
		if(ClientSync.DEBUG) err.println("Client2: preparePing");
		ASNUDPPing aup = new ASNUDPPing();
		aup.senderIsPeer=false;
		aup.senderIsInitiator=true;
		aup.initiator_domain = Identity.get_a_server_domain();//client.getInetAddress().getHostAddress();
		aup.initiator_globalID=Identity.getMyPeerGID(); //dr.initiator_globalID;
		aup.initiator_port = Identity.udp_server_port;//dr.UDP_port;
		aup.peer_globalID=global_peer_ID;
		//aup.peer_domain=domain;//;
		//if(ClientSync.DEBUG) System.out.println("Client:try_connect: domain ping = \""+aup.peer_domain+"\" vs \""+/*Util.getNonBlockingHostName(domain)+*/"\"");
		//aup.peer_port=peer_port;
		if(ClientSync.DEBUG) err.println("Client2: preparePing: done");
		return aup;
	}
	private void try_UDP_connection_directory(Peer_Connection pc, Peer_Directory ps, ASNUDPPing aup) {
		if(ClientSync.DEBUG) err.println("Client2: try_UDP_connection_directory: "+ps);
		if(ps.supernode_addr == null){ if(ClientSync.DEBUG)err.println("Client2:try_UDP_connection_directory:x1"); return;}
		if(ps.supernode_addr.isa==null){ if(ClientSync.DEBUG)err.println("Client2:try_UDP_connection_directory:x2"); return;}
		if(ps.supernode_addr.isa.isUnresolved()){ if(ClientSync.DEBUG)err.println("Client2:try_UDP_connection_directory:x3"); return;}
		if(ps.reported_peer_addr == null){ if(ClientSync.DEBUG)err.println("Client2:try_UDP_connection_directory:x4"); return;}
		if(ps.reported_peer_addr.isa==null){ if(ClientSync.DEBUG)err.println("Client2:try_UDP_connection_directory:x5"); return;}
		if(ps.reported_peer_addr.isa.isUnresolved()){ if(ClientSync.DEBUG)err.println("Client2:try_UDP_connection_directory:x6"); return;}
		aup.peer_domain=ps.reported_peer_addr.ad.domain;
		aup.peer_port=ps.reported_peer_addr.ad.udp_port;
		byte[]msg = aup.encode();
		DatagramPacket dp = new DatagramPacket(msg, msg.length);
		if(!sendUDP(ps.supernode_addr.isa, dp, pc.name)){
			if(ClientSync.DEBUG) System.out.println("Client2:try_UDP_connection_directory: fail "+ps.supernode_addr);
			return;
		}
		if(!sendUDP(ps.reported_peer_addr.isa, dp, pc.name)){
			if(ClientSync.DEBUG) System.out.println("Client2:try_UDP_connection_directory: fail "+ps.reported_peer_addr+" from "+ps.supernode_addr);
			return;
		}
		registerPeerContact(ps.reported_peer_addr.isa, pc.GID, pc.name, ps.supernode_addr, "UDP");
		if(ClientSync.DEBUG) err.println("Client2: try_UDP_connection_directory: done");
	}
	private void registerPeerContact(InetSocketAddress sock_addr, String global_peer_ID, String peer_name, SocketAddress_Domain supernode_addr, String trans){
		if(ClientSync.DEBUG) System.out.println("Client2: registerPeerContact: "+peer_name);
		Address ad = null;
		if(supernode_addr!=null) ad = supernode_addr.ad;
		registerPeerContact(sock_addr, global_peer_ID, peer_name, ad, trans);
		if(ClientSync.DEBUG) System.out.println("Client2: registerPeerContact: done");
	}
	private void registerPeerContact(InetSocketAddress sock_addr, String global_peer_ID, String peer_name, Address ad, String trans){
		if(ClientSync.DEBUG) System.out.println("Client2: registerPeerContact2: ad");
		String old_address=null; String old_type=null;
		if(ad != null) {
			old_type = ad.protocol;
			old_address = ad.domain;
		}
		String peer_key = peer_name;
		String now = Util.getGeneralizedTime();
		if(ClientSync.DEBUG) out.println("Client2: registerPeerContact2: now="+now);
		if(peer_key==null) peer_key=Util.trimmed(global_peer_ID);
		Hashtable<String, Hashtable<String,String>> pc = PeerContacts.peer_contacts.get(peer_key);
		if(pc==null){
			pc = new Hashtable<String, Hashtable<String,String>>();
			PeerContacts.peer_contacts.put(peer_key, pc);
		}
		String key = old_type+":"+old_address;
		Hashtable<String,String> value = pc.get(key);
		if(value==null){
			value = new Hashtable<String,String>();
			pc.put(key, value);
		}
		value.put(sock_addr+":"+trans+"***", now);
		if(ClientSync.DEBUG) out.println("Client2: registerPeerContact2: set adr="+old_type+":"+old_address+" val="+sock_addr+":"+trans+" "+now);
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
			//System.out.print("#_"+dp.getSocketAddress());
			if(Application.aus!=null) Application.aus.send(dp);
			else if(ClientSync.DEBUG)System.out.println("Client2: sendUDP: fail due to absent UDP Server");
		} catch (IOException e) {
			if(ClientSync.DEBUG)System.out.println("Client2: sendUDP: Fail to send ping to peer \""+peer_name+"\" at "+sock_addr);
			return false;
		}
		if(ClientSync.DEBUG) System.out.println("Client2: sendUDP done");
		return true;
	}
	private void try_UDP_connection_socket(Peer_Connection pc, Peer_Socket ps, byte[] msg) {
		if(ClientSync.DEBUG) System.out.println("Client2: try_UDP_connection_socket");
		if((ps==null)||(ps.addr==null)||(ps.addr.isa_udp==null)){
			if(ClientSync.DEBUG) System.out.println("Client2: try_UDP_connection_socket: done empty");
			return;
		}
		if(ps.addr.isa_udp.isUnresolved()) {
			if(ClientSync._DEBUG) out.println("Client2: try_UDP_connection_socket: UPeer "+pc.name+" is unresolved! "+ps.addr.isa_tcp);
			return;
		}
		DatagramPacket dp = new DatagramPacket(msg, msg.length);
		sendUDP(ps.addr.isa_udp, dp, pc.name);
		if(ClientSync.DEBUG) System.out.println("Client2: try_UDP_connection_socket: done");
	}
	private Socket try_TCP_connection(Peer_Connection pc, Peer_Socket ps) {
		if(ClientSync.DEBUG) System.out.println("Client2: try_TCP_connection: "+ps);
		if((ps==null)||(ps.addr==null)||(ps.addr.isa_tcp==null)){
			if(ClientSync.DEBUG) System.out.println("Client2: try_TCP_connection: done empty");
			return null;
		}
		if(ps.addr.isa_tcp.isUnresolved()) {
			if(ClientSync._DEBUG) out.println("Client: try_connect: UPeer "+pc.name+" is unresolved! "+ps.addr.isa_tcp);
			return null;
		}
		InetSocketAddress sock_addr = ps.addr.isa_tcp;
		try{
			if(ClientSync.DEBUG) out.println("Client: Try TCP connection!");
			Socket client_socket = new Socket();
			client_socket.connect(sock_addr, Server.TIMEOUT_Client_wait_Server);
			if(ClientSync.DEBUG) out.println("Client: Success connecting Server: "+sock_addr);
			DD.ed.fireClientUpdate(new CommEvent(this, pc.name, sock_addr,"SERVER", "Connected: "+pc.GID));
			this.registerPeerContact(sock_addr, pc.GID, pc.name, ps.addr.ad, "TCP");
			if(ClientSync.DEBUG) System.out.println("Client2: try_TCP_connection: done "+client_socket);
			return client_socket;
		}
		catch(SocketTimeoutException e){
			if(ClientSync.DEBUG) out.println("Client: TIMEOUT connecting: "+sock_addr);
			DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Connection TIMEOUT"));
			//continue; //
			return null;
		}
		catch(ConnectException e){
			if(ClientSync.DEBUG) out.println("Client: Connection Exception: "+e);
			DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Connection e="+e));
			//continue; //
			return null;
		}
		catch(Exception e){
			if(ClientSync.DEBUG) out.println("Client: General exception try-ing: "+sock_addr+" is: "+e);
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
	@Override
	public boolean try_connect(
			ArrayList<SocketAddress_Domain> tcp_sock_addresses,
			ArrayList<SocketAddress_Domain> udp_sock_addresses,
			String old_address, String s_address, String type,
			String global_peer_ID, String peer_name,
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
					Socket client_socket = new Socket();
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
					//if(ClientSync.DEBUG) e.printStackTrace();
					DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Exception="+e));
					continue; //return false;
				}
			}
		}
		//System.out.print("#0");
		if(DD.ClientUDP) {
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
	
}