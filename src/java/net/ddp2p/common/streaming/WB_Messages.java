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

package net.ddp2p.common.streaming;

import static java.lang.System.err;
import static net.ddp2p.common.util.Util.__;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_GIDH;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_News;
import net.ddp2p.common.data.D_OrgDistribution;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Translations;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.hds.ASNSyncRequest;
import net.ddp2p.common.hds.SyncAnswer;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

/**
 * The structure containing ArrayLists with various types of data
 * @author msilaghi
 *
 */
public class WB_Messages extends ASNObj{

	static final boolean _DEBUG = true;
	public static final String NEWS_POOL = "NEWS_POOL";
	public static final String TRAN_POOL = "TRAN_POOL";
	public static final String PEER_POOL = "PEER_POOL";
	public static boolean DEBUG = false;
	public ArrayList<D_Peer> peers = new ArrayList<D_Peer>();
	public ArrayList<D_Constituent> cons = new ArrayList<D_Constituent>();
	public ArrayList<D_Neighborhood> neig = new ArrayList<D_Neighborhood>();
	public ArrayList<D_Witness> witn = new ArrayList<D_Witness>();
	public ArrayList<D_Motion> moti = new ArrayList<D_Motion>();
	public ArrayList<D_Justification> just = new ArrayList<D_Justification>();
	public ArrayList<D_Organization> orgs = new ArrayList<D_Organization>();
	public ArrayList<D_Vote> sign = new ArrayList<D_Vote>();
	public ArrayList<D_News> news = new ArrayList<D_News>();
	public ArrayList<D_Translations> tran = new ArrayList<D_Translations>();
	@Override
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) {
		Encoder enc = new Encoder();
		enc.addToSequence(Encoder.getEncoder(orgs.toArray(new D_Organization[0]), dictionary_GIDs));
		enc.addToSequence(Encoder.getEncoder(cons.toArray(new D_Constituent[0]), dictionary_GIDs));
		enc.addToSequence(Encoder.getEncoder(neig.toArray(new D_Neighborhood[0]), dictionary_GIDs));
		enc.addToSequence(Encoder.getEncoder(moti.toArray(new D_Motion[0]), dictionary_GIDs));
		enc.addToSequence(Encoder.getEncoder(witn.toArray(new D_Witness[0]), dictionary_GIDs));
		enc.addToSequence(Encoder.getEncoder(just.toArray(new D_Justification[0]), dictionary_GIDs));
		enc.addToSequence(Encoder.getEncoder(sign.toArray(new D_Vote[0]), dictionary_GIDs));
		enc.addToSequence(Encoder.getEncoder(news.toArray(new D_News[0]), dictionary_GIDs));
		enc.addToSequence(Encoder.getEncoder(tran.toArray(new D_Translations[0]), dictionary_GIDs));
		if (peers.size() > 0) enc.addToSequence(Encoder.getEncoder(peers.toArray(new D_Peer[0]), dictionary_GIDs).setASN1Type(DD.TAG_AC1));
		return enc;
	}

	@Override
	public WB_Messages decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		orgs = d.getFirstObject(true).getSequenceOfAL(D_Organization.getASN1Type(), D_Organization.getEmpty());
		cons = d.getFirstObject(true).getSequenceOfAL(D_Constituent.getASN1Type(), D_Constituent.getEmpty());
		neig = d.getFirstObject(true).getSequenceOfAL(D_Neighborhood.getASN1Type(), D_Neighborhood.getEmpty());
		moti = d.getFirstObject(true).getSequenceOfAL(D_Motion.getASN1Type(), D_Motion.getEmpty());
		witn = d.getFirstObject(true).getSequenceOfAL(D_Witness.getASN1Type(), new D_Witness());
		just = d.getFirstObject(true).getSequenceOfAL(D_Justification.getASN1Type(), D_Justification.getEmpty());
		sign = d.getFirstObject(true).getSequenceOfAL(D_Vote.getASN1Type(), new D_Vote());
		news = d.getFirstObject(true).getSequenceOfAL(D_News.getASN1Type(), D_News.getEmpty());
		tran = d.getFirstObject(true).getSequenceOfAL(D_Translations.getASN1Type(), new D_Translations());
		if (d.getTypeByte() == DD.TAG_AC1) peers = d.getFirstObject(true).getSequenceOfAL(D_Peer.getASN1Type(), D_Peer.getEmpty());
		return this;
	}
	public String toString() {
		String result = "\n WB_Messages[";
		if(peers != null) result += "\n  WM peers = ["+ Util.concat(peers, "\n", "null")+" WM]";
		if(orgs != null) result += "\n  WM orgs = ["+ Util.concat(orgs, "\n", "null"+" WM]");
		if(cons != null) result += "\n  WM cons = ["+ Util.concat(cons, "\n", "null")+" WM]";
		if(neig != null) result += "\n  WM neig = ["+ Util.concat(neig, "\n", "null")+" WM]";
		if(moti != null) result += "\n  WM moti = ["+ Util.concat(moti, "\n", "null")+" WM]";
		if(witn != null) result += "\n  WM witn = ["+ Util.concat(witn, "\n", "null")+" WM]";
		if(just != null) result += "\n  WM just = ["+ Util.concat(just, "\n", "null")+" WM]";
		if(sign != null) result += "\n  WM sign = ["+ Util.concat(sign, "\n", "null")+" WM]";
		if(news != null) result += "\n  WM news = ["+ Util.concat(news, "\n", "null")+" WM]";
		if(tran != null) result += "\n  WM tran = ["+ Util.concat(tran, "\n", "null")+" WM]";
		result += "\n]";
		return result;
	}

	public String toSummaryString() {
		String result = "\n WB_Messages[";
		if((peers != null) && (peers.size()>0)) result += "\n  peer = "+ Util.concatSummary(peers, "\n", "null");
		if((orgs != null) && (orgs.size()>0)) result += "\n  orgs = "+ Util.concatSummary(orgs, "\n", "null");
		if((cons != null) && (cons.size()>0)) result += "\n  cons = "+ Util.concatSummary(cons, "\n", "null");
		if((neig != null) && (neig.size()>0)) result += "\n  neig = "+ Util.concatSummary(neig, "\n", "null");
		if((moti != null) && (moti.size()>0)) result += "\n  moti = "+ Util.concatSummary(moti, "\n", "null");
		if((witn != null) && (witn.size()>0)) result += "\n  witn = "+ Util.concatSummary(witn, "\n", "null");
		if((just != null) && (just.size()>0)) result += "\n  just = "+ Util.concat(just, "\n", "null");
		if((sign != null) && (sign.size()>0)) result += "\n  sign = "+ Util.concat(sign, "\n", "null");
		if((news != null) && (news.size()>0)) result += "\n  news = "+ Util.concat(news, "\n", "null");
		if((tran != null) && (tran.size()>0)) result += "\n  tran = "+ Util.concat(tran, "\n", "null");
		result += "\n]";
		return result;
	}
	/**
	 * check of hasConstituent not yet fully implemented
	 * @param asr
	 * @param sa
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static WB_Messages getRequestedData(ASNSyncRequest asr,
			SyncAnswer sa) throws P2PDDSQLException {
		if(DEBUG) System.out.println("WB_Messages: getRequestedData: start");
		if(asr==null){
			if(DEBUG) System.out.println("WB_Messages: getRequestedData: exit no sync request");
			return null;
		}
		if(asr.request==null){
			if(DEBUG) System.out.println("WB_Messages: getRequestedData: exit no data request");
			return null;
		}
		WB_Messages result = new WB_Messages();
		
		fillPeers(asr.request.peers, result.peers, sa);
		fillNews(asr.request.news, result.news, sa);
		fillTran(asr.request.tran, result.tran, sa);
		
		if(asr.request.rd==null){
			if(DEBUG) System.out.println("WB_Messages: getRequestedData: exit no data request content");
			return null;
		}
		ArrayList<RequestData> rd = asr.request.rd;
		if(DEBUG) System.out.println("WB_Messages: getRequestedData: data request content #"+rd.size());
		
		for(RequestData r: rd) {
			long p_oLID = -1;
			if (r.global_organization_ID_hash != null) 
				p_oLID = D_Organization.getLIDbyGIDH(r.global_organization_ID_hash);
			for(String gid_or_hash: r.orgs) {
				if(DEBUG) System.out.println("WB_Messages: getRequestedData: org gid="+gid_or_hash);
				String gid = D_GIDH.getGID(gid_or_hash);
				String gidh = D_GIDH.getGIDH(gid_or_hash);
				String org_id = Util.getStringID(D_Organization.getLocalOrgID(gid, gidh));

				if(!OrgHandling.serving(asr, org_id)) continue;
				
				if((sa!=null)&&sa.hasOrg(gid)) continue;
				try {
					D_Organization o = D_Organization.getOrgByGID_or_GIDhash_NoCreate(gid, gidh, true, false);
					if ( (o == null)  || ! o.readyToSend()) continue;
					result.orgs.add(o);
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: got org");
				} catch (Exception e) {
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: I don't have requested org: "+gid_or_hash);
					//e.printStackTrace();
				}
			}
			for (String gid_or_hash: r.cons.keySet()) {
				if (DEBUG) System.out.println("WB_Messages: getRequestedData: cons gid="+gid_or_hash);
				// TODO if gid is sa, then skip to avoid duplicating work 
				String gid = D_GIDH.getGID(gid_or_hash);
				String gidh = D_GIDH.getGIDH(gid_or_hash);
				if ((sa != null) && (gid != null) && sa.hasConstituent(gid, r.cons.get(gid))) continue;
				if ((sa != null) && (gidh != null) && sa.hasConstituent(gidh, r.cons.get(gidh))) continue;
				try {
					D_Constituent c;
					if (p_oLID <= 0) {
						System.out.println("WC_Messages: getRequestedData: rd="+rd);
						c = D_Constituent.getConstByGID_or_GIDH(gid, gidh, true, false);
					}else
						c = D_Constituent.getConstByGID_or_GIDH(gid, gidh, true, false, false, null, p_oLID);
					if (c == null || ! c.readyToSend()) continue;
					if (!OrgHandling.serving(asr, c.getOrganizationLIDstr())) continue;
					result.cons.add(c);
				} catch(Exception e) {
					if (DEBUG) System.out.println("WB_Messages: getRequestedData: I don't have requested const: "+gid_or_hash);
					//e.printStackTrace();
				}
				if(DEBUG) System.out.println("WB_Messages: getRequestedData: got cons");
			}
			for (String gid: r.neig) {
				if(DEBUG) System.out.println("WB_Messages: getRequestedData: neigh gid="+gid);
				if((sa!=null)&&sa.hasNeighborhod(gid)) continue;
				try {
					D_Neighborhood n = D_Neighborhood.getNeighByGID(gid, true, false);
					if (!OrgHandling.serving(asr, n.getOrgLIDstr())) continue;
					result.neig.add(n);
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: got neig");
				} catch (Exception e) {
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: I don't have requested neig: "+gid);
					//e.printStackTrace();
				}
			}
			for (String gid: r.witn) {
				//boolean DEBUG = true;
				if (DEBUG) System.out.println("WB_Messages: getRequestedData: witn gid="+gid);
				if ((sa != null) && sa.hasWitness(gid)) continue;
				D_Witness w = null;
				try {
					w = new D_Witness(gid);
					if(!OrgHandling.serving(asr, w.organization_ID)) continue;
					result.witn.add(w);
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: got witn: "+w);
				}
				catch (net.ddp2p.common.data.D_NoDataException e) {
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: I don't have requested witn: "+gid);
				}
				catch (Exception e) {
					if(_DEBUG) System.out.println("WB_Messages: getRequestedData: Failure requesting witn: "+gid);
					if(w!=null){
						if(_DEBUG) System.out.println("WB_Messages: getRequestedData: git witn="+w.organization_ID);
					}
					if(_DEBUG) System.out.println("WB_Messages: getRequestedData: git witn="+w);
					e.printStackTrace();
				}
			}
			for (String gid: r.moti) {
				if (DEBUG) System.out.println("WB_Messages: getRequestedData: moti gid="+gid);
				if ((sa != null) && sa.hasMotion(gid)) continue;
				try {
					D_Motion m = D_Motion.getMotiByGID(gid, true, false);
					if (! m.readyToSend()) {
						if (_DEBUG) System.out.println("WB_Messages: getRequestedData: not ready moti gid="+m);
						continue;
					}
					if (! OrgHandling.serving(asr, m.getOrganizationLIDstr())) {
						if (_DEBUG) System.out.println("WB_Messages: getRequestedData: not serving moti gid="+m);
						continue;
					}
					result.moti.add(m);
					if (_DEBUG) System.out.println("WB_Messages: getRequestedData: shipping moti:"+m.getGIDH()+" "+m.getTitleStrOrMy());
					//if (DEBUG) System.out.println("WB_Messages: getRequestedData: got moti");
				}
				catch (net.ddp2p.common.data.D_NoDataException e) {
					if (_DEBUG) System.out.println("WB_Messages: getRequestedData: I don't have requested motion: "+gid);
				}
				catch (Exception e) {
					if (DEBUG) System.out.println("WB_Messages: getRequestedData: Error for moti: "+gid);
					e.printStackTrace();
				}
			}
			for (String gid: r.just) {
				if (DEBUG) System.out.println("WB_Messages: getRequestedData: cons just="+gid);
				if ((sa != null) && sa.hasJustification(gid)) continue;
				try {
					D_Justification j = D_Justification.getJustByGID(gid, true, false);
					if (!j.readyToSend()) continue;
					if (!OrgHandling.serving(asr, j.getOrganizationLIDstr())) continue;
					result.just.add(j);
					//if (DEBUG) System.out.println("WB_Messages: getRequestedData: got just");
					if (DEBUG) System.out.println("WB_Messages: getRequestedData: shipping just:"+j.getGIDH()+" "+j.getTitleStrOrMy());
				}
				catch (net.ddp2p.common.data.D_NoDataException e) {
					if(_DEBUG) System.out.println("WB_Messages: getRequestedData: I don't have requested just: "+gid);
				}
				catch (Exception e) {
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: Error for justification: "+gid);
					e.printStackTrace();
				}
			}			
			for (String gid: r.sign.keySet()) {
				if (DEBUG) System.out.println("WB_Messages: getRequestedData: vote just="+gid);
				if ((sa != null) && sa.hasSignature(gid, r.sign.get(gid))) continue;
				try{
					D_Vote v = new D_Vote(gid);
					if (! v.readyToSend()) continue;
					if (! OrgHandling.serving(asr, v.getOrganizationLIDstr())) continue;
					result.sign.add(v);
					if (DEBUG) System.out.println("WB_Messages: getRequestedData: got vote o:"+v.getOrganizationLID()+" m:"+v.getMotionLID()+" c:"+v.getConstituentLIDstr());
				}
				catch (net.ddp2p.common.data.D_NoDataException e) {
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: I don't have requested vote: "+gid);
				}
				catch (Exception e) {
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: Error for vote: "+gid);
					e.printStackTrace();
				}
			}			
		}
		return result;
	}

	private static void fillTran(ArrayList<String> request,
			ArrayList<D_Translations> result, SyncAnswer sa) {
		for (String gid: request) {
			if (_DEBUG) System.out.println("WB_Messages: fillPeers: peer gid="+gid);
			Util.printCallPath("");
		}
	}
	private static void fillNews(ArrayList<String> request,
			ArrayList<D_News> result, SyncAnswer sa) {
		for(String gid: request) {
			if(_DEBUG) System.out.println("WB_Messages: fillPeers: peer gid="+gid);
			Util.printCallPath("");
		}		
	}
	private static void fillPeers(Hashtable<String, String> request,
			ArrayList<D_Peer> result, SyncAnswer sa) {
		for (String gid_or_hash: request.keySet()) {
			if(DEBUG) System.out.println("WB_Messages: fillPeers: peer gid="+gid_or_hash);
			String gid = D_GIDH.getGID(gid_or_hash);
			String gidh = D_GIDH.getGIDH(gid_or_hash);
			// TODO if gid is sa, then skip to avoid duplicating work 
			if ((sa != null) && (gid != null) && sa.hasPeer(gid, request.get(gid))) continue;
			if ((sa != null) && (gidh != null) && sa.hasPeer(gidh, request.get(gidh))) continue;
			try{ 
				D_Peer p = D_Peer.getPeerByGID_or_GIDhash(gid, gidh, true, false, false, null);
				if ((p == null) || !p.readyToSend()) continue;
				String localDate = p.getCreationDate();
				if (localDate == null) 
						localDate = DD.EMPTYDATE;
				if (request.get(gid_or_hash).compareTo(localDate) > 0) {
					if(_DEBUG) System.out.println("WB_Messages: fillPeers: I don't have requested peer date: "+gid_or_hash+", "+request.get(gid_or_hash));
					continue;
				}
				result.add(p);
			} catch (Exception e) {
				if (_DEBUG) System.out.println("WB_Messages: fillPeers: I don't have requested peer: "+gid_or_hash);
				e.printStackTrace();
			}
			if (DEBUG) System.out.println("WB_Messages: fillPeers: got peer");
		}
	}
	/**
	 * 	Hashtable<String, RequestData> obtained_sr = new Hashtable<String, RequestData>();
		Hashtable<String, RequestData> sq_sr = new Hashtable<String, RequestData>();

	 eventually, we should get mostly org_hash rather than orgGID....

	 * @param _asa : the incoming message (or null if manually added)
	 * @param peer : the peer sending the message (to be used to tag the temporary items)
	 * @param r : the set of messages to store
	 * @param missing_sr {GID : RequestData}
	 * @param obtained_sr
	 * @param orgs : the list of orgs mentioned
	 * @param dbg_msg : a debugging message (like the ID of the sender, with a text)
	 * @throws P2PDDSQLException
	 */
	public static void store(ASNSyncPayload _asa, D_Peer peer, WB_Messages r,
			Hashtable<String, RequestData> missing_sr,
			Hashtable<String, RequestData> obtained_sr,
			HashSet<String> orgs, String dbg_msg) throws P2PDDSQLException {
		boolean default_const_blocked = false;
		RequestData obtained;
		RequestData rq, sol_rq, new_rq; 
		Calendar arrival_time = Util.CalendargetInstance();//.getGeneralizedTime();
		
		if (DEBUG) System.out.println("WB_Messages: store: start");
		if (r == null) {
			if(DEBUG) System.out.println("WB_Messages: store: exit null requested");
			return;
		}
		
		if (DEBUG || DD.DEBUG_CHANGED_ORGS) err.println("WB_Messages: store: srs: obtained="+obtained_sr.size()+" - missing="+missing_sr.size());
		
		try {
			for (D_Peer p: r.peers) {
				if (DEBUG) System.out.println("WB_Messages: store: handle peer: "+p);
				if (! p.verifySignature()) {
					if (_DEBUG) System.out.println("WB_Messages: store: failed signature for: "+p);
					continue;
				}
				rq = missing_sr.get(PEER_POOL);
				if (rq == null) rq = new RequestData();
				sol_rq = new RequestData();
				new_rq = new RequestData();
				p.fillLocals(sol_rq, new_rq);
				if (DEBUG) System.out.println("WB_Messages: store: nou="+p);
				D_Peer old = D_Peer.getPeerByGID_or_GIDhash(p.getGID(), null, true, true, true, null);
				if (DEBUG) System.out.println("WB_Messages: store: old="+old);
				//old = D_Peer.getPeerByPeer_Keep(p);
				if (old.loadRemote(p, sol_rq, new_rq)) {
					if (old.dirty_any()) {
						old.setArrivalDate();
						old.storeRequest();
						
						net.ddp2p.common.config.Application_GUI.inform_arrival(old, peer);
					}
					rq.update(sol_rq, new_rq);
					missing_sr.put(PEER_POOL, rq);			
					
					obtained = obtained_sr.get(PEER_POOL);
					if (obtained == null) obtained = new RequestData();
					obtained.peers.put(p.getGID(), Util.getNonNullDate(p.getCreationDate()));//DD.EMPTYDATE);
					obtained.peers.put(p.getGIDH_force(), Util.getNonNullDate(p.getCreationDate()));//DD.EMPTYDATE);
					obtained_sr.put(PEER_POOL, obtained);
				}
				old.releaseReference();
				orgs.add(PEER_POOL);
			}
		} catch(Exception e) {e.printStackTrace();}

		
		for (D_Organization org: r.orgs) { // should we store new orgs like that?
			if (DEBUG) System.out.println("WB_Messages: store: handle org: "+org);
			//org.store(rq);
			///_obtained.orgs.add(org.global_organization_ID);
			//obtained.orgs.add(org.global_organization_ID_hash);
			orgs.add(org.getGID());

			boolean _changed[] = new boolean[1];
			//RequestData _new_rq = missing_sr.get(org.getGID());
			rq = missing_sr.get(org.getGID());
			if (rq == null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			//if (_new_rq == null) { _new_rq = new RequestData(org.getGIDH()); missing_sr.put(org.getGID(), _new_rq); }
			//long id = org.store(peer, _changed, sol_rq, new_rq); // should set blocking new orgs
			long id = D_Organization.storeRemote(org, _changed, sol_rq, new_rq, peer);
			rq.update(sol_rq, new_rq);
			missing_sr.put(org.getGID(), rq);			
			if (DEBUG) System.out.println("OrgHandling:updateOrg: sharing: ch="+_changed[0]+
					" br="+org.broadcast_rule+" id="+id+" creat="+org.creator_ID);
			if (DEBUG) System.out.println("OrgHandling:updateOrg: sharing: ch="+_changed[0]+
					" br="+org.broadcast_rule+" id="+id+" creat="+org.creator_ID);
			if (
					(org.broadcast_rule == false) &&
					(id > 0) &&
					(org.creator_ID != null) &&
					(_changed[0])
					)
			{
				if(DEBUG)System.out.println("OrgHandling:updateOrg: sharing: auto="+DD.AUTOMATE_PRIVATE_ORG_SHARING);
				if(DD.AUTOMATE_PRIVATE_ORG_SHARING == 0) {
					int pos = Application_GUI.ask(__("In this session, do you want to share data of private orgs\n to all those sending it to you?"),
							__("Share private orgs with your providers"), Application_GUI.YES_NO_OPTION);
					if(pos==0) DD.AUTOMATE_PRIVATE_ORG_SHARING = 1;
					else DD.AUTOMATE_PRIVATE_ORG_SHARING = -1;
					if(DEBUG)System.out.println("OrgHandling:updateOrg: sharing: auto:="+DD.AUTOMATE_PRIVATE_ORG_SHARING);
				}
				if(DD.AUTOMATE_PRIVATE_ORG_SHARING == 1)
				{
					D_OrgDistribution.add(Util.getStringID(id), org.creator_ID);
				}
			}
		}
		for (D_Constituent c: r.cons) {
			if (DEBUG) System.out.println("WB_Messages: store: handle const: "+c);
			rq = missing_sr.get(c.getOrganizationGID());
			if (rq==null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			
			D_Constituent _c = D_Constituent.integrateRemote(c, peer, sol_rq, new_rq, default_const_blocked, arrival_time); //c.store(sol_rq, new_rq);
			if (_c == null) {
				if (_DEBUG) System.out.println("WB_Messages: store: failed to handled const: "+c+" "+dbg_msg);
				continue;
			}
			long lid = _c.getLID();
			rq.update(sol_rq, new_rq);
			missing_sr.put(c.getOrganizationGID(), rq);			
			
			obtained = obtained_sr.get(c.getOrganizationGID());
			if(obtained==null) obtained = new RequestData();
			obtained.cons.put(c.getGID(), DD.EMPTYDATE);
			obtained.cons.put(c.getGIDH(), DD.EMPTYDATE);
			obtained_sr.put(c.getOrganizationGID(), obtained);
			orgs.add(c.getOrganizationGID());
			//orgs.add(c.global_organization_ID_hash);
		}
		for (D_Neighborhood n: r.neig) {
			if(DEBUG) System.out.println("WB_Messages: store: handle neig: "+n);
			rq = missing_sr.get(n.getOrgGIDH());
			if (rq == null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			long oLID = D_Organization.getLIDbyGIDH(n.getOrgGIDH());
			if (oLID <= 0) {
				if (_DEBUG) System.out.println("WB_Messages: store: neigh: organization not found for: "+n.getOrgGIDH());
				continue;
			}
			long lid = n.storeRemoteThis(n.getOrgGID(), oLID, Util.getGeneralizedTime(), sol_rq, new_rq, peer);
			//long lid=n.store(sol_rq, new_rq);
			if (lid <= 0) {
				if (_DEBUG) System.out.println("WB_Messages: store: failed to handled neigh: "+n+" "+dbg_msg);
				continue;
			}
			rq.update(sol_rq, new_rq);
			missing_sr.put(n.getOrgGIDH(), rq);			
			
			obtained = obtained_sr.get(n.getOrgGIDH());
			if (obtained == null) obtained = obtained_sr.get(n.getOrgGID());
			if (obtained == null) obtained = new RequestData();
			obtained.neig.add(n.getGID());
			obtained_sr.put(n.getOrgGIDH(), obtained);
			orgs.add(n.getOrgGIDH());
			
			obtained_sr.put(n.getOrgGID(), obtained);
			orgs.add(n.getOrgGID());
		}
		for (D_Witness w: r.witn) {
			if(DEBUG) System.out.println("WB_Messages: store: handle witn: "+w);
			rq = missing_sr.get(w.global_organization_ID);
			if (rq == null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			long lid = w.store(sol_rq, new_rq, peer);
			if (lid <= 0) {
				if (DEBUG) System.out.println("WB_Messages: store: either old, or failed to handled witn: "+w+" "+dbg_msg);
				continue;
			}
			rq.update(sol_rq, new_rq);
			missing_sr.put(w.global_organization_ID, rq);			
			
			obtained = obtained_sr.get(w.global_organization_ID);
			if(obtained==null) obtained = new RequestData();
			obtained.witn.add(w.global_witness_ID);
			obtained_sr.put(w.global_organization_ID, obtained);
			orgs.add(w.global_organization_ID);
		}
		for (D_Motion m: r.moti) {
			if(DEBUG) System.out.println("WB_Messages: store: handle moti: "+m);
			if(DEBUG) System.out.println("WB_Messages: store: handle moti: "+m.getGID()+" "+m.getMotionTitle());

			String oGID = m.getOrganizationGID_force();
			if (oGID == null) {
				if (_DEBUG) System.out.println("WB_Messages: store: no GID handling moti: "+m);
				if (_DEBUG) System.out.println("WB_Messages: store: faulty: "+r);
				continue;
			}
			rq = null;
			if (missing_sr != null) rq = missing_sr.get(oGID);
			if (rq == null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			
			long p_oLID = m.getOrganizationLID();
			if (p_oLID <= 0) p_oLID = D_Organization.getLIDbyGIDH(m.getOrganizationGIDH());
			if (p_oLID <= 0) {
				if (_DEBUG) System.out.println("WB_Messages: store: moti: organization not found for: "+m.getOrganizationGIDH());
				continue;
			}
			
			D_Motion mot = D_Motion.getMotiByGID(m.getGID(), true, true, true, peer, p_oLID, null);
			if (mot.loadRemote(m, sol_rq, new_rq, peer)) {
				net.ddp2p.common.config.Application_GUI.inform_arrival(mot, peer);
			}
			long lid;
			if (mot.dirty_any() || (mot.getLIDstr() == null))
				lid = mot.storeRequest_getID(); //m.store(sol_rq, new_rq);
			else lid = mot.getLID();
			mot.releaseReference();
			if (lid <= 0) {
				if (_DEBUG) System.out.println("WB_Messages: store: failed to handled motion: "+m+" "+dbg_msg);
				continue;
			}
			rq.update(sol_rq, new_rq);
			missing_sr.put(m.getOrganizationGIDH(), rq);			
			
			obtained = obtained_sr.get(m.getOrganizationGIDH());
			if (obtained == null) obtained_sr.get(m.getOrganizationGID_force());
			if (obtained == null) obtained = new RequestData();
			
			obtained.moti.add(m.getGIDH());

			obtained_sr.put(m.getOrganizationGIDH(), obtained);
			orgs.add(m.getOrganizationGIDH());

			obtained_sr.put(m.getOrganizationGID_force(), obtained);
			orgs.add(m.getOrganizationGID_force());

			if(DEBUG) System.out.println("WB_Messages: store: handled moti: "+m.getGID()+" "+m.getMotionTitle());
		}
		for (D_Justification j: r.just) {
			if (DEBUG) System.out.println("WB_Messages: store: handle just: "+j);
			if (j.getOrgGIDH() == null){
				j.setOrgGID(j.guessOrganizationGID());
				if (j.getOrgGIDH() == null) {
					if(_DEBUG) System.out.println("WB_Messages: store: cannot determine org: skip "+j);
					continue;
				}
			}
			long p_oLID = D_Organization.getLIDbyGIDH(j.getOrgGIDH());
			if (p_oLID <= 0) {
				if (_DEBUG) System.out.println("WB_Messages: store: justif: organization not found for: "+j.getOrgGIDH());
				continue;
			}
			long p_mLID = D_Motion.getLIDFromGID(j.getMotionGID(), p_oLID);
			if (p_mLID <= 0) {
				if (_DEBUG) System.out.println("WB_Messages: store: justif: motion not found for: "+j.getMotionGID());
				continue;
			}
			
			if (! j.isGIDValidAndNotBlocked()) {
				if (_DEBUG) System.out.println("WB_Messages: store: justif: invalid or blocked: "+j);
				continue;
			}
			// preparing management of missing/obtained GIDH
			rq = missing_sr.get(j.getOrgGIDH());
			if (rq == null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			
			// actual integration
			D_Justification jus = D_Justification.getJustByGID(j.getGID(), true, true, true, peer, p_oLID, p_mLID, j);
			long lid = jus.getLID();
			if (jus == j) {
			    jus.fillLocals(sol_rq, new_rq, peer);
			    net.ddp2p.common.config.Application_GUI.inform_arrival(jus, peer);
			    if (j.isTemporary() || lid <= 0) lid = jus.storeRequest_getID();
			    else jus.storeRequest();
				if (DEBUG) System.out.println("WB_Messages: store: got jsame: "+j+" "+dbg_msg);
			} else {
				if (jus.loadRemote(j, sol_rq, new_rq, peer)) {
					net.ddp2p.common.config.Application_GUI.inform_arrival(jus, peer);
					jus.storeRequest();
					// the following is just for debugging but could be eventually commented out
					lid = jus.storeRequest_getID(); //j.store(sol_rq, new_rq);
					if (DEBUG) System.out.println("WB_Messages: store: go lid=: "+lid+" for "+j+" "+dbg_msg);
				} else {
					if (DEBUG) System.out.println("WB_Messages: store: skipped just: "+j+" "+dbg_msg);
				}
			}
			jus.releaseReference();
			if (lid <= 0) {
				if (_DEBUG) System.out.println("WB_Messages: store: failed to handled just: "+j+" "+dbg_msg);
				continue;
			}
			
			// management of new and missing GIDH
			rq.update(sol_rq, new_rq);
			missing_sr.put(j.getOrgGIDH(), rq);			
			
			obtained = obtained_sr.get(j.getOrgGIDH());
			if (obtained == null) obtained_sr.get(j.getOrgGID());
			if (obtained == null) obtained = new RequestData();
			
			obtained.just.add(j.getGIDH());
			
			obtained_sr.put(j.getOrgGIDH(), obtained);
			orgs.add(j.getOrgGIDH());

			obtained_sr.put(j.getOrgGID(), obtained);
			orgs.add(j.getOrgGID());
		}
		for (D_Vote v: r.sign) {
			if (DEBUG) System.out.println("WB_Messages: store: handle vote: "+v);
			try {
				//String oGID = v.getOrganizationGID();
				String oGIDH = v.getOrganizationGIDH();
				if (oGIDH == null){
					v.setOrganizationGID(oGIDH = v.guessOrganizationGIDH());
					if (oGIDH == null) {
						if(_DEBUG) System.out.println("WB_Messages: store: cannot determine org: skip "+v);
						continue;
					}
				}

				rq = missing_sr.get(oGIDH);
				if (rq == null) rq = new RequestData();
				sol_rq = new RequestData();
				new_rq = new RequestData();
				long lid = v.store(sol_rq, new_rq, peer);
				if (lid <= 0) {
					if(_DEBUG) System.out.println("WB_Messages: store: failed to handled vote: "+v+" "+dbg_msg);
					continue;
				}
				if(DEBUG) System.out.println("WB_Messages: store: handled vote: "+v);
				if(DEBUG) System.out.println("WB_Messages: store: handled vote: new="+new_rq+" sol="+sol_rq);
				rq.update(sol_rq, new_rq);
				missing_sr.put(oGIDH, rq);			
				
				obtained = obtained_sr.get(oGIDH);
				if (obtained == null) obtained = new RequestData();
				obtained.sign.put(v.getGID(), DD.EMPTYDATE);
				obtained_sr.put(oGIDH, obtained);
				orgs.add(oGIDH);
			} catch(Exception e) {
				if (_DEBUG) System.out.println("WB_Messages: store: failed vote: "+dbg_msg+" for v="+v);
				e.printStackTrace();
			}
		}
		for(D_News w: r.news) {
			if(DEBUG) System.out.println("WB_Messages: store: handle news: "+w);
			rq = missing_sr.get(newsGID(w.global_organization_ID));
			if(rq==null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			long lid = w.store(sol_rq, new_rq);
			if (lid <= 0) {
				if(_DEBUG) System.out.println("WB_Messages: store: failed to handled news: "+w+" "+dbg_msg);
				continue;
			}
			rq.update(sol_rq, new_rq);
			missing_sr.put(newsGID(w.global_organization_ID), rq);			
			
			obtained = obtained_sr.get(newsGID(w.global_organization_ID));
			if(obtained==null) obtained = new RequestData();
			obtained.news.add(w.global_news_ID);
			obtained_sr.put(newsGID(w.global_organization_ID), obtained);
			orgs.add(newsGID(w.global_organization_ID));
		}
		for(D_Translations t: r.tran) {
			if(DEBUG) System.out.println("WB_Messages: store: handle tran: "+t);
			rq = missing_sr.get(tranGID(t.global_organization_ID));
			if(rq==null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			long lid = t.store(sol_rq, new_rq, peer);
			if (lid <= 0) {
				if(_DEBUG) System.out.println("WB_Messages: store: failed to handled translation: "+t+" "+dbg_msg);
				continue;
			}
			rq.update(sol_rq, new_rq);
			missing_sr.put(tranGID(t.global_organization_ID), rq);			
			
			obtained = obtained_sr.get(tranGID(t.global_organization_ID));
			if(obtained==null) obtained = new RequestData();
			obtained.news.add(t.global_translation_ID);
			obtained_sr.put(tranGID(t.global_organization_ID), obtained);
			orgs.add(tranGID(t.global_organization_ID));
		}
		if(DEBUG) System.out.println("WB_Messages: store: done");
		return;
	}

	private static String newsGID(String global_organization_ID) {
		if (global_organization_ID == null) return NEWS_POOL;
		return global_organization_ID;
	}
	private static String tranGID(String global_organization_ID) {
		if (global_organization_ID == null) return TRAN_POOL;
		return global_organization_ID;
	}
	public boolean empty() {
		return 0==orgs.size()+
				neig.size()+
				cons.size()+
				moti.size()+
				just.size()+
				witn.size()+
				sign.size()+
				tran.size()+
				news.size();
	}

	public void add(WB_Messages requested) {
		// TODO Auto-generated method stub
		
	}
}
