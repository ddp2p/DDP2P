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

public class mirror {

	public static final String TNAME = "mirror";//"updates";
	public static final String mirror_ID = "mirror_ID";//"mirror_ID";
	public static final String public_key = "public_key"; //new field, part of mirror's url
	public static final String original_mirror_name = "original_mirror_name";
	public static final String my_name_for_mirror = "my_name_for_mirror";
	public static final String url = "url";
	public static final String last_version = "last_version";
	public static final String last_version_branch = "last_version_branch";
	public static final String used = "used";
	public static final String last_releaseQD = "last_version_releaseQD";// changed column name in DB
	public static final String tester_info = "last_version_testers_info";// string format {T1:QoT1:RoT1:QoT2:RoT2;T2:QoT1:RoT1:...}
	public static final String last_contact_date = "last_contact_date";
	public static final String activity = "activity";
	public static final String last_version_info = "last_version_info"; //to serialize versionInfo object
	public static final String location = "location" ; // new field good for faster download! 
	public static final String protocol = "protocol" ; // new field "http" or "ftp"
	public static final String data_version= "data_version"; // new field, signed fields
	public static final String creation_date_for_mirror_data = "creation_date"; // new field, ??
	public static final String preference_date = "preference_date"; // new field, ??
	public static final String signature = "signature"; // new field, signature of the mirror data
	
	public static final String revoked = "revoked";
	public static final String revoked_info = "revoked_info";
	public static final String revoked_GID_hash = "revoked_GID_hash";
	
			
	public static final String fields_updates_no_ID = 
			public_key+","+
			original_mirror_name+","+
			my_name_for_mirror+","+
			url+","+
			last_version+","+
			last_version_branch+","+
			used+","+
			last_releaseQD+","+	
			tester_info+","+
			last_contact_date+","+
			activity+","+
			last_version_info+","+
			location+","+
			protocol+","+
			data_version+","+
			creation_date_for_mirror_data+","+
			preference_date+","+
			signature+","+
			revoked+","+
			revoked_info+","+
			revoked_GID_hash;
			
	public static final String[] _fields_updates_no_ID = Util.trimmed(fields_updates_no_ID.split(Pattern.quote(",")));
	public static final String fields_updates = fields_updates_no_ID+","+mirror_ID;
	public static final int F_FIELDS_NOID = _fields_updates_no_ID.length;
	public static final int F_FIELDS = fields_updates.split(Pattern.quote(",")).length;
	public static final int F_PUBLIC_KEY=0;
	public static final int F_ORIGINAL_MIRROR_NAME = 1;
	public static final int F_MY_MIRROR_NAME = 2;
	public static final int F_URL = 3;
	public static final int F_LAST_VERSION = 4;
	public static final int F_LAST_VERSION_BRANCH = 5;
	public static final int F_USED = 6;
	public static final int F_RELEASE_QOT = 7;
	public static final int F_TESTER_INFO = 8;
	public static final int F_LAST_CONTACT = 9;
	public static final int F_ACTIVITY = 10;
	public static final int F_LAST_VERSION_INFO = 11;
	public static final int F_LOCATION=12;
	public static final int F_PROTOCOL=13;
	public static final int F_DATA_VERSION=14;
	public static final int F_CREATION_DATE=15;
	public static final int F_PREFERENCE_DATE=16;
	public static final int F_SIGNATURE=17;
	public static final int F_REVOKED = 18;
	public static final int F_REVOKED_INFO = 19;
	public static final int F_REVOKED_GID_HASH = 20;
	public static final int F_ID = 21;
	
}
