package net.ddp2p.common.util;
import java.util.ArrayList;
public 
interface DB_Implementation{
	public ArrayList<ArrayList<Object>> select(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException;
	public ArrayList<ArrayList<Object>> _select(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException;
	public long _insert(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException;
	public long insert(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException;
    public void delete(String sql, String[] params, boolean DEBUG) throws P2PDDSQLException;
    public void update(String sql, String[] params, boolean dbg) throws P2PDDSQLException;
    public void _update(String sql, String[] params, boolean dbg) throws P2PDDSQLException;
    public void open(String _filename) throws P2PDDSQLException;
	public void close() throws P2PDDSQLException;
	public boolean hasParamInsert();
	public long tryInsert(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException;
	public boolean hasParamDelete();
	public void tryDelete(String table, String[] fields, String[] params,
			boolean dbg) throws P2PDDSQLException;
	public boolean hasParamUpdate();
	public void tryUpdate(String table, String[] fields, String[] selector,
			String[] params, boolean dbg) throws P2PDDSQLException;
}
