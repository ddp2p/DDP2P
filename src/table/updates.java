
/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Khalid AlHamed and Marius C. Silaghi
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

import java.util.regex.Pattern;

import util.Util;

public class updates {

	public static final String TNAME = "updates";
	public static final String updates_ID = "updates_ID";
	public static final String original_mirror_name = "original_mirror_name";
	public static final String my_name_for_mirror = "my_name_for_mirror";
	public static final String url = "url";
	public static final String last_version = "last_version";
	public static final String used = "used";
	public static final String last_releaseQD = "last_releaseQD";
	public static final String tester_info = "tester_info";// string format {T1:QoT1:RoT1:QoT2:RoT2;T2:QoT1:RoT1:...}
	public static final String last_contact_date = "last_contact_date";
	public static final String activity = "activity";
	
	public static final String fields_updates_no_ID = 
			original_mirror_name+","+
			my_name_for_mirror+","+
			url+","+
			last_version+","+
			used+","+
			last_releaseQD+","+	
			tester_info+","+
			last_contact_date+","+
			activity;
	public static final String[] _fields_updates_no_ID = Util.trimmed(fields_updates_no_ID.split(Pattern.quote(",")));
	public static final String fields_updates = fields_updates_no_ID+","+updates_ID;
	public static final int F_FIELDS_NOID = _fields_updates_no_ID.length;
	public static final int F_FIELDS = fields_updates.split(Pattern.quote(",")).length;
	public static final int F_ORIGINAL_MIRROR_NAME = 0;
	public static final int F_MY_MIRROR_NAME = 1;
	public static final int F_URL = 2;
	public static final int F_LAST_VERSION = 3;
	public static final int F_USED = 4;
	public static final int F_RELEASE_QOT = 5;
	public static final int F_TESTER_INFO = 6;
	public static final int F_LAST_CONTACT = 7;
	public static final int F_ACTIVITY = 8;
	public static final int F_ID = 9;
	
}
