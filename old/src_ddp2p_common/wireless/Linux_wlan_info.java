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
import java.util.ArrayList;

import config.Application;
import util.Util;

public class Linux_wlan_info {

	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	/**
	 * Uses ip link to get the interfaces.
	 * 
	 * Uses iwconfig to get SSID.
	 * 
	 * Uses ip addr list to get the IP
	 * 
	 * Returns a string in the format: ArrayList<Interface_name:IP:SSID>
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<InterfaceData> process() throws IOException{
		
		String output_line=null;
		ArrayList<String> interface_names = new ArrayList<String>();
		ArrayList<InterfaceData> wlan_params = new ArrayList<InterfaceData>();
		if(DEBUG)System.out.println("Linux_wlan_info:process: result init="+Util.concat(interface_names,"|||","null"));
		String[] cmd_names=new String[]{"bash",Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_LINUX_DETECT_WIRELESS_INTERFACES+"",};//"linux_wlan_interfaces";
		if(DEBUG)System.out.println("Linux_wlan_info:process: "+Util.concat(cmd_names," "));
		//Process p1 = Runtime.getRuntime().exec(cmd_names);
		//BufferedReader bri1 = new BufferedReader (new InputStreamReader(p1.getInputStream()));
		
		BufferedReader bri1 = Util.getProcessOutput(cmd_names, null);
		if(bri1!=null){
			while((output_line = bri1.readLine()) != null){
				if(DEBUG)System.out.println("Linux_wlan_info:process: Interface add : "+output_line);
				interface_names.add(output_line);
			}
			bri1.close();
		}
		if(DEBUG)System.out.println("Linux_wlan_info:process: Interfaces : "+Util.concat(interface_names,",","null"));
		output_line=null;
		ArrayList<String> wlan_SSID = new ArrayList<String>();
		for(int i=0;i<interface_names.size();i++){
			String[] cmd_ssid=new String[]{"bash", Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_LINUX_DETECT_WIRELESS_SSID,interface_names.get(i)};
			if(DEBUG)System.out.println(Util.concat(cmd_ssid," "));
			//Process p2 = Runtime.getRuntime().exec(cmd_ssid);
			BufferedReader bri2 = Util.getProcessOutput(cmd_ssid, null); // new BufferedReader (new InputStreamReader(p2.getInputStream()));
			if((bri2!=null) && (output_line = bri2.readLine()) != null){
				if(DEBUG)System.out.println("Linux_wlan_info:process: Got wifi line:"+output_line);
				wlan_SSID.add(output_line);
			}
			else{
				if(DEBUG)System.out.println("Linux_wlan_info:process: Got wired line");
				interface_names.set(i, null); //it's not wireless!!
			}
			if(DEBUG)System.out.println("Linux_wlan_info:process: Got line:"+output_line);
		}
		if(DEBUG)System.out.println("Linux_wlan_info:process: before clean wlan_names "+Util.concat(interface_names,"|||","null"));
		
		Object null_Obj=null;
		for(int i=interface_names.size()-1;i>=0;i--){
			Object o = interface_names.get(i);
			if(DEBUG)System.out.println("Linux_wlan_info:process: Evaluating :"+o);
			if(o==null){
				if(DEBUG)System.out.println("Linux_wlan_info:process: deleting :"+o);
				interface_names.remove(i);
			}
		}
		if(DEBUG)System.out.println("Linux_wlan_info:process: clean wlan_names="+Util.concat(interface_names,"|||","null"));
		
		output_line=null;
		ArrayList<String> wlan_ip = new ArrayList<String>();
		for(int i=0;i<interface_names.size();i++){
			String[] cmd_ip=new String[]{"bash",Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_LINUX_DETECT_WIRELESS_IP,interface_names.get(i)};
			if(DEBUG)System.out.println(Util.concat(cmd_ip," "));
			// Process p3 = Runtime.getRuntime().exec(cmd_ip);
			BufferedReader bri3 = Util.getProcessOutput(cmd_ip, null); //new BufferedReader (new InputStreamReader(p3.getInputStream()));
			
			if((bri3!=null) && (output_line = bri3.readLine()) != null){
				if(DEBUG)System.out.println("Linux_wlan_info:process: IP Got line:"+output_line);
				wlan_ip.add(output_line);
			}
			else{
				if(DEBUG)System.out.println("Linux_wlan_info:process: IP Got line: NONE");
				wlan_ip.add("NONE");
			}
		}
		
		for(int i = 0;i<interface_names.size();i++){
			if ( (i >= wlan_ip.size()) ||
			     (i >= wlan_SSID.size())) {
				System.out.println("Linux_wlan_info1: No IP/SSID found for WLAN["+i+"] "+interface_names.get(i));
				break;
			}
			InterfaceData id = new InterfaceData();
			id.interface_name = interface_names.get(i);
			id.SSID = wlan_SSID.get(i);
			id.IP = wlan_ip.get(i);
			wlan_params.add(id);
			//wlan_names.set(i, wlan_names.get(i)+":"+wlan_ip.get(i)+":"+wlan_SSID.get(i));
			String old = interface_names.get(i)+":"+wlan_ip.get(i)+":"+wlan_SSID.get(i);
			String _new = ""+id;
			if(DEBUG)System.out.println("Linux_wlan_info:process: result="+old+" vs "+_new);
		}
		
		if(DEBUG)System.out.println("Linux_wlan_info:process: result="+Util.concat(wlan_params,"|||","null"));
		
		return wlan_params;
	
	}
	
	
	public static void main (String arg[]) throws IOException{
		//Linux_wlan_info testObj = new Linux_wlan_info1();
		Linux_wlan_info.process();
	}
}
