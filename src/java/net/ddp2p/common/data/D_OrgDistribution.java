/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2013 Marius C. Silaghi
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

/**
 * Registered a listener on peer and peer_my_data,
 * to update the names in hashtables.
 */

package net.ddp2p.common.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
class OrgDistributionListener implements DBListener{

	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		D_OrgDistribution.initStatics();
	}
	
}
public class D_OrgDistribution{
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	// left to be initialized after Application.db (on first query)
	private static OrgDistributionListener listener = null;
	public long od_ID;
	public long organization_ID;
	public long peer_ID;
	public String peer_name;
	public String emails;
	public String reset_date;
	public String organization_GIDhash;
	
	static Hashtable<String,ArrayList<D_OrgDistribution>> od_by_peerID;
	static Hashtable<String,ArrayList<D_OrgDistribution>> od_by_orgID;
	
	static boolean initStatics(){
		od_by_peerID = new Hashtable<String,ArrayList<D_OrgDistribution>>();
		od_by_orgID = new Hashtable<String,ArrayList<D_OrgDistribution>>();
		return true;
	}
	static boolean inited = initStatics();
	
	private D_OrgDistribution(){
		
	}
	/*
	private static String _sql_peerID = 
			"SELECT "+util.Util.setDatabaseAlias(table.org_distribution.fields,"o")+
				",p."+table.peer.name+
				",m."+table.peer_my_data.name+
				",p."+table.peer.emails+
				",g."+table.organization.global_organization_ID_hash+
			" FROM "+table.peer.TNAME+" AS p "+
			" JOIN "+table.org_distribution.TNAME+" AS o ON (o."+table.org_distribution.peer_ID+"=p."+table.peer.peer_ID+") "+
			" LEFT JOIN "+table.peer_my_data.TNAME+" AS m ON (o."+table.org_distribution.peer_ID+"=m."+table.peer_my_data.peer_ID+") "+
			" LEFT JOIN "+table.organization.TNAME+" AS g ON (o."+table.org_distribution.organization_ID+"=g."+table.organization.organization_ID+") "+
			" WHERE p."+table.peer.peer_ID+"=? ;";
*/
	private static String sql_peerID = 
			"SELECT "+net.ddp2p.common.util.Util.setDatabaseAlias(net.ddp2p.common.table.org_distribution.fields,"o")+
			" FROM "+net.ddp2p.common.table.org_distribution.TNAME+" AS o "+
			" WHERE o."+net.ddp2p.common.table.org_distribution.peer_ID+" = ? ;";
	
	private static ArrayList<D_OrgDistribution> load_Org_Distribution(String peer_ID) throws P2PDDSQLException{
		D_Peer peer = D_Peer.getPeerByLID_NoKeep(peer_ID, true);
		ArrayList<D_OrgDistribution> res = new ArrayList<D_OrgDistribution>();
		if (peer == null) return res;

		ArrayList<ArrayList<Object>> l = Application.getDB().select(sql_peerID, new String[]{peer_ID}, DEBUG);
		if ((l!=null)&&(l.size()!=0)){
			for(int i=0; i<l.size(); i++){
				ArrayList<Object> o = l.get(i);
				D_OrgDistribution r = new D_OrgDistribution();
				r.od_ID = net.ddp2p.common.util.Util.lval(o.get(net.ddp2p.common.table.org_distribution.OD_ID), -1);
				r.peer_ID = net.ddp2p.common.util.Util.lval(o.get(net.ddp2p.common.table.org_distribution.OD_PEER_ID), -1);
				r.reset_date = net.ddp2p.common.util.Util.getString(o.get(net.ddp2p.common.table.org_distribution.OD_RESET_DATE));
				r.organization_ID = net.ddp2p.common.util.Util.lval(o.get(net.ddp2p.common.table.org_distribution.OD_ORG_ID), -1);
				r.peer_name = peer.getName_MyOrDefault(); //util.Util.getString(o.get(table.org_distribution.OD_FIELDS+1));
				r.emails = peer.getEmail(); //util.Util.getString(o.get(table.org_distribution.OD_FIELDS+2));
				r.organization_GIDhash = D_Organization.getGIDHbyLID(r.organization_ID);//util.Util.getString(o.get(table.org_distribution.OD_FIELDS+3));
//				if((r.peer_name == null)||(r.peer_name.trim().length()==0)) { // use original
//					r.peer_name = util.Util.getString(o.get(table.org_distribution.OD_FIELDS));
//				}
				res.add(r);
			}
		}
		return res;
	}
/*
	private static String _sql_orgID = 
			"SELECT "+util.Util.setDatabaseAlias(table.org_distribution.fields,"o")+
				",p."+table.peer.name+
				",m."+table.peer_my_data.name+
				",p."+table.peer.emails+
			" FROM "+table.org_distribution.TNAME+" AS o "+
			" LEFT JOIN "+table.peer.TNAME+" AS p ON (o."+table.org_distribution.peer_ID+"=p."+table.peer.peer_ID+") "+
			" LEFT JOIN "+table.peer_my_data.TNAME+" AS m ON (o."+table.org_distribution.peer_ID+"=m."+table.peer_my_data.peer_ID+") "+
			" WHERE "+table.org_distribution.organization_ID+"=? ;";
*/
	private static String sql_orgID = 
			"SELECT "+net.ddp2p.common.util.Util.setDatabaseAlias(net.ddp2p.common.table.org_distribution.fields,"o")+
			" FROM "+net.ddp2p.common.table.org_distribution.TNAME+" AS o "+
			" WHERE "+net.ddp2p.common.table.org_distribution.organization_ID+"=? ;";
	
	private static ArrayList<D_OrgDistribution> load_Org_Distribution_byOrg(String org_ID) throws P2PDDSQLException{
		if (listener == null) {
			listener = new OrgDistributionListener();
			Application.getDB().addListener(listener, new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.peer.TNAME,net.ddp2p.common.table.peer_my_data.TNAME)), null);
		}
		ArrayList<ArrayList<Object>> l =
				Application.getDB().select(sql_orgID, new String[]{org_ID}, DEBUG);
		ArrayList<D_OrgDistribution> res = new ArrayList<D_OrgDistribution>();
		if ((l != null) && (l.size() != 0)) {
			for (int i = 0; i < l.size(); i ++) {
				ArrayList<Object> o = l.get(i);
				D_OrgDistribution r = new D_OrgDistribution();
				r.od_ID = net.ddp2p.common.util.Util.lval(o.get(net.ddp2p.common.table.org_distribution.OD_ID), -1);
				r.peer_ID = net.ddp2p.common.util.Util.lval(o.get(net.ddp2p.common.table.org_distribution.OD_PEER_ID), -1);
				r.reset_date = net.ddp2p.common.util.Util.getString(o.get(net.ddp2p.common.table.org_distribution.OD_RESET_DATE));
				r.organization_ID = net.ddp2p.common.util.Util.lval(o.get(net.ddp2p.common.table.org_distribution.OD_ORG_ID), -1);
				D_Peer peer = D_Peer.getPeerByLID_NoKeep(r.peer_ID, true);
				if (peer != null) {
					r.peer_name = peer.getName_MyOrDefault(); //util.Util.getString(o.get(table.org_distribution.OD_FIELDS+1));
					r.emails = peer.getEmail(); //util.Util.getString(o.get(table.org_distribution.OD_FIELDS+2));
	//				if((r.peer_name == null)||(r.peer_name.trim().length()==0)) { // use original
	//					r.peer_name = util.Util.getString(o.get(table.org_distribution.OD_FIELDS));
	//				}
				}
				res.add(r);
			}
		}
		return res;
	}
	synchronized public static ArrayList<D_OrgDistribution> get_Org_Distribution_byPeerID(String _peer_ID){
		try{
			ArrayList<D_OrgDistribution> res = od_by_peerID.get(_peer_ID);
			if (res != null) {
				if(DEBUG||DD.DEBUG_CHANGED_ORGS) System.out.println("O_OrgDistr: get_Org_Distribution_byPeerID: existing");
				return res;
			}
			res = load_Org_Distribution(_peer_ID);
			od_by_peerID.put(_peer_ID, res);
			if(DEBUG||DD.DEBUG_CHANGED_ORGS) System.out.println("O_OrgDistr: get_Org_Distribution_byPeerID: loaded");
			return res;
		}catch(Exception e){
			return null;
		}
	}
	synchronized public static ArrayList<D_OrgDistribution> get_Org_Distribution_byOrgID(String org_ID){
		try{
			ArrayList<D_OrgDistribution> res = od_by_orgID.get(org_ID);
			if (res!=null) return res;
			res = load_Org_Distribution_byOrg(org_ID);
			od_by_orgID.put(org_ID, res);
			return res;
		}catch(Exception e){
			return null;
		}
	}
	synchronized public static void add(String org_ID, String peer_ID) throws P2PDDSQLException{
		long _org_ID = net.ddp2p.common.util.Util.lval(org_ID, -1);
		long _peer_ID = net.ddp2p.common.util.Util.lval(peer_ID, -1);
		ArrayList<D_OrgDistribution> op = od_by_peerID.get(peer_ID);
		if(contains_org(op, _org_ID)){
			if(DEBUG||DD.DEBUG_CHANGED_ORGS) System.out.println("D_OrgDistrib:add: contained in org : p="+peer_ID+" o="+org_ID);
			return;
		}
		ArrayList<D_OrgDistribution> oo = od_by_orgID.get(org_ID);
		if(contains_peer(oo, _peer_ID)){
			if(DEBUG||DD.DEBUG_CHANGED_ORGS) System.out.println("D_OrgDistrib:add: contained in peer: p="+peer_ID+" o="+org_ID);
			return;
		}
		
		String sql =
				"SELECT "+net.ddp2p.common.table.org_distribution.peer_distribution_ID+
				" FROM "+net.ddp2p.common.table.org_distribution.TNAME+
				" WHERE "+net.ddp2p.common.table.org_distribution.peer_ID+"=? "+
				" AND "+net.ddp2p.common.table.org_distribution.organization_ID+"=?; ";
		ArrayList<ArrayList<Object>> od = Application.getDB().select(sql, new String[]{peer_ID, org_ID}, DEBUG);
		long new_ID = -1;
		String now = net.ddp2p.common.util.Util.getGeneralizedTime();
		if(od.size()<=0){
			if(DEBUG||DD.DEBUG_CHANGED_ORGS) System.out.println("D_OrgDistrib:add: insert org_distr: p="+peer_ID+" o="+org_ID);
//			new_ID = Application.db.insertNoSync(
			new_ID = Application.getDB().insert(
					net.ddp2p.common.table.org_distribution.TNAME,
				new String[]{net.ddp2p.common.table.org_distribution.peer_ID,
							net.ddp2p.common.table.org_distribution.organization_ID,
							net.ddp2p.common.table.org_distribution.reset_date},
				new String[]{peer_ID, org_ID, now}, DEBUG||DD.DEBUG_CHANGED_ORGS);
			
		}else{
			if(DEBUG||DD.DEBUG_CHANGED_ORGS) System.out.println("D_OrgDistrib:add:  preexisted: p="+peer_ID+" o="+org_ID);
			return;
			// since it existed, it must already be in Hashtable
			// new_ID = util.Util.lval(od.get(0).get(0),-1);
		}
		
		if((op==null)&&(oo==null)) return;
		D_OrgDistribution n = new D_OrgDistribution();
		n.peer_ID = net.ddp2p.common.util.Util.lval(peer_ID, -1);
		n.organization_ID = net.ddp2p.common.util.Util.lval(org_ID, -1);
		n.od_ID = new_ID;
		n.reset_date = now;
		n.peer_name = D_Peer.getDisplayName(n.peer_ID);
		if(op!=null) op.add(n);
		if(oo!=null) oo.add(n);
	}
	public static String getResetDate(String org_ID, String peer_ID2){
		D_OrgDistribution v = D_OrgDistribution.get_peer(od_by_orgID.get(org_ID), net.ddp2p.common.util.Util.lval(peer_ID2,-1));
		return v.reset_date;
	}
	public static void setResetDate(String org_ID, String peer_ID2) {
		if(DEBUG) System.out.println("Orgs:setResetDate: for orgID="+org_ID);
		try {
			Application.getDB().update(net.ddp2p.common.table.org_distribution.TNAME,
					new String[]{net.ddp2p.common.table.org_distribution.reset_date},
					new String[]{net.ddp2p.common.table.org_distribution.organization_ID,
					net.ddp2p.common.table.org_distribution.peer_ID},
					new String[]{net.ddp2p.common.util.Util.getGeneralizedTime(),
					org_ID, peer_ID2}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if(DEBUG) System.out.println("Orgs:setBroadcasting: Done");
	}
	synchronized public static void del(String org_ID, String peer_ID) throws P2PDDSQLException{
		long _org_ID = net.ddp2p.common.util.Util.lval(org_ID, -1);
		long _peer_ID = net.ddp2p.common.util.Util.lval(peer_ID, -1);
		ArrayList<D_OrgDistribution> op = od_by_peerID.get(peer_ID);
		if(delete_org(op, _org_ID));
		ArrayList<D_OrgDistribution> oo = od_by_orgID.get(org_ID);
		if(delete_peer(oo, _peer_ID));
		//Application.db.deleteNoSync(
		Application.getDB().delete(
				net.ddp2p.common.table.org_distribution.TNAME,
				new String[]{net.ddp2p.common.table.org_distribution.peer_ID, net.ddp2p.common.table.org_distribution.organization_ID},
				new String[]{peer_ID, org_ID}, DEBUG);

	}

	public static boolean contains_org(ArrayList<D_OrgDistribution> op,
			long _org_ID) {
		if(op==null) return false;
		for(int k=0; k<op.size(); k++){
			if(op.get(k).organization_ID == _org_ID) return true;
		}
		return false;
	}

	public static D_OrgDistribution get_org(ArrayList<D_OrgDistribution> op,
			long _org_ID) {
		if(op==null) return null;
		for(int k=0; k<op.size(); k++){
			if(op.get(k).organization_ID == _org_ID) return op.get(k);
		}
		return null;
	}

	private static boolean contains_peer(ArrayList<D_OrgDistribution> op,
			long _peer_ID) {
		if(op==null) return false;
		for(int k=0; k<op.size(); k++){
			if(op.get(k).peer_ID == _peer_ID) return true;
		}
		return false;
	}
	private static D_OrgDistribution get_peer(ArrayList<D_OrgDistribution> op,
			long _peer_ID) {
		if(op==null) return null;
		for(int k=0; k<op.size(); k++){
			if(op.get(k).peer_ID == _peer_ID) return op.get(k);
		}
		return null;
	}
	private static boolean delete_org(ArrayList<D_OrgDistribution> op,
			long _org_ID) {
		if(op==null) return false;
		for(int k=0; k<op.size(); k++){
			if(op.get(k).organization_ID == _org_ID){
				op.remove(k);
				return true;
			}
		}
		return false;
	}

	private static boolean delete_peer(ArrayList<D_OrgDistribution> op,
			long _peer_ID) {
		if(op==null) return false;
		for(int k=0; k<op.size(); k++){
			if(op.get(k).peer_ID == _peer_ID){
				op.remove(k);
				return true;
			}
		}
		return false;
	}
}
