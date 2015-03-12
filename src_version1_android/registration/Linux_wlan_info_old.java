package registration;

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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import util.DBInterface;
@Deprecated
public class Linux_wlan_info_old {

	public ArrayList<String> process() throws IOException{
		int pos=0;
		ArrayList<String> wlan_name = new ArrayList<String>();
		NetworkInterface result = null;
		InetAddress broadcast;
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		if (interfaces == null) 
			System.out.println("Linux_wlan_info: --No interfaces found--");
		else{
			while (interfaces.hasMoreElements()) {
				String _name=null,_ip=null;
				NetworkInterface networkInterface = interfaces.nextElement();
				//System.out.println(networkInterface.getName());
				if (networkInterface.isLoopback()) continue;
				//wlan_name.add(networkInterface.getName());
				_name=networkInterface.getName();
				Enumeration<InetAddress> addrList = networkInterface.getInetAddresses();
				if (!addrList.hasMoreElements()) {
					//System.out.println("Linux_wlan_info: No addresses for this interface");
				}

				while (addrList.hasMoreElements()) {
					InetAddress address = addrList.nextElement();
					if(!(address instanceof Inet4Address)) continue;
					else _ip=address.getHostAddress();
				}
				wlan_name.add(_name+":"+_ip);
				//System.out.println("Linux_wlan_info: names: "+_name+":"+_ip);
			}
		}
		//System.out.println(wlan_name);

		ArrayList<String> iwconfig = new ArrayList<String>();
		for(int i=0,j=-1;i<wlan_name.size();i++){
			pos=0;
			String _name=null,line=null;
			pos=wlan_name.get(i).indexOf(":");
			_name=wlan_name.get(i).substring(0,pos);
			String cmd="sh \""+Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_LINUX_DETECT_WIRELESS_SSID+"\" \""+_name+"\"";
			//System.out.println(cmd);
			Process p1 = Runtime.getRuntime().exec(cmd);
			BufferedReader bri = new BufferedReader (new InputStreamReader(p1.getInputStream()));
			if((line = bri.readLine()) != null) {
				System.out.println("Linux_wlan_info: iwconfig returns: "+line);
				wlan_name.set(i,wlan_name.get(i)+":"+line);
				iwconfig.add(wlan_name.get(i)+":"+line);
			}else{
				wlan_name.set(i,wlan_name.get(i)+":"+"NONE");
			}
			bri.close();
		}

		System.out.println("Linux_wlan_info : wlan_name: "+wlan_name);
		return iwconfig;
		//return wlan_name;
	}

/*
	public static void main(String args[]) throws IOException{
		Linux_wlan_info p1=new Linux_wlan_info();
		p1.process();
	}
	*/

}
