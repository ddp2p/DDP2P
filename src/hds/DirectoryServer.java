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

import util.DBInterface;
import util.Util;
import static java.lang.System.out;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;

/**
 * @author msilaghi
 *
 * DAAnswer = IMPLICIT [APPLICATION 14] SEQUENCE {
 * 		result BOOLEAN,
 * 		remote_IP OCTET STRING OPTIONAL,
 *      remote_port INTEGER OPTIONAL
 * }
 */

class D_DAAnswer extends ASN1.ASNObj{
	boolean result = true;
	byte[] remote_IP;
	int remote_port = 0;
	
	public String toString() {
		return "D_DAAnswer: ["+Util.concat(remote_IP, ".", "?.?.?.?")+":"+remote_port+"]";
	}
	
	public D_DAAnswer(String detected_sa) {
		if(detected_sa != null) {
			Address ad = new Address(detected_sa);
			remote_port = ad.udp_port;
			remote_IP = Util.getBytesFromCleanIPString(Util.get_IP_from_SocketAddress(ad.domain));
		}
	}

	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(result));
		if(remote_IP!=null) enc.addToSequence(new Encoder(remote_IP));
		if(remote_port > 0) enc.addToSequence(new Encoder(remote_port));
		enc.setASN1Type(DD.TAG_AC14);
		return enc;
	}

	@Override
	public D_DAAnswer decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		result = d.getFirstObject(true).getBoolean();
		Decoder rest = d.getFirstObject(true);
		if(rest == null) return this;
		if(rest.getTypeByte() == Encoder.TAG_OCTET_STRING){
			remote_IP = rest.getBytes();
			rest = d.getFirstObject(true);
		}
		if(rest == null) return this;
		if(rest.getTypeByte() == Encoder.TAG_INTEGER){
			remote_port = rest.getInteger().intValue();
			rest = d.getFirstObject(true);
		}
		if(rest != null) throw new ASN1DecoderFail("Extra bytes in answer");
		return this;
	}
	
}

// TODO Make it concurrent ... any takers?
public class DirectoryServer extends Thread{
	public static final int PORT = 25123;
	static final int MAX_DR = 100000;
	static final int MAX_DR_DA = 100000;
	static final int MAX_LEN = 100000;
	public static final String ADDR_SEP = ",";
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public DatagramSocket udp_socket;
	DirectoryServerUDP dsu;
	public DBInterface db = null;
	ServerSocket ss = null;
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
		if (db==null) {
				db = new DBInterface(Application.DIRECTORY_FILE);
		}
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
	public static byte[] handleAnnouncement(DirectoryAnnouncement da, String detected_sa, DBInterface db, boolean storeNAT) throws P2PDDSQLException{
		if(DEBUG)System.out.println("Got announcement: "+da);
		db.delete("registered", new String[]{"global_peer_ID"}, new String[]{da.globalID});
		String adr = da.address.addresses;
						//da.address.domain+":"+da.address.port+ADDR_SEP+detected_sa,
		if(storeNAT) adr = Address.joinAddresses(detected_sa, adr);
			
		long id=db.insert("registered", new String[]{"global_peer_ID","certificate","addresses","signature","timestamp"},
				new String[]{da.globalID,
				(da.certificate.length==0)?null:Util.stringSignatureFromByte(da.certificate),
						adr,
						(da.signature.length==0)?null:Util.stringSignatureFromByte(da.signature),
								(Util.CalendargetInstance().getTimeInMillis()/1000)+""}); // strftime('%s', 'now'));
		if(DEBUG)out.println("inserted with ID="+id);
		byte[] answer = new D_DAAnswer(detected_sa).encode();
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
	public void run () {
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
					String detected_sa = detectUDPAddress(isa, da.address.udp_port);
					detected_sa = DirectoryServer.addr_NAT_detection(da, detected_sa);
					byte[] answer = handleAnnouncement(da, detected_sa, db, false);
					//byte[] answer = new D_DAAnswer(detected_sa).encode();
					client.getOutputStream().write(answer);
				}else{
					if(DEBUG)out.println("Received directory request");
					DirectoryRequest dr = new DirectoryRequest(buffer,peek,client.getInputStream());
					if(DEBUG)out.println("DirServ: Looking for: "+Util.getGIDhash(dr.globalID)+"\n  by "+
							Util.getGIDhash(dr.initiator_globalID));//+"\n  with source udp="+dr.UDP_port);
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
	public static void main(String[] args) {
		try {
			if(args.length>0) Application.DIRECTORY_FILE = args[0];
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
	public static String addr_NAT_detection(DirectoryAnnouncement da,
			String detected_sa) {
		if(detected_sa == null) return null;
		Address det = new Address(detected_sa);
		String[] addr = Address.split(da.address.addresses);
		for(int k =0; k<addr.length; k++) {
			Address a = new Address(addr[k]);
			if(a.domain.equals(det.domain) && a.udp_port==det.udp_port) return null; // not NAT
		}
		det.protocol = Address.NAT;
		return det.toString();
	}
}
