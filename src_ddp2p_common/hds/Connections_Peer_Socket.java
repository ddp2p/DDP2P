package hds;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import util.Util;

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
	public final static boolean OPEN_CONNECTIONS = false;
	public Address_SocketResolved addr;
	public long address_LID; // local ID when stored in database
	public boolean behind_NAT = false; // true for transient addresses if there was a NAT with the report
	
	// status of the TCP connection
	public String _last_contact_date_TCP;
	public boolean contacted_since_start_TCP = false;
	public boolean last_contact_successful_TCP = false;
	
	// status of the UDP connections
	public boolean replied_since_start_UDP = false;
	/** Set if a ping was sent since the last reply */
	public boolean last_contact_pending_UDP = false;
	public String _last_ping_sent_date;
	public String _last_ping_received_date;
	
	// Open connections not yet used (have to decide when to close them if used)
	public Socket tcp_connection_open = null; // non-null if a TCP connection is open (synchronized)
	// not yet used but should be used for synchronization (if ever using open sockets)
	public boolean tcp_connection_open_busy = false; // is some thread using this connection?
	
	public Connections_Peer_Socket() {}
	public Connections_Peer_Socket(Address ad, InetAddress ia) {
		InetSocketAddress isa_t = null, isa_u = null;
		if (ad.tcp_port > 0) isa_t = new InetSocketAddress(ia, ad.tcp_port);
		if (ad.udp_port > 0) isa_u = new InetSocketAddress(ia, ad.udp_port);
		addr = new Address_SocketResolved(ia, isa_t, isa_u, ad);
		address_LID = -1;
	}
	public String toString() {
		return "[Peer_Socket: ID="+address_LID+" addr="+addr+" date="+_last_contact_date_TCP+"]";
	}
	public Address getAddress() {return addr.addr;}
	public void setLastContactDateTCP() {
		_last_contact_date_TCP = Util.getGeneralizedTime();
		contacted_since_start_TCP = last_contact_successful_TCP = true;
	}
	public void setLastContactDateUDP() {
		_last_ping_received_date = Util.getGeneralizedTime();
		replied_since_start_UDP = true;
		last_contact_pending_UDP = false;
	}
	public void setPingSentDateUDP() {
		_last_ping_sent_date = Util.getGeneralizedTime();
		last_contact_pending_UDP = true;
	}
	public boolean worksRecently() {
		return (this.replied_since_start_UDP && ! this.last_contact_pending_UDP);
	}
	/**
	 * Compares first the socketAddress with the saved resolved socket available for this address, returning true on success.
	 * Then fails if either the domain or port are different
	 * @param peer_domain
	 * @param peer_port
	 * @param socketAddress
	 * @return
	 */
	public boolean isSameAs(String peer_domain, int peer_port,
			SocketAddress socketAddress) {
		if (socketAddress != null && addr.isa_udp != null && addr.isa_udp.equals(socketAddress)) return true;
		if (! Util.equalStrings_null_or_not(peer_domain, addr.addr.getDomain())) return false;
		if (peer_port != addr.addr.udp_port) return false;
		// TODO setting behindNAT to false?
		return false;
	}
}