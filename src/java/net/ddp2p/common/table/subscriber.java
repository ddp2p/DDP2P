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
package net.ddp2p.common.table;

import java.util.regex.Pattern;

import net.ddp2p.common.util.Util;

public class subscriber {

	public static final String TNAME = "subscriber";
	public static final String subscriber_ID = "subscriber_ID";
	public static final String GID = "GID";
	public static final String GID_hash = "GID_hash";
	public static final String all_instances = "all_instances";
	public static final String instance = "instance";
	public static final String name = "name";
	public static final String topic = "topic";
	public static final String ad = "ad";
	public static final String plaintext = "plaintext";
	public static final String payment = "payment";
	public static final String payment_amount = "payment_amount"; // not yet supported
	public static final String mode = "mode";
	public static final String expiration = "expiration";
	
	public static final String fields_subscribers_no_ID = 
			GID+","+
			GID_hash+","+
			instance+","+
			all_instances+","+
			name+","+
			topic+","+
			ad+","+
			plaintext+","+
			payment+","+
			mode+","+
			expiration;
	public static final String[] _fields_subscribers_no_ID = Util.trimmed(fields_subscribers_no_ID.split(Pattern.quote(",")));
	public static final String fields_subscribers = fields_subscribers_no_ID+","+subscriber_ID;
	public static final int F_FIELDS_NOID = _fields_subscribers_no_ID.length;
	public static final int F_FIELDS = fields_subscribers.split(Pattern.quote(",")).length;
	public static final int F_GID = 0;
	public static final int F_GID_HASH = 1;
	public static final int F_INSTANCE =2 ;
	public static final int F_ALL_INSTANCES =3 ;
	public static final int F_NAME = 4;
	public static final int F_TOPIC = 5;
	public static final int F_AD = 6;
	public static final int F_PLAINTEXT = 7;
	public static final int F_PAYMENT = 8;
	public static final int F_MODE = 9;
	public static final int F_EXPIRATION = 10;
	public static final int F_ID = 11;
	
}