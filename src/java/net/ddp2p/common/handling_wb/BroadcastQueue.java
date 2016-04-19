package net.ddp2p.common.handling_wb;
import java.util.ArrayList;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Interests;
import net.ddp2p.common.simulator.WirelessLog;
import net.ddp2p.common.util.DDP2P_ServiceThread;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.common.wireless.Broadcasting_Probabilities;
public abstract class BroadcastQueue implements BroadcastingQueue{
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	public static final Object loader_monitor = new Object();
	ArrayList<PreparedMessage> m_PreparedMessagesVotes = new ArrayList<PreparedMessage>();
	ArrayList<PreparedMessage> m_PreparedMessagesOrgs = new ArrayList<PreparedMessage>();
	ArrayList<PreparedMessage> m_PreparedMessagesConstituents = new ArrayList<PreparedMessage>();
	ArrayList<PreparedMessage> m_PreparedMessagesPeers = new ArrayList<PreparedMessage>();
	ArrayList<PreparedMessage> m_PreparedMessagesWitnesses = new ArrayList<PreparedMessage>();
	ArrayList<PreparedMessage> m_PreparedMessagesNeighborhoods = new ArrayList<PreparedMessage>();
	long m_lastVote=-1;
	long m_lastRecvVote=-1;
	long m_lastOrg=-1;
	long m_lastConstituent=-1;
	long m_lastPeer = -1;
	long m_lastWitness = -1;
	long m_lastNeighborhoods = -1;
	int m_iCrtVotes = 0;
	int m_iCrtOrgs = 0;
	int m_iCrtConstituents = 0;
	int m_iCrtPeers = 0;
	int m_iCrtWitnesses = 0;
	int m_iCrtNeighborhoods = 0;
	public int count_broadcasted_orgs = 0;
	public int count_broadcasted_constituents = 0;
	public int count_broadcasted_votes = 0;
	public int count_broadcasted_peers = 0;
	public int count_broadcasted_witnesses = 0;
	public int count_broadcasted_neighborhoods = 0;
	public boolean loader_neighborhoods_running = false;
	public boolean loader_peers_running = false;
	public boolean loader_votes_running = false;
	public boolean loader_constituents_running = false;
	public boolean loader_witnesses_running = false;
	public boolean loader_orgs_running = false;
	final static public int broadcasts = 1;
	private static final int Q_ORGS = 0;
	private static final int Q_CONS = 1;
	private static final int Q_VOTE = 2;
	private static final int Q_WITN = 3;
	private static final int Q_PEER = 4;
	private static final int Q_NEIG = 5;
	public byte[] getNext(String queue_type,long _msg_c) {
		try {
			return _getNext(queue_type,_msg_c, null).raw;
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	public PreparedMessage getNext(String queue_type,long _msg_c, PreparedMessage pm) {
		try {
			return _getNext(queue_type,_msg_c, pm);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	static final int SELECT_ORGS = 0;
	static final int SELECT_CONS = 1;
	static final int SELECT_VOTE = 2;
	static final int SELECT_WITN = 3;
	static final int SELECT_PEER = 4;
	static final int SELECT_NEIG = 5;
	/**
	 * choose either Org, Constituent, Vote , Peer, Witness by a certain probability
	 	and prepare it to be broadcasted
	 * @return byte[]
	 * @throws P2PDDSQLException
	 */
	public PreparedMessage _getNext(String queue_type,long msg_cntr, PreparedMessage pm) throws P2PDDSQLException {
		if(pm==null){pm=new PreparedMessage(); if(DEBUG)System.out.println("input pm was null");}
		if(DEBUG)System.out.println("BroadcastQueue : _getNext()");
		int used_queue = -1;
		int used_queue_idx = -1;
		byte[] result = null;
		int choice = Util.pick_randomly(
				new float[]{Broadcasting_Probabilities.broadcast_organization,
						Broadcasting_Probabilities.broadcast_constituent,
						Broadcasting_Probabilities.broadcast_vote,
						Broadcasting_Probabilities.broadcast_witness,
						Broadcasting_Probabilities.broadcast_peer,
						Broadcasting_Probabilities.broadcast_neighborhood});
		if(DEBUG)System.out.println("Choice : "+choice);
		switch(choice){
		case SELECT_ORGS:
			boolean toLoadO = false;
			if(DEBUG)System.out.println("BroadcastQueue : getNext() : Org to be send");
			ArrayList<PreparedMessage> crt_orgs_queue;
			int crt_orgs_idx;
			synchronized(BroadcastQueue.loader_monitor){
				crt_orgs_idx = m_iCrtOrgs;
				m_iCrtOrgs++;
				crt_orgs_queue = m_PreparedMessagesOrgs;
				if(crt_orgs_idx >= crt_orgs_queue.size()){
					if(DEBUG)System.out.println("BroadcastQueue : getNext() : count = "+count_broadcasted_orgs);
					++ count_broadcasted_orgs;
					crt_orgs_idx = m_iCrtOrgs = 0;
					if((count_broadcasted_orgs >= broadcasts)&& (!net.ddp2p.common.simulator.WirelessLog.Handled_queue.equals(queue_type))) {
						toLoadO = true;
					}
				}
			}
			if(toLoadO) toLoadOrganizations();
			if(crt_orgs_queue.size() > 0){
				result = crt_orgs_queue.get(crt_orgs_idx).raw;
				if(pm!=null)pm = crt_orgs_queue.get(crt_orgs_idx);
				used_queue = Q_ORGS;
				used_queue_idx = crt_orgs_idx;
				if(DEBUG)System.out.println("BroadcastQueue : getNext() :count_crt = "+crt_orgs_idx);
				break;
			}
			break;
		case SELECT_CONS:
			boolean toLoadC = false;
			if(DEBUG)System.out.println("BroadcastQueue : getNext() : Constituent to be send");
			ArrayList<PreparedMessage> crt_constituents_queue;
			int crt_constituents_idx;
			synchronized(BroadcastQueue.loader_monitor){
				crt_constituents_idx = m_iCrtConstituents;
				m_iCrtConstituents++;
				crt_constituents_queue = m_PreparedMessagesConstituents;
				if(crt_constituents_idx >= crt_constituents_queue.size()){
					if(DEBUG)System.out.println("BroadcastQueue : getNext() : count = "+count_broadcasted_constituents);
					++ count_broadcasted_constituents;
					crt_constituents_idx = m_iCrtConstituents = 0;
					if((count_broadcasted_constituents >= broadcasts) && (!net.ddp2p.common.simulator.WirelessLog.Handled_queue.equals(queue_type))) {
						toLoadC = true;
					}
				}
			}
			if(toLoadC) toLoadConstituents();
			if(crt_constituents_queue.size() > 0){
				result = crt_constituents_queue.get(crt_constituents_idx).raw;
				if(pm!=null)pm=crt_constituents_queue.get(crt_constituents_idx);
				used_queue = Q_CONS;
				used_queue_idx = crt_constituents_idx;
				if(DEBUG)System.out.println("BroadcastQueue : getNext() :count_crt = "+crt_constituents_idx);
				break;
			}
			break;
		case SELECT_VOTE:
			boolean toLoadV = false;
			if(DEBUG)System.out.println("BroadcastQueue : getNext() : Vote to be send");
			ArrayList<PreparedMessage> crt_votes_queue;
			int crt_votes_idx;
			synchronized(BroadcastQueue.loader_monitor){
				m_iCrtVotes++;
				crt_votes_idx = m_iCrtVotes;
				crt_votes_queue = m_PreparedMessagesVotes;
				if(crt_votes_idx >= crt_votes_queue.size()){
					if(DEBUG)System.out.println("BroadcastQueue : getNext() : count = "+count_broadcasted_votes);
					++ count_broadcasted_votes;
					setNewRoundFromQueue(crt_votes_queue, count_broadcasted_votes);
					crt_votes_idx = m_iCrtVotes = 0;
					if((count_broadcasted_votes >= broadcasts)&& (!net.ddp2p.common.simulator.WirelessLog.Handled_queue.equals(queue_type))) {
						toLoadV = true;
					}
				}
			}
			if(toLoadV) toLoadVote();
			if(DEBUG)System.out.println("BroadcastQueue : getNext() :crt_votes_queue.size = "+crt_votes_queue.size());
			if(crt_votes_queue.size() > 0){
				result = getItemFromQueue(crt_votes_queue,crt_votes_idx,count_broadcasted_votes,Q_VOTE);
				if(DEBUG)System.out.println("BroadcastQueue : getNext() : result= "+result);
				if(pm!=null)pm = crt_votes_queue.get(crt_votes_idx);
				used_queue = Q_VOTE;
				used_queue_idx = crt_votes_idx;
				if(DEBUG)System.out.println("BroadcastQueue : getNext() :count_crt = "+crt_votes_idx);
				break;
			}
			break;
		case SELECT_WITN:
			boolean toLoadW = false;
			if(DEBUG)System.out.println("BroadcastQueue : getNext() : Witness to be send");
			ArrayList<PreparedMessage> crt_witnesses_queue;
			int crt_witnesses_idx;
			synchronized(BroadcastQueue.loader_monitor){
				if(DEBUG)System.out.println("BroadcastQueue : SELECT_WITN : m_iCrtWitnesses="+m_iCrtWitnesses);
				crt_witnesses_idx = m_iCrtWitnesses;
				m_iCrtWitnesses++;
				crt_witnesses_queue = m_PreparedMessagesWitnesses;
				if(crt_witnesses_idx >= crt_witnesses_queue.size()){
					if(DEBUG)System.out.println("BroadcastQueue : getNext() : count = "+count_broadcasted_witnesses);
					++ count_broadcasted_witnesses;
					setNewRoundFromQueue(crt_witnesses_queue, count_broadcasted_witnesses);
					crt_witnesses_idx = m_iCrtWitnesses = 0;
					if((count_broadcasted_witnesses >= broadcasts) && (!net.ddp2p.common.simulator.WirelessLog.Handled_queue.equals(queue_type))) {
						toLoadW = true;
					}
				}
			}
			if(toLoadW) toLoadWitness();
			if(DEBUG)System.out.println("BroadcastQueue : getNext() :crt_witnesses_queue.size() = "+crt_witnesses_queue.size());
			if(crt_witnesses_queue.size() > 0){
				result = getItemFromQueue(crt_witnesses_queue,crt_witnesses_idx,count_broadcasted_witnesses, Q_WITN);
				if(DEBUG)System.out.println("BroadcastQueue : getNext() : result= "+result);
				if(pm!=null)pm = crt_witnesses_queue.get(crt_witnesses_idx);
				used_queue = Q_WITN;
				used_queue_idx = crt_witnesses_idx;
				if(DEBUG)System.out.println("BroadcastQueue : getNext() :count_crt = "+crt_witnesses_idx);
				break;
			}
			break;
		case SELECT_PEER:
			boolean toLoadP = false;
			if(DEBUG)System.out.println("BroadcastQueue : getNext() : Peer to be send");
			ArrayList<PreparedMessage> crt_peers_queue;
			int crt_peers_idx;
			synchronized(BroadcastQueue.loader_monitor){
				crt_peers_idx = m_iCrtPeers;
				m_iCrtPeers++;
				crt_peers_queue = m_PreparedMessagesPeers;
				if(crt_peers_idx >= crt_peers_queue.size()){
					if(DEBUG)System.out.println("BroadcastQueue : getNext() : count = "+count_broadcasted_peers);
					++ count_broadcasted_peers;
					crt_peers_idx = m_iCrtPeers = 0;
					if((count_broadcasted_peers >= broadcasts) && (!net.ddp2p.common.simulator.WirelessLog.Handled_queue.equals(queue_type))) {
						toLoadP = true;
					}
				}
			}
			if(toLoadP)toLoadPeers();
			if(crt_peers_queue.size() > 0){
				result = crt_peers_queue.get(crt_peers_idx).raw;
				if(pm!=null)pm = crt_peers_queue.get(crt_peers_idx);
				used_queue = Q_PEER;
				used_queue_idx = crt_peers_idx;
				if(DEBUG)System.out.println("BroadcastQueue : getNext() :count_crt = "+crt_peers_idx);
				break;
			}
			break;
		case SELECT_NEIG:
			boolean toLoadN = false;
			if(DEBUG)System.out.println("BroadcastQueue : getNext() : Neighborhoods hierarchy to be send");
			ArrayList<PreparedMessage> crt_neighbors_queue;
			int crt_neighbors_idx;
			synchronized(BroadcastQueue.loader_monitor ) {
				crt_neighbors_idx = m_iCrtNeighborhoods;
				m_iCrtNeighborhoods++;
				crt_neighbors_queue = m_PreparedMessagesNeighborhoods;
				if(crt_neighbors_idx >= crt_neighbors_queue.size()) {
					if(DEBUG)System.out.println("BroadcastQueue : getNext() : count = "+count_broadcasted_neighborhoods);
					++ count_broadcasted_neighborhoods;
					crt_neighbors_idx = m_iCrtNeighborhoods = 0;
					if((count_broadcasted_neighborhoods >= broadcasts) && (!net.ddp2p.common.simulator.WirelessLog.Handled_queue.equals(queue_type))) {
						toLoadN = true;
					}
				}
			}
			if(toLoadN) toLoadNeighborhoods();
			if(crt_neighbors_queue.size() > 0){
				result = crt_neighbors_queue.get(crt_neighbors_idx).raw;
				if(pm!=null)pm=crt_neighbors_queue.get(crt_neighbors_idx);
				used_queue = Q_NEIG;
				used_queue_idx = crt_neighbors_idx;
				if(DEBUG)System.out.println("BroadcastQueue : getNext() :count_crt = "+crt_neighbors_idx);
				break;
			}
			break;
		}
		if(used_queue==-1) {if(DEBUG)System.out.println("Trying to broadcast from a queue that has no data !!");} 
		else WirelessLog.Print_to_log_Param(WirelessLog.log_broadcast, queue_type, ""+used_queue_idx, QUEUE_POLICY_NAME[used_queue], result,msg_cntr);
		if(pm!=null){if(DEBUG)System.out.println("BroadcastQueue:_getNext: return:"+pm.raw); 
		return pm;}
		else{
			pm = new PreparedMessage();
			pm.raw=result;
			return pm;
		}
	}
	/**
	 * To let specialized queues redefine this for preparing a new broadcast
	 * such as reseting the sent_flag to false;
	 * @param crt_witnesses_queue
	 * @param count_broadcasted_witnesses2
	 */
	protected void setNewRoundFromQueue(
			ArrayList<PreparedMessage> crt_witnesses_queue,
			int count_broadcasted_witnesses2) {
	}
	/**
	 * Defined to let specialized queues redefine this procedure for special effects
	 * such as the extraction of a random element in the circular queue
	 * @param crt_witnesses_queue
	 * @param crt_witnesses_idx
	 * @param count_broadcasted_witnesses2
	 * @return
	 */
	protected byte[] getItemFromQueue(
			ArrayList<PreparedMessage> crt_witnesses_queue,
			int crt_witnesses_idx, int count_broadcasted_witnesses2, int QUEUE) {
		if(DEBUG)System.out.println("BroadcastQueue : getItemFromQueue : crt_witnesses_idx="+crt_witnesses_idx);
		return crt_witnesses_queue.get(crt_witnesses_idx).raw;
	}
	public void toLoadOrganizations(){
		if(DEBUG)System.out.println("BroadcastableMessages : getNext() : reload Orgs");
		if(! loader_orgs_running) {
			loader_orgs_running  = true;
			new DDP2P_ServiceThread("Broadcast Queue: org loading", true){
				public void _run() {
					ArrayList<PreparedMessage> newQueue = loadOrgs();
					synchronized(BroadcastQueue.loader_monitor) {
						count_broadcasted_orgs = 0;
						m_iCrtOrgs = 0;
						m_PreparedMessagesOrgs = newQueue;
					}
					loader_orgs_running = false;
				}
			}.start();
		}
	}
	public void toLoadConstituents(){
		if(DEBUG)System.out.println("BroadcastableMessages : getNext() : reload Constituents");
		if(! loader_constituents_running) {
			loader_constituents_running  = true;
			new DDP2P_ServiceThread("Broadcast Queue: loadConstituents", true) {
				public void _run() {
					ArrayList<PreparedMessage> newQueue = loadConstituents();
					synchronized(BroadcastQueue.loader_monitor) {
						count_broadcasted_constituents = 0;
						m_iCrtConstituents = 0;
						m_PreparedMessagesConstituents = newQueue;
					}
					loader_constituents_running = false;
				}
			}.start();
		}
	}
	public void toLoadPeers(){
		if(DEBUG)System.out.println("BroadcastableMessages : getNext() : reload Peers");
		if(! loader_peers_running) {
			loader_peers_running  = true;
			new DDP2P_ServiceThread("BroadcastQueue: peers load", true) {
				public void _run() {
					ArrayList<PreparedMessage> newQueue = loadPeers();
					synchronized(BroadcastQueue.loader_monitor) {
						count_broadcasted_peers = 0;
						m_iCrtPeers = 0;
						m_PreparedMessagesPeers = newQueue;
					}
					loader_peers_running = false;
				}
			}.start();
		}
	}
	public void toLoadVote(){
		if(DEBUG)System.out.println("\n\n\nBroadcastQueue:toLoadVote() : reload Votes :loader_votes_running="+ loader_votes_running+ " \n\n");
		if(! loader_votes_running) {
			loader_votes_running  = true;
			if(DEBUG)System.out.println("\n\n\nBroadcastQueue:toLoadVote() : starting thread to load votes\n\n");
			new DDP2P_ServiceThread("BroadcastQueue: vote loading", true) {
				public void _run() {
					if(DEBUG)System.out.println("BroadcastQueue: toLoadVote() : thread started calling loadVotes()");
					ArrayList<PreparedMessage> newQueue = loadVotes();
					synchronized(BroadcastQueue.loader_monitor) {
						count_broadcasted_votes = 0;
						m_iCrtVotes = 0;
						m_PreparedMessagesVotes = newQueue;
					}
					if(DEBUG)System.out.println("BroadcastQueue: toLoadVote() : loading done! new queue size = "+newQueue.size());
					loader_votes_running = false;
				}
			}.start();
		}
	}
	public void toLoadWitness(){
		if(DEBUG)System.out.println("BroadcastableMessages : getNext() : reload Witnesses");
		if(! loader_witnesses_running) {
			loader_witnesses_running  = true;
			new DDP2P_ServiceThread("BroadcastQueue: witness load", true){
				public void _run() {
					ArrayList<PreparedMessage> newQueue = loadWitnesses();
					synchronized(BroadcastQueue.loader_monitor) {
						count_broadcasted_witnesses = 0;
						m_iCrtWitnesses = 0;
						m_PreparedMessagesWitnesses = newQueue;
					}
					loader_witnesses_running = false;
				}
			}.start();
		}
	}
	public void toLoadNeighborhoods(){
		if(DEBUG)System.out.println("BroadcastQueue : getNext() : reload Neighborhoods");
		if(! loader_neighborhoods_running) {
			loader_neighborhoods_running  = true;
			new DDP2P_ServiceThread("BroadcastQueue: neighborhoods load", true){
				public void _run() {
					ArrayList<PreparedMessage> newQueue = loadNeighborhoods();
					synchronized(BroadcastQueue.loader_monitor) {
						count_broadcasted_neighborhoods = 0;
						m_iCrtNeighborhoods = 0;
						m_PreparedMessagesNeighborhoods = newQueue;
					}
					loader_neighborhoods_running = false;
				}
			}.start();
		}
	}
	String[] QUEUE_POLICY_NAME={"O","C","V","W","P","N"};
	ArrayList<PreparedMessage> loadNeighborhoods() {
		if(DEBUG)System.out.println("BroadcastQueue : loadNeighborhoods() : from db");
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		m_lastNeighborhoods = loadNeighborhoods(newQueue, m_lastNeighborhoods);
		if(DEBUG)System.out.println("size of vote queue is : "+m_PreparedMessagesNeighborhoods.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	abstract long loadNeighborhoods(ArrayList<PreparedMessage> m_ALbaPreparedMessagesNeighborhoods2, long m_lastNeighborhoods2);
	ArrayList<PreparedMessage> loadVotes() {
		if(DEBUG)System.out.println("BroadcastQueue : loadVotes() : from db");
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		m_lastVote = loadVotes(newQueue, m_lastVote);
		if(DEBUG)System.out.println("size of vote queue is : "+m_PreparedMessagesVotes.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	abstract long loadVotes(ArrayList<PreparedMessage> m_ALbaPreparedMessagesVotes2, long m_lastVote2);
	ArrayList<PreparedMessage> loadConstituents() {
		if(DEBUG)System.out.println("BroadcastQueue : loadConstituents() : from db");
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		m_lastConstituent = loadConstituents(newQueue,m_lastConstituent);
		if(DEBUG)System.out.println("size of constituent queue is : "+m_PreparedMessagesConstituents.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	abstract long loadConstituents(
			ArrayList<PreparedMessage> m_PreparedMessagesConstituents2,
			long m_lastConstituent2);
	ArrayList<PreparedMessage> loadPeers(){
		if(DEBUG)System.out.println("BroadcastQueue : loadPeers() : from db");
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		m_lastPeer = loadPeers(newQueue,m_lastPeer);
		if(DEBUG)System.out.println("size of peer queue is : "+m_PreparedMessagesPeers.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	abstract long loadPeers(ArrayList<PreparedMessage> m_ALbaPreparedMessagesPeers2,
			long m_lastPeer2);
	ArrayList<PreparedMessage> loadOrgs() {
		if(DEBUG)System.out.println("BroadcastQueue : loadOrgs() : from db");
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		try {
			m_lastOrg = loadOrgs(newQueue,m_lastOrg);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		if(DEBUG)System.out.println("size of Orgs queue is : "+m_PreparedMessagesOrgs.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	abstract long loadOrgs(ArrayList<PreparedMessage> m_PreparedMessagesOrgs2,
			long m_lastOrg2) throws P2PDDSQLException;
	ArrayList<PreparedMessage> loadWitnesses() {
		if(DEBUG)System.out.println("BroadcastQueue : loadWitnesses() : from db");
		ArrayList<PreparedMessage> newQueue = new ArrayList<PreparedMessage>();
		m_lastWitness = loadWitnesses(newQueue,m_lastWitness);
		if(DEBUG)System.out.println("size of Witnesses queue is : "+m_PreparedMessagesWitnesses.size());
		if(DEBUG)System.out.println("\n---------------------------------------------------------------\n");
		return newQueue;
	}
	abstract long loadWitnesses(
			ArrayList<PreparedMessage> m_ALbaPreparedMessagesWitnesses2,
			long m_lastWitness2);
}
