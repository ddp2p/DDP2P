package net.ddp2p.common.wireless;
import java.net.*;
public class BroadcastInterface{
	public static final InetAddress BROADCAST_ADDRESS = getByAddress(new byte[]{(byte) 255,(byte) 255,(byte) 255,(byte) 255});
    public InetAddress broadcast_address = BROADCAST_ADDRESS;
	public int servPort=BroadcastServer.BROADCAST_SERVER_PORT;
    BroadcastInterface(){}
    public static InetAddress getByAddress(byte[] addr) {
    	try {
			return InetAddress.getByAddress(addr);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
    }
    public BroadcastInterface(InetAddress _address){
    	broadcast_address = _address;
    }
}
