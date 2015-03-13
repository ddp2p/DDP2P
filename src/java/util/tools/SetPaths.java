package util.tools;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.hds.StartUp;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB;

public class SetPaths {

	static public void main(String args[]) {
		System.out.println("Saved in application field values="+args.length);
		if (args.length == 0) return;
		try {
			
			Vendor_JDBC_EMAIL_DB.initJDBCEmail();
			
			String linux_path = args[0];
			if(!linux_path.endsWith(Application.LINUX_PATH_SEPARATOR)) linux_path += Application.LINUX_PATH_SEPARATOR;
			String _linux_parent=Util.getParent(linux_path);
			if((_linux_parent!=null) && !_linux_parent.endsWith(Application.LINUX_PATH_SEPARATOR)) _linux_parent += Application.LINUX_PATH_SEPARATOR;
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			DD.setAppText(DD.APP_LINUX_INSTALLATION_PATH, linux_path);
			DD.setAppText(DD.APP_LINUX_INSTALLATION_ROOT_PATH, _linux_parent);
			System.out.println("Saved in application field="+DD.APP_LINUX_INSTALLATION_PATH+" value=\""+args[0]+"\"\nparent: \""+_linux_parent+"\"");
			if(args.length==1) return;
			String win_path = args[1];
			if(!win_path.endsWith(Application.WINDOWS_PATH_SEPARATOR)){
				System.out.println("Windows path without terminator="+win_path);
				win_path += Application.WINDOWS_PATH_SEPARATOR;
				System.out.println("Windows path with terminator="+win_path);
			}
			System.out.println("Windows path with terminator="+win_path);
			String _win_parent=Util.getParent(win_path);
			if((_win_parent!=null) && !_win_parent.endsWith(Application.WINDOWS_PATH_SEPARATOR)) _win_parent += Application.WINDOWS_PATH_SEPARATOR;
			DD.setAppText(DD.APP_WINDOWS_INSTALLATION_PATH, win_path);
			DD.setAppText(DD.APP_WINDOWS_INSTALLATION_ROOT_PATH, _win_parent);
			System.out.println("Saved in application field="+DD.APP_WINDOWS_INSTALLATION_PATH+" value=\""+args[1]+"\"\nparent: \""+_win_parent+"\"");
	
			StartUp.detect_OS_and_store_in_DD_OS_var();
			StartUp.fill_install_paths_all_OSs_from_DB(); // to be done before importing!!!
			StartUp.switch_install_paths_to_ones_for_current_OS();		
		
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}

}
