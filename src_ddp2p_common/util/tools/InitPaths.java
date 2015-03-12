package util.tools;

import hds.StartUp;
import util.DBInterface;
import util.P2PDDSQLException;
import config.Application;

public class InitPaths {
	static public void main(String args[]) {
		util.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		System.out.println("Saved in application field values="+args.length);
		if (args.length == 0) return;
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
			tools.Directories.setLinuxPaths(args[0]);
			
			if(args.length > 1)
				tools.Directories.setWindowsPaths(args[1]);
			
			StartUp.detect_OS_and_store_in_DD_OS_var();
			StartUp.fill_install_paths_all_OSs_from_DB(); // to be done before importing!!!
			StartUp.switch_install_paths_to_ones_for_current_OS();		
		
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	
}