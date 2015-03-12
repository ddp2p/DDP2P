/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Khalid AlHamed and Marius C. Silaghi
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

import util.Util;

public class directory_forwarding_terms {

	public static final String TNAME = "directory_forwarding_terms";
	public static final String term_ID = "term_ID";
	public static final String priority = "priority";
	public static final String topic = "topic";
	public static final String ad = "ad";
	public static final String plaintext = "plaintext";
	public static final String payment = "payment";
	public static final String payment_amount = "payment_amount"; // not yet supported
	public static final String peer_ID = "peer_ID";
	public static final String peer_instance_ID = "peer_instance_ID";
	public static final String dir_domain = "dir_addr";
	//public static final String all_instances = "all_instances";
	public static final String dir_tcp_port = "dir_tcp_port";
	public static final String service = "service";
	public static final String priority_type = "priority_type";
	
	public static final String fields_terms_no_ID = 
			priority+","+
			topic+","+
			ad+","+
			plaintext+","+
			payment+","+
			peer_ID+","+
			peer_instance_ID+","+
			dir_domain+","+
			dir_tcp_port+","+
			//all_instances+","+
			service+","+
			priority_type;
	public static final String[] _fields_terms_no_ID = Util.trimmed(fields_terms_no_ID.split(Pattern.quote(",")));
	public static final String fields_terms = fields_terms_no_ID+","+term_ID;
	public static final int F_FIELDS_NOID = _fields_terms_no_ID.length;
	public static final int F_FIELDS = fields_terms.split(Pattern.quote(",")).length;
	public static final int F_PRIORITY = 0;
	public static final int F_TOPIC = 1;
	public static final int F_AD = 2;
	public static final int F_PLAINTEXT = 3;
	public static final int F_PAYMENT = 4;
	public static final int F_PEER_ID = 5;
	public static final int F_PEER_INSTANCE_ID = 6;
	public static final int F_DIR_DOMAIN = 7;
	public static final int F_DIR_TCP_PORT = 8;
	//public static final int F_ALL_INSTANCES = 9;
	public static final int F_SERVICE = 9;
	public static final int F_PRIORITY_TYPE = 10;
	public static final int F_ID = 11;
	
}
