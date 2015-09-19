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

package net.ddp2p.common.data;

import static java.lang.System.out;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.handling_wb.BroadcastQueueHandled;
import net.ddp2p.common.handling_wb.PreparedMessage;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.hds.ClientSync;
import net.ddp2p.common.streaming.ConstituentHandling;
import net.ddp2p.common.streaming.JustificationHandling;
import net.ddp2p.common.streaming.MotionHandling;
import net.ddp2p.common.streaming.OrgHandling;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.common.wireless.BroadcastClient;
/**
WB_VOTE ::= SEQUENCE {
    global_vote_ID PrintableString,
	choice UTF8String,
	voter WB_Constituent,
	date GeneralizedDate,
	justification WB_JUSTIFICATION,
	signature OCTET_STRING
}
 */
public
class D_Vote extends ASNObj{
	private static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final String V0 = "0";
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	// the value stored in choice for the vote counted as "yes"
	public static final String DEFAULT_YES_COUNTED_LABEL = "0";
	private String hash_alg = V0;
	private String global_vote_ID; //Printable
	private String global_constituent_ID; //Printable
	private String global_motion_ID; //Printable
	private String global_justification_ID; //Printable
	private String global_organization_ID; //Printable
	private String choice;//UTF8
	public String format;//UTF8
	private Calendar creation_date;
	private byte[] signature; //OCT STR
	private Calendar arrival_date;

	private D_Constituent constituent;
	private D_Motion motion;
	private D_Justification justification;
	
	private String vote_ID;
	private String constituent_ID;
	private String justification_ID;
	private String motion_ID;
	private String organization_ID;
	private boolean dirty_main;

	public static D_Vote getEmpty() {return new D_Vote();}
	public D_Vote() {}
	public final static String sql_get_vote_by_LID = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.signature.fields,"v")+
			//", c."+table.constituent.global_constituent_ID+
			//", m."+table.motion.organization_ID+
			//", m."+table.motion.global_motion_ID+
			//", j."+table.justification.global_justification_ID+
			//", o."+table.organization.global_organization_ID+
			//", o."+table.organization.organization_ID+
			" FROM "+net.ddp2p.common.table.signature.TNAME+" AS v "+
			//" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"+
			//" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"+
			//" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"+
			//" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE v."+net.ddp2p.common.table.signature.signature_ID+"=?;"
			;
	public D_Vote(long _vote_LID) throws P2PDDSQLException {
		if(_vote_LID<=0) return;
		setLID(Util.getStringID(_vote_LID));
		ArrayList<ArrayList<Object>> v = Application.getDB().select(sql_get_vote_by_LID, new String[]{getLIDstr()}, DEBUG);
		if(v.size() == 0) return;
		init(v.get(0));
	}
	public final static String sql_get_vote_by_GID = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.signature.fields,"v")+
			//", c."+table.constituent.global_constituent_ID+
			//", m."+table.motion.organization_ID+
			//", m."+table.motion.global_motion_ID+
			//", j."+table.justification.global_justification_ID+
			//", o."+table.organization.global_organization_ID+
			//", o."+table.organization.organization_ID+
			" FROM "+net.ddp2p.common.table.signature.TNAME+" AS v "+
			//" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"+
			//" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"+
			//" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"+
			//" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE v."+net.ddp2p.common.table.signature.global_signature_ID+"=?;"
			;
	/**
	 * Tries to get a saved vote.
	 * @param vote_GID
	 * @throws P2PDDSQLException
	 */
	public D_Vote(String vote_GID) throws P2PDDSQLException {
		if(vote_GID == null) return;
		this.setGID(vote_GID);
		ArrayList<ArrayList<Object>> v = Application.getDB().select(sql_get_vote_by_GID, new String[]{vote_GID}, DEBUG);
		if (v.size() == 0) return;
		init(v.get(0));
	}
	public final static String sql_vote_for_motionID_from_constituent = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.signature.fields,"v")+
			//", c."+table.constituent.global_constituent_ID+
			//", m."+table.motion.organization_ID+
			//", m."+table.motion.global_motion_ID+
			//", j."+table.justification.global_justification_ID+
			//", o."+table.organization.global_organization_ID+
			//", o."+table.organization.organization_ID+
			" FROM "+net.ddp2p.common.table.signature.TNAME+" AS v "+
			// " LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"+
			//" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"+
			//" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"+
			//" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
			" WHERE "
			+ " v."+net.ddp2p.common.table.signature.motion_ID+"=? "
			+ " AND v."+net.ddp2p.common.table.signature.constituent_ID+"=? "
			+ ";";
	/**
	 * 
	 * @param motionID
	 * @param _constituentID : must be non-null
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_Vote getOpinionForMotion(String motionID, long _constituentID) throws P2PDDSQLException {
		//getOneBroadcastedSupportForMotion(D_Motion.getMotiByLID(motionID, true, false), _constituentID, null);
		if (motionID == null) return null;
		if (_constituentID <= 0) return null;
		D_Vote v = new D_Vote();
		v.setMotionLID(motionID);
		ArrayList<ArrayList<Object>> _v =
				Application.getDB().select(
						sql_vote_for_motionID_from_constituent,
						new String[]{motionID, Util.getStringID(_constituentID)},
						DEBUG);
		if (_v.size() == 0) return null;
		v.init(_v.get(0));
		return v;
	}
	public final static String sql_vote_for_motionID_from_constituent_with_opinion = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.signature.fields,"v")+
			" FROM "+net.ddp2p.common.table.signature.TNAME+" AS v "+
			" WHERE "
			+ " v."+net.ddp2p.common.table.signature.motion_ID+"=? "
			+ " AND v."+net.ddp2p.common.table.signature.constituent_ID+"=? "
			+ " AND v."+net.ddp2p.common.table.signature.choice+"=? "
			+ ";";
	public final static String sql_one_vote_for_motionID = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.signature.fields,"v")+
			" FROM "+net.ddp2p.common.table.signature.TNAME+" AS v "+
			" WHERE "
			+ " v."+net.ddp2p.common.table.signature.motion_ID+"=? "
			+ " LIMIT 1;";
	public final static String sql_one_vote_with_opinion = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.signature.fields,"v")+
			" FROM "+net.ddp2p.common.table.signature.TNAME+" AS v "+
			" WHERE "
			+ " v."+net.ddp2p.common.table.signature.motion_ID+"=? "
			+ " AND v."+net.ddp2p.common.table.signature.choice+"=? "
			+ " LIMIT 1;";
	/**
	 * If  supportChoice is provided, then will only return a D_Vote with that value!
	 * 
	 * If the _constituent is null (of if he did not support), looks for any support with value "supportChoice"
	 * If supportChoice is null, looks for any from _constituent, or for any....
	 * If the _constituent is null, selected support must be broadcasted.
	 * @param _motion
	 * @param _constituent
	 * @param supportChoice
	 * @return
	 */
	public static D_Vote getOneBroadcastedSupportForMotion(D_Motion _motion,
			D_Constituent _constituent, String supportChoice) {
		
		ArrayList<ArrayList<Object>> _v;
		if (_motion == null)
			return null;
		D_Vote v = new D_Vote();
		v.setMotionLID(_motion.getLIDstr());
		try {
			if (_constituent == null && supportChoice == null) {
				_v = Application.getDB().select(
						sql_one_vote_for_motionID,
						new String[]{_motion.getLIDstr()},
						DEBUG);
				
			}  
			else if (_constituent != null && supportChoice == null) {
				_v = Application.getDB().select(
						sql_vote_for_motionID_from_constituent,
						new String[]{_motion.getLIDstr(), _constituent.getLIDstr()},
						DEBUG);
				if (_v.size() <= 0) {
					_v = Application.getDB().select(
							sql_one_vote_for_motionID,
							new String[]{_motion.getLIDstr()},
							DEBUG);
				}
			} 
			else if (_constituent == null && supportChoice != null) {
				_v = Application.getDB().select(
						sql_one_vote_with_opinion,
						new String[]{_motion.getLIDstr(), supportChoice},
						DEBUG);
						
			}  
			else if (_constituent != null && supportChoice != null)  {
				_v = Application.getDB().select(
						sql_vote_for_motionID_from_constituent_with_opinion,
						new String[]{_motion.getLIDstr(), supportChoice},
						DEBUG);
				if (_v.size() <= 0) {
					_v = Application.getDB().select(
							sql_one_vote_with_opinion,
							new String[]{_motion.getLIDstr(), supportChoice},
							DEBUG);
				}
			}
			else return null;
			
			if (_v.size() == 0) return null;
			
			v.init(_v.get(0));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return v;
	}
	public final static String sql_vote_for_justifID_from_constituent = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.signature.fields,"v")+
			" FROM "+net.ddp2p.common.table.signature.TNAME+" AS v "+
			" WHERE "
			+ " v."+net.ddp2p.common.table.signature.justification_ID+"=? "
			+ " AND v."+net.ddp2p.common.table.signature.constituent_ID+"=? "
			+ ";";
	public final static String sql_vote_for_justifID_from_constituent_with_opinion = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.signature.fields,"v")+
			" FROM "+net.ddp2p.common.table.signature.TNAME+" AS v "+
			" WHERE "
			+ " v."+net.ddp2p.common.table.signature.justification_ID+"=? "
			+ " AND v."+net.ddp2p.common.table.signature.constituent_ID+"=? "
			+ " AND v."+net.ddp2p.common.table.signature.choice+"=? "
			+ ";";
	public final static String sql_one_vote_for_justifID = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.signature.fields,"v")+
			" FROM "+net.ddp2p.common.table.signature.TNAME+" AS v "+
			" WHERE "
			+ " v."+net.ddp2p.common.table.signature.justification_ID+"=? "
			+ " LIMIT 1;";
	public final static String sql_one_vote_for_justif_with_opinion = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.signature.fields,"v")+
			" FROM "+net.ddp2p.common.table.signature.TNAME+" AS v "+
			" WHERE "
			+ " v."+net.ddp2p.common.table.signature.justification_ID+"=? "
			+ " AND v."+net.ddp2p.common.table.signature.choice+"=? "
			+ " LIMIT 1;";
	
	public static D_Vote getOneBroadcastedSupportForJustification(
			D_Motion crt_motion, D_Justification crt_justification,
			D_Constituent voting_constituent, String supportChoice) {	
		
		ArrayList<ArrayList<Object>> _v;
		if (crt_justification == null)
			return null;
		D_Vote v = new D_Vote();
		v.setMotionLID(crt_motion.getLIDstr());
		v.setJustificationLID(crt_justification.getLIDstr());
		try {
			if (voting_constituent == null && supportChoice == null) {
				_v = Application.getDB().select(
						sql_one_vote_for_justifID,
						new String[]{crt_justification.getLIDstr()},
						DEBUG);
				
			}  
			else if (voting_constituent != null && supportChoice == null) {
				_v = Application.getDB().select(
						sql_vote_for_justifID_from_constituent,
						new String[]{crt_justification.getLIDstr(), voting_constituent.getLIDstr()},
						DEBUG);
				if (_v.size() <= 0) {
					_v = Application.getDB().select(
							sql_one_vote_for_justifID,
							new String[]{crt_justification.getLIDstr()},
							DEBUG);
				}
			} 
			else if (voting_constituent == null && supportChoice != null) {
				_v = Application.getDB().select(
						sql_one_vote_for_justif_with_opinion,
						new String[]{crt_justification.getLIDstr(), supportChoice},
						DEBUG);
						
			}  
			else if (voting_constituent != null && supportChoice != null)  {
				_v = Application.getDB().select(
						sql_vote_for_justifID_from_constituent_with_opinion,
						new String[]{crt_justification.getLIDstr(), supportChoice},
						DEBUG);
				if (_v.size() <= 0) {
					_v = Application.getDB().select(
							sql_one_vote_for_justif_with_opinion,
							new String[]{crt_justification.getLIDstr(), supportChoice},
							DEBUG);
				}
			}
			else return null;
			
			if (_v.size() == 0) return null;
			
			v.init(_v.get(0));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return v;
	}

	/**
			"SELECT "+Util.setDatabaseAlias(table.signature.fields,"v")+
			" FROM "+table.signature.TNAME+" AS v "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"+
			" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
	 * 
	 * @param o
	 * @throws P2PDDSQLException
	 */
	public D_Vote(ArrayList<Object> o) throws P2PDDSQLException {
		init(o);
	}
	public D_Vote instance() throws CloneNotSupportedException{
		return new D_Vote();
	}
	/**
	 * Sets current date and arrival date to now.
	 * @param _motion (cannot be null. Must be saved having a LID and a GID.)
	 * @param _justification (may be null)
	 * @param _choice
	 * @return
	 */
	public static D_Vote createVote(D_Motion _motion, D_Justification _justification, String _choice) {
		return createVote( _motion, _justification, Identity.getCrtConstituent(_motion.getOrganizationLID()), _choice);
	}
	/**
	 * 
	 * @param _motion
	 * @param _justification
	 * @param _choice
	 * @return
	 */
	public static D_Vote createVote(D_Motion _motion, D_Justification _justification, D_Constituent _constituent, String _choice) {
		SK sk;
		if (_motion == null || !_motion.realized()) {
			if (_DEBUG) System.out.println("D_Vote: createVote: fail for incomplete motion: "+_motion);
			return null;
		}
		if (_constituent == null || !_constituent.realized() ) {
			if (_DEBUG) System.out.println("D_Vote: createVote: fail for incomplete constituent: "+_constituent);
			return null;
		}
		if ((sk = _constituent.getSK()) == null) {
			if (_DEBUG) System.out.println("D_Vote: createVote: fail for lacking keys from: "+_constituent);
			if (null == (sk = net.ddp2p.common.util.Util.getStoredSK(_constituent.getGID()))) {
				if (_DEBUG) System.out.println("D_Vote: createVote: fail for incomplete keys: "+_constituent);
				return null;
			}
			_constituent.setSK(sk);
		}
		if (_choice == null) {
			if (_DEBUG) System.out.println("D_Vote: createVote: fail for unspecified choice: "+_choice+" "+_constituent);
			return null;
		}
		Calendar now = Util.CalendargetInstance();
		D_Vote v = new D_Vote();
		v.setMotionAndOrganizationAll(_motion);
		v.setConstituentAll(_constituent);
		if (_justification != null) v.setJustificationAll(_justification);
		v.setChoice(_choice);
		v.setCreationDate(now);
		if (v.sign(sk) == null) {
			if (_DEBUG) System.out.println("D_Vote: createVote: fail to sign: "+_constituent);
			return null;
		}
		v.setArrivalDate(now);
		try {
			v.storeVerified();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return v;
	}
	/**
			"SELECT "+Util.setDatabaseAlias(table.signature.fields,"v")+
			" FROM "+table.signature.TNAME+" AS v "+
			" LEFT JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=v."+table.signature.constituent_ID+")"+
			" LEFT JOIN "+table.motion.TNAME+" AS m ON(m."+table.motion.motion_ID+"=v."+table.signature.motion_ID+")"+
			" LEFT JOIN "+table.justification.TNAME+" AS j ON(j."+table.justification.justification_ID+"=v."+table.signature.justification_ID+")"+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=m."+table.motion.organization_ID+")"+
	 * 
	 * @param o
	 * @throws P2PDDSQLException
	 */
	public void init(ArrayList<Object> o) throws P2PDDSQLException {
		this.setVersion(Util.getString(o.get(net.ddp2p.common.table.signature.S_HASH_ALG)));
		this.setGID(Util.getString(o.get(net.ddp2p.common.table.signature.S_GID)));
		this.setCreationDate(Util.getCalendar(Util.getString(o.get(net.ddp2p.common.table.signature.S_CREATION))));
		this.setArrivalDate(Util.getCalendar(Util.getString(o.get(net.ddp2p.common.table.signature.S_ARRIVAL))));
		this.setSignature(Util.byteSignatureFromString(Util.getString(o.get(net.ddp2p.common.table.signature.S_SIGNATURE))));
		this.setChoice(Util.getString(o.get(net.ddp2p.common.table.signature.S_CHOICE)));
		this.format = Util.getString(o.get(net.ddp2p.common.table.signature.S_FORMAT));
		this.setMotionLID(Util.getString(o.get(net.ddp2p.common.table.signature.S_MOTION_ID)));
		this.setConstituentLID(Util.getString(o.get(net.ddp2p.common.table.signature.S_CONSTITUENT_ID)));
		this.setJustificationLID(Util.getString(o.get(net.ddp2p.common.table.signature.S_JUSTIFICATION_ID)));
		this.setLID(Util.getString(o.get(net.ddp2p.common.table.signature.S_ID)));
		
		this.setMotionObjOnly(D_Motion.getMotiByLID(getMotionLIDstr(), true, false));
		this.setMotionGID(getMotionFromObjOrLID().getGID()); //Util.getString(o.get(table.signature.S_FIELDS+1));

		this.setConstituentObjOnly(D_Constituent.getConstByLID(getConstituentLIDstr(), true, false));
		this.setConstituentGID(getConstituent_force().getGID()); //D_Constituent.getGIDFromLID(constituent_ID);
		
		this.setJustificationAll(D_Justification.getJustByLID(getJustificationLIDstr(), true, false));
//		if (getJustification() != null)
//			this.setJustificationGID(getJustification().getGID()); //D_Justification.getGlobalID(justification_ID); //Util.getString(o.get(table.signature.S_FIELDS+2));

		this.setOrganizationLID(getMotionFromObjOrLID().getOrganizationLIDstr()); //Util.getString(o.get(table.signature.S_FIELDS+0));
		this.setOrganizationGID(getMotionFromObjOrLID().getOrganizationGID_force()); //D_Organization.getGIDbyLIDstr(organization_ID);//Util.getString(o.get(table.signature.S_FIELDS+3));

		//this.organization_ID = Util.getString(o.get(table.signature.S_FIELDS+4));
		//this.choices = WB_Choice.getChoices(motionID);
	}
	/**
	 * Gets constituent/motion/justif objects based on LIDs and justif GID).
	 * Used in loadVotes from the adhoc broadcast queues
	 * @param v
	 * @throws Exception
	 */
	public void init_all(ArrayList<Object> v) throws Exception {
		if (DEBUG) System.out.println("D_Vote:init_all: start");
		try {
			init(v);
		} catch(Exception e){e.printStackTrace();}
		if (DEBUG) System.out.println("D_Vote:init_all: contained objects");
		if (getConstituentLIDstr() != null)
			this.setConstituentAll(D_Constituent.getConstByLID(Util.lval(this.getConstituentLIDstr()), true, false));
		//if(global_motion_ID!=null) this.motion =  D_Motion.getMotiByGID(this.global_motion_ID, true, false, Util.lval(this.organization_ID));
		if (getMotionLIDstr()!=null)
			this.setMotionAndOrganizationAll(D_Motion.getMotiByLID(this.getMotionLIDstr(), true, false));
		if (getJustificationLIDstr() != null && getJustificationFromObjOrLID() == null)
			this.setJustificationAll(D_Justification.getJustByLID(this.getJustificationLIDstr(), true, false));
		if (getJustificationGID() != null && getJustificationFromObjOrLID() == null)
			this.setJustificationAll(D_Justification.getJustByGID(this.getJustificationGID(), true, false, Util.lval(getOrganizationLIDstr()), Util.lval(this.getMotionLIDstr())));
		if(DEBUG)System.out.println("D_Vote:init_all: done");
	}
	public String getConstituentNameOrMy() {
		D_Constituent c = this.getConstituent_force();
		if (c == null) return null;
		return c.getNameOrMy();
	}
	public Encoder getSignableEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(getVersion()!=null)enc.addToSequence(new Encoder(getVersion(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(getGID()!=null)enc.addToSequence(new Encoder(getGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(getConstituentGID()!=null)enc.addToSequence(new Encoder(getConstituentGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(getMotionGID()!=null)enc.addToSequence(new Encoder(getMotionGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(getJustificationGID()!=null)enc.addToSequence(new Encoder(getJustificationGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		if(getChoice()!=null)enc.addToSequence(new Encoder(getChoice(),Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC5));
		if(format!=null)enc.addToSequence(new Encoder(format,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC6));
		if(getCreationDate()!=null)enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC7));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC8));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC9));
		//if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		//if(justification!=null) enc.addToSequence(justification.getEncoder().setASN1Type(DD.TAG_AC11));
		return enc;
	}
	/**
	 * Computes the hash that is to be signed.
	 * It only uses constituentGID and motionGID (if present) replacing them with null otherwise
	 * @return
	 */
	public Encoder getHashEncoder() {
		Encoder enc = new Encoder().initSequence();
		//if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		//if(global_vote_ID!=null)enc.addToSequence(new Encoder(global_vote_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(getConstituentGID()!=null)enc.addToSequence(new Encoder(getConstituentGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(getMotionGID()!=null)enc.addToSequence(new Encoder(getMotionGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		//if(global_justification_ID!=null)enc.addToSequence(new Encoder(global_justification_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		//if(choice!=null)enc.addToSequence(new Encoder(choice,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC5));
		//if(format!=null)enc.addToSequence(new Encoder(format,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC6));
		//if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC7));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC8));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC9));
		//if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		//if(justification!=null) enc.addToSequence(justification.getEncoder().setASN1Type(DD.TAG_AC11));
		return enc;
	}	
	
	private Encoder getEntityEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(getVersion()!=null)enc.addToSequence(new Encoder(getVersion(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(getGID()!=null)enc.addToSequence(new Encoder(getGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(getConstituentGID()!=null)enc.addToSequence(new Encoder(getConstituentGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(getMotionGID()!=null)enc.addToSequence(new Encoder(getMotionGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(getJustificationGID()!=null)enc.addToSequence(new Encoder(getJustificationGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		if(getChoice()!=null)enc.addToSequence(new Encoder(getChoice(),Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC5));
		if(format!=null)enc.addToSequence(new Encoder(format,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC6));
		if(getCreationDate()!=null)enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC7));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC8));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC9));
		//if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		//if(justification!=null) enc.addToSequence(justification.getEncoder().setASN1Type(DD.TAG_AC11));
		if(getOrganizationGID()!=null)enc.addToSequence(new Encoder(getOrganizationGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		return enc;
	}
	@Override
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) {
		return getEncoder(dictionary_GIDs, 0);
	}
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs, int dependants) {
//		Util.printCallPath("getEncoder: you need to implement getEncoder(dictionaries) for objects of type: "+this);
//		return getEncoder();}
		Encoder enc = new Encoder().initSequence();
		if (getVersion() != null) enc.addToSequence(new Encoder(getVersion(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if (getGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getGID());
			enc.addToSequence(new Encoder(repl_GID, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		}
		if (getConstituentGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getConstituentGID());
			enc.addToSequence(new Encoder(repl_GID, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		}
		if (getMotionGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getMotionGID());
			enc.addToSequence(new Encoder(repl_GID, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		} else {
			Util.printCallPath("Null motionGID: "+this);
		}
		if (getJustificationGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getJustificationGID());
			enc.addToSequence(new Encoder(repl_GID, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		}
		if (getChoice() != null) enc.addToSequence(new Encoder(getChoice(),Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC5));
		if (format != null) enc.addToSequence(new Encoder(format,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC6));
		if (getCreationDate() != null) enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC7));
		if (getSignature() != null) enc.addToSequence(new Encoder(getSignature()).setASN1Type(DD.TAG_AC8));
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			int new_dependants = dependants;
			if (dependants > 0) new_dependants = dependants - 1;
				
			if (getConstituent_force() != null) enc.addToSequence(getConstituent_force().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC9));
			if (getMotionFromObjOrLID() != null) enc.addToSequence(getMotionFromObjOrLID().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC10));
			if (getJustificationFromObjOrLID() != null) enc.addToSequence(getJustificationFromObjOrLID().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC11));
		}
		/**
		 * May decide to comment encoding of "global_organization_ID" out completely, since the org_GID is typically
		 * available at the destination from enclosing fields, and will be filled out at expansion
		 * by ASNSyncPayload.expand at decoding.
		 * However, it is not that damaging when using compression, and can be stored without much overhead.
		 * So it is left here for now.  Test if you comment out!
		 */
		if (getOrganizationGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getOrganizationGID());
			enc.addToSequence(new Encoder(repl_GID, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		}
		return enc;
	}

	@Override
	public D_Vote decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0) setVersion(dec.getFirstObject(true).getString(DD.TAG_AC0));
		if(dec.getTypeByte()==DD.TAG_AC1) setGID(dec.getFirstObject(true).getString(DD.TAG_AC1));
		if(dec.getTypeByte()==DD.TAG_AC2) setConstituentGID(dec.getFirstObject(true).getString(DD.TAG_AC2));
		if(dec.getTypeByte()==DD.TAG_AC3) setMotionGID_or_Compact(dec.getFirstObject(true).getString(DD.TAG_AC3));
		if(dec.getTypeByte()==DD.TAG_AC4) setJustificationGID(dec.getFirstObject(true).getString(DD.TAG_AC4));
		if(dec.getTypeByte()==DD.TAG_AC5) setChoice(dec.getFirstObject(true).getString(DD.TAG_AC5));
		if(dec.getTypeByte()==DD.TAG_AC6) format = dec.getFirstObject(true).getString(DD.TAG_AC6);
		if(dec.getTypeByte()==DD.TAG_AC7)  setCreationDate(dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC7));
		if(dec.getTypeByte()==DD.TAG_AC8)  setSignature(dec.getFirstObject(true).getBytes(DD.TAG_AC8));
		if(dec.getTypeByte()==DD.TAG_AC9)  setConstituentObjOnly(D_Constituent.getEmpty().decode(dec.getFirstObject(true)));
		if(dec.getTypeByte()==DD.TAG_AC10) setMotionObjOnly(D_Motion.getEmpty().decode(dec.getFirstObject(true)));
		if(dec.getTypeByte()==DD.TAG_AC11) setJustification(D_Justification.getEmpty().decode(dec.getFirstObject(true)));
		if(dec.getTypeByte()==DD.TAG_AC12) setOrganizationGID(dec.getFirstObject(true).getString(DD.TAG_AC12));
		return this;
	}	
	
	public String toString() {
		return "WB_Vote:" +
				"\n hash_alg="+getVersion()+
				"\n global_vote_id="+getGID()+
				"\n global_constituent_id="+getConstituentGID()+
				"\n global_motion_id="+getMotionGID()+
				"\n global_justification_id="+getJustificationGID()+
				"\n global_organization_ID="+getOrganizationGID()+
				"\n choice="+getChoice()+
				"\n format="+format+
				"\n creation_date="+Encoder.getGeneralizedTime(getCreationDate())+
				"\n signature="+Util.byteToHexDump(getSignature())+
				"\n arrival_date="+Encoder.getGeneralizedTime(getArrivalDate())+
				
				"\n vote_ID="+getLIDstr()+
				"\n constituent_ID="+getConstituentLIDstr()+
				"\n justification_ID="+getJustificationLIDstr()+
				"\n motion_ID="+getMotionLIDstr()+
				"\n organization_ID="+getOrganizationLIDstr()+
				
				"\n voter="+getConstituent_force()+
				"\n motion="+getMotionFromObjOrLID()+
				"\n justification="+getJustificationFromObjOrLID();
	}
	/**
	 * Signs with the currently set ConstituentGID Key
	 */
	public boolean sign() {
		return null != sign(this.getConstituentGID());
	}
	public byte[] sign(String signer_GID) {
		if (DEBUG) System.out.println("WB_Vote:sign: start signer="+signer_GID);
		net.ddp2p.ciphersuits.SK sk = net.ddp2p.common.util.Util.getStoredSK(signer_GID);
		if (sk == null) 
			if(_DEBUG) System.out.println("WB_Vote:sign: no signature");
		if (DEBUG) System.out.println("WB_Vote:sign: sign="+sk);

		return sign(sk);
	}
	/**
	 * Both store signature and returns it.
	 * Returns null if cannot set GID
	 * @param sk
	 * @return
	 */
	public byte[] sign(SK sk){
		if (DEBUG) System.out.println("WB_Vote: sign: this="+this+"\nsk="+sk);
		if (this.getGID() == null) {
			if (! this.setGID()) {
				if (_DEBUG) System.out.println("WB_Vote: sign: fail for null GID, this="+this);
				return null;
			}
		}
		setSignature(Util.sign(this.getSignableEncoder().getBytes(), sk));
		
		net.ddp2p.common.hds.ClientSync.addToPayloadAdvertisements(this.getGID(),
				D_Organization.getOrgGIDHashGuess(this.getOrganizationGID()), 
				Encoder.getGeneralizedTime(this.getCreationDate()), 
				net.ddp2p.common.streaming.RequestData.SIGN);
		if (DEBUG) System.out.println("WB_Vote: sign: got this="+Util.byteToHexDump(getSignature()));
		return getSignature();
	}
	/**
	 * calls fill Globals, first.
	 * Then checks if all needed elements are present.
	 * if readyToSign (eg motion and constituent), then makes the ID and returns it.
	 * Otherwise returns 0; 
	 * @return
	 */
	public String make_ID() {
		if (DEBUG) System.out.println("WB_Vote: makeID: start");
		try {
			fillGlobalsInGID();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if (DEBUG) System.out.println("WB_Vote: makeID: id of "+this);
		// return this.global_witness_ID =  
		if (! readyToBeSigned()) {
			if (_DEBUG) System.out.println("WB_Vote: makeID: not ready yet "+this);
			return null;
		}
		String result = "V:"+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
		if (DEBUG) System.out.println("WB_Vote: makeID: id = "+result);
		return result;
	}
	/**
	 * Tells if this is ready to compute the GID and to be signed.
	 * Uses only motionGID and constituentGID.
	 * @return
	 */
	public boolean readyToBeSigned() {
		//if (this.getOrganizationGID() == null) return false;
		if (this.getMotionGID() == null) return false;
		if (this.getConstituentGID() == null) return false;
		return true;
	}

	public boolean verifySignature(){
		if (DEBUG) System.out.println("WB_Vote:verifySignature: start");
		String pk_ID = this.getConstituentGID();//.submitter_global_ID;
		if((pk_ID == null) && (this.getConstituent_force()!=null) && (this.getConstituent_force().getGID()!=null))
			pk_ID = this.getConstituent_force().getGID();
		if(pk_ID == null) return false;
		
		String newGID = make_ID();
		if (newGID == null || this.getGID() == null || ! newGID.equals(this.getGID())) {
			Util.printCallPath("WB_Vote: WRONG EXTERNAL GID");
			if (DEBUG) System.out.println("WB_Vote:verifySignature: WRONG HASH GID="+this.getGID()+" vs="+newGID);
			if (DEBUG) System.out.println("WB_Vote:verifySignature: WRONG HASH GID result="+false);
			return false;
		}
		
		boolean result = Util.verifySignByID(this.getSignableEncoder().getBytes(), pk_ID, getSignature());
		if (DEBUG) System.out.println("WB_Vote:verifySignature: result wGID="+result);
		return result;
	}
	private boolean hashConflictCreationDateDropThis() throws P2PDDSQLException {
		String this_hash=new String(this.getEntityEncoder().getBytes());
		String old = new String(new D_Vote(this.getGID()).getEntityEncoder().getBytes());
		if(old.compareTo(this_hash)>=0){
			if (DEBUG) System.out.println("D_Vote:store: signature hashConflictCreationDateDropThis true");
			return true;
		}
		if(DEBUG) System.out.println("D_Vote:store: signature hashConflictCreationDateDropThis false");
		return false;
	}
	/**
	 * before call, one should set organization_ID and global_motionID
	 * @param rq
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long store(net.ddp2p.common.streaming.RequestData sol_rq, RequestData new_rq, D_Peer __peer) throws P2PDDSQLException {
		return store(null, sol_rq, new_rq, __peer);
	}
	public long store(PreparedMessage pm,
			net.ddp2p.common.streaming.RequestData sol_rq, RequestData new_rq, D_Peer __peer) throws P2PDDSQLException {
		boolean default_blocked = false;
		boolean default_blocked_mot = false;
		boolean default_blocked_just = false;
		if(DEBUG) System.out.println("D_Vote:store: signature start");
		
		/*
		boolean locals = fillLocals(new_rq, true, true, true, true, true);
		if (! locals) {
			if (_DEBUG) System.out.println("D_Vote:store: I do not store this since I do not have some of its refered elements:"+this);
			return -1;
		}
		*/
		
		if (! this.verifySignature()) {
			if (_DEBUG) System.out.println("D_Vote:store: signature test failure="+this);
			if (! DD.ACCEPT_UNSIGNED_DATA)
				if(_DEBUG) System.out.println("D_Vote:store: signature test quit");
				return -1;
		}
		if (DEBUG) System.out.println("D_Vote:store: signature storing");

		String _old_date[] = new String[1];
		if ((this.getLIDstr() == null) && (this.getGID() != null))
			this.setLID(getLocalIDandDateforGID(this.getGID(),_old_date));
		if (this.getLIDstr() != null ) {
			String old_date = _old_date[0];//getDateFor(this.vote_ID);
			if(old_date != null) {
				String new_date = Encoder.getGeneralizedTime(this.getCreationDate());
				if(new_date.compareTo(old_date) < 0) return new Integer(getLIDstr()).longValue();
				if(new_date.compareTo(old_date)==0) {
					if(hashConflictCreationDateDropThis()) {
						if(DEBUG) System.out.println("D_Vote:store: signature hashConflictCreationDateDropThis quit");
						return new Integer(getLIDstr()).longValue();
					}
				}
			}
		}
		
		if ((this.getOrganizationLIDstr() == null ) && (this.getOrganizationGID() != null))
			this.setOrganizationLID(D_Organization.getLIDstrByGID_(this.getOrganizationGID()));
		if ((this.getOrganizationLIDstr() == null ) && (this.getOrganizationGID() != null)) {
			setOrganizationLID(""+net.ddp2p.common.data.D_Organization.insertTemporaryGID(getOrganizationGID(), __peer));
			new_rq.orgs.add(getOrganizationGID());
		}
		
		if((this.getConstituentLIDstr() == null ) && (this.getConstituentGID() != null))
			this.setConstituentLID(D_Constituent.getLIDstrFromGID(this.getConstituentGID(), Util.Lval(this.getOrganizationLIDstr())));
		if((this.getConstituentLIDstr() == null ) && (this.getConstituentGID() != null)) {
			setConstituentLID(Util.getStringID(D_Constituent.insertTemporaryGID(getConstituentGID(), null, Util.lval(this.getOrganizationLIDstr()), __peer, default_blocked)));
			new_rq.cons.put(getConstituentGID(),DD.EMPTYDATE);
		}
		
		if ((this.getMotionLIDstr() == null) && (this.getMotionGID() != null))
			this.setMotionLID(D_Motion.getLIDstrFromGID(this.getMotionGID(), Util.lval(this.getOrganizationLIDstr())));
		if ((this.getMotionLIDstr() == null) && (this.getMotionGID() != null)) {
			this.setMotionLID(Util.getStringID(D_Motion.insertTemporaryGID(getMotionGID(), Util.lval(this.getOrganizationLIDstr()), __peer, default_blocked_mot)));
			new_rq.moti.add(getMotionGID());
		}

		if ((this.getJustificationLIDstr() == null) && (this.getJustificationGID() != null)) {
			String jLID = D_Justification.getLIDstrFromGID(this.getJustificationGID(), Util.Lval(this.getOrganizationLIDstr()), Util.Lval(this.getMotionLIDstr()));
			if (DEBUG) System.out.println("D_Vote: store: previous jLID="+jLID);
			this.setJustificationLID(jLID);
		}
		if ((this.getJustificationLIDstr() == null) && (this.getJustificationGID() != null)) {
			long jLID = D_Justification.insertTemporaryGID(getJustificationGID(), Util.lval(this.getOrganizationLIDstr()), Util.lval(this.getMotionLIDstr()), __peer, default_blocked_just);
			String _jLID = Util.getStringID(jLID);
			this.setJustificationLID(_jLID);
			if (DEBUG) System.out.println("D_Vote: store: previous jLID="+jLID+" _jLID="+_jLID);
			new_rq.just.add(getJustificationGID());
		} else {
			if (DEBUG) System.out.println("D_Vote: store: previous jLID="+this.getJustificationLIDstr());
		}
		if (sol_rq != null) sol_rq.sign.put(this.getGID(), DD.EMPTYDATE);

		net.ddp2p.common.config.Application_GUI.inform_arrival(this, __peer);
		
		return storePMVerified(pm);
	
	}
	private static String getLocalIDandDateforGID(String global_vote_ID, String[]_date) throws P2PDDSQLException {
		if(DEBUG) System.out.println("WB_Vote:getLocalIDforGID: start");
		if(global_vote_ID==null) return null;
		String sql = "SELECT "+net.ddp2p.common.table.signature.signature_ID+","+net.ddp2p.common.table.signature.creation_date+
		" FROM "+net.ddp2p.common.table.signature.TNAME+
		" WHERE "+net.ddp2p.common.table.signature.global_signature_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.getDB().select(sql, new String[]{global_vote_ID}, DEBUG);
		if(n.size()==0) return null;
		_date[0] = Util.getString(n.get(0).get(1));
		return Util.getString(n.get(0).get(0));
	}
	private static String getLocalIDforGID(String global_vote_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("WB_Vote:getLocalIDforGID: start");
		if(global_vote_ID==null) return null;
		String sql = "SELECT "+net.ddp2p.common.table.signature.signature_ID+
		" FROM "+net.ddp2p.common.table.signature.TNAME+
		" WHERE "+net.ddp2p.common.table.signature.global_signature_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.getDB().select(sql, new String[]{global_vote_ID}, DEBUG);
		if(n.size()==0) return null;
		return Util.getString(n.get(0).get(0));
	}
	public static long insertTemporaryGID(String sign_GID, String mot_ID) throws P2PDDSQLException {
		if(DEBUG) System.out.println("WB_Vote:insertTemporaryGID: start");
		return Application.getDB().insert(net.ddp2p.common.table.signature.TNAME,
				new String[]{net.ddp2p.common.table.signature.global_signature_ID, net.ddp2p.common.table.signature.motion_ID},
				new String[]{sign_GID, mot_ID},
				DEBUG);
	}
	/*
	private static String getDateFor(String signID) throws P2PDDSQLException {
		String sql = "SELECT "+table.signature.creation_date+" FROM "+table.signature.TNAME+
		" WHERE "+table.signature.signature_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{""+signID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	
	public boolean fillLocals(RequestData new_rq, boolean tempOrg, boolean default_blocked_org, boolean tempConst, boolean tempMotion, boolean tempJust) throws P2PDDSQLException {
		if (DEBUG) System.out.println("D_Vote: fillLocals: start");
		D_Peer __peer = null;
		boolean default_blocked = false;
		boolean default_blocked_mot = false;
		boolean default_blocked_just = false;
	
		if((getOrganizationGID()==null)&&(getOrganizationLIDstr() == null)){
			Util.printCallPath("cannot store vote with not orgGID");
			return false;
		}
		
		if((this.getConstituentGID()==null)&&(getConstituentLIDstr() == null)){
			Util.printCallPath("cannot store vote with not submitterGID");
			return false;
		}
		if((this.getMotionGID()==null)&&(getMotionLIDstr() == null)){
			Util.printCallPath("cannot store vote with no motionGID");
			return false;
		}
		
		if((getOrganizationGID()!=null)&&(getOrganizationLIDstr() == null)){
			setOrganizationLID(Util.getStringID(D_Organization.getLIDbyGID(getOrganizationGID())));
			if(tempOrg && (getOrganizationLIDstr() == null)) {
				String orgGID_hash = D_Organization.getOrgGIDHashGuess(getOrganizationGID());
				if(new_rq!=null)new_rq.orgs.add(orgGID_hash);
				setOrganizationLID(Util.getStringID(D_Organization.insertTemporaryGID(getOrganizationGID(), orgGID_hash, default_blocked_org, __peer)));
				if(default_blocked_org) {
					Util.printCallPath("cannot store vote for blocked org");
					return false;
				}
			}
			if(getOrganizationLIDstr() == null) {
				Util.printCallPath("cannot store vote with no organization");
				return false;
			}
		}
		
		if ((this.getConstituentGID() != null) && (getConstituentLIDstr() == null)) {
			this.setConstituentLID(D_Constituent.getLIDstrFromGID(getConstituentGID(), Util.Lval(this.getOrganizationLIDstr())));
			if (tempConst && (getConstituentLIDstr() == null ))  {
				String consGID_hash = D_Constituent.getGIDHashFromGID(getConstituentGID());
				if (consGID_hash != null) {
					if (new_rq != null) new_rq.cons.put(consGID_hash,DD.EMPTYDATE);
					setConstituentLID(Util.getStringID(D_Constituent.insertTemporaryGID(getConstituentGID(), null, Util.lval(this.getOrganizationLIDstr()), __peer, default_blocked)));
				}else{
					if(_DEBUG) System.out.println("D_Vote:fill_locals: invalidGID:"+this.getConstituentGID());
				}
			}
			if(getConstituentLIDstr() == null) {
				Util.printCallPath("cannot store vote with no constituent");
				return false;
			}
		}
		if (DEBUG) System.out.println("D_Vote: fillLocals: done half");
		
		if ((this.getMotionGID() != null) && (getMotionLIDstr() == null)) {
			this.setMotionLID(D_Motion.getLIDstrFromGID(getMotionGID(), Util.lval(this.getOrganizationLIDstr())));
			if (tempMotion && (getMotionLIDstr() == null ))  {
				if (new_rq != null) new_rq.moti.add(getMotionGID());
				long mLID = D_Motion.insertTemporaryGID(getMotionGID(), Util.lval(this.getOrganizationLIDstr()), __peer, default_blocked_mot);
				if (_DEBUG) System.out.println("D_Vote: fillLocals: got temp mLID= "+mLID);
				setMotionLID(Util.getStringID(mLID));
			} else {
				if (_DEBUG) System.out.println("D_Vote: fillLocals: not tempMotion? "+tempMotion);
			}
			if (getMotionLIDstr() == null) {
				Util.printCallPath("cannot store vote with no motion");
				return false;
			}
		}
		
		if ((this.getJustificationGID() != null) && (getJustificationLIDstr() == null)) {
			this.setJustificationLID(D_Justification.getLIDstrFromGID(getJustificationGID(), Util.Lval(this.getOrganizationLIDstr()), Util.Lval(this.getMotionLIDstr())));
			if (tempJust && (getJustificationLIDstr() == null ))  {
				if (new_rq != null) new_rq.just.add(getJustificationGID());
				long jLID = D_Justification.insertTemporaryGID(getJustificationGID(), Util.Lval(this.getOrganizationLIDstr()), Util.Lval(this.getMotionLIDstr()), __peer, default_blocked_just);
				if (_DEBUG) System.out.println("D_Vote: fillLocals: got temp jLID= "+jLID);
				setJustificationLID(Util.getStringID(jLID));
			} else {
				if (_DEBUG) System.out.println("D_Vote: fillLocals: not tempJust? "+tempJust);
			}
			if (getJustificationLIDstr() == null) {
				Util.printCallPath("cannot store vote with no local justification");
				return false;
			}
		}
		if (DEBUG) System.out.println("D_Vote: fillLocals: done success");
		return true;
	}
	*/
	/**
	 * Filling all GIDs needed for packing and sending the vote away.
	 * @throws P2PDDSQLException
	 */
	public void fillGlobals() throws P2PDDSQLException {
		if((this.getOrganizationLIDstr() != null ) && (this.getOrganizationGID() == null))
			this.setOrganizationGID(D_Organization.getGIDbyLIDstr(this.getOrganizationLIDstr()));

		if((this.getJustificationLIDstr() != null ) && (this.getJustificationGID() == null))
			this.setJustificationGID(D_Justification.getGIDFromLID(this.getJustificationLIDstr()));
		
		if((this.getMotionLIDstr() != null ) && (this.getMotionGID() == null))
			this.setMotionGID(D_Motion.getGIDFromLID(this.getMotionLIDstr()));
		
		if((this.getConstituentLIDstr() != null ) && (this.getConstituentGID() == null))
			this.setConstituentGID(D_Constituent.getGIDFromLID(this.getConstituentLIDstr()));
	}
	/**
	 * Filling all GIDs needed for packing and sending the vote away.
	 * @throws P2PDDSQLException
	 */
	public void fillGlobalsInGID() throws P2PDDSQLException {
//		if((this.getOrganizationLIDstr() != null ) && (this.getOrganizationGID() == null))
//			this.setOrganizationGID(D_Organization.getGIDbyLIDstr(this.getOrganizationLIDstr()));
//
//		if((this.getJustificationLIDstr() != null ) && (this.getJustificationGID() == null))
//			this.setJustificationGID(D_Justification.getGIDFromLID(this.getJustificationLIDstr()));
		
		if((this.getMotionLIDstr() != null ) && (this.getMotionGID() == null))
			this.setMotionGID(D_Motion.getGIDFromLID(this.getMotionLIDstr()));
		
		if((this.getConstituentLIDstr() != null ) && (this.getConstituentGID() == null))
			this.setConstituentGID(D_Constituent.getGIDFromLID(this.getConstituentLIDstr()));
	}
	public long storeVerified() throws P2PDDSQLException {
		return storePMVerified(null);
	}
	public long storePMVerified(PreparedMessage pm) throws P2PDDSQLException {
		Calendar now = Util.CalendargetInstance();
		return storePMVerified(pm, now);
	}
	public long storeVerified(Calendar arrival_date) throws P2PDDSQLException {
		return storePMVerified(null, arrival_date);
	}
	static Object monitored_storePMVerified = new Object();
	public long storePMVerified(PreparedMessage pm, Calendar arrival_date) throws P2PDDSQLException {
		synchronized(monitored_storePMVerified) {
			return _monitored_storePMVerified(pm, arrival_date);
		}
	}
	private long _monitored_storePMVerified(PreparedMessage pm, Calendar arrival_date) throws P2PDDSQLException {
		//boolean DEBUG = true;
		long result = -1;
		if(DEBUG) System.out.println("WB_Vote:storeVerified: start arrival="+Encoder.getGeneralizedTime(arrival_date));
		
		if((this.getOrganizationLIDstr() == null ) && (this.getOrganizationGID() != null))
			this.setOrganizationLID(D_Organization.getLIDstrByGID_(this.getOrganizationGID()));
		
		if(this.getConstituentLIDstr() == null )
			setConstituentLID(D_Constituent.getLIDstrFromGID(this.getConstituentGID(), Util.Lval(getOrganizationLIDstr())));
		
		if(getConstituentLIDstr() == null){
			if(_DEBUG) System.out.println("WB_Vote:storeVerified: abandon no signer!");
			return -1;
		}
		if((this.getMotionLIDstr() == null ) && (this.getMotionGID() != null))
			this.setMotionLID(D_Motion.getLIDstrFromGID(this.getMotionGID(), Util.lval(getOrganizationLIDstr())));
		
		if((this.getJustificationLIDstr() == null ) && (this.getJustificationGID() != null))
			this.setJustificationLID(D_Justification.getLIDstrFromGID(this.getJustificationGID(), Util.lval(getOrganizationLIDstr()), Util.lval(getMotionLIDstr())));

		
		if((this.getLIDstr() == null ) && (this.getGID() != null))
			this.setLID(D_Vote.getSignatureLocalID(this.getGID()));
		
		if(DEBUG) System.out.println("WB_Vote:storeVerified: fixed local="+this);
		
		if (this.getGID() == null)
			if (_DEBUG) System.out.println("WB_Vote:storeVerified: why null GID="+this);
		
		String params[] = new String[net.ddp2p.common.table.signature.S_FIELDS];
		params[net.ddp2p.common.table.signature.S_HASH_ALG] = this.getVersion();
		params[net.ddp2p.common.table.signature.S_GID] = this.getGID();
		params[net.ddp2p.common.table.signature.S_CONSTITUENT_ID] = this.getConstituentLIDstr();
		params[net.ddp2p.common.table.signature.S_JUSTIFICATION_ID] = this.getJustificationLIDstr();
		params[net.ddp2p.common.table.signature.S_MOTION_ID] = this.getMotionLIDstr();
		params[net.ddp2p.common.table.signature.S_SIGNATURE] = Util.stringSignatureFromByte(getSignature());
		params[net.ddp2p.common.table.signature.S_CHOICE] = this.getChoice();
		params[net.ddp2p.common.table.signature.S_FORMAT] = this.format;
		params[net.ddp2p.common.table.signature.S_CREATION] = Encoder.getGeneralizedTime(this.getCreationDate());
		params[net.ddp2p.common.table.signature.S_ARRIVAL] = Encoder.getGeneralizedTime(arrival_date);
			
		/**
		 * Resetting caches.
		 */
		if (this.getJustificationLID() > 0) {
			D_Justification j = D_Justification.getJustByLID_AttemptCacheOnly(this.getJustificationLID(), false, false);
			if (j != null) j.resetCache(); // removing cached memory of statistics about signatures!
		}
		if (this.getMotionLIDstr() != null) {
			D_Motion m = D_Motion.getMotiByLID_AttemptCacheOnly(this.getMotionLID(), false, false);
			if (m != null) m.resetCache(); // removing cached memory of statistics about justifications!
		}
		if (this.getOrganizationLIDstr() != null) {
			D_Organization o = D_Organization.getOrgByLID_AttemptCacheOnly_NoKeep(this.getOrganizationLID(), false);
			if (o != null) o.resetCache(); // removing cached memory of statistics about justifications!
		}
			
		/**
		 * Performing the actual storage
		 */
		if (this.getLIDstr() == null) {
			if (DEBUG) System.out.println("WB_Vote:storeVerified:inserting");
			result = Application.getDB().insert(net.ddp2p.common.table.signature.TNAME,
					net.ddp2p.common.table.signature.fields_array,
					params,
					DEBUG
					);
			setLID(""+result);
		}else{
			if(DEBUG) System.out.println("WB_Vote:storeVerified:inserting");
			params[net.ddp2p.common.table.signature.S_ID] = getLIDstr();
			Application.getDB().update(net.ddp2p.common.table.signature.TNAME,
					net.ddp2p.common.table.signature.fields_noID_array,
					new String[]{net.ddp2p.common.table.signature.signature_ID},
					params,
					DEBUG
					);
			result = Util.lval(this.getLIDstr(), -1);
		}
		synchronized(BroadcastClient.msgs_monitor) {
			if(BroadcastClient.msgs!=null) {
				D_Message dm = new D_Message();
				if((this.getConstituentLIDstr() != null ) && (this.getConstituentGID() == null))
					this.setConstituentGID(D_Constituent.getGIDFromLID(this.getConstituentLIDstr()));
				if((this.getMotionLIDstr() != null ) && (this.getMotionGID() == null))
					this.setMotionGID(D_Motion.getGIDFromLID(this.getMotionLIDstr()));
				if((this.getJustificationLIDstr() != null ) && (this.getJustificationGID() == null))
					this.setJustificationGID(D_Justification.getGIDFromLID(this.getJustificationLIDstr()));
				if((this.getOrganizationLIDstr() != null ) && (this.getOrganizationGID() == null))
					this.setOrganizationGID(D_Organization.getGIDbyLIDstr(this.getOrganizationLIDstr()));
				dm.vote = this; // may have to add GIDs
				dm.sender = D_Peer.getEmpty();
				dm.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID(); //DD.getAppText(DD.APP_my_global_peer_ID);
				dm.sender.component_basic_data.name = HandlingMyself_Peer.getMyPeerName(); //DD.getAppText(DD.APP_my_peer_name);

				if((this.getSignature()!=null) && (getGID() != null)) {
					if(pm != null) {
						if(pm.raw == null)pm.raw = dm.encode();
						if(pm.motion_ID == null)pm.motion_ID=this.getMotionFromObjOrLID().getGID();
						if(this.getConstituent_force().getGIDH() != null)pm.constituent_ID_hash.add(this.getConstituent_force().getGIDH());
						if(this.getJustificationFromObjOrLID()!=null)
							if(this.getJustificationFromObjOrLID().getGID() != null)
						pm.justification_ID = this.getJustificationFromObjOrLID().getGID();
						if(this.getOrganizationGID() !=null)pm.org_ID_hash = this.getOrganizationGID();
					
						BroadcastClient.msgs.registerRecent(pm, BroadcastQueueHandled.VOTE);
					}
//					hds.ClientSync.addToPayloadAdvertisements(this.getGID(),
//							D_Organization.getOrgGIDHashGuess(this.getOrganizationGID()), 
//							Encoder.getGeneralizedTime(this.getCreationDate()), 
//							streaming.RequestData.SIGN);
					//ClientSync.payload_recent.add(streaming.RequestData.SIGN, this.getGID(), this.getCreationDate(), D_Organization.getOrgGIDHashGuess(this.getOrganizationGID()), ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
				}
			}
		}
		return result;
	}
	/**
	 * update signature
	 * @param witness_ID
	 * @param signer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static long readSignSave(long vote_ID, long signer_ID) throws P2PDDSQLException {
		D_Vote w=new D_Vote(vote_ID);
		net.ddp2p.ciphersuits.SK sk = net.ddp2p.common.util.Util.getStoredSK(D_Constituent.getGIDFromLID(signer_ID));
		w.sign(sk);
		return w.storeVerified();
	}
	public static String getSignatureLocalID(String global_vote_ID) throws P2PDDSQLException {
		String sql = "SELECT "+net.ddp2p.common.table.signature.signature_ID+" FROM "+net.ddp2p.common.table.signature.TNAME+
		" WHERE "+net.ddp2p.common.table.signature.global_signature_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.getDB().select(sql, new String[]{global_vote_ID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}

	public boolean setEditable() {
		if(getSignature() == null){
			if(DEBUG) out.println("D_Vote:editable: no sign");
			return true;
		}
		if(this.getGID() == null){
			if(DEBUG) out.println("D_Vote:editable: no GID");
			return true;
		}
		return false;
	}
	public static Hashtable<String, String> checkAvailability(
			Hashtable<String, String> sign, String orgID, boolean DBG) throws P2PDDSQLException {
		Hashtable<String, String> result = new Hashtable<String, String>();
		if(DEBUG||DBG) out.println("D_Vote:checkAvailability: not available: ads #"+sign.size());
		for (String vHash : sign.keySet()) {
			if(vHash == null){
				if(DEBUG||DBG) out.println("D_Vote:checkAvailability: null hash");
				continue;
			}
			String date = sign.get(vHash);
			if(!available(vHash, date, orgID, DBG)){
				result.put(vHash, DD.EMPTYDATE);
			}else
				if(DEBUG||DBG) out.println("D_Vote:checkAvailability: available here: "+vHash +" date="+date);
		}
		return result;
	}
	public static ArrayList<String> checkAvailability(ArrayList<String> sign,
			String orgID, boolean DBG) throws P2PDDSQLException {
		ArrayList<String> result = new ArrayList<String>();
		for (String hash : sign) {
			if(!available(hash, orgID, DBG)){
				if(DEBUG) out.println("D_Vote:editable: not available: requested: "+hash);
				result.add(hash);
			}else{
				if(DEBUG) out.println("D_Vote:editable: available already: "+hash);
			}
		}
		return result;
	}
	/**
	 * check blocking at this level
	 * @param hash
	 * @param orgID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static boolean available(String hash, String orgID, boolean DBG) throws P2PDDSQLException {
		boolean result = true;
		String sql = 
			"SELECT "+net.ddp2p.common.table.signature.signature_ID+
			" FROM "+net.ddp2p.common.table.signature.TNAME+
			" WHERE "+net.ddp2p.common.table.signature.global_signature_ID+"=? "+
			//" AND "+table.signature.organization_ID+"=? "+
			" AND "+net.ddp2p.common.table.signature.signature + " IS NOT NULL ";
		ArrayList<ArrayList<Object>> a = Application.getDB().select(sql, new String[]{hash}, DEBUG);
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_Vote:available: "+hash+" in "+orgID+"(?) = "+result);
		return result;
	}
	/**
	 * TODO: Could check whether the motion/constituent/org are not blocked/broadcastable
	 * @param hash
	 * @param creation
	 * @param orgID
	 * @param DBG
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static boolean available(String hash, String creation_date, String orgID, boolean DBG) throws P2PDDSQLException {
		String sql = 
			"SELECT "+net.ddp2p.common.table.signature.signature_ID+
			" FROM "+net.ddp2p.common.table.signature.TNAME+
			" WHERE "+net.ddp2p.common.table.signature.global_signature_ID+"=? "+
			//" AND "+table.signature.organization_ID+"=? "+
			" AND "+net.ddp2p.common.table.signature.creation_date+">= ? "+
			" AND ( "+net.ddp2p.common.table.signature.signature + " IS NOT NULL " +
			// " OR "+table.signature.blocked+" = '1'" +
					");";
		ArrayList<ArrayList<Object>> a = Application.getDB().select(sql, new String[]{hash, creation_date}, DEBUG || DBG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_Vote:available: "+hash+" in "+orgID+" = "+result);
		return result;
	}
	public static byte getASN1Type() {
		return TAG;
	}
	/**
	 * 
	 * @param gID
	 * @param DBG
	 * @return
	 *  0 for absent,
	 *  1 for present&signed,
	 *  -1 for temporary
	 * @throws P2PDDSQLException
	 */
	public static int isGIDavailable(String gID, boolean DBG) throws P2PDDSQLException {
		String sql = 
			"SELECT "+net.ddp2p.common.table.signature.signature_ID+","+net.ddp2p.common.table.signature.signature+
			" FROM "+net.ddp2p.common.table.signature.TNAME+
			" WHERE "+net.ddp2p.common.table.signature.global_signature_ID+"=? ;";
			//" AND "+table.constituent.organization_ID+"=? "+
			//" AND ( "+table.constituent.sign + " IS NOT NULL " +
			//" OR "+table.constituent.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.getDB().select(sql, new String[]{gID}, DEBUG);
		boolean result = true;
		if(a.size()==0) result = false;
		if(DEBUG||DBG) System.out.println("D_News:available: "+gID+" in "+" = "+result);
		if(a.size()==0) return 0;    
		String signature = Util.getString(a.get(0).get(1));
		if((signature!=null) && (signature.length()!=0)) return 1;
		return -1;
	}
	

	public static void _main(String[] args) {
		try {
			Application.setDB(new DBInterface(Application.DELIBERATION_FILE));
			if(args.length>0){readSignSave(3,1); if(true) return;}
			
			D_Vote c=new D_Vote(1);
			if(!c.verifySignature()) System.out.println("\n************Signature Failure\n**********\nread="+c);
			else System.out.println("\n************Signature Pass\n**********\nread="+c);
			Decoder dec = new Decoder(c.getEncoder().getBytes());
			D_Vote d = new D_Vote().decode(dec);
			Calendar arrival_date = d.setArrivalDate(Util.CalendargetInstance());
			//if(d.global_organization_ID==null) d.global_organization_ID = OrgHandling.getGlobalOrgID(d.organization_ID);
			if(!d.verifySignature()) System.out.println("\n************Signature Failure\n**********\nrec="+d);
			else System.out.println("\n************Signature Pass\n**********\nrec="+d);
			d.setGID();
			//d.storeVerified(arrival_date);
			if(_DEBUG) out.println("D_Vote:editable: ID="+d.getGID());
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		try {
			if(args.length == 0) {
				System.out.println("prog database id fix verbose");
				return;
			}
			
			String database = Application.DELIBERATION_FILE;
			if(args.length>0) database = args[0];
			
			long id = 0;
			if(args.length>1) id = Long.parseLong(args[1]);
			
			boolean fix = false;
			if(args.length>2) fix = Util.stringInt2bool(args[2], false);
			
			//boolean verbose = false;
			if(args.length>3) DEBUG = Util.stringInt2bool(args[3], false);
			
			
			Application.setDB(new DBInterface(database));
			
			ArrayList<ArrayList<Object>> l;
			D_Organization organization = null;
			D_Constituent constituent = null;
			D_Motion motion = null;
			if(id<=0){
				l = Application.getDB().select(
						"SELECT "+net.ddp2p.common.table.signature.signature_ID+
						" FROM "+net.ddp2p.common.table.signature.TNAME, new String[]{}, DEBUG);
				for(ArrayList<Object> a: l){
					String m_ID = Util.getString(a.get(0));
					long ID = Util.lval(m_ID, -1);
					D_Vote m = new D_Vote(ID);
					/*
					if(m.signature==null){
						if(organization==null)organization = D_Organization.getOrgByLID(m.organization_ID, true);
						if(constituent==null)constituent = D_Constituent.getConstByLID(Util.lval(m.constituent_ID), true, false);
						if(motion==null)motion = new D_Motion(Util.lval(m.motion_ID, -1));
						System.out.println("Fail:temporary "+m.vote_ID+": from c="+m.constituent_ID+":"+constituent.getNameFull()+",m="+m.motion_ID+":"+motion.getMotionTitle()+" in "+m.organization_ID+":"+organization.name);

//						if(fix){
//							m.global_vote_ID = m.make_ID();
//							m.storeVerified();
//							readSignSave(ID, Util.lval(m.constituent_ID, -1));
//						}
						continue;
					}
					if(!m.verifySignature()) {
						if(organization==null)organization = D_Organization.getOrgByLID(m.organization_ID, true);
						if(constituent==null)constituent = D_Constituent.getConstByLID(Util.lval(m.constituent_ID), true, false);
						if(motion==null)motion = new D_Motion(Util.lval(m.motion_ID, -1));
						System.out.println("Fail:signature "+m.vote_ID+": from c="+m.constituent_ID+":"+constituent.getNameFull()+",m="+m.motion_ID+":"+motion.getMotionTitle()+" in "+m.organization_ID+":"+organization.name);
						//System.out.println("Fail:signature "+m.vote_ID+": from c="+constituent.getName()+",m="+motion.motion_title+" in "+m.organization_ID+":"+organization.name);

						if(fix){
							m.global_vote_ID = m.make_ID();
							m.storeVerified();
							readSignSave(ID, Util.lval(m.constituent_ID, -1));
							System.out.println("Fixed:signature "+m.vote_ID+": from c="+m.constituent_ID+":"+constituent.getNameFull()+",m="+m.motion_ID+":"+motion.getMotionTitle()+" in "+m.organization_ID+":"+organization.name);
						}
						continue;
					}
					*/
				}
				return;
			}else{
				long ID = id;
				D_Vote m = new D_Vote(ID);
				/*
				if(fix)
					if(!m.verifySignature()) {
						if(organization==null)organization = D_Organization.getOrgByLID(m.organization_ID, true);
						if(constituent==null)constituent = D_Constituent.getConstByLID(Util.lval(m.constituent_ID), true, false);
						if(motion==null)motion = new D_Motion(Util.lval(m.motion_ID, -1));
						m.storeVerified();
						System.out.println("Fixing:signature "+m.vote_ID+": from c="+m.constituent_ID+":"+constituent.getNameFull()+",m="+m.motion_ID+":"+motion.getMotionTitle()+" in "+m.organization_ID+":"+organization.name);
						//System.out.println("Fixing:signature "+m.vote_ID+": from c="+constituent.getName()+",m="+motion.motion_title+" in "+m.organization_ID+":"+organization.name);
						//System.out.println("Fixing: "+m.constituent_ID+":"+m.surname+","+m.forename+" in "+m.organization_ID+":"+organization.name);
						readSignSave(ID, Util.lval(m.constituent_ID, -1));
					}
				else if(!m.verifySignature()){
					if(organization==null)organization = D_Organization.getOrgByLID(m.organization_ID, true);
					if(constituent==null)constituent = D_Constituent.getConstByLID(Util.lval(m.constituent_ID), true, false);
					if(motion==null)motion = new D_Motion(Util.lval(m.motion_ID, -1));
					System.out.println("Fail:signature "+m.vote_ID+": from c="+m.constituent_ID+":"+constituent.getNameFull()+",m="+m.motion_ID+":"+motion.getMotionTitle()+" in "+m.organization_ID+":"+organization.name);
					//System.out.println("Fail:signature "+m.vote_ID+": from c="+constituent.getName()+",m="+motion.motion_title+" in "+m.organization_ID+":"+organization.name);
				}
				*/
				return;
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		//catch (ASN1DecoderFail e) {e.printStackTrace();}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	public boolean readyToSend() {
		if(this.getMotionGID()==null) return false;
		if(this.getGID()==null) return false;
		if(this.getConstituentGID()==null) return false;
		if((this.getSignature()==null)||(this.getSignature().length==0)) return false;
		return true;
	}
	static final String sql_ac = "SELECT count(*) FROM "+net.ddp2p.common.table.signature.TNAME+" AS s "+
		" LEFT JOIN "+net.ddp2p.common.table.motion.TNAME+" AS m ON(s."+net.ddp2p.common.table.signature.motion_ID+"=m."+net.ddp2p.common.table.motion.motion_ID+")"+
		" WHERE "+net.ddp2p.common.table.motion.organization_ID+" = ?;";
	static final String sql_ac2 = "SELECT count(*) FROM "+net.ddp2p.common.table.signature.TNAME+" AS s "+
			" LEFT JOIN "+net.ddp2p.common.table.motion.TNAME+" AS m ON(s."+net.ddp2p.common.table.signature.motion_ID+"=m."+net.ddp2p.common.table.motion.motion_ID+")"+
			" WHERE m."+net.ddp2p.common.table.motion.organization_ID+" = ? AND s."+net.ddp2p.common.table.signature.creation_date+">?;";
	public static long getOrgCount(String orgID, int days) {
		long result = 0;
		try {
			ArrayList<ArrayList<Object>> orgs;
			if (days <= 0) orgs = Application.getDB().select(sql_ac, new String[]{orgID});
			else orgs = Application.getDB().select(sql_ac2, new String[]{orgID, Util.getGeneralizedDate(days)});
			if(orgs.size()>0) result = Util.lval(orgs.get(0).get(0));
			else result = 0;
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			//break;
		}
		return result;
	}
	public String getMotionGID() {
		return global_motion_ID;
	}
	public void setMotionGID(String global_motion_ID) {
		try{
			int id = Integer.parseInt(global_motion_ID);
			throw new RuntimeException("Impossible happened: Packing already packed:" + global_motion_ID);
			//return id;
		}catch(NumberFormatException e){}
		catch(Exception e) {
			Util.printCallPath(e.getLocalizedMessage());
		}

		this.global_motion_ID = global_motion_ID;
	}
	public void setMotionGID_or_Compact(String global_motion_ID) {
		//try {
			//int id = Integer.parseInt(global_motion_ID);
			//throw new RuntimeException("Impossible happened: Packing already packed:" + global_motion_ID);
		//}catch(NumberFormatException e){}

		this.global_motion_ID = global_motion_ID;
	}
	/**
	 * Returns with no check;
	 * @return
	 */
	public String getJustificationLIDstr() {
		return justification_ID;
	}
	/**
	 * This converts the string to long
	 * @return
	 */
	public long getJustificationLID() {
		return Util.lval(justification_ID);
	}
	public void setJustificationLID(String justification_LID) {
		this.justification_ID = justification_LID;
	}
	public void setJustificationLID_Dirty(String justification_LID) {
		if (! Util.equalStrings_null_or_not(this.justification_ID, justification_LID)) {
			this.justification_ID = justification_LID;
			this.dirty_main = true;
		}
	}
	/**
	 * Sets both to the parameter value, and cleans the object
	 * @param justification_LID
	 * @param justification_GID
	 */
	public void setJustificationLID_Dirty(String justification_LID, String justification_GID) {
		this.justification = null;
		if (! Util.equalStrings_null_or_not(this.justification_ID, justification_LID)) {
			this.justification_ID = justification_LID;
			this.dirty_main = true;
		}
		if (! Util.equalStrings_null_or_not(this.global_justification_ID, justification_GID)) {
			this.global_justification_ID = justification_GID;
			this.dirty_main = true;
		}
	}
	public String getJustificationGID() {
		return global_justification_ID;
	}
	/**
	 * No dirty flag
	 * @param global_justification_ID
	 */
	public void setJustificationGID(String global_justification_ID) {
		this.global_justification_ID = global_justification_ID;
	}
//	public void setJustificationGID_Dirty(String global_justification_ID) {
//		if (! Util.equalStrings_null_or_not(this.global_justification_ID, global_justification_ID)) {
//			this.global_justification_ID = global_justification_ID;
//			this.dirty_main = true;
//		}
//	}
	public Calendar getCreationDate() {
		return creation_date;
	}
	public void setCreationDate(Calendar creation_date) {
		this.creation_date = creation_date;
	}
	/**
	 * Set current date
	 */
	public void setCreationDate() {
		setCreationDate(Util.CalendargetInstance());
	}
	public Calendar getArrivalDate() {
		return arrival_date;
	}
	public Calendar setArrivalDate(Calendar arrival_date) {
		this.arrival_date = arrival_date;
		return arrival_date;
	}
	public byte[] getSignature() {
		return signature;
	}
	/**
	 * Just sets the signature member with no check
	 * @param signature
	 */
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}
	public String getChoice() {
		return choice;
	}
	public void setChoice(String choice) {
		this.choice = choice;
	}
	/**
	 * gets the GID with no check
	 * @return
	 */
	public String getGID() {
		return global_vote_ID;
	}
	/**
	 * Just sets the GID.
	 * returns true if it is non-null
	 * @param gID
	 */
	public boolean setGID(String gID) {
		return (this.global_vote_ID = gID) != null;
	}
	/**
	 * Sets the GID to makeID
	 */
	public boolean setGID() {
		return this.setGID(this.make_ID());
	}
	public String getVersion() {
		return hash_alg;
	}
	public void setVersion(String hash_alg) {
		this.hash_alg = hash_alg;
	}
	/**
	 * Gets the Org GID with no other check.
	 * @return
	 */
	public String getOrganizationGID() {
		return global_organization_ID;
	}
	/**
	 * Just sets the OrgGID with no other check
	 * @param global_organization_ID
	 */
	public void setOrganizationGID(String global_organization_ID) {
		this.global_organization_ID = global_organization_ID;
	}
	/**
	 * Gets the LID with no other check.
	 * @return
	 */
	public String getLIDstr() {
		return vote_ID;
	}
	/**
	 * Sets the LID with no other check
	 * @param vote_ID
	 */
	public void setLID(String vote_ID) {
		this.vote_ID = vote_ID;
	}
	/**
	 * Sets the LID with no other check
	 * @return
	 */
	public String getConstituentLIDstr() {
		return constituent_ID;
	}
	/**
	 * Sets the Constituent LID with no other check.
	 * @param constituent_ID
	 */
	public void setConstituentLID(String constituent_ID) {
		this.constituent_ID = constituent_ID;
	}
	/**
	 * Gets the LID with no other check
	 * @return
	 */
	public String getOrganizationLIDstr() {
		return organization_ID;
	}
	/**
	 * Converts from string
	 * @return
	 */
	public long getOrganizationLID() {
		return Util.lval(organization_ID);
	}
	/**
	 * Gets the constituent LID with no check
	 * @param organization_ID
	 */
	public void setOrganizationLID(String organization_ID) {
		this.organization_ID = organization_ID;
	}
	/**
	 * Gets the constituent GID with no check
	 * @return
	 */
	public String getConstituentGID() {
		return global_constituent_ID;
	}
	public void setConstituentGID(String global_constituent_ID) {
		this.global_constituent_ID = global_constituent_ID;
	}
	/**
	 *  Gets ConstutituentObject, forcing computation from LID if it is absent
	 * @return
	 */
	public D_Constituent getConstituent_force() {
		String cLID = this.getConstituentLIDstr();
		if ((this.constituent == null) && (cLID != null))
			this.constituent = D_Constituent.getConstByLID(cLID, true, false);
		return constituent;
	}
	/**
	 * This returns the object without checking null
	 * @return
	 */
	public D_Constituent getConstituent_direct() {
		return constituent;
	}
	/**
	 * Sets only object
	 * @param constituent
	 */
	public void setConstituentObjOnly(D_Constituent constituent) {
		this.constituent = constituent;
	}
	/**
	 * Sets object, LID and GID. and sets Signature to null
	 * @param constituent
	 */
	public void setConstituentAll(D_Constituent constituent) {
		this.constituent = constituent;
		if (constituent != null) {
			this.setConstituentGID(constituent.getGID());
			this.setConstituentLID(constituent.getLIDstr());
		} else {
			this.setConstituentGID(null);
			this.setConstituentLID(null);
		}
		this.setSignature(null);
	}
	/**
	 * Sets Motion LID with no other check
	 * @return
	 */
	public String getMotionLIDstr() {
		return motion_ID;
	}
	/**
	 * Gets Motion from string with Util.lval conversion
	 * @return
	 */
	public long getMotionLID() {
		return Util.lval(motion_ID);
	}
	public void setMotionLID(String motion_ID) {
		this.motion_ID = motion_ID;
	}
	/**
	 * Tries first the loaded object. If absent, reloads from LID.
	 * @return
	 */
	public D_Motion getMotionFromObjOrLID() {
		String mLID = this.getMotionLIDstr();
		if ((this.motion == null) && (mLID != null))
			this.motion = D_Motion.getMotiByLID(mLID, true, false);
		return motion;
	}
	/**
	 * Based only on object (can return null)
	 * @return
	 */
	public D_Motion getMotion() {
		return motion;
	}
	public void setMotionObjOnly(D_Motion motion) {
		this.motion = motion;
	}
	/**
	 * sets motion obj/LID/GID and organization LIDs and GID
	 * @param motion
	 */
	public void setMotionAndOrganizationAll(D_Motion motion) {
		this.motion = motion;
		if (motion != null) {
			this.setMotionGID(motion.getGID());
			this.setMotionLID(motion.getLIDstr());
			this.setOrganizationGID(motion.getOrganizationGID_force());
			this.setOrganizationLID(motion.getOrganizationLIDstr());
		} else {
			this.setMotionGID(null);
			this.setMotionLID(null);
		}
	}
	/**
	 * Forces to obtain the justification either from the object or from LID
	 * @return
	 */
	public D_Justification getJustificationFromObjOrLID() {
		String jLID = this.getJustificationLIDstr();
		if ((this.justification == null) && (jLID != null))
			this.justification = D_Justification.getJustByLID(jLID, true, false);
		return justification;
	}
	/**
	 * Returns only the object (maybe null if unset)
	 * @return
	 */
	public D_Justification getJustificationFromObj() {
		return justification;
	}
	/**
	 * Just sets the object and returns the parameter
	 * @param justification
	 * @return
	 */
	public D_Justification setJustification(D_Justification justification) {
		this.justification = justification;
		return justification;
	}
	/**
	 * Sets the object, GID and LID
	 * @param justification
	 * @return
	 */
	public D_Justification setJustificationAll(D_Justification justification) {
		this.justification = justification;
		if (justification != null) {
			this.setJustificationGID(justification.getGID());
			this.setJustificationLID_Dirty(justification.getLIDstr());
		} else {
			this.setJustificationGID(null);
			this.setJustificationLID_Dirty(null);
		}
		return justification;
	}
	final public static String sql_VotersByMotion =
			"SELECT "+net.ddp2p.common.table.signature.fields+" FROM "+net.ddp2p.common.table.signature.TNAME+" WHERE "+net.ddp2p.common.table.signature.motion_ID+"=?;";
	/**
	 * Returns the fields ordered as per their definition in table.signature
	 * @param motionID
	 * @param offset (use <=0 to not use)
	 * @param limit (use <=0 to not use)
	 * @return
	 */
	public static ArrayList<ArrayList<Object>> getListOfVoters(long motionID, int offset, int limit) {
		try {
			String sql = sql_VotersByMotion;
			if (limit > 0) sql += " LIMIT "+limit;
			if (offset > 0) sql += " OFFSET "+offset;			
			
			return Application.getDB().select(sql+";", new String[]{Util.getStringID(motionID)}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return new ArrayList<ArrayList<Object>>();
	}
	final public static String sql_VotersByNoJustification =
			"SELECT "+net.ddp2p.common.table.signature.fields+" FROM "+net.ddp2p.common.table.signature.TNAME+
			" WHERE "+net.ddp2p.common.table.signature.motion_ID+"=? AND ( "+net.ddp2p.common.table.signature.justification_ID+
			" IS NULL OR "+net.ddp2p.common.table.justification.justification_ID+" <= 0 )";
	final public static String sql_VotersByMotionAndJustification =
			"SELECT "+net.ddp2p.common.table.signature.fields+" FROM "+net.ddp2p.common.table.signature.TNAME+
			" WHERE "+net.ddp2p.common.table.signature.motion_ID+"=? AND "+net.ddp2p.common.table.signature.justification_ID+"=?";
	final public static String sql_VotersByJustification =
			"SELECT "+net.ddp2p.common.table.signature.fields+" FROM "+net.ddp2p.common.table.signature.TNAME+
			" WHERE "+net.ddp2p.common.table.signature.justification_ID+"=?";
	/**
	 * Returns the fields ordered as per their definition in table.signature
	 * @param motionID
	 * @param justificationID
	 * @param offset
	 * @param limit
	 * @return
	 */
	public static ArrayList<ArrayList<Object>> getListOfVoters(long motionID, long justificationID, int offset, int limit) {
		try {
			if (justificationID <= 0) {
				String sql = sql_VotersByNoJustification;
				if (limit > 0) sql += " LIMIT "+limit;
				if (offset > 0) sql += " OFFSET "+offset;			
				return Application.getDB().select(sql+";", new String[]{Util.getStringID(motionID)}, DEBUG);
			}
			if (motionID <= 0) {
				String sql = sql_VotersByJustification;
				if (limit > 0) sql += " LIMIT "+limit;
				if (offset > 0) sql += " OFFSET "+offset;			
				return Application.getDB().select(sql+";", new String[]{Util.getStringID(justificationID)}, DEBUG);
			}
			{
				String sql = sql_VotersByMotionAndJustification;
				if (limit > 0) sql += " LIMIT "+limit;
				if (offset > 0) sql += " OFFSET "+offset;			
				return Application.getDB().select(sql+";", new String[]{Util.getStringID(motionID), Util.getStringID(justificationID)}, DEBUG);
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return new ArrayList<ArrayList<Object>>();
	}
	public static ArrayList<D_Vote> getVotes(ArrayList<ArrayList<Object>> v) {
		ArrayList<D_Vote> r = new ArrayList<D_Vote> ();
		for (ArrayList<Object> vote : v) {
			try {
				r.add(new D_Vote(vote));
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		return r;
	}
}
