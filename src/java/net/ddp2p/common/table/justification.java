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

public class justification {
	 public static final String justification_ID = "justification_ID";
	 public static final String global_justification_ID = "global_justification_ID";
	 public static final String motion_ID = "motion_ID";
	 public static final String organization_ID = "organization_ID";
	 public static final String hash_justification_alg = "hash_justification_alg";
	 public static final String hash_justification = "hash_justification";
	 public static final String justification_title_format = "justification_title_format";
	 public static final String justification_title = "justification_title";
	 public static final String answerTo_ID = "answerTo_ID";
	 public static final String constituent_ID = "constituent_ID";
	 public static final String justification_text_format = "justification_text_format";
	 public static final String justification_text = "justification_text";
	 public static final String signature = "signature";
	 public static final String last_reference_date = "last_reference_date";
	 public static final String creation_date = "creation_date";
	 public static final String arrival_date = "arrival_date";
	 public static final String preferences_date = "preferences_date";
	 public static final String temporary = "temporary";
	 public static final String hidden = "hidden";
	 public static final String peer_source_ID = "peer_source_ID";
	 public static final String blocked = "blocked";
	public static final String requested = "requested";
	public static final String broadcasted = "broadcasted";
	//public static final String organization_ID = "organization_ID";
	 public static final String TNAME = "justification";
	 
	public static String fields_noID =
		global_justification_ID +","+
		motion_ID+","+
		hash_justification_alg+","+
		hash_justification+","+
		justification_title_format+","+
		justification_title+","+
		answerTo_ID+","+
		constituent_ID+","+
		justification_text_format+","+
		justification_text+","+
		signature+","+
		last_reference_date+","+
		creation_date+","+
		arrival_date+","+
		requested+","+
		blocked+","+
		broadcasted+","+
		temporary+","+
		peer_source_ID+","+
		preferences_date+","+
		hidden+","+
		organization_ID
		;
	public static String fields = fields_noID + "," + justification_ID;
	public static String[] fields_noID_array = fields_noID.split(",");
	public static final String[] fields_array = fields.split(",");
	
	
	public static final int J_GID = 0;
	public static final int J_MOTION_ID = 1;
	public static final int J_HASH_ALG = 2;
	public static final int J_HASH = 3;	
	public static final int J_TITLE_FORMAT = 4;
	public static final int J_TITLE = 5;
	public static final int J_ANSWER_TO_ID = 6;	
	public static final int J_CONSTITUENT_ID = 7;
	public static final int J_TEXT_FORMAT = 8;
	public static final int J_TEXT = 9;
	public static final int J_SIGNATURE = 10;
	public static final int J_REFERENCE = 11;
	public static final int J_CREATION = 12;
	public static final int J_ARRIVAL = 13;
	public static final int J_REQUESTED = 14;
	public static final int J_BLOCKED = 15;
	public static final int J_BROADCASTED = 16;
	public static final int J_TEMPORARY = 17;
	public static final int J_PEER_SOURCE_ID = 18;
	public static final int J_PREFERENCES_DATE = 19;
	public static final int J_HIDDEN = 20;
	public static final int J_ORG_ID = 21;
	public static final int J_ID = 22;
	public static final int J_FIELDS = fields_array.length;
}
