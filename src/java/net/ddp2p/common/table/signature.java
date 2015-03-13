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

public class signature {
	 public static final String signature_ID = "signature_ID";
	 public static final String global_signature_ID = "global_signature_ID";
	 public static final String hash_signature_alg = "hash_signature_alg";
	 public static final String hash_signature = "hash_signature";
	 public static final String constituent_ID = "constituent_ID";
	 public static final String justification_ID = "justification_ID";
	 public static final String motion_ID = "motion_ID";
	 public static final String signature = "signature";
	 public static final String format = "format";
	 public static final String choice = "choice";
	 public static final String creation_date = "creation_date";
	 public static final String arrival_date = "arrival_date";
	 public static final String TNAME = "signature";

//public static final String fields_votes = " global_signature_ID, choice, constituent_ID, motion_ID,"+" justification_ID, creation_date, signature, signature_ID, arrival_date";
//public static final int SIG_COL_ID = 7;

	public static final String fields_noID=
		global_signature_ID+ "," +
		hash_signature_alg+ "," +
		hash_signature+ "," +
		constituent_ID+ "," +
		justification_ID+ "," +
		motion_ID+ "," +
		signature+ "," +
		format+ "," +
		choice+ "," +
		creation_date+ "," +
		arrival_date
		;
	public static final String fields = fields_noID + "," + signature_ID;
	public static final String[] fields_array = fields.split(",");
	public static final String[] fields_noID_array = fields_noID.split(",");
	public static final int S_GID = 0;
	public static final int S_HASH_ALG = 1;
	public static final int S_HASH = 2;
	public static final int S_CONSTITUENT_ID = 3;
	public static final int S_JUSTIFICATION_ID = 4;
	public static final int S_MOTION_ID = 5;
	public static final int S_SIGNATURE = 6;
	public static final int S_FORMAT = 7;
	public static final int S_CHOICE = 8;
	public static final int S_CREATION = 9;
	public static final int S_ARRIVAL = 10;
	public static final int S_ID = 11;
	
	public static final int S_FIELDS = fields_array.length;
	public static final int S_FIELDS_NOID = fields_noID_array.length;
}
