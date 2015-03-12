/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package config;
import static util.Util.__;

import java.io.File;
import java.util.regex.Pattern;

import simulator.Fill_database;
import util.DBInterface;
import util.Util;
import wireless.BroadcastClient;
import wireless.BroadcastServer;
import hds.*;     

public class Application{
	public static String USERNAME = System.getProperty("user.name");
	public static String DIRECTORY_FILE = "directory-app.db";
	public static final String DEFAULT_DELIBERATION_FILE = "deliberation-app.db";
	public static String DELIBERATION_FILE = DEFAULT_DELIBERATION_FILE;
	//public static String CURRENT_DELIBERATION_FILE = null;
	static public DBInterface db;
	static public DBInterface db_dir;
	static public DirectoryServer g_DirectoryServer;
	static public Server g_TCPServer;
	static public UDPServer g_UDPServer; //have to unify with the one in DD
	static public BroadcastServer g_BroadcastServer = null; // reference to the unique BroadcastServer
	static public BroadcastClient g_BroadcastClient = null; // reference to the unique BroadcastClient
	static public IClient g_PollingStreamingClient;
	static public Directories_View directory_status=null;
	static public Peers_View peers=null;
	//public static String my_global_peer_ID;
	//public static String my_peer_name;
	//public static String my_peer_slogan;


	//public static WLAN_widget wlan=null;
	/**
	 * ActionListener
	 */
	public static Object appObject;
	public static Fill_database g_Simulator;
	public static DirectoriesData_View directoriesData;
	public static String DB_PATH = null;

	
	public static final String SCRIPTS_RELATIVE_PATH="scripts";
	public static final String SCRIPT_LINUX_SUID_DISCONNECT_ADHOC = "script_linux_suid_disconnect_adhoc";
	public static final String SCRIPT_LINUX_SUID_CONNECT_ADHOC = "script_linux_suid_connect_adhoc";

	public static final String SCRIPT_WINDOWS_WIRELESS_DETECT_SSID = "SCRIPT_WIN_WIRELESS_DETECT_SSID.bat";
	public static final String SCRIPT_WINDOWS_WIRELESS_DETECT_SSID_RAW = "SCRIPT_WIN_WIRELESS_DETECT_SSID_RAW.bat";
	public static final String ADHOC_WINDOWS_PROFILE_FILE = "Wireless Network Connection-DirectDemocracy.xml";
	public static final String SCRIPT_WINDOWS_ADMIN_SET_WIRELESS_DD_IP = "SCRIPT_WINDOWS_ADMIN_SET_WIRELESS_DD_IP.bat";
	public static final String SCRIPT_WINDOWS_ADMIN_RECONNECT_WIRELESS_DHCP = "SCRIPT_WINDOWS_ADMIN_RECONNECT_WIRELESS_DHCP.bat";
	//public static final String SCRIPT_WINDOWS_RECONNECT_OLD_WIRELESS_CONFIG = "SCRIPT_WINDOWS_RECONNECT_OLD_WIRELESS_CONFIG.bat";

	public static final String RESOURCES_ENTRY_POINT = "/p2pdd_resources/";
	public static final String PLUGIN_ENTRY_POINT = "dd_p2p.plugin.Main";
	public static final String USER_CURRENT_DIR = System.getProperty("user.dir");
	public static final String USER_HOME_DIR = System.getProperty("user.home");
	public static final String APP_INSTALLED_PLUGINS_ROOT_FOLDER = "plugins";
	public static final String WIRELESS_THANKS = "wireless_thanks.wav"; // in scripts
	
	static public final String OS_LINE_SEPARATOR = System.getProperty("line.separator", "\r\n");
	static public final String OS_PATH_SEPARATOR = System.getProperty("file.separator", "/");

	public static String WINDOWS_INSTALLATION_ROOT_BASE_DIR="";
	public static String WINDOWS_INSTALLATION_VERSION_BASE_DIR="";
	public static String WINDOWS_SCRIPTS_BASE_DIR="";
	static public String WINDOWS_DATABASE_DIR = "";
	static public String WINDOWS_PLUGINS_BASE_DIR = "";
	static public String WINDOWS_LOGS_BASE_DIR = "";
	static public String WINDOWS_DD_JAR_BASE_DIR = "";
	
	public static String LINUX_INSTALLATION_ROOT_BASE_DIR="";
	static public String LINUX_INSTALLATION_VERSION_BASE_DIR = "";//DD.getAppText(DD.SCRIPT_WIRELESS_LINUX_PATH);
	static public String LINUX_SCRIPTS_BASE_DIR = "";
	static public String LINUX_DATABASE_DIR = "";
	static public String LINUX_PLUGINS_BASE_DIR = "";
	static public String LINUX_LOGS_BASE_DIR = "";
	static public String LINUX_DD_JAR_BASE_DIR = "";

	//static public String CURRENT_INSTALLATION_DIR = "";//DD.getAppText(DD.SCRIPT_WIRELESS_LINUX_PATH);
	private static String CURRENT_INSTALLATION_ROOT_BASE_DIR = null;
	private static String CURRENT_INSTALLATION_VERSION_BASE_DIR = null;
	private static String CURRENT_SCRIPTS_BASE_DIR = null; //Application.linux_scripts_prefix+Application.scripts_path
	static private String CURRENT_DATABASE_DIR = "";
	static private String CURRENT_PLUGINS_BASE_DIR = "";
	static private String CURRENT_LOGS_BASE_DIR = "";
	static private String CURRENT_DD_JAR_BASE_DIR = "";

	public static void switchToWindowsPaths() {
		Application.CURRENT_INSTALLATION_ROOT_BASE_DIR (Application.WINDOWS_INSTALLATION_ROOT_BASE_DIR);
		Application.CURRENT_INSTALLATION_VERSION_BASE_DIR (Application.WINDOWS_INSTALLATION_VERSION_BASE_DIR);
		Application.CURRENT_SCRIPTS_BASE_DIR (Application.WINDOWS_SCRIPTS_BASE_DIR);
		Application.CURRENT_DATABASE_DIR (Application.WINDOWS_DATABASE_DIR);
		Application.CURRENT_PLUGINS_BASE_DIR (Application.WINDOWS_PLUGINS_BASE_DIR);
		Application.CURRENT_LOGS_BASE_DIR (Application.WINDOWS_LOGS_BASE_DIR);
		Application.CURRENT_DD_JAR_BASE_DIR (Application.WINDOWS_DD_JAR_BASE_DIR);
	}
	public static void switchToLinuxPaths() {
		Application.CURRENT_INSTALLATION_ROOT_BASE_DIR (Application.LINUX_INSTALLATION_ROOT_BASE_DIR);
		Application.CURRENT_INSTALLATION_VERSION_BASE_DIR (Application.LINUX_INSTALLATION_VERSION_BASE_DIR);
		Application.CURRENT_SCRIPTS_BASE_DIR (Application.LINUX_SCRIPTS_BASE_DIR);
		Application.CURRENT_DATABASE_DIR (Application.LINUX_DATABASE_DIR);
		Application.CURRENT_PLUGINS_BASE_DIR (Application.LINUX_PLUGINS_BASE_DIR);
		Application.CURRENT_LOGS_BASE_DIR (Application.LINUX_LOGS_BASE_DIR);
		Application.CURRENT_DD_JAR_BASE_DIR (Application.LINUX_DD_JAR_BASE_DIR);
		//Application.CURRENT_LOG_BASE_DIR (Application.LINUX_INSTALLATION_VERSION_BASE_DIR);
		//Application.CURRENT_SCRIPTS_BASE_DIR (Application.LINUX_INSTALLATION_VERSION_BASE_DIR+Application.OS_PATH_SEPARATOR+Application.SCRIPTS_RELATIVE_PATH+Application.OS_PATH_SEPARATOR);
	}
	/**
	 * Calls setter functions for this OS
	 */
	public static void switchToMacOSPaths() {
		Application.CURRENT_INSTALLATION_ROOT_BASE_DIR (Application.LINUX_INSTALLATION_ROOT_BASE_DIR);
		Application.CURRENT_INSTALLATION_VERSION_BASE_DIR (Application.LINUX_INSTALLATION_VERSION_BASE_DIR);
		Application.CURRENT_SCRIPTS_BASE_DIR (Application.LINUX_SCRIPTS_BASE_DIR);
		Application.CURRENT_DATABASE_DIR (Application.LINUX_DATABASE_DIR);
		Application.CURRENT_PLUGINS_BASE_DIR (Application.LINUX_PLUGINS_BASE_DIR);
		Application.CURRENT_LOGS_BASE_DIR (Application.LINUX_LOGS_BASE_DIR);
		Application.CURRENT_DD_JAR_BASE_DIR (Application.LINUX_DD_JAR_BASE_DIR);
		//Application.CURRENT_LOG_BASE_DIR (Application.LINUX_INSTALLATION_VERSION_BASE_DIR);
		//Application.CURRENT_SCRIPTS_BASE_DIR (Application.LINUX_INSTALLATION_VERSION_BASE_DIR+Application.OS_PATH_SEPARATOR+Application.SCRIPTS_RELATIVE_PATH+Application.OS_PATH_SEPARATOR);
	}
	
	static public String CURRENT_INSTALLATION_ROOT_BASE_DIR(){return CURRENT_INSTALLATION_ROOT_BASE_DIR;}
	static public void CURRENT_INSTALLATION_ROOT_BASE_DIR(String _CURRENT_INSTALLATION_ROOT_BASE_DIR){CURRENT_INSTALLATION_ROOT_BASE_DIR=_CURRENT_INSTALLATION_ROOT_BASE_DIR;}
	
	static public String CURRENT_INSTALLATION_VERSION_BASE_DIR(){return CURRENT_INSTALLATION_VERSION_BASE_DIR;}
	static public void CURRENT_INSTALLATION_VERSION_BASE_DIR(String _CURRENT_INSTALLATION_VERSION_BASE_DIR){CURRENT_INSTALLATION_VERSION_BASE_DIR=_CURRENT_INSTALLATION_VERSION_BASE_DIR;}
	
	// this may be usable to locate old databases when upgrading
	static public String CURRENT_DATABASE_DIR() {return CURRENT_DATABASE_DIR;}
	static public void CURRENT_DATABASE_DIR(String _CURRENT_DATABASE_DIR) {CURRENT_DATABASE_DIR=_CURRENT_DATABASE_DIR;}
	
	static public String CURRENT_PLUGINS_BASE_DIR(){return CURRENT_PLUGINS_BASE_DIR;}
	static public void CURRENT_PLUGINS_BASE_DIR(String _CURRENT_PLUGINS_BASE_DIR){CURRENT_PLUGINS_BASE_DIR=_CURRENT_PLUGINS_BASE_DIR;}
	
	static public String CURRENT_DD_JAR_BASE_DIR(){return CURRENT_DD_JAR_BASE_DIR;}
	static public void CURRENT_DD_JAR_BASE_DIR(String _CURRENT_DD_JAR_BASE_DIR){CURRENT_DD_JAR_BASE_DIR=_CURRENT_DD_JAR_BASE_DIR;}
	
	public static void CURRENT_SCRIPTS_BASE_DIR(String dir) {
		File crt = new File(dir);
		if (((!crt.exists() || !crt.isDirectory())) &&
				!Util.equalStrings_null_or_not(DD.WARN_OF_INVALID_SCRIPTS_BASE_DIR, dir)) {
			Util.printCallPath("Wrong path: "+dir);
			System.err.println("Script existence: "+ crt.exists());
			System.err.println("Script dir: "+ crt.isDirectory());
			System.err.println("Current folder: "+crt.getAbsolutePath());
			DD.WARN_OF_INVALID_SCRIPTS_BASE_DIR = dir;
		}else{
			DD.WARN_OF_INVALID_SCRIPTS_BASE_DIR = null;
		}
		CURRENT_SCRIPTS_BASE_DIR = dir;
	}
	static boolean CURRENT_SCRIPTS_BASE_DIR_WARNING = true;
	/**
	 * This returns the path with an added separator at the end
	 * @return
	 */
	public static String CURRENT_SCRIPTS_BASE_DIR() {
		if (CURRENT_SCRIPTS_BASE_DIR==null) Util.printCallPath("No path");
		if (CURRENT_SCRIPTS_BASE_DIR_WARNING && (CURRENT_SCRIPTS_BASE_DIR==null)){
			CURRENT_SCRIPTS_BASE_DIR_WARNING = false;
			Application_GUI.warning(__("No path set for scripts. Will be using \"scripts\""), __("No path set!"));
		}
		if (CURRENT_SCRIPTS_BASE_DIR==null) return SCRIPTS_RELATIVE_PATH + Application.OS_PATH_SEPARATOR;
		File crt = new File(CURRENT_SCRIPTS_BASE_DIR);
		if (!crt.exists() || !crt.isDirectory()) return ""+SCRIPTS_RELATIVE_PATH + Application.OS_PATH_SEPARATOR;
		if (!CURRENT_SCRIPTS_BASE_DIR.endsWith(Application.OS_PATH_SEPARATOR))
			CURRENT_SCRIPTS_BASE_DIR = CURRENT_SCRIPTS_BASE_DIR + Application.OS_PATH_SEPARATOR;
		return CURRENT_SCRIPTS_BASE_DIR;//+Application.OS_PATH_SEPARATOR;
	}

	public static String CURRENT_LOGS_BASE_DIR() {return CURRENT_LOGS_BASE_DIR;}
	public static void CURRENT_LOGS_BASE_DIR(String dir) {
		CURRENT_LOGS_BASE_DIR = dir;
		//CURRENT_INSTALLATION_VERSION_BASE_DIR = CURRENT_DATABASE_DIR = CURRENT_PLUGINS_BASE_DIR = CURRENT_LOGS_BASE_DIR = CURRENT_DD_JAR_BASE_DIR = dir;
	}
	
	
	public static final String SCRIPT_LINUX_UPGRADE_DB = "SCRIPT_LINUX_UPGRADE_DB.sh";
	public static final String SCRIPT_MACOS_UPGRADE_DB = "SCRIPT_MACOS_UPGRADE_DB.sh";
	public static final String SCRIPT_WINOS_UPGRADE_DB = "SCRIPT_WINOS_UPGRADE_DB.sh";
	public static final String SCRIPT_LINUX_DETECT_WIRELESS_SSID = "SCRIPT_LINUX_DETECT_WIRELESS_SSID.sh";
	public static final String SCRIPT_LINUX_DETECT_WIRELESS_INTERFACES = "SCRIPT_LINUX_DETECT_WIRELESS_INTERFACES.sh";
	//public static final String SCRIPT_LINUX_DETECT_WIRELESS_ESSID = "SCRIPT_LINUX_DETECT_WIRELESS_ESSID.sh";
	public static final String SCRIPT_LINUX_DETECT_WIRELESS_IP = "SCRIPT_LINUX_DETECT_WIRELESS_IP.sh";
	public static final String SCRIPT_MACOS_WLAN_GETINFO = "SCRIPT_MACOS_WLAN_GETINFO.sh";
	public static final String SCRIPT_MACOS_WLAN_GET_WIFI_INTERFACES = "SCRIPT_MACOS_WLAN_GET_WIFI_INTERFACES.sh";
	public static final String SCRIPT_MACOS_WLAN_GET_WIFI_INTERFACE = "SCRIPT_MACOS_WLAN_GET_WIFI_INTERFACE.sh";

	public static final String UPDATES_UNIX_SCRIPT_EXTENSION = ".sh";
	public static final String UPDATES_WIN_SCRIPT_EXTENSION = ".bat";
	public static final String LATEST = "LATEST";
	public static final String PREVIOUS = "PREVIOUS";
	public static final String HISTORY = "HISTORY";
	public static final String RELATIVE_DIR_LOGS = "log_data";
	public static final String JARS = "jars";
	public static final String DD_JAR = "DD.jar"; // in jars: JARS+Application.OS_PATH_SEPARATOR + 
	private static final boolean DEBUG = false;
	public static final String LINUX_PATH_SEPARATOR = "/";
	public static final String WINDOWS_PATH_SEPARATOR = "\\";
	public static final String MISSING_ICON = "missing_icon_22.png";
	
	/**
	 * Parses a string with the Windows directories qualified (separated by ",").
	 * V(base)=...||R(root)=...||S(scripts)=...||P(plugin)=...D(db)=...||L(log)=...||J(jar)=...
	 * 
	 * Fills unknown paths (ones with ":") with paths derived from version
	 * @param val
	 */
	public static void parseWindowsPaths(String val) {
		//boolean DEBUG=true;
		if(DEBUG)System.out.println("Application:setting windows paths: "+val);
		String version = Application.WINDOWS_INSTALLATION_VERSION_BASE_DIR;  // keep default
		String root=":", logs=":", plugins=":", ddjar=":", scripts=":", database=":";
		String[]paths = val.split(Pattern.quote(","));
		for(String p:paths){
			p = p.trim(); //??????? maybe spaces should be allowed at the end (any opinion?)
			if(p.startsWith("R=")){ root=p.substring(2);}
			else if(p.startsWith("V=")){ version=p.substring(2);}
			else if(p.startsWith("S=")){ scripts=p.substring(2);}
			else if(p.startsWith("P=")){ plugins=p.substring(2);}
			else if(p.startsWith("D=")){ database=p.substring(2);}
			else if(p.startsWith("L=")){ logs=p.substring(2);}
			else if(p.startsWith("J=")){ ddjar=p.substring(2);}
			else {Application_GUI.warning(__("Unexpected path component type: "+p.substring(0, 2))+"\n"+__("Should be:")+"\nV=...version,R=..root.,S=...scripts,P=...plugin,D=...db,L=...logs,J=...jars",
					__("Unknown path component type"));return;}
		}
		if(":".equals(version)) {
			Application_GUI.warning(__("The WINDOWS_INSTALLATION_VERSION_BASE_DIR cannot be set to default, as it is the base of default."), __("Invalid input"));
			return;
		}
		if(":".equals(root)){
			root = Util.getParent(version);
			if(root==null) root = version;
		}
		
		
		if(!version.endsWith(Application.WINDOWS_PATH_SEPARATOR)) version = version + Application.WINDOWS_PATH_SEPARATOR;
		//if(!root.endsWith(Application.WINDOWS_PATH_SEPARATOR)) root = root + Application.WINDOWS_PATH_SEPARATOR;
		//if(!plugins.endsWith(Application.WINDOWS_PATH_SEPARATOR)) plugins = plugins + Application.WINDOWS_PATH_SEPARATOR;
		//if(!scripts.endsWith(Application.WINDOWS_PATH_SEPARATOR)) scripts = scripts + Application.WINDOWS_PATH_SEPARATOR;
		//if(!database.endsWith(Application.WINDOWS_PATH_SEPARATOR)) database = database + Application.WINDOWS_PATH_SEPARATOR;
		//if(!logs.endsWith(Application.WINDOWS_PATH_SEPARATOR)) logs = logs + Application.WINDOWS_PATH_SEPARATOR;
		//if(!ddjar.endsWith(Application.WINDOWS_PATH_SEPARATOR)) ddjar = ddjar + Application.WINDOWS_PATH_SEPARATOR;

		
		if(":".equals(scripts)) scripts = version+Application.SCRIPTS_RELATIVE_PATH;
		if(":".equals(plugins)) plugins = version+Application.APP_INSTALLED_PLUGINS_ROOT_FOLDER;
		if(":".equals(database)) database = version;
		if(":".equals(logs)) logs = version+RELATIVE_DIR_LOGS;
		if(":".equals(ddjar)) ddjar = version+JARS;
		if(DEBUG)System.out.println("Application:setting paths: tested':' V="+version+",R="+root+",S="+scripts+",P="+plugins+",D="+database+",L="+logs+",J="+ddjar);
		
		//if(!version.endsWith(Application.WINDOWS_PATH_SEPARATOR)) version = version + Application.WINDOWS_PATH_SEPARATOR;
		if(!root.endsWith(Application.WINDOWS_PATH_SEPARATOR)) root = root + Application.WINDOWS_PATH_SEPARATOR;
		if(!plugins.endsWith(Application.WINDOWS_PATH_SEPARATOR)) plugins = plugins + Application.WINDOWS_PATH_SEPARATOR;
		if(!scripts.endsWith(Application.WINDOWS_PATH_SEPARATOR)) scripts = scripts + Application.WINDOWS_PATH_SEPARATOR;
		if(!database.endsWith(Application.WINDOWS_PATH_SEPARATOR)) database = database + Application.WINDOWS_PATH_SEPARATOR;
		if(!logs.endsWith(Application.WINDOWS_PATH_SEPARATOR)) logs = logs + Application.WINDOWS_PATH_SEPARATOR;
		if(!ddjar.endsWith(Application.WINDOWS_PATH_SEPARATOR)) ddjar = ddjar + Application.WINDOWS_PATH_SEPARATOR;
		
		Application.WINDOWS_INSTALLATION_VERSION_BASE_DIR = version;
		Application.WINDOWS_INSTALLATION_ROOT_BASE_DIR = root;
		Application.WINDOWS_SCRIPTS_BASE_DIR = scripts;
		Application.WINDOWS_PLUGINS_BASE_DIR = plugins;
		Application.WINDOWS_LOGS_BASE_DIR = logs;
		Application.WINDOWS_DATABASE_DIR = database;
		Application.WINDOWS_DD_JAR_BASE_DIR = ddjar;
		
		try{
			DD.setAppText(DD.APP_WINDOWS_INSTALLATION_PATH, Application.WINDOWS_INSTALLATION_VERSION_BASE_DIR);
			DD.setAppText(DD.APP_WINDOWS_INSTALLATION_ROOT_PATH, Application.WINDOWS_INSTALLATION_ROOT_BASE_DIR);
			DD.setAppText(DD.APP_WINDOWS_SCRIPTS_PATH, Application.WINDOWS_SCRIPTS_BASE_DIR);
			DD.setAppText(DD.APP_WINDOWS_PLUGINS_PATH, Application.WINDOWS_PLUGINS_BASE_DIR);
			DD.setAppText(DD.APP_WINDOWS_LOGS_PATH, Application.WINDOWS_LOGS_BASE_DIR);
			DD.setAppText(DD.APP_WINDOWS_DATABASE_PATH, Application.WINDOWS_DATABASE_DIR);
			DD.setAppText(DD.APP_WINDOWS_DD_JAR_PATH, Application.WINDOWS_DD_JAR_BASE_DIR);
		}catch(Exception e){e.printStackTrace();}
	}
	/**
	 * Parses a string with the Linux directories qualified (separated by ",").
	 * V(base)=...||R(root)=...||S(scripts)=...||P(plugin)=...D(db)=...||L(log)=...||J(jar)=...
	 * 
	 * Fills unknown paths (ones with ":") with paths derived from version
	 * @param val
	 */
	public static void parseLinuxPaths(String val) {
		//boolean DEBUG=true;
		if(DEBUG)System.out.println("Application:setting paths: "+val);
		String version = Application.LINUX_INSTALLATION_VERSION_BASE_DIR; // keep default
		String root=":", logs=":", plugins=":", ddjar=":", scripts=":", database=":";
		String[]paths = val.split(Pattern.quote(","));
		for(String p:paths){
			p = p.trim(); //??????? maybe spaces should be allowed at the end (any opinion)
			if(p.startsWith("R=")){ root=p.substring(2);}
			else if(p.startsWith("V=")){ version=p.substring(2);}
			else if(p.startsWith("S=")){ scripts=p.substring(2);}
			else if(p.startsWith("P=")){ plugins=p.substring(2);}
			else if(p.startsWith("D=")){ database=p.substring(2);}
			else if(p.startsWith("L=")){ logs=p.substring(2);}
			else if(p.startsWith("J=")){ ddjar=p.substring(2);}
			else {
				System.out.println("Application:setting paths p: "+val);
				Application_GUI.warning(__("Unexpected path component type: "+p)+"\n"+__("Should be:")+"\nV=...version,R=..root.,S=...scripts,P=...plugin,D=...db,L=...logs,J=...jars",
					__("Unknown path component type"));
				return;
			}
		}
		if(":".equals(version)) {
			Application_GUI.warning(__("The LINUX_INSTALLATION_VERSION_BASE_DIR cannot be set to default, as it is the base of default."), __("Invalid input"));
			return;
		}
		if(":".equals(root)){
			root = Util.getParent(version);
			if(root==null) root = version;
		}
		
		if(!version.endsWith(Application.LINUX_PATH_SEPARATOR)) version = version + Application.LINUX_PATH_SEPARATOR;
		
		if(":".equals(scripts)) scripts = version+Application.SCRIPTS_RELATIVE_PATH;
		if(":".equals(plugins)) plugins = version+Application.APP_INSTALLED_PLUGINS_ROOT_FOLDER;
		if(":".equals(database)) database = version;
		if(":".equals(logs)) logs = version+RELATIVE_DIR_LOGS;
		if(":".equals(ddjar)) ddjar = version+JARS;
		
		if(DEBUG)System.out.println("Application:setting paths: tested':' V="+version+",R="+root+",S="+scripts+",P="+plugins+",D="+database+",L="+logs+",J="+ddjar);

		
		if(!root.endsWith(Application.LINUX_PATH_SEPARATOR)) root = root + Application.LINUX_PATH_SEPARATOR;
		if(!plugins.endsWith(Application.LINUX_PATH_SEPARATOR)) plugins = plugins + Application.LINUX_PATH_SEPARATOR;
		if(!scripts.endsWith(Application.LINUX_PATH_SEPARATOR)) scripts = scripts + Application.LINUX_PATH_SEPARATOR;
		if(!database.endsWith(Application.LINUX_PATH_SEPARATOR)) database = database + Application.LINUX_PATH_SEPARATOR;
		if(!logs.endsWith(Application.LINUX_PATH_SEPARATOR)) logs = logs + Application.LINUX_PATH_SEPARATOR;
		if(!ddjar.endsWith(Application.LINUX_PATH_SEPARATOR)) ddjar = ddjar + Application.LINUX_PATH_SEPARATOR;

		if(DEBUG)System.out.println("Application:setting paths: added/ V="+version+",R="+root+",S="+scripts+",P="+plugins+",D="+database+",L="+logs+",J="+ddjar);
		
		Application.LINUX_INSTALLATION_VERSION_BASE_DIR = version;
		Application.LINUX_INSTALLATION_ROOT_BASE_DIR = root;
		Application.LINUX_SCRIPTS_BASE_DIR = scripts;
		Application.LINUX_PLUGINS_BASE_DIR = plugins;
		Application.LINUX_LOGS_BASE_DIR = logs;
		Application.LINUX_DATABASE_DIR = database;
		Application.LINUX_DD_JAR_BASE_DIR = ddjar;
		
		if(DEBUG)System.out.println("Application:setting paths: setting ="+Application.getCurrentLinuxPathsString());

		
		try{
			DD.setAppText(DD.APP_LINUX_INSTALLATION_PATH, Application.LINUX_INSTALLATION_VERSION_BASE_DIR);
			DD.setAppText(DD.APP_LINUX_INSTALLATION_ROOT_PATH, Application.LINUX_INSTALLATION_ROOT_BASE_DIR);
			DD.setAppText(DD.APP_LINUX_SCRIPTS_PATH, Application.LINUX_SCRIPTS_BASE_DIR);
			DD.setAppText(DD.APP_LINUX_PLUGINS_PATH, Application.LINUX_PLUGINS_BASE_DIR);
			DD.setAppText(DD.APP_LINUX_LOGS_PATH, Application.LINUX_LOGS_BASE_DIR);
			DD.setAppText(DD.APP_LINUX_DATABASE_PATH, Application.LINUX_DATABASE_DIR);
			DD.setAppText(DD.APP_LINUX_DD_JAR_PATH, Application.LINUX_DD_JAR_BASE_DIR);
		}catch(Exception e){e.printStackTrace();}
	}
	/**
	 * Builds a string with the Linux directories qualified (separated by ",").
	 * V(base)=...||R(root)=...||S(scripts)=...||P(plugin)=...D(db)=...||L(log)=...||J(jar)=...
	 * @return
	 */
	public static String getCurrentLinuxPathsString(){
		String SEP=",";
		return getCurrentLinuxPathsString(SEP);
	}
	/**
	 * Builds a string with the Linux directories qualified (separated by separators).
	 * V(base)=...||R(root)=...||S(scripts)=...||P(plugin)=...D(db)=...||L(log)=...||J(jar)=...
	 * @param SEP
	 * @return
	 */
	public static String getCurrentLinuxPathsString(String SEP){
		return
		"V="+Application.LINUX_INSTALLATION_VERSION_BASE_DIR+
		SEP+"R="+Application.LINUX_INSTALLATION_ROOT_BASE_DIR+
		SEP+"S="+Application.LINUX_SCRIPTS_BASE_DIR+
		SEP+"P="+Application.LINUX_PLUGINS_BASE_DIR+
		SEP+"D="+Application.LINUX_DATABASE_DIR+
		SEP+"L="+Application.LINUX_LOGS_BASE_DIR+
		SEP+"J="+Application.LINUX_DD_JAR_BASE_DIR;
	}
	/**
	 * Builds a string with the Windows directories qualified (separated by ",").
	 * V(base)=...||R(root)=...||S(scripts)=...||P(plugin)=...D(db)=...||L(log)=...||J(jar)=...
	 * @return
	 */
	public static String getCurrentWindowsPathsString(){
		String SEP = ",";
		return getCurrentWindowsPathsString(SEP);
	}
	/**
	 * Builds a string with the Windows directories qualified (separated by separators).
	 * V(base)=...||R(root)=...||S(scripts)=...||P(plugin)=...D(db)=...||L(log)=...||J(jar)=...
	 * @param SEP
	 * @return
	 */
	public static String getCurrentWindowsPathsString(String SEP){
		return 
		"V="+Application.WINDOWS_INSTALLATION_VERSION_BASE_DIR+
		SEP+"R="+Application.WINDOWS_INSTALLATION_ROOT_BASE_DIR+
		SEP+"S="+Application.WINDOWS_SCRIPTS_BASE_DIR+
		SEP+"P="+Application.WINDOWS_PLUGINS_BASE_DIR+
		SEP+"D="+Application.WINDOWS_DATABASE_DIR+
		SEP+"L="+Application.WINDOWS_LOGS_BASE_DIR+
		SEP+"J="+Application.WINDOWS_DD_JAR_BASE_DIR;
	}
}