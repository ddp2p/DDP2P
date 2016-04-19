package net.ddp2p.common.table;
import java.util.regex.Pattern;
public class peer_instance {
	public static final String peer_instance_ID = "peer_instance_ID";
	public static final String peer_ID = "peer_ID";
	public static final String peer_instance = "peer_instance";
	public static final String branch = "branch";
	public static final String agent_version = "version";
	public static final String plugin_info = "plugin_info";
	public static final String last_sync_date = "last_sync_date";
	public static final String last_reset = "last_reset";
	public static final String last_contact_date = "last_contact_date";
	public static final String objects_synchronized = "objects_synchronized";
	public static final String signature_date = "signature_date"; // not used ? not needed
	public static final String signature = "signature"; // not used ? not needed
	public static final String created_locally = "created_locally";
	public static final String TNAME = "peer_instance";
	public static final String fields_noID =
			net.ddp2p.common.table.peer_instance.peer_ID
			 +","+net.ddp2p.common.table.peer_instance.peer_instance
			 +","+net.ddp2p.common.table.peer_instance.branch
			 +","+net.ddp2p.common.table.peer_instance.agent_version
			 +","+net.ddp2p.common.table.peer_instance.plugin_info
			 +","+net.ddp2p.common.table.peer_instance.last_sync_date
			 +","+net.ddp2p.common.table.peer_instance.objects_synchronized
			 +","+net.ddp2p.common.table.peer_instance.last_reset
			 +","+net.ddp2p.common.table.peer_instance.last_contact_date
			 +","+net.ddp2p.common.table.peer_instance.created_locally
			 +","+net.ddp2p.common.table.peer_instance.signature_date
			 +","+net.ddp2p.common.table.peer_instance.signature
			 ;
	public static final String fields=fields_noID+","+peer_instance_ID;
	public static final String[]fields_noID_list = fields_noID.split(Pattern.quote(","));
	public static final String[]fields_list = fields.split(Pattern.quote(","));
	public static final int PI_PEER_ID = 0;
	public static final int PI_PEER_INSTANCE = 1;
	public static final int PI_PEER_BRANCH = 2;
	public static final int PI_PEER_AGENT_VERSION = 3;
	public static final int PI_PLUGIN_INFO = 4;
	public static final int PI_LAST_SYNC_DATE = 5;
	public static final int PI_OBJECTS_SYNCH = 6;
	public static final int PI_LAST_RESET = 7;
	public static final int PI_LAST_CONTACT_DATE = 8;
	public static final int PI_CREATED_LOCALLY = 9;
	public static final int PI_SIGNATURE_DATE = 10;
	public static final int PI_SIGNATURE = 11;
	public static final int PI_PEER_INSTANCE_ID = 12;
	public static final int FIELDS_NOID = fields_noID.split(",").length;
	public static final int FIELDS = fields.split(",").length;	
}
