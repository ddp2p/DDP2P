package hds;

import java.net.InetSocketAddress;

public class Address_SocketResolved_TCP{
	public Address ad;
	public InetSocketAddress isa, isa_udp;
	
	public Address_SocketResolved_TCP(InetSocketAddress _isa, InetSocketAddress _isa_udp, Address _ad){
		ad = _ad;
		isa = _isa;
		isa_udp = _isa_udp;
		if (isa_udp == null) isa_udp = isa;
	}
	public Address_SocketResolved_TCP(InetSocketAddress _isa, Address _ad){
		ad = _ad;
		isa_udp = isa = _isa;
	}
	public String toString() {
		return "[SockAd_D: ad="+ad+" sock="+isa+"]";
	}
	public static Address_SocketResolved_TCP getTCP(Address_SocketResolved addr) {
		Address_SocketResolved_TCP sad = new Address_SocketResolved_TCP(addr.isa_tcp, addr.ad);
		return sad;
	}
	public static Address_SocketResolved_TCP getUDP(Address_SocketResolved addr) {
		Address_SocketResolved_TCP sad = new Address_SocketResolved_TCP(addr.isa_udp, addr.ad);
		return sad;
	}
}