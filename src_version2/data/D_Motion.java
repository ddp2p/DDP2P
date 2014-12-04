package data;

import static java.lang.System.out;
import static util.Util.__;
import hds.ASNSyncPayload;
import hds.ClientSync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import ciphersuits.Cipher;
import ciphersuits.PK;
import ciphersuits.SK;
import config.Application;
import config.Application_GUI;
import config.DD;
import data.D_Justification.D_Justification_Node;
import data.D_Neighborhood.D_Neighborhood_Node;
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
D_MOTION ::= SEQUENCE {
	global_motionID PrintableString,
	motion_title Title_Document,
	motion_text Document
	constituent WB_Constituent
	date GeneralizedDate,
	signature OCTET_STRING
}
 */
public class D_Motion extends ASNObj implements  DDP2P_DoubleLinkedList_Node_Payload<D_Motion>, util.Summary {
	private static final String V0 = "V0";
	private static final String V1 = "V1";  // In this version we removed creation date
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final int DEFAULT_STATUS = 0;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	private static Object motion_GID_saving_lock = new Object(); // lock for storing a GID only once
	
	private String hash_alg = V1;
	private String global_motionID;//Printable
	private D_Document_Title motion_title = new D_Document_Title();
	private D_Document motion_text = new D_Document();
	private String global_constituent_ID;//Printable
	private String global_enhanced_motionID;//Printable
	private String global_organization_ID;//Printable, does not enter encoding
	
	private D_MotionChoice[] choices;
	private Calendar creation_date;
	private byte[] signature; //OCT STR
	
	private D_Constituent constituent;
	private D_Motion enhanced;
	private D_Organization organization;
	
	
	private String _motionLIDstr;
	private long _motionLID;
	private String constituent_ID;
	private String enhanced_motionID;
	private String organization_ID;
	
	private Calendar arrival_date;
	private Calendar preferences_date;
	private long peer_source_ID;
	private int status; // never used. set to 0 (could store number of votes known for it, or expiration mechanism)
	private boolean requested = false;
	private boolean blocked = false;
	private boolean broadcasted = D_Organization.DEFAULT_BROADCASTED_ORG;
	private boolean hidden = false;
	private boolean temporary = true;
	private String category;
	private int status_references = 0;
	
	boolean loaded_globals = false;
	boolean dirty_main = false;
	boolean dirty_mydata = false;
	boolean dirty_preferences = false;
	boolean dirty_local = false;
	boolean dirty_choices = false;
	
	private static Object monitor_object_factory = new Object();
	//static Object lock_organization_GID_storage = new Object(); // duplicate of the above
	public static int MAX_LOADED_OBJECTS = 10000;
	public static long MAX_OBJECTS_RAM = 10000000;
	private static final int MIN_OBJECTS = 2;
	D_Motion_Node component_node = new D_Motion_Node(null, null);
		
	public static D_Motion getEmpty() {return new D_Motion();}
	public D_Motion instance() throws CloneNotSupportedException{return new D_Motion();}

	private D_Motion() {}
	private D_Motion(Long lID, boolean load_Globals) throws P2PDDSQLException {
		try {
			init(lID);
			if (load_Globals) this.fillGlobals();
		} catch (Exception e) {
			//e.printStackTrace();
			throw new P2PDDSQLException(e.getLocalizedMessage());
		}
	}
	private D_Motion(String gID, boolean load_Globals, boolean create,
			D_Peer __peer, long p_oLID) throws P2PDDSQLException {
		try {
			init(gID);
			if (load_Globals) this.fillGlobals();
		} catch (Exception e) {
			//e.printStackTrace();
			if (create) {
				this.setGID(gID);
				this.setOrganizationLID(p_oLID);
				if (__peer != null) this.setPeerSourceLID((__peer.getLID()));
				this.dirty_main = true;
				this.setTemporary();
				return;
			}
			throw new P2PDDSQLException(e.getLocalizedMessage());
		}
	}
	/**
	 *  Motion.getOrganizationLID()
	 * @param _title
	 * @param _body
	 * @return
	 */
	public static D_Motion createMotion (String _title, String _body, long oLID) {
		D_Motion new_motion = D_Motion.getEmpty();
		
		D_Document_Title title = new D_Document_Title();
		title.title_document = new D_Document();
		title.title_document.setDocumentString(_title);
		title.title_document.setFormatString(D_Document.TXT_FORMAT);
		new_motion.setMotionTitle(title);
		
		if (_body != null) {
			D_Document body_d = new D_Document();
			body_d.setDocumentString(_body);
			body_d.setFormatString(D_Document.TXT_FORMAT);
			new_motion.setMotionText(body_d);
		}
		
		new_motion.setTemporary(false);
		new_motion.__setGID(new_motion.make_ID());
		//D_Motion.DEBUG = true;
		String GID = new_motion.getGID();
		
		D_Motion m = D_Motion.getMotiByGID(GID, true, true, true, null, oLID, new_motion);
		if (m != new_motion) {
			m.loadRemote(new_motion, null, null, null);
			new_motion = (m);
		}
		new_motion.setTemporary(false);
		
		new_motion.setBroadcasted(true); // if you sign it, you probably want to broadcast it...
		new_motion.setArrivalDate();
		long m_id = new_motion.storeRequest_getID();
		//new_motion.storeRequest();
		new_motion.releaseReference();
		return new_motion;
	}
	String sql_motions = "SELECT  "+Util.setDatabaseAlias(table.motion.fields, "n")
		+ ", p."+table.motion.global_motion_ID
		+ " FROM "+table.motion.TNAME+" AS n "
		+ " LEFT JOIN "+table.motion.TNAME+" AS p ON(n."+table.motion.enhances_ID+"=p."+table.motion.motion_ID+") "
		;
	String cond_ID = sql_motions+" WHERE n."+table.motion.motion_ID+"=?;";
	String cond_GID = sql_motions+" WHERE n."+table.motion.global_motion_ID+"=?;";

	private void init(Long lID) throws Exception {
		ArrayList<ArrayList<Object>> a;
		a = Application.db.select(cond_ID, new String[]{Util.getStringID(lID)});
		if (a.size() == 0) throw new Exception("D_Motion:init:None for lID="+lID);
		init (a.get(0));
		if(DEBUG) System.out.println("D_Motion: init: got="+this);//result);
	}
	private void init(String gID) throws Exception {
		ArrayList<ArrayList<Object>> a;
		a = Application.db.select(cond_GID, new String[]{gID});
		if (a.size() == 0) throw new Exception("D_Motion:init:None for GID="+gID);
		init (a.get(0));
		if(DEBUG) System.out.println("D_Motion: init: got="+this);//result);
	}
	
	void init(ArrayList<Object> o) throws P2PDDSQLException {
		hash_alg = Util.getString(o.get(table.motion.M_HASH_ALG));
		String _title_format = Util.getString(o.get(table.motion.M_TITLE_FORMAT));
		if (_title_format != null) {
			motion_title = new D_Document_Title();
			motion_title.title_document.setFormatString(_title_format);
		}
		
		String _motion_title = Util.getString(o.get(table.motion.M_TITLE));
		if (_motion_title != null) {
			if(motion_title == null) motion_title = new D_Document_Title();
			motion_title.title_document.setDocumentString(_motion_title);
		}
		
		String _text_format = Util.getString(o.get(table.motion.M_TEXT_FORMAT));
		if (_text_format != null) {
			motion_text = new D_Document();
			motion_text.setFormatString(_text_format);
		}
		
		String _text = Util.getString(o.get(table.motion.M_TEXT));
		if (_text != null) {
			if (motion_text == null) motion_text = new D_Document();
			motion_text.setDocumentString(_text);
		}
		
		status = Util.ival(Util.getString(o.get(table.motion.M_STATUS)), DEFAULT_STATUS);
		creation_date = Util.getCalendar(Util.getString(o.get(table.motion.M_CREATION)));
		arrival_date = Util.getCalendar(Util.getString(o.get(table.motion.M_ARRIVAL)));
		preferences_date = Util.getCalendar(Util.getString(o.get(table.motion.M_PREFERENCES_DATE)));
		signature = Util.byteSignatureFromString(Util.getString(o.get(table.motion.M_SIGNATURE)));
		this.setLIDstr(Util.getString(o.get(table.motion.M_MOTION_ID)));
		constituent_ID = Util.getString(o.get(table.motion.M_CONSTITUENT_ID));
		enhanced_motionID = Util.getString(o.get(table.motion.M_ENHANCED_ID));
		organization_ID = Util.getString(o.get(table.motion.M_ORG_ID));
		peer_source_ID = Util.lval(o.get(table.motion.M_PEER_SOURCE_ID));
		
		
		this.category = Util.getString(o.get(table.motion.M_CATEGORY));		
		this.blocked = Util.stringInt2bool(o.get(table.motion.M_BLOCKED),false);
		this.requested = Util.stringInt2bool(o.get(table.motion.M_REQUESTED),false);
		this.hidden = Util.stringInt2bool(o.get(table.motion.M_HIDDEN),false);
		this.temporary = Util.stringInt2bool(o.get(table.motion.M_TEMPORARY),false);
		this.broadcasted = Util.stringInt2bool(o.get(table.motion.M_BROADCASTED), D_Organization.DEFAULT_BROADCASTED_ORG_ITSELF);
		
		_setGID(Util.getString(o.get(table.motion.M_MOTION_GID)));
		global_constituent_ID = D_Constituent.getGIDFromLID(constituent_ID); //Util.getString(o.get(table.motion.M_FIELDS+0));
		global_enhanced_motionID = Util.getString(o.get(table.motion.M_FIELDS+0)); //+1
		global_organization_ID = D_Organization.getGIDbyLIDstr(organization_ID); //Util.getString(o.get(table.motion.M_FIELDS+2));
		
		
		String db_choices = Util.getString(o.get(table.motion.M_CHOICES));
		String[] s_choices = D_OrgConcepts.stringArrayFromString(db_choices);
		choices = D_MotionChoice.getChoices(s_choices);
		if (DEBUG) {
			System.out.println("D_Motion:init:db_choices="+db_choices);
			System.out.println("D_Motion:init: s_choices="+Util.concat(s_choices, ":"));
			System.out.println("D_Motion:init: choices="+Util.concat(choices, ":"));
		}
		D_MotionChoice[] _choices = D_MotionChoice.getChoices(getLIDstr());
		// System.out.println("D_Motion:init: _choices="+Util.concat(_choices, ":"));
		//if (_choices != null)
		choices = _choices;
			
		String my_const_sql = "SELECT "+table.my_motion_data.fields_list+
				" FROM "+table.my_motion_data.TNAME+
				" WHERE "+table.my_motion_data.motion_ID+" = ?;";
		ArrayList<ArrayList<Object>> my_org = Application.db.select(my_const_sql, new String[]{getLIDstr()}, DEBUG);
		if (my_org.size() != 0) {
			ArrayList<Object> my_data = my_org.get(0);
			mydata.name = Util.getString(my_data.get(table.my_motion_data.COL_NAME));
			mydata.category = Util.getString(my_data.get(table.my_motion_data.COL_CATEGORY));
			mydata.creator = Util.getString(my_data.get(table.my_motion_data.COL_CREATOR));
			// skipped preferences_date (used from main)
			mydata.row = Util.lval(my_data.get(table.my_motion_data.COL_ROW));
		}
			
		this.loaded_globals = true;
	}
	
	@Override
	public String toString() {
		return "D_Motion: "+
		"\n hash_alg="+getHashAlg()+
		"\n global_motionID="+getGID()+
		"\n motion_title="+Util.trimmed(getMotionTitle().toString())+
		"\n motion_text="+Util.trimmed(getMotionText().toString())+
		"\n constituent="+getConstituent()+
		"\n global_constituent_ID="+getConstituentGID()+
		"\n global_enhanced_motionID="+getEnhancedMotionGID()+
		"\n global_organization_ID="+getOrganizationGID_force()+
		"\n status="+getStatus()+
		"\n choices="+Util.concat(getChoices(), "::")+
		"\n creation_date="+Encoder.getGeneralizedTime(getCreationDate())+
		"\n signature="+Util.byteToHexDump(getSignature())+
		"\n motionID="+getLIDstr()+
		"\n constituent_ID="+getConstituentLIDstr()+
		"\n enhanced_motionID="+getEnhancedMotionLIDstr()+
		"\n organization_ID="+getOrganizationLIDstr();
	}
	@Override
	public String toSummaryString() {
		return "[D_Motion:] "+
				"\n motion_title="+Util.trimmed(getMotionTitle().toString())+
				"\n motion_text="+Util.trimmed(getMotionText().toString());
	}

	public static class D_Motion_Node {
		private static final int MAX_TRIES = 30;
		/** Currently loaded peers, ordered by the access time*/
		private static DDP2P_DoubleLinkedList<D_Motion> loaded_objects = new DDP2P_DoubleLinkedList<D_Motion>();
		private static Hashtable<Long, D_Motion> loaded_By_LocalID = new Hashtable<Long, D_Motion>();
		//private static Hashtable<String, D_Motion> loaded_const_By_GID = new Hashtable<String, D_Motion>();
		//private static Hashtable<String, D_Motion> loaded_const_By_GIDhash = new Hashtable<String, D_Motion>();
		private static Hashtable<String, Hashtable<Long, D_Motion>> loaded_const_By_GIDH_ORG = new Hashtable<String, Hashtable<Long, D_Motion>>();
		private static Hashtable<Long, Hashtable<String, D_Motion>> loaded_const_By_ORG_GIDH = new Hashtable<Long, Hashtable<String, D_Motion>>();
		private static Hashtable<String, Hashtable<Long, D_Motion>> loaded_const_By_GID_ORG = new Hashtable<String, Hashtable<Long, D_Motion>>();
		private static Hashtable<Long, Hashtable<String, D_Motion>> loaded_const_By_ORG_GID = new Hashtable<Long, Hashtable<String, D_Motion>>();
		private static long current_space = 0;
		
		//return loaded_const_By_GID.get(GID);
		private static D_Motion getConstByGID(String GID, Long organizationLID) {
			if (organizationLID != null && organizationLID > 0) {
				Hashtable<String, D_Motion> t1 = loaded_const_By_ORG_GID.get(organizationLID);
				if (t1 == null || t1.size() == 0) return null;
				return t1.get(GID);
			}
			Hashtable<Long, D_Motion> t2 = loaded_const_By_GID_ORG.get(GID);
			if ((t2 == null) || (t2.size() == 0)) return null;
			if ((organizationLID != null) && (organizationLID > 0))
				return t2.get(organizationLID);
			for (Long k : t2.keySet()) {
				return t2.get(k);
			}
			return null;
		}
		//return loaded_const_By_GID.put(GID, c);
		private static D_Motion putConstByGID(String GID, Long organizationLID, D_Motion c) {
			Hashtable<Long, D_Motion> v1 = loaded_const_By_GID_ORG.get(GID);
			if (v1 == null) loaded_const_By_GID_ORG.put(GID, v1 = new Hashtable<Long, D_Motion>());
			D_Motion result = v1.put(organizationLID, c);
			
			Hashtable<String, D_Motion> v2 = loaded_const_By_ORG_GID.get(organizationLID);
			if (v2 == null) loaded_const_By_ORG_GID.put(organizationLID, v2 = new Hashtable<String, D_Motion>());
			D_Motion result2 = v2.put(GID, c);
			
			if (result == null) result = result2;
			return result; 
		}
		//return loaded_const_By_GID.remove(GID);
		private static D_Motion remConstByGID(String GID, Long organizationLID) {
			D_Motion result = null;
			D_Motion result2 = null;
			Hashtable<Long, D_Motion> v1 = loaded_const_By_GID_ORG.get(GID);
			if (v1 != null) {
				result = v1.remove(organizationLID);
				if (v1.size() == 0) loaded_const_By_GID_ORG.remove(GID);
			}
			
			Hashtable<String, D_Motion> v2 = loaded_const_By_ORG_GID.get(organizationLID);
			if (v2 != null) {
				result2 = v2.remove(GID);
				if (v2.size() == 0) loaded_const_By_ORG_GID.remove(organizationLID);
			}
			
			if (result == null) result = result2;
			return result; 
		}
//		private static D_Motion getConstByGID(String GID, String organizationLID) {
//			return getConstByGID(GID, Util.Lval(organizationLID));
//		}
		
		private static D_Motion getConstByGIDH(String GIDH, Long organizationLID) {
			if (organizationLID != null && organizationLID > 0) {
				Hashtable<String, D_Motion> t1 = loaded_const_By_ORG_GIDH.get(organizationLID);
				if (t1 == null || t1.size() == 0) return null;
				return t1.get(GIDH);
			}
			Hashtable<Long, D_Motion> t2 = loaded_const_By_GIDH_ORG.get(GIDH);
			if ((t2 == null) || (t2.size() == 0)) return null;
			if ((organizationLID != null) && (organizationLID > 0))
				return t2.get(organizationLID);
			for (Long k : t2.keySet()) {
				return t2.get(k);
			}
			return null;
		}
		private static D_Motion putConstByGIDH(String GIDH, Long organizationLID, D_Motion c) {
			Hashtable<Long, D_Motion> v1 = loaded_const_By_GIDH_ORG.get(GIDH);
			if (v1 == null) loaded_const_By_GIDH_ORG.put(GIDH, v1 = new Hashtable<Long, D_Motion>());
			D_Motion result = v1.put(organizationLID, c);
			
			Hashtable<String, D_Motion> v2 = loaded_const_By_ORG_GIDH.get(organizationLID);
			if (v2 == null) loaded_const_By_ORG_GIDH.put(organizationLID, v2 = new Hashtable<String, D_Motion>());
			D_Motion result2 = v2.put(GIDH, c);
			
			if (result == null) result = result2;
			return result; 
		}
		//return loaded_const_By_GIDhash.remove(GIDH);
		private static D_Motion remConstByGIDH(String GIDH, Long organizationLID) {
			D_Motion result = null;
			D_Motion result2 = null;
			Hashtable<Long, D_Motion> v1 = loaded_const_By_GIDH_ORG.get(GIDH);
			if (v1 != null) {
				result = v1.remove(organizationLID);
				if (v1.size() == 0) loaded_const_By_GIDH_ORG.remove(GIDH);
			}
			
			Hashtable<String, D_Motion> v2 = loaded_const_By_ORG_GIDH.get(organizationLID);
			if (v2 != null) {
				result2 = v2.remove(GIDH);
				if (v2.size() == 0) loaded_const_By_ORG_GIDH.remove(organizationLID);
			}
			
			if (result == null) result = result2;
			return result; 
		}
//		private static D_Motion getConstByGIDH(String GIDH, String organizationLID) {
//			return getConstByGIDH(GIDH, Util.Lval(organizationLID));
//		}
		
		/** message is enough (no need to store the Encoder itself) */
		public byte[] message;
		public DDP2P_DoubleLinkedList_Node<D_Motion> my_node_in_loaded;
	
		public D_Motion_Node(byte[] message,
				DDP2P_DoubleLinkedList_Node<D_Motion> my_node_in_loaded) {
			this.message = message;
			this.my_node_in_loaded = my_node_in_loaded;
		}
		private static void register_fully_loaded(D_Motion crt) {
			assert((crt.component_node.message==null) && (crt.loaded_globals));
			if(crt.component_node.message != null) return;
			if(!crt.loaded_globals) return;
			if ((crt.getGID() != null) && (!crt.temporary)) {
				byte[] message = crt.encode();
				synchronized(loaded_objects) {
					crt.component_node.message = message; // crt.encoder.getBytes();
					if(crt.component_node.message != null) current_space += crt.component_node.message.length;
				}
			}
		}
		/*
		private static void unregister_loaded(D_Motion crt) {
			synchronized(loaded_orgs) {
				loaded_orgs.remove(crt);
				loaded_org_By_LocalID.remove(new Long(crt.getLID()));
				loaded_org_By_GID.remove(crt.getGID());
				loaded_org_By_GIDhash.remove(crt.getGIDH());
			}
		}
		*/
		private static void register_loaded(D_Motion crt) {
			if (DEBUG) System.out.println("D_Motion: register_loaded: start crt = "+crt);
			crt.reloadMessage(); 
			synchronized (loaded_objects) {
				String gid = crt.getGID();
				String gidh = crt.getGIDH();
				long lid = crt.getLID();
				Long organizationLID = crt.getOrganizationLID();
				
				loaded_objects.offerFirst(crt);
				if (lid > 0) {
					loaded_By_LocalID.put(new Long(lid), crt);
					if (DEBUG) System.out.println("D_Motion: register_loaded: store lid="+lid+" crt="+crt.getGIDH());
				}else{
					if (DEBUG) System.out.println("D_Motion: register_loaded: no store lid="+lid+" crt="+crt.getGIDH());
				}
				if (gid != null) {
					D_Motion_Node.putConstByGID(gid, crt.getOrganizationLID(), crt); //loaded_const_By_GID.put(gid, crt);
					if (DEBUG) System.out.println("D_Motion: register_loaded: store gid="+gid);
				} else {
					if (DEBUG) System.out.println("D_Motion: register_loaded: no store gid="+gid);
				}
				if (gidh != null) {
					D_Motion_Node.putConstByGIDH(gidh, organizationLID, crt);//loaded_const_By_GIDhash.put(gidh, crt);
					if (DEBUG) System.out.println("D_Motion: register_loaded: store gidh="+gidh);
				} else {
					if (DEBUG) System.out.println("D_Motion: register_loaded: no store gidh="+gidh);
				}
				if (crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				int tries = 0;
				while ((loaded_objects.size() > MAX_LOADED_OBJECTS)
						|| (current_space > MAX_OBJECTS_RAM)) {
					if (loaded_objects.size() <= MIN_OBJECTS) break; // at least _crt_peer and _myself
	
					if (tries > MAX_TRIES) break;
					tries ++;
					D_Motion candidate = loaded_objects.getTail();
					if ((candidate.status_references > 0)
							//||
							//D_Motion.is_crt_const(candidate)
							//||
							//(candidate == HandlingMyself_Peer.get_myself())
							) 
					{
						setRecent(candidate);
						continue;
					}
					
					D_Motion removed = loaded_objects.removeTail();//remove(loaded_peers.size()-1);
					loaded_By_LocalID.remove(new Long(removed.getLID())); 
					D_Motion_Node.remConstByGID(removed.getGID(), removed.getOrganizationLID());//loaded_const_By_GID.remove(removed.getGID());
					D_Motion_Node.remConstByGIDH(removed.getGIDH(), removed.getOrganizationLID()); //loaded_const_By_GIDhash.remove(removed.getGIDH());
					if (DEBUG) System.out.println("D_Motion: register_loaded: remove GIDH="+removed.getGIDH());
					if (removed.component_node.message != null) current_space -= removed.component_node.message.length;				
				}
			}
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_Motion crt) {
			loaded_objects.moveToFront(crt);
		}
		public static boolean dropLoaded(D_Motion removed, boolean force) {
			boolean result = true;
			synchronized(loaded_objects) {
				if (removed.status_references > 0 || removed.get_DDP2P_DoubleLinkedList_Node() == null)
					result = false;
				if (! force && ! result) {
					System.out.println("D_Motion: dropLoaded: abandon: force="+force+" rem="+removed);
					return result;
				}
				
				if (loaded_objects.inListProbably(removed)) {
					try {
						loaded_objects.remove(removed);
						if (removed.component_node.message != null) current_space -= removed.component_node.message.length;	
						if (DEBUG) System.out.println("D_Motion: dropLoaded: exit with force="+force+" result="+result);
					} catch (Exception e) {
						if (_DEBUG) e.printStackTrace();
					}
				}
				if (removed.getLIDstr() != null) loaded_By_LocalID.remove(new Long(removed.getLID())); 
				if (removed.getGID() != null) D_Motion_Node.remConstByGID(removed.getGID(), removed.getLID()); //loaded_const_By_GID.remove(removed.getGID());
				if (removed.getGIDH() != null) D_Motion_Node.remConstByGIDH(removed.getGIDH(), removed.getLID()); //loaded_const_By_GIDhash.remove(removed.getGIDH());
				if (DEBUG) System.out.println("D_Motion: drop_loaded: remove GIDH="+removed.getGIDH());
				return result;
			}
		}
	}
	static class D_Motion_My {
		String name;// table.my_organization_data.
		String creator;
		String category;
		long row;
	}
	D_Motion_My mydata = new D_Motion_My();	
	


	/**
	 * exception raised on error
	 * @param ID
	 * @param load_Globals 
	 * @return
	 */
	static public D_Motion getMotiByLID_AttemptCacheOnly (long ID, boolean load_Globals) {
		if (ID <= 0) return null;
		Long id = new Long(ID);
		D_Motion crt = D_Motion_Node.loaded_By_LocalID.get(id);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals) {
			crt.fillGlobals();
			D_Motion_Node.register_fully_loaded(crt);
		}
		D_Motion_Node.setRecent(crt);
		return crt;
	}
	/**
	 * exception raised on error
	 * @param LID
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static public D_Motion getMotiByLID_AttemptCacheOnly(Long LID, boolean load_Globals, boolean keep) {
		if (LID == null) return null;
		if (keep) {
			synchronized (monitor_object_factory) {
				D_Motion  crt = getMotiByLID_AttemptCacheOnly(LID.longValue(), load_Globals);
				if (crt != null) {			
					crt.status_references ++;
					if (crt.status_references > 1) {
						System.out.println("D_Motion: getMotiByGIDhash_AttemptCacheOnly: "+crt.status_references);
						Util.printCallPath("");
					}
				}
				return crt;
			}
		} else {
			return getMotiByLID_AttemptCacheOnly(LID.longValue(), load_Globals);
		}
	}
	/**
	 * This is Calling the version with "long LID".
	 * @param LID
	 * @param load_Globals
	 * @param keep
	 * @return
	 */
	static public D_Motion getMotiByLID(String LID, boolean load_Globals, boolean keep) {
		return getMotiByLID(Util.Lval(LID), load_Globals, keep);
	}
	/**
	 * 
	 * @param LID
	 * @param load_Globals
	 * @param keep
	 * @return
	 */
	static public D_Motion getMotiByLID(Long LID, boolean load_Globals, boolean keep) {
		// boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Motion: getMotiByLID: "+LID+" glob="+load_Globals);
		if ((LID == null) || (LID <= 0)) {
			if (DEBUG) System.out.println("D_Motion: getMotiByLID: null LID = "+LID);
			if (DEBUG) Util.printCallPath("?");
			return null;
		}
		D_Motion crt = D_Motion.getMotiByLID_AttemptCacheOnly(LID, load_Globals, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Motion: getMotiByLID: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_Motion.getMotiByLID_AttemptCacheOnly(LID, load_Globals, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_Motion: getMotiByLID: got sync cached crt="+crt);
				return crt;
			}

			try {
				crt = new D_Motion(LID, load_Globals);
				if (DEBUG) System.out.println("D_Motion: getMotiByLID: loaded crt="+crt);
				D_Motion_Node.register_loaded(crt);
				if (keep) {
					crt.status_references ++;
				}
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Motion: getMotiByLID: error loading");
				// e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_Motion: getMotiByLID: Done");
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
	static private D_Motion getMotiByGID_AttemptCacheOnly(String GID, Long organizationLID, boolean load_Globals) {
		D_Motion  crt = null;
		if ((GID == null)) return null;
		if ((GID != null)) crt = D_Motion_Node.getConstByGID(GID, organizationLID); //.loaded_const_By_GID.get(GID);
		
				
		if (crt != null) {
			//crt.setGID(GID, crt.getOrganizationID());
			
			if (load_Globals && !crt.loaded_globals) {
				crt.fillGlobals();
				D_Motion_Node.register_fully_loaded(crt);
			}
			D_Motion_Node.setRecent(crt);
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
	static private D_Motion getOrgByGIDhash_AttemptCacheOnly(String GIDhash, boolean load_Globals, boolean keep) {
		if (GIDhash == null) return null;
		if (keep) {
			synchronized(monitor_object_factory) {
				D_Motion  crt = getOrgByGID_or_GIDhash_AttemptCacheOnly(null, GIDhash, load_Globals);
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
	static private D_Motion getMotiByGID_AttemptCacheOnly(String GID, Long oID, boolean load_Globals, boolean keep) {
		if ((GID == null)) return null;
		if (keep) {
			synchronized (monitor_object_factory) {
				D_Motion  crt = getMotiByGID_AttemptCacheOnly(GID, oID, load_Globals);
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
			return getMotiByGID_AttemptCacheOnly(GID, oID, load_Globals);
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
	static private D_Motion getOrgByGID_only_AttemptCacheOnly(String GID, boolean load_Globals) {
		if (GID == null) return null;
		D_Motion crt = D_Motion_Node.loaded_org_By_GID.get(GID);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals){
			crt.fillGlobals();
			D_Motion_Node.register_fully_loaded(crt);
		}
		D_Motion_Node.setRecent(crt);
		return crt;
	}
	*/
	/**
	 * When not knowing the organization
	 * @param GID
	 * @param load_Globals
	 * @param keep
	 * @return
	 */
	@Deprecated
	static public D_Motion getMotiByGID(String GID, boolean load_Globals, boolean keep) {
		System.out.println("Remove me setting orgID");
		return getMotiByGID(GID, load_Globals, false, keep, null, -1, null);
	}
	/**
	 * Without creation
	 * @param GID
	 * @param load_Globals
	 * @param keep
	 * @param oID
	 * @return
	 */
	static public D_Motion getMotiByGID(String GID, boolean load_Globals, boolean keep, Long oID) {
		return getMotiByGID(GID, load_Globals, false, keep, null, oID, null);
	}
	/**
	 * Does not call storeRequest on creation.
	 * Therefore If create, should also set keep!
	 * 
	 * @param GID
	 * @param load_Globals
	 * @param create
	 * @param keep
	 * @param __peer
	 * @param p_oLID
	 * @param storage
	 * @return
	 */
	static public D_Motion getMotiByGID(String GID, boolean load_Globals, boolean create, boolean keep, D_Peer __peer, long p_oLID, D_Motion storage) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Motion: getMotiByGID: "+GID+" glob="+load_Globals+" cre="+create+" _peer="+__peer);
		if ((GID == null)) {
			if (_DEBUG) System.out.println("D_Motion: getMotiByGID: null GID and GIDH");
			return null;
		}
		if (create && ! keep) {
			if (_DEBUG) System.out.println("D_Motion: getMotiByGID: keeping by force");
			Util.printCallPath("");
			keep = true;
		}
		D_Motion crt = D_Motion.getMotiByGID_AttemptCacheOnly(GID, p_oLID, load_Globals, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Motion: getMotiByGID: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_Motion.getMotiByGID_AttemptCacheOnly(GID, p_oLID, load_Globals, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_Motion: getMotiByGID: got sync cached crt="+crt);
				return crt;
			}

			try {
				if (storage == null)
					crt = new D_Motion(GID, load_Globals, create, __peer, p_oLID);
				else {
					D_Motion_Node.dropLoaded(storage, true);
					crt = storage.initNew(GID, __peer, p_oLID);
				}
				if (DEBUG) System.out.println("D_Motion: getMotiByGID: loaded crt="+crt);
				if (keep) {
					crt.status_references ++;
				}
				D_Motion_Node.register_loaded(crt);
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Motion: getMotiByGID: error loading");
				if (DEBUG) e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_Motion: getMotiByGID: Done");
			return crt;
		}
	}	
	/**
	 * Usable until calling releaseReference()
	 * Verify with assertReferenced()
	 * @param peer
	 * @return
	 */
	static public D_Motion getMotiByMoti_Keep(D_Motion neigh) {
		//System.out.println("D_Organization: getOrgByOrg_Keep: start " + neigh);
		if (neigh == null) return null;
		D_Motion result = null;
		String GID = neigh.getGID();
		if (GID != null)
			result = D_Motion.getMotiByGID(GID, true, true, neigh.getOrganizationLID());
		if (result == null) {
			long LID = neigh.getLID();
			//System.out.println("D_Organization: getOrgByOrg_Keep: LID == "+LID);
			result = D_Motion.getMotiByLID(LID, true, true);
			//System.out.println("D_Organization: getOrgByOrg_Keep: LID locked");
		}
		if (result == null) {
			if ((neigh.getLIDstr() == null) && (neigh.getGIDH() == null)) {
				result = neigh;
				{
					neigh.status_references ++;
					System.out.println("D_Organization: getOrgByOrg_Keep: "+neigh.status_references);
					Util.printCallPath("");
				}
			} else {
				//System.out.println("D_Organization: getOrgByOrg_Keep: not null all == " + neigh);
			}
		}
		if (result == null) {
			System.out.println("D_Organization: getOrgByOrg_Keep: got null for "+neigh);
		}
		return result;
	}
	private D_Motion initNew(String gID, D_Peer __peer, long p_oLID) 
	{
		if (this.global_motionID != null)
			assert(this.getGID().equals(gID));
		this.setOrganizationLID(p_oLID);
		this._setGID(gID);
		if (__peer != null) this.setPeerSourceLID((__peer.getLID()));
		this.dirty_main = true;
		return this;
	}
	
	public static long getMotiLIDByNameAndParent(String name, long parent_nID, long organizationID) {
		ArrayList<ArrayList<Object>> sel;
		try {
			if (parent_nID <= 0)
				sel = Application.db.select("select  "+table.motion.fields+
						" from "+table.motion.TNAME+
						" where "+table.motion.organization_ID+" = ? and ( "+table.motion.enhances_ID+" ISNULL OR "+table.motion.enhances_ID+" < 0 ) and "+table.motion.motion_title+" = ?;",
						new String[]{Util.getStringID(organizationID), name});
			else sel = Application.db.select("select " + table.motion.fields +
					" from "+table.motion.TNAME+" where "+table.motion.organization_ID+" = ? and "+table.motion.enhances_ID+" = ? and "+table.motion.motion_title+" = ?;",
					new String[]{Util.getStringID(organizationID), Util.getStringID(parent_nID), name});
			if (sel.size() != 0)
				return Util.lval(sel.get(0).get(table.motion.M_MOTION_ID));
		} catch(Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public static int getMotiCount(long oID) {
		   String sql = "SELECT count(*) FROM "+table.motion.TNAME+" where "+table.motion.organization_ID+"=?;";
		   ArrayList<ArrayList<Object>> count;
		try {
			count = Application.db.select(sql, new String[]{Util.getString(oID)});
			   int nbrs = Integer.parseInt(Util.getString(count.get(0).get(0)));
			   return nbrs;
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	private void fillGlobals() {
		if (this.loaded_globals) return;
		
		if((this.getEnhancedMotionLIDstr() != null ) && (this.getEnhancedMotionGID_direct() == null))
			this.setEnhancedMotionGID(D_Motion.getGIDFromLID(this.getEnhancedMotionLIDstr()));
		
		if((this.getOrganizationLIDstr() != null ) && (this.getOrganizationGID_direct() == null))
			this.setOrganizationGID(D_Organization.getGIDbyLIDstr(this.getOrganizationLIDstr()));

		//if((this.motionID != null ) && (this.global_motionID == null)) this.global_motionID = D_Motion.getMotionGlobalID(this.motionID);
		
		if((this.getConstituentLIDstr() != null ) && (this.getConstituentGID_direct() == null))
			this.setConstituentGID(D_Constituent.getGIDFromLID(this.getConstituentLIDstr()));
		
		this.loaded_globals = true;
	}
	public static String getGIDFromLID(String nlID) {
		return getGIDFromLID(Util.lval(nlID));
	}
	public static String getGIDFromLID(long n_lID) {
		if (n_lID <= 0) return null;
		Long LID = new Long(n_lID);
		D_Motion c = D_Motion.getMotiByLID(LID, true, false);
		if (c == null) {
			if (_DEBUG) System.out.println("D_Motion: getGIDFromLID: null for nLID = "+n_lID);
			for (Long l : D_Motion_Node.loaded_By_LocalID.keySet()) {
				if (_DEBUG) System.out.println("D_Motion: getGIDFromLID: available ["+l+"]"+D_Motion_Node.loaded_By_LocalID.get(l).getGIDH());
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
	public static long getLIDFromGID(String GID2, Long oID) {
		if (GID2 == null) return -1;
		D_Motion c = D_Motion.getMotiByGID(GID2, true, false, oID);
		if (c == null) return -1;
		return c.getLID();
	}
	public static String getLIDstrFromGID(String GID2, Long oID) {
		if (GID2 == null) return null;
		D_Motion c = D_Motion.getMotiByGID(GID2, true, false, oID);
		if (c == null) return null;
		return c.getLIDstr();
	}
	/**
	 * Inserts a temporary one if needed
	 * @param GID2
	 * @param oID
	 * @param __peer_first_sending_this
	 * @return
	 */
	public static String getLIDstrFromGID_or_Tmp(String GID2, Long oID, D_Peer __peer_first_sending_this) {
		if (GID2 == null) return null;
		D_Motion c = D_Motion.getMotiByGID(GID2, true, true, false, __peer_first_sending_this, oID, null);
		return c.getLIDstr();
	}
	public static String getLIDstrFromGID_or_GIDH(
			String GID2, String GIDH2, Long oID) {
		if (GID2 == null) return null;
		D_Motion c = D_Motion.getMotiByGID(GID2, true, false, oID);
		return c.getLIDstr();
	}
	public static long getLIDFromGID_or_GIDH(
			String GID2, String GIDH2, Long oID) {
		if (GID2 == null) return -1;
		D_Motion c = D_Motion.getMotiByGID(GID2, true, false, oID);
		return c.getLID();
	}
	
	
	/**
	 * Load this message in the storage cache node.
	 * Should be called each time I load a node and when I sign myself.
	 */
	private void reloadMessage() {
		if (this.loaded_globals) this.component_node.message = this.encode(); //crt.encoder.getBytes();
	}
	
	/**
	 * Storage Methods
	 */
	
	public void releaseReference() {
		if (status_references <= 0) Util.printCallPath("Null reference already!");
		else status_references --;
		//System.out.println("D_Motion: releaseReference: "+status_references);
		//Util.printCallPath("");
	}
	
	public void assertReferenced() {
		assert (status_references > 0);
	}
	
	/**
	 * The entries that need saving
	 */
	private static HashSet<D_Motion> _need_saving = new HashSet<D_Motion>();
	private static HashSet<D_Motion> _need_saving_obj = new HashSet<D_Motion>();
	
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Motion> 
	set_DDP2P_DoubleLinkedList_Node(DDP2P_DoubleLinkedList_Node<D_Motion> node) {
		DDP2P_DoubleLinkedList_Node<D_Motion> old = this.component_node.my_node_in_loaded;
		if (DEBUG) System.out.println("D_Motion: set_DDP2P_DoubleLinkedList_Node: set = "+ node);
		this.component_node.my_node_in_loaded = node;
		return old;
	}
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Motion> 
	get_DDP2P_DoubleLinkedList_Node() {
		if (DEBUG) System.out.println("D_Motion: get_DDP2P_DoubleLinkedList_Node: get");
		return component_node.my_node_in_loaded;
	}
	static void need_saving_remove(D_Motion c) {
		if (DEBUG) System.out.println("D_Motion: need_saving_remove: remove "+c);
		_need_saving.remove(c);
		if (DEBUG) dumpNeedsObj(_need_saving);
	}
	static void need_saving_obj_remove(D_Motion org) {
		if (DEBUG) System.out.println("D_Motion: need_saving_obj_remove: remove "+org);
		_need_saving_obj.remove(org);
		if (DEBUG) dumpNeedsObj(_need_saving_obj);
	}
	static D_Motion need_saving_next() {
		Iterator<D_Motion> i = _need_saving.iterator();
		if (!i.hasNext()) return null;
		D_Motion c = i.next();
		if (DEBUG) System.out.println("D_Motion: need_saving_next: next: "+c);
		//D_Motion r = D_Motion_Node.getConstByGIDH(c, null);//.loaded_const_By_GIDhash.get(c);
		if (c == null) {
			if (_DEBUG) {
				System.out.println("D_Motion Cache: need_saving_next null entry "
						+ "needs saving next: "+c);
				System.out.println("D_Motion Cache: "+dumpDirCache());
			}
			return null;
		}
		return c;
	}
	static D_Motion need_saving_obj_next() {
		Iterator<D_Motion> i = _need_saving_obj.iterator();
		if (!i.hasNext()) return null;
		D_Motion r = i.next();
		if (DEBUG) System.out.println("D_Motion: need_saving_obj_next: next: "+r);
		//D_Motion r = D_Motion_Node.loaded_org_By_GIDhash.get(c);
		if (r == null) {
			if (_DEBUG) {
				System.out.println("D_Motion Cache: need_saving_obj_next null entry "
						+ "needs saving obj next: "+r);
				System.out.println("D_Motion Cache: "+dumpDirCache());
			}
			return null;
		}
		return r;
	}
	private static void dumpNeeds(HashSet<String> _need_saving2) {
		System.out.println("D_Motion: Needs:");
		for ( String i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	private static void dumpNeedsObj(HashSet<D_Motion> _need_saving2) {
		System.out.println("Needs:");
		for ( D_Motion i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	public static String dumpDirCache() {
		String s = "[";
		s += D_Motion_Node.loaded_objects.toString();
		s += "]";
		return s;
	}
		
	public Encoder getSignableEncoder() {
		if (V0.equals(hash_alg)) return getSignableEncoder_V0();
		if (V1.equals(hash_alg)) return getSignableEncoder_V1();
		return getSignableEncoder_V1();
	}
	public Encoder getSignableEncoder_V0() {
		Encoder enc = new Encoder().initSequence();
		if (hash_alg != null) enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if (getGID() != null) enc.addToSequence(new Encoder(getGID() ,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if (motion_title != null) enc.addToSequence(motion_title.getEncoder().setASN1Type(DD.TAG_AC2));
		if (motion_text != null) enc.addToSequence(motion_text.getEncoder().setASN1Type(DD.TAG_AC3));
		if (category!=null)enc.addToSequence(new Encoder(category).setASN1Type(DD.TAG_AC11));
		if (global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC5));
		if (global_enhanced_motionID!=null)enc.addToSequence(new Encoder(global_enhanced_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		if (global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC7));
		if (creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC8));
		if (choices!=null && choices.length>0)enc.addToSequence(Encoder.getEncoder(choices).setASN1Type(DD.TAG_AC10));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC9));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC4));
		return enc;
	}
	public Encoder getSignableEncoder_V1() {
		Encoder enc = new Encoder().initSequence();
		if (hash_alg != null) enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if (getGID() != null) enc.addToSequence(new Encoder(getGID() ,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if (motion_title != null) enc.addToSequence(motion_title.getEncoder().setASN1Type(DD.TAG_AC2));
		if (motion_text != null) enc.addToSequence(motion_text.getEncoder().setASN1Type(DD.TAG_AC3));
		if (category!=null)enc.addToSequence(new Encoder(category).setASN1Type(DD.TAG_AC11));
		if (global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC5));
		if (global_enhanced_motionID!=null)enc.addToSequence(new Encoder(global_enhanced_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		if (global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC7));
		//if (creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC8));
		if (choices!=null && choices.length>0)enc.addToSequence(Encoder.getEncoder(choices).setASN1Type(DD.TAG_AC10));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC9));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC4));
		return enc;
	}
	public Encoder getHashEncoder() {
		if (V0.equals(hash_alg)) return getHashEncoder_V0();
		if (V1.equals(hash_alg)) return getHashEncoder_V1();
		return getHashEncoder_V1();
	}
	public Encoder getHashEncoder_V0() {
		Encoder enc = new Encoder().initSequence();
		//if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		//if(global_motionID!=null)enc.addToSequence(new Encoder(global_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(motion_title!=null)enc.addToSequence(motion_title.getEncoder().setASN1Type(DD.TAG_AC2));
		//System.out.println("mti=\""+motion_title+"\"\n" + Util.stringSignatureFromByte(enc.getBytes()));
		if(motion_text!=null)enc.addToSequence(motion_text.getEncoder().setASN1Type(DD.TAG_AC3));
		//System.out.println("mtx=\""+motion_text+"\"\n" + Util.stringSignatureFromByte(enc.getBytes()));
		if(category!=null)enc.addToSequence(new Encoder(category).setASN1Type(DD.TAG_AC11));
		//System.out.println("cat=\""+category+"\"\n" + Util.stringSignatureFromByte(enc.getBytes()));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC5));
		//System.out.println("cGID="+global_constituent_ID+"\n" + Util.stringSignatureFromByte(enc.getBytes()));
		if(global_enhanced_motionID!=null)enc.addToSequence(new Encoder(global_enhanced_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		//System.out.println("eGID="+global_enhanced_motionID+"\n" + Util.stringSignatureFromByte(enc.getBytes()));
		if(global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC7));
		//System.out.println("oGID="+global_organization_ID+"\n" + Util.stringSignatureFromByte(enc.getBytes()));
		if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC8));
		//System.out.println("crea="+creation_date+"\n" + Util.stringSignatureFromByte(enc.getBytes()));
		if(choices!=null && choices.length>0)enc.addToSequence(Encoder.getEncoder(choices).setASN1Type(DD.TAG_AC10));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC9));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC4));
		//System.out.println("D_Motion:getHash: this_0="+this);
		return enc;
	}
	public Encoder getHashEncoder_V1() {
		Encoder enc = new Encoder().initSequence();
		if(motion_title!=null)enc.addToSequence(motion_title.getEncoder().setASN1Type(DD.TAG_AC2));
		if(motion_text!=null)enc.addToSequence(motion_text.getEncoder().setASN1Type(DD.TAG_AC3));
		if(category!=null)enc.addToSequence(new Encoder(category).setASN1Type(DD.TAG_AC11));
		if(global_constituent_ID!=null)enc.addToSequence(new Encoder(global_constituent_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC5));
		if(global_enhanced_motionID!=null)enc.addToSequence(new Encoder(global_enhanced_motionID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		if(global_organization_ID!=null)enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC7));
		//if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC8));
		if(choices!=null && choices.length>0)enc.addToSequence(Encoder.getEncoder(choices).setASN1Type(DD.TAG_AC10));
		//System.out.println("D_Motion:getHash: this_1="+this);
		return enc;
	}
	
	@Override
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	/**
	 * parameter used for dictionaries
	 */
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) {
		return getEncoder(dictionary_GIDs, 0);
	}
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs, int dependants) {
		if (V0.equals(hash_alg)) return getEncoder_V0(dictionary_GIDs, dependants);
		if (V1.equals(hash_alg)) return getEncoder_V1(dictionary_GIDs, dependants);
		return getEncoder_V1(dictionary_GIDs, dependants);
	}
	public Encoder getEncoder_V0(ArrayList<String> dictionary_GIDs, int dependants) {
		int new_dependants = dependants;
		if (dependants > 0) new_dependants = dependants - 1;

		Encoder enc = new Encoder().initSequence();
		if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(getGID()!=null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		}
		if(motion_title!=null)enc.addToSequence(motion_title.getEncoder().setASN1Type(DD.TAG_AC2));
		if(motion_text!=null)enc.addToSequence(motion_text.getEncoder().setASN1Type(DD.TAG_AC3));
		if (global_constituent_ID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_constituent_ID);
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		}
		if (global_enhanced_motionID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_enhanced_motionID);
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC5));
		}
		/**
		 * May decide to comment encoding of "global_organization_ID" out completely, since the org_GID is typically
		 * available at the destination from enclosing fields, and will be filled out at explansion
		 * by ASNSyncPayload.expand at decoding.
		 * However, it is not that damaging when using compression, and can be stored without much overhead.
		 * So it is left here for now.  Test if you comment out!
		 */
		if (global_organization_ID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_organization_ID);
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		}
		if (creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC7));
		if (signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC8));
		if (choices!=null)enc.addToSequence(Encoder.getEncoder(choices).setASN1Type(DD.TAG_AC9));
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (constituent!=null)enc.addToSequence(constituent.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC10));
			if (enhanced!=null)enc.addToSequence(enhanced.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC11));
			if (organization!=null)enc.addToSequence(organization.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC12));
		}
		if (category != null) enc.addToSequence(new Encoder(category).setASN1Type(DD.TAG_AC13));
		
		enc.setASN1Type(getASN1Type());
		return enc;
	}
	public Encoder getEncoder_V1(ArrayList<String> dictionary_GIDs, int dependants) {
		int new_dependants = dependants;
		if (dependants > 0) new_dependants = dependants - 1;

		Encoder enc = new Encoder().initSequence();
		if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(getGID()!=null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		}
		if(motion_title!=null)enc.addToSequence(motion_title.getEncoder().setASN1Type(DD.TAG_AC2));
		if(motion_text!=null)enc.addToSequence(motion_text.getEncoder().setASN1Type(DD.TAG_AC3));
		if (global_constituent_ID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_constituent_ID);
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		}
		if (global_enhanced_motionID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_enhanced_motionID);
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC5));
		}
		/**
		 * May decide to comment encoding of "global_organization_ID" out completely, since the org_GID is typically
		 * available at the destination from enclosing fields, and will be filled out at explansion
		 * by ASNSyncPayload.expand at decoding.
		 * However, it is not that damaging when using compression, and can be stored without much overhead.
		 * So it is left here for now.  Test if you comment out!
		 */
		if (global_organization_ID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_organization_ID);
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		}
		//if (creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC7));
		if (signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC8));
		if (choices!=null)enc.addToSequence(Encoder.getEncoder(choices).setASN1Type(DD.TAG_AC9));
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (constituent!=null)enc.addToSequence(constituent.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC10));
			if (enhanced!=null)enc.addToSequence(enhanced.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC11));
			if (organization!=null)enc.addToSequence(organization.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC12));
		}
		if (category != null) enc.addToSequence(new Encoder(category).setASN1Type(DD.TAG_AC13));
		
		enc.setASN1Type(getASN1Type());
		return enc;
	}
	public static byte getASN1Type() {
		return TAG;
	}

	@Override
	public D_Motion decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0)hash_alg = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if (V0.equals(hash_alg)) return decode_V0(dec);
		if (V1.equals(hash_alg)) return decode_V1(dec);
		return decode_V1(dec);
	}
	public D_Motion decode_V0(Decoder dec) throws ASN1DecoderFail {
		if(dec.getTypeByte()==DD.TAG_AC1) __setGID(dec.getFirstObject(true).getString(DD.TAG_AC1));
		if(dec.getTypeByte()==DD.TAG_AC2)motion_title = new D_Document_Title().decode(dec.getFirstObject(true));	
		//System.out.println("D_Motion:decode: title="+motion_title);
		if(dec.getTypeByte()==DD.TAG_AC3)motion_text = new D_Document().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC4)global_constituent_ID = dec.getFirstObject(true).getString(DD.TAG_AC4);
		if(dec.getTypeByte()==DD.TAG_AC5)global_enhanced_motionID = dec.getFirstObject(true).getString(DD.TAG_AC5);
		if(dec.getTypeByte()==DD.TAG_AC6)global_organization_ID = dec.getFirstObject(true).getString(DD.TAG_AC6);
		if(dec.getTypeByte()==DD.TAG_AC7)creation_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC7);
		//else System.out.println("D_Motion:decode: no creation date");
		if(dec.getTypeByte()==DD.TAG_AC8)signature = dec.getFirstObject(true).getBytes(DD.TAG_AC8);
		//else System.out.println("D_Motion:decode: no signature");
		if(dec.getTypeByte()==DD.TAG_AC9)choices = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_SEQUENCE, new D_MotionChoice[0], new D_MotionChoice());	
		//else System.out.println("D_Motion:decode: no choices");
		if(dec.getTypeByte()==DD.TAG_AC10)constituent = D_Constituent.getEmpty().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC11)enhanced = new D_Motion().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC12)organization = D_Organization.getOrgFromDecoder(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC13)category = dec.getFirstObject(true).getString(DD.TAG_AC13);
		
		this.setTemporary(false);
		return this;
	}
	public D_Motion decode_V1(Decoder dec) throws ASN1DecoderFail {
		if(dec.getTypeByte()==DD.TAG_AC1) __setGID(dec.getFirstObject(true).getString(DD.TAG_AC1));
		if(dec.getTypeByte()==DD.TAG_AC2)motion_title = new D_Document_Title().decode(dec.getFirstObject(true));	
		//System.out.println("D_Motion:decode: title="+motion_title);
		if(dec.getTypeByte()==DD.TAG_AC3)motion_text = new D_Document().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC4)global_constituent_ID = dec.getFirstObject(true).getString(DD.TAG_AC4);
		if(dec.getTypeByte()==DD.TAG_AC5)global_enhanced_motionID = dec.getFirstObject(true).getString(DD.TAG_AC5);
		if(dec.getTypeByte()==DD.TAG_AC6)global_organization_ID = dec.getFirstObject(true).getString(DD.TAG_AC6);
		//if(dec.getTypeByte()==DD.TAG_AC7)creation_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC7);
		//else System.out.println("D_Motion:decode: no creation date");
		if(dec.getTypeByte()==DD.TAG_AC8)signature = dec.getFirstObject(true).getBytes(DD.TAG_AC8);
		//else System.out.println("D_Motion:decode: no signature");
		if(dec.getTypeByte()==DD.TAG_AC9)choices = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_SEQUENCE, new D_MotionChoice[0], new D_MotionChoice());	
		//else System.out.println("D_Motion:decode: no choices");
		if(dec.getTypeByte()==DD.TAG_AC10)constituent = D_Constituent.getEmpty().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC11)enhanced = new D_Motion().decode(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC12)organization = D_Organization.getOrgFromDecoder(dec.getFirstObject(true));	
		if(dec.getTypeByte()==DD.TAG_AC13)category = dec.getFirstObject(true).getString(DD.TAG_AC13);
		
		this.setTemporary(false);
		return this;
	}
	/**
	 * 	 * Both store signature and returns it, using current constituent
	 * @return
	 */
	public byte[] sign() {
		if(DEBUG) System.out.println("D_Motion:sign: start");
		
		//D_Constituent 
		long cLID = this.getConstituentLID();
		if (cLID <= 0) {
			if (_DEBUG) System.out.println("D_Motion: sign: no signature constituent LID!");
			Application_GUI.warning(__("No secret key to sign motion, no constituent LID"), __("No Secret Key!"));
			return null;
		}
		this.constituent = D_Constituent.getConstByLID(cLID, true, false);
		if (constituent == null) {
			if (_DEBUG) System.out.println("D_Motion: sign: no signature Constituent!");
			Application_GUI.warning(__("No secret key to sign motion, no constituent"), __("No Secret Key!"));
			return null;
		}
		SK sk = constituent.getSK();
		//if (sk == null) return null;
		//ciphersuits.SK sk = util.Util.getStoredSK(signer_GID);
		if (sk == null) {
			if(_DEBUG) System.out.println("D_Motion:sign: no signature secret key!");
			Application_GUI.warning(__("No secret key to sign motion, no constituent GID"), __("No Secret Key!"));
			return null;
		}
		if (DEBUG) System.out.println("D_Motion:sign: sign="+sk);

		return sign(sk);
	}
	/**
	 * Both store signature and returns it
	 * @param sk
	 * @return
	 */
	public byte[] sign(SK sk){
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Motion: sign: this="+this+"\nsk="+sk);
		byte[] msg = this.getSignableEncoder().getBytes();
		byte[] sgn = Util.sign(msg, sk);
		boolean ver = Util.verifySign(msg, sk.getPK(), sgn);
		if (! ver) {
			if (_DEBUG) System.out.println("D_Motion: sign: troubles verifying signature");
		}
		
		setSignature(sgn);
		if ((getSignature() != null) && (getSignature().length == 0)) setSignature(null);
		if (DEBUG) System.out.println("D_Motion: sign: sk="+sk);
		if (DEBUG) System.out.println("D_Motion: sign: got sgn="+Util.byteToHex(getSignature()));
		if (DEBUG) System.out.println("D_Motion: sign: msg="+Util.byteToHex(msg));
		if (DEBUG) System.out.println("D_Motion: sign: got this for hash="+Util.byteToHex(Util.simple_hash(msg, Cipher.MD5), ":"));
		return getSignature();
	}
	/**
	 * This does not set the GID, to enable tests
	 * @return
	 */
	public String make_ID(){
		fillGlobals();
		byte hash[] = this.getHashEncoder().getBytes();
		//System.out.println("D_Motion:make_ID: got: "+Util.stringSignatureFromByte(hash));
		// return this.global_motion_ID =  
		return D_GIDH.d_Moti+Util.getGID_as_Hash(hash);
	}
	public boolean verifySignature() {
		if(DEBUG) System.out.println("D_Motion:verifySignature: start");
		String pk_ID = this.getConstituentGID();//.submitter_global_ID;
		
		if ((pk_ID == null) && (this.getConstituent() != null) && (this.getConstituent().getGID()!=null))
			pk_ID = this.getConstituent().getGID();
		if (pk_ID == null) {
			if(_DEBUG) System.out.println("D_Motion: verifySignature: no pk_ID for const: "+this.getConstituent());
			if (! DD.ACCEPT_ANONYMOUS_MOTIONS) {
				if (_DEBUG) System.out.println("D_Motion: verifySignature: reject anonymous");
				return false;
			}
		}
		
		String newGID = make_ID();
		if (! newGID.equals(this.getGID())) {
			Util.printCallPath("D_Motion: WRONG EXTERNAL GID");
			if(_DEBUG) System.out.println("D_Motion:verifySignature: WRONG HASH GID="+this.getGID()+" vs="+newGID);
			if(DEBUG) System.out.println("D_Motion:verifySignature: WRONG HASH GID result="+false);
			System.err.println("D_Motion: "+this.toString());
			return false;
		}

		if (DD.ACCEPT_ANONYMOUS_MOTIONS && pk_ID == null) {
			if (_DEBUG) System.out.println("D_Motion: verifySignature: accepted anonymous");
			return true;
		}
		
		//D_Constituent 
		String cLID = this.getConstituentLIDstr();
		if (cLID != null) //return false;
			constituent = D_Constituent.getConstByLID(cLID, true, false);
		else
			constituent = D_Constituent.getConstByGID_or_GIDH(pk_ID, null, true, false, this.getOrganizationLID());
		
		if (constituent == null) {
			if (_DEBUG) System.out.println("D_Motion: verifySignature: abandon motions from unknow constituents!");
			return false;
		}
		PK pk = constituent.getPK();
		if (pk == null) {
			if (_DEBUG) System.out.println("D_Motion: verifySignature: no pk for const: "+this.getConstituent());
			return false;
		}
		byte sgn[] = getSignature();
		if (DD.FIX_UNSIGNED_MOTIONS && sgn == null) {
			if (constituent.getSK() != null) {
				D_Motion m = D_Motion.getMotiByMoti_Keep(this);
				m.sign();
				m.storeRequest();
				m.releaseReference();
			}
		}
		byte msg[] = this.getSignableEncoder().getBytes();
		boolean result = Util.verifySign(msg, pk, sgn);
		//Util.verifySignByID(this.getSignableEncoder().getBytes(), pk_ID, getSignature());
		if (DEBUG) System.out.println("D_Motion: verifySignature: result wGID="+result);
		if (! result) {
			if (_DEBUG) System.out.println("D_Motion: verifySignature: fail "+this.getMotionTitle()+" "+Util.byteToHex( Util.simple_hash(msg, Cipher.MD5), ":")+"\n with pk="+pk);
			if (_DEBUG) System.out.println("D_Motion: verifySignature: sgn="+Util.byteToHexDump(getSignature()));
		}
		return result;
	}

	/**
	 * The next monitor is used for the storeAct, to avoid concurrent modifications of the same object.
	 * Potentially the monitor can be a field in the same object (since saving of different objects
	 * is not considered dangerous, even when they are of the same type)
	 * 
	 * What we do is the equivalent of a synchronized method "storeAct" that we avoid to avoid accidental synchronization
	 * with other methods.
	 */
	final Object monitor = new Object();
	//static final Object monitor = new Object();
	
	public long storeAct() throws P2PDDSQLException {
		synchronized(monitor) {
			return _storeAct();
		}
	}
	/**
	 * This is not synchronized
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long _storeAct() throws P2PDDSQLException {
		if (this.dirty_local || this.dirty_main || this.dirty_preferences) {
			if (DEBUG) System.out.println("D_Motion: storeAct: main dirty: l="+this.dirty_local +" ma="+ this.dirty_main +" p="+ this.dirty_preferences);
			storeAct_main();
		}
		if (this.dirty_choices)
			storeAct_choices();
		
		if (this.dirty_mydata)
			storeAct_my();
		return this.getLID();
	}
	private void storeAct_choices() throws P2PDDSQLException {
		boolean sync = true;
		if (sync) {
			Application.db.delete(table.motion_choice.TNAME, new String[]{table.motion_choice.motion_ID}, new String[]{this.getLIDstr()}, DEBUG);
		} else {
			Application.db.deleteNoSync(table.motion_choice.TNAME, new String[]{table.motion_choice.motion_ID}, new String[]{this.getLIDstr()}, DEBUG);
		}
		dirty_choices = false;
		D_MotionChoice.save(getChoices(), getLIDstr(), sync);
		
		if ((this.getSignature() != null) && (getGID() != null))
			ClientSync.payload_recent.add(streaming.RequestData.MOTI, this.getGID(), D_Organization.getOrgGIDHashGuess(this.getOrganizationGID_force()), ClientSync.MAX_ITEMS_PER_TYPE_PAYLOAD);
	}
	private void storeAct_my() {
		//boolean DEBUG = true;
		try {
			if (DEBUG) System.out.println("D_Motion: storeAct_my: r="+this.mydata.row);
			this.dirty_mydata = false;
			String param[];
			if (this.mydata.row <= 0) {
				param = new String[table.my_motion_data.FIELDS_NB_NOID];
			} else {
				param = new String[table.my_motion_data.FIELDS_NB];
			}
			param[table.my_motion_data.COL_NAME] = this.mydata.name;
			param[table.my_motion_data.COL_CATEGORY] = this.mydata.category;
			param[table.my_motion_data.COL_CREATOR] = this.mydata.creator;
			param[table.my_motion_data.COL_MOTION_LID] = this.getLIDstr();
			
			if (this.mydata.row <= 0) {
				this.mydata.row =
						Application.db.insert(true, table.my_motion_data.TNAME,
								table.my_motion_data.fields_noID, param, DEBUG);
			} else {
				param[table.my_motion_data.COL_ROW] = this.mydata.row+"";
				Application.db.update(true, table.my_motion_data.TNAME,
						table.my_motion_data.fields_noID,
						new String[]{table.my_motion_data.row}, param, DEBUG);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (DEBUG) System.out.println("D_Motion: storeAct_my: done="+this.mydata.row);
	}
	public long storeAct_main() throws P2PDDSQLException {
		//boolean DEBUG = true;

		if (this.arrival_date == null && (this.getGIDH() != null)) {
			this.arrival_date = Util.CalendargetInstance();
			if (_DEBUG) System.out.println("D_Motion: storeAct_main: missing arrival_date");
		}
	   	
		long result = -1;
		boolean sync = true;
		String id = this.getLIDstr();
	   	if (DEBUG) System.err.println("D_Motion: storeAct_main: old motion_LID="+ this.getLID()+" LIDstr="+id);

	   	String[] fields;
	   	String[] params;
	   	fields = table.motion.fields_noID_array;
	   	boolean inserting = false;
	   	if (id == null) inserting = true;
	   	
	   	if (inserting) {
	   		params = new String[fields.length];
		} else {
			params = new String[fields.length + 1];
		}
	   	this.dirty_local = this.dirty_main = this.dirty_preferences = false;
		params[table.motion.M_MOTION_GID] = this.getGID();
		params[table.motion.M_HASH_ALG] = this.getHashAlg();
		params[table.motion.M_TITLE_FORMAT] = this.getMotionTitle().title_document.getFormatString();
		params[table.motion.M_TEXT_FORMAT] = this.getMotionText().getFormatString();
		params[table.motion.M_TITLE] = this.getMotionTitle().title_document.getDocumentString();
		params[table.motion.M_TEXT] = this.getMotionText().getDocumentString();
		params[table.motion.M_ENHANCED_ID] = this.getEnhancedMotionLIDstr();
		params[table.motion.M_CONSTITUENT_ID] = this.getConstituentLIDstr();
		params[table.motion.M_ORG_ID] = getOrganizationLIDstr();
		params[table.motion.M_STATUS] = getStatus()+"";
		params[table.motion.M_CHOICES] = D_OrgConcepts.stringFromStringArray(D_MotionChoice.getNames(this.getChoices()));
		params[table.motion.M_SIGNATURE] = Util.stringSignatureFromByte(getSignature());
		params[table.motion.M_CREATION] = Encoder.getGeneralizedTime(this.getCreationDate());
		params[table.motion.M_ARRIVAL] = Encoder.getGeneralizedTime(this.setArrivalDate(arrival_date));
		params[table.motion.M_PREFERENCES_DATE] = this.getPreferencesDateStr();
		params[table.motion.M_CATEGORY] = this.getCategory();
		params[table.motion.M_BLOCKED] = Util.bool2StringInt(isBlocked());
		params[table.motion.M_HIDDEN] = Util.bool2StringInt(isHidden());
		params[table.motion.M_TEMPORARY] = Util.bool2StringInt(isTemporary());
		params[table.motion.M_PEER_SOURCE_ID] = Util.getStringID(this.getPeerSourceLID());
		params[table.motion.M_REQUESTED] = Util.bool2StringInt(isRequested());
		params[table.motion.M_BROADCASTED] = Util.bool2StringInt(isBroadcasted());
		if (inserting) {
			if (DEBUG) System.out.println("D_Motion: storeAct_main: inserting");
			if (sync) {
				result = Application.db.insert(table.motion.TNAME,
						table.motion.fields_noID_array, //fields_array,
						params,
						DEBUG
						);
			} else {
				result = Application.db.insertNoSync(table.motion.TNAME,
						table.motion.fields_noID_array, //fields_array,
						params,
						DEBUG
						);
			}
			setLID(result);
		} else {
			if (DEBUG) System.out.println("D_Motion: storeAct_main: updating");
			params[table.motion.M_MOTION_ID] = getLIDstr();
			if (sync) {
				Application.db.update(table.motion.TNAME,
						table.motion.fields_noID_array,
						new String[]{table.motion.motion_ID},
						params,
						DEBUG
						);
			} else {
				Application.db.updateNoSync(table.motion.TNAME,
						table.motion.fields_noID_array,
						new String[]{table.motion.motion_ID},
						params,
						DEBUG
						);
			}
			//result = Util.lval(this.getLIDstr(), -1);
		}
	   	return this.getLID();
	}
	/**
	 * Tests newer
	 * @param r
	 * @param sol_rq
	 * @param new_rq
	 * @return 
	 */
	public boolean loadRemote(D_Motion r, RequestData sol_rq,
			RequestData new_rq, D_Peer __peer) {
		boolean default_blocked_org = false;
		boolean default_blocked_cons = false;
		boolean default_blocked_mot = false;
		if (DEBUG) System.out.println("D_Motion: loadRemote: in r =" + r);
		
		if (! this.isTemporary()) {
			if (_DEBUG) System.out.println("D_Motion: loadRemote: abandon incoming since this is not temporary!");
			return false;
		}
		
		this.hash_alg = r.hash_alg;
		this.motion_title = r.motion_title;
		this.motion_text = r.motion_text;
		this.category = r.category;
		//if (r.choices != this.choices) {
		if (!((r.choices == null || r.choices.length == 0) && (this.choices == null || this.choices.length == 0))) {
			this.choices = r.choices;
			this.dirty_choices = true;
		}
		this.creation_date = r.creation_date;
		this.signature = r.signature;
		if (DEBUG) System.out.println("D_Motion: loadRemote: loaded this="+this);
		
		if (! Util.equalStrings_null_or_not(this.getGID(), r.getGID())) {
			this._setGID(r.getGID());
			this.setLIDstr(null);
		}
		if (! Util.equalStrings_null_or_not(this.global_organization_ID, r.global_organization_ID)) {
			this.setOrganizationGID(r.getOrganizationGID_force());
//			long oID = D_Organization.getLIDbyGIDorGIDH(r.getOrganizationGID(), r.getOrganizationGIDH());
//			if (oID <= 0) {
//				oID = D_Organization.insertTemporaryGID(r.getOrganizationGID(), r.getOrganizationGIDH(), default_blocked_org, __peer);
//				if (new_rq != null) new_rq.orgs.add(r.getGIDH());
//			}
			long oID = D_Organization.getLIDbyGID(r.global_organization_ID);
			this.setOrganizationLID(oID);
			if (this.global_organization_ID != null && oID <= 0) {
				D_Organization o = D_Organization.insertTemporaryGID_org(r.global_organization_ID, null, __peer, default_blocked_org, null);
				this.setOrganizationLID(o.getLID_forced());
				if (new_rq != null && o.isTemporary()) new_rq.orgs.add(o.getGIDH_or_guess());
			}
		}
		if (!Util.equalStrings_null_or_not(this.global_enhanced_motionID, r.global_enhanced_motionID)) {
			this.setEnhancedMotionGID(r.getEnhancedMotionGID());
			this.setEnhancedMotionLIDstr(D_Motion.getLIDstrFromGID(r.global_enhanced_motionID, this.getOrganizationLID()));
			if (r.getEnhancedMotionGID() != null && this.getEnhancedMotionLIDstr() == null) {
				this.enhanced = D_Motion.getMotiByGID(r.getEnhancedMotionGID(), true, true, false, __peer, this.getOrganizationLID(), null);
				this.setEnhancedMotionLIDstr(this.enhanced.getLIDstr_force());
				if (new_rq != null && this.enhanced.isTemporary()) new_rq.moti.add(r.global_enhanced_motionID);
				//this.enhanced = r.enhanced;
			}
		}
		if (!Util.equalStrings_null_or_not(this.global_constituent_ID, r.global_constituent_ID)) {
			this.setConstituentGID(r.getConstituentGID());
			this.setConstituentLIDstr(D_Constituent.getLIDstrFromGID(global_constituent_ID, this.getOrganizationLID()));
			if (r.getConstituentGID() != null && this.getConstituentLIDstr() == null) {
				this.constituent = D_Constituent.getConstByGID_or_GIDH(r.global_constituent_ID, null, true, true, false, __peer, this.getOrganizationLID());
				this.setConstituentLIDstr(this.constituent.getLIDstr_force());
				if (new_rq != null && this.constituent.isTemporary()) new_rq.cons.put(r.global_constituent_ID, DD.EMPTYDATE);
			}
			//this.constituent = r.constituent;
		}
		if (sol_rq != null) sol_rq.moti.add(this.getGID());
		this.dirty_main = true;
		if (this.peer_source_ID <= 0 && __peer != null)
			this.peer_source_ID = __peer.getLID_keep_force();
		this.setTemporary(false);
		this.setArrivalDate();
		if (DEBUG) System.out.println("D_Motion: loadRemote: done this="+this);
		return true;
	}
	
	public String getLIDstr_force() {
		return Util.getStringID(this.getLID_force());
	}
	/** Storing */
	public static D_Motion_SaverThread saverThread = new D_Motion_SaverThread();
	public boolean dirty_any() {
		return dirty_main || dirty_local || dirty_preferences || dirty_mydata || dirty_choices;
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
	public static int isGIDavailable(String gID, long oID, boolean DBG) throws P2PDDSQLException {
		D_Motion n = D_Motion.getMotiByGID(gID, true, false, oID);
		if (n == null) return 0;
		if (n.isTemporary()) return -1;
		return 1;
	}
	/**
	 * Sets the temporary flag
	 */
	public void setTemporary() {
		this.setTemporary(true);
		//this.setSignature(null);
	}
	public void setTemporary(boolean b) {
		this.temporary = b;
		this.dirty_local = true;
	}
	public static void setTemporary(D_Motion c) {
		c = D_Motion.getMotiByMoti_Keep(c);
		c.setTemporary();
		c.storeRequest();
		c.releaseReference();
	}
	/**
	 * If marked temporary, no signature or no GID
	 * @return
	 */
	public boolean isTemporary() {
		return this.temporary 
				//|| ((this.signature == null || this.signature.length == 0) )
				|| this.getGID() == null;
	}
	/*
	public static long _insertTemporaryNeighborhoodGID(String motion_GID,
			String org_ID, long _default, D_Peer __peer) throws P2PDDSQLException {
		D_Motion n = D_Motion.getMotiByGID(motion_GID, true, true, true, __peer, Util.lval(org_ID));
		n.setTemporary();
		n.storeRequest();
		n.releaseReference();
		return n.getLID_force();
	}
	*/
	public static long insertTemporaryGID(String p_cGID,
			long p_oLID, D_Peer __peer, boolean default_blocked) {
		D_Motion neigh = D_Motion.insertTemporaryGID_org(p_cGID, p_oLID, __peer, default_blocked);
		if (neigh == null) return -1;
		return neigh.getLID_force(); 
	}
	public static D_Motion insertTemporaryGID_org(
			String p_cGID, long p_oLID,
			D_Peer __peer, boolean default_blocked) {
		D_Motion moti;
		if ((p_cGID != null)) {
			moti = D_Motion.getMotiByGID(p_cGID, true, true, true, __peer, p_oLID, null);
			//consts.setName(_name);
			moti.setBlocked(default_blocked);
			moti.setArrivalDate();
			moti.storeRequest();
			moti.releaseReference();
		}
		else return null;
		return moti; 
	}
	/**
	 * This function has to be called after the appropriate dirty flags are set:
	 * dirty_main - for elements in the peer table
	 * 
	 * This returns asynchronously (without waiting for the storing to happen).
	 */
	public void storeRequest() {
		//Util.printCallPath("Why store?"+this.getMotionTitle());
		if (!this.dirty_any()) {
			Util.printCallPath("Why store when not dirty?");
			return;
		}

		if (this.arrival_date == null && (this.getGIDH() != null)) {
			this.arrival_date = Util.CalendargetInstance();
			if (_DEBUG) System.out.println("D_Motion: storeRequest: missing arrival_date");
			Util.printCallPath("D_Motion: storeRequest: Why no arrival time??");
		}
		
		String save_key = this.getGIDH();
		
		if (save_key == null) {
			//save_key = ((Object)this).hashCode() + "";
			//Util.printCallPath("Cannot store null:\n"+this);
			//return;
			D_Motion._need_saving_obj.add(this);
			if (DEBUG) System.out.println("D_Motion: storeRequest: added to _need_saving_obj");
		} else {		
			if (DEBUG) System.out.println("D_Motion: storeRequest: GIDH="+save_key);
			D_Motion._need_saving.add(this);
		}
		try {
			if (! saverThread.isAlive()) { 
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
		//Util.printCallPath("Why store sync?"+this.getMotionTitle());
		try {
			return storeAct();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
		
	
	public boolean readyToSend() {
		if(this.getGID() == null) return false;
		if(this.getOrganizationGID_force() == null) return false;
		return true;
	}
	public void setEditable() {
		setSignature(null);
		this.setGID(null);
	}
	public boolean isEditable() {
		if (this.getGID() == null) {
			if(DEBUG) out.println("D_Motion:editable: no GID");
			return true;
		}
		
//		if (getSignature() == null) {
//			if(DEBUG) out.println("D_Motion:editable: no sign");
//			return true;
//		}
		
//		if (! verifySignature()) {
//			if (_DEBUG) out.println("D_Motion:editable: no verifiable signature");
//			Util.printCallPath("");
////			this.setGID(make_ID());
////			setSignature(null);
////			this.storeRequest();
////			return true;			
//		}
		
		return false;
	}
	public String getDefaultChoice() {
		if((this.getChoices()!=null)&&(this.getChoices().length>0)) return this.getChoices()[0].short_name;
		return "0";
	}
	public void changeToDefaultEnhancement() {
		this.setEnhancedMotionLIDstr(getLIDstr());
		this.setEnhancedMotionGID(this.getGID());
		this.setLIDstr(null);
		this.setGID(null);
		this.setSignature(null);
		this.setConstituentLIDstr(null);
		this.setConstituentGID(null);
		this.getMotionTitle().title_document.setDocumentString(__("Enhancement Of:")+" "+this.getMotionTitle().title_document.getDocumentUTFString());
		this.getMotionText().setDocumentString(__("Enhancement Of:")+" \n"+this.getMotionText().getDocumentUTFString());
		this.setCreationDate(this.setPreferencesDate(this.setArrivalDate(Util.CalendargetInstance())));
		this.setRequested(this.setBlocked(false));
		this.setBroadcasted(true);		
		//this.storeRequest();
	}
	public static ArrayList<String> checkAvailability(ArrayList<String> cons,
			String orgID, boolean DBG) {
		ArrayList<String> result = new ArrayList<String>();
		for (String cHash : cons) {
			if (!available(cHash, orgID, DBG)) result.add(cHash);
		}
		return result;
	}
	/**
	 * check blocking at this level
	 * @param cHash
	 * @param orgID
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static boolean available(String hash, String orgID, boolean DBG) {
		boolean result = true;
		D_Motion c = D_Motion.getMotiByGID (hash, true, false, Util.Lval(orgID));
		if (
				(c == null) 
				|| (c.getOrganizationLID() != Util.lval(orgID)) 
				|| ( c.isTemporary() && !c.isBlocked() )
				) result = false;
		
		if (DEBUG || DBG) System.out.println("D_Motion: available: "+hash+" in "+orgID+" = "+result);
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
		return global_motionID;
	}
	public String getGID() {
		return global_motionID;
	}
	/**
	 * Just sets the GID (and tests that it is not compressed)
	 * @param global_motion_ID
	 */
	public void _setGID(String global_motion_ID) {
		try{
			int id = Integer.parseInt(global_motion_ID);
			throw new RuntimeException("Impossible happened: Packing already packed:" + global_motion_ID);
			//return id;
		}catch(NumberFormatException e){}
		catch(Exception e) {
			Util.printCallPath(e.getLocalizedMessage());
		}
		__setGID(global_motion_ID);
	}
	/**
	 * Just sets the GID,
	 * and also resets the verification that the GID is valid
	 * @param global_motion_ID
	 */
	public void __setGID(String global_motion_ID) {
		this.global_motionID = global_motion_ID;
		this.__LastGIDValid = null;
	}
	/**
	 * If already loaded adjusts adding indexes for GID
	 * @param _global_motionID
	 * @return
	 */
	public String setGID(String _global_motionID) {
		if (Util.equalStrings_null_or_not(_global_motionID, this.getGID())) {
			return _global_motionID;
		}
		boolean loaded_in_cache = this.isLoaded();
		_setGID(_global_motionID);
		this.dirty_main = true;
		Long oID = this.getOrganizationLID();
		if (loaded_in_cache) {
			if (this.getGID() != null)
				D_Motion_Node.putConstByGID(this.getGID(), oID, this); //.loaded_const_By_GID.put(this.getGID(), this);
			if (this.getGIDH() != null)
				D_Motion_Node.putConstByGIDH(this.getGIDH(), oID, this); //.loaded_const_By_GIDhash.put(this.getGIDH(), this);
		}
		return _global_motionID;
	}
	public boolean isLoaded() {
		String GIDH, GID;
		if (!D_Motion_Node.loaded_objects.inListProbably(this)) return false;
		long lID = this.getLID();
		long oID = this.getOrganizationLID();
		if (lID > 0)
			if ( null != D_Motion_Node.loaded_By_LocalID.get(new Long(lID)) ) return true;
		if ((GIDH = this.getGIDH()) != null)
			if ( null != D_Motion_Node.getConstByGIDH(GIDH, oID) //.loaded_const_By_GIDhash.get(GIDH)
			) return true;
		if ((GID = this.getGID()) != null)
			if ( null != D_Motion_Node.getConstByGID(GID, oID)  //.loaded_const_By_GID.get(GID)
			) return true;
		return false;
	}
	public D_Document_Title getMotionTitle() {
		return motion_title;
	}
	public void setMotionTitle(D_Document_Title motion_title) {
		this.motion_title = motion_title;
		this.dirty_main = true;
	}
	public D_Document getMotionText() {
		return motion_text;
	}
	public void setMotionText(D_Document motion_text) {
		this.motion_text = motion_text;
		this.dirty_main = true;
	}
	/**
	 * This calls fillGlobals first (if needed)
	 * @return
	 */
	public String getConstituentGID() {
		if (! this.loaded_globals) this.fillGlobals();
		return global_constituent_ID;
	}
	/**
	 * This returns null if globals not yet filled.
	 * @return
	 */
	private String getConstituentGID_direct() {
		return global_constituent_ID;
	}
	public void setConstituentGID(String global_constituent_ID) {
		this.global_constituent_ID = global_constituent_ID;
		if (global_constituent_ID == null) {
			this.constituent_ID = null;
			this.constituent = null;
		} else {
			this.constituent = D_Constituent.getConstByGID_or_GIDH(global_constituent_ID, null, true, false, this.getOrganizationLID());
			this.constituent_ID = constituent.getLIDstr();//Util.getStringID(D_Constituent.getLIDFromGID(global_constituent_ID, this.getOrganizationLID()));
		}
		this.dirty_main = true;
	}
	/**
	 * This calls fillGlobals if not yet done.
	 * @return
	 */
	public String getEnhancedMotionGID() {
		if (! this.loaded_globals) this.fillGlobals();
		return global_enhanced_motionID;
	}
	/**
	 * This returns nill if globals not yet called
	 * @return
	 */
	private String getEnhancedMotionGID_direct() {
		return global_enhanced_motionID;
	}
	public void setEnhancedMotionGID(String global_enhanced_motionID) {
		this.global_enhanced_motionID = global_enhanced_motionID;
		this.enhanced_motionID = D_Motion.getLIDstrFromGID(global_enhanced_motionID, this.getOrganizationLID());
		this.dirty_main = true;
	}
	public D_MotionChoice[] getChoices() {
		return choices;
	}
	public void setChoices(D_MotionChoice[] choices) {
		this.choices = choices;
		this.dirty_choices = true;
	}
	public Calendar getCreationDate() {
		return creation_date;
	}
	/**
	 * Sets dirty only if different
	 * @param creation_date
	 */
	public void setCreationDate(Calendar creation_date) {
		if (! Util.equalCalendars_null_or_not(this.creation_date, creation_date)) {
			this.creation_date = creation_date;
			this.dirty_main = true;
		}
	}
	public byte[] getSignature() {
		return signature;
	}
	public byte[] setSignature(byte[] signature) {
		if (signature != this.signature) {
			this.signature = signature;
			this.__LastSignValid = null;
			this.dirty_main = true;
		}
		return signature;
	}
	
	/**
	 * If the constituent object was not loaded, load it.
	 * @return
	 */
	public D_Constituent getConstituent() {
		String cLID = this.getConstituentLIDstr();
		if ((this.constituent == null) && (cLID != null))
			this.constituent = D_Constituent.getConstByLID(cLID, true, false);
		return constituent;
	}
	public void setConstituent(D_Constituent constituent) {
		this.constituent = constituent;
	}
	/**
	 * If the enhanced motion object is not loaded, loads it
	 * @return
	 */
	public D_Motion getEnhancedMotion() {
		String mLID = this.getEnhancedMotionLIDstr();
		if ((this.enhanced == null) && (mLID != null))
			this.enhanced = D_Motion.getMotiByLID(mLID, true, false);
		return enhanced;
	}
	public void setEnhancedMotion(D_Motion enhanced) {
		this.enhanced = enhanced;
	}
	/**
	 * If the organization object was not loaded, loads it.
	 * @return
	 */
	public D_Organization getOrganization() {
		String oLID = this.getOrganizationLIDstr();
		if ((this.organization == null) && (oLID != null))
			this.organization = D_Organization.getOrgByLID(oLID, true, false);
		
		//D_Organization.getOrgByLID(getOrganizationLIDstr(), true, false);
		return organization;
	}
	public void setOrganization(D_Organization organization) {
		this.organization = organization;
	}
	// set others
	public String getOrganizationGIDH() {
		if (global_organization_ID != null) return D_Organization.getOrgGIDHashGuess(getOrganizationGID_force());
		if (this.organization != null) return organization.getGIDH_or_guess();
		return null;
	}
	/**
	 * fillGlobals first if not yet done
	 * @return
	 */
	public String getOrganizationGID_force() {
		if (! this.loaded_globals) this.fillGlobals();
		return global_organization_ID;
	}
	/**
	 * This returns null if globals not yet called.
	 * @return
	 */
	public String getOrganizationGID_direct() {
		return global_organization_ID;
	}
	public void setOrganizationGID(String global_organization_ID) {
		this.global_organization_ID = global_organization_ID;
		this.organization_ID = Util.getStringID(D_Organization.getLIDbyGID(global_organization_ID));
		this.dirty_main = true;
	}
	public long getOrganizationLID() {
		return Util.lval(organization_ID);
	}
	/**
	 * No force (just current lid)
	 * @return
	 */
	public String getOrganizationLIDstr() {
		return organization_ID;
	}
	public void setOrganizationLIDstr(String organization_ID) {
		this.organization_ID = organization_ID;
	}
	public void setOrganizationLID(long organization_ID) {
		this.organization_ID = Util.getStringID(organization_ID);
		this.dirty_main = true;
	}
	public String getEnhancedMotionLIDstr() {
		return enhanced_motionID;
	}
	public void setEnhancedMotionLIDstr(String enhanced_motionID) {
		this.enhanced_motionID = enhanced_motionID;
		this.dirty_main = true;
	}
	public long getConstituentLID() {
		return Util.lval(constituent_ID);
	}
	public String getConstituentLIDstr() {
		return constituent_ID;
	}
	public void setConstituentLIDstr(String constituent_ID) {
		this.constituent_ID = constituent_ID;
		this.dirty_main = true;
	}
	public void setConstituentLID(long id) {
		this.constituent_ID = Util.getStringID(id);
		this.dirty_main = true;
	}
	public long getLID_force() {
		if (this._motionLID >= 0) return _motionLID;
		return this.storeSynchronouslyNoException();
	}
	public String getLIDstr() {
		return _motionLIDstr;
	}
	public long getLID() {
		return _motionLID;
	}
	public void setLIDstr(String motionID) {
		this._motionLIDstr = motionID;
		this._motionLID = Util.lval(motionID);
	}
	public void setLID(long _motionLID) {
		this._motionLIDstr = Util.getStringID(_motionLID);
		this._motionLID = _motionLID;
	}
	// preferences
	public Calendar getArrivalDate() {
		return arrival_date;
	}
	public Calendar setArrivalDate() {
		return setArrivalDate(Util.CalendargetInstance());
	}
	public Calendar setArrivalDate(Calendar arrival_date) {
		this.arrival_date = arrival_date;
		this.dirty_local = true;
		return arrival_date;
	}
	public Calendar getPreferencesDate() {
		return preferences_date;
	}
	public Calendar setPreferencesDate(Calendar preferences_date) {
		this.preferences_date = preferences_date;
		this.dirty_preferences = true;
		return preferences_date;
	}
	public Calendar setPreferencesDate() {
		this.preferences_date = Util.CalendargetInstance();
		this.dirty_preferences = true;
		return preferences_date;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
		this.dirty_preferences = true;
	}
	/**
	 * The signed category
	 * @param category
	 */
	public void setCategory(String category) {
		this.category = category;
		this.dirty_main = true;
	}
	private long getPeerSourceLID() {
		return peer_source_ID;
	}
	private void setPeerSourceLID(long peer_source_ID) {
		this.peer_source_ID = peer_source_ID;
		this.dirty_local = true;
	}
	public Object getTitleOrMy() {
		String n = mydata.name;
		if (n != null) return n;
		return getMotionTitle();
	}
	public Object getCategoryOrMy() {
		String n = mydata.category;
		if (n != null) return n;
		return this.getCategory();
	}
	public Object getCreatorOrMy() {
		String n = mydata.creator;
		if (n != null) return n;
		if (this.constituent_ID != null) this.constituent = D_Constituent.getConstByLID(constituent_ID, true, false);
		if (this.constituent == null) return null;
		return this.constituent.getNameOrMy();
	}
	public long storeLinkNewTemporary() {
		synchronized (monitor_object_factory) {
			if (this.getGID() != null) {
				D_Motion m = D_Motion.getMotiByGID(this.getGID(), true, false, this.getOrganizationLID());
				if (m != null) return m.getLID_force();
			}
			this.setLID(this.storeSynchronouslyNoException());
			D_Motion_Node.register_loaded(this);
		}
		return 0;
	}
	/**
	 * Not using cache
	 * @param organization_ID2
	 * @return
	 */
	public static long getCount(long organization_ID2) {
		String sql = "SELECT count(*) FROM "+table.motion.TNAME+" WHERE "+table.motion.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> count;
		long motions = 0;
		try {
			count = Application.db.select(sql, new String[]{Util.getStringID(organization_ID2)});
			motions = Integer.parseInt(count.get(0).get(0).toString());
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return motions;
	}
	
	static final String sql_answer =
				"SELECT "+table.motion.motion_title+
				","+table.motion.global_motion_ID+
				","+table.motion.motion_ID+
				" FROM "+table.motion.TNAME+
				" WHERE "+table.motion.organization_ID+"=? "
				+" ORDER BY "+table.motion.creation_date+" DESC "
				+ " LIMIT "+DD.MAX_MOTION_ANSWERTO_CHOICES
				+";";
	public static ArrayList<MotionsGIDItem> getAnswerToChoice(
			String organizationLIDstr) {
			ArrayList<ArrayList<Object>> j;
			ArrayList<MotionsGIDItem> r = new ArrayList<MotionsGIDItem>();
			try {
				j = Application.db.select(sql_answer, new String[]{organizationLIDstr}, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return r;
			}
			for (ArrayList<Object> _j :j){
				String gid = Util.getString(_j.get(1));
				String id = Util.getString(_j.get(2));
				String name = Util.getString(_j.get(0));
				r.add(new MotionsGIDItem(gid, id, name));
			}
			return r;
	}
	static final String sql_categories = "SELECT "+table.motion.category+
		" FROM "+table.motion.TNAME+
		" WHERE "+table.motion.organization_ID+"=?"+
		" GROUP BY "+table.motion.category;
	public static ArrayList<Object> getCategories(String organizationLIDstr) {
		ArrayList<Object> r = new ArrayList<Object>();
		ArrayList<ArrayList<Object>> c;
		try {
			c = Application.db.select(sql_categories, new String[]{organizationLIDstr});
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return r;
		}
		for (ArrayList<Object> _c :c) {
			r.add(_c.get(0));
		}
		return r;
	}
	public static void zapp(String _m_ID) {
   		try {
				Application.db.delete(table.justification.TNAME,
						new String[]{table.justification.motion_ID},
						new String[]{_m_ID}, DEBUG);
				Application.db.delete(table.signature.TNAME,
						new String[]{table.signature.motion_ID},
						new String[]{_m_ID}, DEBUG);
				Application.db.delete(table.news.TNAME,
						new String[]{table.news.motion_ID},
						new String[]{_m_ID}, DEBUG);
				Application.db.delete(table.motion.TNAME,
						new String[]{table.motion.motion_ID},
						new String[]{_m_ID}, DEBUG);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
	}
	public static void zappPartial() {
		try {
			Application.db.delete(
					"DELETE FROM "+table.motion.TNAME+
					" WHERE "+table.motion.signature+" IS NULL OR "+table.motion.global_motion_ID+" IS NULL",
					new String[]{}, DEBUG);
			Application.db.delete(
					"DELETE FROM "+table.justification.TNAME+
					" WHERE "+table.justification.signature+" IS NULL OR "+table.justification.global_justification_ID+" IS NULL",
					new String[]{}, DEBUG);
			Application.db.delete(
					"DELETE FROM "+table.signature.TNAME+
					" WHERE "+table.signature.signature+" IS NULL OR "+table.signature.global_signature_ID+" IS NULL",
					new String[]{}, DEBUG);
			Application.db.sync(new ArrayList<String>(Arrays.asList(table.justification.TNAME,table.signature.TNAME)));
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
	}
	/**
	 * Maps days_old into # votes
	 */
	Hashtable<Integer, Long> activity_memory = new Hashtable<Integer, Long>();
	/**
	 * With cache
	 * 
	 * @param days
	 * @param refresh
	 * @return
	 */
	public long getActivity_WithCache(int days, boolean refresh) {
		if (! refresh) {
			Long r = activity_memory.get(new Integer(days));
			if (r != null) return r;
		}
		long result = getActivity(days);
		activity_memory.put(new Integer(days), new Long(result));
		return result;
	}
	static final String sql_activity_count = "SELECT count(*) FROM "+table.signature.TNAME+" AS s "+
			" WHERE "+table.signature.motion_ID+" = ?;";
	static final String sql_activity_count_creationdate = "SELECT count(*) FROM "+table.signature.TNAME+" AS s "+
			" WHERE s."+table.signature.motion_ID+" = ? AND s."+table.signature.creation_date+">?;";
	public long getActivity(int days) {
		long result = 0;
		try {
			ArrayList<ArrayList<Object>> orgs;
			if (days == 0) orgs = Application.db.select(sql_activity_count, new String[]{this.getLIDstr()});
			else orgs = Application.db.select(sql_activity_count_creationdate, new String[]{this.getLIDstr(), Util.getGeneralizedDate(days)});
			
			if (orgs.size() > 0) result = Util.lval(orgs.get(0).get(0), 0);
			else result = new Integer("0");
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			//break;
		}
		return result;
	}
	/**
	 * Maps days_old into #news
	 */
	Hashtable<Integer, Long> countNews_memory = new Hashtable<Integer, Long>();
	/**
	 * negative parameter is used for search by arrival date while positive is for search by creation date
	 * With cache
	 * 
	 * @param days
	 * @param refresh
	 * @return
	 */
	public long getCountNews_WithCache(int days, boolean refresh) {
		if (! refresh) {
			Long r = countNews_memory.get(new Integer(days));
			if (r != null) return r;
		}
		long result = getCountNews(days);
		countNews_memory.put(new Integer(days), new Long(result));
		return result;
	}
	static final String sql_new = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
		" WHERE "+table.news.motion_ID+" = ?;";
	static final String sql_new_cr = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE n."+table.news.motion_ID+" = ? AND n."+table.news.creation_date+">?;";
	static final String sql_new_ar = "SELECT count(*) FROM "+table.news.TNAME+" AS n "+
			" WHERE n."+table.news.motion_ID+" = ? AND n."+table.news.arrival_date+">?;";
	/**
	 * negative parameter is used for search by arrival date while positive is for search by creation date
	 * @param days
	 * @return
	 */
	public long getCountNews(int days) {
		long result = 0;
		try {
			ArrayList<ArrayList<Object>> orgs;
			if (days == 0) orgs = Application.db.select(sql_new, new String[]{this.getLIDstr()});
			else if (days > 0) orgs = Application.db.select(sql_new_cr, new String[]{this.getLIDstr(), Util.getGeneralizedDate(days)});
			else orgs = Application.db.select(sql_new_cr, new String[]{this.getLIDstr(), Util.getGeneralizedDate(-days)});
			if (orgs.size() > 0) result = Util.lval(orgs.get(0).get(0), 0); //new Integer(""+((Util.get_long(result))+(Util.get_long(orgs.get(0).get(0)))));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	/**
	 * Sets serving both in peer.served_orgs and in organization.broadcasted
	 * has to sign the peer again because of served_orgs changes
	 * @return
	 */
	public boolean toggleServing() {
		D_Motion m = D_Motion.getMotiByMoti_Keep(this);
		m.setBroadcasted(!m.isBroadcasted());
		m.storeRequest();
		m.releaseReference();
		return m.isBroadcasted();
	}
	public String getCreationDateStr() {
		return Encoder.getGeneralizedTime(this.creation_date);
	}
	public String getArrivalDateStr() {
		return Encoder.getGeneralizedTime(this.arrival_date);
	}
	public String getPreferencesDateStr() {
		return Encoder.getGeneralizedTime(this.preferences_date);
	}
	public String getNameMy() {
		return this.mydata.name;
	}
	public void setNameMy(String _name) {
		this.mydata.name = _name;
		this.setPreferencesDate();
		this.dirty_mydata = true;
	}
	public String getCreatorMy() {
		return this.mydata.creator;
	}
	public void setCreatorMy(String _creat) {
		this.mydata.creator = _creat;
		this.setPreferencesDate();
		this.dirty_mydata = true;
	}
	public String getCategoryMy() {
		return this.mydata.category;
	}
	public void setCategoryMy(String _cat) {
		this.mydata.category = _cat;
		this.setPreferencesDate();
		this.dirty_mydata = true;
	}
	public boolean isRequested() {
		return requested;
	}
	public void setRequested(boolean requested) {
		this.requested = requested;
		this.setPreferencesDate();
		this.dirty_preferences = true;
	}
	public boolean isBlocked() {
		return blocked;
	}
	public boolean setBlocked(boolean blocked) {
		this.blocked = blocked;
		this.setPreferencesDate();
		this.dirty_preferences = true;
		return blocked;
	}
	public boolean isBroadcasted() {
		return broadcasted;
	}
	public void setBroadcasted(boolean broadcasted) {
		this.broadcasted = broadcasted;
		this.setPreferencesDate();
		this.dirty_preferences = true;
	}
	public boolean isHidden() {
		return hidden;
	}
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
		this.setPreferencesDate();
		this.dirty_preferences = true;
	}
	public String getCategory() {
		return category;
	}
	public long getProviderID() {
		return this.peer_source_ID;
	}
	public D_Peer getProvider() {
		if (this.peer_source_ID <= 0) return null;
		return D_Peer.getPeerByLID(peer_source_ID, true, false);
	}
	/**
	 * 
	 * @param new_text
	 * @param editor_format
	 */
	public void setMotionText(String new_text, String editor_format) {
		String old_text = this.getMotionText().getDocumentString();
		
		String old_old_text = this.getMotionText().getFormatString();

		if (! Util.equalStrings_null_or_not(new_text, old_text)) {
			this.getMotionText().setDocumentString(new_text);
			this.dirty_main = true;
		}
		if (! Util.equalStrings_null_or_not(editor_format, old_old_text)) {
			this.getMotionText().setFormatString(editor_format);//BODY_FORMAT);
			this.dirty_main = true;
		}
	}
	/**
	 * Returns the value stored in the field "choice" for a support vote (typically "0").
	 * Obtained now from the static constant D_Vote.DEFAULT_YES_COUNTED_LABEL.
	 * @return
	 */
	public String getSupportChoice() {
		return D_Vote.DEFAULT_YES_COUNTED_LABEL;
	}
	/**
	 * Gets the custom or organization-wide motions
	 * @return
	 */
	public D_MotionChoice[] getActualChoices() {
		D_MotionChoice[] _choices;
		if ((this.getChoices() != null) && (this.getChoices().length > 0)) _choices = this.getChoices();
		else _choices = this.getOrganization().getDefaultMotionChoices();
		return _choices;
	}
	
	/**
	 * Returns the first choice, considered a support choice!
	 * @return
	 */
	public D_MotionChoice getActualSupportChoice() {
		D_MotionChoice[] _choices;
		if ((this.getChoices() != null) && (this.getChoices().length > 0)) _choices = this.getChoices();
		else _choices = this.getOrganization().getDefaultMotionChoices();
		return _choices[Util.ival(this.getSupportChoice(),0)];
	}

	public static final String sql_motion_choice_support = 
			"SELECT count(*), sum(c."+table.constituent.weight+") " +
			" FROM "+table.signature.TNAME+" AS s "+
			" JOIN "+table.constituent.TNAME+" AS c ON(c."+table.constituent.constituent_ID+"=s."+table.signature.constituent_ID+")"+
			" WHERE s."+table.signature.choice+"=? AND s."+table.signature.motion_ID+"=?;";
	
	/**
	 * Class holding the number of voters and the sum of their weights,
	 * for voting a motion with a given choice.
	 * Use by getMotionChoiceSupport()
	 * @author msilaghi
	 *
	 */
	public static class MotionChoiceSupport {
		public MotionChoiceSupport() {
		}
		public MotionChoiceSupport(int _cnt) {
			setCnt(_cnt);
		}
		public MotionChoiceSupport(int _cnt, double _w) {
			setCnt(_cnt);
			setWeight(_w);
		}
		public int getCnt() {
			return cnt;
		}
		public void setCnt(int cnt) {
			this.cnt = cnt;
		}
		public double getWeight() {
			return weight;
		}
		public void setWeight(double weight) {
			this.weight = weight;
		}
		public String toString() {
			return "MCS[cnt="+cnt+" w="+weight+"]";
		}
		private int cnt = 0;
		private double weight = 0.;
	}
	/**
	 * Without cache
	 * 
	 * @param motion_LID
	 * @param short_name
	 * @return
	 */
	public static MotionChoiceSupport getMotionChoiceSupport(String motion_LID, String short_name) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Motion: getMotionChoiceSupport: mLID="+motion_LID+" ch="+short_name);
		MotionChoiceSupport result = new MotionChoiceSupport();
		try {
			ArrayList<ArrayList<Object>> l =
					Application.db.select(sql_motion_choice_support,
							new String[]{short_name, motion_LID}, DEBUG);
			if (l.size() > 0) {
				result.setCnt(Util.ival(l.get(0).get(0), 0));
				result.setWeight(Util.dval(l.get(0).get(1), 0.));
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if (DEBUG) System.out.println("D_Motion: getMotionChoiceSupport: result="+result);
		return result;
	}
	/**
	 * Maps choice to MotionChoiceSupport (count,weight)
	 */
	Hashtable<Integer, MotionChoiceSupport> supports_memory = new Hashtable<Integer, MotionChoiceSupport>();

	/**
	 * With cache
	 * 
	 * @param _choice
	 * @param refresh
	 * @return
	 */
	public MotionChoiceSupport getMotionSupport_WithCache(int _choice, boolean refresh) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Motion: getMotionChoiceSupport: mLID="+this.getLID()+" ch="+_choice);
		if (! refresh) {
			MotionChoiceSupport r = supports_memory.get(new Integer(_choice));
			if (r != null){
				if (DEBUG) System.out.println("D_Motion: getMotionChoiceSupport: r="+r);
				return r;
			}
		}
		MotionChoiceSupport result = getMotionChoiceSupport(this.getLIDstr(), _choice+""); //this.getSupportChoice());
		supports_memory.put(Integer.valueOf(_choice), result);
		if (DEBUG) System.out.println("D_Motion: getMotionChoiceSupport: result="+result);
		return result;
	}
//	static final String sql_co = "SELECT count(*) FROM "+table.signature.TNAME+
//			" WHERE "+table.signature.motion_ID+" = ? AND "+table.signature.choice+" = ?;";
//	/**
//	 * Get number of supporting people, with memory
//	 * @param i
//	 * @param refresh
//	 * @return
//	 */
//	public Object getSupport(int i, boolean refresh) {
//		if (! refresh) {
//			MotionChoiceSupport r = supports_memory.get(new Integer(i));
//			if (r != null) return r.getCnt();
//		}
//		Object result = null;
//		try {
//			ArrayList<ArrayList<Object>> orgs =
//					Application.db.select(sql_co, new String[]{this.getLIDstr(), i+""});//this.getSupportChoice()});
//			if (orgs.size() > 0) result = orgs.get(0).get(0);
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
//		supports_memory.put(new Integer(i), new MotionChoiceSupport(Util.ival(result,0)));
//		return result;
//	}
	/**
	 * Query to get all motions for an organization;
	 */
	public final static String sql_all_motions = 
			"SELECT " + table.motion.motion_ID
			+" FROM "+table.motion.TNAME+
			" WHERE "+table.motion.organization_ID + "=? ";
	/**
	 * The index of the motion LID in the result of getAllMotions()
	 */
	public static final int SELECT_ALL_MOTI_LID = 0;
	/**
	 * 
	 * @param hide (if true, then skip hidden)
	 * @param o_LID (organization LID)
	 * @param crt_enhanced_LID (if nonull, then set filter)
	 * @return
	 */
	public static java.util.ArrayList<java.util.ArrayList<Object>>
	getAllMotions(String o_LID, boolean hide, String crt_enhanced_LID) {
		ArrayList<ArrayList<Object>> moti;
		if (Application.db == null) return new ArrayList<ArrayList<Object>>();
		String sql = sql_all_motions;
		if (hide)	sql	 +=	" AND "+table.motion.hidden+" != '1' ";
		try {
			if (crt_enhanced_LID != null) {
				sql += " AND " + table.motion.enhances_ID+" = ?;";
				moti = Application.db.select(sql, new String[]{o_LID, crt_enhanced_LID});
			} else {
				moti = Application.db.select(sql+";", new String[]{o_LID});
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return new ArrayList<ArrayList<Object>>();
		}
		return moti;
	}
	//boolean __isLastGIDChecked = false;
	Boolean __LastGIDValid = null;
	public boolean isLastGIDChecked() {
		return __LastGIDValid != null;//__isLastGIDChecked;
	}
	public Boolean getLastGIDValid() {
		return __LastGIDValid;
	}
	public void setLastGIDValid(Boolean result) {
		__LastGIDValid = result;
	}
	/**
	 * Returns the last valid GID if available
	 * @param refresh 
	 * @return
	 */
	public Boolean getGIDValid_WithMemory(boolean refresh) {
		Boolean result;
		if (!refresh && isLastGIDChecked()) result = getLastGIDValid();
		else {
			//System.out.println("D_Motion: getSignValid_WithMemory: check GID");
			String newGID = make_ID();
			if (newGID == null) result = new Boolean(false);
			else result = new Boolean(newGID.equals(getGID()));
			setLastGIDValid((Boolean)result);
		}
		return result;
	}
	Boolean __LastSignValid = null;
	public boolean isLastSignChecked() {
		return __LastSignValid != null;//__isLastGIDChecked;
	}
	public Boolean getLastSignValid() {
		return __LastSignValid;
	}
	public void setLastSignValid(Boolean result) {
		__LastSignValid = result;
	}
	public Boolean getSignValid_WithMemory(boolean refresh) {
		Boolean result;
		if (!refresh && isLastSignChecked()) result = getLastSignValid();
		else {
			//System.out.println("D_Motion: getSignValid_WithMemory: check Sign");
			if ((getGID() == null) || (getConstituentGID() == null)) result = new Boolean(false);
			else result = new Boolean(verifySignature());
			
			setLastSignValid((Boolean)result);
		}
		return result;
	}
	static final String sql_list_of_justifications_for_motion = 
			"SELECT "+table.justification.justification_title+
			","+table.justification.global_justification_ID+
			","+table.justification.justification_ID+
			" FROM "+table.justification.TNAME+
			" WHERE "+table.justification.motion_ID+"=?;";

	/**
		 * Get the list of all justifications for a given motion
		 * 
		 * Not cached
		 * @param motionLID
		 * @return
		 */
	public static ArrayList<JustGIDItem> getJustificationsListForMotion(String motionLID) 
	{
		ArrayList<ArrayList<Object>> j;
		ArrayList<JustGIDItem> r = new ArrayList<JustGIDItem>();
		try {
			j = Application.db.select(sql_list_of_justifications_for_motion, new String[]{motionLID}, DEBUG);
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
	ArrayList<JustGIDItem> justifications_memory = null;
	/**
	 * Get the list of all justifications for this motion.
	 * Cached
	 * @return
	 */
	public ArrayList<JustGIDItem> getJustificationsListForMotion() {
		if (justifications_memory != null) return justifications_memory;
		return justifications_memory = getJustificationsListForMotion(this.getLIDstr());
	}
	Calendar lastCacheResetTime = Util.CalendargetInstance();
	/**
	 * This resets when now is age after lastCacheResetTime
	 * @param now
	 * @param age_milliseconds (use -1 for one day)
	 */
	public void resetCacheWhenOld(Calendar now, long age_milliseconds) {
		if (age_milliseconds < 0) age_milliseconds = 1000*24*60*60; // one day is milliseconds
		if (now == null) now = Util.CalendargetInstance();
		/**
		 * Only one thread can compare with a given resetTime
		 */
		synchronized(lastCacheResetTime) {
			if (now.getTimeInMillis() - lastCacheResetTime.getTimeInMillis() < age_milliseconds) return;
			resetCache();
		}
	}
	/**
	 * Call this to force re-reading from the disk statistics at the next time they are requested.
	 * To be used on insertion/delete of new votes/justifications/news related to this motion.
	 * Should also be used at least once each day, since some statistics (recentness) are based on days.
	 */
	public void resetCache() {
		__LastSignValid = null;
		__LastGIDValid = null;
		supports_memory.clear(); //.remove(new Integer(_choice));
		countNews_memory.clear();
		activity_memory.clear();
		justifications_memory = null;
		/**
		 * Last reset day, to enable resetting if too old
		 */
		lastCacheResetTime = Util.CalendargetInstance();
	}
	/**
	 * Checks that LID and GID are not null
	 * @return
	 */
	public boolean realized() {
		if (this.getGID() == null) return false;
		if (this.getLIDstr() == null) return false;
		return true;
	}

}

class D_Motion_SaverThread extends util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000; // ms to sleep
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	/**
	 * The next monitor is needed to ensure that two D_Motion_SaverThreadWorker are not concurrently modifying the database,
	 * and no thread is started when it is not needed (since one is already running).
	 */
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Motion_SaverThread() {
		super("D_Motion Saver", true);
		//start ();
	}
	public void _run() {
		for (;;) {
			if (stop) return;
			//System.out.println("D_Motion_SaverThread: _run: will lock");
			synchronized(saver_thread_monitor) {
				new D_Motion_SaverThreadWorker().start();
			}
			//System.out.println("D_Motion_SaverThread: _run: will unlock");
			/*
			synchronized(saver_thread_monitor) {
				D_Motion de = D_Motion.need_saving_next();
				if (de != null) {
					if (DEBUG) System.out.println("D_Motion_Saver: loop saving "+de);
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
					//System.out.println("D_Motion_Saver: idle ...");
				}
			}
			*/
			synchronized(this) {
				try {
					//System.out.println("D_Motion_SaverThread: _run: will sleep");
					wait(SAVER_SLEEP);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

class D_Motion_SaverThreadWorker extends util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000;
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	//public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Motion_SaverThreadWorker() {
		super("D_Motion Saver Worker", false);
		//start ();
	}
	public void _run() {
		if (DEBUG) System.out.println("D_Motion_SaverThreadWorker: wait to start");
		synchronized(D_Motion_SaverThread.saver_thread_monitor) {
			D_Motion de;
			boolean edited = true;
			if (DEBUG) System.out.println("D_Motion_SaverThreadWorker: start");
			
			 // first try objects being edited
			de = D_Motion.need_saving_obj_next();
			
			// then try remaining objects 
			if (de == null) { de = D_Motion.need_saving_next(); edited = false; }
			
			if (de != null) {
				if (DEBUG) System.out.println("D_Motion_SaverThreadWorker: loop saving "+de.getMotionTitle());
				Application_GUI.ThreadsAccounting_ping("Saving");
				if (edited) D_Motion.need_saving_obj_remove(de);
				else D_Motion.need_saving_remove(de);//, de.instance);
				if (DEBUG) System.out.println("D_Motion_SaverThreadWorker: loop removed need_saving flag");
				// try 3 times to save
				for (int k = 0; k < 3; k++) {
					try {
						if (DEBUG) System.out.println("D_Motion_SaverThreadWorker: loop will try saving k="+k);
						de.storeAct();
						if (DEBUG) System.out.println("D_Motion_SaverThreadWorker: stored org:"+de.getMotionTitle());
						break;
					} catch (Exception e) {
						e.printStackTrace();
						synchronized(this) {
							try {
								if (DEBUG) System.out.println("D_Motion_SaverThreadWorker: sleep");
								wait(SAVER_SLEEP_ON_ERROR);
								if (DEBUG) System.out.println("D_Motion_SaverThreadWorker: waked error");
							} catch (InterruptedException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
			} else {
				Application_GUI.ThreadsAccounting_ping("Nothing to do!");
				if (DEBUG) System.out.println("D_Motion_SaverThreadWorker: idle ...");
			}
		}
		/*
		synchronized(this) {
			try {
				if (DEBUG) System.out.println("D_Motion_Saver: sleep");
				wait(SAVER_SLEEP);
				if (DEBUG) System.out.println("D_Motion_Saver: waked");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		*/
	}
}

