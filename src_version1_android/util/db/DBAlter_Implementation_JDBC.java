package util.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import table.SQLITE_MASTER;
import util.DBInterface;
import util.P2PDDSQLException;
import util.Util;

import config.DD;

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
	
	public static boolean _copyData(File database_old, File database_new, BufferedReader DDL, String[]_DDL) throws IOException, P2PDDSQLException{//
		DB_Implementation_JDBC_SQLite conn_src = null;
		DB_Implementation_JDBC_SQLite conn_dst = null;
		try{ 
			//DBInterface conn_src = new DBInterface(database_old);
			conn_src = new DB_Implementation_JDBC_SQLite();
			conn_src.open(database_old.getAbsolutePath());
			conn_src.open_and_keep(true);
			
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
			int crt_table = 0;
			String table_DDL = null;
			if(DDL!=null) table_DDL = DDL.readLine();
			else if((_DDL!=null) && (_DDL.length>0)) table_DDL = _DDL[crt_table];
			for(;;) {
				if(table_DDL==null) break;
				table_DDL = table_DDL.trim();
				if(table_DDL.length()==0) continue; // skip empty lines
				String []table__DDL = Util.trimmed(table_DDL.split(Pattern.quote(" ")));
				if(_DEBUG) System.out.println("DBAlter:_copyData: next table DDL= "+table__DDL[0]);
	    		st = conn_src.conn.prepareStatement("SELECT * FROM "+table__DDL[0]+";");
				//ArrayList<ArrayList<Object>> olddb_all = conn_src.select("SELECT * FROM ?;", new String[]{table__DDL[0]});

	    		ResultSet rs = st.executeQuery();
    			ResultSetMetaData md = rs.getMetaData();
    			int cols_src = md.getColumnCount(); 

    			while (rs.next()) {
    				if(_DEBUG) System.out.print(rs.getRow()+" ");
	    		//for(ArrayList<Object> olddb : olddb_all){
					int attributes_count_insert = table__DDL.length - 2;
					String[] values_old = new String[attributes_count_insert];
	
    				for(int j=1; j<=cols_src; j++){
					//for(int j=0; j<attributes_count_insert; j++){
						values_old[j-1] = rs.getString(j); // Util.getString(olddb.get(j));
					}
					String[] attr_new = Arrays.copyOfRange(table__DDL, 2, table__DDL.length);
					
					try{
						conn_dst._insert(table__DDL[1], attr_new, values_old, DEBUG);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				crt_table++;
				if(DDL != null) table_DDL = DDL.readLine();
				else if(_DDL.length>crt_table) table_DDL = _DDL[crt_table];
				else table_DDL = null;
				if(DEBUG) System.out.println("DBAlter:_copyData: will next table DDL= "+table_DDL);
			}
			if(_DEBUG) System.out.println("DBAlter:_copyData: done tables");
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
