/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 
		Author: Marius Silaghi msilaghi@fit.edu
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
package net.ddp2p.common.wireless;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Pattern;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.util.Util;
import static net.ddp2p.common.util.Util.__;

class WIFI_Interface_Info{
	String name = null;
	String SSID = null;
	ArrayList<String> ip4 = new ArrayList<String>();
	ArrayList<String> ip6 = new ArrayList<String>();
	boolean up;
	boolean up_set = false;
	boolean multicast;
	boolean multicast_set = false;

	public void merge(WIFI_Interface_Info a) {
		if(name == null) name = a.name;
		if(SSID == null) SSID = a.SSID;
		if(!up_set){
			up_set = a.up_set;
			up = a.up;
		}
		if(!multicast_set){
			multicast_set = a.multicast_set;
			multicast = a.multicast;
		}
		for(String ip: a.ip4) if(!ip4.contains(ip)) ip4.add(ip);
		for(String ip: a.ip6) if(!ip6.contains(ip)) ip6.add(ip);
	}
}
public class Mac_wlan_info {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;

	public Hashtable<String,WIFI_Interface_Info> getInterfaces_ifconfig() throws IOException {
		Hashtable<String,WIFI_Interface_Info> wii = new Hashtable<String,WIFI_Interface_Info>();
		String line;
		String scripts_path = Application.CURRENT_SCRIPTS_BASE_DIR();
		String cmd[]= new String[]{""+scripts_path+Application.SCRIPT_MACOS_WLAN_GET_WIFI_INTERFACES};
		 if(DEBUG)System.out.println("Mac_wlan_info: Will run: \""+net.ddp2p.common.util.Util.concat(cmd," ")+"\"");
		 BufferedReader bri = Util.getProcessOutput(cmd, null);
		 if(bri!=null) {
			 while ((line = bri.readLine()) != null) {
				 if(DEBUG) System.out.println("Mac_wlan_info: script returns: " + line);
					String names[] = line.split(Pattern.quote(":"));
					for(String name: names){
						if(name==null) continue;
						name = name.trim();
						if(name.length()==0) continue;
						WIFI_Interface_Info value;
						value = new WIFI_Interface_Info();
						value.name = name;
						wii.put(name, value);
					}				 
			 }
			 bri.close();
		 }
		return wii;
	}

	public Hashtable<String,WIFI_Interface_Info> getInterfaces_airport() throws IOException {
		Hashtable<String,WIFI_Interface_Info> wii = new Hashtable<String,WIFI_Interface_Info>();
		String line;
		String scripts_path = Application.CURRENT_SCRIPTS_BASE_DIR();
		String cmd[]= new String[]{""+scripts_path+Application.SCRIPT_MACOS_WLAN_GET_WIFI_INTERFACE};
		 if(DEBUG)System.out.println("Mac_wlan_info: Will run: \""+net.ddp2p.common.util.Util.concat(cmd," ")+"\"");
		 BufferedReader bri = Util.getProcessOutput(cmd, null);
		 if(bri!=null) {
			 while ((line = bri.readLine()) != null) {
				 if(DEBUG) System.out.println("Mac_wlan_info: script returns: " + line);
					String names[] = line.split(Pattern.quote(":"));
					for(String name: names){
						if(name==null) continue;
						name = name.trim();
						if(name.length()==0) continue;
						WIFI_Interface_Info value;
						value = new WIFI_Interface_Info();
						value.name = name;
						wii.put(name, value);
					}				 
			 }
			 bri.close();
		 }
		return wii;
	}
	public Hashtable<String,WIFI_Interface_Info> getInterfaces_Java() throws SocketException {
		Hashtable<String,WIFI_Interface_Info> wii = new Hashtable<String,WIFI_Interface_Info>();
		ArrayList<String> wlan_name = new ArrayList<String>();
		NetworkInterface result = null;
		InetAddress broadcast;
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		if (interfaces == null) {
	        if(DEBUG)System.out.println("Mac_wlan_info: --No interfaces found-- null");
	        return wii;
		}
		if (!interfaces.hasMoreElements()) {
	        if(DEBUG)System.out.println("Mac_wlan_info: --No interfaces found-- empty");
	        return wii;
		}
		while (interfaces.hasMoreElements()) {
			WIFI_Interface_Info value;
			String _name=null,_ip=null;
			NetworkInterface networkInterface = interfaces.nextElement();
			//wlan_name.add(networkInterface.getName());
			_name=networkInterface.getName();
			if(DEBUG)System.out.println("Mac_wlan_info: considering "+_name);
			if (networkInterface.isLoopback()){
				if(DEBUG)System.out.println("Mac_wlan_info: considering "+_name+" loopback");
				continue;
			}
			
			value = new WIFI_Interface_Info();
			value.name = _name;
			value.up = networkInterface.isUp(); value.up_set = true;
			value.multicast = networkInterface.supportsMulticast(); value.multicast_set = true;
			
			Enumeration<InetAddress> addrList = networkInterface.getInetAddresses();
			 if (!addrList.hasMoreElements()) {
		            if(DEBUG)System.out.println("Mac_wlan_info: No addresses for this interface");
			 }
			 
			 while (addrList.hasMoreElements()) {
				 InetAddress address = addrList.nextElement();
				 if(DEBUG)System.out.println("Mac_wlan_info: "+_name+" addr="+address);
				 if(!(address instanceof Inet4Address)){
					 if(DEBUG)System.out.println("Mac_wlan_info: "+_name+" addr is v6... discarded...");
					 value.ip6.add(address.getHostAddress());
					 continue;
				 }else{
					 _ip=address.getHostAddress();
					 value.ip4.add(_ip);
					 if(DEBUG)System.out.println("Mac_wlan_info: "+_name+" ip="+_ip);
				 }
		     }
			 wlan_name.add(_name+":"+_ip);
			 wii.put(_name, value);
			
		 }
		return wii;
	}

	public ArrayList<WIFI_Interface_Info> getInterfaces(){
		Hashtable<String,WIFI_Interface_Info> wiis = new Hashtable<String,WIFI_Interface_Info>();
		try {
			Hashtable<String,WIFI_Interface_Info> wii_crt = getInterfaces_Java();
			for(String name: wii_crt.keySet()){
				WIFI_Interface_Info old = wiis.get(name);
				if(old==null) {
					wiis.put(name, wii_crt.get(name));
					continue;
				}
				old.merge(wii_crt.get(name));
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		try {
			Hashtable<String,WIFI_Interface_Info> wii_crt = getInterfaces_airport();
			for(String name: wii_crt.keySet()){
				WIFI_Interface_Info old = wiis.get(name);
				if(old==null) {
					wiis.put(name, wii_crt.get(name));
					continue;
				}
				old.merge(wii_crt.get(name));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			Hashtable<String,WIFI_Interface_Info> wii_crt = getInterfaces_ifconfig();
			for(String name: wii_crt.keySet()){
				WIFI_Interface_Info old = wiis.get(name);
				if(old==null) {
					wiis.put(name, wii_crt.get(name));
					continue;
				}
				old.merge(wii_crt.get(name));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ArrayList<WIFI_Interface_Info> wii = new ArrayList<WIFI_Interface_Info>(wiis.values());
		return wii;
	}

	public ArrayList<WIFI_Interface_Info> setSSIDs(ArrayList<WIFI_Interface_Info> wiis){
		for (WIFI_Interface_Info w : wiis) {
			if(w.SSID != null) continue;
			try {
				setSSID(w);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return wiis;
	}
	
	private void setSSID(WIFI_Interface_Info w) throws IOException {
		String _name = w.name;
		String cmd[];
		String line;
		String scripts_path = Application.CURRENT_SCRIPTS_BASE_DIR();
		cmd = new String[]{""+scripts_path+Application.SCRIPT_MACOS_WLAN_GETINFO, _name};
		if(DEBUG)System.out.println("Mac_wlan_info: Will run: \""+net.ddp2p.common.util.Util.concat(cmd," ")+"\"");
		BufferedReader bri = Util.getProcessOutput(cmd, null);
		if(bri!=null) {
			while ((line = bri.readLine()) != null) {
				if(DEBUG) System.out.println("Mac_wlan_info: script returns: " + line);
				//iwconfig.add(line);
				if(w.SSID != null){
					Application_GUI.warning(__("Replacing wlan SSID "+w.SSID+" with "+line), __("SSID for:")+" "+_name);
				}
				w.SSID = line;
			}
			bri.close();
		}
		//wlan_name.set(i,wlan_name.get(i)+":"+iwconfig.get(i));	
	}

	public ArrayList<String> process() throws IOException{
		ArrayList<String> wlan_name = new ArrayList<String>();
		ArrayList<WIFI_Interface_Info> wii = getInterfaces();
		setSSIDs(wii);
		for(WIFI_Interface_Info w : wii){
			if(w.SSID == null) w.SSID="";
			wlan_name.add(w.name+":"+Util.concat(w.ip4,";", "")+":"+w.SSID);
		}		 
		 if(DEBUG)System.out.println("Mac_wlan_info: found "+ wlan_name);
		 return wlan_name;
	}
	
	
	public static void main(String args[]) throws IOException{
		Mac_wlan_info p1=new Mac_wlan_info();
		p1.process();
	}

}
