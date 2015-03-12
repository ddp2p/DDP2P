package util;

import static util.Util._;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import table.application;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

import config.Application;
import config.DD;

public class DBAlter_Implementation_Sqlite4Java {

	public static String[] __extractDDL(File database_file){//
		ArrayList<String> array=new ArrayList<String>();
		ArrayList<String> result=new ArrayList<String>();
	
		try {
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
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		return result.toArray(new String[0]);
	}

	public static boolean _copyData(File database_old, File database_new, BufferedReader DDL, String[]_DDL) throws IOException, P2PDDSQLException{//
		try{ 
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
				if(DBAlter.DEBUG) System.out.println("DBAlter:copyData: next table DDL= "+table_DDL);
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
					if(DBAlter.DEBUG) System.out.println("DBAlter:copyData:running: "+sql);
					SQLiteStatement newdb = conn_dst.prepare(sql);
					try {
						for(int k=0; k<attributes_count_insert; k++){
							newdb.bind(k+1, values_old[k]);
							if(DBAlter.DEBUG) System.out.println("DBAlter:copyData:bind: "+Util.nullDiscrim(values_old[k]));
						}
						newdb.stepThrough();
	
						long result = conn_dst.getLastInsertId();
						if(DBAlter.DEBUG) System.out.println("DBAlter:copyData:result: "+result);
					} finally {newdb.dispose();}
	
					//newdb.stepThrough();
				}
				crt_table++;
				if(DDL != null) table_DDL = DDL.readLine();
				else if(_DDL.length>crt_table) table_DDL = _DDL[crt_table];
				else table_DDL = null;
			}
			DBAlter_Implementation_Sqlite4Java.setAppText(conn_dst, DD.APP_UPDATES_SERVERS,
					DBAlter_Implementation_Sqlite4Java.getExactAppText(conn_src, DD.APP_UPDATES_SERVERS));
			DBAlter_Implementation_Sqlite4Java.setAppText(conn_dst, DD.TRUSTED_UPDATES_GID,
					DBAlter_Implementation_Sqlite4Java.getExactAppText(conn_src, DD.TRUSTED_UPDATES_GID));
			DBAlter_Implementation_Sqlite4Java.setAppText(conn_dst, DD.APP_LISTING_DIRECTORIES,
					DBAlter_Implementation_Sqlite4Java.getExactAppText(conn_src, DD.APP_LISTING_DIRECTORIES));
			conn_src.exec("COMMIT");
			conn_dst.exec("COMMIT");
			conn_src.dispose();
			conn_dst.dispose();
		}catch(SQLiteException e){e.printStackTrace();};
		return true;
	}

	static public String getExactAppText(SQLiteConnection conn, String field) throws P2PDDSQLException{
		ArrayList<ArrayList<Object>> id;
		DBInterface db = new DBInterface(conn);
		id=db._select("SELECT "+table.application.value +
				" FROM "+table.application.TNAME+" AS a " +
				" WHERE "+table.application.field+"=? LIMIT 1;",
				new String[]{field}, DBAlter.DEBUG);
		if(id.size()==0){
			if(DBAlter.DEBUG) System.err.println(_("No application record found for field: ")+field);
			return null;
		}
		String result = Util.getString(id.get(0).get(0));
		return result;
	}

	static public boolean setAppText(SQLiteConnection conn, String field, String value) throws P2PDDSQLException{
		//boolean DEBUG = true;
		if(DBAlter.DEBUG) System.err.println("DD:setAppText: field="+field+" new="+value);
		DBInterface db = new DBInterface(conn);
		db._updateNoSync(table.application.TNAME, new String[]{table.application.value}, new String[]{table.application.field},
				new String[]{value, field}, DBAlter.DEBUG);
		if (value!=null){
			String old_val = getExactAppText(conn, field);
			if(DBAlter.DEBUG) System.err.println("DD:setAppText: field="+field+" old="+old_val);
			if (!value.equals(old_val)) {
				db._insertNoSync(
						table.application.TNAME,
						new String[]{table.application.field, table.application.value},
						new String[]{field, value},
						DBAlter.DEBUG);
				if(DBAlter.DEBUG)Application.warning(_("DBAlter: Added absent property: ")+field, _("Properties"));
			}
		}
		return true;
	}
	
}