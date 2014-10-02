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
	 * The address books listing this peer
	 */
	public ArrayList<Connections_Peer_Directory> shared_peer_directories; //order by last_contact!
	/**
	 * Known IP addresses for this peer (assumed static)
	 */
	public ArrayList<Connections_Peer_Socket> shared_peer_sockets; // static addresses, order by contact!
	
	public boolean contacted_since_start = false;
	public boolean last_contact_successful = false;
	
	/**
	 * list of clones for this peer
	 */
	public ArrayList<Connection_Instance> instances_AL = new ArrayList<Connection_Instance>();
	/**
	 * clones, accessed by instance_name made unique (based on Util.getUniqueNameNotNull).
	 */
	public Hashtable<String, Connection_Instance> instances_HT = new Hashtable<String, Connection_Instance>();
	
	public Connection_Peer() {
		shared_peer_directories = new ArrayList<Connections_Peer_Directory>();
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
	public Connection_Instance getInstanceConnection(String instance) {
		return this.instances_HT.get(Util.getStringNonNullUnique(instance));
	}
	public void putInstanceConnection(String instance, Connection_Instance ic) {
		 this.instances_HT.put(Util.getStringNonNullUnique(instance), ic);
		 instances_AL.add(ic);
		 sortInstances();
	}
	public String getName() {return peer.getName();}
	public String getGID() {return peer.getGID();}
	public String getGIDH() {return peer.getGIDH_force();}
	public long getID() {return peer.getLID_keep_force();}
	public boolean getFiltered() {return peer.getFiltered();}
	//public String getLastSyncDate() {return peer.getLastSyncDate(instance);}
	public String toString() {
		return "[Peer_Connection: ID="+getID()+" name="+getName()+//" date="+getLastSyncDate()+
				" contact="+contacted_since_start+
				" success="+last_contact_successful+
				"\n shared dirs=\n  " + Util.concat(shared_peer_directories, "\n  ", "empty")+
				"\n shared socks=\n  " + Util.concat(shared_peer_sockets, "\n  ", "empty")+
				"\n instances=\n  " + Util.concat(instances_AL, "\n  ", "empty")+
				"]";
	}
	public String getInstance(Connections_Peer_Socket ps) {
		System.out.println("Connection_Peer: getInstance: TODO");
		// TODO Auto-generated method stub
		return null;
	}
}