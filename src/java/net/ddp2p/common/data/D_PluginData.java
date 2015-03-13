/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */

package net.ddp2p.common.data;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.plugin_data.PeerConnection;
import net.ddp2p.common.plugin_data.PeerPlugin;
import net.ddp2p.common.plugin_data.PluginMethods;
import net.ddp2p.common.plugin_data.PluginRequest;
import net.ddp2p.common.table.plugin_local_storage;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
class Msg extends ASNObj{
	String id;
	byte[] msg;
	public String toString() {
		return "Msg: (id=\""+id+"\" msg="+Util.byteToHexDump(msg, 10)+")";
	}
	Msg(){}
	Msg(String peer, byte[] _msg) {
		id = peer;
		msg = _msg;
	}
	public Msg(PluginRequest _msg) {
		msg = _msg.msg;
		id = _msg.plugin_GID;
	}
	@Override
	public Msg instance() {
		return new Msg();
	}
	/**
	 * Msg ::= SEQUENCE {
	 * 	id UTF8String,
	 *  msg NULLOCTETSTRING
	 * }
	 */
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(id));
		enc.addToSequence(new Encoder(msg));
		return enc;
	}
	@Override
	public Msg decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		id = d.getFirstObject(true).getString();
		msg = d.getFirstObject(true).getBytes();
		if(d.getFirstObject(false)!=null){
			throw new ASN1DecoderFail("Extraneous elements in Msg message queue");
		}
		return this;
	}
	public static byte getTypeByte() {
		return Encoder.TAG_SEQUENCE;
	}
}
public class D_PluginData extends ASNObj implements PeerConnection{

	private static final boolean DEBUG = false;
	private ArrayList<Msg> msgs_queue = new ArrayList<Msg>();
	
	private static String sync="";
	private static D_PluginData connection = new D_PluginData();
	
	public boolean empty() {
		return (msgs_queue==null)||(msgs_queue.size()==0);
	}
	public String toString() {
		String result = "D_PluginData: ";
		//if(msgs_queue==null) return result + "msgs_queue=null ";
		result += toStringMsg(msgs_queue);
		return result;
	}
	public static String toStringMsg(ArrayList<Msg> msgs_queue){
		if(msgs_queue==null) return "msgs_queue=null ";
		return "msgs_queue=["+Util.nullDiscrimArray(msgs_queue.toArray(new Msg[0]), "---")+"] ";

	}
	
	public D_PluginData() {}
	public D_PluginData(ArrayList<Msg> m) {
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: constructor: start");
		if(m==null){
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: constructor: null m");
			m = new ArrayList<Msg>();
		}
		msgs_queue = m;
	}

	public void addMsg(PluginRequest msg) {
		msgs_queue.add(new Msg(msg));
	}
	
	public static PeerConnection getPeerConnection() {
		return connection;
	}

	/**
	 * D_PluginData ::= SEQUENCE {
	 *   msgs_queue SEQUENCE OF Msg OPTIONAL
	 * }
	 */
	@Override
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		if(msgs_queue!=null) enc.addToSequence(Encoder.getEncoder(msgs_queue.toArray(new Msg[0])));
		return enc;
	}

	@Override
	public D_PluginData decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		msgs_queue = null;
		Decoder dmsg = d.getFirstObject(true);
		if(dmsg!=null) msgs_queue = dmsg.getSequenceOfAL(Msg.getTypeByte(), new Msg());
		if(msgs_queue==null) msgs_queue = new ArrayList<Msg>();
		return this;
	}
	/**
	 * not used, but rather getPluginMessages is used
	 * This procedure should be used to build sync requests and answers, to send data
	 * data in database is emptied
	 * plugins should check that the data really arrived
	 * @param peerID
	 * @return
	 * @throws P2PDDSQLException
	 * @throws ASN1DecoderFail
	 */
	public static Encoder getMessages(String peerID) throws P2PDDSQLException, ASN1DecoderFail {
		Encoder enc;
		ArrayList<Msg> result;
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getMessages: for peerID="+peerID);
		result = getPluginMsg(peerID);
		enc = Encoder.getEncoder(result.toArray(new Msg[0]));
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getMessages: result enc #"+result.size());
		return enc;
	}
	public static ArrayList<Msg> getPluginMsg(String peerID) throws P2PDDSQLException {
		ArrayList<Msg> result = null;
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getPluginMsg: for peerID="+peerID);
		synchronized(sync) {
			try {
				result = getEnqueuedMessages(peerID);
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
			}
			if(result == null){
				if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getPluginMsg: result = null");
				result = new ArrayList<Msg>();
			}
			setEnqueuedMessages(new ArrayList<Msg>(), peerID);
		}
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getPluginMsg: result #"+result.size());
		return result;
	}
	/**
	 * This procedure should be used to build sync requests and answers, to send data
	 * data in database is emptied
	 * plugins should check that the data really arrived
	 * @param peerID
	 * @return
	 * @throws P2PDDSQLException
	 * @throws ASN1DecoderFail
	 */
	public static D_PluginData getPluginMessages(String peerID) throws P2PDDSQLException {
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getPluginMessages: for peerID="+peerID);
		ArrayList<Msg> result = getPluginMsg(peerID);
		D_PluginData pdata = new D_PluginData(result);
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getPluginMessages: return="+pdata);
		return pdata;
	}
	/**
	 * not used, direct distribution
	 * Distribute incoming data to plugins,
	 * to be used with sendMessages()
	 * @param d : a sequence of messages
	 * @throws ASN1DecoderFail
	 */
	public static void distributeToPlugins(Decoder d, String peer_GID) throws ASN1DecoderFail{
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginData: distributeToPlugins: start");
		
		
		ArrayList<Msg> msgs = d.getSequenceOfAL(Encoder.TAG_SEQUENCE, new Msg());
		if(msgs==null){
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginData: distributeToPlugins: null queue");
			return;
		}
		distributeToPlugins(msgs, peer_GID);
	}
	public void distributeToPlugins(String peer_GID) throws ASN1DecoderFail{
		distributeToPlugins(msgs_queue, peer_GID);
	}
	public static void  distributeToPlugins(ArrayList<Msg> msgs_queue, String peer_GID) throws ASN1DecoderFail{
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginData: distributeToPlugins: queueu#="+ msgs_queue.size()+" peerGID="+Util.trimmed(peer_GID));
		for(Msg m : msgs_queue) {
			String id = m.id;
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginData: distributeToPlugins: for pluginGID="+ id);
			PluginMethods plugin = net.ddp2p.common.plugin_data.PluginRegistration.s_methods.get(id);
			if(plugin==null){
				if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginData: distributeToPlugins: no such plugin");
				continue;
			}
			Method pp = plugin.handleReceivedMessage;
			try {
				if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginData: distributeToPlugins: distributed msg ="+((m.msg!=null)?("["+m.msg.length+"]="+Util.byteToHexDump(m.msg,10)):"null"));
				pp.invoke(null, m.msg, peer_GID);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	/**
	 * messages saved in database, along pre-existing data
	 * messages are sent further with getMessages
	 */
	public boolean enqueueForSending(byte[] msg, String peerGID, String plugin_GID) {
		return _enqueueForSending(msg, peerGID, plugin_GID);
	}
	public static boolean _enqueueForSending(byte[] msg, String peerGID, String plugin_GID) {
		String peerID;
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("\nD_PluginData: _enqueueForSending: msg.length="+ msg.length);
		try {
			synchronized(sync) {
				peerID = net.ddp2p.common.data.D_Peer.getLocalPeerIDforGID(peerGID);
				ArrayList<Msg> qm = getEnqueuedMessages(peerID);
				if(qm == null) qm = new ArrayList<Msg>();
				if(qm.size()>PluginRequest.MAX_ENQUEUED_FOR_SENDING){
					if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: _enqueueForSending: previous qm.length="+ qm.size());
					return false;
				}
				qm.add(new Msg(plugin_GID, msg));
				setEnqueuedMessages(qm, peerID);
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return false;
		}
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: _enqueueForSending: Done\n");
		return true;
	}
	/**
	 * Store data in database to be forwarded later to a give peer, overwriting prior data
	 * use enqueueForSending() to append data
	 * @param qm
	 * @param peerID
	 * @throws P2PDDSQLException
	 */
	private static void setEnqueuedMessages(ArrayList<Msg> qm, String peerID) throws P2PDDSQLException {
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: setEnqueuedMessages: for peerID="+peerID+" qm.length="+ qm.size());
		Encoder enc = Encoder.getEncoder(qm.toArray(new Msg[0]));
		byte[] b = enc.getBytes();
		String p = Util.stringSignatureFromByte(b);
		D_Peer peer = D_Peer.getPeerByLID(peerID, true, true);
		if (peer == null) return;
		peer.setPluginsMessage(p);
		peer.storeRequest();
		peer.releaseReference();
		/*
		Application.db.updateNoSync(table.peer.TNAME,
				new String[]{table.peer.plugins_msg},
				new String[]{table.peer.peer_ID},
				new String[]{p,peerID},
				DEBUG || DD.DEBUG_PLUGIN);
		*/
	}
	/**
	 *  Gets data from the database (put there by some plugin) that should be sent to peer
	 * @param peerID
	 * @return
	 * @throws P2PDDSQLException
	 * @throws ASN1DecoderFail
	 */
	private static  ArrayList<Msg> getEnqueuedMessages(String peerID) throws P2PDDSQLException, ASN1DecoderFail {
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getEnqueuedMessages: for peerID="+peerID);
		//String sql="SELECT "+table.peer.plugins_msg+" FROM "+table.peer.TNAME+" WHERE "+table.peer.peer_ID+"=?;";
		//ArrayList<ArrayList<Object>> p = Application.db.select(sql, new String[]{peerID}, DEBUG || DD.DEBUG_PLUGIN);
//		if(p.size()==0){
//			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getEnqueuedMessages: quit: not found peerID="+peerID);
//			return null;
//		}
//		String msgs = Util.getString(p.get(0).get(0));
		D_Peer peer = D_Peer.getPeerByLID_NoKeep(peerID, true);
		if (peer == null) {
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getEnqueuedMessages: quit: not found peerID="+peerID);
			return null;
		}
		String msgs = peer.getPluginsMessage();
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getEnqueuedMessages: retrieved "+msgs+" for peerID="+peerID);
		if(msgs == null){
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getEnqueuedMessages: quit: never enqueued for peerID="+peerID);
			return null;
		}
		byte[] _msgs = Util.byteSignatureFromString(msgs);
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getEnqueuedMessages: retrieved bytes "+Util.stringSignatureFromByte(_msgs)+" for peerID="+peerID);
		if(_msgs == null){
			if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getEnqueuedMessages: quit: failed to extract from peerID="+peerID);
			return null;
		}
		Decoder d = new Decoder(_msgs);
		ArrayList<Msg> result = d.getSequenceOfAL(Encoder.TAG_SEQUENCE, new Msg());
		if(DEBUG || DD.DEBUG_PLUGIN) System.out.println("D_PluginData: getEnqueuedMessages: retrieved #"+result.size()+" val="+toStringMsg(result)+" from peerID="+peerID);
		return result;
	}
	
	/**
	 * To be used by plugins to store data associated with a key
	 */
	@Override
	public boolean storeData(String key, byte[] data, String pluginGID) {
		return _storeData(key, data, pluginGID);
	}
	public static boolean _storeData(String key, byte[] data, String pluginGID) {
		try {
			if(key==null) return false;
			if(pluginGID==null) return false;
			String ID = getLocalPluginIDforGID(pluginGID);
			
			if(data == null){
				Application.db.delete(net.ddp2p.common.table.plugin_local_storage.TNAME,
						new String[]{net.ddp2p.common.table.plugin_local_storage.plugin_ID, net.ddp2p.common.table.plugin_local_storage.plugin_key},
						new String[]{ID, key},
						DEBUG);
			}
			
			String _data = Util.stringSignatureFromByte(data);
			String sql=
				"SELECT "+net.ddp2p.common.table.plugin_local_storage.data+
				" FROM "+net.ddp2p.common.table.plugin_local_storage.TNAME+
				" WHERE "+net.ddp2p.common.table.plugin_local_storage.plugin_ID+"=? AND "+net.ddp2p.common.table.plugin_local_storage.plugin_key+"=?;";
			ArrayList<ArrayList<Object>> d = Application.db.select(sql, new String[]{ID, key}, DEBUG);
			if(d.size()!=0) {
				Application.db.update(net.ddp2p.common.table.plugin_local_storage.TNAME,
						new String[]{net.ddp2p.common.table.plugin_local_storage.data}, 
						new String[]{net.ddp2p.common.table.plugin_local_storage.plugin_ID,net.ddp2p.common.table.plugin_local_storage.plugin_key}, 
						new String[]{_data, ID, key}, DEBUG);
				
				return true;
			}else{
				Application.db.insert(net.ddp2p.common.table.plugin_local_storage.TNAME,
					new String[]{net.ddp2p.common.table.plugin_local_storage.plugin_ID,net.ddp2p.common.table.plugin_local_storage.plugin_key,net.ddp2p.common.table.plugin_local_storage.data}, 
					new String[]{ID, key, _data}, DEBUG);
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static String getLocalPluginIDforGID(String pluginGID) throws P2PDDSQLException {
		String sql = "SELECT "+net.ddp2p.common.table.plugin.plugin_ID+" FROM "+net.ddp2p.common.table.plugin.TNAME+
		" WHERE "+net.ddp2p.common.table.plugin.global_plugin_ID+"=?;";
		ArrayList<ArrayList<Object>> o = Application.db.select(sql, new String[]{pluginGID}, DEBUG);
		if(o.size()==0) return null;
		return Util.getString(o.get(0).get(0));
	}
	/**
	 * To be used by plugin GID to get the gata associated with a given key
	 */
	@Override
	public byte[] getData(String key, String pluginGID) {
		return _getData(key, pluginGID);
	}
	public static byte[] _getData(String key, String pluginGID) {
		String ID;
		if(key==null) return null;
		if(pluginGID==null) return null;
		try {
			ID = getLocalPluginIDforGID(pluginGID);
			return _getLData(key, ID);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * Function to be used by plugins to get the data associated with a given key
	 * @param key
	 * @param pluginID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public byte[] getLData(String key, String pluginID) throws P2PDDSQLException {
		return _getLData(key, pluginID);
	}
	public static byte[] _getLData(String key, String pluginID) throws P2PDDSQLException {
		String sql=
			"SELECT "+net.ddp2p.common.table.plugin_local_storage.data+
			" FROM "+net.ddp2p.common.table.plugin_local_storage.TNAME+
			" WHERE "+net.ddp2p.common.table.plugin_local_storage.plugin_ID+"=? AND "+net.ddp2p.common.table.plugin_local_storage.plugin_key+"=?;";
		ArrayList<ArrayList<Object>> d = Application.db.select(sql, new String[]{pluginID, key}, DEBUG);
		if(d.size()==0) return null;
		String data = Util.getString(d.get(0).get(0));
		return Util.byteSignatureFromString(data);
	}

}
