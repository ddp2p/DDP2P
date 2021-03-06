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
import hds.DirectoryServerCache.D_DirectoryEntry;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

import util.P2PDDSQLException;
import config.Application;
import data.D_Peer;
import util.DBInterface;
import util.Util;
import static java.lang.System.out;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
 *  TODO Make it concurrent ... any takers?
 * @author msilaghi
 *
 */
public class DirectoryServer extends util.DDP2P_ServiceThread{
	public static int PORT = 25123;
	static final int MAX_DR = 100000;
	static final int MAX_DR_DA = 100000;
	static final int MAX_LEN = 100000;
	public static final String ADDR_SEP = ",";
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final boolean VERIFY_SIGNATURES = false;
	private static final long REMOTE_DATE_THRESHOLD_MILLIS = 60*60*1000; // 1 hour
	/**
	 * Monitor for opening the db database (it can be open from two place: server and widget)
	 */
	private static final Object db_monitor = new Object();
	public DatagramSocket udp_socket;
	DirectoryServerUDP dsu;
	public static DBInterface db_dir = null;
	ServerSocket ss = null;
	public static DBInterface getDirDB(String dbfile) {
		synchronized (db_monitor) {
			if (db_dir==null) {
					try {
						db_dir = new DBInterface(dbfile);
						ArrayList<ArrayList<Object>> a = db_dir._select("SELECT * FROM "+table.registered.TNAME+" LIMIT 1", new String[]{}, DEBUG);
						if (DEBUG) {
							System.out.println("DirectoryServer:getDBDir got "+a.size());
							for (ArrayList<Object> t : a) {
								System.out.println("DirectoryServer:getDBDir table "+t.size());
							}
						}
					} catch (P2PDDSQLException e) {
						System.out.println("Failure to open directory database: "+dbfile);
						e.printStackTrace();
					}
			}
		}
		return db_dir;
	}
	public DirectoryServer(int port) throws Exception {
		super ("Directory Server", false);
		boolean connected = false;
		try{
			udp_socket = new DatagramSocket(port);
			System.out.println("The DirectoryServer UDP socket was bound to port: "+port);
		}catch(SocketException e){
			udp_socket = new DatagramSocket();
			System.out.println("The DirectoryServer UDP socket was bound to port: "+udp_socket.getLocalPort());
		}
		udp_socket.setSoTimeout(Server.TIMEOUT_Server);
		dsu = new DirectoryServerUDP(this);
		if (Application.db_dir == null) {
			db_dir = getDirDB(Application.CURRENT_DATABASE_DIR()+Application.DIRECTORY_FILE);
			Application.db_dir = db_dir;
		}else db_dir = Application.db_dir;
		do {
			try {
				if (port <= 0) port = Server.getRandomPort();
				ss = new ServerSocket(port);
				connected = true;
			} catch (Exception e) {
				e.printStackTrace();
				connected = false;
				port = Server.getRandomPort();
			}
		} while(!connected);
		System.out.println("Got port: "+ss.getLocalPort());
		/*
		System.out.println("Got net: "+ss.getInetAddress());
		System.out.println("Got sock: "+ss.getLocalSocketAddress());
		System.out.println("Got obj: "+ss);
		 */
		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
		for (NetworkInterface netint : Collections.list(nets)) {
			out.printf("Display name: %s\n", netint.getDisplayName());
			out.printf("Name: %s (loopback: %s; p2p:%s; up: %s, v: %s, m:%s)\n", netint.getName(),
					""+netint.isLoopback(),
					""+netint.isPointToPoint(),
					""+netint.isUp(),
					""+netint.isVirtual(),
					""+netint.supportsMulticast()
					);
			Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
			for (InetAddress inetAddress : Collections.list(inetAddresses)) {
				out.printf("inetAddress: %s\n", inetAddress);
			}
		}
		
	}
	boolean turnOff=false;
	public void turnOff(){
		turnOff=true;
		try {ss.close();} catch (IOException e) {}
		udp_socket.close();
		this.interrupt();
		dsu.turnOff();
		//db.close();
		out.println("Turning DS off");
	}
	static Object monitor_handleAnnouncement = new Object();
	public static ArrayList<byte[]> recently_sent_challenges = new ArrayList<byte[]>();
	public static DirectoryAnnouncement_Answer handleAnnouncement(
			DirectoryAnnouncement da, 
			Address detected_sa, 
			DBInterface db, 
			boolean storeNAT,
			boolean TCP) throws P2PDDSQLException{
		if (DirectoryServer.VERIFY_SIGNATURES) {
			da.globalID = getGlobalPeerID(da);
			if(da.globalID == null) return null; // packet telling I need globalID
			if(unknownChallenge(da.challenge)&&remoteDate(da.date)) return null; // packet offering challenge
			if(!da.verifySignature()) return null; // packet for signature failure
		}
		synchronized (monitor_handleAnnouncement) {
			return _monitored_handleAnnouncement(da, detected_sa, db, storeNAT, TCP);
		}
	}
	/**
	 * Check that date is less than 1 hour apart from now
	 * @param date
	 * @return
	 */
	private static boolean remoteDate(Calendar date) {
		return Math.abs(date.getTimeInMillis()-Util.CalendargetInstance().getTimeInMillis()) > REMOTE_DATE_THRESHOLD_MILLIS;
	}
	/**
	 * Compare with recently sent challenge
	 *  (store last 2 challenges and do not generate more than 1 challenge/hour)
	 * @param challenge
	 * @return
	 */
	private static boolean unknownChallenge(byte[] challenge) {
		for (byte[] ch : recently_sent_challenges){
			if(Util.equalBytes_null_or_not(challenge, ch)) return true;
		}
		return false;
	}
	/**
	 * get it from cache
	 * @param da
	 * @return
	 */
	private static String getGlobalPeerID(DirectoryAnnouncement da) {
		if(da.globalID !=null) return da.globalID;
		D_DirectoryEntry e = DirectoryServerCache.getEntry(da.globalID, da.globalIDhash);
		return e.globalID;
	}
	/**
	 * Called with a parameter "da" already verified and validated as new and possibly signed 
	 * @param da
	 * @param detected_sa
	 * @param db
	 * @param storeNAT
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static DirectoryAnnouncement_Answer _monitored_handleAnnouncement(
			DirectoryAnnouncement da,
			Address detected_sa,
			DBInterface db,
			boolean storeNAT, boolean TCP_UDP) throws P2PDDSQLException {
		if (DEBUG) System.out.println("DirectoryServer:_monitored_handleAnnouncement:Got announcement: "+da);
		if (da.address._addresses == null) {
			if (DEBUG) System.out.println("DirectoryServer:_monitored_handleAnnouncement:Got empty announcement: "+da
					+" detected="+detected_sa);
		}
		if (detected_sa != null)
			da.address._addresses = prependAddress(da.address._addresses, detected_sa);

		DirectoryServerCache.loadAndSetEntry(da, TCP_UDP);
		
		//byte[] answer = 
		return new DirectoryAnnouncement_Answer(Util.getString(detected_sa));
		//if (DEBUG) out.println("DS:_monitored_handleAnnouncement: sending answer: "+Util.byteToHexDump(answer));
		//return answer;
	}
	/**
	 * places h in front of i
	 * @param tail
	 * @param h
	 * @return
	 */
	private static Address[] prependAddress(Address[] tail, Address h) {
		if (tail == null)
			return new Address[]{h};
		ArrayList<Address> ad = new ArrayList<Address>();
		for (Address a : tail)
			if (a!=null) ad.add(a);
		ad.add(0, h);
		return ad.toArray(new Address[0]);
		
	}
	private static byte[] _monitor_handleAnnouncement(DirectoryAnnouncement da, String detected_sa, DBInterface db, boolean storeNAT) throws P2PDDSQLException{
		if(DEBUG)System.out.println("Got announcement: "+da);
		if(da.globalID!=null)
			db.deleteNoSyncNULL(table.registered.TNAME,
					new String[]{table.registered.global_peer_ID,table.registered.instance},
					new String[]{da.globalID, da.instance},DEBUG);
		else
			db.deleteNoSyncNULL(table.registered.TNAME,
					new String[]{table.registered.global_peer_ID_hash,table.registered.instance},
					new String[]{da.globalIDhash, da.instance},DEBUG);
		String adr = da.address.addresses();
						//da.address.domain+":"+da.address.port+ADDR_SEP+detected_sa,
		if(storeNAT) adr = Address.joinAddresses(detected_sa, adr);
			
		String params[] = new String[table.registered.fields_noID_list.length];
		params[table.registered.REG_GID] = da.globalID;
		params[table.registered.REG_GID_HASH] = da.globalIDhash;
		params[table.registered.REG_INSTANCE] = da.instance;
		params[table.registered.REG_CERT] = (da.certificate.length==0)?null:Util.stringSignatureFromByte(da.certificate);
		params[table.registered.REG_ADDR] = adr;
		params[table.registered.REG_SIGN] = (da.signature.length==0)?null:Util.stringSignatureFromByte(da.signature);
		Calendar timestamp = da.date;
		if(timestamp == null) timestamp = Util.CalendargetInstance();
		params[table.registered.REG_TIME] = Encoder.getGeneralizedTime(timestamp); //(Util.CalendargetInstance().getTimeInMillis()/1000)+"";
		
		long id=db.insert(table.registered.TNAME, table.registered.fields_noID_list,
//				new String[]{table.registered.global_peer_ID,table.registered.certificate,table.registered.addresses,table.registered.signature,table.registered.timestamp},
				params);
		if(DEBUG)out.println("DirectoryServer: mon_handleAnnoncement:inserted with ID="+id);
		byte[] answer = new DirectoryAnnouncement_Answer(detected_sa).encode();
		if(DEBUG) out.println("sending answer: "+Util.byteToHexDump(answer));
		return answer;
	}
	public static String detectAddress(InetSocketAddress isa, int udp_port){
		return null;
		/* // not really useful to find TCP addresses, since they cannot be used for piercing NATs
		int tcp_port = isa.getPort(); 
		String hostName = Util.getNonBlockingHostName(isa);
		String result = (hostName.equals(isa.getAddress().getHostAddress()))?
				hostName+":"+tcp_port+":"+udp_port:
					Address.joinAddresses(isa.getAddress().getHostAddress()+":"+tcp_port+":"+udp_port, hostName+":"+tcp_port+":"+udp_port);
		return result;
		*/
	}
	public static String detectUDPAddress(InetSocketAddress isa, int port){
		return isa.getAddress().getHostAddress()+":-1:"+port;
		/* // learning the IP is enough for NAT addresses 
		String hostName = Util.getNonBlockingHostName(isa);
		return (hostName.equals(isa.getAddress().getHostAddress()))?
			hostName+":"+port:
				Address.joinAddresses(hostName+":-1:"+port,isa.getAddress().getHostAddress()+":-1:"+port);
		*/
	}
	/**
	 * 
	 * @param isa
	 * @param port
	 * @return
	 */
	public static Address detectUDP_Address(InetSocketAddress isa, int port){
		Address result = new Address(isa.getAddress().getHostAddress(), -1, port);
		return result;
	}
	public void _run () {
		//this.setName("Directory Server");
		//ThreadsAccounting.registerThread();
		try {
			DirectoryServerCache.startSaverThread();
			__run();
		}catch (Exception e){
			e.printStackTrace();
		}
		DirectoryServerCache.stopSaverThread();
		//ThreadsAccounting.unregisterThread();
	}
	public void __run () {
		out.println("Enter DS Server Thread");
		dsu.start();
		for(;;) {
			if(turnOff){
				out.println("Turned off");
				break;
			}
			//out.println("DirServ: *******");
			try{
				Socket client = ss.accept();
				//out.println("DirServ: Accepted... from: "+client.getRemoteSocketAddress());
				byte buffer[]=new byte[DirectoryServer.MAX_DR];
				int peek=client.getInputStream().read(buffer);
				//out.println("DirServ: Got ASN1 dump: "+Util.byteToHexDump(buffer,peek));
				Decoder test=new Decoder(buffer,0,peek);
				//out.println("DirServ: Decoded ASN1: class="+test.typeClass()+" val="+test.tagVal());
				DirMessage m; // recording msgs
				if (test.typeClass()==Encoder.CLASS_APPLICATION && test.tagVal()==DirectoryAnnouncement.TAG) {
					//out.println("DirServ: Detected directory announcement");
					InetSocketAddress isa= (InetSocketAddress)client.getRemoteSocketAddress();
					DirectoryAnnouncement da = new DirectoryAnnouncement(buffer,peek,client.getInputStream());
					out.println("DirServTCP: got announcement: "+da+"\n from: "+isa);
					
					recordAnnouncementMessage(isa, da, null, DirMessage.TCP, DirMessage.ANNOUNCEMENT);
					
					Address detected_sa = detectUDP_Address(isa, da.address.udp_port);
					out.println("DirServTCP: got announcement: detected = "+detected_sa);
					//Address detected_sa = new Address(_detected_sa);
					detected_sa = DirectoryServer.addr_NAT_detection(da, detected_sa);
					out.println("DirServTCP: got announcement: detected tuned = "+detected_sa);
					if (da.address.udp_port <= 0) detected_sa = null;
					DirectoryAnnouncement_Answer daa = handleAnnouncement(da, detected_sa, db_dir, false, true);
					byte[] answer = new byte[0];
					if (daa != null) answer = daa.encode();
					//byte[] answer = new D_DAAnswer(detected_sa).encode();
					client.getOutputStream().write(answer);
					
					recordAnnouncementMessage(isa, da, daa, DirMessage.TCP, DirMessage.ANN_ANSWER);
				} else {
					boolean DEBUG = true;
					if(DEBUG)out.println("Received directory request");
					// handling terms here
					DirectoryRequest dr = new DirectoryRequest(buffer,peek,client.getInputStream());
					//boolean acceptedTerms = areTermsAccepted(dr);
					InetSocketAddress isa= (InetSocketAddress)client.getRemoteSocketAddress();
					if(DEBUG)out.println("Received directory request: "+dr);

					recordRequestMessage(isa, dr, null, DirMessage.TCP, DirMessage.REQUEST);
					if(DEBUG)out.println("DirServ: Looking for: "+
							D_Peer.getGIDHashFromGID(dr.globalID)+"\n  by "+
							D_Peer.getGIDHashFromGID(dr.initiator_globalID));//+"\n  with source udp="+dr.UDP_port);

					String globalID = dr.globalID; // looking for peer GID
					String globalIDhash = dr.globalIDhash; // looking for peer GID hash
					// de has the look-for-peer and all instances stored in the db
					D_DirectoryEntry de = DirectoryServerCache.getEntry(globalID, globalIDhash);
					if (DEBUG) out.println("DirServ: From cache got: "+de);
					ASNObj da = getDA(de, dr, dr.version);

					/*
					if ((da == null) || (da.date == null))
						System.out.println("DirectoryServer:run ?why da="+da+
								"\n\tde="+de+
								"\n\tdr="+dr);
					*/
					recordRequestMessage(isa, dr, da, DirMessage.TCP, DirMessage.REQUEST_ANSWER);

					byte msg[] = da.encode();
					if (DEBUG) {
						Decoder dec = new Decoder(msg);
						DirectoryAnswerMultipleIdentities dami = new DirectoryAnswerMultipleIdentities(dec);
						System.out.println("DirectoryServer:_run:encode "+da+"\nto "+dami);
					}
					//out.println("answer: "+Util.byteToHexDump(msg, " ")+"\n\tI.e.: "+da);
					/*
					if(_DEBUG&&(da.addresses.size()>0)){
						out.println("DirServ: *******");
						out.println("DirServ: Aanswer: "+client.getRemoteSocketAddress()+" <- "+da.toString());
					}
					*/
					client.getOutputStream().write(msg);
				}
				client.close();
			}
			catch (SocketException e){
				out.println("server: "+e);
				continue;
			}
			catch(Exception e) {
				e.printStackTrace();
				continue;
			}
		}
		out.println("Turning off");
	}
	public static void recordRequestMessage(InetSocketAddress isa, DirectoryRequest dr,
			 ASNObj da, String transport, String msg_type) {
		DirMessage m;
		// recording request message
		m = new DirMessage(null);
		m.sourceIP=isa.toString();
		m.sourceGID=dr.initiator_globalID;//dr.globalID;
		m.sourceInstance = dr.initiator_instance;
		m.MsgType = transport+":"+msg_type;
		m.timestamp = m.getCurrentDateAndTime(); 
		m.requestedPeerGIDhash=D_Peer.getGIDHashFromGID(dr.globalID); // Looking for
		m.initiatorGIDhash=D_Peer.getGIDHashFromGID(dr.initiator_globalID);
		m.requestTerms = dr.terms_default;
		// Why terms are null : client issue!
//		if(dr.terms==null)
//			System.out.println("terms==null !!!!!!!!!!!!!!!!!!!!!!!!!");
//		else
//			System.out.println("terms!=null -------------------------");
		
		DirectoryMessageStorage.addNoPingMsg(dr.initiator_globalID/*dr.globalID*/, m); //GID??
		if(DirMessage.REQUEST_ANSWER.equals(msg_type)) {
			m.msg = da;
			m.respondTerms = null;//da.terms;
			// only one service offered (address of the requested peer (registered peer) )
			// no support for Symmetric NAT case (Bridging) ??
			String status = "rejected: no address avaliable";
			if(da instanceof DirectoryAnswerMultipleIdentities){ 
				if(((DirectoryAnswerMultipleIdentities)da).known)
					status = "accepted";
			}else if(((DirectoryAnswer)da).addresses!=null &&
					((DirectoryAnswer)da).addresses.size()!=0)
				status = "accepted";
			m.status = status;
			DirectoryMessageStorage.addNoPingMsg(dr.initiator_globalID/*dr.globalID*/, m); //GID??
			DirectoryMessageStorage.addLatestRequest_storage(dr.initiator_globalID/*dr.globalID*/, m); // record the latest request
			if (DEBUG) System.out.println("DirectoryServer:recording Answer message: "+m);			
		} else {
			m.msg = dr;
			DirectoryMessageStorage.addNoPingMsg(dr.initiator_globalID/*dr.globalID*/, m); //GID??

		}
	}
	public static void recordAnnouncementMessage(InetSocketAddress isa,
			DirectoryAnnouncement da, DirectoryAnnouncement_Answer daa, 
			String transport, String msg_type) {
		// recording answer message
		DirMessage m;
		m = new DirMessage(null);
		if (DirMessage.ANNOUNCEMENT.equals(msg_type)) {
			m.msg = da;
		}else m.msg = daa;
		m.sourceIP=isa.toString();
		m.sourceGID=da.globalID;
		m.sourceInstance= da.instance; //?? set or not
		m.MsgType = transport+":"+msg_type;//+DirMessage.ANN_ANSWER;
		m.timestamp = m.getCurrentDateAndTime(); 
		DirectoryMessageStorage.addAnnouncementMsg(da.globalID, m); //GID??
	}
	public static void recordPingMessage(InetSocketAddress risa, ASNUDPPing aup, String transport, String type) {
		DirMessage m;
		// recording UDP EmptyPing message (to open the nat)
		m = new DirMessage(aup);
		m.sourceIP=risa.toString();
		m.MsgType = transport+":"+type;
		m.timestamp = m.getCurrentDateAndTime(); 
		if(DirMessage.PING.equals(type)) {
			m.sourceGID = aup.initiator_globalID;
			//m.sourceInstance = aup.instance();
			m.peerGID = aup.peer_globalID;
			m.peerAddress = aup.peer_domain+":"+aup.peer_port;
		}
		DirectoryMessageStorage.addPingMsg(m); //GID?? m.sourceGID=dr.globalID;
	}
	/*
	private boolean areTermsAccepted(DirectoryRequest dr){
		//String globalID = dr.globalID;// looking for peer GID
		if(dr.only_given_instance)
			// know about specific instance or only null instance (only gid)
			// Requested peer should be notified about other instances
			return areTermsAccepted(dr.globalID, dr.instance, dr.terms);
		else
			// already know about the existance of instances and prepose terms for each desired instance.
			return areTermsAccepted(dr.globalID, dr.req_instances);
	}
	private boolean areTermsAccepted(String gid, String instance, DIR_Terms_Preaccepted[] preacceptedTerms){
			isMatch(preacceptedTerms, retrieveTerms(gid, instance));
	}
	private DIR_Terms_Requested[] retrieveTerms(String gid, String instance){
		String sql;
		String[]params;
		
		// check first for terms specific to this instance
		sql = "SELECT "+Subscriber.fields_subscribers+
			" FROM  "+Subscriber.TNAME+
		    " WHERE "+Subscriber.GID+" =? " +
			" AND " + Subscriber.instance+" =? ;";
		params = new String[]{gid, instance};
		if (DEBUG) System.out.println("DirectoryRequest:getTerms: select directory this.dir_address: "+ this.dir_address);
		
		ArrayList<ArrayList<Object>> u;
		try {
			u = Application.db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		
		if(u==null){
			// retrieve default terms 
		    sql = "SELECT "+Subscriber.fields_subscribers+
				  " FROM  "+Subscriber.TNAME+
			      " WHERE "+Subscriber.GID+" is NULL ;";
		}
		if (DEBUG) System.out.println("DirectoryRequest:getTerms: select directory this.dir_address: "+ this.dir_address);
		
		ArrayList<ArrayList<Object>> u;
		try {
			u = Application.db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		if(u==null)
			return null;
		//D_SubscriberInfo subscriber = new D_SubscriberInfo();
		DIR_Terms_Requested[] reqTerms = new DIR_Terms_Requested[u.size()];
		for(int i=0; i< u.size(); i++){
			//subscriber.init(u.get(i));
			reqTerms[i] = new DIR_Terms_Requested();
			reqTerms[i].topic = u.get(i).get(table.Subscriber.F_TOPIC);
			reqTerms[i].ad = u.get(i).get(table.Subscriber.F_AD);
			reqTerms[i].plaintext = u.get(i).get(table.Subscriber.F_PLAINTEXT);
			if(Util.stringInt2bool(_u.get(table.Subscriber.F_PAYMENT), false))
			    reqTerms[i].payment = new ;
			
		}	
	}
*/
	public static ASNObj getDA(D_DirectoryEntry de, DirectoryRequest dr, int version) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("DS: getDA: enter version"+version);
		if (version <= 1) return getDA_old(de, dr, version);
		if (DEBUG) System.out.println("DS: getDA: call multi");
		return getDA_Multi(de, dr, version);
	}

	private static ASNObj getDA_Multi(D_DirectoryEntry de, DirectoryRequest dr,
			int version) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("DS: getDA_Multi: enter only"+dr.only_given_instances);
		if (dr.only_given_instances) return getDA_Only(de, dr);
		if (DEBUG) System.out.println("DS: getDA_Multi: call All");
		return getDA_All(de, dr);
		//hds.DirectoryAnswerMultipleIdentities dami = new DirectoryAnswerMultipleIdentities();
		//return null;
	}
	private static ASNObj getDA_All(D_DirectoryEntry de, DirectoryRequest dr) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("DS: getDA_All: not implemented!");
		// TODO Auto-generated method stub
		return null;
	}
	/**
	 * Instance object passed for terms match check (not currently observed)
	 * @param dami
	 * @param i
	 * @param de
	 */
	private static void addInstanceData(DirectoryAnswerMultipleIdentities dami,
			DirectoryRequestPerInstance i,
			D_DirectoryEntry de) {
		if (!de.known) {
			if (DEBUG) System.out.println("DS: getDA_Only: unknown root de! "+de);
			return;
		}
		DirectoryAnswerInstance e = new DirectoryAnswerInstance();
		
		de.buildInstanceRequestedTerms();

		DIR_Terms_Preaccepted[] preaccepted = null;
		if (i != null) preaccepted = i.instance_terms;
		boolean termsMatch = termsMatch(preaccepted, de.instance_terms);
		if (!termsMatch) System.out.println("DS: getDA_Only: NO TERMS MATCH");
		// TODO
		//... termsMatch = true;
		if (!termsMatch) {
			e.instance_terms = de.instance_terms;
		} else {
			if (de.addresses != null) {
				for (int k = 0; k < de.addresses.length; k++) {
					e.addresses.add(de.addresses[k]);
					if (e.branch == null)
						e.branch = de.addresses[k].branch;
					if (e.agent_version == null)
						e.agent_version = de.addresses[k].getAgentVersionInts();
				}
			}
			e.signature_peer = de.signature;
			e.date_last_contact = de.timestamp;
			dami.known |= de.known;
		}
		if (dami.remote_GIDhash == null) dami.remote_GIDhash = de.globalIDhash;
		if (dami.remote_GIDhash == null) dami.remote_GIDhash = D_Peer.getGIDHashFromGID(de.globalID);
		e.instance = de.instance;
		dami.instances.add(e);
		dami.date_most_recent_among_instances = e.date_last_contact = de.timestamp;
		if (DEBUG) System.out.println("DS: getDA_Only: added NULL instance "+e);
		return;
	}
	private static ASNObj getDA_Only(D_DirectoryEntry de, DirectoryRequest dr) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("DS: getDA_Only: enter");
		DirectoryAnswerMultipleIdentities dami = new DirectoryAnswerMultipleIdentities();
		dami.directory_domain = dr.directory_domain;
		dami.directory_udp_port = dr.director_udp_port;
		//dami.version = DirectoryAnswerMultipleIdentities.V3;//dr.version;

		if ((dr == null) || (dr.req_instances == null)) {
			if (_DEBUG) System.out.println("DS: getDA_Only: enter null dr="+dr);
			// may support old versions of requests not having terms.
			addInstanceData(dami, null, de);
		}
		
		if ((dr != null) && (dr.req_instances != null)) {
			for (DirectoryRequestPerInstance i : dr.req_instances) {
				if (DEBUG) System.out.println("DS: getDA_Only: analyzing query: "+i);
				if (i.instance == null) {
					addInstanceData(dami, i, de);
					continue;
				}
				
			
				if (DEBUG) System.out.println("DS: getDA_Only: non NULL instance \""+i.instance+"\"");
				if (!de.instances.containsKey(i.instance)) {
					if (DEBUG) System.out.println("DS: getDA_Only: unknown instance \""+i.instance+"\"");
					continue;
				}
				
				D_DirectoryEntry de_crt = de.instances.get(i.instance);
				addInstanceData(dami, i, de_crt);
			/*
				DirectoryAnswerInstance e = new DirectoryAnswerInstance();
				e.instance = de_crt.instance;
				//e.branch = de_crt.;
				//de_crt
				dami.known |= de_crt.known; 
				e.signature_peer = de_crt.signature;
				dami.date_most_recent_among_instances = e.date_last_contact = de_crt.timestamp;
				if (de_crt.addresses != null) {
					if (DEBUG) System.out.println("DS: getDA_Only: got addresses #"+de_crt.addresses.length);
					for (int k=0; k < de_crt.addresses.length; k++) {
						if (DEBUG) System.out.println("DS: getDA_Only: adding address \""+k+"="+de_crt.addresses[k]+"\"");
						e.addresses.add(de_crt.addresses[k]);
						if (e.branch == null)
							e.branch = de_crt.addresses[k].branch;
						if (e.agent_version == null)
							e.agent_version = de_crt.addresses[k].getAgentVersionInts();
						// here one can delete them from addresses and reset them later
						 
					}
				} else {
					if (DEBUG) System.out.println("DS: getDA_Only: no addresses in de");
				}
				if (dami.remote_GIDhash == null) dami.remote_GIDhash = de_crt.globalIDhash;
				if (dami.remote_GIDhash == null) dami.remote_GIDhash = D_Peer.getGIDHashFromGID(de_crt.globalID);
				e.date_last_contact = de_crt.timestamp;
				dami.instances.add(e);
				if (DEBUG) System.out.println("DS: getDA_Only: added instance: "+e+" from "+de_crt+" of "+de);
				*/
			}
		}
		if (dami.remote_GIDhash == null) dami.remote_GIDhash = dr.globalIDhash;
		if (dami.remote_GIDhash == null) dami.remote_GIDhash = D_Peer.getGIDHashFromGID(dr.globalID);
		return dami;
	}
	/**
	 * Satisfied if the client offer is at least as good as the server request
	 * @param client_proposed_terms
	 * @param server_requested_terms
	 * @return
	 */
	private static boolean termsMatch(DIR_Terms_Preaccepted[] client_proposed_terms,
			DIR_Terms_Requested[] server_requested_terms) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("DirectoryServer: termsMatch: enter client ptr"+ client_proposed_terms+" server ptr="+server_requested_terms);
		if ((server_requested_terms == null) || (server_requested_terms.length == 0)) {
			if (_DEBUG) System.out.println("DirectoryServer: termsMatch: no server terms");
			return false;
		}
		if (((client_proposed_terms == null) || (client_proposed_terms.length == 0)) && (server_requested_terms.length > 0)) {
			client_proposed_terms = new DIR_Terms_Preaccepted[] {new DIR_Terms_Preaccepted().setExpectFree()};
		}
		if (DEBUG) System.out.println("DirectoryServer: termsMatch: enter client #"+ client_proposed_terms.length+" server #"+server_requested_terms.length);
		boolean matchFound = false;
		for(DIR_Terms_Preaccepted from_client : client_proposed_terms) {
			if (DEBUG) System.out.println("DirectoryServer: termsMatch: from_client"+ from_client);
			for (DIR_Terms_Requested from_server : server_requested_terms ) {
				if (DEBUG) System.out.println("DirectoryServer: termsMatch: from_client"+ server_requested_terms);
				if (
						(from_client.ad >= from_server.ad)
						&& ((from_server.payment == null) 
								|| (
										(from_client.payment != null) 
										&& (from_client.payment.satisfies(from_server.payment))
									))
						&& (from_client.plaintext >= from_server.plaintext)
						&& (
								(from_server.topic == null)
								|| Util.equalStrings_null_or_not(from_client.topic,from_server.topic))
						)
				{
					if (DEBUG) System.out.println("DirectoryServer: termsMatch: match");
					matchFound = true;
					return matchFound;
				}
			}
		}
		return matchFound;
	}
	private static DirectoryAnswer getDA_old(D_DirectoryEntry de, DirectoryRequest dr, int version) {
		DirectoryAnswer da = new DirectoryAnswer();
		if (de.addresses == null) {
			return da;
		}
		da.version = version;
		da.date = de.timestamp;
		if (da.date == null) {
			da.date = Util.CalendargetInstance();
		}
		
		if(de.addresses != null)
			for(int k=0; k<de.addresses.length; k++) {
				da.addresses.add(de.addresses[k]);
			}
		return da;
	}
	DirectoryAnswer getDA(DirectoryRequest dr) throws P2PDDSQLException {
		String sql = "select addresses, timestamp, strftime('%Y%m%d%H%M%fZ',timestamp,'unixepoch') from registered where global_peer_ID = ?;";
		ArrayList<ArrayList<Object>> adr = 
			db_dir.select(sql,
				new String[]{dr.globalID}, DEBUG);
		if(DEBUG) System.out.println("Query: "+sql+" with ?= "+Util.trimmed(dr.globalID));
		if(DEBUG) System.out.println("Found addresses #: "+adr.size());
		DirectoryAnswer da = new DirectoryAnswer();
		da.version = dr.version;
		if (adr.size() != 0) {
			Integer time= Util.Ival(adr.get(0).get(1));
			if(time==null) {
				time = new Integer(0);
				out.println("EMPTY TIME. WHY?");
			}
			Date date = new Date();
			date.setTime(time.longValue());
			da.date.setTime(date);
		
			String addresses = (String)adr.get(0).get(0);
			if(DEBUG)System.out.println("This address: "+addresses);
			String a[] = Address.split(addresses);
			for(int k=0; k<a.length; k++) {
				if((a[k]==null)||("".equals(a[k]))||("null".equals(a[k]))) continue;
				if(DEBUG)System.out.println("This address ["+k+"]"+a[k]);
				da.addresses.add(new Address(a[k]));
			}
		}else{
			if(DEBUG) out.print("Empty ");
		}
		return da;
	}
	public static void main(String[] args) {
		try {
			if(args.length>0) Application.DIRECTORY_FILE = args[0];
			try{
				if(args.length>1) DirectoryServer.PORT = Integer.parseInt(args[1]);
			}catch(Exception e){e.printStackTrace();}
			DirectoryServer ds = new DirectoryServer(DirectoryServer.PORT);
			ds.start();
		}catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}		
	}
	/**
	 * If detected not in announced, then set its protocol to NAT and return it
	 * else return null
	 * @param da
	 * @param detected_sa
	 * @return
	 */
	public static Address addr_NAT_detection(DirectoryAnnouncement da,
			Address detected_sa) {
		if(detected_sa == null) return null;
		Address[] addr = da.address._addresses;
		if ((da == null) || (da.address == null) || (da.address._addresses == null))
			return detected_sa;
		for (int k =0; k<addr.length; k++) {
			Address a = addr[k];
			if(a.domain.equals(detected_sa.domain) && a.udp_port==detected_sa.udp_port)
				return null; // not NAT
		}
		detected_sa.pure_protocol = Address.NAT;
		return detected_sa;
	}
	public static String _addr_NAT_detection(DirectoryAnnouncement da,
			String detected_sa) {
		if(detected_sa == null) return null;
		Address det = new Address(detected_sa);
		//String[] addr = Address.split(da.address.addresses);
		Address[] addr = da.address._addresses;
		for(int k =0; k<addr.length; k++) {
			Address a = addr[k]; //new Address(addr[k]);
			if(a.domain.equals(det.domain) && a.udp_port==det.udp_port) return null; // not NAT
		}
		det.pure_protocol = Address.NAT;
		return det.toString();
	}
}
