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

public class recommendation_of_tester {

	public static final String TNAME = "recommendation_of_tester";
	public static final String  recommendationID="recommendation_ID";// primary key recommendation_ID
	public static final String senderPeerLID="senderPeerLID";// source_Peer local id
	public static final String testerLID="testerLID";//local id
	public static final String weight="weight";   // score
	public static final String address="address"; //address where the tester can be contacted
	public static final String creation_date="creation_date"; //sending date, the timestamp of the given testres rating
	public static final String signature="signature"; //signature of the whole package with this date from this peer 
    // (only stored with the item for the tester with the smallest LID)
    //s (assumes that the items are concatenated ordered by testerGID, ascending)
	public static final String arrival_date="arrival_date"; // arriving date
	
	
			
	public static final String fields_recommendationOfTester_no_ID = 
			senderPeerLID+","+
			testerLID+","+
			weight+","+
			address+","+
			creation_date+","+
			signature+","+
			arrival_date;
			
			
	public static final String[] _fields_recommendationOfTester_no_ID = Util.trimmed(fields_recommendationOfTester_no_ID.split(Pattern.quote(",")));
	public static final String fields_recommendationOfTester = fields_recommendationOfTester_no_ID+","+recommendationID;
	public static final int F_FIELDS_NOID = _fields_recommendationOfTester_no_ID.length;
	public static final int F_FIELDS = fields_recommendationOfTester.split(Pattern.quote(",")).length;
	public static final int F_SENDER_PEER_LID = 0;
	public static final int F_TESTER_LID = 1;
	public static final int F_WEIGH = 2;
	public static final int F_ADDRESS = 3;
	public static final int F_CREATION_DATE = 4;
	public static final int F_SIGNATURE = 5;
	public static final int F_ARRIVAL_DATE = 6;
	public static final int F_ID = 7;
	
}
