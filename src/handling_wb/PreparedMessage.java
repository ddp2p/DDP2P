package handling_wb;

import java.util.ArrayList;

public class PreparedMessage{
	public byte[] raw;
	public String org_ID_hash = new String();
	public ArrayList<String> constituent_ID_hash = new ArrayList<String>();
	public String motion_ID= new String();;
	public ArrayList<String> neighborhood_ID = new ArrayList<String>();
	public String justification_ID= new String();;
	boolean sent_flag = false;
	
	
}
