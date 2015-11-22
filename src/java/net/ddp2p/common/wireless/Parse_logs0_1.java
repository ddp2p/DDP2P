package net.ddp2p.common.wireless;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import net.ddp2p.common.simulator.WirelessLog;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class Parse_logs0_1 {
	public static String FOLDER_PATH = "";
	public static final String ROOT_PATH = "/home/osa/logs/static/";
	public static final String Result = "results/";
	public static final String FILE_SEP = "/";
	public static String SEND_NAME = "";
	public static String RCV_NAME = "";
	public static String SEND_PATH;
	public static String RCV_PATH;
	public static final String SEND_LOG = "_send.log";
	public static final String RCV_LOG = "_rcv.log";
	public static final String DB = ".db";
	public static String SEND_LOG_PATH = "";
	public static String SEND_DB_PATH = "";
	public static String RCV_LOG_PATH = "";
	public static String RCV_DB_PATH = "";
	private static final int INPUT_LOGS_NUMBER = 4;
	private static final int SENDER_LOG = 0;
	private static final int SENDER_DB = 1;
	private static final int RECEIVER_LOG = 2;
	private static final int RECEIVER_DB = 3;
	private static final int UNUSED_LINES_SEND = 4;
	private static final int UNUSED_LINES_RCV = 3;
	public static int zero_Pos = 0;
	public static int Send_Time_Pos = 1;
	public static int Send_counter_Pos = 6;
	public static int Rcv_counter_Pos = 7;
	public static int Rcv_MsgHash_Pos = 4;
	public static int Rcv_peerHash_Pos = 3;
	public static int Send_MsgHash_Pos = 4;
	public static ArrayList<String[]> send_arr = new ArrayList<String[]>();
	public static ArrayList<String[]> rcv_arr = new ArrayList<String[]>();
	public static ArrayList<String[]> result_arr = new ArrayList<String[]>();
	public static String SET_LOGS_DBs_PATH = "/home/osa/logs/";
	public static String SET_RESULT_PATH = null;
	public static DBInterface send_db;
	public static DBInterface rcv_db;
	/** Process_logs()
	 * 
	 * @throws IOException
	 * @throws SQLiteException 
	 */
	public static void process_logs() throws IOException {
		BufferedReader send_br = Open_file_return_buffer(SEND_LOG_PATH);
		BufferedReader rcv_br = Open_file_return_buffer(RCV_LOG_PATH);
		send_arr = Buffer_to_arrlist(send_br,UNUSED_LINES_SEND);
		rcv_arr = Buffer_to_arrlist(rcv_br,UNUSED_LINES_RCV);
		for(int i=0;i<rcv_arr.size();i++)
			for(int j=0;j<rcv_arr.get(i).length;j++)
				rcv_arr.get(i)[Rcv_peerHash_Pos]=null;
		/**
		 *  convert Array list of send_arr to Hash table 
		 * 	key:counter
		 * 	value:all_info 
		 */
		Hashtable<String, String> rcv_ht = new Hashtable<String, String>();
		for(int i=0;i<rcv_arr.size();i++) {
			rcv_ht.put(rcv_arr.get(i)[Rcv_counter_Pos], Util.concat(rcv_arr.get(i),WirelessLog.tab));
		}
		for(int i=0;i<send_arr.size();i++){
			if(send_arr.get(i)[zero_Pos].contains("(***Queue***)")) continue;
			if(rcv_ht.containsKey(send_arr.get(i)[Send_counter_Pos])){
				String rcv_info = rcv_ht.get(send_arr.get(i)[Send_counter_Pos]);
				String[] rcv_segments = rcv_info.split(WirelessLog.tab);
				send_arr.add(i+1,rcv_segments );
			}
		}
		SET_RESULT_PATH = get_result_path();
		System.out.println(SET_RESULT_PATH);
		FileWriter writer = new FileWriter(SET_RESULT_PATH); 
		PrintWriter pw = new PrintWriter(writer);
		ArrayList<String> merged = new ArrayList<String>();
		for(int i=0;i<send_arr.size();i++)
			merged.add(Util.concat(send_arr.get(i),WirelessLog.tab));
		for(String str : merged) {
		  pw.println(str);
		}
		pw.close();
	}
	private static String get_result_path() {
		String result = FOLDER_PATH + Result + SEND_NAME + "_send_"+ RCV_NAME +
						"_rcv.log";
		return result;
	}
	/** Calculate_Statistics for result (merged file)
	 * 
	 * @throws IOException
	 * @throws SQLiteException 
	 * @throws P2PDDSQLException 
	 */
	public static void Calculate_Statistics() throws IOException, P2PDDSQLException{
		send_db = new DBInterface(SEND_DB_PATH);
		rcv_db = new DBInterface(RCV_DB_PATH);
		BufferedReader result_br = Open_file_return_buffer(SET_RESULT_PATH);
		result_arr = Buffer_to_arrlist(result_br,0);
	}
	/**
	 * Open_file_return_buffer
	 * @param FilePath
	 * @return
	 * @throws FileNotFoundException
	 */
	public static BufferedReader Open_file_return_buffer(String FilePath) throws FileNotFoundException{
		FileReader file = new FileReader(FilePath);
		BufferedReader br = new BufferedReader(file);
		return br;
	}
	/** Buffer to array list of String[]
	 * 
	 * @param br
	 * @return
	 * @throws IOException 
	 */
	public static ArrayList<String[]> Buffer_to_arrlist(BufferedReader br,int unused_lines) throws IOException{
		ArrayList<String[]> arr = new ArrayList<String[]>();
		String line;
		int count = 0;
		while((line = br.readLine()) != null){
			if(unused_lines!=0){
				count++;
				if(count<=unused_lines) continue;  
			}
			if(!line.contains(Parse_logs2.Queue_Indicator)) 
			arr.add(line.split(WirelessLog.tab)); 
		}
		br.close();
		return arr;
	}
	public static void main(String args[]) throws IOException {
		set_paths(args);
		Parse_logs0_1.process_logs();
	}
	/**
	 * arg0 : experiment
	 * arg1 : send file name
	 * arg2 : receive file name
	 * @param args
	 */
	private static void set_paths(String[] args) {
		if(args.length!=3) System.out.println("Error : number of args should be 3");
		FOLDER_PATH = ROOT_PATH + args[0] + FILE_SEP;
		SEND_NAME = args[1];
		RCV_NAME = args[2];
		SEND_PATH = FOLDER_PATH + SEND_NAME + FILE_SEP;
		RCV_PATH = FOLDER_PATH + RCV_NAME + FILE_SEP;
		SEND_LOG_PATH = SEND_PATH + SEND_NAME + SEND_LOG;
		SEND_DB_PATH = SEND_PATH + SEND_NAME + DB; 
		RCV_LOG_PATH = RCV_PATH + RCV_NAME + RCV_LOG;
		RCV_DB_PATH = RCV_PATH + RCV_NAME + DB; 
	}
}
