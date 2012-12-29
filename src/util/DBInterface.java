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
 

package util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almworks.sqlite4java.*;

import config.Application;

import java.awt.Event;
import java.io.File;

import javax.swing.JOptionPane;
class Listener{
	DBListener listener;
	ArrayList<String> tables;
	Listener(DBListener object, ArrayList<String> _tables){
		listener = object;
		tables= _tables;
	}
}
public class DBInterface {
	//ArrayList<Listener> listeners= new ArrayList<Listener>();
	Hashtable <DBListener, ArrayList<String>>hash_listeners=new Hashtable<DBListener, ArrayList<String>>();
	Hashtable<String,ArrayList<DBListener>> hash_tables=new Hashtable<String,ArrayList<DBListener>>();
	static final int MAX_SELECT = 10000;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
    String filename = Application.DELIBERATION_FILE;
    File file = null;
    SQLiteConnection db;
	public boolean conn_open = false;
    public DBInterface(String _filename) throws com.almworks.sqlite4java.SQLiteException{
    	filename = _filename;
    	file = new File(filename);
    	Logger logger = Logger.getLogger("com.almworks.sqlite4java");
    	logger.setLevel(Level.SEVERE);
    	db = new SQLiteConnection(file);
    	db.open(true);
    	db.dispose();
    }
    /**
     * Call with an open connection
     * @param conn
     */
    public DBInterface(SQLiteConnection conn) {
    	db = conn;
    	conn_open = true;
	}
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
    private void fireTableUpdate(ArrayList<String> tables){
    	if(DEBUG) System.out.println("FIRE UPDATE");
    	HashSet<DBListener> list = new HashSet<DBListener>();
    	for(String table: tables){
    		ArrayList<DBListener> al = hash_tables.get(table);
    		if(al!=null)list.addAll(al);
    	}
    	if(list.size()==0){
    		if(DEBUG) System.out.println("FIRED UPDATE: Nobody Listens!");
    		return;
    	}
    	for (DBListener l : list) {
    		try{
    			l.update(tables, null);
    		}catch(RuntimeException e){
    			e.printStackTrace();
    		}
    		if(DEBUG) System.out.println("FIRED UPDATE to: "+l);
    	}
    	if(DEBUG) System.out.println("FIRED UPDATE");
   	
    }
    private void fireTableUpdate(String table){
    	ArrayList<String> als = new ArrayList<String>();
    	als.add(table);
    	fireTableUpdate(als);
    }
	public synchronized void sync(ArrayList<String> tables) {
		fireTableUpdate(tables);
	}
    public synchronized void exec(String sql) throws com.almworks.sqlite4java.SQLiteException{
    	db = new SQLiteConnection(file);
    	db.open(true);
    	db.exec("BEGIN IMMEDIATE");
    	db.exec(sql);
    	if(DEBUG) System.err.println("executed: "+sql);
    	db.exec("COMMIT");
    	db.dispose();
    }
    public synchronized long insert(String table, String[] fields, String[] params) throws com.almworks.sqlite4java.SQLiteException {
    	return insert(table, fields, params, DEBUG);
    }
    public synchronized long insert(String table, String[] fields, String[] params, boolean dbg) throws com.almworks.sqlite4java.SQLiteException {
    	long result = insertNoSync(table, fields, params, dbg);
    	fireTableUpdate(table);
    	return result;
    }
    public synchronized long insertNoSync(String table, String[] fields, String[] params) throws com.almworks.sqlite4java.SQLiteException {
    	return insertNoSync(table, fields, params, DEBUG);
    }
    public String makeInsertSQL(String table, String[] fields, String[] params){
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
    public synchronized long insertNoSync(String table, String[] fields, String[] params, boolean dbg) throws com.almworks.sqlite4java.SQLiteException {
    	//if(dbg) Util.printCallPath("insert sources");
   		if(dbg) System.out.println("DBInterface:insertNoSync: insert in: "+table+" "+fields.length+" "+params.length);
    	String sql = makeInsertSQL(table, fields, params);
    	return insert(sql, params,dbg);
    }
    public synchronized long _insertNoSync(String table, String[] fields, String[] params, boolean dbg) throws com.almworks.sqlite4java.SQLiteException {
    	//if(dbg) Util.printCallPath("insert sources");
    	if(dbg) System.out.println("DBInterface:insertNoSync: insert in: "+table+" "+fields.length+" "+params.length);
    	String sql = makeInsertSQL(table, fields, params);
    	return _insert(sql, params,dbg);
    }
    public synchronized long insert(String sql, String[] params) throws com.almworks.sqlite4java.SQLiteException{
    	return insert(sql, params, DEBUG);
    }
    public synchronized long insert(String sql, String[] params, boolean DEBUG) throws com.almworks.sqlite4java.SQLiteException{
    	long result;
    	db = new SQLiteConnection(file);
    	db.open(true);
    	result = _insert(sql, params, DEBUG);
    	db.dispose();
    	return result;
    }
    public synchronized long _insert(String sql, String[] params, boolean DEBUG) throws com.almworks.sqlite4java.SQLiteException{
    	long result;
    	SQLiteStatement st = db.prepare(sql);
    	if(DEBUG) System.out.println("sqlite:insert:sql: "+sql);
    	try {
    		for(int k=0; k<params.length; k++){
    			st.bind(k+1, params[k]);
    			if(DEBUG) System.out.println("sqlite:insert:bind: "+Util.nullDiscrim(params[k]));
    		}
    		st.stepThrough();

    		result = db.getLastInsertId();
    		if(DEBUG) System.out.println("sqlite:insert:result: "+result);
    	}
    	catch(com.almworks.sqlite4java.SQLiteException e){
    		e.printStackTrace();
    		System.err.println("sqlite:insert:sql: "+sql);
    		for(int k=0; k<params.length; k++){
    			System.err.println("sqlite:insert:bind: "+Util.nullDiscrim(params[k]));
    		}
    		throw e;
    	}finally {st.dispose();}
    	return result;
    }
    public synchronized void update(String table, String[] fields, String[]selector, String[] params, boolean dbg) throws com.almworks.sqlite4java.SQLiteException {
    	updateNoSync(table, fields, selector, params, dbg);
     	fireTableUpdate(table);
    }
    public synchronized void update(String table, String[] fields, String[]selector, String[] params) throws com.almworks.sqlite4java.SQLiteException {
    	update(table, fields, selector, params, DEBUG);
    }
	public void updateNoSync(String table, String[] fields, String[] selector, String[] params) throws SQLiteException {
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
    public synchronized void updateNoSync(String table, String[] fields, String[]selector, String[] params, boolean dbg) throws com.almworks.sqlite4java.SQLiteException {
    	String sql = makeUpdateSQL(table, fields, selector, params);
    	update(sql, params, dbg);    	
    }
    public synchronized void _updateNoSync(String table, String[] fields, String[]selector, String[] params, boolean dbg) throws com.almworks.sqlite4java.SQLiteException {
    	String sql = makeUpdateSQL(table, fields, selector, params);
    	_update(sql, params, dbg);
    }
    public synchronized void update(String sql, String[] params) throws SQLiteException {
		update(sql, params, DEBUG);
	}
	public synchronized void update(String sql, String[] params, boolean dbg) throws com.almworks.sqlite4java.SQLiteException{
    	db = new SQLiteConnection(file);
    	db.open(true);
    	_update(sql, params, dbg);    	
    	db.dispose();
	}	
	public synchronized void _update(String sql, String[] params, boolean dbg) throws com.almworks.sqlite4java.SQLiteException{
    	SQLiteStatement st = db.prepare(sql);
    	if(dbg) System.out.println("sqlite:update:sql: "+sql);
    	try {
    	    for(int k=0; k<params.length; k++){
    	    	st.bind(k+1, params[k]);
    	    	if(dbg) System.out.println("sqlite:update:bind: "+params[k]);
    	    }
    	    st.stepThrough();
    	} finally {st.dispose();}
    }
	public void delete(String table, String[] fields, String[] params) throws com.almworks.sqlite4java.SQLiteException {
		delete(table,fields,params, DEBUG);
	}
   public synchronized void delete(String table, String[] fields, String[] params,
			boolean dbg) throws com.almworks.sqlite4java.SQLiteException{
    	deleteNoSync(table, fields, params, dbg);
    	fireTableUpdate(table);
    }
   public synchronized void deleteNoSync(String table, String[] fields, String[] params) throws com.almworks.sqlite4java.SQLiteException{
	   deleteNoSync(table, fields, params, DEBUG);
   }
   public synchronized void deleteNoSync(String table, String[] fields, String[] params, boolean dbg) throws com.almworks.sqlite4java.SQLiteException{
    	String sql;
    	if(fields.length == 0) sql = "delete from "+table+";";
    	else{
    		sql="delete from "+table+" where "+fields[0]+"=?";
    		for( int k=1; k<fields.length; k++) sql = sql+" AND "+fields[k]+"=?";
    		sql = sql+";";
    	}
    	delete(sql, params, dbg);    	
    }
   public synchronized void delete(String sql, String[] params) throws com.almworks.sqlite4java.SQLiteException{
	   delete(sql, params, DEBUG);
   }
   public synchronized void delete(String sql, String[] params, boolean DEBUG) throws com.almworks.sqlite4java.SQLiteException{
    	db = new SQLiteConnection(file);
    	db.open(true);
    	db.exec("BEGIN IMMEDIATE");
    	SQLiteStatement st = db.prepare(sql);
    	if(DEBUG) System.out.println("sqlite:delete:sql: "+sql);
    	try {
    		for(int k=0; k<params.length; k++){
    			st.bind(k+1, params[k]);
    			if(DEBUG) System.out.println("sqlite:delete:bind: "+params[k]);
    		}
    		st.stepThrough();	    
    	} finally {st.dispose();}
    	db.exec("COMMIT");
    	db.dispose();
    }
    public synchronized ArrayList<ArrayList<Object>> select(String sql, String[] params) throws com.almworks.sqlite4java.SQLiteException{
    	return select(sql, params, DEBUG);
    }
    public synchronized ArrayList<ArrayList<Object>> select(String sql, String[] params, boolean DEBUG) throws com.almworks.sqlite4java.SQLiteException{
    	//try{throw new Exception("?");}catch(Exception e){e.printStackTrace();}
    	//conn_open = true;
    	db = new SQLiteConnection(file);
    	db.open(true);
    	ArrayList<ArrayList<Object>> result = _select(sql, params, DEBUG);
    	db.dispose();
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
    public synchronized ArrayList<ArrayList<Object>> _select(String sql, String[] params, boolean DEBUG) throws com.almworks.sqlite4java.SQLiteException{
    	//if(!conn_open) throw new RuntimeException("Assumption failed!");
    	SQLiteStatement st = db.prepare(sql);
    	if(DEBUG) System.out.println("sqlite:select:sql: "+sql+" length="+params.length);
    	ArrayList<ArrayList<Object>> result = new ArrayList<ArrayList<Object>>();
    	try {
    		for(int k=0; k<params.length; k++){
    			st.bind(k+1, params[k]);
    			if(DEBUG) System.out.println("sqlite:select:bind: "+params[k]);
    		}
    		//if(DEBUG) System.out.println("F: populateIDs will step");
    		while (st.step()) {
    			//if(DEBUG) System.out.println("F: populateIDs step");
    			ArrayList<Object> cresult = new ArrayList<Object>();
    			for(int j=0; j<st.columnCount(); j++){
    				//if(DEBUG) System.out.println("F: populateIDs col:"+j);
    				cresult.add(st.columnValue(j));
    			}
    			result.add(cresult);
    			if (result.size()>MAX_SELECT) {
    				JOptionPane.showMessageDialog(null,"Found more results than: "+MAX_SELECT);
    				break;
    			}
    			//if(DEBUG) System.out.println("F: populateIDs did step");
    		}
    		//if(DEBUG) System.out.println("F: populateIDs step done");
    	} finally {st.dispose();}
    	if(DEBUG) System.out.println("DBInterface:select:results#="+result.size());
    	return result;
    }
}
/*
 select fv.name, fv.forename, fv.constituentID, fv.external, fv.global_constituentID  from constituents AS fv JOIN fields_values AS f ON f.constituentID = fv.constituentID   JOIN fields_values AS fv0 ON fv.constituentID=fv0.constituentID  JOIN fields_values AS fv1 ON fv.constituentID=fv1.constituentID  JOIN fields_values AS fv2 ON fv.constituentID=fv2.constituentID  JOIN fields_values AS fv3 ON fv.constituentID=fv3.constituentID  JOIN fields_values AS fv4 ON fv.constituentID=fv4.constituentID  JOIN fields_values AS fv5 ON fv.constituentID=fv5.constituentID  WHERE fv.organizationID = 100 AND  (  f.value = 'Lunii' AND f.fieldID = 17  AND fv0.value = 'EU' AND fv0.fieldID = 11  AND fv1.value = 'RO' AND fv1.fieldID = 12  AND fv2.value = 'Transylvania' AND fv2.fieldID = 14  AND fv3.value = 'Cluj' AND fv3.fieldID = 14  AND fv4.value = 'Cluj-Napoca' AND fv4.fieldID = 15  AND fv5.value = 'Zorilor' AND fv5.fieldID = 16  ) OR fv.neighborhoodID = 29  GROUP BY fv.constituentID;
 */
