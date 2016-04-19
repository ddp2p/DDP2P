package net.ddp2p.common.wireless;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
@Deprecated
public class Process_wlan_information {
	 public int wlan_number=0;
	@Deprecated
	 public ArrayList<String> process() throws IOException{
		ArrayList<String> w_info = new ArrayList<String>();
		ArrayList<String> w_info1 = new ArrayList<String>();
		String line;
	    String s="E:\\convert.exe";
	    Process p = Runtime.getRuntime().exec(s);
	    BufferedReader bri = new BufferedReader (new InputStreamReader(p.getInputStream()));
	    while ((line = bri.readLine()) != null) {
	    		w_info.add(line);
	      		}
	    bri.close();
	    int index1=0;
	    index1=w_info.get(0).indexOf(':');
	    index1++;
	    Character c=w_info.get(0).charAt(index1);
	    String s_char=c.toString();
	    int entries= Integer.parseInt(s_char);
	    wlan_number=entries;
	    int indx=0;
		int in=-1;
		char a[];
		String new_s;
		int new_indx=0;
		for(int i=1;i<w_info.size();i++){
		    indx=w_info.get(i).indexOf(':');
		    new_indx=w_info.get(i).length()-indx;
		    indx++;
		    a=new char[new_indx-1];
		    for(int j=indx;j<w_info.get(i).length();j++){
		    	in++;
		    	a[in]=w_info.get(i).charAt(j);		    		
		    	}
		    new_s =new String(a);
		    w_info.set(i,new_s);
		    indx=0;
		    in=-1;
		    new_s=null;
		    new_indx=0;
		}
		int i=0;
	    while(i<entries){
	    	for(int j=1;j<w_info.size();j=j+5){
	    		w_info1.add(w_info.get(j).substring(1,w_info.get(j).length()));
	    		w_info1.add(w_info.get(j+1));
		    	w_info1.add(w_info.get(j+2));
		    	w_info1.add(w_info.get(j+3));
		    	w_info1.add(w_info.get(j+4));
		    	i=i+1;
	    	}
	    }
		return w_info1;
	}
	 public int get_wlan_num()	{ return wlan_number; }
}
