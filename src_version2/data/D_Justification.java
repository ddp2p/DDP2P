package data;

import static java.lang.System.out;
import static util.Util.__;
import hds.ASNSyncPayload;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import ciphersuits.SK;
import config.Application;
import config.Application_GUI;
import config.DD;
import streaming.RequestData;
import util.DDP2P_DoubleLinkedList;
import util.DDP2P_DoubleLinkedList_Node;
import util.DDP2P_DoubleLinkedList_Node_Payload;
import util.P2PDDSQLException;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
 D_JUSTIFICATION ::= SEQUENCE {
global_justificationID PrintableString,
	justification_title [0] Title_Document  OPTIONAL,
	justification_text [1] Document  OPTIONAL,
	author [2] D_Constituent OPTIONAL
	date GeneralizedDate,
	signature OCTET_STRING,
}
 */

public class D_Justification extends ASNObj implements  DDP2P_DoubleLinkedList_Node_Payload<D_Justification>, util.Summary {
	private static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	private static final String V0 = "0";
	
	private String hash_alg = V0;
	private String global_justificationID;//Printable
	private String global_motionID;//Printable
	private String global_answerTo_ID;//Printable
	private String global_constituent_ID;//Printable
	private String global_organization_ID;//Printable
	private D_Document_Title justification_title = new D_Document_Title();
	private D_Document justification_text = new D_Document();
	//public D_Constituent author;
	private Calendar last_reference_date; //not to be sent
	private Calendar creation_date;
	private byte[] signature; //OCT STR
	private Calendar arrival_date;
	private Calendar preferences_date;
	
	private D_Organization organization;
	private D_Motion motion;
	private D_Constituent constituent;
	private D_Justification answerTo;
	
	private String justification_ID;
	private String motion_ID;
	private String answerTo_ID;
	private String constituent_ID;
	private String organization_ID;
	
	private long peer_source_ID;
	private boolean temporary = false;
	private boolean requested = false;
	private boolean blocked = false;
	private boolean broadcasted = D_Organization.DEFAULT_BROADCASTED_ORG;
	private int status_references = 0;
	
	// temporary stored only in memory
	boolean loaded_globals = false;
	boolean dirty_main = false;
	boolean dirty_mydata = false;
	boolean dirty_preferences = false;
	boolean dirty_local = false;
	
	Object votes;
	
	private static Object monitor_object_factory = new Object();
	public static int MAX_LOADED_OBJECTS = 10000;
	public static long MAX_OBJECTS_RAM = 10000000;
	private static final int MIN_OBJECTS = 2;
	D_Justification_Node component_node = new D_Justification_Node(null, null);
	
	@Override
	public String toSummaryString() {
		return "D_Justification:" +
				"\n hash_alg="+getHashAlg()+
				"\n global_justificationID="+getGID()+
				"\n global_motionID="+getMotionGID()+
				"\n global_answerTo_ID="+getAnswerTo_GID()+
				"\n global_constituent_ID="+getConstituentGID()+
				"\n justification_title="+getJustificationTitle()+
				"\n justification_text="+getJustificationText()+
				//"\n author="+author+
				"\n last_reference_date="+Encoder.getGeneralizedTime(getLastReferenceDate())+
				"\n creation_date="+Encoder.getGeneralizedTime(getCreationDate())+
				"\n arrival_date="+Encoder.getGeneralizedTime(getArrivalDate())+
				"\n signature="+Util.byteToHexDump(getSignature())+
				"\n justification_ID="+getLIDstr()+
				"\n motion_ID="+getMotionLIDstr()+
				"\n answerTo_ID="+getAnswerToLIDstr()+
				"\n constituent_ID="+getConstituentLIDstr()+
				"\n motion="+getMotion()+
				"\n constituent="+getConstituent();
	}
	
	@Override
	public String toString() {
		return "D_Justification:" +
				"\n hash_alg="+getHashAlg()+
				"\n global_justificationID="+getGID()+
				"\n global_motionID="+getMotionGID()+
				"\n global_answerTo_ID="+getAnswerTo_GID()+
				"\n global_constituent_ID="+getConstituentGID()+
				"\n justification_title="+getJustificationTitle()+
				"\n justification_text="+getJustificationText()+
				//"\n author="+author+
				"\n last_reference_date="+Encoder.getGeneralizedTime(getLastReferenceDate())+
				"\n creation_date="+Encoder.getGeneralizedTime(getCreationDate())+
				"\n arrival_date="+Encoder.getGeneralizedTime(getArrivalDate())+
				"\n signature="+Util.byteToHexDump(getSignature())+
				"\n justification_ID="+getLIDstr()+
				"\n motion_ID="+getMotionLIDstr()+
				"\n answerTo_ID="+getAnswerToLIDstr()+
				"\n constituent_ID="+getConstituentLIDstr()+
				"\n motion="+getMotion()+
				"\n constituent="+getConstituent();
	}

	public static class D_Justification_Node {
		private static final int MAX_TRIES = 30;
		/** Currently loaded peers, ordered by the access time*/
		private static DDP2P_DoubleLinkedList<D_Justification> loaded_consts = new DDP2P_DoubleLinkedList<D_Justification>();
		private static Hashtable<Long, D_Justification> loaded_const_By_LocalID = new Hashtable<Long, D_Justification>();
		//private static Hashtable<String, D_Justification> loaded_const_By_GID = new Hashtable<String, D_Justification>();
		//private static Hashtable<String, D_Justification> loaded_const_By_GIDhash = new Hashtable<String, D_Justification>();
		private static Hashtable<String, Hashtable<Long, D_Justification>> loaded_const_By_GIDH_ORG = new Hashtable<String, Hashtable<Long, D_Justification>>();
		private static Hashtable<Long, Hashtable<String, D_Justification>> loaded_const_By_ORG_GIDH = new Hashtable<Long, Hashtable<String, D_Justification>>();
		private static Hashtable<String, Hashtable<Long, D_Justification>> loaded_const_By_GID_ORG = new Hashtable<String, Hashtable<Long, D_Justification>>();
		private static Hashtable<Long, Hashtable<String, D_Justification>> loaded_const_By_ORG_GID = new Hashtable<Long, Hashtable<String, D_Justification>>();
		private static long current_space = 0;
		
		//return loaded_const_By_GID.get(GID);
		private static D_Justification getConstByGID(String GID, Long organizationLID) {
			if (organizationLID != null && organizationLID > 0) {
				Hashtable<String, D_Justification> t1 = loaded_const_By_ORG_GID.get(organizationLID);
				if (t1 == null || t1.size() == 0) return null;
				return t1.get(GID);
			}
			Hashtable<Long, D_Justification> t2 = loaded_const_By_GID_ORG.get(GID);
			if ((t2 == null) || (t2.size() == 0)) return null;
			if ((organizationLID != null) && (organizationLID > 0))
				return t2.get(organizationLID);
			for (Long k : t2.keySet()) {
				return t2.get(k);
			}
			return null;
		}
		//return loaded_const_By_GID.put(GID, c);
		private static D_Justification putConstByGID(String GID, Long organizationLID, D_Justification c) {
			Hashtable<Long, D_Justification> v1 = loaded_const_By_GID_ORG.get(GID);
			if (v1 == null) loaded_const_By_GID_ORG.put(GID, v1 = new Hashtable<Long, D_Justification>());
			D_Justification result = v1.put(organizationLID, c);
			
			Hashtable<String, D_Justification> v2 = loaded_const_By_ORG_GID.get(organizationLID);
			if (v2 == null) loaded_const_By_ORG_GID.put(organizationLID, v2 = new Hashtable<String, D_Justification>());
			D_Justification result2 = v2.put(GID, c);
			
			if (result == null) result = result2;
			return result; 
		}
		//return loaded_const_By_GID.remove(GID);
		private static D_Justification remConstByGID(String GID, Long organizationLID) {
			D_Justification result = null;
			D_Justification result2 = null;
			Hashtable<Long, D_Justification> v1 = loaded_const_By_GID_ORG.get(GID);
			if (v1 != null) {
				result = v1.remove(organizationLID);
				if (v1.size() == 0) loaded_const_By_GID_ORG.remove(GID);
			}
			
			Hashtable<String, D_Justification> v2 = loaded_const_By_ORG_GID.get(organizationLID);
			if (v2 != null) {
				result2 = v2.remove(GID);
				if (v2.size() == 0) loaded_const_By_ORG_GID.remove(organizationLID);
			}
			
			if (result == null) result = result2;
			return result; 
		}
//		private static D_Justification getConstByGID(String GID, String organizationLID) {
//			return getConstByGID(GID, Util.Lval(organizationLID));
//		}
		
		private static D_Justification getConstByGIDH(String GIDH, Long organizationLID) {
			if (organizationLID != null && organizationLID > 0) {
				Hashtable<String, D_Justification> t1 = loaded_const_By_ORG_GIDH.get(organizationLID);
				if (t1 == null || t1.size() == 0) return null;
				return t1.get(GIDH);
			}
			Hashtable<Long, D_Justification> t2 = loaded_const_By_GIDH_ORG.get(GIDH);
			if ((t2 == null) || (t2.size() == 0)) return null;
			if ((organizationLID != null) && (organizationLID > 0))
				return t2.get(organizationLID);
			for (Long k : t2.keySet()) {
				return t2.get(k);
			}
			return null;
		}
		private static D_Justification putConstByGIDH(String GIDH, Long organizationLID, D_Justification c) {
			Hashtable<Long, D_Justification> v1 = loaded_const_By_GIDH_ORG.get(GIDH);
			if (v1 == null) loaded_const_By_GIDH_ORG.put(GIDH, v1 = new Hashtable<Long, D_Justification>());
			D_Justification result = v1.put(organizationLID, c);
			
			Hashtable<String, D_Justification> v2 = loaded_const_By_ORG_GIDH.get(organizationLID);
			if (v2 == null) loaded_const_By_ORG_GIDH.put(organizationLID, v2 = new Hashtable<String, D_Justification>());
			D_Justification result2 = v2.put(GIDH, c);
			
			if (result == null) result = result2;
			return result; 
		}
		//return loaded_const_By_GIDhash.remove(GIDH);
		private static D_Justification remConstByGIDH(String GIDH, Long organizationLID) {
			D_Justification result = null;
			D_Justification result2 = null;
			Hashtable<Long, D_Justification> v1 = loaded_const_By_GIDH_ORG.get(GIDH);
			if (v1 != null) {
				result = v1.remove(organizationLID);
				if (v1.size() == 0) loaded_const_By_GIDH_ORG.remove(GIDH);
			}
			
			Hashtable<String, D_Justification> v2 = loaded_const_By_ORG_GIDH.get(organizationLID);
			if (v2 != null) {
				result2 = v2.remove(GIDH);
				if (v2.size() == 0) loaded_const_By_ORG_GIDH.remove(organizationLID);
			}
			
			if (result == null) result = result2;
			return result; 
		}
//		private static D_Justification getConstByGIDH(String GIDH, String organizationLID) {
//			return getConstByGIDH(GIDH, Util.Lval(organizationLID));
//		}
		
		/** message is enough (no need to store the Encoder itself) */
		public byte[] message;
		public DDP2P_DoubleLinkedList_Node<D_Justification> my_node_in_loaded;
	
		public D_Justification_Node(byte[] message,
				DDP2P_DoubleLinkedList_Node<D_Justification> my_node_in_loaded) {
			this.message = message;
			this.my_node_in_loaded = my_node_in_loaded;
		}
		private static void register_fully_loaded(D_Justification crt) {
			assert((crt.component_node.message==null) && (crt.loaded_globals));
			if(crt.component_node.message != null) return;
			if(!crt.loaded_globals) return;
			if ((crt.getGID() != null) && (!crt.isTemporary())) {
				byte[] message = crt.encode();
				synchronized(loaded_consts) {
					crt.component_node.message = message; // crt.encoder.getBytes();
					if(crt.component_node.message != null) current_space += crt.component_node.message.length;
				}
			}
		}
		/*
		private static void unregister_loaded(D_Justification crt) {
			synchronized(loaded_orgs) {
				loaded_orgs.remove(crt);
				loaded_org_By_LocalID.remove(new Long(crt.getLID()));
				loaded_org_By_GID.remove(crt.getGID());
				loaded_org_By_GIDhash.remove(crt.getGIDH());
			}
		}
		*/
		private static void register_loaded(D_Justification crt) {
			if (DEBUG) System.out.println("D_Justification: register_loaded: start crt = "+crt);
			crt.reloadMessage(); 
			synchronized (loaded_consts) {
				String gid = crt.getGID();
				String gidh = crt.getGIDH();
				long lid = crt.getLID();
				Long organizationLID = crt.getOrganizationLID();
				
				loaded_consts.offerFirst(crt);
				if (lid > 0) {
					loaded_const_By_LocalID.put(new Long(lid), crt);
					if (DEBUG) System.out.println("D_Justification: register_loaded: store lid="+lid+" crt="+crt.getGIDH());
				}else{
					if (DEBUG) System.out.println("D_Justification: register_loaded: no store lid="+lid+" crt="+crt.getGIDH());
				}
				if (gid != null) {
					D_Justification_Node.putConstByGID(gid, crt.getOrganizationLID(), crt); //loaded_const_By_GID.put(gid, crt);
					if (DEBUG) System.out.println("D_Justification: register_loaded: store gid="+gid);
				} else {
					if (DEBUG) System.out.println("D_Justification: register_loaded: no store gid="+gid);
				}
				if (gidh != null) {
					D_Justification_Node.putConstByGIDH(gidh, organizationLID, crt);//loaded_const_By_GIDhash.put(gidh, crt);
					if (DEBUG) System.out.println("D_Justification: register_loaded: store gidh="+gidh);
				} else {
					if (DEBUG) System.out.println("D_Justification: register_loaded: no store gidh="+gidh);
				}
				if (crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				int tries = 0;
				while ((loaded_consts.size() > MAX_LOADED_OBJECTS)
						|| (current_space > MAX_OBJECTS_RAM)) {
					if (loaded_consts.size() <= MIN_OBJECTS) break; // at least _crt_peer and _myself
	
					if (tries > MAX_TRIES) break;
					tries ++;
					D_Justification candidate = loaded_consts.getTail();
					if ((candidate.status_references > 0)
							//||
							//D_Justification.is_crt_const(candidate)
							//||
							//(candidate == HandlingMyself_Peer.get_myself())
							) 
					{
						setRecent(candidate);
						continue;
					}
					
					D_Justification removed = loaded_consts.removeTail();//remove(loaded_peers.size()-1);
					loaded_const_By_LocalID.remove(new Long(removed.getLID())); 
					D_Justification_Node.remConstByGID(removed.getGID(), removed.getOrganizationLID());//loaded_const_By_GID.remove(removed.getGID());
					D_Justification_Node.remConstByGIDH(removed.getGIDH(), removed.getOrganizationLID()); //loaded_const_By_GIDhash.remove(removed.getGIDH());
					if (DEBUG) System.out.println("D_Justification: register_loaded: remove GIDH="+removed.getGIDH());
					if (removed.component_node.message != null) current_space -= removed.component_node.message.length;				
				}
			}
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_Justification crt) {
			loaded_consts.moveToFront(crt);
		}
		public static boolean dropLoaded(D_Justification removed, boolean force) {
			boolean result = true;
			synchronized(loaded_consts) {
				if (removed.status_references > 0 || removed.get_DDP2P_DoubleLinkedList_Node() == null)
					result = false;
				if (! force && ! result) {
					System.out.println("D_Justification: dropLoaded: abandon: force="+force+" rem="+removed);
					return result;
				}
				
				loaded_consts.remove(removed);
				if (removed.getLIDstr() != null) loaded_const_By_LocalID.remove(new Long(removed.getLID())); 
				if (removed.getGID() != null) D_Justification_Node.remConstByGID(removed.getGID(), removed.getLID()); //loaded_const_By_GID.remove(removed.getGID());
				if (removed.getGIDH() != null) D_Justification_Node.remConstByGIDH(removed.getGIDH(), removed.getLID()); //loaded_const_By_GIDhash.remove(removed.getGIDH());
				if (DEBUG) System.out.println("D_Justification: drop_loaded: remove GIDH="+removed.getGIDH());
				if (removed.component_node.message != null) current_space -= removed.component_node.message.length;	
				if (DEBUG) System.out.println("D_Justification: dropLoaded: exit with force="+force+" result="+result);
				return result;
			}
		}
	}
	static class D_Justification_My {
		String name;// table.my_organization_data.
		String creator;
		String category;
		long row;
	}
	D_Justification_My mydata = new D_Justification_My();	
	
	public static D_Justification getEmpty() {return new D_Justification();}
	public D_Justification instance() throws CloneNotSupportedException{return new D_Justification();}
	
	private D_Justification() {}
	private D_Justification(Long lID, boolean load_Globals) throws P2PDDSQLException {
		try {
			init(lID);
			if (load_Globals) this.fillGlobals();
		} catch (Exception e) {
			//e.printStackTrace();
			throw new P2PDDSQLException(e.getLocalizedMessage());
		}
	}
	private D_Justification(String gID, boolean load_Globals, boolean create,
			D_Peer __peer, long p_oLID, long p_mLID) throws P2PDDSQLException {
		try {
			init(gID);
			if (load_Globals) this.fillGlobals();
		} catch (Exception e) {
			//e.printStackTrace();
			if (create) {
				initNew(gID, __peer, p_oLID, p_mLID);
				return;
			}
			throw new P2PDDSQLException(e.getLocalizedMessage());
		}
	}
	
	void init(ArrayList<Object> o) throws P2PDDSQLException {
		this.hash_alg = Util.getString(o.get(table.justification.J_HASH_ALG));
		this.global_justificationID = Util.getString(o.get(table.justification.J_GID));
		this.justification_title.title_document.setFormatString(Util.getString(o.get(table.justification.J_TITLE_FORMAT)));	
		this.justification_title.title_document.setDocumentString(Util.getString(o.get(table.justification.J_TITLE)));
		this.justification_text.setFormatString(Util.getString(o.get(table.justification.J_TEXT_FORMAT)));
		this.justification_text.setDocumentString(Util.getString(o.get(table.justification.J_TEXT)));
		//this.status = Util.ival(Util.getString(o.get(table.justification.J_STATUS)), DEFAULT_STATUS);
		this.creation_date = Util.getCalendar(Util.getString(o.get(table.justification.J_CREATION)));
		this.preferences_date = Util.getCalendar(Util.getString(o.get(table.justification.J_PREFERENCES_DATE)));
		this.arrival_date = Util.getCalendar(Util.getString(o.get(table.justification.J_ARRIVAL)));
		this.signature = Util.byteSignatureFromString(Util.getString(o.get(table.justification.J_SIGNATURE)));
		this.motion_ID = Util.getString(o.get(table.justification.J_MOTION_ID));
		this.constituent_ID = Util.getString(o.get(table.justification.J_CONSTITUENT_ID));
		this.answerTo_ID = Util.getString(o.get(table.justification.J_ANSWER_TO_ID));
		this.last_reference_date = Util.getCalendar(Util.getString(o.get(table.justification.J_REFERENCE)));
		this.justification_ID = Util.getString(o.get(table.justification.J_ID));

		this.peer_source_ID = Util.lval(o.get(table.justification.J_PEER_SOURCE_ID));
		this.temporary = Util.stringInt2bool(o.get(table.justification.J_TEMPORARY),false);
		this.blocked = Util.stringInt2bool(o.get(table.justification.J_BLOCKED),false);
		this.requested = Util.stringInt2bool(o.get(table.justification.J_REQUESTED),false);
		this.broadcasted = Util.stringInt2bool(o.get(table.justification.J_BROADCASTED), D_Organization.DEFAULT_BROADCASTED_ORG_ITSELF);
		
		this.global_constituent_ID = D_Constituent.getGIDFromLID(constituent_ID); //Util.getString(o.get(table.justification.J_FIELDS+0));
		this.global_answerTo_ID = Util.getString(o.get(table.justification.J_FIELDS+0)); //+2
		//this.global_organization_ID = Util.getString(o.get(table.justification.J_FIELDS+3));
		
		//this.choices = WB_Choice.getChoices(motionID);
		//this.global_motionID = Util.getString(o.get(table.justification.J_FIELDS+0)); // +1
		//this.organization_ID = Util.getString(o.get(table.justification.J_FIELDS+2)); //+3
		//this.global_organization_ID = D_Organization.getGIDbyLIDstr(organization_ID);
		this.motion = D_Motion.getMotiByLID(motion_ID, true, false);
		if (motion != null) {
			this.global_motionID = motion.getGID();
			this.organization_ID = motion.getOrganizationLIDstr();
			this.global_organization_ID = motion.getOrganizationGID();
		}
			
		String my_const_sql = "SELECT "+table.my_justification_data.fields_list+
				" FROM "+table.my_justification_data.TNAME+
				" WHERE "+table.my_justification_data.justification_ID+" = ?;";
		ArrayList<ArrayList<Object>> my_org = Application.db.select(my_const_sql, new String[]{getLIDstr()}, DEBUG);
		if (my_org.size() != 0) {
			ArrayList<Object> my_data = my_org.get(0);
			mydata.name = Util.getString(my_data.get(table.my_justification_data.COL_NAME));
			mydata.category = Util.getString(my_data.get(table.my_justification_data.COL_CATEGORY));
			mydata.creator = Util.getString(my_data.get(table.my_justification_data.COL_CREATOR));
			// skipped preferences_date (used from main)
			mydata.row = Util.lval(my_data.get(table.my_justification_data.COL_ROW));
		}
	
		this.loaded_globals = true;
	}
	
	private D_Justification initNew(String gID, D_Peer __peer, long p_oLID,
			long p_mLID) 
	{
		if (this.global_justificationID != null)
			assert(this.getGID().equals(gID));
		this.setGID(gID);
		this.setOrganizationLID(p_oLID);
		this.setMotionLID(p_mLID);
		if (__peer != null) this.setPeerSourceLID((__peer.getLID()));
		this.dirty_main = true;
		return this;
	}
	
	static final String cond_ID = 
			"SELECT "+Util.setDatabaseAlias(table.justification.fields,"j")+
			", a."+table.justification.global_justification_ID+
			" FROM "+table.justification.TNAME+" AS j "+
			" LEFT JOIN "+table.justification.TNAME+" AS a ON(a."+table.justification.justification_ID+"=j."+table.justification.answerTo_ID+")"+
			" WHERE j."+table.justification.justification_ID+"=?;"
			;
	private void init(Long lID) throws Exception {
		ArrayList<ArrayList<Object>> a;
		a = Application.db.select(cond_ID, new String[]{Util.getStringID(lID)});
		if (a.size() == 0) throw new Exception("D_Justification:init:None for lID="+lID);
		init(a.get(0));
		if(DEBUG) System.out.println("D_Justification: init: got="+this);//result);
	}
	static final String cond_GID = 
			"SELECT "+Util.setDatabaseAlias(table.justification.fields,"j")+
			", a."+table.justification.global_justification_ID+
			" FROM "+table.justification.TNAME+" AS j "+
			" LEFT JOIN "+table.justification.TNAME+" AS a ON(a."+table.justification.justification_ID+"=j."+table.justification.answerTo_ID+")"+
			" WHERE j."+table.justification.global_justification_ID+"=?;"
			;
	private void init(String gID) throws Exception {
		ArrayList<ArrayList<Object>> a;
		a = Application.db.select(cond_GID, new String[]{gID});
		if (a.size() == 0) throw new Exception("D_Justification:init:None for GID="+gID);
		init(a.get(0));
		if(DEBUG) System.out.println("D_Justification: init: got="+this);//result);
	}	

	/**
	 * exception raised on error
	 * @param ID
	 * @param load_Globals 
	 * @return
	 */
	static private D_Justification getJustByLID_AttemptCacheOnly (long ID, boolean load_Globals) {
		if (ID <= 0) return null;
		Long id = new Long(ID);
		D_Justification crt = D_Justification_Node.loaded_const_By_LocalID.get(id);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals) {
			crt.fillGlobals();
			D_Justification_Node.register_fully_loaded(crt);
		}
		D_Justification_Node.setRecent(crt);
		return crt;
	}
	/**
	 * exception raised on error
	 * @param LID
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static private D_Justification getJustByLID_AttemptCacheOnly(Long LID, boolean load_Globals, boolean keep) {
		if (LID == null) return null;
		if (keep) {
			synchronized (monitor_object_factory) {
				D_Justification  crt = getJustByLID_AttemptCacheOnly(LID.longValue(), load_Globals);
				if (crt != null) {			
					crt.status_references ++;
					if (crt.status_references > 1) {
						System.out.println("D_Justification: getJustByGIDhash_AttemptCacheOnly: "+crt.status_references);
						Util.printCallPath("");
					}
				}
				return crt;
			}
		} else {
			return getJustByLID_AttemptCacheOnly(LID.longValue(), load_Globals);
		}
	}

	static public D_Justification getJustByLID(String LID, boolean load_Globals, boolean keep) {
		return getJustByLID(Util.Lval(LID), load_Globals, keep);
	}
	static public D_Justification getJustByLID(Long LID, boolean load_Globals, boolean keep) {
		// boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Justification: getJustByLID: "+LID+" glob="+load_Globals);
		if ((LID == null) || (LID <= 0)) {
			if (DEBUG) System.out.println("D_Justification: getJustByLID: null LID = "+LID);
			if (DEBUG) Util.printCallPath("?");
			return null;
		}
		D_Justification crt = D_Justification.getJustByLID_AttemptCacheOnly(LID, load_Globals, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Justification: getJustByLID: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_Justification.getJustByLID_AttemptCacheOnly(LID, load_Globals, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_Justification: getJustByLID: got sync cached crt="+crt);
				return crt;
			}

			try {
				crt = new D_Justification(LID, load_Globals);
				if (DEBUG) System.out.println("D_Justification: getJustByLID: loaded crt="+crt);
				D_Justification_Node.register_loaded(crt);
				if (keep) {
					crt.status_references ++;
				}
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Justification: getJustByLID: error loading");
				// e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_Justification: getJustByLID: Done");
			return crt;
		}
	}	
	
	/**
	 * No keep
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals 
	 * @return
	 */
	static private D_Justification getJustByGID_AttemptCacheOnly(String GID, Long organizationLID, boolean load_Globals) {
		D_Justification  crt = null;
		if ((GID == null)) return null;
		if ((GID != null)) crt = D_Justification_Node.getConstByGID(GID, organizationLID); //.loaded_const_By_GID.get(GID);
		
				
		if (crt != null) {
			//crt.setGID(GID, crt.getOrganizationID());
			
			if (load_Globals && !crt.loaded_globals) {
				crt.fillGlobals();
				D_Justification_Node.register_fully_loaded(crt);
			}
			D_Justification_Node.setRecent(crt);
			return crt;
		}
		return null;
	}
	/**
	 * exception raised on error
	 * @param GIDhash
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	/*
	static private D_Justification getOrgByGIDhash_AttemptCacheOnly(String GIDhash, boolean load_Globals, boolean keep) {
		if (GIDhash == null) return null;
		if (keep) {
			synchronized(monitor_object_factory) {
				D_Justification  crt = getOrgByGID_or_GIDhash_AttemptCacheOnly(null, GIDhash, load_Globals);
				if (crt != null) {			
					crt.status_references ++;
					//System.out.println("D_Organization: getOrgByGIDhash_AttemptCacheOnly: "+crt.status_references);
					//Util.printCallPath("");
				}
				return crt;
			}
		} else {
			return getOrgByGID_or_GIDhash_AttemptCacheOnly(null, GIDhash, load_Globals);
		}
	}
	*/
	/**
	 * exception raised on error
	 * @param GID
	 * @param oID
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static private D_Justification getJustByGID_AttemptCacheOnly(String GID, Long oID, boolean load_Globals, boolean keep) {
		if ((GID == null)) return null;
		if (keep) {
			synchronized (monitor_object_factory) {
				D_Justification  crt = getJustByGID_AttemptCacheOnly(GID, oID, load_Globals);
				if (crt != null) {			
					crt.status_references ++;
					if (crt.status_references > 1) {
						System.out.println("D_Organization: getOrgByGIDhash_AttemptCacheOnly: "+crt.status_references);
						Util.printCallPath("");
					}
				}
				return crt;
			}
		} else {
			return getJustByGID_AttemptCacheOnly(GID, oID, load_Globals);
		}
	}
	/**
	 * Only attempts too load (only based on GID) if the data is already in the cache
	 * exception raised on error
	 * @param GID
	 * @param load_Globals 
	 * @return
	 */
	/*
	static private D_Justification getOrgByGID_only_AttemptCacheOnly(String GID, boolean load_Globals) {
		if (GID == null) return null;
		D_Justification crt = D_Justification_Node.loaded_org_By_GID.get(GID);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals){
			crt.fillGlobals();
			D_Justification_Node.register_fully_loaded(crt);
		}
		D_Justification_Node.setRecent(crt);
		return crt;
	}
	*/
	@Deprecated
	static public D_Justification getJustByGID(String GID, boolean load_Globals, boolean keep) {
		System.out.println("Remove me setting orgID");
		return getJustByGID(GID, load_Globals, false, keep, null, -1, -1, null);
	}
	static public D_Justification getJustByGID(String GID, boolean load_Globals, boolean keep, Long oID, Long mID) {
		return getJustByGID(GID, load_Globals, false, keep, null, oID, mID, null);
	}
	/**
	 * Does not call storeRequest on creation.
	 * Therefore If create, should also set keep!
	 * @param GID
	 * @param load_Globals
	 * @param create
	 * @param keep
	 * @param __peer
	 * @param storage TODO
	 * @param oID
	 * @return
	 */
	static public D_Justification getJustByGID(String GID, 
			boolean load_Globals, boolean create, boolean keep, 
			D_Peer __peer, long p_oLID, long p_mLID, 
			D_Justification storage) 
	{
		
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Justification: getJustByGID: "+GID+" glob="+load_Globals+" cre="+create+" _peer="+__peer);
		if ((GID == null)) {
			if (_DEBUG) System.out.println("D_Justification: getJustByGID: null GID and GIDH");
			return null;
		}
		if (create) keep = true;
		D_Justification crt = D_Justification.getJustByGID_AttemptCacheOnly(GID, p_oLID, load_Globals, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Justification: getJustByGID: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_Justification.getJustByGID_AttemptCacheOnly(GID, p_oLID, load_Globals, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_Justification: getJustByGID: got sync cached crt="+crt);
				return crt;
			}

			try {
				if (storage != null)
					crt = new D_Justification(GID, load_Globals, create, __peer, p_oLID, p_mLID);
				else 
					crt = storage.initNew(GID, __peer, p_oLID, p_mLID);
				
				if (DEBUG) System.out.println("D_Justification: getJustByGID: loaded crt="+crt);
				if (keep) {
					crt.status_references ++;
				}
				D_Justification_Node.register_loaded(crt);
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Justification: getJustByGID: error loading");
				e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_Justification: getJustByGID: Done");
			return crt;
		}
	}	

	/**
	 * Usable until calling releaseReference()
	 * Verify with assertReferenced()
	 * 
	 * Do not use if changing the GID with it
	 * 
	 * @param peer
	 * @return
	 */
	static public D_Justification getJustByJust_Keep(D_Justification _obj) {
		if (_obj == null) return null;
		String GID = _obj.getGID();
		D_Justification result = null;
		if (GID != null)
			result = D_Justification.getJustByGID(GID, true, true, _obj.getOrganizationLID(), _obj.getMotionLID());
		if (result == null) {
			result = D_Justification.getJustByLID(_obj.getLID(), true, true);
		}
		if (result == null) {
			if ((_obj.getLIDstr() == null) && (_obj.getGIDH() == null)) {
				result = _obj;
				{
					_obj.status_references ++;
					//System.out.println("D_Organization: getOrgByOrg_Keep: "+org.status_references);
					//Util.printCallPath("");
				}
			}
		}
		if (result == null) {
			System.out.println("D_Organization: getOrgByOrg_Keep: got null for "+_obj);
		}
		return result;
	}
	
	private void fillGlobals() {
		if (this.loaded_globals) return;
		
		if((this.getAnswerToLIDstr() != null ) && (this.getAnswerTo_GID() == null))
			this.setAnswerTo_GID(D_Justification.getGIDFromLID(this.getAnswerToLIDstr()));
		
		if((this.getOrganizationLIDstr() != null ) && (this.getOrgGID() == null))
			this.setOrgGID(D_Organization.getGIDbyLIDstr(this.getOrganizationLIDstr()));

		if((this.getMotionLIDstr() != null ) && (this.getMotionGID() == null))
			this.setMotionGID(D_Motion.getGIDFromLID(this.getMotionLIDstr()));
		
		if((this.getConstituentLIDstr() != null ) && (this.getConstituentGID() == null))
			this.setConstituentGID(D_Constituent.getGIDFromLID(this.getConstituentLIDstr()));
		
		loaded_globals = true;
	}
	public static String getGIDFromLID(String nlID) {
		return getGIDFromLID(Util.lval(nlID));
	}
	public static String getGIDFromLID(long n_lID) {
		if (n_lID <= 0) return null;
		Long LID = new Long(n_lID);
		D_Justification c = D_Justification.getJustByLID(LID, true, false);
		if (c == null) {
			if (_DEBUG) System.out.println("D_Justification: getGIDFromLID: null for nLID = "+n_lID);
			for (Long l : D_Justification_Node.loaded_const_By_LocalID.keySet()) {
				if (_DEBUG) System.out.println("D_Justification: getGIDFromLID: available ["+l+"]"+D_Justification_Node.loaded_const_By_LocalID.get(l).getGIDH());
			}
			return null;
		}
		return c.getGID();
	}
	/**
	 * Does not create but returns -1
	 * @param GID2
	 * @return
	 */
	public static long getLIDFromGID(String GID2, Long oID, Long mID) {
		if (GID2 == null) return -1;
		D_Justification c = D_Justification.getJustByGID(GID2, true, false, oID, mID);
		return c.getLID();
	}
	public static String getLIDstrFromGID(String GID2, Long oID, Long mID) {
		if (GID2 == null) return null;
		D_Justification c = D_Justification.getJustByGID(GID2, true, false, oID, mID);
		return c.getLIDstr();
	}
	public static String getLIDstrFromGID_or_GIDH(
			String GID2, String GIDH2, Long oID, Long mID) {
		if (GID2 == null) return null;
		D_Justification c = D_Justification.getJustByGID(GID2, true, false, oID, mID);
		return c.getLIDstr();
	}
	public static long getLIDFromGID_or_GIDH(
			String GID2, String GIDH2, Long oID, Long mID) {
		if (GID2 == null) return -1;
		D_Justification c = D_Justification.getJustByGID(GID2, true, false, oID, mID);
		return c.getLID();
	}
	
	/**
	 * Storage Methods
	 */
	
	public void releaseReference() {
		if (status_references <= 0) Util.printCallPath("Null reference already!");
		else status_references --;
		//System.out.println("D_Justification: releaseReference: "+status_references);
		//Util.printCallPath("");
	}
	
	public void assertReferenced() {
		assert (status_references > 0);
	}
	
	/**
	 * The entries that need saving
	 */
	private static HashSet<D_Justification> _need_saving = new HashSet<D_Justification>();
	private static HashSet<D_Justification> _need_saving_obj = new HashSet<D_Justification>();
	
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Justification> 
	set_DDP2P_DoubleLinkedList_Node(DDP2P_DoubleLinkedList_Node<D_Justification> node) {
		DDP2P_DoubleLinkedList_Node<D_Justification> old = this.component_node.my_node_in_loaded;
		if (DEBUG) System.out.println("D_Justification: set_DDP2P_DoubleLinkedList_Node: set = "+ node);
		this.component_node.my_node_in_loaded = node;
		return old;
	}
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Justification> 
	get_DDP2P_DoubleLinkedList_Node() {
		if (DEBUG) System.out.println("D_Justification: get_DDP2P_DoubleLinkedList_Node: get");
		return component_node.my_node_in_loaded;
	}
	static void need_saving_remove(D_Justification c) {
		if (DEBUG) System.out.println("D_Justification: need_saving_remove: remove "+c);
		_need_saving.remove(c);
		if (DEBUG) dumpNeedsObj(_need_saving);
	}
	static void need_saving_obj_remove(D_Justification org) {
		if (DEBUG) System.out.println("D_Justification: need_saving_obj_remove: remove "+org);
		_need_saving_obj.remove(org);
		if (DEBUG) dumpNeedsObj(_need_saving_obj);
	}
	static D_Justification need_saving_next() {
		Iterator<D_Justification> i = _need_saving.iterator();
		if (!i.hasNext()) return null;
		D_Justification c = i.next();
		if (DEBUG) System.out.println("D_Justification: need_saving_next: next: "+c);
		//D_Justification r = D_Justification_Node.getConstByGIDH(c, null);//.loaded_const_By_GIDhash.get(c);
		if (c == null) {
			if (_DEBUG) {
				System.out.println("D_Justification Cache: need_saving_next null entry "
						+ "needs saving next: "+c);
				System.out.println("D_Justification Cache: "+dumpDirCache());
			}
			return null;
		}
		return c;
	}
	static D_Justification need_saving_obj_next() {
		Iterator<D_Justification> i = _need_saving_obj.iterator();
		if (!i.hasNext()) return null;
		D_Justification r = i.next();
		if (DEBUG) System.out.println("D_Justification: need_saving_obj_next: next: "+r);
		//D_Justification r = D_Justification_Node.loaded_org_By_GIDhash.get(c);
		if (r == null) {
			if (_DEBUG) {
				System.out.println("D_Justification Cache: need_saving_obj_next null entry "
						+ "needs saving obj next: "+r);
				System.out.println("D_Justification Cache: "+dumpDirCache());
			}
			return null;
		}
		return r;
	}
	private static void dumpNeeds(HashSet<String> _need_saving2) {
		System.out.println("D_Justification: Needs:");
		for ( String i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	private static void dumpNeedsObj(HashSet<D_Justification> _need_saving2) {
		System.out.println("Needs:");
		for ( D_Justification i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	public static String dumpDirCache() {
		String s = "[";
		s += D_Justification_Node.loaded_consts.toString();
		s += "]";
		return s;
	}
	
	public Encoder getSignableEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(getHashAlg()!=null)enc.addToSequence(new Encoder(getHashAlg(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(getGID()!=null)enc.addToSequence(new Encoder(getGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(getMotionGID()!=null)enc.addToSequence(new Encoder(getMotionGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(getAnswerTo_GID()!=null)enc.addToSequence(new Encoder(getAnswerTo_GID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(getJustificationTitle()!=null) enc.addToSequence(getJustificationTitle().getEncoder().setASN1Type(DD.TAG_AC4));
		if(getJustificationText()!=null)enc.addToSequence(getJustificationText().getEncoder().setASN1Type(DD.TAG_AC5));
		if(getConstituentGID()!=null)enc.addToSequence(new Encoder(getConstituentGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		//if(author!=null)enc.addToSequence(author.getEncoder().setASN1Type(DD.TAG_AC6));
		//if(last_reference_date!=null)enc.addToSequence(new Encoder(last_reference_date).setASN1Type(DD.TAG_AC7));
		if(getCreationDate()!=null)enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC8));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC9));
		//if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC11));
		return enc;
	}
	
	public Encoder getHashEncoder() {
		Encoder enc = new Encoder().initSequence();
		//if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		//if(global_justificationID!=null)enc.addToSequence(new Encoder(global_justificationID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(getMotionGID()!=null)enc.addToSequence(new Encoder(getMotionGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(getAnswerTo_GID()!=null)enc.addToSequence(new Encoder(getAnswerTo_GID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(getJustificationTitle()!=null) enc.addToSequence(getJustificationTitle().getEncoder().setASN1Type(DD.TAG_AC4));
		if(getJustificationText()!=null)enc.addToSequence(getJustificationText().getEncoder().setASN1Type(DD.TAG_AC5));
		if(getConstituentGID()!=null)enc.addToSequence(new Encoder(getConstituentGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		//if(author!=null)enc.addToSequence(author.getEncoder().setASN1Type(DD.TAG_AC6));
		//if(last_reference_date!=null)enc.addToSequence(new Encoder(last_reference_date).setASN1Type(DD.TAG_AC7));
		//if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC8));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC9));
		//if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC11));
		return enc;
	}
	public static byte getASN1Type() {
		return TAG;
	}

	@Override
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) {
		return getEncoder(dictionary_GIDs, 0);
	}
	/**
	 * parameter used for dictionaries
	 */
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs, int dependants) {
		int new_dependants = dependants;
		if (dependants > 0) new_dependants = dependants - 1;
		Encoder enc = new Encoder().initSequence();
		if (getHashAlg() != null)enc.addToSequence(new Encoder(getHashAlg(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if (getGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		}
		if (getMotionGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getMotionGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		}
		if (getAnswerTo_GID()!=null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getAnswerTo_GID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		}
		if (getConstituentGID()!=null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getConstituentGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		}
		if (getJustificationTitle()!=null) enc.addToSequence(getJustificationTitle().getEncoder().setASN1Type(DD.TAG_AC5));
		if (getJustificationText()!=null)enc.addToSequence(getJustificationText().getEncoder().setASN1Type(DD.TAG_AC6));
		//if(author!=null)enc.addToSequence(author.getEncoder().setASN1Type(DD.TAG_AC7));
		//if(last_reference_date!=null)enc.addToSequence(new Encoder(last_reference_date).setASN1Type(DD.TAG_AC8));
		if(getCreationDate()!=null)enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC9));
		if(getSignature()!=null)enc.addToSequence(new Encoder(getSignature()).setASN1Type(DD.TAG_AC10));
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (getMotion()!=null)enc.addToSequence(getMotion().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC11));
			if (getConstituent()!=null)enc.addToSequence(getConstituent().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC12));
			if (getAnswerTo()!=null)enc.addToSequence(getAnswerTo().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC13));
		}
		/**
		 * May decide to comment encoding of "global_organization_ID" out completely, since the org_GID is typically
		 * available at the destination from enclosing fields, and will be filled out at expansion
		 * by ASNSyncPayload.expand at decoding.
		 * However, it is not that damaging when using compression, and can be stored without much overhead.
		 * So it is left here for now.  Test if you comment out!
		 */
		if (getOrgGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getOrgGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC14));
		}
		return enc;
	}

	@Override
	public D_Justification decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0)hash_alg = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if(dec.getTypeByte()==DD.TAG_AC1)global_justificationID = dec.getFirstObject(true).getString(DD.TAG_AC1);
		if(dec.getTypeByte()==DD.TAG_AC2)global_motionID = dec.getFirstObject(true).getString(DD.TAG_AC2);
		if(dec.getTypeByte()==DD.TAG_AC3)global_answerTo_ID = dec.getFirstObject(true).getString(DD.TAG_AC3);
		if(dec.getTypeByte()==DD.TAG_AC4)global_constituent_ID = dec.getFirstObject(true).getString(DD.TAG_AC4);
		if(dec.getTypeByte()==DD.TAG_AC5)justification_title = new D_Document_Title().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC6)justification_text = new D_Document().decode(dec.getFirstObject(true));
		//if(dec.getTypeByte()==DD.TAG_AC7)author = new D_Constituent().decode(dec.getFirstObject(true));
		//if(dec.getTypeByte()==DD.TAG_AC8)last_reference_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC8);
		if(dec.getTypeByte()==DD.TAG_AC9)creation_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC9);
		if(dec.getTypeByte()==DD.TAG_AC10)signature = dec.getFirstObject(true).getBytes(DD.TAG_AC10);
		if(dec.getTypeByte()==DD.TAG_AC11)motion = D_Motion.getEmpty().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC12)constituent = D_Constituent.getEmpty().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC13)answerTo = new D_Justification().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC14)global_organization_ID = dec.getFirstObject(true).getString(DD.TAG_AC14);
		
		if ((global_constituent_ID == null) && (constituent != null)) global_constituent_ID = constituent.getGID();
		if ((global_answerTo_ID == null) && (answerTo != null)) global_answerTo_ID = answerTo.global_justificationID;
		if ((global_motionID == null) && (motion != null)) global_motionID = motion.getGID();
		return this;
	}

	public static ArrayList<String> checkAvailability(ArrayList<String> hashes,
			String orgID, String mID, boolean DBG) throws P2PDDSQLException {
		ArrayList<String> result = new ArrayList<String>();
		for (String hash : hashes) {
			if(!available(hash, orgID, mID, DBG)) result.add(hash);
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
	public static boolean available(String hash, String orgID, String mID, boolean DBG) throws P2PDDSQLException {
		boolean result = true;
		/*
		String sql = 
			"SELECT "+table.justification.justification_ID+
			" FROM "+table.justification.TNAME+
			" WHERE "+table.justification.global_justification_ID+"=? "+
			//" AND "+table.justification.organization_ID+"=? "+
			" AND ( "+table.justification.signature + " IS NOT NULL " +
			" OR "+table.justification.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{hash}, DEBUG);
		if(a.size()==0) result = false;
		*/
		D_Justification j = D_Justification.getJustByGID(hash, true, false, Util.lval(orgID), Util.lval(mID));
		if (
				(j == null) 
				|| ( j.getOrganizationLID() != Util.lval(orgID)) 
				|| ( (mID != null) && (j.getMotionLID() != 0) && j.getMotionLID() != Util.lval(mID))
				|| ( (
						(j.getSignature() == null )
						|| (j.getSignature().length == 0)
						|| ( j.isTemporary())) 
						&& !j.isBlocked() )
				) result = false;
		if(DEBUG||DBG) System.out.println("D_Justification: available: "+hash+" in "+orgID+"(?) = "+result);
		return result;
	}
	public long storeLinkNewTemporary() {
		synchronized (monitor_object_factory) {
			if (this.getGID() != null) {
				D_Justification m = D_Justification.getJustByGID(this.getGID(), true, false, this.getOrganizationLID(), this.getMotionLID());
				if (m != null) return m.getLID_force();
			}
			this.storeSynchronouslyNoException();
			D_Justification_Node.register_loaded(this);
		}
		return 0;
	}
	public void changeToDefaultAnswer() {
		this.setAnswerToLIDstr(getLIDstr());
		this.setAnswerTo_GID(this.getGID());
		this.setLIDstr(null);
		this.setGID(null);
		this.setSignature(null);
		this.setConstituentLIDstr(null);
		this.setConstituentGID(null);
		this.getJustificationTitle().title_document.setDocumentString(__("Answer To:")+" "+this.getJustificationTitle().title_document.getDocumentUTFString());
		this.getJustificationText().setDocumentString(__("Answer To:")+" \n"+this.getJustificationText().getDocumentUTFString());
		this.setCreationDate(this.setArrivalDate(Util.CalendargetInstance()));
		this.setRequested(this.setBlocked(this.setBroadcasted(false)));
	}
	public void setEditable() {
		setSignature(null);
		this.setGID(null);
	}
	public boolean isEditable() {
		if(getSignature() == null){
			if(DEBUG) out.println("D_Justification:editable: no sign");
			return true;
		}
		if(getGID() == null){
			if(DEBUG) out.println("D_Justification:editable: no GID");
			return true;
		}
		/*
		if(!verifySignature()) {
			if(DEBUG) out.println("D_Justification:editable: not verifiable signature");
			setSignature(null);
			try {
				storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return true;
		}
		*/
		return false;
	}
	
	long storeAct() throws P2PDDSQLException {
		///Util.printCallPath("");
		
		if (dirty_main || dirty_preferences || dirty_local) {
			storeAct_main();
		}
		if (this.dirty_mydata) {
			storeAct_mydata();
		}
		return this.getLID();
	}
	long storeAct_mydata() throws P2PDDSQLException {
		this.dirty_mydata = false;
		String param[];
		if (this.mydata.row <= 0) {
			param = new String[table.my_justification_data.FIELDS_NB_NOID];
		} else {
			param = new String[table.my_justification_data.FIELDS_NB];
		}
		param[table.my_justification_data.COL_NAME] = this.mydata.name;
		param[table.my_justification_data.COL_CATEGORY] = this.mydata.category;
		param[table.my_justification_data.COL_CREATOR] = this.mydata.creator;
		param[table.my_justification_data.COL_JUSTIFICATION_LID] = this.getLIDstr();
		try {
			if (this.mydata.row <= 0) {
				this.mydata.row =
						Application.db.insert(true, table.my_justification_data.TNAME,
								table.my_justification_data.fields_noID, param, DEBUG);
			} else {
				param[table.my_justification_data.COL_ROW] = this.mydata.row+"";
				Application.db.update(true, table.my_justification_data.TNAME,
						table.my_justification_data.fields_noID,
						new String[]{table.my_justification_data.justification_ID}, param, DEBUG);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this.mydata.row;
	}
	long storeAct_main() throws P2PDDSQLException {
		dirty_main = dirty_preferences = dirty_local = false;
		long result;


		if (this.arrival_date == null && (this.getGIDH() != null)) {
			this.arrival_date = Util.CalendargetInstance();
			if (_DEBUG) System.out.println("D_Justification: storeAct_main: missing arrival_date");
		}		
		
		if (DEBUG) System.out.println("D_Justification:storeVerified: fixed local="+this);
		
		String params[] = new String[table.justification.J_FIELDS];
		params[table.justification.J_HASH_ALG] = this.hash_alg;
		params[table.justification.J_GID] = this.global_justificationID;
		params[table.justification.J_TITLE_FORMAT] = this.justification_title.title_document.getFormatString();
		params[table.justification.J_TEXT_FORMAT] = this.justification_text.getFormatString();
		params[table.justification.J_TITLE] = this.justification_title.title_document.getDocumentString();
		params[table.justification.J_TEXT] = this.justification_text.getDocumentString();
		params[table.justification.J_ANSWER_TO_ID] = this.answerTo_ID;
		params[table.justification.J_CONSTITUENT_ID] = this.constituent_ID;
		//params[table.justification.J_ORG_ID] = organization_ID;
		//params[table.justification.J_STATUS] = status+"";
		params[table.justification.J_MOTION_ID] = this.motion_ID;
		params[table.justification.J_SIGNATURE] = Util.stringSignatureFromByte(signature);
		params[table.justification.J_REFERENCE] = Encoder.getGeneralizedTime(this.last_reference_date);
		params[table.justification.J_CREATION] = Encoder.getGeneralizedTime(this.creation_date);
		params[table.justification.J_PREFERENCES_DATE] = Encoder.getGeneralizedTime(this.preferences_date);
		params[table.justification.J_ARRIVAL] = Encoder.getGeneralizedTime(arrival_date);
		params[table.justification.J_BLOCKED] = Util.bool2StringInt(blocked);
		params[table.justification.J_REQUESTED] = Util.bool2StringInt(requested);
		params[table.justification.J_BROADCASTED] = Util.bool2StringInt(broadcasted);
		params[table.justification.J_TEMPORARY] = Util.bool2StringInt(temporary);
		params[table.justification.J_PEER_SOURCE_ID] = Util.getStringID(peer_source_ID);
		if(this.justification_ID == null) {
			if(DEBUG) System.out.println("WB_Justification:storeVerified:inserting");
			result = Application.db.insert(table.justification.TNAME,
					table.justification.fields_array,
					params,
					DEBUG
					);
			justification_ID=""+result;
		}else{
			if(DEBUG) System.out.println("WB_Justification:storeVerified:updating ID="+this.justification_ID);
			params[table.justification.J_ID] = justification_ID;
			Application.db.update(table.justification.TNAME,
					table.justification.fields_noID_array,
					new String[]{table.justification.justification_ID},
					params,
					DEBUG
					);
		}
		return this.getLID();
	}
	
	/** Storing */
	public static D_Justification_SaverThread saverThread = new D_Justification_SaverThread();
	private boolean dirty_any() {
		return dirty_main || dirty_local || dirty_preferences || dirty_mydata;
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
	public static int isGIDavailable(String gID, long oID, long mID, boolean DBG) throws P2PDDSQLException {
		D_Justification n = D_Justification.getJustByGID(gID, true, false, oID, mID);
		if (n == null) return 0;
		if (n.isTemporary()) return -1;
		return 1;
	}
	/**
	 * This function has to be called after the appropriate dirty flags are set:
	 * dirty_main - for elements in the peer table
	 * 
	 * This returns asynchronously (without waiting for the storing to happen).
	 */
	public void storeRequest() {
		
		//Util.printCallPath("Why store?");
		
		if (!this.dirty_any()) {
			Util.printCallPath("Why store when not dirty?");
			return;
		}

		if (this.arrival_date == null && (this.getGIDH() != null)) {
			this.arrival_date = Util.CalendargetInstance();
			if (_DEBUG) System.out.println("D_Justification: storeRequest: missing arrival_date");
			Util.printCallPath("");
		}
		
		String save_key = this.getGIDH();
		
		if (save_key == null) {
			//save_key = ((Object)this).hashCode() + "";
			//Util.printCallPath("Cannot store null:\n"+this);
			//return;
			D_Justification._need_saving_obj.add(this);
			if (DEBUG) System.out.println("D_Motion: storeRequest: added to _need_saving_obj");
		} else {		
			if (DEBUG) System.out.println("D_Motion: storeRequest: GIDH="+save_key);
			D_Justification._need_saving.add(this);
		}
		try {
			if (!saverThread.isAlive()) { 
				if (DEBUG) System.out.println("D_Motion: storeRequest:startThread");
				saverThread.start();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * This function has to be called after the appropriate dirty flags are set:
	 * dirty_main - for elements in the peer table
	 * dirty_my_data - ...
	 * dirty_choices - ...
	 * 
	 * @return This returns synchronously (waiting for the storing to happen), and returns the local ID.
	 */
	public long storeRequest_getID() {
		if ( this.getLIDstr() == null ) 
			return this.storeSynchronouslyNoException();
		this.storeRequest();
		return this.getLID();
	}
	/**
	 * This can be delayed saving
	 * @return
	 */
	public long storeSynchronouslyNoException() {
		try {
			return storeAct();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
		
	
	/**
	 * Load this message in the storage cache node.
	 * Should be called each time I load a node and when I sign myself.
	 */
	private void reloadMessage() {
		if (this.loaded_globals) this.component_node.message = this.encode(); //crt.encoder.getBytes();
	}
	
	public boolean readyToSend() {
		if(this.getGID()==null) return false;
		if(this.getMotionGID()==null) return false;
		return true;
	}
	public byte[] sign() {
		return sign(this.getConstituentGID());
	}
	public byte[] sign(String signer_GID) {
		if(DEBUG) System.out.println("WB_Justification:sign: start signer="+signer_GID);
		ciphersuits.SK sk = util.Util.getStoredSK(signer_GID);
		if(sk==null) {
			if(_DEBUG) System.out.println("WB_Justification:sign: no signature");
			Application_GUI.warning(__("No secret key to sign motion, no constituent GID"), __("No Secret Key!"));
			return null;
		}
		if(DEBUG) System.out.println("WB_Justification:sign: sign="+sk);

		return sign(sk);
	}
	/**
	 * Both store signature and returns it
	 * @param sk
	 * @return
	 */
	public byte[] sign(SK sk){
		if(DEBUG) System.out.println("WB_Justification:sign: this="+this+"\nsk="+sk);
		setSignature(Util.sign(this.getSignableEncoder().getBytes(), sk));
		if(DEBUG) System.out.println("WB_Justification:sign:got this="+Util.byteToHexDump(getSignature()));
		return getSignature();
	}
	public String make_ID() {
		fillGlobals();
		// return this.global_witness_ID =  
		return D_GIDH.d_Just+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
	}
	public boolean verifySignature(){
		if(DEBUG) System.out.println("WB_Justification:verifySignature: start");
		String pk_ID = this.getConstituentGID();//.submitter_global_ID;
		if((pk_ID == null) && (this.getConstituent()!=null) && (this.getConstituent().getGID()!=null))
			pk_ID = this.getConstituent().getGID();
		if(pk_ID == null) return false;
		
		String newGID = make_ID();
		if(!newGID.equals(this.getGID())) {
			Util.printCallPath("WB_Justification: WRONG EXTERNAL GID");
			if(DEBUG) System.out.println("WB_Justification:verifySignature: WRONG HASH GID="+this.getGID()+" vs="+newGID);
			if(DEBUG) System.out.println("WB_Justification:verifySignature: WRONG HASH GID result="+false);
			return false;
		}
		
		boolean result = Util.verifySignByID(this.getSignableEncoder().getBytes(), pk_ID, getSignature());
		if(DEBUG) System.out.println("WB_Justification:verifySignature: result wGID="+result);
		return result;
	}
	
	public String getHashAlg() {
		return hash_alg;
	}
	public void setHashAlg(String hash_alg) {
		this.hash_alg = hash_alg;
		this.dirty_main = true;
	}
	public String getGIDH() {
		return global_justificationID;
	}
	public String getGID() {
		return global_justificationID;
	}
	public void setGID(String _global_justificationID) {
		assert(Util.equalStrings_or_one_null(_global_justificationID, this.global_justificationID));
		this.global_justificationID = _global_justificationID;
		this.dirty_main = true;
	}
	public String getMotionGID() {
		return global_motionID;
	}
	public void setMotionGID(String global_motionID) {
		this.global_motionID = global_motionID;
	}
	public String getAnswerTo_GID() {
		return global_answerTo_ID;
	}
	public void setAnswerTo_GID(String global_answerTo_ID) {
		this.global_answerTo_ID = global_answerTo_ID;
	}
	public String getConstituentGIDH() {
		if (global_constituent_ID != null) return D_Constituent.getGIDHashFromGID(this.getConstituentGID());
		if (this.constituent != null) return constituent.getGIDH();
		return null;
	}
	public String getConstituentGID() {
		return global_constituent_ID;
	}
	public void setConstituentGID(String global_constituent_ID) {
		this.global_constituent_ID = global_constituent_ID;
	}
	public String getOrgGID() {
		return global_organization_ID;
	}
	public String getOrgGIDH() {
		if (global_organization_ID != null) return D_Organization.getOrgGIDHashGuess(getOrgGID());
		if (this.organization != null) return organization.getGIDH_or_guess();
		return null;
	}
	public void setOrgGID(String global_organization_ID) {
		this.global_organization_ID = global_organization_ID;
	}
	public String guessOrganizationGID() throws P2PDDSQLException {
		String motion_org;
		String const_org;
		long oID=-1,m_oID=-1,c_oID=-1;
		
		D_Motion m = D_Motion.getMotiByGID(this.getMotionGID(), true, false);
		if (m != null) return m.getOrganizationGID();

		D_Constituent cs = D_Constituent.getConstByGID_or_GIDH(this.getConstituentGID(), null, true, false);
		if (cs != null) c_oID = cs.getOrganizationID();
		if((c_oID>0)&&(oID>0)&&(c_oID!=oID)){
			if(_DEBUG) System.out.println("D_Just:guess: "+c_oID+" vs "+oID);
			return null;
		}
		if(DEBUG) System.out.println("D_Just:guess right: "+c_oID+" vs "+oID);
		if(c_oID>0) oID=c_oID;
		return D_Organization.getGIDbyLIDstr(Util.getStringID(oID));
	}
	public D_Document_Title getJustificationTitle() {
		return justification_title;
	}
	public void setJustificationTitle(D_Document_Title justification_title) {
		this.justification_title = justification_title;
		this.dirty_main = true;
	}
	public D_Document getJustificationText() {
		return justification_text;
	}
	public void setJustificationText(D_Document justification_text) {
		this.justification_text = justification_text;
		this.dirty_main = true;
	}
	public Calendar getLastReferenceDate() {
		return last_reference_date;
	}
	public void setLastReferenceDate(Calendar last_reference_date) {
		this.last_reference_date = last_reference_date;
		this.dirty_local = true;
	}
	public String getPreferencesDateStr() {
		return Encoder.getGeneralizedTime(this.getPreferencesDate());
	}
	public Calendar getPreferencesDate() {
		return preferences_date;
	}
	public void setPreferencesDate(Calendar preferences_date) {
		this.preferences_date = preferences_date;
		this.dirty_main = true;
	}
	public String getCreationDateStr() {
		return Encoder.getGeneralizedTime(this.getCreationDate());
	}
	public Calendar getCreationDate() {
		return creation_date;
	}
	public void setCreationDate(Calendar creation_date) {
		this.creation_date = creation_date;
		this.dirty_main = true;
	}
	public byte[] getSignature() {
		return signature;
	}
	public void setSignature(byte[] signature) {
		this.signature = signature;
		this.dirty_main = true;
	}
	public String getArrivalDateStr() {
		return Encoder.getGeneralizedTime(this.getArrivalDate());
	}
	public Calendar getArrivalDate() {
		return arrival_date;
	}
	public Calendar setArrivalDate(Calendar arrival_date) {
		this.arrival_date = arrival_date;
		this.dirty_local = true;
		return arrival_date;
	}
	public D_Motion getMotion() {
		return motion;
	}
	public void setMotion(D_Motion motion) {
		this.motion = motion;
	}
	public D_Constituent getConstituent() {
		return constituent;
	}
	public void setConstituent(D_Constituent constituent) {
		this.constituent = constituent;
	}
	public D_Justification getAnswerTo() {
		return answerTo;
	}
	public void setAnswerTo(D_Justification answerTo) {
		this.answerTo = answerTo;
		this.dirty_main = true;
	}
	public long getLID_force() {
		if (this.justification_ID != null) return this.getLID();
		return this.storeSynchronouslyNoException();
	}
	public long getLID() {
		return Util.lval(justification_ID);
	}
	public String getLIDstr() {
		return justification_ID;
	}
	public void setLIDstr(String justification_ID) {
		this.justification_ID = justification_ID;
	}
	public String getMotionLIDstr() {
		return motion_ID;
	}
	public long getMotionLID() {
		return Util.lval(motion_ID);
	}
	public void setMotionLIDstr(String motion_ID) {
		this.motion_ID = motion_ID;
		this.dirty_main = true;
	}
	public void setMotionLID(long _motion_ID) {
		this.motion_ID = Util.getStringID(_motion_ID);
		this.dirty_main = true;
	}
	public String getAnswerToLIDstr() {
		return answerTo_ID;
	}
	public void setAnswerToLIDstr(String answerTo_ID) {
		this.answerTo_ID = answerTo_ID;
		this.dirty_main = true;
	}
	public String getConstituentLIDstr() {
		return constituent_ID;
	}
	public void setConstituentLIDstr(String constituent_ID) {
		this.constituent_ID = constituent_ID;
		this.dirty_main = true;
	}
	public void setConstituentLID(long _constituent_ID) {
		this.constituent_ID = Util.getString(_constituent_ID);
		this.dirty_main = true;
	}
	public String getOrganizationLIDstr() {
		return organization_ID;
	}
	public long getOrganizationLID() {
		return Util.lval(organization_ID);
	}
	public void setOrganizationLIDstr(String organization_ID) {
		this.organization_ID = organization_ID;
		//this.dirty_main = true; // not saved in db
	}
	public void setOrganizationLID(long _organization_ID) {
		this.organization_ID = "" + _organization_ID;
		//this.dirty_main = true; // not in db
	}
	public boolean isRequested() {
		return requested;
	}
	public void setRequested(boolean requested) {
		this.requested = requested;
		this.dirty_preferences = true;
	}
	public boolean isBlocked() {
		return blocked;
	}
	public boolean setBlocked(boolean blocked) {
		this.blocked = blocked;
		return blocked;
	}
	public boolean isBroadcasted() {
		return broadcasted;
	}
	public boolean setBroadcasted(boolean broadcasted) {
		this.broadcasted = broadcasted;
		this.dirty_preferences = true;
		return broadcasted;
	}
	public void setPeerSourceLID(long l) {
		this.peer_source_ID = l;
		this.dirty_local = true;
	}

	public boolean isTemporary() {
		return temporary;
	}

	public void setTemporary(boolean temporary) {
		this.temporary = temporary;
		this.dirty_main = true;
	}
	/**
	 * 
	 * @param p_cGID
	 * @param p_oLID
	 * @param p_mLID
	 * @param __peer
	 * @param default_blocked
	 * @return the LID
	 */
	public static long insertTemporaryGID(String p_GID,
			long p_oLID, long p_mLID, D_Peer __peer, boolean default_blocked) {
		D_Justification obj = D_Justification.insertTemporaryGID_org(p_GID, p_oLID, p_mLID, __peer, default_blocked);
		if (obj == null) return -1;
		return obj.getLID_force(); 
	}
	/**
	 * 
	 * @param p_GID
	 * @param p_oLID
	 * @param p_mLID
	 * @param __peer
	 * @param default_blocked
	 * @return the object
	 */
	public static D_Justification insertTemporaryGID_org(
			String p_GID, long p_oLID, long p_mLID,
			D_Peer __peer, boolean default_blocked) {
		D_Justification obj;
		if ((p_GID != null)) {
			obj = D_Justification.getJustByGID(p_GID, true, true, true, __peer, p_oLID, p_mLID, null);
			//consts.setName(_name);
			obj.setBlocked(default_blocked);
			obj.storeRequest();
			obj.releaseReference();
		}
		else return null;
		return obj; 
	}
	/**
	 * Tests newer
	 * @param r
	 * @param sol_rq
	 * @param new_rq
	 * @return 
	 */
	public boolean loadRemote(D_Justification r, RequestData sol_rq,
			RequestData new_rq, D_Peer __peer) {
		boolean default_blocked_org = false;
		boolean default_blocked_cons = false;
		boolean default_blocked_mot = false;
		
		if (!this.isTemporary()) return false;
		
		this.hash_alg = r.hash_alg;
		this.justification_title = r.justification_title;
		this.justification_text = r.justification_text;
		this.creation_date = r.creation_date;
		this.signature = r.signature;
		
		if (!Util.equalStrings_null_or_not(this.global_justificationID, r.global_justificationID)) {
			this.global_justificationID = r.global_justificationID;
			this.setLIDstr(null);
		}
		if (!Util.equalStrings_null_or_not(this.global_organization_ID, r.global_organization_ID)) {
			this.setOrgGID(r.getOrgGID());
			long oID = D_Organization.getLIDbyGIDorGIDH(r.getOrgGID(), r.getOrgGIDH());
			if (oID <= 0) {
				oID = D_Organization.insertTemporaryGID(r.getOrgGID(), r.getOrgGIDH(), default_blocked_org, __peer);
				if (new_rq != null) new_rq.orgs.add(r.getGIDH());
			}
			this.setOrganizationLID(oID);
		}
		if (!Util.equalStrings_null_or_not(this.global_constituent_ID, r.global_constituent_ID)) {
			this.setConstituentGID(r.getConstituentGID());
			long cID = D_Constituent.getLIDFromGID_or_GIDH(r.getConstituentGID(), r.getConstituentGIDH(), getOrganizationLID());
			if (cID <= 0) {
				cID = D_Constituent.insertTemporaryGID(r.getConstituentGID(), r.getConstituentGIDH(), this.getOrganizationLID(), __peer, default_blocked_cons);
				if (new_rq != null) new_rq.cons.put(r.getConstituentGIDH(), DD.EMPTYDATE);
			}
			this.setConstituentLID(cID);
			//this.constituent = r.constituent;
		}
		if (!Util.equalStrings_null_or_not(this.global_motionID, r.global_motionID)) {
			this.setMotionGID(r.getMotionGID());
			long mID = D_Motion.getLIDFromGID(r.getMotionGID(), this.getOrganizationLID());
			if (mID <= 0) {
				mID = D_Motion.insertTemporaryGID(this.getMotionGID(), this.getOrganizationLID(), __peer, default_blocked_mot);
				if (new_rq != null) new_rq.moti.add(r.getMotionGID());
			}
			this.setMotionLID(mID);
		}
		if (!Util.equalStrings_null_or_not(this.global_answerTo_ID, r.global_answerTo_ID)) {
			this.setAnswerTo_GID(r.getAnswerTo_GID());
			this.setAnswerToLIDstr(D_Justification.getLIDstrFromGID(r.getAnswerTo_GID(), this.getOrganizationLID(), this.getMotionLID()));
			if (new_rq != null) new_rq.just.add(r.getAnswerTo_GID());
			//this.answerTo = r.answerTo;
		}
		sol_rq.just.add(this.getGID());
		this.dirty_main = true;
		if (this.peer_source_ID <= 0 && __peer != null)
			this.peer_source_ID = __peer.getLID_keep_force();
		this.setTemporary(false);
		return true;
	}

	public static long getCountForMotion(long motion_ID2) {
		String sql = "SELECT count(*) FROM "+table.justification.TNAME+" WHERE "+table.justification.motion_ID+"=?;";
		ArrayList<ArrayList<Object>> count;
		try {
			count = Application.db.select(sql, new String[]{Util.getStringID(motion_ID2)});
			long justifications = Integer.parseInt(count.get(0).get(0).toString());
			return justifications;
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	static final String sql_co = "SELECT count(*) FROM "+table.signature.TNAME+
		" WHERE "+table.signature.justification_ID+" = ? AND "+table.signature.choice+" = ?;";
	public long getVotersNb() {
		try {
			ArrayList<ArrayList<Object>> orgs = Application.db.select(sql_co, new String[]{this.getLIDstr(), "y"}, DEBUG);
			if(orgs.size()>0) return Util.lval(orgs.get(0).get(0));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	static final String sql_ac = "SELECT count(*) FROM "+table.signature.TNAME+" AS s "+
		" WHERE "+table.signature.justification_ID+" = ?;";
	static final String sql_ac2 = "SELECT count(*) FROM "+table.signature.TNAME+" AS s "+
	" WHERE s."+table.signature.justification_ID+" = ? AND s."+table.signature.arrival_date+">?;";
	public long getActivity(int days) {
		try {
			ArrayList<ArrayList<Object>> orgs;
			if (days == 0)
				orgs = Application.db.select(sql_ac, new String[]{this.getLIDstr()});
			else
				orgs = Application.db.select(sql_ac2, new String[]{this.getLIDstr(), Util.getGeneralizedDate(days)});
			
			if (orgs.size() > 0) return Util.lval(orgs.get(0).get(0));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	/**
	 * Sets serving both in peer.served_orgs and in organization.broadcasted
	 * has to sign the peer again because of served_orgs changes
	 * @return
	 */
	public boolean toggleServing() {
		D_Justification m = D_Justification.getJustByJust_Keep(this);
		m.setBroadcasted(!m.isBroadcasted());
		m.storeRequest();
		m.releaseReference();
		return m.isBroadcasted();
	}
	public Object getTitleOrMy() {
		String n = mydata.name;
		if (n != null) return n;
		return getJustificationTitle();
	}
	public Object getCategoryOrMy() {
		String n = mydata.category;
		if (n != null) return n;
		return null;//this.getCategory();
	}
	public Object getCreatorOrMy() {
		String n = mydata.creator;
		if (n != null) return n;
		if (this.constituent_ID != null) this.constituent = D_Constituent.getConstByLID(constituent_ID, true, false);
		if (this.constituent == null) return null;
		return this.constituent.getNameOrMy();
	}
	public void setNameMy(String _name) {
		this.mydata.name = _name;
		this.dirty_mydata = true;
	}
	public void setCreatorMy(String _creat) {
		this.mydata.creator = _creat;
		this.dirty_mydata = true;
	}
	public void setCategoryMy(String _cat) {
		this.mydata.category = _cat;
		this.dirty_mydata = true;
	}

	public Object getVotes() {
		return votes;
	}
	public void setVotes(Object v) {
		votes = v;
	}
	static final String sql_answer = 
		"SELECT "+table.justification.justification_title+
		","+table.justification.global_justification_ID+
		","+table.justification.justification_ID+
		" FROM "+table.justification.TNAME+
		" WHERE "+table.justification.motion_ID+"=?;";

	public static ArrayList<JustGIDItem> getAnswerToChoice(
			String motionID) 
		{
			ArrayList<ArrayList<Object>> j;
			ArrayList<JustGIDItem> r = new ArrayList<JustGIDItem>();
			try {
				j = Application.db.select(sql_answer, new String[]{motionID}, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return r;
			}
			for (ArrayList<Object> _j :j){
				String gid = Util.getString(_j.get(1));
				String id = Util.getString(_j.get(2));
				String name = Util.getString(_j.get(0));
				r.add(new JustGIDItem(gid, id, name));
			}
			return r;
	}
}

class D_Justification_SaverThread extends util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000; // ms to sleep
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	/**
	 * The next monitor is needed to ensure that two D_Justification_SaverThreadWorker are not concurrently modifying the database,
	 * and no thread is started when it is not needed (since one is already running).
	 */
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Justification_SaverThread() {
		super("D_Justification Saver", true);
		//start ();
	}
	public void _run() {
		for (;;) {
			if (stop) return;
			synchronized(saver_thread_monitor) {
				new D_Justification_SaverThreadWorker().start();
			}
			/*
			synchronized(saver_thread_monitor) {
				D_Justification de = D_Justification.need_saving_next();
				if (de != null) {
					if (DEBUG) System.out.println("D_Justification_Saver: loop saving "+de);
					ThreadsAccounting.ping("Saving");
					D_Peer.need_saving_remove(de.getGIDH(), de.instance);
					// try 3 times to save
					for (int k = 0; k < 3; k++) {
						try {
							de.storeAct();
							break;
						} catch (P2PDDSQLException e) {
							e.printStackTrace();
							synchronized(this){
								try {
									wait(SAVER_SLEEP_ON_ERROR);
								} catch (InterruptedException e2) {
									e2.printStackTrace();
								}
							}
						}
					}
				} else {
					ThreadsAccounting.ping("Nothing to do!");
					//System.out.println("D_Justification_Saver: idle ...");
				}
			}
			*/
			synchronized(this) {
				try {
					wait(SAVER_SLEEP);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

class D_Justification_SaverThreadWorker extends util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000;
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Justification_SaverThreadWorker() {
		super("D_Justification Saver Worker", false);
		//start ();
	}
	public void _run() {
		synchronized(D_Justification_SaverThread.saver_thread_monitor) {
			D_Justification de;
			boolean edited = true;
			if (DEBUG) System.out.println("D_Justification_Saver: start");
			
			 // first try objects being edited
			de = D_Justification.need_saving_obj_next();
			
			// then try remaining objects 
			if (de == null) { de = D_Justification.need_saving_next(); edited = false; }
			
			if (de != null) {
				if (DEBUG) System.out.println("D_Justification_Saver: loop saving "+de.getJustificationTitle());
				Application_GUI.ThreadsAccounting_ping("Saving");
				if (edited) D_Justification.need_saving_obj_remove(de);
				else D_Justification.need_saving_remove(de);//, de.instance);
				if (DEBUG) System.out.println("D_Justification_Saver: loop removed need_saving flag");
				// try 3 times to save
				for (int k = 0; k < 3; k++) {
					try {
						if (DEBUG) System.out.println("D_Justification_Saver: loop will try saving k="+k);
						de.storeAct();
						if (DEBUG) System.out.println("D_Justification_Saver: stored org:"+de.getJustificationTitle());
						break;
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
						synchronized(this) {
							try {
								if (DEBUG) System.out.println("D_Justification_Saver: sleep");
								wait(SAVER_SLEEP_ON_ERROR);
								if (DEBUG) System.out.println("D_Justification_Saver: waked error");
							} catch (InterruptedException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
			} else {
				Application_GUI.ThreadsAccounting_ping("Nothing to do!");
				if (DEBUG) System.out.println("D_Justification_Saver: idle ...");
			}
		}
		synchronized(this) {
			try {
				if (DEBUG) System.out.println("D_Justification_Saver: sleep");
				wait(SAVER_SLEEP);
				if (DEBUG) System.out.println("D_Justification_Saver: waked");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

