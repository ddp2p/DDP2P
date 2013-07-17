package util;

import java.util.ArrayList;

import com.almworks.sqlite4java.SQLiteConnection;

interface DB_Implementation{
	public ArrayList<ArrayList<Object>> select(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException;
	public ArrayList<ArrayList<Object>> _select(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException;
	public long _insert(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException;
	public long insert(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException;
    public void delete(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException;
    public void update(String sql, String[] params, boolean dbg) throws P2PDDSQLException;
    public void _update(String sql, String[] params, boolean dbg) throws P2PDDSQLException;
    public void keep_open(SQLiteConnection conn);
    public void open(String _filename) throws P2PDDSQLException;
}