package net.ddp2p.common.table;
import java.util.ArrayList;
import java.util.regex.Pattern;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
public class directory_tokens {
	public static final String directory_tokens_ID = "directory_tokens_ID";
	public static final String peer_ID = "peer_ID";
	public static final String peer_instance_ID = "peer_instance_ID";
	public static final String directory_domain = "directory_domain";
	public static final String directory_tcp_port = "directory_tcp_port";
	public static final String token = "token";
	public static final String instructions_from_directory = "instructions_from_directory";
	public static final String date_instructions = "date_instructions";
	public static final String TNAME = "directory_tokens";
	public static final String fields_noID =
			peer_ID+","+
					peer_instance_ID+","+
					directory_domain+","+
					directory_tcp_port+","+
					token+","+
					instructions_from_directory+","+
					date_instructions;
	public static final String fields =
			fields_noID+","+directory_tokens_ID;
	public static final String[] fields_list = fields.split(Pattern.quote(","));
	public static final String[] fields_noID_list = fields_noID.split(Pattern.quote(","));
	public static final int PA_PEER_ID = 0;
	public static final int PA_INSTANCE = 1;
	public static final int PA_DOMAIN = 2;
	public static final int PA_TCP = 3;
	public static final int PA_TOKEN = 4;
	public static final int PA_INSTRUCTIONS_DIR = 5;
	public static final int PA_DATE_INSTRUCTIONS = 6;
	public static final int PA_DIRECTORY_TOKEN_ID = 7;
	public static final int FIELDS = fields_list.length;
	public static final int FIELDS_NOID = fields_noID_list.length;
	private static final boolean DEBUG = false;
	public static ArrayList<Object> searchForToken(long peer_ID2, long instance_ID2, String dirAddr2, String port){
		String sql;
		String[]params;
		sql = "SELECT "+fields+
			  " FROM  "+TNAME+
		      " WHERE "+peer_ID+" =? " +
		      " AND   "+peer_instance_ID+" =? "+
			  " AND   "+directory_domain+" =? "+
		      " AND   "+directory_tcp_port+" =? "+";";
		params = new String[]{""+peer_ID2, ""+instance_ID2, dirAddr2, port};
		DBInterface db = Application.getDB();
		ArrayList<ArrayList<Object>> u;
		try {
			u = db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
	    if(u==null || u.size()==0) {
	    	if (DEBUG) System.out.println("TermsPanel: no tokens for entry peerID="+peer_ID2+":"+instance_ID2+" IP="+dirAddr2+":"+port+" in the DB table:"+TNAME);
	    	return null;
	    }
	    if (u.size() > 1){
	     System.out.println("multi tokens for the same enrty in the DB table:"+TNAME);
	     return null;
	    }
	    return u.get(0);
	}
}
