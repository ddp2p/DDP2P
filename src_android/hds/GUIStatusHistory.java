package hds;

import java.util.ArrayList;

import util.P2PDDSQLException;
import util.Util;
import config.ConstituentListener;
import config.DD;
import config.Identity;
import config.JustificationsListener;
import config.MotionsListener;
import config.OrgListener;
import config.PeerListener;
import data.D_Constituent;
import data.D_Justification;
import data.D_Motion;
import data.D_Neighborhood;
import data.D_News;
import data.D_Organization;
import data.D_Peer;
import data.HandlingMyself_Peer;

public
class GUIStatusHistory implements OrgListener, MotionsListener, JustificationsListener, PeerListener, ConstituentListener{
	private final class DispatcherOrg extends Thread {
		private D_Organization org;
		private OrgListener l;

		public DispatcherOrg(OrgListener l, D_Organization org) {
			this.l = l;
			this.org = org;
		}

		public void run(){
			if(DEBUG) System.out.println("GUIStatus:DispatcherOrg: disp");
			l.orgUpdate(org.organization_ID, D_Organization.A_NON_FORCE_COL, org);
			if(DEBUG) System.out.println("GUIStatus:DispatcherOrg: disp done");
		}
	}
	private static final int MAX_HISTORY = 20;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	static ArrayList<GUIStatus> history = new ArrayList<GUIStatus>();
	static int crt = 0;
	
	ArrayList<PeerListener> peer_me_listeners = new ArrayList<PeerListener>();
	ArrayList<PeerListener> peer_selected_listeners = new ArrayList<PeerListener>();
	ArrayList<ConstituentListener> constituent_me_listeners = new ArrayList<ConstituentListener>();
	ArrayList<ConstituentListener> constituent_selected_listeners = new ArrayList<ConstituentListener>();
	ArrayList<OrgListener> org_listeners = new ArrayList<OrgListener>();
	ArrayList<MotionsListener> mot_listeners = new ArrayList<MotionsListener>();
	ArrayList<JustificationsListener> just_listeners = new ArrayList<JustificationsListener>();


	/**
	 * Add one empty entry
	 */
	public GUIStatusHistory() {
		//GUIStatus original = new GUIStatus(null);
		//history.add(original);
		D_Peer peer = null;
		synchronized (DD.status_monitor) {
			peer = HandlingMyself_Peer.get_myself_or_null();
			this.setMePeer(peer);
		}
			//original.setMePeer(peer);
	}
	public void firePeerSelectedListeners() {
		if(DEBUG) System.out.println("GUIStatus: peer_selected fireListener #"+peer_selected_listeners.size());
		D_Peer peer = getSelectedPeer();
		for(PeerListener l: peer_selected_listeners) {
			if(DEBUG) System.out.println("GUIStatus: fireListener: l="+l);
			try{
				if(peer==null) l.update_peer(peer, null, false, true);
				else l.update_peer(peer, peer.component_basic_data.name, false, true);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	public void addPeerSelectedStatusListener(PeerListener l) {
		if(DEBUG) System.out.println("GUIStatus: addPeerSelected Listener "+l);
		if(peer_selected_listeners.contains(l)) return;
		peer_selected_listeners.add(l);
		D_Peer peer = getSelectedPeer();
		if(peer != null) {
			if(DEBUG) System.out.println("GUIStatus: addPeerSelected Listener update "+l);
			l.update_peer(peer, peer.component_basic_data.name, false, true);
		}else{
			if(DEBUG) System.out.println("GUIStatus: addPeerSelected Listener no peer_selected to update "+l);
		}
	}
	public void removePeerSelectedListener(PeerListener l){
		if(DEBUG) System.out.println("GUIStatus: removePeerSelected Listener "+l);
		peer_selected_listeners.remove(l);
	}
	public void firePeerMeListeners() {
		if(DEBUG) System.out.println("GUIStatus: peer_me fireListener #"+peer_me_listeners.size());
		D_Peer peer = this.getMePeer();
		for(PeerListener l: peer_me_listeners) {
			if(DEBUG) System.out.println("GUIStatus: firePeerMeListener: l="+l);
			try{
				if(peer==null) l.update_peer(peer, null, true, false);
				else l.update_peer(peer, peer.component_basic_data.name, true, false);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	public void addPeerMeStatusListener(PeerListener l) {
		if(DEBUG) System.out.println("GUIStatus: addPeerMe Listener "+l);
		if(peer_me_listeners.contains(l)) return;
		peer_me_listeners.add(l);
		D_Peer peer = getMePeer();
		if(peer != null) {
			if(DEBUG) System.out.println("GUIStatus: addPeerMe Listener update "+l);
			l.update_peer(peer, peer.component_basic_data.name, true, false);
		}else{
			if(DEBUG) System.out.println("GUIStatus: addPeerMe Listener no peer_me to update "+l);
		}
	}
	public void removePeerMeListener(PeerListener l){
		if(DEBUG) System.out.println("GUIStatus: removePeerMe Listener "+l);
		peer_me_listeners.remove(l);
	}
	public void fireConstituentSelectedListeners() {
		if(DEBUG) System.out.println("GUIStatus: const_sel fireListener #"+peer_selected_listeners.size());
		D_Constituent c = getSelectedConstituent();
		for(ConstituentListener l: constituent_selected_listeners) {
			if(DEBUG) System.out.println("GUIStatus: fireCSListener: l="+l);
			try{
				if(c==null) l.constituentUpdate(c, false, true);
				else l.constituentUpdate(c, false, true);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	public void addConstituentSelectedStatusListener(ConstituentListener l) {
		if(DEBUG) System.out.println("GUIStatus: addConstituentSelectedStatusListener Listener "+l);
		if(constituent_selected_listeners.contains(l)) return;
		constituent_selected_listeners.add(l);
		D_Constituent c = getSelectedConstituent();
		if(c != null) {
			if(DEBUG) System.out.println("GUIStatus: addConstituentSelectedStatusListener Listener update "+l);
			l.constituentUpdate(c, false, true);
		}else{
			if(DEBUG) System.out.println("GUIStatus: addConstituentSelectedStatusListener Listener no peer_sel to update "+l);
		}
	}
	public void removeConstituentSelectedListener(ConstituentListener l){
		if(DEBUG) System.out.println("GUIStatus: removeConstituentSelectedListener Listener "+l);
		constituent_selected_listeners.remove(l);
	}
	public void fireConstituentMeListeners() {
		if(DEBUG) System.out.println("GUIStatus: const_me fireListener #"+peer_me_listeners.size());
		D_Constituent c = getMeConstituent();
		for(ConstituentListener l: constituent_me_listeners) {
			if(DEBUG) System.out.println("GUIStatus: fireCMListener: l="+l);
			try{
				if(c==null) l.constituentUpdate(c, true, false);
				else l.constituentUpdate(c, true, false);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	public void addConstituentMeStatusListener(ConstituentListener l) {
		if(DEBUG) System.out.println("GUIStatus: addConstituentMeStatusListener Listener "+l);
		if(constituent_me_listeners.contains(l)) return;
		constituent_me_listeners.add(l);
		D_Constituent c = getMeConstituent();
		if(c != null) {
			if(DEBUG) System.out.println("GUIStatus: addConstituentMeStatusListener Listener update "+l);
			l.constituentUpdate(c, true, false);
		}else{
			if(DEBUG) System.out.println("GUIStatus: addConstituentMeStatusListener Listener no peer_me to update "+l);
		}
	}
	public void removeConstituentMeListener(ConstituentListener l){
		if(DEBUG) System.out.println("GUIStatus: removeConstituentMeListener Listener "+l);
		constituent_me_listeners.remove(l);
	}
	public void fireOrgListeners() {
		if(DEBUG) System.out.println("GUIStatus: fireListener #"+org_listeners.size());
		D_Organization org = getSelectedOrg();
		for(OrgListener l: org_listeners) {
			if(DEBUG) System.out.println("GUIStatus: fireListener: l="+l);
			try{
				if(org==null) l.orgUpdate(null, D_Organization.A_NON_FORCE_COL, org);
				else l.orgUpdate(org.organization_ID, D_Organization.A_NON_FORCE_COL, org);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	public void addOrgStatusListener(OrgListener l){
		if(DEBUG) System.out.println("GUIStatus: addOrgs Listener "+l);
		if(org_listeners.contains(l)) return;
		org_listeners.add(l);
		D_Organization org = getSelectedOrg();
		if(org != null){
			if(DEBUG) System.out.println("GUIStatus: addOrgs Listener update "+l);
			l.orgUpdate(org.organization_ID, D_Organization.A_NON_FORCE_COL, org);
			//new DispatcherOrg(l, org).start();
			
		}else{
			if(DEBUG) System.out.println("GUIStatus: addOrgs Listener no org to update "+l);
		}
		if(DEBUG) System.out.println("GUIStatus: done addOrgs Listener "+l);
	}
	public void removeOrgListener(OrgListener l){
		if(DEBUG) System.out.println("GUIStatus: removeOrgs Listener "+l);
		org_listeners.remove(l);
	}

	public void fireMotListeners() {
		if(DEBUG) System.out.println("GUIStatus: fireListener #"+mot_listeners.size());
		D_Motion mot = getSelectedMot();
		for(MotionsListener l: mot_listeners) {
			if(DEBUG) System.out.println("GUIStatus: fireListener: l="+l);
			try{
				if(mot==null) l.motion_update(null, 0, mot);
				else l.motion_update(mot.motionID, 0, mot);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	public void addMotionStatusListener(MotionsListener l) {
		if(DEBUG) System.out.println("GUIStatus: addMots Listener "+l);
		if(mot_listeners.contains(l)) return;
		mot_listeners.add(l);
		D_Motion mot = getSelectedMot();
		if(mot != null){
			if(DEBUG) System.out.println("GUIStatus: addMots Listener update "+l);
			l.motion_update(mot.motionID, 0, mot);
		}
	}
	public void removeMotListener(MotionsListener l){
		if(DEBUG) System.out.println("GUIStatus: removeMots Listener "+l);
		mot_listeners.remove(l);
	}

	public void fireJustListeners(boolean sync) {
		if(DEBUG) System.out.println("GUIStatusHistory: fireListener #"+just_listeners.size());
		D_Justification just = getSelectedJust();
		for(JustificationsListener l: just_listeners) {
			if(DEBUG) System.out.println("GUIStatus: fireListener: l="+l);
			try{
				if(just==null) l.justUpdate(null, 0, sync, just);
				else l.justUpdate(just.justification_ID, 0, sync, just);
			}catch(Exception e){e.printStackTrace();}
		}
	}
	public void addJustificationStatusListener(JustificationsListener l) {
		if(DEBUG) System.out.println("GUIStatus: addJusts Listener "+l);
		if(just_listeners.contains(l)) return;
		just_listeners.add(l);
		D_Justification just = getSelectedJust();
		if(just != null){
			if(DEBUG) System.out.println("GUIStatus: addJusts Listener update "+l);
			l.justUpdate(just.justification_ID, 0, false, just);
		}
	}
	public void removeJustificationListener(JustificationsListener l){
		if(DEBUG) System.out.println("GUIStatus: removeJusts Listener "+l);
		just_listeners.remove(l);
	}
	/**
	 * Adds a new GUIStatus at the top of the stack (duplicating existing one)
	 * and discarding the bottom if the stack is too high
	 */
	static private void pushStack() {
		GUIStatus n = null;
		if (history.size() > 0) n = new GUIStatus(history.get(0));
		else n = new GUIStatus(null);
		history.add(0, n);
		if(history.size()>MAX_HISTORY) history.remove(history.size()-1);
		if(DEBUG) System.out.println("GUIS: pushStack n="+n.toString(1));
//		Util.printCallPath("stack");
	}
	void printCrt(String context){
		if(DEBUG) System.out.println("GUIS: printCrt ctx="+context+" /"+history.size()+"\n "+history.get(crt).toString(1));
	}
	void cleanForward(){
		while (crt > 0) {
			crt --;
			history.remove(0);
			if(DEBUG) System.out.println("GUIS: cleanForward: crt="+crt);
		}
	}
	synchronized
	public void setTab(int tab){
		if(DEBUG) System.out.println("GUIStatus: setTab "+tab);
		if(tab == getTab()){
			if(DEBUG) System.out.println("GUIStatusHistory: setTab same old selected tab");
			return;
		}else{
			if(DEBUG) System.out.println("GUIStatusHistory: setTab new selected tab="+tab+" vs "+getTab());
		}
		cleanForward();
		pushStack();
		history.get(crt).setSelectedTab(tab);
		//this.firePeerSelectedListeners();
		printCrt("Tab");
	}
	synchronized
	void setSelectedPeer(D_Peer peer){
		if(DEBUG) System.out.println("GUIStatus: setSelectedPeer "+((peer==null)?"null":peer.component_basic_data.name));
		if(samePeer(peer, getSelectedPeer())) {
			if(DEBUG) System.out.println("GUIStatusHistory: setSelectedPeer same old selected peer");
			return;
		}
		cleanForward();
		pushStack();
		history.get(crt).setSelectedPeer(peer);
		this.firePeerSelectedListeners();
		printCrt("SelPeer");
	}
	/**
	 * To be replaced with the next
	 * @param me
	 */
	/*
	public void setMePeer(D_PeerAddress me) {
		Util.printCallPath("");
		System.exit(1);
	}
	*/
	synchronized
	public void setMePeer(D_Peer peer) {
		//boolean DEBUG = true;
		if(DEBUG) System.out.println("GUIStatus: setMePeer "+((peer==null)?"NULL":"\""+peer.component_basic_data.name+"\""));
		if(samePeer(peer, getMePeer())) {
			if(DEBUG) System.out.println("GUIStatusHistory: setMePeer same old me_peer");
			return;
		}
		if(DEBUG) Util.printCallPath("call");
		if(DEBUG) System.out.println("GUIStatus: setMePeer "+peer);
		cleanForward();
		pushStack();
		history.get(crt).setMePeer(peer);
		this.firePeerMeListeners();
		printCrt("MePeer");
	}
	synchronized
	void setSelectedConstituent(D_Constituent c){
		if(DEBUG) System.out.println("GUIStatus: setSelectedConst "+((c==null)?"null":c.getName()));
		if(sameConstituent(c, getSelectedConstituent())) {
			if(DEBUG) System.out.println("GUIStatusHistory: setSelectedConst same old selected cons");
			return;
		}else
			if(DEBUG) System.out.println("GUIStatusHistory: setSelCons new SelCons");
		cleanForward();
		pushStack();
		history.get(crt).setSelectedConstituent(c);
		this.fireConstituentSelectedListeners();
		printCrt("SelConst");
	}
	synchronized
	public void setMeConstituent(D_Constituent c) {
		if(DEBUG) System.out.println("GUIStatus: setMeConstituent "+((c==null)?"null":c.getName()));
		if(sameConstituent(c, getMeConstituent())){
			if(DEBUG) System.out.println("GUIStatusHistory: setMeConstituent same old me_const");
			return;
		}else{
			if(DEBUG) System.out.println("GUIStatusHistory: setCons new MeCons");
		}
		if(crt>0){
			if(sameConstituent(c, getMeConstituent(crt-1))){
				if(DEBUG) System.out.println("GUIStatusHistory: setMeConstituent same old me_const");
				crt--;
				return;
			}else{
				if(DEBUG) System.out.println("GUIStatusHistory: setCons new prev MeCons");
			}
		}
		cleanForward();
		pushStack();
		history.get(crt).setMeConstituent(c);
		printCrt("MeConst");
		this.fireConstituentMeListeners();
		if(DEBUG) System.out.println("GUIStatusHistory: setCons done");
	}
	public synchronized
	void setSelectedOrg(D_Organization org){
		if(DEBUG) System.out.println("GUIStatusHistory: setOrgs "+((org==null)?null:org.name));
		if(sameOrganization(org, getSelectedOrg())){
			if(DEBUG) System.out.println("GUIStatusHistory: setOrgs same old org");
			return;
		}
		cleanForward();
		//	if(DEBUG) System.out.println("GUIStatusHistory: setOrgs removed to crt="+crt);
		pushStack();
		//if(DEBUG) System.out.println("GUIStatusHistory: setOrgs set to="+((org==null)?null:org.name));
		history.get(crt).setSelectedOrg(org);
		printCrt("Org");
		this.fireOrgListeners();
		if(DEBUG) System.out.println("GUIStatusHistory: setOrgs done");
	}
	public synchronized
	void setSelectedMot(D_Motion mot){
		if(DEBUG) System.out.println("GUIStatusHistory: setMots "+((mot==null)?null:mot.motion_title));
		if(sameMotion(mot, getSelectedMot())){
			if(DEBUG) System.out.println("GUIStatusHistory: setMots same old mot");
			return;
		}else
			if(DEBUG) System.out.println("GUIStatusHistory: setMots new mot");
		cleanForward();
		pushStack();
		if(DEBUG) System.out.println("GUIStatusHistory: setMots set to="+((mot==null)?null:mot.motion_title));
		history.get(crt).setSelectedMot(mot);
		printCrt("Mot");
		this.fireMotListeners();
		if(DEBUG) System.out.println("GUIStatusHistory: setMots done");
	}
	public synchronized
	void setSelectedJust(D_Justification just, boolean sync){
		if(DEBUG) System.out.println("GUIStatusHistory: setJusts "+((just==null)?null:just.justification_title));
		if(sameJust(just, getSelectedJust())) {
			if(DEBUG) System.out.println("GUIStatusHistory: setJusts same old just");
			return;
		}
		cleanForward();
		//	if(DEBUG) System.out.println("GUIStatusHistory: setJusts removed to crt="+crt);
		pushStack();
		if(DEBUG) System.out.println("GUIStatusHistory: setJusts set to="+((just==null)?null:just.justification_title));
		history.get(crt).setSelectedJust(just);
		this.fireJustListeners(sync);
		printCrt("Just");
	}
	static public boolean samePeer(D_Peer c, D_Peer d) {
		if((c==null)&&(d==null)) return true;
		if((c==null)||(d==null)) return false;
		return (c._peer_ID==d._peer_ID);
	}
	static public boolean sameConstituent(D_Constituent c, D_Constituent d) {
		if((c==null)&&(d==null)) return true;
		if((c==null)||(d==null)) return false;
		if((c.constituent_ID==null)&&(d.constituent_ID==null)) return true;
		if((c.constituent_ID==null)||(d.constituent_ID==null)) return false;
		return c.constituent_ID.equals(d.constituent_ID);
	}
	static public boolean sameOrganization(D_Organization c, D_Organization d) {
		if((c==null)&&(d==null)) return true;
		if((c==null)||(d==null)) return false;
		if((c.organization_ID==null)&&(d.organization_ID==null)) return true;
		if((c.organization_ID==null)||(d.organization_ID==null)) return false;
		return c.organization_ID.equals(d.organization_ID);
	}
	static private boolean sameMotion(D_Motion c, D_Motion d) {
		if((c==null)&&(d==null)) return true;
		if((c==null)||(d==null)) return false;
		if((c.motionID==null)&&(d.motionID==null)) return true;
		if((c.motionID==null)||(d.motionID==null)) return false;
		return c.motionID.equals(d.motionID);
	}
	static private boolean sameJust(D_Justification c, D_Justification d) {
		if((c==null)&&(d==null)) return true;
		if((c==null)||(d==null)) return false;
		if((c.justification_ID==null)&&(d.justification_ID==null)) return true;
		if((c.justification_ID==null)||(d.justification_ID==null)) return false;
		return c.justification_ID.equals(d.justification_ID);
	}
	synchronized
	public void printHistory() {
		for(int i=0; i<history.size(); i++) {
			if(i==crt) System.out.print(">");
			System.out.println(i+":"+history.get(i).toString(2));
		}
	}
	synchronized
	public boolean back(){
		if (DEBUG) printHistory();
		if(crt == history.size()-1){
			if (DEBUG) System.out.println("GUISH: back: at the bottom: crt="+crt);
			return false; // cannot go further back
		}
		crt++;
		tellListeners(crt, crt-1);
		if (DEBUG) System.out.println("GUISH: back: from "+(crt-1)+" to "+crt);
		return true;
	}
	synchronized
	public boolean forward(){
		if (DEBUG) printHistory();
		if(crt == 0) return false; // cannot go further forward
		crt--;
		tellListeners(crt, crt+1);
		if (DEBUG) System.out.println("GUISH: back: from "+(crt+1)+" to "+crt);
		return true;
	}
	private void tellListeners(int crt, int previous) {
		D_Organization org = getSelectedOrg();
		D_Organization o_prev = getSelectedOrg(previous);
		if(org!=o_prev){
			if (DEBUG) System.out.println("GUISH: tellListeners: new org "+((org==null)?"null":org.name));
			fireOrgListeners();
		}

		D_Motion mot = getSelectedMot();
		D_Motion m_prev = getSelectedMot(previous);
		if(mot!=m_prev){
			if (DEBUG) System.out.println("GUISH: tellListeners: new mot "+((mot==null)?"null":mot.motion_title.title_document.getDocumentUTFString()));
			fireMotListeners();
		}
		
		D_Constituent selcon = getSelectedConstituent();
		D_Constituent sc_prev = getSelectedConstituent(previous);
		if(selcon!=sc_prev){
			if (DEBUG) System.out.println("GUISH: tellListeners: new selcon "+((selcon==null)?"null":selcon.getName()));
			fireConstituentSelectedListeners();
		}
		
		D_Constituent mecon = getMeConstituent();
		D_Constituent mc_prev = getMeConstituent(previous);
		if(mecon!=mc_prev){
			if (DEBUG) System.out.println("GUISH: tellListeners: new mecon "+((mecon==null)?"null":mecon.getName()));
			fireConstituentMeListeners();
		}
		
		D_Peer selpeer = getSelectedPeer();
		D_Peer sp_prev = getSelectedPeer(previous);
		if(selpeer!=sp_prev){
			if (DEBUG) System.out.println("GUISH: tellListeners: new selpeer "+((selpeer==null)?"null":selpeer.component_basic_data.name));
			firePeerSelectedListeners();
		}
		
		D_Peer mepeer = getMePeer();
		D_Peer mp_prev = getMePeer(previous);
		if(mepeer!=mp_prev){
			if (DEBUG) System.out.println("GUISH: tellListeners: new mepeer "+((mepeer==null)?"null":mepeer.component_basic_data.name));
			firePeerMeListeners();
		}
		
		int crtTab = getTab();
		int prevTab = getTab(previous);
		if((crtTab != prevTab)&&(crtTab > 0)) {
			if (DEBUG) System.out.println("GUISH: tellListeners: new tab "+crtTab);
		}
	}

	boolean checkIdx(int i){
		if (DEBUG) System.out.println("GUISH: checkIdx: "+i+" < "+history.size());
		return (0<=i)&&(i<history.size());
	}
	public synchronized
	int getTab() {
		return getTab(crt);
	}
	synchronized
	D_Peer getSelectedPeer() {
		return history.get(crt).getSelectedPeer();
	}
	synchronized
	D_Peer getMePeer() {
		if (crt >= history.size()) return null;
		return history.get(crt).getMePeer();
	}
	public synchronized
	D_Constituent getSelectedConstituent() {
		return history.get(crt).getSelectedConstituent();
	}
	public synchronized
	D_Constituent getMeConstituent() {
		return history.get(crt).getMeConstituent();
	}
	public synchronized
	D_Organization getSelectedOrg() {
		return getSelectedOrg(crt);
	}
	public synchronized
	D_Motion getSelectedMot() {
		return getSelectedMot(crt);
	}
	public synchronized
	D_Justification getSelectedJust() {
		return getSelectedJust(crt);
	}
	private
	int getTab(int prev) {
		if(!checkIdx(prev)) return -1;
		return history.get(prev).getSelectedTab();
	}
	synchronized
	D_Peer getSelectedPeer(int prev) {
		if(!checkIdx(prev)) return null;
		return history.get(prev).getSelectedPeer();
	}
	synchronized
	D_Peer getMePeer(int prev) {
		if(!checkIdx(prev)) return null;
		return history.get(prev).getMePeer();
	}
	private
	D_Constituent getSelectedConstituent(int prev) {
		if(!checkIdx(prev)) return null;
		return history.get(prev).getSelectedConstituent();
	}
	private
	D_Constituent getMeConstituent(int prev) {
		if(!checkIdx(prev)) return null;
		return history.get(prev).getMeConstituent();
	}
	private
	D_Organization getSelectedOrg(int prev) {
		//if(prev<0) return null;
		if(!checkIdx(prev)) return null;
		return history.get(prev).getSelectedOrg();
	}
	private
	D_Motion getSelectedMot(int prev) {
		//if(prev<0) return null;
		if(!checkIdx(prev)) return null;
		return history.get(prev).getSelectedMot();
	}
	private
	D_Justification getSelectedJust(int prev) {
		//if(prev<0) return null;
		if(!checkIdx(prev)) return null;
		return history.get(prev).getSelectedJust();
	}
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
		if((org==null) &&  (orgID!=null)) {
			try {
				org = new D_Organization(Util.lval(orgID));
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return;
			}
		}
		setSelectedOrg(org);
	}
	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
		if((org==null) &&  (orgID!=null)) {
			try {
				org = new D_Organization(Util.lval(orgID));
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return;
			}
		}
		setSelectedOrg(org);
	}
	@Override
	public void motion_update(String motID, int col, D_Motion mot) {
		if((mot == null) &&  (motID != null)) {
			try {
				mot = new D_Motion(Util.lval(motID));
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return;
			}
		}
		setSelectedMot(mot);
	}
	@Override
	public void motion_forceEdit(String motID) {
		D_Motion mot = null;
		if((mot == null) &&  (motID != null)) {
			try {
				mot = new D_Motion(Util.lval(motID));
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return;
			}
		}
		setSelectedMot(mot);
	}
	@Override
	public void justUpdate(String justID, int col, boolean db_sync, D_Justification just) {
		if((just == null) &&  (justID != null)) {
			try {
				just = new D_Justification(Util.lval(justID));
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return;
			}
		}
		if(DEBUG) System.out.println("GUIStatusHistory: setSelected="+justID);
		setSelectedJust(just, db_sync);
	}
	@Override
	public void forceJustificationEdit(String justID) {
		D_Justification just = null;
		if((just == null) &&  (justID != null)) {
			try {
				just = new D_Justification(Util.lval(justID));
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return;
			}
		}
		setSelectedJust(just, false);
	}
	@Override
	public void update_peer(D_Peer peer, String my_peer_name,
			boolean me, boolean selected) {
		if(DEBUG) System.out.println("GUIStatusHistory: setSelected="+((peer==null)?"null":peer.component_basic_data.name));
		if(selected) setSelectedPeer(peer);
		if(me) setMePeer(peer);
	}
	@Override
	public void constituentUpdate(D_Constituent c, boolean me, boolean selected) {
		if(DEBUG) System.out.println("GUIStatusHistory: setSelected="+((c==null)?"null":c.getName()));
		if(selected) setSelectedConstituent(c);
		if(me) setMeConstituent(c);
	}
	public boolean is_crt_peer(D_Peer candidate) {
		//int i = 0;
		for (GUIStatus h : history) {
			if (h.selected_peer == candidate) return true;
			if (h.myself_peer == candidate) return true;
			//i ++;
			//if (i > MAX_HISTORY) return false;
		}
		return false;
	}
}

class GUIStatus {
	private static final int B_MOTION = 3;
	private static final int B_ORGANIZATION = 2;
	private static final int B_IDENTITY = 1;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	Identity selected_identity;
	//Identity.current_id_branch;
	D_Peer myself_peer;
	D_Peer selected_peer;
	D_Organization selected_organization;
	D_Neighborhood selected_neighborhood;
	D_Constituent myself_constituent;
	D_Constituent selected_constituent;
	D_Motion selected_motion;
	D_Justification selected_justification;
	D_News selected_news;
	int crt_tab = -1;
	
	public GUIStatus(GUIStatus guiStatus) {
		if(guiStatus!=null) init(guiStatus);
	}
	/**
	 * spaces
	 * @param i
	 * @return
	 */
	public String s(int i){
		String r="";
		for(int k=0; k<i; k++) r+=" ";
		return r;
	}
	public String toString(int i) {
		String r="tab ="+crt_tab+"\n";
		if(selected_identity!=null) r+=s(i)+"id  ="+selected_identity.name+"\n";
		if(myself_peer!=null) r+=s(i)+"me  ="+myself_peer.component_basic_data.name+"\n";
		if(selected_peer!=null) r+=s(i)+"peer="+selected_peer.component_basic_data.name+"\n";
		if(selected_organization!=null) r+=s(i)+"org ="+selected_organization.name+"\n";
		if(selected_neighborhood!=null) r+=s(i)+"neig="+selected_neighborhood.name+"\n";
		if(myself_constituent!=null) r+=s(i)+"me_c="+myself_constituent.getName()+"\n";
		if(selected_constituent!=null) r+=s(i)+"cons="+selected_constituent.getName()+"\n";
		if(selected_motion!=null) r+=s(i)+"moti="+selected_motion.motion_title.title_document.getDocumentUTFString()+"\n";
		if(selected_justification!=null) r+=s(i)+"just="+selected_justification.justification_title.title_document.getDocumentUTFString()+"\n";
		if(selected_news!=null) r+=s(i)+"news="+selected_news.title.title_document.getDocumentUTFString()+"\n";
		return r;
	}

	public D_Constituent getMeConstituent() {
		return myself_constituent;
	}

	public D_Constituent getSelectedConstituent() {
		return selected_constituent;
	}

	private void init(GUIStatus s) {
		selected_identity = s.selected_identity;
		myself_peer = s.myself_peer;
		selected_peer = s.selected_peer;
		selected_organization = s.selected_organization;
		selected_neighborhood = s.selected_neighborhood;
		myself_constituent = s.myself_constituent;
		selected_constituent = s.selected_constituent;
		selected_motion = s.selected_motion;
		selected_justification = s.selected_justification;
		selected_news = s.selected_news;
		crt_tab = s.crt_tab;
	}


	public void setSelectedTab(int tab) {
		backtrack(0);
		crt_tab = tab;
	}
	int getSelectedTab(){
		return crt_tab;
	}
	public void setSelectedOrg(D_Organization org) {
		backtrack(0);
		selected_organization = org;
	}
	D_Organization getSelectedOrg(){
		return selected_organization;
	}
	void setSelectedPeer(D_Peer peer){
		backtrack(0);
		selected_peer = peer;
	}
	void setSelectedConstituent(D_Constituent c){
		backtrack(0);
		selected_constituent = c;
	}
	void setMeConstituent(D_Constituent c){
		backtrack(0);
		myself_constituent = c;
	}
	D_Peer getSelectedPeer(){
		return selected_peer;
	}
	void setMePeer(D_Peer peer){
		backtrack(0);
		myself_peer = peer;
	}
	D_Peer getMePeer(){
		return myself_peer;
	}
	public void setSelectedMot(D_Motion mot) {
		backtrack(0);
		selected_motion = mot;
	}
	D_Motion getSelectedMot(){
		return selected_motion;
	}
	public void setSelectedJust(D_Justification just) {
		backtrack(0);
		selected_justification = just;
	}
	public D_Justification getSelectedJust() {
		return selected_justification;
	}
	void setSelectedIdentity(Identity identity){
		backtrack(0);
		selected_identity = identity;
	}
	Identity getSelectedIdentity(){
		return selected_identity;
	}
	private void backtrack(int level){
		switch(level) {
		case B_IDENTITY:
			if(DEBUG) System.out.println("GUIStatus: backtrack: Identity");
			selected_identity = null;
			myself_peer = null;
			selected_peer = null;
		case B_ORGANIZATION:
			if(DEBUG) System.out.println("GUIStatus: backtrack: ORG");
			selected_organization = null;
			selected_neighborhood = null;
			selected_constituent = null;
			myself_constituent = null;
		case B_MOTION:
			if(DEBUG) System.out.println("GUIStatus: backtrack: MOTION");
			//selected_news = null;
			selected_justification = null;
			selected_motion = null;
		}
	}
	
}
