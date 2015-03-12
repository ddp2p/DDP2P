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
package plugin;
import java.util.*;

class Hello1PeerPluginRenderer implements plugin_data.PeerPluginRenderer {
    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable a,java.lang.Object b,boolean c,boolean d,int e,int f) {
	return new javax.swing.JLabel("1st Plugin");
    }
};
class Hello1PeerPluginEditor implements plugin_data.PeerPluginEditor {
    public java.awt.Component getTableCellEditorComponent(javax.swing.JTable a,java.lang.Object b,boolean c,int d,int e){
	return null;
    }
    public void removeCellEditorListener(javax.swing.event.CellEditorListener a){}
    public void addCellEditorListener(javax.swing.event.CellEditorListener a){}
    public void cancelCellEditing(){}
    public boolean stopCellEditing(){return false;}
    public boolean shouldSelectCell(java.util.EventObject a){return false;}
    public boolean isCellEditable(java.util.EventObject a){return false;}
    public Object  getCellEditorValue(){return null;}
};
class Hello1PeerPlugin implements plugin_data.PeerPlugin {
    java.util.ArrayList<plugin_data.PluginRequest> queue =
	new java.util.ArrayList<plugin_data.PluginRequest>();
    public void handleReceivedMessage(byte[] msg, String peer_GID){
	System.out.println("From: "+peer_GID+" got="+msg);
    }
    public plugin_data.PluginRequest checkOutgoingMessage(){
	synchronized(queue){
	    while(queue.size()==0) {
		try {
		    queue.wait();
		} catch(Exception e){}
	    }
	    return queue.remove(0);
	}
    }
    public void answerMessage(String key, byte[] msg){
	System.out.println("From: db "+key+" got="+msg);
    }
    public void setSendPipe(plugin_data.PeerConnection connection){
    }
};

public
class Main {
    static String plugin_GID = "1";
    static String peer_GID;
    static String plugin_name = "Hello 1";
    static String name;
    static Hello1PeerPluginRenderer renderer = new Hello1PeerPluginRenderer();
    static Hello1PeerPluginEditor editor = new Hello1PeerPluginEditor();
    static Hello1PeerPlugin plugin = new Hello1PeerPlugin();

    public static void init() {}
    public static String getPluginGID() {
	return plugin_GID;
    }
    public static String getPluginName() {
	return plugin_name;
    }
    public static plugin_data.PeerPlugin getPeerPlugin() {
	return plugin;
    }
    public static data.D_PluginInfo getPluginData() {
	    return new data.D_PluginInfo(plugin_GID,plugin_name,"info","url",
					 (plugin_data.PeerPluginEditor)editor,
					 ( plugin_data.PeerPluginRenderer)renderer
					 );
    }
    public static Hashtable<String,Object> getPluginDataHashtable() {
	Hashtable<String,Object> pd = new Hashtable<String,Object>();
	pd.put("plugin_GID", plugin_GID);
	pd.put("plugin_name", plugin_name);
	pd.put("plugin_info", "info");
	pd.put("plugin_url", "url");
	pd.put("editor",editor);
	pd.put("renderer",renderer);
	return pd;
    }

    public static void handleReceivedMessage(byte[] msg, String peer_GID) {
	plugin.handleReceivedMessage(msg, peer_GID);
    }
    public static void answerMessage(String key, byte[] msg) {
	plugin.answerMessage(key, msg);
    }
    public static Hashtable<String,Object> checkOutgoingMessageHashtable() {
	System.out.println("Plugin:Hello1:checkOutgoingMessageHashtable: start");
	Hashtable<String,Object> pd;
	pd = plugin.checkOutgoingMessage().getHashtable();
	System.out.println("Plugin:Hello1:checkOutgoingMessageHashtable: got="+plugin_data.PluginRequest.display(pd));
	return pd;
    }

    public static void setPluginData(String peer_GID, String name){
	System.out.println("Plugin:Hello1:setPluginData: "+peer_GID+" name="+name);
	Main.peer_GID = peer_GID;
	Main.name = name;
    }
};

