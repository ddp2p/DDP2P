/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Khalid Alhamed and Marius Silaghi
		Author: Khalid Alhamed and Marius Silaghi: msilaghi@fit.edu
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList_Node;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList_Node_Payload;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

public class D_RecommendationOfTester  implements  DDP2P_DoubleLinkedList_Node_Payload<D_RecommendationOfTester> {

	public static boolean DEBUG = false;
	
	public long recommendationID; //the pseudokey (Primary Key)
	public long senderPeerLID;// source_Peer local id
	public long testerLID;//tester local id
	public String weight;   // score
	public float _weight;   // score
	public String address; //address where the tester can be contacted
	public Calendar creation_date; //the timestamp of the given testers rating
	public Calendar arrival_date; //the timestamp of the given testers rating
	public byte[] signature; //only for one record at a time
	
	public String toString() {
		return this.toTXT();
	}
	public String toTXT() {
		String result ="";
		result += "recommendationID=" +this.recommendationID+"\r\n";
		result += "peerLID=" +this.senderPeerLID+"\r\n";
		result += "weight="+this.weight+"\r\n";
		result += "address="+this.address+"\r\n";
		result += "testerLID="+this.testerLID+"\r\n";
		result += "creation_date="+Encoder.getGeneralizedTime(this.creation_date)+"\r\n";
		return result;
	}
	private D_RecommendationOfTester(long peer_LID, long tester_LID, boolean create) throws P2PDDSQLException {
		try {
			init(peer_LID, tester_LID);
		} catch (Exception e) {
			//e.printStackTrace();
			if (create) {
				if (_DEBUG) System.out.println("D_RecommendationOfTester: <init>: create gid="+peer_LID + "," + tester_LID);
				initNew(peer_LID, tester_LID);
				if (_DEBUG) System.out.println("D_RecommendationOfTester: <init>: got gid obj="+this);
				return;
			}
			throw new P2PDDSQLException(e.getLocalizedMessage());
		}
	}
	private D_RecommendationOfTester(long recommendation_LID) throws P2PDDSQLException {
		try {
			init(recommendation_LID);
		} catch (Exception e) {
			//e.printStackTrace();
			throw new P2PDDSQLException(e.getLocalizedMessage());
		}
	}
	
	private static final String sql_load_all_senderPeerIDs_from_RecOT = 
			"SELECT DISTINCT "+net.ddp2p.common.table.recommendation_of_tester.senderPeerLID 
		  + " FROM "+net.ddp2p.common.table.recommendation_of_tester.TNAME+" ;";
	final static int SELECT_PEER_LID = 0;
	/**
	 * Returns all recommenderID. (Later one by one get them from cache with getByLID())
	 * 
	 * @return
	 */
	public static ArrayList<Long> retrieveAllRecommendationSenderPeerLID() {
		ArrayList<Long> result = new ArrayList<Long>();
		ArrayList<ArrayList<Object>> obj;
		try {
			obj = Application.db.select(sql_load_all_senderPeerIDs_from_RecOT, new String[] {}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return result;
		}
		//if (obj.size() <= 0) throw new  Exception("D_RecommendationOfTester: retrieveAllTestersLIDs:");
		//init(obj.get(0));
		for(int i=0; i<obj.size(); i++)
			result.add(Util.lval(obj.get(i).get(SELECT_PEER_LID), -1));
		if(DEBUG) System.out.println("D_RecommendationOfTester: retrieveAllRecommendationSenderPeerLID: got="+result.size());//result);
		return result;
	}
	
	private static final String sql_load_all_testersIDs_from_RecOT = 
			"SELECT DISTINCT "+net.ddp2p.common.table.recommendation_of_tester.testerLID 
		  + " FROM "+net.ddp2p.common.table.recommendation_of_tester.TNAME+" ;";
	final static int SELECT_TESTER_LID = 0;
	
	/**
	 * Returns all recommenderID. (Later one by one get them from cache with getByLID())
	 * 
	 * @return
	 */
	public static ArrayList<Long> retrieveAllTestersLIDFromRecievedRecommendation() {
		ArrayList<Long> result = new ArrayList<Long>();
		ArrayList<ArrayList<Object>> obj;
		try {
			obj = Application.db.select(sql_load_all_testersIDs_from_RecOT, new String[] {}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return result;
		}
		//if (obj.size() <= 0) throw new  Exception("D_RecommendationOfTester: retrieveAllTestersLIDs:");
		//init(obj.get(0));
		for(int i=0; i<obj.size(); i++)
			result.add(Util.lval(obj.get(i).get(SELECT_TESTER_LID), -1));
		if(DEBUG) System.out.println("D_RecommendationOfTester: retrieveAllRecommendationTestersLID: got="+result.size());//result);
		return result;
	}
	private static final String sql_load_allRecOT = 
			"SELECT "+net.ddp2p.common.table.recommendation_of_tester.recommendationID 
			+ " FROM "+net.ddp2p.common.table.recommendation_of_tester.TNAME+" ;";
	final static int SELECT_ID = 0;
	
	/**
	 * Returns all recommenderID. (Later one by one get them from cache with getByLID())
	 * 
	 * @return
	 */
	public static ArrayList<Long> retrieveAllRecommendationTestersLIDs() {
		ArrayList<Long> result = new ArrayList<Long>();
		ArrayList<ArrayList<Object>> obj;
		try {
			obj = Application.db.select(sql_load_allRecOT, new String[] {}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return result;
		}
		//if (obj.size() <= 0) throw new  Exception("D_RecommendationOfTester: retrieveAllTestersLIDs:");
		//init(obj.get(0));
		for(int i=0; i<obj.size(); i++)
			result.add(Util.lval(obj.get(i).get(SELECT_ID), -1));
		if(DEBUG) System.out.println("D_RecommendationOfTester: retrieveAllTestersLIDs: got="+result.size());//result);
		return result;
	}
	private static final String sql_load_allRecOTByPeer = 
			"SELECT "+net.ddp2p.common.table.recommendation_of_tester.recommendationID 
			+ " FROM "+net.ddp2p.common.table.recommendation_of_tester.TNAME+
			  " WHERE "+net.ddp2p.common.table.recommendation_of_tester.senderPeerLID+"=?;";
	static final int SELECT_ID_BYSENDER = 0;
	
	public static ArrayList<Long> retrieveAllRecommendationTestersLIDs(String pLID) {
		ArrayList<Long> result = new ArrayList<Long>();
		ArrayList<ArrayList<Object>> obj;
		try {
			obj = Application.db.select(sql_load_allRecOTByPeer, new String[] {pLID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return result;
		}
		//if (obj.size() <= 0) throw new  Exception("D_RecommendationOfTester: retrieveAllTestersLIDs:");
		//init(obj.get(0));
		if(DEBUG) System.out.println("D_RecommendationOfTester: retrieveAllTestersLIDs: got="+obj.size());
		for(int i = 0; i < obj.size(); i ++)
			result.add(Util.lval(obj.get(i).get(SELECT_ID_BYSENDER), -1));
		//if(DEBUG) System.out.println("D_RecommendationOfTester: retrieveAllTestersLIDs: got="+result.size());//result);
		if(DEBUG){
			System.out.println("D_RecommendationOfTester: retrieveAllTestersLIDs: got="+result.size());
			for(int i=0; i<result.size();i++)
				System.out.print("D_RecommendationOfTester: retrieveAllTestersLIDs: got[="+result.get(i)+" ,");
			System.out.println("]");
		}
		
		return result;
	}
	
	public static ArrayList<D_RecommendationOfTester> retrieveAllRecommendationTestersKeep(String pLID) {
		ArrayList<Long> lids = retrieveAllRecommendationTestersLIDs(pLID);
		ArrayList<D_RecommendationOfTester> result = new ArrayList<D_RecommendationOfTester>();
		for (Long rLID : lids) {
			D_RecommendationOfTester o;
			result.add(o=D_RecommendationOfTester.getRecommendationOfTesterByLID(rLID, true));
			System.out.println("D_RecommendationOfTester: retrieveAllRecommendationTestersKeep: got "+o);
		}
		return result;
	}
	private static final String sql_load_recOT = 
			"SELECT "+net.ddp2p.common.table.recommendation_of_tester.fields_recommendationOfTester 
			+ " FROM "+net.ddp2p.common.table.recommendation_of_tester.TNAME
			+ " WHERE "+net.ddp2p.common.table.recommendation_of_tester.senderPeerLID+"=? "
			+ " AND "+net.ddp2p.common.table.recommendation_of_tester.testerLID+"=?;";
	public void init(long peer_LID, long tester_LID) throws Exception {
		ArrayList<ArrayList<Object>> obj = Application.db.select(sql_load_recOT, new String[] {Util.getStringID(peer_LID), Util.getStringID(tester_LID)}, DEBUG);
		if (obj.size() <= 0) throw new  Exception("D_RecommendationOfTester: init:None for GID="+peer_LID+","+tester_LID);
		init(obj.get(0));
		if(DEBUG) System.out.println("D_RecommendationOfTester: init: got="+this);//result);
	}
	private static final String sql_load_recOTBtLID = 
			"SELECT "+net.ddp2p.common.table.recommendation_of_tester.fields_recommendationOfTester 
			+ " FROM "+net.ddp2p.common.table.recommendation_of_tester.TNAME
			+ " WHERE "+net.ddp2p.common.table.recommendation_of_tester.recommendationID+"=?;";
	private void init(long recommendation_LID) throws Exception {
		ArrayList<ArrayList<Object>> obj = Application.db.select(sql_load_recOTBtLID, new String[] {Util.getStringID(recommendation_LID)}, DEBUG);
		if (obj.size() <= 0) throw new  Exception("D_RecommendationOfTester: init: None for LID="+recommendation_LID);
		init(obj.get(0));
		if(DEBUG) System.out.println("D_RecommendationOfTester: init: got="+this);//result);
	}
	/**
	 * TODO
	 * add rest of fields
	 * @param a
	 */
	private void init(ArrayList<Object> a) {
		this.weight = Util.getString(a.get(net.ddp2p.common.table.recommendation_of_tester.F_WEIGH));
		this._weight = Util.fval(this.weight, 0.0f);
		this.creation_date = Util.getCalendar(Util.getString(a.get(net.ddp2p.common.table.recommendation_of_tester.F_CREATION_DATE)));
		this.arrival_date  = Util.getCalendar(Util.getString(a.get(net.ddp2p.common.table.recommendation_of_tester.F_ARRIVAL_DATE)));
		this.address = Util.getString(a.get(net.ddp2p.common.table.recommendation_of_tester.F_ADDRESS));
		this.recommendationID = Util.lval(a.get(net.ddp2p.common.table.recommendation_of_tester.F_ID), -1);
		this.senderPeerLID = Util.lval(a.get(net.ddp2p.common.table.recommendation_of_tester.F_SENDER_PEER_LID), -1);
		this.testerLID = Util.lval(a.get(net.ddp2p.common.table.recommendation_of_tester.F_TESTER_LID), -1);
		this.signature = Util.byteSignatureFromString(Util.getString(a.get(net.ddp2p.common.table.recommendation_of_tester.F_SIGNATURE))); 
	}
	static public D_RecommendationOfTester getRecommendationOfTesterByLID(Long LID, boolean keep) {
		// boolean DEBUG = true;
		if (DEBUG) System.out.println("D_RecommendationOfTester: getRecommendationOfTesterByLID: "+LID);
		if ((LID == null) || (LID <= 0)) {
			if (DEBUG) System.out.println("D_RecommendationOfTester: getRecommendationOfTesterByLID: null LID = "+LID);
			if (DEBUG) Util.printCallPath("?");
			return null;
		}
		D_RecommendationOfTester crt = D_RecommendationOfTester.getByLID_AttemptCacheOnly(LID, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_RecommendationOfTester: getRecommendationOfTesterByLID: got GID cached crt="+crt);
			return crt;
		}
		//crt.releaseReference();
		synchronized (monitor_object_factory) {
			crt = D_RecommendationOfTester.getByLID_AttemptCacheOnly(LID, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_RecommendationOfTester: getRecommendationOfTesterByLID: got sync cached crt="+crt);
				return crt;
			}

			try {
				crt = new D_RecommendationOfTester(LID);
				if (DEBUG) System.out.println("D_RecommendationOfTester: getRecommendationOfTesterByLID: loaded crt="+crt);
				D_RecommendationOfTester_Node.register_loaded(crt);
				if (keep) {
					crt.status_references ++;
				}
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_RecommendationOfTester: getRecommendationOfTesterByLID: error loading");
				// e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_RecommendationOfTester: getRecommendationOfTesterByLID: Done");
			return crt;
		}
	}	
	private static D_RecommendationOfTester getByLID_AttemptCacheOnly(Long LID,
			boolean keep) {
		if (LID == null) return null;
		if (keep) {
			synchronized (monitor_object_factory) {
				D_RecommendationOfTester  crt = getByLID_AttemptCacheOnly(LID.longValue());
				if (crt != null) {			
					crt.status_references ++;
					if (crt.status_references > 1) {
						System.out.println("D_RecommendationOfTester: getByLID_AttemptCacheOnly: "+crt.status_references);
						Util.printCallPath("");
					}
				}
				return crt;
			}
		} else {
			return getByLID_AttemptCacheOnly(LID.longValue());
		}
	}
	private static D_RecommendationOfTester getByLID_AttemptCacheOnly(
			long LID) {
		if (LID <= 0) return null;
		Long id = new Long(LID);
		D_RecommendationOfTester crt = D_RecommendationOfTester_Node.loaded_By_LocalID.get(id);
		if (crt == null) return null;
		
		D_RecommendationOfTester_Node.setRecent(crt);
		return crt;
	}
	public static D_RecommendationOfTester getRecommendationOfTester(long peer_LID, long tester_LID,
			boolean create, boolean keep, D_RecommendationOfTester storage) {
		boolean load_Globals = true;
		String GID = getGID(peer_LID, tester_LID);
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_RecommendationOfTester: getJustByGID: "+GID+" glob="+load_Globals+" cre="+create+" _peer="+peer_LID);
		if ((GID == null)) {
			if (_DEBUG) System.out.println("D_RecommendationOfTester: getJustByGID: null GID and GIDH");
			return null;
		}
		if (create) keep = true;
		D_RecommendationOfTester crt = D_RecommendationOfTester.getROTByGID_AttemptCacheOnly(peer_LID, tester_LID, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_RecommendationOfTester: getJustByGID: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_RecommendationOfTester.getROTByGID_AttemptCacheOnly(peer_LID, tester_LID, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_RecommendationOfTester: getJustByGID: got sync cached crt="+crt);
				return crt;
			}

			try {
				if (storage == null)
					crt = new D_RecommendationOfTester(peer_LID, tester_LID, create);
				else {
					D_RecommendationOfTester_Node.dropLoaded(storage, true);
					crt = storage.initNew(peer_LID, tester_LID);
				}
				if (DEBUG) System.out.println("D_RecommendationOfTester: getJustByGID: loaded crt="+crt);
				if (keep) {
					crt.status_references ++;
				}
				D_RecommendationOfTester_Node.register_loaded(crt);
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_RecommendationOfTester: getJustByGID: error loading");
				if (DEBUG) e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_RecommendationOfTester: getJustByGID: Done");
			return crt;
		}
	}
	private D_RecommendationOfTester initNew(long peer_LID, long tester_LID) {
		this.senderPeerLID = peer_LID;
		this.testerLID = tester_LID;
		this.dirty_main = true;
		return this;
	}
	public static D_RecommendationOfTester getROTByGID_AttemptCacheOnly(
			long peer_LID, long tester_LID, boolean keep) {
		//if ((GID == null)) return null;
		if (keep) {
			synchronized (monitor_object_factory) {
				D_RecommendationOfTester  crt = getROTByGID_AttemptCacheOnly(peer_LID, tester_LID);
				if (crt != null) {			
					crt.status_references ++;
					if (crt.status_references > 2) {
						System.out.println("D_RecommendationOfTester: getROTByGID_AttemptCacheOnly: "+crt.status_references);
						//Util.printCallPath("");
						Util.printCallPathTop("Why references increase for GID="+peer_LID+","+tester_LID);
					}
				}
				return crt;
			}
		} else {
			return getROTByGID_AttemptCacheOnly(peer_LID, tester_LID);
		}
	}
	public static D_RecommendationOfTester getROTByGID_AttemptCacheOnly(
			long peer_LID, long tester_LID) {
		D_RecommendationOfTester  crt = null;
		String GID = getGID(peer_LID, tester_LID);
		//if ((GID == null)) return null;
		if ((GID != null)) crt = D_RecommendationOfTester_Node.getByGID(GID); //.loaded_const_By_GID.get(GID);
		
				
		if (crt != null) {
			//crt.setGID(GID, crt.getOrganizationID());
			
//			if (! crt.loaded_globals) {
//				crt.fillGlobals();
//				D_RecommendationOfTester_Node.register_fully_loaded(crt);
//			}
			D_RecommendationOfTester_Node.setRecent(crt);
			return crt;
		}
		return null;
	}
	void storeAct() throws P2PDDSQLException {
		
		this.clear_dirty();
		if (this.isDeleteFlag()) {
			if(DEBUG)System.out.println("Try to delete:"+ recommendationID);
			if (recommendationID > 0)
				Application.db.delete(true, net.ddp2p.common.table.recommendation_of_tester.TNAME,
						new String[] {net.ddp2p.common.table.recommendation_of_tester.recommendationID},
						new String[]{this.recommendationID+""}, DEBUG);
			this.releaseReference();
			D_RecommendationOfTester_Node.dropLoaded(this, true);
			
			return;
		}
		
		boolean update;
		
		//D_RecommentationOfTester testerRecom = new D_RecommentationOfTester(this.senderPeerLID, this.testerLID);
		if (recommendationID > 0) {
			update = true;
			//recommendationID = testerRecom.recommendationID;
		}
		else update = false;
		
		String params[] = new String[net.ddp2p.common.table.recommendation_of_tester.F_FIELDS];

		params[net.ddp2p.common.table.recommendation_of_tester.F_SENDER_PEER_LID] = Util.getStringID(this.senderPeerLID);
		params[net.ddp2p.common.table.recommendation_of_tester.F_TESTER_LID] = Util.getStringID(this.testerLID);
		params[net.ddp2p.common.table.recommendation_of_tester.F_WEIGH] = this.weight;
		params[net.ddp2p.common.table.recommendation_of_tester.F_ADDRESS] = this.address;
		params[net.ddp2p.common.table.recommendation_of_tester.F_CREATION_DATE] = Encoder.getGeneralizedTime(this.creation_date);
		params[net.ddp2p.common.table.recommendation_of_tester.F_SIGNATURE] = Util.stringSignatureFromByte(this.signature);
		params[net.ddp2p.common.table.recommendation_of_tester.F_ARRIVAL_DATE] = Encoder.getGeneralizedTime(this.arrival_date);
		params[net.ddp2p.common.table.recommendation_of_tester.F_ID] = Util.getStringID(this.recommendationID);
		
		
		if (update) {
			Application.db.update(net.ddp2p.common.table.recommendation_of_tester.TNAME, net.ddp2p.common.table.recommendation_of_tester._fields_recommendationOfTester_no_ID,
					new String[]{net.ddp2p.common.table.recommendation_of_tester.recommendationID},
					params,DEBUG);
		}else{
			//params[table.tester.F_ID] = Util.getStringID(this.recommendationID);
			
			String params2[]=new String[net.ddp2p.common.table.recommendation_of_tester.F_FIELDS_NOID];
			System.arraycopy(params,0,params2,0,params2.length);
			if(DEBUG)System.out.println("params2[last]: "+ params2[net.ddp2p.common.table.recommendation_of_tester.F_FIELDS_NOID-1]);
			setLID_AndLink(Application.db.insert(net.ddp2p.common.table.recommendation_of_tester.TNAME, net.ddp2p.common.table.recommendation_of_tester._fields_recommendationOfTester_no_ID,params2, DEBUG));
			if(DEBUG)System.out.println("this.recommendationID: "+ this.recommendationID);
		}
		
	}
	private void setLID_AndLink(long recommendationID2) {
		setLID(recommendationID2);
		if (this.recommendationID > 0)
			D_RecommendationOfTester_Node.register_newLID_ifLoaded(this);
	}
	private void setLID(long recommendationID2) {
		this.recommendationID = recommendationID2;
	}
	public void clear_dirty() {
		this.dirty_main = false;
	}
	//crt.loadRemote(testerRating, this, (k==0)?this.signature:null);
	public boolean loadRemote(D_TesterRatingByPeer remoteRating,
			D_RecommendationOfTestersBundle remoteMsg, byte[] signature) {
		// ignore old recommendations
		if(remoteMsg.creation_date != null && this.creation_date!=null && !this.creation_date.before(remoteMsg.creation_date)){
			// keep with no change
			if(DEBUG) System.out.println("Recom keep with no change: ");
			return false;
		}
		
		this.weight = remoteRating.weight;
		this._weight = remoteRating._weight;
		this.creation_date = remoteMsg.creation_date;
		this.address = remoteRating.address;
		this.signature = signature;
		this.arrival_date = null;
		this.dirty_main = true;
		
		if(DEBUG) System.out.println("Rget new rec: "+ this);
		
		return true;
	}
	public void storeRequest() {
		
		//Util.printCallPath("Why store?");
		
		if (! this.dirty_any()) {
			Util.printCallPath("Why store when not dirty?");
			return;
		}

		if (this.arrival_date == null && (this.getGID() != null)) {
			this.arrival_date = Util.CalendargetInstance();
			if (_DEBUG) {
				System.out.println("D_RecommentationOfTester: storeRequest: missing arrival_date");
				Util.printCallPath("");
			}
						
		}
		
		String save_key = this.getGID();
		
		if (save_key == null) {
			//save_key = ((Object)this).hashCode() + "";
			//Util.printCallPath("Cannot store null:\n"+this);
			//return;
			D_RecommendationOfTester._need_saving_obj.add(this);
			/*D_RecommendationOfTester renewCrt=null;
			if((renewCrt = D_RecommendationOfTester_Node.loaded_By_LocalID.get(this.getLID()))!=null){
				if(!DEBUG)System.out.println(">>>>>>>renew cache msg=" + this.getLID());
				renewCrt._weight = this._weight;
				renewCrt.weight = this.weight;
				renewCrt.signature = this.signature;
				renewCrt.address = this.address;
				renewCrt.arrival_date = this.arrival_date;
				renewCrt.creation_date = this.creation_date;
			}*/
			if (DEBUG) System.out.println("D_RecommentationOfTester: storeRequest: added to _need_saving_obj");
		} else {		
			if (DEBUG) System.out.println("D_RecommentationOfTester: storeRequest: GIDH="+save_key);
			D_RecommendationOfTester._need_saving.add(this);
		}
		try {
			if (DEBUG) System.out.println("save_key:="+save_key);
			if (DEBUG) System.out.println("saverThread.isAlive():="+saverThread.isAlive());
			if (! saverThread.isAlive()) { 
				if (DEBUG) System.out.println("D_RecommentationOfTester: storeRequest:startThread");
				saverThread.start();
			}
			synchronized(saverThread) {saverThread.notify();}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	public void releaseReferences() {
		if (status_references <= 0) Util.printCallPath("Null reference already!");
		else status_references --;
	}
	/**
	 * Could once get the tester name from the corresponding D_Tester.
	 * Now just returns the GID since D_Tester is not cached
	 * @return
	 */
	public String getTesterName() {
		return this.getGID();
	}
	
	/**
	 * Storage segment
	 */
	public static D_RecommendationOfTester_SaverThread saverThread = new D_RecommendationOfTester_SaverThread();
	public boolean dirty_any() {
		return dirty_main;
	}
	private int status_references = 0;
	boolean loaded_globals = true;
	boolean dirty_main = false;
	private static Object monitor_object_factory = new Object();
	private static final boolean _DEBUG = true;
	D_RecommendationOfTester_Node component_node = new D_RecommendationOfTester_Node(null, null);

	private boolean deleted;

	private boolean deleteTest;

	
	public static class D_RecommendationOfTester_Node {
		private static final int MAX_TRIES = 30;
		private static final boolean _DEBUG = false;
		/** Currently loaded peers, ordered by the access time*/
		private static DDP2P_DoubleLinkedList<D_RecommendationOfTester> loaded_objects = new DDP2P_DoubleLinkedList<D_RecommendationOfTester>();
		private static Hashtable<Long, D_RecommendationOfTester> loaded_By_LocalID = new Hashtable<Long, D_RecommendationOfTester>();
		private static Hashtable<String, D_RecommendationOfTester> loaded_By_GID = new Hashtable<String, D_RecommendationOfTester>();
		private static long current_space = 0;
		
		//return loaded_const_By_GID.get(GID);
		private static D_RecommendationOfTester getByGID(String GID) {
			return loaded_By_GID.get(GID);
		}
		//return loaded_const_By_GID.put(GID, c);
		private static D_RecommendationOfTester putByGID(String GID, D_RecommendationOfTester c) {
			loaded_By_GID.put(GID, c);
			return c; 
		}
		//return loaded_const_By_GID.remove(GID);
		private static D_RecommendationOfTester remByGID(String GID) {
			D_RecommendationOfTester result = null;
			D_RecommendationOfTester result2 = null;
			D_RecommendationOfTester v1 = loaded_By_GID.get(GID);
			if (v1 != null) {
				result = loaded_By_GID.remove(GID);
			}			
			return result; 
		}
		/** message is enough (no need to store the Encoder itself) */
		public byte[] message;
		public DDP2P_DoubleLinkedList_Node<D_RecommendationOfTester> my_node_in_loaded;
	
		public D_RecommendationOfTester_Node(byte[] message,
				DDP2P_DoubleLinkedList_Node<D_RecommendationOfTester> my_node_in_loaded) {
			this.message = message;
			this.my_node_in_loaded = my_node_in_loaded;
		}
		private static void register_fully_loaded(D_RecommendationOfTester crt) {
			assert((crt.component_node.message==null) && (crt.loaded_globals));
			if(crt.component_node.message != null) return;
			if(!crt.loaded_globals) return;
			//if ((crt.getGID() != null) && (! crt.isTemporary())) 
			{
				//byte[] message = crt.encode();
				synchronized(loaded_objects) {
					//crt.component_node.message = message; // crt.encoder.getBytes();
					if(crt.component_node.message != null) current_space += crt.component_node.message.length;
				}
			}
		}
		/**
		 * This function is used to link an object by its LID when this is obtained
		 * by storing an object already linked by its GIDH (if it was linked)
		 * @param crt
		 * @return
		 * true if i is linked and false if it is not
		 */
		public static boolean register_newLID_ifLoaded(D_RecommendationOfTester crt) {
			if (DEBUG) System.out.println("D_RecommendationOfTester: register_newLID_ifLoaded: start crt = "+crt);
			synchronized (loaded_objects) {
				//String gid = crt.getGID();
				String gid = crt.getGID();
				long lid = crt.getLID();
				if (gid == null) {
					if (_DEBUG) { System.out.println("D_RecommendationOfTester: register_newLID_ifLoaded: had no gid! no need of this call.");
					Util.printCallPath("Path");}
					return false;
				}
				if (lid <= 0) {
					Util.printCallPath("Why call without LID="+crt);
					return false;
				}
				
				D_RecommendationOfTester old = loaded_By_GID.get(gid); //getByGIDH(gidh, );
				if (old == null) {
					if (DEBUG) System.out.println("D_RecommendationOfTester: register_newLID_ifLoaded: was not registered.");
					return false;
				}
				
				if (old != crt)	{
					Util.printCallPath("Different linking of: old="+old+" vs crt="+crt);
					return false;
				}
				
				Long pLID = new Long(lid);
				D_RecommendationOfTester _old = loaded_By_LocalID.get(pLID);
				if (_old != null && _old != crt) {
					Util.printCallPath("Double linking of: old="+_old+" vs crt="+crt);
					return false;
				}
				loaded_By_LocalID.put(pLID, crt);
				if (DEBUG) System.out.println("D_RecommendationOfTester: register_newLID_ifLoaded: store lid="+lid+" crt="+crt.getGID());

				return true;
			}
		}
		/*
		private static void unregister_loaded(D_RecommentationOfTester crt) {
			synchronized(loaded_orgs) {
				loaded_orgs.remove(crt);
				loaded_org_By_LocalID.remove(new Long(crt.getLID()));
				loaded_org_By_GID.remove(crt.getGID());
				loaded_org_By_GIDhash.remove(crt.getGIDH());
			}
		}
		*/
		private static void register_loaded(D_RecommendationOfTester crt) {
			if (DEBUG) System.out.println("D_RecommentationOfTester: register_loaded: start crt = "+crt);
			//crt.reloadMessage(); 
			synchronized (loaded_objects) {
				String gid = crt.getGID();
//				String gidh = crt.getGIDH();
				long lid = crt.getLID();
//				Long organizationLID = crt.getOrganizationLID();
				
				loaded_objects.offerFirst(crt);
				if (lid > 0) {
					// section added for duplication control
					Long oLID = new Long(lid);
					D_RecommendationOfTester old = loaded_By_LocalID.get(oLID);
					if (old != null && old != crt) {
						Util.printCallPath("Double linking of: old="+old+" vs crt="+crt);
						//return false;
					}
					
					loaded_By_LocalID.put(oLID, crt);
					if (DEBUG) System.out.println("D_RecommentationOfTester: register_loaded: store lid="+lid+" crt="+crt.getGID());
				}else{
					if (DEBUG) System.out.println("D_RecommentationOfTester: register_loaded: no store lid="+lid+" crt="+crt.getGID());
				}
				if (gid != null) {
					D_RecommendationOfTester old = loaded_By_GID.get(gid);
					if (old != null && old != crt) {
						Util.printCallPath("D_RecommendationOfTester conflict: gidh old="+old+" crt="+crt);
					}
					D_RecommendationOfTester_Node.putByGID(gid, crt); //loaded_const_By_GID.put(gid, crt);
					if (DEBUG) System.out.println("D_RecommentationOfTester: register_loaded: store gid="+gid);
				} else {
					if (DEBUG) System.out.println("D_RecommentationOfTester: register_loaded: no store gid="+gid);
				}
				
				int tries = 0;
				while ((loaded_objects.size() > SaverThreadsConstants.MAX_LOADED_RECOMMENDATIONS_OT)
						|| (current_space > SaverThreadsConstants.MAX_RECOMMENDATIONS_OT_RAM)) {
					if (loaded_objects.size() <= SaverThreadsConstants.MIN_LOADED_RECOMMENDATIONS_OT) break; // at least _crt_peer and _myself
	
					if (tries > MAX_TRIES) break;
					tries ++;
					D_RecommendationOfTester candidate = loaded_objects.getTail();
					if ((candidate.status_references > 0)
							//||
							//D_RecommentationOfTester.is_crt_const(candidate)
							//||
							//(candidate == HandlingMyself_Peer.get_myself())
							) 
					{
						setRecent(candidate);
						continue;
					}
					
					D_RecommendationOfTester removed = loaded_objects.removeTail();//remove(loaded_peers.size()-1);
					loaded_By_LocalID.remove(new Long(removed.getLID())); 
					D_RecommendationOfTester_Node.remByGID(removed.getGID());//loaded_const_By_GID.remove(removed.getGID());
					if (DEBUG) System.out.println("D_RecommentationOfTester: register_loaded: remove GIDH="+removed.getGID());
					if (removed.component_node.message != null) current_space -= removed.component_node.message.length;				
				}
			}
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_RecommendationOfTester crt) {
			loaded_objects.moveToFront(crt);
		}
		public static boolean dropLoaded(D_RecommendationOfTester removed, boolean force) {
			boolean result = true;
			synchronized(loaded_objects) {
				if (removed.status_references > 0 || removed.get_DDP2P_DoubleLinkedList_Node() == null)
					result = false;
				if (! force && ! result) {
					System.out.println("D_RecommentationOfTester: dropLoaded: abandon: force="+force+" rem="+removed);
					return result;
				}
				
				if (loaded_objects.inListProbably(removed)) {
					try {
						loaded_objects.remove(removed); // Exception if not loaded
						if (removed.component_node.message != null) current_space -= removed.component_node.message.length;	
						if (DEBUG) System.out.println("D_RecommentationOfTester: dropLoaded: exit with force="+force+" result="+result);
					} catch (Exception e) {
						if (_DEBUG) e.printStackTrace();
					}
				}
				if (removed.getLID() > 0) loaded_By_LocalID.remove(new Long(removed.getLID())); 
				if (removed.getGID() != null) D_RecommendationOfTester_Node.remByGID(removed.getGID()); //loaded_const_By_GID.remove(removed.getGID());
				if (DEBUG) System.out.println("D_RecommentationOfTester: drop_loaded: remove GIDH="+removed.getGID());
				return result;
			}
		}
	}
	
	/**
	 * Storage Methods
	 */
	
	public void releaseReference() {
		if (status_references <= 0) Util.printCallPath("Null reference already!");
		else status_references --;
		//System.out.println("D_RecommentationOfTester: releaseReference: "+status_references);
		//Util.printCallPath("");
	}
	
	private static String getGID(long peer_LID, long tester_LID) {
		return peer_LID+":"+tester_LID;
	}
	public String getGID() {
		return getGID(senderPeerLID, this.testerLID);
	}
	public long getLID() {
		return this.recommendationID;
	}
	public void assertReferenced() {
		assert (status_references > 0);
	}
	public int getReferenced() {
		return status_references ;
	}
	/**
	 * The entries that need saving
	 */
	private static HashSet<D_RecommendationOfTester> _need_saving = new HashSet<D_RecommendationOfTester>();
	private static HashSet<D_RecommendationOfTester> _need_saving_obj = new HashSet<D_RecommendationOfTester>();
	
	@Override
	public DDP2P_DoubleLinkedList_Node<D_RecommendationOfTester> 
	set_DDP2P_DoubleLinkedList_Node(DDP2P_DoubleLinkedList_Node<D_RecommendationOfTester> node) {
		DDP2P_DoubleLinkedList_Node<D_RecommendationOfTester> old = this.component_node.my_node_in_loaded;
		if (DEBUG) System.out.println("D_RecommentationOfTester: set_DDP2P_DoubleLinkedList_Node: set = "+ node);
		this.component_node.my_node_in_loaded = node;
		return old;
	}
	@Override
	public DDP2P_DoubleLinkedList_Node<D_RecommendationOfTester> 
	get_DDP2P_DoubleLinkedList_Node() {
		if (DEBUG) System.out.println("D_RecommentationOfTester: get_DDP2P_DoubleLinkedList_Node: get");
		return component_node.my_node_in_loaded;
	}
	static void need_saving_remove(D_RecommendationOfTester c) {
		if (DEBUG) System.out.println("D_RecommentationOfTester: need_saving_remove: remove "+c);
		_need_saving.remove(c);
		if (DEBUG) dumpNeedsObj(_need_saving);
	}
	static void need_saving_obj_remove(D_RecommendationOfTester org) {
		if (DEBUG) System.out.println("D_RecommentationOfTester: need_saving_obj_remove: remove "+org);
		_need_saving_obj.remove(org);
		if (DEBUG) dumpNeedsObj(_need_saving_obj);
	}
	static D_RecommendationOfTester need_saving_next() {
		Iterator<D_RecommendationOfTester> i = _need_saving.iterator();
		if (!i.hasNext()) return null;
		D_RecommendationOfTester c = i.next();
		if (DEBUG) System.out.println("D_RecommentationOfTester: need_saving_next: next: "+c);
		//D_RecommentationOfTester r = D_RecommentationOfTester_Node.getConstByGIDH(c, null);//.loaded_const_By_GIDhash.get(c);
		if (c == null) {
			if (_DEBUG) {
				System.out.println("D_RecommentationOfTester Cache: need_saving_next null entry "
						+ "needs saving next: "+c);
				System.out.println("D_RecommentationOfTester Cache: "+dumpDirCache());
			}
			return null;
		}
		return c;
	}
	static D_RecommendationOfTester need_saving_obj_next() {
		Iterator<D_RecommendationOfTester> i = _need_saving_obj.iterator();
		if (!i.hasNext()) return null;
		D_RecommendationOfTester r = i.next();
		if (DEBUG) System.out.println("D_RecommentationOfTester: need_saving_obj_next: next: "+r);
		//D_RecommentationOfTester r = D_RecommentationOfTester_Node.loaded_org_By_GIDhash.get(c);
		if (r == null) {
			if (_DEBUG) {
				System.out.println("D_RecommentationOfTester Cache: need_saving_obj_next null entry "
						+ "needs saving obj next: "+r);
				System.out.println("D_RecommentationOfTester Cache: "+dumpDirCache());
			}
			return null;
		}
		return r;
	}
	private static void dumpNeeds(HashSet<String> _need_saving2) {
		System.out.println("D_RecommentationOfTester: Needs:");
		for ( String i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	private static void dumpNeedsObj(HashSet<D_RecommendationOfTester> _need_saving2) {
		System.out.println("Needs:");
		for ( D_RecommendationOfTester i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	public static String dumpDirCache() {
		String s = "[";
		s += D_RecommendationOfTester_Node.loaded_objects.toString();
		s += "]";
		return s;
	}
	public void setDeleteTestFlag(boolean val) {
		deleteTest = val;
	}
	public boolean isDeleteTestFlag() {
		return deleteTest;
	}
	public boolean isDeleteFlag() {
		return deleted;
	}
	public void delete() {
		this.deleted = true;
		this.dirty_main = true;
		this.storeRequest();
	}
	public void setArrivalDate() {
		this.arrival_date = Util.CalendargetInstance();
		this.dirty_main = true;
	}

	public static int getNumberItemsNeedSaving() {
		return _need_saving.size() + _need_saving_obj.size();
	}
	
}
class D_RecommendationOfTester_SaverThread extends net.ddp2p.common.util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000; // ms to sleep
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	/**
	 * The next monitor is needed to ensure that two D_RecommentationOfTester_SaverThreadWorker are not concurrently modifying the database,
	 * and no thread is started when it is not needed (since one is already running).
	 */
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_RecommendationOfTester_SaverThread() {
		super("D_RecommendationOftester Saver", true);
		//start ();
	}
	public void _run() {
		for (;;) {
			if (stop) return;
			if (net.ddp2p.common.data.SaverThreadsConstants.getNumberRunningSaverThreads() < SaverThreadsConstants.MAX_NUMBER_CONCURRENT_SAVING_THREADS && D_RecommendationOfTester.getNumberItemsNeedSaving() > 0)
			synchronized(saver_thread_monitor) {
				new D_RecommendationOfTester_SaverThreadWorker().start();
			}
			/*
			synchronized(saver_thread_monitor) {
				D_RecommentationOfTester de = D_RecommentationOfTester.need_saving_next();
				if (de != null) {
					if (DEBUG) System.out.println("D_RecommentationOfTester_Saver: loop saving "+de);
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
					//System.out.println("D_RecommentationOfTester_Saver: idle ...");
				}
			}
			*/
			synchronized(this) {
				try {
					long timeout = (D_RecommendationOfTester.getNumberItemsNeedSaving() > 0)?
							SaverThreadsConstants.SAVER_SLEEP_BETWEEN_RECOMMENDATION_TESTER_MSEC:
								SaverThreadsConstants.SAVER_SLEEP_WAITING_RECOMMENDATION_TESTER_MSEC;
					wait(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

class D_RecommendationOfTester_SaverThreadWorker extends net.ddp2p.common.util.DDP2P_ServiceThread {
	private static final long SAVER_SLEEP = 5000;
	private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	//public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_RecommendationOfTester_SaverThreadWorker() {
		super("D_RecommendationOfTester Saver Worker", false);
		//start ();
	}
	public void _run() {
		synchronized (SaverThreadsConstants.monitor_threads_counts) {SaverThreadsConstants.threads_test ++;}
		try{__run();} catch (Exception e){}
		synchronized (SaverThreadsConstants.monitor_threads_counts) {SaverThreadsConstants.threads_test --;}
	}
	@SuppressWarnings("unused")
	public void __run() {
		synchronized(D_RecommendationOfTester_SaverThread.saver_thread_monitor) {
			D_RecommendationOfTester de;
			boolean edited = true;
			if (DEBUG) System.out.println("D_RecommentationOfTester_Saver: start");
			
			 // first try objects being edited
			de = D_RecommendationOfTester.need_saving_obj_next();
			
			// then try remaining objects 
			if (de == null) { de = D_RecommendationOfTester.need_saving_next(); edited = false; }
			
			if (de != null) {
				if (DEBUG) System.out.println("D_RecommendationOfTester_SaverThread: loop saving "+de.getTesterName());
				Application_GUI.ThreadsAccounting_ping("Saving");
				if (edited) D_RecommendationOfTester.need_saving_obj_remove(de);
				else D_RecommendationOfTester.need_saving_remove(de);//, de.instance);
				if (DEBUG) System.out.println("D_RecommendationOfTester_SaverThread: loop removed need_saving flag");
				// try 3 times to save
				for (int k = 0; k < net.ddp2p.common.data.SaverThreadsConstants.ATTEMPTS_ON_ERROR; k++) {
					try {
						if (DEBUG) System.out.println("D_RecommendationOfTester_SaverThread: loop will try saving k="+k);
						de.storeAct();
						if (DEBUG) System.out.println("D_RecommendationOfTester_SaverThread: stored recom:"+de.getTesterName());
						break;
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
						synchronized(this) {
							try {
								if (DEBUG) System.out.println("D_RecommendationOfTester_SaverThread: sleep");
								wait(SaverThreadsConstants.SAVER_SLEEP_WORKER_RECOMMENDATION_TESTER_ON_ERROR);
								if (DEBUG) System.out.println("D_RecommendationOfTester_SaverThread: waked error");
							} catch (InterruptedException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
			} else {
				Application_GUI.ThreadsAccounting_ping("Nothing to do!");
				if (DEBUG) System.out.println("D_RecommentationOfTester_Saver: idle ...");
			}
		}
		if (SaverThreadsConstants.SAVER_SLEEP_WORKER_RECOMMENDATION_TESTER_MSEC >= 0) {
			synchronized(this) {
				try {
					if (DEBUG) System.out.println("D_RecommentationOfTester_Saver: sleep");
					wait(SaverThreadsConstants.SAVER_SLEEP_WORKER_RECOMMENDATION_TESTER_MSEC);
					if (DEBUG) System.out.println("D_RecommentationOfTester_Saver: waked");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}