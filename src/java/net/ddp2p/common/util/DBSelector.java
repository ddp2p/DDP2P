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
package net.ddp2p.common.util;

import java.util.Hashtable;

public class DBSelector{
	public String field;
	public String value;
	public DBSelector(String _field, String _value){
		field = _field;
		value = _value;
	}
	public static DBSelector[] getSelectorList(String _field, String _value) {
		return new DBSelector[]{new DBSelector(_field, _value)};
	}
	public static Hashtable<String, DBSelector[]> getHashTable(String table, String _field, String _value){
		return getHashTable(table, getSelectorList(_field, _value));
	}
	public static Hashtable<String, DBSelector[]> getHashTable(String table, DBSelector[] d){
		Hashtable<String, DBSelector[]> h = new Hashtable<String, DBSelector[]>();
		h.put(table, d);
		return h;
	}
}
