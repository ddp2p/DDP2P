package net.ddp2p.common.table;

import java.util.ArrayList;
import java.util.regex.Pattern;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

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
					" WHERE "+net.ddp2p.common.table.constituent_verification.constituent_ID +"=?;";
			ArrayList<ArrayList<Object>> a;
			a = Application.getDB().select(sql, new String[]{Util.getStringID(constituent_id)});
			if(a.size()==0) return null;
			return Util.getString(a.get(0).get(net.ddp2p.common.table.constituent_verification.CV_CHALLENGE));
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
				Application.getDB().deleteNoSync(net.ddp2p.common.table.constituent_verification.TNAME,
						new String[]{net.ddp2p.common.table.constituent_verification.constituent_ID},
						new String[]{Util.getStringID(constituent_id)}, DEBUG);
				Application.getDB().insertNoSync(net.ddp2p.common.table.constituent_verification.TNAME,
						new String[]{net.ddp2p.common.table.constituent_verification.challenge,
						net.ddp2p.common.table.constituent_verification.date,
						net.ddp2p.common.table.constituent_verification.constituent_ID},
						new String[]{challenge, date, Util.getStringID(constituent_id)}, DEBUG);
			}catch (Exception e){
				e.printStackTrace();
			}
		}
	}
}
