package net.ddp2p.common.hds;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.simulator.WirelessLog;
import net.ddp2p.common.updates.ClientUpdates;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.common.wireless.BroadcastConsummerBuffer;
import net.ddp2p.common.wireless.Broadcasting_Probabilities;
public class StartUp {
	public static final boolean DEBUG = false;
	public static final boolean _DEBUG = true;
	public static boolean directory_server_on_start;
	public static boolean data_userver_on_start;
	public static boolean data_nserver_on_start;
	public static boolean data_server_on_start;
	public static boolean data_client_on_start;
	public static boolean data_client_updates_on_start;
	public static boolean wireless_server_on_start;
	public static boolean wireless_client_on_start;
	public static boolean wireless_simulator_on_start;
	/**
	 * Fill the path variables for all operating systems from value in database
	 * 
	 * @throws P2PDDSQLException
	 */
	public static void fill_install_paths_all_OSs_from_DB() throws P2PDDSQLException{
		if(StartUp.DEBUG) System.out.println("StartUpThread:fill_OS_install_path: start");
		Application.LINUX_INSTALLATION_VERSION_BASE_DIR = DD.getAppText(DD.APP_LINUX_INSTALLATION_PATH);
		if (Application.LINUX_INSTALLATION_VERSION_BASE_DIR==null) {
			Application.LINUX_INSTALLATION_VERSION_BASE_DIR = "./";
			DD.setAppText(DD.APP_LINUX_INSTALLATION_PATH,Application.LINUX_INSTALLATION_VERSION_BASE_DIR);
		}
		Application.LINUX_INSTALLATION_ROOT_BASE_DIR = DD.getAppText(DD.APP_LINUX_INSTALLATION_ROOT_PATH);
		Application.LINUX_SCRIPTS_BASE_DIR = DD.getAppText(DD.APP_LINUX_SCRIPTS_PATH);
		Application.LINUX_PLUGINS_BASE_DIR = DD.getAppText(DD.APP_LINUX_PLUGINS_PATH);
		Application.LINUX_DATABASE_DIR = DD.getAppText(DD.APP_LINUX_DATABASE_PATH);
		Application.LINUX_LOGS_BASE_DIR = DD.getAppText(DD.APP_LINUX_LOGS_PATH);
		Application.LINUX_DD_JAR_BASE_DIR = DD.getAppText(DD.APP_LINUX_DD_JAR_PATH);
		if(Application.LINUX_INSTALLATION_ROOT_BASE_DIR==null)Application.LINUX_INSTALLATION_ROOT_BASE_DIR=":";
		if(Application.LINUX_SCRIPTS_BASE_DIR==null)Application.LINUX_SCRIPTS_BASE_DIR=":";
		if(Application.LINUX_PLUGINS_BASE_DIR==null)Application.LINUX_PLUGINS_BASE_DIR=":";
		if(Application.LINUX_DATABASE_DIR==null)Application.LINUX_DATABASE_DIR=":";
		if(Application.LINUX_LOGS_BASE_DIR==null)Application.LINUX_LOGS_BASE_DIR=":";
		if(Application.LINUX_DD_JAR_BASE_DIR==null)Application.LINUX_DD_JAR_BASE_DIR=":";
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: Application.linux_install_prefix ="+ Application.LINUX_INSTALLATION_VERSION_BASE_DIR);
		Application.parseLinuxPaths(
				Application.getCurrentLinuxPathsString()
		);
		Application.WINDOWS_INSTALLATION_VERSION_BASE_DIR = DD.getAppText(DD.APP_WINDOWS_INSTALLATION_PATH);
		if(Application.WINDOWS_INSTALLATION_VERSION_BASE_DIR==null){
			Application.WINDOWS_INSTALLATION_VERSION_BASE_DIR = ".\\";
			DD.setAppText(DD.APP_WINDOWS_INSTALLATION_PATH,Application.WINDOWS_INSTALLATION_VERSION_BASE_DIR);
		}
		Application.WINDOWS_INSTALLATION_ROOT_BASE_DIR = DD.getAppText(DD.APP_WINDOWS_INSTALLATION_ROOT_PATH);
		Application.WINDOWS_SCRIPTS_BASE_DIR = DD.getAppText(DD.APP_WINDOWS_SCRIPTS_PATH);
		Application.WINDOWS_PLUGINS_BASE_DIR = DD.getAppText(DD.APP_WINDOWS_PLUGINS_PATH);
		Application.WINDOWS_DATABASE_DIR = DD.getAppText(DD.APP_WINDOWS_DATABASE_PATH);
		Application.WINDOWS_LOGS_BASE_DIR = DD.getAppText(DD.APP_WINDOWS_LOGS_PATH);
		Application.WINDOWS_DD_JAR_BASE_DIR = DD.getAppText(DD.APP_WINDOWS_DD_JAR_PATH);
		if(Application.WINDOWS_INSTALLATION_ROOT_BASE_DIR==null)Application.WINDOWS_INSTALLATION_ROOT_BASE_DIR=":";
		if(Application.WINDOWS_SCRIPTS_BASE_DIR==null)Application.WINDOWS_SCRIPTS_BASE_DIR=":";
		if(Application.WINDOWS_PLUGINS_BASE_DIR==null)Application.WINDOWS_PLUGINS_BASE_DIR=":";
		if(Application.WINDOWS_DATABASE_DIR==null)Application.WINDOWS_DATABASE_DIR=":";
		if(Application.WINDOWS_LOGS_BASE_DIR==null)Application.WINDOWS_LOGS_BASE_DIR=":";
		if(Application.WINDOWS_DD_JAR_BASE_DIR==null)Application.WINDOWS_DD_JAR_BASE_DIR=":";
		Application.parseWindowsPaths(
				Application.getCurrentWindowsPathsString()
		);
		if(StartUp.DEBUG) System.out.println("StartUpThread:fill_OS_install_path: done");
	}
	/**
	 * Assumes that the current OS was already set in DD.OS by prior call to StartUpThread.detect_OS..., 
	 * Here switch paths to DD.OS.
	 */
	public static void switch_install_paths_to_ones_for_current_OS(){
		switch (DD.OS) {
		case DD.WINDOWS:{
			Application.switchToWindowsPaths();
			break;}
		case DD.LINUX: {
			Application.switchToLinuxPaths();
			break;}
		case DD.MAC: {
			Application.switchToMacOSPaths();
			break;
		}
		default: {
			if(StartUp._DEBUG)System.out.println("Unable to detect OS: \""+DD.OS+"\""); break;}
		}
	}
	public static int getCrtOS() {
		ArrayList<String> os_Names=new ArrayList<String>();
		os_Names.add("Windows");
		os_Names.add("Linux");
		String osName= System.getProperty("os.name");
		int ch=0;
		if (osName.contains(os_Names.get(0))) ch=1;
		else if (osName.contains(os_Names.get(1))) ch=2;
		else if (osName.contains("Mac OS X")) ch=3;
		switch (ch) {
		case 1:{
			return DD.WINDOWS;
			}
		case 2: {
			return DD.LINUX;
			}
		case 3: {
			return DD.MAC;
		}
		default: {
			if(StartUp._DEBUG)System.out.println("Unable to detect OS: \""+osName+"\"");
			return -1;
			}
		}
	}
	/**
	 * Fills the DS.OS variable with the current os
	 */
	public static void detect_OS_and_store_in_DD_OS_var() {
		ArrayList<String> os_Names=new ArrayList<String>();
		os_Names.add("Windows");
		os_Names.add("Linux");
		String osName= System.getProperty("os.name");
		int ch=0;
		if (osName.contains(os_Names.get(0))) ch=1;
		else if (osName.contains(os_Names.get(1))) ch=2;
		else if (osName.contains("Mac OS X")) ch=3;
		switch (ch) {
		case 1:{
			DD.OS = DD.WINDOWS;
			break;}
		case 2: {
			DD.OS = DD.LINUX;
			break;}
		case 3: {
			DD.OS = DD.MAC;
			break;
		}
		default: {
			if(StartUp._DEBUG)System.out.println("Unable to detect OS: \""+osName+"\""); break;}
		}
	}
	public static void initBroadcastSettings() throws P2PDDSQLException {
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: plugins loaded, go wireless");
		String wlans = net.ddp2p.common.wireless.Detect_interface.detect_wlan(); 
		DD.setAppText(DD.APP_NET_INTERFACES,wlans);
		try {
			WirelessLog.init();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Random r = new Random();
		byte[] byteArray = new byte[8];    
		r.nextBytes(byteArray);
		DD.Random_peer_Number = byteArray;
	}
	/**
	 * Starts server, simulator, broadcast client, and stored data (IP, etc)
	 * @throws P2PDDSQLException
	 */
	public static void initBroadcastServers() throws P2PDDSQLException {
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stat updates");
		if (wireless_server_on_start) BroadcastConsummerBuffer.queue = new BroadcastConsummerBuffer();
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: started adhoc server consummer");
		if (wireless_server_on_start) DD.setBroadcastServerStatus(true);
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stat adhoc server");
		if (wireless_client_on_start) DD.setBroadcastClientStatus(true);
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stat client server");
		if (wireless_simulator_on_start) { DD.setSimulatorStatus(true); }
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stats ended");
		DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP = DD.getExactAppText("WIRELESS_ADHOC_DD_NET_BROADCAST_IP");
		if(DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP==null) DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP = DD.DEFAULT_WIRELESS_ADHOC_DD_NET_BROADCAST_IP;
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: IPBase def = "+DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
		DD.WIRELESS_ADHOC_DD_NET_IP_BASE = DD.getExactAppText("WIRELESS_ADHOC_DD_NET_IP_BASE");
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: IPBase db = "+DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
		if(DD.WIRELESS_ADHOC_DD_NET_IP_BASE==null) DD.WIRELESS_ADHOC_DD_NET_IP_BASE = DD.DEFAULT_WIRELESS_ADHOC_DD_NET_IP_BASE;
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: IPBase upd = "+DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
		DD.WIRELESS_ADHOC_DD_NET_MASK = DD.getExactAppText("WIRELESS_ADHOC_DD_NET_MASK");
		if(DD.WIRELESS_ADHOC_DD_NET_MASK==null) DD.WIRELESS_ADHOC_DD_NET_MASK = DD.DEFAULT_WIRELESS_ADHOC_DD_NET_MASK;
		DD.DD_SSID = DD.getExactAppText("DD_SSID");
		if(DD.DD_SSID==null) DD.DD_SSID = DD.DEFAULT_DD_SSID;		
	}
	/**
	 * Clients/Servers/Directory/Updates
	 * @throws NumberFormatException
	 * @throws P2PDDSQLException
	 */
	public static void initServers_no_GUI() throws NumberFormatException, P2PDDSQLException {
		if (directory_server_on_start){
			DD.startDirectoryServer(true, -1);
		}
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stat dir");
		if (data_userver_on_start){
			DD.startUServer(true, Application.getCurrent_Peer_ID());
		}
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stat userver");
		if (data_server_on_start){
			DD.startServer(true, Application.getCurrent_Peer_ID());
		}
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stat tcpserver");
		if (data_client_on_start){
			DD.startClient(true);
		}
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stat client");
		if (data_client_updates_on_start) {
			ClientUpdates.startClient(true, false);
		}
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: stat nat");
		if (data_nserver_on_start){
			DD.startNATServer(true);
		}
	}
	/**
	 * Establishes whether the servers/clients should be running or not, based on database
	 * @throws P2PDDSQLException
	 */
	public static void init_startup() throws P2PDDSQLException {
		DD.RELEASE = false;
		DD.ClientTCP=DD.getAppBoolean(DD.APP_ClientTCP);
		DD.ClientUDP=!DD.getAppBoolean(DD.APP_NON_ClientUDP);
		DD.ClientNAT=!DD.getAppBoolean(DD.APP_NON_ClientNAT);
		Broadcasting_Probabilities.initFromDB();
		DD.serveDataDirectly(DD.getAppBoolean(DD.SERVE_DIRECTLY));
		directory_server_on_start = DD.getAppBoolean(DD.DD_DIRECTORY_SERVER_ON_START);
		data_userver_on_start = DD.DD_DATA_USERVER_ON_START_DEFAULT^DD.getAppBoolean(DD.DD_DATA_USERVER_INACTIVE_ON_START);
		data_nserver_on_start = DD.DD_DATA_NSERVER_ON_START_DEFAULT^DD.getAppBoolean(DD.DD_DATA_NSERVER_INACTIVE_ON_START);
		data_server_on_start = DD.getAppBoolean(DD.DD_DATA_SERVER_ON_START);
		data_client_on_start = DD.DD_DATA_CLIENT_ON_START_DEFAULT^DD.getAppBoolean(DD.DD_DATA_CLIENT_INACTIVE_ON_START);
		data_client_updates_on_start = DD.DD_DATA_CLIENT_UPDATES_ON_START_DEFAULT^DD.getAppBoolean(DD.DD_DATA_CLIENT_UPDATES_INACTIVE_ON_START);
		wireless_server_on_start = DD.getAppBoolean(DD.DD_WIRELESS_SERVER_ON_START);
		wireless_client_on_start = DD.getAppBoolean(DD.DD_CLIENT_SERVER_ON_START);
		wireless_simulator_on_start = DD.getAppBoolean(DD.DD_SIMULATOR_ON_START);
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: got servers status: db updates inactive="+DD.getAppBoolean(DD.DD_DATA_CLIENT_UPDATES_INACTIVE_ON_START));
		if(StartUp.DEBUG) System.out.println("StartUpThread:run: got servers status: updates="+Util.bool2StringInt(data_client_updates_on_start));
		if(DD.ClientUDP) data_userver_on_start = true;
		if(DD.ClientNAT) data_nserver_on_start = true;
	}
}
