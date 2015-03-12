package wireless;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;

import ASN1.Encoder;

import simulator.WirelessLog;
import util.Util;

public class Parse_buf_log {

	public static void process(String sfile) throws IOException{
	FileReader file = new FileReader(sfile);
	BufferedReader br = new BufferedReader(file);
	
	ArrayList<String[]> arr = new ArrayList<String[]>();
	ArrayList<String[]> arr2 = new ArrayList<String[]>();
	String line;
	while((line = br.readLine()) != null){
		arr.add(line.split(WirelessLog.tab)); //for each line in log: split it base on tap.
	}
	br.close();
	
	for(int i=0;i<arr.size();i++){
		arr2.add(new String[]{arr.get(i)[0],arr.get(i)[1]});
	}
	
	Calendar c = Util.getCalendar(arr2.get(0)[0]);
	//System.out.println("c: "+Encoder.getGeneralizedTime(c));
	int min = c.get(Calendar.MINUTE);
	int sec = c.get(Calendar.SECOND)-1;
	min *= -1;
	sec *= -1;
	//System.out.println("first="+arr.get(0)[0]);
	for(int i=0;i<arr2.size();i++){
		Calendar x = Util.getCalendar(arr2.get(i)[0]);
		x.add(Calendar.MINUTE, min);
		x.add(Calendar.SECOND, sec);
		String z = Encoder.getGeneralizedTime(x).substring(8, 14);
		//z = z.substring(0, 2)+":"+z.substring(2,z.length());
		arr2.get(i)[0] = z;
	}
	
	//create the result data file
			FileWriter writer = new FileWriter("result.log"); 
			PrintWriter pw = new PrintWriter(writer);
			ArrayList<String> merged = new ArrayList<String>();
	
	for(int i=0;i<arr2.size();i++)
		merged.add(Util.concat(arr2.get(i),WirelessLog.tab));
	for(String str : merged) {
	  pw.println(str);
	}
	pw.close();
	}
	
	public static void main(String args[]) throws IOException{
		String file = args[0];
		Parse_buf_log.process(file);
	}
	
}
