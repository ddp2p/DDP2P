package util.tools;

import hds.DirectoryServer;
import config.Application;

public class AddrBook {
	public static void main(String[] args) {
		try {
			util.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
			if (args.length > 0) Application.DIRECTORY_FILE = args[0];
			try {
				if (args.length > 1) DirectoryServer.PORT = Integer.parseInt(args[1]);
			} catch(Exception e) {e.printStackTrace();}
			DirectoryServer ds = new DirectoryServer(DirectoryServer.PORT);
			ds.start();
		}catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}		
	}

}
