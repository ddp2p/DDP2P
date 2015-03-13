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
	/*
	@Deprecated
	static Table buildPeersTable1(String last_sync_date) throws P2PDDSQLException{
		Table recentPeers=new Table();
		recentPeers.name = table.peer.G_TNAME;
		recentPeers.fields=peer.peerFields1;
		recentPeers.fieldTypes=peer.peersFieldsTypes1;
		if(DEBUG) out.print("v");
		String query = "SELECT p."+table.peer.global_peer_ID+",p."+table.peer.name+",p."+table.peer.slogan+",a."+table.peer_address.address+
			",a."+table.peer_address.type+",a."+table.peer_address.arrival_date+"," +
			"n."+table.organization.global_organization_ID+",n."+table.organization.name+",p."+table.peer.hash_alg+",p."+table.peer.signature +
			" FROM "+table.peer.TNAME+" AS p" +
			" LEFT JOIN "+table.peer_address.TNAME+" AS a ON (p."+table.peer.peer_ID+"==a."+table.peer_address.peer_ID+") " +
			" LEFT JOIN "+table.peer_org.TNAME+" AS o ON (p."+table.peer.peer_ID+"==o."+table.peer_org.peer_ID+")" +
			" LEFT JOIN "+table.organization.TNAME+" AS n ON (o."+table.peer_org.organization_ID+"==n."+table.organization.global_organization_ID+")" +
			" WHERE p."+table.peer.broadcastable+" == 1 AND ( ( a."+table.peer_address.arrival_date+" > ? ) OR ( p."+table.peer.arrival_date+" > ? ) ) " +
			" ORDER BY p."+table.peer.arrival_date+" LIMIT 1000;";
		ArrayList<ArrayList<Object>>p_data =
			Application.db.select(query, new String[]{last_sync_date, last_sync_date});
		recentPeers.rows = new byte[p_data.size()][][];
		if(DEBUG) out.println("sent peer rows=: "+p_data.size());
		
		if(DEBUG) out.println("QUERY was: "+query+" with: "+" ( ( a.arrival_date > "+last_sync_date+" ) OR ( p.arrival_date > "+last_sync_date+" ) )");
		 
		for(int i=0; i<p_data.size(); i++) {
			if(DEBUG) out.print("^");
			byte row[][] = new byte[peer.peerFields1.length][];
			//for(int j=0; j<row.length; j++) {row[j] = (String) p_data.get(i).get(j);}
			row[0] = getFieldData(p_data.get(i).get(0));
			row[1] = getFieldData(p_data.get(i).get(1));
			row[2] = getFieldData(p_data.get(i).get(2));
			row[3] = getFieldData(p_data.get(i).get(3));
			row[4] = getFieldData(p_data.get(i).get(4));
			//Calendar cal=Util.CalendargetInstance();
			//cal.setTimeInMillis( Long.parseLong((String)p_data.get(i).get(5)) );
			//row[5] = (Encoder.getGeneralizedTime(cal)).getBytes();
			row[5] = getFieldData(p_data.get(i).get(5));
			row[6] = getFieldData(p_data.get(i).get(6));
			row[7] = getFieldData(p_data.get(i).get(7));
			row[8] = getFieldData(p_data.get(i).get(8));
			row[9] = getFieldData(p_data.get(i).get(9));
			if(DEBUG) out.println(Util.concat(row, ":"));
			recentPeers.rows[i] = row;
		}
		if(DEBUG) out.println("answer: Finished preparing peers");
		return recentPeers;		
	}
	*/
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
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("UpdatePeersTable:getNextDate: lsd="+last_sync_date+" maxDate="+_maxDate);
		String queryPeers;
		ArrayList<ArrayList<Object>> p_data = null, p_addr = null;
		// check first for the max allowed number of peer entries, max_date only set if limit reached
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
				// probably should be valid + 1 (if that is a valid limit)
				_maxDate = Util.getString(p_data.get(valid).get(0)); // setting the limit
				if (DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("UpdatePeersTable: getNextDate: limits reached: "+limitPeersLow+" date="+_maxDate);
			}
			orgs2.add(OrgHandling.ORG_PEERS); // constant to signal that there were peers to add
		}
		
		// check for the same number of maximum peer_address entries, was setting max_date
		p_addr = D_Peer.getAdvertisedPeerAddressNextDate(last_sync_date, _maxDate, limitPeersLow);
		// If less than max rows, was setting the date as the last one
		if ((p_addr.size() <= limitPeersLow)) {
			if (((p_addr.size() > 0) || (total > 0)) && (_maxDate == null)) {
				if (LIMITING_DATE_ALWAYS) {
					if (total > 0) _maxDate = Util.getString(p_data.get(p_data.size()-1).get(0));
					//.......//else _maxDate = Util.getString(p_addr.get(p_addr.size()-1).get(2));
					for (int k = p_addr.size() - 1; k >= 0; k --) {
						String candidate = Util.getString(p_addr.get(k).get(2));
						if (candidate == null) continue;
						if (_maxDate == null) _maxDate = candidate;
						else if (_maxDate.compareTo(candidate) < 0)
							_maxDate = Util.getString(candidate);
					}
				}
				//if(p_addr.size() > 0) _maxDate = Util.getString(p_addr.get(p_addr.size()-1).get(0));
				//else _maxDate = Util.getString(p_data.get(p_data.size()-1).get(0));
				//TODO: if both, take maximum (addr will anyhow be likely higher than peer p_addr)
				//_maxDate = Util.getGeneralizedTime();
			}
			if (DEBUG) System.out.println("UpdatePeersTable:getNextDate: result maxDate="+_maxDate);
			return _maxDate;
		} else {
			// get the last but one last date;
			orgs2.add(OrgHandling.ORG_PEERS);
			int valid_addr = Math.min(limitPeersLow-1, p_addr.size()-1);
			_maxDate = Util.getString(p_addr.get(valid_addr).get(0));
			if (DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("UpdatePeersTable: getNextDate: limits reached: "+limitPeersLow+" date="+_maxDate);
			//if(DEBUG) System.out.println("UpdatePeersTable:getNextDate: result maxDate="+_maxDate);
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
	/*
	@Deprecated
	static String getNextDate2(String last_sync_date, String _maxDate) throws P2PDDSQLException {
		String queryPeers;
		ArrayList<ArrayList<Object>>p_data = null;
		if(_maxDate == null) {
			queryPeers = "SELECT a."+table.peer_address.arrival_date+" AS mindate, COUNT(*) AS c FROM "+table.peer.TNAME+" AS p " +
			" LEFT JOIN "+table.peer_address.TNAME+" AS a ON (p."+table.peer.peer_ID+"==a."+table.peer_address.peer_ID+") " +
			" WHERE p."+table.peer.broadcastable+" == 1 AND ( ( a."+table.peer_address.arrival_date+" > ? ) OR ( p."+table.peer.arrival_date+" > ? ) ) GROUP BY mindate ORDER BY mindate ASC LIMIT 20;";
			p_data = Application.db.select(queryPeers, new String[]{last_sync_date, last_sync_date}, DEBUG);
		}else {
			queryPeers = "SELECT a."+table.peer_address.arrival_date+" AS mindate, COUNT(*) AS c FROM "+table.peer.TNAME+" AS p " +
					" LEFT JOIN "+table.peer_address.TNAME+" AS a ON (p."+table.peer.peer_ID+"==a."+table.peer_address.peer_ID+") " +
					" WHERE p."+table.peer.broadcastable+" == 1 AND ( ( a."+table.peer_address.arrival_date+" > ? ) AND (a."+table.peer_address.arrival_date+" <= ?) ) OR ( ( p."+table.peer.arrival_date+" > ? ) AND ( p."+table.peer.arrival_date+" <= ? ) ) " +
					" GROUP BY mindate ORDER BY mindate ASC LIMIT 20;";			
			p_data = Application.db.select(queryPeers, new String[]{last_sync_date, _maxDate, last_sync_date, _maxDate}, DEBUG);
		}
		if(p_data.size()<=0) return null;
		int valid=0, total=0;
		for(int k=0; k<p_data.size(); k++) {
			total += Integer.parseInt(p_data.get(k).get(1).toString());
		
			if(total>MAX_ROWS) break;
			valid = k;
			
		}
		if(DEBUG) out.println("NextDate after "+last_sync_date+" = "+p_data.get(0).get(0)+" ("+p_data.get(0).get(1)+")");
		return Util.getString(p_data.get(valid).get(0));
	}
	
	public static String getPeerAddresses(String peer_ID, String minDate, String maxDate) throws P2PDDSQLException {
		String result = "";
		final int Q_ADDR=0;
		final int Q_TYPE=1;
		final int Q_ARRIV=2;
		final int Q_CERT=3;
		final int Q_PRI=4;
		String queryAddresses =
				"SELECT a."+table.peer_address.address+",a."+table.peer_address.type+
						",a."+table.peer_address.arrival_date+",a."+table.peer_address.certified+
						",a."+table.peer_address.priority+
			" FROM "+table.peer_address.TNAME+" AS a " +
			" WHERE (a."+table.peer_address.peer_ID+" == ?) AND (a."+table.peer_address.arrival_date+" > ?)" +
				((maxDate!=null)?(" AND (a."+table.peer_address.arrival_date+" <= ?) "):"") +
			" ORDER BY a."+table.peer_address.arrival_date+" DESC LIMIT 1000;";
		ArrayList<ArrayList<Object>>p_data =
			Application.db.select(queryAddresses,
					((maxDate!=null)?(new String[]{peer_ID, minDate, maxDate}):new String[]{peer_ID, minDate}),
					DEBUG);
		for(ArrayList<Object> a: p_data) {
			String addr = Util.getString(a.get(Q_TYPE)) + Address.ADDR_PROT_NET_SEP + Util.getString(a.get(Q_ADDR));
			if(Util.stringInt2bool(a.get(Q_CERT), false))
				addr += Address.PRI_SEP + Util.getString(a.get(Q_PRI));
			if("".equals(result)) result = addr;
			else result = Address.joinAddresses(result, addr);
		}
		return result;
	}
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
	/*
	public static String getPeerAddresses(String peer_ID) throws P2PDDSQLException {
		String result = "";
		String queryAddresses = "SELECT a."+table.peer_address.address+",a."+table.peer_address.type+
				",a."+table.peer_address.arrival_date+",a."+table.peer_address.certified+",a."+table.peer_address.priority+
			" FROM "+table.peer_address.TNAME+" AS a " +
			" WHERE (a."+table.peer_address.peer_ID+" == ?) " +
			" ORDER BY a."+table.peer_address.arrival_date+" LIMIT 1000;";
		ArrayList<ArrayList<Object>>p_data = Application.db.select(queryAddresses, new String[]{peer_ID});
		for(ArrayList<Object> a: p_data) {
			String addr = Util.getString(a.get(1)) + Address.ADDR_PROT_NET_SEP + Util.getString(a.get(0));
			if(Util.stringInt2bool(a.get(3), false))
				addr += Address.ADDR_PROT_NET_SEP + Util.getString(a.get(4));
			if("".equals(result)) result = addr;
			else result = Address.joinAddresses(result, addr);
		}
		return result;
	}
	*/
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
		//boolean DEBUG = true;
		String maxDate = _maxDate[0];
		Table recentPeers = new Table();
		recentPeers.name = net.ddp2p.common.table.peer.G_TNAME;
		recentPeers.fields = D_Peer.peersFields2;
		recentPeers.fieldTypes = D_Peer.peersFieldsTypes2;
		recentPeers.rows = new byte[0][][];
		if (DEBUG) out.print("buildPeersTable2:v");
		//if(maxDate == null) maxDate = last_sync_date;
		//_maxDate[0] = maxDate;
		if (DEBUG) out.println("buildPeersTable2:maxDate after "+last_sync_date+" =: "+maxDate);
		// extract broadcastable in the given data range

		Hashtable<String, String> res = D_Peer.getPeersAdvertisement(last_sync_date, maxDate, limitPeersMax);
		if (sa == null) {
			System.out.println("UpdatePeersTable: __buildPeersTable2: Null sa");
			Util.printCallPath(""); 
			return null;
		}
		if (sa.advertised == null) // &&(sa.advertised.rd!=null))
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
		
		/*
		recentPeers.rows = new byte[p_data.size()][][];
		if (DEBUG) out.println("buildPeersTable2:sent peer rows=: "+p_data.size());
		int i = 0;
		for(ArrayList<Object> p: p_data) {
			// for(int j=0; j<row.length; j++) {row[j] = (String) p_data.get(i).get(j);}
			// Prepare data
			String peer_ID = Util.getString(p.get(0));
			String addresses = getPeerAddresses(peer_ID, last_sync_date, maxDate);
			String orgs = data.D_Peer.getPeerOrgs(peer_ID, null);
			String global_peer_ID = Util.getString(p.get(1));
			String name = Util.getString(p.get(2));
			String slogan = Util.getString(p.get(3));
			String date = Util.getString(p.get(4));
			String hash_alg = Util.getString(p.get(5));
			String signature = Util.getString(p.get(6));
			String broadcastable = Util.getString(p.get(7));
			String version = Util.getString(p.get(9));
			String emails = Util.getString(p.get(10));
			String phones = Util.getString(p.get(11));
			
			if(global_peer_ID.equals(Identity.getMyPeerGID())) {
				D_Peer me = HandlingMyself_Peer.get_myself();
				if(!me.verifySignature()){
					me.component_basic_data.creation_date = Util.CalendargetInstance();
					me.signMe();
					me.storeRequest();
				}
				
				peer_ID = me.peer_ID;
				name = me.component_basic_data.name;
				slogan = me.component_basic_data.slogan;
				date = Encoder.getGeneralizedTime(me.component_basic_data.creation_date);
				hash_alg = me.component_basic_data.hash_alg;
				signature = Util.stringSignatureFromByte(me.getSignature());
				broadcastable = Util.bool2StringInt(me.component_basic_data.broadcastable);
				version = me.component_basic_data.version;
				emails = me.component_basic_data.emails;
				phones = me.component_basic_data.phones;
			}

			// Fill the actual rows
			byte row[][] = new byte[peersFields2.length][];
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
		*/
		//if(DEBUG) out.println("answer: Finished preparing peers");
		if(DEBUG) System.out.println("UpdatePeersTable:buildPeersTable2: result final maxDate="+_maxDate[0]);
		return recentPeers;		
	}
	static Table _buildPeersTable2(SyncAnswer sa, String last_sync_date, String[] _maxDate, boolean justDate, HashSet<String> orgs2, int limitPeersLow, int limitPeersMax) throws P2PDDSQLException{
		//boolean DEBUG = true;
		String maxDate = _maxDate[0];
		Table recentPeers=new Table();
		recentPeers.name = net.ddp2p.common.table.peer.G_TNAME;
		recentPeers.fields = D_Peer.peersFields2;
		recentPeers.fieldTypes = D_Peer.peersFieldsTypes2;
		if (DEBUG) out.print("buildPeersTable2:v");
		//if(maxDate == null) maxDate = last_sync_date;
		//_maxDate[0] = maxDate;
		if (DEBUG) out.println("buildPeersTable2:maxDate after "+last_sync_date+" =: "+maxDate);
		/*
		String _queryPeers =
			"SELECT " +
			" p."+table.peer.peer_ID+
			",p."+table.peer.global_peer_ID+
			",p."+table.peer.name+
			",p."+table.peer.slogan+
			",p."+table.peer.creation_date+
			",p."+table.peer.hash_alg+
			",p."+table.peer.signature+
			",p."+table.peer.broadcastable+
			",MAX(p."+table.peer.arrival_date+",a."+table.peer_address.arrival_date+") " + 
			",p."+table.peer.version+
			",p."+table.peer.emails+
			",p."+table.peer.phones+
				" FROM "+table.peer.TNAME+" AS p " +
				" LEFT JOIN "+table.peer_my_data.TNAME+" AS m ON (p."+table.peer.peer_ID+"==m."+table.peer_my_data.peer_ID+")" +
				" LEFT JOIN "+table.peer_address.TNAME+" AS a ON (p."+table.peer.peer_ID+"==a."+table.peer_address.peer_ID+")" +
				" WHERE ( ( m."+table.peer_my_data.broadcastable+" == 1 ) OR " +
						" ( ( m."+table.peer_my_data.broadcastable+" ISNULL ) AND ( p."+table.peer.broadcastable+" == 1 ) ) ) "+
				" AND ( ( ( a."+table.peer_address.arrival_date+" > ? )" +
						((maxDate!=null)?(" AND ( a."+table.peer_address.arrival_date+" <= ? ) "):"")+"  )" +
					" OR ( ( p."+table.peer.arrival_date+" > ? )" +
					((maxDate!=null)?(" AND ( p."+table.peer.arrival_date+" <= ? ) "):"")+" ) ) " +
				" GROUP BY p."+table.peer.peer_ID+" LIMIT "+limitPeersMax+";";
		*/
		ArrayList<D_Peer> p_data = D_Peer.getPeersChunk(last_sync_date, maxDate, limitPeersMax);
		recentPeers.rows = new byte[p_data.size()][][];
		if(DEBUG) out.println("buildPeersTable2:sent peer rows=: "+p_data.size());
		int i = 0;
		for(D_Peer p: p_data) {
			// for(int j=0; j<row.length; j++) {row[j] = (String) p_data.get(i).get(j);}
			// Prepare data
			String peer_ID = p.getLIDstr_keep_force(); //Util.getString(p.get(0));
			String addresses = getPeerAddresses(p);//(peer_ID, last_sync_date, maxDate);
			String orgs = net.ddp2p.common.data.D_Peer.getPeerOrgs(peer_ID, null);
			String global_peer_ID = p.getGID(); //Util.getString(p.get(1));
			String name = p.getName(); //Util.getString(p.get(2));
			String slogan = p.getSlogan(); //Util.getString(p.get(3));
			String date = p.getCreationDate(); //Util.getString(p.get(4));
			String hash_alg = p.getHashAlgDB();//Util.getString(p.get(5));
			String signature = p.getSignatureDB(); //Util.getString(p.get(6));
			String broadcastable = p.getBroadcastableStr();//Util.getString(p.get(7));
			String version = p.getVersion(); //Util.getString(p.get(9));
			String emails = p.getEmail(); //Util.getString(p.get(10));
			String phones = p.getPhones(); //Util.getString(p.get(11));
			
			if (global_peer_ID.equals(Identity.getMyPeerGID())) {
				D_Peer me = HandlingMyself_Peer.get_myself_with_wait();
				if (!me.verifySignature()){
					me.setCreationDate(); //.component_basic_data.creation_date = Util.CalendargetInstance();
					me.signMe();
					me.storeRequest();
				}
				
				peer_ID = me.getLIDstr();
				name = me.component_basic_data.name;
				slogan = me.component_basic_data.slogan;
				date = me.getCreationDate(); //Encoder.getGeneralizedTime(me.getCreationTime());
				hash_alg = me.component_basic_data.hash_alg;
				signature = Util.stringSignatureFromByte(me.getSignature());
				broadcastable = Util.bool2StringInt(me.component_basic_data.broadcastable);
				version = me.component_basic_data.version;
				emails = me.component_basic_data.emails;
				phones = me.component_basic_data.phones;
			}
			/*
			if(true) { // verify sent peer signature & on failure refill data from D_PeerAddress
				// Verify signature of peer
				D_PeerAddress peer_data = new D_PeerAddress(global_peer_ID,0,false);
				if(DEBUG) out.println("UpdatePeersTable:buildPeersTable2: prepare: "+peer_data);
				if(!peer_data.verifySignature()) { //Util.verifySign(peer_data, pk, peer_signature)) {
					if(_DEBUG) out.println("UpdatePeersTable:buildPeersTable2: dir sign verification failed: "+peer_data.name);
					if(DEBUG) out.println("UpdatePeersTable:buildPeersTable2: dir sign verification failed: "+peer_data);
					//if(!DD.ACCEPT_UNSIGNED_PEERS) continue;
				}else{
					if(DEBUG) out.println("UpdatePeersTable:buildPeersTable2: dir sign verification passed");
				}			
				
				
				if(DEBUG) out.println("UpdatePeersTable:buildPeersTable2: will get peer orgs from: "+orgs);
				peer_data.served_orgs = table.peer_org.peerOrgsFromString(orgs);
				byte[] peer_signature = Util.byteSignatureFromString(signature);
				// version, globalID, name, slogan, creation_date, address*, broadcastable, signature_alg, served_orgs, signature*
				if(peer_signature != null) {
					peer_data.version = version;
					peer_data.globalID = global_peer_ID;
					peer_data.name = name;
					peer_data.slogan = slogan;
					peer_data.creation_date = Util.getCalendar(date);
					peer_data.signature = peer_signature;
					peer_data.signature_alg = D_PeerAddress.getHashAlgFromString(hash_alg);
					//peer_data.signature = new byte[0];
					peer_data.broadcastable = Util.stringInt2bool(broadcastable, false);//"1".equals(broadcastable);
					if(DEBUG) out.println("UpdatePeersTable:buildPeersTable2: handling row ["+i+"] "+peer_data);
					ciphersuits.PK pk = ciphersuits.Cipher.getPK(global_peer_ID);
					if(DEBUG) out.println("UpdatePeersTable:buildPeersTable2: pk= "+pk+" sign=["+peer_signature.length+"]"+Util.byteToHexDump(peer_signature)+" text="+signature);
					
					if(DEBUG) out.println("UpdatePeersTable:buildPeersTable2: prepared now: "+peer_data);
	
					if(!peer_data.verifySignature()) { //Util.verifySign(peer_data, pk, peer_signature)) {
						if(_DEBUG) out.println("UpdatePeersTable:buildPeersTable2: sign verification failed: "+peer_data);
						//if(!DD.ACCEPT_UNSIGNED_PEERS) continue;
					}else{
						if(DEBUG) out.println("UpdatePeersTable:buildPeersTable2: sign verification passed");
					}
				}else{
					if(DEBUG) out.println("UpdatePeersTable:buildPeersTable2: signature absent");
				}
			}
			*/

			// Fill the actual rows
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
		//if(DEBUG) out.println("answer: Finished preparing peers");
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
	 * @param peer
	 * @param tab
	 * @param filteredID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_Peer integratePeersTable2(ASNSyncPayload asa, D_Peer peer, Table tab, String filteredID) throws P2PDDSQLException{
		//boolean DEBUG = true;
		if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: START");
		
		// There is a limit for the number of peers, probably to avoid some DoS attack
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
			/*
			if ((global_peer_ID == null) || (global_peer_ID.equals(HandlingMyself_Peer.getMyPeerGID()))) {
				if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: handling myself, skip ");
				continue;
			}
			*/
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
			//String org_name = Util.getBString(tab.rows[i][7]);
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
			// version, globalID, name, slogan, creation_date, address*, broadcastable, signature_alg, served_orgs, signature*
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
			
			// Verify signature of peer
			if (received.getSignature() != null) {
				//peer_data.signature = new byte[0];
				if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: handling row ["+i+"] "+received);
				net.ddp2p.ciphersuits.PK pk = net.ddp2p.ciphersuits.Cipher.getPK(global_peer_ID);
				if (pk == null) System.err.println("UpdatePeersTable:integratePeersTable2: pk fail: \n"+received);
				if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: pk= "+pk+" sign=["+peer_signature.length+"]"+Util.byteToHexDump(peer_signature)+" text="+peer_sign);
				//if(!Util.verifySign(peer_data, pk, peer_signature)) {
				if (!received.verifySignature()) { //  Util.verifySign(peer_data, pk, peer_signature)) {
					if (_DEBUG || DD.DEBUG_TODO) System.err.println("UpdatePeersTable:integratePeersTable2: sign verification failed for "+received.getName());
					if (DEBUG) System.err.println("UpdatePeersTable:integratePeersTable2: sign verification failed for "+received);
					if (!DD.ACCEPT_DATA_FROM_UNSIGNED_PEERS) continue;
					received.setSignature(null);
				} else {
					if(_DEBUG) out.println("UpdatePeersTable:integratePeersTable2: sign verification passed for "+received.getName());					
				}
			}
			
			// Verify signature of peer
			D_Peer peer_data = D_Peer.getPeerByGID_or_GIDhash(global_peer_ID, null, true, true, true, null);
			//peer_data = D_Peer.getPeerByPeer_Keep(peer_data);
			if (peer_data.getLID() <= 0) {
				peer_data.setUsed_setDirty(_used); //.component_preferences.used = _used;
				peer_data.setProvider(peer.getLIDstr());// component_local_agent_state.first_provider_peer = peer.peer_ID;
				
				peer_data.setBlocked_setDirty(//.component_preferences.blocked =
						(filteredID != null) ? DD.BLOCK_NEW_ARRIVING_PEERS_ANSWERING_ME:
							DD.BLOCK_NEW_ARRIVING_PEERS_FORWARDED_TO_ME);
			}
			if ( ! peer_data.verifySignature()) { //  Util.verifySign(peer_data, pk, peer_signature)) {
				if (DEBUG || DD.DEBUG_TODO) System.err.println("UpdatePeersTable:integratePeersTable2: orig verification failed for "+peer_data.getName());
				//if (DEBUG) System.err.println("UpdatePeersTable:integratePeersTable2: sign verification failed for "+received);
			} else {
				if(DEBUG) out.println("UpdatePeersTable:integratePeersTable2: orig verification passed for "+peer_data);					
			}
			peer_data.loadRemote(received, null, null);
			if (! peer_data.verifySignature()) { //  Util.verifySign(peer_data, pk, peer_signature)) {
				if (_DEBUG || DD.DEBUG_TODO) System.err.println("UpdatePeersTable:integratePeersTable2: fin verification failed for "+peer_data.getName());
				//if (DEBUG) System.err.println("UpdatePeersTable:integratePeersTable2: sign verification failed for "+received);
			} else {
				if(DEBUG) out.println("UpdatePeersTable:integratePeersTable2: fin verification passed for "+peer_data);					
			}
			/*// may have to controll addresses, to defend from DOS based on false addresses
//			peer_data.address = new TypedAddress[addresses_l.length];
//			for(int k=0; k<addresses_l.length; k++) {
//				peer_data.address[k] = new TypedAddress();
//				String taddr[] = addresses_l[k].split(Pattern.quote(Address.ADDR_PART_SEP),2);
//				if(taddr.length < 2) continue;
//				peer_data.address[k].type = taddr[0];
//				peer_data.address[k].address = taddr[1];
//			}
//			
//			if((global_peer_ID==null) || (global_peer_ID.equals(DD.getMyPeerID()))){
//				if(DEBUG) out.println("UpdatePeersTable:integratePeersTable2: handling myself, skip ");
//				continue;
//			}
			*/
			//long peer_ID = D_PeerAddress.storeVerified(global_peer_ID, name, adding_date, slogan, _used, _broadcastable, peer_hash_alg, peer_signature,
			//		creation_date, null, version);
			peer_data.setArrivalDate(null, adding_date);
			long peer_ID = peer_data.storeRequest_getID();
			//peer_data.storeRequest(); // needed in case an ID was present
			peer_data.releaseReference();
			if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: got local peer_ID: "+peer_ID);
			//integratePeerOrgs(peer_data.served_orgs, peer_ID, adding_date);

			
			if (DEBUG) out.println("UpdatePeersTable:integratePeersTable2: got addresses: "+addresses);
			//integratePeerAddresses(peer_ID, typed_address, adding__date);
			if (filteredID != null) return peer_data;
			/*
			String[]addresses_l = Address.split(addresses);
			if(DEBUG)out.println("UpdatePeersTable:integratePeersTable2: got addresses #: "+addresses_l.length);
			for(int j=0; j<addresses_l.length; j++) {
				adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
				if(DEBUG)out.println("got addr: "+addresses_l[j]);
				String[] t_address = addresses_l[j].split(Pattern.quote(Address.ADDR_TYPE_SEP));
				//out.println("got addr: "+addresses_l[j]);
				String type = t_address[0];
				String address = null;
				if(t_address.length>1) address = t_address[1];
				adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
				long address_ID = UpdateMessages.get_peer_addresses_ID(address, type, peer_ID, adding_date);
				if(DEBUG)out.println("UpdatePeersTable:integratePeersTable2: got local address_ID: "+address_ID);
			}
			*/
		}
		return null;
	}
	/*
	//public static String addressStringFromArray(hds.TypedAddress[] address) {return null;}
	public static void integratePeerAddresses(long peer_ID, TypedAddress[] addresses_l, Calendar adding__date ) throws P2PDDSQLException{
		if(DEBUG)out.println("UpdatePeersTable:integratePeersTable2: got addresses #: "+addresses_l.length);
		if(addresses_l==null) return;
		for(int j=0; j<addresses_l.length; j++) {
			String adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
			if(addresses_l[j] == null){
				if(DD.WARN_ABOUT_OTHER)out.println("UpdatePeersTable:integratePeersTable2: from peer:"+peer_ID+":#"+j+"/"+addresses_l.length+":got addr: "+addresses_l[j]);
				for(int k=0; k<addresses_l.length; k++)
					if(DD.WARN_ABOUT_OTHER)out.println("UpdatePeersTable:integratePeersTable2: list from peer:"+peer_ID+":#"+j+"/"+addresses_l.length+":got addr: ["+k+"]="+addresses_l[k]);
				continue;
			}
			if(DEBUG)out.println("got addr: "+addresses_l[j]);
			//String[] t_address = addresses_l[j].split(Pattern.quote(Address.ADDR_TYPE_SEP));
			//out.println("got addr: "+addresses_l[j]);
			String type = addresses_l[j].type; //t_address[0];
			String address = addresses_l[j].address;//null;
			//if(t_address.length>1) address = t_address[1];
			adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
			long address_ID = D_Peer.get_peer_addresses_ID(address, type, peer_ID, adding_date,
					addresses_l[j].certified, addresses_l[j].priority);
			if(DEBUG)out.println("UpdatePeersTable:integratePeersTable2: got local address_ID: "+address_ID);
		}
	}
	
	public static void integratePeerAddresses(long peer_ID, String[] addresses_l, Calendar adding__date ) throws P2PDDSQLException{
		if(DEBUG)out.println("UpdatePeersTable:integratePeersTable2: got addresses #: "+addresses_l.length);
		for(int j=0; j<addresses_l.length; j++) {
			String adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
			if(DEBUG)out.println("got addr: "+addresses_l[j]);
			String[] t_address = addresses_l[j].split(Pattern.quote(Address.ADDR_PROT_NET_SEP));
			//out.println("got addr: "+addresses_l[j]);
			String type = t_address[0];
			String address = null;
			boolean certificate = false;
			int priority = 0;
			if(t_address.length>1){
				String ta[] = t_address[1].split(Pattern.quote(TypedAddress.PRI_SEP),2);
				address = ta[0];
				if(ta.length>1) {
					certificate = true;
					priority = Util.get_int(ta[1]);
				}
			}
			adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
			long address_ID = D_Peer.get_peer_addresses_ID(address, type, peer_ID, adding_date,
					certificate, priority);
			if(DEBUG)out.println("UpdatePeersTable:integratePeersTable2: got local address_ID: "+address_ID);
		}
	}
	*/
	
	public static void main(String args[]){
		try {
			Application.db = new DBInterface(Application.DEFAULT_DELIBERATION_FILE);
		} catch (P2PDDSQLException e1) {
			// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Got="+a);
	}
}
