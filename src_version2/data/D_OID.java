package data;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Hashtable;

import util.P2PDDSQLException;
import util.Util;
import config.Application;

public class D_OID {
	private static final boolean DEBUG = false;
	public String sequence;
	public String oid_ID;
	public String OID_name;
	public String explanation;

	public static Object monitor_oid = new Object();
	public static Hashtable<String, D_OID> oid;
	
	public static boolean _load_OIDs() {
		synchronized (monitor_oid) {
			if (oid != null) return true;
			//boolean DEBUG = false;
			oid = new Hashtable<String, D_OID>();
			String sql = "SELECT " + table.oid.fields_list + 
			" FROM "+
					table.oid.TNAME +";";
			try {
				ArrayList<ArrayList<Object>> oids = Application.db.select(sql, new String[]{},DEBUG);
				for (ArrayList<Object> o : oids) {
					
					D_OID param = new D_OID(); 
					param.sequence = Util.getString(o.get(table.oid.COL_SEQ));
					if (param.sequence == null) continue;
					param.oid_ID = Util.getString(o.get(table.oid.COL_LID));
					param.explanation = Util.getString(o.get(table.oid.COL_EXPL));
					param.OID_name = Util.getString(o.get(table.oid.COL_NAME));
					oid.put(param.sequence, param);
				}
				return true;
				//this.fireTableStructureChanged();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return false;
		}
	}
	public static D_OID getBySequence(BigInteger[] oid2) {
		if (oid == null) _load_OIDs();
		return oid.get(Util.BNOID2String(oid2));
	}
}