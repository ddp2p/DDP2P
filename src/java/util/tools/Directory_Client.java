package util.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

import net.ddp2p.ASN1.Decoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.hds.DirectoryAnnouncement;
import net.ddp2p.common.hds.DirectoryAnnouncement_Answer;
import net.ddp2p.common.hds.DirectoryAnswer;
import net.ddp2p.common.hds.DirectoryRequest;
import net.ddp2p.common.util.DBInterface;

public
class Directory_Client {
	public static String domain = "163.118.78.40";
	public static int port = 25123;
	static String hash;
	
	public static void main(String args[]){
		try {
			_main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void _main(String args[]) throws Exception{
		net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		String op="r";
		String addr=null;
		String dir_address = null;
		DBInterface db = new DBInterface(Application.DELIBERATION_FILE);
		Application.setDB(db);
		if (args.length > 0) hash = D_Peer.DB_getPeerGIDforID(args[0]);
		System.out.println("Try: hash="+hash);
		if(hash == null) return;
		if(args.length > 1) op = args[1];
		if(args.length > 2) domain = args[2];
		if(args.length > 3) addr = args[3];
		if(args.length > 4) dir_address = args[4];
		if("r".equals(op)) request(domain, port, hash);
		if("ru".equals(op)) requestUDP(domain, port, hash);
		if(addr==null) return;
		ArrayList<Address> a = new ArrayList<Address>();
		a.add(new Address(addr));
		if("a".equals(op)) announce(domain, port, hash, a, dir_address);
		if("u".equals(op)) announceUDP(domain, port, hash, a, dir_address);
	}
	public static void request(String domain, int port, String hash) throws Exception{
		Socket s = new Socket();
		InetSocketAddress isa = new InetSocketAddress(domain, port);
		System.out.println("Try: "+isa);
		s.connect(isa);
		DirectoryRequest dr = new DirectoryRequest();
		//dr.globalIDhash = hash;
		dr.globalID = hash;
		dr.initiator_globalID = hash;
		dr.UDP_port = 4500;
		OutputStream os = s.getOutputStream();
		os.write(dr.encode());
		System.out.println("Sent: "+dr);
		InputStream io = s.getInputStream();
		byte b[] = new byte[1000];
		int k = io.read(b);
		System.out.println("Got: bytes="+k);
		DirectoryAnswer da = new DirectoryAnswer().decode(new Decoder(b));
		System.out.println("Got: "+da);
	}
	public static void requestUDP(String domain, int port, String hash) throws Exception{
		DatagramSocket s = new DatagramSocket();
		InetSocketAddress isa = new InetSocketAddress(domain, port);
		System.out.println("Try: "+isa);
		//s.connect(isa);
		DirectoryRequest dr = new DirectoryRequest();
		dr.director_udp_port = port;
		dr.directory_domain = domain;
		//dr.globalIDhash = hash;
		dr.globalID = hash;
		dr.initiator_globalID = hash;
		dr.UDP_port = 4500;
		byte[] msg = dr.encode();
		DatagramPacket os = new DatagramPacket(msg, msg.length, isa);
		s.send(os);
		System.out.println("Sent: "+dr);
		//InputStream io = s.getInputStream();
		byte b[] = new byte[1000];
		os.setData(b);
		s.receive(os);
		int k = os.getLength();
		System.out.println("Got: bytes="+k);
		DirectoryAnswer da = new DirectoryAnswer().decode(new Decoder(b));
		System.out.println("Got: "+da);
	}
	public static void announce(String domain, int port, String hash, ArrayList<Address> addr, String dir_address)
			throws Exception{
		Socket s = new Socket();
		InetSocketAddress isa = new InetSocketAddress(domain, port);
		System.out.println("Try: "+isa);
		s.connect(isa);
		DirectoryAnnouncement da = new DirectoryAnnouncement(dir_address);
		//dr.globalIDhash = hash;
		da.branch = DD.BRANCH;
		da.agent_version = DD.getMyVersion();
		da.name = net.ddp2p.common.data.HandlingMyself_Peer.getMyPeerName();
		da.setGID(hash);
		da.address.setAddresses(addr); //initiator_globalID = hash;
		da.address.udp_port = 4500;
		OutputStream os = s.getOutputStream();
		os.write(da.encode());
		System.out.println("Sent: "+da);
		InputStream io = s.getInputStream();
		byte b[] = new byte[1000];
		int k = io.read(b);
		System.out.println("Got: bytes="+k);
		DirectoryAnnouncement_Answer daa = new DirectoryAnnouncement_Answer(new Decoder(b));
		//DirectoryAnnouncement_Answer daa = new DirectoryAnnouncement_Answer().decode(new Decoder(b));
		System.out.println("Got: "+daa);
	}
	public static void announceUDP(String domain, int port, String hash,
			ArrayList<Address> addr, String dir_address)
			throws Exception{
		DatagramSocket s = new DatagramSocket();
		InetSocketAddress isa = new InetSocketAddress(domain, port);
		System.out.println("Try: "+isa);
		s.connect(isa);
		DirectoryAnnouncement da = new DirectoryAnnouncement((dir_address));
		//dr.globalIDhash = hash;
		da.branch = DD.BRANCH;
		da.agent_version = DD.getMyVersion();
		da.name = net.ddp2p.common.data.HandlingMyself_Peer.getMyPeerName();
		da.setGID(hash);
		da.address.setAddresses(addr); //initiator_globalID = hash;
		da.address.udp_port = 4500;
		byte msg[] = da.encode();
		DatagramPacket os = new DatagramPacket(msg, msg.length, isa);
		s.send(os);
		System.out.println("Sent: "+da);
		byte b[] = new byte[1000];
		os.setData(b);
		s.receive(os);
		int k = os.getLength();
		System.out.println("Got: bytes="+k);
		DirectoryAnnouncement_Answer daa = new DirectoryAnnouncement_Answer(new Decoder(b));
		//DirectoryAnnouncement_Answer daa = new DirectoryAnnouncement_Answer().decode(new Decoder(b));
		System.out.println("Got: "+daa);
	}
}
