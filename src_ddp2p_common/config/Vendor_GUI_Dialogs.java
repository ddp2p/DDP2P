package config;


import hds.PeerInput;
import data.D_Constituent;
import data.D_Organization;
import data.D_Peer;

public interface Vendor_GUI_Dialogs {
	public void fixScriptsBaseDir(String dir);
	public void warning(String war, String title);
	public int ask(String war, String title, int type);
	public int ask(String title, String war, Object[] options, Object def_option, Object icon);
	public String input(String prompt, String title, int type);
	public void setBroadcastServerStatus_GUI(boolean run);
	public void setBroadcastClientStatus_GUI(boolean run);
	public void setSimulatorStatus_GUI(boolean run);
	public String html2text(String document);
	public String[] getWitnessScores(); 
	public D_Constituent getMeConstituent();
	public void registerThread();
	public void unregisterThread();
	public String queryDatabaseFile();
	public void info(String inf, String title);
	public void updateProgress(Object ctx, String string);
	public void eventQueue_invokeLater(Runnable rp);
	public void clientUpdates_Start();
	public void clientUpdates_Stop();
	public void ThreadsAccounting_ping(String string);
	public void setClientUpdatesStatus(boolean b);
	public void peer_contacts_update();
	public boolean is_crt_peer(D_Peer candidate);
	public boolean is_crt_org(D_Organization candidate);
	public boolean is_crt_const(D_Constituent candidate);
	/**
	 * Set this as me in the GUI status and displays
	 * 
	 * @param me
	 */
	public void setMePeer(D_Peer me);
	/**
	 * Query the user whether the new peer should have the attributes in pi, or other ones.
	 * The final ones are set in _data (input as an empty array of size one).
	 * The result may be kept and unsigned, or signed and unkept. To be verified by caller using kept()
	 * 
	 * @param pi
	 * @param _data
	 * @return
	 */
	public D_Peer createPeer(PeerInput pi, PeerInput[] _data);
	public void update_broadcast_client_sockets(Long msg_cnter);
	public void inform_arrival(Object obj, D_Peer source);
}