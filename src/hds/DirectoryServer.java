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
import java.io.InputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import config.ThreadsAccounting;

import util.DBInterface;
import util.Util;
import static java.lang.System.out;
import ASN1.Decoder;
import ASN1.Encoder;

// TODO Make it concurrent ... any takers?
public class DirectoryServer extends Thread{
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
	public static DBInterface db = null;
	ServerSocket ss = null;
	public static DBInterface getDirDB(String dbfile) {
		synchronized (db_monitor) {
			if (db==null) {
					try {
						db = new DBInterface(dbfile);
						ArrayList<ArrayList<Object>> a = db._select("SELECT * FROM "+table.registered.TNAME+" LIMIT 1", new String[]{}, DEBUG);
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
		return db;
	}
	public DirectoryServer(int port) throws Exception {
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
		db = getDirDB(Application.CURRENT_DATABASE_DIR()+Application.DIRECTORY_FILE);
		do{
			try{
				if(port <= 0) port = Server.getRandomPort();
				ss = new ServerSocket(port);
				connected = true;
			}catch(Exception e){
				e.printStackTrace();
				connected = false;
				port = Server.getRandomPort();
			}
		}while(!connected);
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
	public static byte[] handleAnnouncement(
			DirectoryAnnouncement da, 
			Address detected_sa, 
			DBInterface db, 
			boolean storeNAT,
			boolean TCP) throws P2PDDSQLException{
		if(DirectoryServer.VERIFY_SIGNATURES) {
			da.globalID = getGlobalPeerID(da);
			if(da.globalID == null) return null; // packet telling I need globalID
			if(unknownChallenge(da.challenge)&&remoteDate(da.date)) return null; // packet offering challenge
			if(!da.verifySignature()) return null; // packet for signature failure
		}
		synchronized(monitor_handleAnnouncement){
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
	private static byte[] _monitored_handleAnnouncement(
			DirectoryAnnouncement da,
			Address detected_sa,
			DBInterface db,
			boolean storeNAT, boolean TCP) throws P2PDDSQLException {
		if (DEBUG) System.out.println("DirectoryServer:_monitored_handleAnnouncement:Got announcement: "+da);
		if (da.address._addresses==null) {
			if (DEBUG) System.out.println("DirectoryServer:_monitored_handleAnnouncement:Got empty announcement: "+da
					+" detected="+detected_sa);
		}
		if (detected_sa != null)
			da.address._addresses = prependAddress(da.address._addresses, detected_sa);

		DirectoryServerCache.loadAndSetEntry(da, TCP);
		
		byte[] answer = new DirectoryAnnouncement_Answer(Util.getString(detected_sa)).encode();
		if (DEBUG) out.println("DS:_monitored_handleAnnouncement: sending answer: "+Util.byteToHexDump(answer));
		return answer;
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
	public void run () {
		this.setName("Directory Server");
		ThreadsAccounting.registerThread();
		try {
			DirectoryServerCache.startSaverThread();
			_run();
		}catch (Exception e){
			e.printStackTrace();
		}
		DirectoryServerCache.stopSaverThread();
		ThreadsAccounting.unregisterThread();
	}
	public void _run () {
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
				if(test.typeClass()==Encoder.CLASS_APPLICATION && test.tagVal()==DirectoryAnnouncement.TAG) {
					//out.println("DirServ: Detected directory announcement");
					InetSocketAddress isa= (InetSocketAddress)client.getRemoteSocketAddress();
					DirectoryAnnouncement da = new DirectoryAnnouncement(buffer,peek,client.getInputStream());
					out.println("DirServ: got announcement: "+da+"\n from: "+isa);
					Address detected_sa = detectUDP_Address(isa, da.address.udp_port);
					//Address detected_sa = new Address(_detected_sa);
					detected_sa = DirectoryServer.addr_NAT_detection(da, detected_sa);
					byte[] answer = handleAnnouncement(da, detected_sa, db, false, true);
					//byte[] answer = new D_DAAnswer(detected_sa).encode();
					client.getOutputStream().write(answer);
				}else{
					if(DEBUG)out.println("Received directory request");
					DirectoryRequest dr = new DirectoryRequest(buffer,peek,client.getInputStream());
					if(DEBUG)out.println("DirServ: Looking for: "+Util.getGIDhash(dr.globalID)+"\n  by "+
							Util.getGIDhash(dr.initiator_globalID));//+"\n  with source udp="+dr.UDP_port);

					String globalID = dr.globalID;
					String globalIDhash = dr.globalIDhash;
					D_DirectoryEntry de = DirectoryServerCache.getEntry(globalID, globalIDhash);
					DirectoryAnswer da = getDA(de, dr.version);

					if ((da == null) || (da.date == null))
						System.out.println("DirectoryServer:run ?why da="+da+
								"\n\tde="+de+
								"\n\tdr="+dr);
					
					byte msg[] = da.encode();
					//out.println("answer: "+Util.byteToHexDump(msg, " ")+"\n\tI.e.: "+da);
					if(_DEBUG&&(da.addresses.size()>0)){
						out.println("DirServ: *******");
						out.println("DirServ: Aanswer: "+client.getRemoteSocketAddress()+" <- "+da.toString());
					}
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
	private DirectoryAnswer getDA(D_DirectoryEntry de, int version) {
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
			db.select(sql,
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
		detected_sa.protocol = Address.NAT;
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
		det.protocol = Address.NAT;
		return det.toString();
	}
}
