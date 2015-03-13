/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014 Marius C. Silaghi
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
package net.ddp2p.ciphersuits;
public
class Cipher_Sizes{
	public static final int INT_RANGE = 0;
	public static final int LIST = 1;
	public int _default;
	public int type;
	public Object range;
	Cipher_Sizes(int __default, int _type, Object _range) {
		_default = __default;
		type = _type;
		range = _range;
	}
	public String toString() {
		return "[type="+type+" def="+_default+" range="+range+"]";
	}
}