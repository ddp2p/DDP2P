/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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
package net.ddp2p.java.db;

import java.io.File;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DB_Implementation;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

//for a in */*.java ; do  mv $a /tmp/ttt; sed 's/SQLiteException/P2PDDSQLException/g' /tmp/ttt > $a ; rm /tmp/ttt  ; done
//for a in */*/*.java ; do  mv $a /tmp/ttt; sed 's/SQLiteException/P2PDDSQLException/g' /tmp/ttt > $a ; rm /tmp/ttt  ; done
//for a in */*.java ; do  mv $a /tmp/ttt; sed 's/com.almworks.sqlite4java.P2PDDSQLException/util.P2PDDSQLException/g' /tmp/ttt > $a ; rm /tmp/ttt  ; done
//for a in */*/*.java ; do  mv $a /tmp/ttt; sed 's/com.almworks.sqlite4java.P2PDDSQLException/util.P2PDDSQLException/g' /tmp/ttt > $a ; rm /tmp/ttt  ; done

class DB_Implementation_SQLite implements DB_Implementation {
    SQLiteConnection db;
    File file;
	private boolean conn_open;
	private SQLiteConnection conn;
	private String filename;
	@Override
	public void close() {
		if(conn_open)
			if (db != null) db.dispose();
	}
    public synchronized ArrayList<ArrayList<Object>> select(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	//try{throw new Exception("?");}catch(Exception e){e.printStackTrace();}
    	//conn_open = true;
    	db = new SQLiteConnection(file);
    	try {
			db.open(true);
		} catch (SQLiteException e) {
			//e.printStackTrace();
			throw new P2PDDSQLException(e);
		}
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
    public synchronized ArrayList<ArrayList<Object>> _select(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	//if(!conn_open) throw new RuntimeException("Assumption failed!");
    	ArrayList<ArrayList<Object>> result = new ArrayList<ArrayList<Object>>();
    	SQLiteStatement st;
    	try {
    		st = db.prepare(sql);
    		if(DEBUG) System.out.println("sqlite:select:sql: "+sql+" length="+params.length);
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
    				if (result.size()>DBInterface.MAX_SELECT) {
    					Application_GUI.warning("Found more results than: "+DBInterface.MAX_SELECT, "SQLITE select");
    					break;
    				}
    				//if(DEBUG) System.out.println("F: populateIDs did step");
    			}
    			//if(DEBUG) System.out.println("F: populateIDs step done");
    		} finally {st.dispose();}
    	} catch (SQLiteException e) {
    		throw new P2PDDSQLException(e);
    	}
    	if(DEBUG) System.out.println("DBInterface:select:results#="+result.size());
    	return result;
    }

    public synchronized long insert(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	long result;
    	try {
    	db = new SQLiteConnection(file);
			db.open(true);
			result = _insert(sql, params, DEBUG);
			db.dispose();
		} catch (SQLiteException e) {
			throw new P2PDDSQLException(e);
		}
    	return result;
    }
    public synchronized long _insert(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	long result;
    	SQLiteStatement st;
    	try {
    		st = db.prepare(sql);
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
    	} catch (SQLiteException e1) {
    		throw new P2PDDSQLException(e1);
    	}
    	return result;
    }
	public synchronized void update(String sql, String[] params, boolean dbg) throws P2PDDSQLException{
    	try {
    		db = new SQLiteConnection(file);
			db.open(true);
			_update(sql, params, dbg);    	
			db.dispose();
		} catch (SQLiteException e) {
			throw new P2PDDSQLException(e);
		}
	}	
    public synchronized void _update(String sql, String[] params, boolean dbg) throws P2PDDSQLException{
    	SQLiteStatement st;
    	try {
    		st = db.prepare(sql);
    		if(dbg) System.out.println("sqlite:update:sql: "+sql);
    		try {
    			for(int k=0; k<params.length; k++){
    				st.bind(k+1, params[k]);
    				if(dbg) System.out.println("sqlite:update:bind: "+params[k]);
    			}
    			st.stepThrough();
    		} finally {st.dispose();}
    	} catch (SQLiteException e) {
			throw new P2PDDSQLException(e);
		}
    }
    public synchronized void delete(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException{
    	try {
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
    	} catch (SQLiteException e) {
			throw new P2PDDSQLException(e);
    	}
    }
	public void keep_open(SQLiteConnection conn) {
    	db = conn;
    	conn_open = true;
	}
	@Override
	public void open(String _filename) throws P2PDDSQLException {
		try {
			filename = _filename;
			file = new File(filename);
			Logger logger = Logger.getLogger("com.almworks.sqlite4java");
			logger.setLevel(Level.SEVERE);
			db = new SQLiteConnection(file);
			db.open(true);
			db.dispose();
		} catch (SQLiteException e) {
			throw new P2PDDSQLException(e);
		}
	}

	@Override
	public boolean hasParamInsert() {
		return false;
	}

	@Override
	public long tryInsert(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException {
		throw new RuntimeException("");
	}

	@Override
	public boolean hasParamDelete() {
		return false;
	}

	@Override
	public void tryDelete(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException {
		throw new RuntimeException("");
	}

	@Override
	public boolean hasParamUpdate() {
		return false;
	}

	@Override
	public void tryUpdate(String table, String[] fields, String[] selector,
			String[] params, boolean dbg) throws P2PDDSQLException {
		throw new RuntimeException("");
	}
	@Override
	public String getName() {
		return filename;
	}
}
/**
SELECT DISTINCT m.motion_ID FROM motion AS m  WHERE m.organization_ID=1 AND
m.motion_ID NOT IN ( SELECT nm.motion_ID FROM motion AS nm LEFT JOIN
signature AS s ON(s.motion_ID=nm.motion_ID)  WHERE nm.organization_ID=1 AND
s.constituent_ID=23) LIMIT 1 OFFSET 194;

SELECT count(DISTINCT m.motion_ID)  FROM motion AS m  WHERE
m.organization_ID=1 AND m.motion_ID NOT IN ( SELECT nm.motion_ID FROM
motion AS nm LEFT JOIN signature AS s ON(s.motion_ID=nm.motion_ID)  WHERE
nm.organization_ID=1 AND s.constituent_ID=23);
*/