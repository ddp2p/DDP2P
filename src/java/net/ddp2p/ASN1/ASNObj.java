/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
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
 package net.ddp2p.ASN1;
/**
 * This class does not require the implementation of instance, but will throw an exception when decoding
 * arrays.
 * @author msilaghi
 *
 */
public abstract class ASNObj extends ASNObjArrayable {
	public static final int DEPENDANTS_NONE = 0;
	public static final int DEPENDANTS_ALL = -1;
	/**
	 * Must be implemented whenever this object is encoded in a sequence (array/list)
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public ASNObj instance() throws CloneNotSupportedException{return (ASNObj) clone();}
}
