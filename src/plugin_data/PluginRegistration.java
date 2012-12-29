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

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import config.Application;
import config.DD;
import data.D_PluginInfo;

public class PluginRegistration {
	public static Hashtable<String,D_PluginInfo> plugin_applets = new Hashtable<String, D_PluginInfo>();
	public static Hashtable<Integer,Hashtable<String,PluginMenus>> plugin_menus = new Hashtable<Integer,Hashtable<String,PluginMenus>>();
	public static Hashtable<String,PluginMethods> s_methods = new Hashtable<String,PluginMethods>();
	private static Hashtable<String,PluginThread> threads =  new Hashtable<String,PluginThread>();
	private static HashSet<URL> plugin_urls = new HashSet<URL>();
	
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public static boolean registerPluginMenu(String plugin_GID, String plugin_name, int column, Action plugin_menuItem) {
		Integer id = new Integer(column);
		Hashtable<String,PluginMenus> ms = plugin_menus.get(id);
		if(ms==null) ms = new Hashtable<String,PluginMenus>();
		plugin_menus.put(id, ms);
		PluginMenus pm = ms.get(plugin_GID);
		if(pm==null) pm = new PluginMenus(plugin_GID, plugin_name);
		ms.put(plugin_GID, pm);
		pm.add(column, plugin_menuItem);
		return Application.peers.registerPluginMenu(plugin_GID, plugin_name, column, plugin_menuItem);
	}
	
	public static boolean registerPluginMenu(String plugin_ID, String plugin_name, int column, JMenuItem plugin_menu_item) {
		Integer id = new Integer(column);
		Hashtable<String,PluginMenus> ms = plugin_menus.get(id);
		if(ms==null) ms = new Hashtable<String,PluginMenus>();
		plugin_menus.put(id, ms);
		PluginMenus pm = ms.get(plugin_ID);
		if(pm==null) pm = new PluginMenus(plugin_ID, plugin_name);
		ms.put(plugin_ID, pm);
		pm.add(column, plugin_menu_item);
		return Application.peers.registerPluginMenu(plugin_ID, plugin_name, column, plugin_menu_item);
	}
	public static boolean unRegisterPluginMenu(String plugin_ID, int column){
		Integer id = new Integer(column);
		Hashtable<String,PluginMenus> ms = plugin_menus.get(id);
		if(ms == null) return false;
		ms.remove(plugin_ID);
		return Application.peers.deregisterPluginMenu(plugin_ID, column);
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
			TableCellRenderer renderer, TableCellEditor editor) {
		boolean result = false;
		if(DEBUG) System.out.println("PluginRegistration: plugin="+plugin_GID);
		if(D_PluginInfo.plugins.contains(plugin_GID)) {
			if(DEBUG) System.out.println("PluginRegistration: duplicate ="+plugin_GID);
			return false;
		}
		D_PluginInfo pd = new D_PluginInfo(plugin_GID,plugin_name,plugin_info,plugin_url,editor,renderer);
		D_PluginInfo.plugin_data.put(plugin_GID, pd);
		D_PluginInfo.plugins.add(plugin_GID);
		//if(plugin!=null)plugin.setSendPipe(data.D_PluginData.getPeerConnection());
		plugin_applets.put(plugin_GID, pd);

		if(plugin_GID==null){
			if(DEBUG) System.out.println("PluginRegistration: null ID ="+plugin_GID);
			return false;
		}
		
		if(Application.peers!=null){
			PluginRegistration x = new PluginRegistration();
			RegisterPlugins rp = x.new RegisterPlugins(plugin_GID, plugin_name, plugin_info, plugin_url, renderer, editor);
			EventQueue.invokeLater(rp);
			//result = Application.peers.registerPlugin(plugin_GID, plugin_name, plugin_info, plugin_url, renderer, editor);
		}
		if(DEBUG) System.out.println("StartUpThread:loadPlugins: Plugins Got ="+D_PluginInfo.plugin_data.size());
		return result;
	}
	class RegisterPlugins implements Runnable {
		private String plugin_GID;
		private String plugin_name;
		private String plugin_info;
		private String plugin_url;
		private TableCellRenderer renderer;
		private TableCellEditor editor;
		RegisterPlugins(String plugin_GID, String plugin_name, String plugin_info, String plugin_url, TableCellRenderer renderer, TableCellEditor editor){
			this.plugin_GID = plugin_GID;
			this.plugin_name = plugin_name; 
			this.plugin_info= plugin_info;
			this.plugin_url = plugin_url;
			this.renderer = renderer;
			this.editor = editor;
		}
		public void run(){
			Application.peers.registerPlugin(plugin_GID, plugin_name, plugin_info, plugin_url, renderer, editor);
		}
	}
	public static boolean unRegisterPlugin(String plugin_GID) {
		if(!D_PluginInfo.plugins.contains(plugin_GID)) return false;
		D_PluginInfo.plugins.remove(plugin_GID);
		plugin_applets.remove(plugin_GID);
		D_PluginInfo.plugin_data.remove(plugin_GID);
		return Application.peers.deregisterPlugin(plugin_GID);
	}

	public static void loadNewPlugins() {
		String peer_GID = DD.getMyPeerGIDFromIdentity();
		String peer_name = DD.getMyPeerName();
		//boolean DEBUG = true;
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
		loadPlugins(DD.getMyPeerGIDFromIdentity(), DD.getMyPeerName());
	}
	public static void loadPlugins(String peer_GID, String peer_name) throws MalformedURLException{
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("PluginRegistration:loadPlugins: start");
		URL[] urls = listFiles();
		for(URL url : urls) {
			loadPlugin(url, peer_GID, peer_name);
		}
		if(DEBUG) System.out.println("PluginRegistration:loadPlugins: Plugins main Got ="+D_PluginInfo.plugin_data.size());
	}
	public static boolean loadPlugin(URL url, String peer_GID, String peer_name) throws MalformedURLException {
		if(DEBUG) System.out.println("PluginRegistration:loadPlugins: try: "+url);
		boolean result = false;
		URL dd = new File(Application.CURRENT_DD_JAR_BASE_DIR()/*+Application.OS_PATH_SEPARATOR*/+Application.DD_JAR).toURI().toURL();
		if(DEBUG) System.out.println("PluginRegistration:loadPlugins: try: ddjar "+dd);
		/*
		String[] bjars={"eventbus.jar", 
		"jME3-desktop.jar", 
		"jME3-lwjgl-natives.jar",
		"jME3-plugins.jar",
		"lwjgl.jar",
		"stack-alloc.jar",
		"j-ogg-oggd.jar",
		"jME3-effects.jar",
		"jME3-lwjgl.jar",
		"jME3-terrain.jar",
		"nifty-default-controls.jar",
		"vecmath.jar",
		"j-ogg-vorbisd.jar",
		"jME3-jbullet.jar",
		"jME3-networking.jar",
		"jbullet.jar",
		"nifty-style-black.jar", 
		"xmlpull-xpp3.jar",
		"jME3-core.jar",
		"jME3-jogg.jar", 
		"jME3-niftygui.jar",
		"jinput.jar",
		"nifty.jar",
		"MyGame.jar"};
		URL[]ujars = new URL[bjars.length+2];
		for(int i = 0; i<bjars.length; i++) {
			ujars[i+2] = new File(Application.CURRENT_PLUGINS_BASE_DIR()+"BG"+Application.OS_PATH_SEPARATOR+bjars[i]).toURI().toURL();
		}
		ujars[0]=url; ujars[1] = dd;
		URLClassLoader child = new URLClassLoader(ujars, new Object().getClass().getClassLoader());
		*/
		URLClassLoader child = new URLClassLoader(new URL[]{url,dd}, new Object().getClass().getClassLoader());
		try {
			Class<?> classToLoad = Class.forName(Application.PLUGIN_ENTRY_POINT, true, child);
			java.lang.reflect.Method[] methods = classToLoad.getDeclaredMethods();
			java.lang.reflect.Method method_init_void = null;
			//java.lang.reflect.Method method_getPluginData = null;
			//java.lang.reflect.Method method_getPluginGID = null;
			//java.lang.reflect.Method method_getPluginName = null;
			//java.lang.reflect.Method method_getPeerPlugin = null;
			java.lang.reflect.Method method_setPluginData = null;
			java.lang.reflect.Method method_getPluginDataHashtable = null;
			java.lang.reflect.Method method_handleReceivedMessage = null;
			java.lang.reflect.Method method_answerMessage = null;
			java.lang.reflect.Method method_checkOutgoingMessageHashtable = null;
			java.lang.reflect.Method method_confirmStatus = null;
			for(java.lang.reflect.Method m : methods) {
				if(DEBUG) System.out.println("PluginRegistration:loadPlugins: method= "+m);
				if(DEBUG) System.out.println("StartUpThread:loadPlugins: method name= "+m.getName());
				//if(DEBUG) System.out.println("StartUpThread:loadPlugins: method modifiers= "+m.getModifiers());
				//if(DEBUG) System.out.println("StartUpThread:loadPlugins: method params= "+Util.concat(m.getParameterTypes(),""));
				// if(m.getName() == "getPluginData") if(m.getParameterTypes().length==0){method_getPluginData = m;}
				//if(m.getName() == "getPluginGID") if(m.getParameterTypes().length==0){method_getPluginGID = m;}
				//if(m.getName() == "getPluginName") if(m.getParameterTypes().length==0){method_getPluginName = m;}
				//if(m.getName() == "getPeerPlugin")if(m.getParameterTypes().length==0){method_getPeerPlugin = m;}
				if(m.getName() == "init")
					if(m.getParameterTypes().length==0){method_init_void = m;}
				if(m.getName() == "getPluginDataHashtable")
					if(m.getParameterTypes().length==0){method_getPluginDataHashtable = m;}
				if(m.getName() == "checkOutgoingMessageHashtable")
					if(m.getParameterTypes().length==0){method_checkOutgoingMessageHashtable = m;}
				if(m.getName() == "handleReceivedMessage")
					if(m.getParameterTypes().length==2){method_handleReceivedMessage = m;}
				if(m.getName() == "answerMessage")
					if(m.getParameterTypes().length==2){method_answerMessage = m;}
				if(m.getName() == "setPluginData")
					if(m.getParameterTypes().length==2){method_setPluginData = m;}
				if(m.getName() == "confirmStatus")
					if(m.getParameterTypes().length==2){method_confirmStatus = m;}
			}

			if(DEBUG) System.out.println("PluginRegistration:loadPlugins: invoking init");
			if(method_init_void==null) method_init_void = classToLoad.getDeclaredMethod("init", new Class[]{});
			method_init_void.setAccessible(true);
			//Object instance = classToLoad.newInstance();
			//Object result = 
			method_init_void.invoke(null/*instance*/);
			//D_PluginInfo plugin_info = null;
			String plugin_GID = null;
			try {
				//if(method_getPluginData==null) method_getPluginData = classToLoad.getDeclaredMethod("getPluginData", new Class[]{});
				//method_getPluginData.setAccessible(true);
				//plugin_info = (data.D_PluginInfo) method_getPluginData.invoke(null/*instance*/);
				//plugin_GID = (String) method_getPluginGID.invoke(null/*instance*/);
				//if(DEBUG) System.out.println("*****StartUpThread:loadPlugins:Got PR:"+result);
				if(DEBUG) System.out.println("PluginRegistration:loadPlugins: invoking setPluginData");
				if(method_setPluginData==null) method_setPluginData = classToLoad.getDeclaredMethod("setPluginData", new Class[]{String.class,String.class});
				method_setPluginData.setAccessible(true);
				method_setPluginData.invoke(null/*instance*/, new Object[]{peer_GID, peer_name});

				@SuppressWarnings("unchecked")
				Hashtable<String,Object> data = (Hashtable<String,Object>) method_getPluginDataHashtable.invoke(null);
				D_PluginInfo i = new D_PluginInfo().setHashtable(data);
				if(s_methods.get(i.plugin_GID) != null){
					//if(_DEBUG)
						System.out.println("PluginRegistration:loadPlugins: pluginGID in s_methods should be null: LOOKS LIKE A GID CONFLICT: this GID is: "+i.plugin_GID);
					return false;
				}
				plugin_GID = i.plugin_GID;
				PluginRegistration.registerPlugin(i.plugin_GID, i.plugin_name, i.plugin_info, i.plugin_url, i.renderer, i.editor);
			}catch(Exception e){e.printStackTrace();}
			if(plugin_GID == null){
				if(_DEBUG) System.out.println("PluginRegistration:loadPlugins: null pluginGID");
				return result;
			}
			PluginMethods p_methods = new PluginMethods();
			p_methods.method_init_void = method_init_void;
			p_methods.setPluginData = method_setPluginData;
			//p_methods.getPluginGID = method_getPluginGID;
			//p_methods.getPluginName = method_getPluginName;
			//p_methods.getPeerPlugin = method_getPeerPlugin;
			//p_methods.getPluginData = method_getPluginData;
			p_methods.getPluginDataHashtable = method_getPluginDataHashtable;
			p_methods.checkOutgoingMessageHashtable = method_checkOutgoingMessageHashtable;
			p_methods.confirmStatus = method_confirmStatus;
			p_methods.handleReceivedMessage = method_handleReceivedMessage;
			p_methods.answerMessage = method_answerMessage;

			s_methods.put(plugin_GID, p_methods);

			PluginThread pThread = new PluginThread(classToLoad, p_methods);
			pThread.start();
			if(DEBUG) System.out.println("PluginRegistration:loadPlugins: thread started");
			PluginRegistration.threads.put(plugin_GID, pThread);

			PluginRegistration.plugin_urls.add(url);
			result = true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		//} catch (InstantiationException e) {e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return result;
	}
	public static URL[] listFiles() {
		// Directory path here
		String path = Application.CURRENT_PLUGINS_BASE_DIR(); //+Application.OS_PATH_SEPARATOR+Application.APP_INSTALLED_PLUGINS_ROOT_FOLDER; 

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
					//System.out.println(files);
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

	public static void removePlugins() {
		// TODO Auto-generated method stub
		
	}
}
