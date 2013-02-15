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
import streaming.SpecificRequest;
import streaming.UpdateMessages;
import util.CommEvent;
import util.Util;
import widgets.directories.DirectoriesData;
import widgets.peers.PeerContacts;
import widgets.peers.Peers;
import static util.Util._;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;

import ciphersuits.SK;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;
import config.Identity;
import data.D_PeerAddress;
import data.D_PluginInfo;
import data.D_PluginData;

public
class Client extends Thread{
	private static final long PAUSE = 300000;
	static final int MAX_BUFFER = 1000000;
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	//private static final boolean SIGN_PEER_ADDRESS_SEPARATELY = false;
	public static int MAX_ITEMS_PER_TYPE_PAYLOAD = 10;
	
	public static ASNSyncPayload payload = new ASNSyncPayload();
	private static SpecificRequest _payload_fix = new SpecificRequest();
	public static void addToPayloadFix(int type, String hash, String org_hash, int MAX_ITEM){
		_payload_fix.add(type, hash, org_hash, MAX_ITEM);
	}
	public static SpecificRequest payload_recent = new SpecificRequest();
	public static boolean recentlyTouched = false; // to avoid stopping
	public static int peersAvailable = 0;
	public static int peersToGo = 0;
	Socket s=new Socket();
	public Client() {
		try {
			buildStaticPayload();
		} catch (SQLiteException e) {
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
		for(RequestData a: payload_recent.rd) {
			if(org_hash.equals(a.global_organization_ID_hash)) {
				target = a;
			}
		}
		if(target == null) {
			target = new RequestData();
			payload_recent.rd.add(target);
		}
		
		if (target.addHashIfNewTo(hash, type, MAX_ITEMS_PER_TYPE_PAYLOAD))
			try {
				DD.touchClient();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (SQLiteException e) {
				e.printStackTrace();
			}
	}
	
	public boolean isMyself(int port, InetSocketAddress sock_addr){
		if(sock_addr.getPort()!=port){
			if(DEBUG) out.println("Client: Peer has different port! "+sock_addr+"!="+port);
			return false;
		}
		String haddress = "";
		if(sock_addr.getAddress()!=null) haddress=sock_addr.getAddress().getHostAddress();
		String phost = Util.getNonBlockingHostName(sock_addr);
		if(
						phost.equals("/127.0.0.1")
						||phost.equals("localhost/127.0.0.1")
						||phost.equals("localhost")
						||phost.equals("127.0.0.1")
						||"127.0.0.1".equals(haddress)
				){
			if(DEBUG) out.println("Client: Peer is Myself lo! "+sock_addr);
			//DD.ed.fireClientUpdate(new CommEvent(this, null, null,"FAIL ADDRESS", sock_addr+" Peer is LOOPBACK myself!"));
			return true;
		}else
			if(DEBUG)out.println("Client: Peer is not Myself lo! \""+phost+
				"\""+haddress+"\""+port);
		return false;
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
				ArrayList<InetSocketAddress> peer_directories_udp_sockets, ArrayList<String> peer_directories) {
		String old_address = s_address;
		if(DEBUG) out.println("Client:try_connect: Try to connect to: "+ s_address+" ty="+type+" ID="+Util.trimmed(global_peer_ID));
		InetSocketAddress sock_addr;
		ArrayList<Address> adr_addresses;
		ArrayList<InetSocketAddress> tcp_sock_addresses=new ArrayList<InetSocketAddress>();
		ArrayList<InetSocketAddress> udp_sock_addresses=new ArrayList<InetSocketAddress>();
		
		String peer_key = peer_name;
		String now = Util.getGeneralizedTime();
		if(DEBUG) out.println("Client: now="+now);
		if(peer_key==null) peer_key=Util.trimmed(global_peer_ID);
		Hashtable<String, Hashtable<String,String>> pc = PeerContacts.peer_contacts.get(peer_key);
		if(pc==null){
			pc = new Hashtable<String, Hashtable<String,String>>();
			PeerContacts.peer_contacts.put(peer_key, pc);
		}
		
		if(Address.DIR.equals(type)) {
			DD.ed.fireClientUpdate(new CommEvent(this, s_address,null,"DIR REQUEST", peer_name+" ("+global_peer_ID+")"));
			if(DEBUG) out.println("Client: will getDir");
			adr_addresses = getDirAddress(s_address, global_peer_ID, peer_name);
			if(DEBUG) out.print("Client: did getDir: ");
			if(adr_addresses == null){
				if(DEBUG) out.print(" ");
				
				String key = type+":"+s_address;
				Hashtable<String,String> value = pc.get(key);
				if(value==null){
					value = new Hashtable<String,String>();
					pc.put(key, value);
				}
				value.put("No contact", now);
				
				return false;
			}
			for(int k=0;k<adr_addresses.size();k++) {
				Address ad = adr_addresses.get(k);
				if(DEBUG) out.print("+"+ad+" ");
				
				String key = type+":"+s_address;
				Hashtable<String,String> value = pc.get(key);
				if(value==null){
					value = new Hashtable<String,String>();
					pc.put(key, value);
				}
				value.put(ad+"", now);
				
				if(DEBUG) out.println("Client: enum d_adr="+peer_key+":"+type+":"+s_address+" val="+ad+" "+now);

				InetSocketAddress ta=null,ua=null;
				try{
					if(ad.tcp_port>0)
						ta=new InetSocketAddress(ad.domain,ad.tcp_port);
				}catch(Exception e){e.printStackTrace();}
				if(ta!=null)tcp_sock_addresses.add(ta);
				try{
					if(ad.udp_port>0)
						ua=new InetSocketAddress(ad.domain,ad.udp_port);
				}catch(Exception e){e.printStackTrace();}
				if(ua!=null)udp_sock_addresses.add(ua);
			}
			if(DEBUG) out.println("");
			s_address=Util.concat(adr_addresses.toArray(), DirectoryServer.ADDR_SEP);
			if(DEBUG) out.println("Client: Will try DIR obtained address: "+s_address);
			DD.ed.fireClientUpdate(new CommEvent(this, old_address, null,"DIR ANSWER", peer_name+" ("+s_address+")"));
		}else{
			if(DEBUG) out.println("Client: Will try simple address: "+s_address);
			
			String key = type+":"+s_address;
			Hashtable<String,String> value = pc.get(key);
			if(value==null){
				value = new Hashtable<String,String>();
				pc.put(key, value);
			}
			value.put(s_address, now);
			pc.put(type+":"+s_address, value);
			if(DEBUG) out.println("Client: enum s_adr="+peer_key+":"+type+":"+s_address+" val="+s_address+" "+now);
			
			tcp_sock_addresses=new ArrayList<InetSocketAddress>();
			sock_addr=getTCPSockAddress(s_address);
			if(sock_addr!=null) tcp_sock_addresses.add(sock_addr);
			udp_sock_addresses=new ArrayList<InetSocketAddress>();
			sock_addr=getUDPSockAddress(s_address);
			if(sock_addr!=null) udp_sock_addresses.add(sock_addr);
		}

		if(DEBUG) out.println("Client: Client received addresses:"+s_address);
		if(DEBUG) out.println("Client: Client received addresses #:"+tcp_sock_addresses.size());
		if(DD.ClientTCP) {			
			for(int k=0; k<tcp_sock_addresses.size(); k++) {
				if(DEBUG) out.println("Client try address["+k+"]"+tcp_sock_addresses.get(k));
			
				//address = addresses[k];
				sock_addr=tcp_sock_addresses.get(k);//getSockAddress(address);
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
				if(this.isMyself(Identity.port, sock_addr)){
					if(DEBUG) out.println("Client: Peer is myself!");
					continue;
				}
				try{
					if(DEBUG) out.println("Client: Try TCP connection!");
					s = new Socket();
					s.connect(sock_addr, Server.TIMEOUT_Client_wait_Server);
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
		if(DD.ClientUDP) {
			if(Application.aus == null){
				DD.ed.fireClientUpdate(new CommEvent(this, s_address,null,"FAIL: UDP Server not running", peer_name+" ("+global_peer_ID+")"));
				if(_DEBUG) err.println("UClient socket not yet open, no UDP server");
				return false;
			}
			if(DEBUG) out.println("UClient received addresses #:"+udp_sock_addresses.size());
			for(int k=0; k<udp_sock_addresses.size(); k++) {
				if(DEBUG) out.println("UClient try address["+k+"]"+udp_sock_addresses.get(k));

				if((Application.aus!=null)&&(!Application.aus.hasSyncRequests(global_peer_ID))) {
					DD.ed.fireClientUpdate(new CommEvent(this, peer_name, null, "LOCAL", "Stop sending: Received ping confirmation already handled from peer"));
					if(DEBUG) System.out.println("UDPServer Ping already handled for: "+Util.trimmed(global_peer_ID));
					{
						String key = type+":"+old_address;
						Hashtable<String,String> value = pc.get(key);
						if(value==null){
							value = new Hashtable<String,String>();
							pc.put(key, value);
						}
						sock_addr=udp_sock_addresses.get(k);//getSockAddress(address);
						value.put(sock_addr+" UDP-"+PeerContacts.ALREADY_CONTACTED, now);
					}
					return false;					
				}
			
				sock_addr=udp_sock_addresses.get(k);//getSockAddress(address);
				if(this.isMyself(Identity.udp_server_port, sock_addr)){
					if(DEBUG) out.println("Client: UPeer is myself!"+sock_addr);
					continue;
				}
				if(sock_addr.isUnresolved()) {
					if(_DEBUG) out.println("Client: UPeer is unresolved! "+sock_addr);
					continue;
				}

				if(DEBUG)System.out.println("Client Sending Ping to: "+sock_addr+" for \""+peer_name+"\"");
				ASNUDPPing aup = new ASNUDPPing();
				aup.senderIsPeer=false;
				aup.senderIsInitiator=true;
				aup.initiator_domain = Identity.get_a_server_domain();//client.getInetAddress().getHostAddress();
				aup.initiator_globalID=Identity.current_peer_ID.globalID; //dr.initiator_globalID;
				aup.initiator_port = Identity.udp_server_port;//dr.UDP_port;
				aup.peer_globalID=global_peer_ID;
				aup.peer_domain=Util.getNonBlockingHostName(sock_addr);
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
					Application.aus.send(dp);
				} catch (IOException e) {
					if(DEBUG)System.out.println("Fail to send ping to peer \""+peer_name+"\" at "+sock_addr);
					continue;
				}
				ArrayList<InetSocketAddress> directories = peer_directories_udp_sockets; // Identity.listing_directories_inet
				if((peer_directories.size()!=directories.size()) && (directories.size()==0) )
					directories = getUDPDirectoriesSockets(peer_directories, directories);
				if(DEBUG)System.out.println("I have sent to peer the UDP packet: "+aup);
				if(DEBUG)System.out.println("I have sent the UDP ping packet to: "+directories.size()+" directories");
				for (int d=0; d<directories.size(); d++) {
					try {
						InetSocketAddress dir_adr = (InetSocketAddress)directories.get(d);
						if(dir_adr.isUnresolved()) continue;
						dp.setSocketAddress(dir_adr);
						Application.aus.send(dp);
						if(DEBUG)System.out.println("I requested ping via: "+dp.getSocketAddress()+" ping="+aup);
					} catch (IOException e) {
						if(_DEBUG)System.out.println("Client: try_connect: EEEEERRRRRRRROOOOOOORRRRR "+e.getMessage());
						//e.printStackTrace();
					}
				}
				if(DEBUG)System.out.println("I have sent to peer the UDP packet");
			}
		}
		DD.ed.fireClientUpdate(new CommEvent(this, peer_name, null, "SERVER", "Fail for: "+s_address+"("+old_address+")"));
		return false;
	}
	InetSocketAddress getTCPSockAddress(String address) {
		if(DEBUG) out.println("Client: getSockAddress of: "+address);
		String addresses[] = Address.split(address);
		if (addresses.length<=0){
			if(DEBUG) out.println("Client: Addresses length <=0 for: "+address);
			return null;
		}
		int a=Address.getTCP(addresses[0]);//.lastIndexOf(":");
		if(a<=0){
			if(DEBUG) out.println("Client: Address components !=2 for: "+addresses[0]);
			return null;
		}
		String c=Address.getDomain(addresses[0]);//.substring(0, a);
		return new InetSocketAddress(c,a);		
	}
	InetSocketAddress getUDPSockAddress(String address) {
		if(DEBUG) out.println("Client: getSockAddress of: "+address);
		String addresses[] = Address.split(address);
		if (addresses.length<=0){
			if(DEBUG) out.println("Client: Addresses length <=0 for: "+address);
			return null;
		}
		int a=Address.getUDP(addresses[0]);//.lastIndexOf(":");
		if(a<=0) a= Address.getTCP(addresses[0]);
		if(a<=0){
			if(DEBUG) out.println("Client: Address components !=2 for: "+addresses[0]);
			return null;
		}
		String c=Address.getDomain(addresses[0]);//.substring(0, a);
		return new InetSocketAddress(c,a);		
	}
	/**
	 * Send info got from remote directory to the display
	 * @param dir_address
	 * @param global_peer_ID
	 * @param peer_name
	 * @param da
	 * @param err
	 */
	public void reportDa(String dir_address, String global_peer_ID, String peer_name, DirectoryAnswer da, String err){
		String key = "DIR:"+dir_address;
		Hashtable<String,DirectoryAnswer> old_bag = DirectoriesData.dir_data.get(key);
		if(da==null) {
			da = new DirectoryAnswer();
			da.addresses=new ArrayList<Address>();
			peer_name=peer_name+": error: "+err;
		}
		if(old_bag == null) old_bag = new Hashtable<String,DirectoryAnswer>();
		if(peer_name!=null) old_bag.put(peer_name, da);
		else  old_bag.put(global_peer_ID, da);
		DirectoriesData.dir_data.put(key, old_bag);
		if(Application.directoriesData!=null){
			Application.directoriesData.setData(DirectoriesData.dir_data);
		}else{
			if(_DEBUG) out.println("Client:reportDa: cannot report to anybody");				
		}		
	}
	private ArrayList<Address> getDirAddress(String dir_address, String global_peer_ID, String peer_name) {
		if(DEBUG) out.println("Client: getDirAddress: "+dir_address+" ID="+Util.trimmed(global_peer_ID));
		InetSocketAddress sock_addr=getTCPSockAddress(dir_address);
		if(sock_addr == null){
			if(_DEBUG) out.println("Client: getDirAddress");
			reportDa(dir_address, global_peer_ID, peer_name, null, _("Null Socket"));
			return null;//"";
		}
		Socket socket = new Socket();
		try {
			socket.connect(sock_addr, Server.TIMEOUT_Client_wait_Dir);
			DirectoryRequest dr = new DirectoryRequest(global_peer_ID, Identity.current_peer_ID.globalID, Identity.udp_server_port);
			byte[] msg = dr.encode();
			socket.setSoTimeout(Server.TIMEOUT_Client_wait_Dir);
			socket.getOutputStream().write(msg);
			if(DEBUG) out.println("Client: Sending to Directory Server: "+Util.byteToHexDump(msg, " ")+dr);
			DirectoryAnswer da = new DirectoryAnswer(socket.getInputStream());
			reportDa(dir_address, global_peer_ID, peer_name, da, null);
			if(da.addresses.size()==0){
				if(_DEBUG) out.println("Client: Got no addresses!");
				socket.close();
				return null;
			}
			if(DEBUG) out.println("Dir Answer: "+da);
			socket.close();
			if(da.addresses==null){
				if(_DEBUG) out.println("Client: Got empty addresses!");
				return null;
			}
			return da.addresses;
			//InetSocketAddress s= da.address.get(0);
			//return s.getHostName()+":"+s.getPort();
		}catch (IOException e) {
			if(DEBUG) out.println("Client: getDirAddress fail: "+e+" peer: "+peer_name+" DIR addr="+dir_address);
			reportDa(dir_address, global_peer_ID, peer_name, null, e.getLocalizedMessage());
			//e.printStackTrace();
			//Directories.setUDPOn(dir_address, new Boolean(false));
		} catch (Exception e) {
			if(DEBUG) out.println("Client: getDirAddress fail: "+e+" peer: "+peer_name+" DIR addr="+dir_address);
			reportDa(dir_address, global_peer_ID, peer_name, null, e.getLocalizedMessage());
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
	public static void buildStaticPayload() throws SQLiteException{
		payload.changed_orgs = buildResetPayload();
	}
	public static ArrayList<ResetOrgInfo> buildResetPayload() throws SQLiteException{
		String sql = 
				"SELECT "+table.organization.reset_date+","+table.organization.global_organization_ID_hash+
				" FROM "+table.organization.TNAME+
				" WHERE "+table.organization.signature+" IS NOT NULL AND "+table.organization.reset_date+" IS NOT NULL AND "+table.organization.broadcasted+"='1';";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{});
		if(a.size()==0) return null;
		ArrayList<ResetOrgInfo> changed_orgs = new ArrayList<ResetOrgInfo>();
		for(ArrayList<Object> o: a) {
			ResetOrgInfo roi = new ResetOrgInfo();
			roi.reset_date = Util.getCalendar(Util.getString(o.get(0)));
			roi.hash = Util.getString(o.get(1));
			changed_orgs.add(roi);
		}
		return changed_orgs;
	}
	public static ASNSyncRequest buildRequest(String _lastSnapshotString, Calendar _lastSnapshot, String peer_ID) {
		if(DEBUG) System.out.println("Client: buildRequests: start: "+peer_ID);
		if(_lastSnapshot==null) _lastSnapshot =  Util.getCalendar(_lastSnapshotString);
		ASNSyncRequest sr = new ASNSyncRequest();
		if(DEBUG) out.println("lastSnapshot = "+_lastSnapshotString);
		sr.lastSnapshot = _lastSnapshot;
		sr.tableNames=SR.tableNames;
		
		if(!Client.payload_recent.empty() || !Client._payload_fix.empty()) {
			if(!Client.payload_recent.empty() && !Client._payload_fix.empty()) {
				Client.payload.advertised = Client.payload_recent.clone();
				Client.payload.advertised.add(Client._payload_fix);
			}
			sr.pushChanges = Client.payload;
		}
		
		try {
			sr.request = streaming.SpecificRequest.getPeerRequest(peer_ID);
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		if(DEBUG) System.out.println("Client: buildRequests: request=: "+sr);
		//sr.orgFilter=getOrgFilter();
		// version, globalID, name, slogan, creation_date, address*, broadcastable, signature_alg, served_orgs, signature*
		if(//(Application.as!=null)&&
				(Identity.getAmIBroadcastableToMyPeers())) {
			String global_peer_ID = Identity.current_peer_ID.globalID;
			if(DEBUG) System.out.println("Client: buildRequests: myself=: "+global_peer_ID);
			try {
				sr.address = D_PeerAddress.get_myself(global_peer_ID);//new D_PeerAddress(global_peer_ID, 0, true, false, true);
			} catch (Exception e) {
				e.printStackTrace();
			}
			/*
			sr.address.globalID = global_peer_ID;
			sr.address.name = Identity.current_peer_ID.name;
			sr.address.slogan = Identity.current_peer_ID.slogan;
			sr.address.creation_date = Identity.current_identity_creation_date;//Util.CalendargetInstance();
			sr.address.broadcastable = Identity.getAmIBroadcastable();
			sr.address.signature_alg = SR.HASH_ALG_V1;
			//sr.address.served_orgs = table.peer_org.getPeerOrgs(sr.address.globalID);
			//sr.address.signature = new byte[0];
			//if(SIGN_PEER_ADDRESS_SEPARATELY)
			*/
			SK sk = DD.getMyPeerSK();
			if(DEBUG)System.out.println("Client: buildRequests: will verify: "+sr.address);
			sr.address.signature = sr.address.sign(sk); // not needed as encompassed by request signature
			if(DD.VERIFY_SENT_SIGNATURES||DD.VERIFY_SIGNATURE_MYPEER_IN_REQUEST) {
				if(!sr.address.verifySignature()) {
					System.err.println("Client: buildRequests: Signature failure for: "+sr.address);
					//System.err.println("Client: buildRequests: Signature failure for sk: "+sk);
					System.err.println("Client: buildRequests: Signature failure for pk: "+ciphersuits.Cipher.getPK(sr.address.globalID));
				}else{
					if(DEBUG)System.out.println("Client: buildRequests: Signature success for: "+sr.address);				
				}
			}
			
			/*
			int dir_len=0;
			if(Identity.listing_directories_string!=null)
				dir_len = Identity.listing_directories_string.size();
			String my_server_address = Identity.current_server_addresses();
			if(my_server_address==null) {
				try {
					Server.detectDomain(Identity.udp_server_port);
				} catch (SocketException e) {
					e.printStackTrace();
				}
				my_server_address = Identity.current_server_addresses();
			}
			int server_addresses = 0;
			String[] server_addr = null;
			if (my_server_address!=null) {
				server_addr = my_server_address.split(Pattern.quote(DirectoryServer.ADDR_SEP));
				if (server_addr!=null) server_addresses = server_addr.length;
			}
			sr.address.address = new TypedAddress[dir_len + server_addresses];
			for(int k=0; k<dir_len; k++) {
				sr.address.address[k] = new TypedAddress();
				sr.address.address[k].address = Identity.listing_directories_string.get(k);
				sr.address.address[k].type = Server.DIR;
			}
			for(int k=0; k<server_addresses; k++) {
				sr.address.address[dir_len+k] = new TypedAddress();
				sr.address.address[dir_len+k].address = server_addr[k];
				sr.address.address[dir_len+k].type = Server.SOCKET;
			}
			*/
			
		/*
		if ((Identity.listing_directories_string!=null)&&(Identity.listing_directories_string.size()>0))
		sr.directory = new Address(Identity.listing_directories_string.get(Identity.preferred_directory_idx));
		 */
		}else{
			out.println("Not broadcastable: "+Application.as);
		}
		//sr.sign(); //signed before encoding
		try {
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("Client: buildRequest: will get plugin data for peerID="+peer_ID);			
			sr.plugin_msg = D_PluginData.getPluginMessages(peer_ID);
			sr.plugin_info = D_PluginInfo.getRegisteredPluginInfo();
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		return sr;
	}
	public static String getLastSnapshot(String peerID) throws SQLiteException {
		ArrayList<ArrayList<Object>> peers;
		peers = Application.db.select("SELECT "+table.peer.peer_ID+", "+table.peer.name+", "+table.peer.last_sync_date +
				" FROM "+ table.peer.TNAME +	
				" WHERE "+table.peer.used+" = 1 AND "+table.peer.global_peer_ID+" = ?;",
				new String[]{peerID});
		if(peers.size()<=0) return null;
		return (String)peers.get(0).get(2);
	}
	synchronized public void run() {
		DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Start"));
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
		} catch (SQLiteException e1) {
			e1.printStackTrace();
			return;
		}
		Client.peersAvailable = peers.size();
		if(DEBUG) out.println("Client: Found used peer adresses: "+peers.size());
		for(int p=0;;p++) {
			if(turnOff) break;
			// Avoid asking in parallel too many synchronizations (wait for answers)
			if((Application.aus!=null)&&(Application.aus.getThreads() > UDPServer.MAX_THREADS/2))
				try {
					System.out.println("Client: run: overloaded threads = "+Application.aus.getThreads());
					this.wait(Client.PAUSE);
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
					if(DEBUG) out.println("Will wait ms: "+Client.PAUSE);
					if((!Client.recentlyTouched) && (Client.peersToGo <= 0)) {
						DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Will Sleep: "+Client.PAUSE));
						synchronized(wait_lock ){
							wait_lock.wait(Client.PAUSE);
						}
						DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Wakes Up"));
					}
					Client.peersToGo--;
					Client.recentlyTouched = false;
					peers = Application.db.select(peers_scan_sql, new String[]{});
					
				} catch (InterruptedException e) {
					if(DEBUG) e.printStackTrace();
				} catch (SQLiteException e1) {
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
			try {
				peer = new D_PeerAddress(global_peer_ID, false, false, true);
			} catch (SQLiteException e2) {
				e2.printStackTrace();
			}
			String _lastSnapshotString = (String)peers.get(p).get(3);
			boolean filtered = "1".equals(Util.getString(peers.get(p).get(4)));
			ArrayList<ArrayList<Object>> peers_addr = null;
			try{
				peers_addr = Application.db.select("SELECT "+table.peer_address.address+", "+table.peer_address.type+", "+table.peer_address.peer_address_ID+
						" FROM "+table.peer_address.TNAME+" WHERE "+table.peer_address.peer_ID+" = ? ORDER BY "+table.peer_address.my_last_connection+" DESC;",
						new String[]{peer_ID});
				
			} catch (SQLiteException e1) {
				Application.warning(_("Database: ")+e1, _("Database"));
				return;
			}
			if(DEBUG)System.out.println("Client handling peer: "+Util.trimmed(peer_name));
			
			if(UDPServer.transferringPeerMessage(global_peer_ID)){
				if(DEBUG)System.out.println("Client peer already handled");
				continue;
			}
			
			
			if(Application.aus!=null) Application.aus.addSyncRequests(global_peer_ID);
			int p_addresses = peers_addr.size();
			ArrayList<String> peer_directories = getDirectories(peers_addr);
			ArrayList<InetSocketAddress> peer_directories_sockets = new ArrayList<InetSocketAddress>();
			peer_directories_sockets = getUDPDirectoriesSockets(peer_directories, peer_directories_sockets);
			Calendar _lastSnapshot=null;
			if(p_addresses>0) _lastSnapshot = Util.getCalendar(_lastSnapshotString);
			DD.ed.fireClientUpdate(new CommEvent(this, peer_name, null, "LOCAL", "Attempt to contact peer"));

			if(DEBUG) out.println("Client: Will try #"+p_addresses);
			for(int a=0; a < p_addresses; a++) {
				String address = (String) peers_addr.get(a).get(0);
				String type=(String)peers_addr.get(a).get(1);
				if((Application.aus!=null)&&!Application.aus.hasSyncRequests(global_peer_ID)) {
					DD.ed.fireClientUpdate(new CommEvent(this, peer_name, null, "LOCAL", "Stop sending: Received ping confirmation already handled from peer"));
					if(DEBUG)System.out.println("Client:run: Ping already handled for: "+Util.trimmed(global_peer_ID));
					if(DEBUG)System.out.println("Client:run: will skip: "+address+":"+type);
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
				try{peer_sockaddr=s.getRemoteSocketAddress();}catch(Exception e){
					e.printStackTrace();
					continue;
				}
			
				DD.ed.fireClientUpdate(new CommEvent(this, peer_name, peer_sockaddr, "LOCAL", "Try to connect to: "+address));
				// This function tried both TCP and UDP connections, based on the DD.ClientUDP and DD.ClientTCP, true if TCP
				if(!try_connect(address, type, global_peer_ID, peer_name, peer_directories_sockets, peer_directories)){
					if(DEBUG)System.out.println("Client:run: Ping failed for: \""+peer_name+"\" at \""+address+"\" id="+Util.trimmed(global_peer_ID));
					if(Application.peer!=null) Application.peer.update(PeerContacts.peer_contacts);
					continue;
				}
				if(Application.peer!=null)Application.peer.update(PeerContacts.peer_contacts);
				if(Application.peers!=null) Application.peers.setConnectionState(peer_ID, Peers.STATE_CONNECTION_TCP);
				if(DEBUG) out.println("Client: Connected!");
				DD.ed.fireClientUpdate(new CommEvent(this, peer_name, s.getRemoteSocketAddress(), "Server", "Connected"));
						
				ASNSyncRequest sr = Client.buildRequest(_lastSnapshotString, _lastSnapshot, peer_ID);
				if(filtered) sr.orgFilter=UpdateMessages.getOrgFilter(peer_ID);
				sr.sign();
				try {
					//out.println("Request sent last sync date: "+Encoder.getGeneralizedTime(sr.lastSnapshot)+" ie "+sr.lastSnapshot);
					//out.println("Request sent last sync date: "+sr);

					byte[] msg = sr.encode();
					if(DEBUG) out.println("Client: Sync Request sent: "+Util.byteToHexDump(msg, " ")+"::"+sr);
					s.getOutputStream().write(msg);
					/*
					Decoder testDec = new Decoder(msg);
					ASNSyncRequest asr = new ASNSyncRequest();				
					asr.decode(testDec);
					out.println("Request sent last sync date: "+Encoder.getGeneralizedTime(asr.lastSnapshot)+" ie "+asr.lastSnapshot);
					*/
					DD.ed.fireClientUpdate(new CommEvent(this, peer_name, s.getRemoteSocketAddress(), "Server", "Request Sent"));
					byte update[] = new byte[Client.MAX_BUFFER];
					if(DEBUG) out.println("Waiting data from socket: "+s+" on len: "+update.length+" timeout ms:"+Server.TIMEOUT_Client_Data);
					s.setSoTimeout(Server.TIMEOUT_Client_Data);
					InputStream is=s.getInputStream();
					int len = is.read(update);
					if(len>0){
						if(DEBUG) err.println("Client: answer received length: "+len);
						DD.ed.fireClientUpdate(new CommEvent(this, peer_name, s.getRemoteSocketAddress(), "Server", "Answered Received"));
						Decoder dec = new Decoder(update,0,len);
						System.out.println("Got first msg size: "+len);//+"  bytes: "+Util.byteToHex(update, 0, len, " "));
						if(!dec.fetchAll(is)){
							System.err.println("Buffer too small for receiving update answer!");
							continue;
						}
						len = dec.getMSGLength();
						//System.out.println("Got msg size: "+len);//+"  bytes: "+Util.byteToHex(update, 0, len, " "));
						RequestData rq = new RequestData();
						integrateUpdate(update,len, (InetSocketAddress)s.getRemoteSocketAddress(), this, global_peer_ID, peer_ID, address_ID, rq, peer);
						if(DEBUG) err.println("Client: answer received rq: "+rq);
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
						if(DEBUG) out.println("Client: No answered received!");
						DD.ed.fireClientUpdate(new CommEvent(this, peer_name, s.getRemoteSocketAddress(), "Server", "TIMEOUT_Client_Data may be too short: No Answered Received"));
					}
				} catch (SocketTimeoutException e1) {
					if(DEBUG) out.println("Read done: "+e1);
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (ASN1DecoderFail e) {
					e.printStackTrace();
				} catch (SQLiteException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				try {
					if(DEBUG) out.println("Client: will close socket");
					s.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(DEBUG) out.println("Client: done peer");
				break;
			}
		}
		DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Will Stop"));
		if(DEBUG) out.println("Client: turned Off");
	}
	/**
	 * Resolve addresses for extracting UDP sockets 
	 * used for directory servers requested to help with pierceNATs if needed
	 * @param peer_directories
	 * @return
	 */
	private ArrayList<InetSocketAddress> getUDPDirectoriesSockets(
			ArrayList<String> peer_directories, ArrayList<InetSocketAddress> udp_sock_addresses) {
		for(int i=0; i<peer_directories.size(); i++) {
				String s_address = peer_directories.get(i);
				InetSocketAddress sock_addr=getUDPSockAddress(s_address);
			if(sock_addr!=null) udp_sock_addresses.add(sock_addr);
		}
		return udp_sock_addresses;
	}

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
			String global_peer_ID, String peer_ID, String address_ID, RequestData rq, D_PeerAddress peer) throws ASN1DecoderFail, SQLiteException {
		if(DEBUG) err.println("Client: will integrate update: "+update.length+" datalen="+len+"::");
		if(DEBUG) err.println("Update: "+Util.byteToHexDump(update,len));
		SyncAnswer asa = new SyncAnswer();
		Decoder dec = new Decoder(update, 0, len);
		if(DEBUG) err.println("Client: will decode");
		asa.decode(dec);
		if(DEBUG) out.println("Client: Got answer: "+asa.toString());
		UpdateMessages.integrateUpdate(asa, s_address, src, global_peer_ID, peer_ID, address_ID, rq, peer);
	}
}
