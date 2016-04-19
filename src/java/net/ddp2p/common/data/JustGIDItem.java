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
package net.ddp2p.common.data;
public
class JustGIDItem{
	public String gid;
	public String id;
	public String name;
	/**
	 *  create from gid, id , name
	 * @param _gid
	 * @param _id
	 * @param _name
	 */
	public JustGIDItem(String _gid, String _id, String _name) {set(_gid, _id, _name);}
	public void set(String _gid, String _id, String _name){
		gid = _gid;
		id = _id;
		name = _name;	
	}
	public String toString() {
		if(name != null) return name;
		if(id == null) return "JUST:"+super.toString();
		return "JUST:"+id;
	}
	public String toStringDump() {
		return "JUST:"+name+" id="+id+" gid="+gid;
	}
}
