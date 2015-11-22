package net.ddp2p.common.hds;
import java.util.Hashtable;
import java.util.ArrayList;
import net.ddp2p.common.data.D_DirectoryServerPreapprovedTermsInfo;
public class DirectoryMessageStorage{
	private static final boolean DEBUG = false;
	public static Hashtable<String, DirMessage> latestRequest_storage; 
	public static Hashtable<String, ArrayList<DirMessage>> noping_storage; 
	public static Hashtable<String, ArrayList<DirMessage>> ping_storage; 
	/**
	 * Storing the last announcement message received from each GID separately (together with its source)
	 */
	public static Hashtable<String, ArrayList<DirMessage>> announcement_storage; 
	public static Hashtable<String, D_DirectoryServerPreapprovedTermsInfo> last_terms;
	public static Hashtable<String, String> IP_GID; 
	static int maxMsgStorage = 100;
	/**
	 * gets from global IP_GID
	 * @param ip
	 * @return
	 */
	public static String getGID(String ip){
		if (IP_GID == null)
			return null;
		return IP_GID.get(ip); 
	}
	public static void addLatestRequest_storage(String GID, DirMessage m ){
		if(latestRequest_storage == null){
			latestRequest_storage = new Hashtable<String, DirMessage>();
		}
		latestRequest_storage.put(GID, m); 
	}
	public static void addNoPingMsg(String GID, DirMessage m ){
		if (noping_storage == null) {
			noping_storage = new Hashtable<String, ArrayList<DirMessage>>();
		}
		addToStorage(noping_storage, GID, m);
	}
	public static void addPingMsg( DirMessage m ){
		if(ping_storage == null){
			ping_storage = new Hashtable<String, ArrayList<DirMessage>>();
		}
		String GID = getGID(m.sourceIP);
		if(GID!=null)
			addToStorage(ping_storage, GID,m);
	}
	/**
	 * Saves the message m in the announcement_storage queue.
	 * Also add the association IP - GID in the global hash IP_GID
	 * @param GID
	 * @param m
	 */
	public static void addAnnouncementMsg(String GID, DirMessage m ){
		if (DEBUG) System.out.println("DMS:addAnnouncementMsg()");
		if (getGID(m.sourceIP)==null) {
			if (IP_GID == null)
				IP_GID = new Hashtable<String, String>();
			IP_GID.put(m.sourceIP,GID);
		}
		if (announcement_storage == null) {
			announcement_storage = new Hashtable<String, ArrayList<DirMessage>>();
		}
		addToStorage(announcement_storage, GID, m);
	}
	public static void addToStorage(Hashtable<String, ArrayList<DirMessage>> s, String GID, DirMessage m ){	
		if (DEBUG) System.out.println("DMS:addToStorage() msgType:"+m.MsgType+" msgSourceIP:"+m.sourceIP+" DIR="+GID);
		ArrayList<DirMessage> msgs = s.get(GID);
		if (msgs == null) {
		    msgs = new ArrayList<DirMessage>();
		    msgs.add(m);
			s.put(GID, msgs);
			return;
		}
		if (msgs.size()>= maxMsgStorage) {
			msgs.remove(0); 
		}
		msgs.add(m);
	}	
}
