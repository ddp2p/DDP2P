package tools;


import java.util.ArrayList;

import util.DBInterface;
import config.DD;
import table.mirror;
import table.tester; // = tester + updateKeys;
import table.updates;  // = mirror
import table.updatesKeys;
import table.application;
import data.D_MirrorInfo;
import data.D_ReleaseQuality;
import data.D_SoftwareUpdatesReleaseInfoByTester;
import data.D_UpdatesKeysInfo;
import data. D_Tester;
import util.P2PDDSQLException;
import util.Util;

public class MigrateMirrors {
	
	private static final boolean _DEBUG = true;
	static boolean DEBUG = false; 
	public static final String sql_getAppFieldRaw = " SELECT "+application.value+
			         " FROM "+application.TNAME+
			         " WHERE "+application.field+" =?"+" ;";
	public static boolean migrateIfNeeded(String oldDB, String newDB, DBInterface _oldDB, DBInterface _newDB){
		try{
			_migrateIfNeeded(oldDB, newDB, _oldDB, _newDB);
			return true;
		}catch(Exception e){
			return false;
		}
	}
	public static void _migrateIfNeeded(String oldDB, String newDB,
			DBInterface dbSrc, DBInterface dbDes
			)throws Exception{
		
		String[]params = new String[]{DD.DD_DB_VERSION};// where clause?
		
		ArrayList<ArrayList<Object>> u,u2;
		try {
			u = dbSrc.select(sql_getAppFieldRaw, params, DEBUG);
			u2 = dbDes.select(sql_getAppFieldRaw, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		if(u==null || u.size()== 0 || u2==null || u2.size()== 0 )
			return;
		String oldVersion = Util.getString(u.get(0).get(0));
		String newVersion = Util.getString(u2.get(0).get(0));
		if(_DEBUG) System.out.println("MigrateMirrors: oldVersion= "+oldVersion + ",  newVersion= "+newVersion);
		if(Util.isVersionNewer(oldVersion, "1.0.5")) return;
		if(Util.isVersionNewer("1.0.6", newVersion)) return;
		migrate(dbDes);
	}	
	/**
	 * Move from database versions up to 1.0.5 having the "updates" table, to newer databases using the "mirror" table 
	 * @param db
	 */
	public static void migrate(DBInterface db){
	    // migrate from updates to mirror table
		String sql = "SELECT "+updates.fields_updates+" FROM "+updates.TNAME+";";
		String[]params = new String[]{};// where clause?
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		for(ArrayList<Object> _u :u){
			D_MirrorInfo mi = new D_MirrorInfo();
			mi.original_mirror_name = Util.getString(_u.get(table.updates.F_ORIGINAL_MIRROR_NAME));
			mi.my_mirror_name = Util.getString(_u.get(table.updates.F_MY_MIRROR_NAME));	
			mi.url = Util.getString(_u.get(table.updates.F_URL));
			mi.last_version = Util.getString(_u.get(table.updates.F_LAST_VERSION));	
			mi.last_contact_date = Util.getCalendar(Util.getString(_u.get(table.updates.F_LAST_CONTACT)));
			mi.activity = Util.getString(_u.get(table.updates.F_ACTIVITY));
			try {
				if(DEBUG) System.out.println("D_MirrorInfo: releaseQoT <init>: reconstr");
				mi.releaseQoT = D_ReleaseQuality.reconstructArrayFromString(Util.getString(_u.get(table.updates.F_RELEASE_QOT)));
				if(DEBUG) System.out.println("D_MirrorInfo: releaseQoT <init>: reconstructed");
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("MigrateMirror: releaseQoT <migrate>: error handled");
			}
				
			try {
				if(DEBUG) System.out.println("D_MirrorInfo: <init>: testerInfo reconstr");
				mi.testerInfo = D_SoftwareUpdatesReleaseInfoByTester.reconstructArrayFromString(Util.getString(_u.get(table.updates.F_TESTER_INFO)));
				if(DEBUG) System.out.println("D_MirrorInfo: <init>: reconstructed");
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("MigrateMirror: testerInfo <migrate>: error handled");
			}	
			mi.used = Util.stringInt2bool(_u.get(table.updates.F_USED),false);
			
			// how to use other fields, example branch, preferanceDate, ...
			try{
				if(!mi.existsInDB(db))
					mi.store(db);
			} catch(P2PDDSQLException e){
				e.printStackTrace();
				System.err.println("MigrateMirror: mi.store() <migrate>: error handled");
			}
		    
		}
		
		// merge deprecated tester attributes and updatesKeys into the new tester table
		sql = "SELECT "+updatesKeys.fields_updates_keys+" FROM "+updatesKeys.TNAME+";";
		//String[]params = new String[]{};// where clause?
		//ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		
		for(ArrayList<Object> _u :u){
			D_UpdatesKeysInfo ti = new D_UpdatesKeysInfo();
			ti.original_tester_name = Util.getString(_u.get(table.updatesKeys.F_ORIGINAL_TESTER_NAME));
			ti.my_tester_name = Util.getString(_u.get(table.updatesKeys.F_MY_TESTER_NAME));
			ti.public_key = Util.getString(_u.get(table.updatesKeys.F_PUBLIC_KEY));
			ti.public_key_hash = Util.getString(_u.get(table.updatesKeys.F_PUBLIC_KEY_HASH));
			ti.trusted_as_tester =Util.stringInt2bool(_u.get(table.updatesKeys.F_USED_TESTER),false);
			ti.trusted_as_mirror =Util.stringInt2bool(_u.get(table.updatesKeys.F_USED_MIRROR),false);
			ti.reference =Util.stringInt2bool(_u.get(table.updatesKeys.F_REFERENCE),false);
			ti.expected_test_thresholds = Util.getString(_u.get(table.updatesKeys.F_EXPECTED_TEST_THRESHOLDS));
			
			//.retrieveTesterDefinition_ByGID(ti.public_key);
			D_Tester td = D_Tester.getTesterInfoByGID(ti.public_key, false, null, null);
			try {
				if (td != null) {
					ti.tester_ID = td.tester_ID;
					ti.description = td.description;
					ti.email = td.email;
					ti.url = td.url;
					ti.store("update");
			// how to use other fields, example  ...
				} else ti.store("insert");
			} catch(P2PDDSQLException e) {
				e.printStackTrace();
				System.err.println("MigrateMirror: mi.store() <migrate>: error handled");
			}
		    
		}
	}
}
