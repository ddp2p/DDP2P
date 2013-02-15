package handling_wb;

import java.util.ArrayList;

public class PreparedMessage{
	public byte[] raw;
	public String org_ID_hash;
	public ArrayList<String> constituent_ID_hash;
	public String motion_ID;
	public ArrayList<String> neighborhood_ID;
	public String justification_ID;
	boolean sent_flag = false;
	
	
}
