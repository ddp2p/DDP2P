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

package net.ddp2p.common.plugin_data;

public class PeerPluginData {
	String peer_GID;
	String name;
	
	/*
    static Hashtable<String,PluginObject> plugins = new Hashtable<String,PluginObject>();
    static Hashtable<String, ASNPluginInfo> plugin_info = new Hashtable<String,ASNPluginInfo>();
    
    public static void registerPlugin(String id, String name, String info, String url, PluginObject plugin) {
    	// check id = HASH(info || url);
    	plugins.put(id, plugin);
    	plugin_info.put(id, new ASNPluginInfo(id,name,info,url));
    }
    
    public static void scheduleMessage(String global_peer_ID, 
    		String plugin_id, byte[] message, boolean immediate){
    	
    }
	*/

}
