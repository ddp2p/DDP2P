package net.ddp2p.common.util;
import static net.ddp2p.common.util.Util.__;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.PK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_News;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Translations;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.streaming.WB_Messages;
class SK_SaverThread extends net.ddp2p.common.util.DDP2P_ServiceThread {
	private static final boolean DEBUG = false;
	DD_SK ds;
	SK_SaverThread(DD_SK ds) {
		super("SK_Saver", false, ds);
		this.ds = ds;
	}
	public void _run() {
		try {
			__run();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void __run() throws P2PDDSQLException{
    	for (DD_SK_Entry d : ds.sk) {
    		try {
	    		String params[] = new String[net.ddp2p.common.table.key.fields.length];
	    		PK pk = d.key.getPK();
	    		params[net.ddp2p.common.table.key.COL_SK] = Util.stringSignatureFromByte(d.key.encode());
	    		params[net.ddp2p.common.table.key.COL_NAME] = d.name;
	    		params[net.ddp2p.common.table.key.COL_TYPE] = d.type;
	    		params[net.ddp2p.common.table.key.COL_CREATION_DATE] = Encoder.getGeneralizedTime(d.creation);
	       		params[net.ddp2p.common.table.key.COL_PK] = Util.stringSignatureFromByte(pk.encode());
	       		params[net.ddp2p.common.table.key.COL_IDHASH] = Util.getGIDhash((String)params[net.ddp2p.common.table.key.COL_PK]);
	       		Application.getDB().insert(net.ddp2p.common.table.key.TNAME, net.ddp2p.common.table.key.fields, params, DEBUG);
    		} catch (Exception e) {
    			String localized = "";
    			if (e.getLocalizedMessage() != null) localized = "\n"+__("Error:")+e.getLocalizedMessage();
    			Application_GUI.warning(__("Error saving key:")+d+localized+"\nError:"+e, __("Error saving key"));
    			e.printStackTrace();
    		}
     	}
    	WB_Messages wm = new WB_Messages();
    	wm.peers.addAll(ds.peer);
    	if (ds.sender != null) wm.peers.add(0, ds.sender);
    	wm.orgs.addAll(ds.org);
    	wm.neig.addAll(ds.neigh);
    	wm.cons.addAll(ds.constit);
    	wm.witn.addAll(ds.witn);
    	wm.moti.addAll(ds.moti);
       	wm.just.addAll(ds.just);
      	wm.sign.addAll(ds.vote);
      	wm.news.addAll(ds.news);
      	wm.tran.addAll(ds.tran);
		Hashtable<String, RequestData> missing_sr = new Hashtable<String, RequestData>();
		Hashtable<String, RequestData> obtained_sr = new Hashtable<String, RequestData>();
		HashSet<String> orgs = new HashSet<String>();
		String dbg_msg = __("Importing manually!");
		D_Peer myself = ds.sender; 
		WB_Messages.store(null, myself, wm, missing_sr, obtained_sr, orgs, dbg_msg);
	}
}
public class DD_SK extends ASNObj implements StegoStructure {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	public final int V0 = 0;
	int version = V0;
	public D_Peer sender;
	public ArrayList<DD_SK_Entry> sk = new ArrayList<DD_SK_Entry>();
	public ArrayList<D_Peer> peer = new ArrayList<D_Peer>();
	public ArrayList<D_Organization> org = new ArrayList<D_Organization>();
	public ArrayList<D_Neighborhood> neigh = new ArrayList<D_Neighborhood>();
	public ArrayList<D_Constituent> constit = new ArrayList<D_Constituent>();
	public ArrayList<D_Witness> witn = new ArrayList<D_Witness>();
	public ArrayList<D_Motion> moti = new ArrayList<D_Motion>();
	public ArrayList<D_Justification> just = new ArrayList<D_Justification>();
	public ArrayList<D_Vote> vote = new ArrayList<D_Vote>();
	public ArrayList<D_News> news = new ArrayList<D_News>();
	public ArrayList<D_Translations> tran = new ArrayList<D_Translations>();
	private byte[] signature;
	public boolean empty() {
		return sk.size() == 0 
				&& peer.size() == 0
				&& org.size() == 0
				&& neigh.size() == 0
				&& constit.size() == 0
				&& witn.size() == 0
				&& moti.size() == 0
				&& just.size() == 0
				&& vote.size() == 0
				&& news.size() == 0
				&& tran.size() == 0
				&& sender == null;
	}
	public String toString() {
		String result = "";
		result += "SK[: sk="+Util.concat(sk, ";;;", "NULL");
		if (peer.size() > 0) result += " peers="+Util.concat(peer, ",,,", "NULL");
		if (org.size() > 0) result += " org="+Util.concat(org, ",,,", "NULL");
		if (constit.size() > 0) result += " constit="+Util.concat(constit, ",,,", "NULL");
		if (witn.size() > 0) result += " witn="+Util.concat(witn, ",,,", "NULL");
		if (moti.size() > 0) result += " moti="+Util.concat(moti, ",,,", "NULL");
		if (just.size() > 0) result += " just="+Util.concat(just, ",,,", "NULL");
		if (vote.size() > 0) result += " vote="+Util.concat(vote, ",,,", "NULL");
		if (news.size() > 0) result += " news="+Util.concat(news, ",,,", "NULL");
		if (tran.size() > 0) result += " tran="+Util.concat(tran, ",,,", "NULL");
		result += "]";
		return result;
	}
	@Override
	public void save() throws P2PDDSQLException {
		if (DEBUG) System.out.println("DD_SK: save");
		new SK_SaverThread(this).start();
    	if (true) {
    		Application_GUI.warning(__("Work Launched to Add Objects:")+" \n"+this.getNiceDescription(), __("Import DD_SK"));
    	}
	}
	@Override
	public void saveSync() throws P2PDDSQLException {
		if (DEBUG) System.out.println("DD_SK: save");
		new SK_SaverThread(this).run();
    	if (false) {
    		Application_GUI.warning(__("Work Launched to Add Objects:")+" \n"+this.getNiceDescription(), __("Import DD_SK"));
    	}
	}
	@Override
	public void setBytes(byte[] asn1) throws ASN1DecoderFail {
		if (DEBUG) System.out.println("DD_SK: setBytes "+asn1.length);
		decode(new Decoder(asn1));
		if (this.empty()) throw new net.ddp2p.ASN1.ASNLenRuntimeException("Empty object!");
		if (DEBUG) System.out.println("DD_SK: setBytes");
	}
	@Override
	public byte[] getBytes() {
		return encode();
	}
	@Override
	public String getNiceDescription() {
		String result = "";
		for (DD_SK_Entry _sk : sk)
			result += "SK[: sk="+_sk.name+"]";//Util.concat(sk, ";;;", "NULL");
		for (D_Peer _peer : peer)
			result += " Safe=["+_peer.getName()+"]";
		for (D_Organization _org : org)
			result += " Org=["+_org.getName()+"]";
		for (D_Constituent _constit : constit)
			result += " Cons=["+_constit.getNameFull()+"]";
		for (D_Witness _witn : witn)
			result += " Witn=["+_witn.witnessed_global_constituentID+"]";
		for (D_Motion _moti : moti)
			result += " Moti=["+_moti.getTitleStrOrMy()+"]";
		for (D_Justification _just : just)
			result += " Just=["+_just.getTitleStrOrMy()+"]";
		for (D_News _news : news)
			result += " News=["+_news.getTitleStrOrMy()+"]";
		for (D_Vote _vote : vote)
			result += " Vote=["+_vote.getChoice()+" "+_vote.getMotionGID()+"]";
		if (tran.size() > 0) result += " tran="+Util.concat(tran, ",,,", "NULL");
		result += "]";
		return result;
	}
	@Override
	public String getString() {
		String result = "";
		result += "SK[: sk="+Util.concat(sk, ";;;", "NULL");
		if (peer.size() > 0) result += " peers="+Util.concat(peer, ",,,", "NULL");
		if (org.size() > 0) result += " org="+Util.concat(org, ",,,", "NULL");
		if (constit.size() > 0) result += " constit="+Util.concat(constit, ",,,", "NULL");
		if (witn.size() > 0) result += " witn="+Util.concat(witn, ",,,", "NULL");
		if (moti.size() > 0) result += " moti="+Util.concat(moti, ",,,", "NULL");
		if (just.size() > 0) result += " just="+Util.concat(just, ",,,", "NULL");
		if (vote.size() > 0) result += " vote="+Util.concat(vote, ",,,", "NULL");
		if (news.size() > 0) result += " news="+Util.concat(news, ",,,", "NULL");
		if (tran.size() > 0) result += " tran="+Util.concat(tran, ",,,", "NULL");
		result += "]";
		return result;
	}
	@Override
	public boolean parseAddress(String content) {
		return false;
	}
	@Override
	public short getSignShort() {
		if(DEBUG) System.out.println("DD_SK: get_sign=:"+DD.STEGO_SK);
		return DD.STEGO_SK;
	}
	public static BigInteger getASN1Tag() {
		return new BigInteger(DD.getPositive(DD.STEGO_SK)+"");
	}
	/**
	 * sender has its own signature, so needs not be signed. Its PK has to match anyhow.
	 * @return
	 */
	public Encoder getSignEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version).setASN1Type(DD.TAG_AP0));
		if (sk.size() > 0) enc.addToSequence(Encoder.getEncoder(sk).setASN1Type(DD.TAG_AC0));
		if (peer.size() > 0) enc.addToSequence(Encoder.getEncoder(peer).setASN1Type(DD.TAG_AC1));
		if (org.size() > 0) enc.addToSequence(Encoder.getEncoder(org).setASN1Type(DD.TAG_AC2));
		if (neigh.size() > 0) enc.addToSequence(Encoder.getEncoder(neigh).setASN1Type(DD.TAG_AC3));
		if (constit.size() > 0) enc.addToSequence(Encoder.getEncoder(constit).setASN1Type(DD.TAG_AC4));
		if (witn.size() > 0) enc.addToSequence(Encoder.getEncoder(witn).setASN1Type(DD.TAG_AC5));
		if (moti.size() > 0) enc.addToSequence(Encoder.getEncoder(moti).setASN1Type(DD.TAG_AC6));
		if (just.size() > 0) enc.addToSequence(Encoder.getEncoder(just).setASN1Type(DD.TAG_AC7));
		if (vote.size() > 0) enc.addToSequence(Encoder.getEncoder(vote).setASN1Type(DD.TAG_AC8));
		if (news.size() > 0) enc.addToSequence(Encoder.getEncoder(news).setASN1Type(DD.TAG_AC9));
		if (tran.size() > 0) enc.addToSequence(Encoder.getEncoder(tran).setASN1Type(DD.TAG_AC10));
		return enc;
}
	@Override
	public Encoder getEncoder() {
		Encoder enc = getSignEncoder();
		if (sender != null) enc.addToSequence(sender.getEncoder().setASN1Type(DD.TAG_AP1));
		if (signature != null) enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AP2));	
		enc.setASN1Type(Encoder.CLASS_APPLICATION, Encoder.PC_CONSTRUCTED, getASN1Tag());
		return enc;
	}
	@Override
	public DD_SK decode(Decoder dec) throws ASN1DecoderFail {
		String default_orgGID = null;
		String default_orgGIDH = null;
		Decoder d = dec.getContent();
		if (d.getTypeByte() == DD.TAG_AP0) version = d.getFirstObject(true).getInteger(DD.TAG_AP0).intValue();
		if (d.getTypeByte() == DD.TAG_AC0)
			sk =  d.getFirstObject(true).getSequenceOfAL(DD_SK_Entry.getASN1Tag(), new DD_SK_Entry());
		if (d.getTypeByte() == DD.TAG_AC1)
			peer =  d.getFirstObject(true).getSequenceOfAL(D_Peer.getASN1Type(), D_Peer.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC2) {
			org =  d.getFirstObject(true).getSequenceOfAL(D_Organization.getASN1Type(), D_Organization.getEmpty());
			if (org.size() == 1) {
				default_orgGID = org.get(0).getGID(); 
				default_orgGIDH = org.get(0).getGIDH();
				if (DEBUG) System.out.println("DD_SK: has a GID for orgs: " + default_orgGID+" H="+default_orgGIDH);
			}
		}
		if (d.getTypeByte() == DD.TAG_AC3)
			neigh =  d.getFirstObject(true).getSequenceOfAL(D_Neighborhood.getASN1Type(), D_Neighborhood.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC4)
			constit =  d.getFirstObject(true).getSequenceOfAL(D_Constituent.getASN1Type(), D_Constituent.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC5)
			witn =  d.getFirstObject(true).getSequenceOfAL(D_Witness.getASN1Type(), D_Witness.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC6) {
			moti =  d.getFirstObject(true).getSequenceOfAL(D_Motion.getASN1Type(), D_Motion.getEmpty());
			for (D_Motion m : moti) {
				if (m.getOrganizationGIDH() == null) {
					if (DEBUG) System.out.println("DD_SK: put a default orgGIDH for "+m);
					m.setOrganizationGID(default_orgGIDH);
					if (DEBUG) System.out.println("DD_SK: did put a default orgGIDH for "+m);
				} else
					if (DEBUG) System.out.println("DD_SK: had a GIDH for "+m);
			}
		}
		if (d.getTypeByte() == DD.TAG_AC7)
			just =  d.getFirstObject(true).getSequenceOfAL(D_Justification.getASN1Type(), D_Justification.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC8)
			vote =  d.getFirstObject(true).getSequenceOfAL(D_Vote.getASN1Type(), D_Vote.getEmpty());
		if (d.getTypeByte() == DD.TAG_AC10)
			tran =  d.getFirstObject(true).getSequenceOfAL(D_Translations.getASN1Type(), D_Translations.getEmpty());
		if (d.getTypeByte() == DD.TAG_AP1) sender = D_Peer.getEmpty().decode(d.getFirstObject(true));
		if (d.getTypeByte() == DD.TAG_AP2) signature = d.getFirstObject(true).getBytes(DD.TAG_AP2);
		return this;
	}
	/**
	 * Sets the sender and computes the signature
	 * @param me
	 */
	public void sign_and_set_sender(D_Peer me) {
		if (me == null) {
			sender = null;
			signature = null;
			return;
		}
		byte[] msg = this.getSignEncoder().getBytes();
		signature = Util.sign(msg, me.getSK());
		this.sender = me;
	}
	/**
	 * Verifies the signature.
	 * Always false if there is no sender.
	 * @return
	 */
	public boolean verify() {
		if (sender == null) return false;
		byte[] msg = this.getSignEncoder().getBytes();
		return Util.verifySign(msg, sender.getPK(), signature);
	}
	public static void addOrganizationToDSSK(DD_SK d_SK, D_Organization crt_org) {
		if (crt_org == null) return;
		for (D_Organization c : d_SK.org) {
			if (c == crt_org) return;
			if (c.getLID() == crt_org.getLID()) return;
		}
		d_SK.org.add(crt_org);
	}
	public static void addNeighborhoodToDSSK(DD_SK d_SK, D_Neighborhood neighborhood) {
		if (neighborhood == null) return;
		for (D_Neighborhood n : d_SK.neigh) {
			if (n == neighborhood) return;
			if (n.getLID() == neighborhood.getLID()) return;
		}
		d_SK.neigh.add(neighborhood);
	}
	public static void addConstituentToDSSK(DD_SK d_SK, D_Constituent constituent) {
		if (constituent == null) return;
		for (D_Constituent c : d_SK.constit) {
			if (c == constituent) return;
			if (c.getLID() == constituent.getLID()) return;
		}
		d_SK.constit.add(constituent);
		addOrganizationToDSSK(d_SK, constituent.getOrganization());
		constituent.loadNeighborhoods(D_Constituent.EXPAND_ALL);
		D_Neighborhood[] n = constituent.getNeighborhood();
		if (n != null) {
			for (D_Neighborhood _n : n) {
				addNeighborhoodToDSSK(d_SK, _n);
			}
		}
	}
	/**
	 * This does not add a vote
	 * @param d_SK
	 * @param crt_justification
	 */
	public static void addJustificationToDSSK(DD_SK d_SK, D_Justification crt_justification) {
		if (crt_justification != null) {
			String jGID = crt_justification.getGID();
			if (jGID == null) return;
			for (D_Justification old_j : d_SK.just) {
				if (Util.equalStrings_and_not_null(old_j.getGID(), jGID)) return;
			}
			d_SK.just.add(crt_justification);
			addConstituentToDSSK(d_SK, crt_justification.getConstituentForce());
			addMotionToDSSK(d_SK, crt_justification.getMotionForce());
		}
	}
	/**
	 * 
	 * @param d_SK
	 * @param crt_justification
	 * @param voting_constituent
	 * @param alreadySupported
	 */
	public static void addJustificationWithAnySupportToDSSK(DD_SK d_SK, D_Justification crt_justification, D_Constituent voting_constituent, boolean alreadySupported) {
		addJustificationToDSSK(d_SK, crt_justification);
		if (alreadySupported) return;
		D_Motion crt_motion = crt_justification.getMotionForce();
		D_Vote my_vote = D_Vote.getOneBroadcastedSupportForJustification(crt_motion, crt_justification, voting_constituent, crt_motion.getSupportChoice());
		if (my_vote != null) {
			addVoteIfNew(d_SK, my_vote);
			addConstituentToDSSK(d_SK, my_vote.getConstituent_force());
		}		
	}
	public static boolean addMotionToDSSK(DD_SK d_SK, D_Motion crt_motion) {
		if (crt_motion == null) return false;
		String mGID = crt_motion.getGID();
		if (mGID == null) return false;
		for (D_Motion old_m : d_SK.moti) {
			if (Util.equalStrings_and_not_null(old_m.getGID(), mGID)) return false;
		}
		D_Organization org = crt_motion.getOrganization();
		if (org == null) return false;
		d_SK.moti.add(crt_motion);
		D_Constituent constituent = crt_motion.getConstituent();
		addConstituentToDSSK(d_SK, constituent);
		addOrganizationToDSSK(d_SK, org);
		D_Constituent crt_constituent = DD.getCrtConstituent(crt_motion.getOrganizationLID());
		long _constituentID_myself = -1;
		if (crt_constituent != null) {
			_constituentID_myself = crt_constituent.getLID();
		}
		D_Vote my_vote = null;
		if (_constituentID_myself > 0) {
			try {
				my_vote = D_Vote.getOpinionForMotion(crt_motion.getLIDstr(), _constituentID_myself);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		if (my_vote == null) {
			my_vote = D_Vote.getOneBroadcastedSupportForMotion(crt_motion, null, crt_motion.getSupportChoice());
		}
		if (my_vote != null) {
			addVoteIfNew(d_SK, my_vote);
			addConstituentToDSSK(d_SK, my_vote.getConstituent_force());
			D_Justification j = my_vote.getJustificationFromObjOrLID();
			addJustificationToDSSK(d_SK, j);
		}
		return true;
	}
	private static void addVoteIfNew(DD_SK d_SK, D_Vote my_vote) {
		String vGID = my_vote.getGID();
		if (vGID == null) return;
		for (D_Vote old_v : d_SK.vote) {
			if (Util.equalStrings_and_not_null(old_v.getGID(), vGID)) return;
		}
		d_SK.vote.add(my_vote);
	}
}
