package net.ddp2p.common.hds;
import java.net.SocketAddress;
import java.util.ArrayList;
import net.ddp2p.common.data.D_PeerInstance;
import net.ddp2p.common.util.Util;
public class Connection_Instance {
	/**
	 * The clone we talk about ...
	 */
	public D_PeerInstance dpi;
	/**
	 * Any dedicated address books //order by last_contact!
	 */
	public ArrayList<Connections_Peer_Directory> peer_directories = new ArrayList<Connections_Peer_Directory>();
	/**
	 * The current sockets known for this clone // static addresses, order by contact!
	 */
	public ArrayList<Connections_Peer_Socket> peer_sockets = new ArrayList<Connections_Peer_Socket>();
	public ArrayList<Connections_Peer_Socket> peer_sockets_transient = new ArrayList<Connections_Peer_Socket>();
	public static class Connection_Instance_Status {
		private boolean contacted_since_start_TCP = false;
		private boolean last_contact_successful_TCP = false;
		private boolean ping_pending_UDP = false;
		private String last_contact_date_UDP = null;
	}
	Connection_Instance_Status status = new Connection_Instance_Status(); 
	public String toString() {
		return "Connection_Instance: ["+dpi+"] contacted="+isContactedSinceStart_TCP()+" succ="+isLastContactSuccessful_TCP()+
				"\n\t inst dirs="+Util.concat(peer_directories, ",", "NULL")+
				"\n\t inst sock="+Util.concat(peer_sockets, ",", "NULL");
	}
	public boolean isContactedSinceStart_TCP() {
		return status.contacted_since_start_TCP;
	}
	public void setContactedSinceStart_TCP(boolean contacted_since_start) {
		status.contacted_since_start_TCP = contacted_since_start;
	}
	public boolean isLastContactSuccessful_TCP() {
		return status.last_contact_successful_TCP;
	}
	public void setLastContactSuccessful_TCP(boolean last_contact_successful) {
		status.last_contact_successful_TCP = last_contact_successful;
	}
	/**
	 * Set when a pingReply comes from this peer.
	 */
	public void setLastContactDate_UDP() {
		status.last_contact_date_UDP = Util.getGeneralizedTime();
		status.ping_pending_UDP = false;
	}
	public void setPingPending_UDP() {
		status.ping_pending_UDP = true;
	}
	public boolean getPingPending_UDP() {
		return status.ping_pending_UDP;
	}
	public String getLastContactDate_UDP() {
		return status.last_contact_date_UDP;
	}
	public Connections_Peer_Socket getPeerSocket(String peer_domain, int peer_port, SocketAddress socketAddress) {
		for (Connections_Peer_Socket ps : this.peer_sockets) {
			if (ps.isSameAs(peer_domain, peer_port, socketAddress)) return ps;
		}
		for (Connections_Peer_Socket ps : this.peer_sockets_transient) {
			if (ps.isSameAs(peer_domain, peer_port, socketAddress)) return ps;
		}
		return null;
	}
	/**
	 * Checks with the specialized directories of this instance
	 * @param peer_domain
	 * @param peer_port
	 * @param socketAddress
	 * @param instance
	 * @return
	 * 	Returns true on the first match. But note that the NAT address that worked might have been forwarded by a different directory.
	 */
	public boolean confirmNAT(String peer_domain, int peer_port,
			SocketAddress socketAddress, String instance) {
		for (Connections_Peer_Directory pd : this.peer_directories) {
			if (pd.confirmNAT(peer_domain, peer_port, socketAddress, instance)) return true;
		}
		return false;
	}
}
