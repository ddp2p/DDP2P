/* Copyright (C) 2014,2015 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
Florida Tech, Human Decision Support Systems Laboratory
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation; either the current version of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. */
/* ------------------------------------------------------------------------- */

package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.regex.Pattern;

import net.ddp2p.common.util.DB_Implementation;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DB_Implementation_Android_SQLITE_ extends SQLiteOpenHelper {
	private Context context;
	
	public DB_Implementation_Android_SQLITE_(Context context, String name,
			int version) {
		
		super(context, name, null, version);
		this.context = context;
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		String create = null;
		try {
			//fix the directory (fixed)	
			AssetManager am = context.getAssets();
			InputStream is = am.open("createEmptyDelib.sqlite");	
			create = Util.readAll(new BufferedReader(new InputStreamReader(is)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//new FileInputStream("")); 
		String creates[] = create.split(Pattern.quote(";"));
		for (String createTable : creates) {
			if (createTable == null) continue;
			String _createTable = createTable.trim();
			if (_createTable.length() == 0) continue;
			database.execSQL(_createTable+";");
		}
	}
	

	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
	
		Log.d("DB", "upgrading");
		if (newVersion > oldVersion && oldVersion <= 1) {
			Log.d("DB", "adding j.org_ID");
			database.execSQL("ALTER TABLE justification ADD COLUMN organization_ID INTEGER;");
            oldVersion = 2;
		}
        if (newVersion > oldVersion && oldVersion <= 2) {
            Log.d("DB", "adding j.subsumes_LID");
            database.execSQL("ALTER TABLE justification ADD COLUMN subsumes_LID INTEGER;");
            oldVersion = 3;
        }
		//util.DBAlter.extractDDL(arg0);
		//db.execSQL("")
	}
}

class DB_Implementation_Android_SQLITE implements DB_Implementation {
	final public static int crt_db_version = 3;
	private SQLiteDatabase db;
	private DB_Implementation_Android_SQLITE_ dbHelper;
	//SQLiteConnection db;
    File file;
	private boolean conn_open = true;
	Connection conn;
	private String filename;

	public DB_Implementation_Android_SQLITE(Context context, String name) {
		dbHelper = new DB_Implementation_Android_SQLITE_(context, name, crt_db_version);
	}

	@Override
	public void open(String arg0) throws P2PDDSQLException {
		db = dbHelper.getWritableDatabase();
	}
	@Override
	public void close() throws P2PDDSQLException {
		dbHelper.close();
	}
	
	@Override
	public long _insert(String sql, String[] params, boolean DEBUG)
			throws P2PDDSQLException {
    	try {
			return _nosyncinsert(sql, params, DEBUG, true);
		} catch (java.sql.SQLException e) {
			e.printStackTrace();
		}
    	return -1;
    }
    public synchronized long _nosyncinsert(String sql, String[] params, boolean DEBUG, boolean return_result) throws P2PDDSQLException, java.sql.SQLException{
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
    				//if(DEBUG) System.out.println("sqlite:insert:bind: "+Util.nullDiscrim(params[k]));
    			}
    			st.execute();
    			if(return_result) {
	    			ResultSet keys = st.getGeneratedKeys();
	    			result = keys.getLong("last_insert_rowid()");//db.getLastInsertId();
    			}
    			if(DEBUG) System.out.println("sqlite:insert:result: "+result);
    		}
    		catch(SQLException e){
    			e.printStackTrace();
    			System.err.println("sqlite:insert:sql: "+sql);
    			for(int k=0; k<params.length; k++){
    				//System.err.println("sqlite:insert:bind: "+Util.nullDiscrim(params[k]));
    			}
    			throw e;
    		}finally {st.close();}
    	} catch (SQLException e1) {
    		throw new P2PDDSQLException(e1);
    	}
    	return result;
	}

	@Override
	public ArrayList<ArrayList<Object>> _select(String sql, String[] params,
			boolean dbg) throws P2PDDSQLException {
		Cursor cursor = db.rawQuery(sql, params);
		ArrayList<ArrayList<Object>> result = new ArrayList<ArrayList<Object>>();
		if (!cursor.moveToFirst()) {
			cursor.close();
			return result;
		}
		int nb = cursor.getColumnCount();
		for (;;) {
			ArrayList<Object> crt = new ArrayList<Object>();
			for (int k = 0; k < nb; k ++)
				crt.add(cursor.getString(k));
			result.add(crt);
			if (!cursor.moveToNext()) break;
		}
		cursor.close();
		return result;
	}

	@Override
	public void _update(String arg0, String[] arg1, boolean arg2)
			throws P2PDDSQLException {
		// TODO Auto-generated method stub
		throw new RuntimeException("");
	}

	@Override
	public void delete(String table, String[] arg1, boolean arg2)
			throws P2PDDSQLException {
		throw new RuntimeException("");
	}

	@Override
	public long insert(String arg0, String[] arg1, boolean arg2)
			throws P2PDDSQLException {
		// TODO Auto-generated method stub
		//return 0;
		throw new RuntimeException("");
	}

	@Override
	public ArrayList<ArrayList<Object>> select(String sql, String[] params,
			boolean arg2) throws P2PDDSQLException {
		Cursor cursor = db.rawQuery(sql, params);
		ArrayList<ArrayList<Object>> result = new ArrayList<ArrayList<Object>>();
		if (!cursor.moveToFirst()) {
			cursor.close();
			return result;
		}
		int nb = cursor.getColumnCount();
		for (;;) {
			ArrayList<Object> crt = new ArrayList<Object>();
			for (int k = 0; k < nb; k ++)
				crt.add(cursor.getString(k));
			result.add(crt);
			if (!cursor.moveToNext()) break;
		}
		cursor.close();
		return result;
	}

	@Override
	public void update(String arg0, String[] arg1, boolean arg2)
			throws P2PDDSQLException {
		// TODO Auto-generated method stub
		throw new RuntimeException("");
	}

	@Override
	public boolean hasParamInsert() {
		return true;
	}

	@Override
	public long tryInsert(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException {
		ContentValues values = new ContentValues();
		for (int k = 0; k < fields.length;  k ++)
			values.put(fields[k], params[k]);
		return db.insertOrThrow(table, null, values);
	}
	
	@Override
	public boolean hasParamDelete() {
		return true;
	}

	@Override
	public void tryDelete(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException {
		String sql = "";
		ArrayList<String> para2 = new ArrayList<String>();
		if (fields.length == 0) sql = "";// "delete from "+table+";";
		else {
	   		if (params[0] != null){
	   			sql = //"delete from "+table+" where "+
	   					fields[0]+"=?";
	   			para2.add(params[0]);
	   		} else
	   			sql = //"delete from "+table+" where "+
	   			fields[0] + " IS NULL ";
	   		for ( int k = 1; k < fields.length; k ++) {
	   	   		if (params[k] != null) {
	   	   			sql = sql + " AND " + fields[k] + "=?";
	   	   			para2.add(params[k]);
	   	   		}else
	   	   			sql = sql + " AND " + fields[k] + " IS NULL ";
	   		}
	   		sql = sql+";";
	   	}
	   	for (int k = 0; k < params.length; k++){
	   		System.out.println("\t orig_param["+k+"]="+params[k]);
	   	}
		String whereClause = sql;
		db.delete(table, whereClause, para2.toArray(new String[0]));
	}

	@Override
	public boolean hasParamUpdate() {
		return true;
	}

	@Override
	public void tryUpdate (String table, String[] fields, String[] filter, String[] params,
			boolean dbg) throws P2PDDSQLException {
		ContentValues values = new ContentValues();
		for (int k = 0; k < fields.length;  k ++)
			values.put(fields[k], params[k]);

		String sql = "";
		ArrayList<String> para2 = new ArrayList<String>();
		if (filter.length == 0) sql = "";// "delete from "+table+";";
		else {
	   		if (params[fields.length + 0] != null){
	   			sql = //"delete from "+table+" where "+
	   					filter[0]+"=?";
	   			para2.add(params[fields.length + 0]);
	   		} else
	   			sql = //"delete from "+table+" where "+
	   			filter[0] + " IS NULL ";
	   		for ( int k = 1; k < filter.length; k ++) {
	   	   		if (params[fields.length + k] != null) {
	   	   			sql = sql + " AND " + filter[k] + "=?";
	   	   			para2.add(params[fields.length + k]);
	   	   		} else
	   	   			sql = sql + " AND " + filter[k] + " IS NULL ";
	   		}
	   		sql = sql+";";
	   	}
		if (dbg)
		   	for (int k = 0; k < params.length; k++) {
		   		System.out.println("DB:\t orig_param["+k+"]="+params[k]);
		   	}
		String whereClause = sql;
		
		db.update(table, values, whereClause, para2.toArray(new String[0]));
	}

}
