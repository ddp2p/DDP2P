package table;

import java.util.regex.Pattern;

public class registered{

	public static final String TNAME = "registered";
	public static final String global_peer_ID = "global_peer_ID";
	public static final String global_peer_ID_hash = "global_peer_ID_hash";
	public static final String instance = "instance";
	public static final String certificate = "certificate";
	public static final String addresses = "addresses";
	public static final String signature = "signature";
	public static final String timestamp = "timestamp";
	public static final String registeredID = "registeredID";
	
	public static final String fields_noID = 
			global_peer_ID+
			","+global_peer_ID_hash+
			","+instance+
			","+certificate+
			","+addresses+
			","+signature+
			","+timestamp;
	
	public static final String[] fields_noID_list = fields_noID.split(Pattern.quote(","));
	public static final String fields =  fields_noID+","+registeredID;
	public static final String[] fields_list = fields.split(Pattern.quote(","));
	public static final int REG_GID = 0;
	public static final int REG_GID_HASH = 1;
	public static final int REG_INSTANCE = 2;
	public static final int REG_CERT = 3;
	public static final int REG_ADDR = 4;
	public static final int REG_SIGN = 5;
	public static final int REG_TIME = 6;
	public static final int REG_ID = 7;
}