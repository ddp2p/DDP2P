package table;

import java.util.regex.Pattern;

public class my_constituent_data {
	public static final String TNAME = "my_constituent_data";
	public static final String constituent_ID = "constituent_ID";
	public static final String name = "name";
	public static final String submitter = "submitter";
	public static final String category = "category";
	public static final String preferences_date = "preferences_date";
	public static final String row = "ROWID";
	
	public static final String fields_list_noID = 
				constituent_ID + ","
				+ name + ","
				+ submitter + ","
				+ category + ","
				+ preferences_date;
	public static final String fields_list = fields_list_noID + "," + row;
	public static final String[] fields_noID =  fields_list_noID.split(Pattern.quote(","));
	public static final String[] fields =  fields_list.split(Pattern.quote(","));
	public static final int FIELDS_NB_NOID = fields_noID.length;
	public static final int FIELDS_NB = fields.length;
	
	public static final int COL_CONSTITUENT_LID = 0;
	public static final int COL_NAME = 1;
	public static final int COL_SUBMITTER = 2;
	public static final int COL_CATEGORY = 3;
	public static final int COL_PREFERENCE_DATE = 4;
	public static final int COL_ROW = 5;
}