package net.ddp2p.common.handling_wb;
import java.util.ArrayList;
import java.util.Calendar;
import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Message;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_OrgConcepts;
import net.ddp2p.common.data.D_OrgParams;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.data.HandlingMyself_Peer;
import net.ddp2p.common.simulator.WirelessLog;
import net.ddp2p.common.streaming.UpdatePeersTable;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
public class BroadcastQueueRecent extends BroadcastQueue {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static int PEERS_SIZE = 10;
	public static int ORGS_SIZE = 10;
	public static int VOTES_SIZE = 1000;
	public static int CONSTITUENTS_SIZE = 10;
	public static int WITNESSES_SIZE = 10;
	public static int NEIGHS_SIZE = 6;
	private String my_peer_ID;
	public long delay = 24*60*60*1000; // milliseconds
	Calendar getDelay() {
		Calendar now = Util.CalendargetInstance();
		now.setTimeInMillis(now.getTimeInMillis() - delay);
		return now;
	}
	String getDelayString() {
		return Encoder.getGeneralizedTime(getDelay());
	}
	void load(){}
	public BroadcastQueueRecent() throws P2PDDSQLException {
		if(DEBUG)System.out.println("BroadcastQueueRecent() : Constructor ");
		loadAll();
	}
	void setMyPeerID(String _my_peer_ID){
		my_peer_ID = _my_peer_ID;
	}
	@Override
	public void loadAll() {
		if(DEBUG)System.out.println("BroadcastQueueRecent() :  loadAll() ");
				this.m_PreparedMessagesVotes = loadVotes();
				this.m_iCrtVotes = 0;
				this.m_PreparedMessagesConstituents = loadConstituents();
				this.m_iCrtConstituents = 0;
				this.m_PreparedMessagesPeers = loadPeers();
				this.m_iCrtPeers = 0;
				this.m_PreparedMessagesOrgs = loadOrgs();
				this.m_iCrtOrgs = 0;
				this.m_PreparedMessagesWitnesses = loadWitnesses();
				this.m_iCrtWitnesses = 0;
				this.m_PreparedMessagesNeighborhoods = loadNeighborhoods();
				this.m_iCrtNeighborhoods = 0;
	}
	/**
	 * load the orgs to the ArrayList<byte[]>
	 * @param org_msgs
	 * @param rowID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public long loadOrgs(ArrayList<PreparedMessage> org_msgs, long rowID){
		if(DEBUG)System.out.println("BroadcastQueueRecent() : loadOrgs() : START");
		try{
			return _loadOrgs(org_msgs,rowID);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return -1;
		}
	}
	static String sql_orgs = "SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.organization.field_list,"o")+
			" FROM "+net.ddp2p.common.table.organization.TNAME+" AS o "+
			" LEFT JOIN "+net.ddp2p.common.table.peer.TNAME+" AS p ON(p."+net.ddp2p.common.table.peer.peer_ID+"=o."+net.ddp2p.common.table.organization.creator_ID+")"+
			" LEFT JOIN "+net.ddp2p.common.table.key.TNAME+" AS k ON(k."+net.ddp2p.common.table.key.public_key+"=p."+net.ddp2p.common.table.peer.global_peer_ID+")"+
			" WHERE" +
			" k."+net.ddp2p.common.table.key.secret_key+" IS NOT NULL AND ";
	public long _loadOrgs(ArrayList<PreparedMessage> org_msgs, long rowID) throws P2PDDSQLException {
		long start_row = rowID;
		boolean wrapped = false;
		long last_row = -1;
		ArrayList<ArrayList<Object>> orgs;
		int retrieved = 0;
		org_msgs.clear();
		for(;;){
			String sql = 
					sql_orgs+
					" o."+net.ddp2p.common.table.organization.arrival_date+" > ?  AND "+
					" o.ROWID > "+rowID+
					(wrapped?(" AND o.ROWID <= "+start_row):"")+
					" AND o."+net.ddp2p.common.table.organization.signature+ " IS NOT NULL "+
					" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' " +
					" ORDER BY o.ROWID "+//table.organization.organization_ID+
					" LIMIT "+(ORGS_SIZE - retrieved);
			if(DEBUG)System.out.println("Prepared : messages : "+sql);
			if(rowID==-1) rowID=1;
			if(DEBUG)System.out.println("ROWID: "+rowID);
			try{
				orgs = Application.getDB().select(sql, new String[]{this.getDelayString()});
				for(ArrayList<Object> org : orgs){
					last_row = Integer.parseInt(org.get(net.ddp2p.common.table.organization.ORG_COL_ID).toString());
					byte[]msg = buildOrganizationMessage(org, ""+rowID);
					if(DEBUG)System.out.println("MSG : "+msg);
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					org_msgs.add(pm);
					if(org_msgs.size()>=0)
						WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Recent_queue,
								""+(org_msgs.size()-1),WirelessLog.org_type,msg);
				}
			}catch(Exception e){
				if(_DEBUG)System.out.println("BroadcastQueueRecent() : loadOrgs: EXCEPTION");
				e.printStackTrace();
				return -1;
			}
			retrieved = org_msgs.size();
			if(wrapped) {
				if(DEBUG)System.out.println("BroadcastQueueRecent() : loadOrgs() : "+org_msgs.size()+" loaded");
				break;
			}
			if((orgs.size() < ORGS_SIZE)&&(rowID != -1)) {
				rowID = -1;
				wrapped = true;
				continue;
			}
			if(DEBUG)System.out.println("BroadcastQueueRecent() : loadOrgs() : "+org_msgs.size()+" loaded");
			break;
		}
		return last_row;
	}
	/**
	 * load the Witnesses to the ArrayList<byte[]>
	 * @param Witnesses_msgs
	 * @param rowID
	 * @return
	 */
	public long loadWitnesses(ArrayList<PreparedMessage> Witnesses_msgs,long rowID) {
		if(DEBUG)System.out.println("BroadcastQueueRecent() : loadWitnesses() : START");
		try {
			return _loadWitnesses(Witnesses_msgs, rowID);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return -1;
		}
	}
	static String sql_witnesses = 
			"SELECT "
					+ Util.setDatabaseAlias(net.ddp2p.common.table.witness.witness_fields, "w")+" "
					+", n."+net.ddp2p.common.table.neighborhood.global_neighborhood_ID
					+", t."+net.ddp2p.common.table.constituent.global_constituent_ID
					+", s."+net.ddp2p.common.table.constituent.global_constituent_ID
					+", o."+net.ddp2p.common.table.organization.global_organization_ID
					+", o."+net.ddp2p.common.table.organization.organization_ID
					+" FROM "+net.ddp2p.common.table.witness.TNAME+" AS w "+
					" LEFT JOIN "+net.ddp2p.common.table.neighborhood.TNAME+" AS n ON(n."+net.ddp2p.common.table.neighborhood.neighborhood_ID+"=w."+net.ddp2p.common.table.witness.neighborhood_ID+") "+
					" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS s ON(s."+net.ddp2p.common.table.constituent.constituent_ID+"=w."+net.ddp2p.common.table.witness.source_ID+") "+
					" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS t ON(t."+net.ddp2p.common.table.constituent.constituent_ID+"=w."+net.ddp2p.common.table.witness.target_ID+") "+
					" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(o."+net.ddp2p.common.table.organization.organization_ID+"=s."+net.ddp2p.common.table.constituent.organization_ID+") "+
					" LEFT JOIN "+net.ddp2p.common.table.key.TNAME+" AS k ON(k."+net.ddp2p.common.table.key.public_key+"=s."+net.ddp2p.common.table.constituent.global_constituent_ID+")"+
					" WHERE" +
					" k."+net.ddp2p.common.table.key.secret_key+" IS NOT NULL AND "
					;
	public long _loadWitnesses(ArrayList<PreparedMessage> Witnesses_msgs,long rowID) throws P2PDDSQLException {
		long start_row = rowID;
		boolean wrapped = false;
		long last_row = -1;
		ArrayList<ArrayList<Object>> witnesses;
		int retrieved = 0;
		Witnesses_msgs.clear();
		for(;;) {
			String sql =
					sql_witnesses+
					" w."+net.ddp2p.common.table.witness.arrival_date+" > ?  AND "+
					" w.ROWID > "+rowID+
					(wrapped?(" AND w.ROWID <= "+start_row):"")+
					" AND w."+net.ddp2p.common.table.witness.signature +" IS NOT NULL "+
					" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' " +
					" AND s."+net.ddp2p.common.table.constituent.broadcasted+" <> '0' " +
					" ORDER BY w.ROWID "+
					" LIMIT "+(WITNESSES_SIZE - retrieved);
			if(DEBUG)System.out.println("_loadWitnesses : sql "+sql);
			witnesses = Application.getDB().select(sql, new String[]{this.getDelayString()});
			for(ArrayList<Object> witness : witnesses){
				D_Witness wbw = new D_Witness();
				D_Message asn1=new D_Message();
				asn1.sender = D_Peer.getEmpty(); 
				asn1.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID();
				last_row = Integer.parseInt(witness.get(net.ddp2p.common.table.witness.WIT_COL_ID).toString());
				try{
					wbw.init_all(witness);
				}catch(Exception e){e.printStackTrace();}
				asn1.witness = wbw;
				if(DEBUG)System.out.println("Wintnessed_constituent INFO : "+asn1.witness.witnessed);
				if(DEBUG)System.out.println("Wintnessing_constituent INFO : "+asn1.witness.witnessing);
				if(DEBUG)System.out.println("Witness_ID : "+asn1.witness.witnessID);
				String sql1 = " SELECT "+net.ddp2p.common.table.constituent.organization_ID+
						" FROM "+net.ddp2p.common.table.constituent.TNAME+" WHERE "+
						net.ddp2p.common.table.constituent.constituent_ID+"=?;";
				ArrayList<ArrayList<Object>> org_id = Application.getDB().select(sql1, new String[]{""+wbw.witnessed_constituentID});
				String OrgID = Util.getString(org_id.get(0).get(0));	
				if(DEBUG)System.out.println("DATA : "+OrgID);
				ArrayList<Object> org_data = get_org_db_by_local(OrgID);
				asn1.organization = get_ASN_Organization_by_OrgID(org_data,OrgID);
				Encoder enc = asn1.getEncoder();	
				byte msg [] = enc.getBytes();
				if(DEBUG)System.out.println("MSG : "+msg);
				PreparedMessage pm = new PreparedMessage();
				pm.raw = msg;
				if(wbw.witnessing!=null){
				}
				if(wbw.witnessed!=null){
				}
				Witnesses_msgs.add(pm);
				if(Witnesses_msgs.size()>=0)
					WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Recent_queue,
							""+(Witnesses_msgs.size()-1),WirelessLog.wit_type,msg);
			}
			retrieved = Witnesses_msgs.size();
			if(wrapped) {
				if(DEBUG)System.out.println("BroadcastQueueRecent() : loadWitnesses() : "+Witnesses_msgs.size()+" loaded");
				break;				
			}
			if((witnesses.size() < WITNESSES_SIZE)&&(rowID != -1)) {
				rowID = -1;
				wrapped = true;
				continue;
			}
			if(DEBUG)System.out.println("BroadcastQueueRecent() : loadWitnesses() : "+Witnesses_msgs.size()+" loaded");
			break;
		}
		return last_row;
	}
	/**
	 * load the Constituents to the ArrayList<byte[]>
	 * @param constituent_msgs
	 * @param rowID
	 * @return
	 */
	public long loadConstituents(ArrayList<PreparedMessage> constituent_msgs, long rowID) {
		if(DEBUG)System.out.println("BroadcastQueueRecent() : loadConstituents() : START");
		return _loadConstituents(constituent_msgs,rowID);			
	}
	static String c_fields_constituents = Util.setDatabaseAlias(net.ddp2p.common.table.constituent.fields_constituents,"c");
	static String sql_cons = 
			"SELECT "+
					c_fields_constituents+
					",n."+net.ddp2p.common.table.neighborhood.global_neighborhood_ID+
					" FROM "+net.ddp2p.common.table.constituent.TNAME+" as c " +
					" LEFT JOIN "+net.ddp2p.common.table.neighborhood.TNAME+" AS n ON(c."+net.ddp2p.common.table.constituent.neighborhood_ID+" = n."+net.ddp2p.common.table.neighborhood.neighborhood_ID+") "+
					" LEFT JOIN "+net.ddp2p.common.table.key.TNAME+" AS k ON(k."+net.ddp2p.common.table.key.public_key+"=c."+net.ddp2p.common.table.constituent.global_constituent_ID+")"+
					" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(c."+net.ddp2p.common.table.constituent.organization_ID+" = o."+net.ddp2p.common.table.organization.organization_ID+") "+
					" WHERE "+ 
					" k."+net.ddp2p.common.table.key.secret_key+" IS NOT NULL AND";
	@SuppressWarnings("static-access")
	public long _loadConstituents(ArrayList<PreparedMessage> constituent_msgs, long rowID) {
		long start_row = rowID;
		boolean wrapped = false;
		long last_row = -1;
		ArrayList<ArrayList<Object>> constituents;
		int retrieved = 0;
		constituent_msgs.clear();
		for(;;){	
			String sql_get_const =
					sql_cons+
					" c."+net.ddp2p.common.table.constituent.arrival_date+" > ?  AND "+	
					" c.ROWID > "+rowID+
					(wrapped?(" AND c.ROWID <= "+start_row):"")+
					" AND c."+net.ddp2p.common.table.constituent.sign+" IS NOT NULL"+
					" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' "+
					" AND c."+net.ddp2p.common.table.constituent.broadcasted+" <> '0' "+
					" ORDER BY c.ROWID "+
					" LIMIT "+(CONSTITUENTS_SIZE - retrieved);
			if(DEBUG)System.out.println(sql_get_const);
			try{
				constituents = Application.getDB().select(sql_get_const, new String[]{this.getDelayString()});
				for(ArrayList<Object> constituent : constituents) {
					Long LID = Util.Lval(constituent.get(net.ddp2p.common.table.constituent.CONST_COL_ID));
					D_Constituent con = D_Constituent.getConstByLID(LID, true, false);
					D_Message asn1=new D_Message();
					asn1.sender = D_Peer.getEmpty(); 
					asn1.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID();
					last_row = Integer.parseInt(constituent.get(net.ddp2p.common.table.constituent.CONST_COL_ID).toString());
					con.loadNeighborhoods(D_Constituent.EXPAND_ALL);
					asn1.constituent = con;
					String org_id = con.getLIDstr();
					ArrayList<Object> org_data = get_org_db_by_local(org_id);
					asn1.organization = get_ASN_Organization_by_OrgID(org_data,org_id);
					Encoder enc = asn1.getEncoder();	
					byte msg [] = enc.getBytes();
					if(DEBUG)System.out.println("MSG : "+msg);
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					if(con.getNeighborhood()!=null){
						for(int i=0; i<con.getNeighborhood().length; i++) 
						{
						}
					}
					constituent_msgs.add(pm);
					if(constituent_msgs.size()>=0)
						WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Recent_queue,
								""+(constituent_msgs.size()-1),WirelessLog.const_type,msg);
				}
			}catch(Exception e){
				if(_DEBUG)System.out.println("BroadcastQueueRecent() : loadConstituent: EXCEPTION");
				e.printStackTrace();
				return -1;
			}
			retrieved = constituent_msgs.size();
			if(wrapped) {
				if(DEBUG)System.out.println("BroadcastQueueRecent() : loadConstituents() : "
						+constituent_msgs.size()+" loaded");
				break;	
			}
			if((constituents.size() < CONSTITUENTS_SIZE)&&(rowID != -1)){
				rowID = -1;
				wrapped = true;
				continue;
			}
			if(DEBUG)System.out.println("BroadcastQueueRecent() : loadConstituents() : "
					+constituent_msgs.size()+" loaded");
			break;
		}
		return last_row;
	}
	/**
	 * load the Peers to the ArrayList<byte[]>
	 * @param peers_msgs
	 * @param rowID
	 * @return
	 */
	public long loadPeers(ArrayList<PreparedMessage> peers_msgs, long rowID){
		if(DEBUG)System.out.println("BroadcastQueueRecent() : loadPeers() : START");
		return _loadPeers(peers_msgs, rowID);
	}
	static String sql_peers = "SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.peer.fields_peers,"p")
			+" FROM "+net.ddp2p.common.table.peer.TNAME+" AS p "
			+" LEFT JOIN "+net.ddp2p.common.table.key.TNAME
			+" AS k ON(k."+net.ddp2p.common.table.key.public_key
			+"=p."+net.ddp2p.common.table.peer.global_peer_ID+")"
			+" WHERE" +" k."+net.ddp2p.common.table.key.secret_key+" IS NOT NULL AND";
	public long _loadPeers(ArrayList<PreparedMessage> peers_msgs, long rowID) {
		long start_row = rowID;
		boolean wrapped = false;
		long last_row = -1;
		ArrayList<ArrayList<Object>> peers;
		int retrieved = 0;
		peers_msgs.clear();
		for(;;){
			String sql = 
					sql_peers
					+" p."+net.ddp2p.common.table.peer.arrival_date+" > ?  AND "
					+" p.ROWID > "+rowID
					+(wrapped?(" AND p.ROWID <= "+start_row):"")
					+" ORDER BY p.ROWID "
					+" LIMIT "+(PEERS_SIZE - retrieved);
			if(DEBUG)System.out.println(sql);
			try{
				peers = Application.getDB().select(sql, new String[]{this.getDelayString()});
				if(DEBUG)System.out.println("this time we took : "+peers.size());
				for(ArrayList<Object> peer : peers){
					last_row = Integer.parseInt(peer.get(net.ddp2p.common.table.peer.PEER_COL_ID).toString());
					byte[]msg = buildPeerMessage(peer, rowID);
					if(DEBUG)System.out.println("MSG : "+msg);
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					peers_msgs.add(pm);
					if(peers_msgs.size()>=0)
						WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Recent_queue,
								""+(peers_msgs.size()-1),WirelessLog.peer_type,msg);
				}
			}catch(P2PDDSQLException e){
				if(_DEBUG)System.out.println("BroadcastQueueRecent() : loadPeers: EXCEPTION");
				e.printStackTrace();
				return -1;
			} catch (ASN1DecoderFail e) {
				e.printStackTrace();
				return -1;
			}
			retrieved = peers_msgs.size();
			if(wrapped){
				if(DEBUG)System.out.println("BroadcastQueueRecent() : loadPeers() : "
						+peers_msgs.size()+" loaded");
				break;
			}
			if((peers.size() < PEERS_SIZE)&&(rowID != -1)) {
				rowID = -1;
				wrapped = true;
				continue;
			}
			if(DEBUG)System.out.println("BroadcastQueueRecent() : loadPeers() : "
					+peers_msgs.size()+" loaded");
			break;
		}
		return last_row;
	}
	/**
	 * loadVotes
	 * @param vote_msgs
	 * @param rowID
	 * @return last vote index
	 */
	public long loadVotes(ArrayList<PreparedMessage> vote_msgs, long rowID) {
		if(DEBUG)System.out.println("BroadcastQueueRecent() : loadVotes() : START");
		return _loadVotes(vote_msgs, rowID);
	}
	static String sql_votes =
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.signature.fields,"v")
			+" FROM "+net.ddp2p.common.table.signature.TNAME+" AS v "
			+" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON(c."+net.ddp2p.common.table.constituent.constituent_ID+"=v."+net.ddp2p.common.table.signature.constituent_ID+")"
			+" LEFT JOIN "+net.ddp2p.common.table.motion.TNAME+" AS m ON(m."+net.ddp2p.common.table.motion.motion_ID+"=v."+net.ddp2p.common.table.signature.motion_ID+")"
			+" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(o."+net.ddp2p.common.table.organization.organization_ID+"=m."+net.ddp2p.common.table.motion.organization_ID+")"
			+" LEFT JOIN "+net.ddp2p.common.table.key.TNAME+" AS k ON(k."+net.ddp2p.common.table.key.public_key+"=c."+net.ddp2p.common.table.constituent.global_constituent_ID+")"
			+" WHERE" 
			+" k."+net.ddp2p.common.table.key.secret_key+" IS NOT NULL AND";
	public long _loadVotes(ArrayList<PreparedMessage> vote_msgs, long rowID) {
		long start_row = rowID;
		boolean wrapped = false;
		long last_row = -1;
		ArrayList<ArrayList<Object>> votes;
		int retrieved = 0;
		vote_msgs.clear();
		for(;;){
			String sql = 
					sql_votes+
					" v."+net.ddp2p.common.table.signature.arrival_date+" > ?  AND "+
					" v.ROWID > "+rowID+
					(wrapped?(" AND v.ROWID <= "+start_row):"")+
					" AND v."+net.ddp2p.common.table.signature.signature+" IS NOT NULL "+
					" AND m."+net.ddp2p.common.table.motion.broadcasted+" <> '0' "+
					" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' "+
					" AND c."+net.ddp2p.common.table.constituent.broadcasted+" <> '0' "+
					" ORDER BY v.ROWID"+
					" LIMIT "+(VOTES_SIZE - retrieved);
			try{
				votes = Application.getDB().select(sql, new String[]{this.getDelayString()});
				if(DEBUG)System.out.println("loadVotes : ArrayList size "+votes.size());
				for(ArrayList<Object> vote : votes){
					D_Vote v = new D_Vote();
					D_Message asn1=new D_Message();
					asn1.sender = D_Peer.getEmpty(); 
					asn1.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID();
					last_row = Integer.parseInt(vote.get(net.ddp2p.common.table.signature.S_ID).toString());
					v.init_all(vote);
					asn1.vote = v;
					if(DEBUG)System.out.println("VOTE_ID : "+asn1.vote.getLIDstr());
					ArrayList<Object> org_data = get_org_db_by_local(v.getOrganizationLIDstr());
					asn1.organization = get_ASN_Organization_by_OrgID(org_data,v.getOrganizationLIDstr());
					Encoder enc = asn1.getEncoder();	
					byte msg [] = enc.getBytes();
					if(DEBUG)System.out.println("MSG : "+msg);
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					vote_msgs.add(pm);	
					if(vote_msgs.size()>=0)
						WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Recent_queue,
								""+(vote_msgs.size()-1),WirelessLog.vote_type,msg);
				}
			}catch(Exception e){
				if(_DEBUG)System.out.println("BroadcastQueueRecent() : loadVotes: EXCEPTION");
				e.printStackTrace();
				return -1;
			}
			retrieved = vote_msgs.size();
			if(wrapped) {
				if(DEBUG)System.out.println("BroadcastQueueRecent : loadVotes() : "
						+vote_msgs.size()+" loaded");
				break;
			}
			if((votes.size() < VOTES_SIZE)&&(rowID != -1)) {
				rowID = -1;
				wrapped = true;
				continue;
			}
			if(DEBUG)System.out.println("BroadcastQueueMyData : loadVotes() : "
					+vote_msgs.size()+" loaded");
			break;
		}
		return last_row;
	} 
	/**
	 * loadNeihoborhoods
	 * @param neigh_msgs
	 * @param rowID
	 * @return last neigh index
	 */
	long loadNeighborhoods(ArrayList<PreparedMessage> neighs_msgs,long rowID) {
		if(DEBUG)System.out.println("BroadcatQueueRecent : loadNeighborhoods() : START");
		return _loadNeighborhoods(neighs_msgs, rowID);
	}
	static String sql_neighs =
			"SELECT n."
					+net.ddp2p.common.table.neighborhood.neighborhood_ID
					+" FROM "+net.ddp2p.common.table.neighborhood.TNAME+" AS n "
					+" LEFT JOIN "+net.ddp2p.common.table.neighborhood.TNAME+" AS d ON(d."+net.ddp2p.common.table.neighborhood.parent_nID+"=n."+net.ddp2p.common.table.neighborhood.neighborhood_ID+") "
					+" LEFT JOIN "+net.ddp2p.common.table.organization.TNAME+" AS o ON(o."+net.ddp2p.common.table.organization.organization_ID+"=n."+net.ddp2p.common.table.neighborhood.organization_ID+") "
					+" LEFT JOIN "+net.ddp2p.common.table.constituent.TNAME+" AS c ON(c."+net.ddp2p.common.table.constituent.constituent_ID+"=n."+net.ddp2p.common.table.neighborhood.submitter_ID+")"
					+" LEFT JOIN "+net.ddp2p.common.table.key.TNAME+" AS k ON(k."+net.ddp2p.common.table.key.public_key+"=c."+net.ddp2p.common.table.constituent.global_constituent_ID+")"
					+" WHERE" 
					+" k."+net.ddp2p.common.table.key.secret_key+" IS NOT NULL AND"
					+" d."+net.ddp2p.common.table.neighborhood.neighborhood_ID+" IS NULL"
					;
	private long _loadNeighborhoods(ArrayList<PreparedMessage> neighs_msgs, long rowID) {
		long start_row = rowID;
		boolean wrapped = false;
		long olID = -1;
		long last_row = -1;
		ArrayList<ArrayList<Object>> neighs;
		int retrieved = 0;
		neighs_msgs.clear();
		int i=0;
		for(;;){
			String sql = 
					sql_neighs+
					" AND n."+net.ddp2p.common.table.neighborhood.arrival_date+" > ?"+
					" AND n.ROWID > "+rowID+
					(wrapped?(" AND n.ROWID <= "+start_row):"")+
					" AND n."+net.ddp2p.common.table.neighborhood.signature+" IS NOT NULL " +
					" AND o."+net.ddp2p.common.table.organization.broadcasted+" <> '0' "+
					" ORDER BY n.ROWID"+
					" LIMIT "+(NEIGHS_SIZE - retrieved);
			if(DEBUG)System.out.println("sql_neighs : "+sql);
			try{
				neighs = Application.getDB().select(sql, new String[]{this.getDelayString()});
				if(DEBUG)System.out.println("loadNeighborhoods : ArrayList size "+neighs.size());
				for(ArrayList<Object> neigh : neighs) {
					D_Message asn1=new D_Message();
					asn1.sender = D_Peer.getEmpty(); 
					asn1.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID();
					String local_id = Util.getString(neigh.get(0));
					String gid = D_Neighborhood.getGIDFromLID(local_id);
					D_Neighborhood n[] = new  D_Neighborhood[6];
					n = D_Neighborhood.getNeighborhoodHierarchy(gid, local_id, 2, olID);
					asn1.neighborhoods = n;
					for(int j=0;j<asn1.neighborhoods.length;j++)
						if(DEBUG)System.out.println("NID : "+asn1.neighborhoods[j].getLID());
					if(DEBUG)System.out.println("NID : "+asn1.neighborhoods[asn1.neighborhoods.length-1].getLID());
					last_row  = asn1.neighborhoods[0].getLID();
					long c_id = Long.parseLong(asn1.neighborhoods[0].getSubmitterLIDstr());
					asn1.constituent = D_Constituent.getConstByLID(c_id, true, false);
					Encoder enc = asn1.getEncoder();	
					byte msg [] = enc.getBytes();
					if(DEBUG)System.out.println("MSG : "+msg.length);					
					PreparedMessage pm = new PreparedMessage();
					pm.raw = msg;
					if(n!=null){
						for(int j=0;j< n.length;j++){
						}
					}
					neighs_msgs.add(pm);
					if(neighs_msgs.size()>=0)
						WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Recent_queue,
								""+(neighs_msgs.size()-1),WirelessLog.neigh_type,msg);
				}
			}catch(Exception e){
				if(_DEBUG)System.out.println("BroadcatQueueRecent : loadNeighborhoods: EXCEPTION");
				e.printStackTrace();
				return -1;
			}
			retrieved = neighs_msgs.size();
			if(wrapped) {
				if(DEBUG)System.out.println("BroadcatQueueRecent : loadNeighborhoods() : "
						+neighs_msgs.size()+" loaded");
				break;
			}
			if((neighs.size() < NEIGHS_SIZE)&&(rowID != -1)) {
				rowID = -1;
				wrapped = true;
				continue;
			}
			if(DEBUG)System.out.println("BroadcatQueueRecent : loadNeighborhoods() : "
					+neighs_msgs.size()+" loaded");
			break;
		}
		return last_row;
	}
	/**
	 * get_org_db_by_local
	 * @param org_id
	 * @return ArrayList<Object>
	 * @throws P2PDDSQLException
	 */
	public static ArrayList<Object> get_org_db_by_local(String org_id) throws P2PDDSQLException {
		String sql = "SELECT "+net.ddp2p.common.table.organization.field_list+" FROM "+
				net.ddp2p.common.table.organization.TNAME+" WHERE "+net.ddp2p.common.table.organization.organization_ID+
				"=?;";
		ArrayList<ArrayList<Object>> org_data = Application.getDB().select(sql, new String[]{org_id});
		return org_data.get(0);
	}
	/**
	 * buildPeerMessage() : build the Peer MSG
	 * @param peer
	 * @param rowID
	 * @return byte[]
	 * @throws P2PDDSQLException
	 * @throws ASN1DecoderFail
	 */
	@SuppressWarnings("static-access")
	private byte[] buildPeerMessage(ArrayList<Object> peer, long rowID) throws P2PDDSQLException, ASN1DecoderFail {
		D_Message asn1=new D_Message();
		asn1.sender = D_Peer.getEmpty();
		asn1.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID();
		String GID =  Util.getString(peer.get(net.ddp2p.common.table.peer.PEER_COL_GID));
		asn1.Peer = D_Peer.getPeerByGID_or_GIDhash(GID, null, true, false, false, null);
		if(DEBUG)System.out.println("PEER DATA : "+asn1.Peer);
		if(DEBUG)System.out.println("PEER_ID : "+asn1.Peer.getLocalPeerIDforGID(asn1.Peer.component_basic_data.globalID));
		Encoder enc = asn1.getEncoder();	
		byte msg [] = enc.getBytes();
		return msg;
	}
	/**
	 * buildOrganizationMessage() : build the Org MSG
	 * @param org
	 * @param local_id
	 * @return
	 * @throws P2PDDSQLException
	 * @throws ASN1DecoderFail
	 */
	@SuppressWarnings("static-access")
	byte[] buildOrganizationMessage(ArrayList<Object> org, String local_id) throws P2PDDSQLException, ASN1DecoderFail{
		D_Message asn1=new D_Message();
		asn1.sender = D_Peer.getEmpty(); 
		asn1.sender.component_basic_data.globalID = HandlingMyself_Peer.getMyPeerGID();
		asn1.organization = null; 
		asn1.organization =  get_ASN_Organization_by_OrgID(org,local_id);
		if(DEBUG){
			if(_DEBUG)System.out.println("BroadcastQueueMyData:buildOrganizationMessage: org is "+asn1.organization);
			if(!asn1.organization.verifySignature()) 
				if(_DEBUG)System.out.println("Fail to verify signature!");
		}
		if(DEBUG)System.out.println("ORG_ID : "+asn1.organization.getLIDbyGID(asn1.organization.getGID()));
		Encoder enc = asn1.getEncoder();	
		byte msg [] = enc.getBytes();
		return msg;
	}
	/**
	 * 
	 * @param _org
	 * @param _local_id
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_Organization get_ASN_Organization_by_OrgID(ArrayList<Object> _org, String _local_id)
			throws P2PDDSQLException {
		D_Organization ASN_Org1 = null; 
		try {
			ASN_Org1 = D_Organization.getOrgByLID_NoKeep(_local_id, true);
			ASN_Org1.creator = D_Peer.getPeerByLID_NoKeep(Util.getString(_org.get(net.ddp2p.common.table.organization.ORG_COL_CREATOR_ID)),true);
			return ASN_Org1;
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * construct Peer MSG
	 * @param global_creator_id
	 * @return
	 * @throws P2PDDSQLException
	 */
	private static D_Peer Get_creatorby_ID(String global_creator_id) throws P2PDDSQLException {
		return D_Peer.getPeerByLID_NoKeep(global_creator_id, true);
	}
	@Override
	public byte[] getNext(long msg_c) {
		return null;
	}
}
