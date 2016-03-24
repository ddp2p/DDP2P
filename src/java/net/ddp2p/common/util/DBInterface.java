/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
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
 

package net.ddp2p.common.util;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
//import com.almworks.sqlite4java.*;
import java.io.File;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;

//import util.db.DB_Implementation_SQLite;

//import util.db.DB_Implementation_JDBC_SQLite;


class Listener {
	DBListener listener;
	ArrayList<String> tables;
	Listener(DBListener object, ArrayList<String> _tables){
		listener = object;
		tables= _tables;
	}
}
class DBWorkThread extends net.ddp2p.common.util.DDP2P_ServiceThread {
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	DBListener l;
	ArrayList<String> tables;
	Hashtable<String, DBInfo> info;
	static private int cnt = 0;
	DBWorkThread(DBListener l, ArrayList<String> tables, Hashtable<String, DBInfo> info){
		super ("DB_Worker", false);
		//Util.printCallPath("where called");
		this.l=l;
		this.tables = tables;
		this.info=info;
		start();
	}
	static int name = 0;
	public void _run() {
		this.setName("DB_Worker:"+" "+(name++));
		//ThreadsAccounting.registerThread();
		try {
			__run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		//ThreadsAccounting.unregisterThread();
	}
	public void __run() {
		cnt ++;
		if (DBInterface.DEBUG) System.out.println("DBInterface:DBWorkThread:start cnt="+cnt+" tables="+Util.concat(tables, ":", "def") +" l="+l);
		try {
			if (DEBUG) System.out.println("DBInterface:Work:"+l);
			l.update(tables, info);
			if (DEBUG) System.out.println("DBInterface:Work:done:"+l);
		} catch(RuntimeException e){
			e.printStackTrace();
		}
		cnt --;
		if (DBInterface.DEBUG) System.out.println("done cnt="+cnt+" tables="+Util.concat(tables, ":", "def")+" l="+l);
	}
}
public class DBInterface implements DB_Implementation {
	//ArrayList<Listener> listeners= new ArrayList<Listener>();
	Hashtable <DBListener, ArrayList<String>>hash_listeners=new Hashtable<DBListener, ArrayList<String>>();
	Hashtable<String,ArrayList<DBListener>> hash_tables=new Hashtable<String,ArrayList<DBListener>>();
	public static final int MAX_SELECT = 10000;
	//static final boolean DEBUG = true;
	static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
    //String filename = Application.DELIBERATION_FILE;
    //File file = null;
	public boolean conn_open = false;
	/**
     * 
     * @param object
     * @param tables
     * @param or_selectors: a Hashtable mapping table names to an array of selectors. 
     * 					If any matches then trigger this listener.
     * @return
     */
    public synchronized boolean addListener(DBListener object, ArrayList<String> tables, Hashtable<String,DBSelector[]> or_selectors){
    	//listeners.add(new Listener(object, tables));
    	if(DEBUG) System.out.println("LISTENING: "+object+" ("+tables+")");
    	ArrayList<String> olds = hash_listeners.get(object);
    	if(olds!=null) return false;
    	hash_listeners.put(object,tables);
    	for(String t : tables) {
    		if(DEBUG) System.out.println("LISTENING table: "+t);
    		ArrayList<DBListener> list = hash_tables.get(t);
    		if(list!=null) list.add(object);
    		else {
    			ArrayList<DBListener> elem = new ArrayList<DBListener>();
    			elem.add(object);
    			hash_tables.put(t, elem);
    		}
    	}
    	return true;
    }
    public synchronized void delListener(DBListener object){
    	ArrayList<String> olds = hash_listeners.get(object);
    	hash_listeners.remove(object);
    	if(olds == null) return;
    	for(String s: olds) {
    		ArrayList<DBListener> listeners = hash_tables.get(s);
    		listeners.remove(object);
    	}
    }
    private void fireTableUpdate(ArrayList<String> tables) {
    	//boolean DEBUG = true;
    	if(DEBUG) System.out.println("DBInterface:fireTableUpdate: FIRE UPDATE");
    	HashSet<DBListener> list = new HashSet<DBListener>();
    	for(String table: tables){
        	if(DEBUG) System.out.println("DBInterface:fireTableUpdate: FIRE UPDATE table="+table);
    		ArrayList<DBListener> al = hash_tables.get(table);
    		if(al!=null)list.addAll(al);
    	}
    	if(list.size()==0){
    		if(DEBUG) System.out.println("DBInterface:fireTableUpdate: FIRED UPDATE: Nobody Listens!");
    		return;
    	}
    	for (DBListener l : list) {
    		new DBWorkThread(l,tables,null);
    		if(DEBUG) System.out.println("DBInterface:fireTableUpdate: FIRED UPDATE to: "+l);
    	}
    	if(DEBUG) System.out.println("DBInterface:fireTableUpdate: FIRED UPDATE");
   	
    }
    private void fireTableUpdate(String table){
    	ArrayList<String> als = new ArrayList<String>();
    	als.add(table);
    	fireTableUpdate(als);
    }
	public synchronized void sync(ArrayList<String> tables) {
		fireTableUpdate(tables);
	}
    public synchronized long insert(String table, String[] fields, String[] params) throws P2PDDSQLException {
    	return insert(table, fields, params, DEBUG);
    }
    public synchronized long insert(boolean sync, String table, String[] fields, String[] params, boolean dbg) throws P2PDDSQLException {
    	long result = insertNoSync(table, fields, params, dbg);
    	if(sync) fireTableUpdate(table);
    	return result;
    }
    public synchronized long insert(String table, String[] fields, String[] params, boolean dbg) throws P2PDDSQLException {
    	long result = insert(true, table, fields, params, dbg);
    	return result;
    }
    public synchronized long insertNoSync(String table, String[] fields, String[] params) throws P2PDDSQLException {
    	return insertNoSync(table, fields, params, DEBUG);
    }
    public static String makeInsertSQL(String table, String[] fields, String[] params){
    	String sql="insert into "+table;
    	if(fields.length==0) return sql+" (ROWID) VALUES (NULL);";
//    	if(fields.length==0) return _insert(sql+" (ROWID) VALUES (NULL);", params, dbg);
    	sql+=" ("+fields[0];
    	for( int k=1; k<fields.length; k++) sql = sql+","+fields[k];
    	sql = sql+") values (?";
    	for( int k=1; k<params.length; k++) sql = sql+",?";
    	sql = sql+");";
    	return sql;
    }
    public static String makeInsertOrIgnoreSQL(String table, String[] fields, String[] params){
    	String sql="insert or ignore into "+table;
    	if(fields.length==0) return sql+" (ROWID) VALUES (NULL);";
//    	if(fields.length==0) return _insert(sql+" (ROWID) VALUES (NULL);", params, dbg);
    	sql+=" ("+fields[0];
    	for( int k=1; k<fields.length; k++) sql = sql+","+fields[k];
    	sql = sql+") values (?";
    	for( int k=1; k<params.length; k++) sql = sql+",?";
    	sql = sql+");";
    	return sql;
    }
    public synchronized long insertNoSync(String table, String[] fields, String[] params, boolean dbg) throws P2PDDSQLException {
    	//if(dbg) Util.printCallPath("insert sources");
   		if(dbg) System.out.println("DBInterface:insertNoSync: insert in: "+table+" "+fields.length+" "+params.length);
    	if(db.hasParamInsert())
    		return db.tryInsert(table, fields, params, dbg);
    	String sql = makeInsertSQL(table, fields, params);
    	return insert(sql, params,dbg);
    }
    /**
     * To be called only when the database is already open
     * @param table
     * @param fields
     * @param params
     * @param dbg
     * @return
     * @throws P2PDDSQLException
     */
    public synchronized long _insertNoSync(String table, String[] fields, String[] params, boolean dbg) throws P2PDDSQLException {
    	//if(dbg) Util.printCallPath("insert sources");
    	if(dbg) System.out.println("DBInterface:insertNoSync: insert in: "+table+" "+fields.length+" "+params.length);
    	String sql = makeInsertSQL(table, fields, params);
    	return _insert(sql, params,dbg);
    }
    public synchronized long insert(String sql, String[] params) throws P2PDDSQLException{
    	return insert(sql, params, DEBUG);
    }
    public synchronized void update(boolean sync, String table, String[] fields, String[]selector, String[] params, boolean dbg) throws P2PDDSQLException {
    	updateNoSync(table, fields, selector, params, dbg);
     	if(sync) fireTableUpdate(table);
    }
    public synchronized void update(String table, String[] fields, String[]selector, String[] params, boolean dbg) throws P2PDDSQLException {
    	update(true, table, fields, selector, params, dbg);
    }
    public synchronized void update(String table, String[] fields, String[]selector, String[] params) throws P2PDDSQLException {
    	update(table, fields, selector, params, DEBUG);
    }
	public void updateNoSync(String table, String[] fields, String[] selector, String[] params) throws P2PDDSQLException {
		updateNoSync(table, fields, selector, params, DEBUG);
	}
	public String makeUpdateSQL(String table, String[] fields, String[]selector, String[] params) {
    	String sql="update "+table+" set "+fields[0]+"=?";
    	for( int k=1; k<fields.length; k++) sql = sql+","+fields[k]+"=?";
    	if(selector.length>0) {
    		sql = sql+" where "+selector[0]+"=?";
    		for( int k=1; k<selector.length; k++) sql = sql+" AND "+selector[k]+"=?";
    	}
    	sql = sql+";";
		return sql;
	}
	public String makeUpdateNULLSQL(String table, String[] fields, String[]selector, String[] params, ArrayList<String> para2) {
		int cp = 0;
    	String sql="update "+table+" set "+fields[0]+"=?";
    	for( int k=1; k<fields.length; k++) {
    		sql = sql+","+fields[k]+"=?";
    		//para2.add(params[cp++]);
    		cp++;
    	}
    	if(selector.length>0) {
    		if(params[fields.length] != null){
    			sql = sql+" where "+selector[0]+"=?";
    			para2.add(params[fields.length]);
    		}else
    			sql = sql+" where "+selector[0]+" IS NULL ";
    		cp++;
    		for( int k=1; k<selector.length; k++){
        		if(params[fields.length + k] != null){
        			sql = sql+" AND "+selector[k]+"=?";
        			para2.add(params[cp]);
        		}else
        			sql = sql+" AND "+selector[k]+" IS NULL ";
        		cp++;
    		}
    	}
    	sql = sql+";";
		return sql;
	}
	/**
	 * Update setting null to IS NULL
	 * @param table
	 * @param fields
	 * @param selector
	 * @param params
	 * @param dbg
	 * @throws P2PDDSQLException
	 */
    public synchronized void updateNoSyncNULL(String table, String[] fields, String[]selector, String[] params, boolean dbg) throws P2PDDSQLException {
    	if (db.hasParamUpdate()) {
    		tryUpdate(table, fields, selector, params, dbg);
    		return;
    	}
		ArrayList<String> para2 = new ArrayList<String>();
		if(dbg){
			for(int k = 0; k<params.length; k++) {
				System.out.println("\t orig_param:["+k+"]="+params[k]);
			}
		}
    	String sql = makeUpdateNULLSQL(table, fields, selector, params, para2);
    	String[] _params = new String[fields.length+para2.size()];
    	for(int k=0; k<fields.length; k++){
    		_params[k] = params[k];
    	}
    	for(int k=0; k<para2.size(); k++){
    		_params[k+fields.length] = para2.get(k);
    	}
    	update(sql, _params, dbg);    	
    }
    public synchronized void updateNoSync(String table, String[] fields, String[]selector, String[] params, boolean dbg) throws P2PDDSQLException {
    	if (db.hasParamUpdate()) {
    		db.tryUpdate(table, fields, selector, params, dbg);
    		return;
    	}
    	String sql = makeUpdateSQL(table, fields, selector, params);
    	update(sql, params, dbg);    	
    }
    /**
     * To be called only when the database is already open
     * @param table
     * @param fields
     * @param selector
     * @param params
     * @param dbg
     * @throws P2PDDSQLException
     */
    public synchronized void _updateNoSync(String table, String[] fields, String[]selector, String[] params, boolean dbg) throws P2PDDSQLException {
    	if (db.hasParamUpdate()) {
    		db.tryUpdate(table, fields, selector, params, dbg);
    		return; 
    	}
    	String sql = makeUpdateSQL(table, fields, selector, params);
    	_update(sql, params, dbg);
    }
 	public synchronized void update(String sql, String[] params) throws P2PDDSQLException {
		update(sql, params, DEBUG);
	}
	public void delete(String table, String[] fields, String[] params) throws P2PDDSQLException {
		delete(table,fields,params, DEBUG);
	}
	public synchronized void delete(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException{
		deleteNoSync(table, fields, params, dbg);
		fireTableUpdate(table);
	}
	public synchronized void delete(boolean sync, String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException{
		deleteNoSync(table, fields, params, dbg);
		if(sync)fireTableUpdate(table);
	}
   public synchronized void deleteNoSync(String table, String[] fields, String[] params) throws P2PDDSQLException{
	   deleteNoSync(table, fields, params, DEBUG);
   }
   /**
    * Probably should be replaced with deleteNoSyncNULL
    * @param table
    * @param fields
    * @param params
    * @param dbg
    * @throws P2PDDSQLException
    */
   public synchronized void deleteNoSync(String table, String[] fields, String[] params, boolean dbg) throws P2PDDSQLException{
   	String sql;
   	if (db.hasParamDelete()) {
   		db.tryDelete(table, fields, params, dbg);
   		return;
   	}
   	if (fields.length == 0) sql = "delete from "+table+";";
   	else {
   		sql = "delete from " + table + " where " + fields[0] + "=?";
   		for ( int k = 1; k < fields.length; k ++) sql = sql+" AND "+fields[k]+"=?";
   		sql = sql+";";
   	}
   	delete (sql, params, dbg);
   }
   public synchronized void deleteNoSyncNULL(String table, String[] fields, String[] params, boolean dbg) throws P2PDDSQLException{
   	String sql;
   	ArrayList<String> para2 = new ArrayList<String>();
   	if(fields.length == 0) sql = "delete from "+table+";";
   	else{
   		if(params[0]!=null){
   			sql="delete from "+table+" where "+fields[0]+"=?";
   			para2.add(params[0]);
   		}else
   			sql="delete from "+table+" where "+fields[0]+" IS NULL ";
   		for( int k=1; k<fields.length; k++){
   	   		if(params[k]!=null) {
   	   			sql = sql+" AND "+fields[k]+"=?";
   	   			para2.add(params[k]);
   	   		}else
   	   			sql = sql+" AND "+fields[k]+" IS NULL ";
   		}
   		sql = sql+";";
   	}
   	for(int k=0;k<params.length;k++){
   		System.out.println("\t orig_param["+k+"]="+params[k]);
   	}
   	delete(sql, para2.toArray(new String[]{}), dbg);
   }
   public synchronized void delete(String sql, String[] params) throws P2PDDSQLException{
	   delete(sql, params, DEBUG);
   }
    public synchronized ArrayList<ArrayList<Object>> select(String sql, String[] params) throws P2PDDSQLException{
    	return select(sql, params, DEBUG);
    }

/**
 * DB procedures
 */
    DB_Implementation db;
    /**
     * Called with an open connection
     * 
     * @param _db
     */
     public DBInterface(DB_Implementation _db) {
    	db = _db;
    }
    public DBInterface(String _filename) throws P2PDDSQLException{
    	init(_filename);
    }
    public DBInterface(File _filename) throws P2PDDSQLException{
     	init(_filename.getAbsolutePath());
    }
    public void init(String _filename) throws P2PDDSQLException{
    	if (DEBUG) System.out.println("DBInterface: init "+_filename);
    	//this.filename = _filename;
     	//db = new DB_Implementation_SQLite();
    	//db = new DB_Implementation_JDBC_SQLite();
    	db = Application_GUI.get_DB_Implementation();
    	if (db == null) {
    		System.out.println("DBInterface:init: database implementation is missing");
    		System.exit(0);
    	}
    	db.open(_filename);
    }
//    public synchronized void exec(String sql) throws P2PDDSQLException{
//    	db = new SQLiteConnection(file);
//    	db.open(true);
//    	db.exec("BEGIN IMMEDIATE");
//    	db.exec(sql);
//    	if(DEBUG) System.err.println("executed: "+sql);
//    	db.exec("COMMIT");
//    	db.dispose();
//    }
	@Override
	public ArrayList<ArrayList<Object>> select(String sql, String[] params,
			boolean DEBUG) throws P2PDDSQLException {
		return db.select(sql, params, DEBUG);
	}
	@Override
	public ArrayList<ArrayList<Object>> _select(String sql, String[] params,
			boolean DEBUG) throws P2PDDSQLException {
		return db._select(sql, params, DEBUG);
	}
	@Override
	public long _insert(String sql, String[] params, boolean DEBUG)
			throws P2PDDSQLException {
		return db._insert(sql, params, DEBUG);
	}
	@Override
	public long insert(String sql, String[] params, boolean DEBUG)
			throws P2PDDSQLException {
		return db.insert(sql, params, DEBUG);
	}
	@Override
	public void delete(String sql, String[] params, boolean DEBUG)
			throws P2PDDSQLException {
		db.delete(sql, params, DEBUG);
	}
	@Override
	public void update(String sql, String[] params, boolean dbg)
			throws P2PDDSQLException {
		db.update(sql, params, dbg);
	}
	@Override
	public void _update(String sql, String[] params, boolean dbg)
			throws P2PDDSQLException {
		db._update(sql, params, dbg);
	}
	@Override
	public void open(String _filename) throws P2PDDSQLException {
		db.open(_filename);
	}
//	@Override
//	public void keep_open(SQLiteConnection conn) {
//		db.keep_open(conn);
//	}
	public DB_Implementation getImplementation() {
		return db;
	}
	public void close() throws P2PDDSQLException {
		db.close();
	}
	@Override
	public boolean hasParamInsert() {
		return true;
	}
	@Override
	public long tryInsert(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException {
		return this.insertNoSync(table, fields, params, dbg);
	}
	@Override
	public boolean hasParamDelete() {
		return true;
	}
	@Override
	public void tryDelete(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException {
		delete(table, fields, params, dbg);
		
	}
	@Override
	public boolean hasParamUpdate() {
		return true;
	}
	@Override
	public void tryUpdate(String table, String[] fields, String[] selector,
			String[] params, boolean dbg) throws P2PDDSQLException {
		updateNoSync(table, fields, selector,
				params, dbg);
	}
	@Override
	public String getName() {
		if (this.db != null) return this.db.getName();
		return null;
	}	

}
