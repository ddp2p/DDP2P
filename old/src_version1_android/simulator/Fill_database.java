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

package simulator;

import handling_wb.Prepare_messages;
import hds.Address;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.UUID;

import ASN1.ASN1DecoderFail;
import ASN1.Encoder;
import util.P2PDDSQLException;
import config.Application;
import config.Application_GUI;
import config.DD;
import util.Util;
import ciphersuits.Cipher;
import ciphersuits.SK;
import data.D_Document;
import data.D_Document_Title;
import data.D_Justification;
import data.D_Motion;
import data.D_OrgConcepts;
import data.D_Organization;
import data.D_OrgParams;
import data.D_Peer;
import data.D_Constituent;
import data.D_Neighborhood;
import data.D_Vote;
import data.D_Witness;

public class Fill_database<org_id_for> extends util.DDP2P_ServiceThread  {

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
		Application.db.delete("delete from "+table.application.TNAME+" where "+table.application.field+"=\"WLAN_INTERESTS\";",new String[]{});
		Application.db.delete("delete from "+table.organization.TNAME,new String[]{});
		Application.db.delete("delete from "+table.peer.TNAME,new String[]{});
		Application.db.delete("delete from "+table.peer_address.TNAME,new String[]{});
		Application.db.delete("delete from "+table.constituent.TNAME,new String[]{});
		Application.db.delete("delete from "+table.motion.TNAME,new String[]{});
		Application.db.delete("delete from "+table.signature.TNAME,new String[]{});
		Application.db.delete("delete from "+table.justification.TNAME,new String[]{});
		Application.db.delete("delete from "+table.witness.TNAME,new String[]{});
		Application.db.delete("delete from "+table.neighborhood.TNAME,new String[]{});
		Application.db.delete("delete from "+table.key.TNAME,new String[]{});
		Application.db.delete("delete from "+table.application.TNAME+" WHERE "+
							table.application.field+"=?"+" or "+table.application.field+"=?"+
							" or "+table.application.field+"=?"+" or "+table.application.field+"=?;",
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
		vote.organization_ID = ""+organization_ID;
		vote.global_organization_ID = D_Organization.getGlobalOrgID(vote.organization_ID);
		vote.constituent_ID = ""+constituent_ID;
		vote.global_constituent_ID =  D_Constituent.getConstituentGlobalID(vote.constituent_ID);
		vote.constituent = new D_Constituent(constituent_ID);
		if(DEBUG)System.out.println("CONS ID : "+vote.constituent_ID);
		if(DEBUG)System.out.println("CONS data : "+vote.constituent);
		vote.motion_ID = ""+motion_ID;
		vote.global_motion_ID = D_Motion.getMotionGlobalID(vote.motion_ID);
		vote.motion = new D_Motion(motion_ID);
		vote.creation_date = creation_date;
		vote.arrival_date = creation_date;
		vote.choice = Util.random_Y_N();
		vote.format = "format";
		if(DEBUG)System.out.println("Fill_database : add_vote() : after filling -without jus-");
		int justification_choice = Util.pick_randomly(new float[]{SimulationParameters.adding_new_justification_in_vote,
				SimulationParameters.no_justification_vote,SimulationParameters.using_old_justification_in_vote});
		switch(justification_choice){

		case 0: //add a new vote and add a new justification with it
		{ 
			if(DEBUG)System.out.println("Fill_database : vote(): VOTE AND JUSTIFICATION");
			long just_id =-1;
			String sql = "SELECT count(*) FROM "+table.justification.TNAME+" WHERE "+table.justification.motion_ID+"=?;";
			ArrayList<ArrayList<Object>> count = Application.db.select(sql, new String[]{""+motion_ID});
			long justifications = Integer.parseInt(count.get(0).get(0).toString());
			if(justifications == 0){
				just_id = add_justification(motion_ID,constituent_ID,organization_ID,false);
				if(DEBUG)System.out.print(" \"just with no answerID\" \n");
			}
			else{
				int r = Util.get_one_or_zero();
				just_id = add_justification(motion_ID, constituent_ID,organization_ID, r>0);
			}

			vote.justification_ID = ""+just_id;
			vote.global_justification_ID = getJustificationGlobalID(vote.justification_ID);
			vote.justification = new D_Justification(just_id);
			vote.global_vote_ID = vote.make_ID();
			vote.signature = vote.sign(sk);
			added_vote = vote.storeVerified();
			if(DEBUG)System.out.println("Fill_database : vote() : END CASE 0 : "+vote.global_vote_ID);
			if(DEBUG)System.out.println("Fill_database : add_vote_with_justification : END vote_id="+added_vote);
			return added_vote;
		} 

		case 1: //add vote and do not add justification with it
		{ 
			if(DEBUG)System.out.println("Fill_database : vote(): adding VOTE ONLY");
			vote.global_vote_ID = vote.make_ID();
			if(DEBUG)System.out.println("Fill_database : vote(): adding VOTE ONLY before sign");
			vote.signature = vote.sign(sk);
			if(DEBUG)System.out.println("Fill_database : vote(): adding VOTE ONLY after sign befor storing");
			added_vote = vote.storeVerified();
			if(DEBUG)System.out.println("Fill_database : vote(): adding VOTE ONLY after storing");
			if(DEBUG)System.out.println("Fill_database : vote() : END CASE 1 : "+vote.global_vote_ID);
			if(DEBUG)System.out.println("Fill_database : add_vote_only : END vote_id="+added_vote);
			return added_vote;
		}

		case 2: // refer existing justification
		{ 
			if(DEBUG)System.out.println("Fill_database : vote(): adding VOTE AND EXISTING JUSTIFICATION");
			long just_id =-1;
			just_id = select_random_justification(motion_ID,constituent_ID,organization_ID);
			vote.justification_ID = ""+just_id;
			vote.global_justification_ID = getJustificationGlobalID(vote.justification_ID);
			vote.justification = new D_Justification(just_id);
			vote.global_vote_ID = vote.make_ID();
			vote.signature = vote.sign(sk);
			added_vote = vote.storeVerified();
			if(DEBUG)System.out.println("Fill_database : vote() : END CASE 2 : "+vote.global_vote_ID);
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
		String sql = "SELECT count(*) FROM "+table.justification.TNAME+" WHERE "+table.justification.motion_ID+"=?;";
		ArrayList<ArrayList<Object>> count = Application.db.select(sql, new String[]{""+motion_id});
		long justifications = Integer.parseInt(count.get(0).get(0).toString());
		if(justifications == 0) return add_justification(motion_id, constituent_id, org_id, false);
		long selected = (long)Util.random(justifications);
		String sql_sel = "SELECT "+table.justification.justification_ID+" FROM "+
				table.justification.TNAME+" LIMIT 1 OFFSET "+selected;
		ArrayList<ArrayList<Object>> j_data = Application.db.select(sql_sel, new String[]{});
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
		D_Justification jus = new D_Justification();
		jus.hash_alg = "0";
		jus.creation_date = creation_date;
		jus.arrival_date = creation_date;
		jus.constituent_ID = ""+constituent_id;
		jus.global_constituent_ID =  D_Constituent.getConstituentGlobalID(jus.constituent_ID);
		jus.constituent = new D_Constituent(constituent_id);
		jus.motion_ID = ""+motion_id;
		jus.global_motionID = D_Motion.getMotionGlobalID(jus.motion_ID);
		jus.motion = new D_Motion(motion_id);
		jus.organization_ID = ""+org_id;
		jus.global_organization_ID = D_Organization.getGlobalOrgID(jus.organization_ID);
		D_Document doc1 = new D_Document();
		doc1.setDocumentString(justification_text);
		doc1.setFormatString(justification_format);
		D_Document doc2 = new D_Document();
		doc2.setDocumentString(justification_title);
		doc2.setFormatString(justification_title);
		D_Document_Title doc3 = new D_Document_Title();
		doc3.title_document = doc2;
		jus.justification_text = doc1;
		jus.justification_title = doc3;
		if(DEBUG)System.out.println("add_justification : org_id"+jus.organization_ID);
		if(add_answerTo){ 
			 ans_id = select_random_justification(motion_id,constituent_id,org_id);
			 jus.answerTo_ID = ""+ans_id;
			 jus.answerTo = new D_Justification(ans_id);
			 jus.global_answerTo_ID = jus.answerTo.global_justificationID;  //getJustificationGlobalID(jus.answerTo_ID);
		}
		else {
			jus.answerTo_ID = null;
			jus.answerTo = null;
			jus.global_answerTo_ID = null;
		}
		jus.global_justificationID = jus.make_ID();
		jus.signature = jus.sign(sk);
		j_id = jus.storeVerified();
		if(DEBUG)System.out.println("JUS sig : "+jus.signature);
		if(jus.verifySignature()==false){
			if(DEBUG)System.out.println("WRONG JUSTIFICATION SIG");
		}
		}catch(Exception e){e.printStackTrace();}
		if(DEBUG)System.out.println("Fill_database : add_justification : JUST ADDED jus_id="+j_id);
		return j_id;
	}
	
	public static String getJustificationGlobalID(String justification_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("Fill_database:getJustificationGlobalID: start");
		if(justification_ID==null) return null;
		String sql = "SELECT "+table.justification.global_justification_ID+
		" FROM "+table.justification.TNAME+
		" WHERE "+table.justification.justification_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.db.select(sql, new String[]{justification_ID}, DEBUG);
		if(n.size()==0) return null;
		return Util.getString(n.get(0).get(0));
	}

	private static long select_random_motion(long organization_ID,
			long constituent_ID) throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG)System.out.println("calling select_random_motion...before 1 sel: "+Util.getGeneralizedTime());
		long motion_ID = -1;
		String sql =
			"SELECT count(DISTINCT m."+table.motion.motion_ID+") " +
			" FROM "+table.motion.TNAME+" AS m "+
			" WHERE m."+table.motion.organization_ID+"=?" +
			" AND m."+table.motion.motion_ID+" NOT IN (" +
					" SELECT nm."+table.motion.motion_ID+
					" FROM "+table.motion.TNAME+" AS nm"+
					" LEFT JOIN "+table.signature.TNAME+" AS s ON(s."+table.signature.motion_ID+"=nm."+table.motion.motion_ID+") "+
					" WHERE nm."+table.motion.organization_ID+"=?" +
					" AND s."+table.signature.constituent_ID+"=?);";
		ArrayList<ArrayList<Object>> count =
				Application.db.select(sql, new String[]{""+organization_ID,""+organization_ID,""+constituent_ID});
		if(DEBUG)System.out.println("Fill_database : select_random_motion : after 1 sel :"+Util.getGeneralizedTime());
		if(DEBUG)System.out.println("Fill_database : select_random_motion : Org_ID="+organization_ID+" Cons_ID="+constituent_ID);
		long motions = Integer.parseInt(count.get(0).get(0).toString());
		if(motions == 0) return add_motion(organization_ID);
		long selected = (long)Util.random(motions);
		if(DEBUG)System.out.println("Fill_database : select_random_motion : before 2 sel :"+Util.getGeneralizedTime());
		String sql_sel =
			"SELECT DISTINCT m."+table.motion.motion_ID +
			" FROM "+table.motion.TNAME+" AS m "+
			" WHERE m."+table.motion.organization_ID+"=?" +
			" AND m."+table.motion.motion_ID+" NOT IN (" +
					" SELECT nm."+table.motion.motion_ID+
					" FROM "+table.motion.TNAME+" AS nm"+
					" LEFT JOIN "+table.signature.TNAME+" AS s ON(s."+table.signature.motion_ID+"=nm."+table.motion.motion_ID+") "+
					" WHERE nm."+table.motion.organization_ID+"=?" +
					" AND s."+table.signature.constituent_ID+"=?)"
				+" LIMIT 1 OFFSET "+selected;
		
		
		ArrayList<ArrayList<Object>> m_data = Application.db.select(sql_sel, new String[]{""+organization_ID,""+organization_ID,""+constituent_ID});
		if(DEBUG)System.out.println("Fill_database : select_random_motion : after 2 sel :"+Util.getGeneralizedTime());
		motion_ID = Integer.parseInt(m_data.get(0).get(0).toString());
		if(DEBUG)System.out.println("select_random_motion(): motion selected id="+motion_ID);
		return motion_ID;
	}


	// we can use it when we want to select random motion using only org_id without constituent
	private static long select_random_motion(long organization_ID) throws P2PDDSQLException, ASN1DecoderFail {
		long motion_ID = -1;
		String sql = "SELECT count(*) FROM "+table.motion.TNAME+" WHERE "+table.motion.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> count = Application.db.select(sql, new String[]{""+organization_ID});
		long motions = Integer.parseInt(count.get(0).get(0).toString());
		if(motions == 0) return add_motion(organization_ID);
		long selected = (long)Util.random(motions);
		String sql_sel = "SELECT "+table.motion.motion_ID+
				" FROM "+table.motion.TNAME+" WHERE "+
				table.motion.organization_ID+"=? LIMIT 1 OFFSET "+selected;
		ArrayList<ArrayList<Object>> m_data = Application.db.select(sql_sel, new String[]{""+organization_ID});
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
		D_Motion mot = new D_Motion();
		mot.global_organization_ID = D_Organization.getGlobalOrgID(organization_ID);
		mot.organization_ID = organization_ID;
		if(DEBUG)System.out.println("add_motion : before org"+mot.organization_ID);
		ArrayList<Object> org_data = Prepare_messages.get_org_db_by_local(mot.organization_ID);
		if(DEBUG)System.out.println("add_motion : orgData"+org_data);
		mot.organization = Prepare_messages.get_ASN_Organization_by_OrgID(org_data,mot.organization_ID);
		if(DEBUG)System.out.println("add_motion : orgData : "+mot.organization);
		mot.creation_date = creation_date;
		mot.arrival_date = creation_date;
		mot.preferences_date = creation_date;
		mot.constituent_ID = ""+Cons_ID;
		if(DEBUG)System.out.println("add_motion : cons id"+mot.constituent_ID);
		mot.global_constituent_ID = D_Constituent.getConstituentGlobalID(mot.constituent_ID);
		if(DEBUG)System.out.println("add_motion : global cons id"+mot.global_constituent_ID);
		mot.constituent = new D_Constituent(Cons_ID);
		if(DEBUG)System.out.println("add_motion : before motion text : ");
		D_Document doc1 = new D_Document();
		doc1.setDocumentString(motion_text);
		doc1.setFormatString(motion_format);
		D_Document doc2 = new D_Document();
		doc2.setDocumentString(motion_title);
		doc2.setFormatString(motion_title);
		D_Document_Title doc3 = new D_Document_Title();
		doc3.title_document = doc2;
		mot.motion_text = doc1;
		if(DEBUG)System.out.println("add_motion : mot.text : "+mot.motion_text);
		mot.motion_title = doc3;
		if(DEBUG)System.out.println("add_motion : mot.title : "+mot.motion_title);
		mot.status = 0;
		mot.global_motionID = mot.make_ID();
		if(DEBUG)System.out.println("add_motion : before signing");
		mot.signature = mot.sign(sk);
		if(DEBUG)System.out.println("add_motion : after signing : "+mot);
		try{
		motion_ID = mot.storeVerified();
		}catch(Exception e){e.printStackTrace();}
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
		String organizationGID = D_Organization.getGlobalOrgID(organization_ID);
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
		wbw.witnessed_global_constituentID = D_Constituent.getConstituentGlobalID(Util.getStringID(Con_witnessed_ID));
		wbw.witnessing_constituentID = Con_witnessing_ID;
		wbw.witnessing_global_constituentID = D_Constituent.getConstituentGlobalID(Util.getStringID(Con_witnessing_ID));
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
		String sql = "SELECT count(*) FROM "+table.constituent.TNAME+" WHERE "+table.constituent.organization_ID+"=? AND "+table.constituent.external+"=?;";
		ArrayList<ArrayList<Object>> count = Application.db.select(sql, new String[]{""+organization_ID,external});
		long c = Integer.parseInt(count.get(0).get(0).toString());
		//COUNT = c;
		if(DEBUG)System.out.println("count : "+c);
		if(c == 0){ //COUNT = add_constituent(organization_ID);
			//return COUNT;
			return add_constituent(organization_ID);
		}
		
		long selected = (long)Util.random(c);
		String sql_sel = "SELECT "+table.constituent.constituent_ID+" FROM "+table.constituent.TNAME+" WHERE "+table.constituent.organization_ID+"=? AND "+table.constituent.external+"=? LIMIT 1 OFFSET "+selected;
		ArrayList<ArrayList<Object>> p_data = Application.db.select(sql_sel, new String[]{""+organization_ID, external});
		p_ID = Integer.parseInt(p_data.get(0).get(0).toString());
		return p_ID;
	}
	
	private static long select_random_constituent_except(long organization_ID,long avoid) throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG)System.out.println("Fill_database : select_random_constituent");
		long p_ID;
		ArrayList<ArrayList<Object>> p_data = null;
		String sql = "SELECT count(*) FROM "+table.constituent.TNAME+
				" JOIN "+table.key.TNAME+" ON "+table.constituent.global_constituent_ID+"="+table.key.public_key
				+" WHERE "+table.constituent.organization_ID+"=?  AND "+table.constituent.constituent_ID+"<>?";
		ArrayList<ArrayList<Object>> count = Application.db.select(sql, new String[]{Util.getStringID(organization_ID),""+avoid});
		long c = Integer.parseInt(count.get(0).get(0).toString());
		if(DEBUG)System.out.println("Fill_database : select_random_constituent : selected cons="+c);
		if(c == 0) return add_constituent(organization_ID);
		long selected = (long)Util.random(c);
		
		String sql_sel = "SELECT "+table.constituent.constituent_ID+" FROM "+table.constituent.TNAME+
				" JOIN "+table.key.TNAME+" ON "+table.constituent.global_constituent_ID+"="+table.key.public_key+
				" WHERE "+table.constituent.organization_ID+"=? AND "+table.constituent.constituent_ID+"<>?"+
				"LIMIT 1 OFFSET "+selected;
		try{
		p_data = Application.db.select(sql_sel, new String[]{""+organization_ID,""+avoid});
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
		String sql = "SELECT count(*) FROM "+table.constituent.TNAME+
				" JOIN "+table.key.TNAME+" ON "+table.constituent.global_constituent_ID+"="+table.key.public_key
				+" WHERE "+table.constituent.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> count = Application.db.select(sql, new String[]{Util.getStringID(organization_ID)});
		long c = Integer.parseInt(count.get(0).get(0).toString());
		if(DEBUG)System.out.println("Fill_database : select_random_constituent : selected cons="+c);
		if(c == 0) return add_constituent(organization_ID);
		long selected = (long)Util.random(c);
		
		String sql_sel = "SELECT "+table.constituent.constituent_ID+" FROM "+table.constituent.TNAME+
				" JOIN "+table.key.TNAME+" ON "+table.constituent.global_constituent_ID+"="+table.key.public_key+
				" WHERE "+table.constituent.organization_ID+"=? "+
				"LIMIT 1 OFFSET "+selected;
		try{
		p_data = Application.db.select(sql_sel, new String[]{""+organization_ID});
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
	
		D_Constituent Cons = new D_Constituent();
		Cons.global_constituent_id = gcID;
		Cons.global_constituent_id_hash = gcIDhash;
		Cons.surname = surname;
		Cons.forename = forename;
		Cons.slogan = slogan;
		Cons.address = null;
		Cons.certificate = null;
		Cons.picture = null;
		Cons.external = false;
		Cons.email = email;
		Cons.organization_ID = organization_ID;
		Cons.global_organization_ID = D_Organization.getGlobalOrgID(Cons.organization_ID);
		Cons.picture = byteArray;
		Cons.creation_date = creation_date;
		Cons.hash_alg = table.constituent.CURRENT_HASH_CONSTITUENT_ALG;
		Cons.languages = new String[0];
		Cons.neighborhood = null;
		String orgGID = D_Organization.getGlobalOrgID(organization_ID2+"");
		//System.out.println("add_constituent(): before storeVerified()");
		constituent_ID = Cons.storeVerified(orgGID, ""+organization_ID2, now);
		//System.out.println("add_constituent(): after storeVerified()");
		//Cons.neighborhood = new WB_Neighborhood[6];
		Cons.neighborhood = select_neighborhood_or_create_by_cID(organization_ID2, constituent_ID, sk);
		if(Cons.neighborhood.length>0)
			Cons.global_neighborhood_ID = Cons.neighborhood[0].global_neighborhood_ID;
		Cons.creation_date = Util.CalendargetInstance();
		Cons.signature = Cons.sign(Cons.global_organization_ID, Cons.global_constituent_id);
		Cons.storeVerified(orgGID, ""+organization_ID2, now);
		if(DEBUG)System.out.println("Fill_database : add_constituent() : CONS ADDED cons_id="+constituent_ID);
		return constituent_ID;
	}

   private static D_Neighborhood[] select_neighborhood_or_create_by_cID(
			long organization_ID2, long cID, SK sk) throws P2PDDSQLException {
	   D_Neighborhood[] Nbre = new D_Neighborhood[6];
	   String sql = "SELECT count(*) FROM "+table.neighborhood.TNAME+" where "+table.neighborhood.organization_ID+"=?;";
	   ArrayList<ArrayList<Object>> count = Application.db.select(sql, new String[]{Util.getString(organization_ID2)});
	   long nbrs = Integer.parseInt(Util.getString(count.get(0).get(0)));
	   String pID = null;
	   if(DEBUG)System.out.println("Neigh Count : "+nbrs);
	   if(nbrs == 0) {
		   for(int j=0; j<6; j++) {
			   Nbre[j] = add_neighborhood(organization_ID2,cID,sk,pID);
			   pID = Nbre[j].neighborhoodID;
		   }
		   return Nbre;
	   }
	  
	   String sql1 = "SELECT n."+table.neighborhood.neighborhood_ID+
				" FROM "+table.neighborhood.TNAME+" AS n "+
				" LEFT JOIN "+table.neighborhood.TNAME+" AS d ON(d."+table.neighborhood.parent_nID+"=n."+table.neighborhood.neighborhood_ID+") "+
				" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=n."+table.neighborhood.organization_ID+") "+
				" WHERE o."+table.organization.organization_ID+"=? AND d."+table.neighborhood.neighborhood_ID+" IS NULL;"
				;
	   if(DEBUG)System.out.println("select_neighborhood_or_create_by_cID : sql1:"+sql1);
	   if(DEBUG)System.out.println("select_neighborhood_or_create_by_cID : Org_id:"+organization_ID2);
		ArrayList<ArrayList<Object>> a = Application.db.select(sql1, new String[]{Util.getStringID(organization_ID2)}, DEBUG);
		if(DEBUG)System.out.println("a:"+a);
		long n_ID = Integer.parseInt(a.get(0).get(0).toString());
		 if(DEBUG)System.out.println("select_neighborhood_or_create_by_cID : n_ID:"+n_ID);
		String sql2 = "SELECT "+table.neighborhood.global_neighborhood_ID+
				" FROM "+table.neighborhood.TNAME+" WHERE "+
				table.neighborhood.neighborhood_ID+"=?;";
		ArrayList<ArrayList<Object>> GID = Application.db.select(sql2, new String[]{Util.getStringID(n_ID)}, DEBUG);
		String global_n_id = Util.getString(GID.get(0).get(0));
	   if(DEBUG)System.out.println("NID : "+n_ID);
	   if(DEBUG)System.out.println("GNID : "+global_n_id);
	   Nbre = D_Neighborhood.getNeighborhoodHierarchy(global_n_id,""+n_ID,D_Constituent.EXPAND_ALL);
		
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
	   D_Neighborhood wbn = new D_Neighborhood();
	   wbn.boundary = null;
	   wbn.creation_date = creation_date;
	   wbn.description = null;
	   wbn.name = name;
	   wbn.name_division = name_division;
	   wbn.name_lang = name_lang;
	   wbn.names_subdivisions = _subdivisions;
	   wbn.parent = null;
	   wbn.parent_ID = pID;
	   wbn.parent_global_ID = D_Neighborhood.getNeighborhoodGlobalID(pID);
	   wbn.picture = null;
	   wbn.submitter = null;
	   wbn.submitter_ID = ""+cID;
	   wbn.submitter_global_ID = D_Constituent.getConstituentGlobalID(Util.getStringID(cID));
	   wbn.organization_ID = organization_ID;
	   wbn.global_organization_ID = D_Organization.getGlobalOrgID(Util.getStringID(organization_ID));
	   wbn.global_neighborhood_ID = wbn.make_ID(wbn.global_organization_ID);
	  wbn.signature = wbn.sign(sk, wbn.global_organization_ID);
	  wbn.neighborhoodID =  wbn.storeVerified(Util.getStringID(cID), wbn.global_organization_ID, ""+organization_ID, now, null, null);
	  if(DEBUG)System.out.println("Fill_database : add_neighborhood() : NEIGH ADDED Neig_id="+ wbn.neighborhoodID);
	   return wbn;
   }
	
	private static long select_random_organization() throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG)System.out.println("calling select_random_organization");
		long organization_ID = -1;
		String sql = "SELECT count(*) FROM "+table.organization.TNAME+
				" WHERE "+table.organization.broadcasted+"=1";
		ArrayList<ArrayList<Object>> count = Application.db.select(sql, new String[]{});
		long orgs = Integer.parseInt(count.get(0).get(0).toString());
		if(orgs == 0) return add_organization();
		long selected = (long)Util.random(orgs);
		String sql_sel = "SELECT "+table.organization.organization_ID+
				" FROM "+table.organization.TNAME+" LIMIT 1 OFFSET "+selected;
		ArrayList<ArrayList<Object>> o_data = Application.db.select(sql_sel, new String[]{});
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
		String s_hash_org = Util.byteToHex(hash_org, table.organization.ORG_HASH_BYTE_SEP);
		String sql = "SELECT "+table.peer.global_peer_ID+" from "+
				table.peer.TNAME+" where "+table.peer.peer_ID+"=?;";
		ArrayList<ArrayList<Object>> p_data = Application.db.select(sql, new String[]{""+p_ID});
		String creator_global_ID = Util.getString(p_data.get(0).get(0));
		
		D_Organization od = new D_Organization();
		od.global_organization_ID = gID;
		od.name = name;
		od.setDate(_creation_date);
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
		try{
		if(!od.verifySign(od.signature)){
			if(_DEBUG)System.err.println("Fail to verify signature just created for:"+ od);
		}}
		catch (Exception e) {
			e.printStackTrace();
		}
		String Signature = 	Util.stringSignatureFromByte(od.signature); 
		long org_id = -1;
		try {
			org_id = handling_wb.ReceivedBroadcastableMessages.insert_org(od,false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(DEBUG)System.out.println("Fill_database : add_organization() : Org is added org_id"+org_id);
		return org_id;
	}
	
private static D_Peer get_peer_by_ID(long p_id) throws P2PDDSQLException {
		
		D_Peer pa = D_Peer.getPeerByLID(p_id, true);
		return pa;
	}

	
	private static long select_random_peer() throws P2PDDSQLException, ASN1DecoderFail {
		long peer_ID=-1;
		String sql = "SELECT count(*) FROM "+table.peer.TNAME;
		ArrayList<ArrayList<Object>> count = Application.db.select(sql, new String[]{});
		
		long peers = Integer.parseInt(count.get(0).get(0).toString());
		if(peers == 0) return add_peer();
		long selected = (long)Util.random(peers);
		String sql_sel = "SELECT "+table.peer.peer_ID+
				" FROM "+table.peer.TNAME+" LIMIT 1 OFFSET "+selected;
		ArrayList<ArrayList<Object>> p_data = Application.db.select(sql_sel, new String[]{});
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
		
		D_Peer pa = new D_Peer();
		pa.component_basic_data.broadcastable = true;
		pa.component_basic_data.creation_date = _creation_date;
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
			peer_ID = handling_wb.ReceivedBroadcastableMessages.add_peer(pa, false);
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
