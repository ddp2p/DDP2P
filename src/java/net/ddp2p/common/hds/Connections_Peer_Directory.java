package net.ddp2p.common.hds;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;

import net.ddp2p.common.util.Util;

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
	/**
	 * Addresses of clones as reported by this address book, by instance_name.
	 */
	public Hashtable<String,Address_SocketResolved_TCP> _reported_peer_addr = new Hashtable<String,Address_SocketResolved_TCP>(); // NAT
	public Hashtable<String,String> _reported_last_contact_date = new Hashtable<String,String>(); // NAT
	public Hashtable<String,Boolean> _reported_last_ping_pending = new Hashtable<String,Boolean>(); // NAT
	// it is behindNAT if the reported NAT address is not empty
	//public Hashtable<String,Boolean> behindNAT = new Hashtable<String,Boolean>(); // NAT
	//public Address_SocketResolved_TCP reported_peer_addr_; // NAT, old, if only one stored
	
	// Calendar last_contact;
	public String _last_contact_TCP;
	public boolean contacted_since_start_TCP = false;
	public boolean last_contact_successful_TCP = false;
	
	private String _last_contact_UDP;
	public boolean contacted_since_start_UDP = false;
	public boolean last_directory_address_request_pending = false;
	// conditions negotiated
	public long address_LID;
	/* a new Connections_Peer_Directory is added to instance if it was missing my myself, no instance is created if missing */

	///** To deprecate and replace by lastAnswer when time allows */
	public ArrayList<Address> reportedAddressesUDP; // list received from this directory for this instance (null for shared dirs)
	//public ArrayList<Address> reportedAddressesTCP; // list received from this directory
	public DirectoryAnswerMultipleIdentities lastAnswer; 
	
	public Connections_Peer_Directory() {}
	public Connections_Peer_Directory(Address ad, InetAddress ia) {
		InetSocketAddress isa = null, isa_udp = null;
		if (ad.tcp_port > 0) isa = new InetSocketAddress(ia, ad.tcp_port);
		if (ad.udp_port > 0) isa_udp = new InetSocketAddress(ia, ad.udp_port);
		
		supernode_addr = new Address_SocketResolved_TCP(isa, isa_udp, ad);
		address_LID = -1;
	}
	public String toString() {
		return "[Peer_Directory: ID="+address_LID+" dir="+supernode_addr+" reported="+getReportedAddresses()+"]";
	}
	public boolean isBehindNAT(String instance) {
		return this._reported_peer_addr.get(Util.getStringNonNullUnique(instance)) != null;
	}
	public String getReportedAddress(Address_SocketResolved_TCP reported_peer_addr) {
		String rep = ((reported_peer_addr != null)?(reported_peer_addr.getAddress()+" ("+reported_peer_addr.isa_tcp+") "):"null_val ");
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
	public String getLastContactUDP() {
		return _last_contact_UDP;
	}
	/**
	 * Set on a directory answer
	 */
	public void setLastDirContactUDP() {
		this._last_contact_UDP = Util.getGeneralizedTime();
		this.last_directory_address_request_pending = false;
		this.contacted_since_start_UDP = true;
	}
	public void setDirAddressReqPending() {
		last_directory_address_request_pending = true;
	}
	/**
	 * Checks that the NAT address matches and sets the lastPeerContactDate.
	 * @param peer_domain
	 * @param peer_port
	 * @param socketAddress
	 * @param peer_instance
	 * @return
	 */
	public boolean confirmNAT(String peer_domain, int peer_port,
			SocketAddress socketAddress, String peer_instance) {
		Address_SocketResolved_TCP adr = this.getReportedAddress(peer_instance);
		if (! Util.equalStrings_null_or_not(peer_domain, adr.getAddress().getDomain())) return false;
		if (peer_port != adr.getAddress().udp_port) return false;
		this.setLastPeerContactUDP(peer_instance);
		return true;
	}
	/**
	 * Just puts the current GeneralizedTime at the instance of this parameter at _reported_last_contact.
	 * Also puts false at _reported_last_ping_pending.
	 * @param peer_instance
	 */
	public void setLastPeerContactUDP(String peer_instance) {
		this._reported_last_contact_date.put(Util.getStringNonNullUnique(peer_instance), Util.getGeneralizedTime());
		this._reported_last_ping_pending.put(Util.getStringNonNullUnique(peer_instance), Boolean.FALSE);
	}
	public void setPeerPingPendingUDP(String peer_instance) {
		this._reported_last_ping_pending.put(Util.getStringNonNullUnique(peer_instance), Boolean.TRUE);
	}
	/**
	 * Checks that the instance is false in _reported_last_ping_pending
	 * @param peer_instance
	 * @return
	 */
	public boolean recentlyContacted(String peer_instance) {
		//(pd.contacted_since_start_UDP && pd.last_contact_successful_UDP);
		if (this._reported_last_ping_pending.get(Util.getStringNonNullUnique(peer_instance)) == null) return false;
		if (this._reported_last_ping_pending.get(Util.getStringNonNullUnique(peer_instance))) return false;
		return true;
	}
}