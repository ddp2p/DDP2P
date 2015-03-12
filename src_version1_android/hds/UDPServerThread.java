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

import static java.lang.System.out;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Calendar;
import java.util.HashSet;

import streaming.RequestData;
import streaming.UpdateMessages;
import util.CommEvent;
import util.DDP2P_ServiceThread;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;
import ciphersuits.Cipher;
import ciphersuits.SK;
import util.P2PDDSQLException;
import config.Application;
import config.Application_GUI;
import config.DD;
import config.Identity;
import data.D_Peer;
import data.D_PeerInstance;
import data.HandlingMyself_Peer;

public class UDPServerThread extends util.DDP2P_ServiceThread {
	private static final HashSet<SocketAddress> handled = new HashSet<SocketAddress>();
	public static boolean DEBUG = false;
	public static boolean _DEBUG = true;
	DatagramPacket pak;
	UDPServer us;
	String peer_address;
	private static int verification_warning = 0;
	private static final int MAX_VERIFICATION_WARNING = 3;
	/**
	 * Handle a UDP packet
	 * @param _pak
	 * @param _us
	 */
	public UDPServerThread(DatagramPacket _pak, UDPServer _us){
		super("UDP Server Thread", false);
		pak = _pak; us = _us;
		InetSocketAddress sa = (InetSocketAddress) pak.getSocketAddress();
		try{
			peer_address = sa.getAddress().getHostAddress()+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+sa.getPort();
		}catch(Exception e){};
	}
	static int k = 0;
	public void _run() {
		this.setName("UDP Server Thread: "+(k++));
		//ThreadsAccounting.registerThread();
		try{
			synchronized(us.lock){
				us.incThreads();
			}
			try{
				__run();
			}catch(Exception e){
				e.printStackTrace();
			}
			synchronized(us.lock){
				us.decThreads();
				us.lock.notify();
			}
			if(us.getThreads() < 0) {
				System.err.println("UDPServerThread:run:Threads number under 0: "+ us.getThreads()+"/"+UDPServer.MAX_THREADS);
			}
		}catch(Exception e){e.printStackTrace();}
		//ThreadsAccounting.unregisterThread();
	}
	
	public void __run() {
		if(DEBUG)System.out.println("UDPServerThread:run: Running UDPHandler thread:"+us.getThreads()+" from"+pak.getSocketAddress());
		byte[] buffer = pak.getData();
		byte[] msg=null;
		Decoder dec = new Decoder(pak.getData(),pak.getOffset(),pak.getLength());

		// Fragment
		if (dec.getTypeByte() == DD.TAG_AC12) { //Fragment
			//System.out.println("F_");
			if (DEBUG) System.out.println("UDPServer:run: Fragment received, will decode");
			UDPFragment frag = new UDPFragment();
			try {
				frag.decode(dec);
				if (DEBUG) System.out.println("UDPServer:run: receives fragment "+frag.sequence+"/"+frag.fragments+" from:"+pak.getSocketAddress());
				if (frag.fragments > DD.UDP_MAX_FRAGMENTS) {
					if(_DEBUG)System.out.println("UDPServer:run: Too Many Fragments: "+frag);
					return;
				}
				if ((frag.data!=null) && (frag.data.length > DD.UDP_MAX_FRAGMENT_LENGTH)) {
					if (_DEBUG) System.out.println("UDPServer:run: Too Large Fragments: "+frag.data.length);
					return;
				}
				msg = us.getFragment(frag, pak.getSocketAddress());
				if (DEBUG) System.out.println("UDPServer:run: Fragment received: "+frag);
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (msg == null) {
				if (DEBUG) System.out.println("UDPServer:run: Fragment not ready");
				return;
			}
			dec = new Decoder(msg);
			if (DEBUG) System.out.println("UDPServer:run: Continuing to decode message!");
		} else {
			//System.out.println("Packet received not fragment");
			
			// Fragment ACK
			if (dec.getTypeByte()==DD.TAG_AC11) { //Fragment ACK
				//System.out.println("A");
				UDPFragmentAck frag = new UDPFragmentAck();
				try {
					frag.decode(dec);
					if(DEBUG)System.out.println("UDPServer:run: Packet received is fragment "+frag.msgID+" ack from: "+pak.getSocketAddress());
					us.getFragmentAck(frag, pak.getSocketAddress());
				} catch (ASN1DecoderFail e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			} else {
				
				// Fragments NACK
				if(dec.getTypeByte()==DD.TAG_AC15){
					//System.out.println("N");
					UDPFragmentNAck frag = new UDPFragmentNAck();
					try {
						frag.decode(dec);
						if (DEBUG) System.out.println("UDPServer:run: Packet received is fragment "+frag.msgID+" nack from: "+pak.getSocketAddress());
						us.getFragmentNAck(frag);
					} catch (ASN1DecoderFail e) {
						e.printStackTrace();
					}
					return;
					
				} else {
					
					// Fragments Reclaim
				  if (dec.getTypeByte() == DD.TAG_AC16) {
					//System.out.println("R");
					if (DEBUG) System.out.println("UDPServer:run: Packet received is fragments reclaim");
					UDPReclaim frag = new UDPReclaim();
					try {
						frag.decode(dec);
					} catch (ASN1DecoderFail e) {
						e.printStackTrace();
					}
					try {
						us.getFragmentReclaim(frag, pak.getSocketAddress());
					} catch (IOException e) {
						e.printStackTrace();
					}
					return;					
				  }
				}
			}
		}
		
		
		if (DEBUG) System.out.println("UDPServer:run: Message received will be interpreted");
		
		
		if	(dec.getTypeByte()	==	DD.TAG_AC13) {
			//System.out.println("P");
			ASNUDPPing aup = new ASNUDPPing();
			try { // check if this is a PING
				if (DEBUG) System.out.println("UDPServer:run: UDPServer receives ping from:"+pak.getSocketAddress());
				if (DEBUG) System.out.println("UDPServer:run: UDPServer attempts decoding Ping");
				aup.decode(dec);
				if (DEBUG) System.out.println("UDPServer:run: receives: "+aup+" from:"+pak.getSocketAddress());
				if (aup.senderIsPeer) {
					if (!handleSTUNfromPeer(aup)) return;
				} else {
					if (DEBUG) System.out.println("UDPServer:run: receives forwarded ping from directory/initiator! "+aup);
					if (!handleSTUNForward(aup)) return;
				}			
			
				if (DEBUG) System.out.println("UDPServer:run: UDPServer pinged on request: "+aup);
			} catch (ASN1DecoderFail e) {
				if (DEBUG) System.out.println("Ping decoding failed! "+e);
				e.printStackTrace();
			} catch (P2PDDSQLException e) {
				if (DEBUG) System.out.println("Database access failed! "+e);
				e.printStackTrace();
			}
			return;
		}
		
		// Directory announcement answer
		if (dec.getTypeByte() == DirectoryAnnouncement_Answer.getASN1Type()) {// AC14
		  if (DEBUG) System.out.println("UDPServer:run: Announcement answer decoding!");
		  try {
			Decoder answer_dec=dec;
			DirectoryAnnouncement_Answer daa=new DirectoryAnnouncement_Answer(answer_dec);
			//daa.decode(answer_dec);
			if (DEBUG) out.println("UDPServer: Directory Answer: "+daa.result);
			if (Application.directory_status != null) {
				Application.directory_status.setUDPOn(peer_address, new Boolean(true));
			} else {
				System.out.println("UDPServer:gotAnnouncementAnswer no display");
			}
		  } catch(Exception e){
			  if (DEBUG) System.out.println("UDPServer: UDP Announcement answer decoding failed! "+e);
			  if (Application.directory_status != null)
					Application.directory_status.setUDPOn(peer_address, new Boolean(false));
		  }
		  return;
		}
		
		// Directory answer
		if (dec.getTypeByte() == hds.DirectoryAnswerMultipleIdentities.getASN1Type()) { // AC19
			try {
				DirectoryAnswerMultipleIdentities dami = new DirectoryAnswerMultipleIdentities(dec);
				hds.Connections.registerIncomingDirectoryAnswer(dami, pak);
				if (DEBUG) out.println("UDPServer:DirectoryAnswerMultipleIdentities: Directory Answer: "+dami);
				if (Application.directory_status != null) {
					Application.directory_status.setUDPOn(peer_address, new Boolean(true));
				} else {
					System.out.println("UDPServer:DirectoryAnswerMultipleIdentities no display");
				}
			} catch (ASN1DecoderFail e) {
				  if(DEBUG)System.out.println("UDPServer:DirectoryAnswerMultipleIdentities: UDP Announcement answer decoding failed! "+e);
				  if(Application.directory_status!=null)
						Application.directory_status.setUDPOn(peer_address, new Boolean(false));
				e.printStackTrace();
			}
			return;
		}
		
		if (dec.getTypeByte()==DD.TAG_AC7) {
			if (DEBUG) System.out.println("\n*******************\nUDPServer:run: Trying to decode request!");
			handleRequest(dec);
			return;
		}

		if (dec.getTypeByte() == DD.TAG_AC8) {
			//boolean DEBUG=true;
			if (DEBUG || DD.DEBUG_COMMUNICATION) System.out.println("\n*************\nUDPServer:run: Answer received fully from "+pak.getSocketAddress());
			handleAnswer(dec);
			return;
		}
		if (DEBUG) System.out.println("Unknown message["+pak.getLength()+"]: "+Util.byteToHexDump(buffer));
		if (msg != null) if (DEBUG) System.out.println(" msg["+msg.length+"]="+Util.byteToHexDump(msg));
		return;
	}

	private void handleAnswer(Decoder dec) {
		//boolean DEBUG=true;
		SyncAnswer sa = new SyncAnswer();
		try {
			sa.decode(dec);
			// System.out.println("Answer received is: "+Util.trimmed(sa.toString(),Util.MAX_UPDATE_DUMP));
			if (DEBUG) System.out.println("UDPServer:run: Answer received & decoded from: "+pak.getSocketAddress());
			if (DEBUG || DD.DEBUG_COMMUNICATION) System.out.println("UDPServer:run: Answer received is: "+sa.toSummaryString());
			//if(_DEBUG || DD.DEBUG_COMMUNICATION)System.out.println("UDPServer:run: Answer received is: "+sa.toSummaryString());
			if (DEBUG || DD.DEBUG_COMMUNICATION || DD.DEBUG_CHANGED_ORGS)
				System.out.println("\n\n\nUDPServer:run: Answer rcv! ch_org="+Util.nullDiscrimArraySummary(sa.changed_orgs,"--"));
			// integrate answer
			//int len = dec.getMSGLength();
			String global_peer_ID = sa.responderGID;
			//String instance = sa.peer_instance;
			
			D_Peer peer = null;
			String peer_ID = null;
			boolean blocked[] = new boolean[]{true};
			
			// check if blocked
			if (global_peer_ID != null) {
				// may decide to load addresses only for versions >= 2
				// peer = D_Peer.getPeerByGID(global_peer_ID, true, false);
				peer = D_Peer.getPeerByGID_or_GIDhash(global_peer_ID, global_peer_ID, true, false);
				if (peer != null) {
					peer_ID = peer.peer_ID; //table.peer.getLocalPeerID(global_peer_ID);
					blocked[0] = peer.getBlocked();//.component_preferences.blocked;
				}
			}
			//String peer_ID = table.peer.getLocalPeerID(global_peer_ID, blocked);
			if ((peer_ID != null) && blocked[0]) return;
			
			
			//////////////////// Not blocked yet//////////////////////
			
			
			// some debug messages
			if (peer_ID == null) {
				if (DEBUG || DD.DEBUG_TODO) System.out.println("UDPServerThread:_run: Answer received from unknown peer: "+global_peer_ID);
			} else
				if (Application.peers != null) Application.peers.setConnectionState(peer_ID, DD.PEERS_STATE_CONNECTION_UDP);
			//System.out.println("Got msg size: "+len);//+"  bytes: "+Util.byteToHex(update, 0, len, " "));
			if (DEBUG) System.out.println("UDPServer:run: Answer received will be integrated");
			
			
			// prepare (try to guess) addressID for integration (may send it with request to keep robust track of successful address!!)
			InetSocketAddress saddr = (InetSocketAddress) pak.getSocketAddress();
			String address_ID = null;
			if (peer_ID != null) address_ID = D_Peer.getAddressID (saddr, peer_ID);
			RequestData rq = new RequestData(); // used to gather unknown GIDs, to be requested next
			if (peer != null) if (_DEBUG) System.out.println("UDPServerThread:handleAnswer: answer from safe: "+peer.getName()+" from IP: "+saddr.getAddress()+":"+saddr.getPort()+" upto: "+Encoder.getGeneralizedTime(sa.upToDate)+" e: "+sa.elements());
			if (( UpdateMessages.integrateUpdate(sa, saddr, this, global_peer_ID, sa.peer_instance, peer_ID, address_ID, rq, peer)) ||
					((rq != null) && !rq.empty()) ) {
				if (DEBUG) System.out.println("UDPServer:run: Should reply with request for: "+rq);
				DD.touchClient();
			}

			
			if (DEBUG) System.out.println("UDPServer:run: Answer received and integrated "+pak.getSocketAddress());
			if (DEBUG) if ((rq != null) && !rq.empty()) System.out.println("UDPServer:run: Should reply with request for: "+rq);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} 
	}
	
	private void handleRequest(Decoder dec) {
		
		  ASNSyncRequest asr;
		  SocketAddress psa = null;
		  try { // This is not a ping. Check if it is a request
			psa = pak.getSocketAddress();

			if (DD.DROP_DUPLICATE_REQUESTS) {
				synchronized (handled) {
					if (handled.contains(psa)) {
						if (DEBUG || DD.DEBUG_COMMUNICATION) System.out.println("UDPServer: Abandoned duplicate request from: "+psa);
						return;
					}
					handled.add(psa);
				}
			}
			asr = new ASNSyncRequest();
			asr.decode(dec);
			if (DEBUG) System.out.println("UDPServer: Received request from: "+psa);
			if (DEBUG || DD.DEBUG_COMMUNICATION)System.out.println("UDPServer: Decoded request: "+asr.toSummaryString()+" from: "+psa);
			if (DEBUG || DD.DEBUG_COMMUNICATION || DD.DEBUG_CHANGED_ORGS)
				if(asr.pushChanges!=null)System.out.println("\n\n\nUDPServer:run: Request rcv! ch_org="+Util.nullDiscrimArraySummary(asr.pushChanges.changed_orgs,"--"));
			if (!DD.ACCEPT_STREAMING_REQUEST_UNSIGNED && !asr.verifySignature()) {
				DD.ed.fireServerUpdate(new CommEvent(this, null, psa, "UDPServer", "Unsigned Sync Request received: "+asr));
				System.err.println("UDPServer:run: Unsigned Request received: "+asr.toSummaryString());
				System.err.println("UDPServer:run: Unsigned Request received: "+asr.toString());
				throw new Exception("Unsigned request");
			}
			
			if (!Server.extractDataSyncRequest(asr, psa, this)) {
				if (_DEBUG) System.out.println("UDPServer:run: Request Discarded ************************* \""+((asr.address == null)?"Unknown":asr.address.getName())+"\"");
				return;
			}
			if (DEBUG) System.out.println("UDPServer:run: Request Data extracted *************************");

			String peerGID = null, instance = null;
			if (asr.address != null) {
				peerGID = asr.address.component_basic_data.globalID;
				instance = asr.address.getInstance();
			}
			else {
				if (DEBUG) System.out.println("UDPServer:run: request from UNKNOWN abandoned");
				if (DEBUG) System.out.println("UDPServer:run: Answer not sent!");
				throw new Exception("Unknown peer");
			}
			
			if (UDPServer.transferringPeerAnswerMessage(peerGID, instance)) {
				if(DEBUG)System.out.println("UDPServer:run: UDPServer Answer being sent for: "+Util.trimmed(peerGID));
				//throw new Exception("While transferring answer to same peer");
				return;
			}
			String peer_ID = null;
			D_Peer peer_sender = D_Peer.getPeerByGID(peerGID, true, false);
			boolean blocked[] = new boolean[] {true}; // blocking new requesters?
			if (peer_sender != null) {
				peer_ID = peer_sender.get_ID(); // table.peer.getLocalPeerID(peerGID, blocked);
				blocked = new boolean[] {peer_sender.getBlocked()};
			}
			if (asr.address != null) asr.address.peer_ID = peer_ID;
			//D_PluginInfo.recordPluginInfo(asr.plugin_info, peerGID, peer_ID);
			if ((Application.peers != null) && (peer_ID != null)) Application.peers.setConnectionState(peer_ID, DD.PEERS_STATE_CONNECTION_UDP);
			if (blocked[0] && (peer_sender != null)) {
				if (DEBUG) System.out.println("UDPServer:run: Blocked! "+peer_ID+" "+((asr.address!=null)?asr.address.component_basic_data.name:"noname"));
				return;
			}
			if (peer_sender != null) if (_DEBUG) System.out.println("UDPServerThread:handleAnswer: request from safe: "+peer_sender.getName()+" from IP: "+psa+" from: "+Encoder.getGeneralizedTime(asr.lastSnapshot));
			//if data is a request from a peer
			// then send an answer to that peer
			SyncAnswer sa = UpdateMessages.buildAnswer(asr, peer_ID);
			
			D_Peer p = data.HandlingMyself_Peer.get_myself();
			sa.peer_instance = p.getPeerInstance(p.getInstance());
			
			if (peer_sender != null) if (_DEBUG) System.out.println("UDPServerThread:handleAnswer: return "+sa.elements()+" upto: "+Encoder.getGeneralizedTime(sa.upToDate));
			if (DEBUG) System.out.println("UDPServer:run: Prepared answer!");
			if (sa == null) return;
			
			if (DEBUG) System.out.println("\n\nPrepared answer: "+sa.toString());
			byte[] sa_msg = sa.encode();
			if (DEBUG) System.out.println("\n\nPrepared answer2: "+new ASNSyncPayload().decode(new Decoder(sa_msg)).toString());

			us.sendLargeMessage(psa, sa_msg, DD.MTU, peerGID, DD.MSGTYPE_SyncAnswer);
			//if(_DEBUG || DD.DEBUG_COMMUNICATION)System.out.println("UDPServer:run: Answer sent! "+sa.toSummaryString());
			if (DEBUG || DD.DEBUG_COMMUNICATION || DD.DEBUG_CHANGED_ORGS)
				System.out.println("\n\n\nUDPServer:run: Answer sent! ch_org="+Util.nullDiscrimArraySummary(sa.changed_orgs,"--"));
			//System.out.println("Answer sent: "+Util.trimmed(sa.toString(),Util.MAX_UPDATE_DUMP));
		  } catch (ASN1DecoderFail e) {
			e.printStackTrace();
		  } catch (P2PDDSQLException e) {
			e.printStackTrace();
		  } catch (IOException e) {
			e.printStackTrace();
		  } catch (Exception e) {
			e.printStackTrace();
		  }
		  
		  if (DD.DROP_DUPLICATE_REQUESTS) {
			  synchronized(handled) {
				  if (psa != null) handled.remove(psa);
			  }
		  }
	}
	private boolean handleSTUNForward(ASNUDPPing aup) throws P2PDDSQLException {
		//if data is from directory server,
		// then ping the peer willing to contact me
		if (!aup.senderIsInitiator) { // this is from directory (since from peer does not come here)
			if (Application.directory_status != null)
				Application.directory_status.setNATOn(peer_address, new Boolean(true));
		}
		if (Identity.getMyPeerGID().equals(aup.initiator_globalID)) {
			if (DEBUG) System.out.println("UDPServer:run: Ping received from myself! Abandon.");
			return true;
		}
		if (!Util.equalStrings_null_or_not(Identity.getMyPeerGID(), aup.peer_globalID)) {
			if (DEBUG) System.out.println("UDPServer:run: Ping received for smbdy else!"+aup+" \n\tme="+Identity.getMyPeerGID());
			aup.peer_globalID = Identity.getMyPeerGID();
			aup.peer_instance = Identity.getMyPeerInstance();
		}
		if (!Util.equalStrings_null_or_not(Identity.getMyPeerInstance(), aup.peer_instance)) {
			if (DEBUG) System.out.println("UDPServer:run: Ping received for other instance!"+aup+" \n\tme="+Identity.getMyPeerGID());
			aup.peer_instance = Identity.getMyPeerInstance();
		}
		aup.senderIsPeer = true;
		InetSocketAddress next_dest;
		String peer_ID = D_Peer.getPeerLIDbyGID(aup.initiator_globalID);
		if (aup.senderIsInitiator == true) {
			aup.senderIsInitiator = false;
			next_dest = (InetSocketAddress) pak.getSocketAddress();
			if(Application.peers!=null) Application.peers.setConnectionState(peer_ID, DD.PEERS_STATE_CONNECTION_UDP_NAT);
		}else{
			next_dest = new InetSocketAddress(aup.initiator_domain,aup.initiator_port);
			if(Application.peers!=null) Application.peers.setConnectionState(peer_ID, DD.PEERS_STATE_CONNECTION_UDP);
		}
		byte[] buf = aup.encode();
		DatagramPacket reqDP = new DatagramPacket(buf, buf.length);
		reqDP.setSocketAddress(next_dest);
		try {
			us.ds.send(reqDP);
			if(DEBUG)System.out.println("UDPServer:run: ping returned to initiator: "+next_dest+" ping="+aup);
		} catch (IOException e) {
			e.printStackTrace();
			return true;
		}
		return false;
	}

	private boolean handleSTUNfromPeer(ASNUDPPing aup) throws ASN1DecoderFail {
		//if data is a ping from a contacted peer
		// than send a request to that peer
		synchronized (us.lock_reply) {
			String g_peerID = aup.peer_globalID;
			if (g_peerID == null) {
				if (_DEBUG) System.out.println("UDPServer:run: receives null GID ping: "+aup+" from:"+pak.getSocketAddress());
				return false;
			}
	
			String instance = aup.peer_instance;

			if (DD.AVOID_REPEATING_AT_PING && !Application.aus.hasSyncRequests(g_peerID, instance)) {
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Received ping confirmation already handled from peer"));
				if (DEBUG) System.out.println("UDPServer:run: Ping already handled for: "+Util.trimmed(g_peerID));
				return false;					
			}
			
			D_Peer peer = D_Peer.getPeerByGID(g_peerID, true, false);
			if (peer == null) return false; // not requesting unknown peers
			
			DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Received ping confirmation from peer"));
			if (DEBUG) System.out.println("UDPServer:run: GID ping: "+aup+" from: "+pak.getSocketAddress());
			Application.aus.delSyncRequests(g_peerID, instance);

			//get last snapshot for peerID
			D_PeerInstance dpi = peer.getPeerInstance(instance);
			String _lastSnapshotString = null;
			Calendar lastSnapshotString = null;
			if (dpi != null) {
				_lastSnapshotString = dpi.get_last_sync_date_str(); //ClientSync.getLastSnapshot(g_peerID);
				lastSnapshotString = dpi.get_last_sync_date(); //ClientSync.getLastSnapshot(g_peerID);
			}

			if (_DEBUG) System.out.println("UDPServer:run: Request being sent from: "+_lastSnapshotString+" to: "+peer.getName()+" GID: "+Util.trimmed(g_peerID));

			if (UDPServer.transferringPeerMessage(g_peerID, instance)) {
				if (_DEBUG) System.out.println("UDPServer:run: Request being sent for: "+Util.trimmed(g_peerID));
				return false;
			}
			if (DEBUG) System.out.println("UDPServer:run: will build request! "+Util.trimmed(g_peerID)+" ... time="+_lastSnapshotString);
			//D_Peer dp = D_Peer.getPeerByGID(g_peerID, true, false, null);
			if (_DEBUG) System.out.println("UDPServer:run: ping reply from: "+peer.getName()+" <"+peer.getEmail()+"> "+" from:"+pak.getSocketAddress());
			String peer_ID = peer.get_ID();
			boolean filtered = peer.component_preferences.filtered;

			ASNSyncRequest asreq;
			asreq = ClientSync.peer_scheduled_requests.get(g_peerID);
			if (asreq != null) {
				System.out.println("\n\n\nUDPServer:run: scheduled Request: "+asreq);
				ClientSync.peer_scheduled_requests.remove(g_peerID);
				asreq.lastSnapshot = lastSnapshotString;
				//String global_peer_ID = Identity.current_peer_ID.globalID;
				//if(DEBUG) System.out.println("Client: buildRequests: myself=: "+global_peer_ID);
				try {
					asreq.address = HandlingMyself_Peer.get_myself();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (DEBUG || DD.DEBUG_COMMUNICATION || DD.DEBUG_CHANGED_ORGS)
					if (asreq.pushChanges != null) System.out.println("\n\n\nUDPServer:run: sched Request snd! ch_org="+Util.nullDiscrimArraySummary(asreq.pushChanges.changed_orgs,"--"));
					else System.out.println("\n\n\nUDPServer:run: sched Request snd! ch_org=null");
			} else {
				if (DEBUG || DD.DEBUG_CHANGED_ORGS) System.out.println("\n\n\nUDPServer:run: build Request");
				asreq = ClientSync.buildRequest(_lastSnapshotString, null, peer_ID);
				if (DEBUG || DD.DEBUG_COMMUNICATION || DD.DEBUG_CHANGED_ORGS)
					if (asreq.pushChanges != null) System.out.println("\n\n\nUDPServer:run: Request snd! ch_org="+Util.nullDiscrimArraySummary(asreq.pushChanges.changed_orgs,"--"));
					else System.out.println("\n\n\nUDPServer:run: Request snd! ch_org=null");
			}
			if (filtered) asreq.orgFilter = UpdateMessages.getOrgFilter(peer_ID);
			SK sk = asreq.sign();

			ciphersuits.PK _pk = null;
			if (DD.VERIFY_SENT_SIGNATURES) {
				try {
					_pk = ciphersuits.Cipher.getPK(asreq.address.component_basic_data.globalID);
					if (!Cipher.isPair(sk,_pk)) {
						Util.printCallPath("Unmatched keys undecoded");
						System.out.println("\n\n\nUDPServer:run: pk="+_pk);
						System.out.println("\n\n\nUDPServer:run: sk="+sk);
						System.out.println("\n\n\nUDPServer:run: asreq="+asreq);
					}
				}catch(Exception e){e.printStackTrace();}
				boolean r = asreq.verifySignature();
				if (!r) Util.printCallPath("failed verifying: "+asreq);
				else
					if(DEBUG)System.out.println("UDPServThread: _run: signature success");
			}

			byte[] buf = asreq.encode();
			if (DD.VERIFY_SENT_SIGNATURES) {
				Decoder d = new Decoder(buf);
				ASNSyncRequest asr = new ASNSyncRequest();
				asr.decode(d);
				//if(DEBUG)System.out.println("UDPServer: Received request from: "+psa);
				if(DEBUG)System.out.println("UDPServer: verif sent Decoded request: "+asr.toSummaryString());

				ciphersuits.PK pk;
				try {
					pk = ciphersuits.Cipher.getPK(asr.address.component_basic_data.globalID);
					if (!Cipher.isPair(sk,pk)) {
						Util.printCallPath("Unmatched keys");
						System.out.println("UDPServThread: _run: pk ="+pk);
						System.out.println("UDPServThread: _run: _pk ="+_pk);
						System.out.println("UDPServThread: _run: pk_eq ="+pk.equals(_pk));
					}
				} catch(Exception e) { e.printStackTrace(); }
				if (!asr.verifySignature()) {
					//DD.ed.fireServerUpdate(new CommEvent(this, null, psa, "UDPServer", "Unsigned Sync Request received: "+asr));
					System.err.println("UDPServer:run: Unsigned Request sent: "+asr.toSummaryString());
					System.err.println("UDPServer:run: Unsigned Request rsent: "+asr.toString());
					System.err.println("UDPServer:run: Unsigned Request old: "+asreq.toString());
					if (verification_warning ++ < MAX_VERIFICATION_WARNING)
						new DDP2P_ServiceThread("UDP Server Warning", true) {
						public void _run(){
							Application_GUI.warning(Util._("Abandoned sending message with inconsistent request")+
									"\n"+
									Util._("You can disable verification of sent messages!")+
									"\n"+verification_warning+"/"+MAX_VERIFICATION_WARNING,
									Util._("Message no longer sent!"));
						}
					}.start();
					return false;
				}					
			}
			SocketAddress rsa = new InetSocketAddress(aup.peer_domain,aup.peer_port);
			try {
				//us.ds.send(reqDP);
				us.sendLargeMessage(rsa, buf, DD.MTU, g_peerID, DD.MSGTYPE_SyncRequest);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			if(DEBUG) {
				String slogan=null;
				if(asreq.address!=null) slogan = asreq.address.component_basic_data.slogan;
				if(DEBUG)System.out.println("UDPServer:run: sends request: "+slogan+" to: "+rsa);
				if(DEBUG)System.out.println("UDPServer:run: sends request: "+asreq+" to: "+rsa);
			}
		}
		return true;
	}
}