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
package util.tools;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB;

public class UnitSetImport {
	static public void main(String args[]) {
		System.out.println("UnitSetImport: Saved in application field values="+args.length);
		if (args.length == 0) return;
		
		try {
			
			Vendor_JDBC_EMAIL_DB.initJDBCEmail();
			
			String db_to_import = args[0];
			Application.setDB(new DBInterface(Application.DELIBERATION_FILE));
			DD.setAppText(DD.APP_DB_TO_IMPORT, db_to_import);
			System.out.println("UnitSetImport: Saved in application field="+DD.APP_DB_TO_IMPORT+" value="+args[0]);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}	
}