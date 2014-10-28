package data;

import handling_wb.PreparedMessage;
import hds.ASNSyncPayload;

import java.util.ArrayList;
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
import streaming.RequestData;
import table.field_value;
import table.identity_ids;
import table.justification;
import table.motion;
import table.news;
import table.translation;
import table.witness;
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

public class D_Constituent extends ASNObj  implements  DDP2P_DoubleLinkedList_Node_Payload<D_Constituent>, Summary {
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public static final int EXPAND_NONE = 0;
	public static final int EXPAND_ONE = 1;
	public static final int EXPAND_ALL = 2;
	
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	public int version = 2;
	
	private String global_constituent_id;//Printable
	public String global_constituent_id_hash;
	public String global_submitter_id;//Printable
	public String global_neighborhood_ID;
	public String global_organization_ID;
	
	public String surname;//UTF8
	public String forename;//UTF8
	public String slogan;
	public boolean external;
	public String[] languages;
	public D_FieldValue[] address;
	public String email;//Printable
	private Calendar creation_date;
	private String _creation_date;
	public String weight;
	public boolean revoked;
	public byte[] picture;//OCT STR
	public String hash_alg;//Printable
	public byte[] signature; //OCT STR
	public byte[] certificate; //OCT STR
	
	public D_Witness valid_support;
	public D_Neighborhood[] neighborhood;
	public D_Constituent submitter;

	private String constituent_ID;
	private long _constituent_ID;
	public String submitter_ID;
	public String neighborhood_ID;
	private String organization_ID;
	private long _organization_ID;
	
	
	private D_Peer source_peer;
	public boolean blocked = false;
	public boolean requested = false;
	public boolean broadcasted = D_Organization.DEFAULT_BROADCASTED_ORG;
	private boolean temporary = true;
	private long source_peer_ID;
	private boolean hidden = true;
	D_Constituent_My mydata = new D_Constituent_My();
	private Calendar arrival_date;
	private String _arrival_date;
	private Calendar preferences_date;
	private String _preferences_date;
	
	private int status_references = 0;
	private int status_lock_write = 0; 
	ciphersuits.Cipher keys;
	
	public boolean dirty_main = false;
	public boolean dirty_params = false;
	public boolean dirty_locals = true;
	public boolean dirty_mydata = false;
	
	public boolean loaded_globals = false;
	public boolean loaded_locals = false;
	
	private static Object monitor_object_factory = new Object();
	static Object lock_organization_GID_storage = new Object(); // duplicate of the above
	public static int MAX_LOADED_CONSTS = 10000;
	public static long MAX_CONSTS_RAM = 10000000;
	private static final int MIN_CONSTS_RAM = 2;
	D_Constituent_Node component_node = new D_Constituent_Node(null, null);
	
	/**
	 * All fields prefixed with alias "c."
	 */
	public static String c_fields_constituents = Util.setDatabaseAlias(table.constituent.fields_constituents,"c");
	/**
	 * Gets the constituent fields and its neighborhoodGID,
	 * joins table constituent, neighborhood, and organization (to check broadcasting rights).
	 * Could check organization and neighborhood later.
	 */
	public static String sql_get_const =
			"SELECT "+c_fields_constituents+",n."+table.neighborhood.global_neighborhood_ID+
			" FROM "+table.constituent.TNAME+" as c " +
			" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(c."+table.constituent.neighborhood_ID+" = n."+table.neighborhood.neighborhood_ID+") ";
			//" LEFT JOIN "+table.organization.TNAME+" AS o ON(c."+table.constituent.organization_ID+" = o."+table.organization.organization_ID+") ";
	public static String sql_get_consts =
			"SELECT "+c_fields_constituents+",n."+table.neighborhood.global_neighborhood_ID+
			" FROM "+table.constituent.TNAME+" as c " +
			" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(c."+table.constituent.neighborhood_ID+" = n."+table.neighborhood.neighborhood_ID+") "+
			" LEFT JOIN "+table.organization.TNAME+" AS o ON(c."+table.constituent.organization_ID+" = o."+table.organization.organization_ID+") ";
	/**
	 * Gets the constituent fields and its neighborhoodGID,
	 * joins table constituent, neighborhood, and organization
	 */
	static String sql_get_const_by_ID =
		sql_get_const +
		" WHERE c."+table.constituent.constituent_ID+" = ?;";
	/**
	 * Gets the constituent fields and its neighborhoodGID,
	 * joins table constituent, neighborhood, and organization
	 * by ID=? OR GID=?
	 */
	static String sql_get_const_by_GID =
		sql_get_const +
		" WHERE c."+table.constituent.global_constituent_ID+" = ? "+
		" OR c."+table.constituent.global_constituent_ID_hash+" = ?;";

	
	public static D_Constituent getEmpty() {return new D_Constituent();}
	public D_Constituent instance() {return new D_Constituent();}
	private D_Constituent() {}
	private D_Constituent(String gID, String gIDH, boolean load_Globals, boolean create, D_Peer __peer, long p_olID) {
		if (DEBUG) System.out.println("D_Constituent: gID="+gID+" gIDH="+gIDH+" glob="+load_Globals+" create="+create+" peer="+__peer);
		ArrayList<ArrayList<Object>> c;
		try {
			c = Application.db.select(sql_get_const_by_GID, new String[]{gID, gIDH}, DEBUG);
			if (c.size() == 0) {
				if (! create) throw new D_NoDataException("No such constituent: c_GIDH="+gIDH+" GID="+gID);
				this.source_peer = __peer;
				this.setOrganization(null, p_olID);
				this.setGID(gID, gIDH, p_olID);
				if (__peer != null) this.source_peer_ID = __peer.getLID();
				this.dirty_main = true;
				this.setTemporary();
				//this.storeRequest();
			} else {
				load(c.get(0), EXPAND_NONE);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}
	private D_Constituent(Long lID, boolean load_Globals) {
		if ((lID == null) || (lID.longValue() <= 0)) throw new RuntimeException("LID="+lID);
		init(Util.getStringID(lID), load_Globals);
	}
	private D_Constituent(String lID, boolean load_Globals) {
		if (lID == null) throw new RuntimeException("LID="+lID);
		init(lID, load_Globals);
	}
	private void init(String lID, boolean load_Globals) {
		ArrayList<ArrayList<Object>> c;
		try {
			c = Application.db.select(sql_get_const_by_ID, new String[]{lID}, DEBUG);
			if (c.size() == 0) throw new RuntimeException("LID="+lID);
			load(c.get(0), EXPAND_NONE);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void load(ArrayList<Object> alk, int _neighborhoods) throws P2PDDSQLException {
		if (DEBUG) System.out.println("D_Constituent:load: neigh="+_neighborhoods);		
		constituent_ID = Util.getString(alk.get(table.constituent.CONST_COL_ID));
		_constituent_ID = Util.lval(constituent_ID);
		version = Util.ival(alk.get(table.constituent.CONST_COL_VERSION), 0);
		submitter_ID = Util.getString(alk.get(table.constituent.CONST_COL_SUBMITTER));
		if (submitter_ID != null)
			global_submitter_id = D_Constituent.getGIDFromLID(getSubmitterLID());

		organization_ID = Util.getString(alk.get(table.constituent.CONST_COL_ORG));
		_organization_ID = Util.lval(organization_ID);
		global_organization_ID = D_Organization.getGIDbyLID(_organization_ID);
		
		_set_GID(Util.getString(alk.get(table.constituent.CONST_COL_GID)));
		global_constituent_id_hash = Util.getString(alk.get(table.constituent.CONST_COL_GID_HASH));
		if (global_constituent_id_hash == null)
			global_constituent_id_hash = D_Constituent.getGIDHashFromGID(getGID());
		
		// this.setOrganization(global_organization_ID, _organization_ID);
		
		surname = Util.getString(alk.get(table.constituent.CONST_COL_SURNAME));
		//if(DEBUG) System.out.println("ConstituentHandling:load: surname:"+surname+" from="+alk.get(table.constituent.CONST_COL_SURNAME));		
		forename = Util.getString(alk.get(table.constituent.CONST_COL_FORENAME));
		weight = Util.getString(alk.get(table.constituent.CONST_COL_WEIGHT));
		slogan = Util.getString(alk.get(table.constituent.CONST_COL_SLOGAN));
		if (DEBUG) System.out.println("D_Constituent:load: external="+alk.get(table.constituent.CONST_COL_EXTERNAL));
		languages = D_OrgConcepts.stringArrayFromString(Util.getString(alk.get(table.constituent.CONST_COL_LANG)));
		address = D_FieldValue.getFieldValues(constituent_ID);		
		email = Util.getString(alk.get(table.constituent.CONST_COL_EMAIL));
		
		setCreationDate(Util.getString(alk.get(table.constituent.CONST_COL_DATE_CREATION)));
		setArrivalDate(Util.getString(alk.get(table.constituent.CONST_COL_DATE_ARRIVAL)));
		setPreferencesDate(Util.getString(alk.get(table.constituent.CONST_COL_DATE_PREFERENCES)));
		
		global_neighborhood_ID = Util.getString(alk.get(table.constituent.CONST_COLs+0));
		this.neighborhood_ID = Util.getString(alk.get(table.constituent.CONST_COL_NEIGH));
		this.external = Util.stringInt2bool(alk.get(table.constituent.CONST_COL_EXTERNAL), false);
		this.revoked = Util.stringInt2bool(alk.get(table.constituent.CONST_COL_REVOKED), false);
		this.blocked = Util.stringInt2bool(alk.get(table.constituent.CONST_COL_BLOCKED),false);
		this.requested = Util.stringInt2bool(alk.get(table.constituent.CONST_COL_REQUESTED),false);
		this.source_peer_ID = Util.lval(alk.get(table.constituent.CONST_COL_PEER_TRANSMITTER_ID));
		this.broadcasted = Util.stringInt2bool(alk.get(table.constituent.CONST_COL_BROADCASTED), D_Organization.DEFAULT_BROADCASTED_ORG_ITSELF);
		this.hidden = Util.stringInt2bool(alk.get(table.constituent.CONST_COL_HIDDEN),false);
		
		//picture = strToBytes(Util.getString(alk.get(table.constituent.CONST_COL_PICTURE)));
		picture = Util.byteSignatureFromString(Util.getString(alk.get(table.constituent.CONST_COL_PICTURE)));
		hash_alg = Util.getString(alk.get(table.constituent.CONST_COL_HASH_ALG));
		String _sgn = Util.getString(alk.get(table.constituent.CONST_COL_SIGNATURE));
		if ("".equals(_sgn)) _sgn = null;
		signature = Util.byteSignatureFromString(_sgn);
		//certificate = strToBytes(Util.getString(alk.get(table.constituent.CONST_COL_CERTIF)));
		certificate = Util.byteSignatureFromString(Util.getString(alk.get(table.constituent.CONST_COL_CERTIF)));			
		//c.hash = Util.hexToBytes(Util.getString(al.get(k).get(table.constituent.CONST_COL_HASH)).split(Pattern.quote(":")));
		//c.cerReq = al.get(k).get(table.constituent.CONST_COL_CERREQ);
		//c.cert_hash_alg = al.get(k).get(table.constituent.CONST_COL_CERT_HASH_ALG);
				
		
		String my_const_sql = "SELECT "+table.my_constituent_data.fields_list+
				" FROM "+table.my_constituent_data.TNAME+
				" WHERE "+table.my_constituent_data.constituent_ID+" = ?;";
		ArrayList<ArrayList<Object>> my_org = Application.db.select(my_const_sql, new String[]{getLIDstr()}, DEBUG);
		if (my_org.size() != 0) {
			ArrayList<Object> my_data = my_org.get(0);
			mydata.name = Util.getString(my_data.get(table.my_constituent_data.COL_NAME));
			mydata.category = Util.getString(my_data.get(table.my_constituent_data.COL_CATEGORY));
			mydata.submitter = Util.getString(my_data.get(table.my_constituent_data.COL_SUBMITTER));
			// skipped preferences_date (used from main)
			mydata.row = Util.lval(my_data.get(table.my_constituent_data.COL_ROW));
		}
		
		initSK();
		
		loadNeighborhoods(_neighborhoods);
		if (DEBUG) System.out.println("D_Constituent:load: done");		
	}
	
	public long getSubmitterLID() {
		return Util.lval(submitter_ID);
	}
	public void loadNeighborhoods (int _neighborhoods) {
		if (_neighborhoods == EXPAND_NONE || ((global_neighborhood_ID == null) && (neighborhood_ID == null))) { neighborhood = null; return; }
		try {
			neighborhood = D_Neighborhood.getNeighborhoodHierarchy(global_neighborhood_ID, neighborhood_ID, _neighborhoods, this.getOrganizationID());
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public String toString() {
		String result="D_Constituent: [ #"+this.constituent_ID;
		result += "\n version="+version;
		result += "\n orgGID=["+organization_ID+","+this._organization_ID+"]="+global_organization_ID;
		result += "\n costGID="+getGID();
		result += "\n constGIDH="+global_constituent_id_hash;
		result += "\n surname="+surname;
		result += "\n forename="+forename;
		result += "\n address="+Util.nullDiscrim(address, Util.concat(address,":"));
		result += "\n email="+email;
		result += "\n crea_date="+Encoder.getGeneralizedTime(creation_date);
		result += "\n neigGID="+global_neighborhood_ID;
		result += "\n picture="+Util.nullDiscrim(picture, Util.stringSignatureFromByte(picture));
		result += "\n hash_alg="+hash_alg;
		result += "\n certif="+Util.nullDiscrim(certificate, Util.stringSignatureFromByte(certificate));
		result += "\n lang="+Util.nullDiscrim(languages, Util.concat(languages,":"));
		result += "\n submitterGID="+global_submitter_id;
		result += "\n submitter="+submitter;
		result += "\n slogan="+slogan;
		result += "\n weight="+weight;
		result += "\n external="+external;
		result += "\n revoked="+revoked;
		result += "\n signature="+Util.byteToHexDump(signature);
		result += "\n  row="+this.mydata.row;
		result += "\n  name="+this.mydata.name;
		result += "\n  submiter="+this.mydata.submitter;
		result += "\n  category="+this.mydata.category;
		if(neighborhood!=null) result += "\n neigh=["+Util.concat(neighborhood, "\n\n")+"]";
		return result+"]";
	}
	public String toSummaryString() {
		String result="D_Constituent: [";
		if (global_organization_ID != null) result += "\n orgGID="+global_organization_ID;
		result += "\n surname="+surname+" [#"+this.mydata.row+" "+this.mydata.name+"]";
		result += "\n forename="+forename;
		if(neighborhood!=null) result += "\n neigh=["+Util.concatSummary(neighborhood, "\n\n", null)+"]";
		return result+"]";
	}

	public static class D_Constituent_Node {
		private static final int MAX_TRIES = 30;
		/** Currently loaded peers, ordered by the access time*/
		private static DDP2P_DoubleLinkedList<D_Constituent> loaded_consts = new DDP2P_DoubleLinkedList<D_Constituent>();
		private static Hashtable<Long, D_Constituent> loaded_const_By_LocalID = new Hashtable<Long, D_Constituent>();
		//private static Hashtable<String, D_Constituent> loaded_const_By_GID = new Hashtable<String, D_Constituent>();
		//private static Hashtable<String, D_Constituent> loaded_const_By_GIDhash = new Hashtable<String, D_Constituent>();
		private static Hashtable<String, Hashtable<Long, D_Constituent>> loaded_const_By_GIDH_ORG = new Hashtable<String, Hashtable<Long, D_Constituent>>();
		private static Hashtable<Long, Hashtable<String, D_Constituent>> loaded_const_By_ORG_GIDH = new Hashtable<Long, Hashtable<String, D_Constituent>>();
		private static Hashtable<String, Hashtable<Long, D_Constituent>> loaded_const_By_GID_ORG = new Hashtable<String, Hashtable<Long, D_Constituent>>();
		private static Hashtable<Long, Hashtable<String, D_Constituent>> loaded_const_By_ORG_GID = new Hashtable<Long, Hashtable<String, D_Constituent>>();
		private static long current_space = 0;
		
		//return loaded_const_By_GID.get(GID);
		private static D_Constituent getConstByGID(String GID, Long organizationLID) {
			if (organizationLID != null && organizationLID > 0) {
				Hashtable<String, D_Constituent> t1 = loaded_const_By_ORG_GID.get(organizationLID);
				if (t1 == null || t1.size() == 0) return null;
				return t1.get(GID);
			}
			Hashtable<Long, D_Constituent> t2 = loaded_const_By_GID_ORG.get(GID);
			if ((t2 == null) || (t2.size() == 0)) return null;
			if ((organizationLID != null) && (organizationLID > 0))
				return t2.get(organizationLID);
			for (Long k : t2.keySet()) {
				return t2.get(k);
			}
			return null;
		}
		//return loaded_const_By_GID.put(GID, c);
		private static D_Constituent putConstByGID(String GID, Long organizationLID, D_Constituent c) {
			Hashtable<Long, D_Constituent> v1 = loaded_const_By_GID_ORG.get(GID);
			if (v1 == null) loaded_const_By_GID_ORG.put(GID, v1 = new Hashtable<Long, D_Constituent>());
			D_Constituent result = v1.put(organizationLID, c);
			
			Hashtable<String, D_Constituent> v2 = loaded_const_By_ORG_GID.get(organizationLID);
			if (v2 == null) loaded_const_By_ORG_GID.put(organizationLID, v2 = new Hashtable<String, D_Constituent>());
			D_Constituent result2 = v2.put(GID, c);
			
			if (result == null) result = result2;
			return result; 
		}
		//return loaded_const_By_GID.remove(GID);
		private static D_Constituent remConstByGID(String GID, Long organizationLID) {
			D_Constituent result = null;
			D_Constituent result2 = null;
			Hashtable<Long, D_Constituent> v1 = loaded_const_By_GID_ORG.get(GID);
			if (v1 != null) {
				result = v1.remove(organizationLID);
				if (v1.size() == 0) loaded_const_By_GID_ORG.remove(GID);
			}
			
			Hashtable<String, D_Constituent> v2 = loaded_const_By_ORG_GID.get(organizationLID);
			if (v2 != null) {
				result2 = v2.remove(GID);
				if (v2.size() == 0) loaded_const_By_ORG_GID.remove(organizationLID);
			}
			
			if (result == null) result = result2;
			return result; 
		}
//		private static D_Constituent getConstByGID(String GID, String organizationLID) {
//			return getConstByGID(GID, Util.Lval(organizationLID));
//		}
		
		private static D_Constituent getConstByGIDH(String GIDH, Long organizationLID) {
			if (GIDH == null) {
				Util.printCallPath("Why calling this with null?");
				return null;
			}
			if (organizationLID != null && organizationLID > 0) {
				Hashtable<String, D_Constituent> t1 = loaded_const_By_ORG_GIDH.get(organizationLID);
				if (t1 == null || t1.size() == 0) return null;
				return t1.get(GIDH);
			}
			Hashtable<Long, D_Constituent> t2 = loaded_const_By_GIDH_ORG.get(GIDH);
			if ((t2 == null) || (t2.size() == 0)) return null;
			if ((organizationLID != null) && (organizationLID > 0))
				return t2.get(organizationLID);
			for (Long k : t2.keySet()) {
				return t2.get(k);
			}
			return null;
		}
		private static D_Constituent putConstByGIDH(String GIDH, Long organizationLID, D_Constituent c) {
			Hashtable<Long, D_Constituent> v1 = loaded_const_By_GIDH_ORG.get(GIDH);
			if (v1 == null) loaded_const_By_GIDH_ORG.put(GIDH, v1 = new Hashtable<Long, D_Constituent>());
			D_Constituent result = v1.put(organizationLID, c);
			
			Hashtable<String, D_Constituent> v2 = loaded_const_By_ORG_GIDH.get(organizationLID);
			if (v2 == null) loaded_const_By_ORG_GIDH.put(organizationLID, v2 = new Hashtable<String, D_Constituent>());
			D_Constituent result2 = v2.put(GIDH, c);
			
			if (result == null) result = result2;
			return result; 
		}
		//return loaded_const_By_GIDhash.remove(GIDH);
		private static D_Constituent remConstByGIDH(String GIDH, Long organizationLID) {
			D_Constituent result = null;
			D_Constituent result2 = null;
			Hashtable<Long, D_Constituent> v1 = loaded_const_By_GIDH_ORG.get(GIDH);
			if (v1 != null) {
				result = v1.remove(organizationLID);
				if (v1.size() == 0) loaded_const_By_GIDH_ORG.remove(GIDH);
			}
			
			Hashtable<String, D_Constituent> v2 = loaded_const_By_ORG_GIDH.get(organizationLID);
			if (v2 != null) {
				result2 = v2.remove(GIDH);
				if (v2.size() == 0) loaded_const_By_ORG_GIDH.remove(organizationLID);
			}
			
			if (result == null) result = result2;
			return result; 
		}
//		private static D_Constituent getConstByGIDH(String GIDH, String organizationLID) {
//			return getConstByGIDH(GIDH, Util.Lval(organizationLID));
//		}
		
		/** message is enough (no need to store the Encoder itself) */
		public byte[] message;
		public DDP2P_DoubleLinkedList_Node<D_Constituent> my_node_in_loaded;
	
		public D_Constituent_Node(byte[] message,
				DDP2P_DoubleLinkedList_Node<D_Constituent> my_node_in_loaded) {
			this.message = message;
			this.my_node_in_loaded = my_node_in_loaded;
		}
		private static void register_fully_loaded(D_Constituent crt) {
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
		private static void unregister_loaded(D_Constituent crt) {
			synchronized(loaded_orgs) {
				loaded_orgs.remove(crt);
				loaded_org_By_LocalID.remove(new Long(crt.getLID()));
				loaded_org_By_GID.remove(crt.getGID());
				loaded_org_By_GIDhash.remove(crt.getGIDH());
			}
		}
		*/
		private static void register_loaded(D_Constituent crt) {
			if (DEBUG) System.out.println("D_Constituent: register_loaded: start crt = "+crt);
			crt.reloadMessage(); 
			synchronized (loaded_consts) {
				String gid = crt.getGID();
				String gidh = crt.getGIDH();
				long lid = crt.getLID();
				Long organizationLID = crt.getOrganizationID();
				
				loaded_consts.offerFirst(crt);
				if (lid > 0) {
					loaded_const_By_LocalID.put(new Long(lid), crt);
					if (DEBUG) System.out.println("D_Constituent: register_loaded: store lid="+lid+" crt="+crt.getGIDH());
				}else{
					if (DEBUG) System.out.println("D_Constituent: register_loaded: no store lid="+lid+" crt="+crt.getGIDH());
				}
				if (gid != null) {
					D_Constituent_Node.putConstByGID(gid, crt.getOrganizationID(), crt); //loaded_const_By_GID.put(gid, crt);
					if (DEBUG) System.out.println("D_Constituent: register_loaded: store gid="+gid);
				} else {
					if (DEBUG) System.out.println("D_Constituent: register_loaded: no store gid="+gid);
				}
				if (gidh != null) {
					D_Constituent_Node.putConstByGIDH(gidh, organizationLID, crt);//loaded_const_By_GIDhash.put(gidh, crt);
					if (DEBUG) System.out.println("D_Constituent: register_loaded: store gidh="+gidh);
				} else {
					if (DEBUG) System.out.println("D_Constituent: register_loaded: no store gidh="+gidh);
				}
				if (crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				int tries = 0;
				while ((loaded_consts.size() > MAX_LOADED_CONSTS)
						|| (current_space > MAX_CONSTS_RAM)) {
					if (loaded_consts.size() <= MIN_CONSTS_RAM) break; // at least _crt_peer and _myself
	
					if (tries > MAX_TRIES) break;
					tries ++;
					D_Constituent candidate = loaded_consts.getTail();
					if ((candidate.get_StatusReferences() + candidate.get_StatusLockWrite() > 0)
							||
							D_Constituent.is_crt_const(candidate)
							//||
							//(candidate == HandlingMyself_Peer.get_myself())
							) 
					{
						setRecent(candidate);
						continue;
					}
					
					D_Constituent removed = loaded_consts.removeTail();//remove(loaded_peers.size()-1);
					loaded_const_By_LocalID.remove(new Long(removed.getLID())); 
					D_Constituent_Node.remConstByGID(removed.getGID(), removed.getOrganizationID());//loaded_const_By_GID.remove(removed.getGID());
					D_Constituent_Node.remConstByGIDH(removed.getGIDH(), removed.getOrganizationID()); //loaded_const_By_GIDhash.remove(removed.getGIDH());
					if (DEBUG) System.out.println("D_Constituent: register_loaded: remove GIDH="+removed.getGIDH());
					if (removed.component_node.message != null) current_space -= removed.component_node.message.length;				
				}
			}
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_Constituent crt) {
			loaded_consts.moveToFront(crt);
		}
		public static boolean dropLoaded(D_Constituent removed, boolean force) {
			boolean result = true;
			synchronized(loaded_consts) {
				if (removed.get_StatusLockWrite() > 0 || removed.get_DDP2P_DoubleLinkedList_Node() == null)
					result = false;
				if (! force && ! result) {
					System.out.println("D_Constituent: dropLoaded: abandon: force="+force+" rem="+removed);
					return result;
				}
				
				if (loaded_consts.inListProbably(removed)) {
					try {
						loaded_consts.remove(removed);
						if (removed.component_node.message != null) current_space -= removed.component_node.message.length;	
						if (DEBUG) System.out.println("D_Constituent: dropLoaded: exit with force="+force+" result="+result);
					} catch (Exception e) {
						if (_DEBUG) e.printStackTrace();
					}
				}
				if (removed.getLIDstr() != null) loaded_const_By_LocalID.remove(new Long(removed.getLID())); 
				if (removed.getGID() != null) D_Constituent_Node.remConstByGID(removed.getGID(), removed.getLID()); //loaded_const_By_GID.remove(removed.getGID());
				if (removed.getGIDH() != null) D_Constituent_Node.remConstByGIDH(removed.getGIDH(), removed.getLID()); //loaded_const_By_GIDhash.remove(removed.getGIDH());
				if (DEBUG) System.out.println("D_Constituent: drop_loaded: remove GIDH="+removed.getGIDH());
				return result;
			}
		}
	}
	static class D_Constituent_My {
		String name;// table.my_organization_data.
		String category;
		String submitter;
		Calendar preference_date;
		String _preference_date;
		long row;
	}

	/**
	 * exception raised on error
	 * @param ID
	 * @param load_Globals 
	 * @return
	 */
	static private D_Constituent getConstByLID_AttemptCacheOnly (long ID, boolean load_Globals) {
		if (ID <= 0) return null;
		Long id = new Long(ID);
		D_Constituent crt = D_Constituent_Node.loaded_const_By_LocalID.get(id);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals) {
			crt.fillGlobals();
			D_Constituent_Node.register_fully_loaded(crt);
		}
		D_Constituent_Node.setRecent(crt);
		return crt;
	}
	/**
	 * exception raised on error
	 * @param LID
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static private D_Constituent getConstByLID_AttemptCacheOnly(Long LID, boolean load_Globals, boolean keep) {
		if (LID == null) return null;
		if (keep) {
			synchronized (monitor_object_factory) {
				D_Constituent  crt = getConstByLID_AttemptCacheOnly(LID.longValue(), load_Globals);
				if (crt != null) {			
					crt.inc_StatusLockWrite();
					if (crt.get_StatusLockWrite() > 1) {
						System.out.println("D_Constituent: getOrgByGIDhash_AttemptCacheOnly: "+crt.get_StatusLockWrite());
						Util.printCallPath("");
					}
				}
				return crt;
			}
		} else {
			return getConstByLID_AttemptCacheOnly(LID.longValue(), load_Globals);
		}
	}

	static public D_Constituent getConstByLID(String LID, boolean load_Globals, boolean keep) {
		return getConstByLID(Util.Lval(LID), load_Globals, keep);
	}
	static public D_Constituent getConstByLID(Long LID, boolean load_Globals, boolean keep) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Constituent: getConstByLID: "+LID+" glob="+load_Globals);
		if ((LID == null) || (LID <= 0)) {
			if (DEBUG) System.out.println("D_Constituent: getConstByLID: null LID = "+LID);
			if (DEBUG) Util.printCallPath("?");
			return null;
		}
		D_Constituent crt = D_Constituent.getConstByLID_AttemptCacheOnly(LID, load_Globals, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Constituent: getConstByLID: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_Constituent.getConstByLID_AttemptCacheOnly(LID, load_Globals, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_Constituent: getConstByLID: got sync cached crt="+crt);
				return crt;
			}

			try {
				crt = new D_Constituent(LID, load_Globals);
				if (DEBUG) System.out.println("D_Constituent: getConstByLID: loaded crt="+crt);
				D_Constituent_Node.register_loaded(crt);
				if (keep) {
					crt.inc_StatusLockWrite();
				}
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Constituent: getConstByLID: error loading");
				// e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_Constituent: getConstByLID: Done");
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
	static private D_Constituent getConstByGID_or_GIDhash_AttemptCacheOnly(String GID, String GIDhash, Long organizationLID, boolean load_Globals) {
		D_Constituent  crt = null;
		if ((GID == null) && (GIDhash == null)) return null;
		if (GIDhash != null) crt = D_Constituent_Node.getConstByGIDH(GIDhash, organizationLID);//.loaded_const_By_GIDhash.get(GIDhash);
		if ((crt == null) && (GID != null)) crt = D_Constituent_Node.getConstByGID(GID, organizationLID); //.loaded_const_By_GID.get(GID);
		
		
		if ((GID != null) && ((crt == null) || (GIDhash == null) || DD.VERIFY_GIDH_ALWAYS)) {
			String hash = D_Constituent.getGIDHashFromGID(GID);
			if (hash == null) {
				System.out.println("D_Constituent: getOrgByGID_or_GIDhash_Attempt: fail to get GIDH from "+GID+" to compare with: "+GIDhash );
				throw new RuntimeException("No GIDhash computation possible");
			}
			if (GIDhash != null) {
				if (! hash.equals(GIDhash)) {
					System.out.println("D_Constituent: getOrgByGID_or_GIDhash_Attempt: mismatch "+GIDhash+" vs "+hash);
					throw new RuntimeException("No GID and GIDhash match");
				}
			} else {
				GIDhash = hash;
			}
			if (crt == null) crt = D_Constituent_Node.getConstByGIDH(GIDhash, organizationLID); //.loaded_const_By_GIDhash.get(GIDhash);
		}
		
		if (crt != null) {
			crt.setGID(GID, GIDhash, crt.getOrganizationID());
			
			if (load_Globals && !crt.loaded_globals) {
				crt.fillGlobals();
				D_Constituent_Node.register_fully_loaded(crt);
			}
			D_Constituent_Node.setRecent(crt);
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
	static private D_Constituent getOrgByGIDhash_AttemptCacheOnly(String GIDhash, boolean load_Globals, boolean keep) {
		if (GIDhash == null) return null;
		if (keep) {
			synchronized(monitor_object_factory) {
				D_Constituent  crt = getOrgByGID_or_GIDhash_AttemptCacheOnly(null, GIDhash, load_Globals);
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
	 * @param GIDhash
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static private D_Constituent getConstByGID_or_GIDhash_AttemptCacheOnly(String GID, String GIDhash, Long oID, boolean load_Globals, boolean keep) {
		if ((GID == null) && (GIDhash == null)) return null;
		if (keep) {
			synchronized(monitor_object_factory) {
				D_Constituent  crt = getConstByGID_or_GIDhash_AttemptCacheOnly(GID, GIDhash, oID, load_Globals);
				if (crt != null) {			
					crt.inc_StatusLockWrite();
					if (crt.get_StatusLockWrite() > 1) {
						System.out.println("D_Organization: getOrgByGIDhash_AttemptCacheOnly: "+crt.get_StatusLockWrite());
						Util.printCallPath("");
					}
				}
				return crt;
			}
		} else {
			return getConstByGID_or_GIDhash_AttemptCacheOnly(GID, GIDhash, oID, load_Globals);
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
	static private D_Constituent getOrgByGID_only_AttemptCacheOnly(String GID, boolean load_Globals) {
		if (GID == null) return null;
		D_Constituent crt = D_Constituent_Node.loaded_org_By_GID.get(GID);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals){
			crt.fillGlobals();
			D_Constituent_Node.register_fully_loaded(crt);
		}
		D_Constituent_Node.setRecent(crt);
		return crt;
	}
	*/
	@Deprecated
	static public D_Constituent getConstByGID_or_GIDH(String GID, String GIDH, boolean load_Globals, boolean keep) {
		System.out.println("D_Constituent: getConstByGID_or_GIDH: Remove me setting orgID");
		return getConstByGID_or_GIDH(GID, GIDH, load_Globals, false, keep, null, -1);
	}
	static public D_Constituent getConstByGID_or_GIDH(String GID, String GIDH, boolean load_Globals, boolean keep, Long oID) {
		return getConstByGID_or_GIDH(GID, GIDH, load_Globals, false, keep, null, oID);
	}
	/**
	 * Does not call storeRequest on creation.
	 * Therefore If create, should also set keep!
	 * @param GID
	 * @param GIDH
	 * @param oID
	 * @param load_Globals
	 * @param create
	 * @param keep
	 * @param __peer
	 * @return
	 */
	static public D_Constituent getConstByGID_or_GIDH(String GID, String GIDH, boolean load_Globals, boolean create, boolean keep, D_Peer __peer, long p_oLID) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: "+GID+", GIDH="+GIDH+" glob="+load_Globals+" cre="+create+" _peer="+__peer);
		if (create) {
			if (!keep) Util.printCallPath("Why");
			keep = true;
		}
		if ((GID == null) && (GIDH == null)) {
			if (_DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: null GID and GIDH");
			return null;
		}
		D_Constituent crt = D_Constituent.getConstByGID_or_GIDhash_AttemptCacheOnly(GID, GIDH, p_oLID, load_Globals, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_Constituent.getConstByGID_or_GIDhash_AttemptCacheOnly(GID, GIDH, p_oLID, load_Globals, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: got sync cached crt="+crt);
				return crt;
			}

			try {
				crt = new D_Constituent(GID, GIDH, load_Globals, create, __peer, p_oLID);
				if (DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: loaded crt="+crt);
				if (keep) {
					crt.inc_StatusLockWrite();
				}
				D_Constituent_Node.register_loaded(crt);
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: error loading");
				if (DEBUG) e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_Constituent: getConstByGID_or_GIDH: Done");
			return crt;
		}
	}	
	/**
	 * Usable until calling releaseReference()
	 * Verify with assertReferenced()
	 * @param peer
	 * @return
	 */
	static public D_Constituent getConstByConst_Keep(D_Constituent constit) {
		if (constit == null) return null;
		D_Constituent result = D_Constituent.getConstByGID_or_GIDH(constit.getGID(), constit.getGIDH(), true, true, constit.getOrganizationID());
		if (result == null) {
			result = D_Constituent.getConstByLID(constit.getLID(), true, true);
		}
		if (result == null) {
			if ((constit.getLIDstr() == null) && (constit.getGIDH() == null)) {
				result = constit;
				{
					constit.inc_StatusLockWrite();
					
					System.out.println("D_Constituent: getConstByConst_Keep: "+constit.get_StatusLockWrite());
					Util.printCallPath("Why: constit="+constit);
				}
			}
		}
		if (result == null) {
			System.out.println("D_Constituent: getConstByConst_Keep: got null for "+constit);
			Util.printCallPath("");
		}
		return result;
	} 

	
	/** Storing */
	public static D_Constituent_SaverThread saverThread = new D_Constituent_SaverThread();
	private boolean dirty_any() {
		return dirty_main || dirty_params || dirty_locals || dirty_mydata;
	}
	/**
	 * This function has to be called after the appropriate dirty flags are set:
	 * dirty_main - for elements in the peer table
	 * 
	 * This returns asynchronously (without waiting for the storing to happen).
	 */
	public void storeRequest() {
		//Util.printCallPath("Why store?");
		if (! this.dirty_any()) {
			Util.printCallPath("Why store when not dirty?");
			return;
		}

		if (this.arrival_date == null && (this.signature != null && this.signature.length > 0)) {
			this.arrival_date = Util.CalendargetInstance();
			if (_DEBUG) System.out.println("D_Constituent: storeRequest: missing arrival_date");
			Util.printCallPath("D_Constituent: storeRequest: Why no arrival time??");
		}
		
		String save_key = this.getGIDH();
		
		if (save_key == null) {
			//save_key = ((Object)this).hashCode() + "";
			//Util.printCallPath("Cannot store null:\n"+this);
			//return;
			D_Constituent._need_saving_obj.add(this);
			if (DEBUG) System.out.println("D_Peer:storeRequest: added to _need_saving_obj");
		} else {		
			if (DEBUG) System.out.println("D_Organization:storeRequest: GIDH="+save_key);
			D_Constituent._need_saving.add(this);
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
	static Object monitor = new Object();
	public long storeAct() throws P2PDDSQLException {
		boolean sync = true; 
		D_Organization org = D_Organization.getOrgByLID(this.organization_ID, true, false);
		if (org == null) {
			if (_DEBUG) System.out.print("D_Constituent: storeAct: no org "+this);
			return -1;
		}
		if (DEBUG) System.out.print("."+this.constituent_ID+".");
		if (this.dirty_locals || this.dirty_main || (this.getLID() <= 0)) {
			this.dirty_locals = this.dirty_main = false;
			storeAct_main(sync);
		}
		if (this.dirty_params) {
			this.dirty_params = false;
			try {
				D_FieldValue.store(sync, address, constituent_ID, this.getOrganizationID(), DD.ACCEPT_TEMPORARY_AND_NEW_CONSTITUENT_FIELDS, org);
			} catch (ExtraFieldException e) {
				e.printStackTrace();
			}
		}
		if (this.dirty_mydata) {
			this.dirty_mydata = false;
			storeAct_my(sync);
		}
		if (DEBUG) System.out.println("ConstituentHandling:storeVerified: return="+_constituent_ID);
		return _constituent_ID;
	}
	private long storeAct_main(boolean sync) throws P2PDDSQLException {
		if (this.arrival_date == null && (this.signature != null && this.signature.length > 0)) {
			this.arrival_date = Util.CalendargetInstance();
			if (_DEBUG) System.out.println("D_Constituent: missing arrival_date");
			Util.printCallPath("D_Constituent: storeRequest: Why no arrival time??");
		}
			
		//String[] fields = table.constituent.fields_constituents_no_ID_list;
		String[] params = new String[(constituent_ID != null) ?
				table.constituent.CONST_COLs:
					table.constituent.CONST_COLs_NOID];
		//params[table.constituent.CONST_COL_ID] = ;
		params[table.constituent.CONST_COL_GID] = getGID();
		params[table.constituent.CONST_COL_GID_HASH] = this.global_constituent_id_hash;
		params[table.constituent.CONST_COL_SURNAME] = surname;
		params[table.constituent.CONST_COL_FORENAME] = forename;
		params[table.constituent.CONST_COL_SLOGAN] = slogan;
		params[table.constituent.CONST_COL_EXTERNAL] = Util.bool2StringInt(external);
		params[table.constituent.CONST_COL_REVOKED] = Util.bool2StringInt(revoked);
		params[table.constituent.CONST_COL_VERSION] = ""+version;
		params[table.constituent.CONST_COL_LANG] = D_OrgConcepts.stringFromStringArray(languages);
		params[table.constituent.CONST_COL_EMAIL] = email;
		params[table.constituent.CONST_COL_PICTURE] = Util.stringSignatureFromByte(picture);
		params[table.constituent.CONST_COL_DATE_CREATION] = _creation_date; //Encoder.getGeneralizedTime(creation_date);
		params[table.constituent.CONST_COL_DATE_ARRIVAL] = getArrivalDateStr();
		params[table.constituent.CONST_COL_DATE_PREFERENCES] = getPreferencesDateStr();
		params[table.constituent.CONST_COL_NEIGH] = this.neighborhood_ID;
		params[table.constituent.CONST_COL_PEER_TRANSMITTER_ID] = Util.getStringID(this.source_peer_ID);
		params[table.constituent.CONST_COL_HASH_ALG] = hash_alg;
		params[table.constituent.CONST_COL_SIGNATURE] = Util.stringSignatureFromByte(signature);
		params[table.constituent.CONST_COL_CERTIF] = Util.stringSignatureFromByte(certificate);
		params[table.constituent.CONST_COL_ORG] = this.organization_ID;
		params[table.constituent.CONST_COL_SUBMITTER] = submitter_ID;
		params[table.constituent.CONST_COL_OP] = "1";
		params[table.constituent.CONST_COL_BLOCKED] = Util.bool2StringInt(blocked);
		params[table.constituent.CONST_COL_REQUESTED] = Util.bool2StringInt(requested);
		params[table.constituent.CONST_COL_BROADCASTED] = Util.bool2StringInt(broadcasted);
		params[table.constituent.CONST_COL_HIDDEN] = Util.bool2StringInt(this.hidden);

		try {
			if (this.constituent_ID == null) {
					
				setConstituentLID(Application.db.insert(sync, table.constituent.TNAME,
						table.constituent.fields_constituents_no_ID_list, params, DEBUG));
					
			} else {
				params[table.constituent.CONST_COL_ID] = constituent_ID;
				Application.db.update(sync, table.constituent.TNAME,
						table.constituent.fields_constituents_no_ID_list,
						new String[]{table.constituent.constituent_ID}, params, DEBUG);
					
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return _constituent_ID;
	}
	private void storeAct_my(boolean sync) {
		String param[];
		if (this.mydata.row <= 0) {
			param = new String[table.my_constituent_data.FIELDS_NB_NOID];
		} else {
			param = new String[table.my_constituent_data.FIELDS_NB];
		}
		param[table.my_constituent_data.COL_NAME] = this.mydata.name;
		param[table.my_constituent_data.COL_CATEGORY] = this.mydata.category;
		param[table.my_constituent_data.COL_SUBMITTER] = this.mydata.submitter;
		param[table.my_constituent_data.COL_CONSTITUENT_LID] = this.constituent_ID;
		try {
			if (this.mydata.row <= 0) {
				this.mydata.row =
						Application.db.insert(sync, table.my_constituent_data.TNAME,
								table.my_constituent_data.fields_noID, param, DEBUG);
			} else {
				param[table.my_constituent_data.COL_ROW] = this.mydata.row+"";
				Application.db.update(sync, table.my_constituent_data.TNAME,
						table.my_constituent_data.fields_noID,
						new String[]{table.my_constituent_data.row}, param, DEBUG);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public long storeRemoteThis(PreparedMessage pm, 
			RequestData sol_rq, RequestData new_rq, D_Peer __peer) throws P2PDDSQLException {
		if (DEBUG) System.out.println("ConstituentHandling:storeVerified: start");
		long result;
		this._organization_ID = D_Organization.getLIDbyGID(this.getOrgGID());
		if (_organization_ID <= 0) {
			System.out.println("D_Constituent: storeRemoteThis: unknown organization: "+this.getOrgGID());
			return -1;
		}
		if ( ( neighborhood != null) && ( neighborhood.length > 0 ) ) {
			for (int k = 0; k < neighborhood.length; k ++) {
				neighborhood[k].storeRemoteThis(this.getOrgGID(), this._organization_ID, this.getArrivalDateStr(), sol_rq, new_rq, __peer);
			}
		}
		D_Constituent c = D_Constituent
				.getConstByGID_or_GIDH(this.getGID(), this.getGIDH(), true, true, true, __peer, this._organization_ID);
		c.loadRemote(null, null, this, __peer, false);
		c.storeRequest();
		c.releaseReference();
		result = c.getLID_force();
		if(sol_rq!=null)sol_rq.cons.put(this.global_constituent_id_hash, DD.EMPTYDATE);
		return result;
//		if (this._constituent_ID <= 0) result = this.storeSynchronouslyNoException();
//		else {
//			D_Constituent c = D_Constituent.getConstByGID_or_GIDhash_AttemptCacheOnly(getGID(), getGIDH(), this.getOrganizationID(), true, true);
//			c.storeRequest();
//			c.releaseReference();
//			result = this._constituent_ID;
//		}
//		if(result > 0) if(sol_rq!=null)sol_rq.cons.put(this.global_constituent_id_hash, DD.EMPTYDATE);
//		return result;		
	}
			/**
			 * Not needed when there is no concurrency!
			 */
			/*
			synchronized(D_Constituent.monitor){
				if(DEBUG) System.out.print(";"+this.constituent_ID+";");
				if((this.global_constituent_id_hash!=null)&&(this.constituent_ID==null)){
					this.constituent_ID = D_Constituent.getConstituentLocalIDByGID_or_Hash(this.global_constituent_id_hash, null);
					if(DEBUG) System.out.println("D_Constituent:storeVerified: late reget id=="+this.constituent_ID+" for:"+this.getName());
					if(this.constituent_ID != null){
						params = Util.extendArray(params, table.constituent.CONST_COLs);
						params[table.constituent.CONST_COL_ID] = constituent_ID;
						this._constituent_ID = Util.lval(this.constituent_ID, -1);
					}
				}
				
				if(DEBUG) System.out.println(":"+this.constituent_ID+":");
				if(this.constituent_ID==null){
					if(DEBUG) System.out.println("ConstituentHandling:storeVerified: insert!");
					try{
						_constituent_ID=Application.db.insert(sync, table.constituent.TNAME,
								table.constituent.fields_constituents_no_ID_list, params, DEBUG);
					}catch(Exception e){
						if(_DEBUG) System.out.println("D_Constituent:storeVerified: failed hash="+global_constituent_id_hash);
						e.printStackTrace();
						if((this.global_constituent_id_hash!=null)&&(this.constituent_ID==null)){
							this.constituent_ID = D_Constituent_D.getConstituentLocalIDByGID_or_Hash(this.global_constituent_id_hash, null);
							if(_DEBUG) System.out.println("D_Constituent:storeVerified: failed reget id=="+this.constituent_ID);
						}
						this._constituent_ID = Util.lval(this.constituent_ID, -1);
					}
					constituent_ID = Util.getStringID(_constituent_ID);
					if(constituent_ID==null) return _constituent_ID;
				}else {
					if(DEBUG) System.out.println("ConstituentHandling:storeVerified: update!");
					//params[table.constituent.CONST_COLs] = constituent_ID;
					//params[table.constituent.CONST_COL_ID] = constituent_ID;
					if ((date[0]==null)||(date[0].compareTo(params[table.constituent.CONST_COL_DATE_CREATION])<0)) {
						params[params.length-1] = constituent_ID;
						Application.db.update(sync, table.constituent.TNAME,
								table.constituent.fields_constituents_no_ID_list, new String[]{table.constituent.constituent_ID}, params, DEBUG);
					} else {
						if (DEBUG) System.out.println("ConstituentHandling:storeVerified: not new data vs="+date[0]);				
					}
					_constituent_ID = new Integer(constituent_ID).longValue();
				}
				if (DEBUG) System.out.println("ConstituentHandling:storeVerified: stored constituent!");
			*/
				/*
				if ( (neighborhood != null ) && ( neighborhood.length > 0 ) ) {
					for (int k = 0; k < neighborhood.length; k ++) {
						neighborhood[k].store(sync, orgGID, org_local_ID, arrival_time, sol_rq, new_rq);
					}
				}
				*/
			/*
				if(DEBUG) System.out.println("ConstituentHandling:storeVerified: store address!");
				//long _organization_ID = Util.lval(org_local_ID, -1);
				try {
					D_FieldValue.store(sync, address, constituent_ID, this.getOrganizationID(), DD.ACCEPT_TEMPORARY_AND_NEW_CONSTITUENT_FIELDS);
				} catch (ExtraFieldException e) {
					Application.db.update(sync, table.constituent.TNAME, new String[]{table.constituent.sign},
							new String[]{table.constituent.constituent_ID},
							new String[]{null, constituent_ID}, DEBUG);
					e.printStackTrace();
					Application_GUI.warning(__("Extra Field Type for constituent.")+" "+forename+", "+surname+"\n"+
							__("Constituent dropped:")+" "+e.getLocalizedMessage(),
							__("Unknow constituent info dropped"));
				}
			}
		}
				*/
	
	public void setConstituentLID(long _constituentLID) {
		this._constituent_ID = _constituentLID;
		this.constituent_ID = Util.getStringID(_constituent_ID);
	}
	public long getOrganizationID() {
		return this._organization_ID;
	}
	public String getOrganizationIDStr() {
		return this.organization_ID;
	}
	public PK getPK() {
		if (this.isExternal()) return null;
		//if (stored_pk != null) return stored_pk;
		//SK sk = getSK();
		if (keys != null) return keys.getPK();
		PK pk = ciphersuits.Cipher.getPK(getGID());
		//stored_pk = pk;
		keys = Cipher.getCipher(pk);
		return pk;
	}
	public boolean isExternal() {
		return this.external;
	}
	public String getLIDstr() {
		return this.constituent_ID;
	}
	public long getLID() {
		return this._constituent_ID;
	}
	public long getLID_force() {
		if (this._constituent_ID > 0) return this._constituent_ID;
		return this.storeSynchronouslyNoException();
	}
	public String getGIDH() {
		return this.global_constituent_id_hash;
	}
	public String getGID() {
		return global_constituent_id;
	}
	public String _set_GID(String global_constituent_id) {
		if (! D_GIDH.isGID(global_constituent_id)) {
			Util.printCallPath("Why:" + global_constituent_id);
		}
		this.global_constituent_id = global_constituent_id;
		return global_constituent_id;
	}
	
	public void setGID (String gID, String gIDH, Long oID) {
		boolean loaded_in_cache = this.isLoaded();
		
		String oldGID = this.getGID();
		String oldGIDH = this.getGIDH();

		// sanitize input
		if (gID != null) {
			if (D_GIDH.isCompactedGID(gID)) {
				Util.printCallPath("Should not be compacted: error in ASNSyncPayload? gID="+gID+" gidh="+gIDH);
				gID = null;
			}
		}
		if (gIDH != null) {
			if (D_GIDH.isCompactedGID(gIDH)) {
				Util.printCallPath("Should not be compacted: error in ASNSyncPayload? gID="+gID+" gidh="+gIDH);
				gIDH = null;
			}
		}
		// sanitize GIDH
		if (gIDH != null) {
			if (! D_GIDH.isGIDH(gIDH)) {
				Util.printCallPath("Should be a GIDH: error: gID="+gID+" gidh="+gIDH+" in:"+this);
				gIDH = null;
			}
		}
		// sanitize input: infer a gidh if possible from GID
		if ((gID != null) && (gIDH == null)) {
			if (D_GIDH.isGID(gID)) {
				gIDH = D_Constituent.getGIDHashFromGID(gID);
			} else {
				if (D_GIDH.isGIDH(gID)) {
					gIDH = gID;
				} else {
					Util.printCallPath("Why? GID is nothing: gID="+gID+" IDH="+gIDH);
				}
				//gID = null; // done later
			}
			if (gIDH == null) Util.printCallPath("D_Constituent: null GIDH when setGID:"+gID+" for: "+this);
		}
		// sanitize  remove non gid
		if (gID != null) {
			if (! D_GIDH.isGID(gID)) {
//				if (!this.isExternal())
				Util.printCallPath("Should be a GID: error: gID="+gID+" gidh="+gIDH+" in:"+this);
				gID = null;
			}
		}
		
		if ( (gID != null) && ! Util.equalStrings_null_or_not(oldGID, gID)) {		
			if (oldGID != null) Util.printCallPath("Why: new GID="+gID+" vs oldGID="+oldGID);
			if (oldGID != null) {
				if (D_GIDH.isGID(oldGIDH) && !D_GIDH.isGID(gID)) {
					// dead code: should never be here since we tested before
					gID = oldGIDH;
				} else {
					D_Constituent_Node.remConstByGID(oldGID, oID); //.loaded_const_By_GID.remove(oldGID);
					this._set_GID(gID);
					this.dirty_main = true;
				}
			} else {
				this._set_GID(gID);
				this.dirty_main = true;
			}
		}
		
//		// redundant sanitizing handling the GIDH
//		if (gIDH != null) {
//			if (! D_GIDH.isGIDH(gIDH)) {
//				Util.printCallPath("Why is this not a GIDH? GID="+gID+" IDH="+gIDH);
//				if (gID != null) gIDH = D_Constituent.getGIDHashFromGID(gID);
//				else gIDH = null;
//			}
//		}
		
		// handle GIDH
		if (gIDH != null) {
			if ( ! Util.equalStrings_null_or_not(oldGIDH, gIDH)) {		
				if (oldGIDH != null) {
					D_Constituent_Node.remConstByGIDH(this.getGIDH(), oID); //.loaded_const_By_GIDhash.remove(this.getGIDH());
					Util.printCallPath("Why? change to ID="+gID+" IDH="+gIDH);
				}
				this.global_constituent_id_hash = gIDH;
				this.dirty_main = true;
			}
		}		
		
		if (loaded_in_cache) {
			if (this.getGID() != null)
				D_Constituent_Node.putConstByGID(this.getGID(), oID, this); //.loaded_const_By_GID.put(this.getGID(), this);
			if (this.getGIDH() != null)
				D_Constituent_Node.putConstByGIDH(this.getGIDH(), oID, this); //.loaded_const_By_GIDhash.put(this.getGIDH(), this);
		}
	}
	

	public static boolean is_crt_const(D_Constituent candidate) {
		//D_Peer myself = data.HandlingMyself_Peer.get_myself();
		//if (myself == candidate) return true;
		return Application_GUI.is_crt_const(candidate);
	}
	/**
	 * Load this message in the storage cache node.
	 * Should be called each time I load a node and when I sign myself.
	 */
	private void reloadMessage() {
		if (this.loaded_globals) this.component_node.message = this.encode(); //crt.encoder.getBytes();
	}
	/**
	 * Gets the SK from cache or DB.
	 * Assumes cache loaded (should be loaded when importing keys)!
	 * @return
	 */
	public SK getSK() {
		SK sk = null;
		PK pk = null;
		String key_gID;
		
		if (!this.isExternal()) {
			key_gID = this.getGID();
			if (key_gID == null) {
				this.fillGlobals();
				key_gID = this.getGID();
			}
		} else {
			key_gID = this.getSubmitterGID();
			if (key_gID == null) {
				this.fillGlobals();
				key_gID = this.getSubmitterGID();
			}
		}

		if (key_gID == null) return null;
		if (keys != null) {
			sk = keys.getSK();
			pk = keys.getPK();
		}
		if (sk != null) return sk;
		/*
		if (keys != null) {
			//System.out.println("D_Peer:getSK: has to load SK keys when importing keys");
			//Util.printCallPath(""+this);
			return null; // this under assumption no sk if keys seen
		}
		*/
		
		sk = Util.getStoredSK(key_gID, this.getGIDH());
		if (sk == null) return null;
		keys = Cipher.getCipher(sk, pk);
		//if (keys == null) return sk;
		return sk;
	}
	public String getSubmitterGID() {
		if (this.global_submitter_id != null) return this.global_submitter_id;
		if (this.getSubmitterLID() > 0) {
			this.global_submitter_id = D_Constituent.getGIDFromLID(this.getSubmitterLID());
			if (this.global_submitter_id != null) return this.global_submitter_id;
		}
		return null;
	}
	public void initSK() {
		if (this.isExternal()) return;
		String gID = this.getGID();
		SK sk = Util.getStoredSK(gID, this.getGIDH());
		PK pk = null;
		if (sk == null) return;
		keys = Cipher.getCipher(sk, pk);
	}
	
	/**
	 * Storage Methods
	 */
	
	public void releaseReference() {
		if (get_StatusLockWrite() <= 0) Util.printCallPath("Null reference already!");
		else dec_StatusLockWrite();
		//System.out.println("D_Constituent: releaseReference: "+status_references);
		//Util.printCallPath("");
	}
	
	public void assertReferenced() {
		assert (get_StatusLockWrite() > 0);
	}
	
	/**
	 * The entries that need saving
	 */
	private static HashSet<D_Constituent> _need_saving = new HashSet<D_Constituent>();
	private static HashSet<D_Constituent> _need_saving_obj = new HashSet<D_Constituent>();
	
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Constituent> 
	set_DDP2P_DoubleLinkedList_Node(DDP2P_DoubleLinkedList_Node<D_Constituent> node) {
		DDP2P_DoubleLinkedList_Node<D_Constituent> old = this.component_node.my_node_in_loaded;
		if (DEBUG) System.out.println("D_Constituent: set_DDP2P_DoubleLinkedList_Node: set = "+ node);
		this.component_node.my_node_in_loaded = node;
		return old;
	}
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Constituent> 
	get_DDP2P_DoubleLinkedList_Node() {
		if (DEBUG) System.out.println("D_Constituent: get_DDP2P_DoubleLinkedList_Node: get");
		return component_node.my_node_in_loaded;
	}
	/*
	static boolean need_saving_contains(String GIDH) {
		return _need_saving.contains(GIDH);
	}
	static void need_saving_add(String GIDH, String instance){
		_need_saving.add(GIDH);
	}
	*/
	static void need_saving_remove(D_Constituent c) {
		if (DEBUG) System.out.println("D_Constituent:need_saving_remove: remove "+c);
		_need_saving.remove(c);
		if (DEBUG) dumpNeedsObj(_need_saving);
	}
	static void need_saving_obj_remove(D_Constituent org) {
		if (DEBUG) System.out.println("D_Constituent:need_saving_obj_remove: remove "+org);
		_need_saving_obj.remove(org);
		if (DEBUG) dumpNeedsObj(_need_saving_obj);
	}
	static D_Constituent need_saving_next() {
		Iterator<D_Constituent> i = _need_saving.iterator();
		if (!i.hasNext()) return null;
		D_Constituent c = i.next();
		if (DEBUG) System.out.println("D_Constituent: need_saving_next: next: "+c);
		//D_Constituent r = D_Constituent_Node.getConstByGIDH(c, null);//.loaded_const_By_GIDhash.get(c);
		if (c == null) {
			if (_DEBUG) {
				System.out.println("D_Constituent Cache: need_saving_next null entry "
						+ "needs saving next: "+c);
				System.out.println("D_Constituent Cache: "+dumpDirCache());
			}
			return null;
		}
		return c;
	}
	static D_Constituent need_saving_obj_next() {
		Iterator<D_Constituent> i = _need_saving_obj.iterator();
		if (!i.hasNext()) return null;
		D_Constituent r = i.next();
		if (DEBUG) System.out.println("D_Constituent: need_saving_obj_next: next: "+r);
		//D_Constituent r = D_Constituent_Node.loaded_org_By_GIDhash.get(c);
		if (r == null) {
			if (_DEBUG) {
				System.out.println("D_Constituent Cache: need_saving_obj_next null entry "
						+ "needs saving obj next: "+r);
				System.out.println("D_Constituent Cache: "+dumpDirCache());
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
	private static void dumpNeedsObj(HashSet<D_Constituent> _need_saving2) {
		System.out.println("Needs:");
		for ( D_Constituent i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	public static String dumpDirCache() {
		String s = "[";
		s += D_Constituent_Node.loaded_consts.toString();
		s += "]";
		return s;
	}
	
	
	
	/**
	Sign_D_Constituent ::= IMPLICIT [UNIVERSAL 48] SEQUENCE {
		version INTEGER DEFAULT 0,
		global_organization_ID PrintableString,
		global_constituent_id [APPLICATION 0] PrintableString OPTIONAL,
		surname [APPLICATION 1] UTF8String OPTIONAL,
		forename [APPLICATION 15] UTF8String OPTIONAL,
		address [APPLICATION 2] SEQUENCE OF D_FieldValue OPTIONAL,
		email [APPLICATION 3] PrintableString OPTIONAL,
		creation_date [APPLICATION 4] GeneralizedDate OPTIONAL,
		global_neighborhood_ID [APPLICATION 10] PrintableString OPTIONAL,
		-- neighborhood [APPLICATION 5] SEQUENCE OF D_Neighborhood OPTIONAL,
		picture [APPLICATION 6] OCTET_STRING OPTIONAL,
		hash_alg PrintableString OPTIONAL,
		-- signature [APPLICATION 7] OCTET_STRING OPTIONAL,
		-- global_constituent_id_hash [APPLICATION 8] PrintableString OPTIONAL,
		certificate [APPLICATION 9] OCTET_STRING OPTIONAL,
		languages [APPLICATION 11] SEQUENCE OF PrintableString OPTIONAL,
		global_submitter_id [APPLICATION 12] PrintableString OPTIONAL,
		slogan [APPLICATION 13] UTF8String OPTIONAL,
		weight [APPLICATION 14] UTF8String OPTIONAL,
		-- submitter [APPLICATION 15] D_Constituent OPTIONAL,
		external BOOLEAN,
		revoked BOOLEAN OPTIONAL -- only if not external and versions past 2
	}
	 */
	/**
	 * SIGN(C',<C,O,i,r>)
	 * @param global_organization_id
	 * @return
	 */
	private Encoder getSignableEncoder() {//String global_organization_id
		//this.global_organization_ID = global_organization_id;
		Encoder enc = new Encoder().initSequence();
		if(version>=2) enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString));
		if(getGID()!=null)enc.addToSequence(new Encoder(getGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(surname!=null) enc.addToSequence(new Encoder(surname,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC1));
		if(forename!=null) enc.addToSequence(new Encoder(forename,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC15));
		if(address!=null) enc.addToSequence(Encoder.getEncoder(address).setASN1Type(DD.TAG_AC2));
		if(email!=null) enc.addToSequence(new Encoder(email,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(creation_date!=null) enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC4));
		if(this.global_neighborhood_ID!=null)
			enc.addToSequence(new Encoder(this.global_neighborhood_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC10));
		//if(neighborhood!=null) enc.addToSequence(Encoder.getEncoder(neighborhood).setASN1Type(DD.TAG_AC5));
		if(picture!=null) enc.addToSequence(new Encoder(picture).setASN1Type(DD.TAG_AC6));
		if(version<2) if(hash_alg!=null) enc.addToSequence(new Encoder(hash_alg,false));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC7));
		//if(global_constituent_id_hash!=null)enc.addToSequence(new Encoder(global_constituent_id_hash,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC8));
		if(version<2) if(certificate!=null) enc.addToSequence(new Encoder(certificate).setASN1Type(DD.TAG_AC9));
		if(languages!=null) enc.addToSequence(Encoder.getStringEncoder(languages,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC11));
		if(version<2) if(global_submitter_id!=null)enc.addToSequence(new Encoder(global_submitter_id,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		if(slogan!=null) enc.addToSequence(new Encoder(slogan).setASN1Type(DD.TAG_AC13));
		if(weight!=null) enc.addToSequence(new Encoder(weight).setASN1Type(DD.TAG_AC14));
		enc.addToSequence(new Encoder(external));
		if(!external && version>=2) enc.addToSequence(new Encoder(revoked));
		return enc;
	}
	/**
	HashExtern_D_Constituent ::= IMPLICIT [UNIVERSAL 48] SEQUENCE {
		-- version INTEGER DEFAULT 0,
		global_organization_ID PrintableString,
		-- global_constituent_id [APPLICATION 0] PrintableString OPTIONAL,
		surname [APPLICATION 1] UTF8String OPTIONAL,
		forename [APPLICATION 15] UTF8String OPTIONAL,
		address [APPLICATION 2] SEQUENCE OF D_FieldValue OPTIONAL,
		email [APPLICATION 3] PrintableString OPTIONAL,
		-- creation_date [APPLICATION 4] GeneralizedDate OPTIONAL,
		global_neighborhood_ID [APPLICATION 10] PrintableString OPTIONAL,
		-- neighborhood [APPLICATION 5] SEQUENCE OF D_Neighborhood OPTIONAL,
		picture [APPLICATION 6] OCTET_STRING OPTIONAL,
		hash_alg PrintableString OPTIONAL,
		-- signature [APPLICATION 7] OCTET_STRING OPTIONAL,
		-- global_constituent_id_hash [APPLICATION 8] PrintableString OPTIONAL,
		certificate [APPLICATION 9] OCTET_STRING OPTIONAL,
		languages [APPLICATION 11] SEQUENCE OF PrintableString OPTIONAL,
		-- global_submitter_id [APPLICATION 12] PrintableString OPTIONAL,
		-- slogan [APPLICATION 13] UTF8String OPTIONAL,
		weight [APPLICATION 14] UTF8String OPTIONAL,
		-- submitter [APPLICATION 15] D_Constituent OPTIONAL,
		external BOOLEAN,
		-- revoked BOOLEAN OPTIONAL -- only if not external and versions past 2
	}
	 */
	/**
	 * Make GID for external: HASH(O,i)
	 * Without current GID, date, and slogan (externals should not have slogans?)
	 * @param global_organization_id
	 * @return
	 */
	private Encoder getHashEncoder(){//String global_organization_id) {
		//this.global_organization_ID = global_organization_id;
		Encoder enc = new Encoder().initSequence();
		
		enc.addToSequence(new Encoder(global_organization_ID,Encoder.TAG_PrintableString));
		//if(global_constituent_id!=null)enc.addToSequence(new Encoder(global_constituent_id,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(surname!=null) enc.addToSequence(new Encoder(surname,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC1));
		if(forename!=null) enc.addToSequence(new Encoder(forename,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC15));
		if(address!=null) enc.addToSequence(Encoder.getEncoder(address).setASN1Type(DD.TAG_AC2));
		if(email!=null)enc.addToSequence(new Encoder(email,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		//if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC4));
		if(this.global_neighborhood_ID!=null)
			enc.addToSequence(new Encoder(this.global_neighborhood_ID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC10));
		//if(neighborhood!=null) enc.addToSequence(Encoder.getEncoder(neighborhood).setASN1Type(DD.TAG_AC5));
		if(picture!=null) enc.addToSequence(new Encoder(picture).setASN1Type(DD.TAG_AC6));
		if(version<2)if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,false));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC7));
		//if(global_constituent_id_hash!=null)enc.addToSequence(new Encoder(global_constituent_id_hash,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC8));
		if(version<2) if(certificate!=null) enc.addToSequence(new Encoder(certificate).setASN1Type(DD.TAG_AC9));
		if(languages!=null) enc.addToSequence(Encoder.getStringEncoder(languages,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC11));
		//if(global_submitter_id!=null)enc.addToSequence(new Encoder(global_submitter_id,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		//if(slogan!=null)enc.addToSequence(new Encoder(slogan).setASN1Type(DD.TAG_AC13));
		if(weight!=null) enc.addToSequence(new Encoder(weight).setASN1Type(DD.TAG_AC14)); // weight should be signed by authority/witnessed
		enc.addToSequence(new Encoder(external));
		return enc;
		//if(true)return enc;
	}
	/**
	D_Constituent ::= IMPLICIT [APPLICATION 48] SEQUENCE {
		version INTEGER DEFAULT 0,
		global_organization_ID PrintableString OPTIONAL,
		global_constituent_id [APPLICATION 0] PrintableString OPTIONAL,
		surname [APPLICATION 1] UTF8String OPTIONAL,
		forename [APPLICATION 15] UTF8String OPTIONAL,
		address [APPLICATION 2] SEQUENCE OF D_FieldValue OPTIONAL,
		email [APPLICATION 3] PrintableString OPTIONAL,
		creation_date [APPLICATION 4] GeneralizedDate OPTIONAL,
		global_neighborhood_ID [APPLICATION 10] PrintableString OPTIONAL,
		neighborhood [APPLICATION 5] SEQUENCE OF D_Neighborhood OPTIONAL,
		picture [APPLICATION 6] OCTET_STRING OPTIONAL,
		hash_alg PrintableString OPTIONAL,
		signature [APPLICATION 7] OCTET_STRING OPTIONAL,
		global_constituent_id_hash [APPLICATION 8] PrintableString OPTIONAL,
		certificate [APPLICATION 9] OCTET_STRING OPTIONAL,
		languages [APPLICATION 11] SEQUENCE OF PrintableString OPTIONAL,
		global_submitter_id [APPLICATION 12] PrintableString OPTIONAL,
		slogan [APPLICATION 13] UTF8String OPTIONAL,
		weight [APPLICATION 14] UTF8String OPTIONAL,
		valid_support [APPLICATION 16] D_Witness OPTIONAL,
		submitter [APPLICATION 17] D_Constituent OPTIONAL,
		external BOOLEAN,
		revoked BOOLEAN
	}
	 */
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
	/**
	 * TODO: Have to decide later if neighborhoods and submitter should be send even when descendants is none...
	 */
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs, int dependants) {
		int new_dependants = dependants;
		if (dependants > 0) new_dependants = dependants - 1;
		
		Encoder enc = new Encoder().initSequence();
		if (version >= 2) enc.addToSequence(new Encoder(version));
		
		/**
		 * May decide to comment encoding of "global_organization_ID" out completely, since the org_GID is typically
		 * available at the destination from enclosing fields, and will be filled out at expansion
		 * by ASNSyncPayload.expand at decoding.
		 * However, it is not that damaging when using compression, and can be stored without much overhead.
		 * So it is left here for now.  Test if you comment out!
		 */
		if (global_organization_ID!=null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_organization_ID);
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString));
		}
		if (getGID()!=null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		}
		if (surname!=null) enc.addToSequence(new Encoder(surname,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC1));
		if (forename!=null) enc.addToSequence(new Encoder(forename,Encoder.TAG_UTF8String).setASN1Type(DD.TAG_AC15));
		if (address!=null) enc.addToSequence(Encoder.getEncoder(address).setASN1Type(DD.TAG_AC2));
		if (email!=null) enc.addToSequence(new Encoder(email,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if (creation_date!=null) enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC4));
		if (global_neighborhood_ID != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_neighborhood_ID);
			enc.addToSequence(new Encoder(repl_GID, false).setASN1Type(DD.TAG_AC10));
		}
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (neighborhood != null) enc.addToSequence(Encoder.getEncoder(neighborhood, dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC5));
		}
		
		if (picture != null) enc.addToSequence(new Encoder(picture).setASN1Type(DD.TAG_AC6));
		if (hash_alg != null)enc.addToSequence(new Encoder(hash_alg,false));
		if (signature != null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC7));
		if (false && global_constituent_id_hash != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_constituent_id_hash);
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC8));
		}
		if (certificate!=null) enc.addToSequence(new Encoder(certificate).setASN1Type(DD.TAG_AC9));
		if (languages!=null) enc.addToSequence(Encoder.getStringEncoder(languages,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC11));
		if (global_submitter_id!=null){
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_submitter_id);
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC12));
		}
		if (slogan != null) enc.addToSequence(new Encoder(slogan).setASN1Type(DD.TAG_AC13));
		if (weight != null) enc.addToSequence(new Encoder(weight).setASN1Type(DD.TAG_AC14));
		if (valid_support != null) enc.addToSequence(valid_support.getEncoder().setASN1Type(DD.TAG_AC16));
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (submitter != null) enc.addToSequence(submitter.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC17));
		}
		enc.addToSequence(new Encoder(external));
		if (! external) enc.addToSequence(new Encoder(revoked));
		enc.setASN1Type(D_Constituent.getASN1Type());
		return enc;
	}

	@Override
	public D_Constituent decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==Encoder.TAG_INTEGER) version = dec.getFirstObject(true).getInteger().intValue();
		else version = 0;
		if(dec.getTypeByte()==Encoder.TAG_PrintableString)global_organization_ID = dec.getFirstObject(true).getString();
		if(dec.getTypeByte()==DD.TAG_AC0) _set_GID(dec.getFirstObject(true).getString(DD.TAG_AC0));
		if(dec.getTypeByte()==DD.TAG_AC1) surname = dec.getFirstObject(true).getString(DD.TAG_AC1);
		if(dec.getTypeByte()==DD.TAG_AC15)forename = dec.getFirstObject(true).getString(DD.TAG_AC15);
		if(dec.getTypeByte()==DD.TAG_AC2) address = dec.getFirstObject(true).getSequenceOf(D_FieldValue.getASN1Type(),new D_FieldValue[]{},new D_FieldValue());
		if(dec.getTypeByte()==DD.TAG_AC3) email = dec.getFirstObject(true).getString(DD.TAG_AC3);
		if(dec.getTypeByte()==DD.TAG_AC4) setCreationDate(dec.getFirstObject(true).getGeneralizedTime(DD.TAG_AC4));
		if(dec.getTypeByte()==DD.TAG_AC10)global_neighborhood_ID = dec.getFirstObject(true).getString(DD.TAG_AC10);
		if(dec.getTypeByte()==DD.TAG_AC5) neighborhood = dec.getFirstObject(true).getSequenceOf(D_Neighborhood.getASN1Type(),new D_Neighborhood[]{}, D_Neighborhood.getEmpty());
		if(dec.getTypeByte()==DD.TAG_AC6) picture = dec.getFirstObject(true).getBytes(DD.TAG_AC6);
		if(dec.getTypeByte()==Encoder.TAG_PrintableString) hash_alg = dec.getFirstObject(true).getString(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC7)signature = dec.getFirstObject(true).getBytes(DD.TAG_AC7);
		if(dec.getTypeByte()==DD.TAG_AC8)global_constituent_id_hash = dec.getFirstObject(true).getString(DD.TAG_AC8);
		if(dec.getTypeByte()==DD.TAG_AC9)certificate = dec.getFirstObject(true).getBytes(DD.TAG_AC9);
		if(dec.getTypeByte()==DD.TAG_AC11) languages = dec.getFirstObject(true).getSequenceOf(Encoder.TAG_PrintableString);
		if(dec.getTypeByte()==DD.TAG_AC12) global_submitter_id = dec.getFirstObject(true).getString(DD.TAG_AC12);
		if(dec.getTypeByte()==DD.TAG_AC13) slogan = dec.getFirstObject(true).getString(DD.TAG_AC13);
		if(dec.getTypeByte()==DD.TAG_AC14) weight = dec.getFirstObject(true).getString(DD.TAG_AC14);
		if(dec.getTypeByte()==DD.TAG_AC16) valid_support = new D_Witness().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC17) submitter = new D_Constituent().decode(dec.getFirstObject(true));
		external = dec.getFirstObject(true).getBoolean();
		if(!external)
			if(dec.getTypeByte()==Encoder.TAG_BOOLEAN)
				revoked = dec.getFirstObject(true).getBoolean();
		try{
			getGIDHashFromGID(false);
		}catch(Exception e){global_constituent_id_hash = null;}
		return this;
	}
	public static byte getASN1Type() {
		return Encoder.buildASN1byteType(Encoder.CLASS_APPLICATION,
				Encoder.PC_CONSTRUCTED, TAG);
	}
	public void setCreationDate() {
		this.setCreationDate(Util.CalendargetInstance());
	}
	public void setCreationDate(Calendar c) {
		this.creation_date = c;
		this._creation_date = Encoder.getGeneralizedTime(c);
		this.dirty_main = true;
	}
	public void setCreationDate(String creationDate) {
		this._creation_date = creationDate;
		this.creation_date = Util.getCalendar(creationDate);
		this.dirty_main = true;
	}
	public String getCreationDateStr() {
		return this._creation_date;
	}
	public Calendar getCreationDate() {
		return this.creation_date;
	}
	public void setArrivalDate(String _arrivalDate) {
		this._arrival_date = _arrivalDate;
		this.arrival_date = Util.getCalendar(_arrivalDate);
		this.dirty_locals = true;
	}
	public void setArrivalDate(Calendar arrivalDate) {
		this.arrival_date = arrivalDate;
		this._arrival_date = Encoder.getGeneralizedTime(arrivalDate);
		this.dirty_locals = true;
	}
	public void setArrivalDate() {
		this.setArrivalDate(Util.CalendargetInstance());
	}
	public String getArrivalDateStr() {
		return this._arrival_date;
	}
	public Calendar getArrivalDate() {
		return this.arrival_date;
	}
	public void setPreferencesDate(String preferencesDate) {
		this._preferences_date = preferencesDate;
		this.preferences_date = Util.getCalendar(preferencesDate);
		this.dirty_locals = true;
		//this.dirty_mydata = true;
	}
	public String getPreferencesDateStr() {
		return this._preferences_date;
	}
	public Calendar getPreferencesDate() {
		return this.preferences_date;
	}
	/**
	 * 
	 * @return
	 */
	public String getGIDHashFromGID(){
		return getGIDHashFromGID(true);
	}
	/**
	 * 
	 * @param verbose : false for silent on expected exception
	 * @return
	 */
	public String getGIDHashFromGID(boolean verbose){
		if (DEBUG) System.out.println("D_Constituent: prepareGIDHash: start");
		if ((global_constituent_id_hash == null) && (getGID() != null)) {
			if (external) global_constituent_id_hash = getGID();
			else global_constituent_id_hash = getGIDHashFromGID_NonExternalOnly(getGID(), verbose);
		}	
		if (DEBUG) System.out.println("D_Constituent: prepareGIDHash: got="+global_constituent_id_hash);
		return global_constituent_id_hash;
	}
	/**
	 * Adds "R:"/D_GIDH.d_ConsR in front of the hash of the GID, for non_external (verbose on error)
	 * @param GID
	 * @return
	 */
	public static String getGIDHashFromGID_NonExternalOnly(String GID){
		return getGIDHashFromGID_NonExternalOnly(GID, true);
	}
	/**
	 *  Adds "R:" in front of the hash of the GID, for non_external
	 * @param GID
	 * @param verbose : false for silent on expected exceptions
	 * @return
	 */
	public static String getGIDHashFromGID_NonExternalOnly(String GID, boolean verbose){
		String hash = Util.getGIDhashFromGID(GID, verbose);
		if (hash == null) return null;
		return D_GIDH.d_ConsR+hash;
	}
	/**
	 * unchanged if starts with "C:"/D_GIDH.d_ConsE (external) or "R:"/D_GIDH.d_ConsR (already a hash)
	 * @param s
	 * @return
	 */
	public static String getGIDHashFromGID(String s) {
		if (s.startsWith(D_GIDH.d_ConsE)) return s; // it is an external
		if (s.startsWith(D_GIDH.d_ConsR)) return s; // it is a GID hash
		String hash = D_Constituent.getGIDHashFromGID_NonExternalOnly(s);
		if (hash == null) return null;
		if (hash.length() != s.length()) return hash;
		return s;
	}
	/**
	 * Should be called after the data is initialized.
	 * Does not assign the GID itself
	 * @return
	 */
	public String makeExternalGID() {
		fillGlobals();
		//return Util.getGlobalID("constituentID_neighbor",email+":"+forename+":"+surname);  
		return D_GIDH.d_ConsE+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
	}
	private void fillGlobals() {
		if (this.loaded_globals) return;
		
		if ((this.organization_ID != null ) && (this.global_organization_ID == null))
			this.global_organization_ID = D_Organization.getGIDbyLID(this._organization_ID);
		
		this.loaded_globals = true;
	}
	public String getNameFull() {
		if((this.forename==null)||(this.forename.trim().length()==0)) return surname;
		if((this.surname==null)||(this.surname.trim().length()==0)) return surname;
		return this.surname+", "+ this.forename;
	}
	public String getNameOrMy() {
		String n = mydata.name;
		if (n != null) return n;
		return getNameFull();
	}
	public static String getGIDFromLID(String constituentlID) {
		return getGIDFromLID(Util.lval(constituentlID));
	}
	public static String getGIDFromLID(long constituentlID) {
		if (constituentlID <= 0) return null;
		Long LID = new Long(constituentlID);
		D_Constituent c = D_Constituent.getConstByLID(LID, true, false);
		if (c == null) {
			if (_DEBUG) System.out.println("D_Constituent: getGIDFromLID: null for cLID = "+constituentlID);
			for (Long l : D_Constituent_Node.loaded_const_By_LocalID.keySet()) {
				if (_DEBUG) System.out.println("D_Constituent: getGIDFromLID: available ["+l+"]"+D_Constituent_Node.loaded_const_By_LocalID.get(l).getGIDH());
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
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(GID2, null, true, false, oID);
		if (c == null) return -1;
		return c.getLID();
	}
	public static String getLIDstrFromGID(String GID2, Long oID) {
		if (GID2 == null) return null;
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(GID2, null, true, false, oID);
		return c.getLIDstr();
	}
	public static String getLIDstrFromGID_or_GIDH(
			String GID2, String GIDH2, Long oID) {
		if (GID2 == null) return null;
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(GID2, GIDH2, true, false, oID);
		return c.getLIDstr();
	}
	public static long getLIDFromGID_or_GIDH(
			String GID2, String GIDH2, Long oID) {
		if (GID2 == null) return -1;
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(GID2, GIDH2, true, false, oID);
		return c.getLID();
	}
	public static void setTemporary(D_Constituent c) {
		c = D_Constituent.getConstByConst_Keep(c);
		c.setTemporary();
		c.storeRequest();
		c.releaseReference();
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
		this.dirty_locals = true;
	}
	/**
	 * If marked temporary, no signature or no GID
	 * @return
	 */
	public boolean isTemporary() {
		return this.temporary 
				|| ( (! this.external) && (this.signature == null || this.signature.length == 0) )
				|| this.getGID() == null;
	}
	
	public boolean readyToSend() {
		return this.isTemporary();
		/*
		if(this.global_constituent_id == null) return false;
		if(this.global_organization_ID == null) return false;
		if(!this.external)
			if((this.signature == null)||(this.signature.length==0)) return false;
		return true;
		*/
	}

	public static long insertTemporaryGID(String p_cGID,
			String p_cGIDH, long p_oLID, D_Peer __peer, boolean default_blocked) {
		D_Constituent consts = D_Constituent.insertTemporaryGID_org(p_cGID, p_cGIDH, p_oLID, __peer, default_blocked);
		if (consts == null) {
			Util.printCallPath("Why null");
			return -1;
		}
		return consts.getLID_force();//.getLID(); 
	}
	public static D_Constituent insertTemporaryGID_org(
			String p_cGID, String p_cGIDH, long p_oLID,
			D_Peer __peer, boolean default_blocked) {
		D_Constituent consts;
		if ((p_cGID != null) || (p_cGIDH != null)) {
			consts = D_Constituent.getConstByGID_or_GIDH(p_cGID, p_cGIDH, true, true, true, __peer, p_oLID);
			//consts.setName(_name);
			if (consts.isTemporary()) {
				consts.setBlocked(default_blocked);
				consts.storeRequest();
			}
			consts.releaseReference();
		}
		else return null;
		return consts; 
	}
	public void setHidden() {
		this.hidden = true;
		this.dirty_locals = true;
	}
	public boolean getHidden() {
		return this.hidden;
	}
	public void setBlocked(boolean default_blocked) {
		if (this.blocked != default_blocked) {
			this.dirty_locals = true;
			this.blocked = default_blocked;
		}
	}
	public void setOrganization(String goid, long added_Org) {
		if (DEBUG) System.out.println("D_Constituent: setOrganization: GID="+goid+" lID="+added_Org);
		if (goid == null) {
			goid = D_Organization.getGIDbyLID(added_Org);
			if (DEBUG) System.out.println("D_Constituent: setOrganization: recomputed GID="+goid);
		}
		if (! Util.equalStrings_null_or_not(goid, global_organization_ID)) {
			if (DEBUG) System.out.println("D_Constituent: setOrganization: set goid old="+global_organization_ID);
			if ((goid != null) && (global_organization_ID != null))
				if (_DEBUG) System.out.println("D_Constituent: setOrganization: set goid="+goid+" old="+global_organization_ID);
			this.global_organization_ID = goid;
			this.dirty_main = true;
		}
		if (_organization_ID != added_Org) {
			this._organization_ID = added_Org;
			this.organization_ID = Util.getStringID(_organization_ID);
			this.dirty_main = true;
		}
	}
	/**
	 * 
	 * @param GIDH
	 * @param DBG
	 * @return 0:absent, 1:available, -1: temporary
	 */
	public static int isGIDHash_available(String GIDH, Long oID,
			boolean DBG) {
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(null, GIDH, false, false, oID);
		if (c == null) return 0;
		if (c.isTemporary()) return -1;
		if (c.getGID() == null) return -1;
		
		if (! c.external && (c.getSignature() != null) ) {
			return 1;
		} else {
			return -1;
		}
	}
	/**
	 * 
	 * @param GIDH
	 * @param DBG
	 * @return 0:absent, 1:available, -1: temporary
	 */
	public static int isGID_available(String GID, Long oID,
			boolean DBG) {
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH(GID, null, false, false, oID);
		if (c == null) return 0;
		if (c.isTemporary()) return -1;
		if (c.getGID() == null) return -1;
		
		if (! c.external && (c.getSignature() != null) ) {
			return 1;
		} else {
			return -1;
		}
	}
	public byte[] getSignature() {
		return this.signature;
	}
	public void setSK(SK sk) {
		PK pk = null;
		if (sk == null) return;
		keys = Cipher.getCipher(sk, pk);
	}
	public byte[] sign() {
		if (this.external) {
			if (this.global_submitter_id != null) {
				SK sk = getSK();
				if (sk != null)
					return sign_with_ini(sk);
			}
			return null;
		} else {
			SK sk = getSK();
			if (sk == null) return null;
			if(DEBUG) System.out.println("D_Constituent:sign: orgGID="+this.global_organization_ID);
			byte[] msg=this.getSignableEncoder().getBytes();
			if(DEBUG) System.out.println("D_Constituent:sign: msg["+msg.length+"]="+Util.byteToHex(msg));
			return signature = Util.sign(msg, sk);
		}
	}
	public byte[] sign_with_ini(SK sk_ini) {
		D_Constituent wbc = this;
		String gcdhash, gcd;
		boolean present = isLoaded();
		if (wbc.isExternal()) {
			gcdhash = gcd = wbc.makeExternalGID();
			if (wbc.getGID() != null) {
				present |= (null != D_Constituent_Node.remConstByGID(wbc.getGID(), wbc.getOrganizationID())); //.loaded_const_By_GID.remove(wbc.global_constituent_id));
			}
			if (wbc.global_constituent_id_hash != null) {
				present |= (null != D_Constituent_Node.remConstByGIDH(wbc.global_constituent_id_hash, wbc.getOrganizationID()));//.loaded_const_By_GIDhash.remove(wbc.global_constituent_id_hash));
			}
//			wbc._set_GID(gcd);
//			wbc.global_constituent_id_hash = gcdhash;
//			if (present  || D_Constituent_Node.loaded_const_By_LocalID.get(this.getLID()) != null) {
//				D_Constituent_Node.putConstByGID(wbc.set_GID(gcd), wbc.getOrganizationID(), wbc); //.loaded_const_By_GID.put(wbc.global_constituent_id = gcd, wbc);
//				D_Constituent_Node.putConstByGIDH(wbc.global_constituent_id_hash = gcdhash, wbc.getOrganizationID(), wbc); //.loaded_const_By_GIDhash.put(wbc.global_constituent_id_hash = gcdhash, wbc);				
//			}
			wbc.setGID(gcd, gcd, wbc.getOrganizationID());
		}
		if ((sk_ini != null) || (! wbc.external)) {
			byte[] msg = this.getSignableEncoder().getBytes();
			if(DEBUG) System.out.println("WB_Constituents:sign: msg["+msg.length+"]="+Util.byteToHex(msg));
			this.setSignature(Util.sign(msg, sk_ini));
		} else {
			setSignature(null);
		}
		return getSignature();
	}
	public boolean verifySignature() {
		String newGID;
		if(DEBUG) System.out.println("D_Constituents:verifySignature: orgGID="+getGID());
		this.fillGlobals();
		
		if (external)
			if (!(newGID = this.makeExternalGID()).equals(this.getGID())){
				Util.printCallPath("WRONG EXTERNAL GID");
				if(DEBUG) System.out.println("D_Constituent:verifySignature: WRONG HASH GID="+this.getGID()+" vs="+newGID);
				if(DEBUG) System.out.println("D_Constituent:verifySignature: WRONG HASH GID result="+false);
				return false;
			}
		
		String pk_ID = this.getGID();
		if (external) {
			pk_ID = this.global_submitter_id;
			if (pk_ID == null) return true;
		}
		if (DEBUG) System.out.println("D_Constituent:verifySignature: pk="+pk_ID);
		byte[] msg = getSignableEncoder().getBytes();
		boolean result = util.Util.verifySignByID(msg, pk_ID, signature);
		if (DEBUG) System.out.println("D_Constituents:verifySignature: result="+result);
		return result;
	}
	public static D_Constituent integrateRemote(D_Constituent c, D_Peer __peer,
			RequestData sol_rq, RequestData new_rq, boolean default_blocked, Calendar arrival_date) {
		long oID = D_Organization.getLIDbyGID (c.global_organization_ID);
		if (oID <= 0) {
			D_Organization o = D_Organization.insertTemporaryGID_org(c.getGID(), c.getGIDH(), __peer, default_blocked, null);
			//c.setOrganization(o.getGID(), o.getLID());
			oID = o.getLID_forced();
		}
		D_Constituent lc =
				D_Constituent.getConstByGID_or_GIDH(c.getGID(), c.getGIDH(), true, true, true, __peer, oID);
		if (lc == null) return null;
		try{
			if (lc.loadRemote(sol_rq, new_rq, c, __peer, default_blocked, arrival_date)) {
				lc.storeRequest();
				config.Application_GUI.inform_arrival(lc, __peer);
			}
			lc.releaseReference();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lc;
	}
	public boolean loadRemote(RequestData sol_rq, RequestData new_rq, D_Constituent r, D_Peer __peer, boolean default_blocked) {
		return loadRemote(sol_rq, new_rq, r, __peer, default_blocked, Util.CalendargetInstance());
	}
	public boolean loadRemote(RequestData sol_rq, RequestData new_rq, D_Constituent r, D_Peer __peer, boolean default_blocked, Calendar arrival_date) {
		
		if (!this.isTemporary() && !newer(r, this)) return false;
		
		this.version = r.version;
		this.creation_date = r.creation_date;
		this._creation_date = r._creation_date;
		
		if (! Util.equalStrings_null_or_not(global_organization_ID, r.global_organization_ID)) {
			long oID = D_Organization.getLIDbyGID(r.global_organization_ID);
			if (this.global_organization_ID != null && oID <= 0) {
				D_Organization o = D_Organization.insertTemporaryGID_org(r.global_organization_ID, null, __peer, default_blocked, null);
				this.setOrganization(o.getGID(), o.getLID_forced());
				if (new_rq != null && o.isTemporary()) new_rq.orgs.add(o.getGIDH_or_guess());
			} else
				this.setOrganization(r.global_organization_ID, oID);
		}
		if (! Util.equalStrings_null_or_not(getGID(), r.getGID())) {
			if (r.getGID() != null) this.setGID(r.getGID(), r.global_constituent_id_hash, this.getOrganizationID());
			//if (r.global_constituent_id_hash != null) this.global_constituent_id_hash = r.global_constituent_id_hash;
			this.constituent_ID = null;
			this._constituent_ID = -1;
		}
		
		if (! Util.equalStrings_null_or_not(global_submitter_id, r.global_submitter_id)) {
			this.global_submitter_id = r.global_submitter_id;
			long sID = D_Constituent.getLIDFromGID(r.global_submitter_id, this._organization_ID);
			if (this.global_submitter_id != null && sID <= 0) {
				D_Constituent c = D_Constituent.insertTemporaryGID_org(r.global_submitter_id, null, this.getOrganizationID(), __peer, default_blocked);
				this.submitter = c; //r.submitter;
				this.submitter_ID = c.getLIDstr_force(); //r.submitter_ID;
				if (new_rq != null && c.isTemporary()) new_rq.cons.put(c.getGIDH(), DD.EMPTYDATE);
			}
		}
		if (! Util.equalStrings_null_or_not(this.global_neighborhood_ID, r.global_neighborhood_ID)) {
			this.global_neighborhood_ID = r.global_neighborhood_ID;
			//this.neighborhood = null;// r.neighborhood;
			//this.neighborhood_ID = r.neighborhood_ID;
			D_Neighborhood n;
			long nID = D_Neighborhood.getLIDFromGID(this.global_neighborhood_ID, this.getOrganizationID());
			this.neighborhood_ID = Util.getStringID(nID);
			if (this.global_neighborhood_ID != null && nID <= 0) {
				n = D_Neighborhood.getNeighByGID(this.global_neighborhood_ID, true, true, false, __peer, this.getOrganizationID());
				this.neighborhood_ID = Util.getStringID(n.getLID_force());
				if (new_rq != null && n.isTemporary()) new_rq.neig.add(this.global_neighborhood_ID);
			}
			//this.setNeighborhoodIDs(global_neighborhood_ID, -1);
		}
		this.surname = r.surname;
		this.forename = r.forename;
		this.email = r.email;
		this.weight = r.weight;
		this.revoked |= r.revoked;
		this.slogan = r.slogan;
		this.external = r.external;
		this.languages = r.languages;
		this.picture = r.picture;
		this.hash_alg = r.hash_alg;
		this.signature = r.signature;
		this.certificate = r.certificate;
		//if (r.valid_support != null) this.valid_support = r.valid_support;
		//this.valid_support = null;
		
		this.dirty_main = true;
		if (data.D_FieldValue.different(this.address, r.address)) {
			this.address = r.address;
			this.dirty_params = true;
		}
		if ((sol_rq != null) && (sol_rq.cons != null)) sol_rq.cons.put(getGID(), DD.EMPTYDATE);
		this.dirty_main = true;
		if (this.source_peer_ID <= 0 && __peer != null)
			this.source_peer_ID = __peer.getLID_keep_force();
		this.setTemporary(false);
		this.setArrivalDate(arrival_date);
		return true;
	}
	String getLIDstr_force() {
		return Util.getStringID(this.getLID_force());
	}
	/**
	 * If one newer than two?
	 * @param one
	 * @param two
	 * @return
	 */
	public static boolean newer(D_Constituent one, D_Constituent two) {
		assert(one != null && two != null);
		if (two.revoked) return false;
		return newer(one.getCreationDate(), two.getCreationDate());
	}
	/**
	 * If one newer than two?
	 * @param one
	 * @param two
	 * @return
	 */
	public static boolean newer(Calendar one, Calendar two) {
		if ((one != null) && (two == null)) return true;
		if ((one == null) && (two != null)) return false;
		if ((one == null) && (two == null)) return false;
		return one.compareTo(two) > 0;
	}
	public static boolean newer(String one, String two) {
		if ((one != null) && (two == null)) return true;
		if ((one == null) && (two != null)) return false;
		if ((one == null) && (two == null)) return false;
		return one.compareTo(two) > 0;
	}
	public static Hashtable<String, String> checkAvailability(
			Hashtable<String, String> cons, String orgID, boolean DBG) {
		Hashtable<String, String> result = new Hashtable<String, String>();
		for (String cHash : cons.keySet()) {
			if(cHash == null) continue;
			if(!available(cHash, cons.get(cHash), orgID, DBG)) result.put(cHash, DD.EMPTYDATE);
		}
		return result;
	}
	/*
	public static ArrayList<String> checkAvailability(ArrayList<String> cons,
			String orgID, boolean DBG) throws P2PDDSQLException {
		ArrayList<String> result = new ArrayList<String>();
		for (String cHash : cons) {
			if(!available(cHash, orgID, DBG)) result.add(cHash);
		}
		return result;
	}
	*/
	/**
	 * 
	 * @param hash
	 * @param creation (not available if creation is newer than this)
	 * @param orgID
	 * @param DBG
	 * @return
	 */
	private static boolean available(String hash, String creation, String orgID, boolean DBG) {
		boolean result = true;
		
		D_Constituent c = D_Constituent.getConstByGID_or_GIDH (null, hash, true, false, Util.Lval(orgID));
		if (
				(c == null) 
				|| (c.getOrganizationID() != Util.lval(orgID)) 
				|| (D_Constituent.newer(creation, c.getCreationDateStr()))
				|| c.isTemporary()
				) result = false;
		
		if ((c != null) && c.isBlocked()) result = true;
		if (_DEBUG || DBG) System.out.println("D_Constituent: available: "+hash+" in "+orgID+" = "+result);
		return result;
	}
	private boolean isBlocked() {
		return this.blocked;
	}
	public boolean toggleBlock() {
		this.blocked = ! this.blocked;
		this.dirty_locals = true;
		return this.blocked;
	}
	public boolean toggleBroadcast() {
		this.broadcasted = ! this.broadcasted;
		this.dirty_locals = true;
		return this.broadcasted;
	}
	public static D_Constituent zapp(long constituentID) {
		String cID = Util.getStringID(constituentID);
		try {
			D_Constituent o = D_Constituent_Node.loaded_const_By_LocalID.get(constituentID);
			if (o != null) {
				if (o.get_StatusLockWrite() <= 0) {
					if (! D_Constituent_Node.dropLoaded(o, false)) {
						System.out.println("D_Constituent: deleteAllAboutOrg: referred = "+o.get_StatusLockWrite());
					}
				}
			}
			Application.db.delete(table.witness.TNAME,
					new String[]{table.witness.source_ID},
					new String[]{Util.getStringID(constituentID)},
					DEBUG);
			Application.db.delete(table.witness.TNAME,
					new String[]{table.witness.target_ID},
					new String[]{Util.getStringID(constituentID)},
					DEBUG);
		
			//
			return deleteConsDescription(cID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	public static D_Constituent deleteConsDescription(String constituentID) {
		try {
			Application.db.delete(table.field_value.TNAME,
					new String[]{table.field_value.constituent_ID},
					new String[]{constituentID},
					DEBUG);
			Application.db.delete(table.constituent.TNAME,
					new String[]{table.constituent.constituent_ID},
					new String[]{constituentID},
					DEBUG);
			return unlinkMemory(constituentID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	private static D_Constituent unlinkMemory(String orgID) {
		Long lID = new Long(orgID);
		D_Constituent constit = D_Constituent_Node.loaded_const_By_LocalID.get(lID);
		if (constit == null) { 
			System.out.println("D_Constituent: unlinkMemory: referred null org for: "+orgID);
			return null;
		}
		if (DEBUG) System.out.println("D_Constituent: unlinkMemory: dropped org");
		if (!D_Constituent_Node.dropLoaded(constit, true)) {
			if (DEBUG) System.out.println("D_Constituent: unlinkMemory: referred = "+constit.get_StatusLockWrite());
		} else {
			if (DEBUG) System.out.println("D_Constituent: unlinkMemory: no problem dropping");
		}
		return constit;
	}
	public static String readSignSave(long signedLID, long signerLID) {
		D_Constituent signed = D_Constituent.getConstByLID(signedLID, true, true);
		if (signed == null) {
			if (_DEBUG) System.out.println("D_Constituent: readSignSave: null for signedLID = "+signedLID+" signedLID="+signerLID);
			return null;
		}
		signed.setCreationDate();
		SK sk_ini = null;
		if (signerLID > 0) {
			D_Constituent signer = D_Constituent.getConstByLID(signerLID, true, false);
			sk_ini = signer.getSK();
		} else {
			if (! signed.isExternal())
				sk_ini = signed.getSK();
		}
		signed.sign_with_ini(sk_ini);
		
		signed.storeRequest();
		signed.releaseReference();
		return signed.getGID();
	}
/*
	public boolean isLoaded() {
		if (this.getLID() > 0) if (D_Constituent_Node.loaded_org_By_LocalID.get(this.getLID()) != null) return true;
		if (D_Constituent_Node.loaded_org_By_GIDhash.get(getGIDH()) != null) return true;
		if (D_Constituent_Node.loaded_org_By_GID.get(getGID()) != null) return true;
		return false;
	}
*/	
/*
	public boolean isLoadedInCache() {
		if (this.getLIDstr() != null)
			return ( null != D_Constituent_Node.loaded_const_By_LocalID.get(new Long(this.getLID())) );
	}
	*/
	public boolean isLoaded() {
		String GIDH, GID;
		if (!D_Constituent_Node.loaded_consts.inListProbably(this)) return false;
		long lID = this.getLID();
		long oID = this.getOrganizationID();
		if (lID > 0)
			if ( null != D_Constituent_Node.loaded_const_By_LocalID.get(new Long(lID)) ) return true;
		if ((GIDH = this.getGIDH()) != null)
			if ( null != D_Constituent_Node.getConstByGIDH(GIDH, oID) //.loaded_const_By_GIDhash.get(GIDH)
			) return true;
		if ((GID = this.getGID()) != null)
			if ( null != D_Constituent_Node.getConstByGID(GID, oID)  //.loaded_const_By_GID.get(GID)
			) return true;
		return false;
	}
	public void setSignature(byte[] sign) {
		this.signature = sign;
		this.dirty_main = true;
	}
	/**
	 * If any is not empty, this procedure tries to set the other.
	 * @param nGID
	 * @param nLID
	 */
	public void setNeighborhoodIDs(String nGID, long nLID) {
		if (nGID == null && nLID > 0) {
			nGID = D_Neighborhood.getGIDFromLID(nLID);
		}
		if (nGID != null && nLID <= 0) {
			nLID = D_Neighborhood.getLIDFromGID(nGID, this.getOrganizationID());
		}
		this.neighborhood_ID = Util.getStringID(nLID);
		this.global_neighborhood_ID = nGID;
		this.neighborhood = null;
		this.dirty_main = true;
	}
	public String getForename() {
		return this.forename;
	}
	public String getSurName() {
		return this.surname;
	}
	public D_Organization getOrganization() {
		long oID = this.getOrganizationID();
		if (oID <= 0) return null;
		return D_Organization.getOrgByLID_NoKeep(oID, true);
	}
	public static ArrayList<D_Constituent> getAllConstsByGID(String GID) {
		String sql =
				"SELECT "+" c."+table.constituent.constituent_ID+
						//" c."+table.constituent.name+
						//",c."+table.constituent.forename+
						//",o."+table.organization.name+
						" FROM "+table.constituent.TNAME+" AS c"+
						//" LEFT JOIN "+table.organization.TNAME+" AS o ON(o."+table.organization.organization_ID+"=c."+table.constituent.organization_ID+") "+
						" WHERE c."+table.constituent.global_constituent_ID+"=?;"
						;
		ArrayList<D_Constituent> list = new ArrayList<D_Constituent>();
		try {
			ArrayList<ArrayList<Object>> r = Application.db.select(sql, new String[]{GID}, DEBUG);
			for (ArrayList<Object> o : r) {
				long lID = Util.lval(o.get(0));
				D_Constituent cons = D_Constituent.getConstByLID(GID, true, false);
				list.add(cons);
			}
			return list; //return new JComboBox<Object>(list.toArray());
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return list;
	}
	public String getOrgGID() {
		return this.global_organization_ID;
	}
	public static int getConstNBinNeighborhood(long n_ID) {
		boolean DEBUG= false;
		String sql_c =
				"SELECT COUNT(*) "+//table.constituent.neighborhood_ID+
				" FROM "+table.constituent.TNAME+
				" WHERE "+table.constituent.neighborhood_ID+"=?;";
		ArrayList<ArrayList<Object>> c;
		try {
			c = Application.db.select(sql_c, new String[]{n_ID+""}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return 0;
		}
		return Util.ival(c.get(0).get(0), 0);
			//return c.size();
	}
	public static ArrayList<Long> getConstInNeighborhood(long n_ID, long o_ID) {
		ArrayList<Long> sel_c = new ArrayList<Long>();
		String sql_c = 
				"select "+table.constituent.constituent_ID+
				" from "+table.constituent.TNAME+
				" where "+table.constituent.neighborhood_ID+"=? AND "+table.constituent.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> c;
		try {
			c = Application.db.select(sql_c, new String[]{n_ID+"", o_ID+""}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return sel_c;
		}
		for (int k = 0; k < c.size(); k++ ) {
			sel_c.add(Util.Lval(c.get(k).get(0)));
		}
		return sel_c;
	}
	/**
	 * <fv.value,fv.fe_ID,count,fe.tip,fe.neigh,fv.fe_above,fv.fe_next,fv.neigh_ID> grouped by value
	 * @param fe_ID
	 * @param o_ID
	 * @return
	 */
	public static ArrayList<ArrayList<Object>> getRootConstValues(long fe_ID, long o_ID) {
		String constituents_by_values_sql =
				"SELECT "
		+ "fv." + table.field_value.value
		+ ", fv."+table.field_value.field_extra_ID
		+ ", COUNT(*)"
		+ ", fe." + table.field_extra.tip
		+ ", fe." + table.field_extra.partNeigh
		+ ", fv." + table.field_value.fieldID_above
		+ ", fv." + table.field_value.field_default_next
		+ ", fv." + table.field_value.neighborhood_ID
		+ " FROM "+table.field_value.TNAME+" AS fv " +
		// " JOIN field_extra ON fv.fieldID = field_extra.field_extra_ID " +
		" JOIN " + table.constituent.TNAME+" AS c ON c."+table.constituent.constituent_ID+" = fv."+table.field_value.constituent_ID +
		" JOIN " + table.field_extra.TNAME+" AS fe ON fe."+table.field_extra.field_extra_ID+" = fv."+table.field_value.field_extra_ID+
		" WHERE c." + table.constituent.organization_ID+"=? AND " +
		" (fv."+table.field_value.field_extra_ID+" = ?)"
				+ " OR (fv."+table.field_value.fieldID_above+" ISNULL AND fe."+table.field_extra.partNeigh+" > 0) "
		+ " GROUP BY fv."+table.field_value.value
		+ " ORDER BY fv."+table.field_value.value+" DESC;";
		
		ArrayList<ArrayList<Object>> subneighborhoods = new ArrayList<ArrayList<Object>>();
		try {
			subneighborhoods = Application.db.select( constituents_by_values_sql,
				new String[]{""+o_ID,
				""+fe_ID}, DEBUG);
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return subneighborhoods;
	}
	public int getFieldValuesFixedNB() {
		if (address == null) return 0;
		D_Organization org = D_Organization.getOrgByLID_NoKeep(_organization_ID, true);
		int result = 0;
		for ( int k = 0; k < address.length ; k ++ ) {
			D_OrgParam fe = address[k].field_extra;
			if (fe == null) address[k].field_extra = fe = org.getFieldExtra(address[k].field_extra_GID);
			if (fe == null) continue;
			if (fe.partNeigh <= 0) result ++;
		}
		return result;
	}
	public static void delConstituent(long constituentID) {
		boolean DEBUG = false;
		if(DEBUG) System.err.println("Deleting ID = "+constituentID);
		try{
			Application.db.delete(table.field_value.TNAME, new String[]{table.field_value.constituent_ID}, new String[]{constituentID+""});
			Application.db.delete(table.witness.TNAME, new String[]{table.witness.source_ID}, new String[]{constituentID+""});
			Application.db.delete(table.witness.TNAME, new String[]{table.witness.target_ID}, new String[]{constituentID+""});
			Application.db.delete(table.identity_ids.TNAME, new String[]{table.identity_ids.constituent_ID}, new String[]{constituentID+""});
			Application.db.delete(table.motion.TNAME, new String[]{table.motion.constituent_ID}, new String[]{constituentID+""});
			Application.db.delete(table.justification.TNAME, new String[]{table.justification.constituent_ID}, new String[]{constituentID+""});
			Application.db.delete(table.signature.TNAME, new String[]{table.signature.constituent_ID}, new String[]{constituentID+""});
			Application.db.delete(table.news.TNAME, new String[]{table.news.constituent_ID}, new String[]{constituentID+""});
			Application.db.delete(table.translation.TNAME, new String[]{table.translation.submitter_ID}, new String[]{constituentID+""});
			Application.db.delete(table.constituent.TNAME, new String[]{table.constituent.constituent_ID}, new String[]{constituentID+""});
			Application.db.delete(table.constituent.TNAME, new String[]{table.constituent.submitter_ID}, new String[]{constituentID+""});
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	public String getSlogan() {
		return this.slogan;
	}
	public String getEmail() {
		return this.email;
	}
	public String getSubmitterLIDstr() {
		return this.submitter_ID;
	}
	public static ArrayList<Long> getOrphans(long o_ID) {
		boolean DEBUG = false;
		ArrayList<Long> r = new ArrayList<Long>();
	    	String sql = 
	    		"SELECT "
				//+ " c."+table.constituent.name+
				//", c."+table.constituent.forename+
		        //","
		        + " c."+table.constituent.constituent_ID+
		        //", c."+table.constituent.external+
		        //", c."+table.constituent.global_constituent_ID +
		        //", c."+table.constituent.submitter_ID +
		        //", c."+table.constituent.slogan +
		        //", c."+table.constituent.email +
	    		//+table.constituent._fields_constituents+
	    		" FROM "+table.constituent.TNAME+" AS c "+
	    		" LEFT JOIN "+table.neighborhood.TNAME+" AS n ON(c."+table.constituent.neighborhood_ID+"=n."+table.neighborhood.neighborhood_ID+") "+
	    		" WHERE ( c."+table.constituent.neighborhood_ID+" ISNULL OR n."+table.neighborhood.neighborhood_ID+" ISNULL )" +
	    		((DD.CONSTITUENTS_ORPHANS_FILTER_BY_ORG)?" AND ( c."+table.constituent.organization_ID+"=? ) ":"")+
	    				";";
		
	    		String[] params = new String[]{};
	    		if(DD.CONSTITUENTS_ORPHANS_FILTER_BY_ORG) params = new String[]{Util.getStringID(o_ID)};
	    		try {
					ArrayList<ArrayList<Object>> identities = Application.db.select(sql, params, DEBUG);
					for (int k = 0; k < identities.size(); k ++) {
						r.add(Util.Lval(identities.get(k).get(0)));
					}
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
				return r;
	}
	public static Object getConstNBinOrganization(String orgID) {
		Object result = null;
		String sql_co = "SELECT count(*) FROM "+table.constituent.TNAME+
		" WHERE "+table.constituent.organization_ID+" = ? AND "+table.constituent.op+" = ?;";
		try {
			ArrayList<ArrayList<Object>> orgs = Application.db.select(sql_co, new String[]{orgID, "1"});
			if(orgs.size()>0) result = orgs.get(0).get(0);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}
	public int get_StatusReferences() {
		return status_references;
	}
	public int inc_StatusReferences() {
		this.assertReferenced(); // keep it in the process to avoid it being dropped before inc
		Application_GUI.ThreadsAccounting_ping("Raised constituent references for "+this.getNameFull());
		return status_references++;
	}
	public void dec_StatusReferences() {
		if (this.get_StatusReferences() <= 0) {
			Util.printCallPath("Why getting: "+get_StatusReferences());
			return;
		}
		this.status_references--;
		Application_GUI.ThreadsAccounting_ping("Dropped constituent references for "+this.getNameFull());
	}
	public int get_StatusLockWrite() {
		return status_lock_write;
	}
	StackTraceElement[] lastPath;
	final private Object monitor_reserve = new Object();
	public void inc_StatusLockWrite() {
		if (this.get_StatusLockWrite() > 0) {
			//Util.printCallPath("Why getting: "+getStatusReferences());
			//Util.printCallPath("D_Peer: incStatusReferences: Will sleep for getting: "+getStatusLockWrite()+" for "+getName());
			//Util.printCallPath(lastPath, "Last lp path was: ", "     ");
			int limit = 1;
//			if (this == data.HandlingMyself_Peer.get_myself_or_null()) {
//				limit = 2;
//			}
			synchronized(monitor_reserve) {
				if (
						(this.get_StatusLockWrite() >= limit)
						||
						(this.get_StatusLockWrite() >= limit)
						)
					try {
						do {
							Application_GUI.ThreadsAccounting_ping("Wait peer references for "+getNameFull());
							monitor_reserve.wait(10000); // wait 5 seconds, and do not re-sleep on spurious wake-up
							Application_GUI.ThreadsAccounting_ping("Got peer references for "+getNameFull());
							//Util.printCallPath("D_Peer: incStatusReferences: After sleep is getting: "+getStatusLockWrite()+" for "+getName());
							if (this.get_StatusLockWrite() > limit) Util.printCallPath(lastPath, "Last l path was: ", "     ");
							if (DD.RELEASE) break;
						} while (this.get_StatusLockWrite() > limit);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				if (this.get_StatusLockWrite() >= limit) {
					Util.printCallPath(this+"\nSpurious wake after 5s this=: "+get_StatusLockWrite()+" for "+getNameFull());
					Util.printCallPath(lastPath, "Last path was: ", "     ");
				}
			}
		}
		lastPath = Util.getCallPath();
		this.status_lock_write++;
	}
	public void dec_StatusLockWrite() {
		if (this.get_StatusLockWrite() <= 0) {
			Util.printCallPath("Why getting: "+get_StatusLockWrite());
			return;
		}
		this.status_lock_write--;
		// Application_GUI.ThreadsAccounting_ping("Drop peer references for "+getName());
		synchronized(monitor_reserve) {
			monitor_reserve.notify();
		}
		//Application_GUI.ThreadsAccounting_ping("Dropped peer references for "+getName());
	}
	
}

class D_Constituent_SaverThread extends util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000; // ms to sleep
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	/**
	 * The next monitor is needed to ensure that two D_Constituent_SaverThreadWorker are not concurrently modifying the database,
	 * and no thread is started when it is not needed (since one is already running).
	 */
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Constituent_SaverThread() {
		super("D_Constituent Saver", true);
		//start ();
	}
	public void _run() {
		for (;;) {
			if (stop) return;
			synchronized(saver_thread_monitor) {
				new D_Constituent_SaverThreadWorker().start();
			}
			/*
			synchronized(saver_thread_monitor) {
				D_Constituent de = D_Constituent.need_saving_next();
				if (de != null) {
					if (DEBUG) System.out.println("D_Constituent_Saver: loop saving "+de);
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
					//System.out.println("D_Constituent_Saver: idle ...");
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

class D_Constituent_SaverThreadWorker extends util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000;
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Constituent_SaverThreadWorker() {
		super("D_Constituent Saver Worker", false);
		//start ();
	}
	public void _run() {
		synchronized(D_Constituent_SaverThread.saver_thread_monitor) {
			D_Constituent de;
			boolean edited = true;
			if (DEBUG) System.out.println("D_Constituent_Saver: start");
			
			 // first try objects being edited
			de = D_Constituent.need_saving_obj_next();
			
			// then try remaining objects 
			if (de == null) { de = D_Constituent.need_saving_next(); edited = false; }
			
			if (de != null) {
				if (DEBUG) System.out.println("D_Constituent_Saver: loop saving "+de.getNameFull());
				Application_GUI.ThreadsAccounting_ping("Saving");
				if (edited) D_Constituent.need_saving_obj_remove(de);
				else D_Constituent.need_saving_remove(de);//, de.instance);
				if (DEBUG) System.out.println("D_Constituent_Saver: loop removed need_saving flag");
				// try 3 times to save
				for (int k = 0; k < 3; k++) {
					try {
						if (DEBUG) System.out.println("D_Constituent_Saver: loop will try saving k="+k);
						de.storeAct();
						if (DEBUG) System.out.println("D_Constituent_Saver: stored org:"+de.getNameFull());
						break;
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
						synchronized(this) {
							try {
								if (DEBUG) System.out.println("D_Constituent_Saver: sleep");
								wait(SAVER_SLEEP_ON_ERROR);
								if (DEBUG) System.out.println("D_Constituent_Saver: waked error");
							} catch (InterruptedException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
			} else {
				Application_GUI.ThreadsAccounting_ping("Nothing to do!");
				if (DEBUG) System.out.println("D_Constituent_Saver: idle ...");
			}
		}
		synchronized(this) {
			try {
				if (DEBUG) System.out.println("D_Constituent_Saver: sleep");
				wait(SAVER_SLEEP);
				if (DEBUG) System.out.println("D_Constituent_Saver: waked");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

