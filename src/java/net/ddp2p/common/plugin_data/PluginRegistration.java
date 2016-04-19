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
package net.ddp2p.common.plugin_data;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.data.HandlingMyself_Peer;
public class PluginRegistration {
	public static Hashtable<String,D_PluginInfo> plugin_applets = new Hashtable<String, D_PluginInfo>();
	public static Hashtable<Integer,Hashtable<String,PluginMenus>> plugin_menus = new Hashtable<Integer,Hashtable<String,PluginMenus>>();
	public static Hashtable<String,PluginMethods> s_methods = new Hashtable<String,PluginMethods>();
	private static Hashtable<String,PluginThread> threads =  new Hashtable<String,PluginThread>();
	private static HashSet<URL> plugin_urls = new HashSet<URL>();
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public static boolean registerPluginMenu(String plugin_ID, String plugin_name, int column, 
			Object plugin_menu_item) {
		Integer id = new Integer(column);
		Hashtable<String,PluginMenus> ms = plugin_menus.get(id);
		if(ms==null) ms = new Hashtable<String,PluginMenus>();
		plugin_menus.put(id, ms);
		PluginMenus pm = ms.get(plugin_ID);
		if(pm==null) pm = new PluginMenus(plugin_ID, plugin_name);
		ms.put(plugin_ID, pm);
		pm.addMenu(column, plugin_menu_item);
		if(Application.peers!=null) 
			return Application.peers.registerPluginMenu(plugin_ID, plugin_name, column, plugin_menu_item);
		return false;
	}
	public static boolean unRegisterPluginMenu(String plugin_ID, int column){
		Integer id = new Integer(column);
		Hashtable<String,PluginMenus> ms = plugin_menus.get(id);
		if(ms == null) return false;
		ms.remove(plugin_ID);
		if(Application.peers!=null) 
			return Application.peers.deregisterPluginMenu(plugin_ID, column);
		return false;
	}
	/** 
	 * puts plugin data into:
	 *  D_PluginInfo.plugins,D_PluginInfo.plugin_data, plugin_applets
	 *  calls: Application.peers.registerPlugin
	 * @param plugin_GID
	 * @param plugin_name
	 * @param plugin_info
	 * @param plugin_url
	 * @param renderer
	 * @param editor
	 * @param plugin
	 * @return
	 */
	public static boolean registerPlugin(
			String plugin_GID, String plugin_name, String plugin_info, String plugin_url,
			Object renderer,
			Object editor) {
		boolean result = false;
		if (DEBUG) System.out.println("PluginRegistration: plugin="+plugin_GID);
		if (D_PluginInfo.plugins.contains(plugin_GID)) {
			if(DEBUG) System.out.println("PluginRegistration: duplicate ="+plugin_GID);
			return false;
		}
		D_PluginInfo pd = new D_PluginInfo(plugin_GID,plugin_name,plugin_info,plugin_url,editor,renderer);
		D_PluginInfo.plugin_data.put(plugin_GID, pd);
		D_PluginInfo.plugins.add(plugin_GID);
		plugin_applets.put(plugin_GID, pd);
		if (plugin_GID == null){
			if (DEBUG) System.out.println("PluginRegistration: null ID ="+plugin_GID);
			return false;
		}
		if (Application.peers != null) {
			PluginRegistration x = new PluginRegistration();
			RegisterPlugins rp = x.new RegisterPlugins(plugin_GID, plugin_name, plugin_info, plugin_url, renderer, editor);
			Application_GUI.eventQueue_invokeLater(rp);
		}
		if (DEBUG) System.out.println("StartUpThread:loadPlugins: Plugins Got ="+D_PluginInfo.plugin_data.size());
		return result;
	}
	class RegisterPlugins extends net.ddp2p.common.util.DDP2P_ServiceRunnable {
		private String plugin_GID;
		private String plugin_name;
		private String plugin_info;
		private String plugin_url;
		private Object renderer; 
		private Object editor; 
		/**
		 * 
		 * @param plugin_GID
		 * @param plugin_name
		 * @param plugin_info
		 * @param plugin_url
		 * @param renderer TableCellRenderer
		 * @param editor  TableCellEditor
		 */
		RegisterPlugins(String plugin_GID, String plugin_name, String plugin_info, String plugin_url, Object renderer, Object editor){
			super("RegisterPlugins", false, false, null);
			this.plugin_GID = plugin_GID;
			this.plugin_name = plugin_name; 
			this.plugin_info= plugin_info;
			this.plugin_url = plugin_url;
			this.renderer = renderer;
			this.editor = editor;
		}
		public void _run(){
			if(Application.peers!=null) 
				Application.peers.registerPlugin(plugin_GID, plugin_name, plugin_info, plugin_url, renderer, editor);
		}
	}
	public static boolean unRegisterPlugin(String plugin_GID) {
		if(!D_PluginInfo.plugins.contains(plugin_GID)) return false;
		D_PluginInfo.plugins.remove(plugin_GID);
		plugin_applets.remove(plugin_GID);
		D_PluginInfo.plugin_data.remove(plugin_GID);
		if(Application.peers!=null) 
			return Application.peers.deregisterPlugin(plugin_GID);
		return false;
	}
	public static void loadNewPlugins() {
		String peer_GID = HandlingMyself_Peer.getMyPeerGID();
		String peer_name = HandlingMyself_Peer.getMyPeerName();
		if(DEBUG) System.out.println("PluginRegistration:loadNewPlugins: start");
		URL[] urls = listFiles();
		for(URL url : urls) {
			if(plugin_urls.contains(url)) continue;
			if(DEBUG) System.out.println("PluginRegistration:loadNewPlugins: url="+url);
			try {
				loadPlugin(url, peer_GID, peer_name);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		if(DEBUG) System.out.println("PluginRegistration:loadPlugins: Plugins main Got ="+D_PluginInfo.plugin_data.size());		
	}
	public static void loadPlugins() throws MalformedURLException{
		loadPlugins(HandlingMyself_Peer.getMyPeerGID(), HandlingMyself_Peer.getMyPeerName());
	}
	/**
	 * 
	 * @param plugin_GID
	 * @param plugin_name
	 * @param column
	 * @param plugin_menuItem Action
	 * @return
	 */
	public static boolean registerPluginMenuAction(String plugin_GID, String plugin_name, int column, Object plugin_menuItem) {
		Integer id = new Integer(column);
		Hashtable<String,PluginMenus> ms = PluginRegistration.plugin_menus.get(id);
		if(ms==null) ms = new Hashtable<String,PluginMenus>();
		PluginRegistration.plugin_menus.put(id, ms);
		PluginMenus pm = ms.get(plugin_GID);
		if(pm==null) pm = new PluginMenus(plugin_GID, plugin_name);
		ms.put(plugin_GID, pm);
		pm.addAction(column, plugin_menuItem);
		if(Application.peers!=null)
			return Application.peers.registerPluginMenu(plugin_GID, plugin_name, column, plugin_menuItem);
		return false;
	}
	public static void loadPlugins(String peer_GID, String peer_name) throws MalformedURLException{
		if(DEBUG) System.out.println("PluginRegistration:loadPlugins: start");
		URL[] urls = listFiles();
		for(URL url : urls) {
			loadPlugin(url, peer_GID, peer_name);
		}
		if(DEBUG) System.out.println("PluginRegistration:loadPlugins: Plugins main Got ="+D_PluginInfo.plugin_data.size());
	}
	public static boolean loadPlugin(URL url, String peer_GID, String peer_name) throws MalformedURLException {
		if(_DEBUG) System.out.println("PluginRegistration:loadPlugins: try: "+url);
		boolean result = false;
		URL dd = new File(Application.CURRENT_DD_JAR_BASE_DIR()+Application.DD_JAR).toURI().toURL();
		if(DEBUG) System.out.println("PluginRegistration:loadPlugins: try: ddjar "+dd);
		URLClassLoader child = new URLClassLoader(new URL[]{url,dd}, new Object().getClass().getClassLoader());
		try {
			Class<?> classToLoad = Class.forName(Application.PLUGIN_ENTRY_POINT, true, child);
			java.lang.reflect.Method[] methods = classToLoad.getDeclaredMethods();
			java.lang.reflect.Method method_init_void = null;
			java.lang.reflect.Method method_setPluginData = null;
			java.lang.reflect.Method method_getPluginDataHashtable = null;
			java.lang.reflect.Method method_handleReceivedMessage = null;
			java.lang.reflect.Method method_answerMessage = null;
			java.lang.reflect.Method method_checkOutgoingMessageHashtable = null;
			java.lang.reflect.Method method_confirmStatus = null;
			for (java.lang.reflect.Method m : methods) {
				if (DEBUG) System.out.println("PluginRegistration:loadPlugins: method= "+m);
				if (DEBUG) System.out.println("StartUpThread:loadPlugins: method name= "+m.getName());
				if (m.getName() == "init")
					if (m.getParameterTypes().length == 0) {method_init_void = m;}
				if (m.getName() == "getPluginDataHashtable")
					if (m.getParameterTypes().length == 0) {method_getPluginDataHashtable = m;}
				if (m.getName() == "checkOutgoingMessageHashtable")
					if (m.getParameterTypes().length == 0) {method_checkOutgoingMessageHashtable = m;}
				if (m.getName() == "handleReceivedMessage")
					if (m.getParameterTypes().length == 2) {method_handleReceivedMessage = m;}
				if (m.getName() == "answerMessage")
					if (m.getParameterTypes().length == 2) {method_answerMessage = m;}
				if (m.getName() == "setPluginData")
					if (m.getParameterTypes().length == 2) {method_setPluginData = m;}
				if (m.getName() == "confirmStatus")
					if (m.getParameterTypes().length == 2) {method_confirmStatus = m;}
			}
			if (DEBUG) System.out.println("PluginRegistration:loadPlugins: invoking init");
			if (method_init_void == null) method_init_void = classToLoad.getDeclaredMethod("init", new Class[]{});
			method_init_void.setAccessible(true);
			method_init_void.invoke(null);
			String plugin_GID = null;
			D_PluginInfo plugin_info = null;
			try {
				if (DEBUG) System.out.println("PluginRegistration:loadPlugins: invoking setPluginData");
				if (method_setPluginData == null) method_setPluginData = classToLoad.getDeclaredMethod("setPluginData", new Class[]{String.class,String.class});
				method_setPluginData.setAccessible(true);
				method_setPluginData.invoke(null, new Object[]{peer_GID, peer_name});
				@SuppressWarnings("unchecked")
				Hashtable<String,Object> data = (Hashtable<String,Object>) method_getPluginDataHashtable.invoke(null);
				plugin_info = new D_PluginInfo().setHashtable(data);
				if (_DEBUG) System.out.println("PluginRegistration:loadPlugins: pluginGID="+plugin_info.plugin_GID);
				if ((plugin_info.plugin_GID == null) || (s_methods.get(plugin_info.plugin_GID) != null)) {
						System.out.println("PluginRegistration:loadPlugins: pluginGID in s_methods should be null: LOOKS LIKE A GID CONFLICT: this GID is: "+plugin_info.plugin_GID);
					return false;
				}
				plugin_GID = plugin_info.plugin_GID;
				PluginRegistration.registerPlugin(plugin_info.plugin_GID, plugin_info.plugin_name, plugin_info.plugin_info, plugin_info.plugin_url,
						plugin_info.mTableCellRenderer,
						plugin_info.mTableCellEditor);
			}catch(Exception e){System.err.println("PluginRegistration:loadPlugin: method: try: "+url);e.printStackTrace();}
			if(plugin_GID == null){
				if(_DEBUG) System.out.println("PluginRegistration:loadPlugins: null pluginGID");
				return result;
			}
			PluginMethods p_methods = new PluginMethods();
			p_methods.method_init_void = method_init_void;
			p_methods.setPluginData = method_setPluginData;
			p_methods.getPluginDataHashtable = method_getPluginDataHashtable;
			p_methods.checkOutgoingMessageHashtable = method_checkOutgoingMessageHashtable;
			p_methods.confirmStatus = method_confirmStatus;
			p_methods.handleReceivedMessage = method_handleReceivedMessage;
			p_methods.answerMessage = method_answerMessage;
			s_methods.put(plugin_GID, p_methods);
			PluginThread pThread = new PluginThread(classToLoad, p_methods, plugin_info.plugin_name, url);
			pThread.start();
			if(DEBUG) System.out.println("PluginRegistration:loadPlugins: thread started");
			PluginRegistration.threads.put(plugin_GID, pThread);
			PluginRegistration.plugin_urls.add(url);
			result = true;
		} catch (ClassNotFoundException e) {
			System.err.println("PluginRegistration:loadPlugin: abandon: try: "+url);
			e.printStackTrace();
		} catch (SecurityException e) {
			System.err.println("PluginRegistration:loadPlugin: abandon: try: "+url);
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			System.err.println("PluginRegistration:loadPlugin: abandon: try: "+url);
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.err.println("PluginRegistration:loadPlugin: abandon: try: "+url);
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.err.println("PluginRegistration:loadPlugin: abandon: try: "+url);
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			System.err.println("PluginRegistration:loadPlugin: abandon: try: "+url);
			e.printStackTrace();
		}
		return result;
	}
	public static boolean loadPlugin(Class<?> plugin, String peer_GID, String peer_name) throws MalformedURLException {
		boolean DEBUG = true;
		if (_DEBUG) System.out.println("PluginRegistration:loadPlugins: try: "+plugin);
		boolean result = false;
		URL url = new URL("file://class/"+plugin);
		if (plugin_urls.contains(url)) {
			if (_DEBUG) System.out.println("PluginRegistration:loadPlugins: quit already installed: "+plugin);
			return result;
		}
		try {
			Class<?> classToLoad = plugin; 
			java.lang.reflect.Method[] methods = classToLoad.getDeclaredMethods();
			java.lang.reflect.Method method_init_void = null;
			java.lang.reflect.Method method_setPluginData = null;
			java.lang.reflect.Method method_getPluginDataHashtable = null;
			java.lang.reflect.Method method_handleReceivedMessage = null;
			java.lang.reflect.Method method_answerMessage = null;
			java.lang.reflect.Method method_checkOutgoingMessageHashtable = null;
			java.lang.reflect.Method method_confirmStatus = null;
			for (java.lang.reflect.Method m : methods) {
				if (DEBUG) System.out.println("PluginRegistration:loadPlugins: method= "+m);
				if (DEBUG) System.out.println("StartUpThread:loadPlugins: method name= \""+m.getName()+"\"");
				if ("init".equals(m.getName())) {
					if (DEBUG) System.out.println("PluginRegistration:loadPlugins: found method init");
					if (m.getParameterTypes().length == 0) {method_init_void = m;}
					else {
						if (DEBUG) System.out.println("PluginRegistration:loadPlugins: found method init #"+m.getParameterTypes().length);
					}
				} else {
					if (DEBUG) System.out.println("PluginRegistration:loadPlugins: not found method init");
				}
				if ("getPluginDataHashtable".equals(m.getName())) {
					if (DEBUG) System.out.println("PluginRegistration:loadPlugins: found method getPluginDataHashtable");
					if (m.getParameterTypes().length == 0) {method_getPluginDataHashtable = m;}
					else {
						if (DEBUG) System.out.println("PluginRegistration:loadPlugins: found method getPluginDataHa #"+m.getParameterTypes().length);
					}
				} else {
					if (DEBUG) System.out.println("PluginRegistration:loadPlugins: not found method getPluginDataHashtable");
				}
				if ("checkOutgoingMessageHashtable".equals(m.getName()))
					if (m.getParameterTypes().length == 0) {method_checkOutgoingMessageHashtable = m;}
				if ("handleReceivedMessage".equals(m.getName()))
					if (m.getParameterTypes().length == 2) {method_handleReceivedMessage = m;}
				if ("answerMessage".equals(m.getName()))
					if (m.getParameterTypes().length == 2) {method_answerMessage = m;}
				if ("setPluginData".equals(m.getName())) {
					if (DEBUG) System.out.println("PluginRegistration:loadPlugins: found method setPluginData");
					if (m.getParameterTypes().length == 2) {method_setPluginData = m;}
					else {
						if (DEBUG) System.out.println("PluginRegistration:loadPlugins: found method setPluginData #"+m.getParameterTypes().length);
					}
				}
				if ("confirmStatus".equals(m.getName()))
					if (m.getParameterTypes().length == 2) {method_confirmStatus = m;}
			}
			if (DEBUG) System.out.println("PluginRegistration:loadPlugins: invoking init");
			if (method_init_void == null) method_init_void = classToLoad.getDeclaredMethod("init", new Class[]{});
			method_init_void.setAccessible(true);
			method_init_void.invoke(null);
			String plugin_GID = null;
			D_PluginInfo plugin_info = null;
			try {
				if (DEBUG) System.out.println("PluginRegistration:loadPlugins: invoking setPluginData");
				if (method_setPluginData == null) method_setPluginData = classToLoad.getDeclaredMethod("setPluginData", new Class[]{String.class,String.class});
				method_setPluginData.setAccessible(true);
				method_setPluginData.invoke(null, new Object[]{peer_GID, peer_name});
				@SuppressWarnings("unchecked")
				Hashtable<String,Object> data = (Hashtable<String,Object>) method_getPluginDataHashtable.invoke(null);
				plugin_info = new D_PluginInfo().setHashtable(data);
				if (_DEBUG) System.out.println("PluginRegistration:loadPlugins: pluginGID="+plugin_info.plugin_GID);
				if ((plugin_info.plugin_GID == null) || (s_methods.get(plugin_info.plugin_GID) != null)) {
						System.out.println("PluginRegistration:loadPlugins: pluginGID in s_methods should be null: LOOKS LIKE A GID CONFLICT: this GID is: "+plugin_info.plugin_GID);
					return false;
				}
				plugin_GID = plugin_info.plugin_GID;
				PluginRegistration.registerPlugin(plugin_info.plugin_GID, plugin_info.plugin_name, plugin_info.plugin_info, plugin_info.plugin_url,
						plugin_info.mTableCellRenderer,
						plugin_info.mTableCellEditor);
			} catch(Exception e){System.err.println("PluginRegistration:loadPlugin: method: try: "+plugin);e.printStackTrace();}
			if (plugin_GID == null) {
				if (_DEBUG) System.out.println("PluginRegistration:loadPlugins: null pluginGID");
				return result;
			}
			PluginMethods p_methods = new PluginMethods();
			p_methods.method_init_void = method_init_void;
			p_methods.setPluginData = method_setPluginData;
			p_methods.getPluginDataHashtable = method_getPluginDataHashtable;
			p_methods.checkOutgoingMessageHashtable = method_checkOutgoingMessageHashtable;
			p_methods.confirmStatus = method_confirmStatus;
			p_methods.handleReceivedMessage = method_handleReceivedMessage;
			p_methods.answerMessage = method_answerMessage;
			s_methods.put(plugin_GID, p_methods);
			PluginThread pThread = new PluginThread(classToLoad, p_methods, plugin_info.plugin_name, url);
			pThread.start();
			if(DEBUG) System.out.println("PluginRegistration:loadPlugins: thread started");
			PluginRegistration.threads.put(plugin_GID, pThread);
			PluginRegistration.plugin_urls.add(url);
			result = true;
		} catch (SecurityException e) {
			System.err.println("PluginRegistration:loadPlugin: abandon: try: "+plugin);
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			System.err.println("PluginRegistration:loadPlugin: abandon: try: "+plugin);
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			System.err.println("PluginRegistration:loadPlugin: abandon: try: "+plugin);
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.err.println("PluginRegistration:loadPlugin: abandon: try: "+plugin);
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			System.err.println("PluginRegistration:loadPlugin: abandon: try: "+plugin);
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("PluginRegistration:loadPlugin: abandon: try: "+plugin);
			e.printStackTrace();
		}
		return result;
	}
	public static URL[] listFiles() {
		String path = Application.CURRENT_PLUGINS_BASE_DIR(); 
		String files;
		File folder = new File(path);
		if(!folder.exists()) return new URL[0];
		File[] listOfFiles = folder.listFiles();
		if(listOfFiles==null) return new URL[0];
		ArrayList<URL> urls = new ArrayList<URL>();
		for (int i = 0; i < listOfFiles.length; i++) 
		{
			if (listOfFiles[i].isFile()) 
			{
				files = listOfFiles[i].getName();
				if (files.toLowerCase().endsWith(".jar")) // || files.endsWith(".JAR"))
				{
					try {
						urls.add(listOfFiles[i].toURI().toURL());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return urls.toArray(new URL[0]);
	}
	public static void main(String[] args) {
		try {
			loadPlugins("gid","name");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 
	 */
	public static void removePlugins() {
		for (String plugin: threads.keySet()) {
			PluginThread pt = threads.get(plugin);
			pt.turnOff();
			unRegisterPlugin(plugin);
		}
	}
}
