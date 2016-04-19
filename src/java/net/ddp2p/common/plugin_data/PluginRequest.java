package net.ddp2p.common.plugin_data;
import java.util.*;
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
public class PluginRequest {
	public static final int MSG = 0;   				
	public static final int STORE = 1; 				
	public static final int RETRIEVE = 2; 			
	public static final int REGISTER_ACTION = 3; 	
	public static final int REGISTER_MENU = 4;   	
	public static final int MAX_ENQUEUED_FOR_SENDING = 10;
	public int type;  
	public String key; 
	public String plugin_GID; 
	public String peer_GID; 
	public byte[] msg; 
	public int column;	
	public Object plugin_action; 
	public Object plugin_menuItem; 
	public String toString(){
		return "PluginRequest ["+
		"\n type = "+type+
		"\n key = "+key+
		"\n plugin_GID = "+plugin_GID+
		"\n peer_GID = "+peer_GID+
		"\n msg = "+byteToHex(msg)+
		"\n column = "+column+
		"\n action = "+plugin_action+
		"\n]";
	}
	public static String display(Hashtable<String,Object> r) {
		return new PluginRequest().setHashtable(r).toString();
	}
	public Hashtable<String,Object> getHashtable() {
		Hashtable<String,Object> pd = new Hashtable<String,Object>();
		pd.put("type", new Integer(type+""));
		pd.put("column", new Integer(column+""));
		if(key!=null) pd.put("key", key);
		if(plugin_GID!=null) pd.put("plugin_GID", plugin_GID);
		if(peer_GID!=null) pd.put("peer_GID", peer_GID);
		if(msg!=null) pd.put("msg", msg);
		if(plugin_action!=null) pd.put("plugin_action", plugin_action);
		if(plugin_menuItem!=null) pd.put("plugin_menuItem", plugin_menuItem);
		return pd;
	}
	public PluginRequest setHashtable(Hashtable<String,Object> pd){
		type = ((Integer)pd.get("type")).intValue();
		column = ((Integer)pd.get("column")).intValue();
		key = ((String)pd.get("key"));
		plugin_GID = ((String)pd.get("plugin_GID"));
		peer_GID = ((String)pd.get("peer_GID"));
		msg = ((byte[])pd.get("msg"));
		plugin_action = ((Object)pd.get("plugin_action")); // Action
		plugin_menuItem = ((Object)pd.get("plugin_menuItem")); // JMenuItem
		return this;
	}
    static String HEX[]={"0","1","2","3","4","5","6","7","8","9",
		"A","B","C","D","E","F"};
    public static String byteToHex(byte[] b, String sep){
    	if(b==null) return "NULL";
    	String result="";
    	for(int i=0; i<b.length; i++)
    		result = result+sep+HEX[(b[i]>>4)&0x0f]+HEX[b[i]&0x0f];
    	return result;
    }
    public static String byteToHex(byte[] b){
    	return byteToHex(b,"");
    }
}
