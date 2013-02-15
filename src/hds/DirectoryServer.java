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

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;

import util.DBInterface;
import util.Util;
import static java.lang.System.out;
import ASN1.Decoder;
import ASN1.Encoder;

// TODO Make it iterative ... any takers?
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
	public static byte[] handleAnnouncement(DirectoryAnnouncement da, String detected_sa, DBInterface db) throws SQLiteException{
		if(DEBUG)System.out.println("Got announcement: "+da);
		db.delete("registered", new String[]{"global_peer_ID"}, new String[]{da.globalID});
		long id=db.insert("registered", new String[]{"global_peer_ID","certificate","addresses","signature","timestamp"},
				new String[]{da.globalID,
				(da.certificate.length==0)?null:Util.stringSignatureFromByte(da.certificate),
						//da.address.domain+":"+da.address.port+ADDR_SEP+detected_sa,
						Address.joinAddresses(detected_sa, da.address.domain),
						(da.signature.length==0)?null:Util.stringSignatureFromByte(da.signature),
								(Util.CalendargetInstance().getTimeInMillis()/1000)+""}); // strftime('%s', 'now'));
		if(DEBUG)out.println("inserted with ID="+id);
		Encoder DAanswer = new Encoder().initSequence().addToSequence(new Encoder(true));
		DAanswer.setASN1Type(DD.TAG_AC14);
		byte[] answer=DAanswer.getBytes();
		if(DEBUG) out.println("sending answer: "+Util.byteToHexDump(answer));
		return answer;
	}
	public static String detectAddress(InetSocketAddress isa, int port){
		String hostName = Util.getNonBlockingHostName(isa);
		String result = (hostName.equals(isa.getAddress().getHostAddress()))?
				hostName+":"+port:
					Address.joinAddresses(isa.getAddress().getHostAddress()+":"+port, hostName+":"+port);
		return result;
	}
	public static String detectUDPAddress(InetSocketAddress isa, int port){
		String hostName = Util.getNonBlockingHostName(isa);
		return (hostName.equals(isa.getAddress().getHostAddress()))?
			hostName+":"+port:
				Address.joinAddresses(hostName+":-1:"+port,isa.getAddress().getHostAddress()+":-1:"+port);
	}
	public void run () {
		out.println("Enter DS Server Thread");
		dsu.start();
		for(;;) {
			if(turnOff){
				out.println("Turned off");
				break;
			}
			out.println("Next loop!");
			try{
				Socket client = ss.accept();
				out.println("Accepted...");
				byte buffer[]=new byte[DirectoryServer.MAX_DR];
				int peek=client.getInputStream().read(buffer);
				out.println("Got: "+Util.byteToHexDump(buffer,peek));
				Decoder test=new Decoder(buffer,0,peek);
				out.println("Decoded: class="+test.typeClass()+" val="+test.tagVal());
				if(test.typeClass()==Encoder.CLASS_APPLICATION && test.tagVal()==DirectoryAnnouncement.TAG) {
					out.println("Detected directory announcement");
					InetSocketAddress isa= (InetSocketAddress)client.getRemoteSocketAddress();
					DirectoryAnnouncement da = new DirectoryAnnouncement(buffer,peek,client.getInputStream());
					out.println("Received TCP announcement: "+da+"\n from: "+isa);
					String detected_sa = detectAddress(isa, da.address.udp_port);
					byte[] answer = handleAnnouncement(da, detected_sa, db);
					client.getOutputStream().write(answer);
				}else{
					if(DEBUG)out.println("Received directory request");
					DirectoryRequest dr = new DirectoryRequest(buffer,peek,client.getInputStream());
					out.println("Looking for: "+Util.trimmed(dr.globalID)+"\n  by "+
							Util.trimmed(dr.initiator_globalID)+"\n  with source udp="+dr.UDP_port);
					String sql = "select addresses, timestamp, strftime('%Y%m%d%H%M%fZ',timestamp,'unixepoch') from registered where global_peer_ID = ?;";
					ArrayList<ArrayList<Object>> adr = 
						db.select(sql,
							new String[]{dr.globalID});
					
					if(DEBUG) System.out.println("Query: "+sql+" with ?= "+Util.trimmed(dr.globalID));
					if(DEBUG)System.out.println("Found addresses #: "+adr.size());
					DirectoryAnswer da = new DirectoryAnswer();
					if (adr.size() != 0) {
						Integer time= (Integer)adr.get(0).get(1);
						if(time==null) {
							time = new Integer(0);
							out.println("EMPTY TIME. WHY?");
						}
						Date date = new Date();
						date.setTime(time.longValue());
						da.date.setTime(date);
					
						String addresses = (String)adr.get(0).get(0);
						System.out.println("This address: "+addresses);
						String a[] = Address.split(addresses);
						for(int k=0; k<a.length; k++) {
							if((a[k]==null)||("".equals(a[k]))||("null".equals(a[k]))) continue;
							System.out.println("This address ["+k+"]"+a[k]);
							da.addresses.add(new Address(a[k]));
						}
					}else{
						if(DEBUG) out.print("Empty ");
					}
					byte msg[] = da.encode();
					out.println("answer: "+Util.byteToHexDump(msg, " ")+"\n\tI.e.: "+da);
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
}
