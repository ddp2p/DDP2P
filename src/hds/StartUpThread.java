/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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

package hds;

import static java.lang.System.out;
import static util.Util._;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import plugin_data.PluginObject;

import com.almworks.sqlite4java.SQLiteException;

import simulator.WirelessLog;
import streaming.OrgHandling;
import util.DBInterface;
import util.Util;
import wireless.Broadcasting_Probabilities;
import wireless.Detect_interface;
import config.Application;
import config.DD;
import config.DDIcons;
import config.Identity;
import data.D_PluginInfo;

/**
 * This class starts the services configured in the application table
 * @author silaghi
 *
 */
public class StartUpThread extends Thread {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	TrayIcon trayIcon;
	public StartUpThread() {
		
	}
	
	public void registerTray(){
        if(DEBUG)System.err.println("TrayIcon support checked.");


		if (SystemTray.isSupported()) {
		       if(DEBUG)System.err.println("TrayIcon supported.");

		    SystemTray tray = SystemTray.getSystemTray();
		    //Image image = Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"favicon.ico"));
		    Image image = Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"angry16.png"));
		    if(image==null) System.err.println("TrayIcon image failed.");
		    //Image image = Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"favicon.ico"));
		    //Image image = DDIcons.getImageIconFromResource("favicon.ico", "P2P Direct Democracy");//Toolkit.getDefaultToolkit().getImage("tray.gif");

		    MouseListener mouseListener = new MouseListener() {
		                
		        public void mouseClicked(MouseEvent e) {
		            System.out.println("Tray Icon - Mouse clicked!");                 
		        }

		        public void mouseEntered(MouseEvent e) {
		            System.out.println("Tray Icon - Mouse entered!");                 
		        }

		        public void mouseExited(MouseEvent e) {
		            System.out.println("Tray Icon - Mouse exited!");                 
		        }

		        public void mousePressed(MouseEvent e) {
		            System.out.println("Tray Icon - Mouse pressed!");                 
		        }

		        public void mouseReleased(MouseEvent e) {
		            System.out.println("Tray Icon - Mouse released!");                 
		        }
		    };

		    ActionListener exitListener = new ActionListener() {
		        public void actionPerformed(ActionEvent e) {
		            System.out.println("Exiting...");
		            System.exit(0);
		        }
		    };
		            
		    PopupMenu popup = new PopupMenu();
		    MenuItem defaultItem = new MenuItem("Exit");
		    defaultItem.addActionListener(exitListener);
		    popup.add(defaultItem);

		    trayIcon = new TrayIcon(image, "P2P Direct Democracy", popup);

		    ActionListener actionListener = new ActionListener() {
				@Override
		        public void actionPerformed(ActionEvent e) {
		            trayIcon.displayMessage("Action Event", 
		                "An Action Event Has Been Performed!",
		                TrayIcon.MessageType.INFO);
		        }
		    };
		            
		    trayIcon.setImageAutoSize(true);
		    trayIcon.addActionListener(actionListener);
		    trayIcon.addMouseListener(mouseListener);

		    try {
		        tray.add(trayIcon);
		    } catch (AWTException e) {
		        System.err.println("TrayIcon could not be added.");
		    }
		    
//		    trayIcon.displayMessage("Finished downloading", 
//		            "Your Java application has finished downloading",
//		            TrayIcon.MessageType.INFO);

		} else {

	        System.err.println("TrayIcon not supported.");
		    //  System Tray is not supported

		}
		
		DD.frame.setIconImage(Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"favicon.ico")));
		List<Image> icons = new ArrayList<Image>();
		//icons.add(Toolkit.getDefaultToolkit().getImage("p2pdd_resources/folder_images_256.png"));
		icons.add(Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"folder_images_256.png")));
		icons.add(Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"folder_32x32.png")));
		icons.add(Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"favicon.ico")));
		icons.add(Toolkit.getDefaultToolkit().getImage(DD.class.getResource(Application.RESOURCES_ENTRY_POINT+"happy.smiley19.gif")));
		DD.frame.setIconImages(icons);
	}
	
	/**
	 * Should be called only from dispatcher
	 */
	public void initQueuesStatus(){
		if(DEBUG) System.out.println("StartUpThread:initQueuesStatus: start");
		if(_DEBUG) if(!EventQueue.isDispatchThread()) Util.printCallPath("Not dispatcher");
			DD.controlPane.q_MD.removeItemListener(DD.controlPane);
			DD.controlPane.q_C.removeItemListener(DD.controlPane);
			DD.controlPane.q_RA.removeItemListener(DD.controlPane);
			DD.controlPane.q_RE.removeItemListener(DD.controlPane);
			DD.controlPane.q_BH.removeItemListener(DD.controlPane);
			DD.controlPane.q_BR.removeItemListener(DD.controlPane);
			DD.controlPane.q_MD.setSelected(Broadcasting_Probabilities._broadcast_queue_md);
			DD.controlPane.q_C.setSelected(Broadcasting_Probabilities._broadcast_queue_c);
			DD.controlPane.q_RA.setSelected(Broadcasting_Probabilities._broadcast_queue_ra);
			DD.controlPane.q_RE.setSelected(Broadcasting_Probabilities._broadcast_queue_re);
			DD.controlPane.q_BH.setSelected(Broadcasting_Probabilities._broadcast_queue_bh);
			DD.controlPane.q_BR.setSelected(Broadcasting_Probabilities._broadcast_queue_br);
			DD.controlPane.q_MD.addItemListener(DD.controlPane);
			DD.controlPane.q_C.addItemListener(DD.controlPane);
			DD.controlPane.q_RA.addItemListener(DD.controlPane);
			DD.controlPane.q_RE.addItemListener(DD.controlPane);
			DD.controlPane.q_BH.addItemListener(DD.controlPane);
			DD.controlPane.q_BR.addItemListener(DD.controlPane);
			
			DD.controlPane.tcpButton.removeItemListener(DD.controlPane);
			DD.controlPane.udpButton.removeItemListener(DD.controlPane);
			DD.controlPane.tcpButton.setSelected(DD.ClientTCP);
			DD.controlPane.udpButton.setSelected(DD.ClientUDP);
			DD.controlPane.tcpButton.addItemListener(DD.controlPane);
			DD.controlPane.udpButton.addItemListener(DD.controlPane);
			
			
			DD.controlPane.serveDirectly.removeItemListener(DD.controlPane);
			DD.controlPane.serveDirectly.setSelected(OrgHandling.SERVE_DIRECTLY_DATA);
			DD.controlPane.serveDirectly.addItemListener(DD.controlPane);
			
			if(DEBUG) System.out.println("StartUpThread:initQueuesStatus: start");
	}
	public void run(){
		//boolean DEBUG = true;
		try{
			DD.ClientTCP=DD.getAppBoolean(DD.APP_ClientTCP);
			DD.ClientUDP=!DD.getAppBoolean(DD.APP_NON_ClientUDP);
			Broadcasting_Probabilities.initFromDB();

			DD.serveDataDirectly(DD.getAppBoolean(DD.SERVE_DIRECTLY));

			
			Runnable initQS = new Runnable(){
				public void run(){initQueuesStatus();}
			};
			EventQueue.invokeLater(initQS);
			
			boolean directory_server_on_start = DD.getAppBoolean(DD.DD_DIRECTORY_SERVER_ON_START);
			boolean data_userver_on_start = DD.DD_DATA_USERVER_ON_START_DEFAULT^DD.getAppBoolean(DD.DD_DATA_USERVER_INACTIVE_ON_START);
			boolean data_server_on_start = DD.getAppBoolean(DD.DD_DATA_SERVER_ON_START);
			boolean data_client_on_start = DD.DD_DATA_CLIENT_ON_START_DEFAULT^DD.getAppBoolean(DD.DD_DATA_CLIENT_INACTIVE_ON_START);
			boolean data_client_updates_on_start = DD.DD_DATA_CLIENT_UPDATES_ON_START_DEFAULT^DD.getAppBoolean(DD.DD_DATA_CLIENT_UPDATES_INACTIVE_ON_START);
			boolean wireless_server_on_start = DD.getAppBoolean(DD.DD_WIRELESS_SERVER_ON_START);
			boolean wireless_client_on_start = DD.getAppBoolean(DD.DD_CLIENT_SERVER_ON_START);
			boolean wireless_simulator_on_start = DD.getAppBoolean(DD.DD_SIMULATOR_ON_START);
			
			if(DEBUG) System.out.println("StartUpThread:run: got servers status: db updates inactive="+DD.getAppBoolean(DD.DD_DATA_CLIENT_UPDATES_INACTIVE_ON_START));
			if(DEBUG) System.out.println("StartUpThread:run: got servers status: updates="+Util.bool2StringInt(data_client_updates_on_start));
			
			if(DD.ClientUDP) data_userver_on_start = true;
			
			if(DEBUG) System.out.println("Starting UDP Server");
			//DD.userver= new UDPServer(Server.PORT);
			//DD.userver.start();
			if(DEBUG) System.out.println("StartUpThread:run: will create peer");
			DD.createMyPeerIDIfEmpty();
			if(DEBUG) System.out.println("StartUpThread:run: created peer");
			
			if (directory_server_on_start) DD.controlPane.setDirectoryStatus(true);//startDirectoryServer(true, -1);

			if(DEBUG) System.out.println("StartUpThread:run: stat dir");
			if (data_userver_on_start)DD.controlPane.setUServerStatus(true);
			/*{
				new Thread(){
					public void run(){
						try {
							DD.controlPane.setUServerStatus(true);
						} catch (NumberFormatException e) {
							e.printStackTrace();
						} catch (SQLiteException e) {
							e.printStackTrace();
						}//startUServer(true, Identity.current_peer_ID);
					}
				}.start();
			}*/
			
			if(DEBUG) System.out.println("StartUpThread:run: stat userver");
			if (data_server_on_start)DD.controlPane.setServerStatus(true);
			/*{
				new Thread(){
					public void run(){
						try {
							DD.controlPane.setServerStatus(true);//startServer(true, Identity.current_peer_ID);
						} catch (SQLiteException e) {
							e.printStackTrace();
						}
					}
				}.start();
			}*/

			if(DEBUG) System.out.println("StartUpThread:run: stat tcpserver");
			if (data_client_on_start) DD.controlPane.setClientStatus(true);//startClient(true);
			if(DEBUG) System.out.println("StartUpThread:run: stat client");
			if (data_client_updates_on_start)
				DD.controlPane.setClientUpdatesStatus(true);//startClient(true);
			if(DEBUG) System.out.println("StartUpThread:run: stat updates");
			if (wireless_server_on_start) DD.setBroadcastServerStatus(true);
			/*{
				new Thread(){
					public void run(){
							DD.setBroadcastServerStatus(true);
					}
				}.start();
			}*/
			if(DEBUG) System.out.println("StartUpThread:run: stat adhoc server");
			if (wireless_client_on_start) DD.setBroadcastClientStatus(true);
			if(DEBUG) System.out.println("StartUpThread:run: stat client server");
			if (wireless_simulator_on_start) DD.setSimulatorStatus(true);
			if(DEBUG) System.out.println("StartUpThread:run: stats ended");
			
			DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP = DD.getExactAppText("WIRELESS_ADHOC_DD_NET_BROADCAST_IP");
			if(DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP==null) DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP = DD.DEFAULT_WIRELESS_ADHOC_DD_NET_BROADCAST_IP;

			if(DEBUG) System.out.println("StartUpThread:run: IPBase def = "+DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
			DD.WIRELESS_ADHOC_DD_NET_IP_BASE = DD.getExactAppText("WIRELESS_ADHOC_DD_NET_IP_BASE");
			if(DEBUG) System.out.println("StartUpThread:run: IPBase db = "+DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
			if(DD.WIRELESS_ADHOC_DD_NET_IP_BASE==null) DD.WIRELESS_ADHOC_DD_NET_IP_BASE = DD.DEFAULT_WIRELESS_ADHOC_DD_NET_IP_BASE;
			if(DEBUG) System.out.println("StartUpThread:run: IPBase upd = "+DD.WIRELESS_ADHOC_DD_NET_IP_BASE);

			DD.WIRELESS_ADHOC_DD_NET_MASK = DD.getExactAppText("WIRELESS_ADHOC_DD_NET_MASK");
			if(DD.WIRELESS_ADHOC_DD_NET_MASK==null) DD.WIRELESS_ADHOC_DD_NET_MASK = DD.DEFAULT_WIRELESS_ADHOC_DD_NET_MASK;

			DD.DD_SSID = DD.getExactAppText("DD_SSID");
			if(DD.DD_SSID==null) DD.DD_SSID = DD.DEFAULT_DD_SSID;
			
			EventQueue.invokeLater(new Runnable(){
				public void run(){
					if(Application.controlPane.m_area_ADHOC_BIP==null) return;
					Application.controlPane.m_area_ADHOC_BIP.setText(DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP);
					Application.controlPane.m_area_ADHOC_IP.setText(DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
					if(DEBUG) System.out.println("StartUpThread:run: IPBase later = "+DD.WIRELESS_ADHOC_DD_NET_IP_BASE);
					Application.controlPane.m_area_ADHOC_Mask.setText(DD.WIRELESS_ADHOC_DD_NET_MASK);
					Application.controlPane.m_area_ADHOC_SSID.setText(DD.DD_SSID);
					
					if(Application.panelUpdates!=null) {
						try {
							String testers_count_value=DD.getAppText(DD.UPDATES_TESTERS_THRESHOLD_COUNT_VALUE);
							try{if(testers_count_value != null)
									testers_count_value = ""+Integer.parseInt(testers_count_value);}catch(Exception e){};
							if (testers_count_value == null) testers_count_value = "" + DD.UPDATES_TESTERS_THRESHOLD_COUNT_DEFAULT; 
							Application.panelUpdates.numberTxt.setText(testers_count_value);
							
							String testers_count_weight = DD.getAppText(DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_VALUE);
							try{if(testers_count_weight != null)
								testers_count_weight = ""+Float.parseFloat(testers_count_weight);}catch(Exception e){};
							if (testers_count_weight == null) testers_count_weight = "" + DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_DEFAULT; 							
							Application.panelUpdates.percentageTxt.setText(testers_count_weight);
							if(DD.getAppBoolean(DD.UPDATES_TESTERS_THRESHOLD_WEIGHT)){
								Application.panelUpdates.percentageButton.setSelected(true);
							}else{
								Application.panelUpdates.numberButton.setSelected(true);								
							}
							Application.panelUpdates.absoluteCheckBox.setSelected(!DD.getAppBoolean(DD.UPDATES_TESTERS_THRESHOLDS_RELATIVE));
						} catch (SQLiteException e) {
							e.printStackTrace();
						}
					}
				}
			});

			/*
			String my_plugins = DD.getAppText(DD.APP_INSTALLED_PLUGINS);
			if(my_plugins != null) {
				String plugins_list[] = my_plugins.split(Pattern.quote(DD.APP_INSTALLED_PLUGINS_SEP));
				for (String e : plugins_list) {
					String plugin[] = e.split(Pattern.quote(DD.APP_INSTALLED_PLUGINS_ELEM_SEP));
					// id,name,path,date
					//String id=plugin[0];
					String class_name=plugin[1];
					//String install_path=plugin[2];
					//String date=plugin[3];
					Class plugin_class = Class.forName(DD.APP_INSTALLED_PLUGINS_ROOT + class_name);
					PluginObject plugin_instance = null;
					Object plugin_object = plugin_class.newInstance();
					if(plugin_object instanceof PluginObject) plugin_instance = (PluginObject) plugin_object;
					// the plugin_instance will have to call DD.registerPlugin()
					//DD.running_plugins.put(id, plugin_instance);
				}
			}
			*/
			
			plugin_data.PluginRegistration.loadPlugins(DD.getMyPeerGIDFromIdentity(), DD.getMyPeerName());
			if(DEBUG) System.out.println("StartUpThread:run: plugins loaded, go wireless");
			
			//System.err.println("OS_SEP="+Application.OS_PATH_SEPARATOR);
			String wlans = wireless.Detect_interface.detect_wlan(); // detect OS and initial SSIDs
			DD.setAppText(DD.APP_NET_INTERFACES,wlans);
			//Application.wlan.update(); // automatically done by DBInterface
			
			WirelessLog.init();
			
			if(DEBUG) System.out.println("StartUpThread:run: startThread done, go in Tray");
			registerTray();
			
		}
		catch(Exception e){e.printStackTrace();}
	}
	public static void fill_OS_install_path() throws SQLiteException{
		if(DEBUG) System.out.println("StartUpThread:fill_OS_install_path: start");
		Application.LINUX_INSTALLATION_VERSION_BASE_DIR = DD.getAppText(DD.APP_LINUX_INSTALLATION_PATH);
		if(Application.LINUX_INSTALLATION_VERSION_BASE_DIR==null){
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
		
		if(DEBUG) System.out.println("StartUpThread:run: Application.linux_install_prefix ="+ Application.LINUX_INSTALLATION_VERSION_BASE_DIR);
		
		
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
		if(DEBUG) System.out.println("StartUpThread:fill_OS_install_path: done");
	}
	/*
	public static String detect_OS_fill_path(){
		String result = null;
		ArrayList<String> os_Names=new ArrayList<String>();
		os_Names.add("Windows");
		os_Names.add("Linux");
		String osName= System.getProperty("os.name");

		int ch=0;
		if(osName.contains(os_Names.get(0))) ch=1;
		else if(osName.contains(os_Names.get(1))) ch=2;
		else if(osName.contains("Mac OS X")) ch=3;
		switch(ch){
		case 1:{
			Application.CURRENT_BASE_DIR (Application.WINDOWS_INSTALLATION_DIR);
			Application.CURRENT_SCRIPTS_BASE_DIR (Application.WINDOWS_INSTALLATION_DIR+Application.OS_PATH_SEPARATOR+Application.SCRIPTS_RELATIVE_PATH+Application.OS_PATH_SEPARATOR);
			DD.OS = DD.WINDOWS;
			break;}
		case 2: {
			Application.CURRENT_BASE_DIR (Application.LINUX_INSTALLATION_DIR);
			Application.CURRENT_SCRIPTS_BASE_DIR (Application.LINUX_INSTALLATION_DIR+Application.OS_PATH_SEPARATOR+Application.SCRIPTS_RELATIVE_PATH+Application.OS_PATH_SEPARATOR);
			DD.OS = DD.LINUX;
			break;}
		case 3: {
			Application.CURRENT_BASE_DIR (Application.LINUX_INSTALLATION_DIR);
			Application.CURRENT_SCRIPTS_BASE_DIR (Application.LINUX_INSTALLATION_DIR+Application.OS_PATH_SEPARATOR+Application.SCRIPTS_RELATIVE_PATH+Application.OS_PATH_SEPARATOR);
			DD.OS = DD.MAC;
			break;
		}
		default: {
			if(_DEBUG)System.out.println("Unable to detect OS: \""+osName+"\""); break;}
		}

		return result;
	}
	*/
	public static void fill_OS_scripts_path(){

		switch(DD.OS){
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
			if(_DEBUG)System.out.println("Unable to detect OS: \""+DD.OS+"\""); break;}
		}
	}

	public static void detect_OS_fill_var(){
		ArrayList<String> os_Names=new ArrayList<String>();
		os_Names.add("Windows");
		os_Names.add("Linux");
		String osName= System.getProperty("os.name");

		int ch=0;
		if(osName.contains(os_Names.get(0))) ch=1;
		else if(osName.contains(os_Names.get(1))) ch=2;
		else if(osName.contains("Mac OS X")) ch=3;
		switch(ch){
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
			if(_DEBUG)System.out.println("Unable to detect OS: \""+osName+"\""); break;}
		}
	}

}
