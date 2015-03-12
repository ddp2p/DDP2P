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

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import util.P2PDDSQLException;

import simulator.WirelessLog;
import streaming.RequestData;
import streaming.UpdatePeersTable;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.Decoder;
import ASN1.Encoder;
import config.Application;
import config.DD;
import data.D_Neighborhood;
import data.D_OrgConcepts;
import data.D_Organization;
import data.D_PeerAddress;
import data.D_Message;
import data.D_Constituent;
import data.D_Vote;
import data.D_Witness;
import java.util.Calendar;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class ReceivedBroadcastableMessages {
	public static long all_msg_received=0;
	public static long all_myInterest_msg_received=0;
	public static long all_msg_received_fromMyself=0;
	public static long all_not_myInterest_msg_received=0;
	
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private static final int MY_PEER_BAG_MAX_SIZE = 10;
	public static D_Message public_msg;
	private static boolean CHECK_MESSAGE_SIGNATURES = false;
	public static ArrayList<String> my_bag_of_peers = new ArrayList<String>();
	static String my_GPIDhash;
	private static long last =  Util.CalendargetInstance().getTimeInMillis()-10000;
	private static long crt;
	public static int count_dots = 0;
	
	/**
	 * 
	 * @param obtained
	 * @param _amount  starting point in received message buffer?
	 * @param address
	 * @param length
	 * @param IP
	 * @param cnt_val
	 * @throws P2PDDSQLException
	 * @throws ASN1DecoderFail
	 * @throws IOException
	 */
	public static void integrateMessage(byte[] obtained, int _amount, SocketAddress address,
									int length, String IP, long cnt_val, String Msg_Time) throws P2PDDSQLException, ASN1DecoderFail, IOException {
		
		PreparedMessage pm = new PreparedMessage();
		pm.raw=obtained;
		public_msg = null;
		if(DEBUG)System.out.println("ReceivedBroadcastableMessages : integrateMessage : msg : "+pm.raw);
		Decoder dec = new Decoder(obtained);
		D_Message msg = new D_Message().decode(dec);
		if(DEBUG)System.out.println("integrateMessage : After Decoding msg : msg.vote.org_ID :"+msg.vote.global_organization_ID);
		
		
		
		
		
		public_msg = msg;
		if(obtained!=null)if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage: msg received");
		try{
			long added_org=-1,added_motion=-1,added_constituent=-1,
					added_vote=-1,added_peer=-1,added_witness=-1, added_neigh=-1;
			check_global_peerID(msg); // play Thanks
			/*
			if (check_global_peerID(msg)) {//this condition is useless!  we are using random numbers!
				if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage: Receive from my self");
				//all_msg_received_fromMyself++;
				//if(_DEBUG)System.out.println("So far : Total msg received:"+all_msg_received+" Total myInterest msg received:"+ all_myInterest_msg_received+" Total my own msg received:"+all_msg_received_fromMyself);
				return;
				
			}else */
			{	
				//check if the message does not include myInterests
				all_msg_received++;
				if(handling_wb.BroadcastQueueRequested.myInterests!=null && msg.organization!=null)
				{
					pm.org_ID_hash = msg.organization.global_organization_IDhash;
					boolean exists = false;
					
					if(handling_wb.BroadcastQueueRequested.myInterests.org_ID_hashes!=null)
					for(int i=0; i<handling_wb.BroadcastQueueRequested.myInterests.org_ID_hashes.size(); i++)
						if(handling_wb.BroadcastQueueRequested.myInterests.org_ID_hashes.get(i).equals(pm.org_ID_hash))
						{
							exists = true;
							all_myInterest_msg_received++;
							break;
						}
					if(!exists)
					{
						all_not_myInterest_msg_received++;
						if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage: So far : Total msg received:"+all_msg_received+" Total myInterest msg received:"+ all_myInterest_msg_received+" Total msg received not my interest:"+all_not_myInterest_msg_received);
						//If we want to ignore msgs not of our interests then enable the next line
						//else just keep it comment out in order to receive everything else.
						
						return;
						//check the QRE check mark on the GUI?
						//If the QRE is checked then we must return here!!!
						/*
						String val =  DD.getAppText("Q_RE");
						if(val!=null){
							if(val.equals("1"))
							{
								if(DEBUG) System.out.println("ReceivedBroadcastableMessages:integrateMessage: Q_RE is checked!!");
								return;
							}
						}*/
					}
					//integrateMessage() returns if the message received is not overlapping with myInterests
				}
				if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage: So far : Total msg received:"+all_msg_received+" Total myInterest msg received:"+ all_myInterest_msg_received+" Total msg received not my interest:"+all_not_myInterest_msg_received);

				if((msg.organization!=null)&&(msg.constituent==null)&&(msg.witness==null)&&(msg.vote==null)) { 
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage:RECEIVE ORGANIZATION ONLY"); 
					int result = D_Organization.isGIDhashAvailable(msg.organization.global_organization_IDhash, false);
					//added_org=handle_org(msg.organization);
					pm.org_ID_hash = msg.organization.global_organization_IDhash;
					added_org=handle_org(pm, msg.organization);
					WirelessLog.RCV_logging(WirelessLog.org_type,msg.sender.component_basic_data.globalID,pm.raw,length,result,IP,cnt_val,Msg_Time);
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage:.");
					count_dots++;
				}

				//if(msg.vote != null)System.out.println("Vote is not null!");
				
				if((msg.constituent!=null)&&(msg.neighborhoods==null)&&(msg.vote==null)&&(msg.witness==null)) { 
					if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:RECEIVE CONSTITUENT");
					long added_Org=-1;
					String goid = null;
					if(msg.organization!=null) {
						pm.org_ID_hash = msg.organization.global_organization_IDhash;
						added_Org = handle_org(pm, msg.organization); 
					//goid = GOID_by_local(added_Org);
					}
					if(goid == null) {
						goid = msg.constituent.global_organization_ID;
						pm.constituent_ID_hash.add(msg.constituent.global_constituent_id_hash);
					}
					if(goid==null){
						if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage: So far : Total msg received:"+all_msg_received+" Total myInterest msg received:"+ all_myInterest_msg_received+" Total msg received not my interest:"+all_not_myInterest_msg_received);
						return;
					}
					if(added_Org<=0) added_Org = D_Organization.getLocalOrgID(goid);
					if(added_Org<=0) added_Org = D_Organization.insertTemporaryGID(goid);
					int result = D_Constituent.isGIDHash_available(msg.constituent.global_constituent_id_hash, false);				
					added_constituent = handle_constituent(pm,msg.constituent,goid,added_Org);
					WirelessLog.RCV_logging(WirelessLog.const_type,msg.sender.component_basic_data.globalID,pm.raw,length,result,IP,cnt_val,Msg_Time);
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage: .");
					count_dots++;
				}

				if((msg.witness!=null)&&(msg.constituent==null)&&(msg.vote==null)){
					if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage: RECEIVE Witness : "+msg.witness);
					long added_Org = -1;
					long added_witnessed_cons = -1;
					long added_witnessing_cons = -1;
					String goid = null;
					if(msg.organization!=null){
						pm.org_ID_hash=msg.organization.global_organization_IDhash;
						added_Org = handle_org(pm, msg.organization); 
					}
					//String goid = GOID_by_local(added_Org);
					goid = msg.witness.global_organization_ID;
					if(msg.witness.witnessed!=null) added_witnessed_cons = handle_constituent(pm,msg.witness.witnessed,goid,added_Org);
					if(msg.witness.witnessing!=null) added_witnessing_cons = handle_constituent(pm, msg.witness.witnessing,goid,added_Org);
					int result = D_Witness.isGIDavailable(msg.witness.global_witness_ID, false);
					added_witness = handle_witness(pm,msg.witness);
					WirelessLog.RCV_logging(WirelessLog.wit_type,msg.sender.component_basic_data.globalID,pm.raw,length,result,IP,cnt_val,Msg_Time);
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage:.");
					count_dots++;
				}

				if((msg.neighborhoods!=null)&&(msg.witness==null)&&(msg.vote==null)&&(msg.organization==null)) {
					if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:RECEIVE Neighborhoods hierarchy");
					int result = D_Neighborhood.isGIDavailable(msg.neighborhoods[0].global_neighborhood_ID, false);
					for(int i=0;i<msg.neighborhoods.length;i++) {
						if(DEBUG)System.out.println("Parent_ID : "+msg.neighborhoods[i]);
						added_neigh = handle_Neighborhoods(pm,msg.neighborhoods[i]);
						if(DEBUG)System.out.println("RCV NID : "+added_neigh);
					}
					WirelessLog.RCV_logging(WirelessLog.neigh_type,msg.sender.component_basic_data.globalID,pm.raw,length,result,IP,cnt_val,Msg_Time);
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage:.");
					count_dots++;
				}

				if(msg.vote!=null) { 
					if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:RECEIVE VOTE in ReceivedBroacastableMessage:integrateMessage"); 
					if(msg.organization!=null) { 
						pm.org_ID_hash=msg.organization.global_organization_IDhash;
						//System.out.println("ReceivedBroacastableMessage:integrateMessage(): msg.organization.ID:"+msg.organization.global_organization_ID);				
						long added_Org = handle_org(pm, msg.organization);
						//System.exit(1);
						if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:ORG ID : "+added_Org);
						long added_cons =  handle_constituent(pm, msg.vote.constituent,msg.organization.global_organization_ID,added_Org);
						if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:CONS ID : "+added_cons);
					}
					//else{System.out.println("ReceivedBroacastableMessage:integrateMessage ms.organization is null");}
					int result = D_Vote.isGIDavailable(msg.vote.global_vote_ID, false);
					added_vote = handle_vote(pm, msg.vote);
					WirelessLog.RCV_logging(WirelessLog.vote_type,msg.sender.component_basic_data.globalID,pm.raw,length,result,IP,cnt_val,Msg_Time);
					if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:VOTE DATA : "+msg.vote.global_vote_ID);
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage:.");
					count_dots++;
				}

				if(msg.Peer!=null){
					if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:RECEIVE PEER");
					int result = D_PeerAddress.isGIDavailable(msg.Peer.component_basic_data.globalIDhash, false);
					added_peer = get_peer(msg.Peer);
					WirelessLog.RCV_logging(WirelessLog.peer_type,msg.sender.component_basic_data.globalID,pm.raw,length,result,IP,cnt_val,Msg_Time);
					if(DEBUG)System.out.print("ReceivedBroadcastableMessages:integrateMessage:.");
					count_dots++;
				}
				if(count_dots%40==0) System.out.println();
			}
		}catch(SignatureVerificationException e){e.printStackTrace();}
		if(DEBUG)System.out.println("ReceivedBroadcastableMessages:integrateMessage:So far : Total msg received:"+all_msg_received+" Total myInterest msg received:"+ all_myInterest_msg_received+" Total msg received not my interest:"+all_not_myInterest_msg_received);
	}


	private static String GOID_by_local(long org_id) throws P2PDDSQLException {
		String sql = "select "+table.organization.global_organization_ID+
				" from "+table.organization.TNAME+" WHERE "+table.organization.organization_ID+
				"=?;";
		ArrayList<ArrayList<Object>> org = Application.db.select(sql, new String[]{Util.getStringID(org_id)});
		if(org.size()==0) return null;
		String GOid = Util.getString(org.get(0).get(0));
		return GOid;
	}

	/**
	 * check the new incoming vote if its new add it and return the ID. if its already exist in
		the db do not add it just return the ID
	 * @param vote
	 * @return
	 * @throws P2PDDSQLException
	 * @throws SignatureVerificationException
	 */

	private static long handle_vote(PreparedMessage pm, D_Vote vote) throws P2PDDSQLException, SignatureVerificationException {

		long just_id = -1;
		if(vote.justification!=null) {
			if(DEBUG)System.out.println("with JUSTIFICATION");
			if(DEBUG)System.out.println("JUST Data : "+vote.justification);
			RequestData sol_rq = new RequestData();
			RequestData new_rq = new RequestData();
			try{
				just_id =  vote.justification.store(sol_rq, new_rq);//pm should be passed
			}catch(Exception e){e.printStackTrace();}
			if(DEBUG)System.out.println("JUST ID : "+just_id);
		}
		else 
			if(DEBUG)System.out.println("without justificatio");
		
		long motion_id = -1;
		if(vote.motion!=null) {
			if(DEBUG)System.out.println("with MOTION");
			if(DEBUG)System.out.println("MOTION Data : "+vote.motion);
			RequestData sol_rq = new RequestData();
			RequestData new_rq = new RequestData();
			try{
				motion_id =  vote.motion.store(sol_rq, new_rq);//pm should be passed
			}catch(Exception e){e.printStackTrace();}
			if(DEBUG)System.out.println("MOTION ID : "+motion_id);
		}
		else 
			if(DEBUG)System.out.println("WITHOUT MOTION");

		long vote_id = -1;
		RequestData sol_rq = new RequestData();
		RequestData new_rq = new RequestData();
		try{
			vote_id =  vote.store(pm, sol_rq, new_rq);//pm should be passed
		}catch(Exception e){e.printStackTrace();}
		if(DEBUG)System.out.println("vote ID : "+vote_id);

		return vote_id;
	}

	private static long handle_witness(PreparedMessage pm, D_Witness witness) throws P2PDDSQLException {
		long add_witness =-1;
		RequestData sol_rq = new RequestData();
		RequestData new_rq = new RequestData();
		try{
			add_witness = witness.store(sol_rq, new_rq);//pm should be passed to this function.
			if(DEBUG)System.out.println("handle_witness : after witness.stor");
		}catch(Exception e){e.printStackTrace();}
		return add_witness;
	}

	/**
	 * check the new incoming constituent if its new add it and return the ID. if its already exist in
	   the db do not add it just return the ID
	 * @param constituent
	 * @param goid
	 * @param added_Org
	 * @return
	 * @throws P2PDDSQLException
	 * @throws SignatureVerificationException
	 */
	private static long handle_constituent(PreparedMessage pm,D_Constituent constituent, String goid, long added_Org) throws P2PDDSQLException, SignatureVerificationException {
		String now = Util.getGeneralizedTime();
		if(DEBUG)System.out.println("Org_id : "+added_Org);
		if(DEBUG)System.out.println("Global_Org_id : "+goid);
		if(DEBUG)System.out.println("handle_constituent: Neighborhood data : "+constituent.neighborhood[0]);
		if(DEBUG)System.out.println("Reaceived : handle_constituent() : CON HASH : "+constituent.global_constituent_id_hash);
		return constituent.store(pm, goid,Util.getStringID(added_Org), now, null, null);
	}

	private static long handle_Neighborhoods(PreparedMessage pm,D_Neighborhood neigh) {
		long neigh_id = -1;
		RequestData sol_rq = new RequestData();
		RequestData new_rq = new RequestData();
		try{
			neigh_id = neigh.store(sol_rq, new_rq);//pm should be passed to this function.
		}catch(Exception e){e.printStackTrace();}
		return neigh_id;
	}

	/**
	 * check the new incoming org if its new add it and return the ID. if its already exist in
	   the db do not add it just return the ID
	 * @param ORG
	 * @return
	 * @throws P2PDDSQLException
	 * @throws SignatureVerificationException
	 */
	//private static long handle_org(D_Organization ORG) throws P2PDDSQLException, SignatureVerificationException {
	private static long handle_org(PreparedMessage pm, D_Organization ORG) throws P2PDDSQLException, SignatureVerificationException {
		if(ORG == null) return -1;
		ArrayList<ArrayList<Object>> orgs = null;
		String sql = "select "+table.organization.organization_ID+
				" from "+table.organization.TNAME+" WHERE "+table.organization.global_organization_ID+
				"=?;";
		orgs = Application.db.select(sql, new String[]{ORG.global_organization_ID});

		if(orgs.isEmpty()) 
		{
			if(DEBUG)System.out.println("Its a new Organization : "+ORG.creator);
			return insert_org(ORG,true);
		}
		else {
			if(DEBUG)System.out.println("we already have this Organization");
			if(DEBUG)System.out.print(" it's ID is : "+orgs.get(0).get(0)+"\n");
			return Integer.parseInt(Util.getString(orgs.get(0).get(0)));
		}		
	}

	/**
	 * insert the new org in the db and return it's id
	 * @param ORG
	 * @param needs_verification
	 * @return
	 * @throws P2PDDSQLException
	 * @throws SignatureVerificationException
	 */
	public static long insert_org(D_Organization ORG, boolean needs_verification) throws P2PDDSQLException, SignatureVerificationException {	
		Calendar _arrival_date = Util.CalendargetInstance();
		String arrival_date = Encoder.getGeneralizedTime(_arrival_date);
		String global_organization_ID = ORG.global_organization_ID;
		String gIDhash = Util.getGIDhash(global_organization_ID);
		String name = ORG.name;
		long peer_id = get_peer(ORG.creator);
		String creator_ID = ""+peer_id;
		String Signature = Util.stringSignatureFromByte(ORG.signature);
		String creation_date = ORG._last_sync_date;
		int cert_methods = ORG.params.certifMethods;
		String s_cert_methods = ""+cert_methods;
		String s_certificate = Util.stringSignatureFromByte(ORG.params.certificate);
		String hash_org_alg = ORG.params.hash_org_alg;
		String category = ORG.params.category;
		String s_default_scoring_options = D_OrgConcepts.stringFromStringArray(ORG.params.default_scoring_options);//, table.organization.ORG_SCORE_SEP, null);
		String instructions_new_motions = ORG.params.instructions_new_motions;
		String instructions_registration = ORG.params.instructions_registration;
		String description = ORG.params.description;
		String s_languages = D_OrgConcepts.stringFromStringArray(ORG.params.languages);//, table.organization.ORG_LANG_SEP,null);
		String S_name_forum = D_OrgConcepts.stringFromStringArray(ORG.concepts.name_forum);
		String s_name_justification = D_OrgConcepts.stringFromStringArray(ORG.concepts.name_justification);
		String s_name_motion = D_OrgConcepts.stringFromStringArray(ORG.concepts.name_motion);
		String s_name_organization = D_OrgConcepts.stringFromStringArray(ORG.concepts.name_organization);
		if(DEBUG) System.out.println("ReceivedBroadcastable:insert_org: org="+ORG);

		if(needs_verification) {
			if(!ORG.verifySign(ORG.signature))  
				throw new SignatureVerificationException("Invalid Signature");
		}
		ORG.broadcasted = true;
		long org_id = ORG.storeVerified();
		
		/*
		long org_id = Application.db.insert(table.organization.TNAME,
				new String[]{table.organization.global_organization_ID,
				table.organization.name,//table.organization.description,
				table.organization.creation_date,table.organization.creator_ID,
				table.organization.global_organization_ID_hash,
				table.organization.certification_methods,table.organization.category,
				table.organization.certificate,table.organization.signature,
				table.organization.default_scoring_options,
				table.organization.instructions_new_motions,
				table.organization.instructions_registration,
				table.organization.description,
				table.organization.languages,
				table.organization.name_forum,table.organization.name_justification,
				table.organization.name_motion,table.organization.name_organization,
				table.organization.hash_org_alg,
				//table.organization.hash_org,
				table.organization.arrival_date,
				table.organization.broadcasted},
				new String[]{global_organization_ID,name,creation_date,creator_ID,gIDhash,
				s_cert_methods,category,s_certificate,Signature,s_default_scoring_options,
				instructions_new_motions,instructions_registration,description,s_languages,S_name_forum,
				s_name_justification,s_name_motion,s_name_organization,hash_org_alg,//s_hash_org,
				arrival_date,"1"
		}, DEBUG);
		*/
		if(DEBUG)System.out.println("New Org ID is : "+org_id);
		return org_id;
	}

	/**
	 * check the incoming peer who create the org if it's new or already exist
	 * @param creator
	 * @return
	 * @throws P2PDDSQLException
	 * @throws SignatureVerificationException
	 */
	private static long get_peer(D_PeerAddress creator) throws P2PDDSQLException, SignatureVerificationException {
		ArrayList<ArrayList<Object>> peers = null;
		String sql = "select "+table.peer.peer_ID+
				" from "+table.peer.TNAME+" WHERE "+table.peer.global_peer_ID+
				"=?;";
		peers = Application.db.select(sql, new String[]{creator.component_basic_data.globalID});
		if(peers.isEmpty()) 
		{
			if(DEBUG)System.out.println("Its a new Peer");
			return add_peer(creator, true);
		}
		else {
			if(DEBUG)System.out.println("We already have this Peer");
			if(DEBUG)System.out.println("Old peer ID is : "+peers.get(0).get(0));
			return Integer.parseInt(peers.get(0).get(0).toString());
		}
	}

	// add the new peer and return the id
	public static long add_peer(D_PeerAddress creator, boolean needs_verification) throws P2PDDSQLException, SignatureVerificationException {
//		String global_peer_id = creator.globalID;
//		String global_peer_id_hash = Util.getGIDhash(creator.globalID);;
//		hds.TypedAddress[] peer_address = creator.address;
//		String creation_date = Encoder.getGeneralizedTime(creator.creation_date);
//		String slogan = creator.slogan;
//		String name = creator.name;
		String signature = Util.stringSignatureFromByte(creator.component_basic_data.signature);
		if(DEBUG)System.out.println("SIGNATURE : "+signature);
//		String broadcastable = creator.broadcastable?"1":"0";
//		String hash_alg = Util.concat(creator.signature_alg, DD.APP_ID_HASH_SEP);
		creator.component_local_agent_state.arrival_date=Util.CalendargetInstance();
		String crt_date = Encoder.getGeneralizedTime(creator.component_local_agent_state.arrival_date);
		if(needs_verification) {
			D_PeerAddress pa = new D_PeerAddress(creator, false);
			if(!pa.verifySignature())
				if(!DD.ACCEPT_UNSIGNED_PEERS_FROM_TABLES) throw new SignatureVerificationException("Invalid Signature");
		}
		long peer_id =  D_PeerAddress._storeReceived(creator, creator.component_local_agent_state.arrival_date, crt_date); //._storeVerified();
		/*
		Calendar adding_date = Util.CalendargetInstance();
		String s_adding_date = Encoder.getGeneralizedTime(adding_date);

		long peer_id =  Application.db.insert(table.peer.TNAME,
				new String[]{table.peer.global_peer_ID,table.peer.global_peer_ID_hash,
				table.peer.arrival_date, table.peer.name, table.peer.slogan, table.peer.signature, 
				table.peer.creation_date, table.peer.broadcastable, table.peer.hash_alg},
				new String[]{global_peer_id,global_peer_id_hash, s_adding_date,
				name, slogan, signature, creation_date, broadcastable, hash_alg});
		for(int i=0;i<peer_address.length;i++){
			String address = peer_address[i].address;
			String type = peer_address[i].type;
			Application.db.insert(table.peer_address.TNAME,
					new String[]{table.peer_address.peer_ID,
					table.peer_address.type,table.peer_address.address,table.peer_address.arrival_date},
					new String[]{peer_id+"",type, address,creation_date}, DEBUG);

		}
		D_PeerAddress.integratePeerOrgs(creator.served_orgs, peer_id, s_adding_date);
		UpdatePeersTable.integratePeerAddresses(peer_id, peer_address, adding_date);
		*/
		if(DEBUG)System.out.println("New Peer ID is : "+peer_id);
		return peer_id;
	}
	public static void playThanks(){
		if(Application.CURRENT_SCRIPTS_BASE_DIR() == null) return;
		crt = Util.CalendargetInstance().getTimeInMillis();
		if((crt-last)<10000) return;
		last =  Util.CalendargetInstance().getTimeInMillis();
		
		String wav_file = Application.CURRENT_SCRIPTS_BASE_DIR()+Application.WIRELESS_THANKS;
		int EXTERNAL_BUFFER_SIZE = 524288;
		File soundFile = new File(wav_file);
		AudioInputStream audioInputStream = null;
		try
		{audioInputStream = AudioSystem.getAudioInputStream(soundFile);}
		catch(Exception e) {e.printStackTrace();}

		AudioFormat format = audioInputStream.getFormat();
		SourceDataLine auline = null;
		//Describe a desired line
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		try
		{
			auline = (SourceDataLine) AudioSystem.getLine(info);
			//Opens the line with the specified format,
			//causing the line to acquire any required
			//system resources and become operational.
			auline.open(format);
		}
		catch(Exception e) {e.printStackTrace();}

		//Allows a line to engage in data I/O
		auline.start();
		int nBytesRead = 0;
		byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
		try {
			while (nBytesRead != -1)
			{
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
				if (nBytesRead >= 0)
				{
					//Writes audio data to the mixer via this source data line
					//NOTE : A mixer is an audio device with one or more lines
					auline.write(abData, 0, nBytesRead);
				}
			}
		}
		catch(Exception e) {e.printStackTrace();}
		finally
		{
			//Drains queued data from the line
			//by continuing data I/O until the
			//data line's internal buffer has been emptied
			auline.drain();
			//Closes the line, indicating that any system
			//resources in use by the line can be released
			auline.close();
		}		
	}

	/**
	 * Check if it should be discarded:
	 *  - you are receiving from the same machine
	 *  - signature fails (CHECK_MESSAGE_SIGNATURES)
	 * @param msg
	 * @return
	 * @throws P2PDDSQLException
	 * @throws IOException 
	 */
	private static boolean check_global_peerID(D_Message msg) throws P2PDDSQLException, IOException {
		String my_GPID = DD.getMyPeerGIDFromIdentity();
		String hash_GID = Util.getGIDhash(msg.sender.component_basic_data.globalID);
		my_GPIDhash = Util.getGIDhash(my_GPID);
		
		if(hash_GID.compareTo(my_GPIDhash)==0)  {
			if(DEBUG)System.out.println("RCV from myself");
			return true;
			}
		
		if(CHECK_MESSAGE_SIGNATURES ){
			// check signature is messages is signed and config requires it.
		}
		my_bag_of_peers.remove(hash_GID);
		my_bag_of_peers.add(hash_GID);
		if(my_bag_of_peers.size()>MY_PEER_BAG_MAX_SIZE) my_bag_of_peers.remove(0);
		
		//play music
		if((msg.recent_senders!=null)&&(msg.recent_senders.contains(my_GPIDhash))){
			playThanks();
		}
		return false;
	}
}
