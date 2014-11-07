package util.tools;

import tools.MigrateMirrors;
import util.DBInterface;
import config.Application;

/**
 * With one database as parameter, it moves table updates to mirror.
 * 
 * With two parameters, the first (old database) is used just to check the version to be at least as old as 1.0.5
 * @author msilaghi
 *
 */
public class MigrateTableUpdates2Mirrors_1_0_5 {
	public static void main(String args[]){
		try {
			_main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void _main(String args[]) throws Exception{
		util.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		
//		System.out.println("Try: hash="+hash);
//		if(hash == null) return;
		if (args.length > 1) {
			String oldDB = args[0];
			String newDB = args[1];
			DBInterface dbSrc = new DBInterface(oldDB);
			DBInterface dbDes = new DBInterface(newDB);
			MigrateMirrors.migrateIfNeeded(args[0], args[1], dbSrc, dbDes);
		} else {
			String dbase = Application.DELIBERATION_FILE;
			if (args.length > 0) dbase = args[0];
			DBInterface db = new DBInterface(dbase);// this is just a default DB
			String db2path=null;
			Application.db = db;
			MigrateMirrors.migrate(db); 
		}
	}
	
}