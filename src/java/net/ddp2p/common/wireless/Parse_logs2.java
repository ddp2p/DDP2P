package net.ddp2p.common.wireless;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.simulator.WirelessLog;
import net.ddp2p.common.util.Util;

public class Parse_logs2 {
	
	public static final String ROOT_PATH = "/home/osa/logs/static/";
	public static String FOLDER_PATH = "";
	public static final String FILE_SEP = "/";
	public static final String Plots = "plots";
	public static String Result_log = "";
	public static String Merge_file_name = "";
	public static ArrayList<String[]> m_arr = new ArrayList<String[]>();
	public static final String Queue_Indicator = "(***Queue***)";
	public static final String Rcv_Indicator = "(*Receiving*)";
	public static final String Bcast_Indicator = "(*Broadcast*)";
	private static final int TIME_POS_IN_BCAST = 1;
	private static final int COUNT_POS_IN_BCAST = 6;
	private static final long WINDOW_SIZE = 100;
	private static final int COUNT_POS_IN_RCV = 7;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public static ArrayList<String[]> TR_arr = new ArrayList<String[]>();
	public static int count_rcv = 0;
	public static float div = 0;
	public static long bcast_count = 0;
	public static Calendar time_count;
	public static Calendar time_limit;
	public static long rcv_count = 0;
	public static boolean flag=true;
	public static String plot_w_time = "time";
	public static String plot_w_counter = "counter";
	public static boolean plot_with_counter = false;
	public static boolean plot_with_time = false;
	public static boolean got_first_time = false;
	public static int Time_Window = 30; //30 seconds
	public static int check_sec=-1;
	public static int check_sec_rcv=-1;
	public static int old_count =0;
	
	
	/**
	 * will output a file that contain two columns either (Msg_counter or Time) 
	 * and (rcv_msg_count by time or count frame)
	 * @throws IOException
	 */
	private static void process() throws IOException {
		
		BufferedReader m_br = Parse_logs.Open_file_return_buffer(Merge_file_name);
		m_arr = Parse_logs.Buffer_to_arrlist(m_br,0);
		
		
		if(DEBUG)System.out.println("plot with counter : "+plot_with_counter);
		if(DEBUG)System.out.println("plot with time : "+plot_with_time);
		
		
		//create data file using counter frame.
		if(plot_with_counter) TR_arr = process_p_w_c(m_arr);

		//create data file using time frame, now is 30 seconds.
		if(plot_with_time) 
		{ 
			time_limit = get_first_time_limit(m_arr);
			if(DEBUG)System.out.println("start_time_limit : "+Encoder.getGeneralizedTime(time_limit));
			TR_arr = process_p_w_t(m_arr); 
		}
		
		TR_arr = m_arr;
		//create the result data file
		FileWriter writer = new FileWriter(Result_log); 
		PrintWriter pw = new PrintWriter(writer);
		ArrayList<String> merged = new ArrayList<String>();
		
		for(int i=0;i<TR_arr.size();i++)
			merged.add(Util.concat(TR_arr.get(i),WirelessLog.tab));
		for(String str : merged) {
		  pw.println(str);
		}
		pw.close();
	}
	
	/**
	 * return the first limit time of broadcasting
	 * @param m_arr2
	 * @return
	 */
	private static Calendar get_first_time_limit(ArrayList<String[]> m_arr2) {
		Calendar limit = null;
		for(int i=0;i<m_arr2.size();i++)
		{
			for(int j=0;j<m_arr2.get(i).length;j++){
				if(m_arr2.get(i)[j].contains(Bcast_Indicator)){
					limit = get_time_limit(m_arr2.get(i)[TIME_POS_IN_BCAST]);
					got_first_time = true;
					break;
				}
			}
			if(got_first_time) return limit;
		}
		return null;
	}


	/**
	 * process plot base on time info
	 * @param m_arr2
	 * @return
	 */
	private static ArrayList<String[]> process_p_w_t(ArrayList<String[]> m_arr2) {
		ArrayList<String[]> arr = new ArrayList<String[]>();
		
		for(int i=0;i<m_arr2.size();i++)
		{
			flag=true;
			for(int j=0;j<m_arr2.get(i).length;j++){

				if(m_arr2.get(i)[j].contains(Queue_Indicator)) { flag=false; break; }
				if(m_arr2.get(i)[j].contains(Bcast_Indicator)){
					bcast_count  = Integer.parseInt(m_arr2.get(i)[COUNT_POS_IN_BCAST]);
					if(DEBUG)System.out.println("bcast_count: "+bcast_count);
					time_count = get_time(m_arr2.get(i)[TIME_POS_IN_BCAST]);
					if(DEBUG)System.out.println("time_count: "+time_count);
					if(time_count.compareTo(time_limit)>=0) {
						time_limit = get_time_limit(m_arr2.get(i)[TIME_POS_IN_BCAST]);
						count_rcv=0;
						if(DEBUG)System.out.println("time_limit : "+Encoder.getGeneralizedTime(time_limit));
					} 	
				}
				if(m_arr2.get(i)[j].contains(Rcv_Indicator)){
					rcv_count  = Integer.parseInt(m_arr2.get(i)[COUNT_POS_IN_RCV]);
					count_rcv++;
				}
			}
			
			if(flag){
				if(bcast_count==rcv_count){
						arr.add(new String[]{time_count.getTimeInMillis()/1000+"",count_rcv+""});
						if(DEBUG)System.out.println("recv : "+count_rcv);
				}
				else {
					if(check_sec!=time_count.get(Calendar.SECOND)) 
						arr.add(new String[]{time_count.getTimeInMillis()/1000+"","0"});
				}
				check_sec = time_count.get(Calendar.SECOND);
			}
		}
		
		long last = Integer.parseInt(arr.get(0)[0]);
		System.out.println(last);
		for(int i=0;i<arr.size();i++){
			long x = Integer.parseInt(arr.get(i)[0]);
			long val = x-last;
			arr.get(i)[0] = val+"";
		}
	
		
		ArrayList<String[]> arr2 = new ArrayList<String[]>();
		int count_iter = 0;
		int x = -1;
		int z = -1;
		
		for(int i=0;i<arr.size();i++){
			count_iter=0;
			if(i!=0)
				if(Integer.parseInt(arr.get(i)[0])==x) continue;
			x = Integer.parseInt(arr.get(i)[0]);
			if(Integer.parseInt(arr.get(i)[1])>0) count_iter++;
			for(int j=i+1;j<arr.size();j++){
				z = Integer.parseInt(arr.get(j)[0]);
				if(x==z) {
					//System.out.println("x="+x+" z="+z);
					if(Integer.parseInt(arr.get(j)[1])>0) count_iter++;
					//arr2.add(new String[]{x+"",z+""});
				}
				//else System.out.println("x="+x+" z="+z);
			}
			//System.out.println("x="+x+" c="+count_iter);
			arr2.add(new String[]{x+"",count_iter+""});
		}
			
		return arr2;		
	}

	//Convert time from String to Calendar
	private static Calendar get_time(String time) {
		Calendar c = Util.getCalendar(time);
		//int min = Util.getCalendar(time).get(Calendar.MINUTE);
		return c;
	}

	// set the time limit by increasing the time with 30 seconds.
	private static Calendar get_time_limit(String time) {
		//int min = Util.getCalendar(time).get(Calendar.MINUTE);
		int sec = Util.getCalendar(time).get(Calendar.SECOND);
		sec = sec + Time_Window;
		Calendar c = Util.getCalendar(time);
		c.set(Calendar.SECOND, sec);
		return c;
	}

	/**
	 * process plot base on counter info
	 * @param m_arr2
	 * @return
	 */
	private static ArrayList<String[]> process_p_w_c(ArrayList<String[]> m_arr2) {
		ArrayList<String[]> arr = new ArrayList<String[]>();

		for(int i=0;i<m_arr2.size();i++)
		{
			flag=true;
			for(int j=0;j<m_arr2.get(i).length;j++){

				if(m_arr2.get(i)[j].contains(Queue_Indicator)) { flag=false; break; }
				if(m_arr2.get(i)[j].contains(Bcast_Indicator)){
					bcast_count  = Integer.parseInt(m_arr2.get(i)[COUNT_POS_IN_BCAST]);
					if(bcast_count%WINDOW_SIZE==0) { count_rcv=0; }
				}
				if(m_arr2.get(i)[j].contains(Rcv_Indicator)){
					rcv_count  = Integer.parseInt(m_arr2.get(i)[COUNT_POS_IN_RCV]);
					count_rcv++;
				}
			}
			if(flag)
				if(bcast_count==rcv_count){
					div = count_rcv / WINDOW_SIZE;
					arr.add(new String[]{bcast_count+"",div*100+""});
				}
				else arr.add(new String[]{bcast_count+"","0"});
		}
		return arr;
	}

	
	public static void main(String args[]) throws IOException{
		set_paths(args);
		Parse_logs2.process();
	}
	
	/**
	 * arg0 : experiment file name
	 * arg1 : merged file name
	 * arg2 : plot type : time / counter
	 * @param args
	 */
	private static void set_paths(String[] args) {
		FOLDER_PATH = ROOT_PATH + args[0] + FILE_SEP + Parse_logs.Result;
		Merge_file_name = FOLDER_PATH + args[1];
		if(args[2].equals(plot_w_time)) { 
			plot_with_time = true;
			Result_log = ROOT_PATH + args[0] + FILE_SEP + Plots + FILE_SEP + plot_w_time + "_" +args[1];
		}
		else if(args[2].equals(plot_w_counter)) {
			plot_with_counter = true;
			Result_log = ROOT_PATH + args[0] + FILE_SEP + Plots + FILE_SEP + plot_w_counter + "_" +args[1];
		}
	}

}
