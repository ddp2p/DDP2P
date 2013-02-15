package handling_wb;

import java.util.ArrayList;

import ciphersuits.Cipher;

import com.almworks.sqlite4java.SQLiteException;

import simulator.WirelessLog;
import util.Util;

public class BroadcastQueueHandled extends BroadcastQueue {

	public static final int VOTE = 1;
	public static final int CONSTITUENT = 2;
	public static final int ORGANIZATION = 3;
	public static final int PEER = 4;
	public static final int WITNESS = 5;
	public static final int NEIGHBORHOOD = 6;
	public static final int MAX_QUEUE = 100;
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;

	@Override
	public void loadAll() {
		// TODO Auto-generated method stub
	}

	public void addMessage(PreparedMessage pm, int type) {
		if(DEBUG)System.out.println("BroadcastQueueHandled : addMessage()");
		byte[] msg=pm.raw;
		switch(type) {
		case VOTE:
		
			this.m_PreparedMessagesVotes.add(pm);
			//if(this.m_ALbaPreparedMessagesVotes.size() >= MAX_QUEUE)
			WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Handled_queue,
						""+(m_PreparedMessagesVotes.size()-1),WirelessLog.vote_type,msg);
			if(DEBUG)System.out.println("BroadcastQueueHandled : addMessage() : VOTE");
			if(this.m_PreparedMessagesVotes.size() > MAX_QUEUE) {
				this.m_PreparedMessagesVotes.remove(0);
				this.m_iCrtVotes--;
				if(m_iCrtVotes < 0) m_iCrtVotes = m_PreparedMessagesVotes.size() - 1;
			}
			break;

		case CONSTITUENT:
			
			this.m_PreparedMessagesConstituents.add(pm);
			//if(this.m_ALbaPreparedMessagesConstituents.size()>=0)
				WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Handled_queue,
						""+(m_PreparedMessagesConstituents.size()-1),WirelessLog.const_type,msg);
			if(DEBUG)System.out.println("BroadcastQueueHandled : addMessage() : CONSTITUENT");
			if(this.m_PreparedMessagesConstituents.size() > MAX_QUEUE) {
				this.m_PreparedMessagesConstituents.remove(0);
				this.m_iCrtConstituents--;
				if(m_iCrtConstituents < 0) m_iCrtConstituents = m_PreparedMessagesConstituents.size() - 1;
			}
			break;

		case ORGANIZATION:
			this.m_PreparedMessagesOrgs.add(pm);
			//if(this.m_ALbaPreparedMessagesOrgs.size()>=0)
				WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Handled_queue,
						""+(m_PreparedMessagesOrgs.size()-1),WirelessLog.org_type,msg);
			if(DEBUG)System.out.println("BroadcastQueueHandled : addMessage() : ORGANIZATION");
			if(this.m_PreparedMessagesOrgs.size() > MAX_QUEUE) {
				this.m_PreparedMessagesOrgs.remove(0);
				this.m_iCrtOrgs--;
				if(m_iCrtOrgs < 0) m_iCrtOrgs = m_PreparedMessagesOrgs.size() - 1;
			}
			break;

		case PEER:
			this.m_PreparedMessagesPeers.add(pm);
			//if(this.m_ALbaPreparedMessagesPeers.size()>=0)
				WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Handled_queue,
						""+(m_PreparedMessagesPeers.size()-1),WirelessLog.peer_type,msg);
			if(DEBUG)System.out.println("BroadcastQueueHandled : addMessage() : PEER");
			if(this.m_PreparedMessagesPeers.size() > MAX_QUEUE) {
				this.m_PreparedMessagesPeers.remove(0);
				this.m_iCrtPeers--;
				if(m_iCrtPeers < 0) m_iCrtPeers = m_PreparedMessagesPeers.size() - 1;
			}
			break;

		case NEIGHBORHOOD:
			this.m_PreparedMessagesNeighborhoods.add(pm);
			//if(this.m_ALbaPreparedMessagesNeighborhoods.size()>=0)
				WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Handled_queue,
						""+(m_PreparedMessagesNeighborhoods.size()-1),WirelessLog.neigh_type,msg);
			if(DEBUG)System.out.println("BroadcastQueueHandled : addMessage() : NEIGHBORHOOD");
			if(this.m_PreparedMessagesNeighborhoods.size() > MAX_QUEUE) {
				this.m_PreparedMessagesNeighborhoods.remove(0);
				this.m_iCrtNeighborhoods--;
				if(m_iCrtNeighborhoods < 0) m_iCrtNeighborhoods = m_PreparedMessagesNeighborhoods.size() - 1;
			}
			break;

		case WITNESS:
			this.m_PreparedMessagesWitnesses.add(pm);
			//if(this.m_ALbaPreparedMessagesWitnesses.size()>=0)
				WirelessLog.logging(WirelessLog.log_queue,WirelessLog.Handled_queue,
						""+(m_PreparedMessagesWitnesses.size()-1),WirelessLog.wit_type,msg);
			if(DEBUG)System.out.println("BroadcastQueueHandled : addMessage() : WITNESS");
			if(this.m_PreparedMessagesWitnesses.size() > MAX_QUEUE) {
				this.m_PreparedMessagesWitnesses.remove(0);
				this.m_iCrtWitnesses--;
				if(m_iCrtWitnesses < 0) m_iCrtWitnesses = m_PreparedMessagesWitnesses.size() - 1;
			}
			break;
		}
	}

	@Override
	long loadVotes(ArrayList<PreparedMessage> m_ALbaPreparedMessagesVotes2,
			long m_lastVote2) {
		System.err.println("BroadcastQueueHandled: loadVotes: not used!");
		System.exit(1);
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	long loadConstituents(ArrayList<PreparedMessage> m_ALbaPreparedMessagesConstituents2,
			long m_lastConstituent2) {
		System.err.println("BroadcastQueueHandled: loadConstituents: not used!");
		System.exit(1);
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	long loadPeers(ArrayList<PreparedMessage> m_ALbaPreparedMessagesPeers2,
			long m_lastPeer2) {
		System.err.println("BroadcastQueueHandled: loadPeers: not used!");
		System.exit(1);
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	long loadOrgs(ArrayList<PreparedMessage> m_ALbaPreparedMessagesOrgs2, long m_lastOrg2)
			throws SQLiteException {
		System.err.println("BroadcastQueueHandled: loadOrgs: not used!");
		System.exit(1);
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	long loadWitnesses(ArrayList<PreparedMessage> m_ALbaPreparedMessagesWitnesses2,
			long m_lastWitness2) {
		System.err.println("BroadcastQueueHandled: loadWitnesses: not used!");
		System.exit(1);
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	long loadNeighborhoods(
			ArrayList<PreparedMessage> m_ALbaPreparedMessagesNeighborhoods2,
			long m_lastNeighborhoods2) {
		System.err.println("BroadcastQueueHandled: loadNeighborhoods: not used!");
		System.exit(1);
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte[] getNext(long msg_c) {
		System.err.println("BroadcastQueueHandled: getNext: not used!");
		System.exit(1);
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void toLoadVote(){
		
	}
	@Override
	public void toLoadPeers(){
		
	}
	@Override
	public void toLoadOrganizations(){
		
	}
	@Override
	public void toLoadWitness(){
		
	}
	@Override
	public void toLoadConstituents(){
		
	}
	@Override
	public void toLoadNeighborhoods(){
		
	}

		
}