/*   Copyright (C) 2012 
		Author: Osamah Dhannoon
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
package net.ddp2p.common.wireless;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.handling_wb.BroadcastQueues;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class BroadcastClient extends net.ddp2p.common.util.DDP2P_ServiceThread {
	private static final int CLIENT_TIMEOUT_MILLISECONDS = 0;
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public static int MAX_RUNS = 1;
	public static Object msgs_monitor = new Object();
	final static Object run_monitor = new Object();
	public static long startTime;
	public static int counter = 0;
	public static long msg_cnter = 0;
	InetAddress[] addresses;
	static boolean m_running = true;
	public static BroadcastQueues msgs = null; 
	public static DatagramSocket[] broadcast_client_sockets;
	int c = -1,x=-1;
	Refresh START_REFRESH;
	public BroadcastClient() throws SocketException, P2PDDSQLException {
		super ("Broadcast Client", false);
		synchronized(msgs_monitor) {
			msgs = new BroadcastQueues();
			try {
				String interests = DD.getAppText(DD.WLAN_INTERESTS);
				if(interests == null) return;
				byte[] wlan_interests = Util.byteSignatureFromString(interests);
				RequestData rq;
				rq = new RequestData().decode(new net.ddp2p.ASN1.Decoder(wlan_interests));
				msgs.registerRequest(rq);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public BroadcastData updateSockets(BroadcastData old_bcs) {
		BroadcastData BD = old_bcs;
		synchronized(BroadcastServer.semaphore_interfaces){
			if(!BroadcastServer.client_address_updated) return old_bcs;
			if(BroadcastServer.client_address_updated_clear()){
				addresses=BroadcastServer.load_client_addresses();
				if(DEBUG)System.out.println("BroadcastClient:updateSockets:addresses len : "+addresses.length);
				BD.bcs = new DatagramSocket[addresses.length];
				BD.Interfaces_names = new String[addresses.length];
				for(int i=0; i<addresses.length; i++) {
					if(DEBUG)System.out.println("BClient: create socket: "+addresses[i]);
					if(!m_running) break;
					try {
						if(DEBUG)System.out.println("BClient: Handling broad interface: ["+i+"]="+addresses[i]);
						int port = 0; 
						String iP_Mask = BroadcastServer.interfaces_IP_Masks.get(i);
						if(DEBUG)System.out.println("BClient : interfaces_names["+i+"] "+BroadcastServer.interfaces_names.get(i));
						BD.Interfaces_names[i] = BroadcastServer.interfaces_names.get(i);
						if(DEBUG)System.out.println("BClient: BD.Interfaces_names["+i+"] :"+BD.Interfaces_names[i]); 
						if(DEBUG)System.out.println("BClient : IP: "+iP_Mask);
						InetAddress ia;
						try {
							byte[] adr = Util.getInetAddressBytes_from_IP_Mask(iP_Mask);
							if(adr==null){
								Application_GUI.warning(Util.__("Trouble retrieving interface:")+addresses[i], Util.__("Cannot use interface"));
								continue;								
							}
							ia = InetAddress.getByAddress(adr);
						} catch (UnknownHostException e) {
							e.printStackTrace();
							Application_GUI.warning(Util.__("Trouble retrieving interface:")+e.getLocalizedMessage(), Util.__("Cannot use interface"));
							continue;
						}
						if(DEBUG)System.out.println("BClient: Handling ip interface: ["+i+"]="+ia);
						BD.bcs[i] = new DatagramSocket(port, ia);
						BD.bcs[i].setSoTimeout(CLIENT_TIMEOUT_MILLISECONDS);
						try {
							BD.bcs[i].connect(InetAddress.getByAddress(Util.getBytesFromCleanIPString(DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP)), BroadcastServer.BROADCAST_SERVER_PORT);
							if(_DEBUG)System.out.println(DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP);
						} catch (Exception e) {
							e.printStackTrace();
						}
						BD.bcs[i].setBroadcast(true);
						BD.bcs[i].setTrafficClass(0x10);
					} catch (SocketException e) {
						e.printStackTrace();
						Application_GUI.warning(e.getLocalizedMessage(), Util.__("Failure to get interface"));
					}
				}
			}
		}
		return BD;
	}
	/**
	 * Stop the client thread by setting a boolean to false
	 */
	public static void stopClient() {
		if(_DEBUG)System.out.println("Client Stop");
		net.ddp2p.common.handling_wb.BroadcastQueueRequested.stopThread=true;
		synchronized (run_monitor){
			m_running = false;
			run_monitor.notifyAll();
		}		
		DD.START_REFRESH.START_REFRESH = false;
	}
	public void _run() {
		boolean DEBUG=false;
		if(DEBUG)System.out.println("Start time is : "+System.currentTimeMillis());
		BroadcastData _BD = new BroadcastData();
		_BD.bcs = new DatagramSocket[0];
		_BD.Interfaces_names = new String[0];
		broadcast_client_sockets = _BD.bcs;
		if ((DD.OS==DD.WINDOWS) && DD.ADHOC_WINDOWS_DD_CONTINUOUS_REFRESH) {
			DD.START_REFRESH = START_REFRESH = new Refresh();
			START_REFRESH.start();
		}
		long inc = 0;
		long start = -1;
		long timeout = 6000;
		boolean check = false;
		for(int i=0;;i++) {
			synchronized (run_monitor){
				if(!m_running){
					try {
						if(DEBUG)System.out.println("BroadcastClient:run: wait monitor, to run");
						run_monitor.wait();
						if(DEBUG)System.out.println("BroadcastClient:run: waked monitor to run");
					} catch (InterruptedException e) {
						e.printStackTrace();
						if(START_REFRESH != null){
							START_REFRESH.START_REFRESH = false;
							DD.START_REFRESH = null;
						}
						if(_DEBUG)System.out.println("BroadcastClient:run: exit interrupt");
						return;						
					}
					continue;
				}				
			}
			counter++;
			_BD = updateSockets(_BD);
			broadcast_client_sockets = _BD.bcs;
			Application_GUI.update_broadcast_client_sockets(null);
			if ((DD.OS==DD.WINDOWS) && (DD.ADHOC_DD_IP_WINDOWS_DETECTED_ON_EACH_SEND))
				broadcast_client_sockets = Win_wlan_info.extractValidIPs(_BD);
			if(broadcast_client_sockets.length<=0){
				synchronized(BroadcastServer.semaphore_interfaces){
					try {
						if(DEBUG)System.out.println("BroadcastClient:run: wait monitor, interf");
						BroadcastServer.semaphore_interfaces.wait(DD.ADHOC_EMPTY_TIMEOUT_MILLISECONDS);
						if(DEBUG)System.out.println("BroadcastClient:run: waked monitor interf");
					} catch (InterruptedException e) {
						e.printStackTrace();
						if(START_REFRESH != null){
							START_REFRESH.START_REFRESH = false;
							DD.START_REFRESH = null;
						}
						if(_DEBUG)System.out.println("BroadcastClient:run: exit interrupt interf");
						return;
					}
				}
				continue;
			}
			byte[] _crt =null;
			msg_cnter++;
			_crt = msgs.getNext(msg_cnter);
			if(_crt == null){
				if(DEBUG)System.out.println("BroadcastClient: MESSAGE NULL ");
				msg_cnter--;
				continue;
			}
			byte[] crt = appendCounter(_crt);
			DatagramPacket dp = new DatagramPacket(crt, crt.length);
			Calendar cal = Util.CalendargetInstance();
			if(DEBUG)System.out.println(cal.get(Calendar.MINUTE));
			if(
					(DD.ADHOC_SENDER_SLEEP_MINUTE_START_LONG_SLEEP>1) &&
					(DD.ADHOC_SENDER_SLEEP_SECONDS_DURATION_LONG_SLEEP>0) &&
					(cal.get(Calendar.MINUTE)%DD.ADHOC_SENDER_SLEEP_MINUTE_START_LONG_SLEEP == 0) &&
					(cal.get(Calendar.SECOND)<5)
					) {
				try {
					if(_DEBUG)System.out.println("Sleep");
					x++;
					Thread.sleep(DD.ADHOC_SENDER_SLEEP_SECONDS_DURATION_LONG_SLEEP*1000);
					if(x==7) System.exit(0);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				if(DD.ADHOC_SENDER_SLEEP_MILLISECONDS!=0) {
					try {
						Thread.sleep(DD.ADHOC_SENDER_SLEEP_MILLISECONDS);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
			if(DEBUG)System.out.println("BClient: sending to #"+broadcast_client_sockets.length);
			for(int j=0; j<broadcast_client_sockets.length; j++){
				if(broadcast_client_sockets[j]==null) {
					if(DEBUG)System.out.println("sending: null bcs "+j);
					continue;
				}
				try {
					try{
						if(DEBUG)System.out.println("sending: ["+j+"] from ="+broadcast_client_sockets[j].getLocalSocketAddress());
						if(DEBUG)System.out.println("sending: ["+j+"] to ="+broadcast_client_sockets[j].getRemoteSocketAddress());
						if(DEBUG)System.out.println("sending: "+dp);
					}catch(Exception e) {e.printStackTrace();}
					dp.setSocketAddress(broadcast_client_sockets[j].getRemoteSocketAddress()); 
					try{
						if(DEBUG)System.out.println("sending: ["+j+"] from ="+broadcast_client_sockets[j].getLocalSocketAddress());
						if(DEBUG)System.out.println("sending: ["+j+"] to ="+broadcast_client_sockets[j].getRemoteSocketAddress());
					}catch(Exception e) {e.printStackTrace();}
					broadcast_client_sockets[j].send(dp);
					Application_GUI.update_broadcast_client_sockets(msg_cnter);
					if(DEBUG)System.out.println("BroadcastClient: MESSAGE hash : "+Util.stringSignatureFromByte(Util.simple_hash(dp.getData(), DD.APP_INSECURE_HASH)));
					if(DEBUG)System.out.println("sending : "+(++c));
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(DEBUG)System.out.println("BClient: sending DONE");
			}
		}
	}
	public byte[] appendCounter(byte[] original) {
		byte[] cnter = new byte[8];
		ByteBuffer buf = ByteBuffer.wrap(cnter);  
		buf.putLong(msg_cnter);
		byte[] crt = new byte[original.length+cnter.length];
		System.arraycopy(original, 0, crt, 0, original.length);
		System.arraycopy(cnter,0,crt,original.length,cnter.length);
		return crt;
	}
	public static void main (String args[]) {
		DatagramSocket ds;
		try {
			SocketAddress sa = new InetSocketAddress(args[0], 0);
			ds = new DatagramSocket(sa);
			int port = Integer.parseInt(args[2]);
			SocketAddress ba = new InetSocketAddress(args[1], port);
			ds.setBroadcast(true);
			int len = 9000;
			int cnt=0;
			DatagramPacket dp = new DatagramPacket(new byte[len], len, ba);
			for (;;){
				ds.send(dp);
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
