/*   Copyright (C) 2012 
		Author: Ossamah Dhannoon
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.handling_wb.ReceivedBroadcastableMessages;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
class BroadcastClientRecord {
	public SocketAddress clientAddress;
	public ByteBuffer buffer = ByteBuffer.allocate(BroadcastServer.DATAGRAM_MAX_SIZE);
}
public class BroadcastServer extends net.ddp2p.common.util.DDP2P_ServiceThread {
	public static int BROADCAST_SERVER_PORT = 54321;
	public static ArrayList<BroadcastInterface> interfaces_broadcast = new ArrayList<BroadcastInterface>();
	public static ArrayList<String> interfaces_IP_Masks = new ArrayList<String>();
	public static ArrayList<String> interfaces_names = new ArrayList<String>();
	static Object semaphore_interfaces = new Object();
	public static final int DATAGRAM_MAX_SIZE = 15000;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	static boolean client_address_updated = false;
	static boolean server_address_updated = false;
	private boolean running=true; 
	ReceivedBroadcastableMessages bm = new ReceivedBroadcastableMessages();
	private long TIMEOUT=10000; 
	Selector selector;
	public BroadcastServer() throws  IOException, P2PDDSQLException{
		super ("Broadcast Server", false);
		if(_DEBUG) System.out.println("Server START");
		if(Application.getG_BroadcastServer()!=null) throw new RuntimeException("2nd Server");
	}
	/**
	 * Test and clear the "address_updated" flag
	 * @return
	 */
	public static boolean client_address_updated_clear(){
		boolean old;
		synchronized(semaphore_interfaces){
			old = client_address_updated;
			client_address_updated = false;
		}
		return old;
	}
	/**
	 * Test and clear the "address_updated" flag
	 * @return
	 */
	public static boolean server_address_updated_clear(){
		boolean old;
		synchronized(semaphore_interfaces){
			old = server_address_updated;
			server_address_updated = false;
		}
		return old;
	}
	/**
	 * Get an array of current broadcast interfaces
	 * @return
	 */
	public static InetAddress[] load_client_addresses(){
		int  i = 0;
		synchronized(semaphore_interfaces){
			InetAddress[] result = new InetAddress[interfaces_broadcast.size()];
			for(BroadcastInterface bi: interfaces_broadcast){
				result[i++] = bi.broadcast_address;
			}
			client_address_updated = false;
			return result;
		}
	}
	/**
	 * wakes up server to reload sockets
	 * @param address
	 */
	public void addBroadcastAddress(BroadcastInterface address){
		try {
			if(selector!=null)selector.close();
		} catch (IOException e) {}
	}
	public static boolean addBroadcastAddressStatic(String iP_Mask, String Interf_name) {
		if(_DEBUG) System.out.println("BroadcastServer:addBroadcastAddressStatic: IP/Mask="+iP_Mask);
		BroadcastInterface address;
		try {
			String ipmask[] = iP_Mask.split(Pattern.quote("/"));
			if((ipmask==null) || (ipmask.length!=2)) return false;
			String IP = ipmask[0];
			String mask = ipmask[1];
			byte[] ba = Util.get_broadcastIP_from_IP_and_NETMASK(IP,mask);
			if(_DEBUG) System.out.println("BroadcastServer:addBroadcastAddressStatic: broadcast="+Util.byteToHex(ba));
			InetAddress _ba = InetAddress.getByAddress(ba);
			if(_DEBUG) System.out.println("BroadcastServer:addBroadcastAddressStatic: broadcast="+_ba);
			address = new BroadcastInterface(_ba);
		} catch (Exception e) {
			e.printStackTrace();
			Application_GUI.warning(Util.__("Error:")+e.getLocalizedMessage(), Util.__("Error parsing new IP"));
			return false;
		}
		return addBroadcastAddressStatic(address, iP_Mask, Interf_name);
	}
	public static void initAddresses() {
		synchronized(semaphore_interfaces) {
			if((interfaces_broadcast!=null)&&(interfaces_broadcast.size()!=0)){
				server_address_updated = true;
				client_address_updated = true;				
			}
			interfaces_broadcast = new ArrayList<net.ddp2p.common.wireless.BroadcastInterface>();
			interfaces_IP_Masks = new ArrayList<String>();
		}
	}
	/**
	 * Add a broadcast interface and set the "address_updated flag"
	 * @param address
	 * @param iP_Mask 
	 */
	public static boolean addBroadcastAddressStatic(BroadcastInterface address, String iP_Mask, String Interf_name){
		synchronized(semaphore_interfaces) {
			if(DEBUG)System.out.println("BroadcastServer : addBroadcastAddressStatic : address : "+address.broadcast_address);
			if(DEBUG)System.out.println("BroadcastServer : addBroadcastAddressStatic : iP_Mask : "+iP_Mask);			
			if(DEBUG)System.out.println("BroadcastServer : addBroadcastAddressStatic : Int_name : "+Interf_name);
			interfaces_broadcast.add(address);
			interfaces_IP_Masks.add(iP_Mask);
			interfaces_names.add(Interf_name);
			server_address_updated = true;
			client_address_updated = true;
			semaphore_interfaces.notifyAll();
			if(Application.getG_BroadcastServer()==null) return false;
			Application.getG_BroadcastServer().addBroadcastAddress(address);
			return true;
		}
	}
	/**
	 * Del a broadcast interface and set the "address_updated flag"
	 * @param address
	 */
	public void delBroadcastAddress(BroadcastInterface address){
		try {
			if(selector!=null)selector.close();
		} catch (IOException e) {}
	}
	/**
	 * Del a broadcast interface and set the "address_updated flag"
	 * @param address
	 */
	public static void delBroadcastAddressStatic(BroadcastInterface address){
		synchronized(semaphore_interfaces) {
			int idx = interfaces_broadcast.indexOf(address);
			interfaces_broadcast.remove(idx);
			interfaces_IP_Masks.remove(idx);
			server_address_updated = true;
			client_address_updated = true;
			if(Application.getG_BroadcastServer()==null) return;
			Application.getG_BroadcastServer().delBroadcastAddress(address);
		}
	}
	/**
	 * Get an array list of channels, one per broadcast interface
	 * @return
	 * @throws IOException
	 */
	ArrayList<DatagramChannel> getChannels() throws IOException{
		ArrayList<DatagramChannel> result = new ArrayList<DatagramChannel>();
		ArrayList<Integer> ports = new ArrayList<Integer>();
		for(BroadcastInterface bi: interfaces_broadcast) 
		{
			Integer _port = new Integer(bi.servPort);
			if(ports.contains(_port)) continue;
			ports.add(_port);
			DatagramChannel channel = DatagramChannel.open();
			channel.configureBlocking(false);
			channel.socket().setReuseAddress(true);
			channel.socket().setBroadcast(true);
			InetAddress computer = InetAddress.getByName("0.0.0.0");
			channel.socket().bind(new InetSocketAddress(computer, bi.servPort));
			channel.socket().setTrafficClass(0x10);
			result.add(channel);
		}
		return result;
	}
	public void stopServer(){
		if(_DEBUG) System.out.println("Server STOP");
		synchronized(semaphore_interfaces) {
			running = false;
			if(selector!=null)
				try {selector.close();
				} catch (IOException e) {}
		}
		if(DEBUG) System.out.println("BroadcastServer:stopServer done");
	}
	public void _run() {
		if(DEBUG) System.out.println("BroadcastServer:running");
		int keys = 0;
		ArrayList<DatagramChannel> server_channels;
		try {
			server_channels = getChannels();
			selector = Selector.open();
		} catch (IOException e1) {e1.printStackTrace();return;}
		while(running) {
			try{
				if(server_address_updated_clear()) {
					server_channels = getChannels();
					synchronized (semaphore_interfaces) {
						if(!running) break; 
						selector.close();
						selector = Selector.open();
					}
					for(DatagramChannel ch: server_channels)
						ch.register(selector, SelectionKey.OP_READ, new BroadcastClientRecord());
				}
				keys = selector.select(TIMEOUT);
				if(keys==0) continue;
				Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
				while (keyIter.hasNext()) {
					SelectionKey key = keyIter.next(); 
					if (key.isReadable())
						try {
							handleRead(key);
						} catch (ASN1DecoderFail e) {
							e.printStackTrace();
						} catch (P2PDDSQLException e) {
							e.printStackTrace();
						}
					keyIter.remove();
				}
			}catch(ClosedSelectorException c){
				if(DEBUG) System.out.println("BroadcastServer:run closed selector");
				try {
					selector = Selector.open();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}catch(IOException e){}
		}
		if(DEBUG) System.out.println("BroadcastServer:run stopping");
	}
	@SuppressWarnings("null")
	private void handleRead(SelectionKey key) throws IOException, ASN1DecoderFail, P2PDDSQLException {		
		DatagramChannel channel = (DatagramChannel) key.channel();
		BroadcastClientRecord clntRec = (BroadcastClientRecord) key.attachment();
		clntRec.buffer.clear();    
		clntRec.clientAddress = channel.receive(clntRec.buffer);
		if (clntRec.clientAddress != null) {  
			byte[] obtained;
			int position_start, msg_size;
			String IP = Util.get_IP_from_SocketAddress(clntRec.clientAddress);
			String Msg_rcv_time = Util.getGeneralizedTime();
			long cnter_val; 
			if(clntRec.buffer.hasArray()){
				int pkt_size = clntRec.buffer.array().length - clntRec.buffer.remaining();				
				byte[] msg = Arrays.copyOfRange(clntRec.buffer.array(), 0, pkt_size-8);
				byte[] cnter = Arrays.copyOfRange(clntRec.buffer.array(),pkt_size-8,pkt_size);
				ByteBuffer buf = ByteBuffer.wrap(cnter);  
				int size = clntRec.buffer.array().length - clntRec.buffer.remaining()-8;
				if(DEBUG)System.out.println("IP : "+IP);
				if(DEBUG)System.out.println("size : "+size);
				obtained = msg;
				position_start = clntRec.buffer.position();
				msg_size = size;
				cnter_val = buf.getLong(); 
				if(DEBUG)System.out.println("counter : "+cnter_val);
				if(DEBUG)System.out.println("clntRec.buffer.array() hash : "+Util.stringSignatureFromByte(Util.simple_hash(clntRec.buffer.array(),0,size, DD.APP_INSECURE_HASH)));
			}
			else{
				int length = clntRec.buffer.position();
				byte[]dst = new byte[length];
				clntRec.buffer.get(dst, 0, length);
				byte[] cnter = Arrays.copyOfRange(clntRec.buffer.array(),length-8,length);
				ByteBuffer buf = ByteBuffer.wrap(cnter);  
				if(DEBUG)System.out.println("dst len : "+dst.length);
				obtained = dst;
				position_start =clntRec.buffer.position();
				msg_size = dst.length;
				cnter_val = buf.getLong(); 
				if(DEBUG)System.out.println("dst hash : "+Util.stringSignatureFromByte(Util.simple_hash(dst, DD.APP_INSECURE_HASH)));
			}
			net.ddp2p.common.data.D_Interests_Message dim = new net.ddp2p.common.data.D_Interests_Message();
			dim.decode(new Decoder(obtained));
			byte[] msg_payload = dim.message;
			byte[] msg_interests = dim.interests;
			if(msg_interests != null) {
				net.ddp2p.common.data.D_Interests interests = new net.ddp2p.common.data.D_Interests();
				interests.decode(new Decoder(msg_interests));
				if(Util.equalBytes(interests.Random_peer_number, DD.Random_peer_Number)){
					if(DEBUG) System.out.println("BroadcastServer:handleRead: Receiving from my self");
					return;
					}
				if(DEBUG)System.out.println("BroadcastServer:handleRead: Incoming="+interests.Random_peer_number+"  mine="+DD.Random_peer_Number);
				else if(DEBUG)System.out.println("BroadcastServer:handleRead: Not Receiving from my self");
				net.ddp2p.common.handling_wb.BroadcastQueueRequested.handleNewInterests(interests,  clntRec.clientAddress, IP);
			}
			if(BroadcastConsummerBuffer.queue == null) BroadcastConsummerBuffer.queue = new BroadcastConsummerBuffer();
			synchronized(BroadcastConsummerBuffer.queue) {
				BroadcastConsummerBuffer.queue.add(msg_payload, position_start, clntRec.clientAddress,msg_size,IP,cnter_val,Msg_rcv_time);
			}
		}
	}
	public static byte[] extractCounter(long cnt[], byte[] orig){
		byte[] cnter = Arrays.copyOfRange(orig,orig.length-8,orig.length);
		ByteBuffer buf = ByteBuffer.wrap(cnter);
		cnt[0] = buf.getLong();
		byte[] msg = Arrays.copyOfRange(orig, 0, orig.length-8);
		return msg;
	}
	public static String[][] parseInterfaceDescription(String desc){
		String[][] result = new String[0][];
		if(desc==null){
			if(DEBUG) System.out.println("BroadcastServer:parseInterfaceDesc: null");
			return null;
		}
		String []_ld = desc.split(Pattern.quote(","));
		int rows = _ld.length;
		int cols = 0;
		result = new String[rows][];
		for(int k = 0; k<rows; k++) {
			result[k] = _ld[k].split(Pattern.quote(":"));
			if(DEBUG) System.out.println("BroadcastServer:parseInterfaceDesc: row["+k+"]="+_ld[k]);
		}
		return result;
	}
	public static String old_interfaces_description = null;
}
