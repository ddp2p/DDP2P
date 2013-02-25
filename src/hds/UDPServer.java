/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.regex.Pattern;

import javax.swing.Icon;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;
import ciphersuits.Cipher;
import ciphersuits.SK;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;
import config.Identity;
import data.D_PeerAddress;
import data.D_PluginInfo;
import streaming.OrgHandling;
import streaming.RequestData;
import streaming.UpdateMessages;
import streaming.UpdatePeersTable;
import util.CommEvent;
import util.DBInterface;
import util.Util;
import widgets.peers.Peers;
import static java.lang.System.err;
import static java.lang.System.out;
import static util.Util._;

public class UDPServer extends Thread {
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static final int MAX_THREADS = 6;
	private static final int UDP_BUFFER_LENGTH = 1000000;
	public static final Object directoryAnnouncementLock = new Object();
	public static boolean DEBUG_ALIVE = false;
	public static boolean DEBUG_DIR = false;
	public static DirectoryAnnouncement directoryAnnouncement = null;
	private byte[] buffer;
	public DatagramSocket ds;
	Hashtable<String, UDPMessage> sentMessages = new Hashtable<String, UDPMessage>();
	Hashtable<String, UDPMessage> recvMessages = new Hashtable<String, UDPMessage>();
	// Set of peers to which a sync was sent
	public HashSet<String> synced = new HashSet<String>(); 
	Random rnd = new Random();
	public static boolean transferringPeerAnswerMessage(String global_peer_ID) {
		if(global_peer_ID==null) return false;
		for (UDPMessage um : Application.aus.recvMessages.values()) {
			if (global_peer_ID.equals(um.senderID)&&
					(um.type == DD.MSGTYPE_SyncAnswer)) {
				return true;
			}
		}
		return false;
	}
	public static boolean transferringPeerRequestMessage(String global_peer_ID) {
		if(global_peer_ID==null) return false;
		for (UDPMessage um : Application.aus.sentMessages.values()) {
			if (global_peer_ID.equals(um.destinationID)&&
					(um.type == DD.MSGTYPE_SyncRequest) )return true;
		}
		return false;
	}
	public static boolean transferringPeerMessage(String global_peer_ID) {
		if(Application.aus == null) return false;
		if(transferringPeerAnswerMessage(global_peer_ID)) return true;
		if(transferringPeerRequestMessage(global_peer_ID)) return true;
		return false;
	}
	public void resetSyncRequests(){
		synchronized(synced){
			synced.clear();
		}
	}
	public void addSyncRequests(String gID){
		if(DEBUG)System.out.print("UDServer:addSyncRequests:Adding "+Util.trimmed(gID)+" in synced: ");
		synchronized(synced){
			if(!synced.contains(gID))
				synced.add(gID);
			else if(DEBUG)System.out.print(" (old) ");
			for(String s:synced){
				if(DEBUG)System.out.print(Util.trimmed(s)+",");
			}
		}
		if(DEBUG)System.out.println("");
	}
	public void delSyncRequests(String gID){
		if(DEBUG)System.out.print("UDServer:delSyncRequests:Deleting "+Util.trimmed(gID)+" in synced: ");
		synchronized(synced){
			synced.remove(gID);
			for(String s:synced){
				if(DEBUG)System.out.print(Util.trimmed(s)+",");
			}
		}
		if(DEBUG)System.out.println("");
	}
	public boolean hasSyncRequests(String gID){
		boolean res=false;
		if(DEBUG)System.out.print("UDPServer:hasSyncRequests:Look "+Util.trimmed(gID)+" in synced: ");
		synchronized(synced){
			for(String s:synced){
				if(DEBUG)System.out.print(Util.trimmed(s)+",");
				if(gID.equals(s)){
					res=true;
					break;
				}
			}
		}
		if(DEBUG)System.out.println(" result="+res);
		return res;//synced.contains(gID);
	}
	public void sendLargeMessage(SocketAddress sa, byte[] msg, int MTU, String destGID, int type) throws IOException {
		if(MTU >= msg.length) {
			DatagramPacket dp= new DatagramPacket(msg, msg.length);
			dp.setSocketAddress(sa);
			ds.send(dp);
			if(DEBUG) System.out.println("Sent message in one fragment to "+sa);
			return;
		}
		int frags = (int) Math.ceil(msg.length/(MTU*1.0));
		
		// The umsg has to be stored in sentMessages before sending fragments
		// to avoid generating a Nack on an early ack
		UDPMessage umsg = new UDPMessage(frags);
		umsg.msgID = rnd.nextInt()+"";
		umsg.type = type;
		sentMessages.put(umsg.msgID,umsg);

		if(DEBUG)System.out.println("Sending to: "+sa+" msgID="+umsg.msgID+" msg["+msg.length+"]="+Util.byteToHexDump(msg));
		for(int k=0; k<frags; k++) {
			UDPFragment uf = new UDPFragment();
			uf.senderID = Identity.current_peer_ID.globalID;
			uf.destinationID = destGID;
			uf.msgType = type;
			uf.msgID = umsg.msgID;
			uf.sequence = k;
			uf.fragments = frags;
			uf.signature = new byte[0];
			int len_this=Math.min(msg.length-MTU*k, MTU);
			uf.data = new byte[len_this];
			Util.copyBytes(uf.data, 0, msg, uf.data.length, MTU*k);
			umsg.fragment[k] = uf;
			byte[]frag = uf.encode();
			DatagramPacket dp= new DatagramPacket(frag, frag.length);
			dp.setSocketAddress(sa);
			ds.send(dp);
			if(DEBUG)System.out.println("Sent UDPFragment: "+uf+" to "+sa);
			if(DEBUG)System.out.println("Sent UDPFragment: "+uf.sequence+"/"+uf.fragments+" to "+sa+" of "+uf.msgID);
		}
	}
	public void getFragmentAck(UDPFragmentAck frag){
		if(DEBUG) System.out.println("getAck");
		
		synchronized(sentMessages) {
		UDPMessage umsg = sentMessages.get(frag.msgID+"");
		if(umsg == null) return;

		byte[] signature = frag.signature;
		String senderID = frag.senderID;
		
		// prepare for verification
		frag.signature = new byte[0];
		frag.senderID = "";
		if(!Util.verifySign(frag, Cipher.getPK(senderID), signature)){
			System.err.println("Failure verifying: "+frag+
					"\n ID="+Util.trimmed(senderID)+ 
					"\n sign="+Util.byteToHexDump(signature));
			return;
		}
		frag.signature = signature;
		frag.senderID = senderID;
		
		for(int i=0; i<frag.transmitted.length; i++) {
			if(frag.transmitted[i]==1)	
				if(umsg.transmitted[i] == 0) {
					umsg.received++;
					umsg.transmitted[i] = 1;
				}
		}
		if(umsg.received >= umsg.fragment.length) {
			sentMessages.remove(umsg.msgID);
			if(DEBUG)System.out.println("Message discarded "+umsg.msgID);
		}
		}
	}
	public void getFragmentNAck(UDPFragmentNAck frag){
		if(DEBUG) System.out.println("get Nack: "+frag.msgID);
		byte[] signature = frag.signature;
		String senderID = frag.senderID;
		
		// prepare for verification
		frag.signature = new byte[0];
		frag.senderID = "";
		if(!Util.verifySign(frag, Cipher.getPK(senderID), signature)){
			System.err.println("Failure verifying: "+frag+
					"\n ID="+Util.trimmed(senderID)+ 
					"\n sign="+Util.byteToHexDump(signature));
			return;
		}
		frag.signature = signature;
		frag.senderID = senderID;
		
		synchronized(recvMessages) {
			recvMessages.remove(frag.msgID+"");
		}
		if(DEBUG) System.out.println("get Nack: removing message"+frag.msgID);
	}
	public void getFragmentReclaim(UDPFragmentAck frag, SocketAddress sa) throws IOException{
		if(DEBUG) System.out.println("get Reclaim");
		byte[] signature = frag.signature;
		String senderID = frag.senderID;
		
		// prepare for verification
		frag.signature = new byte[0];
		frag.senderID = "";
		if(!Util.verifySign(frag, Cipher.getPK(senderID), signature)){
			System.err.println("Failure verifying: "+frag+
					"\n ID="+Util.trimmed(senderID)+ 
					"\n sign="+Util.byteToHexDump(signature));
			return;
		}
		
		UDPMessage umsg = sentMessages.get(frag.msgID+"");
		if(umsg == null) {
			if(DEBUG) System.out.println("Reclaim for discarded message: "+frag.msgID);
			UDPFragmentNAck uf = new UDPFragmentNAck();
			uf.msgID = frag.msgID;
			uf.destinationID = frag.senderID;
			uf.transmitted = frag.transmitted;
			
			// prepare for signature
			uf.signature = new byte[0];
			uf.senderID="";
			uf.signature = Util.sign_peer(uf);//frag.destinationID);
			uf.senderID = frag.destinationID;
			if(DEBUG)System.out.println("Sending NAck: "+uf);
			byte[] ack = uf.encode();
			DatagramPacket dp = new DatagramPacket(ack, ack.length,sa);
			this.ds.send(dp);
			if(DEBUG)System.out.println("Sending NAck: "+uf.msgID);
			return;
		}
		if(DEBUG) System.out.println("Answering Reclaim: "+frag+"   for umsg="+umsg);
		for(int i=0; i<frag.transmitted.length; i++) {
			if(frag.transmitted[i]==1)	
				if(umsg.transmitted[i] == 0) {
					umsg.received++;
					umsg.transmitted[i] = 1;
				}
			if(umsg.transmitted[i] == 0) {
				UDPFragment uf = umsg.fragment[i];
				byte[]fragb = uf.encode();
				DatagramPacket dp= new DatagramPacket(fragb, fragb.length);
				dp.setSocketAddress(sa);
				ds.send(dp);
				if(DEBUG)System.out.println("Sent UDPFragment: "+uf+" to "+sa);
				if(DEBUG)System.out.println("ReSent UDPFragment: "+uf.sequence+"/"+umsg.msgID+" to "+sa);				
			}
		}
	}
	public void sendFragmentReclaim(){
		if(DEBUG_ALIVE) out.println("userver: UDPServer Reclaim! messages #"+recvMessages.size());
		long crt_time = Util.CalendargetInstance().getTimeInMillis();
		synchronized(this.recvMessages) {
		for(UDPMessage umsg: recvMessages.values()) {
			if(DEBUG) out.println("userver: UDPServer Reclaim "+umsg.msgID+" check: "+crt_time+"-"+umsg.date+"="+(crt_time-umsg.date)+">"+Server.TIMEOUT_UDP_Reclaim);
			if(crt_time-umsg.date>Server.TIMEOUT_UDP_Reclaim) {
				umsg.date = crt_time;
				if(DEBUG) out.println("userver: UDPServer Reclaim at "+crt_time+"ms: "+umsg);
				//for(int k=0; k<umsg.fragment.length; k++) {
				//	if(umsg.transmitted[k]==0) {
						UDPReclaim uf = new UDPReclaim();
						uf.msgID = umsg.msgID;
						uf.destinationID = umsg.senderID;
						uf.transmitted = umsg.transmitted;
						
						// prepare for signature
						uf.signature = new byte[0];
						uf.senderID="";
						uf.signature = Util.sign_peer(uf);//umsg.destinationID);
						uf.senderID = umsg.destinationID;
						if(DEBUG)System.out.println("Sending reclaim: "+uf);
						byte[] ack = uf.encode();
						DatagramPacket dp;
						try {
							dp = new DatagramPacket(ack, ack.length, umsg.sa);
							if(DEBUG) out.println("userver: UDPServer Reclaim sent to "+umsg.sa+" recl=: "+ uf);
							this.ds.send(dp);
							if(DEBUG) out.println("userver: UDPServer Reclaim sent to "+umsg.sa+" recl=: "+ umsg.received+"/"+umsg.fragment.length+" of "+umsg.msgID);
						} catch (SocketException e1) {
							e1.printStackTrace();
						} catch (IOException e2) {
							e2.printStackTrace();
						}
					//}
				//}
			}
		}
		}
	}
	public byte[] getFragment(UDPFragment frag, SocketAddress sa) throws IOException{
		byte[] result = null;
		
		byte[] signature = frag.signature;
		String senderID = frag.senderID;
		
		// prepare for verification
		frag.signature = new byte[0];
		frag.senderID = "";
		if(!Util.verifySign(frag, Cipher.getPK(senderID), signature)){
			System.err.println("Failure verifying: "+frag+
					"\n ID="+Util.trimmed(senderID)+ 
					"\n sign="+Util.byteToHexDump(signature));
			return null;
		}
		frag.signature = signature;
		frag.senderID = senderID;
		
		String msgID = frag.msgID+"";
		
		
		if(DEBUG){
			System.out.println("Running messages: "+recvMessages.size());
			Enumeration<String> e = recvMessages.keys();		   
			//iterate through Hashtable keys Enumeration
			while(e.hasMoreElements()){
				String key = e.nextElement();
				if(DEBUG)System.out.println("Element "+key+" comparison:"+msgID.equals(key)+" vs="+msgID);
			}
		}
	
	    UDPMessage umsg;
	    synchronized (recvMessages) {
	    	umsg = recvMessages.get(msgID);
	    	if(umsg == null) {
	    		umsg = new UDPMessage(frag.fragments);
	    		umsg.uf.msgID = frag.msgID;
	    		umsg.uf.destinationID = frag.senderID;
	    		umsg.uf.transmitted = umsg.transmitted;
	    		umsg.type = frag.msgType;
	    		
	    		umsg.sa = sa;
	    		umsg.destinationID = frag.destinationID;
	    		umsg.senderID = frag.senderID;
	    		recvMessages.put(msgID, umsg);
	    		umsg.msgID = frag.msgID;
	    		if(DEBUG)System.out.println("Starting new message: "+umsg);
	    	}else
	    		if(DEBUG)System.out.println("Located message: "+umsg);
	    	umsg.date = Util.CalendargetInstance().getTimeInMillis();
	    	
	    	// add new fragment
	    	if(frag.sequence>=umsg.fragment.length){
	    		if(DEBUG)System.err.println("Failure sequence: "+frag.sequence+" vs. "+umsg.fragment.length);
	    		return null;
	    	}
	    	if(umsg.transmitted[frag.sequence]==0) {
	    		umsg.transmitted[frag.sequence] = 1;
	    		umsg.received++;
	    		umsg.ack_changed = true;
	    	}
	    	umsg.fragment[frag.sequence] = frag;
	    
		
			if(umsg.received>=umsg.fragment.length){
				byte[] msg = umsg.assemble();
				recvMessages.remove(umsg.fragment[0].msgID);
				if(DEBUG)System.out.println("Removing received message: "+umsg.msgID);
				if(DEBUG)System.out.println("getFragments: Got message completely: "+umsg.msgID+" msg="+Util.byteToHexDump(msg));
				result = msg;
			}else{
				if(DEBUG)System.out.println("getFragments. Got: "+umsg.received+"/"+umsg.fragment.length+" received msg="+umsg);
				result = null;//umsg.assemble();
			}
	    }
	    
	    // could arrange for a separate thread to send the ack outside the critical section
	    
	    synchronized(umsg.lock_ack){
	    	// prepare for signature
	    	if(umsg.ack_changed) {
	    		umsg.uf.signature = new byte[0];
	    		umsg.uf.senderID="";
	    		umsg.uf.senderID = frag.destinationID;
	    		umsg.uf.signature = Util.sign_peer(umsg.uf);//frag.destinationID);
	    		if(DEBUG)System.out.println("getFragments: Preparing ack: "+umsg.uf+" umsgID="+umsg.msgID+" to: "+sa);
	    		umsg.ack = umsg.uf.encode();
	    	}
	    	DatagramPacket dp = new DatagramPacket(umsg.ack, umsg.ack.length,sa);
	    	this.ds.send(dp);
	    	if(DEBUG)System.out.println("getFragments: Sent ack: "+frag.sequence+"/"+umsg.received+" umsgID="+umsg.msgID+" to: "+sa);
	    }
	    return result;
	}
	/**
	 * The socket is allocated in the parent Server class. Here we only handle it.
	 * @param _ser
	 * @throws SQLiteException 
	 */
	public UDPServer(int _port) throws SQLiteException{
		try {
			try_connect(_port);
			ds.setSoTimeout(Server.TIMEOUT_UDP_NAT_BORER);
			Identity.udp_server_port = ds.getLocalPort();
			if(DEBUG)System.out.println("UDP Local port obtained is: "+Identity.udp_server_port);
			Server.detectDomain(Identity.udp_server_port);

			Identity peer_ID = new Identity();
			peer_ID.globalID = Identity.current_peer_ID.globalID;
			peer_ID.name = Identity.current_peer_ID.name;
			peer_ID.slogan = Identity.current_peer_ID.slogan;
			Server.set_my_peer_ID_UDP(peer_ID, ds);
		
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public UDPServer(Identity id) throws SQLiteException{
		//boolean DEBUG = true;
		try {
			int _port = Server.PORT;
			if(DEBUG) System.out.println("UDPServer:<init>: start, try connect");
			try_connect(_port);
			if(DEBUG) System.out.println("UDPServer:<init>: start, connected");
			ds.setSoTimeout(Server.TIMEOUT_UDP_NAT_BORER);
			Identity.udp_server_port = ds.getLocalPort();
			if(DEBUG) System.out.println("UDPServer:<init>: Local port obtained is: "+Identity.udp_server_port);
			Server.detectDomain(Identity.udp_server_port);
			if(DEBUG) System.out.println("UDPServer:<init>: domain detected");
			Server.set_my_peer_ID_UDP (id, ds);
			if(DEBUG) System.out.println("UDPServer:<init>: peer ID set");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/*
	public void set_my_peer_ID (Identity id) throws SQLiteException {
		if(DEBUG) out.println("userver: setID");
		if(id==null) return;
		if(id.globalID==null) return;
		
		announceMyselfToDirectories();
		
		String addresses = Identity.current_server_addresses();
		if(addresses!=null) {
			String address[] = addresses.split(Pattern.quote(DirectoryServer.ADDR_SEP));
			for(int k=0; k<address.length; k++)
				Server.update_insert_peer_myself(id, address[k], "Socket");
		}
		for(String dir : Identity.listing_directories_string ) {
			if(DEBUG) out.println("userver: announce to: "+dir);
			String address_dir=dir;//.getHostName()+":"+dir.getPort();
			Server.update_insert_peer_myself(id, address_dir, "DIR");
		}
		if(DEBUG) out.println("userver: setID Done!");
	}
	*/
	public static DirectoryAnnouncement prepareDirectoryAnnouncement(){
		DirectoryAnnouncement da;
		synchronized(UDPServer.directoryAnnouncementLock) {
			if(UDPServer.directoryAnnouncement == null) {
				da = new DirectoryAnnouncement();
				da.globalID = Identity.current_peer_ID.globalID;
				//da.address.domain=Identity.domain.toString().split("/")[1];
				da.address.addresses=Identity.current_server_addresses();
				da.address.udp_port=Identity.udp_server_port;
				UDPServer.directoryAnnouncement = da;
			} else {
				da = UDPServer.directoryAnnouncement;
			}
		}	
		return da;
	}
	/**
	 * Send to each directory in the list Identity.listing_directories_inet!
	 * @param da A prepared Directory Announcement
	 */
	public static void announceMyselfToDirectories(DatagramSocket ds) {
		//boolean DEBUG_DIR = true;
		if(DEBUG_DIR) out.println("UDPServer: announceMyselfToDirectory: start");
		prepareDirectoryAnnouncement();
		if(DEBUG_DIR) out.println("UDPServer: announceMyselfToDirectory: prepeared");
		DirectoryAnnouncement da = UDPServer.directoryAnnouncement;
		if(DEBUG_DIR) out.println("UDPServer: announceMyselfToDirectory Registering: domain=\""+da.address.addresses+"\" UDP port=\""+da.address.udp_port+"\"");
		_announceMyselfToDirectories(UDPServer.directoryAnnouncement, ds);
		if(DEBUG_DIR) out.println("UDPServer: announceMyselfToDirectory: done");
	}	
	public static void announceMyselfToDirectoriesTCP(){
		if(DEBUG_DIR) out.println("Server: announceMyselfToDirectories");
		DirectoryAnnouncement da = prepareDirectoryAnnouncement();
		if(DEBUG_DIR) out.println("Server: Registering: "+da.address.addresses+":"+da.address.udp_port);
		Server.announceMyselfToDirectories(da);		
	}
	public void pingDirectories(){
		UDPEmptyPing uep = new UDPEmptyPing();
		byte[]msg = uep.encode();
		__announceMyselfToDirectories(msg, uep, ds);
	}
	public static void _announceMyselfToDirectories(DirectoryAnnouncement da, DatagramSocket ds) {
		byte msg[]=da.encode();
		__announceMyselfToDirectories(msg, da, ds);
	}
	/**
	 * Send to each directory in the list Identity.listing_directories_inet!
	 * @param da A prepared Directory Announcement
	 */
	public static void __announceMyselfToDirectories(byte[] msg, Object da, DatagramSocket ds) {
		//boolean DEBUG_DIR = true;
		if(DEBUG_DIR) out.println("UDPServer: __announceMyselfToDirectory: start");
		for(InetSocketAddress dir : Identity.listing_directories_inet ) {
			if(DEBUG_DIR) out.println("UDPServer:__announceMyselfToDirectories: announce to: "+dir);
			String address=null;
			try{
				address = dir.getAddress().getHostAddress()+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+dir.getPort();
				DatagramPacket dp = new DatagramPacket(msg, msg.length);
				dp.setSocketAddress(dir);
				ds.send(dp);
				if(DEBUG_DIR) out.println("UDPServer:__announceMyselfToDirectories: sent: "+da+"\n"+Util.byteToHexDump(msg," "));
				//Directories.setUDPOn(address, new Boolean(true));
			}catch(Exception e) {
				//Application.warning(_("Error announcing myself to directory:")+dir, _("Announcing Myself to Directory"));
				//e.printStackTrace();
				DD.directories_failed.add(dir);
				if(DEBUG_DIR) err.println("UDPServer: "+_("Error announcing myself to directory:")+dir);
				if(Application.ld!=null)
					Application.ld.setUDPOn(address, new Boolean(false));
			}
		}
		if(DEBUG_DIR) out.println("server: announceMyselfToDirectory Done!");
	}
	
	public void try_connect(int port) throws SocketException {
		try {
			ds = new DatagramSocket(port);
		} catch (SocketException e) {
			ds = new DatagramSocket();
			if(DEBUG)System.out.println("Got UDP port: "+ds.getLocalPort());
		}
		
	}
	boolean turnOff = false;
	private int _threads=0;
	int getThreads(){return _threads;}
	int incThreads(){synchronized(lock){return ++_threads;}}
	int decThreads(){synchronized(lock){return --_threads;}}
	public Object lock=new Object();
	public Object lock_reply=new Object();
	public void turnOff(){
		turnOff = true;
		try{ds.close();}catch(Exception e){}
		this.interrupt();
	}
	synchronized void wait_if_needed() throws InterruptedException{
		while(getThreads()>=MAX_THREADS){
			if(DEBUG) System.out.println("UDPServer:wait_if_needed: threads="+getThreads()+"> max="+MAX_THREADS);
			synchronized(lock){
				if (getThreads()>=MAX_THREADS)
					lock.wait(DD.UDP_SERVER_WAIT_MILLISECONDS);
			}
			if(getThreads()>=MAX_THREADS) {
				if(DEBUG)System.out.println("UDPServer:wait_if_needed: crtThreads="+getThreads()+">MAX_THREADS="+MAX_THREADS);
				// Util.printCallPath("Threads="+getThreads()+">MAX_THREADS="+MAX_THREADS);
			}
		}
	}
	public void send(DatagramPacket dp) throws IOException{
		ds.send(dp);
	}
	public void run() {
		try{
			_run();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	public void _run() {
		DD.ed.fireServerUpdate(new CommEvent(this, null, null, "LOCAL", "UDPServer starting"));
		//this.announceMyselfToDirectories();
		int cnt = 0;
		for(;;) {
			if (turnOff) break;
			if(DEBUG_ALIVE) out.println("userver: UDPServer reclaim!");
			try {
				this.sendFragmentReclaim();
				if(this.isInterrupted()) continue;
				if(DEBUG_ALIVE) out.println("userver: ************* wait!");
				wait_if_needed();
				if(this.isInterrupted()) continue;
				if(DEBUG_ALIVE) out.println("userver: UDPServer will accept!*************");
				buffer = new byte[UDP_BUFFER_LENGTH];
				DatagramPacket pak = new DatagramPacket(buffer, UDP_BUFFER_LENGTH);
				// calling the DatagramPacket receive call
				ds.setSoTimeout(Server.TIMEOUT_UDP_NAT_BORER); // might have changed
				ds.receive(pak);
				
				if(DEBUG) out.println("userver: ************ UDPServer accepted from "+pak.getSocketAddress()+", will launch!");
				if(this.isInterrupted()) continue;
				if(DEBUG) out.println("userver: ************* not interrupted, start!");
				new UDPServerThread(pak, this).start();
				if(DEBUG) out.println("userver: ************* UDPServer started!");
			}
			catch (SocketTimeoutException e){
				if(((++cnt) %Server.TIMEOUT_UDP_Announcement_Diviser) == 0) {
					if(DEBUG) out.println("userver: ************* UDPServer announce!");
					UDPServer.announceMyselfToDirectories(ds);
				}else{
					if(DEBUG_ALIVE) out.println("userver: ************* UDPServer ping!");
					this.pingDirectories();
				}
			}
			catch (SocketException e){
				if(DEBUG) out.println("server: ************* "+e);
			}
			catch(Exception e){
				e.printStackTrace();
			}
			if(DEBUG_ALIVE) out.println("userver: ************* UDPServer loopend!");
		}
		if(DEBUG) out.println("userver: ************* UDPServer Good Bye!");
		DD.ed.fireServerUpdate(new CommEvent(this, null, null, "LOCAL", "UDPServer stopping"));
		//System.exit(-1);
	}

	public static void main(String args[]){
		//boolean DEBUG = true;
		String source = args[0]; // Application.DEFAULT_DELIBERATION_FILE
		String target = args[1]; 
		boolean direct = true;
		if(args.length>2){
			direct = Util.stringInt2bool(args[2], true); 
			out.println("direct = "+args[2]+"="+direct);
		}
		try {
			System.out.println("UDPServThread: main: start");
			Application.db = new DBInterface(target);
			Identity.init_Identity();
			System.out.println("UDPServThread: main: inited IDs");
			D_PeerAddress.get_myself(Identity.current_peer_ID.globalID);
			System.out.println("UDPServThread: main: got myself");
			//String last_sync_date = Encoder.getGeneralizedTime(Util.getCalendar("00000000000000.000Z"));
			//String[] _maxDate  = new String[]{Util.getGeneralizedTime()};
			//boolean justDate=false;
			//<String> orgs = new HashSet<String>();
			//int limitPeersLow = 100;
			//int limitPeersMax = 1000;
			//Table a=null;
			SyncAnswer sa=null;
			OrgHandling.SERVE_DIRECTLY_DATA = direct;

			ASNSyncRequest asr = new ASNSyncRequest();
			asr.lastSnapshot = null;
			asr.lastSnapshot = Util.getCalendar("00000000000000.000Z");
			String peerID="1";
			asr.tableNames=new String[]{table.peer.G_TNAME};
			asr.address = D_PeerAddress.get_myself(null);
			
			
			//if(filtered) asr.orgFilter=UpdateMessages.getOrgFilter(peerID);
			System.out.println("UDPServThread: main: will sign");
			asr.sign();
			
			if(DEBUG){
				boolean r = asr.verifySignature();
				if(!r) Util.printCallPath("failed verifying: "+asr);
				else
					System.out.println("UDPServThread: _run: signature success");
			}
			
			byte[] _buf = asr.encode();
			if(DEBUG) {
				Decoder dec = new Decoder(_buf);
				ASNSyncRequest _asr = new ASNSyncRequest();
				_asr.decode(dec);
				//if(DEBUG)System.out.println("UDPServer: Received request from: "+psa);
				if(_DEBUG)System.out.println("UDPServer: verif sent Decoded request: "+_asr.toSummaryString());
				if(!_asr.verifySignature()) {
					//DD.ed.fireServerUpdate(new CommEvent(this, null, psa, "UDPServer", "Unsigned Sync Request received: "+asr));
					System.err.println("UDPServer:run: Unsigned Request sent: "+_asr.toSummaryString());
					System.err.println("UDPServer:run: Unsigned Request rsent: "+_asr.toString());
					return;
				}					
			}

			

			if(DEBUG)System.out.println("\n\n*****************\n*******************\n\nUDPServer:run: Request recv: "+asr.toSummaryString());
			
			for(int cnt=0; cnt<10; cnt++) {
				System.out.println("\n\n**************   Round "+cnt+"\n\n");
				
				Application.db = new DBInterface(source);
				Identity.current_peer_ID = null;
				Identity.current_peer_ID = Identity.initMyCurrentPeerIdentity();
				SK sk = DD.getMyPeerSK();

				sa = UpdateMessages.buildAnswer(asr , peerID);

				byte[]sa_msg = sa.encode();
				
				//System.out.println("Got="+sa.toSummaryString());

				//us.sendLargeMessage(psa, sa_msg, DD.MTU, peerGID, DD.MSGTYPE_SyncAnswer);
				//if(DEBUG)System.out.println("\n\n***************************\nUDPServer:run: Answer sent! "+sa_msg.length+"\n"+sa.toSummaryString());

				Application.db = new DBInterface(target);
				Identity.current_peer_ID = null;
				Identity.current_peer_ID = Identity.initMyCurrentPeerIdentity();

				sa = null;
				SyncAnswer ra = null;
				ra = new SyncAnswer().decode(new Decoder(sa_msg));
				//if(DEBUG)System.out.println("\n\n************************\nUDPServer:run: Answer received! "+ra.toSummaryString());

				String global_peer_ID = ra.responderID;
				D_PeerAddress peer =null;
				String peer_ID = null;
				if(global_peer_ID!=null) {
					peer = new D_PeerAddress(global_peer_ID, false, false, true);
					peer_ID = peer.peer_ID; //table.peer.getLocalPeerID(global_peer_ID);
				}
				if(peer_ID == null) {
					if(DEBUG)System.out.println("UDPServer:run: Answer received from unknown peer: "+global_peer_ID);

				} else
					if(Application.peers!=null) Application.peers.setConnectionState(peer_ID, Peers.STATE_CONNECTION_UDP);

				//System.out.println("Got msg size: "+len);//+"  bytes: "+Util.byteToHex(update, 0, len, " "));
				if(DEBUG)System.out.println("UDPServer:run: Answer received will be integrated");
				RequestData rq= new RequestData();


				if(DEBUG)System.out.println("\n\n*****************\n*******************\n\nUDPServer:run: Answer received: final"+ra.toSummaryString());

				UpdateMessages.integrateUpdate(ra, new InetSocketAddress(10000), new Object(), global_peer_ID, peer_ID, null,rq, peer);

				if(DEBUG)System.out.println("\n\n*****************\n*******************\n\nUDPServer:run: Done");

				//if(true) return;
				sk = DD.getMyPeerSK();
				boolean filtered = false;
				String _lastSnapshotString = null;
				ASNSyncRequest asreq = Client.buildRequest(_lastSnapshotString, ra.upToDate, peer_ID);
				if(filtered) asreq.orgFilter=UpdateMessages.getOrgFilter(peer_ID);
				asreq.sign(sk);
				byte[] buf = asreq.encode();
				//if(DEBUG)System.out.println("\n\n*****************\n*******************\n\nUDPServer:run: Request sent: "+asreq.toSummaryString());
				//ASNSyncRequest asr2 = new ASNSyncRequest();
				asr = new ASNSyncRequest();
				asr.decode(new Decoder(buf));
				if(DEBUG)System.out.println("\n\n*****************\n*******************\n\nUDPServer:run: Request recv: "+asr.toSummaryString());
			}
		} catch (SQLiteException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
	}
}
