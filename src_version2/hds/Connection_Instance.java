package hds;

import java.util.ArrayList;

import util.Util;
import data.D_PeerInstance;

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

	public boolean contacted_since_start = false;
	public boolean last_contact_successful = false;
	
	public String toString() {
		return "Connection_Instance:"+dpi+" contacted="+contacted_since_start+" succ="+last_contact_successful+
				"\n\t inst dirs="+Util.concat(peer_directories, ",", "NULL")+
				"\n\t inst sock="+Util.concat(peer_sockets, ",", "NULL");
	}
	
}