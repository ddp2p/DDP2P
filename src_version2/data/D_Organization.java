package data;

import static java.lang.System.err;
import static java.lang.System.out;
import static util.Util.__;
import hds.ASNSyncPayload;
import hds.ASNSyncRequest;
import hds.ResetOrgInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;

import ciphersuits.Cipher;
import ciphersuits.PK;
import ciphersuits.SK;
import config.Application;
import config.Application_GUI;
import config.DD;
import streaming.OrgPeerDataHashes;
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

public
class D_Organization extends ASNObj implements  DDP2P_DoubleLinkedList_Node_Payload<D_Organization>, Summary { 
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;

	public static final int A_NON_FORCE_COL = 4; 
	private static final String V0 = "0";
	public static final byte TAG = Encoder.TAG_SEQUENCE;
	public static final boolean DEFAULT_BROADCASTED_ORG = true;
	public static final boolean DEFAULT_BROADCASTED_ORG_ITSELF = false;

	// Data signed
	public String version=V0; //PrintableString
	public String global_organization_ID; //PrintableString
	public String global_organization_IDhash;
	public String name; //UTF8String OPTIONAL
	public Calendar reset_date;
	public Calendar last_sync_date;
	public String _last_sync_date;
	public D_OrgParams params = new D_OrgParams(); // AC1 OPTIONAL
	public D_OrgConcepts concepts = new D_OrgConcepts(); //AC2 OPTIONAL
	public byte[] signature;//=new byte[0]; in String representation may start with "G:" 
	public byte[] signature_initiator;// in String representation may start with "G:"

	// Data to be transmitted, relevant to this organization
	public RequestData availableHashes;
	public OrgPeerDataHashes specific_request = new OrgPeerDataHashes();
	
	public D_Peer creator;
	public data.D_Message requested_data[]; //AC11, data requested on a GID basis
	public ASNNeighborhoodOP neighborhoods[]; //AC3 OPTIONAL
	public ASNConstituentOP constituents[]; //AC4 OPTIONAL
	public D_Witness[] witnesses; //AC5 PTIONAL
	public D_Motion motions[]; //AC6 OPTIONAL
	public D_Justification justifications[]; //AC7 OPTIONAL
	public D_Vote signatures[]; // AC8 OPT
	public D_Translations translations[]; //AC9 OPTIONAL
	public D_News news[];	// AC10 OPTIONAL
	
	// locals
	private String organization_ID;
	private long _organization_ID;
	public String creator_ID;
	
	// preferences
	public Calendar arrival_date;
	public Calendar preferences_date; 
	public String _preferences_date; 
	private String first_provider_peer;
	public boolean blocked = false;
	public boolean requested = false;
	public boolean broadcasted = DEFAULT_BROADCASTED_ORG_ITSELF;
	public boolean broadcast_rule = true;
	public HashSet<String> preapproved = new HashSet<String>();
	public String _preapproved = null; // ordered (for saving)
	//public int status_references = 0;
	
	ciphersuits.Cipher keys;
	
	public boolean dirty_main = false;
	public boolean dirty_params = false;
	public boolean dirty_locals = true;
	public boolean dirty_mydata = false;
	
	public boolean loaded_globals = false;
	public boolean loaded_locals = false;
	private static Object monitor_object_factory = new Object();
	static Object lock_organization_GID_storage = new Object(); // duplicate of the above
	public static int MAX_LOADED_ORGS = 10000;
	public static long MAX_ORGS_RAM = 10000000;
	private static final int MIN_ORGS_RAM = 2;
	D_Organization_Node component_node = new D_Organization_Node(null, null);
	private int status_references = 0;
	private boolean temporary = true;
	private boolean hidden = false;
	
	D_Organization_My mydata = new D_Organization_My();

	public static class D_Organization_Node {
		private static final int MAX_TRIES = 30;
		/** Currently loaded peers, ordered by the access time*/
		private static DDP2P_DoubleLinkedList<D_Organization> loaded_orgs = new DDP2P_DoubleLinkedList<D_Organization>();
		private static Hashtable<Long, D_Organization> loaded_org_By_LocalID = new Hashtable<Long, D_Organization>();
		private static Hashtable<String, D_Organization> loaded_org_By_GID = new Hashtable<String, D_Organization>();
		private static Hashtable<String, D_Organization> loaded_org_By_GIDhash = new Hashtable<String, D_Organization>();
		private static long current_space = 0;
		
		/** message is enough (no need to store the Encoder itself) */
		public byte[] message;
		public DDP2P_DoubleLinkedList_Node<D_Organization> my_node_in_loaded;
	
		public D_Organization_Node(byte[] message,
				DDP2P_DoubleLinkedList_Node<D_Organization> my_node_in_loaded) {
			this.message = message;
			this.my_node_in_loaded = my_node_in_loaded;
		}
		private static void register_fully_loaded(D_Organization crt) {
			assert((crt.component_node.message==null) && (crt.loaded_globals));
			if(crt.component_node.message != null) return;
			if(!crt.loaded_globals) return;
			if ((crt.getGID() != null) && (!crt.isTemporary())) {
				byte[] message = crt.encode();
				synchronized(loaded_orgs) {
					crt.component_node.message = message; // crt.encoder.getBytes();
					if(crt.component_node.message != null) current_space += crt.component_node.message.length;
				}
			}
		}
		/*
		private static void unregister_loaded(D_Organization crt) {
			synchronized(loaded_orgs) {
				loaded_orgs.remove(crt);
				loaded_org_By_LocalID.remove(new Long(crt.getLID()));
				loaded_org_By_GID.remove(crt.getGID());
				loaded_org_By_GIDhash.remove(crt.getGIDH());
			}
		}
		*/
		private static void register_loaded(D_Organization crt) {
			crt.reloadMessage(); 
			synchronized(loaded_orgs) {
				String gid = crt.getGID();
				String gidh = crt.getGIDH_or_guess();
				long lid = crt.getLID_forced();
				
				loaded_orgs.offerFirst(crt);
				if (lid > 0) loaded_org_By_LocalID.put(new Long(lid), crt);
				if (gid != null) loaded_org_By_GID.put(gid, crt);
				if (gidh != null) loaded_org_By_GIDhash.put(gidh, crt);
				if (crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				int tries = 0;
				while ((loaded_orgs.size() > MAX_LOADED_ORGS)
						|| (current_space > MAX_ORGS_RAM)) {
					if (loaded_orgs.size() <= MIN_ORGS_RAM) break; // at least _crt_peer and _myself
	
					if (tries > MAX_TRIES) break;
					tries ++;
					D_Organization candidate = loaded_orgs.getTail();
					if ((candidate.getStatusReferences() > 0)
							||
							D_Organization.is_crt_org(candidate)
							//||
							//(candidate == HandlingMyself_Peer.get_myself())
							) 
					{
						setRecent(candidate);
						continue;
					}
					
					D_Organization removed = loaded_orgs.removeTail();//remove(loaded_peers.size()-1);
					loaded_org_By_LocalID.remove(new Long(removed.getLID_forced())); 
					loaded_org_By_GID.remove(removed.getGID());
					loaded_org_By_GIDhash.remove(removed.getGIDH_or_guess());
					if (DEBUG) System.out.println("D_Organization: register_loaded: remove GIDH="+removed.getGIDH_or_guess());
					if (removed.component_node.message != null) current_space -= removed.component_node.message.length;				
				}
			}
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_Organization crt) {
			loaded_orgs.moveToFront(crt);
		}
		public static boolean dropLoaded(D_Organization removed, boolean force) {
			boolean result = true;
			synchronized(loaded_orgs) {
				if (removed.getStatusReferences() > 0 || removed.get_DDP2P_DoubleLinkedList_Node() == null)
					result = false;
				if (! force && ! result) {
					System.out.println("D_Organization: dropLoaded: abandon: force="+force+" rem="+removed);
					return result;
				}
				
				loaded_orgs.remove(removed);
				if (removed.getLIDstr_forced() != null) loaded_org_By_LocalID.remove(new Long(removed.getLID_forced())); 
				if (removed.getGID() != null) loaded_org_By_GID.remove(removed.getGID());
				if (removed.getGIDH_or_guess() != null) loaded_org_By_GIDhash.remove(removed.getGIDH_or_guess());
				if (DEBUG) System.out.println("D_Organization: drop_loaded: remove GIDH="+removed.getGIDH_or_guess());
				if (removed.component_node.message != null) current_space -= removed.component_node.message.length;	
				if (DEBUG) System.out.println("D_Organization: dropLoaded: exit with force="+force+" result="+result);
				return result;
			}
		}
	}
	static class D_Organization_My {
		String name;// table.my_organization_data.
		String category;
		String creator;
		//String data_to_request; //replicates organization.specific_requests
		long row;
	}
	
	/**
	 * Storage Methods
	 */
	
	public void releaseReference() {
		if (getStatusReferences() <= 0) Util.printCallPath("Null reference already!");
		else decStatusReferences();
		//System.out.println("D_Organization: releaseReference: "+status_references);
		//Util.printCallPath("");
	}
	
	public void assertReferenced() {
		assert (getStatusReferences() > 0);
	}
	
	/**
	 * The entries that need saving
	 */
	private static HashSet<String> _need_saving = new HashSet<String>();
	private static HashSet<D_Organization> _need_saving_obj = new HashSet<D_Organization>();
	
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Organization> 
	set_DDP2P_DoubleLinkedList_Node(DDP2P_DoubleLinkedList_Node<D_Organization> node) {
		DDP2P_DoubleLinkedList_Node<D_Organization> old = this.component_node.my_node_in_loaded;
		this.component_node.my_node_in_loaded = node;
		return old;
	}
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Organization> 
	get_DDP2P_DoubleLinkedList_Node() {
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
	static void need_saving_remove(String GIDH) {
		if (DEBUG) System.out.println("D_Organization:need_saving_remove: remove "+GIDH);
		_need_saving.remove(GIDH);
		if (DEBUG) dumpNeeds(_need_saving);
	}
	static void need_saving_obj_remove(D_Organization org) {
		if (DEBUG) System.out.println("D_Organization:need_saving_obj_remove: remove "+org);
		_need_saving_obj.remove(org);
		if (DEBUG) dumpNeedsObj(_need_saving_obj);
	}
	static D_Organization need_saving_next() {
		Iterator<String> i = _need_saving.iterator();
		if (!i.hasNext()) return null;
		String c = i.next();
		if (DEBUG) System.out.println("D_Organization: need_saving_next: next: "+c);
		D_Organization r = D_Organization_Node.loaded_org_By_GIDhash.get(c);
		if (r == null) {
			if (_DEBUG) {
				System.out.println("D_Organization Cache: need_saving_next null entry "
						+ "needs saving next: "+c);
				System.out.println("D_Organization Cache: "+dumpDirCache());
			}
			return null;
		}
		return r;
	}
	static D_Organization need_saving_obj_next() {
		Iterator<D_Organization> i = _need_saving_obj.iterator();
		if (!i.hasNext()) return null;
		D_Organization r = i.next();
		if (DEBUG) System.out.println("D_Organization: need_saving_obj_next: next: "+r);
		//D_Organization r = D_Organization_Node.loaded_org_By_GIDhash.get(c);
		if (r == null) {
			if (_DEBUG) {
				System.out.println("D_Organization Cache: need_saving_obj_next null entry "
						+ "needs saving obj next: "+r);
				System.out.println("D_Organization Cache: "+dumpDirCache());
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
	private static void dumpNeedsObj(HashSet<D_Organization> _need_saving2) {
		System.out.println("Needs:");
		for ( D_Organization i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	public static String dumpDirCache() {
		String s = "[";
		s += D_Organization_Node.loaded_orgs.toString();
		s += "]";
		return s;
	}
	
	/**
	 * Methods
	 */
	
	@Override
	public String toSummaryString() {
		String result = "\n OrgData: [\n";
		//";\n  ver = "+version+
		//";\n  id  = "+global_organization_ID+";\n"+
		result += " name="+name+"; ";
		result += " b_r="+broadcast_rule+"; ";
		//";\n  l_s_d="+_last_sync_date+
		//";\n  params="+params+
		//";\n  creator="+creator+
		//";\n  concepts=<"+concepts+">"+
		//";\n  signature=<"+Util.byteToHexDump(signature)+">"+
		if(creator != null) result += ";\n  creator="+creator.toSummaryString();
		if(neighborhoods != null) result += ";\n  neighborhoods="+Util.concatSummary(neighborhoods, "\nOrgData-Neighborhood:", null);
		if(constituents != null) result += ";\n  constituents="+Util.concatSummary(constituents, "\nOrgData-Constituents:", null);
		if(witnesses != null) result += ";\n  witnesses="+Util.concatSummary(witnesses, "\nOrgData-Witnesses:", null);
		if(motions != null) result += ";\n  motions="+Util.concatSummary(motions, "\nOrgData-Motions:", null);
		if(justifications != null) result += ";\n  justifications="+Util.concat(justifications, "\nOrgData-Justifications:");
		if(signatures != null) result += ";\n  signatures="+Util.concat(signatures, "\nOrgData-Signatures:");
		if(translations != null) result += ";\n  translations="+Util.concat(translations, "\nOrgData-Translations:");
		if(news != null) result += ";\n  news="+Util.concat(news, "\nOrgData-News:");
		if(availableHashes!=null) result += ";\n  available="+availableHashes.toSummaryString();
		//else result += ";\n  available=null"; // not encoded (sent in advertised in SyncPayload)
		result += "\n]";
		return result;
	}
	public String toString() {
		String result = "OrgData: [tmp="+temporary;
		result += ";\n  ver = "+version;
		result += ";\n  id  = "+global_organization_ID;
		result += ";\n  name="+name;
		result += ";\n  broadcast_rule="+broadcast_rule;
		result += ";\n  l_s_d="+_last_sync_date;
		result += ";\n  params="+params;
		result += ";\n  creator_ID="+creator_ID;
		result += ";\n  creator="+creator;
		result += ";\n  concepts=<"+concepts+">";
		result += ";\n  signature=<"+Util.byteToHexDump(signature)+">";
		result += ";\n  signature_initiator=<"+Util.byteToHexDump(signature_initiator)+">";
		if(neighborhoods != null)
			result += ";\n  neighborhoods="+Util.concat(neighborhoods, "\nOrgData-Neighborhood:");
		if(constituents != null)
			result += ";\n  constituents="+Util.concat(constituents, "\nOrgData-Constituents:");
		if(witnesses != null)
			result += ";\n  witnesses="+Util.concat(witnesses, "\nOrgData-Witnesses:");
		if(motions != null)
			result += ";\n  motions="+Util.concat(motions, "\nOrgData-Motions:");
		if(justifications != null)
			result += ";\n  justifications="+Util.concat(justifications, "\nOrgData-Justifications:");
		if(signatures != null)
			result += ";\n  signatures="+Util.concat(signatures, "\nOrgData-Signatures:");
		if(translations != null)
			result +=  ";\n  translations="+Util.concat(translations, "\nOrgData-Translations:");
		if(news != null)
			result +=  ";\n  news="+Util.concat(news, "\nOrgData-News:");
		if(availableHashes!=null)
			result +=  ";\n  available="+availableHashes;
		result += "\n]";
		return result;
	}
	/**
	 * Allocate an empty organization with the current creation time, and return lID.
	 * To be used during editing.
	 * @return
	 */
	public static long createNewOrg() {
		String currentTime = Util.getGeneralizedTime();
		long org_id = -1;
		try {
			org_id = Application.db.insert(table.organization.TNAME,
					new String[]{table.organization.creation_date, table.organization.creation_date, table.organization.hash_org_alg},
					new String[]{currentTime, currentTime, table.organization.hash_org_alg_crt});
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return org_id;
	}
	private static final String sql_getOrgByLID =
			"SELECT " + table.organization.field_list +
			" FROM " + table.organization.TNAME +
			" WHERE "+ table.organization.organization_ID + " = ?;";
	public static D_Organization getEmpty() {return new D_Organization();}
	@Override
	public D_Organization instance() {return new D_Organization();}
	private D_Organization() {}
	/**
	 * To be used only privately. Loads from database!
	 * @param __organization_ID
	 * @throws P2PDDSQLException
	 */
	private D_Organization (long __organization_ID) throws P2PDDSQLException {
		if (__organization_ID <= 0) throw new RuntimeException("Invalid org LID: "+__organization_ID);
		this.setLID(__organization_ID);
		init_ByLID(getLIDstr());
	}

	private D_Organization (String __organization_ID) throws P2PDDSQLException {
		this.setLID(__organization_ID);
		if (getLID() <= 0) throw new RuntimeException("Invalid org LID: "+__organization_ID);
		init_ByLID(getLIDstr());
	}
	/**
	 * On creation sets dirty flags but caller should call storeRequest()
	 * @param gID
	 * @param isGID
	 * @param failOnNew
	 * @param __org
	 * @throws P2PDDSQLException 
	 */
	private D_Organization(String gID, String gIDH, boolean create, D_Peer __peer) throws P2PDDSQLException {
		if (_DEBUG) err.println("D_Organization: getOrgByGID_or_GIDhash: org_gidh: "+gIDH);
		if (gIDH == null) gIDH = D_Organization.getOrgGIDHashGuess(gID);
		if (_DEBUG) err.println("D_Organization: getOrgByGID_or_GIDhash: org_gidh: "+gIDH);
		init_ByGID(gID, gIDH, create, __peer);
	}
	
	/**
	 * 
	 * @param GID
	 * @param GIDhash
	 * @param exception (true to throw RuntimeException of failure)
	 * @return
	 */
	static public boolean verifyGIDhash(String GID, String GIDhash, boolean exception) {
		if (GID == null || GIDhash == null) return true;
			String hash = D_Organization.getOrgGIDHashGuess(GID);
			if (!hash.equals(GIDhash)) {
				System.out.println("D_Organization:getOrgByGID_or_GIDhash_Attempt: mismatch "+GIDhash+" vs "+hash);
				if (exception) throw new RuntimeException("No GID and GIDhash match");
				return false;
			} 
			return true;
	}
	/**
	 * This should return non-null if the coomparison fails!
	 * @param GID
	 * @param GIDhash
	 * @return the correct hash for the GID
	 */
	static public String verifyGIDhashGetGIDH(String GID, String GIDhash) {
		if (GID == null || GIDhash == null) return null;
			String hash = D_Organization.getOrgGIDHashGuess(GID);
			if (!hash.equals(GIDhash)) {
				System.out.println("D_Organization:getOrgByGID_or_GIDhash_Attempt: mismatch "+GIDhash+" vs "+hash);
				//throw new RuntimeException("No GID and GIDhash match");
				return hash;
			} 
			return null;
	}

	/**
	 * exception raised on error
	 * @param ID
	 * @param load_Globals 
	 * @return
	 */
	static private D_Organization getOrgByLID_AttemptCacheOnly_NoKeep(long ID, boolean load_Globals) {
		if (ID <= 0) return null;
		Long id = new Long(ID);
		D_Organization crt = D_Organization_Node.loaded_org_By_LocalID.get(id);
		if (crt == null) return null;
		
		synchronized (monitor_object_factory) {
			if (load_Globals && !crt.loaded_globals){
				crt.loadGlobals();
				D_Organization_Node.register_fully_loaded(crt);
			}
		}
		D_Organization_Node.setRecent(crt);
		return crt;
	}
	/**
	 * exception raised on error
	 * @param LID
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static private D_Organization getOrgByLID_AttemptCacheOnly(Long LID, boolean load_Globals, boolean keep) {
		if (LID == null) return null;
		if (keep) {
			synchronized(monitor_object_factory) {
				D_Organization  crt = getOrgByLID_AttemptCacheOnly_NoKeep(LID.longValue(), load_Globals);
				if (crt != null) {			
					crt.incStatusReferences();
					if (crt.getStatusReferences() > 1) {
						System.out.println("D_Organization: getOrgByGIDhash_AttemptCacheOnly: "+crt.getStatusReferences());
						Util.printCallPath("");
					}
				}
				return crt;
			}
		} else {
			return getOrgByLID_AttemptCacheOnly_NoKeep(LID.longValue(), load_Globals);
		}
	}

	static public D_Organization getOrgByLID(String ID, boolean load_Globals, boolean keep) {
		return getOrgByLID(Util.lval(ID), load_Globals, keep);
	}
	static public D_Organization getOrgByLID(long ID, boolean load_Globals, boolean keep) {
		if (ID <= 0) {
			if (DEBUG) Util.printCallPath("Why?");
			return null;
		}
		Long id = new Long(ID);
		// try first to see if one can avoid waiting for the monitor
		// can comment this out if the structure is not safe for multithreading
		D_Organization crt = D_Organization.getOrgByLID_AttemptCacheOnly(id, load_Globals, keep);
		if (crt != null) return crt;
		
		synchronized (monitor_object_factory) {
			// retry, just in case some other thread just got it
			crt = D_Organization.getOrgByLID_AttemptCacheOnly(id, load_Globals, keep);
			if (crt != null) return crt;
			
			try {
				crt = new D_Organization(ID);
				if (keep) {
					crt.incStatusReferences();
					if (crt.getStatusReferences() > 1) {
						System.out.println("D_Organization: getOrgByLID_AttemptCacheOnly: "+crt.getStatusReferences());
						Util.printCallPath("");
					}
				}
			} catch (Exception e) {
				if (DEBUG) e.printStackTrace();
				return null;
			}
			D_Organization_Node.register_loaded(crt);
			return crt;
		}
	}
	/**
	 * If the data is not in the cache, it will load it
	 * @param ID
	 * @param load_Globals
	 * @return
	 */
	static public D_Organization getOrgByLID_NoKeep(long ID, boolean load_Globals) {
		if (ID <= 0) return null;
		return getOrgByLID(ID, load_Globals, false);
	}
	static public D_Organization getOrgByLID_NoKeep(String ID, boolean load_Globals){
		try {
			return getOrgByLID_NoKeep(Util.lval(ID), load_Globals);
		} catch (Exception e) {
			return null;
		}
	}

	static private D_Organization getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(String GID, String GIDhash, boolean load_Globals) {
		D_Organization  crt = null;
		if (GIDhash != null) crt = D_Organization_Node.loaded_org_By_GIDhash.get(GIDhash);
		if ((crt == null) && (GID != null)) crt = D_Organization_Node.loaded_org_By_GID.get(GID);
		
//		if ((GID != null) && ((crt == null) || (GIDhash == null) || DD.VERIFY_GIDH_ALWAYS)) {
//			if (crt == null) crt = D_Organization_Node.loaded_org_By_GIDhash.get(GIDhash);
//		}
		
		if (crt != null) {
			if (load_Globals && !crt.loaded_globals) {
				crt.loadGlobals();
				D_Organization_Node.register_fully_loaded(crt);
			}
			D_Organization_Node.setRecent(crt);
			return crt;
		}
		return null;
	}
	/**
	 * No keep
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals 
	 * @return
	 */
	static private D_Organization getOrgByGID_or_GIDhash_AttemptCacheOnly(String GID, String GIDhash, boolean load_Globals, boolean keep) {
		if ((GID == null) && (GIDhash == null)) return null;
		if ((GID != null) && (GIDhash == null)) GIDhash = D_Organization.getOrgGIDHashGuess(GID);
		if (keep) {
			synchronized (monitor_object_factory) {
				D_Organization crt = getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(GID, GIDhash, load_Globals);
				if (crt != null) {
					crt.incStatusReferences();
					if (crt.getStatusReferences() >  1) {
						Util.printCallPath("Why?");
					}
				}
				return crt;
			}
		} else 
			return getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(GID, GIDhash, load_Globals);
	}
	/**
	 * 
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals
	 * @param keep
	 * @return
	 */
	static public D_Organization getOrgByGID_or_GIDhash_NoCreate(String GID, String GIDhash, boolean load_Globals, boolean keep) {
		return getOrgByGID_or_GIDhash(GID, GIDhash, load_Globals, false, keep, null);
	}
	/**
	 * 
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals
	 * @param create
	 * @param keep
	 * @param __peer : provider
	 * @return
	 */
	static public D_Organization getOrgByGID_or_GIDhash(String GID, String GIDhash, boolean load_Globals, boolean create, boolean keep, D_Peer __peer) {
		if ((GID == null) && (GIDhash == null)) return null;
		if ((GID != null) && (GIDhash == null)) GIDhash = D_Organization.getOrgGIDHashGuess(GID);
		if (!D_Organization.isOrgGID(GID)) GID = null;
		
		if (create) {
			if (! keep) Util.printCallPath("Why?");
			keep = true;
		}
		D_Organization crt;
		try {
			crt = getOrgByGID_or_GIDhash_AttemptCacheOnly(GID, GIDhash, load_Globals, keep);
			if (crt != null) return crt;
		} catch (Exception e) {e.printStackTrace();}

		
		synchronized (monitor_object_factory) {
			try {
				crt = getOrgByGID_or_GIDhash_AttemptCacheOnly(GID, GIDhash, load_Globals, keep);
				if (crt != null) return crt;
			} catch (Exception e) {e.printStackTrace();}
			
			try {
				if (DEBUG) err.println("D_Organization: getOrgByGID_or_GIDhash: org_gidh: "+GIDhash);
				crt = new D_Organization(GID, GIDhash, create, __peer);
				if (keep) {
					crt.incStatusReferences();
				}
				D_Organization_Node.register_loaded(crt);
				return crt;
			} catch (Exception e) {
				//e.printStackTrace();//simply not present
				if (DEBUG) System.out.println("D_Organization: getOrgByGID_or_GIDhash: error loading");
				return null;
			}
			// if newly created, then they are dirty
			//if (crt.dirty_any()) crt.storeRequest();
			//Util.printCallPath("CRT="+crt);
		}
	}
	
	
	
//	/**
//	 * exception raised on error
//	 * @param GIDhash
//	 * @param load_Globals 
//	 * @param keep : if true, avoid releasing this until calling releaseReference()
//	 * @return
//	 */
//	static private D_Organization getOrgByGIDhash_AttemptCacheOnly(String GIDhash, boolean load_Globals, boolean keep) {
//		if (GIDhash == null) return null;
//		if (keep) {
//			synchronized(monitor_object_factory) {
//				D_Organization  crt = getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(null, GIDhash, load_Globals);
//				if (crt != null) {			
//					crt.status_references ++;
//					if (crt.status_references > 1) {
//						System.out.println("D_Organization: getOrgByGIDhash_AttemptCacheOnly: "+crt.status_references);
//						Util.printCallPath("");
//					}
//				}
//				return crt;
//			}
//		} else {
//			return getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(null, GIDhash, load_Globals);
//		}
//	}
//	/**
//	 * Only attempts too load (only based on GID) if the data is already in the cache
//	 * exception raised on error
//	 * @param GID
//	 * @param load_Globals 
//	 * @return
//	 */
//	static private D_Organization getOrgByGID_only_AttemptCacheOnly(String GID, boolean load_Globals) {
//		if (GID == null) return null;
//		D_Organization crt = D_Organization_Node.loaded_org_By_GID.get(GID);
//		if (crt == null) return null;
//		
//		if (load_Globals && !crt.loaded_globals){
//			crt.loadGlobals();
//			D_Organization_Node.register_fully_loaded(crt);
//		}
//		D_Organization_Node.setRecent(crt);
//		return crt;
//	}
	/**
	 * 
	 * @param GID
	 * @param GIDH
	 * @param load_Globals
	 * @param create
	 * @param __peer
	 * @return
	 */
	static public D_Organization getOrgByGID_or_GIDhash_StoreIfNew_NoKeep(String GID, String GIDH, boolean load_Globals, boolean create, D_Peer __peer) {
		D_Organization p = //getOrgByGID_UpdatingNewGID(GID, load_Globals, create, __peer);
				D_Organization.getOrgByGID_or_GIDhash(GID, GIDH, load_Globals, create, true, __peer);
		// if newly created, then they are dirty
		if ((p != null) && p.dirty_any()) {
			p.storeRequest();
		}
		p.releaseReference();
		return p;
	}
//	/**
//	 * No keep
//	 * @param GID
//	 * @param load_Globals
//	 * @param create
//	 * @param __peer
//	 * @return
//	 */
	/*
	static public D_Organization getOrgByGID_UpdatingNewGID(String GID, boolean load_Globals, boolean create, D_Peer __peer) {
		// boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: "+GID+" glob="+load_Globals+" cre="+create+" _peer="+__peer);
		if (GID == null) {
			if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: null GID");
			return null;
		}
		D_Organization crt = D_Organization.getOrgByGID_only_AttemptCacheOnly(GID, load_Globals);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: got GID cached crt="+crt);
			return crt;
		}
		String gIDH = D_Organization.getOrgGIDHashGuess(GID);

		crt = D_Organization.getOrgByGIDhash_AttemptCacheOnly(gIDH, load_Globals, false);
		
		if (crt != null) {
			crt.setGID(GID, gIDH);
			if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: got GIDH cached crt="+crt);
			return crt;
		}

		synchronized(monitor_object_factory) {
			crt = D_Organization.getOrgByGIDhash_AttemptCacheOnly(gIDH, load_Globals, false);
			if (crt != null) {
				crt.setGID(GID, gIDH);
				if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: got sync cached crt="+crt);
				return crt;
			}

			try {
				crt = new D_Organization(GID, gIDH, create, __peer);
				if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: loaded crt="+crt);
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: error loading");
				if (DEBUG) e.printStackTrace();
				return null;
			}
			D_Organization_Node.register_loaded(crt);
			if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: Done");
			return crt;
		}
	}
	*/
//	/**
//	 * 
//	 * @param GIDhash
//	 * @param load_Globals
//	 * @param keep
//	 * @param createTemp
//	 * @param __peer
//	 * @return
//	 */
//	static public D_Organization getOrgByGIDhash(String GIDhash, boolean load_Globals, boolean keep, boolean createTemp, D_Peer __peer) {
//		return D_Organization.getOrgByGID_or_GIDhash(null, GIDhash, load_Globals, createTemp, keep, __peer);
//	}
//		if (GIDhash == null) return null;
//		
//		D_Organization crt = getOrgByGIDhash_AttemptCacheOnly(GIDhash, load_Globals, keep);
//		if (crt != null) return crt;
//		
//		synchronized(monitor_object_factory) {
//			crt = getOrgByGIDhash_AttemptCacheOnly(GIDhash, load_Globals, keep);
//			if(crt != null) return crt;
//
//			try {
//				crt = new D_Organization(null, GIDhash, createTemp, null);
//				if (__peer != null) crt.first_provider_peer = __peer.getLIDstr_keep_force();
//				if (keep) {
//					crt.status_references ++;
//					System.out.println("D_Organization: getOrgByGIDhash_AttemptCacheOnly: "+crt.status_references);
//					Util.printCallPath("");
//				}
//			} catch (Exception e) {
//				//e.printStackTrace();
//				return null;
//			}
//			D_Organization_Node.register_loaded(crt);
//			return crt;
//		}
//	}
	/**
	 * Usable until calling releaseReference()
	 * Verify with assertReferenced()
	 * @param peer
	 * @return
	 */
	static public D_Organization getOrgByOrg_Keep(D_Organization org) {
		if (org == null) return null;
		D_Organization result = getOrgByGID_or_GIDhash_NoCreate(org.getGID(), org.getGIDH_or_guess(), true, true);
		if (result == null) {
			result = getOrgByLID(org.getLID_forced(), true, true);
		}
		if (result == null) {
			if ((org.getLIDstr_forced() == null) && (org.getGIDH_or_guess() == null)) {
				result = org;
				
				//synchronized (monitor_object_factory) 
				{
					org.incStatusReferences();
					System.out.println("D_Organization: getOrgByOrg_Keep: "+org.getStatusReferences());
					Util.printCallPath("");
				}
			}
		}
		if (result == null) {
			System.out.println("D_Organization: getOrgByOrg_Keep: got null for "+org);
			Util.printCallPath("");
		}
		return result;
	} 
//	/**
//	 * Used when we are now sure whether what we have is a GID or a GIDhash
//	 * exception raised on error
//	 * @param load_Globals (should GIDs in served orgs load full D_Orgs)
//	 * @param createTemp TODO
//	 * @param GID
//	 * @param GIDhash
//	 * @return
//	 */
//	static public D_Organization getOrgBy_or_hash(String GID_or_hash, boolean load_Globals, boolean createTemp){
//		if (GID_or_hash == null) return null;
//		String GID = null;
//		String GIDhash = D_Organization.getOrgGIDHashGuess(GID_or_hash);
//		if (!GIDhash.equals(GID_or_hash)) GID = GID_or_hash;
//
//		D_Organization crt = getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(GID, GIDhash, load_Globals);
//		if (crt != null) return crt;
//
//		synchronized (monitor_object_factory) {
//			crt = getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(GID, GIDhash, load_Globals);
//			if (crt != null) return crt;
//			
//			try {
//				crt = new D_Organization(GID, GIDhash, createTemp, null);
//			} catch (Exception e) {
//				e.printStackTrace();
//				return null;
//			}
//			D_Organization_Node.register_loaded(crt);
//			// if newly created, then they are dirty
//			if (crt.dirty_any()) crt.storeRequest();
//			return crt;
//		}
//	}
	
	private void loadGlobals() {
		if (this.loaded_globals) return;

		if ((this.params == null) || (this.creator_ID == null)) {
			this.loaded_globals = true;
 			return;
		}
		if ((this.creator_ID != null ) && (this.params.creator_global_ID == null))
			this.params.creator_global_ID =
				D_Peer.getGIDfromLID(this.creator_ID);
		
		this.loaded_globals = true;
	}

	private static final String sql_getOrgByGIDH =
			"SELECT "+table.organization.field_list+
			" FROM "+table.organization.TNAME+
			" WHERE "+table.organization.global_organization_ID_hash+"=?;";
	private void init_ByLID(String __organization_ID) throws P2PDDSQLException {
		ArrayList<ArrayList<Object>> p_data;

		p_data = Application.db.select(sql_getOrgByLID, new String[]{getLIDstr()}, DEBUG);
		if (p_data.size() >= 1) {
				ArrayList<Object> row = p_data.get(0);
				init(row);
		} else {
				throw new RuntimeException("Invalid org LID: "+__organization_ID);
		}
		if (DEBUG) System.out.println("D_Organization:<init(long)>:creatorID="+this.creator_ID+
					"; param.GID="+this.params.creator_global_ID+"; peer_creat="+this.creator);
	}
	/**
	 * sets dirty but does not store, if absent
	 * @param gID
	 * @param orgGIDhash
	 * @param create
	 * @param __peer
	 * @throws P2PDDSQLException
	 */
	public void init_ByGID(String gID, String orgGIDhash, boolean create, D_Peer __peer) throws P2PDDSQLException{
		boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Organization: init: gID: GIDhash: "+orgGIDhash+" gID="+gID);
		// sanitize inputs
		orgGIDhash = D_Organization.getOrgGIDHashGuess(orgGIDhash);
		gID = D_Organization.getOrgGIDGuess(gID);
		if (DEBUG) System.out.println("D_Organization: init: 2 gID: GIDhash: "+orgGIDhash+" gID="+gID);
		if (orgGIDhash == null && gID != null) {
			orgGIDhash = D_Peer.getGIDHashGuess(gID);
		}
		if (DEBUG) System.out.println("D_Organization: init: 2 gID: GIDhash: "+orgGIDhash+" gID="+gID);
		assert (orgGIDhash != null);
		
		ArrayList<ArrayList<Object>> p = Application.db.select(sql_getOrgByGIDH, new String[]{orgGIDhash}, DEBUG);
		if (p.size() == 0) {
			if (!create) throw new RuntimeException("Absent org hash "+orgGIDhash);
			this.setGID(gID, orgGIDhash);
			if (__peer != null) {
				assert(__peer.getLIDstr_force() != null);
				this.first_provider_peer = __peer.getLIDstr_force();
			}
			dirty_main = true;
			dirty_params = true;
			init_new();
		} else {
			init(p.get(0), true, true);
		}
	}
	
	/**
	 * Set all "loaded" markers to true.
	 * The only one actually used is loaded_served_orgs, but all seems to be discardable.
	 */
	private void init_new() {
		if (DEBUG) System.out.println("D_Peer: init_new");
		loaded_globals = true;
		loaded_locals = true;
//		params = new D_OrgParams();
//		concepts = new D_OrgConcepts();
	}
	
	/**
	 * Load creator and extra fields
	 * @param row
	 * @throws P2PDDSQLException
	 */
	private void init(ArrayList<Object> row) throws P2PDDSQLException {
		init(row, true, true);
		if(DEBUG)System.out.println("D_Organization:<init(AL)>:creatorID="+this.creator_ID+
				"; param.GID="+this.params.creator_global_ID+"; peer_creat="+this.creator);
	}
	/**
	 * 
	 * @param row
	 * @param extra_fields
	 * @param extra_creator
	 * @throws P2PDDSQLException
	 */
	private void init(ArrayList<Object> row, boolean extra_fields, boolean extra_creator) throws P2PDDSQLException {
		setLID(Util.getString(row.get(table.organization.ORG_COL_ID),null));
		this.arrival_date = Util.getCalendar(Util.getString(row.get(table.organization.ORG_COL_ARRIVAL)),null);
		this.global_organization_ID = Util.getString(row.get(table.organization.ORG_COL_GID),null);
		this.global_organization_IDhash = Util.getString(row.get(table.organization.ORG_COL_GID_HASH),null);
		this.name = Util.getString(row.get(table.organization.ORG_COL_NAME),null);
		this.params = new D_OrgParams();
		this.params.languages = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_LANG)));
		this.params.instructions_registration = Util.getString(row.get(table.organization.ORG_COL_INSTRUC_REGIS),null);
		this.params.instructions_new_motions = Util.getString(row.get(table.organization.ORG_COL_INSTRUC_MOTION),null);
		this.params.description = Util.getString(row.get(table.organization.ORG_COL_DESCRIPTION),null);
		this.params.default_scoring_options = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_SCORES),null));//new String[0];//null;
		this.params.category = Util.getString(row.get(table.organization.ORG_COL_CATEG),null);
		
		try {this.params.certifMethods = Integer.parseInt(Util.getString(row.get(table.organization.ORG_COL_CERTIF_METHODS)));} catch(NumberFormatException e) {this.params.certifMethods = 0;}
		this.params.hash_org_alg = Util.getString(row.get(table.organization.ORG_COL_HASH_ALG),null);
		this.params.creation_time = Util.getCalendar(Util.getString(row.get(table.organization.ORG_COL_CREATION_DATE)), Util.CalendargetInstance());
		this.reset_date = Util.getCalendar(Util.getString(row.get(table.organization.ORG_COL_RESET_DATE)), null);
		this.params.certificate = Util.byteSignatureFromString(Util.getString(row.get(table.organization.ORG_COL_CERTIF_DATA),null));
		//String 
		this.creator_ID = Util.getString(row.get(table.organization.ORG_COL_CREATOR_ID));
		this.blocked = Util.stringInt2bool(row.get(table.organization.ORG_COL_BLOCK),false);
		this.temporary = Util.stringInt2bool(row.get(table.organization.ORG_COL_TEMPORARY),false);
		this.hidden = Util.stringInt2bool(row.get(table.organization.ORG_COL_HIDDEN),false);
		this.requested = Util.stringInt2bool(row.get(table.organization.ORG_COL_REQUEST),false);
		this.broadcasted = Util.stringInt2bool(row.get(table.organization.ORG_COL_BROADCASTED), D_Organization.DEFAULT_BROADCASTED_ORG_ITSELF);
		this.broadcast_rule = Util.stringInt2bool(row.get(table.organization.ORG_COL_BROADCAST_RULE), true);

		_preapproved = Util.getString(row.get(table.organization.ORG_COL_PREAPPROVED));
		updatePreapproved();
		setPreferencesDate(null, Util.getString(row.get(table.organization.ORG_COL_PREFERENCES_DATE)));
		this.first_provider_peer = Util.getString(row.get(table.organization.ORG_COL_FIRST_PROVIDER_PEER),null);
		
		this.concepts = new D_OrgConcepts();
		this.concepts.name_organization = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_NAME_ORG),null));
		this.concepts.name_forum = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_NAME_FORUM),null));
		this.concepts.name_motion = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_NAME_MOTION),null));
		this.concepts.name_justification = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(table.organization.ORG_COL_NAME_JUST),null));
		this.signature = getSignatureFromString(Util.getString(row.get(table.organization.ORG_COL_SIGN),null));
		this.signature_initiator = getSignatureFromString(Util.getString(row.get(table.organization.ORG_COL_SIGN_INITIATOR),null));
		
		try {
			String d = Util.getString(row.get(table.organization.ORG_COL_SPECIFIC));
			if (d != null) {
				this.specific_request = new OrgPeerDataHashes(new Decoder(Util.byteSignatureFromString(d)));
			}
			if(DEBUG) System.out.println("OrgPeerDataHashes: "+this.specific_request);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		
		if (extra_fields) this.params.orgParam = D_Organization.getOrgParams(getLIDstr());
		if (DEBUG) System.out.println("D_Organization:init: extra_creator="+extra_creator+" ID="+creator_ID);
		creator = D_Peer.getPeerByLID_NoKeep(creator_ID, true); // for creator one also needs _served
		if (extra_creator) {
			if (creator == null) {
				if (DEBUG) System.out.println("D_Organization:init: no creator");
				//if(DEBUG)Util.printCallPath("No creator:"+creator_ID);
				//Application.warning(Util._("Missing organization creator, or may have incomplete data. You should create a new one!"), Util._("Missing organization creator."));
				//throw new Exception("No creator");
			} else {
				this.params.creator_global_ID = creator.component_basic_data.globalID;
				if (DEBUG) System.out.println("D_Organization:init: creator="+creator);
			}
		} else {
			if (creator != null) this.params.creator_global_ID = creator.getGID();
			creator = null;
			if (DEBUG) System.out.println("D_Organization:init: param_creator="+this.params.creator_global_ID);
		}

		if ((this.signature_initiator == null) && (this.params.creator_global_ID != null)) {

			//byte[] msg = this.getSignableEncoder().getBytes();
			//SK sk = Util.getStoredSK(this.params.creator_global_ID, null);
			//if (sk != null) {
			//	this.signature_initiator = Util.sign(msg, sk);
			if (signIni(false) != null) {
				Application.db.updateNoSync(table.organization.TNAME,
						new String[]{table.organization.signature_initiator},
						new String[]{table.organization.organization_ID},
						new String[]{Util.stringSignatureFromByte(this.signature_initiator), this.getLIDstr()}, _DEBUG);
				Application_GUI.warning(__("Updated org initiator signature for:")+this.name, __("Updated org Signature"));
			}
		}
		
		String my_org_sql = "SELECT "+table.my_organization_data.fields_list+
				" FROM "+table.my_organization_data.TNAME+
				" WHERE "+table.my_organization_data.organization_ID+" = ?;";
		ArrayList<ArrayList<Object>> my_org = Application.db.select(my_org_sql, new String[]{getLIDstr_forced()}, DEBUG);
		if (my_org.size() != 0) {
			ArrayList<Object> my_data = my_org.get(0);
			mydata.name = Util.getString(my_data.get(table.my_organization_data.COL_NAME));
			mydata.category = Util.getString(my_data.get(table.my_organization_data.COL_CATEGORY));
			mydata.creator = Util.getString(my_data.get(table.my_organization_data.COL_CREATOR));
			mydata.row = Util.lval(my_data.get(table.my_organization_data.COL_ROW));
		}
		
		initSK();
	}
	/**
	 * Init the hashset from the string _preapproved
	 */ 
	private void updatePreapproved() {
		if (_preapproved != null) {
			preapproved = getEmailsSet(_preapproved);
		} else
			preapproved = new HashSet<String>();
	}
	public static D_Organization updatePreapprovedRequest(D_Organization crt_org, String __preapproved,  Calendar _currentTime, String currentTime) {
		crt_org = D_Organization.getOrgByOrg_Keep(crt_org);
		crt_org._preapproved = __preapproved;
		crt_org.updatePreapproved();
		crt_org.setPreferencesDate(_currentTime, currentTime);
		crt_org.storeLocalFlags();
		crt_org.releaseReference();
		return crt_org;
	}
	/**
	 * Splits email list by any separator
	 *  (table.organization.SEP_PREAPPROVED ; : , space) and trims spaces.
	 * Intended for simple emails, without names
	 * @param emails
	 * @return
	 */
	public static HashSet<String> getEmailsSet(String emails) {
		HashSet<String> emailsSet = new HashSet<String>();
		String[] _p;
		_p = Util.trimmed(emails.split(Pattern.quote(table.organization.SEP_PREAPPROVED)));
		if (_p.length == 1)
			_p = Util.trimmed(emails.split(Pattern.quote(";")));
		if (_p.length == 1)
			_p = Util.trimmed(emails.split(Pattern.quote(":")));
		if (_p.length == 1)
			_p = Util.trimmed(emails.split(Pattern.quote(",")));
		if (_p.length == 1)
			_p = Util.trimmed(emails.split(Pattern.quote(" ")));
		for (String p: _p) {
			emailsSet.add(p);
		}
		return emailsSet;
	}
	/**
	 * Assumes the signature starts with "G:" (D_GIDH.d_OrgSign)
	 * @param s
	 * @return
	 */
	private static byte[] getSignatureFromString(String s) {
		if (s == null) return null;
		int pref_len = D_GIDH.d_OrgGrassSign.length(); // 2
		if (s.length() < pref_len) return null;
		if (s.startsWith(D_GIDH.d_OrgGrassSign)) return Util.byteSignatureFromString(s.substring(pref_len));
		return Util.byteSignatureFromString(s);
	}
	
	/**
	 * Without id, (which is the result of hash/signing, and without constituents etc
	 * @return
	 */
	private Encoder getSignableEncoder() {
		if (ASNSyncRequest.DEBUG || DEBUG) System.out.println("Encoding OrgData sign: "+this);
		if (DD.ANONYMOUS_ORG_ENFORCED_AT_HANDLING && this.getCreatorGID() == null) {
			Util.printCallPath("No creator for this org!");
			//return null;
		}
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version,false));
		//enc.addToSequence(new Encoder(id,false));
		if (name != null) enc.addToSequence(new Encoder(name));
		//if (last_sync_date != null) enc.addToSequence(new Encoder(last_sync_date));
		if (params != null) enc.addToSequence(params.getEncoder().setASN1Type(DD.TAG_AC1));
		if (concepts != null) enc.addToSequence(concepts.getEncoder().setASN1Type(DD.TAG_AC2));
		if (broadcast_rule != true) enc.addToSequence(new Encoder(broadcast_rule).setASN1Type(DD.TAG_AC15));
		if (ASNSyncRequest.DEBUG || DEBUG) System.out.println("Encoded OrgData sign: "+this);
		return enc;
	}
	/**
	 * Used to compare between two authoritarian orgs with identical GID and creation date
	 * @return
	 */
	private Encoder getEntityEncoder() {
		if (ASNSyncRequest.DEBUG) System.out.println("Encoding OrgData: "+this);
		Encoder enc = new Encoder().initSequence();
		//enc.addToSequence(new Encoder(version,false));
		enc.addToSequence(new Encoder(global_organization_ID,false));
		if (name != null) enc.addToSequence(new Encoder(name));
		//if (last_sync_date != null) enc.addToSequence(new Encoder(last_sync_date));
		if (params != null) enc.addToSequence(params.getEncoder().setASN1Type(DD.TAG_AC1));
		if (concepts != null) enc.addToSequence(concepts.getEncoder().setASN1Type(DD.TAG_AC2));
		if (broadcast_rule != true) enc.addToSequence(new Encoder(broadcast_rule).setASN1Type(DD.TAG_AC15));
		//if (signature != null) enc.addToSequence(new Encoder(signature));
		//if (creator != null) enc.addToSequence(creator.getEncoder().setASN1Type(DD.TAG_AC0));
		//if (neighborhoods != null) enc.addToSequence(Encoder.getEncoder(neighborhoods).setASN1Type(DD.TAG_AC3));
		//if (constituents != null) enc.addToSequence(Encoder.getEncoder(constituents).setASN1Type(DD.TAG_AC4));
		//if (witnesses != null) enc.addToSequence(Encoder.getEncoder(witnesses).setASN1Type(DD.TAG_AC5));
		//if (motions != null) enc.addToSequence(Encoder.getEncoder(motions).setASN1Type(DD.TAG_AC6));
		//if (justifications != null) enc.addToSequence(Encoder.getEncoder(justifications).setASN1Type(DD.TAG_AC7));		
		//if (signatures != null) enc.addToSequence(Encoder.getEncoder(signatures).setASN1Type(DD.TAG_AC8));
		//if (translations != null) enc.addToSequence(Encoder.getEncoder(translations).setASN1Type(DD.TAG_AC9));
		//if (news != null) enc.addToSequence(Encoder.getEncoder(news).setASN1Type(DD.TAG_AC10));
		//if (requested_data != null) enc.addToSequence(Encoder.getEncoder(requested_data).setASN1Type(DD.TAG_AC11));
		enc.setASN1Type(DD.TYPE_ORG_DATA);
		if (ASNSyncRequest.DEBUG) System.out.println("Encoded OrgData: "+this);
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
	/**
	 * TODO: Have to decide later if neighborhoods and submitter should be send even when descendants is none...
	 * TODO: Also have to decide if to encode descendants using the provided dependants (or inferred new_dependents)
	 * Currently only passing new_dependants to the creator
	 */
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs, int dependants) {
		int new_dependants = dependants;
		if (dependants > 0) new_dependants = dependants - 1;
		
		if (ASNSyncRequest.DEBUG) System.out.println("Encoding OrgData: "+this);
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version,false));
		String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, global_organization_ID);
		enc.addToSequence(new Encoder(repl_GID,false));
		if (name != null) enc.addToSequence(new Encoder(name));
		if (last_sync_date != null) enc.addToSequence(new Encoder(last_sync_date));
		if (params != null) enc.addToSequence(params.getEncoder(dictionary_GIDs).setASN1Type(DD.TAG_AC1));
		if (concepts != null) enc.addToSequence(concepts.getEncoder().setASN1Type(DD.TAG_AC2));
		if (broadcast_rule != true) enc.addToSequence(new Encoder(broadcast_rule).setASN1Type(DD.TAG_AC15));
		if (signature != null) enc.addToSequence(new Encoder(signature));
		if (signature_initiator != null) enc.addToSequence(new Encoder(signature_initiator).setASN1Type(DD.TAG_AC14));
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (ASNSyncPayload.STREAMING_SEND_CREATOR_IN_PEER)
				if (creator != null) enc.addToSequence(creator.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC0));
		}
		
		if (neighborhoods != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in neighborhoods: "+neighborhoods);
			enc.addToSequence(Encoder.getEncoder(neighborhoods, dictionary_GIDs).setASN1Type(DD.TAG_AC3));
		}
		if (constituents != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in constituents: "+constituents);
			enc.addToSequence(Encoder.getEncoder(constituents, dictionary_GIDs).setASN1Type(DD.TAG_AC4));
		}
		if (witnesses != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in witnesses: "+witnesses);
			enc.addToSequence(Encoder.getEncoder(witnesses, dictionary_GIDs).setASN1Type(DD.TAG_AC5));
		}
		if (motions != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in motions: "+motions);
			enc.addToSequence(Encoder.getEncoder(motions, dictionary_GIDs).setASN1Type(DD.TAG_AC6));
		}
		if (justifications != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in justifications: "+justifications);
			enc.addToSequence(Encoder.getEncoder(justifications, dictionary_GIDs).setASN1Type(DD.TAG_AC7));		
		}
		if (signatures != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in signatures: "+signatures);
			enc.addToSequence(Encoder.getEncoder(signatures).setASN1Type(DD.TAG_AC8));
		}
		if (translations != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in translations: "+translations);
			enc.addToSequence(Encoder.getEncoder(translations, dictionary_GIDs).setASN1Type(DD.TAG_AC9));
		}
		if (news != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in news: "+news);
			enc.addToSequence(Encoder.getEncoder(news, dictionary_GIDs).setASN1Type(DD.TAG_AC10));
		}
	
		if (requested_data != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in requested_data: "+requested_data);
			enc.addToSequence(Encoder.getEncoder(requested_data, dictionary_GIDs).setASN1Type(DD.TAG_AC11));
		}
		// these are aggregated in ASNSyncPayload in "advertised"
		//if (this.availableHashes != null) enc.addToSequence(availableHashes.getEncoder().setASN1Type(DD.TAG_AC12));
		
		enc.setASN1Type(DD.TYPE_ORG_DATA);
		if (ASNSyncRequest.DEBUG) System.out.println("Encoded OrgData: "+this);
		return enc;
	}

	@Override
	public D_Organization decode(Decoder decoder) throws ASN1DecoderFail {
		if(ASNSyncRequest.DEBUG)System.out.println("DEncoding OrgData: "+this);
		if((decoder==null)||(decoder.getTypeByte()==Encoder.TAG_NULL)) return null;
		Decoder dec = decoder.getContent();
		version = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString(Encoder.TAG_PrintableString);
		global_organization_ID = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString(Encoder.TAG_PrintableString);
		
		if (DEBUG) System.out.println("OrgData id="+global_organization_ID);
		if (dec.getTypeByte() == Encoder.TAG_UTF8String) {
			name = dec.getFirstObject(true, Encoder.TAG_UTF8String).getString(Encoder.TAG_UTF8String);
			if (DEBUG) System.out.println("OrgData name="+name);
		}
		
		if(dec.getTypeByte()==Encoder.TAG_GeneralizedTime){ setLastSyncDate(dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType()); if(DEBUG )System.out.println("OrgData d="+last_sync_date);}
		if(dec.getTypeByte()==DD.TAG_AC1){ params=new D_OrgParams().decode(dec.getFirstObject(true)); if(DEBUG )System.out.println("OrgData p="+params);}
		if(dec.getTypeByte()==DD.TAG_AC2){ concepts=new D_OrgConcepts().decode(dec.getFirstObject(true)); if(DEBUG)System.out.println("OrgData c="+concepts);}
		if(dec.getTypeByte()==DD.TAG_AC15){ broadcast_rule = dec.getFirstObject(true).getBoolean(DD.TAG_AC15); if(DEBUG)System.out.println("OrgData b_r="+broadcast_rule);}
		if(dec.getTypeByte()==Encoder.TAG_OCTET_STRING){ signature=dec.getFirstObject(true).getBytes(); if(DEBUG)System.out.println("OrgData s="+Util.byteToHexDump(signature));}
		if(dec.getTypeByte()==DD.TAG_AC14){ signature_initiator=dec.getFirstObject(true).getBytes(DD.TAG_AC14); if(DEBUG)System.out.println("OrgData s="+Util.byteToHexDump(signature_initiator));}
		if(dec.getTypeByte()==DD.TAG_AC0){ creator = D_Peer.getEmpty().decode(dec.getFirstObject(true)); if(DEBUG)System.out.println("OrgData cr="+creator);}
		if(dec.getTypeByte()==DD.TAG_AC3){ neighborhoods = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new ASNNeighborhoodOP[]{}, new ASNNeighborhoodOP()); if(DEBUG)System.out.println("OrgData n="+neighborhoods);}
		if(dec.getTypeByte()==DD.TAG_AC4){ constituents = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new ASNConstituentOP[]{}, new ASNConstituentOP()); if(DEBUG)System.out.println("OrgData co="+constituents);}
		if(dec.getTypeByte()==DD.TAG_AC5){ witnesses = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Witness[]{}, new D_Witness()); if(DEBUG)System.out.println("OrgData w="+witnesses);}
		if(dec.getTypeByte()==DD.TAG_AC6){ motions = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Motion[]{}, D_Motion.getEmpty()); if(DEBUG)System.out.println("OrgData m="+motions);}
		if(dec.getTypeByte()==DD.TAG_AC7){ justifications = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Justification[]{}, D_Justification.getEmpty()); if(DEBUG)System.out.println("OrgData j="+justifications);}
		if(dec.getTypeByte()==DD.TAG_AC8){ signatures = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Vote[]{}, new D_Vote()); if(DEBUG)System.out.println("OrgData s="+signatures);}
		if(dec.getTypeByte()==DD.TAG_AC9){ translations = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Translations[]{}, new D_Translations()); if(DEBUG)System.out.println("OrgData t="+translations);}
		if(dec.getTypeByte()==DD.TAG_AC10){ news = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_News[]{}, new D_News()); if(DEBUG)System.out.println("OrgData nw="+news);}
		if(dec.getTypeByte()==DD.TAG_AC11){ requested_data = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new data.D_Message[]{}, new data.D_Message()); if(DEBUG)System.out.println("OrgData rd="+requested_data);}
		//if(dec.getTypeByte()==DD.TAG_AC12){ availableHashes = new RequestData().decode(dec.getFirstObject(true)); if(DEBUG)System.out.println("OrgData available="+availableHashes);}
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects in decoder: "+dec.dumpHex()+"\n"+decoder.dumpHex());
		if(ASNSyncRequest.DEBUG)System.out.println("DEncoded OrgData: "+this);
		return this;
	}
	
	public String getCreationDate_str() {
		return Encoder.getGeneralizedTime(this.params.creation_time);
	}
	private void setCreationDate(Calendar time) {
		this.params.creation_time = time;
		this.dirty_main = true;
	}
	public void setCreationDate(String currentTime) {
		this.params.creation_time = Util.getCalendar(currentTime);
		this.dirty_main = true;
	}
	public void setCreationDate() {
		this.params.creation_time = Util.CalendargetInstance();
		this.dirty_main = true;
	}
	
	private String getArrivalDate_str() {
		return Encoder.getGeneralizedTime(this.arrival_date);
	}
	public void setArrivalDate(String currentTime, Calendar _currentTime) {
		this.arrival_date = _currentTime;
		if (_currentTime == null)
			this.arrival_date = Util.getCalendar(currentTime);
		this.dirty_locals = true;
	}
	
	/**
	 * Sets both the Calendar and String version.
	 * @param p_last_sync_date
	 */
	public void setLastSyncDate(Calendar p_last_sync_date){
		last_sync_date = p_last_sync_date;
		_last_sync_date = Encoder.getGeneralizedTime(p_last_sync_date);
	}
	/**
	 * sets the last_sync_date
	 * @param lsd
	 */
	public void setLastSyncDate(String lsd) {
		_last_sync_date = lsd;
		last_sync_date = Util.getCalendar(lsd);
	}	
	/**
	 * 
	 * @param c
	 * @param _c
	 */
	public void setPreferencesDate(Calendar c, String _c) {
		if ((c == null) && (_c == null)) return;
		preferences_date = ((c == null) ? Util.getCalendar(_c) : c);
		_preferences_date = ((_c == null) ? Encoder.getGeneralizedTime(c) : _c);
	}
	public void setRequested(boolean val) {
		if (DEBUG) System.out.println("Orgs:setRequested: set="+val);
		this.requested = val;
		this.dirty_locals = true;
	}
	public void setBlocking(boolean val) {
		if(DEBUG) System.out.println("Orgs:setBlocking: set="+val);
		this.blocked = val;
		this.dirty_locals = true;
	}
	public void setBroadcasting(boolean val) {
		if(DEBUG) System.out.println("Orgs:setBlocking: set="+val);
		this.broadcasted = val;
		this.reset_date = Util.CalendargetInstance();
		this.dirty_locals = true;
	}
	/**
	 * This function just sets the "broadcasted" flag (calling sync on the database).
	 * To change "org.broadcasted" (if also advertising it!), better change with toggleServing
	 *  which sets also "peer.served_orgs" (by calling this).
	 * @param orgID
	 * @param val
	 */
	public static void setBroadcasting(String orgID, boolean val) {
		if (DEBUG) System.out.println("Orgs:setBroadcasting: set="+val+" for orgID="+orgID);
		D_Organization org = D_Organization.getOrgByLID(orgID, true, true);
		
		org.broadcasted = val;
		org.reset_date = Util.CalendargetInstance();
		org.dirty_main = true;
		org.dirty_locals = true;
		org.storeRequest();
		org.releaseReference();
		
		if (DEBUG) System.out.println("Orgs:setBroadcasting: Done");
	}

	public static boolean is_crt_org(D_Organization candidate) {
		//D_Peer myself = data.HandlingMyself_Peer.get_myself();
		//if (myself == candidate) return true;
		return Application_GUI.is_crt_org(candidate);
	}
	/**
	 * Load this message in the storage cache node.
	 * Should be called each time I load a node and when I sign myself.
	 */
	private void reloadMessage() {
		if (this.loaded_globals) this.component_node.message = this.encode(); //crt.encoder.getBytes();
	}
	/**
	 * Returns the GIDH. If null but GID nonull, computes it
	 * @return
	 */
	public String getGIDH_or_guess() {
		if ((global_organization_IDhash == null) && (global_organization_ID != null))
			global_organization_IDhash = D_Organization.getOrgGIDHashGuess(global_organization_ID);
		return this.global_organization_IDhash;
	}
	/**
	 * Returns the GIDH variables as is
	 * @return
	 */
	public String getGIDH() {
		return this.global_organization_IDhash;
	}
	/**
	 * Returns GID as is
	 * @return
	 */
	public String getGID() {
		return this.global_organization_ID;
	}
	
	/**
	 * Remove the "G:" prefix (D_GIDH.d_OrgSign)
	 * @param id
	 * @return
	 */
	public static byte[] getHashFromGrassrootGID(String gID) {
		if ((gID == null) || (gID.length() < D_GIDH.d_OrgGrassSign.length())) return null;
		return Util.byteSignatureFromString(gID.substring(D_GIDH.d_OrgGrassSign.length()));
	}
	/**
	 * Returns 
	 * 
	 * "O:"(D_GIDH.d_OrgAuthSign)+DD.APP_ID_HASH+DD.APP_ID_HASH_SEP+base64(hash(decode64(GID)))
	 * @param GID
	 * @return
	 */
	public static String getOrgGIDHashAuthoritarian(String GID) {
		if (GID == null) return null;
		String hash = Util.getGIDhash(GID);
		if (hash == null) return null;
		return D_GIDH.d_OrgAuthSign + hash;
	}
	/**
	 * Gets GIDhash from internal GIFfunction on type
	 * @return
	 */
	public String getOrgGIDHashFromGID() {
		String result = null;
		if (params == null) return null;
		if (params.certifMethods == table.organization._GRASSROOT) {
			result = global_organization_ID;
		} else if (params.certifMethods == table.organization._AUTHORITARIAN) {
			result = getOrgGIDHashAuthoritarian(global_organization_ID);
		} else result = null;
		return  result;
	}
	/**
	 * decides based on prefix (O: GIDH, G: grassroot GID/GIDH, else GID->GIDH) 
	 * old version: try to compute hash. if not shorter, then this is already the hash
	 * @param GID
	 * @return
	 */
	public static String getOrgGIDHashGuess(String GID) {
		if (GID == null) return null;
		if (GID.startsWith(D_GIDH.d_OrgAuthSign)) return GID; // already hash
		if (GID.startsWith(D_GIDH.d_OrgGrassSign)) return GID; // grass root
		String GID_hash = getOrgGIDHashAuthoritarian(GID);
		//if(GID_hash.length() == GID.length()) return GID;
		return GID_hash;
	}
	/**
	 * returns GID if it is not hash of authoritarian
	 * @param GID
	 * @return
	 */
	public static String getOrgGIDGuess(String GID) {
		if (GID == null) return null;
		if (GID.startsWith(D_GIDH.d_OrgAuthSign)) return null; // already hash
		if (GID.startsWith(D_GIDH.d_OrgGrassSign)) return GID; // grass root
		return GID;
	}
	public static boolean isOrgGID(String GID) {
		if (GID == null) return false;
		if (GID.startsWith(D_GIDH.d_OrgAuthSign)) return false; // already hash
		if (GID.startsWith(D_GIDH.d_OrgGrassSign)) return true; // grass root
		return true;
	}
	public static boolean isOrgGIDH(String GID) {
		if (GID == null) return false;
		if (GID.startsWith(D_GIDH.d_OrgAuthSign)) return true; // already hash
		if (GID.startsWith(D_GIDH.d_OrgGrassSign)) return true; // grass root
		return false;
	}
	public void setGID (String gID, String gIDH) {
		//boolean DEBUG = true;
		boolean loaded_in_cache = this.isLoadedInCache();
		if (DEBUG) System.out.println("D_Organization:setGID: start loaded="+loaded_in_cache);
		String oldGID = this.getGID();
		if ( ! Util.equalStrings_null_or_not(oldGID, gID)) {		
			if (DEBUG) System.out.println("D_Organization:setGID: change GID");
			if (oldGID != null)
				loaded_in_cache |= (null!=D_Organization_Node.loaded_org_By_GID.remove(oldGID));
			this.global_organization_ID = gID;
			this.dirty_main = true;
		}
		if ((gID != null) && (gIDH == null)) {
			if (DEBUG) System.out.println("D_Organization:setGID: compute GIDH");
			if (!D_GIDH.isCompactedGID(gID)) {
				gIDH = D_Organization.getOrgGIDHashGuess(gID);
			} else
				gIDH = gID;
			if (gIDH == null) Util.printCallPath("D_Organization: setGID:"+gID+" for: "+this);
		}

		if ( ! Util.equalStrings_null_or_not(global_organization_IDhash, gIDH)) {		
			if (DEBUG) System.out.println("D_Organization:setGID: change GIDH");
			if (this.global_organization_IDhash != null)
				loaded_in_cache |= (null!=D_Organization_Node.loaded_org_By_GIDhash.remove(this.getGIDH_or_guess()));
			this.global_organization_IDhash = gIDH;
			this.dirty_main = true;
		}
		
		if (loaded_in_cache) {
			if (DEBUG) System.out.println("D_Organization:setGID: register");
			if (this.getGID() != null) {
				D_Organization_Node.loaded_org_By_GID.put(this.getGID(), this);
				if (DEBUG) System.out.println("D_Organization:setGID: register GID="+this.getGID());
			}
			if (this.getGIDH_or_guess() != null) {
				D_Organization_Node.loaded_org_By_GIDhash.put(this.getGIDH_or_guess(), this);
				if (DEBUG) System.out.println("D_Organization:setGID: register GIDH="+this.getGIDH_or_guess());
			}
		}
		if (DEBUG) System.out.println("D_Organization:setGID: quit loaded="+loaded_in_cache);
	}
	
	
	public boolean isLoadedInCache() {
		String GIDH;
		if (!D_Organization_Node.loaded_orgs.inListProbably(this)) return false;
		long lID = this.getLID_forced();
		if (lID > 0)
			if ( null != D_Organization_Node.loaded_org_By_LocalID.get(new Long(lID)) ) return true;
		if ((GIDH = this.getGIDH_or_guess()) != null)
			if ( null != D_Organization_Node.loaded_org_By_GIDhash.get(GIDH) ) return true;
		return false;
	}

	/** Storing */
	public static D_Organization_SaverThread saverThread = new D_Organization_SaverThread();
	public boolean dirty_any() {
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
		if (!this.dirty_any()) {
			Util.printCallPath("Why store when not dirty?");
			return;
		}
		
		String save_key = this.getGIDH_or_guess();
		
		if (save_key == null) {
			//save_key = ((Object)this).hashCode() + "";
			//Util.printCallPath("Cannot store null:\n"+this);
			//return;
			D_Organization._need_saving_obj.add(this);
			if (DEBUG) System.out.println("D_Peer:storeRequest: added to _need_saving_obj");
		} else {		
			if (DEBUG) System.out.println("D_Organization:storeRequest: GIDH="+save_key);
			D_Organization._need_saving.add(save_key);
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
		//if ( this.getLIDstr() == null ) return this.storeSynchronouslyNoException();
		this.storeRequest();
		return this.getLID_forced();
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
	 * use:  this.arrival_date = Util.getCalendar(arrival_time);
	 * sync not sent
	 * 
	 * @throws P2PDDSQLException
	 */
	public long storeAct() throws P2PDDSQLException {
		// final boolean DEBUG = true;
		if (DEBUG) out.println("D_Organization: storeAct: start "+this.global_organization_IDhash);
		//D_Peer source_peer = null;
		synchronized(D_Organization.lock_organization_GID_storage ) {
			if (DEBUG) out.println("D_Organization: storeAct: synced "+this.global_organization_IDhash);
			long result = -1;
			/*
			//this.arrival_date = Util.getCalendar(arrival_time);
			//arrival_time = Encoder.getGeneralizedTime(arrival_date);
					
			if (this.global_organization_IDhash == null) global_organization_IDhash = getOrgGIDHashFromGID();
			
			String creation_time = Encoder.getGeneralizedTime(params.creation_time);
			String old_date[] = new String[1];
			String organization_ID = D_Organization_D.getLocalOrgIDandDate(global_organization_ID, old_date);
			if ((organization_ID != null) && (old_date != null) && (old_date[0] != null) && (creation_time.compareTo(old_date[0]) <= 0)) {
				if (! Util.equalStrings_null_or_not(creation_time, old_date[0]) || hashConflictCreationDateDropThis()) {
					if (DEBUG) out.println("D_Organization: storeVerified: Will not integrate old ["+creation_time+"]: "+this);
					if (changed != null) changed[0] = false;
					return Util.lval(organization_ID,-1);
				}
			}
			if (changed != null) changed[0] = true;
			*/
			fillLocals();
			
			if (this.dirty_params || dirty_locals || dirty_main) {
				int filter = 0;
				this.dirty_params = dirty_locals = dirty_main = false;
				
				if (DEBUG) out.println("D_Organization: storeAct: dirty main & locals & params");

				String field_sign = (signature != null) ? Util.stringSignatureFromByte(signature) : null;
				
				if ((field_sign != null) && (params.certifMethods == table.organization._GRASSROOT)) {
					field_sign = D_GIDH.d_OrgGrassSign+field_sign;
				}
							
				//String[] fieldNames = table.organization.fields; // Util.trimmed(table.organization.org_list.split(","));
				//if (organization_ID != null) filter = 1;
				if (getLIDstr() != null) filter = 1;
				
				String[] p = new String[table.organization.FIELDS_NOID + filter];
				p[table.organization.ORG_COL_GID] = global_organization_ID;
				p[table.organization.ORG_COL_GID_HASH] = global_organization_IDhash;
				p[table.organization.ORG_COL_NAME] = name;
				p[table.organization.ORG_COL_NAME_ORG] = D_OrgConcepts.stringFromStringArray(concepts.name_organization);
				p[table.organization.ORG_COL_NAME_FORUM] = D_OrgConcepts.stringFromStringArray(concepts.name_forum);
				p[table.organization.ORG_COL_NAME_MOTION] = D_OrgConcepts.stringFromStringArray(concepts.name_motion);
				p[table.organization.ORG_COL_NAME_JUST] = D_OrgConcepts.stringFromStringArray(concepts.name_justification);//, table.organization.ORG_TRANS_SEP,null);
				p[table.organization.ORG_COL_LANG] = D_OrgConcepts.stringFromStringArray(params.languages);//, table.organization.ORG_LANG_SEP,null);
				p[table.organization.ORG_COL_INSTRUC_REGIS] = params.instructions_registration;
				p[table.organization.ORG_COL_INSTRUC_MOTION] = params.instructions_new_motions;
				p[table.organization.ORG_COL_DESCRIPTION] = params.description;
				p[table.organization.ORG_COL_SCORES] = D_OrgConcepts.stringFromStringArray(params.default_scoring_options);//, table.organization.ORG_SCORE_SEP,null);
				p[table.organization.ORG_COL_CATEG] = params.category;
				p[table.organization.ORG_COL_CERTIF_METHODS] = ""+params.certifMethods;
				p[table.organization.ORG_COL_HASH_ALG] = params.hash_org_alg;
				//p[table.organization.ORG_COL_HASH] = (params.hash_org!=null)?Util.byteToHex(params.hash_org, table.organization.ORG_HASH_BYTE_SEP):null;
				p[table.organization.ORG_COL_CREATION_DATE] = getCreationDate_str();
				p[table.organization.ORG_COL_CERTIF_DATA] = (params.certificate!=null)?Util.stringSignatureFromByte(params.certificate):null;
				p[table.organization.ORG_COL_CREATOR_ID] = creator_ID;
				p[table.organization.ORG_COL_SIGN] = field_sign;
				p[table.organization.ORG_COL_SIGN_INITIATOR] = Util.stringSignatureFromByte(this.signature_initiator);
				p[table.organization.ORG_COL_ARRIVAL] = getArrivalDate_str(); //Util.getGeneralizedTime(); s
				p[table.organization.ORG_COL_RESET_DATE] = (reset_date!=null)?Encoder.getGeneralizedTime(reset_date):null; //Util.getGeneralizedTime();
				p[table.organization.ORG_COL_BLOCK] = Util.bool2StringInt(blocked);
				p[table.organization.ORG_COL_REQUEST] = Util.bool2StringInt(requested);
				p[table.organization.ORG_COL_BROADCASTED] = Util.bool2StringInt(broadcasted);
				p[table.organization.ORG_COL_BROADCAST_RULE] = Util.bool2StringInt(broadcast_rule);
				p[table.organization.ORG_COL_PREAPPROVED] = _preapproved;
				p[table.organization.ORG_COL_PREFERENCES_DATE] = this._preferences_date;
				p[table.organization.ORG_COL_FIRST_PROVIDER_PEER] = this.first_provider_peer;
				p[table.organization.ORG_COL_TEMPORARY] = Util.bool2StringInt(this.temporary);
				p[table.organization.ORG_COL_HIDDEN] = Util.bool2StringInt(this.hidden);
				if (this.specific_request != null) p[table.organization.ORG_COL_SPECIFIC] = Util.stringSignatureFromByte(this.specific_request.encode());
				
				//String orgID;
				if (filter == 0) {//organization_ID == null) {
					setLID(result = Application.db.insertNoSync(table.organization.TNAME, table.organization.fields_noID, p, DEBUG));//changed = true;
					if (DEBUG) out.println("D_Organization: storeVerified: Inserted: "+this);
				} else {
					//orgID = Util.getString(p_data.get(0).get(table.organization.ORG_COL_ID));
					p[table.organization.ORG_COL_ID] = getLIDstr();
					//if (filter > 0) p[table.organization.FIELDS] = organization_ID;
					Application.db.updateNoSync(table.organization.TNAME,  table.organization.fields_noID, new String[]{table.organization.organization_ID}, p, DEBUG);//changed = true;
					result = Util.lval(getLIDstr(), -1);
					if(DEBUG) out.println("\nD_Organization: storeVerified: Updated: "+this);
				}
				//if (sync) 
				Application.db.sync(new ArrayList<String>(Arrays.asList(table.organization.TNAME)));
				//organization_ID = orgID;
				if (params != null) D_Organization.storeOrgParams(getLIDstr(), params.orgParam, filter == 0);
				this.setLID(result);
			}
			
			if (this.dirty_mydata) {
				if (DEBUG) out.println("D_Organization: storeAct: dirty my data");
				this.dirty_mydata = false;
				String p[];
				if (mydata.row > 0) {
					p = new String[table.my_organization_data.FIELDS_NB];
				} else {
					p = new String[table.my_organization_data.FIELDS_NB_NOID];
				}
				p[table.my_organization_data.COL_ORGANIZATION_LID] = this.getLIDstr_forced();
				p[table.my_organization_data.COL_CATEGORY] = mydata.category;
				p[table.my_organization_data.COL_CREATOR] = mydata.creator;
				p[table.my_organization_data.COL_NAME] = mydata.name;
				if (mydata.row <= 0) {
					mydata.row = Application.db.insertNoSync(table.my_organization_data.TNAME, table.my_organization_data.fields_noID, p, DEBUG);//changed = true;
					if (DEBUG) out.println("D_Organization: storeVerified: Inserted: "+mydata);
				} else {
					p[table.my_organization_data.COL_ROW] = mydata.row + "";
					Application.db.updateNoSync(table.my_organization_data.TNAME, table.my_organization_data.fields_noID, new String[]{table.my_organization_data.row}, p, DEBUG);//changed = true;
					if(DEBUG) out.println("\nD_Organization: storeVerified: Updated: "+mydata);
				}
			}
			
			return 
					result;
		}
	}
	private void fillLocals() {
		if ((creator_ID == null) && (this.params.creator_global_ID != null)) {
			////creator_ID = D_Peer.storePeerAndGetOrInsertTemporaryLocalForPeerGID(null, params.creator_global_ID,creator,arrival_time);
			//D_Peer _creator = D_Peer.getPeerByGID(this.params.creator_global_ID, true, true, source_peer);
			//if (creator != null) _creator.loadRemote(creator);
			if (creator == null) creator = D_Peer.getPeerByGID_or_GIDhash(this.params.creator_global_ID, null, true, true, false, null);
			creator_ID = creator.getLIDstr_keep_force();
			System.out.println("D_Organization: fillLocals: should not be here: unknown source peer");
		}
	}
	/**
	 * Gets the components of field_extra, ordered by GID (needed because encoder is used in comparing orgs with same date)
	 * including temporary
	 * @param local_id
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_OrgParam[]  getOrgParams(String local_id) throws P2PDDSQLException {
		if (DEBUG) out.println("\n****************\nOrgHandling:getOrgParams: start organization orgID="+local_id);
		D_OrgParam[] result=null;
		String psql = "SELECT "+table.field_extra.org_field_extra +
				" FROM "+table.field_extra.TNAME+
				" WHERE "+table.field_extra.organization_ID+" = ?" +
						// " AND ("+table.field_extra.tmp+" IS NULL OR "+table.field_extra.tmp+"='0' )" +
				" ORDER BY "+table.field_extra.field_extra_ID+";";
		ArrayList<ArrayList<Object>>p_org = Application.db.select(psql, new String[]{local_id}, DEBUG);
		if (p_org.size() > 0) {
			result = new D_OrgParam[p_org.size()];
			for (int p_i = 0; p_i < p_org.size(); p_i ++) {
				D_OrgParam op = D_OrgParam.getOrgParam(p_org.get(p_i));
				result[p_i] = op;
				if (DEBUG) System.out.println("OrgHandling:signOrgData: encoded orgParam="+op);
			}
		}
		if (DEBUG) out.println("OrgHandling:getOrgParams: exit\n****************\n");
		return result;
	}
	/**
	 * 
	 * @param orgID local organization ID
	 * @param orgParam An OrgParam[] array
	 * @param just_created To avoid redundant search for IDs when known to not be there
	 * @throws P2PDDSQLException
	 */
	public static void storeOrgParams(String orgID, D_OrgParam[] orgParam, boolean just_created) throws P2PDDSQLException{
		// Cannot be deleted and rewritten since we would lose references to the IDs from const values
		Application.db.updateNoSync(table.field_extra.TNAME,
				new String[]{table.field_extra.tmp},
				new String[]{table.organization.organization_ID},
				new String[]{"1",orgID}, DEBUG);
		
		long _orgID = Util.lval(orgID, -1);
		
		if (orgParam != null) {
			String[] fieldNames_extra = table.field_extra.org_field_extra_insert_list;
			//String[] fieldNames_extra_update = Util.trimmed(table.field_extra.org_field_extra_insert.split(","));
			for (int k = 0; k < orgParam.length; k ++) {
				D_OrgParam op = orgParam[k];
				if (DEBUG) out.println("OrgHandling:updateOrg: Inserting/Updating field: "+op);
				String fe;
				if (op.field_LID <= 0 && ! just_created)
					fe = D_Organization.getFieldExtraID_FromTable(op.global_field_extra_ID, _orgID);
				else fe = Util.getStringID(op.field_LID);
				
				String[] p_extra;
				if (fe == null) p_extra = new String[fieldNames_extra.length];
				else p_extra = new String[fieldNames_extra.length+1];
				p_extra[table.field_extra.OPARAM_LABEL] = op.label;
				p_extra[table.field_extra.OPARAM_LABEL_L] = op.label_lang;
				p_extra[table.field_extra.OPARAM_LATER] = Util.bool2StringInt(op.can_be_provided_later);
				p_extra[table.field_extra.OPARAM_CERT] = Util.bool2StringInt(op.certificated);
				p_extra[table.field_extra.OPARAM_SIZE] = op.entry_size+"";
				p_extra[table.field_extra.OPARAM_NEIGH] = op.partNeigh+"";
				p_extra[table.field_extra.OPARAM_REQ] = Util.bool2StringInt(op.required);
				p_extra[table.field_extra.OPARAM_DEFAULT] = op.default_value;
				p_extra[table.field_extra.OPARAM_DEFAULT_L] = op.default_value_lang;
				p_extra[table.field_extra.OPARAM_LIST_VAL] = Util.concat(op.list_of_values, table.organization.ORG_VAL_SEP, null);
				p_extra[table.field_extra.OPARAM_LIST_VAL_L] = op.list_of_values_lang;
				p_extra[table.field_extra.OPARAM_TIP] = op.tip;
				p_extra[table.field_extra.OPARAM_TIP_L] = op.tip_lang;
				p_extra[table.field_extra.OPARAM_OID] = Util.BNOID2String(op.oid);
				p_extra[table.field_extra.OPARAM_ORG_ID] = orgID;
				p_extra[table.field_extra.OPARAM_GID] = op.global_field_extra_ID;
				p_extra[table.field_extra.OPARAM_VERSION] = op.version;
				p_extra[table.field_extra.OPARAM_TMP] = op.tmp;
				if (fe == null) {
					Application.db.insertNoSync(table.field_extra.TNAME, fieldNames_extra, p_extra, DEBUG);
				} else {
					p_extra[table.field_extra.OPARAM_EXTRA_FIELD_ID] = fe;
					Application.db.updateNoSync(table.field_extra.TNAME, fieldNames_extra,
							new String[]{table.field_extra.field_extra_ID},
							p_extra, DEBUG);
				}
			}
		}
		Application.db.deleteNoSync(table.field_extra.TNAME,
				new String[]{table.field_extra.tmp, table.organization.organization_ID},
				new String[]{"1",orgID}, DEBUG);
	}
	
	public static String getGIDbyLIDstr(String p_organization_ID) {
		long lID = Util.lval(p_organization_ID);
		D_Organization org = D_Organization.getOrgByLID_NoKeep(lID, true);
		if (org == null) return null;
		return org.getGID();
	}
	public static String getGIDbyLID(Long p_organization_ID) {
		if (DEBUG) System.out.println("D_Organization: getGIDbyLID: lID="+p_organization_ID);
		D_Organization org = D_Organization.getOrgByLID_NoKeep(p_organization_ID, true);
		if (org == null) {
			if (_DEBUG) System.out.println("D_Organization: getGIDbyLID: no org for LID="+p_organization_ID);
			return null;
		}
		String result = org.getGID();
		if (result == null)
			if (_DEBUG) System.out.println("D_Organization: getGIDbyLID: no orgGID for LID="+p_organization_ID+"\n in "+org);
		return result;
	}
	public static String getGIDHbyLID(Long p_organization_ID) {
		if (_DEBUG) System.out.println("D_Organization: getGIDbyLID: lID="+p_organization_ID);
		D_Organization org = D_Organization.getOrgByLID_NoKeep(p_organization_ID, true);
		if (org == null) {
			if (_DEBUG) System.out.println("D_Organization: getGIDbyLID: no org for LID="+p_organization_ID);
			return null;
		}
		String result = org.getGIDH_or_guess();
		if (result == null)
			if (_DEBUG) System.out.println("D_Organization: getGIDbyLID: no orgGID for LID="+p_organization_ID+"\n in "+org);
		return result;
	}
	/**
	 * returns -1 id not existent
	 * @param p_global_organization_IDH
	 * @return
	 */
	public static long getLocalOrgIDByGIDH(String p_global_organization_IDH) {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(null, p_global_organization_IDH, true, false);
		if (org == null) return -1;
		return org.getLID_forced();
	}
	/**
	 * returns -1 if inexistent
	 * @param p_global_organization_ID
	 * @return
	 */
	public static long getLIDbyGID(String p_global_organization_ID) {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(p_global_organization_ID, null, true, false);
		if (org == null) return -1;
		return org.getLID_forced();
	}
	/**
	 * returns -1 if inexistent
	 * @param p_global_organization_ID
	 * @return
	 */
	public static String getLocalOrgID_(String p_global_organization_ID) {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(p_global_organization_ID, null, true, false);
		if (org == null) return null;
		return org.getLIDstr_forced();
	}
	/**
	 * Synonym for getLocalOrgID
	 * @param orgGID
	 * @param orgGIDH
	 * @return
	 */
	public static long getLIDbyGIDorGIDH(String orgGID, String orgGIDH) {
		return getLocalOrgID (orgGID, orgGIDH);
	}
	/**
	 * Synonym for getLIDbyGIDorGIDH
	 * @param p_global_organization_ID
	 * @param orgGID_hash
	 * @return
	 */
	public static long getLocalOrgID(String p_global_organization_ID, String orgGID_hash) {
		if ((p_global_organization_ID != null) && (orgGID_hash == null)) {
			orgGID_hash = D_Organization.getOrgGIDHashGuess(p_global_organization_ID);
			//return getLocalOrgID(p_global_organization_ID);
		}
		if (orgGID_hash != null)
			return getLocalOrgIDByGIDH(orgGID_hash);
		return -1;
	}
	public static void setTemporary(D_Organization org) {
		org = D_Organization.getOrgByOrg_Keep(org);
		org.setTemporary();
		org.storeRequest();
		org.releaseReference();
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
		return this.temporary || this.signature == null || this.signature.length == 0 || this.getGID() == null;
	}
	public void setHidden() {
		this.hidden = true;
		this.dirty_locals = true;
	}
	public boolean getHidden() {
		return this.hidden;
	}
	public static long insertTemporaryGID(String p_global_organization_ID,
			String orgGID_hash, boolean default_blocked_org, D_Peer __peer) {
		D_Organization org;
		org = D_Organization.getOrgByGID_or_GIDhash_StoreIfNew_NoKeep(p_global_organization_ID, orgGID_hash, true, true, __peer);
		return org.getLID_forced(); 
	}
	public static D_Organization insertTemporaryGID_org(String p_global_organization_ID, D_Peer __peer) {
		D_Organization org;
		org = D_Organization.getOrgByGID_or_GIDhash_StoreIfNew_NoKeep(p_global_organization_ID, null, true, true, __peer);
		return org; 
	}
	public static String insertTemporaryGID(String p_global_organization_ID, D_Peer __peer) {
		D_Organization org;
		org = D_Organization.getOrgByGID_or_GIDhash_StoreIfNew_NoKeep(p_global_organization_ID, null, true, true, __peer);
		return org.getLIDstr_forced(); 
	}
	public static long insertTemporaryGID_long(String p_global_organization_ID, D_Peer __peer) {
		D_Organization org;
		org = D_Organization.getOrgByGID_or_GIDhash_StoreIfNew_NoKeep(p_global_organization_ID, null, true, true, __peer);
		return org.getLID_forced(); 
	}
	public static long insertTemporaryGID(String p_global_organization_ID,
			String orgGID_hash, boolean blocked2, String name2, D_Peer __peer) {
		D_Organization org = D_Organization.insertTemporaryGID_org(p_global_organization_ID, orgGID_hash, __peer, blocked2, name2);
		return org.getLID_forced();
	}
	/**
	 * 
	 * @param p_global_organization_ID
	 * @param orgGID_hash
	 * @param __peer
	 * @param blocked2
	 * @param name2
	 * @return
	 */
	public static D_Organization insertTemporaryGID_org(
			String p_global_organization_ID, String orgGID_hash, D_Peer __peer,
			boolean blocked2, String name2) {
		D_Organization org;
		//org = D_Organization.getOrgByGID_or_GIDhash_StoreIfNew_NoKeep(p_global_organization_ID, orgGID_hash, true, true, __peer);
		org = D_Organization.getOrgByGID_or_GIDhash(p_global_organization_ID, orgGID_hash, true, true, true, __peer);
		if ((name2 != null) || (blocked2 != org.blocked)) {
			//org = D_Organization.getOrgByOrg_Keep(org);
			org.setName(name2);
			org.setBlocked(blocked2);
		}
		if (org.dirty_any()) org.storeRequest();
		org.releaseReference();
		return org;
	}
	/**
	 * Used to check if an incoming org is already known or needs to be adopted.
	 * @param gIDhash
	 * @param DBG
	 * @return
	 *  0 for absent,
	 *  1 for present&signed,
	 *  -1 for temporary
	 * @throws P2PDDSQLException
	 */
	public static int isOrgAvailableForGIDH(String gIDhash, boolean DBG) throws P2PDDSQLException {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(null, gIDhash, true, false);
		if (org == null) return 0;
		if (org.isTemporary()) return -1;
		return 1;
	}
	public static int isOrgAvailableForLID(long _organizationID, boolean DBG) {
		D_Organization org = D_Organization.getOrgByLID_NoKeep(_organizationID, true);
		if (org == null) {
			if(DEBUG||DBG) System.out.println("D_Org:isIDavailable: 0");
			return 0;
		}
		if (org.isTemporary()) {
			if(DEBUG||DBG) System.out.println("D_Org:isIDavailable: -1");
			return -1;
		}
		if (DEBUG || DBG) System.out.println("D_Org:isIDavailable: 1:"+org.signature);
		return 1;
	}
	public static D_MotionChoice[] getDefaultMotionChoices(String[] opts) {
		D_MotionChoice[] result;
		if(opts == null) return null;
		result = new D_MotionChoice[opts.length];
		for(int k=0; k<result.length; k++) {
			result[k]=new D_MotionChoice(opts[k], ""+k);
		}
		return result;		
	}
	public D_MotionChoice[] getDefaultMotionChoices() {
		D_MotionChoice[] result;
		String[] opts = this.params.default_scoring_options;
		if(opts==null) opts = get_DEFAULT_OPTIONS();
		result = getDefaultMotionChoices(opts);
		return result;
	}
	public static String[] get_DEFAULT_OPTIONS() {
		String[] DEFAULT_OPTIONS = new String[]{__("Endorse"),__("Abstain"),__("Oppose")};
		return DEFAULT_OPTIONS;
	}
	
	public static byte getASN1Type() {
		return TAG;
	}
	public static D_Organization getOrgFromDecoder(Decoder decoder) {
		D_Organization org;
		try {
			org = new D_Organization().decode(decoder);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return null;
		}
		return org;
	}
	/**
	 * Do I know the organization with this GIDH
	 * I should probably also add the creation date in the test (and RequestData)
	 * @param orgGIDH
	 * @return
	 */
	public static boolean hasGIDH(String orgGIDH) {
		D_Organization o = D_Organization.getOrgByGID_or_GIDhash_NoCreate(null, orgGIDH, true, false);
		return o != null; 
	}
	public boolean verifyExtraFieldGIDs() {
		return verifyExtraFieldGIDs(this);
	}
	public static boolean verifyExtraFieldGIDs(D_Organization od) {
		if ((od.params==null) || (od.params.orgParam==null)) return true;
		for (int i = 0; i < od.params.orgParam.length; i ++) {
			D_OrgParam dop = od.params.orgParam[i];
			String eGID = dop.makeGID();
			if (! eGID.equals(dop.global_field_extra_ID)) {
				if (_DEBUG) System.out.println("D_Organization: verifyExtraFieldGIDs: fail to verify field ID: "+dop.label+" for "+dop);
				return false;
			}
		}
		return true;
	}
	/*
	public static String _getOrgGIDandHashForGrassRoot(String org_ID, String currentTime, D_Organization[] org) {
		D_Organization orgData = D_Organization.getOrgByLID(org_ID, true);
		if (orgData == null) return null;
		byte[] h = orgData.getOrgGIDandHashForGrassRoot(currentTime);
		if (h == null) {
			Util.printCallPath("Failure to compute Hash, incomplete data");
			return null;
		}
		String _gid = Util.stringSignatureFromByte(h);
		return D_GIDH.d_OrgGrassSign+_gid;
	}
	public byte[] getOrgGIDandHashForGrassRoot(String currentTime) {
		setCreationData(currentTime);
		if(DEBUG) out.println("OrgHandling:getOrgHash: Prepared org: "+this);
		if(DEBUG) out.println("*****************");
		
		byte[]h = this.hash(DD.APP_ORGID_HASH);
		SK sk_ini = Util.getStoredSK(this.params.creator_global_ID, null);
		if (sk_ini != null) this.signature_initiator = Util.sign(h, sk_ini);
		
		return h;
	}
	*/
	/**
	 * makeGID
	 * @return
	 */
	public String getOrgGIDandHashForGrassRoot() {
		return getOrgGIDandHashForGrassRoot(null);
	}
	public String getOrgGIDandHashForGrassRoot(boolean[] verif){
		byte[] hash = this.hash(DD.APP_ORGID_HASH);
		if (verif != null)
			verif[0] =
			! DD.ANONYMOUS_ORG_ENFORCED_AT_HANDLING
			|| Util.verifySignByID(hash, this.getCreatorGID(), this.signature_initiator);
		return D_GIDH.d_OrgGrassSign+Util.stringSignatureFromByte(hash);
	}
	public byte[]hash(String _h) {
		return hash(_h, null);
	}
	public byte[]hash(String _h, ciphersuits.SK sk_ini) {
		Encoder enc = getSignableEncoder();
		byte[] msg = enc.getBytes();
		if(sk_ini !=null)this.signature_initiator = Util.sign(msg, sk_ini);
		return Util.simple_hash(msg, _h);	
	}
	/**
	 * Signs and sets the current creation date
	 * @param id
	 * @return
	 */
	public static D_Organization readSignSave(long id) {
		D_Organization org = D_Organization.getOrgByLID(id, true, true);
		org.updateExtraGIDs();
		org.setCreationDate(Util.CalendargetInstance());
		org.setSignature(org.sign());
		
		org.storeRequest();
		org.releaseReference();
		return org;
	}
	/**
	 * 
	 * @param id
	 * @param creationTime
	 * @return
	 */
	public static D_Organization readSignSave(String id, String creationTime) {
		D_Organization org = D_Organization.getOrgByLID(id, true, true);
		org.updateExtraGIDs();
		
		if (creationTime == null) org.setCreationDate(Util.CalendargetInstance());
		else org.setCreationDate(creationTime);
		
		org.setSignature(org.sign());
		
		org.storeRequest();
		org.releaseReference();
		return org;
	}
	/**
	 * This assigns the initiator signature before returning.
	 * @return
	 */
	public byte[] signIni() {
		return signIni(true);
	}
	/**
	 * Optimized for signing bost with a creator and an organization key
	 * @return
	 */
	public byte[]sign() {
		SK sk = getSK(); //Util.getStoredSK(global_organization_ID, global_organization_IDhash);
		SK sk_ini = getCreatorSK();
		return sign(sk, sk_ini);
	}
	/**
	 * sk of the organization
	 * @param sk
	 * @return
	 */
	/*
	public byte[]sign(ciphersuits.SK sk) {
		return sign(sk, null);
	}
	*/
	/**
	 * Will compute the signature using the provided secret key
	 * @param sk
	 * @return
	 * @throws P2PDDSQLException
	 *
	 */
	/*
	public byte[] signAndGetOrgSignature(ciphersuits.SK sk) {
		if(DEBUG) out.println("OrgHandling:getOrgSignature: Prepared org: "+this);
		if(DEBUG) out.println("*****************");
		SK sk_ini = this.getCreatorSK(); //Util.getStoredSK(this.params.creator_global_ID, null);
		//orgData.signature_initiator = Util.sign(h, sk_ini);
		return this.sign(sk, sk_ini);
	}
	*/
	/**
	 * sign data with Org (for authoritarian), and with initiator key 
	 * Stores signature_initiator, but does not store the signature with organization key.
	 * @param sk (must be non-null) else call sign_ini()
	 * @param sk_ini
	 * @return
	 */
	public byte[]sign(ciphersuits.SK sk, ciphersuits.SK sk_ini){
		return sign(sk, sk_ini, true);
	}
	public byte[]sign(ciphersuits.SK sk, ciphersuits.SK sk_ini, boolean set_dirty){
		Encoder enc = getSignableEncoder();
		byte[] msg = enc.getBytes();
		if(DEBUG) System.out.println("D_Organization:sign:");
		if(DEBUG) System.out.println("msg=#"+msg.length+" hash(msg)="+Util.getGID_as_Hash(msg));
		if(DEBUG) System.out.println("sk_ini=#"+sk_ini);
		if (sk_ini != null){
			this.signature_initiator = Util.sign(msg, sk_ini);
			if (set_dirty) this.dirty_main = true;
			if(DEBUG) System.out.println("sgn_ini=#"+signature_initiator.length+" hash(sgn_ini)="+Util.getGID_as_Hash(signature_initiator));
		}else{
			this.signature_initiator = null;			
		}
		if(DEBUG) System.out.println("sk=#"+sk);
		if (sk != null) {
			byte[] sgn = Util.sign(msg, sk);	
			if (set_dirty) this.dirty_main = true;
			if(DEBUG) System.out.println("sgn=#"+sgn.length+" hash(sgn)="+Util.getGID_as_Hash(sgn));
			return sgn;
		} else
			return null;
	}
	/**
	 * 				organization.setSignatureInitiator(organization.getSignatureInitiator());
	 * Optimized for signing only initiator (not caring about authoritarian org signature)
	 * @param set_dirty
	 * @return
	 */
	private byte[] signIni(boolean set_dirty) {
		if (this.params.creator_global_ID == null) return null;
		// reusing, but does not controll dirty 
		sign(null, getCreatorSK(), false);
		return getSignatureInitiator();
		
		/*
		SK sk_ini = getCreatorSK();
		if (sk_ini != null) {
			byte[] msg = this.getSignableEncoder().getBytes();
			this.signature_initiator = Util.sign(msg, sk_ini);
			if (set_dirty) this.dirty_main = true;
		}
		return this.signature_initiator;
		*/
	}
	/**
	 * Sets the signature (which is a GIDH for a grassroot)
	 * organization.hash(DD.APP_ORGID_HASH)
	 * @param sgn
	 */
	public void setSignature(byte[] sgn) {
		this.signature = sgn;
		this.dirty_main = true;
	}
	public byte[] getSignatureInitiator() {
		if (this.signature_initiator == null) signIni();
		return this.signature_initiator;
	}
	public void setSignatureInitiator(byte[] signatureInitiator) {
		this.signature_initiator = signatureInitiator;
		this.dirty_main = true;
	}
	public boolean verifySignInitiator() {
		return verifySignInitiator(null);
	}
	/**
	 * If the message encoded is already available
	 * @param msg
	 * @return
	 */
	public boolean verifySignInitiator(byte[] msg) {
		if (msg == null) {
			this.loadGlobals();
			Encoder enc = getSignableEncoder();
			msg = enc.getBytes();
		}
		PK creator_pk = Cipher.getPK(this.params.creator_global_ID);
		if((creator_pk!=null) && !Util.verifySign(msg, creator_pk, this.signature_initiator)){
			if(DEBUG) System.out.println("D_Organization:Failed Creator verification:");
			if(DEBUG) System.out.println("msg=#"+msg.length+" hash(msg)="+Util.getGID_as_Hash(msg));
			if(DEBUG) System.out.println("sgn=#"+((signature_initiator==null)?"null":signature_initiator)+" hash(sgn)="+Util.getGID_as_Hash(signature_initiator));
			if(DEBUG) System.out.println("pk=#"+creator_pk);
			return false;
		}else{
			if(DEBUG) System.out.println("D_Organization:Success Creator verification:");
			if(DEBUG) System.out.println("msg=#"+msg.length+" hash(msg)="+Util.getGID_as_Hash(msg));
			if(DEBUG) System.out.println("sgn=#"+((signature_initiator==null)?"null":signature_initiator)+" hash(sgn)="+Util.getGID_as_Hash(signature_initiator));
			if(DEBUG) System.out.println("pk=#"+creator_pk);
		}
		return true;
	}
	public boolean isAnonymous() {
		return this.signature_initiator == null;
	}
	/**
	 * Checks both the org sign and (if creator claimed) creator sign
	 * @param sign
	 * @return
	 */
	public boolean verifySignAuthoritarian() {
		if(DEBUG) System.out.println("OrgData:verifySign: KEY=="+global_organization_ID);
		if(DEBUG) System.out.println("OrgData:verifySign: sign="+Util.byteToHex(signature, ":"));
		this.loadGlobals();
		Encoder enc = getSignableEncoder();
		byte[] msg = enc.getBytes();
		if(DEBUG) System.out.println("OrgData:verifySign: msg="+Util.byteToHex(msg, ":"));
		
		if ((! this.isAnonymous()) && !this.verifySignInitiator(msg)) return false;
		
		PK org_pk = Cipher.getPK(global_organization_ID);
		boolean result = Util.verifySign(msg, org_pk, signature);
		if (! result) {
			if(DEBUG) System.out.println("D_Organization:Failed Org verification:");
			if(DEBUG) System.out.println("msg=#"+msg.length+" hash(msg)="+Util.getGID_as_Hash(msg));
			//if(DEBUG) System.out.println("sgn=#"+signature_initiator.length+" hash(sgn)="+Util.getGID_as_Hash(signature_initiator));
			//if(DEBUG) System.out.println("pk=#"+creator_pk);
			return false;
		}else{
			if(DEBUG) System.out.println("D_Organization:Success Org verification:");
			if(DEBUG) System.out.println("msg=#"+msg.length+" hash(msg)="+Util.getGID_as_Hash(msg));
			//if(DEBUG) System.out.println("sgn=#"+signature_initiator.length+" hash(sgn)="+Util.getGID_as_Hash(signature_initiator));
			//if(DEBUG) System.out.println("pk=#"+creator_pk);
		}
		return result;
	}
	/**
	 * @return
	 */
	public boolean verifySignature() {
		boolean verified = true;
		//verify signature!
		if (params == null) {
			if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes: null param for="+this);
			return false;
		}
		if (params.certifMethods == table.organization._GRASSROOT) {
			if(!this.verifyExtraFieldGIDs()){
				if(DEBUG) out.println("D_Organization:verifySignature: grassroot extras failed");
				verified = false;
			}else{
				//byte[] hash = hash(DD.APP_ORGID_HASH);
				boolean[]verif = new boolean[]{false};
				String tmpGID = this.getOrgGIDandHashForGrassRoot(verif);
				//if(!Util.equalBytes(hash, getHashFromGrassrootGID(global_organization_ID))) {
				if(!verif[0]||(tmpGID==null)||(global_organization_ID==null)||(tmpGID.compareTo(global_organization_ID)!=0)) {
					verified = false;
					if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes: recomp hash="+tmpGID);
					if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes:      IDhash="+global_organization_ID);
					if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes: exit grassroot signature verification failed");
				}
			}
			if (verified && (! isAnonymous()) && !this.verifySignInitiator()) {
				verified = false;
				if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes: exit grassroot signature ini verification failed");
			}
			if (! verified) {
				return false;
			}
		} else if(params.certifMethods == table.organization._AUTHORITARIAN){
			if ((signature == null) || (signature.length == 0) || !verifySignAuthoritarian()){
				if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes exit authoritarian signature verification failed");
				return false;
			}
		} else {
			if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes exit unknown type signature verification failed");
			return false;
		}
		return true;
	}

	public PK getPK() {
		if (this.getCertifyingMethod() != table.organization._AUTHORITARIAN) return null;
		//if (stored_pk != null) return stored_pk;
		//SK sk = getSK();
		if (keys != null) return keys.getPK();
		PK pk = ciphersuits.Cipher.getPK(getGID());
		//stored_pk = pk;
		keys = Cipher.getCipher(pk);
		return pk;
	}
	/**
	 * Gets the SK from cache or DB.
	 * Assumes cache loaded (should be loaded when importing keys)!
	 * @return
	 */
	public SK getSK() {
		if (this.getCertifyingMethod() != table.organization._AUTHORITARIAN) return null;
		SK sk = null;
		PK pk = null;
		String gID = this.getGID();
		if (this.getGID() == null) {
			this.loadGlobals();
			gID = this.getGID();
		}
		if (gID == null) return null;
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
		
		sk = Util.getStoredSK(gID, this.getGIDH_or_guess());
		if (sk == null) return null;
		keys = Cipher.getCipher(sk, pk);
		//if (keys == null) return sk;
		return sk;
	}
	public void initSK() {
		if (this.getCertifyingMethod() != table.organization._AUTHORITARIAN) return;
		String gID = this.getGID();
		SK sk = Util.getStoredSK(gID, this.getGIDH_or_guess());
		PK pk = null;
		if (sk == null) return;
		keys = Cipher.getCipher(sk, pk);
	}
	public void setKeys(Cipher keys2) {
		this.keys = keys2;
	}
	public SK getCreatorSK() {
		SK sk_ini = null;
		if (DEBUG) System.out.println("D_Organization:sign:*******************");
		if (DEBUG) System.out.println("D_Organization:sign:creatorID="+creator_ID+
				" GID="+params.creator_global_ID+" peer="+creator);
		if (creator != null) {
			sk_ini = creator.getSK(); //Util.getStoredSK(creator.component_basic_data.globalID, creator.component_basic_data.globalIDhash);
			if (sk_ini == null)
				Util.printCallPath("Why!!");
			if ( ! Util.equalStrings_null_or_not(creator.component_basic_data.globalID, params.creator_global_ID))
				System.out.println("D_Organization:readSignSave: diff GIDs:!!!");
		} else
			if (params.creator_global_ID != null) {
				this.creator = D_Peer.getPeerByGID_or_GIDhash(this.params.creator_global_ID, null, true, false, false, null);
				if (this.creator == null) return null;
				//SK sk;
				sk_ini = this.creator.getSK();
				if (sk_ini == null)
					sk_ini = Util.getStoredSK(this.params.creator_global_ID, null);
				//sk_ini = Util.getStoredSK(params.creator_global_ID, null);
				if (sk_ini == null)
					Util.printCallPath("Why!!");				
			}
		return sk_ini;
	}
	
	public boolean fillLocals(D_Peer __peer, RequestData _new_rq, boolean tempPeer, String arrival_time) {
		if(DD.ANONYMOUS_ORG_ENFORCED_AT_HANDLING &&
				((this.params==null)
						||((this.params.creator_global_ID==null)
								&&(this.creator_ID ==  null)))) {
			if(DEBUG)Util.printCallPath("cannot store org with no peerGID (while enforcing INITIATOR)");
			return false;
		}
		
		if((this.params.creator_global_ID!=null)&&(this.creator_ID == null)) {
			creator_ID = D_Peer.getLocalPeerIDforGID(this.params.creator_global_ID);
			
			if(tempPeer && (creator_ID == null))  {
				//creator_ID = D_Peer.storePeerAndGetOrInsertTemporaryLocalForPeerGID(__peer, this.params.creator_global_ID, creator, arrival_time);
				
				//__peer.setArrivalDate(null, arrival_time);
				__peer = D_Peer.storeReceived(__peer, !DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME, false, arrival_time);
				if (__peer == null) return false;
				
				D_Peer _creator = D_Peer.getPeerByGID_or_GIDhash(this.params.creator_global_ID, null, true, true, false, __peer);
				if (creator != null) _creator.loadRemote(creator, null, _new_rq);
				creator_ID = _creator.getLIDstr_keep_force();

				String creaGID_hash = D_Peer.getGIDHashFromGID(this.params.creator_global_ID);
				if (_new_rq != null) _new_rq.peers.put(creaGID_hash, DD.EMPTYDATE);
				//creator_ID = Util.getStringID(D_PeerAddress.insertTemporaryGID(this.params.creator_global_ID, consGID_hash));
			}
			if(DD.ANONYMOUS_ORG_ENFORCED_AT_HANDLING && (creator_ID == null)) return false;
		}
		
		return true;
	}
	/**
	 * Called on a new arriving org with same creation date as old, to decide whether it is newer based on hash.
	 * Store the one with smaller hash.
	 * @param old_org
	 * @return
	 */
	private boolean hashConflictCreationDateDropThis(D_Organization old_org) {
		D_Peer o_creator = this.creator;
		this.creator = null;
		String this_hash=new String(this.getEntityEncoder().getBytes());
		creator = o_creator;
//		D_Organization dorg;
//		try {
//			dorg = D_Organization.getOrgByGID_or_GIDhash_NoCreate(this.global_organization_ID, this.global_organization_IDhash, true, false); // no creator
//		} catch (Exception e) {
//			e.printStackTrace();
//			return false; // old not valid!
//		}
		String old = new String(old_org.getEntityEncoder().getBytes());
		if (old.compareTo(this_hash) >= 0) return true;
		return false;
	}
	public static long storeRemote(D_Organization oRG, boolean[] _changed,
			RequestData __sol_rq, RequestData __new_rq, D_Peer provider) {
		if (_DEBUG) out.println("D_Organization: storeRemote: start");
/*		
		if (oRG.getLID() == 0) { oRG.setLID(null);}
		boolean locals = oRG.fillLocals(provider, __new_rq, true, Util.getGeneralizedTime());
		if ( ! locals )//return -1;
			if (_DEBUG) out.println("D_Organization: store: locals failed");
*/
		if ( //( ! locals ) || ( oRG.params == null ) || 
				! oRG.verifySignature() ) {
			if ( ( oRG.signature != null ) || (DD.ANONYMOUS_ORG_ENFORCED_AT_HANDLING && (oRG.signature_initiator != null)))
				if (_DEBUG) out.println("D_Organization: storeRemote: exit signature verification failed for:" + oRG);
			if (_changed != null) _changed[0] = false;
			//long local_org_ID = D_Organization.getLocalOrgID(global_organization_ID);
			//return local_org_ID;
			return  D_Organization.getLIDbyGID(oRG.getGID());// -1;//oRG.getLID();
		}
		
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash(oRG.getGID(), oRG.getGIDH_or_guess(), true, true, true, provider);
		if (org.loadRemote(oRG, provider, _changed, __sol_rq, __new_rq)) {
			Application_GUI.inform_arrival(org, provider);
			org.storeRequest();
		}
		org.releaseReference();
		return org.getLID_forced();
	}
	public static D_Organization storeRemote(D_Organization oRG, D_Peer provider) {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash(oRG.getGID(), oRG.getGIDH_or_guess(), true, true, true, provider);
		if (org.loadRemote(oRG, provider, null, null, null)) {
			Application_GUI.inform_arrival(org, provider);
			org.storeRequest();
		}
		org.releaseReference();
		return org;
	}
	private boolean loadRemote(D_Organization oRG, D_Peer provider, boolean[] changed, RequestData _sol_rq, RequestData _new_rq) {
		if (_DEBUG) out.println("D_Organization: storeVerified: Will not integrate old: "+this+" vs incoming: "+oRG);
		
		if (! this.isTemporary() && ! newer(oRG, this)) {
			if (DEBUG) out.println("D_Organization: storeVerified: Will not integrate old ["+oRG.getCreationDate_str()+"]: "+this);
			if (changed != null) changed[0]=false;
			this.getLID_forced();
			return false;
		}

		if (changed != null) changed[0] = true;

		this.broadcast_rule = oRG.broadcast_rule;
		this.name = oRG.name;
		this.signature = oRG.signature;
		this.params = oRG.params;
		this.concepts = oRG.concepts;
		
		if (!Util.equalStrings_null_or_not(this.global_organization_ID, oRG.global_organization_ID)) {
			this.global_organization_ID = oRG.global_organization_ID;
			this.setLID(null);
		}
		if (!Util.equalStrings_null_or_not(this.params.creator_global_ID, oRG.params.creator_global_ID)) {
			//this.creator_ID = oRG.creator_ID;
			String GIDhash = null;//D_Peer.getGIDHashFromGID(this.params.creator_global_ID); 
			D_Peer p = D_Peer.getPeerByGID_or_GIDhash(this.params.creator_global_ID, GIDhash, true, false, false, null);
			if (p != null) 	this.setCreatorID(p.getLIDstr(), oRG.getCreatorGID());
			else {
				String creaGID_hash = oRG.getCreatorGIDH_orGuess();
				if (_new_rq != null) _new_rq.peers.put(creaGID_hash, DD.EMPTYDATE);

				p = D_Peer.getPeerByGID_or_GIDhash(oRG.params.creator_global_ID, null, true, true, false, provider);
				if (provider != null) p.setProvider(provider.getLIDstr_keep_force());
				p.storeRequest_getID();
				this.setCreatorID(p.getLIDstr(), oRG.getCreatorGID());
			}
		}
		
		dirty_main = true;
		
		if (this.isTemporary() && this.first_provider_peer == null && provider != null) this.first_provider_peer = provider.getLIDstr_keep_force();
		setTemporary(false);
		getLID_forced();
		return true;
	}
	private String getCreatorGIDH_orGuess() {
		return D_Peer.getGIDHashFromGID(this.getCreatorGID());
	}

	/**
	 * called for 
	 * @param a : incoming
	 * @param b : old
	 * @return true if a should be stored (a nonull creation and > b,  or == b but a's hash smaller)
	 */
	private static boolean newer(D_Organization a, D_Organization b) {
		if (a.getCreationDate_str() == null) return false;
		if (b.getCreationDate_str() == null) return true;
		int date_diff = a.getCreationDate_str().compareTo(b.getCreationDate_str());
		if (date_diff > 0) return true;
		if (date_diff < 0) return false;
		return !a.hashConflictCreationDateDropThis(b);
	}

	public boolean readyToSend() {
		if(this.global_organization_ID==null) return false;
		if(this.params.certifMethods == table.organization._AUTHORITARIAN)
			if((this.signature == null)||(this.signature.length==0)) return false;
		return true;
	}
	/**
	 * Fill locals (lID/blocked/broadcasted/requested/temporary) of this based on its GID
	 */
	public void getLocalIDfromGIDandBlock() {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(this.global_organization_ID, this.global_organization_IDhash, true, false);
		if (org == null) {
			this.blocked = DD.BLOCK_NEW_ARRIVING_ORGS;
			return;
		}
		this.setLID(org.getLIDstr(), org.getLID());
		this.blocked = org.blocked;
		this.broadcasted = org.broadcasted;
		this.requested = org.requested;
		this.temporary = org.temporary;
	}
	/**
	 * called if both parameters are valid
	 * @param liDstr
	 * @param lid
	 */
	public void setLID(String liDstr, long lid) {
		this.organization_ID = liDstr;
		this._organization_ID = lid;
	}

	public static String getLocalOrgID_fromHashIfNotBlocked(String gIDH) {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(null, gIDH, true, false);
		if (org == null) return null;
		if (org.blocked) return null;
		return org.getLIDstr_forced();
	}
	/**
	 * TODO: Have to implement this
	 * @return
	 */
	public boolean hasWeights() {
		return true;
	}
	/**
	 * TODO: Have to implement this
	 * Returns null for no limit
	 * @return
	 */
	public Comparable getMaxVotingWeight() {
		// 
		//return null;
		return 1;
	}
	/**
	 * Dirty: broadcasted, blocked, requested, preapproved, preferences_date
	 */
	public void storeLocalFlags() {
		this.dirty_locals = true;
		this.storeRequest();
	}
	/**
	 * Is this editable(not ready)?
	 * @param method   grassroot?
	 * @param GID		global ID
	 * @param cID		creator ID
	 * @return
	 */
	public static boolean isEditable(int method, String GID, String cID) {
		if ((method == table.organization._GRASSROOT) && (GID != null))
			return false;
		
		// AUTHORITARIAN
		
		// AURHORITARIAN TEMPORARY (NO GID)
		if(/*(method==table.organization._GRASSROOT) &&*/ (GID == null))
			return true;
		
		// AUTHORITARIAN with GID
		//String _sql = "SELECT p."+table.peer.name+" FROM "+table.peer.TNAME +" AS p JOIN "+table.key.TNAME+" AS k"+
		//" ON ("+table.peer.global_peer_ID_hash+"=k."+table.key.ID_hash+") WHERE "+table.peer.peer_ID +"=?;";
		//String sql = "SELECT COUNT(*) FROM "+table.key.TNAME+" WHERE "+table.key.ID_hash +"=?;";
		String sql = "SELECT "+table.key.name+" FROM "+table.key.TNAME+" WHERE "+table.key.public_key +"=?;";

		//String cID=Util.getString(org.get(table.organization.ORG_COL_CREATOR_ID));
		if (DD.ORG_CREATOR_REQUIRED) {
			if (cID == null) return true; // Unknown creator? probably just not set => editable
			D_Peer _creator = D_Peer.getPeerByLID_NoKeep(cID, true);
			ArrayList<ArrayList<Object>> a;
			try {
				a = Application.db.select(sql, new String[]{_creator.getGID()});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return false;
			}
			if (a.size() > 0) return true; // I have the key => editable
			return false; // I do not have the key => not editable;
		}
		
		String gsql = "SELECT k."+table.key.name+" FROM "+table.key.TNAME+" AS k "+
		" WHERE "+table.key.public_key +"=?;";
		ArrayList<ArrayList<Object>> a;
		try {
			if(DEBUG) System.out.println("D_Organization:isEditable: check authoritarian GID");
			a = Application.db.select(gsql, new String[]{GID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		if (a.size() > 0) return true; // I have the key => editable
		return false; // I do not have the key => not editable;
	}
	public int getCertifyingMethod() {
		return this.params.certifMethods;
	}
	public void setCertifyingMethod(int method) {
		this.params.certifMethods = method;
		this.dirty_main = true;
	}

	public String getCreatorGID() {
		if (this.params == null) return null;
		return this.params.creator_global_ID;
	}
	public String getCreatorLID() {
		return this.creator_ID;
	}
	public boolean getRequested() {
		return this.requested;
	}
	public boolean getBlocked() {
		return this.blocked;
	}
	public void setBlocked(boolean blocked2) {
		this.blocked = blocked2;
		dirty_main = true;
	}
	public boolean getBroadcastRule() {
		return this.broadcast_rule;
	}
	public void setBroadcastRule(boolean val) {
		this.broadcast_rule = val;
		this.dirty_main = true;
	}

	public boolean getBroadcasted() {
		return this.broadcasted;
	}
	public void setBroadcasted(boolean val) {
		this.broadcasted = val;
		this.dirty_locals = true;
	}
	
	public String[] getLanguages() {
		return this.params.languages;
	}
	public void setLanguages(String new_text, String[] lang) {
		this.params.languages = lang;
		this.dirty_main = true;
	}
	
	public String getOrgName() {
		return this.name;
	}
	public void setName(String name2) {
		this.name = name2;
		dirty_main = true;
	}
	
	public String[] getNamesOrg() {
		return this.concepts.name_organization;
	}
	public void setNamesOrg(String new_text, String[] names_org) {
		this.concepts.name_organization = names_org;
		this.dirty_main = true;
	}

	public String[] getNamesForum() {
		return this.concepts.name_forum;
	}
	public void setNamesForum(String new_text, String[] _names_forum) {
		this.concepts.name_forum = _names_forum;
		this.dirty_main = true;
	}
	
	public String[] getNamesMotion() {
		return this.concepts.name_motion;
	}
	public void setNamesMotion(String new_text, String[] _names_motion) {
		this.concepts.name_motion = _names_motion;
		this.dirty_main = true;
	}
	
	public String[] getNamesJustification() {
		return this.concepts.name_justification;
	}
	public void setNamesJustification(String just,
			String[] _names_justification) { 
		this.concepts.name_justification = _names_justification;
		dirty_main = true;
	}	
	
	public String[] getDefaultScoringOptions() {
		return this.params.default_scoring_options;
	}
	public void setDefaultScoring(String new_text, String[] _scores) {
		this.params.default_scoring_options = _scores;
	}
	
	public String getDescription() {
		return this.params.description;
	}
	public void setDescription(String descr) {
		this.params.description = descr;
	}
	
	public String getInstructionsNewMotions() {
		return this.params.instructions_new_motions;
	}
	public void setInstructionsNewMotions(String instr) {
		this.params.instructions_new_motions = instr;
		this.dirty_main = true;
	}
	
	public String getInstructionsRegistration() {
		return this.params.instructions_registration;
	}
	public void setInstructionsRegistration(String instr) {
		this.params.instructions_registration = instr;
		this.dirty_main = true;
	}
	
	public String getPreapproved() {
		return this._preapproved;
	}
	public String getCategory() {
		return this.params.category;
	}
	public void setCategory(String new_category) { 
		this.params.category = new_category;
		this.dirty_main = true;
	}
/**
 * If cGID null, get it from cLID
 * @param cLID
 * @param cGID
 */
	public void setCreatorID(String cLID, String cGID) {
		if ((cGID == null) && (cLID != null)) cGID = D_Peer.getGIDfromLID(cLID);
		if ((cLID == null) && (cGID != null)) cLID = D_Peer.getLocalPeerIDforGID(cGID);
		this.creator_ID = cLID;
		this.params.creator_global_ID = cGID;
		this.dirty_main = true;
	}
	
	public static ArrayList<String> getExistingCategories() {
		String sql_cat = "SELECT "+table.organization.category+
		" FROM "+table.organization.TNAME+
		" GROUP BY "+table.organization.category;
		ArrayList<ArrayList<Object>> c;
		ArrayList<String> result = new ArrayList<String>();
		try {
			c = Application.db.select(sql_cat, new String[]{});
			for (ArrayList<Object> i: c) {
				String crt = Util.getString(i.get(0));
				result.add(crt);
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	public boolean hasCreatorID() {
		return this.getCreatorLID() != null;
	}
	/**
	 * Returns the org LIDs
	 * @return
	 */
	public static ArrayList<ArrayList<Object>> getListOrgLIDs() {
		final String sql_orgs = "SELECT "+
				table.organization.organization_ID
				//+","+table.organization.certification_methods+","
				//+table.organization.global_organization_ID_hash+","+table.organization.creator_ID+
				//  ","+table.organization.blocked+","+table.organization.broadcasted+","+table.organization.requested
				+" FROM "+table.organization.TNAME+";";
		try {
			ArrayList<ArrayList<Object>> orgs = Application.db.select(sql_orgs, new String[]{},DEBUG);
			return orgs;
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getOrgNameOrMy() {
		String n = mydata.name;
		if (n != null) return n;
		return getOrgName();
	}
	
	public D_Peer getCreator() {
		String cLID = this.getCreatorLID();
		if ((this.creator == null) && (cLID != null))
			this.creator = D_Peer.getPeerByLID_NoKeep(cLID, true);
		return this.creator;
	}

	public String getCreatorNameMy() {
		return mydata.creator;
	}

	public String getCreatorPeerNameMy() {
		D_Peer c = getCreator();
		if (c == null) return null;
		return c.getNameMy();
	}

	public String getCreatorNameOriginal() {
		D_Peer c = getCreator();
		if (c == null) return null;
		return c.getName();
	}

	public boolean getCreatorNameVerified() {
		D_Peer c = getCreator();
		if (c == null) return false;
		return c.isVerifiedName();
	}

	public String getCategoryOrMy() {
		if (getCategoryMy() != null) return getCategoryMy();
		return getCategory();
	}
	public String getCategoryMy() {
		return mydata.category;
	}

	public void setNameMy(String _name) {
		this.dirty_mydata = true;
		this.mydata.name = _name;
	}

	public void setCreatorMy(String creator2) {
		this.dirty_mydata = true;
		this.mydata.creator = creator2;
	}

	public void setCategoryMy(String cat2) {
		this.dirty_mydata = true;
		this.mydata.category = cat2;
	}
	/*
	private void set_my_data(String field_name, String value, int row) {
		if (row >= data.size()) return;
		if ("".equals(value)) value = null;
		if (_DEBUG) System.out.println("Set value =\""+value+"\"");
		String org_ID = getLIDstr(row);
		try {
			String sql = "SELECT "+field_name+" FROM "+table.my_organization_data.TNAME+" WHERE "+table.my_organization_data.organization_ID+"=?;";
			ArrayList<ArrayList<Object>> orgs = db.select(sql,new String[]{org_ID});
			if(orgs.size()>0){
				db.update(table.my_organization_data.TNAME, new String[]{field_name},
						new String[]{table.my_organization_data.organization_ID}, new String[]{value, org_ID}, _DEBUG);
			}else{
				if(value==null) return;
				db.insert(table.my_organization_data.TNAME,
						new String[]{field_name,table.my_organization_data.organization_ID},
						new String[]{value, org_ID}, _DEBUG);
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	*/

	public boolean haveCreatorKey() {
		D_Peer c = getCreator();
		if (c == null) return false;
		return c.getSK() != null;
	}

	public boolean currentlyEdited() {
		if (this.getLID() <= 0) {
			return true;
		}
		return false;
	}
	public long addEmptyOrgExtraParam(String partNeigh) {
		synchronized(D_Organization.lock_organization_GID_storage) {
			assertReferenced();
			D_OrgParam[] old = params.orgParam;
			if (old == null) old = new D_OrgParam[0];
			D_OrgParam[] nou = new D_OrgParam[old.length+1];
			for (int k = 0; k < old.length; k ++) {
				nou[k] = old[k];
			}
			nou[old.length] = new D_OrgParam();
			nou[old.length].partNeigh = Util.ival(partNeigh, table.field_extra.partNeigh_non_neighborhood_indicator);
			long extra_ID;
			try {
				String new_global_extra = null;
	 			extra_ID = Application.db.insert(table.field_extra.TNAME,
						new String[]{table.field_extra.organization_ID, table.field_extra.global_field_extra_ID, table.field_extra.partNeigh},
						new String[]{this.getLIDstr_forced(), new_global_extra, partNeigh
						}, DEBUG);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
				return -1;
			}
			nou[old.length].field_LID = extra_ID;
			params.orgParam = nou;
			if (DEBUG) System.out.println("D_Organization:addEmptyOrgExtraParam: nou["+old.length+"]="+nou[old.length]);
			return extra_ID;
		}
	}
	/**
	 * Sts dirty
	 * @param new_global_extra
	 * @return
	 */
	public long addEmptyOrgExtraParamTmp(String new_global_extra) {
		
		synchronized(D_Organization.lock_organization_GID_storage) {
			long eID = this.getFieldExtraID(new_global_extra);
			if (eID > 0) return eID;

			assertReferenced();
			D_OrgParam[] old = params.orgParam;
			if (old == null) old = new D_OrgParam[0];
			D_OrgParam[] nou = new D_OrgParam[old.length+1];
			for (int k = 0; k < old.length; k ++) {
				nou[k] = old[k];
			}
			nou[old.length] = new D_OrgParam();
			nou[old.length].partNeigh = table.field_extra.partNeigh_non_neighborhood_indicator;
			nou[old.length].global_field_extra_ID = new_global_extra;
			nou[old.length].tmp = "1";
			nou[old.length].dirty = true;
			this.dirty_params = true;
			long extra_ID;
			try {
				//String new_global_extra = null;
	 			extra_ID = Application.db.insert(table.field_extra.TNAME,
						new String[]{
	 					table.field_extra.organization_ID,
	 					table.field_extra.global_field_extra_ID,
	 					table.field_extra.partNeigh,
	 					table.field_extra.tmp
	 					},
						new String[]{this.getLIDstr_forced(), new_global_extra, nou[old.length].partNeigh+"", "1"
						}, DEBUG);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
				return -1;
			}
			nou[old.length].field_LID = extra_ID;
			params.orgParam = nou;
			return extra_ID;
		}
	}
	public D_OrgParam getOrgParam(int row) {
		if (row < 0) return null;
		if (params == null) return null;
		if (params.orgParam == null) return null;
		if (row >= params.orgParam.length) return null;
		return params.orgParam[row];
	}
	public void deleteOrgParam(int row) {
		synchronized(D_Organization.lock_organization_GID_storage) {
			assertReferenced();
			if (row < 0) return;
			if (params == null) return;
			if (params.orgParam == null) return;
			if (params.orgParam.length >= row) return;

			D_OrgParam[] old = params.orgParam;
			if ((old == null) || (old.length == 0)) return;
			D_OrgParam[] nou = new D_OrgParam[old.length - 1];
			int n = 0;
			for (int k = 0; k < old.length; k ++) {
				if (k == row) continue;
				nou[n ++] = old[k];
			}
			params.orgParam = nou;
	    	try {
				Application.db.delete(table.field_extra.TNAME, new String[]{table.field_extra.field_extra_ID}, new String[]{Util.getStringID(old[row].field_LID)}, DEBUG);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
		}
	}
	public D_OrgParam[] getOrgParams() {
		if (params == null) return null;
		if (params.orgParam == null) return null;
		return params.orgParam;
	}
	/*
	public static boolean set_my_data(String field_name, String value, D_OrgParam row) {
		//synchronized()
		//boolean DEBUG = true;
		if(D_OrgParam.DEBUG)System.out.println("OrgExtraModel:set_my_data: field="+field_name);
		//ArrayList<Object> field = this.m_extras.get(row);
		String fieldID = Util.getStringID(row.field_LID);//Util.getString(field.get(table.field_extra.OPARAM_EXTRA_FIELD_ID));
		String hash = row.global_field_extra_ID;
		try {
			if (table.field_extra.label.equals(field_name) || table.field_extra.list_of_values.equals(field_name)) {
				hash = D_OrgParam.makeGID(value, row.list_of_values, row.version);
			}
			Application.db.updateNoSync(table.field_extra.TNAME,
					new String[]{field_name, table.field_extra.global_field_extra_ID},
					new String[]{table.field_extra.field_extra_ID}, 
					new String[]{value,hash,fieldID}, D_OrgParam.DEBUG);
		
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	*/
	public static D_Organization deleteAllAboutOrg(String orgID) {
		try {
			D_Organization o = D_Organization_Node.loaded_org_By_GID.get(orgID);
			if (o != null) {
				if (o.getStatusReferences() <= 0) {
					if (!D_Organization_Node.dropLoaded(o, false)) {
						System.out.println("D_Organization: deleteAllAboutOrg: referred = "+o.getStatusReferences());
					}
				}
			}
			
			//Application.db.delete(table.field_value.TNAME, new String[]{table.field_value.organization_ID}, new String[]{orgID}, DEBUG);
			//Application.db.delete(table.witness.TNAME, new String[]{table.witness.organization_ID}, new String[]{orgID}, DEBUG);
			//Application.db.delete(table.justification.TNAME, new String[]{table.justification.organization_ID}, new String[]{orgID}, DEBUG);
			//Application.db.delete(table.signature.TNAME, new String[]{table.signature.organization_ID}, new String[]{orgID}, DEBUG);
			String sql="SELECT "+table.constituent.constituent_ID+" FROM "+table.constituent.TNAME+" WHERE "+table.constituent.organization_ID+"=?;";
			ArrayList<ArrayList<Object>> constits = Application.db.select(sql, new String[]{orgID}, DEBUG);
			for (ArrayList<Object> a: constits) {
				String cID = Util.getString(a.get(0));
				Application.db.delete(table.witness.TNAME, new String[]{table.witness.source_ID}, new String[]{cID}, DEBUG);
				Application.db.delete(table.witness.TNAME, new String[]{table.witness.target_ID}, new String[]{cID}, DEBUG);
	
				Application.db.delete(table.motion.TNAME, new String[]{table.motion.constituent_ID}, new String[]{cID}, DEBUG);
				Application.db.delete(table.justification.TNAME, new String[]{table.justification.constituent_ID}, new String[]{cID}, DEBUG);
	   			Application.db.delete(table.signature.TNAME, new String[]{table.signature.constituent_ID}, new String[]{cID}, DEBUG);
	   			// Application.db.delete(table.news.TNAME, new String[]{table.news.constituent_ID}, new String[]{cID}, DEBUG);
	   		}
			Application.db.delete(table.news.TNAME, new String[]{table.news.organization_ID}, new String[]{orgID}, DEBUG);
			Application.db.delete(table.motion.TNAME, new String[]{table.motion.organization_ID}, new String[]{orgID}, DEBUG);
			String sql_del_fv_fe =
					"DELETE FROM "+table.field_value.TNAME+
					" WHERE "+table.field_value.field_extra_ID+
					" IN ( SELECT "+table.field_extra.field_extra_ID+" FROM "+table.field_extra.TNAME+" WHERE "+table.field_extra.organization_ID+"=? )";
			Application.db.delete(sql_del_fv_fe, new String[]{orgID}, DEBUG);
			String sql_del_fv =
					"DELETE FROM "+table.field_value.TNAME+
					" WHERE "+table.field_value.constituent_ID+
					" IN ( SELECT DISTINCT c."+table.constituent.constituent_ID+" FROM "+table.constituent.TNAME+" AS c " +
							" JOIN "+table.field_value.TNAME+" AS v ON (c."+table.constituent.constituent_ID+"=v."+table.field_value.constituent_ID+") " +
									" WHERE c."+table.constituent.organization_ID+"=? GROUP BY c."+table.constituent.constituent_ID+" ) ";
			Application.db.delete(sql_del_fv, new String[]{orgID}, DEBUG);
			Application.db.delete(table.constituent.TNAME, new String[]{table.constituent.organization_ID}, new String[]{orgID}, DEBUG);
			Application.db.delete(table.neighborhood.TNAME, new String[]{table.neighborhood.organization_ID}, new String[]{orgID}, DEBUG);
			Application.db.delete(table.identity_ids.TNAME, new String[]{table.identity_ids.organization_ID}, new String[]{orgID}, DEBUG);
			Application.db.update(table.identity.TNAME, new String[]{table.identity.organization_ID}, new String[]{table.identity.organization_ID}, new String[]{"-1",orgID}, DEBUG);

			return deleteOrgDescription(orgID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	public static D_Organization deleteOrgDescription(String orgID) {
		try {
			Application.db.delete(table.field_extra.TNAME, new String[]{table.field_extra.organization_ID}, new String[]{orgID}, DEBUG);
			Application.db.delete(table.organization.TNAME, new String[]{table.organization.organization_ID}, new String[]{orgID}, DEBUG);
			return unlinkMemory(orgID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	private static D_Organization unlinkMemory(String orgID) {
		Long lID = new Long(orgID);
		D_Organization org = D_Organization_Node.loaded_org_By_LocalID.get(lID);
		if (org == null) { 
			System.out.println("D_Organization: unlinkMemory: referred null org for: "+orgID);
			return null;
		}
		if (DEBUG) System.out.println("D_Organization: unlinkMemory: dropped org");
		if (!D_Organization_Node.dropLoaded(org, true)) {
			if (DEBUG) System.out.println("D_Organization: unlinkMemory: referred = "+org.getStatusReferences());
		} else {
			if (DEBUG) System.out.println("D_Organization: unlinkMemory: no problem dropping");
		}
		return org;
	}
	public long insertTmpFieldExtra(String fieldGID) throws P2PDDSQLException {
		boolean _DEBUG = true;
		if (_DEBUG) System.out.println("D_FieldExtra: insertTmpFieldExtra: start");
		if (fieldGID == null) Util.printCallPath("Null field");
		
		D_Organization org = D_Organization.getOrgByOrg_Keep(this);
		long id = org.addEmptyOrgExtraParamTmp(fieldGID);
		if (org.dirty_any()) org.storeRequest();
//		long id = Application.db.insert(table.field_extra.TNAME,
//				new String[]{table.field_extra.global_field_extra_ID, table.field_extra.organization_ID, table.field_extra.tmp},
//				new String[]{fieldGID, Util.getStringID(org_ID), "1"}, _DEBUG );
		org.releaseReference();
		return id;
	}
	/**
	 * Search in existing fields, else may insert a temporary
	 * @param fieldGID
	 * @param insertTmp
	 * @return
	 * @throws P2PDDSQLException
	 * @throws ExtraFieldException
	 */
	public String getFieldExtraID(String fieldGID, boolean insertTmp) throws P2PDDSQLException, ExtraFieldException {
		if (fieldGID == null) return null;
		//if ((org_ID <= 0) || (org == null)) return null;
		String id = Util.getStringID(getFieldExtraID(fieldGID)); //D_Organization.getFieldExtraID_FromTable(fieldGID, org_ID);
		if (id != null) return id;
		if (! insertTmp) return id;
		if (insertTmp)
			return Util.getStringID(insertTmpFieldExtra(fieldGID));
		throw new ExtraFieldException("Unknown field type: \""+fieldGID+"\" for org \""+getLID_forced()+"\"");
	}
	/**
	 * Search in existing fields (even on disk if not in memory- which should not happen)
	 * @param field_extra_GID
	 * @return
	 */
	public long getFieldExtraID(String field_extra_GID) {
		if (params == null) return -1;
		if (params.orgParam == null) return -1;
		for (D_OrgParam op : params.orgParam) {
			if (Util.equalStrings_null_or_not(field_extra_GID, op.global_field_extra_ID)) {
				if (op.field_LID > 0) return op.field_LID;
				try {
					return Util.lval(D_Organization.getFieldExtraID_FromTable(field_extra_GID, this.getLID_forced()));
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}
		}
		return -1;
	}
	/**
	 * Get the LID of fiels in org_ID, directly from the database
	 * @param field
	 * @param org_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String getFieldExtraID_FromTable(String field, long org_ID) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if (DEBUG) Util.printCallPath("D_Organization: getFieldExtraID: should not get here! field="+field+" oID="+org_ID);
		if (field==null) return null;
		field = field.trim();
		if("".equals(field)) return null;
		
		
		String sql = "SELECT "+table.field_extra.field_extra_ID+
		" FROM "+table.field_extra.TNAME+
		" WHERE "+table.field_extra.global_field_extra_ID+"=? AND "+table.field_extra.organization_ID+"=?;";
		ArrayList<ArrayList<Object>> n = Application.db.select(sql, new String[]{field,Util.getStringID(org_ID)}, DEBUG);
		if (n.size() != 0) {
			if (_DEBUG) Util.printCallPath("D_Organization: getFieldExtraID: unexpectedly found "+field+" ("+org_ID+") as "+Util.getString(n.get(0).get(0)));
			return Util.getString(n.get(0).get(0));
		}
		return null;
	}
	public static D_OrgParam[] getNonEphemeral(D_OrgParam[] orgParam) {
		ArrayList<D_OrgParam> aop = new ArrayList<D_OrgParam>(Arrays.asList(orgParam));
		ArrayList<D_OrgParam> nop = new ArrayList<D_OrgParam>();
		for (D_OrgParam op : aop) {
			if (
					(op.tmp == null) 
					|| "0".equals(op.tmp) //|| Util.equalStrings_null_or_not("0",op.tmp)
					) nop.add(op);
		}
		return nop.toArray(new D_OrgParam[0]);
	}
	public int getOrgParamsLen() {
		if (getOrgParams() == null) return 0;
		return getOrgParams().length;
	}

	public static ArrayList<ResetOrgInfo> buildResetPayload() throws P2PDDSQLException{
		String sql = 
				"SELECT "+table.organization.reset_date+","+table.organization.global_organization_ID_hash+
				" FROM "+table.organization.TNAME+
				" WHERE "+table.organization.signature+" IS NOT NULL AND "+
				table.organization.reset_date+" IS NOT NULL AND "+table.organization.broadcasted+"='1';";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{});
		if(a.size()==0) return null;
		ArrayList<ResetOrgInfo> changed_orgs = new ArrayList<ResetOrgInfo>();
		for(ArrayList<Object> o: a) {
			ResetOrgInfo roi = new ResetOrgInfo();
			roi.reset_date = Util.getCalendar(Util.getString(o.get(0)));
			roi.hash = Util.getString(o.get(1));
			changed_orgs.add(roi);
		}
		
		return changed_orgs;
	}

	public D_OrgParam getFieldExtra(String field_extra_GID) {
		if (params == null || params.orgParam == null) return null;
		for (int k = 0 ; k < this.params.orgParam.length ; k ++ ) {
			if ( Util.equalStrings_null_or_not(this.params.orgParam[k].global_field_extra_ID, field_extra_GID) ) {
				return this.params.orgParam[k];
			}
		}
		return null;
	}

	public static final String sql_extra_fields_RootSubdivisions =
				"SELECT "+table.field_extra.field_extra_ID+
				", "+table.field_extra.label+
				" FROM "+table.field_extra.TNAME+
				" WHERE "+table.field_extra.organization_ID+" = ? AND "+table.field_extra.partNeigh+" > 0 ORDER BY "+table.field_extra.partNeigh+";";
	public static ArrayList<Object> getDefaultRootSubdivisions(long organizationID) {
		ArrayList<Object> result = new ArrayList<Object>();
		String subdivisions;
		long[] fieldIDs = new long[0];
		ArrayList<ArrayList<Object>> fields_neighborhood;
		
		subdivisions = table.neighborhood.SEP_names_subdivisions;
		try {
			fields_neighborhood = Application.db.select(sql_extra_fields_RootSubdivisions,	new String[] {"" + organizationID}, DEBUG);
			
			fieldIDs = new long[fields_neighborhood.size()];
			for (int i = 0; i < fields_neighborhood.size(); i ++) {
				fieldIDs[i] = Util.Lval(fields_neighborhood.get(i).get(0));
				subdivisions = subdivisions+Util.sval(fields_neighborhood.get(i).get(1), "")+table.neighborhood.SEP_names_subdivisions;
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		result.add(subdivisions);
		result.add(fieldIDs);
		return result;
	}


	/**
	 * 
	 * First a store request
	 * @return
	 */
	/*
	public long getLID_force_save() {
		this.storeRequest();
		return this.getLID_forced();
	}
*/
	public void setLID(String __organization_ID) {
		this._organization_ID = Util.lval(__organization_ID);
		this.organization_ID = __organization_ID;
	}
	public void setLID(long __organization_ID) {
		this._organization_ID = __organization_ID;
		this.organization_ID = Util.getStringID(__organization_ID);
	}
	public long getLID() {
		return this._organization_ID;
	}

	public String getLIDstr() {
		return this.organization_ID;
	}
	/**
	 * If none yet and dirty, then save!
	 * return -1 if not dirty and null
	 * 
	 * @return
	 */
	public long getLID_forced() {
		if (this.getLID() <= 0) {
			//String lid = 
			getLIDstr_forced();
		}
		return this.getLID();
	}
	/**
	 * If none yet and dirty, then save!
	 * return null if not dirty and null
	 * @return
	 */
	public String getLIDstr_forced() {
//		if ((this.getLIDstr() == null) && (this.getLID() > 0))
//			this.setLID(Util.getStringID(getLID()));
		if ((this.getLIDstr() == null) && (this.dirty_any())) {
			this.storeSynchronouslyNoException();
			return this.getLIDstr();
		}
		return this.getLIDstr();
	}

	public void updateExtraGIDs() {
		if (this.params == null) return;
		if (this.params.orgParam == null) return;
		for (D_OrgParam p : this.params.orgParam) {
			String eGID = p.makeGID();
			if (!Util.equalStrings_null_or_not(eGID, p.global_field_extra_ID)) {
				p.global_field_extra_ID = eGID;
				p.dirty = true;
				this.dirty_params = true;
			}
		}
	}

	public int getStatusReferences() {
		return status_references;
	}

	StackTraceElement[] lastPath;
	final private Object monitor_reserve = new Object();
	public void incStatusReferences() {
		if (this.getStatusReferences() > 0) {
			//Util.printCallPath("Why getting: "+getStatusReferences());
			Util.printCallPath("D_Organization: incStatusReferences: Will sleep for getting: "+getStatusReferences()+" for "+getOrgName());
			synchronized(monitor_reserve) {
				if (this.getStatusReferences() > 0)
					try {
						do {
							Application_GUI.ThreadsAccounting_ping("Wait org references for "+getOrgName());
							monitor_reserve.wait(10000); // wait 5 seconds, and do not re-sleep on spurious wake-up
							Application_GUI.ThreadsAccounting_ping("Got org references for "+getOrgName());
							Util.printCallPath("D_Organization: incStatusReferences: After sleep getting: "+getStatusReferences()+" for "+getOrgName());
						} while (this.getStatusReferences() > 0);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				if (this.getStatusReferences() > 0) {
					Util.printCallPath(this+"\nSpurious wake after 5s this=: "+getStatusReferences()+" for "+getOrgName());
					Util.printCallPath(lastPath, "Last path was: ", "     ");
				}
			}
		}
		lastPath = Util.getCallPath();
		this.status_references++;
	}
	public void decStatusReferences() {
		if (this.getStatusReferences() <= 0) {
			Util.printCallPath("Why getting: "+getStatusReferences());
			return;
		}
		this.status_references--;
		Application_GUI.ThreadsAccounting_ping("Drop org references for "+getOrgName());
		synchronized(monitor_reserve) {
			monitor_reserve.notify();
		}
		Application_GUI.ThreadsAccounting_ping("Dropped org references for "+getOrgName());
	}
}

class D_Organization_SaverThread extends util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000; // ms to sleep
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	/**
	 * The next monitor is needed to ensure that two D_Organization_SaverThreadWorker are not concurrently modifying the database,
	 * and no thread is started when it is not needed (since one is already running).
	 */
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Organization_SaverThread() {
		super("D_Organization Saver", true);
		//start ();
	}
	public void _run() {
		for (;;) {
			if (stop) return;
			synchronized(saver_thread_monitor) {
				new D_Organization_SaverThreadWorker().start();
			}
			/*
			synchronized(saver_thread_monitor) {
				D_Organization de = D_Organization.need_saving_next();
				if (de != null) {
					if (DEBUG) System.out.println("D_Organization_Saver: loop saving "+de);
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
					//System.out.println("D_Organization_Saver: idle ...");
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

class D_Organization_SaverThreadWorker extends util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000;
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Organization_SaverThreadWorker() {
		super("D_Organization Saver Worker", false);
		//start ();
	}
	public void _run() {
		synchronized(D_Organization_SaverThread.saver_thread_monitor) {
			D_Organization de;
			boolean edited = true;
			if (DEBUG) System.out.println("D_Organization_Saver: start");
			
			 // first try objects being edited
			de = D_Organization.need_saving_obj_next();
			
			// then try remaining objects 
			if (de == null) { de = D_Organization.need_saving_next(); edited = false; }
			
			if (de != null) {
				if (DEBUG) System.out.println("D_Organization_Saver: loop saving "+de.getOrgName());
				Application_GUI.ThreadsAccounting_ping("Saving");
				if (edited) D_Organization.need_saving_obj_remove(de);
				else D_Organization.need_saving_remove(de.getGIDH_or_guess());//, de.instance);
				if (DEBUG) System.out.println("D_Organization_Saver: loop removed need_saving flag");
				// try 3 times to save
				for (int k = 0; k < 3; k++) {
					try {
						if (DEBUG) System.out.println("D_Organization_Saver: loop will try saving k="+k);
						de.storeAct();
						if (DEBUG) System.out.println("D_Organization_Saver: stored org:"+de.getOrgName());
						break;
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
						synchronized(this) {
							try {
								if (DEBUG) System.out.println("D_Organization_Saver: sleep");
								wait(SAVER_SLEEP_ON_ERROR);
								if (DEBUG) System.out.println("D_Organization_Saver: waked error");
							} catch (InterruptedException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
			} else {
				Application_GUI.ThreadsAccounting_ping("Nothing to do!");
				if (DEBUG) System.out.println("D_Organization_Saver: idle ...");
			}
		}
		synchronized(this) {
			try {
				if (DEBUG) System.out.println("D_Organization_Saver: sleep");
				wait(SAVER_SLEEP);
				if (DEBUG) System.out.println("D_Organization_Saver: waked");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

