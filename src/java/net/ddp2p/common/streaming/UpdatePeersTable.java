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
package net.ddp2p.common.streaming;
import static java.lang.System.out;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.regex.Pattern;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.hds.SyncAnswer;
import net.ddp2p.common.hds.Table;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class UpdatePeersTable {
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static int MAX_ROWS = 30;
	private static final boolean LIMITING_DATE_ALWAYS = false;
	public static byte[] getFieldData(Object object) {
		return UpdateMessages.getFieldData(object);
	}
	/**
	 * 
	 * @param last_sync_date : The date from which we start building the result
	 * @param _maxDate : return of the date up to which the data is harvested
	 * @param justDate : whether to answer just the date or also to build a result
	 * @param orgs : set of organizations contained in new peers
	 * @param limitPeersLow  : how many peers
	 * @param limitPeersMax
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static Table buildPeersTable(SyncAnswer sa, String last_sync_date, String[] _maxDate, boolean justDate, HashSet<String> orgs, int limitPeersLow, int limitPeersMax) throws P2PDDSQLException{
		return buildPeersTable2(sa, last_sync_date, _maxDate, justDate, orgs, limitPeersLow, limitPeersMax);
	}
	/**
	 * returns the cache of the peer with GID given by "filteredID"
	 * 
	 * @param asa
	 * @param peer
	 * @param tab
	 * @param filteredID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_Peer integratePeersTable(ASNSyncPayload asa, D_Peer peer, Table tab, String filteredID) throws P2PDDSQLException{
		if (DEBUG) out.println("UpdatePeersTable:integratePeersTable: now");
		return integratePeersTable2(asa, peer, tab, filteredID);
	}
	/**
	 * Get the next date in peer or peer_address
	 * @param last_sync_date
	 * @param _maxDate
	 * @param orgs2 
	 * @param limitPeersLow     like 30 (or MAX_ROW)
	 * @return null iff nothing found
	 * @throws P2PDDSQLException
	 */
	static String getNextDate(String last_sync_date, String _maxDate, HashSet<String> orgs2, int limitPeersLow) throws P2PDDSQLException {
		if (DEBUG) System.out.println("UpdatePeersTable:getNextDate: lsd="+last_sync_date+" maxDate="+_maxDate);
		String queryPeers;
		ArrayList<ArrayList<Object>> p_data = null, p_addr = null;
		p_data = D_Peer.getAdvertisedPeerNextDate(last_sync_date, _maxDate, limitPeersLow);
		int valid=0, total=0;
		if (p_data.size() > 0){
			for(int k=0; k<p_data.size(); k++) {
				total += Integer.parseInt(p_data.get(k).get(1).toString());
				if (total>limitPeersLow) break;
				valid = k;
			}
			if (DEBUG) out.println("NextDate after "+last_sync_date+" = "+p_data.get(0).get(0)+" ("+p_data.get(0).get(1)+")");
			if (DEBUG) out.println("LastDate after "+last_sync_date+" = "+p_data.get(valid).get(0)+" ("+p_data.get(valid).get(1)+")");
			if (total > limitPeersLow){
				_maxDate = Util.getString(p_data.get(valid).get(0)); 
				if (DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("UpdatePeersTable: getNextDate: limits reached: "+limitPeersLow+" date="+_maxDate);
			}
			orgs2.add(OrgHandling.ORG_PEERS); 
		}
		p_addr = D_Peer.getAdvertisedPeerAddressNextDate(last_sync_date, _maxDate, limitPeersLow);
		if ((p_addr.size() <= limitPeersLow)) {
			if (((p_addr.size() > 0) || (total > 0)) && (_maxDate == null)) {
				if (LIMITING_DATE_ALWAYS) {
					if (total > 0) _maxDate = Util.getString(p_data.get(p_data.size()-1).get(0));
					for (int k = p_addr.size() - 1; k >= 0; k --) {
						String candidate = Util.getString(p_addr.get(k).get(2));
						if (candidate == null) continue;
						if (_maxDate == null) _maxDate = candidate;
						else if (_maxDate.compareTo(candidate) < 0)
							_maxDate = Util.getString(candidate);
					}
				}
			}
			if (DEBUG) System.out.println("UpdatePeersTable:getNextDate: result maxDate="+_maxDate);
			return _maxDate;
		} else {
			orgs2.add(OrgHandling.ORG_PEERS);
			int valid_addr = Math.min(limitPeersLow-1, p_addr.size()-1);
			_maxDate = Util.getString(p_addr.get(valid_addr).get(0));
			if (DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("UpdatePeersTable: getNextDate: limits reached: "+limitPeersLow+" date="+_maxDate);
			return _maxDate;
		}
	}
	/**
	 * No longer used. Deprecated!
	 * @param last_sync_date
	 * @param _maxDate
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String getPeerAddresses(D_Peer p) {
		String result = "";
		for (Address a : p.shared_addresses) {
			String addr = a.getExtendedProtocol() + Address.ADDR_PROT_NET_SEP + a.getSocketAddress();
			if(a.certified)
				addr += Address.PRI_SEP + a.priority;
			if("".equals(result)) result = addr;
			else result = Address.joinAddresses(result, addr);
		}
		return result;
	}
	/**
	 * 
	 * @param last_sync_date
	 * @param _maxDate
	 * @param justDate
	 * @param orgs2 
	 * @param limitPeersLow low as 5
	 * @param limitPeersMax can be 1000
	 * @return
	 * @throws P2PDDSQLException
	 */
	static Table buildPeersTable2(SyncAnswer sa, String last_sync_date, String[] _maxDate, boolean justDate, HashSet<String> orgs2, int limitPeersLow, int limitPeersMax) throws P2PDDSQLException{
		if (justDate) {
			_maxDate[0] = getNextDate(last_sync_date, _maxDate[0], orgs2, limitPeersLow);
			if (DEBUG) System.out.println("UpdatePeersTable:buildPeersTable2: result justDate maxDate="+_maxDate[0]);
			return null;
		}
		if (DD.STREAMING_TABLE_PEERS) {
			return _buildPeersTable2(sa, last_sync_date, _maxDate, justDate, orgs2, limitPeersLow, limitPeersMax);
		} else {
			return __buildPeersTable2(sa, last_sync_date, _maxDate, justDate, orgs2, limitPeersLow, limitPeersMax);			
		}
	}
	/**
	 * Only advertise
	 * @param sa
	 * @param last_sync_date
	 * @param _maxDate
	 * @param justDate
	 * @param orgs2
	 * @param limitPeersLow
	 * @param limitPeersMax
	 * @return
	 * @throws P2PDDSQLException
	 */
	static Table __buildPeersTable2(SyncAnswer sa, String last_sync_date, String[] _maxDate, boolean justDate, HashSet<String> orgs2, int limitPeersLow, int limitPeersMax) throws P2PDDSQLException{
		String maxDate = _maxDate[0];
		Table recentPeers = new Table();
		recentPeers.name = net.ddp2p.common.table.peer.G_TNAME;
		recentPeers.fields = D_Peer.peersFields2;
		recentPeers.fieldTypes = D_Peer.peersFieldsTypes2;
		recentPeers.rows = new byte[0][][];
		if (DEBUG) out.print("buildPeersTable2:v");
		if (DEBUG) out.println("buildPeersTable2:maxDate after "+last_sync_date+" =: "+maxDate);
		Hashtable<String, String> res = D_Peer.getPeersAdvertisement(last_sync_date, maxDate, limitPeersMax);
		if (sa == null) {
			System.out.println("UpdatePeersTable: __buildPeersTable2: Null sa");
			Util.printCallPath(""); 
			return null;
		}
		if (sa.advertised == null) 
			sa.advertised = new SpecificRequest();
		if (sa.advertised == null) {
			System.out.println("UpdatePeersTable: __buildPeersTable2: Null sa advertised");
			Util.printCallPath(""); 
			return null;
		}
		if (sa.advertised.peers == null) {
			System.out.println("UpdatePeersTable: __buildPeersTable2: Null peers advertised");
			Util.printCallPath(""); 
			return null;
		}
		sa.advertised.peers.putAll(res);
		if(DEBUG) System.out.println("UpdatePeersTable:buildPeersTable2: result final maxDate="+_maxDate[0]);
		return recentPeers;		
	}
	static Table _buildPeersTable2(SyncAnswer sa, String last_sync_date, String[] _maxDate, boolean justDate, HashSet<String> orgs2, int limitPeersLow, int limitPeersMax) throws P2PDDSQLException{
		String maxDate = _maxDate[0];
		Table recentPeers=new Table();
		recentPeers.name = net.ddp2p.common.table.peer.G_TNAME;
		recentPeers.fields = D_Peer.peersFields2;
		recentPeers.fieldTypes = D_Peer.peersFieldsTypes2;
		if (DEBUG) out.print("buildPeersTable2:v");
		if (DEBUG) out.println("buildPeersTable2:maxDate after "+last_sync_date+" =: "+maxDate);
		ArrayList<D_Peer> p_data = D_Peer.getPeersChunk(last_sync_date, maxDate, limitPeersMax);
		recentPeers.rows = new byte[p_data.size()][][];
		if(DEBUG) out.println("buildPeersTable2:sent peer rows=: "+p_data.size());
		int i = 0;
		for(D_Peer p: p_data) {
			String peer_ID = p.getLIDstr_keep_force(); 
			String addresses = getPeerAddresses(p);
			String orgs = net.ddp2p.common.data.D_Peer.getPeerOrgs(peer_ID, null);
			String global_peer_ID = p.getGID(); 
			String name = p.getName(); 
			String slogan = p.getSlogan(); 
			String date = p.getCreationDate(); 
			String hash_alg = p.getHashAlgDB();
			String signature = p.getSignatureDB(); 
			String broadcastable = p.getBroadcastableStr();
			String version = p.getVersion(); 
			String emails = p.getEmail(); 
			String phones = p.getPhones(); 
			if (global_peer_ID.equals(Identity.getMyPeerGID())) {
				D_Peer me = HandlingMyself_Peer.get_myself_with_wait();
				if (!me.verifySignature()){
					me.setCreationDate(); 
					me.signMe();
					me.storeRequest();
				}
				peer_ID = me.getLIDstr();
				name = me.component_basic_data.name;
				slogan = me.component_basic_data.slogan;
				date = me.getCreationDate(); 
				hash_alg = me.component_basic_data.hash_alg;
				signature = Util.stringSignatureFromByte(me.getSignature());
				broadcastable = Util.bool2StringInt(me.component_basic_data.broadcastable);
				version = me.component_basic_data.version;
				emails = me.component_basic_data.emails;
				phones = me.component_basic_data.phones;
			}
			byte row[][] = new byte[D_Peer.peersFields2.length][];
			row[0] = getFieldData(global_peer_ID);
			row[1] = getFieldData(name);
			row[2] = getFieldData(slogan);
			row[3] = getFieldData(addresses);
			row[4] = getFieldData(date);
			row[5] = getFieldData(orgs);
			row[6] = getFieldData(hash_alg);
			row[7] = getFieldData(signature);
			row[8] = getFieldData(broadcastable);
			row[9] = getFieldData(version);
			row[10] = getFieldData(emails);
			row[11] = getFieldData(phones);
			if(DEBUG) out.println("UpdatePeersTable: buildPT2: got row:"+concatRowFields(row, ":"));
			recentPeers.rows[i] = row;i++;
		}
		if(DEBUG) System.out.println("UpdatePeersTable:buildPeersTable2: result final maxDate="+_maxDate[0]);
		return recentPeers;		
	}
	private static String concatRowFields(byte[][] row, String sep) {
		String result = "";
		if(row==null) return null;
		for(int k = 0; k<row.length; k++) result += sep+((row[k]==null)?"null":new String(row[k]));
		return result;
	}
	/**
	 * Should return cache of filtered peer
	 * @param asa
	 * @param peer (used only for setting provider peer)
	 * @param tab
	 * @param filteredID (if present, then only extract this one! just in case sender is blocked)
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_Peer integratePeersTable2(ASNSyncPayload asa, D_Peer peer, Table tab, String filteredID) throws P2PDDSQLException{
		if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: START");
		if ( D_Peer.getPeersCount() > DD.ACCEPT_STREAMING_UPTO_MAX_PEERS) {
			Application_GUI.warning(Util.__("Upper limit for number of peers is reached:")+DD.ACCEPT_STREAMING_UPTO_MAX_PEERS+
					"\n "+("You can delete some peers, or raise the limit in Control Panel."),
					Util.__("Upper Limit For Peers Reached"));
			return null;
		}
		Calendar adding__date = Util.CalendargetInstance();
		String adding_date = Encoder.getGeneralizedTime(adding__date);
		for (int i = 0; i < tab.rows.length; i ++) {
			if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: handling row ["+i+"] ="+Table.printRow(tab.rows[i]));
			String global_peer_ID = Util.getBString(tab.rows[i][0]);
			if (global_peer_ID == null) continue;
			if ((filteredID != null) && (!filteredID.equals(global_peer_ID))) continue;
			if (tab.rows[i].length <= 9) {
				if (DEBUG || DD.DEBUG_TODO) Application_GUI.warning(Util.__("Peer has wrong message format"), Util.__("Old version peer"));
				if (DEBUG || DD.DEBUG_TODO) System.out.println("UpdatePeersTable: integratePeersTable2: old version of peers, too few columns");
				continue;
			}
			String emails = null, phones = null;
			String name = Util.getBString(tab.rows[i][1]);
			String slogan = Util.getBString(tab.rows[i][2]);
			String addresses = Util.getBString(tab.rows[i][3]);
			String creation_date = Util.getBString(tab.rows[i][4]);
			String global_organizationIDs = Util.getBString(tab.rows[i][5]);
			String peer_hash_alg = Util.getBString(tab.rows[i][6]);
			String peer_sign = Util.getBString(tab.rows[i][7]);
			String broadcastable = Util.getBString(tab.rows[i][8]);
			String version = Util.getBString(tab.rows[i][9]);
			if (tab.rows[i].length > 10) emails = Util.getBString(tab.rows[i][10]);
			if (tab.rows[i].length > 11) phones = Util.getBString(tab.rows[i][11]);
			byte[] peer_signature = null;
			try {
				if (peer_sign != null) peer_signature = Util.byteSignatureFromString(peer_sign);
			} catch (Exception e){e.printStackTrace();}
			boolean _broadcastable = Util.stringInt2bool(broadcastable, false);
			boolean _used = DD.DEFAULT_RECEIVED_PEERS_ARE_USED;
			/**
			 * Fill the peer_data with the received one
			 */
			D_Peer received = D_Peer.getEmpty();
			received.setGID(global_peer_ID, null);
			received.served_orgs = net.ddp2p.common.table.peer_org.peerOrgsFromString(global_organizationIDs);
			if (peer_signature == null) {
				if (DEBUG || DD.DEBUG_TODO) out.println("UpdatePeersTable:integratePeersTable2: invalid empty signature");
				if (!DD.ACCEPT_UNSIGNED_PEERS_FROM_TABLES) continue;				
			} else {
				received.setSignature(peer_signature);
			}
			received.component_basic_data.version = version;
			received.component_basic_data.emails = emails;
			received.component_basic_data.phones = phones;
			received.component_basic_data.globalID = global_peer_ID;
			received.component_basic_data.name = name;
			received.component_basic_data.slogan = slogan;
			received.setCreationDateNoDirty(null, creation_date);
			received.component_basic_data.broadcastable = _broadcastable;
			received.signature_alg = D_Peer.D_Peer_Basic_Data.getHashAlgFromString(peer_hash_alg);
			if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: got addresses: "+addresses);
			ArrayList<Address> typed_address = Address.getAddress(addresses);
			received.shared_addresses = typed_address;
			if (received.getSignature() != null) {
				if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: handling row ["+i+"] "+received);
				net.ddp2p.ciphersuits.PK pk = net.ddp2p.ciphersuits.Cipher.getPK(global_peer_ID);
				if (pk == null) System.err.println("UpdatePeersTable:integratePeersTable2: pk fail: \n"+received);
				if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: pk= "+pk+" sign=["+peer_signature.length+"]"+Util.byteToHexDump(peer_signature)+" text="+peer_sign);
				if (!received.verifySignature()) { 
					if (_DEBUG || DD.DEBUG_TODO) System.err.println("UpdatePeersTable:integratePeersTable2: sign verification failed for "+received.getName());
					if (DEBUG) System.err.println("UpdatePeersTable:integratePeersTable2: sign verification failed for "+received);
					if (!DD.ACCEPT_DATA_FROM_UNSIGNED_PEERS) continue;
					received.setSignature(null);
				} else {
					if(_DEBUG) out.println("UpdatePeersTable:integratePeersTable2: sign verification passed for "+received.getName());					
				}
			}
			D_Peer peer_data = D_Peer.getPeerByGID_or_GIDhash(global_peer_ID, null, true, true, true, null);
			if (peer_data.getLID() <= 0) {
				peer_data.setUsed_setDirty(_used); 
				peer_data.setProvider(peer.getLIDstr());
				peer_data.setBlocked_setDirty(
						(filteredID != null) ? DD.BLOCK_NEW_ARRIVING_PEERS_ANSWERING_ME:
							DD.BLOCK_NEW_ARRIVING_PEERS_FORWARDED_TO_ME);
			}
			if ( ! peer_data.verifySignature()) { 
				if (DEBUG || DD.DEBUG_TODO) System.err.println("UpdatePeersTable:integratePeersTable2: orig verification failed for "+peer_data.getName());
			} else {
				if(DEBUG) out.println("UpdatePeersTable:integratePeersTable2: orig verification passed for "+peer_data);					
			}
			peer_data.loadRemote(received, null, null);
			if (! peer_data.verifySignature()) { 
				if (_DEBUG || DD.DEBUG_TODO) System.err.println("UpdatePeersTable:integratePeersTable2: fin verification failed for "+peer_data.getName());
			} else {
				if(DEBUG) out.println("UpdatePeersTable:integratePeersTable2: fin verification passed for "+peer_data);					
			}
			/*// may have to controll addresses, to defend from DOS based on false addresses
			*/
			peer_data.setArrivalDate(null, adding_date);
			long peer_ID = peer_data.storeRequest_getID();
			peer_data.releaseReference();
			if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: got local peer_ID: "+peer_ID);
			if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: got addresses: "+addresses);
			if (filteredID != null) return peer_data;
		}
		return null;
	}
	public static void main(String args[]){
		try {
			Application.setDB(new DBInterface(Application.DEFAULT_DELIBERATION_FILE));
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
			return;
		}
		SyncAnswer sa = new SyncAnswer();
		String last_sync_date = Encoder.getGeneralizedTime(Util.getCalendar("00000000000000.000Z"));
		String[] _maxDate  = new String[]{Util.getGeneralizedTime()};
		boolean justDate=false;
		HashSet<String> orgs = new HashSet<String>();
		int limitPeersLow = 100;
		int limitPeersMax = 1000;
		Table a=null;
		try {
			a = UpdatePeersTable.buildPeersTable(sa, last_sync_date, _maxDate, justDate, orgs, limitPeersLow, limitPeersMax);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		System.out.println("Got="+a);
	}
}
