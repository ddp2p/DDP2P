package hds;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Encapsulate 
 * Address ad, InetAddress ia, InetSocketAddress isa_tcp, isa_udp (ia + ports)
 * @author msilaghi
 *
 */
public class Address_SocketResolved{
	public Address ad;
	InetSocketAddress isa_tcp, isa_udp;
	public InetAddress ia;
	
	Address_SocketResolved(InetAddress _ia, InetSocketAddress _isa_tcp, InetSocketAddress _isa_udp, Address _ad){
		ad = _ad;
		ia = _ia;
		isa_tcp = _isa_tcp;
		isa_udp = _isa_udp;
	}
	public String toString() {
		return "[SockAddr_D: ad="+ad+" ia="+ia+" tcp="+isa_tcp+" udp="+isa_udp+"]";
	}
}