package data;

import hds.ASNPluginInfo;
import hds.Address;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import plugin_data.D_PluginInfo;
import ciphersuits.PK;
import ciphersuits.SK;
import util.P2PDDSQLException;
import util.Util;
import config.Application;
import config.DD;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class D_PeerInstance extends ASNObj {
	static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private String peer_instance_ID;
	private long peer_ID;
	private String _peer_ID;
	
	public String peer_instance;
	public String branch;
	public String agent_version;
	
	private String plugin_info;
	private ASNPluginInfo[] plugin_info_array;
	private String _last_sync_date;
	private Calendar last_sync_date;
	private String _last_reset;
	private Calendar last_reset;
	private String _last_contact_date;
	private Calendar last_contact_date;
	private int objects_synchronized;
	public boolean createdLocally = false;
	
	public ArrayList<Address> addresses = new ArrayList<Address>(); // addresses of this instance (socket)
	public ArrayList<Address> addresses_orig = new ArrayList<Address>(); // addresses of this instance (socket)
	public Calendar creation_date;
	public String _creation_date;
	public byte[] signature;

	public boolean dirty = false;

	/**
	 * Not yet implemented flags
	 */
	public boolean deleted = false;
	private long _peer_instance_ID;
	private int version = 1;
	
	public String toString() {
		String result = "";
		result += " v="+version;
		result += " ID="+peer_instance_ID;
		result += " peer_ID="+peer_ID;
		result += " instance="+peer_instance+" ("+this.peer_instance_ID+")";
		result += " info="+get_PluginInfo();
		result += " info[]="+Util.concat(get_PluginInfoArray(), ":");
		result += " branch="+this.branch;
		result += " av="+this.agent_version;
		result += " sync="+_last_sync_date;
		result += " rst="+_last_reset;
		result += " cnt="+_last_contact_date;
		result += " loc="+createdLocally;
		result += " addresses="+Util.concatA(addresses, "---", "NULL");
		result += " creat="+_creation_date;
		result += " obj="+this.objects_synchronized;
		result += " dty/del="+this.dirty+"/"+this.deleted;
		return result;
	}
	public ASNPluginInfo[] getPluginInfo() {
		return get_PluginInfoArray();
//		try {
//			ASNPluginInfo[] _info = new Decoder(Util.byteSignatureFromString(plugin_info)).getSequenceOf(ASNPluginInfo.getASN1Type(), new ASNPluginInfo[0], new ASNPluginInfo());
//			ASNPluginInfo[] result = _info; //"+Util.concat(_info, ":");
//			return result;
//		} catch (ASN1DecoderFail e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}		
//		return null;
	}

	static String sql_peer = 
			"SELECT "+table.peer_instance.fields+
			" FROM "+table.peer_instance.TNAME+
			" WHERE "+table.peer_instance.peer_ID+" = ?;";
	static String sql_peer_instance = 
			"SELECT "+table.peer_instance.fields+
			" FROM "+table.peer_instance.TNAME+
			" WHERE "+table.peer_instance.peer_ID+" = ? AND "+table.peer_instance.peer_instance+"=?;";
	/*
	static void store(String _peer_ID, ArrayList<D_PeerInstance> instances){
		if(_peer_ID==null) return;
		try {
			Application.db.delete(table.peer_instance.TNAME,
					new String[]{table.peer_instance.peer_ID},
					new String[]{_peer_ID}, DEBUG);
			if(instances == null) return;
			for(D_PeerInstance i : instances){
				i.store();
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	*/
	static void store(String _peer_ID, long peer_ID, Hashtable<String,D_PeerInstance> instances){
		if (DEBUG) System.out.println("D_PeerInstance: store peer_ID="+_peer_ID);
		if (_peer_ID == null) return;
		try {
			Application.db.delete(table.peer_instance.TNAME,
					new String[]{table.peer_instance.peer_ID},
					new String[]{_peer_ID}, DEBUG);
			if (instances == null) return;
			for (D_PeerInstance i : instances.values()) {
				i.setLID(_peer_ID, peer_ID);
				i.set_peer_instance_ID(null, -1); // to avoid an update
				i.store();
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	private void set_peer_instance_ID(String ID, int _ID) {
		this._peer_instance_ID = _ID;
		this.peer_instance_ID = ID;
	}
	long store() throws P2PDDSQLException {
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_PeerInstance: store starts");
		
		String[] params;
		if (this.peer_instance_ID == null)
			params = new String[table.peer_instance.FIELDS_NOID];
		else
			params = new String[table.peer_instance.FIELDS];
		params[table.peer_instance.PI_PEER_ID] = _peer_ID;
		params[table.peer_instance.PI_PEER_INSTANCE] = peer_instance;
		params[table.peer_instance.PI_PEER_BRANCH] = branch;
		params[table.peer_instance.PI_PEER_AGENT_VERSION] = agent_version;
		params[table.peer_instance.PI_PLUGIN_INFO] = get_PluginInfo();
		params[table.peer_instance.PI_OBJECTS_SYNCH] = ""+objects_synchronized;
		params[table.peer_instance.PI_LAST_SYNC_DATE] = _last_sync_date;
		params[table.peer_instance.PI_LAST_RESET] = _last_reset;
		params[table.peer_instance.PI_LAST_CONTACT_DATE] = _last_contact_date;
		params[table.peer_instance.PI_SIGNATURE_DATE] = this._creation_date;
		params[table.peer_instance.PI_SIGNATURE] = Util.stringSignatureFromByte(signature);		
		params[table.peer_instance.PI_CREATED_LOCALLY] = Util.bool2StringInt(this.createdLocally);

		try {
			if (this.peer_instance_ID == null) {
				this._peer_instance_ID =
						Application.db.insert(table.peer_instance.TNAME,
								table.peer_instance.fields_noID_list,
								params, DEBUG);
				this.peer_instance_ID = Util.getStringID(this._peer_instance_ID);
	
			} else {
				params[table.peer_instance.PI_PEER_INSTANCE_ID] = this.peer_instance_ID;
				this._peer_instance_ID = Util.lval(this.peer_instance_ID);
				
				Application.db.update(table.peer_instance.TNAME,					
						table.peer_instance.fields_noID_list,
						new String[]{table.peer_instance.peer_instance_ID},
						params, DEBUG);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("D_PeerInstance: error storing: "+this);
			if (this.peer_instance_ID == null) {
				this._peer_instance_ID =
						Application.db.insert(table.peer_instance.TNAME,
								table.peer_instance.fields_noID_list,
								params, _DEBUG);
				this.peer_instance_ID = Util.getStringID(this._peer_instance_ID);
	
			} else {
				params[table.peer_instance.PI_PEER_INSTANCE_ID] = this.peer_instance_ID;
				
				Application.db.update(table.peer_instance.TNAME,					
						table.peer_instance.fields_noID_list,
						new String[]{table.peer_instance.peer_instance_ID},
						params, _DEBUG);
			}
			throw e;
		}
		if (DEBUG) System.out.println("D_PeerInstance: store gets: "+this.peer_instance_ID);
		return this._peer_instance_ID;
	}
	public D_PeerInstance(ArrayList<Object> k) {
		this.createdLocally = Util.stringInt2bool(k.get(table.peer_instance.PI_CREATED_LOCALLY), false);
		peer_instance_ID = Util.getString(k.get(table.peer_instance.PI_PEER_INSTANCE_ID));
		_peer_ID = Util.getString(k.get(table.peer_instance.PI_PEER_ID));
		peer_ID = Util.lval(_peer_ID, -1);
		peer_instance = Util.getString(k.get(table.peer_instance.PI_PEER_INSTANCE));
		branch = Util.getString(k.get(table.peer_instance.PI_PEER_BRANCH));
		agent_version = Util.getString(k.get(table.peer_instance.PI_PEER_AGENT_VERSION));
		set_PluginInfo(Util.getString(k.get(table.peer_instance.PI_PLUGIN_INFO)));
		
		_last_sync_date = Util.getString(k.get(table.peer_instance.PI_LAST_SYNC_DATE));
		last_sync_date = Util.getCalendar(_last_sync_date);
		
		objects_synchronized = Util.ival(k.get(table.peer_instance.PI_OBJECTS_SYNCH), 0);
		
		_last_reset = Util.getString(k.get(table.peer_instance.PI_LAST_RESET));
		last_reset = Util.getCalendar(_last_reset);
		
		_last_contact_date = Util.getString(k.get(table.peer_instance.PI_LAST_CONTACT_DATE));
		last_contact_date = Util.getCalendar(_last_contact_date);
		
		this._creation_date = Util.getString(k.get(table.peer_instance.PI_SIGNATURE_DATE));
		this.creation_date = Util.getCalendar(this._creation_date);
		
		this.signature = Util.byteSignatureFromString(Util.getString(k.get(table.peer_instance.PI_SIGNATURE)));
	}
	public D_PeerInstance() {
		this.peer_instance = null;
		this.peer_instance_ID = null;
	}
	public D_PeerInstance(String instance) {
		this.peer_instance = instance;
	}
	public static D_PeerInstance getPeerInstance(String peer_ID, String instance) {
		long _peer_ID = Util.lval(peer_ID, -1);
		D_Peer dpa = D_Peer.getPeerByLID_NoKeep(_peer_ID, false);
		D_PeerInstance i;
		if (dpa != null) {
			i = dpa.getPeerInstance(instance);
			if (i != null) return i;
		}
		try {
			ArrayList<ArrayList<Object>> insts = Application.db.select(sql_peer_instance, new String[]{peer_ID, instance}, DEBUG);
			if (insts.size() > 0) return null;
			i = new D_PeerInstance(insts.get(0));
			if (dpa != null) dpa.putPeerInstance_setDirty(i.peer_instance, i);
			return i;
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	public D_PeerInstance(D_PeerInstance d) {
		peer_instance_ID = d.peer_instance_ID;
		this.createdLocally = d.createdLocally;
		this.dirty = d.dirty;
		this.deleted = d.deleted;
		peer_instance = d.peer_instance;
		set_PluginInfo_(d.get_PluginInfo());
		set_PluginInfoArray_(d.get_PluginInfoArray());
		_peer_ID = d._peer_ID;
		peer_ID = d.peer_ID;
		
		_last_sync_date = d._last_sync_date;
		last_sync_date = d.last_sync_date;
		
		_last_reset = d._last_reset;
		last_reset = d.last_reset;
		
		_last_contact_date = d._last_contact_date;
		last_contact_date = d.last_contact_date;
	}
	@Override
	public D_PeerInstance instance() {
		return new D_PeerInstance();
	}
	public static Hashtable<String, D_PeerInstance> deep_clone(
			Hashtable<String, D_PeerInstance> instances) {
		Hashtable<String, D_PeerInstance> result = new Hashtable<String, D_PeerInstance>();
		for (String inst: instances.keySet()) {
			result.put(inst, new D_PeerInstance(instances.get(inst)));
		}
		return result;
	}
	public boolean createdLocally() {
		return createdLocally;
	}
	public static long getInstance(String peer_ID, String instance) {
		D_PeerInstance dPeerInstance = getPeerInstance(peer_ID, instance);
		
		return Util.lval(dPeerInstance.peer_instance_ID);
	}
	public void set_last_sync_date_str(String string) {
		_last_sync_date = string;
		this.last_sync_date = Util.getCalendar(string, null);
	}
	public void set_last_sync_date(Calendar calendar) {
		this.last_sync_date = calendar;
		_last_sync_date = Encoder.getGeneralizedTime(calendar);
	}
	public void set_last_reset(String string) {
		_last_reset = string;
		this.last_reset = Util.getCalendar(string, null);
	}
	public void set_last_contact_date(Calendar calendar) {
		this.last_contact_date = calendar;
		_last_contact_date = Encoder.getGeneralizedTime(calendar);
	}
	public Calendar get_last_sync_date() {
		return last_sync_date;
	}
	public String get_last_sync_date_str() {
		return _last_sync_date;
	}
	public Calendar get_last_reset() {
		return last_reset;
	}
	public Calendar get_last_contact_date() {
		return last_contact_date;
	}
	/**
	 * At least one should be non-null
	 * @param _peer_ID2
	 * @param peer_ID2
	 */
	public void setLID(String _peer_ID2, long peer_ID2) {
		if (_peer_ID2 == null) {
			this._peer_ID = Util.getStringID(peer_ID2);
		} else {
			this._peer_ID = _peer_ID2;
		}
		
		if (peer_ID2 <= 0) {
			this.peer_ID = Util.lval(_peer_ID2);
		} else {
			this.peer_ID = peer_ID2;
		}
	}
	public String get_peer_instance_ID() {
		return peer_instance_ID;
	}
	public long get_peer_instance_ID_long() {
		return this._peer_instance_ID;
	}
	public String get_last_reset_str() {
		return this._last_reset;
	}
	public void set_last_reset(Calendar calendar) {
		this.last_reset = calendar;
		_last_reset = Encoder.getGeneralizedTime(calendar);
	}
	public void setCreationDate() {
		this.creation_date = Util.CalendargetInstance();
		this._creation_date = Encoder.getGeneralizedTime(creation_date);
	}
	public byte[] sign(SK sk) {
		//signature = null;
		Encoder enc = getSignatureEncoder();
		byte []msg = enc.getBytes();
		signature = Util.sign(msg, sk);
		this.dirty = true;
		return signature;
	}
	public boolean verifySignature(PK pk) {
		//byte[] _signature = signature;
		//signature = null;
		if (signature == null) return false;
		Encoder enc = getSignatureEncoder();
		byte []msg = enc.getBytes();
		boolean s = Util.verifySign(msg, pk, signature);
		//signature = _signature;
		return s;
	}
	/**
D_PeerInstance := [AC11] SEQUENCE {
	version INTEGER,
	instance UTF8String,
	creation_date GeneralizedTime,
	branch UTF8String,
	agent_version UTF8String,
	addresses SEQUENCE OF Address OPTIONAL,
	plugin_info UTF8String OPTIONAL,
	signature NULLOCTETSTRING
}
	 */
	public Encoder getSignatureEncoder() {
		switch (version) {
		case 0: return getSignatureEncoder_0();
		case 1: return getSignatureEncoder_0();
		}
		throw new RuntimeException("Unknown version:"+this);
	}
	public Encoder getSignatureEncoder_0() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(peer_instance));
		enc.addToSequence(new Encoder(creation_date));
		enc.addToSequence(new Encoder(branch));
		enc.addToSequence(new Encoder(agent_version));
		enc.addToSequence(Encoder.getEncoder(addresses));
		enc.addToSequence(new Encoder(get_PluginInfo()));
		byte[] _signature = null;
		enc.addToSequence(new Encoder(_signature)); //signature
		return enc.setASN1Type(getASN1Type());
	}
	public Encoder getSignatureEncoder_1() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(peer_instance));
		enc.addToSequence(new Encoder(creation_date));
		enc.addToSequence(new Encoder(branch));
		enc.addToSequence(new Encoder(agent_version));
		if ((addresses != null) && (addresses.size() > 0)) enc.addToSequence(Encoder.getEncoder(addresses).setASN1Type(DD.TAG_AC1));
		if (get_PluginInfo() != null) enc.addToSequence(new Encoder(get_PluginInfo()).setASN1Type(DD.TAG_AP2));
		//if ((signature != null) && (signature.length > 0)) enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC3));
		return enc.setASN1Type(getASN1Type());
	}
	@Override
	public Encoder getEncoder() {
		switch (version) {
		case 0: return getEncoder_0();
		case 1: return getEncoder_1();
		}
		throw new RuntimeException("Unknown version:"+this);
	}
	public Encoder getEncoder_0() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(peer_instance));
		enc.addToSequence(new Encoder(creation_date));
		enc.addToSequence(new Encoder(branch));
		enc.addToSequence(new Encoder(agent_version));
		enc.addToSequence(Encoder.getEncoder(addresses));
		enc.addToSequence(new Encoder(get_PluginInfo()));
		enc.addToSequence(new Encoder(signature));
		return enc.setASN1Type(getASN1Type());
	}
	public Encoder getEncoder_1() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version));
		enc.addToSequence(new Encoder(peer_instance));
		enc.addToSequence(new Encoder(creation_date));
		enc.addToSequence(new Encoder(branch));
		enc.addToSequence(new Encoder(agent_version));
		if ((addresses != null) && (addresses.size() > 0)) enc.addToSequence(Encoder.getEncoder(addresses).setASN1Type(DD.TAG_AC1));
		if (get_PluginInfo() != null) enc.addToSequence(new Encoder(get_PluginInfo()).setASN1Type(DD.TAG_AP2));
		if ((signature != null) && (signature.length > 0)) enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC3));
		return enc.setASN1Type(getASN1Type());
	}
	public void loadRemote(D_PeerInstance in) {
		version = in.version;
		peer_instance = in.peer_instance;
		creation_date = in.creation_date;
		branch = in.branch;
		agent_version = in.agent_version;
		addresses = in.addresses;
		set_PluginInfo_(in.get_PluginInfo());
		set_PluginInfoArray_(in.get_PluginInfoArray());
		signature = in.signature;
		this.dirty = true;
		if (addresses != null) for (Address a: addresses) a.dirty = true;
	}
	public void joinNew(D_PeerInstance in) {
		version = in.version;
		if (in.peer_instance != null) peer_instance = in.peer_instance;
		if (in.creation_date != null) creation_date = in.creation_date;
		branch = in.branch;
		agent_version = in.agent_version;
		if (in.addresses != null) addresses = in.addresses;
		if (in.get_PluginInfo() != null) {
			set_PluginInfo_(in.get_PluginInfo());
			set_PluginInfoArray_(in.get_PluginInfoArray());
		}
		this.dirty = true;
		if (addresses != null) for (Address a: addresses) a.dirty = true;
	}
	public void joinOld(D_PeerInstance in) {
		if (peer_instance == null) peer_instance = in.peer_instance;
		if (creation_date == null) creation_date = in.creation_date;
		//branch = in.branch;
		//agent_version = in.agent_version;
		if (addresses == null) addresses = in.addresses;
		if (get_PluginInfo() == null) {
			set_PluginInfo_(in.get_PluginInfo());
			set_PluginInfoArray_(in.get_PluginInfoArray());
		}
		this.dirty = true;
		if (addresses != null) for (Address a: addresses) a.dirty = true;
	}
	@Override
	public D_PeerInstance decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		version = d.getFirstObject(true).getInteger().intValue();
		switch (version) {
		case 0: return decode_0(d);
		case 1: return decode_1(d);
		}
		throw new RuntimeException("Unknown version:"+this);
	}
	public D_PeerInstance decode_0(Decoder d) throws ASN1DecoderFail {
		peer_instance = d.getFirstObject(true).getString();
		set_creation_date(d.getFirstObject(true).getGeneralizedTime_(), null);
		branch = d.getFirstObject(true).getString();
		agent_version = d.getFirstObject(true).getString();
		addresses = d.getFirstObject(true).getSequenceOfAL(Address.getASN1Type(), new Address());
		set_PluginInfo(d.getFirstObject(true).getString());
		signature = d.getFirstObject(true).getBytes();
		return this;
	}
	public D_PeerInstance decode_1(Decoder d) throws ASN1DecoderFail {
		peer_instance = d.getFirstObject(true).getString();
		set_creation_date(d.getFirstObject(true).getGeneralizedTime_(), null);
		branch = d.getFirstObject(true).getString();
		agent_version = d.getFirstObject(true).getString();
		if (d.getTypeByte() == DD.TAG_AC1) addresses = d.getFirstObject(true).getSequenceOfAL(Address.getASN1Type(), new Address());
		else addresses = new ArrayList<Address>();
		if (d.getTypeByte() == DD.TAG_AP2) {
			set_PluginInfo(d.getFirstObject(true).getString(DD.TAG_AP2));
		}
		if (d.getTypeByte() == DD.TAG_AC3) signature = d.getFirstObject(true).getBytes(DD.TAG_AC3);
		return this;
	}
	public void set_creation_date(String cd, Calendar cl) {
		this._creation_date = cd;
		this.creation_date = cl;
		if (cl == null)
			this.creation_date = Util.getCalendar(cd);
		if (cd == null)
			this._creation_date = Encoder.getGeneralizedTime(cl);
	}
	public static byte getASN1Type() {
		return DD.TAG_AC11;
	}
	public int getNbSyncObjects() {
		return this.objects_synchronized;
	}
	public String get_peer_instance() {
		return peer_instance;
	}
	public String get_PluginInfo() {
		return plugin_info;
	}
	public void set_PluginInfo(String plugin_info) {
		this.plugin_info = plugin_info;
		plugin_info_array = D_PluginInfo.getPluginInfoArray(plugin_info);
	}
	public void set_PluginInfo_(String plugin_info) {
		this.plugin_info = plugin_info;
	}
	public ASNPluginInfo[] get_PluginInfoArray() {
		return plugin_info_array;
	}
	/**
	 * Sets this without setting the "string" version
	 * @param plugin_info_array
	 */
	public void set_PluginInfoArray_(ASNPluginInfo[] plugin_info_array) {
		this.plugin_info_array = plugin_info_array;
	}
	/**
	 * Setting all
	 * @param plugin_info_array
	 */
	public void set_PluginInfoArray(ASNPluginInfo[] plugin_info_array) {
		this.plugin_info_array = plugin_info_array;
		this.plugin_info = D_PluginInfo.getPluginInfoFromArray(plugin_info_array);// Util.stringSignatureFromByte(Encoder.getEncoder(plugin_info_array).getBytes());
	}
	public void set_PluginInfoAndArray(String plugin_info, ASNPluginInfo[] plugin_info_array) {
		this.plugin_info_array = plugin_info_array;
		this.plugin_info = plugin_info;
	}
}