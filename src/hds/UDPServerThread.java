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
import java.util.ArrayList;

import streaming.RequestData;
import streaming.UpdateMessages;
import util.CommEvent;
import util.Util;
import widgets.peers.Peers;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;

import ciphersuits.Cipher;
import ciphersuits.SK;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;
import config.Identity;
import data.D_PeerAddress;

public class UDPServerThread extends Thread {
	public static boolean DEBUG = false;
	public static boolean _DEBUG = true;
	DatagramPacket pak;
	UDPServer us;
	String peer_address;
	/**
	 * Handle a UDP packet
	 * @param _pak
	 * @param _us
	 */
	public UDPServerThread(DatagramPacket _pak, UDPServer _us){
		pak = _pak; us = _us;
		InetSocketAddress sa = (InetSocketAddress) pak.getSocketAddress();
		try{
			peer_address = sa.getAddress().getHostAddress()+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+sa.getPort();
		}catch(Exception e){};
	}
	public void run() {
		try{
			synchronized(us.lock){
				us.incThreads();
			}
			try{
				_run();
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
	}
	public void _run() {
		if(DEBUG)System.out.println("UDPServer:run: Running UDPHandler thread:"+us.getThreads()+" from"+pak.getSocketAddress());
		byte[] buffer = pak.getData();
		byte[] msg=null;
		Decoder dec = new Decoder(pak.getData(),pak.getOffset(),pak.getLength());
		if(dec.getTypeByte()==DD.TAG_AC12) { //Fragment
			if(DEBUG)System.out.println("UDPServer:run: Fragment received, will decode");
			UDPFragment frag = new UDPFragment();
			try {
				frag.decode(dec);
				if(DEBUG)System.out.println("UDPServer:run: receives fragment "+frag.sequence+"/"+frag.fragments+" from:"+pak.getSocketAddress());
				msg = us.getFragment(frag, pak.getSocketAddress());
				if(DEBUG)System.out.println("UDPServer:run: Fragment received: "+frag);
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(msg == null) {
				if(DEBUG)System.out.println("UDPServer:run: Fragment not ready");
				return;
			}
			dec = new Decoder(msg);
			if(DEBUG)System.out.println("UDPServer:run: Continuing to decode message!");
		}else{
			//System.out.println("Packet received not fragment");
			if(dec.getTypeByte()==DD.TAG_AC11) { //Fragment
				UDPFragmentAck frag = new UDPFragmentAck();
				try {
					frag.decode(dec);
					if(DEBUG)System.out.println("UDPServer:run: Packet received is fragment "+frag.msgID+" ack from: "+pak.getSocketAddress());
					us.getFragmentAck(frag);
				} catch (ASN1DecoderFail e) {
					e.printStackTrace();
				}
				return;
			}else{
				if(dec.getTypeByte()==DD.TAG_AC15){
					UDPFragmentNAck frag = new UDPFragmentNAck();
					try {
						frag.decode(dec);
						if(DEBUG)System.out.println("UDPServer:run: Packet received is fragment "+frag.msgID+" nack from: "+pak.getSocketAddress());
						us.getFragmentNAck(frag);
					} catch (ASN1DecoderFail e) {
						e.printStackTrace();
					}
					return;
					
				}else{
				  if(dec.getTypeByte()==DD.TAG_AC16) {
					if(DEBUG)System.out.println("UDPServer:run: Packet received is fragments reclaim");
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
		if(DEBUG)System.out.println("UDPServer:run: Message received will be interpreted");
		if(dec.getTypeByte()==DD.TAG_AC13) {
		 ASNUDPPing aup = new ASNUDPPing();
		 try { // check if this is a PING
			if(DEBUG)System.out.println("UDPServer:run: UDPServer receives ping from:"+pak.getSocketAddress());
			if(DEBUG)System.out.println("UDPServer:run: UDPServer attempts decoding Ping");
			aup.decode(dec);
			if(DEBUG)System.out.println("UDPServer:run: receives: "+aup+" from:"+pak.getSocketAddress());
			if(aup.senderIsPeer) {
				//if data is a ping from a contacted peer
				// than send a request to that peer
			  String g_peerID = aup.peer_globalID;
			  synchronized(us.lock_reply){
				if(!Application.aus.hasSyncRequests(g_peerID)) {
					DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Received ping confirmation already handled from peer"));
					if(DEBUG)System.out.println("UDPServer:run: Ping already handled for: "+Util.trimmed(g_peerID));
					return;					
				}
				DD.ed.fireClientUpdate(new CommEvent(this, null, null, "LOCAL", "Received ping confirmation from peer"));
				Application.aus.delSyncRequests(g_peerID);
				//get last snapshoot for peerID
				String _lastSnapshotString;
				try {
					_lastSnapshotString = Client.getLastSnapshot(g_peerID);
				} catch (SQLiteException e1) {
					e1.printStackTrace();
					return;
				}
				if(UDPServer.transferringPeerMessage(g_peerID)){
					if(DEBUG)System.out.println("UDPServer:run: Request being sent for: "+Util.trimmed(g_peerID));
					return;
				}
				if(DEBUG)System.out.println("UDPServer:run: will build request! "+Util.trimmed(g_peerID)+" ... time="+_lastSnapshotString);
				ArrayList<Object> peer = D_PeerAddress.getD_PeerAddress_Info(g_peerID);
				String peer_ID = Util.getString(peer.get(table.peer.PEER_COL_ID));
				String _filtered = Util.getString(peer.get(table.peer.PEER_COL_FILTERED));
				boolean filtered = Util.stringInt2bool(_filtered, false);
				ASNSyncRequest asreq = Client.buildRequest(_lastSnapshotString, null, peer_ID);
				if(filtered) asreq.orgFilter=UpdateMessages.getOrgFilter(peer_ID);
				SK sk = asreq.sign();
				
				ciphersuits.PK _pk=null;
				if(DD.VERIFY_SENT_SIGNATURES){
					try {
						_pk = ciphersuits.Cipher.getPK(asreq.address.globalID);
						if(!Cipher.isPair(sk,_pk)){
							Util.printCallPath("Unmatched keys undecoded");
						}
					}catch(Exception e){e.printStackTrace();}
					boolean r = asreq.verifySignature();
					if(!r) Util.printCallPath("failed verifying: "+asreq);
					else
						if(DEBUG)System.out.println("UDPServThread: _run: signature success");
				}
				
				byte[] buf = asreq.encode();
				if(DD.VERIFY_SENT_SIGNATURES) {
					Decoder d = new Decoder(buf);
					ASNSyncRequest asr = new ASNSyncRequest();
					asr.decode(d);
					//if(DEBUG)System.out.println("UDPServer: Received request from: "+psa);
					if(DEBUG)System.out.println("UDPServer: verif sent Decoded request: "+asr.toSummaryString());

					ciphersuits.PK pk;
					try {
						pk = ciphersuits.Cipher.getPK(asr.address.globalID);
						if(!Cipher.isPair(sk,pk)){
							Util.printCallPath("Unmatched keys");
							System.out.println("UDPServThread: _run: pk ="+pk);
							System.out.println("UDPServThread: _run: _pk ="+_pk);
							System.out.println("UDPServThread: _run: pk_eq ="+pk.equals(_pk));
						}
					}catch(Exception e){e.printStackTrace();}
					if(!asr.verifySignature()) {
						//DD.ed.fireServerUpdate(new CommEvent(this, null, psa, "UDPServer", "Unsigned Sync Request received: "+asr));
						System.err.println("UDPServer:run: Unsigned Request sent: "+asr.toSummaryString());
						System.err.println("UDPServer:run: Unsigned Request rsent: "+asr.toString());
						System.err.println("UDPServer:run: Unsigned Request old: "+asreq.toString());
						return;
					}					
				}
				SocketAddress rsa = new InetSocketAddress(aup.peer_domain,aup.peer_port);
				try {
					//us.ds.send(reqDP);
					us.sendLargeMessage(rsa, buf, DD.MTU, g_peerID, DD.MSGTYPE_SyncRequest);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				if(DEBUG) {
					String slogan=null;
					if(asreq.address!=null) slogan = asreq.address.slogan;
					if(DEBUG)System.out.println("UDPServer:run: sends request: "+slogan+" to: "+rsa);
					if(DEBUG)System.out.println("UDPServer:run: sends request: "+asreq+" to: "+rsa);
				}
			  }
			}else{
				//if data is from directory server,
				// then ping the peer willing to contact me
				if(!aup.senderIsInitiator) {
					if(Application.ld!=null)
						Application.ld.setNATOn(peer_address, new Boolean(true));
				}
				if(DEBUG)System.out.println("UDPServer:run: receives forwarded ping from directory/initiator! "+aup);
				if(Identity.current_peer_ID.globalID.equals(aup.initiator_globalID)){
					if(DEBUG)System.out.println("UDPServer:run: Ping received from myself! Abandon.");
					return;
				}
				aup.senderIsPeer=true;
				InetSocketAddress next_dest;
				String peer_ID = Peers.getPeerID(aup.initiator_globalID);
				if(aup.senderIsInitiator==true){
					aup.senderIsInitiator=false;
					next_dest = (InetSocketAddress) pak.getSocketAddress();
					Application.peers.setConnectionState(peer_ID, Peers.STATE_CONNECTION_UDP_NAT);
				}else{
					next_dest = new InetSocketAddress(aup.initiator_domain,aup.initiator_port);
					Application.peers.setConnectionState(peer_ID, Peers.STATE_CONNECTION_UDP);
				}
				byte[] buf = aup.encode();
				DatagramPacket reqDP = new DatagramPacket(buf, buf.length);
				reqDP.setSocketAddress(next_dest);
				try {
					us.ds.send(reqDP);
					if(DEBUG)System.out.println("UDPServer:run: ping returned to initiator: "+next_dest+" ping="+aup);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}			
			
			if(DEBUG)System.out.println("UDPServer:run: UDPServer pinged on request: "+aup);
		  } catch (ASN1DecoderFail e) {
			  if(DEBUG)System.out.println("Ping decoding failed! "+e);
		  } catch (SQLiteException e) {
			  if(DEBUG)System.out.println("Database access failed! "+e);
			e.printStackTrace();
		}
		  return;
		}
		
		if(dec.getTypeByte()==DD.TAG_AC14) {
		  if(DEBUG)System.out.println("UDPServer:run: Announcement answer decoding!");
		  try{
			Decoder answer_dec=dec;
			DA_Answer daa=new DA_Answer();
			daa.decode(answer_dec);
			if(DEBUG) out.println("UDPServer: Directory Answer: "+daa.result);
			if(Application.ld!=null)
				Application.ld.setUDPOn(peer_address, new Boolean(true));
		  }catch(Exception e){
			  if(DEBUG)System.out.println("UDPServer: UDP Announcement answer decoding failed! "+e);
			  if(Application.ld!=null)
					Application.ld.setUDPOn(peer_address, new Boolean(false));
		  }
		  return;
		}
		
		if(dec.getTypeByte()==DD.TAG_AC7) {
		  ASNSyncRequest asr = new ASNSyncRequest();
		  if(DEBUG)System.out.println("\n*******************\nUDPServer:run: Trying to decode request!");
		  try { // This is not a ping. Check if it is a request
			SocketAddress psa = pak.getSocketAddress();
			asr.decode(dec);
			if(DEBUG)System.out.println("UDPServer: Received request from: "+psa);
			if(_DEBUG)System.out.println("UDPServer: Decoded request: "+asr.toSummaryString()+" from: "+psa);
			if(!DD.ACCEPT_STREAMING_REQUEST_UNSIGNED && !asr.verifySignature()) {
				DD.ed.fireServerUpdate(new CommEvent(this, null, psa, "UDPServer", "Unsigned Sync Request received: "+asr));
				System.err.println("UDPServer:run: Unsigned Request received: "+asr.toSummaryString());
				System.err.println("UDPServer:run: Unsigned Request received: "+asr.toString());
				return;
			}
			
			Server.extractDataSyncRequest(asr, psa, this);
			if(DEBUG)System.out.println("UDPServer:run: Request Data extracted *************************");

			String peerGID = null;
			if(asr.address!=null) peerGID = asr.address.globalID;
			else{
				if(DEBUG)System.out.println("UDPServer:run: request from UNKNOWN abandoned");
				if(DEBUG)System.out.println("UDPServer:run: Answer not sent!");
				return;			
			}
			
			if(UDPServer.transferringPeerAnswerMessage(peerGID)){
				if(DEBUG)System.out.println("UDPServer:run: UDPServer Answer being sent for: "+Util.trimmed(peerGID));
				return;
			}
			String peer_ID = table.peer.getLocalPeerID(peerGID);
			//D_PluginInfo.recordPluginInfo(asr.plugin_info, peerGID, peer_ID);
			if(Application.peers!=null) Application.peers.setConnectionState(peer_ID, Peers.STATE_CONNECTION_UDP);
			
			//if data is a request from a peer
			// then send an answer to that peer
			SyncAnswer sa = UpdateMessages.buildAnswer(asr, peer_ID);
			if(DEBUG)System.out.println("UDPServer:run: Prepared answer!");
			//System.out.println("Prepared answer: "+Util.trimmed(sa.toString()));
			byte[]sa_msg = sa.encode();
			us.sendLargeMessage(psa, sa_msg, DD.MTU, peerGID, DD.MSGTYPE_SyncAnswer);
			if(_DEBUG)System.out.println("UDPServer:run: Answer sent! "+sa.toSummaryString());
			//System.out.println("Answer sent: "+Util.trimmed(sa.toString(),Util.MAX_UPDATE_DUMP));
		  } catch (ASN1DecoderFail e) {
			e.printStackTrace();
		  } catch (SQLiteException e) {
			e.printStackTrace();
		  } catch (IOException e) {
			e.printStackTrace();
		  } catch (Exception e) {
			e.printStackTrace();
		  }
		  return;
		}

		if(dec.getTypeByte()==DD.TAG_AC8) {
		  if(_DEBUG)System.out.println("\n*************\nUDPServer:run: Answer received fully from "+pak.getSocketAddress());
		  SyncAnswer sa = new SyncAnswer();
		  try {
			sa.decode(dec);
			// System.out.println("Answer received is: "+Util.trimmed(sa.toString(),Util.MAX_UPDATE_DUMP));
			if(DEBUG)System.out.println("UDPServer:run: Answer received & decoded from: "+pak.getSocketAddress());
			if(_DEBUG)System.out.println("UDPServer:run: Answer received is: "+sa.toSummaryString());
			// integrate answer
			//int len = dec.getMSGLength();
			String global_peer_ID = sa.responderID;
			D_PeerAddress peer =null;
			String peer_ID = null;
			boolean blocked[] = new boolean[]{true};
			if(global_peer_ID!=null) {
				peer = new D_PeerAddress(global_peer_ID, false, false, true);
				peer_ID = peer.peer_ID; //table.peer.getLocalPeerID(global_peer_ID);
				blocked[0] = peer.blocked;
			}
			//String peer_ID = table.peer.getLocalPeerID(global_peer_ID, blocked);
			if((peer_ID!=null)&&blocked[0]) return;
			if(peer_ID == null) {
				if(_DEBUG)System.out.println("UDPServer:run: Answer received from unknown peer: "+global_peer_ID);
				
			} else
				if(Application.peers!=null) Application.peers.setConnectionState(peer_ID, Peers.STATE_CONNECTION_UDP);
			
			//System.out.println("Got msg size: "+len);//+"  bytes: "+Util.byteToHex(update, 0, len, " "));
			if(DEBUG)System.out.println("UDPServer:run: Answer received will be integrated");
			InetSocketAddress saddr = (InetSocketAddress)pak.getSocketAddress();
			String address_ID = null;
			if(peer_ID != null) address_ID = D_PeerAddress.getAddressID(saddr, peer_ID);
			RequestData rq = new RequestData();
			if((UpdateMessages.integrateUpdate(sa, saddr, this, global_peer_ID, peer_ID, address_ID, rq, peer))||
					((rq!=null) && !rq.empty())){
				if(DEBUG) System.out.println("UDPServer:run: Should reply with request for: "+rq);
				DD.touchClient();
			}
			if(DEBUG)System.out.println("UDPServer:run: Answer received and integrated "+pak.getSocketAddress());
			if(DEBUG)if((rq!=null) && !rq.empty())System.out.println("UDPServer:run: Should reply with request for: "+rq);
		  } catch (ASN1DecoderFail e) {
			e.printStackTrace();
		  } catch (SQLiteException e) {
			e.printStackTrace();
		  } 
		  return;
		}
		if(DEBUG)System.out.println("Unknown message["+pak.getLength()+"]: "+Util.byteToHexDump(buffer));
		if(msg!=null)if(DEBUG)System.out.println(" msg["+msg.length+"]="+Util.byteToHexDump(msg));
		return;
	}
	
}