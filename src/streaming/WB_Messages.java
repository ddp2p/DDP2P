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

package streaming;

import static util.Util._;
import hds.ASNSyncPayload;
import hds.ASNSyncRequest;
import hds.SyncAnswer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

import javax.swing.JOptionPane;

import util.Util;

import util.P2PDDSQLException;

import config.Application;
import config.DD;


import data.D_News;
import data.D_OrgDistribution;
import data.D_Organization;
import data.D_Message;
import data.D_Motion;
import data.D_Constituent;
import data.D_Justification;
import data.D_Neighborhood;
import data.D_PeerAddress;
import data.D_Translations;
import data.D_Vote;
import data.D_Witness;


import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

/**
 * The structure containing ArrayLists with various types of data
 * @author msilaghi
 *
 */
public class WB_Messages extends ASNObj{

	static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
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
		Encoder enc = new Encoder();
		enc.addToSequence(Encoder.getEncoder(orgs.toArray(new D_Organization[0])));
		enc.addToSequence(Encoder.getEncoder(cons.toArray(new D_Constituent[0])));
		enc.addToSequence(Encoder.getEncoder(neig.toArray(new D_Neighborhood[0])));
		enc.addToSequence(Encoder.getEncoder(moti.toArray(new D_Motion[0])));
		enc.addToSequence(Encoder.getEncoder(witn.toArray(new D_Witness[0])));
		enc.addToSequence(Encoder.getEncoder(just.toArray(new D_Justification[0])));
		enc.addToSequence(Encoder.getEncoder(sign.toArray(new D_Vote[0])));
		enc.addToSequence(Encoder.getEncoder(news.toArray(new D_News[0])));
		enc.addToSequence(Encoder.getEncoder(tran.toArray(new D_Translations[0])));
		return enc;
	}

	@Override
	public WB_Messages decode(Decoder dec) throws ASN1DecoderFail {
		Decoder d = dec.getContent();
		orgs = d.getFirstObject(true).getSequenceOfAL(D_Organization.getASN1Type(), new D_Organization());
		cons = d.getFirstObject(true).getSequenceOfAL(D_Constituent.getASN1Type(), new D_Constituent());
		neig = d.getFirstObject(true).getSequenceOfAL(D_Neighborhood.getASN1Type(), new D_Neighborhood());
		moti = d.getFirstObject(true).getSequenceOfAL(D_Motion.getASN1Type(), new D_Motion());
		witn = d.getFirstObject(true).getSequenceOfAL(D_Witness.getASN1Type(), new D_Witness());
		just = d.getFirstObject(true).getSequenceOfAL(D_Justification.getASN1Type(), new D_Justification());
		sign = d.getFirstObject(true).getSequenceOfAL(D_Vote.getASN1Type(), new D_Vote());
		news = d.getFirstObject(true).getSequenceOfAL(D_News.getASN1Type(), new D_News());
		tran = d.getFirstObject(true).getSequenceOfAL(D_Translations.getASN1Type(), new D_Translations());
		return this;
	}
	public String toString() {
		String result = "\n WB_Messages[";
		if(orgs != null) result += "\n  orgs = "+ Util.concat(orgs, "\n", "null");
		if(cons != null) result += "\n  cons = "+ Util.concat(cons, "\n", "null");
		if(neig != null) result += "\n  neig = "+ Util.concat(neig, "\n", "null");
		if(moti != null) result += "\n  moti = "+ Util.concat(moti, "\n", "null");
		if(witn != null) result += "\n  witn = "+ Util.concat(witn, "\n", "null");
		if(just != null) result += "\n  just = "+ Util.concat(just, "\n", "null");
		if(sign != null) result += "\n  sign = "+ Util.concat(sign, "\n", "null");
		if(news != null) result += "\n  news = "+ Util.concat(news, "\n", "null");
		if(tran != null) result += "\n  tran = "+ Util.concat(tran, "\n", "null");
		result += "\n]";
		return result;
	}

	public String toSummaryString() {
		String result = "\n WB_Messages[";
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
		if(asr.request.rd==null){
			if(DEBUG) System.out.println("WB_Messages: getRequestedData: exit no data request content");
			return null;
		}
		ArrayList<RequestData> rd = asr.request.rd;
		if(DEBUG) System.out.println("WB_Messages: getRequestedData: data request content #"+rd.size());
		WB_Messages result = new WB_Messages();
		for(RequestData r: rd) {
			for(String gid: r.orgs) {
				if(DEBUG) System.out.println("WB_Messages: getRequestedData: org gid="+gid);
				String org_id = Util.getStringID(D_Organization.getLocalOrgID(gid, gid));

				if(!OrgHandling.serving(asr, org_id)) continue;
				
				if((sa!=null)&&sa.hasOrg(gid)) continue;
				try {
					D_Organization o = new D_Organization(gid,gid);
					if(!o.readyToSend()) continue;
					result.orgs.add(o);
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: got org");
				} catch (Exception e) {
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: I don't have requested org: "+gid);
					//e.printStackTrace();
				}
			}
			for(String gid: r.cons.keySet()) {
				if(DEBUG) System.out.println("WB_Messages: getRequestedData: cons gid="+gid);
				// TODO if gid is sa, then skip to avoid duplicating work 
				if((sa!=null)&&sa.hasConstituent(gid, r.cons.get(gid))) continue;
				try{
					D_Constituent c = new D_Constituent(gid, gid, D_Constituent.EXPAND_NONE);
					if(!c.readyToSend()) continue;
					if(!OrgHandling.serving(asr, c.organization_ID)) continue;
					result.cons.add(c);
				}catch(Exception e){
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: I don't have requested const: "+gid);
					//e.printStackTrace();
				}
				if(DEBUG) System.out.println("WB_Messages: getRequestedData: got cons");
			}
			for(String gid: r.neig) {
				if(DEBUG) System.out.println("WB_Messages: getRequestedData: neigh gid="+gid);
				if((sa!=null)&&sa.hasNeighborhod(gid)) continue;
				try {
					D_Neighborhood n = new D_Neighborhood(gid);
					if(!OrgHandling.serving(asr, Util.getStringID(n.organization_ID))) continue;
					result.neig.add(n);
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: got neig");
				} catch (Exception e) {
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: I don't have requested neig: "+gid);
					//e.printStackTrace();
				}
			}
			for(String gid: r.witn) {
				if(DEBUG) System.out.println("WB_Messages: getRequestedData: witn gid="+gid);
				if((sa!=null)&&sa.hasWitness(gid)) continue;
				D_Witness w = null;
				try {
					w = new D_Witness(gid);
					if(!OrgHandling.serving(asr, w.organization_ID)) continue;
					result.witn.add(w);
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: got witn");
				}
				catch (data.D_NoDataException e) {
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
			for(String gid: r.moti) {
				if(DEBUG) System.out.println("WB_Messages: getRequestedData: moti gid="+gid);
				if((sa!=null)&&sa.hasMotion(gid)) continue;
				try {
					D_Motion m = new D_Motion(gid);
					if(!m.readyToSend()) continue;
					if(!OrgHandling.serving(asr, m.organization_ID)) continue;
					result.moti.add(m);
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: got moti");
				}
				catch (data.D_NoDataException e) {
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: I don't have requested motion: "+gid);
				}
				catch (Exception e) {
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: Error for moti: "+gid);
					e.printStackTrace();
				}
			}
			for(String gid: r.just) {
				if(DEBUG) System.out.println("WB_Messages: getRequestedData: cons just="+gid);
				if((sa!=null)&&sa.hasJustification(gid)) continue;
				try{
					D_Justification j = new D_Justification(gid);
					if(!j.readyToSend()) continue;
					if(!OrgHandling.serving(asr, j.organization_ID)) continue;
					result.just.add(j);
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: got just");
				}
				catch (data.D_NoDataException e) {
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: I don't have requested just: "+gid);
				}
				catch (Exception e) {
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: Error for justification: "+gid);
					e.printStackTrace();
				}
			}			
			for(String gid: r.sign.keySet()) {
				if(DEBUG) System.out.println("WB_Messages: getRequestedData: vote just="+gid);
				if((sa!=null)&&sa.hasSignature(gid, r.sign.get(gid))) continue;
				try{
					D_Vote v = new D_Vote(gid);
					if(!v.readyToSend()) continue;
					if(!OrgHandling.serving(asr, v.organization_ID)) continue;
					result.sign.add(v);
					if(DEBUG) System.out.println("WB_Messages: getRequestedData: got vote");
				}
				catch (data.D_NoDataException e) {
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

	/**
	 * 		Hashtable<String, RequestData> obtained_sr = new Hashtable<String, RequestData>();
		Hashtable<String, RequestData> sq_sr = new Hashtable<String, RequestData>();

	 eventually, we should get mostly org_hash rather than orgGID....

	 * @param r
	 * @param sp
	 * @param obtained
	 * @param orgs : the list of orgs mentioned
	 * @throws P2PDDSQLException
	 */
	public static void store(ASNSyncPayload asa, D_PeerAddress peer, WB_Messages r,
			Hashtable<String, RequestData> sq_sr,
			Hashtable<String, RequestData> obtained_sr,
			HashSet<String> orgs, String dbg_msg) throws P2PDDSQLException {
		RequestData obtained;
		RequestData rq, sol_rq, new_rq; 
		if(DEBUG) System.out.println("WB_Messages: store: start");
		if(r == null) {
			if(DEBUG) System.out.println("WB_Messages: store: exit null requested");
			return;
		}
		for(D_Organization org: r.orgs) { // should we store new orgs like that?
			if(DEBUG) System.out.println("WB_Messages: store: handle org: "+org);
			//org.store(rq);
			///_obtained.orgs.add(org.global_organization_ID);
			//obtained.orgs.add(org.global_organization_ID_hash);
			orgs.add(org.global_organization_ID);

			boolean _changed[] = new boolean[1];
			long id = org.store(peer, _changed, sq_sr.get(org.global_organization_ID), null); // should set blocking new orgs
			if(DEBUG)System.out.println("OrgHandling:updateOrg: sharing: ch="+_changed[0]+
					" br="+org.broadcast_rule+" id="+id+" creat="+org.creator_ID);
			if(DEBUG)System.out.println("OrgHandling:updateOrg: sharing: ch="+_changed[0]+
					" br="+org.broadcast_rule+" id="+id+" creat="+org.creator_ID);
			if(
					(org.broadcast_rule == false) &&
					(id > 0) &&
					(org.creator_ID != null) &&
					(_changed[0])
					)
			{
				if(DEBUG)System.out.println("OrgHandling:updateOrg: sharing: auto="+DD.AUTOMATE_PRIVATE_ORG_SHARING);
				if(DD.AUTOMATE_PRIVATE_ORG_SHARING == 0) {
					int pos = Application.ask(_("In this session, do you want to share data of private orgs\n to all those sending it to you?"),
							_("Share private orgs with your providers"), JOptionPane.YES_NO_OPTION);
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
		for(D_Constituent c: r.cons) {
			if(DEBUG) System.out.println("WB_Messages: store: handle const: "+c);
			rq = sq_sr.get(c.global_organization_ID);
			if(rq==null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			long lid = c.store(sol_rq, new_rq);
			if(lid<=0){
				if(_DEBUG) System.out.println("WB_Messages: store: failed to handled const: "+c+" "+dbg_msg);
				continue;
			}
			rq.update(sol_rq, new_rq);
			sq_sr.put(c.global_organization_ID, rq);			
			
			obtained = obtained_sr.get(c.global_organization_ID);
			if(obtained==null) obtained = new RequestData();
			obtained.cons.put(c.global_constituent_id, DD.EMPTYDATE);
			obtained.cons.put(c.global_constituent_id_hash, DD.EMPTYDATE);
			obtained_sr.put(c.global_organization_ID, obtained);
			orgs.add(c.global_organization_ID);
			//orgs.add(c.global_organization_ID_hash);
		}
		for(D_Neighborhood n: r.neig) {
			if(DEBUG) System.out.println("WB_Messages: store: handle neig: "+n);
			rq = sq_sr.get(n.global_organization_ID);
			if(rq==null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			long lid=n.store(sol_rq, new_rq);
			if(lid<=0){
				if(_DEBUG) System.out.println("WB_Messages: store: failed to handled neigh: "+n+" "+dbg_msg);
				continue;
			}
			rq.update(sol_rq, new_rq);
			sq_sr.put(n.global_organization_ID, rq);			
			
			obtained = obtained_sr.get(n.global_organization_ID);
			if(obtained==null) obtained = new RequestData();
			obtained.neig.add(n.global_neighborhood_ID);
			obtained_sr.put(n.global_organization_ID, obtained);
			orgs.add(n.global_organization_ID);
		}
		for(D_Witness w: r.witn) {
			if(DEBUG) System.out.println("WB_Messages: store: handle witn: "+w);
			rq = sq_sr.get(w.global_organization_ID);
			if(rq==null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			long lid = w.store(sol_rq, new_rq);
			if(lid<=0){
				if(_DEBUG) System.out.println("WB_Messages: store: failed to handled witn: "+w+" "+dbg_msg);
				continue;
			}
			rq.update(sol_rq, new_rq);
			sq_sr.put(w.global_organization_ID, rq);			
			
			obtained = obtained_sr.get(w.global_organization_ID);
			if(obtained==null) obtained = new RequestData();
			obtained.witn.add(w.global_witness_ID);
			obtained_sr.put(w.global_organization_ID, obtained);
			orgs.add(w.global_organization_ID);
		}
		for(D_Motion m: r.moti) {
			if(DEBUG) System.out.println("WB_Messages: store: handle moti: "+m);
			if(DEBUG) System.out.println("WB_Messages: store: handle moti: "+m.global_motionID+" "+m.motion_title);
			rq = sq_sr.get(m.global_organization_ID);
			if(rq==null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			long lid=m.store(sol_rq, new_rq);
			if(lid<=0){
				if(_DEBUG) System.out.println("WB_Messages: store: failed to handled motion: "+m+" "+dbg_msg);
				continue;
			}
			rq.update(sol_rq, new_rq);
			sq_sr.put(m.global_organization_ID, rq);			
			
			obtained = obtained_sr.get(m.global_organization_ID);
			if(obtained==null) obtained = new RequestData();
			obtained.moti.add(m.global_motionID);
			obtained_sr.put(m.global_organization_ID, obtained);
			orgs.add(m.global_organization_ID);
			if(DEBUG) System.out.println("WB_Messages: store: handled moti: "+m.global_motionID+" "+m.motion_title);
		}
		for(D_Justification j: r.just) {
			if(DEBUG) System.out.println("WB_Messages: store: handle just: "+j);
			if(j.global_organization_ID == null){
				j.global_organization_ID = j.guessOrganizationGID();
				if(j.global_organization_ID == null) {
					if(_DEBUG) System.out.println("WB_Messages: store: cannot determine org: skip");
					continue;
				}
			}
			rq = sq_sr.get(j.global_organization_ID);
			if(rq==null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			long lid=j.store(sol_rq, new_rq);
			if(lid<=0){
				if(_DEBUG) System.out.println("WB_Messages: store: failed to handled just: "+j+" "+dbg_msg);
				continue;
			}
			rq.update(sol_rq, new_rq);
			sq_sr.put(j.global_organization_ID, rq);			
			
			obtained = obtained_sr.get(j.global_organization_ID);
			if(obtained==null) obtained = new RequestData();
			obtained.just.add(j.global_justificationID);
			obtained_sr.put(j.global_organization_ID, obtained);
			orgs.add(j.global_organization_ID);
		}
		for(D_Vote v: r.sign) {
			if(DEBUG) System.out.println("WB_Messages: store: handle vote: "+v);
			try{
				rq = sq_sr.get(v.global_organization_ID);
				if(rq==null) rq = new RequestData();
				sol_rq = new RequestData();
				new_rq = new RequestData();
				long lid=v.store(sol_rq, new_rq);
				if(lid<=0){
					if(_DEBUG) System.out.println("WB_Messages: store: failed to handled vote: "+v+" "+dbg_msg);
					continue;
				}
				if(DEBUG) System.out.println("WB_Messages: store: handled vote: "+v);
				rq.update(sol_rq, new_rq);
				sq_sr.put(v.global_organization_ID, rq);			
				
				obtained = obtained_sr.get(v.global_organization_ID);
				if(obtained==null) obtained = new RequestData();
				obtained.sign.put(v.global_vote_ID, DD.EMPTYDATE);
				obtained_sr.put(v.global_organization_ID, obtained);
				orgs.add(v.global_organization_ID);
			}catch(Exception e){
				if(_DEBUG) System.out.println("WB_Messages: store: failed vote: "+dbg_msg+" for v="+v);
				e.printStackTrace();
			}
		}
		for(D_News w: r.news) {
			if(DEBUG) System.out.println("WB_Messages: store: handle news: "+w);
			rq = sq_sr.get(w.global_organization_ID);
			if(rq==null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			long lid=w.store(sol_rq, new_rq);
			if(lid<=0){
				if(_DEBUG) System.out.println("WB_Messages: store: failed to handled news: "+w+" "+dbg_msg);
				continue;
			}
			rq.update(sol_rq, new_rq);
			sq_sr.put(w.global_organization_ID, rq);			
			
			obtained = obtained_sr.get(w.global_organization_ID);
			if(obtained==null) obtained = new RequestData();
			obtained.news.add(w.global_news_ID);
			obtained_sr.put(w.global_organization_ID, obtained);
			orgs.add(w.global_organization_ID);
		}
		for(D_Translations t: r.tran) {
			if(DEBUG) System.out.println("WB_Messages: store: handle tran: "+t);
			rq = sq_sr.get(t.global_organization_ID);
			if(rq==null) rq = new RequestData();
			sol_rq = new RequestData();
			new_rq = new RequestData();
			long lid=t.store(sol_rq, new_rq);
			if(lid<=0){
				if(_DEBUG) System.out.println("WB_Messages: store: failed to handled translation: "+t+" "+dbg_msg);
				continue;
			}
			rq.update(sol_rq, new_rq);
			sq_sr.put(t.global_organization_ID, rq);			
			
			obtained = obtained_sr.get(t.global_organization_ID);
			if(obtained==null) obtained = new RequestData();
			obtained.news.add(t.global_translation_ID);
			obtained_sr.put(t.global_organization_ID, obtained);
			orgs.add(t.global_organization_ID);
		}
		if(DEBUG) System.out.println("WB_Messages: store: done");
		return;
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
