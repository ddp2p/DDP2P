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
		if (Identity.isListing_directories_loaded()) {
			try {
				DD.load_listing_directories();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (Application.getG_TCPServer() != null) {
			D_Peer myself = HandlingMyself_Peer.get_myself_or_null();
			if (myself != null) {
				HandlingMyself_Peer.updateAddress(myself);
				UDPServer.announceMyselfToDirectories();
			}
		}
	}
}
