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
import static net.ddp2p.common.util.Util.__;
import net.ddp2p.common.util.Util;

public class field_extra {
	 public static final String field_extra_ID = "field_extra_ID";
	 public static final String global_field_extra_ID = "global_field_extra_ID";
	 public static final String can_be_provided_later = "can_be_provided_later";
	 public static final String certificated = "certificated";
	 public static final String default_val = "default_val";
	 public static final String entry_size = "entry_size";
	 public static final String label = "label";
	 public static final String list_of_values = "list_of_values";
	 public static final String organization_ID = "organization_ID";
	 public static final String partNeigh = "partNeigh";
	 public static final String required = "required";
	 public static final String tip = "tip";
	 public static final String tip_lang = "tip_lang";
	 public static final String label_lang = "label_lang";
	 public static final String list_of_values_lang = "list_of_values_lang";
	 public static final String default_value_lang = "default_value_lang";
	 public static final String oid = "oid";
	 public static final String version = "version";
	 public static final String tmp = "tmp"; // in a temporary constituent, not part of org
	 
	 public static final String TNAME = "field_extra";
	 public static final String org_field_extra_insert = 
			 label+","+
					 label_lang+","+ 
					 can_be_provided_later+","+ 
					 certificated+"," +
					 entry_size+","  +
					 partNeigh+","+ 
					 required+","+ 
					 default_val+","+ 
					 default_value_lang+","+ 
					 list_of_values+","  +
					 list_of_values_lang+","+ 
					 tip+","+ 
					 tip_lang+","+ 
					 oid+","+ 
					 organization_ID+","+
					 global_field_extra_ID+","+
					 version+","+
					 tmp;
	 public static final String org_field_extra =
		 	org_field_extra_insert+"," 
			+field_extra_ID;
	 public static final int org_field_extras = org_field_extra.split(",").length;
		public static final int OPARAM_LABEL = 0;
		public static final int OPARAM_LABEL_L = 1;
		public static final int OPARAM_LATER = 2;
		public static final int OPARAM_CERT = 3;
		public static final int OPARAM_SIZE = 4;
		public static final int OPARAM_NEIGH = 5;
		public static final int OPARAM_REQ = 6;
		public static final int OPARAM_DEFAULT = 7;
		public static final int OPARAM_DEFAULT_L = 8;
		public static final int OPARAM_LIST_VAL = 9;
		public static final int OPARAM_LIST_VAL_L = 10;
		public static final int OPARAM_TIP = 11;
		public static final int OPARAM_TIP_L = 12;
		public static final int OPARAM_OID = 13;
		public static final int OPARAM_ORG_ID = 14;
		public static final int OPARAM_GID = 15;
		public static final int OPARAM_VERSION = 16;
		public static final int OPARAM_TMP = 17;
		public static final int OPARAM_EXTRA_FIELD_ID = 18;
	 
	public static final String some_field_extra = 
		 can_be_provided_later+","+ certificated+"," + default_val+"," +entry_size+"," + field_extra_ID+", "+
		 label+", "+list_of_values+", " + partNeigh+","+ required+","+ tip+","+ 
		 label_lang;
	public static final int SPARAM_LATER = 0;
	public static final int SPARAM_CERT = 1;
	public static final int SPARAM_DEFAULT = 2;
	public static final int SPARAM_SIZE = 3;
	public static final int SPARAM_EXTRA_FIELD_ID = 4;
	public static final int SPARAM_LABEL = 5;
	public static final int SPARAM_LIST_VAL = 6;
	public static final int SPARAM_NEIGH = 7;
	public static final int SPARAM_REQ = 8;
	public static final int SPARAM_TIP = 9;
	public static final int SPARAM_LABEL_L = 10;
	public static final int SPARAM_DEFAULT_L = 11;
	public static final int SPARAM_TIP_L = 12;
	public static final int SPARAM_OID = 13;
	public static final int SPARAM_ORG_ID = 14;
	public static final int SPARAM_LIST_VAL_L = 15;
	 
	public static final String SEP_list_of_values = ";";
	public static final int partNeigh_non_neighborhood_indicator=0;  // this is not neigh
	public static final int partNeigh_non_neighborhood_upper_indicator = 0; // anything smaller is not neigh
	
	public static final long SPARAM_LATER_DEFAULT = 0;
	public static final long SPARAM_CERT_DEFAULT = 0;
	public static final int SPARAM_SIZE_DEFAULT = 50;
	public static final long SPARAM_EXTRA_FIELD_ID_DEFAULT = -1;
	public static final String SPARAM_DEFAULT_DEFAULT = "";
	public static final String SPARAM_LIST_VAL_DEFAULT = "";
	public static final int SPARAM_NEIGH_DEFAULT = 0;
	public static final long SPARAM_REQ_DEFAULT = 0;
	
	public static final String SPARAM_TIP_DEFAULT = null;
	public static final String SPARAM_LABEL_L_DEFAULT = "en";
	public static final String NEIGHBORHOOD_ID_NA = "0";
	public static String SPARAM_LABEL_DEFAULT = __("Unspecified field");
	public static final String[] org_field_extra_insert_list = Util.trimmed(net.ddp2p.common.table.field_extra.org_field_extra_insert.split(","));;
	public static boolean isANeighborhood(int part_of_neigh) {
		return part_of_neigh>0;
	}
}
