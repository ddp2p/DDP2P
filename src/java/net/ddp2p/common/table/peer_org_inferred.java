package net.ddp2p.common.table;
import java.util.regex.Pattern;
import net.ddp2p.common.util.Util;
public class peer_org_inferred {
	 public static final String peer_org_inferred_ID = "peer_org_inferred_ID";
	 public static final String peer_ID = "peer_ID";
	 public static final String organization_ID = "organization_ID";
	 public static final String TNAME = "peer_org_inferred";
	 public static final String ORG_NAME_SEP = "^"; // should not conflict with base64
	 public static final String ORG_SEP = ";"; // should not conflict with base64
	 public static final boolean DEBUG = false;	
	 public static final String fields_no_ID = 
			 peer_ID
			 + "," + organization_ID
			 ;
	 public static final String fields =
			 fields_no_ID
			 + "," + peer_org_inferred_ID;
	 public static final int COL_PEER_ID = 0;
	 public static final int COL_ORG_ID = 1;
	 public static final String[] fields_no_ID_list = Util.trimmed(fields_no_ID.split(Pattern.quote(",")));
	 public static final int COL_ID = fields_no_ID_list.length;
	 public static final String[] fields_list = Util.trimmed(fields.split(Pattern.quote(",")));
}
