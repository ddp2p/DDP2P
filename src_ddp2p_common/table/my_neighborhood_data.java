package table;

import java.util.regex.Pattern;

public class my_neighborhood_data {
	public static final String TNAME = "my_neighborhood_data";
	public static final String neighborhood_ID = "neighborhood_ID";
	public static final String name = "name";
	public static final String category = "category";
	public static final String row = "ROWID";
	
	public static final String fields_list_noID = 
			neighborhood_ID + ","
				+ name + ","
				+ category
				;
	public static final String fields_list = fields_list_noID + "," + row;
	public static final String[] fields_noID =  fields_list_noID.split(Pattern.quote(","));
	public static final String[] fields =  fields_list.split(Pattern.quote(","));
	public static final int FIELDS_NB_NOID = fields_noID.length;
	public static final int FIELDS_NB = fields.length;
	
	public static final int COL_NEIGHBORHOOD_LID = 0;
	public static final int COL_NAME = 1;
	public static final int COL_CATEGORY = 2;
	public static final int COL_ROW = 3;
}