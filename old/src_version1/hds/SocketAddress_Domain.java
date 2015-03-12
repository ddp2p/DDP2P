package hds;

import java.net.InetSocketAddress;

class SocketAddress_Domain{
	Address ad;
	InetSocketAddress isa;
	
	SocketAddress_Domain(InetSocketAddress _isa, Address _ad){
		ad = _ad;
		isa = _isa;
	}
	public String toString() {
		return "[SockAd_D: ad="+ad+" sock="+isa+"]";
	}
	public static SocketAddress_Domain getTCP(SocketAddresses_Domain addr) {
		SocketAddress_Domain sad = new SocketAddress_Domain(addr.isa_tcp, addr.ad);
		return sad;
	}
	public static SocketAddress_Domain getUDP(SocketAddresses_Domain addr) {
		SocketAddress_Domain sad = new SocketAddress_Domain(addr.isa_udp, addr.ad);
		return sad;
	}
}