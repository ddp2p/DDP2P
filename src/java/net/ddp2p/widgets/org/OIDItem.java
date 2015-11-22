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
package net.ddp2p.widgets.org;
import net.ddp2p.common.util.Util;
class OIDItem{
	Object gid;
	Object expl;
	Object name;
	String sequence;
	public OIDItem(Object _name, Object _gid) {
		set(_gid, _name);
	}
	public OIDItem(Object _name, Object _gid, Object _expl) {
		set(_gid, _name); expl=_expl;
	}
	public OIDItem(Object _name, Object _gid, Object _expl, String _sequence) {
		set(_gid, _name); expl=_expl; sequence = _sequence;
	}
	public void set(Object _gid, Object _name){
		gid = _gid;
		name = _name;
	}
	public String toString() {
		return Util.getString(expl,"") + Util.getString(name,"") + "("+ Util.getString(gid)+")";
	}
}
