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

package data;

import hds.ASNPluginInfo;

import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import plugin_data.PeerPlugin;
import plugin_data.PeerPluginEditor;
import plugin_data.PeerPluginRenderer;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;

import ASN1.Encoder;

import util.Util;

public
class D_PluginInfo{
	private static final boolean DEBUG = false;
	public static ArrayList<String> plugins= new ArrayList<String>();
	public static Hashtable<String, D_PluginInfo> plugin_data = new Hashtable<String, D_PluginInfo>();

	
	public TableCellEditor editor;
	public TableCellRenderer renderer;
	public String plugin_name;
	public String plugin_info;
	public String plugin_GID;
	public String plugin_url;
	//public PeerPlugin plugin;
	/**
	 * Used only to unpack data coming from Plugin, internally. remote communication is based on ASNPluginInfo
	 */
	public D_PluginInfo(){}
	/**
	 * Used only to unpack data coming from Plugin, internally. remote communication is based on ASNPluginInfo
	 * @param plugin_GID
	 * @param plugin_name
	 * @param plugin_info
	 * @param plugin_url
	 * @param editor
	 * @param renderer
	 */
	public D_PluginInfo(
			String plugin_GID,
			String plugin_name,
			String plugin_info,
			String plugin_url,
			TableCellEditor editor,
			TableCellRenderer renderer
			//plugin_data.PeerPlugin plugin
			){
		this.editor = editor;
		this.renderer = renderer;
		this.plugin_GID = plugin_GID;
		this.plugin_name = plugin_name;
		this.plugin_info = plugin_info;
		this.plugin_url = plugin_url;
		//this.plugin = plugin;
	}
	public String toString() {
		return "D_PluginInfo: (name="+plugin_name+", info="+Util.trimmed(plugin_info)+", url="+plugin_url+", GID="+Util.trimmed(plugin_GID)+") ";
	}
	public D_PluginInfo instance() {return new D_PluginInfo();}
	public static D_PluginInfo getPluginInfo(String gid){
		if(D_PluginInfo.plugin_data == null) return null;
		D_PluginInfo i = D_PluginInfo.plugin_data.get(gid);
		return i;
	}
	
	public static ASNPluginInfo[] getRegisteredPluginInfo(){
		if(plugin_data.size()==0) return null;
		ArrayList<ASNPluginInfo> api = new ArrayList<ASNPluginInfo>();
		for(D_PluginInfo pd : plugin_data.values()) {
			api.add(new ASNPluginInfo(pd.plugin_GID,pd.plugin_name,pd.plugin_info,pd.plugin_url));
		}
		return api.toArray(new ASNPluginInfo[0]);
	}

	public static void recordPluginInfo(ASNPluginInfo[] plugins,
			String _global_peer_ID, String _peer_ID) throws SQLiteException {
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginInfo: recordPluginInfo: start from peer_ID="+_peer_ID+" gid="+Util.trimmed(_global_peer_ID));
		if(plugins==null) {
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginInfo: recordPluginInfo: no plugin info received");
			return;
		}
		if(_peer_ID==null) {
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginInfo: recordPluginInfo: no plugin remote peer info received");
			return;
		}
		Application.peers.setPluginsInfo(plugins, _global_peer_ID, _peer_ID);
		String info = Util.stringSignatureFromByte(Encoder.getEncoder(plugins).getBytes());
		Application.db.update(
				table.peer.TNAME,
				new String[]{table.peer.plugin_info},
				new String[]{table.peer.peer_ID},
				new String[]{info, _peer_ID},
				DEBUG || DD.DEBUG_PLUGIN);
	}
	public Hashtable<String,Object> getHashtable() {
		Hashtable<String,Object> pd = new Hashtable<String,Object>();
		pd.put("plugin_GID", plugin_GID);
		pd.put("plugin_name", plugin_name);
		pd.put("plugin_info", plugin_info);
		pd.put("plugin_url", plugin_url);
		pd.put("editor",editor);
		pd.put("renderer",renderer);
		return pd;		
	}
	public D_PluginInfo setHashtable(Hashtable<String,Object> pd) {
		plugin_GID = (String)pd.get("plugin_GID");
		plugin_name = (String)pd.get("plugin_name");
		plugin_info = (String)pd.get("plugin_info");
		plugin_url = (String)pd.get("plugin_url");
		editor = (TableCellEditor)pd.get("editor");
		renderer = (TableCellRenderer)pd.get("renderer");
		return this;
	}
	public static boolean contains(ASNPluginInfo[] info, String pluginID) {
		if((info == null)||(pluginID == null)) return false;
		for(ASNPluginInfo i : info) {
			if(!pluginID.equals(i.gid)) continue;
			return true;
		}
		return false;
	}
}