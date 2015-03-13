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

import java.util.regex.Pattern;

public class oid {
	 public static final String oid_ID = "oid_ID";
	 public static final String OID_name = "OID_name";
	 public static final String explanation = "explanation";
	 public static final String sequence = "sequence";
	 public static final String TNAME = "oid";
	
	 public static final String fields_list_noID = 
			 net.ddp2p.common.table.oid.sequence + ","
			 + net.ddp2p.common.table.oid.explanation+","
			 + net.ddp2p.common.table.oid.OID_name;
	 public static final String fields_list = 
			 fields_list_noID + ","
			 + net.ddp2p.common.table.oid.oid_ID;
	 public static final String fields_noID[] = fields_list_noID.split(Pattern.quote(","));
	 public static final String fields[] = fields_list.split(Pattern.quote(","));
	 
	 public static final int COL_SEQ = 0;
	 public static final int COL_EXPL = 1;
	 public static final int COL_NAME = 2;
	 public static final int COL_LID = 3;
	 public static final int FIELDS_NOID = fields_noID.length;
	 public static final int FIELDS = fields.length;
}
