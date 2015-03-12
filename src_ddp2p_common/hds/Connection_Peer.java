package hds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

import util.Util;
import data.D_Peer;

/**
 *  peer (redundant: name, ID, GID, filtered, last_sync_date)
 *  ArrayList<Peer_Directory> peer_directories; //order by last_contact!
 *  ArrayList<Peer_Socket> peer_sockets; // static addresses, order by contact!
 *  contacted_since_start, last_contact_successful
 * @author msilaghi
 *
 */
public class Connection_Peer {
	/**
	 * Who are we talking about...
	 */
	public D_Peer peer;

	/**
	 * The address books listing this peer, //order by last_contact!
	 */
	private ArrayList<Connections_Peer_Directory> shared_peer_directories; 
	/**
	 * Known IP addresses for this peer (assumed static) // static addresses, order by contact!
	 */
	public ArrayList<Connections_Peer_Socket> shared_peer_sockets; 
	
	
	/**
	 * list of clones for this peer
	 */
	public ArrayList<Connection_Instance> instances_AL = new ArrayList<Connection_Instance>();
	/**
	 * clones, accessed by instance_name made unique (based on Util.getStringNonNullUnique).
	 */
	public Hashtable<String, Connection_Instance> instances_HT = new Hashtable<String, Connection_Instance>();
	static class Connection_Peer_Status {
		private boolean contacted_since_start = false;
		private boolean last_contact_successful = false;

		/**
		 * If not using TCP, Client2 cannot know if the addresses were just added and not yet tested,
		 *  therefore will ask them again redundantly.
		 *  
		 */
		private boolean justRequestedSupernodesAddresses;
	}
	Connection_Peer_Status status = new Connection_Peer_Status();
	
	public Connection_Peer() {
		setSharedPeerDirectories(new ArrayList<Connections_Peer_Directory>());
		shared_peer_sockets = new ArrayList<Connections_Peer_Socket>();
	}
	/**
	 * Sort whenever needed based on number of already exchanged objects.
	 */
	public void sortInstances() {
		Collections.sort(instances_AL, new Comparator<Connection_Instance>(){

			@Override
			public int compare(Connection_Instance arg0,
					Connection_Instance arg1) {
				return arg1.dpi.getNbSyncObjects() - arg0.dpi.getNbSyncObjects();
			}});
	}
	/**
	 * Gets based on potentially null parameters
	 * @param instance
	 * @return
	 */
	public Connection_Instance getInstanceConnection(String instance) {
		return this.instances_HT.get(Util.getStringNonNullUnique(instance));
	}
	/**
	 * Puts using potentially null instance names.
	 * Sorts instances based on the number of exchanged objects (to access first the most accessed)
	 * @param instance
	 * @param ic
	 */
	public void putInstanceConnection(String instance, Connection_Instance ic) {
		 this.instances_HT.put(Util.getStringNonNullUnique(instance), ic);
		 // add the element at the end such that the iterator in update may be able to pass over it, if added from a directory.
		 instances_AL.add(ic);
		 sortInstances();
	}
	public String getName() {return peer.getName();}
	/** gets GID of peer */
	public String getGID() {return peer.getGID();}
	/** gets GIDH of peer */
	public String getGIDH() {return peer.getGIDH_force();}
	public long getID() {return peer.getLID_keep_force();}
	public boolean getFiltered() {return peer.getFiltered();}
	//public String getLastSyncDate() {return peer.getLastSyncDate(instance);}
	public String toString() {
		return "[Peer_Connection: ID = "+getID()+" name = \""+getName()+//" date="+getLastSyncDate()+
				"\" contact = "+isContactedSinceStart()+
				" success = "+isLastContactSuccessful()+
				"\n shared dirs = \n  " + Util.concat(getSharedPeerDirectories(), "\n  ", "empty")+
				"\n shared socks = \n  " + Util.concat(shared_peer_sockets, "\n  ", "empty")+
				"\n instances = \n  " + Util.concat(instances_AL, "\n  ", "empty")+
				"]";
	}
	/**
	 * TODO
	 * Not implemented
	 * @param ps
	 * @return
	 */
	public String getInstance(Connections_Peer_Socket ps) {
		System.out.println("Connection_Peer: getInstance: TODO");
		// TODO Auto-generated method stub
		return null;
	}
	public ArrayList<Connections_Peer_Directory> getSharedPeerDirectories() {
		return shared_peer_directories;
	}
	public void setSharedPeerDirectories(ArrayList<Connections_Peer_Directory> shared_peer_directories) {
		this.shared_peer_directories = shared_peer_directories;
	}
	public boolean isContactedSinceStart() {
		return status.contacted_since_start;
	}
	public void setContactedSinceStart(boolean contacted_since_start) {
		status.contacted_since_start = contacted_since_start;
	}
	public boolean isLastContactSuccessful() {
		return status.last_contact_successful;
	}
	public void setLastContactSuccessful(boolean last_contact_successful) {
		status.last_contact_successful = last_contact_successful;
	}
	public boolean isJustRequestedSupernodesAddresses() {
		return status.justRequestedSupernodesAddresses;
	}
	public void setJustRequestedSupernodesAddresses(
			boolean justRequestedSupernodesAddresses) {
		status.justRequestedSupernodesAddresses = justRequestedSupernodesAddresses;
	}
}