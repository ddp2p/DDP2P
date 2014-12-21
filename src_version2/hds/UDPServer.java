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
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;

import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ciphersuits.Cipher;
import ciphersuits.SK;
import util.P2PDDSQLException;
import config.Application;
import config.Application_GUI;
import config.DD;
import config.Identity;
import data.D_Peer;
import data.HandlingMyself_Peer;
import streaming.OrgHandling;
import streaming.RequestData;
import streaming.UpdateMessages;
import util.CommEvent;
import util.DBInterface;
import util.Util;
import static java.lang.System.err;
import static java.lang.System.out;
import static util.Util.__;

public class UDPServer extends util.DDP2P_ServiceThread {
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static final int MAX_THREADS = 6;
	public static final int UDP_BUFFER_LENGTH = 1000000;
	public static final Object directoryAnnouncementLock = new Object();
	private static final boolean ANNOUNCE_TCP = false;
	public static boolean DEBUG_DIR = false;
	public static DirectoryAnnouncement directoryAnnouncement = null;
	private byte[] buffer;
	private static DatagramSocket ds;
	Hashtable<String, UDPMessage> sentMessages = new Hashtable<String, UDPMessage>();
	Hashtable<String, UDPMessage> recvMessages = new Hashtable<String, UDPMessage>();
	// Set of peers to which a sync was sent
	// public HashSet<String> synced = new HashSet<String>(); 
	public Hashtable<String, HashSet<String>> synced = new Hashtable<String, HashSet<String>>(); 
	Random rnd = new Random();
	
	public static boolean transferringPeerAnswerMessage(String global_peer_ID, String instance) {
		if (global_peer_ID == null) return false;
		for (UDPMessage um : Application.aus.recvMessages.values()) {
			if (global_peer_ID.equals(um.sender_GID) && Util.equalStrings_null_or_not(instance, um.sender_instance) &&
					(um.type == DD.MSGTYPE_SyncAnswer)) {
				if(DEBUG)System.out.println("UDPServer: transfAnswer:now="+Util.CalendargetInstance().getTimeInMillis()+" checks="+um.checked +"/"+ DD.UDP_SENDING_CONFLICTS);
				if(DEBUG)System.out.println("UDPServer: transfAnswer:"+um);
				um.checked++;
				if(um.checked > DD.UDP_SENDING_CONFLICTS) // potentially lost
					return false; //let it go further!
				Application.aus.sendReclaim(um);
				return true;
			}
		}
		return false;
	}
	public static boolean transferringPeerRequestMessage(String global_peer_ID, String instance) {
		if (global_peer_ID == null) return false;
		for (UDPMessage um : Application.aus.sentMessages.values()) {
			if (global_peer_ID.equals(um.destination_GID) && Util.equalStrings_null_or_not(instance, um.sender_instance) &&
					(um.type == DD.MSGTYPE_SyncRequest) ) {
				um.checked ++;
				if (um.checked > DD.UDP_SENDING_CONFLICTS) // potentially lost
					return false; //let it go further!
				if (um.no_ack_received()) {
					try {
						Application.aus.reclaim(um);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return true;
			}
		}
		return false;
	}
	public static boolean transferringPeerMessage(String global_peer_ID, String instance) {
		if (Application.aus == null) return false;
		if (transferringPeerAnswerMessage(global_peer_ID, instance)) return true;
		if (transferringPeerRequestMessage(global_peer_ID, instance)) return true;
		return false;
	}
	public void resetSyncRequests(){
		synchronized(synced){
			synced.clear();
		}
	}
	public void addSyncRequests(String gID, String instance) {
		if (DEBUG) System.out.print("UDServer:addSyncRequests:Adding "+Util.trimmed(gID)+" in synced: ");
		String inst = Util.getStringNonNullUnique(instance);
		synchronized (synced) {
			//if (!synced.contains(gID)) synced.add(gID);
			HashSet<String> elem = synced.get(gID);
			if (elem == null) synced.put(gID, elem = new HashSet<String>());			
			if (!elem.contains(inst)) elem.add(inst);
			else if (DEBUG) System.out.print(" (old) ");
			
			if (DEBUG)  for (String s : synced.keySet()) System.out.print(Util.trimmed(s)+",");
		}
		if (DEBUG) System.out.println("");
	}
	public void delSyncRequests (String gID, String instance) {
		if (DEBUG) System.out.print("UDServer:delSyncRequests:Deleting "+Util.trimmed(gID)+" in synced: ");
		String inst = Util.getStringNonNullUnique(instance);
		synchronized (synced) {
			//synced.remove(gID);
			HashSet<String> elem = synced.get(gID);
			if (elem == null) {} // {synced.put(gID, elem = new HashSet<String>());}
			else {elem.remove(inst); if (elem.size() == 0) synced.remove(gID); }
			
			if (DEBUG)  for (String s : synced.keySet()) System.out.print(Util.trimmed(s)+",");
			
		}
		if (DEBUG) System.out.println("");
	}
	public boolean hasSyncRequests (String gID, String instance) {
		boolean res = false;
		if (DEBUG) System.out.print("UDPServer:hasSyncRequests:Look "+Util.trimmed(gID)+" in synced: ");
		String inst = Util.getStringNonNullUnique(instance);
		synchronized (synced) {
			HashSet<String> elem = synced.get(gID);
			if (elem == null) {res = false;}
			else {res = elem.contains(inst);}
			/*
			if (! DEBUG) return synced.contains(gID);
			for (String s : synced) {
				if (DEBUG) System.out.print(Util.trimmed(s)+",");
				if (gID.equals(s)) {
					res = true;
					break;
				}
			}
			*/
			if (DEBUG)  for (String s : synced.keySet()) System.out.print(Util.trimmed(s)+",");
		}
		if (DEBUG) System.out.println(" result="+res);
		return res; // synced.contains(gID);
	}
	/**
	 * If message length larger than DD.MTU, then break it into fragments
	 * else send in a single fragment
	 * 
	 * Synchronizes on "UDPServer.sentMessages"
	 * @param sa
	 * @param msg
	 * @param MTU
	 * @param destGID
	 * @param type
	 * @throws IOException
	 */
	public void sendLargeMessage(SocketAddress sa, byte[] msg, int MTU, String destGID, int type) throws IOException {
		if(MTU >= msg.length) {
			DatagramPacket dp= new DatagramPacket(msg, msg.length);
			dp.setSocketAddress(sa);
			getUDPSocket().send(dp);
			if(DEBUG) System.out.println("Sent message in one fragment to "+sa);
			return;
		}
		int frags = (int) Math.ceil(msg.length/(MTU*1.0));
		
		// The umsg has to be stored in sentMessages before sending fragments
		// to avoid generating a Nack on an early ack
		UDPMessage umsg = new UDPMessage(frags);
		umsg.msgID = rnd.nextInt()+"";
		umsg.type = type;
		umsg.sa = sa;
		synchronized (sentMessages) {
			int trials=0;
			while(sentMessages.get(umsg.msgID) != null){
				if(DEBUG) System.out.println("UDPServer: sendLargeMessage: repeating msgID! "+umsg.msgID);
				umsg.msgID = rnd.nextInt()+"";
				if(trials++ < 100) continue;
			}
			sentMessages.put(umsg.msgID,umsg);
		}
		
		if(DEBUG)System.out.println("Sending to: "+sa+" msgID="+umsg.msgID+" msg["+msg.length+"]="+Util.byteToHexDump(msg));
		for(int k=0; k < frags; k++) {
			UDPFragment uf = new UDPFragment();
			uf.senderID = Identity.current_peer_ID.getPeerGID();
			uf.destinationID = destGID;
			uf.msgType = type;
			uf.msgID = umsg.msgID;
			uf.sequence = k;
			uf.fragments = frags;
			uf.signature = new byte[0];
			if(DD.PRODUCE_FRAGMENT_SIGNATURE){
				uf.signature = Util.sign_peer(uf.getEncoder().getBytes());
			}
			int len_this=Math.min(msg.length-MTU*k, MTU);
			uf.data = new byte[len_this];
			Util.copyBytes(uf.data, 0, msg, uf.data.length, MTU*k);
			umsg.fragment[k] = uf;
		}
		int messages_to_send_in_parallel = Math.min(DD.FRAGMENTS_WINDOW, frags);
		for(int k=0; k < messages_to_send_in_parallel; k++) {
			UDPFragment uf = umsg.fragment[k];
			synchronized(sentMessages) { // these may change during transmission/reception
				if(umsg.sent_attempted[k]>0){
					System.out.println("UDPServer:sendLargeMessage: fragment already sent:"+k);
					messages_to_send_in_parallel = Math.min(messages_to_send_in_parallel+1, frags);
					continue;
				}
				umsg.sent_attempted[k]++;
				umsg.unacknowledged++;
			}
			byte[]frag = uf.encode();
			DatagramPacket dp= new DatagramPacket(frag, frag.length);
			dp.setSocketAddress(sa);
			getUDPSocket().send(dp);
			if(DEBUG)System.out.println("Sent UDPFragment: "+uf+" to "+sa);
			if(DEBUG)System.out.println("Sent UDPFragment: "+uf.sequence+"/"+uf.fragments+" to "+sa+" of "+uf.msgID);
		}
	}
	/**
	 * Synchronizes on sentMessages
	 * @param sa 
	 * @param frag
	 * @throws IOException 
	 */
	public void getFragmentAck(UDPFragmentAck ack, SocketAddress sa) throws IOException{
		if(DEBUG) System.out.println("getAck");
		if(DD.VERIFY_FRAGMENT_ACK_SIGNATURE) {
			/**
			 * Just check fast to not verify the signature for no benefit
			 */
			synchronized(sentMessages) {
				UDPMessage umsg = sentMessages.get(ack.msgID+"");
				if(umsg == null) return;
			}
			
			byte[] signature = ack.signature;
			String senderID = ack.senderID;
				
			// prepare for verification
			ack.signature = new byte[0];
			ack.senderID = "";
			if(!Util.verifySign(ack, Cipher.getPK(senderID), signature)){
				System.err.println("Failure verifying FragAck: "+ack+
						"\n ID="+Util.trimmed(senderID)+ 
						"\n sign="+Util.byteToHexDump(signature));
				return;
			}
			ack.signature = signature;
			ack.senderID = senderID;
		}
		
		int bagged=0;
		boolean bag[] = new boolean[ack.transmitted.length];
		UDPMessage umsg = null;
		synchronized(sentMessages) {
			umsg = sentMessages.get(ack.msgID+"");
			if((umsg == null)||(umsg.fragment.length!=ack.transmitted.length)) return;
	
			for(int i=0; i<ack.transmitted.length; i++) {
				if(ack.transmitted[i]==1)	
					if(umsg.transmitted[i] == 0) {
						umsg.received++;
						umsg.transmitted[i] = 1;
					}
				umsg.unacknowledged --;
				umsg.unacknowledged=Math.max(0, umsg.unacknowledged);
			}
			/**
			 * If not yet done!
			 */
			if((umsg.received < umsg.fragment.length)&&
					//(umsg.received > umsg.sent_once-DD.FRAGMENTS_WINDOW_LOW_WATER)
					(umsg.unacknowledged < DD.FRAGMENTS_WINDOW_LOW_WATER)
					){
				/**
				 * could try a loop, up to the largest value in sent_attempted
				 * First try to select (to re-send) fragments never sent before
				 * Then attempt to select some that were already sent (up to window)
				 * When all were sent once, the approach will keep sending the first ones
				 */
				bagged = this.prepareBag(bag, umsg, umsg.unacknowledged);
			}
			
			if(umsg.received >= umsg.fragment.length) {
				sentMessages.remove(umsg.msgID);
				if(DEBUG)System.out.println("Message discarded "+umsg.msgID);
				return;
			}
		}
		sendBag(bag, umsg, bagged);
	}
	/**
	 * Synchronizes on recvMessages
	 * Removes the message from the queue of received ones
	 * @param frag
	 */
	public void getFragmentNAck(UDPFragmentNAck frag){
		if(DEBUG) System.out.println("get Nack: "+frag.msgID);
		if(DD.VERIFY_FRAGMENT_NACK_SIGNATURE) {
			byte[] signature = frag.signature;
			String senderID = frag.senderID;
			
			// prepare for verification
			frag.signature = new byte[0];
			frag.senderID = "";
			if(!Util.verifySign(frag, Cipher.getPK(senderID), signature)){
				System.err.println("Failure verifying FragNack: "+frag+
					"\n ID="+Util.trimmed(senderID)+ 
					"\n sign="+Util.byteToHexDump(signature));
				return;
			}
			frag.signature = signature;
			frag.senderID = senderID;
		}
		
		synchronized(recvMessages) {
			recvMessages.remove(frag.msgID+"");
		}
		if(DEBUG) System.out.println("get Nack: removing message"+frag.msgID);
	}
	/**
	 * Synchronizes on sentMessages
	 * @param frag
	 * @param sa
	 * @throws IOException
	 */
	public void getFragmentReclaim(UDPFragmentAck recl, SocketAddress sa) throws IOException{
		if(DEBUG) System.out.println("get Reclaim");
		if(DD.VERIFY_FRAGMENT_RECLAIM_SIGNATURE){
			byte[] signature = recl.signature;
			String senderID = recl.senderID;
			
			// prepare for verification
			recl.signature = new byte[0];
			recl.senderID = "";
			if(!Util.verifySign(recl, Cipher.getPK(senderID), signature)){
				System.err.println("Failure verifying Reclaim: "+recl+
					"\n ID="+Util.trimmed(senderID)+ 
					"\n sign="+Util.byteToHexDump(signature));
				return;
			}
		}
		boolean bag[] = new boolean[recl.transmitted.length];
		int bagged = 0;
		UDPMessage umsg = null;
		synchronized(sentMessages) {
			umsg = sentMessages.get(recl.msgID+"");
			if(umsg != null){
				if(DEBUG) System.out.println("Answering Reclaim: "+recl+"   for umsg="+umsg);
				for(int i=0; i<recl.transmitted.length; i++) {
					if(recl.transmitted[i]==1)	{
						if(umsg.transmitted[i] == 0) {
							umsg.received++;
							umsg.transmitted[i] = 1;
						}
					}
				}
//				// Surely not yet fully sent!
				/**
				 * Forget acknowledgments! They will no longer be waited for
				 */
				umsg.unacknowledged = 0;
				bagged = this.prepareBag(bag, umsg, umsg.unacknowledged);
			}
		}
		sendBag(bag, umsg, bagged);
		if(umsg == null) {
			if(DEBUG) System.out.println("Reclaim for discarded message: "+recl.msgID);
			UDPFragmentNAck uf = new UDPFragmentNAck();
			uf.msgID = recl.msgID;
			uf.destinationID = recl.senderID;
			uf.transmitted = recl.transmitted;
			
			if(DD.PRODUCE_FRAGMENT_NACK_SIGNATURE) {
				// prepare for signature
				uf.signature = new byte[0];
				uf.senderID="";
				uf.signature = Util.sign_peer(uf);//frag.destinationID);
				uf.senderID = recl.destinationID;
			}
			if(DEBUG)System.out.println("Sending NAck: "+uf);
			byte[] ack = uf.encode();
			DatagramPacket dp = new DatagramPacket(ack, ack.length,sa);
			this.getUDPSocket().send(dp);
			if(DEBUG)System.out.println("Sending NAck: "+uf.msgID);
			return;
		}
	}
	public int prepareBag(boolean[]bag, UDPMessage umsg, int unacks){
		int bagged=0;
		int largest = 0;
		for(int i=0; i<bag.length; i++)
			largest = Math.max(largest, umsg.sent_attempted[i]);
		for(int j=0; j<=largest; j++) {
			for(int i=0; i<bag.length; i++) {
				if((umsg.sent_attempted[i] == j)&&(umsg.transmitted[i] == 0)) {
					if(bag[i]) continue; // redundant
					bag[i] = true;
					bagged++;
					if(bagged+unacks >= DD.FRAGMENTS_WINDOW) break;
				}
			}
			if(bagged+unacks >= DD.FRAGMENTS_WINDOW) break;
		}
		return bagged;
	}
	/**
	 * Called to restart a stalled sending that is unknown to the recipient.
	 * @param umsg
	 * @throws IOException 
	 */
	private void reclaim(UDPMessage umsg) throws IOException {
		if(umsg == null) return;
		boolean bag[] = new boolean[umsg.transmitted.length];
		int bagged = 0;
		synchronized(sentMessages) {
			if(DEBUG) System.out.println("UDPServer: Reclaim for umsg="+umsg);
			/**
			 * Forget acknowledgments! They will no longer be waited for
			 */
			umsg.unacknowledged = 0;
			bagged = this.prepareBag(bag, umsg, umsg.unacknowledged);
		}
		sendBag(bag, umsg, bagged);
	}
	private void sendBag(boolean bag[], UDPMessage umsg, int bagged) throws IOException{
		if(bagged!=0){
			for(int k=0; k<bag.length; k++) {
				if(!bag[k]) continue;
					
				UDPFragment uf = umsg.fragment[k];
				if(umsg.transmitted[k] == 1){
					System.out.println("UDPServer:getFragmentAck: fragment already acked:"+k);
					continue;
				}
				synchronized(sentMessages) { // these may change during transmission/reception
					umsg.sent_attempted[k]++;
					umsg.unacknowledged++;
				}
				byte[]fragb = uf.encode();
				DatagramPacket dp = new DatagramPacket(fragb, fragb.length);
				dp.setSocketAddress(umsg.sa);
				getUDPSocket().send(dp);
				if(DEBUG)System.out.println("Sent UDPFragment: "+uf+" to "+umsg.sa);
				if(DEBUG)System.out.println("ReSent UDPFragment: "+uf.sequence+"/"+uf.fragments+" ID="+umsg.msgID+" to "+umsg.sa);
			}
			return;
		}
	}
	private void sendReclaim(UDPMessage umsg) {
		long crt_time = Util.CalendargetInstance().getTimeInMillis();
		sendReclaim (umsg, crt_time);
	}
	private void sendReclaim(UDPMessage umsg, long crt_time) {
		umsg.date = crt_time;
		if(DEBUG) out.println("userver: UDPServer Reclaim at "+crt_time+"ms: "+umsg);
		UDPReclaim uf = new UDPReclaim();
		uf.msgID = umsg.msgID;
		uf.destinationID = umsg.sender_GID;
		uf.transmitted = umsg.transmitted;
						
		if(DD.PRODUCE_FRAGMENT_RECLAIM_SIGNATURE) {
			// prepare for signature
			uf.signature = new byte[0];
			uf.senderID="";
			uf.signature = Util.sign_peer(uf);//umsg.destinationID);
			uf.senderID = umsg.destination_GID;
		}
		if(DEBUG)System.out.println("Sending reclaim: "+uf);
		byte[] ack = uf.encode();
		DatagramPacket dp;
		try {
			dp = new DatagramPacket(ack, ack.length, umsg.sa);
			if(DEBUG) out.println("userver: UDPServer Reclaim sent to "+umsg.sa+" recl=: "+ uf);
			this.getUDPSocket().send(dp);
			if(DEBUG) out.println("userver: UDPServer Reclaim sent to "+umsg.sa+" recl=: "+ umsg.received+"/"+umsg.fragment.length+" of "+umsg.msgID);
		} catch (SocketException e1) {
			e1.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
	/**
	 * Synchronized on recvMessages
	 */
	public void sendFragmentReclaim(){
		if(DD.DEBUG_COMMUNICATION_LOWLEVEL) out.println("userver: UDPServer Reclaim! messages #"+recvMessages.size());
		ArrayList<UDPMessage> bag = new ArrayList<UDPMessage>();
		long crt_time = Util.CalendargetInstance().getTimeInMillis();
		/**
		 * First extract all messages to reclaim (to not keep lock for long)
		 */
		synchronized(this.recvMessages) {
			for(UDPMessage umsg: recvMessages.values()) {
				if(DEBUG) out.println("userver: UDPServer Reclaim "+umsg.msgID+" check: "+crt_time+"-"+umsg.date+"="+(crt_time-umsg.date)+">"+Server.TIMEOUT_UDP_Reclaim);
				if(crt_time-umsg.date>Server.TIMEOUT_UDP_Reclaim) {
					bag.add(umsg);
				}
			}
		}
		/**
		 * Then reclaim them. Synchronization is probably not needed
		 */
		for(UDPMessage umsg: bag){
			sendReclaim(umsg, crt_time);
		}
	}

	/**
	 * Temporary locks UDPServer.recvMessages and
	 *  umsg.lock_ack where umsg is the structure of the large message being received
	 * @param frag
	 * @param sa
	 * @return
	 * @throws IOException
	 */
	public byte[] getFragment(UDPFragment frag, SocketAddress sa) throws IOException{
		byte[] result = null;
		
		if(DD.VERIFY_FRAGMENT_SIGNATURE) {
			byte[] signature = frag.signature;
			String senderID = frag.senderID;
			//System.out.println("F");
			// prepare for verification
			frag.signature = new byte[0];
			frag.senderID = "";
			if((signature == null)||(signature.length==0)){
				System.err.println("Fragment came unsigned: rejected\n"+frag);
				return null;
			}
			if(!Util.verifySign(frag, Cipher.getPK(senderID), signature)){
				if(DD.isThisAnApprovedPeer(senderID))
				System.err.println("Failure verifying frag: "+frag+
					"\n sender ID="+Util.trimmed(senderID)+ 
					"\n sign="+Util.byteToHexDump(signature));
				return null;
			}
			frag.signature = signature;
			frag.senderID = senderID;
		}
		
		String msgID = frag.msgID+"";
		
		
		if (DEBUG) {
			System.out.println("Running messages: "+recvMessages.size());
			Enumeration<String> e = recvMessages.keys();		   
			//iterate through Hashtable keys Enumeration
			while (e.hasMoreElements()) {
				String key = e.nextElement();
				if (DEBUG) System.out.println("Element "+key+" comparison:"+msgID.equals(key)+" vs="+msgID);
			}
		}
	
	    UDPMessage umsg = null;
	    /**
	     * Here we extract the message from recv (or create a new one there)
	     */
	    synchronized (recvMessages) {
	    	umsg = recvMessages.get(msgID);
	    	if(umsg == null) {
	    		/**
	    		 * For new messages
	    		 */
	    		umsg = new UDPMessage(frag.fragments);
	    		umsg.uf.msgID = frag.msgID;
	    		umsg.uf.destinationID = frag.senderID;
	    		umsg.uf.transmitted = umsg.transmitted;
	    		umsg.type = frag.msgType;
	    		
	    		umsg.sa = sa;
	    		umsg.destination_GID = frag.destinationID;
	    		umsg.sender_GID = frag.senderID;
	    		recvMessages.put(msgID, umsg);
	    		umsg.msgID = frag.msgID;
	    		if(DEBUG)System.out.println("Starting new message: "+umsg);
	    	}else
	    		if(DEBUG)System.out.println("Located message: "+umsg);
	    	/**
	    	 * Update date of last contact
	    	 */
	    	umsg.date = Util.CalendargetInstance().getTimeInMillis();

	    	/**
	    	 * add new fragment
	    	 */
	    	// if the fragment longer then length of previous fragment: strange => drop
	    	if(frag.sequence>=umsg.fragment.length){
	    		if(DEBUG)System.err.println("Failure sequence: "+frag.sequence+" vs. "+umsg.fragment.length);
	    		return null;
	    	}
	    	/**
	    	 * Set the flag for having received this
	    	 */
	    	if(umsg.transmitted[frag.sequence]==0) {
	    		umsg.transmitted[frag.sequence] = 1;
	    		umsg.received++;
	    		umsg.ack_changed = true;
	    	
	    		umsg.fragment[frag.sequence] = frag;
	    	}
		
			if(umsg.received>=umsg.fragment.length){
				recvMessages.remove(umsg.fragment[0].msgID);
			}
	    }
	    
		if(umsg.received>=umsg.fragment.length){
			byte[] msg = umsg.assemble();
			if(DEBUG)System.out.println("Removing received message: "+umsg.msgID);
			if(DEBUG)System.out.println("getFragments: Got message completely: "+umsg.msgID+" msg="+Util.byteToHexDump(msg));
			result = msg;
		}else{
			if(DEBUG)System.out.println("getFragments. Got: "+umsg.received+"/"+umsg.fragment.length+" received msg="+umsg);
			result = null;//umsg.assemble();
		}
	    
	    // could arrange for a separate thread to send the ack outside the critical section
	    
	    synchronized(umsg.lock_ack){
	    	// prepare for signature
	    	if(umsg.ack_changed) {
	    		if(DD.PRODUCE_FRAGMENT_ACK_SIGNATURE) {
	    			umsg.uf.signature = new byte[0];
	    			umsg.uf.senderID="";
	    			umsg.uf.senderID = frag.destinationID;
	    			umsg.uf.signature = Util.sign_peer(umsg.uf);//frag.destinationID);
	    		}
	    		if(DEBUG)System.out.println("getFragments: Preparing ack: "+umsg.uf+" umsgID="+umsg.msgID+" to: "+sa);
	    		umsg.ack = umsg.uf.encode();
	    		umsg.ack_changed = false;
	    	}
	    }
	    DatagramPacket dp = new DatagramPacket(umsg.ack, umsg.ack.length,sa);
	    this.getUDPSocket().send(dp);
	    if(DEBUG)System.out.println("getFragments: Sent ack: "+frag.sequence+"/"+umsg.received+" umsgID="+umsg.msgID+" to: "+sa);
	    return result;
	}
	/**
	 * The socket is allocated in the parent Server class. Here we only handle it.
	 * @param _ser
	 * @throws P2PDDSQLException 
	 */
	public UDPServer(int _port) throws P2PDDSQLException {
		super("UDP Server", false);
		//boolean DEBUG = true;
		try {
			if(DEBUG) System.out.println("UDPServer:<init>: start, connected port:"+_port);
			try_connect(_port);
			getUDPSocket().setSoTimeout(Server.TIMEOUT_UDP_NAT_BORER);
			Identity.udp_server_port = getUDPSocket().getLocalPort();
			Application.aus = this;
			if(DEBUG)System.out.println("UDP Local port obtained is: "+Identity.udp_server_port);
			//Server.detectDomain(Identity.udp_server_port);

//			Identity peer_ID = new Identity();
//			peer_ID.globalID = Identity.current_peer_ID.globalID;
//			peer_ID.instance = Identity.current_peer_ID.instance;
//			peer_ID.name = Identity.current_peer_ID.name;
//			peer_ID.slogan = Identity.current_peer_ID.slogan;
//			MyselfHandling.set_my_peer_ID_UDP(peer_ID, ds);
		
			//UDPServer.announceMyselfToDirectoriesReset();
			//if(DEBUG) System.out.println("UDPServer:<init>: peer ID set port:"+Identity.udp_server_port);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Sets Identity.udp_server_port with the final port
	 * @param id
	 * @throws P2PDDSQLException
	 */
	public UDPServer(Identity id) throws P2PDDSQLException{
		super("UDP Server", false);
		//boolean DEBUG = true;
		try {
			int _port = Server.PORT;
			if (DEBUG) System.out.println("UDPServer:<init>: start, try connect");
			try_connect(_port);
			if (DEBUG) System.out.println("UDPServer:<init>: start, connected");
			getUDPSocket().setSoTimeout(Server.TIMEOUT_UDP_NAT_BORER);
			Identity.udp_server_port = getUDPSocket().getLocalPort();
			Application.aus = this;
			if (DEBUG) System.out.println("UDPServer:<init>: Local port obtained is: "+Identity.udp_server_port);
			//Server.detectDomain(Identity.udp_server_port);
			if (DEBUG) System.out.println("UDPServer:<init>: domain detected");
			////MyselfHandling.set_my_peer_ID_UDP (id, ds);
			//UDPServer.announceMyselfToDirectoriesReset();
			//if(DEBUG) System.out.println("UDPServer:<init>: peer ID set");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/*
	public void set_my_peer_ID (Identity id) throws P2PDDSQLException {
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
	public static DirectoryAnnouncement prepareDirectoryAnnouncement() {
		DirectoryAnnouncement da;
		synchronized (UDPServer.directoryAnnouncementLock) {
			if ((UDPServer.directoryAnnouncement == null) 
					|| (UDPServer.directoryAnnouncement.address.udp_port != Identity.udp_server_port)) {
				da = new DirectoryAnnouncement();
				da.branch = DD.BRANCH;
				da.agent_version = DD.getMyVersion();
				da.name = data.HandlingMyself_Peer.getMyPeerName();
				da.setGID(HandlingMyself_Peer.getMyPeerGID());//Identity.current_peer_ID.globalID;
				da.instance = HandlingMyself_Peer.getMyPeerInstance();//Identity.current_peer_ID.instance;
				//da.address.domain=Identity.domain.toString().split("/")[1];
				da.address.setAddresses(Identity.current_server_addresses_list());
				da.address.udp_port=Identity.udp_server_port;
				//if (da.address.udp_port <= 0)	Util.printCallPath("UDPServer: "+da);
				UDPServer.directoryAnnouncement = da;
				if (DEBUG) out.println("UDPServer:prepDirAnn: da="+da);
			} else {
				da = UDPServer.directoryAnnouncement;
				if (DEBUG) out.println("UDPServer:prepDirAnn: old da="+da);
			}
		}	
		return da;
	}
	/**
	 * This reconstructs the announcements (socket addresses, and udp port)
	 */
	public static void announceMyselfToDirectoriesReset() {
		try {
			Server.detectDomain();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		UDPServer.directoryAnnouncement = null;
		announceMyselfToDirectories(getUDPSocket());
	}
	/**
	 * This uses a precomputed announcement if exists and has the same udp_port.
	 * else use: announceMyselfToDirectoriesReset() to redetect addresses
	 */
	public static void announceMyselfToDirectories() {
		announceMyselfToDirectories(getUDPSocket());
	}
	/**
	 * Send to each directory in the list Identity.listing_directories_inet!
	 * @param da A prepared Directory Announcement
	 */
	private static void announceMyselfToDirectories(DatagramSocket ds) {
		//boolean DEBUG_DIR = true;
		if(DEBUG_DIR) out.println("UDPServer: announceMyselfToDirectory: start");
		DirectoryAnnouncement da =  prepareDirectoryAnnouncement();
		if (DEBUG_DIR) out.println("UDPServer: announceMyselfToDirectory: prepared: "+UDPServer.directoryAnnouncement);
		if (DEBUG_DIR) out.println("UDPServer: announceMyselfToDirectory Registering: domain=\""+da.address.addresses()+"\" UDP port=\""+da.address.udp_port+"\"");
		
		if (DD.DIRECTORY_ANNOUNCEMENT_UDP && UDPServer.isRunning() && (ds != null))
			_announceMyselfToDirectories(da, ds);
		if (DD.DIRECTORY_ANNOUNCEMENT_TCP)
			Server.announceMyselfToDirectories(da);
		
		if(DEBUG_DIR) out.println("UDPServer: announceMyselfToDirectory: done");
	}
	/**
	 * Prepares an announcement from the static Identity parameters.
	 * 
	 */
	/*
	public static void announceMyselfToDirectoriesTCP() {
		if (DEBUG_DIR) out.println("Server: announceMyselfToDirectories");
		DirectoryAnnouncement da = prepareDirectoryAnnouncement();
		if (DEBUG_DIR) out.println("Server: Registering: "+da.address.addresses()+":"+da.address.udp_port);
		Server.announceMyselfToDirectories(da);		
	}
	*/
	public void pingDirectories(){
		UDPEmptyPing uep = new UDPEmptyPing();
		byte[]msg = uep.encode();
		__broadcastObjectToDirectoriesByUDP(msg, uep, getUDPSocket());
	}
	/*
	public static void ___announceMyselfToDirectories(DirectoryAnnouncement da, DatagramSocket ds) {
		byte msg[]=da.encode();
		__announceMyselfToDirectories(msg, da, ds);
	}
	*/
	/**
	 * Address adr should have set its inetSockAddr, branch and version
	 * @param da
	 * @param ds
	 * @param adr
	 * @return
	 */
	public static boolean _announceMyselfToDirectories(DirectoryAnnouncement da, DatagramSocket ds, Address adr) {
		if (DEBUG) System.out.println("UDPServer: announceToDir: "+adr);
		//da.setTarget(adr);
		//Address adr = new Address(a);
		InetSocketAddress dir = adr.inetSockAddr;
		if (dir == null) return false;
		String address=null;
		try {
			address = adr.ipPort();
			da.setAddressVersionForDestination(adr);
			//System.out.println("UDPServer:_announceMy.. da="+da);
			byte msg[] = da.encode();
			if (DEBUG) out.println("Server:announceMyselfToDirectories: sent length: "+msg.length);
			
			if (DEBUG) {
				Decoder d = new Decoder(msg);
				DirectoryAnnouncement _da = new DirectoryAnnouncement(d);
				if (DEBUG) out.println("Server:announceMyselfToDirectories: actually sent length: "+_da);
			}
			DatagramPacket dp = new DatagramPacket(msg, msg.length);
			dp.setSocketAddress(dir);
			ds.send(dp);
			if (DEBUG_DIR) out.println("UDPServer:__announceMyselfToDirectories: sent: "+
			da+"\n"+Util.byteToHexDump(msg, " ")+" to "+dp.getSocketAddress());
			String sgn= "P";
			if (da instanceof DirectoryAnnouncement) sgn = "*";
			if (DEBUG) out.println("UDPServer:__announceMyselfToDirectories: sent: announcement to "+dp.getSocketAddress()+" "+sgn);
			return true;
		} catch (Exception e) {
			DD.directories_failed.add(dir);
			if (DEBUG_DIR) err.println("UDPServer: "+__("Error announcing myself to directory:")+dir);
			if (Application.directory_status != null) {
				Application.directory_status.setUDPOn(address, new Boolean(false));
			} else {
				System.out.println("UDPServer: __announceMyselfToDirs: UDP success no display");
			}
			return false;
		}
	}
	/**
	 * Sending to all directories in "listing_directories_addr" (to be loaded with DD.load_listing_directories).
	 * @param da
	 * @param ds
	 */
	public static void _announceMyselfToDirectories(DirectoryAnnouncement da, DatagramSocket ds) {
		//boolean DEBUG = true;
		//boolean DEBUG_DIR = true;
		//byte msg[]=da.encode();
		//__announceMyselfToDirectories(msg, da, ds);
		for (Address adr : Identity.getListing_directories_addr()) {
			_announceMyselfToDirectories(da, ds, adr);
		}
	}
	/**
	 * Send empty ping (or other object) to each directory in the list Identity.listing_directories_inet!
	 * @param da A prepared Directory Announcement
	 */
	public static void __broadcastObjectToDirectoriesByUDP(byte[] msg, Object da, DatagramSocket ds) {
		//boolean DEBUG_DIR = true;
		if(DEBUG_DIR) out.println("UDPServer: __broadcastObjectToDirectoriesByUDP: start");
//		if(ANNOUNCE_TCP){
//			if(da instanceof DirectoryAnnouncement) {
//				DirectoryAnnouncement Da = (DirectoryAnnouncement)da;
//				new AnnouncingThread(Da).start();
//			}
//		}		
		for (Address adr : Identity.getListing_directories_addr()) {
			if (DEBUG) System.out.println("UDPServer: __broadcastObjectToDirectoriesByUDP: "+adr);
			//da.setTarget(adr);
			//Address adr = new Address(a);
			InetSocketAddress dir = adr.inetSockAddr;
//		for (InetSocketAddress dir : Identity.listing_directories_inet ) {
			if (DEBUG_DIR) out.println("UDPServer:__broadcastObjectToDirectoriesByUDP: announce to: "+dir);
			String address=null;
			try {
				address = dir.getAddress().getHostAddress()+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+dir.getPort();
				DatagramPacket dp = new DatagramPacket(msg, msg.length);
				dp.setSocketAddress(dir);
				ds.send(dp);
				if(DEBUG_DIR) out.println("UDPServer:__broadcastObjectToDirectoriesByUDP: sent: "+
				da+"\n"+Util.byteToHexDump(msg," ")+" to "+dp.getSocketAddress());
				if (DEBUG) out.println("UDPServer:__broadcastObjectToDirectoriesByUDP: sent: announcement to "+dp.getSocketAddress());
				//Directories.setUDPOn(address, new Boolean(true));
			} catch(Exception e) {
				//Application.warning(_("Error announcing myself to directory:")+dir, _("Announcing Myself to Directory"));
				//e.printStackTrace();
				DD.directories_failed.add(dir);
				if (DEBUG_DIR) err.println("UDPServer: "+__("Error announcing myself to directory:")+dir);
				if (Application.directory_status != null) {
					Application.directory_status.setUDPOn(address, new Boolean(false));
				} else {
					System.out.println("UDPServer: __broadcastObjectToDirectoriesByUDP: UDP success no display");
				}
			}
		}
		if (DEBUG_DIR) out.println("server: __broadcastObjectToDirectoriesByUDP Done!");
	}
	
	/**
	 * Simply allocate the DatagramSocket sd
	 * @param port
	 * @throws SocketException
	 */
	public void try_connect(int port) throws SocketException {
		try {
			setUDPSocket(new DatagramSocket(port));
		} catch (SocketException e) {
			setUDPSocket(new DatagramSocket());
			if(DEBUG)System.out.println("Got UDP port: "+getUDPSocket().getLocalPort());
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
		try{getUDPSocket().close();}catch(Exception e){}
		this.interrupt();
	}
	synchronized void wait_if_needed() throws InterruptedException{
		while (getThreads() >= MAX_THREADS) {
			if (DEBUG) System.out.println("UDPServer:wait_if_needed: threads="+getThreads()+"> max="+MAX_THREADS);
			synchronized (lock) {
				if (getThreads() >= MAX_THREADS)
					lock.wait(DD.UDP_SERVER_WAIT_MILLISECONDS);
			}
			if (getThreads() >= MAX_THREADS) {
				if (DEBUG) System.out.println("UDPServer:wait_if_needed: crtThreads="+getThreads()+">MAX_THREADS="+MAX_THREADS);
				// Util.printCallPath("Threads="+getThreads()+">MAX_THREADS="+MAX_THREADS);
			}
		}
	}
	public void send(DatagramPacket dp) throws IOException{
		getUDPSocket().send(dp);
	}
	public void _run() {
		//this.setName("UDP Server");
		//ThreadsAccounting.registerThread();
		synchronized(lock) {
			try {
				lock.wait(DD.PAUSE_BEFORE_UDP_SERVER_START);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
		if (DEBUG||DD.DEBUG_LIVE_THREADS)System.out.println("UDPServer: run: go");
		_name = name++;
		try {
			__run();
		} catch(Exception e) {
			e.printStackTrace();
		}
		//ThreadsAccounting.unregisterThread();
	}
	public static int name = 0;
	public int _name;
	public void __run() {
		try {
			Server.detectDomain(Identity.udp_server_port);
			if(DEBUG) System.out.println("UDPServer:<init>: domain detected");
		} catch (Exception e) {
			e.printStackTrace();
		}
		// wait for myself
		data.HandlingMyself_Peer.get_myself_with_wait();
		//MyselfHandling.set_my_peer_ID_UDP (id, ds);
		if (DEBUG) System.out.println("UDPServer:_run: will broadcast");
		UDPServer.announceMyselfToDirectories();
		if (DEBUG) System.out.println("UDPServer:_run: peer ID just set & broadcast");

		synchronized(Client2.conn_monitor) {
			if (Client2.conn == null)
				Client2.conn  = new Connections(Application.db);
		}

		DD.ed.fireServerUpdate(new CommEvent(this, null, null, "LOCAL", "UDPServer starting at:"+Identity.udp_server_port));
		//this.announceMyselfToDirectories();
		int cnt = 0;
		for (;;) {
			Application_GUI.ThreadsAccounting_ping("Cycle");
			if (DEBUG||DD.DEBUG_LIVE_THREADS) out.print("(UDP*)");
			if (turnOff) break;
			if (DD.DEBUG_COMMUNICATION_LOWLEVEL) out.println("userver: UDPServer reclaim!");
			try {
				this.sendFragmentReclaim();
				if (this.isInterrupted()) continue;
				if (DD.DEBUG_COMMUNICATION_LOWLEVEL) out.println("userver: ************* wait!");
				Application_GUI.ThreadsAccounting_ping("Waiting");
				wait_if_needed();
				if (this.isInterrupted()) continue;
				if (DD.DEBUG_COMMUNICATION_LOWLEVEL) out.println("userver: UDPServer will accept!*************");
				buffer = new byte[UDP_BUFFER_LENGTH];
				DatagramPacket pak = new DatagramPacket(buffer, UDP_BUFFER_LENGTH);
				// calling the DatagramPacket receive call
				getUDPSocket().setSoTimeout(Server.TIMEOUT_UDP_NAT_BORER); // might have changed
				Application_GUI.ThreadsAccounting_ping("Accepting");
				getUDPSocket().receive(pak);
				
				if (DEBUG) out.println("userver: ************ UDPServer accepted from "+pak.getSocketAddress()+", will launch!");
				if (this.isInterrupted()) continue;
				if (DEBUG) out.println("userver: ************* not interrupted, start!");
				//System.out.println("U");
				new UDPServerThread(pak, this).start();
				if (DEBUG) out.println("userver: ************* UDPServer started!");
			}
			catch (SocketTimeoutException e){
				if((((++cnt) %Server.TIMEOUT_UDP_Announcement_Diviser) == 0)) {
					if(DEBUG) out.println("userver: ************* UDPServer announce!");
					UDPServer.announceMyselfToDirectories();
				}else{
					if(DEBUG) out.println(""+cnt);
					if(DD.DEBUG_COMMUNICATION_LOWLEVEL) out.println("userver: ************* UDPServer ping!");
					this.pingDirectories();
				}
				continue;
			}
			catch (SocketException e){
				if(DEBUG) out.println("server: ************* "+e);
			}
			catch(Exception e){
				e.printStackTrace();
			}

			if(((++cnt) %Server.TIMEOUT_UDP_Announcement_Diviser) == 0) {
				if(DEBUG) out.println("userver: 2************* UDPServer announce!");
				UDPServer.announceMyselfToDirectories();
			}else{
				if(DEBUG) out.println(":"+cnt);
				if(DD.DEBUG_COMMUNICATION_LOWLEVEL) out.println("userver: 2************* UDPServer ping!");
				this.pingDirectories();
			}

			if(DD.DEBUG_COMMUNICATION_LOWLEVEL) out.println("userver: ************* UDPServer loopend!");
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
		if (args.length > 2){
			direct = Util.stringInt2bool(args[2], true); 
			out.println("direct = "+args[2]+"="+direct);
		}
		try {
			System.out.println("UDPServThread: main: start");
			Application.db = new DBInterface(target);
			// quit when no peer found
			Identity.init_Identity(true, true, false);
			System.out.println("UDPServThread: main: inited IDs");
			Identity id = HandlingMyself_Peer.loadIdentity(null);
			D_Peer me = HandlingMyself_Peer.getPeer(id);
			HandlingMyself_Peer.setMyself_currentIdentity_announceDirs(me, false, false); // me not kept
			HandlingMyself_Peer.get_myself_with_wait(); // just tests by exit
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
			asr.address = HandlingMyself_Peer.get_myself_with_wait();
			
			
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
				if(DEBUG)System.out.println("UDPServer: verif sent Decoded request: "+_asr.toSummaryString());
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
				Identity.current_peer_ID = Identity.getCurrentPeerIdentity_QuitOnFailure();
				SK sk = HandlingMyself_Peer.getMyPeerSK();

				sa = UpdateMessages.buildAnswer(asr , peerID);

				byte[]sa_msg = sa.encode();
				
				//System.out.println("Got="+sa.toSummaryString());

				//us.sendLargeMessage(psa, sa_msg, DD.MTU, peerGID, DD.MSGTYPE_SyncAnswer);
				//if(DEBUG)System.out.println("\n\n***************************\nUDPServer:run: Answer sent! "+sa_msg.length+"\n"+sa.toSummaryString());

				Application.db = new DBInterface(target);
				Identity.current_peer_ID = null;
				Identity.current_peer_ID = Identity.initMyCurrentPeerIdentity_fromDB(true);

				sa = null;
				SyncAnswer ra = null;
				ra = new SyncAnswer().decode(new Decoder(sa_msg));
				//if(DEBUG)System.out.println("\n\n************************\nUDPServer:run: Answer received! "+ra.toSummaryString());

				String global_peer_ID = ra.responderGID;
				D_Peer peer =null;
				String peer_ID = null;
				if (global_peer_ID != null) {
					peer = D_Peer.getPeerByGID_or_GIDhash(global_peer_ID, null, true, false, false, null);// new D_Peer(global_peer_ID, false, false, true);
					peer_ID = peer.getLIDstr_keep_force(); //table.peer.getLocalPeerID(global_peer_ID);
				}
				if (peer_ID == null) {
					if(DEBUG)System.out.println("UDPServer:run: Answer received from unknown peer: "+global_peer_ID);

				} else
					if(Application.peers!=null) Application.peers.setConnectionState(peer_ID, DD.PEERS_STATE_CONNECTION_UDP);

				//System.out.println("Got msg size: "+len);//+"  bytes: "+Util.byteToHex(update, 0, len, " "));
				if(DEBUG)System.out.println("UDPServer:run: Answer received will be integrated");
				RequestData rq= new RequestData();


				if(DEBUG)System.out.println("\n\n*****************\n*******************\n\nUDPServer:run: Answer received: final"+ra.toSummaryString());

				UpdateMessages.integrateUpdate(ra, new InetSocketAddress(10000), new Object(), global_peer_ID, ra.peer_instance, peer_ID, null,rq, peer, false);

				if(DEBUG)System.out.println("\n\n*****************\n*******************\n\nUDPServer:run: Done");

				//if(true) return;
				sk = HandlingMyself_Peer.getMyPeerSK();
				boolean filtered = false;
				String _lastSnapshotString = null;
				ASNSyncRequest asreq = ClientSync.buildRequest(_lastSnapshotString, ra.upToDate, peer_ID);
				if(filtered) asreq.orgFilter=UpdateMessages.getOrgFilter(peer_ID);
				asreq.sign(sk);
				byte[] buf = asreq.encode();
				//if(DEBUG)System.out.println("\n\n*****************\n*******************\n\nUDPServer:run: Request sent: "+asreq.toSummaryString());
				//ASNSyncRequest asr2 = new ASNSyncRequest();
				asr = new ASNSyncRequest();
				asr.decode(new Decoder(buf));
				if(DEBUG)System.out.println("\n\n*****************\n*******************\n\nUDPServer:run: Request recv: "+asr.toSummaryString());
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
	}
	public static boolean isRunning() {
		return Application.aus != null;
	}
	public static DatagramSocket getUDPSocket() {
		return ds;
	}
	public static void setUDPSocket(DatagramSocket ds) {
		UDPServer.ds = ds;
	}
}
class AnnouncingThread extends util.DDP2P_ServiceThread {
	DirectoryAnnouncement Da;
	AnnouncingThread (DirectoryAnnouncement Da) {
		super("TCP Announcing to Directory", true);
		this.Da = Da;
	}
	public void _run(){
		Server.announceMyselfToDirectories(Da);
	}
}
