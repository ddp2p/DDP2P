package data;

import static util.Util._;
import hds.ASNSyncPayload;
import hds.Address;
import hds.PeerInput;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;

import config.Application;
import config.Application_GUI;
import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import streaming.RequestData;
import streaming.UpdatePeersTable;
import table.peer_org;
import util.DBInterface;
import util.DD_Address;
import util.DDP2P_DoubleLinkedList;
import util.DDP2P_DoubleLinkedList_Node;
import util.DDP2P_DoubleLinkedList_Node_Payload;
import util.P2PDDSQLException;
import util.Summary;
import util.Util;
import ciphersuits.Cipher;
import ciphersuits.CipherSuit;
import ciphersuits.PK;
import ciphersuits.SK;

public class D_Peer extends ASNObj implements DDP2P_DoubleLinkedList_Node_Payload<D_Peer>, Summary  {
	public static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	private static final String DEFAULT_VERSION = DD_Address.V3;
	public boolean last_signature_verified_successful = false;
	public boolean signature_verified = false;
	public static class D_Peer_Basic_Data {
		public String globalID;
		public String name;
		public String slogan;
		public String emails;
		public String phones;
		private byte[] _signature;
		public Calendar creation_date;
		public byte[] picture;
		public boolean broadcastable;
		public String version;
		public String globalIDhash;
		//public String plugin_info;
		public boolean revoked;
		public String revokation_instructions;
		public String revokation_GIDH; // proposed next key/ID
		@Deprecated
		public String hash_alg = getStringFromHashAlg(hds.SR.HASH_ALG_V1);
		public String creation_date_str;
		@Deprecated
		private static String getStringFromHashAlg(String[] signature_alg) {
			if (signature_alg==null) return null;
			return Util.concat(signature_alg, DD.APP_ID_HASH_SEP);
		}
		public static String[] getHashAlgFromString(String hash_alg) {
			if (hash_alg!=null) return hash_alg.split(Pattern.quote(DD.APP_ID_HASH_SEP));
			return null;
		}
/*
		public D_Peer_Basic_Data(String hash_alg, byte[] signature,
				byte[] picture, String version) {
			if ((signature == null) || (signature.length == 0)) System.out.println("D_Peer:D_Peer_Basic 2: null sgn");
			this.hash_alg = hash_alg;
			this._signature = signature;
			//System.out.println("D_Peer:D_Peer_Basic_Data:<init_2>: set sign="+Util.concat(signature, ":", "NULL"));
			this.picture = picture;
			this.version = version;
		}
		*/
		public D_Peer_Basic_Data(byte[] signature,
				byte[] picture, String version) {
			//if ((signature == null) || (signature.length == 0)) System.out.println("D_Peer:D_Peer_Basic 1: null sgn");
			this._signature = signature;
			if ((data.HandlingMyself_Peer.get_myself_or_null() != null) 
					&& (this == data.HandlingMyself_Peer.get_myself_or_null().component_basic_data)) {
				System.out.println("D_Peer:D_Peer_Basic 1: sgn = "+signature.length);
			}
			//System.out.println("D_Peer:D_Peer_Basic_Data:<init>: set sign="+Util.concat(signature, ":", "NULL"));
			this.picture = picture;
			this.version = version;
		}
	}
	public static class D_Peer_Local_Agent_State {
		//local_agent_state
		public Calendar arrival_date;
		public String first_provider_peer;
		public String arrival_date_str;
		public String plugins_msg;
		
		/** To date for synchronization for the null instance */
		//public Calendar last_sync_date;
		// Last generalized date when last_sync_date was reset for all orgs
		// (once may need extension for each org separately (advertised by remote)
		//public Calendar last_reset;

		public D_Peer_Local_Agent_State() {
		}
	}
	public static class D_Peer_Preferences {
		// on addition of local fields do not forget to update them in setLocals()
		public Calendar preferences_date; // TODO:
		public boolean used; // whether I try to contact and pull/push from this peer
		public boolean filtered;
		public boolean blocked;
		//public boolean no_update;  // replaced by blocked
		//local_preferences
		private boolean hidden;
		private boolean name_verified;
		private boolean email_verified;
		private String category;
		
		public String my_name;
		public String my_slogan;
		public String my_picture;
		public String my_topic;
		public boolean my_broadcastable;
		public String my_broadcastable_obj; // to show if the broadcastable was set
		public String my_ID;

		public D_Peer_Preferences(boolean used) {
			this.used = used;
		}
	}
	public static class D_Peer_Node {
		private static final int MAX_TRIES = 30;
		/** Currently loaded peers, ordered by the access time*/
		private static DDP2P_DoubleLinkedList<D_Peer> loaded_peers = new DDP2P_DoubleLinkedList<D_Peer>();
		private static Hashtable<Long, D_Peer> loaded_peer_By_LocalID = new Hashtable<Long, D_Peer>();
		private static Hashtable<String, D_Peer> loaded_peer_By_GID = new Hashtable<String, D_Peer>();
		private static Hashtable<String, D_Peer> loaded_peer_By_GIDhash = new Hashtable<String, D_Peer>();
		private static long current_space = 0;
		
		/** message is enough (no need to store the Encoder itself) */
		public byte[] message;
		public DDP2P_DoubleLinkedList_Node<D_Peer> my_node_in_loaded;

		public D_Peer_Node(byte[] message,
				DDP2P_DoubleLinkedList_Node<D_Peer> my_node_in_loaded) {
			this.message = message;
			this.my_node_in_loaded = my_node_in_loaded;
		}
		private static void register_fully_loaded(D_Peer crt) {
			assert((crt.component_node.message==null) && (crt.loaded_globals));
			if(crt.component_node.message != null) return;
			if(!crt.loaded_globals) return;
			byte[] message = crt.encode();
			synchronized(loaded_peers) {
				crt.component_node.message = message; // crt.encoder.getBytes();
				if(crt.component_node.message != null) current_space += crt.component_node.message.length;
			}
		}
		private static void unregister_loaded(D_Peer crt) {
			synchronized(loaded_peers) {
				loaded_peers.remove(crt);
				loaded_peer_By_LocalID.remove(new Long(crt._peer_ID));
				loaded_peer_By_GID.remove(crt.component_basic_data.globalID);
				loaded_peer_By_GIDhash.remove(crt.component_basic_data.globalIDhash);
			}
		}
		private static void register_loaded(D_Peer crt) {
			crt.reloadMessage();
			synchronized(loaded_peers) {
				loaded_peers.offerFirst(crt);
				loaded_peer_By_LocalID.put(new Long(crt._peer_ID), crt);
				loaded_peer_By_GID.put(crt.component_basic_data.globalID, crt);
				loaded_peer_By_GIDhash.put(crt.component_basic_data.globalIDhash, crt);
				if(crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				int tries = 0;
				while((loaded_peers.size() > MAX_LOADED_PEERS)
						|| (current_space > MAX_PEERS_RAM)) {
					if(loaded_peers.size() <= MIN_PEERS_RAM) break; // at least _crt_peer and _myself

					if (tries > MAX_TRIES) break;
					tries ++;
					D_Peer candidate = loaded_peers.getTail();
					if ((candidate.status_references > 0)
							||
							D_Peer.is_crt_peer(candidate)
							||
							(candidate == HandlingMyself_Peer.get_myself())){
						setRecent(candidate);
						continue;
					}
					
					D_Peer removed = loaded_peers.removeTail();//remove(loaded_peers.size()-1);
					loaded_peer_By_LocalID.remove(new Long(removed.get_ID_long()));
					loaded_peer_By_GID.remove(removed.getGID());
					loaded_peer_By_GIDhash.remove(removed.getGIDH());
					if (_DEBUG) System.out.println("D_Peer: register_loaded: remove GIDH="+removed.getGIDH());
					if(removed.component_node.message != null) current_space -= removed.component_node.message.length;				
				}
			}
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_Peer crt) {
			loaded_peers.moveToFront(crt);
		}
	}	
	
	boolean loaded_basics = false; //have we loaded local parameters (name, slogan)
	boolean loaded_locals = false; //have we loaded localIDs (for item received from remote)
	boolean loaded_globals = false; //have we loaded GIDs (for item loaded from database)
	boolean loaded_addresses = false; //have we loaded addresses
	boolean loaded_served_orgs = false; //have we loaded local IDs for served orgs
	boolean loaded_instances = false; //have we loaded instances of this peer
	boolean loaded_my_data = false; //have we loaded my display strings for this peer
	boolean loaded_local_preferences = false; //have we loaded other data about this peer (blocked, etc)
	boolean loaded_local_agent_state = false; //have we loaded other data about this peer (schedules, etc)

	/**
	 * Flags about things that have to be saved since they were changed.
	 * Currently not always flagged when needed, and a re-implementation should implement them better...
	 */
	public boolean dirty_main = false;
	public boolean dirty_addresses = false;
	public boolean dirty_served_orgs = false;
	public boolean dirty_instances = false;
	public boolean dirty_my_data = false;

	public Object dirty_main_monitor = new Object();
	public Object dirty_addresses_monitor = new Object();
	public Object dirty_served_orgs_monitor = new Object();
	public Object dirty_instances_monitor = new Object();
	public Object dirty_my_data_monitor = new Object();

	@Deprecated
	public static final String SIGN_ALG_SEPARATOR = ":";
	@Deprecated
	public String[] signature_alg = hds.SR.HASH_ALG_V1; // of PrintStr OPT

	
	public D_Peer_Basic_Data component_basic_data = new D_Peer_Basic_Data(
			new byte[0], null, DD_Address.V2);
	//locals
	public long _peer_ID = -1;
	public String peer_ID;
	// here go also the local_IDs or served orgs
	// local cache (not encoded/decoded)
	public Cipher keys;
	
	//globals
	public String instance;  // not part of database entry, not signed, but encoded/decoded

	
	public ArrayList<Address> shared_addresses = new ArrayList<Address>();
	public ArrayList<Address> shared_addresses_orig = new ArrayList<Address>();
	public Calendar date_addresses_name_slogan;
	public String _date_addresses_name_slogan;
	
	
	/** served_orgs: _orig Not yet implemented */
	public D_PeerOrgs[] served_orgs = null; //OPT
	public D_PeerOrgs[] served_orgs_orig = null;

	// instances
	public Hashtable<String,D_PeerInstance> _instances = new Hashtable<String, D_PeerInstance>();  
	public Hashtable<String,D_PeerInstance>  _instances_orig =  new Hashtable<String, D_PeerInstance>();
	Hashtable <String, D_PeerInstance> inst_by_ID = new Hashtable <String, D_PeerInstance>();
	//public D_PeerInstance instance_null;
	
	public D_Peer_Preferences component_preferences = new D_Peer_Preferences(false);
	public D_Peer_Local_Agent_State component_local_agent_state = new D_Peer_Local_Agent_State();

	// printing
	public String toSummaryString() {
		String result = "\nPeer: v=" +component_basic_data.version+
				" [gIDH="+this.getGIDH()+" gID="+Util.trimmed(component_basic_data.globalID)+" name="+((component_basic_data.name==null)?"null":"\""+component_basic_data.name+"\"")
		+" slogan="+(component_basic_data.slogan==null?"null":"\""+component_basic_data.slogan+"\"") + " date="+Encoder.getGeneralizedTime(component_basic_data.creation_date);
		//result += " address="+Util.nullDiscrimArray(address, " --- ");
		//result += "\n\t broadcastable="+(broadcastable?"1":"0");
		//result += " sign_alg["+((signature_alg==null)?"NULL":signature_alg.length)+"]="+Util.concat(signature_alg, ":") +" sign="+Util.byteToHexDump(signature, " ");
		//result += " pict="+Util.byteToHexDump(picture, " ");
		result += " served_org["+((served_orgs!=null)?served_orgs.length:"")+"]="+Util.concat(served_orgs, "----", "\"NULL\"");
		return result+"]";
	}	
	public static boolean is_crt_peer(D_Peer candidate) {
		D_Peer myself = data.HandlingMyself_Peer.get_myself();
		if (myself == candidate) return true;
		return Application_GUI.is_crt_peer(candidate);
	}
	public String toString() {
		String result = 
				"\nD_Peer:ID=["+this._peer_ID+"] v=" +component_basic_data.version+
				" [gIDH="+this.getGIDH()+" gID="+Util.trimmed(component_basic_data.globalID)+
				" instance="+instance+
				" name="+((component_basic_data.name==null)?"null":"\""+component_basic_data.name+"\"")+
				" slogan="+(component_basic_data.slogan==null?"null":"\""+component_basic_data.slogan+"\"") + 
				" emails="+((component_basic_data.emails==null)?"null":"\""+component_basic_data.emails+"\"")+
				" phones="+((component_basic_data.phones==null)?"null":"\""+component_basic_data.phones+"\"")+
				" crea_date="+Encoder.getGeneralizedTime(component_basic_data.creation_date);
		
		result += "\n\t address="+Util.concatA(this.shared_addresses, " --- ", "NULL");
		result += "\n\t broadcastable="+(component_basic_data.broadcastable?"1":"0");
		byte[] sgn = getSignature();
		result += "\n\t [] sign_alg["+((signature_alg==null)?"NULL":signature_alg.length)+"]="+Util.concat(signature_alg, ":") +" sign="+((sgn==null)?"null":("["+sgn.length+"]"))+Util.byteToHexDump(sgn, " ") 
		+"\n\t[] pict="+Util.byteToHexDump(component_basic_data.picture, " ")+
		"\n\t served_org["+((served_orgs!=null)?served_orgs.length:"")+"]="+Util.concatOrgs(served_orgs, "----", "\"NULL\"");
		result += "\n\t Me:("+this.my_exists()+"#"+this.component_preferences.my_ID+")"+this.component_preferences.my_name+" b: "+this.component_preferences.my_broadcastable_obj+"("+this.getBroadcastableMy()+","+this.getBroadcastableMyOrDefault()+")"+" sl:"+this.component_preferences.my_slogan;
		result += "\n\t Instances:";
		for (String key : _instances.keySet()) {
			D_PeerInstance inst = _instances.get(key);
			result += "\n\t\t "+inst;
		}
		return result+"]";
	}

	
	// Constructors
	public D_Peer() {this.component_basic_data.version = D_Peer.DEFAULT_VERSION; init_new();}

	// Private constructors
	/**
	 * Exits with Exception when no such peer exists.
	 * loads everything (addresses & orgs)
	 * @param peerID
	 * @throws P2PDDSQLException
	 */
	private D_Peer (long peerID) throws P2PDDSQLException{
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.peer_ID+"=?;";
		String _peerID = Util.getStringID(peerID);
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{_peerID});
		if (p.size() == 0) {
			peer_ID = _peerID;
			if (true) throw new RuntimeException("Absent peer "+peerID);
		} else {
			_init(p.get(0), true, true);
		}
	}
	/**
	 * 
	 * Simple presence of parameter isGID signals a GID.
	 * loads addresses & orgs
	 * 
	 * If newly created then sets dirty flags (but caller should call storeRequest()
	 * after enqueueing it in the cache
	 * 
	 * @param peerGID
	 * @param isGID dummy parameter to differentiate from case with local peerID
	 * @param failOnNew
	 * @throws P2PDDSQLException
	 */
	/*
	private D_Peer(String peerGID, int isGID, boolean failOnNew) throws P2PDDSQLException{
		String gIDH = D_Peer.getGIDHashFromGID(peerGID);
		init(peerGID, gIDH, isGID, failOnNew, null);
	}
	*/
	/**
	 * On creation sets dirty flags but caller should call storeRequest()
	 * @param gID
	 * @param isGID
	 * @param failOnNew
	 * @param __peer
	 * @throws P2PDDSQLException 
	 */
	private D_Peer(String gID, int isGID, boolean failOnNew, D_Peer __peer) throws P2PDDSQLException {
		String gIDH = D_Peer.getGIDHashFromGID(gID);
		init(gID, gIDH, isGID, failOnNew, __peer);
	}
	/**
	 * This does not fail on new.
	 * To fail try: D_Peer(String peerGID, int isGID, boolean failOnNew)
	 * Loads addresses and orgs
	 * @param peerGID
	 * @throws P2PDDSQLException
	 */
	/*
	private D_Peer(String peerGID) throws P2PDDSQLException {
		init(peerGID, 1, false);
	}
	*/
	/**
	 * Loads addresses and orgs.
	 * @param GIDhash
	 * @param failOnNew
	 * @param dummy
	 * @throws P2PDDSQLException 
	 */
	private D_Peer(String GIDhash, boolean failOnNew, int dummy, D_Peer __peer) throws P2PDDSQLException{
		init(null, GIDhash, dummy, failOnNew, __peer);
	}
	/**
	 * Set all "loaded" markers to true.
	 * The only one actually used is loaded_served_orgs, but all seems to be discardable.
	 */
	private void init_new() {
		if (DEBUG) System.out.println("D_Peer: init_new");
		loaded_addresses = true;
		loaded_globals = true;
		loaded_instances = true;
		loaded_locals = true;
		loaded_basics = true;
		loaded_served_orgs = true;
		loaded_my_data = true;
		loaded_local_agent_state = true;
		loaded_local_preferences = true;
	}
	/**
	 * Loading both addresses and served_orgs
	 * @param p
	 */
	/*
	private void init(ArrayList<Object> p) {
		_init(p, true, true);
	}
	*/
	/*
	private void init(ArrayList<Object> p, boolean encode_addresses, boolean encode_served_orgs) {
		if(!encode_addresses)Util.printCallPath("Here we thought addresses are not needed!");
		_init(p,true,encode_served_orgs);
	}
	*/
	/**
	 * Simple presence of parameter isGID signals a GID.
	 * On new object sets dirty but does not call storeRequest()
	 * which would make no sense since it is not yet in the queue
	 * @param peerGID
	 * @param isGID
	 * @param failOnNew
	 * @throws P2PDDSQLException
	 */
	/*
	public void init(String peerGID, int isGID, boolean failOnNew) throws P2PDDSQLException{
		init(peerGID, isGID, failOnNew, null);
	}
	
	public void init(String peerGID, int isGID, boolean failOnNew, D_Peer __peer) throws P2PDDSQLException{
		if(DEBUG) System.out.println("D_Peer: init: GID");
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID+"=?;";
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerGID}, DEBUG);
		if (p.size() == 0) {
			if (failOnNew) throw new RuntimeException("Absent peer "+peerGID);
			this.component_basic_data.globalID = peerGID;
			this.component_basic_data.globalIDhash = getGIDHashFromGID(peerGID);
			if (__peer != null) {
				assert(__peer.get_ID() != null);
				this.component_local_agent_state.first_provider_peer = __peer.get_ID();
			}
			this.dirty_main = true;
			init_new();
		} else {
			init(p.get(0));
		}
	}
	*/
	/**
	 * 
	 * @param gID
	 * @param peerGIDhash should be nonnull
	 * @param isGID
	 * @param failOnNew
	 * @param __peer
	 * @throws P2PDDSQLException
	 */
	public void init(String gID, String peerGIDhash, int isGID, boolean failOnNew, D_Peer __peer) throws P2PDDSQLException{
		// boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Peer: init: gID: GIDhash");
		assert (peerGIDhash != null);
		String sql =
			"SELECT "+table.peer.fields_peers+
			" FROM "+table.peer.TNAME+
			" WHERE "+table.peer.global_peer_ID_hash+"=?;";
		ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerGIDhash}, DEBUG);
		if (p.size() == 0) {
			if (failOnNew) throw new RuntimeException("Absent peer hash "+peerGIDhash);
			this.component_basic_data.globalID = gID;
			this.component_basic_data.globalIDhash = peerGIDhash; //getGIDHashFromGID(peerGID);
			if (__peer != null) {
				assert(__peer.get_ID() != null);
				this.component_local_agent_state.first_provider_peer = __peer.get_ID();
			}
			dirty_main = true;
			init_new();
		} else {
			_init(p.get(0), true, true);
		}
	}
	@SuppressWarnings("unchecked")
	private void _init(ArrayList<Object> p, boolean encode_addresses, boolean encode_served_orgs) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Peer: _init: addr="+encode_addresses+" org="+encode_served_orgs);
		this.loaded_basics = true;
		component_basic_data.version = Util.getString(p.get(table.peer.PEER_COL_VERSION));
		component_basic_data.globalID = Util.getString(p.get(table.peer.PEER_COL_GID)); //dd.globalID;
		component_basic_data.globalIDhash = Util.getString(p.get(table.peer.PEER_COL_GID_HASH)); //dd.globalID;
		component_basic_data.name =  Util.getString(p.get(table.peer.PEER_COL_NAME)); //dd.name;
		component_basic_data.emails =  Util.getString(p.get(table.peer.PEER_COL_EMAILS));
		component_basic_data.phones =  Util.getString(p.get(table.peer.PEER_COL_PHONES));
		component_basic_data.slogan = Util.getString(p.get(table.peer.PEER_COL_SLOGAN)); //dd.slogan;
		component_basic_data.creation_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_CREATION)));
		component_basic_data.picture = Util.byteSignatureFromString(Util.getString(p.get(table.peer.PEER_COL_PICTURE)));
		component_basic_data.broadcastable = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_BROADCAST)), false);
		component_basic_data.hash_alg = Util.getString(p.get(table.peer.PEER_COL_HASH_ALG));
		component_basic_data.revoked = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_REVOKED)), false);
		component_basic_data.revokation_instructions = Util.getString(p.get(table.peer.PEER_COL_REVOK_INSTR));
		component_basic_data.revokation_GIDH = Util.getString(p.get(table.peer.PEER_COL_REVOK_GIDH));
		signature_alg = D_Peer_Basic_Data.getHashAlgFromString(component_basic_data.hash_alg);
		byte[] sgn = Util.byteSignatureFromString(Util.getString(p.get(table.peer.PEER_COL_SIGN)));
		if ((sgn != null) && (sgn.length > 0)) setSignature(sgn, false);
		if (DEBUG) System.out.println("D_Peer: _init: name="+getName()+" gidh="+this.getGIDH());
		if ((component_basic_data.globalIDhash == null) && (component_basic_data.globalID != null)) {
			component_basic_data.globalIDhash = D_Peer.getGIDHashFromGID(component_basic_data.globalID);
			if (_DEBUG) System.out.println("D_Peer: _init: correcting GIDH for: "+getName());
			this.dirty_main = true;
		}
		
		this.loaded_locals = true;
		this.set_ID(Util.getString(p.get(table.peer.PEER_COL_ID)));
		//_peer_ID = Util.lval(peer_ID, -1);

		// a first loading (Deprecated).
		// in the future it will be loaded from the peer_instance tables
		D_PeerInstance instance_null = new D_PeerInstance();
		instance_null.plugin_info =  Util.getString(p.get(table.peer.PEER_COL_PLUG_INFO));
		instance_null.set_last_sync_date(Util.getString(p.get(table.peer.PEER_COL_LAST_SYNC)));
		instance_null.set_last_reset(Util.getString(p.get(table.peer.PEER_COL_LAST_RESET)));
		instance_null.setLID(peer_ID, _peer_ID);
		this.putPeerInstance(null, instance_null);
		
		if (encode_served_orgs) {
			served_orgs = _getPeerOrgs(_peer_ID); // this also loads globals
			loaded_globals = true;
			this.loaded_served_orgs = true;
			if (served_orgs != null) {
				served_orgs_orig = served_orgs.clone();
			}
		}

		try {
			loadInstancesToHash();
			_instances_orig = D_PeerInstance.deep_clone(_instances);
			this.loaded_instances = true;
			this.inst_by_ID = get_inst_By_ID();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (encode_addresses) { //address = TypedAddress.getPeeres(peer_ID);
			// addresses always needed
			load_addresses();
			//address = TypedAddress.loadPeeres(peer_ID);
			//this.address_orig = TypedAddress.deep_clone(address);
			this.loaded_addresses = true;
		}

		component_local_agent_state.arrival_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_ARRIVAL)));
		component_local_agent_state.plugins_msg = Util.getString(p.get(table.peer.PEER_COL_PLUG_MSG));
		component_local_agent_state.first_provider_peer = Util.getString(p.get(table.peer.PEER_COL_FIRST_PROVIDER_PEER));
		this.loaded_local_agent_state = true;

		component_preferences.preferences_date = Util.getCalendar(Util.getString(p.get(table.peer.PEER_COL_PREFERENCES_DATE)));
		//no_update = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_NOUPDATE)), false);
		component_preferences.used = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_USED)), false);
		component_preferences.blocked = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_BLOCKED)), false);
		component_preferences.filtered = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_FILTERED)), false);
		component_preferences.hidden = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_HIDDEN)), false);
		component_preferences.name_verified = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_VER_NAME)), false);
		component_preferences.email_verified = Util.stringInt2bool(Util.getString(p.get(table.peer.PEER_COL_VER_EMAIL)), false);
		component_preferences.category = Util.getString(p.get(table.peer.PEER_COL_CATEG));

		try {
			loadMyPeerData();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		this.loaded_local_preferences = true;
		
		//experience = Util.getString(p.get(table.peer.PEER_COL_EXPERIENCE));
		//exp_avg = Util.getString(p.get(table.peer.PEER_COL_EXP_AVG));
		if (this.dirty_any()) this.storeRequest();
	}
	/**
	 * Loads instances (including the null instance, if found)
	 * @param _peer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public Hashtable<String,D_PeerInstance> loadInstancesToHash() {
		if (this.loaded_instances) return _instances;
		if (D_PeerInstance.DEBUG) System.out.println("D_PeerInstance:loadInstances: start:"+_peer_ID);
		try {
			//Hashtable<String,D_PeerInstance> result = new Hashtable<String,D_PeerInstance>();
			ArrayList<ArrayList<Object>> i = Application.db.select(D_PeerInstance.sql_peer, new String[]{peer_ID}, D_PeerInstance.DEBUG);
			for (ArrayList<Object> k: i) {
				D_PeerInstance crt = new D_PeerInstance(k);
				//if (crt.peer_instance == null) this.instance_null = crt;
				//else result.put(crt.peer_instance, crt);
				this.putPeerInstance(crt.peer_instance, crt);
				if (D_PeerInstance.DEBUG) System.out.println("D_PeerInstance:loadInstances: got:"+crt);
				if (D_PeerInstance.DEBUG) Util.printCallPath("why");
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		return _instances;
	}
	private String sql_peer_my_data = 
			"SELECT "+table.peer_my_data.fields+
			" FROM "+table.peer_my_data.TNAME+
			" WHERE "+table.peer_my_data.peer_ID+"=?;" // .peer_my_data_ID+"=?;"
			;
	private void loadMyPeerData() throws P2PDDSQLException {
		if (peer_ID == null) return;
		ArrayList<ArrayList<Object>> data =
				Application.db.select(sql_peer_my_data,
				new String[]{peer_ID});
		if (data.size() == 0) return;
		ArrayList<Object> d = data.get(0);
		this.component_preferences.my_ID = Util.getString(d.get(table.peer_my_data.COL_ROW));
		this.component_preferences.my_name = Util.getString(d.get(table.peer_my_data.COL_NAME));
		this.component_preferences.my_slogan = Util.getString(d.get(table.peer_my_data.COL_SLOGAN));
		this.component_preferences.my_picture = Util.getString(d.get(table.peer_my_data.COL_PICTURE));
		this.component_preferences.my_topic = Util.getString(d.get(table.peer_my_data.COL_TOPIC));
		this.component_preferences.my_broadcastable_obj = Util.getString(d.get(table.peer_my_data.COL_BROADCASTABLE));
		this.component_preferences.my_broadcastable = Util.stringInt2bool(this.component_preferences.my_broadcastable_obj, false);
	}
	/**
	 * Builds a hash of the instances by their ID.
	 * @return
	 */
	public Hashtable <String, D_PeerInstance> get_inst_By_ID() {
		Hashtable <String, D_PeerInstance> inst_By_ID = new Hashtable <String, D_PeerInstance>();
		for (D_PeerInstance i : _instances.values()) {
			String id = i.get_peer_instance_ID();
			if (id == null) continue; // no real "null" identity
			inst_By_ID.put(id, i);
		}
		return inst_By_ID;
	}
	private static String sql_get_addresses_by_peerID =
			"SELECT "+table.peer_address.fields+
			" FROM "+table.peer_address.TNAME+
			" WHERE "+table.peer_address.peer_ID+"=?" +
			" ORDER BY "+table.peer_address.certified+" DESC," +
					table.peer_address.priority+"," +
					table.peer_address.type+"," +
					table.peer_address.address+";";
	private void load_addresses() {
		if (DEBUG) System.out.println("D_Peer:loadPeeres: start = "+peer_ID);
		ArrayList<ArrayList<Object>> a;
		try {
			a = Application.db.select(sql_get_addresses_by_peerID, new String[]{peer_ID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return;
		}
		int len = a.size();
		if (len == 0) {
			if (DEBUG) System.out.println("D_Peer:loadPeeres: empty = "+len);
			return;
		}
		for (ArrayList<Object> _address : a) {
			D_PeerInstance inst;
			Address address = new Address(_address, this);
			if (isSharedAddressInstanceID(address._instance_ID)) {
				shared_addresses.add(address);
				shared_addresses_orig.add(address);
				continue;
			}
			if (isNullInstanceID(address._instance_ID)) {
				inst = this.getPeerInstance(null);
			} else {
				inst = inst_by_ID.get(address.instance_ID_str);
			}
			if (inst == null) {
				System.out.println("D_Peer: load_addresses: missing instance");
				continue;
			}
			
			address.branch = inst.branch;
			address.agent_version = inst.agent_version;
			inst.addresses.add(address);
			inst.addresses_orig.add(address);
		}
		
		if (DEBUG) System.out.println("D_Peer:loadPeeres: done = "+peer_ID);
	}
	/**
	 * Negative and null instanceID as assumed shared
	 * @param _instance_ID
	 * @return
	 */
	public static boolean isSharedAddressInstanceID(long _instance_ID) {
		return _instance_ID <= getSharedAddressInstanceID();
	}
	public static long getSharedAddressInstanceID() {
		return -1;
	}

	/**
	 * 0 is the null instance ID, if not special
	 * @param _instance_ID
	 * @return
	 */
	public static boolean isNullInstanceID(long _instance_ID) {
		return _instance_ID == getNullInstanceID();
	}
	public static long getNullInstanceID() {
		return 0;
	}
	/**
	 * Without throwing exceptions
	 * @param peer_ID
	 * @return
	 */
	private static D_PeerOrgs[] _getPeerOrgs(long peer_ID) {
		D_PeerOrgs[] result = null;
		if (peer_org.DEBUG) System.out.println("\n************\npeer_org: getPeerOrgs: enter "+peer_ID);
		try {
			result = getPeerOrgs(peer_ID);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if (peer_org.DEBUG) System.out.println("\n************\npeer_org: getPeerOrgs: result "+result);
		return result;
	}
	/**
	 * With exception
	 * @param _peer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static D_PeerOrgs[] getPeerOrgs(long _peer_ID) throws P2PDDSQLException {
		String peer_ID = Util.getStringID(_peer_ID);
		if(DEBUG) System.out.println("peer_org: getPeerOrgs: "+peer_ID);
		D_PeerOrgs[] result = null;
		String queryOrgs = "SELECT o."+
					table.organization.global_organization_ID+
					", o."+table.organization.name+
					", po."+table.peer_org.last_sync_date+
					", o."+table.organization.global_organization_ID_hash+
					", po."+table.peer_org.served+
					", po."+table.peer_org.organization_ID+
					", po."+table.peer_org.peer_org_ID+
				" FROM "+table.peer_org.TNAME+" AS po " +
				" LEFT JOIN "+table.organization.TNAME+" AS o ON (po."+table.peer_org.organization_ID+"==o."+table.organization.organization_ID+") " +
				" WHERE ( po."+table.peer_org.peer_ID+" == ? ) "+
				" AND po."+table.peer_org.served+"== '1'"+
				" ORDER BY o."+table.organization.global_organization_ID;
		ArrayList<ArrayList<Object>>p_data = Application.db.select(queryOrgs, new String[]{peer_ID}, DEBUG);
		result = new D_PeerOrgs[p_data.size()];
		for (int i = 0; i < p_data.size(); i++) {
			ArrayList<Object> o = p_data.get(i);
			result[i] = new D_PeerOrgs();
			result[i].global_organization_ID = Util.getString(o.get(0));
			result[i].peer_org_ID = Util.lval(o.get(0), -1);
			result[i].org_name = Util.getString(o.get(1));
			result[i].set_last_sync_date(Util.getString(o.get(2)), false);
			result[i].setOrgGIDH(Util.getString(o.get(3)));
			result[i].setServed(Util.stringInt2bool(o.get(4), false), false);
			result[i].organization_ID = Util.lval(o.get(5), -1);
		}
		if(DEBUG) System.out.println("peer_org: getPeerOrgs: result ="+result);
		return result;
	}
	
	// Factories

	/**
	 * exception raised on error
	 * @param ID
	 * @param load_Globals 
	 * @return
	 */
	static public D_Peer getPeerByLID_AttemptCacheOnly(long ID, boolean load_Globals) {
		Long id = new Long(ID);
		D_Peer crt = D_Peer_Node.loaded_peer_By_LocalID.get(id);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals){
			crt.loadGlobals();
			D_Peer_Node.register_fully_loaded(crt);
		}
		D_Peer_Node.setRecent(crt);
		return crt;
	}	
	/**
	 * If the data is not in the cache, it will load it
	 * @param ID
	 * @param load_Globals
	 * @return
	 */
	static public D_Peer getPeerByLID(long ID, boolean load_Globals){
		if (ID <= 0) return null;
		Long id = new Long(ID);
		// try first to see if one can avoid waiting for the monitor
		// can comment this out if the structure is not safe for multithreading
		D_Peer crt = D_Peer.getPeerByLID_AttemptCacheOnly(id, load_Globals);
		if (crt != null) return crt;
		
		synchronized(monitor_object_factory) {
			// retry, just in case some other thread just got it
			crt = D_Peer.getPeerByLID_AttemptCacheOnly(id, load_Globals);
			if(crt != null) return crt;
			
			try {
				crt = new D_Peer(ID);
			} catch (Exception e) {
				if(DEBUG) e.printStackTrace();
				return null;
			}
			D_Peer_Node.register_loaded(crt);
			return crt;
		}
	}
	static public D_Peer getPeerByLID(String ID, boolean load_Globals){
		try{
			return getPeerByLID(Util.lval(ID), load_Globals);
		}catch (Exception e) {
			return null;
		}
	}
	/**
	 * Only attempts too load if the data is already in the cache
	 * exception raised on error
	 * @param GID
	 * @param load_Globals 
	 * @return
	 */
	static public D_Peer getPeerByGID_AttemptCacheOnly(String GID, boolean load_Globals) {
		if (GID == null) return null;
		D_Peer crt = D_Peer_Node.loaded_peer_By_GID.get(GID);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals){
			crt.loadGlobals();
			D_Peer_Node.register_fully_loaded(crt);
		}
		D_Peer_Node.setRecent(crt);
		return crt;
	}
	/**
	 * 
	 * @param GID
	 * @param load_Globals
	 * @param __peer 
	 * @return
	 */
	static public D_Peer getPeerByGID(String GID, boolean load_Globals, boolean create) {
		return getPeerByGID(GID, load_Globals, create, null);
	}
	static public D_Peer getPeerByGID(String GID, boolean load_Globals, boolean create, D_Peer __peer) {
		D_Peer p = getPeerByGID_NoStore(GID, load_Globals, create, __peer);
		// if newly created, then they are dirty
		if ((p != null) && p.dirty_any()) p.storeRequest();
		return p;
	}
	static public D_Peer getPeerByGID_NoStore(String GID, boolean load_Globals, boolean create, D_Peer __peer) {
		// boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Peer: getPeerByGID_NoStore: "+GID+" glob="+load_Globals+" cre="+create+" _peer="+__peer);
		if (GID == null){
			if (DEBUG) System.out.println("D_Peer: getPeerByGID_NoStore: null GID");
			return null;
		}
		D_Peer crt = D_Peer.getPeerByGID_AttemptCacheOnly(GID, load_Globals);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Peer: getPeerByGID_NoStore: got GID cached crt="+crt);
			return crt;
		}
		String gIDH = D_Peer.getGIDHashFromGID(GID);

		crt = D_Peer.getPeerByGIDhash_AttemptCacheOnly(gIDH, load_Globals, false);
		if (crt != null) {
			crt.setGID(GID, gIDH);
			if (DEBUG) System.out.println("D_Peer: getPeerByGID_NoStore: got GIDH cached crt="+crt);
			return crt;
		}

		synchronized(monitor_object_factory) {
			crt = D_Peer.getPeerByGIDhash_AttemptCacheOnly(gIDH, load_Globals, false);
			if (crt != null) {
				crt.setGID(GID, gIDH);
				if (DEBUG) System.out.println("D_Peer: getPeerByGID_NoStore: got sync cached crt="+crt);
				return crt;
			}

			try {
				crt = new D_Peer(GID, 1, !create, __peer);
				if (DEBUG) System.out.println("D_Peer: getPeerByGID_NoStore: loaded crt="+crt);
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Peer: getPeerByGID_NoStore: error loading");
				e.printStackTrace();
				return null;
			}
			D_Peer_Node.register_loaded(crt);
			if (DEBUG) System.out.println("D_Peer: getPeerByGID_NoStore: Done");
			return crt;
		}
	}
	/**
	 * exception raised on error
	 * @param GIDhash
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static public D_Peer getPeerByGIDhash_AttemptCacheOnly(String GIDhash, boolean load_Globals, boolean keep) {
		if (GIDhash == null) return null;
		if (keep) {
			synchronized(monitor_object_factory) {
				D_Peer crt = D_Peer_Node.loaded_peer_By_GIDhash.get(GIDhash);
				if (crt != null) {			
					if (keep) crt.status_references ++;
					if(load_Globals && !crt.loaded_globals){
						crt.loadGlobals();
						D_Peer_Node.register_fully_loaded(crt);
					}
					D_Peer_Node.setRecent(crt);
					return crt;
				}
			}
		}else{
			D_Peer crt = D_Peer_Node.loaded_peer_By_GIDhash.get(GIDhash);
			if (crt != null) {			
				//if (keep) crt.status_references ++;
				if (load_Globals && !crt.loaded_globals) { // unlikely
					synchronized(monitor_object_factory) {
						if(load_Globals && !crt.loaded_globals){ // retest to avoid race
							crt.loadGlobals();
							D_Peer_Node.register_fully_loaded(crt);
						}
					}
				}
				D_Peer_Node.setRecent(crt);
				return crt;
			}
		}
		return null;
	}
	/**
	 * 
	 * @param GIDhash
	 * @param load_Globals
	 * @param keep  : if true, avoid releasing this until calling releaseReference()
	 * @param createTemp : create a temporary if not founs?
	 * @return
	 */
	static public D_Peer getPeerByGIDhash(String GIDhash, boolean load_Globals, boolean keep, boolean createTemp){
		if (GIDhash == null) return null;
		D_Peer crt = getPeerByGIDhash_AttemptCacheOnly(GIDhash, load_Globals, keep);
		if(crt != null) return crt;
		
		synchronized(monitor_object_factory) {
			crt = getPeerByGIDhash_AttemptCacheOnly(GIDhash, load_Globals, keep);
			if(crt != null) return crt;

			try {
				crt = new D_Peer(GIDhash, !createTemp, 0, null);
				if (keep) crt.status_references ++;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			D_Peer_Node.register_loaded(crt);
			return crt;
		}
	}
	/**
	 * Usable until calling releaseReference()
	 * Verify with assertReferenced()
	 * @param peer
	 * @return
	 */
	static public D_Peer getPeerByPeer_Keep(D_Peer peer) {
		if (peer == null) return null;
		return getPeerByGIDhash(peer.getGIDH(), true, true, false);
	} 
	/**
	 * No keep
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals 
	 * @return
	 */
	static public D_Peer getPeerByGID_or_GIDhash_AttemptCacheOnly(String GID, String GIDhash, boolean load_Globals){
		if ((GID == null) && (GIDhash == null)) return null;
		if (GID != null) {
			String hash = D_Peer.getGIDHashFromGID(GID);
			if (GIDhash != null) {
				if (!hash.equals(GIDhash)) {
					System.out.println("D_Peer:getPeerByGID_or_GIDhash_Attempt: mismatch "+GIDhash+" vs "+hash);
					throw new RuntimeException("No GID and GIDhash match");
				}
			}else GIDhash = hash;
		}
		D_Peer crt = D_Peer_Node.loaded_peer_By_GIDhash.get(GIDhash);
		if((crt==null)&&(GID!=null)) crt = D_Peer_Node.loaded_peer_By_GID.get(GID);
		if(crt != null){
			if(load_Globals && !crt.loaded_globals){
				crt.loadGlobals();
				D_Peer_Node.register_fully_loaded(crt);
			}
			D_Peer_Node.setRecent(crt);
			return crt;
		}
		return null;
	}
	/**
	 * No keep.
	 * exception raised on error
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals
	 * @param createTemp TODO
	 * @return
	 */
	static public D_Peer getPeerByGID_or_GIDhash(String GID, String GIDhash, boolean load_Globals, boolean createTemp) {
		if ((GID == null) && (GIDhash == null)) return null;
		D_Peer crt;
		try {
			crt = getPeerByGID_or_GIDhash_AttemptCacheOnly(GID, GIDhash, load_Globals);
			if (crt != null) return crt;
		} catch (Exception e) {e.printStackTrace();}

		synchronized(monitor_object_factory) {
			try {
				crt = getPeerByGID_or_GIDhash_AttemptCacheOnly(GID, GIDhash, load_Globals);
				if (crt != null) return crt;
			} catch (Exception e) {e.printStackTrace();}
			
			try {
				if (GID != null) crt = new D_Peer(GID, 1, !createTemp, null);
				else crt = new D_Peer(GIDhash, !createTemp, 0, null);
			} catch (Exception e) {
				//e.printStackTrace();//simply not present
				return null;
			}
			D_Peer_Node.register_loaded(crt);
			// if newly created, then they are dirty
			if (crt.dirty_any()) crt.storeRequest();
			//Util.printCallPath("CRT="+crt);
			return crt;
		}
	}
	/**
	 * Used when we are now sure whether what we have is a GID or a GIDhash
	 * exception raised on error
	 * @param load_Globals (should GIDs in served orgs load full D_Orgs)
	 * @param createTemp TODO
	 * @param GID
	 * @param GIDhash
	 * @return
	 */
	static public D_Peer getPeerBy_or_hash(String GID_or_hash, boolean load_Globals, boolean createTemp){
		if(GID_or_hash == null) return null;
		String GID = null;
		String GIDhash = D_Peer.getGIDHashGuess(GID_or_hash);
		if(!GIDhash.equals(GID_or_hash)) GID = GID_or_hash;

		D_Peer crt = getPeerByGID_or_GIDhash_AttemptCacheOnly(GID, GIDhash, load_Globals);
		if(crt != null) return crt;

		synchronized(monitor_object_factory) {
			crt = getPeerByGID_or_GIDhash_AttemptCacheOnly(GID, GIDhash, load_Globals);
			if(crt != null) return crt;
			
			try {
				if(GID != null) crt = new D_Peer(GID, 1, !createTemp, null);
				else crt = new D_Peer(GIDhash, !createTemp, 0, null);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			D_Peer_Node.register_loaded(crt);
			// if newly created, then they are dirty
			if (crt.dirty_any()) crt.storeRequest();
			return crt;
		}
	}
	
	
	
	/** Storing */
	public static D_Peer_SaverThread saverThread = new D_Peer_SaverThread();
	/**
	 * This function has to be called after the appropriate dirty flags are set:
	 * dirty_main - for elements in the peer table
	 * dirty_instances - ...
	 * dirty_served_orgs - ...
	 * dirty_my_data - ...
	 * dirty_addresses - ...
	 * 
	 * This returns asynchronously (without waiting for the storing to happen).
	 */
	public void storeRequest() {
		//Util.printCallPath("Why store?");
		if (!this.dirty_any()) {
			Util.printCallPath("Why store when not dirty?");
			return;
		}
		if (this.getGIDH() == null) {
			Util.printCallPath("Cannot store null:\n"+this);
			return;
		}
		if (DEBUG) System.out.println("D_Peer:storeRequest: GIDH="+this.getGIDH());
		D_Peer._need_saving.add(this.getGIDH());
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
		this.storeRequest();
		return this.get_ID_long();
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
	 * 
	 * The actual saving function where everything happens :)
	 * @return
	 * @throws P2PDDSQLException 
	 */
	synchronized public long storeAct() throws P2PDDSQLException {
		//boolean DEBUG = true;
		//if (DEBUG) Util.printCallPath("");
		if (DEBUG) System.err.println("D_Peer: _storeAct: start this="+this);
		
		// use status_references in the following way:
		// 1. change each D_Peer.getPeerxxx, adding a parameter: "boolean reserve"
		// 2. when reserve is true, increment status_references
		// 3. have a method releasePeer() to decrement status_references
		// 4. only remove from queue elements with status_references == 0;
		D_Peer other = D_Peer.getPeerByGIDhash_AttemptCacheOnly(this.getGIDH(), false, false);
		if ((other != null) && (other != this)) {
			//storeReceived(this, true);
			// 
			Util.printCallPath("Why is this peer forgotten? Its preferences are lost. Reload before modifications! May have to introduce usage counters..");
			return other._peer_ID;
		}
		
		if (!dirty_any()) {
			if (DEBUG) System.err.println("D_Peer: _storeAct: exit none dirty");
			return _peer_ID;
		}
		
		if (this.dirty_main){
			if (DEBUG) System.err.println("D_Peer: _storeAct: dirty_main");
			storeBasicComponent();
		}
		if (this.dirty_instances) {
			if (DEBUG) System.err.println("D_Peer: _storeAct: dirty_instances");
			storeInstances();
		}
		if (this.dirty_served_orgs) {
			if (DEBUG) System.err.println("D_Peer: _storeAct: dirty_orgs");
			storeServedOrgs();
		}
		if (this.dirty_my_data) {
			if (DEBUG) System.err.println("D_Peer: _storeAct: dirty_my_data");
			storeMyPeerData();
		}
		if (this.dirty_addresses) {
			if (DEBUG) System.err.println("D_Peer: _storeAct: dirty_addresses");
			storeAddresses();
		}
		if (dirty_any())
			if (DEBUG) System.err.println("D_Peer: _storeAct: still dirty: m:"+
					dirty_main+", i:"+dirty_instances+", o:"+dirty_served_orgs+
					", my="+dirty_my_data+
					", a"+dirty_addresses);

		if (DEBUG) System.err.println("D_Peer: _storeAct: exit done: "+_peer_ID);
		return _peer_ID;
	}

	private void storeBasicComponent() throws P2PDDSQLException {
		if(DEBUG) System.out.println("\n\n***********\nD_Peer: doSave: "+this);

		if ((component_basic_data.globalIDhash == null) && (component_basic_data.globalID != null)) {
			component_basic_data.globalIDhash = D_Peer.getGIDHashFromGID(component_basic_data.globalID);
			if (_DEBUG) System.out.println("D_Peer: storeBasicComponent: setting GIDH for: "+getName());
			//this.dirty_main = true;
		}

		String params[];
		if (peer_ID == null) params = new String[table.peer.PEER_COL_FIELDS_NO_ID];
		else params = new String[table.peer.PEER_COL_FIELDS];
		synchronized (dirty_main_monitor) {
			this.dirty_main = false;
			params[table.peer.PEER_COL_GID]=component_basic_data.globalID;
			params[table.peer.PEER_COL_GID_HASH]=component_basic_data.globalIDhash;
			params[table.peer.PEER_COL_NAME]=component_basic_data.name;
			params[table.peer.PEER_COL_SLOGAN]=component_basic_data.slogan;
			params[table.peer.PEER_COL_EMAILS]=component_basic_data.emails;
			params[table.peer.PEER_COL_PHONES]=component_basic_data.phones;
			params[table.peer.PEER_COL_HASH_ALG]=component_basic_data.hash_alg;
			params[table.peer.PEER_COL_REVOKED]=Util.bool2StringInt(component_basic_data.revoked);
			params[table.peer.PEER_COL_REVOK_INSTR]=component_basic_data.revokation_instructions;
			params[table.peer.PEER_COL_REVOK_GIDH]=component_basic_data.revokation_GIDH;
			params[table.peer.PEER_COL_SIGN]=Util.stringSignatureFromByte(getSignature());
			params[table.peer.PEER_COL_CREATION]=Encoder.getGeneralizedTime(component_basic_data.creation_date);
			params[table.peer.PEER_COL_BROADCAST]=Util.bool2StringInt(component_basic_data.broadcastable);
			//params[table.peer.PEER_COL_NOUPDATE]=Util.bool2StringInt(no_update);
			//params[table.peer.PEER_COL_PLUG_INFO]=component_basic_data.plugin_info;
			params[table.peer.PEER_COL_PLUG_MSG]=component_local_agent_state.plugins_msg;
			params[table.peer.PEER_COL_FILTERED]=Util.bool2StringInt(component_preferences.filtered);
			//params[table.peer.PEER_COL_LAST_SYNC]=Encoder.getGeneralizedTime(component_local_agent_state.last_sync_date);
			params[table.peer.PEER_COL_ARRIVAL]=Encoder.getGeneralizedTime(component_local_agent_state.arrival_date);
			params[table.peer.PEER_COL_FIRST_PROVIDER_PEER]=component_local_agent_state.first_provider_peer;
			params[table.peer.PEER_COL_PREFERENCES_DATE]=Encoder.getGeneralizedTime(component_preferences.preferences_date);
			//params[table.peer.PEER_COL_LAST_RESET]=(component_local_agent_state.last_reset!=null)?Encoder.getGeneralizedTime(component_local_agent_state.last_reset):null;
			params[table.peer.PEER_COL_USED]=Util.bool2StringInt(component_preferences.used);
			params[table.peer.PEER_COL_BLOCKED]=Util.bool2StringInt(component_preferences.blocked);
			params[table.peer.PEER_COL_HIDDEN]=Util.bool2StringInt(component_preferences.hidden);
			params[table.peer.PEER_COL_VER_EMAIL]=Util.bool2StringInt(component_preferences.email_verified);
			params[table.peer.PEER_COL_VER_NAME]=Util.bool2StringInt(component_preferences.name_verified);
			params[table.peer.PEER_COL_CATEG]=component_preferences.category;
			params[table.peer.PEER_COL_PICTURE]=Util.stringSignatureFromByte(component_basic_data.picture);
			//params[table.peer.PEER_COL_EXP_AVG]=exp_avg;
			//params[table.peer.PEER_COL_EXPERIENCE]=experience;
			params[table.peer.PEER_COL_VERSION]=component_basic_data.version;
		}
		
		if (peer_ID == null) {
			_peer_ID = Application.db.insert(table.peer.TNAME,
					Util.trimmed(table.peer.list_fields_peers_no_ID),
					params,
					DEBUG);
			peer_ID = Util.getStringID(_peer_ID);
			this.component_node.loaded_peer_By_LocalID.put(new Long(_peer_ID), this);

			if (DEBUG) System.out.println("D_Peer: doSave: inserted "+_peer_ID);
		} else {
			params[table.peer.PEER_COL_ID] = peer_ID;
			if("-1".equals(peer_ID))Util.printCallPath("peer_ID -1: "+this);
			Application.db.update(table.peer.TNAME,
					Util.trimmed(table.peer.list_fields_peers_no_ID),
					new String[]{table.peer.peer_ID},
					params,
					DEBUG);
			if (DEBUG) System.out.println("D_Peer: doSave: updated "+peer_ID);
		}
	}

	private void storeInstances() throws P2PDDSQLException {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Peer: storeInstances peer_ID="+_peer_ID);
		this.dirty_instances = false;
		// if (_peer_ID == null) return;
		for (D_PeerInstance inst : _instances_orig.values()) {
			D_PeerInstance pi = this.getPeerInstance(inst.peer_instance);
			if (pi == null) {
				assert (inst.get_peer_instance_ID() != null);
				this.inst_by_ID.remove(inst.get_peer_instance_ID());
				Application.db.delete(table.peer_instance.TNAME,
						new String[]{table.peer_instance.peer_instance_ID},
						new String[]{inst.get_peer_instance_ID()}, DEBUG);
			}
		}
		for (D_PeerInstance inst : _instances.values()) {
			if (inst.dirty) {
				inst.setLID(peer_ID, _peer_ID);
				long instID = inst.store();
				this.inst_by_ID.put(inst.get_peer_instance_ID(), inst);
				if (DEBUG) System.out.println("D_Peer: storeInstances dirty got:"+instID);
			} else {
				if (DEBUG) System.out.println("D_Peer: storeInstances not dirty:"+inst);
			}
		}
		if (DEBUG) System.out.println("D_PeerInstance: storeInstances done");
	}
	
	@SuppressWarnings("unchecked")
	private void storeAddresses() throws P2PDDSQLException {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_PeerInstance: storeAddresses start");
		synchronized(this.dirty_addresses_monitor) {
			//update global addresses (directories)
			// first delete removed ones
			purgeExtraAddresses(null, this.shared_addresses);
			this.dirty_addresses = false;
			for (Address adr : this.shared_addresses_orig) {
				assert (adr.peer_address_ID != null);
				if (DEBUG) System.out.println("D_PeerInstance: storeAddresses shared orig ="+adr.toLongString());
				Address pi = this.getPeerAddress(adr.peer_address_ID, shared_addresses, adr);
				if (pi == null) {
					Application.db.delete(table.peer_address.TNAME,
							new String[]{table.peer_address.peer_address_ID},
							new String[]{adr.peer_address_ID}, DEBUG);
				}else
					if (DEBUG) System.out.println("D_PeerInstance: storeAddresses shared new ="+adr.toLongString());
			}
			// save extra addresses
			for (Address adr : shared_addresses) {
				if (DEBUG) System.out.println("D_PeerInstance: storeAddresses shared new one ="+adr.toLongString());
				if (adr.dirty || (adr.peer_address_ID == null)) {
					// next two lines catch a different protocol.. probably redundant
					//Address pi = this.getPeerAddress(adr.domain, adr.tcp_port, adr.udp_port, shared_addresses_orig);
					//if (pi != null) adr.peer_address_ID = pi.peer_address_ID;
					adr.dirty = false;
					adr.store(peer_ID, null);
					if (DEBUG) System.out.println("D_PeerInstance: storeAddresses shared new saved ="+adr.toLongString());
				}
			}
			shared_addresses_orig = (ArrayList<Address>) shared_addresses.clone();
			// remove addresses of discarded instances
			for (D_PeerInstance i : _instances_orig.values()) {
				if (DEBUG) System.out.println("D_PeerInstance: storeAddresses inst orig ="+i);
				D_PeerInstance pi = this.getPeerInstance(i.peer_instance);
				if (pi == null) {
					Application.db.delete(table.peer_address.TNAME,
							new String[]{table.peer_address.instance},
							new String[]{i.get_peer_instance_ID()}, DEBUG);
				}else{
					if (DEBUG) System.out.println("D_PeerInstance: storeAddresses inst new ="+pi);
				}
			}
			
			// updated addresses of instances
			for (D_PeerInstance i : _instances.values()) {
				purgeExtraAddresses(i, i.addresses);
				if (DEBUG) System.out.println("D_PeerInstance: storeAddresses storing from instance: "+i);
				for (Address adr : i.addresses_orig) {
					assert (adr.peer_address_ID != null);
					if (DEBUG) System.out.println("D_PeerInstance: storeAddresses i-adr orig ="+adr.toLongString());
					Address pi = this.getPeerAddress(adr.peer_address_ID, i.addresses, adr);
					if (pi == null) {
						Application.db.delete(table.peer_address.TNAME,
								new String[]{table.peer_address.peer_address_ID},
								new String[]{adr.peer_address_ID}, DEBUG);
					} else {
						if (DEBUG) System.out.println("D_PeerInstance: storeAddresses i-adr new ="+adr.toLongString());
					}
				}
				for (Address adr : i.addresses) {
					if (DEBUG) System.out.println("D_PeerInstance: storeAddresses i-adr new save? ="+adr.toLongString());
					if (adr.dirty || (adr.peer_address_ID == null)) {
						// next three lines catch a different protocol.. probably redundant
						//Address pi = this.getPeerAddress(adr.domain, adr.tcp_port, adr.udp_port, i.addresses_orig);
						//if (DEBUG) System.out.println("D_PeerInstance: storeAddresses i-adr new save? ="+pi);
						//if (pi != null) adr.peer_address_ID = pi.peer_address_ID;
						adr.dirty = false;
						adr.store(peer_ID, i.get_peer_instance_ID());
						if (DEBUG) System.out.println("D_PeerInstance: storeAddresses i-adr was dirty: "+adr.toLongString());
					}else{
						if (DEBUG) System.out.println("D_PeerInstance: storeAddresses i-adr not dirty: "+adr.toLongString());
					}
				}
				i.addresses_orig = (ArrayList<Address>) i.addresses.clone();
			}
		}
		if (DEBUG) System.out.println("D_PeerInstance: storeAddresses end");
	}
	/**
	 * Remove uncertified domains (if any certified exists). Anyhow, do not keep more that DD.MAX_UNCERT=5
	 * @param i :  if nonull, then do not keep uncertified
	 * @param addresses
	 */
	private void purgeExtraAddresses(D_PeerInstance i, ArrayList<Address> addresses) {
		HashSet<Address> h = new HashSet<Address>();
		int MAX_UNCERT = DD.MAX_DPEER_UNCERTIFIED_ADDRESSES;
		int certified = 0;
		int directories_uncertified = 0, keep_dirs = 0;
		int sockets_uncertified = 0, keep_socks = 0;
		for (Address a : addresses) {
			if (a.certified) { certified ++; continue;}
			if (Address.DIR.equals(a.getPureProtocol())) { directories_uncertified ++; continue;}
			if (Address.SOCKET.equals(a.getPureProtocol())) { sockets_uncertified ++; continue;}
			h.add(a);
		}
		if ((certified > 0) || (i != null)) MAX_UNCERT = 0;
		keep_dirs = Math.min(MAX_UNCERT, directories_uncertified);
		keep_socks = Math.min(sockets_uncertified, MAX_UNCERT - keep_dirs);
		for (Address a : addresses) {
			if (a.certified) { certified ++; continue;}
			if (Address.DIR.equals(a.getPureProtocol())) {
				if (keep_dirs > 0) keep_dirs --; else h.add(a);
				continue;
			}
			if (Address.SOCKET.equals(a.getPureProtocol())) {
				sockets_uncertified ++;
				if (keep_socks > 0) keep_socks --; else h.add(a);
				continue;
			}
			h.add(a);
		}
		for (Address a : h) {
			addresses.remove(a);
		}
	}
	private Address getPeerAddress(String domain, int tcp_port, int udp_port,
			ArrayList<Address> a) {
		for (Address inst : a) {
			if (!Util.equalStrings_and_not_null(inst.domain, domain)) continue;
			if (tcp_port != inst.tcp_port) continue;
			if (udp_port != inst.udp_port) continue;
			return inst;
		}
		return null;
	}
	/**
	 * Finds in the list_a an address with the ID equal to peer_address_ID, or "prot://domain:tcp:udp" equal to adr.
	 * If found, sets the ID to peer_address_ID.
	 * @param peer_address_ID
	 * @param list_a
	 * @param old_adr
	 * @return
	 */
	private Address getPeerAddress(String peer_address_ID, ArrayList<Address> list_a, Address old_adr) {
		for (Address crt : list_a) {
			if (Util.equalStrings_and_not_null(crt.peer_address_ID, peer_address_ID)) {
				checkDirtyAddress(crt, old_adr);
				return crt;
			}
			if (crt.peer_address_ID != null) continue;
			if (Util.equalStrings_and_not_null(crt.domain, old_adr.domain)
					&& Util.equalStrings_and_not_null(crt.pure_protocol, old_adr.pure_protocol)
					&& (crt.tcp_port == old_adr.tcp_port)
					&& (crt.udp_port == old_adr.udp_port)) {
				crt.peer_address_ID = peer_address_ID;
				checkDirtyAddress(crt, old_adr);
				return crt;
			}
		}
		return null;
	}
	static private void checkDirtyAddress(Address crt, Address old_adr) {
		if (crt.certified != old_adr.certified) crt.dirty = true;
		if (crt.certified && (old_adr.priority != crt.priority)) crt.dirty = true; 
		if (!Util.equalStrings_and_not_null(crt.branch, old_adr.branch)
				|| !Util.equalStrings_and_not_null(crt.agent_version, old_adr.agent_version)
				|| !Util.equalStrings_and_not_null(crt.name, old_adr.name)
				|| !Util.equalStrings_and_not_null(crt.last_contact, old_adr.last_contact)
				|| !Util.equalStrings_and_not_null(crt.arrival_date, old_adr.arrival_date)
				|| (crt.active != old_adr.active)
				|| (crt.version_structure != old_adr.version_structure)
				) crt.dirty = true;
	}
	private void storeMyPeerData() throws P2PDDSQLException {
		if (DEBUG) System.out.println("\n\n***********\nD_Peer: storeMyPeerData: "+this);
		String params[];
		if (this.component_preferences.my_ID == null) params = new String[table.peer_my_data.FIELDS_NOID];
		else params = new String[table.peer.PEER_COL_FIELDS];
		synchronized (this.dirty_my_data_monitor) {
			if (!this.dirty_my_data) {
				if (DEBUG) System.out.println("\n\n***********\nD_Peer: storeMyPeerData: not dirty");
				return;
			}
			this.dirty_my_data = false;
			params[table.peer_my_data.COL_PEER_ID] = peer_ID;
			params[table.peer_my_data.COL_NAME] = component_preferences.my_name;
			params[table.peer_my_data.COL_SLOGAN] = component_preferences.my_slogan;
			params[table.peer_my_data.COL_PICTURE] = component_preferences.my_picture;
			params[table.peer_my_data.COL_TOPIC] = component_preferences.my_topic;
			params[table.peer_my_data.COL_BROADCASTABLE] = component_preferences.my_broadcastable_obj;

			if (this.component_preferences.my_ID == null) {
				long id = Application.db.insert(table.peer_my_data.TNAME, 
						table.peer_my_data.fields_list_noID,
						params, DEBUG);
				this.component_preferences.my_ID = Util.getString(id);
			} else {
				params[table.peer_my_data.COL_ROW] = component_preferences.my_ID;

				Application.db.update(table.peer_my_data.TNAME, 
						table.peer_my_data.fields_list_noID,
						new String[]{table.peer_my_data.peer_my_data_ID},
						params, DEBUG);
			}
		}
	}

	private void storeServedOrgs() throws P2PDDSQLException {
		if (DEBUG) System.out.println("D_Peer: storeServedOrgs peer_ID="+_peer_ID);
		// if (_peer_ID == null) return;
		this.dirty_served_orgs = false;
		if (this.served_orgs_orig != null) {
			for (D_PeerOrgs p_org_orig : this.served_orgs_orig) {
				assert (p_org_orig.organization_ID > 0);
				if (DEBUG) System.out.println("D_Peer: storeServedOrgs org="+p_org_orig+" hash="+p_org_orig.getOrgGIDH());
				D_PeerOrgs pi = this.getPeerOrg(p_org_orig.organization_ID, p_org_orig.getOrgGIDH());
				if (pi == null) {
					Application.db.delete(table.peer_org.TNAME,
							new String[]{table.peer_org.peer_ID, table.peer_org.organization_ID},
							new String[]{peer_ID, Util.getString(p_org_orig.organization_ID)}, DEBUG);
				} else {
					if (DEBUG) System.out.println("D_Peer: storeServedOrgs keeping: "+p_org_orig);
				}
			}
		}
		served_orgs_orig = served_orgs.clone();
		for (D_PeerOrgs p_org : served_orgs) {
			if (p_org.dirty) {
				//inst.setLID(peer_ID, _peer_ID);
				p_org.store(this);
			} else {
				if (DEBUG) System.out.println("D_Peer: storeServedOrgs not dirty: "+p_org);
			}
		}
		
	}
	/** Searches this organization in current orgs */
	private D_PeerOrgs getPeerOrg(long organization_ID, String global_organization_IDhash) {
		if (DEBUG && (global_organization_IDhash == null)) Util.printCallPath("null orgs");
		for (int k = 0; k < served_orgs.length; k++) {
			if (DEBUG) System.out.println("D_Peer: getPeerOrg: check org "+k+"= "+served_orgs[k].toLongString());
			if (served_orgs[k].organization_ID == organization_ID)
				return served_orgs[k];
			if ((served_orgs[k].organization_ID <= 0)
					&& (global_organization_IDhash.equals(served_orgs[k].getOrgGIDH())))
				return served_orgs[k];
		}
		return null;
	}

	/** Getters Setters */
	
	public boolean dirty_any() {
		if (dirty_main) return true;
		if (dirty_addresses) return true;
		if (dirty_served_orgs) return true;
		if (dirty_instances) return true;
		if (dirty_my_data) return true;
		return false;
	}
	public boolean dirty_all() {
		if (
				(dirty_main)
				&& (dirty_addresses)
				&& (dirty_served_orgs)
				&& (dirty_instances)
				&& (dirty_my_data))
			return true;
		return false;
	}
	public void set_dirty_all(boolean dirty) {
		dirty_main = dirty;
		dirty_addresses = dirty;
		dirty_served_orgs = dirty;
		dirty_instances = dirty;
		dirty_my_data = dirty;
	}
	public D_PeerInstance getPeerInstanceOrig(String inst) {
		return _instances_orig.get(Util.getStringNonNullUnique(inst));
	}
	public D_PeerInstance getPeerInstance(String inst) {
		return _instances.get(Util.getStringNonNullUnique(inst));
	}
	public boolean containsPeerInstance(String inst) {
		return _instances.containsKey(Util.getStringNonNullUnique(inst));
	}
	public void putPeerInstance(String inst, D_PeerInstance dpi) {
		assertReferenced();
		_instances.put(Util.getStringNonNullUnique(inst), dpi);
	}
	/**
	 * Just sets the corresponding member
	 * @param peer_instance
	 */
	public void setCurrentInstance(String peer_instance) {
		this.instance = peer_instance;
		D_PeerInstance dpi = this.getPeerInstance(peer_instance);
		if (dpi == null) {
			dpi = new D_PeerInstance();
			dpi.peer_instance = peer_instance;
			this.putPeerInstance(peer_instance, dpi);
			dpi.createdLocally = true;
			dpi.dirty = true;
			this.dirty_instances = true;
		}
		if (!Util.equalStrings_null_or_not(DD.BRANCH, dpi.branch)) {
			dpi.dirty = true;
			dpi.branch = DD.BRANCH;
			this.dirty_instances = true;
		}
		if (!Util.equalStrings_null_or_not(DD.VERSION, dpi.agent_version)) {
			dpi.dirty = true;
			dpi.agent_version = DD.VERSION;
			this.dirty_instances = true;
		}
		if (dpi.dirty) {
			dpi.setCreationDate();
			dpi.sign(getSK());
		}
		//dpi.set_peer_ID(this.get_ID(), this.get_ID_long());
	}
	/**
	 * Create an instance with name given by the current date
	 */
	public void makeNewInstance() {
		assertReferenced();
		Calendar cal;
		this.loadInstancesToHash();
		do {
			cal = Util.CalendargetInstance();
			this.instance = Encoder.getGeneralizedTime(cal);//Util.getHash(enc.getBytes(), Cipher.MD5);
		} while (this.containsPeerInstance(this.instance));
		
		if (!addInstanceElem(this.instance, true)) return;
	}
	/**
	 * 
	 * @param instance2 (if null, try current time)
	 * @param createdLocally
	 * @return: returns false if it already existed
	 */
	public boolean addInstanceElem(String instance2, boolean createdLocally){
		if (instance2 == null) instance2 = Util.getGeneralizedTime();
		
		if (this.containsPeerInstance(instance2)) return false;

		D_PeerInstance nou = new D_PeerInstance(instance2);
		nou.createdLocally = createdLocally;
		nou.dirty = true;
		nou.branch = DD.BRANCH;
		nou.agent_version = DD.VERSION;
		nou.setCreationDate();
		nou.set_last_sync_date(Util.CalendargetInstance());
		nou.sign(getSK());
		//nou._last_sync_date = Encoder.getGeneralizedTime(nou.last_sync_date);

		//if (this.instances_orig == null) {this.instances_orig = D_PeerInstance.deep_clone(instances);}
		this.putPeerInstance(instance2, nou);
		this.dirty_instances = true;
		//nou.set_peer_ID(this.get_ID(), this.get_ID_long());
		return true;
	}
	public static boolean samePeers(D_Peer p1, D_Peer p2) {
		if (p1 == p2) return true;
		if ((p1 == null) || (p2 == null)) return false;
		if (p1.getGIDH() == p2.getGIDH()) return true;
		if ((p1.getGIDH() == null) || (p2.getGIDH() == null)) return false;
		if (p1.getGIDH().equals(p2.getGIDH())) return true;
		return true;
	}
	
	public void setKeys(Cipher _keys) {
		this.keys = _keys;
	}
	public void setBroadcastable(boolean val) {
		this.component_basic_data.broadcastable = val;
	}
	public boolean getBroadcastable() {
		return this.component_basic_data.broadcastable;
	}
	public boolean my_exists() {
		return (this.component_preferences.my_ID != null) || this.dirty_my_data;
	}
	public void my_init () {
		if (my_exists()) return;
		synchronized(this.component_preferences) {
			this.setBroadcastableMy(this.getBroadcastable());
		}
	}
	public static void setBroadcastableMy(D_Peer peer, boolean val) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setBroadcastableMy(val);
		peer.storeRequest();
		peer.releaseReference();
	}
	public void setBroadcastableMy(boolean val) {
		assertReferenced();
		this.component_preferences.my_broadcastable = val;
		this.component_preferences.my_broadcastable_obj = Util.bool2StringInt(val);
		this.dirty_my_data = true;
	}
	/**
	 * Should only be used with care (ingetBroadcastableMeOrDefault ) since my_broadcastable may not exist
	 * @return
	 */
	private boolean getBroadcastableMy() {
		return this.component_preferences.my_broadcastable;
	}
	public boolean getBroadcastableMyOrDefault() {
		if ((my_exists()) && (this.component_preferences.my_broadcastable_obj!=null)) return getBroadcastableMy(); 
		return getBroadcastable();
	}
	/**
	 * Set creation date to now
	 */
	public void setCreationDate() {
		assertReferenced();
		Calendar now = Util.CalendargetInstance();
		String _now = Encoder.getGeneralizedTime(now);
		setCreationDate(now, _now);
	}
	public void setCreationDate(Calendar now, String _now) {
		assertReferenced();
		this.dirty_main = true;
		if (now == null) now = Util.getCalendar(_now);
		if (_now == null) _now = Encoder.getGeneralizedTime(now);
		this.component_basic_data.creation_date = now;
		this.component_basic_data.creation_date_str = _now;
	}
	public String getCreationDate() {
		return this.component_basic_data.creation_date_str;
	}

	public void setArrivalDate() {
		assertReferenced();
		Calendar now = Util.CalendargetInstance();
		String _now = Encoder.getGeneralizedTime(now);
		setArrivalDate(now, _now);
	}
	/**
	 * Sets dirty_main since arrival_date is currently stored in main peer table
	 * @param now
	 * @param _now
	 */
	public void setArrivalDate(Calendar now, String _now) {
		assertReferenced();
		this.dirty_main = true;
		if (now == null) now = Util.getCalendar(_now);
		if (_now == null) _now = Encoder.getGeneralizedTime(now);
		this.component_local_agent_state.arrival_date = now;
		this.component_local_agent_state.arrival_date_str = _now;
	}
	public String getArrivalDate() {
		return this.component_local_agent_state.arrival_date_str;
	}
	public String getGID() {
		if(this.component_basic_data == null) return null;
		return this.component_basic_data.globalID;
	}

	public String getGIDH() {
		if (this.component_basic_data == null) return null;
		return this.component_basic_data.globalIDhash;
	}
	/**
	 * Adds a "P:" in front f the hash of the GID
	 * @param GID
	 * @return
	 */
	public static String getGIDHashFromGID(String GID) {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Peer:getGIDHashFromGID from GID = "+GID);
		String hash = Util.getGIDhash(GID);
		if (DEBUG) System.out.println("D_Peer:getGIDHashFromGID got hash="+hash);
		if (hash == null) return null;
		String gidh =  getGIDHashFromHash(hash);
		if (DEBUG) System.out.println("D_Peer:getGIDHashFromGID got gidh="+gidh);
		return gidh;
	}
	/**
	 * Adds a "P:" in front f the pGIDhash
	 * @param GID
	 * @return
	 */
	public static String getGIDHashFromHash(String pGIDhash) {
		if (pGIDhash.startsWith(D_GIDH.d_Peer)) return pGIDhash;
		return D_GIDH.d_Peer+pGIDhash;
	}
	/**
	 * if GIDH is null, computes it from GID
	 * @param gID
	 * @param gIDH
	 */
	/**
	 * Checks if there is a P: in front, else generates a GIDhash with getGIDHashFromGID
	 */
	public static String getGIDHashGuess(String s) {
		if (s.startsWith(D_GIDH.d_Peer)) return s; // it is an external
		String hash = D_Peer.getGIDHashFromGID(s);
		if (hash.length() != s.length()) return hash;
		return s;
	}
	public void setGID(String gID, String gIDH) {
		this.component_basic_data.globalID = gID;
		if ((gID != null) && (gIDH == null) && !D_GIDH.isCompactedGID(gID)) {
			gIDH = D_Peer.getGIDHashFromGID(gID);
			if (gIDH == null) Util.printCallPath("D_Peer: setGID:"+gID+" for: "+this);
		}
		this.component_basic_data.globalIDhash = gIDH;
	}
	
	public void setPeerInputNoCiphersuit(PeerInput data) {
		setEmail(data.email);
		setSlogan(data.slogan);
		setName(data.name);
		this.setCurrentInstance(data.instance);
	}
	public CipherSuit getCipherSuite() {
		PK pk = Cipher.getPK(getGID());
		return Cipher.getCipherSuite(pk);
	}
	public String getName() {
		return this.component_basic_data.name;
	}
	public void setName(String _name) {
		assertReferenced();
		this.component_basic_data.name = _name;
	}
	public String getSlogan() {
		return this.component_basic_data.slogan;
	}
	public static void setSlogan(D_Peer peer, String _slogan) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setSlogan(_slogan);
		peer.storeRequest();
		peer.releaseReference();
	}
	public void setSlogan(String _slogan) {
		assertReferenced();
		this.component_basic_data.slogan = _slogan;
		this.dirty_main = true;
	}
	public String getEmail() {
		return this.component_basic_data.emails;
	}
	public static void setEmail(D_Peer peer, String _email) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setEmail(_email);
		peer.storeRequest();
		peer.releaseReference();
	}
	public void setEmail(String _email) {
		assertReferenced();
		this.component_basic_data.emails = _email;
		this.dirty_main = true;
	}
	public String get_ID() {
		if ((this.peer_ID == null) && (this._peer_ID > 0))
			this.peer_ID = Util.getStringID(_peer_ID);
		if ((this.peer_ID == null) && (this.dirty_any())) {
			this.storeSynchronouslyNoException();
			return this.peer_ID;
		}
		return this.peer_ID;
	}
	public long get_ID_long() {
		String ID = get_ID();
		return Util.lval(ID);
	}
	public void set_ID(String __peer_ID) {
		this.peer_ID = __peer_ID;
		this._peer_ID = Util.lval(__peer_ID, -1);
	}
	public void set_ID(long __peer_ID) {
		this.peer_ID = Util.getStringID(__peer_ID);
		this._peer_ID = __peer_ID;
	}
	/**
	 * Loads instances if needed,
	 * @param instance2 : the name should have been provided
	 * @param createdLocally
	 * @return : returns false if it already existed
	 */
	public boolean addInstance(String instance2, boolean createdLocally) {
		//this.loadInstances();
		if (!addInstanceElem(instance2, createdLocally)) return false;
		this.storeRequest();
		return true;
	}
	public String getInstance() {
		return instance;
	}
	public void deleteInstance(String peer_instance) {
		//this.loadInstances();
		this._instances.remove(peer_instance);
		this.dirty_instances = true;
		this.storeRequest();
	}
	public static byte getASN1Type() {
		return TAG;
	}
	public static String DB_getPeerGIDforID(String peerID) throws P2PDDSQLException {
		if(DEBUG)System.out.println("DD_PeerAddress: getPeerGIDforID");
		String sql = "SELECT "+table.peer.global_peer_ID+" FROM "+table.peer.TNAME+
		" WHERE "+table.peer.peer_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{peerID}, DEBUG);
		if(o.size()==0) return null;
		String result = Util.getString(o.get(0).get(0));
		if(DEBUG)System.out.println("DD_PeerAddress: getPeerGIDforID: result = "+result);
		return result;
	}
	/**
	 * return the ID of the instance
	 * @return
	 */
	public long getInstanceID() {
		if (instance == null) return -1;
		return getInstanceID(instance);
	}
	public long getInstanceID(String inst) {
		return Util.lval(this.getPeerInstance(inst).get_peer_instance_ID(), -1);
	}
	//PK stored_pk = null;
	public PK getPK() {
		//if (stored_pk != null) return stored_pk;
		//SK sk = getSK();
		if (keys != null) return keys.getPK();
		PK pk = ciphersuits.Cipher.getPK(component_basic_data.globalID);
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
		
		sk = Util.getStoredSK(gID, this.getGIDH());
		if (sk == null) return null;
		keys = Cipher.getCipher(sk, pk);
		//if (keys == null) return sk;
		return sk;
	}
	/**
	 * TODO
	 * Have to load the served_orgs GIDs
	 */
	private void loadGlobals() {
		if(loaded_globals) return;
		if(!this.loaded_served_orgs) loadServedOrgsLocals();
		// TODO load globals
		Util.printCallPath("Why?");
		loaded_globals = true;
	}
	/**
	 * TODO
	 */
	
	private void loadServedOrgsLocals() {
		// TODO Auto-generated method stub
		Util.printCallPath("Why?");
		
	}
	public static int isGIDavailable(String gID, boolean DBG) throws P2PDDSQLException {
		D_Peer p = D_Peer.getPeerByGID(gID, true, false, null);
		boolean result = true;
		if (p == null) result = false;
		if (DEBUG||DBG) System.out.println("D_News:available: "+gID+" in "+" = "+result);
		if (p.isSignatureAvailable()) return 1;
		return -1;
	}
	private boolean isSignatureAvailable() {
		return (this.getSignature() != null) && (this.getSignature().length > 0);
	}
	/**
	 * Load this message in the storage cache node.
	 * Should be called each time I load a node and when I sign myself.
	 */
	private void reloadMessage() {
		if (this.loaded_globals) this.component_node.message = this.encode(); //crt.encoder.getBytes();
	}

	/**
	 * version, globalID, name, slogan, creation_date, address*, broadcastable, signature_alg, served_orgs, signature*
	 * D_Peer ::= SEQUENCE {
	 * version PrintableString,
	 * globalID PrintableString,
	 * name UTF8String OPTIONAL,
	 * slogan [AC0] IMPLICIT UTF8String OPTIONAL,
	 * emails [AC1] IMPLICIT UTF8String OPTIONAL,
	 * phones [AC2] IMPLICIT UTF8String OPTIONAL,
	 * instance [AC3] IMPLICIT UTF8String OPTIONAL,
	 * creation_date GeneralizedTime OPTIONAL,
	 * address SEQUENCE OF Address OPTIONAL,
	 * broadcastable BOOLEAN,
	 * signature_alg SEQUENCE OF PrintableString OPTIONAL,
	 * served_orgs [AC12] SEQUENCE OF D_PeerOrgs [1..] OPTIONAL
	 * signature NULLOCTETSTRING
	 * }
	 */
	@Override
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(component_basic_data.version,false));
		String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, component_basic_data.globalID);
		enc.addToSequence(new Encoder(repl_GID).setASN1Type(Encoder.TAG_PrintableString));
		if (component_basic_data.name != null)enc.addToSequence(new Encoder(component_basic_data.name,Encoder.TAG_UTF8String));
		if (component_basic_data.slogan != null)enc.addToSequence(new Encoder(component_basic_data.slogan,DD.TAG_AC0));
		if (component_basic_data.emails != null)enc.addToSequence(new Encoder(component_basic_data.emails,DD.TAG_AC1));
		if (component_basic_data.phones != null)enc.addToSequence(new Encoder(component_basic_data.phones,DD.TAG_AC2));
		if (instance != null)enc.addToSequence(new Encoder(instance,DD.TAG_AC3));
		if (component_basic_data.creation_date != null)enc.addToSequence(new Encoder(component_basic_data.creation_date));
		if (this.shared_addresses != null) {
			if (Util.Ival(this.component_basic_data.version) <= Util.Ival(DD_Address.V2))
				for (Address sa : shared_addresses) sa.set_version_structure(Address.V0); 
			enc.addToSequence(Encoder.getEncoder(shared_addresses));
		}
		enc.addToSequence(new Encoder(component_basic_data.broadcastable));
		if (Util.Ival(this.component_basic_data.version) <= Util.Ival(DD_Address.V2))
			if (signature_alg != null)enc.addToSequence(Encoder.getStringEncoder(signature_alg, Encoder.TAG_PrintableString));
		if ((served_orgs != null) && (served_orgs.length > 0))
			enc.addToSequence(Encoder.getEncoder(this.served_orgs, dictionary_GIDs).setASN1Type(DD.TAG_AC12));
		enc.addToSequence(new Encoder(getSignature()));
		enc.setASN1Type(TAG);
		return enc;
	}
	@Override
	public D_Peer decode(Decoder dec) throws ASN1DecoderFail {
		String instance; // incoming instances should not change current instance here
		Decoder content = dec.getContent();
		component_basic_data.version=content.getFirstObject(true).getString();
		String globalID = content.getFirstObject(true).getString();
		//this.component_basic_data.globalID = globalID;
		this.setGID(globalID, null); // do not attempt to set the GIDH since the GIDs may be compacted (into indexes) and this would fail
		if(content.getTypeByte() == Encoder.TAG_UTF8String)component_basic_data.name = content.getFirstObject(true).getString();else component_basic_data.name=null;
		if(content.getTypeByte() == DD.TAG_AC0)component_basic_data.slogan = content.getFirstObject(true).getString();else component_basic_data.slogan = null;
		if(content.getTypeByte() == DD.TAG_AC1)component_basic_data.emails = content.getFirstObject(true).getString();else component_basic_data.emails = null;
		if(content.getTypeByte() == DD.TAG_AC2)component_basic_data.phones = content.getFirstObject(true).getString();else component_basic_data.phones = null;
		if(content.getTypeByte() == DD.TAG_AC3) {
			instance = content.getFirstObject(true).getString();
			//if (instances == null) instances = new Hashtable<String, D_PeerInstance>();
			if (!this.containsPeerInstance(instance)) {
				D_PeerInstance inst = new D_PeerInstance(instance);
				this.putPeerInstance(instance, inst);
				this.dirty_instances = true;
			}
		} else {
			instance = null;
		}
//		try {
//			instances = data.D_PeerInstance.loadInstancesToHash(peer_ID);
//			this.loaded_instances = true;
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
		
		if(content.getTypeByte() == Encoder.TAG_GeneralizedTime)
			component_basic_data.creation_date = content.getFirstObject(true).getGeneralizedTimeCalenderAnyType();
		else component_basic_data.creation_date = null;
		if(content.getTypeByte() == Encoder.TYPE_SEQUENCE)
			shared_addresses = content.getFirstObject(true)
			.getSequenceOfAL(Encoder.TYPE_SEQUENCE,
				new Address());
		else shared_addresses=null;
		//if(content.getTypeByte() == Encoder.TAG_BOOLEAN)
		component_basic_data.broadcastable = content.getFirstObject(true).getBoolean();
		if(content.getTypeByte() == Encoder.TYPE_SEQUENCE)
			signature_alg=content.getFirstObject(true)
			.getSequenceOf(Encoder.TAG_PrintableString);
		if (content.getTypeByte() == DD.TAG_AC12) {
			served_orgs = content.getFirstObject(true)
			.getSequenceOf(Encoder.TYPE_SEQUENCE,
					new D_PeerOrgs[]{}, new D_PeerOrgs());
		} else {
			served_orgs = new D_PeerOrgs[0];
		}
		if(content.getTypeByte() == Encoder.TAG_OCTET_STRING) 
			setSignature(content.getFirstObject(true).getBytes());
		if (DEBUG){
			System.out.println("D_Peer:decode: name="+getName()+" e="+getEmail()+" sign="+getSignature());
			Util.printCallPath("");
		}
		return this;
	}
	public static boolean verify(DD_Address addr) {
		//boolean DEBUG = true;
		if (DEBUG) System.err.println("D_Peer:DDAddress: verify: start ");
		if ((addr.signature == null) || (addr.signature.length == 0)) {
			if (_DEBUG) System.err.println("D_Peer:DDAddress: verify: exit: empty signature ");
			return false;
		}
		//byte[] sign = signature;
		//signature = new byte[0];
		D_Peer pa;
		if (addr.V2.equals(addr.version))
			pa = new D_Peer(addr,true);
		else //V0,V1
			if ((addr.V1.equals(addr.version)) || (addr.V0.equals(addr.version)))
				pa = new D_Peer(addr,false);
			else {
				Util.printCallPath("Unknown address version:"+addr.version+"\naddr="+addr);
				return false;
			}
		//byte[] msg = getBytes();
		//if(DEBUG)System.err.println("D_Peer:DDAddress: verify: msg=["+msg.length+"]"+Util.byteToHex(msg, ":"));
		//signature = sign;
		if(DEBUG)System.err.println("D_Peer:DDAddress: verify: will get PK from "+addr.globalID);
		//PK senderPK = ciphersuits.Cipher.getPK(this.globalID);
		if(DEBUG)System.err.println("D_Peer:DDAddress: verify: will verify signature of pa="+pa);
		if(DEBUG)System.err.println("D_Peer:DDAddress: verify: sign=["+addr.signature.length+"]"+Util.byteToHex(addr.signature, ":"));
		//return Util.verifySign(pa, senderPK, sign);
		boolean result = pa.verifySignature();
		if (DEBUG) System.err.println("D_Peer:DDAddress: verify: got = " + result);
		return result;
	}
	public static void loadPeer(DD_Address dda) {
		// boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Peer:loadPeer: from "+dda);
		D_Peer p = D_Peer.getPeerByGID_NoStore(dda.globalID, true, true, null);
		if (DEBUG) System.out.println("D_Peer:loadPeer:  into "+p+"\n dirty: "+p.dirty_any());
		p.fillFromDDAddress(dda, true);
		if (DEBUG) System.out.println("D_Peer:loadPeer:  got "+p);
		p.storeRequest();
	}
	/**
	 * Does not call storeRequest (only loads for tests, not to save).
	 * 
	 * @param dd
	 * @param encode_addresses
	 */
	private D_Peer(DD_Address dd, boolean encode_addresses) {
		if (DEBUG) System.out.println("D_Peer:<init>: "+dd.address);
		this.setGID(dd.globalID, null);
		fillFromDDAddress(dd, encode_addresses);
	}
	/**
	 * Expects list of addresses, each of type
	 *   "<pure[%branch[%version]]>:<domain>:<tcp>:<udp>:<name>[:<active_int_boolean>:...][/<priority>]"
	 * unlike the other version:
	 *   "[<pure[%branch[%version]]>://]<domain>:<tcp>:<udp>:<name>[:<active_int_boolean>:...]"
	 * Separated by DirectoryServer.ADDR_SEP=","
	 * Inner separators: PROT_SEP="%",  ADDR_PROT_INTERN_SEP="://", ADDR_PART_SEP=":", PRI_SEP="/",

	 * Does not call storeRequest.
	 * @param dd
	 * @param encode_addresses
	 */
	private void fillFromDDAddress(DD_Address dd, boolean encode_addresses) {
		//boolean DEBUG = true;
		component_basic_data.version = dd.version;
		if (!encode_addresses && DD_Address.V2.equals(component_basic_data.version))
			Util.printCallPath("Here we thought that no address is needed");
		component_basic_data.globalID = dd.globalID;
		component_basic_data.name = dd.name;
		component_basic_data.slogan = dd.slogan;
		component_basic_data.emails = dd.emails;
		component_basic_data.phones = dd.phones;
		component_basic_data.creation_date = Util.getCalendar(dd.creation_date);
		component_basic_data.picture = dd.picture;
		if (encode_addresses) {
			String[]addresses_l = Address.split(dd.address);
			if (DEBUG) System.out.println("D_Peer:fillFromDDAddress: "+dd.address+" splits="+Util.concat(addresses_l," -- "));
			
			// may have to control addresses, to defend from DOS based on false addresses
			shared_addresses = new ArrayList<Address>(addresses_l.length);
			for (int k = 0; k < addresses_l.length; k++) {
				//Address a = new Address(); //addresses_l[k]); // cannot construct from this string!
				String taddr[] = addresses_l[k].split(Pattern.quote(Address.ADDR_PART_SEP),Address.COMPONENTS);
				if (taddr.length < 2) continue;
				String adr[] = taddr[1].split(Pattern.quote(Address.PRI_SEP), 2);
				//Address a = new Address(taddr[0] + Address.ADDR_PROT_INTERN_SEP + adr[0]);
				Address a = new Address(adr[0]);
				a.set_version_structure(Address.V0);
				a.pure_protocol = taddr[0];
				//a.address = adr[0];
				
				if (adr.length > 1) {
					a.certified = true;
					try {
						a.priority = Integer.parseInt(adr[1]);
					}catch(Exception e){e.printStackTrace();}
				}
				shared_addresses.add(a);
				a.dirty = true;
			}
		}
		component_basic_data.broadcastable = dd.broadcastable;
		signature_alg = dd.hash_alg;
		component_basic_data.hash_alg = D_Peer.D_Peer_Basic_Data.getStringFromHashAlg(signature_alg);
		setSignature(dd.signature);
		served_orgs = dd.served_orgs;
		markDirtyOrgs();
		this.dirty_main = true;
		this.dirty_addresses = true;
		this.dirty_served_orgs = true;
	}
	/**
	 * Set all orgs to dirty. then clean those found on _orig
	 */
	public void markDirtyOrgs() {
		if (served_orgs != null) {
			for (D_PeerOrgs i : served_orgs) {
				i.dirty = true;
				if (served_orgs_orig != null)
					for (D_PeerOrgs j : served_orgs_orig) {
						if (
								((i.organization_ID == j.organization_ID) && i.organization_ID > 0)
								||								
								Util.equalStrings_and_not_null(i.getOrgGIDH(), j.getOrgGIDH())
								||
								Util.equalStrings_and_not_null(i.global_organization_ID, j.global_organization_ID)
								) {
							i.dirty = false;
							continue;
						}
							
					}
			}
		}
	}
	/**
	 * update signature
	 * @param peer_ID should exist
	 * @param signer_ID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_Peer readSignSave(long peer_ID, long signer_ID, boolean trim) throws P2PDDSQLException {
		D_Peer w = D_Peer.getPeerByLID(peer_ID, true);
		//if (trim) w.address = TypedAddress.trimTypedAddresses(w.address, 2);
		ciphersuits.SK sk = w.getSK();
		w.setCreationDate();
		w.sign(sk);
		w.storeRequest();
		return w;
	}
	public static boolean checkValid(long peerID) {
		D_Peer m = null;
		try {
			m = D_Peer.getPeerByLID(peerID, true);
			if (m == null) return false;
			return m.verifySignature();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("D_Peer:checkValid: peer="+m);
		}
		return false;
	}
	/**
	 * Verifying with SK=this.globalID
	 * @return
	 */
	public boolean verifySignature() {
		//boolean DEBUG = true;
		if (DEBUG) System.err.println("Peer: verifySignature: start: peer_addr="
				+ "\n>>>>\n"+this+"\n<<<<\n");
		PK c;
		c = this.getPK();
		//c = ciphersuits.Cipher.getPK(component_basic_data.globalID);
		if (c == null) {
			System.err.println("\n\nPeer: verifySignature: no PK for "+this);
			Util.printCallPath("");
			return false;
		}
		if (DEBUG) System.err.println("Peer: verifySignature: pk="+c+"\n\n");
		boolean result = verifySignature(c);
		if (!result) {
			System.err.println("\n\nPeer: verifySignature: got="+result+" for "+this);
			Util.printCallPath("");
		}
		if (DEBUG) System.err.println("Peer: verifySignature: got="+result+"\n\n");
		return result;
	}
	
	public boolean verifySignature(ciphersuits.PK pk) {
		signature_verified = true;
		//boolean DEBUG = true;
		if(DEBUG)System.err.println("Peer: verifySignature(pk): start this="
				+ "\n>>>>\n"+this+"\n<<<<\n");
		boolean result = false;
		if(DEBUG)System.err.println("Peer: verifySignature:" +
				"\n\t sign=["+getSignature().length+"]"+Util.byteToHex(getSignature(), "")+
				"\n\t hash(sign)="+Util.getGID_as_Hash(getSignature()));

		byte[] _sgn = getSignature();
		if ((_sgn == null) || (_sgn.length == 0)) {
			if (_DEBUG) System.out.println("D_Peer:verifySignature: null signature for: "+getName()+" e="+this.getEmail());
			// Util.printCallPath("");
			Util.printCallPath("");
			if (DEBUG) System.out.println("\nD_Peer:verifySignature: cache=: \n"+D_Peer.dumpDirCache());
			return last_signature_verified_successful = false;
		}
		// avoid changes to the object, since the object is shared and it can create conflicts somewhere
		//ArrayList<Address> addr = this.shared_addresses; 
		//shared_addresses = Address.getOrderedCertifiedTypedAddresses(shared_addresses);
		
		byte msg[] =  this.getSignatureEncoder().getBytes();
		if (DEBUG) System.out.println("Peer: verifySignature(pk): Will check: this" +
				"\n\t msg["+msg.length+"]="+Util.byteToHex(msg)+
				"\n\t hash(msg)="+Util.getGID_as_Hash(msg));
		try {
			result = Util.verifySign(msg, pk, _sgn);
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("D_Peer:verifySignature: "+this+"\n sgn="+Util.concat(_sgn, "", "NULL"));
			last_signature_verified_successful = false;
		}
		if (result == false) { if (_DEBUG || DD.DEBUG_TODO) System.out.println("Peer:verifySignature(pk): Failed verifying: "+this+
				"\n sgn="+Util.byteToHexDump(_sgn,":")+Util.getGID_as_Hash(_sgn)+
				"\n msg="+Util.byteToHexDump(msg,":")+Util.getGID_as_Hash(msg)+
				"\n pk="+pk
				);
		} else { if (DEBUG) System.out.println("Peer:verifySignature(pk):Success verifying: "+this+
				"\n sgn="+Util.byteToHexDump(_sgn,":")+"\n\t hash(sgn)="+Util.getGID_as_Hash(_sgn)+
				"\n msg="+Util.byteToHexDump(msg,":")+"\n\t hash(msg)="+Util.getGID_as_Hash(msg)+
				"\n pk="+pk
				);
		}
		//if (DEBUG) System.out.println("D_Peer:verifySignature: restaured sgn="+Util.concat(component_basic_data._signature, "", "NULL"));
		//shared_addresses = addr;
		return last_signature_verified_successful = result;
	}
	/**
	 * Usable to show how e is encoded and its impact on enc, with an explanation in s
	 * @param s
	 * @param e
	 * @param enc
	 */
	public void dumpEncoderStep(String s, Encoder e, Encoder enc) {
		System.out.println(s+" e="+Util.hashEncoder(e)+" enc="+Util.hashEncoder(enc));
	}
	boolean _UPGRADE_PEER_VERSION_MSG = false;
	public Encoder getSignatureEncoder() {
		if(DEBUG) System.out.println("D_Peer: getSignatureEncoder: start this="
				+ "\n>>>>\n"+this+"\n<<<<\n");
		if(DD_Address.V0.equals(component_basic_data.version)) return getSignatureEncoder_V0();
		if(DD_Address.V1.equals(component_basic_data.version)) return getSignatureEncoder_V1();
		if(DD_Address.V2.equals(component_basic_data.version)) return getSignatureEncoder_V2();
		if(DD_Address.V3.equals(component_basic_data.version)) return getSignatureEncoder_V3();
		if(_DEBUG) System.out.println("D_Peer: getSignatureEncoder: default version");
		if (!_UPGRADE_PEER_VERSION_MSG) {
			Application_GUI.warning(_("Handling an unknown peer version:")+
					component_basic_data.version, _("May have to upgrade!"));
			_UPGRADE_PEER_VERSION_MSG = true;
			Util.printCallPath("Unknown version");
		}
		throw new RuntimeException("Unknown version");
		//return getSignatureEncoder_V0();
	}
	/**
	 *  Avoid modification in the object, since it is shared
	 */
	public Encoder getSignatureEncoder_V0(){
		if(DEBUG) System.out.println("D_Peer: getSignatureEncoder_V0: start this=");
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(component_basic_data.version, false));
		enc.addToSequence(new Encoder(component_basic_data.globalID).setASN1Type(Encoder.TAG_PrintableString));
		if (component_basic_data.name != null) enc.addToSequence(new Encoder(component_basic_data.name, Encoder.TAG_UTF8String));
		if (component_basic_data.slogan != null) enc.addToSequence(new Encoder(component_basic_data.slogan, DD.TAG_AC0));
		if (component_basic_data.emails != null) enc.addToSequence(new Encoder(component_basic_data.emails, DD.TAG_AC1));
		if (component_basic_data.phones != null) enc.addToSequence(new Encoder(component_basic_data.phones, DD.TAG_AC2));
		if (instance != null) enc.addToSequence(new Encoder(instance, DD.TAG_AC3));
		if (component_basic_data.creation_date != null) enc.addToSequence(new Encoder(component_basic_data.creation_date));
		/*
		if (this.shared_addresses != null) {
			ArrayList<Address> _shared_addresses = Address.getOrderedCertifiedTypedAddresses(shared_addresses);
			if (Util.Ival(this.component_basic_data.version) <= Util.Ival(DDAddress.V2))
				for (Address sa : _shared_addresses) sa.set_version_structure(Address.V0); 
			enc.addToSequence(Encoder.getEncoder(_shared_addresses));
		}
		*/
		enc.addToSequence(new Encoder(component_basic_data.broadcastable));
		if (Util.Ival(this.component_basic_data.version) <= Util.Ival(DD_Address.V2))
			if (signature_alg != null)enc.addToSequence(Encoder.getStringEncoder(signature_alg, Encoder.TAG_PrintableString));
		if ((served_orgs != null) && (served_orgs.length > 0)) enc.addToSequence(Encoder.getEncoder(this.served_orgs).setASN1Type(DD.TAG_AC12));
		enc.addToSequence(new Encoder(new byte[0])); // getSignature()
		enc.setASN1Type(TAG);
		return enc;
	}

	private Encoder getSignatureEncoder_V1() {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V1: start");
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(component_basic_data.version,false));
		if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V1: ver="+component_basic_data.version);
		enc.addToSequence(new Encoder(component_basic_data.globalID).setASN1Type(Encoder.TAG_PrintableString));
		 	if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V1: GID="+component_basic_data.globalID);
		if (component_basic_data.name!=null) {enc.addToSequence(new Encoder(component_basic_data.name,Encoder.TAG_UTF8String));
		 	if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V1: name="+component_basic_data.name);}
		if (component_basic_data.slogan!=null) {enc.addToSequence(new Encoder(component_basic_data.slogan,DD.TAG_AC0));
			if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V1: slog="+component_basic_data.slogan);}
		if (component_basic_data.emails!=null) {enc.addToSequence(new Encoder(component_basic_data.emails,DD.TAG_AC1));
			if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V1: email="+component_basic_data.emails);}
		if (component_basic_data.phones!=null) {enc.addToSequence(new Encoder(component_basic_data.phones,DD.TAG_AC2));
			if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V1: phone="+component_basic_data.phones);}
		//if(instance!=null)enc.addToSequence(new Encoder(instance,DD.TAG_AC3));
		if (component_basic_data.creation_date!=null){enc.addToSequence(new Encoder(component_basic_data.creation_date));
			if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V1: cdate="+component_basic_data.creation_date);}
		/*
		if (shared_addresses != null) {
			ArrayList<Address> _shared_addresses = Address.getOrderedCertifiedTypedAddresses(shared_addresses);
			//TypedAddress[] address = getTypedAddress();
			enc.addToSequence(Encoder.getEncoder(_shared_addresses));
			if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V1: adr="+Util.concat(_shared_addresses,"---","NULL"));
		}
		*/
		enc.addToSequence(new Encoder(component_basic_data.broadcastable));
		if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V1: broadc="+component_basic_data.broadcastable);
		//if(signature_alg!=null)enc.addToSequence(Encoder.getStringEncoder(signature_alg, Encoder.TAG_PrintableString));
		if ((served_orgs != null) && (served_orgs.length > 0)) {
			D_PeerOrgs[] old = served_orgs;
			served_orgs = makeSignaturePeerOrgs_VI(served_orgs);
			enc.addToSequence(Encoder.getEncoder(this.served_orgs).setASN1Type(DD.TAG_AC12));
			if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V1: orgs="+Util.concat(served_orgs,"---"));
			served_orgs = old;
		}
		//enc.addToSequence(new Encoder(signature));
		//enc.setASN1Type(TAG);
		return enc;
	}
	private Encoder getSignatureEncoder_V2() {
		//boolean DEBUG = true;
		Encoder e = null;
		if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V2: start");
		Encoder enc = new Encoder().initSequence();
		//dumpEncoderStep("i", e, enc);
		enc.addToSequence(e=new Encoder(component_basic_data.version,false));
		if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V2: ver="+component_basic_data.version);
		//dumpEncoderStep("v", e, enc);
		enc.addToSequence(e=new Encoder(component_basic_data.globalID).setASN1Type(Encoder.TAG_PrintableString));
		if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V2: GID="+component_basic_data.globalID);
		//dumpEncoderStep("G", e, enc);
		if (component_basic_data.name!=null) {enc.addToSequence(e=new Encoder(component_basic_data.name,Encoder.TAG_UTF8String));
			if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V2: name="+component_basic_data.name);}
		//dumpEncoderStep("n", e, enc);
		if (component_basic_data.slogan!=null) {enc.addToSequence(e=new Encoder(component_basic_data.slogan,DD.TAG_AC0));
			if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V2: slog="+component_basic_data.slogan);}
		//dumpEncoderStep("s", e, enc);
		if (component_basic_data.emails!=null) {enc.addToSequence(e=new Encoder(component_basic_data.emails,DD.TAG_AC1));
			if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V2: email="+component_basic_data.emails);}
		//dumpEncoderStep("e", e, enc);
		if (component_basic_data.phones!=null) {enc.addToSequence(e=new Encoder(component_basic_data.phones,DD.TAG_AC2));
			if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V2: phone="+component_basic_data.phones);}
		//if(instance!=null)enc.addToSequence(new Encoder(instance,DD.TAG_AC3));
		//dumpEncoderStep("p", e, enc);
		if (component_basic_data.creation_date!=null) {enc.addToSequence(e=new Encoder(component_basic_data.creation_date));
			if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V2: cdate="+component_basic_data.creation_date+" H:"+Util.hashEncoder(enc));}
		//dumpEncoderStep("d", e, enc);
		if (shared_addresses != null) {
			ArrayList<Address> _shared_addresses = Address.getOrderedCertifiedTypedAddresses(shared_addresses);
			if (_shared_addresses != null) {
				for (Address sa : _shared_addresses) sa.set_version_structure(Address.V0); 
				enc.addToSequence(e = Encoder.getEncoder(_shared_addresses));
				if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V2: adr="+Util.concatA(_shared_addresses,"---","NULL")+" H:"+Util.hashEncoder(e));
			}
		}
		//dumpEncoderStep("a", e, enc);
		enc.addToSequence(e=new Encoder(component_basic_data.broadcastable));
		if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V2: broadc="+component_basic_data.broadcastable+" "+Util.hashEncoder(enc));
		//dumpEncoderStep("b", e, enc);
		//if(signature_alg!=null)enc.addToSequence(Encoder.getStringEncoder(signature_alg, Encoder.TAG_PrintableString));
		if ((served_orgs!=null)&&(served_orgs.length>0)) {
			D_PeerOrgs[] old = served_orgs;
			served_orgs = makeSignaturePeerOrgs_VI(served_orgs);
			enc.addToSequence(e=Encoder.getEncoder(this.served_orgs).setASN1Type(DD.TAG_AC12));
			if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V2: orgs="+Util.concat(served_orgs,"---","NULL")+" "+Util.hashEncoder(e));
			served_orgs = old;
		}
		if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V2: got:"+Util.byteToHexDump(enc.getBytes())+" H:"+Util.hashEncoder(enc));
		//enc.addToSequence(new Encoder(signature));
		//enc.setASN1Type(TAG);
		return enc;
	}
	Encoder getSignatureEncoder_V3() {
		if (DEBUG) System.out.println("D_Peer: getSignatureEncoder_V3: start");
		Encoder enc = new Encoder().initSequence();
		//enc.addToSequence(new Encoder(component_basic_data.version,false));
		//enc.addToSequence(new Encoder(component_basic_data.globalID).setASN1Type(Encoder.TAG_PrintableString));
		if (component_basic_data.name != null)enc.addToSequence(new Encoder(component_basic_data.name,Encoder.TAG_UTF8String));
		if (component_basic_data.slogan != null)enc.addToSequence(new Encoder(component_basic_data.slogan,DD.TAG_AC0));
		if (component_basic_data.emails != null)enc.addToSequence(new Encoder(component_basic_data.emails,DD.TAG_AC1));
		if (component_basic_data.phones != null)enc.addToSequence(new Encoder(component_basic_data.phones,DD.TAG_AC2));
		//if (instance != null)enc.addToSequence(new Encoder(instance,DD.TAG_AC3));
		if (component_basic_data.creation_date!=null)enc.addToSequence(new Encoder(component_basic_data.creation_date));
		if (this.shared_addresses != null) {
			ArrayList<Address> _shared_addresses = Address.getOrderedCertifiedTypedAddresses(shared_addresses);
			// addresses are ordered in the verifySignature method
			if (_shared_addresses != null) {
				for (Address sa : _shared_addresses) sa.set_version_structure(Address.V3); 
				enc.addToSequence(Encoder.getEncoder(_shared_addresses));
			}
		}
		enc.addToSequence(new Encoder(component_basic_data.broadcastable));
		//if (signature_alg != null)enc.addToSequence(Encoder.getStringEncoder(signature_alg, Encoder.TAG_PrintableString));
		if ((served_orgs != null) && (served_orgs.length > 0)) {
			D_PeerOrgs[] old = served_orgs;
			served_orgs = makeSignaturePeerOrgs_VI(served_orgs);
			enc.addToSequence(Encoder.getEncoder(this.served_orgs).setASN1Type(DD.TAG_AC12));
			served_orgs = old;
		}
		//enc.addToSequence(new Encoder(component_basic_data.signature));
		//enc.setASN1Type(TAG);
		return enc;
	}
	/**
	 * Sorts them by GIDH, and only uses GIDH (eventually computing it from GID)
	 * @param served_orgs
	 * @return
	 */
	public static D_PeerOrgs[] makeSignaturePeerOrgs_VI(D_PeerOrgs[] served_orgs) {
		D_PeerOrgs[] result = new D_PeerOrgs[served_orgs.length];
		for (int k=0;k<result.length;k++) {
			result[k] = new D_PeerOrgs();
			result[k].setOrgGIDH( served_orgs[k].getOrgGIDH());
			/*
			if(served_orgs[k].global_organization_IDhash != null) 
				result[k].global_organization_IDhash = served_orgs[k].global_organization_IDhash;
			else 
				result[k].global_organization_IDhash =
				D_Organization.getOrgGIDHashGuess(served_orgs[k].global_organization_ID);
			*/
		}
		result = sortByOrgGIDHash(result);
		return result;
	}
	public static D_PeerOrgs[] sortByOrgGIDHash(D_PeerOrgs[] result) {
		Arrays.sort(result, new Comparator<D_PeerOrgs>() {
			@Override
			public int compare(D_PeerOrgs o1, D_PeerOrgs o2) {
				String s1 = o1.getOrgGIDH();
				if(s1==null) return -1;
                String s2 = o2.getOrgGIDH();
				if(s2==null) return -1;
                return s1.compareTo(s2);
			}
       });
		return result;
	}

	public boolean hasAddresses() {
		if (this.shared_addresses.size() > 0) return true;
		D_PeerInstance pi = this.getPeerInstance(instance);
		if (pi == null) return false;
		if (pi.addresses.size() > 0) return true;
		return false;
	}
	/**
	 * Convert shared_address to TypedAddress[], for compatibility with old peers
	 * @return
	 */
	/*
	private TypedAddress[] getTypedAddress() {
		Util.printCallPath("typed address not implemented");
		// TODO Auto-generated method stub
		return null;
	}
	*/
	public void
	addAddress(Address _address, Calendar _creation_date,
			String arrival_date, 
			boolean keep_old_priority_if_exists, D_PeerInstance dpi) throws P2PDDSQLException {
		boolean certified = _address.certified;
		int priority = _address.priority;
		if (_DEBUG) System.out.println("D_Peer: addAddress: enter="+_address.toLongString());
		if (dpi == null) { // add to root
			Address e = locateAddress(shared_addresses, _address);
			if (e != null) {
				if (e.certified == certified) {
					if (_DEBUG) System.out.println("D_Peer: addAddress: quit exists already="+e.toLongString());
					return;
				}
				e.certified = certified;
				if (certified && existsCertifiedAddressPriority(shared_addresses, priority))
					e.priority = getMaxCertifiedAddressPriority(shared_addresses)+1;
				else e.priority = priority;

				e.dirty = true;
				this.dirty_addresses = true;
				this.dirty_main = true;
				return;
			}
			if (certified && existsCertifiedAddressPriority(shared_addresses, priority))
				_address.priority = getMaxCertifiedAddressPriority(shared_addresses)+1;
			//else e.priority = priority;
			shared_addresses.add(_address);
			
			_address.dirty = true;
			this.dirty_addresses = true;
			this.dirty_main = true;
			return;
		}
		Address e = locateAddress(dpi.addresses, _address);
		if (e != null) {
			if (e.certified == certified) {
				if (_DEBUG) System.out.println("D_Peer: addAddress: quit exists already="+e.toLongString());
				return;
			}
			e.certified = certified;
			if (certified && existsCertifiedAddressPriority(dpi.addresses, priority))
				e.priority = getMaxCertifiedAddressPriority(dpi.addresses)+1;
			else e.priority = priority;
			
			e.dirty = true;
			dpi.dirty = true;
			this.dirty_addresses = true;
			this.dirty_instances = true;
			return;
		}
		if (certified && existsCertifiedAddressPriority(dpi.addresses, priority))
			_address.priority = getMaxCertifiedAddressPriority(dpi.addresses)+1;
		//else e.priority = priority;
		dpi.addresses.add(_address);
		
		_address.dirty = true;
		dpi.dirty = true;
		this.dirty_addresses = true;
		this.dirty_instances = true;
		return;
	}
	public void
	addAddress(String _address, String type, Calendar _creation_date,
			String arrival_date, boolean certified, int priority, 
			boolean keep_old_priority_if_exists, D_PeerInstance dpi) throws P2PDDSQLException {

		if (dpi == null) { // add to root
			Address e = locateAddress(shared_addresses, _address, type);
			if (e != null) {
				if (e.certified == certified) return;
				e.certified = certified;
				if (certified && existsCertifiedAddressPriority(shared_addresses, priority))
					e.priority = getMaxCertifiedAddressPriority(shared_addresses)+1;
				else e.priority = priority;
				this.dirty_addresses = true;
				e.dirty = true;
				return;
			}
			e = new Address(_address);
			e.dirty = true;
			e.pure_protocol = type;
			e.certified = certified;
			if (certified && existsCertifiedAddressPriority(shared_addresses, priority))
				e.priority = getMaxCertifiedAddressPriority(shared_addresses)+1;
			else e.priority = priority;
			shared_addresses.add(e);
			this.dirty_addresses = true;
			this.dirty_main = true;
			//if (this.dirty_main) this.sign();
			return;
		}
		//D_PeerInstance dpi = this.getPeerInstanceByID(instance_ID);
//		if (dpi == null) {
//			Util.printCallPath("implement addAddress");
//			System.exit(1);
//		}
		Address e = locateAddress(dpi.addresses, _address, type);
		if (e != null) {
			if (e.certified == certified) return;
			e.certified = certified;
			if (certified && existsCertifiedAddressPriority(dpi.addresses, priority))
				e.priority = getMaxCertifiedAddressPriority(dpi.addresses)+1;
			else e.priority = priority;
			this.dirty_addresses = true;
			e.dirty = true;
			return;
		}
		e = new Address(_address);
		e.dirty = true;
		e.pure_protocol = type;
		e.certified = certified;
		if (certified && existsCertifiedAddressPriority(dpi.addresses, priority))
			e.priority = getMaxCertifiedAddressPriority(dpi.addresses)+1;
		else e.priority = priority;
		dpi.addresses.add(e);
		this.dirty_addresses = true;
		/*
		TypedAddress ta = new TypedAddress();
		if(DEBUG) System.out.println("D_Peer: addAddress: adr="+_address+" ty="+type+" cer="+certified+" pri="+priority+" keep="+keep_old_priority_if_exists);
		if(DEBUG) System.out.println("D_Peer: existing adr="+Util.concat(address, ":", "null"));
		ta.address = _address;
		ta.type = type;
		ta.certified=certified;
		ta.priority = priority;
		ta.arrival_date = arrival_date;
		ta.instance_ID = instance_ID;
		
		if(DEBUG) System.out.println("D_Peer: ta="+ta);
		if(this.address==null){
			if(DEBUG) System.out.println("D_Peer: address=null, just add this");
			address = new TypedAddress[]{ta};
		}else{
			if(ta.certified && existsCertifiedAddressPriority(address, priority)) {
				ta.priority = priority = getMaxCertifiedAddressPriority()+1;
				if(DEBUG) System.out.println("D_Peer: new priority="+priority);
			}else{
				if(DEBUG) System.out.println("D_Peer: no conflict on priority="+priority);
			}
			TypedAddress c = TypedAddress.getLastContact(address, ta);
			if (c == null) {
				if (DEBUG) System.out.println("D_Peer: adding current="+ta);
				address = TypedAddress.insert(address, ta);
				if (ta.certified) {
					if(DEBUG) System.out.println("D_Peer: date changed adding current="+ta);
					this.component_basic_data.creation_date = _creation_date;
					signMe();
					_storeVerified(_creation_date, arrival_date, true);
				} else {
					ta.store_or_update(peer_ID, false);
				}
			} else {
				if (keep_old_priority_if_exists && (c.certified == ta.certified)) {
					ta.priority = c.priority;
					ta.peer_address_ID = c.peer_address_ID;
					ta.store_or_update(peer_ID, false);
				} else {
					c.certified = certified;
					c.priority = priority;
					ta.peer_address_ID = c.peer_address_ID;
					ta.store_or_update(peer_ID, false);
					//c.arrival_date = arrival_date;
				}
				if(DEBUG) System.out.println("D_Peer: modify old="+ta);
			}
		}
		*/
	}
	
	private Address locateAddress(ArrayList<Address> addresses,
			String _address, String type) {
		Address nou = new Address(_address);
		for (Address a : addresses) {
			if (!Util.equalStrings_null_or_not(a.pure_protocol, type)) continue;
			if (!Util.equalStrings_null_or_not(a.domain, nou.domain)) continue;
			if (a.tcp_port != nou.tcp_port) continue;
			if (a.udp_port != nou.udp_port) continue;
			return a;
		}
		return null;
	}
	private Address locateAddress(ArrayList<Address> addresses,
			Address nou) {
		for (Address a : addresses) {
			if (!Util.equalStrings_null_or_not(a.pure_protocol, nou.pure_protocol)) continue;
			if (!Util.equalStrings_null_or_not(a.domain, nou.domain)) continue;
			if (a.tcp_port != nou.tcp_port) continue;
			if (a.udp_port != nou.udp_port) continue;
			return a;
		}
		return null;
	}
	public static int getMaxCertifiedAddressPriority(ArrayList<Address> addresses) {
		int max = -1;
		for (Address a : addresses) {
			if (a.certified && a.priority > max) max = a.priority;
		}
		return max;
	}
	public static boolean existsCertifiedAddressPriority(
			ArrayList<Address> addresses, int priority) {
		for (Address a : addresses) {
			if (a.certified && a.priority == priority) return true;
		}
		return false;
	}
	/**
	 * Both sets and returns signature made with getSK()
	 * @return
	 */
	public byte[] sign() {
		byte[] r = sign(getSK());
		reloadMessage();
		return r;
	}
	public void signMe() {
		sign(HandlingMyself_Peer.getMyPeerSK());
		reloadMessage();
	}
	public static byte[] sign(D_Peer peer) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.sign();
		peer.storeRequest();
		peer.releaseReference();
		return peer.getSignature();
	}
	public byte[] getSignature() {
		return this.component_basic_data._signature;
	}
	public void setSignature(byte[] sgn) {
		setSignature(sgn, true);
	}
	/**
	 * To be called directly only from init (when not setting dirty)
	 * @param sgn
	 * @param dirty
	 */
	private void setSignature(byte[] sgn, boolean dirty) {
		this.component_basic_data._signature = sgn;
		if (DEBUG) System.out.println("D_Peer:setSignature: setting "+Util.concat(sgn, ":", "NULL"));
		if ((sgn == null) || (sgn.length == 0)) Util.printCallPath(""+this);
		this.dirty_main = dirty;
	}
	/**
	 * Both sets component_basic_data.signature and returns signature made. 
	 * Sets dirty_main flag.
	 * 
	 * @param sk
	 * @return
	 */
	public byte[] sign(ciphersuits.SK sk){
		this.assertReferenced();
		//boolean DEBUG = true;
		component_basic_data.version = DEFAULT_VERSION;
		//Util.printCallPath("Missing addresses");
		/*
		ArrayList<Address> addr = this.shared_addresses;
		byte[] pict = component_basic_data.picture;
		component_basic_data._signature = new byte[0];
		if (DEBUG) System.err.println("Peer: sign: old (expected NULL) sign set to = "+Util.concat(getSignature(), ":", "NULL"));
		shared_addresses = Address.getOrderedCertifiedTypedAddresses(addr);
		String old_instance = instance; instance = null;
		component_basic_data.picture=null;
		signature_alg = SR.HASH_ALG_V1;
		*/
		if (DEBUG) System.err.println("Peer: sign: peer_addr="+this);
		byte msg[] = this.getSignatureEncoder().getBytes();
		if (DEBUG) System.out.println("Peer: sign:\n\t msg["+msg.length+"]="+Util.byteToHex(msg)+"\n\t hash="+Util.getGID_as_Hash(msg));
		byte[] signature = component_basic_data._signature = Util.sign(msg,  sk);
		if ((signature == null) || (signature.length == 0)) System.err.println("Peer: sign: sign set to null");
		if (DEBUG) System.err.println("Peer: sign: sign set to ="+Util.concat(getSignature(), ":", "NULL"));
/*
		component_basic_data.picture = pict;
		shared_addresses = addr;
		instance = old_instance;
		*/
		if (DEBUG) System.err.println("Peer: sign: sign=["+getSignature().length+"]"+Util.byteToHex(getSignature(), "")+" hash="+Util.getGID_as_Hash(getSignature()));
		this.dirty_main = true;
		return getSignature();
	}
	public static int MAX_LOADED_PEERS = 10000;
	public static long MAX_PEERS_RAM = 10000000;
	private static final int MIN_PEERS_RAM = 2;
	private static final Object monitor_object_factory = new Object();
	D_Peer_Node component_node = new D_Peer_Node(null, null);
	public int status_references = 0;
	/***  (GID: (instance: (DIR_Address:(ADR:date))))*/
	public static Hashtable<String, Hashtable<String, Hashtable<String, Hashtable<String,String>>>> peer_contacts = 
			new Hashtable<String, Hashtable<String, Hashtable<String, Hashtable<String,String>>>>();
	
	public void releaseReference() {
		if (status_references <= 0) Util.printCallPath("Null reference already!");
		else status_references --;
	}
	
	public void assertReferenced() {
		assert (status_references > 0);
	}
	
	/**
	 * The entries that need saving
	 */
	private static HashSet<String> _need_saving = new HashSet<String>();
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Peer> 
	set_DDP2P_DoubleLinkedList_Node(DDP2P_DoubleLinkedList_Node<D_Peer> node) {
		DDP2P_DoubleLinkedList_Node<D_Peer> old = this.component_node.my_node_in_loaded;
		this.component_node.my_node_in_loaded = node;
		return old;
	}
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Peer> 
	get_DDP2P_DoubleLinkedList_Node() {
		return component_node.my_node_in_loaded;
	}
	static boolean need_saving_contains(String GIDH) {
		return _need_saving.contains(GIDH);
	}
	static void need_saving_add(String GIDH, String instance){
		_need_saving.add(GIDH);
	}
	static void need_saving_remove(String GIDH, String instance) {
		if (DEBUG) System.out.println("D_Peer:need_saving_remove: remove "+GIDH+" inst="+instance);
		_need_saving.remove(GIDH);
		if (DEBUG) dumpNeeds(_need_saving);
	}
	static D_Peer need_saving_next() {
		Iterator<String> i = _need_saving.iterator();
		if (!i.hasNext()) return null;
		String c = i.next();
		if (DEBUG) System.out.println("D_Peer: need_saving_next: next: "+c);
		D_Peer r = D_Peer_Node.loaded_peer_By_GIDhash.get(c);
		if (r == null) {
			if (_DEBUG) {
				System.out.println("D_Peer Cache: need_saving_next null entry "
						+ "needs saving next: "+c);
				System.out.println("D_Peer Cache: "+dumpDirCache());
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
	public static String dumpDirCache() {
		String s = "[";
		s += D_Peer_Node.loaded_peers.toString();
		s += "]";
		return s;
	}
	public Calendar getCreationTime() {
		return this.component_basic_data.creation_date;
	}
	/*
	public static D_Peer getPeerIfValid(Decoder d) {
		
		D_Peer p;
		try {
			p = new D_Peer().decode(d);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return null;
		}
		if (!p.verifySignature()) return null;
		D_Peer crt;
		crt = D_Peer.getPeerByGID(p.getGID(), true, false, null);
		if (crt == null)
			crt = D_Peer.getPeerByGIDhash(p.getGIDH(), true, false, true);
		
		crt.load(p);
		if (crt.dirty_any()) crt.storeRequest();
		return crt;
	}
	*/
	/**
	 * Load p into this, setting dirty as appropriate
	 * @param p
	 */
	private void load(D_Peer p) {
		Util.printCallPath("?");
		System.exit(1);
	}
	public static String getLocalPeerIDforGID(String GID) {
		D_Peer p = D_Peer.getPeerByGID(GID, true, false, null);
		if (p == null) return null;
		return p.get_ID();
	}
	public static D_Peer storeReceived(D_Peer __peer, boolean create) {
		assert (__peer.get_ID() == null);
		D_Peer p;
		p = D_Peer.getPeerByGID(__peer.getGID(), true, create);
		if (p == null) return p;
		p.loadRemote(__peer);
		if (p.dirty_any()) p.storeRequest();
		return p;
	}
	/**
	 * Does not set arrival date, preferences and local data (as providing peer).
	 * Save even equal date if the verify signature passes
	 * @param pa
	 * @return
	 */
	public boolean loadRemote(D_Peer pa) {
		if ((!newer(pa, this) && last_signature_verified_successful) || newer(this, pa)) {
			//System.out.println("D_Peer:loadRemote: not newer: "+pa.getName());
			return false;
		}
		last_signature_verified_successful = pa.last_signature_verified_successful;
		signature_verified = pa.signature_verified;
		this.shared_addresses = pa.shared_addresses;
		for (Address sa : this.shared_addresses) {
			if (sa != null) sa.dirty = true;
		}
		this.component_basic_data.globalID = pa.component_basic_data.globalID;
		this.component_basic_data.name = pa.component_basic_data.name;
		this.component_basic_data.emails = pa.component_basic_data.emails;
		this.component_basic_data.phones = pa.component_basic_data.phones;
		this.component_local_agent_state.arrival_date = pa.component_local_agent_state.arrival_date;
		//this.component_preferences.preferences_date = pa.component_preferences.preferences_date;
		this.component_basic_data.slogan = pa.component_basic_data.slogan;
		//this.component_preferences.used = used;
		this.component_basic_data.broadcastable = pa.component_basic_data.broadcastable;
		this.component_basic_data.hash_alg = pa.component_basic_data.hash_alg;
		this.signature_alg = D_Peer.D_Peer_Basic_Data.getHashAlgFromString(this.component_basic_data.hash_alg);
		this.setSignature(pa.getSignature());
		this.component_basic_data.creation_date = pa.component_basic_data.creation_date;
		if(this.component_basic_data.picture == null)
			this.component_basic_data.picture = pa.component_basic_data.picture; // this condition should be removed if picture is part of signature
		this.component_basic_data.version = pa.component_basic_data.version;
		this.served_orgs = pa.served_orgs;
		if (this.served_orgs != null)
			for (D_PeerOrgs so : this.served_orgs) {
				if (so != null) so.dirty = true;
			}
		this.dirty_main = true;
		this.dirty_addresses = true;
		this.dirty_served_orgs = true;
		//this.dirty_instances = true;
		Util.printCallPath(""+this);
		return true;
	}
	public static boolean newer(D_Peer nou, D_Peer old) {
		if (old.getSignature() == null) return true;
		if (old.getSignature().length == 0) return true;
		return false;
	}
	public static String getDisplayName(long peer_ID) {
		D_Peer peer = D_Peer.getPeerByLID(peer_ID, true);
		if (peer == null) return null;
		if (peer.getMyName() != null) return peer.getMyName();
		String name = peer.getName();
		return name;
	}
	public String getMyName() {
		return this.component_preferences.my_name;
	}
	public String getAddressesDesc() {
		if (this.shared_addresses == null) return "NULL";
		String result = "#"+shared_addresses.size()+"#";
		result += Util.concat(this.shared_addresses, ", ", "NULL");
		return result;
	}
	/**
	 * Store "now" as the last date when reset was made,
	 * and set last_sync_date to null
	 * @param peerID
	 * @param reset 
	 */
	public static void reset(String peerID, String instance, Calendar reset) {
		D_Peer p = D_Peer.getPeerByLID(peerID, true);
		if (p == null) return;
		p = D_Peer.getPeerByPeer_Keep(p);
		D_PeerInstance pi = p.getPeerInstance(instance);
		if (pi == null) return;
		pi.set_last_reset(reset);
		pi.dirty = true;
		p.dirty_instances = true;
		p.storeRequest();
		p.releaseReference();
	}	
	/**
	 * get the last reset date
	 * @param _peerID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String getLastResetDate(String _peerID, String instance) throws P2PDDSQLException {
		D_Peer p = D_Peer.getPeerByLID(_peerID, true);
		if (p == null) return null;
		D_PeerInstance pi = p.getPeerInstance(instance);
		if (pi == null) return null;
		return pi.get_last_reset_str();
	}
	/**
	 * Does this serve this orgID?
	 * only implement if it can be done efficiently
	 * @param orgID
	 * @return
	 */
	public boolean servesOrgEntryExists(long orgID) {
		if (orgID <= 0) return false;
		if (this.served_orgs == null) return false;
		for (int k = 0; k < this.served_orgs.length; k ++) {
			if (this.served_orgs[k].organization_ID == orgID) return true;
		}
		return false;
	}
	public boolean servesOrg(long orgID) {
		if (orgID <= 0) return false;
		if (this.served_orgs == null) return false;
		for (int k = 0; k < this.served_orgs.length; k ++) {
			if (this.served_orgs[k].organization_ID == orgID)
				return this.served_orgs[k].getServed();
		}
		return false;
	}
	public void setServingOrg(long orgID, boolean serve) {
		assertReferenced();
		if (serve) setServingOrg(orgID);
		else removeServingOrg(orgID);
	}
	public void setServingOrg(long orgID) {
		assertReferenced();
		if (orgID <= 0) return;
		if (this.served_orgs == null) {
			this.served_orgs = new D_PeerOrgs[1];
			this.served_orgs[0] = new D_PeerOrgs(orgID);
			this.served_orgs[0].setServed(true, true);
			this.dirty_served_orgs = true;
			return;
		}
		for (int k = 0; k < this.served_orgs.length; k ++) {
			if (this.served_orgs[k].organization_ID == orgID) {
				this.dirty_served_orgs |= this.served_orgs[k].setServed(true, true);
				return;
			}
		}
		this.dirty_served_orgs = true;
		D_PeerOrgs nou = new D_PeerOrgs(orgID);
		nou.setServed(true, true);
		D_PeerOrgs[] _po = new D_PeerOrgs[this.served_orgs.length + 1];
		boolean inserted = false;
		int s = 0, d = 0;
		for (; s < this.served_orgs.length;) {
			if (inserted || compare(this.served_orgs[s], nou) < 0) {
				_po[d++] = served_orgs[s++];
			} else {
				_po[d++] = nou; inserted = true;
			}
		}
		if (d < _po.length) _po[d++] = served_orgs[s++];
		this.served_orgs = _po;
	}
	public void removeServingOrg(long orgID) {
		if (orgID <= 0) return;
		if ((this.served_orgs == null) || (this.served_orgs.length == 0)) {
			return;
		}
		boolean change = false;
		for (int k = 0; k < this.served_orgs.length; k ++) {
			if (this.served_orgs[k].organization_ID == orgID) {
				this.dirty_served_orgs |= this.served_orgs[k].setServed(false, true);
				change = true;
			}
		}
		if (!change) return;
		D_PeerOrgs[] _po = new D_PeerOrgs[this.served_orgs.length - 1];
		int s = 0, d = 0;
		boolean deleted = false;
		for (; s < this.served_orgs.length;) {
			if (deleted || !this.served_orgs[s].getServed()) {
				_po[d++] = served_orgs[s++];
			} else {
				s++; deleted = true;
			}
		}
		this.served_orgs = _po;
	}
	public static int compare(D_PeerOrgs old, D_PeerOrgs nou) {
		return old.getOrgGIDH().compareTo(nou.getOrgGIDH());
	}
	/**
	 * Should I search in addresses for something matching isa?
	 * when it is contacting me.
	 * I should also check instances and sockets
	 * @param isa
	 * @param peer_ID2
	 * @return
	 */
	public static String getAddressID(InetSocketAddress isa, String peer_ID2) {
		//Util.printCallPath("Why? isa="+isa+" hn="+isa.getHostName()+" peerID="+peer_ID2);
		String r = _getAddressID(isa, peer_ID2);
		if (r != null) System.out.println("D_Peer:getAddressID: isa="+isa+" ip="+isa.getHostName()+" peerID="+peer_ID2+" -> "+r);
		return r;
	}
	public static String _getAddressID(InetSocketAddress isa, String peer_ID2) {
		D_Peer p = D_Peer.getPeerByLID(peer_ID2, true);
		String ip = isa.getAddress().toString().split(Pattern.quote("/"))[1];
		for ( Address a : p.shared_addresses ) {
			if (a.domain.endsWith(isa.getHostName()))
				if (a.udp_port == isa.getPort())
					return a.peer_address_ID;
		}
		for ( Address a : p.shared_addresses ) {
			if (a.domain.endsWith(ip)) 
				if (a.udp_port == isa.getPort())
					return a.peer_address_ID;
		}
		for ( String k : p._instances.keySet()) {
			D_PeerInstance o = p._instances.get(k);
			for ( Address a : o.addresses ) {
				if (a.domain.endsWith(isa.getHostName()))
					if (a.udp_port == isa.getPort())
						return a.peer_address_ID;
			}
			for ( Address a : o.addresses ) {
				if (a.domain.endsWith(ip)) 
					if (a.udp_port == isa.getPort())
						return a.peer_address_ID;
			}
		}
		//System.exit(1);
		return null;
	}
	public static String getPeerOrgs(String peer_ID2, ArrayList<String> orgs) {
		String result = null;
		D_Peer p = D_Peer.getPeerByLID(peer_ID2, true);
		for(D_PeerOrgs o: p.served_orgs) {
			String name = o.org_name; //make "null" for null
			String name64 = util.Base64Coder.encodeString(name);
			String global_org_ID = o.global_organization_ID+peer_org.ORG_NAME_SEP+name64;
			if(peer_org.DEBUG) System.out.println("peer_org: getPeerOrgs: next ="+global_org_ID);
			if(orgs!=null){
					orgs.add(global_org_ID);
			}
			if (null == result) result = global_org_ID;
			else result = result+peer_org.ORG_SEP+global_org_ID;
		}
		if (peer_org.DEBUG) System.out.println("peer_org: getPeerOrgs: result ="+result);
		return result;
	}
	public boolean getBlocked() {
		return this.component_preferences.blocked;
	}

	public static void main(String args[]) {
		try{_main(args);}catch(Exception e){e.printStackTrace();}
	}
	public static void _main(String args[]) throws P2PDDSQLException {
		Application.db = new DBInterface(Application.DELIBERATION_FILE);
		System.out.println("D_Peer:main: prog pID, verify, sign, store");
		long pID = Long.parseLong(args[0]);
		int verif = Integer.parseInt(args[1]);
		int sign = Integer.parseInt(args[2]);
		int store = Integer.parseInt(args[3]);
		/*
		HandlingMyself_Peer.loadIdentity(null);
		D_Peer me = HandlingMyself_Peer.getPeer(Identity.current_peer_ID);
		System.out.println("D_Peer: main: me= "+me);
		HandlingMyself_Peer.setMyself(me, false);
		*/
		
		//D_Peer d = D_Peer.getPeerByLID(1, true);
		//System.out.println("D_Peer: main:read(1) "+d);
		D_Peer d2 = D_Peer.getPeerByLID(pID, true);
		//System.out.println("D_Peer: main: cache="+dumpDirCache());
		System.out.println("\nD_Peer: main: d="+d2);
		
		//System.out.println("\nD_Peer: main 1: ********\n");
		//d2.verifySignature();
		//System.out.println("\nD_Peer: main 2: ********\n");
		//d2.sign();
		System.out.println("\nD_Peer: main 3: ********\n");
		if (verif > 0) {
			boolean r = d2.verifySignature();
			System.out.println("\nD_Peer: main 3: verif result="+r);			
		}
		if (sign > 0) d2.sign();
		if (store > 0) d2.storeSynchronouslyNoException();
		
	}
	/**
	 * get D_PeerInstance
	 * @param _instance_ID
	 * @return
	 */
	public D_PeerInstance getPeerInstance_ByID(String _instance_ID) {
		return this.inst_by_ID.get(_instance_ID);
	}	
	/*
	public D_PeerInstance getPeerInstance_ByID(long _instance_ID) {
		for (D_PeerInstance i : _instances.values()) {
			if (i.get_peer_instance_ID_long() == _instance_ID) return i;
		}
		return null;
	}
	
	public static void __main (String args[]) {
		Application.db = new util.DBInterface(Application.DELIBERATION_FILE);
		//Identity.init_Identity();
		//D_Peer p = 
	}
	*/
	public boolean getUsed() {
		return this.component_preferences.used;
	}
	public static void setUsed(D_Peer peer, boolean b) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setUsed(b);
		peer.storeRequest();
		peer.releaseReference();
	}
	public void setUsed(boolean b) {
		assertReferenced();
		this.component_preferences.used = b;
		this.dirty_main = true;
	}
	public String getLastSyncDate(String instance) {
		D_PeerInstance dpi = this.getPeerInstance(instance);
		if (dpi == null) return null;
		return dpi.get_last_sync_date_str();
	}
	public Calendar getLastSyncDateCalendar(String instance) {
		D_PeerInstance dpi = this.getPeerInstance(instance);
		if (dpi == null) return null;
		return dpi.get_last_sync_date();
	}
	public String getName_MyOrDefault() {
		if (component_preferences.my_name != null) return component_preferences.my_name;
		return this.component_basic_data.name;
	}
	public String getSlogan_MyOrDefault() {
		if (component_preferences.my_slogan != null) return component_preferences.my_slogan;
		return this.component_basic_data.slogan;
	}
	public boolean getHidden() {
		return this.component_preferences.hidden;
	}
	public String getProvider() {
		return this.component_local_agent_state.first_provider_peer;
	}
	public String getProviderName() {
		if (this.component_local_agent_state.first_provider_peer == null) return null;
		D_Peer provider = D_Peer.getPeerByLID(component_local_agent_state.first_provider_peer, true);
		if (provider == null) return null;
		return provider.getName_MyOrDefault();
	}
	public boolean isTemporary() {
		return getSignature() == null;
	}
	/**
	 * Could return instructions
	 * @return
	 */
	public boolean isRevoked() {
		return this.component_basic_data.revoked;
	}
	public boolean isEmailVerified() {
		return this.component_preferences.email_verified;
	}
	public boolean isNameVerified() {
		return this.component_preferences.name_verified;
	}
	public String getCategory() {
		return this.component_preferences.category;
	}
	public static void setName_My(D_Peer peer, String name) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setName_My(name);
		peer.releaseReference();
	}
	public void setName_My(String name) {
		assertReferenced();
		this.component_preferences.my_name = name;
		this.dirty_my_data = true;
		this.storeRequest();
	}
	public static void setSlogan_My(D_Peer peer, String slogan) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setSlogan_My(slogan);
		peer.releaseReference();
	}
	public void setSlogan_My(String slogan) {
		assertReferenced();
		this.component_preferences.my_slogan = slogan;
		this.dirty_my_data = true;
		this.storeRequest();
	}
	public static void setBlocked(D_Peer peer, Boolean value) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setBlocked(value);
		peer.releaseReference();
	}
	public void setBlocked(Boolean value) {
		assertReferenced();
		this.component_preferences.blocked = value;
		this.dirty_main = true;
		this.storeRequest();
	}
	public static void setCategory(D_Peer peer, String value) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setCategory(value);
		peer.releaseReference();
	}
	public void setCategory(String cat) {
		assertReferenced();
		this.component_preferences.category = cat;
		this.dirty_main = true;
		this.storeRequest();
	}
	public static void setHidden(D_Peer peer, Boolean value) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setHidden(value);
		peer.releaseReference();
	}
	private void setHidden(Boolean value) {
		assertReferenced();
		this.component_preferences.hidden = value;
		this.dirty_main = true;
		this.storeRequest();
	}
	public static void setTemporary(D_Peer peer) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setTemporary();
		peer.releaseReference();
	}
	public void setTemporary() {
		this.setSignature(null);
		this.dirty_main = true;
		this.storeRequest();
	}
	public static void setName_Verified(D_Peer peer) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setName_Verified();
		peer.releaseReference();
	}
	public void setName_Verified() {
		assertReferenced();
		this.component_preferences.name_verified = true;
		this.dirty_main = true;
		this.storeRequest();
	}
	public boolean getFiltered() {
		return this.component_preferences.filtered;
	}
	public static void setFiltered(D_Peer peer, Boolean value) {
		peer = D_Peer.getPeerByPeer_Keep(peer);
		peer.setFiltered(value);
		peer.releaseReference();
	}
	public void setFiltered(boolean filtered) {
		assertReferenced();
		this.component_preferences.filtered = filtered;
		this.dirty_main = true;
		this.storeRequest();
	}
	/**
	 * Should be rewritten to purge the caches
	 */
	public void purge() {
		String peerID = this.get_ID();
    	try {
			Application.db.deleteNoSync(table.peer_address.TNAME, new String[]{table.peer_address.peer_ID}, new String[]{peerID}, DEBUG);
			Application.db.deleteNoSync(table.peer_my_data.TNAME, new String[]{table.peer_my_data.peer_ID}, new String[]{peerID}, DEBUG);
			Application.db.deleteNoSync(table.org_distribution.TNAME, new String[]{table.org_distribution.peer_ID}, new String[]{peerID}, DEBUG);
			Application.db.deleteNoSync(table.peer_org.TNAME, new String[]{table.peer_org.peer_ID}, new String[]{peerID}, DEBUG);
			Application.db.deleteNoSync(table.peer_plugin.TNAME, new String[]{table.peer_plugin.peer_ID}, new String[]{peerID}, DEBUG);
			delete(D_Peer.getPeerByLID(peer_ID, true));
			Application.db.sync(new ArrayList<String>(Arrays.asList(table.peer.TNAME, table.peer_my_data.TNAME, table.org_distribution.TNAME,
					table.peer_org.TNAME,table.peer_plugin.TNAME, table.peer.TNAME))); //table never loaded
    	} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
	}
	static public void delete(D_Peer peer) throws P2PDDSQLException {
		synchronized(monitor_object_factory) {
			if (peer == null) {
				Util.printCallPath("Why delete null?");
				return;
			}
			if (peer.status_references > 1) {
				int i = Application_GUI.ask(_("The peer cannot be deleted because it is in use! Force?\nUsers: ")+peer.status_references, _("Deleting peer failed"), Application_GUI.OK_CANCEL_OPTION);
				if (i != 0)
					return;
			}
			Application.db.deleteNoSync(table.peer.TNAME, new String[]{table.peer.peer_ID}, new String[]{peer.get_ID()}, DEBUG); // never loaded
			D_Peer_Node.unregister_loaded(peer);
		}
	}
	public boolean getEmailVerified() {
		return this.component_preferences.email_verified;
	}
	/**
	 * Returns a list with all local IDs for peers, ordered by "used DESC, last_sync_date DESC".
	 * Since last_sync_date is no longer used in table peer, probably we should used the new corresponding instance
	 * @return
	 */
	public static ArrayList<ArrayList<Object>> getAllPeers() {
		boolean DEBUG = false;
		ArrayList<ArrayList<Object>> _peers;
		try {
			String sql = "SELECT "+table.peer.peer_ID+ 
					" FROM "+table.peer.TNAME +
					" ORDER BY "+ table.peer.used+" DESC; ";
					//" ORDER BY "+ table.peer.used+" DESC, "+table.peer.last_sync_date+" DESC; ";
			String[] params = new String[0];
			if (DEBUG) System.out.println("PeersModel:update: will select");
			_peers = Application.db.select(sql, params, DEBUG);
			if (DEBUG) System.out.println("PeersModel:update: did select");
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		return _peers;
	}
	public void setProvider(String peer_ID2) {
		if (component_local_agent_state.first_provider_peer != peer_ID2) {
			component_local_agent_state.first_provider_peer = peer_ID2;
			dirty_main = true;
		}
	}
	public void setLastSyncDate(String instance2, String gdate) {
		this.assertReferenced();
		D_PeerInstance inst = this.getPeerInstance(instance2);
		if (inst == null) {
			inst = new D_PeerInstance();
			inst.peer_instance = instance2;
		}
		inst.dirty = true;
		inst.set_last_sync_date(gdate);
		this.dirty_instances = true;
		this.storeRequest();
	}
	public void setLastSyncDate(D_PeerInstance _instance, String gdate) {
		D_PeerInstance inst = integratePeerInstance(_instance);

		inst.set_last_sync_date(gdate);
		inst.dirty = true;
		this.dirty_instances = true;
		this.storeRequest();
	}
	public D_PeerInstance integratePeerInstance(D_PeerInstance _instance) {
		this.assertReferenced();
		String instance2 = _instance.get_peer_instance();
		D_PeerInstance inst = this.getPeerInstance(instance2);
		if (! _instance.verifySignature(getPK())) inst.signature = null;
		if (inst == null) {
			inst = _instance; //new D_PeerInstance();
			this._instances.put(Util.getStringNonNullUnique(instance2), _instance);

			inst.dirty = true;
			this.dirty_instances = true;
			this.dirty_addresses = true;
			this.storeRequest();
			return inst;
		}
		if (inst.signature != null && _instance.signature == null) {
			return inst;
		}

		if (inst.signature == null && _instance.signature != null) {
			inst.loadRemote(_instance);
			this.dirty_addresses = true;
			this.dirty_instances = true;
			this.storeRequest();
			return inst;
		}
		// comparing signed
		if (inst.signature != null && _instance.signature != null) {
			// new is older or unknown as date
			if (_instance.creation_date == null || _instance.creation_date.compareTo(inst.creation_date) <= 0) {
				return inst;
			}
			inst.loadRemote(_instance);
			this.dirty_addresses = true;
			this.dirty_instances = true;
			return inst;
		}
		
		// comparing unsigned:
		// new is older or unknown as date
		if (_instance.creation_date == null || _instance.creation_date.compareTo(inst.creation_date) < 0) {
			inst.joinOld(_instance);
		} else {
			inst.joinNew(_instance);
		}
		inst.dirty = true;
		this.dirty_addresses = true;
		this.dirty_instances = true;
		this.storeRequest();
		return inst;
	}
	/**
	 *  updates the date in the orgID is found
	 * @param date
	 * @param orgID
	 */
	public void updateLastSyncDate(String date, String orgID) {
		for (D_PeerOrgs o : this.served_orgs) {
			if (Util.equalStrings_and_not_null(orgID, Util.getStringID(o.organization_ID))) {
				o.set_last_sync_date(date, true);
				this.dirty_served_orgs = true;
			}
		}
	}
	/**
	 * Iterates over all addresses and checks the peer_address_ID equality
	 * @param crtDate
	 * @param address_ID
	 */
	public void updateAddress_LastConnection(String crtDate, String address_ID) {
		if (address_ID == null) return;
		for (Address a : this.shared_addresses) {
			if (Util.equalStrings_and_not_null(a.peer_address_ID, address_ID)) a.last_contact = crtDate;
		}
		for (D_PeerInstance dpi : this._instances.values()) {
			for (Address a : dpi.addresses) {
				if (Util.equalStrings_and_not_null(a.peer_address_ID, address_ID)) a.last_contact = crtDate;
			}
		}
	}
	public void updateAddress_LastConnection(String crtDate,
			InetSocketAddress s_address) {
		if (s_address == null) return;
		for (Address a : this.shared_addresses) {
			if (equalAddress(a, s_address)) a.last_contact = crtDate;
		}
		for (D_PeerInstance dpi : this._instances.values()) {
			for (Address a : dpi.addresses) {
				if (equalAddress(a, s_address)) a.last_contact = crtDate;
			}
		}
	}
	public static long getPeersCount() throws P2PDDSQLException {
		String sql = "SELECT count(*) FROM "+table.peer.TNAME;
		ArrayList<ArrayList<Object>> c = Application.db.select(sql, new String[]{}, UpdatePeersTable.DEBUG);
		if (c.size() > 0) return Util.lval(c.get(0).get(0));
		return 0;
	}
	public static boolean equalAddress(Address ad, InetSocketAddress s_a) {
		//Address ad = new Address(address);
		if ((ad == null) || (ad.domain == null) || (ad.udp_port <= 0)) {
			// out.println("Empty address "+ad+" ("+ad.domain+"):("+ad.udp_port+") from: "+s_a);
			return false;
		}
		InetSocketAddress isa = new InetSocketAddress(ad.domain, ad.udp_port);
		if (
				(isa.getPort() == s_a.getPort()) &&
				( Util.getNonBlockingHostName(isa).equals(Util.getNonBlockingHostName(s_a)) ||
				  (isa.isUnresolved() && s_a.isUnresolved() &&
				    equalByteArrays(isa.getAddress().getAddress(),s_a.getAddress().getAddress())))) return true;
		return false;
	}
	private static boolean equalByteArrays(byte[] a1, byte[] a2) {
		if((a1==null) || (a2==null)) return a1==a2;
		if(a1.length != a2.length) return false;
		for(int k=0; k<a1.length; k++) if(a1[k]!=a2[k]) return false;
		return true;
	}
	/**
	 * Is this not a temporary object?
	 * @return
	 */
	public boolean readyToSend() {
		return this.getBroadcastableMyOrDefault() && (this.getGID() != null);
	}
	public void fillLocals(RequestData sol_rq, RequestData new_rq) {
		if (sol_rq != null) sol_rq.peers.put(this.getGIDH(), this.getCreationDate());
		for (D_PeerOrgs o : this.served_orgs) {
			if (!D_Organization.hasGIDH(o.getOrgGIDH())) {
				RequestData n = new RequestData(o.getOrgGIDH());
				n.orgs.add(o.getOrgGIDH());
				new_rq.add(n);
			}
		}
	}
	public static String getPeerLIDbyGID(String global_peer_ID) throws P2PDDSQLException {
			D_Peer aup_peer = getPeerByGID(global_peer_ID, true, false);
			String peer_ID = null;
			if (aup_peer != null) peer_ID = aup_peer.get_ID();
			return peer_ID;
	/*
			String result = "-1";
			ArrayList<ArrayList<Object>> dt=Application.db.select("SELECT "+table.peer.peer_ID+" FROM "+table.peer.TNAME+" WHERE "+table.peer.global_peer_ID+" = ?;",
					new String[]{""+global_peer_ID});
			if((dt.size()>=1) && (dt.get(0).size()>=1)) {
				result = dt.get(0).get(0).toString();
			}
			return result;
			*/
		}

}
class D_Peer_SaverThread extends util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000;
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	/**
	 * The next monitor is needed to ensure that two D_Peer_SaverThreadWorker are not concurrently modifying the database,
	 * and no thread is started when it is not needed (since one is already running).
	 */
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Peer_SaverThread() {
		super("D_Peer Saver", true);
		//start ();
	}
	public void _run() {
		for (;;) {
			if (stop) return;
			synchronized(saver_thread_monitor) {
				new D_Peer_SaverThreadWorker().start();
			}
			/*
			synchronized(saver_thread_monitor) {
				D_Peer de = D_Peer.need_saving_next();
				if (de != null) {
					if (DEBUG) System.out.println("D_Peer_Saver: loop saving "+de);
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
					//System.out.println("D_Peer_Saver: idle ...");
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

class D_Peer_SaverThreadWorker extends util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000;
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Peer_SaverThreadWorker() {
		super("D_Peer Saver Worker", false);
		//start ();
	}
	public void _run() {
		synchronized(D_Peer_SaverThread.saver_thread_monitor) {
			D_Peer de = D_Peer.need_saving_next();
			if (de != null) {
				if (DEBUG) System.out.println("D_Peer_Saver: loop saving "+de);
				Application_GUI.ThreadsAccounting_ping("Saving");
				D_Peer.need_saving_remove(de.getGIDH(), de.instance);
				// try 3 times to save
				for (int k = 0; k < 3; k++) {
					try {
						de.storeAct();
						break;
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
						synchronized(this) {
							try {
								wait(SAVER_SLEEP_ON_ERROR);
							} catch (InterruptedException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
			} else {
				Application_GUI.ThreadsAccounting_ping("Nothing to do!");
				//System.out.println("D_Peer_Saver: idle ...");
			}
		}
		synchronized(this) {
			try {
				wait(SAVER_SLEEP);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
