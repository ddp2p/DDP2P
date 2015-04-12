package net.ddp2p.common.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;

import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.hds.PeerInput;
import net.ddp2p.common.updates.VersionInfo;
import net.ddp2p.common.util.DB_Implementation;
import net.ddp2p.common.util.DD_IdentityVerification_Answer;
import net.ddp2p.common.util.Util;

public class Application_GUI {
	
	public static final int OK_CANCEL_OPTION = 2;
	public static final int QUESTION_MESSAGE = 3;
	public static final int WARNING_MESSAGE = 2;
	public static final int YES_NO_OPTION = 0; 
	public static final int CLOSED_OPTION = -1;
	public static final int YES_NO_CANCEL_OPTION = 1;
	public static final int YES_OPTION = 0;
	
	public static Vendor_GUI_Dialogs gui = null;
	public static Vendor_DB_Email dbmail = null;
	
	public static void fixScriptsBaseDir(String dir) {
		if (gui == null) Util.printCallPath("");
		if (!DD.GUI || (gui == null)) {
			System.out.println("Application: fixScriptsBaseDir: "+dir);
			return;
		}
		gui.fixScriptsBaseDir(dir);
	}

	public static void warning(String war, String title) {
		if (gui == null) Util.printCallPath("");
		if (!DD.GUI || (gui == null)) {
			System.out.println("Application: warning: "+title+" "+war);
			return;
		}
		gui.warning(war, title);
	}

	public static void info(String inf, String title) {
		if (gui == null) Util.printCallPath("");
		if (!DD.GUI || (gui == null)) {
			System.out.println("Application: info: "+title+" "+inf);
			return;
		}
		gui.info(inf, title);
	}

	/**
	 * 
	 * @param war
	 * @param title
	 * @param type may be: JOptionPane.OK_CANCEL_OPTION
	 *    0 for  JOptionPane.YES_NO_OPTION
	 * @return index of selected option
	 */
	public static int ask(String war, String title, int type){
		if (gui == null) Util.printCallPath("");
		if (!DD.GUI || (gui == null)) {
			System.out.println("Application: ask: "+title+" "+war);
			InputStreamReader br = new InputStreamReader(System.in);
			BufferedReader bsr = new BufferedReader(br);
			int result = 0;
			try {
				result = Integer.parseInt(bsr.readLine());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return result;
		}
		return gui.ask(war, title, type);
	}

	/**
	 * type = JOptionPane.QUESTION_MESSAGE
	 * @param prompt
	 * @param title
	 * @return
	 */
	public static String input(String prompt, String title, int type) {
		if (gui == null) Util.printCallPath("");
		if (!DD.GUI || (gui == null)) {
			System.out.println("Application: ask: "+title+"\n"+prompt);
			InputStreamReader br = new InputStreamReader(System.in);
			BufferedReader bsr = new BufferedReader(br);
			String result = null;
			try {
				result = bsr.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return result;
		}
		return gui.input(prompt, title, type);
	}

	/**
	 *  Returns the option of CLOSED_OPTION for default option
		 * @param war
		 * @param title
		 * @param options
		 * @param def_option
		 * @param icon
		 * @return
		 */
	public static int ask(String title, String war, Object[] options, Object def_option, Object icon){
		if (gui == null) Util.printCallPath("");
		if (!DD.GUI || (gui == null)) {
			System.out.println("Application: ask: "+title+"\n\t "+war+"\nOptions: \""+Util.concat(options, "\", \"")+"\"");
			InputStreamReader br = new InputStreamReader(System.in);
			BufferedReader bsr = new BufferedReader(br);
			int result = 0;
			try {
				result = Integer.parseInt(bsr.readLine());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return result;
		}
		return gui.ask(title, war, options, def_option, icon);
	}

	public static void setBroadcastServerStatus_GUI(boolean run) {
		if (gui == null) return;
		gui.setBroadcastServerStatus_GUI(run);
	}

	public static void setBroadcastClientStatus_GUI(boolean run) {
		if (gui == null) return;
		gui.setBroadcastClientStatus_GUI(run);
	}

	public static void setSimulatorStatus_GUI(boolean run) {
		if (gui == null) return;
		gui.setSimulatorStatus_GUI(run);
	}

	public static String html2text(String document) {
		if (gui == null) return document;
		return gui.html2text(document);
	}

	public static String[] getWitnessScores() {
		if (gui == null) return null;
		return gui.getWitnessScores();
	}

	public static D_Constituent getMeConstituent() {
		if (gui == null) return null;
		return gui.getMeConstituent();
	}

	public static void ThreadsAccounting_registerThread() {
		if (gui == null) return;
		gui.registerThread();
	}

	public static void ThreadsAccounting_unregisterThread() {
		if (gui == null) return;
		gui.unregisterThread();
	}

	public static String queryDatabaseFile() {
		if (gui == null) return null;
		return gui.queryDatabaseFile();
	}

	public static void updateProgress(Object ctx, String string) {
		if (gui == null) return;
		gui.updateProgress(ctx, string);
	}

	public static void eventQueue_invokeLater(Runnable rp) {
		if (gui == null) return;
		gui.eventQueue_invokeLater(rp);
	}

	public static void clientUpdates_Start() {
		if (gui == null) return;
		gui.clientUpdates_Start();
	}

	public static void clientUpdates_Stop() {
		if (gui == null) return;
		gui.clientUpdates_Stop();
	}

	public static void ThreadsAccounting_ping(String string) {
		if (gui == null) return;
		gui.ThreadsAccounting_ping(string);
	}

	public static void setClientUpdatesStatus(boolean b) {
		if (gui == null) return;
		gui.setClientUpdatesStatus(b);
	}

	public static void peer_contacts_update() {
		if (gui == null) return;
		gui.peer_contacts_update();
	}

	public static boolean is_crt_peer(D_Peer candidate) {
		if (gui == null) return false;
		return gui.is_crt_peer(candidate);
	}

	public static boolean is_crt_org(D_Organization candidate) {
		if (gui == null) return false;
		return gui.is_crt_org(candidate);
	}	

	public static boolean is_crt_const(D_Constituent candidate) {
		if (gui == null) return false;
		return gui.is_crt_const(candidate);
	}
	/**
	 * 
	 * Set this as me in the GUI status and displays
	 * @param me
	 */
	public static void setMePeer(D_Peer me) {
		if (gui == null) return;
		gui.setMePeer(me);
	}
	/**
	 * 
	 * Query the user whether the new peer should have the attributes in pi, or other ones.
	 * The final ones are set in _data (input as an empty array of size one).
	 * The result may be kept and unsigned, or signed and unkept. To be verified by caller using kept()
	 * @param pi
	 * @param _data
	 * @return
	 */
	public static D_Peer createPeer(PeerInput pi, PeerInput[] _data) {
		if (gui == null) return null;
		return gui.createPeer(pi, _data);
	}

	public static void update_broadcast_client_sockets(Long msg_cnter) {
		if (gui == null) return;
		gui.update_broadcast_client_sockets(msg_cnter);
	}
	
	public static void inform_arrival(Object obj, D_Peer source) {
		if (gui == null) return;
		gui.inform_arrival(obj, source);
	}

	public static DB_Implementation get_DB_Implementation() {
		if (dbmail == null) return null;
		return dbmail.get_DB_Implementation();
	}

	public static String[] extractDDL(File database_file, int notSQLITE4JAVA) {
		if (dbmail == null) return null;
		return dbmail.extractDDL(database_file, notSQLITE4JAVA);
	}

	public static boolean db_copyData(File database_old, File database_new,
			BufferedReader dDL, String[] _DDL, boolean _SQLITE4JAVA) {
		if (dbmail == null) return false;
		return dbmail.db_copyData(database_old, database_new, dDL, _DDL, _SQLITE4JAVA);
	}

	public static void sendEmail(DD_IdentityVerification_Answer answer) {
		if (dbmail == null) return;
		dbmail.sendEmail(answer);
	}

	public static boolean playThanks() {
		if (gui == null) return false;
		return gui.playThanks();
	}

	public static URL isWSVersionInfoService(String url_str) {
		if (dbmail == null) return null;
		return dbmail.isWSVersionInfoService(url_str);
	}

	public static VersionInfo getWSVersionInfo(URL _url, String myPeerGID,
			SK myPeerSK, Hashtable<Object, Object> ctx) {
		if (dbmail == null) return null;
		return dbmail.getWSVersionInfo(_url, myPeerGID, myPeerSK, ctx);
	}

	/*
	public static boolean clipboardCopy(String exportText) {
		if (gui == null) return false;
		return gui.clipboardCopy();
	}
	*/
}
