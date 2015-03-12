/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package table;

public class neighborhood {
	public static final String neighborhood_ID = "neighborhood_ID";
	/**
	 * Probably best GID=HASH(parentGID,name)
	 * With each subscriber/vote-on-neighborhood
	 *   generate a vote on the other parameters, like for translations (use the most voted one)
	 */
	public static final String global_neighborhood_ID = "global_neighborhood_ID";
	public static final String address = "address";
	public static final String description = "description";
	public static final String parent_nID = "parent_nID";
	public static final String name = "name";
	public static final String name_lang = "name_lang";
	public static final String name_charset = "name_charset";
	public static final String name_division = "name_division";
	public static final String name_division_lang = "name_division_lang";
	public static final String name_division_charset = "name_division_charset";
	public static final String names_subdivisions = "names_subdivisions";
	public static final String name_subdivisions_lang = "name_subdivisions_lang";
	public static final String name_subdivisions_charset = "name_subdivisions_charset";
	public static final String submitter_ID = "submitter_ID";
	public static final String organization_ID = "organization_ID";
	public static final String picture = "picture";
	public static final String creation_date = "creation_date";
	public static final String signature = "signature";
	public static final String arrival_date = "arrival_date";
	public static final String preferences_date = "preferences_date";
	public static final String peer_source_ID = "peer_source_ID";
	public static final String TNAME = "neighborhood";
	public static String blocked = "blocked";
	public static String requested = "requested";
	public static String broadcasted = "broadcasted";

	public static final String fields_neighborhoods_noID =
		 address+
		 ","+global_neighborhood_ID+
		 ","+parent_nID+
		 ","+name+
		 ","+name_lang+
		 ","+name_charset+
		 ","+name_division+
		 ","+name_division_lang+
		 ","+name_division_charset+
		 ","+names_subdivisions+
		 ","+name_subdivisions_lang+
		 ","+name_subdivisions_charset+
		 ","+submitter_ID+
		 ","+organization_ID+"," + signature+
		 ","+creation_date+","+arrival_date+","+picture+
		 ","+description+
		 ","+preferences_date+
		 ","+peer_source_ID+		 
		 ","+blocked+
		 ","+requested+
		 ","+broadcasted;
	public static final String fields_neighborhoods = fields_neighborhoods_noID + ","+neighborhood_ID;
	 
	public static final String SEP_names_subdivisions = ":"; // stops working if replaced with ";"!
	public static final int IDX_ADDRESS = 0;
	public static final int IDX_GID = 1;
	public static final int IDX_PARENT_ID = 2;
	public static final int IDX_NAME = 3;
	public static final int IDX_NAME_LANG = 4;
	public static final int IDX_NAME_CHARSET = 5;
	public static final int IDX_NAME_DIVISION = 6;
	public static final int IDX_NAME_DIVISION_LANG = 7;
	public static final int IDX_NAME_DIVISION_CHARSET = 8;
	public static final int IDX_NAMES_DUBDIVISIONS = 9;
	public static final int IDX_NAMES_DUBDIVISIONS_LANG = 10;
	public static final int IDX_NAMES_DUBDIVISIONS_CHARSET = 11;
	public static final int IDX_SUBMITTER_ID = 12;
	public static final int IDX_ORG_ID = 13;
	public static final int IDX_SIGNATURE = 14;
	public static final int IDX_CREATION_DATE = 15;
	public static final int IDX_ARRIVAL_DATE = 16;
	public static final int IDX_PICTURE = 17;
	public static final int IDX_DESCRIPTION = 18;
	public static final int IDX_PREFERENCES_DATE = 19;
	public static final int IDX_PEER_SOURCE_ID = 20;
	public static final int IDX_BLOCKED = 21;
	public static final int IDX_REQUESTED = 22;
	public static final int IDX_BROADCASTED = 23;
	public static final int IDX_ID = 24;
	public static final String FIELDS_SEP = ",";
	//public static int IDX_FIELDs=fields_neighborhoods.split(FIELDS_SEP).length;
	public static String[] fields_neighborhoods_list = fields_neighborhoods.split(","); //23;
	public static int IDX_FIELDs = fields_neighborhoods_list.length; //23;
	public static String[] fields_neighborhoods_noID_list = fields_neighborhoods_noID.split(","); //23;
	public static int IDX_FIELD_noIDs = fields_neighborhoods_noID_list.length; //.split(Pattern.quote(FIELDS_SEP));
}
