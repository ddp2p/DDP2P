package data;

import handling_wb.PreparedMessage;
import hds.ASNPoint;
import hds.ASNSyncPayload;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;

import streaming.RequestData;
import util.DDP2P_DoubleLinkedList;
import util.DDP2P_DoubleLinkedList_Node;
import util.DDP2P_DoubleLinkedList_Node_Payload;
import util.P2PDDSQLException;
import util.Summary;
import util.Util;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import ciphersuits.SK;
import config.Application;
import config.Application_GUI;
import config.DD;
import config.Language;
import data.D_Constituent.D_Constituent_Node;

/**
D_Neighborhood ::= SEQUENCE {
 global_neighborhood_ID PrintableString,
 address Document,
 "name" UTF8String,
 "name_lang" PrintableString,
 name_division UTF8String,
 name_division_lang PrintableString,
 name_division_charset PrintableString,
 names_subdivisions SEQUENCE OF UTF8String,
 name_subdivisions_lang SEQUENCE OF PrintableString,
 name_subdivisions_charset SEQUENCE OF PrintableString,
 submitter D_Constituent,
 signature OCTET_STRING
}
 */
public
class D_Neighborhood extends ASNObj implements   DDP2P_DoubleLinkedList_Node_Payload<D_Neighborhood>, Summary {
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	private String global_neighborhood_ID;
	private String name;
	private String name_lang;
	private String name_charset;
	private String description;
	private String address;
	private ASNPoint[] boundary; //explanation, GPS coordinates, etc
	private String name_division;
	private String[] names_subdivisions;
	private String name_division_lang;
	private String name_division_charset;
	private String name_subdivision_lang;
	private String name_subdivision_charset;
	
	private String parent_global_ID;
	
	private String submitter_global_ID; //OPTIONAL

	private String global_organization_ID = null;
	
	private Calendar creation_date;
	private String _creation_date;
	
	private byte[] picture;
	private byte[] signature;

	private Calendar arrival_date;
	private String _arrival_date;

	private Calendar preferences_date;
	private String _preferences_date;
	
	private String peer_source_ID = null;
	private boolean blocked=false, requested=false, broadcasted=true;
	private boolean temporary = true;
	
	public D_Neighborhood parent;
	public D_Constituent submitter; //OPTIONAL
	
	// temporary values: may not have been initialized
	private String parent_ID = null;
	private String submitter_ID; // if negative we know it was not yet initialized
	private long organization_ID = -1;
	private String neighborhoodID = null;
	private long _neighborhoodID = -1;
	
	public int status_references = 0;
	private boolean dirty_main = false;
	private boolean dirty_local = false; // local to the agent (peer_source, arrival_date)
	private boolean dirty_mydata = false;  //(may be later merged to preferences)
	private boolean dirty_preferences = false; // shared among agents of a peer (blocked, requested, broadcasted)

	public boolean loaded_globals = false;
	
	private static Object monitor_object_factory = new Object();
	//static Object lock_organization_GID_storage = new Object(); // duplicate of the above
	public static int MAX_LOADED_CONSTS = 10000;
	public static long MAX_CONSTS_RAM = 10000000;
	private static final int MIN_CONSTS_RAM = 2;
	D_Neighborhood_Node component_node = new D_Neighborhood_Node(null, null);
		
	public static D_Neighborhood getEmpty() {return new D_Neighborhood();}
	public D_Neighborhood instance() throws CloneNotSupportedException{return new D_Neighborhood();}
	private D_Neighborhood() {}
	private D_Neighborhood(Long lID, boolean load_Globals) throws P2PDDSQLException {
		try {
			init(lID);
			if (load_Globals) this.fillGlobals();
		} catch (Exception e) {
			//e.printStackTrace();
			throw new P2PDDSQLException(e.getLocalizedMessage());
		}
	}
	private D_Neighborhood(String gID, boolean load_Globals, boolean create,
			D_Peer __peer, long p_oLID) throws P2PDDSQLException {
		try {
			init(gID);
			if (load_Globals) this.fillGlobals();
		} catch (Exception e) {
			//e.printStackTrace();
			if (create) {
				this.global_neighborhood_ID = gID;
				this.organization_ID = p_oLID;
				if (__peer != null) this.peer_source_ID = (__peer.getLIDstr_keep_force());
				this.dirty_main = true;
				return;
			}
			throw new P2PDDSQLException(e.getLocalizedMessage());
		}
	}
	String sql = "SELECT  "+Util.setDatabaseAlias(table.neighborhood.fields_neighborhoods, "n")
		+ ", p."+table.neighborhood.global_neighborhood_ID
		+ " FROM "+table.neighborhood.TNAME+" AS n "
		+ " LEFT JOIN "+table.neighborhood.TNAME+" AS p ON(n."+table.neighborhood.parent_nID+"=p."+table.neighborhood.neighborhood_ID+") "
		;
	String cond_ID = sql+" WHERE n."+table.neighborhood.neighborhood_ID+"=?;";
	String cond_GID = sql+" WHERE n."+table.neighborhood.global_neighborhood_ID+"=?;";

	private void init(Long lID) throws Exception {
		ArrayList<ArrayList<Object>> a;
		a = Application.db.select(cond_ID, new String[]{Util.getStringID(lID)});
		if (a.size() == 0) throw new Exception("D_Neighborhood:init:None for lID="+lID);
		init(a.get(0));
		if(DEBUG) System.out.println("D_Neighborhood: init: got="+this);//result);
	}
	private void init(String gID) throws Exception {
		ArrayList<ArrayList<Object>> a;
		a = Application.db.select(cond_GID, new String[]{gID});
		if (a.size() == 0) throw new Exception("D_Neighborhood:init:None for GID="+gID);
		init(a.get(0));
		if(DEBUG) System.out.println("D_Neighborhood: init: got="+this);//result);
	}
	private void init(ArrayList<Object> N) {
		/*
		setBoundary(null);
		setCreationDate(Util.getCalendar(Util.getString(N.get(table.neighborhood.IDX_CREATION_DATE))));
		setArrivalDate(Util.getCalendar(Util.getString(N.get(table.neighborhood.IDX_ARRIVAL_DATE))));
		setGID(Util.getString(N.get(table.neighborhood.IDX_GID)));
		setLIDstr(Util.getString(N.get(table.neighborhood.IDX_ID)));
		setDescription(Util.getString(N.get(table.neighborhood.IDX_DESCRIPTION)));
		setName(Util.getString(N.get(table.neighborhood.IDX_NAME)));
		setName_lang(Util.getString(N.get(table.neighborhood.IDX_NAME_LANG)));
		name_charset = Util.getString(N.get(table.neighborhood.IDX_NAME_CHARSET));
		setName_division(Util.getString(N.get(table.neighborhood.IDX_NAME_DIVISION)));
		setSubmitterLIDstr(Util.getString(N.get(table.neighborhood.IDX_SUBMITTER_ID)));
		setParentLIDstr(Util.getString(N.get(table.neighborhood.IDX_PARENT_ID)));
		setLIDOrg(Util.lval(Util.getString(N.get(table.neighborhood.IDX_ORG_ID)), -1));
		String subs = Util.getString(N.get(table.neighborhood.IDX_NAMES_DUBDIVISIONS));
		if (subs!=null) setNames_subdivisions(splitSubDivisions(subs));
		setParent_GID(Util.getString(N.get(table.neighborhood.IDX_FIELDs+0)));
		setGIDOrg(D_Organization.getGIDbyLID(getLIDOrg()));//Util.getString(N.get(table.neighborhood.IDX_FIELDs+2));
		setSubmitter_GID(D_Constituent.getGIDFromLID(getSubmitterLIDstr()));//Util.getString(N.get(table.neighborhood.IDX_FIELDs+1));
		setPicture(Util.byteSignatureFromString(Util.getString(N.get(table.neighborhood.IDX_PICTURE))));
		setSignature(Util.byteSignatureFromString(Util.getString(N.get(table.neighborhood.IDX_SIGNATURE))));	
		setBlocked(Util.stringInt2bool(Util.getString(N.get(table.neighborhood.IDX_BLOCKED)), false));	
		setRequested(Util.stringInt2bool(Util.getString(N.get(table.neighborhood.IDX_REQUESTED)), false));	
		setBroadcasted(Util.stringInt2bool(Util.getString(N.get(table.neighborhood.IDX_BROADCASTED)), false));
		*/
		address = Util.getString(N.get(table.neighborhood.IDX_ADDRESS), null);
		creation_date = (Util.getCalendar(_creation_date = Util.getString(N.get(table.neighborhood.IDX_CREATION_DATE))));
		global_neighborhood_ID = (Util.getString(N.get(table.neighborhood.IDX_GID)));
		setLIDstr(Util.getString(N.get(table.neighborhood.IDX_ID)));
		description = (Util.getString(N.get(table.neighborhood.IDX_DESCRIPTION)));
		name = (Util.getString(N.get(table.neighborhood.IDX_NAME)));
		name_lang = (Util.getString(N.get(table.neighborhood.IDX_NAME_LANG)));
		name_charset = Util.getString(N.get(table.neighborhood.IDX_NAME_CHARSET));
		name_division = (Util.getString(N.get(table.neighborhood.IDX_NAME_DIVISION)));
		
		name_division_lang = (Util.getString(N.get(table.neighborhood.IDX_NAME_DIVISION_LANG)));
		name_division_charset = Util.getString(N.get(table.neighborhood.IDX_NAME_DIVISION_CHARSET));
		name_subdivision_lang = (Util.getString(N.get(table.neighborhood.IDX_NAMES_DUBDIVISIONS_LANG)));
		name_subdivision_charset = Util.getString(N.get(table.neighborhood.IDX_NAMES_DUBDIVISIONS_CHARSET));
		
		submitter_ID = (Util.getString(N.get(table.neighborhood.IDX_SUBMITTER_ID)));
		parent_ID = (Util.getString(N.get(table.neighborhood.IDX_PARENT_ID)));
		organization_ID = (Util.lval(Util.getString(N.get(table.neighborhood.IDX_ORG_ID)), -1));
		String subs = Util.getString(N.get(table.neighborhood.IDX_NAMES_DUBDIVISIONS));
		if (subs!=null) names_subdivisions = (splitSubDivisions(subs));
		picture = (Util.byteSignatureFromString(Util.getString(N.get(table.neighborhood.IDX_PICTURE))));
		boundary = (null);
		signature = (Util.byteSignatureFromString(Util.getString(N.get(table.neighborhood.IDX_SIGNATURE))));	
		arrival_date = (Util.getCalendar(_arrival_date = Util.getString(N.get(table.neighborhood.IDX_ARRIVAL_DATE))));
		preferences_date = (Util.getCalendar(_preferences_date = Util.getString(N.get(table.neighborhood.IDX_PREFERENCES_DATE))));
		blocked = (Util.stringInt2bool(Util.getString(N.get(table.neighborhood.IDX_BLOCKED)), false));	
		requested = (Util.stringInt2bool(Util.getString(N.get(table.neighborhood.IDX_REQUESTED)), false));	
		broadcasted = (Util.stringInt2bool(Util.getString(N.get(table.neighborhood.IDX_BROADCASTED)), false));
		peer_source_ID = (Util.getString(N.get(table.neighborhood.IDX_PEER_SOURCE_ID)));
		
		this.parent_global_ID = (Util.getString(N.get(table.neighborhood.IDX_FIELDs+0)));
		//this.global_organization_ID = (D_Organization.getGIDbyLID(getLIDOrg())); //Util.getString(N.get(table.neighborhood.IDX_FIELDs+2));
		//this.submitter_global_ID = (D_Constituent.getGIDFromLID(getSubmitterLIDstr())); //Util.getString(N.get(table.neighborhood.IDX_FIELDs+1));
		//if (DEBUG) System.out.println("D_Neighborhood: init: got="+this);
		
		init_my();
	}
	private void init_my() {
		if (_neighborhoodID <= 0) return;
		String sql_my_neighborhood = 
				"SELECT "+ table.my_neighborhood_data.fields_list
				+ " FROM "+ table.my_neighborhood_data.TNAME
				+ " WHERE "+ table.my_neighborhood_data.COL_NEIGHBORHOOD_LID + " = ?;"; 
		try {
			ArrayList<ArrayList<Object>> my = Application.db.select(sql_my_neighborhood, new String[]{Util.getString(this._neighborhoodID)});
			if (my.size() == 0) {
				this.mydata.row = -1;
				return;
			}
			this.mydata.category = Util.getString(my.get(0).get(table.my_neighborhood_data.COL_CATEGORY));
			this.mydata.name = Util.getString(my.get(0).get(table.my_neighborhood_data.COL_NAME));
			this.mydata.row = Util.lval(my.get(0).get(table.my_neighborhood_data.COL_ROW));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public String toSummaryString() {
		return "D_Neighborhood: ["+
				//";\n creation_date*="+Encoder.getGeneralizedTime(creation_date)+
		";\n name*="+name+
		";\n name_division*="+name_division+
		"]";
	}
	public String toString() {
		return "D_Neighborhood: ["+
		";\n creation_date*="+Encoder.getGeneralizedTime(creation_date)+
		";\n name*="+name+
		";\n name_lang*="+name_lang+
		";\n description*="+description+
		";\n global_neighborhood_ID="+global_neighborhood_ID+
		";\n boundary*=["+Util.nullDiscrim(boundary, Util.concat(boundary, ":"))+"]"+
		";\n name_division*="+name_division+
		";\n name_subdivisions*="+Util.nullDiscrimArray(names_subdivisions)+
		";\n orgID="+organization_ID+
		";\n orgGID="+global_organization_ID+
		";\n parentGID*="+parent_global_ID+
		";\n parentID="+parent_ID+
		";\n parent(GID*)="+parent+
		";\n submitterGID*="+submitter_global_ID+
		";\n submit_ID="+submitter_ID+
		";\n submit(GID*)="+submitter+
		";\n picture*="+Util.nullDiscrim(picture, Util.byteToHexDump(picture))+
		";\n signature="+Util.byteToHexDump(signature)+
		"]";
	}

	public static class D_Neighborhood_Node {
		private static final int MAX_TRIES = 30;
		/** Currently loaded peers, ordered by the access time*/
		private static DDP2P_DoubleLinkedList<D_Neighborhood> loaded_consts = new DDP2P_DoubleLinkedList<D_Neighborhood>();
		private static Hashtable<Long, D_Neighborhood> loaded_const_By_LocalID = new Hashtable<Long, D_Neighborhood>();
		//private static Hashtable<String, D_Neighborhood> loaded_const_By_GID = new Hashtable<String, D_Neighborhood>();
		//private static Hashtable<String, D_Neighborhood> loaded_const_By_GIDhash = new Hashtable<String, D_Neighborhood>();
		private static Hashtable<String, Hashtable<Long, D_Neighborhood>> loaded_const_By_GIDH_ORG = new Hashtable<String, Hashtable<Long, D_Neighborhood>>();
		private static Hashtable<Long, Hashtable<String, D_Neighborhood>> loaded_const_By_ORG_GIDH = new Hashtable<Long, Hashtable<String, D_Neighborhood>>();
		private static Hashtable<String, Hashtable<Long, D_Neighborhood>> loaded_const_By_GID_ORG = new Hashtable<String, Hashtable<Long, D_Neighborhood>>();
		private static Hashtable<Long, Hashtable<String, D_Neighborhood>> loaded_const_By_ORG_GID = new Hashtable<Long, Hashtable<String, D_Neighborhood>>();
		private static long current_space = 0;
		
		//return loaded_const_By_GID.get(GID);
		private static D_Neighborhood getConstByGID(String GID, Long organizationLID) {
			if (organizationLID != null && organizationLID > 0) {
				Hashtable<String, D_Neighborhood> t1 = loaded_const_By_ORG_GID.get(organizationLID);
				if (t1 == null || t1.size() == 0) return null;
				return t1.get(GID);
			}
			Hashtable<Long, D_Neighborhood> t2 = loaded_const_By_GID_ORG.get(GID);
			if ((t2 == null) || (t2.size() == 0)) return null;
			if ((organizationLID != null) && (organizationLID > 0))
				return t2.get(organizationLID);
			for (Long k : t2.keySet()) {
				return t2.get(k);
			}
			return null;
		}
		//return loaded_const_By_GID.put(GID, c);
		private static D_Neighborhood putConstByGID(String GID, Long organizationLID, D_Neighborhood c) {
			Hashtable<Long, D_Neighborhood> v1 = loaded_const_By_GID_ORG.get(GID);
			if (v1 == null) loaded_const_By_GID_ORG.put(GID, v1 = new Hashtable<Long, D_Neighborhood>());
			D_Neighborhood result = v1.put(organizationLID, c);
			
			Hashtable<String, D_Neighborhood> v2 = loaded_const_By_ORG_GID.get(organizationLID);
			if (v2 == null) loaded_const_By_ORG_GID.put(organizationLID, v2 = new Hashtable<String, D_Neighborhood>());
			D_Neighborhood result2 = v2.put(GID, c);
			
			if (result == null) result = result2;
			return result; 
		}
		//return loaded_const_By_GID.remove(GID);
		private static D_Neighborhood remConstByGID(String GID, Long organizationLID) {
			D_Neighborhood result = null;
			D_Neighborhood result2 = null;
			Hashtable<Long, D_Neighborhood> v1 = loaded_const_By_GID_ORG.get(GID);
			if (v1 != null) {
				result = v1.remove(organizationLID);
				if (v1.size() == 0) loaded_const_By_GID_ORG.remove(GID);
			}
			
			Hashtable<String, D_Neighborhood> v2 = loaded_const_By_ORG_GID.get(organizationLID);
			if (v2 != null) {
				result2 = v2.remove(GID);
				if (v2.size() == 0) loaded_const_By_ORG_GID.remove(organizationLID);
			}
			
			if (result == null) result = result2;
			return result; 
		}
//		private static D_Neighborhood getConstByGID(String GID, String organizationLID) {
//			return getConstByGID(GID, Util.Lval(organizationLID));
//		}
		
		private static D_Neighborhood getConstByGIDH(String GIDH, Long organizationLID) {
			if (organizationLID != null && organizationLID > 0) {
				Hashtable<String, D_Neighborhood> t1 = loaded_const_By_ORG_GIDH.get(organizationLID);
				if (t1 == null || t1.size() == 0) return null;
				return t1.get(GIDH);
			}
			Hashtable<Long, D_Neighborhood> t2 = loaded_const_By_GIDH_ORG.get(GIDH);
			if ((t2 == null) || (t2.size() == 0)) return null;
			if ((organizationLID != null) && (organizationLID > 0))
				return t2.get(organizationLID);
			for (Long k : t2.keySet()) {
				return t2.get(k);
			}
			return null;
		}
		private static D_Neighborhood putConstByGIDH(String GIDH, Long organizationLID, D_Neighborhood c) {
			Hashtable<Long, D_Neighborhood> v1 = loaded_const_By_GIDH_ORG.get(GIDH);
			if (v1 == null) loaded_const_By_GIDH_ORG.put(GIDH, v1 = new Hashtable<Long, D_Neighborhood>());
			D_Neighborhood result = v1.put(organizationLID, c);
			
			Hashtable<String, D_Neighborhood> v2 = loaded_const_By_ORG_GIDH.get(organizationLID);
			if (v2 == null) loaded_const_By_ORG_GIDH.put(organizationLID, v2 = new Hashtable<String, D_Neighborhood>());
			D_Neighborhood result2 = v2.put(GIDH, c);
			
			if (result == null) result = result2;
			return result; 
		}
		//return loaded_const_By_GIDhash.remove(GIDH);
		private static D_Neighborhood remConstByGIDH(String GIDH, Long organizationLID) {
			D_Neighborhood result = null;
			D_Neighborhood result2 = null;
			Hashtable<Long, D_Neighborhood> v1 = loaded_const_By_GIDH_ORG.get(GIDH);
			if (v1 != null) {
				result = v1.remove(organizationLID);
				if (v1.size() == 0) loaded_const_By_GIDH_ORG.remove(GIDH);
			}
			
			Hashtable<String, D_Neighborhood> v2 = loaded_const_By_ORG_GIDH.get(organizationLID);
			if (v2 != null) {
				result2 = v2.remove(GIDH);
				if (v2.size() == 0) loaded_const_By_ORG_GIDH.remove(organizationLID);
			}
			
			if (result == null) result = result2;
			return result; 
		}
//		private static D_Neighborhood getConstByGIDH(String GIDH, String organizationLID) {
//			return getConstByGIDH(GIDH, Util.Lval(organizationLID));
//		}
		
		/** message is enough (no need to store the Encoder itself) */
		public byte[] message;
		public DDP2P_DoubleLinkedList_Node<D_Neighborhood> my_node_in_loaded;
	
		public D_Neighborhood_Node(byte[] message,
				DDP2P_DoubleLinkedList_Node<D_Neighborhood> my_node_in_loaded) {
			this.message = message;
			this.my_node_in_loaded = my_node_in_loaded;
		}
		private static void register_fully_loaded(D_Neighborhood crt) {
			assert((crt.component_node.message==null) && (crt.loaded_globals));
			if(crt.component_node.message != null) return;
			if(!crt.loaded_globals) return;
			if ((crt.getGID() != null) && (!crt.temporary)) {
				byte[] message = crt.encode();
				synchronized(loaded_consts) {
					crt.component_node.message = message; // crt.encoder.getBytes();
					if(crt.component_node.message != null) current_space += crt.component_node.message.length;
				}
			}
		}
		/*
		private static void unregister_loaded(D_Neighborhood crt) {
			synchronized(loaded_orgs) {
				loaded_orgs.remove(crt);
				loaded_org_By_LocalID.remove(new Long(crt.getLID()));
				loaded_org_By_GID.remove(crt.getGID());
				loaded_org_By_GIDhash.remove(crt.getGIDH());
			}
		}
		*/
		private static void register_loaded(D_Neighborhood crt) {
			if (DEBUG) System.out.println("D_Neighborhood: register_loaded: start crt = "+crt);
			crt.reloadMessage(); 
			synchronized (loaded_consts) {
				String gid = crt.getGID();
				String gidh = crt.getGIDH();
				long lid = crt.getLID();
				Long organizationLID = crt.getOrgLID();
				
				loaded_consts.offerFirst(crt);
				if (lid > 0) {
					loaded_const_By_LocalID.put(new Long(lid), crt);
					if (DEBUG) System.out.println("D_Neighborhood: register_loaded: store lid="+lid+" crt="+crt.getGIDH());
				}else{
					if (DEBUG) System.out.println("D_Neighborhood: register_loaded: no store lid="+lid+" crt="+crt.getGIDH());
				}
				if (gid != null) {
					D_Neighborhood_Node.putConstByGID(gid, crt.getOrgLID(), crt); //loaded_const_By_GID.put(gid, crt);
					if (DEBUG) System.out.println("D_Neighborhood: register_loaded: store gid="+gid);
				} else {
					if (DEBUG) System.out.println("D_Neighborhood: register_loaded: no store gid="+gid);
				}
				if (gidh != null) {
					D_Neighborhood_Node.putConstByGIDH(gidh, organizationLID, crt);//loaded_const_By_GIDhash.put(gidh, crt);
					if (DEBUG) System.out.println("D_Neighborhood: register_loaded: store gidh="+gidh);
				} else {
					if (DEBUG) System.out.println("D_Neighborhood: register_loaded: no store gidh="+gidh);
				}
				if (crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				int tries = 0;
				while ((loaded_consts.size() > MAX_LOADED_CONSTS)
						|| (current_space > MAX_CONSTS_RAM)) {
					if (loaded_consts.size() <= MIN_CONSTS_RAM) break; // at least _crt_peer and _myself
	
					if (tries > MAX_TRIES) break;
					tries ++;
					D_Neighborhood candidate = loaded_consts.getTail();
					if ((candidate.status_references > 0)
							//||
							//D_Neighborhood.is_crt_const(candidate)
							//||
							//(candidate == HandlingMyself_Peer.get_myself())
							) 
					{
						setRecent(candidate);
						continue;
					}
					
					D_Neighborhood removed = loaded_consts.removeTail();//remove(loaded_peers.size()-1);
					loaded_const_By_LocalID.remove(new Long(removed.getLID())); 
					D_Neighborhood_Node.remConstByGID(removed.getGID(), removed.getOrgLID());//loaded_const_By_GID.remove(removed.getGID());
					D_Neighborhood_Node.remConstByGIDH(removed.getGIDH(), removed.getOrgLID()); //loaded_const_By_GIDhash.remove(removed.getGIDH());
					if (DEBUG) System.out.println("D_Neighborhood: register_loaded: remove GIDH="+removed.getGIDH());
					if (removed.component_node.message != null) current_space -= removed.component_node.message.length;				
				}
			}
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_Neighborhood crt) {
			loaded_consts.moveToFront(crt);
		}
		public static boolean dropLoaded(D_Neighborhood removed, boolean force) {
			boolean result = true;
			synchronized(loaded_consts) {
				if (removed.status_references > 0 || removed.get_DDP2P_DoubleLinkedList_Node() == null)
					result = false;
				if (! force && ! result) {
					System.out.println("D_Neighborhood: dropLoaded: abandon: force="+force+" rem="+removed);
					return result;
				}
				
				loaded_consts.remove(removed);
				if (removed.getLIDstr() != null) loaded_const_By_LocalID.remove(new Long(removed.getLID())); 
				if (removed.getGID() != null) D_Neighborhood_Node.remConstByGID(removed.getGID(), removed.getLID()); //loaded_const_By_GID.remove(removed.getGID());
				if (removed.getGIDH() != null) D_Neighborhood_Node.remConstByGIDH(removed.getGIDH(), removed.getLID()); //loaded_const_By_GIDhash.remove(removed.getGIDH());
				if (DEBUG) System.out.println("D_Neighborhood: drop_loaded: remove GIDH="+removed.getGIDH());
				if (removed.component_node.message != null) current_space -= removed.component_node.message.length;	
				if (DEBUG) System.out.println("D_Neighborhood: dropLoaded: exit with force="+force+" result="+result);
				return result;
			}
		}
	}
	static class D_Neighborhood_My {
		String name;// table.my_organization_data.
		String category;
		long row;
	}
	D_Neighborhood_My mydata = new D_Neighborhood_My();

	/**
	 * exception raised on error
	 * @param ID
	 * @param load_Globals 
	 * @return
	 */
	static private D_Neighborhood getNeighByLID_AttemptCacheOnly (long ID, boolean load_Globals) {
		if (ID <= 0) return null;
		Long id = new Long(ID);
		D_Neighborhood crt = D_Neighborhood_Node.loaded_const_By_LocalID.get(id);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals) {
			crt.fillGlobals();
			D_Neighborhood_Node.register_fully_loaded(crt);
		}
		D_Neighborhood_Node.setRecent(crt);
		return crt;
	}
	/**
	 * exception raised on error
	 * @param LID
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static private D_Neighborhood getNeighByLID_AttemptCacheOnly(Long LID, boolean load_Globals, boolean keep) {
		if (LID == null) return null;
		if (keep) {
			synchronized (monitor_object_factory) {
				D_Neighborhood  crt = getNeighByLID_AttemptCacheOnly(LID.longValue(), load_Globals);
				if (crt != null) {			
					crt.status_references ++;
					if (crt.status_references > 1) {
						System.out.println("D_Neighborhood: getOrgByGIDhash_AttemptCacheOnly: "+crt.status_references);
						Util.printCallPath("");
					}
				}
				return crt;
			}
		} else {
			return getNeighByLID_AttemptCacheOnly(LID.longValue(), load_Globals);
		}
	}

	static public D_Neighborhood getNeighByLID(String LID, boolean load_Globals, boolean keep) {
		return getNeighByLID(Util.Lval(LID), load_Globals, keep);
	}
	static public D_Neighborhood getNeighByLID(Long LID, boolean load_Globals, boolean keep) {
		// boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Neighborhood: getNeighByLID: "+LID+" glob="+load_Globals);
		if ((LID == null) || (LID <= 0)) {
			if (DEBUG) System.out.println("D_Neighborhood: getNeighByLID: null LID = "+LID);
			if (DEBUG) Util.printCallPath("?");
			return null;
		}
		D_Neighborhood crt = D_Neighborhood.getNeighByLID_AttemptCacheOnly(LID, load_Globals, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Neighborhood: getNeighByLID: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_Neighborhood.getNeighByLID_AttemptCacheOnly(LID, load_Globals, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_Neighborhood: getNeighByLID: got sync cached crt="+crt);
				return crt;
			}

			try {
				crt = new D_Neighborhood(LID, load_Globals);
				if (DEBUG) System.out.println("D_Neighborhood: getNeighByLID: loaded crt="+crt);
				D_Neighborhood_Node.register_loaded(crt);
				if (keep) {
					crt.status_references ++;
				}
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Neighborhood: getNeighByLID: error loading");
				// e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_Neighborhood: getNeighByLID: Done");
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
	static private D_Neighborhood getNeighByGID_AttemptCacheOnly(String GID, Long organizationLID, boolean load_Globals) {
		D_Neighborhood  crt = null;
		if ((GID == null)) return null;
		if ((GID != null)) crt = D_Neighborhood_Node.getConstByGID(GID, organizationLID); //.loaded_const_By_GID.get(GID);
		
				
		if (crt != null) {
			//crt.setGID(GID, crt.getOrganizationID());
			
			if (load_Globals && !crt.loaded_globals) {
				crt.fillGlobals();
				D_Neighborhood_Node.register_fully_loaded(crt);
			}
			D_Neighborhood_Node.setRecent(crt);
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
	static private D_Neighborhood getOrgByGIDhash_AttemptCacheOnly(String GIDhash, boolean load_Globals, boolean keep) {
		if (GIDhash == null) return null;
		if (keep) {
			synchronized(monitor_object_factory) {
				D_Neighborhood  crt = getOrgByGID_or_GIDhash_AttemptCacheOnly(null, GIDhash, load_Globals);
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
	static private D_Neighborhood getNeighByGID_AttemptCacheOnly(String GID, Long oID, boolean load_Globals, boolean keep) {
		if ((GID == null)) return null;
		if (keep) {
			synchronized (monitor_object_factory) {
				D_Neighborhood  crt = getNeighByGID_AttemptCacheOnly(GID, oID, load_Globals);
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
			return getNeighByGID_AttemptCacheOnly(GID, oID, load_Globals);
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
	static private D_Neighborhood getOrgByGID_only_AttemptCacheOnly(String GID, boolean load_Globals) {
		if (GID == null) return null;
		D_Neighborhood crt = D_Neighborhood_Node.loaded_org_By_GID.get(GID);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals){
			crt.fillGlobals();
			D_Neighborhood_Node.register_fully_loaded(crt);
		}
		D_Neighborhood_Node.setRecent(crt);
		return crt;
	}
	*/
	@Deprecated
	static public D_Neighborhood getNeighByGID(String GID, boolean load_Globals, boolean keep) {
		System.out.println("Remove me setting orgID");
		return getNeighByGID(GID, load_Globals, false, keep, null, -1);
	}
	static public D_Neighborhood getNeighByGID(String GID, boolean load_Globals, boolean keep, Long oID) {
		return getNeighByGID(GID, load_Globals, false, keep, null, oID);
	}
	/**
	 * Does not call storeRequest on creation.
	 * Therefore If create, should also set keep!
	 * @param GID
	 * @param oID
	 * @param load_Globals
	 * @param create
	 * @param keep
	 * @param __peer
	 * @return
	 */
	static public D_Neighborhood getNeighByGID(String GID, boolean load_Globals, boolean create, boolean keep, D_Peer __peer, long p_oLID) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Neighborhood: getNeighByGID: "+GID+" glob="+load_Globals+" cre="+create+" _peer="+__peer);
		if ((GID == null)) {
			if (_DEBUG) System.out.println("D_Neighborhood: getNeighByGID: null GID and GIDH");
			return null;
		}
		if (create) keep = true;
		D_Neighborhood crt = D_Neighborhood.getNeighByGID_AttemptCacheOnly(GID, p_oLID, load_Globals, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Neighborhood: getNeighByGID: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_Neighborhood.getNeighByGID_AttemptCacheOnly(GID, p_oLID, load_Globals, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_Neighborhood: getNeighByGID: got sync cached crt="+crt);
				return crt;
			}

			try {
				crt = new D_Neighborhood(GID, load_Globals, create, __peer, p_oLID);
				if (DEBUG) System.out.println("D_Neighborhood: getNeighByGID: loaded crt="+crt);
				if (keep) {
					crt.status_references ++;
				}
				D_Neighborhood_Node.register_loaded(crt);
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Neighborhood: getNeighByGID: error loading");
				e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_Neighborhood: getNeighByGID: Done");
			return crt;
		}
	}	
	/**
	 * Usable until calling releaseReference()
	 * Verify with assertReferenced()
	 * @param peer
	 * @return
	 */
	static public D_Neighborhood getNeighByNeigh_Keep(D_Neighborhood neigh) {
		if (neigh == null) return null;
		D_Neighborhood result = D_Neighborhood.getNeighByGID(neigh.getGID(), true, true, neigh.getOrgLID());
		if (result == null) {
			result = D_Neighborhood.getNeighByLID(neigh.getLID(), true, true);
		}
		if (result == null) {
			if ((neigh.getLIDstr() == null) && (neigh.getGIDH() == null)) {
				result = neigh;
				{
					neigh.status_references ++;
					//System.out.println("D_Organization: getOrgByOrg_Keep: "+org.status_references);
					//Util.printCallPath("");
				}
			}
		}
		if (result == null) {
			System.out.println("D_Organization: getOrgByOrg_Keep: got null for "+neigh);
		}
		return result;
	}
	public static long getNeighLIDByNameAndParent(String name, long parent_nID, long organizationID) {
		ArrayList<ArrayList<Object>> sel;
		try {
			if (parent_nID <= 0)
				sel = Application.db.select("select  "+table.neighborhood.fields_neighborhoods+
						" from "+table.neighborhood.TNAME+
						" where "+table.neighborhood.organization_ID+" = ? and ( "+table.neighborhood.parent_nID+" ISNULL OR "+table.neighborhood.parent_nID+" < 0 ) and "+table.neighborhood.name+" = ?;",
						new String[]{Util.getStringID(organizationID), name});
			else sel = Application.db.select("select " + table.neighborhood.fields_neighborhoods +
					" from "+table.neighborhood.TNAME+" where "+table.neighborhood.organization_ID+" = ? and "+table.neighborhood.parent_nID+" = ? and "+table.neighborhood.name+" = ?;",
					new String[]{Util.getStringID(organizationID), Util.getStringID(parent_nID), name});
			if (sel.size() != 0)
				return Util.lval(sel.get(0).get(table.neighborhood.IDX_ID));
		} catch(Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	public static int getNeighCount(long oID) {
		   String sql = "SELECT count(*) FROM "+table.neighborhood.TNAME+" where "+table.neighborhood.organization_ID+"=?;";
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
	
	/**
	 * Returns a single leaf (not having descendants)
	 * @param organization_ID2
	 * @return
	 */
	static public long getLeaves(long organization_ID2) {
		String sql1 = "SELECT n."+table.neighborhood.neighborhood_ID +
				" FROM "+table.neighborhood.TNAME+" AS n " +
				" LEFT JOIN "+table.neighborhood.TNAME+" AS d ON(d."+table.neighborhood.parent_nID+"=n."+table.neighborhood.neighborhood_ID+") "+
				" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=n."+table.neighborhood.organization_ID+") "+
				" WHERE o."+table.organization.organization_ID+"=? AND d."+table.neighborhood.neighborhood_ID+" IS NULL;"
				;
		if(DEBUG)System.out.println("select_neighborhood_or_create_by_cID : sql1:"+sql1);
		if(DEBUG)System.out.println("select_neighborhood_or_create_by_cID : Org_id:"+organization_ID2);
		ArrayList<ArrayList<Object>> a;
		try {
			a = Application.db.select(sql1, new String[]{Util.getStringID(organization_ID2)}, DEBUG);
			if(DEBUG)System.out.println("a:"+a);
			long n_ID = Long.parseLong(a.get(0).get(0).toString());
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	/** Storing */
	public static D_Neighborhood_SaverThread saverThread = new D_Neighborhood_SaverThread();
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
	public static int isGIDavailable(String gID, long oID, boolean DBG) throws P2PDDSQLException {
		D_Neighborhood n = D_Neighborhood.getNeighByGID(gID, true, false, oID);
		if (n == null) return 0;
		if (n.isTemporary()) return -1;
		return 1;
	}
	public static long _insertTemporaryNeighborhoodGID(String neighborhood_GID,
			String org_ID, long _default, D_Peer __peer) throws P2PDDSQLException {
		D_Neighborhood n = D_Neighborhood.getNeighByGID(neighborhood_GID, true, true, true, __peer, Util.lval(org_ID));
		n.setTemporary();
		n.storeRequest();
		n.releaseReference();
		return n.getLID_force();
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
		
		String save_key = this.getGIDH();
		
		if (save_key == null) {
			//save_key = ((Object)this).hashCode() + "";
			//Util.printCallPath("Cannot store null:\n"+this);
			//return;
			D_Neighborhood._need_saving_obj.add(this);
			if (DEBUG) System.out.println("D_Peer:storeRequest: added to _need_saving_obj");
		} else {		
			if (DEBUG) System.out.println("D_Organization:storeRequest: GIDH="+save_key);
			D_Neighborhood._need_saving.add(this);
		}
		try {
			if (!saverThread.isAlive()) { 
				if (DEBUG) System.out.println("D_Peer:storeRequest:startThread");
				saverThread.start();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * This function has to be called after the appropriate dirty flags are set:
	 * dirty_main - for elements in the peer table
	 * dirty_instances - ...
	 * dirty_served_orgs - ...
	 * dirty_my_data - ...
	 * dirty_addresses - ...
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
	

	@Override
	public D_Neighborhood decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		/*
		if(dec.getTypeByte()==Encoder.TAG_PrintableString)setGIDOrg(dec.getFirstObject(true).getString());
		if(dec.getTypeByte()==DD.TAG_AC0)setGID(dec.getFirstObject(true).getString(DD.TAG_AC0));
		if(dec.getTypeByte()==DD.TAG_AC1)setName(dec.getFirstObject(true).getString(DD.TAG_AC1));
		if(dec.getTypeByte()==DD.TAG_AC2)setName_lang(dec.getFirstObject(true).getString(DD.TAG_AC2));
		if(dec.getTypeByte()==DD.TAG_AC3)setDescription(dec.getFirstObject(true).getString(DD.TAG_AC3));
		if(dec.getTypeByte()==Encoder.TAG_SEQUENCE) setBoundary(dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new ASNPoint[]{}, new ASNPoint()));
		if(dec.getTypeByte()==DD.TAG_AC4)setName_division(dec.getFirstObject(true).getString(DD.TAG_AC4));
		if(dec.getTypeByte()==DD.TAG_AC6)setNames_subdivisions(dec.getFirstObject(true).getSequenceOf(DD.TAG_AC5));
		if(dec.getTypeByte()==DD.TAG_AC7)setParent_GID(dec.getFirstObject(true).getString(DD.TAG_AC7));
		if(dec.getTypeByte()==DD.TAG_AC8)parent = new D_Neighborhood().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC9)setSubmitter_GID(dec.getFirstObject(true).getString(DD.TAG_AC9));
		if(dec.getTypeByte()==DD.TAG_AC10)submitter = D_Constituent.getEmpty().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC11)setCreationDate(dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC11));
		if(dec.getTypeByte()==DD.TAG_AC12)setPicture(dec.getFirstObject(true).getBytes(DD.TAG_AC12));
		if(dec.getTypeByte()==DD.TAG_AC13)setSignature(dec.getFirstObject(true).getBytes(DD.TAG_AC13));
		*/
		// sets without setters to avoid marking dirty flags;
		if(dec.getTypeByte()==Encoder.TAG_PrintableString) this.global_organization_ID = (dec.getFirstObject(true).getString());
		if(dec.getTypeByte()==DD.TAG_AC0) this.global_neighborhood_ID = (dec.getFirstObject(true).getString(DD.TAG_AC0));
		if(dec.getTypeByte()==DD.TAG_AC1) this.name = (dec.getFirstObject(true).getString(DD.TAG_AC1));
		if(dec.getTypeByte()==DD.TAG_AC2) this.name_lang = (dec.getFirstObject(true).getString(DD.TAG_AC2));
		if(dec.getTypeByte()==DD.TAG_AC3) this.description = (dec.getFirstObject(true).getString(DD.TAG_AC3));
		if(dec.getTypeByte()==Encoder.TAG_SEQUENCE) boundary = (dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new ASNPoint[]{}, new ASNPoint()));
		if(dec.getTypeByte()==DD.TAG_AC4) this.name_division = (dec.getFirstObject(true).getString(DD.TAG_AC4));
		if(dec.getTypeByte()==DD.TAG_AC6) this.names_subdivisions = (dec.getFirstObject(true).getSequenceOf(DD.TAG_AC5));
		if(dec.getTypeByte()==DD.TAG_AC7) this.parent_global_ID = (dec.getFirstObject(true).getString(DD.TAG_AC7));
		if(dec.getTypeByte()==DD.TAG_AC8) parent = new D_Neighborhood().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC9) this.submitter_global_ID = (dec.getFirstObject(true).getString(DD.TAG_AC9));
		if(dec.getTypeByte()==DD.TAG_AC10) submitter = D_Constituent.getEmpty().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC11) this.creation_date = (dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC11));
		if(dec.getTypeByte()==DD.TAG_AC12) this.picture = (dec.getFirstObject(true).getBytes(DD.TAG_AC12));
		if(dec.getTypeByte()==DD.TAG_AC13) this.signature = (dec.getFirstObject(true).getBytes(DD.TAG_AC13));
		return this;
	}
	/**
	 * ASN1 type of the structure returned by getEncoder
	 * @return
	 */
	public static byte getASN1Type() {
		return Encoder.TAG_SEQUENCE;
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
		Encoder enc = new Encoder().initSequence();
		if (this.getOrgGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getOrgGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString));
		}
		if (getGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getGID());
			enc.addToSequence(new Encoder(repl_GID, Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		}
		if(getName()!=null) enc.addToSequence(new Encoder(getName()).setASN1Type(DD.TAG_AC1));
		if(getName_lang()!=null)enc.addToSequence(new Encoder(getName_lang(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(getDescription()!=null) enc.addToSequence(new Encoder(getDescription()).setASN1Type(DD.TAG_AC3));
		if(getBoundary()!=null) enc.addToSequence(Encoder.getEncoder(getBoundary()));
		if(getName_division()!=null) enc.addToSequence(new Encoder(getName_division()).setASN1Type(DD.TAG_AC4));
		if(getNames_subdivisions()!=null) enc.addToSequence(Encoder.getStringEncoder(getNames_subdivisions(), DD.TAG_AC5).setASN1Type(DD.TAG_AC6));
		if (getParent_GID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getParent_GID());
			enc.addToSequence(new Encoder(repl_GID).setASN1Type(DD.TAG_AC7));
		}
		//else if((parent!=null)&&(parent.global_neighborhood_ID!=null)) enc.addToSequence(new Encoder(parent.global_neighborhood_ID).setASN1Type(DD.TAG_AC7));
		if (parent != null) enc.addToSequence(parent.getEncoder(dictionary_GIDs)).setASN1Type(DD.TAG_AC8);
		if (getSubmitter_GID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getSubmitter_GID());
			enc.addToSequence(new Encoder(repl_GID).setASN1Type(DD.TAG_AC9));
		}
		//else if((submitter!=null)&&(submitter.global_ID!=null)) enc.addToSequence(new Encoder(submitter.global_ID).setASN1Type(DD.TAG_AC9));
		if (submitter!=null) enc.addToSequence(submitter.getEncoder(dictionary_GIDs)).setASN1Type(DD.TAG_AC10);
		if(getCreationDate()!=null)enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC11));
		if(getPicture()!=null) enc.addToSequence(new Encoder(getPicture()).setASN1Type(DD.TAG_AC12));
		if(getSignature()!=null)enc.addToSequence(new Encoder(getSignature()).setASN1Type(DD.TAG_AC13));
		return enc;
	}
	public Encoder getSignableEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(this.getOrgGID(),Encoder.TAG_PrintableString));
		if(getGID()!=null)enc.addToSequence(new Encoder(getGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(getName()!=null) enc.addToSequence(new Encoder(getName()).setASN1Type(DD.TAG_AC1));
		if(getName_lang()!=null)enc.addToSequence(new Encoder(getName_lang(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(getDescription()!=null) enc.addToSequence(new Encoder(getDescription()).setASN1Type(DD.TAG_AC3));
		if(getBoundary()!=null) enc.addToSequence(Encoder.getEncoder(getBoundary()));
		if(getName_division()!=null) enc.addToSequence(new Encoder(getName_division()).setASN1Type(DD.TAG_AC4));
		if(getNames_subdivisions()!=null) enc.addToSequence(Encoder.getStringEncoder(getNames_subdivisions(), DD.TAG_AC5).setASN1Type(DD.TAG_AC6));
		if(getParent_GID()!=null) enc.addToSequence(new Encoder(getParent_GID()).setASN1Type(DD.TAG_AC7));
		else if((parent!=null)&&(parent.getGID()!=null)) enc.addToSequence(new Encoder(parent.getGID()).setASN1Type(DD.TAG_AC7));
		//if(parent!=null) enc.addToSequence(parent.getEncoder()).setASN1Type(DD.TAG_AC8);
		if(getSubmitter_GID()!=null) enc.addToSequence(new Encoder(getSubmitter_GID()).setASN1Type(DD.TAG_AC9));
		else if((submitter!=null)&&(submitter.global_constituent_id!=null)) enc.addToSequence(new Encoder(submitter.global_constituent_id).setASN1Type(DD.TAG_AC9));
		//if(submitter!=null) enc.addToSequence(submitter.getEncoder()).setASN1Type(DD.TAG_AC10);
		if(getCreationDate()!=null)enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC11));
		if(getPicture()!=null) enc.addToSequence(new Encoder(getPicture()).setASN1Type(DD.TAG_AC12));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC13));
		return enc;
	}
	/**
	 * H(name,name_lang,description,boundary,name_divisions,names_subdivisions,parent_global_ID,picture)
	 * @param orgGID
	 * @return
	 */
	public Encoder getHashEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(this.getOrgGID(),Encoder.TAG_PrintableString));
		//if(global_neighborhood_ID!=null)enc.addToSequence(new Encoder(global_neighborhood_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(getName()!=null) enc.addToSequence(new Encoder(getName()).setASN1Type(DD.TAG_AC1));
		if(getName_lang()!=null)enc.addToSequence(new Encoder(getName_lang(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(getDescription()!=null) enc.addToSequence(new Encoder(getDescription()).setASN1Type(DD.TAG_AC3));
		if(getBoundary()!=null) enc.addToSequence(Encoder.getEncoder(getBoundary()));
		if(getName_division()!=null) enc.addToSequence(new Encoder(getName_division()).setASN1Type(DD.TAG_AC4));
		if(getNames_subdivisions()!=null) enc.addToSequence(Encoder.getStringEncoder(getNames_subdivisions(), DD.TAG_AC5).setASN1Type(DD.TAG_AC6));
		if(getParent_GID()!=null) enc.addToSequence(new Encoder(getParent_GID()).setASN1Type(DD.TAG_AC7));
		else if((parent!=null)&&(parent.getGID()!=null)) enc.addToSequence(new Encoder(parent.getGID()).setASN1Type(DD.TAG_AC7));
		//if(parent!=null) enc.addToSequence(parent.getEncoder()).setASN1Type(DD.TAG_AC8);
		//if(submitter_global_ID!=null) enc.addToSequence(new Encoder(submitter_global_ID).setASN1Type(DD.TAG_AC9));
		//else if((submitter!=null)&&(submitter.global_constituent_id!=null)) enc.addToSequence(new Encoder(submitter.global_constituent_id).setASN1Type(DD.TAG_AC9));
		//if(submitter!=null) enc.addToSequence(submitter.getEncoder()).setASN1Type(DD.TAG_AC10);
		//if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC11));
		if(getPicture()!=null) enc.addToSequence(new Encoder(getPicture()).setASN1Type(DD.TAG_AC12));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC13));
		return enc;
	}
	
	public byte[] sign(SK sk){
	//	return sign(sk, this.global_organization_ID);
	//}
	//public byte[] sign(SK sk, String orgGID){
		if(DEBUG) System.out.println("WB_Neighborhood:sign: start");
		if(DEBUG) System.out.println("WB_Neighborhood:sign: this="+this);
		//if(DEBUG) System.out.println("WB_Neighborhood:sign: this="+this+"\nsk="+sk+"\norgID=\""+orgGID+"\"");
		if(DEBUG) System.out.println("WB_Neighborhood:sign: sk="+sk);
		if(DEBUG) System.out.println("WB_Neighborhood:sign: orgGID=\""+this.getOrgGID()+"\"");
		if (this.creation_date == null) this.setCreationDate(Util.CalendargetInstance());
		//this.global_organization_ID = orgGID;
		if (this.getGID() == null) {
			this.setGID(make_ID());
			if (DEBUG) System.out.println("WB_Neighborhood:sign: nGID="+this.getGID());
		}
		if(DEBUG) System.out.println("WB_Neighborhood:sign: this="+this+"\norgGID="+this.getOrgGID());
		byte[] msg = this.getSignableEncoder().getBytes();
		if(DEBUG) System.out.println("\nWB_Neighborhood:sign:got hash="+Util.byteToHexDump(msg)+"\n\tsk="+sk);
		setSignature(Util.sign(msg, sk));
		if(DEBUG) System.out.println("\nWB_Neighborhood:sign:got this="+Util.byteToHexDump(getSignature()));
		if(DEBUG) System.out.println("\nWB_Neighborhood:sign: equiv pk="+sk.getPK());
		if(DD.VERIFY_AFTER_SIGNING_NEIGHBORHOOD)if(!verifySignature()) Util.printCallPath("Fail to test signature");
		return getSignature();
	}
	/*
	public String make_ID(){
		return make_ID(this.global_organization_ID);
	}
	*/
	/**
	 * Sets global_neigh_ID = null
	 * Does not consider submitter,
	 *  creation_date,
	 *  submitter_global_ID
	 * @param orgGID
	 * @return
	 */
	public String make_ID() {
		//boolean DEBUG = true;
		//String orgGID = this.getOrgGID();
		//if(DEBUG) System.out.println("WB_Neighborhood:make_ID: orgGID=\""+orgGID+"\"");
		if(DEBUG) System.out.println("WB_Neighborhood:make_ID: this=\""+this+"\"");
		fillGlobals();
		
		/*
		if ((this.parent_ID != null) && (this.parent_global_ID == null)){
			try {
				this.parent_global_ID = D_Neighborhood.getGlobalID(this.parent_ID);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		*/
		
		//Calendar _creation_date=this.creation_date; // date should not differentiate between neighborhoods
		//String _submitter_global_ID = submitter_global_ID; // submitter should not differentiate between neighborhoods
		//D_Constituent _submitter = submitter;
		//String GID = this.global_neighborhood_ID;
		
		//this.global_neighborhood_ID  = null; // this will be created as result
		//this.creation_date = null;
		//submitter_global_ID=null;
		//submitter=null;
		
		byte[] data = this.getHashEncoder().getBytes();
		
		//this.creation_date = _creation_date;
		//this.submitter_global_ID = _submitter_global_ID;
		//this.submitter = _submitter;
		//this.global_neighborhood_ID = GID;
		
		if(DEBUG) System.out.println("WB_Neighborhood:make_ID: data="+Util.byteToHex(data));
		String gid = D_GIDH.d_Neigh + Util.getGID_as_Hash(data);
		if(DEBUG) System.out.println("WB_Neighborhood:make_ID: gid="+gid);
		return gid;
	}	
	public boolean verifySignature() {
		String orgGID = this.getOrgGID();
		if(DEBUG) System.out.println("D_Neighborhood: verifySign: orgGID=\""+this.getOrgGID()+"\"");
		String old_orgGID = this.global_organization_ID;
		
		fillGlobals();
		if(_DEBUG) System.out.println("D_Neighborhood:verifySign: cmp ORGID neworgGID=\""+this.getOrgGID()+"\" vs "+old_orgGID);

		//Util.printCallPath("recursive?");
		String old_GID = this.global_neighborhood_ID;
		String tID = make_ID();
		if (!tID.equals(old_GID)) {
			this.global_neighborhood_ID = old_GID;
			if (_DEBUG) System.out.println("D_Neighborhood: verifySign: wrong GID:"+old_GID+" vs newGID="+tID);
			Util.printCallPath("Wrong NeighGID");
			if (_DEBUG) System.out.println("D_Neighborhood: verifySign: wrong GID for"+this);
			return false;
		}
		
		String pk_ID = this.submitter_global_ID;
		if ((pk_ID == null) && (this.submitter != null) && (this.submitter.global_constituent_id != null))
			pk_ID = this.submitter.global_constituent_id;
		if (pk_ID == null){
			if(_DEBUG) System.out.println("D_Neighborhood: verifySign: unknown submitter");
			return false;
		}
		//this.global_organization_ID = orgGID;
		if (DEBUG) System.out.println("D_Neighborhood: verifySign: neigh=\""+this+"\""+"\norgGID="+orgGID);
		if (DEBUG) System.out.println("D_Neighborhood: verifySign: pk_ID=\""+pk_ID+"\"");
		if (DEBUG) System.out.println("D_Neighborhood: verifySign: sign=\""+Util.byteToHexDump(signature)+"\"");
		byte[] msg = this.getSignableEncoder().getBytes();
		if (DEBUG) System.out.println("\nD_Neighborhood: verifySign:got this="+Util.byteToHexDump(msg));
		boolean result = Util.verifySignByID(msg, pk_ID, signature);
		if (!result)
			if (_DEBUG) System.out.println("\nD_Neighborhood: verifySign: failed this="+Util.byteToHexDump(msg)+"\n\t pk="+pk_ID+"\n\t obj="+this+" oldOGID="+old_orgGID);
		return result;
	}
	private void fillGlobals() {
		if (this.loaded_globals) return;

		if ((this.organization_ID > 0 ) && (this.global_organization_ID == null))
			this.global_organization_ID = D_Organization.getGIDbyLIDstr(Util.getStringID(this.organization_ID));

		if ((this.submitter_ID != null ) && (this.submitter_global_ID == null))
			this.submitter_global_ID = D_Constituent.getGIDFromLID(this.submitter_ID);

		if ((this.parent_ID != null ) && (this.parent_global_ID == null))
			this.parent_global_ID = D_Neighborhood.getGIDFromLID(this.parent_ID);
		
		this.loaded_globals = true;
	}
	public static String getGIDFromLID(String nlID) {
		return getGIDFromLID(Util.lval(nlID));
	}
	public static String getGIDFromLID(long n_lID) {
		if (n_lID <= 0) return null;
		Long LID = new Long(n_lID);
		D_Neighborhood c = D_Neighborhood.getNeighByLID(LID, true, false);
		if (c == null) {
			if (_DEBUG) System.out.println("D_Neighborhood: getGIDFromLID: null for nLID = "+n_lID);
			for (Long l : D_Neighborhood_Node.loaded_const_By_LocalID.keySet()) {
				if (_DEBUG) System.out.println("D_Neighborhood: getGIDFromLID: available ["+l+"]"+D_Neighborhood_Node.loaded_const_By_LocalID.get(l).getGIDH());
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
		D_Neighborhood c = D_Neighborhood.getNeighByGID(GID2, true, false, oID);
		return c.getLID();
	}
	public static String getLIDstrFromGID(String GID2, Long oID) {
		if (GID2 == null) return null;
		D_Neighborhood c = D_Neighborhood.getNeighByGID(GID2, true, false, oID);
		return c.getLIDstr();
	}
	public static String getLIDstrFromGID_or_GIDH(
			String GID2, String GIDH2, Long oID) {
		if (GID2 == null) return null;
		D_Neighborhood c = D_Neighborhood.getNeighByGID(GID2, true, false, oID);
		return c.getLIDstr();
	}
	public static long getLIDFromGID_or_GIDH(
			String GID2, String GIDH2, Long oID) {
		if (GID2 == null) return -1;
		D_Neighborhood c = D_Neighborhood.getNeighByGID(GID2, true, false, oID);
		return c.getLID();
	}

	/**
	 * Creation Date
	 * @return
	 */
	public Calendar getCreationDate() {
		return creation_date;
	}
	public String getCreationDateStr() {
		return _creation_date;
	}
	public void setCreationDate(Calendar creation_date) {
		this.creation_date = creation_date;
		this._creation_date = Encoder.getGeneralizedTime(creation_date);
		this.dirty_main = true;
	}
	public void setCreationDateStr(String _creation_date) {
		this._creation_date = _creation_date;
		this.creation_date = Util.getCalendar(_creation_date); //Encoder.getGeneralizedTime(creation_date);
		this.dirty_main = true;
	}
	public void setCreationDate(Calendar creation_date, String _creation_date) {
		if (creation_date == null) setCreationDateStr(_creation_date);
		if (_creation_date == null) setCreationDate(creation_date);
		this.creation_date = creation_date;
		this._creation_date = _creation_date;
	}

	/**
	 * Arrival Date
	 * @return
	 */
	public Calendar getArrivalDate() {
		return arrival_date;
	}
	public String getArrivalDateStr() {
		return _arrival_date;
	}
	public void setArrivalDate(Calendar arrival_date) {
		this.arrival_date = arrival_date;
		this._arrival_date = Encoder.getGeneralizedTime(arrival_date);
		this.dirty_local = true;
	}
	public void setArrivalDateStr(String _arrival_date) {
		this._arrival_date = _arrival_date;
		this.arrival_date = Util.getCalendar(_arrival_date); //Encoder.getGeneralizedTime(creation_date);
		this.dirty_local = true;
	}
	public void setArrivalDate(Calendar arrival_date, String _arrival_date) {
		if (arrival_date == null) setCreationDateStr(_arrival_date);
		if (_arrival_date == null) setCreationDate(arrival_date);
		this.arrival_date = arrival_date;
		this._arrival_date = _arrival_date;
		this.dirty_local = true;
	}
	
	/**
	 * Arrival Date
	 * @return
	 */
	public Calendar getPreferencesDate() {
		return preferences_date;
	}
	public String getPreferencesDateStr() {
		return _preferences_date;
	}
	public void setPreferencesDate(Calendar preferences_date) {
		this.preferences_date = preferences_date;
		this._preferences_date = Encoder.getGeneralizedTime(preferences_date);
		this.dirty_local = true;
	}
	public void setPreferencesDateStr(String _preferences_date) {
		this._preferences_date = _preferences_date;
		this.preferences_date = Util.getCalendar(_preferences_date); //Encoder.getGeneralizedTime(creation_date);
		this.dirty_local = true;
	}
	public void setPreferencesDate(Calendar preferences_date, String _preferences_date) {
		if (preferences_date == null) setCreationDateStr(_preferences_date);
		if (_preferences_date == null) setCreationDate(preferences_date);
		this.preferences_date = preferences_date;
		this._preferences_date = _preferences_date;
		this.dirty_local = true;
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
		//System.out.println("D_Neighborhood: releaseReference: "+status_references);
		//Util.printCallPath("");
	}
	
	public void assertReferenced() {
		assert (status_references > 0);
	}
	
	/**
	 * The entries that need saving
	 */
	private static HashSet<D_Neighborhood> _need_saving = new HashSet<D_Neighborhood>();
	private static HashSet<D_Neighborhood> _need_saving_obj = new HashSet<D_Neighborhood>();
	
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Neighborhood> 
	set_DDP2P_DoubleLinkedList_Node(DDP2P_DoubleLinkedList_Node<D_Neighborhood> node) {
		DDP2P_DoubleLinkedList_Node<D_Neighborhood> old = this.component_node.my_node_in_loaded;
		if (DEBUG) System.out.println("D_Neighborhood: set_DDP2P_DoubleLinkedList_Node: set = "+ node);
		this.component_node.my_node_in_loaded = node;
		return old;
	}
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Neighborhood> 
	get_DDP2P_DoubleLinkedList_Node() {
		if (DEBUG) System.out.println("D_Neighborhood: get_DDP2P_DoubleLinkedList_Node: get");
		return component_node.my_node_in_loaded;
	}
	static void need_saving_remove(D_Neighborhood c) {
		if (DEBUG) System.out.println("D_Neighborhood:need_saving_remove: remove "+c);
		_need_saving.remove(c);
		if (DEBUG) dumpNeedsObj(_need_saving);
	}
	static void need_saving_obj_remove(D_Neighborhood org) {
		if (DEBUG) System.out.println("D_Neighborhood:need_saving_obj_remove: remove "+org);
		_need_saving_obj.remove(org);
		if (DEBUG) dumpNeedsObj(_need_saving_obj);
	}
	static D_Neighborhood need_saving_next() {
		Iterator<D_Neighborhood> i = _need_saving.iterator();
		if (!i.hasNext()) return null;
		D_Neighborhood c = i.next();
		if (DEBUG) System.out.println("D_Neighborhood: need_saving_next: next: "+c);
		//D_Neighborhood r = D_Neighborhood_Node.getConstByGIDH(c, null);//.loaded_const_By_GIDhash.get(c);
		if (c == null) {
			if (_DEBUG) {
				System.out.println("D_Neighborhood Cache: need_saving_next null entry "
						+ "needs saving next: "+c);
				System.out.println("D_Neighborhood Cache: "+dumpDirCache());
			}
			return null;
		}
		return c;
	}
	static D_Neighborhood need_saving_obj_next() {
		Iterator<D_Neighborhood> i = _need_saving_obj.iterator();
		if (!i.hasNext()) return null;
		D_Neighborhood r = i.next();
		if (DEBUG) System.out.println("D_Neighborhood: need_saving_obj_next: next: "+r);
		//D_Neighborhood r = D_Neighborhood_Node.loaded_org_By_GIDhash.get(c);
		if (r == null) {
			if (_DEBUG) {
				System.out.println("D_Neighborhood Cache: need_saving_obj_next null entry "
						+ "needs saving obj next: "+r);
				System.out.println("D_Neighborhood Cache: "+dumpDirCache());
			}
			return null;
		}
		return r;
	}
	private static void dumpNeeds(HashSet<String> _need_saving2) {
		System.out.println("Needs:");
		for ( String i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	private static void dumpNeedsObj(HashSet<D_Neighborhood> _need_saving2) {
		System.out.println("Needs:");
		for ( D_Neighborhood i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	public static String dumpDirCache() {
		String s = "[";
		s += D_Neighborhood_Node.loaded_consts.toString();
		s += "]";
		return s;
	}
	public boolean isBlocked() {
		return blocked;
	}
	public void setBlocked(boolean blocked) {
		this.blocked = blocked;
		this.dirty_preferences = true;
	}
	public boolean isRequested() {
		return requested;
	}
	public void setRequested(boolean requested) {
		this.requested = requested;
		this.dirty_preferences = true;
	}
	public boolean isBroadcasted() {
		return broadcasted;
	}
	public void setBroadcasted(boolean broadcasted) {
		this.broadcasted = broadcasted;
		this.dirty_preferences = true;
	}
	public byte[] getSignature() {
		return signature;
	}
	public void setSignature(byte[] signature) {
		this.signature = signature;
		this.dirty_main = true;
	}
	public String getGIDH() {
		return this.global_neighborhood_ID;
	}
	public String getGID() {
		return global_neighborhood_ID;
	}
	public void setGID(String global_neighborhood_ID) {
		boolean loaded_in_cache = this.isLoaded();
		this.global_neighborhood_ID = global_neighborhood_ID;
		this.dirty_main = true;
		Long oID = this.organization_ID;
		if (loaded_in_cache) {
			if (this.getGID() != null)
				D_Neighborhood_Node.putConstByGID(this.getGID(), oID, this); //.loaded_const_By_GID.put(this.getGID(), this);
			if (this.getGIDH() != null)
				D_Neighborhood_Node.putConstByGIDH(this.getGIDH(), oID, this); //.loaded_const_By_GIDhash.put(this.getGIDH(), this);
		}
	}
	public boolean isLoaded() {
		String GIDH, GID;
		if (!D_Neighborhood_Node.loaded_consts.inListProbably(this)) return false;
		long lID = this.getLID();
		long oID = this.getOrgLID();
		if (lID > 0)
			if ( null != D_Neighborhood_Node.loaded_const_By_LocalID.get(new Long(lID)) ) return true;
		if ((GIDH = this.getGIDH()) != null)
			if ( null != D_Neighborhood_Node.getConstByGIDH(GIDH, oID) //.loaded_const_By_GIDhash.get(GIDH)
			) return true;
		if ((GID = this.getGID()) != null)
			if ( null != D_Neighborhood_Node.getConstByGID(GID, oID)  //.loaded_const_By_GID.get(GID)
			) return true;
		return false;
	}
	public String getNameOrMy() {
		String n = mydata.name;
		if (n != null) return n;
		return getName();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
		this.dirty_main = true;
	}
	public void setNameMy(String name) {
		this.mydata.name = name;
		this.dirty_mydata = true;
	}
	public String getNameMy() {
		return this.mydata.name;
	}
	public void setCategoryMy(String cat) {
		this.mydata.category = cat;
		this.dirty_mydata = true;
	}
	public String getCategoryMy() {
		return this.mydata.category;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
		this.dirty_main = true;
	}
	public String getParent_GID() {
		return parent_global_ID;
	}
	public void setParent_GID(String parent_global_ID) {
		this.parent_global_ID = parent_global_ID;
		this.dirty_main = true;
	}
	public String getSubmitter_GID() {
		return submitter_global_ID;
	}
	public void setSubmitter_GID(String submitter_global_ID) {
		this.submitter_global_ID = submitter_global_ID;
		this.dirty_main = true;
	}
	public byte[] getPicture() {
		return picture;
	}
	public void setPicture(byte[] picture) {
		this.picture = picture;
		this.dirty_main = true;
	}
	public String getName_division() {
		return name_division;
	}
	public void setName_division(String name_division) {
		this.name_division = name_division;
		this.dirty_main = true;
	}
	public String[] getNames_subdivisions() {
		return names_subdivisions;
	}
	public void setNames_subdivisions(String[] names_subdivisions) {
		this.names_subdivisions = names_subdivisions;
		this.dirty_main = true;
	}
	public ASNPoint[] getBoundary() {
		return boundary;
	}
	public void setBoundary(ASNPoint[] boundary) {
		this.boundary = boundary;
		this.dirty_main = true;
	}
	public String getName_lang() {
		return name_lang;
	}
	public void setName_lang(String name_lang) {
		this.name_lang = name_lang;
		this.dirty_main = true;
	}
	public long getLID() {
		return _neighborhoodID;
	}
	public long getLID_force() {
		if (this._neighborhoodID >= 0) return _neighborhoodID;
		return this.storeSynchronouslyNoException();
	}
	public String getLIDstr() {
		return this.neighborhoodID;
	}
	/**
	 * Not setting dirty (used in init and save)
	 * @param neighborhoodID
	 */
	public void setLIDstr(String neighborhoodID) {
		this.neighborhoodID = neighborhoodID;
		this._neighborhoodID = Util.lval(neighborhoodID);
	}
	/**
	 * Not setting dirty (used in init and save)
	 * @param _neighborhoodID
	 */
	public void setLID(long _neighborhoodID) {
		this.neighborhoodID = Util.getStringID(_neighborhoodID);
		this._neighborhoodID = _neighborhoodID;
	}
	public String getOrgGID() {
		return this.global_organization_ID;
	}
	public void setOrgGID(String global_organization_ID) {
		this.global_organization_ID = global_organization_ID;
		this.dirty_main = true;
	}
	public long getOrgLID() {
		return organization_ID;
	}
	public String getOrgLIDstr() {
		return Util.getStringID(organization_ID);
	}
	public void setOrgLID(long organization_ID) {
		this.organization_ID = organization_ID;
		this.dirty_main = true;
	}
	public long getSubmitterLID() {
		return Util.lval(this.getSubmitterLIDstr(),-1);
	}
	public String getSubmitterLIDstr() {
		return submitter_ID;
	}
	public void setSubmitterLIDstr(String submitter_ID) {
		this.submitter_ID = submitter_ID;
		this.dirty_main = true;
	}
	public long getParentLID() {
		return Util.lval(this.getParentLIDstr(),-1);
	}
	public String getParentLIDstr() {
		return parent_ID;
	}
	public void setParentLIDstr(String parent_ID) {
		this.parent_ID = parent_ID;
		//this.global_neighborhood_ID = D_Neighborhood.getGIDFromLID(Util.lval(parent_ID));
		this.dirty_main = true;
	}
	public long getPeerSourceLID() {
		return Util.lval(peer_source_ID);
	}
	public String getPeerSourceLIDstr() {
		return peer_source_ID;
	}
	public void setPeerSourceLID(String peer_source_ID) {
		this.peer_source_ID = peer_source_ID;
		this.dirty_local = true;
	}
	public String getName_charset() {
		return name_charset;
	}
	public void setName_charset(String name_charset) {
		this.name_charset = name_charset;
		this.dirty_main = true;
	}
	public long storeAct() throws P2PDDSQLException {
		if (this.dirty_local || this.dirty_main || this.dirty_preferences)
			storeAct_main();
		if (this.dirty_mydata)
			storeAct_my();
		return this.getLID();
	}
	private void storeAct_my() {
		this.dirty_mydata = false;
		String param[];
		if (this.mydata.row <= 0) {
			param = new String[table.my_neighborhood_data.FIELDS_NB_NOID];
		} else {
			param = new String[table.my_neighborhood_data.FIELDS_NB];
		}
		param[table.my_neighborhood_data.COL_NAME] = this.mydata.name;
		param[table.my_neighborhood_data.COL_CATEGORY] = this.mydata.category;
		//param[table.my_neighborhood_data.COL_SUBMITTER] = this.n_my.submitter;
		param[table.my_neighborhood_data.COL_NEIGHBORHOOD_LID] = this.getLIDstr();
		try {
			if (this.mydata.row <= 0) {
				this.mydata.row =
						Application.db.insert(true, table.my_neighborhood_data.TNAME,
								table.my_neighborhood_data.fields_noID, param, DEBUG);
			} else {
				param[table.my_neighborhood_data.COL_ROW] = this.mydata.row+"";
				Application.db.update(true, table.my_neighborhood_data.TNAME,
						table.my_neighborhood_data.fields_noID,
						new String[]{table.my_neighborhood_data.neighborhood_ID}, param, DEBUG);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public long storeAct_main() throws P2PDDSQLException {
		long result = -1;
		boolean sync = true;
		String id = this.getLIDstr();
	   	if (DEBUG) System.err.println("ConstitentsModel:integrateNewverif: old neigh_ID="+ this.getLID()+" id="+id);

	   	String[] fields;
	   	String[] params;
	   	fields = table.neighborhood.fields_neighborhoods_noID_list;
	   	if (id == null) {
	   		params = new String[fields.length];
		} else {
			params = new String[fields.length + 1];
		}
			
	   	this.dirty_local = this.dirty_main = this.dirty_preferences = false;
			
			params[table.neighborhood.IDX_ADDRESS] = address;
			params[table.neighborhood.IDX_CREATION_DATE] = Encoder.getGeneralizedTime(this.getCreationDate());
			params[table.neighborhood.IDX_ARRIVAL_DATE] = this.getArrivalDateStr();
			params[table.neighborhood.IDX_PREFERENCES_DATE] = this.getPreferencesDateStr();
			params[table.neighborhood.IDX_GID] = this.getGID();
			params[table.neighborhood.IDX_DESCRIPTION] = this.getDescription();
			params[table.neighborhood.IDX_SIGNATURE] = Util.stringSignatureFromByte(this.getSignature());
			params[table.neighborhood.IDX_ORG_ID] = this.getOrgLIDstr();
			params[table.neighborhood.IDX_NAME] = this.getName();
			params[table.neighborhood.IDX_NAME_LANG] = this.getName_lang();
			params[table.neighborhood.IDX_NAME_CHARSET] = this.getName_charset();
			params[table.neighborhood.IDX_NAME_DIVISION] = this.getName_division();
			params[table.neighborhood.IDX_NAMES_DUBDIVISIONS] = getNames_subdivisions_str();// Util.concat(this.names_subdivisions,table.neighborhood.SEP_names_subdivisions,null);
			params[table.neighborhood.IDX_PARENT_ID] = this.getParentLIDstr();
			params[table.neighborhood.IDX_SUBMITTER_ID] = this.getSubmitterLIDstr();// submit_ID;
			params[table.neighborhood.IDX_BLOCKED] = Util.bool2StringInt(this.isBlocked());
			params[table.neighborhood.IDX_REQUESTED] = Util.bool2StringInt(this.isRequested());
			params[table.neighborhood.IDX_BROADCASTED] = Util.bool2StringInt(this.isBroadcasted());
			params[table.neighborhood.IDX_PEER_SOURCE_ID] = this.getPeerSourceLIDstr();
			if (id == null) {
				if (DEBUG) System.out.println("\nNeighborhoodHandling:integrateNewVerifiedNeighborhoodData: insert");
				long _neig_local_ID = -1;
				try {
					_neig_local_ID = Application.db.insert(sync, table.neighborhood.TNAME, fields, params, DEBUG);
					this.setLID(_neig_local_ID);
				} catch (Exception e) {
					if (_DEBUG) System.out.println("\nNeighborhoodHandling:integrateNewVerifiedNeighborhoodData: insert fail: "+this.getGID());
					id = D_Neighborhood.getLIDstrFromGID(this.getGID(), this.organization_ID);
					if (_DEBUG) System.out.println("\nNeighborhoodHandling:integrateNewVerifiedNeighborhoodData: insert reget: "+id);
					_neig_local_ID = Util.lval(id, -1);
				}
				result = this.getLID();
			} else {
				if (DEBUG) System.out.println("\nNeighborhoodHandling:integrateNewVerifiedNeighborhoodData: update");
				//if ((date[0] == null) || (date[0].compareTo(params[table.neighborhood.IDX_CREATION_DATE]) < 0)) {
				params[table.neighborhood.IDX_ID] = id;
				//params[table.neighborhood.IDX_FIELDs] = id;
				Application.db.update(sync, table.neighborhood.TNAME, fields, new String[]{table.neighborhood.neighborhood_ID}, params, DEBUG);
				//}
				result = this.getLID(); //id;
			}
			//this.setLID(id);
			//if(result!=null)if(sol_rq!=null)sol_rq.neig.add(this.getGID());

			if (DEBUG) System.out.println("NeighborhoodHandling:integrateNewVerifiedNeighborhoodData:  exit id="+id);
			return result;
	}
	/**
	 * Used for storing [val,val] into database 
	 * @param names_subdivisions
	 * @return
	 */
	public static String concatSubdivisions(String[] names_subdivisions) {
		if (names_subdivisions == null) return null;
		return ":"+Util.concat(names_subdivisions,table.neighborhood.SEP_names_subdivisions,null)+":";
	}
	/**
	 * Used when loading from :val:val: into array  ["":val:val:""] 
	 * @param subs
	 * @return
	 */
	public static String[] splitSubDivisionsFromCAND(String subs) {
		return subs.split(Pattern.quote(table.neighborhood.SEP_names_subdivisions));
	}
	/**
	 * Used when loading from database into WB_Neighbornood [val:val]
	 * @param subs
	 * @return
	 */
	public static String[] splitSubDivisions(String subs) {
		if (DEBUG) System.out.println("D_Neighborhood:splitSubDivisions: split subs="+subs);
		if (subs == null) return null;
		String[] val = subs.split(Pattern.quote(table.neighborhood.SEP_names_subdivisions));
		if (val.length < 1) return null;
		if (! "".equals(val[0])) return val; // backward compatibility: if storage was not starting with ":"
		if (val.length < 2) return null;
		String[] result = new String[val.length-1];
		for (int k = 0; k < result.length; k ++) result[k] = val[k + 1];
		if (DEBUG) System.out.println("D_Neighborhood:splitSubDivisions: got["+result.length+"]="+Util.concat(result, ":"));
		return result;
	}
	/**
	 * Returns the subdivisions after the idx-th position
	 * @param names_subdivisions
	 * @param idx
	 * @return
	 */
	public static String getChildSubDivision(String names_subdivisions, int idx) {
		if(DEBUG) System.out.println("Child Subdivisions: "+names_subdivisions);
		if (names_subdivisions == null) return null;
		String [] splits = names_subdivisions.split(table.neighborhood.SEP_names_subdivisions,idx+3);
		if (splits.length < idx + 3) return null;
		return table.neighborhood.SEP_names_subdivisions+splits[idx+2];
	}
	/**
	 * Get the neighborhood subdivision at index idx
	 * @param names_subdivisions
	 * @param idx
	 * @return
	 */
	public static String getChildDivision(String names_subdivisions, int idx) {
		if (names_subdivisions == null) return null;
		String[]splits = names_subdivisions.split(table.neighborhood.SEP_names_subdivisions,idx+3);
		if(splits.length<idx+2) return null;
		return splits[idx+1];
	}
	/**
	 * Break subdivisions and reassemble after extracting out the prefix ending with crt
	 * returns all if crt is not found
	 * @param names_subdivisions
	 * @param crt
	 * @return
	 */
	public static String _getChildSubDivision(String names_subdivisions, String crt) {
		boolean DEBUG = false;
		int k;
		String result=table.neighborhood.SEP_names_subdivisions;
		if(DEBUG) System.out.println("Util:getChildSubDivision:Child Subdivisions: "+names_subdivisions);
		if (names_subdivisions==null) return null;
		String[]splits = names_subdivisions.split(Pattern.quote(table.neighborhood.SEP_names_subdivisions));
		for(k=1; k<splits.length; k++)
			if (splits[k].equals(crt)) break;
		if(k==splits.length) return names_subdivisions;
		for(k++; k<splits.length; k++)
			if(!"".equals(splits[k])) result = result+splits[k]+table.neighborhood.SEP_names_subdivisions;
		return result;
	}
	/**
	 * Break subdivisions and reassemble after extracting out the prefix ending with crt
	 * returns all if crt is not found
	 * @param names_subdivisions
	 * @param crt
	 * @return
	 */
	public static String[] _getChildSubDivisions(String names_subdivisions, String crt) {
		boolean DEBUG = false;
		String result_c = D_Neighborhood._getChildSubDivision(names_subdivisions, crt);
		if(result_c == null) return null;
		if(DEBUG) System.out.println("Util:getChildSubDivisions: input "+names_subdivisions);
		String[] splits = result_c.split(Pattern.quote(table.neighborhood.SEP_names_subdivisions));
		if(splits.length < 2) return null;
		String[] result = new String[splits.length - 1];
		int i=1;
		for(int k=0; k<result.length; k++,i++) {
			result[k] = splits[i];
		}
		return result;
	}
	/*
	int k;
	//String result=table.neighborhood.SEP_names_subdivisions;
	if(DEBUG) System.out.println("Child Subdivisions: "+names_subdivisions);
	if (names_subdivisions==null) return null;
	String[]splits = names_subdivisions.split(table.neighborhood.SEP_names_subdivisions);
	for(k=1; k<splits.length; k++)
		if (splits[k].equals(crt)) break;
	if(k==splits.length) return splits;
	k++;
	String[] new_splits = new String[splits.length-k];
	int i=0;
	for(; k<splits.length; k++,i++)
		if(!"".equals(splits[k])) new_splits[i] = splits[k];
	return new_splits;
}
*/
	public long storeRemoteThis(String orgGID,
			long orgLID, String arrivalDateStr, RequestData sol_rq,
			RequestData new_rq, D_Peer source) {
		D_Neighborhood result = D_Neighborhood.getNeighByGID(orgGID, true, true, true, source, orgLID);
		if (result.loadRemote(this, sol_rq, new_rq, source))
			config.Application_GUI.inform_arrival(result, source);
		result.fillLocals();
		
		result.storeRequest();
		result.releaseReference();
		return result.getLID_force();
	}
	private void fillLocals() {
		if (this.organization_ID <= 0 && this.global_organization_ID != null) {
			this.organization_ID = D_Organization.getLIDbyGID(global_organization_ID);
		}
		if (this.submitter_ID == null && this.submitter_global_ID != null) {
			this.submitter_ID = D_Constituent.getLIDstrFromGID(submitter_global_ID, organization_ID);
		}
		if (this.parent_ID == null && this.parent_global_ID != null) {
			this.parent_ID = D_Neighborhood.getLIDstrFromGID(parent_global_ID, organization_ID);
		}
		
	}

	/**
	 * Returns the array of parent neighborhoods of global_neighborhood_ID
	 * @param global_neighborhood_ID
	 * @param nID :  local neighborhood ID
	 * @param _neighborhoods takes values: {EXPAND_NONE, EXPAND_ONE, EXPAND_ALL} EXPAND_NONE-not checked
	 * @param olID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_Neighborhood[] getNeighborhoodHierarchy(String global_neighborhood_ID, String nID, int _neighborhoods, long olID) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("D_Neighborhood:getNeighborhoodHierarchy: neighGID="+global_neighborhood_ID+" nID="+nID+" #="+_neighborhoods);
		if(global_neighborhood_ID==null)
			global_neighborhood_ID = D_Neighborhood.getGIDFromLID(nID);
		D_Neighborhood[] neighborhood;
		HashSet<String> nGID = new HashSet<String>();
		ArrayList<D_Neighborhood> awn = new ArrayList<D_Neighborhood>();
		String neigh_next = global_neighborhood_ID;
		do {
			if(DEBUG) System.out.println("D_Neighborhood:getNeighborhoodHierarchy: loading nGID="+neigh_next+" nID="+nID);
			if(nGID.contains(neigh_next)) {
				Util.printCallPath(Util.__("D_Neighborhood:getNeighborhoodHierarchy: Circular neighborhood detected for: GID="+neigh_next+" GID="+nID));
				awn = new ArrayList<D_Neighborhood>();
				break;
			}
			//neighborhood=new WB_Neighborhood[1];
			D_Neighborhood crt_neighborhood;
			try {
				if (nID != null)
					crt_neighborhood = D_Neighborhood.getNeighByLID(nID, true, false); //.getNeighborhood(nID, neigh_next);
				else
					crt_neighborhood = D_Neighborhood.getNeighByGID(neigh_next, true, false, olID);
				if (DEBUG) System.out.println("D_Neighborhood: getNeighborhoodHierarchy: crt = "+crt_neighborhood);
				if (crt_neighborhood == null) break;
			} catch(Exception e) {
				e.printStackTrace();
				break;
			}
			nGID.add(neigh_next);
			awn.add(crt_neighborhood);
			neigh_next = crt_neighborhood.getParent_GID();
			nID = crt_neighborhood.getParentLIDstr();
		} while((neigh_next!=null) && (nID!=null) && (_neighborhoods!=D_Constituent.EXPAND_ONE));
		neighborhood = awn.toArray(new D_Neighborhood[0]);
		return neighborhood;
	}
	/**
	 * Tests newer
	 * @param r
	 * @param sol_rq
	 * @param new_rq
	 */
	public boolean loadRemote(D_Neighborhood r, RequestData sol_rq,
			RequestData new_rq, D_Peer __peer) {
		boolean default_blocked_org = false;
		boolean default_blocked_cons = false;
		boolean default_blocked_mot = false;
		
		if (!this.isTemporary() && !newer(r, this)) return false;
		
		this._creation_date = r._creation_date;
		this.creation_date = r.creation_date;
		this.boundary = r.boundary;
		this.description = r.description;
		this.name = r.name;
		this.name_charset = r.name_charset;
		this.name_division = r.name_division;
		this.name_lang = r.name_lang;
		this.names_subdivisions = r.names_subdivisions;
		this.picture = r.picture;
		this.signature = r.picture;
		if (!Util.equalStrings_null_or_not(this.global_neighborhood_ID, r.global_neighborhood_ID)) {
			this.global_neighborhood_ID = r.global_neighborhood_ID;
			this.neighborhoodID = null;
			this._neighborhoodID = -1;
		}
		if (!Util.equalStrings_null_or_not(this.submitter_global_ID, r.submitter_global_ID)) {
			this.submitter_global_ID = r.submitter_global_ID;
			this.setSubmitterLIDstr(D_Constituent.getLIDstrFromGID(submitter_global_ID, this.getOrgLID()));
			//this.submitter = r.submitter;
			if (r.getSubmitter_GID() != null && this.getSubmitterLIDstr() == null) {
				this.submitter = D_Constituent.getConstByGID_or_GIDH(r.submitter_global_ID, null, true, true, false, __peer, this.getOrgLID());
				this.setSubmitterLIDstr(this.submitter.getLIDstr_force());
				if (new_rq != null && this.submitter.isTemporary()) new_rq.cons.put(r.submitter_global_ID, DD.EMPTYDATE);
			}
		}
		if (!Util.equalStrings_null_or_not(this.global_organization_ID, r.global_organization_ID)) {
			this.setOrgGID(r.getOrgGID());
			long oID = D_Organization.getLIDbyGID(r.global_organization_ID);
			this.setOrgLID(oID);
			if (r.getOrgGID() != null && oID <= 0) {
				D_Organization o = D_Organization.insertTemporaryGID_org(r.global_organization_ID, null, __peer, default_blocked_org, null);
				this.setOrgLID(o.getLID_forced());
				if (new_rq != null && o.isTemporary()) new_rq.orgs.add(o.getGIDH_or_guess());
			}
		}
		if (!Util.equalStrings_null_or_not(this.parent_global_ID, r.parent_global_ID)) {
			this.setParent_GID(r.getParent_GID());
			//this.setParentLIDstr(null);
			//this.parent = r.parent;
			D_Neighborhood n;
			long nID = D_Neighborhood.getLIDFromGID(this.parent_global_ID, this.getOrgLID());
			this.setParentLIDstr(Util.getStringID(nID));
			if (this.getParent_GID() != null && nID <= 0) {
				n = D_Neighborhood.getNeighByGID(this.parent_global_ID, true, true, false, __peer, this.getOrgLID());
				this.setParentLIDstr(Util.getStringID(n.getLID_force()));
				if (new_rq != null && n.isTemporary()) new_rq.neig.add(this.parent_global_ID);
			}
		}
		if (sol_rq != null) sol_rq.neig.add(this.global_neighborhood_ID);
		this.dirty_main = true;
		if (this.peer_source_ID == null && __peer != null)
			this.peer_source_ID = __peer.getLIDstr_keep_force();
		this.setTemporary(false);
		return true;
	}
	public static boolean newer(D_Neighborhood r, D_Neighborhood o) {
		if (o.getCreationDateStr() == null) return true;
		if (r.getCreationDateStr() == null) return false;
		if (r.getCreationDate().compareTo(o.getCreationDate()) > 0) return true;
		return false;
	}
	public  static boolean newer(String r, String o) {
		if (o == null) return true;
		if (r == null) return false;
		if (r.compareTo(o) > 0) return true;
		return false;
	}
	public void setOrgIDs(String giDbyLID, long organization_ID2) {
		if (giDbyLID == null && organization_ID2 > 0)
			giDbyLID = D_Organization.getGIDbyLID(organization_ID2);
		if (giDbyLID != null && organization_ID2 <= 0) {
			organization_ID2 = D_Organization.getLIDbyGID(giDbyLID);
		}
		this.setGID(giDbyLID);
		this.setOrgLID(organization_ID2);
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
	public static void setTemporary(D_Neighborhood c) {
		c = D_Neighborhood.getNeighByNeigh_Keep(c);
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
				|| ((this.signature == null || this.signature.length == 0) )
				|| this.getGID() == null;
	}
	public static long insertTemporaryGID(String p_cGID,
			long p_oLID, D_Peer __peer, boolean default_blocked) {
		D_Neighborhood neigh = D_Neighborhood.insertTemporaryGID_org(p_cGID, p_oLID, __peer, default_blocked);
		if (neigh == null) return -1;
		return neigh.getLID_force(); 
	}
	public static D_Neighborhood insertTemporaryGID_org(
			String p_cGID, long p_oLID,
			D_Peer __peer, boolean default_blocked) {
		D_Neighborhood neigh;
		if ((p_cGID != null)) {
			neigh = D_Neighborhood.getNeighByGID(p_cGID, true, true, true, __peer, p_oLID);
			//consts.setName(_name);
			neigh.setBlocked(default_blocked);
			neigh.storeRequest();
			neigh.releaseReference();
		}
		else return null;
		return neigh; 
	}
	final public static String sql_n_children =
				"SELECT " + table.neighborhood.neighborhood_ID
				+ " FROM " + table.neighborhood.TNAME
				+ " WHERE " + table.neighborhood.parent_nID + "=? "
				+ " ORDER BY "+table.neighborhood.name+" DESC"
						+ ";";
	public static ArrayList<Long> getNeighborhoodChildrenIDs(long n_ID) {
		ArrayList<Long> result = new ArrayList<Long>();
		boolean DEBUG = false;
		ArrayList<ArrayList<Object>> n;
		try {
			n = Application.db.select(sql_n_children, new String[]{Util.getStringID(n_ID)}, DEBUG);
			for (int k = 0; k < n.size(); k ++) {
				result.add(Util.Lval(n.get(k).get(0)));
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	final public static String sql_n_root_children = 
		    	"SELECT n." //+ table.neighborhood.name + ", n."
		    	+ table.neighborhood.neighborhood_ID
	    		+ " FROM "+table.neighborhood.TNAME + " AS n "+
	    		" LEFT JOIN "+table.neighborhood.TNAME + " AS p ON(n."+table.neighborhood.parent_nID+"=p."+table.neighborhood.neighborhood_ID+") "+
		    		" WHERE n."+table.neighborhood.organization_ID+" = ? AND ( n."+table.neighborhood.parent_nID+" ISNULL OR p."+table.neighborhood.neighborhood_ID+" ISNULL ) " +
//		    				" GROUP BY "+table.neighborhood.name+
		    				" ORDER BY n."+table.neighborhood.name+" DESC;";
	public static ArrayList<Long> getNeighborhoodRootsLIDs(long o_ID) {
		ArrayList<Long> result = new ArrayList<Long>();
		boolean DEBUG = false;
		ArrayList<ArrayList<Object>> n;
		try {
			n = Application.db.select(sql_n_root_children, new String[]{Util.getStringID(o_ID)}, DEBUG);
			for (int k = 0; k < n.size(); k ++) {
				result.add(Util.Lval(n.get(k).get(0)));
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return result;
	}
    static public void delNeighborhood(long nID, long oID) {
    	ArrayList<ArrayList<Object>> sel;
    	ArrayList<Long> sel_c;
    	try{
    		sel = Application.db.select("select "+table.neighborhood.neighborhood_ID+" from "+table.neighborhood.TNAME+" where "+table.neighborhood.parent_nID+"=? AND "+table.neighborhood.organization_ID+"=?;",
    				new String[]{Util.getStringID(nID),Util.getStringID(oID)});
    		for(int i=0; i<sel.size(); i++) {
    			long c_nID = Util.lval(sel.get(i).get(0),-1);
    			delNeighborhood(c_nID, oID);
    		}
    		sel_c = D_Constituent.getConstInNeighborhood(nID, oID);
    		for (int i = 0; i < sel_c.size(); i ++) {
    			long c_ID = sel_c.get(i); //Util.lval(sel.get(i).get(0),-1);
    			D_Constituent.delConstituent(c_ID);
    		}
    		Application.db.delete(table.neighborhood.TNAME, new String[]{table.neighborhood.neighborhood_ID}, new String[]{Util.getStringID(nID)});
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
	public static void zapp(long neighborhoodID2) {
		// TODO Auto-generated method stub
		try {
			Application.db.delete(table.neighborhood.TNAME,
					new String[]{table.neighborhood.neighborhood_ID},
					new String[]{Util.getStringID(neighborhoodID2)},
					DEBUG);
/*			Application.db.delete(table.constituent.TNAME,
					new String[]{table.constituent.neighborhood_ID},
					new String[]{Util.getStringID(neighborhoodID2)},
					DEBUG);
					*/
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
	public static Hashtable<String, String> checkAvailability(
			Hashtable<String, String> neigh, String orgID, boolean DBG) {
		Hashtable<String, String> result = new Hashtable<String, String>();
		for (String cHash : neigh.keySet()) {
			if(cHash == null) continue;
			if(!available(cHash, neigh.get(cHash), orgID, DBG)) result.put(cHash, DD.EMPTYDATE);
		}
		return result;
	}
	private static boolean available(String hash, String creation, String orgID, boolean DBG) {
		boolean result = true;
		
		D_Neighborhood c = D_Neighborhood.getNeighByGID (hash, true, false, Util.Lval(orgID));
		if (
				(c == null) 
				|| (c.getOrgLID() != Util.lval(orgID)) 
				|| (D_Neighborhood.newer(creation, c.getCreationDateStr()))
				|| c.isTemporary()
				) result = false;
		
		if ((c != null) && c.isBlocked()) result = true;
		if (DEBUG || DBG) System.out.println("D_Neighborhood: available: "+hash+" in "+orgID+" = "+result);
		return result;
	}
	*/
	public static ArrayList<String> checkAvailability(ArrayList<String> cons,
			String orgID, boolean DBG) throws P2PDDSQLException {
		ArrayList<String> result = new ArrayList<String>();
		for (String cHash : cons) {
			if(!available(cHash, orgID, DBG)) result.add(cHash);
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
	private static boolean available(String hash, String orgID, boolean DBG) throws P2PDDSQLException {
//		String sql = 
//			"SELECT "+table.neighborhood.neighborhood_ID+
//			" FROM "+table.neighborhood.TNAME+
//			" WHERE "+table.neighborhood.global_neighborhood_ID+"=? "+
//			" AND "+table.neighborhood.organization_ID+"=? "+
//			" AND ( "+table.neighborhood.signature + " IS NOT NULL " +
//			" OR "+table.neighborhood.blocked+" = '1');";
//		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{hash, orgID}, DEBUG);
		boolean result = true;
//		if(a.size()==0) result = false;
		D_Neighborhood c = D_Neighborhood.getNeighByGID (hash, true, false, Util.Lval(orgID));
		if (
				(c == null) 
				|| (c.getOrgLID() != Util.lval(orgID)) 
				//|| (D_Neighborhood.newer(creation, c.getCreationDateStr()))
				|| ( c.isTemporary() && !c.isBlocked() )
				) result = false;
		
		//if ((c != null) ) result = true;//&& c.isBlocked()
		if (DEBUG || DBG) System.out.println("D_Neighborhood: available: "+hash+" in "+orgID+" = "+result);
		return result;
	}
	public void setNames_division_lang(String lang) {
		// TODO Auto-generated method stub
		
	}
	public void setNames_subdivisions_lang(String lang) {
		// TODO Auto-generated method stub
		
	}
	public void setNames_subdivisions_charset(String flavor) {
		// TODO Auto-generated method stub 
		
	}
	public void setNames_division_charset(String flavor) {
		// TODO Auto-generated method stub
		
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
		this.dirty_main = true;
	}
	public Language getName_Language() {
		return new Language(this.name_lang, this.name_charset);
	}
	public Language getName_division_Language() {
		return new Language(this.getName_division_lang(), this.getName_division_charset());
	}
	public Language getNames_subdivisions_Language() {
		return new Language(this.getName_subdivision_lang(), this.getName_subdivision_charset());
	}
	public String getNames_subdivisions_str() {
		return D_Neighborhood.concatSubdivisions(this.getNames_subdivisions());
	}
	private String getName_division_lang() {
		return name_division_lang;
	}
	private void setName_division_lang(String name_division_lang) {
		this.name_division_lang = name_division_lang;
	}
	private String getName_division_charset() {
		return name_division_charset;
	}
	private void setName_division_charset(String name_division_charset) {
		this.name_division_charset = name_division_charset;
	}
	private String getName_subdivision_lang() {
		return name_subdivision_lang;
	}
	private void setName_subdivision_lang(String name_subdivision_lang) {
		this.name_subdivision_lang = name_subdivision_lang;
	}
	private String getName_subdivision_charset() {
		return name_subdivision_charset;
	}
	private void setName_subdivision_charset(String name_subdivision_charset) {
		this.name_subdivision_charset = name_subdivision_charset;
	}
}
class D_Neighborhood_SaverThread extends util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000; // ms to sleep
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	/**
	 * The next monitor is needed to ensure that two D_Neighborhood_SaverThreadWorker are not concurrently modifying the database,
	 * and no thread is started when it is not needed (since one is already running).
	 */
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Neighborhood_SaverThread() {
		super("D_Neighborhood Saver", true);
		//start ();
	}
	public void _run() {
		for (;;) {
			if (stop) return;
			synchronized(saver_thread_monitor) {
				new D_Neighborhood_SaverThreadWorker().start();
			}
			/*
			synchronized(saver_thread_monitor) {
				D_Neighborhood de = D_Neighborhood.need_saving_next();
				if (de != null) {
					if (DEBUG) System.out.println("D_Neighborhood_Saver: loop saving "+de);
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
					//System.out.println("D_Neighborhood_Saver: idle ...");
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

class D_Neighborhood_SaverThreadWorker extends util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000;
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Neighborhood_SaverThreadWorker() {
		super("D_Neighborhood Saver Worker", false);
		//start ();
	}
	public void _run() {
		synchronized(D_Neighborhood_SaverThread.saver_thread_monitor) {
			D_Neighborhood de;
			boolean edited = true;
			if (DEBUG) System.out.println("D_Neighborhood_Saver: start");
			
			 // first try objects being edited
			de = D_Neighborhood.need_saving_obj_next();
			
			// then try remaining objects 
			if (de == null) { de = D_Neighborhood.need_saving_next(); edited = false; }
			
			if (de != null) {
				if (DEBUG) System.out.println("D_Neighborhood_Saver: loop saving "+de.getName());
				Application_GUI.ThreadsAccounting_ping("Saving");
				if (edited) D_Neighborhood.need_saving_obj_remove(de);
				else D_Neighborhood.need_saving_remove(de);//, de.instance);
				if (DEBUG) System.out.println("D_Neighborhood_Saver: loop removed need_saving flag");
				// try 3 times to save
				for (int k = 0; k < 3; k++) {
					try {
						if (DEBUG) System.out.println("D_Neighborhood_Saver: loop will try saving k="+k);
						de.storeAct();
						if (DEBUG) System.out.println("D_Neighborhood_Saver: stored org:"+de.getName());
						break;
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
						synchronized(this) {
							try {
								if (DEBUG) System.out.println("D_Neighborhood_Saver: sleep");
								wait(SAVER_SLEEP_ON_ERROR);
								if (DEBUG) System.out.println("D_Neighborhood_Saver: waked error");
							} catch (InterruptedException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
			} else {
				Application_GUI.ThreadsAccounting_ping("Nothing to do!");
				if (DEBUG) System.out.println("D_Neighborhood_Saver: idle ...");
			}
		}
		synchronized(this) {
			try {
				if (DEBUG) System.out.println("D_Neighborhood_Saver: sleep");
				wait(SAVER_SLEEP);
				if (DEBUG) System.out.println("D_Neighborhood_Saver: waked");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

