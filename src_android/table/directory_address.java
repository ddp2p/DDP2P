package table;

import java.util.regex.Pattern;
public class directory_address {
	public static final String directory_address_ID = "directory_address_ID";
	public static final String GID = "GID";
	public static final String GIDH = "GIDH";
	public static final String instance = "instance";
	public static final String protocol = "protocol";
	public static final String branch = "branch";
	public static final String version = "version";
	public static final String agent_version = "agent_version";
	public static final String domain = "domain";
	public static final String tcp_port = "tcp_port";
	public static final String udp_port = "udp_port";
	public static final String name = "name"; // 1 if it is digitally signed (in order of priority)
	public static final String comments = "comments"; // 1 if it is digitally signed (in order of priority)
	public static final String signature = "signature"; // 1 if it is digitally signed (in order of priority)
	public static final String active = "active"; // in ascending order, tells when to test an address (apriori)
	public static final String revokation = "revokation"; // in ascending order, tells when to test an address (apriori)
	public static final String date_last_connection = "date_last_connection";
	public static final String date_signature = "date_signature";
	public static final String TNAME = "directory_address";
	
	public static final String fields_noID =
			agent_version+","+
			protocol+","+
					branch+","+
					domain+","+
					version+","+
			tcp_port+","+
			udp_port+","+
			GID+","+
			GIDH+","+
			instance+","+
			name+","+
			comments+","+
			signature+","+
			active+","+
			revokation+","+
			date_last_connection+","+
			date_signature;
	public static final String fields =
			fields_noID+","+directory_address_ID;
	public static final String[] fields_list = fields.split(Pattern.quote(","));
	public static final String[] fields_noID_list = fields_noID.split(Pattern.quote(","));
	public static final int DA_AGENT_VERSION = 0;
	public static final int DA_PROTOCOL = 1;
	public static final int DA_BRANCH = 2;
	public static final int DA_DOMAIN = 3;
	public static final int DA_VERSION = 4;
	public static final int DA_TCP_PORT = 5;
	public static final int DA_UDP_PORT = 6;
	public static final int DA_GID = 7;
	public static final int DA_GIDH = 8;
	public static final int DA_INSTANCE = 9;
	public static final int DA_NAME = 10;
	public static final int DA_COMMENTS = 11;
	public static final int DA_SIGN = 12;
	public static final int DA_ACTIVE = 13;
	public static final int DA_REVOKATION = 14;
	public static final int DA_CONTACT = 15;
	public static final int DA_DATE_SIGNATURE = 16;
	public static final int DA_DIRECTORY_ADDR_ID = 17;
	public static final int FIELDS = fields_list.length;
	public static final int FIELDS_NOID = fields_noID_list.length;
}
