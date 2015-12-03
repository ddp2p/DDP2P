package net.ddp2p.simulator;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
public class InterfaceSupplier {
	static ArrayList<String> ready_list = new ArrayList<String>();  
	static ArrayList<String> busy_list = new ArrayList<String>();
	/**
	 * Loads a list of IPs for the alias interfaces from a file into ready_list
	 * 
	 * @param interface_list
	 * Path to interface_list file created by bash script.
	 * 
	 */
	public static void init(String interface_list) {
	    BufferedReader br = null;
	    try {
	        String sCurrentLine;
	        br = new BufferedReader(new FileReader(interface_list));
	        while ((sCurrentLine = br.readLine()) != null) {
	        	String[] arr = sCurrentLine.split(" ");
	        	ready_list.add(arr[1]);
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            if (br != null) br.close();
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	    }
	}
	/**
	 * Moves the first element in ready_list to busy_list
	 * 
	 * @return first element in ready_list or null
	 */
	public static String getVacant() {
		if (ready_list.size() == 0)
			return null;
		String response = ready_list.remove(0);
		busy_list.add(response);
		return response;
	}
}
