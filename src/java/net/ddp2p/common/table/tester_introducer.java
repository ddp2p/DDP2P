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

public class tester_introducer {

	public static final String TNAME = "tester_introducer";
	public static final String testerIntroducerID="tester_introducer_ID";// primary key
	public static final String testerLID="testerLID";// LID of the tester about which we store his/her creation history
	public static final String introducerPeer_LID="introducer_peer_LID";// the peer who has introduced the tester in the recommendation 
	public static final String weight="weight";  // the initial weight given by the creator peer
	public static final String creation_date="creation_date"; //date set by the creator when introduced the tester first time
	public static final String signature="signature"; //signature of date,weight and tester_GID (from tester table if exist??) 
	public static final String testerRejectingDate="tester_rejecting_date";//date when reject a recommendation of testerLID because of the introducer.
	public static final String attackByIntroducer = "attack_by_introducer"; // count how many attack attempts with in a time limit based on the expiration date
	
			
	public static final String fields_tester_introducer_no_ID = 
			testerLID+","+
			introducerPeer_LID+","+
			weight+","+
			creation_date+","+
			signature+","+
			testerRejectingDate+","+
			attackByIntroducer;
			
			
	public static final String[] _fields_tester_introducer_no_ID = Util.trimmed(fields_tester_introducer_no_ID.split(Pattern.quote(",")));
	public static final String fields_tester_introducer = fields_tester_introducer_no_ID+","+testerIntroducerID;
	public static final int F_FIELDS_NOID = _fields_tester_introducer_no_ID.length;
	public static final int F_FIELDS = fields_tester_introducer.split(Pattern.quote(",")).length;
	public static final int F_TESTER_LID = 0;
	public static final int F_INTRODUCER_PEER_LID = 1;
	public static final int F_WEIGH = 2;
	public static final int F_CREATION_DATE = 3;
	public static final int F_SIGNATURE = 4;
	public static final int F_TESTER_REJECTING_DATE = 5;
	public static final int F_ATTACK_BY_INTRODUCER = 6;
	public static final int F_ID = 7;
	
}
