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


import java.util.ArrayList;
import java.util.regex.Pattern;

import streaming.UpdatePeersTable;
import util.Util;

import ciphersuits.Cipher;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import data.D_Peer;

public class peer {
	// global names used in ASNSyncRequest for external communication
	public static final String G_TNAME = "peer";
	public static final String G_name = "name";
	public static final String G_slogan = "slogan";
	public static final String G_broadcastable = "broadcastable";
	
	public static final String peer_ID = "peer_ID";
	public static final String global_peer_ID = "global_peer_ID";
	public static final String global_peer_ID_hash = "global_peer_ID_hash";
	public static final String name = "name";
	public static final String broadcastable = "broadcastable";
	public static final String blocked = "blocked";
	public static final String slogan = "slogan";
	public static final String used = "used";
	public static final String hash_alg = "hash_alg";
	public static final String signature = "signature";
	public static final String picture = "picture";
	//public static final String no_update = "no_update"; // replaced by blocked
	public static final String exp_avg = "exp_avg";
	public static final String experience = "experience";
	public static final String filtered = "filtered";
	public static final String last_sync_date = "last_sync_date";
	public static final String last_reset = "last_reset";
	public static final String creation_date = "creation_date";
	public static final String arrival_date = "arrival_date";
	public static final String preferences_date = "preferences_date";
	public static final String plugins_msg = "plugins_msg";
	public static final String plugin_info = "plugin_info";
	public static final String version = "version";
	public static final String emails = "emails";
	public static final String phones = "phones";
	public static final String name_verified = "name_verified";
	public static final String email_verified = "email_verified";
	public static final String category = "category";
	public static final String revoked = "revoked";
	public static final String revokation_instructions = "revokation_instructions";
	public static final String revokation_replacement_GIDhash = "revokation_replacement_GIDhash";
	public static final String hidden = "hidden";
	public static final String first_provider_peer = "first_provider_peer";
	public static final String TNAME = "peer";

	/*
	public static String peer_fields = 
		table.peer.peer_ID+
		","+table.peer.global_peer_ID+
		","+table.peer.name+
		","+table.peer.broadcastable+
		","+table.peer.last_sync_date+
		"," +table.peer.arrival_date+
		","+table.peer.slogan+
		","+table.peer.used+
		","+table.peer.hash_alg+
		","+table.peer.signature+
		","+table.peer.picture+
		","+table.peer.no_update+
		","+table.peer.exp_avg+
		","+table.peer.experience+
		","+table.peer.creation_date+
		","+table.peer.filtered+
		","+table.peer.plugin_info+
		","+table.peer.plugins_msg;
	public static final int PEER_FIELDS_COL_ID = 0;
	public static final int PEER_FIELDS_COL_FILTERED = 15;
*/
	
	public static final String fields_peers_no_ID =
			" "+
			global_peer_ID+","+
			global_peer_ID_hash+","+
			name+","+
			slogan+","+
			hash_alg+","+

			signature+","+
			creation_date+","+
			broadcastable+","+
			plugin_info+","+
			plugins_msg+","+
			
			filtered+","+
			last_sync_date+","+
			last_reset+","+
			arrival_date+","+
			used+","+
			
			picture+","+
			exp_avg+","+
			experience+","+
			blocked+","+
			version+","+
			
			emails+","+
			phones+","+
			preferences_date+","+			
			name_verified +","+
			email_verified +","+
			
			category +","+
			revoked +","+
			revokation_instructions +","+
			revokation_replacement_GIDhash +","+
			hidden+","+
			
			first_provider_peer
			;
//			no_update+","+
	public static final String[] list_fields_peers_no_ID = Util.trimmed(fields_peers_no_ID.split(Pattern.quote(",")));
	public static final String fields_peers = fields_peers_no_ID + "," + peer_ID;
	public static final String[] list_fields_peers = Util.trimmed(fields_peers.split(Pattern.quote(",")));
	public static final int PEER_COL_FIELDS_NO_ID = table.peer.list_fields_peers_no_ID.length;
	public static final int PEER_COL_FIELDS = table.peer.list_fields_peers.length;
			
	public static final int PEER_COL_GID = 0;
	public static final int PEER_COL_GID_HASH = 1;
	public static final int PEER_COL_NAME = 2;
	public static final int PEER_COL_SLOGAN = 3;
	public static final int PEER_COL_HASH_ALG = 4;
	public static final int PEER_COL_SIGN = 5;
	public static final int PEER_COL_CREATION = 6;
	public static final int PEER_COL_BROADCAST = 7;
	public static final int PEER_COL_PLUG_INFO = 8;
	public static final int PEER_COL_PLUG_MSG = 9;
	public static final int PEER_COL_FILTERED = 10;
	public static final int PEER_COL_LAST_SYNC = 11;
	public static final int PEER_COL_LAST_RESET = 12;
	public static final int PEER_COL_ARRIVAL = 13;
	public static final int PEER_COL_USED = 14;
	public static final int PEER_COL_PICTURE = 15;
	public static final int PEER_COL_EXP_AVG = 16;
	public static final int PEER_COL_EXPERIENCE = 17;
	public static final int PEER_COL_BLOCKED = 18;
	public static final int PEER_COL_VERSION = 19;
	public static final int PEER_COL_EMAILS = 20;
	public static final int PEER_COL_PHONES = 21;
	public static final int PEER_COL_PREFERENCES_DATE = 22;
	public static final int PEER_COL_VER_NAME = 23;
	public static final int PEER_COL_VER_EMAIL = 24;
	public static final int PEER_COL_CATEG = 25;
	public static final int PEER_COL_REVOKED = 26;
	public static final int PEER_COL_REVOK_INSTR = 27;
	public static final int PEER_COL_REVOK_GIDH = 28;
	public static final int PEER_COL_HIDDEN = 29;
	public static final int PEER_COL_FIRST_PROVIDER_PEER = 29;
	public static final int PEER_COL_ID = PEER_COL_FIELDS_NO_ID;

	//public static final int PEER_COL_NOUPDATE = ?; // replaced by blocked
	/**
	 *  Get the local ID for a given hash_peer_ID
	 * @param global_peer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	/*
	public static String getLocalPeerIDByHash(String global_peer_ID_hash) throws P2PDDSQLException {
		ArrayList<ArrayList<Object>> p =
		Application.db.select("select "+table.peer.peer_ID+" from "+table.peer.TNAME+" where "+table.peer.global_peer_ID_hash+" = ?", 
				new String[]{global_peer_ID_hash});
		if(p.size()==0) return null;
		return Util.getString(p.get(0).get(0));
	}
	*/
	/**
	 *  Get the local ID for a given global_peer_ID
	 * @param global_peer_ID
	 * @param blocked2 
	 * @return
	 * @throws P2PDDSQLException
	 */
	/*
	public static String getLocalPeerID(String global_peer_ID) throws P2PDDSQLException {
		return getLocalPeerID(global_peer_ID, null);
	}
	public static String getLocalPeerID(String global_peer_ID, boolean[] blocked) throws P2PDDSQLException {
		ArrayList<ArrayList<Object>> p =
		Application.db.select("SELECT "+table.peer.peer_ID+","+table.peer.blocked+
				" FROM "+table.peer.TNAME+" WHERE "+table.peer.global_peer_ID+" = ?;", 
				new String[]{global_peer_ID});
		if(p.size()==0) return null;
		if((blocked != null)&&(blocked.length>0))
			blocked[0] = Util.stringInt2bool(p.get(0).get(1), false);
		return Util.getString(p.get(0).get(0));
	}
	*/
	/**
	 *  Get the global ID for a given peer_ID
	 * @param global_peer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	/*
	public static String getGlobalPeerID(String peer_ID) throws P2PDDSQLException {
		ArrayList<ArrayList<Object>> p =
		Application.db.select("select "+table.peer.global_peer_ID+" from "+table.peer.TNAME+" where "+table.peer.peer_ID+" = ?", 
				new String[]{peer_ID});
		if(p.size()==0) return null;
		return Util.getString(p.get(0).get(0));
	}
	*/
}
