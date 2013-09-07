package table;

import java.util.ArrayList;
import java.util.regex.Pattern;

import util.P2PDDSQLException;
import util.Util;

import config.Application;

public class constituent_verification {
	public static final String TNAME = "constituent_verification";
	public static final String constituent_ID = "constituent_ID";
	public static final String challenge = "challenge";
	public static final String date = "date";
	
	public static final String fields = 
					constituent_ID+","+
					challenge+","+
					date;
	public static final String[] fields_list = fields.split(Pattern.quote(","));

	public static final int CV_CONSTITUENT_ID = 0;
	public static final int CV_CHALLENGE = 1;
	public static final int CV_DATE = 2;
	
	public static final int CV_FIELDS = fields_list.length;
	private static final boolean DEBUG = false;

	public static String getChallenge(long constituent_id) {
		try{
			String sql = "SELECT "+fields+
					" WHERE "+table.constituent_verification.constituent_ID +"=?;";
			ArrayList<ArrayList<Object>> a;
			a = Application.db.select(sql, new String[]{Util.getStringID(constituent_id)});
			if(a.size()==0) return null;
			return Util.getString(a.get(0).get(table.constituent_verification.CV_CHALLENGE));
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	private static Object monitor = new Object();
	public static void add(long constituent_id, String challenge,
			String date) {
		synchronized(monitor) {
			try{
				Application.db.deleteNoSync(table.constituent_verification.TNAME,
						new String[]{table.constituent_verification.constituent_ID},
						new String[]{Util.getStringID(constituent_id)}, DEBUG);
				Application.db.insertNoSync(table.constituent_verification.TNAME,
						new String[]{table.constituent_verification.challenge,
						table.constituent_verification.date,
						table.constituent_verification.constituent_ID},
						new String[]{challenge, date, Util.getStringID(constituent_id)}, DEBUG);
			}catch (Exception e){
				e.printStackTrace();
			}
		}
	}
}
