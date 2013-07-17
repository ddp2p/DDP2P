package wireless;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;

import util.Util;

import config.Application;


public class Mac_wlan_info {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;


	public ArrayList<String> process() throws IOException{
		int pos=0;
		ArrayList<String> wlan_name = new ArrayList<String>();
		NetworkInterface result = null;
		InetAddress broadcast;
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		if (interfaces == null) {
	        if(DEBUG)System.out.println("Mac_wlan_info: --No interfaces found--");
	        return wlan_name;
	}
		if (!interfaces.hasMoreElements()) {
	        if(DEBUG)System.out.println("Mac_wlan_info: --No interfaces found--");
	        return wlan_name;
	}
		while (interfaces.hasMoreElements()) {
			String _name=null,_ip=null;
			NetworkInterface networkInterface = interfaces.nextElement();
			if(DEBUG)System.out.println("Mac_wlan_info: considering "+networkInterface.getName());
			if (networkInterface.isLoopback()) continue;
			//wlan_name.add(networkInterface.getName());
			_name=networkInterface.getName();
			Enumeration<InetAddress> addrList = networkInterface.getInetAddresses();
			 if (!addrList.hasMoreElements()) {
		            if(DEBUG)System.out.println("Mac_wlan_info: No addresses for this interface");
		          }
			 
			 while (addrList.hasMoreElements()) {
		            InetAddress address = addrList.nextElement();
		            if(!(address instanceof Inet4Address)) continue;
		            else _ip=address.getHostAddress();
		     }
			 wlan_name.add(_name+":"+_ip);
			
		 }
		 //System.out.println(wlan_name);
		 
		 ArrayList<String> iwconfig = new ArrayList<String>();
		 for(int i=0;i<wlan_name.size();i++){
			 pos=0;
			 String _name=null,line=null;
			 pos=wlan_name.get(i).indexOf(":");
			 _name=wlan_name.get(i).substring(0,pos);
			 //String script_path = "scripts/";
			 String[] cmd;
			 /*
			 cmd = "/bin/pwd";
			 if(DEBUG)System.out.println("Mac_wlan_info: Will run: "+cmd);
			 //Process p_pwd = Runtime.getRuntime().exec(cmd);
			 //BufferedReader bri_pwd = new BufferedReader (new InputStreamReader(p_pwd.getInputStream()));
			 while ((line = bri_pwd.readLine()) != null) {
				 if(DEBUG) System.out.println("Mac_wlan_info: script returns: " + line);
			 }
			 bri_pwd.close();
			 */
			 cmd= 
				 //"sh "+
				 new String[]{""+Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_MACOS_WLAN_GETINFO,_name};
			 if(DEBUG)System.out.println("Mac_wlan_info: Will run: \""+util.Util.concat(cmd," ")+"\"");
			 //Process p1 = Runtime.getRuntime().exec(cmd);
			 BufferedReader bri = Util.getProcessOutput(cmd); //new BufferedReader (new InputStreamReader(p1.getInputStream()));
			 if(bri!=null) {
				 while ((line = bri.readLine()) != null) {
					 if(DEBUG) System.out.println("Mac_wlan_info: script returns: " + line);
					 iwconfig.add(line);
				 }
				 bri.close();
			 }
			 wlan_name.set(i,wlan_name.get(i)+":"+iwconfig.get(i));	
			 pos=0;
			//String _ssid=null;
			/*
			pos=iwconfig.get(i).indexOf("\"");
			if(pos==-1) {
				wlan_name.set(i,wlan_name.get(i)+":"+"No SSID");	
			}
			else{
				iwconfig.set(i, iwconfig.get(i).substring(pos+1));
				pos=0;
				pos=iwconfig.get(i).indexOf("\"");
				iwconfig.set(i, iwconfig.get(i).substring(0,pos));
				wlan_name.set(i,wlan_name.get(i)+":"+iwconfig.get(i));
			}
			*/
		 }
		 
		 if(DEBUG)System.out.println("Mac_wlan_info: found "+ wlan_name);
		 return wlan_name;
	}
	
	
	public static void main(String args[]) throws IOException{
		Mac_wlan_info p1=new Mac_wlan_info();
		p1.process();
	}

}
