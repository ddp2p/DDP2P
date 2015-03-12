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
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import config.Application;
import config.DD;

import util.Util;

public class Win_wlan_info {

	
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static final String Wireless_NETSH = "Configuration for interface";// used to detect a new interface entry in netsh output


	public static String get_output_ADHOC_DD_IP_WINDOWS_NETSH_INTERFACE_IDENTIFIER(String interf){
		String[] s =
				new String[]{Application.CURRENT_SCRIPTS_BASE_DIR() + Application.SCRIPT_WINDOWS_WIRELESS_DETECT_SSID_RAW};
		try {
			BufferedReader bri = Util.getProcessOutput(s, null); 
			return Util.readAll(bri);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * Parse_netsh_wlan : get wireless lan info for Windows using netsh 
	 * the format Hashtable<Interface_Name,SSID>
	 * @return Hashtable<Interface_Name,SSID>
	 * @throws IOException
	 */
	public static Hashtable<String, String> Parse_netsh_wlan() throws IOException {
		Hashtable<String, String> Int_ssid = new Hashtable<String, String>();
		int pos = 0;
		String line;
		ArrayList<String> netsh = new ArrayList<String>();
		ArrayList<String> netsh1 = new ArrayList<String>();
		String wireless_SSID_script[]=
				new String[]{Application.CURRENT_SCRIPTS_BASE_DIR() + Application.SCRIPT_WINDOWS_WIRELESS_DETECT_SSID,
				DD.ADHOC_DD_IP_WINDOWS_NETSH_INTERFACE_IDENTIFIER,
				DD.ADHOC_DD_IP_WINDOWS_NETSH_SSID_IDENTIFIER};
		// To avoid annoying errors when running on a desktop with no wireless
		boolean old_err = DD.SCRIPTS_ERRORS_WARNING;
		DD.SCRIPTS_ERRORS_WARNING = false;
		BufferedReader bri = Util.getProcessOutput(wireless_SSID_script, null);
		DD.SCRIPTS_ERRORS_WARNING = old_err;
		if(bri!=null){
			while ((line = bri.readLine()) != null) {
				if(line.length()!=0)
					netsh.add(line);
			}
			bri.close();
		}
		netsh.remove(0);
		for(int i=0;i<netsh.size();i++)
			netsh.set(i, netsh.get(i).trim());

		for(int i=0;i<netsh.size();i++){
			pos=0;
			if(netsh.get(i).startsWith(DD.ADHOC_DD_IP_WINDOWS_NETSH_INTERFACE_IDENTIFIER)){

				if((i+1>=netsh.size())){
					pos=netsh.get(i).indexOf(':');
					netsh.set(i, netsh.get(i).substring(pos+1));
					netsh.set(i,netsh.get(i).trim());
					//netsh1.add(netsh.get(i)+":"+Util._("No SSID"));
					Int_ssid.put(netsh.get(i),Util._("No SSID"));
				}
				else if(netsh.get(i+1).startsWith(DD.ADHOC_DD_IP_WINDOWS_NETSH_INTERFACE_IDENTIFIER)){
					pos=0;
					pos=netsh.get(i).indexOf(':');
					netsh.set(i, netsh.get(i).substring(pos+1));
					netsh.set(i,netsh.get(i).trim());
					//netsh1.add(netsh.get(i)+":"+Util._("No SSID"));
					Int_ssid.put(netsh.get(i),Util._("No SSID"));
				}

				else if(netsh.get(i+1).startsWith(DD.ADHOC_DD_IP_WINDOWS_NETSH_SSID_IDENTIFIER)){
					pos=netsh.get(i).indexOf(':');
					netsh.set(i, netsh.get(i).substring(pos+1));
					netsh.set(i,netsh.get(i).trim());
					pos=0;
					pos=netsh.get(i+1).indexOf(':');
					netsh.set(i+1, netsh.get(i+1).substring(pos+1));
					//netsh1.add(netsh.get(i)+":"+netsh.get(i+1).trim());
					Int_ssid.put(netsh.get(i),netsh.get(i+1).trim());
				}
			}
		}
		if(DEBUG)System.out.println("Win_wlan_info:Parse_netsh_wlan: netsh_output: "+Int_ssid);
		return Int_ssid;
	}
	
	/**
	 * Returns a string in the format: ArrayList<Interface_name:IP:SSID>
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<InterfaceData> process() throws IOException {
		Hashtable<String, String> Int_ssid = Win_wlan_info.Parse_netsh_wlan();
		ArrayList<InterfaceData> w_info = new ArrayList<InterfaceData>();
		
		Enumeration<String> keys = Int_ssid.keys();
		while(keys.hasMoreElements()){
			 Object key = keys.nextElement();
			 if(DD.ADHOC_DD_IP_WINDOWS_DETECTED_WITH_NETSH)
				 if(Int_ssid.get(key).contains(DD.DD_SSID)) {//if ssid is DD then get ip using netsh cause its static
					 w_info.add(get_ip_from_netsh(key));
					 continue;
				 }
			 //else get ip for other interfaces using ipconfig cause its dynamic
			 w_info.add(get_ip_from_ipconfig(key,Int_ssid.get(key)));
		}
		if(_DEBUG)System.out.println("Win_wlan_info:process:w_info : "+w_info);
		return w_info;
	}

	/**
	 * 
	 * @param key
	 * @param ssid 
	 * @return
	 * @throws IOException 
	 */
	public static InterfaceData get_ip_from_ipconfig(Object key, String ssid) throws IOException {
		String line;
		ArrayList<String> ipconfig = new ArrayList<String>();
		ArrayList<String> ipconfig1 = new ArrayList<String>();
		InterfaceData id = new InterfaceData();
		String s="ipconfig";		
		BufferedReader bri = Util.getProcessOutput(new String[]{s}, null); //new BufferedReader (new InputStreamReader(new ByteArrayInputStream(ipconfig_result.getBytes())));
		if(bri!=null){
			while ((line = bri.readLine()) != null) {
				if(line.length()!=0)
					ipconfig.add(line);
			}
			bri.close();
		}


		//extract  wireless lines	   
		for(int i=0;i<ipconfig.size();i++){
			if(ipconfig.get(i).endsWith((String) key+":")){ 
				ipconfig1.add(ipconfig.get(i));
				for(int j=i+1;j<ipconfig.size();j++){ // ignore the first line after Wireless (i+1) (typically empty) (needed?)
					if(!ipconfig.get(j).startsWith("  ")) break;
					ipconfig1.add(ipconfig.get(j));
				}
			}
		}
		if(DEBUG)System.out.println("Win_wlan_info:get_ip_from_ipconfig: ipconfig1: "+ipconfig1);

		// extract IP for each interface
		int flag=0;
		int pos =0;
		for(int i=0;i<ipconfig1.size();i++){
			if(ipconfig1.get(i).contains(DD.ADHOC_DD_IP_WINDOWS_IPCONFIG_IPv4_IDENTIFIER)){
				pos=ipconfig1.get(i).indexOf(':');
				ipconfig1.set(i, ipconfig1.get(i).substring(pos+2));
				ipconfig1.set(i,ipconfig1.get(i).trim());
				id.IP = ipconfig1.get(i);
				flag=1;
			}
			if(flag==0) id.IP = Util._(DD.WINDOWS_NO_IP);
		}
		id.interface_name = ""+key;
		id.SSID = ssid;
		if(DEBUG)System.out.println("Win_wlan_info:get_ip_from_ipconfig: id: "+id);
		return id;
	}

	public static String get_output_ADHOC_DD_IP_WINDOWS_NETSH_IP_IDENTIFIER(String interf){
		String[] s = new String[]{"netsh int ipv4 show addresses name=\""+interf+"\""};
		try {
			BufferedReader bri = Util.getProcessOutputConcatParams(s, null); 
			return Util.readAll(bri);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public static InterfaceData get_ip_from_netsh(Object key) throws IOException {
		if(DEBUG)System.out.println("Win_wlan_info:Parse_netsh_wlan:get_ip_from_netsh: Int_name"+key);
		String line;
		ArrayList<String> netsh_ip = new ArrayList<String>();
		String[] s = new String[]{"netsh int ipv4 show addresses name=\""+key+"\""};
		BufferedReader bri = Util.getProcessOutputConcatParams(s, null); //new BufferedReader (new InputStreamReader(new ByteArrayInputStream(ipconfig_result.getBytes())));
		if(bri!=null){
			while ((line = bri.readLine()) != null) {
				if(line.length()!=0)
					netsh_ip.add(line);
			}
			bri.close();
		}
		if(DEBUG)System.out.println("Win_wlan_info:Parse_netsh_wlan:get_ip_from_netsh: netsh_ip"+netsh_ip);

		int pos=0;
		int flag=0;
		for(int i=0;i<netsh_ip.size();i++){
			InterfaceData id = new InterfaceData();
			id.interface_name = ""+key;
			flag=0;
			String Ip=null;
			String Int_name=null;
			flag=0;
			if(netsh_ip.get(i).contains(Wireless_NETSH))
			{
				pos=0;
				pos = netsh_ip.get(i).indexOf("\"");
				//Int_name = netsh_ip.get(i).substring(pos+1,netsh_ip.get(i).length()-1);
				//System.out.println("name : "+Int_name);
				for(int j=i+1;j<netsh_ip.size();j++){
					if(!netsh_ip.get(j).startsWith("  "))  break;
					if(netsh_ip.get(j).contains(DD.ADHOC_DD_IP_WINDOWS_NETSH_IP_IDENTIFIER)){
						pos=0;
						//Ip = netsh_ip.get(j).substring(pos+1,netsh_ip.get(j).length());						
						pos=netsh_ip.get(j).indexOf(':');
						netsh_ip.set(j, netsh_ip.get(j).substring(pos+2));
						netsh_ip.set(j,netsh_ip.get(j).trim());
						id.IP = netsh_ip.get(j);
						//System.out.println("IP : "+Ip);
						flag=1;
					}
				}
				if(flag==0) { Ip=DD.WINDOWS_NO_IP;  id.IP = Ip;//System.out.println("IP : "+Ip); 
				}
			}
			id.SSID = DD.DD_SSID;
			if(DEBUG)System.out.println("Win_wlan_info:get_ip_from_netsh: id : "+id);
			return id;
		}
		return null;
	}

	public static void main(String args[]) throws IOException{
		Win_wlan_info.Parse_netsh_wlan();
	}

	public static DatagramSocket[] extractValidIPs(BroadcastData _BD) {
		if((_BD.Interfaces_names==null)|| (DD.OS!=DD.WINDOWS)) return _BD.bcs;

		ArrayList<DatagramSocket> result = new ArrayList<DatagramSocket>();
		for(int i=0;i<_BD.Interfaces_names.length;i++){
			try {
				InterfaceData id = Win_wlan_info.get_ip_from_ipconfig(_BD.Interfaces_names[i],DD.DD_SSID);
				if(!DD.WINDOWS_NO_IP.equals(id.IP))
					result.add(_BD.bcs[i]);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result.toArray(new DatagramSocket[0]);
	}



}
