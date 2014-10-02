package hds;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Address ad (SocketAddresses_Domain addr (long address_ID))
 * behind_NAT (always false?)
 * (Calendar/String) last_contact
 * contacted_since_start_TCP/UDP
 * last_contact_successful_TCP/UDP
 * Socket tcp_connection_open
 * boolean tcp_connection_open_busy
 * 
 * @author msilaghi
 *
 */
public class Connections_Peer_Socket {
	public Address_SocketResolved addr;
	public long address_ID;
	public boolean behind_NAT = false; // always false (otherwise keep in Peer_Directory)
	public String _last_contact;
	public boolean contacted_since_start_TCP = false;
	public boolean last_contact_successful_TCP = false;
	public boolean contacted_since_start_UDP = false;
	public boolean last_contact_successful_UDP = false;
	public Socket tcp_connection_open = null; // non-null if a TCP connection is open (synchronized)
	public boolean tcp_connection_open_busy = false; // is some thread using this connection?
	
	public Connections_Peer_Socket() {}
	public Connections_Peer_Socket(Address ad, InetAddress ia) {
		InetSocketAddress isa_t = null, isa_u = null;
		if (ad.tcp_port > 0) isa_t = new InetSocketAddress(ia, ad.tcp_port);
		if (ad.udp_port > 0) isa_u = new InetSocketAddress(ia, ad.udp_port);
		addr = new Address_SocketResolved(ia, isa_t, isa_u, ad);
		address_ID = -1;
	}
	public String toString() {
		return "[Peer_Socket: ID="+address_ID+" addr="+addr+" date="+_last_contact+"]";
	}
	public Address getAddress() {return addr.ad;}
}