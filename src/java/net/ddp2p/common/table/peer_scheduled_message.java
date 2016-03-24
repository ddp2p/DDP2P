/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014 Khalid AlHamed and Marius C. Silaghi
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

import net.ddp2p.common.util.Util;

public class peer_scheduled_message {
	public final static int MESSAGE_TYPE_RECOMMENDATION_OF_TESTERS = 0;
	public final static int MESSAGE_TYPE_PLUGINS_DATA = 1; // Not yet used

	public static final String TNAME = "peer_scheduled_message";
	public static final String message_ID="message_ID";// table ID
	public static final String peer_ID="peer_ID";// Peer_id use as F_key
	public static final String message="message";//message that should deliver to peer_ID
	public static final String message_type = "message_type"; // integer: 0 -recommendation of testers, 1-plugin messages, ..
	public static final String creation_date = "creation_date"; 
	
	
			
	public static final String fields_peer_scheduled_message_no_ID = 
			peer_ID+","+
			message+","+
			message_type+","+
			creation_date
			;
			
			
	public static final String[] _fields_peer_scheduled_message_no_ID = Util.trimmed(fields_peer_scheduled_message_no_ID.split(Pattern.quote(",")));
	public static final String fields_peer_scheduled_message = fields_peer_scheduled_message_no_ID+","+message_ID;
	public static final String[] _fields_peer_scheduled_message = Util.trimmed(fields_peer_scheduled_message.split(Pattern.quote(",")));
	
	public static final int F_FIELDS_NOID = _fields_peer_scheduled_message_no_ID.length;
	public static final int F_FIELDS = _fields_peer_scheduled_message.length;
	
	public static final int F_PEER_LID = 0;
	public static final int F_MESSAGE = 1;
	public static final int F_MESSAGE_TYPE = 2;
	public static final int F_CREATION_DATE = 3;
	public static final int F_ID = 4;
	
}



