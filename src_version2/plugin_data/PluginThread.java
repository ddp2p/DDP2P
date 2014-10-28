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
package plugin_data;

import hds.Address;
import hds.ClientSync;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;

import util.DDP2P_ServiceThread;
import util.P2PDDSQLException;
import config.Application_GUI;
import config.DD;
import util.Util;
import data.D_Peer;
import data.D_PluginData;
import static util.Util.__;

class PluginThread extends DDP2P_ServiceThread {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	Class<?> classToLoad;
	PluginMethods p_methods;
	public PluginThread(Class<?> classToLoad, PluginMethods p_methods, String name, URL url) {
		super("\""+name+"\" "+__("Plugin Thread"), true, url);
		if(DEBUG) System.out.println("PluginThread:PluginThread: start");
		this.classToLoad = classToLoad;
		this.p_methods = p_methods;
	}
	@SuppressWarnings("unchecked")
	public void _run () {
		if(DEBUG) System.out.println("PluginThread:run: start");
		try{
			__run();
		}catch(Exception e){
			e.printStackTrace();
		}
		if(_DEBUG) System.out.println("PluginThread:run: done");
	}
	@SuppressWarnings("unchecked")
	public void __run () {
		if (DEBUG) System.out.println("PluginThread:_run: start");
		//data.D_PluginInfo info = null;
		//PeerPlugin plugin = null;
		String plugin_GID = null;
		String plugin_name = null;
		Hashtable<String, Object> request = null;
		try {
			//info = (data.D_PluginInfo) p_methods.getPluginData.invoke(null);
			//plugin_GID = (String) p_methods.getPluginGID.invoke(null);
			//plugin_name = (String) p_methods.getPluginName.invoke(null);
			//Object _plugin = p_methods.getPeerPlugin.invoke(null);
			//if(_plugin instanceof PeerPlugin) plugin = (PeerPlugin) _plugin;
			Hashtable<String,Object> data = (Hashtable<String,Object>) p_methods.getPluginDataHashtable.invoke(null);
			if (data == null) {
				ping(__("Quit no plugins!"));
				return;
			}
			D_PluginInfo i = new D_PluginInfo().setHashtable(data); // getting the details from the plugin
			if (i == null) {
				ping(__("Quit no plugin data!"));
				return;
			}
			plugin_GID = i.plugin_GID;
			plugin_name = i.plugin_name;

		} catch (IllegalArgumentException e) {e.printStackTrace();}
		catch (IllegalAccessException e) {e.printStackTrace();}
		catch (InvocationTargetException e) {e.printStackTrace();}
		//info.plugin.setSendPipe(connection);
		/*
		if(plugin==null){
			if(_DEBUG) System.out.println("PluginThread:run: no plugin object");
			return;
		}
		*/
		int cnt = 0;
		for (;;) {
			cnt ++;
			//System.out.print("_0");
			if(DEBUG) System.out.println("PluginThread:_run: loop DBGPLUG="+DD.DEBUG_PLUGIN);
			try {
				request = null;
				request = (Hashtable<String,Object>) p_methods.checkOutgoingMessageHashtable.invoke(null);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			if(DEBUG) System.out.println("PluginThread:_run: got =" +request);
			if(request == null) continue;
			ping(__("Will handle request")+" ("+cnt+")");
			PluginRequest msg = new PluginRequest().setHashtable(request);
			boolean result = false;
			switch (msg.type) {
			case PluginRequest.MSG:
				ping(__("Will enqueue message")+" ("+cnt+")");
				//System.out.print("_1");
				if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("PluginThread:_run: will enqueue MSG " + msg);
				if ((msg == null) || (msg.plugin_GID == null) || (msg.peer_GID == null)) {
					if (DD.WARN_OF_INVALID_PLUGIN_MSG)
						Application_GUI.warning(__("Plugin named: \""+plugin_name(msg.plugin_GID)+"\" sends unspecified request"), __("Invalid plugin message"));
					if (_DEBUG || DD.DEBUG_PLUGIN) System.out.println("PluginThread: _run: skip null MSG/TARGET");
					try {
						if (p_methods.confirmStatus != null)
							p_methods.confirmStatus.invoke(request, result);
					} catch (IllegalArgumentException e) {e.printStackTrace();
					} catch (IllegalAccessException e) {e.printStackTrace();
					} catch (InvocationTargetException e) {e.printStackTrace();}
					continue;
				}
				ArrayList<Address> pca = ClientSync.peer_contacted_addresses.get(msg.peer_GID);
				if (DEBUG || DD.DEBUG_PLUGIN) System.out.println("PluginThread: _run: Found stored client address = "+pca);
				if (DD.WARN_OF_UNUSED_PEERS && (pca == null)) {
					D_Peer peer;
					peer = D_Peer.getPeerByGID_or_GIDhash(msg.peer_GID, null, true, false, false, null);
					if (peer == null || ! peer.component_preferences.used) {
						Application_GUI.warning(__("Plugin: "+plugin_name(msg.plugin_GID)+" sends request to blocked/unused user"),
								__("Jammed plugin message"));
						if (_DEBUG || DD.DEBUG_PLUGIN) System.out.println("PluginThread:_run: Abandon sending To unused Peer = "+peer);
						try {
							if (p_methods.confirmStatus != null)
								p_methods.confirmStatus.invoke(request, result);
						} catch (IllegalArgumentException e) {e.printStackTrace();
						} catch (IllegalAccessException e) {e.printStackTrace();
						} catch (InvocationTargetException e) {e.printStackTrace();}
						continue;
					}
				}
				//System.out.print("_5");
				if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("PluginThread:_run: schedule for sending");
				ClientSync.schedule_for_sending_plugin_data(msg);
				if (pca != null) {
					if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("PluginThread:_run: try send");
					ClientSync.try_send(msg, pca);
				}
				if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("PluginThread:_run: enqueue for sending");
				result  = D_PluginData._enqueueForSending(msg.msg, msg.peer_GID, msg.plugin_GID);
				
				if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("PluginThread:_run: sending result = "+result);
				
				if (! result) {
					if (_DEBUG || DD.DEBUG_PLUGIN) System.out.println("PluginThread:run: failed to enqueue");
					try {
						if (p_methods.confirmStatus != null)
							p_methods.confirmStatus.invoke(request, result);
					} catch (IllegalArgumentException e) {e.printStackTrace();
					} catch (IllegalAccessException e) {e.printStackTrace();
					} catch (InvocationTargetException e) {e.printStackTrace();}
				} else
					try {
						if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("PluginThread:_run: touch client");
						DD.touchClient();
					} catch (NumberFormatException e1) {
						e1.printStackTrace();
					} catch (P2PDDSQLException e1) {
						e1.printStackTrace();
					}
				break;
			case PluginRequest.STORE:
				ping(__("Store!")+" ("+cnt+")");
				if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("PluginThread:run: will store MSG "+msg);
				result = D_PluginData._storeData(msg.key, msg.msg, msg.plugin_GID);
				break;
			case PluginRequest.RETRIEVE:
				ping(__("Retrieve!")+" ("+cnt+")");
				byte[] val = D_PluginData._getData(msg.key, msg.plugin_GID);
				try {
					p_methods.answerMessage.invoke(null, msg.key, val);
				} catch (IllegalArgumentException e) {e.printStackTrace();
				} catch (IllegalAccessException e) {e.printStackTrace();
				} catch (InvocationTargetException e) {e.printStackTrace();}
				break;
			case PluginRequest.REGISTER_ACTION:
				ping(__("Register action!")+" ("+cnt+")");
				if(DEBUG) System.out.println("PluginThread:run: will register menu action");
				PluginRegistration.registerPluginMenuAction(plugin_GID, plugin_name, msg.column, msg.plugin_action);
				break;
			case PluginRequest.REGISTER_MENU:
				ping(__("Register menu!")+" ("+cnt+")");
				if(DEBUG) System.out.println("PluginThread:run: will register Menu item");
				PluginRegistration.registerPluginMenu(plugin_GID, plugin_name, msg.column, msg.plugin_menuItem);
				break;
			default:
				ping(__("Unknown command!")+" ("+cnt+")");
				if(DEBUG) System.out.println("PluginThread:run: Unknown command from plugin: "+msg.type);
			}
			if(DEBUG) System.out.println("PluginThread:run: result="+result);
		}
	}
	/**
	 * Returns the name, or (if absent) the GID between brackets.
	 * @param pluginGID
	 * @return
	 */
	public static String plugin_name(String pluginGID){
		try {
			return PluginRegistration.plugin_applets.get(pluginGID).plugin_name;
		} catch (Exception e) {
			e.printStackTrace();
			return "GID["+pluginGID+"]";
		}
	}
	public static void main(String args[]) {
		String val = args[0];
		//byte[] IV = val.getBytes();
		byte[] IV = Util.byteSignatureFromString(val);
		String m1 = "Pay Bob 100$";
		String m2 = "Pay Bob 500$";
		byte[] M1 = m1.getBytes();
		byte[] M2 = m2.getBytes();
		System.out.println("m1="+Util.byteToHex(M1));
		System.out.println("m2="+Util.byteToHex(M2));
		byte m[] = new byte[m1.length()];
		for(int k=0; k<m.length; k++) m[k] = (byte)(M1[k]^M2[k]);
		System.out.println("m^="+Util.byteToHex(m));
		System.out.println("IV="+Util.byteToHex(IV));
		byte iv[] = new byte[IV.length];
		for(int k=0; k<m.length; k++) iv[k] = (byte)(IV[k] ^ m[k]);
		for(int k=m.length; k<IV.length; k++) iv[k] = (byte)(IV[k]);
		System.out.println("iv="+Util.byteToHex(iv));
		System.out.println("iv="+Util.byteToHex(iv).toLowerCase());
	}
}