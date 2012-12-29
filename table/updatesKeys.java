/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 
		Author: Khalid Alhamed and Marius Silaghi: msilaghi@fit.edu
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

import java.util.regex.Pattern;

import util.Util;

public class updatesKeys {

	public static final String TNAME = "updates_keys";
	public static final String updates_keys_ID = "updates_keys_ID";
	public static final String original_tester_name = "original_tester_name";
	public static final String my_name_for_tester = "my_name_for_tester";
	public static final String public_key = "public_key";
	public static final String public_key_hash = "public_key_hash";
	public static final String trusted_as_mirror = "trusted_as_mirror";
	public static final String trusted_as_tester = "trusted_as_tester";
	public static final String trust_weight = "trust_weight";
	public static final String reference_tester = "reference_tester";
	
	public static final String fields_updates_keys_no_ID = 
			original_tester_name+","+
			my_name_for_tester+","+
			public_key+","+
			public_key_hash+","+
			trusted_as_mirror+","+
			trusted_as_tester+","+
			trust_weight+","+
			reference_tester
			;
//	+","+		last_contact_date+","+	activity;
	public static final String[] _fields_updates_keys_no_ID = Util.trimmed(fields_updates_keys_no_ID.split(Pattern.quote(",")));
	public static final String fields_updates_keys = fields_updates_keys_no_ID+","+updates_keys_ID;
	public static final int F_FIELDS_NOID = _fields_updates_keys_no_ID.length+1;
	public static final int F_FIELDS = fields_updates_keys.split(Pattern.quote(",")).length;
	public static final int F_ORIGINAL_MIRROR_NAME = 0;
	public static final int F_MY_MIRROR_NAME = 1;
	public static final int F_PUBLIC_KEY = 2;
	public static final int F_PUBLIC_KEY_HASH = 3;
	public static final int F_USED_MIRROR = 4;
	public static final int F_USED_TESTER = 5;
	public static final int F_WEIGHT = 6;
	public static final int F_REFERENCE = 7;
	public static final int F_ID = 8;
	
}
