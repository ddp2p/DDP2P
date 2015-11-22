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
package net.ddp2p.widgets.wireless;
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
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.DDP2P_ServiceThread;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.common.wireless.BroadcastInterface;
import net.ddp2p.common.wireless.BroadcastServer;
import net.ddp2p.common.wireless.Detect_interface;
import net.ddp2p.common.wireless.Linux_wlan_info;
import net.ddp2p.common.wireless.Process_wlan_information;
import net.ddp2p.widgets.app.MainFrame;
import net.ddp2p.widgets.app.Util_GUI;
class InstructWireless implements Runnable{
	private static final boolean _DEBUG = true;
	String interf;
	int ip_byte;
	public InstructWireless(String _interf, int _ip_byte){interf = _interf; ip_byte = _ip_byte;}
	@Override
	public void run() {
		if(MainFrame.wireless_hints==null){
			if(_DEBUG)System.out.println("WirelessSetup: InstructWireless: no wireless_hints widget");
			return;
		}
		MainFrame.wireless_hints.setText("netsh interface ip set address \""+interf+"\" static 10.0.0."+ip_byte+" 255.0.0.0");
	}
}
class InstructWirelessDHCP implements Runnable{
	private static final boolean _DEBUG = true;
	String interf;
	int ip_byte;
	public InstructWirelessDHCP(String _interf){interf = _interf;}
	@Override
	public void run() {
		if(MainFrame.wireless_hints==null){
			if(_DEBUG)System.out.println("WirelessSetup: InstructWireless: no wireless_hints widget");
			return;
		}
		MainFrame.wireless_hints.setText("netsh int ip set address  \""+interf+"\" dhcp");
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
			if(DEBUG)System.out.println("WirelessSetup : configureInterface IP : "+IP_Mask);
		}
	}
	static public void interfaceUp(Object interf, boolean configured) throws P2PDDSQLException, IOException, InterruptedException {
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
			String[] linux_comm=new String[]{Application.CURRENT_SCRIPTS_BASE_DIR() +Application.SCRIPT_LINUX_SUID_DISCONNECT_ADHOC};
			new DDP2P_ServiceThread("Wireless: disconnect warning", true) { 
				public void _run() {
					Application_GUI.warning(Util.__("Wait a moment... we are disconnecting from Adhoc!"), Util.__("Disconnecting!"));
				}
			}.start();
			Util.getProcessOutput(linux_comm, Util_GUI.crtProcessLabel);
			WlanModel.refreshRepeatedly();
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
		String []disconnect={"netsh", "wlan", "disconnect", "interface=\""+_interf+"\""};
		if(WirelessSetup.ARRAY_EXEC)
			Util.getProcessOutput(disconnect, Util_GUI.crtProcessLabel);
		else
			Util.getProcessOutputConcatParams(disconnect, Util_GUI.crtProcessLabel);
		Thread.sleep(3000);
		String[] reconnect;
		String name = Application.USERNAME;
		reconnect = new String[]{Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_WINDOWS_ADMIN_RECONNECT_WIRELESS_DHCP,name,_interf};
		if(_DEBUG)System.out.println("WirelessSetup:disconnectWindows: "+Util.concat(reconnect," "));
		if(WirelessSetup.ARRAY_EXEC)
			Util.getProcessOutput(reconnect, Util_GUI.crtProcessLabel);
		else
			Util.getProcessOutput(reconnect, Util_GUI.crtProcessLabel); 
		new DDP2P_ServiceThread("Wireless: reconnect warning", true) {
			public void _run(){
				Application_GUI.warning(Util.__(" On Windows Home Edition and other OSs,\n you may have to reconnect manually.\n If you need to reconnect with DHCP use:\n "+
						"dhcp.bat in scripts folder (RUN as Admin)\n"+
						" Wireless Interface Name : \n"
						+s_interf), Util.__("Reconnect"));			
			}
		}.start();
		WlanModel.refreshRepeatedly();
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
		String[] add_profile=new String[]{"netsh", "wlan", "add", "profile", "filename=\""+profile_path+"\"", "interface=\""+s_interf+"\""};
		if(WirelessSetup.ARRAY_EXEC)
			Util.getProcessOutput(add_profile, Util_GUI.crtProcessLabel); 
		else
			Util.getProcessOutputConcatParams(add_profile, Util_GUI.crtProcessLabel);
		if(DEBUG)System.out.println("WirelessSetup : Windows_InetConfigure() : add_profile string :  "+Util.concat(add_profile," "));
		Thread.sleep(3000);
		String[] connect=new String[]{"netsh", "wlan", "connect", "name=\""+DD.DD_SSID+"\"", "interface=\""+s_interf+"\""};
		if(WirelessSetup.ARRAY_EXEC)
			Util.getProcessOutput(connect, Util_GUI.crtProcessLabel); 
		else
			Util.getProcessOutputConcatParams(connect, Util_GUI.crtProcessLabel); 
		Thread.sleep(3000);
		int ip_byte = (1+(int)Util.random(253));
		DD.WIRELESS_IP_BYTE = ""+ip_byte;
		EventQueue.invokeLater(new InstructWireless(Util.getString(interf), ip_byte));
		DD.WIRELESS_ADHOC_DD_NET_IP = Util.makeIPFromBaseAndByte(DD.WIRELESS_IP_BYTE);
		new DDP2P_ServiceThread("Wireless: netIP warning", true) {
			public void _run() {
				Application_GUI.warning(Util.__(" On Windows Home Edition and other Operating systems,\n you may have to set IP manually.\n If the IP is not set right now, set it manually to:\n "+
						DD.WIRELESS_ADHOC_DD_NET_IP+"/"+DD.WIRELESS_ADHOC_DD_NET_MASK+ "\n Wireless Interface Name is :\n"+s_interf+
				"\n Use set_ip_manually.bat in scripts folder(RUN as Admin)"), Util.__("Set IP"));	
			}
		}.start();
		DD.setAppText(DD.APP_LAST_IP, DD.WIRELESS_ADHOC_DD_NET_IP);
		String name = Application.USERNAME;
		String[] change_ip = new String[]{Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_WINDOWS_ADMIN_SET_WIRELESS_DD_IP,
				name,DD.WIRELESS_IP_BYTE,s_interf};
		if(_DEBUG)System.out.println("WirelessSetup : Windows_InetConfigure() :  change_ip string :"+Util.concat(change_ip," "));
		if(WirelessSetup.ARRAY_EXEC)
			Util.getProcessOutput(change_ip, Util_GUI.crtProcessLabel);
		else
			Util.getProcessOutput(change_ip, Util_GUI.crtProcessLabel);
		WlanModel.refreshRepeatedly();
		return DD.WIRELESS_ADHOC_DD_NET_IP+"/"+DD.WIRELESS_ADHOC_DD_NET_MASK;
	}
	@SuppressWarnings("static-access")
	static public String Linux_IntConfigure(Object interf) throws P2PDDSQLException, IOException, InterruptedException {
		String w_name=interf.toString();
		String[] linux_comm = null;
		linux_comm = new String[]{Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_LINUX_SUID_CONNECT_ADHOC,w_name,DD.DD_SSID, "0.0."
		+(DD.WIRELESS_IP_BYTE=""+(1+(int)Util.random(253)))};
		new DDP2P_ServiceThread("Wireless: connecting warning", true) {
			public void _run() {
				Application_GUI.warning(Util.__("Wait a moment... we are connecting to DD!"), Util.__("Connecting!"));
			}
		}.start();
		Util.getProcessOutput(linux_comm, Util_GUI.crtProcessLabel);
		WlanModel.refreshRepeatedly();
		return Util.makeIPFromBaseAndByte(DD.WIRELESS_IP_BYTE)+"/"+DD.WIRELESS_ADHOC_DD_NET_MASK;
	}
}
