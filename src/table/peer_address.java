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
package table;

import java.util.regex.Pattern;

public class peer_address {
	public static final String peer_address_ID = "peer_address_ID";
	public static final String peer_ID = "peer_ID";
	public static final String address = "address";
	public static final String type = "type";
	public static final String certified = "certified"; // 1 if it is digitally signed (in order of priority)
	public static final String priority = "priority"; // in ascending order, tells when to test an address (apriori)
	public static final String my_last_connection = "my_last_connection";
	public static final String arrival_date = "arrival_date";
	public static final String TNAME = "peer_address";
	
	public static final String fields_peer_address = " "+
	address+","+type+","+certified+","+priority;
	public static final String fields_noID =
			address+","+
			type+","+
			certified+","+
			priority+","+
			peer_ID+","+
			my_last_connection+","+
			arrival_date;
	public static final String fields =
			fields_noID+","+peer_address_ID;
	public static final String[] fields_list = fields.split(Pattern.quote(","));
	public static final String[] fields_noID_list = fields_noID.split(Pattern.quote(","));
	public static final int PA_ADDRESS = 0;
	public static final int PA_TYPE = 1;
	public static final int PA_CERTIFIED = 2;
	public static final int PA_PRIORITY = 3;
	public static final int PA_PEER_ID = 4;
	public static final int PA_CONTACT = 5;
	public static final int PA_ARRIVAL = 6;
	public static final int PA_PEER_ADDR_ID = 7;
	public static final int FIELDS = fields_list.length;
	public static final int FIELDS_NOID = fields_noID_list.length;
}
