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
import hds.DirectoryServerCache.D_DirectoryEntry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import util.P2PDDSQLException;
import config.DD;
import data.D_Peer;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

//TODO: Make it a concurrent server for announcements and let it iterative for pings
public class DirectoryServerUDP extends util.DDP2P_ServiceThread {
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	DirectoryServer ds;
	boolean turnOff=false;
//
//	Hashtable<String, ArrayList<Message>> noping_storage;
//	Hashtable<String, ArrayList<Message>> ping_storage;
//	Hashtable<String, ArrayList<Message>> announcement_storage;
//	Hashtable<String, Terms> last_terms;
//	
	public DirectoryServerUDP(DirectoryServer _ds) {
		super("Directory Server UDP", false);
		ds = _ds;
	}
	
	public void turnOff() {
		turnOff=true;
		this.interrupt();
		out.println("Turning DS UDP off");
	}
	
	public void _run () {
		if(DEBUG) out.println("Enter DS UDP Server Thread");
		byte buffer[] = new byte[DirectoryServer.MAX_DR];
		for(;;) {
			if (turnOff) {
				out.println("Turned off");
				break;
			}
			if(DEBUG) out.print("*");
			DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
			int peek = 0;
			try {
				ds.udp_socket.receive(dp);
				if (DEBUG) out.println("DS UDP Accepted...");
			} catch (SocketTimeoutException e2) {
				continue;
			}catch(IOException e){
				continue;
			}
			DirMessage m;
			InetSocketAddress risa = null;
			try {
				risa = (InetSocketAddress)dp.getSocketAddress();
				// "50.88.84.239"
				if (DirectoryServer.mAcceptedIPs.size() > 0) {
					boolean accepted = false;
					for ( String ip : DirectoryServer.mAcceptedIPs) {
						InetAddress _isa = new InetSocketAddress(ip, 45000).getAddress();
						if (DEBUG) out.print("(UDP: "+risa+")");
						if (risa.getAddress().equals(_isa)) {
							if (DEBUG) out.print("(UDP: from accepted "+_isa+")");
							accepted = true;
							break;
						} else {
							if (_DEBUG) out.print("(UDP: not from "+_isa+" but "+risa+")");
						}
					}
					if (! accepted) return;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (risa == null) continue;
			
			Decoder dec=new Decoder(buffer,0,dp.getLength());
			if (dec.getTypeByte() == DD.MSGTYPE_EmptyPing){
				if (DEBUG) out.println("DS UDP EmptyPing");
				if (_DEBUG) out.print("^"+dp.getSocketAddress()+"^");
				DirectoryServer.recordPingMessage(risa, null, DirMessage.UDP, DirMessage.EMPTY_PING);
				continue;
			}
			
			// Rest should be concurrent ... any taker?
			
			if (DEBUG) out.println("DirectoryServerUDP: UDP Decoded: class="+dec.typeClass()+" val="+dec.tagVal());
			if (dec.typeClass()==Encoder.CLASS_APPLICATION && dec.tagVal()==DirectoryAnnouncement.TAG) {
				DirectoryAnnouncement da;
				try {
					da = new DirectoryAnnouncement(dec);
				} catch (ASN1DecoderFail e1) {
					e1.printStackTrace();
					continue;
				}
				if(_DEBUG)out.println("\n\nDirectoryServerUDP: Received UDP announcement: "+da.toSummaryString()+"\n from: "+risa+"\n");

				DirectoryServer.recordAnnouncementMessage(risa, da, null, DirMessage.UDP, DirMessage.ANNOUNCEMENT);

				Address detected_sa = DirectoryServer.detectUDP_Address(risa, risa.getPort());
				if (DEBUG)out.println("DirectoryServerUDP: UDP announcement: detected = "+detected_sa);
				detected_sa = DirectoryServer.addr_NAT_detection(da, detected_sa);
				if (_DEBUG)out.println("DirectoryServerUDP: UDP announcement: detected tuned = "+detected_sa);
				byte[] answer = new byte[0];
				try {
					DirectoryAnnouncement_Answer daa = DirectoryServer.handleAnnouncement(da, detected_sa, DirectoryServer.db_dir, true, false);
					if (daa != null) answer = daa.encode(); 
					DirectoryServer.recordAnnouncementMessage(risa, da, daa, DirMessage.UDP, DirMessage.ANN_ANSWER);
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
					continue;
				}
				DatagramPacket ans = new DatagramPacket(answer, answer.length);
				ans.setSocketAddress(dp.getSocketAddress());
				try {
					ds.udp_socket.send(ans);
					if(DEBUG) System.out.println("DirectoryServerUDP: answers announcement");
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
				continue;
			} else {
				if (dec.getTypeByte() == ASNUDPPing.getASN1Tag()) {
					if (DEBUG) out.println("\nDirectoryServerUDP:run Detected ping request");
					ASNUDPPing aup = new ASNUDPPing();
					try { // check if this is a PING
						aup.decode(dec);
						if (DEBUG) System.out.println("\nDirectoryServerUDP:run: receives: "+aup);
						if (_DEBUG) System.out.println("\nDirectoryServerUDP:run: ping req from: "+dp.getSocketAddress());
						if (!aup.senderIsInitiator) {
							if(_DEBUG) System.out.println("DirectoryServerUDP:run: sender is not initiator => DROP");
							continue;
						}
						DirectoryServer.recordPingMessage((InetSocketAddress)dp.getSocketAddress(), aup, DirMessage.UDP, DirMessage.PING);
						aup.senderIsInitiator=false;
						aup.senderIsPeer=false;
						aup.initiator_domain=dp.getAddress().getHostAddress();
						aup.initiator_port=dp.getPort();
						if (aup.peer_port <= 0) {
							System.err.println("DirectoryServerUDP:run:  receives port: "+aup.peer_port);
							continue;
						}
						InetSocketAddress isa;
						try {
							isa = new InetSocketAddress(aup.peer_domain,aup.peer_port);
						} catch(Exception e){e.printStackTrace(); continue;}
						if(isa.isUnresolved()){
							System.err.println("DirectoryServerUDP:run:  unresolved: "+isa);
							continue;
						}
						byte[] answer = aup.encode();
						DatagramPacket ans = new DatagramPacket(answer, answer.length);
						ans.setSocketAddress(isa);
						if(DEBUG) System.out.println("\nDirectoryServerUDP:run: DirectoryServerUDP:  forwards: "+aup+" to: "+isa);
						int localPort = -1;
						if ((config.Application.ds != null))
							if(config.Application.ds.udp_socket!=null)
								localPort = config.Application.ds.udp_socket.getLocalPort();
						try {
							if(true || (aup.peer_port != localPort)) {
								ds.udp_socket.send(ans);
								if(DEBUG) System.out.println("DirectoryServerUDP:run: ping forwarded to: "+ans.getSocketAddress()+" port:"+aup.peer_port+" from port: "+localPort);
							}else
								if(DEBUG) System.out.println("DirectoryServerUDP:run: believe it is myself -> ping not forwarded to: "+ans.getSocketAddress());
						} catch (IOException e) {
							e.printStackTrace();
						}
					} catch (ASN1DecoderFail e) {
						e.printStackTrace();
					}		
				} else {
					if (dec.getTypeByte() == hds.DirectoryRequest.getASN1Tag()) {
						//boolean DEBUG = true;
						if (DEBUG) out.println("DirServUDP: Received directory request");
						// handling terms here
						DirectoryRequest dr = new DirectoryRequest(dec);//buffer,peek,client.getInputStream());
						//boolean acceptedTerms = areTermsAccepted(dr);
						InetSocketAddress isa= risa;//(InetSocketAddress)client.getRemoteSocketAddress();
						if (DEBUG) out.println("DirServUDP: Received directory request: "+dr);

						DirectoryServer.recordRequestMessage(isa, dr, null, DirMessage.UDP, DirMessage.REQUEST);
						if (DEBUG) out.println("DirServUDP: Looking for: "+
								D_Peer.getGIDHashFromGID(dr.globalID)+"\n  by "+
								D_Peer.getGIDHashFromGID(dr.initiator_globalID));//+"\n  with source udp="+dr.UDP_port);

						String globalID = dr.globalID; // looking for peer GID
						String globalIDhash = dr.globalIDhash; // looking for peer GID hash
						// de has the look-for-peer and all instances stored in the db
						D_DirectoryEntry de = DirectoryServerCache.getEntry(globalID, globalIDhash);
						if (DEBUG) out.println("DirServUDP: From cache got: "+de);
						ASNObj da = DirectoryServer.getDA(de, dr, dr.version);

						DirectoryServer.recordRequestMessage(isa, dr, da, DirMessage.UDP, DirMessage.REQUEST_ANSWER);

						byte answer[] = da.encode();
						if (DEBUG) {
							Decoder deco = new Decoder(answer);
							DirectoryAnswerMultipleIdentities dami = null;
							try {
								dami = new DirectoryAnswerMultipleIdentities(deco);
							} catch (ASN1DecoderFail e) {
								e.printStackTrace();
							}
							System.out.println("DirectoryServerUDP:_run:encode "+da+"\nto "+dami);
						}
						//out.println("answer: "+Util.byteToHexDump(msg, " ")+"\n\tI.e.: "+da);
						/*
						if(_DEBUG&&(da.addresses.size()>0)){
							out.println("DirServ: *******");
							out.println("DirServ: Aanswer: "+client.getRemoteSocketAddress()+" <- "+da.toString());
						}
						*/
						//client.getOutputStream().write(msg);
						//byte[] answer = aup.encode();
						DatagramPacket ans = new DatagramPacket(answer, answer.length);
						ans.setSocketAddress(isa);
						try {
							ds.udp_socket.send(ans);
							if (DEBUG) System.out.println("DirectoryServerUDP:run: answer forwarded to: "+ans.getSocketAddress());
						} catch (IOException e) {
							if (DEBUG) System.out.println("DirectoryServerUDP:run: answer forwarded to: "+ans.getSocketAddress()+" for "+ans);
							e.printStackTrace();
						}
					} else {
						System.err.println("DirectoryServer: _run: unknown message type: "+dec.getTypeByte());
					}
				}
			}
		}
	}

}
