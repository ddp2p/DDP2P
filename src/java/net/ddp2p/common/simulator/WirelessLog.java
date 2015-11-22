package net.ddp2p.common.simulator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Message;
import net.ddp2p.common.util.DDP2P_ServiceThread;
import net.ddp2p.common.util.Util;
public class WirelessLog {
    public static File f1;
    public static FileWriter fw1;
    public static PrintWriter pw1;
    public static File f2;
    public static FileWriter fw2;
    public static PrintWriter pw2;
    public static File f3;
    public static FileWriter fw3;
    public static PrintWriter pw3;
	public static final DateFormat dateFormat = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
	public static final Date date = new Date();
	public static final String Log_Time = (dateFormat.format(date))+"\n";
	public static final String Log_Queue_info 		= "#***Queue_Fields***#	Queue_Name	Queue_idx	Type	Hash_Size\n";
	public static final String Log_Broadcast_info 	= "#*Broadcast_Fields*#	Time	Queue_Name	Queue_idx	Type	Msg_counter	Msg_Size\n";
	public static final String Log_Info = Log_Time+Log_Queue_info+Log_Broadcast_info;
	public static final String RCV_Info				= "#*Receiving_Fields*#	DB_store_Time	Type	Peer_GID_Hash	Hash_Msg	New?	Received_IP	Msg_counter	Rcv_Msg_Time\n";
	public static final String RCV_Log_Info = Log_Time+RCV_Info;
	public static final String RCV_log = "(*Receiving*)	";
	public static final String log_queue = "(***Queue***)	";
	public static final String log_broadcast = "(*Broadcast*)	";
	public static final String Circular_queue = "Circular";
	public static final String Handled_queue = " Handled";
	public static final String MyData_queue = " My_Data";
	public static final String Recent_queue = "  Recent";
	public static final String org_type = "org";
	public static final String const_type = "const";
	public static final String neigh_type = "neigh";
	public static final String vote_type = "vote";
	public static final String wit_type = "witness";
	public static final String peer_type = "peer";
	public static final String tab = "\t";
	public static final String nl = "\n";
	public static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public static long counter_for_circular = -1;
	public static long counter_for_handled = -1;
	public static long counter_for_mydata = -1;
	public static long counter_for_recent = -1;
	public static long current_circular_idx = 0;
	public static long current_handled_idx = 0;
	public static long current_recent_idx = 0;
	public static long current_mydata_idx = 0;
	public static synchronized void Print_to_log(String val){
		if(WirelessLog.pw1!=null) WirelessLog.pw1.println(val);
	}
	public static synchronized void Print_to_RCV_log(String val){
		if(WirelessLog.pw2!=null) WirelessLog.pw2.println(val);
	}
	public static synchronized void Print_to_BS_log(String val){
		if(WirelessLog.pw3!=null) WirelessLog.pw3.println(val);
	}
public static String inc_c_counter(){
		WirelessLog.counter_for_circular++;
		return ""+WirelessLog.counter_for_circular;
	}
	public static String inc_h_counter(){
		WirelessLog.counter_for_handled++;
		return ""+WirelessLog.counter_for_handled;
	}
	public static String inc_md_counter(){
		WirelessLog.counter_for_mydata++;
		return ""+WirelessLog.counter_for_mydata;
	}
	public static String inc_r_counter(){
		WirelessLog.counter_for_recent++;
		return ""+WirelessLog.counter_for_recent;
	}
	public static void check_which_queue(String queue_type){
		ArrayList<String> Queue_Names=new ArrayList<String>();
		Queue_Names.add(WirelessLog.Circular_queue);
		Queue_Names.add(WirelessLog.Recent_queue);
		Queue_Names.add(WirelessLog.MyData_queue);
		Queue_Names.add(WirelessLog.Handled_queue);
		int ch=0;
		if(queue_type.compareTo(Queue_Names.get(0))==0) ch=1;
		else if(queue_type.compareTo(Queue_Names.get(1))==0) ch=2;
		else if(queue_type.compareTo(Queue_Names.get(2))==0) ch=3;
		else if(queue_type.compareTo(Queue_Names.get(3))==0) ch=4;
		switch(ch){
		case 1 : 
			WirelessLog.counter_for_circular = -1;
			break;
		case 2 :
			WirelessLog.counter_for_recent = -1;
			break;
		case 3 : 
			WirelessLog.counter_for_mydata = -1;
			break;
		case 4 : 
			WirelessLog.counter_for_handled = -1;
			break;
		}
	}
	public static void check_which_queue_idx(String queue_type, long idx){
		ArrayList<String> Queue_Names=new ArrayList<String>();
		Queue_Names.add(WirelessLog.Circular_queue);
		Queue_Names.add(WirelessLog.Recent_queue);
		Queue_Names.add(WirelessLog.MyData_queue);
		Queue_Names.add(WirelessLog.Handled_queue);
		int ch=0;
		if(queue_type.compareTo(Queue_Names.get(0))==0) ch=1;
		else if(queue_type.compareTo(Queue_Names.get(1))==0) ch=2;
		else if(queue_type.compareTo(Queue_Names.get(2))==0) ch=3;
		else if(queue_type.compareTo(Queue_Names.get(3))==0) ch=4;
		switch(ch){
		case 1 : 
			WirelessLog.current_circular_idx = idx;
			break;
		case 2 :
			WirelessLog.current_recent_idx = idx;
			break;
		case 3 : 
			WirelessLog.current_mydata_idx = idx;
			break;
		case 4 : 
			WirelessLog.current_handled_idx = idx;
			break;
		}
	}
	public static void logging(String logQueue, String Queue_Name, String Queue_idx, String Type, byte[] msg){
		String hash_msg = Util.stringSignatureFromByte(Util.simple_hash(msg, DD.APP_INSECURE_HASH));
		String log = logQueue+
				Queue_Name+
				WirelessLog.tab+
				Queue_idx+
				WirelessLog.tab+
				Type+
				WirelessLog.tab+
				hash_msg;
				WirelessLog.Print_to_log(log);
	}
	public static void RCV_logging(String Type, String Peer_GID, byte[] data,
									int length, int status,String IP,long counter, String Msg_time){
		String _status = null;
		switch(status){
		case 0:
			_status = "New";
			break;
		case 1:
			_status = "Present&Signed";
			break;
		case -1:
			_status = "Temporary";
			break;
		default : 
			_status = "?:"+status;
			break;
		}
		String Peer_GID_hash = Util.getGIDhash(Peer_GID);
		String hash_msg = "";//Util.stringSignatureFromByte(Util.simple_hash(data,0,length, DD.APP_INSECURE_HASH));
		 String Time = Util.getGeneralizedTime();
		 String log = RCV_log+
				 	Time+
				 	WirelessLog.tab+
				 	Type+
				 	WirelessLog.tab+
				 	Peer_GID_hash+
				 	WirelessLog.tab+
				 	hash_msg+
				 	WirelessLog.tab+
				 	_status+
				 	WirelessLog.tab+
				 	IP+
				 	WirelessLog.tab+
				 	counter+
				 	WirelessLog.tab+
				 	Msg_time
				 	;
		 WirelessLog.Print_to_RCV_log(log);
	}
	public static void init() throws IOException {
		if (DEBUG) System.out.println("WirelessLog:init");
		String t = Util.getGeneralizedTime();
        File dir = new File(Application.CURRENT_LOGS_BASE_DIR()); 
        if (!dir.mkdirs() && (!dir.exists() || !dir.isDirectory())) {
        	System.err.println("WirelessLog: init: failure to create folder: "+dir);
        	return;
        }
        WirelessLog.f1 = new File(Application.CURRENT_LOGS_BASE_DIR()+t+".log");
        if((!f1.exists() && !dir.canWrite()) || (f1.exists() && !f1.canWrite())) {
        	System.err.println("WirelessLog: init: cannot create/modify: "+f1);
        	return;
        }
        WirelessLog.fw1 = new FileWriter(f1);
        WirelessLog.pw1 = new PrintWriter(WirelessLog.fw1);
        WirelessLog.Print_to_log(WirelessLog.Log_Info);
        WirelessLog.f2  = new File(Application.CURRENT_LOGS_BASE_DIR()+"RECEIVING_LOG_"+t+".log");
        WirelessLog.fw2 = new FileWriter(f2);
        WirelessLog.pw2 = new PrintWriter(WirelessLog.fw2);
        WirelessLog.Print_to_RCV_log(WirelessLog.RCV_Log_Info);
        WirelessLog.f3 = new File(Application.CURRENT_LOGS_BASE_DIR()+"BS_LOG"+t+".log");
        WirelessLog.fw3 = new FileWriter(f3);
        WirelessLog.pw3 = new PrintWriter(WirelessLog.fw3);
        try {
            Runtime.getRuntime().addShutdownHook(
            new DDP2P_ServiceThread("WirelessLog", true) {
                public void _run(){
                    WirelessLog.pw1.close();
                    WirelessLog.pw2.close();
                    WirelessLog.pw3.close();
                 }
            } );
        }
        catch( Throwable t1 )
        {
        }
	}
	public static String getMsgHash(byte[] msg){
		return Util.stringSignatureFromByte(Util.simple_hash(msg,0,msg.length, DD.APP_INSECURE_HASH));
	}
	public static void Print_to_log_Param(String logQueue, String queue_type,
			String idx, String subqueue, byte[] result, long msg_c) {
		String log = logQueue+
		Util.getGeneralizedTime()+
		WirelessLog.tab+
		queue_type+
		WirelessLog.tab+
		idx+
		WirelessLog.tab+
		subqueue+
		WirelessLog.tab+
		result.length+
		WirelessLog.tab+
		msg_c;
		if(queue_type == WirelessLog.Handled_queue){
			log += (WirelessLog.tab+getMsgHash(result));}
		WirelessLog.Print_to_log(log);
	}
}
