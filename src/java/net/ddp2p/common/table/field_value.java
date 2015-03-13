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
package net.ddp2p.common.table;

public class field_value {
	 public static final String field_value_ID = "field_value_ID";
	 public static final String constituent_ID = "constituent_ID";
	 public static final String field_extra_ID = "field_extra_ID";
	 public static final String value = "value";
	 public static final String fieldID_above = "fieldID_above";
	 public static final String field_default_next = "field_default_next";
	 public static final String neighborhood_ID = "neighborhood_ID";
	 public static final String value_lang = "value_lang";
	public static final String TNAME = "field_value";


	public static final String fields_list = 
		" "+ constituent_ID+
		","+ field_extra_ID+
		","+ field_value_ID+
		","+ value+
		","+ fieldID_above+
		","+ field_default_next+
		","+ neighborhood_ID+
		","+ value_lang;
	public static final int VAL_COL_CONSTITUENT_ID = 0;
	public static final int VAL_COL_FIELD_EXTRA_ID = 1;
	public static final int VAL_COL_VALUE_ID = 2;
	public static final int VAL_COL_VALUE = 3;
	public static final int VAL_COL_FIELD_ID_ABOVE = 4;
	public static final int VAL_COL_FIELD_DEFAULT_NEXT = 5;
	public static final int VAL_COL_NEIGH_ID = 6;
	public static final int VAL_COL_LANG = 7;
	public static final int VAL_COLs = fields_list.split(",").length;//8;
}
