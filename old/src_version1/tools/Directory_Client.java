package tools;

import hds.DirectoryAnnouncement;
import hds.DirectoryAnnouncement_Answer;
import hds.DirectoryAnswer;
import hds.DirectoryRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import util.DBInterface;

import config.Application;
import data.D_PeerAddress;

import ASN1.Decoder;

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
		String op="r";
		String addr=null;
		DBInterface db = new DBInterface(Application.DELIBERATION_FILE);
		Application.db = db;
		if(args.length>0) hash = D_PeerAddress.getPeerGIDforID(args[0]);
		System.out.println("Try: hash="+hash);
		if(hash == null) return;
		if(args.length>1) op = args[1];
		if(args.length>2) domain = args[2];
		if(args.length>3) addr = args[3];
		if("r".equals(op)) request(domain, port, hash);
		if("ru".equals(op)) requestUDP(domain, port, hash);
		if(addr==null) return;
		if("a".equals(op)) announce(domain, port, hash, addr);
		if("u".equals(op)) announceUDP(domain, port, hash, addr);
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
	public static void announce(String domain, int port, String hash, String addr) throws Exception{
		Socket s = new Socket();
		InetSocketAddress isa = new InetSocketAddress(domain, port);
		System.out.println("Try: "+isa);
		s.connect(isa);
		DirectoryAnnouncement da = new DirectoryAnnouncement();
		//dr.globalIDhash = hash;
		da.globalID = hash;
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
	public static void announceUDP(String domain, int port, String hash, String addr) throws Exception{
		DatagramSocket s = new DatagramSocket();
		InetSocketAddress isa = new InetSocketAddress(domain, port);
		System.out.println("Try: "+isa);
		s.connect(isa);
		DirectoryAnnouncement da = new DirectoryAnnouncement();
		//dr.globalIDhash = hash;
		da.globalID = hash;
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
