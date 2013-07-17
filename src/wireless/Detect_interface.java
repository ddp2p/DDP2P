/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 
		Author: Osamah Dhannoon
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
package wireless;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.regex.Pattern;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import util.DBInterface;
import util.Util;


public class Detect_interface {
	final static boolean DEBUG = false;
	final static boolean _DEBUG = true;
	public static int Windows = 1;
	public static int Linux = 2;
	public static int Mac = 3;
	public static final String WINDOWS_NO_IP = "No IP"; // to signal no IP4 in ipconfig
	public Detect_interface() throws  IOException, P2PDDSQLException{
		String wlan = detect_os_and_load_wireless_data();
		//Application.db=new DBInterface("deliberation-app.db");
		
		BroadcastServer.updateInterfaces(wlan);
		
		DD.setAppText(DD.APP_NET_INTERFACES,wlan);
	}
	/**
	 *  detect OS and initial SSIDs
	 *  SSID is stored in Application table row INTERFACES 
	 *  in format:
	 *  wlan0:163.118.242.179:PathAIR:1
	 *  i.e.
	 *  Interface_name:IP:SSID:boolean_connected
	 * @return
	 * @throws P2PDDSQLException 
	 */
	static public String detect_wlan() throws P2PDDSQLException {
		try{
			return detect_os_and_load_wireless_data();
		}catch(IOException e){e.printStackTrace();}
		return null;
	}
	/**
	 * Compare System property with strings
	 * Windows (contained), Linux(contained), MAX OS X (contained)
	 * 
	 * then load wireless data
	 * @return
	 * @throws IOException
	 * @throws P2PDDSQLException 
	 */
	public static String detect_os_and_load_wireless_data()  throws IOException, P2PDDSQLException{
		String result = null;
		ArrayList<String> os_Names=new ArrayList<String>();
		os_Names.add("Windows");
		os_Names.add("Linux");
		String osName= System.getProperty("os.name");
		
		int ch=0;
		if(osName.contains(os_Names.get(0))) ch=Windows;
		else if(osName.contains(os_Names.get(1))) ch=Linux;
		else if(osName.contains("Mac OS X")) ch=Mac;
		switch(ch){
		case 1:{
			Application.switchToWindowsPaths();
			DD.OS = DD.WINDOWS;
			result = win_detect_interf();
			break;}
		case 2: {
			Application.switchToLinuxPaths();
			DD.OS = DD.LINUX;
			result = lin_detect_interf();
			break;}
		case 3: {
			Application.switchToMacOSPaths();
			DD.OS = DD.MAC;
			result = mac_detect_interf();
			break;
			}
			default: { if(DEBUG)System.out.println("Unable to detect OS: \""+osName+"\""); break;}
		}

		return result;
	}
	/**
	 * Should load wireless data 
	 * @return
	 * @throws IOException
	 * @throws P2PDDSQLException 
	 */
	static public String win_detect_interf() throws IOException, P2PDDSQLException{

		if(DEBUG)System.out.println("It's Windows !");
		ArrayList<InterfaceData> w_info ;
		w_info=Win_wlan_info.process();  // this should get the data from the system in a given format
		if(DEBUG)System.out.println("Detect_interface : win_detect_interf() : "+w_info);
		String bcast1="";
		HashSet<String> selected = getSelectedInterfaces();
		boolean running = false;
		ArrayList<String> Int = new ArrayList<String>();
		InterfaceData entry_test_ip;
		
		for(int i=0;i<w_info.size();i++){
			InterfaceData entry = w_info.get(i);
			if(entry==null) continue;
			String SSID = entry.SSID;
			if(DEBUG) System.out.println("entry only : "+entry);
			if(SSID.compareTo(DD.DD_SSID)==0) {
				if(selected.contains(entry.interface_name))
					bcast1=bcast1+entry+":"+"1";
				else
					bcast1=bcast1+entry+":"+"0";
				if(Util.ip_compatible_with_network_mask(entry.IP, DD.WIRELESS_ADHOC_DD_NET_IP_BASE, DD.WIRELESS_ADHOC_DD_NET_MASK)){
					entry_test_ip = Win_wlan_info.get_ip_from_ipconfig(entry.interface_name, entry.SSID);
					if(DEBUG)System.out.println("entry_test_ip : "+entry_test_ip.IP);
					if(entry_test_ip.IP.compareTo(Detect_interface.WINDOWS_NO_IP)==0)
						BroadcastServer.initAddresses();
					else{
						running = true;
						Int.add(entry.interface_name);	
					}
				}
			}
			else bcast1=bcast1+entry+":"+"0";
			if(i+1<w_info.size()){ bcast1=bcast1+",";  }
		}
		if(running) {
			for(int i=0;i<Int.size();i++)
			initiat_broadcast_win(Int.get(i));
		}
		if(DEBUG)System.out.println("Detect_interface:win_detect_interf result="+bcast1);
		return bcast1;
	}
	
	public static HashSet<String> currentlySelected = null;
	public static HashSet<String> getSelectedInterfaces() throws P2PDDSQLException{
		if(DEBUG)System.out.println("Detect_interface:getSelectedInterfaces: start");
		if(currentlySelected!=null){
			if(DEBUG){
				String _selected = Util.concat(currentlySelected.toArray(), DD.WIRELESS_SELECTED_INTERFACES_SEP);
				System.out.println("Detect_interface:getSelectedInterfaces: get "+_selected);
			}
			return currentlySelected;
		}
		// Get stored selected intefaces into selected
		String running_interfaces = DD.getAppText(DD.WIRELESS_SELECTED_INTERFACES);
		if(DEBUG)System.out.println("Detect_interface:getSelectedInterfaces: get dir from DB "+running_interfaces);
		String[] interfaces = new String[0];
		if(running_interfaces!=null)
			interfaces = running_interfaces.split(Pattern.quote(DD.WIRELESS_SELECTED_INTERFACES_SEP));
		HashSet<String> selected = new HashSet<String>();//interfaces);
		for(String s : interfaces) selected.add(s);
		if(DEBUG){
			String _selected = Util.concat(selected.toArray(), DD.WIRELESS_SELECTED_INTERFACES_SEP);
			System.out.println("Detect_interface:getSelectedInterfaces: get from DB "+_selected);
		}
		return currentlySelected = selected;
	}
	public static void setSelectedInterfaces(HashSet<String> selected) throws P2PDDSQLException {
		setSelectedInterfaces(selected, true);
	}
	public static void setSelectedInterfaces(HashSet<String> selected, boolean sync) throws P2PDDSQLException {
		if(DEBUG)System.out.println("Detect_interface:setSelectedInterfaces: start");
		String _selected = Util.concat(selected.toArray(), DD.WIRELESS_SELECTED_INTERFACES_SEP);
		if(sync) DD.setAppText(DD.WIRELESS_SELECTED_INTERFACES, _selected);
		else DD.setAppTextNoSync(DD.WIRELESS_SELECTED_INTERFACES, _selected);
		if(DEBUG)System.out.println("Detect_interface:setSelectedInterfaces: set "+_selected);
	}
	private static String lin_detect_interf() throws IOException, P2PDDSQLException {
		if(DEBUG)System.out.println("It's Linux !");
		ArrayList<InterfaceData> Lin_wlan;
		Lin_wlan=Linux_wlan_info.process();
		String bcast1="";
		HashSet<String> selected = getSelectedInterfaces();
		boolean running = false;
		
		for(int i=0;i<Lin_wlan.size();i++){
			InterfaceData entry = Lin_wlan.get(i);
			if(entry == null) continue;
			String SSID = entry.SSID; 
			if(DEBUG) System.out.println("SSID only : "+SSID);
			if(SSID.compareTo(DD.DD_SSID)==0) {
				if(selected.contains(entry.interface_name))
					bcast1=bcast1+entry+":"+"1";
				else 
					bcast1=bcast1+entry+":"+"0";
				if(Util.ip_compatible_with_network_mask(entry.IP, DD.WIRELESS_ADHOC_DD_NET_IP_BASE, DD.WIRELESS_ADHOC_DD_NET_MASK))
					running = true;
			}
			else bcast1=bcast1+entry+":"+"0";
			if(i+1<Lin_wlan.size()){ bcast1=bcast1+",";  }
		}
		if(running) initiat_broadcast_linux();
		if(DEBUG)System.out.println("Detect_interface:lin_detect: result="+bcast1);
		return bcast1;
	}

	private static String mac_detect_interf() throws IOException {
		
		if(DEBUG)System.out.println("Detect_interface:mac_detect_interf: It's Mac !");
		ArrayList<String> Mac_wlan = new ArrayList<String>();
		Mac_wlan_info p1=new Mac_wlan_info();
		Mac_wlan=p1.process();
		 
		 //System.out.println(Lin_wlan);
		 String bcast1=new String();
                 
		 for(int i=0;i<Mac_wlan.size();i++){
			 bcast1=bcast1+Mac_wlan.get(i)+":"+"false";
			 if(i+1<Mac_wlan.size()){ bcast1=bcast1+",";  }
		 }
		 if(DEBUG)System.out.println("Detect_interface:mac_detect_interf: result="+bcast1);

		return bcast1;
	}
	
	// to get the nth occurrence of a char 
	public static int nthOccurrence(String str, char c, int n) {
		if(str==null) return -1;
	    int pos = str.indexOf(c, 0);
	    while (n-- > 0 && pos != -1)
	        pos = str.indexOf(c, pos+1);
	    return pos;
	}
	
	
	public static void main(String[] args) throws  IOException, P2PDDSQLException {
		//Detect_interface d=new Detect_interface();
	}
	
	
	private static void initiat_broadcast_linux() throws SocketException {
		if(DEBUG)System.out.println(" Detect_interface : initiat_broadcast_linux");
		BroadcastServer.initAddresses();
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface networkInterface = interfaces.nextElement();
			if (networkInterface.isLoopback())
				continue;    // Don't want to broadcast to the loopback interface
			for (InterfaceAddress interfaceAddress :
				networkInterface.getInterfaceAddresses()) {
				if(DEBUG)System.out.println("DetectInterface: initiat_broadcast_linux: interface address :"+interfaceAddress);
				String Host_IP = interfaceAddress.getAddress().toString();
				if(Host_IP==null) continue;
				if(!Util.ip_compatible_with_network_mask(Util.get_IP_from_SocketAddress(Host_IP), DD.WIRELESS_ADHOC_DD_NET_IP_BASE, DD.WIRELESS_ADHOC_DD_NET_MASK)) {
					// warning to be commented out later
					if(DEBUG)System.out.println("Really incompatible?"+ Host_IP);
					//Application.warning(Util._("??")+Host_IP, Util._("Incompatible address"));
					continue;
				}
				String IP = Util.get_IP_from_SocketAddress(Host_IP);
				String ia = interfaceAddress.toString();
				if(DEBUG)System.out.println("DetectInterface: initiat_broadcast_linux: Host_IP :"+Host_IP);
				if(DEBUG)System.out.println("DetectInterface: initiat_broadcast_linux: IP :"+IP);
				if(DEBUG)System.out.println("DetectInterface: initiat_broadcast_linux: addr :"+ia);
				InetAddress broadcast = interfaceAddress.getBroadcast();
				if (broadcast == null){
					Application.warning(Util._("Cannot found broadcast address:")+ia, Util._("No address for warning"));
					continue;
				}
				if(DEBUG)System.out.println("DetectInterface: initiat_broadcast_linux: broadcasts :"+broadcast);
				String iP_Mask = IP+"/"+DD.WIRELESS_ADHOC_DD_NET_MASK;
				
				BroadcastServer.addBroadcastAddressStatic(new BroadcastInterface(broadcast), iP_Mask,networkInterface.getDisplayName());
			}
		}
	}
	
	private static void initiat_broadcast_win(String Int_name) throws P2PDDSQLException, IOException {
		if(_DEBUG)System.out.println(" Detect_interface : initiat_broadcast_win");
		BroadcastServer.initAddresses();
		InterfaceData id = new InterfaceData();
		id = Win_wlan_info.get_ip_from_netsh(Int_name);
		String IP = id.IP;
		String MASK =DD.WIRELESS_ADHOC_DD_NET_MASK;
		if(DEBUG)System.out.println(" Detect_interface : initiat_broadcast_win : broadcast_add : "+IP+"/"+MASK);
		InetAddress broadcast = InetAddress.getByAddress(Util.get_broadcastIP_from_IP_and_NETMASK(IP, MASK));
		BroadcastServer.addBroadcastAddressStatic(new BroadcastInterface(broadcast), IP,Int_name);
	}
	 
}
