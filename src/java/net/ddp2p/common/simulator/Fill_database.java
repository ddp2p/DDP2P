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

package net.ddp2p.common.simulator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.UUID;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Document;
import net.ddp2p.common.data.D_Document_Title;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_OrgConcepts;
import net.ddp2p.common.data.D_OrgParams;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.handling_wb.Prepare_messages;
import net.ddp2p.common.hds.Address;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

public class Fill_database<org_id_for> extends net.ddp2p.common.util.DDP2P_ServiceThread  {

	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	private static final int MAX_ADDED = 1;
	private static final boolean ADDED_BOUNDED = false;
	public static long COUNT = 0;
	static final int SELECT_ORGS = 0;
	static final int SELECT_CONS = 1;
	static final int SELECT_NEIG = 2;
	static final int SELECT_PEER = 3;
	static final int SELECT_MOT = 4;
	static final int SELECT_VOTE = 5;
	static final int SELECT_WITN = 6;
	


	public Fill_database() {
		super("Simulator Fill Database", false);
	//Application.db=new DBInterface("deliberation-app.db");
	}
	int i=-1;
	int randomInt,randomInt1,randomInt2,randomInt3,randomInt4,randomInt5;
	public static String org_id_for_motion = null;
	Random randomGenerator = new Random();
	private boolean running = true;

	public static void cleanDatabase() throws P2PDDSQLException {
		Application.getDB().delete("delete from "+net.ddp2p.common.table.application.TNAME+" where "+net.ddp2p.common.table.application.field+"=\"WLAN_INTERESTS\";",new String[]{});
		Application.getDB().delete("delete from "+net.ddp2p.common.table.organization.TNAME,new String[]{});
		Application.getDB().delete("delete from "+net.ddp2p.common.table.peer.TNAME,new String[]{});
		Application.getDB().delete("delete from "+net.ddp2p.common.table.peer_address.TNAME,new String[]{});
		Application.getDB().delete("delete from "+net.ddp2p.common.table.constituent.TNAME,new String[]{});
		Application.getDB().delete("delete from "+net.ddp2p.common.table.motion.TNAME,new String[]{});
		Application.getDB().delete("delete from "+net.ddp2p.common.table.signature.TNAME,new String[]{});
		Application.getDB().delete("delete from "+net.ddp2p.common.table.justification.TNAME,new String[]{});
		Application.getDB().delete("delete from "+net.ddp2p.common.table.witness.TNAME,new String[]{});
		Application.getDB().delete("delete from "+net.ddp2p.common.table.neighborhood.TNAME,new String[]{});
		Application.getDB().delete("delete from "+net.ddp2p.common.table.key.TNAME,new String[]{});
		Application.getDB().delete("delete from "+net.ddp2p.common.table.application.TNAME+" WHERE "+
							net.ddp2p.common.table.application.field+"=?"+" or "+net.ddp2p.common.table.application.field+"=?"+
							" or "+net.ddp2p.common.table.application.field+"=?"+" or "+net.ddp2p.common.table.application.field+"=?;",
							new String[]{"my_global_peer_ID","INTERFACES","my_peer_name","my_global_peer_ID_hash"});

	}
	public void _run() {
		//boolean DEBUG = false;
		int counter = 0, count =0;
		try{
			if(_DEBUG)System.out.println("Simulator runing");
			for(i=0;i<1;i++)
			{
				Application_GUI.ThreadsAccounting_ping("Cycle "+i);
				if(ADDED_BOUNDED&&(counter==MAX_ADDED)) running = false;
				counter++;
				if(!running) break;
				//add_constituent();
				add_witness();
				//add_motion();
				//add_organization();
				//add_vote();
				if(_DEBUG) System.out.println("everything=	"+i);
				//add_peer();
				/*
				if(DEBUG)System.out.println("6 NEIGHS TO BE ADDED :-");
				String pID = null;
				long org_id = select_random_organization();
				if(DEBUG)System.out.println("ORG_ID : "+org_id);
				long c_id = select_random_constituent(org_id);
				if(DEBUG)System.out.println("CON_ID : "+c_id);
				SK sk = DD.getConstituentSK(c_id);
				if(DEBUG)System.out.println("sk : "+sk);
				D_Neighborhood[] Nbre = new D_Neighborhood[6];
				for(int j=0; j<6; j++) {
					Nbre[j] = add_neighborhood(org_id,c_id,sk,pID);
					pID = Nbre[j].neighborhoodID;
				}
				
				*/
				/*
				if(DEBUG)System.out.println("Fill_database : START picking a probability for simulation");
				int choice = Util.pick_randomly(
						new float[]{SimulationParameters.adding_new_organization,
								SimulationParameters.adding_new_constituent,
								SimulationParameters.adding_new_neighbor,
								SimulationParameters.adding_new_peer,
								SimulationParameters.adding_new_motion,
								SimulationParameters.adding_new_vote,
								SimulationParameters.adding_new_witness});
				if(DEBUG)System.out.println("Fill_database : END picking a probability for simulation");
				switch(choice){
				case SELECT_ORGS: 
					if(DEBUG)System.out.println("ORG TO BE ADDED :-");
					add_organization();
					if(DEBUG)System.out.println("---------------------------------------------------------------\n");
					break;
				case SELECT_CONS:
					if(DEBUG)System.out.println("CONS TO BE ADDED :-");
					 add_constituent();
					if(DEBUG)System.out.println("---------------------------------------------------------------\n");
					break;
				case SELECT_NEIG:
					if(DEBUG)System.out.println("6 NEIGHS TO BE ADDED :-");
					String pID = null;
					long org_id = select_random_organization();
					long c_id = select_random_constituent(org_id);
					SK sk = DD.getConstituentSK(c_id);
					D_Neighborhood[] Nbre = new D_Neighborhood[6];
					for(int j=0; j<6; j++) {
						Nbre[j] = add_neighborhood(org_id,c_id,sk,pID);
						pID = Nbre[j].neighborhoodID;
					}
					if(DEBUG)System.out.println("---------------------------------------------------------------\n");
					break;
				case SELECT_PEER:	
					if(DEBUG)System.out.println("PEER TO BE ADDED :-");
					add_peer(); 
					if(DEBUG)System.out.println("---------------------------------------------------------------\n");
					break;
				case SELECT_MOT:
					if(DEBUG)System.out.println("MOTION TO BE ADDED :-");
					add_motion();
					if(DEBUG)System.out.println("---------------------------------------------------------------\n");
					break;
				case SELECT_VOTE:
					if(DEBUG)System.out.println("VOTE TO BE ADDED :-");
					add_vote();
					if(_DEBUG) System.out.println("everything=	"+i+"	VOTES=	"+(++count));
					if(DEBUG)System.out.println("---------------------------------------------------------------\n");
					break;
				case SELECT_WITN:
					if(DEBUG)System.out.println("WITNESS TO BE ADDED :-");
					add_witness();
					if(DEBUG)System.out.println("---------------------------------------------------------------\n");
					break;
				default:
					break;
				}
				*/
				Thread.sleep(5);
			}
		}catch(Exception e){}
	}


	private static long add_vote() throws P2PDDSQLException, ASN1DecoderFail {
		
		if(DEBUG)System.out.println("Fill_database : add_vote() : START ");
		long added_vote = -1;
		long organization_ID = select_random_organization();
		long constituent_ID = select_random_constituent(organization_ID);
		long motion_ID = select_random_motion(organization_ID, constituent_ID);
		if(DEBUG)System.out.println("Fill_database : add_vote() : after selecting");
		Calendar creation_date = Util.CalendargetInstance();
		SK sk = DD.getConstituentSK(constituent_ID);
		if(DEBUG)System.out.println("Fill_database : add_vote() : after creating sk");
		D_Vote vote = new D_Vote();
		vote.setOrganizationLID(""+organization_ID);
		vote.setOrganizationGID(D_Organization.getGIDbyLIDstr(vote.getOrganizationLIDstr()));
		vote.setConstituentLID(""+constituent_ID);
		vote.setConstituentGID(D_Constituent.getGIDFromLID(vote.getConstituentLIDstr()));
		vote.setConstituentObjOnly(D_Constituent.getConstByLID(constituent_ID, true, false));
		if(DEBUG)System.out.println("CONS ID : "+vote.getConstituentLIDstr());
		if(DEBUG)System.out.println("CONS data : "+vote.getConstituent_force());
		vote.setMotionLID(""+motion_ID);
		vote.setMotionObjOnly(D_Motion.getMotiByLID(motion_ID, true, false));
		vote.setMotionGID(vote.getMotionFromObjOrLID().getGID());
		vote.setCreationDate(creation_date);
		vote.setArrivalDate(creation_date);
		vote.setChoice(Util.random_Y_N());
		vote.format = "format";
		if(DEBUG)System.out.println("Fill_database : add_vote() : after filling -without jus-");
		int justification_choice = Util.pick_randomly(new float[]{SimulationParameters.adding_new_justification_in_vote,
				SimulationParameters.no_justification_vote,SimulationParameters.using_old_justification_in_vote});
		switch(justification_choice){

		case 0: //add a new vote and add a new justification with it
		{ 
			if(DEBUG)System.out.println("Fill_database : vote(): VOTE AND JUSTIFICATION");
			long just_id =-1;
			
			long justifications = D_Justification.getCountForMotion(motion_ID); //Integer.parseInt(count.get(0).get(0).toString());
			if(justifications == 0){
				just_id = add_justification(motion_ID,constituent_ID,organization_ID,false);
				if(DEBUG)System.out.print(" \"just with no answerID\" \n");
			}
			else{
				int r = Util.get_one_or_zero();
				just_id = add_justification(motion_ID, constituent_ID,organization_ID, r>0);
			}

			vote.setJustificationLID(""+just_id);
			vote.setJustificationGID(D_Justification.getGIDFromLID(vote.getJustificationLIDstr()));
			vote.setJustification(D_Justification.getJustByLID(just_id, true, false));
			vote.setGID();
			vote.setSignature(vote.sign(sk));
			added_vote = vote.storeVerified();
			if(DEBUG)System.out.println("Fill_database : vote() : END CASE 0 : "+vote.getGID());
			if(DEBUG)System.out.println("Fill_database : add_vote_with_justification : END vote_id="+added_vote);
			return added_vote;
		} 

		case 1: //add vote and do not add justification with it
		{ 
			if(DEBUG)System.out.println("Fill_database : vote(): adding VOTE ONLY");
			vote.setGID(vote.make_ID());
			if(DEBUG)System.out.println("Fill_database : vote(): adding VOTE ONLY before sign");
			vote.setSignature(vote.sign(sk));
			if(DEBUG)System.out.println("Fill_database : vote(): adding VOTE ONLY after sign befor storing");
			added_vote = vote.storeVerified();
			if(DEBUG)System.out.println("Fill_database : vote(): adding VOTE ONLY after storing");
			if(DEBUG)System.out.println("Fill_database : vote() : END CASE 1 : "+vote.getGID());
			if(DEBUG)System.out.println("Fill_database : add_vote_only : END vote_id="+added_vote);
			return added_vote;
		}

		case 2: // refer existing justification
		{ 
			if(DEBUG)System.out.println("Fill_database : vote(): adding VOTE AND EXISTING JUSTIFICATION");
			long just_id =-1;
			just_id = select_random_justification(motion_ID,constituent_ID,organization_ID);
			vote.setJustificationLID(""+just_id);
			vote.setJustificationGID(D_Justification.getGIDFromLID(vote.getJustificationLIDstr()));
			vote.setJustification(D_Justification.getJustByLID(just_id, true, false));
			vote.setGID(vote.make_ID());
			vote.setSignature(vote.sign(sk));
			added_vote = vote.storeVerified();
			if(DEBUG)System.out.println("Fill_database : vote() : END CASE 2 : "+vote.getGID());
			if(DEBUG)System.out.println("Fill_database : add_vote_with_refering_to_existing_just : END vote_id="+added_vote);
			return added_vote;
		} 
		default:
			break;
		}
		return added_vote;
	}

	private static long select_random_justification(long motion_id,long constituent_id,long org_id) throws P2PDDSQLException, ASN1DecoderFail {
		long J_ID=-1;
//		String sql = "SELECT count(*) FROM "+table.justification.TNAME+" WHERE "+table.justification.motion_ID+"=?;";
//		ArrayList<ArrayList<Object>> count = Application.db.select(sql, new String[]{""+motion_id});
		long justifications = D_Justification.getCountForMotion(motion_id); //Integer.parseInt(count.get(0).get(0).toString());
		if(justifications == 0) return add_justification(motion_id, constituent_id, org_id, false);
		long selected = (long)Util.random(justifications);
		String sql_sel = "SELECT "+net.ddp2p.common.table.justification.justification_ID+" FROM "+
				net.ddp2p.common.table.justification.TNAME+" LIMIT 1 OFFSET "+selected;
		ArrayList<ArrayList<Object>> j_data = Application.getDB().select(sql_sel, new String[]{});
		J_ID = Integer.parseInt(j_data.get(0).get(0).toString());
		return J_ID;
	}

	/**
	 * 
	 * @param motion_id
	 * @param constituent_id
	 * @param org_id
	 * @param add_answerTo
	 * @return
	 * @throws P2PDDSQLException
	 * @throws ASN1DecoderFail
	 */
	private static long add_justification(long motion_id,long constituent_id,long org_id,boolean add_answerTo) throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG)System.out.println("ADDING JUSTIFICATION");
		long j_id=-1;
		long ans_id=-1;
		Calendar creation_date = Util.CalendargetInstance();
		String uuid = UUID.randomUUID().toString().substring(31);
		//
		String randomStrings = new String();
	    Random random = new Random();
	        char[] word = new char[300]; // words of length 3 through 10. (1 and 2 letter words are boring.)
	        for(int j = 0; j < 300; j++)
	        {
	            word[j] = (char)('a' + random.nextInt(26));
	        }
	        randomStrings = new String(word);
		String justification_text = randomStrings ;
		String justification_title = "JT"+uuid;
		String justification_format = "JF"+uuid;
		SK sk = DD.getConstituentSK(constituent_id);
		try{
			D_Justification jus = D_Justification.getEmpty();
			jus.setHashAlg("0");
			jus.setCreationDate(creation_date);
			jus.setArrivalDate(creation_date);
			jus.setConstituentLIDstr_Dirty(""+constituent_id);
			jus.setConstituentGID(D_Constituent.getGIDFromLID(jus.getConstituentLIDstr()));
			jus.setConstituentObj(D_Constituent.getConstByLID(constituent_id, true, false));
			jus.setMotionLIDstr(""+motion_id);
			jus.setMotionGID(jus.getMotion().getGID());
			jus.setMotionObj(D_Motion.getMotiByLID(motion_id, true, false));
			jus.setOrganizationLIDstr(""+org_id);
			jus.setOrgGID(D_Organization.getGIDbyLIDstr(jus.getOrganizationLIDstr()));
			D_Document doc1 = new D_Document();
			doc1.setDocumentString(justification_text);
			doc1.setFormatString(justification_format);
			D_Document doc2 = new D_Document();
			doc2.setDocumentString(justification_title);
			doc2.setFormatString(justification_title);
			D_Document_Title doc3 = new D_Document_Title();
			doc3.title_document = doc2;
			jus.setJustificationBody(doc1);
			jus.setJustificationTitle(doc3);
			if(DEBUG)System.out.println("add_justification : org_id"+jus.getOrganizationLIDstr());
			if(add_answerTo){ 
				 ans_id = select_random_justification(motion_id,constituent_id,org_id);
				 jus.setAnswerToLIDstr(""+ans_id);
				 jus.setAnswerTo_SetAll(D_Justification.getJustByLID(ans_id, true, false));
				 jus.setAnswerTo_GID(jus.getAnswerTo().getGID());  //getJustificationGlobalID(jus.answerTo_ID);
			}
			else {
				jus.setAnswerToLIDstr(null);
				jus.setAnswerTo_SetAll(null);
				jus.setAnswerTo_GID(null);
			}
			jus.setGID();
			jus.setSignature(jus.sign(sk));
			
			D_Justification j = D_Justification.getJustByGID(jus.getGID(), true, true, jus.getOrganizationLID(), jus.getMotionLID());
			j.loadRemote(jus, null, null, null);
			j_id = j.storeRequest_getID();
			j.releaseReference();
			
	//		j_id = jus.storeVerified();
	//		if(DEBUG)System.out.println("JUS sig : "+jus.getSignature());
			if(j.verifySignature()==false){
				if(DEBUG)System.out.println("WRONG JUSTIFICATION SIG");
			}
		}catch(Exception e){e.printStackTrace();}
		if(DEBUG)System.out.println("Fill_database : add_justification : JUST ADDED jus_id="+j_id);
		return j_id;
	}
	
//	public static String getJustificationGlobalID(String justification_ID) throws P2PDDSQLException {
//		if(DEBUG) System.out.println("Fill_database:getJustificationGlobalID: start");
//		if(justification_ID==null) return null;
//		String sql = "SELECT "+table.justification.global_justification_ID+
//		" FROM "+table.justification.TNAME+
//		" WHERE "+table.justification.justification_ID+"=?;";
//		ArrayList<ArrayList<Object>> n = Application.db.select(sql, new String[]{justification_ID}, DEBUG);
//		if(n.size()==0) return null;
//		return Util.getString(n.get(0).get(0));
//	}

	private static long select_random_motion(long organization_ID,
			long constituent_ID) throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG)System.out.println("calling select_random_motion...before 1 sel: "+Util.getGeneralizedTime());
		long motion_ID = -1;
		String sql =
			"SELECT count(DISTINCT m."+net.ddp2p.common.table.motion.motion_ID+") " +
			" FROM "+net.ddp2p.common.table.motion.TNAME+" AS m "+
			" WHERE m."+net.ddp2p.common.table.motion.organization_ID+"=?" +
			" AND m."+net.ddp2p.common.table.motion.motion_ID+" NOT IN (" +
					" SELECT nm."+net.ddp2p.common.table.motion.motion_ID+
					" FROM "+net.ddp2p.common.table.motion.TNAME+" AS nm"+
					" LEFT JOIN "+net.ddp2p.common.table.signature.TNAME+" AS s ON(s."+net.ddp2p.common.table.signature.motion_ID+"=nm."+net.ddp2p.common.table.motion.motion_ID+") "+
					" WHERE nm."+net.ddp2p.common.table.motion.organization_ID+"=?" +
					" AND s."+net.ddp2p.common.table.signature.constituent_ID+"=?);";
		ArrayList<ArrayList<Object>> count =
				Application.getDB().select(sql, new String[]{""+organization_ID,""+organization_ID,""+constituent_ID});
		if(DEBUG)System.out.println("Fill_database : select_random_motion : after 1 sel :"+Util.getGeneralizedTime());
		if(DEBUG)System.out.println("Fill_database : select_random_motion : Org_ID="+organization_ID+" Cons_ID="+constituent_ID);
		long motions = Integer.parseInt(count.get(0).get(0).toString());
		if(motions == 0) return add_motion(organization_ID);
		long selected = (long)Util.random(motions);
		if(DEBUG)System.out.println("Fill_database : select_random_motion : before 2 sel :"+Util.getGeneralizedTime());
		String sql_sel =
			"SELECT DISTINCT m."+net.ddp2p.common.table.motion.motion_ID +
			" FROM "+net.ddp2p.common.table.motion.TNAME+" AS m "+
			" WHERE m."+net.ddp2p.common.table.motion.organization_ID+"=?" +
			" AND m."+net.ddp2p.common.table.motion.motion_ID+" NOT IN (" +
					" SELECT nm."+net.ddp2p.common.table.motion.motion_ID+
					" FROM "+net.ddp2p.common.table.motion.TNAME+" AS nm"+
					" LEFT JOIN "+net.ddp2p.common.table.signature.TNAME+" AS s ON(s."+net.ddp2p.common.table.signature.motion_ID+"=nm."+net.ddp2p.common.table.motion.motion_ID+") "+
					" WHERE nm."+net.ddp2p.common.table.motion.organization_ID+"=?" +
					" AND s."+net.ddp2p.common.table.signature.constituent_ID+"=?)"
				+" LIMIT 1 OFFSET "+selected;
		
		
		ArrayList<ArrayList<Object>> m_data = Application.getDB().select(sql_sel, new String[]{""+organization_ID,""+organization_ID,""+constituent_ID});
		if(DEBUG)System.out.println("Fill_database : select_random_motion : after 2 sel :"+Util.getGeneralizedTime());
		motion_ID = Integer.parseInt(m_data.get(0).get(0).toString());
		if(DEBUG)System.out.println("select_random_motion(): motion selected id="+motion_ID);
		return motion_ID;
	}


	// we can use it when we want to select random motion using only org_id without constituent
	private static long select_random_motion(long organization_ID) throws P2PDDSQLException, ASN1DecoderFail {
		long motion_ID = -1;
		long motions = D_Motion.getCount(organization_ID);
		if(motions == 0) return add_motion(organization_ID);
		long selected = (long)Util.random(motions);
		String sql_sel = "SELECT "+net.ddp2p.common.table.motion.motion_ID+
				" FROM "+net.ddp2p.common.table.motion.TNAME+" WHERE "+
				net.ddp2p.common.table.motion.organization_ID+"=? LIMIT 1 OFFSET "+selected;
		ArrayList<ArrayList<Object>> m_data = Application.getDB().select(sql_sel, new String[]{""+organization_ID});
		motion_ID = Integer.parseInt(m_data.get(0).get(0).toString());
		return motion_ID;
	}

	private static long add_motion() throws P2PDDSQLException, ASN1DecoderFail {
		return add_motion(-1);
	}

	private static long add_motion(long org_ID) throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG)System.out.println("add_motion : start");
		long motion_ID = -1;
		String organization_ID = null;
		if(org_ID==-1) org_ID = select_random_organization();
		organization_ID = ""+org_ID;
		long Cons_ID = select_random_constituent(org_ID);
		if(DEBUG)System.out.println("Cons ID : "+Cons_ID);
		Calendar creation_date = Util.CalendargetInstance();
		String uuid = UUID.randomUUID().toString().substring(31);
		//
		String randomStrings = new String();
	    Random random = new Random();
	        char[] word = new char[1000];
	        for(int j = 0; j < 1000; j++)
	        {
	            word[j] = (char)('a' + random.nextInt(26));
	        }
	        randomStrings = new String(word);
	        //
		String motion_title = "MT"+uuid;
		String motion_text = randomStrings;
		String motion_format = "MF"+uuid;
		SK sk = DD.getConstituentSK(Cons_ID);
		D_Motion mot = D_Motion.getEmpty();
		mot.setOrganizationGID(D_Organization.getGIDbyLIDstr(organization_ID));
		mot.setOrganizationLIDstr(organization_ID);
		if(DEBUG)System.out.println("add_motion : before org"+mot.getOrganizationLIDstr());
		ArrayList<Object> org_data = Prepare_messages.get_org_db_by_local(mot.getOrganizationLIDstr());
		if(DEBUG)System.out.println("add_motion : orgData"+org_data);
		mot.setOrganization(Prepare_messages.get_ASN_Organization_by_OrgID(org_data,mot.getOrganizationLIDstr()));
		if(DEBUG)System.out.println("add_motion : orgData : "+mot.getOrganization());
		mot.setCreationDate(creation_date);
		mot.setArrivalDate(creation_date);
		mot.setPreferencesDate(creation_date);
		mot.setConstituentLIDstr(""+Cons_ID);
		if(DEBUG)System.out.println("add_motion : cons id"+mot.getConstituentLIDstr());
		mot.setConstituentGID(D_Constituent.getGIDFromLID(mot.getConstituentLIDstr()));
		if(DEBUG)System.out.println("add_motion : global cons id"+mot.getConstituentGID());
		mot.setConstituent(D_Constituent.getConstByLID(Cons_ID, true, false));
		if(DEBUG)System.out.println("add_motion : before motion text : ");
		D_Document doc1 = new D_Document();
		doc1.setDocumentString(motion_text);
		doc1.setFormatString(motion_format);
		D_Document doc2 = new D_Document();
		doc2.setDocumentString(motion_title);
		doc2.setFormatString(motion_title);
		D_Document_Title doc3 = new D_Document_Title();
		doc3.title_document = doc2;
		mot.setMotionText(doc1);
		if(DEBUG)System.out.println("add_motion : mot.text : "+mot.getMotionText());
		mot.setMotionTitle(doc3);
		if(DEBUG)System.out.println("add_motion : mot.title : "+mot.getMotionTitle());
		mot.setStatus(0);
		mot._setGID(mot.make_ID());
		if(DEBUG)System.out.println("add_motion : before signing");
		mot.setSignature(mot.sign(sk));
		if(DEBUG)System.out.println("add_motion : after signing : "+mot);
		D_Motion m = D_Motion.getMotiByGID(mot.getGID(), true, true, true, null, mot.getOrganizationLID(), null);
		m.loadRemote(mot, null, null, null);
		motion_ID = m.storeRequest_getID();
		m.releaseReference();
		//try {
		//motion_ID = mot.storeVerified();
		//}catch(Exception e){e.printStackTrace();}
		if(DEBUG)System.out.println("add_motion : end");
		if(DEBUG)System.out.println("Fill_database : add_motion : MOTION ADDED ID="+motion_ID);
		return motion_ID;
	}
	
	
	private static long add_witness() throws P2PDDSQLException, ASN1DecoderFail  {
		return add_witness(-1);
	}
	
	public static long add_witness(long org_id) throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG)System.out.println("Fill_database : add_witness Start");
		long witness_id = -1;
		D_Witness wbw = new D_Witness();
		if(DEBUG)System.out.println("Fill_database : add_witness : before select_random_org");
		if(org_id==-1)
			org_id = select_random_organization();
		if(DEBUG)System.out.println("Fill_database : add_witness : after select_random_org");
		String organization_ID = Util.getStringID(org_id);
		String organizationGID = D_Organization.getGIDbyLIDstr(organization_ID);
		long Con_witnessed_ID;
		long Con_witnessing_ID;
		if(DEBUG)System.out.println("Fill_database : add_witness : before select_random_Cons");
		Con_witnessing_ID = select_random_constituent(org_id,"0");
		if(DEBUG)System.out.println("Fill_database : add_witness : after select_random_Cons");
		SK sk = DD.getConstituentSK(Con_witnessing_ID);
		if(sk==null) return -1; //trying to generate witness using a receiving constituent
		
	//f(COUNT==1) {
		//ong c=add_constituent(org_id);
		//
		//do{
			Con_witnessed_ID=select_random_constituent_except(org_id,Con_witnessing_ID);
			if(DEBUG)System.out.println("Fill_database : add_witness : Con_witnessed_ID:"+Con_witnessed_ID+
					" Con_witnessing_ID:"+Con_witnessing_ID);
		//}while (Con_witnessing_ID==Con_witnessed_ID);
		if(DEBUG)System.out.println("Simulator:Fill_database:First (witnessed) con ID="+Con_witnessed_ID);
		if(DEBUG)System.out.println("Simulator:Fill_database:Second (witnessing) con ID="+Con_witnessing_ID);
		Calendar creation_date = Util.CalendargetInstance();
		
		
		wbw.global_organization_ID(organizationGID);
		wbw.witnessed_constituentID = Con_witnessed_ID;
		wbw.witnessed_global_constituentID = D_Constituent.getGIDFromLID(Con_witnessed_ID);
		wbw.witnessing_constituentID = Con_witnessing_ID;
		wbw.witnessing_global_constituentID = D_Constituent.getGIDFromLID(Con_witnessing_ID);
		wbw.sense_y_n = Util.get_one_or_zero();
		wbw.witness_eligibility_category = "your_explanation";
		wbw.creation_date = creation_date;
		wbw.arrival_date = creation_date;
		wbw.global_organization_ID = organizationGID;
		wbw.global_witness_ID = wbw.make_ID();
		wbw.sign(sk);
		witness_id = wbw.storeVerified();
		if(DEBUG)System.out.println("Fill_database : Witness is added wid : "+witness_id);
		return witness_id;
	}

	private static long select_random_constituent(long organization_ID, String external) throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG)System.out.println("Fill_database : select_random_constituent with external");
		long p_ID;
		String sql = "SELECT count(*) FROM "+net.ddp2p.common.table.constituent.TNAME+" WHERE "+net.ddp2p.common.table.constituent.organization_ID+"=? AND "+net.ddp2p.common.table.constituent.external+"=?;";
		ArrayList<ArrayList<Object>> count = Application.getDB().select(sql, new String[]{""+organization_ID,external});
		long c = Integer.parseInt(count.get(0).get(0).toString());
		//COUNT = c;
		if(DEBUG)System.out.println("count : "+c);
		if(c == 0){ //COUNT = add_constituent(organization_ID);
			//return COUNT;
			return add_constituent(organization_ID);
		}
		
		long selected = (long)Util.random(c);
		String sql_sel = "SELECT "+net.ddp2p.common.table.constituent.constituent_ID+" FROM "+net.ddp2p.common.table.constituent.TNAME+" WHERE "+net.ddp2p.common.table.constituent.organization_ID+"=? AND "+net.ddp2p.common.table.constituent.external+"=? LIMIT 1 OFFSET "+selected;
		ArrayList<ArrayList<Object>> p_data = Application.getDB().select(sql_sel, new String[]{""+organization_ID, external});
		p_ID = Integer.parseInt(p_data.get(0).get(0).toString());
		return p_ID;
	}
	
	private static long select_random_constituent_except(long organization_ID,long avoid) throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG)System.out.println("Fill_database : select_random_constituent");
		long p_ID;
		ArrayList<ArrayList<Object>> p_data = null;
		String sql = "SELECT count(*) FROM "+net.ddp2p.common.table.constituent.TNAME+
				" JOIN "+net.ddp2p.common.table.key.TNAME+" ON "+net.ddp2p.common.table.constituent.global_constituent_ID+"="+net.ddp2p.common.table.key.public_key
				+" WHERE "+net.ddp2p.common.table.constituent.organization_ID+"=?  AND "+net.ddp2p.common.table.constituent.constituent_ID+"<>?";
		ArrayList<ArrayList<Object>> count = Application.getDB().select(sql, new String[]{Util.getStringID(organization_ID),""+avoid});
		long c = Integer.parseInt(count.get(0).get(0).toString());
		if(DEBUG)System.out.println("Fill_database : select_random_constituent : selected cons="+c);
		if(c == 0) return add_constituent(organization_ID);
		long selected = (long)Util.random(c);
		
		String sql_sel = "SELECT "+net.ddp2p.common.table.constituent.constituent_ID+" FROM "+net.ddp2p.common.table.constituent.TNAME+
				" JOIN "+net.ddp2p.common.table.key.TNAME+" ON "+net.ddp2p.common.table.constituent.global_constituent_ID+"="+net.ddp2p.common.table.key.public_key+
				" WHERE "+net.ddp2p.common.table.constituent.organization_ID+"=? AND "+net.ddp2p.common.table.constituent.constituent_ID+"<>?"+
				"LIMIT 1 OFFSET "+selected;
		try{
		p_data = Application.getDB().select(sql_sel, new String[]{""+organization_ID,""+avoid});
		}
		catch(Exception e) {e.printStackTrace();}
		//if(p_data.size() == 0) return add_constituent(organization_ID);
		
		p_ID = Integer.parseInt(p_data.get(0).get(0).toString());
		if(DEBUG) System.out.println("select_random_constituent(): constituent selected is:"+p_ID);
		//if(p_ID == 0) return add_constituent(organization_ID);
		return p_ID;
	}

	private static long select_random_constituent(long organization_ID) throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG)System.out.println("Fill_database : select_random_constituent");
		long p_ID;
		ArrayList<ArrayList<Object>> p_data = null;
		String sql = "SELECT count(*) FROM "+net.ddp2p.common.table.constituent.TNAME+
				" JOIN "+net.ddp2p.common.table.key.TNAME+" ON "+net.ddp2p.common.table.constituent.global_constituent_ID+"="+net.ddp2p.common.table.key.public_key
				+" WHERE "+net.ddp2p.common.table.constituent.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> count = Application.getDB().select(sql, new String[]{Util.getStringID(organization_ID)});
		long c = Integer.parseInt(count.get(0).get(0).toString());
		if(DEBUG)System.out.println("Fill_database : select_random_constituent : selected cons="+c);
		if(c == 0) return add_constituent(organization_ID);
		long selected = (long)Util.random(c);
		
		String sql_sel = "SELECT "+net.ddp2p.common.table.constituent.constituent_ID+" FROM "+net.ddp2p.common.table.constituent.TNAME+
				" JOIN "+net.ddp2p.common.table.key.TNAME+" ON "+net.ddp2p.common.table.constituent.global_constituent_ID+"="+net.ddp2p.common.table.key.public_key+
				" WHERE "+net.ddp2p.common.table.constituent.organization_ID+"=? "+
				"LIMIT 1 OFFSET "+selected;
		try{
		p_data = Application.getDB().select(sql_sel, new String[]{""+organization_ID});
		}
		catch(Exception e) {e.printStackTrace();}
		//if(p_data.size() == 0) return add_constituent(organization_ID);
		
		p_ID = Integer.parseInt(p_data.get(0).get(0).toString());
		if(DEBUG) System.out.println("select_random_constituent(): constituent selected is:"+p_ID);
		//if(p_ID == 0) return add_constituent(organization_ID);
		return p_ID;
	}

	private static long add_constituent() throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG) System.out.println("simulator: add_constituent()");
		return add_constituent(-1);
	}

	public static long add_constituent(long organization_ID2) throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG)Util.printCallPath("Fill_database : calling add_constituent") ;
		if(DEBUG)System.out.println("add_constituent : org ID : "+organization_ID2);
		long constituent_ID = -1;
		String uuid = UUID.randomUUID().toString().substring(31);
		String surname = "Surname"+uuid;
		String forename = "Forename"+uuid;
		String slogan = "Slogan"+uuid;
		String email = "Email"+uuid; 
		Calendar creation_date = Util.CalendargetInstance();
    	String now = Encoder.getGeneralizedTime(creation_date);
		if(organization_ID2==-1) 
			organization_ID2 = select_random_organization();
		String organization_ID = ""+organization_ID2;
		byte[] byteArray=null;
		org_id_for_motion = null;
		org_id_for_motion = organization_ID;
		Cipher keys = Util.getKeyedGlobalID("GRASSROOT", ""+surname+now);
		keys.genKey(1024);
		DD.storeSK(keys, "CONSTITUENT");
		String gcID = Util.getKeyedIDPK(keys);
		String gcIDhash = D_Constituent.getGIDHashFromGID_NonExternalOnly(gcID);
		SK sk = keys.getSK();
	
		D_Constituent Cons = D_Constituent.getConstByGID_or_GIDH(gcID, gcIDhash, true, true, true, null, organization_ID2);
		Cons._set_GID(gcID);
		Cons.setGIDH(gcIDhash);
		Cons.setSurname(surname);
		Cons.setForename(forename);
		Cons.setSlogan(slogan);
		Cons.address = null;
		Cons.setCertificate(null);
		Cons.setPicture(null);
		Cons.setExternal(false);
		Cons.setEmail(email);
		//Cons.organization_ID = organization_ID;
		Cons.setOrganizationGID(D_Organization.getGIDbyLIDstr(Cons.getOrganizationLIDstr()));
		Cons.setPicture(byteArray);
		Cons.setCreationDate(creation_date);
		Cons.setHash_alg(net.ddp2p.common.table.constituent.CURRENT_HASH_CONSTITUENT_ALG);
		Cons.languages = new String[0];
		Cons.setNeighborhood(null);
		//String orgGID = D_Organization.getGlobalOrgID(organization_ID2+"");
		//System.out.println("add_constituent(): before storeVerified()");
		Cons.setArrivalDate(now);
		//constituent_ID = Cons.storeVerified(orgGID, ""+organization_ID2, now);
		//System.out.println("add_constituent(): after storeVerified()");
		//Cons.neighborhood = new WB_Neighborhood[6];
		Cons.setNeighborhood(select_neighborhood_or_create_by_cID(organization_ID2, constituent_ID, sk));
		if (Cons.getNeighborhood().length > 0)
			Cons.setNeighborhoodGID(Cons.getNeighborhood()[0].getGID());
		Cons.setCreationDate(Util.CalendargetInstance());
		Cons.sign();
		//Cons.signature = Cons.sign(Cons.global_organization_ID, Cons.global_constituent_id);
		constituent_ID = Cons.storeRequest_getID();
		Cons.releaseReference();
		//Cons.storeVerified(orgGID, ""+organization_ID2, now);
		if (DEBUG) System.out.println("Fill_database : add_constituent() : CONS ADDED cons_id="+constituent_ID);
		return constituent_ID;
	}

   private static D_Neighborhood[] select_neighborhood_or_create_by_cID(
			long organization_ID2, long cID, SK sk) throws P2PDDSQLException {
	   D_Neighborhood[] Nbre = new D_Neighborhood[6];
	   int nbrs = D_Neighborhood.getNeighCount(organization_ID2);
	   String pID = null;
	   if(DEBUG)System.out.println("Neigh Count : "+nbrs);
	   if(nbrs == 0) {
		   for(int j=0; j<6; j++) {
			   Nbre[j] = add_neighborhood(organization_ID2,cID,sk,pID);
			   pID = Nbre[j].getLIDstr();
		   }
		   return Nbre;
	   }
	  
	   // select one leaf (who have no descendants)
		long n_ID = D_Neighborhood.getLeaves(organization_ID2);
		 if(DEBUG)System.out.println("select_neighborhood_or_create_by_cID : n_ID:"+n_ID);
//		String sql2 = "SELECT "+table.neighborhood.global_neighborhood_ID+
//				" FROM "+table.neighborhood.TNAME+" WHERE "+
//				table.neighborhood.neighborhood_ID+"=?;";
//		ArrayList<ArrayList<Object>> GID = Application.db.select(sql2, new String[]{Util.getStringID(n_ID)}, DEBUG);
		D_Neighborhood dn = D_Neighborhood.getNeighByLID(n_ID, true, false);
		if (dn == null) return null;
		String global_n_id = dn.getGID(); //Util.getString(GID.get(0).get(0));
	   if(DEBUG)System.out.println("NID : "+n_ID);
	   if(DEBUG)System.out.println("GNID : "+global_n_id);
	   Nbre = D_Neighborhood.getNeighborhoodHierarchy(global_n_id, Util.getStringID(n_ID), D_Constituent.EXPAND_ALL, organization_ID2);
		
	   return Nbre;
	}

   private static D_Neighborhood add_neighborhood(long organization_ID, long cID, SK sk, String pID) throws P2PDDSQLException{
	
	   String uuid = UUID.randomUUID().toString().substring(31);
	   String name = "name "+uuid;
	   Calendar creation_date = Util.CalendargetInstance();
	   String now = Encoder.getGeneralizedTime(creation_date);
	   String name_division = "nd"+uuid;
	   String name_lang = "nl"+uuid;
	   String[] _subdivisions = null;//new String[0];
	   String n_key =  ""+cID+":"+name_division+"="+name;
	   //String gID=Util.getGlobalID("neighborhoods", n_key);
	   D_Neighborhood wbn = D_Neighborhood.getEmpty();
	   wbn.setBoundary(null);
	   wbn.setCreationDate(creation_date, now);
	   wbn.setDescription(null);
	   wbn.setName(name);
	   wbn.setName_division(name_division);
	   wbn.setName_lang(name_lang);
	   wbn.setNames_subdivisions(_subdivisions);
	   wbn.parent = null;
	   wbn.setParentLIDstr(pID);
	   wbn.setParent_GID(D_Neighborhood.getGIDFromLID(pID));
	   wbn.setPicture(null);
	   wbn.submitter = null;
	   wbn.setSubmitterLIDstr(""+cID);
	   wbn.setSubmitter_GID(D_Constituent.getGIDFromLID(cID));
	   //wbn.organization_ID = organization_ID;
	   wbn.setOrgIDs(D_Organization.getGIDbyLID(organization_ID), organization_ID);
	   wbn.setGID(wbn.make_ID());
	  wbn.setSignature(wbn.sign(sk));
	 // wbn.setLID(wbn.storeVerified(Util.getStringID(cID), wbn.getOrgGID(), ""+organization_ID, now, null, null));
	  
	  wbn.storeRemoteThis(wbn.getOrgGID(), organization_ID, now, null, null, null);
	  
	  if(DEBUG)System.out.println("Fill_database : add_neighborhood() : NEIGH ADDED Neig_id="+ wbn.getLID());
	   return wbn;
   }
	
	private static long select_random_organization() throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG)System.out.println("calling select_random_organization");
		long organization_ID = -1;
		String sql = "SELECT count(*) FROM "+net.ddp2p.common.table.organization.TNAME+
				" WHERE "+net.ddp2p.common.table.organization.broadcasted+"=1";
		ArrayList<ArrayList<Object>> count = Application.getDB().select(sql, new String[]{});
		long orgs = Integer.parseInt(count.get(0).get(0).toString());
		if(orgs == 0) return add_organization();
		long selected = (long)Util.random(orgs);
		String sql_sel = "SELECT "+net.ddp2p.common.table.organization.organization_ID+
				" FROM "+net.ddp2p.common.table.organization.TNAME+" LIMIT 1 OFFSET "+selected;
		ArrayList<ArrayList<Object>> o_data = Application.getDB().select(sql_sel, new String[]{});
		organization_ID = Integer.parseInt(o_data.get(0).get(0).toString());
		if(DEBUG)System.out.println("simulator: select_random_organization selected org id :"+organization_ID);
		return organization_ID;
	}

	public static long add_organization() throws P2PDDSQLException, ASN1DecoderFail {
		
		long p_ID = select_random_peer();
		String uuid = UUID.randomUUID().toString().substring(31);
		String name = "ON"+uuid;
		Calendar _creation_date = Util.CalendargetInstance();
		String creation_date = Encoder.getGeneralizedTime(_creation_date);
		Cipher keys = Util.getKeyedGlobalID("GRASSROOT", ""+name+creation_date);
		keys.genKey(1024);
		DD.storeSK(keys, "ORG");
		String gID = Util.getKeyedIDPK(keys);
		String s_id = Util.getKeyedIDSK(keys);
		int Cert_methods = 1;
		String category = "category"+uuid;
		byte[] certificate = new byte[0];
		String[] default_scoring_options = new String[0];
		String s_default_scoring_options = D_OrgConcepts.stringFromStringArray(default_scoring_options);//, table.organization.ORG_SCORE_SEP, null);
		String instructions_new_motions = "inst_new_motin"+uuid;
		String instructions_registration = "inst_reg"+uuid;
		String[] languages = new String[]{"en"};
		String s_languages = D_OrgConcepts.stringFromStringArray(languages);//, table.organization.ORG_LANG_SEP,null);
		String[] name_forum = new String[1];
		String S_name_forum = D_OrgConcepts.stringFromStringArray(name_forum);
		String[] name_justification = new String[1];
		String s_name_justification = D_OrgConcepts.stringFromStringArray(name_justification);
		String[] name_motion =  new String[1];
		String s_name_motion = D_OrgConcepts.stringFromStringArray(name_motion);
		String[] name_organization = new String[1];
		String s_name_organization = D_OrgConcepts.stringFromStringArray(name_organization);
		String hash_org_alg = "Horg_alg"+uuid;
		byte[] hash_org = null; //new byte[0];
		String s_hash_org = Util.byteToHex(hash_org, net.ddp2p.common.table.organization.ORG_HASH_BYTE_SEP);
		String sql = "SELECT "+net.ddp2p.common.table.peer.global_peer_ID+" from "+
				net.ddp2p.common.table.peer.TNAME+" where "+net.ddp2p.common.table.peer.peer_ID+"=?;";
		ArrayList<ArrayList<Object>> p_data = Application.getDB().select(sql, new String[]{""+p_ID});
		String creator_global_ID = Util.getString(p_data.get(0).get(0));
		
		D_Organization od = D_Organization.getEmpty(); //.getOrgByGID(gID, true, true);
		od.setGID(gID, null);
		od.setName(name);
		od.setLastSyncDate(_creation_date);
		od.creator = get_peer_by_ID(p_ID); 
		od.params = new D_OrgParams();
		od.params.certifMethods = Cert_methods;
		od.params.hash_org_alg = hash_org_alg;
		od.params.creation_time = _creation_date;
		od.params.creator_global_ID = creator_global_ID;
		od.params.category = category;
		od.params.certificate = certificate;
		od.params.default_scoring_options = default_scoring_options;
		od.params.instructions_new_motions =instructions_new_motions;
		od.params.instructions_registration =instructions_registration;
		od.params.languages = languages;
		od.concepts = new D_OrgConcepts();
		od.concepts.name_forum =name_forum ;
		od.concepts.name_justification = name_justification;
		od.concepts.name_motion = name_motion;
		od.concepts.name_organization = name_organization;
		od.broadcasted = true;
		od.signature = od.sign(Cipher.getSK(s_id), Util.getStoredSK(od.params.creator_global_ID));
		if(DEBUG) System.out.println("Fill_database:add_organization: org="+od);
		try {
		if (!od.verifySignature()) {
			if(_DEBUG)System.err.println("Fail to verify signature just created for:"+ od);
		}}
		catch (Exception e) {
			e.printStackTrace();
		}
		String Signature = 	Util.stringSignatureFromByte(od.signature); 
		long org_id = -1;
		try {
			org_id = net.ddp2p.common.handling_wb.ReceivedBroadcastableMessages.insert_org(od,false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(DEBUG)System.out.println("Fill_database : add_organization() : Org is added org_id"+org_id);
		return org_id;
	}
	
private static D_Peer get_peer_by_ID(long p_id) throws P2PDDSQLException {
		
		D_Peer pa = D_Peer.getPeerByLID_NoKeep(p_id, true);
		return pa;
	}

	
	private static long select_random_peer() throws P2PDDSQLException, ASN1DecoderFail {
		long peer_ID=-1;
		String sql = "SELECT count(*) FROM "+net.ddp2p.common.table.peer.TNAME;
		ArrayList<ArrayList<Object>> count = Application.getDB().select(sql, new String[]{});
		
		long peers = Integer.parseInt(count.get(0).get(0).toString());
		if(peers == 0) return add_peer();
		long selected = (long)Util.random(peers);
		String sql_sel = "SELECT "+net.ddp2p.common.table.peer.peer_ID+
				" FROM "+net.ddp2p.common.table.peer.TNAME+" LIMIT 1 OFFSET "+selected;
		ArrayList<ArrayList<Object>> p_data = Application.getDB().select(sql_sel, new String[]{});
		peer_ID = Integer.parseInt(p_data.get(0).get(0).toString());
		return peer_ID;
	}
	
	private static long add_peer() throws P2PDDSQLException {
		
		long peer_ID = -1;	
		String uuid = UUID.randomUUID().toString().substring(31);
		String name = "PN"+uuid;
		String slogan = "SLOGAN "+uuid;
		Calendar _creation_date = Util.CalendargetInstance();
		String creation_date = Encoder.getGeneralizedTime(_creation_date);
		Cipher keys = Util.getKeyedGlobalID("GRASSROOT", ""+name+creation_date);
		keys.genKey(1024);
		DD.storeSK(keys, "PEER");
		String gp_id = Util.getKeyedIDPK(keys); 
		
		D_Peer pa = D_Peer.getEmpty();
		pa.component_basic_data.broadcastable = true;
		pa.setCreationDateNoDirty(_creation_date, creation_date);
		pa.component_basic_data.globalID = gp_id;
		pa.component_basic_data.name = name;
		pa.component_basic_data.slogan = slogan;
		pa.setSignature(pa.sign(keys.getSK()));
		//pa.shared_addresses = new TypedAddress[4];
		for(int i=0;i<4;i++) {
			Address a = new Address();
			a.setAddress("163.118.78.4"+i+":25123");
			a.pure_protocol = "DIR";
			a.certified = true;
			a.priority = i;
			pa.shared_addresses.add(a);
			pa.dirty_addresses = true;
			pa.storeRequest();
		}
		try {
			peer_ID = net.ddp2p.common.handling_wb.ReceivedBroadcastableMessages.add_peer(pa, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(DEBUG)System.out.println("Fill_database : Peer is added peer_id"+peer_ID);
		return peer_ID;
	}

	public void stopSimulator() {
		if(_DEBUG)System.out.println("Simulator stop");
		running  = false;
		this.interrupt();
	}

	/*
	public static void main(String args[]) throws P2PDDSQLException{
		if(args[0].compareTo("dl")==0) {
			Fill_database s=new Fill_database();
			Fill_database.cleanDatabase();
			System.out.println("db deleted");
			//s.run();
		}
		else {
		Fill_database s=new Fill_database();
		s.run();
		}
	}
	 */
}
