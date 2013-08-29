package table;

import java.util.regex.Pattern;

public class peer_instance {
	public static final String peer_instance_ID = "peer_instance_ID";
	public static final String peer_ID = "peer_ID";
	public static final String peer_instance = "peer_instance";
	public static final String plugin_info = "plugin_info";
	public static final String last_sync_date = "last_sync_date";
	public static final String last_reset = "last_reset";
	public static final String last_contact_date = "last_contact_date";
	public static final String TNAME = "peer_instance";
	public static final String fields_noID=
			table.peer_instance.peer_ID +","+
			table.peer_instance.peer_instance +","+
			table.peer_instance.plugin_info +","+
			table.peer_instance.last_sync_date +","+
			table.peer_instance.last_reset +","+
			table.peer_instance.last_contact_date;
	public static final String fields=fields_noID+","+peer_instance_ID;
	public static final String[]fields_noID_list = fields_noID.split(Pattern.quote(","));
	public static final String[]fields_list = fields.split(Pattern.quote(","));
	public static final int PI_PEER_ID = 0;
	public static final int PI_PEER_INSTANCE = 1;
	public static final int PI_PLUGIN_INFO = 2;
	public static final int PI_LAST_SYNC_DATE = 3;
	public static final int PI_LAST_RESET = 4;
	public static final int PI_LAST_CONTACT_DATE = 5;
	public static final int PI_PEER_INSTANCE_ID = 6;
	public static final int FIELDS_NOID = fields_noID.split(",").length;
	public static final int FIELDS = fields.split(",").length;	
}
