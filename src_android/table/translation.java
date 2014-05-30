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

public class translation {
	 public static final String translation_ID = "translation_ID";
	 public static final String global_translation_ID = "global_translation_ID";
	 public static final String hash_alg = "hash_alg";
	 public static final String value = "value";
	 public static final String value_lang = "value_lang";
	 public static final String value_ctx = "value_ctx";
	 public static final String translation = "translation";
	 public static final String translation_lang = "translation_lang";
	 public static final String translation_charset = "translation_charset";
	 public static final String translation_flavor = "translation_flavor";
	 public static final String organization_ID = "organization_ID";
	 public static final String submitter_ID = "submitter_ID";
	 public static final String creation_date = "creation_date";
	 public static final String arrival_date = "arrival_date";
	 public static final String signature = "signature";
	public static final String TNAME = "translation";
	
	public static final String fields_noID = 
		global_translation_ID + "," +
		hash_alg + "," +
		value + "," +
		value_lang + "," +
		value_ctx + "," +
		translation + "," +
		translation_lang + "," +
		translation_charset + "," +
		translation_flavor + "," +
		organization_ID + "," +
		submitter_ID + "," +
		creation_date + "," +
		arrival_date + "," +
		signature
		;
	public static final String fields = fields_noID + "," + translation_ID;
	
	public static final String[] fields_array = fields.split(",");
	public static final String[] fields_noID_array = fields_noID.split(",");
	
	public static final int T_GID = 0;
	public static final int T_HASH_ALG = 1;
	public static final int T_VALUE = 2;
	public static final int T_VALUE_LANG = 3;
	public static final int T_VALUE_CTX = 4;
	public static final int T_TRANSLATION = 5;
	public static final int T_TRANSLATION_LANG = 6;
	public static final int T_TRANSLATION_CHARSET = 7;
	public static final int T_TRANSLATION_FLAVOR = 8;
	public static final int T_ORGANIZATION_ID = 9;
	public static final int T_CONSTITUENT_ID = 10;
	public static final int T_CREATION_DATE = 11;
	public static final int T_ARRIVAL_DATE = 12;
	public static final int T_SIGN = 13;
	public static final int T_ID = 14;
	
	public static final int T_FIELDS = fields_array.length;
	public static final int T_FIELDS_NOID = fields_noID_array.length;
}
