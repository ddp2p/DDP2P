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

public class news {
	// Global name used for external communication
	public static final String G_TNAME = "news";
	// local database names
	 public static final String hash_alg = "hash_alg";
	 public static final String news_ID = "news_ID";
	 public static final String global_news_ID = "global_news_ID";
	 public static final String constituent_ID = "constituent_ID";
	 public static final String organization_ID = "organization_ID";
	 public static final String news = "news";
	 public static final String type = "type";
	 public static final String title = "title";
	 public static final String title_type = "title_type";
	 public static final String signature = "signature";
	 public static final String creation_date = "creation_date";
	 public static final String arrival_date = "arrival_date";
	public static final String motion_ID = "motion_ID";
	public static final String justification_ID = "justification_ID";
	public static final String blocked = "blocked";
	public static final String broadcasted = "broadcasted";
	public static final String requested = "requested";
	public static final String TNAME = "news";
	
	public static final String fields_no_ID =
		hash_alg + "," +
		global_news_ID + "," +
		constituent_ID + "," +
		organization_ID + "," +
		motion_ID + "," +
		news + "," +
		type + "," +
		title + "," +
		title_type + "," +
		signature + "," +
		creation_date + "," +
		arrival_date+ "," +
		requested+ "," +
		blocked+ "," +
		broadcasted
		//+ "," +justification_ID
		;
	public static final String fields = fields_no_ID + "," + news_ID;
	public static final String[] fields_no_ID_array = fields_no_ID.split(",");
	public static final String[] fields_array = fields.split(",");
	public static final int N_HASH_ALG = 0;
	public static final int N_NEWS_GID = 1;
	public static final int N_CONSTITUENT_ID = 2;
	public static final int N_ORG_ID = 3;
	public static final int N_MOT_ID = 4;
	public static final int N_TEXT = 5;
	public static final int N_TEXT_FORMAT = 6;
	public static final int N_TITLE = 7;
	public static final int N_TITLE_FORMAT = 8;
	public static final int N_SIGNATURE = 9;
	public static final int N_CREATION = 10;
	public static final int N_ARRIVAL = 11;
	public static final int N_REQUESTED = 12;
	public static final int N_BLOCKED = 13;
	public static final int N_BROADCASTED = 14;
	public static final int N_ID = 15;
	public static final int N_FIELDS = fields_array.length;
	public static final int N_FIELDS_NOID = fields_no_ID_array.length;
}
