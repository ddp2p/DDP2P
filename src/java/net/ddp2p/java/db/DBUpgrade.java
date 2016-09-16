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
package net.ddp2p.java.db;

import java.io.IOException;

import net.ddp2p.common.util.DBAlter;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

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
		net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
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