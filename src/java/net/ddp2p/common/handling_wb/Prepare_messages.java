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

package net.ddp2p.common.handling_wb;


import java.util.ArrayList;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Message;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_OrgConcepts;
import net.ddp2p.common.data.D_OrgParams;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.simulator.WirelessLog;
import net.ddp2p.common.streaming.UpdatePeersTable;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

public class Prepare_messages {

	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static int PEERS_SIZE = 10;
	public static int ORGS_SIZE = 10;
	public static int VOTES_SIZE = 1000;
	public static int CONSTITUENTS_SIZE = 10;
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
	 * @throws P2PDDSQLException
	 */
	public static long loadOrgs(ArrayList<PreparedMessage> org_msgs, long rowID) {
		if(DEBUG)System.out.println("Prepare_messages : loadOrgs() : START");
		try { 
			return _loadOrgs(org_msgs, rowID);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public static long _loadOrgs(ArrayList<PreparedMessage> org_msgs, long rowID) throws P2PDDSQLException {
		long start_row = rowID;
		boolean wrapped = false;
		long last_row = -1;
		ArrayList<ArrayList<Object>> orgs;
		int retrieved = 0;
		org_msgs.clear();
		for(;;){
			String sql = "SELECT "+net.ddp2p.common.table.organization.field_list+
					" FROM "+net.ddp2p.common.table.organization.TNAME
					+" WHERE ROWID > "+rowID+
					(wrapped?(" AND ROWID <= "+start_row):"")+
					" AND " +net.ddp2p.common.table.organization.signature+ " IS NOT NULL "+
					" AND "+net.ddp2p.common.table.organization.broadcasted+" <> '0' " +
					" ORDER BY ROWID"
					+" LIMIT "+(ORGS_SIZE - retrieved);

			if(DEBUG)System.out.println("Prepared : messages : "+sql);
			if(rowID==-1) rowID=1;
			if(DEBUG)System.out.println("ROWID: "+rowID);
			try{
				orgs = Application.getDB().select(sql, new String[]{});
				for(ArrayList<Object> org : orgs){
					last_row = Integer.parseInt(org.get(net.ddp2p.common.table.organization.ORG_COL_ID).toString());
					byte[]msg = buildOrganizationMessage(org, ""+rowID);
					if(DEBUG)System.out.println("MSG : "+msg);
					
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					pm.org_ID_hash = Util.getString(org.get(net.ddp2p.common.table.organization.ORG_COL_GID_HASH));
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
					+ Util.setDatabaseAlias(net.ddp2p.common.table.witness.witness_fields, "w")+" "+
					", n."+net.ddp2p.common.table.neighborhood.global_neighborhood_ID+
					", t."+net.ddp2p.common.table.constituent.global_constituent_ID+
					", s."+net.ddp2p.common.table.constituent.global_constituent_ID+
					", o."+net.ddp2p.common.table.organization.global_organization_ID+
					", o."+net.ddp2p.common.table.organization.organization_ID+
					" FROM "+net.ddp2p.common.table.witness.TNAME+" AS w "+
					" LEFT JOIN "+net.ddp2p.common.table.neighborhood.TNAME+" AS n ON(n."+net.ddp2p.common.table.neighborhood.neighborhood_ID+"=w."+net.ddp2p.common.table.witness.neighborhood_ID+") "+
					" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS s ON(s."+net.ddp2p.common.table.constituent.constituent_ID+"=w."+net.ddp2p.common.table.witness.source_ID+") "+
					" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS t ON(t."+net.ddp2p.common.table.constituent.constituent_ID+"=w."+net.ddp2p.common.table.witness.target_ID+") "+
					" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(o."+net.ddp2p.common.table.organization.organization_ID+"=s."+net.ddp2p.common.table.constituent.organization_ID+") ";

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
					" AND w."+net.ddp2p.common.table.witness.signature +" IS NOT NULL "+
					" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' " +
					" AND s."+net.ddp2p.common.table.constituent.broadcasted+" <> '0' " +
					" ORDER BY w.ROWID "
					+" LIMIT "+(WITNESSES_SIZE - retrieved);

			D_Witness wbw = new D_Witness();
			try{
				witnesses = Application.getDB().select(sql, new String[]{});
				for(ArrayList<Object> witness : witnesses){
					D_Message asn1=new D_Message();
					asn1.sender = D_Peer.getEmpty(); 
					asn1.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID();
					last_row = Integer.parseInt(witness.get(net.ddp2p.common.table.witness.WIT_COL_ID).toString());
					try{
						wbw.init_all(witness);
					}catch(Exception e){e.printStackTrace();}
					asn1.witness = wbw;
					if(DEBUG)System.out.println("Wintnessed_constituent INFO : "+asn1.witness.witnessed);
					if(DEBUG)System.out.println("Wintnessing_constituent INFO : "+asn1.witness.witnessing);
					if(DEBUG)System.out.println("Witness_ID : "+asn1.witness.witnessID);
					String sql1 = " SELECT "+net.ddp2p.common.table.constituent.organization_ID+
							" FROM "+net.ddp2p.common.table.constituent.TNAME+" WHERE "+
							net.ddp2p.common.table.constituent.constituent_ID+"=?;";
					ArrayList<ArrayList<Object>> org_id = Application.getDB().select(sql1, new String[]{""+wbw.witnessed_constituentID});
					String OrgID = Util.getString(org_id.get(0).get(0));	
					if(DEBUG)System.out.println("DATA : "+OrgID);
					ArrayList<Object> org_data = get_org_db_by_local(OrgID);
					asn1.organization = get_ASN_Organization_by_OrgID(org_data,OrgID);
					Encoder enc = asn1.getEncoder();	
					byte msg [] = enc.getBytes();
					if(DEBUG)System.out.println("MSG : "+msg);
					
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					if(asn1.organization!=null)pm.org_ID_hash = asn1.organization.getGIDH(); 
					if(wbw.witnessing!=null){
							pm.constituent_ID_hash.add(wbw.witnessing.getGIDH()); 
							for(int i=0; i<wbw.witnessing.getNeighborhood().length; i++) {pm.neighborhood_ID.add(wbw.witnessing.getNeighborhood()[i].getLIDstr());}
					}
					if(wbw.witnessed!=null){
							pm.constituent_ID_hash.add(wbw.witnessed.getGIDH()); 
							for(int i=0; i<wbw.witnessed.getNeighborhood().length; i++) {pm.neighborhood_ID.add(wbw.witnessed.getNeighborhood()[i].getLIDstr());}
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
	static String c_fields_constituents = Util.setDatabaseAlias(net.ddp2p.common.table.constituent.fields_constituents,"c");
	static String sql_cons =
			"SELECT "
					+c_fields_constituents
					+",n."+net.ddp2p.common.table.neighborhood.global_neighborhood_ID
					+" FROM "+net.ddp2p.common.table.constituent.TNAME+" as c " 
					+" LEFT JOIN "+net.ddp2p.common.table.neighborhood.TNAME+" AS n ON(c."+net.ddp2p.common.table.constituent.neighborhood_ID+" = n."+net.ddp2p.common.table.neighborhood.neighborhood_ID+") "
					+" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(c."+net.ddp2p.common.table.constituent.organization_ID+" = o."+net.ddp2p.common.table.organization.organization_ID+") ";


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
					" AND c."+net.ddp2p.common.table.constituent.sign+" IS NOT NULL"+
					" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' "+
					" AND c."+net.ddp2p.common.table.constituent.broadcasted+" <> '0' "+
					" ORDER BY c.ROWID "+
					" LIMIT "+(CONSTITUENTS_SIZE - retrieved);

			if(DEBUG)System.out.println(sql_get_const);
			try{
				constituents = Application.getDB().select(sql_get_const, new String[]{});
				for(ArrayList<Object> constituent : constituents) {
					Long LID = Util.Lval(constituent.get(net.ddp2p.common.table.constituent.CONST_COL_ID));
					D_Constituent con = D_Constituent.getConstByLID(LID, true, false);
					D_Message asn1=new D_Message();
					asn1.sender = D_Peer.getEmpty(); 
					asn1.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID();
					last_row = Integer.parseInt(constituent.get(net.ddp2p.common.table.constituent.CONST_COL_ID).toString());
					con.loadNeighborhoods(D_Constituent.EXPAND_ALL);
					asn1.constituent = con;
					String org_id = con.getLIDstr();
					ArrayList<Object> org_data = get_org_db_by_local(org_id);
					asn1.organization = get_ASN_Organization_by_OrgID(org_data,org_id);
					/*
					String sql = "SELECT "+table.constituent.constituent_ID
							+" FROM "+table.constituent.TNAME
							+" WHERE "+table.constituent.global_constituent_ID
							+"=?;";
					ArrayList<ArrayList<Object>> local_id = Application.db.select(sql,
							new String[]{asn1.constituent.global_constituent_id});
					if(DEBUG)System.out.println("CONS_ID : "+local_id.get(0).get(0));
					*/
					Encoder enc = asn1.getEncoder();	
					byte msg [] = enc.getBytes();
					if(DEBUG)System.out.println("MSG : "+msg);
					
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					if(con.getOrganizationGID() !=null)pm.org_ID_hash = con.getOrganizationGID();
					//pm.motion_ID = ;
					pm.constituent_ID_hash.add(con.getGIDH());
					if(con.getNeighborhood()!=null){
						for(int i=0; i<con.getNeighborhood().length; i++) 
						{
							pm.neighborhood_ID.add(con.getNeighborhood()[i].getLIDstr());
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
			String sql = "SELECT "+net.ddp2p.common.table.peer.fields_peers+
					" FROM "+net.ddp2p.common.table.peer.TNAME
					+" WHERE ROWID > "+rowID
					+(wrapped?(" AND ROWID <= "+start_row):"")
					+" ORDER BY ROWID"
					+" LIMIT "+(PEERS_SIZE - retrieved);

			if(DEBUG)System.out.println(sql);
			try{
				peers = Application.getDB().select(sql, new String[]{});
				if(DEBUG)System.out.println("this time we took : "+peers.size());
				for(ArrayList<Object> peer : peers){
					last_row = Integer.parseInt(peer.get(net.ddp2p.common.table.peer.PEER_COL_ID).toString());
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
					+Util.setDatabaseAlias(net.ddp2p.common.table.signature.fields,"v")
					//+", c."+table.constituent.global_constituent_ID
					//+", m."+table.motion.global_motion_ID
					//+", j."+table.justification.global_justification_ID
					//+", o."+table.organization.global_organization_ID
					//+", o."+table.organization.organization_ID
					+" FROM "+net.ddp2p.common.table.signature.TNAME+" AS v "
					+" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON(c."+net.ddp2p.common.table.constituent.constituent_ID+"=v."+net.ddp2p.common.table.signature.constituent_ID+")"
					+" LEFT JOIN "+net.ddp2p.common.table.motion.TNAME+" AS m ON(m."+net.ddp2p.common.table.motion.motion_ID+"=v."+net.ddp2p.common.table.signature.motion_ID+")"
					//+" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"
					+" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(o."+net.ddp2p.common.table.organization.organization_ID+"=m."+net.ddp2p.common.table.motion.organization_ID+")"; 

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
					" AND v."+net.ddp2p.common.table.signature.signature+" IS NOT NULL "+
					//	" AND j."+table.justification.broadcasted+" <> '0' "+
					" AND m."+net.ddp2p.common.table.motion.broadcasted+" <> '0' "+
					" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' "+
					" AND c."+net.ddp2p.common.table.constituent.broadcasted+" <> '0' "+
					" ORDER BY v.ROWID"+
					" LIMIT "+(VOTES_SIZE - retrieved);
			try{
				votes = Application.getDB().select(sql, new String[]{});
				if(DEBUG)System.out.println("loadVotes : ArrayList size "+votes.size());
				for(ArrayList<Object> vote : votes){
					D_Vote v = new D_Vote();
					D_Message asn1=new D_Message();
					asn1.sender = D_Peer.getEmpty(); 
					asn1.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID();
					last_row = Integer.parseInt(vote.get(net.ddp2p.common.table.signature.S_ID).toString());
					v.init_all(vote);
					asn1.vote = v;
					if(DEBUG)System.out.println("VOTE_ID : "+asn1.vote.getLIDstr());
					if(DEBUG)System.out.println("VOTE DATA : "+asn1.vote);
					ArrayList<Object> org_data = get_org_db_by_local(v.getOrganizationLIDstr());
					asn1.organization = get_ASN_Organization_by_OrgID(org_data,v.getOrganizationLIDstr());
					Encoder enc = asn1.getEncoder();	
					byte msg [] = enc.getBytes();
					if(DEBUG)System.out.println("MSG : "+msg.length);					
					if(DEBUG)System.out.println("MSG : "+msg);
					
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					if(asn1.organization!=null)pm.org_ID_hash = asn1.organization.getGIDH();
					if(v.getMotionFromObjOrLID()!=null)pm.motion_ID = v.getMotionFromObjOrLID().getGID();
					if(v.getConstituent_force()!=null)pm.constituent_ID_hash.add(v.getConstituent_force().getGIDH());
					if(v.getJustificationFromObjOrLID()!=null)pm.justification_ID=v.getJustificationFromObjOrLID().getGID();
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
					+net.ddp2p.common.table.neighborhood.neighborhood_ID
					+" FROM "+net.ddp2p.common.table.neighborhood.TNAME+" AS n "
					+" LEFT JOIN "+net.ddp2p.common.table.neighborhood.TNAME+" AS d ON(d."+net.ddp2p.common.table.neighborhood.parent_nID+"=n."+net.ddp2p.common.table.neighborhood.neighborhood_ID+") "
					+" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(o."+net.ddp2p.common.table.organization.organization_ID+"=n."+net.ddp2p.common.table.neighborhood.organization_ID+") "
					+" WHERE d."+net.ddp2p.common.table.neighborhood.neighborhood_ID+" IS NULL"
					;

	public static long _loadNeighborhoods(ArrayList<PreparedMessage> neighs_msgs, long rowID) {
		long start_row = rowID;
		boolean wrapped = false;
		long olID = -1;
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
					" AND n."+net.ddp2p.common.table.neighborhood.signature+" IS NOT NULL " +
					" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' "+
					" ORDER BY n.ROWID"+
					" LIMIT "+(NEIGHS_SIZE - retrieved);
			if(DEBUG)System.out.println("sql_neighs : "+sql);
			try{
				neighs = Application.getDB().select(sql, new String[]{});
				if(DEBUG)System.out.println("loadNeighborhoods : ArrayList size "+neighs.size());
				for(ArrayList<Object> neigh : neighs) {
					D_Message asn1=new D_Message();
					asn1.sender = D_Peer.getEmpty(); 
					asn1.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID();
					String local_id = Util.getString(neigh.get(0));

					String gid = D_Neighborhood.getGIDFromLID(local_id);
					D_Neighborhood n[] = new  D_Neighborhood[6];
					n = D_Neighborhood.getNeighborhoodHierarchy(gid, local_id, 2, olID);
					asn1.neighborhoods = n;
					for(int j=0;j<asn1.neighborhoods.length;j++)
						if(DEBUG)System.out.println("NID : "+asn1.neighborhoods[j].getLID());
					if(DEBUG)System.out.println("NID : "+asn1.neighborhoods[asn1.neighborhoods.length-1].getLID());
					last_row  = asn1.neighborhoods[0].getLID();
					long c_id = Long.parseLong(asn1.neighborhoods[0].getSubmitterLIDstr());
					asn1.constituent = D_Constituent.getConstByLID(c_id, true, false);
					/*
					String sql_local = "SELECT "+table.constituent.constituent_ID
							+" FROM "+table.constituent.TNAME
							+" WHERE "+table.constituent.global_constituent_ID
							+"=?;";
					ArrayList<ArrayList<Object>> cons_local_id = Application.db.select(sql_local,
							new String[]{asn1.constituent.global_constituent_id});
					if(DEBUG)System.out.println("CONS_ID : "+cons_local_id.get(0).get(0));
					*/
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
							pm.neighborhood_ID.add(n[j].getGID());
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
	 * @throws P2PDDSQLException
	 */
	public static ArrayList<Object> get_org_db_by_local(String org_id) throws P2PDDSQLException {
		String sql = "SELECT "+net.ddp2p.common.table.organization.field_list+" FROM "+
				net.ddp2p.common.table.organization.TNAME+" WHERE "+net.ddp2p.common.table.organization.organization_ID+
				"=?;";
		ArrayList<ArrayList<Object>> org_data = Application.getDB().select(sql, new String[]{org_id});
		return org_data.get(0);
	}

	/**
	 * buildPeerMessage() : build the Peer MSG
	 * @param peer
	 * @param rowID
	 * @return byte[]
	 * @throws P2PDDSQLException
	 * @throws ASN1DecoderFail
	 */
	@SuppressWarnings("static-access")
	private static byte[] buildPeerMessage(ArrayList<Object> peer, long rowID) throws P2PDDSQLException, ASN1DecoderFail {
		D_Message asn1=new D_Message();
		asn1.sender = D_Peer.getEmpty();
		asn1.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID();
		String GID =  Util.getString(peer.get(net.ddp2p.common.table.peer.PEER_COL_GID));
		asn1.Peer = D_Peer.getPeerByGID_or_GIDhash(GID, null, true, false, false, null);
		
		if(DEBUG)System.out.println("PEER DATA : "+asn1.Peer);
		if(DEBUG)System.out.println("PEER_ID : "+asn1.Peer.getLocalPeerIDforGID(asn1.Peer.component_basic_data.globalID));
		Encoder enc = asn1.getEncoder();	
		byte msg [] = enc.getBytes();
		return msg;
	}
/*
	public static TypedAddress[] get_Paddress(String peer_id) throws P2PDDSQLException {
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
		pa_ta.certified = Util.stringInt2bool(pa.get(2), false);
		pa_ta.priority = Util.get_int(pa.get(3));
		return pa_ta;
	}
*/
	/**
	 * buildOrganizationMessage() : build the Org MSG
	 * @param org
	 * @param local_id
	 * @return
	 * @throws P2PDDSQLException
	 * @throws ASN1DecoderFail
	 */

	@SuppressWarnings("static-access")
	static byte[] buildOrganizationMessage(ArrayList<Object> org, String local_id) throws P2PDDSQLException, ASN1DecoderFail{
		D_Message asn1=new D_Message();
		asn1.sender = D_Peer.getEmpty(); 
		asn1.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID();//DD.getAppText(DD.APP_my_global_peer_ID);
		asn1.organization = null;//new D_Organization();
		asn1.organization =  get_ASN_Organization_by_OrgID(org,local_id);
		if(DEBUG){
			if(_DEBUG)System.out.println("Prepare_messages:buildOrganizationMessage: org is "+asn1.organization);
			if(!asn1.organization.verifySignature()) //(asn1.organization.signature))
				if(_DEBUG)System.out.println("Fail to verify signature!");
		}
		if(DEBUG)System.out.println("ORG_ID : "+asn1.organization.getLIDbyGID(asn1.organization.getGID()));
		Encoder enc = asn1.getEncoder();	
		byte msg [] = enc.getBytes();
		return msg;
	}

	/**
	 * 
	 * @param _org
	 * @param _local_id
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_Organization get_ASN_Organization_by_OrgID(ArrayList<Object> _org, String _local_id)
			throws P2PDDSQLException {
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
		D_Organization ASN_Org1 = null; //new D_Organization();
		try {
			ASN_Org1 = D_Organization.getOrgByLID_NoKeep(_local_id, true);
			ASN_Org1.creator = D_Peer.getPeerByLID_NoKeep(Util.getString(_org.get(net.ddp2p.common.table.organization.ORG_COL_CREATOR_ID)),true);

			return ASN_Org1;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * construct Peer MSG
	 * @param global_creator_id
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static D_Peer Get_creatorby_ID(String global_creator_id) throws P2PDDSQLException {
		return D_Peer.getPeerByLID_NoKeep(global_creator_id, true);
	}

}
