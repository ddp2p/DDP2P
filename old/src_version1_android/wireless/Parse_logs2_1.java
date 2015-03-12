package wireless;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import ASN1.Encoder;

import simulator.WirelessLog;
import util.Util;

public class Parse_logs2_1 {
	
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
		if(plot_with_time) { 
			TR_arr = process_p_w_t(m_arr); 
		}
		
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
		ArrayList<String[]> arr1 = new ArrayList<String[]>();

		String time = null;
		
		for(int i=0;i<m_arr2.size();i++)
		{
			for(int j=0;j<m_arr2.get(i).length;j++){
				if(m_arr2.get(i)[j].contains(Bcast_Indicator)){
					bcast_count  = Integer.parseInt(m_arr2.get(i)[COUNT_POS_IN_BCAST]);
					if(DEBUG)System.out.println("bcast_count : "+bcast_count);
					time = m_arr2.get(i)[TIME_POS_IN_BCAST];
					//time = time.substring(0,14);
					
					//int min = Util.getCalendar(time).get(Calendar.MINUTE);
					//int sec = Util.getCalendar(time).get(Calendar.SECOND);
					//time = time.substring(10,14);				
					//arr.add(new String[]{time});
				}
				
				if(m_arr2.get(i)[j].contains(Rcv_Indicator)){
					rcv_count  = Integer.parseInt(m_arr2.get(i)[COUNT_POS_IN_RCV]);
					if(DEBUG)System.out.println("rcv_count : "+rcv_count);
					count_rcv++;
				}
			}
			
			if(bcast_count==rcv_count){
				if(DEBUG)System.out.println("bcast_count : "+bcast_count+" rcv_count : "+rcv_count);
				arr.add(new String[]{time,count_rcv+""});
			}
			else { 
					arr.add(new String[]{time,"0"});
			}
		}
		
		Calendar c = Util.getCalendar(arr.get(0)[0]);
		//System.out.println("c: "+Encoder.getGeneralizedTime(c));
		int min = c.get(Calendar.MINUTE);
		int sec = c.get(Calendar.SECOND)-1;
		min *= -1;
		sec *= -1;
		//System.out.println("first="+arr.get(0)[0]);
		for(int i=0;i<arr.size();i++){
			Calendar x = Util.getCalendar(arr.get(i)[0]);
			x.add(Calendar.MINUTE, min);
			x.add(Calendar.SECOND, sec);
			String z = Encoder.getGeneralizedTime(x).substring(8, 14);
			//z = z.substring(0, 2)+":"+z.substring(2,z.length());
			arr.get(i)[0] = z;
		}
		
		
		
		
		float sum = 0.f;
		int sum_c = -1;
		float secs = 0.f;
		float sum_ave = 0.f;
		
		ArrayList<String[]> arr2 = new ArrayList<String[]>();
		int count_iter = 0;
		long x = -1;
		long z = -1;

		for(int i=0;i<arr.size();i++){
			count_iter=0;
			if(i!=0)
				if(Long.parseLong(arr.get(i)[0])==x) continue;
			x = Long.parseLong(arr.get(i)[0]);
			if(Long.parseLong(arr.get(i)[1])>0) count_iter++;
			for(int j=i+1;j<arr.size();j++){
				z = Long.parseLong(arr.get(j)[0]);
				if(x==z) {
					if(Long.parseLong(arr.get(j)[1])>0) count_iter++;
				}
			}
			secs = secs+1;
			sum = sum +count_iter;
			sum_ave = sum/secs;
			 
			arr2.add(new String[]{arr.get(i)[0]+"",count_iter+"",sum+"",secs+"",sum_ave+""});
			//arr2.add(new String[]{arr.get(i)[0]+"",sum_ave+""});
			if(secs==30) { secs = 29;}
		}
		/*
		for(int i=0;i<arr2.size();i++){
			String val = arr2.get(i)[0];
			val = val.substring(0, 2)+":"+val.substring(2,val.length());
			arr2.get(i)[0] = val;
		}
*/
		//System.out.println("size:"+arr2.size());
		ArrayList<String[]> arr3 = new ArrayList<String[]>();
		float val;
		float avg=0;
		
		for(int i=0;i<arr2.size();i++){
			if(i>=30){
				val = Float.parseFloat(arr2.get(i)[2]) - Float.parseFloat(arr2.get(i-30)[2]);
				//if(i<100)
				//System.out.println(i+" : "+arr2.get(i)[2]+" - "+Float.parseFloat(arr2.get(i-30)[2])+"= "+val);
				avg = val/Float.parseFloat(arr2.get(i)[3]);
				arr3.add(new String[]{ arr2.get(i)[0],avg+""});
				//arr3.add(new String[]{ arr2.get(i)[0],arr2.get(i)[1],val+"",arr2.get(i)[3],avg+""});
			}
			
			else {
				avg = Float.parseFloat(arr2.get(i)[2])/Float.parseFloat(arr2.get(i)[3]);
				arr3.add(new String[]{arr2.get(i)[0],avg+""});
				//arr3.add(new String[]{ arr2.get(i)[0],arr2.get(i)[1],arr2.get(i)[2],arr2.get(i)[3],avg+""});
			}
		}
		
		/*
		int count = 0;
		int x = Integer.parseInt(arr.get(0)[0]);
		arr1.add(new String[]{arr.get(0)[0],count+""});
		
		for(int i=1;i<arr.size();i++){
			int z = Integer.parseInt(arr.get(i)[0]);
			if(x==z) arr1.add(new String[]{arr.get(i)[0],count+""});
			else if(z>x) {
				count = count+1;
				arr1.add(new String[]{arr.get(i)[0],count+""});}
			x=z;
		}
			*/
		

		return arr3;		
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
		Parse_logs2_1.process();
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
