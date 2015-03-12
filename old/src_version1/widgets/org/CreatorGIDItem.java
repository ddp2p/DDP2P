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
package widgets.org;

import util.Util;

public class CreatorGIDItem{
	String gid;
	String hash;
	String name;
	public CreatorGIDItem(String _gid, String _hash, String _name) {set(_gid, _hash, _name);}
	public void set(String _gid, String _hash, String _name){
		gid = _gid;
		hash = _hash;
		name = _name;	
	}
	public String toString() {
		if(name != null) return name;
		if(hash == null) return "PEER:"+super.toString();
		return "PEER:"+hash;
	}
	public String toStringDump() {
		return "CreatorGIDItem: name=\""+name+"\" gid=\""+Util.trimmed(gid)+"\" hash=\""+Util.trimmed(hash)+"\"";
	}
}