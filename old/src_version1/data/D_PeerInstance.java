package data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import util.P2PDDSQLException;
import util.Util;
import config.Application;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class D_PeerInstance{
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public String peer_instance_ID;
	public long peer_ID;
	public String _peer_ID;
	public String peer_instance;
	public String plugin_info;
	public String _last_sync_date;
	public Calendar last_sync_date;
	public String _last_reset;
	public Calendar last_reset;
	public String _last_contact_date;
	public Calendar last_contact_date;
	public boolean createdLocally = false;
	
	/**
	 * Not yet implemented flags
	 */
	public boolean dirty, deleted;
	public String toString() {
		String result = "";
		result += " ID="+peer_instance_ID;
		result += " peer_ID="+peer_ID;
		result += " instance="+peer_instance;
		result += " info="+plugin_info;
		result += " sync="+_last_sync_date;
		result += " rst="+_last_reset;
		result += " cnt="+_last_contact_date;
		result += " loc="+createdLocally;
		return result;
	}

	static String sql_peer = 
			"SELECT "+table.peer_instance.fields+
			" FROM "+table.peer_instance.TNAME+
			" WHERE "+table.peer_instance.peer_ID+" = ?;";
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
		if (_DEBUG) System.out.println("D_PeerInstance: store peer_ID="+_peer_ID);
		if (_peer_ID==null) return;
		try {
			Application.db.delete(table.peer_instance.TNAME,
					new String[]{table.peer_instance.peer_ID},
					new String[]{_peer_ID}, _DEBUG);
			if (instances == null) return;
			for (D_PeerInstance i : instances.values()) {
				i.setLID(_peer_ID, peer_ID);
				i.store();
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
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
	private void store() throws P2PDDSQLException {
		if (DEBUG) System.out.println("D_PeerInstance: store starts");
		String[]params = new String[table.peer_instance.FIELDS_NOID];
		params[table.peer_instance.PI_PEER_ID] = _peer_ID;
		params[table.peer_instance.PI_PEER_INSTANCE] = peer_instance;
		params[table.peer_instance.PI_PLUGIN_INFO] = plugin_info;
		params[table.peer_instance.PI_LAST_SYNC_DATE] = _last_sync_date;
		params[table.peer_instance.PI_LAST_RESET] = _last_reset;
		params[table.peer_instance.PI_LAST_CONTACT_DATE] = _last_contact_date;
		params[table.peer_instance.PI_CREATED_LOCALLY] = Util.bool2StringInt(this.createdLocally);
		this.peer_instance_ID = ""+
				Application.db.insert(table.peer_instance.TNAME,
						table.peer_instance.fields_noID_list,
						params, DEBUG);
		if (DEBUG) System.out.println("D_PeerInstance: store gets: "+this.peer_instance_ID);
	}
	public D_PeerInstance(ArrayList<Object> k) {
		this.createdLocally = Util.stringInt2bool(k.get(table.peer_instance.PI_CREATED_LOCALLY), false);
		peer_instance_ID = Util.getString(k.get(table.peer_instance.PI_PEER_ID));
		_peer_ID = Util.getString(k.get(table.peer_instance.PI_PEER_ID));
		peer_ID = Util.lval(_peer_ID, -1);
		peer_instance = Util.getString(k.get(table.peer_instance.PI_PEER_INSTANCE));
		plugin_info = Util.getString(k.get(table.peer_instance.PI_PLUGIN_INFO));
		
		_last_sync_date = Util.getString(k.get(table.peer_instance.PI_LAST_SYNC_DATE));
		last_sync_date = Util.getCalendar(_last_sync_date);
		
		_last_reset = Util.getString(k.get(table.peer_instance.PI_LAST_RESET));
		last_reset = Util.getCalendar(_last_reset);
		
		_last_contact_date = Util.getString(k.get(table.peer_instance.PI_LAST_CONTACT_DATE));
		last_contact_date = Util.getCalendar(_last_contact_date);
	}
	public D_PeerInstance(String instance) {
		this.peer_instance = instance;
	}
	public D_PeerInstance(D_PeerInstance d) {
		peer_instance_ID = d.peer_instance_ID;
		this.createdLocally = d.createdLocally;
		this.dirty = d.dirty;
		this.deleted = d.deleted;
		peer_instance = d.peer_instance;
		plugin_info = d.plugin_info;
		
		_peer_ID = d._peer_ID;
		peer_ID = d.peer_ID;
		
		_last_sync_date = d._last_sync_date;
		last_sync_date = d.last_sync_date;
		
		_last_reset = d._last_reset;
		last_reset = d.last_reset;
		
		_last_contact_date = d._last_contact_date;
		last_contact_date = d.last_contact_date;
	}
	/*
	public static ArrayList<D_PeerInstance> loadInstancesToAL(String _peer_ID) throws P2PDDSQLException {
		ArrayList<D_PeerInstance> result = new ArrayList<D_PeerInstance>();
		ArrayList<ArrayList<Object>> i = Application.db.select(sql_peer, new String[]{_peer_ID}, DEBUG);
		for(ArrayList<Object> k: i){
			D_PeerInstance crt = new D_PeerInstance(k);
			result.add(crt);
		}
		return result;
	}
	*/
	public static Hashtable<String,D_PeerInstance> loadInstancesToHash(String _peer_ID) throws P2PDDSQLException {
		if (DEBUG) System.out.println("D_PeerInstance:loadInstances: start:"+_peer_ID);
		Hashtable<String,D_PeerInstance> result = new Hashtable<String,D_PeerInstance>();
		ArrayList<ArrayList<Object>> i = Application.db.select(sql_peer, new String[]{_peer_ID}, DEBUG);
		for(ArrayList<Object> k: i){
			D_PeerInstance crt = new D_PeerInstance(k);
			result.put(crt.peer_instance, crt);
			if (DEBUG) System.out.println("D_PeerInstance:loadInstances: got:"+crt);
			if (DEBUG) Util.printCallPath("why");
		}
		return result;
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
}