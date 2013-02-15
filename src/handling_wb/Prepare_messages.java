/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Osamah Dhannoon
 		Author: Osamah Dhannoon: odhannoon2011@fit.edu
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

package handling_wb;


import hds.TypedAddress;
import java.util.ArrayList;
import ASN1.ASN1DecoderFail;
import ASN1.Encoder;
import ciphersuits.Cipher;

import com.almworks.sqlite4java.SQLiteException;
import config.Application;
import config.DD;
import simulator.WirelessLog;
import data.D_Neighborhood;
import data.D_OrgConcepts;
import data.D_Organization;
import data.D_OrgParams;
import data.D_PeerAddress;
import data.D_Message;
import data.D_Vote;
import data.D_Constituent;
import data.D_Witness;

import streaming.UpdatePeersTable;
import util.Util;

public class Prepare_messages {

	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static int PEERS_SIZE = 10;//20;
	public static int ORGS_SIZE = 10;//20;
	public static int VOTES_SIZE = 10;//1000;
	public static int CONSTITUENTS_SIZE = 10;//100;
	public static int WITNESSES_SIZE = 10;
	public static int NEIGHS_SIZE = 6;
	public String m_sG_org_id=null;




	public Prepare_messages(){
		//Application.db=new DBInterface("deliberation-app.db");
	}

	/**
	 * load the orgs to the ArrayList<byte[]>
	 * @param org_msgs
	 * @param rowID
	 * @return
	 * @throws SQLiteException
	 */
	public static long loadOrgs(ArrayList<PreparedMessage> org_msgs, long rowID) {
		if(DEBUG)System.out.println("Prepare_messages : loadOrgs() : START");
		try { 
			return _loadOrgs(org_msgs, rowID);
		} catch (SQLiteException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public static long _loadOrgs(ArrayList<PreparedMessage> org_msgs, long rowID) throws SQLiteException {
		long start_row = rowID;
		boolean wrapped = false;
		long last_row = -1;
		ArrayList<ArrayList<Object>> orgs;
		int retrieved = 0;
		org_msgs.clear();
		for(;;){
			String sql = "SELECT "+table.organization.org_list+
					" FROM "+table.organization.TNAME
					+" WHERE ROWID > "+rowID+
					(wrapped?(" AND ROWID <= "+start_row):"")+
					" AND " +table.organization.signature+ " IS NOT NULL "+
					" AND "+table.organization.broadcasted+" <> '0' " +
					" ORDER BY ROWID"
					+" LIMIT "+(ORGS_SIZE - retrieved);

			if(DEBUG)System.out.println("Prepared : messages : "+sql);
			if(rowID==-1) rowID=1;
			if(DEBUG)System.out.println("ROWID: "+rowID);
			try{
				orgs = Application.db.select(sql, new String[]{});
				for(ArrayList<Object> org : orgs){
					last_row = Integer.parseInt(org.get(table.organization.ORG_COL_ID).toString());
					byte[]msg = buildOrganizationMessage(org, ""+rowID);
					if(DEBUG)System.out.println("MSG : "+msg);
					
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					pm.org_ID_hash = Util.getString(org.get(table.organization.ORG_COL_GID_HASH));
					// pm.motion_ID = Util.getString(org.get(table.motion.M_MOTION_GID));
					// pm.constituent_ID_hash = Util.getString(org.get(table.constituent.CONST_COL_GID_HASH));
					// pm.neighborhood_ID = Util.getString(org.get(table.neighborhood.IDX_GID));

					org_msgs.add(pm);
					
					
					if(org_msgs.size()>=0)
						WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Circular_queue,
								""+(org_msgs.size()-1),WirelessLog.org_type,msg);
				}
			}catch(Exception e){
				if(_DEBUG)System.out.println("Prepared_messages : loadOrgs: EXCEPTION");
				return -1;

			}
			retrieved = org_msgs.size();
			if(wrapped) {
				if(DEBUG)System.out.println("BroadcatQueueMyData() : loadOrgs() : "+org_msgs.size()+" loaded");
				break;
			}
			if((orgs.size() < ORGS_SIZE)&&(rowID != -1)) {
				rowID = -1;
				wrapped = true;
				continue;
			}
			if(DEBUG)System.out.println("Prepare_messages : loadOrgs() : "+org_msgs.size()+" loaded");
			break;
		}
		return last_row;
	}

	/**
	 * load the Witnesses to the ArrayList<byte[]>
	 * @param Witnesses_msgs
	 * @param rowID
	 * @return
	 */
	public static long loadWitnesses(ArrayList<PreparedMessage> Witnesses_msgs,long rowID) {
		if(DEBUG)System.out.println("Prepare_messages : loadWitnesses() : START");
		return _loadWitnesses(Witnesses_msgs, rowID);
	}
	static String sql_witnesses = 
			"SELECT "
					+ Util.setDatabaseAlias(table.witness.witness_fields, "w")+" "+
					", n."+table.neighborhood.global_neighborhood_ID+
					", t."+table.constituent.global_constituent_ID+
					", s."+table.constituent.global_constituent_ID+
					", o."+table.organization.global_organization_ID+
					" FROM "+table.witness.TNAME+" AS w "+
					" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(n."+table.neighborhood.neighborhood_ID+"=w."+table.witness.neighborhood_ID+") "+
					" LEFT JOIN "+table.constituent.TNAME+" AS s ON(s."+table.constituent.constituent_ID+"=w."+table.witness.source_ID+") "+
					" LEFT JOIN "+table.constituent.TNAME+" AS t ON(t."+table.constituent.constituent_ID+"=w."+table.witness.target_ID+") "+
					" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=s."+table.constituent.organization_ID+") ";

	public static long _loadWitnesses(ArrayList<PreparedMessage> Witnesses_msgs,long rowID) {
		long start_row = rowID;
		boolean wrapped = false;
		long last_row = -1;
		ArrayList<ArrayList<Object>> witnesses;
		int retrieved = 0;
		Witnesses_msgs.clear();
		for(;;){
			String sql = 
					sql_witnesses
					+" WHERE w.ROWID > "+rowID+
					(wrapped?(" AND w.ROWID <= "+start_row):"")+
					" AND w."+table.witness.signature +" IS NOT NULL "+
					" AND o."+table.organization.broadcasted+" <> '0' " +
					" AND s."+table.constituent.broadcasted+" <> '0' " +
					" ORDER BY w.ROWID "
					+" LIMIT "+(WITNESSES_SIZE - retrieved);

			D_Witness wbw = new D_Witness();
			try{
				witnesses = Application.db.select(sql, new String[]{});
				for(ArrayList<Object> witness : witnesses){
					D_Message asn1=new D_Message();
					asn1.sender = new D_PeerAddress(); 
					asn1.sender.globalID = DD.getMyPeerGIDFromIdentity();
					last_row = Integer.parseInt(witness.get(table.witness.WIT_COL_ID).toString());
					try{
						wbw.init_all(witness);
					}catch(Exception e){e.printStackTrace();}
					asn1.witness = wbw;
					if(DEBUG)System.out.println("Wintnessed_constituent INFO : "+asn1.witness.witnessed);
					if(DEBUG)System.out.println("Wintnessing_constituent INFO : "+asn1.witness.witnessing);
					if(DEBUG)System.out.println("Witness_ID : "+asn1.witness.witnessID);
					String sql1 = " SELECT "+table.constituent.organization_ID+
							" FROM "+table.constituent.TNAME+" WHERE "+
							table.constituent.constituent_ID+"=?;";
					ArrayList<ArrayList<Object>> org_id = Application.db.select(sql1, new String[]{""+wbw.witnessed_constituentID});
					String OrgID = Util.getString(org_id.get(0).get(0));	
					if(DEBUG)System.out.println("DATA : "+OrgID);
					ArrayList<Object> org_data = get_org_db_by_local(OrgID);
					asn1.organization = get_ASN_Organization_by_OrgID(org_data,OrgID);
					Encoder enc = asn1.getEncoder();	
					byte msg [] = enc.getBytes();
					if(DEBUG)System.out.println("MSG : "+msg);
					
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					if(asn1.organization!=null)pm.org_ID_hash = asn1.organization.global_organization_IDhash; 
					if(wbw.witnessing!=null){
							pm.constituent_ID_hash.add(wbw.witnessing.global_constituent_id_hash); 
							for(int i=0; i<wbw.witnessing.neighborhood.length; i++) {pm.neighborhood_ID.add(wbw.witnessing.neighborhood[i].neighborhoodID);}
					}
					if(wbw.witnessed!=null){
							pm.constituent_ID_hash.add(wbw.witnessed.global_constituent_id_hash); 
							for(int i=0; i<wbw.witnessed.neighborhood.length; i++) {pm.neighborhood_ID.add(wbw.witnessed.neighborhood[i].neighborhoodID);}
					}
					
					Witnesses_msgs.add(pm);					
					
					if(Witnesses_msgs.size()>=0)
						WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Circular_queue,
								""+(Witnesses_msgs.size()-1),WirelessLog.wit_type,msg);
				}
			}catch(Exception e){
				if(_DEBUG)System.out.println("Prepared_messages : loadWitness: EXCEPTION");
				return -1;
			}
			retrieved = Witnesses_msgs.size();
			if(wrapped) {
				if(DEBUG)System.out.println("Prepare_messages : loadWitnesses() : "+Witnesses_msgs.size()+" loaded");
				break;				
			}
			if((witnesses.size() < WITNESSES_SIZE)&&(rowID != -1)) {
				rowID = -1;
				wrapped = true;
				continue;
			}
			if(DEBUG)System.out.println("Prepare_messages : loadWitnesses() : "+Witnesses_msgs.size()+" loaded");
			break;
		}
		return last_row;
	}

	/**
	 * load the Constituents to the ArrayList<byte[]>
	 * @param constituent_msgs
	 * @param rowID
	 * @return
	 */
	public static long loadConstituents(ArrayList<PreparedMessage> constituent_msgs, long rowID) {
		if(DEBUG)System.out.println("Prepare_messages : loadConstituents() : START");
		return _loadConstituents(constituent_msgs,rowID);			
	}
	static String c_fields_constituents = Util.setDatabaseAlias(table.constituent.fields_constituents,"c");
	static String sql_cons =
			"SELECT "
					+c_fields_constituents
					+",n."+table.neighborhood.global_neighborhood_ID
					+" FROM "+table.constituent.TNAME+" as c " 
					+" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(c."+table.constituent.neighborhood_ID+" = n."+table.neighborhood.neighborhood_ID+") "
					+" LEFT JOIN "+table.organization.TNAME+" AS o ON(c."+table.constituent.organization_ID+" = o."+table.organization.organization_ID+") ";


	public static long _loadConstituents(ArrayList<PreparedMessage> constituent_msgs, long rowID) {
		long start_row = rowID;
		boolean wrapped = false;
		long last_row = -1;
		ArrayList<ArrayList<Object>> constituents;
		int retrieved = 0;
		constituent_msgs.clear();
		for(;;){	
			String sql_get_const =
					sql_cons+
					" WHERE c.ROWID > "+rowID+
					(wrapped?(" AND c.ROWID <= "+start_row):"")+
					" AND c."+table.constituent.sign+" IS NOT NULL"+
					" AND o."+table.organization.broadcasted+" <> '0' "+
					" AND c."+table.constituent.broadcasted+" <> '0' "+
					" ORDER BY c.ROWID "+
					" LIMIT "+(CONSTITUENTS_SIZE - retrieved);

			if(DEBUG)System.out.println(sql_get_const);
			try{
				constituents = Application.db.select(sql_get_const, new String[]{});
				for(ArrayList<Object> constituent : constituents){
					D_Constituent con = new D_Constituent();
					D_Message asn1=new D_Message();
					asn1.sender = new D_PeerAddress(); 
					asn1.sender.globalID = DD.getMyPeerGIDFromIdentity();
					last_row = Integer.parseInt(constituent.get(table.constituent.CONST_COL_ID).toString());
					con.load(constituent,D_Constituent.EXPAND_ALL);
					asn1.constituent = con;
					String org_id = con.organization_ID;
					ArrayList<Object> org_data = get_org_db_by_local(org_id);
					asn1.organization = get_ASN_Organization_by_OrgID(org_data,org_id);

					String sql = "SELECT "+table.constituent.constituent_ID
							+" FROM "+table.constituent.TNAME
							+" WHERE "+table.constituent.global_constituent_ID
							+"=?;";
					ArrayList<ArrayList<Object>> local_id = Application.db.select(sql,
							new String[]{asn1.constituent.global_constituent_id});
					if(DEBUG)System.out.println("CONS_ID : "+local_id.get(0).get(0));

					Encoder enc = asn1.getEncoder();	
					byte msg [] = enc.getBytes();
					if(DEBUG)System.out.println("MSG : "+msg);
					
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					if(con.global_organization_ID !=null)pm.org_ID_hash = con.global_organization_ID;
					//pm.motion_ID = ;
					pm.constituent_ID_hash.add(con.global_constituent_id_hash);
					if(con.neighborhood!=null){
						for(int i=0; i<con.neighborhood.length; i++) 
						{
							pm.neighborhood_ID.add(con.neighborhood[i].neighborhoodID);
						}
					}
					
					constituent_msgs.add(pm);
					
					
					if(constituent_msgs.size()>=0)
						WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Circular_queue,
								""+(constituent_msgs.size()-1),WirelessLog.const_type,msg);
				}
			}catch(Exception e){
				if(_DEBUG)System.out.println("Prepared_messages : loadConstituent: EXCEPTION");
				return -1;
			}
			retrieved = constituent_msgs.size();
			if(wrapped) {
				if(DEBUG)System.out.println("Prepare_messages : loadConstituents() : "
						+constituent_msgs.size()+" loaded");
				break;	
			}
			if((constituents.size() < CONSTITUENTS_SIZE)&&(rowID != -1)){
				rowID = -1;
				wrapped = true;
				continue;
			}
			if(DEBUG)System.out.println("Prepare_messages : loadConstituents() : "
					+constituent_msgs.size()+" loaded");
			break;
		}
		return last_row;
	}

	/**
	 * load the Peers to the ArrayList<byte[]>
	 * @param peers_msgs
	 * @param rowID
	 * @return
	 */
	public static long loadPeers(ArrayList<PreparedMessage> peers_msgs, long rowID){
		if(DEBUG)System.out.println("Prepare_messages : loadPeers() : START");
		return _loadPeers(peers_msgs, rowID);
	}

	public static long _loadPeers(ArrayList<PreparedMessage> peers_msgs, long rowID) {
		long start_row = rowID;
		boolean wrapped = false;
		long last_row = -1;
		ArrayList<ArrayList<Object>> peers;
		int retrieved = 0;
		peers_msgs.clear();
		for(;;){
			String sql = "SELECT "+table.peer.fields_peers+
					" FROM "+table.peer.TNAME
					+" WHERE ROWID > "+rowID
					+(wrapped?(" AND ROWID <= "+start_row):"")
					+" ORDER BY ROWID"
					+" LIMIT "+(PEERS_SIZE - retrieved);

			if(DEBUG)System.out.println(sql);
			try{
				peers = Application.db.select(sql, new String[]{});
				if(DEBUG)System.out.println("this time we took : "+peers.size());
				for(ArrayList<Object> peer : peers){
					last_row = Integer.parseInt(peer.get(table.peer.PEER_COL_ID).toString());
					byte[]msg = buildPeerMessage(peer, rowID);
					if(DEBUG)System.out.println("MSG : "+msg);
					
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					
					peers_msgs.add(pm);
					
					
					if(peers_msgs.size()>=0)
						WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Circular_queue,
								""+(peers_msgs.size()-1),WirelessLog.peer_type,msg);
				}
			}catch(Exception e){
				if(_DEBUG)System.out.println("Prepared_messages : loadPeers: EXCEPTION");
				return -1;
			}
			retrieved = peers_msgs.size();
			if(wrapped){
				if(DEBUG)System.out.println("Prepare_messages : loadPeers() : "
						+peers_msgs.size()+" loaded");
				break;
			}
			if((peers.size() < PEERS_SIZE)&&(rowID != -1)) {
				rowID = -1;
				wrapped = true;
				continue;
			}
			if(DEBUG)System.out.println("Prepare_messages : loadPeers() : "
					+peers_msgs.size()+" loaded");
			break;
		}
		return last_row;
	}

	/**
	 * loadVotes
	 * @param vote_msgs
	 * @param rowID
	 * @return last vote index
	 */
	public static long loadVotes(ArrayList<PreparedMessage> vote_msgs, long rowID) {
		if(DEBUG)System.out.println("Prepare_messages : loadVotes() : START");
		return _loadVotes(vote_msgs, rowID);
	}
	static String sql_votes =
			"SELECT "
					+Util.setDatabaseAlias(table.signature.fields,"v")
					+", c."+table.constituent.global_constituent_ID
					+", m."+table.motion.global_motion_ID
					+", j."+table.justification.global_justification_ID
					+", o."+table.organization.global_organization_ID
					+", o."+table.organization.organization_ID
					+" FROM "+table.signature.TNAME+" AS v "
					+" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"
					+" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"
					+" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"
					+" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"; 

	public static long _loadVotes(ArrayList<PreparedMessage> vote_msgs, long rowID) {
		long start_row = rowID;
		boolean wrapped = false;
		long last_row = -1;
		ArrayList<ArrayList<Object>> votes;
		int retrieved = 0;
		vote_msgs.clear();
		for(;;){
			String sql = 
					sql_votes+
					" WHERE v.ROWID > "+rowID+
					(wrapped?(" AND v.ROWID <= "+start_row):"")+
					" AND v."+table.signature.signature+" IS NOT NULL "+
					//	" AND j."+table.justification.broadcasted+" <> '0' "+
					" AND m."+table.motion.broadcasted+" <> '0' "+
					" AND o."+table.organization.broadcasted+" <> '0' "+
					" AND c."+table.constituent.broadcasted+" <> '0' "+
					" ORDER BY v.ROWID"+
					" LIMIT "+(VOTES_SIZE - retrieved);
			try{
				votes = Application.db.select(sql, new String[]{});
				if(DEBUG)System.out.println("loadVotes : ArrayList size "+votes.size());
				for(ArrayList<Object> vote : votes){
					D_Vote v = new D_Vote();
					D_Message asn1=new D_Message();
					asn1.sender = new D_PeerAddress(); 
					asn1.sender.globalID = DD.getMyPeerGIDFromIdentity();
					last_row = Integer.parseInt(vote.get(table.signature.S_ID).toString());
					v.init_all(vote);
					asn1.vote = v;
					if(DEBUG)System.out.println("VOTE_ID : "+asn1.vote.vote_ID);
					if(DEBUG)System.out.println("VOTE DATA : "+asn1.vote);
					ArrayList<Object> org_data = get_org_db_by_local(v.organization_ID);
					asn1.organization = get_ASN_Organization_by_OrgID(org_data,v.organization_ID);
					Encoder enc = asn1.getEncoder();	
					byte msg [] = enc.getBytes();
					if(DEBUG)System.out.println("MSG : "+msg.length);					
					if(DEBUG)System.out.println("MSG : "+msg);
					
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					if(asn1.organization!=null)pm.org_ID_hash = asn1.organization.global_organization_IDhash;
					if(v.motion!=null)pm.motion_ID = v.motion.global_motionID;
					if(v.constituent!=null)pm.constituent_ID_hash.add(v.constituent.global_constituent_id_hash);
					if(v.justification!=null)pm.justification_ID=v.justification.global_justificationID;
					//pm.neighborhood_ID = ;
					
					vote_msgs.add(pm);
					
					
					
					if(vote_msgs.size()>=0)
						WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Circular_queue,
								""+(vote_msgs.size()-1),WirelessLog.vote_type,msg);
				}
			}catch(Exception e){
				if(_DEBUG)System.out.println("Prepared_messages : loadVotes: EXCEPTION");
				e.printStackTrace();
				return -1;
			}
			retrieved = vote_msgs.size();
			if(wrapped) {
				if(DEBUG)System.out.println("Prepare_messages : loadVotes() : "
						+vote_msgs.size()+" loaded");
				break;
			}
			if((votes.size() < VOTES_SIZE)&&(rowID != -1)) {
				rowID = -1;
				wrapped = true;
				continue;
			}
			if(DEBUG)System.out.println("Prepare_messages : loadVotes() : "
					+vote_msgs.size()+" loaded");
			break;
		}
		return last_row;
	} 

	/**
	 * loadNeighborhoods
	 * @param neighs_msgs
	 * @param rowID
	 * @return long
	 */
	public static long loadNeighborhoods(ArrayList<PreparedMessage> neighs_msgs, long rowID) {
		if(DEBUG)System.out.println("Prepare_messages : loadNeighborhoods() : START");
		return _loadNeighborhoods(neighs_msgs, rowID);
	}

	static String sql_neighs =
			"SELECT n."
					+table.neighborhood.neighborhood_ID
					+" FROM "+table.neighborhood.TNAME+" AS n "
					+" LEFT JOIN "+table.neighborhood.TNAME+" AS d ON(d."+table.neighborhood.parent_nID+"=n."+table.neighborhood.neighborhood_ID+") "
					+" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=n."+table.neighborhood.organization_ID+") "
					+" WHERE d."+table.neighborhood.neighborhood_ID+" IS NULL"
					;

	public static long _loadNeighborhoods(ArrayList<PreparedMessage> neighs_msgs, long rowID) {
		long start_row = rowID;
		boolean wrapped = false;
		long last_row = -1;
		ArrayList<ArrayList<Object>> neighs;
		int retrieved = 0;
		neighs_msgs.clear();
		int i=0;
		for(;;){
			String sql = 
					sql_neighs+
					" AND n.ROWID > "+rowID+
					(wrapped?(" AND n.ROWID <= "+start_row):"")+
					" AND n."+table.neighborhood.signature+" IS NOT NULL " +
					" AND o."+table.organization.broadcasted+" <> '0' "+
					" ORDER BY n.ROWID"+
					" LIMIT "+(NEIGHS_SIZE - retrieved);
			if(DEBUG)System.out.println("sql_neighs : "+sql);
			try{
				neighs = Application.db.select(sql, new String[]{});
				if(DEBUG)System.out.println("loadNeighborhoods : ArrayList size "+neighs.size());
				for(ArrayList<Object> neigh : neighs) {
					D_Message asn1=new D_Message();
					asn1.sender = new D_PeerAddress(); 
					asn1.sender.globalID = DD.getMyPeerGIDFromIdentity();
					String local_id = Util.getString(neigh.get(0));

					String gid = D_Neighborhood.getNeighborhoodGlobalID(local_id);
					D_Neighborhood n[] = new  D_Neighborhood[6];
					n = D_Neighborhood.getNeighborhoodHierarchy(gid, local_id, 2);
					asn1.neighborhoods = n;
					for(int j=0;j<asn1.neighborhoods.length;j++)
						if(DEBUG)System.out.println("NID : "+asn1.neighborhoods[j].neighborhoodID);
					if(DEBUG)System.out.println("NID : "+asn1.neighborhoods[asn1.neighborhoods.length-1].neighborhoodID);
					last_row  = Integer.parseInt(asn1.neighborhoods[0].neighborhoodID);
					long c_id = Integer.parseInt(asn1.neighborhoods[0].submitter_ID);
					asn1.constituent = new D_Constituent(c_id);

					String sql_local = "SELECT "+table.constituent.constituent_ID
							+" FROM "+table.constituent.TNAME
							+" WHERE "+table.constituent.global_constituent_ID
							+"=?;";
					ArrayList<ArrayList<Object>> cons_local_id = Application.db.select(sql_local,
							new String[]{asn1.constituent.global_constituent_id});
					if(DEBUG)System.out.println("CONS_ID : "+cons_local_id.get(0).get(0));
					Encoder enc = asn1.getEncoder();	
					byte msg [] = enc.getBytes();
					if(DEBUG)System.out.println("MSG : "+msg.length);
					
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					//pm.org_ID_hash = Util.getString(neighs.get(table.organization.ORG_COL_GID_HASH));
					//pm.motion_ID = Util.getString(neighs.get(table.motion.M_MOTION_GID));
					//pm.constituent_ID_hash = Util.getString(neighs.get(table.constituent.CONST_COL_GID_HASH));
					//pm.neighborhood_ID = Util.getString(neighs.get(table.neighborhood.IDX_GID));
					if(n!=null){
						for(int j=0;j< n.length;j++){
							pm.neighborhood_ID.add(n[j].global_neighborhood_ID);
						}
					}
					
					neighs_msgs.add(pm);
					
					if(neighs_msgs.size()>=0)
						WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Circular_queue,
								""+(neighs_msgs.size()-1),WirelessLog.neigh_type,msg);
				}
			}catch(Exception e){
				if(_DEBUG)System.out.println("Prepared_messages : loadNeighborhoods: EXCEPTION");
				e.printStackTrace();
				return -1;
			}

			retrieved = neighs_msgs.size();
			if(wrapped) {
				if(DEBUG)System.out.println("Prepare_messages : loadNeighborhoods() : "
						+neighs_msgs.size()+" loaded");
				break;
			}

			if((neighs.size() < NEIGHS_SIZE)&&(rowID != -1)) {
				rowID = -1;
				wrapped = true;
				continue;
			}
			if(DEBUG)System.out.println("Prepare_messages : loadNeighborhoods() : "
					+neighs_msgs.size()+" loaded");
			break;
		}
		return last_row;
	}


	/**
	 * get_org_db_by_local
	 * @param org_id
	 * @return ArrayList<Object>
	 * @throws SQLiteException
	 */
	public static ArrayList<Object> get_org_db_by_local(String org_id) throws SQLiteException {
		String sql = "SELECT "+table.organization.org_list+" FROM "+
				table.organization.TNAME+" WHERE "+table.organization.organization_ID+
				"=?;";
		ArrayList<ArrayList<Object>> org_data = Application.db.select(sql, new String[]{org_id});
		return org_data.get(0);
	}

	/**
	 * buildPeerMessage() : build the Peer MSG
	 * @param peer
	 * @param rowID
	 * @return byte[]
	 * @throws SQLiteException
	 * @throws ASN1DecoderFail
	 */
	@SuppressWarnings("static-access")
	private static byte[] buildPeerMessage(ArrayList<Object> peer, long rowID) throws SQLiteException, ASN1DecoderFail {
		D_Message asn1=new D_Message();
		asn1.sender = new D_PeerAddress();
		asn1.sender.globalID = DD.getMyPeerGIDFromIdentity();
		asn1.Peer = new D_PeerAddress();
		asn1.Peer.globalID = Util.getString(peer.get(table.peer.PEER_COL_GID));
		asn1.Peer.served_orgs = data.D_PeerAddress._getPeerOrgs(rowID);
		if(peer.get(3)!=null) asn1.Peer.name = Util.getString(peer.get(table.peer.PEER_COL_NAME));
		if(peer.get(4)!=null) asn1.Peer.slogan = Util.getString(peer.get(table.peer.PEER_COL_SLOGAN));
		asn1.Peer.signature_alg = D_PeerAddress.getHashAlgFromString(Util.getString(peer.get(table.peer.PEER_COL_HASH_ALG)));
		asn1.Peer.signature = Util.byteSignatureFromString(Util.getString(peer.get(table.peer.PEER_COL_SIGN)));
		asn1.Peer.creation_date = Util.getCalendar(Util.getString(peer.get(table.peer.PEER_COL_CREATION)));
		if(Util.getString(peer.get(table.peer.PEER_COL_BROADCAST)).compareTo("0")==0)
			asn1.Peer.broadcastable = Boolean.FALSE;
		if(peer.get(table.peer.PEER_COL_BROADCAST).toString().compareTo("1")==0)
			asn1.Peer.broadcastable = Boolean.TRUE;
		asn1.Peer.address = get_Paddress(Util.getString(peer.get(table.peer.PEER_COL_ID)));
		if(DEBUG)System.out.println("PEER DATA : "+asn1.Peer);
		if(DEBUG)System.out.println("PEER_ID : "+asn1.Peer.getLocalPeerIDforGID(asn1.Peer.globalID));
		Encoder enc = asn1.getEncoder();	
		byte msg [] = enc.getBytes();
		return msg;
	}

	public static TypedAddress[] get_Paddress(String peer_id) throws SQLiteException {
		int i=-1;
		String sql = "SELECT "+table.peer_address.fields_peer_address+
				" FROM "+table.peer_address.TNAME+
				" WHERE "+table.peer_address.peer_ID+"=?;";
		ArrayList<ArrayList<Object>> paddress_data = Application.db.select(sql, new String[]{peer_id});
		TypedAddress[] p_ad = new TypedAddress[paddress_data.size()];
		for(ArrayList<Object> pa : paddress_data){
			p_ad[++i] = new TypedAddress();
			p_ad[i] = get_TA(pa);
		}
		return p_ad;
	}

	private static TypedAddress get_TA(ArrayList<Object> pa) {
		TypedAddress pa_ta = new TypedAddress();
		pa_ta.address = pa.get(0).toString();
		pa_ta.type = pa.get(1).toString();
		return pa_ta;
	}

	/**
	 * buildOrganizationMessage() : build the Org MSG
	 * @param org
	 * @param local_id
	 * @return
	 * @throws SQLiteException
	 * @throws ASN1DecoderFail
	 */

	@SuppressWarnings("static-access")
	static byte[] buildOrganizationMessage(ArrayList<Object> org, String local_id) throws SQLiteException, ASN1DecoderFail{
		D_Message asn1=new D_Message();
		asn1.sender = new D_PeerAddress(); 
		asn1.sender.globalID = DD.getMyPeerGIDFromIdentity();//DD.getAppText(DD.APP_my_global_peer_ID);
		asn1.organization = new D_Organization();
		asn1.organization =  get_ASN_Organization_by_OrgID(org,local_id);
		if(DEBUG){
			if(_DEBUG)System.out.println("Prepare_messages:buildOrganizationMessage: org is "+asn1.organization);
			if(!asn1.organization.verifySign(asn1.organization.signature))
				if(_DEBUG)System.out.println("Fail to verify signature!");
		}
		if(DEBUG)System.out.println("ORG_ID : "+asn1.organization.getLocalOrgID(asn1.organization.global_organization_ID));
		Encoder enc = asn1.getEncoder();	
		byte msg [] = enc.getBytes();
		return msg;
	}

	/**
	 * 
	 * @param _org
	 * @param _local_id
	 * @return
	 * @throws SQLiteException
	 */
	public static D_Organization get_ASN_Organization_by_OrgID(ArrayList<Object> _org, String _local_id)
			throws SQLiteException {
		/*
		D_Organization ASN_Org = new D_Organization();
		ASN_Org.params = new D_OrgParams();
		ASN_Org.concepts = new D_OrgConcepts();
		// packing organization values into ASN 
		ASN_Org.global_organization_ID=Util.getString(_org.get(table.organization.ORG_COL_GID));
		ASN_Org.name=Util.getString(_org.get(table.organization.ORG_COL_NAME));
		ASN_Org.creator = new D_PeerAddress();
		ASN_Org.creator	= Get_creatorby_ID(Util.getString(_org.get(table.organization.ORG_COL_CREATOR_ID)));
		ASN_Org.signature = Util.byteSignatureFromString(Util.getString(_org.get(table.organization.ORG_COL_SIGN)));
		ASN_Org.setGT(Util.getString(_org.get(table.organization.ORG_COL_CREATION_DATE)));
		ASN_Org.params.creation_time = Util.getCalendar(Util.getString(_org.get(table.organization.ORG_COL_CREATION_DATE)));
		ASN_Org.params.certifMethods = Integer.parseInt(Util.getString(_org.get(table.organization.ORG_COL_CERTIF_METHODS)));
		ASN_Org.params.hash_org_alg = Util.getString(_org.get(table.organization.ORG_COL_HASH_ALG));
		ASN_Org.params.creator_global_ID = ASN_Org.creator.globalID;
		ASN_Org.params.category = Util.getString(_org.get(table.organization.ORG_COL_CATEG));
		ASN_Org.params.certificate = Util.byteSignatureFromString(Util.getString(_org.get(table.organization.ORG_COL_CERTIF_DATA),null));
		ASN_Org.params.default_scoring_options = D_OrgConcepts.stringArrayFromString(Util.getString(_org.get(table.organization.ORG_COL_SCORES),null));//new String[0];//null;
		ASN_Org.params.instructions_new_motions = Util.getString(_org.get(table.organization.ORG_COL_INSTRUC_MOTION),null);
		ASN_Org.params.instructions_registration = Util.getString(_org.get(table.organization.ORG_COL_INSTRUC_REGIS),null);
		ASN_Org.params.description = Util.getString(_org.get(table.organization.ORG_COL_DESCRIPTION),null);
		ASN_Org.params.languages = D_OrgConcepts.stringArrayFromString(Util.getString(_org.get(table.organization.ORG_COL_LANG)));
		ASN_Org.concepts.name_organization = D_OrgConcepts.stringArrayFromString(Util.getString(_org.get(table.organization.ORG_COL_NAME_ORG),null));
		ASN_Org.concepts.name_forum = D_OrgConcepts.stringArrayFromString(Util.getString(_org.get(table.organization.ORG_COL_NAME_FORUM),null));
		ASN_Org.concepts.name_motion = D_OrgConcepts.stringArrayFromString(Util.getString(_org.get(table.organization.ORG_COL_NAME_MOTION),null));
		ASN_Org.concepts.name_justification = D_OrgConcepts.stringArrayFromString(Util.getString(_org.get(table.organization.ORG_COL_NAME_JUST),null));
		 */
		D_Organization ASN_Org1 =new D_Organization();
		try {
			ASN_Org1 = new D_Organization(Integer.parseInt(_local_id));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		ASN_Org1.creator = new D_PeerAddress(Util.getString(_org.get(table.organization.ORG_COL_CREATOR_ID)),true);

		return ASN_Org1;
	}

	/**
	 * construct Peer MSG
	 * @param global_creator_id
	 * @return
	 * @throws SQLiteException
	 */
	private static D_PeerAddress Get_creatorby_ID(String global_creator_id) throws SQLiteException {
		D_PeerAddress l_peer = new D_PeerAddress();
		ArrayList<ArrayList<Object>> peers = new ArrayList<ArrayList<Object>>();
		String sql;
		sql = "select "+table.peer.fields_peers+
				" from "+table.peer.TNAME+//", "+table.peer_address.TNAME+
				" WHERE peer.peer_ID =?";// and peer_address.peer_ID =?;" 
		;
		peers = Application.db.select(sql,new String[]{global_creator_id});
		for(ArrayList<Object> peer : peers) {
			l_peer.globalID =Util.getString(peer.get(table.peer.PEER_COL_GID)); ;
			l_peer.served_orgs = data.D_PeerAddress._getPeerOrgs(global_creator_id);
			if(peer.get(table.peer.PEER_COL_NAME)!=null) l_peer.name = Util.getString(peer.get(table.peer.PEER_COL_NAME));
			if(peer.get(table.peer.PEER_COL_SLOGAN)!=null) l_peer.slogan = Util.getString(peer.get(table.peer.PEER_COL_SLOGAN));
			l_peer.signature_alg = Util.getString(peer.get(table.peer.PEER_COL_HASH_ALG)).split(":");
			l_peer.signature = Util.byteSignatureFromString(Util.getString(peer.get(table.peer.PEER_COL_SIGN)));
			l_peer.creation_date = Util.getCalendar(Util.getString(peer.get(table.peer.PEER_COL_CREATION)));
			if(Util.getString(peer.get(table.peer.PEER_COL_BROADCAST)).compareTo("0")==0)
				l_peer.broadcastable = Boolean.FALSE;
			if(peer.get(table.peer.PEER_COL_BROADCAST).toString().compareTo("1")==0)
				l_peer.broadcastable = Boolean.TRUE;
			l_peer.address = get_Paddress(Util.getString(peer.get(table.peer.PEER_COL_ID)));
		}
		l_peer.served_orgs = data.D_PeerAddress._getPeerOrgs(global_creator_id);
		l_peer.address = D_PeerAddress.getAddress(UpdatePeersTable.getPeerAddresses(global_creator_id));
		return l_peer;
	}

}
