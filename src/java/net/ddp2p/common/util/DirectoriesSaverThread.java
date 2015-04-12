package net.ddp2p.common.util;

import static net.ddp2p.common.util.Util.__;

import java.util.ArrayList;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.hds.UDPServer;

public
class DirectoriesSaverThread extends net.ddp2p.common.util.DDP2P_ServiceThread {
	public static boolean DEBUG = false;
	DD_DirectoryServer ds;
	ArrayList<DirectoryAddress> dirs;
	public DirectoriesSaverThread(DD_DirectoryServer ds) {
		super("DD_DirectoryServer SaverThread", false);
		this.ds = ds;
		dirs = ds.dirs;
	}
	public DirectoriesSaverThread(ArrayList<DirectoryAddress> _dirs){
		super("DD_DirectoryServer SaverThread", false);
		dirs = _dirs;
	}
	public DirectoriesSaverThread(DirectoryAddress _dir){
		super("DD_DirectoryServer SaverThread", false);
		dirs = new ArrayList<DirectoryAddress>();
		dirs.add(_dir);
	}
	public void _run() {
		try {
			__run();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void __run() throws P2PDDSQLException {
		DirectoryAddress[] old_ad = DirectoryAddress.getDirectoryAddresses();
    	for (DirectoryAddress d : dirs) {
    		int old_ID;
    		String new_dir = d.toString();
    		if (DEBUG) System.out.println("DirectoriesSaverThread: __run newdir="+new_dir+" old="+Util.concat(old_ad, "\",\""));
			if ((old_ID = Util.contains(old_ad, d)) < 0) {
	    		if (DEBUG) System.out.println("DirectoriesSaverThread: __run not contained old ="+old_ID);
				d.store();
			} else {
	    		d.directory_address_ID = old_ad[old_ID].directory_address_ID;
	    		if (DEBUG) System.out.println("DirectoriesSaverThread: __run contained old ="+old_ID);
				d.store();
	    		Application_GUI.warning(__("Already known directory:")+new_dir, __("Added directory"));
	    	}
    	}
		
		// If a list of directories was loaded, reload it!
		if (Identity.isListing_directories_loaded()) {
			try {
				DD.load_listing_directories();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (Application.g_TCPServer != null) {
			//HandlingMyself_Peer.updateAddress(HandlingMyself_Peer.get_myself_with_wait());
			D_Peer myself = HandlingMyself_Peer.get_myself_or_null();
			if (myself != null) {
				HandlingMyself_Peer.updateAddress(myself);
				UDPServer.announceMyselfToDirectories();
			}
		}
	}
	/*
	public void ___run() throws P2PDDSQLException {
    	String ld = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
    	if (ld != null) ld = ld.trim();
    	else ld = "";
    	String old_dirs[] = ld.split(Pattern.quote(DD.APP_LISTING_DIRECTORIES_SEP));
    	Address old_ad[] = new Address[old_dirs.length];
    	for (int k = 0; k < old_ad.length; k++) {
    		old_ad[k] = new Address(old_dirs[k]);
    	}
    	//String new_dir = domain+DD.APP_LISTING_DIRECTORIES_ELEM_SEP+port;
    	for (DirectoryAddress d : dirs) {
    		String new_dir = d.toString();
    		if (DEBUG) System.out.println("DirectoriesSaverThread: __run newdir="+new_dir+" old="+Util.concat(old_dirs, "\",\""));
			if (Util.contains(old_ad, d) < 0) {
	    		if (DEBUG) System.out.println("DirectoriesSaverThread: __run not contained");
	    		ld = new_dir+DD.APP_LISTING_DIRECTORIES_SEP+ld;
	    		Identity.listing_directories_string.add(new_dir);
	    		try {
	    			Identity.listing_directories_inet.add(new InetSocketAddress(InetAddress.getByName(d.domain),d.tcp__port));
	    		} catch(Exception e) {
	    			Application.warning(_("Error for "+d+"\nError: "+e.getMessage()), _("Error installing directories"));
	    		}
	    	} else {
	    		if (DEBUG) System.out.println("DirectoriesSaverThread: __run contained");
	    	}
    	}
    	DD.setAppText(DD.APP_LISTING_DIRECTORIES, ld);
//		try {
//			DD.load_listing_directories();
//		} catch (NumberFormatException | UnknownHostException e) {
//			e.printStackTrace();
//		}
		if(Application.as!=null) {
			
//			Identity peer_ID = new Identity();
//			peer_ID.globalID = Identity.current_peer_ID.globalID;
//			peer_ID.name = Identity.current_peer_ID.name;
//			peer_ID.slogan = Identity.current_peer_ID.slogan;
//			peer_ID.instance = Identity.current_peer_ID.instance;
//			Server.set_my_peer_ID_TCP(peer_ID);
			
			//MyselfHandling.set_my_peer_ID_TCP(MyselfHandling.get_myself());
			HandlingMyself_Peer.updateAddress(HandlingMyself_Peer.get_myself());
			HandlingMyself_Peer.announceMyselfToDirectories_UDP_or_TCP();
		}
	}
	*/
}