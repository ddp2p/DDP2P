/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 
		Author: Ossamah Dhannoon
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
package widgets.wireless;

import hds.StartUpThread;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Pattern;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import util.DBInterface;
import util.Util;
import wireless.BroadcastInterface;
import wireless.BroadcastServer;
import wireless.Detect_interface;
import wireless.Linux_wlan_info;
import wireless.Process_wlan_information;
class InstructWireless implements Runnable{
	private static final boolean _DEBUG = true;
	String interf;
	int ip_byte;
	public InstructWireless(String _interf, int _ip_byte){interf = _interf; ip_byte = _ip_byte;}
	@Override
	public void run() {
		if(DD.wireless_hints==null){
			if(_DEBUG)System.out.println("WirelessSetup: InstructWireless: no wireless_hints widget");
			return;
		}
		DD.wireless_hints.setText("netsh interface ip set address \""+interf+"\" static 10.0.0."+ip_byte+" 255.0.0.0");
	}
}
class InstructWirelessDHCP implements Runnable{
	private static final boolean _DEBUG = true;
	String interf;
	int ip_byte;
	public InstructWirelessDHCP(String _interf){interf = _interf;}
	@Override
	public void run() {
		if(DD.wireless_hints==null){
			if(_DEBUG)System.out.println("WirelessSetup: InstructWireless: no wireless_hints widget");
			return;
		}
		DD.wireless_hints.setText("netsh int ip set address  \""+interf+"\" dhcp");
	}
}

public class WirelessSetup {
	static boolean ARRAY_EXEC = false;

	private static final boolean DEBUG = false;
	static String s_interf;
	static String comp_IP="0.0.0.0";
	static ArrayList<String> IPs = new ArrayList<String>();
	static String bcast_add;
	public static boolean _DEBUG = true;
	static public void configureInterface(Object interf, boolean configured) throws P2PDDSQLException, IOException, InterruptedException{	
		if(configured) {
			return;
		}
		ArrayList<String> os_Names=new ArrayList<String>();
		os_Names.add("Windows 7");
		os_Names.add("Linux");
		os_Names.add("Windows 8");
		String osName= System.getProperty("os.name");

		int ch=0;
		if(osName.compareTo(os_Names.get(0))==0) ch=1;
		else if(osName.compareTo(os_Names.get(1))==0) ch=2;
		String IP_Mask=null;
		switch(ch){
		case 1:
		case 3: { IP_Mask = Windows_IntConfigure(interf); break;}
		case 2: { IP_Mask = Linux_IntConfigure(interf); break;}
		default: {
			if(osName.contains("Windows")){
				IP_Mask=Windows_IntConfigure(interf);
			}else
			System.out.println("Unable to detect OS: \""+ osName+"\""); break;
			}
		}
		if(IP_Mask!=null){
			if(DEBUG) System.out.println("BroadcastServer:addBroadcastAddressStatic: IP/Mask="+IP_Mask);
			//if(DEBUG) BroadcastServer.addBroadcastAddressStatic(IP_Mask);
			if(DEBUG)System.out.println("WirelessSetup : configureInterface IP : "+IP_Mask);
		}
	}

	static public void interfaceUp(Object interf, boolean configured) throws P2PDDSQLException, IOException, InterruptedException {
		//if(!interfaceConfigured(interf)) 
		configureInterface( interf, configured);

	}

	private static boolean interfaceConfigured(Object interf) {
		return false;
	}

	static public void interfaceDown(Object interf) {

	}
	static public void DisconnectInterface(Object interf) throws IOException, P2PDDSQLException, InterruptedException {

		ArrayList<String> os_Names=new ArrayList<String>();
		os_Names.add("Windows 7");
		os_Names.add("Linux");
		os_Names.add("Windows 8");
		String osName= System.getProperty("os.name");

		int ch=0;
		if(osName.compareTo(os_Names.get(0))==0) ch=1;
		else if(osName.compareTo(os_Names.get(1))==0) ch=2;

		switch(ch){
		case 1:
		case 3:
		{
			disconnectWindows(interf);
			break;
		}
		case 2: {
			String[] linux_comm=new String[]{Application.CURRENT_SCRIPTS_BASE_DIR() +Application.SCRIPT_LINUX_SUID_DISCONNECT_ADHOC};//"start";
			//Runtime.getRuntime().exec(linux_comm);
			new Thread(){
				public void run() {
					Application.warning(Util._("Wait a moment... we are disconnecting from Adhoc!"), Util._("Disconnecting!"));
				}
			}.start();
			Util.getProcessOutput(linux_comm);
			//WlanModel.refresh();
			WlanModel.refreshRepeatedly();
//			for(int k=0; k<15; k++){ // may stop on the first difference!
//				Thread.sleep((k<5)?500:1000);
//				WlanModel.refresh();
//			}
				/*
				ArrayList<String> new_f=new ArrayList<String>();
				Linux_wlan_info p1=new Linux_wlan_info();
				new_f=p1.process();
				String bcast1=new String(); 
				for(int i=0;i<new_f.size();i++){
					bcast1=bcast1+new_f.get(i)+":"+"false";
					if(i+1<new_f.size()){ bcast1=bcast1+",";  }
				}
				if(DEBUG)System.out.println("WirelessSetup.DisconnectInterface : "+bcast1);
				DD.setAppText(DD.APP_NET_INTERFACES,bcast1);
				*/
			break;
		}
		default: {
			if(osName.contains("Windows")){
				disconnectWindows(interf);
			}else
			System.out.println("Unable to detect OS :\""+ osName+"\""); break;
			}
		}


	}
	static public void disconnectWindows(Object interf) throws IOException, P2PDDSQLException, InterruptedException{
		EventQueue.invokeLater(new InstructWirelessDHCP(Util.getString(interf)));
		String _interf=interf.toString();
		//String disconnect="netsh wlan disconnect interface=\""+_interf+"\"";
		String []disconnect={"netsh", "wlan", "disconnect", "interface=\""+_interf+"\""};
		if(WirelessSetup.ARRAY_EXEC)
			Util.getProcessOutput(disconnect);//Runtime.getRuntime().exec(disconnect); 
		else
			Util.getProcessOutputConcatParams(disconnect);//Runtime.getRuntime().exec(Util.concat(disconnect, " ")); 
		Thread.sleep(3000);
		String[] reconnect;
//		reconnect = new String[]{Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_WINDOWS_RECONNECT_OLD_WIRELESS_CONFIG,_interf};
//		if(_DEBUG)System.out.println("WirelessSetup:disconnectWindows: "+Util.concat(reconnect," "));
//		if(WirelessSetup.ARRAY_EXEC)
//			Util.getProcessOutput(reconnect);//Runtime.getRuntime().exec(reconnect);
//		else
//			Util.getProcessOutputConcatParams(reconnect);//Runtime.getRuntime().exec(Util.concat(reconnect, " "));

		String name = Application.USERNAME;
//		if (name!=null){
//			String []_name = name.split(Pattern.quote(" "));
//			if(_name.length>0) Application.warning(Util._("Usernames with spaces not supported :(")+"\""+name+"\"", Util._("Usernames with spaces!"));
//			name = _name[0];
//		}
		reconnect = new String[]{Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_WINDOWS_ADMIN_RECONNECT_WIRELESS_DHCP,name,_interf};
		if(_DEBUG)System.out.println("WirelessSetup:disconnectWindows: "+Util.concat(reconnect," "));
		if(WirelessSetup.ARRAY_EXEC)
			Util.getProcessOutput(reconnect);//Runtime.getRuntime().exec(reconnect);
		else
			Util.getProcessOutput(reconnect); //Runtime.getRuntime().exec(Util.concat(reconnect, " "));
		new Thread(){
			public void run(){
				Application.warning(Util._(" On Windows Home Edition and other OSs,\n you may have to reconnect manually.\n If you need to reconnect with DHCP use:\n "+
						"dhcp.bat in scripts folder (RUN as Admin)\n"+
						" Wireless Interface Name : \n"
						+s_interf), Util._("Reconnect"));			
			}
		}.start();
		
		WlanModel.refreshRepeatedly();
/*
		
		Thread.sleep(10000);
		
		Detect_interface d = new Detect_interface(); 
		String s = d.win_detect_interf();
		DD.setAppText(DD.APP_NET_INTERFACES,s);		
		*/
	}
	@Deprecated
	static public ArrayList<String> check_connection(String w_name) throws IOException{

		ArrayList<String> test=new ArrayList<String>();
		ArrayList<String> _IPs = new ArrayList<String>();
		Process_wlan_information p1=new Process_wlan_information();
		test=p1.process();

		for(int i=0;i<test.size();i++){
			if(test.get(i).compareTo(w_name)==0) {
				_IPs.add(test.get(i+3));
				_IPs.add(test.get(i+4));
				break;
			}
		}

		return _IPs;

	}

	/**
	 *  Set the IP to a random 10.0.0.<random>
	 *  returns the IP/mask
	 *
	 * @param interf
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws P2PDDSQLException
	 */
	static public String Windows_IntConfigure(Object interf) throws IOException, InterruptedException, P2PDDSQLException {

		s_interf=Util.getString(interf);
		if(DEBUG)System.out.println("WirelessSetup : Windows_InetConfigure() : interf name : "+s_interf);
		
		String profile_path = Application.CURRENT_SCRIPTS_BASE_DIR()+Application.ADHOC_WINDOWS_PROFILE_FILE;
		
		// Set of the DirectDemocracy profile (sets the SSID, frequency cell)
		String[] add_profile=new String[]{"netsh", "wlan", "add", "profile", "filename=\""+profile_path+"\"", "interface=\""+s_interf+"\""};

		if(WirelessSetup.ARRAY_EXEC)
			Util.getProcessOutput(add_profile); //Runtime.getRuntime().exec(add_profile);
		else
			Util.getProcessOutputConcatParams(add_profile);//Runtime.getRuntime().exec(Util.concat(add_profile," "));
		if(DEBUG)System.out.println("WirelessSetup : Windows_InetConfigure() : add_profile string :  "+Util.concat(add_profile," "));
		Thread.sleep(3000);

		// connect to the wireless profile
		String[] connect=new String[]{"netsh", "wlan", "connect", "name=\""+DD.DD_SSID+"\"", "interface=\""+s_interf+"\""};

		if(WirelessSetup.ARRAY_EXEC)
			Util.getProcessOutput(connect); // Runtime.getRuntime().exec(connect);
		else
			Util.getProcessOutputConcatParams(connect); // Runtime.getRuntime().exec(Util.concat(connect," "));
		
		Thread.sleep(3000);
	
		int ip_byte = (1+(int)Util.random(253));
		DD.WIRELESS_IP_BYTE = ""+ip_byte;
		EventQueue.invokeLater(new InstructWireless(Util.getString(interf), ip_byte));

		//DD.BROADCAST_IP_BYTE = lan_ip;
		//String IP="10.0.0."+DD.BROADCAST_IP_BYTE;
		DD.WIRELESS_ADHOC_DD_NET_IP = Util.makeIPFromBaseAndByte(DD.WIRELESS_IP_BYTE);
		new Thread(){
			public void run() {
				Application.warning(Util._(" On Windows Home Edition and other Operating systems,\n you may have to set IP manually.\n If the IP is not set right now, set it manually to:\n "+
						DD.WIRELESS_ADHOC_DD_NET_IP+"/"+DD.WIRELESS_ADHOC_DD_NET_MASK+ "\n Wireless Interface Name is :\n"+s_interf+
				"\n Use set_ip_manually.bat in scripts folder(RUN as Admin)"), Util._("Set IP"));	
				
			}
		}.start();
		DD.setAppText(DD.APP_LAST_IP, DD.WIRELESS_ADHOC_DD_NET_IP);
		String name = Application.USERNAME;
//		if (name!=null){
//			String []_name = name.split(Pattern.quote(" "));
//			if(_name.length>0) Application.warning(Util._("Usernames with spaces not supported :(")+"\""+name+"\"", Util._("Usernames with spaces!"));
//			name = _name[0];
//		}
		String[] change_ip = new String[]{Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_WINDOWS_ADMIN_SET_WIRELESS_DD_IP,
				name,DD.WIRELESS_IP_BYTE,s_interf};
		if(_DEBUG)System.out.println("WirelessSetup : Windows_InetConfigure() :  change_ip string :"+Util.concat(change_ip," "));
		// now we execute a script that may succeed to set this IP
		if(WirelessSetup.ARRAY_EXEC)
			Util.getProcessOutput(change_ip);// Runtime.getRuntime().exec(change_ip);
		else
			Util.getProcessOutput(change_ip);// Runtime.getRuntime().exec(Util.concat(change_ip," "));
		WlanModel.refreshRepeatedly();

		return DD.WIRELESS_ADHOC_DD_NET_IP+"/"+DD.WIRELESS_ADHOC_DD_NET_MASK;
		/*
		Thread.sleep(5000);
		 
		String s = Detect_interface.win_detect_interf();
		if(DEBUG) System.out.println("WirelessSetup : Windows_InetConfigure() : string from win_detect_interf : "+s);
		DD.setAppText(DD.APP_NET_INTERFACES,s);
		
		DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP = Windows_IP.get_broadcastIP_from_IP_and_NETMASK(DD.WIRELESS_ADHOC_DD_NET_IP,DD.WIRELESS_ADHOC_DD_NET_MASK);
		InetAddress broadcast = InetAddress.getByName(DD.WIRELESS_ADHOC_DD_NET_BROADCAST_IP);
		BroadcastServer.addBroadcastAddressStatic(new BroadcastInterface(broadcast));
		*/
	}
	@SuppressWarnings("static-access")
	static public String Linux_IntConfigure(Object interf) throws P2PDDSQLException, IOException, InterruptedException {
		//System.out.println("I did it!!");
		String w_name=interf.toString();
		//in Linux laptop:
		//String linux_comm=Application.scripts_path+"ad-hoc_linux "+w_name+" DirectDemocracy 0.0."+(1+(int)Util.random(253));
		// In Windows with Linux Laptop
		String[] linux_comm = null;
		//if (Application.LINUX_INSTALLATION_DIR != null) linux_comm = Application.LINUX_INSTALLATION_DIR;
		//linux_comm += Application.SCRIPTS_RELATIVE_PATH+"ad-hoc_linux "+w_name+" DirectDemocracy 0.0."+(1+(int)Util.random(253));
		linux_comm = new String[]{Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_LINUX_SUID_CONNECT_ADHOC,w_name,DD.DD_SSID, "0.0."
		+(DD.WIRELESS_IP_BYTE=""+(1+(int)Util.random(253)))};
		//System.out.println(linux_comm);
		new Thread(){
			public void run() {
				Application.warning(Util._("Wait a moment... we are connecting to DD!"), Util._("Connecting!"));
			}
		}.start();
		//Runtime.getRuntime().exec(linux_comm);
		Util.getProcessOutput(linux_comm);
		WlanModel.refreshRepeatedly();
		return Util.makeIPFromBaseAndByte(DD.WIRELESS_IP_BYTE)+"/"+DD.WIRELESS_ADHOC_DD_NET_MASK;
		//Thread.sleep(2000);
		/*
		NetworkInterface result = null;
		InetAddress broadcast=null;
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		if (interfaces == null) 
			if(_DEBUG)System.out.println("--No interfaces found--");
		else{
			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();
				if (networkInterface.isLoopback()) continue;
				for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
					broadcast = interfaceAddress.getBroadcast();
					result = networkInterface;
					//System.out.println(result);
					if(broadcast == null) continue;
				}
				if(broadcast != null) {
					
					ArrayList<String> new_f=new ArrayList<String>();
					Linux_wlan_info p1=new Linux_wlan_info();
					new_f=p1.process();
					String bcast1=new String(); 
					for(int i=0;i<new_f.size();i++){
						if(new_f.get(i).startsWith(w_name))
						{	 
							bcast1=bcast1+new_f.get(i)+":"+"true";
						}
						else bcast1=bcast1+new_f.get(i)+":"+"false";
						if(i+1<new_f.size()){ bcast1=bcast1+",";  }
					}
					if(DEBUG) System.out.println("WirelessSetup.Linux_IntConfigure : "+bcast1);
					DD.setAppText(DD.APP_NET_INTERFACES,bcast1);
					
					
					//BroadcastServer.interfaces.add(new BroadcastInterface(broadcast));
					BroadcastServer.addBroadcastAddressStatic(new BroadcastInterface(broadcast));

					break;
				}
			}
		}
		*/
	}
	static public void main(String args[]) {
		System.out.println("Saved in application field values="+args.length);
		if(args.length==0) return;
		try {
			String linux_path = args[0];
			if(!linux_path.endsWith(Application.LINUX_PATH_SEPARATOR)) linux_path += Application.LINUX_PATH_SEPARATOR;
			String _linux_parent=Util.getParent(linux_path);
			if((_linux_parent!=null) && !_linux_parent.endsWith(Application.LINUX_PATH_SEPARATOR)) _linux_parent += Application.LINUX_PATH_SEPARATOR;
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			DD.setAppText(DD.APP_LINUX_INSTALLATION_PATH, linux_path);
			DD.setAppText(DD.APP_LINUX_INSTALLATION_ROOT_PATH, _linux_parent);
			System.out.println("Saved in application field="+DD.APP_LINUX_INSTALLATION_PATH+" value=\""+args[0]+"\"\nparent: \""+_linux_parent+"\"");
			if(args.length==1) return;
			String win_path = args[1];
			if(!win_path.endsWith(Application.WINDOWS_PATH_SEPARATOR)){
				System.out.println("Windows path without terminator="+win_path);
				win_path += Application.WINDOWS_PATH_SEPARATOR;
				System.out.println("Windows path with terminator="+win_path);
			}
			System.out.println("Windows path with terminator="+win_path);
			String _win_parent=Util.getParent(win_path);
			if((_win_parent!=null) && !_win_parent.endsWith(Application.WINDOWS_PATH_SEPARATOR)) _win_parent += Application.WINDOWS_PATH_SEPARATOR;
			DD.setAppText(DD.APP_WINDOWS_INSTALLATION_PATH, win_path);
			DD.setAppText(DD.APP_WINDOWS_INSTALLATION_ROOT_PATH, _win_parent);
			System.out.println("Saved in application field="+DD.APP_WINDOWS_INSTALLATION_PATH+" value=\""+args[1]+"\"\nparent: \""+_win_parent+"\"");

			StartUpThread.detect_OS_fill_var();
			StartUpThread.fill_OS_install_path(); // to be done before importing!!!
			StartUpThread.fill_OS_scripts_path();		
		
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
}
