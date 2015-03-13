package data;

import static java.lang.System.out;
import static util.Util.__;
import hds.Address;
import hds.DirectoryServer;
import hds.PeerInput;
import hds.Server;
import hds.UDPServer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Pattern;

import util.DD_Address;
import util.P2PDDSQLException;
import util.Util;
import ASN1.Encoder;
import ciphersuits.Cipher;
import ciphersuits.SK;
import config.Application_GUI;
import config.DD;
import config.Identity;

public class HandlingMyself_Peer {
	// handling myself
	private static D_Peer _myself = null;
	/**
	 * Protects the whole process of setting myself
	 *  or changing its instance (which are likely not needed)
	 */
	private static final Object monitor_init_myself = new Object();
	public static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final boolean FINAL = true;
	private static final Object _myself_monitor = new Object();
	private static final long MYSELF_TIMEOUT = 5000;

	/**
	 * returns myself if set,
	 * System.exist if interrupted when un-existing (should never happen!)
	 * 
	 * Otherwise blocks and waits until it is created!
	 * 
	 * @return
	 */
	public static D_Peer get_myself_with_wait() {
		if (DEBUG) System.out.println("HandlingMyself: get_myself: start");
		//Util.printCallPath("Who?");
		synchronized(_myself_monitor) {
			int max = 5;
			while (_myself == null) {
				try {
					if (_DEBUG) System.out.println("HandlingMyself: get_myself: ...");
					if (max-- < 0) { Util.printCallPath(""); max = 10; }
					_myself_monitor.wait(MYSELF_TIMEOUT);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		if (_myself == null) {
			Util.printCallPath("Null myself");
			//return null;
			Application_GUI.warning(__("No identity set!"), __("Identity absent"));
			System.exit(1);
		}
		if (DEBUG) System.out.println("HandlingMyself: get_myself: found");
		return _myself;
	}
	public static D_Peer get_myself_or_null() {
		if (DEBUG) System.out.println("HandlingMyself: get_myself_or_null: start");
		return _myself;
	}
	/*
	public static void announceMyselfToDirectories_UDP_or_TCP() {
		if (DEBUG) out.println("Server: announceMyself_UDP_or_TCP: will announce");
		//DatagramSocket ds = UDPServer.ds; 
		if (UDPServer.isRunning()) UDPServer.announceMyselfToDirectories();
		else UDPServer.announceMyselfToDirectoriesTCP();
		if (DEBUG) out.println("Server: announceMyself_UDP_or_TCP: will set my ID");
	}
	*/
	/**
	 * If param null, use Identity.current_peer_ID (initialized if null)
	 * Loads the identity from the database application fields into: peerGID, peerInstance, peerGIDH.
	 * @param id
	 * @return
	 */
	public static Identity loadIdentity(Identity id) {
		if (DEBUG) out.println("HandlingMyself: loadIdentity: start="+id);
		if (id == null) id = Identity.current_peer_ID;
		if (id == null) id = Identity.current_peer_ID = new Identity();
		try {
			String GID = DD.getAppText(DD.APP_my_global_peer_ID);
			String instance = DD.getAppText(DD.APP_my_peer_instance);
			id.setPeerGID(GID);
			id.peerInstance = instance;
			//id.name = DD.getAppText(DD.APP_my_peer_name);
			//id.slogan = DD.getAppText(DD.APP_my_peer_slogan);
			id.peerGIDH = DD.getAppText(DD.APP_my_global_peer_ID_hash);
			if (DEBUG)
				System.out.println("HandlingMyself_Peer: loadIdentity: found GID="+GID+"\n\t inst="+instance+"\n\t GIDH="+id.peerGIDH+"\n\t ID="+id);
		} catch (P2PDDSQLException e) {}
		if (DEBUG) out.println("HandlingMyself: loadIdentity: done="+id);
		return id;
	}
	private static Identity saveInIdentity(D_Peer me, Identity my_ID) {
		//Identity id = new Identity();
		if (my_ID == null){
			return null;
		}
		my_ID.setPeerGID(me.getGID());
		my_ID.peerGIDH = me.getGIDH_force();
		my_ID.name = me.getName();
		my_ID.slogan = me.getSlogan();
		my_ID.peerInstance = me.getInstance();
		Identity.setAgentWideEmails(me.getEmail());
		if (DD.WARN_ON_IDENTITY_CHANGED_DETECTION) {
			if (me.getName() != null) Application_GUI.warning(__("Now you are:")+" \""+me.getName()+"\"", __("Peer Changed!"));
			else{ Application_GUI.warning(__("Now have an anonymous identity. You have to choose a name!"), __("Peer Changed!"));
			}
		}
		return my_ID;
	}
	/**
	 * Saving from D_Peer, since the duplication in Identity.current_peer should be removed
	 * @param me
	 */
	private static void saveCrtIdentityInDB(D_Peer me) {
		try {
			DD.setAppText(DD.APP_my_global_peer_ID, me.getGID());
			DD.setAppText(DD.APP_my_global_peer_ID_hash, me.getGIDH_force());
			//DD.setAppText(DD.APP_my_peer_name, me.getName());
			//DD.setAppText(DD.APP_my_peer_slogan, me.getSlogan());
			DD.setAppText(DD.APP_my_peer_instance, me.getInstance());		
		} catch (P2PDDSQLException e) {
			Application_GUI.warning(__("Cannot save identity in database. Will exit!"), __("Failed database access!"));
			e.printStackTrace();
			System.exit(1);
		}
	}
	/**
	 * Code to randomly select an instance
	 * @param me
	 * @return
	 */
	static public boolean forceSomeInstance(D_Peer me) {
		if (me == null) return false;
		synchronized (monitor_init_myself) {
			me.loadInstancesToHash();
			if ((me.getInstance() == null) || (!me.containsPeerInstance(me.getInstance()))) {
				for ( D_PeerInstance i : me._instances.values()) {
					if (i.createdLocally()) {
						me.setCurrentInstance(i.peer_instance);
						break;
					}
				}
			}
		}
		return me.getInstance() != null;
	}
	static public String peekSomeInstance(D_Peer me) {
		if (me == null) return null;
		String result = me.getInstance();
		synchronized (monitor_init_myself) {
			me.loadInstancesToHash();
			if ((result == null) || (!me.containsPeerInstance(result))) {
				for ( D_PeerInstance i : me._instances.values()) {
					if (i.createdLocally()) {
						return i.peer_instance;
					}
				}
			}
		}
		return null;
	}
	/**
	 * - save in database
	 * - save in Identity
	 * - save in _myself
	 * - trigger listener in DD.status
	 * - announce_Directories of the change
	 * 
	 * - will warn if no instance set, and possible
	 * 
	 * @param me
	 * @param saveInDB set to false when loading from DB
	 * @param me_kept (is me already kept?)
	 * @return true on success
	 */
	static public boolean setMyself_currentIdentity_announceDirs( D_Peer me, boolean saveInDB, boolean me_kept) {
		return setMyself_announceDirs(me, saveInDB, Identity.current_peer_ID, me_kept);
	}
//	static public boolean setMyself( D_Peer me, boolean saveInDB) {
//		return setMyself(me, saveInDB, Identity.current_peer_ID);
//	}
//	/**
//	 * - save in database
//	 * - save in Identity
//	 * - save in _myself
//	 * - trigger listener in DD.status
//	 * - announce_Directories of the change
//	 * 
//	 * - will warn if no instance set, and possible
//	 *  
//	 * @param me
//	 * @param saveInDB set to false when loading from DB
//	 * @return true on success
//	 */
//	static public boolean setMyself_announceDirs_NoYetKept( D_Peer me, boolean saveInDB, Identity my_ID) {
//		return setMyself_announceDirs (me, saveInDB, my_ID, false);
//	}
	/**
	 * - save in database
	 * - save in Identity
	 * - save in _myself
	 * - trigger listener in DD.status
	 * - announce_Directories of the change
	 * 
	 * - will warn if no instance set, and possible
	 *  
	 * @param me
	 * @param saveInDB set to false when loading from DB
	 * @param my_ID
	 * @param kept (is me already kept)
	 * @return  true on success
	 */
	static public boolean setMyself_announceDirs( D_Peer me, boolean saveInDB, Identity my_ID, boolean kept) {
		return setMyself(me, saveInDB, my_ID, kept, true);
	}
	/**
	 * - save in database
	 * - save in Identity
	 * - save in _myself
	 * - trigger listener in DD.status
	 * - announce_Directories of the change
	 * 
	 * - will warn if no instance set, and possible
	 * 
	 * @param me
	 * @param saveInDB
	 * @param my_ID
	 * @param kept
	 * @param announce_it
	 * @return
	 */
	static public boolean setMyself( D_Peer me, boolean saveInDB, Identity my_ID, boolean kept,
			boolean announce_it) {
		if (DEBUG) System.out.println("HandlingMyself: setMyself: start");
		if (me == null) {
			if (DEBUG) System.out.println("HandlingMyself: setMyself: exit null");
			return false;
		}

		synchronized (monitor_init_myself) {
			SK sk = me.getSK();
			if (sk == null) {
				System.out.println("HandlingMyself_Peer: setMyself: exit since unknown secret key!");
				return false;
			}
			me.loadInstancesToHash();
			if ((me.getInstance() == null) || (!me.containsPeerInstance(me.getInstance()))) {
				for ( D_PeerInstance i : me._instances.values()) {
					if (i.createdLocally() && (i.peer_instance != null)) {
						//me.setCurrentInstance(i.peer_instance);
						//if (DEBUG) Util.printCallPath("Sure I should not have set an instance?\nLocally created is:"+i);
						if (_DEBUG) System.out.println("Sure I should not have set an instance?\nLocally created is:"+i);
						break;
					}
				}
			}
			
			if (saveInDB) {
				saveInIdentity(me, my_ID);
				saveCrtIdentityInDB(me);
			}
			synchronized(_myself_monitor) {
				boolean keeping_here = false;
				if (_myself != null) _myself.dec_StatusReferences(); //if (_myself != null) _myself.releaseReference();
				if (me != null && !kept) {
					me = D_Peer.getPeerByGID_or_GIDhash(null, me.getGIDH_force(), true, false, true, null);
					keeping_here = true;
				}
				if (me != null) {
					me.inc_StatusReferences(); // keep it during the process to avoid it being dropped in-between
					if (keeping_here) me.releaseReference();
				}
				_myself = me;
				_myself_monitor.notifyAll();
			}
			//MyselfHandling._myself = _myself;
			synchronized (DD.status_monitor) {
				Application_GUI.setMePeer(me);
			}
			if (announce_it)
				if (FINAL) UDPServer.announceMyselfToDirectories();
			if (DEBUG) System.out.println("HandlingMyself:setMyself: done");
			return true;
		}
	}
	
	/**
	 * To be called on startUp (used id.globalID and id.instance)
	 * @param id
	 * @return null if none
	 */
	static public D_Peer getPeer(Identity id) {
		if (DEBUG) out.println("HandlingMyself: getPeer: start="+id);
		if (id.getPeerGID() == null) {
			if (DEBUG) out.println("HandlingMyself: getPeer: exit null GID");
			return null;
		}
		D_Peer me = D_Peer.getPeerByGID_or_GIDhash(id.getPeerGID(), null, true, false, false, null);
		if (me == null) {
			if (DEBUG) out.println("HandlingMyself: getPeer: exit peer myself not found");
			return null;
		}
		me.setCurrentInstance(id.peerInstance);
		if (DEBUG) out.println("HandlingMyself: getPeer: done");
		return me;
	}
	
	/**
	 * Create if not already existing.
	 * Does not set new Identity.emails and name in Identity.current_peer_ID.identity!
	 * @param id
	 * @return
	 */
	private static D_Peer createPeer_by_dialog_Keep() {
		if (DEBUG) System.out.println("HandlingMyself:createPeer_by_dialog: start");
		String name = System.getProperty("user.name", __("MySelf"));
		String email = System.getProperty("user.name", __("MySelf"))+"@"+DD.DEFAULT_EMAIL_PROVIDER;
		PeerInput pi = new PeerInput(name, null, email);
		
		PeerInput _data[] = new PeerInput[1];
		D_Peer peer = Application_GUI.createPeer(pi, _data);
		PeerInput data = _data[0];
		if ((data == null) && (peer == null)) return null;
		
		if (peer != null) { // loaded from a file
			if (peer.kept() <= 0 ) D_Peer.getPeerByPeer_Keep(peer);
			//peer.makeNewInstance();
			//Identity.emails = peer.getEmail();
			//if (Identity.current_peer_ID.name == null) Identity.current_peer_ID.name = peer.getName();
		}else{
			//Identity.emails = data.email;
			//if (Identity.current_peer_ID.name == null) Identity.current_peer_ID.name = data.name;
			try {
				peer = createPeerUnsigned_Keep(data);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
//		try {
//			updateAddress(peer);
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
		if (DEBUG) System.out.println("HandlingMyself:createPeer_by_dialog: done");
		return peer;
	}
	private static D_Peer createPeer_by_dialog_inited_Keep(PeerInput pi) {
		if (DEBUG) System.out.println("HandlingMyself:createPeer_by_dialog: start");
		/*
		String name = System.getProperty("user.name", __("MySelf"));
		String email = System.getProperty("user.name", __("MySelf"))+"@"+DD.DEFAULT_EMAIL_PROVIDER;
		PeerInput pi = new PeerInput(name, null, email);
		*/
		
		PeerInput _data[] = new PeerInput[1];
		D_Peer peer = Application_GUI.createPeer(pi, _data);
		PeerInput data = _data[0];
		if ((data == null) && (peer == null)) return null;
		
		if (peer != null) { // loaded from a file
			if (peer.kept() <= 0 ) D_Peer.getPeerByPeer_Keep(peer);
			//peer.makeNewInstance();
			//Identity.emails = peer.getEmail();
			//if (Identity.current_peer_ID.name == null) Identity.current_peer_ID.name = peer.getName();
		}else{
			//Identity.emails = data.email;
			//if (Identity.current_peer_ID.name == null) Identity.current_peer_ID.name = data.name;
			try {
				peer = createPeerUnsigned_Keep(data);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
//		try {
//			updateAddress(peer);
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
		if (DEBUG) System.out.println("HandlingMyself:createPeer_by_dialog: done");
		return peer;
	}
	/**
	 * Creates based on pi (with createPeerUnsigned_Keep(pi)), 
	 * then sets it as myself (with setMyself()),
	 *  updates the address (with updateAddress(peer)), and 
	 *  saves it (and releases it).
	 * 
	 * Calls setMyself.
	 * 
	 * @param pi
	 * @param saveIdentityInIB
	 * @return
	 */
	public static D_Peer createMyselfPeer_w_Addresses(PeerInput pi, boolean saveIdentityInIB) {
		if (DEBUG) System.out.println("HandlingMyself: createMyselfPeer_w_Addresses: start");
		D_Peer peer = null;
		try {
			peer = HandlingMyself_Peer.createPeerUnsigned_Keep(pi);
			if (peer != null) {
				HandlingMyself_Peer.setMyself_currentIdentity_announceDirs(peer, saveIdentityInIB, true); //kept
				HandlingMyself_Peer.updateAddress(peer);
				if (peer.dirty_any()) peer.storeRequest();
				peer.releaseReference();
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}

		if (DEBUG) System.out.println("HandlingMyself: createMyselfPeer_w_Addresses: done");
		return peer;
	}
	public static D_Peer createMyselfPeer_by_dialog_w_Addresses(boolean saveIdentityInIB) {
		return createMyselfPeer_by_dialog_w_Addresses(saveIdentityInIB, Identity.current_peer_ID);
	}
	public static D_Peer createMyselfPeer_by_dialog_w_Addresses(boolean saveIdentityInIB, Identity my_ID) {
		if (DEBUG) System.out.println("HandlingMyself: createMyselfPeer_by_dialog_w_Addresses: start");
		D_Peer peer = null;
		try {
			peer = HandlingMyself_Peer.createPeer_by_dialog_Keep();
			if (DEBUG) System.out.println("HandlingMyself_Peer:createMyselfPeer_by_dialog_w_Addresses: got:"+peer);
			if (peer != null) {
				HandlingMyself_Peer.setMyself_announceDirs(peer, saveIdentityInIB, my_ID, true); //kept
				HandlingMyself_Peer.updateAddress(peer);
				peer.sign();
				peer.storeRequest();
				peer.releaseReference();
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}

		if (DEBUG) System.out.println("HandlingMyself: createMyselfPeer_by_dialog_w_Addresses: done");
		return peer;
	}
	public static D_Peer createMyselfPeer_by_dialog_inited_w_Addresses(boolean saveIdentityInIB, Identity my_ID, PeerInput pi) {
		if (DEBUG) System.out.println("HandlingMyself: createMyselfPeer_by_dialog_w_Addresses: start");
		D_Peer peer = null;
		try {
			peer = HandlingMyself_Peer.createPeer_by_dialog_inited_Keep(pi);
			if (DEBUG) System.out.println("HandlingMyself_Peer:createMyselfPeer_by_dialog_w_Addresses: got:"+peer);
			if (peer != null) {
				HandlingMyself_Peer.setMyself_announceDirs(peer, saveIdentityInIB, my_ID, true); //kept
				HandlingMyself_Peer.updateAddress(peer);
				peer.sign();
				peer.storeRequest();
				peer.releaseReference();
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}

		if (DEBUG) System.out.println("HandlingMyself: createMyselfPeer_by_dialog_w_Addresses: done");
		return peer;
	}
	public static D_Peer _createPeer(PeerInput peerInput) throws P2PDDSQLException {
		D_Peer p = createPeerUnsigned_Keep(peerInput);
		if (p != null) {
			p.sign();
			if (p.dirty_any()) p.storeRequest();
			p.releaseReference();
		}
		return p;
	}	
	/**
	 * Create a new peer, and its keys in "key" table and "application" table. No address is set, but creation and arrival.
	 * Returns it kept and unsigned!
	 * @throws P2PDDSQLException
	 */
	public static D_Peer createPeerUnsigned_Keep(PeerInput peerInput) throws P2PDDSQLException{
		boolean DEBUG = false;
		if (DEBUG) out.println("HandlingMyself: createPeer: start: peerInput ["+peerInput+"]");
		// addresses used just as seed in PRNG
		ArrayList<Address> addresses = Identity.current_server_addresses_list();
	
		if (peerInput.name == null) peerInput.name = System.getProperty("user.name", __("MySelf"));
		String _addresses = Util.concat(addresses, DirectoryServer.ADDR_SEP, __("LocalMachine"));
		
		ciphersuits.Cipher keys;
		keys = Cipher.getCipher(
				peerInput.cipherSuite.cipher,
				peerInput.cipherSuite.hash_alg,
				peerInput.name+"+"+_addresses);
		keys.genKey(peerInput.cipherSuite.ciphersize);
		//if (D_Peer.DEBUG) out.println("DD:createMyPeerID: keys generated");
		String secret_key = Util.getKeyedIDSK(keys);
		//if (D_Peer.DEBUG) out.println("DD:createMyPeerID: secret_key="+secret_key);
		byte[] pIDb = Util.getKeyedIDPKBytes(keys);
		String pGID = Util.getKeyedIDPK(pIDb);
		//if (D_Peer.DEBUG) out.println("DD:createMyPeerID: public_key="+pGID);
		String pGIDhash = Util.getGIDhash(pGID);
		String pGIDname = "PEER:"+peerInput.name+":"+Util.getGeneralizedTime();
		DD.storeSK(keys, pGIDname, pGID, secret_key, pGIDhash);
		
		String pGIDH = D_Peer.getGIDHashFromHash(pGIDhash);
		D_Peer me = D_Peer.getPeerByGID_or_GIDhash(pGID, pGIDH, true, true, true, null);
//		if (me == null) {
//			Util.printCallPath("");
//			System.exit(1);
//		}
		//me = D_Peer.getPeerByPeer_Keep(me);
		if (me == null) {
			Util.printCallPath("");
			System.exit(1);
		}
		//me.setGID(pGID, pGIDH);
		me.setPeerInputNoCiphersuit(peerInput);
		me.setKeys(keys);
		me.setCreationDate();
		me.setArrivalDate();
		me.dirty_main = true;
		/*
		me.loaded_addresses = true;
		me.loaded_globals = true;
		me.loaded_instances = true;
		me.loaded_locals = true;
		me.loaded_basics = true;
		me.loaded_served_orgs = true;
		me.loaded_my_data = true;
		me.loaded_local_agent_state = true;
		me.loaded_local_preferences = true;
		*/
		//me.sign();
		if (DEBUG) out.println("HandlingMyself: createPeer: exit");
		return me;
	}
	/**
	 * Detect and set current address for current identity (only if no current address).
	 * Set only the listing directories.
	 * If no listing directory, then set the current server socket addresses.
	 * 
	 * saves result
	 * 
	 * @param me
	 * @throws P2PDDSQLException 
	 */
	static public boolean updateAddress(D_Peer me) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if (DEBUG) out.println("HandlingMyself: updateAddress: start");
		if ((me == null) || (me.hasAddresses())) {
			if (DEBUG) out.println("HandlingMyself: updateAddress: exit null or have: "+me);
			return false; 
		}
		ArrayList<Address> addresses = Identity.current_server_addresses_list();
		Calendar _creation_date = Util.CalendargetInstance();
		String creation_date = Encoder.getGeneralizedTime(_creation_date);
		
		// try to detect domains
		if ((Identity.getListing_directories_string().size() == 0) &&
				((addresses == null)
						|| (addresses.size() == 0)
						//|| (addresses.split(Pattern.quote(DirectoryServer.ADDR_SEP)).length == 0)
						)
				) {
			try {
				Server.detectDomain();
				addresses = Identity.current_server_addresses_list();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if ((addresses != null)
			&& (Identity.getListing_directories_string().size() == 0)) {
			if (DEBUG) out.println("HandlingMyself: updateAddress: addresses");
			//String address[] = addresses.split(Pattern.quote(DirectoryServer.ADDR_SEP));
			
			D_PeerInstance dpi = me.getPeerInstance(me.getInstance());
			boolean toSign = false;
			if (dpi == null) {
				me.putPeerInstance_setDirty(me.getInstance(), dpi = new D_PeerInstance());
				dpi.peer_instance = me.getInstance();
				dpi.setLID(me.getLIDstr_keep_force(), me.getLID_keep_force());

				dpi.dirty = true;
				dpi.createdLocally = true;
				dpi.setCreationDate();
				me.dirty_instances = true;
				toSign = true;
			}
			else if (!dpi.createdLocally()) {
				dpi.dirty = true;
				dpi.createdLocally = true;
				me.dirty_instances = true;
			}
			if (addresses.size() > 0) {
				if (DEBUG) out.println("HandlingMyself: updateAddress: addrs ="+addresses.size());
				for (int k = 0; k < addresses.size(); k ++) {
					me.addAddress(addresses.get(k), Address.SOCKET,
							_creation_date, creation_date, true, k, true,
							dpi);
				}
				me.dirty_addresses = true;
				me.dirty_instances = true;
				dpi.dirty = true;
				toSign = true;
			}
			if (toSign) dpi.sign(me.getSK());
			//if (peer.dirty_main) peer.sign();
			//if (peer.dirty_any()) peer.storeRequest();
		}
		if (DEBUG) out.println("HandlingMyself: updateAddress: added sock addresses");
		
		if (Identity.getListing_directories_string().size() > 0) {
			int i = 0;
			for (String dir : Identity.getListing_directories_string()) {
				if (DEBUG) out.println("HandlingMyself: updateAddress: add dir="+dir);
				Address d  = new Address(dir);
				String address_dir = d.getStringAddress(); //dir;//.getHostName()+":"+dir.getPort();
				if (D_Peer.DEBUG) out.println("D_PeerAddr:_set_my_pID: dir="+dir+" ->"+address_dir+" via d="+d);
				me.addAddress(address_dir, Address.DIR,
						_creation_date, creation_date,
						true, i, true,
						null);
				i++;
			}
			me.setCreationDate(_creation_date, creation_date);
			me.signMe(); //.sign(DD.getMyPeerSK());
			me.dirty_main = true;
			me.setArrivalDate(_creation_date, creation_date);
		}
		if (me.dirty_any()) {
			me.storeRequest();
		}
		
		Application_GUI.setMePeer(me);
		if (DEBUG) out.println("HandlingMyself: updateAddress: start");
		return true;
	}
	/**
	 * Get the hash of my peer ID from application table
	 * @return
	 */
	public static String getMyPeerIDhash() {
		return get_myself_with_wait().getGIDH_force();
//		try {return DD.getAppText(DD.APP_my_global_peer_ID_hash);
//		} catch (P2PDDSQLException e) {}
//		return null;
	}
	public static String getMyPeerName() {
		return get_myself_with_wait().getName();
//		try {return DD.getAppText(DD.APP_my_peer_name);
//		} catch (P2PDDSQLException e) {}
//		return null;
	}
	/*
			public static void setMyPeerGID(String gID, String gIDhash) {
				if(DD.DEBUG) out.println("\n*********\nDD:setMyPeerID;: start");
				if(Identity.current_peer_ID==null){
					Identity.current_peer_ID = Identity.initMyCurrentPeerIdentity();
				}
				Identity.current_peer_ID.globalID=gID;
				try {
					//byte[] bPK = pk.encode();
					//String gID = Util.byteToHex(bPK);
					//String gID = Util.getKeyedIDPK(pk);
					//String gIDhash = Util.getHash(bPK);
					DD.setAppText(DD.APP_my_global_peer_ID, gID);
					DD.setAppText(DD.APP_my_global_peer_ID_hash, gIDhash);
				} catch (P2PDDSQLException e) {}
				if(DD.DEBUG) out.println("DD:setMyPeerID;: exit");
				if(DD.DEBUG) out.println("*********");
				return;
			}
	*/
			/**
			 * Compute the SK based on my peerID (PK) from cache
			 * calling Util.getStoredSK(GID, null)
			 * @return
			 */
	public static SK getMyPeerSK() {
		return get_myself_with_wait().getSK();
	}
	/**
	 * Get my PK as string from Identity
	 * @return
	 */
	public static String getMyPeerGID() {
		return get_myself_with_wait().getGID();
	}
	public static String getMyPeerID() {
		return get_myself_with_wait().getLIDstr_keep_force();
	}
	public static void setAmIBroadcastable(boolean val){
		get_myself_with_wait().setBroadcastable(val);
	}
	/**
	 * Should use "myself"
	 * @return
	 * @throws P2PDDSQLException 
	 */
	public static DD_Address getMyDDAddress() throws P2PDDSQLException {
		DD_Address myAddress;
		D_Peer me = HandlingMyself_Peer.get_myself_with_wait();
		if(D_Peer.DEBUG) System.out.println("D_Peer:getMyDDAddress: D_Peer: "+me);
		if(!me.verifySignature()) {
			if(D_Peer._DEBUG) System.out.println("D_Peer:getMyDDAddress: fail signature: "+me);
			me.sign(HandlingMyself_Peer.getMyPeerSK());
			me.setCreationDate();
			me.dirty_main = true;
			me.setArrivalDate();
			me.storeRequest();
		}
		if(!me.verifySignature()) {
			Application_GUI.warning(__("Inconsistent identity. Please restart app."), __("Inconsistant Identity"));
			return null;
		}
		myAddress = new DD_Address(me);
		if(D_Peer.DEBUG) System.out.println("D_Peer:getMyDDAddress: DD_Address: "+myAddress);
		return myAddress;
	}
	public static String getMyPeerSlogan() {
		return get_myself_with_wait().getSlogan();
	}
	public static String getMyPeerEmail() {
		return get_myself_with_wait().getEmail();
	}
	public static String getMyPeerInstance() {
		return get_myself_with_wait().getInstance();
	}
}