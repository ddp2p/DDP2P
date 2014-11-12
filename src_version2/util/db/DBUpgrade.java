package util.db;

import java.io.IOException;

import util.DBAlter;
import util.P2PDDSQLException;
import util.Util;

public
class DBUpgrade {

	public static void main(String args[]) {
		if (args.length < 3) {
			System.out.println("Usage: prog old_db new_db temporary_DDL_filename [exist_DDL]");
			System.out.println("Usage: 		- new_db must be initialized already: e.g., with install");
			System.out.println("Usage: 		- exist [1:exist_DDL , 0:create_DDL_only");
			System.out.println("Usage: 		- if exist not provided then temporary_DDL_file must not exist, and will contain the used DDL");
			System.out.println("Usage: 		- the created DDL starts each line with repeating twice a table name, followed by the attributes (from the old database)");
			System.out.println("Usage: 		- an input DDL starts each line with the old table name, then its new table name, followed by the attributes in the new database set to positional values in old table");
			System.out.println("Usage: 		- the DDL separator character is the space");
			return;
		}
		util.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
		String old_db = args[0];
		String new_db = args[1];
		String DDL = args[2];
		try {
			if (args.length > 3) {
				boolean exist = Util.stringInt2bool(args[3], false);
				if (exist) {
					DBAlter.copyData(old_db,new_db, DDL, DBAlter.SQLITE4JAVA_COPY);
				} else {
					DBAlter.extractDDL(old_db, DDL);
				}
				return;
			}
			DBAlter.extractDDL(old_db, DDL);
			DBAlter.copyData(old_db,new_db, DDL, DBAlter.SQLITE4JAVA_COPY);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
}