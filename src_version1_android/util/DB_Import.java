/* ------------------------------------------------------------------------- */
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
/* ------------------------------------------------------------------------- */
package util;

import static util.Util._;

import java.io.File;
import java.io.IOException;

import updates.ClientUpdates;
import util.P2PDDSQLException;
import config.Application;
import config.Application_GUI;
import config.DD;


public class DB_Import {
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public static boolean import_db(String previous, String db_new) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("DB_Import: import_db: will import db indicated by "+previous+" into "+db_new);
		File indicator_to_previous_db = new File(previous);
		if(!indicator_to_previous_db.exists() || !indicator_to_previous_db.isFile()){
			Application_GUI.warning(_("Nonexistent old database version indicator file:")+" "+previous, _("Abandon database import!"));
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
		// may eventually (if implemented) use as path: CURRENT_DATABASE_DIR()
		String db_to_import = ClientUpdates.getFileInstallRootPathWithSeparator()+old_version+Application.OS_PATH_SEPARATOR+Application.DEFAULT_DELIBERATION_FILE;
		if(DEBUG) System.out.println("DB_Import: import_db: old db "+db_to_import);
		String error_db = DD.testProperDB(db_to_import);
		if(error_db != null) {
			if(DEBUG) System.out.println("DB_Import: import_db: failing old db "+db_to_import+" with "+error_db);
			Application_GUI.warning(_("Fail to open old database:")+"\n\""+db_to_import+"\"\n"+error_db, _("Abandon database import!"));
			return false;
		}
		int ok = Application_GUI.ask(
				_("Succeeded to open old database:")+" \""+db_to_import+"\""+
				((DD.TESTED_VERSION!=null)?("\n"+_("containing schema version")+" \""+DD.TESTED_VERSION+"\".\n"):".\n")+
				_("To start import from this database select (YES).")+"\n"+
				_("For import from another database select (NO).")+"\n"+
				_("Select (Cancel) to exit or continue without import!"),
				_("Start Importing From This?"),
				Application_GUI.YES_NO_CANCEL_OPTION);
		switch(ok) {
		case 0: //yes
			break;
		case 1: //no
			db_to_import = Application_GUI.queryDatabaseFile();
			if (db_to_import == null) break;
			int c = Application_GUI.ask(_("Do you want to exit?")+"\n"+
			_("If you continue without importing your eventual old data,\n later you will have to mix the databases!"),
			_("Want to exit?"),
			Application_GUI.YES_NO_OPTION);
			if(c==0) System.exit(1);
		case 2: //no
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
			Application_GUI.warning(_("Error importing: "+e.getLocalizedMessage()), _("Error importing"));
		}
		if(DEBUG) System.out.println("DB_Import: import_db: quit on error with false");		
		return false;
	}
	
	/*
	@Deprecated
	public boolean try_scripts(String db_to_import, String db_new){
		boolean DEBUG = false;
		if((DD.OS == DD.LINUX)||(DD.OS == DD.MAC)||(DD.OS == DD.WINDOWS)) {
			if(DEBUG) System.out.println("DB_Import: import_db: supported OS");
			Application.warning(("Supported upgrade OS:")+DD.OS, _("Start database import!"));
			File dir = null;
			dir = new File(Application.CURRENT_INSTALLATION_VERSION_BASE_DIR());
			if(!dir.exists() || !dir.isDirectory()) {
				if(DEBUG) System.out.println("DB_Import: import_db: cannot locate glob version base "+Application.CURRENT_INSTALLATION_VERSION_BASE_DIR());
				Application.warning(_("Cannot locate instalation root directory:"+"\n"+Application.CURRENT_INSTALLATION_VERSION_BASE_DIR()), _("May Abandon database import!"));
				//return false;

				if((DD.OS == DD.LINUX)||(DD.OS == DD.MAC)) {
					//Application.LINUX_INSTALLATION_DIR = DD.getAppText(DD.APP_LINUX_INSTALLATION_PATH);
					dir = new File(Application.LINUX_INSTALLATION_VERSION_BASE_DIR);
					if(!dir.exists() || !dir.isDirectory()) {
						if(DEBUG) System.out.println("DB_Import: import_db: cannot locate version base "+Application.LINUX_INSTALLATION_VERSION_BASE_DIR);
						Application.warning(_("Cannot locate instalation root directory:"+"\n"+Application.LINUX_INSTALLATION_VERSION_BASE_DIR), _("Abandon database import!"));
						return false;
					}
				}else if((DD.OS == DD.WINDOWS)) {
					dir = new File(Application.WINDOWS_INSTALLATION_VERSION_BASE_DIR);
					if(!dir.exists() || !dir.isDirectory()) {
						if(DEBUG) System.out.println("DB_Import: import_db: cannot locate version base "+Application.WINDOWS_INSTALLATION_VERSION_BASE_DIR);
						Application.warning(_("Cannot locate instalation root directory:"+"\n"+Application.WINDOWS_INSTALLATION_VERSION_BASE_DIR), _("Abandon database import!"));
						return false;
					}				
				}
			}

			String script_upgrade;
			if(DD.OS == DD.LINUX){
				script_upgrade = Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_LINUX_UPGRADE_DB;
				//script_upgrade = Application.SCRIPTS_RELATIVE_PATH+Application.OS_PATH_SEPARATOR+Application.SCRIPT_LINUX_UPGRADE_DB;
			}else if(DD.OS == DD.MAC){ //DD.MAC
				script_upgrade = Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_MACOS_UPGRADE_DB;
			}else if(DD.OS == DD.WINDOWS){
				script_upgrade = Application.CURRENT_SCRIPTS_BASE_DIR()+Application.SCRIPT_WINOS_UPGRADE_DB;
			} else{
				if(DEBUG) System.out.println("DB_Import: import_db: cannot find upgrade script for this OS");
				Application.warning(_("Unknown OS:"+DD.OS), _("Abandon database import!"));
				return false;
			}
			
			if(DEBUG) System.out.println("DB_Import: import_db: try run script upgrade: "+ script_upgrade+" on "+
					db_to_import+" into "+db_new+" in dir "+dir);
			File script = new File(script_upgrade);
			if(!script.exists() || !script.canExecute()) {
				if(DEBUG) System.out.println("DB_Import: import_db: cannot find run script upgrade: "+ script_upgrade);
				Application.warning(_("Cannot execute:"+script_upgrade), _("Abandon database import!"));
				return false;
			}
			
			String[] update = new String[]{script_upgrade, db_to_import, db_new};
			try {
				//Application.db.close();
				if(DEBUG) System.out.println("DB_Import: import_db: launch script");
				BufferedReader output = Util.getProcessOutput(update, null, dir);
				if(DEBUG) System.out.println("DB_Import: import_db: done script");
				String outp = Util.readAll(output);
				String lines[] = outp.split(Pattern.quote("\n"));
				System.out.println("\n\nClientUpdates:downloadNewer: updating script:\n"+outp+"\n\n");
				Application.warning(_("Output:")+" \n"+Util.concat(Util.selectRange(lines, lines.length-20, new String[20]), "\n", "null"), _("Updates Process Output"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(DEBUG) System.out.println("DB_Import: import_db: claiming success");
		return true;
	}
	*/
}
