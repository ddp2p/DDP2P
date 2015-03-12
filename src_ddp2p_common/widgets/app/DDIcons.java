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

package widgets.app;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import config.Application;
import config.DD;
import util.Util;

public class DDIcons {
	private static String I_ADD="add.png"; // action
	private static String I_IMP="ImportIcon.jpg"; // action
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
	
	private static String I_PEER10 = "network-node10.gif";	
	private static String I_PEERGRAY10 = "network-node_gray10.gif";	
	private static String I_ORG = "castle10.gif";	
	private static String I_MOTION10 = "laws10.gif";	
	private static String I_JUSTIFICATION10 = "justifications10.gif";	
	private static String I_SIGNATURE10 = "voting_booth20.gif";	
	private static String I_CONSTITUENT10 = "profile10.gif";	
	private static String I_STAT = "sad.smiley10.gif";	
	
	private static String I_GREEN_BALL = "green-sphere.gif";
	private static String I_GRAY_BALL = "dark-sphere.gif";
	private static String I_RED_BALL = "red-sphere.gif";		

	private static String I_BACK10 = "back10.gif";		
	private static String I_FORW10 = "forward10.gif";	
	
	private static final String I_REGISTRATION20 = "registration_icon20.gif";	
	private static final String I_MAILPOST20 = "mail20.gif";	
	private static final String I_IDENTITIES20 = "identities20.gif";	
	private static final String I_CENSUS20 = "census20.gif";	
	private static final String I_FIRE20 = "fire20.gif";	
	private static final String I_FIREG20 = "fire_grey_20.gif";	
	private static final String I_WIRELESS20 = "wireless20.gif";	
	private static final String I_DIRECTORY20 = "directory20.gif";	
	private static final String I_KEYS20 = "key_20.gif";	
	private static final String I_NEWS20 = "news20.gif";	
	private static final String I_RECONFIGURE20 = "reconfigure20.gif";	
	private static final String I_PEER20 = "network-node20.gif";	
	private static final String I_PEERGRAY20 = "network-node_gray20.gif";	
	private  static final String I_ORG20 = "castle20.gif";	
	private static final String I_MOTION20 = "laws20.gif";	
	private static final String I_JUSTIFICATION20 = "justifications20.gif";	
	private static final String I_BALANCE20 = "balance20.gif";	
	private static final String I_SIGNATURE20 = "voting_booth20.gif";	
	private static final String I_CONSTITUENT20 = "profile20.gif";	
	private static final String I_CREATOR20 = "creator20.jpg";	
	private static final String I_PROVIDER20 = "delivery20.jpg";//"delivery20.png"	
	private static final String I_LANDING20 = "landings20.jpg";	
	private static final String I_BACK20 = "back20.gif";		
	private static final String I_FORW20 = "forward20.gif";		

	public static final String I_BROADCAST20 = "broadcast20.gif";
	public static final String I_BLOCK_DOWNLOAD20 = "block20.gif";
	private static final String I_HIDE20 = "hide20.gif";
	private static final String I_TMP20 = "tmp20.gif";
	private static final String I_GID20 = "gid20.gif";
	private static final String I_SIGNED20 = "signed20.gif";
	private static final String I_VERIF20 = "verif20.gif";
	private static final String I_REVOKED20 = "revoked20.gif";
	private static final String I_CONNECTED20 = "connected20.gif";

	public  static final String I_DDP2P40 = "ddp2p40.gif";	
	public  static final String I_DDP2P32 = "ddp2p32.gif";	

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
	private static ImageIcon _II_IMP = null;
	public static ImageIcon getImpImageIcon(String descr){
		try{if(_II_IMP == null) _II_IMP= getImageIconFromResource(I_IMP, descr);}catch(Exception e){e.printStackTrace();}
		if(_II_IMP == null) Util.printCallPath("null");
		return _II_IMP;
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
	private static ImageIcon _II_SIGNED_ = null;
	public static ImageIcon getSignedImageIcon(String descr){
		if(_II_SIGNED_ == null) _II_SIGNED_= getImageIconFromResource(I_SIGNED20, descr);
		if(_II_SIGNED_ == null) Util.printCallPath("null");
		return _II_SIGNED_;
	}
	private static ImageIcon _II_VERIF_ = null;
	public static ImageIcon getVerifImageIcon(String descr){
		if(_II_VERIF_ == null) _II_VERIF_= getImageIconFromResource(I_VERIF20, descr);
		if(_II_VERIF_ == null) Util.printCallPath("null");
		return _II_VERIF_;
	}
	private static ImageIcon _II_REVOKED_ = null;
	public static ImageIcon getRevokedImageIcon(String descr){
		if(_II_REVOKED_ == null) _II_REVOKED_= getImageIconFromResource(I_REVOKED20, descr);
		if(_II_REVOKED_ == null) Util.printCallPath("null");
		return _II_REVOKED_;
	}
	private static ImageIcon _II_CONNECTED_ = null;
	public static ImageIcon getConnectedImageIcon(String descr){
		if(_II_CONNECTED_ == null) _II_CONNECTED_= getImageIconFromResource(I_CONNECTED20, descr);
		if(_II_CONNECTED_ == null) Util.printCallPath("null");
		return _II_CONNECTED_;
	}
	private static ImageIcon _II_GID_ = null;
	public static ImageIcon getGIDImageIcon(String descr){
		if(_II_GID_ == null) _II_GID_= getImageIconFromResource(I_GID20, descr);
		if(_II_GID_ == null) Util.printCallPath("null");
		return _II_GID_;
	}
	private static ImageIcon _II_TMP_ = null;
	public static ImageIcon getTmpImageIcon(String descr){
		if(_II_TMP_ == null) _II_TMP_= getImageIconFromResource(I_TMP20, descr);
		if(_II_TMP_ == null) Util.printCallPath("null");
		return _II_TMP_;
	}
	private static ImageIcon _II_HIDE_ = null;
	public static ImageIcon getHideImageIcon(String descr){
		if(_II_HIDE_ == null) _II_HIDE_= getImageIconFromResource(I_HIDE20, descr);
		if(_II_HIDE_ == null) Util.printCallPath("null");
		return _II_HIDE_;
	}
	private static ImageIcon _II_BLOCK_DOWNLOAD_ = null;
	public static ImageIcon getBlockImageIcon(String descr){
		if(_II_BLOCK_DOWNLOAD_ == null) _II_BLOCK_DOWNLOAD_= getImageIconFromResource(I_BLOCK_DOWNLOAD20, descr);
		if(_II_BLOCK_DOWNLOAD_ == null) Util.printCallPath("null");
		return _II_BLOCK_DOWNLOAD_;
	}
	private static ImageIcon _II_BROADCAST_ = null;
	public static ImageIcon getBroadcastImageIcon(String descr){
		if(_II_BROADCAST_ == null) _II_BROADCAST_= getImageIconFromResource(I_BROADCAST20, descr);
		if(_II_BROADCAST_ == null) Util.printCallPath("null");
		return _II_BROADCAST_;
	}
	private static ImageIcon _II_BACK_ = null;
	public static ImageIcon getBackImageIcon(String descr){
		if(_II_BACK_ == null) _II_BACK_= getImageIconFromResource(I_BACK20, descr);
		if(_II_BACK_ == null) Util.printCallPath("null");
		return _II_BACK_;
	}
	private static ImageIcon _II_FORW_ = null;
	public static ImageIcon getForwImageIcon(String descr){
		if(_II_FORW_ == null) _II_FORW_= getImageIconFromResource(I_FORW20, descr);
		if(_II_FORW_ == null) Util.printCallPath("null");
		return _II_FORW_;
	}
	private static ImageIcon _II_MAILPOST = null;
	public static ImageIcon getMailPostImageIcon(String descr){
		if(_II_MAILPOST == null) _II_MAILPOST = getImageIconFromResource(I_MAILPOST20, descr);
		if(_II_MAILPOST == null) Util.printCallPath("null");
		return _II_MAILPOST;
	}
	private static ImageIcon _II_REGISTRATION = null;
	public static ImageIcon getRegistrationImageIcon(String descr){
		if(_II_REGISTRATION == null) _II_REGISTRATION = getImageIconFromResource(I_REGISTRATION20, descr);
		if(_II_REGISTRATION == null) Util.printCallPath("null");
		return _II_REGISTRATION;
	}
	private static ImageIcon _II_IDENTITIES = null;
	public static ImageIcon getIdentitiesImageIcon(String descr){
		if(_II_IDENTITIES == null) _II_IDENTITIES = getImageIconFromResource(I_IDENTITIES20, descr);
		if(_II_IDENTITIES == null) Util.printCallPath("null");
		return _II_IDENTITIES;
	}
	private static ImageIcon _II_CENSUS = null;
	public static ImageIcon getCensusImageIcon(String descr){
		if(_II_CENSUS == null) _II_CENSUS = getImageIconFromResource(I_CENSUS20, descr);
		if(_II_CENSUS == null) Util.printCallPath("null");
		return _II_CENSUS;
	}
	private static ImageIcon _II_HOT= null;
	public static Icon getHotImageIcon(String descr) {
		if(_II_HOT == null) _II_HOT = getImageIconFromResource(I_FIRE20, descr);
		if(_II_HOT == null) Util.printCallPath("null");
		return _II_HOT;
	}
	private static ImageIcon _II_HOTG= null;
	public static Icon getHotGImageIcon(String descr) {
		if(_II_HOTG == null) _II_HOTG = getImageIconFromResource(I_FIREG20, descr);
		if(_II_HOTG == null) Util.printCallPath("null");
		return _II_HOTG;
	}
	private static ImageIcon _II_WIRELESS = null;
	public static ImageIcon getWirelessImageIcon(String descr){
		if(_II_WIRELESS == null) _II_WIRELESS = getImageIconFromResource(I_WIRELESS20, descr);
		if(_II_WIRELESS == null) Util.printCallPath("null");
		return _II_WIRELESS;
	}
	private static ImageIcon _II_DIR = null;
	public static ImageIcon getDirImageIcon(String descr){
		if(_II_DIR == null) _II_DIR = getImageIconFromResource(I_DIRECTORY20, descr);
		if(_II_DIR == null) Util.printCallPath("null");
		return _II_DIR;
	}
	private static ImageIcon _II_NEWS = null;
	public static ImageIcon getNewsImageIcon(String descr){
		if(_II_NEWS == null) _II_NEWS = getImageIconFromResource(I_NEWS20, descr);
		if(_II_NEWS == null) Util.printCallPath("null");
		return _II_NEWS;
	}
	private static ImageIcon _II_KEYS = null;
	public static ImageIcon getKeyImageIcon(String descr){
		if(_II_KEYS == null) _II_KEYS= getImageIconFromResource(I_KEYS20, descr);
		if(_II_KEYS == null) Util.printCallPath("null");
		return _II_KEYS;
	}
	private static ImageIcon _II_CONFIG = null;
	public static ImageIcon getConfigImageIcon(String descr){
		if(_II_CONFIG == null) _II_CONFIG= getImageIconFromResource(I_RECONFIGURE20, descr);
		if(_II_CONFIG == null) Util.printCallPath("null");
		return _II_CONFIG;
	}
	private static ImageIcon _II_BALANCE = null;
	public static ImageIcon getBalanceImageIcon(String descr){
		if(_II_BALANCE == null) _II_BALANCE= getImageIconFromResource(I_BALANCE20, descr);
		if(_II_BALANCE == null) Util.printCallPath("null");
		return _II_BALANCE;
	}
	private static ImageIcon _II_PEER_ME = null;
	public static ImageIcon getPeerMeImageIcon(String descr){
		if(_II_PEER_ME == null) _II_PEER_ME= getImageIconFromResource(I_PEER20, descr);
		if(_II_PEER_ME == null) Util.printCallPath("null");
		return _II_PEER_ME;
	}
	private static ImageIcon _II_PEER_SEL = null;
	public static ImageIcon getPeerSelImageIcon(String descr){
		if(_II_PEER_SEL == null) _II_PEER_SEL= getImageIconFromResource(I_PEERGRAY20, descr);
		if(_II_PEER_SEL == null) Util.printCallPath("null");
		return _II_PEER_SEL;
	}
	private static ImageIcon _II_ORG = null;
	public static ImageIcon getOrgImageIcon(String descr){
		if(_II_ORG == null) _II_ORG= getImageIconFromResource(I_ORG20, descr);
		if(_II_ORG == null) Util.printCallPath("null");
		return _II_ORG;
	}
	private static ImageIcon _II_MOT = null;
	public static ImageIcon getMotImageIcon(String descr){
		if(_II_MOT == null) _II_MOT= getImageIconFromResource(I_MOTION20, descr);
		if(_II_MOT == null) Util.printCallPath("null");
		return _II_MOT;
	}
	private static ImageIcon _II_JUS = null;
	public static ImageIcon getJusImageIcon(String descr){
		if(_II_JUS == null) _II_JUS= getImageIconFromResource(I_JUSTIFICATION20, descr);
		if(_II_JUS == null) Util.printCallPath("null");
		return _II_JUS;
	}
	public static Icon getAnsweredImageIcon(String descr) {
		return getJusImageIcon(descr);
	}
	public static Icon getAnsweringImageIcon(String descr) {
		return getBalanceImageIcon(descr);
	}
	private static ImageIcon _II_SIG = null;
	public static ImageIcon getSigImageIcon(String descr){
		if(_II_SIG == null) _II_SIG= getImageIconFromResource(I_SIGNATURE20, descr);
		if(_II_SIG == null) Util.printCallPath("null");
		return _II_SIG;
	}
	private static ImageIcon _II_CON = null;
	public static ImageIcon getConImageIcon(String descr){
		if(_II_CON == null) _II_CON= getImageIconFromResource(I_CONSTITUENT20, descr);
		if(_II_CON == null) Util.printCallPath("null");
		return _II_CON;
	}
	private static ImageIcon _II_CRE = null;
	public static ImageIcon getCreatorImageIcon(String descr){
		if(_II_CRE == null) _II_CRE= getImageIconFromResource(I_CREATOR20, descr);
		if(_II_CRE == null) Util.printCallPath("null");
		return _II_CRE;
	}
	private static ImageIcon _II_MAIL = null;
	public static ImageIcon getMailImageIcon(String descr){
		if(_II_MAIL == null) _II_MAIL= getImageIconFromResource(I_PROVIDER20, descr);
		if(_II_MAIL == null) Util.printCallPath("null");
		return _II_MAIL;
	}
	private static ImageIcon _II_ARRIV = null;
	public static ImageIcon getLandingImageIcon(String descr){
		if(_II_ARRIV == null) _II_ARRIV= getImageIconFromResource(I_LANDING20, descr);
		if(_II_ARRIV == null) Util.printCallPath("null");
		return _II_ARRIV;
	}
	private static ImageIcon _II_STAT = null;
	public static ImageIcon getStaImageIcon(String descr){
		if(_II_STAT == null) _II_STAT= getImageIconFromResource(I_STAT, descr);
		if(_II_STAT == null) Util.printCallPath("null");
		return _II_STAT;
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
		return getImageIconFromResource(splash, MainFrame.frame, null);
	}
	public static ImageIcon getImageIconFromResource(String file, String descr){
		return getImageIconFromResource(file, MainFrame.frame, descr);
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
