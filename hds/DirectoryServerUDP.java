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
import java.net.SocketTimeoutException;
import com.almworks.sqlite4java.SQLiteException;

import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;

//TODO: Make it a concurrent server for announcements and let it iterative for pings
public class DirectoryServerUDP extends Thread {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	DirectoryServer ds;
	boolean turnOff=false;

	public DirectoryServerUDP(DirectoryServer _ds) {
		ds = _ds;
	}
	
	public void turnOff(){
		turnOff=true;
		this.interrupt();
		out.println("Turning DS UDP off");
	}
	
	public void run () {
		if(DEBUG) out.println("Enter DS UDP Server Thread");
		byte buffer[]=new byte[DirectoryServer.MAX_DR];
		for(;;) {
			if(turnOff){
				out.println("Turned off");
				break;
			}
			if(DEBUG) out.print("*");
			DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
			int peek=0;
			try{
				ds.udp_socket.receive(dp);
				if(DEBUG) out.println("DS UDP Accepted...");
			}catch(SocketTimeoutException e2){
				continue;
			}catch(IOException e){
				continue;
			}
			InetSocketAddress risa = (InetSocketAddress)dp.getSocketAddress();
			if(DEBUG) out.print("(UDP: "+risa+")");
			Decoder dec=new Decoder(buffer,0,dp.getLength());
			if(dec.getTypeByte() == DD.MSGTYPE_EmptyPing){
				if(DEBUG) out.println("DS UDP EmptyPing");
				if(DEBUG) out.print("^");
				continue;
			}
			
			// Rest should be concurrent ... any taker?
			
			if(DEBUG) out.println("DirectoryServerUDP: UDP Decoded: class="+dec.typeClass()+" val="+dec.tagVal());
			if(dec.typeClass()==Encoder.CLASS_APPLICATION && dec.tagVal()==DirectoryAnnouncement.TAG) {
				DirectoryAnnouncement da;
				try {
					da = new DirectoryAnnouncement(dec);
				} catch (ASN1DecoderFail e1) {
					continue;
				}
				if(DEBUG)out.println("DirectoryServerUDP: Received UDP announcement: "+da+"\n from: "+risa);
				String detected_sa = DirectoryServer.detectUDPAddress(risa, risa.getPort());
						// da.address.port);
				byte[] answer;
				try {
					answer = DirectoryServer.handleAnnouncement(da, detected_sa, ds.db);
				} catch (SQLiteException e) {
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
			}else{
				if(DEBUG) out.println("DirectoryServerUDP:run Detected ping request");
				ASNUDPPing aup = new ASNUDPPing();
				try { // check if this is a PING
					aup.decode(dec);
					if(DEBUG) System.out.println("DirectoryServerUDP:run: receives: "+aup);
					if(!aup.senderIsInitiator){
						if(DEBUG) System.out.println("DirectoryServerUDP:run: sender is not initiator => DROP");
						continue;
					}
					aup.senderIsInitiator=false;
					aup.senderIsPeer=false;
					aup.initiator_domain=dp.getAddress().getHostAddress();
					aup.initiator_port=dp.getPort();
					if(aup.peer_port<=0){
						System.err.println("DirectoryServerUDP:run:  receives port: "+aup.peer_port);
						continue;
					}
					InetSocketAddress isa;
					try{
						isa = new InetSocketAddress(aup.peer_domain,aup.peer_port);
					}catch(Exception e){e.printStackTrace(); continue;}
					if(isa.isUnresolved()) continue;
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
			}
		}
	}
}
