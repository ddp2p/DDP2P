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

public class peer_my_data {
	 public static final String peer_my_data_ID = "ROWID";
	 public static final String peer_ID = "peer_ID";
	 public static final String name = "name";
	 public static final String slogan = "slogan";
	 public static final String picture = "picture";
	 public static final String my_topic = "my_topic";
	 public static final String broadcastable = "broadcastable";
	 
	 public static final String TNAME = "peer_my_data";
	 
	 public static final int COL_PEER_ID = 0;
	 public static final int COL_NAME = 1;
	 public static final int COL_SLOGAN = 2;
	 public static final int COL_PICTURE = 3;
	 public static final int COL_TOPIC = 4;
	 public static final int COL_BROADCASTABLE = 5;
	 public static final int COL_ROW = 6;
	 
	 public static final String fields_noID = 
			 peer_ID
			 + "," + name
			 + "," + slogan
			 + "," + picture
			 + "," + my_topic
			 + "," + broadcastable
			 ;
	 public static final String fields = 
			 fields_noID +","+ peer_my_data_ID;
	 public static final String[] fields_list_noID = fields_noID.split(Pattern.quote(","));
	 public static final String[] fields_list = fields.split(Pattern.quote(","));
	 
	 public static int FIELDS_NOID = fields_list_noID.length;
	 public static int FIELDS = fields_list.length;
}