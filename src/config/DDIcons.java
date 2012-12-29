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

package config;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.Hashtable;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import util.Util;

public class DDIcons{
	private static String I_ADD="add.png"; // action
	private static String I_DEL="remove.png"; //action
	//private static String I_RES="neutral.smiley.gif"; // action
	private static String I_RES="sad.smiley19.gif"; // action
	private static String I_WIT="neutral.smiley.gif"; // action
	private static final String I_DELE = "del.png";
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	
	//public static String I_INHAB = "anonym.gif"; // not used
	//public static String I_NEIGHS30 = "neighs30.gif";
	public static String I_NEIGHS19 = "neighs19.gif"; //icon constitutents tree
	public static String I_INHAB19 = "anonym19.gif"; //icon constitutents tree
	private static String I_PROF = "profile20.gif";
	
	//private static String I_UP15 = "icons/happy.smiley15.gif"; //icon constitutents tree
	//private static String I_DOWN15 = "icons/sad.smiley15.gif"; //icon constitutents tree
	public static String I_UP19 = "happy.smiley19.gif"; //icon constitutents tree
	public static String I_DOWN19 = "sad.smiley19.gif"; //icon constitutents tree
	//private static String I_UP32 = "icons/happy.smiley.gif"; //icon constitutents tree
	//private static String I_DOWN32 = "icons/sad.smiley.gif"; //icon constitutents tree
	
	private static String I_ORG = "sad.smiley10.gif";	
	
	private static String I_GREEN_BALL = "green-sphere.gif";
	private static String I_GRAY_BALL = "dark-sphere.gif";
	private static String I_RED_BALL = "red-sphere.gif";		
	
	//LargeSplash.jpg

	private static ImageIcon _II_WIT = null;
	public static ImageIcon getWitImageIcon(String descr){
		//System.out.println("SSIcons:getWitImageIcon:");
		try{if(_II_WIT == null) _II_WIT= getImageIconFromResource(I_WIT, descr);}catch(Exception e){e.printStackTrace();}
		//if(_II_WIT == null) Util.printCallPath("null");
		//System.out.println("SSIcons:getWitImageIcon: got="+_II_WIT);
		return _II_WIT;
	}
	private static ImageIcon _II_DEL = null;
	public static ImageIcon getDelImageIcon(String descr){
		try{if(_II_DEL == null) _II_DEL= getImageIconFromResource(I_DEL, descr);}catch(Exception e){e.printStackTrace();}
		if(_II_DEL == null) Util.printCallPath("null");
		return _II_DEL;
	}
	private static ImageIcon _II_ADD = null;
	public static ImageIcon getAddImageIcon(String descr){
		try{if(_II_ADD == null) _II_ADD= getImageIconFromResource(I_ADD, descr);}catch(Exception e){e.printStackTrace();}
		if(_II_ADD == null) Util.printCallPath("null");
		return _II_ADD;
	}
	private static ImageIcon _II_RES = null;
	public static ImageIcon getResImageIcon(String descr){
		try{
			if(_II_RES == null){
				if(DEBUG) System.out.println("DDIcons:getResImageIcon: will get:"+descr);
				_II_RES= getImageIconFromResource(I_RES, descr);
			}
		}catch(Exception e){e.printStackTrace();}
		if(_II_RES == null) Util.printCallPath("null");
		return _II_RES;
	}
	private static URL _II_DELE = null;
	public static URL getDeleURL() {
		if(_II_DELE==null){
			if(DEBUG) System.out.println("DDIcons:getDeleImageIcon: will get");
			_II_DELE = DD.class.getResource(Application.RESOURCES_ENTRY_POINT+I_DELE);
		}
		if(_II_DELE == null) Util.printCallPath("null");
		return _II_DELE;
	}
	private static ImageIcon _II_DELE_ = null;
	public static ImageIcon getDeleImageIcon(String descr){
		if(_II_DELE_ == null) _II_DELE_= getImageIconFromResource(I_DELE, descr);
		if(_II_DELE_ == null) Util.printCallPath("null");
		return _II_DELE_;
	}
	private static ImageIcon _II_ORG = null;
	public static ImageIcon getOrgImageIcon(String descr){
		if(_II_ORG == null) _II_ORG= getImageIconFromResource(I_ORG, descr);
		if(_II_ORG == null) Util.printCallPath("null");
		return _II_ORG;
	}
	private static ImageIcon _II_PROF = null;
	public static ImageIcon getProfileImageIcon(String descr){
		if(_II_PROF == null) _II_PROF= getImageIconFromResource(I_PROF, descr);
		return _II_PROF;
	}
	private static ImageIcon _II_GREEN_BALL = null;
	public static ImageIcon getGreenBallImageIcon(String descr){
		if(_II_GREEN_BALL == null) _II_GREEN_BALL= getImageIconFromResource(I_GREEN_BALL, descr);
		return _II_GREEN_BALL;
	}
	private static ImageIcon _II_GRAY_BALL = null;
	public static ImageIcon getGreyBallImageIcon(String descr){
		if(_II_GRAY_BALL == null) _II_GRAY_BALL= getImageIconFromResource(I_GRAY_BALL, descr);
		return _II_GRAY_BALL;
	}
	private static ImageIcon _II_RED_BALL = null;
	public static ImageIcon getRedBallImageIcon(String descr){
		if(_II_RED_BALL == null) _II_RED_BALL= getImageIconFromResource(I_RED_BALL, descr);
		return _II_RED_BALL;
	}
	
	/**
	 * DD.frame should be non-null
	 * splash will get prepended  Application.RESOURCES_ENTRY_POINT
	 * 
	 * @param splash
	 * @return
	 */
	public static ImageIcon getImageIconFromResource(String splash){
		return getImageIconFromResource(splash, DD.frame, null);
	}
	public static ImageIcon getImageIconFromResource(String splash, String descr){
		return getImageIconFromResource(splash, DD.frame, descr);
	}
	/**
	 * _frame should be non-null
	 * splash will get prepended  Application.RESOURCES_ENTRY_POINT
	 * 
	 * @param _splash
	 * @param _frame
	 * @return
	 */
	public static ImageIcon getImageIconFromResource(String _splash, Object _frame, String descr){
		Class<?> cl;
		if(_frame == null) cl = DD.class;
		else cl = _frame.getClass();
		if(_splash==null) {
			if(_DEBUG) System.out.println("DDIcons:getImageIconFromResource2: will get null :"+_splash);
			Util.printCallPath("missing null");
			_splash = Application.MISSING_ICON;
		}
		if("null".equals(_splash)){
			if(_DEBUG) System.out.println("DDIcons:getImageIconFromResource2: will get \"null\"");
			Util.printCallPath("missing null");
			_splash = Application.MISSING_ICON;
		}
		String splash = Application.RESOURCES_ENTRY_POINT+_splash;
		ImageIcon image = new ImageIcon();//");
		if(DEBUG) System.out.println("DDIcons:getImageIconFromResource2: will get: "+splash);
		URL url = cl.getResource(splash);
		if(url == null) {
			System.err.println("DDIcons:getImageIconFromResource2:Fail to find resouce: "+splash);
			//Util.printCallPath("missing image");
			if(_DEBUG) System.out.println("DDIcons:getImageIconFromResource2: will get missing:"+Application.RESOURCES_ENTRY_POINT+Application.MISSING_ICON);
			url = DD.class.getResource(Application.RESOURCES_ENTRY_POINT+Application.MISSING_ICON);
			if(url==null)
				if(_DEBUG) System.out.println("DDIcons:getImageIconFromResource2: missing missing:"+Application.RESOURCES_ENTRY_POINT+Application.MISSING_ICON);
		}
		if(url==null){
			if(_DEBUG) System.out.println("DDIcons:getImageIconFromResource2: abandon");
			return null;
		}
        image.setImage(Toolkit.getDefaultToolkit().getImage(url));
    	//Image newimg = img.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
        if(descr!=null) image.setDescription(descr);
        return image;
	}
	public static ImageIcon getImageIconFromResource(String entry, String _splash, Object _frame, String descr){
		Class<?> cl;
		if(_frame == null) cl = DD.class;
		else cl = _frame.getClass();
		if(_splash==null) {
			if(_DEBUG) System.out.println("DDIcons:getImageIconFromResource: will get null :"+_splash);
			Util.printCallPath("missing null");
			_splash = Application.MISSING_ICON;
		}
		if("null".equals(_splash)){
			if(_DEBUG) System.out.println("DDIcons:getImageIconFromResource: will get \"null\"");
			Util.printCallPath("missing null");
			_splash = Application.MISSING_ICON;
		}
		String splash = entry+_splash;
		ImageIcon image = new ImageIcon();//");
		if(DEBUG) System.out.println("DDIcons:getImageIconFromResource: will get:"+splash);
		URL url = cl.getResource(splash);
		if(url == null) {
			System.err.println("Fail to find resouce: "+splash);
			Util.printCallPath("missing image");
			if(_DEBUG) System.out.println("DDIcons:getImageIconFromResource: will get missing:"+Application.RESOURCES_ENTRY_POINT+Application.MISSING_ICON);
			url = DD.class.getResource(Application.RESOURCES_ENTRY_POINT+Application.MISSING_ICON);
			if(url==null)
				if(_DEBUG) System.out.println("DDIcons:getImageIconFromResource: missing missing:"+Application.RESOURCES_ENTRY_POINT+Application.MISSING_ICON);
		}
		if(url==null){
			if(_DEBUG) System.out.println("DDIcons:getImageIconFromResource: abandon");
			return null;
		}
		Image im = Toolkit.getDefaultToolkit().getImage(url);
        image.setImage(im);
    	//Image newimg = img.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
        if(descr!=null) image.setDescription(descr);
        return image;
	}
	public static Image getImageFromResource(String _splash, Object _frame){
	    return getImageFromResource(Application.RESOURCES_ENTRY_POINT, _splash, _frame);
	}
	public static Image getImageFromResource(String entry, String _splash, Object _frame){
		Class<?> cl;
		if(_frame == null) cl = DD.class;
		else cl = _frame.getClass();
		if(_splash==null) {
			if(_DEBUG) System.out.println("DDIcons:getImageFromResource: will get null :"+_splash);
			Util.printCallPath("missing null");
			_splash = Application.MISSING_ICON;
		}
		if("null".equals(_splash)){
			if(_DEBUG) System.out.println("DDIcons:getImageFromResource: will get \"null\"");
			Util.printCallPath("missing null");
			_splash = Application.MISSING_ICON;
		}
		String splash = entry+_splash;
		Image image; // = new Image();//");
		if(DEBUG) System.out.println("DDIcons:getImageFromResource: will get:"+splash);
		URL url = cl.getResource(splash);
		if(url == null) {
			System.err.println("Fail to find resouce: "+splash);
			Util.printCallPath("missing image");
			if(_DEBUG) System.out.println("DDIcons:getImageFromResource: will get missing:"+Application.RESOURCES_ENTRY_POINT+Application.MISSING_ICON);
			url = DD.class.getResource(Application.RESOURCES_ENTRY_POINT+Application.MISSING_ICON);
			if(url==null)
				if(_DEBUG) System.out.println("DDIcons:getImageFromResource: missing missing:"+Application.RESOURCES_ENTRY_POINT+Application.MISSING_ICON);
		}
		if(url==null){
			if(_DEBUG) System.out.println("DDIcons:getImageFromResource: abandon");
			return null;
		}
        image = Toolkit.getDefaultToolkit().getImage(url);
    	//Image newimg = img.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
        //if(descr!=null) image.setDescription(descr);
        return image;
	}
	static Hashtable<String,URL> loadedIcons = new Hashtable<String,URL>();
	public static URL getResourceURL(String name) {
		URL url = null;
		if(name==null) {
			if(_DEBUG) System.out.println("DDIcons:getResourceURL: will get null :"+name);
			Util.printCallPath("missing null");
			name = Application.MISSING_ICON;
		}
		if("null".equals(name)){
			if(_DEBUG) System.out.println("DDIcons:getResourceURL: will get \"null\"");
			Util.printCallPath("missing null");
			name = Application.MISSING_ICON;
		}
		url = loadedIcons.get(name);
		if (url==null){
			if(DEBUG) System.out.println("DDIcons:getResourceURL: will get:"+Application.RESOURCES_ENTRY_POINT+name);
			url = DD.class.getResource(Application.RESOURCES_ENTRY_POINT+name);
			if(url == null) {
				System.err.println("Fail to find resouce: "+Application.RESOURCES_ENTRY_POINT+name);
				Util.printCallPath("missing image");
				if(_DEBUG) System.out.println("DDIcons:getResourceURL: will get missing:"+Application.RESOURCES_ENTRY_POINT+Application.MISSING_ICON);
				url = DD.class.getResource(Application.RESOURCES_ENTRY_POINT+Application.MISSING_ICON);
				if(url==null)
					if(_DEBUG) System.out.println("DDIcons:getResourceURL: missing missing:"+Application.RESOURCES_ENTRY_POINT+Application.MISSING_ICON);
			}
			if(url!=null)
				loadedIcons.put(name, url);
		}
		return url;
	}
	public static URL getResourceURLOrNull(String name) {
		URL url = null;
		if((name==null) ||("null".equals(name))) return null;
		url = loadedIcons.get(name);
		if (url==null){
			if(DEBUG) System.out.println("DDIcons:getResourceURL: will get:"+Application.RESOURCES_ENTRY_POINT+name);
			url = DD.class.getResource(Application.RESOURCES_ENTRY_POINT+name);
			if(url == null) {
				return null;
			}
			if(url!=null)
				loadedIcons.put(name, url);
		}
		return url;
	}
}
