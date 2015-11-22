/*   Copyright (C) 2012
		Authors:
		 Shi Chen and
		 Marius Silaghi: msilaghi@fit.edu
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import net.ddp2p.common.config.Application_GUI;
public class DBAlter {
	public static final boolean SQLITE4JAVA_COPY = true;
	public static boolean SQLITE4JAVA = false;
	public static boolean DEBUG = false;
	public static void extractDDL(String database_file_name, String DDL_file_name) throws P2PDDSQLException{
		FileOutputStream fos;
		String opDDL = extractDDL(new File(database_file_name));
		try{
			fos = new FileOutputStream(DDL_file_name);
			fos.write(opDDL.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static String extractDDL(File database_file) throws P2PDDSQLException{
		return Util.concat(_extractDDL(database_file), "\n");
	}
	public static String[] _extractDDL(File database_file){
		return Application_GUI.extractDDL(database_file, SQLITE4JAVA?0:1);
	}
	public static boolean copyData(File database_old, File database_new, BufferedReader DDL, String[]_DDL) throws IOException, P2PDDSQLException{
		return copyData(database_old, database_new, DDL, _DDL, SQLITE4JAVA_COPY);
	}
	public static boolean copyData(File database_old, File database_new, BufferedReader DDL, String[]_DDL, boolean _SQLITE4JAVA) throws IOException, P2PDDSQLException{
		return Application_GUI.db_copyData(database_old, database_new, DDL, _DDL, _SQLITE4JAVA);
	}
	public static void copyData(String database_old, String database_new, String DDL_file_name, boolean sqlite4java) throws IOException, P2PDDSQLException{
		FileReader read = new FileReader(DDL_file_name);
		BufferedReader DDL = new BufferedReader(read);
		copyData(new File(database_old), new File(database_new), DDL, null, sqlite4java);
		DDL.close();
		read.close();
	}
}
