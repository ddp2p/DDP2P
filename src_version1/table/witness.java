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

public class witness {
	 public static final String witness_ID = "witness_ID";
	 public static final String global_witness_ID = "global_witness_ID";
	 public static final String hash_witness_alg = "hash_witness_alg";
	 //public static final String hash_witness = "hash_witness";
	 public static final String category = "category";
	 public static final String category_trustworthiness = "category_trustworthiness";
	 public static final String neighborhood_ID = "neighborhood_ID";
	 public static final String sense_y_n = "sense_y_n";
	 public static final String sense_y_trustworthiness = "sense_y_trustworthiness";
	 public static final String statements = "statements";
	 public static final String source_ID = "source_ID";
	 public static final String target_ID = "target_ID";
	 public static final String signature = "signature";
	 public static final String creation_date = "creation_date";
	 public static final String arrival_date = "arrival_date";
	
	 public static final String TNAME = "witness";
	 
	static public String witness_fields =
		" "+table.witness.category+
		", "+table.witness.creation_date+
		", "+table.witness.global_witness_ID+
		", "+table.witness.hash_witness_alg+
		", "+table.witness.neighborhood_ID+
		", "+table.witness.sense_y_n+
		", "+table.witness.signature+
		", "+table.witness.source_ID+
		", "+table.witness.target_ID+
		", "+table.witness.witness_ID+
		", "+table.witness.arrival_date+
		", "+table.witness.sense_y_trustworthiness+
		", "+table.witness.category_trustworthiness+
		", "+table.witness.statements
		;

	public static final int WIT_COL_CAT = 0;
	public static final int WIT_COL_CREATION_DATE = 1;
	public static final int WIT_COL_GID = 2;
	public static final int WIT_COL_HASH_ALG = 3;
	public static final int WIT_COL_T_N_ID = 4;
	public static final int WIT_COL_SENSE = 5;
	public static final int WIT_COL_SIGN = 6;
	public static final int WIT_COL_S_C_ID = 7;
	public static final int WIT_COL_T_C_ID = 8;
	public static final int WIT_COL_ID = 9;
	public static final int WIT_COL_ARRIVAL_DATE = 10;
	public static final int WIT_COL_SENSE_TRUSTWORTHINESS = 11;
	public static final int WIT_COL_CAT_TRUSTWORTHINESS = 12;
	public static final int WIT_COL_STATEMENTS = 13;
	public static final int WIT_FIELDS = witness_fields.split(",").length;//14;
}
