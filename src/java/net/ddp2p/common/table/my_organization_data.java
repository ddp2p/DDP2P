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
package net.ddp2p.common.table;

import java.util.regex.Pattern;

public class my_organization_data {
	public static final String TNAME = "my_organization_data";
	public static final String organization_ID = "organization_ID";
	public static final String name = "name";
	public static final String creator = "creator";
	public static final String category = "category";
	public static final String data_to_request = "data_to_request"; // not yet used: meant for data (GIDH) to send in syncs. Doubled by table.organization.specific_requests
	public static final String row = "ROWID";
	
	public static final String fields_list_noID = 
			organization_ID + ","
				+ name + ","
				+ creator + ","
				+ category + ","
				+ data_to_request;
	public static final String fields_list = fields_list_noID + "," + row;
	public static final String[] fields_noID =  fields_list_noID.split(Pattern.quote(","));
	public static final String[] fields =  fields_list.split(Pattern.quote(","));
	public static final int FIELDS_NB_NOID = fields_noID.length;
	public static final int FIELDS_NB = fields.length;
	
	public static final int COL_ORGANIZATION_LID = 0;
	public static final int COL_NAME = 1;
	public static final int COL_CREATOR = 2;
	public static final int COL_CATEGORY = 3;
	public static final int COL_DATA_TO_REQUEST = 4;
	public static final int COL_ROW = 5;
}