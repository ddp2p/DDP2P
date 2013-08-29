package table;

import java.util.regex.Pattern;

public class org_distribution{
	 public static final String TNAME = "org_distribution";
	 public static final String peer_distribution_ID = "peer_distribution_ID";
	 public static final String organization_ID = "organization_ID";
	 public static final String peer_ID = "peer_ID";
	 public static final String fields_noID = //util.Util.trimInterFieldSpaces()
			organization_ID+","+
			peer_ID;
	 public static final String fields = fields_noID+","+peer_distribution_ID;
	 public static final String[] fields_list = fields.split(Pattern.quote(","));
	 //public static final String od_list = " "+util.Util.trimInterFieldSpaces(fields)+" ";
	 public static final int OD_ORG_ID = 0;
	 public static final int OD_PEER_ID = 1;
	 public static final int OD_ID = 2;
	 public static final int OD_FIELDS = fields_list.length;
}