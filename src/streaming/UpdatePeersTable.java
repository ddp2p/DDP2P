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

package streaming;
import static java.lang.System.out;
import hds.ASNSyncPayload;
import hds.Address;
import hds.Table;
import hds.TypedAddress;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.regex.Pattern;

import util.DBInterface;
import util.Util;
import ASN1.Encoder;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import config.Identity;
import data.D_PeerAddress;

public class UpdatePeersTable {
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static int MAX_ROWS = 30;
	//private static final int MAX_ADDR = 40;
	public static final String peersFields2[] = new String[]{table.peer.global_peer_ID,table.peer.name,table.peer.slogan,table.peer_address.address,table.peer.creation_date,table.organization.Gs_global_organization_ID,table.peer.hash_alg,table.peer.signature,table.peer.broadcastable,table.peer.version,table.peer.emails,table.peer.phones};
	public static final String peersFieldsTypes2[] = new String[]{"TEXT","TEXT","TEXT","TEXT","TEXT","TEXT","TEXT","TEXT","TEXT","TEXT"};
	private static final boolean LIMITING_DATE_ALWAYS = false;
	
	public static byte[] getFieldData(Object object) {
		return UpdateMessages.getFieldData(object);
	}
	public static Table buildPeersTable(String last_sync_date, String[] _maxDate, boolean justDate, HashSet<String> orgs, int limitPeersLow, int limitPeersMax) throws P2PDDSQLException{
		return buildPeersTable2(last_sync_date, _maxDate, justDate, orgs, limitPeersLow, limitPeersMax);
	}
	public static D_PeerAddress integratePeersTable(ASNSyncPayload asa, D_PeerAddress peer, Table tab, String filteredID) throws P2PDDSQLException{
		if(DEBUG) out.println("UpdatePeersTable:integratePeersTable: now");
		return integratePeersTable2(asa, peer, tab, filteredID);
	}
	@Deprecated
	static Table buildPeersTable1(String last_sync_date) throws P2PDDSQLException{
		Table recentPeers=new Table();
		recentPeers.name = table.peer.G_TNAME;
		recentPeers.fields=DD.peerFields1;
		recentPeers.fieldTypes=DD.peersFieldsTypes1;
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
			byte row[][] = new byte[DD.peerFields1.length][];
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
		if(DEBUG) System.out.println("UpdatePeersTable:getNextDate: lsd="+last_sync_date+" maxDate="+_maxDate);
		String queryPeers;
		ArrayList<ArrayList<Object>>p_data = null, p_addr = null;
		// check first for the max allowed number of peer entries, max_date only set if limit reached
		if(_maxDate == null) {
			queryPeers = "SELECT p."+table.peer.arrival_date+" AS mindate, COUNT(*) AS c FROM "+table.peer.TNAME+" AS p " +
			" LEFT JOIN "+table.peer_my_data.TNAME+" AS m ON (p."+table.peer.peer_ID+"==m."+table.peer_my_data.peer_ID+") " +
			" WHERE " +
			" ( ( m."+table.peer_my_data.broadcastable+" == 1 ) OR " +
						" ( ( m."+table.peer_my_data.broadcastable+" ISNULL ) AND ( p."+table.peer.broadcastable+" == 1 ) ) ) " +
			" AND ( p."+table.peer.arrival_date+" > ? ) GROUP BY mindate ORDER BY mindate ASC LIMIT "+(limitPeersLow+1)+";";
			p_data = Application.db.select(queryPeers, new String[]{last_sync_date}, DEBUG);
		}else {
			queryPeers = "SELECT p."+table.peer.arrival_date+" AS mindate, COUNT(*) AS c FROM "+table.peer.TNAME+" AS p " +
				" LEFT JOIN "+table.peer_my_data.TNAME+" AS m ON (p."+table.peer.peer_ID+"==m."+table.peer_my_data.peer_ID+") " +
					" WHERE ( ( m."+table.peer_my_data.broadcastable+" == 1 ) OR " +
						" ( ( m."+table.peer_my_data.broadcastable+" ISNULL ) AND ( p."+table.peer.broadcastable+" == 1 ) ) ) "+
					" AND ( ( p."+table.peer.arrival_date+" > ? )  AND ( p."+table.peer.arrival_date+" <= ? ) ) " +
					" GROUP BY mindate ORDER BY mindate ASC LIMIT "+(limitPeersLow+1)+";";
			p_data = Application.db.select(queryPeers, new String[]{last_sync_date, _maxDate}, DEBUG);
		}
		int valid=0, total=0;
		if(p_data.size() > 0){
			for(int k=0; k<p_data.size(); k++) {
				total += Integer.parseInt(p_data.get(k).get(1).toString());
				if(total>limitPeersLow) break;
				valid = k;
			}
			if(DEBUG) out.println("NextDate after "+last_sync_date+" = "+p_data.get(0).get(0)+" ("+p_data.get(0).get(1)+")");
			if(total>limitPeersLow){
				_maxDate = Util.getString(p_data.get(valid).get(0)); // setting the limit
				if(DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("UpdatePeersTable: getNextDate: limits reached: "+limitPeersLow+" date="+_maxDate);
			}
			orgs2.add(OrgHandling.ORG_PEERS);
		}
		
		// check for the same number of maximum peer_address entries, was setting max_date
		if(_maxDate == null) {
			queryPeers = "SELECT min(a."+table.peer_address.arrival_date+") AS mindate, COUNT(*) AS c,  max(a."+table.peer_address.arrival_date+") AS maxdate FROM "+table.peer.TNAME+" AS p " +
			" LEFT JOIN "+table.peer_my_data.TNAME+" AS m ON (p."+table.peer.peer_ID+"==m."+table.peer_my_data.peer_ID+") " +
			" LEFT JOIN "+table.peer_address.TNAME+" AS a ON (p."+table.peer.peer_ID+"==a."+table.peer_address.peer_ID+") " +
			" WHERE " +
			"( ( m."+table.peer_my_data.broadcastable+" == 1 ) OR " +
			" ( ( m."+table.peer_my_data.broadcastable+" ISNULL ) AND ( p."+table.peer.broadcastable+" == 1 ) ) )" +
			" AND ( a."+table.peer_address.arrival_date+" > ? ) " +
					" GROUP BY a."+table.peer_address.peer_ID+" ORDER BY mindate ASC LIMIT "+(limitPeersLow+1)+";";
			p_addr = Application.db.select(queryPeers, new String[]{last_sync_date}, DEBUG);
		}else {
			queryPeers = "SELECT min(a."+table.peer_address.arrival_date+") AS mindate, COUNT(*) AS c,  max(a."+table.peer_address.arrival_date+") AS maxdate FROM "+table.peer.TNAME+" AS p " +
					" LEFT JOIN "+table.peer_my_data.TNAME+" AS m ON (p."+table.peer.peer_ID+"==m."+table.peer_my_data.peer_ID+") " +
					" LEFT JOIN "+table.peer_address.TNAME+" AS a ON (p."+table.peer.peer_ID+"==a."+table.peer_address.peer_ID+") " +
					" WHERE " +
					" ( ( m."+table.peer_my_data.broadcastable+" == 1 ) OR " +
						" ( ( m."+table.peer_my_data.broadcastable+" ISNULL ) AND ( p."+table.peer.broadcastable+" == 1 ) ) ) " +
					" AND ( ( a."+table.peer_address.arrival_date+" > ? ) AND (a."+table.peer_address.arrival_date+" <= ?) ) " +
					" GROUP BY a."+table.peer_address.peer_ID+" ORDER BY mindate ASC LIMIT "+(limitPeersLow+1)+";";		
			p_addr = Application.db.select(queryPeers, new String[]{last_sync_date, _maxDate}, DEBUG);
		}
		// If less than max rows, was setting the date as the last one
		if((p_addr.size() <= limitPeersLow)) {
			if (((p_addr.size() > 0) || (total > 0)) && (_maxDate == null)) {
				if(LIMITING_DATE_ALWAYS) {
					if(total>0) _maxDate = Util.getString(p_data.get(p_data.size()-1).get(0));
					//.......//else _maxDate = Util.getString(p_addr.get(p_addr.size()-1).get(2));
					for(int k=p_addr.size()-1; k>=0; k--) {
						String candidate = Util.getString(p_addr.get(k).get(2));
						if(candidate == null) continue;
						if(_maxDate== null) _maxDate = candidate;
						else if(_maxDate.compareTo(candidate) < 0)
							_maxDate = Util.getString(candidate);
					}
				}
				//if(p_addr.size() > 0) _maxDate = Util.getString(p_addr.get(p_addr.size()-1).get(0));
				//else _maxDate = Util.getString(p_data.get(p_data.size()-1).get(0));
				//TODO: if both, take maximum (addr will anyhow be likely higher than peer p_addr)
				//_maxDate = Util.getGeneralizedTime();
			}
			if(DEBUG) System.out.println("UpdatePeersTable:getNextDate: result maxDate="+_maxDate);
			return _maxDate;
		}else{
			// get the last but one last date;
			orgs2.add(OrgHandling.ORG_PEERS);
			int valid_addr = Math.min(limitPeersLow-1, p_addr.size()-1);
			_maxDate = Util.getString(p_addr.get(valid_addr).get(0));
			if(DD.WARN_BROADCAST_LIMITS_REACHED || DEBUG) System.out.println("UpdatePeersTable: getNextDate: limits reached: "+limitPeersLow+" date="+_maxDate);
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
			String addr = Util.getString(a.get(Q_TYPE)) + Address.ADDR_TYPE_SEP + Util.getString(a.get(Q_ADDR));
			if(Util.stringInt2bool(a.get(Q_CERT), false))
				addr += TypedAddress.PRI_SEP + Util.getString(a.get(Q_PRI));
			if("".equals(result)) result = addr;
			else result = Address.joinAddresses(result, addr);
		}
		return result;
	}
	
	public static String getPeerAddresses(String peer_ID) throws P2PDDSQLException {
		String result = "";
		String queryAddresses = "SELECT a."+table.peer_address.address+",a."+table.peer_address.type+
				",a."+table.peer_address.arrival_date+",a."+table.peer_address.certified+",a."+table.peer_address.priority+
			" FROM "+table.peer_address.TNAME+" AS a " +
			" WHERE (a."+table.peer_address.peer_ID+" == ?) " +
			" ORDER BY a."+table.peer_address.arrival_date+" LIMIT 1000;";
		ArrayList<ArrayList<Object>>p_data = Application.db.select(queryAddresses, new String[]{peer_ID});
		for(ArrayList<Object> a: p_data) {
			String addr = Util.getString(a.get(1)) + Address.ADDR_TYPE_SEP + Util.getString(a.get(0));
			if(Util.stringInt2bool(a.get(3), false))
				addr += Address.ADDR_TYPE_SEP + Util.getString(a.get(4));
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
	static Table buildPeersTable2(String last_sync_date, String[] _maxDate, boolean justDate, HashSet<String> orgs2, int limitPeersLow, int limitPeersMax) throws P2PDDSQLException{
		if(justDate){
			_maxDate[0] = getNextDate(last_sync_date, _maxDate[0], orgs2, limitPeersLow);
			if(DEBUG) System.out.println("UpdatePeersTable:buildPeersTable2: result justDate maxDate="+_maxDate[0]);
			return null;
		}
		//boolean DEBUG = true;
		String maxDate = _maxDate[0];
		Table recentPeers=new Table();
		recentPeers.name = table.peer.G_TNAME;
		recentPeers.fields = peersFields2;
		recentPeers.fieldTypes = peersFieldsTypes2;
		if(DEBUG) out.print("buildPeersTable2:v");
		//if(maxDate == null) maxDate = last_sync_date;
		//_maxDate[0] = maxDate;
		if(DEBUG) out.println("buildPeersTable2:maxDate after "+last_sync_date+" =: "+maxDate);
		String queryPeers =
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
		ArrayList<ArrayList<Object>>p_data =
			Application.db.select(queryPeers,
					((maxDate!=null)?(new String[]{last_sync_date, maxDate, last_sync_date, maxDate}):new String[]{last_sync_date,last_sync_date}),
					DEBUG);
		recentPeers.rows = new byte[p_data.size()][][];
		if(DEBUG) out.println("buildPeersTable2:sent peer rows=: "+p_data.size());
		int i = 0;
		for(ArrayList<Object> p: p_data) {
			// for(int j=0; j<row.length; j++) {row[j] = (String) p_data.get(i).get(j);}
			// Prepare data
			String peer_ID = Util.getString(p.get(0));
			String addresses = getPeerAddresses(peer_ID, last_sync_date, maxDate);
			String orgs = data.D_PeerAddress.getPeerOrgs(peer_ID, null);
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
				D_PeerAddress me = D_PeerAddress.get_myself(global_peer_ID);
				if(!me.verifySignature()){
					me.component_basic_data.creation_date = Util.CalendargetInstance();
					me.signMe();
					me.storeVerified();
				}
				
				peer_ID = me.peer_ID;
				name = me.component_basic_data.name;
				slogan = me.component_basic_data.slogan;
				date = Encoder.getGeneralizedTime(me.component_basic_data.creation_date);
				hash_alg = me.component_basic_data.hash_alg;
				signature = Util.stringSignatureFromByte(me.component_basic_data.signature);
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
	public static D_PeerAddress integratePeersTable2(ASNSyncPayload asa, D_PeerAddress peer, Table tab, String filteredID) throws P2PDDSQLException{
		//boolean DEBUG = true;
		if(DEBUG) out.println("UpdatePeersTable:integratePeersTable2: START");
		
		String sql = "SELECT count(*) FROM "+table.peer.TNAME;
		ArrayList<ArrayList<Object>> c = Application.db.select(sql, new String[]{}, DEBUG);
		if((c.size()>0) && (Util.lval(c.get(0).get(0), 0)>DD.ACCEPT_STREAMING_UPTO_MAX_PEERS)) {
			Application.warning(Util._("Upper limit for number of peers is reached:")+DD.ACCEPT_STREAMING_UPTO_MAX_PEERS+
					"\n "+("You can delete some peers, or raise the limit in Control Panel."),
					Util._("Upper Limit For Peers Reached"));
			return null;
		}
		
		Calendar adding__date = Util.CalendargetInstance();
		String adding_date = Encoder.getGeneralizedTime(adding__date);
		for(int i=0; i<tab.rows.length; i++) {
			if(DEBUG) out.println("UpdatePeersTable:integratePeersTable2: handling row ["+i+"] ="+Table.printRow(tab.rows[i]));
			String global_peer_ID = Util.getBString(tab.rows[i][0]);
			if((filteredID!=null)&&(!filteredID.equals(global_peer_ID))) continue;
			if((global_peer_ID==null) || (global_peer_ID.equals(DD.getMyPeerGIDFromIdentity()))){
				if(DEBUG) out.println("UpdatePeersTable:integratePeersTable2: handling myself, skip ");
				continue;
			}
			if(tab.rows[i].length<=9){
				if(DEBUG||DD.DEBUG_TODO) Application.warning(Util._("Peer has wrong message format"), Util._("Old version peer"));
				if(DEBUG||DD.DEBUG_TODO) System.out.println("UpdatePeersTable: integratePeersTable2: old version of peers, too few columns");
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
			if(tab.rows[i].length>10) emails = Util.getBString(tab.rows[i][10]);
			if(tab.rows[i].length>11) phones = Util.getBString(tab.rows[i][11]);
			boolean _broadcastable = Util.stringInt2bool(broadcastable, false);
			boolean _used = DD.DEFAULT_RECEIVED_PEERS_ARE_USED;
			
			byte[] peer_signature = null;
			try{
				if(peer_sign!=null) peer_signature = Util.byteSignatureFromString(peer_sign);
			}catch (Exception e){e.printStackTrace();}
			// Verify signature of peer
			D_PeerAddress peer_data = new D_PeerAddress(global_peer_ID, 0, false);
			if(peer_data._peer_ID<=0){
				peer_data.component_preferences.used = _used;
				peer_data.component_local_agent_state.first_provider_peer = peer.peer_ID;
				if(filteredID != null)
					peer_data.component_preferences.blocked = DD.BLOCK_NEW_ARRIVING_PEERS_ANSWERING_ME;
				else
					peer_data.component_preferences.blocked = DD.BLOCK_NEW_ARRIVING_PEERS_FORWARDED_TO_ME;
			}
			/**
			 * Fill the peer_data with the received one
			 */
			
			peer_data.served_orgs = table.peer_org.peerOrgsFromString(global_organizationIDs);
			if(peer_signature == null) {
				if(DEBUG||DD.DEBUG_TODO) out.println("UpdatePeersTable:integratePeersTable2: invalid empty signature");
				if(!DD.ACCEPT_UNSIGNED_PEERS_FROM_TABLES) continue;				
			}else{
				peer_data.component_basic_data.signature = peer_signature;
			}
			
			// version, globalID, name, slogan, creation_date, address*, broadcastable, signature_alg, served_orgs, signature*
			peer_data.component_basic_data.version = version;
			peer_data.component_basic_data.emails = emails;
			peer_data.component_basic_data.phones = phones;
			peer_data.component_basic_data.globalID = global_peer_ID;
			peer_data.component_basic_data.name = name;
			peer_data.component_basic_data.slogan = slogan;
			peer_data.component_basic_data.creation_date = Util.getCalendar(creation_date);
			peer_data.component_basic_data.broadcastable = _broadcastable;
			peer_data.signature_alg = D_PeerAddress.getHashAlgFromString(peer_hash_alg);
			if(DEBUG)out.println("UpdatePeersTable:integratePeersTable2: got addresses: "+addresses);
			hds.TypedAddress[] typed_address = hds.TypedAddress.getAddress(addresses);
			peer_data.address = typed_address;
			
			if(peer_data.component_basic_data.signature != null) {
				//peer_data.signature = new byte[0];
				if(DEBUG) out.println("UpdatePeersTable:integratePeersTable2: handling row ["+i+"] "+peer_data);
				ciphersuits.PK pk = ciphersuits.Cipher.getPK(global_peer_ID);
				if(pk==null) System.err.println("UpdatePeersTable:integratePeersTable2: pk fail: \n"+peer_data);
				if(DEBUG) out.println("UpdatePeersTable:integratePeersTable2: pk= "+pk+" sign=["+peer_signature.length+"]"+Util.byteToHexDump(peer_signature)+" text="+peer_sign);
				//if(!Util.verifySign(peer_data, pk, peer_signature)) {
				if(!peer_data.verifySignature()) { //  Util.verifySign(peer_data, pk, peer_signature)) {
					if(DEBUG||DD.DEBUG_TODO) System.err.println("UpdatePeersTable:integratePeersTable2: sign verification failed for "+peer_data.component_basic_data.name);
					if(DEBUG) System.err.println("UpdatePeersTable:integratePeersTable2: sign verification failed for "+peer_data);
					if(!DD.ACCEPT_DATA_FROM_UNSIGNED_PEERS) continue;
					peer_data.component_basic_data.signature = null;
				}else{
					if(DEBUG) out.println("UpdatePeersTable:integratePeersTable2: sign verification passed for "+peer_data);					
				}
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
			long peer_ID = peer_data._storeVerified(adding_date);
			if(DEBUG)out.println("UpdatePeersTable:integratePeersTable2: got local peer_ID: "+peer_ID);
			//integratePeerOrgs(peer_data.served_orgs, peer_ID, adding_date);

			
			if(DEBUG)out.println("UpdatePeersTable:integratePeersTable2: got addresses: "+addresses);
			//integratePeerAddresses(peer_ID, typed_address, adding__date);
			if(filteredID!=null) return peer_data;
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
			long address_ID = D_PeerAddress.get_peer_addresses_ID(address, type, peer_ID, adding_date,
					addresses_l[j].certified, addresses_l[j].priority);
			if(DEBUG)out.println("UpdatePeersTable:integratePeersTable2: got local address_ID: "+address_ID);
		}
	}
	public static void integratePeerAddresses(long peer_ID, String[] addresses_l, Calendar adding__date ) throws P2PDDSQLException{
		if(DEBUG)out.println("UpdatePeersTable:integratePeersTable2: got addresses #: "+addresses_l.length);
		for(int j=0; j<addresses_l.length; j++) {
			String adding_date = Encoder.getGeneralizedTime(Util.incCalendar(adding__date, 1));
			if(DEBUG)out.println("got addr: "+addresses_l[j]);
			String[] t_address = addresses_l[j].split(Pattern.quote(Address.ADDR_TYPE_SEP));
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
			long address_ID = D_PeerAddress.get_peer_addresses_ID(address, type, peer_ID, adding_date,
					certificate, priority);
			if(DEBUG)out.println("UpdatePeersTable:integratePeersTable2: got local address_ID: "+address_ID);
		}
	}
	
	public static void main(String args[]){
		try {
			Application.db = new DBInterface(Application.DEFAULT_DELIBERATION_FILE);
		} catch (P2PDDSQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}
		String last_sync_date = Encoder.getGeneralizedTime(Util.getCalendar("00000000000000.000Z"));
		String[] _maxDate  = new String[]{Util.getGeneralizedTime()};
		boolean justDate=false;
		HashSet<String> orgs = new HashSet<String>();
		int limitPeersLow = 100;
		int limitPeersMax = 1000;
		Table a=null;
		try {
			a = UpdatePeersTable.buildPeersTable(last_sync_date, _maxDate, justDate, orgs, limitPeersLow, limitPeersMax);
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Got="+a);
	}
}
