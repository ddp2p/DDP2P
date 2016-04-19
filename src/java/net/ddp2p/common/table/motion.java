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
package net.ddp2p.common.table;
public class motion {
	 public static final String motion_ID = "motion_ID";
	 public static final String global_motion_ID = "global_motion_ID";
	 public static final String hash_motion_alg = "hash_motion_alg";
	 public static final String hash_motion = "hash_motion";
	 public static final String format_title_type = "format_title_type";
	 public static final String format_text_type = "format_text_type";
	 public static final String motion_title = "motion_title";
	 public static final String motion_text = "motion_text";
	 public static final String constituent_ID = "constituent_ID";
	 public static final String organization_ID = "organization_ID";
	 public static final String enhances_ID = "enhances_ID";
	 public static final String signature = "signature";
	 public static final String status = "status";
	 public static final String choices = "choices";
	 public static final String creation_date = "creation_date";
	 public static final String arrival_date = "arrival_date";
	 public static final String preferences_date = "preferences_date";
	 public static final String hidden = "hidden";
	 public static final String temporary = "temporary";
	 public static final String peer_source_ID = "peer_source_ID";
		public static final String requested = "requested";
		public static final String broadcasted = "broadcasted";
		public static final String blocked = "blocked";
		public static final String category="category";
	 public static final String TNAME = "motion";
	public static final int M_MOTION_GID = 0;
	public static final int M_HASH_ALG = 1;
	public static final int M_HASH_MOTION = 2;	
	public static final int M_TITLE_FORMAT = 3;
	public static final int M_TEXT_FORMAT = 4;
	public static final int M_TITLE = 5;
	public static final int M_TEXT = 6;
	public static final int M_CONSTITUENT_ID = 7;
	public static final int M_ORG_ID = 8;
	public static final int M_ENHANCED_ID = 9;
	public static final int M_SIGNATURE = 10;
	public static final int M_STATUS = 11;
	public static final int M_CREATION = 12;
	public static final int M_ARRIVAL = 13;
	public static final int M_CATEGORY = 14;
	public static final int M_CHOICES = 15;
	public static final int M_REQUESTED = 16;
	public static final int M_BLOCKED = 17;
	public static final int M_BROADCASTED = 18;
	public static final int M_HIDDEN = 19;
	public static final int M_TEMPORARY = 20;
	public static final int M_PEER_SOURCE_ID = 21;
	public static final int M_PREFERENCES_DATE = 22;
	public static final int M_MOTION_ID = 23;
	public static final String fields_noID =
		global_motion_ID+","+
		hash_motion_alg+","+
		hash_motion+","+
		format_title_type+","+
		format_text_type+","+
		motion_title+","+
		motion_text+","+
		constituent_ID+","+
		organization_ID+","+
		enhances_ID+","+
		signature+","+
		status+","+
		creation_date+ "," +
		arrival_date+ "," +
		category+ "," +
		choices+ "," +
		requested+ "," +
		blocked+ "," +
		broadcasted+ "," +
		hidden + "," +
		temporary + "," +
		peer_source_ID + "," +
		preferences_date
		;
	public static final String fields = fields_noID + "," + motion_ID;
	public static final String[] fields_array = fields.split(",");
	public static final String[] fields_noID_array = fields_noID.split(",");	
	public static final int M_FIELDS = fields_array.length;
}
