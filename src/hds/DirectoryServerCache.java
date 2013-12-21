package hds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import config.Application;
import data.D_PeerAddress;

import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

import util.DDP2P_DoubleLinkedList;
import util.DDP2P_DoubleLinkedList_Node;
import util.P2PDDSQLException;
import util.Util;

public
class DirectoryServerCache{
	public static DirectoryServerCache.D_Directory_Node.SaverThread saverThread = null;
	DirectoryServerCache(){
		if(saverThread == null)
			saverThread = new DirectoryServerCache.D_Directory_Node.SaverThread();
	}
	class TurnMessage{
		byte[] data;
	}
	public static class D_DirectoryEntry extends ASNObj {
		private static final boolean DEBUG = false;
		public D_Directory_Node component_node;
		//public boolean loaded_globals;
		// public String _peer_ID;
		public long registered_ID;
		public String globalID;
		public String globalIDhash;
		public String instance;
		public byte[] certificate;
		public Address[] addresses;
		public byte[] signature;
		public Calendar timestamp;
		//public boolean need_saving;
		D_DirectoryEntry parent;
		public Hashtable<String,ArrayList<TurnMessage>> messages = new Hashtable<String,ArrayList<TurnMessage>>(); // from_peer, messages

		public Hashtable<String, D_DirectoryEntry> instances = new Hashtable<String, D_DirectoryEntry>();
		/**
		 * 
		 * @param _globalID
		 */
		public D_DirectoryEntry(String _globalID){
			initGID(_globalID);
		}
		/**
		 * 
		 * @param _globalIDH
		 * @param hash
		 */
		public D_DirectoryEntry(String _globalIDH, boolean hash){
			initGIDH(_globalIDH);
		}
		/**
		 * 
		 * @param globalID2
		 * @param globalIDhash2
		 */
		public D_DirectoryEntry(String globalID2, String globalIDhash2) {
			if(globalID2 != null) initGID(globalID2);
			else if(globalIDhash2 != null) initGIDH(globalIDhash2);
		}
		/**
		 * 
		 * @param a
		 * @param _parent
		 */
		public D_DirectoryEntry(ArrayList<Object> a,  D_DirectoryEntry _parent) {
			init(a, this);
		}
		public D_DirectoryEntry() {}
		long update() throws P2PDDSQLException{
			if(registered_ID<=0) return this.storeNew();
			D_Directory_Node.need_saving_remove(globalIDhash, instance); //need_saving = false;
			String params[] = new String[table.registered.fields_list.length];
			params[table.registered.REG_GID] = globalID;
			params[table.registered.REG_GID_HASH] = globalIDhash;
			params[table.registered.REG_INSTANCE] = instance;
			params[table.registered.REG_CERT] = (certificate.length==0)?null:Util.stringSignatureFromByte(certificate);
			params[table.registered.REG_ADDR] = Address.joinAddresses(addresses);
			params[table.registered.REG_SIGN] = (signature.length==0)?null:Util.stringSignatureFromByte(signature);
			params[table.registered.REG_TIME] = (Util.CalendargetInstance().getTimeInMillis()/1000)+"";
			params[table.registered.REG_ID] = Util.getStringID(this.registered_ID);
			
			Application.db_dir.update
					(
							table.registered.TNAME,
							table.registered.fields_noID_list,
							new String[]{table.registered.registeredID},
							params, DEBUG
					);
//					new String[]{table.registered.global_peer_ID,table.registered.certificate,table.registered.addresses,table.registered.signature,table.registered.timestamp},
			return registered_ID;
		}
		long storeNew() throws P2PDDSQLException{
			D_Directory_Node.need_saving_remove(globalIDhash, instance); // need_saving = false;
			String params[] = new String[table.registered.fields_noID_list.length];
			params[table.registered.REG_GID] = globalID;
			params[table.registered.REG_GID_HASH] = globalIDhash;
			params[table.registered.REG_INSTANCE] = instance;
			params[table.registered.REG_CERT] = (certificate.length==0)?null:Util.stringSignatureFromByte(certificate);
			params[table.registered.REG_ADDR] = Address.joinAddresses(addresses);
			params[table.registered.REG_SIGN] = (signature.length==0)?null:Util.stringSignatureFromByte(signature);
			params[table.registered.REG_TIME] = (Util.CalendargetInstance().getTimeInMillis()/1000)+"";
			
			registered_ID=Application.db_dir.insert(table.registered.TNAME, table.registered.fields_noID_list,
					params);
//					new String[]{table.registered.global_peer_ID,table.registered.certificate,table.registered.addresses,table.registered.signature,table.registered.timestamp},
			return registered_ID;
		}
		void load(D_DirectoryEntry n) {
			if(n.globalID!=null) globalID = n.globalID;
			if(n.globalIDhash!=null) globalIDhash = n.globalIDhash;
			instance = n.instance;
			certificate = n.certificate;
			addresses = n.addresses;
			signature = n.signature;
			timestamp = n.timestamp;
			D_Directory_Node.need_saving_add(globalIDhash, instance);//need_saving = true;
		}
		private static void set_NAT(Address[] addresses2, Address nat, boolean tCP) {
			if(nat == null) return;
			Address n = get_NAT(addresses2, tCP);
			if(n==null) return;
			if(!n.domain.equals(nat.domain)) return;
			if(tCP) n.udp_port = nat.udp_port;
			else n.tcp_port = nat.tcp_port;
		}
		private static Address get_NAT(Address[] addresses2, boolean tCP) {
			for(Address a: addresses2) 
				if(Address.NAT.equals(a.protocol)) return a;
			return null;
		}
		/**
		 * Loads (if needed) and sets da.
		 * Does register new instances in the list .
		 * @param da
		 */
		public void loadInstance(DirectoryAnnouncement da, boolean tCP) {
			if(da.instance == null){
				load(da, tCP);
				return;
			}
			D_DirectoryEntry c = instances.get(da.instance);
			if(c != null) {
				c.load(da, tCP);
				return;
			}
			c = new D_DirectoryEntry();
			c.parent = this;
			c.load(da, tCP);
			instances.put(instance, c);
			D_Directory_Node.register_loaded_instance(c);
		}
		/**
		 * Set needs_saving on only differences
		 * @param da
		 */
		public void load(DirectoryAnnouncement da, boolean TCP) {
			boolean need_saving = false;
			if(da.globalID!=null){
				if(!Util.equalStrings_null_or_not(globalID, da.globalID)){
					need_saving = true;
					globalID = da.globalID;
				}
			}
			if(da.globalIDhash!=null){
				if(!Util.equalStrings_null_or_not(globalIDhash, da.globalIDhash)){
					need_saving = true;
					globalIDhash = da.globalIDhash;
				}
			}
			if(!Util.equalStrings_null_or_not(instance, da.instance)){
				need_saving = true;
				instance = da.instance;
			}
			if(!Util.equalBytes_null_or_not(certificate, da.certificate)){
				need_saving = true;
				certificate = da.certificate;
			}
			if(!Util.equalBytes_null_or_not(signature, da.signature)){
				need_saving = true;
				signature = da.signature;
			}
			set_NAT(da.address._addresses, get_NAT(addresses, TCP), TCP);
			String _addresses = Address.joinAddresses(addresses);
			String _addresses_ = Address.joinAddresses(da.address._addresses);
			if(!Util.equalStrings_null_or_not(_addresses, _addresses_)) {
				need_saving = true;
				addresses = da.address._addresses;
			}
			timestamp = da.date;
			//need_saving = true;
			if(need_saving)
				D_Directory_Node.need_saving_add(globalIDhash, instance);
		}
		private void initGIDH(String _globalIDH) {
			clean();
			String sql =
					"SELECT "+table.registered.fields+
					" FROM "+table.registered.TNAME+
					" WHERE "+table.registered.global_peer_ID_hash+" = ?;";
			ArrayList<ArrayList<Object>> d;
			try {
				d = Application.db_dir.select(sql, new String[]{_globalIDH}, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				this.globalIDhash = _globalIDH;
				return;
			}
			if(d.size() > 0) initAll(d);
			
			if(this.globalIDhash == null) this.globalIDhash = _globalIDH;
		}
		public void initAll(ArrayList<ArrayList<Object>> d){
				for(ArrayList<Object> a : d){
					Object _instance = a.get(table.registered.REG_INSTANCE); 
					if(_instance == null) {
						init(d.get(0), this);
						continue;
					}
					this.instances.put(Util.getString(_instance), new D_DirectoryEntry(a, this));
				}
		}
		
		private void initGID(String _globalID) {
			clean();
			String sql =
					"SELECT "+table.registered.fields+
					" FROM "+table.registered.TNAME+
					" WHERE "+table.registered.global_peer_ID+" = ?;";
			ArrayList<ArrayList<Object>> d;
			try {
				d = Application.db_dir.select(sql, new String[]{_globalID}, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				this.globalID = _globalID;
				this.globalIDhash = D_PeerAddress.getGIDHashFromGID(globalID);
				return;
			}
			if(d.size() > 0) initAll(d);
			
			if(this.globalID == null) this.globalID = _globalID;
			if(this.globalIDhash == null) this.globalIDhash = D_PeerAddress.getGIDHashFromGID(globalID);
		}
		
		private void clean() {
			registered_ID = -1;
			if(globalIDhash!=null)D_Directory_Node.need_saving_remove(globalIDhash, instance); // need_saving = false;
		}
		private void init(ArrayList<Object> d, D_DirectoryEntry _parent) {
			parent = _parent;
			this.registered_ID = Util.lval(d.get(table.registered.REG_ID));
			//this.addresses = TypedAddress.parseStringAddresses(Util.getString(d.get(table.registered.REG_ADDR)));
			this.addresses = Address.getAddresses(Util.getString(d.get(table.registered.REG_ADDR)));
			this.certificate = Util.byteSignatureFromString(Util.getString(d.get(table.registered.REG_CERT)));
			this.globalID = Util.getString(d.get(table.registered.REG_GID));
			this.globalIDhash = Util.getString(d.get(table.registered.REG_GID_HASH));
			this.instance = Util.getString(d.get(table.registered.REG_INSTANCE));
			this.signature = Util.byteSignatureFromString(Util.getString(d.get(table.registered.REG_SIGN)));
		}

		@Override
		public Encoder getEncoder() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Object decode(Decoder dec) throws ASN1DecoderFail {
			// TODO Auto-generated method stub
			return null;
		}
	}
	/**
	 * Linked List node of firectory entries
	 * @author msilaghi
	 *
	 */
	public static class D_Directory_Node {
		
		private static final int MAX_LOADED_PEERS = 0;
		private static final long MAX_PEERS_RAM = 0;
		private static final int MIN_PEERS_RAM = 0;
		/**
		 * Currently loaded peers, ordered by the access time
		 */
		private static DDP2P_DoubleLinkedList<D_DirectoryEntry> loaded_peers = new DDP2P_DoubleLinkedList<D_DirectoryEntry>();
		// private static Hashtable<Long, D_DirectoryEntry> loaded_peer_By_LocalID = new Hashtable<Long, D_DirectoryEntry>();
		private static Hashtable<String, D_DirectoryEntry> loaded_peer_By_GID = new Hashtable<String, D_DirectoryEntry>();
		private static Hashtable<String, D_DirectoryEntry> loaded_peer_By_GIDhash = new Hashtable<String, D_DirectoryEntry>();
		private static class KEY{
			String hash, instance;
			KEY(String _hash, String _instance){hash = _hash; instance = _instance;}
			@Override
			public boolean equals(Object o){
				if(! (o instanceof KEY)) return false;
				KEY k = (KEY) o;
				if(!Util.equalStrings_null_or_not(hash, k.hash)) return false;
				if(!Util.equalStrings_null_or_not(instance, k.instance)) return false;
				return true;
			}
		}
		static HashSet<KEY> _need_saving = new HashSet<KEY>();
		static private KEY obj_key(String gIDH, String instance) {
			return new KEY(gIDH,instance);
		}
		static boolean need_saving_contains(String GIDH, String instance){
			return _need_saving.contains(obj_key(GIDH, instance));
		}
		static void need_saving_add(String GIDH, String instance){
			_need_saving.add(obj_key(GIDH, instance));
		}
		static void need_saving_remove(String GIDH, String instance){
			_need_saving.remove(obj_key(GIDH, instance));
		}
		static D_DirectoryEntry need_saving_next(){
			Iterator<KEY> i = D_Directory_Node._need_saving.iterator();
			if(!i.hasNext()) return null;
			KEY c = i.next();
			String h = c.hash;
			String index = c.instance;
			D_DirectoryEntry r = loaded_peer_By_GIDhash.get(h);
			return r.instances.get(index);
		}
		private static long current_space = 0;
		private static final Object saver_monitor = new Object();
		public static class SaverThread extends Thread{
			private static final long SAVER_SLEEP = 100;
			SaverThread(){
				start();
			}
			public void run(){
				for(;;) {
					synchronized(saver_monitor) {
						D_DirectoryEntry de = need_saving_next();
						if(de != null)
							try {
								need_saving_remove(de.globalIDhash, de.instance);
								de.update();
							} catch (P2PDDSQLException e) {
								e.printStackTrace();
							}
					}
					synchronized(this){
						try {
							wait(SAVER_SLEEP);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		/**
		 * message is enough (no need to store the Encoder itself)
		 */
		public byte[] message;
		public DDP2P_DoubleLinkedList_Node<D_DirectoryEntry> my_node_in_loaded;

		public D_Directory_Node(byte[] message,
				DDP2P_DoubleLinkedList_Node<D_DirectoryEntry> my_node_in_loaded) {
			this.message = message;
			this.my_node_in_loaded = my_node_in_loaded;
		}
		/**
		 * if it was loaded but not fully, now we know its size
		 * @param crt
		 */
		private static void register_fully_loaded(D_DirectoryEntry crt) {
			//assert((crt.component_node.message==null) && (crt.loaded_globals));
			if(crt.component_node.message != null) return;
			//if(!crt.loaded_globals) return;
			byte[] message = crt.encode();
			synchronized(loaded_peers) {
				crt.component_node.message = message; // crt.encoder.getBytes();
				if(crt.component_node.message != null) current_space += crt.component_node.message.length;
			}
		}
		/**
		 * Add a new instance to an existing peer cache.
		 * 
		 * Since the peer is assumed preloaded in the cache, instance not needed in global hashtables.
		 * @param crt
		 */
		public static void register_loaded_instance(D_DirectoryEntry crt) {
			ArrayList<D_DirectoryEntry> rem = new ArrayList<D_DirectoryEntry>();
			synchronized(loaded_peers) {
				loaded_peers.offerFirst(crt);
				if(crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				while((loaded_peers.size() > MAX_LOADED_PEERS)
						|| (current_space > MAX_PEERS_RAM)) {
					if(loaded_peers.size() <= MIN_PEERS_RAM) break; // at least _crt_peer and _myself
					D_DirectoryEntry removed = loaded_peers.removeTail();//remove(loaded_peers.size()-1);
					if(removed.component_node.message != null) current_space -= removed.component_node.message.length;
					if(D_Directory_Node.need_saving_contains(removed.globalIDhash, removed.instance)) rem.add(removed);
					else{
						for(D_DirectoryEntry a: removed.instances.values()){
							if(D_Directory_Node.need_saving_contains(a.globalIDhash, a.instance)) rem.add(a);
						}
					}
				}
			}
			for(D_DirectoryEntry removed : rem){
				try {
					synchronized(D_Directory_Node.saver_monitor){
						if(D_Directory_Node.need_saving_contains(removed.globalIDhash, removed.instance)) removed.update();
						for(D_DirectoryEntry a: removed.instances.values()){
							if(D_Directory_Node.need_saving_contains(a.globalIDhash, a.instance)) a.update();
						}
					}
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}				
			}
		}
		/**
		 * Here we manage the registered peers.
		 * @param crt
		 */
		private static void register_loaded(D_DirectoryEntry crt){
			ArrayList<D_DirectoryEntry> rem = new ArrayList<D_DirectoryEntry>();
			//crt.encoder = crt.getEncoder();
			//if(crt.loaded_globals) crt.component_node.message = crt.encode(); //crt.encoder.getBytes();
			synchronized(loaded_peers) {
				loaded_peers.offerFirst(crt);
				// loaded_peer_By_LocalID.put(new Long(crt._peer_ID), crt);
				loaded_peer_By_GID.put(crt.globalID, crt);
				loaded_peer_By_GIDhash.put(crt.globalIDhash, crt);
				if(crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				while((loaded_peers.size() > MAX_LOADED_PEERS)
						|| (current_space > MAX_PEERS_RAM)) {
					if(loaded_peers.size() <= MIN_PEERS_RAM) break; // at least _crt_peer and _myself
//					D_DirectoryEntry candidate = loaded_peers.getTail();
//					if((candidate == D_DirectoryEntry._crt_peer)||(candidate == D_DirectoryEntry._myself)){
//						setRecent(candidate);
//						continue;
//					}
					
					D_DirectoryEntry removed = loaded_peers.removeTail();//remove(loaded_peers.size()-1);
					// loaded_peer_By_LocalID.remove(new Long(removed._peer_ID));
					loaded_peer_By_GID.remove(removed.globalID);
					loaded_peer_By_GIDhash.remove(removed.globalIDhash);
					if(removed.component_node.message != null) current_space -= removed.component_node.message.length;
					if(D_Directory_Node.need_saving_contains(removed.globalIDhash, removed.instance)) rem.add(removed);
				}
			}
			for(D_DirectoryEntry removed : rem){
				try {
					synchronized(D_Directory_Node.saver_monitor){
						if(D_Directory_Node.need_saving_contains(removed.globalIDhash, removed.instance)) removed.update();
					}
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}				
			}
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_DirectoryEntry crt) {
			loaded_peers.moveToFront(crt);
		}
	}
	/**
	 * It loads the whole peer in cache.
	 * Will return null if instance does not exist.
	 * 
	 * Could be optimized in the future to share the globalID among instances, and to
	 * not have all instances necessarily loaded (?)... 
	 * @param globalID
	 * @param globalIDhash
	 * @param instance
	 * @return
	 */
	public static D_DirectoryEntry getEntry(String globalID, String globalIDhash, String instance) {
		 D_DirectoryEntry e = getEntry(globalID, globalIDhash);
		 if(instance == null) return e;
		 D_DirectoryEntry crt = e.instances.get(instance);
		 if(crt == null){
//			 crt = new D_DirectoryEntry();
//			 crt.globalID = globalID;
//			 crt.globalIDhash = globalIDhash;
//			 crt.instance = instance;
//			 e.instances.put(instance, crt);
//			 D_Directory_Node.register_loaded_instance(crt);
		 }
		 return crt;
	}
	/**
	 * 
	 * @param da
	 * @return
	 */
	public static D_DirectoryEntry loadAndSetEntry(DirectoryAnnouncement da, boolean TCP) {
		String globalID = da.globalID;
		String globalIDhash = da.globalIDhash; 
		String instance = da.instance;
		 D_DirectoryEntry e = getEntry(globalID, globalIDhash);
		 if(instance == null){
			 e.load(da, TCP);
			 return e;
		 }
		 D_DirectoryEntry crt = e.instances.get(instance);
		 if(crt == null){
			 crt = new D_DirectoryEntry();
			 crt.parent = e;
			 crt.load(da, TCP);
//			 crt.globalID = globalID;
//			 crt.globalIDhash = globalIDhash;
//			 crt.instance = instance;
			 e.instances.put(instance, crt);
			 D_Directory_Node.register_loaded_instance(crt);
		 }else
			 crt.load(da, TCP);
		 return crt;
	}
	/**
	 * Loads the whole set of instances in memory.
	 * When no instance=null entry is found, returns an empty entry (with only GID or GIDH set)
	 * 
	 * @param globalID
	 * @param globalIDhash
	 * @return
	 */
	public static D_DirectoryEntry getEntry(String globalID, String globalIDhash) {
		D_DirectoryEntry elem;
		if(globalID != null) {
			elem = D_Directory_Node.loaded_peer_By_GID.get(globalID);
			if(elem!=null){
				D_Directory_Node.setRecent(elem);
				return elem;
			}
		
			if(globalIDhash == null)
				globalIDhash = D_PeerAddress.getGIDHashFromGID(globalID);
		}
		elem = D_Directory_Node.loaded_peer_By_GIDhash.get(globalIDhash);
		if(elem !=null){
			D_Directory_Node.setRecent(elem);
			if(globalID!=null) D_Directory_Node.loaded_peer_By_GID.put(globalID, elem);
			return elem;
		}
		elem = new D_DirectoryEntry(globalID, globalIDhash);
		D_Directory_Node.register_loaded(elem);
		return elem;
	}
	
}