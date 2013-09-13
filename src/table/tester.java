/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2013 Khalid Alhamed and  Marius C. Silaghi
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

public class tester {

	public static final String TNAME = "tester";
	public static final String tester_ID = "tester_ID";
	public static final String name = "name";
	public static final String public_key = "public_key";
	public static final String email = "email";
	public static final String url = "url";
	public static final String description = "description";
	
	public static final String fields_tester_no_ID = 
			name+","+
			public_key+","+
			email+","+
			url+","+
			description;
	public static final String[] _fields_tester_no_ID = Util.trimmed(fields_tester_no_ID.split(Pattern.quote(",")));
	public static final String fields_tester = fields_tester_no_ID+","+tester_ID;
	public static final int F_FIELDS_NOID = _fields_tester_no_ID.length;
	public static final int F_FIELDS = fields_tester.split(Pattern.quote(",")).length;
	public static final int F_NAME = 0;
	public static final int F_PUBLIC_KEY = 1;
	public static final int F_EMAIL = 2;
	public static final int F_URL = 3;
	public static final int F_DESCRIPTION = 4;
	public static final int F_ID = 5;
	
}
