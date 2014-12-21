package hds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import config.Application;
import config.Application_GUI;
import data.D_Peer;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;
import util.DDP2P_DoubleLinkedList;
import util.DDP2P_DoubleLinkedList_Node;
import util.DDP2P_DoubleLinkedList_Node_Payload;
import util.P2PDDSQLException;
import util.Util;

public
class DirectoryServerCache{
	private static final boolean DEBUG = false;
	private static DirectoryServerCache.D_Directory_Storage.SaverThread saverThread = null;
	
	private DirectoryServerCache() {
		/*
		if(saverThread == null)
			saverThread = new DirectoryServerCache.D_Directory_Storage.SaverThread();
			*/
	}
	public static void startSaverThread() {
		if (saverThread == null)
			saverThread = new DirectoryServerCache.D_Directory_Storage.SaverThread();
	}
	public static void stopSaverThread() {
		if (saverThread == null) return;
		saverThread.stop = true;
		synchronized(saverThread) {
			saverThread.notify();
		}
		saverThread = null;
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
		 if (instance == null) return e;
		 D_DirectoryEntry crt = e.instances.get(instance);
		 if (crt == null) {
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
	public static D_DirectoryEntry loadAndSetEntry(DirectoryAnnouncement da, boolean TCP_or_UDP) {
		if (DEBUG) System.out.println("DSCache: loadAndSetEntry: start "+da);
		String globalID = da.getGID();
		String globalIDhash = da.getGIDH(); 
		String instance = da.instance;
		D_DirectoryEntry e = getEntry(globalID, globalIDhash);
		if (instance == null) {
			e.load(da, TCP_or_UDP);
			if (DEBUG) System.out.println("DSCache: loadAndSetEntry: loaded root "+e);
			D_Directory_Storage.register_loaded(e);
			return e;
		}
		D_DirectoryEntry crt = e.instances.get(instance);
		if (DEBUG) System.out.println("DSCache: loadAndSetEntry: instance "+crt);
		if (crt == null) {
			crt = new D_DirectoryEntry();
			crt.parent = e;
			crt.load(da, TCP_or_UDP);
			if (DEBUG) System.out.println("DSCache: loadAndSetEntry: loaded new instance "+crt);
//			 crt.globalID = globalID;
//			 crt.globalIDhash = globalIDhash;
//			 crt.instance = instance;
			e.instances.put(instance, crt);
			if (DEBUG) System.out.println("DSCache: loadAndSetEntry: loaded root "+e);
			D_Directory_Storage.register_loaded_instance(crt);
		} else {
			/**
			 * May need a way to update the fact that memory is taken
			 */
			crt.load(da, TCP_or_UDP);
			if (DEBUG) System.out.println("DSCache: loadAndSetEntry: loaded instance "+crt);
		}
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
		//boolean DEBUG = true;
		
		if (DEBUG) System.out.println("DirServCache: getEntry: "+globalID+" GIDH="+globalIDhash);
		
		D_DirectoryEntry elem;
		// Try GID first
		if (globalID != null) {
			elem = D_Directory_Storage.loaded_peer_By_GID.get(globalID);
			if (elem != null) {
				D_Directory_Storage.setRecent(elem);
				if (DEBUG) System.out.println("DirServCache: getEntry found in loaded by GID");
				return elem;
			}
		
			// If GID not found, at least extract GIDH from it.
			if (globalIDhash == null) {
				globalIDhash = D_Peer.getGIDHashFromGID(globalID);
				if (DEBUG) System.out.println("DirServCache: GIDH="+globalIDhash);
			}
		}
		// if not found with GID
		elem = D_Directory_Storage.loaded_peer_By_GIDhash.get(globalIDhash);
		if (elem != null) {
			D_Directory_Storage.setRecent(elem);
			if (globalID != null) D_Directory_Storage.loaded_peer_By_GID.put(globalID, elem);
			if (DEBUG) System.out.println("DirServCache: getEntry found in loaded by GIDH");
			return elem;
		}
		if (DEBUG) System.out.println("DirServCache: getEntry will try to load");
		D_DirectoryEntry _elem = new D_DirectoryEntry(globalID, globalIDhash);
		//_elem.known = false;
		//_elem.root = true;
		D_Directory_Storage.register_loaded(_elem);
		return _elem;
	}
	

	class TurnMessage{
		byte[] data;
	}
	public static class D_DirectoryEntry extends ASNObj implements DDP2P_DoubleLinkedList_Node_Payload
	{
		private static final boolean DEBUG = false;
		private static final boolean _DEBUG = true;
		public D_Directory_Storage component_node = new D_Directory_Storage();
		public DDP2P_DoubleLinkedList_Node<D_DirectoryEntry> ddl_node;
		//public boolean loaded_globals;
		// public String _peer_ID;
		public long registered_ID;
		public String globalID;
		public String globalIDhash;
		public String instance;
		public String branch;
		public String agent_version;
		public String name;
		public byte[] certificate;
		public Address[] addresses;
		public byte[] signature;
		public Calendar timestamp;
		//public boolean need_saving;
		/** set to "this" in the parent itself */
		D_DirectoryEntry parent; 
		/** is this a parent node? */
		boolean root;
		public DIR_Terms_Requested[] instance_terms;
		/**
		 * @return
		 * Returns the first address with pureProtocol set to Address.NAT
		 * Normally there should be only one!
		 * 
		 * Else returns null.
		 */
		public Address getNATAddress() {
			if (addresses == null) return null;
			for (Address a : addresses) {
				if (Util.equalStrings_null_or_not(a.getPureProtocol(), Address.NAT))
					return a;
			}
			return null;
		}
		/**
		 * Pre-Initialized
		 */
		public Hashtable<String,ArrayList<TurnMessage>> messages = new Hashtable<String,ArrayList<TurnMessage>>(); // from_peer, messages
		/**
		 * Pre-Initialized
		 */
		public Hashtable<String, D_DirectoryEntry> instances = new Hashtable<String, D_DirectoryEntry>();
		/**
		 * Does this contain valid data? (typically true if the address is not empty)
		 */
		public boolean known = true; 
		
		public void buildInstanceRequestedTerms(){
			//boolean DEBUG = true;
			if (DEBUG) System.out.println("DirServerCache: buildInstanceRequestedTerms: enter");
			if (instance_terms != null) return;
			String isNull = " IS NULL ;";
			String instanceType = " = ? ;" ;
			if(this.instance==null)
				instanceType= isNull;
			// a specific instance for a specific peer ID [sql1 and sql2]
			String sql1 =
					"SELECT "+table.subscriber.fields_subscribers+
					" FROM "+table.subscriber.TNAME+
					" WHERE "+table.subscriber.GID+" = ?" +
					" AND   "+table.subscriber.instance+instanceType;
			String sql2 =
					"SELECT "+table.subscriber.fields_subscribers+
					" FROM "+table.subscriber.TNAME+
					" WHERE "+table.subscriber.GID_hash+" = ?" +
					" AND   "+table.subscriber.instance+instanceType;
			// all instance for a specific peer ID [sql11 and sql22]
			String sql11 =
					"SELECT "+table.subscriber.fields_subscribers+
					" FROM "+table.subscriber.TNAME+
					" WHERE "+table.subscriber.GID+" = ?" +
					" AND   "+table.subscriber.all_instances+" = '1' ;";
			String sql22 =
					"SELECT "+table.subscriber.fields_subscribers+
					" FROM "+table.subscriber.TNAME+
					" WHERE "+table.subscriber.GID_hash+" = ?" +
					" AND   "+table.subscriber.all_instances+" = '1' ;";
			// default for all instance for all peer IDs [sql111]
			String sql111 =
					"SELECT "+table.subscriber.fields_subscribers+
					" FROM "+table.subscriber.TNAME+
					" WHERE "+table.subscriber.GID+" is NULL" +
					" AND   "+table.subscriber.GID_hash+" is NULL ;";
			String sql =
					"SELECT "+table.subscriber.subscriber_ID+
					" FROM "+table.subscriber.TNAME+" LIMIT 1;";
			

			ArrayList<ArrayList<Object>> d, any;
			try {
				
				//if (d == null || d.size()==0) {
					any = Application.db_dir.select(sql, new String[]{}, DEBUG);
					if ((any == null) || (any.size() == 0)) {
						DIR_Terms_Requested[] terms_any = new DIR_Terms_Requested[1];
						DIR_Terms_Requested t = new DIR_Terms_Requested();
						t.setServeLiberally();
						terms_any[0] = t;
						this.instance_terms = terms_any;
						
						return;
					}
				//}

				
				String params[] = new String[1];
				if (instance != null) {
					params = new String[2];
					params[1] = instance;
				}
				params[0] = this.globalID;
				d = Application.db_dir.select(sql1, params, DEBUG);
				
				if (d == null || d.size()==0) {
					params[0] = this.globalIDhash;
					d = Application.db_dir.select(sql2, params, DEBUG);
				}
				
				if (d == null || d.size()==0)
					d = Application.db_dir.select(sql11, new String[]{this.globalID}, DEBUG);	
				
				if (d == null || d.size()==0)
					d = Application.db_dir.select(sql22, new String[]{this.globalIDhash}, DEBUG);
				
				if (d == null || d.size()==0)
					d = Application.db_dir.select(sql111, new String[]{}, DEBUG);	
					
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				DIR_Terms_Requested[] terms_any = new DIR_Terms_Requested[1];
				DIR_Terms_Requested t = new DIR_Terms_Requested();
				t.setServeLiberally();
				terms_any[0] = t;
				this.instance_terms = terms_any;
				
				return;
			}

			//if (d==null || d.size()==0) return;
			DIR_Terms_Requested[] terms = new DIR_Terms_Requested[d.size()];
			int i=0;
			for (ArrayList<Object> _u : d){
				//D_SubscriberInfo s = new D_SubscriberInfo(_d, Application.db_dir );
				DIR_Terms_Requested t = new DIR_Terms_Requested();
				t.ad = Util.get_int(_u.get(table.subscriber.F_AD));
				t.payment = null; // not yet implemented
				t.plaintext = Util.get_int(_u.get(table.subscriber.F_PLAINTEXT));
				// ask?
				t.services_available = Util.getString(_u.get(table.subscriber.F_MODE)).getBytes();
				t.topic = Util.get_int(_u.get(table.subscriber.F_TOPIC))+"";
				//t.version = 1; // ask??
				terms[i] = t;
			}
			this.instance_terms = terms;
		}
		public String toSummary() {
			return toRecString(false);
		}
		public String toString() {
			return toRecString(true);
		}
		public String toRecString(boolean _parent) {
			String r ="D_DirEntry[";
			r += "\n\t"+"known="+known+" root="+root;
			r += "\n\t"+"ID="+registered_ID;
			r += "\n\t"+"GID="+globalID;
			r += "\n\t"+"GIDH="+globalIDhash;
			r += "\n\t"+"inst="+instance;
			r += "\n\t"+"branch="+branch;
			r += "\n\t"+"agent_version="+agent_version;
			r += "\n\t"+"name="+name;
			r += "\n\t"+"cert="+Util.byteToHexDump(certificate, 20);
			r += "\n\t"+"addr="+Util.concat(addresses, "\n\t\t", "NULL");
			r += "\n\t"+"sign="+Util.byteToHexDump(signature);
			r += "\n\t"+"time="+Encoder.getGeneralizedTime(timestamp);
			if (_parent && (parent != null) && (parent != this)) r += "\n\t"+"parent="+parent.toSummary();
			r += "\n\t"+"instances="+Util.concat(instances.values(), "}{", "NULL")+
					"\n]";
			return r;
		}
		
		/**
		 * 
		 * @param _globalID
		 */
		public D_DirectoryEntry(String _globalID) {
			root = true;
			initGID(_globalID);
		}
		/**
		 * 
		 * @param _globalIDH
		 * @param hash
		 */
		public D_DirectoryEntry(String _globalIDH, boolean hash) {
			root = true;
			initGIDH(_globalIDH);
		}
		/**
		 * 
		 * @param globalID2
		 * @param globalIDhash2
		 */
		public D_DirectoryEntry(String globalID2, String globalIDhash2) {
			if (DEBUG) System.out.println("DirServCache: DirEntry<init> ID="+globalID2+" GIDH="+globalIDhash2);
			root = true;
			known = false;
			if (globalID2 != null) initGID(globalID2);
			else if(globalIDhash2 != null) initGIDH(globalIDhash2);
		}
		/**
		 * 
		 * @param a
		 * @param _parent
		 */
		public D_DirectoryEntry(ArrayList<Object> a,  D_DirectoryEntry _parent) {
			init(a, _parent);
			this.known = true;
		}
		public D_DirectoryEntry() {}
		static class AddressSequence extends ASNObj {
			AddressSequence(Address[] addr) {
				_addr = addr;
			}
			AddressSequence(String _adr) {
				byte adr [] = Util.byteSignatureFromString(_adr);
				try {//Address.getAddresses(_adr);
					decode(new Decoder(adr));
//					Decoder dec = new Decoder(adr).getContent();
//					this.addresses = dec.getSequenceOf(Address.getASN1Type(), new Address[0], new Address());
				} catch (Exception e) {
					//e.printStackTrace();
					Util.printCallPathTop("Address Encoding failed");
					this._addr = Address.getAddresses(_adr);
				}
			}
			Address[] _addr;
			@Override
			public Encoder getEncoder() {
				Encoder enc = new Encoder().initSequence();
				enc.addToSequence(Encoder.getEncoder(_addr));
				return enc;
			}

			@Override
			public AddressSequence decode(Decoder d) throws ASN1DecoderFail {
				Decoder dec = d.getContent();
				this._addr = dec.getSequenceOf(Address.getASN1Type(), new Address[0], new Address());
				return this;
			}
			public String getB64ASN1Addresses() {
				return Util.stringSignatureFromByte(encode());
			}
			public Address[] getAddresses() {
				return _addr;
			}
			
		}
		long update() throws P2PDDSQLException{
			//System.out.println("=================================>update "+this);
			if (registered_ID <= 0) return this.storeNew();
			D_Directory_Storage.need_saving_remove(globalIDhash, instance); //need_saving = false;
			String params[] = new String[table.registered.fields_list.length];
			params[table.registered.REG_GID] = globalID;
			params[table.registered.REG_GID_HASH] = globalIDhash;
			params[table.registered.REG_INSTANCE] = instance;
			params[table.registered.REG_BRANCH] = branch;
			params[table.registered.REG_AGENT_VERSION] = agent_version;
			params[table.registered.REG_NAME] = name;
			params[table.registered.REG_CERT] = (certificate.length == 0)?null:Util.stringSignatureFromByte(certificate);
			
//			Encoder enc = new Encoder().initSequence();
//			enc.addToSequence(Encoder.getEncoder(addresses));
//			byte[]  _adr = new AddressSequence(addresses).getB64ASN1Addresses();//enc.getBytes();
			
			params[table.registered.REG_ADDR] = new AddressSequence(addresses).getB64ASN1Addresses(); //Address.joinAddresses(addresses);
			params[table.registered.REG_SIGN] = (signature.length==0)?null:Util.stringSignatureFromByte(signature);
			if (timestamp == null)
				timestamp = Util.CalendargetInstance();
			params[table.registered.REG_TIME] = Encoder.getGeneralizedTime(timestamp); // (Util.CalendargetInstance().getTimeInMillis()/1000)+"";
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
			D_Directory_Storage.need_saving_remove(globalIDhash, instance); // need_saving = false;
			String params[] = new String[table.registered.fields_noID_list.length];
			params[table.registered.REG_GID] = globalID;
			params[table.registered.REG_GID_HASH] = globalIDhash;
			params[table.registered.REG_INSTANCE] = instance;
			params[table.registered.REG_BRANCH] = branch;
			params[table.registered.REG_AGENT_VERSION] = agent_version;
			params[table.registered.REG_NAME] = name;
			params[table.registered.REG_CERT] = (certificate.length==0)?null:Util.stringSignatureFromByte(certificate);

//			Encoder enc = new Encoder().initSequence();
//			enc.addToSequence(Encoder.getEncoder(addresses));
//			byte[]  _adr = enc.getBytes();
			
			params[table.registered.REG_ADDR] = new AddressSequence(addresses).getB64ASN1Addresses(); //Util.stringSignatureFromByte(_adr); //Address.joinAddresses(addresses);
			params[table.registered.REG_SIGN] = (signature.length==0)?null:Util.stringSignatureFromByte(signature);
			if(timestamp == null)
				timestamp = Util.CalendargetInstance(); //.getTimeInMillis()/1000)+"";
			params[table.registered.REG_TIME] = Encoder.getGeneralizedTime(timestamp);
			
			registered_ID=Application.db_dir.insert(table.registered.TNAME, table.registered.fields_noID_list,
					params);
//					new String[]{table.registered.global_peer_ID,table.registered.certificate,table.registered.addresses,table.registered.signature,table.registered.timestamp},
			return registered_ID;
		}
		int discardMessage () {
			if ((component_node == null) || (component_node.message == null))
				return 0;
			int val = this.component_node.message.length;
			this.component_node.message = null;
			return val;
		}
		void load(D_DirectoryEntry n) {
			if (n.globalID != null) globalID = n.globalID;
			if (n.globalIDhash != null) globalIDhash = n.globalIDhash;
			instance = n.instance;
			branch = n.branch;
			agent_version = n.agent_version;
			name = n.name;
			certificate = n.certificate;
			addresses = n.addresses;
			signature = n.signature;
			timestamp = n.timestamp;
			D_Directory_Storage.need_saving_add(globalIDhash, instance);//need_saving = true;
			known = true;
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
			if(addresses2 == null) return null;
			for(Address a: addresses2) 
				if(Address.NAT.equals(a.pure_protocol)) return a;
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
			if (c != null) {
				c.load(da, tCP);
				return;
			}
			c = new D_DirectoryEntry();
			c.parent = this;
			c.load(da, tCP);
			instances.put(instance, c);
			D_Directory_Storage.register_loaded_instance(c);
		}
		/**
		 * Set needs_saving on only differences
		 * @param da
		 */
		public void load(DirectoryAnnouncement da, boolean TCP) {
			if (DEBUG) System.out.println("DSCache: load: start "+da);
			
			boolean need_saving = false;
			if (da.getGID()!=null) {
				if(!Util.equalStrings_null_or_not(globalID, da.getGID())) {
					need_saving = true;
					globalID = da.getGID();
				}
			}
			if (da.getGIDH() != null) {
				if (!Util.equalStrings_null_or_not(globalIDhash, da.getGIDH())) {
					need_saving = true;
					globalIDhash = da.getGIDH();
				}
			}
			if (!Util.equalStrings_null_or_not(instance, da.instance)) {
				need_saving = true;
				instance = da.instance;
			}
			if (!Util.equalStrings_null_or_not(branch, da.branch)) {
				need_saving = true;
				branch = da.branch;
			}
			String da_agent_version = Util.getVersion(da.agent_version);
			if (!Util.equalStrings_null_or_not(agent_version, da_agent_version)) {
				need_saving = true;
				agent_version = da_agent_version;
			}
			if (!Util.equalStrings_null_or_not(name, da.name)) {
				need_saving = true;
				name = da.name;
			}
			if (!Util.equalBytes_null_or_not(certificate, da.certificate)) {
				need_saving = true;
				certificate = da.certificate;
			}
			if (!Util.equalBytes_null_or_not(signature, da.signature)) {
				need_saving = true;
				signature = da.signature;
			}
			set_NAT(da.address._addresses, get_NAT(addresses, TCP), TCP);
			String _addresses = Address.joinAddresses(addresses);
			String _addresses_ = Address.joinAddresses(da.address._addresses);
			if (!Util.equalStrings_null_or_not(_addresses, _addresses_)) {
				need_saving = true;
				addresses = da.address._addresses;
			}
			timestamp = da.date;
			//need_saving = true;
			if (need_saving) {
				D_Directory_Storage.discardMessage(this);
				D_Directory_Storage.need_saving_add(globalIDhash, instance);
				known = true;
			}
			if (DEBUG) System.out.println("DSCache: load: end "+this);
		}
		private void initGIDH(String _globalIDH) {
			//boolean DEBUG = true;
			if (DEBUG) System.out.println("DirServC: initGIDH "+_globalIDH);
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
			if (DEBUG) System.out.println("DirServC: initGIDH gets: #"+d.size());

			if (d.size() > 0) {
				initAll(d);
				if (DEBUG) System.out.println("DirServC: initGIDH gets: "+this);
			}
			
			if(this.globalIDhash == null) this.globalIDhash = _globalIDH;
		}
		/**
		 * Fills a D_DirEntry and its instances based on the result of a query in registered.fields
		 * @param d
		 */
		public void initAll(ArrayList<ArrayList<Object>> d) {
			this.root = true;
			for (ArrayList<Object> a : d) {
					Object _instance = a.get(table.registered.REG_INSTANCE); 
					if (_instance == null) {
						init(d.get(0), this);
						this.known = true;
						continue;
					}
					this.instances.put(Util.getString(_instance), new D_DirectoryEntry(a, this));
			}
		}
		
		private void initGID(String _globalID) {
			//boolean DEBUG = true;
			if (DEBUG) System.out.println("DirServC: initGID "+_globalID);
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
				this.globalIDhash = D_Peer.getGIDHashFromGID(globalID);
				return;
			}
			if (DEBUG) System.out.println("DirServC: initGID #" + d.size());
			if (d.size() > 0) {
				initAll(d);
				if (DEBUG) System.out.println("DirServC: initGID got " + this);
			}
			
			if (this.globalID == null) this.globalID = _globalID;
			if (this.globalIDhash == null) this.globalIDhash = D_Peer.getGIDHashFromGID(globalID);
		}
		/**
		 * Cleans structure:
		 * removes this from "needs_saving", and sets registered_ID to -1
		 */
		private void clean() {
			registered_ID = -1;
			if (globalIDhash != null)
				D_Directory_Storage.need_saving_remove(globalIDhash, instance); // need_saving = false;
		}
		private void init(ArrayList<Object> d, D_DirectoryEntry _parent) {
			parent = _parent;
			this.registered_ID = Util.lval(d.get(table.registered.REG_ID));
			//this.addresses = TypedAddress.parseStringAddresses(Util.getString(d.get(table.registered.REG_ADDR)));

//			String _adr = Util.getString(d.get(table.registered.REG_ADDR));
//			byte adr [] = Util.byteSignatureFromString(_adr);
//			try {//Address.getAddresses(_adr);
//				Decoder dec = new Decoder(adr).getContent();
//				this.addresses = dec.getSequenceOf(Address.getASN1Type(), new Address[0], new Address());
//			} catch (Exception e) {
//				//e.printStackTrace();
//				Util.printCallPathTop("Address Encoding failed");
//				this.addresses = Address.getAddresses(_adr);
//			}
			this.addresses = new AddressSequence(Util.getString(d.get(table.registered.REG_ADDR))).getAddresses();
			this.certificate = Util.byteSignatureFromString(Util.getString(d.get(table.registered.REG_CERT)));
			this.globalID = Util.getString(d.get(table.registered.REG_GID));
			this.globalIDhash = Util.getString(d.get(table.registered.REG_GID_HASH));
			this.instance = Util.getString(d.get(table.registered.REG_INSTANCE));
			this.branch = Util.getString(d.get(table.registered.REG_BRANCH));
			this.agent_version = Util.getString(d.get(table.registered.REG_AGENT_VERSION));
			this.name = Util.getString(d.get(table.registered.REG_NAME));
			this.signature = Util.byteSignatureFromString(Util.getString(d.get(table.registered.REG_SIGN)));
			this.timestamp = Util.getCalendar(Util.getString(d.get(table.registered.REG_TIME)));
			this.known = true;
			if (timestamp == null) timestamp = Util.CalendargetInstance();
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
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public DDP2P_DoubleLinkedList_Node<DirectoryServerCache.D_DirectoryEntry> set_DDP2P_DoubleLinkedList_Node(
				DDP2P_DoubleLinkedList_Node node) {
			DDP2P_DoubleLinkedList_Node<D_DirectoryEntry> old = this.ddl_node;
			this.ddl_node = node;
			return old;
		}
		@Override
		public DDP2P_DoubleLinkedList_Node<D_DirectoryEntry> get_DDP2P_DoubleLinkedList_Node() {
			return ddl_node;
		}		
	}
	/**
	 * Linked List nodes of directory entries
	 * @author msilaghi
	 *
	 */
	public static class D_Directory_Storage {

		/**
		 * Each agent is hashed by a key (GIDH,instance)
		 * This class allows to test equality
		 * 
		 * @author msilaghi
		 *
		 */
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
		
		/**
		 * Max number of peers
		 */
		public static final int MAX_LOADED_PEERS = 100;
		/**
		 * Number of bytes reserved for storing peers
		 */
		public static final long MAX_PEERS_RAM = 1000000;
		/**
		 * Number required to ensure one stores at least myself and current_peer
		 */
		public static final int MIN_PEERS_RAM = 2;
		/**
		 * Currently loaded peers, ordered by the access time
		 */
		private static DDP2P_DoubleLinkedList<D_DirectoryEntry> loaded_peers = new DDP2P_DoubleLinkedList<D_DirectoryEntry>();
		// private static Hashtable<Long, D_DirectoryEntry> loaded_peer_By_LocalID = new Hashtable<Long, D_DirectoryEntry>();
		/**
		 * Loaded indexed by GID
		 */
		private static Hashtable<String, D_DirectoryEntry> loaded_peer_By_GID = new Hashtable<String, D_DirectoryEntry>();
		/**
		 * Loaded indexed by GIDH
		 */
		private static Hashtable<String, D_DirectoryEntry> loaded_peer_By_GIDhash = new Hashtable<String, D_DirectoryEntry>();
		/**
		 * Space used by this storage (estimation)
		 */
		private static long current_space = 0;
		/**
		 * Used by ServerThread to dequeue and by register_loaded* to enqueue in "needs_saving"
		 */
		private static final Object saver_thread_monitor = new Object();
		private static final boolean DEBUG = false;
		/**
		 * The entries that need saving
		 */
		private static HashSet<KEY> _need_saving = new HashSet<KEY>();
		/**
		 * 
		 * @return
		 */
		private static String dumpDirCache() {
			String s = "[";
			s += loaded_peers.toString();
			s += "]";
			return s;
		}
		/**
		 * If connected:
		 * 		current_space -= elem.discardMessage();
		 * @param elem
		 */
		public static void discardMessage(D_DirectoryEntry elem) {
			int dm = elem.discardMessage();
			if (elem.get_DDP2P_DoubleLinkedList_Node() != null) {
				current_space -= dm;
			}
		}

		/**
		 * Creates a KEY object for a GIDH and instance
		 * @param gIDH
		 * @param instance
		 * @return
		 */
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
		static D_DirectoryEntry need_saving_next() {
			Iterator<KEY> i = D_Directory_Storage._need_saving.iterator();
			if(!i.hasNext()) return null;
			KEY c = i.next();
			String h = c.hash;
			String index = c.instance;
			D_DirectoryEntry r = loaded_peer_By_GIDhash.get(h);
			if (r == null) {
				System.out.println("DirectoryServerCache: need_saving_next null entry "
						+ "needs saving next: "+h);
				System.out.println("DirectoryServerCache: "+dumpDirCache());
				return null;
			}
			if (r.instances == null) {
				System.out.println("DirectoryServerCache:need_saving-next null instances");
				return null;
			}
			if (index == null) return r;
			return r.instances.get(index);
		}
				
		/**
		 * Monitors: 
		 * - saver_thread_monitor for needs_saving
		 * - this for sleeping
		 * @author msilaghi
		 *
		 */
		public static class SaverThread extends util.DDP2P_ServiceThread {
			private static final long SAVER_SLEEP = 100;
			private static final long SAVER_SLEEP_ON_ERROR = 2000;
			boolean stop = false;
			SaverThread() {
				super("Directory Saver", false);
				start ();
			}
			public void _run() {
				for(;;) {
					if(stop) return;
					synchronized(saver_thread_monitor) {
						D_DirectoryEntry de = need_saving_next();
						if (de != null) {
							Application_GUI.ThreadsAccounting_ping("Saving");
							need_saving_remove(de.globalIDhash, de.instance);
							// try 3 times to save
							for (int k=0; k<3; k++) {
								try {
									de.update();
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
							Application_GUI.ThreadsAccounting_ping("Nothing to do!");
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
		//public DDP2P_DoubleLinkedList_Node<D_DirectoryEntry> my_node_in_loaded;

		public D_Directory_Storage(byte[] message)
				//,DDP2P_DoubleLinkedList_Node<D_DirectoryEntry> my_node_in_loaded)
		{
			this.message = message;
			//this.my_node_in_loaded = my_node_in_loaded;
		}
		public D_Directory_Storage() {
		}
		/**
		 * This procedure creates the message (encoding of current) if not yet loaded.
		 * if it was loaded but not fully, now we know its size
		 * @param crt
		 */
		private static void register_fully_loaded(D_DirectoryEntry crt) {
			//assert((crt.component_node.message==null) && (crt.loaded_globals));
			/**
			 * Message already loaded
			 */
			if(crt.component_node.message != null)
				return;
			//if(!crt.loaded_globals) return;
			byte[] message = crt.encode();
			synchronized(loaded_peers) {
				crt.component_node.message = message; // crt.encoder.getBytes();
				if(crt.component_node.message != null)
					current_space += crt.component_node.message.length;
			}
		}
		/**
		 * Add a new instance to an existing peer cache.
		 * 
		 * Since the peer is assumed preloaded in the cache, 
		 * instance not needed in global hashtables.
		 * @param crt
		 */
		public static void register_loaded_instance(D_DirectoryEntry crt) {
			ArrayList<D_DirectoryEntry> rem = new ArrayList<D_DirectoryEntry>();
			synchronized(loaded_peers) {
				loaded_peers.offerFirst(crt);
				if(crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				/**
				 * Disconnect overflowing instances from list, set them in "removed"
				 */
				while ((loaded_peers.size() > MAX_LOADED_PEERS)
						|| (current_space > MAX_PEERS_RAM)) {
					if (loaded_peers.size() <= MIN_PEERS_RAM) break; // at least _crt_peer and _myself
					D_DirectoryEntry removed = loaded_peers.removeTail();//remove(loaded_peers.size()-1);
					if (removed.component_node.message != null) current_space -= removed.component_node.message.length;
					if (D_Directory_Storage.need_saving_contains(removed.globalIDhash, removed.instance)) rem.add(removed);
					else {
						for (D_DirectoryEntry a: removed.instances.values()) {
							if (D_Directory_Storage.need_saving_contains(a.globalIDhash, a.instance)) rem.add(a);
						}
					}
				}
			}
			/**
			 * Do the saving
			 */
			for (D_DirectoryEntry removed : rem) {
				try {
					synchronized(D_Directory_Storage.saver_thread_monitor) {
						if (D_Directory_Storage.need_saving_contains(removed.globalIDhash, removed.instance))
							removed.update();
						for (D_DirectoryEntry a: removed.instances.values()) {
							if (D_Directory_Storage.need_saving_contains(a.globalIDhash, a.instance)) a.update();
						}
					}
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}				
			}
		}
		/**
		 * Here we manage the registered peers:
		 *  - set crt as first in loaded_peers
		 *  it may have been part of the list before but we assume that its message 
		 *  was accounted in current_space
		 *  
		 * @param crt
		 */
		private static void register_loaded(D_DirectoryEntry crt) {
			ArrayList<D_DirectoryEntry> rem = new ArrayList<D_DirectoryEntry>();
			//crt.encoder = crt.getEncoder();
			//if(crt.loaded_globals) crt.component_node.message = crt.encode(); //crt.encoder.getBytes();
			synchronized (loaded_peers) {
				if (DEBUG) System.out.println("DSCache:register_loaded registering "+crt);
				if (crt.get_DDP2P_DoubleLinkedList_Node() == null) {
					boolean result = loaded_peers.offerFirst(crt);
					if (DEBUG) System.out.println("DSCache:register_loaded registered "+result);
					if (DEBUG) System.out.println("DSCache:register_loaded "+loaded_peers);
					// loaded_peer_By_LocalID.put(new Long(crt._peer_ID), crt);
					loaded_peer_By_GID.put(crt.globalID, crt);
					loaded_peer_By_GIDhash.put(crt.globalIDhash, crt);
					if(crt.component_node.message != null) {
						current_space += crt.component_node.message.length;
					}
				} else {
					loaded_peers.moveToFront(crt);
				}
				
				while ((loaded_peers.size() > MAX_LOADED_PEERS)
						|| (current_space > MAX_PEERS_RAM)) {
					if (loaded_peers.size() <= MIN_PEERS_RAM) break; // at least _crt_peer and _myself
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
					if(D_Directory_Storage.need_saving_contains(removed.globalIDhash, removed.instance)) rem.add(removed);
				}
			}
			if (DEBUG) System.out.println("DSCache:register_loaded cleaned "+loaded_peers);
			for(D_DirectoryEntry removed : rem){
				try {
					synchronized(D_Directory_Storage.saver_thread_monitor){
						if(D_Directory_Storage.need_saving_contains(removed.globalIDhash, removed.instance)) removed.update();
					}
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}				
			}
			if (DEBUG) System.out.println("DSCache:register_loaded exit "+loaded_peers);
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_DirectoryEntry crt) {
			loaded_peers.moveToFront(crt);
		}
	}
}