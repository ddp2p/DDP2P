/*   Copyright (C) 2012 Marius Silaghi
		Authors: Marius Silaghi: msilaghi@fit.edu
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
package net.ddp2p.common.util;
import static net.ddp2p.common.util.Util.__;
import java.io.File;
import java.io.IOException;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.updates.ClientUpdates;
import net.ddp2p.common.util.P2PDDSQLException;
public class DB_Import {
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public static boolean import_db(String previous, String db_new) throws P2PDDSQLException {
		if(DEBUG) System.out.println("DB_Import: import_db: will import db indicated by "+previous+" into "+db_new);
		File indicator_to_previous_db = new File(previous);
		if(!indicator_to_previous_db.exists() || !indicator_to_previous_db.isFile()){
			Application_GUI.warning(__("Nonexistent old database version indicator file:")+" "+previous, __("Abandon database import!"));
			return false;
		}
		String old_version;
		try{
			old_version = Util.loadFile(previous);
			if(DEBUG) System.out.println("DB_Import: import_db: old version "+old_version);
			if(old_version == null){
				if(DEBUG) System.out.println("DB_Import: import_db: old version null: exit");
				return false;
			}
			old_version=old_version.trim();
			if(old_version.length()==0){
				if(DEBUG) System.out.println("DB_Import: import_db: old version empty: exit");
				return false;
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		String db_to_import = ClientUpdates.getFileInstallRootPathWithSeparator()+old_version+Application.OS_PATH_SEPARATOR+Application.DEFAULT_DELIBERATION_FILE;
		if(DEBUG) System.out.println("DB_Import: import_db: old db "+db_to_import);
		String error_db = DD.testProperDB(db_to_import);
		if(error_db != null) {
			if(DEBUG) System.out.println("DB_Import: import_db: failing old db "+db_to_import+" with "+error_db);
			Application_GUI.warning(__("Fail to open old database:")+"\n\""+db_to_import+"\"\n"+error_db, __("Abandon database import!"));
			return false;
		}
		int ok = Application_GUI.ask(
				__("Succeeded to open old database:")+" \""+db_to_import+"\""+
				((DD.TESTED_VERSION!=null)?("\n"+__("containing schema version")+" \""+DD.TESTED_VERSION+"\".\n"):".\n")+
				__("To start import from this database select (YES).")+"\n"+
				__("For import from another database select (NO).")+"\n"+
				__("Select (Cancel) to exit or continue without import!"),
				__("Start Importing From This?"),
				Application_GUI.YES_NO_CANCEL_OPTION);
		switch(ok) {
		case 0: 
			break;
		case 1: 
			db_to_import = Application_GUI.queryDatabaseFile();
			if (db_to_import == null) break;
			int c = Application_GUI.ask(__("Do you want to exit?")+"\n"+
			__("If you continue without importing your eventual old data,\n later you will have to mix the databases!"),
			__("Want to exit?"),
			Application_GUI.YES_NO_OPTION);
			if(c==0) System.exit(1);
		case 2: 
			return true;
		}
		if(DEBUG) System.out.println("DB_Import: import_db: success old db "+db_to_import+" with "+DD.TESTED_VERSION);
		File _db_new = new File(db_new);
		File _db_to_import = new File(db_to_import);
		if(_db_new.equals(_db_to_import)) return false;
		String[] DDL = DBAlter._extractDDL(_db_to_import);
		if(_DEBUG) System.out.println("DB_Import: import_db: DDL=\n"+Util.concat(DDL, "\n"));
		try {
			return DBAlter.copyData(_db_to_import, _db_new, null, DDL);
		} catch (IOException e) {
			e.printStackTrace();
			Application_GUI.warning(__("Error importing: "+e.getLocalizedMessage()), __("Error importing"));
		}
		if(DEBUG) System.out.println("DB_Import: import_db: quit on error with false");		
		return false;
	}
}
