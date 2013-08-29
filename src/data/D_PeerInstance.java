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

	static String sql_peer = 
			"SELECT "+table.peer_instance.fields+
			" FROM "+table.peer_instance.TNAME+
			" WHERE "+table.peer_instance.peer_ID+" = ?;";
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
	static void store(String _peer_ID, Hashtable<String,D_PeerInstance> instances){
		if(_peer_ID==null) return;
		try {
			Application.db.delete(table.peer_instance.TNAME,
					new String[]{table.peer_instance.peer_ID},
					new String[]{_peer_ID}, DEBUG);
			if(instances == null) return;
			for(D_PeerInstance i : instances.values()){
				i.store();
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	private void store() throws P2PDDSQLException {
		String[]params = new String[table.peer_instance.FIELDS_NOID];
		params[table.peer_instance.PI_PEER_ID] = _peer_ID;
		params[table.peer_instance.PI_PEER_INSTANCE] = peer_instance;
		params[table.peer_instance.PI_PLUGIN_INFO] = plugin_info;
		params[table.peer_instance.PI_LAST_SYNC_DATE] = _last_sync_date;
		params[table.peer_instance.PI_LAST_RESET] = _last_reset;
		params[table.peer_instance.PI_LAST_CONTACT_DATE] = _last_contact_date;
		this.peer_instance_ID = ""+
				Application.db.insert(table.peer_instance.TNAME,
						table.peer_instance.fields_list,
						params, DEBUG);
	}
	public D_PeerInstance(ArrayList<Object> k) {
		peer_instance_ID = Util.getString(k.get(table.peer_instance.PI_PEER_ID));
		_peer_ID = Util.getString(k.get(table.peer_instance.PI_PEER_ID));
		peer_ID = Util.lval(_peer_ID, -1);
		peer_instance = Util.getString(k.get(table.peer_instance.PI_PEER_INSTANCE));
		plugin_info = Util.getString(k.get(table.peer_instance.PI_PLUGIN_INFO));
		_last_sync_date = Util.getString(k.get(table.peer_instance.PI_LAST_SYNC_DATE));
		last_sync_date = Util.getCalendar(_last_contact_date);
		_last_reset = Util.getString(k.get(table.peer_instance.PI_LAST_RESET));
		last_reset = Util.getCalendar(_last_reset);
		_last_contact_date = Util.getString(k.get(table.peer_instance.PI_LAST_CONTACT_DATE));
		last_contact_date = Util.getCalendar(_last_reset);
	}
	public static ArrayList<D_PeerInstance> loadInstancesToAL(String _peer_ID) throws P2PDDSQLException {
		ArrayList<D_PeerInstance> result = new ArrayList<D_PeerInstance>();
		ArrayList<ArrayList<Object>> i = Application.db.select(sql_peer, new String[]{_peer_ID}, DEBUG);
		for(ArrayList<Object> k: i){
			D_PeerInstance crt = new D_PeerInstance(k);
			result.add(crt);
		}
		return result;
	}
	public static Hashtable<String,D_PeerInstance> loadInstancesToHash(String _peer_ID) throws P2PDDSQLException {
		Hashtable<String,D_PeerInstance> result = new Hashtable<String,D_PeerInstance>();
		ArrayList<ArrayList<Object>> i = Application.db.select(sql_peer, new String[]{_peer_ID}, DEBUG);
		for(ArrayList<Object> k: i){
			D_PeerInstance crt = new D_PeerInstance(k);
			result.put(crt.peer_instance, crt);
		}
		return result;
	}
}