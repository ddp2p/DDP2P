package net.ddp2p.java.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import net.ddp2p.common.config.DD;
import net.ddp2p.common.table.SQLITE_MASTER;
import net.ddp2p.common.util.DBAlter;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

public class DBAlter_Implementation_JDBC{
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	
	private static final int SEL_MASTER_TBL_NAME = 0;

	public static String[] __extractDDL(File database_file){//
		ArrayList<String> array=new ArrayList<String>();
		ArrayList<String> result=new ArrayList<String>();
	
		try {
			DBInterface connection = new DBInterface(database_file);
			ArrayList<ArrayList<Object>> tables = //connection.select("SELECT * FROM SQLITE_MASTER", new String[]{});
					connection.select("SELECT "+SQLITE_MASTER.tbl_name+" FROM "+SQLITE_MASTER.TNAME+" WHERE "+SQLITE_MASTER.type+"=?;",
							new String[]{SQLITE_MASTER.TYPE_table});
			
			for(int k=0; k<tables.size(); k++) {
				ArrayList<Object> table = tables.get(k);
				//if(!TABLE.equals(Util.getString(table.get(COL_MASTER_TYPE)))) continue;
				//String id = Util.getString(table.get(COL_MASTER_TBL_NAME));
				String id = Util.getString(table.get(SEL_MASTER_TBL_NAME));
				array.add(id);
			}
	
			for(int j=0;j<array.size();j++){
				String tbName=array.get(j);
				String s=tbName+" "+tbName+" ";
				ArrayList<ArrayList<Object>> AllColumnName = connection.select("pragma table_info("+tbName+")",new String[]{});
				for(int k=0; k<AllColumnName.size(); k++){
					ArrayList<Object> cols = AllColumnName.get(k);
					String id = Util.getString(cols.get(1));
					s = s+id+" ";
				}
				result.add(s);
			}
		} catch (Exception e) {
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
	public static boolean _copyData(File database_old, File database_new, BufferedReader DDL, String[]_DDL) throws IOException, P2PDDSQLException{//
		DB_Implementation_JDBC_SQLite conn_src = null;
		DB_Implementation_JDBC_SQLite conn_dst = null;
		try { 
			//DBInterface conn_src = new DBInterface(database_old);
			/**
			 * Try to open old database. If not existing: fail!
			 */
			conn_src = new DB_Implementation_JDBC_SQLite();
			conn_src.open(database_old.getAbsolutePath());
			conn_src.open_and_keep(true);
			
			/**
			 * Tries to open new database, If not existing: fail!s
			 */
			DBInterface db_dst = new DBInterface(database_new);
			conn_dst = (DB_Implementation_JDBC_SQLite)db_dst.getImplementation();
			conn_dst.open_and_keep(true);
//			try{
//				conn_src._query("BEGIN IMMEDIATE", new String[]{}, DEBUG);
//			}catch(Exception e){
//				e.printStackTrace();
//				System.err.println("Trying:"+database_old);
//				conn_src.dispose_and_keep();
//				return false;
//			}
//			try{
//				conn_dst.select("BEGIN IMMEDIATE", new String[]{});
//			}catch(Exception e){
//				e.printStackTrace();
//				System.err.println("Trying:"+database_new);
//				return false;
//			}
	    	PreparedStatement st;
			/**
			 * Parse the DDL from the received bufferReader or array of Strings.
			 * crt_table is the index of current table in DDL
			 */
			int crt_table = 0;
			/**
			 * The DDL line describing the current table (read either from the reader or from the array parameters)
			 */
			String table_DDL = null;
			if (DDL != null) table_DDL = DDL.readLine();
			else if ((_DDL != null) && (_DDL.length > 0)) table_DDL = _DDL[crt_table];
			for (;;) {
				/**
				 * Handling the table at index/line crt_table
				 */
				if (table_DDL == null) {
					if ( DBAlter.DEBUG) System.out.println("DBAlter: copyData: end of DDL at line = "+crt_table);
					break;
				}
				table_DDL = table_DDL.trim();
				if (table_DDL.length() == 0) {
					continue; // skip empty lines
				}
				
				/**
				 * split the DDL line into fields
				 */
				String []table__DDL = Util.trimmed(table_DDL.split(Pattern.quote(" ")));
				
				if(_DEBUG) System.out.println("DBAlter:_copyData: next table DDL= "+table__DDL[0]);
				
				/**
				 * Select all attributes, in positional order, from the old table (in table__DDL[0]).
				 */
	    		st = conn_src.conn.prepareStatement("SELECT * FROM "+table__DDL[0]+";");
				//ArrayList<ArrayList<Object>> olddb_all = conn_src.select("SELECT * FROM ?;", new String[]{table__DDL[0]});

	    		ResultSet rs = st.executeQuery();
    			ResultSetMetaData md = rs.getMetaData();
    			int cols_src = md.getColumnCount(); 

    			/**
    			 * Number of attributes inserted from old. used to detect number of columns in old (elements in array of parameters to insert).
    			 * Could be taken inside next loop (was there since the count was computed from result of query)
    			 */
				//int attributes_count_insert = olddb.columnCount();
    			int attributes_count_insert = table__DDL.length - 2;
    			if (cols_src != attributes_count_insert) {
    				System.out.println("DB_Implement_JDBC: _copy: different nb of attributes in source and DDL: "+cols_src+" vs "+ attributes_count_insert);
    			}
    			/**
    			 * intialized. could go in loop to make sure fields are null when needed.
    			 * Using the number in DDL, since that is used in insert!
    			 */
    			String[] values_old = new String[attributes_count_insert];
    			
    			while (rs.next()) {
    				if(_DEBUG) System.out.print(rs.getRow()+" ");
	
					/**
					 * Also preparing the "values_old" array, to be passed as parameter to insert
					 */
    				for (int j = 1; j <= cols_src; j ++) {
    					if (j > values_old.length) {
    	    				System.out.println("DB_Implement_JDBC: _copy: different nb of attributes in source and DDL. Skip: "+j+" -> "+rs.getString(j));
    						continue;
    					}
    					//for(int j=0; j<attributes_count_insert; j++){
						values_old[j-1] = rs.getString(j); // Util.getString(olddb.get(j));
					}
					String[] attr_new = Arrays.copyOfRange(table__DDL, 2, table__DDL.length);
					
					/**
					 * Perform the actual insert
					 */
					try {
						conn_dst._insert(table__DDL[1], attr_new, values_old, DEBUG);
					} catch(Exception e){
						e.printStackTrace();
					}
				}
				/**
				 * Read the next table's line in the DDL, and loop
				 */
				crt_table ++;
				if (DDL != null) table_DDL = DDL.readLine();
				else if(_DDL.length>crt_table) table_DDL = _DDL[crt_table];
				else table_DDL = null;
				if (DEBUG) System.out.println("DBAlter:_copyData: will next table DDL= "+table_DDL);
			}
			if(_DEBUG) System.out.println("DBAlter:_copyData: done tables");
			/**
			 * Initialized default listing directories and updates server, and trusted updated GID,
			 * taking them from the old database
			 */
			DD.setAppText(db_dst, DD.APP_UPDATES_SERVERS,
					DD.getExactAppText(conn_src, DD.APP_UPDATES_SERVERS), DEBUG);
			DD.setAppText(db_dst, DD.TRUSTED_UPDATES_GID,
					DD.getExactAppText(conn_src, DD.TRUSTED_UPDATES_GID), DEBUG);
			DD.setAppText(db_dst, DD.APP_LISTING_DIRECTORIES,
					DD.getExactAppText(conn_src, DD.APP_LISTING_DIRECTORIES), DEBUG);
			try{
				conn_src._query("COMMIT", new String[]{}, DEBUG);//exec("COMMIT");
			}catch(Exception e){}
			//conn_dst.exec("COMMIT");
			conn_src.dispose_and_keep();//.dispose();
			conn_dst.dispose_and_keep();
			//conn_dst.dispose();
		}catch(Exception e){
			if(conn_src!=null) conn_src.dispose_and_keep();
			if(conn_dst!=null) conn_dst.dispose_and_keep();
			e.printStackTrace();
		};
		return true;
	}	
}
