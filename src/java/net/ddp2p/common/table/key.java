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
import java.util.regex.Pattern;
import net.ddp2p.common.util.Util;
public class key {
	public static final String TNAME = "key";
	public static final String key_ID = "key_ID";
	public static final String public_key = "public_key";
	public static final String secret_key = "secret_key";
	public static final String type = "type";
	public static final String name = "name";
	public static final String hide = "hide";
	public static final String preference_date = "preference_date";
	public static final String creation_date = "creation_date";
	public static final String ID_hash = "ID_hash";
	public static final int COL_NAME = 0;
	public static final int COL_TYPE = 1;
	public static final int COL_IDHASH = 2;
	public static final int COL_PK = 3;
	public static final int COL_SK = 4;
	public static final int COL_HIDE = 5;
	public static final int COL_PREF_DATE = 6;
	public static final int COL_CREATION_DATE = 7;
	public static String fields_list_noID=
			name + ","+
			type+","+
			ID_hash+","+
			public_key+","+
			secret_key+","+
			hide+","+
			preference_date+","+
			creation_date;
	public static String fields_list = fields_list_noID+","+key_ID;
	public static String[] fields_noID =
			Util.trimmed(fields_list_noID.split(Pattern.quote(",")));
	public static String[] fields =
			Util.trimmed(fields_list.split(Pattern.quote(",")));
	public static final int FIELDS = fields.length;
	public static final int FIELDS_NOID = fields_noID.length;
	public static final int COL_ID = FIELDS_NOID;
}
