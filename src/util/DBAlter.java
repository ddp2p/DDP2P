/* ------------------------------------------------------------------------- */
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
/* ------------------------------------------------------------------------- */
package util;
import static util.Util._;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import com.almworks.sqlite4java.*;

import config.Application;
import config.DD;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class DBAlter {
	
	public static boolean DEBUG = false;
	
	public static void extractDDL(String database_file_name, String DDL_file_name) throws SQLiteException{//
		FileOutputStream fos;
		String opDDL = extractDDL(new File(database_file_name));
		try{
			fos = new FileOutputStream(DDL_file_name);
			fos.write(opDDL.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String extractDDL(File database_file) throws SQLiteException{//
		return Util.concat(_extractDDL(database_file), "\n");
	}
	public static String[] _extractDDL(File database_file) throws SQLiteException{//
		ArrayList<String> array=new ArrayList<String>();
		ArrayList<String> result=new ArrayList<String>();
		
		SQLiteConnection connection = new SQLiteConnection(database_file);
		connection.open(true);
		connection.exec("BEGIN IMMEDIATE");
		SQLiteStatement loadAllRecordSt = connection.prepare("SELECT * FROM SQLITE_MASTER");

		// String opDDL="";
		while(loadAllRecordSt.step()){
			int columnCount = loadAllRecordSt.columnCount();
			for(int i=0; i<columnCount; i++){
				String id = loadAllRecordSt.columnString(i);
				if(i==2&&loadAllRecordSt.columnString(0).equals("table"))
					array.add(id);
			}
		}

		for(int j=0;j< array.size();j++){
			String tbName=array.get(j);
			String s=tbName+" "+tbName+" ";
			SQLiteStatement AllColumnName = connection.prepare("pragma table_info("+tbName+")");
			while(AllColumnName.step()){
				int columnCount = AllColumnName.columnCount();
				for(int i=0; i<columnCount; i++){
					String id = AllColumnName.columnString(i);
					if(i==1)
						s=s+id+" ";
				}
			}
			// opDDL=opDDL+s+"\r\n";
			result.add(s);
		}
		connection.exec("COMMIT");
		connection.dispose();
		return result.toArray(new String[0]);
	}
	
	public static void copyData(String database_old, String database_new, String DDL_file_name) throws IOException, SQLiteException{//
		FileReader read = new FileReader(DDL_file_name);
		BufferedReader DDL = new BufferedReader(read);
		copyData(new File(database_old), new File(database_new), DDL, null);
		DDL.close();
		read.close();
	}
	public static boolean copyData(File database_old, File database_new, BufferedReader DDL, String[]_DDL) throws IOException, SQLiteException{//
		SQLiteConnection conn_src = new SQLiteConnection(database_old);
		try {
			conn_src.open(true);
			conn_src.exec("BEGIN IMMEDIATE");
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("Trying:"+database_old);
			return false;
		}
		SQLiteConnection conn_dst = new SQLiteConnection(database_new);
		try{
			conn_dst.open(true);
			conn_dst.exec("BEGIN IMMEDIATE");
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("Trying:"+database_new);
			return false;
		}
		int crt_table = 0;
		String table_DDL = null;
		if(DDL!=null) table_DDL = DDL.readLine();
		else if((_DDL!=null) && (_DDL.length>0)) table_DDL = _DDL[crt_table];
		for(;;) {
    	    if(DEBUG) System.out.println("DBAlter:copyData: next table DDL= "+table_DDL);
			if(table_DDL==null) break;
			table_DDL = table_DDL.trim(); if(table_DDL.length()==0) continue; // skip empty lines
			String []table__DDL = table_DDL.split(Pattern.quote(" "));
			SQLiteStatement olddb = conn_src.prepare("SELECT * FROM "+table__DDL[0]);
			while(olddb.step()){
				String attributes_new=null;
				//int attributes_count_insert = olddb.columnCount();
				int attributes_count_insert = table__DDL.length - 2;
				String[] values_old = new String[attributes_count_insert];
				String values_old_place = null;
				for(int l=2;l<table__DDL.length;l++) { // concat fields new DB
					if(attributes_new==null) attributes_new=table__DDL[l];
					else attributes_new=attributes_new+" , "+table__DDL[l];
				}
				
				for(int j=0; j<attributes_count_insert; j++){
					values_old[j] = olddb.columnString(j);
					if(values_old_place==null) values_old_place= "?";
					else values_old_place += " , ? ";
				}
				if(values_old_place==null) values_old_place = "";

				//sql query "insert or replace" would insert if the row does not exist, or replace the values if it does. 
				//Otherwise, use "insert or ignore" to ignore the entity if it already exists or primary key conflict
				String sql = "insert or ignore into "+table__DDL[1]+ " ( "+attributes_new+" ) values ( "+values_old_place+" )";
	    	    if(DEBUG) System.out.println("DBAlter:copyData:running: "+sql);
				SQLiteStatement newdb = conn_dst.prepare(sql);
		    	try {
		    	    for(int k=0; k<attributes_count_insert; k++){
		    	    	newdb.bind(k+1, values_old[k]);
		    	    	if(DEBUG) System.out.println("DBAlter:copyData:bind: "+Util.nullDiscrim(values_old[k]));
		    	    }
		    	    newdb.stepThrough();
		    	    
		    	    long result = conn_dst.getLastInsertId();
		    	    if(DEBUG) System.out.println("DBAlter:copyData:result: "+result);
		    	} finally {newdb.dispose();}

				//newdb.stepThrough();
			}
			crt_table++;
			if(DDL != null) table_DDL = DDL.readLine();
			else if(_DDL.length>crt_table) table_DDL = _DDL[crt_table];
			else table_DDL = null;
		}
		setAppText(conn_dst, DD.APP_UPDATES_SERVERS,
				getExactAppText(conn_src, DD.APP_UPDATES_SERVERS));
		setAppText(conn_dst, DD.TRUSTED_UPDATES_GID,
				getExactAppText(conn_src, DD.TRUSTED_UPDATES_GID));
		setAppText(conn_dst, DD.APP_LISTING_DIRECTORIES,
				getExactAppText(conn_src, DD.APP_LISTING_DIRECTORIES));
		conn_src.exec("COMMIT");
		conn_dst.exec("COMMIT");
		conn_src.dispose();
		conn_dst.dispose();
		return true;
	}
	static public String getExactAppText(SQLiteConnection conn, String field) throws SQLiteException{
    	ArrayList<ArrayList<Object>> id;
    	DBInterface db = new DBInterface(conn);
    	id=db._select("SELECT "+table.application.value +
    			" FROM "+table.application.TNAME+" AS a " +
    			" WHERE "+table.application.field+"=? LIMIT 1;",
    			new String[]{field}, DEBUG);
    	if(id.size()==0){
    		if(DEBUG) System.err.println(_("No application record found for field: ")+field);
    		return null;
    	}
    	String result = Util.getString(id.get(0).get(0));
   		return result;
	}
	static public boolean setAppText(SQLiteConnection conn, String field, String value) throws SQLiteException{
		//boolean DEBUG = true;
		if(DEBUG) System.err.println("DD:setAppText: field="+field+" new="+value);
    	DBInterface db = new DBInterface(conn);
    	db._updateNoSync(table.application.TNAME, new String[]{table.application.value}, new String[]{table.application.field},
    			new String[]{value, field}, DEBUG);
    	if (value!=null){
    		String old_val = getExactAppText(conn, field);
    		if(DEBUG) System.err.println("DD:setAppText: field="+field+" old="+old_val);
    		if (!value.equals(old_val)) {
    			db._insertNoSync(
    					table.application.TNAME,
    					new String[]{table.application.field, table.application.value},
    					new String[]{field, value},
    					DEBUG);
    			if(DEBUG)Application.warning(_("DBAlter: Added absent property: ")+field, _("Properties"));
    		}
    	}
		return true;
	}
	public static void main(String args[]) {
		if(args.length<3){
			System.out.println("Usage: prog old_db new_db temporary_DDL_filename [exist_DDL]");
			System.out.println("Usage: 		- new_db must be initialized already: e.g., with install");
			System.out.println("Usage: 		- exist [0:exist_DDL , 1:create_DDL_only");
			System.out.println("Usage: 		- if exist not provided then temporary_DDL_file must not exist, and will contain the used DDL");
			return;
		}
		String old_db = args[0];
		String new_db = args[1];
		String DDL = args[2];
		try {
			if(args.length>3){
				boolean exist = Util.stringInt2bool(args[3], false);
				if(exist){
					copyData(old_db,new_db, DDL);
				}else{
					extractDDL(old_db, DDL);
				}
				return;
			}
			extractDDL(old_db, DDL);
			copyData(old_db,new_db, DDL);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}