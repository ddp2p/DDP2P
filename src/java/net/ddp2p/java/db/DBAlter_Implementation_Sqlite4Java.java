package net.ddp2p.java.db;

import static net.ddp2p.common.util.Util.__;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.table.application;
import net.ddp2p.common.util.DBAlter;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

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
	/**
	 * Returns false on error.
	 * 
	 * The parameter passed as bufferReader has priority if not null
	 * 
	 * @param database_old
	 * @param database_new
	 * @param p_reader_DDL
	 * @param p_array_DDL (may be null)
	 * @return 
	 * @throws IOException
	 * @throws P2PDDSQLException
	 */
	public static boolean _copyData(File database_old, File database_new, BufferedReader p_reader_DDL, String[]p_array_DDL) throws IOException, P2PDDSQLException{//
		try { 
			/**
			 * Try to open old database. If not existing: fail!
			 */
			SQLiteConnection conn_src = new SQLiteConnection(database_old);
			try {
				conn_src.open(true);
				conn_src.exec("BEGIN IMMEDIATE");
			}catch(Exception e){
				e.printStackTrace();
				System.err.println("Trying:"+database_old);
				return false;
			}
			/**
			 * Tries to open new database, If not existing: fail!s
			 */
			SQLiteConnection conn_dst = new SQLiteConnection(database_new);
			try{
				conn_dst.open(true);
				conn_dst.exec("BEGIN IMMEDIATE");
			}catch(Exception e){
				e.printStackTrace();
				System.err.println("Trying:"+database_new);
				return false;
			}
			/**
			 * Parse the DDL from the received bufferReader or array of Strings.
			 * crt_table is the index of current table in DDL
			 */
			int crt_table = 0;
			/**
			 * The DDL line describing the current table (read either from the reader or from the array parameters)
			 */
			String crt_table_DDL = null;
			if (p_reader_DDL != null) crt_table_DDL = p_reader_DDL.readLine();
			else if ((p_array_DDL != null) && (p_array_DDL.length > 0)) crt_table_DDL = p_array_DDL[crt_table];
			
			for (;;) {
				/**
				 * Handling the table at index/line crt_table
				 */
				if( DBAlter.DEBUG) System.out.println("DBAlter:copyData: next table DDL= "+crt_table_DDL);
				
				if (crt_table_DDL == null) {
					if ( DBAlter.DEBUG) System.out.println("DBAlter:copyData: end of DDL at line = "+crt_table);
					break;
				}
				
				crt_table_DDL = crt_table_DDL.trim();
				
				if (crt_table_DDL.length() == 0) {
					continue; // skip empty lines
				}
				
				/**
				 * split the DDL line into fields
				 */
				String [] table__DDL = crt_table_DDL.split(Pattern.quote(" "));
				/**
				 * Select all attributes, in positional order, from the old table (in table__DDL[0]).
				 */
				SQLiteStatement olddb = conn_src.prepare("SELECT * FROM " + table__DDL[0]);
				
				while (olddb.step()) {
					/**
					 * In attributes_new we aggregate all the attribute names in DDL from position 2, on, concatenated with comma.
					 */
					String attributes_new = null;
	    			/**
	    			 * Number of attributes inserted from old. used to detect number of columns in old (elements in array of parameters to insert).
	    			 * Could be taken outside this loop, for efficiency. (was here since the count was computed from result of query)
	    			 */
					//int attributes_count_insert = olddb.columnCount();
					int attributes_count_insert = table__DDL.length - 2;
					String[] values_old = new String[attributes_count_insert];
					/**
					 * values_old_place is a list of placeholder "?" separated by comma, to be used in the insert statement.
					 * Use empty string instead of "?" for no list of attributes copied.
					 */
					String values_old_place = null;
					for (int line = 2; line < table__DDL.length; line ++) { // concat fields new DB
						if (attributes_new == null) attributes_new = table__DDL[line];
						else attributes_new = attributes_new+" , "+table__DDL[line];
					}
					/**
					 * Preparing "values_old_place", the a list of placeholder "?" separated by comma, to be used in the insert statement.
					 * Use empty string instead of "?" for no parameters.
					 * Also preparing the "values_old" array, to be passed as parameter to insert
					 */
					for (int j = 0; j < attributes_count_insert; j ++){
						values_old[j] = olddb.columnString(j);
						if (values_old_place == null) values_old_place= "?";
						else values_old_place += " , ? ";
					}
					if (values_old_place == null) values_old_place = "";
	
					//sql query "insert or replace" would insert if the row does not exist, or replace the values if it does. 
					//Otherwise, use "insert or ignore" to ignore the entity if it already exists or primary key conflict
					String sql = "insert or ignore into "+table__DDL[1]+ " ( "+attributes_new+" ) values ( "+values_old_place+" )";
					
					if (DBAlter.DEBUG) System.out.println("DBAlter:copyData:running: "+sql);
					/**
					 * Perform the actual insert
					 */
					SQLiteStatement newdb = conn_dst.prepare(sql);
					try {
						for (int k = 0; k < attributes_count_insert; k++){
							newdb.bind(k+1, values_old[k]);
							if (DBAlter.DEBUG) System.out.println("DBAlter:copyData:bind: "+Util.nullDiscrim(values_old[k]));
						}
						newdb.stepThrough();
	
						long result = conn_dst.getLastInsertId();
						if (DBAlter.DEBUG) System.out.println("DBAlter:copyData:result: "+result);
					} finally {newdb.dispose();}
	
					//newdb.stepThrough();
				}
				/**
				 * Read the next table's line in the DDL, and loop
				 */
				crt_table ++;
				if (p_reader_DDL != null) crt_table_DDL = p_reader_DDL.readLine();
				else if(p_array_DDL.length>crt_table) crt_table_DDL = p_array_DDL[crt_table];
				else crt_table_DDL = null;
			}
			/**
			 * Initialized default listing directories and updates server, and trusted updated GID,
			 * taking them from the old database
			 */
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

	static public DBInterface getSQLiteIntf(SQLiteConnection conn) {
		DB_Implementation_SQLite idb = new DB_Implementation_SQLite();
    	idb.keep_open(conn);
		DBInterface db = new DBInterface(idb);
		return db;
	}
	static public String getExactAppText(SQLiteConnection conn, String field) throws P2PDDSQLException{
		ArrayList<ArrayList<Object>> id;

		DBInterface db = getSQLiteIntf(conn);
		id=db._select("SELECT "+net.ddp2p.common.table.application.value +
				" FROM "+net.ddp2p.common.table.application.TNAME+" AS a " +
				" WHERE "+net.ddp2p.common.table.application.field+"=? LIMIT 1;",
				new String[]{field}, DBAlter.DEBUG);
		if(id.size()==0){
			if(DBAlter.DEBUG) System.err.println(__("No application record found for field: ")+field);
			return null;
		}
		String result = Util.getString(id.get(0).get(0));
		return result;
	}

	static public boolean setAppText(SQLiteConnection conn, String field, String value) throws P2PDDSQLException{
		//boolean DEBUG = true;
		if(DBAlter.DEBUG) System.err.println("DD:setAppText: field="+field+" new="+value);

		DBInterface db = getSQLiteIntf(conn);
		db._updateNoSync(net.ddp2p.common.table.application.TNAME, new String[]{net.ddp2p.common.table.application.value}, new String[]{net.ddp2p.common.table.application.field},
				new String[]{value, field}, DBAlter.DEBUG);
		if (value!=null){
			String old_val = getExactAppText(conn, field);
			if(DBAlter.DEBUG) System.err.println("DD:setAppText: field="+field+" old="+old_val);
			if (!value.equals(old_val)) {
				db._insertNoSync(
						net.ddp2p.common.table.application.TNAME,
						new String[]{net.ddp2p.common.table.application.field, net.ddp2p.common.table.application.value},
						new String[]{field, value},
						DBAlter.DEBUG);
				if(DBAlter.DEBUG)Application_GUI.warning(__("DBAlter: Added absent property: ")+field, __("Properties"));
			}
		}
		return true;
	}
	
}