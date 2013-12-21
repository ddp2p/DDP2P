package util;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import config.Application;

class DB_Implementation_JDBC_SQLite implements DB_Implementation {
    private static final boolean DEBUG = false;
	//SQLiteConnection db;
    File file;
	private boolean conn_open = true;
	Connection conn;
	private String filename;
	
	static boolean loaded = loadClass();
	static boolean loadClass(){
		try {
			Object c = Class.forName("org.sqlite.JDBC");
			if(c!=null) return true;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}
	
    public synchronized ArrayList<ArrayList<Object>> select(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	//try{throw new Exception("?");}catch(Exception e){e.printStackTrace();}
    	//conn_open = true;
    	//db = new SQLiteConnection(file);
    	try {
			tmp_open(true);
		} catch (Exception e) {
			//e.printStackTrace();
			throw new P2PDDSQLException(e);
		}
    	ArrayList<ArrayList<Object>> result = _select(sql, params, DEBUG);
    	try {
			tmp_dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
    	//conn_open = false;
    	return result;
    }
    /**
     * For already existing connections in db
     * @param sql
     * @param params
     * @param DEBUG
     * @return
     * @throws com.almworks.sqlite4java.SQLiteException
     */
    public synchronized void _query(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	PreparedStatement st;
    	try {
    		st = conn.prepareStatement(sql);
    		if(DEBUG) System.out.println("sqlite_jdbc:query:sql: "+sql+" length="+params.length);
    		try {
    			for(int k=0; k<params.length; k++){
    				st.setString(k+1, params[k]);//.bind(k+1, params[k]);
    				if(DEBUG) System.out.println("sqlite_jdbc:query:bind: "+params[k]);
    			}
    			//ResultSet rs = st.executeQuery();
    		} finally {st.close();}
    	} catch (SQLException e) {
    		e.printStackTrace();
    		throw new P2PDDSQLException(e);
    	}
    	if(DEBUG) System.out.println("DBInterface_jdbc:query:results#=ok");
    	return;
    }
    public synchronized ArrayList<ArrayList<Object>> _select(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	//if(!conn_open) throw new RuntimeException("Assumption failed!");
    	ArrayList<ArrayList<Object>> result = new ArrayList<ArrayList<Object>>();
    	PreparedStatement st;
    	try {
    		st = conn.prepareStatement(sql);
    		if(DEBUG) System.out.println("sqlite:select:sql: "+sql+" length="+params.length);
    		try {
    			for(int k=0; k<params.length; k++){
    				st.setString(k+1, params[k]);//.bind(k+1, params[k]);
    				if(DEBUG) System.out.println("sqlite:select:bind: "+params[k]);
    			}
    			ResultSet rs = st.executeQuery();
    			ResultSetMetaData md = rs.getMetaData();
    			int cols = md.getColumnCount(); 
    			//if(DEBUG) System.out.println("F: populateIDs will step");
    			while (rs.next()) {
    				//if(DEBUG) System.out.println("F: populateIDs step");
    				ArrayList<Object> cresult = new ArrayList<Object>();
    				for(int j=1; j<=cols; j++){
    					//if(DEBUG) System.out.println("F: populateIDs col:"+j);
    					cresult.add(rs.getString(j));
    				}
    				result.add(cresult);
    				if (result.size()>DBInterface.MAX_SELECT) {
    					JOptionPane.showMessageDialog(null,"Found more results than: "+DBInterface.MAX_SELECT);
    					break;
    				}
    				//if(DEBUG) System.out.println("F: populateIDs did step");
    			}
    			//if(DEBUG) System.out.println("F: populateIDs step done");
    		} finally {st.close();}
    	} catch (SQLException e) {
    		e.printStackTrace();
    		throw new P2PDDSQLException(e);
    	}
    	if(DEBUG) System.out.println("DBInterface:select:results#="+result.size());
    	return result;
    }

    public synchronized long insert(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	long result;
    	try {
    		//db = new SQLiteConnection(file);
			tmp_open(true);
			result = _insert(sql, params, DEBUG);
			tmp_dispose();
		} catch (Exception e) {
			throw new P2PDDSQLException(e);
		}
    	return result;
    }
    public synchronized long _insert(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	return _nosyncinsert(sql, params, DEBUG, true);
    }
    public long _nosyncinsert(String sql, String[] params, boolean DEBUG, boolean return_result) throws P2PDDSQLException{
    	long result = -1;
    	PreparedStatement st;
    	try {
    		if(return_result)
    			st = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    		else
    			st = conn.prepareStatement(sql, Statement.NO_GENERATED_KEYS);
    		if(DEBUG) System.out.println("sqlite:insert:sql: "+sql);
    		try {
    			for(int k=0; k<params.length; k++){
    				st.setString(k+1, params[k]);//.bind(k+1, params[k]);
    				if(DEBUG) System.out.println("sqlite:insert:bind: "+Util.nullDiscrim(params[k]));
    			}
    			st.execute(); //Statement.RETURN_GENERATED_KEYS);
    			if(return_result) {
	    			ResultSet keys = st.getGeneratedKeys();
	//    			for(int i = 0; i<100; i++) {
	//    				try{
	//    					System.out.println("sqlite:insert:result: "+i+" "+keys.getString(i));
	//    				}catch(Exception e){}
	//    				//System.out.println("sqlite:insert:result: "+keys..getString(i));
	//    			}
	    			//System.out.println("sqlite:insert:result: "+" "+keys.getMetaData().getColumnLabel(1)+" n=" +keys.getMetaData().getColumnName(1));
	    			//result = keys.getLong(1);//db.getLastInsertId();
	    			result = keys.getLong("last_insert_rowid()");//db.getLastInsertId();
	    			//result = keys.getRow();//db.getLastInsertId();
    			}
    			if(DEBUG) System.out.println("sqlite:insert:result: "+result);
    		}
    		catch(SQLException e){
    			e.printStackTrace();
    			System.err.println("sqlite:insert:sql: "+sql);
    			for(int k=0; k<params.length; k++){
    				System.err.println("sqlite:insert:bind: "+Util.nullDiscrim(params[k]));
    			}
    			throw e;
    		}finally {st.close();}
    	} catch (SQLException e1) {
    		throw new P2PDDSQLException(e1);
    	}
    	return result;
    }
	public synchronized void update(String sql, String[] params, boolean dbg) throws P2PDDSQLException{
    	try {
    		//conn = new SQLiteConnection(file);
			tmp_open(true);
			_update(sql, params, dbg);    	
			tmp_dispose();
		} catch (Exception e) {
			throw new P2PDDSQLException(e);
		}
	}	
    public synchronized void _update(String sql, String[] params, boolean dbg) throws P2PDDSQLException{
    	PreparedStatement st;
    	try {
    		st = conn.prepareStatement(sql);
    		if(dbg) System.out.println("sqlite:update:sql: "+sql);
    		try {
    			for(int k=0; k<params.length; k++){
    				st.setString(k+1, params[k]);//.bind(k+1, params[k]);
    				if(dbg) System.out.println("sqlite:update:bind: "+params[k]);
    			}
    			st.execute();//.stepThrough();
    		} finally {st.close();}
    	} catch (SQLException e) {
			throw new P2PDDSQLException(e);
		}
    }
    public synchronized void delete(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	try {
    		//db = new SQLiteConnection(file);
    		tmp_open(true);
    		PreparedStatement st = conn.prepareStatement(sql);//, columnIndexes);//.createStatement();
    		st.setQueryTimeout(30);
    		//db.exec("BEGIN IMMEDIATE");
    		//SQLiteStatement st = db.prepare(sql);
    		//statement.setString(parameterIndex, x)
    		if(DEBUG) System.out.println("sqlite:delete:sql: "+sql);
    		try {
    			for(int k=0; k<params.length; k++){
    				st.setString(k+1, params[k]);//bind(k+1, params[k]);
    				if(DEBUG) System.out.println("sqlite:delete:bind: "+params[k]);
    			}
    			st.execute();//.stepThrough();	
    			//conn.setAutoCommit(false);
    			//conn.commit();
    		} finally {st.close();}
    		//db.exec("COMMIT");
    		tmp_dispose();
    	} catch (SQLException e) {
			throw new P2PDDSQLException(e);
    	}
    }
	private void tmp_dispose() throws P2PDDSQLException {
		if(!conn_open) {
			try {
				conn.close();
			} catch (SQLException e) {
				throw new P2PDDSQLException(e);
			}
		}
	}

	private void tmp_open(boolean b) throws P2PDDSQLException {
		if(!conn_open) {
			try {
				conn = DriverManager.getConnection("jdbc:sqlite:"+filename);
			} catch (SQLException e) {
				throw new P2PDDSQLException(e);
			}
		}
	}
	public void open_and_keep(boolean b) throws P2PDDSQLException {
		tmp_open(b);
	}
	public void dispose_and_keep() throws P2PDDSQLException {
		tmp_dispose();
	}

//	@Override
//	public void keep_open(SQLiteConnection conn) {
//    	db = conn;
//    	conn_open = true;
//	}
	@Override
	public void open(String _filename) throws P2PDDSQLException {
		try {
			filename = _filename;
			file = new File(filename);
			Logger logger = Logger.getLogger("com.almworks.sqlite4java");
			logger.setLevel(Level.SEVERE);
//			db = new SQLiteConnection(file);
//			db.open(true);
//			db.dispose();
			conn = DriverManager.getConnection("jdbc:sqlite:"+filename);
			if(!conn_open)
				conn.close();
		} catch (SQLException e) {
			throw new P2PDDSQLException(e);
		}
	}
	public static void main(String args[]) {
		DB_Implementation_JDBC_SQLite db = new DB_Implementation_JDBC_SQLite();
		try {
			db.open(Application.DELIBERATION_FILE);
			//db.delete("DELETE FROM peer WHERE ROWID=?;", new String[]{args[0]}, DEBUG);
			long id = db.insert("INSERT INTO peer (name) VALUES (?);", new String[]{args[0]}, DEBUG);
			System.out.println("Result = "+id);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}

	public void _insert(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException {
    	String sql = DBInterface.makeInsertOrIgnoreSQL(table, fields, params);
    	_nosyncinsert(sql, params, dbg, false);
	}
}