package hds;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;

import util.Util;

/**
 * For a peer.
 * Contains SocketAddress_Domain supernode_addr (the socket of the supernode),
 * behindNAT (TODO not used at all),
 * reported_peer_addr (NAT address, old, if only one stored),
 * last_contact, 
 * contacted_since_start, 
 * last_contact_successful
 * address_ID (for supernode_addr, LID in peerTable)
 * reportedAddressesUDP (list received from this directory)
 * @author msilaghi
 *
 */
public class Connections_Peer_Directory {
	/**
	 * The socket of the supernode
	 */
	public Address_SocketResolved_TCP supernode_addr;
	public boolean behindNAT; // TODO
	/**
	 * Addresses of clones as reported by this address book, by instance_name.
	 */
	public Hashtable<String,Address_SocketResolved_TCP> _reported_peer_addr = new Hashtable<String,Address_SocketResolved_TCP>(); // NAT
	//public Address_SocketResolved_TCP reported_peer_addr_; // NAT, old, if only one stored
	
	// Calendar last_contact;
	public String _last_contact_TCP;
	public boolean contacted_since_start_TCP = false;
	public boolean last_contact_successful_TCP = false;
	public String _last_contact_UDP;
	public boolean contacted_since_start_UDP = false;
	public boolean last_contact_successful_UDP = false;
	// conditions negotiated
	public long address_ID;
	/* a new Connections_Peer_Directory is added to instance if it was missing my myself, no instance is created if missing */
	public ArrayList<Address> reportedAddressesUDP; // list received from this directory for this instance
	//public ArrayList<Address> reportedAddressesTCP; // list received from this directory
	
	public Connections_Peer_Directory() {}
	public Connections_Peer_Directory(Address ad, InetAddress ia) {
		InetSocketAddress isa = null, isa_udp = null;
		if (ad.tcp_port > 0) isa = new InetSocketAddress(ia, ad.tcp_port);
		if (ad.udp_port > 0) isa_udp = new InetSocketAddress(ia, ad.udp_port);
		
		supernode_addr = new Address_SocketResolved_TCP(isa, isa_udp, ad);
		address_ID = -1;
	}
	public String toString() {
		return "[Peer_Directory: ID="+address_ID+" dir="+supernode_addr+" reported="+getReportedAddresses()+"]";
	}
	public String getReportedAddress(Address_SocketResolved_TCP reported_peer_addr) {
		String rep = ((reported_peer_addr != null)?(reported_peer_addr.getAddressSupernode()+" ("+reported_peer_addr.isa+") "):"null_val ");
		return rep;
	}
	/**
	 * Builds a string representation of the reported addresses, as:
	 * {inst: (addr)}{inst: (addr)}
	 * @return
	 */
	public String getReportedAddresses() {
		//Address_SocketResolved_TCP reported_peer_addr = reported_peer_addr_;
		String rep = ""; //getReportedAddress(reported_peer_addr);
				//((reported_peer_addr != null)?(reported_peer_addr.ad+" ("+reported_peer_addr.isa+") "):"null_val ");
		for (String x : this._reported_peer_addr.keySet())
			rep = rep +"{"+x+":"+getReportedAddress(this._reported_peer_addr.get(x))+"}";
		return rep;
	}
	/**
	 * Tries to get addresses of clone "instance" without a query, in the hashtable: "_reported_peer_addr".
	 * In case of failure, returns "reported_peer_addr_" (else value found in hashtable)
	 * @param instance
	 * @return
	 */
	public Address_SocketResolved_TCP getReportedAddress(String instance) {
		Address_SocketResolved_TCP result = this._reported_peer_addr.get(Util.getStringNonNullUnique(instance));
		//if (result == null) result = this.reported_peer_addr_;
		return result;
	}
	public Address_SocketResolved_TCP getReportedAddressSome() {
		for (Address_SocketResolved_TCP result : this._reported_peer_addr.values())
			return result;
		return null;
	}
}